# notification-system

Scalable, stateless notification server built with **Java 21 + Spring Boot 3.2**, PostgreSQL, Redis, and RabbitMQ.
Supports iOS push (APNs), Android push (FCM), SMS (Twilio/Nexmo), and Email (SendGrid/Mailchimp) channels with
retry-on-failure semantics and per-user opt-in controls.

---

## Table of Contents

1. [Architecture](#architecture)
2. [Technology Stack](#technology-stack)
3. [Database Schema](#database-schema)
4. [Message Queue Design](#message-queue-design)
5. [Caching Strategy](#caching-strategy)
6. [Notification Flow](#notification-flow)
7. [Retry & Failure Handling](#retry--failure-handling)
8. [Third-Party Service Stubs](#third-party-service-stubs)
9. [Scalability Design](#scalability-design)
10. [How to Run](#how-to-run)
11. [API Reference](#api-reference)
12. [Notification Templates](#notification-templates)
13. [Configuration Reference](#configuration-reference)
14. [Project Structure](#project-structure)

---

## Architecture

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        Upstream Callers                                  │
│          (micro-services · cron jobs · distributed event systems)        │
└───────────────────────────────────┬──────────────────────────────────────┘
                                    │  POST /api/notifications/send
┌───────────────────────────────────▼──────────────────────────────────────┐
│                     Notification Service  (port 8080)                    │
│                                                                          │
│  UserController  DeviceController  SettingsController  NotificationCtrl  │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │                    NotificationService                              │ │
│  │  1. Resolve User  →  Redis cache  →  (miss) PostgreSQL             │ │
│  │  2. Check opt-in  →  notification_settings table                   │ │
│  │  3. Resolve Devices → Redis cache → (miss) PostgreSQL              │ │
│  │  4. Fetch Template → Redis cache → (miss) PostgreSQL               │ │
│  │  5. Render payload ({{variable}} substitution)                     │ │
│  │  6. Persist NotificationLog  (status=QUEUED)                       │ │
│  │  7. Publish to RabbitMQ queue                                      │ │
│  └──────────┬──────────────┬──────────────┬──────────────┬────────────┘ │
└─────────────│──────────────│──────────────│──────────────│──────────────┘
              │              │              │              │
       ┌──────▼──────┐ ┌────▼────┐  ┌─────▼──────┐ ┌────▼──────┐
       │notification │ │notif.   │  │notification│ │notif.     │
       │.ios         │ │android  │  │.sms        │ │.email     │
       │   (queue)   │ │(queue)  │  │  (queue)   │ │ (queue)   │
       └──────┬──────┘ └────┬────┘  └─────┬──────┘ └────┬──────┘
              │              │              │              │
       ┌──────▼──────┐ ┌────▼────┐  ┌─────▼──────┐ ┌────▼──────┐
       │IOS Worker   │ │Android  │  │SMS Worker  │ │Email      │
       │(APNs stub)  │ │Worker   │  │(Twilio     │ │Worker     │
       │             │ │(FCM     │  │ stub)      │ │(SendGrid  │
       │             │ │ stub)   │  │            │ │ stub)     │
       └──────┬──────┘ └────┬────┘  └─────┬──────┘ └────┬──────┘
              │   on failure: re-queue (max 5 retries)   │
              └──────────────────────────────────────────┘
                        │ exhausted retries
                  ┌─────▼──────────────────┐
                  │  Dead Letter Exchange  │
                  │  (notification.dlx)    │
                  │  .ios.dlq  .sms.dlq    │
                  │  .android.dlq .email.. │
                  └────────────────────────┘
```

---

## Technology Stack

| Component        | Technology                                         |
|------------------|----------------------------------------------------|
| Language         | Java 21                                            |
| Framework        | Spring Boot 3.2.4                                  |
| Build            | Maven 3.9 + Maven Wrapper (`./mvnw`)               |
| REST API         | Spring MVC (`spring-boot-starter-web`)             |
| Persistence      | Spring Data JPA + Hibernate + PostgreSQL 16        |
| DB Migrations    | Flyway 10                                          |
| Cache            | Redis 7 via Spring Cache + Lettuce client          |
| Message Queue    | RabbitMQ 3.13 via Spring AMQP                      |
| Error format     | RFC 7807 `ProblemDetail`                           |
| Observability    | Spring Boot Actuator (`/actuator/health`, metrics) |
| Containerisation | Docker + Docker Compose                            |

---

## Database Schema

Flyway migration: [`V1__init.sql`](src/main/resources/db/migration/V1__init.sql)

```
┌──────────────────┐       ┌────────────────────────┐
│      users       │       │        devices          │
├──────────────────┤       ├────────────────────────┤
│ id  BIGSERIAL PK │◄──┐   │ id  BIGSERIAL PK       │
│ name             │   │   │ user_id  FK → users(id) │
│ email  UNIQUE    │   └───│ token    UNIQUE          │
│ phone            │       │ platform  IOS|ANDROID   │
│ created_at       │       │ active   BOOLEAN        │
│ updated_at       │       │ created_at              │
└──────────────────┘       └────────────────────────┘

┌───────────────────────────────┐     ┌──────────────────────────────┐
│    notification_settings      │     │    notification_templates    │
├───────────────────────────────┤     ├──────────────────────────────┤
│ id  BIGSERIAL PK              │     │ id  BIGSERIAL PK             │
│ user_id  FK → users(id)       │     │ name  UNIQUE                 │
│ channel  PUSH|SMS|EMAIL       │     │ title_template               │
│ opt_in   BOOLEAN              │     │ body_template                │
│ UNIQUE(user_id, channel)      │     │ channel  PUSH|SMS|EMAIL|ALL  │
└───────────────────────────────┘     └──────────────────────────────┘

┌──────────────────────────────────────────────────┐
│                notification_logs                  │
├──────────────────────────────────────────────────┤
│ id  BIGSERIAL PK                                 │
│ user_id      FK → users(id)                      │
│ channel      PUSH | SMS | EMAIL                  │
│ platform     IOS | ANDROID  (nullable, PUSH only)│
│ template_name                                    │
│ status       QUEUED | SENT | RETRYING | FAILED   │
│ retry_count  INT DEFAULT 0                       │
│ error_message TEXT                               │
│ payload      TEXT  (serialised JSON sent)        │
│ created_at, updated_at                           │
└──────────────────────────────────────────────────┘
```

**Indexes:** `devices(user_id)`, `devices(token)`, `notification_settings(user_id)`, `notification_logs(user_id)`, `notification_logs(status)`, `notification_logs(created_at)`

---

## Message Queue Design

### Queues

| Queue                  | Consumer                   | Purpose                          |
|------------------------|----------------------------|----------------------------------|
| `notification.ios`     | `IOSNotificationWorker`    | iOS push via APNs                |
| `notification.android` | `AndroidNotificationWorker`| Android push via FCM             |
| `notification.sms`     | `SMSNotificationWorker`    | SMS via Twilio/Nexmo             |
| `notification.email`   | `EmailNotificationWorker`  | Email via SendGrid/Mailchimp     |

### Dead-Letter Infrastructure

Every main queue is configured with:
```
x-dead-letter-exchange:    notification.dlx
x-dead-letter-routing-key: notification.<channel>.dlq
```

Messages that exhaust all retries are routed to:
- `notification.ios.dlq`
- `notification.android.dlq`
- `notification.sms.dlq`
- `notification.email.dlq`

### Message Format

Payloads are JSON-serialised `NotificationPayload` objects:

```json
{
  "logId": 42,
  "userId": 1,
  "userName": "Alice",
  "userEmail": "alice@example.com",
  "userPhone": "+14155550001",
  "deviceToken": "abc123...",
  "devicePlatform": "IOS",
  "channel": "PUSH",
  "templateName": "default",
  "title": "Hello, Alice!",
  "body": "Your order has shipped!",
  "retryCount": 0
}
```

---

## Caching Strategy

Redis is the primary cache for hot-path data. All caches use JSON serialisation (human-readable via `redis-cli`).

| Cache name  | Key pattern  | TTL      | Eviction trigger           |
|-------------|--------------|----------|----------------------------|
| `users`     | `{userId}`   | 1 hour   | User update / delete       |
| `devices`   | `{userId}`   | 30 min   | Device register/deactivate |
| `templates` | `{name}`     | 24 hours | *(manual cache eviction)*  |

**Cache-aside pattern:** The service first checks Redis. On a miss, data is loaded from PostgreSQL and written back to Redis. Stale entries are proactively evicted via `@CacheEvict` on mutating operations.

---

## Notification Flow

A full end-to-end trace for `POST /api/notifications/send` with `channel: EMAIL`:

```
1.  Controller receives NotificationRequest
2.  NotificationService.send() called
3.  → UserService.getUserById(userId)
        → Check Redis "users::{userId}"
        → Cache miss → query PostgreSQL users table
        → Store result in Redis (TTL 1h)
4.  → NotificationSettingsService.isOptedIn(userId, EMAIL)
        → Query notification_settings WHERE user_id=? AND channel='EMAIL'
        → If opt_in=false → return empty list (skip silently)
5.  → NotificationPayloadBuilderService.getTemplate("default")
        → Check Redis "templates::default"
        → Cache miss → query notification_templates table
        → Store result in Redis (TTL 24h)
6.  → render("Hello, {{userName}}!", {userName: "Alice"}) → "Hello, Alice!"
7.  → INSERT notification_logs (status=QUEUED, retry_count=0)
8.  → NotificationPublisher.publishEmail(payload)
        → RabbitTemplate.convertAndSend("notification.email", payload)
9.  Controller returns HTTP 202 Accepted:
        {"status":"QUEUED","queuedLogIds":[42],"count":1}

--- async, in EmailNotificationWorker ---

10. Consumer pulls message from "notification.email"
11. EmailService.send(payload)  [stub: simulates delivery]
12. Success → UPDATE notification_logs SET status='SENT'
    Failure → retry_count++
              → if retry_count < 5: re-queue, SET status='RETRYING'
              → if retry_count >= 5: SET status='FAILED', message → DLQ
```

---

## Retry & Failure Handling

### Worker-level retry

Each worker (`IOSNotificationWorker`, `AndroidNotificationWorker`, `SMSNotificationWorker`, `EmailNotificationWorker`) implements identical retry logic:

```
On delivery failure:
  newRetryCount = payload.retryCount + 1
  if newRetryCount >= maxRetries (default: 5):
    log.status = FAILED
    message dropped (routed to DLQ by broker)
  else:
    log.status = RETRYING
    log.retryCount = newRetryCount
    re-publish cloned payload with updated retryCount
```

### Spring AMQP broker-level retry

The listener container also has Spring AMQP retry configured (separate from worker logic):
- Initial interval: 2 seconds
- Multiplier: 2.0 (exponential backoff)
- Max interval: 10 seconds
- Max attempts: 3

### Status lifecycle

```
QUEUED → (worker picks up)
       → SENT           (delivery succeeded)
       → RETRYING       (delivery failed, will retry)
         → SENT         (subsequent retry succeeded)
         → RETRYING     (...)
           → FAILED     (max retries exhausted)
```

---

## Third-Party Service Stubs

All stubs are in the `com.notifications.thirdparty` package and implement the same contract: call `send(NotificationPayload)`, throw `ThirdPartyDeliveryException` on failure.

| Class          | Maps to          | Failure trigger                     |
|----------------|------------------|-------------------------------------|
| `APNsService`  | Apple APNs       | Device token contains `"fail"`      |
| `FCMService`   | Firebase FCM     | Device token contains `"fail"`      |
| `SMSService`   | Twilio / Nexmo   | Phone number contains `"fail"`      |
| `EmailService` | SendGrid / Mailchimp | Email address contains `"fail"` |

Each stub also simulates realistic network latency (`Thread.sleep`):
- APNs: 50–150 ms
- FCM: 30–100 ms
- SMS: 80–300 ms
- Email: 100–400 ms

**To replace a stub** with a real integration, implement the same `send(NotificationPayload)` signature in the corresponding class and add the required SDK dependency to `pom.xml`.

---

## Scalability Design

The service is designed to be **horizontally scalable** with zero shared in-process state:

| Concern              | Solution                                                      |
|----------------------|---------------------------------------------------------------|
| **Stateless API**    | No in-memory session or cache — all state in PostgreSQL/Redis |
| **Worker scaling**   | Run multiple service instances; each pulls from the same queues (RabbitMQ round-robins) |
| **Cache coherence**  | Redis is the single shared cache; `@CacheEvict` keeps it consistent |
| **DB connections**   | HikariCP pool (max 20) per instance; read-only queries use `@Transactional(readOnly=true)` |
| **Queue durability** | All queues declared durable; messages survive broker restarts |
| **Payload isolation**| Workers are decoupled — an email failure does not block push processing |
| **Observability**    | `/actuator/health`, `/actuator/metrics` for load-balancer health checks |

To scale horizontally:
```bash
# Run 3 service instances (each will consume from all queues concurrently)
docker compose up --scale notification-service=3
```

---

## How to Run

### Prerequisites

- **Java 21+** (or any JDK that supports `--release 21`)
- **Docker + Docker Compose** for the infrastructure services
- **Maven Wrapper** included (`./mvnw`) — no local Maven installation needed

### Option A — Infrastructure in Docker, service local

```bash
# 1. Start PostgreSQL, Redis, and RabbitMQ
docker compose up -d postgres redis rabbitmq

# 2. Build and run the service
./mvnw spring-boot:run

# 3. Verify
curl http://localhost:8080/actuator/health
```

### Option B — Everything in Docker

```bash
# Build the image and start all services
docker compose up --build

# The service will wait for postgres/redis/rabbitmq health checks before starting
```

### Option C — Build a JAR

```bash
./mvnw package -DskipTests
java -jar target/notification-system-1.0.0.jar
```

### Verify the RabbitMQ queues

Open **http://localhost:15672** (user: `guest`, password: `guest`) → Queues tab.
You should see:
- `notification.ios`
- `notification.android`
- `notification.sms`
- `notification.email`
- Four corresponding `.dlq` queues

---

## API Reference

### Users — `POST/GET/PUT/DELETE /api/users`

| Method | Path              | Description           | Request Body              |
|--------|-------------------|-----------------------|---------------------------|
| POST   | `/api/users`      | Create a user         | `{name, email, phone?}`   |
| GET    | `/api/users`      | List all users        | —                         |
| GET    | `/api/users/{id}` | Get user by ID        | —                         |
| PUT    | `/api/users/{id}` | Update name & phone   | `{name, email, phone?}`   |
| DELETE | `/api/users/{id}` | Delete user (cascade) | —                         |

```bash
# Create a user
curl -X POST http://localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"Alice","email":"alice@example.com","phone":"+14155550001"}'

# Response: 201 Created
{
  "id": 1,
  "name": "Alice",
  "email": "alice@example.com",
  "phone": "+14155550001",
  "createdAt": "2026-09-30T17:00:00"
}
```

---

### Devices — `/api/devices`

| Method | Path                       | Description                             |
|--------|----------------------------|-----------------------------------------|
| POST   | `/api/devices`             | Register device token (idempotent)      |
| GET    | `/api/devices?userId=`     | List active devices for a user          |
| DELETE | `/api/devices/{id}?userId=`| Deactivate a device (soft delete)       |

```bash
# Register an iOS device
curl -X POST http://localhost:8080/api/devices \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"token":"device-token-abc123","platform":"IOS"}'

# Register an Android device
curl -X POST http://localhost:8080/api/devices \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"token":"fcm-reg-id-xyz","platform":"ANDROID"}'

# platform: "IOS" or "ANDROID"
```

---

### Notification Settings — `/api/notification-settings`

| Method | Path                          | Description                              |
|--------|-------------------------------|------------------------------------------|
| GET    | `?userId=`                    | Get all channel settings for a user      |
| PUT    | `/api/notification-settings`  | Create or update a channel opt-in setting |

```bash
# Opt out of email notifications
curl -X PUT http://localhost:8080/api/notification-settings \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"channel":"EMAIL","optIn":false}'

# channel: "PUSH" | "SMS" | "EMAIL"
# optIn: true | false  (default: true if no record exists)
```

---

### Send a Notification — `POST /api/notifications/send`

This is the primary integration endpoint for upstream services.

```bash
curl -X POST http://localhost:8080/api/notifications/send \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 1,
    "channel": "PUSH",
    "templateName": "alert_push",
    "variables": {
      "message": "Your package has been delivered!"
    }
  }'
```

**Response: `202 Accepted`**
```json
{
  "status": "QUEUED",
  "queuedLogIds": [101, 102],
  "count": 2
}
```

> For `PUSH`, one log entry is created per active device. For `SMS` and `EMAIL`, one entry per send.

**Channel values:**
- `PUSH` — routes to iOS and/or Android queues based on registered device platforms
- `SMS` — routes to SMS queue (user must have a `phone` number set)
- `EMAIL` — routes to email queue

---

### Notification Logs — `/api/notifications/logs`

```bash
# Paginated history for a user (newest first)
curl "http://localhost:8080/api/notifications/logs?userId=1&page=0&size=20"

# Single log entry with full status and payload
curl "http://localhost:8080/api/notifications/logs/101"
```

**Log status values:** `QUEUED` → `SENT` | `RETRYING` → `SENT` | `FAILED`

---

### Health & Metrics

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics
```

---

## Notification Templates

Templates use `{{variableName}}` placeholder substitution. Unresolved tokens are left as-is.

**Built-in variables** (automatically injected from user record):

| Variable       | Value                 |
|----------------|-----------------------|
| `{{userName}}` | User's display name   |
| `{{userEmail}}`| User's email address  |
| `{{userPhone}}`| User's phone number   |

Additional variables are supplied by the caller in the `variables` map.

**Seeded templates** (from [`V1__init.sql`](src/main/resources/db/migration/V1__init.sql)):

| Name          | Channel | Title                              | Body                                                      |
|---------------|---------|------------------------------------|-----------------------------------------------------------|
| `default`     | ALL     | `Hello, {{userName}}!`             | `You have a new notification: {{message}}`                |
| `promo_email` | EMAIL   | `Exclusive offer for {{userName}}` | `Hi {{userName}}, check out our latest deal: {{message}}` |
| `alert_push`  | PUSH    | `Alert`                            | `{{message}}`                                             |
| `alert_sms`   | SMS     | *(no title)*                       | `Alert for {{userName}}: {{message}}`                     |

---

## Configuration Reference

All values can be overridden via environment variables or `application.yml`.

### Environment Variables

| Variable            | Default           | Description                           |
|---------------------|-------------------|---------------------------------------|
| `DB_HOST`           | `localhost`       | PostgreSQL hostname                   |
| `DB_PORT`           | `5432`            | PostgreSQL port                       |
| `DB_NAME`           | `notifications`   | PostgreSQL database                   |
| `DB_USER`           | `notify_user`     | PostgreSQL username                   |
| `DB_PASSWORD`       | `notify_pass`     | PostgreSQL password                   |
| `REDIS_HOST`        | `localhost`       | Redis hostname                        |
| `REDIS_PORT`        | `6379`            | Redis port                            |
| `REDIS_PASSWORD`    | *(empty)*         | Redis password (if auth enabled)      |
| `RABBITMQ_HOST`     | `localhost`       | RabbitMQ hostname                     |
| `RABBITMQ_PORT`     | `5672`            | RabbitMQ AMQP port                    |
| `RABBITMQ_USER`     | `guest`           | RabbitMQ username                     |
| `RABBITMQ_PASSWORD` | `guest`           | RabbitMQ password                     |
| `RABBITMQ_VHOST`    | `/`               | RabbitMQ virtual host                 |
| `SERVER_PORT`       | `8080`            | HTTP server port                      |

### Application Properties (`application.yml`)

| Property                              | Default   | Description                          |
|---------------------------------------|-----------|--------------------------------------|
| `notification.retry.max-attempts`     | `5`       | Max delivery attempts per message    |
| `notification.retry.initial-delay-ms` | `1000`    | Initial retry delay (ms)             |
| `notification.retry.backoff-multiplier`| `2.0`    | Exponential backoff multiplier       |
| `notification.cache.user-ttl-seconds` | `3600`    | Redis TTL for user records           |
| `notification.cache.device-ttl-seconds`| `1800`   | Redis TTL for device records         |
| `notification.cache.template-ttl-seconds`| `86400` | Redis TTL for templates             |
| `notification.queues.ios`             | `notification.ios`     | iOS queue name         |
| `notification.queues.android`         | `notification.android` | Android queue name     |
| `notification.queues.sms`             | `notification.sms`     | SMS queue name         |
| `notification.queues.email`           | `notification.email`   | Email queue name       |

---

## Project Structure

```
notification-system/
├── pom.xml                              # Maven build (Spring Boot 3.2.4, Java 21)
├── Dockerfile                           # Multi-stage build (JDK 21 builder → JRE runtime)
├── docker-compose.yml                   # PostgreSQL + Redis + RabbitMQ + service
├── mvnw / .mvn/                         # Maven Wrapper
└── src/main/
    ├── resources/
    │   ├── application.yml              # Full configuration
    │   └── db/migration/
    │       └── V1__init.sql             # Flyway schema + seed templates
    └── java/com/notifications/
        ├── NotificationApplication.java # Spring Boot entry point
        │
        ├── config/
        │   ├── RabbitMQConfig.java      # 4 queues + DLX + DLQs + JSON converter
        │   └── RedisConfig.java         # Cache manager with per-cache TTLs
        │
        ├── model/                       # JPA entities
        │   ├── User.java
        │   ├── Device.java
        │   ├── NotificationSettings.java
        │   ├── NotificationTemplate.java
        │   ├── NotificationLog.java
        │   ├── NotificationChannel.java # Enum: PUSH | SMS | EMAIL
        │   ├── DevicePlatform.java      # Enum: IOS | ANDROID
        │   ├── NotificationStatus.java  # Enum: QUEUED | SENT | RETRYING | FAILED
        │   └── TemplateChannel.java     # Enum: PUSH | SMS | EMAIL | ALL
        │
        ├── dto/                         # Request / response objects
        │   ├── UserRequest.java
        │   ├── DeviceRequest.java
        │   ├── NotificationSettingsRequest.java
        │   ├── NotificationRequest.java # Inbound trigger from upstream services
        │   └── NotificationPayload.java # AMQP message body (serialised to JSON)
        │
        ├── repository/                  # Spring Data JPA
        │   ├── UserRepository.java
        │   ├── DeviceRepository.java
        │   ├── NotificationSettingsRepository.java
        │   ├── NotificationTemplateRepository.java
        │   └── NotificationLogRepository.java
        │
        ├── service/
        │   ├── UserService.java                     # CRUD + Redis caching
        │   ├── DeviceService.java                   # Registration + cache eviction
        │   ├── NotificationSettingsService.java     # Opt-in upsert + check
        │   ├── NotificationPayloadBuilderService.java # Template render + cache
        │   └── NotificationService.java             # Main orchestrator
        │
        ├── messaging/
        │   ├── NotificationPublisher.java           # Routes payloads to queues
        │   └── worker/                              # RabbitMQ consumers
        │       ├── IOSNotificationWorker.java       # APNs delivery + retry
        │       ├── AndroidNotificationWorker.java   # FCM delivery + retry
        │       ├── SMSNotificationWorker.java       # SMS delivery + retry
        │       └── EmailNotificationWorker.java     # Email delivery + retry
        │
        ├── controller/
        │   ├── UserController.java
        │   ├── DeviceController.java
        │   ├── NotificationSettingsController.java
        │   ├── NotificationController.java          # /send + /logs endpoints
        │   └── GlobalExceptionHandler.java          # RFC 7807 ProblemDetail errors
        │
        └── thirdparty/                              # Stubbed third-party clients
            ├── APNsService.java                     # Apple Push Notification stub
            ├── FCMService.java                      # Firebase Cloud Messaging stub
            ├── SMSService.java                      # Twilio / Nexmo stub
            ├── EmailService.java                    # SendGrid / Mailchimp stub
            └── ThirdPartyDeliveryException.java     # Triggers worker retry
```

---

## End-to-End Quick Test

Once the service is running (`./mvnw spring-boot:run` with Docker infra up):

```bash
# 1. Create a user
curl -s -X POST http://localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"Alice","email":"alice@example.com","phone":"+14155550001"}' | jq .

# 2. Register an iOS device for the user (userId=1)
curl -s -X POST http://localhost:8080/api/devices \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"token":"my-apns-device-token-xyz","platform":"IOS"}' | jq .

# 3. Send a push notification
curl -s -X POST http://localhost:8080/api/notifications/send \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"channel":"PUSH","templateName":"alert_push","variables":{"message":"Hello!"}}' | jq .

# 4. Send an email
curl -s -X POST http://localhost:8080/api/notifications/send \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"channel":"EMAIL","templateName":"default","variables":{"message":"Your order shipped!"}}' | jq .

# 5. Check the log (use the logId from step 3/4 response)
curl -s http://localhost:8080/api/notifications/logs/1 | jq .

# 6. Trigger a failure + retry (token contains "fail")
curl -s -X POST http://localhost:8080/api/devices \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"token":"fail-this-token","platform":"ANDROID"}' | jq .

curl -s -X POST http://localhost:8080/api/notifications/send \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"channel":"PUSH","templateName":"alert_push","variables":{"message":"Retry me"}}' | jq .
# → Watch logs: worker will retry 5 times, then mark FAILED
```

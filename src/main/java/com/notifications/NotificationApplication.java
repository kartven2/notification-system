package com.notifications;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the Notification System.
 *
 * <p>This is a stateless Spring Boot application that:
 * <ul>
 *   <li>Exposes REST APIs for managing users, devices, notification settings, and sending notifications.</li>
 *   <li>Publishes notification events to RabbitMQ queues (IOS, Android, SMS, Email).</li>
 *   <li>Runs consumer workers that pull from those queues and dispatch to stubbed third-party services.</li>
 *   <li>Caches user, device, and template data in Redis for low-latency reads.</li>
 * </ul>
 * </p>
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}

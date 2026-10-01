# ============================================================
# Multi-stage Dockerfile for notification-system
# ============================================================

# --- Build stage ---
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and POM first for dependency caching
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw

RUN chmod +x mvnw

# Download dependencies (cached as a layer)
RUN ./mvnw dependency:go-offline -B

# Copy source and build
COPY src src
RUN ./mvnw package -DskipTests -B

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Add non-root user for security
RUN addgroup -S notifygroup && adduser -S notifyuser -G notifygroup
USER notifyuser

# Copy the built jar from the builder stage
COPY --from=builder /app/target/notification-system-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]

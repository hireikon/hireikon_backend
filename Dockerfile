# ── Stage 1: Build ────────────────────────────────────────────────
FROM gradle:8.7-jdk17 AS build
WORKDIR /app

# Copy gradle files first for layer caching
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle gradle

# Download dependencies (cached if build files unchanged)
RUN gradle dependencies --no-daemon || true

# Copy source and build
COPY src src
RUN gradle bootJar --no-daemon -x test

# ── Stage 2: Run ──────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Render sets PORT env var — Spring reads server.port
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]

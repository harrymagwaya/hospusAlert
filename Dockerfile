# --- Stage 1: Build ---
FROM gradle:8.5-jdk17-alpine AS build
WORKDIR /app

# Copy only the gradle files first to cache dependencies
COPY build.gradle settings.gradle ./
# Copy the gradle wrapper if you're using it
COPY gradlew ./
COPY gradle ./gradle

# Download dependencies (this layer is cached)
RUN ./gradlew build -x test --continue > /dev/null 2>&1 || true

# Copy source code and build the jar
COPY src ./src
RUN ./gradlew clean bootJar -x test

# --- Stage 2: Runtime ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Security: Run as a non-root user
RUN addgroup -S hospusgroup && adduser -S hospususer -G hospusgroup
USER hospususer

# Copy the executable jar from the build stage
# Note: Spring Boot Gradle plugin names the jar in build/libs/
COPY --from=build /app/build/libs/*.jar app.jar

# JVM Tuning for Containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
# Multi-stage Dockerfile for Spring Boot Discovery Service
# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="helpdesk-team"
LABEL version="0.0.1-SNAPSHOT"
LABEL description="Discovery Service - Eureka Server"

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Copy the JAR from build stage
COPY --from=build /app/target/*.jar app.jar

RUN chown spring:spring app.jar

USER spring:spring

# Expose Eureka port
EXPOSE 8761

# Configure JVM options
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8761/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

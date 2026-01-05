# Multi-stage Dockerfile for Spring Boot Ticket Service
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
LABEL description="Ticket Service - Helpdesk Microservice"

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Copy the JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Create uploads directory for file storage
RUN mkdir -p /app/uploads && chown -R spring:spring /app/uploads

# Switch to non-root user
USER spring:spring

# Expose the service port
EXPOSE 8084

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=40s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8084/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

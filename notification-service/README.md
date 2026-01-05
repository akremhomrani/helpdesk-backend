# Notification Service

## Overview
The Notification Service is a microservice responsible for managing notifications in the helpdesk system. It handles creating, retrieving, and managing notifications for users, particularly for notifying admins when tickets are created or updated.

## Features
- Create notifications
- Retrieve notifications by recipient
- Get unread notifications
- Mark notifications as read
- Delete notifications
- **Kafka integration for async notifications** - Automatically creates notifications when:
  - A ticket is created (notifies admin)
  - A ticket is updated (notifies admin)
  - A ticket is assigned (notifies assigned user)

## Technology Stack
- Java 17
- Spring Boot 3.2.0
- Spring Data MongoDB
- MongoDB
- Spring Cloud Netflix Eureka (Service Discovery)
- Spring Kafka (prepared for future integration)
- Lombok

## API Endpoints

### Health Check
- `GET /api/notifications/health` - Service health check

### Notification Management
- `POST /api/notifications` - Create a new notification
- `GET /api/notifications` - Get all notifications
- `GET /api/notifications/recipient/{recipientId}` - Get notifications by recipient
- `GET /api/notifications/recipient/{recipientId}/unread` - Get unread notifications
- `GET /api/notifications/recipient/{recipientId}/unread/count` - Get unread count
- `PUT /api/notifications/{id}/read` - Mark notification as read
- `PUT /api/notifications/recipient/{recipientId}/read-all` - Mark all as read
- `DELETE /api/notifications/{id}` - Delete a notification

## Configuration

### Database
The service uses MongoDB. Update the following in `application.yml`:
```yaml
spring:
  mongodb:
    uri: mongodb://localhost:27017/helpdesk_notifications
```

### Service Port
The service runs on port `8086` by default.

### Eureka Discovery
Registers with Eureka Server at `http://localhost:8761/eureka/`

## Running the Service

### Prerequisites
- Java 17 or higher
- MongoDB running on port 27017
- **Kafka running on port 9092**
- Eureka Discovery Server running on port 8761

### Run MongoDB Container
```bash
docker run -d -p 27017:27017 --name mongodb mongo:latest
```

### Run Kafka (Docker)
```bash
# Run Zookeeper
docker run -d --name zookeeper -p 2181:2181 zookeeper:latest

# Run Kafka
docker run -d --name kafka -p 9092:9092 \
  -e KAFKA_ZOOKEEPER_CONNECT=host.docker.internal:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  confluentinc/cp-kafka:latest
```

### Run with Maven
```bash
mvn spring-boot:run
```

### Build JAR
```bash
mvn clean package
java -jar target/notification-service-0.0.1-SNAPSHOT.jar
```

### Run with Docker
```bash
docker build -t notification-service .
docker run -p 8084:8084 notification-service
```

## Testing
Use the `api-tests.http` file to test the API endpoints. You can use VS Code with the REST Client extension or IntelliJ IDEA's HTTP Client.

## Notification Types
- `TICKET_CREATED` - When a new ticket is created
- `TICKET_UPDATED` - When a ticket is updated
- `TICKET_ASSIGNED` - When a ticket is assigned
- `TICKET_RESOLVED` - When a ticket is resolved
- `TICKET_CLOSED` - When a ticket is closed
- `SYSTEM_ALERT` - System-level alerts

## Kafka Integration

### Topics Consumed
The notification service listens to the following Kafka topics:

- **ticket-created** - Creates notifications for admins when a new ticket is created
- **ticket-updated** - Creates notifications for admins when a ticket is updated
- **ticket-assigned** - Creates notifications for assigned users when a ticket is assigned to them

### How It Works
1. When a user creates a ticket in the ticket-service, it publishes a `TicketEvent` to the `ticket-created` topic
2. The notification-service consumes this event via `TicketEventConsumer`
3. A notification is automatically created for the admin user
4. The admin can retrieve their notifications via the REST API

### Event Flow
```
Ticket Created (ticket-service)
    ↓
Kafka Topic: ticket-created
    ↓
TicketEventConsumer (notification-service)
    ↓
Notification Created in MongoDB
    ↓
Admin retrieves via GET /notifications/recipient/admin
```

## Future Enhancements
- WebSocket support for real-time push notifications
- Email notifications
- SMS notifications
- Notification templates
- User preferences for notification types

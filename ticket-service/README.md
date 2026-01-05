# Ticket Service

Microservice for managing helpdesk tickets using MongoDB, Spring Boot, Keycloak authentication, and Kafka for inter-service communication.

## Features

- **CRUD Operations**: Create, Read, Update, Delete tickets
- **Ticket Status Management**: PENDING, UNDER_REVIEW, IN_PROGRESS, REJECTED, AWAITING_CLIENT, RESOLVED, CLOSED
- **Assignment & Transfer**: Assign tickets to users and transfer between departments
- **File Attachments**: Upload and download ticket attachments and solutions
- **Ticket History**: Track all changes made to tickets
- **Review & Resubmit**: Support for ticket review and resubmission workflow
- **Keycloak Integration**: JWT-based authentication and authorization
- **MongoDB**: NoSQL database for flexible ticket storage
- **Kafka Events**: Publish ticket events for inter-service communication

## Technology Stack

- **Spring Boot 4.0.1**
- **MongoDB** - NoSQL database
- **Spring Security + OAuth2** - Keycloak integration
- **Spring Kafka** - Event publishing
- **Eureka Client** - Service discovery
- **Lombok** - Reduce boilerplate code

## Prerequisites

- Java 17+
- MongoDB running on `localhost:27017`
- Keycloak running on `localhost:9090`
- Kafka running on `localhost:9092`
- Eureka Server running on `localhost:8761`

## Configuration

Update `src/main/resources/application.yml`:

```yaml
server:
  port: 8084

spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/helpdesk_tickets
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9090/realms/helpdesk-realm
  kafka:
    bootstrap-servers: localhost:9092
```

## API Endpoints

### Tickets

- `POST /api/tickets` - Create a new ticket
- `GET /api/tickets` - Get all tickets
- `GET /api/tickets/{id}` - Get ticket by ID
- `PUT /api/tickets/{id}` - Update ticket
- `DELETE /api/tickets/{id}` - Delete ticket
- `GET /api/tickets/my-tickets` - Get current user's tickets
- `GET /api/tickets/assigned` - Get tickets assigned to current user
- `GET /api/tickets/assigned/{status}` - Get assigned tickets by status

### Ticket Management

- `PATCH /api/tickets/{id}/status` - Update ticket status
- `PATCH /api/tickets/{id}/assign` - Assign ticket to user
- `PATCH /api/tickets/{id}/transfer` - Transfer ticket to another user
- `POST /api/tickets/{id}/review` - Review ticket (accept/reject)
- `POST /api/tickets/{id}/resubmit` - Resubmit rejected ticket

### Files

- `GET /api/tickets/{id}/attachment` - Download ticket attachment
- `POST /api/tickets/{id}/solution` - Upload solution file
- `GET /api/tickets/{id}/solution` - Download solution file

### History

- `GET /api/tickets/{id}/history` - Get ticket history

## Entity Structure

### Ticket
```java
{
  "id": "string",
  "title": "string",
  "description": "string",
  "creatorId": "string",           // Keycloak user ID
  "assignedUserId": "string",       // Keycloak user ID
  "departementId": "string",        // Department ID from department-service
  "status": "UNDER_REVIEW",
  "priority": "MEDIUM",
  "type": "PROBLEM",
  "createdAt": "2025-12-27T10:00:00",
  "updatedAt": "2025-12-27T11:00:00",
  "resolvedAt": null,
  "attachmentPath": "string",
  "solutionPath": "string",
  "feedback": "string",
  "transferredById": "string",
  "transferredFromDepartementId": "string"
}
```

## Kafka Events

The service publishes the following events:

- `ticket-created` - When a new ticket is created
- `ticket-updated` - When a ticket is updated
- `ticket-assigned` - When a ticket is assigned
- `ticket-resolved` - When a ticket is resolved
- `ticket-deleted` - When a ticket is deleted

## Integration Points

### User Service
- TODO: Verify user existence via Kafka/REST
- TODO: Fetch user details (name, email) for display

### Department Service
- TODO: Verify department existence via Kafka/REST
- TODO: Fetch department details for display
- TODO: Get users in a department for auto-assignment

## Running the Service

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Or run the JAR
java -jar target/ticket-service-0.0.1-SNAPSHOT.jar
```

## File Storage

Files are stored in the `uploads/` directory relative to the application root.

## Security

- All endpoints require JWT authentication (Keycloak)
- User ID is extracted from JWT token (`sub` claim)
- Roles can be extracted from `realm_access.roles` for authorization

## TODO

- [ ] Implement UserServiceClient for inter-service communication
- [ ] Implement DepartementServiceClient for inter-service communication
- [ ] Add Kafka event publishing in service methods
- [ ] Implement auto-assignment logic based on department
- [ ] Add email notification service
- [ ] Add more granular role-based access control
- [ ] Add pagination for list endpoints
- [ ] Add filtering and sorting capabilities
- [ ] Add metrics and monitoring (Actuator, Prometheus)

## Notes

- MongoDB is used instead of JPA/PostgreSQL
- Category entity has been replaced with `departementId` (string reference)
- User references use Keycloak user IDs instead of embedded User entities
- Service is designed to communicate with other microservices via Kafka

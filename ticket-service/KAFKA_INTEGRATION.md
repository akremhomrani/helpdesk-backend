# Kafka Department Validation Integration

## Overview

The ticket-service now validates department IDs via Kafka before creating or updating tickets. This ensures that only valid departments (that exist in the department-service MongoDB) can be used.

## How It Works

### Architecture

```
ticket-service                    Kafka                    department-service
      |                             |                              |
      |  1. Send DepartmentRequest  |                              |
      |--------------------------->|                               |
      |    (correlationId,         |   2. Forward request          |
      |     departmentId)          |----------------------------->|
      |                            |                               |
      |                            |   3. Query MongoDB            |
      |                            |   4. Send DepartmentResponse  |
      |                            |<-----------------------------|
      |  5. Receive Response       |                               |
      |<---------------------------|                               |
      |  6. Create ticket or       |                               |
      |     throw error            |                               |
```

### Kafka Topics

- **department-request**: ticket-service sends validation requests
- **department-response**: department-service sends validation responses

### Request-Response Pattern

1. **ticket-service** sends a `DepartmentRequestEvent` with:
   - `correlationId`: UUID to track the request
   - `departmentId`: ID to validate

2. **department-service** listens on `department-request` topic:
   - Queries MongoDB for the department
   - Sends back `DepartmentResponseEvent` with:
     - Same `correlationId`
     - `exists`: boolean
     - `name`, `description`: if found

3. **ticket-service** waits up to 5 seconds for response:
   - If department exists: continues with ticket creation
   - If department not found: throws `InvalidRequestException`
   - If timeout: throws exception

## Files Changed

### ticket-service

- **DepartmentKafkaClient.java**: Kafka client with request-response pattern
- **DepartmentRequestEvent.java**: Request event POJO
- **DepartmentResponseEvent.java**: Response event POJO
- **TicketService.java**: Validates departments in:
  - `createTicket()`
  - `updateTicket()`
  - `resubmitTicket()`
  - `mapToResponse()` (fetches department name)
- **KafkaProducerConfig.java**: Updated to use Object type
- **KafkaConsumerConfig.java**: Consumer configuration
- **KafkaTopicConfig.java**: Added department topics

### department-service

- **DepartmentKafkaListener.java**: Listens to requests and responds
- **DepartmentRequestEvent.java**: Request event POJO
- **DepartmentResponseEvent.java**: Response event POJO
- **application.yml**: Kafka configuration

## Testing

### Prerequisites

1. **Kafka** must be running on `localhost:9092`
2. **MongoDB** must be running on `localhost:27017`
3. **Eureka** should be running on `localhost:8761` (optional)
4. **Keycloak** on `localhost:9090` (or disable security temporarily)

### Start Services

```bash
# Terminal 1: Start department-service
cd department-service
./mvnw spring-boot:run

# Terminal 2: Start ticket-service
cd ticket-service
./mvnw spring-boot:run
```

### Test Valid Department (Should Succeed)

```bash
curl -X POST http://localhost:8084/api/tickets \
  -H "Content-Type: multipart/form-data" \
  -F "title=Test Ticket" \
  -F "description=Testing with valid department" \
  -F "type=TECHNICAL_ISSUE" \
  -F "departementId=694f2cda7ddba34f0f4b5818" \
  -F "priority=HIGH"
```

**Expected Result**: 
- HTTP 200 OK
- Ticket created successfully
- Department name appears in response: `"departementName": "IT Support"`

### Test Invalid Department (Should Fail)

```bash
curl -X POST http://localhost:8084/api/tickets \
  -H "Content-Type: multipart/form-data" \
  -F "title=Test Ticket" \
  -F "description=Testing with invalid department" \
  -F "type=TECHNICAL_ISSUE" \
  -F "departementId=692b603f9bdb41da41eb17ee" \
  -F "priority=HIGH"
```

**Expected Result**:
- HTTP 400 Bad Request
- Error message: `"Department not found: 692b603f9bdb41da41eb17ee"`

### Using api-tests.http

Open `api-tests.http` in VS Code and use REST Client extension:

1. **Valid Department Test**:
   - Find "Create a new ticket with VALID department ID"
   - Click "Send Request"
   - Should succeed

2. **Invalid Department Test**:
   - Find "Create a ticket with INVALID department ID"
   - Click "Send Request"
   - Should fail with error

## Troubleshooting

### Kafka Not Running

**Symptom**: `Failed to validate department. Please try again.`

**Solution**:
```bash
# Windows (using Docker)
docker run -d --name kafka -p 9092:9092 apache/kafka

# Or use Kafka installation
bin\windows\kafka-server-start.bat config\server.properties
```

### MongoDB Empty

**Symptom**: All department IDs fail validation

**Solution**: Check department collection:
```javascript
use helpdesk
db.departement.find()
```

Expected output should include:
```json
{
  "_id": "694f2cda7ddba34f0f4b5818",
  "name": "IT Support",
  "description": "..."
}
```

### Kafka Consumer Not Receiving Messages

**Symptom**: Timeout after 5 seconds

**Check**:
1. Both services are running
2. Topics exist:
   ```bash
   kafka-topics.sh --list --bootstrap-server localhost:9092
   ```
3. Check logs:
   ```
   # ticket-service logs
   Sending department validation request...
   
   # department-service logs
   Received department validation request...
   Sending department response...
   ```

### Service Logs

**ticket-service**:
```
DEBUG c.h.ticket.kafka.DepartmentKafkaClient : Sending department validation request for ID: 694f2cda7ddba34f0f4b5818
DEBUG c.h.ticket.kafka.DepartmentKafkaClient : Received department response: DepartmentResponseEvent(correlationId=..., exists=true, name=IT Support)
INFO  c.h.ticket.service.TicketService       : Department validated: IT Support - Information Technology Support
```

**department-service**:
```
DEBUG c.h.department.kafka.DepartmentKafkaListener : Received department validation request: DepartmentRequestEvent(correlationId=..., departmentId=694f2cda7ddba34f0f4b5818)
DEBUG c.h.department.kafka.DepartmentKafkaListener : Found department: IT Support
DEBUG c.h.department.kafka.DepartmentKafkaListener : Sending department response: true
```

## Benefits

✅ **Real-time validation**: Prevents invalid department IDs from being saved
✅ **Loose coupling**: Services communicate via events, not direct REST calls
✅ **Consistency**: All tickets have valid departments
✅ **Scalability**: Kafka handles high-throughput validation requests
✅ **Resilience**: 5-second timeout prevents hanging requests

## Next Steps

- [ ] Implement similar validation for user IDs (Keycloak integration)
- [ ] Add caching to reduce Kafka calls for frequently-accessed departments
- [ ] Implement retry logic for transient failures
- [ ] Add metrics/monitoring for Kafka communication

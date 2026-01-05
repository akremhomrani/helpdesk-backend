# Kafka Integration Testing Guide

## Prerequisites
Make sure the following services are running:
1. MongoDB (port 27017)
2. Kafka (port 9092)
3. Zookeeper (port 2181)
4. Eureka Server (port 8761)
5. Ticket Service (port 8084)
6. Notification Service (port 8086)
7. API Gateway (port 8080)

## Testing Async Notifications

### Step 1: Start Notification Service
```bash
cd helpdesk-backend/notification-service
mvn spring-boot:run
```

Watch the logs for:
```
Received ticket-created event for ticket ID: xxx
Notification created for admin about new ticket: xxx
```

### Step 2: Create a Ticket
Use the ticket-service API to create a ticket (via API Gateway):

```http
POST http://localhost:8080/tickets
Content-Type: application/json
Authorization: Bearer <your-token>

{
  "title": "Test Ticket for Kafka",
  "description": "Testing async notification",
  "priority": "HIGH",
  "type": "BUG",
  "departementId": "your-department-id"
}
```

### Step 3: Check Notification Service Logs
You should see in notification-service logs:
```
INFO - Received ticket-created event for ticket ID: 67890abc...
INFO - Notification created for admin about new ticket: 67890abc...
```

### Step 4: Retrieve Notifications
Get notifications for admin user:

```http
GET http://localhost:8080/notifications/recipient/admin
```

You should see the notification created from the Kafka event:
```json
[
  {
    "id": "...",
    "recipientId": "admin",
    "title": "New Ticket Created",
    "message": "A new ticket 'Test Ticket for Kafka' has been created by user xyz",
    "type": "TICKET_CREATED",
    "isRead": false,
    "createdAt": "2026-01-04T...",
    "relatedEntityId": "67890abc...",
    "relatedEntityType": "TICKET"
  }
]
```

## Testing Different Event Types

### Test Ticket Update Notification
```http
PUT http://localhost:8080/tickets/{ticketId}
Content-Type: application/json
Authorization: Bearer <your-token>

{
  "title": "Updated Ticket Title",
  "status": "IN_PROGRESS"
}
```

Check for notification with type: `TICKET_UPDATED`

### Test Ticket Assignment Notification
```http
PUT http://localhost:8080/tickets/{ticketId}/assign/{userId}
Authorization: Bearer <your-token>
```

Check notifications for the assigned user:
```http
GET http://localhost:8080/notifications/recipient/{userId}
```

You should see notification with type: `TICKET_ASSIGNED`

## Troubleshooting

### If notifications are not created:

1. **Check Kafka is running:**
   ```bash
   docker ps | grep kafka
   ```

2. **Check notification-service logs for errors:**
   Look for connection errors or deserialization issues

3. **Verify Kafka topic exists:**
   ```bash
   docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
   ```
   Should show: `ticket-created`, `ticket-updated`, `ticket-assigned`

4. **Check if ticket-service is publishing events:**
   Look in ticket-service logs for:
   ```
   Publishing ticket created event for ticket ID: xxx
   ```

5. **Verify consumer group:**
   ```bash
   docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group notification-service-group
   ```

## Success Criteria
✅ Ticket created → Admin receives notification
✅ Ticket updated → Admin receives notification
✅ Ticket assigned → Assigned user receives notification
✅ All notifications stored in MongoDB
✅ Notifications retrievable via REST API

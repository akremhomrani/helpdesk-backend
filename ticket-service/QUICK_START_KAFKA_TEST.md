# Quick Start Guide: Testing Kafka Department Validation

## 🚀 Quick Test Steps

### 1. Start Kafka (if not already running)

```powershell
# Using Docker (recommended)
docker run -d --name kafka -p 9092:9092 apache/kafka:latest

# Verify Kafka is running
docker ps | findstr kafka
```

### 2. Start Services

```powershell
# Terminal 1: Start department-service
cd d:\helpdesk-backend\departement-service
mvn spring-boot:run

# Terminal 2: Start ticket-service
cd d:\helpdesk-backend\ticket-service
mvn spring-boot:run
```

Wait for both services to fully start (look for "Started DepartmentServiceApplication" and "Started TicketServiceApplication")

### 3. Test with Valid Department ✅

```powershell
# Using curl (or use api-tests.http in VS Code)
curl -X POST "http://localhost:8084/api/tickets" `
  -H "Content-Type: multipart/form-data" `
  -F "title=Test Ticket - Valid Department" `
  -F "description=This should work because department exists" `
  -F "type=PROBLEM" `
  -F "departementId=694f2cda7ddba34f0f4b5818" `
  -F "priority=HIGH"
```

**Expected Response** (200 OK):
```json
{
  "id": "...",
  "title": "Test Ticket - Valid Department",
  "departementId": "694f2cda7ddba34f0f4b5818",
  "departementName": "IT Support",
  "status": "UNDER_REVIEW",
  ...
}
```

### 4. Test with Invalid Department ❌

```powershell
curl -X POST "http://localhost:8084/api/tickets" `
  -H "Content-Type: multipart/form-data" `
  -F "title=Test Ticket - Invalid Department" `
  -F "description=This should fail" `
  -F "type=PROBLEM" `
  -F "departementId=692b603f9bdb41da41eb17ee" `
  -F "priority=HIGH"
```

**Expected Response** (400 Bad Request):
```json
{
  "message": "Department not found: 692b603f9bdb41da41eb17ee"
}
```

## 📊 What to Check

### ticket-service logs should show:

```
DEBUG c.h.ticket.kafka.DepartmentKafkaClient : Sending department validation request for ID: 694f2cda7ddba34f0f4b5818
DEBUG c.h.ticket.kafka.DepartmentKafkaClient : Received department response: DepartmentResponseEvent(correlationId=xxx, exists=true, name=IT Support)
INFO  c.h.ticket.service.TicketService       : Department validated: IT Support - Information Technology Support
INFO  c.h.ticket.service.TicketService       : Ticket created successfully
```

### department-service logs should show:

```
DEBUG c.h.department.kafka.DepartmentKafkaListener : Received department validation request: DepartmentRequestEvent(correlationId=xxx, departmentId=694f2cda7ddba34f0f4b5818)
DEBUG c.h.department.kafka.DepartmentKafkaListener : Found department: IT Support
DEBUG c.h.department.kafka.DepartmentKafkaListener : Sending department response: true
```

## 🔍 Verify in MongoDB

```javascript
// Connect to MongoDB
mongosh

use helpdesk_tickets

// Check created tickets
db.tickets.find().pretty()

// You should see tickets ONLY with valid departmentId: "694f2cda7ddba34f0f4b5818"
// The invalid ID "692b603f9bdb41da41eb17ee" should NOT appear
```

## ✅ Success Criteria

- [x] Valid department ID: Ticket created successfully
- [x] Invalid department ID: Returns 400 error
- [x] Department name appears in ticket response
- [x] No tickets in MongoDB with invalid department IDs
- [x] Kafka logs show request/response communication

## 🐛 Common Issues

### Issue 1: "Failed to validate department. Please try again."

**Cause**: Kafka not running or services can't connect

**Fix**:
```powershell
# Check if Kafka is running
docker ps | findstr kafka

# If not, start it
docker run -d --name kafka -p 9092:9092 apache/kafka:latest
```

### Issue 2: "Department not found" for valid ID

**Cause**: Department doesn't exist in MongoDB

**Fix**:
```javascript
// Check departments in MongoDB
use helpdesk
db.departement.find()

// If empty or wrong ID, insert valid department
db.departement.insertOne({
  "_id": "694f2cda7ddba34f0f4b5818",
  "name": "IT Support",
  "description": "Information Technology Support",
  "createdAt": new Date()
})
```

### Issue 3: Timeout after 5 seconds

**Cause**: department-service not running or not consuming Kafka messages

**Fix**:
1. Check department-service is running: `http://localhost:8083/actuator/health`
2. Check logs for Kafka consumer errors
3. Restart department-service

## 🎉 You're Done!

Your ticket service now has real-time department validation via Kafka!

**Before**: Tickets could be created with any department ID, including invalid ones
**After**: Only tickets with valid departments (that exist in the database) can be created

This ensures data integrity across your microservices architecture! 🚀

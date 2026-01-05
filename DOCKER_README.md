# Helpdesk Backend - Docker Deployment Guide

This guide explains how to run all backend services using Docker containers.

## Prerequisites

- Docker Desktop installed and running
- PowerShell (for Windows)
- At least 4GB RAM available for Docker
- Ports available: 8080, 8081, 8082, 8083, 8084, 8086, 8761, 9090, 9092, 27017

## Architecture

The system consists of the following services:

### Microservices
- **Discovery Service** (Eureka) - Port 8761
- **API Gateway** - Port 8080
- **User Service** - Port 8082
- **Department Service** - Port 8083
- **Ticket Service** - Port 8084
- **Notification Service** - Port 8086

### Infrastructure
- **MongoDB** - Port 27017 (Data persistence)
- **Mongo Express** - Port 8081 (MongoDB Web UI)
- **Keycloak** - Port 9090 (Authentication)
- **Kafka** - Port 9092 (Message broker)
- **Zookeeper** - Port 2181 (Kafka coordination)

## Quick Start

### 1. Start All Services

```powershell
.\docker-up.ps1
```

This will:
- Build all Docker images
- Start all containers in the correct order
- Wait for services to be healthy
- Display service URLs

**First run takes 5-10 minutes** as it needs to build all images and download dependencies.

### 2. Check Service Status

```powershell
.\docker-status.ps1
```

This shows:
- Container status
- Health check results
- Which services are responding

### 3. View Logs

```powershell
.\docker-logs.ps1
```

Select a specific service or view all logs.

### 4. Stop Services

```powershell
.\docker-down.ps1
```

Choose whether to keep or remove data volumes.

## Service URLs

After starting, access services at:

| Service | URL | Credentials |
|---------|-----|-------------|
| API Gateway | http://localhost:8080 | - |
| Eureka Dashboard | http://localhost:8761 | - |
| Keycloak Admin | http://localhost:9090 | admin/admin |
| Mongo Express | http://localhost:8081 | - |

## Individual Service URLs

| Service | Health Check |
|---------|--------------|
| Discovery Service | http://localhost:8761/actuator/health |
| User Service | http://localhost:8082/actuator/health |
| Department Service | http://localhost:8083/actuator/health |
| Ticket Service | http://localhost:8084/actuator/health |
| Notification Service | http://localhost:8086/actuator/health |
| API Gateway | http://localhost:8080/actuator/health |

## Manual Docker Commands

### Start all services
```bash
docker-compose up -d
```

### Build and start with rebuild
```bash
docker-compose up --build -d
```

### Stop all services
```bash
docker-compose down
```

### Stop and remove volumes (deletes data)
```bash
docker-compose down -v
```

### View logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f user-service
docker-compose logs -f ticket-service
docker-compose logs -f department-service
```

### Restart a specific service
```bash
docker-compose restart user-service
```

### Rebuild a specific service
```bash
docker-compose up -d --build user-service
```

## MongoDB Databases

The system uses three MongoDB databases:

1. **helpdesk_department** - Department data
2. **helpdesk_tickets** - Ticket data
3. **helpdesk_notifications** - Notification data

Access via Mongo Express: http://localhost:8081

## Troubleshooting

### Services not starting
1. Check Docker Desktop is running
2. Ensure all ports are available
3. Check logs: `.\docker-logs.ps1`

### Out of memory errors
Increase Docker Desktop memory:
- Settings → Resources → Memory → 4GB+

### Container health check failing
Wait 2-3 minutes for services to fully start, especially:
- Keycloak (takes longest)
- Kafka (needs Zookeeper first)
- Microservices (need Discovery Service first)

### Rebuild specific service
```bash
docker-compose up -d --build --force-recreate <service-name>
```

### Clean restart (removes all data)
```bash
docker-compose down -v
docker system prune -a --volumes
.\docker-up.ps1
```

## Development Workflow

### Making code changes

1. Make changes to service code
2. Rebuild specific service:
   ```bash
   docker-compose up -d --build user-service
   ```
3. View logs to verify:
   ```bash
   docker-compose logs -f user-service
   ```

### Testing locally without Docker

To run a single service locally while others run in Docker:

1. Stop the specific service:
   ```bash
   docker-compose stop user-service
   ```

2. Run locally from IDE with profile: `spring.profiles.active=local`

3. Ensure local service connects to Docker infrastructure:
   - MongoDB: localhost:27017
   - Kafka: localhost:9092
   - Eureka: localhost:8761
   - Keycloak: localhost:9090

## Data Persistence

Data is persisted in Docker volumes:
- `mongodb_data` - MongoDB data
- `keycloak_data` - Keycloak configuration
- `kafka_data` - Kafka messages
- `ticket_uploads` - Ticket attachments

To backup data:
```bash
docker run --rm -v helpdesk-backend_mongodb_data:/data -v ${PWD}:/backup alpine tar czf /backup/mongodb-backup.tar.gz /data
```

## Network

All services run on the `helpdesk-network` bridge network and can communicate using service names:
- `mongodb:27017`
- `kafka:29092`
- `keycloak:8080`
- `user-service:8082`
- etc.

## Production Considerations

For production deployment:

1. Change default passwords (Keycloak, MongoDB)
2. Add MongoDB authentication
3. Configure proper resource limits
4. Use external volumes for data
5. Implement proper backup strategy
6. Use Docker Swarm or Kubernetes for orchestration
7. Add monitoring (Prometheus, Grafana)
8. Configure proper logging aggregation

## Support

For issues:
1. Check service logs: `.\docker-logs.ps1`
2. Verify status: `.\docker-status.ps1`
3. Check Docker Desktop resources
4. Ensure all ports are available

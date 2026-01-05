# Docker Deployment Guide for Helpdesk Microservices

This guide will help you build, push, and deploy all microservices using Docker.

## Prerequisites

1. **Docker Desktop** installed and running
2. **Docker Hub account** (create one at https://hub.docker.com)
3. **Git** installed (optional, for cloning)

## Services Overview

| Service | Port | Description |
|---------|------|-------------|
| Discovery Service | 8761 | Eureka Server for service registration |
| User Service | 8082 | User management microservice |
| Department Service | 8083 | Department management microservice |
| API Gateway | 8080 | Gateway for routing requests |

---

## Step 1: Login to Docker Hub

Open a terminal and login to Docker Hub:

```bash
docker login
```

Enter your Docker Hub username and password when prompted.

---

## Step 2: Build and Push Images

**IMPORTANT:** Replace `<your-dockerhub-username>` with your actual Docker Hub username in all commands below!

### 2.1 Discovery Service

```bash
# Navigate to the discovery-server directory
cd discovery-server

# Build the Docker image
docker build -t <your-dockerhub-username>/discovery-service:latest .

# Push to Docker Hub
docker push <your-dockerhub-username>/discovery-service:latest

# Go back to the root directory
cd ..
```

### 2.2 User Service

```bash
# Navigate to the user-service directory
cd user-service

# Build the Docker image
docker build -t <your-dockerhub-username>/user-service:latest .

# Push to Docker Hub
docker push <your-dockerhub-username>/user-service:latest

# Go back to the root directory
cd ..
```

### 2.3 Department Service

```bash
# Navigate to the departement-service directory
cd departement-service

# Build the Docker image
docker build -t <your-dockerhub-username>/department-service:latest .

# Push to Docker Hub
docker push <your-dockerhub-username>/department-service:latest

# Go back to the root directory
cd ..
```

### 2.4 API Gateway

```bash
# Navigate to the API Gateway directory
cd "API Gateway"

# Build the Docker image
docker build -t <your-dockerhub-username>/api-gateway:latest .

# Push to Docker Hub
docker push <your-dockerhub-username>/api-gateway:latest

# Go back to the root directory
cd ..
```

---

## Step 3: Update docker-compose.yml

Before running docker-compose, you need to update the image names:

1. Open `docker-compose.yml` in a text editor
2. Replace ALL occurrences of `<your-dockerhub-username>` with your actual Docker Hub username
3. Save the file

Example:
```yaml
# Before
image: <your-dockerhub-username>/discovery-service:latest

# After (if your username is "johndoe")
image: johndoe/discovery-service:latest
```

---

## Step 4: Run All Services with Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs for all services
docker-compose logs -f

# View logs for a specific service
docker-compose logs -f user-service

# Check service status
docker-compose ps
```

---

## Step 5: Verify Services are Running

After about 1-2 minutes, check if all services are healthy:

1. **Eureka Dashboard**: http://localhost:8761
2. **User Service Health**: http://localhost:8082/actuator/health
3. **Department Service Health**: http://localhost:8083/actuator/health
4. **API Gateway Health**: http://localhost:8080/actuator/health
5. **Keycloak Admin Console**: http://localhost:9090 (admin/admin)

---

## Step 6: Stop Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: This deletes all data!)
docker-compose down -v
```

---

## Troubleshooting

### Container fails to start

```bash
# Check container logs
docker-compose logs <service-name>

# Example:
docker-compose logs user-service
```

### Port already in use

If you get a port conflict error:
1. Stop the service using that port
2. Or change the port in `docker-compose.yml`

### Image not found

Make sure you:
1. Built the image using `docker build`
2. Pushed the image using `docker push`
3. Updated `docker-compose.yml` with your Docker Hub username

### Out of memory errors

Increase Docker Desktop memory:
1. Docker Desktop → Settings → Resources
2. Increase Memory to at least 4GB
3. Click "Apply & Restart"

---

## Quick Reference Commands

```bash
# Build all images at once (run from root directory)
docker build -t <your-dockerhub-username>/discovery-service:latest ./discovery-server
docker build -t <your-dockerhub-username>/user-service:latest ./user-service
docker build -t <your-dockerhub-username>/department-service:latest ./departement-service
docker build -t <your-dockerhub-username>/api-gateway:latest "./API Gateway"

# Push all images at once
docker push <your-dockerhub-username>/discovery-service:latest
docker push <your-dockerhub-username>/user-service:latest
docker push <your-dockerhub-username>/department-service:latest
docker push <your-dockerhub-username>/api-gateway:latest

# Start services
docker-compose up -d

# View all logs
docker-compose logs -f

# Stop services
docker-compose down
```

---

## Production Deployment Tips

1. **Use specific version tags** instead of `latest`
   ```bash
   docker build -t yourusername/user-service:1.0.0 .
   ```

2. **Set resource limits** in docker-compose.yml
   ```yaml
   deploy:
     resources:
       limits:
         cpus: '0.5'
         memory: 512M
   ```

3. **Use environment-specific configs**
   - Create `application-docker.yml` for Docker-specific settings
   - Use Spring profiles: `SPRING_PROFILES_ACTIVE=docker`

4. **Enable HTTPS** for production
   - Use a reverse proxy (nginx/traefik)
   - Add SSL certificates

5. **Monitor your services**
   - Use Prometheus + Grafana
   - Enable Spring Boot Admin
   - Check actuator endpoints regularly

---

## Example: Full Workflow

```bash
# 1. Login to Docker Hub
docker login

# 2. Build all images (replace with your username!)
docker build -t myusername/discovery-service:latest ./discovery-server
docker build -t myusername/user-service:latest ./user-service
docker build -t myusername/department-service:latest ./departement-service
docker build -t myusername/api-gateway:latest "./API Gateway"

# 3. Push all images
docker push myusername/discovery-service:latest
docker push myusername/user-service:latest
docker push myusername/department-service:latest
docker push myusername/api-gateway:latest

# 4. Update docker-compose.yml with "myusername"

# 5. Start services
docker-compose up -d

# 6. Check status
docker-compose ps

# 7. View logs
docker-compose logs -f

# 8. Open Eureka Dashboard
# Open browser to http://localhost:8761

# 9. When done, stop services
docker-compose down
```

---

## Need Help?

- Docker Documentation: https://docs.docker.com
- Docker Hub: https://hub.docker.com
- Spring Boot Docker Guide: https://spring.io/guides/gs/spring-boot-docker/

---

**Created:** December 24, 2025  
**Version:** 1.0.0

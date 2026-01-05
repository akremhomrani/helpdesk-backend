# Docker Build and Push Script for Helpdesk Microservices
# Run this script from the helpdesk-backend root directory

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Building and Pushing Docker Images" -ForegroundColor Cyan
Write-Host "Docker Hub Username: akremhomrani" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Discovery Service
Write-Host "[1/4] Building Discovery Service..." -ForegroundColor Yellow
docker build -t akremhomrani/discovery-service:latest ./discovery-server
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Discovery Service built successfully!" -ForegroundColor Green
    Write-Host "Pushing to Docker Hub..." -ForegroundColor Yellow
    docker push akremhomrani/discovery-service:latest
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Discovery Service pushed successfully!" -ForegroundColor Green
    } else {
        Write-Host "✗ Failed to push Discovery Service" -ForegroundColor Red
    }
} else {
    Write-Host "✗ Failed to build Discovery Service" -ForegroundColor Red
}
Write-Host ""

# 2. User Service
Write-Host "[2/4] Building User Service..." -ForegroundColor Yellow
docker build -t akremhomrani/user-service:latest ./user-service
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ User Service built successfully!" -ForegroundColor Green
    Write-Host "Pushing to Docker Hub..." -ForegroundColor Yellow
    docker push akremhomrani/user-service:latest
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ User Service pushed successfully!" -ForegroundColor Green
    } else {
        Write-Host "✗ Failed to push User Service" -ForegroundColor Red
    }
} else {
    Write-Host "✗ Failed to build User Service" -ForegroundColor Red
}
Write-Host ""

# 3. Department Service
Write-Host "[3/4] Building Department Service..." -ForegroundColor Yellow
docker build -t akremhomrani/department-service:latest ./departement-service
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Department Service built successfully!" -ForegroundColor Green
    Write-Host "Pushing to Docker Hub..." -ForegroundColor Yellow
    docker push akremhomrani/department-service:latest
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Department Service pushed successfully!" -ForegroundColor Green
    } else {
        Write-Host "✗ Failed to push Department Service" -ForegroundColor Red
    }
} else {
    Write-Host "✗ Failed to build Department Service" -ForegroundColor Red
}
Write-Host ""

# 4. API Gateway
Write-Host "[4/4] Building API Gateway..." -ForegroundColor Yellow
docker build -t akremhomrani/api-gateway:latest "./API Gateway"
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ API Gateway built successfully!" -ForegroundColor Green
    Write-Host "Pushing to Docker Hub..." -ForegroundColor Yellow
    docker push akremhomrani/api-gateway:latest
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ API Gateway pushed successfully!" -ForegroundColor Green
    } else {
        Write-Host "✗ Failed to push API Gateway" -ForegroundColor Red
    }
} else {
    Write-Host "✗ Failed to build API Gateway" -ForegroundColor Red
}
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Build and Push Complete!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Check your images on Docker Hub:" -ForegroundColor Green
Write-Host "https://hub.docker.com/u/akremhomrani" -ForegroundColor Cyan
Write-Host ""
Write-Host "To run all services:" -ForegroundColor Green
Write-Host "docker-compose up -d" -ForegroundColor Cyan

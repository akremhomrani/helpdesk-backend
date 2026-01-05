# PowerShell script to build and start all helpdesk backend services

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "   Helpdesk Backend - Docker Setup   " -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Check if Docker is running
Write-Host "Checking Docker status..." -ForegroundColor Yellow
$dockerRunning = docker info 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Docker is not running. Please start Docker Desktop first." -ForegroundColor Red
    exit 1
}
Write-Host "✓ Docker is running" -ForegroundColor Green
Write-Host ""

# Build and start all services
Write-Host "Building and starting all services..." -ForegroundColor Yellow
Write-Host "This may take several minutes on first run..." -ForegroundColor Yellow
Write-Host ""

docker-compose up --build -d

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Green
    Write-Host "   All Services Started Successfully  " -ForegroundColor Green
    Write-Host "=====================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Service URLs:" -ForegroundColor Cyan
    Write-Host "  - API Gateway:         http://localhost:8080" -ForegroundColor White
    Write-Host "  - Eureka Dashboard:    http://localhost:8761" -ForegroundColor White
    Write-Host "  - Keycloak Admin:      http://localhost:9090 (admin/admin)" -ForegroundColor White
    Write-Host "  - Mongo Express:       http://localhost:8081" -ForegroundColor White
    Write-Host "  - User Service:        http://localhost:8082" -ForegroundColor White
    Write-Host "  - Department Service:  http://localhost:8083" -ForegroundColor White
    Write-Host "  - Ticket Service:      http://localhost:8084" -ForegroundColor White
    Write-Host "  - Notification Service: http://localhost:8086" -ForegroundColor White
    Write-Host ""
    Write-Host "To view logs: docker-compose logs -f [service-name]" -ForegroundColor Yellow
    Write-Host "To stop all:  docker-compose down" -ForegroundColor Yellow
    Write-Host "To stop all and remove data: docker-compose down -v" -ForegroundColor Yellow
} else {
    Write-Host ""
    Write-Host "ERROR: Failed to start services" -ForegroundColor Red
    Write-Host "Check logs with: docker-compose logs" -ForegroundColor Yellow
    exit 1
}

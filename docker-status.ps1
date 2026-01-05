# PowerShell script to check status of helpdesk backend services

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "   Helpdesk Backend - Service Status " -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Check if Docker is running
$dockerRunning = docker info 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Docker is not running" -ForegroundColor Red
    exit 1
}

Write-Host "Container Status:" -ForegroundColor Yellow
Write-Host ""

docker-compose ps

Write-Host ""
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Check health status of each service
$services = @(
    @{Name="discovery-service"; Port=8761},
    @{Name="api-gateway"; Port=8080},
    @{Name="user-service"; Port=8082},
    @{Name="department-service"; Port=8083},
    @{Name="ticket-service"; Port=8084},
    @{Name="notification-service"; Port=8086}
)

Write-Host "Health Check:" -ForegroundColor Yellow
foreach ($service in $services) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:$($service.Port)/actuator/health" -TimeoutSec 2 -UseBasicParsing -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            Write-Host "  ✓ $($service.Name): " -NoNewline -ForegroundColor Green
            Write-Host "Healthy" -ForegroundColor Green
        }
    } catch {
        Write-Host "  ✗ $($service.Name): " -NoNewline -ForegroundColor Red
        Write-Host "Not responding" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "For detailed logs: .\docker-logs.ps1" -ForegroundColor Cyan

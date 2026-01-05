# PowerShell script to stop all helpdesk backend services

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "   Stopping Helpdesk Backend Services" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

$removeVolumes = Read-Host "Do you want to remove volumes (data will be lost)? (y/N)"

if ($removeVolumes -eq "y" -or $removeVolumes -eq "Y") {
    Write-Host "Stopping services and removing volumes..." -ForegroundColor Yellow
    docker-compose down -v
    Write-Host "✓ All services stopped and data removed" -ForegroundColor Green
} else {
    Write-Host "Stopping services (keeping data)..." -ForegroundColor Yellow
    docker-compose down
    Write-Host "✓ All services stopped (data preserved)" -ForegroundColor Green
}

Write-Host ""
Write-Host "To start again: .\docker-up.ps1" -ForegroundColor Cyan

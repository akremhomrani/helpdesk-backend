# PowerShell script to view logs from helpdesk backend services

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "   Helpdesk Backend - View Logs      " -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Available services:" -ForegroundColor Yellow
Write-Host "  1. discovery-service" -ForegroundColor White
Write-Host "  2. api-gateway" -ForegroundColor White
Write-Host "  3. user-service" -ForegroundColor White
Write-Host "  4. department-service" -ForegroundColor White
Write-Host "  5. ticket-service" -ForegroundColor White
Write-Host "  6. notification-service" -ForegroundColor White
Write-Host "  7. mongodb" -ForegroundColor White
Write-Host "  8. keycloak" -ForegroundColor White
Write-Host "  9. kafka" -ForegroundColor White
Write-Host "  10. All services" -ForegroundColor White
Write-Host ""

$choice = Read-Host "Select a service (1-10)"

switch ($choice) {
    "1" { docker-compose logs -f discovery-service }
    "2" { docker-compose logs -f api-gateway }
    "3" { docker-compose logs -f user-service }
    "4" { docker-compose logs -f department-service }
    "5" { docker-compose logs -f ticket-service }
    "6" { docker-compose logs -f notification-service }
    "7" { docker-compose logs -f mongodb }
    "8" { docker-compose logs -f keycloak }
    "9" { docker-compose logs -f kafka }
    "10" { docker-compose logs -f }
    default { 
        Write-Host "Invalid choice. Showing all logs..." -ForegroundColor Yellow
        docker-compose logs -f 
    }
}

# Test connectivity script
# Run this to diagnose connection issues

Write-Host "`n=== Testing Backend Connectivity ===" -ForegroundColor Cyan

# Test localhost
Write-Host "`n1. Testing localhost:3000..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:3000" -UseBasicParsing -TimeoutSec 5
    Write-Host "   OK - Localhost works! Status: $($response.StatusCode)" -ForegroundColor Green
} catch {
    Write-Host "   ERROR - Localhost failed: $_" -ForegroundColor Red
}

# Test network IP
Write-Host "`n2. Testing 192.168.1.190:3000..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://192.168.1.190:3000" -UseBasicParsing -TimeoutSec 5
    Write-Host "   OK - Network IP works! Status: $($response.StatusCode)" -ForegroundColor Green
} catch {
    Write-Host "   ERROR - Network IP failed: $_" -ForegroundColor Red
}

# Check what's listening
Write-Host "`n3. Checking what is listening on port 3000..." -ForegroundColor Yellow
$listening = netstat -ano | Select-String ":3000.*LISTENING"
if ($listening) {
    Write-Host "   OK - Server is listening:" -ForegroundColor Green
    Write-Host "   $listening"
} else {
    Write-Host "   ERROR - No server listening on port 3000!" -ForegroundColor Red
}

# Get all IPv4 addresses
Write-Host "`n4. Your IPv4 addresses:" -ForegroundColor Yellow
Get-NetIPAddress -AddressFamily IPv4 | Where-Object {$_.IPAddress -notlike "127.*"} | Select-Object IPAddress, InterfaceAlias | ForEach-Object {
    Write-Host "   - $($_.IPAddress) on $($_.InterfaceAlias)" -ForegroundColor Cyan
}

# Instructions
Write-Host "`n=== Instructions for Phone ===" -ForegroundColor Cyan
Write-Host "On your phone, try these URLs:" -ForegroundColor White
Write-Host "   1. http://192.168.1.190:3000" -ForegroundColor Green
Write-Host "   2. http://192.168.1.190:3000/api" -ForegroundColor Green
Write-Host "`nMake sure:" -ForegroundColor White
Write-Host "   - Phone is on same WiFi network" -ForegroundColor Yellow
Write-Host "   - Using http not https" -ForegroundColor Yellow
Write-Host "   - Phone IP starts with 192.168.1" -ForegroundColor Yellow

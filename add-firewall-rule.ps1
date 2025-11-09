# Run this script as Administrator
# Right-click PowerShell -> "Run as Administrator"
# Then run: .\add-firewall-rule.ps1

New-NetFirewallRule -DisplayName "NestJS Backend Port 3000" -Direction Inbound -LocalPort 3000 -Protocol TCP -Action Allow

Write-Host "Firewall rule added successfully!" -ForegroundColor Green
Write-Host "You can now access the backend from your phone at: http://192.168.1.190:3000/api" -ForegroundColor Cyan

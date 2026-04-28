# Kill anything on 8081
$c = Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($null -ne $c) {
    Stop-Process -Id $c.OwningProcess -Force
    Start-Sleep -Seconds 2
}

# Start backend
Set-Location C:\Coding\loyalty\backend
.\gradlew.bat bootRun --args="--spring.profiles.active=local --server.port=8081"

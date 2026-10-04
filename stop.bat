@echo off
setlocal
cd /d "%~dp0"
title Stop Online Food Ordering System
echo Menghentikan semua proses aplikasi Online Food Ordering System...
powershell -NoProfile -Command "Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match 'online-food-ordering-1.0.0.jar' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force; Write-Output ('Stopped PID ' + $_.ProcessId) }"
echo.
echo Selesai. Port 8080 sudah dilepas.
pause
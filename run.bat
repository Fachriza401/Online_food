@echo off
setlocal
cd /d "%~dp0"
title Online Food Ordering System - RM KAK ROS
chcp 65001 >nul

echo ==================================================
echo   ONLINE FOOD ORDERING SYSTEM - RM KAK ROS
echo ==================================================
echo.

netstat -ano | findstr /r /c:":8080 .*LISTENING" >nul
if %errorlevel%==0 (
    echo  [INFO] Aplikasi sepertinya SUDAH BERJALAN.
    echo  Buka  http://localhost:8080  di browser Anda.
    echo  (Jika tidak terbuka, jalankan stop.bat lalu ulangi run.bat)
    echo.
    pause
    exit /b 0
)

if not exist "target\online-food-ordering-1.0.0.jar" (
    echo  [INFO] Jar belum ada. Membangun dulu dengan Maven Wrapper...
    call mvnw.cmd -q -DskipTests package
    if errorlevel 1 (
        echo  [ERROR] Build gagal. Pastikan JDK 21 terpasang.
        pause
        exit /b 1
    )
)

echo  Website  : http://localhost:8080
echo  H2 SQL   : http://localhost:8080/h2-console
echo.
echo  Akun demo:
echo    Admin    : admin / admin123  (panel: http://localhost:8080/admin)
echo    Pelanggan: customer / customer123
echo.
echo  Tekan [Ctrl+C] untuk menghentikan server.
echo ==================================================
echo.
java -jar target\online-food-ordering-1.0.0.jar
pause
@echo off
chcp 65001 >nul
title GrizmTowny Derleme Scripti
echo ========================================================
echo   GrizmMC Network - GrizmTowny (Paper 1.21.4) Plugin
echo ========================================================
echo.

cd /d "%~dp0"

if "%JAVA_HOME%"=="" (
    if exist "%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-21.0.12.101-hotspot" set "JAVA_HOME=%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
    if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.3\jbr" set "JAVA_HOME=C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.3\jbr"
    if exist "C:\Program Files\Java\jdk-21" set "JAVA_HOME=C:\Program Files\Java\jdk-21"
    if exist "C:\Program Files\Eclipse Adoptium\jdk-21" set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21"
    if exist "D:\Aesir Launcher\runtime" set "JAVA_HOME=D:\Aesir Launcher\runtime"
)

set "MAVEN_CMD=mvn"
if exist "..\Grizm SkyBlock\maven\apache-maven-3.9.6\bin\mvn.cmd" (
    set "MAVEN_CMD=..\Grizm SkyBlock\maven\apache-maven-3.9.6\bin\mvn.cmd"
)

echo [1/2] Maven ile paketleniyor...
call "%MAVEN_CMD%" clean package

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [HATA] Derleme basarisiz oldu!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [2/2] Derleme basarili!
echo Cikti dosyasi: target\GrizmTowny-1.0.0.jar
echo.
echo ========================================================
echo   Derleme tamamlandi!
echo ========================================================
pause

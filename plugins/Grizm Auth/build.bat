@echo off
chcp 65001 >nul
title GrizmAuth Derleme Scripti
echo ========================================================
echo   GrizmMC Network - GrizmAuth Plugin Derleme
echo ========================================================
echo.

cd /d "%~dp0"

if "%JAVA_HOME%"=="" (
    if exist "C:\Program Files\Java\jdk-21" set "JAVA_HOME=C:\Program Files\Java\jdk-21"
    if exist "C:\Program Files\Eclipse Adoptium\jdk-21" set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21"
    if exist "C:\Program Files\Java\jdk-17" set "JAVA_HOME=C:\Program Files\Java\jdk-17"
)

set "MAVEN_CMD=mvn"
if exist "..\Grizm SkyBlock\maven\apache-maven-3.9.6\bin\mvn.cmd" (
    set "MAVEN_CMD=..\Grizm SkyBlock\maven\apache-maven-3.9.6\bin\mvn.cmd"
)

echo [1/2] Maven ile derleniyor...
call "%MAVEN_CMD%" clean package

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [HATA] Derleme basarisiz oldu!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [2/2] Derleme basarili!
echo Cikti dosyasi: target\GrizmAuth-1.0.0.jar

if exist "..\..\server\SONYEDEK\lobby\plugins" (
    echo Lobby sunucu plugins klasorune kopyalaniyor...
    copy /y "target\GrizmAuth-1.0.0.jar" "..\..\server\SONYEDEK\lobby\plugins\GrizmAuth-1.0.0.jar"
    echo Kopyalandi!
)

echo.
echo ========================================================
echo   Derleme tamamlandi!
echo ========================================================
pause

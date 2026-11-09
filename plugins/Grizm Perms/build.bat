@echo off
chcp 65001 >nul
echo ===================================================
echo   GrizmPerms 1.0.0 Derleme Araci
echo ===================================================
set "JAVA_HOME=C:\Users\yusuf\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
set "MVN_BIN=d:\Projeler\GrizmMc\plugins\Grizm SkyBlock\maven\apache-maven-3.9.6\bin\mvn.cmd"

call "%MVN_BIN%" clean package
if %ERRORLEVEL% EQU 0 (
    echo [BASARILI] target\GrizmPerms-1.0.0.jar olusturuldu!
) else (
    echo [HATA] Derleme basarisiz oldu!
)
pause


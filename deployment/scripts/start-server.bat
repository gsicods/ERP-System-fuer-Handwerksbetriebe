@echo off
cd /d "%~dp0"

REM Java-Verfuegbarkeit pruefen
where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java nicht gefunden. Bitte Java installieren und PATH setzen.
    pause
    exit /b 1
)

REM JAR dynamisch finden: zuerst Produktionspfad (nach Name sortiert), dann Dev-target als Fallback
set JAR_FILE=

REM 1. Produktionspfad pruefen (alphabetisch absteigend = hoechste Versionsnummer zuerst)
for /f "delims=" %%f in ('dir /b /o-n "C:\Kalkulationsprogramm\*.jar" 2^>nul') do (
    set JAR_FILE=C:\Kalkulationsprogramm\%%f
)

REM 2. Dev-Fallback: target im Repo-Root (zwei Ebenen hoch von deployment\scripts\)
if not defined JAR_FILE (
    for /f "delims=" %%f in ('dir /b /o-n "..\..\target\*.jar" 2^>nul') do (
        set JAR_FILE=..\..\target\%%f
    )
)

if not defined JAR_FILE (
    echo [ERROR] Keine JAR-Datei gefunden. Bitte zuerst UPDATE.bat ausfuehren.
    pause
    exit /b 1
)

echo Starte Kalkulationsprogramm...
echo JAR: %JAR_FILE%
start "Kalkulationsprogramm Server" /min cmd /k java -jar "%JAR_FILE%"
timeout /t 3 >nul
start "" "http://localhost:8080"

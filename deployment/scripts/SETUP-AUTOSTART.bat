@echo off
REM Richtet den automatischen Serverstart nach Neustart ein.
REM Benoetigt Administrator-Rechte - startet automatisch mit Elevation.

net session >nul 2>&1
if errorlevel 1 (
    echo Starte als Administrator...
    powershell -Command "Start-Process '%~f0' -Verb RunAs"
    exit /b
)

echo Richtet den automatischen Serverstart nach Neustart ein...
echo.
powershell.exe -ExecutionPolicy Bypass -File "%~dp0setup-autostart.ps1"

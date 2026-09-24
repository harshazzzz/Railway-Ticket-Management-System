@echo off
cd /d "%~dp0"
if exist "dist\railway-reservations-1.0.0.jar" (
  java -jar "dist\railway-reservations-1.0.0.jar" --demo
) else (
  java -jar "target\railway-reservations-1.0.0.jar" --demo
)
if errorlevel 1 pause

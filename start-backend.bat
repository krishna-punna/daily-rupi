@echo off
rem Starts the Daily Rupi Spring Boot backend so the phone app can reach it.
rem Double-click this file, or run it from any folder. It always switches to the
rem backend folder next to it (for example D:\NEW_GIT\daily-rupi\backend).
rem
rem DB_USERNAME and DB_PASSWORD are not set here. Set them once as Windows
rem user environment variables (see README.md) before starting.

cd /d "%~dp0backend"

rem The app logs in over plain http, so the session cookie must not be Secure.
set COOKIE_SECURE=false

call mvn spring-boot:run -Dspring-boot.run.arguments="--server.address=0.0.0.0"

pause

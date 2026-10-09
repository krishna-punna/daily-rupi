@echo off
rem Starts the Daily Rupi Angular screens on port 4200, reachable from other
rem devices on the same Wi-Fi at http://<this PC's IP>:4200.
rem Double-click this file, or run it from any folder. It always switches to the
rem frontend folder next to it (for example D:\NEW_GIT\daily-rupi\frontend).
rem Start the backend first (start-backend.bat); /api calls are forwarded to it.

cd /d "%~dp0frontend"

rem First run only: install the packages.
if not exist node_modules call npm install

call npm start -- --host 0.0.0.0

pause

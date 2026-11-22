@echo off
setlocal
cd /d "%~dp0"
echo Starting Student Expense Tracker Java API...
powershell -NoExit -ExecutionPolicy Bypass -File "%~dp0run-api.ps1"
endlocal

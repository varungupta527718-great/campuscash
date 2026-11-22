@echo off
setlocal
cd /d "%~dp0"

echo Starting Student Expense Tracker...

start "Student Expense Tracker - Java API" powershell -NoExit -ExecutionPolicy Bypass -File "%~dp0run-api.ps1"

if not exist "%~dp0frontend\node_modules" (
  echo React packages are missing. Installing them first...
  pushd "%~dp0frontend"
  call npm install
  if errorlevel 1 (
    echo Could not install frontend packages. Check your internet connection and try again.
    popd
    pause
    exit /b 1
  )
  popd
)

start "Student Expense Tracker - React UI" cmd /k "cd /d ""%~dp0frontend"" && npm run dev -- --host 127.0.0.1"

timeout /t 3 /nobreak >nul
start "" http://127.0.0.1:5173
echo Backend and frontend are starting in separate windows.
endlocal

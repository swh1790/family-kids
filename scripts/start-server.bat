@echo off
set "BASE=C:\Users\PC\WorkBuddy\2026-10-17-22-41-15"
set "JAVA=%BASE%\.tools\jdk-17.0.13+11\bin\java.exe"
set "JAR=%BASE%\family-kids\target\family-kids-1.0.0.jar"
set "LOG=%BASE%\app-run.log"

if not exist "%JAR%" (
  echo [ERROR] JAR not found: %JAR%
  exit /b 1
)

echo Starting family-kids on port 8090 ...
start "family-kids" /B "%JAVA%" -Dfile.encoding=UTF-8 -jar "%JAR%" --server.port=8090 > "%LOG%" 2>&1
echo Started. Log: %LOG%
exit /b 0

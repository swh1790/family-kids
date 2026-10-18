@echo off
chcp 65001 >nul
setlocal

set "JAVA_HOME=C:\Users\PC\WorkBuddy\2026-10-17-22-41-15\.tools\jdk-17.0.13+11"
set "PGJAR=C:\Users\PC\.m2\repository\org\postgresql\postgresql\42.7.4\postgresql-42.7.4.jar"

set "DB_HOST=localhost"
set "DB_PORT=5432"
set "DB_NAME=ontobricks"
set "DB_SCHEMA=family_kids"
set "DB_USER=ontobricks"
set "DB_PASSWORD=devpass"

cd /d "C:\Users\PC\WorkBuddy\2026-10-17-22-41-15\family-kids\scripts\dbinit"

echo 正在初始化数据库 schema=%DB_SCHEMA% ...
"%JAVA_HOME%\bin\java.exe" -cp "%PGJAR%" DbInit.java ^
  "jdbc:postgresql://%DB_HOST%:%DB_PORT%/%DB_NAME%" %DB_USER% %DB_PASSWORD% ^
  "..\..\src\main\resources\db\schema.sql" "..\..\src\main\resources\db\data.sql"

echo.
pause

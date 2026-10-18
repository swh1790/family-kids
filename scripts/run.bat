@echo off
chcp 65001 >nul
setlocal

set "JAVA_HOME=C:\Users\PC\WorkBuddy\2026-10-17-22-41-15\.tools\jdk-17.0.13+11"
set "MAVEN_HOME=C:\Users\PC\WorkBuddy\2026-10-17-22-41-15\.tools\apache-maven-3.9.9"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

set "DB_HOST=localhost"
set "DB_PORT=5432"
set "DB_NAME=ontobricks"
set "DB_SCHEMA=family_kids"
set "DB_USER=ontobricks"
set "DB_PASSWORD=devpass"

rem 宿主机可能注入 SERVER__PORT 环境变量（Spring 会把它解析成 server.port），这里先清掉
set "SERVER__PORT="
set "APP_PORT=8090"

cd /d "C:\Users\PC\WorkBuddy\2026-10-17-22-41-15\family-kids"

echo.
echo  家庭成长系统启动中...  访问 http://localhost:%APP_PORT%/
echo  孩子端：http://localhost:%APP_PORT%/kid/1   客厅看板：http://localhost:%APP_PORT%/board
echo  （Ctrl+C 停止）
echo.

if exist "target\family-kids-1.0.0.jar" (
  "%JAVA_HOME%\bin\java.exe" -Dfile.encoding=UTF-8 -jar target\family-kids-1.0.0.jar --server.port=%APP_PORT%
) else (
  "%MAVEN_HOME%\bin\mvn.cmd" spring-boot:run -Dspring-boot.run.arguments=--server.port=%APP_PORT%
)

pause

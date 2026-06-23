@echo off
setlocal
set DIR=%~dp0
if exist "%DIR%runtime\bin\java.exe" (
  set JAVA_BIN=%DIR%runtime\bin\java.exe
) else if defined JAVA_HOME (
  if exist "%JAVA_HOME%\bin\java.exe" set JAVA_BIN=%JAVA_HOME%\bin\java.exe
) else (
  set JAVA_BIN=java
)
"%JAVA_BIN%" -cp "%DIR%app-core.jar;%DIR%lib/*" com.dbsyncstudio.core.DbSyncStudioApplication %*
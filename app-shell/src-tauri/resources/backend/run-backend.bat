@echo off
setlocal
set DIR=%~dp0
if defined JAVA_HOME (
  set JAVA_BIN=%JAVA_HOME%\bin\java.exe
) else (
  set JAVA_BIN=java
)
"%JAVA_BIN%" -cp "%DIR%app-core.jar;%DIR%lib/*" com.dbsyncstudio.core.DbSyncStudioApplication %*
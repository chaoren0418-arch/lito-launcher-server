@echo off
setlocal
if "%~2"=="" (
  echo Usage: run-experiment.cmd "fresh-run-directory" "java-17-executable"
  exit /b 2
)
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0verify-run.ps1" -RunDirectory "%~1"
if errorlevel 1 exit /b 1
pushd "%~1"
if errorlevel 1 exit /b 1
"%~2" -Xms256m -Xmx1g -XX:ActiveProcessorCount=2 "-Djava.io.tmpdir=%CD%\tmp" -jar LiTo381-SharedResources-candidate.jar config\experiment.properties > evidence\server-console.txt 2>&1
set "SERVER_EXIT=%ERRORLEVEL%"
(echo %SERVER_EXIT%)>evidence\server-exit-code.txt
popd
exit /b %SERVER_EXIT%

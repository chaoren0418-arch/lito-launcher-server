@echo off
setlocal
rem See ../issue-284.md for the lease, backup, P24Lab lifecycle and timeout requirements.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-experiment.ps1" %*
exit /b %errorlevel%

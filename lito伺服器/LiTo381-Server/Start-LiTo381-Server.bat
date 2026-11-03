@echo off
setlocal EnableExtensions
chcp 65001 >nul
cd /d "%~dp0"
title LiTo Dedicated Server
set "LITO_MODE=standard"
if "%~1"=="" goto run
if /i "%~1"=="standard" goto run
if /i "%~1"=="--check" set "LITO_MODE=check"
if /i "%~1"=="--smoke" set "LITO_MODE=smoke"
if "%LITO_MODE%"=="standard" exit /b 2
:run
if not "%~2"=="" exit /b 2
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\Start-LiToServer.ps1" -Mode "%LITO_MODE%"
set "LITO_EXIT=%ERRORLEVEL%"
if not "%LITO_EXIT%"=="0" if "%~1"=="" pause
exit /b %LITO_EXIT%

@echo off
title local-agent (Codex)
:top
chcp 950 >nul
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0local-agent.ps1" -Once
chcp 950 >nul
timeout /t 60 /nobreak >nul
goto top

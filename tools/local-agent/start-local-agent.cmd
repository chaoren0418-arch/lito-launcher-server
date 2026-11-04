@echo off
title local-agent (Codex)
:top
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0local-agent.ps1" -Once
timeout /t 60 /nobreak >nul
goto top

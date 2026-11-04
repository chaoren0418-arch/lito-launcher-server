@echo off
title local-agent (Codex)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0local-agent.ps1" %*
pause

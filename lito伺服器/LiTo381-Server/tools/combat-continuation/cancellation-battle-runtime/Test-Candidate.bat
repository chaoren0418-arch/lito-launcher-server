@echo off
chcp 65001 >nul
python -B -X utf8 "%~dp0launch_candidate.py"
pause

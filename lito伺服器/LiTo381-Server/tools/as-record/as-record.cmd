@echo off
if defined PYTHON (
  "%PYTHON%" "%~dp0as_record.py" %*
) else (
  python "%~dp0as_record.py" %*
)
exit /b %errorlevel%

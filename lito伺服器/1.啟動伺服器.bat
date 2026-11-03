@echo off
setlocal EnableExtensions
call "%~dp0LiTo381-Server\Start-LiTo381-Server.bat" %*
exit /b %ERRORLEVEL%

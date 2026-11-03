@echo off
cd /d "%~dp0"
call "C:\Program Files\Microsoft Visual Studio\18\Insiders\VC\Auxiliary\Build\vcvars32.bat" >nul
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc /W4 live_stats_test.cpp /Felive_stats_test.exe
if errorlevel 1 exit /b 1
live_stats_test.exe ..\server-patches\repair-040\build\test-output\live-packet.bin

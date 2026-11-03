@echo off
cd /d "%~dp0"
call "C:\Program Files\Microsoft Visual Studio\18\Insiders\VC\Auxiliary\Build\vcvars32.bat" >nul
if errorlevel 1 exit /b 1
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc /W4 caption_drag_test.cpp /Fecaption_drag_test.exe
if errorlevel 1 exit /b 1
caption_drag_test.exe

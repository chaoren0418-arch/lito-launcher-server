@echo off
call "C:\Program Files\Microsoft Visual Studio\18\Insiders\VC\Auxiliary\Build\vcvars32.bat" >nul
if errorlevel 1 exit /b 1
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc /W4 forced_combat_test.cpp /Foforced_combat_test.obj /Feforced_combat_test.exe
if errorlevel 1 exit /b 1
forced_combat_test.exe

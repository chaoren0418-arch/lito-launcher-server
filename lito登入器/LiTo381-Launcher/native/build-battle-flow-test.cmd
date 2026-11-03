@echo off
call "C:\Program Files\Microsoft Visual Studio\18\Insiders\VC\Auxiliary\Build\vcvars32.bat" >nul
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc /W4 battle_result_return_test.cpp /Febattle_result_return_test.exe
if errorlevel 1 exit /b 1
battle_result_return_test.exe
if errorlevel 1 exit /b 1
call build.cmd

@echo off
cd /d "%~dp0"
call "C:\Program Files\Microsoft Visual Studio\18\Insiders\VC\Auxiliary\Build\vcvars32.bat" >nul
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc /W4 source_login_order_test.cpp /Fesource_login_order_test.exe /link user32.lib /BASE:0x10000000 /DYNAMICBASE:NO
if errorlevel 1 exit /b 1
source_login_order_test.exe
if errorlevel 1 exit /b 1
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc /W4 character_source_return_test.cpp /Fecharacter_source_return_test.exe
if errorlevel 1 exit /b 1
character_source_return_test.exe

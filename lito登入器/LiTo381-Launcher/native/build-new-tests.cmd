@echo off
cd /d "%~dp0"
call "C:\Program Files\Microsoft Visual Studio\18\Insiders\VC\Auxiliary\Build\vcvars32.bat" >nul
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc summary_input_test.cpp /Fesummary_input_test.exe
if errorlevel 1 exit /b 1
summary_input_test.exe
if errorlevel 1 exit /b 1
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc item_details_test.cpp /Feitem_details_test.exe
if errorlevel 1 exit /b 1
item_details_test.exe

if errorlevel 1 exit /b 1
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc item_details_native_test.cpp /Feitem_details_native_test.exe /link /MAP:item_details_native_test.map /BASE:0x10000000 /DYNAMICBASE:NO
if errorlevel 1 exit /b 1
item_details_native_test.exe

@echo off
cd /d "%~dp0"
if not exist "..\build\native-current" mkdir "..\build\native-current"
call "C:\Program Files\Microsoft Visual Studio\18\Insiders\VC\Auxiliary\Build\vcvars32.bat" >nul
if errorlevel 1 exit /b 1
cl /nologo /utf-8 /std:c++17 /LD /MT /O2 /EHsc /W4 /DUNICODE /D_UNICODE /Brepro ddraw_proxy.cpp /Fo../build/native-current/ddraw.obj /Fe../build/native-current/litoddraw.dll /link /Brepro /MAP:../build/native-current/ddraw.map /DEF:ddraw.def user32.lib gdi32.lib dxguid.lib
if errorlevel 1 exit /b 1
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc /W4 /WX /Brepro status_catalog_test.cpp /Fo../build/native-current/status_catalog_test.obj /Fe../build/native-current/status_catalog_test.exe
if errorlevel 1 exit /b 1
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc /W4 /WX /Brepro status_lifecycle_test.cpp /Fo../build/native-current/status_lifecycle_test.obj /Fe../build/native-current/status_lifecycle_test.exe
if errorlevel 1 exit /b 1
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc /W4 /DUNICODE /D_UNICODE /Brepro status_install_test.cpp /Fo../build/native-current/status_install_test.obj /Fe../build/native-current/status_install_test.exe /link /BASE:0x10000 /FIXED /MANIFEST:EMBED /MANIFESTUAC:"level='asInvoker' uiAccess='false'" user32.lib gdi32.lib dxguid.lib
if errorlevel 1 exit /b 1

copy /y "..\app\compat\shared_status.bin" "..\build\native-current\shared_status.bin" >nul
if errorlevel 1 exit /b 1
copy /y "shared-resources\native-status-table.bin" "..\build\native-current\native-status-table.bin" >nul
if errorlevel 1 exit /b 1

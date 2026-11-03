@echo off
call "C:\Program Files\Microsoft Visual Studio\18\Insiders\VC\Auxiliary\Build\vcvars32.bat" >nul
if errorlevel 1 exit /b 1
cl /nologo /utf-8 /std:c++17 /LD /MT /O2 /EHsc /W4 /DUNICODE /D_UNICODE /Brepro ddraw_proxy.cpp /Fo../candidate/ddraw.obj /Fe../candidate/litoddraw.dll /link /Brepro /MAP:../candidate/ddraw.map /DEF:ddraw.def user32.lib gdi32.lib dxguid.lib
if errorlevel 1 exit /b 1
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc /W4 /WX /Brepro status_catalog_test.cpp /Fo../candidate/status_catalog_test.obj /Fe../candidate/status_catalog_test.exe
if errorlevel 1 exit /b 1
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc /W4 /WX /Brepro status_lifecycle_test.cpp /Fo../candidate/status_lifecycle_test.obj /Fe../candidate/status_lifecycle_test.exe
if errorlevel 1 exit /b 1
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc /W4 /DUNICODE /D_UNICODE /Brepro status_install_test.cpp /Fo../candidate/status_install_test.obj /Fe../candidate/status_install_test.exe /link /BASE:0x10000 /FIXED /MANIFEST:EMBED /MANIFESTUAC:"level='asInvoker' uiAccess='false'" user32.lib gdi32.lib dxguid.lib
if errorlevel 1 exit /b 1

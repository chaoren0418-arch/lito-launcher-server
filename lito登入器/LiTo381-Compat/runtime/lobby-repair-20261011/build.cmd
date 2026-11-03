@echo off
call "C:\Program Files\Microsoft Visual Studio\18\Insiders\VC\Auxiliary\Build\vcvars32.bat" >nul
if errorlevel 1 exit /b %errorlevel%
cl /nologo /utf-8 /std:c++17 /LD /MT /O2 /EHsc /W4 /DUNICODE /D_UNICODE ddraw_proxy.cpp /Foruntime\lobby-repair-20261011\ddraw_proxy.obj /Feruntime\lobby-repair-20261011\ddraw.dll /link /DEF:ddraw.def /MAP:runtime\lobby-repair-20261011\ddraw.map user32.lib gdi32.lib dxguid.lib
if errorlevel 1 exit /b %errorlevel%
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc /DUNICODE /D_UNICODE /DBASELINE runtime\lobby-repair-20261011\lock_order_test.cpp /Foruntime\lobby-repair-20261011\lock-old.obj /Feruntime\lobby-repair-20261011\lock-old.exe /link /DEF:runtime\lobby-repair-20261011\test_exports.def user32.lib gdi32.lib dxguid.lib
if errorlevel 1 exit /b %errorlevel%
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc /DUNICODE /D_UNICODE runtime\lobby-repair-20261011\lock_order_test.cpp /Foruntime\lobby-repair-20261011\lock-new.obj /Feruntime\lobby-repair-20261011\lock-new.exe /link /DEF:runtime\lobby-repair-20261011\test_exports.def user32.lib gdi32.lib dxguid.lib

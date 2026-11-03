@echo off
call "C:\Program Files\Microsoft Visual Studio\18\Insiders\VC\Auxiliary\Build\vcvars32.bat" >nul
cl /nologo /utf-8 /std:c++17 /LD /MT /O2 /EHsc /W4 /DUNICODE /D_UNICODE /DLITO_SOURCE_UI_TEST ddraw_proxy.cpp /Foddraw-source-test.obj /Feddraw-source-test.dll /link /DEF:ddraw.def user32.lib gdi32.lib dxguid.lib

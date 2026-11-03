@echo off
setlocal
cd /d "%~dp0"
call "C:\Program Files\Microsoft Visual Studio\18\Insiders\VC\Auxiliary\Build\vcvars32.bat" >nul
if not "%errorlevel%"=="0" exit /b %errorlevel%
cl /nologo /utf-8 /std:c++17 /LD /MT /O2 /EHsc /W4 /DUNICODE /D_UNICODE ddraw_proxy.cpp /Foddraw_proxy.obj /Feddraw.dll /link /DEF:ddraw.def user32.lib gdi32.lib dxguid.lib
if not "%errorlevel%"=="0" exit /b %errorlevel%
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc /W4 toolbar_test.cpp /Fetoolbar_test.exe
if not "%errorlevel%"=="0" exit /b %errorlevel%
toolbar_test.exe
if not "%errorlevel%"=="0" exit /b %errorlevel%
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc /W4 edit_canvas_test.cpp /Feedit_canvas_test.exe /link /BASE:0x10000000 /DYNAMICBASE:NO user32.lib gdi32.lib
if not "%errorlevel%"=="0" exit /b %errorlevel%
edit_canvas_test.exe
if not "%errorlevel%"=="0" exit /b %errorlevel%
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc /W4 selector_return_test.cpp /Feselector_return_test.exe /link /BASE:0x10000000 /DYNAMICBASE:NO user32.lib
if not "%errorlevel%"=="0" exit /b %errorlevel%
selector_return_test.exe
if not "%errorlevel%"=="0" exit /b %errorlevel%
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc /W4 exit_dispatch_test.cpp /Feexit_dispatch_test.exe /link /MANIFEST:EMBED /MANIFESTUAC:"level='asInvoker' uiAccess='false'"
if not "%errorlevel%"=="0" exit /b %errorlevel%
exit_dispatch_test.exe
if not "%errorlevel%"=="0" exit /b %errorlevel%
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc /W4 world_exit_test.cpp /Feworld_exit_test.exe /link /MANIFEST:EMBED /MANIFESTUAC:"level='asInvoker' uiAccess='false'"
if not "%errorlevel%"=="0" exit /b %errorlevel%
world_exit_test.exe
if not "%errorlevel%"=="0" exit /b %errorlevel%
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc /W4 channel_click_test.cpp /Fechannel_click_test.exe
if not "%errorlevel%"=="0" exit /b %errorlevel%
channel_click_test.exe
if not "%errorlevel%"=="0" exit /b %errorlevel%
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc /W4 character_source_return_test.cpp /Fecharacter_source_return_test.exe
if not "%errorlevel%"=="0" exit /b %errorlevel%
character_source_return_test.exe
if not "%errorlevel%"=="0" exit /b %errorlevel%
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc battle_clock_test.cpp /Febattle_clock_test.exe
if not "%errorlevel%"=="0" exit /b %errorlevel%
battle_clock_test.exe
if not "%errorlevel%"=="0" exit /b %errorlevel%
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc battle_clock_native_test.cpp /Febattle_clock_native_test.exe /link /BASE:0x10000000 /DYNAMICBASE:NO
if not "%errorlevel%"=="0" exit /b %errorlevel%
battle_clock_native_test.exe
if not "%errorlevel%"=="0" exit /b %errorlevel%
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc /W4 arrow_observe_test.cpp /Fearrow_observe_test.exe
if not "%errorlevel%"=="0" exit /b %errorlevel%
arrow_observe_test.exe
if not "%errorlevel%"=="0" exit /b %errorlevel%
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc smoke_test.cpp /Fesmoke_test.exe /link user32.lib gdi32.lib dxguid.lib
exit /b %errorlevel%


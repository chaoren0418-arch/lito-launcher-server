@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"
set "VCVARS=C:\Program Files\Microsoft Visual Studio\18\Insiders\VC\Auxiliary\Build\vcvars32.bat"
if not exist "%VCVARS%" (
  echo vcvars32.bat not found
  exit /b 1
)
call "%VCVARS%" >nul
if not exist build mkdir build
if not exist publish mkdir publish
cl /nologo /utf-8 /std:c++17 /LD /MT /O2 /EHsc /W4 /DUNICODE /D_UNICODE ^
  ddraw_proxy.cpp /Fobuild\ddraw_proxy.obj /Fepublish\ddraw.dll ^
  /link /DEF:ddraw.def user32.lib gdi32.lib dxguid.lib
if errorlevel 1 exit /b %errorlevel%
copy /y lito381-compat.ini publish\lito381-compat.ini >nul
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc smoke_test.cpp ^
  /Fobuild\smoke_test.obj /Febuild\smoke_test.exe /link user32.lib gdi32.lib dxguid.lib
if errorlevel 1 exit /b %errorlevel%
echo Built publish\ddraw.dll

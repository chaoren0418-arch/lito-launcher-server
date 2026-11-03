@echo off
call "C:\Program Files\Microsoft Visual Studio\18\Insiders\VC\Auxiliary\Build\vcvars32.bat" >nul
if errorlevel 1 exit /b %errorlevel%
link /nologo /DLL /OUT:runtime\lobby-repair-20261011\baseline.dll /MAP:runtime\lobby-repair-20261011\baseline.map /DEF:ddraw.def build\ddraw_proxy.obj user32.lib gdi32.lib dxguid.lib

@echo off
call "C:\Program Files\Microsoft Visual Studio\18\Insiders\VC\Auxiliary\Build\vcvars32.bat" >nul
cl /nologo /utf-8 /std:c++17 /MT /O2 /EHsc arrow_flight_observe_test.cpp /Fearrow_flight_observe_test.exe
if errorlevel 1 exit /b 1
arrow_flight_observe_test.exe

@echo off
call "C:\Program Files\Microsoft Visual Studio\18\Insiders\VC\Auxiliary\Build\vcvars32.bat" >nul
cl /nologo /O2 /GS- /GR- /EHs-c- /LD native_fade_extension.cpp native_catalog_loader.cpp native_tbt_extension.cpp /link /NODEFAULTLIB /ENTRY:DllMain@12 /BASE:0x68000000 /DYNAMICBASE:NO /OUT:native_fade_extension.dll



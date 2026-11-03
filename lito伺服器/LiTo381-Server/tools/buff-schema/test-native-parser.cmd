@echo off
chcp 65001 >nul
call "C:\Program Files\Microsoft Visual Studio\18\Insiders\VC\Auxiliary\Build\vcvars32.bat" >nul
cl /nologo /utf-8 /std:c++17 /EHsc /MT /W4 "D:\lineage開發\3-lito開發\lito登入器\LiTo381-Launcher\native\character_pages_native_test.cpp" /Fo"D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\tools\buff-schema\build\native\native-test.obj" /Fe"D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\tools\buff-schema\build\native\native-test.exe" user32.lib /link /BASE:0x30000000 /DYNAMICBASE:NO
if errorlevel 1 exit /b 1
"D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\tools\buff-schema\build\native\native-test.exe" "D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\tools\buff-schema\build\native\native-image.bin"

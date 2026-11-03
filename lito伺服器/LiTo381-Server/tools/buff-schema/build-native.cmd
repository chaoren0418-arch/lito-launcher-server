@echo off
chcp 65001 >nul
call "C:\Program Files\Microsoft Visual Studio\18\Insiders\VC\Auxiliary\Build\vcvars32.bat" >nul
cl /nologo /utf-8 /std:c++17 /EHsc /MT /W4 "D:\lineage開發\3-lito開發\lito登入器\LiTo381-Launcher\native\character_pages_test.cpp" /Fo"D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\tools\buff-schema\build\native\test.obj" /Fe"D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\tools\buff-schema\build\native\test.exe" user32.lib
if errorlevel 1 exit /b 1
"D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\tools\buff-schema\build\native\test.exe"
if errorlevel 1 exit /b 1
cl /nologo /utf-8 /std:c++17 /EHsc /MT /W4 "D:\lineage開發\3-lito開發\lito登入器\LiTo381-Launcher\native\selector_return_test.cpp" /Fo"D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\tools\buff-schema\build\native\selector_return_test.obj" /Fe"D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\tools\buff-schema\build\native\selector_return_test.exe" user32.lib
if errorlevel 1 exit /b 1
"D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\tools\buff-schema\build\native\selector_return_test.exe"
if errorlevel 1 exit /b 1
cl /nologo /utf-8 /std:c++17 /EHsc /MT /W4 "D:\lineage開發\3-lito開發\lito登入器\LiTo381-Launcher\native\character_source_return_test.cpp" /Fo"D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\tools\buff-schema\build\native\character_source_return_test.obj" /Fe"D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\tools\buff-schema\build\native\character_source_return_test.exe" user32.lib
if errorlevel 1 exit /b 1
"D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\tools\buff-schema\build\native\character_source_return_test.exe"
if errorlevel 1 exit /b 1
cl /nologo /utf-8 /std:c++17 /EHsc /MT /W4 "D:\lineage開發\3-lito開發\lito登入器\LiTo381-Launcher\native\source_login_order_test.cpp" /Fo"D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\tools\buff-schema\build\native\source_login_order_test.obj" /Fe"D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\tools\buff-schema\build\native\source_login_order_test.exe" user32.lib
if errorlevel 1 exit /b 1
"D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\tools\buff-schema\build\native\source_login_order_test.exe"
if errorlevel 1 exit /b 1
cl /nologo /utf-8 /std:c++17 /LD /MT /O2 /EHsc /W4 /DUNICODE /D_UNICODE "D:\lineage開發\3-lito開發\lito登入器\LiTo381-Launcher\native\ddraw_proxy.cpp" /Fo"D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\tools\buff-schema\build\native\ddraw.obj" /Fe"D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\tools\buff-schema\build\native\ddraw.dll" /link /MAP:"D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\tools\buff-schema\build\native\ddraw.map" /DEF:"D:\lineage開發\3-lito開發\lito登入器\LiTo381-Launcher\native\ddraw.def" user32.lib gdi32.lib dxguid.lib

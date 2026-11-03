$ErrorActionPreference='Stop'
& 'C:\Users\CHAOREN\AppData\Local\Programs\Python\Python312\python.exe' -B -X utf8 (Join-Path $PSScriptRoot 'tools\build_current.py')
if($LASTEXITCODE -ne 0){throw 'Build failed'}
& 'C:\Users\CHAOREN\AppData\Local\Programs\Python\Python312\python.exe' -B -X utf8 (Join-Path $PSScriptRoot 'tools\verify_current.py')
if($LASTEXITCODE -ne 0){throw 'Verification failed'}
& (Join-Path $PSScriptRoot 'tools\Clean-Generated.ps1') -Apply

$ErrorActionPreference='Stop'
function Invoke-Python([string]$Script) {
    $exe=$null
    $prefix=@()
    if (Get-Command py -ErrorAction SilentlyContinue) {
        & py -3.12 -c 'import sys' 2>$null
        if ($LASTEXITCODE -eq 0) { $exe='py'; $prefix=@('-3.12') }
    }
    if (-not $exe) {
        foreach ($name in @('python','python3')) {
            $cmd=Get-Command $name -ErrorAction SilentlyContinue
            if ($cmd) { $exe=$cmd.Source; break }
        }
    }
    if (-not $exe) { throw 'Python 3.12 required (py -3.12 or python on PATH)' }
    & $exe @prefix -B -X utf8 $Script
    if ($LASTEXITCODE -ne 0) { throw "Python failed: $Script" }
}
Invoke-Python (Join-Path $PSScriptRoot 'tools\build_current.py')
Invoke-Python (Join-Path $PSScriptRoot 'tools\verify_current.py')
& (Join-Path $PSScriptRoot 'tools\Clean-Generated.ps1') -Apply

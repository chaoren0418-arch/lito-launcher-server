$ErrorActionPreference='Stop'
function Invoke-Python([string]$Script) {
    $exe=$null
    $prefix=@()
    if (Get-Command py -ErrorAction SilentlyContinue) {
        # Expected failure when 3.12 is not installed: isolate from the script-wide
        # 'Stop' so PowerShell 5.1 NativeCommandError does not skip the PATH fallback.
        $probeOk=$false
        $savedPreference=$ErrorActionPreference
        $ErrorActionPreference='Continue'
        try {
            & py -3.12 -c 'import sys' 2>$null | Out-Null
            $probeOk=($LASTEXITCODE -eq 0)
        } catch {
            $probeOk=$false
        } finally {
            $ErrorActionPreference=$savedPreference
        }
        if ($probeOk) { $exe='py'; $prefix=@('-3.12') }
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

param([switch]$VerifyOnly)
& (Join-Path $PSScriptRoot 'tools\Apply-BaselineCleanup.ps1') -VerifyOnly:$VerifyOnly

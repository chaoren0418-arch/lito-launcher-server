param([switch]$VerifyOnly)
& (Join-Path $PSScriptRoot 'Apply-BaselineCleanup.ps1') -VerifyOnly:$VerifyOnly

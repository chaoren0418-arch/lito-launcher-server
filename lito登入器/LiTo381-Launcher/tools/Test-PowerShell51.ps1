$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Write-Output ("PowerShell version: " + $PSVersionTable.PSVersion)
$files=@((Join-Path $root 'Complete-Cleanup.ps1'),(Join-Path $root 'Build.ps1'))
$files+=@(Get-ChildItem -LiteralPath $PSScriptRoot -Filter '*.ps1' | Select-Object -ExpandProperty FullName)
foreach($file in $files){
    $tokens=$null;$parseErrors=$null
    [void][System.Management.Automation.Language.Parser]::ParseFile($file,[ref]$tokens,[ref]$parseErrors)
    if($parseErrors){throw ($parseErrors | Out-String)}
    Write-Output ("Syntax OK: " + [IO.Path]::GetFileName($file))
}
& (Join-Path $root 'Complete-Cleanup.ps1') -VerifyOnly

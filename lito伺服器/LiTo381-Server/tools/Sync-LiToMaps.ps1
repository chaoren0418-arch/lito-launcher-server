[CmdletBinding()]
param(
    [string]$SourceDirectory = "",
    [string]$DestinationDirectory = ""
)

$ErrorActionPreference = "Stop"
$projectDirectory = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($SourceDirectory)) {
    $SourceDirectory = Join-Path (Split-Path -Parent $projectDirectory) "1-伺服器\maps"
}
if ([string]::IsNullOrWhiteSpace($DestinationDirectory)) {
    $DestinationDirectory = Join-Path $projectDirectory "maps"
}

$source = (Resolve-Path -LiteralPath $SourceDirectory).Path
$sourceFiles = @(Get-ChildItem -LiteralPath $source -File -Filter "*.txt" | Sort-Object Name)
if ($sourceFiles.Count -ne 716) {
    throw "Expected 716 source map files, found $($sourceFiles.Count) in $source"
}

New-Item -ItemType Directory -Path $DestinationDirectory -Force | Out-Null
foreach ($file in $sourceFiles) {
    Copy-Item -LiteralPath $file.FullName -Destination (Join-Path $DestinationDirectory $file.Name) -Force
}

$destination = (Resolve-Path -LiteralPath $DestinationDirectory).Path
$destinationFiles = @(Get-ChildItem -LiteralPath $destination -File -Filter "*.txt" | Sort-Object Name)
if ($destinationFiles.Count -ne $sourceFiles.Count) {
    throw "Destination map count mismatch: expected $($sourceFiles.Count), found $($destinationFiles.Count)"
}

$manifest = foreach ($file in $destinationFiles) {
    $hash = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash
    "$hash  $($file.Name)"
}
$manifestPath = Join-Path $destination "MANIFEST.sha256"
Set-Content -LiteralPath $manifestPath -Value $manifest -Encoding UTF8

$bytes = ($destinationFiles | Measure-Object -Property Length -Sum).Sum
[pscustomobject]@{
    Source = $source
    Destination = $destination
    Files = $destinationFiles.Count
    Bytes = $bytes
    Manifest = $manifestPath
}

[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$serverRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$javac = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/javac.exe' } else { (Get-Command javac -ErrorAction Stop).Source }
Push-Location $serverRoot
try {
    $sources = @(Get-ChildItem -LiteralPath 'compile-stubs' -Recurse -File -Filter '*.java' | Sort-Object FullName | ForEach-Object { $_.FullName })
    if ($sources.Count -eq 0) { throw 'No compile-stubs Java sources; run tools/legacy-stubs/scan.py first.' }
    New-Item -ItemType Directory -Force 'target/legacy-stubs' | Out-Null
    & $javac --release 17 -encoding UTF-8 -d target/legacy-stubs @sources
    if ($LASTEXITCODE -ne 0) { throw "legacy-stubs javac failed (exit $LASTEXITCODE)" }
    Write-Output "Compiled $($sources.Count) compile-only sources -> target/legacy-stubs (never packaged)"
} finally {
    Pop-Location
}

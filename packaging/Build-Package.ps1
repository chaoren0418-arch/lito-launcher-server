# Build-Package.ps1 — assemble vendor package + zip from repo-relative paths.
# Windows PowerShell 5.1 and PowerShell 7 (pwsh / Linux CI).
# java / mvn / dotnet are resolved from PATH; no hardcoded C:\ or D:\ tool paths.
# Does not read or copy config/server.properties, LiTo.bin, pak/map/sprite, or archives.
[CmdletBinding()]
param(
    [switch]$SkipMaven
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-RepoRoot {
    return [IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
}

function Get-RepoRelative([string]$RepoRoot, [string]$FullPath) {
    $root = $RepoRoot.TrimEnd('\', '/')
    $full = [IO.Path]::GetFullPath($FullPath)
    if ($full.StartsWith($root, [StringComparison]::OrdinalIgnoreCase)) {
        $rel = $full.Substring($root.Length).TrimStart('\', '/')
        return ($rel -replace '\\', '/')
    }
    return ($full -replace '\\', '/')
}

function Resolve-RepoPath([string]$RepoRoot, [string]$Relative) {
    $combined = [IO.Path]::Combine($RepoRoot, ($Relative -replace '/', [IO.Path]::DirectorySeparatorChar))
    return [IO.Path]::GetFullPath($combined)
}

function Test-WindowsOS {
    if ($env:OS -eq 'Windows_NT') { return $true }
    try {
        if ($IsWindows) { return $true }
    } catch {
    }
    return $false
}

function Find-OnPath([string[]]$Names) {
    $windows = Test-WindowsOS
    foreach ($name in $Names) {
        $cmd = Get-Command $name -ErrorAction SilentlyContinue
        if (-not $cmd) { continue }
        $source = $null
        if ($cmd.Source) { $source = [string]$cmd.Source }
        elseif ($cmd.Path) { $source = [string]$cmd.Path }
        if (-not $source) { continue }
        if (-not $windows -and $source -match '\.(?i)(cmd|bat|exe)$') { continue }
        return $source
    }
    return $null
}

function Test-ReparsePoint([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) { return $false }
    try {
        $item = Get-Item -LiteralPath $Path -Force
        return [bool]($item.Attributes -band [IO.FileAttributes]::ReparsePoint)
    } catch {
        return $false
    }
}

function Test-ForbiddenEntry([string]$RelativePath) {
    $n = ($RelativePath -replace '\\', '/').TrimStart('/')
    $leaf = [IO.Path]::GetFileName($n)
    if ($leaf -ieq 'LiTo.bin') { return 'LiTo.bin' }
    if ($leaf -ieq 'server.properties') { return 'config/server.properties' }
    $parts = @($n.Split([char]'/'))
    foreach ($part in $parts) {
        if ($part -match '^(?i)(pak|map|sprite|sprites)$') { return "pak/map/sprite ($n)" }
        if ($part -match '(?i)\.(pak|map|spr|sprite|til|idx)$') { return "pak/map/sprite ($n)" }
    }
    return $null
}

function New-Directory([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        New-Item -ItemType Directory -Path $Path | Out-Null
    }
}

function Copy-FileSafe([string]$Source, [string]$Destination, [string]$RelativeForCheck) {
    $reason = Test-ForbiddenEntry $RelativeForCheck
    if ($reason) { throw "refused_forbidden_file: $reason ($RelativeForCheck)" }
    if (Test-ReparsePoint $Source) { throw "reparse_point: $Source" }
    $parent = [IO.Path]::GetDirectoryName($Destination)
    New-Directory $parent
    Copy-Item -LiteralPath $Source -Destination $Destination -Force
}

function Copy-DirectoryFiltered([string]$SourceDir, [string]$DestinationDir, [string]$RelativePrefix) {
    if (Test-ReparsePoint $SourceDir) { throw "reparse_point: $SourceDir" }
    $sourceRoot = [IO.Path]::GetFullPath($SourceDir)
    $files = @(Get-ChildItem -LiteralPath $sourceRoot -Recurse -Force -File)
    foreach ($file in $files) {
        if (Test-ReparsePoint $file.FullName) {
            Write-Host ("[WARN] 略過重解析點：{0}" -f $file.FullName)
            continue
        }
        $relInside = $file.FullName.Substring($sourceRoot.Length).TrimStart('\', '/')
        $relInsideUnix = ($relInside -replace '\\', '/')
        $check = if ($RelativePrefix) { "$RelativePrefix/$relInsideUnix" } else { $relInsideUnix }
        $reason = Test-ForbiddenEntry $check
        if ($reason) {
            Write-Host ("[WARN] 略過禁止檔案：{0} ({1})" -f $check, $reason)
            continue
        }
        $dest = [IO.Path]::Combine($DestinationDir, ($relInsideUnix -replace '/', [IO.Path]::DirectorySeparatorChar))
        $parent = [IO.Path]::GetDirectoryName($dest)
        New-Directory $parent
        Copy-Item -LiteralPath $file.FullName -Destination $dest -Force
    }
}

function Read-PomVersion([string]$PomPath) {
    $text = [IO.File]::ReadAllText($PomPath)
    $match = [regex]::Match($text, '<version>\s*([^<]+?)\s*</version>')
    if (-not $match.Success) { throw "pom_version_missing: $PomPath" }
    return $match.Groups[1].Value.Trim()
}

function Invoke-Native([string]$FilePath, [string[]]$Arguments, [string]$WorkDir) {
    $argList = @($Arguments)
    Write-Host ("RUN {0} {1}" -f $FilePath, ($argList -join ' '))
    $saved = $ErrorActionPreference
    $code = 0
    try {
        $ErrorActionPreference = 'Continue'
        if ($WorkDir) {
            Push-Location -LiteralPath $WorkDir
            try {
                & $FilePath @argList
                $code = $LASTEXITCODE
            } finally {
                Pop-Location
            }
        } else {
            & $FilePath @argList
            $code = $LASTEXITCODE
        }
    } finally {
        $ErrorActionPreference = $saved
    }
    if ($null -eq $code) {
        throw "command_failed: $FilePath did not return an exit code"
    }
    if ($code -ne 0) { throw "command_failed: $FilePath exit $code" }
}

function New-ZipFromDirectory([string]$SourceDir, [string]$ZipPath) {
    if (Test-Path -LiteralPath $ZipPath) {
        Remove-Item -LiteralPath $ZipPath -Force
    }
    $entries = @(Get-ChildItem -LiteralPath $SourceDir -Recurse -Force -File)
    if ($entries.Count -eq 0) { throw "package_empty: $SourceDir" }
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [IO.Compression.ZipFile]::Open($ZipPath, [IO.Compression.ZipArchiveMode]::Create)
    try {
        $rootName = [IO.Path]::GetFileName($SourceDir.TrimEnd('\', '/'))
        foreach ($file in $entries) {
            $rel = $file.FullName.Substring($SourceDir.Length).TrimStart('\', '/')
            $entryName = ($rootName + '/' + ($rel -replace '\\', '/'))
            $reason = Test-ForbiddenEntry $entryName
            if ($reason) { throw "zip_forbidden: $reason ($entryName)" }
            [void][IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
                $zip, $file.FullName, $entryName, [IO.Compression.CompressionLevel]::Optimal)
        }
    } finally {
        $zip.Dispose()
    }
}

function Assert-ZipClean([string]$ZipPath) {
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [IO.Compression.ZipFile]::OpenRead($ZipPath)
    try {
        $names = @()
        foreach ($entry in $zip.Entries) {
            $name = $entry.FullName -replace '\\', '/'
            if ($name.EndsWith('/')) { continue }
            $reason = Test-ForbiddenEntry $name
            if ($reason) { throw "zip_contains_forbidden: $reason ($name)" }
            $names += $name
        }
        return $names
    } finally {
        $zip.Dispose()
    }
}

$repoRoot = Get-RepoRoot
Set-Location -LiteralPath $repoRoot

$serverRel = 'lito伺服器/LiTo381-Server'
$pomRel = "$serverRel/pom.xml"
$exampleRel = "$serverRel/config/server.properties.example"
$dbRel = "$serverRel/db"
$wrapperBatRel = 'lito伺服器/1.啟動伺服器.bat'
$startBatRel = "$serverRel/Start-LiTo381-Server.bat"
$launcherRel = 'lito登入器/LiTo381-Launcher/release/current'
$installRel = 'docs/INSTALL.md'

$pomPath = Resolve-RepoPath $repoRoot $pomRel
$examplePath = Resolve-RepoPath $repoRoot $exampleRel
$dbPath = Resolve-RepoPath $repoRoot $dbRel
$wrapperBatPath = Resolve-RepoPath $repoRoot $wrapperBatRel
$startBatPath = Resolve-RepoPath $repoRoot $startBatRel
$launcherPath = Resolve-RepoPath $repoRoot $launcherRel
$installPath = Resolve-RepoPath $repoRoot $installRel
$secretsPath = Resolve-RepoPath $repoRoot "$serverRel/config/server.properties"

if (Test-Path -LiteralPath $secretsPath) {
    Write-Host '[INFO] 偵測到本機 config/server.properties；打包腳本不會讀取或複製該檔。'
}

foreach ($required in @($pomPath, $examplePath, $dbPath, $wrapperBatPath, $startBatPath, $installPath)) {
    if (-not (Test-Path -LiteralPath $required)) { throw "missing_input: $required" }
}

$java = Find-OnPath @('java.exe', 'java')
$mvn = Find-OnPath @('mvn.cmd', 'mvn')
$dotnet = Find-OnPath @('dotnet.exe', 'dotnet')

if (-not $java) { throw 'java_missing: 請把 JDK 17 的 java 放到 PATH（不要寫死安裝路徑）' }
if (-not $mvn -and -not $SkipMaven) { throw 'mvn_missing: 請把 Maven 3.9 的 mvn 放到 PATH（不要寫死安裝路徑）' }

Write-Host ("REPO_ROOT={0}" -f $repoRoot)
Write-Host ("JAVA={0}" -f $java)
Write-Host ("MVN={0}" -f $(if ($mvn) { $mvn } else { '(skipped)' }))
Write-Host ("DOTNET={0}" -f $(if ($dotnet) { $dotnet } else { '(not on PATH; launcher 不會在此腳本重建)' }))

$savedJavaPref = $ErrorActionPreference
try {
    $ErrorActionPreference = 'Continue'
    $javaVersion = (& $java -version 2>&1 | Out-String)
} finally {
    $ErrorActionPreference = $savedJavaPref
}
Write-Host ($javaVersion.Trim())
if ($javaVersion -notmatch 'version "17[\."-]') {
    Write-Host '[WARN] 建議使用 JDK 17；目前 PATH 上的 java 不是 17。編譯目標仍是 --release 17。'
}

$version = Read-PomVersion $pomPath
Write-Host ("VERSION={0}" -f $version)

$packageName = "package-$version"
$releaseRoot = Resolve-RepoPath $repoRoot 'release'
$packageDir = [IO.Path]::Combine($releaseRoot, $packageName)
$zipPath = [IO.Path]::Combine($releaseRoot, "$packageName.zip")

$serverDir = Resolve-RepoPath $repoRoot $serverRel
if (-not $SkipMaven) {
    Invoke-Native $mvn @('-B', '-DskipTests', '-f', $pomPath, 'package') $serverDir
} else {
    Write-Host '[INFO] -SkipMaven：沿用既有 target JAR。'
}

$jarPath = [IO.Path]::Combine($serverDir, 'target', 'LiTo381-Server.jar')
$libDir = [IO.Path]::Combine($serverDir, 'target', 'lib')
if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
    throw "server_jar_missing: 預期 $serverRel/target/LiTo381-Server.jar"
}

if (Test-Path -LiteralPath $packageDir) {
    Remove-Item -LiteralPath $packageDir -Recurse -Force
}
New-Directory $packageDir
New-Directory $releaseRoot

$serverOut = [IO.Path]::Combine($packageDir, 'server')
New-Directory $serverOut
New-Directory ([IO.Path]::Combine($serverOut, 'config'))
New-Directory ([IO.Path]::Combine($serverOut, 'db'))

Copy-FileSafe $jarPath ([IO.Path]::Combine($serverOut, 'LiTo381-Server.jar')) 'server/LiTo381-Server.jar'
if (Test-Path -LiteralPath $libDir -PathType Container) {
    Copy-DirectoryFiltered $libDir ([IO.Path]::Combine($serverOut, 'lib')) 'server/lib'
} else {
    Write-Host '[WARN] 找不到 target/lib（maven-dependency-plugin copy-dependencies）；Class-Path 可能不完整。'
}

$sqlFiles = @(Get-ChildItem -LiteralPath $dbPath -File -Filter '*.sql' | Sort-Object Name)
if ($sqlFiles.Count -eq 0) { throw "sql_missing: $dbRel/*.sql" }
foreach ($sql in $sqlFiles) {
    $rel = "server/db/$($sql.Name)"
    Copy-FileSafe $sql.FullName ([IO.Path]::Combine($serverOut, 'db', $sql.Name)) $rel
}
$candidatesDir = [IO.Path]::Combine($dbPath, 'candidates')
if (Test-Path -LiteralPath $candidatesDir) {
    Write-Host '[INFO] 已排除 db/candidates/（不打包候選 SQL／資源）。'
}

Copy-FileSafe $examplePath ([IO.Path]::Combine($serverOut, 'config', 'server.properties.example')) 'server/config/server.properties.example'
Copy-FileSafe $wrapperBatPath ([IO.Path]::Combine($serverOut, '1.啟動伺服器.bat')) 'server/1.啟動伺服器.bat'
Copy-FileSafe $startBatPath ([IO.Path]::Combine($serverOut, 'Start-LiTo381-Server.bat')) 'server/Start-LiTo381-Server.bat'
Copy-FileSafe $installPath ([IO.Path]::Combine($packageDir, 'INSTALL.md')) 'INSTALL.md'

$launcherIncluded = $false
$launcherNote = ''
if (Test-Path -LiteralPath $launcherPath -PathType Container) {
    $launcherOut = [IO.Path]::Combine($packageDir, 'launcher')
    New-Directory $launcherOut
    Copy-DirectoryFiltered $launcherPath $launcherOut 'launcher'
    $launcherIncluded = $true
    $launcherNote = "已收入 $launcherRel → launcher/"
    Write-Host ("[INFO] {0}" -f $launcherNote)
} else {
    $launcherNote = "找不到登入器 $launcherRel。本次只打包伺服器。若需要登入器，請先在本機執行 lito登入器/LiTo381-Launcher/Build.ps1（T003 已完成；dotnet 走 PATH），再重跑本腳本。"
    Write-Host ("[WARN] {0}" -f $launcherNote)
}

$manifestPath = [IO.Path]::Combine($packageDir, 'MANIFEST.txt')
$packagedFiles = @(Get-ChildItem -LiteralPath $packageDir -Recurse -Force -File | Sort-Object FullName)
$manifest = New-Object System.Collections.Generic.List[string]
[void]$manifest.Add("package=$packageName")
[void]$manifest.Add("version=$version")
[void]$manifest.Add("launcher_included=$launcherIncluded")
[void]$manifest.Add("created_utc=$([DateTime]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ssZ'))")
[void]$manifest.Add('')
[void]$manifest.Add('# files (paths relative to package root)')
foreach ($file in $packagedFiles) {
    $rel = Get-RepoRelative $packageDir $file.FullName
    $reason = Test-ForbiddenEntry $rel
    if ($reason) { throw "package_forbidden: $reason ($rel)" }
    [void]$manifest.Add($rel)
}
[void]$manifest.Add('MANIFEST.txt')
[IO.File]::WriteAllLines($manifestPath, [string[]]$manifest, (New-Object Text.UTF8Encoding $false))
$packagedFiles = @(Get-ChildItem -LiteralPath $packageDir -Recurse -Force -File | Sort-Object FullName)

New-ZipFromDirectory $packageDir $zipPath
$zipNames = Assert-ZipClean $zipPath

Write-Host "PACKAGE_DIR=$([IO.Path]::Combine('release', $packageName))"
Write-Host "ZIP=$([IO.Path]::Combine('release', "$packageName.zip"))"
Write-Host "LAUNCHER_INCLUDED=$launcherIncluded"
Write-Host "FILE_COUNT=$($packagedFiles.Count)"
Write-Host "ZIP_ENTRY_COUNT=$($zipNames.Count)"
Write-Host 'FORBIDDEN_SCAN=pass (no LiTo.bin, server.properties, pak/map/sprite)'
if (-not $launcherIncluded) {
    Write-Host 'LAUNCHER_STATUS=server-only'
}

$zipListPath = [IO.Path]::Combine($packageDir, 'ZIP-LISTING.txt')
$listing = New-Object System.Collections.Generic.List[string]
foreach ($name in ($zipNames | Sort-Object)) { [void]$listing.Add($name) }
[IO.File]::WriteAllLines($zipListPath, $listing, (New-Object Text.UTF8Encoding $false))
Write-Host ("ZIP_LISTING={0}" -f (Get-RepoRelative $repoRoot $zipListPath))

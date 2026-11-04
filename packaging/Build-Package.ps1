# Build-Package.ps1 — assemble vendor package + zip from repo-relative paths.
# Windows PowerShell 5.1 and PowerShell 7 (pwsh / Linux CI).
# java / mvn / dotnet are resolved from PATH; no hardcoded C:\ or D:\ tool paths.
# Does not read or copy config/server.properties, LiTo.bin, pak/map/sprite, or archives.
[CmdletBinding()]
param(
    [switch]$SkipMaven,
    [switch]$SkipQuoteProbe
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
        if ($part -ieq 'candidates') { return "db/candidates ($n)" }
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

function Write-Utf8File([string]$Path, [string]$Text, [bool]$Bom) {
    $parent = [IO.Path]::GetDirectoryName($Path)
    New-Directory $parent
    $enc = New-Object Text.UTF8Encoding $Bom
    [IO.File]::WriteAllText($Path, $Text, $enc)
}

function Assert-SourceSmokeQuoting([string]$Ps1Path) {
    $text = [IO.File]::ReadAllText($Ps1Path)
    if ($text -notmatch 'ConvertTo-StartProcessArgumentList') {
        throw "entry_missing_quoting: $Ps1Path"
    }
    if ($text -notmatch 'Start-Process -FilePath \$java -ArgumentList \$quoted') {
        throw "entry_unquoted_start_process: $Ps1Path"
    }
    Write-Host ("SOURCE_SMOKE_QUOTING=ok ({0})" -f $Ps1Path)
}

function New-FakeJavaCapture([string]$FakeDir) {
    # Prints argv then sleeps so --smoke's 8s check still sees a live process.
    # Does not start LiTo or touch DB.
    New-Directory $FakeDir
    if (Test-WindowsOS) {
        $exe = [IO.Path]::Combine($FakeDir, 'java.exe')
        Add-Type -OutputType ConsoleApplication -OutputAssembly $exe -TypeDefinition @'
using System;
using System.IO;
using System.Threading;
public class FakeJava {
    public static int Main(string[] args) {
        if (args != null && args.Length >= 1 && args[0] == "-version") {
            Console.Error.WriteLine("openjdk version \"17.0.0\"");
            return 0;
        }
        string dest = Environment.GetEnvironmentVariable("LITO_QUOTE_PROBE_OUT");
        if (string.IsNullOrEmpty(dest)) dest = "args.txt";
        File.WriteAllLines(dest, args ?? new string[0]);
        Thread.Sleep(12000);
        return 0;
    }
}
'@
        return $exe
    }
    $java = [IO.Path]::Combine($FakeDir, 'java')
    $script = @'
#!/usr/bin/env bash
set -euo pipefail
if [[ "${1-}" == "-version" ]]; then
  echo 'openjdk version "17.0.0" 2026-01-01' >&2
  exit 0
fi
: "${LITO_QUOTE_PROBE_OUT:?missing LITO_QUOTE_PROBE_OUT}"
printf '%s\n' "$@" > "$LITO_QUOTE_PROBE_OUT"
sleep 12
exit 0
'@
    Write-Utf8File $java ($script.Replace("`r`n", "`n") + "`n") $false
    & chmod +x $java
    if ($LASTEXITCODE -ne 0) { throw "chmod_failed: $java" }
    return $java
}

function Test-PackagedSmokeArgv([string]$RepoRoot, [string]$PackageDir) {
    # Runs packaged --smoke with a fake java first on PATH. Two extract roots:
    # space ('LiTo Test') and Chinese ('中文路徑'). --check is not this path.
    $hostExe = Find-OnPath @('pwsh.exe', 'powershell.exe', 'pwsh', 'powershell')
    if (-not $hostExe) { $hostExe = [string](Get-Process -Id $PID).Path }
    $scratch = [IO.Path]::Combine($RepoRoot, 'scratch', 'issue-32-quote-probe')
    if (Test-Path -LiteralPath $scratch) {
        Remove-Item -LiteralPath $scratch -Recurse -Force
    }
    New-Directory $scratch
    $fakeDir = [IO.Path]::Combine($scratch, 'fake-java')
    $fakeJava = New-FakeJavaCapture $fakeDir
    Write-Host ("QUOTE_PROBE_FAKE_JAVA={0}" -f $fakeJava)

    $cases = @(
        @{ Name = 'space'; RelDir = 'LiTo Test'; Needle = ' ' },
        @{ Name = 'chinese'; RelDir = '中文路徑'; Needle = '中文' }
    )
    $savedPath = [Environment]::GetEnvironmentVariable('PATH')
    $sep = [IO.Path]::PathSeparator
    foreach ($case in $cases) {
        $dest = [IO.Path]::Combine($scratch, [string]$case.RelDir)
        $destParent = [IO.Path]::GetDirectoryName($dest)
        New-Directory $destParent
        if (Test-Path -LiteralPath $dest) {
            Remove-Item -LiteralPath $dest -Recurse -Force
        }
        Copy-Item -LiteralPath $PackageDir -Destination $dest -Recurse -Force
        $serverRoot = [IO.Path]::Combine($dest, 'lito伺服器', 'LiTo381-Server')
        $example = [IO.Path]::Combine($serverRoot, 'config', 'server.properties.example')
        $props = [IO.Path]::Combine($serverRoot, 'config', 'server.properties')
        Copy-Item -LiteralPath $example -Destination $props -Force
        $outFile = [IO.Path]::Combine($scratch, ("args-{0}.txt" -f $case.Name))
        $entry = [IO.Path]::Combine($dest, 'tools', 'Start-PackagedServer.ps1')
        $env:LITO_QUOTE_PROBE_OUT = $outFile
        [Environment]::SetEnvironmentVariable('PATH', ($fakeDir + $sep + $savedPath))
        foreach ($name in @('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS', 'CLASSPATH')) {
            if ([Environment]::GetEnvironmentVariable($name)) {
                [Environment]::SetEnvironmentVariable($name, $null)
            }
        }
        Write-Host ("QUOTE_PROBE_CMD=PATH={0}{1}… {2} -NoProfile -File {3} --smoke" -f $fakeDir, $sep, $hostExe, $entry)
        $code = 0
        $saved = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'Continue'
            & $hostExe -NoProfile -File $entry --smoke
            $code = $LASTEXITCODE
        } finally {
            $ErrorActionPreference = $saved
            [Environment]::SetEnvironmentVariable('PATH', $savedPath)
            $env:LITO_QUOTE_PROBE_OUT = $null
        }
        if ($null -eq $code -or $code -ne 0) { throw "quote_probe_smoke_exit: $($case.Name) $code" }
        if (-not (Test-Path -LiteralPath $outFile -PathType Leaf)) {
            throw "quote_probe_no_output: $($case.Name)"
        }
        $got = @([IO.File]::ReadAllLines($outFile))
        $jarGot = $null
        $propsGot = $null
        foreach ($line in $got) {
            if ($line.EndsWith('LiTo381-Server.jar')) { $jarGot = $line }
            if ($line.EndsWith('server.properties') -and -not $line.EndsWith('.example')) { $propsGot = $line }
        }
        if (-not $jarGot) { throw ("quote_probe_missing_jar: {0}: {1}" -f $case.Name, ($got -join ' | ')) }
        if (-not $propsGot) { throw ("quote_probe_missing_props: {0}: {1}" -f $case.Name, ($got -join ' | ')) }
        $needle = [string]$case.Needle
        if ($jarGot.IndexOf($needle, [StringComparison]::Ordinal) -lt 0) {
            throw ("quote_probe_jar_missing_needle: {0} [{1}] in [{2}]" -f $case.Name, $needle, $jarGot)
        }
        if ($propsGot.IndexOf($needle, [StringComparison]::Ordinal) -lt 0) {
            throw ("quote_probe_props_missing_needle: {0} [{1}] in [{2}]" -f $case.Name, $needle, $propsGot)
        }
        Write-Host ("QUOTE_PROBE_{0}=pass" -f $case.Name)
        Write-Host ("QUOTE_PROBE_{0}_JAR={1}" -f $case.Name, $jarGot)
        Write-Host ("QUOTE_PROBE_{0}_PROPS={1}" -f $case.Name, $propsGot)
        Write-Host ("QUOTE_PROBE_{0}_ARGV_BEGIN" -f $case.Name)
        foreach ($line in $got) { Write-Host $line }
        Write-Host ("QUOTE_PROBE_{0}_ARGV_END" -f $case.Name)
    }
    Write-Host 'QUOTE_PROBE=pass (fake java on PATH; --smoke Start-Process; no LiTo/DB)'
}

function Write-ZipEvidence([string]$ZipPath, [string[]]$Names) {
    $item = Get-Item -LiteralPath $ZipPath
    Write-Host ("ZIP_BYTES={0}" -f $item.Length)
    $hash = Get-FileHash -LiteralPath $ZipPath -Algorithm SHA256
    Write-Host ("ZIP_SHA256={0}" -f $hash.Hash.ToLowerInvariant())
    Write-Host 'ZIP_SHA256_SCOPE=executor-reported for this machine; not a CI artifact digest'
    Write-Host 'ZIP_ENTRIES_BEGIN'
    foreach ($name in ($Names | Sort-Object)) { Write-Host $name }
    Write-Host 'ZIP_ENTRIES_END'
    $hasExample = $false
    $hasPackageBat = $false
    $hasPackagePs1 = $false
    $hasWrapper = $false
    $hasStartBat = $false
    $hasOfficialPs1 = $false
    $hasJar = $false
    foreach ($name in $Names) {
        $n = $name -replace '\\', '/'
        if ($n.EndsWith('server.properties.example')) { $hasExample = $true }
        if ($n.EndsWith('啟動套件伺服器.bat')) { $hasPackageBat = $true }
        if ($n.EndsWith('tools/Start-PackagedServer.ps1')) { $hasPackagePs1 = $true }
        if ($n.EndsWith('lito伺服器/1.啟動伺服器.bat')) { $hasWrapper = $true }
        if ($n.EndsWith('LiTo381-Server/Start-LiTo381-Server.bat')) { $hasStartBat = $true }
        if ($n.EndsWith('tools/Start-LiToServer.ps1')) { $hasOfficialPs1 = $true }
        if ($n.EndsWith('LiTo381-Server.jar')) { $hasJar = $true }
    }
    Write-Host ("HAS_EXAMPLE={0}" -f $hasExample)
    Write-Host ("HAS_PACKAGE_BAT={0}" -f $hasPackageBat)
    Write-Host ("HAS_PACKAGE_PS1={0}" -f $hasPackagePs1)
    Write-Host ("HAS_WRAPPER_BAT={0}" -f $hasWrapper)
    Write-Host ("HAS_START_BAT={0}" -f $hasStartBat)
    Write-Host ("HAS_OFFICIAL_START_PS1={0}" -f $hasOfficialPs1)
    Write-Host ("HAS_SERVER_JAR={0}" -f $hasJar)
    Write-Host 'FORBIDDEN_ABSENT=LiTo.bin,server.properties,pak/map/sprite,db/candidates'
    if (-not ($hasExample -and $hasPackageBat -and $hasPackagePs1 -and $hasWrapper -and $hasStartBat -and $hasOfficialPs1 -and $hasJar)) {
        throw 'startup_chain_incomplete'
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
$officialStartRel = "$serverRel/tools/Start-LiToServer.ps1"
$packagedStartRel = 'packaging/Start-PackagedServer.ps1'
$packagedBatRel = 'packaging/啟動套件伺服器.bat'
$launcherRel = 'lito登入器/LiTo381-Launcher/release/current'
$installRel = 'docs/INSTALL.md'

$pomPath = Resolve-RepoPath $repoRoot $pomRel
$examplePath = Resolve-RepoPath $repoRoot $exampleRel
$dbPath = Resolve-RepoPath $repoRoot $dbRel
$wrapperBatPath = Resolve-RepoPath $repoRoot $wrapperBatRel
$startBatPath = Resolve-RepoPath $repoRoot $startBatRel
$officialStartPath = Resolve-RepoPath $repoRoot $officialStartRel
$packagedStartPath = Resolve-RepoPath $repoRoot $packagedStartRel
$packagedBatPath = Resolve-RepoPath $repoRoot $packagedBatRel
$launcherPath = Resolve-RepoPath $repoRoot $launcherRel
$installPath = Resolve-RepoPath $repoRoot $installRel
$secretsPath = Resolve-RepoPath $repoRoot "$serverRel/config/server.properties"

if (Test-Path -LiteralPath $secretsPath) {
    Write-Host '[INFO] 偵測到本機 config/server.properties；打包腳本不會讀取或複製該檔。'
}

foreach ($required in @($pomPath, $examplePath, $dbPath, $wrapperBatPath, $startBatPath, $officialStartPath, $packagedStartPath, $packagedBatPath, $installPath)) {
    if (-not (Test-Path -LiteralPath $required)) { throw "missing_input: $required" }
}
Assert-SourceSmokeQuoting $packagedStartPath

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

$litoServerOut = [IO.Path]::Combine($packageDir, 'lito伺服器')
$serverOut = [IO.Path]::Combine($litoServerOut, 'LiTo381-Server')
New-Directory $serverOut
New-Directory ([IO.Path]::Combine($serverOut, 'config'))
New-Directory ([IO.Path]::Combine($serverOut, 'db'))
New-Directory ([IO.Path]::Combine($serverOut, 'tools'))
New-Directory ([IO.Path]::Combine($packageDir, 'tools'))

Copy-FileSafe $jarPath ([IO.Path]::Combine($serverOut, 'LiTo381-Server.jar')) 'lito伺服器/LiTo381-Server/LiTo381-Server.jar'
if (Test-Path -LiteralPath $libDir -PathType Container) {
    Copy-DirectoryFiltered $libDir ([IO.Path]::Combine($serverOut, 'lib')) 'lito伺服器/LiTo381-Server/lib'
} else {
    Write-Host '[WARN] 找不到 target/lib（maven-dependency-plugin copy-dependencies）；Class-Path 可能不完整。'
}

$sqlFiles = @(Get-ChildItem -LiteralPath $dbPath -File -Filter '*.sql' | Sort-Object Name)
if ($sqlFiles.Count -eq 0) { throw "sql_missing: $dbRel/*.sql" }
foreach ($sql in $sqlFiles) {
    $rel = "lito伺服器/LiTo381-Server/db/$($sql.Name)"
    Copy-FileSafe $sql.FullName ([IO.Path]::Combine($serverOut, 'db', $sql.Name)) $rel
}
$candidatesDir = [IO.Path]::Combine($dbPath, 'candidates')
if (Test-Path -LiteralPath $candidatesDir) {
    Write-Host '[INFO] 已排除 db/candidates/（不打包候選 SQL／資源）。'
}

Copy-FileSafe $examplePath ([IO.Path]::Combine($serverOut, 'config', 'server.properties.example')) 'lito伺服器/LiTo381-Server/config/server.properties.example'
Copy-FileSafe $wrapperBatPath ([IO.Path]::Combine($litoServerOut, '1.啟動伺服器.bat')) 'lito伺服器/1.啟動伺服器.bat'
Copy-FileSafe $startBatPath ([IO.Path]::Combine($serverOut, 'Start-LiTo381-Server.bat')) 'lito伺服器/LiTo381-Server/Start-LiTo381-Server.bat'
Copy-FileSafe $officialStartPath ([IO.Path]::Combine($serverOut, 'tools', 'Start-LiToServer.ps1')) 'lito伺服器/LiTo381-Server/tools/Start-LiToServer.ps1'
Copy-FileSafe $packagedBatPath ([IO.Path]::Combine($packageDir, '啟動套件伺服器.bat')) '啟動套件伺服器.bat'
Copy-FileSafe $packagedStartPath ([IO.Path]::Combine($packageDir, 'tools', 'Start-PackagedServer.ps1')) 'tools/Start-PackagedServer.ps1'
Copy-FileSafe $installPath ([IO.Path]::Combine($packageDir, 'INSTALL.md')) 'INSTALL.md'
$packagedToolTarget = [IO.Path]::Combine($packageDir, 'tools', 'Start-PackagedServer.ps1')
Assert-SourceSmokeQuoting $packagedToolTarget
if (-not $SkipQuoteProbe) {
    Test-PackagedSmokeArgv $repoRoot $packageDir
} else {
    Write-Host '[INFO] -SkipQuoteProbe：略過假 java --smoke 引號探測。'
}

$wrapperTarget = [IO.Path]::Combine($litoServerOut, 'LiTo381-Server', 'Start-LiTo381-Server.bat')
$officialToolTarget = [IO.Path]::Combine($serverOut, 'tools', 'Start-LiToServer.ps1')
$packagedBatTarget = [IO.Path]::Combine($packageDir, '啟動套件伺服器.bat')
foreach ($link in @($wrapperTarget, $officialToolTarget, $packagedToolTarget, $packagedBatTarget)) {
    if (-not (Test-Path -LiteralPath $link -PathType Leaf)) {
        throw "startup_chain_missing: $link"
    }
}
Write-Host 'STARTUP_CHAIN=ok (wrapper bat → LiTo381-Server/Start-LiTo381-Server.bat → tools/Start-LiToServer.ps1)'
Write-Host 'PACKAGE_ENTRY=ok (啟動套件伺服器.bat → tools/Start-PackagedServer.ps1 → Maven JAR)'

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
Write-ZipEvidence $zipPath $zipNames

$zipListPath = [IO.Path]::Combine($packageDir, 'ZIP-LISTING.txt')
$listing = New-Object System.Collections.Generic.List[string]
foreach ($name in ($zipNames | Sort-Object)) { [void]$listing.Add($name) }
[IO.File]::WriteAllLines($zipListPath, $listing, (New-Object Text.UTF8Encoding $false))
Write-Host ("ZIP_LISTING={0}" -f (Get-RepoRelative $repoRoot $zipListPath))

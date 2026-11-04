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

function ConvertTo-StartProcessArgumentList([string[]]$Arguments) {
    # Start-Process joins -ArgumentList with spaces and does not keep element
    # boundaries (MS docs; also observed on Linux pwsh). Quote each token like
    # tools/Start-LiToServer.ps1 so paths with spaces/Chinese stay one argv.
    $quoted = New-Object System.Collections.Generic.List[string]
    foreach ($arg in $Arguments) {
        [void]$quoted.Add('"' + $arg + '"')
    }
    return [string[]]$quoted.ToArray()
}

function Test-StartProcessQuoting([string]$RepoRoot) {
    # Exercises the same Start-Process -ArgumentList quoting as packaged --smoke,
    # using a capture program. Does not start LiTo or touch DB. --check is not this path.
    $scratch = [IO.Path]::Combine($RepoRoot, 'scratch', 'issue-32-quote-probe')
    if (Test-Path -LiteralPath $scratch) {
        Remove-Item -LiteralPath $scratch -Recurse -Force
    }
    $dir = [IO.Path]::Combine($scratch, 'LiTo Test 測試')
    New-Directory $dir
    $jar = [IO.Path]::Combine($dir, 'LiTo381-Server.jar')
    $props = [IO.Path]::Combine($dir, 'server.properties')
    $capture = [IO.Path]::Combine($dir, 'capture.ps1')
    $outFile = [IO.Path]::Combine($dir, 'args.txt')
    $stdoutLog = [IO.Path]::Combine($dir, 'capture.stdout.log')
    $stderrLog = [IO.Path]::Combine($dir, 'capture.stderr.log')
    [IO.File]::WriteAllText($jar, 'dummy-jar')
    [IO.File]::WriteAllText($props, 'dummy-properties')
    $captureText = @(
        'Set-StrictMode -Version Latest',
        '$out = $args[0]',
        '$rest = @()',
        'if ($args.Count -gt 1) { $rest = $args[1..($args.Count - 1)] }',
        '[IO.File]::WriteAllLines($out, [string[]]$rest, (New-Object Text.UTF8Encoding $false))'
    ) -join "`n"
    Write-Utf8File $capture $captureText $true

    # Same token layout as packaged --smoke (jar + properties after -jar).
    $jvm = @(
        '-Dfile.encoding=UTF-8',
        '-Djava.net.preferIPv4Stack=true',
        '-jar', $jar,
        $props,
        '--service-mode=standard'
    )
    foreach ($arg in @($jar, $props)) {
        if ($arg.Contains(' ') -eq $false -or $arg.Contains('測試') -eq $false) {
            throw 'quote_probe_path_must_contain_space_and_chinese'
        }
    }
    $joined = ((@($jvm | ForEach-Object { '"' + $_ + '"' })) -join ' ')
    foreach ($arg in @($jar, $props)) {
        $needle = '"' + $arg + '"'
        if ($joined.IndexOf($needle, [StringComparison]::Ordinal) -lt 0) {
            throw ("quote_join_missing: {0}" -f $arg)
        }
    }
    Write-Host 'QUOTE_JOIN=ok (space-joined Start-Process command line keeps quoted paths with space+測試)'

    $hostExe = Find-OnPath @('pwsh.exe', 'powershell.exe', 'pwsh', 'powershell')
    if (-not $hostExe) { $hostExe = [string](Get-Process -Id $PID).Path }
    $procArgs = @('-NoProfile', '-File', $capture, $outFile) + $jvm
    $quoted = ConvertTo-StartProcessArgumentList $procArgs
    $saved = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $proc = Start-Process -FilePath $hostExe -ArgumentList $quoted -WorkingDirectory $dir -Wait -PassThru -RedirectStandardOutput $stdoutLog -RedirectStandardError $stderrLog
        $code = $proc.ExitCode
        $proc.Dispose()
    } finally {
        $ErrorActionPreference = $saved
    }
    if ($code -ne 0) { throw "quote_probe_exit: $code" }
    if (-not (Test-Path -LiteralPath $outFile -PathType Leaf)) { throw 'quote_probe_no_output' }
    $got = @([IO.File]::ReadAllLines($outFile))
    $expect = $jvm
    if ($got.Count -ne $expect.Count) {
        throw ("quote_probe_argc: got {0} expect {1}: {2}" -f $got.Count, $expect.Count, ($got -join ' | '))
    }
    for ($i = 0; $i -lt $expect.Count; $i++) {
        if ($got[$i] -cne $expect[$i]) {
            throw ("quote_probe_arg[{0}]: got [{1}] expect [{2}]" -f $i, $got[$i], $expect[$i])
        }
    }
    Write-Host ("QUOTE_PROBE=pass host={0} (Start-Process --smoke ArgumentList; no LiTo/DB)" -f $hostExe)
    Write-Host ("QUOTE_PROBE_JAR={0}" -f $got[3])
    Write-Host ("QUOTE_PROBE_PROPS={0}" -f $got[4])
}

function Assert-GeneratedSmokeQuoting([string]$GeneratedPs1) {
    $text = [IO.File]::ReadAllText($GeneratedPs1)
    if ($text -notmatch 'ConvertTo-StartProcessArgumentList') {
        throw "generated_entry_missing_quoting: $GeneratedPs1"
    }
    if ($text -notmatch 'Start-Process -FilePath \$java -ArgumentList \$quoted') {
        throw "generated_entry_unquoted_start_process: $GeneratedPs1"
    }
    Write-Host 'GENERATED_SMOKE_QUOTING=ok (Start-Process uses quoted ArgumentList)'
}

function Write-PackagedEntry([string]$PackageDir) {
    $bat = @(
        '@echo off',
        'setlocal EnableExtensions',
        'chcp 65001 >nul',
        'cd /d "%~dp0"',
        'powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\Start-PackagedServer.ps1" %*',
        'set "LITO_EXIT=%ERRORLEVEL%"',
        'if not "%LITO_EXIT%"=="0" if "%~1"=="" pause',
        'exit /b %LITO_EXIT%'
    ) -join "`r`n"
    Write-Utf8File ([IO.Path]::Combine($PackageDir, '啟動套件伺服器.bat')) ($bat + "`r`n") $false

    $ps1 = @'
# Generated by packaging/Build-Package.ps1 into <package>/tools/Start-PackagedServer.ps1.
# PATH java + Maven JAR. Does not replace official Start-LiToServer.ps1.
# No param() block: `--check` must stay positional.
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$ModeArg = 'standard'
if ($args.Count -ge 1) { $ModeArg = [string]$args[0] }

function Test-WindowsOS {
    if ($env:OS -eq 'Windows_NT') { return $true }
    try { if ($IsWindows) { return $true } } catch {}
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
function Assert-File([string]$Path, [string]$Hint) {
    if (-not [IO.File]::Exists($Path)) { throw ("缺少檔案：{0}`n{1}" -f $Path, $Hint) }
}
function ConvertTo-StartProcessArgumentList([string[]]$Arguments) {
    $quoted = New-Object System.Collections.Generic.List[string]
    foreach ($arg in $Arguments) { [void]$quoted.Add('"' + $arg + '"') }
    return [string[]]$quoted.ToArray()
}

$mode = 'standard'
if ([string]::IsNullOrWhiteSpace($ModeArg) -or $ModeArg -eq 'standard') { $mode = 'standard' }
elseif ($ModeArg -eq '--check' -or $ModeArg -eq 'check') { $mode = 'check' }
elseif ($ModeArg -eq '--smoke' -or $ModeArg -eq 'smoke') { $mode = 'smoke' }
else { throw "未知參數：$ModeArg（可用 --check、--smoke、或不加參數）" }

$packageRoot = [IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
$serverRoot = [IO.Path]::Combine($packageRoot, 'lito伺服器', 'LiTo381-Server')
$jar = [IO.Path]::Combine($serverRoot, 'LiTo381-Server.jar')
$libDir = [IO.Path]::Combine($serverRoot, 'lib')
$example = [IO.Path]::Combine($serverRoot, 'config', 'server.properties.example')
$properties = [IO.Path]::Combine($serverRoot, 'config', 'server.properties')
Write-Host ("PACKAGE_ROOT={0}" -f $packageRoot)
Write-Host ("SERVER_ROOT={0}" -f $serverRoot)
Write-Host ("MODE={0}" -f $mode)

$java = Find-OnPath @('java.exe', 'java')
if (-not $java) { throw 'java_missing: 請把 JDK 17 的 java 放到 PATH（不要寫死安裝路徑）' }
Write-Host ("JAVA={0}" -f $java)
$saved = $ErrorActionPreference
try { $ErrorActionPreference = 'Continue'; $javaVersion = (& $java -version 2>&1 | Out-String) }
finally { $ErrorActionPreference = $saved }
Write-Host ($javaVersion.Trim())
if ($javaVersion -notmatch 'version "17[\."-]') {
    Write-Host '[WARN] 建議使用 JDK 17；目前 PATH 上的 java 不是 17。'
}
Assert-File $jar '套件應含 lito伺服器/LiTo381-Server/LiTo381-Server.jar（Maven 產物）。'
Assert-File $example '套件應含 config/server.properties.example。'
if (-not (Test-Path -LiteralPath $libDir -PathType Container)) { throw "缺少 lib/：$libDir" }
$libJars = @(Get-ChildItem -LiteralPath $libDir -File -Filter '*.jar')
if ($libJars.Count -lt 1) { throw "lib/ 沒有 runtime JAR：$libDir" }
Write-Host ("LIB_JAR_COUNT={0}" -f $libJars.Count)
if (Test-Path -LiteralPath $properties -PathType Leaf) {
    Write-Host 'CONFIG=server.properties (present; not printed)'
} else {
    Write-Host 'CONFIG=missing'
    Write-Host '請複製 config/server.properties.example 為 config/server.properties 並改掉 CHANGE_ME。此檔不進 zip。'
}
Write-Host '[INFO] 此入口使用包內 Maven JAR，不是正式 candidate JAR。來源缺口見 docs/server-source-gap.md。'
Write-Host '[INFO] 包內沒有 common_catalog.bin、pak/map/sprite、LiTo.bin；客戶端維持 D:\lineage開發\0-客戶端。'
if ($mode -eq 'check') {
    Write-Host '[PASS] 套件入口檢查完成：PATH 上的 java、Maven JAR、lib/、server.properties.example。'
    if (-not (Test-Path -LiteralPath $properties -PathType Leaf)) {
        Write-Host '[PASS] --check 不要求已填的 server.properties；啟動／--smoke 才需要。'
    }
    exit 0
}
if (-not (Test-Path -LiteralPath $properties -PathType Leaf)) {
    throw 'Required file missing: config/server.properties（從 .example 複製，不要把填好的檔打回 zip）'
}
foreach ($name in @('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS', 'CLASSPATH')) {
    if ([Environment]::GetEnvironmentVariable($name)) { throw "Unexpected Java environment override: $name" }
}
$jvm = @(
    '-Dfile.encoding=UTF-8',
    '-Djava.net.preferIPv4Stack=true',
    '-jar', $jar,
    $properties,
    '--service-mode=standard'
)
if ($mode -eq 'smoke') {
    Write-Host '[INFO] --smoke 會啟動包內 Maven JAR（可能連資料庫）。官方 --smoke 仍走 candidate JAR + catalog + deployment/release-inputs.json。'
    Write-Host '[INFO] Start-Process -ArgumentList 以空白串接，路徑含空白／中文時必須逐項加引號。'
    $quoted = ConvertTo-StartProcessArgumentList $jvm
    $process = Start-Process -FilePath $java -ArgumentList $quoted -WorkingDirectory $serverRoot -PassThru
    try {
        Start-Sleep -Seconds 8
        $process.Refresh()
        if ($process.HasExited) {
            throw ("Maven JAR 在 smoke 期間已退出，code={0}。此產物與正式 0.6.71 不完全等價。" -f $process.ExitCode)
        }
        Write-Host '[PASS] Maven JAR 行程仍在執行；正在停止 smoke 行程（未核對 login-listen）。'
    } finally {
        if (-not $process.HasExited) { $process.Kill(); $process.WaitForExit() }
        $process.Dispose()
    }
    exit 0
}
Write-Host '按 Ctrl+C 停止。本入口不會改正式 candidate JAR，也不會讀取包外的機密設定。'
& $java @jvm
exit $LASTEXITCODE
'@
    Write-Utf8File ([IO.Path]::Combine($PackageDir, 'tools', 'Start-PackagedServer.ps1')) $ps1 $true
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
$launcherRel = 'lito登入器/LiTo381-Launcher/release/current'
$installRel = 'docs/INSTALL.md'

$pomPath = Resolve-RepoPath $repoRoot $pomRel
$examplePath = Resolve-RepoPath $repoRoot $exampleRel
$dbPath = Resolve-RepoPath $repoRoot $dbRel
$wrapperBatPath = Resolve-RepoPath $repoRoot $wrapperBatRel
$startBatPath = Resolve-RepoPath $repoRoot $startBatRel
$officialStartPath = Resolve-RepoPath $repoRoot $officialStartRel
$launcherPath = Resolve-RepoPath $repoRoot $launcherRel
$installPath = Resolve-RepoPath $repoRoot $installRel
$secretsPath = Resolve-RepoPath $repoRoot "$serverRel/config/server.properties"

if (Test-Path -LiteralPath $secretsPath) {
    Write-Host '[INFO] 偵測到本機 config/server.properties；打包腳本不會讀取或複製該檔。'
}

foreach ($required in @($pomPath, $examplePath, $dbPath, $wrapperBatPath, $startBatPath, $officialStartPath, $installPath)) {
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
Copy-FileSafe $installPath ([IO.Path]::Combine($packageDir, 'INSTALL.md')) 'INSTALL.md'
Write-PackagedEntry $packageDir
$packagedToolTarget = [IO.Path]::Combine($packageDir, 'tools', 'Start-PackagedServer.ps1')
Assert-GeneratedSmokeQuoting $packagedToolTarget
if (-not $SkipQuoteProbe) {
    Test-StartProcessQuoting $repoRoot
} else {
    Write-Host '[INFO] -SkipQuoteProbe：略過 Start-Process 引號探測。'
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
Write-Host 'PACKAGE_ENTRY=ok (generated 啟動套件伺服器.bat → tools/Start-PackagedServer.ps1 → Maven JAR)'

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

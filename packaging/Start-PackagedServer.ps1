# Package-local server entry. Generated copy lives at <package>/tools/Start-PackagedServer.ps1.
# Uses java on PATH and the Maven JAR inside this zip. Does not replace the official
# Start-LiToServer.ps1 (hard-coded JDK / candidate JAR / catalog / deployment pins).
# Windows PowerShell 5.1 and PowerShell 7.
# No param() block: `--check` must stay a positional token (otherwise PowerShell
# treats it as a named switch and fails).
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$ModeArg = 'standard'
if ($args.Count -ge 1) { $ModeArg = [string]$args[0] }

function Find-OnPath([string[]]$Names) {
    foreach ($name in $Names) {
        $cmd = Get-Command $name -ErrorAction SilentlyContinue
        if (-not $cmd) { continue }
        if ($cmd.Source) { return [string]$cmd.Source }
        if ($cmd.Path) { return [string]$cmd.Path }
    }
    return $null
}

function Assert-File([string]$Path, [string]$Hint) {
    if (-not [IO.File]::Exists($Path)) { throw ("缺少檔案：{0}`n{1}" -f $Path, $Hint) }
}

$mode = 'standard'
if ([string]::IsNullOrWhiteSpace($ModeArg) -or $ModeArg -eq 'standard') {
    $mode = 'standard'
} elseif ($ModeArg -eq '--check' -or $ModeArg -eq 'check') {
    $mode = 'check'
} elseif ($ModeArg -eq '--smoke' -or $ModeArg -eq 'smoke') {
    $mode = 'smoke'
} else {
    throw "未知參數：$ModeArg（可用 --check、--smoke、或不加參數）"
}

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
try {
    $ErrorActionPreference = 'Continue'
    $javaVersion = (& $java -version 2>&1 | Out-String)
} finally {
    $ErrorActionPreference = $saved
}
Write-Host ($javaVersion.Trim())
if ($javaVersion -notmatch 'version "17[\."-]') {
    Write-Host '[WARN] 建議使用 JDK 17；目前 PATH 上的 java 不是 17。'
}

Assert-File $jar '套件應含 lito伺服器/LiTo381-Server/LiTo381-Server.jar（Maven 產物）。'
Assert-File $example '套件應含 config/server.properties.example。'
if (-not (Test-Path -LiteralPath $libDir -PathType Container)) {
    throw "缺少 lib/：$libDir"
}
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
    if ([Environment]::GetEnvironmentVariable($name)) {
        throw "Unexpected Java environment override: $name"
    }
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
    Write-Host 'RUN java -jar LiTo381-Server.jar <server.properties> --service-mode=standard'
    $process = Start-Process -FilePath $java -ArgumentList $jvm -WorkingDirectory $serverRoot -PassThru
    try {
        Start-Sleep -Seconds 8
        $process.Refresh()
        if ($process.HasExited) {
            throw ("Maven JAR 在 smoke 期間已退出，code={0}。此產物與正式 0.6.71 不完全等價。" -f $process.ExitCode)
        }
        Write-Host '[PASS] Maven JAR 行程仍在執行；正在停止 smoke 行程（未核對 login-listen）。'
    } finally {
        if (-not $process.HasExited) {
            $process.Kill()
            $process.WaitForExit()
        }
        $process.Dispose()
    }
    exit 0
}

Write-Host '按 Ctrl+C 停止。本入口不會改正式 candidate JAR，也不會讀取包外的機密設定。'
& $java @jvm
exit $LASTEXITCODE

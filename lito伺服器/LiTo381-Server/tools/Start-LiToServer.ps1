[CmdletBinding()]
param([ValidateSet('standard','check','smoke')][string]$Mode='standard')
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
$configRoot = Join-Path $projectRoot 'config'

function Read-Properties([string]$path) {
    $values = @{}
    foreach ($line in [IO.File]::ReadAllLines($path, [Text.Encoding]::UTF8)) {
        $text = $line.Trim()
        if (-not $text -or $text.StartsWith('#') -or $text.StartsWith('!')) { continue }
        $parts = $text.Split([char[]]'=', 2, [StringSplitOptions]::None)
        if ($parts.Count -ne 2 -or -not $parts[0].Trim() -or $text.EndsWith('\')) { throw "Unsupported property syntax in $path" }
        $key = $parts[0].Trim()
        if ($values.ContainsKey($key)) { throw "Duplicate property: $key" }
        $values[$key] = $parts[1].Trim()
    }
    return $values
}
function Resolve-ConfigPath([string]$value) {
    if (-not $value) { throw 'Missing configuration path' }
    if ([IO.Path]::IsPathRooted($value)) { return [IO.Path]::GetFullPath($value) }
    return [IO.Path]::GetFullPath([IO.Path]::Combine($configRoot,$value))
}
function Assert-File([string]$path) {
    if (-not [IO.File]::Exists($path)) { throw "Required file missing: $path" }
}
function File-Hash([string]$path) {
    $algorithm = [Security.Cryptography.SHA256]::Create()
    $stream = [IO.File]::OpenRead($path)
    try { return [BitConverter]::ToString($algorithm.ComputeHash($stream)).Replace('-','') }
    finally { $stream.Dispose(); $algorithm.Dispose() }
}
function Read-RunningLog([string]$path) {
    if (-not [IO.File]::Exists($path)) { return '' }
    $stream = [IO.File]::Open($path, [IO.FileMode]::Open, [IO.FileAccess]::Read, [IO.FileShare]::ReadWrite)
    $reader = New-Object IO.StreamReader($stream, [Text.Encoding]::UTF8)
    try { return $reader.ReadToEnd() }
    finally { $reader.Dispose() }
}
function Assert-FreeEndpoint([string]$address, [int]$port) {
    $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Parse($address), $port)
    $listener.ExclusiveAddressUse = $true
    try { $listener.Start() }
    catch { throw "Endpoint occupied: ${address}:$port. Stop the existing LiTo server first; no process was killed." }
    finally { $listener.Stop() }
}

try {
    Set-Location -LiteralPath $projectRoot
    $serverPath = Join-Path $configRoot 'server.properties'
    $java = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin/java.exe'
    $jar = Resolve-ConfigPath '../../../lito登入器/LiTo381-Launcher/app/server/LiTo381-SharedResources-candidate.jar'
    $catalog = Resolve-ConfigPath '../../../lito登入器/LiTo381-Launcher/app/common_catalog.bin'
    foreach ($file in @($java,$jar,$catalog,$serverPath)) { Assert-File $file }
    foreach ($name in @('JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS','CLASSPATH')) {
        if ([Environment]::GetEnvironmentVariable($name)) { throw "Unexpected Java environment override: $name" }
    }
    # Check runtime identity, while operator-editable properties remain editable.
    $pins = Get-Content -LiteralPath (Join-Path $projectRoot 'deployment/release-inputs.json') -Raw -Encoding UTF8 | ConvertFrom-Json
    foreach ($pair in @(@($jar,$pins.server_sha256),@($catalog,$pins.catalog_sha256))) {
        if ((File-Hash $pair[0]) -ine $pair[1]) { throw 'Published JAR/catalog hash mismatch' }
    }
    $classPath = $jar + ';' + (Join-Path $projectRoot 'lib/*')
    $effectiveText = & $java '-Dfile.encoding=UTF-8' '-cp' $classPath 'local.lito381.server.UnifiedConfiguration' $serverPath
    if ($LASTEXITCODE -ne 0) { throw 'Single configuration / SQL source directory validation failed' }
    $effective = ($effectiveText -join "`n") | ConvertFrom-Json
    $server = @{}
    foreach ($property in $effective.settings.PSObject.Properties) { $server[$property.Name] = [string]$property.Value }
    $launcher = $server
    $savedPreference = $ErrorActionPreference
    try { $ErrorActionPreference = 'Continue'; $javaInfo = (& $java -version 2>&1 | Out-String) }
    finally { $ErrorActionPreference = $savedPreference }
    if ($LASTEXITCODE -ne 0 -or $javaInfo -notmatch 'version "17[\."]') { throw 'Java 17 is required' }
    foreach ($key in @('memory.initial','memory.maximum')) {
        if ($launcher[$key] -notmatch '^[1-9][0-9]*[mMgG]$') { throw "Invalid JVM memory setting: $key" }
    }
    function Memory-MiB([string]$value) {
        $number = [long]$value.Substring(0,$value.Length-1)
        if ($value.EndsWith('g',[StringComparison]::OrdinalIgnoreCase)) { $number *= 1024 }
        return $number
    }
    if ((Memory-MiB $launcher['memory.initial']) -gt (Memory-MiB $launcher['memory.maximum'])) { throw 'Initial heap exceeds maximum heap' }
    $cores = 0; $port = 0; $timeout = 0; $count = 0
    if (-not [int]::TryParse($launcher['cpu.limit'],[ref]$cores) -or $cores -lt 1 -or $cores -gt 256) { throw 'cpu.limit must be 1..256' }
    if (-not [int]::TryParse($server['port'],[ref]$port) -or $port -lt 1 -or $port -gt 65535) { throw 'Invalid server port' }
    if (-not [int]::TryParse($launcher['startup.timeout.seconds'],[ref]$timeout) -or $timeout -lt 5 -or $timeout -gt 300) { throw 'startup.timeout.seconds must be 5..300' }
    if ($server['database.mode'] -ne 'lito_mysql') { throw 'This entry requires lito_mysql' }
    if ($server['database.read.only'] -notin @('true','false')) { throw 'Invalid database.read.only' }
    $routes = @($effective.routes | ForEach-Object { $_.database })
    $ids = @($effective.routes | ForEach-Object { [string]$_.id })
    $names = @($effective.routes | ForEach-Object { $_.name })
    if ($routes.Count -lt 1) { throw 'At least one source must be enabled' }
    $count = $routes.Count
    if (@($ids | Select-Object -Unique).Count -ne $count -or @($names | Select-Object -Unique).Count -ne $count) { throw 'Duplicate source id/name' }
    if ($server['database.persistence.policy'] -ne 'source-readonly-stats-only' -or $server['database.read.only'] -ne 'true' -or $server['state.persistence.enabled'] -ne 'false') { throw 'Shared database requires source-readonly-stats-only and memory state' }
    if ($server['database.lito.database'] -notmatch '^[A-Za-z0-9_]+$') { throw 'Invalid LiTo database name' }
    if ($server['database.static.database'] -notmatch '^[A-Za-z0-9_]+$') { throw 'Invalid static database name' }
    if ($server['lito.channel.ports'] -or $server['lito.rental.teleport.routes']) { throw 'This entry supports the standard shared-channel endpoint only' }
    Assert-FreeEndpoint $server['bind'] $port
    Assert-FreeEndpoint $server['game.bind'] $port
    $sourcePortBase = 0
    if (-not [int]::TryParse($server['login.source.port.base'],[ref]$sourcePortBase) -or $sourcePortBase -lt 1024 -or $sourcePortBase -gt 65527) { throw 'Invalid login.source.port.base' }
    foreach ($sourceId in $ids) {
        $sourceIdValue = 0
        if (-not [int]::TryParse($sourceId,[ref]$sourceIdValue) -or $sourceIdValue -lt 1 -or $sourceIdValue -gt 8) { throw 'Source ID must be 1..8' }
        $sourcePort = $sourcePortBase + $sourceIdValue
        if ($sourcePort -eq $port) { throw 'Source port overlaps base listener' }
        Assert-FreeEndpoint $server['bind'] $sourcePort
        Assert-FreeEndpoint $server['game.bind'] $sourcePort
    }
    $jvm = @('-server',('-Xms'+$launcher['memory.initial']),('-Xmx'+$launcher['memory.maximum']),
        "-XX:ActiveProcessorCount=$cores",'-XX:+UseZGC','-Dfile.encoding=UTF-8','-Djava.net.preferIPv4Stack=true',
        '-Dlito.resourceProfile=tw13081901-v1','-Dlito.extendedItemDetails=true',('-Dlito.commonCatalog='+$catalog),
        '-jar',$jar,$serverPath,'--service-mode=standard')
    Write-Host 'LiTo 專用伺服器 / Java 17 / 共用資源正式模式'
    Write-Host ("設定：{0}" -f $serverPath)
    Write-Host ("記憶體：{0}～{1}；CPU：{2} 核心" -f $launcher['memory.initial'],$launcher['memory.maximum'],$cores)
    Write-Host ("角色來源：{0}；唯讀：{1}" -f ($names -join '、'),$server['database.read.only'])
    if ($Mode -eq 'check') { Write-Host '[PASS] 設定、Java、核心、資源與埠檢查完成；已唯讀查詢 SQL 來源清單。'; exit 0 }
    $logRoot = Resolve-ConfigPath $launcher['logs.directory']
    [IO.Directory]::CreateDirectory($logRoot) | Out-Null
    $jvm = @('-XX:+HeapDumpOnOutOfMemoryError',('-XX:HeapDumpPath='+$logRoot)) + $jvm
    if ($Mode -eq 'smoke') {
        $tag = 'startup-check-' + [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss-fff')
        $stdout = Join-Path $logRoot ($tag+'.stdout.log')
        $stderr = Join-Path $logRoot ($tag+'.stderr.log')
        $quoted = @($jvm | ForEach-Object { '"' + $_ + '"' })
        $process = Start-Process -FilePath $java -ArgumentList $quoted -WorkingDirectory $projectRoot -WindowStyle Hidden -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
        $retainedHandle = $process.Handle
        $ready = $false
        try {
            $deadline = [DateTime]::UtcNow.AddSeconds($timeout)
            while ([DateTime]::UtcNow -lt $deadline) {
                $process.Refresh()
                if ($process.HasExited) { throw "Server exited during startup. See $stderr" }
                $output = Read-RunningLog $stdout
                if ($output.Contains('[LiTo] login-listen=') -and $output.Contains('[LiTo] game-listen=') -and $output.Contains('[LiTo] battle-animations=') -and $output.Contains('read-only='+$server['database.read.only'])) { $ready=$true; break }
                Start-Sleep -Milliseconds 200
            }
            if (-not $ready) { throw "Startup timed out. See $stdout" }
        } finally {
            if (-not $process.HasExited) { $process.Kill() }
            $process.WaitForExit(); $process.Dispose()
        }
        Assert-FreeEndpoint $server['bind'] $port
        Assert-FreeEndpoint $server['game.bind'] $port
        Write-Host '[PASS] 正式 SQL 與共用資源啟動完成，驗證程序已關閉、埠已釋放。'
        exit 0
    }
    Write-Host '按 Ctrl+C 停止伺服器。本入口不會自動重啟或覆蓋核心。'
    & $java @jvm
    exit $LASTEXITCODE
} catch {
    Write-Host ('[失敗] '+$_.Exception.Message) -ForegroundColor Red
    exit 1
}

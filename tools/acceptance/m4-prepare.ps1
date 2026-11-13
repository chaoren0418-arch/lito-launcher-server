# M4 驗收：建立本次 run 目錄（T128／Issue #379）。相容 Windows PowerShell 5.1；UTF-8 BOM。
# 做的事：從 target/ 複製 JAR 與 lib 到 scratch/m4/run-<UTC>/、逐檔記 SHA-256、建空的 runtime/ 與 evidence/、
#         產生不含帳密的設定範本、對照 UnifiedConfiguration.KEYS 是否仍是 22 鍵。
# 不做的事：不啟動 Java、不連資料庫、不讀寫 config/server.properties、不建立硬連結。
# 用法（先在 lito伺服器\LiTo381-Server 執行 mvn -B -DskipTests package）：
#   powershell -NoProfile -ExecutionPolicy Bypass -File tools\acceptance\m4-prepare.ps1
#   powershell -NoProfile -ExecutionPolicy Bypass -File tools\acceptance\m4-prepare.ps1 -LitoDatabase p24_380lito_lito_exp
param(
    [string]$ServerDirectory = '',
    [string]$RunRoot = '',
    [string]$StaticDatabase = 'p24_380db_lito_exp',
    [string]$LitoDatabase = 'p24_lito_lito_exp',
    [string]$Bind = '127.0.0.1',
    [string]$GameBind = '127.0.0.2',
    [int]$Port = 42083,
    [string]$JdbcBaseUrl = 'jdbc:mysql://127.0.0.1:43324/',
    [string]$JdbcOptions = '?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true',
    [string]$ServerName = 'LiTo M4 P24 Experiment'
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'm4-common.ps1')

try {
    $repo = Get-M4RepoRoot $PSScriptRoot
    if ($ServerDirectory -eq '') { $ServerDirectory = Join-Path $repo 'lito伺服器\LiTo381-Server' }
    if ($RunRoot -eq '') { $RunRoot = Join-Path $repo 'scratch\m4' }
    Assert-M4NotServerProperties -Path $ServerDirectory -What 'ServerDirectory'
    Assert-M4NotServerProperties -Path $RunRoot -What 'RunRoot'

    # ---- 設定值先過守門，再碰檔案 ----
    $overrides = @{
        'bind' = $Bind; 'game.bind' = $GameBind; 'port' = [string]$Port; 'server.name' = $ServerName
        'database.jdbc.base.url' = $JdbcBaseUrl; 'database.jdbc.options' = $JdbcOptions
        'database.user' = $script:M4PlaceholderUser; 'database.password' = $script:M4PlaceholderPassword
        'database.static.database' = $StaticDatabase; 'database.lito.database' = $LitoDatabase
        'capture.directory' = '../captures'; 'logs.directory' = '../logs'; 'startup.timeout.seconds' = '120'
    }
    Assert-M4Settings $overrides
    if ($Bind -eq $GameBind) { throw 'bind 與 game.bind 必須不同（ServerConfig.java:254-258）' }

    # ---- 來源成品 ----
    $target = Join-Path $ServerDirectory 'target'
    $jarName = 'LiTo381-Server.jar'
    $jar = Join-Path $target $jarName
    if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) {
        throw "找不到 $jar；先在 $ServerDirectory 執行 mvn -B -DskipTests package"
    }
    $libDirectory = Join-Path $target 'lib'
    $libraries = @()
    if (Test-Path -LiteralPath $libDirectory -PathType Container) {
        $libraries = @(Get-ChildItem -LiteralPath $libDirectory -File -Filter '*.jar' | Sort-Object Name)
    }
    if (@($libraries | Where-Object { $_.Name -like 'mysql-connector-j-*.jar' }).Count -ne 1) {
        throw "target\lib 需要剛好一個 mysql-connector-j-*.jar（pom.xml:182-187 的 copy-dependencies）"
    }

    # ---- 設定鍵：原始碼 KEYS、範例檔、#284 的 22 鍵三方對照 ----
    $javaFile = Join-Path $ServerDirectory 'src\main\java\local\lito381\server\UnifiedConfiguration.java'
    $sourceKeys = Get-M4ConfigKeysFromSource $javaFile
    if ($sourceKeys -eq $null -or @($sourceKeys).Count -eq 0) { throw "無法從 $javaFile 取出 UnifiedConfiguration.KEYS" }
    $keyDiff = Compare-M4KeySets -Baseline $script:M4BaselineConfigKeys -Actual $sourceKeys
    $example = Read-M4Properties (Join-Path $ServerDirectory 'config\server.properties.example')
    $settings = @{}
    foreach ($key in $sourceKeys) {
        if ($overrides.ContainsKey($key)) { $settings[$key] = $overrides[$key] }
        elseif ($example.ContainsKey($key)) { $settings[$key] = $example[$key] }
        else { throw "KEYS 有新鍵 '$key'，但 server.properties.example 沒有預設值；請補上再重跑" }
    }
    foreach ($key in $overrides.Keys) {
        if (@($sourceKeys) -notcontains $key) { throw "本腳本要寫的鍵 '$key' 已不在 KEYS 內；伺服器會以 Unknown/retired setting 拒絕啟動" }
    }
    Assert-M4Settings $settings

    # ---- run 目錄 ----
    $stamp = [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ')
    $run = Join-Path $RunRoot ('run-' + $stamp)
    Assert-M4RunDirectory $run
    if (Test-Path -LiteralPath $run) { throw "run 目錄已存在：$run" }
    New-Item -ItemType Directory -Path $run | Out-Null
    foreach ($name in @('config', 'runtime', 'evidence', 'captures', 'logs', 'lib', 'tmp')) {
        New-Item -ItemType Directory -Path (Join-Path $run $name) | Out-Null
    }

    # ---- 複製並逐檔核對 SHA-256（普通複製，不建硬連結）----
    $receipts = @()
    $copies = @(@{ Source = (Get-Item -LiteralPath $jar); Relative = $jarName })
    foreach ($library in $libraries) { $copies += @{ Source = $library; Relative = ('lib\' + $library.Name) } }
    foreach ($copy in $copies) {
        $source = $copy.Source
        if ($source.LinkType) { throw "來源是連結檔，拒絕複製：$($source.FullName)" }
        $destination = Join-Path $run $copy.Relative
        Copy-Item -LiteralPath $source.FullName -Destination $destination
        $sourceHash = (Get-FileHash -LiteralPath $source.FullName -Algorithm SHA256).Hash
        $copyHash = (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash
        if ($sourceHash -ne $copyHash) { throw "複製後雜湊不符：$($copy.Relative)" }
        $receipts += [pscustomobject]@{ File = $copy.Relative.Replace('\', '/'); Bytes = $source.Length; SHA256 = $sourceHash; CopyVerified = $true }
    }
    $jarSha = $receipts[0].SHA256
    $sha8 = $jarSha.Substring(0, 8).ToLowerInvariant()
    Write-M4Utf8 -Path (Join-Path $run 'artifact-hashes.json') -Text (($receipts | ConvertTo-Json) + "`n")
    $hashLines = @($receipts | ForEach-Object { "$($_.SHA256)  $($_.Bytes)  $($_.File)" })
    Write-M4Utf8 -Path (Join-Path $run 'evidence\artifact-hashes.txt') -Text (($hashLines -join "`n") + "`n")

    # ---- 設定範本（不含帳密）----
    $order = @($script:M4BaselineConfigKeys | Where-Object { @($sourceKeys) -contains $_ }) + @($keyDiff.Added)
    $templateLines = @('# M4 實驗設定範本（m4-prepare.ps1 產生）。複製成 experiment.properties 後，只把 database.user／database.password',
        '# 兩行換成 #83 私密交接檔 isolated-test.properties 的值；其他行不要改。experiment.properties 不得提交、不得貼進報告。')
    $publicLines = @()
    foreach ($key in $order) {
        $templateLines += "$key=$($settings[$key])"
        if ($key -eq 'database.user' -or $key -eq 'database.password') { $publicLines += "$key=[REDACTED]" }
        else { $publicLines += "$key=$($settings[$key])" }
    }
    Write-M4Utf8 -Path (Join-Path $run 'config\experiment.properties.template') -Text (($templateLines -join "`n") + "`n")
    Write-M4Utf8 -Path (Join-Path $run 'evidence\config-public.properties') -Text (($publicLines -join "`n") + "`n")

    # ---- 啟動命令（只產生檔案，不執行）----
    $startLines = @(
        '@echo off',
        'rem M4 experiment server start. Generated by tools/acceptance/m4-prepare.ps1; run it from this run directory only.',
        'if not exist config\experiment.properties goto missing',
        'java -Xms512m -Xmx2048m -XX:ActiveProcessorCount=4 -Dfile.encoding=UTF-8 "-Djava.io.tmpdir=%CD%\tmp" -jar LiTo381-Server.jar config/experiment.properties 2>&1',
        'exit /b %errorlevel%',
        ':missing',
        'echo config\experiment.properties is missing. Copy experiment.properties.template and fill database.user and database.password.',
        'exit /b 2')
    [System.IO.File]::WriteAllText((Join-Path $run 'start-server.cmd'), (($startLines -join "`r`n") + "`r`n"), [System.Text.Encoding]::ASCII)

    # ---- run 目錄必須被 git 忽略（之後會放私密設定）----
    $ignored = '未檢查（找不到 git）'
    $git = Get-Command git -ErrorAction SilentlyContinue
    if ($git) {
        & git -C $repo check-ignore -q -- $run
        if ($LASTEXITCODE -eq 0) { $ignored = '是' }
        else { throw "run 目錄沒有被 git 忽略：$run（私密設定不能放在會被追蹤的位置）" }
    }

    $summary = [ordered]@{
        Run = $run; Utc = $stamp; JarSha256 = $jarSha; JarSha8 = $sha8
        ResultDirectory = "docs/acceptance/m4-$sha8/"; ResultFile = "docs/acceptance/m4-$sha8.md"
        ConfigKeys = @($sourceKeys).Count; ConfigKeysAdded = @($keyDiff.Added); ConfigKeysRemoved = @($keyDiff.Removed)
        StaticDatabase = $StaticDatabase; LitoDatabase = $LitoDatabase; Bind = $Bind; GameBind = $GameBind; Port = $Port
        JdbcBaseUrl = $JdbcBaseUrl; RuntimeEntries = @(Get-ChildItem -LiteralPath (Join-Path $run 'runtime') -Force).Count
        GitIgnored = $ignored; Libraries = @($libraries | ForEach-Object { $_.Name })
    }
    Write-M4Utf8 -Path (Join-Path $run 'preparation.json') -Text ((New-Object psobject -Property $summary | ConvertTo-Json) + "`n")

    Write-Output "M4-PREPARE: PASS"
    Write-Output "Run=$run"
    Write-Output "JarSha256=$jarSha"
    Write-Output "ResultDirectory=docs/acceptance/m4-$sha8/"
    Write-Output "ConfigKeys=$(@($sourceKeys).Count) Added=[$(@($keyDiff.Added) -join ',')] Removed=[$(@($keyDiff.Removed) -join ',')]"
    Write-Output "Next: copy config\experiment.properties.template to config\experiment.properties and fill the two database.* secrets (never commit)."
    exit 0
}
catch {
    Write-Output "M4-PREPARE: FAIL $($_.Exception.Message)"
    exit 1
}

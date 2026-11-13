# M4 驗收：戰績與收據快照（T128／Issue #379）。相容 Windows PowerShell 5.1；UTF-8 BOM。
# 只做 SELECT：lito_source_server、character_lito（含 T103b 新欄位）、lito_battle_settlement，輸出到 <run>\evidence\sql-<Label>.txt。
# 連線固定 127.0.0.1:43324（TCP）；帳密只從私密的 mysql 用戶端設定檔讀（--defaults-extra-file），不出現在命令列或輸出。
# 用法：
#   powershell -NoProfile -ExecutionPolicy Bypass -File tools\acceptance\m4-sql-snapshot.ps1 -RunDirectory <run> -Label before-15 -DefaultsFile <私密 cnf>
#   powershell -NoProfile -ExecutionPolicy Bypass -File tools\acceptance\m4-sql-snapshot.ps1 -RunDirectory <run> -Label after-15 -DefaultsFile <私密 cnf>
# 私密 cnf 的內容（放在 git 忽略的位置，例如 <run>\config\mysql-client.cnf）：
#   [client]
#   user=<#83 實驗帳號>
#   password=<#83 實驗帳號密碼>
param(
    [Parameter(Mandatory = $true)][string]$RunDirectory,
    [Parameter(Mandatory = $true)][string]$Label,
    [Parameter(Mandatory = $true)][string]$DefaultsFile,
    [string]$LitoDatabase = 'p24_lito_lito_exp',
    [string]$AccountDatabase = 'p24_accounts_lito_exp',
    [string]$MysqlExe = 'D:\BinEngine\environments\p24\mysql\bin\mysql.exe',
    [string]$DatabaseHost = '127.0.0.1',
    [int]$DatabasePort = 43324
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'm4-common.ps1')

try {
    $run = (Resolve-Path -LiteralPath $RunDirectory).Path
    Assert-M4RunDirectory $run
    Assert-M4Label $Label
    $sql = Get-M4SnapshotSql -LitoDatabase $LitoDatabase -AccountDatabase $AccountDatabase -Label $Label
    Assert-M4Endpoint -Address $DatabaseHost -Port $DatabasePort -What '資料庫'
    if ($DatabasePort -ne $script:M4DatabasePort) { throw "M4 守門：資料庫埠 $DatabasePort 不是 P24Lab 的 $($script:M4DatabasePort)" }
    Assert-M4NotServerProperties -Path $DefaultsFile -What 'DefaultsFile'
    if (-not (Test-Path -LiteralPath $DefaultsFile -PathType Leaf)) { throw "找不到私密的 mysql 用戶端設定檔：$DefaultsFile" }
    if (-not (Test-Path -LiteralPath $MysqlExe -PathType Leaf)) { throw "找不到 mysql 用戶端：$MysqlExe" }

    $evidence = Join-Path $run 'evidence'
    if (-not (Test-Path -LiteralPath $evidence -PathType Container)) { throw "run 目錄沒有 evidence\：$run" }
    $output = Join-Path $evidence "sql-$Label.txt"
    if (Test-Path -LiteralPath $output) { throw "快照已存在，不覆蓋：$output（同一步驟重做請換 Label，例如 $Label-r2）" }
    $temporary = Join-Path $run 'tmp'
    if (-not (Test-Path -LiteralPath $temporary -PathType Container)) { New-Item -ItemType Directory -Path $temporary | Out-Null }
    $sqlFile = Join-Path $temporary "snapshot-$Label.sql"
    $errorFile = Join-Path $temporary "snapshot-$Label.err.txt"
    Write-M4Utf8 -Path $sqlFile -Text $sql

    # 位元組直接由 mysql 寫進檔案，不經過 PowerShell 管線（避免 5.1 轉碼把中文角色名稱變成問號）。
    $arguments = @(
        ('"--defaults-extra-file=' + (Resolve-Path -LiteralPath $DefaultsFile).Path + '"'),
        '--protocol=TCP', "--host=$DatabaseHost", "--port=$DatabasePort",
        '--default-character-set=utf8mb4', '--batch', '--connect-timeout=10')
    $process = Start-Process -FilePath $MysqlExe -ArgumentList $arguments -NoNewWindow -Wait -PassThru `
        -RedirectStandardInput $sqlFile -RedirectStandardOutput $output -RedirectStandardError $errorFile
    if ($process.ExitCode -ne 0) {
        $message = ''
        if (Test-Path -LiteralPath $errorFile) { $message = (@(Get-Content -LiteralPath $errorFile -Encoding UTF8) -join ' ') }
        if (Test-Path -LiteralPath $output) { Move-Item -LiteralPath $output -Destination ($output + '.failed') -Force }
        throw "mysql 退出碼 $($process.ExitCode)：$message"
    }
    $lines = @([System.IO.File]::ReadAllLines($output, [System.Text.Encoding]::UTF8))
    $parsed = ConvertFrom-M4Snapshot $lines
    if (-not $parsed.ContainsKey('meta') -or -not $parsed.ContainsKey('end')) {
        Move-Item -LiteralPath $output -Destination ($output + '.failed') -Force
        throw '快照輸出不完整（缺 meta 或 end 區段）'
    }
    if ($parsed['meta'][0]['port'] -ne [string]$script:M4DatabasePort) {
        Move-Item -LiteralPath $output -Destination ($output + '.failed') -Force
        throw "M4 守門：實際連到的埠是 $($parsed['meta'][0]['port'])"
    }
    $characters = 0
    if ($parsed.ContainsKey('character_lito')) { $characters = @($parsed['character_lito']).Count }
    $receipts = 0
    if ($parsed.ContainsKey('lito_battle_settlement')) { $receipts = @($parsed['lito_battle_settlement']).Count }
    Write-Output "M4-SQL-SNAPSHOT: PASS label=$Label character_lito=$characters receipts=$receipts utc=$($parsed['meta'][0]['utc'])"
    Write-Output "Output=$output"
    exit 0
}
catch {
    Write-Output "M4-SQL-SNAPSHOT: FAIL $($_.Exception.Message)"
    exit 1
}

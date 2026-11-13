# CI 專用（T128／Issue #379）：在 windows-latest 的 Windows PowerShell 5.1 上，用假檔案跑一遍只碰檔案的腳本。
# 不啟動 Java、不連資料庫：JAR 是幾個位元組的假檔，快照是手寫文字。目的只是確認腳本在 5.1＋StrictMode 下走得完、退出碼正確。
# 本檔以 UTF-8 BOM 儲存。本機不要執行（本機只做語法解析與 m4-selftest.ps1）。
$ErrorActionPreference = 'Stop'
$tools = Split-Path -Parent $PSScriptRoot
$repo = [System.IO.Path]::GetFullPath((Join-Path (Join-Path $tools '..') '..'))
$work = Join-Path $env:RUNNER_TEMP 'm4-dry-run'
New-Item -ItemType Directory -Force -Path $work | Out-Null

function Invoke-Script {
    # 回傳 @{Code=退出碼; Text=輸出}
    param([string]$Name, [string[]]$Arguments)
    $output = & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $tools $Name) @Arguments
    $code = $LASTEXITCODE
    $text = (@($output) -join "`n")
    Write-Host "---- $Name $($Arguments -join ' ') → exit $code"
    Write-Host $text
    return @{ Code = $code; Text = $text }
}
function Assert-True { param([bool]$Condition, [string]$Message) if (-not $Condition) { throw "ASSERT: $Message" } }

Write-Host "PowerShell $($PSVersionTable.PSVersion)"
Assert-True ($PSVersionTable.PSVersion.Major -eq 5) '這個檢查必須在 Windows PowerShell 5.1 執行'

# ---- 假的伺服器目錄：真的 KEYS 原始碼與範例設定＋假 JAR ----
$server = Join-Path $work 'server'
$realServer = Join-Path $repo 'lito伺服器\LiTo381-Server'
New-Item -ItemType Directory -Force -Path (Join-Path $server 'target\lib'), (Join-Path $server 'config'), (Join-Path $server 'src\main\java\local\lito381\server') | Out-Null
[System.IO.File]::WriteAllBytes((Join-Path $server 'target\LiTo381-Server.jar'), [byte[]](1..64))
[System.IO.File]::WriteAllBytes((Join-Path $server 'target\LiTo381-Server-modern.jar'), [byte[]](1..8))
[System.IO.File]::WriteAllBytes((Join-Path $server 'target\lib\mysql-connector-j-8.0.33.jar'), [byte[]](1..32))
[System.IO.File]::WriteAllBytes((Join-Path $server 'target\lib\HikariCP-5.1.0.jar'), [byte[]](1..16))
Copy-Item -LiteralPath (Join-Path $realServer 'config\server.properties.example') -Destination (Join-Path $server 'config\server.properties.example')
Copy-Item -LiteralPath (Join-Path $realServer 'src\main\java\local\lito381\server\UnifiedConfiguration.java') -Destination (Join-Path $server 'src\main\java\local\lito381\server\UnifiedConfiguration.java')

# ---- m4-prepare ----
$runRoot = Join-Path $repo 'scratch\m4'
$result = Invoke-Script 'm4-prepare.ps1' @('-ServerDirectory', $server, '-RunRoot', $runRoot)
Assert-True ($result.Code -eq 0) 'm4-prepare 應該成功'
Assert-True ($result.Text -match 'ConfigKeys=22 Added=\[\] Removed=\[\]') 'KEYS 應該是 22 鍵、沒有增減'
$run = @(Get-ChildItem -LiteralPath $runRoot -Directory -Filter 'run-*')[0].FullName
foreach ($name in @('LiTo381-Server.jar', 'lib\mysql-connector-j-8.0.33.jar', 'lib\HikariCP-5.1.0.jar', 'artifact-hashes.json', 'preparation.json', 'start-server.cmd',
        'config\experiment.properties.template', 'evidence\config-public.properties', 'evidence\artifact-hashes.txt')) {
    Assert-True (Test-Path -LiteralPath (Join-Path $run $name) -PathType Leaf) "run 目錄缺少 $name"
}
Assert-True (-not (Test-Path -LiteralPath (Join-Path $run 'LiTo381-Server-modern.jar'))) '不應複製 modern JAR'
Assert-True (@(Get-ChildItem -LiteralPath (Join-Path $run 'runtime') -Force).Count -eq 0) 'runtime 應該是空的'
$template = Get-Content -LiteralPath (Join-Path $run 'config\experiment.properties.template') -Encoding UTF8
Assert-True (@($template | Where-Object { $_ -match '^[a-z].*=' }).Count -eq 22) '設定範本應該有 22 個鍵'
Assert-True (@($template | Where-Object { $_ -match '^database\.password=__FILL_FROM_ISSUE83_PRIVATE_HANDOFF_PASSWORD__$' }).Count -eq 1) '密碼應該是佔位符'
Assert-True (@($template | Where-Object { $_ -eq 'database.static.database=p24_380db_lito_exp' }).Count -eq 1) '靜態庫應該是 p24_380db_lito_exp'
Assert-True (@($template | Where-Object { $_ -eq 'database.jdbc.base.url=jdbc:mysql://127.0.0.1:43324/' }).Count -eq 1) 'JDBC 應該指向 43324'
$public = Get-Content -LiteralPath (Join-Path $run 'evidence\config-public.properties') -Encoding UTF8
Assert-True (@($public | Where-Object { $_ -eq 'database.password=[REDACTED]' }).Count -eq 1) '公開設定的密碼應該遮蔽'
$expectedHash = (Get-FileHash -LiteralPath (Join-Path $server 'target\LiTo381-Server.jar') -Algorithm SHA256).Hash
Assert-True ($result.Text -match "JarSha256=$expectedHash") 'JAR 雜湊應該相符'
$sha8 = $expectedHash.Substring(0, 8).ToLowerInvariant()

# ---- m4-wal-check ----
Assert-True ((Invoke-Script 'm4-wal-check.ps1' @('-RunDirectory', $run)).Code -eq 0) '沒有 WAL 目錄時應該 PASS'
$wal = Join-Path $run 'runtime\battle-settlements'
New-Item -ItemType Directory -Force -Path $wal | Out-Null
Set-Content -LiteralPath (Join-Path $wal 'owner.lock') -Value 'x'
Assert-True ((Invoke-Script 'm4-wal-check.ps1' @('-RunDirectory', $run)).Code -eq 0) '只有 owner.lock 時應該 PASS'
Set-Content -LiteralPath (Join-Path $wal 'a.pending') -Value 'x'
$result = Invoke-Script 'm4-wal-check.ps1' @('-RunDirectory', $run)
Assert-True ($result.Code -eq 1 -and $result.Text -match 'PENDING: a\.pending') '.pending 應該 FAIL'
Remove-Item -LiteralPath (Join-Path $wal 'a.pending')
$lock = [System.IO.File]::Open((Join-Path $wal 'owner.lock'), 'Open', 'ReadWrite', 'None')
try {
    Assert-True ((Invoke-Script 'm4-wal-check.ps1' @('-RunDirectory', $run)).Code -eq 1) 'owner.lock 被占用時應該 FAIL'
    Assert-True ((Invoke-Script 'm4-wal-check.ps1' @('-RunDirectory', $run, '-AllowLocked')).Code -eq 0) '-AllowLocked 時應該 PASS'
}
finally { $lock.Dispose() }

# ---- m4-sql-diff（手寫快照）----
$tab = "`t"
$meta = @('section', 'label', 'port', 'server_uuid', 'connected_as', 'utc', 'lito_schema', 'character_schema') -join $tab
$chars = @('section', 'char_obj_id', 'char_name', 'wins', 'losses', 'ranking_points', 'disconnects', 'monthly_period', 'monthly_ranking_points', 'monthly_wins', 'monthly_losses', 'version', 'last_result_id', 'updated_at') -join $tab
$receipts = @('section', 'sequence_id', 'result_id', 'battle_id', 'source_database', 'source_name', 'char_obj_id', 'char_name', 'won', 'disconnected', 'applied', 'created_at', 'applied_at') -join $tab
$a = 'a' * 64
$b = 'b' * 64
$before = @('m4_port_ok', '1', $meta, (@('meta', 'before-15', '43324', 'u', 'x@127.0.0.1', '2026-11-13T01:00:00Z', 'p24_lito_lito_exp', 'p24_accounts_lito_exp') -join $tab),
    $chars, (@('character_lito', '1', '甲', '0', '0', '0', '0', '0', '0', '0', '0', '0', '', '2026-11-13 01:00:00') -join $tab), "section${tab}label", "end${tab}before-15")
$after = @('m4_port_ok', '1', $meta, (@('meta', 'after-15', '43324', 'u', 'x@127.0.0.1', '2026-11-13T01:10:00Z', 'p24_lito_lito_exp', 'p24_accounts_lito_exp') -join $tab),
    $chars, (@('character_lito', '1', '甲', '1', '0', '250', '0', '202611', '250', '1', '0', '1', $a, '2026-11-13 01:09:00') -join $tab),
    (@('character_lito', '2', '乙', '0', '1', '50', '0', '202611', '50', '0', '1', '1', $b, '2026-11-13 01:09:00') -join $tab),
    $receipts, (@('lito_battle_settlement', '1', $a, 'battle', 'p24_accounts_lito_exp', 'S', '1', '甲', '1', '0', '1', '2026-11-13 01:09:00', '2026-11-13 01:09:01') -join $tab),
    (@('lito_battle_settlement', '2', $b, 'battle', 'p24_accounts_lito_exp', 'S', '2', '乙', '0', '0', '1', '2026-11-13 01:09:00', '2026-11-13 01:09:01') -join $tab), "section${tab}label", "end${tab}after-15")
$utf8 = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllLines((Join-Path $run 'evidence\sql-before-15.txt'), [string[]]$before, $utf8)
[System.IO.File]::WriteAllLines((Join-Path $run 'evidence\sql-after-15.txt'), [string[]]$after, $utf8)
[System.IO.File]::WriteAllLines((Join-Path $run 'evidence\sql-before-16.txt'), [string[]]$before, $utf8)
[System.IO.File]::WriteAllLines((Join-Path $run 'evidence\sql-after-16.txt'), [string[]]($before | ForEach-Object { $_.Replace('before-15', 'after-16') }), $utf8)
$result = Invoke-Script 'm4-sql-diff.ps1' @('-RunDirectory', $run, '-Step', '15', '-ExpectWinner', '甲', '-ExpectLoser', '乙')
Assert-True ($result.Code -eq 0 -and $result.Text -match 'M4-SQL-DIFF: PASS') '正確的結算應該 PASS'
Assert-True (Test-Path -LiteralPath (Join-Path $run 'evidence\sql-diff-15.txt')) '應該寫出 sql-diff-15.txt'
Assert-True ((Invoke-Script 'm4-sql-diff.ps1' @('-RunDirectory', $run, '-Step', '16')).Code -eq 3) '沒有新收據應該回傳 3（NO-CHANGE）'
Assert-True ((Invoke-Script 'm4-sql-diff.ps1' @('-RunDirectory', $run, '-Step', '16', '-ExpectNoChange')).Code -eq 0) '-ExpectNoChange 時 NO-CHANGE 視為 PASS'
Assert-True ((Invoke-Script 'm4-sql-diff.ps1' @('-RunDirectory', $run, '-Step', '15', '-ExpectWinner', '乙')).Code -eq 1) '勝者不符應該 FAIL'

# ---- m4-sql-snapshot：沒有資料庫，只確認守門與「找不到用戶端」會以非 0 結束 ----
Set-Content -LiteralPath (Join-Path $run 'config\mysql-client.cnf') -Value '[client]'
$result = Invoke-Script 'm4-sql-snapshot.ps1' @('-RunDirectory', $run, '-Label', 'before-99', '-DefaultsFile', (Join-Path $run 'config\mysql-client.cnf'), '-MysqlExe', (Join-Path $work 'no-such-mysql.exe'))
Assert-True ($result.Code -eq 1 -and -not (Test-Path -LiteralPath (Join-Path $run 'evidence\sql-before-99.txt'))) '沒有 mysql 用戶端時應該 FAIL 且不留檔'
$result = Invoke-Script 'm4-sql-snapshot.ps1' @('-RunDirectory', $run, '-Label', 'before-99', '-DefaultsFile', (Join-Path $run 'config\mysql-client.cnf'), '-LitoDatabase', '380_lito')
Assert-True ($result.Code -eq 1 -and $result.Text -match '_lito_exp') '正式庫名應該被守門拒絕'

# ---- m4-listen-check：對不是 java 的程序應該拒絕 ----
$result = Invoke-Script 'm4-listen-check.ps1' @('-ProcessId', [string]$PID, '-RunDirectory', $run)
Assert-True ($result.Code -eq 1 -and $result.Text -match '不是 java') '非 java 程序應該 FAIL'

# ---- m4-evidence-index ----
$acceptance = Join-Path $work 'acceptance'
New-Item -ItemType Directory -Force -Path (Join-Path $acceptance "m4-$sha8") | Out-Null
foreach ($name in @('01-啟動.png', '04-登入大廳.png', 'G22-1-魔杖點地.png', 'V3-藍水.png', 'random.png')) {
    [System.IO.File]::WriteAllBytes((Join-Path $acceptance "m4-$sha8\$name"), [byte[]](1..4))
}
$result = Invoke-Script 'm4-evidence-index.ps1' @('-RunDirectory', $run, '-AcceptanceRoot', $acceptance)
Assert-True ($result.Code -eq 0) 'm4-evidence-index 應該成功'
$resultFile = Join-Path $acceptance "m4-$sha8.md"
$text = [System.IO.File]::ReadAllText($resultFile, [System.Text.Encoding]::UTF8)
foreach ($needle in @('| 01 | 啟動伺服器 | [01-啟動.png]', '| 05 | 八角色翻頁 | — | 未執行 |', '| G22 | GM 魔杖點地傳送 | [G22-1-魔杖點地.png]', '| G20 | 結算畫面龍騎士／幻術師圖示 | — | 未執行 |',
        '| V3 | [V3-藍水.png]', '| 15 | sql-before-15.txt | sql-after-15.txt | M4-SQL-DIFF: FAIL |', '- `random.png`', '| 伺服器主控台 | 未執行（沒有檔案） |')) {
    Assert-True ($text.Contains($needle)) "結果檔骨架缺少：$needle"
}
[System.IO.File]::WriteAllText($resultFile, 'edited by hand', $utf8)
Invoke-Script 'm4-evidence-index.ps1' @('-RunDirectory', $run, '-AcceptanceRoot', $acceptance) | Out-Null
Assert-True ([System.IO.File]::ReadAllText($resultFile) -eq 'edited by hand') '已存在的結果檔不應被覆寫'

# ---- run 目錄不能出現在 git 狀態裡 ----
$status = @(& git -C $repo status --porcelain | Where-Object { $_ -match 'scratch/|docs/acceptance|tools/acceptance' })
Assert-True ($status.Count -eq 0) "run 目錄或產出出現在 git 狀態：$($status -join '; ')"
Write-Host 'M4-CI-DRY-RUN: PASS'
exit 0

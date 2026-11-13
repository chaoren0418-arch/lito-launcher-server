# M4 驗收腳本的純函式自我檢查（T128／Issue #379）。相容 Windows PowerShell 5.1 與 PowerShell 7；UTF-8 BOM。
# 只呼叫 m4-common.ps1 的純函式並讀取 repo 內的原始碼文字：不啟動 Java、不連資料庫、不開任何 socket、不寫任何檔案。
# 另外對 tools\acceptance\*.ps1 做語法解析（[System.Management.Automation.Language.Parser]::ParseFile）。
# 用法：powershell -NoProfile -ExecutionPolicy Bypass -File tools\acceptance\m4-selftest.ps1
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'm4-common.ps1')

$script:passed = 0
$script:failed = @()

function Test-Case {
    param([string]$Name, [scriptblock]$Body)
    try {
        $ok = & $Body
        if ($ok -eq $true) { $script:passed++ } else { $script:failed += "$Name（回傳 $ok）" }
    }
    catch { $script:failed += "$Name（例外：$($_.Exception.Message)）" }
}

function Test-Throws {
    param([scriptblock]$Body)
    try { & $Body | Out-Null; return $false } catch { return $true }
}

# ---- 語法解析 ----
foreach ($file in @(Get-ChildItem -LiteralPath $PSScriptRoot -Filter '*.ps1' | Sort-Object Name)) {
    Test-Case "語法解析 $($file.Name)" {
        $tokens = $null; $errors = $null
        [System.Management.Automation.Language.Parser]::ParseFile($file.FullName, [ref]$tokens, [ref]$errors) | Out-Null
        return (@($errors).Count -eq 0)
    }
    Test-Case "UTF-8 BOM $($file.Name)" {
        $bytes = [System.IO.File]::ReadAllBytes($file.FullName)
        return ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF)
    }
    Test-Case "不使用 && 或 || 管線鏈 $($file.Name)" {
        $tokens = $null; $errors = $null
        [System.Management.Automation.Language.Parser]::ParseFile($file.FullName, [ref]$tokens, [ref]$errors) | Out-Null
        return (@($tokens | Where-Object { $_.Kind.ToString() -eq 'AndAnd' -or $_.Kind.ToString() -eq 'OrOr' }).Count -eq 0)
    }
}

# ---- loopback ----
foreach ($address in @('127.0.0.1', '127.0.0.2', '127.255.255.254', '::1', '[::1]', 'localhost')) {
    Test-Case "loopback 接受 $address" { Test-M4LoopbackAddress $address }
}
foreach ($address in @('0.0.0.0', '::', '192.168.1.5', '10.0.0.1', '8.8.8.8', '', 'example.com', '127.0.0.1.evil.example')) {
    Test-Case "loopback 拒絕 '$address'" { -not (Test-M4LoopbackAddress $address) }
}

# ---- 埠 ----
foreach ($port in @(2000, 3306, 0, 70000)) { Test-Case "埠拒絕 $port" { Test-Throws { Assert-M4Port -Port $port } } }
foreach ($port in @(42083, 42084, 43324)) { Test-Case "埠接受 $port" { -not (Test-Throws { Assert-M4Port -Port $port }) } }
Test-Case '端點拒絕 0.0.0.0:42083' { Test-Throws { Assert-M4Endpoint -Address '0.0.0.0' -Port 42083 } }
Test-Case '端點拒絕 127.0.0.1:3306' { Test-Throws { Assert-M4Endpoint -Address '127.0.0.1' -Port 3306 } }

# ---- config/server.properties ----
foreach ($path in @('config/server.properties', 'config\server.properties', '..\config\server.properties', 'CONFIG\Server.Properties',
        'D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\config\server.properties', 'D:/x/LiTo381-Server/config//server.properties', '"config\server.properties"')) {
    Test-Case "拒絕正式設定路徑 $path" { Test-Throws { Assert-M4NotServerProperties -Path $path } }
}
foreach ($path in @('config\server.properties.example', 'config\experiment.properties', 'scratch\m4\run-20261113T010203Z\config\experiment.properties', 'server.properties.bak\x')) {
    Test-Case "接受非正式設定路徑 $path" { -not (Test-Throws { Assert-M4NotServerProperties -Path $path }) }
}

# ---- JDBC ----
Test-Case 'JDBC 接受 127.0.0.1:43324' { -not (Test-Throws { Assert-M4JdbcUrl 'jdbc:mysql://127.0.0.1:43324/' }) }
foreach ($url in @('jdbc:mysql://127.0.0.1:3306/', 'jdbc:mysql://localhost/', 'jdbc:mysql://127.0.0.1:2000/', 'jdbc:mysql://10.0.0.5:43324/',
        'jdbc:mysql://127.0.0.1:43325/', 'jdbc:mysql://db.example.com:43324/', 'jdbc:mysql://127.0.0.1:43324/380_db', 'jdbc:postgresql://127.0.0.1:43324/')) {
    Test-Case "JDBC 拒絕 $url" { Test-Throws { Assert-M4JdbcUrl $url } }
}

# ---- schema 名稱 ----
# 迴圈變數不能叫 $name：Test-Case 的參數 $Name 會在動態範圍內遮蔽它。
foreach ($schemaName in @('p24_380db_lito_exp', 'p24_lito_lito_exp', 'p24_380lito_lito_exp', 'p24_accounts_lito_exp')) {
    Test-Case "schema 接受 $schemaName" { -not (Test-Throws { Assert-M4ExperimentSchema -Name $schemaName }) }
}
foreach ($schemaName in @('380_db', '380_lito', '380_ac_1', 'p24_world', 'p24_accounts', 'x_lito_exp; DROP DATABASE y', 'p24_lito_exp_backup', '')) {
    Test-Case "schema 拒絕 '$schemaName'" { Test-Throws { Assert-M4ExperimentSchema -Name $schemaName } }
}

# ---- 整組設定 ----
function New-TestSettings {
    return @{
        'bind' = '127.0.0.1'; 'game.bind' = '127.0.0.2'; 'port' = '42083'; 'database.jdbc.base.url' = 'jdbc:mysql://127.0.0.1:43324/'
        'database.static.database' = 'p24_380db_lito_exp'; 'database.lito.database' = 'p24_lito_lito_exp'
        'capture.directory' = '../captures'; 'logs.directory' = '../logs'
    }
}
Test-Case '設定：實驗值通過' { -not (Test-Throws { Assert-M4Settings (New-TestSettings) }) }
Test-Case '設定：bind 0.0.0.0 拒絕' { $s = New-TestSettings; $s['bind'] = '0.0.0.0'; Test-Throws { Assert-M4Settings $s } }
Test-Case '設定：game.bind 非 loopback 拒絕' { $s = New-TestSettings; $s['game.bind'] = '192.168.0.10'; Test-Throws { Assert-M4Settings $s } }
Test-Case '設定：port 2000 拒絕' { $s = New-TestSettings; $s['port'] = '2000'; Test-Throws { Assert-M4Settings $s } }
Test-Case '設定：port 1999（來源埠會落在 2000）拒絕' { $s = New-TestSettings; $s['port'] = '1999'; Test-Throws { Assert-M4Settings $s } }
Test-Case '設定：port 3300（來源埠會落在 3306）拒絕' { $s = New-TestSettings; $s['port'] = '3300'; Test-Throws { Assert-M4Settings $s } }
Test-Case '設定：正式靜態庫名拒絕' { $s = New-TestSettings; $s['database.static.database'] = '380_db'; Test-Throws { Assert-M4Settings $s } }
Test-Case '設定：正式控制庫名拒絕' { $s = New-TestSettings; $s['database.lito.database'] = '380_lito'; Test-Throws { Assert-M4Settings $s } }
Test-Case '設定：DB 3306 拒絕' { $s = New-TestSettings; $s['database.jdbc.base.url'] = 'jdbc:mysql://127.0.0.1:3306/'; Test-Throws { Assert-M4Settings $s } }
Test-Case '設定：缺鍵拒絕' { $s = New-TestSettings; $s.Remove('port'); Test-Throws { Assert-M4Settings $s } }
Test-Case '設定：logs.directory 指向正式設定拒絕' { $s = New-TestSettings; $s['logs.directory'] = '..\config\server.properties'; Test-Throws { Assert-M4Settings $s } }

# ---- run 目錄 ----
Test-Case 'run 目錄接受 scratch\m4\run-<UTC>' { -not (Test-Throws { Assert-M4RunDirectory 'D:\a\scratch\m4\run-20261113T010203Z' }) }
foreach ($path in @('D:\a\lito伺服器\LiTo381-Server\runtime', 'D:\a\scratch\m4', 'D:\a\scratch\t014\run-20261107T012544Z', 'D:\a\scratch\m4\run-20261113T010203Z\..\..\..')) {
    Test-Case "run 目錄拒絕 $path" { Test-Throws { Assert-M4RunDirectory $path } }
}

# ---- 監聽 ----
$expected = Get-M4ExpectedListeners -Bind '127.0.0.1' -GameBind '127.0.0.2' -Port 42083 -SourceId @(1)
$legacy = Get-M4ExpectedListeners -Bind '127.0.0.1' -GameBind '127.0.0.2' -Port 42083 -SourceId @(1) -LegacyTwoOnly
Test-Case '監聽：預期四個（RUNBOOK A02）' { (($expected | Sort-Object) -join ',') -eq '127.0.0.1:42083,127.0.0.1:42084,127.0.0.2:42083,127.0.0.2:42084' }
Test-Case '監聽：-LegacyTwoOnly 是任務原文的兩個' { ($legacy -join ',') -eq '127.0.0.1:42083,127.0.0.2:42084' }
# 函式以「, $陣列」回傳，要先存進變數再接管線（直接接管線會把整個陣列當成一個物件）。
$sourceThree = Get-M4ExpectedListeners -Bind '127.0.0.1' -GameBind '127.0.0.2' -Port 42083 -SourceId @(3)
$twoSources = Get-M4ExpectedListeners -Bind '127.0.0.1' -GameBind '127.0.0.2' -Port 42083 -SourceId @(1, 2)
Test-Case '監聽：source_id 3 → 42086' { (($sourceThree | Sort-Object) -join ',') -eq '127.0.0.1:42083,127.0.0.1:42086,127.0.0.2:42083,127.0.0.2:42086' }
Test-Case '監聽：兩個來源 → 六個' { @($twoSources).Count -eq 6 }
Test-Case '監聽：source_id 9 拒絕' { Test-Throws { Get-M4ExpectedListeners -Bind '127.0.0.1' -GameBind '127.0.0.2' -Port 42083 -SourceId @(9) } }
Test-Case '監聽：剛好四個 → Ok' { (Compare-M4Listeners -Actual $expected -Expected $expected).Ok }
Test-Case '監聽：只有任務原文的兩個 → 少兩個、不通過' { $r = Compare-M4Listeners -Actual $legacy -Expected $expected; (-not $r.Ok) -and @($r.Missing).Count -eq 2 }
Test-Case '監聽：-LegacyTwoOnly 下四個 → 多兩個、不通過' { $r = Compare-M4Listeners -Actual $expected -Expected $legacy; (-not $r.Ok) -and @($r.Unexpected).Count -eq 2 }
Test-Case '監聽：允許清單仍可用（AllowedSeen）' { $r = Compare-M4Listeners -Actual $expected -Expected $legacy -Allowed @('127.0.0.2:42083', '127.0.0.1:42084'); $r.Ok -and @($r.AllowedSeen).Count -eq 2 }
Test-Case '監聽：少一個 → 不通過' { $r = Compare-M4Listeners -Actual @($expected | Select-Object -First 3) -Expected $expected; (-not $r.Ok) -and @($r.Missing).Count -eq 1 }
Test-Case '監聽：0.0.0.0:42083 → 違規' { $r = Compare-M4Listeners -Actual ($expected + @('0.0.0.0:42083')) -Expected $expected; (-not $r.Ok) -and @($r.Violations).Count -eq 1 }
Test-Case '監聽：127.0.0.1:3306 → 違規' { $r = Compare-M4Listeners -Actual ($expected + @('127.0.0.1:3306')) -Expected $expected; (-not $r.Ok) -and @($r.Violations).Count -eq 1 }
Test-Case '監聽：[::]:42083 → 違規' { $r = Compare-M4Listeners -Actual ($expected + @(':::42083')) -Expected $expected; (-not $r.Ok) -and @($r.Violations).Count -eq 1 }
Test-Case '監聽：多一個 loopback 埠 → 不通過' { $r = Compare-M4Listeners -Actual ($expected + @('127.0.0.1:50000')) -Expected $expected; (-not $r.Ok) -and @($r.Unexpected).Count -eq 1 }

# ---- Label 與唯讀 SQL ----
foreach ($label in @('before-15', 'after-15', 'before-16-r2', 'after-G24.1')) { Test-Case "Label 接受 $label" { -not (Test-Throws { Assert-M4Label $label }) } }
foreach ($label in @('during-15', 'before', 'after-', 'before-15/../x', 'before-15;x', "after-15'")) { Test-Case "Label 拒絕 $label" { Test-Throws { Assert-M4Label $label } } }
$snapshotSql = Get-M4SnapshotSql -LitoDatabase 'p24_lito_lito_exp' -AccountDatabase 'p24_accounts_lito_exp' -Label 'before-15'
Test-Case '快照 SQL 通過唯讀檢查' { -not (Test-Throws { Assert-M4SelectOnly $snapshotSql }) }
Test-Case '快照 SQL 含新欄位' { $snapshotSql -match 'ranking_points, disconnects, monthly_period, monthly_ranking_points, monthly_wins, monthly_losses' }
Test-Case '快照 SQL 只碰實驗 schema' { -not ($snapshotSql -match '`(380_|p24_world|p24_accounts`|p24_lito`)') }
Test-Case '快照 SQL 拒絕正式庫名' { Test-Throws { Get-M4SnapshotSql -LitoDatabase '380_lito' -AccountDatabase 'p24_accounts_lito_exp' -Label 'before-15' } }
foreach ($sql in @('UPDATE x SET a = 1', 'SELECT 1; DROP TABLE x', "SELECT * FROM t INTO OUTFILE 'x'", 'DELETE FROM t', 'SELECT 1; INSERT INTO t VALUES (1)',
        'GRANT SELECT ON a.b TO c', 'TRUNCATE t', 'SELECT 1; ALTER TABLE t ADD c INT', 'SHOW TABLES', 'SELECT 1; CALL p()')) {
    Test-Case "唯讀檢查拒絕：$sql" { Test-Throws { Assert-M4SelectOnly $sql } }
}

# ---- 快照比對 ----
$tab = "`t"
$headMeta = @('section', 'label', 'port', 'server_uuid', 'connected_as', 'utc', 'lito_schema', 'character_schema') -join $tab
$headChar = @('section', 'char_obj_id', 'char_name', 'wins', 'losses', 'ranking_points', 'disconnects', 'monthly_period', 'monthly_ranking_points', 'monthly_wins', 'monthly_losses', 'version', 'last_result_id', 'updated_at') -join $tab
$headReceipt = @('section', 'sequence_id', 'result_id', 'battle_id', 'source_database', 'source_name', 'char_obj_id', 'char_name', 'won', 'disconnected', 'applied', 'created_at', 'applied_at') -join $tab
function New-Meta { param([string]$Label) return @('m4_port_ok', '1', $headMeta, (@('meta', $Label, '43324', 'uuid-1', 'lito83_exp@127.0.0.1', '2026-11-13T01:02:03Z', 'p24_lito_lito_exp', 'p24_accounts_lito_exp') -join $tab)) }
function New-CharLine { param([string]$Id, [string]$Name, [long[]]$Values, [string]$Token) return (@('character_lito', $Id, $Name) + @($Values | ForEach-Object { [string]$_ }) + @($Token, '2026-11-13 01:02:03')) -join "`t" }
function New-ReceiptLine { param([string]$Sequence, [string]$Token, [string]$Id, [string]$Name, [string]$Won, [string]$Disconnected, [string]$Applied, [string]$AppliedAt = '2026-11-13 01:05:00') return (@('lito_battle_settlement', $Sequence, $Token, 'battle-1', 'p24_accounts_lito_exp', 'M4', $Id, $Name, $Won, $Disconnected, $Applied, '2026-11-13 01:04:59', $AppliedAt) -join "`t") }
$endLine = @('section', 'label') -join $tab
# wins, losses, ranking_points, disconnects, monthly_period, monthly_ranking_points, monthly_wins, monthly_losses, version
$beforeSnapshot = (New-Meta 'before-15') + @($headChar, (New-CharLine '1' 'A' @(2, 1, 550, 0, 202611, 550, 2, 1, 3) 'old-a'), (New-CharLine '2' 'B' @(0, 0, 20, 0, 202611, 20, 0, 0, 0) ''), $endLine, "end${tab}before-15")
$tokenA = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'
$tokenB = 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb'
$afterWin = (New-Meta 'after-15') + @($headChar, (New-CharLine '1' 'A' @(3, 1, 800, 0, 202611, 800, 3, 1, 4) $tokenA), (New-CharLine '2' 'B' @(0, 1, 70, 0, 202611, 70, 0, 1, 1) $tokenB),
    $headReceipt, (New-ReceiptLine '10' $tokenA '1' 'A' '1' '0' '1'), (New-ReceiptLine '11' $tokenB '2' 'B' '0' '0' '1'), $endLine, "end${tab}after-15")
Test-Case '比對：勝 +250、敗 +50 → PASS' { (Compare-M4Snapshot -Before $beforeSnapshot -After $afterWin).Verdict -eq 'PASS' }
Test-Case '比對：指定勝者／敗者相符 → PASS' { (Compare-M4Snapshot -Before $beforeSnapshot -After $afterWin -ExpectWinner 'A' -ExpectLoser 'B').Verdict -eq 'PASS' }
Test-Case '比對：指定勝者不符 → FAIL' { (Compare-M4Snapshot -Before $beforeSnapshot -After $afterWin -ExpectWinner 'B').Verdict -eq 'FAIL' }
Test-Case '比對：前後相同 → NO-CHANGE' { (Compare-M4Snapshot -Before $beforeSnapshot -After ((New-Meta 'after-15') + @($beforeSnapshot | Select-Object -Skip 4))).Verdict -eq 'NO-CHANGE' }
$afterDisconnect = (New-Meta 'after-16') + @($headChar, (New-CharLine '1' 'A' @(3, 1, 800, 0, 202611, 800, 3, 1, 4) $tokenA), (New-CharLine '2' 'B' @(0, 1, 0, 1, 202611, 0, 0, 1, 1) $tokenB),
    $headReceipt, (New-ReceiptLine '10' $tokenA '1' 'A' '1' '0' '1'), (New-ReceiptLine '11' $tokenB '2' 'B' '0' '1' '1'), $endLine, "end${tab}after-16")
Test-Case '比對：斷線敗 −50（最低 0）、disconnects+1 → PASS' { (Compare-M4Snapshot -Before $beforeSnapshot -After $afterDisconnect).Verdict -eq 'PASS' }
Test-Case '比對：斷線敗卻照一般敗場加分 → FAIL' {
    $bad = (New-Meta 'after-16') + @($headChar, (New-CharLine '1' 'A' @(3, 1, 800, 0, 202611, 800, 3, 1, 4) $tokenA), (New-CharLine '2' 'B' @(0, 1, 70, 0, 202611, 70, 0, 1, 1) $tokenB),
        $headReceipt, (New-ReceiptLine '10' $tokenA '1' 'A' '1' '0' '1'), (New-ReceiptLine '11' $tokenB '2' 'B' '0' '1' '1'), $endLine, "end${tab}after-16")
    (Compare-M4Snapshot -Before $beforeSnapshot -After $bad).Verdict -eq 'FAIL'
}
Test-Case '比對：收據 applied=0 → FAIL' {
    $bad = (New-Meta 'after-15') + @($headChar, (New-CharLine '1' 'A' @(3, 1, 800, 0, 202611, 800, 3, 1, 4) $tokenA), (New-CharLine '2' 'B' @(0, 1, 70, 0, 202611, 70, 0, 1, 1) $tokenB),
        $headReceipt, (New-ReceiptLine '10' $tokenA '1' 'A' '1' '0' '1'), (New-ReceiptLine '11' $tokenB '2' 'B' '0' '0' '0' 'NULL'), $endLine, "end${tab}after-15")
    (Compare-M4Snapshot -Before $beforeSnapshot -After $bad).Verdict -eq 'FAIL'
}
Test-Case '比對：勝場多加一次 → FAIL' {
    $bad = (New-Meta 'after-15') + @($headChar, (New-CharLine '1' 'A' @(4, 1, 1050, 0, 202611, 1050, 4, 1, 5) $tokenA), (New-CharLine '2' 'B' @(0, 1, 70, 0, 202611, 70, 0, 1, 1) $tokenB),
        $headReceipt, (New-ReceiptLine '10' $tokenA '1' 'A' '1' '0' '1'), (New-ReceiptLine '11' $tokenB '2' 'B' '0' '0' '1'), $endLine, "end${tab}after-15")
    (Compare-M4Snapshot -Before $beforeSnapshot -After $bad).Verdict -eq 'FAIL'
}
Test-Case '比對：沒有收據卻改了戰績 → FAIL' {
    $bad = (New-Meta 'after-15') + @($headChar, (New-CharLine '1' 'A' @(3, 1, 800, 0, 202611, 800, 3, 1, 4) 'old-a'), (New-CharLine '2' 'B' @(0, 0, 20, 0, 202611, 20, 0, 0, 0) ''), $endLine, "end${tab}after-15")
    (Compare-M4Snapshot -Before $beforeSnapshot -After $bad).Verdict -eq 'FAIL'
}
Test-Case '比對：換月時月計從 0 起算 → PASS' {
    $next = (New-Meta 'after-15') + @($headChar, (New-CharLine '1' 'A' @(3, 1, 800, 0, 202612, 250, 1, 0, 4) $tokenA), (New-CharLine '2' 'B' @(0, 1, 70, 0, 202612, 50, 0, 1, 1) $tokenB),
        $headReceipt, (New-ReceiptLine '10' $tokenA '1' 'A' '1' '0' '1'), (New-ReceiptLine '11' $tokenB '2' 'B' '0' '0' '1'), $endLine, "end${tab}after-15")
    (Compare-M4Snapshot -Before $beforeSnapshot -After $next).Verdict -eq 'PASS'
}
Test-Case '比對：新角色首場（before 沒有列）→ PASS' {
    $first = (New-Meta 'after-15') + @($headChar, (New-CharLine '1' 'A' @(2, 1, 550, 0, 202611, 550, 2, 1, 3) 'old-a'), (New-CharLine '2' 'B' @(0, 0, 20, 0, 202611, 20, 0, 0, 0) ''),
        (New-CharLine '3' 'C' @(1, 0, 250, 0, 202611, 250, 1, 0, 1) $tokenA), $headReceipt, (New-ReceiptLine '10' $tokenA '3' 'C' '1' '0' '1'), $endLine, "end${tab}after-15")
    (Compare-M4Snapshot -Before $beforeSnapshot -After $first).Verdict -eq 'PASS'
}
Test-Case '比對：檔案不完整（缺 end）→ FAIL' { (Compare-M4Snapshot -Before $beforeSnapshot -After @($afterWin | Select-Object -First 6)).Verdict -eq 'FAIL' }
Test-Case '比對：不同實例 → FAIL' {
    $other = @($afterWin | ForEach-Object { $_.Replace('uuid-1', 'uuid-2') })
    (Compare-M4Snapshot -Before $beforeSnapshot -After $other).Verdict -eq 'FAIL'
}

# ---- WAL、截圖命名 ----
Test-Case 'WAL：只有 owner.lock → 乾淨' { (Get-M4WalStatus -Files @('owner.lock')).Clean }
Test-Case 'WAL：空目錄 → 乾淨' { (Get-M4WalStatus -Files @()).Clean }
Test-Case 'WAL：.pending 與 .tmp → 不乾淨' { $s = Get-M4WalStatus -Files @('owner.lock', 'a.pending', 'sub\b.TMP'); (-not $s.Clean) -and @($s.Pending).Count -eq 1 -and @($s.Temporary).Count -eq 1 }
Test-Case '截圖：A 部' { $k = Get-M4ScreenshotKind '04-登入大廳.png'; $k.Part -eq 'A' -and $k.Key -eq '04' }
Test-Case '截圖：B 部' { $k = Get-M4ScreenshotKind 'G22-1-魔杖點地.png'; $k.Part -eq 'B' -and $k.Key -eq 'G22' }
Test-Case '截圖：C 部' { $k = Get-M4ScreenshotKind 'V7-藍水.png'; $k.Part -eq 'C' -and $k.Key -eq 'V7' }
Test-Case '截圖：不符契約' { (Get-M4ScreenshotKind 'screenshot.png').Part -eq '?' }

# ---- 設定鍵（讀 repo 內的原始碼文字）----
$javaFile = Join-Path (Get-M4RepoRoot $PSScriptRoot) 'lito伺服器\LiTo381-Server\src\main\java\local\lito381\server\UnifiedConfiguration.java'
if (-not (Test-Path -LiteralPath $javaFile)) { $javaFile = Join-Path (Get-M4RepoRoot $PSScriptRoot) 'lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/UnifiedConfiguration.java' }
$sourceKeys = Get-M4ConfigKeysFromSource $javaFile
Test-Case 'KEYS：可從原始碼取出' { $sourceKeys -ne $null -and @($sourceKeys).Count -gt 0 }
Test-Case 'KEYS：與 #284 的 22 鍵相同（有增減時本項失敗，請更新 m4-common.ps1 與 README）' {
    $diff = Compare-M4KeySets -Baseline $script:M4BaselineConfigKeys -Actual $sourceKeys
    @($sourceKeys).Count -eq 22 -and @($diff.Added).Count -eq 0 -and @($diff.Removed).Count -eq 0
}
Test-Case 'KEYS：增減偵測' { $d = Compare-M4KeySets -Baseline @('a', 'b') -Actual @('b', 'c'); (@($d.Added) -join ',') -eq 'c' -and (@($d.Removed) -join ',') -eq 'a' }

$total = $script:passed + @($script:failed).Count
foreach ($failure in @($script:failed)) { Write-Output "FAIL: $failure" }
if (@($script:failed).Count -eq 0) { Write-Output "M4-SELFTEST: PASS $($script:passed)/$total"; exit 0 }
Write-Output "M4-SELFTEST: FAIL $($script:passed)/$total"
exit 1

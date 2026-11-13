# M4 驗收腳本共用函式（T128／Issue #379）。相容 Windows PowerShell 5.1；本檔以 UTF-8 BOM 儲存。
# 只有函式定義，dot-source 後不會連線、不會啟動任何程式。
# 共用守門：埠 2000／3306、非 loopback 位址、config/server.properties 路徑一律拒絕（throw → 呼叫端退出碼非 0）。

Set-StrictMode -Version 2.0

$script:M4ForbiddenPorts = @(2000, 3306)
$script:M4DatabasePort = 43324
$script:M4ExperimentSchemaPattern = '^[A-Za-z0-9_]+_lito_exp$'
$script:M4RunDirectoryPattern = '[\\/]scratch[\\/]m4[\\/]run-\d{8}T\d{6}Z[\\/]?$'
$script:M4PlaceholderUser = '__FILL_FROM_ISSUE83_PRIVATE_HANDOFF_USER__'
$script:M4PlaceholderPassword = '__FILL_FROM_ISSUE83_PRIVATE_HANDOFF_PASSWORD__'

# 正式 0.6.71 JAR 的 UnifiedConfiguration.KEYS（#284 verify-run.ps1 的 22 鍵）；m4-prepare.ps1 會再對照原始碼。
$script:M4BaselineConfigKeys = @(
    'bind', 'game.bind', 'port', 'server.name', 'database.jdbc.base.url', 'database.jdbc.options',
    'database.user', 'database.password', 'database.static.database', 'database.lito.database',
    'weight.rate', 'capture.directory', 'battle.ai.enabled', 'battle.physical.hit.enabled',
    'battle.natural.mp.regen.enabled', 'native.siege.roster.enforced', 'native.tournament.allowance',
    'memory.initial', 'memory.maximum', 'cpu.limit', 'logs.directory', 'startup.timeout.seconds')

function Test-M4LoopbackAddress {
    param([string]$Address)
    if ([string]::IsNullOrWhiteSpace($Address)) { return $false }
    $text = $Address.Trim().Trim('[', ']')
    if ($text -ieq 'localhost') { return $true }
    $ip = $null
    if (-not [System.Net.IPAddress]::TryParse($text, [ref]$ip)) { return $false }
    return [System.Net.IPAddress]::IsLoopback($ip)
}

function Assert-M4Loopback {
    param([string]$Address, [string]$What = '位址')
    if (-not (Test-M4LoopbackAddress $Address)) {
        throw "M4 守門：$What '$Address' 不是 loopback 位址（只允許 127.0.0.0/8、::1、localhost）"
    }
}

function Assert-M4Port {
    param([int]$Port, [string]$What = '埠')
    if ($script:M4ForbiddenPorts -contains $Port) {
        throw "M4 守門：$What $Port 是正式服務埠（2000／3306），拒絕執行"
    }
    if ($Port -lt 1 -or $Port -gt 65535) { throw "M4 守門：$What $Port 超出範圍" }
}

function Assert-M4Endpoint {
    param([string]$Address, [int]$Port, [string]$What = '端點')
    Assert-M4Loopback -Address $Address -What "$What 位址"
    Assert-M4Port -Port $Port -What "$What 埠"
}

function Test-M4ServerPropertiesPath {
    param([string]$Path)
    if ([string]::IsNullOrWhiteSpace($Path)) { return $false }
    $normalized = $Path.Trim().Trim('"').Replace('/', '\')
    while ($normalized.Contains('\\')) { $normalized = $normalized.Replace('\\', '\') }
    return ($normalized -match '(?i)(^|\\)config\\server\.properties$')
}

function Assert-M4NotServerProperties {
    param([string]$Path, [string]$What = '路徑')
    if (Test-M4ServerPropertiesPath $Path) {
        throw "M4 守門：$What '$Path' 是正式設定 config/server.properties，拒絕讀寫"
    }
}

function Assert-M4ExperimentSchema {
    param([string]$Name, [string]$What = 'schema')
    if ($Name -notmatch $script:M4ExperimentSchemaPattern) {
        throw "M4 守門：$What '$Name' 不是以 _lito_exp 結尾的實驗 schema"
    }
}

function ConvertFrom-M4JdbcUrl {
    # jdbc:mysql://host[:port]/ → @{Host=..; Port=..}；沒寫埠視為 3306（會被守門擋下）。
    param([string]$Url)
    if ($Url -notmatch '^jdbc:mysql://(\[[0-9A-Fa-f:]+\]|[^/:\[\]]+)(?::(\d+))?/$') {
        throw "M4 守門：JDBC 位址格式不符（需要 jdbc:mysql://主機:埠/）：'$Url'"
    }
    $port = 3306
    if ($Matches[2]) { $port = [int]$Matches[2] }
    return @{ Host = $Matches[1]; Port = $port }
}

function Assert-M4JdbcUrl {
    param([string]$Url)
    $parsed = ConvertFrom-M4JdbcUrl $Url
    Assert-M4Endpoint -Address $parsed.Host -Port $parsed.Port -What '資料庫'
    if ($parsed.Port -ne $script:M4DatabasePort) {
        throw "M4 守門：資料庫埠 $($parsed.Port) 不是 P24Lab 的 $($script:M4DatabasePort)"
    }
}

function Assert-M4RunDirectory {
    # run 目錄必須是 <某處>\scratch\m4\run-<UTC>；避免把腳本指到伺服器正式 runtime 或其他目錄。
    param([string]$Path)
    Assert-M4NotServerProperties -Path $Path -What 'run 目錄'
    if ($Path -notmatch $script:M4RunDirectoryPattern) {
        throw "M4 守門：'$Path' 不是 scratch\m4\run-<UTC> 目錄"
    }
}

function Read-M4Properties {
    # 讀 Java properties（key=value，# 或 ! 開頭為註解）；重複鍵視為錯誤。不處理續行。
    param([string]$Path)
    Assert-M4NotServerProperties -Path $Path -What '設定檔'
    $settings = @{}
    foreach ($line in [System.IO.File]::ReadAllLines($Path, [System.Text.Encoding]::UTF8)) {
        if ($line -match '^\s*(#|!|$)') { continue }
        $index = $line.IndexOf('=')
        if ($index -lt 1) { throw "設定檔有無法解析的行：$line" }
        $key = $line.Substring(0, $index).Trim()
        if ($settings.ContainsKey($key)) { throw "設定檔有重複的鍵：$key" }
        $settings[$key] = $line.Substring($index + 1).Replace('\:', ':').Replace('\=', '=')
    }
    return $settings
}

function Assert-M4Settings {
    # 實驗設定的守門：兩個監聽位址是 loopback、基準埠不是正式埠、DB 是 127.0.0.1:43324、兩個庫名是實驗 schema。
    param([hashtable]$Settings)
    foreach ($key in @('bind', 'game.bind', 'port', 'database.jdbc.base.url', 'database.static.database', 'database.lito.database')) {
        if (-not $Settings.ContainsKey($key)) { throw "M4 守門：設定缺少 $key" }
    }
    Assert-M4Loopback -Address $Settings['bind'] -What 'bind'
    Assert-M4Loopback -Address $Settings['game.bind'] -What 'game.bind'
    $port = 0
    if (-not [int]::TryParse($Settings['port'], [ref]$port)) { throw "M4 守門：port 不是整數：$($Settings['port'])" }
    for ($offset = 0; $offset -le 8; $offset++) { Assert-M4Port -Port ($port + $offset) -What "port+$offset" }
    Assert-M4JdbcUrl $Settings['database.jdbc.base.url']
    Assert-M4ExperimentSchema -Name $Settings['database.static.database'] -What 'database.static.database'
    Assert-M4ExperimentSchema -Name $Settings['database.lito.database'] -What 'database.lito.database'
    foreach ($key in @('capture.directory', 'logs.directory')) {
        if ($Settings.ContainsKey($key)) { Assert-M4NotServerProperties -Path $Settings[$key] -What $key }
    }
}

function Get-M4ConfigKeysFromSource {
    # 從 UnifiedConfiguration.java 靜態取出 KEYS 的字串常數（不執行 Java）。找不到宣告時回傳 $null。
    param([string]$JavaFile)
    if (-not (Test-Path -LiteralPath $JavaFile)) { return $null }
    $text = [System.IO.File]::ReadAllText($JavaFile, [System.Text.Encoding]::UTF8)
    $match = [regex]::Match($text, '(?s)\bKEYS\s*=\s*(?:new\s+\w+(?:<[^>]*>)?\s*\()?\s*(?:Set|List|Arrays)\s*\.\s*\w+\s*\((.*?)\)\s*\)?\s*;')
    if (-not $match.Success) { return $null }
    $keys = @()
    foreach ($literal in [regex]::Matches($match.Groups[1].Value, '"([^"\\]+)"')) { $keys += $literal.Groups[1].Value }
    return , $keys
}

function Compare-M4KeySets {
    # 回傳 @{Added=..; Removed=..}：相對基準（22 鍵）多出與減少的鍵。
    param([string[]]$Baseline, [string[]]$Actual)
    $added = @($Actual | Where-Object { $Baseline -notcontains $_ } | Sort-Object)
    $removed = @($Baseline | Where-Object { $Actual -notcontains $_ } | Sort-Object)
    return @{ Added = $added; Removed = $removed }
}

function Get-M4ExpectedListeners {
    # 必要（CHECKLIST 02）：登入 bind:port；每個啟用來源 game.bind:(port+source_id)。
    param([string]$Bind, [string]$GameBind, [int]$Port, [int[]]$SourceId)
    Assert-M4Endpoint -Address $Bind -Port $Port -What '登入監聽'
    $expected = @("${Bind}:$Port")
    foreach ($id in $SourceId) {
        if ($id -lt 1 -or $id -gt 8) { throw "M4 守門：source_id $id 不在 1..8" }
        Assert-M4Endpoint -Address $GameBind -Port ($Port + $id) -What "來源 $id 監聽"
        $expected += "${GameBind}:$($Port + $id)"
    }
    return , $expected
}

function Get-M4AllowedListeners {
    # 依原始碼靜態閱讀，伺服器另外還會開的 loopback 監聽（未實機確認）：
    #   game.bind:port                 Main.java:74-80（channelPorts＝[port]，ServerConfig.java:266）
    #   bind:(port+source_id)          SourceLogin.java:47-50（每個來源的 LOGIN 與 GAME 端點各一個 socket）
    # m4-listen-check.ps1 預設把這些列為「允許」；加 -Strict 時只接受必要清單。
    param([string]$Bind, [string]$GameBind, [int]$Port, [int[]]$SourceId)
    Assert-M4Endpoint -Address $GameBind -Port $Port -What '遊戲監聽'
    $allowed = @("${GameBind}:$Port")
    foreach ($id in $SourceId) {
        Assert-M4Endpoint -Address $Bind -Port ($Port + $id) -What "來源 $id 登入監聽"
        $allowed += "${Bind}:$($Port + $id)"
    }
    return , $allowed
}

function Compare-M4Listeners {
    # $Actual／$Expected／$Allowed：'位址:埠' 字串。回傳 Ok、Unexpected（不在必要也不在允許清單）、Missing（必要但沒有）、
    # AllowedSeen（允許清單中實際出現的）、Violations（碰到正式埠或非 loopback）。
    param([string[]]$Actual, [string[]]$Expected, [string[]]$Allowed = @())
    $actualSet = @($Actual | Sort-Object -Unique)
    $unexpected = @($actualSet | Where-Object { $Expected -notcontains $_ -and $Allowed -notcontains $_ })
    $allowedSeen = @($actualSet | Where-Object { $Allowed -contains $_ -and $Expected -notcontains $_ })
    $missing = @($Expected | Where-Object { $actualSet -notcontains $_ })
    $violations = @()
    foreach ($entry in $actualSet) {
        $index = $entry.LastIndexOf(':')
        $address = $entry.Substring(0, $index)
        $port = [int]$entry.Substring($index + 1)
        if (-not (Test-M4LoopbackAddress $address)) { $violations += "$entry（非 loopback）" }
        if ($script:M4ForbiddenPorts -contains $port) { $violations += "$entry（正式埠）" }
    }
    return @{
        Ok = ($unexpected.Count -eq 0 -and $missing.Count -eq 0 -and $violations.Count -eq 0)
        Unexpected = $unexpected; Missing = $missing; AllowedSeen = $allowedSeen; Violations = $violations
    }
}

function Assert-M4Label {
    param([string]$Label)
    if ($Label -notmatch '^(before|after)-[A-Za-z0-9][A-Za-z0-9_.-]*$') {
        throw "Label 必須是 before-<步驟> 或 after-<步驟>（英數、底線、點、連字號）：'$Label'"
    }
}

function Assert-M4SelectOnly {
    # 快照 SQL 只允許 SELECT／SET NAMES／SET @變數／PREPARE-EXECUTE-DEALLOCATE（守門用）；出現任何寫入或 DDL 關鍵字就拒絕。
    param([string]$Sql)
    foreach ($statement in ($Sql -split ';')) {
        $text = $statement.Trim()
        if ($text -eq '') { continue }
        if ($text -notmatch '^(?i)(SELECT|SET\s+NAMES|SET\s+@|PREPARE\s|EXECUTE\s|DEALLOCATE\s)') {
            throw "M4 守門：快照只允許 SELECT，拒絕：$($text.Substring(0, [Math]::Min(60, $text.Length)))"
        }
        if ($text -match '(?i)\b(INSERT|UPDATE|DELETE|REPLACE|ALTER|DROP|CREATE|TRUNCATE|GRANT|REVOKE|RENAME|LOAD|CALL|HANDLER|LOCK|INTO\s+OUTFILE|INTO\s+DUMPFILE)\b') {
            throw "M4 守門：快照 SQL 含寫入或 DDL 關鍵字，拒絕：$($text.Substring(0, [Math]::Min(60, $text.Length)))"
        }
    }
}

function Get-M4SnapshotSql {
    # 快照查詢全文（唯讀）。每個查詢第一欄是區段名稱，m4-sql-diff.ps1 靠它分段。
    param([string]$LitoDatabase, [string]$AccountDatabase, [string]$Label)
    Assert-M4ExperimentSchema -Name $LitoDatabase -What '控制庫'
    Assert-M4ExperimentSchema -Name $AccountDatabase -What '角色庫'
    Assert-M4Label $Label
    $lines = @(
        'SET NAMES utf8mb4',
        "SET @m4_sql = IF(@@port = $($script:M4DatabasePort), 'SELECT 1 AS m4_port_ok', 'SELECT * FROM ``m4_guard__port_is_not_43324_not_p24lab``.``guard``')",
        'PREPARE m4_stmt FROM @m4_sql',
        'EXECUTE m4_stmt',
        'DEALLOCATE PREPARE m4_stmt',
        "SELECT 'meta' AS section, '$Label' AS label, @@port AS port, @@server_uuid AS server_uuid, CURRENT_USER() AS connected_as, DATE_FORMAT(UTC_TIMESTAMP(), '%Y-%m-%dT%H:%i:%sZ') AS utc, '$LitoDatabase' AS lito_schema, '$AccountDatabase' AS character_schema",
        "SELECT 'lito_source_server' AS section, source_id, display_name, character_database, enabled FROM ``$LitoDatabase``.``lito_source_server`` ORDER BY source_id",
        "SELECT 'character_lito' AS section, char_obj_id, char_name, wins, losses, ranking_points, disconnects, monthly_period, monthly_ranking_points, monthly_wins, monthly_losses, version, last_result_id, updated_at FROM ``$AccountDatabase``.``character_lito`` ORDER BY char_obj_id",
        "SELECT 'lito_battle_settlement' AS section, sequence_id, result_id, battle_id, source_database, source_name, char_obj_id, char_name, won, disconnected, applied, created_at, applied_at FROM ``$LitoDatabase``.``lito_battle_settlement`` ORDER BY sequence_id",
        "SELECT 'end' AS section, '$Label' AS label"
    )
    $sql = ($lines -join ";`n") + ";`n"
    Assert-M4SelectOnly $sql
    return $sql
}

function ConvertFrom-M4Snapshot {
    # 解析 mysql --batch 輸出（tab 分隔、每個結果集先一列欄名）。回傳 @{區段名 = @(每列一個 hashtable)}。
    param([string[]]$Lines)
    $sections = @{}
    $header = $null
    foreach ($line in $Lines) {
        if ($line -eq $null -or $line -eq '') { continue }
        $cells = $line.TrimStart([char]0xFEFF).Split("`t")
        if ($cells[0] -eq 'section') { $header = $cells; continue }
        if ($cells[0] -eq 'm4_port_ok' -or $header -eq $null) { continue }
        if ($cells.Count -ne $header.Count) { continue }
        $row = @{}
        for ($i = 0; $i -lt $header.Count; $i++) { $row[$header[$i]] = $cells[$i] }
        $name = $cells[0]
        if (-not $sections.ContainsKey($name)) { $sections[$name] = @() }
        $sections[$name] += $row
    }
    return $sections
}

function Compare-M4Snapshot {
    # 比對兩份快照，判定「勝者 wins+1、敗者 losses+1、積分增量、收據 applied=1」。
    # 規則同 SettlementSql.next（SettlementSql.java:29-38）：勝 +WinReward；敗 +LossReward；斷線敗 −LossReward、disconnects+1；
    # 積分最低 0；月份（monthly_period）相同時月計同步累加，換月時月計從 0 起算。
    # 回傳 @{Verdict='PASS'|'FAIL'|'NO-CHANGE'; Lines=@(文字報告); Failures=@()}。
    param([string[]]$Before, [string[]]$After, [long]$WinReward = 250, [long]$LossReward = 50,
          [string]$ExpectWinner = '', [string]$ExpectLoser = '')
    $b = ConvertFrom-M4Snapshot $Before
    $a = ConvertFrom-M4Snapshot $After
    $lines = @()
    $failures = @()
    foreach ($side in @(@('before', $b), @('after', $a))) {
        foreach ($need in @('meta', 'character_lito', 'end')) {
            if (-not $side[1].ContainsKey($need)) {
                if ($need -eq 'character_lito' -and $side[1].ContainsKey('end')) { continue }
                $failures += "$($side[0]) 快照缺少區段 $need（檔案不完整或不是 m4-sql-snapshot.ps1 的輸出）"
            }
        }
    }
    if ($failures.Count -gt 0) { return @{ Verdict = 'FAIL'; Lines = $lines; Failures = $failures } }
    if ($b['meta'][0]['server_uuid'] -ne $a['meta'][0]['server_uuid'] -or $b['meta'][0]['character_schema'] -ne $a['meta'][0]['character_schema'] `
            -or $b['meta'][0]['lito_schema'] -ne $a['meta'][0]['lito_schema']) {
        $failures += '兩份快照不是同一個實例或同一組 schema'
        return @{ Verdict = 'FAIL'; Lines = $lines; Failures = $failures }
    }
    $schema = $a['meta'][0]['character_schema']
    $beforeChars = @{}
    if ($b.ContainsKey('character_lito')) { foreach ($row in $b['character_lito']) { $beforeChars[$row['char_obj_id']] = $row } }
    $afterChars = @{}
    if ($a.ContainsKey('character_lito')) { foreach ($row in $a['character_lito']) { $afterChars[$row['char_obj_id']] = $row } }
    $known = @{}
    if ($b.ContainsKey('lito_battle_settlement')) { foreach ($row in $b['lito_battle_settlement']) { $known[$row['result_id']] = $row } }
    $newReceipts = @()
    if ($a.ContainsKey('lito_battle_settlement')) {
        foreach ($row in $a['lito_battle_settlement']) {
            if (-not $known.ContainsKey($row['result_id'])) { $newReceipts += $row }
            elseif ($known[$row['result_id']]['applied'] -ne $row['applied']) {
                $lines += "既有收據 $($row['result_id'].Substring(0, 12))… applied $($known[$row['result_id']]['applied']) → $($row['applied'])"
            }
        }
    }
    $newReceipts = @($newReceipts | Sort-Object { [long]$_['sequence_id'] })
    $lines += "新收據 $($newReceipts.Count) 筆；before=$($b['meta'][0]['label']) $($b['meta'][0]['utc'])；after=$($a['meta'][0]['label']) $($a['meta'][0]['utc'])"
    $counters = @('wins', 'losses', 'ranking_points', 'disconnects', 'monthly_period', 'monthly_ranking_points', 'monthly_wins', 'monthly_losses', 'version')
    $expected = @{}
    $lastToken = @{}
    $winners = @()
    $losers = @()
    foreach ($receipt in $newReceipts) {
        $id = $receipt['char_obj_id']
        $short = $receipt['result_id'].Substring(0, [Math]::Min(12, $receipt['result_id'].Length))
        $lines += "收據 seq=$($receipt['sequence_id']) battle=$($receipt['battle_id']) char=$id($($receipt['char_name'])) won=$($receipt['won']) disconnected=$($receipt['disconnected']) applied=$($receipt['applied']) result=$short…"
        if ($receipt['applied'] -ne '1') { $failures += "收據 $short…（角色 $id）applied=$($receipt['applied'])，預期 1" }
        if ($receipt['applied'] -eq '1' -and ($receipt['applied_at'] -eq 'NULL' -or $receipt['applied_at'] -eq '')) { $failures += "收據 $short… applied=1 但 applied_at 是 NULL" }
        if ($receipt['source_database'] -ne $schema) { $failures += "收據 $short… 的 source_database=$($receipt['source_database'])，不是 $schema" }
        if (-not $expected.ContainsKey($id)) {
            $state = @{}
            foreach ($name in $counters) {
                if ($beforeChars.ContainsKey($id)) { $state[$name] = [long]$beforeChars[$id][$name] } else { $state[$name] = [long]0 }
            }
            $expected[$id] = $state
        }
        $state = $expected[$id]
        $won = ($receipt['won'] -eq '1')
        $disconnected = ($receipt['disconnected'] -eq '1' -and -not $won)
        $reward = $LossReward
        if ($won) { $reward = $WinReward }
        $delta = $reward
        if ($disconnected) { $delta = - $reward }
        $period = [long]0
        if ($afterChars.ContainsKey($id)) { $period = [long]$afterChars[$id]['monthly_period'] }
        if ($state['monthly_period'] -ne $period) {
            $state['monthly_period'] = $period; $state['monthly_ranking_points'] = [long]0; $state['monthly_wins'] = [long]0; $state['monthly_losses'] = [long]0
        }
        if ($won) { $state['wins'] += 1; $state['monthly_wins'] += 1; $winners += $receipt['char_name'] }
        else { $state['losses'] += 1; $state['monthly_losses'] += 1; $losers += $receipt['char_name'] }
        if ($disconnected) { $state['disconnects'] += 1 }
        $state['ranking_points'] = [Math]::Max([long]0, $state['ranking_points'] + $delta)
        $state['monthly_ranking_points'] = [Math]::Max([long]0, $state['monthly_ranking_points'] + $delta)
        $state['version'] += 1
        $lastToken[$id] = $receipt['result_id']
    }
    foreach ($id in ($expected.Keys | Sort-Object)) {
        if (-not $afterChars.ContainsKey($id)) { $failures += "角色 $id 有新收據，但 after 快照沒有 character_lito 列"; continue }
        $row = $afterChars[$id]
        $parts = @()
        foreach ($name in $counters) {
            $old = [long]0
            if ($beforeChars.ContainsKey($id)) { $old = [long]$beforeChars[$id][$name] }
            $parts += "$name $old→$($row[$name])"
            if ([long]$row[$name] -ne $expected[$id][$name]) {
                $failures += "角色 $id($($row['char_name'])) $name：before=$old after=$($row[$name]) 預期=$($expected[$id][$name])"
            }
        }
        $lines += "角色 $id($($row['char_name']))：" + ($parts -join '、')
        if ($row['last_result_id'] -ne $lastToken[$id]) { $failures += "角色 $id 的 last_result_id 不是最後一筆新收據" }
    }
    foreach ($id in ($afterChars.Keys | Sort-Object)) {
        if ($expected.ContainsKey($id)) { continue }
        if (-not $beforeChars.ContainsKey($id)) { $lines += "角色 $id 是新出現的 character_lito 列，但沒有對應的新收據"; $failures += "角色 $id 新增了戰績列卻沒有新收據"; continue }
        foreach ($name in ($counters + @('last_result_id'))) {
            if ($beforeChars[$id][$name] -ne $afterChars[$id][$name]) { $failures += "角色 $id 沒有新收據，但 $name 由 $($beforeChars[$id][$name]) 變成 $($afterChars[$id][$name])" }
        }
    }
    foreach ($id in $beforeChars.Keys) { if (-not $afterChars.ContainsKey($id)) { $failures += "角色 $id 的 character_lito 列在 after 快照消失" } }
    if ($ExpectWinner -ne '' -and $winners -notcontains $ExpectWinner) { $failures += "預期勝者 $ExpectWinner，新收據的勝者是：$($winners -join '、')" }
    if ($ExpectLoser -ne '' -and $losers -notcontains $ExpectLoser) { $failures += "預期敗者 $ExpectLoser，新收據的敗者是：$($losers -join '、')" }
    $verdict = 'PASS'
    if ($failures.Count -gt 0) { $verdict = 'FAIL' }
    elseif ($newReceipts.Count -eq 0) { $verdict = 'NO-CHANGE' }
    return @{ Verdict = $verdict; Lines = $lines; Failures = $failures }
}

function Get-M4WalStatus {
    # $Files：battle-settlements 目錄下的相對路徑清單。回傳待補（.pending）與暫存（.tmp）檔。
    param([string[]]$Files)
    $pending = @($Files | Where-Object { $_ -match '(?i)\.pending$' })
    $temporary = @($Files | Where-Object { $_ -match '(?i)\.tmp$' })
    return @{ Pending = $pending; Temporary = $temporary; Clean = ($pending.Count -eq 0 -and $temporary.Count -eq 0) }
}

function Get-M4ScreenshotKind {
    # T127 命名契約：A 部 NN-<步驟>.png、B 部 G<編號>-<序>-<短名>.png、C 部 V<序>-<短名>.png。
    param([string]$FileName)
    if ($FileName -match '^(\d{2})-(.+)\.png$') { return @{ Part = 'A'; Key = $Matches[1]; Name = $Matches[2] } }
    if ($FileName -match '^G(\d{2})-(\d+)-(.+)\.png$') { return @{ Part = 'B'; Key = "G$($Matches[1])"; Sequence = $Matches[2]; Name = $Matches[3] } }
    if ($FileName -match '^V(\d+)-(.+)\.png$') { return @{ Part = 'C'; Key = "V$($Matches[1])"; Name = $Matches[2] } }
    return @{ Part = '?'; Key = ''; Name = $FileName }
}

function Write-M4Utf8 {
    # 以 UTF-8（無 BOM）寫檔；mysql 用戶端與 git diff 都不需要 BOM。
    param([string]$Path, [string]$Text)
    Assert-M4NotServerProperties -Path $Path -What '輸出檔'
    [System.IO.File]::WriteAllText($Path, $Text, (New-Object System.Text.UTF8Encoding($false)))
}

function Get-M4RepoRoot {
    # tools\acceptance 的上兩層。
    param([string]$ScriptRoot)
    return [System.IO.Path]::GetFullPath((Join-Path (Join-Path $ScriptRoot '..') '..'))
}

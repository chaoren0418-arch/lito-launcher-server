# M4 驗收：掃描截圖與證據，產生結果檔骨架（T128／Issue #379）。相容 Windows PowerShell 5.1；UTF-8 BOM。
# 命名契約（T127）：版本目錄 docs/acceptance/m4-<JAR SHA-256 前 8 碼>/、結果檔 docs/acceptance/m4-<前 8 碼>.md；
#   截圖 A 部 NN-<步驟>.png、B 部 G<編號>-<序>-<短名>.png、C 部 V<序>-<短名>.png；
#   原始證據在 <run>\evidence\，SQL 快照 sql-<before|after>-<步驟>.txt。
# 只讀取檔名與大小，不開啟、不修改截圖。缺檔一律列為「未執行」，不補造。
# 輸出：<run>\evidence\evidence-index.md（每次覆寫）；結果檔不存在時另外建立骨架，已存在就不動（加 -Force 才覆寫）。
# 用法：
#   powershell -NoProfile -ExecutionPolicy Bypass -File tools\acceptance\m4-evidence-index.ps1 -RunDirectory <run>
param(
    [Parameter(Mandatory = $true)][string]$RunDirectory,
    [string]$AcceptanceRoot = '',
    [switch]$Force
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'm4-common.ps1')

# CHECKLIST 01–21 的步驟短名（docs/acceptance/CHECKLIST.md）；B 部為 customer-gaps 的 G19–G27 與 G02。
$stepsA = [ordered]@{
    '01' = '啟動伺服器'; '02' = '監聽位址與來源路由'; '03' = '登入器與伺服器選擇'; '04' = '帳號 A 登入進大廳'; '05' = '八角色翻頁'
    '06' = '相容層載入核對'; '07' = '帳號 B 第二視窗登入'; '08' = '建房與進房'; '09' = '開始戰鬥'; '10' = '近戰技能'
    '11' = '魔法技能'; '12' = '變身 .polyme'; '13' = '完成戰鬥'; '14' = '結算畫面與職業圖示'; '15' = '戰績與收據 SQL 對照'
    '16' = '斷線場'; '17' = 'GM 指令與變身 7200'; '18' = 'GM 魔杖點選'; '19' = 'Ctrl 強制攻擊'; '20' = '正常退出'; '21' = '關閉 P24Lab 與釋放 lease'
}
$stepsB = [ordered]@{
    'G02' = '技能 26 不按 Ctrl 自施／對友方（現場封包）'; 'G19' = '完整流程（＝A 部全程）'; 'G20' = '結算畫面龍騎士／幻術師圖示'
    'G21' = '.polyme'; 'G22' = 'GM 魔杖點地傳送'; 'G23' = 'Ctrl 強制攻擊／光箭／負面技能'; 'G24' = '雙玩家斷線（拔線、關視窗）'
    'G25' = 'BUFF／魔法娃娃／藍水／血之渴望圖示與到期'; 'G26' = '重複登入'; 'G27' = 'map 12000'
}

try {
    $run = (Resolve-Path -LiteralPath $RunDirectory).Path
    Assert-M4RunDirectory $run
    $repo = Get-M4RepoRoot $PSScriptRoot
    if ($AcceptanceRoot -eq '') { $AcceptanceRoot = Join-Path $repo 'docs\acceptance' }
    Assert-M4NotServerProperties -Path $AcceptanceRoot -What 'AcceptanceRoot'
    $hashFile = Join-Path $run 'artifact-hashes.json'
    if (-not (Test-Path -LiteralPath $hashFile -PathType Leaf)) { throw "找不到 $hashFile（run 目錄不是 m4-prepare.ps1 建立的？）" }
    # 5.1 的 ConvertFrom-Json 會把整個陣列當成一個物件送出；先存進變數再展開。
    $parsedReceipts = Get-Content -LiteralPath $hashFile -Raw -Encoding UTF8 | ConvertFrom-Json
    $receipts = @($parsedReceipts | ForEach-Object { $_ })
    $jarReceipt = @($receipts | Where-Object { $_.File -eq 'LiTo381-Server.jar' })
    if ($jarReceipt.Count -ne 1) { throw 'artifact-hashes.json 沒有 LiTo381-Server.jar 的紀錄' }
    $jarSha = [string]$jarReceipt[0].SHA256
    $sha8 = $jarSha.Substring(0, 8).ToLowerInvariant()
    $versionDirectory = Join-Path $AcceptanceRoot "m4-$sha8"
    $resultFile = Join-Path $AcceptanceRoot "m4-$sha8.md"

    # ---- 截圖 ----
    $shots = @{}
    $unknown = @()
    if (Test-Path -LiteralPath $versionDirectory -PathType Container) {
        foreach ($file in @(Get-ChildItem -LiteralPath $versionDirectory -File -Filter '*.png' | Sort-Object Name)) {
            $kind = Get-M4ScreenshotKind $file.Name
            if ($kind.Part -eq '?') { $unknown += $file.Name; continue }
            $key = $kind.Part + ':' + $kind.Key
            if (-not $shots.ContainsKey($key)) { $shots[$key] = @() }
            $shots[$key] += $file.Name
        }
    }

    # ---- 證據 ----
    $evidence = Join-Path $run 'evidence'
    $evidenceFiles = @()
    if (Test-Path -LiteralPath $evidence -PathType Container) {
        $evidenceFiles = @(Get-ChildItem -LiteralPath $evidence -File | Where-Object { $_.Name -ne 'evidence-index.md' } | Sort-Object Name)
    }
    $names = @($evidenceFiles | ForEach-Object { $_.Name })
    $sqlSteps = @{}
    foreach ($name in $names) {
        if ($name -match '^sql-(before|after)-(.+)\.txt$') {
            if (-not $sqlSteps.ContainsKey($Matches[2])) { $sqlSteps[$Matches[2]] = @{ before = $false; after = $false; diff = '' } }
            $sqlSteps[$Matches[2]][$Matches[1]] = $true
        }
    }
    foreach ($step in @($sqlSteps.Keys)) {
        $diffFile = Join-Path $evidence "sql-diff-$step.txt"
        if (Test-Path -LiteralPath $diffFile -PathType Leaf) {
            $first = @(Get-Content -LiteralPath $diffFile -TotalCount 1 -Encoding UTF8)
            if ($first.Count -gt 0) { $sqlSteps[$step]['diff'] = [string]$first[0] }
        }
    }
    $relativeVersion = "m4-$sha8"

    $lines = @(
        "# M4 驗收結果：新 JAR $sha8",
        '',
        '> 本檔骨架由 `tools/acceptance/m4-evidence-index.ps1` 依實際存在的檔案產生。「未執行」＝找不到對應截圖；',
        '> 有截圖只代表檔案存在，**通過／失敗／阻塞要由執行者依畫面與證據填寫**，不能因為有檔案就填通過。',
        '',
        '## 基本資料',
        '',
        '| 項目 | 內容 |',
        '|---|---|',
        "| JAR | ``LiTo381-Server.jar``，SHA-256 ``$jarSha`` |",
        "| run 目錄（本機，不進 git） | ``$run`` |",
        "| 截圖目錄 | ``docs/acceptance/$relativeVersion/`` |",
        '| 執行日期／操作者／worktree／commit | 待填 |',
        '| p24lab lease（持有人、取得與釋放時間） | 待填 |',
        '',
        '## A 部：CHECKLIST 01–21',
        '',
        '| # | 步驟 | 截圖 | 結果 | 備註（現象／證據） |',
        '|---|---|---|---|---|')
    foreach ($id in $stepsA.Keys) {
        $key = 'A:' + $id
        if ($shots.ContainsKey($key)) {
            $links = @($shots[$key] | ForEach-Object { "[$_]($relativeVersion/$_)" }) -join '、'
            $lines += "| $id | $($stepsA[$id]) | $links | 待填（通過／失敗／阻塞） | |"
        }
        else { $lines += "| $id | $($stepsA[$id]) | — | 未執行 | |" }
    }
    $lines += @('', '## B 部：G19–G27 與 G02', '', '| 編號 | 項目 | 截圖 | 結果 | 備註（現象／證據） |', '|---|---|---|---|---|')
    foreach ($id in $stepsB.Keys) {
        $key = 'B:' + $id
        if ($shots.ContainsKey($key)) {
            $links = @($shots[$key] | ForEach-Object { "[$_]($relativeVersion/$_)" }) -join '、'
            $lines += "| $id | $($stepsB[$id]) | $links | 待填（通過／失敗／阻塞） | |"
        }
        else { $lines += "| $id | $($stepsB[$id]) | — | 未執行 | |" }
    }
    $lines += @('', '## C 部：M3 系列實機待驗證（V 序號依 M4-RUNBOOK C 部）', '')
    $partC = @($shots.Keys | Where-Object { $_ -like 'C:*' } | Sort-Object { [int]($_.Substring(3)) })
    if ($partC.Count -eq 0) { $lines += '沒有找到 `V<序>-<短名>.png`：C 部全部**未執行**。' }
    else {
        $lines += @('| 序號 | 截圖 | 結果 | 備註（現象／證據） |', '|---|---|---|---|')
        foreach ($key in $partC) {
            $links = @($shots[$key] | ForEach-Object { "[$_]($relativeVersion/$_)" }) -join '、'
            $lines += "| $($key.Substring(2)) | $links | 待填（通過／失敗／阻塞） | |"
        }
        $lines += @('', 'RUNBOOK C 部有、但上表沒有的序號＝未執行。')
    }
    $lines += @('', '## SQL 前後快照', '')
    if ($sqlSteps.Count -eq 0) { $lines += '沒有找到 `sql-before-*`／`sql-after-*`：**未執行**。' }
    else {
        $lines += @('| 步驟 | before | after | m4-sql-diff 結果 |', '|---|---|---|---|')
        foreach ($step in @($sqlSteps.Keys | Sort-Object)) {
            $entry = $sqlSteps[$step]
            $beforeText = '缺'
            if ($entry['before']) { $beforeText = "sql-before-$step.txt" }
            $afterText = '缺'
            if ($entry['after']) { $afterText = "sql-after-$step.txt" }
            $diffText = '未執行'
            if ($entry['diff'] -ne '') { $diffText = $entry['diff'] }
            $lines += "| $step | $beforeText | $afterText | $diffText |"
        }
    }
    $lines += @('', '## 其他證據（run 目錄 evidence\，原始檔留本機）', '', '| 類別 | 檔案 |', '|---|---|')
    $groups = [ordered]@{
        '成品雜湊' = '^artifact-hashes\.txt$'; '公開設定' = '^config-public\.properties$'; '資料庫準備／核對／資料前置' = '^db-(prepare|verify|seed).*\.txt$'
        '伺服器主控台' = '^server-console.*\.(log|txt)$'; '監聽核對' = '^listen-.*\.txt$'; 'WAL 核對' = '^wal-.*\.txt$'
    }
    foreach ($title in $groups.Keys) {
        $found = @($names | Where-Object { $_ -match $groups[$title] })
        if ($found.Count -eq 0) { $lines += "| $title | 未執行（沒有檔案） |" }
        else { $lines += "| $title | $($found -join '、') |" }
    }
    if ($unknown.Count -gt 0) {
        $lines += @('', '## 不符合命名契約的截圖', '')
        foreach ($name in $unknown) { $lines += "- ``$name``" }
    }
    $lines += @('', '## 失敗／異常記錄', '', '| 現象 | 步驟 | 證據 |', '|---|---|---|', '| 待填 | | |',
        '', '## 已驗證', '', '待填。', '', '## 推測', '', '待填。', '', '## 待驗證', '', '待填（未執行、阻塞的項目全部列在這裡）。', '')
    $text = ($lines -join "`n")

    if (Test-Path -LiteralPath $evidence -PathType Container) { Write-M4Utf8 -Path (Join-Path $evidence 'evidence-index.md') -Text $text }
    $wrote = '已存在，未覆寫（加 -Force 覆寫）'
    if ($Force -or -not (Test-Path -LiteralPath $resultFile)) {
        Write-M4Utf8 -Path $resultFile -Text $text
        $wrote = '已寫入骨架'
    }
    $missingA = @($stepsA.Keys | Where-Object { -not $shots.ContainsKey('A:' + $_) })
    $missingB = @($stepsB.Keys | Where-Object { -not $shots.ContainsKey('B:' + $_) })
    Write-Output "M4-EVIDENCE-INDEX: DONE jar=$sha8"
    Write-Output "ResultFile=$resultFile（$wrote）"
    Write-Output "ScreenshotDirectory=$versionDirectory"
    Write-Output "NotRun-A=$($missingA -join ',')"
    Write-Output "NotRun-B=$($missingB -join ',')"
    Write-Output "SqlSteps=$(@($sqlSteps.Keys | Sort-Object) -join ',')"
    exit 0
}
catch {
    Write-Output "M4-EVIDENCE-INDEX: FAIL $($_.Exception.Message)"
    exit 1
}

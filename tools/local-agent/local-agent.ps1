# local-agent.ps1 — 本機任務代理（事件驅動）
# 監看指定 GitHub repo 中帶 `local-task` 標籤的 open Issue，收到就在該 repo 的本機目錄用 Codex CLI 執行，
# 結果回貼 Issue 並關閉。由 Claude（雲端主控）開 Issue 派工；本程式只在你的電腦上跑。
#
# 需求：git（GitHub Desktop 內建即可）、gh CLI（winget install GitHub.cli，然後 gh auth login）、codex CLI（Codex 桌面版附帶）。
# 用法：powershell -ExecutionPolicy Bypass -File local-agent.ps1
#      可選參數 -Once（只掃一輪）、-IntervalSeconds 60、-Runner codex|claude（預設 codex）。
param(
    [switch]$Once,
    [int]$IntervalSeconds = 60,
    [ValidateSet('codex','claude')][string]$Runner = 'codex',
    [string]$Owner = 'chaoren0418-arch'
)
$ErrorActionPreference = 'Continue'
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)

# repo 名稱 → 本機目錄（新專案在這裡加一行）
$Repos = @{
    'tw-bin-engine'        = 'D:\lineage開發\TW1608262200研究'
    'lito-launcher-server' = 'D:\lineage開發\3-lito開發'
    'lito-modern'          = 'D:\lineage開發\3-lito開發\lito重建版'
}
$LogRoot = Join-Path $PSScriptRoot 'logs'
New-Item -ItemType Directory -Force -Path $LogRoot | Out-Null
function Log($m) { $l = "[{0}] {1}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $m; Write-Host $l; Add-Content -LiteralPath (Join-Path $LogRoot 'agent.log') -Value $l -Encoding UTF8 }

# 找 CLI
function Find-Cli($name, $candidates) {
    $c = Get-Command $name -EA SilentlyContinue
    if ($c) { return $c.Source }
    foreach ($p in $candidates) { $f = Get-ChildItem -Path $p -EA SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1; if ($f) { return $f.FullName } }
    return $null
}
$gh = Find-Cli 'gh' @("$env:ProgramFiles\GitHub CLI\gh.exe", "$env:LOCALAPPDATA\Programs\GitHub CLI\gh.exe")
if (-not $gh) { Write-Host '找不到 gh CLI。請先：winget install --id GitHub.cli -e，然後 gh auth login' -ForegroundColor Red; exit 1 }
$codex = Find-Cli 'codex' @("$env:LOCALAPPDATA\OpenAI\Codex\bin\*\codex.exe", "$env:USERPROFILE\.codex\.sandbox-bin\codex.exe")
$claude = Find-Cli 'claude' @("$env:APPDATA\npm\claude.cmd")
if ($Runner -eq 'codex' -and -not $codex) { Write-Host '找不到 codex CLI（Codex 桌面版應附帶）。' -ForegroundColor Red; exit 1 }
if ($Runner -eq 'claude' -and -not $claude) { Write-Host '找不到 claude CLI。請先：npm install -g @anthropic-ai/claude-code' -ForegroundColor Red; exit 1 }
$ghd = Get-ChildItem -Path "$env:LOCALAPPDATA\GitHubDesktop\app-*\resources\app\git\cmd\git.exe" -EA SilentlyContinue | Sort-Object FullName -Descending | Select-Object -First 1
if ($ghd -and -not (Get-Command git -EA SilentlyContinue)) { $env:Path = (Split-Path $ghd.FullName) + ';' + $env:Path }
Log "local-agent 啟動 runner=$Runner gh=$gh codex=$codex claude=$claude"

function Gh { param([Parameter(ValueFromRemainingArguments)]$a) & $gh @a 2>&1 }

function Run-Task($repo, $issue) {
    $dir = $Repos[$repo]; $n = $issue.number
    $log = Join-Path $LogRoot ("{0}-{1}.log" -f $repo, $n)
    Log "接手 $repo #$n：$($issue.title)"
    Gh issue edit $n -R "$Owner/$repo" --add-label running --remove-label local-task | Out-Null
    Gh issue comment $n -R "$Owner/$repo" --body ("本機代理已接手（runner=$Runner，{0}）。" -f (Get-Date -Format 'yyyy-MM-dd HH:mm')) | Out-Null

    $prompt = @"
你在本機專案目錄 $dir（GitHub repo $Owner/$repo）執行一項由雲端主控（Claude）指派的本機任務。先讀 AGENTS.md 與 TASKS.md 的規則。
任務（來自 Issue #$n「$($issue.title)」）：
$($issue.body)

完成要求：
1. 任務產出（測試結果、擷取的資料、報告）依任務指示寫入專案內指定路徑；沒指定時寫 docs/local-task/issue-$n/。
2. 若有檔案變更：git checkout -b local-task/$n（已存在則切換），commit，push 到 origin；commit 訊息以「local-task #$n:」開頭。不可碰 config/server.properties 等機密，不可改 releases/ 與封存。
3. 最後印出一段以 `=== RESULT ===` 開頭的摘要（做了什麼、產出路徑、分支名、未完成事項），不超過 40 行。
"@
    $promptFile = Join-Path $LogRoot ("{0}-{1}.prompt.md" -f $repo, $n)
    Set-Content -LiteralPath $promptFile -Value $prompt -Encoding UTF8
    Push-Location -LiteralPath $dir
    try {
        if ($Runner -eq 'codex') {
            $out = & $codex exec --full-auto --cd $dir (Get-Content -LiteralPath $promptFile -Raw) 2>&1
        } else {
            $out = & $claude -p (Get-Content -LiteralPath $promptFile -Raw) --permission-mode acceptEdits 2>&1
        }
        $code = $LASTEXITCODE
    } finally { Pop-Location }
    $out | Out-File -LiteralPath $log -Encoding UTF8
    $text = ($out | Out-String)
    $idx = $text.LastIndexOf('=== RESULT ===')
    $summary = if ($idx -ge 0) { $text.Substring($idx) } else { ($out | Select-Object -Last 40) -join "`n" }
    if ($summary.Length -gt 6000) { $summary = $summary.Substring(0, 6000) + "`n…(截斷，完整 log 在本機 $log)" }
    $status = if ($code -eq 0) { 'done' } else { 'failed' }
    $body = "## 本機代理結果（$status，exit $code，runner=$Runner）`n`n```````n$summary`n```````n`n完整 log：本機 ``$log``"
    $bodyFile = Join-Path $LogRoot ("{0}-{1}.comment.md" -f $repo, $n)
    Set-Content -LiteralPath $bodyFile -Value $body -Encoding UTF8
    Gh issue comment $n -R "$Owner/$repo" --body-file $bodyFile | Out-Null
    Gh issue edit $n -R "$Owner/$repo" --remove-label running --add-label $status | Out-Null
    if ($code -eq 0) { Gh issue close $n -R "$Owner/$repo" | Out-Null }
    Log "完成 $repo #$n → $status"
}

do {
    foreach ($repo in $Repos.Keys) {
        if (-not (Test-Path -LiteralPath $Repos[$repo])) { continue }
        $json = Gh issue list -R "$Owner/$repo" -l local-task -s open --json number,title,body --limit 5
        try { $issues = $json | ConvertFrom-Json } catch { Log "讀取 $repo issue 失敗：$json"; continue }
        foreach ($i in @($issues)) { Run-Task $repo $i }
    }
    if (-not $Once) { Start-Sleep -Seconds $IntervalSeconds }
} while (-not $Once)

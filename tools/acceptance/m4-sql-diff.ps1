# M4 驗收：比對兩份 m4-sql-snapshot.ps1 快照（T128／Issue #379）。相容 Windows PowerShell 5.1；UTF-8 BOM。
# 純檔案比對，不連資料庫。判定「勝者 wins+1、敗者 losses+1、積分增量、收據 applied=1」：
#   每筆新收據：勝 +WinReward（預設 250）、敗 +LossReward（預設 50）、斷線敗 −LossReward 且 disconnects+1；積分最低 0；
#   version 每筆 +1；last_result_id＝該角色最後一筆新收據；月計同月累加、換月從 0 起算（SettlementSql.java:29-38、ServerConfig.java:578-579）。
#   沒有新收據的角色必須完全沒變。
# 退出碼：0＝PASS；1＝FAIL；3＝NO-CHANGE（兩份快照之間沒有新收據；加 -ExpectNoChange 時視為 PASS）。
# 用法：
#   powershell -NoProfile -ExecutionPolicy Bypass -File tools\acceptance\m4-sql-diff.ps1 -RunDirectory <run> -Step 15
#   powershell -NoProfile -ExecutionPolicy Bypass -File tools\acceptance\m4-sql-diff.ps1 -Before <檔> -After <檔> -ExpectWinner <角色名> -ExpectLoser <角色名>
param(
    [string]$RunDirectory = '',
    [string]$Step = '',
    [string]$Before = '',
    [string]$After = '',
    [long]$WinReward = 250,
    [long]$LossReward = 50,
    [string]$ExpectWinner = '',
    [string]$ExpectLoser = '',
    [switch]$ExpectNoChange
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'm4-common.ps1')

try {
    $report = ''
    if ($RunDirectory -ne '') {
        $run = (Resolve-Path -LiteralPath $RunDirectory).Path
        Assert-M4RunDirectory $run
        if ($Step -eq '') { throw '使用 -RunDirectory 時必須給 -Step（對應 sql-before-<Step>.txt／sql-after-<Step>.txt）' }
        Assert-M4Label "before-$Step"
        $Before = Join-Path $run "evidence\sql-before-$Step.txt"
        $After = Join-Path $run "evidence\sql-after-$Step.txt"
        $report = Join-Path $run "evidence\sql-diff-$Step.txt"
    }
    if ($Before -eq '' -or $After -eq '') { throw '需要 -RunDirectory＋-Step，或 -Before＋-After' }
    foreach ($path in @($Before, $After)) {
        Assert-M4NotServerProperties -Path $path -What '快照檔'
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "找不到快照：$path" }
    }
    $beforeLines = @([System.IO.File]::ReadAllLines($Before, [System.Text.Encoding]::UTF8))
    $afterLines = @([System.IO.File]::ReadAllLines($After, [System.Text.Encoding]::UTF8))
    $result = Compare-M4Snapshot -Before $beforeLines -After $afterLines -WinReward $WinReward -LossReward $LossReward `
        -ExpectWinner $ExpectWinner -ExpectLoser $ExpectLoser
    $verdict = $result.Verdict
    if ($verdict -eq 'NO-CHANGE' -and $ExpectNoChange) { $verdict = 'PASS' }
    elseif ($verdict -eq 'PASS' -and $ExpectNoChange) { $verdict = 'FAIL'; $result.Failures += '預期沒有變化，但出現了新收據' }
    $text = @("M4-SQL-DIFF: $verdict", "before=$Before", "after=$After", "rewards=win $WinReward / loss $LossReward")
    $text += @($result.Lines)
    foreach ($failure in @($result.Failures)) { $text += "FAIL: $failure" }
    if ($report -ne '') { Write-M4Utf8 -Path $report -Text (($text -join "`n") + "`n") }
    $text | ForEach-Object { Write-Output $_ }
    if ($verdict -eq 'PASS') { exit 0 }
    if ($verdict -eq 'NO-CHANGE') { exit 3 }
    exit 1
}
catch {
    Write-Output "M4-SQL-DIFF: FAIL $($_.Exception.Message)"
    exit 1
}

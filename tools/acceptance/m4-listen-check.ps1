# M4 驗收：核對實驗 Java 程序的監聽位址（T128／Issue #379；CHECKLIST 02）。相容 Windows PowerShell 5.1；UTF-8 BOM。
# 唯讀：只查詢 TCP 監聽表與程序名稱，不啟動、不停止任何程序。
# 預期（CHECKLIST 02／RUNBOOK A02）：每個來源四個——127.0.0.1:<Port>、127.0.0.2:<Port>（Main.java:71-80）、
#   127.0.0.1:(<Port>+source_id)、127.0.0.2:(<Port>+source_id)（SourceLogin.java:42-50）。這是讀原始碼得到的，尚未實機確認。
# 多一個、少一個、非 loopback 位址、埠 2000／3306 一律 FAIL。
# -LegacyTwoOnly：改成 T128 任務原文的兩個（127.0.0.1:<Port> 與 127.0.0.2:(<Port>+source_id)）；只在實機證明伺服器只開兩個時使用。
# 用法：
#   powershell -NoProfile -ExecutionPolicy Bypass -File tools\acceptance\m4-listen-check.ps1 -ProcessId <Java PID> -SourceId 1 -RunDirectory <run>
param(
    [Parameter(Mandatory = $true)][int]$ProcessId,
    [int[]]$SourceId = @(1),
    [string]$Bind = '127.0.0.1',
    [string]$GameBind = '127.0.0.2',
    [int]$Port = 42083,
    [string]$RunDirectory = '',
    [switch]$LegacyTwoOnly
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'm4-common.ps1')

try {
    $expected = Get-M4ExpectedListeners -Bind $Bind -GameBind $GameBind -Port $Port -SourceId $SourceId -LegacyTwoOnly:$LegacyTwoOnly
    $run = ''
    if ($RunDirectory -ne '') {
        $run = (Resolve-Path -LiteralPath $RunDirectory).Path
        Assert-M4RunDirectory $run
    }
    $process = Get-Process -Id $ProcessId -ErrorAction Stop
    if ($process.ProcessName -notmatch '^(?i)javaw?$') { throw "PID $ProcessId 是 $($process.ProcessName)，不是 java" }

    $actual = @()
    $command = Get-Command Get-NetTCPConnection -ErrorAction SilentlyContinue
    if ($command) {
        $connections = @(Get-NetTCPConnection -State Listen -OwningProcess $ProcessId -ErrorAction SilentlyContinue)
        foreach ($connection in $connections) { $actual += "$($connection.LocalAddress):$($connection.LocalPort)" }
    }
    else {
        foreach ($line in @(netstat -ano -p TCP)) {
            if ($line -match '^\s*TCP\s+(\S+):(\d+)\s+\S+\s+LISTENING\s+(\d+)\s*$' -and [int]$Matches[3] -eq $ProcessId) {
                $actual += "$($Matches[1]):$($Matches[2])"
            }
        }
    }
    $actual = @($actual | Sort-Object -Unique)
    $result = Compare-M4Listeners -Actual $actual -Expected $expected
    $verdict = 'FAIL'
    if ($result.Ok) { $verdict = 'PASS' }
    $text = @(
        "M4-LISTEN-CHECK: $verdict",
        "utc=$([DateTime]::UtcNow.ToString('o')) pid=$ProcessId process=$($process.ProcessName) legacy_two_only=$([bool]$LegacyTwoOnly)",
        "expected=$($expected -join ', ')",
        "actual=$($actual -join ', ')",
        "missing=$(@($result.Missing) -join ', ')",
        "unexpected=$(@($result.Unexpected) -join ', ')",
        "violations=$(@($result.Violations) -join ', ')")
    if ($run -ne '') {
        $file = Join-Path $run ("evidence\listen-" + [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ') + '.txt')
        Write-M4Utf8 -Path $file -Text (($text -join "`n") + "`n")
        $text += "Output=$file"
    }
    $text | ForEach-Object { Write-Output $_ }
    if ($result.Ok) { exit 0 }
    exit 1
}
catch {
    Write-Output "M4-LISTEN-CHECK: FAIL $($_.Exception.Message)"
    exit 1
}

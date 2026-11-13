# M4 驗收：核對實驗 Java 程序的監聽位址（T128／Issue #379；CHECKLIST 02）。相容 Windows PowerShell 5.1；UTF-8 BOM。
# 唯讀：只查詢 TCP 監聽表與程序名稱，不啟動、不停止任何程序。
# 必要（CHECKLIST 02）：127.0.0.1:<Port>、127.0.0.2:(<Port>+source_id)。
# 允許（依原始碼靜態閱讀，未實機確認）：127.0.0.2:<Port>（Main.java:74-80）、127.0.0.1:(<Port>+source_id)（SourceLogin.java:47-50）。
# 其他任何監聽、非 loopback 位址、埠 2000／3306 一律 FAIL。加 -Strict 時「允許」清單也算 FAIL（＝只接受必要的兩類）。
# 用法：
#   powershell -NoProfile -ExecutionPolicy Bypass -File tools\acceptance\m4-listen-check.ps1 -ProcessId <Java PID> -SourceId 1 -RunDirectory <run>
param(
    [Parameter(Mandatory = $true)][int]$ProcessId,
    [int[]]$SourceId = @(1),
    [string]$Bind = '127.0.0.1',
    [string]$GameBind = '127.0.0.2',
    [int]$Port = 42083,
    [string]$RunDirectory = '',
    [switch]$Strict
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'm4-common.ps1')

try {
    $expected = Get-M4ExpectedListeners -Bind $Bind -GameBind $GameBind -Port $Port -SourceId $SourceId
    $allowed = @()
    if (-not $Strict) { $allowed = Get-M4AllowedListeners -Bind $Bind -GameBind $GameBind -Port $Port -SourceId $SourceId }
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
    $result = Compare-M4Listeners -Actual $actual -Expected $expected -Allowed $allowed
    $verdict = 'FAIL'
    if ($result.Ok) { $verdict = 'PASS' }
    $text = @(
        "M4-LISTEN-CHECK: $verdict",
        "utc=$([DateTime]::UtcNow.ToString('o')) pid=$ProcessId process=$($process.ProcessName) strict=$([bool]$Strict)",
        "required=$($expected -join ', ')",
        "allowed=$(@($allowed) -join ', ')",
        "actual=$($actual -join ', ')",
        "allowed_seen=$(@($result.AllowedSeen) -join ', ')",
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

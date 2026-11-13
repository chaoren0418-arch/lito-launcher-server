# M4 驗收：檢查實驗 run 目錄的結算 WAL（T128／Issue #379；CHECKLIST 16、20）。相容 Windows PowerShell 5.1；UTF-8 BOM。
# 唯讀：列出 <run>\runtime\battle-settlements\，回報 .pending（待補收據）與 .tmp（寫到一半的暫存檔）。不刪、不搬、不改任何檔案。
# owner.lock 留檔是正常的；本腳本另外嘗試以獨占方式開啟它，開得起來代表鎖已釋放（伺服器已退出）。
# 退出碼：0＝沒有 .pending／.tmp；1＝有殘留或檢查失敗。伺服器還在執行時加 -AllowLocked（owner.lock 被占用不算失敗）。
# 用法：
#   powershell -NoProfile -ExecutionPolicy Bypass -File tools\acceptance\m4-wal-check.ps1 -RunDirectory <run>
param(
    [Parameter(Mandatory = $true)][string]$RunDirectory,
    [switch]$AllowLocked
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'm4-common.ps1')

try {
    $run = (Resolve-Path -LiteralPath $RunDirectory).Path
    Assert-M4RunDirectory $run
    $directory = Join-Path $run 'runtime\battle-settlements'
    $text = @()
    $clean = $true
    if (-not (Test-Path -LiteralPath $directory -PathType Container)) {
        $text += "directory=$directory（不存在：伺服器尚未啟動過，或沒有建立 WAL）"
    }
    else {
        $files = @(Get-ChildItem -LiteralPath $directory -Recurse -File -Force | Sort-Object FullName)
        $relative = @()
        foreach ($file in $files) {
            $name = $file.FullName.Substring($directory.Length).TrimStart('\')
            $relative += $name
            $text += "file=$name bytes=$($file.Length) modified_utc=$($file.LastWriteTimeUtc.ToString('o'))"
        }
        $status = Get-M4WalStatus -Files $relative
        $clean = $status.Clean
        $text += "files=$($files.Count) pending=$(@($status.Pending).Count) tmp=$(@($status.Temporary).Count)"
        foreach ($name in @($status.Pending)) { $text += "PENDING: $name" }
        foreach ($name in @($status.Temporary)) { $text += "TMP: $name" }
        $lock = Join-Path $directory 'owner.lock'
        if (Test-Path -LiteralPath $lock -PathType Leaf) {
            $released = $false
            try {
                $stream = [System.IO.File]::Open($lock, [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::None)
                $stream.Dispose()
                $released = $true
            }
            catch { $released = $false }
            $text += "owner.lock released=$released"
            if (-not $released -and -not $AllowLocked) { $clean = $false; $text += 'LOCKED: owner.lock 仍被占用（伺服器還在執行？執行中檢查請加 -AllowLocked）' }
        }
        else { $text += 'owner.lock=（不存在）' }
    }
    $verdict = 'FAIL'
    if ($clean) { $verdict = 'PASS' }
    $text = @("M4-WAL-CHECK: $verdict", "utc=$([DateTime]::UtcNow.ToString('o')) run=$run") + $text
    $evidence = Join-Path $run 'evidence'
    if (Test-Path -LiteralPath $evidence -PathType Container) {
        $output = Join-Path $evidence ('wal-' + [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ') + '.txt')
        Write-M4Utf8 -Path $output -Text (($text -join "`n") + "`n")
        $text += "Output=$output"
    }
    $text | ForEach-Object { Write-Output $_ }
    if ($clean) { exit 0 }
    exit 1
}
catch {
    Write-Output "M4-WAL-CHECK: FAIL $($_.Exception.Message)"
    exit 1
}

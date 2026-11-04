# Issue #4 / T003 — 登入器建置腳本去硬編路徑

## 做了什麼

2026-11-04 本機驗證：在既有 worktree `D:\lineage開發\3-lito開發\scratch\wt\task-4`、分支 `task/4`，以 `git fetch origin task/4` 與 `git merge --ff-only origin/task/4` 同步 Claude 修正 `69748b0f4b2bf4c4fdc50b51db8465e4e14a5c45`。本輪只更新本報告，三支建置腳本及 `docs/REPORT.md` 均未修改。

既有實作的範圍：

| 檔案 | 原先路徑／行為 | 現在行為 |
|---|---|---|
| `lito登入器/LiTo381-Launcher/Build.ps1` | 固定 `C:\Users\CHAOREN\AppData\Local\Programs\Python\Python312\python.exe` | 優先 `py -3.12`，失敗則找 PATH 的 `python`／`python3`；版本探測局部使用 `Continue`，完成後還原 `Stop` |
| `lito登入器/LiTo381-Launcher/tools/build_current.py` | 固定 `C:\Program Files\dotnet\dotnet.exe` | `shutil.which('dotnet')`；publish 的組態、RID、self-contained、輸出目錄不變 |
| `lito登入器/LiTo381-Launcher/tools/verify_current.py` | 發布輸入路徑依賴 | 相對路徑從 repo 根解析；缺少 `server_jar` 欄位時保留既有預設路徑 |

沒有變更發布產物；本輪未執行完整 publish，不能據此宣稱重建產物逐位元相同。`deployment/release-inputs.json` 維持不追蹤，未讀寫其內容。

## 怎麼驗證（已驗證）

### 環境與隔離方式

- 實際程序：`C:\WINDOWS\System32\WindowsPowerShell\v1.0\powershell.exe`。
- `$PSVersionTable.PSVersion`：`5.1.19041.6456`，不是 pwsh。
- `py`：`C:\WINDOWS\py.exe`；Python：`3.12.10`。
- Python 實際位置：`C:\Users\CHAOREN\AppData\Local\Programs\Python\Python312\python.exe`。
- 原始 PATH 沒有 `python`，因此各測試子程序局部設定 PATH；未改使用者或系統環境。
- 從正式 `Build.ps1` 的 PowerShell AST 擷取 `Invoke-Python`，只載入函式，不執行檔尾 build／verify／cleanup 呼叫。
- 暫存皆在 worktree 的 `scratch/issue-4/`，不進 git。成功測試腳本只有 `import sys; print(sys.executable)`。
- 用 `Set-PSBreakpoint` 的觀測動作輸出 `$exe`、`$prefix`、`$ErrorActionPreference`，確認呼叫方式；不只依賴相同的 `sys.executable` 判斷分支。
- `MissingVersion` 只將擷取副本的探測命令改成 `py -3.99`；正式 `Build.ps1` 從未改成 3.99。

以下命令均從 worktree 根目錄執行；測試腳本完整內容見文末，原始輸出暫存於 `scratch/issue-4/results.log`。

### 1. py 有 3.12 → 選 py -3.12

子程序 PATH 為 py 所在目錄及 Python 所在目錄，兩個命令都可用。

```powershell
powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File .\scratch\issue-4\test-selection.ps1 -Scenario Py312 -Failure None
```

實際關鍵輸出：

```text
PS_VERSION=5.1.19041.6456
HOST_EXE=C:\WINDOWS\System32\WindowsPowerShell\v1.0\powershell.exe
SCENARIO=Py312 FAILURE=None
PY_PRESENT=True
PATH_PYTHON=C:\Users\CHAOREN\AppData\Local\Programs\Python\Python312\python.exe
SELECTED=py PREFIX=-3.12 PREFERENCE=Stop
C:\Users\CHAOREN\AppData\Local\Programs\Python\Python312\python.exe
AFTER_INVOKE PREFERENCE=Stop
CHILD_EXIT=0
```

### 2. py 存在但指定版本不存在 → 退回 PATH python

另直接確認不存在版本的實際失敗：

```powershell
$ErrorActionPreference='Continue'
py -3.99 -c 'import sys'
Write-Output "MISSING_VERSION_EXIT=$LASTEXITCODE"
```

```text
No suitable Python runtime found
Pass --list (-0) to see all detected environments on your machine
or set environment variable PYLAUNCHER_ALLOW_INSTALL to use winget
or open the Microsoft Store to the requested version.
MISSING_VERSION_EXIT=103
```

測試函式副本局部修改探測版本，其餘邏輯不變：

```powershell
powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File .\scratch\issue-4\test-selection.ps1 -Scenario MissingVersion -Failure None
```

```text
SCENARIO=MissingVersion FAILURE=None
PY_PRESENT=True
PATH_PYTHON=C:\Users\CHAOREN\AppData\Local\Programs\Python\Python312\python.exe
SELECTED=C:\Users\CHAOREN\AppData\Local\Programs\Python\Python312\python.exe PREFIX= PREFERENCE=Stop
C:\Users\CHAOREN\AppData\Local\Programs\Python\Python312\python.exe
AFTER_INVOKE PREFERENCE=Stop
CHILD_EXIT=0
```

### 3. PATH 沒有 py、只有 python → 選 PATH python

子程序 PATH 僅含 Python 所在目錄，`Get-Command py` 確認找不到。

```powershell
powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File .\scratch\issue-4\test-selection.ps1 -Scenario NoPy -Failure None
```

```text
SCENARIO=NoPy FAILURE=None
PY_PRESENT=False
PATH_PYTHON=C:\Users\CHAOREN\AppData\Local\Programs\Python\Python312\python.exe
SELECTED=C:\Users\CHAOREN\AppData\Local\Programs\Python\Python312\python.exe PREFIX= PREFERENCE=Stop
C:\Users\CHAOREN\AppData\Local\Programs\Python\Python312\python.exe
AFTER_INVOKE PREFERENCE=Stop
CHILD_EXIT=0
```

### 失敗仍會 throw 並中止

三種情境各執行一支 `import sys; sys.exit(23)`，確認版本探測的例外處理不會吞掉後續腳本失敗：

```powershell
powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File .\scratch\issue-4\test-selection.ps1 -Scenario Py312 -Failure Exit23
powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File .\scratch\issue-4\test-selection.ps1 -Scenario MissingVersion -Failure Exit23
powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File .\scratch\issue-4\test-selection.ps1 -Scenario NoPy -Failure Exit23
```

三案均輸出：

```text
CAUGHT=Python failed: D:\lineage開發\3-lito開發\scratch\wt\task-4\scratch\issue-4\exit23.py
PYTHON_EXIT=23 PREFERENCE=Stop
CHILD_EXIT=1
```

再把真實 `build_current.py`／`verify_current.py` 原封不動複製到 `scratch/issue-4/isolated/tools/`，每次先核對副本 SHA-256 與來源相同。隔離目錄刻意沒有 helper，兩支腳本在讀取 helper 時真實失敗，尚未進入改寫來源、publish 或發布輸入驗證。

```powershell
powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File .\scratch\issue-4\test-selection.ps1 -Scenario MissingVersion -Failure Build
powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File .\scratch\issue-4\test-selection.ps1 -Scenario MissingVersion -Failure Verify
```

| 案例 | Python 真實錯誤 | PowerShell 結果 |
|---|---|---|
| Build | `FileNotFoundError`：隔離目錄缺 `src/development_startup.py` | `CAUGHT=Python failed: ...\isolated\tools\build_current.py`；`PYTHON_EXIT=1 PREFERENCE=Stop`；子程序退出 1 |
| Verify | `FileNotFoundError`：隔離目錄缺 `release/current/development_startup.py` | `CAUGHT=Python failed: ...\isolated\tools\verify_current.py`；`PYTHON_EXIT=1 PREFERENCE=Stop`；子程序退出 1 |

兩案都有 `REAL_SCRIPT_COPY_SHA256_MATCH=True`。五個失敗案均未輸出 `AFTER_INVOKE`；外層測試 catch 只記錄函式拋出的例外並 `exit 1`。共 8 案通過（3 個選擇、3 個非零退出、2 個真實腳本缺輸入）；彙整輸出為 `ALL_8_CASES_PASS`。這些是錯誤傳遞測試，不代表完整 build／verify 成功。

### CI 與差異核對

- 修正提交 `69748b0f4b2bf4c4fdc50b51db8465e4e14a5c45` 的 [Launcher job 126522700157](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41814783606/job/126522700157) 已 `completed / success`，`Build (Release)` 通過。
- 同一 run 的 Server package 已成功；查詢時 Server tests 仍執行中，不宣稱整個 workflow 全綠。
- Launcher CI 執行 `dotnet build ... -c Release`，不涵蓋 `Build.ps1` 的直譯器選擇或完整 publish；本輪本機測試補上前者。
- `git diff --exit-code 69748b0 --` 三支建置腳本與 `docs/REPORT.md` 回傳 0；實驗修改未留在正式檔案。
- 本報告提交後的最新 CI 結果記錄於 PR #8 描述，避免為記錄自身提交 SHA 不斷追加報告提交。

## 未驗證什麼

- 未執行完整 publish、完整發布輸入檢查或 EXE／DLL 雜湊比較。
- 未在其他實體機安裝不同 Python 版本；缺版本與缺 py 是以探測副本／子程序 PATH 隔離測試。
- 未測 PATH 的 `python3` 分支及完全沒有 Python 的情境（不在本輪三個驗收情境）。
- 未啟動登入器、伺服器或遊戲，未連正式 DB，未讀寫機密設定、封存、硬連結或客戶端資源。

## 產出路徑

- 提交：`docs/reports/issue-4.md`。
- 本機實驗（gitignored）：`scratch/issue-4/test-selection.ps1`、`run.ps1`、`results.log`、Python 小腳本、擷取函式與隔離腳本副本。

## 給下一棒的提示

1. Claude 雲端複審既有 PR #8：本輪驗證通過，無需再改 `Invoke-Python`；查核最新 Launcher CI 與本報告即可。
2. 調度器於合併後自動加 `docs/REPORT.md` 索引；本輪沒有修改該檔，也未改 `TASKS.md` 狀態。
3. 若日後另外派完整 publish 測試，由本機執行器在核准範圍進行；真機登入與畫面驗收仍由使用者負責。
4. 保持 `deployment/release-inputs.json` 不進 git，不調整每機 `java_runtime` pin。

## 重現用暫存腳本

以下內容存為 worktree 內 `scratch/issue-4/test-selection.ps1`（UTF-8 BOM，供 Windows PowerShell 5.1 正確讀取中文路徑），不需修改正式檔案。

```powershell
param(
    [ValidateSet('Py312','MissingVersion','NoPy')][string]$Scenario,
    [ValidateSet('None','Exit23','Build','Verify')][string]$Failure='None'
)
$ErrorActionPreference='Stop'
if ($PSVersionTable.PSVersion.Major -ne 5 -or $PSVersionTable.PSVersion.Minor -ne 1) { throw 'Windows PowerShell 5.1 required' }
$root=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$source=Join-Path $root 'lito登入器/LiTo381-Launcher/Build.ps1'
$tokens=$null; $errors=$null
$ast=[System.Management.Automation.Language.Parser]::ParseFile($source,[ref]$tokens,[ref]$errors)
if ($errors.Count) { throw 'Parse failed' }
$fn=$ast.Find({ param($node) $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq 'Invoke-Python' },$true).Extent.Text
if ($Scenario -eq 'MissingVersion') {
    $probe="& py -3.12 -c 'import sys'"
    if (-not $fn.Contains($probe)) { throw 'Probe not found' }
    $fn=$fn.Replace($probe,"& py -3.99 -c 'import sys'")
}
$functionFile=Join-Path $PSScriptRoot ('invoke-'+$Scenario+'.ps1')
Set-Content -LiteralPath $functionFile -Value $fn -Encoding UTF8
$pythonExe=(& py -3.12 -c 'import sys; print(sys.executable)').Trim()
if ($LASTEXITCODE -ne 0) { throw 'Setup Python not available' }
$pythonDir=Split-Path $pythonExe -Parent
$pyDir=Split-Path (Get-Command py).Source -Parent
$env:PATH=if ($Scenario -eq 'NoPy') { $pythonDir } else { $pyDir+';'+$pythonDir }
$env:PYLAUNCHER_ALLOW_INSTALL=$null
$env:PYTHON_DONT_WRITE_BYTECODE='1'
Write-Output ('PS_VERSION='+$PSVersionTable.PSVersion)
Write-Output ('HOST_EXE='+(Get-Process -Id $PID).Path)
Write-Output ('SCENARIO='+$Scenario+' FAILURE='+$Failure)
Write-Output ('PY_PRESENT='+[bool](Get-Command py -ErrorAction SilentlyContinue))
Write-Output ('PATH_PYTHON='+(Get-Command python).Source)
. $functionFile
$line=(Select-String -LiteralPath $functionFile -SimpleMatch '& $exe @prefix -B -X utf8 $Script').LineNumber
$bp=Set-PSBreakpoint -Script $functionFile -Line $line -Action {
    Write-Host ('SELECTED='+$exe+' PREFIX='+($prefix -join ' ')+' PREFERENCE='+$ErrorActionPreference)
}
$target=Join-Path $PSScriptRoot 'print-executable.py'
if ($Failure -eq 'Exit23') { $target=Join-Path $PSScriptRoot 'exit23.py' }
if ($Failure -in @('Build','Verify')) {
    $name=if ($Failure -eq 'Build') { 'build_current.py' } else { 'verify_current.py' }
    $target=Join-Path $PSScriptRoot ('isolated/tools/'+$name)
    $original=Join-Path (Split-Path $source -Parent) ('tools/'+$name)
    if ((Get-FileHash $target).Hash -ne (Get-FileHash $original).Hash) { throw 'Copied script mismatch' }
    Write-Output 'REAL_SCRIPT_COPY_SHA256_MATCH=True'
}
try {
    Invoke-Python $target
    Write-Output ('AFTER_INVOKE PREFERENCE='+$ErrorActionPreference)
} catch {
    Write-Output ('CAUGHT='+$_.Exception.Message)
    Write-Output ('PYTHON_EXIT='+$LASTEXITCODE+' PREFERENCE='+$ErrorActionPreference)
    exit 1
} finally { Remove-PSBreakpoint $bp }
```

執行前準備（同樣只寫 worktree 的 scratch）：

```powershell
New-Item -ItemType Directory -Path .\scratch\issue-4\isolated\tools -Force | Out-Null
'import sys; print(sys.executable)' | Set-Content .\scratch\issue-4\print-executable.py -Encoding ASCII
'import sys; sys.exit(23)' | Set-Content .\scratch\issue-4\exit23.py -Encoding ASCII
Copy-Item -LiteralPath 'lito登入器/LiTo381-Launcher/tools/build_current.py','lito登入器/LiTo381-Launcher/tools/verify_current.py' -Destination .\scratch\issue-4\isolated\tools
```

各命令後以 `$LASTEXITCODE` 檢查子程序退出碼（上述 `CHILD_EXIT` 為外層 runner 記錄）；負向案例預期退出 1 且沒有 `AFTER_INVOKE`。

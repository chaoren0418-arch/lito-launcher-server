# Issue #4 / T003 — 登入器建置腳本去硬編路徑

## 做了什麼

只改 T003 列出的建置入口，讓腳本不再綁死單一開發機路徑。產物參數（`publish` 組態、RID、self-contained、輸出目錄）未改。

### 被替換的硬編路徑

| 檔案 | 原硬編路徑 | 改為 |
|---|---|---|
| `lito登入器/LiTo381-Launcher/Build.ps1` | `C:\Users\CHAOREN\AppData\Local\Programs\Python\Python312\python.exe`（兩處：build / verify） | 先試 `py -3.12`，沒有再找 PATH 的 `python`／`python3` |
| `lito登入器/LiTo381-Launcher/tools/build_current.py` | `C:\Program Files\dotnet\dotnet.exe` | `shutil.which('dotnet')`（PATH） |
| `lito登入器/LiTo381-Launcher/tools/verify_current.py` | 無 Python／dotnet 硬編；`release-inputs.json` 已用相對路徑 | 明確以 repo 根解析；若 JSON 有 `server_jar` 則依 repo 根拼接 |
| `lito伺服器/LiTo381-Server/deployment/release-inputs.json` | 本 clone 無此檔（整個 `deployment/` 被 gitignore）。依 `docs/maintenance/release-0.6.71.json` 與 0.6.66 pins 重建 | 新增相對 repo 根的 `server_jar`、`catalog`；`server_sha256`／`catalog_sha256` 沿用 0.6.71 收據 |

未改、且說明原因：

- `java_runtime.path` 仍是 `C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin\java.exe`。這是系統 JDK 身分 pin（`Start-Shared-Formal.py`／`Start-Shared-World.py` 比 image SHA），不在 repo 內，改成相對路徑會讓正式啟動身分核對失敗。不在本次範圍改啟動腳本。
- `src/FormalStandardSources.cs`、`src/development_startup.py` 仍有開發機絕對路徑；T003 範圍未列，未動。改了會改 pin 重算，可能動到產物。

## 怎麼驗證

**已驗證**

- 靜態核對：`Build.ps1`、`build_current.py` 不再出現上述兩個絕對路徑。
- `build_current.py` 的 `dotnet publish` 參數與原先相同：`-c Release -r win-x86 --self-contained true -o release/current`。
- `python3 -m py_compile` 通過 `build_current.py`、`verify_current.py`。
- `pwsh -NoProfile -Command` 解析 `Build.ps1` 無語法錯誤（此環境無 `py`／Windows Python，未真正執行建置）。
- CI launcher job 只跑 `dotnet build`，不走 `Build.ps1`；此次未改 `.csproj`／C# 來源，預期仍綠。

**推測**

- 本機若已裝 Python 3.12（`py -3.12`）與 PATH 上的 `dotnet`，`Build.ps1` 應能走到與原先相同的 publish 指令。
- 本機既有 `deployment/release-inputs.json` 若只有舊三欄（`server_sha256`／`catalog_sha256`／`java_runtime`），`verify_current.py` 仍走原本 JAR 相對路徑，不需立刻改本機檔。

## 未驗證什麼

- 未在 Windows 實機跑 `Build.ps1` 到 publish（任務指定由本機 test 另派）。
- 未比較 publish 前後 EXE／DLL SHA（此環境無 .NET Windows 工作負載、無 `release/current`）。
- 未啟動遊戲、未連正式 DB、未改 `LiTo.bin`／shared BIN。
- 未跑 GitHub Actions；合併後看 PR 的 launcher job。

## 產出路徑

- `lito登入器/LiTo381-Launcher/Build.ps1`
- `lito登入器/LiTo381-Launcher/tools/build_current.py`
- `lito登入器/LiTo381-Launcher/tools/verify_current.py`
- `lito伺服器/LiTo381-Server/deployment/release-inputs.json`（gitignore 目錄，以 `git add -f` 納入，讓 clone 後有相對路徑版本）
- `docs/reports/issue-4.md`
- `docs/REPORT.md`

## 給下一棒的提示

1. **本機 test 任務**：在 Windows 於 `D:\tmp\litotest` clone `task/4`，確認 PATH 有 `dotnet` 與 `py -3.12`（或 `python`），跑 `lito登入器/LiTo381-Launcher/Build.ps1`，核對有「Published release/current (0.6.71).」且 `release/current/LiTo381Launcher.exe` 出現。
2. 若本機已有舊 `deployment/release-inputs.json`，把 `server_jar`／`catalog` 兩欄抄進去即可；不要改 `java_runtime` 的 path／bytes／sha256，否則正式啟動身分核對會失敗。
3. 若 publish 產物 SHA 與 `docs/maintenance/release-0.6.71.json` 不同：先比 `src/development_startup.py` 的 PINS 重寫（`build_current.py` 仍會依本機檔重算 pin），那是既有行為，不是這次 PATH 解析造成的。
4. CI 綠且本機 publish 過後，把 `TASKS.md` T003 標成 review／done。

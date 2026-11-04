# Issue #4 / T003 — 登入器建置腳本去硬編路徑

## 做了什麼

只改 T003 列出的建置入口，讓腳本不再綁死單一開發機路徑。產物參數（`publish` 組態、RID、self-contained、輸出目錄）未改。

第 2 次：依審查退回，把誤納入 git 的 `deployment/release-inputs.json` 用 `git rm --cached` 移出索引。該檔在 `.gitignore` 是刻意的（每台機器 `java_runtime` pin 不同，commit 進去會在 `git reset --hard` 時蓋掉本機檔）。工作區複本仍可留在本機，不再追蹤。

### 被替換的硬編路徑

| 檔案 | 原硬編路徑 | 改為 |
|---|---|---|
| `lito登入器/LiTo381-Launcher/Build.ps1` | `C:\Users\CHAOREN\AppData\Local\Programs\Python\Python312\python.exe`（兩處：build / verify） | 先試 `py -3.12`，沒有再找 PATH 的 `python`／`python3` |
| `lito登入器/LiTo381-Launcher/tools/build_current.py` | `C:\Program Files\dotnet\dotnet.exe` | `shutil.which('dotnet')`（PATH） |
| `lito登入器/LiTo381-Launcher/tools/verify_current.py` | 無 Python／dotnet 硬編；`release-inputs.json` 已用相對路徑 | 以 repo 根解析；沒有 `server_jar` 欄就用 `app/server/LiTo381-SharedResources-candidate.jar` |
| `lito伺服器/LiTo381-Server/deployment/release-inputs.json` | 不進 git（審查要求） | 本機既有檔維持原樣；路徑改相對 repo 根留給本機檔／後續任務，不寫進 repo |

未改、且說明原因：

- `java_runtime.path` 是每機 JDK 身分 pin，所以整個 `release-inputs.json` 不進 git。
- `src/FormalStandardSources.cs`、`src/development_startup.py` 仍有開發機絕對路徑；T003 範圍未列，未動。
- `verify_current.py` 的 `server_jar` fallback 維持原寫法，未再改。

## 怎麼驗證

**已驗證**

- 靜態核對：`Build.ps1`、`build_current.py` 不再出現上述兩個絕對路徑。
- `build_current.py` 的 `dotnet publish` 參數與原先相同：`-c Release -r win-x86 --self-contained true -o release/current`。
- `python3 -m py_compile` 通過 `build_current.py`、`verify_current.py`。
- CI 兩個 job 皆綠（PR #8，commit `823e2b4`）：`build / Launcher`、`build / Server`。launcher job 只跑 `dotnet build`，不走 `Build.ps1`。
- `git ls-files` 不再列出 `deployment/release-inputs.json`；`git check-ignore` 對到 `.gitignore` 第 27 行。

**推測**

- 本機若已裝 Python 3.12（`py -3.12`）與 PATH 上的 `dotnet`，`Build.ps1` 應能走到與原先相同的 publish 指令。
- 本機既有 `release-inputs.json` 沒有 `server_jar` 時，`verify_current.py` 走預設相對路徑，不必改本機檔。

## 未驗證什麼

- 未在 Windows 實機跑 `Build.ps1` 到 publish（任務指定由本機 test 另派）。
- 未比較 publish 前後 EXE／DLL SHA。
- 未啟動遊戲、未連正式 DB、未改 `LiTo.bin`／shared BIN。
- 本次文件／untrack commit 之後的 CI 尚未跑完。

## 產出路徑

- `lito登入器/LiTo381-Launcher/Build.ps1`
- `lito登入器/LiTo381-Launcher/tools/build_current.py`
- `lito登入器/LiTo381-Launcher/tools/verify_current.py`
- `docs/reports/issue-4.md`
- `docs/REPORT.md`

## 給下一棒的提示

1. **Claude 複審**：確認 PR #8 diff 已不含 `deployment/release-inputs.json`；其餘 Invoke-Python／`shutil.which('dotnet')`／repo 根解析維持通過。
2. **本機 test**：在 Windows 於 `D:\tmp\litotest` clone `task/4`，確認 PATH 有 `dotnet` 與 `py -3.12`（或 `python`），跑 `lito登入器/LiTo381-Launcher/Build.ps1`。`verify` 需要本機既有的 `deployment/release-inputs.json`（gitignore，不在 clone 裡）。
3. 不要把本機 `release-inputs.json` 加進 git；不要改 `java_runtime` 的 path／bytes／sha256。
4. 本機 publish 過後，把 `TASKS.md` T003 標成 review／done。

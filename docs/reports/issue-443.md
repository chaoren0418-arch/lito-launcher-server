# Issue #443（T152）：外部入口 `TW13081901.txt` 釘選不符——正式模式資源檢查查明

## 結論

**正式 standard 模式也檢查這個檔，而且用的是同一個釘選值（`7EBE2B46…4008`）。** 外部入口本來就是呼叫正式 helper 的同一個 `check_source`，兩邊的資源檢查**完全相同**，所以沒有「對齊」可做。
客戶端的 `TW13081901.txt` 在 **2026-11-03 14:57** 被換成 `2-登入器` 專案編碼器目錄裡的新版（`28C4FC60…8DB8`），時間晚於 0.6.71 發布（2026-10-23）。依任務規則：**不改釘選**，證據列在下面，交主控／使用者決定。
另外修好第 4 項：外部入口在 `check_source` 等啟動前檢查失敗時，現在一律送出 `failed`＋`finished(cleanup_verified=true)`，GUI 不會再顯示「清理尚未確認」。

## 已驗證

### 1. 釘選從哪裡來、兩種模式各自檢查什麼

| 項目 | 正式 standard（0.6.71） | 外部入口（`--external-server-profile`） |
|---|---|---|
| C# 入口 | `DevelopmentStartup.cs`：先比對 `development_startup.py` 的 SHA（`EntrySha = ADFF1EDC…C9CD`），再用 Python 執行它 | `ExternalServerStartup.cs`：比對 `external_server_startup.py`（`HelperSha`），**也**比對同一個 `development_startup.py` 的 `EntrySha` |
| Python 資源檢查 | `development_startup.py:180` `main()` → `check_source(args.client_directory)` | `external_server_startup.py` `_formal_helper()` 載入同一個 `development_startup.py`（再比對一次 `ADFF1EDC…`），然後呼叫 `dev.check_source(...)`；啟動原生程式前和多開時各再呼叫一次 |
| `check_source` 內容 | 客戶端目錄必須是 `D:\lineage開發\0-客戶端`；`PINS` 表的 33 個檔案逐一比 SHA-256（其中唯一的客戶端檔是 `0-客戶端\TW13081901.txt` → `7EBE2B46…4008`，`development_startup.py:35`）；`LiTo.bin` = `CE6EE68A…`；`world_resource_preflight.configure`（客戶端不得有 `ddraw.dll`） | 同左，是同一個函式 |
| 外部入口多做的檢查 | — | `litoddraw.dll`（`E47BD6C6…`）、`lito381-compat.ini` 只讀比對（T139）；外部端點 TCP 連線 |

- 三份 `development_startup.py` 的 SHA-256 都是 `ADFF1EDC784B6F54803A050D32E3D6D3EE06EB9242DA0AF1B4B227DCB3EAC9CD`：本 worktree `src/`、原始專案 `src/`、原始專案正式發布 `release/current/`（`release-0.6.71.json:18` 列的就是這個檔）。所以 0.6.71 的正式啟動跑的就是同一張 PINS 表。
- 同一個 `7EBE…` 值也出現在 `app/runtime-pins.json:18`、`server-patches/repair-032～034/build_launcher*.py:30`（釘選的建置來源），以及 `app/common_catalog_manifest.json:3` 的 `source_sha256`——**正式共用動畫目錄 `common_catalog.bin`（30000 筆）就是從 `7EBE…` 版產生的**。
- `FormalStandardSources.cs` 沒有客戶端資源檢查（只核對 Python 執行檔等來源）；`ClientResourceAudit.cs` 檢查的是 `TW13081901.bin`／`.pak`，不是 `.txt`。

### 2. `28C4FC60…` 的來源

- repo（含全部分支歷史）中，`28C4FC60` 只出現在 #413、#414 的報告裡，沒有任何資源 profile、tw-bin 更新紀錄或建置腳本使用它。
- 唯讀掃描 `D:\lineage開發` 下所有 `TW13081901*.txt`（深度 5）：

| SHA-256 前 16 碼 | 大小 | 修改時間 | 路徑 |
|---|---|---|---|
| `28C4FC600833FB8C` | 5,990,880 | 2026-11-03 14:57:05 | `0-客戶端\TW13081901.txt`（建立時間 2026-10-31 06:37） |
| `28C4FC600833FB8C` | 5,990,880 | 2026-11-03 14:57:05 | `2-登入器\打包建置\編碼器\TW13081901.txt` |
| `7EBE2B464C8B1EA9` | 5,974,928 | 2026-07-21 14:12:50 | `2-登入器備份\打包建置\編碼器\`、`登入器參考用資源\2-登入器260721／260809／260814\打包建置\編碼器\` |
| 其他（`C1ED…`、`2061…`、`AA68…`） | — | 2026-03～07 | 公版修正門跟icon、L1JGO 參考、泡麵登入器（無關） |

- 客戶端現檔與 `2-登入器` 專案（另一個登入器專案，不是本 repo）編碼器目錄的檔案**大小、修改時間、雜湊完全相同**；釘選值則是 `2-登入器` 07-21 版。
- 內容差異（cp950 逐行比對，腳本 `scratch/issue-443/diff_tw.py`、`sum_tw.py`）：4 個區塊，**30 個原本是空佔位（`#id 1 / 102.(0)`）的 sprite 編號被填上定義**，總筆數不變（131,706 → 131,897 行）：
  - `#12233` milkcow（`32=945`）
  - `#22713～22728` 三國造型（呂布、關羽、貂蟬、趙雲本體／影子／特效）
  - `#24004` minotaur（`1=6624`）
  - `#26001～26012` 歐瑞壁爐／煙囪特效
- 本 repo 的正式成功啟動紀錄（原始專案 `app/runs/`）最後一筆是 2026-10-23（`cleanup-smoke-…`，有 `native-probe.shared-ready.json`）；`app/gui-runs/` 最後一筆是 2026-10-14。**沒有任何 2026-11-03 之後的正式啟動紀錄。**

### 3. 處理（依任務第 3 點第二種情況）

正式模式用的是同一個釘選 → **沒改釘選、沒改客戶端、沒改正式 helper（`development_startup.py` 的 SHA 仍是 `ADFF1EDC…`）**。

### 4. `check_source` 在事件保護區外拋錯（#414 報告第 47 行）

- 原因：`external_server_startup.py` 的 `main()` 在 `try` 之外呼叫 `_formal_helper()`、DLL 釘選比對與 `dev.check_source()`；拋錯時 Python 直接以 traceback 結束，不送 `finished`，`ExternalServerStartup.cs:220` 因而得到 `CleanupQualified=false`，GUI 顯示「本次外部伺服器啟動清理尚未確認，保留控制」。
- 修正：建立 run 目錄之前的所有檢查（run id、外部 profile、正式 helper 指紋、DLL 釘選、**`check_source`**、ini／DLL 比對、run root）都包在同一個保護區，失敗時由新的 `refuse_before_start()` 送出 `failed`（含 `stage`、`reason`、`writes: 0`）與 `finished`（`cleanup_verified: true`、`accepted: false`）。這時候沒有 run 目錄、沒有 JVM、沒有原生程序，清理確實是完成的。GUI 會顯示真正的失敗原因（例如 `Startup file changed: …TW13081901.txt`）。
- `main()` 新增可選參數 `argv`、`formal_helper`（只供測試注入），正常執行行為不變。
- `ExternalServerStartup.cs` 的 `HelperSha` 更新為 `7E9EE3D832C594E4F31D8FF4CAFD4CF1600B6A4978CF392F2569BED09D547C2F`。
- 新增測試 `PreStartRefusalTests`（`tests/test_external_server_startup.py`）：
  - 模擬 `check_source` 以 TW13081901.txt 指紋不符拒絕 → 事件只有 `failed`(stage=`check_source`)＋`finished`(cleanup_verified=true)，沒建 run 目錄，客戶端目錄不變；
  - `check_source` 通過時才會走到 ini／DLL 比對（順序與正式流程一致）；
  - 錯誤 run id、錯誤 profile 也都正常結束；
  - 原始碼層確認外部入口呼叫的就是正式 helper 的 `check_source`，正式 helper 仍釘選 `0-客戶端\TW13081901.txt`。
- `dotnet build`（Release）：成功，0 警告 0 錯誤。Python 檔以 `py_compile` 檢查語法通過。

## 推測

- 客戶端的 `TW13081901.txt` 是 2026-11-03 從 `2-登入器` 專案的編碼器輸出更新過來的（兩檔時間戳完全一致；客戶端檔建立於 10-31，可能是 10-31 整體複製客戶端、11-03 再覆蓋）。是誰、用什麼工具覆蓋，本機沒有紀錄可查。
- 「使用者用正式登入器可以進遊戲」應該是 11-03 以前的經驗。依程式碼，**目前正式 0.6.71 standard 模式按啟動也會以同一個 `Startup file changed: …TW13081901.txt` 拒絕**。
- 即使放寬或更新釘選，`common_catalog.bin` 和 `common_catalog_manifest.json`（也在 PINS 內）是從 `7EBE…` 版產生的；新版新增的 30 個 sprite（三國造型、壁爐等）不會出現在 LiTo 的共用目錄中。若要改用新版，應該重產目錄並重新發布，而不只是換一個雜湊。

## 待驗證

- CI（GitHub Actions `test_external_server_startup.py`、`dotnet build`）的結果：依本機規定沒有在本機執行 unittest，以 PR 的 CI 為準。
- 實機：目前客戶端狀態下，正式 0.6.71 standard 模式按啟動是否真的顯示 `Startup file changed`（依程式碼推斷會）。需要使用者或 Codex 實測確認。
- 實機：外部入口在指紋不符時，GUI 是否顯示失敗原因、且不再出現「清理尚未確認」。

## 要交給使用者／主控決定（不可逆，標 `needs:user`）

擇一：
1. **把客戶端 `TW13081901.txt` 還原成 07-21 版**（`2-登入器備份\打包建置\編碼器\TW13081901.txt`，`7EBE…`）：0.6.71 正式版與外部入口可立刻恢復，但會失去 11-03 新增的 30 個 sprite 定義（如果 `2-登入器` 那邊需要它們）。
2. **接受新版**：開新任務，從 `28C4…` 重產 `common_catalog.bin`／manifest、更新 PINS（`development_startup.py`、`runtime-pins.json`、`repair-03x` 建置來源）、更新 `EntrySha`，再發布新版登入器。這會改到正式 helper，屬於新版本發布。

## 產出路徑

- `lito登入器/LiTo381-Launcher/src/external_server_startup.py`（啟動前檢查統一保護）
- `lito登入器/LiTo381-Launcher/src/ExternalServerStartup.cs`（`HelperSha`）
- `lito登入器/LiTo381-Launcher/tests/test_external_server_startup.py`（`PreStartRefusalTests`）
- 本報告；一次性比對腳本與差異輸出在 `scratch/issue-443/`（不進 git）
- 分支：`task/443`

## 給下一棒的提示

- 主控：上面兩個選項要交使用者裁定；選定前，外部入口的 #284／#414 項目 08 無法重測。
- 若選 2，`development_startup.py` 改動後要同步 `DevelopmentStartup.EntrySha` 與 `external_server_startup.FORMAL_HELPER_SHA`，`common_catalog` 的產生工具在 `tools/world-resource-compat/`（`build/candidate/common_catalog_manifest.json` 也記錄 `7EBE…`）。
- 正式 helper `development_startup.py:180` 也是在保護區外呼叫 `check_source`（正式模式同樣會在指紋不符時顯示「清理尚未確認」）。本任務規定正式行為不變，所以沒改；可以在選項 2 的新版本中一起修。

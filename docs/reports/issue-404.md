# Issue #404：T140 登入器連外部實驗伺服器（不自啟、不覆寫埠）

**結論：** 登入器原本**沒有**「連已在跑的外部伺服器、不自己啟動」的入口，所以本任務新增了一個最小的開發／驗收入口：`LiTo381Launcher.exe --external-server-profile <profile.json>`。正式 standard 模式的程式路徑與 helper 都沒改。#284 的 08 可依 [`docs/acceptance/launcher-external-server.md`](../acceptance/launcher-external-server.md) 重測。

## 1. 現況調查（為什麼沒有既有入口）

| 入口 | 能否連外部 42083 且不自啟？ | 依據 |
|---|---|---|
| 預設 GUI（正式 0.6.71） | 否。`DevelopmentStartup` → `development_startup.py` 一定先 `Pair.start_pair()` 啟動自己的 JVM，`FormalStandardReady.Endpoints` 寫死 `127.0.0.1／127.0.0.2:32000` | `DevelopmentStartup.cs:69-88`、`FormalStandardReady.cs:5-6`、`app/server/Start-Shared-World.py:20` |
| `--legacy-formal-mode`（#284 用的） | 否。`LoadSettingsIntoUi`／`ReadSettingsFromUi` 每次都強制 `StartLocalServer=true`、`ServerProfilePath=lito-shared.json`（32000） | `MainForm.cs` 原 289–292、364–367 行 |
| `--smoke-test` | 只做預檢，也強制 `StartLocalServer=true` | `Program.cs` 原 206–235 行 |
| `--server-orchestration-smoke-test` | 自己啟動伺服器 | `Program.cs` 原 72–167 行 |
| `--start-memory-server／--start-memory-game` | 記憶體測試服，自己啟動 | `MemoryServerControl`、`DefaultMemoryStartup` |

## 2. 做了什麼

| 檔案 | 內容 |
|---|---|
| `src/ExternalServerProfile.cs`（新） | 外部伺服器設定（`kind: "external"`、host、port、staticDatabase、sources）；只允許 loopback IPv4、port 1024–65527、來源 id 1–8 唯一、名稱 cp950 ≤ 63 bytes 且無逗號、資料庫名稱唯一；沒有 AllowNetwork 例外 |
| `src/external_server_startup.py`（新） | 驗收 helper：載入並比對正式 `development_startup.py`（SHA 必須是 `ADFF1EDC…`），重用它的 `check_source`、`native_args`、`select_source`、`source_catalog`、`find_child`、`AdditionalNative`；把「啟動 Pair」換成對外部端點做 TCP 連線檢查（不送資料）。不啟動、不停止任何 JVM，不改埠 |
| `src/ExternalServerStartup.cs`（新） | C# 端：helper 指紋釘選（`D0FE39EB…1D32`）、可用性檢查、READY 驗證（`external_server=true`、`server_owned=false`、全部端點可連、port 與設定一致）、原版客戶端身分驗證與多開，結構比照 `DevelopmentStartup` |
| `src/Program.cs` | 新增 `--external-server-profile <path>`；和舊版模式、隔離測試、記憶體服／遊戲、開發自啟、smoke 測試同時出現時拒絕（結束碼 2）；沒給這個參數時回傳 null，正式流程不變 |
| `src/MainForm.cs` | 只有在外部模式（`_externalProfilePath != null`）才分支到 `ExternalServerStartup`；選服 id 取自外部設定，不呼叫讀正式 `server.properties` 的 `SourceServerChoice.Load()`；標題與結束按鈕文字標示外部模式 |
| `src/DevelopmentStartup.cs` | `EntrySha` 由 `private` 改 `internal`（讓外部模式驗證同一個正式 helper），值不變 |
| `src/LiTo381Launcher.csproj` | 複製 `external_server_startup.py` 到輸出；`InternalsVisibleTo LiTo381Launcher.Tests` |
| `server-profiles/external/p24-experiment-42083.json`（新） | #284 實驗服範本（42083、source 1 → 42084）；放子目錄，不會被 `..\server-profiles\*.json` 複製到正式輸出，也不會出現在舊版 profile 清單 |
| `tests/LiTo381Launcher.Tests/`（新） | xUnit：設定驗證（19 種非法案例、範本可載入、正式 `lito-shared.json` 格式不被當外部設定）、READY 驗證（正式埠 32000、server_owned、缺端點、端點不可連、來源 id 不符都拒絕）、helper 參數、CLI 互斥、`FormalStandardReady.Endpoints` 仍是 32000 |
| `tests/test_external_server_startup.py`（新） | Python unittest：同一套設定規則、`effective` 與正式 `select_source` 輸入一致、端點檢查只連線不送資料（假連線函式） |
| `.github/workflows/build.yml` | launcher job 加 `dotnet test` 與 Python unittest |
| `.gitattributes`（新） | `external_server_startup.py -text`，避免換行轉換讓 SHA 釘選失效 |
| `docs/acceptance/launcher-external-server.md`（新） | Codex 重測 08 的完整步驟 |

## 3. 資源預檢失敗的原因（任務第 4 點）

是**另一個原因，不是埠**：#284 用 `--legacy-formal-mode`，該模式檢查 0.5.x 時代的隔離相容執行副本（`runtime/client-isolated`、舊世代 Text／Tile／Sprite 大小、`litolauncher-c.pak`、`LiTo\LTtext` 領獎覆蓋層、`LiTo381-Compat\publish\ddraw.dll`）。#284 證據 `docs/reports/issue-284/launcher-validation-retest.txt`（`task/284`）裡的 `[X]` 項目全部屬於這套檢查與強制自啟服。0.6.71 正式預設 GUI 改用共用資源流程（`world_resource_preflight`、TW13081901、`app\compat\litoddraw.dll`），直接用 `D:\lineage開發\0-客戶端`，不需要那些檔案。新入口沿用正式流程的資源路徑，所以這些 `[X]` 不會出現。詳見驗收文件「為什麼 #284 第 4 輪的 08 會失敗」。

## 4. 怎麼驗證

### 已驗證

- `dotnet build lito登入器/LiTo381-Launcher/tests/LiTo381Launcher.Tests/LiTo381Launcher.Tests.csproj -c Release`（會一併建 `src/LiTo381Launcher.csproj`）：0 警告、0 錯誤。
- `python -m py_compile` 檢查 helper 與測試檔語法通過。
- `src/development_startup.py` 的 SHA 仍是 `ADFF1EDC…C9CD`（等於 `DevelopmentStartup.EntrySha`）；`external_server_startup.py` 的 SHA `D0FE39EB…1D32` 已寫進 `ExternalServerStartup.HelperSha`。
- 靜態對照：#284 公開設定 `port=42083`、`bind=127.0.0.1`，`UnifiedConfiguration` 的來源埠規則是 `port + id`，與 #284 實測監聽（42083／42084）一致；新入口的端點預期同為 `127.0.0.1:42083`、`127.0.0.1:42084`。
- 正式路徑：沒給 `--external-server-profile` 時 `ExternalServerMode=false`，`MainForm` 走原本的 `DevelopmentStartup`／`DefaultMemoryStartupRelease` 分支；`development_startup.py`、`Start-Shared-World.py`、`FormalStandardReady`、`FormalStandardSources`、`LauncherService` 都沒改。

### 待驗證（交 CI／Codex）

- 單元測試（`dotnet test` 與 Python unittest）依本機執行限制**沒有在本機執行**，交給 PR 的 GitHub Actions。
- 實機：在實驗服 ready 的狀態下用新入口連 42083 → 選服 → 登入 → 大廳 PNG（#284 項目 08）。
- 正式交付檔 PINS（`app\…`、`0-客戶端\TW13081901.txt`）在 Codex 環境是否仍一致：helper 會在啟動前檢查，不符會直接報 `Startup file changed`。

### 推測

- 外部模式下的登入、角色選單與大廳行為取決於實驗服本身與實驗帳號資料；入口只保證客戶端以正式流程啟動、connect 轉向到設定的 host／port。
- 外部模式的「Server 完整素質」面板不會更新（JVM stdout 不歸登入器所有），不影響登入流程。

## 5. 產出路徑

- 程式：`lito登入器/LiTo381-Launcher/src/{ExternalServerProfile.cs, ExternalServerStartup.cs, external_server_startup.py, Program.cs, MainForm.cs, DevelopmentStartup.cs, LiTo381Launcher.csproj}`
- 範本：`lito登入器/LiTo381-Launcher/server-profiles/external/p24-experiment-42083.json`
- 測試：`lito登入器/LiTo381-Launcher/tests/`
- 文件：`docs/acceptance/launcher-external-server.md`、本報告
- 分支：`task/404`

## 6. 給下一棒的提示

- **Codex（#284 只重測 08）**：照 `docs/acceptance/launcher-external-server.md`。**不要加 `--legacy-formal-mode`**。來源名稱建議先唯讀查 `lito_source_server.display_name` 填進設定複本。證據在 `app\gui-runs\external-*` 與 `app\runs\external-*`。
- 若失敗在「選服後連線／帳密」，問題已經在伺服器協定或帳號資料層，請附 Server 主控台與 `native.stderr.log`。
- 若 helper 指紋不符：確認是從本分支（含 `.gitattributes`）建置的輸出，不要手改常數。
- 舊版正式模式的強制覆寫（`MainForm` 的 `StartLocalServer=true`）沒有改，因為那是正式行為的一部分；之後若要淘汰舊版模式，另開任務。

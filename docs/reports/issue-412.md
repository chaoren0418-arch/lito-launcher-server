# Issue #412 — T139：外部伺服器驗收入口不寫 0-客戶端、收據可改寫到 worktree

階段 code／runner claude-local／分支 `task/412`

## 做了什麼

### 1. 外部入口不再寫 `0-客戶端`（`external_server_startup.py`）
- 移除 `shutil.copyfile(dev.DLL, dev.CLIENT / 'litoddraw.dll')` 與 `(dev.CLIENT / 'lito381-compat.ini').write_text(...)`，`shutil` import 也一併移除。
- 新增純函式 `verify_client_compat(client, scale)`：只讀取 `litoddraw.dll`（比對 SHA-256 釘選 `E47BD6C6…6A8A`）與 `lito381-compat.ini`（比對正式流程在本次縮放下會寫的內容，CRLF／LF 視為相同）。
  - 兩者都相同：直接沿用，收據寫 `writes: 0`。
  - DLL 不存在或 SHA 不同：拒絕，訊息列出檔名、現有 SHA、預期 SHA。
  - ini 的 `ScalePercent` 不同：拒絕，訊息寫出現有 ScalePercent，並請操作者「在登入器改選 N% 縮放後再試」。
  - ini 其他內容不同或不存在：拒絕，列出現有與預期內容。
  - 任何情況都**不覆寫**。
- 核對做兩次：helper 一啟動（`check_source` 之後、建立任何目錄之前）做一次，開 `LiTo.bin` 前（原本複製的位置）再做一次。第一次拒絕時 helper 送出 `failed`（含 `differences`、`writes: 0`）與 `finished`（`cleanup_verified: true`，什麼都沒啟動），所以 C# 端會顯示原因，不會誤判成「清理尚未確認」。
- 釘選一致性：`CLIENT_DDRAW_SHA` 與正式 helper 的 `dev.PINS[dev.DLL]` 執行期互相核對，不同就拒絕。
- 0.6.71 正式流程 `development_startup.py`（SHA `ADFF1EDC…C9CD`）**未改**，`--legacy-formal-mode` 未改。

### 2. 收據／run 目錄開關：環境變數 `LITO_LAUNCHER_RUN_ROOT`
- 選用環境變數（與既有 `LITO_LAUNCHER_SETTINGS_DIRECTORY` 同風格；不動 `Program.cs` 的命令列解析）。
- C#（`ExternalServerStartup.cs`）：`ResolveRunRoot`／`CurrentRunRoot`／`ReceiptDirectory`。設定時 `gui-runs/external-*` 改寫到 `<run root>\gui-runs\`，並以 `--run-root <dir>` 傳給 helper；helper 的 `runs/external-*` 改到 `<run root>\runs\`。未設定時兩邊行為與以前相同。
- 拒絕條件（C# 在 `CheckAvailability` 就檢查，狀態列顯示且不能按啟動；helper `resolve_run_root` 再檢查一次）：非絕對路徑、目錄不存在（不自動建立）、路徑任何一層是 junction／symlink／reparse point、位於客戶端目錄底下（`client-runs` 這種同前綴的兄弟目錄可以）。
- `request.json` 多記 `run_root` 與 `client_writes_by_launcher: 0`；helper `terminal.json` 多記 `run_root`、`client_compat`、`client_compat_before_native`。

### 3. Helper SHA 釘選
| 檔案 | 舊 SHA-256 | 新 SHA-256 |
|---|---|---|
| `external_server_startup.py`（`ExternalServerStartup.HelperSha`） | `D0FE39EB6E379875F04E28FBF12B532E8F6AA2EB54347161E905DD3C86191D32` | `3CAE67C05697510CA3B35C8354D7C14E362D4F19A145750A472FD665139428B9` |
| `development_startup.py`（`DevelopmentStartup.EntrySha`） | `ADFF1EDC…C9CD` | 不變（檔案未改） |

新值用 `Get-FileHash` 對 repo 內檔案計算後替換常數，並對 `dotnet build` 輸出目錄的副本再算一次，兩者相同。`DevelopmentStartup.cs` 不需要改。

### 4. 文件 `docs/acceptance/launcher-external-server.md`
- 新增「與 `0-客戶端` 的互動：只讀取與核對」一節（逐檔列出外部入口做什麼、不符時怎麼辦）。
- 列出啟動遊戲後由 `LiTo.bin`／`litoddraw.dll` 自己寫的執行期檔：`lito381-compat.log`（`native/ddraw_proxy.cpp:180`，ini `Logging=1`）、`LiToError.txt`，交 T141 以前後快照記錄。
- 步驟 3 改成同時設定 `LITO_LAUNCHER_SETTINGS_DIRECTORY` 與 `LITO_LAUNCHER_RUN_ROOT` 的完整命令；更新 helper SHA、證據位置、常見失敗訊息。

### 5. 測試
- Python `tests/test_external_server_startup.py`（CI windows-latest、Python 3.12）新增：
  - `ClientCompatTests`：DLL 與 ini 相同→通過且目錄快照（內容＋mtime）不變；LF ini 也接受；DLL 不同→拒絕、訊息含現有／預期 SHA、檔案不變；DLL 不存在→拒絕；ini 縮放不同→拒絕且訊息含 `ScalePercent=200`／`200%`；ini 其他內容不同→拒絕；原始碼不得再出現 `copyfile`／`shutil`，含 `CLIENT` 的行不得有 `write_text`／`write_bytes`／寫入模式 `open(`；`CLIENT_DDRAW_SHA` 與 ini 樣板要跟正式 helper 原文一致。
  - `RunRootTests`：存在的目錄可用；空白／相對路徑／不存在→拒絕；客戶端本身或底下→拒絕；路徑上有 junction（注入判斷）→拒絕；真實 symlink→拒絕（不能建 symlink 時 skip）。
  - 全部用 `tempfile` 模擬客戶端，不碰真實 `0-客戶端`。
- C# `ExternalServerTests.cs` 新增 `ExternalRunRootTests`（未設定沿用 app、存在目錄改寫收據、相對／不存在拒絕、客戶端底下拒絕、同前綴兄弟目錄接受、junction 拒絕）與 `HelperArgumentsCarryRunRootWhenSet`；原有 `HelperArgumentsPassProfileAndPortUntouched` 不變（未設定時不加參數）。

## 怎麼驗證

### 已驗證（本機）
- `dotnet build lito登入器/LiTo381-Launcher/tests/LiTo381Launcher.Tests/LiTo381Launcher.Tests.csproj -c Release`：建置成功、0 個錯誤（連帶建置登入器）。
- 輸出目錄 `src/bin/Release/net10.0-windows/external_server_startup.py` SHA = `3CAE67C0…28B9` = 新釘選；`development_startup.py` = `ADFF1EDC…C9CD`（未變）。
- Python 靜態檢查：兩個 `.py` 都能 `ast.parse`。
- grep：`external_server_startup.py` 已沒有 `copyfile`、`shutil`；對 `dev.CLIENT` 的使用只剩 `verify_client_compat`（讀取）、`render_log` 讀取、`cwd=dev.CLIENT`、`find_child(..., dev.CLIENT / 'LiTo.bin')` 與收據字串。helper 的寫入（`save`、`open('wb')`、`write_bytes`）全部落在 `run` 底下。
- 正式 helper 相關路徑唯讀確認：`world_resource_preflight.configure` 只有 `open("rb")`；`lito_runtime_probe_named.py` 只寫 `--output`（run 目錄）旁的檔；`AdditionalNative` 只寫 `run/client-N`。
- `0-客戶端\LiToError.txt` 存在（只用 `Test-Path` 看是否存在，沒有開檔）；`ddraw_proxy.cpp:180` 確實是 `strcat_s(path, "lito381-compat.log")` 後以 `"a"` 開檔附加。
- 建議 T141 用的 run root 路徑 `D:\lineage開發\3-lito開發\scratch\wt\task-284\scratch` 以上各層屬性都是 `Directory`（不是 reparse point）。

### 推測
- `LiToError.txt` 由 `LiTo.bin` 本身寫出（沿用 #409 的判斷，沒有逆向確認）；其他原版客戶端可能產生的執行期檔沒有逐一列舉。
- 依 #409 的唯讀觀察，現有 `0-客戶端\litoddraw.dll` 等於釘選、ini 等於 200% 內容，所以選 200% 縮放時核對應該直接通過。

### 已驗證（CI，PR #415，run 43505861070，整體 success）
- Launcher job：`dotnet test` 52/52 通過（含 `ExternalRunRootTests` 全部與 `HelperArgumentsCarryRunRootWhenSet`）；Python helper 測試 18 項通過（含 `ClientCompatTests`、`RunRootTests`）。
- 「Server tests (CI MySQL fixture, non-blocking…)」失敗，屬非阻擋 job，main 最新一次 run 同樣失敗，與本變更無關（本 PR 沒改伺服器）。
- 本機依規定沒有執行任何測試、登入器、helper 或遊戲。

### 待驗證
- 實機（T141，Codex）：用下方命令重測 08，確認日誌、收據位置、`0-客戶端` 前後快照。

## 給 T141 照抄的啟動命令

先依 `docs/acceptance/launcher-external-server.md` 步驟 1 建置，並確認輸出目錄 helper SHA = `3CAE67C05697510CA3B35C8354D7C14E362D4F19A145750A472FD665139428B9`。登入器縮放請選 **200%**（目前 `0-客戶端\lito381-compat.ini` 的 ScalePercent；選錯會被拒絕並告訴你現有值）。

```powershell
$run = "D:\lineage開發\3-lito開發\scratch\wt\task-284\scratch\t014\run-<時間>"
New-Item -ItemType Directory -Force "$run\launcher-settings","$run\launcher-runs" | Out-Null
# 啟動前快照（只讀）
Get-ChildItem "D:\lineage開發\0-客戶端" -File | ForEach-Object { [pscustomobject]@{Name=$_.Name;Length=$_.Length;LastWriteTimeUtc=$_.LastWriteTimeUtc.ToString('o');Sha256=(Get-FileHash $_.FullName).Hash} } | Export-Csv "$run\client-before.csv" -NoTypeInformation -Encoding UTF8
$env:LITO_LAUNCHER_SETTINGS_DIRECTORY = "$run\launcher-settings"
$env:LITO_LAUNCHER_RUN_ROOT = "$run\launcher-runs"
& "<輸出目錄>\LiTo381Launcher.exe" --external-server-profile "$run\external-profile.json"
# 結束後再拍 client-after.csv（同一行命令換檔名），兩份比對；預期只有 lito381-compat.log／LiToError.txt 可能變動
```

收據會在 `$run\launcher-runs\gui-runs\external-*` 與 `$run\launcher-runs\runs\external-*`。

## 產出路徑
- `lito登入器/LiTo381-Launcher/src/external_server_startup.py`
- `lito登入器/LiTo381-Launcher/src/ExternalServerStartup.cs`
- `lito登入器/LiTo381-Launcher/tests/test_external_server_startup.py`
- `lito登入器/LiTo381-Launcher/tests/LiTo381Launcher.Tests/ExternalServerTests.cs`
- `docs/acceptance/launcher-external-server.md`
- `docs/reports/issue-412.md`（本檔）

## 給下一棒的提示
- 合併後 #284 worktree 要同步 main 並重建，否則輸出目錄的舊 helper（`D0FE39EB…`）會因指紋不符被拒。
- 如果 `0-客戶端\litoddraw.dll` 或 ini 不符，**不要手動覆寫**：回報差異，由使用者決定是否以 0.6.71 正式流程開一次遊戲（正式流程會寫回正確內容）。
- 快照對比時 `lito381-compat.log`、`LiToError.txt` 的變動屬遊戲本身，不算登入器寫入；其他檔有變動才需要追查。

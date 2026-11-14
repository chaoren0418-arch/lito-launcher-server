# 登入器連外部實驗伺服器（T140／Issue #404）

給 Codex 在實驗環境重測 #284 項目 **08（登入器連大廳）** 用。登入器只連一個**已經在跑**的 LiTo Server（例如 #284 實驗服 `127.0.0.1:42083`），**不啟動內建伺服器、不覆寫埠**。正式 standard 模式（`127.0.0.1／127.0.0.2:32000`）的行為不變。

## 為什麼 #284 第 4 輪的 08 會失敗

#284 用的是 `--legacy-formal-mode`（舊版正式模式），這條路有兩個與埠無關、各自獨立的阻擋：

1. **設定被覆寫**：`MainForm.cs` 的 `LoadSettingsIntoUi`／`ReadSettingsFromUi` 每次都把 `StartLocalServer=true`、`ServerProfilePath=server-profiles/lito-shared.json`（32000）寫死，所以 `settings.json` 指定的 42083／`StartLocalServer=false` 不會生效。
2. **資源預檢走舊世代的隔離執行副本**：舊版模式由 `LauncherService.ValidateAsync` → `LiToCompatibilityResources`／`ClientResourceAudit` 檢查 `runtime/client-isolated`（舊世代 Text／Tile／Sprite 大小、`litolauncher-c.pak`、`LiTo\LTtext` 領獎覆蓋層、`LiTo381-Compat\publish\ddraw.dll`）。#284 的實際訊息（`docs/reports/issue-284/launcher-validation-retest.txt`，在 `task/284` 分支）：
   - `[X] 天堂小幫手領獎 UI: LiTo\LTtext 領獎覆蓋層缺少或指紋不符`
   - `[X] LiTo 隔離相容資源: Mss32.dll 大小 373,136 != 372,736；缺少 litolauncher-c.pak；Text.idx 大小 1,049,948 != 1,044,152；… Sprite00.pak …；其他 47 項`
   - `[X] TW/LiTo 執行隔離: TW 監測目錄與 LiTo 執行目錄都是 D:\lineage開發\0-客戶端`（沒有隔離副本）
   - `[X] LiTo DirectDraw proxy`／`LiTo window compatibility config`：run 複本下沒有 `LiTo381-Compat\publish`
   - `[X] LiTo local server`／`LiTo server config`／`一般擂台伺服器契約`：因為被強制 `StartLocalServer=true`

   0.6.71 正式流程（預設 GUI，不加 `--legacy-formal-mode`）**不走這套檢查**：它用 `development_startup.py` 的共用資源流程（`world_resource_preflight`、TW13081901 profile、`app\compat\litoddraw.dll`），直接使用 `D:\lineage開發\0-客戶端`。所以資源預檢失敗是「入口選錯」，不是客戶端資源缺漏，也不是伺服器協定問題。

本任務新增的外部伺服器入口建立在**預設 GUI 的正式客戶端流程**上，只把「啟動並檢查 32000 的內建 Server」換成「檢查外部 Server 的端點」。

## 與 `0-客戶端` 的互動：只讀取與核對（T139／Issue #412）

外部伺服器入口（登入器＋`external_server_startup.py`）對 `D:\lineage開發\0-客戶端` **只讀取、只核對，不寫入任何檔案**。正式流程（`development_startup.py`、預設 GUI、`--legacy-formal-mode`）不受影響，仍照原樣在啟動時複製 `litoddraw.dll` 並寫 `lito381-compat.ini`。

| 客戶端檔案 | 外部入口做什麼 | 不符時 |
|---|---|---|
| `LiTo.bin` | 讀取比對 SHA（同正式 helper 的 `check_source`） | 拒絕啟動 |
| `TW13081901.txt` | **只檢查存在且可讀，不比對內容 SHA**（T153／#448，見下節） | 不存在、是目錄或讀不到才拒絕啟動 |
| `litoddraw.dll` | 讀取比對 SHA-256 = `E47BD6C6EC9979C8747A523E4457133802D19F673C8DC1E67707ABFA96216A8A`（與 `app\compat\litoddraw.dll` 的正式釘選相同） | 拒絕啟動，訊息列出檔名、現有 SHA、預期 SHA；**不覆寫** |
| `lito381-compat.ini` | 讀取比對內容 = 正式流程在本次縮放下會寫的內容（CRLF／LF 視為相同） | 縮放不同：訊息寫出現有 `ScalePercent`，請在登入器改選同一縮放再按啟動；其他內容不同：列出現有與預期內容。**都不覆寫** |
| `lito381-compat.log` | 啟動前記下大小、結束後讀取新增部分複製到 run 目錄的 `native-render.log` | — |

核對在 helper 啟動時做一次（任何目錄都還沒建立前），開 `LiTo.bin` 前再做一次。拒絕時 helper 送出 `failed`（含 `differences` 與 `writes: 0`）與 `finished`（`cleanup_verified: true`，因為什麼都沒啟動），登入器日誌會顯示完整原因。

### `TW13081901.txt` 不釘選（T153／Issue #448）

- **規則**：正式 helper `development_startup.py` 的 `check_source`（正式 standard 模式與外部入口共用）對 `0-客戶端\TW13081901.txt` 只做「存在、是一般檔案、可開啟讀取、路徑不含 junction／symlink」檢查，**不比對內容 SHA**。它已從 `PINS` 移到 `UNPINNED`，`app\runtime-pins.json` 也不再列它。
- **原因**：這個檔是客戶端的 sprite 動作與速率資料表，依既定規則可以更新（2026-11-03 客戶端換成新版 `28C4FC60…8DB8`，新增 30 個原本空白的 sprite 定義，見 `docs/reports/issue-443.md`）。使用者 2026-11-14 在 #447 裁定選項 C：登入器忽略它的內容；不還原客戶端、不重產 `common_catalog.bin`、不發新版。
- **影響範圍**：LiTo 的共用動畫目錄 `common_catalog.bin`／`common_catalog_manifest.json` 仍是從 07-21 版（`7EBE…`）產生並**照舊釘選**；新版多出的 sprite 不會進 LiTo 的共用目錄，這是裁定接受的結果。
- **其他檔一律不放寬**：`LiTo.bin`（`CE6EE68A…`）、`app\` 下其餘 32 個正式交付檔、`litoddraw.dll`、`lito381-compat.ini` 的檢查都和以前相同，內容不同照樣拒絕。

0.6.71 正式流程每次啟動都會把 `0-客戶端` 的這兩個檔寫成正確內容，所以只要之前用正式流程開過一次同縮放的遊戲，外部入口就會直接沿用。若要換縮放，請在登入器改選與現有 `ScalePercent` 相同的縮放，不要手改客戶端。

### 啟動遊戲後由遊戲本身寫出的執行期檔（不是登入器寫的）

`LiTo.bin` 以 `0-客戶端` 為工作目錄執行，它和它載入的 `litoddraw.dll` 會自己寫檔，這部分不在登入器的控制範圍：

| 檔案 | 寫入者 | 依據 |
|---|---|---|
| `lito381-compat.log` | `litoddraw.dll`（相容層） | `native/ddraw_proxy.cpp:180`；ini 的 `[Diagnostics] Logging=1` 啟用時附加寫入 |
| `LiToError.txt` | `LiTo.bin` 本身 | 原版客戶端錯誤紀錄 |
| 其他原版客戶端自行產生的執行期檔 | `LiTo.bin` | 未逐一列舉 |

T141 重測 08 時請在啟動前、結束後各對 `0-客戶端` 拍一次快照（檔名、大小、修改時間、SHA-256），把差異記進報告；上表以外的變動才需要追查。

## 前置條件

| 項目 | 要求 |
|---|---|
| 實驗伺服器 | 依 #284 `docs\reports\issue-284\run-experiment.cmd` 啟動且 ready；`m4-listen-check.ps1` 為 PASS：`127.0.0.1:42083`、`127.0.0.1:42084`、`127.0.0.2:42083`、`127.0.0.2:42084` |
| 備份／lease | 依 #284 流程：有效 lease、冷備份、P24 啟動核對（本入口不碰 DB） |
| 客戶端 | `D:\lineage開發\0-客戶端`，`LiTo.bin` SHA `CE6EE68A…0292`；**不得改任何檔** |
| 正式交付檔 | `D:\lineage開發\3-lito開發\lito登入器\LiTo381-Launcher\app\`（0.6.71 正式部署）必須存在且未改；helper 會逐檔比對 `development_startup.py` 的 PINS |
| 其他程序 | 沒有其他 `LiTo.bin`、沒有其他登入器正在跑；**不需要也不應啟動**正式 32000 Server |
| Python | `C:\Users\CHAOREN\AppData\Local\Programs\Python\Python312\python.exe`（正式釘選的那一支） |

## 步驟

### 1. 取得本分支並建置

在 #284 的 worktree 合併本分支（或 main 合併 PR 後同步 main），然後：

```powershell
dotnet build "lito登入器/LiTo381-Launcher/src/LiTo381Launcher.csproj" -c Release
```

成品：`lito登入器\LiTo381-Launcher\src\bin\Release\net10.0-windows\LiTo381Launcher.exe`。同一目錄必須有：

| 檔案 | SHA-256 |
|---|---|
| `development_startup.py` | `56532A2FF06DFCF2C1569681D997E84410C8EA68CB4F65B4ED9BCA0D7CDA8189`（正式 helper；T153／#448 移除 `TW13081901.txt` 釘選，舊版 `ADFF1EDC…C9CD` 已作廢） |
| `external_server_startup.py` | `BCB588542BDD3D9D1BD52806389F2A0ECD5F661347155F1FDC4CBC360D56F3DD`（T153／#448；舊版 `7E9EE3D8…7C2F`、`3CAE67C0…28B9`、`D0FE39EB…1D32` 已作廢） |

```powershell
Get-FileHash "<輸出目錄>\development_startup.py","<輸出目錄>\external_server_startup.py"
```

不符時不要改檔或改常數，回報即可（`.gitattributes` 已設定 helper 不做換行轉換）。

### 2. 準備外部伺服器設定

範本：`lito登入器/LiTo381-Launcher/server-profiles/external/p24-experiment-42083.json`

```json
{
  "schemaVersion": 1,
  "kind": "external",
  "profileId": "t014-p24-external",
  "displayName": "T014 P24 實驗伺服器（外部，已在執行）",
  "host": "127.0.0.1",
  "port": 42083,
  "staticDatabase": "p24_380db_lito_exp",
  "sources": [
    { "id": 1, "name": "P24LiToExp", "database": "p24_accounts_lito_exp" }
  ]
}
```

- `host`／`port`：實驗服 `server.properties` 的 `bind`／`port`。來源 `id` 的登入埠 = `port + id`（42084），與正式規則（32000 → 32001）相同。
- `sources[].name`：建議改成實驗庫 `lito_source_server.display_name`（唯讀查詢，與 #284 的 05 相同）：
  `SELECT source_id, display_name, character_database FROM p24_lito_lito_exp.lito_source_server WHERE enabled=1;`
  這只是原生選服清單顯示名稱；路由靠埠。名稱不可有逗號，cp950 需 ≤ 63 bytes。
- 只允許 loopback（`127.0.0.0/8`）；不接受 `localhost`、`0.0.0.0`、IPv6 或外部 IP，也沒有「允許外部網路」選項。
- 建議把複本放在 `scratch\t014\run-<時間>\external-profile.json` 再修改，不改 repo 的範本。

### 3. 啟動登入器（外部伺服器驗收模式）

```powershell
$run = "D:\lineage開發\3-lito開發\scratch\wt\task-284\scratch\t014\run-<時間>"
New-Item -ItemType Directory -Force "$run\launcher-settings","$run\launcher-runs" | Out-Null
$env:LITO_LAUNCHER_SETTINGS_DIRECTORY = "$run\launcher-settings"
$env:LITO_LAUNCHER_RUN_ROOT = "$run\launcher-runs"
& "<輸出目錄>\LiTo381Launcher.exe" --external-server-profile "$run\external-profile.json"
```

#### 收據目錄開關 `LITO_LAUNCHER_RUN_ROOT`（T139／#412）

- 未設定（或空白）：行為不變，收據寫到 `app\gui-runs\external-*`、helper 寫到 `app\runs\external-*`。
- 設定為某個目錄：登入器收據改寫到 `<目錄>\gui-runs\external-*`，helper 改寫到 `<目錄>\runs\external-*`（登入器以 `--run-root <目錄>` 傳給 helper）。
- 拒絕條件（狀態列顯示、不能按啟動；helper 端同樣再檢查一次）：不是絕對路徑、目錄不存在（不會自動建立，請先 `New-Item`）、路徑上任何一層是 junction／symlink／reparse point、位於 `D:\lineage開發\0-客戶端` 底下。
- 這只搬收據與 run 目錄；設定檔仍由 `LITO_LAUNCHER_SETTINGS_DIRECTORY` 決定。兩個都設定後，此入口在 `0-客戶端` 與 `app\` 都不寫任何檔（`app\` 只讀取比對釘選）。

- **不要加** `--legacy-formal-mode`、`--start-memory-game`、`--start-memory-server`、`--start-development-game`、`--current-isolated-test`；同時出現會被拒絕（結束碼 2）。
- 視窗標題：`LiTo 3.81C 啟動器 0.6.71（外部伺服器驗收模式）`。
- 日誌第一行應為：`[外部伺服器] 外部伺服器驗收模式：…（127.0.0.1:42083、127.0.0.1:42084）；不啟動內建伺服器、不覆寫埠；0-客戶端只讀取核對、不寫入。`
- 有設定 `LITO_LAUNCHER_RUN_ROOT` 時，狀態列說明後面會多一句 `收據寫到 <目錄>。`。
- 介面選項：客戶端 `D:\lineage開發\0-客戶端`、視窗化、縮放任選、遊戲模式「一般無界擂台」。狀態列沒有錯誤時「啟動 LiTo.bin」可按。

### 4. 啟動遊戲並登入到大廳

按「啟動 LiTo.bin」。預期日誌順序：

1. `[選服流程] 外部伺服器：先開啟原生伺服器選單，選定後才輸入帳密。`
2. `[外部伺服器] 新場次 external-<32 碼>；…`
3. `[外部伺服器] 端點皆可連線（只做 TCP 連線、不送資料）；開始原版客戶端。`
4. `[外部伺服器] LiTo.bin PID <n> 已建立；尚不代表畫面或登入驗收。`
5. `[共同來源] TW 30000 筆＋LiTo 專用 285 筆已完成原生載入；請先在遊戲中選服，再輸入帳密。`

接著在遊戲內：選伺服器（步驟 2 的來源名稱）→ 輸入實驗帳號 → 角色選單 → 進大廳 → 截 PNG。這就是 #284 項目 08 的通過條件。

### 5. 結束與清理

- 先關遊戲，或按「結束全部遊戲（外部伺服器保持執行）」。外部 Server **不會**被登入器停止。
- 再依 #284 流程以 Ctrl+C 結束 Java、核對 PID 與埠釋放、P24 SHUTDOWN。

## 證據位置

| 路徑 | 內容 |
|---|---|
| `<run root>\gui-runs\external-*\`（未設定 run root 時為 `…\LiTo381-Launcher\app\gui-runs\external-*\`） | 登入器端：`request.json`（含外部設定、`server_owned=false`、`run_root`、`client_writes_by_launcher=0`）、`server-ready.json`、`native-created.json`、`terminal.json` |
| `<run root>\runs\external-*\`（未設定 run root 時為 `…\LiTo381-Launcher\app\runs\external-*\`） | helper 端：`server-ready.json`（端點檢查）、`server-profile.json`（給 probe 的 connect 轉向）、`native.stdout.log`／`native.stderr.log`、`native-render.log`、`terminal.json`（含 `client_compat`、`client_compat_before_native` 核對紀錄與 `client_writes_by_launcher=0`） |

未設定 run root 時這兩個位置與正式流程的 `development-*` 相同，只多了 `external-` 開頭的新場次目錄；重測 08 請設定 run root，讓收據留在 #284 worktree。

## 常見失敗訊息

| 訊息 | 原因與處理 |
|---|---|
| `External Server is not listening on: 127.0.0.1:4208x` | 實驗服沒起來或埠不同；先跑 `m4-listen-check.ps1`，或修正設定的 `port` |
| `外部伺服器只允許 loopback`、`port 必須介於 1024 到 65527` 等 | 設定檔不合規，依訊息修正 |
| `外部伺服器 helper 指紋不符`／`開發啟動 helper 指紋不符` | 輸出目錄的 `.py` 與釘選不同；重建，不要手改 |
| `Startup file changed: …`、`Reparse startup path refused` | 正式交付檔（`app\…`）或 `LiTo.bin` 與正式釘選不符，或路徑含 junction；回報，不要修改（`TW13081901.txt` 已不釘選，不會再因內容出現這個訊息） |
| `Startup file missing: …TW13081901.txt`、`Startup file unreadable: …` | 客戶端缺 `TW13081901.txt`、它變成目錄，或檔案被鎖定／無權限讀取；回報，不要從別處複製 |
| `指定客戶端的 LiTo.bin 指紋不符`、`共用資源版請選擇 D:\lineage開發\0-客戶端` | 客戶端目錄選錯 |
| `外部伺服器入口只讀取、不改 0-客戶端；…已拒絕啟動` ＋ `litoddraw.dll SHA-256 …，預期 E47BD6C6…` | `0-客戶端\litoddraw.dll` 不是正式版本；回報，不要手動覆寫（由使用者決定是否以 0.6.71 正式流程開一次遊戲讓它恢復） |
| `… 現有 ScalePercent=200，本次選擇 150；請在登入器改選 200% 縮放後再試` | 在登入器改選訊息中的縮放即可 |
| `… lito381-compat.ini 內容不同` 或 `不存在` | ini 被改過；回報差異，不要手改 |
| `LITO_LAUNCHER_RUN_ROOT 必須是絕對路徑／目錄不存在／路徑含 junction／不得位於客戶端目錄底下`；helper 端 `run root …` | 依訊息修正環境變數或先建立目錄 |
| 選服後連線失敗／帳密被拒 | 這時已經到伺服器協定或帳號資料層，屬 #284 08 本身的結果；保存上面兩個證據目錄與 Server 主控台 |

## 已知限制

- 「Server 完整素質」面板在外部模式不會更新：正式流程是讀自己啟動的 JVM stdout，外部 Server 的輸出不歸登入器所有。
- 只支援一般無界擂台（StandardArena）；租賃伺服器模式不在本入口範圍。
- 「新增遊戲視窗」可用，共用同一個外部 Server。

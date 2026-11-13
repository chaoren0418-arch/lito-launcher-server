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
| `development_startup.py` | `ADFF1EDC784B6F54803A050D32E3D6D3EE06EB9242DA0AF1B4B227DCB3EAC9CD`（正式 helper，未改） |
| `external_server_startup.py` | `D0FE39EB6E379875F04E28FBF12B532E8F6AA2EB54347161E905DD3C86191D32` |

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
$env:LITO_LAUNCHER_SETTINGS_DIRECTORY = "D:\lineage開發\3-lito開發\scratch\wt\task-284\scratch\t014\run-<時間>\launcher-settings"
& "<輸出目錄>\LiTo381Launcher.exe" --external-server-profile "<上一步的 external-profile.json>"
```

- **不要加** `--legacy-formal-mode`、`--start-memory-game`、`--start-memory-server`、`--start-development-game`、`--current-isolated-test`；同時出現會被拒絕（結束碼 2）。
- 視窗標題：`LiTo 3.81C 啟動器 0.6.71（外部伺服器驗收模式）`。
- 日誌第一行應為：`[外部伺服器] 外部伺服器驗收模式：…（127.0.0.1:42083、127.0.0.1:42084）；不啟動內建伺服器、不覆寫埠。`
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
| `D:\lineage開發\3-lito開發\lito登入器\LiTo381-Launcher\app\gui-runs\external-*\` | 登入器端：`request.json`（含外部設定、`server_owned=false`）、`server-ready.json`、`native-created.json`、`terminal.json` |
| `D:\lineage開發\3-lito開發\lito登入器\LiTo381-Launcher\app\runs\external-*\` | helper 端：`server-ready.json`（端點檢查）、`server-profile.json`（給 probe 的 connect 轉向）、`native.stdout.log`／`native.stderr.log`、`native-render.log`、`terminal.json` |

這兩個位置與正式流程的 `development-*` 相同（正式流程本來就會寫），只多了 `external-` 開頭的新場次目錄。

## 常見失敗訊息

| 訊息 | 原因與處理 |
|---|---|
| `External Server is not listening on: 127.0.0.1:4208x` | 實驗服沒起來或埠不同；先跑 `m4-listen-check.ps1`，或修正設定的 `port` |
| `外部伺服器只允許 loopback`、`port 必須介於 1024 到 65527` 等 | 設定檔不合規，依訊息修正 |
| `外部伺服器 helper 指紋不符`／`開發啟動 helper 指紋不符` | 輸出目錄的 `.py` 與釘選不同；重建，不要手改 |
| `Startup file changed: …`、`Reparse startup path refused` | 正式交付檔（`app\…`、`0-客戶端\TW13081901.txt`）與正式釘選不符或路徑含 junction；回報，不要修改 |
| `指定客戶端的 LiTo.bin 指紋不符`、`共用資源版請選擇 D:\lineage開發\0-客戶端` | 客戶端目錄選錯 |
| 選服後連線失敗／帳密被拒 | 這時已經到伺服器協定或帳號資料層，屬 #284 08 本身的結果；保存上面兩個證據目錄與 Server 主控台 |

## 已知限制

- 「Server 完整素質」面板在外部模式不會更新：正式流程是讀自己啟動的 JVM stdout，外部 Server 的輸出不歸登入器所有。
- 只支援一般無界擂台（StandardArena）；租賃伺服器模式不在本入口範圍。
- 「新增遊戲視窗」可用，共用同一個外部 Server。

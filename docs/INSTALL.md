# LiTo 0.6.71 安裝手冊

對象：在本機（Windows）安裝登入器與伺服器，達到登入、大廳、進房、戰鬥、結算、退出。  
遊戲資源**不在本包**：一律使用現有客戶端目錄，不要複製 pak／map／sprite。

## 1. 需求

| 項目 | 要求 |
|---|---|
| JDK | **17**（`java -version` 顯示 `17`）。`java` 走 PATH，不要寫死 `C:\Program Files\...` |
| Maven | 3.9（僅從原始碼打包時需要；`mvn` 走 PATH） |
| .NET | 建置登入器時需要；`dotnet` 走 PATH（T003 已去掉硬編路徑） |
| MySQL | 本機可連；庫名預設 `380_db`（靜態）、`380_lito`（LiTo 控制）、來源角色庫（如 `380_ac`／`380_ac_1`） |
| 客戶端 | **`D:\lineage開發\0-客戶端`**，內含 `LiTo.bin`（SHA-1 開頭 `CE6EE68A…`）。登入器與伺服器都引用此目錄，不把 pak／map／sprite 拷進本專案 |
| 設定 | 只有 `lito伺服器/LiTo381-Server/config/server.properties`。此檔**不進 git**、**不進安裝包** |

可選：從 repo 根跑 `packaging/Build-Package.ps1` 產生 `release/package-0.6.71.zip`（內容見 `packaging/README.md`）。

## 2. 建庫

**不要**對正在使用的玩家角色庫執行匯入。本手冊只描述結構與順序；實際匯入由資料庫管理者在核准環境執行。AI／雲端環境不得連正式 DB、不得啟停 MySQL。

1. 確認 MySQL 已在跑，編碼能收 UTF-8／utf8mb4。
2. 靜態庫 `380_db`：物品、技能、NPC 等模板。權威是現場共用天堂庫，**不是** `db/lito_static.sql`（該檔會建成名為 `lito_static` 的歷史 schema，不能拿來覆蓋 `380_db`）。
3. 建立空庫 `380_lito`（utf8mb4）。
4. 在 `380_lito` 依序執行（皆可重跑、不覆寫已改設定）：

   | 順序 | 檔案 | 用途 |
   |---|---|---|
   | 1 | `db/lito_source_server.sql` | 來源 1～8；預設前三服啟用 |
   | 2 | `db/lito_battle_settlement.sql` | 結算收據；每個啟用來源的 `character_lito` 需有 `last_result_id` |
   | 3 | `db/lito_commands.sql` | LiTo GM 指令表（`.item`／`.spawn`／`.de`） |
   | 4 | `db/lito_gm_travel.sql` | `.loc`／`.move` |
   | 5 | `db/lito_gm_polyme.sql` | `.polyme` |
   | 6 | `db/lito_room_policy.sql` | 測試房／ArenaBot |
   | 7 | `db/lito_resource_compact.sql` | 現行 10 張 `lito_resource_*` |

5. `db/380_lito_seed_example.sql` 只是範例種子，**不是**現場備份；空表且確認 NPC／道具 ID 存在於 `380_db` 後才能匯一次。
6. `db/380_ac_1.sql`、`db/380_ac_2.sql`：歷史測試來源 dump，**禁止**匯入覆蓋正式角色庫。
7. `db/modern_*.sql`：Modern 候選 schema，非正式安裝要求。
8. **不要**執行 `db/candidates/`。
9. 執行帳號（範本為 `lito_shared`）：來源庫與 `380_db`／`380_lito` 控制表僅 **SELECT**；`character_lito` 與 `lito_battle_settlement` 給 **INSERT／UPDATE**。不要給 DROP／DELETE 玩家資料的權限。

天堂角色資料只讀；執行期只回存 `character_lito` 勝敗與結算收據。

## 3. 設定 `server.properties`

```text
copy lito伺服器\LiTo381-Server\config\server.properties.example
     lito伺服器\LiTo381-Server\config\server.properties
```

安裝包對應路徑：`server/config/server.properties.example` → 本機 `config/server.properties`（不要把填好的檔放回 git 或 zip）。

必改與核對：

| 鍵 | 說明 |
|---|---|
| `database.password` | 範本是 `CHANGE_ME`，必須換成該帳號的真實密碼 |
| `database.user` | 預設 `lito_shared` |
| `database.jdbc.base.url` | 預設 `jdbc:mysql://localhost/`，結尾要有 `/` |
| `database.static.database` | `380_db` |
| `database.lito.database` | `380_lito` |
| `bind` / `game.bind` | 必須不同；預設 `127.0.0.1` / `127.0.0.2` |
| `port` | 預設 `32000`（來源入口 = 本值 + `source_id`） |

未知、重複、已退役的鍵會**拒絕啟動**。改完重啟伺服器與登入器。來源清單在表 `380_lito.lito_source_server`，不在 properties。

## 4. 啟動 `--check`／`--smoke`

在 repo 的 `lito伺服器\`（或上層捷徑）：

```text
1.啟動伺服器.bat --check
1.啟動伺服器.bat --smoke
1.啟動伺服器.bat
```

| 模式 | 預期 |
|---|---|
| `--check` | 讀設定、查 Java 17、核對正式 JAR／catalog 雜湊、查 SQL 來源、確認埠空閒後結束。成功列印 `[PASS] 設定、Java、核心、資源與埠檢查完成…` |
| `--smoke` | 真正短啟動，等到 log 出現 `login-listen`／`game-listen` 後關掉程序並釋放埠。成功列印 `[PASS] 正式 SQL 與共用資源啟動完成…` |
| 無參數 | 正式常駐；`Ctrl+C` 停止。BAT **不會**殺掉占用埠的其他程序 |

限制（已驗證於原始碼，未在本任務實跑）：

- 現行 `Start-LiToServer.ps1` 仍指向本機 JDK 與正式 candidate JAR（`lito登入器/LiTo381-Launcher/app/server/LiTo381-SharedResources-candidate.jar`），並核對 `deployment/release-inputs.json`。Maven 產物 `LiTo381-Server.jar` 類別清單與正式 JAR 仍有缺口，**不能**當成已可替換的正式核心。
- BAT 與登入器內建伺服器共用埠，**擇一**啟動。
- 打包進 zip 的 `.bat` 保留開發樹相對路徑，解壓後不能單獨當啟動器；請在完整 repo 本機執行。

## 5. 登入器設定

1. 建置（本機，T003 之後）：在 `lito登入器/LiTo381-Launcher/` 執行 `Build.ps1`。Python 用 `py -3.12` 或 PATH 的 `python`；dotnet 走 PATH。產出 `release/current/LiTo381Launcher.exe`。
2. 啟動該 EXE（或桌面「LiTo 0.6.71 正式啟動器」）。
3. 遊戲載入 `D:\lineage開發\0-客戶端\LiTo.bin`，共用該目錄的 pak／map／sprite；**不要**把它們複製進登入器或安裝包。
4. 伺服器設定與 BAT 相同，讀 `LiTo381-Server/config/server.properties`。選服來自 `lito_source_server`。
5. 相容層 DLL 以登入器 `native/` 建出的為準，不要另外散佈 `LiTo.bin`。

若打包時沒有 `release/current/`，`Build-Package.ps1` 會只打伺服器並明確警告；這是預期行為，不是失敗。

## 6. 常見錯誤

| 現象 | 可能原因 | 處理 |
|---|---|---|
| `Java 17 is required` | PATH／腳本不是 JDK 17 | 改用 17；不要設 `JAVA_TOOL_OPTIONS`、`JDK_JAVA_OPTIONS`、`_JAVA_OPTIONS`、`CLASSPATH` |
| `Required file missing: …server.properties` | 只存在 `.example` | 複製並改密碼 |
| `CHANGE_ME`／連線失敗 | 密碼或 JDBC URL 錯；MySQL 未開 | 改 `database.password`；確認 `localhost` 與帳號權限 |
| `Published JAR/catalog hash mismatch` | 正式 JAR 或 `common_catalog.bin` 與 `deployment/release-inputs.json` 不符 | 不要用未核對的 Maven 產物直接替換正式 JAR |
| `Endpoint occupied: 127.0.0.1:32000` | 已有伺服器或登入器佔埠 | 先停掉現有 LiTo；BAT 不會幫你殺 process |
| `Unsupported property syntax`／`Duplicate property`／未知鍵 | 手改 properties 破行、重複、過時鍵 | 對照 `.example`；UTF-8 無 BOM 較穩 |
| `This entry requires lito_mysql` | 模式不是共用 SQL | 不要改成其他 database.mode |
| `python`／`py` 找不到 | 建置登入器時沒有 Python 3.12 | T003：`py -3.12` 或 PATH `python` |
| `dotnet` 找不到 | PATH 沒有 SDK | 安裝後重開終端；不要寫死 `C:\Program Files\dotnet\dotnet.exe` |
| 客戶端立刻拒絕啟動 | 不是指定的 `LiTo.bin` 或資源目錄被搬走 | 維持 `D:\lineage開發\0-客戶端`；核對 SHA `CE6EE68A…` |
| 登入後沒來源／權限不對 | `lito_source_server.enabled=0` 或 `accounts.access_level` | 改表後重啟並重新登入 |
| zip 裡出現 `LiTo.bin`／`server.properties`／pak | 打包腳本被改壞或手動加檔 | 丟棄該包；用 `Build-Package.ps1` 重打（腳本會掃描並失敗） |

實機登入、看畫面、打一場戰鬥只由使用者驗收（`docs/acceptance/CHECKLIST.md`）。

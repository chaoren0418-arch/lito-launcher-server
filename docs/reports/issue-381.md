# Issue #381 — T130：G27 map 12000 資產查核（唯讀）

日期：2026-11-13　執行：claude-local　分支：`task/381`

## 結論

1. **客戶端：資產齊全。** `D:\lineage開發\0-客戶端\map\12000\` 有 66 個 `.s32`，涵蓋範圍與 `mapids` 定義完全相同；用到的 115 個 tile 在 `Tile.pak` 全部找得到；`Text.pak` 的 `zone3-c.tbl` 有地圖名稱列。查過全部 24 個有索引的 pak，沒有發現缺檔。
2. **伺服器不會經由房間模式把玩家送到 12000。** 六個戰鬥模式加城鎮的地圖池全部落在 500–574，`lito_room_policy` 沒有地圖欄位，GM 傳送只允許同地圖。
3. **G27 真正的缺口在伺服器 JAR，不在客戶端。** #83／#225 的「map 12000 缺資產」原文是啟動時的 `source map physical asset not packaged: 12000 -> 12000`。伺服器啟動時會要求靜態庫 `mapids` 的每一列都有內建地形檔；JAR 內建 715 張，沒有 12000 與 12004。參考伺服器 `1-伺服器` 在 2026-10-29 新增了這兩張圖，所以**只要靜態庫的 `mapids` 含這兩列，伺服器就拒絕啟動**，與玩家選哪張圖無關。正式 0.6.71 JAR 與目前 `src/main` 的索引相同，兩者都受影響（靜態判讀，未實跑）。

給 M4 的判讀規則：啟動 log 出現上面那句，是伺服器內建地形與靜態庫 `mapids` 不一致，不要去查客戶端 pak，也不要複製 map 進專案。

## 做了什麼

### 1. 伺服器哪裡用到 map 12000

`src/main` 沒有任何 12000 的地圖常數（全文搜尋 `12000` 只命中物品重量、怪物數值與逾時毫秒）。相關的程式是資料驅動的：

| 位置 | 作用 |
|---|---|
| `NativeDeTerrainTable.java:20` | 讀靜態庫 `SELECT * FROM mapids`（不篩選） |
| `NativeDeTerrainResources.java:48-68` | 讀 JAR 內 `/combat/terrain/source-index.tsv`，得到已打包的地形 ID |
| `NativeDeTerrainResources.java:168-172` | 逐列檢查，實體 ID（`CopyMapId>0` 用 CopyMapId，否則用 mapid）不在索引就丟 `source map physical asset not packaged: <id> -> <physical>` |
| `NativeDeTerrainResources.java:178-187` | `load` 用 `database.static.database`（範例設定為 `380_db`）開連線 |
| `ResourceDatabase.java:79` | 資源初始化時呼叫上面的 `load` |
| `NativeRoomRules.java:33` | 非記憶體帳號庫時一律呼叫 `ResourceDatabase.load`，失敗即啟動失敗 |

房間模式與地圖池（`BattleRules.java:41-73`），都不含 12000：

| 模式 | 地圖 |
|---|---|
| PET_RACE | 565, 566, 569, 570 |
| DEATHMATCH | 564, 514, 509, 508, 551, 504, 505, 552, 506, 526, 525, 516, 531, 534, 529 |
| QUEST_BATTLE | 554, 553, 513, 527, 518, 519, 520, 521, 522 |
| SIEGE_WAR | 573, 563, 547 |
| TIME_ATTACK | 540 |
| HONOR_MATCH | 510, 528, 532, 512, 517, 524, 530, 535 |
| TOWN | 500, 560 |

其他入口：
- `BattleRules.java:213-216`：選的地圖不屬於該模式就拒絕。
- `ArenaMapCatalog.java:80-171`：競技場地形只有 504–574 共 45 張，來源是客戶端 `LiTo\LTmap\<id>\*.s32`。
- `lito_room_policy`（`db/lito_room_policy.sql`、`NativeRoomRules.java:20`）只有 `policy_id`、`test_room_enabled`、`test_bot_enabled`，沒有地圖欄位。
- `GmTravel.java:49`：GM 傳送只開放同地圖。
- `LitoMapRepository.java:86-90`：`classic.world.compatibility.enabled` 在非記憶體模式預設關閉（`ServerConfig.java:513-514`）。

### 2. 伺服器內建地形與 `mapids` 的對照

| 來源 | `mapids` 列數 | 實體 ID 不在 JAR 索引 |
|---|---|---|
| repo `lito伺服器/LiTo381-Server/db/lito_static.sql` | 731 | 無 |
| `D:\lineage開發\1-伺服器\db\380_db.sql`（2026-11-04 匯出） | 733 | 12000、12004 |

- `src/main/resources/combat/terrain/source-index.tsv`：715 列，沒有 12000、12004。
- 正式 JAR `lito登入器\LiTo381-Launcher\app\server\LiTo381-SharedResources-candidate.jar`（2026-10-23，SHA-256 `E1498871…D99D498`）：索引 715 列、`combat/terrain/source/*.txt.gz` 715 個，同樣沒有 12000、12004。
- `1-伺服器\maps\` 有 `12000.txt`（512×576，2026-10-29）與 `12004.txt`（1856×1472）；比 JAR 索引多的就是這兩張（另有一個備份檔 `70.before-client-update-20260726.txt`）。
- `1-伺服器` 的 `380_db.sql` 中指向 12000 的只有 `mapids`（`【舊】說話之島`，x 32256–32767、y 32704–33279）、`mapids_group` 與 `etcitem_teleport`（道具 300116「活動傳送(古老的說話之島)」）。LiTo 伺服器 `src/main` 不讀後兩張表。12004 是 `【舊】亞丁大陸`，情況相同。

### 3. 客戶端查核

地圖目錄（`D:\lineage開發\0-客戶端\map`，共 717 個子目錄）：

| 地圖 | 檔案 | s32 涵蓋 X | s32 涵蓋 Y | 用到的 tile | `Tile.pak` 缺的 tile |
|---|---|---|---|---|---|
| 12000 | 66 個 `.s32`（2026-10-31） | 32256–32767 | 32704–33279 | 115 | 0 |
| 12004 | 544 個 `.s32`（2026-10-31） | 32512–34367 | 32128–33599 | 932 | 0 |
| 0（對照） | 65 `.s32`、50 `.seg`、4 `.MarketRegion` | 32256–32767 | 32768–33279 | 469 | 0 |
| 4（對照） | 561 `.s32`、231 `.seg`、9 `.MarketRegion`、1 `.scc` | 32448–34303 | 31936–33599 | 1880 | 0 |

12000 與 12004 的 s32 涵蓋範圍和 `1-伺服器` `mapids` 的邊界一致。地圖 0、4 各有一個 `tempseg.s32`，不是區塊檔，已略過。`LiTo\LTmap\` 有 62 個目錄（500–574 與 16384、16896、17408），沒有 12000。

PakViewer 查過的 pak（`list` 全量輸出在 `scratch/t130/list-<名稱>.txt`，不進 git）：

| pak | 項目數 | 檔名含 12000／12004 | 與地圖有關的項目 |
|---|---|---|---|
| `Text.pak` | 37,498 | 無 | `zone-c.tbl`、`zone.tbl`、`zone2-c.tbl`、`zone2.tbl`、`zone3-c.tbl`、`mapDesc-c.tbl` |
| `Tile.pak` | 5,446 | 無 | 5,364 個 `.til`；`MapSystem.xml`、`MapSystem-c.xml`、`MapSystem86.xml`、`map_config.xml`、`ExtMapAttr.xml`、`CollisionZone.xml`、`music.xml`、`SurfList.xml` |
| `Sprite.pak` | 92,356 | 無 | — |
| `Sprite00.pak`–`Sprite15.pak` | 10,207／10,232／10,320／10,458／10,593／10,700／10,844／10,949／10,972／10,970／10,862／10,716／10,522／10,357／10,255／10,184 | 無 | — |
| `LiTo\LTtext.pak` | 353 | 無 | `LTmapstr-c.tbl`、`LTzone-c.tbl`、`LTzone.tbl`、`zone2-c.tbl` |
| `LiTo\LTtile.pak` | 1,210 | 無 | 1,205 個 `.til` |
| `LiTo\LTsurf.pak` | 1,623 | 無 | — |
| `LiTo\LTsprite.pak` | 469 | 無 | — |
| `LiTo\LTicon.pak` | 227 | 無 | — |

`launcher\TW13081901.pak`（1,406,534 bytes）沒有 `.idx`，PakViewer 無法列出，未查。

把上表「與地圖有關的項目」匯出到 `scratch/t130/read/` 後搜尋 12000／12004，只有一處命中：

```text
Text.pak zone3-c.tbl 第 1195 行："古老的說話之島" 0 12000 0 0 0 0 0;
Text.pak zone3-c.tbl 第 1196 行："波士尼亞" 0 12004 0 0 0 0 0;
```

### 4. 與可用地圖的比較

| 檔案種類 | 12000 | 0（說話之島） | 504（競技場，使用者已確認能建房） |
|---|---|---|---|
| 地圖區塊 `.s32` | `map\12000`，66 個 | `map\0`，64 個 | `LiTo\LTmap\504`，7 個（`map\504` 也存在） |
| `.seg`／`.MarketRegion` | 無 | 有 | 無 |
| tile | `Tile.pak` 齊全 | `Tile.pak` 齊全 | 未逐一比對 |
| 地圖名稱 | `Text.pak` `zone3-c.tbl` 1 列（無座標範圍） | `zone3-c.tbl` 2 列 | `LTtext.pak` `LTmapstr-c.tbl`（原生目錄） |
| 小地圖設定 `MapSystem*.xml` | **無** | 有（`mapNum="0"`） | 不適用 |
| `CollisionZone.xml`、`map_config.xml`、`music.xml` | 無 | `CollisionZone.xml` 有 | `music.xml` 列的是 524、535 等 |

12000 相對於地圖 0 少的是小地圖設定與 `.seg`／`.MarketRegion`；12004 相對於地圖 4 也一樣。

## 怎麼驗證

```powershell
# pak 清單（對 24 個 .idx 各跑一次，輸出到 scratch/t130/list-<名稱>.txt）
& "D:\lineage開發\PakViewer-main\publish\win-x64\PakViewer.exe" -cli list "D:\lineage開發\0-客戶端\Text.idx"
# 匯出地圖相關表到 scratch/t130/read/
& "D:\lineage開發\PakViewer-main\publish\win-x64\PakViewer.exe" -cli export "D:\lineage開發\0-客戶端\Text.idx" zone3-c.tbl scratch\t130\read\Text__zone3-c.tbl
# s32 用到的 tile 與涵蓋範圍，對照 Tile.pak 清單
python -I scratch\t130\s32_tiles.py "D:\lineage開發\0-客戶端" scratch\t130\list-Tile.txt scratch\t130\list-LTtile.txt scratch\t130\s32-tiles.json 12000 12004 0 4
```

- s32 解析格式取自 `D:\lineage開發\L1MapViewer-main\CLI\S32Parser.cs:29-144`（第 1、2、4 層的 tile id）；66 + 544 + 64 + 560 個檔案全部解析成功。
- JAR 內容用 .NET `ZipFile.OpenRead` 讀取項目清單與索引檔，沒有執行 JAR。
- `mapids` 對照是用正規表示式讀兩份 `.sql` 的 `INSERT INTO mapids` 列（mapid 與最後一欄 CopyMapId），沒有連任何資料庫。
- 沒有修改 `D:\lineage開發\0-客戶端` 與 `D:\lineage開發\1-伺服器` 的任何檔案；沒有把 pak／map／sprite 內容放進 git（`scratch/` 被 `.gitignore` 排除）。

## 已驗證／推測／待驗證

**已驗證**（本輪靜態讀檔）
- 客戶端 `map\12000` 的檔案數、涵蓋範圍、tile 在 `Tile.pak` 齊全；`zone3-c.tbl` 有名稱列；24 個 pak 的項目數。
- `src/main` 與正式 0.6.71 JAR 的地形索引都是 715 張，不含 12000、12004。
- `1-伺服器` 的 `380_db.sql` 比 repo 的 `lito_static.sql` 多 12000、12004 兩列 `mapids`。
- 房間模式地圖池與 `lito_room_policy` 都不含 12000。

**推測**
- 正式或實驗環境的靜態庫若已含 12000／12004 兩列，伺服器會在資源初始化丟出上述例外而啟動失敗。依據是程式碼路徑；本輪沒有啟動伺服器，也沒有查實際資料庫的 `mapids`。
- #83／#225 當時看到的錯誤就是這條路徑（訊息字串相同，#225 報告也記錄 JAR 內沒有 12000）。
- 12000 沒有 `.seg` 與小地圖設定不影響進圖（客戶端優先讀 `.s32`；小地圖只影響 M 鍵地圖）。
- `launcher\TW13081901.pak` 是登入器自己的資料，與 `LiTo.bin` 載圖無關。

**待驗證**（需要實機或資料庫，交 Codex）
- 實驗庫與正式庫的靜態庫 `SELECT mapid FROM mapids WHERE mapid IN (12000,12004)` 的結果。
- 靜態庫含這兩列時，實際啟動是否停在 `source map physical asset not packaged: 12000 -> 12000`。
- `LiTo.bin` 能否載入 `map\12000`（目前沒有任何伺服器路徑會送玩家過去，所以不擋驗收）。

## 未完成

- `docs/acceptance/M4-RUNBOOK.md` 還在 `task/378`、未合併，所以 G27 小節沒有寫進去；建議文字見下一節。
- 沒有修伺服器的缺口（本任務是唯讀查核）。
- 沒有更新 `TASKS.md` 的 T130 狀態列，避免和 `task/378` 衝突。

## 產出路徑

- `docs/reports/issue-381.md`（本檔）
- `docs/reports/customer-gaps.md`：G27 列與「待驗證」段落
- `scratch/t130/`（不進 git）：24 份 pak 清單、匯出的 `.tbl`／`.xml`、`s32_tiles.py`、`s32-tiles.json`

## 給下一棒的提示

1. **主控需要決定怎麼補伺服器缺口**，三個做法：
   - 把 12000、12004 的地形加進 `src/main/resources/combat/terrain/source/` 與 `source-index.tsv`（來源是 `1-伺服器\maps\12000.txt`、`12004.txt`，屬伺服器地形文字檔，與現有 715 張同類）。建議採用，改動最小，也和參考伺服器保持一致。
   - 改 `NativeDeTerrainResources.read`，遇到未打包的列時記錄並略過，不中止啟動。之後參考伺服器再加圖也不會擋啟動，但會偏離「逐列必須有地形」的原設計。
   - 實驗庫的 `mapids` 不放這兩列。只能解實驗環境，正式庫仍會遇到。
2. M4-RUNBOOK 的 G27 小節建議寫成：「G27 已由 #381 靜態查核：客戶端資產齊全；競技場模式不會進 12000。預檢時先查靜態庫 `mapids` 是否有 12000／12004；有的話確認使用的 JAR 已含這兩張地形，否則啟動會停在 `source map physical asset not packaged`。此項沒有實機步驟。」
3. T128（實驗環境預檢）可以加一條：比對靜態庫 `mapids` 的實體 ID 與 JAR 內 `combat/terrain/source-index.tsv`，有差異就先報出來。
4. 參考伺服器之後若再加地圖，會再出現同樣的問題。

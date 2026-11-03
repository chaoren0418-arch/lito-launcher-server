# LiTo 伺服器

現行部署：**登入器 0.6.66／共用天堂資料來源／只保存 LiTo 勝敗**。本目錄保存 Java 原始碼、設定、SQL 與維護工具；正式核心由相鄰登入器專案集中保存。

基準備份與整理：登入器 `backups/baseline-0.6.60.zip` 同時保存本專案與登入器；還原／排除項目見相鄰登入器 `docs/BASELINE-0.6.60.md`。舊逐版備份已整合，歷史文件中的舊 ZIP 路徑不再作現行回復入口。

## 啟動與設定

- 雙擊 `Start-LiTo381-Server.bat`，或上層 `1.啟動伺服器.bat`。加 `--check` 預檢，`--smoke` 做短暫 SQL 啟動驗證；`Ctrl+C` 停止。
- BAT 與登入器內建伺服器使用同一組埠，擇一啟動。BAT 不會終止占用埠的其他程序。
- 唯一操作設定是 [`config/server.properties`](config/server.properties)，22 項均有中文註解。[各項用途與範圍](docs/maintenance/configuration-audit-0.6.42.md)。修改後重啟生效。
- 伺服器來源在 `380_lito.lito_source_server` 管理。固定 ID 1～8，目前公牛與天使啟用，水蛇保留停用；新增來源需準備角色庫、`character_lito` 與資料庫權限。
- `weight.rate=100.0` 代表 100 倍負重上限，所有來源共用。JVM 記憶體與 CPU 設定同時適用 BAT／登入器。

管理權限只取所選來源動態庫 `accounts.access_level`：正數為管理員；`.item`／`.spawn`／`.de` 仍須符合 `commands.access_level`，最低要求為正數。權限於登入後首次需要時讀取，隨帳號／來源變更及斷線清除；修改資料庫權限後重新登入生效。已移除 `administration.accounts`，舊鍵會被拒絕。

## 房間規則

`380_lito.lito_room_policy` 控制各頻道 Local Lobby 測試房及 ArenaBot。預設兩者啟用；只有死亡競賽單人測試加入假人，測試房不寫入勝敗。玩家自建房不加假人，依 `lito_battle_rule.minimum_players` 檢查真人數；隊伍模式檢查兩隊及人數一致條件。開始倒數與進場前均重新驗證。[詳細規則與驗證](docs/maintenance/room-rules-0.6.61.md)。

測試房列表人數包含實際列出的 ArenaBot：空房 1 人，單人加假人 2 人，兩位真人時顯示 2 人；真人開戰門檻不變。[人數顯示修正](docs/maintenance/bot-room-count-0.6.64.md)。

## 原生資源資料庫

`380_lito` 的資源表已精簡為 10 張 `lito_resource_*` 表（9 張執行表＋1 張統一待補／備註表）；有證據的技能呈現、BUFF selector、箭矢、地圖目錄與既有城鎮傳送於啟動時載入記憶體。缺證保留 NULL／停用及中文備註，查 `lito_resource_pending`。[接線範圍與驗證](docs/maintenance/resource-database-0.6.63.md)。現行建置入口為 `tools/resource-database/build.py`，舊修補工具不可直接覆蓋現版。

## 資料流

`選伺服器 → 輸入帳密 → 選角色 → 頻道／大廳 → 戰鬥 → 結算 → 大廳`

選服先固定資料來源，同名帳號以來源 ID 隔離。登入讀取天堂已保存的 SQL 資料；購買、消耗、技能與戰鬥狀態在記憶體。結算先保存可重試收據，再更新角色所屬庫的 `character_lito` 勝敗，刷新大廳與排行榜並清理場次資料。積分、月排行仍不跨伺服器重啟保存。[完整寫入邊界](SQL-PERSISTENCE-GATES.zh-TW.md)。

| 目錄／檔案 | 用途 |
| --- | --- |
| `config/` | 唯一外控 `server.properties` |
| `deployment/` | 發佈雜湊清單；`memory/` 為隔離記憶體診斷入口與模板 |
| `src/`、`lib/` | Java 主程式、測試與相依函式庫 |
| `db/` | 現行來源表 SQL、LiTo 範例資料與保留的歷史 schema；先讀 [SQL 說明](db/README.zh-TW.md) |
| `tools/` | BAT 實作、驗證及建置工具；歷史診斷需依對應版本執行 |
| `runtime/`、`loginfo/` | 執行輸出、診斷場次與 BAT 紀錄；不再存固定啟動依賴 |
| `docs/` | 歷史索引、維護規範及最新驗證紀錄 |
| `backups/` | 基準與遷移前備份；`history/` 集中舊資料與原始文件 |
| `LiTo381-Server.jar` | 原始 0.7.864／隔離記憶體診斷核心，非現行正式核心 |

## 維護入口

正式 JAR：`../../lito登入器/LiTo381-Launcher/app/server/LiTo381-SharedResources-candidate.jar`。資源目錄與客戶端共用 `0-客戶端`；雜湊固定於 `deployment/release-inputs.json`，此檔是部署驗證資料，不是遊戲外控。

房間規則 0.6.61 修補入口為 `tools/room-rules/build.py`，使用已驗證的 0.6.60 正式核心作固定基底；`tools/room-rules/test.py` 驗證原生加密封包、測試假人與開戰條件。發布需同步正式核心、deployment 雜湊、正式啟動 helper 雜湊，再執行相鄰登入器 `tools/build_current.py`。舊版工具只適用其各自保留的基底，不能直接覆蓋現行核心。

`Build-Server.ps1` 與 `tools/shared-database/build.py` 分別是原始核心、舊版資料庫修補的候選建置；不可直接覆蓋現行正式 JAR。舊版 `publish.py` 已停用，避免覆蓋後丟失後續修正。

- [驗證結果與限制](VALIDATION.md)
- [封包／功能支援範圍](SERVER-PROTOCOL-COVERAGE.zh-TW.md)
- [資料庫與記憶體規範](SQL-PERSISTENCE-GATES.zh-TW.md)
- [資料來源索引、原生證據界線與資料庫補足清單](docs/DATA-SOURCES.zh-TW.md)
- [歷史資源、維護規範、更新日誌及回復方式](docs/HISTORY.md)

## LiTo 專用 GM 指令

`.item`、`.spawn`、`.de` 只讀取 `database.lito.database` 指定庫的 `commands` 表（目前為 `380_lito.commands`）。初始門檻皆為 200，身分依所選來源帳號 `accounts.access_level` 判斷。安裝／遷移 SQL：`db/lito_commands.sql`；天堂 `380_db.commands` 不再控制 LiTo 指令。表格不能替代尚未實作的指令處理器。

### 0.6.46 龍騎士技能與斷線結算

龍騎士指定技能使用原生選取游標及含目標 ID 的 C_USE_SPELL；伺服器拒絕無目標請求，避免誤選附近單位。修復恐懼無助指定召喚物時漏取 NPC 魔防造成的斷線。原版恐懼無助的 NPC 分支不安裝閃避降低狀態，並非所有特效都代表狀態成功。

斷線延遲結算改以登入後的 SQL 戰績快照執行，避免角色暫存先被清除而失去來源及原戰績；不重新建立共用角色暫存，不改写天堂人物或物品。

驗證與限制：[0.6.46 紀錄](docs/maintenance/dragon-skills-0.6.46.json)。重跑伺服器回歸：`python tools/dragon-skill-dispatch/test.py`。目前新版職業 BUFF 狀態圖示尚未補齊；實際減傷與命中消耗已經過伺服器測試。


### 0.6.47 鎖鏈劍、屠宰者與龍之護鎧

鎖鏈劍裝備廣播改用與戰鬥相同的動作解析；龍騎士模型不支援 83 時使用 24 持武動作，武器種類與傷害仍為鎖鏈劍。屠宰者補上同戰鬥室召喚物的明確目標及施法前再驗證，沿用原有三次物理攻擊、弱點消耗與友方限制；自己的召喚物可指定。目標特效改為共用「1-伺服器」的 6509。

龍之護鎧使用 LiTo 專用 `6D h秒數 c181` 通知，客戶端新增第 77 號原生狀態槽及共用圖示 3079，保留原生排列、提示與倒數。重放不重複堆圖示，取消／到期／清理送出零秒；既有 6D 護盾類型繼續交原版接收器。此版本補齊龍之護鎧，不表示其他新增職業狀態圖示全部完成。

驗證與部署：[0.6.47 紀錄](docs/maintenance/dragon-combat-0.6.47.json)。伺服器回歸：`python tools/dragon-combat/test.py`；測試使用記憶體角色，不寫入玩家資料庫。


### 0.6.48 原版連擊節奏與狀態圖示

屠宰者保留三次物理命中與弱點計算，移除額外的施法動作封包；客戶端對 7020 特效啟用原版的連擊計數，將適用持武攻擊動作縮為原時長的三分之一。連擊結束恢復普通攻速，三重矢 4394 仍沿用原機制。

龍之護鎧狀態圖示改為共用 `Tile.pak/effectlist2.xml` 中 effect107 / `DRAGON_SKIN_S` 的 **3080**。3079 是技能欄圖示，0.6.47 曾誤用於狀態列，這版已修正。封包秒數與伺服器減傷規則維持原設定。

驗證：[0.6.48 紀錄](docs/maintenance/dragon-presentation-0.6.48.json)。伺服器測試：`python tools/dragon-presentation/test.py`。

### 0.6.49 強制近戰空擊

修正原生 `C_ATTACK` 的目標編號 0 被誤判為格式錯誤。Ctrl 點空地會朝該方向揮刀，使用 LiTo 原生轉向及零傷害攻擊封包，不搜尋座標上的其他單位，也不消耗物品或回存資料庫。保留死亡、觀戰、麻痺、隱身、負重及動作節奏限制；空擊同樣進入戰鬥退出倒數。此版處理近戰空擊，遠程攻擊仍走原有流程。

遊戲 DLL 沿用已驗收的 0.6.48。驗證：[0.6.49 紀錄](docs/maintenance/empty-melee-0.6.49.json)；測試：`python tools/empty-melee/test.py`。畫面實測待玩家確認。

### 0.6.50 幻術師狀態與原生呈現

補齊鏡像、幻覺：歐吉／巫妖／鑽石高崙／化身、專注、耐力、洞察、恐慌的原生狀態圖示。圖示沿用共用 Tile.pak 資源、原生提示與倒數，以伺服器現有 BUFF 租期同步；不新增戰鬥效果或 SQL 寫入。旗標陣列一起擴充，避免覆蓋相鄰原生資料。

0.6.49 的封包測試未涵蓋原生動畫，實測仍只轉向。此版改正零目標攻擊的基本動作與方向欄位，並驗證幻術師空手及奇古獸動作的動畫幀。自身名字僅在原生游標選中自身時繪製。

證據：[0.6.50 紀錄](docs/maintenance/illusion-presentation-0.6.50.json)。測試：`tools/illusion-presentation/test_icons.py`、`test_empty.py`，以及登入器 `tools/class_compat/verify_lobby.py --presentation`。玩家實測待確認。

### 0.6.51 幻術師指定目標

補上 14 個幻術師技能的原生指向游標及包含物件編號的施法封包。伺服器只處理實際選取且符合原有技能規則的單位；缺少、失效或超距目標不再自動改成自己、上一目標或附近單位。自身及立方塊技能維持原本流程，技能租期、成本與範圍驗證保留。

已驗證 ArenaBot 骷髏毀壞傷害、玩家指定 BUFF、召喚物指定技能、九種狀態圖示及原生目標封包；原生客戶端連續兩輪結算、確定返回大廳及再進戰鬥通過。本次未重現持續大廳卡住，未額外修改結算接線。證據：[0.6.51 紀錄](docs/maintenance/illusion-targets-0.6.51.json)。玩家實測待確認。

0.6.52 僅修正登入器 DLL 的原生角色面板位置顯示，伺服器 JAR 與 SQL 均維持 0.6.51。幻術師指向施法已由玩家實測確認。


### 0.6.54 跨來源大廳房間同步

建立、編輯、加入、離開戰鬥室後，向同頻道、仍停在大廳的其他玩家重送原生房間清單。來源資料庫不影響同頻道通知；不把清單送進其他頻道或房內畫面。沿用原有刪房及預設房間重設封包，某一接收者已斷線時繼續通知其他人，不中斷目前玩家離房。

已用四個記憶體測試玩家驗證跨來源同步、房間名稱、人數、刪除、頻道與畫面階段隔離；原版 JAR 可重現漏送通知。正式資料库及客戶端 DLL 不變，需重啟伺服器。伺服器測試入口：`tools/room-directory/test.py`，證據：伺服器 `docs/maintenance/room-directory-0.6.54.json`。原生雙玩家畫面待實測。

裝備穿脫：修正重新選取同一角色後來源映射遺失，穿戴狀態於評估與記憶體提交成功後發布。[修正及驗證](docs/maintenance/equipment-toggle-0.6.65.md)。

移動碰撞：補齊 S_BLINK 確認後的專用解鎖回覆，保留其他負面狀態。[修正及驗證](docs/maintenance/movement-lock-0.6.66.md)。

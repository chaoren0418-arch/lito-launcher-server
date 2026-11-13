# M4 實機驗收操作手冊（RUNBOOK）

更新：2026-11-13（T127，Issue #378）。A 部＝[`CHECKLIST.md`](CHECKLIST.md) 01–21 逐步展開；B 部＝[`customer-gaps.md`](../reports/customer-gaps.md) 的 G19–G27 與 G02 現場封包；C 部（M3 系列待驗證）由 T129（#380）接在本檔後面。

本檔只是步驟，**沒有任何一步已實機執行**。執行者是 Codex（T011 #285、T137 #388）；本機 Claude 執行器不啟動任何程式。

## 讀法

- `S/` = `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`；`L/` = `lito登入器/LiTo381-Launcher/`；`M/` = `lito伺服器/LiTo381-Server/docs/maintenance/`。
- 封包方向：`C→S` 客戶端送伺服器，`S→C` 伺服器送客戶端。opcode 名稱與數值取自 `S/Protocol.java`（常數定義在 `:88-408` C、`:412-744` 與 `:1530-1560` S）；總分派在 `S/ClientSession.java:1736-1902`。
- 行號是 2026-11-13 的 `main`（`bfe35e8f`）；程式改動後以方法名或字串為準。
- 每一步固定六欄：**輸入／預期畫面／預期封包或伺服器 log／SQL 前後查詢／截圖檔名／失敗時記什麼**。
- 標記：無標記＝讀過程式或文件、行號已確認；〔推測〕＝由程式推論、沒有執行；〔待驗證〕＝要實機才知道。
- 客戶端畫面上的按鈕名稱與位置沒有在本檔查證（沒有開遊戲），一律用功能描述；視窗是 640×480（`L/src/development_startup.py:222`）。

## 命名契約

與 [`CHECKLIST.md`](CHECKLIST.md#命名契約t128-的腳本t137t138-的執行都照這個) 相同，這裡只重列檔名：

| 項目 | 規則 |
|---|---|
| 版本目錄／結果檔 | `docs/acceptance/m4-<JAR SHA-256 前 8 碼>/`、`docs/acceptance/m4-<前 8 碼>.md` |
| 截圖 | A 部 `NN-<步驟>.png`；B 部 `G<編號>-<序>-<短名>.png`；C 部 `V<序>-<短名>.png` |
| 原始證據 | worktree 的 `scratch/m4/run-<UTC>/evidence/` |
| SQL 快照 | `evidence/sql-<before\|after>-<步驟>.txt` |
| 封包節錄 | `evidence/frames-<步驟>.jsonl`（從 `frames.jsonl` 依時間段剪出；不進 git） |

## 共用資訊

### 伺服器 console 的啟動行（依出現順序）

| 行 | 出處 |
|---|---|
| `[LiTo] resource-profile=…` | `S/SharedClientResources.java:103` |
| `[LiTo] shared-source=<db> controls=… source-read-only=true write-table=character_lito` | `S/SharedDatabase.java:86` |
| `LiTo381 Server <版本>` | `S/Main.java:84` |
| `[LiTo] login-listen=127.0.0.1:42083`、`[LiTo] game-listen=127.0.0.2:[42083] shared-channel-endpoint` | `S/Main.java:85-90` |
| `[LiTo] database=…`、`maps=`、`origin-source=`、`state=`、`rental-handoff=`、`battle-ai=`、`battle-control=`、`battle-animations=` | `S/Main.java:91-105` |
| `[LiTo] capture=<…/frames.jsonl>` | `S/Main.java:106` |
| `[LiTo] source-login id=<id> name=<名> port=<42083+id> auth=selected-source-only`（每個來源一行） | `S/SourceLogin.java:68` |
| **`[LiTo] ready`** | `S/Main.java:117` |

`[LiTo] Storm_Walk item cores loaded: […]`（`S/GmTravel.java:22`）也在啟動時印出，G22 用得到。

### 封包紀錄 `frames.jsonl`

| 項目 | 事實 | 出處 |
|---|---|---|
| 路徑 | `<capture.directory>/<yyyyMMdd-HHmmss（UTC）>-p<Java PID>-<序號>/frames.jsonl`；實驗設定下是 `scratch/m4/run-<UTC>/captures/…` | `S/CaptureLog.java:15-16,22-28`；`S/UnifiedConfiguration.java:81` |
| 開關 | 永遠開啟，沒有設定可關 | `S/ClientSession.java:1289` |
| 檔數 | 每個伺服器程序一個檔，所有連線共用；**每筆沒有 session 或帳號欄位** | `S/Main.java:47,116-130` |
| 每行 | `{"time":"<UTC ISO>","direction":"client_to_server"\|"server_to_client","opcode":"0xNN","wire_length":N,"plain":"<hex>","note":"…"}` | `S/CaptureLog.java:38-42` |
| `plain` | C→S 是解密後明文（**在拆掉 Ctrl 尾碼之前**寫入）；S→C 是加密前明文 | `S/ClientSession.java:1288-1290` |
| `note` | C→S 是 `C opcode 0xNN`；S→C 是程式內描述，常帶 `object=`／`target=` 與玩家名 | `S/ClientSession.java:57587-57593,36401,36404` |
| 遮罩 | `C_LOGIN` 等含密碼的封包整筆寫成 `MASKED_…`；聊天、角色名、object id 是明文 | `S/CaptureLog.java:47-80` |

分辨是哪個視窗送的：對 console 的 `[LiTo] connected endpoint=… peer=…`（`S/ClientSession.java:1271`）時間，並且一次只讓一個視窗操作。hex 比對不分大小寫。

### 玩家 object id

單一來源時，戰鬥內的玩家 object id 就是來源庫 `characters.objid`，也就是 `character_lito.char_obj_id`（`S/ClientSession.java:57315-57334`；`gameplay.md:330`）。封包內是 4 bytes little-endian：例如 objid `0x1000ABCD` 在 hex 裡是 `cdab0010`。

### SQL 查詢代號

| 代號 | 內容 |
|---|---|
| Q1 | CHECKLIST「SQL 前後對照」的三條 SELECT（來源路由、`character_lito` 12 欄、收據），整段輸出存成 `sql-<before\|after>-<步驟>.txt` |
| Q0 | 不需要（該步驟不改資料）；填「無」 |

所有查詢只讀 `p24_*_lito_exp`，用 #83 受限帳號連 `127.0.0.1:43324`。

---

## 前置（P 部）

| # | 項目 | 做法 | 通過條件 |
|---|---|---|---|
| P1 | lease | 向調度器取得 `p24lab` lease | 持有人是本任務、剩餘時間足夠一輪（建議 ≥ 60 分鐘） |
| P2 | 建 JAR | CHECKLIST「00 建 JAR 並記 SHA-256」 | 有 commit、JAR 與 `lib/` 的 SHA-256；前 8 碼定出版本目錄 |
| P3 | 執行目錄 | 建 `scratch/m4/run-<UTC>/`，複製 JAR 與 `lib/`，建空的 `config/ runtime/ captures/ logs/ tmp/ evidence/`（T128 的 `m4-prepare.ps1`） | 副本雜湊相同、`runtime/` 為空 |
| P4 | 實驗庫 | P24Lab 啟動、身分核對；`p24_380db_lito_exp` 存在；`character_lito` 有 7 個必要欄位；`lito_battle_settlement` 是 InnoDB（`S/SettlementSql.java:45-47`）；`lito_room_policy` 有 `policy_id=1`（`S/NativeRoomRules.java:21-24`） | 全部成立；不成立由 T128 的 `m4-db-prepare.sql` 處理 |
| P5 | 客戶端啟動方式 | 見下方 | 兩個視窗可連到實驗端點 |
| P6 | 資料前置 | 核對「資料前置」表 | 每列標「已備妥／缺」 |
| P7 | 基線 SQL | 跑 Q1，存 `sql-before-00.txt` | 有 A、B 兩列與現有收據數 |

### P5 客戶端啟動方式（阻塞條件）

**已驗證（讀程式）**：正式 0.6.71 登入器沒有「只開客戶端、連外部伺服器」的模式。

| 事實 | 出處 |
|---|---|
| 預設模式一定自己啟動 `L/app/server/LiTo381-SharedResources-candidate.jar`，路徑寫死並比對雜湊 | `L/src/FormalStandardSources.cs:12`；`L/src/development_startup.py:50-51,56-61` |
| 設定檔固定讀 `lito伺服器/LiTo381-Server/config/server.properties`（正式設定，M4 不得使用） | `L/src/SourceServerChoice.cs:9,18` |
| 給客戶端的登入位址來自環境變數 `LITO_NATIVE_LOGIN_HOST`、`LITO_NATIVE_SOURCES`（格式 `<id>:<埠>:<來源名 cp950 的 hex>`，多筆用 `;`） | `L/src/development_startup.py:98-105,235` |
| 客戶端是由 `L/app/lito_runtime_probe_named.py` 啟動 `0-客戶端\LiTo.bin`，參數見 `native_args` | `L/src/development_startup.py:75-84` |
| 第二個視窗要加三組記憶體內修補（獨立 mutex、兩個單例檢查） | `L/src/development_startup.py:118-131` |
| 正式流程每次會把 `app\compat\litoddraw.dll` 複製到 `0-客戶端` 並覆寫 `lito381-compat.ini` | `L/src/development_startup.py:221-223` |
| 直接用 probe 連非預設埠已有先例（測試夾具，埠 52000） | `L/tools/battle-flow/verify_pair.py:15-19,116-119` |

**建議做法〔推測，待驗證〕**：不開登入器 GUI，照 `verify_pair.py:15-19,116-119` 的方式直接跑 probe：

1. 在 `scratch/m4/run-<UTC>/client-1/` 寫 `server-profile.json`：`host=127.0.0.1`、`port=42083+source_id`、`nativeSourceIds=[source_id]`、`nativeSourcePortBase=42083`，其餘欄位照 `development_startup.py:94-96,230-233`。
2. 環境變數 `LITO_NATIVE_LOGIN_HOST=127.0.0.1`、`LITO_NATIVE_SOURCES=<source_id>:<42083+source_id>:<來源名 cp950 hex>`；移除 `LITO381_ACCOUNT`／`LITO381_PASSWORD`／`LITO381_CHARACTER`（`:236-237`）。
3. 以 `native_args('StandardArena', <client-1 目錄>)` 的參數啟動 probe，工作目錄 `D:\lineage開發\0-客戶端`；第二視窗用 `client-2/` 並加上 `:128-131` 的三組 `--conditional-patch`。
4. **不要**複製 DLL 或改寫 ini：先核對 `0-客戶端\litoddraw.dll` 與 `L/app/compat/litoddraw.dll` 雜湊相同、`lito381-compat.ini` 存在；不同就停下回報（規則：不修改 `0-客戶端`）。
5. 帳號密碼由操作者在原生登入頁手動輸入，不寫進任何檔案或環境變數。

**還要補齊的地方**：
- `native_args` 的路徑常數都寫死指向原始專案目錄：`DELIVERY`＝`L/app`、`PROBE`＝`L/app/lito_runtime_probe_named.py`、`CLIENT`＝`D:\lineage開發\0-客戶端`（`development_startup.py:8-17`）。只讀取它們沒有問題，但 `--output` 一定要指到 `scratch/m4/`。
- probe 載入 `litoddraw.dll` 前會讀環境變數 `LITO_NAMED_DDRAW_SHA256`（`L/app/named_ddraw.py:20`）；正式流程由 `world_resource_preflight.configure` 設定（`L/app/world_resource_preflight.py:73`；呼叫點 `development_startup.py:72-73`）。直接跑 probe 時要自己設成 `litoddraw.dll` 的 SHA-256（小寫 hex）。
- 這些步驟需要一支啟動腳本包起來，手動逐條輸入容易出錯。

**誰能解除**：
- T128（#379）在 `tools/acceptance/` 加一支 `m4-start-client.ps1`／`.py`（本張依任務範圍不寫腳本）；或
- 主控另開任務，替登入器加「外部伺服器」模式（要改 C# 與重建，M4 交付用的登入器就不是正式 0.6.71 成品）。

在這之前，A03 之後所有需要遊戲視窗的步驟都是「阻塞：P5」。A01、A02、A20（伺服器部分）、A21 不受影響。

### 資料前置（交 T128 的 `m4-seed-exp.sql`）

#83 只準備了兩組普通帳號、各一角色（初始勝敗 0、非 GM、沒有既有玩家資料）。下表每列是某些步驟需要、目前沒有或不確定的資料；全部只寫 `*_lito_exp`。

| # | 需要什麼 | 哪些步驟要用 | 判斷依據 | 現況 |
|---|---|---|---|---|
| D1 | A、B 兩角色等級 ≥ 15（否則沒有頻道可進） | A04 起全部 | `gameplay.md:165-166`（`S/TournamentRoomPolicy.java:9-18`） | 不確定：#83 沒寫等級 |
| D2 | 一個帳號有 5～8 個角色（每頁 4 個；伺服器一次最多送 8 個，超過 8 個會拋例外） | A05 | `S/Protocol.java:11686-11696`；`M/buff-schema-and-character-pages-20261018.zh-TW.md:39` | 缺 |
| D3 | GM 帳號：來源庫 `accounts.access_level` ≥ 指令門檻（預設 200）。改完要重新登入 | A12、A17、A18、G21、G22 | `S/LegacyMySqlRepository.java:709-715`；`S/GmPolyMe.java:28-30`；`config/server.properties.example:58-60` | 缺（#83 明寫沒有 GM） |
| D4 | `p24_lito_lito_exp.commands` 有 `polyme`（`L1PolyMe`）、`loc`（`L1Loc`）、`move`（`L1Move`）、`item`（`L1CreateItem`）、`de`（`L1SpawnDe`），`system=0` | 同 D3 | `db/lito_gm_polyme.sql:3-6`、`db/lito_gm_travel.sql:3-8`、`db/lito_commands.sql:14-31`；`S/GmPolyMe.java:23-27` | 不確定：#83 複製了 `commands` 表，內容沒查 |
| D5 | `p24_380db_lito_exp.etcitem` 有 `classname='wand.Storm_Walk'` 的道具（文件記載 item 42501） | A18、G22 | `S/GmTravel.java:16-22`；`M/gm-travel-0.6.68.md:14,19` | 不確定：看啟動行 `Storm_Walk item cores loaded` |
| D6 | 一個龍騎士（`characters.Type=5`）、一個幻術師（`Type=6`）角色，等級 ≥ 15，分屬兩個帳號（要同場對打） | G20 | `S/LegacyMySqlRepository.java:970,995`；`S/EquipmentRules.java:27` | 缺 |
| D7 | A 的角色已學技能 4（光箭）且有近戰武器；MP 足夠 | A10、A11、G23 | `S/ClientSession.java:28988`（`useSpell`） | 不確定 |
| D8 | A 的角色已學技能 26（通暢氣脈術），MP ≥ 50 | G02、G25-1 | `S/ClientSession.java:36306`；`S/BattleSpellCatalog.java:1251-1254` | 缺或不確定 |
| D9 | A、B 在**同一個非空血盟** | G02 對友方 | `S/ClientSession.java:36301`（`sameNonemptyClan`） | 缺 |
| D10 | 一個角色已學一個負面技能（建議 29 緩速術） | G23 | `docs/design/skills-wizard.md:66-121` | 不確定 |
| D11 | 龍騎士角色已學技能 186（血之渴望） | G25-4 | `S/BattleSpellCatalog.java:218` | 缺 |
| D12 | 一個角色背包有魔法娃娃道具（`character_items` 內、且該道具在靜態庫 `etcitem_doll_type` 有資料） | G25-2 | `S/LegacyMySqlRepository.java:8992` | 缺 |
| D13 | 第 3、第 4 個帳號（2 對 2 才有「真正的隊友」） | G23 完整版、G02 對友方的 PvP 版 | `gameplay.md:56`（隊伍模式兩隊都要有真人）；自建房強制雙方人數一致（`S/ClientSession.java:10373`） | 缺；沒有時用「2 帳號替代版」 |
| D14 | `p24_lito_lito_exp.lito_tournament_shop` 有藍色藥水 40015、變形卷軸 40088（統一設定下商店一律開啟並改讀資料表） | G25-3、A12 的替代 | `S/UnifiedConfiguration.java:75`；`S/TournamentShopCatalog.java:39-54` | 不確定 |

D3 之外不需要動權限；D3 是把**實驗庫**的測試帳號設成 GM，不是提升 DB 帳號權限。

---

## A 部：CHECKLIST 01–21

<a id="a01"></a>
### A01 啟動伺服器

| 欄 | 內容 |
|---|---|
| 輸入 | 在 `scratch/m4/run-<UTC>/` 執行 CHECKLIST 步驟 01 的命令（`java … -jar LiTo381-Server.jar config/experiment.properties`） |
| 預期畫面 | console 依「共用資訊」的順序印出各行，最後是 `[LiTo] ready`；沒有 `Exception` |
| 預期封包或伺服器 log | `[LiTo] login-listen=127.0.0.1:42083`（`S/Main.java:85`）、`[LiTo] source-login id=… port=…`（`S/SourceLogin.java:68`）、`[LiTo] capture=…frames.jsonl`（`S/Main.java:106`）、`[LiTo] ready`（`S/Main.java:117`）。此時還沒有封包 |
| SQL 前後查詢 | 啟動前 Q1 → `sql-before-01.txt`；啟動後 Q1 → `sql-after-01.txt`，兩者應相同（啟動只重放既有 WAL，本輪 `runtime/` 是空的） |
| 截圖檔名 | `01-server-ready.png`（console 含 `ready` 行） |
| 失敗時記什麼 | 退出碼、完整 console、最後一個 `Caused by`。常見：`lacks ranking columns`（`S/SettlementSql.java:46`，缺 P4 的欄位）、`SELECT command denied`（授權）、`Unknown column`（靜態庫結構）、`Unknown/retired setting`／`Missing setting`（設定鍵，`S/UnifiedConfiguration.java:24-25`）、`settlement journal already owned`（`S/SettlementJournal.java:13-14`，同一 `runtime/` 有另一個程序） |

<a id="a02"></a>
### A02 監聽位址與來源路由

| 欄 | 內容 |
|---|---|
| 輸入 | 另開一個主控台：`Get-NetTCPConnection -State Listen -OwningProcess <Java PID>`；Q1 的第一條（來源路由） |
| 預期畫面 | 四個監聽：`127.0.0.1:42083`、`127.0.0.2:42083`、`127.0.0.1:(42083+source_id)`、`127.0.0.2:(42083+source_id)`；沒有 `0.0.0.0`、沒有其他埠 |
| 預期封包或伺服器 log | `login-listen`／`game-listen`（`S/Main.java:71-90`）；每個來源在 `bind` 與 `game.bind` 各開一個同埠監聽（`S/SourceLogin.java:42-50`） |
| SQL 前後查詢 | 來源路由只有一列啟用，`character_database=p24_accounts_lito_exp`；記下 `source_id` |
| 截圖檔名 | `02-listen.png` |
| 失敗時記什麼 | 實際監聽清單、PID、多出或缺少的位址／埠。注意：T128 內文寫「只有 `127.0.0.1:42083` 與 `127.0.0.2:(42083+source_id)`」，與程式不符，以本步驟的四個為準 |

<a id="a03"></a>
### A03 啟動第一個遊戲視窗

| 欄 | 內容 |
|---|---|
| 輸入 | 依 P5 啟動 `client-1`；在原生伺服器選單停下，不要按任何鍵 |
| 預期畫面 | 「Lineage Windows Client」視窗（640×480）；伺服器選單列出 `lito_source_server` 的來源名稱 |
| 預期封包或伺服器 log | console：`[LiTo] connected endpoint=LOGIN peer=…`（`S/ClientSession.java:1271`）。S→C `S_KEY 0x01`（`S/ClientSession.java:1280-1281`）；C→S `C_VERSION 0x00`（處理 `version`，`:6979`）；S→C `S_VERSION_CHECK 0x3A`（`S/Protocol.java:5054`） |
| SQL 前後查詢 | 無 |
| 截圖檔名 | `03-server-select.png` |
| 失敗時記什麼 | probe 的 stdout／stderr、視窗是否出現、console 有沒有 `connected` 行；連到的位址若不是 `127.0.0.1:(42083+source_id)` 立即關閉並記錄 |

<a id="a04"></a>
### A04 帳號 A 登入、進大廳

| 欄 | 內容 |
|---|---|
| 輸入 | 選來源 → 輸入帳號 A 的帳密（手動）→ 新聞頁按確認 → 選角色 → 選頻道（依角色等級，`gameplay.md:165`）進大廳 |
| 預期畫面 | 角色清單顯示 A 的角色、等級與職業圖示；進入大廳後看得到房間列表 |
| 預期封包或伺服器 log | C→S `C_LOGIN 0x01`（`login`，`S/ClientSession.java:7002`；capture 內是 `MASKED_C_LOGIN`）→ S→C `S_LOGIN_CHECK 0x00` 結果 `ACCEPTED 0x00`（`S/Protocol.java:1582,5112`）；console `[LiTo] authenticated account=… characters=…`（`:7164`）。選來源 `C_SERVER_SELECT 0x35` → `S_LITO_VS_SERVER_IP 0xF4`（`:7521-7551`），客戶端改連 GAME 端。新聞 `S_LITO_NEWS 0xE6` ↔ `C_LITO_READ_NEWS 0xE2`（`:7351,15313`）。角色清單 `S_NUM_CHARACTER 0x03`＋每角色一筆 `S_LITO_CHARACTER_INFO 0xCB`（`:7555-7617`）。選角 `C_LITO_SELECT_CHAR 0xC9` → `S_LITO_SELECT_CHAR_RESULT 0xC8`（`:93971`）。進頻道 `C_LITO_ENTER_CHANNEL 0xCA` → `S_LITO_ENTER_CHANNEL_RESULT 0xC9`、`S_LITO_CHANNEL_INFO 0xCA`（`:8749,8884-8886,9008-9013`）。房間列表 `C_LITO_ROOM_LIST 0xCE` → `S_LITO_ROOM_INFO 0xCC`，console `[LiTo] sent room list entries=…`（`:9043-9057`） |
| SQL 前後查詢 | 無（登入不寫資料；`character_lito` 應與 `sql-after-01.txt` 相同，可抽查） |
| 截圖檔名 | `04-char-list.png`、`04-lobby.png`（不得拍到帳密輸入中的畫面） |
| 失敗時記什麼 | 停在哪一頁、畫面訊息、`S_LOGIN_CHECK` 的結果碼（`0x05` 系統錯誤、`0x08` 帳密、`0x1A` 封鎖、`0x16` 使用中，`S/Protocol.java:1582-1590`）；沒有頻道可選時查 D1 |

<a id="a05"></a>
### A05 八角色翻頁

| 欄 | 內容 |
|---|---|
| 輸入 | 用 D2 的帳號登入到角色清單；按下一頁、上一頁各一次；選第二頁的一個角色進大廳後再返回角色清單 |
| 預期畫面 | 第一頁 4 個、第二頁其餘角色；來回翻頁內容正確；第二頁角色可進大廳 |
| 預期封包或伺服器 log | 伺服器一次送完：`S_NUM_CHARACTER 0x03`＋最多 8 筆 `S_LITO_CHARACTER_INFO 0xCB`（`S/Protocol.java:11686-11710`）；翻頁沒有對應的 opcode（未找到：查過 `S/Protocol.java` 的 C 常數 `:88-408` 與 `S/ClientSession.java:7555-7631`），是相容層 DLL 在客戶端做的（`M/buff-schema-and-character-pages-20261018.zh-TW.md:35-39`） |
| SQL 前後查詢 | 無 |
| 截圖檔名 | `05-page1.png`、`05-page2.png` |
| 失敗時記什麼 | 角色數、每頁顯示數、翻頁後是否崩潰（記 probe 退出碼；`0x0019FFCC` 單獨不代表崩潰，同文件 `:41`）。沒有 D2 → 「阻塞：D2」，不臨時加角色 |

<a id="a06"></a>
### A06 相容層載入

| 欄 | 內容 |
|---|---|
| 輸入 | 唯讀：`Get-Process -Id <LiTo.bin PID> -Module`（或 probe 輸出的模組清單）找 `litoddraw.dll`；列 `0-客戶端` 目錄 |
| 預期畫面 | 模組清單有 `litoddraw.dll`，路徑在 `D:\lineage開發\0-客戶端`；清單與目錄都沒有該目錄下的 `ddraw.dll`（系統目錄的 `ddraw.dll` 不算） |
| 預期封包或伺服器 log | 無封包。`0-客戶端\lito381-compat.log` 在 `lito381-compat.ini` 的 `[Diagnostics] Logging=1` 時有新行（`L/native/ddraw_proxy.cpp:171-189,221-233`） |
| SQL 前後查詢 | 無 |
| 截圖檔名 | `06-compat-module.png` |
| 失敗時記什麼 | 實際載入的 DLL 路徑與雜湊；只看到檔案存在但沒有載入證據時填「失敗：無載入證據」 |

<a id="a07"></a>
### A07 帳號 B 第二視窗

| 欄 | 內容 |
|---|---|
| 輸入 | 依 P5 啟動 `client-2`（加三組修補）；帳號 B 登入到同一頻道的大廳 |
| 預期畫面 | 兩個視窗都在大廳，各自顯示自己的角色 |
| 預期封包或伺服器 log | 與 A04 相同的一組，console 多一行 `authenticated account=…` |
| SQL 前後查詢 | 無 |
| 截圖檔名 | `07-two-lobby.png`（兩視窗並排） |
| 失敗時記什麼 | 第二視窗是否被單例檢查擋下（視窗沒出現、probe 立即結束）、訊息、probe stderr |

<a id="a08"></a>
### A08 建房與進房

| 欄 | 內容 |
|---|---|
| 輸入 | A 在大廳建立房間（名稱自訂、無密碼、容量 2 以上）；B 在房間列表選該房進入。**不要**用 Local Lobby |
| 預期畫面 | 兩個視窗都在同一房間；模式是死鬥；A、B 自動分到紅、藍兩隊（`gameplay.md:191`）；A 是房主 |
| 預期封包或伺服器 log | C→S `C_LITO_CREATE_ROOM 0xCC`（`createRoom`，`S/ClientSession.java:10348`；固定死鬥並加上「雙方人數一致」，`:10371-10375`），console `[LiTo] C_LITO_CREATE_ROOM name=…`（`:10377`）。B：`C_LITO_JOIN 0xCF`（`joinRoom`，`:9125`）→ `S_LITO_JOIN_RESULT 0xCE`（`:9297`）、`S_LITO_ROOM_USER 0xCD`（`:10470`，內含職業 `characterType`，`S/Protocol.java:2518`） |
| SQL 前後查詢 | 無 |
| 截圖檔名 | `08-room.png` |
| 失敗時記什麼 | 進房被拒的訊息（頻道、密碼、人數，`gameplay.md:185-191`）；若看到「Local Lobby 為測試房，不計勝敗戰績」（`S/NativeRoomRules.java:117-118`）表示進錯房 |

<a id="a09"></a>
### A09 開戰

| 欄 | 內容 |
|---|---|
| 輸入 | 先存 Q1 → `sql-before-15.txt`。A、B 各按準備 |
| 預期畫面 | 倒數 3、2、1 後兩個視窗進入同一張戰鬥地圖，雙方可移動 |
| 預期封包或伺服器 log | C→S `C_LITO_READY_GAME 0xD2`（`setReady`，`S/ClientSession.java:10514`）→ `S_LITO_READY_GAME 0xD1`（`:10558`）；倒數 `S_LITO_COUNTDOWN 0xE1`（`:10582`；從 3 開始，`S/GameHub.java:9554-9556`）；開戰 `S_WORLD 0x28`、`S_LITO_START_GAME 0xD2`、`S_LITO_OBSERVER_BOX 0xE2`、`S_LITO_TIMELIMIT 0xE9`（`startBattle`，`:10651-10687`）。死鬥時限 180 秒（`gameplay.md:48`） |
| SQL 前後查詢 | 開戰前 Q1（已存成 `sql-before-15.txt`） |
| 截圖檔名 | `09-battle.png` |
| 失敗時記什麼 | 不能開始時的系統訊息（`S/NativeRoomRules.java:70-100`）；進圖後黑畫面或崩潰時記地圖編號（console 的 `C_LITO_CREATE_ROOM` 行或 `S_WORLD` 封包）與 probe 輸出，並對照 G27 |

<a id="a10"></a>
### A10 近戰攻擊

| 欄 | 內容 |
|---|---|
| 輸入 | A 走到 B 旁邊，對 B 按左鍵攻擊一次（死鬥中 B 是敵方，不必按 Ctrl） |
| 預期畫面 | A 播放攻擊動作；命中時 B 的 HP 條下降，兩視窗一致 |
| 預期封包或伺服器 log | C→S `C_ATTACK 0x17`（9 bytes，`plain[1..4]`＝B 的 object id；`S/ForcedCombat.java:23-25`；處理 `attack`，`S/ClientSession.java:47553`）。S→C `S_ATTACK 0x23`（`:86553-86561`）。HP：舊路徑是自己收 `S_HIT_POINT 0x0D`、其他人收 `S_HIT_RATIO 0x68`（`:74732-74733`）；正常路徑經 `NativePlayerCommit.enqueueBattle`（`:51271`），實際封包建構點未找到（查過 `S/ClientSession.java:51247-51271,74712-74733`），以 capture 實際內容為準 |
| SQL 前後查詢 | 無 |
| 截圖檔名 | `10-melee.png` |
| 失敗時記什麼 | 有沒有 `C_ATTACK` 幀、目標 id 是否等於 B、有沒有 `S_ATTACK`；「送了但沒有傷害」與「根本沒送」要分開記 |

<a id="a11"></a>
### A11 魔法技能

| 欄 | 內容 |
|---|---|
| 輸入 | A 選技能 4（光箭）後點 B |
| 預期畫面 | 光箭特效飛向 B；命中時 B 的 HP 下降；A 的 MP 減少 |
| 預期封包或伺服器 log | C→S `C_USE_SPELL 0x14`（`useSpell`，`S/ClientSession.java:28988`）；技能 id＝`plain[1]×8＋plain[2]＋1`（`S/ForcedCombat.java:27`），技能 4 的開頭是 `140003`，目標 id 在 `plain[3..6]`。S→C 攻擊魔法用 `S_ATTACK 0x23`（`attackSpell`，`S/Protocol.java:9626`）；`S_MANA_POINT 0x4D` 給施法者 |
| SQL 前後查詢 | 無 |
| 截圖檔名 | `11-magic.png` |
| 失敗時記什麼 | 技能 id、目標 id、MP 前後；console 的 `rejected` 行；角色沒學該技能 → 「阻塞：D7」 |

<a id="a12"></a>
### A12 變身 `.polyme`

| 欄 | 內容 |
|---|---|
| 輸入 | GM 帳號（D3、D4）在戰鬥中於聊天輸入 `.polyme 6080`（外型代號 1～65535；6080 是文件範例） |
| 預期畫面 | 角色外型改變，兩個視窗都看得到；系統訊息「變身外型 6080，持續 7200 秒。」 |
| 預期封包或伺服器 log | C→S `C_SAY 0x13` 或 `C_CHAT 0x77`（`say`，`S/ClientSession.java:14354`；GM 指令 `handleGmChatCommand`，`:14399` → `S/GmTravel.java:30` → `S/GmPolyMe.java`）。S→C `S_POLYMORPH 0x27`（`broadcastPolymorphAppearance`，`:24422-24442`，房間全員）、`S_MESSAGE 0x0F`（`S/GmPolyMe.java:59`）。失敗時 stderr `[LiTo] GM polyme failed: …`（`S/GmPolyMe.java:66-67`） |
| SQL 前後查詢 | 無（不寫回角色庫，`M/gm-polyme-0.6.71.md:3,9`） |
| 截圖檔名 | `12-polyme.png` |
| 失敗時記什麼 | 回覆訊息原文：「資料庫未啟用 .polyme…」＝D4；沒有任何回覆＝帳號不是 GM（D3）；「請先進入遊戲世界再使用 .polyme。」＝不在戰鬥中（`S/GmPolyMe.java:83-86`）。沒有 GM 時填「阻塞：D3」；可另外用商店的變形卷軸 40088（`C_USE_ITEM 0x1C` → `usePolymorphScroll`，`S/ClientSession.java:24198`）做一次並存成 `12-polyme-scroll.png` 當補充證據，但本步驟仍是阻塞 |

<a id="a13"></a>
### A13 分出勝負

| 欄 | 內容 |
|---|---|
| 輸入 | A 持續攻擊 B 到 B 陣亡（或反過來；記下誰贏）。必須在 180 秒內，否則逾時算平手、不記戰績（`gameplay.md:61,86`） |
| 預期畫面 | 敗方角色倒下；雙方出現勝負訊息後進入結算畫面 |
| 預期封包或伺服器 log | 死亡：`S_ACTION 0x20` 動作 8、敗方收 `S_LITO_CONFIRM_DEATH 0xE3`（`S/ClientSession.java:74759-74763`，舊路徑所見）；敗方 C→S `C_LITO_CONFIRM_DEATH 0xE0`（`confirmDeath`，`:24728`），console `[LiTo] battle resolution room=… ended=true … reason=…`（`:24737`）。結束：`S_LITO_END_GAME 0xD3`（`:75212`）＋每位參戰者一筆 `S_LITO_POINT 0xDB`（`:75219,13140-13142`）。console 兩行 `[LiTo] battle result player=… won=true／false ranking-points=…`（`:56906-56915`） |
| SQL 前後查詢 | 見 A15 |
| 截圖檔名 | `13-battle-end.png` |
| 失敗時記什麼 | 結束原因（`reason=`）、是否逾時、console 有沒有 `battle result` 兩行、有沒有 `state save failed`（`:56917`）或 stderr `settlement recovery pending`（`S/DurableBattleSettlement.java:164`） |

<a id="a14"></a>
### A14 結算畫面

| 欄 | 內容 |
|---|---|
| 輸入 | 停在結算畫面截圖；之後雙方按確認離開，回到房間或大廳 |
| 預期畫面 | 勝方隊伍正確；名單列出 A、B 的名稱（格式「帳號(來源名)」，`gameplay.md:326`）、擊殺／死亡數、各自的職業圖示 |
| 預期封包或伺服器 log | `S_LITO_END_GAME 0xD3` 帶勝方隊伍（−1／0／1，`S/Protocol.java:2258-2262`）；`S_LITO_POINT 0xDB` 欄位是 `名稱、points、kills、deaths、assists、force、force`，**沒有職業欄位**（`S/ClientSession.java:13140-13142`）；職業圖示來自進房時的 `S_LITO_ROOM_USER 0xCD`（`S/Protocol.java:2518`）。離開：`C_LITO_QUIT_FIGHT 0xD9` → `S_LITO_QUIT_FIGHT 0xF0`（`:13608`）；`C_LITO_EXIT_ROOM 0xD0` → `S_LITO_EXIT_ROOM 0xD0`（`:13187-13226`） |
| SQL 前後查詢 | 無 |
| 截圖檔名 | `14-result.png` |
| 失敗時記什麼 | 勝負顯示與實際不符、名稱或圖示錯位、卡在結算畫面回不去（記最後一個 C→S／S→C） |

<a id="a15"></a>
### A15 戰績前後對照

| 欄 | 內容 |
|---|---|
| 輸入 | Q1 → `sql-after-15.txt`；與 `sql-before-15.txt` 逐欄比對 |
| 預期畫面 | — |
| 預期封包或伺服器 log | console 的兩行 `battle result` 數值與 SELECT 一致。寫入順序是 WAL → 收據 → `character_lito` 單一 UPDATE → `applied=1` → 刪 WAL（`gameplay.md:281`；`S/SettlementSql.java:63,97,101`） |
| SQL 前後查詢 | 勝者：`wins+1`、`ranking_points+250`、`monthly_wins`／`monthly_ranking_points` 依月基準增加；敗者：`losses+1`、`ranking_points+50`；雙方 `version+1`、`last_result_id`＝新收據的 `result_id`；收據新增兩列，同一個 `battle_id`，`applied=1`。完整表在 CHECKLIST「戰績預期值」 |
| 截圖檔名 | `15-sql.png`（前後對照；角色名以 A／B 遮蔽） |
| 失敗時記什麼 | 哪一欄不符、前後值、收據列（含 `applied`、`applied_at`）、`runtime/battle-settlements/` 清單（有沒有 `.pending`／`.tmp`） |

<a id="a16"></a>
### A16 戰鬥中關閉 B 的視窗

| 欄 | 內容 |
|---|---|
| 輸入 | Q1 → `sql-before-16.txt`。A、B 回同一個自建房、再開一場；進圖後約 10 秒，按 B 視窗右上角的關閉 |
| 預期畫面 | A 的畫面顯示勝利並進入結算；B 視窗消失 |
| 預期封包或伺服器 log | 關視窗若送出 `C_QUIT 0x0F`：console `[LiTo] C_QUIT graceful disconnect phase=BATTLE player=…`（`S/ClientSession.java:13642`）；若直接斷線且有例外：`[LiTo] session ended endpoint=… peer=…: …`（`:1305`）。兩者之後都有 `[LiTo] battle disconnect pending room=… player=… graceMs=300`（`:47547-47549`）與 `[LiTo] battle disconnect resolved room=… reason=…`（`S/GameHub.java:12974`）。A 收 `S_LITO_END_GAME 0xD3`、`S_LITO_POINT 0xDB`。`battle result` 兩行：A `won=true`，B `won=false … disconnected=true` |
| SQL 前後查詢 | Q1 → `sql-after-16.txt`。A：勝列；B：斷線敗列（`losses+1`、`disconnects+1`、`ranking_points=max(0,前−50)`）；收據 B 列 `disconnected=1`；兩列 `applied=1` |
| 截圖檔名 | `16-a-wins.png` |
| 失敗時記什麼 | console 實際出現的是哪一種斷線行、有沒有 `pending`／`resolved` 行、A 是否卡住、SQL 哪一欄不符、WAL 目錄清單。若 B 被記成一般敗（`disconnected=0`）要特別標出 |

<a id="a17"></a>
### A17 GM 指令

| 欄 | 內容 |
|---|---|
| 輸入 | GM 帳號在戰鬥中輸入 `.loc`，再輸入 `.polyme <另一個外型代號>` |
| 預期畫面 | `.loc` 回「座標 X=… Y=… 地圖=… 方向=…」；變身生效，訊息寫持續 7200 秒 |
| 預期封包或伺服器 log | 同 A12；`.loc` 只回 `S_MESSAGE 0x0F`（`S/GmTravel.java:42-45`），沒有 console 行 |
| SQL 前後查詢 | 無 |
| 截圖檔名 | `17-gm-loc.png`、`17-gm-polyme.png` |
| 失敗時記什麼 | 同 A12。`.loc`／`.move` 只能在戰鬥房間內用（`S/GmTravel.java:59`） |

<a id="a18"></a>
### A18 GM 魔杖

與 [G22](#g22) 同一組操作；這裡做一次即可，G22 補做拒絕情況。

| 欄 | 內容 |
|---|---|
| 輸入 | GM 帳號在大廳或房間輸入 `.item 42501` 取得魔杖；進戰鬥後雙擊魔杖，游標變成選取狀態後點一格空地 |
| 預期畫面 | 出現傳送特效，角色出現在點選的位置；另一視窗看到同樣的位置變化 |
| 預期封包或伺服器 log | C→S `C_USE_ITEM 0x1C`（道具 object id、目標 id、X、Y；`useInventoryItem`，`S/ClientSession.java:21428` → `S/GmTravel.java:62`）。S→C `S_PARALYSE 0x25`（解鎖游標）、`S_EFFECT 0x37`（gfx 169）、`S_PORTAL 0x5A`（`S/GmTravel.java:83-85`）；客戶端回 `C_GOTO_PORTAL 0x08`（`completePortal`，`S/ClientSession.java:26207`）。console `[LiTo] wand.Storm_Walk player=… map=… x=… y=… awaiting-C_GOTO_PORTAL`（`S/GmTravel.java:86`）；`.item` 成功時 `[LiTo] GM item character=…`（`S/LiToGmCommands.java:94`） |
| SQL 前後查詢 | 無（不消耗道具、不寫回，`M/gm-travel-0.6.68.md:27`） |
| 截圖檔名 | `18-wand-before.png`、`18-wand-after.png` |
| 失敗時記什麼 | 點選座標與 console 的 `x= y=` 是否一致、錯誤訊息原文、游標有沒有卡在選取狀態 |

<a id="a19"></a>
### A19 Ctrl 強制攻擊

做 [G23](#g23) 的「2 帳號替代版」第 1、2 列即可；截圖 `19-ctrl-attack.png`。六欄內容見 G23。

<a id="a20"></a>
### A20 正常退出

| 欄 | 內容 |
|---|---|
| 輸入 | 兩個視窗各自回到大廳後用遊戲內的離開功能結束；確認兩個 `LiTo.bin` 程序都結束後，在伺服器主控台按一次 Ctrl+C |
| 預期畫面 | 視窗正常關閉；Java 程序結束、回到提示字元 |
| 預期封包或伺服器 log | C→S `C_QUIT 0x0F`，console `[LiTo] C_QUIT graceful disconnect phase=… player=…`（`S/ClientSession.java:13622-13642`）；離開頻道時 `[LiTo] left game lobby player=…`（`:9040`）。Ctrl+C 走 shutdown hook `LiTo-native-shutdown`（`S/NativeServerLifetime.java:68-76`），**正常關閉不印任何行**；只有失敗才有 stderr `[LiTo] application shutdown fenced: …` |
| SQL 前後查詢 | Q1 → `sql-after-20.txt`，應與最後一場之後相同；沒有 `applied=0` 的收據 |
| 截圖檔名 | `20-exit.png` |
| 失敗時記什麼 | Java 退出碼、有沒有 `hs_err_pid*.log`、`Get-NetTCPConnection` 是否還有四個實驗監聽、`runtime/battle-settlements/` 清單（`owner.lock` 可留，`.pending` 不可留）、`owner.lock` 能否以獨占方式開啟 |

<a id="a21"></a>
### A21 關閉 P24Lab、釋放 lease

| 欄 | 內容 |
|---|---|
| 輸入 | 依 #83／#284 的既有流程核對身分與連線數後正常 SHUTDOWN（只關自己啟動的實例），向調度器釋放 lease |
| 預期畫面 | 43324、42083、`42083+source_id` 都沒有監聽 |
| 預期封包或伺服器 log | 無 |
| SQL 前後查詢 | SHUTDOWN 前最後一次 Q1 → `sql-after-21.txt` |
| 截圖檔名 | `21-p24lab-closed.png` |
| 失敗時記什麼 | 還有誰連著、哪個 PID 沒結束；不強殺他人程序 |

---

## B 部：G19–G27 與 G02

建議順序（同一個 lease 內接在 A 部後面）：G26（只要兩個視窗）→ G20（換角色）→ G21、G22（GM）→ G23 → G02 → G25 → G24（最後做，因為會斷線）。

<a id="g19"></a>
### G19 完整流程逐步截圖

G19＝A 部 01–21 全程。A 部每一步的截圖與 A15、A16 的 SQL 對照齊全即通過；不另外重跑。結果檔在 G19 列填「見 A 部」與 A 部的通過／失敗／阻塞統計。

<a id="g20"></a>
### G20 結算畫面的龍騎士／幻術師圖示

需要：D6（龍騎士 `Type=5`、幻術師 `Type=6` 各一，分屬兩個帳號）。不需要 GM。

| 序 | 欄 | 內容 |
|---|---|---|
| 1 | 輸入 | 兩個視窗分別用龍騎士、幻術師進同一個自建房 |
|   | 預期畫面 | 房間名單兩人的職業圖示是龍騎士、幻術師（0.6.70 使用者已確認大廳與戰鬥室，`L/docs/maintenance/world-resource-compat-current.zh-TW.md:5`） |
|   | 預期封包或伺服器 log | `S_LITO_ROOM_USER 0xCD` 的 `characterType` 是 5、6（`S/Protocol.java:2518`；值取自 `characters.Type`，`S/LegacyMySqlRepository.java:970,995`）。若 console 出現職業 fallback 的行（`S/ClientSession.java:10413-10419`）表示被改送 0 |
|   | SQL 前後查詢 | Q1 → `sql-before-G20-1.txt`（角色名換成這兩個角色） |
|   | 截圖檔名 | `G20-1-room.png` |
|   | 失敗時記什麼 | 顯示成哪個職業的圖示、`S_LITO_ROOM_USER` 的 `plain` |
| 2 | 輸入 | 開戰並打完一場（同 A09–A13） |
|   | 預期畫面 | 結算名單上兩人的職業圖示仍是龍騎士、幻術師，顏色與隊伍相符 |
|   | 預期封包或伺服器 log | `S_LITO_END_GAME 0xD3`、`S_LITO_POINT 0xDB`（沒有職業欄位，圖示由客戶端用房間名單的資料畫；DLL 補了結算名單的繪製，同文件 `:17`） |
|   | SQL 前後查詢 | Q1 → `sql-after-G20-2.txt`（勝 +250／敗 +50，同 A15） |
|   | 截圖檔名 | `G20-2-result.png` |
|   | 失敗時記什麼 | 圖示空白、變成舊五職業之一、位置錯位；附房間與結算兩張圖對照 |

阻塞條件：沒有 D6 →「阻塞：D6」，由 T128 的 seed 解除。

<a id="g21"></a>
### G21 `.polyme`

需要：D3、D4。在已開戰的房間內做（可用 GM 帳號單人進 Local Lobby 測試房對 ArenaBot，不影響戰績）。

| 序 | 欄 | 內容 |
|---|---|---|
| 1 | 輸入 | 戰鬥中輸入 `.polyme 6080` |
|   | 預期畫面 | 外型改變；訊息「變身外型 6080，持續 7200 秒。」 |
|   | 預期封包或伺服器 log | 同 [A12](#a12)：`C_SAY 0x13`／`C_CHAT 0x77` → `S_POLYMORPH 0x27`、`S_MESSAGE 0x0F` |
|   | SQL 前後查詢 | 無 |
|   | 截圖檔名 | `G21-1-polyme.png` |
|   | 失敗時記什麼 | 回覆訊息原文、console／stderr |
| 2 | 輸入 | 再輸入一次 `.polyme <另一個代號>`；再輸入格式錯誤的 `.polyme` |
|   | 預期畫面 | 第一個：換成新外型、時間重設；第二個：「用法：.polyme 外型代號（1～65535），例如 .polyme 6080。」 |
|   | 預期封包或伺服器 log | `S/GmPolyMe.java:19-20,55,72-77` |
|   | SQL 前後查詢 | 無 |
|   | 截圖檔名 | `G21-2-repolyme.png` |
|   | 失敗時記什麼 | 同上 |
| 3 | 輸入 | 變身狀態下拿武器攻擊一次；打完回結算、回大廳 |
|   | 預期畫面 | 新外型不支援目前武器時武器被卸下（`S/ClientSession.java:24447-24459`）；回結算或大廳後恢復原外型（`S/GmPolyMe.java:91-101`） |
|   | 預期封包或伺服器 log | `S_POLYMORPH 0x27` 再送一次（恢復） |
|   | SQL 前後查詢 | 無（不寫回） |
|   | 截圖檔名 | `G21-3-restore.png` |
|   | 失敗時記什麼 | 回大廳後外型沒恢復、攻擊動作異常 |
| 4 | 輸入 | 普通帳號（B）輸入 `.polyme 6080` |
|   | 預期畫面 | 不變身 |
|   | 預期封包或伺服器 log | 沒有 `S_POLYMORPH`；回覆內容未找到固定字串（查過 `S/GmPolyMe.java:23-30`），以實際畫面為準 |
|   | SQL 前後查詢 | 無 |
|   | 截圖檔名 | `G21-4-nongm.png` |
|   | 失敗時記什麼 | 普通帳號竟然變身 → 失敗（權限問題），附帳號的 `access_level` |

已知的設計限制（不算失敗）：GM 變身只換外型與裝備限制，不加 Source 外掛的變身能力（`M/gm-polyme-0.6.71.md:13`）。

<a id="g22"></a>
### G22 GM 魔杖點地傳送

需要：D3、D4（`.item`）、D5。只能在已開戰的房間內（`S/GmTravel.java:59`）。

| 序 | 欄 | 內容 |
|---|---|---|
| 1 | 輸入 | 同 [A18](#a18)：取得魔杖、戰鬥中使用、點空地 |
|   | 預期畫面／封包／SQL | 同 A18 |
|   | 截圖檔名 | `G22-1-before.png`、`G22-1-after.png` |
|   | 失敗時記什麼 | 同 A18 |
| 2 | 輸入 | 再用一次，點在另一個玩家或假人站的格子上 |
|   | 預期畫面 | 不傳送，出現錯誤訊息，游標恢復正常 |
|   | 預期封包或伺服器 log | 只有 `S_MESSAGE 0x0F` 與 `S_PARALYSE 0x25`（`S/GmTravel.java:70,88-103`）；沒有 `S_PORTAL` |
|   | SQL 前後查詢 | 無 |
|   | 截圖檔名 | `G22-2-occupied.png` |
|   | 失敗時記什麼 | 竟然傳送成功（兩人重疊）、或游標卡住 |
| 3 | 輸入 | 點在牆或不可走的格子上 |
|   | 預期畫面 | 同 2 |
|   | 預期封包或伺服器 log | 同 2（`S/GmTravel.java:74-79`） |
|   | SQL 前後查詢 | 無 |
|   | 截圖檔名 | `G22-3-blocked.png` |
|   | 失敗時記什麼 | 同 2 |
| 4 | 輸入 | 輸入 `.move <X> <Y>`（X、Y 取 `.loc` 回報值附近的可走格） |
|   | 預期畫面 | 傳送到該座標 |
|   | 預期封包或伺服器 log | console `[LiTo] GM .move player=… awaiting-C_GOTO_PORTAL`（`S/GmTravel.java:47-51,86`） |
|   | SQL 前後查詢 | 無 |
|   | 截圖檔名 | `G22-4-move.png` |
|   | 失敗時記什麼 | 訊息原文 |

<a id="g23"></a>
### G23 Ctrl 強制攻擊／光箭／負面技能

Ctrl 的做法：登入器的原生 hook 在按住 Ctrl、遊戲在前景、戰鬥中時，於封包尾端加 8 bytes `4C 46 43 31 <原 opcode> 01 67 A5`（`L/native/forced_combat.inc:2-18`、`forced_combat.h:19-28`）；伺服器拆掉後只對「被選取的那個目標」放寬敵我判定（`S/ForcedCombat.java:39-81`）。capture 裡看得到尾碼。

敵我規則：非 PvP 模式不能打玩家；隊伍模式不能打同隊（`gameplay.md:58`）。Ctrl 只排除寵物競賽（`S/ForcedCombat.java:72-73`）。

需要：D7、D10。完整版另需 D13（4 個帳號，2 對 2 死鬥）。

**完整版（有 D13）**：死鬥 2 對 2，A、C 紅隊，B、D 藍隊。

| 序 | 輸入 | 預期畫面 | 預期封包或伺服器 log | 截圖檔名 |
|---|---|---|---|---|
| 1 | A 不按 Ctrl 近戰點隊友 C | 沒有攻擊或沒有傷害 | 有沒有 `C_ATTACK 0x17`（9 bytes）都記下；沒有尾碼；沒有對 C 的傷害 | `G23-1-ally-noctrl.png` |
| 2 | A 按住 Ctrl 近戰點隊友 C | C 受到傷害，兩視窗一致 | `C_ATTACK` 17 bytes，尾碼 `4c4643311701 67a5`；`S_ATTACK 0x23` | `G23-2-ally-ctrl.png` |
| 3 | A 不按 Ctrl 近戰點敵人 B | B 受到傷害 | `C_ATTACK` 9 bytes；`S_ATTACK` | `G23-3-enemy.png` |
| 4 | A 不按 Ctrl 對隊友 C 放光箭（技能 4） | 沒有效果 | `C_USE_SPELL 0x14` 開頭 `140003`；沒有對 C 的 `S_ATTACK` | `G23-4-bolt-ally-noctrl.png` |
| 5 | A 按 Ctrl 對隊友 C 放光箭 | C 受到傷害、A 扣 MP | 同上加尾碼 `4c46433114 0167a5` | `G23-5-bolt-ally-ctrl.png` |
| 6 | A 不按 Ctrl 對隊友 C 放負面技能（29） | 沒有效果 | `C_USE_SPELL` 開頭 `140304`（技能 29＝第 3 列第 4 欄） | `G23-6-neg-ally-noctrl.png` |
| 7 | A 按 Ctrl 對隊友 C 放 29 | 走正常成功率：成功時 C 變慢並有圖示；失敗也扣 MP | 加尾碼；成功時有狀態封包 | `G23-7-neg-ally-ctrl.png` |
| 8 | A 不按 Ctrl 對敵人 B 放 29 | 同 7 的結果（敵方不需 Ctrl） | 無尾碼 | `G23-8-neg-enemy.png` |

預期依據：`M/forced-combat-0.6.67.md:5,7,20,24`；`gameplay.md:147-150`。

**2 帳號替代版（沒有 D13）**：

| 序 | 輸入 | 預期畫面 | 預期封包或伺服器 log | 截圖檔名 |
|---|---|---|---|---|
| 1 | 死鬥 1 對 1：A 不按 Ctrl 近戰點敵人 B | B 受到傷害 | `C_ATTACK` 9 bytes、無尾碼 | `G23-1-enemy.png`（＝`19-ctrl-attack.png` 的前半） |
| 2 | 同場：A 按 Ctrl 近戰點 B | B 受到傷害（與不按相同） | `C_ATTACK` 17 bytes、有尾碼 | `G23-2-enemy-ctrl.png` |
| 3 | 房主把模式切到 Time Attack（合作模式，玩家互為友方）開戰：A 不按 Ctrl 近戰點 B | 沒有傷害 | 無尾碼 | `G23-3-coop-noctrl.png` |
| 4 | 同場：A 按 Ctrl 近戰點 B | 〔待驗證〕依程式會造成傷害（Ctrl 只排除寵物競賽）；文件沒有寫非 PvP 模式是否該放寬 | 有尾碼；有沒有 `S_ATTACK` 照實記 | `G23-4-coop-ctrl.png` |
| 5、6 | 同第 3、4 列，改用光箭 | 同上 | `C_USE_SPELL` 開頭 `140003` | `G23-5-…`、`G23-6-…` |

六欄的其餘三欄（三個版本共用）：

| 欄 | 內容 |
|---|---|
| 輸入的共同條件 | 遊戲視窗要在前景；按住 Ctrl 再點；一次只操作一個視窗 |
| SQL 前後查詢 | 無（不看戰績）。Time Attack 那一場若中途離開，離開者會被記一敗（`S/ClientSession.java:13467-13484`），在結果檔註明這是測試造成的 |
| 失敗時記什麼 | 每列記：有沒有 C→S 幀、幀長、有沒有尾碼、目標 id、有沒有對應的 S→C；「客戶端沒送」「送了伺服器沒反應」「反應與預期相反」三種分開。第 4 列的實際結果交主控判定是不是要改規則 |

阻塞條件：模式切不到 Time Attack 或開不了戰（房間強制「雙方人數一致」是否擋住非隊伍模式沒有查證）→ 第 3–6 列填「阻塞」，由 D13 解除。

<a id="g24"></a>
### G24 雙玩家斷線

伺服器端對「關視窗」與「連線被重設」走同一條結算路徑；差別只在 console 是否多一行 `session ended`（`S/ClientSession.java:1287-1308`）。300 毫秒內的多個斷線會合併判定（`S/GameHub.java:31`；`gameplay.md:238`）。

| 序 | 欄 | 內容 |
|---|---|---|
| 1 關視窗 | 輸入 | 同 [A16](#a16)（可直接引用 A16 的結果，不重做） |
|   | 預期畫面／封包／SQL | 同 A16 |
|   | 截圖檔名 | `G24-1-close-window.png`（可複製 `16-a-wins.png`） |
|   | 失敗時記什麼 | 同 A16 |
| 2 強制結束客戶端（「拔線」的替代） | 輸入 | Q1 → `sql-before-G24-2.txt`。A、B 再開一場；進圖後對 **B 的 `LiTo.bin` PID**（自己啟動的那個）執行 `Stop-Process -Id <PID> -Force` |
|   | 預期畫面 | A 顯示勝利並進入結算 |
|   | 預期封包或伺服器 log | `[LiTo] session ended endpoint=GAME peer=…: …`（連線被重設時有這行，`:1305`）→ `battle disconnect pending … graceMs=300`（`:47547-47549`）→ `battle disconnect resolved … reason=…`（`S/GameHub.java:12974`）→ `battle result` 兩行 |
|   | SQL 前後查詢 | Q1 → `sql-after-G24-2.txt`：A 勝列、B 斷線敗列（同 A16） |
|   | 截圖檔名 | `G24-2-kill-client.png` |
|   | 失敗時記什麼 | 從結束程序到 `pending` 行的秒數、A 是否卡住、B 重新登入後能不能進大廳（舊連線有沒有殘留成 `0x16`） |
| 3 B 斷線後重新登入 | 輸入 | 第 2 列之後，重新啟動 `client-2`、帳號 B 登入 |
|   | 預期畫面 | 正常進大廳；不會被帶回舊場次，也不會再多記一筆 |
|   | 預期封包或伺服器 log | `S_LOGIN_CHECK` 結果 `0x00`；`gameplay.md:244` |
|   | SQL 前後查詢 | Q1 → `sql-after-G24-3.txt`，與 `sql-after-G24-2.txt` 相同 |
|   | 截圖檔名 | `G24-3-relogin.png` |
|   | 失敗時記什麼 | 登入結果碼、有沒有多出收據 |
| 4 實體拔線 | — | **阻塞**，見下 |

**實體拔網路線：阻塞條件＋誰能解除**

- 伺服器只接受 IPv4 loopback 的 `bind`／`game.bind`（`S/ServerConfig.java:256-257,1121-1126`），兩個客戶端一定在同一台機器上，沒有網路線可拔。
- 伺服器的讀取沒有逾時（`socket.setSoTimeout(0)`，`S/ClientSession.java:1278`），也沒有 keepalive 或閒置計時（未找到：查過 `S/` 的 `setKeepAlive`、`C_ALIVE` 處理 `:13778-13795`）。所以「對方沒有送 FIN 就消失」的情況，伺服器要等作業系統的 TCP 重傳放棄才知道；戰鬥中伺服器每 5 秒會送 `S_LITO_TIMELIMIT`（`:57548-57554`），時間長短由 OS 決定，也可能先到比賽時限而以逾時平手結束。
- 要在本機模擬「無 FIN」需要防火牆丟包或中繼程式，屬於改系統設定或執行自寫程式，執行限制不允許。
- 解除方式（需**主控或使用者**決定）：(a) 接受第 2 列作為替代，G24 的「拔線」以「強制結束」結案；或 (b) 另開任務評估伺服器加閒置逾時（改程式），之後再測。在決定前第 4 列填「阻塞：loopback-only」。

雙方同時斷線（無效場）、對戰中強制結束伺服器後重啟，屬 #355 的待驗證項目，由 T129 寫進 C 部，不在這裡重複。

<a id="g25"></a>
### G25 BUFF／魔法娃娃／藍水／血之渴望的圖示與到期

在已開戰的房間內做；建議用 Local Lobby 測試房（不影響戰績）。購買要在大廳或房間階段先買好（`S/ClientSession.java:18856-18859`）。

| 序 | 欄 | 內容 |
|---|---|---|
| 1 一般 BUFF（技能 26） | 需要 | D8 |
|   | 輸入 | A 對自己施放技能 26 |
|   | 預期畫面 | 施法動作與特效；狀態列出現敏捷提升圖示；滑鼠移上去有提示。持續 1200 秒；到期前 10 秒圖示變色（`L/docs/maintenance/world-resource-compat-current.zh-TW.md:75`）〔待驗證〕 |
|   | 預期封包或伺服器 log | `S_ACTION 0x20`、`S_EFFECT 0x37`、`S_MANA_POINT 0x4D`、`S_MAGE_DEXTERITY 0x6C`（秒數 1200、DEX、type 5；`S/ClientSession.java:36389-36437`）；console `[LiTo] C_USE_SPELL ability buff=physical-enchant-dexterity caster=… target=… duration=…`（`:36444-36468`） |
|   | SQL 前後查詢 | 無 |
|   | 截圖檔名 | `G25-1-buff-icon.png`；有等到到期時加 `G25-1-buff-expire.png` |
|   | 失敗時記什麼 | 有封包沒圖示、圖示位置或圖案錯；沒等到 1200 秒時填「到期未觀察」，不算通過也不算失敗 |
| 2 魔法娃娃 | 需要 | D12 |
|   | 輸入 | 雙擊背包的魔法娃娃召出；再雙擊收回 |
|   | 預期畫面 | 娃娃出現在角色旁、收回後消失。**狀態圖示：依目前 `src/main` 不會出現**（伺服器刻意不送） |
|   | 預期封包或伺服器 log | console `[LiTo] magic-doll-icon suppressed object=… icon=… remaining=… remove=… reason=no-native-receiver(FA=S_LITO_CLAN,7B=S_EVENT 0..40)`（`S/ClientSession.java:20298-20313`）；沒有圖示封包。登入器文件 `:49` 寫的「`FA→7B`、子碼 56／154」是未發布的隔離候選，與 `src/main` 不同 |
|   | SQL 前後查詢 | 無 |
|   | 截圖檔名 | `G25-2-doll.png` |
|   | 失敗時記什麼 | 娃娃沒出現、客戶端崩潰；「沒有圖示」照實記為「與 `src/main` 一致；是否要圖示交主控決定」 |
| 3 藍色藥水 | 需要 | D14（或 GM `.item 40015`） |
|   | 輸入 | 大廳開商店買藍色藥水 40015（700 金幣，每場額度 20,000）；進戰鬥後使用 |
|   | 預期畫面 | 喝藥特效；狀態列出現回魔圖示；MP 回復變快。600 秒後圖示消失（祝福版 700 秒） |
|   | 預期封包或伺服器 log | 購買清單 `S_LITO_BUY_LIST 0xDF`（`S/ClientSession.java:18640`）；使用 `C_USE_ITEM 0x1C`；圖示 `S_EVENT 0x7B` 子碼 34＋秒數（`S/SharedPresentationPackets.java:7-9`，送出 `S/ClientSession.java:22129`）；`S_MESSAGE_CODE 0x10` 代碼 1007（`:22147`）。到期**不送封包**，只有 console `[LiTo] blue-potion lease expired player=…`（`:75866`），圖示消失靠客戶端自己倒數 |
|   | SQL 前後查詢 | 無 |
|   | 截圖檔名 | `G25-3-bluepotion.png`；到期 `G25-3-bluepotion-expire.png` |
|   | 失敗時記什麼 | 有 `0x7B` 沒圖示、圖案不是藍水、到期行出現後圖示還在 |
| 4 血之渴望 | 需要 | D6 的龍騎士＋D11 |
|   | 輸入 | 龍騎士施放技能 186 |
|   | 預期畫面 | 施法特效；自己的狀態列出現血之渴望圖示（effect110，同文件 `:47`）；到期或被取代時消失 |
|   | 預期封包或伺服器 log | `S_ACTION 0x20`、`S_EFFECT 0x37`、`S_HIT_POINT 0x0D`、`S_MANA_POINT 0x4D`（`S/ClientSession.java:59590-59611`）；圖示 `S_EMOTION 0x62`，內容 object id、mode 6、秒數（`:59598`；`S/Protocol.java:12717-12719`）；被取代時送 mode 0（`:59595`） |
|   | SQL 前後查詢 | 無 |
|   | 截圖檔名 | `G25-4-bloodlust.png` |
|   | 失敗時記什麼 | mode 值、圖示有無、另一視窗看到的速度變化 |
| 5 回大廳清理 | 輸入 | 帶著第 1、3 列的圖示打完或離開，回到大廳 |
|   | 預期畫面 | 狀態列清空（同文件 `:75`） |
|   | 預期封包或伺服器 log | 未找到專用的清除封包（查過 `S/ClientSession.java:13187-13226` 離房段），以畫面為準 |
|   | SQL 前後查詢 | 無 |
|   | 截圖檔名 | `G25-5-lobby-clean.png` |
|   | 失敗時記什麼 | 殘留哪個圖示 |

<a id="g26"></a>
### G26 重複登入 `S_LOGIN_CHECK/0x16`

需要：帳號 A 一組、兩個視窗。不需要戰鬥。

| 序 | 欄 | 內容 |
|---|---|---|
| 1 | 輸入 | 視窗 1 用帳號 A 登入並停在大廳。視窗 2 選同一個來源，輸入**同一個帳號 A** 的帳密 |
|   | 預期畫面 | 視窗 2 出現原生的「帳號使用中」訊息框（文字與外觀是原生的）；視窗 1 不受影響 |
|   | 預期封包或伺服器 log | 視窗 2：`C_LOGIN 0x01`（capture 為 `MASKED_C_LOGIN`）→ `S_LOGIN_CHECK 0x00`，結果碼 `ACCOUNT_IN_USE 0x16`（`S/Protocol.java:1590`；`S/ClientSession.java:7128-7135`，note 含 `account-in-use`）。判斷在 `S/GameHub.java:1127-1136`：同帳號已有一條 GAME 連線就拒絕新的，**不踢舊連線** |
|   | SQL 前後查詢 | 無 |
|   | 截圖檔名 | `G26-1-in-use.png`（不得拍到密碼） |
|   | 失敗時記什麼 | 視窗 2 竟然登入成功、視窗 1 被踢、或沒有任何訊息；附 `S_LOGIN_CHECK` 的 `plain` |
| 2 | 輸入 | 在視窗 2 的訊息框按確定 |
|   | 預期畫面 | 回到**空白的帳號頁**（帳號、密碼欄都是空的），可以再輸入（`L/docs/maintenance/world-resource-compat-current.zh-TW.md:36`） |
|   | 預期封包或伺服器 log | 伺服器把該連線退回登入前的階段（`S/ClientSession.java:7450-7452`）；之後沒有新封包直到再次登入 |
|   | SQL 前後查詢 | 無 |
|   | 截圖檔名 | `G26-2-blank-page.png` |
|   | 失敗時記什麼 | 視窗關閉、卡住、或欄位殘留上次輸入 |
| 3 | 輸入 | 視窗 2 改輸入帳號 B |
|   | 預期畫面 | 正常登入 |
|   | 預期封包或伺服器 log | `S_LOGIN_CHECK` 結果 `0x00` |
|   | SQL 前後查詢 | 無 |
|   | 截圖檔名 | `G26-3-login-b.png` |
|   | 失敗時記什麼 | 結果碼 |

<a id="g27"></a>
### G27 map 12000

T130（#381）用 PakViewer 查客戶端資產後回填下表。在那之前本項是「等待 T130」。

| 欄位 | 內容（T130 回填） |
|---|---|
| T130 結論（三選一） | 「資產齊全」／「缺 X（附查過的 pak 與檔名清單）」／「伺服器不會送玩家到 12000」 |
| 會把玩家送到 12000 的模式／房間設定 | — |
| 對照用的正常地圖 | — |
| T130 報告位置 | `docs/reports/issue-381.md` |

本張先查到的事實（不能取代 T130）：在 `S/` 全部 `.java` 搜尋 `12000`，4 處都是數值不是地圖編號（`ClassicWorldWaterDragonMonsterCatalog.java:40`、`InventoryCatalog.java:1568`、`NativeDeItemTemplate.java:276,280`）；戰鬥地圖清單見 `gameplay.md:47-53`，沒有 12000。

依 T130 結論的實機步驟：

| 結論 | 輸入 | 預期畫面 | 預期封包或伺服器 log | SQL | 截圖檔名 | 失敗時記什麼 |
|---|---|---|---|---|---|---|
| 伺服器不會送玩家到 12000 | 不做實機 | — | 整輪 A、B 部的 console 與 capture 搜尋 `12000`，沒有以地圖編號出現（`S_WORLD 0x28` 的地圖欄） | 無 | 無；結果檔填「不適用（引用 T130）」 | 若搜尋到，改走下一列 |
| 會送，資產齊全或缺 | 依 T130 指出的模式開一場 | 齊全：正常進圖。缺：記錄實際畫面（黑畫面、缺地表、崩潰） | `S_WORLD 0x28`（`S/ClientSession.java:10683`）的地圖編號是 12000 | 無 | `G27-1-map12000.png` | probe 的 stdout／stderr、`lito381-compat.log`、崩潰時的退出碼 |

<a id="g02"></a>
### G02 技能 26 不按 Ctrl 的現場封包

目的：分辨「客戶端沒送／送錯目標」與「伺服器拒絕」。只記錄，不修。

**已知（讀程式與文件）**

| 事實 | 出處 |
|---|---|
| 技能 26＝通暢氣脈術，CLAN 目標；MP 50、冷卻 10 秒、持續 1200 秒、DEX +5 | `M/combat-continuation-current.zh-TW.md:76`；`S/BattleSpellCatalog.java:50,1251-1254` |
| C→S `C_USE_SPELL 0x14`，7 bytes：`14 03 01 <目標 object id 4 bytes LE>`；沒有座標 | `S/Protocol.java:130,9064-9088`；`S/ForcedCombat.java:26-33` |
| 按 Ctrl 時變 15 bytes，尾端多 `4C 46 43 31 14 01 67 A5` | `L/native/forced_combat.h:19-28` |
| 伺服器：目標是自己或有 Ctrl → 放行；沒有 Ctrl 且目標是敵方 → **靜默返回**（不送封包、不印 log）；友方放行 | `S/NativeSelectedPlayerIntent.java:6-17`；`S/ClientSession.java:36279-36281` |
| 放行後：目標不是自己時還要**同一個非空血盟**；否則只印 `[LiTo] C_USE_SPELL ability buff rejected skill=26 target=… same-clan=false same-context=… phase=…`，不送任何封包 | `S/ClientSession.java:36289-36320` |
| 目標 id 為 0 或長度不對：`[LiTo] rejected malformed C opcode=0x14 … reason=invalid native object C_USE_SPELL layout` | `S/Protocol.java:9080-9082`；`S/ClientSession.java:1946-1948` |
| MP 不足：`S_MESSAGE_CODE 0x10` 代碼 278 | `S/ClientSession.java:36380` |
| 成功：`S_ACTION 0x20`、`S_EFFECT 0x37`、`S_MANA_POINT 0x4D`、`S_MAGE_DEXTERITY 0x6C`，console `ability buff=physical-enchant-dexterity caster=… target=…` | `S/ClientSession.java:36389-36468` |
| 登入器端沒有 Ctrl 的追蹤檔；證據只能看伺服器 capture | `L/native/forced_combat.inc`、`forced_combat.h`（沒有寫檔呼叫） |
| 伺服器合成封包 16 案自施都正常；至今沒有現場封包 | `M/combat-continuation-current.zh-TW.md:77`；`M/combat-integration-lifecycle.zh-TW.md:285` |

需要：D8（A 已學技能 26、MP ≥ 50）；「對友方」另需 D9（A、B 同血盟）與一個 B 是友方的場次——D13 的 2 對 2 死鬥（A、C 同隊），或 2 帳號時用 Time Attack（玩家互為友方）〔推測可開，待驗證〕。

**準備**

1. 用 SQL 取 A、B 的 object id：`SELECT char_obj_id, char_name FROM p24_accounts_lito_exp.character_lito WHERE char_name IN ('<角色A>','<角色B>');`，換算成 4 bytes LE 的 hex，寫進 `evidence/G02-objids.txt`（去識別後才進結果檔）。
2. 從 console 的 `[LiTo] capture=` 行記下本次 `frames.jsonl` 路徑。
3. 每一案之前在 `evidence/G02-times.txt` 記 UTC 時間；兩案之間至少等 12 秒（冷卻 10 秒）；另一個視窗不要操作。

**四案**（每案做 3 次）

| 序 | 輸入 | 截圖檔名 |
|---|---|---|
| 1 | A 選技能 26，**不按 Ctrl**，點自己 | `G02-1-self-noctrl.png` |
| 2 | A 選技能 26，**按住 Ctrl**，點自己 | `G02-2-self-ctrl.png` |
| 3 | A 選技能 26，**不按 Ctrl**，點友方 B | `G02-3-ally-noctrl.png` |
| 4 | A 選技能 26，**按住 Ctrl**，點友方 B | `G02-4-ally-ctrl.png` |

每案的六欄：

| 欄 | 內容 |
|---|---|
| 輸入 | 如上表 |
| 預期畫面 | 四案都應該成功：施法動作、目標身上有特效、目標的狀態列出現敏捷圖示、A 的 MP −50。使用者回報的現象是第 1、3 案沒有反應 |
| 預期封包或伺服器 log | 第 1 案：`client_to_server`、`opcode 0x14`、`plain`＝`140301`＋A 的 id（共 7 bytes）。第 2 案：同上＋尾碼（15 bytes）。第 3、4 案：目標是 B 的 id。之後應有成功那一組 S→C 與 console `ability buff=…` |
| SQL 前後查詢 | 無（準備步驟的 object id 查詢除外） |
| 截圖檔名 | 如上表 |
| 失敗時記什麼 | 把該案時間段內所有 `opcode 0x14` 的 C→S 行與其後 2 秒的 S→C 行剪到 `evidence/frames-G02-<序>.jsonl`，並填下面的判讀表 |

**判讀表**（每案填一列，寫進結果檔）

| capture 看到什麼 | console 看到什麼 | 結論 |
|---|---|---|
| 該時間段**沒有** `0x14` 的 C→S 幀 | 無 | 客戶端沒有送包：游標／選取在客戶端就被擋（登入器 `L/native/forced_combat.inc` 的送出點 0x4C6B27 之前） |
| 有幀，`plain[3..6]` ≠ 預期目標（是 0、是別人、或不是任何玩家的 id） | `rejected malformed …` 或 `ability buff rejected … same-context=false` | 客戶端送錯目標：選取物件 ID 的問題 |
| 有幀，目標正確，無尾碼；之後**沒有任何** S→C、也沒有 `rejected` 行 | 無 | 伺服器靜默拒絕：`NativeSelectedPlayerIntent` 把目標當敵方（查該場模式與隊伍） |
| 有幀，目標正確 | `ability buff rejected … same-clan=false` | 伺服器因血盟拒絕：資料問題（D9），不是 Ctrl 問題 |
| 有幀，目標正確 | `ability buff rejected state …` | 冷卻、死亡或狀態（`S/ClientSession.java:36351-36363`）；重做 |
| 有幀，目標正確 | `ability buff=physical-enchant-dexterity …` 且有四個 S→C | 伺服器成功；畫面仍沒效果的話是客戶端顯示問題 |

結果檔另外寫明：四案各 3 次的結果、A／B 的血盟與隊伍、該場模式、所用 `frames.jsonl` 的時間段。

阻塞條件：沒有 D8 → 全部阻塞（T128 seed）。沒有 D9 或沒有友方場次 → 第 3、4 案阻塞，第 1、2 案照做。

---

## C 部

由 T129（#380）依同樣的六欄格式接在這裡，截圖 `V<序>-<短名>.png`。

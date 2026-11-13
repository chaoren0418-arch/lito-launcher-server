# M4 實機驗收操作手冊（RUNBOOK）

更新：2026-11-13（T127，Issue #378）。A 部＝[`CHECKLIST.md`](CHECKLIST.md) 01–21 逐步展開；B 部＝[`customer-gaps.md`](../reports/customer-gaps.md) 的 G19–G27 與 G02 現場封包；C 部＝M3／M3b／M3c 與 #366–#371 各報告的實機待驗證項目 V01–V38（T129，Issue #380）。

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
| 失敗時記什麼 | 退出碼、完整 console、最後一個 `Caused by`。常見：`lacks ranking columns`（`S/SettlementSql.java:46`，缺 P4 的欄位）、`SELECT command denied`（授權）、`Unknown column`（靜態庫結構）、`Unknown/retired setting`／`Missing setting`（設定鍵，`S/UnifiedConfiguration.java:24-25`）、`settlement journal already owned`（`S/SettlementJournal.java:18-19`，同一 `runtime/` 有另一個程序） |

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
| 失敗時記什麼 | 結束原因（`reason=`）、是否逾時、console 有沒有 `battle result` 兩行、有沒有 `state save failed`（`:56917`）或 stderr `settlement recovery pending`（`S/DurableBattleSettlement.java:201`） |

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
| 預期封包或伺服器 log | console 的兩行 `battle result` 數值與 SELECT 一致。寫入順序是 WAL → 收據 → `character_lito` 單一 UPDATE → `applied=1` → 刪 WAL（`gameplay.md:281`；`S/SettlementSql.java:63,138,142`） |
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

T130（#381）已用 PakViewer 靜態查核，報告在 [`issue-381.md`](../reports/issue-381.md)。本項**沒有進圖的實機步驟**，只有預檢與判讀。

| 欄位 | 內容（T130 回填，2026-11-13） |
|---|---|
| T130 結論 | **客戶端資產齊全**，而且**伺服器不會經由房間模式送玩家到 12000**。#83／#225 看到的「缺資產」是伺服器啟動訊息 `source map physical asset not packaged: 12000 -> 12000`，缺的是伺服器 JAR 內建地形，不是客戶端檔案 |
| 客戶端證據 | `D:\lineage開發\0-客戶端\map\12000` 有 66 個 `.s32`，用到的 115 個 tile 在 `Tile.pak` 全部存在；`Text.pak` 的 `zone3-c.tbl` 有名稱列；查過 24 個有索引的 pak（清單與項目數見報告） |
| 會把玩家送到 12000 的模式／房間設定 | 無。六個戰鬥模式加城鎮的地圖池都在 500–574（`BattleRules.java:41-73`）；`lito_room_policy` 沒有地圖欄位；GM 傳送只允許同地圖（`GmTravel.java:49`） |
| 對照用的正常地圖 | 競技場 504（`LiTo\LTmap\504`，使用者已確認能建房）與原版地圖 0。12000 相對於地圖 0 只少小地圖設定與 `.seg`／`.MarketRegion` |
| 伺服器缺口 | JAR 內建地形索引 `combat/terrain/source-index.tsv` 是 715 張，不含 12000、12004；正式 0.6.71 JAR 與 `src/main` 相同。靜態庫 `mapids` 只要有這兩列（參考伺服器 `1-伺服器` 在 2026-10-29 新增），啟動就會失敗，與玩家選哪張圖無關〔程式碼路徑判讀，未實跑〕 |
| T130 報告位置 | `docs/reports/issue-381.md` |

預檢與判讀（T128 預檢或 T137 啟動伺服器前後各做一次）：

| 步驟 | 輸入 | 預期 | 預期封包或伺服器 log | SQL | 截圖檔名 | 失敗時記什麼 |
|---|---|---|---|---|---|---|
| G27-1 靜態庫預檢 | 對實驗環境的靜態庫執行右列查詢（唯讀） | 回傳 0 列：本項不影響本輪。回傳 1–2 列：先確認使用的 JAR 已含這兩張地形，否則走 G27-2 的失敗判讀 | 無 | `SELECT mapid, CopyMapId FROM mapids WHERE mapid IN (12000,12004) OR CopyMapId IN (12000,12004);` | 無；結果檔記查詢輸出 | 查詢輸出與使用的 JAR SHA-256 |
| G27-2 啟動判讀 | 依 A01 啟動伺服器 | 正常啟動 | console 沒有 `source map physical asset not packaged` | 無 | 無 | 出現該訊息：記完整一行（含 `<id> -> <physical>`）與 G27-1 的查詢輸出，回報「伺服器內建地形與靜態庫 `mapids` 不一致」。**不要查客戶端 pak，不要複製 map 進專案**；修法選項見報告「給下一棒的提示」 |
| G27-3 整輪搜尋 | A、B 部跑完後，在 console 與 capture 搜尋 `12000` | 沒有以地圖編號出現 | `S_WORLD 0x28`（`S/ClientSession.java:10683`）的地圖欄不是 12000 | 無 | 無；結果檔填「不適用（引用 T130）」 | 若出現：記模式、房間設定與該封包，這與 T130 的結論矛盾，需回報主控 |

仍待驗證（T130 無法靜態回答）：實驗庫與正式庫的 `mapids` 實際內容；含這兩列時的實際啟動結果；`LiTo.bin` 能否載入 `map\12000`（目前沒有伺服器路徑會送玩家過去，不擋驗收）。

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

## C 部：M3／M3b／M3c 與 #366–#371 的實機待驗證（T129，#380）

來源是 `docs/reports/issue-301.md`～`issue-371.md` 各報告的「待驗證」與「給下一棒的提示」。**沒有任何一項已實機執行**。不需要實機而剔除的項目與理由在 [`issue-380.md`](../reports/issue-380.md)。

### C 部讀法

- `R/` = `docs/reports/`。每項的「來源」寫 `R/issue-NNN.md:行`，行號是 2026-11-13 的 `main`（`9be54257`）。
- 六欄與 A、B 部相同；多一行「需要」：角色、技能、道具、模式。
- 技能封包開頭：`C_USE_SPELL 0x14` 的 `plain[1]`＝`(技能編號−1)÷8` 的商、`plain[2]`＝餘數（`S/ForcedCombat.java:27`）。本部列出的開頭 hex 都是照這條公式換算的，沒有現場封包對過。
- 「記錄型」＝來源報告沒有給單次可判定的預期（手感、頻率、平衡）；照實記數字，不填通過／失敗，交主控判讀。
- 不影響戰績的項目一律在 Local Lobby 測試房做（`gameplay.md:226`；單人時有 ArenaBot，`gameplay.md:87`）；要看 `character_lito` 的項目才用自建房。
- 伺服器若整個卡住（所有視窗沒反應、console 不再出新行）：先對 Java PID 執行 `jstack <PID>` 存成 `evidence/jstack-V<序>.txt` 再處理。已知有一個偶發的鎖順序死結，條件是「同一時間一人開戰、另一人斷線」（`R/issue-370.md:40-44`），V19–V21 最容易碰到。

### C 部的三個整體阻塞

| 代號 | 內容 | 依據 | 誰能解除 |
|---|---|---|---|
| **阻塞：功能未開放**（租屋城鎮） | 實驗設定打不開租屋城鎮。統一設定只接受 22 個鍵，寫 `lito.rental.town.directory.enabled` 會被 `Unknown/retired setting` 拒絕（`S/UnifiedConfiguration.java:7,24`）；載入後又固定寫成 `false`、傳送路線清空（`:75`）。程式裡唯一會把它改回 `true` 的是啟動參數 `--service-mode=rental-source`（`S/SharedResourceMain.java:13` 轉給 `S/Main.java:18-27`；`S/ServerConfig.java:253,703-726`，在統一設定套用之後才執行），但它同時把 `port` 寫死成 **32000**（正式登入器用的埠）、位址寫死 `127.0.0.1`／`127.0.0.2`，還需要另一個 `rental-battle` 程序佔 `127.0.0.3`／`127.0.0.4:32001`（`:727-743`）。這不符合 M4「只用 42083 起的實驗埠、不碰正式埠」的前提；`port` 改掉後 `login.source.port.base` 仍是 42083（`UnifiedConfiguration.java:79`），兩者不一致時能不能啟動沒有查證〔待驗證〕 | 左列行號 | 主控決定：(a) 開一張 code 任務，讓租屋城鎮能在實驗設定下以實驗埠開啟；或 (b) 使用者同意在正式伺服器停機時段用 `--service-mode` 雙程序跑（要佔 32000／32001）。決定前 V35–V37 不做 |
| **阻塞：經典世界關閉** | 統一設定固定 `classic.world.compatibility.enabled=false`、`classic.world.monster.ai.enabled=false`（`S/UnifiedConfiguration.java:74`），沒有鍵可開。來源報告裡寫「WORLD」「經典世界」「攻城戰期間旗區」的項目在實驗環境做不到 | 同左 | 同上（需要改程式或另一種設定載入方式）。V38 列出受影響的來源行 |
| **阻塞：P5** | 與 A 部相同：沒有客戶端啟動腳本時，所有需要遊戲視窗的項目都做不了。只有 V01、V02（伺服器部分）、V06 不受影響 | [P5](#p5-客戶端啟動方式阻塞條件) | T128（#379）或主控 |

另外讀程式時看到一件與既有文件不一致的事〔推測，沒有執行〕：統一設定的 22 個鍵沒有 `battle.reward.win`／`battle.reward.loss`（`S/UnifiedConfiguration.java:7`），寫了會被拒絕，所以統一設定下積分一律是程式預設的勝 250／敗 50（`S/ServerConfig.java:578-579`）；`gameplay.md:304` 寫的「在 `config/server.properties` 加這兩個鍵可改」在統一設定下不成立。C 部的積分預期值都用 250／50。

### C 部資料前置（`tools/acceptance/m4-seed-exp.sql`）

T128（#379）在本張進行中合併，所以能直接改的已經寫進 `m4-seed-exp.sql`（S3 的 `ElfAttr`、S4 技能、S5 道具；CI 的預期筆數一併更新），角色名稱是 seed 建的 `M4…`。seed 還沒有對 P24Lab 執行過。「現況」欄寫「已進 seed」的是本張加的；其餘是還缺的需求，全部只寫 `*_lito_exp`。

| # | 需要什麼 | 哪些項目要用 | 現況 |
|---|---|---|---|
| E1 | 第 3 個帳號 C（`M4C1`，騎士）。D13 是「可選」：私密檔要把 `@m4_c_login`／`@m4_c_password` 填上才會建 | V15、V17、V18 | seed 支援；執行時要填 |
| E2 | 帳號 A 的法師 `M4A4`：技能 10、28、39、44、47、50、58、76、80、17、40，BUFF 用的 3、12、21、43；魔法寶石 40318 ×100（40／44／47／76 的材料） | V09–V16 | 已進 seed |
| E3 | V12 的目標：一個**法師**先對自己上 3、12、21、26、43（3、12、21 在靜態庫是只能對自己的技能）。用 GM 帳號的 `M4GM` | V12 | 已進 seed（`M4GM` 加 3、12、21、43；原本就有 26）。GM 身分會不會影響相消結果〔待驗證〕 |
| E4 | 騎士 `M4A2` 已學 87，另要一把近戰武器 | V14 | 87 有；**武器沒有**（裝備的載入規則 T128 沒查完，`tools/acceptance/README.md`「做不到或沒有做的項目」）。87 空手能不能放待實機；不行時用 GM `.item` 取得武器 |
| E5 | 妖精 `M4A3`：`ElfAttr`＝8（風）、技能 148、149、152、153、167、精靈玉 40319 ×30。靜態庫這五個技能只有 167 的 `skills.attr` 不是 0（＝8） | V15 | 已進 seed |
| E6 | 黑暗妖精 `M4A5`：103＋二級黑魔石 40321 ×30；龍騎士 `M4DK` 加 194 | V15 | 已進 seed |
| E7 | `M4A4` 背包：治癒藥水 40010 ×10、自我加速藥水 40013 ×5、勇敢藥水 40014 ×5、加速魔力回復藥水 40015（編號取自 `docs/design/items-consumables.md:15-23`） | V07 | 已進 seed。背包變重後 `M4A4`（STR 8）會不會超重待實機 |
| E8 | 一件帶加速效果的裝備，或強化勇敢藥水（`R/issue-336.md:47`），放在 V13 的目標身上 | V13 | **缺**：item_id 與裝備載入規則沒有查 |
| E9 | 能在競技房帶出的一隻受控 NPC：法師的召喚術（含材料），或寵物（項圈＋寵物資料列）。哪一種在競技房可用沒有查證 | V17、V31、V40 | **缺** |
| E10 | 魔法娃娃（＝D12；`M4DK`、`M4IL` 各一） | V08 | T128 已放 |
| E11 | GM 帳號（＝D3、D4），用 `.loc`／`.move` 對座標 | V16 | T128 已放 |
| E12 | 讓 A 的 `character_lito.monthly_period` 是上個月（格式 `YYYYMM`，`db/lito_character_ranking.sql:17`）且 `monthly_ranking_points`、`monthly_wins` 不是 0。這是 **UPDATE**，不符合 seed「只新增、不 UPDATE」的規則（`m4-seed-exp.sql` 檔頭） | V22 | **缺**：要主控同意另外放一段一次性的實驗庫 UPDATE |
| E13 | 第二個來源：另一個 `*_lito_exp` 角色庫（結構同 `p24_accounts_lito_exp`，含 `character_lito` 新欄位），`lito_source_server` 第二列 `enabled=1`，兩個來源各有一個 **`objid` 相同**的角色，分屬可登入的帳號。`m4-db-prepare.sql` 目前把路由改成只啟用一條 | V34 | **缺**：量大，建議另開一張 |
| E14 | (a) 屬性不符的妖精：可選帳號 D 的 `M4D1`（`ElfAttr`＝0）學 167、精靈玉 ×5。(b) 風／生命／形象之魔眼任一。(c) 已裝備的幻象之劍或幻象之弓 | V28；V29；V30 | (a) 已進 seed（要填 `@m4_d_login`）；(b)(c) **缺**：item_id 沒有查 |

沒有對應資料的項目填「阻塞：E<編號>」，不臨時改資料。

### C 部時間盒（一輪 60 分鐘）

| 輪 | 內容 | 帳號／角色 | 模式 | 和誰同一輪 |
|---|---|---|---|---|
| **C0** | V01–V06：不另外開場，全部掛在 A 部的步驟上 | A、B | 與 A 部相同 | **和 A 部同一輪**：V01＝A01、V02＝全程、V03＝A14、V04＝A15 與 A20 之後、V05＝A04 之後任何時候、V06＝P4 |
| **C1** | V07–V14：藥水、娃娃、法師效果、時序 | A 法師、B；V14 換騎士 | Local Lobby 死鬥（兩人同房，互為敵方） | 可接在 B 部 G25 後面同一輪（同一批角色與道具）；V07 的藍水＝G25-3、V08＝G25-2，做一次填兩處 |
| **C2** | V15–V18、V40：目標判定、邊界、受控 NPC、觀戰。V40（T135，#386）是後來加的，排在 V17 前面做（用同一隻寵物／召喚物；V17 會讓牠退役） | A、B、C；GM | Local Lobby 死鬥；V15 的「打不到隊友」要 D13 的 2 對 2 | 可與 B 部 G23 完整版同一輪（同樣要第 3、4 個帳號） |
| **C3** | V23–V27、V39：PvE 與逾時（等待時間長：180＋300 秒，V26 再加 1200 秒）。V39（T132，#383）是後來加的，Quest 的部分接在 V23 後面、Time Attack 的部分接在 V27 前面做 | A、B | Quest、死鬥、寵物競賽、Time Attack、攻城 | 獨立一輪；V24 的 180 秒等待期間不要操作 |
| **C4** | V19–V22：斷線與重啟復原 | A、B | 自建房死鬥（要看戰績） | **最後一輪**，接在 B 部 G24 後面；V21 會強制結束實驗伺服器，之後要重跑 A01–A02 |
| **C5** | V28–V33：選做與記錄型 | 依各項 | 依各項 | 有剩餘 lease 時間才做 |
| — | V34–V38：阻塞 | — | — | 不排 |

C0 不增加時間；C1–C4 各一輪。lease 只夠一輪時的優先順序：C0（跟著 A 部）→ C4 的 V19、V21 → C1 的 V07 → 其餘。

---

### C0：併入 A 部的項目

<a id="v01"></a>
#### V01 啟動時消耗品參數讀表

來源：`R/issue-352.md:26`。需要：無（伺服器 console）。

| 欄 | 內容 |
|---|---|
| 輸入 | 在 [A01](#a01) 的 console 輸出裡找 `consumable parameters` |
| 預期畫面 | — |
| 預期封包或伺服器 log | `[LiTo] consumable parameters loaded from p24_380db_lito_exp.etcitem: items=11 differing-from-built-in=0 fallbacks=0`（`S/ConsumableItemParameters.java:189-190`）。來源報告寫的是正式庫名 `380_db.etcitem`；log 印的是設定裡的靜態庫名（`:125,133`），實驗環境是 `p24_380db_lito_exp` |
| SQL 前後查詢 | `SELECT item_id, classname FROM p24_380db_lito_exp.etcitem WHERE item_id IN (40010,40011,40012,40506,40013,40014,40016,40068,40015,140015,40736);` 存 `sql-before-V01.txt`，對照 `docs/design/items-consumables.md:15-25` |
| 截圖檔名 | `V01-consumable-log.png` |
| 失敗時記什麼 | 整行原文。`differing-from-built-in` 不是 0：實驗庫的值與內建不同，把後面逐項差異的 log 行一起記下（不算程式錯，V07 的預期值要跟著改）。`fallbacks` 不是 0：記每一列的原因。出現 `etcitem unavailable (…); using built-in values`（`:136`）：記括號內的例外（多半是授權或表不存在） |

<a id="v02"></a>
#### V02 整輪 console 不得出現的字串

來源：`R/issue-317.md:31`、`R/issue-339.md:90`、`R/issue-370.md:56`、`R/issue-335.md:72`、`R/issue-355.md:119`。需要：無。

| 欄 | 內容 |
|---|---|
| 輸入 | 每一輪結束（A20 的 Ctrl+C 之後）對存下來的 console 檔搜尋下列字串 |
| 預期畫面 | — |
| 預期封包或伺服器 log | 下列都**不出現**：`invalid companion NPC damage input`（`S/ClassicWorldCompanionNpcPhysicalAttack.java:140`）、`invalid source weapon combat traits`（`S/LegacyEquipmentMagicLoader.java:810`、`S/Accounts.java:3016`）、`default room reset snapshot missing`（`S/ClientSession.java:13340`）、`lacks ranking columns`（`S/SettlementSql.java:46`）、`Settlement character identity changed`（`S/SettlementSql.java:129`）、`state save failed`、`settlement recovery pending`（`S/DurableBattleSettlement.java:201`） |
| SQL 前後查詢 | 無 |
| 截圖檔名 | `V02-console-grep.png`（搜尋命令與 0 筆結果） |
| 失敗時記什麼 | 命中的整行、前後各 20 行、當時在做哪一步。第一個字串只有帶寵物／召喚物時才有機會出現（V17、V31），第二個只有角色帶幻象武器時才有機會出現（V30）；沒做那幾項時在結果檔註明「該字串的觸發條件本輪沒有發生」 |

<a id="v03"></a>
#### V03 最後一名成員從結算返回

來源：`R/issue-370.md:33,56`。需要：A、B；自建房死鬥（就是 A08–A14 那一場）。

| 欄 | 內容 |
|---|---|
| 輸入 | [A14](#a14) 的結算畫面上，**輸家先按返回，贏家最後按** |
| 預期畫面 | 兩人都回到大廳；房間列表正常顯示，剛才的自建房不在列表上；沒有卡在結算畫面 |
| 預期封包或伺服器 log | 每人各自 `C_LITO_EXIT_ROOM 0xD0` → 自己的 `S_LITO_EXIT_ROOM 0xD0`；最後離開的那一位之後收到完整的 `S_LITO_ROOM_INFO 0xCC` 房間列表（`R/issue-370.md:55`）。console 沒有 `default room reset snapshot missing`，沒有例外堆疊 |
| SQL 前後查詢 | 無（戰績在 A15 看） |
| 截圖檔名 | `V03-last-leave-lobby.png`（兩視窗並排的大廳） |
| 失敗時記什麼 | 誰卡住、卡在哪個畫面、最後一個 C→S／S→C、console 的例外整段 |

<a id="v04"></a>
#### V04 積分顯示與重啟後保留

來源：`R/issue-335.md:57,75`；`R/issue-301.md:26`（當時推測重啟歸零，#335 已改為持久化）。需要：A、B；A13 那一場的結果。

| 欄 | 內容 |
|---|---|
| 輸入 | ① [A15](#a15) 之後在大廳看 A 的積分／戰績顯示。② [A20](#a20) 正常關閉伺服器後，用同一個 `runtime/` 再做一次 A01 啟動，A 重新登入到大廳再看一次 |
| 預期畫面 | ① 勝者積分比開戰前多 250、敗者多 50。② 重啟後顯示的積分、勝敗數與重啟前相同，沒有歸零 |
| 預期封包或伺服器 log | ① console `battle result player=… won=true … ranking-points=…`（`S/ClientSession.java:56906-56915`）。② 重啟的 console 沒有 `lacks ranking columns`；積分顯示用哪個封包沒有查證，以畫面為準 |
| SQL 前後查詢 | 重啟前 Q1 → `sql-before-V04.txt`（可用 `sql-after-20.txt`）；重啟並登入後 Q1 → `sql-after-V04.txt`。兩者 `ranking_points`、`disconnects`、`monthly_period`、`monthly_ranking_points`、`monthly_wins`、`monthly_losses`、`wins`、`losses`、`version`、`last_result_id` 全部相同；`monthly_period` 是當月 `YYYYMM`；`last_result_id` 是最後一場的收據 |
| 截圖檔名 | `V04-points-before-restart.png`、`V04-points-after-restart.png` |
| 失敗時記什麼 | 畫面數字與 SQL 是否一致（畫面錯、SQL 對＝顯示問題；SQL 也變了＝寫回問題）、哪一欄變了、重啟 console 全文 |

斷線敗 −50（`R/issue-335.md:75` 的③）＝[A16](#a16)／[G24](#g24) 第 2 列，不另列。

<a id="v05"></a>
#### V05 背包物品名稱顏色（祝福欄）

來源：`R/issue-339.md:92`（推測在 `:73-74`）；`R/issue-318.md:89`。需要：A 的角色背包有金幣、任一藥水、一把一般武器（D7、E7 備妥就有）；不必進戰鬥。

| 欄 | 內容 |
|---|---|
| 輸入 | A 進大廳或房間後打開背包，逐一看金幣、藥水、一般武器的名稱顏色；滑鼠移到物品上看提示 |
| 預期畫面 | 〔待驗證〕報告的推測是：從資料庫載入的一般物品（`bless=0`）被客戶端畫成**祝福**的顏色（`R/issue-339.md:55,73-74`）。本項是記錄型：照實記每件物品的顏色 |
| 預期封包或伺服器 log | 背包封包裡每件物品的祝福欄直接送資料庫的值（`S/Protocol.java:12753`）：一般物品是 0。客戶端慣例 0＝祝福、1＝一般（`S/RuntimeInventory.java:17-20`） |
| SQL 前後查詢 | `SELECT item_id, item_name, bless FROM p24_accounts_lito_exp.character_items WHERE char_id=<A 的 objid>;` → `sql-before-V05.txt` |
| 截圖檔名 | `V05-inventory-colors.png`（背包整頁）、`V05-item-tooltip.png` |
| 失敗時記什麼 | 不判通過／失敗。記每件物品：`item_id`、資料庫 `bless`、畫面顏色、提示文字有沒有「受祝福的」字樣。結果交主控決定要不要做 `R/issue-339.md:105-110` 的慣例統一 |

<a id="v06"></a>
#### V06 實驗靜態庫的 bless 分布

來源：`R/issue-318.md:66`（當時沒有連資料庫）。需要：無（只有 SQL；正式庫不查）。

| 欄 | 內容 |
|---|---|
| 輸入 | 在 [P4](#前置p-部) 之後用受限帳號執行右列查詢 |
| 預期畫面 | — |
| 預期封包或伺服器 log | 無 |
| SQL 前後查詢 | `SELECT bless, COUNT(*) FROM p24_380db_lito_exp.weapon GROUP BY bless;`、同樣查 `etcitem`、`armor`。欄名若不是 `bless`（1-伺服器 中文欄位版叫 `祝福狀態`，`R/issue-339.md:49`）先用 `SHOW COLUMNS` 確認。存 `sql-before-V06.txt` |
| 截圖檔名 | `V06-bless-distribution.png` |
| 失敗時記什麼 | 記錄型。預期一般物品多數是 0、祝福是 1（內部慣例，`R/issue-339.md:48-49`）；若出現其他分布照實記，並註明欄名 |

---

### C1：藥水、娃娃、法師效果、時序（Local Lobby，A、B 同房）

共同做法：A、B 進同一個頻道的 Local Lobby 開戰（死鬥，兩人互為敵方）。每個技能施放前在 `evidence/V<序>-times.txt` 記 UTC 時間，一次只操作一個視窗。HP／MP 用畫面數字，並以 capture 的 `S_HIT_POINT 0x0D`、`S_MANA_POINT 0x4D` 為準。

<a id="v07"></a>
#### V07 喝紅水、綠水、勇水、藍水

來源：`R/issue-352.md:26`。需要：E7；任何職業；先讓 A 被 B 打掉一些 HP。

| 欄 | 內容 |
|---|---|
| 輸入 | 戰鬥中 A 依序使用：治癒藥水 40010（連喝 5 瓶，每瓶記回復量）、自我加速藥水 40013、勇敢藥水 40014、加速魔力回復藥水 40015 |
| 預期畫面 | 紅水：每瓶回復 10～20 HP，特效 gfx 189。綠水：移動變快 300 秒，特效 191。勇水：攻擊變快 300 秒，特效 751。藍水：狀態列出現回魔圖示 600 秒，特效 190（`docs/design/items-consumables.md:15,19,20,23`；V01 的 `differing` 不是 0 時改用實驗庫的值） |
| 預期封包或伺服器 log | 每瓶一筆 `C_USE_ITEM 0x1C`；紅水後 `S_HIT_POINT 0x0D`、`S_EFFECT 0x37`；藍水同 [G25](#g25) 第 3 列（`S_EVENT 0x7B` 子碼 34）。綠水、勇水的狀態封包沒有查證，以 capture 為準 |
| SQL 前後查詢 | 無 |
| 截圖檔名 | `V07-red.png`、`V07-green.png`、`V07-brave.png`、`V07-blue.png`（藍水可與 `G25-3-bluepotion.png` 同一張） |
| 失敗時記什麼 | 5 瓶紅水各自的回復量（有沒有超出 10～20）、綠水／勇水實際持續秒數（從使用到效果消失，各等 300 秒；沒等完填「到期未觀察」）、用不了時的訊息原文。勇水若被職業擋下也照實記（`R/issue-352.md:29`：LiTo 目前不檢查職業） |

<a id="v08"></a>
#### V08 魔法娃娃：召喚、血盟畫面、收回

來源：`R/issue-311.md:51`。需要：E10（D12）；戰鬥房（來源寫「WORLD 或戰鬥房」，WORLD 做不到，見 V38）。與 [G25](#g25) 第 2 列同一次操作。

| 欄 | 內容 |
|---|---|
| 輸入 | 雙擊娃娃召出 → 打開血盟視窗看一眼 → 再雙擊一次收回 → 再召出後放著等計時到期（等不到就記「到期未觀察」） |
| 預期畫面 | (1) 娃娃與召喚特效正常出現；(2) 血盟視窗與召喚前相同，沒有多出或改掉任何內容；(3) 再次使用或到期時娃娃消失；狀態列**沒有**娃娃圖示（刻意不送） |
| 預期封包或伺服器 log | console `[LiTo] magic-doll-icon suppressed object=… reason=no-native-receiver(…)`（`S/ClientSession.java:20298-20313`）；capture 裡沒有 opcode `0xFA` 的 S→C 幀 |
| SQL 前後查詢 | 無 |
| 截圖檔名 | `V08-doll-summon.png`、`V08-clan-ui.png`、`V08-doll-recall.png` |
| 失敗時記什麼 | 血盟畫面有變化時前後各一張；capture 出現 `0xFA` 時把該幀剪到 `frames-V08.jsonl`；客戶端崩潰記 probe 退出碼 |

<a id="v09"></a>
#### V09 施法時的正義值封包

來源：`R/issue-325.md:49`；`R/issue-307.md:42`（47 弱化術）。需要：E2（法師，已學 10、28、47）；目標 B。

| 欄 | 內容 |
|---|---|
| 輸入 | A 對 B 依序施放 10（寒冷戰慄）、28（吸血鬼之吻）、47（弱化術），每個之間等冷卻 |
| 預期畫面 | 每次施放後 A 的正義值顯示變動（47 是 −10，`R/issue-307.md:42`；其餘的數值以 `S/BattleSpellCatalog.java` 各技能的 `lawfulDelta` 為準，本檔沒有逐一抄） |
| 預期封包或伺服器 log | C→S `C_USE_SPELL` 開頭 `140101`（10）、`140303`（28）、`140506`（47）。S→C 順序：…→ `S_CHANGE_ALIGNMENT`（來源報告寫 `0x59`）→ `S_MANA_POINT 0x4D`（`R/issue-325.md:49`） |
| SQL 前後查詢 | `SELECT Lawful FROM p24_accounts_lito_exp.characters WHERE char_name='<A>';` 前後各一次 → `sql-before-V09.txt`、`sql-after-V09.txt`。預期**相同**：角色庫唯讀，正義值只在這一場的記憶體裡變 |
| 截圖檔名 | `V09-lawful-10.png`、`V09-lawful-28.png`、`V09-lawful-47.png` |
| 失敗時記什麼 | 每個技能：有沒有 `S_CHANGE_ALIGNMENT`、它在 `S_MANA_POINT` 之前還是之後、封包內的新正義值、畫面數字。SQL 的 `Lawful` 若變了要特別標出（表示寫回了來源庫） |

<a id="v10"></a>
#### V10 吸血（10、28）與吸魔（39）

來源：`R/issue-336.md:44-45`。需要：E2（已學 10、28、39）；A 先被打掉一些 HP；B 是有 MP 的角色。

| 欄 | 內容 |
|---|---|
| 輸入 | ① A（HP 未滿）對 B 放 28，再放 10。② A 補滿 HP 後再放一次 28。③ A 對 B 放 39（魔力奪取）；之後讓 B 把 MP 用到剩個位數，再放一次 39 |
| 預期畫面 | ① A 回復的 HP 等於 B 受到的傷害。② A 滿血時 HP 不超過上限。③ B 的 MP 減少、A 的 MP 增加同樣的量；B 的 MP 不夠時 A 只拿到 B 剩下的量 |
| 預期封包或伺服器 log | `C_USE_SPELL` 開頭 `140303`（28）、`140101`（10）、`140406`（39）。①② A 收 `S_HIT_POINT 0x0D`（增加）、B 的 HP 減少。③ 雙方各收 `S_MANA_POINT 0x4D`：B 減、A 加 |
| SQL 前後查詢 | 無 |
| 截圖檔名 | `V10-drain-hp.png`、`V10-drain-hp-full.png`、`V10-drain-mp.png` |
| 失敗時記什麼 | 每次：B 的傷害、A 的回復量、兩者是否相等；39 的兩個 MP 變動量；A 的 MP 是否扣了施法費用後再加（把兩筆 `S_MANA_POINT` 的值都抄下） |

<a id="v11"></a>
#### V11 火牢（58）每秒一跳

來源：`R/issue-336.md:46`。需要：E2（已學 58）；一件加 INT 的裝備（沒有時只做前半）。來源另寫 WORLD，做不到（V38）。

| 欄 | 內容 |
|---|---|
| 輸入 | A 在 B 腳下放 58，B 站著不動 10 秒，記每一跳的傷害。A 穿上加 INT 的裝備後重做一次 |
| 預期畫面 | B 每秒掉一次 HP；穿上加 INT 裝備後，每跳傷害的最大值變高 |
| 預期封包或伺服器 log | `C_USE_SPELL` 開頭 `140701`。之後每秒一筆 B 的 HP 變動；封包種類以 capture 為準 |
| SQL 前後查詢 | 無 |
| 截圖檔名 | `V11-firewall.png` |
| 失敗時記什麼 | 兩次各 10 跳的時間戳與傷害值；跳的間隔不是 1 秒、或有一秒跳兩次，把 capture 剪到 `frames-V11.jsonl` |

<a id="v12"></a>
#### V12 魔法相消術（44）清掉哪些 BUFF

來源：`R/issue-336.md:48`。需要：E2（`M4A4` 已學 44）；E3（目標是 GM 帳號的法師 `M4GM`，已學 3、12、21、26、43）。下表的 B 在這一項指 `M4GM`。

| 欄 | 內容 |
|---|---|
| 輸入 | B 先對自己放 3、12、21、26、43（五個圖示都出現後截圖）；A 對 B 放 44 |
| 預期畫面 | B 身上 **12、21 還在**；**3、26、43 消失**（來源的完整清單：留 12／21／78／79；消 8／48、3／26／42、43／54。這裡只驗 seed 得到的五個） |
| 預期封包或伺服器 log | `C_USE_SPELL` 開頭 `140503`。被消掉的 BUFF 各有對應的狀態清除封包；種類沒有查證，以 capture 與圖示為準 |
| SQL 前後查詢 | 無 |
| 截圖檔名 | `V12-before-cancel.png`、`V12-after-cancel.png`（都要拍到 B 的狀態列） |
| 失敗時記什麼 | 五個 BUFF 各自「留／消」的實際結果；44 是機率技能，沒中時重放並記次數 |

<a id="v13"></a>
#### V13 集體緩速術（76）對已加速的目標

來源：`R/issue-336.md:47`。需要：E2（A 已學 76）；E8（B 有加速裝備或強化勇水）；第三個在場目標當對照最好（C 或 ArenaBot），沒有就只看 B。

| 欄 | 內容 |
|---|---|
| 輸入 | B 穿上加速裝備（或喝強化勇水）。A 以 B 為目標放 76，重複 5 次 |
| 預期畫面 | B **不會**被緩速，原本的加速也不被抵銷；5 次都一樣（不是機率沒中） |
| 預期封包或伺服器 log | `C_USE_SPELL` 開頭 `140903`；A 扣 MP；沒有給 B 的緩速狀態封包 |
| SQL 前後查詢 | 無 |
| 截圖檔名 | `V13-slow-immune.png` |
| 失敗時記什麼 | B 被緩速的次數、B 身上的加速來源（哪件裝備或哪種藥水）。對照組：B 脫掉裝備後再放 5 次，記被緩速幾次 |

<a id="v14"></a>
#### V14 暈眩與冰凍的時序（G10–G12：87、50、80）

來源：`R/issue-356.md:48`（結論表 `:10-12`）。需要：E4（騎士 87＋近戰武器）；E2（法師 50、80）。記錄型：來源只有「狀態要等 HP 入帳之後才上」，候選收據的競態次數實機無法數。

| 欄 | 內容 |
|---|---|
| 輸入 | ① 騎士對 B 放 87（衝擊之暈），做 5 次。② 法師對 B 放 50（冰矛圍籬，MP 100）5 次。③ 法師對 B 放 80（冰雪颶風）5 次 |
| 預期畫面 | 命中時 B 先掉 HP、再出現暈眩／冰凍；不會出現「已經暈了但 HP 沒扣」或「HP 扣了兩次」；沒命中時不暈、不凍 |
| 預期封包或伺服器 log | `C_USE_SPELL` 開頭 `140a06`（87）、`140601`（50）、`140907`（80）。同一次施放裡，B 的 HP 封包在暈眩／冰凍狀態封包（`S_PARALYSE 0x25` 類）**之前**；以 capture 的順序為準 |
| SQL 前後查詢 | 無 |
| 截圖檔名 | `V14-shock.png`、`V14-icelance.png`、`V14-blizzard.png` |
| 失敗時記什麼 | 每次施放把該秒的 S→C 幀依序抄出（opcode 與 note）；順序相反或狀態沒有對應的 HP 幀時，剪到 `frames-V14-<技能>.jsonl` |

---

### C2：目標判定、邊界、受控 NPC、觀戰

<a id="v15"></a>
#### V15 單體／範圍／自施的目標判定（G03–G08 與 #321）

來源：`R/issue-305.md:53`（逐項結論 `:32-37`）；`R/issue-321.md:53-56`。需要：E1（第三人當「旁人」）、E2、E5、E6；Local Lobby 死鬥三人同房。「打不到隊友」那一列要 D13 的 2 對 2。

共同的三欄：SQL 前後查詢＝無。截圖檔名＝`V15-<序>-<技能>.png`。失敗時記什麼＝施法者／主目標／旁人三方各自有沒有 HP 或狀態變化、A 的 MP 有沒有扣、`C_USE_SPELL` 的目標 id，並分清「客戶端選不到」與「伺服器沒反應」。

| 序 | 輸入 | 預期畫面 | 預期封包或伺服器 log |
|---|---|---|---|
| 1 | 妖精對 B 放 167（風之枷鎖），C 貼著 B 站 | 只有 B 受影響，C 沒事 | 開頭 `141406`；只有一個目標的狀態封包 |
| 2 | 龍騎士對 B 放 194，C 站在施法者與 B 的直線上 | 只有 B 受影響 | 開頭 `141801` |
| 3 | 法師以**自己**為目標放 76，B、C 在 3 格內 | 扣 MP、有施法動作；B、C **不會**被緩速（`R/issue-321.md:53`） | 開頭 `140903`，目標 id 是 A 自己；沒有給 B、C 的緩速封包 |
| 4 | 法師以 B 為目標放 76，C 在 B 的 3 格內 | B 與 C 走正常機率被緩速；A 自己不受影響（`R/issue-321.md:54`） | 同上，目標 id 是 B |
| 5 | 法師對自己放 29；妖精對自己放 152、153 | 三個都能對自己施放（走正常機率） | 開頭 `140304`、`141207`、`141300`，目標 id 是自己 |
| 6 | 法師對 B 放 17（極光雷電），C 在 B 的 3 格內、另一次站到 4 格外 | 3 格內的 C 受傷；4 格外不受傷 | 開頭 `140200` |
| 7 | 妖精對 B 放 148、149；再對一隻召喚物／寵物放（有 E9 時） | 對玩家有效；對召喚物／寵物無效 | 開頭 `141203`、`141204` |
| 8 | 法師在 3 格處對 B 放 40；黑暗妖精在同格對 B 放 103，再退到 1～3 格放 103 | 40：3 格內可中。103：只有同格才生效；1～3 格時扣 MP、有動作、出現失敗訊息（`R/issue-321.md:55`） | 開頭 `140407`、`140c06` |
| 9 | 2 對 2 死鬥：法師放龍捲風、暴風雪，範圍內同時有敵人與隊友 | 敵人受傷、隊友不受傷（`R/issue-321.md:56`） | 只有敵方的 HP 封包 |

第 3 列也要以「自己的寵物」為主目標做一次（有 E9 時；`R/issue-321.md:53`）。來源寫的 WORLD 相位做不到（V38）。

<a id="v16"></a>
#### V16 G09：畫面邊界第 19 格

來源：`R/issue-356.md:50`（差異說明 `:9,38,42`）。需要：E2（29、44、76）；E11（GM 用 `.loc`／`.move`）；A 是施法者、B 是目標。

| 欄 | 內容 |
|---|---|
| 輸入 | 用 `.loc` 讀 A、B 座標，用 `.move` 把 B 放到相對 A 的 (19,0)；A 嘗試點 B 放 29。同樣做 (0,19)、(18,−1)、(−1,18)。再做一格對照：(18,0)。有時間再用 44、76 各做 (19,0) 一次 |
| 預期畫面 | 〔待驗證〕四個邊界格：若客戶端看得到並點得到 B，施法成立（走正常機率）。來源的推測是客戶端可能根本看不到這四格上的目標（`R/issue-356.md:42`）。對照格 (18,0) 一定點得到 |
| 預期封包或伺服器 log | 有送出時：`C_USE_SPELL` 開頭 `140304`，目標 id 是 B；伺服器接受（A 扣 MP、有施法動作）。console 不應有該次的 `rejected` 行 |
| SQL 前後查詢 | 無 |
| 截圖檔名 | `V16-edge-19-0.png`、`V16-edge-0-19.png`、`V16-edge-18-m1.png`、`V16-edge-m1-18.png`、`V16-edge-18-0.png` |
| 失敗時記什麼 | 每格記三件事：畫面上看不看得到 B、點不點得到（有沒有 C→S 幀）、伺服器有沒有接受。「看不到所以沒送」不算失敗，照實記；「送了但被拒」才是失敗，附 `.loc` 的雙方座標與 console 行 |

<a id="v17"></a>
#### V17 受控 NPC 當主目標時退役（G14：76、167）

來源：`R/issue-354.md:57`（`:56` 的真實 `capture()` 路徑同一項）。需要：E9（B 帶一隻寵物或召喚物）、E2（A 已學 76）、E5（妖精 167）；E1 的 C 可以幫忙打寵物。記錄型：退役要剛好落在施法的鎖等待窗內，手動很難對準。

| 欄 | 內容 |
|---|---|
| 輸入 | B 帶出寵物／召喚物。A 以該寵物為主目標放 76（或 167），**同一瞬間** B 把寵物收回（或 C 把寵物打死）。做 10 次，記每次的結果 |
| 預期畫面 | 寵物在施法生效前消失的那幾次：施法放棄、A **不扣 MP**、旁邊的人不受影響。寵物還在的那幾次：正常生效 |
| 預期封包或伺服器 log | 放棄的那幾次：有 `C_USE_SPELL`（目標 id 是寵物的 object id），之後**沒有** `S_MANA_POINT 0x4D`、沒有狀態封包；console 沒有例外，沒有 `invalid companion NPC damage input` |
| SQL 前後查詢 | 無 |
| 截圖檔名 | `V17-pet-retire-76.png`、`V17-pet-retire-167.png` |
| 失敗時記什麼 | 10 次裡「扣了 MP 但沒有任何效果」「對已消失的寵物送出狀態封包」「console 例外」各幾次；每次的 MP 前後值。10 次都沒有撞到退役的時機時填「未重現時機」，不算通過 |

<a id="v40"></a>
#### V40 寵物／召喚物自然回血回魔與主人 HP 條（T135，#386）

來源：`R/issue-386.md`「待驗證」；設計 `docs/design/controlled-npc.md`「G15 前兩組移植設計」。這一項在 T129 之後才加，編號接在 V39 後面，排在 C2、V17 之前做。需要：E9（B 帶一隻寵物或召喚物；E9 還缺時填「阻塞：資料（E9）」）；A 負責打寵物。

開始前先在實驗靜態庫查這隻 NPC 的樣板，換算預期節奏（`hprinterval`／`mprinterval` 是毫秒，除以 1000 取整數秒；結果 ≤0 時是 20 秒；`hpr`／`mpr` ≤0 時是 1 點）：

```sql
SELECT npcid, name, hprinterval, hpr, mprinterval, mpr FROM npc WHERE npcid = <寵物或召喚物的 npc 編號>;
```

| 欄 | 內容 |
|---|---|
| 輸入 | ① B 帶出寵物／召喚物，A 把牠打到大約半血後停手，B 不補血、不下指令，兩人站著等 60 秒，用碼表記 B 畫面上寵物 HP 條每次往上跳的時刻。② A 把寵物打死，再等 30 秒。③（召喚物）B 重新召喚一隻，A 打到半血後 B 立刻解散牠，等 30 秒 |
| 預期畫面 | ① B 畫面上寵物的 HP 條每隔「HP 間隔」秒上升一次，幅度是「HP 回復量 ÷ 最大 HP」；回滿後不再變動。A 的畫面不會因為這個回復多出任何東西。② 寵物死亡後 HP 條停在 0，不會自己爬回來。③ 解散後不再出現那隻召喚物的任何更新 |
| 預期封包或伺服器 log | ① B 的 capture：note 含 `Source companion recovery` 的 `S_HIT_RATIO 0x68`，`plain[1..4]` 是寵物的 object id，`plain[5]` ＝ `100 × 目前 HP ÷ 最大 HP`（無條件捨去）；相鄰兩幀的時間差是 HP 間隔的整數倍。A 的 capture **沒有**這個 note 的幀。②③ 死亡／解散之後 B 的 capture 也不再有這個 note 的幀。整輪 console 沒有例外 |
| SQL 前後查詢 | 上面那一條（只查一次，用來算預期） |
| 截圖檔名 | `V40-pet-regen-before.png`、`V40-pet-regen-after-60s.png` |
| 失敗時記什麼 | 樣板四個欄位的值；每次 HP 條跳動的時刻與 `plain[5]`；60 秒內完全沒有跳動時，記寵物的 npc 編號、是寵物還是召喚物、console 啟動時 `Source NPC templates=` 那一行的數字（為 0 表示樣板沒有載入，回復不會啟動）。MP 回復在畫面上看不到，只在有寵物狀態視窗時順手記 |

`.loc` 之類的 GM 指令不需要。注意：寵物**進化之後**到重新召喚之前不會回復（`controlled-npc.md`「還缺什麼」4），不算失敗；召喚物**不會放自己的技能**也是已知缺口（同節 1），不要記成失敗。

<a id="v18"></a>
#### V18 觀戰者中途離場

來源：`R/issue-366.md:26-27,38`；契約 `docs/reference/battle-observer-exit-contract.md:8-28`。需要：E1（A、B 參戰，C 觀戰）；房間要允許觀戰——Local Lobby 允許（`gameplay.md:226`）；自建房是否允許沒有查證。

| 欄 | 內容 |
|---|---|
| 輸入 | A、B 在 Local Lobby 開戰。C 以觀戰身分進同一房（已開戰的房只有觀戰者能進，`gameplay.md:188`）。C 按退出。A、B 繼續打到分出勝負。C 再次以觀戰身分進同一房（要在 A、B 打完之前，所以 A、B 先不要下重手） |
| 預期畫面 | C 回到大廳的房間列表，沒有結算畫面。A、B 的畫面**沒有**結算、可以繼續移動與攻擊；觀戰名單少一人。A、B 打完後正常出結算。C 第二次能進去觀戰 |
| 預期封包或伺服器 log | C：`C_LITO_EXIT_ROOM 0xD0` → 自己的 `S_LITO_EXIT_ROOM 0xD0`（房號＋C 的顯示名）→ `S_LITO_ROOM_INFO 0xCC`。若客戶端送的是 `C_LITO_QUIT_FIGHT 0xD9`：先收 `S_LITO_QUIT_FIGHT 0xF0`（值 0）再收 `S_LITO_EXIT_ROOM`。A、B 各只收到一筆 C 的 `S_LITO_EXIT_ROOM`，**沒有** `S_LITO_END_GAME 0xD3`。console 沒有 `battle disconnect pending`、沒有 `battle result` |
| SQL 前後查詢 | Q1 前後 → `sql-before-V18.txt`、`sql-after-V18.txt`：C 沒有任何變化、沒有 C 的收據（測試房本來也不記戰績） |
| 截圖檔名 | `V18-observer-in.png`、`V18-observer-left-lobby.png`、`V18-combatants-continue.png`、`V18-observer-rejoin.png` |
| 失敗時記什麼 | C 離場時 A、B 有沒有被結算（最重要）、C 卡在哪個畫面、C 實際送的是 `0xD0` 還是 `0xD9`、第二次進房被拒的訊息。客戶端沒有觀戰入口或退出鈕時填「阻塞：客戶端沒有入口」並截圖 |

---

### C3：PvE 與逾時

<a id="v23"></a>
#### V23 Quest 怪物技能：歐吉、Boss 頻率、死亡騎士

來源：`R/issue-353.md:32`（修正內容 `:10-12`）；怪物清單 `docs/design/skills-npc.md:27-28,61,64-65`。需要：A、B 各一隊（隊伍模式兩隊都要有真人，`gameplay.md:56`）；模式 Quest Battle；耐打的角色（等級 52）。哪一張 Quest 地圖、哪一個怪物房有歐吉（45278）與死亡騎士（45601）沒有查到對照表，從第一張地圖開始找，找到後把地圖編號記進結果檔。後兩項是記錄型。

| 欄 | 內容 |
|---|---|
| 輸入 | 房主把模式切到 Quest Battle 開戰。① 讓歐吉打 A，看到它放技能（前方 1×2 的重擊）至少 3 次。② A 貼著死亡騎士站 60 秒，數它「放技能」與「普通攻擊」各幾次。③ 看到死亡騎士放 10056（自身 4 格的火屬性範圍技）後，注意接下來 3 秒 |
| 預期畫面 | ① 歐吉放技能時播的是另一個動作（action 30），不是普通攻擊的動作。② 貼身時普通攻擊約占一半以上（先擲 50% 決定普攻，剩下才擲技能）。③ 10056 之後 3 秒內不會放吸血鬼之吻（28） |
| 預期封包或伺服器 log | ① 歐吉的 `S_ATTACK 0x23`／`S_ACTION 0x20` 幀，動作欄是 30（`0x1e`）。③ 10056 與下一次 28 的兩幀時間差 ≥ 3 秒。確切的封包欄位位置沒有查證，以 note 欄與時間戳為準 |
| SQL 前後查詢 | 無（Quest 中途離開的人會被記一敗，同 G23 的註記；用 Local Lobby 以外的房時在結果檔註明） |
| 截圖檔名 | `V23-ogre-action30.png`、`V23-dk-melee.png`、`V23-dk-10056.png` |
| 失敗時記什麼 | ① 歐吉技能幀的動作值。② 60 秒內技能次數／普攻次數。③ 10056 之後第一個 28 的時間差（毫秒）。找不到這兩種怪時填「阻塞：找不到怪」並列出試過的地圖 |

<a id="v24"></a>
#### V24 死鬥 180 秒逾時＝平手

來源：`R/issue-310.md:62`。需要：A、B；**自建房**死鬥（要看戰績不變）。

| 欄 | 內容 |
|---|---|
| 輸入 | Q1 → `sql-before-V24.txt`。開戰後雙方不攻擊，等 180 秒 |
| 預期畫面 | 兩人的結果畫面都顯示**平手**；回大廳後戰績、積分與開戰前相同 |
| 預期封包或伺服器 log | `S_LITO_END_GAME 0xD3` 的勝方欄是 −1（沒有勝方，`S/Protocol.java:2258-2262`）。console **沒有** `battle result player=` 兩行；逾時結束時印哪一行沒有查證，照實抄下 |
| SQL 前後查詢 | Q1 → `sql-after-V24.txt`：與前一份完全相同，沒有新收據 |
| 截圖檔名 | `V24-draw-a.png`、`V24-draw-b.png` |
| 失敗時記什麼 | 畫面顯示誰勝、SQL 哪一欄變了、有沒有多出收據 |

<a id="v25"></a>
#### V25 寵物競賽 300 秒沒人跑完

來源：`R/issue-310.md:64`。需要：A、B；模式寵物競賽（最少 2 人，`gameplay.md:47`）；自建房。

| 欄 | 內容 |
|---|---|
| 輸入 | Q1 → `sql-before-V25.txt`。開賽後兩人都不跑，等 300 秒 |
| 預期畫面 | 結果是平手，沒有人記勝或敗 |
| 預期封包或伺服器 log | 同 V24 |
| SQL 前後查詢 | Q1 → `sql-after-V25.txt`：完全相同 |
| 截圖檔名 | `V25-petrace-timeout.png` |
| 失敗時記什麼 | 同 V24。模式切不到寵物競賽或開不了賽（自建房固定死鬥＋雙方人數一致，`S/ClientSession.java:10371-10375`；能不能改模式沒有查證）→ 填「阻塞：開不了寵物競賽」並記系統訊息 |

<a id="v26"></a>
#### V26 攻城逾時＝守方勝

來源：`R/issue-310.md:63`。需要：攻城每隊要 15 人（`gameplay.md:50,104`），只有實驗設定 `native.siege.roster.enforced=false` 時才可能用 2 人開〔待驗證〕；要等 1200 秒。

| 欄 | 內容 |
|---|---|
| 輸入 | 先確認實驗設定的 `native.siege.roster.enforced`。是 `false`：Q1 → `sql-before-V26.txt`，A、B 分兩隊開攻城，攻方不進攻，等 1200 秒。是 `true`：不做 |
| 預期畫面 | 守方那一隊記勝、攻方記敗 |
| 預期封包或伺服器 log | `S_LITO_END_GAME 0xD3` 的勝方是守方隊伍；console 兩行 `battle result` |
| SQL 前後查詢 | Q1 → `sql-after-V26.txt`：守方勝列（+250）、攻方敗列（+50） |
| 截圖檔名 | `V26-siege-timeout.png` |
| 失敗時記什麼 | 哪一隊是攻方（每場開戰時決定，`gameplay.md:106`）、結果、SQL。開不了戰 → 「阻塞：攻城人數」，解除要 30 個帳號或主控接受以 CI 的 `GameplayDecisionRulesTest` 為準 |

<a id="v27"></a>
#### V27 Time Attack 中有人斷線

來源：`R/issue-355.md:103`（壓力測試沒有涵蓋的模式）；規則 `gameplay.md:122`。需要：A（房主＝隊長）、B；模式 Time Attack；自建房。

| 欄 | 內容 |
|---|---|
| 輸入 | Q1 → `sql-before-V27.txt`。開戰後對 B 的 `LiTo.bin` PID 執行 `Stop-Process -Id <PID> -Force`。A 繼續打或等到結束 |
| 預期畫面 | A 的比賽**繼續**，不會因為 B 斷線就結束 |
| 預期封包或伺服器 log | `battle disconnect pending … graceMs=300` → `battle disconnect resolved … reason=…`；A 沒有馬上收到 `S_LITO_END_GAME` |
| SQL 前後查詢 | Q1 → `sql-after-V27.txt`：B 是斷線敗（`losses+1`、`disconnects+1`、積分 −50 最低 0）；A 依實際結果（打贏＝勝、隊長陣亡＝敗、逾時＝不變） |
| 截圖檔名 | `V27-ta-disconnect.png` |
| 失敗時記什麼 | A 是否被強制結束、B 被記成什麼、A 的結果與畫面是否一致。Time Attack 開不了 → 「阻塞」，同 G23 的註記 |

<a id="v39"></a>
#### V39 怪物魔法：反制魔法、安息喊話、多人換目標（T132，#383）

來源：`R/issue-383.md`「待驗證」；設計 `docs/design/skills-npc.md` §6。這一項在 T129 之後才加，編號接在 V38 後面，但排在 C3 這一輪做。三個小項各自獨立，做得到哪一項就記哪一項。

需要：
- ①：一個學了技能 31（魔法屏障，即反制魔法；MP 20、材料魔法寶石 40318 ×1、持續 16 秒，`1-伺服器` `db/380_db.sql:48056`）的法師。E2 的 `M4A4` 有 40318 但**沒有技能 31** → 沒有補 seed 前填「阻塞：資料（技能 31）」。場地：Quest Battle 的死亡騎士（45601，會放 28 吸血鬼之吻與 10056），或 Time Attack 的 Boss。
- ②③：Time Attack 打到最後出現龍（安塔瑞斯 45682／法利昂 45683／巴拉卡斯 45684）。龍是否出現由開場決定，不一定每場都有；沒有出現就重開或填「阻塞：沒有龍」。③ 另要 A、B 兩人同場。

| 欄 | 內容 |
|---|---|
| 輸入 | ① 法師在怪物面前施放技能 31，站在怪物魔法打得到的距離，等它放魔法；被擋一次後不再補，等下一次魔法。② 貼近龍（3 格內），等它放安息（單體、傷害 1500 以上的那一招，機率低，可能要等幾分鐘）；有技能 31 的話先開著。③ A、B 都先打龍幾下，然後 A 貼著龍、B 站在 3–4 格外，觀察 60 秒 |
| 預期畫面 | ① 第一次魔法：法師沒有扣血，身上出現反制魔法被打掉的特效，狀態結束；下一次魔法照常扣血。② 龍放安息時頭上出現「安塔瑞斯: 給你安息」（法利昂／巴拉卡斯同理）；即使開著反制魔法，安息照樣扣血，反制魔法**不會**被消耗。③ 龍的噴吐／甩尾有時打向 B（不是離牠最近的 A）；只有一人在場時永遠打那一人 |
| 預期封包或伺服器 log | ① console `[LiTo] Counter Magic consumed target=<角色> source-skill=<28／10056／31001 等> … damage=0 status=blocked`；capture 有 note 含 `counter-magic-consumed` 的 `S_EFFECT 0x37`。② capture 有 note 含 `mobskill-rest-shout` 的 `S_SAY_CODE 0x2D`，字串是 `$1116: $3717`（法利昂 `$1426`、巴拉卡斯 `$1605`）；console **沒有** `Counter Magic consumed … source-skill=10027`（或 10028／10029）。③ console `[LiTo] Time Attack monster magic npc=4568x … target=<object id>`／`monster visual skill … target=` 的 target 有時是 B |
| SQL 前後查詢 | 無（Quest／Time Attack 中途離開會被記一敗，同 G23 的註記） |
| 截圖檔名 | `V39-counter-magic-npc.png`、`V39-rest-shout.png`、`V39-change-target.png` |
| 失敗時記什麼 | ① 有沒有扣血、扣多少、反制魔法圖示是否還在、console 那一行。② 有沒有出現文字；出現的是原樣的 `$3717` 還是中文；`0x2D` 幀的 `plain`。③ 60 秒內龍的各招各打誰幾次（記錄型，不判通過／失敗）。做不到的小項填阻塞原因 |

---

### C4：斷線與重啟復原（最後做）

[G24](#g24) 第 1–3 列已涵蓋「一方關視窗／強制結束 → 另一方勝 +250、斷線方 −50」（`R/issue-355.md:97,122` 的①），這裡不重複。真正的拔網路線做不到，理由同 G24（loopback-only）。

<a id="v19"></a>
#### V19 雙方 300 毫秒內同時斷線＝無效場

來源：`R/issue-355.md:122`（②）、`R/issue-301.md:27`；規則 `gameplay.md:238-239`；畫面部分 `R/issue-355.md:104`。需要：A、B；自建房死鬥。

| 欄 | 內容 |
|---|---|
| 輸入 | Q1 → `sql-before-V19.txt`。開戰約 10 秒後，用**同一條命令**結束兩個自己啟動的客戶端：`Stop-Process -Id <A 的 PID>,<B 的 PID> -Force` |
| 預期畫面 | 兩個視窗都消失。之後各自重新登入能正常進大廳，不會被帶回舊場次 |
| 預期封包或伺服器 log | 兩行 `battle disconnect pending room=… graceMs=300`（`S/ClientSession.java:47547-47549`），時間差 < 300 毫秒；一行 `battle disconnect resolved room=… reason=…`（`S/GameHub.java:12974`）；**沒有** `battle result player=`。房間被移除 |
| SQL 前後查詢 | Q1 → `sql-after-V19.txt`：A、B 所有欄位與前一份相同，沒有新收據。`runtime/battle-settlements/` 沒有 `.pending` |
| 截圖檔名 | `V19-both-drop-console.png`、`V19-relogin-lobby.png` |
| 失敗時記什麼 | 兩行 `pending` 的時間差。差 ≥ 300 毫秒時這一次不算（會變成「先斷的敗、後斷的勝」，那是正確行為），重做，最多 5 次，仍對不準就填「未重現時機」並附每次的時間差與 SQL。差 < 300 毫秒卻有人被記勝敗 → 失敗，附兩行 log 與收據列 |

<a id="v20"></a>
#### V20 A 建房的同時 B 斷線

來源：`R/issue-355.md:122`（④；缺陷說明 `:26-28`）。需要：A、B 都在大廳，B 不在任何房間。記錄型的成分：要對準的窗口很短。

| 欄 | 內容 |
|---|---|
| 輸入 | A 把建房視窗填好、游標停在確認鈕上。對 B 的 PID 執行 `Stop-Process -Force` 的同時 A 按下確認。做 5 次（每次 B 要重新登入） |
| 預期畫面 | A 每次都正常進入自己建的房間 |
| 預期封包或伺服器 log | A：`C_LITO_CREATE_ROOM 0xCC` → `S_LITO_JOIN_RESULT 0xCE` 成功；console `[LiTo] C_LITO_CREATE_ROOM name=…`（`S/ClientSession.java:10377`）。沒有 `unknown room` |
| SQL 前後查詢 | 無（開戰前斷線不算戰鬥斷線，`gameplay.md:244`）；抽查 Q1 沒有變化 |
| 截圖檔名 | `V20-create-while-drop.png` |
| 失敗時記什麼 | A 建房失敗的訊息、console 有沒有 `unknown room`、兩個動作的時間差；伺服器卡住時先 `jstack` |

<a id="v21"></a>
#### V21 對戰中強制結束實驗伺服器，重啟後核對

來源：`R/issue-355.md:98,122`（③；CI 用 JDBC 層模擬的十個中止點在 `:43-54`）。需要：A、B；自建房死鬥。**只能結束自己在 A01 啟動的實驗 Java 程序**，動手前先核對 PID 的命令列含 `scratch\m4\run-`、監聽埠是 42083。

| 欄 | 內容 |
|---|---|
| 輸入 | ① Q1 → `sql-before-V21.txt`；列 `runtime/battle-settlements/`。② 開戰，A 把 B 打到快死。③ A 打出最後一下的同時，對實驗 Java PID 執行 `taskkill /F /PID <PID>`。④ 列 `runtime/battle-settlements/`（看有沒有 `.pending`／`.tmp`）；Q1 → `sql-mid-V21.txt`。⑤ 用**同一個** `runtime/` 重做 A01 啟動。⑥ Q1 → `sql-after-V21.txt`。⑦ 再正常關閉、再啟動一次，Q1 → `sql-after2-V21.txt` |
| 預期畫面 | 兩個客戶端斷線。重啟後 A、B 能重新登入進大廳 |
| 預期封包或伺服器 log | 重啟的 console 走到 `[LiTo] ready`；沒有 `settlement journal already owned`（`S/SettlementJournal.java:18-19`；出現表示舊程序還活著）、沒有 `Settlement character identity changed` |
| SQL 前後查詢 | 那一場**要嘛完全沒記、要嘛剛好記一次**：(a) 沒記——`sql-after` 與 `sql-before` 相同、沒有新收據；或 (b) 記一次——勝者 +1 勝 +250、敗者 +1 敗 +50、雙方 `version` 只 +1、收據兩列 `applied=1`、`last_result_id` 是這一場。`sql-after2` 與 `sql-after` 相同（再重啟不會多加）。重啟後 `runtime/battle-settlements/` 沒有 `.pending`（`.tmp` 殘檔可留，`R/issue-355.md:120`） |
| 截圖檔名 | `V21-kill-server.png`、`V21-restart-ready.png`、`V21-sql.png` |
| 失敗時記什麼 | 三份（或四份）SQL 的逐欄差異、每個時點的 WAL 目錄清單、收據列（含 `applied`）、重啟 console 全文。一人記了、另一人沒記；`version` 加了 2；有 `applied=0` 留著——都是失敗。taskkill 落在哪個時間點無法控制，結果是 (a) 或 (b) 都算通過，在結果檔寫明是哪一種；有時間就多做幾次以碰到不同的中止點 |

<a id="v22"></a>
#### V22 跨月第一場的月計歸零

來源：`R/issue-335.md:57`；規則 `db/lito_character_ranking.sql:17-20`。需要：E12（A 的 `monthly_period` 是上個月、月計不是 0）；自建房死鬥。沒有 E12 → 阻塞。

| 欄 | 內容 |
|---|---|
| 輸入 | Q1 → `sql-before-V22.txt`（確認 A 的 `monthly_period` 是上個月）。A、B 打一場，A 勝 |
| 預期畫面 | 大廳的月榜／月積分（若畫面有顯示）是本場的數字，不是累加上個月的 |
| 預期封包或伺服器 log | `battle result player=<A> won=true …` |
| SQL 前後查詢 | Q1 → `sql-after-V22.txt`。A：`monthly_period`＝當月 `YYYYMM`（日界是台北時間 05:00）、`monthly_ranking_points`＝250、`monthly_wins`＝1、`monthly_losses`＝0；`ranking_points`＝前值＋250（總積分不歸零）。B 的 `monthly_period` 本來就是當月時照一般敗列累加 |
| 截圖檔名 | `V22-monthly-rollover.png` |
| 失敗時記什麼 | 月計三欄的前後值、`monthly_period` 前後值、執行當下的台北時間（是否在 05:00 前後） |

---

### C5：選做與記錄型

共同的欄：SQL 前後查詢＝無（V30 除外，見該列）。這一組的來源報告都沒有單次可判定的預期，公式本身已由 CI 的參數化測試鎖定；實機只補「畫面與封包有沒有出來」。

| 序 | 來源 | 需要 | 輸入 | 預期畫面 | 預期封包或伺服器 log | 截圖檔名 | 失敗時記什麼 |
|---|---|---|---|---|---|---|---|
| <a id="v28"></a>V28 | `R/issue-308.md:43` | E14(a)：帳號 D 的妖精 `M4D1`（`ElfAttr`＝0）學了風屬性的 167 | `M4D1` 對任一目標放 167 三次，之後立刻做別的動作（移動、普通攻擊） | 屬性不符：沒有效果。重點是客戶端**不會卡在施法動作**，之後照常能移動與攻擊 | 有 `C_USE_SPELL`，之後伺服器不送任何封包（LiTo 與 1-伺服器 都不送）；不扣 MP | `V28-elf-attr-mismatch.png` | 客戶端是否卡住（不能移動、不能再施法）、要怎樣才恢復 |
| <a id="v29"></a>V29 | `R/issue-338.md:36`；`R/issue-304.md:44` | E14（魔眼）；法師 | 記下 SP 面板數字 → 使用魔眼 → 再看面板；前後各對 B 放 10 次光箭記傷害 | SP 面板**不變**（不再多 1）；傷害平均略高（記錄型） | 魔眼使用的 `C_USE_ITEM`；`S_ATTACK` 的傷害值 | `V29-eye-sp-before.png`、`V29-eye-sp-after.png` | 面板前後值、兩組各 10 次傷害 |
| <a id="v30"></a>V30 | `R/issue-339.md:90-91`；`R/issue-318.md:67` | E14（已裝備幻象之劍／弓的角色）；Quest 裡的不死系怪（死亡騎士召喚的史巴托 45161，是否屬不死系沒有查證） | 帶幻象武器的角色登入並進戰鬥；對不死系怪打 20 下。另用一般武器、祝福武器各打 20 下對照（有的話） | 能正常進場；幻象武器與一般武器沒有額外傷害，祝福武器才有 | console 沒有 `invalid source weapon combat traits`（V02）。SQL：`SELECT item_id, bless, is_equipped FROM …character_items WHERE char_id=<objid>` 存 `sql-before-V30.txt` | `V30-illusion-weapon.png` | 進場失敗的 console、三組各 20 下的傷害。來源另寫「租借武器 84 號」，要租裝 → 阻塞：功能未開放 |
| <a id="v31"></a>V31 | `R/issue-340.md:45`；`R/issue-317.md:31` | E9；Quest 或 Time Attack | 帶召喚物進 Quest／Time Attack，讓召喚物打任務怪 30 下；再讓怪打開了迴避的玩家 30 下 | 記錄型：召喚物傷害比舊版低約 35% 是預期中的改動；關卡是否還打得過由使用者判斷 | console 沒有 `invalid companion NPC damage input` | `V31-summon-vs-quest.png` | 30 下的傷害與命中數、該波怪打完花的秒數 |
| <a id="v32"></a>V32 | `R/issue-337.md:48,51,52`；`R/issue-309.md:42` | 幻術師（D6）學立方 205／210／215／220、116、118；龍騎士 181；騎士 112；另要料理道具。seed 都沒有 | 各放一次：立方（看圖示、特效、地裂的束縛）；116（看 729 對話框文字與傳送畫面）；118（看傳送後面向）；112 破壞盔甲打吃了料理的 B；怪物魔法打開著龍之護鎧／耐力的玩家 | 圖示與特效出現；116 的對話框文字正確；118 傳送後面向 5；龍之護鎧／耐力讓怪物魔法傷害少 10／15（記錄型） | 以 capture 為準 | `V32-<技能>.png` | 每個技能：有沒有圖示、特效、訊息代碼；傷害各記 10 次。沒有對應角色 → 「阻塞：資料」（量大，建議主控決定要不要做） |
| <a id="v33"></a>V33 | `R/issue-301.md:27` | 實驗設定改 `battle.ai.enabled=false` 後重啟一次（這個鍵在 22 鍵內，`S/UnifiedConfiguration.java:7,31`）；A 單人 | A 單人進 Local Lobby 開戰 | 記錄型：ArenaBot 有沒有出現、會不會動、會不會攻擊 | 啟動行 `[LiTo] … battle-ai=`（`S/Main.java:91-105`）顯示關閉 | `V33-arenabot-ai-off.png` | Bot 的行為、能不能開戰、比賽怎麼結束。做完把設定改回並重啟 |

---

### 阻塞項目（不排入時間盒）

| 序 | 來源 | 原本要驗什麼 | 阻塞原因 | 解除後的做法 |
|---|---|---|---|---|
| <a id="v34"></a>V34 | `R/issue-306.md:45,60`；`R/issue-301.md:27` | 兩個來源各一個 **objid 相同**的角色同房對戰：看得到彼此、能互打、結算勝負正確；兩個來源庫的 `character_lito` 各自 +1 勝／+1 敗 | **阻塞：E13**（實驗環境只有一個來源） | 六欄照 A08–A15；Q1 對兩個來源庫各跑一次；截圖 `V34-cross-source-room.png`、`V34-cross-source-result.png`、`V34-sql.png`；失敗時記兩人在封包裡的 object id 是否不同（撞號時伺服器會另配，`R/issue-306.md:58`） |
| <a id="v35"></a>V35 | `R/issue-368.md:37-39,55` | 租裝城鎮按返回／重新開始 → 回角色清單（沒有結算畫面）→ 重新選租裝入村；兩人同村時另一人只看到對方消失；一般戰鬥中退出仍出結算 | **阻塞：功能未開放** | 最後一句「一般戰鬥中退出仍出結算」不需要租屋城鎮，已由 [G23](#g23) 的註記與 A16 涵蓋。其餘待解除後依 `docs/reference/rental-town-return-contract.md` 寫六欄；截圖 `V35-rental-return.png` |
| <a id="v36"></a>V36 | `R/issue-369.md:60,74` | 租裝角色在租賃村用傳送石 → 同位址重連 → 選頻道 → 看到頻道人數並進大廳，背包仍是傳送前選的那套；離開頻道回選角畫面後租裝選擇鈕恢復可用 | **阻塞：功能未開放** | 截圖 `V36-same-endpoint.png` |
| <a id="v37"></a>V37 | `R/issue-371.md:54-56,68` | 向勒夫租 29047 → 穿戴 → 屬性視窗火抗 +10 → 卸除還原；向克里租 29032 → 穿戴 → SP +10、AC −12；穿著進戰鬥後的 `S_STATUS`；租期到期時屬性還原 | **阻塞：功能未開放** | 截圖 `V37-merchant-29047.png`、`V37-merchant-29032.png`。注意 `R/issue-371.md:53`：TSV 的 `spell_power`／`magic_resistance` 兩欄可能對調，29032 顯示成 MR +10 而不是 SP +10 時照實記，不直接判失敗 |
| <a id="v38"></a>V38 | `R/issue-337.md:49-50`；`R/issue-340.md:44`；`R/issue-336.md:46`、`R/issue-321.md:53`、`R/issue-311.md:51` 的 WORLD 部分 | 經典世界：安全區隊友吃到立方屬性 +30、衝擊立方不再讓 MR −10；攻城戰期間 118 回 1192；WORLD 怪物打寵物與 Dspy 護送的命中與傷害 log；火牢、76 自施、魔法娃娃在 WORLD 的同樣行為 | **阻塞：經典世界關閉** | 戰鬥房的對應行為已排在 V08、V11、V15；WORLD 部分等解除 |

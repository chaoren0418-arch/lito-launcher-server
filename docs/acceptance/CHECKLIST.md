# M4 實驗環境實機核對表（最新 main 的 Maven JAR＋P24 實驗 schema）

更新：2026-11-13（T127，Issue #378）。前一版（#283，正式 0.6.71 JAR 版）在 git 歷史。

操作者：**Codex（`codex-local`）執行並截圖，使用者觀看驗收**。本表是待執行範本，不代表實機通過。交付基準是 **最新 `main` 的 `mvn package` 產物**（#284 主控 2026-11-11 14:08 裁定：M4 用 M3 的新 JAR，不是正式 0.6.71 JAR）。目標是建立「登入 → 大廳 → 技能 → 進房／戰鬥 → 結算 → 正常退出」的可重跑基線。

每一步的按鍵、預期封包、SQL 與截圖檔名在 [`M4-RUNBOOK.md`](M4-RUNBOOK.md) A 部；本表只列操作、預期與結果欄。程式出處的縮寫 `S/` = `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`。

## 命名契約（T128 的腳本、T137／T138 的執行都照這個）

| 項目 | 規則 |
|---|---|
| 版本目錄 | `docs/acceptance/m4-<JAR SHA-256 前 8 碼，小寫>/`（放去機密後的公開截圖） |
| 結果檔 | `docs/acceptance/m4-<前 8 碼>.md` |
| 截圖 | A 部（本表 01–21）`NN-<步驟>.png`；B 部 `G<編號>-<序>-<短名>.png`；C 部（T129）`V<序>-<短名>.png`。同一步多張時在短名後加 `-a`、`-b` |
| 本次執行目錄 | worktree 的 `scratch/m4/run-<UTC yyyyMMddTHHmmssZ>/`（JAR 副本、`lib/`、`config/`、`runtime/`、`captures/`、`logs/`、`tmp/`、`evidence/`） |
| 原始證據 | `scratch/m4/run-<UTC>/evidence/`（console、SQL、原始截圖；不進 git） |
| SQL 快照 | `evidence/sql-<before\|after>-<步驟>.txt`，步驟寫 `NN`、`G<編號>-<序>` 或 `V<序>` |

## 前置

原始專案根目錄為 `D:\lineage開發\3-lito開發`，只讀取。以下 `scratch/m4/`、證據及結果文件路徑皆相對於**本次執行 worktree**，不可寫回原始專案目錄。不得連接正式服務／正式資料庫（2000／3306）、啟停其程序，或修改正式成品、遊戲資源、硬連結與玩家實表。

| 項目 | 操作／要求 | 預期 | 證據 | 結果 |
|---|---|---|---|---|
| 交接與 lease | 讀 [#83 報告](../reports/issue-83.md)、[#284 報告](../reports/issue-284.md) 與 #284 的 2026-11-07 02:43、2026-11-11 14:08 裁定；啟動 P24Lab 前向調度器取得 `p24lab` lease | 整個 DB 使用期間持有有效 lease；未取得不得啟動或連線 | lease 識別、持有人、取得／到期時間 | 待執行 |
| 00 建 JAR 並記 SHA-256 | 在 worktree 的 `lito伺服器/LiTo381-Server/` 執行 `git rev-parse HEAD`、`mvn -B -DskipTests clean package`，再對 `target/LiTo381-Server.jar` 與 `target/lib/*.jar` 各算 `Get-FileHash -Algorithm SHA256`（命令見下方） | 建置成功；產物為 `target/LiTo381-Server.jar`（`pom.xml:41` finalName、`:168` Main-Class `SharedResourceMain`、`:166-167` Class-Path 前綴 `lib/`）與 `target/lib/`（`pom.xml:176-190`）。JAR SHA-256 前 8 碼決定版本目錄名 | commit、每個檔的大小與 SHA-256；與 CI artifact `server-reproducible-jars/reproducible-jars.sha256`（同 commit）是否一致（不一致照實記錄，不阻塞） | 待執行 |
| JAR 副本 | 把 `LiTo381-Server.jar` 與 `lib/` 普通複製到 `scratch/m4/run-<UTC>/`，逐檔再算雜湊；不建硬連結 | 來源與副本 SHA-256 相同；`runtime/` 為新建空目錄 | 來源／副本路徑、大小、SHA-256 | 待執行 |
| 客戶端啟動方式 | **正式 0.6.71 登入器不能接外部伺服器**（JAR 路徑、設定檔與 32000 埠都寫死，見 RUNBOOK §P5）。M4 改用登入器 `app/` 內同一套 runtime probe 直接啟動 `LiTo.bin`，連到實驗端點；需要 T128 的啟動腳本或主控另開任務 | 兩個遊戲視窗都連到 `127.0.0.1:(42083+source_id)`；所用的 probe、`litoddraw.dll` 與正式 0.6.71 成品雜湊相同；輸出留在 `scratch/m4/` | probe、DLL、`LiTo381Launcher.exe` 的雜湊；去機密的連線目標 | 待執行 |
| 實驗設定 | 依 #83 私密交接建立 `scratch/m4/run-<UTC>/config/experiment.properties`，22 個鍵（`S/UnifiedConfiguration.java:7`）；DB 帳密取自 #83 的 `isolated-test.properties`，不可印出或提交 | 設定在 worktree、受 git 忽略；固定欄位如下方片段 | 去機密的設定（`config-public.properties`）；`git check-ignore` 結果 | 待執行 |
| P24Lab 與實驗庫 | 持 lease 後依既有流程啟動 P24Lab，核對 `127.0.0.1:43324` 身分與 #83 收據；確認 `p24_380db_lito_exp`（由 `D:\BinEngine\environments\p24\sql\380_db.sql` 匯入，02:43 裁定）、`p24_lito_lito_exp`、`p24_accounts_lito_exp` 存在且受限帳號可 SELECT | 實例 UUID、datadir、bind 與 #83 相符；來源路由只指向 `p24_accounts_lito_exp` | 身分核對摘要、來源路由 SELECT | 待執行 |
| `character_lito` 新欄位 | 以 `information_schema.COLUMNS` 查 `p24_accounts_lito_exp.character_lito` 是否有 `ranking_points`、`disconnects`、`monthly_period`、`monthly_ranking_points`、`monthly_wins`、`monthly_losses`、`last_result_id` | 7 欄都在。缺欄位時新 JAR 會拒絕啟動，訊息 `character_lito in <db> lacks ranking columns; run db/lito_character_ranking.sql first`（`S/SettlementSql.java:46`）；補欄位屬 T128 的 `m4-db-prepare.sql`，由 P24Lab 管理身分執行 | 欄位查詢結果 | 待執行 |
| 測試帳號 | 只用 #83 的兩組普通測試登入／角色（A、B）；憑證依 `test-accounts-private.properties` 私密交接；執行下方基線 SELECT | A、B 各一角色，保存當次 9 個計數欄位與 `version`、`last_result_id`，不假設為零 | 去機密的角色對照與前值；禁止截取登入密碼 | 待執行 |
| 客戶端與相容層 | 資源引用 `D:\lineage開發\0-客戶端`；只讀核對 `LiTo.bin` SHA 前綴 `CE6EE68A…`、`litoddraw.dll` 與 `lito381-compat.ini` 的雜湊，以及目錄內**沒有** `ddraw.dll`（有的話具名載入會拒絕啟動，`app/named_ddraw.py:18-19`） | 客戶端版本相符；`0-客戶端\litoddraw.dll` 與登入器 `app\compat\litoddraw.dll` 雜湊相同（不同時停下，不覆寫） | 路徑、雜湊清單、目錄截圖；載入證據在步驟 06 補齊 | 待執行 |
| 實驗輸出與 WAL | Java 工作目錄固定 `scratch/m4/run-<UTC>/`；設定在 `config/`，所以結算 WAL 落在 `run-<UTC>/runtime/battle-settlements/`（`S/DurableBattleSettlement.java:18-19`）；`capture.directory=../captures`、`logs.directory=../logs` | 本輪沒有舊待補 WAL；不讀正式 runtime | 啟動前 `runtime/` 清單（空） | 待執行 |
| 額外案例前置 | 依 RUNBOOK「資料前置」表核對八角色、GM、龍騎士／幻術師、技能、同血盟、道具 | #83 只有兩組普通帳號；不足時記錄阻塞，由 T128 的 `m4-seed-exp.sql` 補，不自行改權限／加資料 | 各案例可用前置與缺口 | 待執行 |
| 採證 | 原始 console、SQL、封包、截圖存 `evidence/`；去機密後的截圖存版本目錄 | 每步可追溯時間、結果、證據；帳密與原始資料不進 git | 證據索引 | 待執行 |

步驟 00 的命令（Codex 執行；本機 Claude 執行器不得執行，因為建置會跑編譯出來的 AsRecord 工具）：

```powershell
# 目前目錄：worktree 的 lito伺服器\LiTo381-Server
git rev-parse HEAD
mvn -B -DskipTests clean package
Get-FileHash -Algorithm SHA256 .\target\LiTo381-Server.jar
Get-ChildItem .\target\lib -File | Get-FileHash -Algorithm SHA256
```

`target/` 內另有 `LiTo381-Server-modern.jar`、`LiTo381-Server-as-record-tests.jar`，M4 不使用。

實驗設定的固定欄位（**必要覆核片段，不是完整設定**；其餘鍵沿用 `config/server.properties.example` 的值，帳密由 #83 私密設定補齊）：

```properties
bind=127.0.0.1
game.bind=127.0.0.2
port=42083
database.jdbc.base.url=jdbc:mysql://127.0.0.1:43324/
database.static.database=p24_380db_lito_exp
database.lito.database=p24_lito_lito_exp
capture.directory=../captures
logs.directory=../logs
startup.timeout.seconds=120
```

- 統一設定只接受 22 個鍵，多寫一個就拒絕啟動（`S/UnifiedConfiguration.java:24`）。所以 `battle.reward.win`／`battle.reward.loss` **不能**寫進實驗設定，積分一律用程式預設 **勝 250、敗 50**（`S/ServerConfig.java:578-579`）。
- 若 `p24_lito_lito_exp` 也有欄位不相容，依 02:43 裁定第 4 點改用 `p24_380lito_lito_exp`，並在結果檔寫明。
- `docs/reports/issue-284/` 的 `run-experiment.ps1`、`verify-run.ps1`、`Prepare.java`、`Probe.java` 寫死了正式 JAR 的 SHA、檔名 `LiTo381-SharedResources-candidate.jar`、`p24_world_lito_exp` 與 `scratch/t014/`，**不能直接用在 M4**；M4 的對應腳本是 T128 的 `tools/acceptance/`。

## 步驟

結果逐項填「通過／失敗／阻塞／未執行」，不能沿用舊版通過紀錄。每個失敗或異常均須新增一列「現象／步驟／證據」；**失敗不中斷其餘可執行步驟，除非無法繼續**。

| # | 操作 | 預期 | 證據 | 結果 |
|---|---|---|---|---|
| 01 | 完成前置、核對實驗埠無衝突；在 `scratch/m4/run-<UTC>/` 以 `java … -jar LiTo381-Server.jar config/experiment.properties` 啟動（命令見下），從命令開始保存 stdout／stderr | Java 17 使用步驟 00 的 JAR 與實驗設定；120 秒內出現 `[LiTo] ready`（`S/Main.java:117`），沒有啟動例外 | 啟動命令／工作目錄／時間／PID、`evidence/server-console.log`、`01-server-ready.png` | 待執行 |
| 02 | 核對 Java PID 的監聽位址及來源路由；來源埠依 SQL 的實際 `source_id` 計算 | 監聽共四個：`127.0.0.1:42083`、`127.0.0.2:42083`（`S/Main.java:71-80`），以及每個來源的 `127.0.0.1:(42083+source_id)` 與 `127.0.0.2:(42083+source_id)`（`S/SourceLogin.java:42-50`）；沒有其他位址或埠 | console 的 `login-listen`／`game-listen`／`source-login` 行＋`Get-NetTCPConnection -State Listen`、路由 SELECT、`02-listen.png` | 待執行 |
| 03 | 依前置「客戶端啟動方式」啟動第一個遊戲視窗，檢查伺服器選單 | 視窗開啟；原生伺服器選單的來源清單對應 `p24_lito_lito_exp.lito_source_server`；連線到實驗端點 | 伺服器選單畫面、console 的 `connected endpoint=LOGIN` 行、`03-server-select.png` | 待執行 |
| 04 | 帳號 A 登入、選角色進大廳 | 登入成功；角色及職業圖示正確，進入大廳 | `04-char-list.png`、`04-lobby.png` | 待執行 |
| 05 | 執行八角色翻頁；資料不足則記錄阻塞 | 帳號有 5 個以上角色時可正確翻頁；不足不算通過、不臨時新增角色 | `05-page1.png`、`05-page2.png`，或角色數不足的證據 | 待執行 |
| 06 | 唯讀核對本輪客戶端實際載入的相容層 DLL | 實際載入的是 `litoddraw.dll`，路徑在 `0-客戶端`；沒有載入通用 `ddraw.dll` | 模組清單或 `lito381-compat.log`＋`06-compat-module.png` | 待執行 |
| 07 | 帳號 B 於第二視窗登入同一實驗來源 | A、B 都在大廳，角色與來源正確 | `07-two-lobby.png` | 待執行 |
| 08 | A 建房、B 進房；用玩家自建房（不是 Local Lobby 測試房） | 房間狀態同步；自建房固定為死鬥、雙方人數一致（`S/ClientSession.java:10371-10375`）；A、B 分屬紅藍兩隊 | `08-room.png` | 待執行 |
| 09 | 雙方準備，倒數 3 秒後開戰 | 兩人進入同一戰鬥場景、可操作；地圖資產缺失則記錄實際錯誤 | `09-battle.png`、對應時間與 console | 待執行 |
| 10 | 執行一次近戰攻擊，記錄目標 | 攻擊動作播放，命中時對方 HP 下降 | **`10-melee.png`**＋封包／console | 待執行 |
| 11 | 執行一次魔法技能（建議技能 4 光箭），記錄技能 ID 與目標 | 魔法施放與效果符合規則，MP 扣減 | **`11-magic.png`**＋封包／console | 待執行 |
| 12 | 執行一次變身：`.polyme 6080`（需 GM 前置）；沒有 GM 時記錄阻塞 | 外觀改變，訊息「變身外型 6080，持續 7200 秒。」（`S/GmPolyMe.java:59`）；權限不足不自行提升 | **`12-polyme.png`**＋指令／回應 | 待執行 |
| 13 | 完成這一場戰鬥，分出勝負（死鬥：一方陣亡） | 正常進入結算，勝負與實際戰況一致 | `13-battle-end.png` 與時間 | 待執行 |
| 14 | 核對結算畫面及職業圖示 | 勝負、雙方角色與職業圖示正確 | `14-result.png` | 待執行 |
| 15 | 執行下方戰績／收據 SELECT，與前值比對 | 見下方「戰績預期值」的勝／敗列；雙方 `last_result_id` 更新並對上各自 `applied=1` 收據 | `sql-before-15.txt`、`sql-after-15.txt`、`15-sql.png` | 待執行 |
| 16 | 再打一場前保存戰績；戰鬥中關閉 B 的客戶端視窗，等待結算 | A 判勝；B 收據 `disconnected=1`；數值見「戰績預期值」的勝／斷線列；WAL 無殘留 `.pending`；無法開第二場則記錄阻塞 | `16-a-wins.png`、`sql-before-16.txt`、`sql-after-16.txt`、`runtime/battle-settlements/` 清單 | 待執行 |
| 17 | 僅在 GM 前置已備妥時，執行一個 GM 指令（`.loc`）與 `.polyme <外型代號>` | `.loc` 回座標；變身生效且持續 7200 秒（7200 是秒數，不是外型代號） | `17-gm-loc.png`、`17-gm-polyme.png`，或 GM 前置不足證據 | 待執行 |
| 18 | 同一 GM 前置下，`.item 42501` 取得 GM 魔杖，使用後點地面 | 角色瞬移到點選位置；沒有點選失效、錯誤位置或崩潰 | `18-wand-before.png`、`18-wand-after.png`＋console；前置不足記錄阻塞 | 待執行 |
| 19 | B 重新登入後，以雙視窗驗證 Ctrl 強制攻擊 | 按鍵與目標反應符合規則，兩視窗觀察一致（細節見 RUNBOOK G23） | `19-ctrl-attack.png` | 待執行 |
| 20 | 兩個客戶端正常登出並關閉，對本次實驗 Java 主控台按 Ctrl+C | 正常關閉，無新增 `hs_err_pid*.log`；Java PID 與四個實驗監聽釋放、WAL 無待補；`owner.lock` 可留檔，確認鎖已釋放 | `20-exit.png`、console、PID／監聽核對、`runtime/` 清單 | 待執行 |
| 21 | 依 lease 約定正常關閉本次啟動的 P24Lab 或交回其管理者，再釋放 `p24lab` lease | 無本次遺留連線／程序；不停止他人程序、不刪 schema、帳號或憑證 | 關閉或交接紀錄、lease 釋放時間、`21-p24lab-closed.png` | 待執行 |

步驟 01 的命令（沿用 `docs/reports/issue-284/run-experiment.ps1:39` 的 JVM 參數，JAR 檔名改為 Maven 產物；**僅由 Codex 在前置完成後執行**）：

```powershell
# 目前目錄：worktree 的 scratch\m4\run-<UTC>\
& 'C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin\java.exe' -Xms512m -Xmx2048m -XX:ActiveProcessorCount=4 `
    '-Dfile.encoding=UTF-8' "-Djava.io.tmpdir=$PWD\tmp" -jar .\LiTo381-Server.jar config/experiment.properties 2>&1 |
    Tee-Object -FilePath .\evidence\server-console.log
```

`startup.timeout.seconds` 不會讓 `java -jar` 自動退出；操作者計時，120 秒內沒有 `[LiTo] ready` 就在該主控台按 Ctrl+C、保存輸出並停止。啟動失敗時保存退出碼與錯誤，不改用其他成品或正式端點。

## SQL 前後對照（僅 P24 實驗庫）

使用 #83 受限 DB 帳號、明確指定 TCP `127.0.0.1:43324`。以下查詢只讀 `p24_accounts_lito_exp`／`p24_lito_lito_exp`，不做 DDL、DML。先把 `<角色A>`、`<角色B>` 換成私密交接中的角色名；公開結果以 A／B 去識別。

```sql
SELECT source_id, display_name, character_database, enabled
FROM p24_lito_lito_exp.lito_source_server
ORDER BY source_id;

SELECT char_obj_id, char_name, wins, losses,
       ranking_points, disconnects,
       monthly_period, monthly_ranking_points, monthly_wins, monthly_losses,
       version, last_result_id
FROM p24_accounts_lito_exp.character_lito
WHERE char_name IN ('<角色A>', '<角色B>')
ORDER BY char_obj_id;

SELECT result_id, sequence_id, battle_id, source_database, char_obj_id, char_name,
       won, disconnected, applied, created_at, applied_at
FROM p24_lito_lito_exp.lito_battle_settlement
WHERE source_database = 'p24_accounts_lito_exp'
  AND char_name IN ('<角色A>', '<角色B>')
ORDER BY sequence_id;
```

來源清單應僅有 #83 建立的實驗路由，啟用列的 `character_database` 必須為 `p24_accounts_lito_exp`。記錄實際 `source_id`；例如值為 1，來源埠就是 42084。以每場新增的 `battle_id`／`result_id` 與戰績前後值核對，不能拿既有收據冒充本次成功。

### 戰績預期值

規則出處：[`docs/design/gameplay.md`](../design/gameplay.md) §4.1（`:266-271`）、§4.3（`:293-295` 積分、`:297` 月計、`:302` 跨月）；程式 `S/SettlementSql.java:29-38`（`next`）、`:97`（單一 UPDATE）。以下「前」＝該場開始前的 SELECT 值。

| 欄位 | 勝（步驟 15 勝者、16 的 A） | 一般敗（步驟 15 敗者） | 斷線敗（步驟 16 的 B） |
|---|---|---|---|
| `wins` | 前 +1 | 不變 | 不變 |
| `losses` | 不變 | 前 +1 | 前 +1 |
| `ranking_points` | 前 **+250**（`gameplay.md:293`） | 前 **+50**（`gameplay.md:294`） | `max(0, 前 − 50)`（`gameplay.md:295`） |
| `disconnects` | 不變 | 不變 | 前 +1 |
| `monthly_period` | 當月 `YYYYMM`（台北時間每天 05:00 為日界，`gameplay.md:297`） | 同左 | 同左 |
| `monthly_ranking_points` | 月基準 +250 | 月基準 +50 | `max(0, 月基準 − 50)` |
| `monthly_wins` | 月基準 +1 | 月基準 | 月基準 |
| `monthly_losses` | 月基準 | 月基準 +1 | 月基準 +1 |
| `version` | 前 +1 | 前 +1 | 前 +1 |
| `last_result_id` | 本場該角色收據的 `result_id` | 同左 | 同左 |
| 收據 | 新增一列 `won=1, disconnected=0, applied=1` | `won=0, disconnected=0, applied=1` | `won=0, disconnected=1, applied=1` |

- 「月基準」：`前.monthly_period` 等於當月時＝前值；不等於當月（含 0）時＝0（`gameplay.md:302`、`S/SettlementSql.java:32-34`）。
- 例：#83 的角色初始全為 0，步驟 15 之後勝者 `ranking_points=250`、敗者 `50`；步驟 16 若 B 是步驟 15 的敗者，B 變成 `max(0, 50−50)=0`、`disconnects=1`、`losses=2`，A 再 +250。
- console 每位參戰者會有一行 `[LiTo] battle result player=… won=… ranking-points=… wins=… losses=… disconnects=… disconnected=…`（`S/ClientSession.java:56906-56915`），數值應與 SELECT 一致。
- 測試房（Local Lobby）不寫戰績（`gameplay.md:227`），不能用來做步驟 15、16。

## 失敗／異常記錄

每個失敗、異常或阻塞步驟各寫一列；即使後續重試通過仍保留原始現象及重試證據。無法繼續的步驟標「阻塞」並引用前置失敗編號；其餘步驟照常執行。GM、翻頁、技能等資料不足不得填「不適用」來當作驗收通過。

| 現象 | 步驟 | 證據 |
|---|---|---|
| 待執行後填寫：實際／預期差異、發生時間、能否繼續及影響 | 前置項目或步驟編號 | 截圖路徑、去機密 console 行號／時間段、封包 `frames.jsonl` 的時間段、SQL 前後對照；阻塞時附前置失敗編號 |

收集該時段 `evidence/server-console.log`、`captures/<執行>/frames.jsonl`、客戶端 probe 的 stdout／stderr；需要時唯讀收集 Windows 應用程式事件中的 `java` 錯誤及本次 `hs_err_pid*.log`。原始資料保留本機；公開證據先遮蔽帳密與非本次資料（`frames.jsonl` 的聊天與角色名是明文，不直接提交）。缺少證據則明列缺少什麼，不補造。

## 結果回寫與交接

Codex 將本表複製為 `docs/acceptance/m4-<前 8 碼>.md`，填入執行日期、操作者、worktree、commit、成品雜湊、lease 紀錄、每步結果／證據及失敗清單；B 部（T137）、C 部（T138）的結果接在同一檔。報告分「已驗證／推測／待驗證」，由使用者觀看畫面驗收；未跑、阻塞與待確認項目不得宣稱通過。

58 列材料技能逐一驗證、NPC／PET 目標語意、長時間浸泡測試不在本表範圍。

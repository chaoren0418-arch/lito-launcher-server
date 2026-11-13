# Issue #387 / T136 — 結算待寫入結果的作廢與 `.tmp` 殘檔清理

分支 `task/387`，PR #403。本機只做編譯（`mvn -B -o -q -DskipTests test-compile`）；測試全部在 GitHub Actions 執行。沒有連任何正式 DB，沒有對任何庫執行遷移。

## 做了什麼

### 1. 設計（`docs/design/gameplay.md` §4.2，新增 5 列、改 2 列）

**作法：選 (b)「啟動時讀營運放的作廢清單」**，不選 (a) 離線命令列。理由：
- 正式啟動入口 `Main` 與正式 JAR 逐位元比對（T002／semantic-diff），加命令列入口要另開一條「讀設定 → 建 Route → 連控制庫」的路徑，和正式啟動重複一份；
- (b) 直接用啟動時已建好的 Route 與控制庫連線，而且是在取得 WAL 目錄的 `owner.lock` 之後才處理，保證沒有另一個伺服器程序同時在寫；
- 兩者都不需要營運手動下 SQL，也都不新增來源庫寫入權限。

流程：伺服器停機 → 營運在 `runtime/battle-settlements/void-requests.txt` 列 `result_id`（一行一筆，空白後可接原因，`#` 為註解）→ 啟動時在**重放 WAL 之前**處理。

**安全條件**（`SettlementSql.voidResult`，和寫入用同一個 `GET_LOCK`，全部成立才作廢）：
1. 收據存在且 `applied=0`。已計入（`applied=1`）拒絕；已作廢（`applied=2`）視為成功（冪等）。
2. 來源庫 `characters` 目前沒有這個 objid，或名字和凍結時不同——也就是「身分已變更」才是它寫不進去的原因。身分沒變的結果會正常寫入，拒絕作廢。
3. 該角色 `character_lito.last_result_id` 不是這筆結果。若計數已寫、只差收據回條，拒絕（否則收據說作廢、計數卻已經加了）。

**寫入範圍**：只更新控制庫收據列（`applied=2`、`voided_at`、`void_reason`）。**不寫 `character_lito`、不寫來源庫任何表**；執行帳號既有的 `lito_battle_settlement` 表級 `UPDATE` 已涵蓋新欄位，不需要新權限。作廢後重放含該筆的 WAL 檔時，收據已存在（`ON DUPLICATE KEY` 不動），`drain` 只處理 `applied=0`，所以檔案正常處理、刪除，同檔其他人的結果照常寫入——不需要另外改寫 WAL 檔。

**稽核紀錄**：①收據列保留不刪，`voided_at`／`void_reason`；②作廢成功後、重放刪檔前，把含這些結果的 `.pending` 檔**複製**到 `voided/`（被拒的請求不複製；在複製前當機，下次啟動會以 `already-voided` 再複製）；③`void-audit.log` 每筆一行（時間、`voided`／`already-voided`、`result_id`、來源庫/objid、凍結名、目前名或 `<deleted>`、原因），寫完 `fsync`；④清單處理完移到 `voided/void-requests-<時間>.txt`；啟動 log 逐筆印出。

**失敗時**：清單格式錯（不是 64 位十六進位、原因超過 200 字或含控制字元）→ 拒絕啟動，資料庫完全不動；任一筆不符安全條件 → 拒絕啟動，清單留在原處；同清單裡排在前面已作廢的不回滾，重跑時記為 `already-voided`。

### 2. 實作（`src/main`）
| 檔案 | 內容 |
|---|---|
| `SettlementSql.java` | `voidResult`（上述三個條件＋冪等）、`requireVoidColumns`（只有清單存在時才檢查，缺欄位提示跑遷移）、`lockName`（與 `drain` 共用）、狀態常數 `PENDING／APPLIED／VOIDED`；`identity changed` 訊息附 `result=<result_id>`，營運才知道要作廢哪一筆 |
| `DurableBattleSettlement.java` | 啟動流程抽成 `recover(Path)`：開 WAL（含 `.tmp` 清理）→ `voidRequested` → 重放 → `drainAll`；`open()` 改呼叫它，行為除新步驟外不變 |
| `SettlementJournal.java` | 建構時清 `.tmp`；`voidRequests`（嚴格解析清單）；`archive`（複製含指定結果的 `.pending` 到 `voided/`，`voided/` 子目錄不會被當成待重放檔） |
| `db/lito_battle_settlement_void.sql`（新） | 在 `380_lito.lito_battle_settlement` 加 `voided_at TIMESTAMP NULL`、`void_reason VARCHAR(255) NULL`；寫法比照 `db/lito_character_ranking.sql`（先查 `information_schema`，可重跑）；`applied` 欄說明 0／1／2 |
| `db/ci/build_ci_database.py` | CI fixture 載入上面的遷移（控制庫名可被 `tools/acceptance/ci/build_m4_fixture.py` 覆寫） |

### 3. `.tmp` 殘檔清理（`SettlementJournal` 建構子）
四個條件**都成立**才刪：
1. 本程序已取得 `owner.lock`（`save` 只在持有者程序內執行，其他程序不可能正在寫這個目錄；第二個程序取不到鎖時在清理前就失敗，什麼都不刪）；
2. 檔名正好是 `save` 產生的 `<小寫 UUID>.tmp`；
3. 是一般檔案（不是目錄、不是連結；用 `NOFOLLOW_LINKS` 讀屬性）；
4. 最後修改時間不晚於取得鎖的時刻（比本程序任何一次 `save` 都早）。

其他 `.tmp` 保留，啟動 log 印 `kept unrecognised <檔名>`；刪掉的印 `removed stale <檔名>`。

### 4. 文件
- `docs/INSTALL.md`：§2 建庫表加第 8 列（作廢遷移）；§6 常見錯誤加 5 列（`identity changed`、`Settlement void refused`、`lacks void columns`、清單格式錯、`.tmp` log）；新增 §6.1 營運作廢步驟。
- `docs/design/gameplay.md` §4.2 與鎖順序表；三個 Java 檔行號變動後，`gameplay.md`、`database.md`、`acceptance/M4-RUNBOOK.md` 對這三個檔的行號引用以 diff 對照重新對齊（用 `scratch/remap_refs_387.py`，新列手動核對）。
- `TASKS.md` T136 → review。

## 怎麼驗證

### 新增／修改的測試
| 測試 | job | 內容 |
|---|---|---|
| `SettlementJournalRecoveryTest`（原 7 案，新增 4 案、改 1 案） | `Server tests`（必過，不需 DB） | 舊持有者留下的 `.tmp` 會被刪；非 `save` 檔名、大寫 UUID、修改時間晚於取鎖、目錄型的 `.tmp` 全部保留；第二個程序取不到鎖時不刪別人的 `.tmp`；清單解析（BOM、註解、重複、預設原因、壞 ID、過長原因）；`archive` 只複製含指定結果的檔、重跑不重複、`voided/` 不被當成待重放檔 |
| `SettlementVoidMySqlIntegrationTest`（新，4 案） | CI MySQL | 見下 |

`SettlementVoidMySqlIntegrationTest`（比照 `DisconnectSettlementRecoveryMySqlIntegrationTest` 的改名角色案例，走正式的 `DurableBattleSettlement.recover`）：
- **作廢後可啟動、其他結果不受影響、作廢的不計入、重複作廢冪等**（MyISAM `380_ac_1`、InnoDB `380_ac_2` 各一次）：Knight 有兩筆以舊名凍結的結果（其中一筆和 Wizard 同檔），另有一筆 Knight 正確的結果排在後面、一筆 Wizard 單獨結果。作廢前啟動被拒，錯誤訊息含 `result=<id>`；放清單後啟動成功、WAL 清空；Knight 只計入正確的那一筆（`version` 只 +1）、Wizard 兩筆都計入；兩筆收據 `applied=2`、`voided_at`、`void_reason` 正確，其他三筆 `applied=1`、`voided_at` 為空；`voided/` 有 2 個 WAL 副本與 1 份清單、`void-audit.log` 內容正確。再放同一份清單啟動：`already-voided`，收據（含 `voided_at`）不變；再送一次舊 ticket／重放：計數與 `version` 不變。
- **不安全的請求被拒且不改資料**：身分沒變（`unchanged`）、已計入（`already counted`）、未知 ID（`unknown result`）、計數已寫只差回條（`already in character_lito`）；每次拒絕後收據狀態與計數不變、清單留在原處；條件修正後同一份請求可以成功。
- **清單格式錯**：在碰資料庫之前就拒絕啟動。

### 結果
- **已驗證**
  - 本機 `mvn -B -o -q -DskipTests test-compile` 通過（依規定本機不執行測試）。
  - 本機執行 `db/ci/build_ci_database.py` 與 `tools/acceptance/ci/build_m4_fixture.py`（輸出到 `scratch/`），兩者都產出含作廢遷移的 `30-control.sql`，控制庫名各自正確。
  - CI：見下方「CI 結果」。
- **推測**
  - 檔案系統時間精度粗的環境（FAT 的 2 秒）可能讓剛寫的 `.tmp` 修改時間被進位到取鎖之後，那種檔會被保留（偏安全的一側），不會誤刪。
- **待驗證**
  - 正式 `380_lito` 執行 `db/lito_battle_settlement_void.sql`（需資料庫管理者、ALTER 權限、停機時）。
  - 實機流程：真的把一個有待寫入結果的角色在來源庫改名 → 啟動被拒 → 照 `INSTALL.md` §6.1 作廢 → 啟動成功。交給 Codex（可併入 M4 的 V21 附近）。

### CI 結果
（見下節，push 後補上）

## 未驗證什麼
- 實機與正式庫（見待驗證）。
- 「計數已寫、只差回條、而且角色已改名」目前是拒絕作廢；這種情況下伺服器仍會因 `identity changed` 拒絕啟動，需要資料庫管理者手動把收據標 `applied=1`（`drain` 檢查身分在檢查 `last_result_id` 之前）。此情況要在寫入與回條之間當機、同時角色被改名才會發生，本次沒有改 `drain` 的順序。
- 執行中（伺服器運作時）不能作廢；清單只在啟動時讀。

## 產出路徑
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/SettlementSql.java`
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/SettlementJournal.java`
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/DurableBattleSettlement.java`
- `lito伺服器/LiTo381-Server/db/lito_battle_settlement_void.sql`（新）
- `lito伺服器/LiTo381-Server/db/ci/build_ci_database.py`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/SettlementJournalRecoveryTest.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/SettlementVoidMySqlIntegrationTest.java`（新）
- `docs/design/gameplay.md`（§4.2）、`docs/INSTALL.md`（§2、§6、§6.1）、`docs/design/database.md`、`docs/acceptance/M4-RUNBOOK.md`（行號）、`TASKS.md`

## 給下一棒的提示
- 正式 JAR 仍由 `tools/*/build.py` 逐層 patch（T002）；本次修改在 `src/main`，要等 T002 鏈併回後才會進正式 JAR。部署前資料庫管理者要先跑 `db/lito_battle_settlement_void.sql`（不跑也能啟動，只是不能作廢）。
- `tools/db-grants/generated/db-access.tsv` 是掃描 SQL 用到的欄位的產物，本次沒有重產（新欄位 `voided_at`／`void_reason` 在同一張表、表級授權已涵蓋，`grants.sql` 不需改）；下次跑 `tools/db-grants` 時會多出這兩欄。
- 若要讓「計數已寫、只差回條」的改名角色也能自動處理，可以把 `SettlementSql.drain` 的 `last_result_id` 檢查移到身分檢查之前（已計入就只補回條）；這會改變 T124 測過的路徑，建議另開任務並跑 `DisconnectSettlementRecoveryMySqlIntegrationTest`。
- 實機清單（給 Codex）：①在實驗庫改一個角色名，讓它有一筆 `applied=0` 的收據 → 啟動應被拒並印 `result=`；②照 `INSTALL.md` §6.1 放清單、啟動 → `[LiTo] ready`，收據 `applied=2`，`character_lito` 不變；③同一份清單再放一次 → `already-voided`；④在 WAL 目錄放一個舊的 `<UUID>.tmp` → 啟動 log 印 `removed stale`。

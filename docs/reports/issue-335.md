# Issue #335 / T103b — 積分持久化：character_lito 加 ranking_points／disconnects／月計（#310 D3）

分支 `task/335`。依 #310 的決定 D3＝B：積分、斷線數、月計寫進各來源庫 `character_lito`，重啟不再歸零。

## 做了什麼

1. **升級 SQL** `lito伺服器/LiTo381-Server/db/lito_character_ranking.sql`（新檔）
   - 對 `character_lito` 加 6 欄：`ranking_points`、`disconnects`、`monthly_period`（YYYYMM，0＝尚無月計）、`monthly_ranking_points`、`monthly_wins`、`monthly_losses`。
   - 全部 `ADD COLUMN … UNSIGNED NOT NULL DEFAULT 0`：不改引擎、不動既有欄位，既有列新欄位＝0；MyISAM 與 InnoDB 都適用（`docs/design/database.md` §7 的結論：兩種引擎都接受，靠「同一列、同一條 UPDATE」保證原子）。
   - 冪等：每欄先查 `information_schema.COLUMNS`，寫法與既有 `db/lito_battle_settlement.sql` 的 `last_result_id` 遷移相同。
   - 升級前的積分只在記憶體、每次重啟歸零，沒有可靠來源可回填，所以不回填。
2. **結算寫回（兩條路徑都改）**
   - WAL 路徑 `SettlementSql.drain`：收據多讀 `disconnected`；新值由新的純函式 `SettlementSql.next` 計算（勝 +`battle.reward.win`、敗 +`battle.reward.loss`、斷線 −`battle.reward.loss`、最低 0；月份不同時先把三個月計歸零），和 wins／losses／version／`last_result_id` 寫在**同一條** UPDATE（沒有列時同一條 INSERT）。冪等機制完全沿用 `last_result_id`。
   - 直接路徑 `SharedDatabase.persist`（`commitResult`）：同一條 UPDATE／INSERT 一併寫 6 欄；CAS 條件不變（version＋wins＋losses）；另外拒絕「斷線數增加卻沒多一敗」與無效月份。為了能測試，拆出 `persist(Route, …)`，原本以 repository 為參數的入口仍先檢查結算權限旗標。
   - 獎勵值：`SharedDatabase.Route` 新增 `winReward`／`lossReward`，`SharedDatabase.open` 從 `ServerConfig.battleWinReward()/battleLossReward()` 帶入（就是 `ClientSession.recordBattleResult` 用的同一組值）。
   - 月份：新增 `GameStateStore.rankingPeriodKeyAt(epochMillis)`，和記憶體 `recordResult` 的月計用同一套算法（台北時間 05:00 日界）。
   - `DurableBattleSettlement.record`：結算寫回後，記憶體的勝敗、積分、斷線、月計一律改成資料庫的值（`durable(...)`），只有經驗／等級留在記憶體；回傳的 `Progress` 也用資料庫值。`reconcileRecovered` 改成比對並同步全部 8 個計數。
3. **啟動／登入載入**：`SharedDatabase.profiles` 與 `SettlementSql.load/current` 讀新欄位，`Route.profile` 放進 `TournamentProfileState`（原本一律填 0）。外部改壞的值會被夾到合法範圍：斷線數大於敗場數時取敗場數，月份無效時月計當 0。
4. **啟動檢查**：`SharedDatabase.check`、`SettlementSql.check` 會檢查 6 個欄位，缺欄位時拒絕啟動，並提示執行 `db/lito_character_ranking.sql`，不會靜默歸零。
5. **CI fixture**（`db/ci/build_ci_database.py`）：三個來源都套用新的升級 SQL；`380_ac_2` 的 `character_lito` 改建 InnoDB（其餘維持 1-伺服器 的 MyISAM），讓兩種引擎都測得到。
6. **測試**
   - `SettlementRankingTest`（不需要 DB，在必過的 `server-test` job 跑）：勝敗加分、斷線扣分到 0 為止、跨月重置月計但不動總計、台北 05:00 月界、`SettlementSql.next` 與記憶體 `GameStateStore.recordResult` 連續 9 場結果完全一致、`Route.profile` 載入與夾值、`durable` 覆寫會保留經驗。
   - `SettlementRankingMySqlIntegrationTest`（T119 的 CI MySQL，`380_ac_1`＝MyISAM、`380_ac_2`＝InnoDB 各跑一次）：
     - 舊月份的列結算一勝後，月計歸零再加本場；
     - 同一結果重放不會重複加分；
     - 模擬「計數已寫入但收據標記遺失」（把 `applied` 改回 0 再 drain），不會重複加；
     - 斷線扣分，最低 0；
     - 用新的 Route（快取是空的）重新載入，積分照樣讀得到；
     - 直接路徑 `persist` 寫入同樣 6 欄，並拒絕不一致的斷線數。
   - 安全閘：只有 `config/server.properties` 的 `database.user` 等於 workflow 的 `LITO_CI_DB_USER` 時才會執行；拿正式設定的本機 checkout 不會執行到寫入敘述。
7. **文件**
   - `docs/design/gameplay.md` §4.3：持久性、寫回、跨月、升級四列；§7 D3 決定欄；推測清單。
   - `docs/design/database.md`：權限表 `character_lito` 的欄位、部署前提、權限說明（**不需要新權限**，見下）、§7 新增「T103b 後的寫入流程」。
   - 部署權限清單：`tools/db-grants/generated/grants.sql` 與 `gen_grants.py` 的前提註解加上積分欄位。
   - `lito伺服器/LiTo381-Server/README.zh-TW.md`、`db/README.zh-TW.md`。

### 權限結論
正式權限範本對 `character_lito` 是**表級** `GRANT SELECT, INSERT, UPDATE`，新欄位自動涵蓋，**LiTo 執行帳號不需要新增任何權限**。只有執行升級 SQL 的管理帳號需要 `ALTER`。如果現場改成欄位級授權，`database.md` §2 已列出 UPDATE 必須包含的欄位。

## 怎麼驗證

- **已驗證**
  - 本機 `mvn -B -o -q -DskipTests test-compile` 通過（依規定本機不跑測試）。
  - 本機執行 `db/ci/build_ci_database.py --out scratch/ci-sql`：三個來源都套用了升級段（`30-control.sql` 出現 6 次欄位檢查）；`380_ac`、`380_ac_1` 是 MyISAM，`380_ac_2` 是 InnoDB。
  - 讀程式確認兩條寫入路徑的新欄位，都和 `last_result_id`／CAS 寫在同一條敘述。
- **已驗證（CI，PR #348，run 43054484101，head `92c03186`）**
  - `Server tests`（必過）：pass。
  - `Server tests (CI MySQL fixture)`（non-blocking）：`SettlementRankingMySqlIntegrationTest` 4/4 通過（MyISAM `380_ac_1`、InnoDB `380_ac_2` 各 2 案）、`SettlementRankingTest` 7/7 通過。全體 `Tests run: 7617, Failures: 39, Errors: 34`，和 T119 的基準（39／34）相同，沒有新增失敗。job 仍是紅燈，原因是 T119 時就存在的失敗。
  - fixture 加上升級 SQL 與 InnoDB 表之後，載入成功，其他測試也沒有因此改變結果。
- **推測**
  - MySQL 5.5 正式庫上執行 `ADD COLUMN` 的行為和 CI 的 5.7 相同（語法都是 5.5 就支援的），但沒有在 5.5 上實測。
  - drain 在結算當下才算月份，所以台北 05:00 跨月前後幾秒的結果可能算到新的月份。WAL 重放延遲時也一樣，以實際寫回的時間為準。
  - 營運如果在 WAL 還有未套用結果時改了 `battle.reward.*`，這幾筆會用新值計分（收據只凍結勝／敗／斷線，沒有凍結分數）。

## 未驗證什麼

- 實機：打一場、重啟伺服器後，排行榜／大廳顯示的積分與月榜仍保留；斷線敗扣 50；跨月第一場的月榜歸零（需要 Codex 或使用者在實驗庫做）。
- 正式 380_ac／380_ac_1／380_ac_2 尚未執行升級 SQL。**新版伺服器上線前一定要先執行**，否則會拒絕啟動。這是現場 DDL，由營運執行。

## 產出路徑

- `lito伺服器/LiTo381-Server/db/lito_character_ranking.sql`（新）
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/{SettlementSql,SharedDatabase,DurableBattleSettlement,GameStateStore}.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/SettlementRankingTest.java`（新）
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/SettlementRankingMySqlIntegrationTest.java`（新）
- `lito伺服器/LiTo381-Server/db/ci/build_ci_database.py`
- `docs/design/gameplay.md`、`docs/design/database.md`、`tools/db-grants/gen_grants.py`、`tools/db-grants/generated/grants.sql`
- `lito伺服器/LiTo381-Server/README.zh-TW.md`、`lito伺服器/LiTo381-Server/db/README.zh-TW.md`

## 給下一棒的提示

- **部署順序**：停機 → 對每個 `enabled=1` 來源執行 `db/lito_character_ranking.sql`（先改 `SET @lito_source`）→ 換新版 JAR → 啟動。啟動 log 若出現 `lacks ranking columns`，表示有來源漏跑。
- **`tools/db-grants/generated/` 已經跟 main 脫節，與本任務無關**：照 README 重跑 `scan_db_access.py`＋`gen_grants.py --update-doc` 會一起帶出 `Accounts.java`→`LegacyMySqlRepository.java` 改名後的大量行號變化、`characters` 的排除理由變化，以及 `grants.sql` 新增的 `380_db.spawnlist_time`、`系統_受擊無動作gfx` 兩張表。本任務只手動改了 `character_lito` 的欄位列與前提註解，其餘沒有提交。建議另開一張小任務重產並審查（新增的兩張表要先確認 CI 靜態庫有這兩張表，否則表級 GRANT 會失敗）。
- 正式 JAR 目前仍由 `tools/*/build.py` 逐層 patch（T002）。本變更在 `src/main`，正式 JAR 要等 T002 的鏈併回後才會帶到。打包前請確認正式 JAR 的 `SettlementSql`／`SharedDatabase` 來自 `src/main`。
- 實機核對清單：①打一場勝 → 大廳積分 +250；②重啟伺服器 → 積分仍在；③斷線敗 → −50（最低 0）；④直接查 `character_lito`，`last_result_id` 是最後一場收據，`monthly_period` 是當月。

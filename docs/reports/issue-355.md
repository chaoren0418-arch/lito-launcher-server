# Issue #355 / T124 — 斷線結算的壓力與重啟復原測試

分支 `task/355`，PR #363。全部是自動化測試，不啟動正式程序，也不連正式 DB。測試在 GitHub Actions 執行；本機只做編譯（`mvn -B -o -q -DskipTests test-compile`）。

## 做了什麼

### 1. 新測試（4 個類別＋1 個共用夾具）

| 測試 | 跑在哪個 job | 內容 |
|---|---|---|
| `SettlementJournalRecoveryTest`（7 案） | `Server tests`（必過，不需 DB） | WAL（`SettlementJournal`）在中止與重啟時的行為 |
| `GameHubDisconnectRoomSweepTest`（1 案） | `Server tests`（必過） | 壓力測試找到的建房競態的確定性回歸測試（見 §2-B） |
| `DisconnectSettlementRecoveryMySqlIntegrationTest`（22 案） | CI MySQL | 10 種中止點 × MyISAM／InnoDB 的重啟復原（20 案）；60 場的 SQL 端壓力；改名角色不再卡住其他結果 |
| `BattleDisconnectStressMySqlIntegrationTest`（1 案） | CI MySQL | 同一個 `GameHub` 同時開 3 個房間、每房 12 回合，在 6 種時機斷線與重連，走正式的結算路徑寫進 `character_lito` |
| `DurableSettlementCiFixture`（夾具） | — | 3 個 CI 來源的 Route（`380_ac`、`380_ac_1` 是 MyISAM，`380_ac_2` 是 InnoDB）；`CrashDriver` 包裝 MySQL 驅動，在指定位置模擬「程序死亡」：之後每個敘述都失敗，只有 `close()` 會傳到真的連線，所以 MySQL 會回滾未提交的交易並釋放 `GET_LOCK`，效果和客戶端斷線一樣 |

安全閘和 `SettlementRankingMySqlIntegrationTest` 相同：只有 `config/server.properties` 的 `database.user` 等於 workflow 的 `LITO_CI_DB_USER` 時才會執行，帶著正式設定的本機 checkout 不會跑到寫入敘述。

### 2. 找到並修正的缺陷

**A. 一筆永久失敗的結果會卡住其他所有待寫入結果**（`SettlementJournal.replay`、`SettlementSql.apply`／`drainAll`、`DurableBattleSettlement.retry`）
- 修正前：WAL 依隨機 UUID 檔名排序重放，遇到第一個失敗的檔案就停止（例如角色在來源庫被改名，`identity changed`）。排在後面的結果、同一場其他玩家的結果，以及 `drainAll`、背景 ticket 重試全部停住，直到營運處理。
- 修正後：失敗的檔案保留下來（不會刪掉，也不會寫進計數），其他檔案照常處理；第一個錯誤在全部處理完之後才拋出，其餘錯誤附在 suppressed。啟動時仍會因這筆失敗拒絕啟動（行為不變），但其他結果已經寫入。
- 同時讓 `replay` 容忍「檔案已被另一個重放者處理並刪除」（`NoSuchFileException` 時略過，刪除改用 `deleteIfExists`）。正式環境只有一個重放者；這種情況是在 CI 發現的：同一個 JVM 裡先前的測試啟動過背景 worker。

**B. 有人斷線時，別人剛建立的空房間會被刪掉**（`GameHub.nativeSummonOriginalDisconnect`）
- `ClientSession.createRoom` 的流程是先呼叫 `hub.createRoom`，再送一個 synthetic `C_LITO_JOIN` 加入（`S/ClientSession.java:10364-10377`）。修正前，`hub.disconnect` 會掃過**所有**房間並移除空房，所以任何玩家在這兩步之間斷線，新房間就會被刪掉，建房者加入時拿到 `unknown room`。這是 hub 壓力測試在 CI 第 2 輪抓到的（room 2 round 0）。
- 修正後：斷線只處理該 session 實際所在（member／ready／force／owner）的房間。原本靠這個全域掃描順便清掉的「結算未落盤時被清空的房間」，改由 `DurableBattleSettlement.retry` 在結算落盤後移除（只看有結算 context 的房間，剛建立的房間不會有 context，所以不會重新引入同樣的競態）。

### 3. 設計文件
`docs/design/gameplay.md`
- §3.8 加「斷線時機」「清理」兩列。
- §4.2 改正過時的「欄位」列（T103b 之後已有積分等欄位），並新增寫入順序、重啟復原、「寫入中」中止（現行行為）、凍結前中止（取捨）、單筆永久失敗（T124 修正）、WAL 檔損毀、同時兩個程序等列。
- 文末待驗證清單加上真實 TCP 斷線與真實程序被強制結束。

## 每個情境的結果

CI run 編號與 commit 見「怎麼驗證」。

### 復原（`DisconnectSettlementRecoveryMySqlIntegrationTest`，MyISAM `380_ac_1`、InnoDB `380_ac_2` 各一次）
每個情境都是同一場兩人戰：Knight 勝，Wizard 斷線敗。重啟方式＝新的 Route（runtime 快取為空）＋重開 WAL 目錄，執行 `replay`＋`drainAll`（也就是 `DurableBattleSettlement.open` 做的事）。驗證項目：勝敗、積分、斷線數、月計、`version` 只 +1、`last_result_id` 是本場結果、收據剛好 1 筆且 `applied=1`、WAL 已清空。之後再重啟一次，並重送舊 ticket，數值仍然不變。

| 中止點 | 結果 |
|---|---|
| 凍結前（WAL 還沒寫） | 兩人都**不計**（現行行為，見設計取捨） |
| WAL 已寫，第一次連 DB 前 | 各計 1 次 ✔ |
| 收據交易 COMMIT 時（回滾） | 各計 1 次 ✔ |
| 收據已提交，`character_lito` 寫入前 | 各計 1 次 ✔ |
| **寫入中**：`character_lito` UPDATE 已執行，回覆前中止 | 各計 1 次 ✔（`last_result_id` 擋住重複） |
| 計數已寫，收據 `applied=1` 前 | 各計 1 次 ✔ |
| WAL 重放已完成所有 SQL，刪檔前 | 各計 1 次 ✔ |
| 兩人同場：勝者已全部寫完，斷線者寫入前 | 各計 1 次 ✔ |
| 收據已提交，WAL 目錄跟著程序遺失 | 各計 1 次 ✔（啟動時掃描收據補寫） |
| 寫入後中止，復原途中又在下一筆寫入後中止 | 各計 1 次 ✔ |

「寫入中」的現行行為：第 ④ 步只有一列、一條敘述（勝敗、積分、斷線數、月計、`version`、`last_result_id` 一起寫）。MyISAM 的單列 UPDATE 不會只寫一半欄位，InnoDB 是 autocommit，所以只有「整列已寫」或「完全沒寫」兩種狀態，上表兩種都測過。

### 壓力（SQL 端，同一類別）
- 60 場、每場 2–4 人、6 個身分（3 個來源 × 2 個角色），5 種時機各 12 場：開戰前、戰鬥中（≥2 人留下時比賽繼續，WAL 會分兩次凍結）、全員斷線、正常結束、結果凍結後才斷線。共 111 筆結果。
- 每筆結果送 3 次（即時 record、回覆遺失後重試、worker 重放整個 WAL 檔），8 條執行緒打亂順序並行執行，同時還有一條執行緒在做 WAL 重放；分成兩個程序生命週期，中間重啟一次。
- 結果：6 個身分的計數完全等於模型（每筆結果套用 `SettlementSql.next` 一次），`version` 每筆 +1，111 筆收據全部 `applied=1`；再重啟一次並重送全部結果，數值仍不變。暫時性失敗（鎖忙碌／死結）：0。✔

### 壓力（GameHub 端，`BattleDisconnectStressMySqlIntegrationTest`）
3 個房間並行（各配一個 Knight 和一個 Wizard，來自不同來源），每房 12 回合，6 種情境輪流，每種情境每房 2 次：

| 情境 | 預期 | 結果 |
|---|---|---|
| 開戰前在房間內斷線 | 不是戰鬥斷線，不記錄 | ✔ |
| 戰鬥中一方斷線（加重複的 resolver 與矛盾的回呼） | 斷線者斷線敗 −50、對方勝 +250，各一次 | ✔ |
| 300 ms 內雙方斷線 | 無效場，不記錄 | ✔ |
| 結算中（勝方已送結果、敗方送結果前）斷線 | 維持一般敗（不是斷線敗），勝方不變 | ✔ |
| 結果畫面上斷線、再送一個「勝」的回呼 | 不是戰鬥斷線，不會推翻結果 | ✔ |
| 斷線後在判定前重新登入 | 舊連線記斷線敗；新連線收到的回呼被拒，不計 | ✔ |

每回合都檢查：房間已移除、`cleanupAllowed`（沒有未落盤的結算）、斷線的 session 已離開 hub。結束時檢查：只透過即時路徑，6 個身分的 `character_lito` 已等於模型；再重放一次 WAL 後數值仍不變、WAL 清空；全部登出後 hub 沒有 session，只剩頻道預設房，沒有任何被釋放的角色記憶體資料被重建。

共 36 回合、48 個 session（每次重連都是新 session）、48 筆結果；全部檢查通過。✔

### WAL（`SettlementJournalRecoveryTest`）
失敗的檔案不會擋住其他檔案；多個失敗時回報第一個；sink 完成後、刪檔前中止，重啟後會再送一次同樣的凍結結果（由結果 ID 去重）；已被其他重放者處理的檔案會略過；存檔中途中止留下的 `.tmp` 會被忽略；已提交的檔案損毀時拒絕啟動，檔案保留；同一目錄不能有兩個擁有者。

## 怎麼驗證

- **已驗證**
  - 本機 `mvn -B -o -q -DskipTests test-compile` 通過（依規定本機不執行測試）。
  - CI（PR #363）：
    - run 43147660301（head `1c75a416`）：`Server tests`（必過）通過。CI MySQL job：`DisconnectSettlementRecoveryMySqlIntegrationTest` 22/22、`SettlementJournalRecoveryTest` 6/6、`GameHubDisconnectRoomSweepTest` 1/1、`SettlementRankingMySqlIntegrationTest` 4/4 通過。`BattleDisconnectStressMySqlIntegrationTest` 的 36 個回合與「即時路徑 DB＝模型」都通過，但之後的 WAL 重放和背景 worker 同時刪同一個檔而失敗，已在 `a9332366` 修正（缺陷 A 的第二點）。全體 39 failures／35 errors：failures 和 T119 基準相同，errors 多出的 1 個就是這一案。
    - run 43150137478（head `a9332366`）：hub 壓力測試的 DB、房間、session 檢查全部通過，最後的「角色記憶體資料未被重建」失敗。原因是測試夾具的 `GameStateStore` 沒有開正式共用 SQL 模式使用的暫時性來源角色資料（`Main` 依 `config.ephemeralSourceProfiles()` 開啟），所以 `releaseSourceCharacter` 一律不釋放。這是夾具問題，不是程式缺陷，已在 `e7c4644d` 修正。
    - **最後一輪** run 43151965482（head `e7c4644d`）：`Server tests`（必過）通過。CI MySQL job 中，本任務 4 個類別 31 案（`SettlementJournalRecoveryTest` 7、`GameHubDisconnectRoomSweepTest` 1、`DisconnectSettlementRecoveryMySqlIntegrationTest` 22、`BattleDisconnectStressMySqlIntegrationTest` 1）與 `SettlementRankingMySqlIntegrationTest` 4 案全部通過。全體 `Tests run: 7699, Failures: 39, Errors: 34`，和 T119 基準相同，沒有新增失敗；job 仍是紅燈，原因是既有的失敗。
    - 過程中另有一輪（run 43144899123）卡在既有的 `ModernSiegeWireOracleTest`（不使用 GameHub，前後各輪都在約 1.5 秒內通過），已取消重跑。之後兩個壓力測試都加了逾時（獨立執行緒）與逾時時的 thread dump。
  - 設計文件引用的行號已重新讀過程式確認。
- **推測**
  - MyISAM 在 mysqld 本身當機（不是 LiTo 中止）時可能讓資料表損壞、需要 `REPAIR TABLE`。這和 LiTo 的寫入順序無關，沒有測試。
  - 改由 worker 移除的空房，不會像原本在 `hub.disconnect` 那樣送出房間刪除廣播；頻道清單可能要到下次刷新才消失。只會發生在「結算未落盤時房間被清空」的磁碟故障情況。
- **待驗證**
  - 實機雙人斷線（G24、CHECKLIST 16）：拔線、強制關閉客戶端，確認 300 ms 合併與結果畫面。
  - 真實程序被強制結束（`taskkill /F`）後重啟：本測試是用 JDBC 層模擬中止，沒有殺真正的 JVM。
  - 長時間（數小時）運轉：本測試是短時間高密度，不是長時間浸泡測試。

## 未驗證什麼
- 實機，以及真實的 TCP 中斷／程序強殺（見待驗證）。
- 隊伍模式（2v2 以上）、寵物競賽、Time Attack 的斷線專用規則沒有放進 GameHub 壓力測試（這次只做死鬥 1v1；SQL 端壓力有 2–4 人個人戰）。
- 正式 `LiTo.bin` 客戶端在房間被移除時的畫面。

## 產出路徑
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/SettlementJournal.java`
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/SettlementSql.java`
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/DurableBattleSettlement.java`
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/GameHub.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/SettlementJournalRecoveryTest.java`（新）
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/GameHubDisconnectRoomSweepTest.java`（新）
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/DisconnectSettlementRecoveryMySqlIntegrationTest.java`（新）
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/BattleDisconnectStressMySqlIntegrationTest.java`（新）
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/DurableSettlementCiFixture.java`（新）
- `docs/design/gameplay.md`（§3.8、§4.2、待驗證清單）

## 給下一棒的提示
- **改名角色的待寫入結果**：啟動 log 出現 `Settlement character identity changed: <db>/<objid>` 時，伺服器會拒絕啟動，但其他結果已經寫入。營運要確認該角色是否真的被改名或刪除；如果確定作廢，把 `380_lito.lito_battle_settlement` 對應 `result_id` 標成 `applied=1`，並把 `runtime/battle-settlements/` 裡含該 id 的 `.pending` 檔移出。這個流程還沒有工具，建議另開小任務（例如加一個「作廢」管理指令）。
- `runtime/battle-settlements/*.tmp` 殘檔目前不會自動清理（無害，只占空間）。
- 正式 JAR 仍由 `tools/*/build.py` 逐層 patch（T002）。本次修改在 `src/main`，要等 T002 鏈併回後才會進正式 JAR。
- 交給 Codex 的實機清單：①兩個客戶端開房對戰，一方拔線 → 另一方勝、斷線方 −50；②雙方同時關閉 → 無效場；③對戰中 `taskkill /F` 伺服器 → 重啟後查 `character_lito`，該場要嘛完全沒記、要嘛剛好記一次；④A 建房的同時 B 斷線 → A 正常進入房間。
- CI MySQL job 仍是 non-blocking，紅燈來自 T119 時就存在的 39／34 失敗；看本任務的測試結果要看 surefire 報告中的這 4 個類別。

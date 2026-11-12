# Issue #370 報告：最後成員離房時房間列已不存在，不再丟例外

來源：lito-modern T040 移交 3／4（chaoren0418-arch/lito-modern#299）。裁定：**接收**，已在 `src/main` 修正（PR #372）。

## 做了什麼

- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ClientSession.java`
  - `broadcastDefaultRoomReset(long roomId)`：開頭加 `hub.roomExists(currentChannelId, roomId)` 守衛。房間已不存在時印一行 log（`default room reset skipped; room row already removed …`）後直接返回，呼叫端 `exitRoomBeforePolyMe` 繼續送 `S_CC` 房間列表與大廳目錄刷新。
  - 房間存在時行為不變：照舊取快照、對頻道其他成員廣播 reset 列；「房間存在但快照缺列」仍丟原本的 `IOException`（這個不一致狀態不吞掉）。
  - 與提案 patch 的差異只有多一行 log。
- 新測試 `src/test/java/local/lito381/server/ClientSessionLastRoomMemberExitTest.java`（真實 `ClientSession`＋selftest 設定＋`MemoryAccountRepository`，不需要 `config/server.properties`，CI 會跑）：
  1. `lastResultMemberLeavesAfterTheEmptyCustomRoomWasAlreadyRemoved`：A、B 在自訂房以 `countdownWork` 進入真正的 BATTLE，`playerDefeated` → `serverEndBattle` → `endBattle`，兩人進 RESULT。B 先 `C_D0` 離房（收到自己的 `S_D0` 與 `S_CC`，回 CHANNEL，房間仍在）。接著直接呼叫 `hub.leaveRoom(roomId, A)` 讓空的自訂房被移除，A 仍停在 RESULT 且帶著舊房號。最後 A 送 `C_D0`：收到自己的 `S_D0`、`S_CC` 房間列表（預設房那一列），回 CHANNEL、`currentRoomId == 0`，頻道只剩預設房。
  2. `lastMemberLeavingTheDefaultRoomStillBroadcastsItsResetRow`：A 是預設房唯一成員，送 `C_D0` 離開；預設房仍存在且成員為空；待在另一間房（ROOM 階段，不會收到一般大廳刷新）的頻道成員 P 收到預設房完整的 6 個 `S_CC` reset frame（sub-opcode 0–5，房號皆為預設房）。
  - 類別層級 `@Timeout(120s, SEPARATE_THREAD)`：原因見下方「另外發現」。

## 怎麼驗證

### 已驗證
- **修正前紅燈**：commit `92d67f23`（只有測試），PR #372 CI run 43264734337 的 `Server tests`：`Tests run: 7436, Failures: 0, Errors: 1`，唯一錯誤是
  `ClientSessionLastRoomMemberExitTest.lastResultMemberLeavesAfterTheEmptyCustomRoomWasAlreadyRemoved:62 » IO default room reset snapshot missing: 2`；同一類別的預設房測試通過。這就是 Issue 描述的例外，在當版 `src/main` 上重現。
- **修正後綠燈**：commit `8615f828`（修正＋已合併 origin/main `b42e01d1`），CI run 43326449858：
  - `Server tests`：`Tests run: 7447, Failures: 0, Errors: 0, Skipped: 117`，BUILD SUCCESS；`ClientSessionLastRoomMemberExitTest` 2/2 通過，log 有 `default room reset skipped; room row already removed room=2 channel=0 player=LastExitA`。
  - `Server (Java 17 / Maven)`、可重現 JAR、Semantic gap、Launcher、Build-Package 全部通過。
  - `Server tests (CI MySQL fixture, non-blocking)`：failure，`Tests run: 7714, Failures: 39, Errors: 34`；新測試在這個 job 也 2/2 通過。main（run 43274590830）同一 job 是 `7712, Failures: 39, Errors: 34`，失敗數相同，差的 2 個就是本 PR 新增且通過的測試——與本 PR 無關。
- 本機 `mvn -B -q -DskipTests test-compile`：main 與 test 編譯通過（依本機限制沒有在本機跑測試）。
- 驗收條件 1（修正前 `IOException`、修正後離房完成並收到 `S_CC`）與 2（預設房最後一人離開仍廣播 reset 列）皆由上述 CI 結果涵蓋。

### 推測
- 自然時序（「結算返回晚於空房被移除」）在正式環境怎麼發生，沒有重現。測試是用直接呼叫 `hub.leaveRoom` 造出「session 還帶著舊房號、房間已刪」的狀態；此狀態下 `hub.leaveRoom` 回傳 false、`roomMembers` 為空，因此走到 `broadcastDefaultRoomReset`——這條分支已由測試實際走過，但觸發它的線上時序仍是推測。
- 房間已刪時，頻道其他成員理應在房間被刪當下就收過 `S_LITO_DELETE_ROOM`，所以這裡不補送任何列。

### 待驗證
- 實機（3.81C 原版客戶端／lito-modern 客戶端）最後一名成員從結算畫面返回大廳的畫面與房間列表：交 Codex／使用者驗收。對應客戶端 G06.1／G06.2。
- 正式 JAR（0.6.71／E149）沒有改；這個修正只在 `src/main`，部署需另走 patch 鏈（T002）。

## 另外發現（不在本張範圍，未修）

第一次推送（`92d67f23`）的 `Server tests (CI MySQL fixture)` job 卡了 6 小時被 GitHub 取消。查 log 後確認**不是本 PR 造成**，是既有的鎖順序死結：

- 已驗證（run 43264734337 MySQL job log 的 thread dump）：`BattleDisconnectStressMySqlIntegrationTest`（#355）先逾時 240 秒，dump 列出 `deadlocked=[947, 2047, 2045]`：
  - 執行緒 A：`GameHub.disconnect` → `resetOrRemove`（持有 `GameHub` 鎖）→ `DurableBattleSettlement.cleanupAllowed`（等 `CONTEXTS` 鎖，`DurableBattleSettlement.java:79`）。
  - 執行緒 B：`DurableBattleSettlement.begin` → `context()`（持有 `CONTEXTS` 鎖，`:28`）→ `new Context` → `NativeRoomRules.testRoom`（等 `GameHub` 鎖，`:9`）。
- `CONTEXTS` 是 static、全 JVM 共用，死結後所有 hub 的 `leaveRoom`／`resetOrRemove`／battle-clock 都卡住。本測試排在它後面、當時沒有逾時設定，所以整個 job 掛到 6 小時上限。
- 這個死結是偶發（同一測試在 main run 43274590830 與本 PR run 43326449858 都是 6–7 秒通過），而且是**正式程式碼**的鎖順序問題：同一個 hub 上「一人開戰（`begin`）」與「另一人斷線（`disconnect`）」同時發生就可能卡死整個伺服器。
- 本 PR 只做了防護：新測試加 `@Timeout`，再遇到也只會讓該測試失敗，不會拖住 job。

## 產出路徑
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ClientSession.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/ClientSessionLastRoomMemberExitTest.java`
- `docs/reports/issue-370.md`
- 分支 `task/370`，PR #372

## 給下一棒的提示
- 主控：建議另開一張 code 任務修 `DurableBattleSettlement.context()` 的鎖順序（例如在取 `CONTEXTS` 鎖之前先算好 `NativeRoomRules.testRoom(hub, room)`，不要在持有 `CONTEXTS` 時進 `GameHub` 鎖）。證據是 run 43264734337 MySQL job 的 thread dump。其他沒有 `@Timeout` 的 `GameHub` 測試遇到同一死結也會把 MySQL job 拖到 6 小時。
- lito-modern：本張結果已留言在 chaoren0418-arch/lito-modern#299；`修補提案/房間最後離開/` 可以刪除。客戶端不需要改封包處理——最後成員離房收到的仍是自己的 `S_D0` 加完整 `S_CC` 房間列表。
- 實機驗收清單：兩人開自訂房打完一場 → 輸家先按返回 → 贏家最後按返回 → 兩人都回到大廳、房間列表正常、伺服器 log 沒有 `default room reset snapshot missing`。

# Issue #366 報告：BATTLE 中觀戰者主動離場只移除自己

## 做了什麼

- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ClientSession.java`
  - 新增 `battleObserverMayLeaveAlone()`：`phase == BATTLE && observer && !rentalTownRoomState && currentRoomId != 0` 且不在 `hub.battleResultCombatants(roomId)`（`battleRoster`）內。
  - `exitRoomBeforePolyMe`（`C_D0`）：BATTLE 時只有「不是可獨自離場的觀戰者」才呼叫 `settleRequestedBattleExit()`；觀戰者改走既有的一般離房流程（自己 `S_D0` → 同房成員 `S_D0` → `hub.leaveRoom` → `phase = CHANNEL` → `S_CC` 房間列表 → 大廳房間目錄刷新）。不呼叫 `serverEndBattle`／`endBattle`。
  - `quitFight`（`C_D9`）：觀戰者先回 `S_F0(0)`，再走同一條離房流程（`leaveBattleAsObserver`）。參戰者行為不變。
- 新測試 `src/test/java/local/lito381/server/ClientSessionBattleObserverExitTest.java`（selftest 設定＋`MemoryAccountRepository`，不需要 `config/server.properties`，CI 會跑）：
  1. `observerExitRoomLeavesAloneAndCombatantsStillSettleNormally`：兩名參戰者＋一名觀戰者以 `countdownWork` 進入真正的 BATTLE；觀戰者 `C_D0` 後，觀戰者收到自己的 `S_D0`、回 CHANNEL；兩名參戰者各只收到觀戰者的 `S_D0`；戰鬥仍 RUNNING、`roomMembers`／`battleRoster` 只剩兩人、無勝敗紀錄。接著 `playerDefeated(A, B)` 正常結束、兩人 `serverEndBattle` 收到 `S_D3`、進 RESULT；觀戰者沒收到 `S_D3`／`S_F0`。
  2. `observerQuitFightLeavesAloneAndCanRejoinTheSameBattleAsObserver`：觀戰者 `C_D9` 收到 `S_F0(0)` → `S_D0`；以觀戰身分重新加入同一場進行中的戰鬥（`tryJoinRoom` 成功、roster 不變），第二次 `C_D0` 一樣隔離；最後參戰者 B 自己 `C_D9` 仍正常走 `player-withdrawal` 結算，兩人進 RESULT，觀戰者沒收到結果。
- 契約文件：`docs/reference/battle-observer-exit-contract.md`（封包順序、其他成員收到什麼、不受影響的行為）。

## 怎麼驗證

### 已驗證
- `mvn -B -q -DskipTests test-compile`：main 與 test 編譯通過（本機）。
- 靜態核對：一般離房流程在 BATTLE 下的副作用——`setReady` 只在 ROOM 執行；房主廣播只在 ROOM／RESULT；`NativeRoomRules.restoreBot` 在 `battleStarted` 時直接返回；`GameHub.leaveRoom` 的 `cancelCountdownIfNotReady` 只在 COUNTDOWN 生效、`resetOrRemove` 在房內仍有成員時不動作；均不碰 `battleRoster`／`battleForces`／match。

### 推測
- 觀戰者在戰場沒有實體物件，因此同房成員不需要 `S_REMOVE_OBJECT`（與既有觀戰者斷線 `notifyPeersOfDisconnect` 一致）。
- 觀戰者若剛好是房主，`hub.leaveRoom` 會轉移房主，但 BATTLE 中不廣播 `S_LITO_GAME_INFO` 房主變更（沿用一般離房在 BATTLE 不廣播的既有規則）；結算後房主列是否需要補送，未驗證。

### 待驗證
- 單元測試本機未執行（本機規定不得執行測試），以 PR 的 GitHub Actions CI 結果為準。
- 原版 3.81C 客戶端／lito-modern 客戶端在戰鬥場景收到自己的 `S_D0`（與 `C_D9` 時先 `S_F0(0)`）後是否正確拆場景回大廳：需實機驗收（交 Codex／使用者）。
- 重新加入觀戰時走真正的 `C_CF` → late-observer `startBattle` bootstrap，本測試只以 `hub.tryJoinRoom` 驗證伺服器准入與 roster 不變，未驗證完整 bootstrap 封包。

## 產出路徑
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ClientSession.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/ClientSessionBattleObserverExitTest.java`
- `docs/reference/battle-observer-exit-contract.md`
- `docs/reports/issue-366.md`
- 分支：`task/366`

## 給下一棒的提示
- 客戶端（lito-modern#189）：移除「觀戰中退出需服務端支援」的拒絕；觀戰中退出送 `C_D0`，等自己的 `S_D0`（roomId＋本機顯示名）後回房間列表；收到別人的 `S_D0` 只更新觀戰名單。契約見 `docs/reference/battle-observer-exit-contract.md`。
- 實機驗收清單：兩名參戰＋一名觀戰 → 觀戰者按退出 → 觀戰者回大廳、參戰者畫面無結算且可繼續移動攻擊 → 打完正常結算 → 觀戰者再進同房觀戰。
- 正式 JAR 未改；若要部署需另走 `tools/*/build.py` patch 鏈（T002）。

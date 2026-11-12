# Issue #368 報告：租裝返回時 quitFight／exitRoom 被 BATTLE 結算攔截（lito-modern T040 移交 1／4）

## 結論

接收並已在 `src/main` 修正。租裝城鎮內已排定的返回／轉場，`C_D9`、`C_D0` 不再被第一個 BATTLE 判斷導向戰鬥結算；一般戰鬥退出與 #366 觀戰者離場不變。正式 JAR 未改。

## 做了什麼

- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ClientSession.java`
  - 新增 `stagedRentalTownQuitFight()`：`rentalTownRoomState && (rentalTownTransferPending || pendingTransition == RESTART_AWAIT_QUIT_FIGHT)`。
  - 新增 `stagedRentalTownReturn()`：`rentalTownRoomState && pendingTransition == RESTART_AWAIT_RETURN`。
  - `quitFight()`：BATTLE 下依序為「#366 觀戰者獨自離場」→「不是已排定租裝轉場 → `settleRequestedBattleExit()`」→ 其餘（含已排定租裝轉場）進原本的 `else`，執行既有的傳送轉場／`RESTART_AWAIT_QUIT_FIGHT` 分支。
  - `exitRoomBeforePolyMe()`：BATTLE 結算條件多加 `!stagedRentalTownReturn()`，放行後走既有的 `completeRestartReturn(true)`。
  - 兩個判斷與 #366 的 `battleObserverMayLeaveAlone()`（要求 `!rentalTownRoomState`）互斥，沒有交疊。
- 測試
  - 新檔 `src/test/java/local/lito381/server/ClientSessionRentalTownReturnTest.java`（真實 `ClientSession`、兩名村民、selftest 設定＋`MemoryAccountRepository`，不需要 `config/server.properties`）：
    1. `restartReturnChainLeavesTheTownWithoutBattleSettlementAndCanReenter`：經 `handle()` 走 `C_RESTART → S_RESTART → C_D9 → S_F0(0) → C_D0 → S_D0 → S_REMOVE_INVENTORY… → S_F5`；離開者回 LOBBY、城鎮欄位全清、`pendingTransition = RESTART_AWAIT_SOURCE_REFRESH`；另一名村民只收到 `S_REMOVE_OBJECT`＋`S_D0`，沒有其他封包、仍在城鎮；之後離開者以 `C_CA(0) → C_D9 → C_CF` 重新入村成功。
    2. `unstagedOutOfOrderAndDuplicateRequestsStayOrdinaryWithdrawals`：沒有 pending 的 `C_D9`／`C_D0`、`C_RESTART` 後直接 `C_D0`、重複 `C_D9`，都不送封包、狀態不變；之後 `C_D0` 仍正常完成一次；完成後重送 `C_D0` 無封包。
    3. `stagedTransferStoneQuitFightLeavesTheTownFromBattlePhase`：`rentalTownTransferPending`（同端點）時 BATTLE 下 `C_D9` 完成離場並回 `S_F0(0)`。
  - `ClientSessionBattleObserverExitTest` 加 2 案：一般戰鬥的參戰者即使有 `RESTART_AWAIT_QUIT_FIGHT`（`C_D9`）或 `RESTART_AWAIT_RETURN`（`C_D0`），仍是 `player-withdrawal` 結算（兩人收 `S_D3`、進 RESULT）。原有 2 案（#366 行為）未動。
  - `NativeLitoRentalTeleportEndpointCatalogTest.exactTeleporterActionSendsStatusThenDirectEndpoint`：原本斷言「BATTLE 下已排定傳送的 `C_D9` 不送任何封包」（#138 記下的 0.6.71 行為），改為新行為：BATTLE 下直接完成設定端點 handoff。**這是本張刻意的行為變更。**
  - `ClientSessionRentalTownDirectoryTest`：只補註解；「沒有 pending 的 `C_D0` 在城鎮內不動作」的既有斷言保留，對應驗收條件 3。
- 契約文件 `docs/reference/rental-town-return-contract.md`（封包順序、放行條件、錯序與重複的處理）。

## 怎麼驗證

### 已驗證
- 本機 `mvn -B -q -DskipTests test-compile`：main 與 test 編譯通過。
- 靜態核對：`startRentalTownScene` 後 session 是 `Phase.BATTLE`（`ClientSessionRentalTownDirectoryTest` 既有斷言）；`rentalTownGameplayOpcodeAllowed` 允許 `C_RESTART`／`C_D9`／`C_D0`；`completeRestartReturn`、`notifyRentalTownDeparture`、`prepareRentalHotelDeparture`、`removeRentalTownServiceItems` 都不依賴 phase。
- CI：見文末「PR CI 結果」。

### 推測
- 重複 `C_D9` 在客戶端不需要第二個 `S_F0`（伺服器現在不回）。提案紀錄 `duplicate_ack_verified=false`，本張只確定伺服器端狀態不被破壞。
- `C_RESTART` 與傳送石探針（`transferStoneProbeHandoffPending`）同時成立時，`quitFight()` 先走探針分支；這是既有的分支順序，本張沒改，也沒測這個組合。

### 待驗證
- 實機：原版 3.81C 客戶端或 lito-modern（T055／G06.3c、U-M3-1）在租裝城鎮按返回，走完到角色清單並重新入村。
- 步驟 4 的 `C_C8` 來源刷新：測試裡以直接把 `pendingTransition` 設回 `NONE` 代替，沒有執行真正的 `C_C8`。
- 旅館租約（map 501／503）清理、屋主返回時共享訪客回 map 500、跨端點傳送（斷線重連到另一埠）：提案紀錄在舊 JAR BBC59308 上通過，本張沒有在當版 src/main 重跑；程式碼路徑與非 BATTLE 時相同。
- 正式 JAR（E149）未含本修正；部署要另走 patch 鏈。

## 產出路徑
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ClientSession.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/ClientSessionRentalTownReturnTest.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/ClientSessionBattleObserverExitTest.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/NativeLitoRentalTeleportEndpointCatalogTest.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/ClientSessionRentalTownDirectoryTest.java`
- `docs/reference/rental-town-return-contract.md`
- `docs/reports/issue-368.md`
- 分支 `task/368`，PR #374

## 給下一棒的提示
- 主控：本張等於「接收」移交 1／4。`quitFight()`／`exitRoomBeforePolyMe()` 對 0.6.71 的語意差異是刻意的（`Semantic gap` job 為 non-blocking；`exitRoomBeforePolyMe` 列在 `docs/combat-merge/effects-approved.txt`，需要的話請補核准註記）。
- lito-modern：已在 chaoren0418-arch/lito-modern#299 留言；`修補提案/租裝返回/` 可依 T040 刪除。客戶端契約見 `docs/reference/rental-town-return-contract.md`。
- 實機驗收清單：入村 → 按返回／重新開始 → 回到角色清單（無結算畫面）→ 重新選租裝入村；兩人同村時另一人只看到對方消失。再測一般戰鬥中退出仍出結算。

## PR CI 結果
PR #374，run 43264993090／43264993083，commit `215c5cd7`（程式與測試的 commit；之後只加了本報告）：

- `Server tests`：通過。`Tests run: 7439, Failures: 0, Errors: 0, Skipped: 117`，其中
  - `ClientSessionRentalTownReturnTest` 3／3
  - `ClientSessionBattleObserverExitTest` 4／4
  - `NativeLitoRentalTeleportEndpointCatalogTest` 4／4
  - `ClientSessionRentalTownDirectoryTest` 10／10
- `Server (Java 17 / Maven)`、`Server reproducible JAR`、`Launcher`、`Build-Package.ps1 zip`、`Semantic gap (non-blocking)`：通過。
- `Server tests (CI MySQL fixture, non-blocking)`：寫報告時還在執行，結果未知。這個 job 在 main 上本來就是失敗狀態（見 #366 報告），不擋合併。

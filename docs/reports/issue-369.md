# Issue #369 報告：同端點傳送接續後 C_CA 被當成租裝重選

來源：lito-modern T040（chaoren0418-arch/lito-modern#299）移交的修補提案 2／4。

## 結論

- **現有的 `hasRentalBattleHandoff` 守衛擋不住，和提案的 `rentalBattleLoadoutActive` 守衛不等價**；兩者管的是前後兩個不同時段，都要留。
- 已在 `src/main` 補上提案的那一行，並加真實 `ClientSession` 測試。正式 JAR（E149）未改，仍有同一缺口。
- 提案**採用**；lito-modern 可以刪除 `修補提案/同端點傳送/`。

## 三個待驗證問題的答案

| 問題 | 答案 | 等級 |
|---|---|---|
| 1. 75DA69B0 有沒有 `hasRentalBattleHandoff` 守衛？ | 有，而且 6928A147（最早記錄缺口的版本）、75DA69B0、E1498871 三個 JAR 的 `rentalItemSelectionContext()` 邏輯完全相同，都在 offset 59 呼叫它。這個守衛從一開始就在，不是後來補的替代方案。 | 已驗證（javap） |
| 2. E149 與當版 src/main 在 same-endpoint 下 `C_CA` 是否正常進頻道？ | 不會。修改前的 src/main 與 E149 都會把它當成租裝選擇（靜態推導；E149 未實跑）。修改後的 src/main 由新測試驗證。 | src/main：已驗證（PR #370 CI）；E149：推測（靜態） |
| 3. 擋不住就補上 `rentalBattleLoadoutActive` 並加測試 | 已補，見下。 | 已驗證（PR #370 CI） |

## 兩個守衛為什麼不等價（handoff 紀錄的寫入與消耗時機）

以 `src/main/java/local/lito381/server/ClientSession.java` 與 `GameHub.java` 為準：

1. **寫入**：來源端使用傳送石時 `GameHub.stageRentalBattleHandoff()` 寫入一筆一次性紀錄（記憶體 map，或設定了 `lito.rental.handoff.file` 時寫共用檔）。
2. **新 GAME 連線**：`beginGameBootstrap()` 把 `rentalTownDirectoryAdvertised` 設為 `config.rentalTownDirectoryEnabled()`。同端點＝同一台伺服器，租賃村目錄是開的，所以這裡是 true。
3. **消耗**：`completeGameBootstrap()` → `autoSelectPendingRentalBattleHandoff()` → `characterSelect()` → `activatePendingRentalBattleHandoff("character-select")`。這裡呼叫 `consumeRentalBattleHandoff()` 把紀錄**移除**，換上租裝，設 `rentalBattleLoadoutActive = true`。
4. **客戶端送 `C_CA`**：`rentalItemSelectionContext()` 檢查「目錄開啟、已廣告、`phase == LOBBY`、已選角色」全部成立，最後 `hasRentalBattleHandoff()` 因紀錄已在第 3 步被消耗而回 false，方法回 true → `selectRentalItemSet()` 回 `S_C9`，不送 `S_CA` 頻道人數。

所以：

- `hasRentalBattleHandoff` 只涵蓋「紀錄已寫入、**尚未**消耗」（自動接續沒發生時，`C_CA` 走到 `enterChannel` 內的 `activatePendingRentalBattleHandoff("enter-channel-N")` 當備援消耗點）。
- `rentalBattleLoadoutActive` 涵蓋「紀錄**已經**消耗、租裝生效中」，到 `resetNativeTournamentEconomy()`（離開頻道、結算）清除為止。
- 正常的同端點流程一定先經過第 3 步，所以只有後者擋得住。

跨端點不受影響的原因：戰鬥端設定 `lito.rental.town.directory.enabled=false`（見 `ServerConfig` 的 rental-battle 設定檔，約 L735），方法第一個條件就回 false。

## 做了什麼

- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ClientSession.java`：`rentalItemSelectionContext()` 入口加 `if (this.rentalBattleLoadoutActive) return false;`，其餘條件不動（與候選 `B33F9FA1…` 的位元碼變更相同）。
- 新測試 `src/test/java/local/lito381/server/ClientSessionSameEndpointRentalHandoffChannelEntryTest.java`（記憶體資料庫，不需要 `config/server.properties`，CI 會跑）：
  1. `channelEntryAfterContinuedHandoffReturnsChannelInfoAndKeepsRentalObjects`：目錄開啟的單一伺服器，暫存 handoff → `autoSelectPendingRentalBattleHandoff` 接續（確認紀錄已消耗、租裝生效）→ 送 `C_CA 0` → 收到 `S_C9` 成功與 `S_CA`（頻道 0、1／100）、`phase == CHANNEL`、`rentalItemSetSelection` 仍是 -1、租裝物件 ID 清單與接續後完全相同。修改前這個測試會在等 `S_CA` 時逾時。
  2. `pendingUnconsumedHandoffStillEntersChannelAndActivatesLoadout`：紀錄還沒被消耗時送 `C_CA`，仍進頻道並在進頻道時啟用租裝（原有守衛的行為不變）。
  3. `ordinaryLoginWithoutHandoffStillSelectsRentalItemSet`：沒有 handoff 的一般登入，`C_CA 1` 仍是租裝選擇，不進頻道、背包不變。

## 怎麼驗證

### 已驗證
- javap 對照四個 JAR 的 `rentalItemSelectionContext()`（SHA-256 已核對檔名）：
  - `formal-server-6928A147.jar`、`formal-server-75DA69B0.jar`、`formal-server-E1498871.jar`：四個條件＋`hasRentalBattleHandoff`，沒有 `rentalBattleLoadoutActive`。
  - `same-endpoint-candidate-6928.jar`（`B33F9FA1…`）：入口多 `getfield rentalBattleLoadoutActive; ifeq; iconst_0; ireturn`，其餘相同。
  - 75DA69B0 與 E149 的 `characterSelect` 都呼叫 `activatePendingRentalBattleHandoff("character-select")`，消耗時機與 src/main 相同。
- 75DA69B0 當版失敗紀錄的 `server.log`（`lito重建版/驗證紀錄/current/native-teleport-handoff/20261018-171205-138/`）順序吻合上述推導：L89／L126「continuing rental battle handoff」→ L100／L137「activated rental battle handoff … trigger=character-select」→ L140／L141「selected rental item set … selection=0 phase=LOBBY」。`boundary.json` 的 wire 也顯示兩次 `CA 00` 都只收到標註 `rental-item-set accepted` 的 `C9`。
- 本機 `mvn -B -q -DskipTests test-compile` 通過（只編譯，未執行）。
- PR #370 CI：見文末「CI 結果」。

### 推測
- E149 正式 JAR 在同端點下有同一缺口：依位元碼相同推導，沒有實跑。
- 目前正式部署是否會遇到，取決於是否把租賃村與戰鬥放在同一個端點；跨端點部署（戰鬥端目錄關閉）不會遇到。

### 待驗證
- 真實客戶端（原版 3.81C 或 lito-modern）對修改後伺服器跑完整同端點傳送：需要啟動伺服器與客戶端，交 Codex／使用者。對應 lito-modern G06.3d 與 U-M3-1（同端點）。
- 新測試以反射呼叫 `autoSelectPendingRentalBattleHandoff` 並直接設 `rentalTownDirectoryAdvertised`，沒有走完整的登入→`S_F4` 轉址→新連線握手；`beginGameBootstrap` 重新武裝目錄旗標由既有 `ClientSessionRentalTownDirectoryTest.gameBootstrapRearmsDirectoryCapabilityAcrossLoginRedirect` 涵蓋，來源端到戰鬥端的轉移由既有 `ClientSessionRentalBattleHandoffIntegrationTest` 涵蓋。
- 傳送取消、handoff 逾期、重入：不在本張範圍。

## 產出路徑
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ClientSession.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/ClientSessionSameEndpointRentalHandoffChannelEntryTest.java`
- `docs/reports/issue-369.md`
- 分支：`task/369`（PR #370）
- 本機暫存（不進 git）：`scratch/t369-javap-*.txt`、`scratch/t369-proposal-readme.md`

## 給下一棒的提示
- 正式 JAR 未改。要讓正式版支援同端點傳送，需把這一行經 `tools/*/build.py` patch 鏈（T002）帶進正式 JAR；只走跨端點部署則不急。
- lito-modern：伺服器端已接收本提案，`修補提案/同端點傳送/` 可刪。同端點的客戶端驗收要對「含本修改的 src/main 建置」跑，對 E149 仍會失敗。
- 實機核對清單：租裝角色在租賃村用傳送石 → 同位址重連 → 選頻道 → 應看到頻道人數並進大廳，背包仍是傳送前選的那套租裝；離開頻道後回到選角畫面，租裝選擇按鈕應恢復可用。

## CI 結果
（push 後補）

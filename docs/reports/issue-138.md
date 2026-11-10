# Issue #138 — T020-B-land：B 暫存區併入 src/main

## 結論

B 擁有的 303 個外層類別已全部併入 `src/main/java/local/lito381/server/`，`mvn -DskipTests package` 在本機成功（AsRecord 轉換 54 個類別）。
本輪套用了[主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/138#issuecomment-6777167802)的 7 點。

**語意比對（r3，本輪重跑）**
- **B 範圍**：1,384 個 entry 無缺漏、無多出、無該移除的 entry。
  - 唯一的 DIFF 是 `ClientSession`，它的 184 個方法與 `b09-shape-diffs.md` 一一對應（#70 已裁定交 M2）。
  - DEVIATION 4 個：既有的 2 個，加上本輪核准的 2 個 method-diff。
- **T029**：8 個方法對 `base-0.6.71.jar` 為 SAME（7 個）或 SAME_EFFECTS（1 個）。
- **A 範圍**：692 個 entry 全部可接受，DIFF＝0。
- **整顆 JAR**：其他類別差異為 0。

**`mvn -B test`（以 PR #315 的 CI 為準）**
- 第 5 輪：6,052 項，Failures 5、Errors 1，比第 3 輪的 37 項少了很多。
- 第 6 輪（`751c7a75`）：Failures 2、Errors 1。修正最後 1 項 fixture 後，第 7 輪預期只剩 Ckew、Rescue 2 項，結果見 PR #315。
- 剩下的 2 項（Ckew、Rescue）是**候選與正式 0.6.71 行為不同、玩家看得到**的差異。依裁定第 7 點不自行決定，交主控裁定。**這 2 項在裁定之前 CI 不會全綠。**

**需要主控裁定（needs:claude）**
1. **Ckew 入口 NPC 首次對話**：候選會讓 L1Quest NPC 轉向說話者並送 `S_CHANGE_DIRECTION`，正式版不會。
2. **Rescue 觀察者收不到 `S_REMOVE_OBJECT`**：候選的 `NativeStaticNpcMotion.nativeSourceWeaponFrame` 會把可見 L1Quest 靜態 NPC 的移除封包吞掉，正式版直接送出。
3. **取鎖順序有真實死鎖路徑（裁定 3 的結果）**
   - 正式 0.6.71 已有的路徑：client 執行緒以 hub → population 取鎖，例如 C_ATTACK、魔法、馴服、斷線。
   - 候選新增的路徑：battle clock 的 monster tick 以 population → hub 取鎖（`moveToward` → `NativeStaticNpcMotion.occupies` → `NativeStaticCancellationReplacement.live`）。
   - 兩者同時發生就會互相等待。battle clock 是單執行緒，只能防止 tick 與 tick 之間的死鎖，擋不住 client 與 battle clock 之間這種。
4. **（提醒）租屋城鎮的退戰流程**：正式版與候選都把「租屋城鎮按離開（C_D0）」和「租屋傳送師（C_D9）」當成一般退戰處理。舊 src/main 原本修好了這兩條流程，B-land 之後這個修正消失。
   - 依裁定 7，測試已改成候選＝正式的行為。
   - 相對舊 src/main，這是玩家看得到的退步。要不要補回，請裁定。
5. **（提醒）死亡懲罰程式碼**：dispatcher 還留在 src/main，但正式版與候選都走不到。要不要清掉可以留給 M2。

## 本輪依裁定做了什麼

| 裁定 | 處理 |
|---|---|
| 1 未登入斷線拋例外 | `NativeSummonRetirement.beforeDisconnect`：session 無法組出來源角色／帳號身分時直接 return（`sourceCharacterIdentity()`／`sourceAccountIdentity()` 拋 IAE 就略過），之後照常走 `nativeSummonOriginalDisconnect` 的完整清理。<br>登記 `deviations.txt` method-diff。<br>新增 `NativeSummonRetirementUnboundDisconnectTest` 2 項：未登入 session 斷線不拋例外且離開 hub；已認證但沒選來源的 session 斷線後釋放帳號。<br>原本受影響的 4 項＋改寫的 2 項在第 4 輪 CI 全部通過 |
| 2 `TamedRelease` 條件反了 | 改成 `hidden.objectId() != released.objectId()` 時才拋例外（正式版的 `ifne`），登記 method-diff。<br>新增 `ClassicWorldMonsterCatalogTest#tamedReleaseAcceptsSameActorAndRejectsSnapshotsOfDifferentActors`：同一 actor 正常釋放不拋例外；不同 actor 的快照會拋 IAE。<br>原失敗項與新測試在第 4 輪 CI 都通過。<br>整顆 JAR 對正式比對，此類別變成 SAME |
| 3 鎖順序 | `b09-shape-diffs.md` 末尾新增一節「M2 回歸：monster tick／companion tick 取鎖順序」。client 執行緒路徑見下文「取鎖順序」，找到可能死鎖的路徑，所以標 needs:claude |
| 4 文字稽核改行為測試 | 見下文「原始碼文字稽核」 |
| 5 `exceptions-exempt` | 已接受，不需再處理 |
| 6 TournamentPresence 接縫 | 不補回。前輪已改寫的 2 項測試保留，列在下文 |
| 7 其餘失敗 | 逐案分類，見下文「測試逐案判斷」 |

偏離登記（`docs/combat-merge/deviations.txt`）：
```text
local/lito381/server/NativeSummonRetirement#beforeDisconnect(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClientSession;)V  method-diff  # approved: …#issuecomment-6777167802
local/lito381/server/ClassicWorldMonsterPopulation$TamedRelease#<init>(…$Snapshot;…$Snapshot;)V  method-diff  # approved: …#issuecomment-6777167802
```

## 前輪已完成的落地工作（摘要）

| 項目 | 內容 |
|---|---|
| 搬入 | `merge-inputs/50635f7e/staged/local/lito381/server/*.java` 303 份：覆蓋 84 個同名檔、新增 219 個檔（`docs/reports/issue-138/land.py`） |
| 第 85 個既有類別 | `LegacyMySqlRepository` 原本放在 A 擁有的 `Accounts.java` 尾端，現改為 staged 的獨立檔；`Accounts.java` 只刪掉這個類別，其餘 A 內容沒動 |
| checked exception 鏈 | 23 個方法各補一個 `throws IOException`，方法本體不變。javac 每輪報錯的證據見前輪報告（git 歷史）。主控裁定 5 已接受 |
| 核准清單同步 | `exceptions-exempt.txt` 新增 3＋23 條；`extra-methods-approved.txt` 新增 1 條 |
| ModernCaptureLog | 依 #78 刪除（含測試 2 項），並重新產生 `docs/server-source-gap.md` |
| `b09-shape-diffs.md` | 依 #70 補上 `ClientSession#sourceApplicationRun()V` |

## 已驗證

### 建置（本機，只編譯）

環境：Microsoft OpenJDK 17.0.17、Apache Maven 3.9.9（放在 `scratch/issue-138/tools/`，SHA-512 與官方相符）。TEMP 指向 `scratch/issue-138/tmp`。

```powershell
scratch/issue-138/mvn.ps1 compile-r4.log "-DskipTests" "-Dmaven.test.skip=true" compile   # exit 0
scratch/issue-138/mvn.ps1 package-r3.log "-DskipTests" package                           # exit 0
#   as-record: scanned=4433 converted=54 ; as-record(test): scanned=2044 converted=1 ; BUILD SUCCESS
scratch/issue-138/mvn.ps1 testcompile-r6.log test-compile                                 # exit 0
```

`target/LiTo381-Server.jar` SHA-256：`20f4c6afb65c3547b23ff5b073bfc75af0b611bae1620cc4ec0a5b3eb7d80d93`（r3）。

### 語意比對（r3）

指令：`py -I scratch/issue-138/verify.py r3`。腳本副本在 `docs/reports/issue-138/verify.py`；argv 在 `semantic-commands.json`，摘要在 `semantic-summary.json`，B 範圍實際傳入的 deviations 清單在 `b-approved-deviations.txt`。

| 比較 | 右側 | 範圍 | SAME | SAME_EFFECTS | SAME_PATHS | SAME_CFG | LAMBDA_PAIRED | ORPHAN | SAME_LAYOUT | DEVIATION | DIFF | ONLY_LEFT | ONLY_RIGHT |
|---|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| B | candidate-50635f7e（T029 → base） | `--classes` 1,384 | 1349 | 3 | 15 | 9 | 2 | 1 | 0 | **4** | **1** | 0 | 0 |
| A | base-0.6.71 | `--classes` 692 | 665 | 4 | 16 | 0 | 0 | 0 | 1 | 6 | **0** | 0 | 0 |
| 整顆 JAR | base-0.6.71 | 全部 | 2382 | 4 | 17 | 0 | 0 | 0 | 1 | 6 | 144 | 1863 | 0 |

- **B 範圍**：`b_missing_in_left=[]`、`b_extra_vs_candidate=[]`。
  - DEVIATION 4 個：`NativeLightAction$Batch`、`NativeSourcePhysicalNpc`（既有），以及本輪的 `NativeSummonRetirement`、`ClassicWorldMonsterPopulation$TamedRelease`。
  - DIFF 1 個是 `ClientSession`。`py -I scratch/issue-138/shape_check.py r3` 的輸出為 `bad 184 in-shape-list 184 NOT-in-list []`。
- **remove_entries**：9 批共宣告 0 個，`remove_entries_present=[]`。
- **整顆 JAR**：`full_other=[]`，代表 A 與其他類別沒有引入差異。B 的 DIFF 由 145 降到 144，就是 TamedRelease 改回正式版的結果。

### 判斷「候選＝正式」的方法

- `py -I docs/reports/issue-138/basediff.py <類別> [方法片段]`：讀取 `full.json`（Maven JAR 對 `base-0.6.71.jar`），列出不是 SAME 的方法。SAME 就代表候選與正式相同。
- 搭配 `javap -p -c` 看正式 JAR，以及 `git show origin/main:<path>` 看舊 src/main。

### 測試逐案判斷（比照 T010；沒有停用或跳過任何測試，也沒有放寬 timeout）

#### 本輪（裁定 7）

| 測試（項數） | 失敗內容 | 判斷 | 證據 | 處理 |
|---|---|---|---|---|
| `ClientSessionClassicWorldDeathPenaltyIntegrationTest`（16） | 經驗值不同（例如 239718 對 244276）、缺 `S_EXP`、掉落件數 0/1 等 | **測試過時（候選＝正式）**，16 項同一根因 | 舊 src/main 的 `takeClassicWorldDirectDamage` 會在死亡時呼叫 `applyClassicWorldDeathExperiencePenalty`。候選的兩個世界死亡尾端（`sourcePlayerWorldCommit`、`sourcePlayerLegacyWorldDirectDamage`）都沒有這個呼叫；兩個 JAR 的 javap 在 `publishClassicWorldPlayerKill` 之後都是 `aload_0; lload_3; pop2; pop`。`tools/resource-database/DeathPenaltyPatch.java`（hook `lito-no-death-penalties`）也寫明 LiTo 死亡不進入懲罰流程。PK／粉名／戰爭／公告 7 個方法新舊版相同 | 經驗斷言改成來源值；順序斷言保留「死亡 → 戰爭結束 → 公告」，並斷言沒有 `S_EXP`；物品、技能、死亡保護改成反向斷言（沒有地面物品、物品與技能保留、保護道具沒被扣、沒有 messageCode 166/638、重新載入後 `deathLostSkills` 為空）；13 個測試改名 |
| `GameHubChannelTest` matchmaking（4） | 期望 0 或 3，實際是 1 或 2 | **測試過時（候選＝正式）** | 正式版 `matchRoom` 只有 8 個 filter，沒有過濾測試房；舊 src/main 第 1427 行的 `!NativeRoomRules.testRoom(...)` 是 src/main 專有的；id 1／2 是各頻道的 Local Lobby | 不指定模式時期望 Local Lobby（新 helper `localLobby`）；protected、滿房、租屋城鎮、等級、觀戰等過濾改用 `PET_RACE` 模式驗證 |
| `GameHubChannelTest#opcodeOnlyNativeStart…`（1） | 期望 `COUNTDOWN_STARTED`，實際 `NONE` | **測試過時（候選＝正式）** | 正式版與候選的 `tryJoinRoom` 都只在 SIEGE_WAR 自動分隊（正式 javap 偏移 409）；舊 src/main 對所有 teamBased 模式分隊。死鬥兩人都在紅隊 → 拒絕開局 | 先斷言同隊時 `NONE`；`setForce(participant,1)` 之後房主開局 → `COUNTDOWN_STARTED` |
| `ClientSessionRentalTownDirectoryTest`（2）、`NativeLitoRentalTeleportEndpointCatalogTest`（1） | Read timed out | **測試過時（候選＝正式）**。不是 timeout 或執行緒問題，是封包根本沒送出；CI 也沒有伺服器端例外 | 正式版 `quitFight`／`exitRoomBeforePolyMe`（T029）第 0–14 條指令：`phase==BATTLE → settleRequestedBattleExit; return`；正式版 `startRentalTownScene` 把場景設為 BATTLE；basediff：`quitFight` SAME_PATHS、`settleRequestedBattleExit` SAME | 先驗證 BATTLE 下不送封包、狀態不變；再把 phase 設成 ROOM，原本的交接斷言全部保留。玩家可見的退步見結論第 4 點 |
| `ClientSessionWorldCkewEntryTest#fourNativeClassesStartQuestAndEnterOneSharedShowId`（1） | 首次對話後收到的封包不是空的 | **候選≠正式，玩家看得到 → needs:claude** | 候選 `ClientSession.java:92922` 新增 `NativeStaticNpcMotion.talk`：L1Quest NPC（80133–80136）轉向說話者並送 `S_CHANGE_DIRECTION`。`NativeStaticNpcMotion` 是正式版沒有的類別（ONLY_LEFT） | **未改**。若核准，斷言改成「只收到 changeDirection(入口 NPC, 朝向說話者)，沒有 S_HYPERTEXT」 |
| `ClientSessionWorldRescueFollowerTest#twoEncryptedSessionsReceiveClaimAndNativeOwnerStrangerPages`（1） | 觀察者沒收到 searcher 71092 的 `S_REMOVE_OBJECT` | **候選≠正式，玩家看得到 → needs:claude** | 候選 `sendEncrypted` 會經過 `NativeStaticNpcMotion.nativeSourceWeaponFrame`，可見 L1Quest 靜態 NPC 的 `S_REMOVE_OBJECT` 會被吞掉；正式版與舊 src/main 都直接送給每個 viewer | **未改** |
| `GameHubCharacterTest`、`GameHubChannelTest#matchmakingQueue…`、`GameHubAccountOwnershipTest`、`ClientSessionNativeRentalTownCitizenshipTest`（4） | `invalid source-qualified character identity` | 缺陷 1 | — | 已修（裁定 1），第 4 輪通過 |
| `ClassicWorldMonsterCatalogTest#taming…`（1） | `invalid classic monster release` | 缺陷 2 | — | 已修（裁定 2），第 4 輪通過 |

#### 原始碼文字稽核（裁定 4：改用行為測試）

| 原測試 | 原本保護什麼 | 處理 |
|---|---|---|
| `NativeHonorRespawnSourceAuditTest#delayedPathPreservesDeathCoordinatesAndOmitsSpawnMovement` | Honor Match 死亡後等 5 秒、在死亡座標送 S_RESURRECT、不送 S_MOVE_OBJECT、只限 Honor Match | 改寫為 `NativeHonorRespawnBehaviorTest#honorDeathWaitsFiveSecondsAndResurrectsAtDeathCoordinatesWithoutSpawnMovement` 與 `#deferredRespawnIsRefusedOutsideHonorMatch`（DEATHMATCH 回 false；HONOR 回 true 當對照）。常數測試原樣搬入；舊檔刪除 |
| `NativeTournamentMatchEndEconomySourceAuditTest#resultPublishesPrizeBeforeRemovingBoughtMatchItems` | RESULT 先記勝負，之後才做經濟重置 | 改寫為 `NativeTournamentMatchEndEconomyBehaviorTest#resultRecordsOutcomeThenResetsPerMatchEconomyKeepingSourceAndRewardItems`：驗證 RESULT、勝敗寫入、shopPoints=0、賽中購買品移除，來源武器與 Tournament 獎勵保留 |
| 同檔 `roomExitBattleQuit…`（仍通過） | 三個重置呼叫點存在 | 保留並加註（這項不綁舊實作文字，候選下仍成立） |
| `NativeWorldGroupHealingSourceAuditTest`（2） | WORLD 會派送 Heal All；只治療同 instance、畫面內的血盟成員；寫入冷卻 | 改寫為 `NativeWorldGroupHealingBehaviorTest#worldHealAllHealsOnlySameInstanceClanMembersAndCommitsReuseDelay`：經由真正的 `useSpell` 派送，驗證血盟成員有被治療、非血盟成員與其他 instance 不治療、MP／HP 成本與 reuse 已寫入、立刻重施無效。舊檔刪除 |
| `PcActionRewardSqlFirstSourceAuditTest#helperCapabilityPrecedesWorldCompanionRoutingAndClaimIsSqlFirst` | 小幫手走 pc_action；SQL claim 成功後才發布 | 改寫為 `PcActionRewardBehaviorTest` 2 項：SUCCESS 之前背包看不到、重送無效；CONFLICT 時背包與 pending 都不變 |
| 同檔 `rewardRowAndDestination…`（仍通過） | SQL 鎖群組 | 刪除，`LitoMySqlCastleIntegrationTest` 已涵蓋 |
| 同檔 `newLitoCharacters…`、`isolatedLitoRuntime…`（仍通過） | 小幫手模板；登入器資源 | 保留（後者檢查的是資源檔，不是實作文字） |
| `WaterDragonSqlFirstPersistenceSourceAuditTest#rentalPublishesOnlyAfterRouteQualifiedSqlCommit` | SQL 提交後才發布憑證；失敗時取消房間預約 | 改寫為 `WaterDragonSqlFirstPersistenceBehaviorTest` 2 項：SQL 呼叫進行中看不到憑證；CONFLICT 時預約歸 0、背包不變。repository 端由 `LitoMySqlWaterDragonPassIntegrationTest` 涵蓋。舊檔刪除 |

第 5 輪 CI 中，有 4 項新行為測試因為 fixture 的前置條件失敗，不是行為改變，第 6 輪已修正：
- **開局順序**：Honor 2 項、Tournament 1 項原本由參加者 ready 觸發倒數。實際上參加者 ready 只會得到 `NONE`，要由房主 `requestOwnerStart` 開始，與已通過的 `GameHubChannelTest` 順序相同。另外 `joinRoom` 改用 `tryJoinRoom` 並斷言加入成功。
- **accounts**（第 6 輪才發現）：Tournament fixture 建立 `ClientSession` 時 `accounts` 傳了 null，`DurableBattleSettlement.route` 反射呼叫 `selectedSourceAccounts` 時發生 NPE。正式環境的 session 一定帶有 AccountRepository，所以改成傳入 `AccountRepository.open(config)`。selftest 是 `database.mode=memory`，結算路由為 null，會略過 SQL。
- **座標**：Heal All 原本寫死世界樹旁的座標，其中有不可通行的格子（`classic-world location is not passable`）。改成依 `ClassicMap.standingPassable` 從世界樹 3 格內動態挑 4 個可通行格。

#### 前輪（第 2、3 輪）已處理

| 測試 | 判斷 | 處理 |
|---|---|---|
| `TournamentPresenceTest`、`ModernNavigationSessionTest`（各 1，裁定 6） | 測試過時：native 與 modern 共用 TournamentPresence 是 src/main 專有的測試接縫，Main 從沒接上，候選也沒有 | 改寫成 `nativeGameHubCapacityIsIndependentOfAModernPresenceCoreAfterBLand`、`nativeHubAndModernAdapterKeepSeparateCapacityAfterBLand` |
| `ClientSessionLitoRoomInvitationTest` | 測試過時（`ModernCaptureLog` → `CaptureLog`） | 改回 A-land 之前的原文 |
| `ModernCaptureLogTest`（2） | 受測類別依 #78 刪除 | 隨類別一起刪除 |
| `NativeMagicDollTest`（2） | 測試過時（T029 採正式封包） | 期望值改成正式封包 |
| `ProtocolBattleTest`（3） | 測試過時（正式版與候選都沒有那兩個嚴格檢查；文字形狀改變） | 改成驗證會被接受；改計數穩定的引數對 |
| `BattleSpellCatalogTest`（1） | 測試過時（候選資料改為 0） | 期望改成 0 |
| `ClientSessionClassicWorldMonsterAggressionIntegrationTest`（2，死鎖逾時） | 測試過時（執行緒模型） | tick 改成提交到 battle clock 執行 |
| `NativeHotelSqlFirstPersistenceSourceAuditTest`、`NativeCastleSecuritySettlementSourceAuditTest`（各 1） | 測試過時（拆檔） | 改成讀 `Accounts.java`＋`LegacyMySqlRepository.java` |

### 取鎖順序（裁定 3）

battle clock 是單執行緒（`GameHub.java:1467`）。

| 執行緒 | 路徑 | 取鎖順序 | 呼叫鏈 |
|---|---|---|---|
| battle clock | monster tick | population → hub | `classicWorldMonsterTick` → `population.moveToward`（synchronized）→ pathfinder 的 blocked lambda → `NativeStaticNpcMotion.occupies` → `NativeStaticCancellationReplacement.all/live` → `synchronized(hub)`。`moveAway`、`idleMove`、`itemAction`、`returnGuardHome`、`nearTeleport`、`respawn`、`spawnQuestMonsterAt` 同類 |
| battle clock | companion tick | hub → population | `NativeStaticNpcMotion.tick`：`synchronized(hub)` → `synchronized(population)` |
| client | C_ATTACK | hub → population | `ClientSession.java:1764` → `attack` → `attackClassicWorld` → `CombatWorldCommit.prepare` |
| client | 魔法攻擊 | hub → population | `CombatWorldMagic.prepare`／`enqueue` |
| client | 馴服捕捉 | hub → population | `NativeTamedCaptureLifecycle` |
| client | 斷線 | hub → population | `run()` finally → `nativeSummonOriginalDisconnect`（synchronized）→ `clearClassicWorldTargetsForOwner` → `population.clearTargetsForPlayer` |

- **死鎖情境**：client 執行緒持有 hub、等 population；同時 battle clock 在 `moveToward` 裡持有 population、等 hub。
- **新舊對照**：client 端的 hub → population 在 0.6.71 就有（`CombatWorldCommit`、`CombatWorldMagic`、`ClassicWorldNpcPathfinder` 對正式都是 SAME）。反向的 population → hub 是候選新增的（`NativeStaticNpcMotion`、`NativeStaticCancellationReplacement`、`lambda$moveToward$32` 在正式版不存在）。
- **另外**：client 執行緒以 population → hub 取鎖的路徑沒有找到。
- 鎖鏈與第 2 輪 CI 的 thread dump 吻合。

## 推測

- 死鎖在正式環境實際發生的機率沒有量測；`groundClock`（`GameHub.java:1475`）的取鎖順序也還沒分析；`serverClassicWorldDspyFollowerTick` 應該跑在 battle clock 上，但沒追到排程點。
- 租屋：phase 設成 ROOM 之後，候選會先做經濟重置、私人商店與交易取消。讀程式判斷這些都不會送封包，也不會動到租屋欄位。
- Heal All 行為測試沒有涵蓋寵物（companion）來源，也沒有涵蓋取消冥想／屏障／隱身的順序；「不同 instance」是直接改目標的 `classicWorldInstanceId=7` 模擬的。

## 待驗證

- `mvn -B test` 全綠：**未達成**。Ckew、Rescue 2 項等主控裁定；第 6 輪的結果見下表。
- 實機回歸：登入、大廳、技能、進房、戰鬥結算、退出，以及 `b09-shape-diffs.md` 的形狀差異與取鎖順序，交 M2 離線回歸與 T020-close，由使用者或 Codex 在本機執行。
- 沒有啟動伺服器、登入器、遊戲，也沒有連任何 DB；本機只編譯。

## CI 結果

| run | commit | Server tests | 備註 |
|---|---|---|---|
| 42876069149 | `0e957635` | 取消（卡住超過 24 分鐘） | 死鎖 |
| 42882305652 | `28247dd8` | 失敗：6048 run，38 F，11 E | 含暫時診斷 |
| 42885667308 | `14e958bb` | 失敗：6048 run，28 F，9 E | 不再卡住 |
| 42890240229 | `ab4cf25e` | 失敗：32 項 | 修好缺陷 1、2，5 項通過；新增的 3 項單元測試通過 |
| 42893352221 | `3ddd435b` | 失敗：6052 run，**5 F，1 E** | 死亡懲罰、配對、租屋相關 26 項全部通過；剩 Ckew、Rescue（needs:claude），以及 4 項行為測試 fixture |
| 42896918558 | `751c7a75` | 失敗：6052 run，**2 F，1 E** | Honor 2 項、Heal All 1 項通過；剩 Ckew、Rescue（needs:claude），以及 Tournament 行為測試 1 項 E（fixture 的 accounts 是 null） |
| （第 7 輪） | 本 commit | 見 PR #315 | Tournament fixture 改為傳入 `AccountRepository.open(config)`；預期只剩 Ckew、Rescue 2 項 |

其他 job（`Server (Java 17 / Maven)`、`Launcher`、`Build-Package.ps1 zip`、`Semantic gap`）每一輪都 pass。

## 產出路徑

- 程式：`lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`（303 個 B 類別；本輪改了 `NativeSummonRetirement.java`、`ClassicWorldMonsterPopulation.java`）
- 測試：
  - 本輪新增：`NativeSummonRetirementUnboundDisconnectTest`、`NativeHonorRespawnBehaviorTest`、`NativeTournamentMatchEndEconomyBehaviorTest`、`NativeWorldGroupHealingBehaviorTest`、`PcActionRewardBehaviorTest`、`WaterDragonSqlFirstPersistenceBehaviorTest`
  - 本輪修改：`ClassicWorldMonsterCatalogTest`、`ClientSessionClassicWorldDeathPenaltyIntegrationTest`、`GameHubChannelTest`、`ClientSessionRentalTownDirectoryTest`、`NativeLitoRentalTeleportEndpointCatalogTest`、`NativeTournamentMatchEndEconomySourceAuditTest`、`PcActionRewardSqlFirstSourceAuditTest`
  - 本輪刪除：`NativeHonorRespawnSourceAuditTest`、`NativeWorldGroupHealingSourceAuditTest`、`WaterDragonSqlFirstPersistenceSourceAuditTest`
- 清單：`docs/combat-merge/deviations.txt`（新增 2 條）、`docs/combat-merge/b09-shape-diffs.md`（新增取鎖順序一節）
- 證據：`docs/reports/issue-138/`（verify.py、shape_check.py、basediff.py、sf_failures.py、semantic-summary.json、semantic-commands.json、b-approved-deviations.txt 等）
- 分支 `task/138`，PR #315

## 給下一棒的提示

1. **主控**：裁定結論中的 needs:claude 第 1–3 點，第 4–5 點是提醒。
   - 若 Ckew／Rescue 判定「接受候選行為」：照上表「若核准」的說明改測試。
   - 若判定「改回正式行為」：要在 `NativeStaticNpcMotion` 加偏離，並登記 method-diff。
   - 死鎖的修法建議：讓 `NativeStaticNpcMotion.occupies` 在 population 鎖內不要取 hub 鎖，例如事先在 hub 鎖內做好快照。這會讓 B 偏離候選，需要核准。
2. **下一輪 code**：第 6 輪若還有失敗，用 `py -I docs/reports/issue-138/sf_failures.py <surefire-reports>` 看明細。artifact `server-test-evidence` 裡有 surefire XML 與 `mvn-test.log`。
3. **T020-close**：B 範圍對候選的比對用 `verify.py` 的 `b-approved` 模式；整顆 JAR 對正式版比對時，只能套用非 B owner 的核准條目。
4. B 批次是各自對候選 JAR 編譯的，跨批次的 checked exception 不一致只有整體跑一次 `javac` 才看得出來。

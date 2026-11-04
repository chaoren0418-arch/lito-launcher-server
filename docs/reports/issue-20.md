# Issue #20 / T010 — 修復 T009 清單上的真實測試失敗

## 做了什麼

T009（#12）把本機資源守門做好後，無 DB／無本機資源的 `mvn test` 仍有 30 案真實斷言失敗。本輪逐案判斷「測試過時」或「程式缺陷」，未刪測試、未用 assume 跳過。

### 程式缺陷（改 `src/main`）

| 位置 | 缺陷 | 依據 |
|---|---|---|
| `GameHub.matchRoom` | S_EB 快速配對會選到每頻道預設測試房 Local Lobby | 測試房應留在房間列表供直接加入／ArenaBot，但不該壓過玩家房。過濾 `NativeRoomRules.testRoom` 後，案 14–17 的配對結果與受保護房／租用城排除語意一致。 |
| `GameHub.tryJoinRoom` | 只在 `SIEGE_WAR` 分配 force | 所有 `teamBased` 模式都要 `smallerSiegeForce`，否則紅藍人數為 0、倒數被拒。案 18、21 因此無法 `COUNTDOWN_STARTED`。 |
| `NativeRoomRules.refusal` | 最低人數與紅藍只算真人 | GM `.de` 假人已在 `combatantCount()` 計入，開始條件卻忽略。案 19–21 有滿額假人仍回 `NONE`。 |
| `Protocol.attackRequest` | 目標 object id=0 不拒絕 | 三個原生攻擊產者共用 envelope；`requireDwordId` 後案 25 才會丟 `IllegalArgumentException`。 |
| `ClientSession.quitFight` | `Phase.BATTLE` 先走 `settleRequestedBattleExit` | 租用城傳送石在 BATTLE 重用 C_D9。必須先處理 `rentalTownTransferPending`。案 30 在 settle 路徑上 timeout。 |
| `ClientSession.exitRoomBeforePolyMe` | 同上，BATTLE 提前 return | 租用城場景也是 `Phase.BATTLE` 且不在 GameHub。原 rental 分支成為死碼，不發 `S_REMOVE_INVENTORY`／peer `S_REMOVE_OBJECT`。案 11–12 的退出／離場因此失敗。 |
| `MemoryAccountRepository` | `accessLevel` 恆 0 | 記憶體 fixture 的 `gmCommand("de")` 要求 200。新增 `grantAccessLevel`。案 6、8 才能走 GM 路徑。 |

### 測試過時（對應斷言改成現行契約，測試本身保留）

圖資／協定／房間契約已變，測試仍釘舊值：

- Create Zombie 法師帶是 class **3**（不是 1）；class 1 回 45065。
- Foe Slayer 目標特效 **6509**；冰錐 gfx **1809**（hex `1107`）；治療特效 **744**。
- 防具 `nativeStatusDetail` 不再發 0x27（LiTo 只收 tag 1..33）。
- PC `S_PUT_OBJECT` 尾 5 bytes；房間 type 合法範圍含 5／6，拒絕改釘 type 7。
- 單人斷線勝者是空集合（ArenaBot 不是贏家）。
- 房間列表含 Local Lobby；使用者自建房沒有 ArenaBot `S_LITO_EXIT_ROOM`。
- `sendEncrypted(S_LITO_START_GAME)` 會立刻跟 `S_LITO_TIMELIMIT`；租用城 skip 13→14。
- `S_MOVE_OBJECT` 由 `NativeMovement.destination` 寫**起點**（客戶端再走一步）。
- C_SAY 會查 `accounts`（GM／管理員）；租用城聊天測試改注入 `MemoryAccountRepository`。
- 地獄剩餘時間鍵是 `GameStateStore.sourceCharacter(source, name)`，不是裸角色名。
- BATTLE 中 C_D9 走 RESULT，不是立刻回 CHANNEL；經濟 audit 不再要求 `"battle-quit"` 字串。
- ArenaBot 只存在預設測試房且假人為空；租用戰鬥交接改加入 Local Lobby，並先排空 0x3810_0711 殘幀。

### CI

`.github/workflows/build.yml` 的 `server-test`：名稱改 **Server tests**，拿掉 `continue-on-error`。Maven 非零退出會讓 job 失敗。

## 逐案判斷

編號對齊 `docs/reports/issue-12.md`。

| # | 測試 | 判斷 | 依據與處理 |
|---|---|---|---|
| 1 | `BattleCompanionTest.createdZombieUsesLevelSelectedTemplateAtFullVitals` | TEST_OUTDATED | 法師殭屍帶是 class 3；`zombieTemplate(3, 47)` → 81186。 |
| 2 | `BattleDisconnectTest.solePlayerDisconnectLosesToTheSyntheticArenaBot` | TEST_OUTDATED | `resolveBattleDisconnect` 勝者為 `[]`、reason=`combatant-disconnected`；ArenaBot 不是贏家。 |
| 3 | `BattleFoeSlayerCatalogTest.pinsSelectedPrimaryRowAndRetainedThreeStrikeContract` | TEST_OUTDATED | 現行目標特效 6509，不是 12119。 |
| 4 | `BattleSpellCatalogTest.createZombiePinsCorpseEnvelopeCostsAndNativeLevelTemplates` | TEST_OUTDATED | 同案 1：class 1 是騎士帶（45065）；法師帶改 class 3。 |
| 5 | `ClassicWorldMonsterMagicProtocolTest.iceConeUsesExactSelectedClientExtendedAttackLayout` | TEST_OUTDATED | 冰錐 gfx 11738→1809，hex `DA2D`→`1107`。 |
| 6 | `ClientSessionClassicWorldDeathPenaltyIntegrationTest.lowLevelVictimAndGmAttackerSuppressFeedAndCounters` | CODE_DEFECT | 記憶體帳號 `accessLevel` 恆 0，GM 抑制被當成一般 PK。`grantAccessLevel("test", 200)` + `SourceAdministration.clear`。 |
| 7 | `ClientSessionClassicWorldHellIntegrationTest.firstEntryEmits552AndPortalThenExpiryReturnsThroughPortalHandshake` | TEST_OUTDATED | `elapseHell`／`reconcileHell` 要用 `sourceCharacter(source, name)`，並設定 `selectedClassicServerName`。 |
| 8 | `ClientSessionGmDeIntegrationTest.databaseAuthorizedDeChatCreatesReadyRoomRowsAndStartsCountdown` | CODE_DEFECT + TEST_OUTDATED | 授權：同案 6 授 200。封包：使用者房不發 ArenaBot `S_LITO_EXIT_ROOM`，改收 3×`S_LITO_ROOM_USER` + `S_MESSAGE`。 |
| 9 | `ClientSessionLitoCreateRoomClanIntegrationTest.compactCreateAutomaticallyJoinsAClannedCreatorAndEmitsSuccess` | TEST_OUTDATED | BATTLE 中 C_D9 走 `settleRequestedBattleExit` → RESULT，房間仍在；不是立刻 CHANNEL。 |
| 10 | `ClientSessionRentalBattleHandoffIntegrationTest.newGameEndpointConsumesRentalLoadoutIntoActorAttackAndResultRestore` | TEST_OUTDATED | 後續攻擊 object id 2／`botHp`，必須用 Local Lobby ArenaBot。使用者房沒有 bot；假人會讓 `combatantCount>=2`、`botPosition==null`。交接殘幀先用舊 cipher drain。 |
| 11 | `ClientSessionRentalTownDirectoryTest.encryptedGroupOneChainBootstrapsResourceDerivedNativeTownScene` | CODE_DEFECT + TEST_OUTDATED | START 後必跟 TIMELIMIT（測過時）。C_SAY 需 accounts（測過時）。`exitRoom` 必須在 BATTLE settle 前走租用城清理（程式缺陷）。 |
| 12 | `ClientSessionRentalTownDirectoryTest.rentalTownPlayersShareRosterActorsMovementDirectionActionAndDeparture` | CODE_DEFECT + TEST_OUTDATED | skip 13→14 以吞 TIMELIMIT；`S_MOVE_OBJECT` 改 `NativeMovement.destination`。離場 peer 封包依案 11 的 `exitRoom` 修復。 |
| 13 | `GameHubChannelTest.assignsAndListsRoomsInsideTheirOwningChannel` | TEST_OUTDATED | 每頻道本來就有 Local Lobby，列表應為 `[Local Lobby, Baphomet Arena]`。 |
| 14 | `GameHubChannelTest.matchmakingAppliesLevelAndObserverEligibilityBeforeSuccess` | CODE_DEFECT | Local Lobby 污染 `matchRoom`。 |
| 15 | `GameHubChannelTest.matchmakingNeverSelectsTheServerGatedRentalTownMode` | CODE_DEFECT | 同上。 |
| 16 | `GameHubChannelTest.matchmakingRejectsWhenOnlyProtectedOrFullRoomsExist` | CODE_DEFECT | 同上。 |
| 17 | `GameHubChannelTest.matchmakingSkipsProtectedRoomsAndSelectsAnEligiblePublicRoom` | CODE_DEFECT | 同上。 |
| 18 | `GameHubChannelTest.opcodeOnlyNativeStartIsAuthorizedOnlyForTheCurrentRoomOwner` | CODE_DEFECT | team-based 未分配 force，倒數 `NONE`。 |
| 19 | `GameHubVirtualPlayerTest.deRosterHonorsCapacityServerNamesBalanceAndAutomaticReadiness` | CODE_DEFECT | `refusal` 不計假人。 |
| 20 | `GameHubVirtualPlayerTest.everyNativeBattleModeAndMapStartsWithDatabaseCommandVirtualRoster` | CODE_DEFECT | 同上。 |
| 21 | `GameHubVirtualPlayerTest.siegeVirtualRosterSatisfiesNativeFifteenPerForceMinimum` | CODE_DEFECT | 假人不計入紅藍 + 攻城未分配 force。 |
| 22 | `GameStateSourceInventoryTest.mirrorsSourceArmorNativeStatusAndRetainsLiToEquippedState` | TEST_OUTDATED | 不再發 0x27。 |
| 23 | `GameStateSourceInventoryTest.retainedArmorStatusSeparatesBodyArmorFromAccessories` | TEST_OUTDATED | 同上。 |
| 24 | `NativeTournamentMatchEndEconomySourceAuditTest.roomExitBattleQuitAndDisconnectRemainIdempotentSafetyNets` | TEST_OUTDATED | BATTLE 退出走 RESULT，來源不再呼叫 `resetNativeTournamentEconomy("battle-quit")`。 |
| 25 | `ProtocolBattleTest.allThreeNativeAttackProducersUseTheSameTargetCoordinateEnvelope` | CODE_DEFECT | 目標 0 未 `requireDwordId`。 |
| 26 | `ProtocolBattleTest.battleActorUsesPcStatusAndTailFieldsFromTheExactPutObjectReceiver` | TEST_OUTDATED | PC tail 5 bytes（0xFF + 4×0）。 |
| 27 | `ProtocolBattleTest.objectHealUsesNativeTargetAndEffectLayouts` | TEST_OUTDATED | 治療特效 11722→744。 |
| 28 | `ProtocolLitoRoomParticipantTest.nativeAffiliationFieldRejectsOverlongOrInvalidRows` | TEST_OUTDATED | type 0..6 合法；拒絕案例改 type 7。 |
| 29 | `ClientSessionLitoModeStepIntegrationTest.encryptedRightNextAndLeftPreviousEmitExactS_D5ModeMapPairs` | TEST_OUTDATED | 使用者自建房切模式不發 ArenaBot EXIT；等待該封包會 timeout。 |
| 30 | `NativeLitoRentalTeleportEndpointCatalogTest.exactTeleporterActionSendsStatusThenDirectEndpoint` | CODE_DEFECT + TEST_OUTDATED | `quitFight` 先處理租用傳送（程式）。偽造 action 前 drain 殘幀（測試）。 |

## 怎麼驗證

- **已驗證**：無 DB、無本機資源。Ubuntu / OpenJDK 17 / Maven 3.8.7。
- **已驗證**：issue-12 的 30 案定向 `mvn -B test`：**30 run / 0 failures / 0 errors / 0 skipped**。
- **已驗證**：完整 `mvn -B test`（無 DB／無本機資源）：**6033 run / 0 failures / 0 errors / 117 skipped**，1266 份 Surefire XML。與 T009 Ubuntu 基線同規模，失敗從 28+2 降為 0。
- **已驗證**：`server-test` 已無 `continue-on-error`，job 名稱為 `Server tests`。
- **待驗證**：GitHub CI `server-test` job（本機已全綠，CI 為同一 `mvn -B test`）。
- 未啟動遊戲或正式服務、未連正式 DB、未讀寫 `config/server.properties`、未改正式 BIN／封存／release。

## 產出路徑

- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/{GameHub,NativeRoomRules,Protocol,ClientSession,Accounts}.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/**`（上表 30 案對應測試）
- `.github/workflows/build.yml`
- `docs/reports/issue-20.md`

## 給下一棒的提示

Claude 審查本 PR 時請逐案核對上表判斷，重點看：

1. `matchRoom` 排除測試房是否仍允許玩家直接加入 Local Lobby（房間列表仍含它）。
2. `refusal` 把 GM 假人計入後，單人測試房 ArenaBot 路徑是否仍只在 `activeBot` 時繞過最低 2 人。
3. `exitRoom`／`quitFight` 的租用城分支不可再被 `Phase.BATTLE` settle 蓋過。
4. CI `server-test` 現在會擋合併；完整套件若出現與這 30 案無關的新失敗，屬回歸，不要再加 `continue-on-error`。
5. 實機 `--check`／`--smoke`、租用城雙人離場、GM `.de` 倒數，仍須使用者跑。不得恢復假 skip。

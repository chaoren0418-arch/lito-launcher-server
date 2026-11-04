# Issue #12 / T009 — 本機資源測試 assume 守門（第 3 次：實際路徑 + 正式驗收調整）

## 做了什麼

Issue 驗收已由 Claude 主控正式調整（14:58）：本任務只負責本機資源守門；守門必須檢查測試**實際讀取的路徑**；不可用旗標跳過無外部依賴測試；不可改 `src/main`。守門後仍失敗的真實斷言（28 fail + 2 error）不在範圍，須逐行列出，且 `server-test` 設 `continue-on-error: true`（名稱註明 non-blocking until T010）。

本輪對應審查 P2，並套用上述驗收：

1. **實際路徑守門**：`LocalResources.assumeIdaClient(Path)`／`assumeLitoBin()`／`assumeExisting` 只檢查呼叫端會開的那條 Path。相對 ida（`../LiTo381-開發/...`）與絕對 ida（`D:/lineage開發/...`）不再互為候選；`LiTo.bin` 只認 `D:/380/.../LiTo.bin`；unpacked 分絕對／相對兩條常數。分析 JSON／目錄／`Sprite.idx` 等改 assume 實際檔。
2. **`LITO_SHARED_RESOURCE_ROOT`**：只認環境變數。`ModernPlayerHealingTlsTest`／`ModernVipWorldTlsTest` 的 `sharedMap` 改走 `LocalResources.sharedResourceRoot()`，`-Dlito.shared.resource.root` 不能放行後再 `Path.of(null)`。
3. **ExternalControl V2 fixture**：production `NativePlayerLoadoutAdapter` 仍是 3 參數、沒有 observe callback。改在測試側 `PhysicalProvider.read`／`readForWorld`（`capture()` 實際呼叫後者）於 SQL 後呼叫 `observeEntry` → `pause()`，恢復 hold／witness。未改 `src/main`。這條 fixture 需要 MySQL，無 DB CI 不會跑。
4. **CI**：`.github/workflows/build.yml` 的 `server-test` 設 `continue-on-error: true`，名稱 `Server tests (no DB, no local resources; non-blocking until T010)`。保留 T007 package＋`jar tf`。
5. **`LocalResourcesTest`**：證明「僅另一候選存在」不能通過 exact-path assume；系統 property 不能代替環境變數。

未恢復假 skip，未改 `src/main`。

### 守門涵蓋（測試類數；一類可同時屬多個）

| 守門 | 方法 | 涵蓋測試類 | 說明 |
|---|---|---|---|
| `server.properties` | `assumeServerProperties()` | **80** | 正式 `config/server.properties` |
| 天堂來源樹 | `assumeLineageSources()` | **254** | `lito伺服器/1-伺服器/db/380_db.sql` |
| `LiTo.bin` | `assumeLitoBin()` | **92** | 硬編碼 `D:/380/.../LiTo.bin` |
| ida-client 反組譯 | `assumeIdaClient(Path)` | **158** | 該類實際讀的相對或絕對 `full-disassembly.asm` |
| **以上去重** | `@BeforeAll` 等 | **325** | 四類本機檔 |
| 顯式 `@BeforeAll assumeLocalResourcesPresent` | — | **328** | 另含 unpacked／分析 JSON／目錄 |
| Windows `java.exe` | `assumeWindowsJvm()` | **3** | `java.home/bin/java.exe` |
| 共享資源根 | `sharedResourceRoot()` | **2** | 只讀 `LITO_SHARED_RESOURCE_ROOT` |

`PcActionRewardSqlFirstSourceAuditTest` 的 overlay 方法改方法級 assume（`../LiTo381-Launcher/...`、pc-action html），其餘讀 `src/main` 的方法在 CI 會跑。

## 怎麼驗證

```
cd lito伺服器/LiTo381-Server && mvn -B test
```

**已驗證（本機無 DB、無本機資源）**：`Tests run: 6014, Failures: 28, Errors: 2, Skipped: 116`。失敗集合與下表 30 案相同；其餘全綠或 skipped。`LocalResourcesTest` 2/2 通過。缺檔類仍 `@BeforeAll` abort（`Tests run: 0`）。

未改 `config/server.properties`、releases、封存、`LiTo.bin`；未啟動遊戲、未連正式 DB。

## 原有失敗（不在本任務範圍；Issue 驗收已正式排除）

一行一案（測試類／方法／失敗訊息）：

1. `BattleCompanionTest.createdZombieUsesLevelSelectedTemplateAtFullVitals` — expected: `<81186>` but was: `<45065>`
2. `BattleDisconnectTest.solePlayerDisconnectLosesToTheSyntheticArenaBot` — expected: `<[2]>` but was: `<[]>`
3. `BattleFoeSlayerCatalogTest.pinsSelectedPrimaryRowAndRetainedThreeStrikeContract` — expected: `<12119>` but was: `<6509>`
4. `BattleSpellCatalogTest.createZombiePinsCorpseEnvelopeCostsAndNativeLevelTemplates` — expected: `<81183>` but was: `<45065>`
5. `ClassicWorldMonsterMagicProtocolTest.iceConeUsesExactSelectedClientExtendedAttackLayout` — hex 差 2 bytes（`110700` vs `0A0621` 段）
6. `ClientSessionClassicWorldDeathPenaltyIntegrationTest.lowLevelVictimAndGmAttackerSuppressFeedAndCounters` — expected: `<0>` but was: `<1>`
7. `ClientSessionClassicWorldHellIntegrationTest.firstEntryEmits552AndPortalThenExpiryReturnsThroughPortalHandshake` — completion packets=`[]` expected: `<true>` but was: `<false>`
8. `ClientSessionGmDeIntegrationTest.databaseAuthorizedDeChatCreatesReadyRoomRowsAndStartsCountdown` — expected: `<4>` but was: `<1>`
9. `ClientSessionLitoCreateRoomClanIntegrationTest.compactCreateAutomaticallyJoinsAClannedCreatorAndEmitsSuccess` — expected: `<CHANNEL>` but was: `<RESULT>`
10. `ClientSessionRentalBattleHandoffIntegrationTest.newGameEndpointConsumesRentalLoadoutIntoActorAttackAndResultRestore` — startSoloBattle expected: `<true>` but was: `<false>`
11. `ClientSessionRentalTownDirectoryTest.encryptedGroupOneChainBootstrapsResourceDerivedNativeTownScene` — array lengths expected: `<9>` but was: `<5>`
12. `ClientSessionRentalTownDirectoryTest.rentalTownPlayersShareRosterActorsMovementDirectionActionAndDeparture` — NPC expected: `<32772>` but was: `<32770>`
13. `GameHubChannelTest.assignsAndListsRoomsInsideTheirOwningChannel` — expected: `<[Baphomet Arena]>` but was: `<[Local Lobby, Baphomet Arena]>`
14. `GameHubChannelTest.matchmakingAppliesLevelAndObserverEligibilityBeforeSuccess` — expected: `<0>` but was: `<2>`
15. `GameHubChannelTest.matchmakingNeverSelectsTheServerGatedRentalTownMode` — expected: `<0>` but was: `<1>`
16. `GameHubChannelTest.matchmakingRejectsWhenOnlyProtectedOrFullRoomsExist` — expected: `<0>` but was: `<1>`
17. `GameHubChannelTest.matchmakingSkipsProtectedRoomsAndSelectsAnEligiblePublicRoom` — expected: `<3>` but was: `<1>`
18. `GameHubChannelTest.opcodeOnlyNativeStartIsAuthorizedOnlyForTheCurrentRoomOwner` — expected: `<COUNTDOWN_STARTED>` but was: `<NONE>`
19. `GameHubVirtualPlayerTest.deRosterHonorsCapacityServerNamesBalanceAndAutomaticReadiness` — expected: `<COUNTDOWN_STARTED>` but was: `<NONE>`
20. `GameHubVirtualPlayerTest.everyNativeBattleModeAndMapStartsWithDatabaseCommandVirtualRoster` — PET_RACE/565 expected: `<COUNTDOWN_STARTED>` but was: `<NONE>`
21. `GameHubVirtualPlayerTest.siegeVirtualRosterSatisfiesNativeFifteenPerForceMinimum` — expected: `<COUNTDOWN_STARTED>` but was: `<NONE>`
22. `GameStateSourceInventoryTest.mirrorsSourceArmorNativeStatusAndRetainsLiToEquippedState` — status hex 少 `2701`
23. `GameStateSourceInventoryTest.retainedArmorStatusSeparatesBodyArmorFromAccessories` — status hex 少 `27`
24. `NativeTournamentMatchEndEconomySourceAuditTest.roomExitBattleQuitAndDisconnectRemainIdempotentSafetyNets` — battle-quit expected: `<true>` but was: `<false>`
25. `ProtocolBattleTest.allThreeNativeAttackProducersUseTheSameTargetCoordinateEnvelope` — Expected `IllegalArgumentException`, nothing thrown
26. `ProtocolBattleTest.battleActorUsesPcStatusAndTailFieldsFromTheExactPutObjectReceiver` — expected: `<0>` but was: `<5>`
27. `ProtocolBattleTest.objectHealUsesNativeTargetAndEffectLayouts` — expected: `<11722>` but was: `<744>`
28. `ProtocolLitoRoomParticipantTest.nativeAffiliationFieldRejectsOverlongOrInvalidRows` — Expected `IllegalArgumentException`, nothing thrown
29. `ClientSessionLitoModeStepIntegrationTest.encryptedRightNextAndLeftPreviousEmitExactS_D5ModeMapPairs` — `SocketTimeoutException: Read timed out`（error）
30. `NativeLitoRentalTeleportEndpointCatalogTest.exactTeleporterActionSendsStatusThenDirectEndpoint` — `SocketTimeoutException: Read timed out`（error）

## 未驗證什麼

- GitHub Actions `ubuntu-latest` + Temurin 17（本機 Java 21／`--release 17`）。
- 開發機「檔案都在」時那 328 類是否仍全綠。
- 上表 30 案在正式 JAR／原始來源上是否本來就綠。
- NativeExternalControlV2 fixture 的 hold／revision 實跑（需要 MySQL；靜態呼叫鏈已接回 `observeEntry`）。
- 實機 `--check`／`--smoke`。

## 產出路徑

- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/LocalResources.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/LocalResourcesTest.java`
- `lito伺服器/LiTo381-Server/src/test/java/**`（exact-path `@BeforeAll`；Healing/Vip TLS resolver；ExternalControl V2 `read`/`readForWorld` hook）
- `.github/workflows/build.yml`（`server-test` continue-on-error）
- `docs/reports/issue-12.md`
- `docs/plans/issue-12.json`

## 給下一棒的提示

- 審查：Issue #12 驗收已正式改為「失敗只剩上表 30 案 + 守門查實際路徑」。不要再要求本 PR 修那 30 案或改 `src/main`。
- 後續 30 案：`docs/plans/issue-12.json` 的 `native-pin-vs-t002`。可改 `src/main` 還原原生合約，或確認 T002 反編譯為準後改 pin。不要加預設 false 的功能開關。
- CI job 名稱寫 until T010 是 Issue 驗收原文；TASKS.md 既有 T010 是實機核對表。30 案對齊應另開任務，合入後再拿掉 `continue-on-error`。
- 與 PR #10（T005）可能搶 `build.yml`：本 PR 保留 T007 package＋jar-compare，只調整 `server-test`。
- `ClientSession` 需要非 null `CaptureLog`；測試用 `discardCapture()`。
- ExternalControl V2：production adapter 無 callback；測試 hook 在 `PhysicalProvider.readForWorld`。

# Issue #12 / T009 — 本機資源測試 assume 守門（第 2 次：收回假 skip）

## 做了什麼

第 1 次把沒有外部檔依賴的回歸測，用 `-Dlito.native.catalog.pins`／`-Dlito.classic.accounts` 預設停測，審查退回。第 2 次只留「檔案／環境真的不在才 skip」。

- `LocalResources` 只保留真實外部依賴守門：`assumeServerProperties`／`assumeLineageSources`／`assumeLitoBin`／`assumeIdaClient`，以及會讀 `java.exe`／`LITO_SHARED_RESOURCE_ROOT` 的 `assumeWindowsJvm`／`assumeSharedResourceRoot`。
- **刪除** `assumeNativeCatalogPins()`、`assumeClassicSourceAccounts()` 與全部呼叫（28 + 4）。無外部依賴的測試恢復預設執行。
- 經典帳號：不再用 boolean 開關。`ClientSessionLitoCreateRoomClanIntegrationTest`、`ClientSessionClassicWorldDeathPenaltyIntegrationTest`（owner + peer）、`ClientSessionRentalTownDirectoryTest.restartReturn…`、`ClientSessionWorldCkewCompletionTest.fixture()` 改注入 `MemoryAccountRepository`（不連正式 DB、不改 production）。
- 收回範圍外全域 extension：刪 `junit-platform.properties`、`META-INF/services/…Extension`、`LocalResourcesExtension`。330 類仍用顯式 `@BeforeAll assumeLocalResourcesPresent()`。
- 測試側編譯／API 對齊（未改 `src/main`）：`GameHubChannelTest` 補 `assertNotEquals`；`NativeExternalControlV2Fixture` 改 3 參數 `NativePlayerLoadoutAdapter`；12 個測試用 `discardCapture()`。
- CI：保留 T007 package＋`jar tf`；blocking `server-test`（`mvn -B test`，無 `continue-on-error`）。

### 守門涵蓋（測試類數；一類可同時屬多個）

| 守門 | 方法 | 涵蓋測試類 | 說明 |
|---|---|---|---|
| `server.properties` | `assumeServerProperties()` | **81** | 正式 `config/server.properties` |
| 天堂來源樹 | `assumeLineageSources()` | **253** | `lito伺服器/1-伺服器/db/380_db.sql` |
| `LiTo.bin` | `assumeLitoBin()` | **95** | `D:/380/.../LiTo.bin` |
| ida-client 反組譯 | `assumeIdaClient()` | **169** | `full-disassembly.asm` |
| **以上去重** | `@BeforeAll` | **330** | 硬讀本機檔的測試類 |
| Windows `java.exe` | `assumeWindowsJvm()` | **3**（4 個方法） | 真的去開 `java.home/bin/java.exe` |
| 共享資源根 | `assumeSharedResourceRoot()` | **2** | 真的讀 `LITO_SHARED_RESOURCE_ROOT` |

`ClientSessionWorldCkewCompletionTest` 在注入 memory accounts 後 4/4 通過。`restartReturnClearsEveryIsolatedTownFieldWithoutGameHubMembership` 通過。

## 怎麼驗證

- 本機無正式 `config/server.properties`、無 `1-伺服器`、無 `LiTo.bin`、無 ida-client、無 MySQL：`cd lito伺服器/LiTo381-Server && mvn -B test`
- **已驗證（本機資源守門）**：硬讀本機檔的類仍以 `@BeforeAll` abort（Surefire `Tests run: 0`），不再因缺檔而紅。
- **已驗證（收回假 skip 後全套）**：`Tests run: 6002, Failures: 28, Errors: 2, Skipped: 115`。這 30 案**不是**缺本機檔，而是現行 T002 反編譯 `src/main` 與測試 pin 不一致（見下）。未改 production。
- 未改 `config/server.properties`、releases、封存、`LiTo.bin`；未啟動遊戲、未連正式 DB。

## 原有失敗（不可當本機資源缺失；未在本任務修復）

T009 禁止改被測程式。下列 28 fail + 2 error 在拿掉 opt-in 後照實跑出，列給後續對齊 `src/main` 或還原原生行為（`docs/plans/issue-12.json`）：

| 測試 | 實際 vs 期望（摘要） |
|---|---|
| `BattleDisconnectTest.solePlayerDisconnectLosesToTheSyntheticArenaBot` | winners `[]` vs `[2]`（合成 Arena bot） |
| `BattleFoeSlayerCatalogTest.pinsSelectedPrimaryRowAndRetainedThreeStrikeContract` | gfx `6509` vs `12119` |
| `BattleSpellCatalogTest.createZombiePinsCorpseEnvelopeCostsAndNativeLevelTemplates` | template `45065` vs `81183` |
| `BattleCompanionTest.createdZombieUsesLevelSelectedTemplateAtFullVitals` | template `45065` vs `81186` |
| `ClassicWorldMonsterMagicProtocolTest.iceConeUsesExactSelectedClientExtendedAttackLayout` | hex 差 2 bytes |
| `ProtocolBattleTest.battleActorUsesPcStatusAndTailFieldsFromTheExactPutObjectReceiver` | tail `5` vs `0` |
| `ProtocolBattleTest.objectHealUsesNativeTargetAndEffectLayouts` | `744` vs `11722` |
| `ProtocolBattleTest.allThreeNativeAttackProducersUseTheSameTargetCoordinateEnvelope` | 應丟 IllegalArgumentException，沒丟 |
| `ProtocolLitoRoomParticipantTest.nativeAffiliationFieldRejectsOverlongOrInvalidRows` | 同上，沒丟 |
| `GameStateSourceInventoryTest.mirrorsSourceArmorNativeStatusAndRetainsLiToEquippedState` | status hex 少 `2701` |
| `GameStateSourceInventoryTest.retainedArmorStatusSeparatesBodyArmorFromAccessories` | status hex 少 `27` |
| `GameHubChannelTest.assignsAndListsRoomsInsideTheirOwningChannel` | 多出 `Local Lobby` |
| `GameHubChannelTest` matchmaking 4 案 | 選房／拒絕計數與 pin 不符 |
| `GameHubChannelTest.opcodeOnlyNativeStartIsAuthorizedOnlyForTheCurrentRoomOwner` | `NONE` vs `COUNTDOWN_STARTED` |
| `GameHubVirtualPlayerTest` 3 案 | `NONE` vs `COUNTDOWN_STARTED` |
| `ClientSessionGmDeIntegrationTest.databaseAuthorizedDeChatCreatesReadyRoomRowsAndStartsCountdown` | 房間數 `1` vs `4` |
| `ClientSessionLitoCreateRoomClanIntegrationTest.…EmitsSuccess` | C_D9 後 phase `RESULT` vs `CHANNEL`（memory accounts 已注入，不再 NPE） |
| `ClientSessionClassicWorldDeathPenaltyIntegrationTest.lowLevelVictimAndGmAttackerSuppressFeedAndCounters` | counter `1` vs `0` |
| `ClientSessionClassicWorldHellIntegrationTest.firstEntryEmits552AndPortalThenExpiryReturnsThroughPortalHandshake` | completion packets 空 |
| `ClientSessionRentalBattleHandoffIntegrationTest.newGameEndpointConsumesRentalLoadoutIntoActorAttackAndResultRestore` | startSoloBattle false |
| `ClientSessionRentalTownDirectoryTest.encryptedGroupOneChainBootstrapsResourceDerivedNativeTownScene` | 封包長 5 vs 9 |
| `ClientSessionRentalTownDirectoryTest.rentalTownPlayersShareRosterActorsMovementDirectionActionAndDeparture` | NPC `32770` vs `32772` |
| `NativeTournamentMatchEndEconomySourceAuditTest.roomExitBattleQuitAndDisconnectRemainIdempotentSafetyNets` | battle-quit false |
| `ClientSessionLitoModeStepIntegrationTest.encryptedRightNextAndLeftPreviousEmitExactS_D5ModeMapPairs` | SocketTimeout（error） |
| `NativeLitoRentalTeleportEndpointCatalogTest.exactTeleporterActionSendsStatusThenDirectEndpoint` | SocketTimeout（error） |

## 未驗證什麼

- GitHub Actions `ubuntu-latest` + Temurin 17（本機 Java 21／`--release 17`）。
- 開發機「檔案都在」時那 330 類是否仍全綠。
- 上表 30 案在正式 JAR／原始來源上是否本來就綠。
- 實機 `--check`／`--smoke`。

## 產出路徑

- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/LocalResources.java`
- `lito伺服器/LiTo381-Server/src/test/java/**`（330 類 `@BeforeAll` + memory accounts 注入）
- `.github/workflows/build.yml`（`server-test`）
- `docs/reports/issue-12.md`
- `docs/plans/issue-12.json`

已刪（範圍外）：`LocalResourcesExtension.java`、`src/test/resources/junit-platform.properties`、`src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension`。

## 給下一棒的提示

- Claude／審查：T009 的本機檔守門已就定位。CI `server-test` **會紅**，直到後續任務對齊上表 30 案。不要再把那些 pin 當成本機資源缺失來 skip。
- 後續：`docs/plans/issue-12.json`。可改 `src/main` 還原原生行為，或在確認 T002 反編譯為準後改測試 pin。T009 範圍不許改 production。
- 與 PR #10（T005）可能搶 `build.yml`：本 PR 保留 T007 package＋jar-compare，只加 `server-test`。
- `ClientSession` 需要非 null `CaptureLog`；測試用 `discardCapture()`。
- `TASKS.md` 尚無 T009。

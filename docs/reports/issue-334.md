# Issue #334 / T119 — CI 加 MySQL 測試庫，讓原本略過的整合測試在 CI 執行

PR：#343（分支 `task/334`）。不改 `src/main`、不改 `src/test`；只動 `.github/workflows/`、`lito伺服器/LiTo381-Server/db/ci/`、本報告。

## 做了什麼

1. **新 CI job `server-integration-test`**（`.github/workflows/build.yml`）
   - MySQL **5.7** service container（host 埠 33306），帳密為 workflow 環境變數裡的固定測試值（`lito_ci` / `lito_ci_test_only`，root `lito_ci_root_test_only`），只用於這個用完即丟的容器，不是任何正式部署的密碼。
   - **為什麼選 5.7**：repo 內 `db/lito_static.sql:9`、`db/380_ac_1.sql:9` 與 `1-伺服器/db/380_db.sql`、`380_ac.sql`、`380_lito.sql` 標頭都寫 `Source Server Version : 50522 (5.5.22)`，表多為 MyISAM、5.5 時代預設值。5.7 是仍有官方映像、最接近 5.5 行為的版本；8.0 會改預設驗證外掛、保留字與 utf8 行為。載入前 `SET GLOBAL sql_mode=''`，對齊 5.5 伺服器預設。JDBC 驅動 `mysql-connector-j 8.0.33`（`pom.xml`）支援 5.7。
   - 步驟：產生 fixture → `docker exec` 用容器內 `mysql` 匯入 → 由 `config/server.properties.example` 產生 CI 設定到 `$RUNNER_TEMP/lito-ci/config/server.properties`，再 symlink 成 `config/server.properties`（該路徑已在根 `.gitignore:87`，job 內還用 `git check-ignore` 確認）→ `mvn -B test`。
   - 原 `server-test`（無 DB）保留當「啟用前」基準；兩個 job 都用 `.github/workflows/scripts/surefire_summary.py` 輸出 `surefire-summary.json`（略過／失敗清單），隨 artifact 上傳。
   - 新 job 先設 `continue-on-error: true`（job 名稱標明 non-blocking），理由見下「結果」；Maven 仍以真實非零值結束，job 顯示紅色，不隱藏失敗。這跟 T009（#12）的做法相同。
2. **測試資料庫 fixture**（`lito伺服器/LiTo381-Server/db/ci/`）
   - `build_ci_database.py`：只讀 repo 內檔案，輸出到 runner 暫存目錄。
     - `380_db`（靜態庫）：`db/lito_static.sql` 整份（它就是 `380_db` 的 dump，只有模板／設定表，沒有 accounts／characters），只把標頭的庫名 `lito_static` 換成 `380_db`。
     - `380_ac`、`380_ac_1`、`380_ac_2`（來源角色庫，對應 `db/lito_source_server.sql` 的三個啟用來源）：**只取 `db/380_ac_1.sql` 的 schema**。所有 `INSERT` 行（20208 列真實帳號、角色、物品、紀錄）都丟掉，產生後會再檢查沒有殘留 INSERT。另外補 `character_lito`（欄位同 `1-伺服器/db/380_ac.sql:983-992`），再套 `db/lito_battle_settlement.sql` 的 `last_result_id` 遷移。
     - `380_lito`（控制庫）：四張 `lito_battle_*`／`lito_tournament_shop` 表結構取自 `db/lito_static.sql`（改 InnoDB），`lito_battle_player_spawn` 欄位依 `1-伺服器/db/380_lito.sql:94-104`（repo 內沒有這張表的 DDL）。資料依序來自 `380_lito_seed_example.sql`、`lito_source_server.sql`（表結構；來源列由 fixture 寫入，三服啟用，4–8 停用）、`lito_room_policy.sql`、`lito_commands.sql`、`lito_resource_compact.sql`、`lito_battle_settlement.sql`。
     - 權限：用 `tools/db-grants/generated/grants.sql`（正式最小權限範本），帳號換成 `'lito_ci'@'%'`，來源庫段落複製到三個來源庫。也就是 CI 用跟正式一樣的只讀＋兩表寫入權限跑。
   - `fake_source_data.sql`：每個來源庫放 2 個假帳號（`ci_player`、`ci_gm`）、2 個假角色（`CiKnight`、`CiWizard`）、`character_other` 兩列，全部是自己編的值。
   - `make_ci_server_properties.py`：從 `server.properties.example` 複製，只換 `database.jdbc.base.url`／`database.user`／`database.password` 三個鍵，其他鍵都維持範例值，所以測試會經過正式那套 `UnifiedConfiguration` 嚴格鍵檢查。
3. 處理過的問題：`db/` 裡有一個 SQL 檔帶 UTF-8 BOM，串接後 MySQL 回 1064。產生器改用 `utf-8-sig` 讀取（第 2 個 commit）。

## 結果（CI run 43038775716，head `f868ac9e`；最新 commit 的 run 見 PR）

### 略過數：啟用前後對照

| 項目 | 無 MySQL（`server-test`） | 有 CI MySQL（`server-integration-test`） |
|---|---:|---:|
| Maven `Tests run` | 7340 | **7579**（+239） |
| Failures / Errors | 0 / 0 | **39 / 34** |
| Maven `Skipped` | 117 | 119 |
| 實際執行的測試類（相較基準多出來的） | — | **70 類** |
| 新執行的案例：通過／失敗或錯誤／略過 | — | **164 / 73 / 2** |
| 基準就有的 7340 案，狀態有變化 | — | 0 |

說明：`@BeforeAll` 裡的 `assumeServerProperties()` 不成立時，JUnit 5 會讓整個類別中止，Surefire **不會**把這些方法記成 skipped，所以 Maven 的 `Skipped` 幾乎沒變（117→119）。真正的差別在多跑了 70 類、239 案。多出的 2 個 skipped 是 `LitoMySqlSourceWeaponIntegrationTest`、`LitoMySqlStaticControlIntegrationTest`：它們還需要 `-Dlito.mysql.integration`／`1-伺服器/config/sql.properties`，不在本任務範圍。

#308／#309 指名的兩個類：
- `ClientSessionBoneBreakIntegrationTest`：5 案跑了 4 案通過（含 #309 的 `lawful=0` 斷言），1 案錯誤（A 類）。
- `ClientSessionNativeTournamentEconomyTest`：67 案跑了 30 案通過、37 案失敗或錯誤（B／C／D 類）。

### 新跑起來的測試（70 類）

| 測試類 | 通過 | 失敗／錯誤 | 略過 |
|---|---:|---:|---:|
| `AttackAllProbeConfigTest` | 3 | 0 | 0 |
| `BattleArmBreakerActorLifecycleTest` | 3 | 0 | 0 |
| `BattleCubeHarmonyCatalogTest` | 2 | 0 | 0 |
| `BattleCubeIgnitionCatalogTest` | 5 | 0 | 0 |
| `BattleCubeQuakeCatalogTest` | 3 | 0 | 0 |
| `BattleCubeShockCatalogTest` | 3 | 0 | 0 |
| `BattleIllusionAvatarCatalogTest` | 4 | 0 | 0 |
| `BattleInsightCatalogTest` | 4 | 0 | 0 |
| `BattleJoyOfPainActorLifecycleTest` | 3 | 0 | 0 |
| `BattleJoyOfPainCatalogTest` | 3 | 0 | 0 |
| `BattlePanicActorLifecycleTest` | 3 | 0 | 0 |
| `BattlePanicCatalogTest` | 3 | 0 | 0 |
| `BattlePatienceCatalogTest` | 3 | 0 | 0 |
| `BattlePhantasmActorLifecycleTest` | 2 | 0 | 0 |
| `BattlePhantasmCatalogTest` | 3 | 0 | 0 |
| `BookListProbeConfigTest` | 3 | 0 | 0 |
| `CharacterEventProbeConfigTest` | 3 | 0 | 0 |
| `ClassicWorldDeathPenaltyConfigTest` | 1 | 0 | 0 |
| `ClassicWorldDropConfigTest` | 1 | 0 | 0 |
| `ClassicWorldMonsterAiConfigTest` | 1 | 0 | 0 |
| `ClassicWorldPirateShipConfigTest` | 2 | 0 | 0 |
| `ClientSessionArmBreakerIntegrationTest` | 3 | 0 | 0 |
| `ClientSessionBattleResultStatisticsTest` | 0 | 2 | 0 |
| `ClientSessionBoneBreakIntegrationTest` | 4 | 1 | 0 |
| `ClientSessionCallOfNatureIntegrationTest` | 0 | 2 | 0 |
| `ClientSessionClassicWorldGroundIntegrationTest` | 0 | 1 | 0 |
| `ClientSessionClassicWorldLineAttackIntegrationTest` | 0 | 1 | 0 |
| `ClientSessionClassicWorldLineAttackMonsterIntegrationTest` | 0 | 1 | 0 |
| `ClientSessionClassicWorldMonsterGiveIntegrationTest` | 1 | 6 | 0 |
| `ClientSessionConcentrationIntegrationTest` | 3 | 0 | 0 |
| `ClientSessionConfusionIntegrationTest` | 0 | 2 | 0 |
| `ClientSessionCubeHarmonyIntegrationTest` | 0 | 1 | 0 |
| `ClientSessionCubeIgnitionIntegrationTest` | 0 | 1 | 0 |
| `ClientSessionCubeQuakeIntegrationTest` | 0 | 1 | 0 |
| `ClientSessionCubeShockIntegrationTest` | 0 | 1 | 0 |
| `ClientSessionDiamondGolemIntegrationTest` | 2 | 0 | 0 |
| `ClientSessionHorrorOfDeathIntegrationTest` | 0 | 1 | 0 |
| `ClientSessionIllusionAvatarIntegrationTest` | 2 | 0 | 0 |
| `ClientSessionIllusionLichIntegrationTest` | 3 | 0 | 0 |
| `ClientSessionIllusionOgreIntegrationTest` | 3 | 0 | 0 |
| `ClientSessionInsightIntegrationTest` | 2 | 0 | 0 |
| `ClientSessionJoyOfPainIntegrationTest` | 2 | 0 | 0 |
| `ClientSessionLitoChannelSelectionIntegrationTest` | 5 | 1 | 0 |
| `ClientSessionMeditationIntegrationTest` | 2 | 0 | 0 |
| `ClientSessionMindBreakIntegrationTest` | 4 | 0 | 0 |
| `ClientSessionMortalBodyIntegrationTest` | 0 | 1 | 0 |
| `ClientSessionNativeDropIntegrationTest` | 6 | 5 | 0 |
| `ClientSessionNativeTournamentEconomyTest` | 30 | 37 | 0 |
| `ClientSessionPanicIntegrationTest` | 1 | 0 | 0 |
| `ClientSessionPatienceIntegrationTest` | 1 | 0 | 0 |
| `ClientSessionPcActionRewardIntegrationTest` | 1 | 0 | 0 |
| `ClientSessionPhantasmIntegrationTest` | 2 | 0 | 0 |
| `ClientSessionResurrectionMapGateIntegrationTest` | 0 | 1 | 0 |
| `ClientSessionSmashIntegrationTest` | 2 | 0 | 0 |
| `ClientSessionThunderGrabIntegrationTest` | 1 | 0 | 0 |
| `CloneAppearanceProbeConfigTest` | 1 | 1 | 0 |
| `DedicatedServerLayoutTest` | 2 | 2 | 0 |
| `FoeSlayerConfigTest` | 1 | 0 | 0 |
| `GameStateEphemeralSourceProfileTest` | 10 | 0 | 0 |
| `LauncherRentalDualServerConfigTest` | 1 | 0 | 0 |
| `LitoDatabaseLayoutTest` | 0 | 3 | 0 |
| `LitoDisplayNameTest` | 1 | 0 | 0 |
| `LitoMySqlSourceWeaponIntegrationTest` | 0 | 0 | 1 |
| `LitoMySqlStaticControlIntegrationTest` | 0 | 0 | 1 |
| `NativeLitoMapBoundaryConfigTest` | 3 | 0 | 0 |
| `NativeTournamentEconomyConfigTest` | 2 | 0 | 0 |
| `OriginSourceConfigTest` | 0 | 1 | 0 |
| `RentalTownDirectoryConfigTest` | 1 | 0 | 0 |
| `ServerConfigChannelPortsTest` | 3 | 0 | 0 |
| `TransferStoneProbeConfigTest` | 1 | 0 | 0 |

### 73 個跑起來才失敗的案例：分類

| 類 | 數 | 判定 | 依據 |
|---|---:|---|---|
| A | 5 | **測試過時**［已驗證］ | 測試把 `bone.break.fixture.d1000`、`confusion.fixture.d100` 等鍵附加到 `config/server.properties` 再載入。0.6.71 的單一設定檔由 `UnifiedConfiguration.validate`（`S/UnifiedConfiguration.java:7,24`）用固定鍵集合檢查，未知鍵直接拒絕。所以只要用正式格式的設定，就算在使用者本機也一定失敗。修法：改用 `src/test/resources/server-config/config/server.*.properties` 那種測試專用、非 unified 的設定。 |
| B | 26 | **測試過時**［推測］ | 測試用 `accounts=null` 建 `ClientSession`。正式設定一定走共用來源模式（`UnifiedConfiguration.apply` 固定 `database.mode=lito_mysql`、`database.read.only=true`，`S/UnifiedConfiguration.java:73`），這時 `ephemeralSourceProfiles()` 為真（`S/ServerConfig.java:975-977`），程式會呼叫 `accounts.repositoryForClassicSource(...)`。正式啟動時 `accounts` 一定存在（`S/Main.java:43`），所以推測是測試 harness 沒跟上共用來源模式，不是正式路徑的 NPE。修法：測試注入 `Accounts`／`AccountRepository` 測試替身，或改用非 unified 測試設定。 |
| C | 6 | 待查（傾向測試設定不符）［推測］ | 預期的加密封包沒送出（`Read timed out`、`missing encrypted opcode 16`、`skill target metadata not loaded`）。`UnifiedConfiguration.apply` 會強制 `classic.world.compatibility.enabled=false`、`battle.compatibility.service.actors.enabled=false`（`S/UnifiedConfiguration.java:74`），而這些案例名稱都有 "BattleWorld／InBattleAndWorld"，要走世界相容路徑。 |
| D | 29 | 待查（測試前提或靜態資料差異）［待驗證］ | 數值或布林斷言不符。其中 `encryptedWaterDragonHealingErosion…` 的 `expected <11722> but was <744>`，和 T009（`docs/reports/issue-12.md:69`，`ProtocolBattleTest.objectHealUsesNativeTargetAndEffectLayouts`）是同一組數值，是已知問題。多數案例前置步驟在 `startSoloBattle`（`ClientSessionNativeTournamentEconomyTest.java:7562`）失敗。推測和 unified 設定的 `battle.ai.enabled=false`（範例值）或單人開戰規則（`lito_battle_rule.minimum_players`、`lito_room_policy`）有關。`80 vs 82`、`145 vs 200` 這類 MP／HP 數值也可能是 `lito_static.sql`（2026-08-14 dump）和使用者本機 `380_db` 的差異。需要逐案追。 |
| E | 7 | **測試過時**［已驗證］ | 綁定已退役的部署佈局或本機特定值：`DedicatedServerLayoutTest` 要求 `config/` 有 `database.properties`、`source-servers.properties`（0.6.71 已是單一 `server.properties`，見 `server.properties.example` 開頭，使用者本機 `config/` 也只有 `server.properties`、`combat.properties`［只列檔名，未讀內容］）。`LitoDatabaseLayoutTest` 要求 `db/generated/`（`db/README.zh-TW.md` 記載已移除），並期望來源只有 `380_ac_1,380_ac_2`（現行種子 `db/lito_source_server.sql` 是三服）。`OriginSourceConfigTest` 要求 `server.name=LiTo 共用資料架構`（範例值是 `LiTo Standard Arena`，這是使用者本機設定的值）。`CloneAppearanceProbeConfigTest` 的來源稽核斷言不成立，需要再看。 |

本輪**沒有修改任何測試或程式**：
- A／B／E 都是測試過時，但修法牽涉「這批測試該用哪一種設定（正式 unified 或測試專用）」，會一次改 30 幾案、上萬行的整合測試，超出本任務「接上 CI MySQL」的範圍，交給下一棒（見提示）。
- C／D 還沒確定是測試還是程式的問題，不能隨便改斷言。
- 任務規定不改 `src/main`，所以就算是程式問題，本輪也只記錄。

#### 逐案清單（類別欄：A–E 同上；種類欄 F=failure、E=error）

| # | 類 | 測試 | 種類 | CI 訊息 |
|---|---|---|---|---|
| 1 | A | `ClientSessionBoneBreakIntegrationTest#classicWorldSelectorUsesTheSameDamageStatusAndEffectLifecycle` | E | Unknown/retired setting: bone.break.fixture.d1000 |
| 2 | A | `ClientSessionConfusionIntegrationTest#exactSelectorPreservesProbabilityOrderAndNonRefreshingSilence` | E | Unknown/retired setting: confusion.fixture.d100 |
| 3 | A | `ClientSessionConfusionIntegrationTest#exactSelectorUsesTheSameContractInClassicWorld` | E | Unknown/retired setting: confusion.fixture.d100 |
| 4 | A | `ClientSessionHorrorOfDeathIntegrationTest#exact141800ConsumesWardThenAppliesAndCancellationRestoresStats` | E | Unknown/retired setting: horror.of.death.fixture.d100 |
| 5 | A | `ClientSessionMortalBodyIntegrationTest#battleCastConsumesHpAndAcceptedPlayerDamageReflectsOnce` | E | Unknown/retired setting: mortal.body.fixture.d100 |
| 6 | B | `ClientSessionBattleResultStatisticsTest#fatalTransitionPublishesIdenticalAuthoritativeKillAndDeathRows` | E | selectedSourceAccounts |
| 7 | B | `ClientSessionBattleResultStatisticsTest#timeoutPublishesScoresBeforeResettingTheMatch` | E | selectedSourceAccounts |
| 8 | B | `ClientSessionCallOfNatureIntegrationTest#nativeObjectRequestGatesThenResurrectsPlayersInBattleAndWorldAtFullHp` | E | Cannot invoke "local.lito381.server.AccountRepository.repositoryForClassicSource(String)" because "this.accounts" is null |
| 9 | B | `ClientSessionClassicWorldGroundIntegrationTest#nativeWorldGroundCastsCommitReducedMpReagentVisualsAndAuthoritativeEffects` | E | combat accessor ClientSession.selectedSourceAccounts failed |
| 10 | B | `ClientSessionClassicWorldMonsterGiveIntegrationTest#worldCDropAndCGetKeepLitLanternStateAndRecomputePlayerLight` | E | Cannot invoke "local.lito381.server.AccountRepository.repositoryForClassicSource(String)" because "this.accounts" is null |
| 11 | B | `ClientSessionClassicWorldMonsterGiveIntegrationTest#worldCGiveAllowsLitLanternAndPublishesTargetThenPlayerLight` | E | Cannot invoke "local.lito381.server.AccountRepository.repositoryForClassicSource(String)" because "this.accounts" is null |
| 12 | B | `ClientSessionClassicWorldMonsterGiveIntegrationTest#worldCGiveCommitsPlayerDebitAndMonsterOnGetItemLifecycle` | E | Cannot invoke "local.lito381.server.AccountRepository.repositoryForClassicSource(String)" because "this.accounts" is null |
| 13 | B | `ClientSessionClassicWorldMonsterGiveIntegrationTest#worldCGiveRoutesPromotedLiveGuardToMovingNpcBag` | E | Cannot invoke "local.lito381.server.AccountRepository.repositoryForClassicSource(String)" because "this.accounts" is null |
| 14 | B | `ClientSessionClassicWorldMonsterGiveIntegrationTest#worldCGiveRoutesRetainedStaticTeleporterToInstanceScopedNpcBag` | E | Cannot invoke "local.lito381.server.AccountRepository.repositoryForClassicSource(String)" because "this.accounts" is null |
| 15 | B | `ClientSessionClassicWorldMonsterGiveIntegrationTest#worldCGiveToPetPublishesImmediateNativeHealingUse` | E | Cannot invoke "local.lito381.server.AccountRepository.repositoryForClassicSource(String)" because "this.accounts" is null |
| 16 | B | `ClientSessionCubeIgnitionIntegrationTest#worldSelectorPlacesAtCasterConsumesOnlyAcceptedCastAndAppliesAllyLease` | E | combat accessor ClientSession.selectedSourceAccounts failed |
| 17 | B | `ClientSessionCubeQuakeIntegrationTest#worldSelectorPlacesAtCasterCommitsOnlyAcceptedCastAndAddsEarthLease` | E | combat accessor ClientSession.selectedSourceAccounts failed |
| 18 | B | `ClientSessionCubeShockIntegrationTest#worldSelectorPlacesCommitsOnlyAcceptedCastAndAddsWindLease` | E | combat accessor ClientSession.selectedSourceAccounts failed |
| 19 | B | `ClientSessionNativeDropIntegrationTest#administratorBypassesTheRetainedGlobalAndTradabilityGates` | E | Cannot invoke "local.lito381.server.AccountRepository.repositoryForClassicSource(String)" because "this.accounts" is null |
| 20 | B | `ClientSessionNativeDropIntegrationTest#ordinaryPlayerGetsNativeEquipmentTradabilityAndActiveCollarMessages` | E | Cannot invoke "local.lito381.server.AccountRepository.repositoryForClassicSource(String)" because "this.accounts" is null |
| 21 | B | `ClientSessionNativeDropIntegrationTest#retainedGlobalGateSends125WhileWhitelistedMeatStillDrops` | E | Cannot invoke "local.lito381.server.AccountRepository.repositoryForClassicSource(String)" because "this.accounts" is null |
| 22 | B | `ClientSessionNativeDropIntegrationTest#sourceMagicDollSummonsRecallsAndExpiresThroughNativePackets` | E | Cannot invoke "local.lito381.server.AccountRepository.repositoryForClassicSource(String)" because "this.accounts" is null |
| 23 | B | `ClientSessionNativeDropIntegrationTest#timedActiveDollAndSealedItemsUseExactNativeDropMessages` | E | Cannot invoke "local.lito381.server.AccountRepository.repositoryForClassicSource(String)" because "this.accounts" is null |
| 24 | B | `ClientSessionNativeTournamentEconomyTest#battleRankingResultDoesNotReplaceTheCurrentMatchAllowance` | E | selectedSourceAccounts |
| 25 | B | `ClientSessionNativeTournamentEconomyTest#nativeBurningWeaponUsesCase162SelfTargetBattleWorldAndPersistence` | E | Cannot invoke "local.lito381.server.AccountRepository.repositoryForClassicSource(String)" because "this.accounts" is null |
| 26 | B | `ClientSessionNativeTournamentEconomyTest#nativeElementalFallDownLowersEncryptedEarthStatusAndExpiryRestoresIt` | E | combat accessor ClientSession.nativeSourceRemainingOriginal133 failed |
| 27 | B | `ClientSessionNativeTournamentEconomyTest#nativeElementalFireUsesCase170InBattleAndWorldWithTransientMeleeProc` | E | Cannot invoke "local.lito381.server.AccountRepository.repositoryForClassicSource(String)" because "this.accounts" is null |
| 28 | B | `ClientSessionNativeTournamentEconomyTest#nativeNaturesBlessingUsesSelectorOnlyWaterGateModifiersAndBothWorlds` | E | combat accessor ClientSession.nativeSourceHealingOriginalNature failed |
| 29 | B | `ClientSessionNativeTournamentEconomyTest#nativePolluteWaterUsesCase172InWorldWithWaterGateAndPrimaryBypass` | E | Cannot invoke "local.lito381.server.AccountRepository.repositoryForClassicSource(String)" because "this.accounts" is null |
| 30 | B | `ClientSessionNativeTournamentEconomyTest#nativeSoulOfFlameUsesCase174AndSelectedSourceReuseInBattleAndWorld` | E | Cannot invoke "local.lito381.server.AccountRepository.repositoryForClassicSource(String)" because "this.accounts" is null |
| 31 | B | `ClientSessionNativeTournamentEconomyTest#nativeStormWalkUsesCase171EconomyOnlyInBattleAndWorld` | E | Cannot invoke "local.lito381.server.AccountRepository.repositoryForClassicSource(String)" because "this.accounts" is null |
| 32 | C | `ClientSessionNativeTournamentEconomyTest#encryptedHonorDeathWaitsFiveSecondsAndResurrectsAtDeathPosition` | E | Read timed out |
| 33 | C | `ClientSessionNativeTournamentEconomyTest#nativeAdditionalFireUsesCase175AndBypassesWeightRegenBattleWorld` | F | missing encrypted opcode 16 within 2 frames |
| 34 | C | `ClientSessionNativeTournamentEconomyTest#nativeEarthSkinUsesCase150ObjectEnvelopeInBattleAndWorld` | E | Read timed out |
| 35 | C | `ClientSessionNativeTournamentEconomyTest#nativeExoticVitalizeUsesCase168AndBypassesWeightRegenBattleWorld` | F | missing encrypted opcode 16 within 2 frames |
| 36 | C | `ClientSessionNativeTournamentEconomyTest#nativeWaterLifeUsesCase169TargetsPcAndDoublesOnlyNextHeal` | F | missing encrypted opcode 16 within 2 frames |
| 37 | C | `ClientSessionNativeTournamentEconomyTest#nativeWindShackleUsesCccdhhHitsScreenAndCounterMagicThenThrottlesActions` | E | skill target metadata not loaded |
| 38 | D | `ClientSessionCallOfNatureIntegrationTest#nativeCallOfNatureRestoresBattleCompanionAndSelectedWorldMonsterAtFullHp` | F | expected: <true> but was: <false> |
| 39 | D | `ClientSessionClassicWorldLineAttackIntegrationTest#lightningUsesAuthoritativeLineIndependentDamageAndPerTargetCounterMagic` | F | expected: <80> but was: <82> |
| 40 | D | `ClientSessionClassicWorldLineAttackMonsterIntegrationTest#lightningDamagesProvokesAndPublishesOrdinaryWorldMonster` | F | expected: <80> but was: <82> |
| 41 | D | `ClientSessionCubeHarmonyIntegrationTest#worldCastCommitsTwoStonesThenAppliesNativeMpHpClock` | F | expected: <145> but was: <200> |
| 42 | D | `ClientSessionLitoChannelSelectionIntegrationTest#nativeLobbyExitQuitRehydratesCachedCharacterBeforeChannelReentry` | F | array lengths differ, expected: <13> but was: <19> |
| 43 | D | `ClientSessionNativeTournamentEconomyTest#battleChillTouchDamagesTheNativeBotAndHealsOnlyAfterAcceptance` | F | expected: <true> but was: <false> |
| 44 | D | `ClientSessionNativeTournamentEconomyTest#battleInstantTeleportUsesDirectPortalAndReleasesCursorAfterRebuild` | F | expected: <true> but was: <false> |
| 45 | D | `ClientSessionNativeTournamentEconomyTest#battleSkillFiveTeleportUsesNativePacketDirectPortalAndUnlocksAfterRebuild` | F | expected: <true> but was: <false> |
| 46 | D | `ClientSessionNativeTournamentEconomyTest#emptyFireStormStillEmitsNativeTypeZeroEffectEnvelope` | F | expected: <true> but was: <false> |
| 47 | D | `ClientSessionNativeTournamentEconomyTest#encryptedHealingPotionsPublishNativeGfxAndConsumeAtFullHp` | F | expected: <true> but was: <false> |
| 48 | D | `ClientSessionNativeTournamentEconomyTest#encryptedLightningWandConsumesSixChargesThenReturnsNativeEmptyMessage` | F | expected: <true> but was: <false> |
| 49 | D | `ClientSessionNativeTournamentEconomyTest#encryptedWaterDragonHealingErosionTurnsHealingIntoFatalDamage` | F | expected: <11722> but was: <744> |
| 50 | D | `ClientSessionNativeTournamentEconomyTest#encryptedWaterDragonPotionErosionPublishesEffectAndFatalDamageFlow` | F | expected: <true> but was: <false> |
| 51 | D | `ClientSessionNativeTournamentEconomyTest#lethalDisintegrateAgainstPlayerDefersConfirmedResultUntilEffectEnds` | F | expected: <true> but was: <false> |
| 52 | D | `ClientSessionNativeTournamentEconomyTest#lethalDisintegrateKeepsNativeWorldAliveUntilItsSixtyFramesComplete` | F | expected: <true> but was: <false> |
| 53 | D | `ClientSessionNativeTournamentEconomyTest#nativeAreaSilenceHitsCasterAndVisiblePcWithoutConsumingCounterMagic` | F | expected: <8> but was: <16> |
| 54 | D | `ClientSessionNativeTournamentEconomyTest#nativeClearMindConsumesCrystalPublishesWisdomAndRestoresItOnExpiry` | F | expected: <0> but was: <1> |
| 55 | D | `ClientSessionNativeTournamentEconomyTest#nativeCounterMirrorConsumesCrystalAndReflectsEncryptedPlayerMagicOnce` | F | expected: <0> but was: <1> |
| 56 | D | `ClientSessionNativeTournamentEconomyTest#nativeElementalProtectionRejectsNoAttributeAndRoutesBattleAndWorld` | F | expected: <2> but was: <3> |
| 57 | D | `ClientSessionNativeTournamentEconomyTest#nativeEntangleUsesCase151WireConsumesCrystalSlowsAndStopsWith711` | F | expected: <23> but was: <12> |
| 58 | D | `ClientSessionNativeTournamentEconomyTest#nativeEraseMagicUsesCase152QuarterMrAndBypassesCounterMagic` | F | expected: <23> but was: <12> |
| 59 | D | `ClientSessionNativeTournamentEconomyTest#nativeGreaterElementalUsesCase161EarthNpcAndExistingPetCostNoOp` | F | expected: <true> but was: <false> |
| 60 | D | `ClientSessionNativeTournamentEconomyTest#nativeLesserElementalUsesCase153EarthNpcAndExistingPetCostNoOp` | F | expected: <true> but was: <false> |
| 61 | D | `ClientSessionNativeTournamentEconomyTest#nativePolluteWaterBypassesCounterMagicAndHalvesEncryptedPotionRecovery` | F | expected: <23> but was: <12> |
| 62 | D | `ClientSessionNativeTournamentEconomyTest#nativePolluteWaterCase172AppliesToClassicWorldMonster` | F | expected: <0> but was: <1> |
| 63 | D | `ClientSessionNativeTournamentEconomyTest#nativeResistElementalPublishesFourResistancesAndFeedsDamageConsumer` | F | expected: <1> but was: <2> |
| 64 | D | `ClientSessionNativeTournamentEconomyTest#nativeReturnToNatureCoversSuccessInvalidPetAndProbabilityFailure` | F | expected: <5> but was: <7> |
| 65 | D | `ClientSessionNativeTournamentEconomyTest#nativeStrikerGaleCase173UsesFiveCrystalsWindGateErZeroAndSourceCounterMagic` | F | expected: <23> but was: <12> |
| 66 | D | `ClientSessionResurrectionMapGateIntegrationTest#map303ConsumesAndPublishesCastButSuppressesQuestion321` | F | expected: <55> but was: <56> |
| 67 | E | `CloneAppearanceProbeConfigTest#productionSourceHasOnlyMutationAndExactRestoreInsideFixture` | F | expected: <true> but was: <false> |
| 68 | E | `DedicatedServerLayoutTest#operationalConfigContainsOnlyServerAndDatabaseProperties` | F | expected: <[server.properties, source-servers.properties, database.properties]> but was: <[server.properties, combat.properties]> |
| 69 | E | `DedicatedServerLayoutTest#serverConfigIsDedicatedAndDoesNotReachIntoReferenceProject` | F | expected: <true> but was: <false> |
| 70 | E | `LitoDatabaseLayoutTest#generatedLayoutContainsOneStaticAndTwoCompleteDynamicSchemas` | F | db/generated/lito_static.sql ==> expected: <true> but was: <false> |
| 71 | E | `LitoDatabaseLayoutTest#generatorOwnsEveryRequiredLitoExtensionTable` | F | expected: <true> but was: <false> |
| 72 | E | `LitoDatabaseLayoutTest#productionConfigsBindEveryDynamicRouteToTheSameStaticSchema` | F | standard ==> expected: <[380_ac_1, 380_ac_2]> but was: <[380_ac, 380_ac_1, 380_ac_2]> |
| 73 | E | `OriginSourceConfigTest#defaultProfileRoutesTheDedicatedReadOnlyCharacterStores` | F | expected: <LiTo 共用資料架構> but was: <LiTo Standard Arena> |

## 怎麼驗證

- **已驗證**：CI run [43038775716](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/43038775716)（head `f868ac9e`）。`server-test` 7340 run / 0 fail / 117 skipped，BUILD SUCCESS。`server-integration-test` 的 fixture 匯入成功，`mvn -B test` 7579 run / 39 failures / 34 errors / 119 skipped；耗時：基準 6:46、整合 11:28。上面的數字是下載兩個 artifact（`server-test-evidence`、`server-integration-test-evidence`），用 Surefire XML 逐案集合比對算出來的：基準的 7340 案在兩邊狀態完全相同，新增的 239 案都來自上表 70 類。
- **已驗證（本機，只跑產生器 Python，沒有執行任何編譯出的程式）**：`build_ci_database.py` 在本機輸出到 `scratch/issue-334/sql/`。三個來源庫各只有 3 段 INSERT（都是 `fake_source_data.sql` 的假資料），`lito_source_server` 寫入 8 列（1–3 啟用），所有輸出都沒有 BOM。
- 本 PR 最新 commit（加 `continue-on-error` 與本報告）的 CI 結果以 PR #343 頁面為準。

## 未驗證什麼

- C／D 共 35 案的根因沒有逐案追；分類欄已標［推測］／［待驗證］。
- `lito_static.sql` 和使用者本機 `380_db` 的資料差異沒有比對（不能連正式 DB）。
- 沒有啟動伺服器、登入器或遊戲，沒有連任何正式 DB（2000／3306），沒有讀取本機 `config/server.properties` 的內容。

## 產出路徑

- `.github/workflows/build.yml`（新 job `server-integration-test`；`server-test` 加略過摘要）
- `.github/workflows/scripts/surefire_summary.py`
- `lito伺服器/LiTo381-Server/db/ci/build_ci_database.py`、`fake_source_data.sql`、`make_ci_server_properties.py`
- `docs/reports/issue-334.md`

## 給下一棒的提示

1. **建議開 T119a（code）：整合測試改用測試專用設定**。A／B／E 共 38 案。先決定這批 `ClientSession*IntegrationTest` 要驗的是「正式 unified 設定」還是「開發設定」：
   - 如果是後者，把 `Path.of("config/server.properties")` 改成 `src/test/resources/server-config/config/` 的專用檔，加上 CI MySQL 連線鍵；
   - 如果是前者，測試要注入 `Accounts` 替身，並且不能再附加 fixture 鍵。
   E 類（`DedicatedServerLayoutTest`、`LitoDatabaseLayoutTest`、`OriginSourceConfigTest`）要改成符合 0.6.71 單一設定檔佈局，或直接刪掉。
2. **建議開 T119b（code）：C／D 35 案逐案追**。先看 `startSoloBattle`（`ClientSessionNativeTournamentEconomyTest.java:7562`）為什麼不能單人開戰。追 `11722 vs 744` 時一併處理 T009 的同一組已知失敗。
3. 上述修完、`server-integration-test` 全綠後，拿掉該 job 的 `continue-on-error` 改成 blocking，也可以考慮合併成單一 `server-test`。
4. #318（bless 換算）、#310 D3（積分持久化）要在真 DB 上驗證，現在可以直接在 `server-integration-test` 加測試：fixture 的 `character_lito`、`lito_battle_settlement` 已建好，權限跟正式一樣（只有兩張表可寫）。
5. fixture 要加資料時，改 `db/ci/fake_source_data.sql`。**不要**改成匯入 `380_ac_1.sql` 的 INSERT（那是真實玩家資料）。

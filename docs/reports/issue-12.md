# Issue #12 / T009 — 實際本機資源守門

## 做了什麼

依 14:58 調整後的驗收，只處理本機資源依賴；保留真實斷言與例外，交 T010 處理。不改 `src/main`，不刪測試、不使用功能旗標隱藏失敗。

- 各類 `@BeforeAll` 改列實際讀取的完整 Path；SQL、來源 Java、相對／絕對反組譯、BIN、unpacked、分析 JSON、照片等均不再用無關檔案代替。來源樹只有 SQL 存在時，不會放行缺少 Java／次要 SQL 的測試。
- 統一 `LocalResources` 讀取 helper 也檢查動態組出的外部路徑；傳入 `Files` 的就是同一 Path。專案內檔案與測試生成檔仍保留正常 IOException／斷言行為。
- 共享資源只用 `LITO_SHARED_RESOURCE_ROOT`，並檢查實際 map 514 sector；property 或其他 map 檔不能代替。
- `server-test` 設 `continue-on-error: true`，名稱含 `non-blocking until T010`；Maven 仍保留真實非零退出碼。`if: always()` 上傳完整 Maven log、Surefire XML／文字與執行 revision。
- 保留遠端 `d375d932` 的實質修正：`ModernStandaloneMainTest` 無外部依賴，移除其錯誤守門；`PcActionRewardSqlFirstSourceAuditTest` 只在 overlay 方法守門；納入 `ModernVipWorldTlsTest` resolver 與 `NativeExternalControlV2Fixture.readForWorld` 觀察 hook。後者僅靜態核對，尚未以 MySQL 驗證時點等價。兩個遠端回歸案例亦保留。
- `docs/REPORT.md` 保留 main 版；前次越界的 `docs/plans/issue-12.json` 收回，後續建議集中本報告。

## 守門涵蓋

各列可重疊，按使用 helper 的測試類計數，不含 helper 自身回歸測試。

| 類型 | 測試類 |
|---|---:|
| 正式 server.properties | 80 |
| 天堂 SQL／來源檔 | 251 |
| LiTo.bin | 93 |
| 實際反組譯檔 | 157 |
| 其他明列的證據檔 | 100 |
| Windows java.exe | 3 |
| 共享地圖 | 2 |

顯式類級守門 326 類；上表前五類去重 327 類（包含一個方法級外部依賴）；另兩個環境變數來源測試保留原環境前提，在實際讀檔處檢查來源檔。

## 怎麼驗證

- **已驗證（驗收環境）**：Ubuntu / Temurin 17、無 DB／本機資源的 `mvn -B test`：**6033 run / 28 failures / 2 errors / 117 skipped**。下載全部 **1266 份 Surefire XML** 做集合比對：與下列 30 案完全相同，額外失敗 0、漏列 0；`LocalResourcesTest` 10/10 通過。Maven 真實退出 1，依核准的 job `continue-on-error`，整體 workflow SUCCESS。
- CI 對應 PR head **`3874d174f6a0ee3b6084285bdf89e4cf7a3ee375`**；GitHub checkout 的合併 revision 為 **`821af9648df18416f83b05b4173c9657674d436e`**（artifact 內 `test-revision.txt`）。[CI 執行與三個 jobs](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41793623352)、[完整 server-test-evidence artifact](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41793623352/artifacts/13304274771)。可重現下載：`gh run download 41793623352 -n server-test-evidence`；內容含 `mvn-test.log`、`test-revision.txt`、`surefire-reports/`。
- 已驗證：Windows Microsoft OpenJDK 17.0.17 / Maven 3.9.16，全部測試來源編譯通過；合併後 `LocalResourcesTest` 10/10 通過（本輪新增 8 案＋遠端保留 2 案）。覆蓋其他 BIN／ida 候選存在、SQL 存在但來源缺失、動態路徑缺失、錯把目錄當檔案、共享 root property／錯誤 sector。
- 已驗證：整合前 `988d910` 測試程式碼的 Windows 全套 `mvn -o -B test`：**6021 run / 27 failures / 2 errors / 109 skipped**，8 分 44 秒。解析全部 Surefire XML 核對失敗集合為下列 30 案的子集，無缺檔錯誤。原始證據：`lito伺服器/LiTo381-Server/target/issue-12-full.log`、`target/issue-12-full-reports/`（不進 git）。本機仍有硬編碼 D: BIN，因此無本機資源的正式驗收以 Ubuntu CI 為準。快取複製至 worktree `scratch/issue-12/maven-repository`，`java.io.tmpdir`／TEMP／TMP 指向 `target/issue-12-tmp`，未啟用 DB 或共享資源環境變數。
- 已驗證：整合後定向測試 `LocalResourcesTest,ModernStandaloneMainTest,PcActionRewardSqlFirstSourceAuditTest,ModernVipWorldTlsTest,ModernPlayerHealingTlsTest`：**20 run / 0 failures / 0 errors / 1 skipped**，另 2 類缺共享資源於 BeforeAll abort。完整 log：`target/issue-12-merge-check.log`。
- 未啟動遊戲或正式服務、未連正式 DB、未讀寫正式設定、未修改正式 BIN／封存／release。

## 真實失敗清單（30 案基準，本輪結果逐項核對）

每行為測試類／方法／CI 實際失敗訊息；Ubuntu 完整驗證正好 28 failures + 2 errors。Windows 的失敗為其中 29 案，第 7 案 Hell 在 Windows 通過、Ubuntu 失敗；保留平台／時間相關差異供 T010 調查。

1. `BattleCompanionTest.createdZombieUsesLevelSelectedTemplateAtFullVitals` — `expected: <81186> but was: <45065>`
2. `BattleDisconnectTest.solePlayerDisconnectLosesToTheSyntheticArenaBot` — `expected: <[2]> but was: <[]>`
3. `BattleFoeSlayerCatalogTest.pinsSelectedPrimaryRowAndRetainedThreeStrikeContract` — `expected: <12119> but was: <6509>`
4. `BattleSpellCatalogTest.createZombiePinsCorpseEnvelopeCostsAndNativeLevelTemplates` — `expected: <81183> but was: <45065>`
5. `ClassicWorldMonsterMagicProtocolTest.iceConeUsesExactSelectedClientExtendedAttackLayout` — `expected: <231293150065100000000A0621AA4400DA2D00A184B57EA084B57E0000> but was: <231293150065100000000A0621AA4400110700A184B57EA084B57E0000>`
6. `ClientSessionClassicWorldDeathPenaltyIntegrationTest.lowLevelVictimAndGmAttackerSuppressFeedAndCounters` — `expected: <0> but was: <1>`
7. `ClientSessionClassicWorldHellIntegrationTest.firstEntryEmits552AndPortalThenExpiryReturnsThroughPortalHandshake` — `completion packets=[] ==> expected: <true> but was: <false>`
8. `ClientSessionGmDeIntegrationTest.databaseAuthorizedDeChatCreatesReadyRoomRowsAndStartsCountdown` — `expected: <4> but was: <1>`
9. `ClientSessionLitoCreateRoomClanIntegrationTest.compactCreateAutomaticallyJoinsAClannedCreatorAndEmitsSuccess` — `expected: <CHANNEL> but was: <RESULT>`
10. `ClientSessionRentalBattleHandoffIntegrationTest.newGameEndpointConsumesRentalLoadoutIntoActorAttackAndResultRestore` — `expected: <true> but was: <false>`
11. `ClientSessionRentalTownDirectoryTest.encryptedGroupOneChainBootstrapsResourceDerivedNativeTownScene` — `array lengths differ, expected: <9> but was: <5>`
12. `ClientSessionRentalTownDirectoryTest.rentalTownPlayersShareRosterActorsMovementDirectionActionAndDeparture` — `expected: <32772> but was: <32770>`
13. `GameHubChannelTest.assignsAndListsRoomsInsideTheirOwningChannel` — `expected: <[Baphomet Arena]> but was: <[Local Lobby, Baphomet Arena]>`
14. `GameHubChannelTest.matchmakingAppliesLevelAndObserverEligibilityBeforeSuccess` — `expected: <0> but was: <2>`
15. `GameHubChannelTest.matchmakingNeverSelectsTheServerGatedRentalTownMode` — `expected: <0> but was: <1>`
16. `GameHubChannelTest.matchmakingRejectsWhenOnlyProtectedOrFullRoomsExist` — `expected: <0> but was: <1>`
17. `GameHubChannelTest.matchmakingSkipsProtectedRoomsAndSelectsAnEligiblePublicRoom` — `expected: <3> but was: <1>`
18. `GameHubChannelTest.opcodeOnlyNativeStartIsAuthorizedOnlyForTheCurrentRoomOwner` — `expected: <COUNTDOWN_STARTED> but was: <NONE>`
19. `GameHubVirtualPlayerTest.deRosterHonorsCapacityServerNamesBalanceAndAutomaticReadiness` — `expected: <COUNTDOWN_STARTED> but was: <NONE>`
20. `GameHubVirtualPlayerTest.everyNativeBattleModeAndMapStartsWithDatabaseCommandVirtualRoster` — `PET_RACE/565 ==> expected: <COUNTDOWN_STARTED> but was: <NONE>`
21. `GameHubVirtualPlayerTest.siegeVirtualRosterSatisfiesNativeFifteenPerForceMinimum` — `expected: <COUNTDOWN_STARTED> but was: <NONE>`
22. `GameStateSourceInventoryTest.mirrorsSourceArmorNativeStatusAndRetainsLiToEquippedState` — `expected: <1300142701000000077F0D630EFF7F207F> but was: <13001401000000077F0D630EFF7F207F>`
23. `GameStateSourceInventoryTest.retainedArmorStatusSeparatesBodyArmorFromAccessories` — `expected: <13050B271E00000002040302077F> but was: <13050B1E00000002040302077F>`
24. `NativeTournamentMatchEndEconomySourceAuditTest.roomExitBattleQuitAndDisconnectRemainIdempotentSafetyNets` — `battle-quit ==> expected: <true> but was: <false>`
25. `ProtocolBattleTest.allThreeNativeAttackProducersUseTheSameTargetCoordinateEnvelope` — `Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.`
26. `ProtocolBattleTest.battleActorUsesPcStatusAndTailFieldsFromTheExactPutObjectReceiver` — `expected: <0> but was: <5>`
27. `ProtocolBattleTest.objectHealUsesNativeTargetAndEffectLayouts` — `expected: <11722> but was: <744>`
28. `ProtocolLitoRoomParticipantTest.nativeAffiliationFieldRejectsOverlongOrInvalidRows` — `Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.`
29. `ClientSessionLitoModeStepIntegrationTest.encryptedRightNextAndLeftPreviousEmitExactS_D5ModeMapPairs` — `java.net.SocketTimeoutException: Read timed out`（error）
30. `NativeLitoRentalTeleportEndpointCatalogTest.exactTeleporterActionSendsStatusThenDirectEndpoint` — `java.net.SocketTimeoutException: Read timed out`（error）


## 未驗證什麼

資源齊全時的來源一致性、正式 JAR 對這 30 案的行為、實機 --check／--smoke 均未驗證。Ubuntu CI 已完成並核對，證據見上方連結。

## 產出路徑

- `lito伺服器/LiTo381-Server/src/test/java/**`
- `.github/workflows/build.yml`
- `docs/reports/issue-12.md`

## 給下一棒的提示

Claude 審查守門與上述同程式碼 CI artifact（最後補記結果僅改報告，測試／workflow 與 3874d174 完全相同）；T010 接手上列真實失敗，修復後移除 non-blocking。不得恢復假 skip。另須核對 `NativeExternalControlV2Fixture` 在真實 readForWorld／Work 內的 observe／hold 時點；現行 production adapter 沒有舊 callback；保留遠端以 provider hook 接回的方案，但 hook 位於 adapter 讀取 store 之前，與原回呼的精確時點及 hold 期間 revision 變更是否等價，須由隔離 MySQL 採證，不能用編譯通過代表完成。

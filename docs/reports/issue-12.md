# Issue #12 / T009 — 實際本機資源守門

## 做了什麼

依 14:58 調整後的驗收，只處理本機資源依賴；保留真實斷言與例外，交 T010 處理。不改 `src/main`，不刪測試、不使用功能旗標隱藏失敗。

- 各類 `@BeforeAll` 改列實際讀取的完整 Path；SQL、來源 Java、相對／絕對反組譯、BIN、unpacked、分析 JSON、照片等均不再用無關檔案代替。來源樹只有 SQL 存在時，不會放行缺少 Java／次要 SQL 的測試。
- 統一 `LocalResources` 讀取 helper 也檢查動態組出的外部路徑；傳入 `Files` 的就是同一 Path。專案內檔案與測試生成檔仍保留正常 IOException／斷言行為。
- 共享資源只用 `LITO_SHARED_RESOURCE_ROOT`，並檢查實際 map 514 sector；property 或其他 map 檔不能代替。
- `server-test` 設 `continue-on-error: true`，名稱含 `non-blocking until T010`；Maven 仍保留真實非零退出碼。`if: always()` 上傳完整 Maven log、Surefire XML／文字與執行 revision。
- `docs/REPORT.md` 保留 main 版；前次越界的 `docs/plans/issue-12.json` 收回，後續建議集中本報告。

## 守門涵蓋

各列可重疊，按使用 helper 的測試類計數，不含 helper 自身回歸測試。

| 類型 | 測試類 |
|---|---:|
| 正式 server.properties | 81 |
| 天堂 SQL／來源檔 | 251 |
| LiTo.bin | 93 |
| 實際反組譯檔 | 157 |
| 其他明列的證據檔 | 100 |
| Windows java.exe | 3 |
| 共享地圖 | 1 |

顯式類級守門 328 類；另兩個環境變數來源測試保留原環境前提，在實際讀檔處檢查來源檔。

## 怎麼驗證

- 已驗證：Windows Microsoft OpenJDK 17.0.17 / Maven 3.9.16，全部測試來源編譯通過；新增 `LocalResourcesTest` 8/8 通過。覆蓋其他 BIN／ida 候選存在、SQL 存在但來源缺失、動態路徑缺失、錯把目錄當檔案、共享 root property／錯誤 sector。
- 全套本機 `mvn -o -B test` 正在執行；本報告將以完整 Surefire 結果更新。快取複製至 worktree `scratch/issue-12/maven-repository`，`java.io.tmpdir`／TEMP／TMP 指向 `target/issue-12-tmp`，未啟用 DB 或共享資源環境變數。
- 未啟動遊戲或正式服務、未連正式 DB、未讀寫正式設定、未修改正式 BIN／封存／release。

## 真實失敗清單（待本輪全套核對；遠端 d375d932 回報基準）

每行為測試類／方法／訊息；預期 28 failures + 2 errors。

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

資源齊全時的來源一致性、正式 JAR 對這 30 案的行為、實機 --check／--smoke 均未驗證。Ubuntu CI 結果待本輪 push 後核對。

## 產出路徑

- `lito伺服器/LiTo381-Server/src/test/java/**`
- `.github/workflows/build.yml`
- `docs/reports/issue-12.md`

## 給下一棒的提示

Claude 審查守門與完整 CI artifact；T010 接手上列真實失敗，修復後移除 non-blocking。不得恢復假 skip。另須核對 `NativeExternalControlV2Fixture` 在真實 readForWorld／Work 內的 observe／hold 時點；現行 production adapter 沒有舊 callback，這項需要隔離 MySQL 的實際採證，不能用編譯通過代表完成。

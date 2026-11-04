# T020a：戰鬥候選與正式 0.6.71 類別差異盤點

Issue #30，2026-11-04，第 3 次嘗試。完整逐 entry 證據見 [class-diff.json](class-diff.json)。

**逐列來源驗收剩餘 1 筆：GmTravel 已復原完整歷史 Java 並重現相同 class；NativeArmorStatus 的最後寫入層與來源仍缺件。**

## 已驗證

**8d26 與正式只有 4 個 CHANGED、4 個 ONLY_IN_FORMAL；50635 相對 8d26 有 140 個 CHANGED、483 個 NEW_IN_CANDIDATE。** 三個指定 JAR SHA 全部符合。Maven package 成功，但與正式仍有大量 bytes 差異，不能把建置成功當成等價驗證。

### 比較輸入

| 輸入 | SHA-256 | class 數 |
|---|---|---:|
| 第一批 8d26 | `8d26c9d791e094ab3d3cd6c8dbe4218d99f96fcbe73858be5af534106c81cfb8` | 2550 |
| 第二批 50635 | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` | 3033 |
| 正式 reference | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` | 2554 |
| Maven target/LiTo381-Server.jar | `0aefa242a7889e3dc1cad348c4d9650579325503e51c3efaa70e49e631182866` | 3929 |

原始本機候選路徑以 `D:/lineage開發/3-lito開發/` 為根；正式與 Maven 以本 worktree 為根。精確路徑、收據與所有 SHA 列於 JSON。使用指定 final-frame current 收據指向的 `20261027T060934904682Z/candidate-negative-final-frame-unqualified.jar`。沒有使用 `75da69b0…` 作比較輸入。

### 分類計數

| 比較（待比較端 → 參考端） | SAME | CHANGED | NEW_IN_CANDIDATE | ONLY_IN_FORMAL |
|---|---:|---:|---:|---:|
| 8d26c9d7 對正式 0.6.71 | 2546 | 4 | 0 | 4 |
| 50635f7e 對 8d26c9d7 | 2410 | 140 | 483 | 0 |
| Maven 產物對正式 0.6.71 | 1008 | 1541 | 1380 | 5 |

第三組為 **Maven → 正式**：NEW 表示 Maven 新增，ONLY_IN_FORMAL 表示正式有而 Maven 無。第二組的參考端是 8d26。所有數量包含 `$` 內部／匿名類別；不是 Java 檔案數。

### 來源與層序

- 以 receipt 的 `base.path` / `base.sha256` 逐層驗證，8d26 的戰鬥鏈含 67 個寫入層（根建置、BUFF 合成、65 層），50635 含 289 層；再各加 1 個共同的 `df9464f0…` 輸入根。未按照檔案修改時間猜測，也沒有把全部 252 支 integration 腳本當作實際祖先。
- `last_layer` 指最後一次 bytes 改變。同 bytes 的重編／重封裝不改內容來源；未聲稱已重播 patch 鏈。
- 第二批 623 筆變動：**380 筆完整 Java、243 筆 asm-only**。380 筆來源 SHA 全部符合凍結收據，對應 `classes/` 產物 SHA 也全部等於最終候選 entry。未重新編譯這些凍結來源。
- 第二批 29 個變動 entry 對應 19 個 `// DECOMPILED` 檔；483 個 entry 在 `src/main` 尚無對應檔。Maven 差異涉及 592 個 DECOMPILED entry、266 個不同 Java 檔。
- 第一批 ClientSession 的最後變動層是 `cancellation-battle-runtime`，Protocol 是 `summon-parity`，兩者為 ASM。GmTravel 最後寫入層已證明為 `resource-database/build.py`（efd4bc36）；完整 Java 已復原且精確重現候選 class。NativeArmorStatus 在已銜接的全部層中保持原 bytes，歷史來源仍缺件。
- 多頂層類別共用 Java 檔時，以 Maven class 的 `SourceFile` 屬性解析；內部類別沿用外層 Java 的 DECOMPILED 標記。

### 歷史來源與第 3 次補證

以下路徑以原始專案 `D:/lineage開發/3-lito開發/` 為根，`S` 表示 `lito伺服器/LiTo381-Server`，`P` 表示 `lito登入器/LiTo381-Launcher/server-patches`；`scratch/`、`target/` 則位於本 worktree。

- **GmTravel 已補齊**：59 條 resource 父鏈將共同根 `df9464f0…` 接到 `efd4bc36…`。`S/tools/resource-database/build/build-manifest.json` 釘選來源 SHA `319b3d460756991907cc03b3a662c15b4212a92ffbfc52109f840fd45a624ffe`。由現版 Java 唯一刪除 `        if (GmPolyMe.command(s,message)) return true;` 與其 CRLF，整份 Java SHA 符合歷史收據。復原檔 `scratch/issue-30/recovered/sources/GmTravel.java` 隔離編譯的 class SHA `8561ad6243444e94417b924e1928e90937b7b481e9eab5eabb77311a3a8dd985` 等於候選；本輪再次核對。重現方法在 JSON `retry_review.source_recovery`。
- **NativeArmorStatus 新增四條父鏈邊**：前輪 12 條早期邊停止於 `004b1ba8…`；本輪讀取前輪範圍外的登入器 `server-patches`，並用既有清理收據銜接 `shared-database`，早期邊增為 16 條。

| 層 | 輸出 SHA 前綴 | 父輸入 SHA 前綴 | 實測不同 class 數 | 連結證據 |
|---|---|---|---:|---|
| repair-042 | 004b1ba8 | eeb0ba0b | 7 | candidate.jar 等於 source-administration 固定輸入；build.py、Patch.java 均釘選父 SHA |
| repair-041 | eeb0ba0b | 8b9f0319 | 4 | candidate.jar 等於 repair-042/base.jar；雙重父 SHA pin |
| repair-040 | 8b9f0319 | 4a3bb9be | 3 | candidate.jar 等於 repair-041/base.jar；雙重父 SHA pin |
| shared-database/build.py | 4a3bb9be | 3f31ef32 | 10 | 清理收據的 removed entry 記錄舊 candidate.jar SHA／大小；保留的 repair-040/base.jar 完全吻合；builder 與 PatchShared.java 釘選父 SHA |

四條邊的 NativeArmorStatus 都未改，entry SHA 全為 `c2da4b0cb66fcbb629090d82c2008e1ed79f16525b2bcf10cc091bd2128b7b1f`。`shared-database/build.py` 先跑 `PatchShared` 再跑 `PatchSourceLogin`，最後改名回 candidate.jar；兩支 ASM 未選取該類別。**這是未改動的證據，不能把它填成 asm-only 最後寫入層。** 未重播 patch，也未讀取清理收據所指的封存檔。

完整 SHA、檔案路徑、builder／ASM SHA、pin 行、差異 class 清單與清理收據 JSON pointer 已內嵌於 `attempt3_review.additional_parent_edges`。可重跑 `py -3 -B -X utf8 scratch/issue-30/attempt3-trace.py`；結果 `PASS 4 earlier edges, armor unchanged; origin still missing`。手動重現：逐一計算各 JAR SHA，核對下一層輸入與 builder 的父 SHA；從各 JAR 解讀該 entry 並計算 SHA。最後一邊另按 JSON pointer 讀 `S/docs/maintenance/layout-cleanup-20261012.json` 的路徑／SHA／大小。

- **新的來源邊界**：`S/tools/shared-database/build/base-0.6.35.jar`，SHA `3f31ef32c8ca1ca8f9507897f03b54ca37593326ad986b5c10fcff68f695ed5c`。此前獨立探查的 0.6.35 現已接上父鏈；但它已含相同 class，仍缺更早的真正最後寫入層與完整歷史 Java／可驗證 ASM 修改證據。
- 本輪搜尋登入器 tools／server-patches／docs，以及伺服器早期 tools／docs，共 562 個文字檔、19 個命中。排除封存、連結、執行紀錄及 combat-continuation／resource-database 等；完整根目錄、查詢及排除項在 `attempt3_review.source_search`。沒有取得 3f31ef32 之前的新父鏈或歷史 Java；此為限定範圍結果，不是全專案不存在的宣告。Git `--all` 的 InventoryCatalog.java 仍只有初始化 commit `6bb4a27bb6cfbef1639d155700b09c6e1bfbf34f`。
- 現有 InventoryCatalog.java SHA `338a3a4299bd35499cae6be58a7c71628c1cccfa0d3ee3f3ab871fdc7f444bc9` 的前輪重編 class SHA `a93dbd3d007e9c6504667e99db85218118f95d0138b928e65d6a905a8adbc121` 不等於候選。33 個正規化方法指令清單相同，不能代替完整歷史來源證明；探查結果保留於 `rerun_review.armor_compile_probe`。
- 本輪 `mvn -B -DskipTests package` 於 2026-11-04 21:30:00 +08:00 成功，重編 940 個主來源、1,413 個測試來源，測試略過；3,929 個 class bytes 與前輪全部相同。log：`scratch/issue-30/maven-package-attempt3.log`。
- `last_layer=null`、`source=null`、`acceptance_passed=false`、`unresolved_entries=1` 保持不變。未通過逐列來源驗收；阻塞為 `historical-provenance-missing`，由任務擁有者／雲端主控補件，不能無條件自動重排。

### T029：正式功能保留清單

| class（省略套件前綴） | 對 8d26 分類 | 正式改、候選未改／雙方皆改 | 保留內容 |
|---|---|---|---|
| `ClientSession.class` | CHANGED | both-changed | 正式呼叫 GmPolyMe.exitRoom、SelectionRecovery.finish、SharedPresentationPackets.bluePotion；sendRoomParticipant 七職業接線也需保留。候選另有大量戰鬥變更，必須三方合併。 |
| `GmPolyMe$Forms.class` | ONLY_IN_FORMAL | formal-only-added | GmPolyMe 的內部型別，與外層一同保留。 |
| `GmPolyMe.class` | ONLY_IN_FORMAL | formal-only-added | 正式專屬完整類別：.polyme、7200 秒與離房清理；來源 tools/gm-polyme/build.py、Patch.java 與 src/main 的 GmPolyMe.java。 |
| `GmTravel.class` | CHANGED | formal-changed-candidate-unchanged | 正式 command() 先呼叫 GmPolyMe.command；保留 .polyme / L1PolyMe 的入口。 |
| `InventoryCatalog$NativeArmorStatus.class` | CHANGED | formal-changed-candidate-unchanged | 正式建構子允許 displayArmorClass 超過 255；detail() 寫封包前 Math.min(value,255)。候選仍限制 255。 |
| `Protocol.class` | CHANGED | both-changed | 正式 litoRoomParticipant 允許職業 0..6；magicDollIcon / magicDollCustomIcon 使用 opcode 123（候選 250），前者另補 h(0)。 |
| `SelectionRecovery.class` | ONLY_IN_FORMAL | formal-only-added | 正式專屬選角恢復 helper；同時保留 ClientSession.run 的 finish() 呼叫。 |
| `SharedPresentationPackets.class` | ONLY_IN_FORMAL | formal-only-added | 正式專屬展示封包 helper；同時保留 ClientSession 的 bluePotion() 呼叫。 |

嚴格的「正式改、候選未改」以共同根作判斷：GmTravel、InventoryCatalog$NativeArmorStatus 是原有類別；另 4 個 ONLY_IN_FORMAL 為正式新增。這 6 筆在 50635 也仍未補入。ClientSession 與 Protocol 在候選鏈亦有修改，屬三方衝突，不能整類覆蓋。

職業圖示、`litoddraw.dll`、LTicon/LTdesc 部署還有 JAR 以外的部分：T029 應核對 `lito登入器/LiTo381-Launcher/docs/maintenance/release-0.6.70.json`；本次只讀該收據，未讀取／複製資源包或驗證現場 DLL。GM 證據見 `tools/gm-polyme/build.py`、`Patch.java`、`finalize.py` 與既有 `release-0.6.71.json`；javap 靜態呼叫證據留在 `scratch/issue-30/javap/`。

### Maven 相對正式缺少的 5 個 entry

- `local/lito381/server/BattleInvisibilityStatus$Lease.class`（外層來源存在，內部結構不同）
- `local/lito381/server/BattleInvisibilityStatus$Snapshot.class`（外層來源存在，內部結構不同）
- `local/lito381/server/NativeCastMaterial$Effect.class`（外層來源存在，內部結構不同）
- `local/lito381/server/NativeDeVisibilitySwitch.class`（src/main 無對應檔）
- `local/lito381/server/NativeResurrectionMaterialAction$Target.class`（外層來源存在，內部結構不同）

### 各組前 50 大差異

依 **class 檔大小差的絕對值** 排序；不是語意或風險排名。第一組只有 8 筆，全部列出。完整 SAME 與其他差異皆保留於 JSON。

#### 8d26c9d7 對正式 0.6.71

| class（省略套件前綴） | 分類 | 參考端 bytes | 待比較端 bytes | 差值 | 最後內容層 |
|---|---|---:|---:|---:|---|
| `GmPolyMe.class` | ONLY_IN_FORMAL | 10370 | — | -10370 | — |
| `SelectionRecovery.class` | ONLY_IN_FORMAL | 2038 | — | -2038 | — |
| `GmPolyMe$Forms.class` | ONLY_IN_FORMAL | 568 | — | -568 | — |
| `SharedPresentationPackets.class` | ONLY_IN_FORMAL | 482 | — | -482 | — |
| `ClientSession.class` | CHANGED | 2401065 | 2400761 | -304 | cancellation-battle-runtime |
| `GmTravel.class` | CHANGED | 15560 | 15412 | -148 | resource-database/build.py（efd4bc36；Java 已復原驗證） |
| `InventoryCatalog$NativeArmorStatus.class` | CHANGED | 7515 | 7507 | -8 | 未確認；已追至 0.6.35 固定輸入 3f31ef32 |
| `Protocol.class` | CHANGED | 162179 | 162174 | -5 | summon-parity |

#### 50635f7e 對 8d26c9d7

| class（省略套件前綴） | 分類 | 參考端 bytes | 待比較端 bytes | 差值 | 最後內容層 |
|---|---|---:|---:|---:|---|
| `ClientSession.class` | CHANGED | 2400761 | 2513092 | +112331 | source-self-final-frame-builds/20261027T022006468248Z |
| `NativeSourcePhysicalNpc.class` | NEW_IN_CANDIDATE | — | 39356 | +39356 | source-physical-transport-builds/20261025T213721561169Z |
| `NativeStaticNpcMotion.class` | NEW_IN_CANDIDATE | — | 35511 | +35511 | source-weapon-final-frame-builds/20261027T003145208155Z |
| `NativeSourcePoisonNpc.class` | NEW_IN_CANDIDATE | — | 31538 | +31538 | source-poison-audience-builds/20261026T090853848393Z |
| `NativeSourceCubeNpc.class` | NEW_IN_CANDIDATE | — | 30437 | +30437 | source-element-audience-builds/20261026T085853042470Z |
| `NativeSourceEquipmentColumns.class` | NEW_IN_CANDIDATE | — | 29583 | +29583 | source-equipment-columns-after-builds/20261023T183731723262Z |
| `NativeSourceCubeStatus.class` | NEW_IN_CANDIDATE | — | 25091 | +25091 | source-cube-template-builds/20261024T230955602499Z |
| `NativeSourceTargetNpc.class` | NEW_IN_CANDIDATE | — | 24976 | +24976 | source-target-npc-builds/20261024T170506603862Z |
| `NativeSourceControlledDisplay.class` | NEW_IN_CANDIDATE | — | 23714 | +23714 | source-controlled-role-frame-builds/20261026T093934385805Z |
| `NativeSourceGroundNpc.class` | NEW_IN_CANDIDATE | — | 23546 | +23546 | source-ground-npc-builds/20261024T202253372846Z |
| `NativeCreateZombieLifecycle.class` | NEW_IN_CANDIDATE | — | 22847 | +22847 | create-zombie-GM-policy-builds/20261020T174019820608Z |
| `NativeSourceSingleNpc.class` | NEW_IN_CANDIDATE | — | 21352 | +21352 | source-special-npc-builds/20261024T172245895664Z |
| `NativeSourceCubePlayer.class` | NEW_IN_CANDIDATE | — | 21313 | +21313 | source-cube-npc-lease-builds/20261024T134604737709Z |
| `NativeSourceRawDamage.class` | NEW_IN_CANDIDATE | — | 20557 | +20557 | source-raw-audience-builds/20261025T210423900170Z |
| `NativeBlindSceneAction.class` | NEW_IN_CANDIDATE | — | 20350 | +20350 | source-target-los-builds/20261025T073658964177Z |
| `NativeSourceDollNpc.class` | NEW_IN_CANDIDATE | — | 20254 | +20254 | source-physical-npc-builds/20261024T155605846444Z |
| `NativeCompanionPlacement.class` | NEW_IN_CANDIDATE | — | 20029 | +20029 | companion-placement-builds/20261020T212151413411Z |
| `NativeAcceptedAreaFreezeMutation.class` | NEW_IN_CANDIDATE | — | 19418 | +19418 | source-indirect-player-proof-builds/20261025T042626516276Z |
| `NativeSourceSpecialNpc.class` | NEW_IN_CANDIDATE | — | 18831 | +18831 | source-special-npc-builds/20261024T172245895664Z |
| `NativeSourceNpcEscape.class` | NEW_IN_CANDIDATE | — | 18803 | +18803 | source-npc-player-view-builds/20261025T163837848110Z |
| `NativeWeaponBreakTargetLifecycle.class` | NEW_IN_CANDIDATE | — | 18522 | +18522 | source-material-pair-ingress-builds/20261027T011504074268Z |
| `NativeBattleControlledCancellation.class` | NEW_IN_CANDIDATE | — | 18287 | +18287 | source-object-target-request-builds/20261025T062319779678Z |
| `NativeShockPlayerAction.class` | NEW_IN_CANDIDATE | — | 18076 | +18076 | source-target-npc-builds/20261024T170506603862Z |
| `NativeAcceptedFreezeMutation.class` | NEW_IN_CANDIDATE | — | 17954 | +17954 | freeze-source-target-builds/20261023T095113618398Z |
| `NativeStaticCancellationReplacement.class` | NEW_IN_CANDIDATE | — | 17819 | +17819 | static-respawn-builds/20261023T015628928604Z |
| `NativeBattleTamedLifecycle.class` | NEW_IN_CANDIDATE | — | 17745 | +17745 | battle-tamed-builds/20261020T114750718938Z |
| `NativeSourceNpcMagicEffects.class` | NEW_IN_CANDIDATE | — | 17318 | +17318 | source-npc-magic-effects-builds/20261025T000317963798Z |
| `GameHub.class` | CHANGED | 372209 | 389457 | +17248 | source-companion-clock-builds/20261025T172535321911Z |
| `NativeSummonGroundTransfer$Stage.class` | NEW_IN_CANDIDATE | — | 16859 | +16859 | companion-nature-scene-builds/20261021T000224303806Z |
| `NativeSourceRemainingMaterials.class` | NEW_IN_CANDIDATE | — | 16406 | +16406 | source-none-target-request-builds/20261025T061239167422Z |
| `NativeSourceMassSlowTargets.class` | NEW_IN_CANDIDATE | — | 16082 | +16082 | source-none-target-request-builds/20261025T061239167422Z |
| `NativeCompanionResurrection.class` | NEW_IN_CANDIDATE | — | 15956 | +15956 | companion-resurrection-scene-builds/20261021T004104071112Z |
| `NativeSourceCubeRemainder.class` | NEW_IN_CANDIDATE | — | 15561 | +15561 | source-cube-visibility-builds/20261024T224502391547Z |
| `NativeStaticNpcRespawn.class` | NEW_IN_CANDIDATE | — | 15514 | +15514 | static-respawn-builds/20261023T015628928604Z |
| `NativeSummonOwnerLifecycle.class` | NEW_IN_CANDIDATE | — | 15416 | +15416 | summon-terminal-builds/20261020T183144742509Z |
| `NativeSourceArenaPoison.class` | NEW_IN_CANDIDATE | — | 15087 | +15087 | source-poison-audience-builds/20261026T090853848393Z |
| `NativeSourceWindTargets.class` | NEW_IN_CANDIDATE | — | 14698 | +14698 | source-none-target-request-builds/20261025T061239167422Z |
| `NativeSourceDollSkills.class` | NEW_IN_CANDIDATE | — | 14504 | +14504 | source-doll-slow-builds/20261024T113132857068Z |
| `NativeSourceNegativeTargets.class` | NEW_IN_CANDIDATE | — | 14498 | +14498 | source-negative-final-frame-builds/20261027T060934904682Z |
| `NativeSourceMagicNpc.class` | NEW_IN_CANDIDATE | — | 14486 | +14486 | source-player-factories-builds/20261025T024030035183Z |
| `NativeSourceNpcPlayer.class` | NEW_IN_CANDIDATE | — | 14217 | +14217 | source-companion-clock-builds/20261025T172535321911Z |
| `NativeCompanionTargets.class` | NEW_IN_CANDIDATE | — | 14215 | +14215 | companion-portal-identity-builds/20261021T022506409166Z |
| `NativeSourceEquipmentOutput.class` | NEW_IN_CANDIDATE | — | 14183 | +14183 | source-equipment-final-output-builds/20261025T204122164382Z |
| `NativeShockCompanionAction.class` | NEW_IN_CANDIDATE | — | 13664 | +13664 | source-controlled-display-builds/20261024T072605536902Z |
| `BattleQuestEncounter.class` | CHANGED | 66921 | 53259 | -13662 | source-npc-player-view-builds/20261025T163837848110Z |
| `NativeTamedWorldLifecycle.class` | NEW_IN_CANDIDATE | — | 13589 | +13589 | tamed-builds/20261020T094837055072Z |
| `NativeSourceFatalityDamage.class` | NEW_IN_CANDIDATE | — | 13285 | +13285 | source-accepted-players-builds/20261025T030814249142Z |
| `NativeSourceSelfFinalFrame.class` | NEW_IN_CANDIDATE | — | 13272 | +13272 | source-self-final-frame-builds/20261027T022006468248Z |
| `NativeSourceCubeVisibility.class` | NEW_IN_CANDIDATE | — | 13236 | +13236 | source-cube-visibility-builds/20261024T224502391547Z |
| `NativeSourceProcScalarSkills.class` | NEW_IN_CANDIDATE | — | 13119 | +13119 | source-proc-scalar-extras-builds/20261026T212449597736Z |

#### Maven 產物對正式 0.6.71

| class（省略套件前綴） | 分類 | 參考端 bytes | 待比較端 bytes | 差值 | 最後內容層 |
|---|---|---:|---:|---:|---|
| `ModernBattleEngine.class` | NEW_IN_CANDIDATE | — | 301697 | +301697 | src/main → mvn package（本次建置） |
| `ModernWorldService.class` | NEW_IN_CANDIDATE | — | 276007 | +276007 | src/main → mvn package（本次建置） |
| `ClientSession.class` | CHANGED | 2401065 | 2601706 | +200641 | src/main → mvn package（本次建置） |
| `ModernNavigationSession.class` | NEW_IN_CANDIDATE | — | 131501 | +131501 | src/main → mvn package（本次建置） |
| `GameHub.class` | CHANGED | 372209 | 499879 | +127670 | src/main → mvn package（本次建置） |
| `ModernApplicationProtocol.class` | NEW_IN_CANDIDATE | — | 75335 | +75335 | src/main → mvn package（本次建置） |
| `ModernBattleEngine$Snapshot.class` | NEW_IN_CANDIDATE | — | 64318 | +64318 | src/main → mvn package（本次建置） |
| `LegacyMySqlRepository.class` | CHANGED | 329050 | 390384 | +61334 | src/main → mvn package（本次建置） |
| `ModernApplicationSession.class` | NEW_IN_CANDIDATE | — | 60816 | +60816 | src/main → mvn package（本次建置） |
| `ModernRooms.class` | NEW_IN_CANDIDATE | — | 60499 | +60499 | src/main → mvn package（本次建置） |
| `ModernWorldConfiguration.class` | NEW_IN_CANDIDATE | — | 55988 | +55988 | src/main → mvn package（本次建置） |
| `Protocol.class` | CHANGED | 162179 | 211110 | +48931 | src/main → mvn package（本次建置） |
| `ModernVipEquipmentAuthority.class` | NEW_IN_CANDIDATE | — | 46982 | +46982 | src/main → mvn package（本次建置） |
| `ModernMatchDescriptor.class` | NEW_IN_CANDIDATE | — | 43116 | +43116 | src/main → mvn package（本次建置） |
| `ModernBattleWire.class` | NEW_IN_CANDIDATE | — | 42668 | +42668 | src/main → mvn package（本次建置） |
| `ModernNpcRoster.class` | NEW_IN_CANDIDATE | — | 41256 | +41256 | src/main → mvn package（本次建置） |
| `ModernEquipmentAuthority.class` | NEW_IN_CANDIDATE | — | 41165 | +41165 | src/main → mvn package（本次建置） |
| `ModernServerConfig.class` | NEW_IN_CANDIDATE | — | 40554 | +40554 | src/main → mvn package（本次建置） |
| `ClassicWorldMonsterPopulation.class` | CHANGED | 113961 | 153848 | +39887 | src/main → mvn package（本次建置） |
| `ModernMain.class` | NEW_IN_CANDIDATE | — | 38430 | +38430 | src/main → mvn package（本次建置） |
| `ModernBattleEngine$PreparedMember.class` | NEW_IN_CANDIDATE | — | 38109 | +38109 | src/main → mvn package（本次建置） |
| `ModernJdbcBackend.class` | NEW_IN_CANDIDATE | — | 37161 | +37161 | src/main → mvn package（本次建置） |
| `ModernBattleObservation$Capture.class` | NEW_IN_CANDIDATE | — | 33657 | +33657 | src/main → mvn package（本次建置） |
| `ModernEquipmentSourceJdbc.class` | NEW_IN_CANDIDATE | — | 32755 | +32755 | src/main → mvn package（本次建置） |
| `ModernServer.class` | NEW_IN_CANDIDATE | — | 31071 | +31071 | src/main → mvn package（本次建置） |
| `ModernVipLoadout.class` | NEW_IN_CANDIDATE | — | 30380 | +30380 | src/main → mvn package（本次建置） |
| `ModernWorldService$Mapping.class` | NEW_IN_CANDIDATE | — | 30271 | +30271 | src/main → mvn package（本次建置） |
| `ModernPhysicalCombat.class` | NEW_IN_CANDIDATE | — | 30083 | +30083 | src/main → mvn package（本次建置） |
| `ModernMatchSettlementJdbc.class` | NEW_IN_CANDIDATE | — | 29911 | +29911 | src/main → mvn package（本次建置） |
| `ModernCombatProtocol$Frame.class` | NEW_IN_CANDIDATE | — | 29606 | +29606 | src/main → mvn package（本次建置） |
| `ModernSpeedEffects$State.class` | NEW_IN_CANDIDATE | — | 29583 | +29583 | src/main → mvn package（本次建置） |
| `ModernBattleEngine$EquipmentChange.class` | NEW_IN_CANDIDATE | — | 29535 | +29535 | src/main → mvn package（本次建置） |
| `ModernPhysicalCombatSourceJdbc.class` | NEW_IN_CANDIDATE | — | 29087 | +29087 | src/main → mvn package（本次建置） |
| `ModernEquipmentHistoryWire.class` | NEW_IN_CANDIDATE | — | 28985 | +28985 | src/main → mvn package（本次建置） |
| `ModernHasteEffects$State.class` | NEW_IN_CANDIDATE | — | 28055 | +28055 | src/main → mvn package（本次建置） |
| `ModernQuestProtocol.class` | NEW_IN_CANDIDATE | — | 27904 | +27904 | src/main → mvn package（本次建置） |
| `ModernBattleObservation$World.class` | NEW_IN_CANDIDATE | — | 27593 | +27593 | src/main → mvn package（本次建置） |
| `ModernTimeAttackProtocol.class` | NEW_IN_CANDIDATE | — | 27313 | +27313 | src/main → mvn package（本次建置） |
| `ModernVipCharacterState.class` | NEW_IN_CANDIDATE | — | 27253 | +27253 | src/main → mvn package（本次建置） |
| `ModernWorldProtocol.class` | NEW_IN_CANDIDATE | — | 27154 | +27154 | src/main → mvn package（本次建置） |
| `ModernSiegeProtocol.class` | NEW_IN_CANDIDATE | — | 26859 | +26859 | src/main → mvn package（本次建置） |
| `ModernBattleActionProtocol.class` | NEW_IN_CANDIDATE | — | 26834 | +26834 | src/main → mvn package（本次建置） |
| `ModernPetRaceOutbox.class` | NEW_IN_CANDIDATE | — | 25042 | +25042 | src/main → mvn package（本次建置） |
| `ModernTlsSessionTransport.class` | NEW_IN_CANDIDATE | — | 24888 | +24888 | src/main → mvn package（本次建置） |
| `ModernPetRaceSettlementJdbc.class` | NEW_IN_CANDIDATE | — | 24576 | +24576 | src/main → mvn package（本次建置） |
| `ModernBattleAudience.class` | NEW_IN_CANDIDATE | — | 24384 | +24384 | src/main → mvn package（本次建置） |
| `ModernShopProtocol.class` | NEW_IN_CANDIDATE | — | 24270 | +24270 | src/main → mvn package（本次建置） |
| `ModernNpcFireStormFrame.class` | NEW_IN_CANDIDATE | — | 23975 | +23975 | src/main → mvn package（本次建置） |
| `ModernSpellRuntime$State.class` | NEW_IN_CANDIDATE | — | 23897 | +23897 | src/main → mvn package（本次建置） |
| `ModernBattleAudience$World.class` | NEW_IN_CANDIDATE | — | 23271 | +23271 | src/main → mvn package（本次建置） |

## 推測

- 多數 Maven bytes 差異可能包含 javac debug 資訊、反編譯後重編、內部類別形狀差異。沒有進行方法語意等價分析，不能將這些項目自動排除。
- 第一批與正式高度重合，表示正式 JAR 已包含大量相同戰鬥 bytecode；這不表示目前 src/main 已完整吸收這些內容。T020b 仍須對照第三組來源缺口訂批次。

## 待驗證

- **仍有 1 筆未完成**：NativeArmorStatus 缺少 3f31ef32（0.6.35 固定輸入）之前可銜接的最後寫入層與完整 Java／ASM 證據。GmTravel 已解除缺件。未進入封存補找；須由任務擁有者將缺件提供於允許讀取的 sources/inputs，不能把來源邊界冒充最後寫入層。
- 未執行單元測試（依要求使用 `-DskipTests`）、候選回歸、遊戲或正式 DB。第二批 380 筆完整 Java 僅核對凍結輸出；前輪重編 GmTravel 並證明 class bytes 相同。前輪另隔離重編 InventoryCatalog，NativeArmorStatus bytes 不同，33 個正規化方法指令清單相同。
- T029 靜態保留清單需在合併後做行為驗證；非 class 資源、圖示／DLL 真機顯示仍屬後續驗收。

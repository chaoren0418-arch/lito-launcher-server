# Issue #6 — T008a evidence.zip 原始碼盤點（第 1 批）

## 結論

**blocked（來源缺失），不是替換完成。** 266 個反編譯類別已完整盤點；指定 ZIP 內沒有同名 Java、同名變體或目標型別宣告，manifest 也沒有目標類別路徑。因此本批替換 **0 / 50** 檔，`DECOMPILED` 數量 **266 → 266**，數量下降驗收未達成。

## 做了什麼／判斷依據（已驗證）

- 基準：`origin/main`，提交 `3f6c9eff1e26337a0cdd8a9fc4aad52cd8849a2d`。根目錄原工作樹仍在初始化提交 `6bb4a27b`，有既有未追蹤 `tools/`；為保留本機設定及既有檔案，使用專案內 `scratch/issue-6-worktree` 稀疏工作樹、分支 `task/6`。
- 已讀根目錄 AGENTS.md／CLAUDE.md、遠端最新版規則、TASKS.md 的 T008、docs/REPORT.md，以及伺服器規則與共同維護規範。原工作樹沒有 docs/REPORT.md，隔離工作樹沿用遠端既有報告。
- 唯讀來源：`lito伺服器/LiTo381-Server/backups/history/server-development-evidence.zip`，大小 **164,341,911 bytes**。
- ZIP SHA-256：`bedebb29a4e1c85a5fa275593700120680332ebf7b6fbe32c8034a92710fdb51`（盤點前後一致）。ZIP 不進 git，沒有修改或解壓封存。
- ZIP 共 **17,904** 個 entries，其中 **2,332** 個 `.java`。逐一讀取 Java entry（同時由 zipfile 驗證 CRC），跨所有子目錄、大小寫不敏感比對檔名，並搜尋 `ClassName.before.java` 等同名前綴變體：**0**。
- 再掃描全部 Java 內容中的 `class / interface / enum / record` 目標名稱宣告（包括非同名檔與合併來源檔）：**0**。
- `ARCHIVE-MANIFEST.json` 的 **41,342** 筆 files 記錄，以 266 個類別名稱搜尋原始路徑（含未保存與去重類別）：**0**。未找到 ZIP／7z／tar／gz 巢狀封存；未將二進位重新反編譯充作原始碼。
- 因候選集合為空，無法進入「公開方法簽名最吻合、同等吻合取日期最新」的選擇階段；沒有可報告的候選簽名相似度或版本日期，未猜選其他類別替代。
- 未修改任何 Java；沒有為了降低計數而移除標頭。全部目標來源檔在隔離工作樹的連結數為 1，未修改硬連結。

## 取代清單

| 檔名 | 來源 ZIP 內路徑 | 差異行數 |
|---|---|---|
| 無（0 檔） | 無符合來源 | +0 / -0 |

## 怎麼驗證

- 已驗證：盤點前後 `^// DECOMPILED` 均為 266；下面列出完整清單。檔案所屬 package 均為 `local.lito381.server`。
- **本機 Maven 通過**：Java 17.0.17、Maven 3.9.16，`mvn -DskipTests compile`（另加 `-B -s <scratch/settings.xml> -Dmaven.repo.local=<scratch/m2> -f <隔離工作樹/pom.xml>`，將依賴及暫存限制於專案內）。編譯 940 個 source files，`BUILD SUCCESS`，exit 0，42.741 s，完成時間 `2026-11-04T13:49:57+08:00`。log：`scratch/issue-6-audit/compile.log`。這是未替換來源的基準編譯，不是替換驗收。
- CI：待 PR 建立後查看；即使 CI 綠，也不能視為本任務來源替換驗收通過。
- 僅編譯，不執行測試／應用程式，不啟動遊戲、伺服器或正式 DB；未讀寫機密設定、玩家實表或發布產物。

## 未驗證什麼／推測

- **待驗證**：取得正確原始碼封存後的簽名匹配、日期選擇、最多 50 檔實際替換、替換後編譯與 CI、標頭數量下降、實機行為。
- **推測**：目前指定封存可能早於這些類別的生成或保存範圍；這不是由完整版本沿革證明的結論，不能宣稱原始碼已遺失。

## 產出路徑／給下一棒的提示

- 分支內正式報告：`docs/reports/issue-6.md`；總報告：`docs/REPORT.md`。
- 本機正式報告位於 `scratch/issue-6-worktree/docs/reports/issue-6.md`；一次性盤點腳本與編譯 log 位於 `scratch/issue-6-audit/`（不提交）。原工作樹保持原狀。
- 下一棒由**調度器／Claude**確認或補充包含下列 266 類的原始碼來源封存（精確路徑、SHA-256、版本）；本機執行器取得正確來源後再做第 1 批 ≤50 檔。不要重新指派同一 ZIP 而不補來源。
- 此 PR 只交付可審查的盤點與阻塞證據；Issue #6 應保持未完成，不因文件合併自動關閉。

## 未找到清單／完整 266 類盤點

下列每列均表示：同名檔、變體、內容型別宣告及 manifest 皆未找到。路徑統一前綴為 `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`；類別完整名稱為 `local.lito381.server.` 加檔名去掉 `.java`。

| # | 檔名（類別） | 結果 |
|---|---|---|
| 1 | `NativeAcceptedHpCompletion.java` | 未找到 |
| 2 | `NativeAcceptedHpContext.java` | 未找到 |
| 3 | `NativeAcceptedState.java` | 未找到 |
| 4 | `NativeArmorProc.java` | 未找到 |
| 5 | `NativeCastMaterialPolicy.java` | 未找到 |
| 6 | `NativeCastTimeline.java` | 未找到 |
| 7 | `NativeCombatAttacker.java` | 未找到 |
| 8 | `NativeCombatOrigin.java` | 未找到 |
| 9 | `NativeCompanionCallbacks.java` | 未找到 |
| 10 | `NativeCompanionCommands.java` | 未找到 |
| 11 | `NativeCompanionCommit.java` | 未找到 |
| 12 | `NativeCompanionMutation.java` | 未找到 |
| 13 | `NativeCubeCastAction.java` | 未找到 |
| 14 | `NativeDeActorState.java` | 未找到 |
| 15 | `NativeDeAiState.java` | 未找到 |
| 16 | `NativeDeAiTimeline.java` | 未找到 |
| 17 | `NativeDeAiTimerProtocol.java` | 未找到 |
| 18 | `NativeDeAiTimers.java` | 未找到 |
| 19 | `NativeDeAiWorkerProtocol.java` | 未找到 |
| 20 | `NativeDeAiWorkers.java` | 未找到 |
| 21 | `NativeDeArmorTemplate.java` | 未找到 |
| 22 | `NativeDeathLifecycleGate.java` | 未找到 |
| 23 | `NativeDeCachedFactoryServices.java` | 未找到 |
| 24 | `NativeDeCharacterActor.java` | 未找到 |
| 25 | `NativeDeCharacterControls.java` | 未找到 |
| 26 | `NativeDeCharacterFields.java` | 未找到 |
| 27 | `NativeDeChatAccess.java` | 未找到 |
| 28 | `NativeDeChatBroadcast.java` | 未找到 |
| 29 | `NativeDeChatChat.java` | 未找到 |
| 30 | `NativeDeChatChatGlobal.java` | 未找到 |
| 31 | `NativeDeChatChatShouting.java` | 未找到 |
| 32 | `NativeDeChatData.java` | 未找到 |
| 33 | `NativeDeChatJobs.java` | 未找到 |
| 34 | `NativeDeChatResources.java` | 未找到 |
| 35 | `NativeDeChatRuntime.java` | 未找到 |
| 36 | `NativeDeChatScope.java` | 未找到 |
| 37 | `NativeDeChatTable.java` | 未找到 |
| 38 | `NativeDeChatTimer.java` | 未找到 |
| 39 | `NativeDeChatWire.java` | 未找到 |
| 40 | `NativeDeChatWorld.java` | 未找到 |
| 41 | `NativeDeClientViewer.java` | 未找到 |
| 42 | `NativeDeConstructionPorts.java` | 未找到 |
| 43 | `NativeDeControlCatalog.java` | 未找到 |
| 44 | `NativeDeCorpseClock.java` | 未找到 |
| 45 | `NativeDeCorpseCore.java` | 未找到 |
| 46 | `NativeDeCorpseState.java` | 未找到 |
| 47 | `NativeDeCorpseTimers.java` | 未找到 |
| 48 | `NativeDeDarkItem.java` | 未找到 |
| 49 | `NativeDeDeletionDeState.java` | 未找到 |
| 50 | `NativeDeDeletionOwner.java` | 未找到 |
| 51 | `NativeDeDeletionPorts.java` | 未找到 |
| 52 | `NativeDeDeletionScope.java` | 未找到 |
| 53 | `NativeDeDeletionState.java` | 未找到 |
| 54 | `NativeDeDeletionVisibility.java` | 未找到 |
| 55 | `NativeDeEquipment.java` | 未找到 |
| 56 | `NativeDeEtcItemTemplate.java` | 未找到 |
| 57 | `NativeDeFactoryControls.java` | 未找到 |
| 58 | `NativeDeFrozenArmorTemplate.java` | 未找到 |
| 59 | `NativeDeFrozenEtcTemplate.java` | 未找到 |
| 60 | `NativeDeFrozenWeaponTemplate.java` | 未找到 |
| 61 | `NativeDeInitializerKernel.java` | 未找到 |
| 62 | `NativeDeInstanceBroadcast.java` | 未找到 |
| 63 | `NativeDeInstanceInitialization.java` | 未找到 |
| 64 | `NativeDeInstanceName.java` | 未找到 |
| 65 | `NativeDeInstanceNames.java` | 未找到 |
| 66 | `NativeDeInstancePorts.java` | 未找到 |
| 67 | `NativeDeInstanceScene.java` | 未找到 |
| 68 | `NativeDeInstanceScope.java` | 未找到 |
| 69 | `NativeDeInstanceState.java` | 未找到 |
| 70 | `NativeDeInventoryIdentity.java` | 未找到 |
| 71 | `NativeDeItemAbility.java` | 未找到 |
| 72 | `NativeDeItemApplication.java` | 未找到 |
| 73 | `NativeDeItemAttrWeapon.java` | 未找到 |
| 74 | `NativeDeItemBlessStatsTable.java` | 未找到 |
| 75 | `NativeDeItemCommonEnchantTable.java` | 未找到 |
| 76 | `NativeDeItemDefinitionsResources.java` | 未找到 |
| 77 | `NativeDeItemDefinitionsScope.java` | 未找到 |
| 78 | `NativeDeItemDefinitionsSql.java` | 未找到 |
| 79 | `NativeDeItemEnchantBonusTable.java` | 未找到 |
| 80 | `NativeDeItemEquipProc.java` | 未找到 |
| 81 | `NativeDeItemEquipProcTable.java` | 未找到 |
| 82 | `NativeDeItemFrozenAbility.java` | 未找到 |
| 83 | `NativeDeItemGreaterTierTable.java` | 未找到 |
| 84 | `NativeDeItemIdStatsTable.java` | 未找到 |
| 85 | `NativeDeItemInlayCard.java` | 未找到 |
| 86 | `NativeDeItemInlayCardTable.java` | 未找到 |
| 87 | `NativeDeItemInventory.java` | 未找到 |
| 88 | `NativeDeItemMagicStone.java` | 未找到 |
| 89 | `NativeDeItemMagicStoneTable.java` | 未找到 |
| 90 | `NativeDeItemMetadata.java` | 未找到 |
| 91 | `NativeDeItemPublication.java` | 未找到 |
| 92 | `NativeDeItemStateCopy.java` | 未找到 |
| 93 | `NativeDeItemTable.java` | 未找到 |
| 94 | `NativeDeItemTemplate.java` | 未找到 |
| 95 | `NativeDeItemTemplatesResources.java` | 未找到 |
| 96 | `NativeDeItemTemplatesScope.java` | 未找到 |
| 97 | `NativeDeItemTransaction.java` | 未找到 |
| 98 | `NativeDeItemVipTemplate.java` | 未找到 |
| 99 | `NativeDeItemWeaponAttrTable.java` | 未找到 |
| 100 | `NativeDeItemWeaponProc.java` | 未找到 |
| 101 | `NativeDeItemWeaponProcTable.java` | 未找到 |
| 102 | `NativeDeKnownState.java` | 未找到 |
| 103 | `NativeDeLiveAiJobs.java` | 未找到 |
| 104 | `NativeDeLiveAiPorts.java` | 未找到 |
| 105 | `NativeDeLiveAiScope.java` | 未找到 |
| 106 | `NativeDeNpcConstructionGeometry.java` | 未找到 |
| 107 | `NativeDeNpcConstructionPorts.java` | 未找到 |
| 108 | `NativeDeNpcConstructionState.java` | 未找到 |
| 109 | `NativeDeNpcExecutor.java` | 未找到 |
| 110 | `NativeDeNpcHateList.java` | 未找到 |
| 111 | `NativeDeNpcInventory.java` | 未找到 |
| 112 | `NativeDeNpcInventoryScope.java` | 未找到 |
| 113 | `NativeDeNpcKnowledge.java` | 未找到 |
| 114 | `NativeDeNpcMobSkill.java` | 未找到 |
| 115 | `NativeDeNpcMobSkillUse.java` | 未找到 |
| 116 | `NativeDeNpcMoveExecutor.java` | 未找到 |
| 117 | `NativeDeNpcMoveState.java` | 未找到 |
| 118 | `NativeDeNpcTemplate.java` | 未找到 |
| 119 | `NativeDeNpcTemplateLoadScope.java` | 未找到 |
| 120 | `NativeDeNpcTemplateLog.java` | 未找到 |
| 121 | `NativeDeNpcTemplateMobLoader.java` | 未找到 |
| 122 | `NativeDeNpcTemplateNpcLoader.java` | 未找到 |
| 123 | `NativeDeNpcTemplatePorts.java` | 未找到 |
| 124 | `NativeDeNpcTemplateResources.java` | 未找到 |
| 125 | `NativeDeNpcTemplateScope.java` | 未找到 |
| 126 | `NativeDeNpcTemplateState.java` | 未找到 |
| 127 | `NativeDeNpcVitalState.java` | 未找到 |
| 128 | `NativeDeObjectBindings.java` | 未找到 |
| 129 | `NativeDeObjectFields.java` | 未找到 |
| 130 | `NativeDeObjectScope.java` | 未找到 |
| 131 | `NativeDePerceptionAccess.java` | 未找到 |
| 132 | `NativeDePerceptionBadText.java` | 未找到 |
| 133 | `NativeDePerceptionCharVisualUpdate.java` | 未找到 |
| 134 | `NativeDePerceptionControls.java` | 未找到 |
| 135 | `NativeDePerceptionDoActionShop.java` | 未找到 |
| 136 | `NativeDePerceptionEmblem.java` | 未找到 |
| 137 | `NativeDePerceptionEmblemData.java` | 未找到 |
| 138 | `NativeDePerceptionFishing.java` | 未找到 |
| 139 | `NativeDePerceptionJobs.java` | 未找到 |
| 140 | `NativeDePerceptionLiquor.java` | 未找到 |
| 141 | `NativeDePerceptionNPCPack_De.java` | 未找到 |
| 142 | `NativeDePerceptionOpcodes.java` | 未找到 |
| 143 | `NativeDePerceptionOutput.java` | 未找到 |
| 144 | `NativeDePerceptionPacket.java` | 未找到 |
| 145 | `NativeDePerceptionPorts.java` | 未找到 |
| 146 | `NativeDePerceptionResource.java` | 未找到 |
| 147 | `NativeDePerceptionScope.java` | 未找到 |
| 148 | `NativeDePerceptionSkillBrave.java` | 未找到 |
| 149 | `NativeDePerceptionSkillHaste.java` | 未找到 |
| 150 | `NativeDePerceptionSpeed.java` | 未找到 |
| 151 | `NativeDePerceptionState.java` | 未找到 |
| 152 | `NativeDeProfileKernel.java` | 未找到 |
| 153 | `NativeDeResurrectableState.java` | 未找到 |
| 154 | `NativeDeResurrectionEffects.java` | 未找到 |
| 155 | `NativeDeShopData.java` | 未找到 |
| 156 | `NativeDeShopResources.java` | 未找到 |
| 157 | `NativeDeShopTable.java` | 未找到 |
| 158 | `NativeDeShopWorld.java` | 未找到 |
| 159 | `NativeDeSpriteCatalog.java` | 未找到 |
| 160 | `NativeDeSprites.java` | 未找到 |
| 161 | `NativeDeTerrainData.java` | 未找到 |
| 162 | `NativeDeTerrainEnvironment.java` | 未找到 |
| 163 | `NativeDeTerrainFileReader.java` | 未找到 |
| 164 | `NativeDeTerrainPoint.java` | 未找到 |
| 165 | `NativeDeTerrainResources.java` | 未找到 |
| 166 | `NativeDeTerrainScope.java` | 未找到 |
| 167 | `NativeDeTerrainTable.java` | 未找到 |
| 168 | `NativeDeTerrainText.java` | 未找到 |
| 169 | `NativeDeTerrainTimer.java` | 未找到 |
| 170 | `NativeDeTerrainV1.java` | 未找到 |
| 171 | `NativeDeTerrainWorld.java` | 未找到 |
| 172 | `NativeDeVisibilityAccess.java` | 未找到 |
| 173 | `NativeDeVisibilityCheck.java` | 未找到 |
| 174 | `NativeDeVisibilityClock.java` | 未找到 |
| 175 | `NativeDeVisibilityCoordinator.java` | 未找到 |
| 176 | `NativeDeVisibilityHp.java` | 未找到 |
| 177 | `NativeDeVisibilityJobs.java` | 未找到 |
| 178 | `NativeDeVisibilityOutput.java` | 未找到 |
| 179 | `NativeDeVisibilityPacket.java` | 未找到 |
| 180 | `NativeDeVisibilityPc.java` | 未找到 |
| 181 | `NativeDeVisibilityPorts.java` | 未找到 |
| 182 | `NativeDeVisibilityRuntime.java` | 未找到 |
| 183 | `NativeDeVisibilityScope.java` | 未找到 |
| 184 | `NativeDeVisibilityTimer.java` | 未找到 |
| 185 | `NativeDeVisibilityWorld.java` | 未找到 |
| 186 | `NativeDeVitalState.java` | 未找到 |
| 187 | `NativeDeWeaponKernel.java` | 未找到 |
| 188 | `NativeDeWeaponTemplate.java` | 未找到 |
| 189 | `NativeDeWorldAccess.java` | 未找到 |
| 190 | `NativeDeWorldConnection.java` | 未找到 |
| 191 | `NativeDeWorldContext.java` | 未找到 |
| 192 | `NativeDeWorldControls.java` | 未找到 |
| 193 | `NativeDeWorldCore.java` | 未找到 |
| 194 | `NativeDeWorldKinds.java` | 未找到 |
| 195 | `NativeDeWorldLists.java` | 未找到 |
| 196 | `NativeDeWorldLocation.java` | 未找到 |
| 197 | `NativeDeWorldLog.java` | 未找到 |
| 198 | `NativeDeWorldMap.java` | 未找到 |
| 199 | `NativeDeWorldMaps.java` | 未找到 |
| 200 | `NativeDeWorldPlayerFilter.java` | 未找到 |
| 201 | `NativeDeWorldPoint.java` | 未找到 |
| 202 | `NativeDeWorldQuestUser.java` | 未找到 |
| 203 | `NativeDeWorldRegistryBow.java` | 未找到 |
| 204 | `NativeDeWorldRegistryCrown.java` | 未找到 |
| 205 | `NativeDeWorldRegistryDarkelf.java` | 未找到 |
| 206 | `NativeDeWorldRegistryDe.java` | 未找到 |
| 207 | `NativeDeWorldRegistryDoll.java` | 未找到 |
| 208 | `NativeDeWorldRegistryDragonKnight.java` | 未找到 |
| 209 | `NativeDeWorldRegistryEffect.java` | 未找到 |
| 210 | `NativeDeWorldRegistryElf.java` | 未找到 |
| 211 | `NativeDeWorldRegistryIllusionist.java` | 未找到 |
| 212 | `NativeDeWorldRegistryItem.java` | 未找到 |
| 213 | `NativeDeWorldRegistryKnight.java` | 未找到 |
| 214 | `NativeDeWorldRegistryMob.java` | 未找到 |
| 215 | `NativeDeWorldRegistryNpc.java` | 未找到 |
| 216 | `NativeDeWorldRegistryPet.java` | 未找到 |
| 217 | `NativeDeWorldRegistryQuest.java` | 未找到 |
| 218 | `NativeDeWorldRegistrySummons.java` | 未找到 |
| 219 | `NativeDeWorldRegistryTrap.java` | 未找到 |
| 220 | `NativeDeWorldRegistryWizard.java` | 未找到 |
| 221 | `NativeDeWorldScope.java` | 未找到 |
| 222 | `NativeEffectCompletions.java` | 未找到 |
| 223 | `NativeEquipmentDefense.java` | 未找到 |
| 224 | `NativeItemWeaponCastAction.java` | 未找到 |
| 225 | `NativeMaterialItem.java` | 未找到 |
| 226 | `NativeNpcLifetime.java` | 未找到 |
| 227 | `NativePcActionAuthority.java` | 未找到 |
| 228 | `NativePlayerActionScope.java` | 未找到 |
| 229 | `NativePlayerCommit.java` | 未找到 |
| 230 | `NativePlayerLifetime.java` | 未找到 |
| 231 | `NativePlayerVitalCompletions.java` | 未找到 |
| 232 | `NativePlayerVitals.java` | 未找到 |
| 233 | `NativeProcEncounterCatalog.java` | 未找到 |
| 234 | `NativeProcEncounterPublication.java` | 未找到 |
| 235 | `NativeProcFollowerReceiver.java` | 未找到 |
| 236 | `NativeProcHealBonus.java` | 未找到 |
| 237 | `NativeProcHealingCompanion.java` | 未找到 |
| 238 | `NativeProcHealingEncounter.java` | 未找到 |
| 239 | `NativeProcHealingKernel.java` | 未找到 |
| 240 | `NativeProcHealingPlayer.java` | 未找到 |
| 241 | `NativeProcHealingWorld.java` | 未找到 |
| 242 | `NativeProcNpcCatalog.java` | 未找到 |
| 243 | `NativeProcNpcState.java` | 未找到 |
| 244 | `NativeProcPlayerState.java` | 未找到 |
| 245 | `NativeProcSkillCatalog.java` | 未找到 |
| 246 | `NativeReuseActor.java` | 未找到 |
| 247 | `NativeReuseGate.java` | 未找到 |
| 248 | `NativeReuseLog.java` | 未找到 |
| 249 | `NativeReuseShutdown.java` | 未找到 |
| 250 | `NativeRolePublication.java` | 未找到 |
| 251 | `NativeSelfCastAction.java` | 未找到 |
| 252 | `NativeServerLifetime.java` | 未找到 |
| 253 | `NativeSkillConsumption.java` | 未找到 |
| 254 | `NativeSkillConsumptionBridges.java` | 未找到 |
| 255 | `NativeSourceActionIntervalCalculator.java` | 未找到 |
| 256 | `NativeSourceClientActionTimeline.java` | 未找到 |
| 257 | `NativeSourceMaterialPlan.java` | 未找到 |
| 258 | `NativeSourcePacketExecutionContext.java` | 未找到 |
| 259 | `NativeSourcePlayerActionLock.java` | 未找到 |
| 260 | `NativeSourceSkillCastAdmission.java` | 未找到 |
| 261 | `NativeSourceSkillConsumptionMath.java` | 未找到 |
| 262 | `NativeSourceSkillDelay.java` | 未找到 |
| 263 | `NativeSourceSpriteActionSpeed.java` | 未找到 |
| 264 | `NativeZeroMpConsumption.java` | 未找到 |
| 265 | `SharedClientResources.java` | 未找到 |
| 266 | `SharedResourceMain.java` | 未找到 |

## 可重現的唯讀盤點

在 task/6 工作樹根目錄執行下列 Python 3 程式（Windows 主控台使用 UTF-8）。ZIP 指向原工作樹指定封存；不解壓、不寫來源、不讀設定。

```python
from pathlib import Path
import hashlib, json, re, zipfile

src = Path("lito伺服器/LiTo381-Server/src/main/java")
archive = Path("D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/backups/history/server-development-evidence.zip")
targets = sorted(p for p in src.rglob("*.java")
                 if re.search(rb"^// DECOMPILED", p.read_bytes(), re.M))
stems = {p.stem for p in targets}
names = {p.name.casefold() for p in targets}
declarations = re.compile(rb"\b(?:class|interface|enum|record)\s+("
    + b"|".join(re.escape(s.encode()) for s in sorted(stems)) + rb")\b")
with zipfile.ZipFile(archive) as z:
    java = [i for i in z.infolist() if i.filename.lower().endswith(".java")]
    same = [i.filename for i in java if Path(i.filename).name.casefold() in names]
    variants = [i.filename for i in java if any(
        Path(i.filename).name.casefold().startswith(s.casefold() + ".") for s in stems)]
    declared = [(i.filename, declarations.findall(z.read(i))) for i in java]
    declared = [(p, matches) for p, matches in declared if matches]
    manifest = json.loads(z.read("ARCHIVE-MANIFEST.json"))["files"]
    indexed = [x["path"] for x in manifest if any(
        s.casefold() in x["path"].casefold() for s in stems)]
print("targets/java/manifest:", len(targets), len(java), len(manifest))
print("same/variants/declarations/manifest matches:",
      len(same), len(variants), len(declared), len(indexed))
with archive.open("rb") as stream:
    print("zip sha256:", hashlib.file_digest(stream, "sha256").hexdigest())
```

本輪輸出：`266 / 2332 / 41342`；四種匹配數均為 `0`。

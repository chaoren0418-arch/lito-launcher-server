# Issue #386 — T135 受控 NPC（寵物、召喚物）恢復節奏與自身技能：G15 前兩組移植

分支 `task/386`，PR #397。設計與逐項對照：`docs/design/controlled-npc.md`「G15 前兩組移植設計（T135／Issue #386）」。

## 做了什麼

1. **設計段**（`docs/design/controlled-npc.md` 文末新增一節）：5 個類別逐項列「候選做法／`1-伺服器` 做法／一致與否／`src/main` 行號」。
   - 第 1 組 16 項（含 6a 間隔單位）：`1-伺服器` 的實際回復類別是 `timecontroller/pet/` 的 `PetHprTimer`、`PetMprTimer`、`SummonHprTimer`、`SummonMprTimer`（由 `StartTimer_Pet.java:83-92` 啟動，呼叫 `HprPet.hpUpdate`／`MprPet.mpUpdate`）；`L1PetInstance`／`L1SummonInstance` 本身只有送主人 HP 條（`S_HPMeter`）。
   - 第 2 組 22 項：對 `model/L1MobSkillUse.java`、`L1SkillDelayforMob.java`、`NpcAI.java`。
   - 結論：**數值與判定沒有找到候選與 `1-伺服器` 不一致的地方**；不一致只在「候選沒做的部分」，列在設計段「還缺什麼」。
2. **移植到 `src/main`**（Java 原始碼，不走 patch 鏈），新增 5 個類別：
   `NativeCloseoutControlledNpcData`、`NativeCloseoutCompanionRecoveryData`、`NativeCloseoutCompanionRecovery`、`NativeCloseoutCompanionSkills`、`NativeCloseoutMobSkillSelection`。
3. **既有類別的插點**（只取這兩組需要的；行號與候選對照見設計段「插點」表）：
   | 類別 | 改動 |
   |---|---|
   | `NativeSourceNpcFactoryType` | `Stamp` 加兩個欄位與一個受控 NPC 用建構子；`controlled()` 的寵物／召喚物兩個分支呼叫 `NativeCloseoutControlledNpcData.capture` |
   | `GameHub` | 回復時鐘欄位；建構子在 `NativePlayerLifetime.openHub` 之後 `init`；`close()` 時 `close`。掛在既有 `battleClock`，不新增執行緒 |
   | `BattleCompanion` | `profile` 加 `volatile`；mobskill 狀態欄位；建構時 `install` 1 處；`deactivate`／`applyDamage` 兩處／`apply("dismiss")` 共 4 處 `terminal` |
   | `NativeCompanionPlacement.State` | `pending` 加 `volatile` |
   | `NativeSourceNpcMagicEffects.allowFrame` | 回傳前再過 `NativeCloseoutCompanionRecovery.allowFrame` |
   | `ClientSession` | `serverCompanionTick`、`serverClassicWorldCompanionTick` 開頭呼叫技能狀態 `tick`（2 行） |
4. **測試**（`src/test/java/local/lito381/server/`，重用 `NativeSourceAreaPairFirstTest.Scene`）：
   - `NativeCloseoutCompanionRecoveryTest`：回復間隔與回復量（含 ≤0 預設、毫秒÷1000）、上限、主人 HP 條封包位元組與「只送主人」、死亡／收回／移出後不回復、等鎖期間退役 12 種、HP 條算好後退役 8 種、時鐘計數與停止。
   - `NativeCloseoutMobSkillSelectionTest`（14 個）：TriRnd 100 種骰值、召喚／變身必擲、HP／距離／次數條件、冷卻跳過與成功才記帳、均勻抽列、`TYPE_NONE` 終止、七種類型分派、換目標。
   - `NativeCloseoutCompanionSkillsTest`（9 個）：每隻一份狀態、冷卻起算／到期／不延長／≤0 不起算、休息無目標時次數歸零冷卻保留、死亡／收回後租約失效（不再施法記帳）。
5. 文件狀態：`controlled-npc.md` G15 表加「`src/main` 狀態」欄；`TASKS.md` T135 → review；`docs/reports/customer-gaps.md` G15 列更新；`docs/acceptance/M4-RUNBOOK.md` 新增 **V40**，並補進 T129 的 C 部：C2 輪內容（排在 V17 前）與 E9 的用途欄。
6. 同步 `origin/main`（#384 合併），`ClientSession.java` 自動合併無衝突。

## 怎麼驗證

### 已驗證
- 本機 `mvn -DskipTests compile`／`test-compile` 通過（依本機規定不在本機跑測試）。
- PR #397 前一個 commit（`a2bd4bf0`）的 CI：`Server (Java 17 / Maven)`、`Server tests`（`mvn -B test`）、`Server reproducible JAR`、`Launcher`、`Build-Package.ps1 zip`、`Semantic gap` 全部 pass；`NativeCloseoutMobSkillSelectionTest` 14／0／0、`NativeCloseoutCompanionSkillsTest` 9／0／0（CI 日誌）。
- `Server tests (CI MySQL fixture, non-blocking…)` 失敗在 `ClientSessionCubeShockIntegrationTest`（`combat accessor ClientSession.selectedSourceAccounts failed`），與本 PR 無關：main 的 `5cf72d78` 這個 job 也是 failure，屬既有問題。
- 合併 main 之後的 commit 交由本次 push 的 CI 重跑。

### 推測
- `gh` 取得的 CI 日誌被截斷，`NativeCloseoutCompanionRecoveryTest` 的單項筆數沒有在日誌裡看到；`Server tests` job 結論是 pass，所以推定這個測試類別也是 0 Failures／0 Errors。要確認可下載 surefire 報告。
- LiTo 實驗靜態庫的 `npc` 表 `hprinterval`／`hpr`／`mprinterval`／`mpr` 是否與 `1-伺服器` `db/380_db.sql` 同值沒有查（不連 DB）；RUNBOOK V40 開頭有查詢 SQL。

## 未驗證什麼（待驗證，已寫進 RUNBOOK V40，T129 C 部 C2 輪）
- 實機：寵物／召喚物被打到半血後，主人畫面 HP 條依樣板間隔上升、回滿停止；對手畫面沒有多餘封包；寵物死亡／召喚物解散後不再更新。
- MP 回復畫面上看不到，只在有寵物狀態視窗時順手記。
- 需要 E9（能在競技房帶出的受控 NPC），E9 目前仍「缺」。

## 剩餘（不在本 PR，交主控另開）
1. **受控 NPC 實際施放 mobskill 的入口**（`1-伺服器` `L1NpcInstance.attack()` `:889-926`）：候選沒有呼叫 `NativeCloseoutMobSkillSelection.trigger`／`use` 的地方，所以**移植後召喚物仍不會放自己的技能**。第 2 組只到選列邏輯與使用狀態。
2. 七種效果轉接（`Ports.physical`／`magic`／`summon`／`poly`／`shockStun`／`cancellation`／`weaponBreak`，`L1MobSkillUse.java:304-675`）與施放後硬直時間（`_sleepTime`）。
3. 進化後樣板更新：`BattleCompanion.nativeSourceNpcFactoryType` 是 `final`，進化不換戳記，**進化後到重新召喚前不回血回魔**；屬 #354 優先 3（`NativeCloseoutCompanionEvolution`）。
4. 其餘 AI 結束分支（設計段第 2 組第 21 項）。
5. G15 其餘 24 個類別照任務規定留 Backlog。

## 產出路徑
- 原始碼：`lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/NativeCloseout{ControlledNpcData,CompanionRecoveryData,CompanionRecovery,CompanionSkills,MobSkillSelection}.java`
- 測試：`lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/NativeCloseout{CompanionRecovery,CompanionSkills,MobSkillSelection}Test.java`
- 文件：`docs/design/controlled-npc.md`、`docs/acceptance/M4-RUNBOOK.md`（V40、C2、E9）、`TASKS.md`、`docs/reports/customer-gaps.md`、本檔

## 給下一棒的提示
- 「召喚物放技能」要另開一張：從 `ClientSession.serverCompanionTick` 的攻擊段接 `NativeCloseoutMobSkillSelection.trigger`，照 `L1NpcInstance.attack()` 的一半機率分流，再實作 `Ports` 的七種效果；先取 `NativeCloseoutCompanionSkills.Lease` 再記帳。
- 進化回復缺口和 `NativeCloseoutCompanionEvolution` 一起做：要讓 `evolve` 換工廠戳記並重新 `install`（候選 `M/CloseoutCompanionSkillsMethods.java:16-18`）。
- 日後移植 `NativeCloseoutNpcRecovery`（自然 NPC 回復）時，可直接共用 `NativeCloseoutCompanionRecoveryData.Data` 與 `NativeCloseoutCompanionRecovery.bounded`。
- MySQL fixture job 的 `ClientSessionCubeShockIntegrationTest` 在 main 上就失敗，若要修請另開。

# Issue #325 報告：T115 法師技能施放時的正義值變動（10、20、27、28、40、41、66、77，加 103）

- 分支：`task/325`（code，第 1 次嘗試）
- 範圍：Issue 本文的 8 個技能，加上主控補充的 **103 暗黑盲咒**（#309），共 9 個。
- 規格：`docs/design/skills-wizard.md` §1 F8、§3、§4.1；`docs/design/skills-others.md` O7、103 列、§4.1（已同步改成「已修正（#325）」）。

## 做了什麼

1. **catalog 加正義值欄位**（`BattleSpellCatalog.java`）：5 種 record 加 `int lawfulDelta`（放在 `key` 前），數值照 `1-伺服器 db/380_db.sql` `skills.lawful`：

   | record | 技能 | lawfulDelta |
   |---|---|---|
   | `AttackSpell` | 10 寒冷戰慄／28 吸血鬼之吻／77 究極光裂術 | −2／−5／**+1** |
   | `BlindSpell` | 20 闇盲咒術／40 黑闇之影／103 暗黑盲咒 | −10／−10／−10 |
   | `WeaponBreakSpell` | 27 壞物術 | −10（建構子固定檢查） |
   | `CreateZombieSpell` | 41 造屍術 | −10 |
   | `SleepSpell` | 66 沉睡之霧 | −50 |

   - `AttackSpell` 原本的 13 參數建構子維持 lawful 0（其他攻擊魔法、包裝技能 192／202／203／207／208／213、108 都是 0，不會多送封包）；新增 14 參數（含 lawful）建構子給 10／28；77 用的 16→17 參數建構子多一個 lawful。108 會心一擊檢查 lawful 必須為 0。
   - 欄位檢查：lawful 必須在 signed word 範圍內（77 是正值，所以不能沿用其他 record「≤0」的檢查）。
2. **11 個處理器呼叫 `applySpellLawfulDelta`**，位置都在扣 MP 之後、送 `S_MANA_POINT` 之前（與 47 弱化術 `ClientSession.java:69363` 相同）。S1 `U:1196-1201` 在 `runSkill` 之後無條件 `useConsume`，所以命中與否都套用：
   - 攻擊魔法：戰鬥 `nativeSourceControlledOriginal_useAttackSpell`（`ClientSession.java:89448`）、世界 `nativeCompanionTargetuseClassicWorldAttackSpell`（`:81581`）——放在包裝技能既有的 `var6／var10` 正義值呼叫之後，包裝技能的內層 AttackSpell 都是 0，不會重複。
   - 盲咒：`nativeMaterialBlindBody`（`:71257`）、`nativeMaterialWorldBlindBody`（`:71554`）、寵物目標 `NativeBlindSceneAction.companion`。
   - 壞物術：`nativeMaterialWeaponBreakBody`（`:65024`）、`nativeMaterialWorldWeaponBreakBody`（`:65377`）、戰鬥寵物目標 `NativeWeaponBreakBattleCompanion.cast`。
   - 沉睡之霧：`nativeAreaBattleSleepBody`（`:60558`）、`nativeAreaWorldSleepBody`（`:59945`）。
   - 造屍術：`nativeMaterialZombieBody`（`:64732`，戰鬥與世界共用）。
   - `applySpellLawfulDelta` 由 `private` 改為套件可見，讓上面兩個獨立類別能直接呼叫（不用反射）。方法內容沒改：只動 `GameStateStore`（LiTo 自己的狀態檔）與 `playerLawful`，**不寫回來源角色庫**；`delta=0` 或數值沒變就不送封包；有變就送 `S_CHANGE_ALIGNMENT`（`Protocol.changeAlignment`，0x59 `cdh`，`docs/reference/client-protocol-needs.md` §1.2，已處理 P:8315），WORLD 送給同畫面的人、BATTLE 送給房內成員。
3. **測試**：
   - `WizardSkillServer1ParityTest.LAWFUL_NOT_APPLIED` 由 {10,20,27,28,40,41,66,77} 改為空集合；`OtherClassSkillServer1ParityTest.LAWFUL_NOT_APPLIED` 由 {103} 改為空集合（`lawfulGapListIsExact` 期望「全部已套用」）。原有的 `damageProbabilityAttributeAndLawfulMatchSource`／`damageAttributeTypeAndLawfulMatchSource` 現在會逐列比對新欄位與 SQL。
   - 新增 `issue325LawfulDeltasMatchSource`：9 個技能的 lawfulDelta 逐一等於 SQL；其他攻擊魔法（4、6、7、15、16、34、38、45、46、108）維持 0。
   - 新增 `issue325EveryCastHandlerAppliesLawful`：讀 `ClientSession.java` 等原始碼，確認 11 個處理器都在扣 MP 之後、`S_MANA_POINT` 之前呼叫。
   - `NativeWorldChillTouchSourceAuditTest` 的 catalog 字串改成含 `-2`（這個測試需要本機 `1-伺服器`，CI 會略過）。

## 怎麼驗證

### 已驗證
- 本機 `mvn -B -q -o -DskipTests test-compile`（只編譯 `src/main` 與 `src/test`，**沒有執行任何測試**，依本機規定）：exit 0，無錯誤輸出。
- 靜態確認扣 MP 的入口完整：`*_effectiveMpCost_III` 對這 5 種 record 的全部呼叫點只有上面 11 個處理器（ClientSession 9 個＋`NativeBlindSceneAction`、`NativeWeaponBreakBattleCompanion`）；其他 `AttackSpell_effectiveMpCost_III` 呼叫點都是 Freeze／Area／Line／Bounce 等其他 record 的處理器。
- 測試 fixture 中 SQL lawful≠0 的技能：法師 10、11、20、27、28、33、40、41、47、51、56、59、66、77、80；其他職業只有 103；妖精 0 個——新欄位不會讓妖精／其他職業的平行比對失敗。
- CI 結果：見 PR（push 後補在 PR 留言）。

### 推測
- 既有封包整合測試（`ClientSessionNativeTournamentEconomyTest` 寒冷戰慄／究極光裂術、`ClientSessionClassicCastleTowerMagicTest`、`ClientSessionClassicWorldMonsterAggressionIntegrationTest` 壞物術）都用「找到某 opcode」或「包含某封包」的方式判斷，多一個 `S_CHANGE_ALIGNMENT` 應不影響；以 CI 為準。
- 究極光裂術在測試裡 `playerLawful=500`，但測試角色的來源正義值是 0，施放後 `playerLawful` 會依 `GameStateStore` 基準變成 1；測試施放後沒有再檢查正義值，應不影響。正式環境基準是來源角色的正義值。
- `ModernDisintegrate*`（modern 引擎）本來就自己 +1，與 ClientSession 這條路徑無關，不會重複。

### 待驗證（交給 Codex／實機）
- 實機施放 9 個技能時，客戶端正義值顯示（0x59）正確變動，封包順序為 …→`S_CHANGE_ALIGNMENT`→`S_MANA_POINT`。
- 正義值變化對 #304 正義／邪惡的守護（魔法傷害、MR）在競技場內的實際效果。
- 沉睡之霧（−50）連續施放時正義值下降速度是否符合廠商預期（S1 行為即如此）。

## 產出路徑
- 程式：`lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/{BattleSpellCatalog,ClientSession,NativeBlindSceneAction,NativeWeaponBreakBattleCompanion}.java`
- 測試：`lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/{WizardSkillServer1ParityTest,OtherClassSkillServer1ParityTest,NativeWorldChillTouchSourceAuditTest}.java`
- 文件：`docs/design/skills-wizard.md`、`docs/design/skills-others.md`、本報告

## 給下一棒的提示
- 以後新增「施放會改正義值」的技能：record 加 `lawfulDelta`，在扣 MP 後、`S_MANA_POINT` 前呼叫 `applySpellLawfulDelta`，並把處理器加進 `issue325EveryCastHandlerAppliesLawful` 的清單。
- 找處理器最快的方法：搜尋 `<Record>_effectiveMpCost_III` 的呼叫點（每個扣 MP 的入口都會經過 `NativeSkillConsumptionBridges`）。
- 若使用者之後決定競技場「維持」不改正義值，只要把 9 個 catalog 值改回 0、兩個 `LAWFUL_NOT_APPLIED` 清單改回原值即可；處理器呼叫在 delta=0 時是 no-op。

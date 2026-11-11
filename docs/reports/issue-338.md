# Issue #338（T118）魔法計算補驗：魔眼 +1 SP、SKD3 技能減免、傷害為 0 狀態

分支：`task/338`。對照表寫在 `docs/design/formulas-magic.md` §9（並更新 §1 M3／M11／M17 與 §7）。

## 做了什麼

1. **魔眼 +1 SP（M3）**：1-伺服器 `src/com/lineage/server/model/L1MagicMode.java:103-116` 對風（6686）、生命（6687）、形象（6689）之魔眼在 `getTargetSp` +1，只用在 `L1MagicPc.java:1035-1036` 的魔法傷害。LiTo 早已在 `CombatSpecialBuffs.addScalar(...,"SP")` 加上（#304 時沒查到）。
   - 發現差異：LiTo 的 SP 面板（`S_MAGIC_STATUS`）與延伸角色資訊也用同一個 `spellPowerModifier`，魔眼期間面板多顯示 1；1-伺服器 `serverpackets/S_SPMR.java:37` 寫 `getSp()-getTrueSp()`，不含這個 +1。
   - **已修正**：抽出 `CombatSpecialBuffs.EYE_MAGIC_SP_BUFFS`／`eyeMagicSpellPower`，`ClientSession` 新增 `displaySpellPowerModifier`（扣掉魔眼 +1），面板兩處改用它；魔法傷害不變。
2. **SKD3（M11）**：1-伺服器 `L1AttackList.java:256-286` 31 列逐筆對照：28 個料理狀態（特製 −5 ×24、4 階 −2 ×4）、龍之護鎧 −10、堅忍 −15、聖結界減半，全部一致；主菜＋甜點相加也一致。`CombatSourceReductions` 新增具名常數 `SOURCE_PATIENCE`／`SOURCE_DRAGON_SKIN` 供測試；`patience`／`dragonSkin` 兩行維持字面值 15／10（#337 的 `OtherClassSkillEffectServer1ParityTest` 以原始碼字串比對這兩行）。
3. **傷害為 0（M17，SKM0）**：`L1AttackList.java:289-294`／`L1Character.java:1797-1808` 6 項（凍結狀態 4000、絕對屏障 78、冰矛 50、冰雪颶風 80、冰凍氣息 194、大地屏障 157）對上 LiTo `CombatPlayerMagic.protectedZero`（絕對屏障‖大地屏障‖凍結租約），三個冰凍技能共用同一凍結租約。
4. 新測試 `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/MagicReductionServer1ParityTest.java`（7 個測試）。

## 怎麼驗證

### 已驗證
- 1-伺服器 原始碼與 `db/380_db.sql` 逐行讀取（唯讀），行號見設計文件 §9。
- `etcitem_buff藥水能力` 欄位表（`sp` 是第 15 個數值欄）：7 個魔眼的 `sp` 都是 0，LiTo `BattleDragonEyeCatalog` 其他能力值與表一致；+1 SP 是寫死在程式裡，不是表值。
- 本機 `mvn -B -o -DskipTests test-compile` 通過（main＋test 都編得過）。
- 依本機執行限制，測試**沒有在本機跑**，交給 PR 的 GitHub Actions CI（`mvn -B test`）。
- PR #346 CI（commit 1）全部通過：Server tests（6m53s）、Server (Java 17 / Maven)、Semantic gap、Launcher、Build-Package.ps1 zip。
- 合併 main（含 #337）後 CI 的 Server tests 失敗 1 項：`OtherClassSkillEffectServer1ParityTest.dragonSkinAndPatienceUseL1AttackListSkd3Literals` 用原始碼字串找 `CombatRules.loaded()?15:legacy`，被本 PR 的常數化改寫打斷。已把兩行還原成字面值（常數仍保留，`MagicReductionServer1ParityTest` 驗兩者一致），本機 `test-compile` 通過；PR #346 CI（commit `c2c70ed5`，run 43058498613）Server tests、Server (Java 17 / Maven)、Launcher、Semantic gap、Build-Package.ps1 zip 全部通過。
- 同一輪的「CI MySQL fixture」job（non-blocking）失敗是 #334 新增的整合測試 job 本身的既有狀況，與本 PR 無關（main `3897f029` 的 run 43053159670 同一 job 也失敗）。
- commit `b5c487d4` 的 CI（build run 43061017186、package run 43061017388）：Server tests、Server (Java 17 / Maven)、Launcher、Semantic gap、Build-Package.ps1 zip 全部通過；只有上面那個 non-blocking 的 MySQL fixture job 失敗。
- 第 3 輪把上一版的 3 個「推測」查完（只讀原始碼與 `db/380_db.sql`，詳見設計文件 §9.3 Z4／Z6／Z7）：
  - **`STATUS_FREEZE`（4000）沒有差異**：1-伺服器 只有 5 隻怪物會放（`db/380_db.sql:34333,34384,34459,34503,34650` 的 `mobskill` 列），另外方塊只對 NPC 放（`L1Cube.java:298`）。LiTo 的怪物技能目錄沒有收這 5 隻怪，也沒有技能 4000，所以 LiTo 沒有任何路徑會讓怪物凍結玩家。玩家身上的凍結只會由冰矛、冰雪颶風、冰凍氣息寫進 `activateFreeze`。
  - **打世界怪物、召喚物／寵物時「傷害為 0」一致**：呼叫點列在 §9.3 Z6。
  - **堅忍**：`GameHub.java:7691-7697`（房間）與 `:4254-4260`（世界）都走 `BattlePatienceStatus.physicalReduction`（`:54-58`）。堅忍生效時回傳 `patience(2)`（載入規則表時是 15），沒生效時回傳 0。所以「不為 0 ⇔ 堅忍生效」成立。
- **新發現的疑似不一致（這次沒改 Java）**：房間 Quest／限時挑戰的怪物被凍結時，LiTo 仍然會扣血。HP 扣除點 `BattleQuestEncounter.java:2141-2143`、`BattleTimeAttackEncounter.java:2101-2103`，以及 `ClientSession.java:51359-51367` 的 `npcEarthBindProtectedCombatDamage`，都只擋大地屏障。1-伺服器 `L1MagicPc.java:945`（`dmg0(_targetNpc)`）對凍結中的 NPC 一律算 0。沒有直接改的原因：還沒確認冰矛等技能是「先扣傷害、再寫凍結」，還是反過來。如果是先寫凍結，在 `damage()` 加凍結判斷會把冰矛本身的傷害也變成 0。本機又不能跑測試，所以留給後續任務。

### 推測
- Z7 也會影響物理攻擊：1-伺服器 `L1AttackMode.java:129` 對凍結中的目標，物理傷害同樣是 0，而 LiTo 這兩種怪物的 `damage()` 共用同一個判斷。
- 方塊「地震」（`cubeQuakeEffects`，`GameHub.java:12504-12506`）只讓 NPC 的 AI 停住，沒有寫進凍結租約；它造成的狀態在 1-伺服器 算不算「傷害為 0」，沒有追。

### 待驗證
- 實機：使用風／生命／形象之魔眼時，SP 面板**不再**多 1，但魔法傷害仍含 +1（交 Codex）。

## 產出路徑
- `docs/design/formulas-magic.md`（§1 M3／M11／M17、§7、新增 §9）
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/CombatSpecialBuffs.java`
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ClientSession.java`（`displaySpellPowerModifier`）
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/CombatSourceReductions.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/MagicReductionServer1ParityTest.java`
- 本報告 `docs/reports/issue-338.md`

## 給下一棒的提示
- §7 只剩 P1（千分比迴避）、M17-Z7 與實機項目未結。
- **建議開 code 任務處理 Z7**：先讀 `ClientSession.java:45263-45296`、`:88521` 等冰凍技能命中流程，確認傷害和 `questEncounter.freeze`／`timeAttackEncounter.freeze` 的先後順序。確認後，在 `BattleQuestEncounter`／`BattleTimeAttackEncounter` 的 `Monster.damage(amount, true)` 加上「凍結中就擋」（`poisonDamage` 要不要一起擋，先對 1-伺服器 的毒傷路徑），並補「凍結中的 Quest 怪物受魔法傷害為 0」的測試。
- NPC 施法（`L1MagicNpc.java:568` 也讀 SKD3）不在本任務範圍；若要做 NPC→玩家 魔法對照，可直接重用本測試的 `refSkd3()`。
- 面板 SP 修正只扣魔眼 +1；若之後有其他「只進傷害、不進面板」的 SP 來源，也要加進 `displaySpellPowerModifier`。

# Issue #384（T133）凍結中的 Quest／Time Attack 怪物仍會受傷（#338 Z7）

分支：`task/384`。對照表寫在 `docs/design/formulas-magic.md` §9.4（並更新 §1 M17、§7、§9.3 Z7）。

## 做了什麼

1. **確認順序（先讀再改）**：三個冰凍技能（冰矛 50、冰雪颶風 80、冰凍氣息 194）打 Quest／限時挑戰怪物，LiTo 都是**先扣血、再寫凍結**，與 1-伺服器 相同，所以在扣血點擋凍結不會擋到造成冰凍的那一擊。
   - 1-伺服器：`skill/L1SkillUse.java:3133-3138` 在這裡只算機率（原始碼註解：凍結狀態與封包必須等主傷害 accepted），傷害計算時目標還沒凍結。
   - LiTo `ClientSession.java`：範圍 `:87408→:87416`（Quest）、`:87449→:87458`（限時）；單體 `:87932→:87993`、`:87940→:88003`；直線 `:88590→:88606`、`:88635→:88651`（前者是 `NativeSourceNpcDamageDisplay.magicQuest/magicTime` 扣血，後者是 `hub.freezeQuestMonster/freezeTimeAttackActor`）。
   - 佇列路徑 `NativeSourceTargetNpc.java:52-58`：傷害進 HP 佇列，commit 被接受（`claim(m,p.accepted)`）之後才 `release` 寫凍結。
   - Issue 記的 `:45263-45296`、`:88521` 是玩家目標的 `activateFreeze`，行號已位移；本次以方法／呼叫名稱為準。
2. **扣血點擋凍結**：`BattleQuestEncounter.Monster`、`BattleTimeAttackEncounter.Monster` 新增 `damageZeroActive(now)`（大地屏障‖凍結），`damage(amount, true)` 遇到就回 `null`、不扣血。物理與魔法都經這個扣血點（`GameHub.java:8095,8898,13456,13473,13542,13559`）。
3. **魔法計算階段也歸 0**：`ClientSession.npcEarthBindProtectedCombatDamage` 加凍結判斷，對應 1-伺服器 `L1MagicPc.java:945`（計算階段 `dmg0(_targetNpc)`）。這樣攻擊封包也會顯示未命中，行為與大地屏障一致。
4. **`poisonDamage` 不擋**：1-伺服器 `poison/L1DamagePoison.java:116-135` 的毒傷 tick 直接呼叫 `mob.receiveDamage`（`:130`），`Instance/L1MonsterInstance.java:1047-1056` 的 `receiveDamage` 沒有 `dmg0`；`dmg0` 只出現在 `L1AttackPc.java:421,1096`、`L1AttackNpc.java:196,395`、`L1MagicPc.java:945`、`L1MagicNpc.java:667` 的計算階段。所以毒傷維持 `damage(amount, false)`，凍結與大地屏障都不擋。
5. **玩家目標／受控 NPC 目標**：抽查現有檢查點（清單見設計文件 §9.4 F6）都已同時看凍結，沒有找到缺口，沒補任何一處。
6. 設計文件：§1 M17、§7 M17-Z7 改為已修正，§9.3 Z7 標註結案，新增 §9.4。

## 怎麼驗證

### 已驗證
- 1-伺服器 原始碼逐行讀取（唯讀），檔名行號如上。
- LiTo 三條冰凍路徑與佇列路徑的先後順序：讀原始碼確認，行號如上。
- 新測試（已寫好，交 CI 執行）：
  - `BattleQuestEncounterTest.frozenQuestMonsterTakesNoDamageUntilThawAndTheFreezingHitStillCounts`、`BattleTimeAttackEncounterTest.frozenTimeAttackActorTakesNoDamageUntilThawAndTheFreezingHitStillCounts`：先打一擊（扣血成立）→ 寫凍結 → 再打（1 點與滿血量）都不扣血 → 到期解凍後恢復扣血 → 再凍結、`cancelFreeze` 後也恢復。
  - `damagePoisonStillTicksOnFrozenQuestMonster`／`…TimeAttackActor`：凍結中直接傷害被擋，但毒傷 tick 照扣。
  - `FrozenEncounterDamageOrderSourceAuditTest`（3 個）：鎖住 `ClientSession` 各冰凍路徑「扣血呼叫在凍結呼叫之前」、佇列路徑要 `accepted` 才寫凍結、兩個 `Monster.damage` 的判斷式。
- 本機編譯與 CI 結果：見本檔最後一節「建置與 CI」。

### 推測
- 「物理與魔法都經同一個扣血點」是依 `questEncounter.damage`／`timeAttackEncounter.damage` 的全部呼叫點（6 處，都在 `GameHub`）判斷的；測試在 encounter 層驗證，沒有另外從 `ClientSession` 的物理攻擊封包一路打到怪物。
- 凍結中的怪物被衝擊之暈命中時，傷害在計算階段歸 0 後會走 `NativeSourceTargetNpc.withoutHp`，暈眩仍可能成立（與現行大地屏障的行為相同）。1-伺服器 對這個組合的結果沒有追。
- F6 是抽查，不是把所有玩家／受控 NPC 的傷害入口逐一列舉。

### 待驗證
- 實機（交 Codex）：Quest 或限時挑戰中，冰矛凍住怪物的那一擊有扣血；凍結期間物理攻擊與魔法都不掉血；毒傷持續扣；解凍後恢復正常扣血。
- 既有測試 `freezeBlocksQuestAiSurvivesDamageAndPublishesOneExactExpiry`、`freezeBlocksOrdinaryAiSurvivesDamageExpiresAndFinalBossesAreImmune` 沒改：它們用合成時間（0～16 秒）寫凍結，而扣血點用 `System.nanoTime()` 判斷，只要執行機開機超過 16 秒就視為已過期、照舊扣血。這個前提在 CI 成立，但測試本身依賴牆鐘。

## 產出路徑
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/BattleQuestEncounter.java`（`Monster.damageZeroActive`、`damage`）
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/BattleTimeAttackEncounter.java`（同上）
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ClientSession.java`（`npcEarthBindProtectedCombatDamage`）
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/BattleQuestEncounterTest.java`、`BattleTimeAttackEncounterTest.java`（各 2 個新測試）
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/FrozenEncounterDamageOrderSourceAuditTest.java`（新）
- `docs/design/formulas-magic.md`（§1 M17、§7、§9.3 Z7、新增 §9.4）
- 本報告 `docs/reports/issue-384.md`

## 給下一棒的提示
- 之後若改動 `ClientSession` 冰凍技能路徑，必須維持「先扣血、再寫凍結」；順序反了 `FrozenEncounterDamageOrderSourceAuditTest` 會失敗，而且冰矛自己的傷害會變 0。這個測試用「凍結呼叫往前 90 行內要有扣血呼叫」判斷，路徑大幅重排時要一起調整。
- `Monster.damage(amount, true)` 的豁免沿用 `NativeCombatAttacker.earth(...)`（方塊原始效果、proc 直接治療、怪物自己被反射），凍結與大地屏障共用；要讓某種傷害穿透凍結，改那裡而不是 `damageZeroActive`。
- 方塊「地震」（`cubeQuakeEffects`）仍然只停 AI、不寫凍結租約，所以不會觸發這次的擋傷害（#338 的推測項，仍未追）。
- §7 剩 P1（千分比迴避）與實機項目。

## 建置與 CI
- 本機 `mvn -B -o -q -DskipTests test-compile`（`lito伺服器/LiTo381-Server`）通過（main＋test 都編得過）。
- 依本機執行限制，測試**沒有在本機跑**，交 PR 的 GitHub Actions CI（`mvn -B test`）。
- PR #396 CI（程式碼 commit `e63e6cf2`，run 43387377711）：
  - 第一次執行「Server tests」在測試開始前失敗：Maven Central 抓不到 `org.apache.maven.surefire:surefire-junit-platform:jar:3.2.5`（下載問題，測試沒有執行）。重跑失敗的 job 後通過。
  - 重跑結果：Server tests **通過**（`BattleQuestEncounterTest` 52 個、`BattleTimeAttackEncounterTest` 63 個、`FrozenEncounterDamageOrderSourceAuditTest` 3 個，皆 Failures 0／Errors 0／Skipped 0）；Server (Java 17 / Maven)、Server reproducible JAR、Launcher、Semantic gap、Build-Package.ps1 zip 都通過。
  - 「Server tests (CI MySQL fixture)」（non-blocking）失敗 16 個測試類別，與 main `b2444c77`（run 43382420925）同一 job 的失敗清單逐類別、逐數字相同，不是本 PR 造成；本 PR 的三個測試類別在這個 job 也都通過。
- 本報告這一節是在上述 CI 之後補寫的文件 commit，該 commit 自己的 CI 結果以 PR 頁面為準。

# Issue #353（T122）競技場 PvE 怪物技能盤點與對照

分支：`task/353`。對照表：`docs/design/skills-npc.md`（任務原指定的這份檔案原本不存在，本次新建）。

## 做了什麼

1. **盤點範圍**：Quest（9 種怪＋召喚的史巴托）與 Time Attack（一般波次、4 隻 Boss、3 隻隨機龍）共 19 種 npc，其中 9 種在 1-伺服器 有 `mobskill` 列，合計 **33 列**；用到 `skills` 表非玩家列 **16 列**（另有 1 個不存在的 31000），玩家列 3 列。588 列非玩家技能中其餘 **572 列不在範圍**，只記總數。
2. **逐列對照**（目標、範圍、傷害或效果、機率、冷卻、動作／特效／延遲）：33 列裡 26 列一致、2 列本次修正、1 列刻意不同、4 列未修；另有 11 條共通行為（C1–C11），其中 C4、C5 本次修正。
3. **修正（Java）**：
   - **歐吉 45278/0 ActId**：1-伺服器 `db/380_db.sql:34296` 是 30，`L1MobSkillUse.java:660-661` 會 `setActId(30)`。LiTo 原本寫 0（送普攻動作 1）→ 改成 30（`BattleTimeAttackMagicSkillCatalog.java:417`）。gfx1020 在 `spr-action-380.csv` 有 action 30；世界版目錄本來就是 30。
   - **攻擊位置 50% 普攻分支（C4）**：1-伺服器 `L1NpcInstance.java:894-907` 在貼身時先擲 `nextInt(2)`，擲到 1 就普攻，擲到 0 才擲 TriRnd。LiTo 原本每次都直接擲 TriRnd，所以貼身放技能的機率大約是 1-伺服器 的 2 倍。新增 `BattleTimeAttackMagicSkillCatalog.ordinaryAttackBranch`。Quest／TA 各 3 個隨機入口（`aiAttackFor`、`nativeSourceViewActionAiAttackFor`、`nativeSourceNpcPlayerFirstAiAttackFor`）改走 `applyOrdinaryAttackCoin`：擲到普攻時，把所有觸發骰設成 100。因為 100 一定不滿足「roll < TriRnd」，就會走原本的貼身普攻分支。固定骰（fixture）入口不變。
   - **reuseDelay 屬於施法者（C5）**：1-伺服器 `L1SkillUse.java:2675-2676` 用 `L1SkillDelay.tryReserveSkillUse(_user, …)` 鎖住整個施法者，`:2642/:2658` 在延遲期間拒絕該 NPC 所有 `L1SkillUse` 施法。LiTo 原本依 actNo 分開計，死亡騎士放完 10056（reuse 3000）後還能立刻放 28 → 新增 `CASTER_SKILL_DELAY_KEY`，Quest／TA 的 magic／visual／ground ready 共用同一把鑰匙。召喚（不經 `L1SkillUse`）不受影響。
4. **測試**：
   - `BattleTimeAttackMagicSkillCatalogTest`：歐吉 actionOverride／effectiveActionId＝30；新增 `attackPositionCoinMatchesRetainedL1NpcInstanceAttack`，驗證擲幣真值表、參數檢查，以及 9 隻怪物每一列在 roll 100 時都不會觸發。
   - `BattleQuestEncounterTest`：歐吉 effectiveActionId 改為 30；新增 `questDeathKnightEarthquakeReuseDelayIsCasterWideLikeRetainedL1SkillDelay`：10056 放出後第 2 秒放不出 28，但同一時間召喚可以放（證明本體已可行動），第 3.5 秒可以放 28。

## 怎麼驗證

### 已驗證
- 1-伺服器 `db/380_db.sql`、`L1MobSkillUse.java`、`L1SkillUse.java`、`L1NpcInstance.java`、`L1MobSkill.java`、`L1SkillDelay.java` 逐行讀取（唯讀），行號寫在設計文件。
- 總數以 PowerShell 計數：`skills` 807 列，其中 >220 有 588 列；`mobskill` 972 列；範圍內 33 列（`scratch/t122/mobskill.txt`、`skills.txt`、`npc.py`）。
- 本機 `mvn -B -o -q -DskipTests test-compile` 通過（main＋test）。
- 依本機執行限制，**測試沒有在本機跑**，交給 PR 的 GitHub Actions CI（`mvn -B test`）。
- PR #361 CI（run 43137706332）：Server tests（6m48s）、Server (Java 17 / Maven)、Launcher、Semantic gap、Build-Package.ps1 zip 全部通過。唯一失敗的是 non-blocking 的「CI MySQL fixture」job；main `c7d45249` 的 run 43137284897 同一個 job 也失敗，是既有狀況，與本 PR 無關。

### 推測
- 30004 火牢在 1-伺服器 只有特效：整個原始碼搜不到 30004 特例，但沒有實際執行確認。
- 物理技能後的動作時間在兩邊應該相等，但沒有逐隻比數字（1-伺服器 用 `getAtkspeed()`；LiTo 用 `NativeSourceNpcBaseSpeeds`）。
- 擲幣只套在距離 ≤1。ranged=2 的怪物（歐吉、龍、殭屍王、烏龜龍、蠍子）在 1-伺服器 距離 2 也算攻擊位置，但 LiTo 的競技場 AI 在距離 2 沒有普攻分支，所以那一格維持原行為。

### 待驗證
- 實機（交 Codex）：Quest 歐吉放技能時播 action 30；貼身時 Boss／龍放技能的頻率約為原本一半；死亡騎士放完 10056 後 3 秒內不再放吸血鬼之吻。

## 產出路徑
- `docs/design/skills-npc.md`（新建）
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/BattleTimeAttackMagicSkillCatalog.java`
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/BattleQuestEncounter.java`
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/BattleTimeAttackEncounter.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/BattleTimeAttackMagicSkillCatalogTest.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/BattleQuestEncounterTest.java`
- 本報告 `docs/reports/issue-353.md`

## 給下一棒的提示（剩餘清單，依玩家可見影響排序）
1. **反制魔法擋 NPC 魔法（C11）**：1-伺服器 `L1SkillUse.java:2767-2775,2871,2899,4243-4245`。玩家有 COUNTER_MAGIC 時會擋掉一次 NPC 魔法，`EXCEPT_COUNTER_MAGIC`（`:334-344`，含 10027–10029）以及 DB 表 `SkillCounterMagicExceptionTable` 例外。LiTo 的 Quest／TA NPC 魔法流程（`ClientSession.java` 約 `:54440-54600` 與 TA 對應段 `:54729` 附近）目前沒有這段。要先讀 DB 例外表內容。
2. **安息 10027／10028／10029 的 NPC 喊話「$3717」**：`L1SkillUse.java:3037-3041`，每次命中都廣播 `S_NpcChat`。三隻龍的 act1 都缺。
3. **ChangeTarget=3（C9）**：龍的 8 列在多人 TA 時會從仇恨名單裡隨機換目標（`L1MobSkillUse.java:777-842`）。GameHub `appendMonsterAiTick`（`GameHub.java:12500` 起）要把候選玩家傳進 encounter。
4. **巴拉卡斯火牢 30004**：LiTo 有 3×3 持續火傷（`CAT:450`），1-伺服器 推測只有特效。要不要對齊需要主控／使用者決定（對齊會削弱 Boss）。
5. 「先選列、後檢查 MP／延遲」的語意差異（C10），影響小。
6. 距離 2 的攻擊位置擲幣（ranged=2 的怪物），需要先有距離 2 的普攻分支。

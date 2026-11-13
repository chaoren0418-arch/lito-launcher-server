# Issue #340 報告 — T121：NPC 打 NPC／寵物／召喚物的公式（#317 N2／N3）

## 裁定
使用者在 needs:user 15 分鐘內沒有回覆，調度器依常設授權採用逾時預設 **A：對齊 1-伺服器**（Issue #340 留言）。N1（閃避）與 #318 H7 同一條規則，維持 LiTo，不在本張。

## 做了什麼
三條路徑從「NPC 打玩家」公式（命中 Lv+5、傷害 `nextInt(Lv) + STR×0.8 + STRD`）改成 1-伺服器 的「NPC 打 NPC」公式：

| 路徑 | 位置 | 命中 | 傷害 |
|---|---|---|---|
| P3 WORLD 怪物 → 玩家的寵物／召喚物 | `ClientSession.nativeNpcWeaponCompanionBody`（`CS:77276`、`77296`） | `CNA.hitRate`：Lv+3、BOSS +30 在防禦骰之前 | `CNA.nonPetBeforeReduction`：`nextInt(Lv) + STR/2 + STRD` → `npcDmgMode` |
| P4 WORLD 怪物 → 護送 NPC（Dspy） | `ClientSession.nativeNpcWeaponDspyBody`（`CS:77343`、`77357`） | 同上 | 同上 |
| P7 競技場召喚物 → NPC（任務怪／時間競技演員） | `ClientSession` 三處（`CS:52325`／`52333`、`79289`／`79296`、`79485`／`79492`） | `companionNpcTargetHitChance` → `CNA.hitRate` | `companionNpcTargetDamage` → `CNA.nonPetBeforeReduction`（補上截斷、BOSS、不死夜間、武器破壞） |

1-伺服器 依據（唯讀）：
- `src/com/lineage/server/model/L1AttackNpc.java:200-250`（`calcNpcHit`：`getLevel() + 3`、BOSS `attackerDice += 30` 在防禦骰之前）
- `src/com/lineage/server/model/L1AttackNpc.java:274-302`（`npcDmgMode`）
- `src/com/lineage/server/model/L1AttackNpc.java:409-415`（`calcNpcDamage` 非寵物分支：`nextInt(lvl) + getStr()/2 + STRD`；怪物與召喚物都走這支）
- `src/com/lineage/server/model/L1AttackMode.java:275-293`（`calcNpcDamageReduction`，LiTo 對應 `BattlePhysicalDefense.againstNpc`，原本就一致）

程式變更：
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ClassicWorldCompanionNpcPhysicalAttack.java`：新增 `hitRate`、`undeadNight`、`nonPetBeforeReduction`；`npcDmgMode` 抽成私有共用，`damage()`（P6 寵物打怪）改呼叫它，數值不變。
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ClientSession.java`：P3／P4／P7 呼叫點；新增 `companionNpcTargetHitChance`／`companionNpcTargetDamage`／`companionSourceBoss`；刪除舊 `companionNpcPhysicalDamage`（STR×0.8、只做暴擊）。
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/NpcToNpcFormulaIssue340Test.java`（新增，7 個測試方法）。
- `docs/design/formulas-physical.md` §8：P3／P4／P7 列、N-H12、N-D15 改成「已對齊」；§8.4 改為裁定結果；新增 §8.5。

## 怎麼驗證
### 已驗證
- 本機 `mvn -B -q -DskipTests test-compile` exit 0（只編譯，**本機未執行任何測試**，依本機執行限制）。
- 讀碼確認舊測試沒有直接依賴 P3／P4／P7 的數值（`src/test` 搜尋 `nativeNpcWeaponCompanionBody`、`nativeNpcWeaponDspyBody`、`companionNpcPhysicalDamage`、`uncannyDodgeHitFixture` 皆無命中）。
- 新測試以 1-伺服器 公式另寫參考函式、列舉每個 `nextInt` 面：
  - `npcToNpcHitRateMatchesCalcNpcHitForEveryFace`：Lv × AC −25..20 × BOSS × d20 × 負 AC 骰。
  - `nonPetDamagePrefixMatchesCalcNpcDamageSummonBranchForEveryFace`：Lv × STR（含 0、1..10 的負 STRD）× 等級骰 × 暴擊 100 面 × 不死夜間 × BOSS × 武器破壞層數。
  - `worldMonsterToNpcPipelineMatchesCalcNpcDamageEndToEnd`：P3／P4 實際順序（前段 → `afterStacks` → `againstNpc`）對 `calcNpcDamage` 全流程（含 `calcNpcDamageReduction`）。
  - `companionDamageSummonBranchEqualsNonPetPrefixMinusReduction`：重構後 P6 `CNA.damage` 不變。
  - 另有平衡錨點、不死類型、輸入範圍檢查。
- CI 結果：見 PR 的 GitHub Actions（push 後由 CI 跑 `mvn -B test`）。

### 推測
- 影響幅度（讀碼推算）：怪物打寵物／護送 NPC 每下約少 0.3×STR；BOSS 打寵物／護送 NPC 幾乎必中；競技場召喚物（Lv 5、STR 15）打 NPC 不暴擊 9..13（原 14..18）、暴擊 16..23（原 25..32）。
- 競技場召喚物的 `npcDefenseQualified()` 推測多為 false，此時 BOSS／不死夜間視為否（與舊行為一致：舊公式本來就沒有這兩項）。

### 待驗證（交給 Codex／實機）
- 實機：WORLD 怪物打寵物、Dspy 護送任務被刺客打的命中率與傷害（伺服器 log `S_ATTACK classic-world-monster=… hit-chance=`、`S_ATTACK Dspy-assassin=…`）。
- 實機：競技場召喚物打任務怪／時間競技演員的傷害，確認關卡平衡仍可接受（傷害降低約 35%）。

## 未做／範圍外
- P2（WORLD 怪物的**物理技能**打寵物，`CS:28440`）仍用 `NativeNpcWeaponBreakDamage.sourceExact`（NPC → PC 公式）；本張範圍只列三條路徑，沒改。
- 競技場召喚物打機器人（AC 10 的假玩家）仍用 NPC → PC 包絡。
- N1 閃避：維持 LiTo。

## 產出路徑
- `docs/reports/issue-340.md`（本檔）
- `docs/design/formulas-physical.md` §8
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/{ClassicWorldCompanionNpcPhysicalAttack,ClientSession}.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/NpcToNpcFormulaIssue340Test.java`
- 分支 `task/340`

## 給下一棒的提示
- 若要讓 P2（怪物物理技能打寵物）也對齊，`CS:28440` 附近在目標是寵物時改呼叫 `CNA.nonPetBeforeReduction`／`CNA.hitRate` 即可，建議另開一張 code 任務並先問使用者。
- 競技場召喚物傷害下降，若使用者驗收時覺得任務怪／時間競技太難打，可調整的是召喚物 STR／等級（資料），不是公式。
- 正式 JAR 仍由 `tools/*/build.py` patch 產生（見 CLAUDE.md T002），本變更只進 `src/main`；部署前要確認 patch 鏈有帶到。

# Issue #303 報告：T101 物理命中／傷害／AC 減傷對照 1-伺服器（規格＋單元測試）

- 分支：`task/303`（code，第 1 次嘗試）
- 規格文件：`docs/design/formulas-physical.md`
- 單元測試：`lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/PhysicalFormulaServer1ParityTest.java`（18 個測試方法）
- 差距矩陣：`docs/reports/server-capability-matrix.md` 第 32–36 列已更新

## 做了什麼

1. 逐項對照 1-伺服器 `L1AttackPc`（calcPcHit／calcNpcHit／weaponDamage1/2／pcDmgMode／calcPcDamage／calcNpcDamage／calcMaterialBlessDmg／calcAttrEnchantDmg）、`L1AttackMode`（attackerDice／calcErEvasion／calcPcDefense／calcNpcDamageReduction）、`CalcStat`＋`BaseStatTable`、`L1*ClassFeature`、`ConfigPcDamage`，與 LiTo 的 `BattlePhysicalAttack`、`BattlePhysicalDamage`、`BattlePhysicalOffense`、`BattleWeaponCritical`、`BattlePhysicalDefense`、`CombatClassDamage`、`CombatRules` 以及 `ClientSession` 的三條物理路徑（競技場、WORLD PvP、WORLD 打怪）。共 39 項，每項並列兩邊的「檔案:行號」並標判定。
2. 修改 LiTo Java 對齊 1-伺服器（3 項）：
   - **H10 NPC 防禦骰**：1-伺服器 打 NPC 用 `10 − AC`，不擲負 AC 骰；LiTo 原本對 NPC 也套玩家的 `10 + rnd(|AC|×1.5)`。新增 `BattlePhysicalAttack.hitChanceAgainst(npcTarget, …)`，`ClientSession` WORLD 打怪（`:47801`）與競技場（`:85923-85929`，只在玩家目標才擲負 AC 骰）改用新方法。
   - **H8 龍騎 ×1.01／火屬性妖精 ×1.02 攻擊骰**：LiTo 原本沒有；新增 `classAttackerDice`，三個呼叫點都傳 `this.elfAttribute`。
   - **R2 AC 額外減傷除數**：1-伺服器 只在除數 > 0 時生效；LiTo 原本只排除 0，負數會變加傷。`CombatRules.extraAcReduction(ac, divisor)` 改成只接受 > 0。
3. 確認 `CombatClassDamage` 的呼叫點在 `src/main` 原始碼裡（不是只靠 patch）：競技場 `ClientSession:86160-86175` → `CombatCompanionDamage.physical` → `CombatKiringku.finish:36` → `CombatClassDamage.physical`；WORLD PvP `:48847`；WORLD 打怪 `:47930` → `CombatWorldPhysical.java:50`；`CombatClassDamage.hit` 在 `:47948`、`:48848`、`:86175`。
4. 素質表：唯讀解析 `1-伺服器/db/380_db.sql`（`scratch/t303_growth_compare.py`），STR／DEX 兩表各 127 列，與 LiTo 後備表 0 筆不一致；表內容原樣放進測試常數。

## 怎麼驗證

### 已驗證
- 本機 `javac`（JDK 17，`-proc:none`，只編譯不執行）全量編譯 `src/main/java`＋`compile-stubs`（1174 檔）加新測試與 `BattlePhysicalAttackTest`：**exit 0**。本機沒有 Maven，依規定也不執行測試。
- 素質表比對：`py -3 -I scratch/t303_growth_compare.py …/380_db.sql` → `mismatches=0`（STR、DEX 各一次）。
- 職業命中補正、職業攻擊等級除數、AC 減傷除數：1-伺服器 `classes/L1*ClassFeature.java` 七個職業逐一讀過，與 LiTo 相同。
- 既有測試相容性推論：舊的 `hitChanceWithStatBonus` 多載改成委派（`npcTarget=false, elfAttribute=0`），只有龍騎在攻擊骰 ≥ 100 時才會 +1，那時已經達到必中門檻（`hit+10`）；`ModernRangedFormulaCrossCheckTest` 的輸入範圍內攻擊骰非必中時最大 99，結果不變。來源稽核測試沒有比對被改到的 `ClientSession` 字串（已搜尋 `hitChanceWithStatBonus`、`negativeAcRollMaximum`、`var97 < 0`、`armorClass() < 0`）。

- **PR #316 CI（run 42910866946，測試修訂 `2d6df013`＝PR 合併提交，內容為 `0dc1a540`）**：`Server tests` 通過，`mvn -B test` → **Tests run: 6072, Failures: 0, Errors: 0, Skipped: 117**，`BUILD SUCCESS`；`PhysicalFormulaServer1ParityTest` 18 項全過（0.135 s）。Server（Maven 建置）、Launcher、Build-Package、Semantic gap 也都通過。WORLD 打怪與競技場打負 AC NPC 的命中率改變（例：AC −20 的 NPC 防禦骰從 11..40 變成固定 30），沒有讓任何既有自測失敗。

### 待驗證（交給 Codex／使用者）
- 實機：負 AC 怪物的命中手感、龍騎／火妖精命中，需要使用者或 Codex 實機確認。
- 文件 §2 D11（PvE 屬性抗性來源）、D12（祝福旗標 0／1 慣例）、§1 H4／H13（負重、千分比迴避在 WORLD 是否需要）。

### 推測
- H3：負強化武器的命中截斷有 0～1 的差（1-伺服器 對整體截斷），影響極小，未改。
- H5、R8：buff 命中來源、寵物倍率、聖結界位置看起來一致，未逐值測（buff 清單屬 T102）。
- LiTo 註解裡的「retained/same-family source」很可能就是 1-伺服器 的較舊版本：config 鍵名（`Other_To_is*`、`AC_REDUCTION_DIVISOR`、`MR_REDUCTION_PER_10`、`PC_TO_PET_DAMAGE_MULTIPLIER` 預設 0.125）與 DB 表名完全相同，但 1-伺服器 之後又加了閃避 /8、弓 DEX 倍率等新規則。

## 未做（待使用者裁定）

以下是有差異、但改了會明顯影響平衡（或有證據顯示 LiTo 是刻意這樣做）的項目，本 PR **沒有改**，詳見 `formulas-physical.md` §7：

| 代號 | 差異 | 為什麼沒改 |
|---|---|---|
| H7 | 閃避：LiTo 直接扣（3.81 原版），1-伺服器 `dodge/8`（至少 1、最多 20） | LiTo 有測試明確釘住「直接扣」，判定為刻意；改了以後迴避／鏡像會從 −5 變成 −1 |
| D4 | 弓 DEX 傷害 PvP ×2.5、PvE ×1.5 | `ModernRangedCombat.java:7-8` 寫明不採用；妖精弓傷會大增 |
| D9 | 一般武器少加職業攻擊等級（`getAttackLevel`，騎士 Lv/6 等），只有奇古獸有加 | 全職業每下傷害上升，會牽動大量既有自測；除數本身已測過一致 |
| D11 | PvE 屬性抗性用弱點 ±50 代替 NPC 四屬性抗性 | 要先確認 NPC 表欄位 |
| D12 | 祝福旗標 0／1 慣例 | 要先確認兩邊載入程式是否已經換算 |

## 產出路徑

- `docs/design/formulas-physical.md`（新增）
- `docs/reports/issue-303.md`（本檔）
- `docs/reports/server-capability-matrix.md`（第 32–36 列）
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/BattlePhysicalAttack.java`
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ClientSession.java`（三個呼叫點）
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/CombatRules.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/PhysicalFormulaServer1ParityTest.java`（新增）
- `scratch/t303_growth_compare.py`、`scratch/t303_growth_compare.txt`（不進 git）

## 給下一棒的提示

- 先看 PR CI；如果有自測因為 NPC 命中率改變而失敗，那是預期中的行為改變，應該更新自測的期望值，不要把 H10 改回去（1-伺服器 與 3.81 原版 L1J 打 NPC 都是 `10 − AC`）。
- H7／D4／D9 請使用者裁定。若決定對齊，建議各開一個 code 任務，每個任務只改一條規則，並把 `PhysicalFormulaServer1ParityTest` 裡對應的參考函式（`refHitRate` 的閃避項、弓 DEX 倍率、`REF_ATTACK_LEVEL_DIVISOR`）接進完整的傷害管線測試。
- NPC／召喚物打人（`L1AttackNpc`、`ClientSession.companionHitChance:52452`）不在本任務範圍；它的負 AC 骰上限是 `−AC`，不是 `|AC|×1.5`，對照時要另外看 `L1AttackNpc.java:126-155`。

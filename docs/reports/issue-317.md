# Issue #317 報告：T112 NPC／召喚物攻擊公式對照 1-伺服器（L1AttackNpc）

- 分支：`task/317`（code，第 1 次嘗試）
- 規格文件：`docs/design/formulas-physical.md` §8（新增「NPC／寵物／召喚物攻擊」一節）
- 單元測試：`lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/NpcAttackFormulaServer1ParityTest.java`（13 個測試方法）
- 差距矩陣：`docs/reports/server-capability-matrix.md` 新增「NPC／寵物／召喚物攻擊」一列（T112）

## 做了什麼

1. 逐項對照 1-伺服器 `L1AttackNpc`（calcPcHit／calcNpcHit／npcDmgMode／calcPcDamage／calcNpcDamage）、`L1AttackMode`（attackerDice／calcErEvasion／calcPcDefense／calcNpcDamageReduction）、`L1AttackList.STRD`、`L1GameTime`、`ConfigPcDamage` 與 LiTo 的 8 條 NPC 物理攻擊路徑（WORLD 怪物打玩家／物理技能／打寵物／打護送 NPC、寵物打玩家、寵物打怪、競技場召喚物、競技場 Tournament／Time Attack NPC）。共 28 項（命中 12、傷害 16），每項並列兩邊的「檔案:行號」並標判定。
2. 確認 LiTo 的 `L1_ATTACK_NPC_SOURCE_SHA256`／`L1_ATTACK_MODE_SOURCE_SHA256` 與目前 1-伺服器 檔案的 SHA256 相同（`Get-FileHash`），所以 LiTo 是對著同一版移植的。
3. 修改 LiTo Java 對齊 1-伺服器（4 項）：
   - **N-D1 STRD**：WORLD 的 NPC 路徑原本用玩家的 `角色_力量成長` 查 STR 傷害。STR 1..127 時兩表相同，但該表沒有 STR 0（會夾到 −5，STRD 是 −6）。新增 `CombatNpcPhysicalSource.npcStrengthDamage`，`ClientSession` 五個呼叫點改用它。
   - **N-D14 召喚物 STR ≤ 10 會丟例外**：`ClassicWorldCompanionNpcPhysicalAttack.damage` 拒絕負的 STRD，但 STR 0..10 的 STRD 本來就是負數（−6..−1）。`380_db` 有 162 隻 STR 1..10、102 隻 STR 0 的怪物，被迷魅或召喚後打怪就會 `IllegalArgumentException`。下限改成 −6。
   - **N-D4 後備公式**：`ClientSession.companionPhysicalDamage`（`CombatRules` 未載入或 ARENA 召喚物用）暴擊後沒有 `(int)` 截斷，STR 傷害用 `floorDiv(STR−11,2)`（STR ≥ 25 起不同）。改成直接呼叫 `CombatNpcPhysicalSource.base/mode`。
   - **N-H3 召喚物打玩家不扣閃避**：怪物打玩家會扣迴避／鏡像等閃避，寵物／召喚物打玩家卻完全不扣。新增 `companionHitChanceAgainst`／`companionPlayerHitChance`，打玩家時用與怪物路徑相同的 `BattleNpcPhysicalAttack.hitChance`（LiTo 直接扣的規則，見 N1），四個呼叫點改用；打 NPC／機器人時行為不變。
4. 唯讀掃描 `1-伺服器/db/380_db.sql` 的 `npc` 表 STR 分布（`scratch/t317_npc_str_scan.py`，`py -3 -I` 執行，輸出 `scratch/t317_npc_str_scan.txt`）：4140 列；STR 0 的 L1Monster 102 隻（Lv 0–100）、STR 1..10 的 162 隻、STR > 127 的 0 隻。

## 怎麼驗證

### 已驗證
- 本機 `javac`（JDK 17，`-proc:none`，只編譯不執行）全量編譯 `src/main/java` + `compile-stubs`（1177 檔）：**exit 0**；新測試 + `BattleCompanionTest` + `ClassicWorldCompanionNpcPhysicalAttackTest` 對編譯結果編譯：**exit 0**。依規定本機不執行測試。
- 參考檔雜湊：`L1AttackNpc.java` = `7BA89744…C3E8`、`L1AttackMode.java` = `F29C4D0B…2C2`，與 LiTo 常數相同。
- 靜態讀碼：`L1AttackNpc.java:173-176` 的 BOSS `attackerDice += 30` 在 `_hitRate` 決定之後才加，之後沒有再讀 → 在 1-伺服器 沒有效果（解掉 `server1-inventory.md` §3 的推測）；NPC→NPC 版（`:215-218`）在防禦骰之前，有效。
- `PET_TO_PC_DAMAGE_MULTIPLIER`：1-伺服器 程式預設 0.125，但實際設定檔 `config/其他控制端/5-職業傷害設定表.properties:90` 是 0.3，與 LiTo 預設相同。
- 既有測試相容性推論：`BattleCompanionTest:670-672` 的期望值 4、5、7 用新公式手算不變（14→4.2、18→5.4、25.2→截斷 25→7.5）。測試目錄搜尋過 `companionHitChance`、`requiredPhysicalGrowthStats`、`floorDiv(n2 - 11`、`summonStrengthDamage`，沒有來源稽核測試比對被改到的字串；`NativeClassicWorldCompanionPlayerSourceAuditTest` 只檢查 `CNA` 檔含 `level + 3`、`runtimeLevel / 14` 等字串與 `ClassicWorldCompanionNpcPhysicalAttack.damage(` 的順序，都還在。

### 待驗證（交給 CI／Codex／使用者）
- **CI**：`mvn -B test` 結果要看 PR 的 GitHub Actions（push 後才有）。本報告寫成時尚未有結果。
- 實機：寵物／召喚物打開了迴避的玩家的命中手感；STR 0 怪物、STR ≤ 10 的迷魅寵物打怪不再出錯（伺服器 log 不再出現 `invalid companion NPC damage input`）。
- N-D7、N-D9：防具減免與 buff 減免在 NPC→PC 路徑的逐值對照（buff 清單屬 T102）。

### 推測
- N-H1：NPC hitup／dmgup 在 LiTo 一律 0，因為 1-伺服器 只由 GM 指令或 buff 寫入，`npc` 表沒有欄位。
- N-D16：競技場 Tournament／Time Attack 的 NPC 先把 STR 夾到 ≥ 1 再查玩家表，結果與 STRD 相同，未改。
- N-H10：物理傷害為 0 的狀態清單（冰凍、絕對屏障、大地屏障）與 1-伺服器 `isSkillEffectPhysicalDamageZero` 是否完全相同，屬 T102。

## 未做（待使用者裁定）

詳見 `formulas-physical.md` §8.4：

| 代號 | 差異 | 為什麼沒改 |
|---|---|---|
| N1 | NPC 打玩家的閃避：LiTo 直接扣，1-伺服器 /8 | 與 §7 H7 是同一條規則，一起裁定 |
| N2 | 怪物打寵物／護送 NPC、競技場召喚物打 NPC 用 Lv+5（NPC→PC 包絡），1-伺服器 NPC→NPC 是 Lv+3、BOSS +30 有效 | 會改變寵物存活率；LiTo 在這些路徑一致地把受控 NPC 當玩家處理，看起來是刻意簡化 |
| N3 | 同上路徑的傷害用 STR×0.8，1-伺服器 是 STR/2（寵物攻擊者另有公式）；競技場召喚物打 NPC 還少了 npcDmgMode 的截斷／BOSS／不死／武器破壞 | 競技場召喚物（Lv 5、STR 15）打 NPC 會從約 12 降到約 7，需要重新平衡 |

## 產出路徑

- `docs/design/formulas-physical.md`（§8 新增、範圍說明更新）
- `docs/reports/issue-317.md`（本檔）
- `docs/reports/server-capability-matrix.md`（新增 T112 一列）
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/CombatNpcPhysicalSource.java`
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ClassicWorldCompanionNpcPhysicalAttack.java`
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ClientSession.java`（9 個呼叫點 + 2 個新 helper + 後備公式）
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/NpcAttackFormulaServer1ParityTest.java`（新增）
- `scratch/t317_npc_str_scan.py`、`scratch/t317_npc_str_scan.txt`（不進 git）

## 給下一棒的提示

- 先看 PR CI。若 `NpcAttackFormulaServer1ParityTest` 失敗，訊息會印出失敗的骰面組合；請以 1-伺服器 行號為準修正 LiTo，不要改參考函式。
- 若有整合測試因為「召喚物打玩家現在會扣閃避」失敗，那是預期中的行為改變，應更新期望值。
- N1／N2／N3 請使用者裁定。若決定對齊，`ClassicWorldCompanionNpcPhysicalAttack.hit/damage` 已是逐行移植，P3（`CS:77178`、`77190-77216`）、P4（`CS:77246`、`77252-77268`）、P7 打 NPC（`CS:52317/52324`、`79194/79201`、`79390/79397`）改呼叫它即可，建議每條路徑一個 code 任務。
- 本節的 `CS` 行號是 task/317 分支修改後的行號；`ClientSession.java` 很常被改，合併後如有偏移請以方法名（`nativeNpcWeaponMonsterBody`、`nativeNpcWeaponCompanionBody`、`nativeNpcWeaponDspyBody`、`companionHitChanceAgainst`）搜尋。

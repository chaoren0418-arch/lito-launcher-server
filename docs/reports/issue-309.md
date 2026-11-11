# Issue #309 報告：T109 技能行為驗證——騎士／黑暗妖精／王族／龍騎士／幻術師（62 個）

- 分支：`task/309`（code，第 1 次嘗試）
- 規格／對照：`docs/design/skills-others.md`（共通流程 O1–O13、效果數值 §2、逐技能 62 列）
- 測試：`lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/OtherClassSkillServer1ParityTest.java`（新）
- 輸入表：`lito伺服器/LiTo381-Server/src/test/resources/server1/skills-others-62.tsv`（62 列 × 16 欄）
- 擷取工具：`lito伺服器/LiTo381-Server/tools/skill-targeting/extract_others.py`
- 覆蓋表：`lito伺服器/LiTo381-Server/docs/maintenance/combat-coverage-current.md` 87–91、97–118、181–195、201–220 共 62 列的「完整行為」欄已更新
- 差距矩陣：`docs/reports/server-capability-matrix.md` 技能列已更新

## 做了什麼

1. **輸入資料**：從 `1-伺服器 db/380_db.sql` 唯讀擷取 `skills` 的 62 列（範圍依覆蓋表 SQL 分組，去掉 name=none 的保留槽）。
2. **靜態比對**（`scratch/t309/compare.py`，未進 git）：解析 catalog 每個 `new XxxSpell(` 的建構參數對應 record 元件，包括包住 `AttackSpell` 的 192／202／203／207／208／213，逐欄對 SQL 的 MP／HP／冷卻／期限／材料／傷害骰／屬性／type／正義值。不一致只有 4 處：
   - **208 骷髏毀壞 `lawfulDelta`=45**，SQL `lawful=0`（45 是同一列的 `probability_value`）→ **已修正**。
   - 208 期限 2 vs SQL 3：S1 `skillmode/BONE_BREAK.java:35` 固定 2 秒，LiTo 一致（正式環境 `CombatDatabase.java:147` 也固定回 2）。
   - 103 暗黑盲咒 SQL `lawful=-10`，`BlindSpell` 沒有欄位 → 與 T107 的 20／40 同一個缺口，未改。
   - 108、202 的 `attribute` 是 16（`ATTRIBUTE_NONE`）vs SQL 0：S1 `L1MagicMode.java:188-230` 對 0 和 16 都不算抗性，等價。
3. **效果數值**：逐項對 `skills_buff_ability`（15 個技能）、skillmode 固定值（覺醒、洞察／恐慌、閃避、SP、ER、增幅防禦、附加劇毒、骷髏毀壞）與 `ConfigSkill` 外控鍵（T108 建議併入本任務的倍率）。全部一致；正式環境 LiTo 經 `config/combat.properties:5` 直接讀 S1 的 `4-職業_技能設定表.properties`。
4. **改 Java**：
   - `BattleSpellCatalog`：208 `lawfulDelta` 45 → 0，建構子檢查與註解同步改。
   - `BattleBoneBreakCatalogTest`、`ClientSessionBoneBreakIntegrationTest`：正義值斷言 45 → 0（後者測試方法改名 `…KeepsLawful…`）。
5. **單元測試**（`OtherClassSkillServer1ParityTest`，11 個單項＋3 組 62 列參數化）：
   - 消耗、冷卻、期限、材料逐列對 SQL（包裝 record 用 `skillId()` 方法；108 用 `AttackSpell.hpCost()`）；傷害骰／屬性（0≡16）／type／正義值；`lawfulGapListIsExact` 釘住 {103}。
   - MP 公式（INT 1..40 × 減免 0..2 × 頭盔，含騎士 `INT−12`）；王族／騎士／黑妖／龍騎／幻術的原始 INT 減免。
   - 能力值、skillmode 固定值、`NativeSourceSkillDefaults` 的 18 個缺鍵預設等於 S1 `ConfigSkill._loadFromProperties`。

## 怎麼驗證

### 已驗證
- 靜態比對：62 個技能都有 catalog 紀錄，建構參數與 SQL 逐欄比對，不一致只有上述 4 處（1 處已修正、1 處 S1 本身固定值、1 處既有缺口、1 處等價）。
- 本機 `javac -proc:none --release 17` 編譯 `src/main`＋`compile-stubs`（1177 檔），以及 `OtherClassSkillServer1ParityTest`、`BattleBoneBreakCatalogTest`、`ClientSessionBoneBreakIntegrationTest`：exit 0。**本機沒有執行任何測試**（依規定）。
- **PR #328 CI（run 42945256024，commit 831c5f97）全部通過**：`Server tests` 的 surefire 報告（artifact `server-test-evidence`）加總 **Tests run: 7303, Failures: 0, Errors: 0, Skipped: 117**；其中 `OtherClassSkillServer1ParityTest` 197 個、`BattleBoneBreakCatalogTest` 2 個、`ElfSkillServer1ParityTest` 210 個、`WizardSkillServer1ParityTest` 248 個全過。Server（Maven 建置）、Launcher、Build-Package、Semantic gap 也都通過。
- 注意：`ClientSessionBoneBreakIntegrationTest` 與 `NativeBoneBreakSourceAuditTest` 在 CI 執行 0 個（缺本機設定檔／1-伺服器 原始碼而整類跳過）。

### 推測
- 正式 `380_db.skills` 第 208 列與 `1-伺服器 db/380_db.sql` 相同（lawful=0）；本 PR 未連正式 DB。`NativeBoneBreakSourceAuditTest` 引用的兩份 SQL 也都是 0，所以 45 應是欄位看錯。
- 改成 0 後玩家可見的差別只有：施放骷髏毀壞不再增加正義值、不再送 `S_CHANGE_ALIGNMENT`（`ClientSession.java:46732` delta=0 直接返回）。

### 待驗證（交給 Codex／實機）
- 各處理器的封包順序、狀態圖示、HP／MP 實際入帳；機率類技能屬 T102。
- 立方 205／210／215／220 範圍效果、116／118 問答與傳送、100 提煉魔石、112 ×1.58、181／206／211 的減傷／回復數值、90 `SOLID_CARRIAGE_MODE` 裝備條件、覺醒每 4 秒扣 MP 與狀態限制。
- `ClientSessionBoneBreakIntegrationTest` 需要本機 `config/server.properties`（`LocalResources.assumeServerProperties`），CI 會整類跳過，本 PR 對它的修改**沒有在 CI 驗證到**。

## 不一致・未改

| 項目 | S1 | LiTo | 建議 |
|---|---|---|---|
| 103 正義值 −10 | `L1SkillUse.java:2047` 加 `skills.lawful` | `BlindSpell` 沒有 `lawfulDelta` | 與 T107 的 8 個技能一起另開 code 任務 |
| 107 遠程 +5 後備值 | `skills_buff_ability` 只有 DMG（近戰） | 離線後備 `rangedDamage=5`；正式環境讀表，結果相同 | 同 T107 §4.3，只影響離線 |
| 87／187／219 離線後備值 | `cfg` 2,3／5 次／1.05 | catalog 2,2／3 次＋20／1.5；正式環境讀 `cfg` | 不影響正式；T002 清重複常數時一併處理 |

## 產出路徑
- `docs/design/skills-others.md`、`docs/reports/issue-309.md`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/OtherClassSkillServer1ParityTest.java`
- `lito伺服器/LiTo381-Server/src/test/resources/server1/skills-others-62.tsv`
- `lito伺服器/LiTo381-Server/tools/skill-targeting/extract_others.py`
- 修改：`BattleSpellCatalog.java`、`BattleBoneBreakCatalogTest.java`、`ClientSessionBoneBreakIntegrationTest.java`、`combat-coverage-current.md`、`server-capability-matrix.md`

## 給下一棒的提示
- **正義值缺口**（T107 的 10／20／27／28／40／41／66／77 ＋本次 103）可以一次處理：`BlindSpell`、`AttackSpell` 等加 `lawfulDelta`，在處理器的扣費後呼叫 `applySpellLawfulDelta`；修好後把各 `LAWFUL_NOT_APPLIED` 清單清空。
- 包住 `AttackSpell` 的 record（ThunderGrab／Confusion／Smash／MindBreak／BoneBreak／ArmBreaker）沒有 `skillId`／`mpCost` 元件，用反射比對時要找 `skillId()` 方法並從內層 `AttackSpell` 取 MP；`OtherClassSkillServer1ParityTest.method()` 是現成寫法。
- 外控倍率的正式值來自 `1-伺服器/config/其他控制端/4-職業_技能設定表.properties`（`config/combat.properties:5`），catalog 常數只是離線後備；比對倍率時要看 `CombatRules.intField/doubleField` 的對應，不要只看 catalog。
- `CombatDatabase` 會用 SQL 覆蓋 `durationSeconds` 等 12 個欄位；S1 skillmode 自己決定時間的技能（例如 208）要在 `CombatDatabase.value` 加例外，否則正式環境會吃到 SQL 值。

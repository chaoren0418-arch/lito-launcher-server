# Issue #304 報告：T102 魔法傷害／MR／技能機率對照 1-伺服器（規格＋單元測試）

- 分支：`task/304`（code，第 1 次嘗試）
- 規格文件：`docs/design/formulas-magic.md`（39 項並列對照＋B12 結論＋呼叫點）
- 單元測試：`lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/MagicFormulaServer1ParityTest.java`（15 個測試方法）
- 差距矩陣：`docs/reports/server-capability-matrix.md` 第 37–38 列已更新

## 做了什麼

1. **找出 1-伺服器 的實際計算位置**（任務要求寫出）：魔法傷害與機率不在 `L1SkillUse` 本身，而在 `src/com/lineage/server/model/L1MagicPc.java`：
   - `calcProbabilityMagic`（`:139-330`）→ `calcProbability`（`:374-734`）→ `calcProbabilityWithConfig`（`:739-791`）；
   - `calcMagicDamage`（`:825-840`）→ `calcPcMagicDamage`（`:852-933`）／`calcNpcMagicDamage`（`:940-1001`）→ `calcMagicDiceDamage`（`:1008-1069`）→ `calcMrDefense`（`:1106-1126`）；
   - 共用：`L1MagicMode.java`（`getTargetSp:82-126`、`getTargetMr:132-166`、`calcAttrResistance:188-251`）、`L1AttackList.java:160-209`（MRDMG 表）；
   - 入口：`skill/L1SkillUse.java:2838`（`new L1Magic`）、`:2875`／`:3003`（傷害）、`:2886`／`:3137`／`:3831`／`:3842`（機率）；`skillmode/*` 只有 `ARM_BREAKER:21`、`BONE_BREAK:28`、`MIND_BREAK:31` 真正呼叫 `calcMagicDamage`。
2. **逐項對照**（39 項，`formulas-magic.md` §1–§3）：魔法傷害 M1–M18、MR R1–R8、機率 P1–P13。另外核對了 `L1PcOriginal`（原始 INT／WIS 四張表）、`BaseStatTable`（精神、智力成長）、7 個 `L1*ClassFeature.getMagicLevel`、`SkillsProbabilityTable`／`BossSkillImmuneTable` 載入、`ConfigPcDamage` 魔法倍率鍵。
3. **修改 LiTo Java 對齊 1-伺服器**（3 項行為修正＋2 項無行為差異的整理）：
   - **P9 MagicBonus**：1-伺服器 的 `角色_智力成長` 表與 `L1Character.getMagicBonus` 在 INT ≥ 44 都是 `INT−28`；LiTo 的 `BattleTurnUndead.magicBonus` 卻在 INT ≥ 40 封頂 15（另一份 `CombatKiringku.magicBonus` 才是對的）。改成表值並讓兩份合一。
   - **R7／P11 正義的守護 MR**：1-伺服器 `getTargetMr` 對正義值 10000～39999 的玩家目標加 MR +3／+6／+9；LiTo 只有魔法相消術有加。現在魔法傷害（`CombatMagicDamage.ordinary`、`finalBurn`）與機率（`CombatProbability.playerTarget`、非表列技能的 MR 參數）都加；相消術改傳基礎 MR 以免重複。
   - **M2／M3 邪惡的守護**：施法者正義值 −10000～−39999 時，1-伺服器 骰值與 SP 各 +1～+3；LiTo 沒有。加在 `ClientSession.sourceMagicDamageBeforeResistance`／`sourceNpcMagicBeforeResistance`。**刻意規則**：只在骰值 > 0 時加，避免把無傷害技能變成有傷害。
   - 整理：抽出 `BattleMagicResistance.skillLevel`（三處行內算式改呼叫它）；`SourceSkillControls.chance` 素質項改成與 1-伺服器 相同的逐項累加順序。
   - 正義／邪惡守護兩項只在載入 1-伺服器 規則（`CombatRules.loaded()`／控制表已載入）時生效，隔離測試與舊路徑不變。
4. **B12 結論**：生命的祝福 `NATURES_BLESSING_HEAL` 實際值是 **0**，LiTo 目前的 0 正確。1-伺服器 `ConfigSkill._loadFromProperties`（`config/ConfigSkill.java:93-134`）根本不讀這個鍵，只有 `_setDefaults`（`:207`）設 0；設定檔 `4-職業_技能設定表.properties:45` 的 `= 100` 是無效設定。舊報告「缺 +100」是看設定檔文字得出的，不成立。詳見 `formulas-magic.md` §4。
5. **呼叫點確認**：`CombatMagicDamage` 在 `ClientSession` 44 處＋其他 5 個類別、`CombatProbability` 在 `ClientSession` 約 190 處＋12 個類別，全部在 `src/main` 原始碼（§5）；LC B10 的「patch 接線」已不成立。

## 怎麼驗證

### 已驗證
- 本機 `javac`（JDK 17，`-proc:none`，只編譯不執行）合併 main（含 #303）後全量編譯 `src/main/java`＋`compile-stubs`＋新測試＋`BattleTurnUndeadTest`＋`PhysicalFormulaServer1ParityTest`（1177 檔，classpath：junit-jupiter-api 5.10.3、HikariCP 5.1.0、mysql-connector-j 8.0.33）：**exit 0**。輸出在 `scratch/t304_classes`（不進 git）。依規定本機不執行測試。
- 參考表唯讀抽取自 `D:\lineage開發\1-伺服器\db\380_db.sql`：
  - `角色_精神成長.魔防` 51 列 → LiTo 0 筆不一致；
  - `角色_智力成長.魔法加成` 127 列 → 本 PR 前 84 筆不一致（INT 44..127），本 PR 後 0 筆；
  - `skills.skill_level`：181 筆非佔位列全等於 `((id−1)>>3)+1`（`py -3 -I scratch/t304_skill_level.py`，36 筆不符全是名稱 `none` 的空列）；
  - `skills_負面技能控制` 34 列、`skills_負面boss免疫` 40 列原樣寫進測試。
- 原始素質四張表（`L1PcOriginal`）、職業基礎 MR、7 職業魔法等級：逐行讀過，與 LiTo 相同，並由測試全值域列舉。
- 既有測試相容性推論：`BattleTurnUndeadTest` 只測到 INT 40（仍為 15）；守護相關變更都在 `CombatRules.loaded()`／控制表載入之後才生效；來源稽核測試沒有比對被改到的字串（已搜尋 `magicDamage(n3`、`unusedConfiguredCancellationFallback`、`intelligence>43`、`guardianMr` 等）。
- **CI**：待 PR 的 GitHub Actions 結果（push 後補記）。

### 推測
- M11（`SKD3` 技能減免清單）、M16（寵物倍率、聖結界）、M17（「傷害為 0」狀態清單）、R8（各 buff MR）：結構一致，未逐值測（屬 T107～T109 技能範圍）。
- LiTo 註解裡的「Source／retained」很可能就是 1-伺服器 的較舊版本：表名、鍵名、多數公式逐字相同；差異（INT≥44、守護）是 1-伺服器 後來的擴充或 LiTo 移植時漏掉的分支。

### 待驗證（交給 Codex／使用者）
- 實機：正義值 ≥ 10000 的角色被魔法打的傷害與被控機率略降；正義值 ≤ −10000 的角色魔法傷害略升；INT ≥ 44 的角色施放弱化術、集體緩速、魔力奪取等成功率上升。
- 風／生命／形象魔眼的 +1 SP 是否已在 LiTo 的 `spellPowerModifier`（M3）。
- 千分比迴避（`get_evasion`）在競技場是否有來源（P1，與 T101 H13 一起）。

## 未做

- NPC 施法（`L1MagicNpc`）、治癒量（`calcHealing`、`getHealDiceBonus`）、各技能效果本體：不在本任務，交 T107～T109。
- 不改 `BattleSpellCatalog` 的骰值常數：正式環境由 `CombatDatabase` 直接讀 `skills` 表覆蓋（`CombatDatabase.java:62-75,143-152`），常數只是後備值。
- 本機沒查客戶端資源（本任務不需要客戶端資源）。

## 產出路徑

- `docs/design/formulas-magic.md`（新增）
- `docs/reports/issue-304.md`（本檔）
- `docs/reports/server-capability-matrix.md`（第 37–38 列）
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`：`BattleTurnUndead.java`、`CombatKiringku.java`、`CombatPlayerMagic.java`、`CombatMagicDamage.java`、`CombatProbability.java`、`NativeCancellationProbability.java`、`BattleMagicResistance.java`、`SourceSkillControls.java`、`ClientSession.java`（兩個方法＋一處 skill level）
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/MagicFormulaServer1ParityTest.java`（新增）
- `scratch/t304_*`（抽表腳本與中間檔，不進 git）

## 給下一棒的提示

- 若 CI 有既有自測因「正義值 ≥ 10000 的目標」或「正義值 ≤ −10000 的施法者」傷害／機率改變而失敗，那是預期中的行為改變（1-伺服器 `L1MagicMode.getTargetMr`、`calcMagicDiceDamage` 都有），應更新自測期望值，不要把守護項拿掉。
- T107～T109（技能行為）可直接沿用本測試的 `REF_ROWS`／`REF_BOSS`／`refConfiguredChance`；表控 34 個技能的機率公式已鎖定，技能測試只需驗證「呼叫時傳入的等級、MR、耐性」是否正確。
- 若廠商要生命的祝福 +100：這是新設計（1-伺服器 也沒有），要改 `CombatRules.java:111-113` 並同步更新 `NativeNaturesBlessingSourceAuditTest` 與本測試的 B12 斷言。
- T101（#303/PR #316）也改了 `server-capability-matrix.md` 第 32–36 列；兩個 PR 先後合併時，第 37–38 列相鄰，可能要手動解一次衝突（兩邊內容都保留）。

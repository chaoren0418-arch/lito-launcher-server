# Issue #307 報告：T107 技能行為驗證——共通魔法／法師（79 個）

- 分支：`task/307`（code，第 1 次嘗試）
- 規格／對照：`docs/design/skills-wizard.md`（79 列逐技能表＋共通流程 F1–F12）
- 測試：`lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/WizardSkillServer1ParityTest.java`（新）
- 輸入表：`lito伺服器/LiTo381-Server/src/test/resources/server1/skills-wizard-79.tsv`（79 列 × 16 欄）
- 擷取工具：`lito伺服器/LiTo381-Server/tools/skill-targeting/extract_wizard.py`
- 覆蓋表：`lito伺服器/LiTo381-Server/docs/maintenance/combat-coverage-current.md` 第 1～80 列「完整行為」欄已更新
- 差距矩陣：`docs/reports/server-capability-matrix.md` 技能列已更新

## 做了什麼

1. **輸入資料**：從 `1-伺服器 db/380_db.sql` 唯讀擷取 `skills` 第 1～80 列（去掉 24 保留槽）的 mpConsume、hpConsume、itemConsumeId／Count、reuseDelay、buffDuration、damage_value／dice／dice_count、probability_value／dice、attr、type、lawful。
2. **逐技能對照**（`skills-wizard.md` §3）：每列寫 S1 效果類別（`L1SkillMode.java:164-178` 的 SkillMode 註冊或 `L1SkillUse.runSkill` 分支行號）、LiTo catalog 紀錄行與主要處理器（`ClientSession` 方法行），以及判定。
3. **共通流程**（§1）：查清楚正式環境的數值從哪裡來（`CombatDatabase` 讀同一張 `skills` 表覆蓋 catalog），以及 MP 消耗的實際路徑——所有處理器都經 `NativeSkillConsumptionBridges` 呼叫 `NativeSourceSkillConsumptionMath.mpCost`，公式與 S1 `isHPMPConsume`（`L1SkillUse.java:1869-1971`）相同；catalog 各 record 自己的 `effectiveMpCost` 回傳值被丟棄（其中 8 個與 S1 不同，見 §4.4，不影響行為）。
4. **單元測試**（`WizardSkillServer1ParityTest`，共 11 個單項＋3 組 79 列參數化）：
   - 消耗：catalog 的 MP／HP／材料／冷卻／期限與 SQL 逐列相等；`mpCost` 對 INT 1..40 × 原始減免 0..2 × 頭盔有無，79 個技能逐值等於另寫的 S1 公式；原始 INT 減免 7 職業 × 0..30；「最低 1 在原始減免之前」邊界。
   - 效果數值：傷害骰／治癒骰、屬性、機率骰、機率值、正義值；BUFF 能力值後備與 `skills_buff_ability` 相同；77 正義值門檻 500。
   - 期限：33 麻痺 5000／10000 ms（`skillmode/CURSE_PARALYZE.java:38-44`）、11 毒 3000 ms／5／30 秒（`L1SkillUse.java:3105`、`L1DamagePoison.java:147`）、43 到期邊界。
   - BUFF 互斥：加速／緩速互相抵銷（`skillmode/HASTE.java:31-61`、`L1SkillUse.java:3050-3091`、`:3753-3777`）、43／54 互蓋、52 與勇水互蓋、`CombatBuffLeases.GROUPS` 與 `ORIGINAL_REPEATED_SKILLS`（`:2520-2527`）同組 ⇔ 同組。
   - `lawfulGapListIsExact`：釘住「SQL lawful≠0 但 LiTo 沒套用」的 8 個技能，清單過期或新增缺口都會失敗。
5. **改 Java（不一致處）**：
   - **47 弱化術**：`BattleSpellCatalog` `lawfulDelta` 0 → −10。S1 `useConsume`（`L1SkillUse.java:2047`）每次施放加 `skills.lawful`（−10）；LiTo 處理器 `nativeMaterialWeaknessBody` 本來就會呼叫 `applySpellLawfulDelta`（`ClientSession.java:69276`），只是值是 0。56 疾病術原本就是 −10。
   - **18 起死回生術**：catalog `probabilityValue` 1 → 0（SQL 為 0，1 是 attr 欄）。正式環境本來就由 DB 覆蓋，這是後備值修正。
   - 同步改 `BattleSpellCatalogTest` 兩個斷言（`:667`、`:1303`）。

## 怎麼驗證

### 已驗證
- 本機靜態比對（`scratch/t307/compare.py`，未進 git）：78 個有 catalog 紀錄的技能，建構子參數的 mpCost／hpCost／reuse／duration／item 與 SQL 0 筆不一致（33 沒有 durationSeconds 欄位，另測）；傷害骰、治癒骰、機率骰全部一致；不一致只有 18 probabilityValue、47 lawfulDelta（已修正）與 8 個正義值缺口。
- 本機 `javac -proc:none --release 17` 編譯 `src/main`＋`compile-stubs`，以及 `WizardSkillServer1ParityTest`、`BattleSpellCatalogTest`：exit 0。**本機沒有執行任何測試**（依規定）。
- CI 結果：見下方「CI」段（push 後補上）。

### 推測
- 正式 `380_db.skills` 與 `1-伺服器 db/380_db.sql` 這 16 欄相同（T106 已驗 6 欄一致；本 PR 未連正式 DB）。
- 29 與 76 在 S1 可兩個效果並存、各自到期，LiTo 只記一個緩速來源；同為速度 2，推測玩家看不出差別。
- 12 擬似魔法武器：正式環境（DB 載入）弓傷害為 0，與 S1 相同；只有離線後備值會 +2。

### 待驗證（交給 Codex／實機）
- 各處理器的封包順序、狀態圖示、HP／MP 實際入帳。
- 10／28 吸血、39 吸魔量、58 火牢每跳傷害、44 相消清單、緩速對「加速道具／強化勇水」目標免疫（`L1SkillUse.java:3053-3061`）。
- 47 修正後，實機施放弱化術會多一個 `S_CHANGE_ALIGNMENT`（正義值 −10），與 56 相同。

## 不一致・未改（需要決定或另開任務）

| 項目 | S1 | LiTo | 建議 |
|---|---|---|---|
| 正義值：10、20、27、28、40、41、66、77 | 每次施放加 `skills.lawful`（`L1SkillUse.java:2047`） | record 沒有 `lawfulDelta`，處理器不呼叫 `applySpellLawfulDelta` | 另開 code 任務：5 個 record 加欄位＋約 15 個 BATTLE／WORLD 處理器加呼叫；或由 T100 裁定競技場要不要正義值變動 |
| 76 集體緩速術自施 | `runSkill:2779` 返回但 `:1140` 仍扣 MP／正義值、播動作 → 扣費不生效 | 仍對範圍內其他人生效（T106 G05） | 維持 T106 的使用者決定項；本 PR 只補上扣費時序 |
| catalog 重複的 `effectiveMpCost` | — | 31、51、56、60、61、75、77、3 的公式與 S1 不同，但執行期不使用 | 之後刪除，避免誤用 |

## 給下一棒的提示

- **T108（妖精）／T109**：直接複製 `WizardSkillServer1ParityTest` 的做法——改 `extract_wizard.py` 的 id 範圍產生新 tsv，參數化比對同一組欄位；MP 不必再測（`runtimeMpCostMatchesIsHpMpConsume` 已涵蓋公式，87..91 騎士段也在 `refMp` 裡，只要把列加進去）。
- 數值的真正來源是 DB（`CombatDatabase.java:12-15` 列出的 12 個欄位）；`lawfulDelta`、`braveMode`、能力值後備等**不在** DB 綁定清單，只能靠 catalog 常數，最容易出錯，優先比對。
- `skills_buff_ability`（`db`）在 DB 載入時由 `SourceSkillControls.ability` 讀取；妖精的 159／168 在 LiTo 是獨立狀態類別（`BattleEarthBlessStatus`、`BattleIronSkinStatus`），不在 `CombatBuffLeases.GROUPS`，T108 要另外比對 `ORIGINAL_REPEATED_SKILLS` 的 {3,99,151,159,168} 與 {148,149,155,156,163,166}。`SkillCoexistSet` 在 1-伺服器 預設是 1（`server_event` 116），原廠覆蓋群組平常不生效。
- 處理器方法名稱可從本機收尾矩陣 `tools/combat-continuation/integration-lifecycle/build/development-closeout-progress/20261028T222200402677Z/skills.tsv`（未進 git）的 `current_method_names_located` 欄取得，再在 `ClientSession.java` 找宣告行。

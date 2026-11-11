# Issue #308 報告：T108 技能行為驗證——妖精（40 個）

- 分支：`task/308`（code，第 1 次嘗試）
- 規格／對照：`docs/design/skills-elf.md`（共通流程 E1–E12、屬性閘門 13 列、逐技能 40 列）
- 測試：`lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/ElfSkillServer1ParityTest.java`（新）
- 輸入表：`lito伺服器/LiTo381-Server/src/test/resources/server1/skills-elf-40.tsv`（40 列 × 16 欄）
- 擷取工具：`lito伺服器/LiTo381-Server/tools/skill-targeting/extract_elf.py`
- 新增程式：`lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/SourceElfAttributeGate.java`
- 覆蓋表：`lito伺服器/LiTo381-Server/docs/maintenance/combat-coverage-current.md` 第 129～176 列（40 個）「完整行為」欄已更新
- 差距矩陣：`docs/reports/server-capability-matrix.md` 技能列已更新

## 做了什麼

1. **輸入資料**：從 `1-伺服器 db/380_db.sql` 唯讀擷取 `skills` 第 129～176 列（去掉 135、136、139～144 保留槽）的 16 欄。妖精 40 列的 `lawful` 全為 0，所以沒有 T107 那種正義值缺口。
2. **靜態比對**（`scratch/t308/compare.py`，未進 git）：解析 catalog 的 record 欄位與 40 個建構參數，逐欄對 SQL。MP／HP／冷卻／期限／材料（精靈玉 40319）／治癒骰全部一致；唯一不一致是 **159、160 的 `attribute` 寫成 0**（SQL 為 1 地、4 水）。
3. **施放屬性檢查**：S1 `L1SkillUse.isAttrAgrees`（`L1SkillUse.java:1850-1863`，由 `:554-556` 在扣費前呼叫）規定精靈魔法 `attr≠0` 時 ElfAttr 必須包含該屬性。逐一追 LiTo 13 個 `attr≠0` 技能的實際施放路徑，發現 **157、159、160、170 沒有檢查**（另外 9 個既有檢查都在呼叫鏈上，位置列在 `skills-elf.md` §2）。
4. **改 Java**：
   - 新增 `SourceElfAttributeGate.agrees`（純函式，依 S1 另寫：技能等級 17..22、attr≠0、ElfAttr 位元、GM 例外）。
   - `ClientSession.elfAttributeRejects`：不符時只記 log、不扣費、不送封包（比照既有 Storm Shot）。
   - 呼叫點：`useEarthBlessSpell`（159）、`useAquaProtectSpell`（160）、`nativeMaterialWaterLifeBody`（170）、`useBindSpell`／`useClassicWorldBindSpell`（157）。
   - `BattleSpellCatalog`：159 `attribute` 0 → `ATTRIBUTE_EARTH`、160 0 → `ATTRIBUTE_WATER`（含 record 建構子檢查）；新增 `EARTH_BIND_ATTRIBUTE`（BindSpell 沒有 attribute 欄位）。
   - `ClientSessionNativeTournamentEconomyTest` 的 159／160 整合測試施法者補設 ElfAttr（地／水），否則會被新閘門擋下。只改這兩處，保留原檔混合換行。
5. **單元測試**（`ElfSkillServer1ParityTest`，10 個單項＋5 組 40 列參數化）：
   - 消耗、冷卻、期限、材料、治癒骰、機率骰欄位、正義值逐列對 SQL；MP 公式（INT 1..40 × 減免 0..2 × 頭盔）；妖精沒有原始 INT 減免。
   - 屬性：catalog `attribute`／`requiredElfAttribute` 等於 SQL；`SourceElfAttributeGate` 對 40 個技能 × ElfAttr 0..15 × GM 有無等於另寫的 S1 公式；非精靈等級技能不受限制；`everyAttributeSkillHasACastTimeGate` 釘住「SQL attr≠0 的 13 個技能 ⇔ 施放路徑上有 ElfAttr 檢查」。
   - 效果數值：`skills_buff_ability` 10 列＋129 MR+10、138 四屬性 +10、147 +50；130 MP+2、146 MP+20（`config/其他控制端/4-職業_技能設定表.properties:60`）；132 三次攻擊、133 −50。
   - BUFF 互斥：150 勇敢速度 4 與神聖疾走同組覆蓋；148／149／155／156／163／166／159／168 各有獨立狀態欄位（S1 預設共存）。

## 怎麼驗證

### 已驗證
- 靜態比對：40 個技能的 catalog 建構參數與 SQL 逐欄比對，不一致只有 159／160 attribute（已修正）。
- 本機 `javac -proc:none --release 17` 編譯 `src/main`＋`compile-stubs`（1177 檔），以及 `ElfSkillServer1ParityTest`、`ClientSessionNativeTournamentEconomyTest`、`WizardSkillServer1ParityTest`、`BattleSpellCatalogTest`：exit 0。**本機沒有執行任何測試**（依規定）。
- CI 結果見 PR（推送後補）。

### 推測
- 正式 `380_db.skills` 的 `attr` 欄與 `1-伺服器 db/380_db.sql` 相同（本 PR 未連正式 DB；`attr` 不在 `CombatDatabase` 覆蓋清單，正式環境也是用 catalog 常數）。
- 新閘門的影響面小：一般角色只能從對應屬性學到這些技能（`PcLvSkillList`），只有屬性重置或資料不一致時才會被擋。
- 檢查順序（屬性先於變身／沉默）與 S1 `isNormalSkillUsable` 一致；只影響「同時有多個拒絕理由時回哪個訊息」。

### 待驗證（交給 Codex／實機）
- 屬性不符被擋下時，客戶端是否卡住施法動作（LiTo 不送封包，S1 也不送）。
- 各處理器的封包順序、狀態圖示、HP／MP 實際入帳；154／162 依屬性選精靈、165 復活問答、治癒倍率組合（`L1SkillUse.java:2918-2924`）。
- `ClientSessionNativeTournamentEconomyTest` 需要本機 `config/server.properties`（`LocalResources.assumeServerProperties`），CI 可能跳過，159／160 的整合流程要在有設定檔的環境再跑。

## 不一致・未改

| 項目 | S1 | LiTo | 建議 |
|---|---|---|---|
| `SkillCoexistSet=0` 時妖精 BUFF 群組 | {148,149,155,156,163,166}、{3,99,151,159,168} 互刪（`L1SkillUse.java:2520-2537`） | 8 個妖精 BUFF 永遠獨立；預設共存時與 S1 相同 | 由 T100 決定是否要支援非共存模式 |
| 機率數值與 `ConfigSkill` 倍率 | `calcProbabilityMagic`、`MIRROR`、`SOUL_OF_FLAME_DAMAGE`、`STRIKER_DMG` | catalog 常數 | 機率屬 T102；倍率未逐值比對，可併入 T109 或另開 |

## 給下一棒的提示

- **T109**：照抄 `ElfSkillServer1ParityTest`——改 `extract_elf.py` 的 id 範圍產生 tsv；先跑 `scratch/t308/compare.py` 的做法（解析 record 元件名稱＋建構參數）找常數不一致，比讀註解快很多。
- **`attr` 不在 DB 覆蓋清單**（`CombatDatabase.java:12-15`），而且屬性檢查散在各處理器；龍騎士／幻術師若有 attr≠0 的技能，要逐一確認施放路徑。`SourceElfAttributeGate` 只處理等級 17..22（S1 只對精靈魔法檢查）。
- 處理器很多是 `useXxxSpell` → `NativeXxx` 類別 → `ClientSession.nativeMaterialXxxBody`；找屬性／前置檢查要追到 `nativeMaterial*Body`。`scratch/t308/handlers.py` 可列出方法宣告行。
- 改到混合換行的測試檔時不要用整檔改寫的工具，會把 CRLF 正規化成一大片 diff。

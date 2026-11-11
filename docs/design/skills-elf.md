# 妖精 40 個技能：1-伺服器 ↔ LiTo 行為對照（T108，Issue #308）

- 參考（唯讀）：`D:\lineage開發\1-伺服器`。引用寫「相對路徑：行號」。縮寫：`U` = `src/com/lineage/server/model/skill/L1SkillUse.java`，`M` = `src/com/lineage/server/model/skill/L1SkillMode.java`，`sm/` = `src/com/lineage/server/model/skill/skillmode/`，`db` = `db/380_db.sql`。
- LiTo：路徑相對於 `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`。`C` = `BattleSpellCatalog.java`（本分支行號，catalog 紀錄的 `new XxxSpell(` 那一行），`CS` = `ClientSession.java`（方法宣告行）。
- 範圍：`db` `skills` 第 129～176 列去掉保留槽 135、136、139～144，共 40 個（`lito伺服器/LiTo381-Server/docs/maintenance/combat-coverage-current.md:139-186`）。
- 做法與 T107 相同（`docs/design/skills-wizard.md`）：目標判定由 T106 負責（`docs/design/skill-targeting.md`），機率公式由 T102 負責（`docs/design/formulas-magic.md`），本文不重複。
- 測試：`lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/ElfSkillServer1ParityTest.java`；輸入表 `src/test/resources/server1/skills-elf-40.tsv`（`tools/skill-targeting/extract_elf.py` 從 `db` 唯讀擷取，16 欄與 T107 相同）。
- 判定：**一致**＝本文列出的面向逐值相等（有單元測試）；**已修正**＝本 PR 改 LiTo；**不一致・未改**＝有差異但本 PR 不改（理由寫在 §4）；效果本體的封包與實機行為一律**待驗證**。

## 1. 共通流程（40 個技能）

| # | 面向 | 1-伺服器 | LiTo | 判定 | 測試 |
|---|---|---|---|---|---|
| E1 | 數值來源 | `db` `skills` | 正式環境 `CombatDatabase.java:62-75` 讀同一張表覆蓋 12 個欄位（`:12-15`）；**`attr` 不在覆蓋清單**，只能靠 catalog 常數 | 消耗／冷卻／期限／材料後備值與 `db` 逐欄相等（40 個都有 catalog 紀錄） | `consumptionCooldownAndDurationMatchSource` |
| E2 | MP 消耗 | `U:1869-1971`：INT 減免只到技能 80、騎士 87–91；頭盔表沒有妖精技能；SQL MP>0 時最低 1 | `NativeSourceSkillConsumptionMath.mpCost` | 一致（INT 1..40 × 減免 0..2 × 頭盔，40 個逐值） | `runtimeMpCostMatchesIsHpMpConsume` |
| E3 | 原始 INT 減免 | `src/com/lineage/server/model/Instance/L1PcOriginal.java:1012-1060`：妖精（type 2）沒有 | `NativeSourceSkillConsumptionMath.originalReduction` | 一致（0..30 全為 0） | `elfHasNoOriginalIntelligenceReduction` |
| E4 | HP 消耗 | 130 HP 8、146 HP 40 | `BodyToMindSpell.hpCost` | 一致 | `mindConversionManaGainMatchesSkillModes` |
| E5 | 材料 | 精靈玉 40319 × 1／2／3／4／5／8 | `InventoryCatalog.SPIRIT_CRYSTAL`（40319） | 一致（23 個有材料的技能 ID／數量逐列相等） | `consumptionCooldownAndDurationMatchSource` |
| E6 | 正義值 | `U:2047` 加 `skills.lawful`；妖精 40 列 lawful 全為 0 | 無 `lawfulDelta` 或為 0 | 一致（妖精沒有 T107 那種正義值缺口） | `healingProbabilityAndLawfulMatchSource` |
| E7 | **施放屬性** | `U:1850-1863` `isAttrAgrees`：技能等級 17..22 且 `attr≠0` 時，`ElfAttr & attr == 0` 不能施放（GM 例外）；由 `isNormalSkillUsable`（`U:554-556`）在扣費前呼叫 | 各處理器各自檢查 `elfAttribute & attribute`；本 PR 新增 `SourceElfAttributeGate` 與 `CS:15265 elfAttributeRejects` | **已修正**：13 個 `attr≠0` 技能中，157、159、160、170 原本沒有檢查（§3.1） | `castAttributeMatchesSource`、`elfAttributeGateMatchesIsAttrAgrees`、`everyAttributeSkillHasACastTimeGate` |
| E8 | 147 屬性防禦 ElfAttr=0 | `U:559-562`：ElfAttr 為 0 時送訊息 280 並拒絕 | `nativeMaterialElementalProtectionBody` 檢查 `elfAttribute != 0` | 一致（既有） | — |
| E9 | BUFF 互斥（必定） | `U:2512-2514` `ALWAYS_REPEATED_SKILLS`：{52 神聖疾走, 101, 150 風之疾走, 勇水×3, 186} | `MovementBuffSpell.braveMode=4`，`BattleTimedStatus.activateBrave` 單一欄位覆蓋 | 一致 | `windWalkUsesBraveMode4LikeHolyWalk` |
| E10 | BUFF 互斥（原廠覆蓋） | `U:2520-2527`：{148,149,155,156,163,166}、{3,99,151,159,168}，只在 `SkillCoexistSet=0` 時生效；`db` `server_event` 116 預設 1（共存） | 148／149／155／156／163／166／159／168 各有獨立狀態欄位（`CS` 建構子 `fireWeaponStatus`…`ironSkinStatus`）；151 走 `CombatBuffLeases.GROUPS` | 預設（共存）一致；`SkillCoexistSet=0` 時不一致（§4.1） | `elfWeaponAndArmorBuffsHaveIndependentLeases`；151 由 T107 `buffReplacementGroupsMatchOriginalRepeatedSkills` |
| E11 | BUFF 能力值 | `db` `skills_buff_ability`：137 WIS+3；148 DMG+4；149 BOW_HIT+6；151 AC−6；155 DMG+4；156 BOW_HIT+2 BOW_DMG+3；159 AC−7；163 HIT+3 DMG+6；166 BOW_HIT−1 BOW_DMG+5；168 AC−10。另 `U:3558` 129 MR+10、`U:3578-3581` 138 四屬性 +10、`U:3592-3603` 147 每個屬性位元 +50 | catalog 後備值；DB 載入時 `SourceSkillControls.ability` | 一致（逐項） | `buffAbilityFallbacksMatchSource` |
| E12 | 期限 | `buffDuration × 1000`（`U:2060-2161 addMagicList`） | 各 `Battle*Status.activate(now, durationSeconds)` | 一致（數值） | `consumptionCooldownAndDurationMatchSource`、`windWalkUsesBraveMode4LikeHolyWalk` |

## 2. 屬性閘門逐項（E7 細節）

SQL `attr≠0` 的 13 個技能。「位置」是 LiTo 實際做 `elfAttribute` 檢查的方法；`everyAttributeSkillHasACastTimeGate` 釘住這張表（清單必須正好等於 SQL `attr≠0` 的集合，且每個方法宣告後 1500 字內要有 `elfAttribute`）。

| ID | 名稱 | attr | LiTo 檢查位置 | 判定 |
|---|---|---|---|---|
| 157 | 大地屏障 | 1 地 | `CS:44384 useBindSpell`、`CS:44379 useClassicWorldBindSpell`（`BindSpell` 沒有 attribute 欄位，改用 `C` `EARTH_BIND_ATTRIBUTE`） | **已修正**（原本沒有檢查） |
| 159 | 大地的祝福 | 1 地 | `CS:42060 useEarthBlessSpell`；catalog `attribute` 0 → `ATTRIBUTE_EARTH`（`C:936`） | **已修正** |
| 160 | 水之防護 | 4 水 | `CS:43191 useAquaProtectSpell`；catalog `attribute` 0 → `ATTRIBUTE_WATER`（`C:1221`） | **已修正** |
| 163 | 烈炎武器 | 2 火 | `CS:90996 nativeSourceFireWeaponOriginalBody` | 一致（既有） |
| 164 | 生命的祝福 | 4 水 | `CS:91188 nativeSourceHealingOriginalNature` | 一致（既有） |
| 165 | 生命呼喚 | 4 水 | `CS:70708 nativeMaterialCallNatureBody`（`requiredElfAttribute`） | 一致（既有） |
| 167 | 風之枷鎖 | 8 風 | `CS:61775 nativeMaterialAreaWindBody` | 一致（既有） |
| 170 | 水之元氣 | 4 水 | `CS:67810 nativeMaterialWaterLifeBody`（catalog 本來就是 4，但處理器沒用） | **已修正** |
| 171 | 屬性之火 | 2 火 | `CS:58139 nativeMaterialPhysicalProcBuffBody` | 一致（既有） |
| 172 | 暴風疾走 | 8 風 | `CS:37600 useOtherNoEffectSpell` | 一致（既有） |
| 173 | 污濁之水 | 4 水 | `CS:83348 nativeMaterialTargetNegativeBody`；寵物路徑 `CombatNpcDebuffs.java:56` | 一致（既有） |
| 174 | 精準射擊 | 8 風 | 同 173 | 一致（既有） |
| 175 | 烈焰之魂 | 2 火 | 同 171 | 一致（既有） |

### 2.1 新增閘門的行為
- 拒絕方式比照既有 Storm Shot（`CS:41961`）：只記 log、不扣 MP／材料、不送封包。S1 `isNormalSkillUsable` 回傳 false 時同樣不扣費（`U:554-556`，扣費在 `runSkill` 之後的 `U:1123-1142`）。
- 159／160／170 的檢查放在「階段為 BATTLE／WORLD」之後、變身／沉默／負重檢查之前，與 S1 `isNormalSkillUsable` 先做 `isAttrAgrees` 的順序相同；157 放在入口（兩個階段各一個入口）。
- GM：`isAdministrator()` 例外，與 S1 `!_player.isGm()` 相同。

## 3. 逐技能對照

「S1」欄寫 `M` 的註冊行／`sm/` 檔或 `U` 的分支行；「LiTo」欄寫 catalog 紀錄行與主要處理器。所有列的消耗／冷卻／期限／材料／治癒骰／機率骰／正義值已由 §1 的參數化測試逐值比對。

| ID | 名稱 | S1 效果類別／行號 | LiTo 實作位置 | 判定 |
|---|---|---|---|---|
| 129 | 魔法防禦 | `U:3552-3559` MR+10 | `C:611` MagicResistanceSpell；`CS:38027` | 一致（MR 10、1200 秒、精靈玉×1） |
| 130 | 心靈轉換 | `M:192` `sm/BODY_TO_MIND.java:22` MP+2 | `C:627` BodyToMindSpell；`CS:38034` | 一致（HP 8、MP+2） |
| 131 | 世界樹的呼喚 | 預設（傳送） | `C:659` WorldTreeCallSpell；`CS:38256` | 數值一致；正式 LiTo.bin 沒有地圖 4，傳送本體被 catalog 註解列為停用（不在本文） |
| 132 | 三重矢 | `M:194` `sm/TRIPLE_ARROW.java:46-47` 三次 `onAction` | `C:675` TripleArrowSpell；`CS:38085` | 一致（3 次、MP 11、100 ms） |
| 133 | 弱化屬性 | `M:191` `sm/ELEMENTAL_FALL_DOWN.java:26-34` 依 ElfAttr −50 | `C:693` ElementalFallDownSpell；`CS:41451` | 一致（−50、32 秒）；機率屬 T102 |
| 134 | 鏡反射 | `U:2393`（與魔法屏障同類） | `C:733` CounterMirrorSpell；`CS:41570` | 一致（16 秒、1000 ms、精靈玉×1）；反射機率 `ConfigSkill.MIRROR` **待驗證** |
| 137 | 淨化精神 | `U:3561-3570`；WIS+3 | `C:749` ClearMindSpell；`CS:41574` | 一致 |
| 138 | 屬性防禦 | `U:3572-3582` 四屬性 +10 | `C:765` ResistElementalSpell；`CS:41578` | 一致 |
| 145 | 釋放元素 | `U:3889-3900`（召喚獸 `returnToNature`，否則訊息 79） | `C:713` ReturnToNatureSpell；`CS:41463` | 一致（精靈玉×2）；機率屬 T102 |
| 146 | 魂體轉換 | `M:193` `sm/BLOODY_SOUL.java:23`；`config/其他控制端/4-職業_技能設定表.properties:60` `BLOODY_SOULADDMP = 20` | `C:640` BodyToMindSpell；`CS:38034` | 一致（HP 40、MP+20、1200 ms） |
| 147 | 單屬性防禦 | `U:559-562`、`U:3584-3603` | `C:781` ElementalProtectionSpell；`CS:41582` | 一致（每個屬性位元 +50、64 秒） |
| 148 | 火焰武器 | `U:3646`；DMG+4 | `C:799` FireWeaponSpell；`CS:41586` | 一致 |
| 149 | 風之神射 | `U:3683`；BOW_HIT+6 | `C:825` WindShotSpell；`CS:41590` | 一致 |
| 150 | 風之疾走 | `U:3779-3786` 勇敢速度 4 | `C:1372` MovementBuffSpell；`CS:37534` | 一致（E9） |
| 151 | 大地防護 | `U:3615`；AC−6 | `C:328` ShieldSpell；`CS:32141` | 一致 |
| 152 | 地面障礙 | `U:3050-3091`（與緩速同分支） | `C:2496` EntangleSpell；`CS:41455` | 一致（64 秒、精靈玉×1）；緩速互斥見 T107 §2 |
| 153 | 魔法消除 | `U:2877-2891`、`U:4196` | `C:2517` EraseMagicSpell；`CS:41459` | 一致（32 秒、600 ms）；機率屬 T102 |
| 154 | 召喚屬性精靈 | `M:198` `sm/LESSER_ELEMENTAL.java` | `C:2535` ElementalSummonSpell；`CS:46577` | 一致（精靈玉×2）；依 ElfAttr 選精靈（`C` `:10757-10760`）**待驗證**實機 |
| 155 | 烈炎氣息 | `U:3658`；DMG+4（隊伍） | `C:843` FireBlessSpell；`CS:41594` | 一致 |
| 156 | 暴風之眼 | `U:3695`；BOW_HIT+2 BOW_DMG+3（隊伍） | `C:861` StormEyeSpell；`CS:41775` | 一致 |
| 157 | 大地屏障 | `U:3140-3153`（凍結）、`U:1493` | `C:2648` BindSpell；`CS:44384`、`CS:44379`、`CS:63726` | **已修正**：補屬性閘門（§2） |
| 158 | 生命之泉 | `U:3025-3029`（消耗水之元氣） | `C:916` NaturesTouchSpell；`CS:43022` | 一致（320 秒） |
| 159 | 大地的祝福 | `U:3541-3550`；AC−7 | `C:936` EarthBlessSpell；`CS:42060` | **已修正**：attribute 0 → 1，補屬性閘門 |
| 160 | 水之防護 | `M:196` `sm/AQUA_PROTECTER.java:22` | `C:1221` AquaProtectSpell；`CS:43191` | **已修正**：attribute 0 → 4，補屬性閘門 |
| 161 | 封印禁地 | 預設 | `C:1238` AreaSilenceSpell；`CS:43360` | 一致（9000 ms、精靈玉×8） |
| 162 | 召喚強力屬性精靈 | `M:197` `sm/GREATER_ELEMENTAL.java` | `C:2547` ElementalSummonSpell；`CS:46577` | 一致（精靈玉×4） |
| 163 | 烈炎武器 | `U:3670`；HIT+3 DMG+6 | `C:810` FireWeaponSpell；`CS:41586` | 一致 |
| 164 | 生命的祝福 | TYPE_HEAL `U:2938`、`U:3026` | `C:295` NaturesBlessingSpell；`CS:46986` | 一致（治癒骰 (10+MB)d12、300 ms） |
| 165 | 生命呼喚 | `M:190` `sm/CALL_OF_NATURE.java` | `C:2449` ResurrectionSpell；`CS:46786` | 一致（精靈玉×1、水屬性） |
| 166 | 暴風神射 | `U:3708`；BOW_HIT−1 BOW_DMG+5 | `C:879` StormShotSpell；`CS:41961` | 一致 |
| 167 | 風之枷鎖 | `M:199` `sm/WIND_SHACKLE.java:31,46` | `C:898` WindShackleSpell；`CS:43390` | 一致（16 秒、100 ms） |
| 168 | 鋼鐵防護 | `U:3604-3613`；AC−10 | `C:955` IronSkinSpell；`CS:42238` | 一致 |
| 169 | 體能激發 | 預設 | `C:1162` ExoticVitalizeSpell；`CS:39348` | 一致（960 秒、精靈玉×1） |
| 170 | 水之元氣 | `U:2918`（治癒 ×2）、`U:3029` | `C:1201` WaterLifeSpell；`CS:43187` → `CS:67810` | **已修正**：補屬性閘門 |
| 171 | 屬性之火 | 預設；傷害加成在 `L1AttackPc` | `C:1410` PhysicalProcBuffSpell；`CS:37593` | 一致（192 秒） |
| 172 | 暴風疾走 | 預設（無效果） | `C:1441` OtherNoEffectSpell；`CS:37600` | 一致 |
| 173 | 污濁之水 | `U:2924`（治癒 ×½） | `C:2325` PolluteWaterSpell；`CS:43409` | 一致（192 秒、1000 ms）；機率屬 T102 |
| 174 | 精準射擊 | 預設；`ConfigSkill.STRIKER_DMG` | `C:2343` StrikerGaleSpell；`CS:43423` | 一致（192 秒、3000 ms、精靈玉×5） |
| 175 | 烈焰之魂 | 預設；`ConfigSkill.SOUL_OF_FLAME_DAMAGE` | `C:1424` PhysicalProcBuffSpell；`CS:37593` | 一致（64 秒、300 ms、精靈玉×3） |
| 176 | 能量激發 | 預設 | `C:1180` AdditionalFireSpell；`CS:39352` | 一致（960 秒） |

統計：一致 36、已修正 4（157、159、160、170）、不一致・未改 0（另有 `SkillCoexistSet=0` 情境的差異，§4.1）。「一致」只代表本文列出的面向；效果本體見 §5。

### 3.1 已修正的根因
`attr` 不在 `CombatDatabase` 的 DB 覆蓋清單（`CombatDatabase.java:12-15`），而每個處理器各自寫屬性檢查；159／160 的 catalog 常數寫成 0（註解只對照了 `target_to`），157 的 record 沒有欄位，170 有欄位但處理器沒用，所以這 4 個技能任何 ElfAttr 的妖精都能放。實際影響：一般情況下技能只能從對應屬性學到（`src/com/lineage/list/PcLvSkillList.java`），但在艾莉溫重置屬性後、或角色資料本來就同時有多屬性技能時，S1 會擋、LiTo 不擋。

## 4. 不一致與未改的理由

### 4.1 `SkillCoexistSet=0` 時的原廠覆蓋群組
S1 在 `SkillCoexistSet=0` 時，{148,149,155,156,163,166} 與 {3,99,151,159,168} 同組互刪（`U:2535-2537`）。LiTo 這 8 個妖精 BUFF 是獨立狀態，`CombatBuffLeases.GROUPS` 只有 3／99／151（T107 已測）。1-伺服器 預設是共存（`server_event` 116 `eventstart=1`），LiTo 也沒有讀這個設定來切換妖精群組，所以預設行為相同；只有把 `SkillCoexistSet` 改成 0 才會不同。本 PR 不改，建議由 T100（玩法設計）決定是否需要支援。

### 4.2 其他沒有比對的數值
- 機率類（133、145、152、153、157、161、167、173、174）：catalog 的 `probabilityHigh/Equal/Low`、`intelligenceMultiplier` 等屬 T102，本文只比對 `probability_dice`／`probability_value` 欄位（這些 record 沒有這兩個欄位，所以實際上沒有比對到；SQL 值已記在 tsv）。
- 134 鏡反射機率、171／175 傷害倍率、174 精準射擊倍率來自 `ConfigSkill`（`MIRROR`、`SOUL_OF_FLAME_DAMAGE`、`STRIKER_DMG`），本文未逐值比對。

## 5. 未驗證（交給實機／後續任務）

- 各處理器的封包順序、狀態圖示、實際 HP／MP 入帳。
- 新增的屬性閘門在實機的表現：屬性不符時客戶端是否會卡住施法動作（LiTo 不送任何封包，比照既有 Storm Shot；S1 也不送）。
- 154／162 依 ElfAttr 選精靈、165 生命呼喚的復活問答、158／164／170／173 治癒倍率的組合順序（`U:2918-2924`）。
- 131 世界樹的呼喚在正式 LiTo.bin 的傳送限制（catalog 註解說明正式客戶端沒有地圖 4）。

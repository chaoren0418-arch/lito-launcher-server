# LiTo 伺服器差距矩陣（M1 T093，Issue #294）

更新：2026-11-11。合併三份盤點：
- `1-伺服器`：[`docs/reference/server1-inventory.md`](../reference/server1-inventory.md)（#291，以下簡稱 **S1**）
- LiTo 現況：[`docs/reports/lito-capability-current.md`](lito-capability-current.md)（#292，以下簡稱 **LC**，列號如 A3、B12、X10）
- 客戶端協定：[`docs/reference/client-protocol-needs.md`](../reference/client-protocol-needs.md)（#293，以下簡稱 **CP**）
- 客戶缺口：[`docs/reports/customer-gaps.md`](customer-gaps.md)（#288，G 編號）

**一句話結論**：LiTo 的角色、道具、戰鬥、技能、多來源都**已經有實作**（大多依 `1-伺服器` 的 L1AttackPc／L1SkillUse 重寫），客戶端需要的封包也都有處理，資源也沒有缺。真正的缺口是：(1) **玩法沒有寫成規格**；(2) **核心計算沒有對照 `1-伺服器` 驗證過**（807 技能中 181 個有入口、**0 個**驗證過行為）；(3) **資料庫的多來源規格與部署清單不完整**，還有跨來源角色 ID 撞號沒有處理。M3 依這三點拆任務。

## 讀法
- 「S1 怎麼做」寫類別與行號（引用 S1 的段落）；「LiTo 現況」引用 LC 的列號。
- 差距等級：**缺**＝沒有實作；**未驗證**＝有實作但沒有對照 `1-伺服器` 或實測；**不需要**＝LiTo 是競技場玩法，刻意不做。
- 「任務」欄是 M3 的任務編號（見文末）。

---

## 區塊一：玩法設計

| 項目 | S1 怎麼做 | LiTo 現況 | 差距 | 任務 |
|---|---|---|---|---|
| 戰鬥模式與勝負 | S1 沒有競技場模式（S1 §3「LiTo 不需要」：PK／粉名／地獄等 WORLD 規則） | 寵物競賽、死鬥、Quest、攻城、Time Attack、Honor Match 都寫在 `BattleRules`；勝負規則在程式裡（LC B14、B21） | **缺**：沒有寫成設計文件；規則只存在程式中，改規則要讀程式 | T100 |
| 房間與對戰流程 | 無 | 建房、準備、開戰、結算、測試房與 ArenaBot（`lito_room_policy`、README「房間規則」） | **缺**：流程沒有規格 | T100 |
| 結算與排名 | S1 只知道 `character_lito`、不寫勝敗（S1 §5） | 寫 `character_lito` 勝敗；排名用勝負、月計、斷線、ranking_points；一般敗場仍加分、斷線才扣分（LC B15–B17） | **缺**：積分規則沒有設計依據；「榮譽點數」欄位未找到 | T100、T103 |
| 跨界對戰規則 | S1 有移民與多服合併，沒有跨服同場（S1 §5） | 同一個 LiTo 程式接最多 8 個來源，跨來源同房有後端測試（LC X11–X13） | **缺**：跨來源配對、平衡、顯示來源的規則沒有設計 | T100 |
| 死亡懲罰、經驗 | S1 有經驗與死亡懲罰（`ExpTable`、`L1PcInstance.deathPenalty`） | 沒有懲罰、經驗只在記憶體（LC A12、B22） | **不需要**（設計上刻意）；寫進 T100 作為明文規則 | T100 |

## 區塊二：核心計算

| 公式 | S1 來源 | LiTo 現況 | 差距 | 任務 |
|---|---|---|---|---|
| PvP 物理命中 | `L1AttackPc.calcPcHit`／`calcNpcHit`（S1 §3） | `BattlePhysicalAttack.hitChanceAgainst`（LC B1、B2） | **已對照**（#303，`docs/design/formulas-physical.md` §1）：NPC 防禦骰、龍騎／火妖精倍率已修正；閃避 /8 待裁定（H7） | T101 |
| 物理傷害 | `L1AttackPc.calcPcDamage`／`calcNpcDamage`、`weaponDamage1/2`、`pcDmgMode` 倍率取最高（S1 §3） | `BattlePhysicalDamage`、`BattlePhysicalOffense`、`BattleWeaponCritical`（LC B3–B5） | **已對照**（#303 §2）：武器骰、鋼爪、空手、屬性係數、倍率取最高一致；弓 DEX×2.5／1.5（D4）、一般武器缺職業攻擊等級（D9）待裁定 | T101 |
| AC 減傷 | `L1AttackMode.calcPcDefense`（ac=10−AC，rnd(max)+max/8）、NPC：rnd(ac/7)+ac/7（S1 §3） | `BattlePhysicalDefense`（LC B6、B7） | **一致**（#303 §3，逐值測試）；AC 額外減傷除數 ≤0 已修正 | T101 |
| 素質補正 | `CalcStat.calcStrHit/DexHit/StrDmg/DexDmg`（查 `角色_力量成長`／`角色_敏捷成長`） | 同一張 SQL 表；後備表 `SourcePhysicalGrowthStats.classic381` | **一致**（#303 §6：兩表各 127 列 0 筆不一致） | T101 |
| 職業傷害倍率 | `ConfigPcDamage`（S1 §0.2） | `CombatClassDamage` 讀 `Other_To_is*` | **一致**；呼叫點已確認在 `src/main`（#303 §4 C3） | T101 |
| 魔法傷害、MR、技能機率 | `L1MagicPc.calcMagicDamage/calcMagicDiceDamage/calcMrDefense/calcProbabilityMagic/calcProbabilityWithConfig`（`~/model/L1MagicPc.java:139-1126`）、`L1MagicMode`、`L1AttackList.MRDMG`；入口 `L1SkillUse.java:2838-3003` | `CombatMagicDamage`、`BattleMagicResistance`、`BattleElementalResistance`、`CombatProbability`、`SourceSkillControls`、`BattleTurnUndead`（LC A8、A10、B9、B10） | **已對照**（T102，`docs/design/formulas-magic.md` 39 項）：MR 表、屬性／INT 係數、爆擊、表控機率 34 列、舊公式、原始素質表皆一致；**已修正** INT≥44 MagicBonus、正義守護 MR、邪惡守護骰值／SP；呼叫點已在 `src/main`（B10 不再是 patch）。待驗證：魔眼 SP、SKD3 清單、千分比迴避 | T102（完成）；T107～T109 |
| 生命的祝福 +100 | `ConfigSkill._loadFromProperties` 不讀 `NATURES_BLESSING_HEAL`（`ConfigSkill.java:93-134`，只有 `_setDefaults:207` 設 0）；設定檔的 `= 100` 是無效設定 | 固定 0（`CombatRules.java:111-113`） | **已驗證：0 是對的**，「缺 +100」不成立（LC B12 結論，見 `formulas-magic.md` §4）；要 +100 屬新設計決定 | T102（完成） |
| HP／MP | `L1Character.getMaxHp/getMaxMp`、`CalcStat.calcStatHp/Mp`（S1 §1） | 進場直接補滿，**沒讀** CurHp／CurMp（LC A3）；回復依藥水與技能 | **缺**（設計待定：進場補滿是否為規則） | T100、T103 |
| 藥水與消耗品 | `data/item_etcitem/hp/UserAddHp` 等，參數寫在 `etcitem.classname`（S1 §2） | 回復範圍寫死在 `InventoryCatalog`（LC I7、I8） | **未驗證**；數值來源不同（S1 讀 SQL，LiTo 寫死） | T111 |
| 積分／排名 | 無 | ranking_points（LC B17） | **缺**：公式沒有設計依據 | T103 |

## 區塊三：資料庫

| 項目 | S1 | LiTo 現況 | 差距 | 任務 |
|---|---|---|---|---|
| 靜態庫與玩家庫分離 | `380_db`（靜態）＋`380_ac`（玩家），兩個連線池（S1 §0.3、§5） | `database.static.database`＋每個來源一個角色庫（LC X5） | 一致 | — |
| 來源表 `lito_source_server` | 不使用（S1 §5） | 1～8；SQL 種子啟用 3 個，README 寫「公牛、天使」，兩份文件不一致（LC X1、X2） | **缺**：沒有權威的部署規格 | T104 |
| 部署權限清單 | — | 逐表 SELECT 漏了 `角色_智力成長`，部署即啟動失敗（G01；#284） | **缺**：沒有從程式產生的完整權限清單 | T104 |
| `character_lito` 引擎 | 380_ac.sql:983 為 **MyISAM** | 結算用 `GET_LOCK`＋`last_result_id` 寫回，允許 InnoDB 或 MyISAM（S1 §6） | **未驗證**：MyISAM 沒有交易，需確認寫回邏輯在 MyISAM 下仍正確，或規定改 InnoDB | T104 |
| 跨來源角色 ID | 多服合併時 objid 可能重編（S1 §5，`CrossAccountMerge` 待驗證） | 戰鬥內直接用來源 objid，兩個來源撞號時的處理**未找到**（LC X10） | **缺**：真實風險 | T105 |
| 中文欄位對照 | weapon／armor 中文欄位對 `L1Weapon/L1Armor`（S1 §2，`ItemTable.java:413-502,548-589`） | `LegacyEquipmentMagicLoader` 讀中文欄位（LC I2）；P24Lab 的 tw-bin 結構不同（#284 `裝備_armor_魔法.id`） | **未驗證**：沒有欄位對照文件，換環境就壞 | T104 |

---

## 領域矩陣（角色／道具／戰鬥／技能／跨界）

| 領域 | 主要差距 | 協定（CP） | 任務 |
|---|---|---|---|
| 角色 | 唯讀載入、不寫回本體是刻意設計（LC A1、A14）；CurHp／MP 不讀（A3）、Sex 沒用到（A15） | 選角、S_STATUS、S_ABILITY_SCORES 等都已處理（CP §1，多數為 A 級契約） | T100、T103 |
| 道具 | 沒有捲軸強化（LC I6）：依 S1 §2「LiTo 不需要」→ **不做**；商店價目不是 3.81C 原服資料（I11）；購買與消耗只在記憶體（I13，刻意） | 已處理；**R1**：魔法娃娃圖示送 0xFA，與 3.81C 的 S_LITO_CLAN 衝突（CP §5） | T110、T111 |
| 戰鬥 | 公式都有、**都沒對照驗證**（區塊二）；受控 NPC 語意未完成（LC B23） | 已處理（CP §3） | T101、T102 |
| 技能 | 181 個有入口；**目標與範圍判定已對照**（#305，`docs/design/skill-targeting.md`：181 列 target_to／area／ranged 規格測試＋目錄有效值比對；183／188／193 目錄 target_to／type 錯位已修正；G03–G08 的 Source 修正已在 `src/main` 接線，實機待驗證）；效果行為仍 **0 個**驗證；競技場「範圍攻擊打 PC／隊友」與 S1 不同屬產品規則，待裁定；588 列非玩家技能是否在範圍內待定（LC §4） | C_USE_SPELL 各尾格式都已處理（CP §4） | T106、T107、T108、T109 |
| 跨界 | 多來源、分埠選服、各自結算都已具備（LC X3–X12）；缺 objid 撞號（X10）；帳號隔離、職業倍率、技能機率的呼叫點目前靠 patch 插入（X6、B8、B10），B-land 後要變成原始碼呼叫 | 選服 `C_LITO_LIN_CHARACTER_INFO` 已處理 | T105；B-land（#138）驗收 |
| 資源 | **沒有缺**：24 個 idx 全部查過，伺服器引用的外觀、圖示、特效全部找得到（CP §6.3） | — | — |

---

## M3 主線（11 項，封頂）

排序依「影響範圍 × 是否擋住其他工作」。T100、T104 不改程式，可以先做；其餘要改 `src/main` 的任務，都等 M2 的 B-land（#138）完成，避免與 B-land 衝突。

| # | ID | Issue | 內容 | 依據 | 依賴 |
|---|---|---|---|---|---|
| 1 | T100 | #301 | **玩法設計文件**：模式與勝負、房間與對戰流程、結算與排名、跨界對戰規則、刻意不做的項目（死亡懲罰、經驗、寫回角色本體、捲軸強化）；從程式整理現況，需要使用者決定的地方逐項列出 | 區塊一 | — |
| 2 | T104 | #302 | **資料庫規格**：多來源 schema、從程式掃描產生的完整權限清單、`lito_source_server` 權威部署值、`character_lito` 引擎規定、中文欄位對照表；修正 README 與 SQL 種子不一致 | 區塊三、G01 | — |
| 3 | T101 | #303 | **物理計算對照**：以 S1 `L1AttackPc`／`L1AttackMode`／`CalcStat` 為準，寫命中、傷害、AC 減傷、素質補正、職業倍率的規格與單元測試；不一致處改 Java | 區塊二 | #138 |
| 4 | T102 | #304 | **魔法計算對照**：魔法傷害、MR、技能機率、生命的祝福 +100（B12 矛盾），同上 | 區塊二 | #138 |
| 5 | T106 | #305 | **技能目標與範圍判定**：以 S1 `L1SkillUse.isTarget/makeTargetList`、`target_to` 位元、`area`／`ranged` 規則為準，寫規格與測試，涵蓋 G03–G08 | LC §4.3 | #138 |
| 6 | T105 | #306 | **跨來源角色 ID 撞號**：戰鬥內的玩家 ID 加上來源區隔，結算仍寫回各自的 `character_lito` | LC X10 | #138 |
| 7 | T107 | #307 | **技能行為驗證：共通魔法／法師**（79 個）：逐技能對照 S1 的 `skillmode/*` 與 `L1SkillUse`，寫測試，不一致改 Java | LC §4.2 | T106 |
| 8 | T108 | #308 | **技能行為驗證：妖精**（40 個） | LC §4.2 | T106 |
| 9 | T109 | #309 | **技能行為驗證：騎士、黑暗妖精、王族、龍騎士、幻術師**（62 個） | LC §4.2 | T106 |
| 10 | T103 | #310 | **HP／MP 與積分規則實作**：依 T100 的決定處理 CurHp／MP 與 ranking_points | 區塊二 | T100、#138 |
| 11 | T110 | #311 | **協定 R1**：魔法娃娃圖示不再送 0xFA（比照 Counter Magic 改為靜默） | CP §5 | #138 |

## 移到 Backlog
| 項目 | 理由 |
|---|---|
| T111 藥水與消耗品數值改讀 SQL | 目前寫死但能用；數值來源不同不是玩家可見錯誤，等 T107–T109 後再看 |
| 捲軸強化／爆裝 | S1 §2：LiTo 只讀角色既有裝備，不需要 |
| 商店價目改成原服資料 | 沒有 3.81C 原服資料來源（LC I11） |
| 588 列非玩家技能（NPC／道具） | 先由 T100 決定 PvE 範圍 |
| 受控 NPC（寵物、召喚）完整語意（LC B23、G13、G14、G18） | 範圍大，等 T107–T109 後再排 |
| 跨程式互連（LC X15） | 目前只需要同一個 LiTo 程式接多個來源庫 |
| G09–G12、G16 等邊界／時序問題 | 依 customer-gaps 原判斷 |

## 已驗證／推測／待驗證
- **已驗證**：各列引用的 S1、LC、CP 段落與行號（三份盤點各自已核對）。
- **推測**：LiTo 的戰鬥公式「依 Source 重寫」所指的 Source 就是 `1-伺服器`（S1 §3 末段：LiTo 註解寫「Retained L1AttackPc」，是否為同一份待 T101 確認）。
- **待驗證**：B-land 後 patch 接線（X6、B8、B10）是否已在原始碼呼叫；T104 的 MyISAM 寫回正確性。

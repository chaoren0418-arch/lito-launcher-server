# Issue #318 報告：T113 物理平衡 5 項依使用者決定處理（含 D12 祝福旗標正確性）

- 分支：`task/318`（code，第 1 次嘗試）；PR #330
- 裁定來源：#318 使用者沒有回覆，調度器依逾時預設處理——**H7、D4、D11 維持 LiTo；D9 對齊 1-伺服器**；D12 屬正確性問題，一定要修。
- 規格文件：`docs/design/formulas-physical.md`（§1 H7、§2 D4／D9／D11／D12 判定欄已更新；§7 改成「裁定結果」）
- 新測試：`lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/PhysicalBalanceDecisionT113Test.java`（9 個測試方法）

## 做了什麼

### 1. D12 祝福旗標（正確性修正）

**查證結果（已驗證）**：
- 1-伺服器 判 `_weaponBless == 1`（`src/com/lineage/server/model/L1AttackPc.java:1365,1373,1381`），值來自**樣板** `_weapon.getItem().getBless()`（`:113`）、箭 `_arrow.getItem().getBless()`（`:134`）、飛刀（`:142`）。
- 1-伺服器 載入樣板時**不換算**：`src/com/lineage/server/datatables/ItemTable.java:461`（weapon `祝福狀態`）、`:327`（etcitem `bless`）、`:590`（armor）。它的內部慣例是 0=一般、1=祝福、2=詛咒、3=幻象，送封包時才用 `L1Item.toPacketBless` 換回客戶端的 0=祝福（`src/com/lineage/server/templates/L1Item.java:246-264`）。
- `1-伺服器/db/380_db.sql`（唯讀掃描，`scratch/t318_bless_scan.py`，輸出 `scratch/t318_bless_scan.txt`）：
  - `weapon.祝福狀態`：0 有 361 筆（一般武器，如「匕首」「歐西斯匕首」），1 只有 1 筆（「測試匕首」），3 有 2 筆（幻象之劍／弓）。
  - `etcitem.bless` 欄位註解寫「0一般 1祝福 2詛咒 3幻象（內部值）」；「復活卷軸(祝福)」「祝福武器卷軸(100%)」等都存 1。
  - → DB 用的是 **1-伺服器 內部慣例（1=祝福）**。
- LiTo 原本：`LegacyEquipmentMagicLoader` 把樣板 `bless` 原值放進 `SourceWeaponCombatTraits`，箭／飛刀的 `e.bless` 也是原值；`BattlePhysicalOffense.blessingMaximum` 卻判 `attackBlessing() == 0`（原生 0=祝福）。
- **影響**：從 `380_db` 載入的一般武器打不死系（undead 1／2／3）**每下都多擲祝福 d4**；真正的祝福武器反而沒有。
- LiTo 其他地方也是同一個認知：`ModernShopCatalog`／`ModernSpellMaterials` 要求一般商品 `e.blessing()==0`、`ModernAmmo` 要求箭 `blessing==0`、`InventoryCatalog` 魔法燈籠註解「ordinary blessing」值 0，`NativeDeItemInventory.packetBless` 把 1-伺服器 內部值換回原生值。只有 classic 物理載入這條路徑漏了換算。

**修正**：
- 新增 `BattlePhysicalOffense.nativeBlessingFromSource`（`BattlePhysicalOffense.java:50`）：0→1（一般）、1→0（祝福），其他值不變，負數會丟例外。
- `LegacyEquipmentMagicLoader.java:803`（武器樣板）與 `:846`（箭／飛刀 `etcitem.bless`）載入時換算。`blessingMaximum` 的判斷本身不改（仍是原生 0=祝福，和 `RuntimeInventory.BLESSING_BLESSED`、`NativeDeItemInventory` 路徑一致）。

### 2. D9 職業攻擊等級補正（對齊 1-伺服器）

- 1-伺服器：`L1AttackPc.pcDmgMode`（`src/com/lineage/server/model/L1AttackPc.java:860`）在 `weaponDamage2` 乘完倍率**之後**、減傷之前，對所有武器加 `getClassFeature().getAttackLevel(level)`；PvP、PvE 都會經過。
- 新增 `BattlePhysicalDamage.withClassAttackLevel(multiplied, hit, kiringku, type, level)`（`BattlePhysicalDamage.java:83`），除數沿用已測過的 `BattleKiringkuDamage.classAttackLevel`。
- 接在三條物理路徑，都放在倍率之後、燃燒擊砍與減傷之前：
  - 競技場（PvP／打怪）：`ClientSession.java:86164`（`var147`）
  - WORLD 打怪：`ClientSession.java:47940`（`var79`）
  - WORLD PvP：`ClientSession.java:48871`（`n35`）
- 奇古獸（type 17）不加：`CombatKiringku.finish` 本來就有加這一項，而且它不用一般路徑的傷害。沒命中也不加。
- 截斷：1-伺服器 是 double 到最後才截斷，LiTo 是整數倍率結果再加整數；因為加的是非負整數，`(int)(x·m) + k == (int)(x·m + k)`，兩者相同。

### 3. H7、D4、D11（維持，寫明是刻意規則）

- `formulas-physical.md` §7 改成裁定表，寫明三項是**刻意的 LiTo 規則**，以後要改必須另開任務並改釘住測試。
- H7：閃避點數 0..10 直接加減攻擊骰（3.81 原版語意），不用 1-伺服器 的 `/8`。
- D4：弓 DEX 傷害直接相加，不乘 PvP ×2.5／PvE ×1.5。
- D11：PvE 地火水風用 NPC 弱點屬性 ±50，光暗聖邪用 MR。

### 4. 單元測試（`PhysicalBalanceDecisionT113Test`）

| 項目 | 測試方法 |
|---|---|
| D12 | `d12SourceBlessIsSwappedFromInternalToNativeConvention`、`d12OnlyADatabaseBlessedWeaponRollsTheD4AgainstUndeadLikeCalcMaterialBlessDmg`、`d12ProjectileBlessFromEtcitemUsesTheSameConversion` |
| D9 | `d9ClassAttackLevelMatchesGetAttackLevelForEveryClass`（7 職業 × 1..99 級）、`d9TermIsAddedAfterTheMultiplierLikePcDmgMode`、`d9SkipsMissesAndKiringkuWhichAlreadyAddsItsOwnTerm` |
| H7 | `h7DodgePointsStillSubtractDirectlyInsteadOfDividingByEight`（dodge 0..10 × 每個 d20） |
| D4 | `d4BowDexterityDamageIsAddedWithoutThePvpOrPveMultiplier` |
| D11 | `d11NpcElementalResistanceStaysTheWeakAttributePlusMinusFifty` |

## 怎麼驗證

### 已驗證
- 1-伺服器 與 LiTo 的程式位置與 DB 慣例：見上方行號與 `scratch/t318_bless_scan.txt`。
- 本機 `javac --release 17`（JDK 17.0.17）全量編譯 `src/main/java`＋`compile-stubs`＋`src/test/java`：**通過（exit 0）**。依本機規定沒有在本機執行測試。
- CI `mvn -B test`：結果見 PR #330 的 Server 檢查（下方「CI 結果」）。

### 推測
- 正式／執行中的 `380_db` 與 `1-伺服器/db/380_db.sql`（2026-11-04）的 bless 慣例相同（LiTo 與 1-伺服器 共用 `380_db`；而且 LiTo 的 Modern 商店／箭程式也以 0=一般為前提）。

### 待驗證
- 沒有連 DB，所以沒有直接查正式 `380_db` 的 weapon／etcitem bless 分布。
- 實機：拿一般銀武器與祝福武器打不死系怪，確認只有祝福武器才有額外傷害；52 級騎士一般武器每下傷害比改前 +8（交給使用者／Codex 實機）。
- MySQL 整合測試（`LegacyEquipmentPhysicalIntegrationTest` 等）在 CI 會跳過，這次的載入換算沒有在真 DB 上跑過。

## 範圍外發現（沒有改，建議另開任務）

1. **樣板 vs. 實例**：1-伺服器 的材質／祝福加傷看的是**樣板** bless；LiTo 的 `NativeDeItemInventory.updatePhysical` 與 `RuntimeInventory`（租借武器）路徑用的是**物品實例** bless（例如用祝福卷軸祝福過的武器也算）。慣例本身是對的（都經過 `packetBless`），但來源不同；要不要改成看樣板，需要裁定。
2. **背包顯示可能也反了**：`LegacyMySqlRepository` 把 `character_items.bless`（`instance_bless`）與樣板 `bless`（`template_bless`）原值放進 `SourceInventoryItem`／`ItemWire`，沒有用 `packetBless` 換算；如果 DB 是內部慣例，從 DB 載入的一般物品在客戶端可能顯示成祝福（金色）。這是推測，要實機看背包確認。
3. 幻象（bless=3）武器：`SourceWeaponCombatTraits` 只接受 0..2，`380_db` 的幻象之劍／弓裝備後載入會丟例外（改前就這樣）。

## 產出路徑
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/BattlePhysicalOffense.java`
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/BattlePhysicalDamage.java`
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/LegacyEquipmentMagicLoader.java`
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ClientSession.java`（3 處）
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/PhysicalBalanceDecisionT113Test.java`
- `docs/design/formulas-physical.md`
- `docs/reports/issue-318.md`（本檔）
- 本機唯讀掃描（不進 git）：`scratch/t318_bless_scan.py`、`scratch/t318_bless_scan.txt`

## 給下一棒的提示
- D9 讓所有物理傷害每下多 Lv/6～Lv/20；如果之後有既有測試因為固定傷害值失敗，要更新期望值，不要拿掉補正。
- H7／D4／D11 是刻意規則，改之前先在 Issue 取得使用者同意，並同步改 `PhysicalBalanceDecisionT113Test`。
- 上面「範圍外發現」1、2 建議各開一張（2 需要實機看背包顏色）。

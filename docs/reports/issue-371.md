# Issue #371 報告：租裝商人 78 項商品的附加屬性計入 RentalEquipmentBonuses

裁定：**接收**（lito-modern T040 移交 4／4）。缺口在當版 `src/main` 仍存在，已修。

## 做了什麼

- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/RentalEquipmentBonuses.java`
  - `from(...)` 對每件「已穿戴、非來源綁定」的物品仍先查 `RentalItemDefinitionCatalog.source(catalogId)`（原 57 項 3.80 配裝，優先順序與算法不變）。
  - 查不到時改查 `NativeLitoRentalMerchantCatalog.findBySourceItemId(catalogId)`：
    - 防具／飾品（53 項）：從該列的 `NativeArmorStatus` 加上力／體／敏／智／精／魅、HP／MP、SP、MR、加速、水／風／火／地抗性、暈眩／石化／睡眠／冰凍／支撐／暗黑抗性。
    - 武器（25 項）：只計入 `matchedItems`／`matchedWeapons`，屬性全 0（TSV 武器列只有原生傷害／命中欄，仍走 `equippedPhysicalWeapon()` 原路徑）。
    - TSV 沒有的欄位（HP／MP 回復、魔法傷害、減傷、負重、防具命中／傷害修正）不加，維持 0。
  - 與提案 patch 的差異：提案用字串欄位名轉接（`BonusSource` 匿名類別＋ `switch`）；本版直接讀 `NativeArmorStatus` 的具名欄位，沒有「不認得的欄位」這種執行期路徑，結果相同。
- 新測試（都不需要 `config/server.properties`、不連資料庫）：
  - `src/test/java/local/lito381/server/RentalEquipmentBonusesMerchantTest.java`
    1. 78 個商人 ID 與 57 個配裝 ID 沒有重疊（所以「優先順序」不會改到任何既有結果）。
    2. 78 項逐一穿戴：`RentalEquipmentBonuses` 35 個欄位逐欄等於測試自己從 TSV 解析的值（不經過 catalog 的欄位對應）；未穿戴、來源綁定各為 `none()`。
    3. 29047 火蜥蝪紋樣戒指：`RuntimeInventory` 穿戴後火抗 10，卸除後回 `none()`。
    4. 商人商品與配裝商品混穿時合計正確。
    5. 57 項配裝逐一穿戴的結果等於修改前的算法（測試內重寫一份原公式）；未穿戴、來源綁定為 `none()`。
  - `src/test/java/local/lito381/server/ClientSessionRentalMerchantBonusTest.java`（真實 `ClientSession`＋ socket，selftest 設定）
    - 在租裝城鎮向勒夫租 29047、向克里租 29032（TSV：SP 10、AC 12），用 `C_USE_ITEM` 穿戴／卸除，核對加密封包：
      - 穿戒指：`S_AC` 等於 `armorClass(基礎AC, 火10, 0, 0, 0)`，`S_STATUS` 結尾的四屬性區塊同為 10/0/0/0，`S_MAGIC_STATUS` 為 (0, 0)。
      - 再穿斗篷：`S_AC` 為 (基礎AC−12, 火10…)，`S_MAGIC_STATUS` 為 (SP 10, MR 0)——各只加一次。
      - 卸戒指：火抗回 0、SP 仍 10；卸斗篷：`S_AC`／`S_MAGIC_STATUS` 回到角色基礎值。

## 重複計入檢查（驗收條件 3）

靜態讀 `src/main`（`origin/main` `edfb5c82`）後的結論：**不會重複加**。

| 路徑 | 讀什麼 | 與本修補的關係 |
|---|---|---|
| 租裝城鎮 `resetAndSendRentalTownStatus`（`ClientSession.java` 約 L10264） | HP／MP、六屬性、四抗性、SP、MR 全部只來自 `RentalEquipmentBonuses`；`equippedNativeArmorBonuses()` 只取 AC | 修補前商人商品在城鎮只有 AC 生效，其餘為 0；修補後補上，AC 仍只算一次 |
| 戰鬥 `requiredEquipmentMagicStats()` → `withRentalEquipment`（`Accounts.java` 約 L4051、`SourceEquipmentPhysicalStats.java` 約 L313） | 力／體／敏／智／精、SP、MR、抗性、回復等；**不含** HP／MP／魅力 | 修補後商人商品的這些屬性在戰鬥也生效 |
| 戰鬥 `equipmentAdjustedBaseMaxHp/Mp`、`effectiveCharisma`、`baseArmorClass` | HP／MP／魅力／AC 來自 `equippedNativeArmorBonuses()`（原本就涵蓋商人商品） | 與上一列欄位不重疊，不重複 |
| `RuntimeInventory.activeFireResistance()` | 只加「非 `sourceArmor`」物品的 `definition.fireResistance()` | 商人防具都有 `sourceArmorStatus`，被排除 |
| `RuntimeInventory.hasEquippedHasteArmor()` | 直接讀 `NativeArmorStatus.haste` | `RentalEquipmentBonuses.haste` 目前沒有任何讀取端；78 項的 haste 也全是 0 |

`ClientSession` 其餘 `sourceArmorStatus()` 用途（職業遮罩、防具類型、鎧甲附魔）都不加屬性。

## 怎麼驗證

### 已驗證
- `mvn -B -q -DskipTests test-compile`（本機）：main 與 test 編譯通過。
- 靜態：上表各路徑；商人 TSV 中 29047 列火抗 10、29032 列 `spell_power` 10／`armor_class` 12（用腳本逐欄讀出，不是目視）；勒夫（資源 12）供應索引 74–90、克里（資源 11）供應 62–73。
- CI 結果：見本檔末「CI」一節。

### 推測
- 提案紀錄的「405 個 catalog 狀態、35 個彙總欄位」離線比對沒有重跑；本張的單元測試涵蓋同一組狀態（78＋57 項 × 穿戴／未穿戴／來源綁定 = 405），但期望值是測試自己算的，不是提案的 `formal-baseline.json`／`review-candidate.json`。
- 城鎮 → 戰鬥交接後，商人商品的力敏體智精／SP／MR／抗性會經 `withRentalEquipment` 生效（依程式碼路徑判斷；現有 `ClientSessionRentalBattleHandoffIntegrationTest` 只用 57 項配裝）。

### 待驗證
- **TSV 的 `spell_power`／`magic_resistance` 兩欄可能對調**：78 列的 `magic_resistance` 全是 0，而 `spell_power` 非 0 的 5 項是 29002 生命樹手套（5）、29025 邪惡蜥蜴皮盾牌（7）、29032 戈爾公皮斗篷（10）、29035 隱士斗篷（5）、29051 咒術師的戒指（10）——從名稱看前四項比較像魔防。本張照 TSV 欄名原樣計入（資源有 SHA256 釘住，沒有改）；需要對照保留的 2010 來源封存（`RETAINED_ARCHIVE_SHA256` C180BB03…）的 armor 表原始欄位才能判定，建議另開一張 resource 任務。
- 實機：原版 3.81C 客戶端／lito-modern 客戶端在租裝城鎮穿戴 29047 後屬性視窗火抗顯示 +10、卸除還原（交 Codex／使用者）。
- 商人商品穿著進戰鬥（交接後）的 `S_STATUS` 與戰鬥計算，沒有專門的整合測試。
- 租期自然到期（`serverExpireTimedInventory`）時已穿戴商人商品的屬性還原：路徑同樣呼叫 `resetAndSendRentalTownStatus`，但本張沒有加到期案例。
- 正式 0.6.71 JAR 沒有改；要上線需另走 `tools/*/build.py` patch 鏈（T002）。

## 產出路徑
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/RentalEquipmentBonuses.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/RentalEquipmentBonusesMerchantTest.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/ClientSessionRentalMerchantBonusTest.java`
- `docs/reports/issue-371.md`
- 分支：`task/371`

## 給下一棒的提示
- lito-modern：本張已接收，`修補提案/商人屬性來源/` 可以刪除（T040 4／4）。客戶端 G05.4／G08.2b 不需要自己補算，照伺服器封包顯示即可；部署新 JAR 之前，正式伺服器仍是舊行為（火抗 0）。
- 實機驗收清單：進租裝城鎮 → 向勒夫租 29047 → 穿戴 → 屬性視窗火抗 +10 → 卸除回原值；向克里租 29032 → 穿戴 → SP +10、AC −12。
- 78 項商人商品沒有 HP／MP 回復、減傷、負重等欄位，是保留的 2010 來源本身沒有（`HISTORICAL_OFFICIAL_EXACT = false`），不是漏算。

## CI
（待 PR CI 完成後補上）

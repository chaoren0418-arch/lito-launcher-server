# Issue #339 報告：T120 道具 bless 一致性與幻象武器例外（#318 範圍外發現）

- 分支：`task/339`（code，第 1 次嘗試）
- 前情：`docs/reports/issue-318.md`「範圍外發現」1～3
- 新測試：`lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/ItemBlessConsistencyT120Test.java`（7 個測試方法）

## 結論一覽

| 項目 | 結論 | 處理 |
|---|---|---|
| (3) 幻象武器（bless=3）裝備後丟例外 | **缺陷，已修** | 戰鬥祝福欄位改成接受 0..3 |
| (1) 樣板 vs. 物品實例 bless | **已統一成 1-伺服器 的做法（看樣板）**；沒找到 LiTo 刻意這樣做的理由，而且舊寫法會把 #318 的 D12 修正蓋掉 | NativeDe 路徑、租借武器路徑都改成看樣板 |
| (2) 背包載入 bless 與 `S_ADD_INVENTORY` 祝福欄的語意 | **已驗證不一致**（DB 是 1-伺服器 內部慣例，封包要的是客戶端慣例，LiTo 沒有換算） | **這次沒有改**：要讀寫兩邊、十幾個資源目錄一起換算，只改一半會把 DB 寫壞。交主控決定（理由與範圍見 §2） |

## 1. (3) 幻象武器例外（已修）

**已驗證**
- `380_db.weapon.祝福狀態=3` 有 2 筆（幻象之劍／幻象之弓，見 `docs/reports/issue-318.md:16`）。1-伺服器 內部慣例 3=幻象（`1-伺服器/src/com/lineage/server/templates/L1Item.java:246-256`）。
- `BattlePhysicalOffense.nativeBlessingFromSource(3)` 回傳 3，但 `SourceWeaponCombatTraits`（`Accounts.java:2999`）與 `SourceProjectilePhysicalStats`（`Accounts.java:3043`）只接受 0..2，所以 `LegacyEquipmentMagicLoader.sourceWeaponCombatTraits` 會丟 `SQLException("invalid source weapon combat traits")`。
- 戰鬥只在 `attackBlessing() == 0`（客戶端慣例的祝福）時加 d4（`BattlePhysicalOffense.java:69`）；1-伺服器 也只在 `_weaponBless == 1`（內部慣例的祝福）時加（`1-伺服器/src/com/lineage/server/model/L1AttackPc.java:1365,1373,1381`）。所以幻象=3 放進來不會多加傷害。

**修正**
- `Accounts.java` 兩個 record 的祝福範圍改成 0..3；4 以上、負數仍丟例外。
- `NativeDeItemInventory.updatePhysical` 原本 `if (blessing <= 2)` 的特判一併拿掉（見 §1-(1)）。
- `LegacyEquipmentPhysicalIntegrationTest.java:780`（MySQL 整合測試，CI 會跳過）的 `blessing() <= 2` 改成 `<= 3`。

## 2. (1) 樣板 vs. 物品實例（已統一成看樣板）

**已驗證**
- 1-伺服器：武器 `_weaponBless = _weapon.getItem().getBless()`（樣板，`L1AttackPc.java:113`）；箭 `_arrow.getItem().getBless()`（`:134`）；飛刀 `_sting.getItem().getBless()`（`:142`）。
- LiTo 原本有兩條路徑用**物品實例**：
  1. `NativeDeItemInventory.updatePhysical`：每次 `put`／物品變更都用 `Item.blessing()` 重建戰鬥祝福。`bind()` 時每個物品都會 `put` 一次，所以 **#318 在 `LegacyEquipmentMagicLoader` 做的樣板換算，綁定 NativeDe 背包後馬上被蓋掉**。而 `Item.blessing()` 是 DB 的原值（內部慣例 0=一般，見 §3），一般武器又會變回「祝福」（客戶端慣例 0）。
  2. `RuntimeInventory.equippedPhysicalWeapon` 的租借武器：用 `item.blessing()`，也就是定義的 `bless`。`rental-item-definitions-380.tsv` 13 把武器、44 件防具的 `bless` 全是 0（`scratch/t339_rental_bless.py` 掃描），也就是內部慣例的「一般」，但被當成客戶端慣例的「祝福」，**13 把租借武器打不死系每下多 d4**。
- 沒找到 LiTo 刻意用物品實例的註解、文件或測試（`src/test` 搜 `combatTraits()`、`attackBlessing`，沒有測試釘住實例覆寫的行為）。

**修正**
- `NativeDeItemInventory.updatePhysical`：只更新強化值與耐久，戰鬥祝福保留載入時已經換算好的樣板值。
- `RuntimeInventory`：租借武器改用 `BattlePhysicalOffense.nativeBlessingFromSource(rentalSource.integer("bless"))`（樣板＋換算）。
- `docs/design/formulas-physical.md` §7 表格新增「D12 補（T120／#339）」一列。

**沒改（交主控）**
- `RuntimeInventory.qualifiedRuntimeProjectile`（LiTo 自有背包裡的箭）仍用箭的 `item.blessing()`。這些箭的定義來自 LiTo 自己的目錄，bless 慣例是混的（見 §3 表），要等 §3 決定慣例後一起改，不然只是換一種錯法。

## 3. (2) 背包 bless 與 `S_ADD_INVENTORY` 祝福欄（已驗證不一致，未改）

**已驗證：兩邊慣例不同**
- 客戶端祝福欄：1-伺服器 送封包前一律 `L1Item.toPacketBless`（內部 0=一般 → 封包 1；內部 1=祝福 → 封包 0；2、3 不變），例如 `S_InvList.java:37`、`S_AddItem.java:29`、`S_ItemColor.java:34`。LiTo 自己的常數也是客戶端慣例：`RuntimeInventory.java:17-20`（0=祝福、1=一般、2=詛咒、3=未鑑定）。`docs/reference/client-protocol-needs.md` §2.2 只寫「祝福 0..3」。
- DB 是內部慣例：
  - `1-伺服器/db/380_db.sql:26708` `etcitem.bless` 註解「0一般 1祝福 2詛咒 3幻象（內部值）」；`weapon.祝福狀態` 一般武器 361 筆存 0。
  - `1-伺服器/db/380_ac.sql:762-800` 的 `character_items` 資料列：金幣（40308）、傳送回家的卷軸、藥水等一般物品 `bless` 全是 0。
  - 1-伺服器 讀寫 `character_items.bless` 都不換算（`CharItemsTable.java:461`、`CharacterItemPersistenceService.java:62,1105,1193,1218`）。
- LiTo 讀 DB 不換算，直接當客戶端慣例用：
  - `LegacyMySqlRepository.java:6210`（`template_bless` → `ItemWire.blessing` → 定義的 bless）、`:6260`（`instance_bless` → `SourceInventoryItem.blessing`）；`:8924`、`:8947`、`:8972`（武器／防具／道具樣板）。
  - 封包直接送 `item.blessing()`（`Protocol.java:12753`，沒有換算）。
  - → **從 DB 載入的一般物品（bless=0），封包祝福欄送 0，客戶端慣例 0=祝福。**
- LiTo 寫 DB 也不換算（`LegacyMySqlRepository.java:2453,2528,6061,6103`；比對 `:8490`）。目前「讀原值、寫原值」對 DB 來說是來回一致的，1-伺服器 看到的值沒有被改。
- LiTo 自己的資源與目錄，慣例是混的：
  | 來源 | 一般物品存的值 | 慣例 |
  |---|---|---|
  | `src/main/resources/world/*-drops.lwd.gz` 的 `[items]`（2892 列） | 全部 0（`scratch/t339_drop_bless.py`） | 內部 |
  | `rental/rental-item-definitions-380.tsv`（57 列） | 全部 0 | 內部 |
  | `InventoryCatalog` 內建（魔法燈籠等，註解寫「ordinary blessing」） | 0 | 內部 |
  | `ClassicWorldShopItemCatalog`（血之靈魂）、`InventoryCatalog` 天堂小幫手 | `BLESSING_BLESSED`（=0） | 看不出是一般還是祝福 |
  | `ClassicWorldRoyalTrialCatalog`、`NativeLitoRentalMerchantCatalog`、`ClientSession` 幾處、解除詛咒（`RuntimeInventory.java:1108`） | `BLESSING_NORMAL`（=1） | 客戶端 |
  | `NativeDeItemInventory.BoundItem`／`NativeDeItemTable` | 把 `Item.blessing()` 當客戶端慣例換成內部 | 客戶端 |

**為什麼這次不換算**
- 只在讀取時換算，寫回 DB 就會把一般物品寫成 1（1-伺服器 會看成祝福）；所以讀、寫、比對要一起改（上面 `LegacyMySqlRepository` 至少 10 處）。
- 讀寫都改之後，掉落、租借、內建目錄這些「內部慣例」資源撿起來存檔時，會被當成客戶端慣例再換一次，變成祝福；所以十幾個 `ClassicWorld*Catalog`、`RentalItemDefinitionCatalog`、`InventoryCatalog` 也要在解析時一起換算，還要逐一判斷 `BLESSING_BLESSED`／`BLESSING_NORMAL` 的寫法原意。
- 這是整個背包慣例的遷移，會動到大量以固定 bless 值斷言的既有測試；本機規定不能跑測試，只改一半風險很高。依任務「不以 stub 冒充完成」的規則，列給主控決定。

**推測**
- 客戶端把祝福欄 0 顯示成祝福（金色名稱）。依據是 1-伺服器 的 `toPacketBless` 與 LiTo 的 `RuntimeInventory` 常數；沒有逆向 `sub_4A4D90` 的顯示分支，也沒有實機看過。
- 如果推測成立，現在從 DB 載入的金幣、藥水、一般武器，在 LiTo 背包裡都會顯示成祝福；掉落、租借物品也一樣。

## 怎麼驗證

### 已驗證
- 上面所有行號與資料分布：唯讀讀過 `D:\lineage開發\1-伺服器` 原始碼與 `db/380_db.sql`、`db/380_ac.sql`，用 `scratch/t339_drop_bless.py`、`scratch/t339_rental_bless.py` 掃描 LiTo 內建資源（輸出 `scratch/t339_rental_bless.txt`，不進 git）。
- 本機 `mvn -B -o -q -DskipTests test-compile`（main＋test 全量編譯）：**通過（exit 0）**。依本機規定沒有在本機執行測試。
- CI（PR #347，run 43051055530）：`Server tests`（`mvn -B test`）**pass，0 個失敗**，`ItemBlessConsistencyT120Test` 7 個全過；Server 建置、Launcher、Semantic gap、Build-Package 全部 pass。
- `Server tests (CI MySQL fixture, non-blocking)` 是 fail，但失敗的 60 個測試和 main 最新一輪（run 43050807847）**完全相同**（逐一比對 `<<< FAILURE!/ERROR!` 清單，PR 沒有多也沒有少），是 main 原本就有的問題，和這次修改無關（例如 `DedicatedServerLayoutTest`、`LitoDatabaseLayoutTest`、`ClientSessionNativeTournamentEconomyTest`）。

### 推測
- §3 的客戶端顯示語意（見上）。
- NativeDe 綁定後 D12 被蓋掉，是照程式流程推的（`bind` → `put` → `updatePhysical`）；NativeDe 需要 SQL 才能建，沒有寫行為測試，改用原始碼檢查測試 `nativeDeInventoryNoLongerOverwritesCombatBlessWithTheInstanceBless` 釘住。

### 待驗證
- 實機（交 Codex／使用者）：
  1. 裝備幻象之劍／幻象之弓的角色進場，伺服器不再丟 `invalid source weapon combat traits`；打不死系沒有額外祝福傷害。
  2. 租借武器（例如 84 號）打不死系，傷害不再多 1～4。
  3. 背包看金幣、藥水、一般武器的名稱顏色，判斷 §3 推測是否成立（有附截圖最好）。
- MySQL 整合測試（`LegacyEquipmentPhysicalIntegrationTest`）在 CI 會跳過，幻象武器在真 DB 上的載入沒有跑過。

## 產出路徑
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/Accounts.java`（2 個 record 的範圍檢查）
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/NativeDeItemInventory.java`（`updatePhysical`）
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/RuntimeInventory.java`（租借武器戰鬥祝福）
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/ItemBlessConsistencyT120Test.java`（新）
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/LegacyEquipmentPhysicalIntegrationTest.java`（`<= 3`）
- `docs/design/formulas-physical.md`（§7 表格新增一列）
- `docs/reports/issue-339.md`（本檔）

## 給下一棒的提示
- **§3 要另開一張任務（建議 design → code）**：先實機確認祝福欄 0 的顯示，再決定 `RuntimeInventory.Item.blessing` 統一用哪種慣例。建議統一成客戶端慣例（和 `RuntimeInventory` 常數、NativeDe、戰鬥路徑一致），並在每個入口換算：
  - DB 讀：`LegacyMySqlRepository.java:6210,6260,8924,8947,8972`、`ModernCharacterDataJdbc.java:125`、`ModernShopCatalogJdbc.java:93`（Modern 路徑；`ModernShopCatalog` 要求一般商品 `blessing()==0`，即以內部慣例為前提，要一起看）。
  - DB 寫／比對：`LegacyMySqlRepository.java:2453,2528,5117,5693,5978,6061,6103,8490`。
  - 資源解析：`ClassicWorldDropCatalog.parse` 與各 `ClassicWorld*Catalog`、`RentalItemDefinitionCatalog`；`InventoryCatalog` 內建常數逐一確認。
  - 換算函式已經有了：`BattlePhysicalOffense.nativeBlessingFromSource`（0↔1 互換，自己就是反函數，寫回時也能用）。
- 慣例統一之後，`RuntimeInventory.qualifiedRuntimeProjectile` 的箭改看定義（樣板）bless，和 1-伺服器 `L1AttackPc.java:134,142` 對齊。

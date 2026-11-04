# Issue #76 / T020-A05 — src/main 對齊正式 0.6.71 暫存（other，27 類）

## 結論

A05 的 27 個外層類別已放入 `merge-inputs/formal-0.6.71/staged/`。**javac 成功**，產出 **剛好 160 個 verify_entries**（無缺漏、無多出）。T020d semantic-diff vs `reference/base-0.6.71.jar`：**153 SAME／7 DIFF／BYTE_SAME=4**。驗收產物是 staged Java 直接編成的 class，沒有把正式 JAR 的 `.class` bytes 覆蓋進去。

**尚未達到「每個 entry 都 SAME」的驗收。** 剩餘 7 筆在合理努力後仍無法用 javac 發出與正式 JAR 相同的形狀，請主控加 `needs:claude` 裁定。

## 做了什麼

範圍只新增 `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/` 下 A05 的 27 個 `.java`，以及本報告。未改 `src/main`、`pom.xml`、`docs/REPORT.md`。

來源分類（`docs/combat-merge/a-batches.json` A05 `sources`）：

| 類型 | 外層類 | 做法 |
|---|---|---|
| `sources.file` 為 null | `AccountRepository`、`MemoryAccountRepository` | Maven class 的 SourceFile 為 `Accounts.java`；為避免編譯 `Accounts.java` 多出清單外 entry，拆成獨立 staged 檔 |
| DECOMPILED | `NativeAcceptedHpContext`、`NativeAcceptedState`、`NativeCastMaterialPolicy`、`NativeCastTimeline`、`NativeCompanionCallbacks`、`NativeCompanionCommands`、`NativeCompanionCommit`、`NativeCompanionMutation` | CFR 0.152 反編譯正式 JAR，修到能編譯；保留 `// DECOMPILED` 標頭 |
| 原始碼 | 其餘 17 個（目錄／GM／Main／GameStateStore 等） | 以 `src/main` 現檔為底，對照正式版行為修正 |

系統性修正（javac 能表達的差異）：

- 正式版行為：`SharedDatabase.enabled`、`MemoryAccountRepository` 無 `accessLevels`、`CaptureLog` 無 `Connection` 內部類、目錄 SHA map、`CombatSpecialBuffs`／`ResourceDatabase` 包裝、`InventoryCatalog` 的 `controlAbilityType` 先走 `120284L`、`NativeArmorStatus` 用 `Integer.MAX_VALUE`＋`Math.min(ac,255)`、`LiToGmCommands` 的 `SourceAdministration.accessLevel`、`LiToItemDetails.describe(session,item)`、`LitoMapRepository` 巢狀 `required()`、`Main` 走 `NativeServerLifetime`。
- `synchronized (x2)` 改 `synchronized (x)`，讓 monitorenter 走 DUP。
- 匿名類捕捉名對齊正式 `val$action`／`val$pc`／`val$spell`／`val$reuse`／`val$skillId`。
- `==` 後直接 `return true`／`break`，對齊 `IF_ACMPNE`；lambda 用 `if (l2-l > 0L) return false`。
- 刪掉多餘區域（`bl2`、`object2`），讓 `NativeCompanionCallbacks.ready`、`NativeCompanionCommit.valid` 達到 SAME。

外層類本身 SAME 的 20 個：`AccountRepository`、`CaptureLog`、`ClassicWorldAdenToiCatalog`、`ClassicWorldIvoryTowerCatalog`、`ClassicWorldNpcCatalog`、`GameStateStore`、`InventoryCatalog`、`LiToGmCommands`、`LiToItemDetails`、`LitoMapRepository`、`MemoryAccountRepository`、`NativeCompanionCallbacks`、`NativeCompanionCommands`、`NativeCompanionCommit`、`NativeCompanionMutation`、`NativeLitoMapTableCatalog`、`NativeLitoRentalTownNpcCatalog`、`NativeLitoTownPortalCatalog`、`NativeLitoZoneCatalog`、`NativeMagicalWeaponAction`。其餘 153−20 筆 SAME 為這些類的 `$` 內部類（含 GameStateStore 四個 PersistenceHook **BYTE_SAME**）。

## 怎麼驗證（已驗證）

正式 JAR SHA-256：`e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`（與 T020e／a-batches.json 記載相同）。

JDK：Temurin 17.0.21（`/tmp/jdk17/bin/javac`）。repo 無 `lib/`，classpath 只有正式 JAR。

```bash
cd lito伺服器/LiTo381-Server
javac --release 17 -encoding UTF-8 \
  -cp reference/base-0.6.71.jar \
  -d /tmp/a05-out-classes \
  merge-inputs/formal-0.6.71/staged/local/lito381/server/*.java
# 輸出：Note: Some input files use unchecked or unsafe operations.
#       Note: Recompile with -Xlint:unchecked for details.
# exit 0
# 產出 160 個 .class，無清單外、無清單缺

jar cf /tmp/a05-left.jar -C /tmp/a05-out-classes .

bash tools/semantic-diff/semantic-diff \
  --left /tmp/a05-left.jar \
  --right reference/base-0.6.71.jar \
  --classes <A05 verify_entries 160 行> \
  --json scratch/t020-a05/result.json --md scratch/t020-a05/result.md
```

本次實測：

```
SUMMARY SAME=153 DIFF=7 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=4 ALLOW_METHODS=0
classSelection.missingLeft []
classSelection.extraLeft []
leftClassCount 160
```

## 剩餘 DIFF（已驗證原因；javac 無法 SAME）

7 筆 firstDiff，皆為外層類（沒有內部類 DIFF）：

| 類 | member | firstDiff | 原因（已驗證） |
|---|---|---|---|
| `ClassicWorldMapCatalog` | `forMap(I)` | left=`ASTORE 1` right=`DUP` | 正式版對 `sourceMap()` 結果走堆疊 `DUP`＋`IFNULL`（不落地區域）。javac 對 `ClassicMap sourced = ...; if (sourced != null) return sourced;` 必 `ASTORE`；改成 `(sourced = expr) != null` 會變成 `DUP`＋`ASTORE`＋`IFNULL`，仍對不上。 |
| `Main` | `accept(...)` | try `L0-L4-L5 Throwable` vs `L0-L4-L4` | 例外表 handler 標籤差一格（try-with-resources／巢狀 try-finally 重建）。 |
| `NativeAcceptedHpContext` | `resolve()` | try `L0-L6-L7 ReflectiveOperationException` vs `L0-L5-L6` | 例外表範圍差一格。return 放 try 內／外都無法對上正式標籤。 |
| `NativeAcceptedState` | `innerClasses` | 左多 `NativePlayerVitalCompletions$Receipt` | 正式 class 常數池有 `Receipt`（`reserve` 回傳型），但 InnerClasses **省略**該列。javac 只要常數池出現巢狀類就會寫 InnerClasses，無法省略。 |
| `NativeCancellationSpeedImmunity` | `magic(...)` | try `L5-L6-L8` vs `L15-L16-L23` | `synchronized`＋`try/finally` 例外表標籤與正式不同；語意等價。 |
| `NativeCastMaterialPolicy` | `nativeOriginalGm` | try `L11-L13-L18 Throwable` vs `L9-L11-L12` | 多層 try-with-resources（Connection／PreparedStatement／ResultSet）的 javac 例外表與正式不同。 |
| `NativeCastTimeline` | `lambda$admitSelf$#0` | `INVOKEDYNAMIC getAsBoolean` 的 impl | 正式版 `admitSelf` 第一個 `BooleanSupplier` 的 metafactory impl 是 **`lambda$admit$0`**（與 `admit()` 共用 synthetic）。javac 會另產 `lambda$admitSelf$4`，無法跨方法共用 lambda synthetic。 |

**禁止**用正式 JAR 的 class bytes 覆蓋驗收產物，也禁止 javac 後 ASM 後處理（主控補充 #72／#76）。因此這 7 筆維持 DIFF。

完整 firstDiff 見本機 `scratch/t020-a05/result.json`（`scratch/` 不進 git）。

## 未驗證／待驗證

- 實機登入／戰鬥（本批規定不啟動遊戲、不連正式 DB）。
- 單元測試／回歸（未動 `src/main`，A-land 再跑）。
- 用 Microsoft JDK 17.0.17 重編是否能消掉部分 try 標籤差（B01 曾對此做推測）。
- A06 同為 other 類，可能再遇到 InnerClasses／try 標籤／DUP vs ASTORE。

## 產出路徑

- `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/{AccountRepository,CaptureLog,ClassicWorldAdenToiCatalog,ClassicWorldIvoryTowerCatalog,ClassicWorldMapCatalog,ClassicWorldNpcCatalog,GameStateStore,InventoryCatalog,LiToGmCommands,LiToItemDetails,LitoMapRepository,Main,MemoryAccountRepository,NativeAcceptedHpContext,NativeAcceptedState,NativeCancellationSpeedImmunity,NativeCastMaterialPolicy,NativeCastTimeline,NativeCompanionCallbacks,NativeCompanionCommands,NativeCompanionCommit,NativeCompanionMutation,NativeLitoMapTableCatalog,NativeLitoRentalTownNpcCatalog,NativeLitoTownPortalCatalog,NativeLitoZoneCatalog,NativeMagicalWeaponAction}.java`
- `docs/reports/issue-76.md`
- 本機驗證產物（不進 git）：`/tmp/a05-left.jar`、`scratch/t020-a05/{result.json,result.md}`

## 給下一棒的提示

1. **不要從 CFR／VF 重頭整批反編譯覆蓋暫存區**；已對齊 153 個 SAME，含目錄／InventoryCatalog／GameStateStore／Companion 整組與拆檔的 AccountRepository。
2. 要 160 SAME，主控需裁定以下其一：
   - 驗收允許 InnerClasses 缺列、try 標籤、`DUP` vs `ASTORE`、跨方法共用 `lambda$admit$0` 的正規化例外；或
   - 允許 javac 之後對這 7 個 entry 做等價 ASM——**這一步目前被主控補充明文禁止**。
3. 調度器若只跑 javac + semantic-diff、不改工具，會看到本報告的 7 DIFF，不是漏檔。
4. `AccountRepository`／`MemoryAccountRepository` 不要改回編譯整份 `Accounts.java`，否則會多出清單外 entry。
5. 請主控在 Issue #76 加上 `needs:claude`（本環境 `gh` 唯讀，無法貼標）。
6. 不要改 `docs/REPORT.md`。

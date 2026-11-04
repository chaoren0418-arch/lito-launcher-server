# Issue #73 / T020-A02 — src/main 對齊正式 0.6.71 暫存（nativede，49 類）

## 做了什麼

- 依 `docs/combat-merge/a-batches.json` 的 **A02**（49 個外層類別、104 個 `verify_entries`）把 staged Java 放進  
  `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/`。
- 全部 49 檔 `src/main` 均為 `DECOMPILED`。以 CFR 0.152 與 Vineflower 1.11.1 重新反編譯 `reference/base-0.6.71.jar`，再與 `src/main` 現檔比對；staged 以能通過 Temurin 17 `javac` 且最接近 SAME 的版本為底，標頭統一為  
  `// DECOMPILED from base-0.6.71.jar, TODO review`。
- 對照正式 bytecode 修正可寫成等價 Java 的差異，例如：
  - `NativeDeShopData.isInRange` 改成兩段 early-return（對上 `IF_ICMPLE`）
  - `NativeDeItemWeaponProc` 改呼叫 `ProcRandomHelper.addOneToInclusive`（去掉 javac 多出來的 private helper）
  - `NativeDeNpcHateList` 用 `NativeDeDeletionPorts.Monster` 區域變數呼叫繼承的 `destroyed()`
  - `synchronized (this)`／正向 `instanceof` 等 DUP／跳躍方向
- **未改** `src/main`、`pom.xml`、`docs/REPORT.md`。驗收產物是 staged Java **直接 javac 編出的 `.class`**，沒有把 `base-0.6.71.jar` 的 class bytes 覆蓋進去。

## 怎麼驗證

JDK：Temurin **17.0.21+9**（`javac 17.0.21`）。`lib/` 不存在；正式 JAR 的 A02 類別還引用了 JAR 裡沒有的 `com.lineage.server.model.proc.ProcRandomHelper`，以及 JAR 版 `NativeDeWorldLog` 沒有、但 GreaterTierTable 會 `invokeinterface debug` 的方法（該介面屬 A03）。編譯時只把這兩類 **compile-only stub** 放在 classpath 最前（輸出目錄不含 stub）。

```bash
# 於 repo 根目錄；STAGED / JAR / STUBS 為本機絕對路徑縮寫
javac --release 17 -encoding UTF-8 \
  -cp <stub-classes>:lito伺服器/LiTo381-Server/reference/base-0.6.71.jar \
  -d <tmp> \
  lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDe*.java
# javac exit=0；unchecked note 2 則；產出剛好 104 個 .class

jar cf <tmp>/left.jar -C <tmp> .
# left 的 .class 集合 = A02 verify_entries（缺 0、多 0）

bash lito伺服器/LiTo381-Server/tools/semantic-diff/semantic-diff \
  --left <tmp>/left.jar \
  --right lito伺服器/LiTo381-Server/reference/base-0.6.71.jar \
  --classes docs/combat-merge/a-batches.json 之 A02.verify_entries
```

已驗證（本輪實測）：

| 項目 | 結果 |
|---|---|
| javac | exit **0** |
| 產出 entry | **104**，`missingLeft=[]`，`extraLeft=[]` |
| T020d counts | **SAME=53，DIFF=51，ONLY_LEFT=0，ONLY_RIGHT=0，BYTE_SAME=9** |
| 未覆蓋 class bytes | BYTE_SAME=9 來自 javac 真的編出與正式相同的 bytes（其餘 SAME 是語意相同、bytes 不同） |

53 個 SAME entry：`NativeDeShopData`、`NativeDeItemWeaponProc`、`NativeDePerceptionAccess`、`NativeDePerceptionControls(+Data)`，以及多個 `$` 內部類（TemplatesScope／LiveAi*／LoadScope／Transaction／Publication$Ticket 等）。

## 未驗證什麼／無法只靠 javac 對上的 entry

實機登入／戰鬥與 A-land 單元測試依任務不適用。

剩餘 **51 DIFF** 是 Temurin 17 `javac` 對正式 class 的最小差異；正式 JAR 這些類經常沒有 SourceFile、InnerClasses 不完整、甚至缺 `ACC_SUPER`，不是 javac 17 的正常輸出。**禁止**用 ASM 覆蓋 class bytes 來消差異。請主控裁定（**needs:claude**）：

### 1. innerClasses（24）— javac 無法省略已引用巢狀型別

正式 class 的 constant pool **有** `NativeDeItemDefinitionsScope$Database` 等 Methodref，但 InnerClasses 往往只留 `MethodHandles$Lookup`（例：`NativeDeItemInlayCardTable`）。javac 只要原始碼呼叫 `Database.get()` 就會寫 InnerClasses。左右差的是屬性完整性，不是少寫了行為。

`NativeDeItemGreaterTierTable`、`NativeDeItemIdStatsTable`、`NativeDeItemInlayCardTable`、`NativeDeItemMagicStoneTable`、`NativeDeItemPublication`、`NativeDeItemTable`、`NativeDeItemWeaponAttrTable`、`NativeDeItemWeaponProcTable`、`NativeDeKnownState`、`NativeDeNpcExecutor`、`NativeDeNpcHateList`、`NativeDeNpcInventory`、`NativeDeNpcMobSkillUse`、`NativeDeNpcMoveState`、`NativeDeNpcTemplate`、`NativeDeNpcTemplateMobLoader`、`NativeDeNpcTemplateNpcLoader`、`NativeDeNpcTemplateState`、`NativeDeObjectFields`、`NativeDePerceptionBadText`、`NativeDePerceptionEmblem`（反向：正式多 Lookup、我們沒有 indy）、`NativeDePerceptionNPCPack_De`、`NativeDePerceptionPacket`、`NativeDePerceptionState`。

### 2. access（2）— 非法／非 javac 旗標

- `NativeDeNpcConstructionState`：`left=0x21`（PUBLIC+SUPER）`right=0x1`（PUBLIC、**無 ACC_SUPER**）。javac 對 class 一定加 SUPER。
- `NativeDeNpcVitalState.getX()`：`left=0x1`（public）`right=0x4`（protected），但父類 `NativeDeObjectFields.getX` 是 public。Java 不許縮小可見度；正式 bytecode 不是 javac 能編出的 override。`$1.<clinit>` length 5 vs 7 同源。

### 3. locals／DUP／CHECKCAST（18）— javac 配置與冗餘 checkcast

`ASTORE` vs `DUP`（`synchronized (local)` vs `synchronized (expr)`）、try-finally 的 slot、參數靜態型別已是 `NativeDeObjectFields` 時 javac **省略**正式還保留的 `CHECKCAST`。涉及：`NativeDeItemInventory`／`$BoundItem`、`NativeDeItemTemplatesResources`／`$Owner`、`NativeDeItemTemplatesScope`／`$Classes`、`NativeDeItemTransaction.nativeCall`、`NativeDeLiveAiScope.call`、`NativeDeNpcKnowledge`、`NativeDeNpcTemplateLoadScope.call`、`NativeDeNpcTemplateResources` lambda、`NativeDeObjectBindings`、`NativeDeObjectScope`／`$World`、`NativeDePerceptionJobs$Task`、`NativeDePerceptionOutput`、`NativeDePerceptionScope`、`NativeDePerceptionSpeed`。

### 4. control（3）／tryCatch（3）

等價反向跳躍（`IFGE`/`IFLT`、`IF_ICMPGT`/`IF_ICMPLE`）與 synchronized 的 `Error` vs `RuntimeException` 表：`NativeDeItemStateCopy`、`NativeDeNpcMobSkill.isTriggerDistance`、`NativeDeProfileKernel.initialize`、`NativeDeLiveAiJobs.background`、`NativeDeLiveAiPorts$Config.load`、`NativeDePerceptionJobs.request`。

## 產出路徑

- `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/*.java`（本批 49 檔）
- `docs/reports/issue-73.md`

## 給下一棒的提示

1. 請 Claude 主控裁定：這 51 筆是否接受「javac 產物 vs 正式 JAR 的 InnerClasses／ACC_SUPER／非法 override／slot」為 A 階段允許差異，或改驗收工具忽略 InnerClasses／ACC_SUPER。**不要**用正式 `.class` 覆蓋產物。
2. 若要繼續磨 method body：先處理非 innerClasses 的 27 筆；innerClasses 24 筆就算方法已等價，T020d 仍會在方法前進 InnerClasses 就停。
3. `ProcRandomHelper` 與 A03 的 `NativeDeWorldLog.debug` 是正式 A02 類別的編譯期 API；A-land 必須先有對應原始碼或把 stub 納入 Maven。
4. 本批與 A01／A03 的 `NativeDe*` 檔在同一 staged 目錄，合併時不要互蓋。

# Issue #74 / T020-A03 — src/main 對齊正式 0.6.71 暫存（nativede，47 類）

`needs:claude`

## 做了什麼

- 依 `docs/combat-merge/a-batches.json` 的 A03：47 個外層類別、88 個驗收 entry。
- 全部 `src/main` 對應檔為 `// DECOMPILED`（`NativeDeVisibilitySwitch` 缺檔）。以 CFR 0.152 反編譯 `reference/base-0.6.71.jar`，Vineflower 1.11.2 作對照；修到 Temurin 17.0.21 `javac --release 17` 能編。
- 新增缺檔 `NativeDeVisibilitySwitch.java`（正式版為 `ACC_SYNTHETIC` switch-map；SourceFile 空、NestHost=`NativeDeDeletionPorts$Viewer`）。`NativeDeVisibilityPc` 改走該 class 的 `$SwitchMap$…RepairKind`，避免 javac 再產出清單外的 `NativeDeVisibilityPc$1`。
- 只寫 `merge-inputs/formal-0.6.71/staged/local/lito381/server/*.java` 與本報告。未改 `src/main`、`pom.xml`、`docs/REPORT.md`。**未覆蓋 `.class` bytes。**

## 怎麼驗證

編譯（repo 根目錄；`lib/` 為空故 classpath 只有正式 JAR）：

```bash
javac --release 17 -encoding UTF-8 \
  -cp lito伺服器/LiTo381-Server/reference/base-0.6.71.jar \
  -d <tmp> \
  lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/*.java
```

- 已驗證：exit 0（unchecked 警告 2 則，無 error）。產出 **剛好 88** 個 `.class`，與 `verify_entries` 無缺漏、無多出。
- 語意（Temurin 17.0.21，T020d）：

```bash
bash lito伺服器/LiTo381-Server/tools/semantic-diff/semantic-diff \
  --left <tmp 打包.jar> \
  --right lito伺服器/LiTo381-Server/reference/base-0.6.71.jar \
  --classes <A03 verify_entries>
```

**SAME=35，DIFF=53，ONLY_LEFT=0，ONLY_RIGHT=0，BYTE_SAME=3。** `classSelection.missingLeft`／`extraLeft` 皆空。

已 SAME 的 35 個 entry：`NativeDeShopResources$Snapshot`、`NativeDeSpriteCatalog` 的 Catalog／Mutable／Reporter／Sprite、`NativeDeTerrainResources$Definition`／`$Snapshot`、`NativeDeTerrainScope` 五個內部類、`NativeDeTerrainWorld` 的 Domain／Key／Owner、`NativeDeVisibilityJobs$Credit`／`$Dispatch`、`NativeDeVisibilityRuntime$Owner`、`NativeDeVisibilityScope$Logs`、`NativeDeWorldContext$1`／`$2`、`NativeDeWorldControls`、`NativeDeWorldCore$SpatialIndexAuditSnapshot`／`$SpatialReconcileResult`、`NativeDeWorldKinds` 的 Bow／Doll／Dwarf／Effect／Merchant／Npc／Pet／Trap、`NativeDeWorldLog`。另 `NativeDeTerrainPoint`／`NativeDeWorldPoint` 的 `isInScreen` 已對到 SAME。

## 未驗證什麼／寫不出等價 Java 的最小差異

未跑單元測試／回歸（未動 `src/main`）。未實機。未連 DB。

以下 53 筆 **無法用 javac 從 Java 源碼復現正式 bytes**（正式 JAR 經 relocator／合成屬性，javac 會多寫 InnerClasses、不能標 `ACC_SYNTHETIC`、switch-map 欄位名仍是舊 `com.lineage…`）。禁止覆蓋 `.class`。交主控裁定：允許這些屬性差異、或允許 A-land 前以受控 ASM 只改 InnerClasses／access／switch-map 欄位名。

### A. `innerClasses`（37）— javac 有、正式常被剝空

典型：`NativeDeWorldRegistryBow` 簽名含 `NativeDeWorldKinds$Bow`，正式 InnerClasses 為空；javac 只要寫 `Outer.Inner` 就會加上。同型：其餘 Registry*（Crown／Darkelf／Doll／DragonKnight／Effect／Elf／Illusionist／Item／Knight／Mob／Pet／Quest／Summons／Trap／TrapCell／Wizard）、`NativeDeVisibilityCheck`／`Hp`／`Coordinator`／`Packet`／`Pc`／`Timer`、`NativeDeShopTable`、`NativeDeTerrainTable`／`Text`／`V1`、`NativeDeWorldContext`／`$Entry`、`NativeDeWorldCore`／`$VisibleBinding`、`NativeDeWorldKinds`／`$Item`／`$Item$EquipmentTimerLease`／`$Item$LastStatus`、`NativeDeWorldLocation`、`NativeDeWorldPlayerFilter`。

### B. 合成旗標與舊名 switch-map（3）

| entry | 最小差異 |
|---|---|
| `NativeDeVisibilitySwitch` | access `0x20` vs `0x1020`（缺 `ACC_SYNTHETIC`）；javac 也不會寫 NestHost／EnclosingMethod／無 `<init>` |
| `NativeDeWorldCore$1` | 欄位名 javac=`$SwitchMap$local$lito381$server$NativeDeWorldCore$SpatialReconcileResult`，正式=`$SwitchMap$com$lineage$server$world$World$SpatialReconcileResult` |
| `NativeDeWorldPlayerFilter$1` | 同上，正式=`$SwitchMap$com$lineage$server$world$PlayerFilter` |

### C. 方法 codegen（13）— 控制流等價，指令／try 標籤不同

`ShopResources.load` PUTSTATIC vs ASTORE；`SpriteCatalog.read`／`TerrainResources$1.close` LABEL vs GOTO；`TerrainResources.assets`／`TerrainScope.call`／`VisibilityRuntime.call`／`VisibilityScope.call`／`WorldScope.call` try 範圍差 1 個 label；`TerrainWorld.call` GOTO vs ALOAD；`VisibilityAccess.getNpcId` 少一個 ASTORE；`VisibilityJobs.schedule` ASTORE 8 vs 7；`Ticket.end` ASTORE 4 vs 5；`VisibilityOutput.accepts` IFEQ vs IFNE。

## 產出路徑

- `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/`（47 個 `.java`）
- `docs/reports/issue-74.md`

## 給下一棒的提示

- 主控（`needs:claude`）：53 DIFF 是否接受「javac 無法復現的 InnerClasses／SYNTHETIC／舊 switch-map 名」；若否，需明確允許驗收後處理 `.class` 屬性（仍禁止整顆覆蓋）。
- A04–A06 同樣會碰到 InnerClasses 被剝空與 `$SwitchMap$com$lineage…`；可先訂統一裁定再派。
- A-land：本批 staged 可搬，但語意尚未全 SAME；落地前需重跑 88 entry。
- `NativeDeWorldLog` 正式版**沒有** `debug(String)`（src/main 多出來的已拿掉）。

# Issue #77 / T020-A06 — src/main 對齊正式 0.6.71 暫存（other，27 類）

## 結論

A06 的 27 個外層類別已放入 `merge-inputs/formal-0.6.71/staged/`。**javac 成功**，產出 **剛好 69 個 verify_entries**（無缺漏、無多出）。T020d semantic-diff vs `reference/base-0.6.71.jar`：**40 SAME／29 DIFF／BYTE_SAME=0**。驗收產物是 staged Java 直接編成的 class；69 個 entry 與正式 JAR 的 SHA-256 **全部不同**，沒有把正式 `.class` bytes 覆蓋進去。

**尚未達到「每個 entry 都 SAME」的驗收。** 剩餘 29 筆在合理努力後仍無法用 javac 發出與正式 JAR 相同的形狀，請主控加 `needs:claude` 裁定。

## 做了什麼

範圍只新增 `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/` 下 A06 的 27 個 `.java`，以及本報告。未改 `src/main`、`pom.xml`、`docs/REPORT.md`。

來源分類（`a-batches.json` `sources`）：

| 種類 | 類別 | 做法 |
|---|---|---|
| DECOMPILED（19） | NativePcActionAuthority、NativePlayerVitals、NativeProc*、NativeReuseLog、NativeServerLifetime、NativeSkillConsumptionBridges、NativeSource*、SharedClientResources | Vineflower 1.11.2 重新反編譯 `reference/base-0.6.71.jar`，標頭 `// DECOMPILED from base-0.6.71.jar, TODO review`；CFR 0.152 對 NativeNpcPhysicalSource／NativeSourceSkillConsumptionMath 作對照。以正式版行為為準，不保留 src/main 中與正式不同的邏輯。 |
| 原始碼（7） | NativeNpcPhysicalSource、NativePetWorldLifetime、NativeRoomRules、ResourceDatabase、RuntimeInventory、ServerConfig、SourceLogin | src/main 已偏離正式版（缺 `physicalSize`、`items` 欄位型別、`SourceLogin` 的 `NativeServerLifetime.acceptSocket`／`abortAccept`、`ResourceDatabase.load` 開頭不同、`ServerConfig` 未呼叫 `SharedDatabase.applySources`）。以正式 JAR 反編譯為底還原正式行為，並補 javac 所需泛型。 |
| sources.file=null（1） | SourceEquipmentPhysicalStats | 正式 class **沒有 SourceFile 屬性**。Maven／src/main 定義在 `Accounts.java`。本批只暫存此類，新增獨立 `SourceEquipmentPhysicalStats.java`；`supportsSolidCarriage` 改為正式版的 `SolidCarriageRules.equipment(shield, guarder)`。 |

系統性修正（javac 能表達的差異）：

- `synchronized (x)` 保持參數本體（Vineflower 已是 DUP 形狀）。
- `NativeSourceSkillCastAdmission.admit` 的 multi-catch 改成 `RuntimeException | Error`（對齊正式例外表順序）→ **整組 SAME**。
- `NativeSourceClientActionTimeline.ceilMillis` 改 early-return，消掉 ternary 的 `GOTO`。
- `NativeSourceSkillConsumptionMath.mpCost` 第一段改 `iinc`、switch 採 CFR 案例順序。
- `SharedClientResources.effect` 改 `<=0 || >=30285` 再 throw。
- `RuntimeInventory` 欄位改 `Map`（非正式 `LinkedHashMap`）；`PersistentItem` compact ctor 改 throw-if-invalid。
- `ServerConfig.applyExternalSourceServers` 呼叫正式版的 `SharedDatabase.applySources`；因 JAR 中該方法 `throws Exception`、目標方法 `throws IOException`，javac 必須包一層 IOException（正式 bytecode 未包，屬 ASM 形狀）。

整組 SAME 的外層類：`NativePlayerVitals`、`NativeReuseLog`、`NativeSourceActionIntervalCalculator`、`NativeSourceSkillCastAdmission`。

## 怎麼驗證（已驗證）

正式 JAR SHA-256：`e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`（與 T020e／a-batches.json 記載相同）。

JDK：Temurin 17.0.21（`/tmp/jdk17/bin/javac`）。repo 無 `lib/`，classpath 只有正式 JAR。

```bash
cd lito伺服器/LiTo381-Server
javac --release 17 -encoding UTF-8 \
  -cp reference/base-0.6.71.jar \
  -d /tmp/a06/out1 \
  merge-inputs/formal-0.6.71/staged/local/lito381/server/*.java
# 輸出：Note: Some input files use unchecked or unsafe operations.
#       Note: Recompile with -Xlint:unchecked for details.
# exit 0
# 產出 69 個 .class，無清單外、無清單缺

jar cf /tmp/a06/left.jar -C /tmp/a06/out1 .
# 69 class SHA-256 與正式 JAR 全部不同（BYTE_SAME=0）

bash tools/semantic-diff/semantic-diff \
  --left /tmp/a06/left.jar \
  --right reference/base-0.6.71.jar \
  --classes <A06 verify_entries 69 行> \
  --json /tmp/a06/result.json --md /tmp/a06/result.md
```

本次實測：

```
SUMMARY SAME=40 DIFF=29 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0
classSelection.missingLeft []
classSelection.extraLeft []
```

## 剩餘 DIFF（已驗證原因；javac 無法 SAME）

29 筆 firstDiff 依種類：

| 筆數 | 種類 | 原因（已驗證） |
|---:|---|---|
| 17 | try-catch 標籤範圍 | 例外表 `TRY Lx Ly Lz` 差一格，或 catch 型別順序（`Throwable` vs `IOException`）。try-with-resources／synchronized 重建造成。例：`NativeNpcPhysicalSource.read`、三個 `NativeProc*.load`、`ResourceDatabase.load`、`SourceLogin.account`。`ServerConfig.applyExternalSourceServers` 因 `SharedDatabase.applySources throws Exception` 必須包 IOException，正式 bytecode 直接 invoke（無 checked-exception 包裝）。 |
| 4 | `class innerClasses` | javac 只要常數池出現巢狀類就會寫 InnerClasses。正式 JAR 省略或旗標不同。例：`NativePcActionAuthority` 多了 `NativePlayerActionScope$Ticket`（try-with-resources）；`NativeSourceSkillDelay`／`$Lease` 多了 `NativeReuseLog$Logger`；`RuntimeInventory$Item` InnerClasses 清單尾段不同。 |
| 7 | 條件跳躍／switch 標籤 | 正／反條件或 LOOKUPSWITCH 標籤編號。語意等價，指令編號對不上。例：`NativeSourceSkillConsumptionMath.mpCost` 的 switch 標籤（keys 相同、labels 不同）；`SourceEquipmentPhysicalStats.totalDamageBonus` `IFNE` vs `IFEQ`；`RuntimeInventory.add` `IFNULL` vs `IFNONNULL`；`NativeProcHealingWorld.allow` 的 pattern-matching `IFEQ` 標籤。 |
| 1 | `class access` | `NativeSkillConsumptionBridges` 正式 `0x10`（只有 `ACC_FINAL`，**沒有 `ACC_SUPER`、沒有 `<init>`、0 attributes**）。這是 ASM 產物；javac 必發 `0x30`（FINAL\|SUPER）並合成建構子。 |

代表例：

- `NativeSkillConsumptionBridges` access：left=`0x30` right=`0x10`，正式 class 無建構子。
- `NativePcActionAuthority` innerClasses：left 有 Ticket，right 只有 `NativeDeItemTransaction$Operation`。
- `NativeSourceSkillConsumptionMath.mpCost`：LOOKUPSWITCH keys 同為 `1,12,13,19,26,42,43,54`，labels 不同。
- `ServerConfig.applyExternalSourceServers`：left 有 `IOException` 包裝 try，right 為 `Throwable` 的 try-with-resources 範圍。

**禁止**用正式 JAR 的 class bytes 覆蓋驗收產物，也禁止 javac 後 ASM 後處理（主控補充 #72／#77）。因此這 29 筆維持 DIFF。

## 未驗證／待驗證

- 實機登入／戰鬥（本批規定不啟動遊戲、不連正式 DB）。
- 單元測試／回歸（未動 `src/main`，A-land 再跑）。
- 用 Microsoft JDK 17.0.17 重編是否能消掉部分 try 標籤差。
- Vineflower 對 `RuntimeInventory`／`ServerConfig` 的剩餘跳躍是否能再靠手寫 if 形狀對上（已試 early-return／ternary，仍差標籤）。

## 產出路徑

- `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/`（A06 的 27 個外層類 `.java`）
- `docs/reports/issue-77.md`
- 本機驗證產物（不進 git）：`/tmp/a06/left.jar`、`/tmp/a06/{result.json,result.md,javac4.log,verify_entries.txt}`

## 給下一棒的提示

1. **不要從 CFR／VF 重頭整批反編譯覆蓋暫存區**；已對齊 40 個 SAME，含 PlayerVitals／ReuseLog／ActionIntervalCalculator／SkillCastAdmission 整組。
2. 要 69 SAME，主控需裁定以下其一：
   - 驗收允許 InnerClasses／try 標籤／LOOKUPSWITCH 標籤／ACC_SUPER 的正規化例外；或
   - 允許 javac 之後對這 29 個 entry 做等價 ASM（重寫 InnerClasses、例外表、去掉 `NativeSkillConsumptionBridges` 的 ACC_SUPER 與 `<init>`）——**這一步目前被主控補充明文禁止**。
3. `NativeSkillConsumptionBridges` 是本批唯一「javac 連 class 標頭都發不出」的 entry：正式版無 ACC_SUPER、無建構子。A-land 若堅持 bytes／語意全 SAME，必須允許 ASM 或改工具。
4. `SourceEquipmentPhysicalStats` 不要合進 `Accounts.java` 再編：那樣會產出清單外 entry。獨立檔是本批唯一合法形狀。
5. 請主控在 Issue #77 加上 `needs:claude`（本環境 `gh` 唯讀，無法貼標）。
6. 不要改 `docs/REPORT.md`。

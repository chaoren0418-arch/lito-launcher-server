# Issue #72 / T020-A01 — src/main 對齊正式 0.6.71 暫存（nativede，49 類）

## 結論

A01 的 49 個外層類別已放入 `merge-inputs/formal-0.6.71/staged/`，標頭 `// DECOMPILED from base-0.6.71.jar, TODO review`。**javac 成功**，產出 **剛好 185 個 verify_entries**（無缺漏、無多出）。T020d semantic-diff vs `reference/base-0.6.71.jar`：**123 SAME／62 DIFF／BYTE_SAME=18**。驗收產物是 staged Java 直接編成的 class，沒有把正式 JAR 的 `.class` bytes 覆蓋進去。

**尚未達到「每個 entry 都 SAME」的驗收。** 剩餘 62 筆在合理努力後仍無法用 javac 發出與正式 JAR 相同的形狀，請主控加 `needs:claude` 裁定。

## 做了什麼

範圍只新增 `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/` 下 A01 的 49 個 `.java`，以及本報告。未改 `src/main`、`pom.xml`、`docs/REPORT.md`。

49 個外層類在 `a-batches.json` 的 `sources` 皆為 DECOMPILED。做法：

1. 以 CFR 0.152 與 Vineflower 1.11.2 重新反編譯 `reference/base-0.6.71.jar`（SHA-256 `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`），對照正式版 bytecode。
2. 以 `src/main` 現有可編譯 CFR 為底（Vineflower 有 29 個編譯錯），再依正式版行為修正，不保留與正式不同的邏輯（例如 `NativeDeItemEquipProc.addZeroToInclusive` 私有方法）。
3. 系統性修正 javac 能表達的差異：
   - CFR 的 `x2 = x; synchronized (x2)` 改成 `synchronized (x)`，讓 monitorenter 走 `DUP`（`NativeDeActorState.acquire` 等）。
   - 匿名類捕捉區域變數改名，對齊正式 `val$s`／`val$effects`／`val$life`／`val$hub`／`val$t`／`val$disposition`。
   - `NativeDeEquipment` 把 `Holder`（TEMP_GFX 開頭的 switch）移到 `perform()` 之前，讓 `$1` switch map 初始化順序與正式相同。
   - `NativeDeItemEquipProc` 改呼叫正式 bytecode 中的 `com.lineage.server.model.proc.ProcRandomHelper.addZeroToInclusive`（編譯期 stub，**不進入** 驗收 JAR）。
   - 刪掉 CFR 留下、方法內未再使用的 `foo2` 區域變數，避免 ASTORE 槽位移。

外層類全部 SAME 的 6 個：`NativeDeActorState`、`NativeDeAiState`、`NativeDeAiWorkerProtocol`、`NativeDeCharacterControls`、`NativeDeEquipment`、`NativeDeItemEquipProc`（含其內部類）。

## 怎麼驗證（已驗證）

正式 JAR SHA-256：`e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`（與 T020e／a-batches.json 記載相同）。

JDK：Temurin 17.0.21（`/tmp/jdk17/bin/javac`）。repo 無 `lib/`，classpath 為正式 JAR ＋ 編譯期 stub（僅 `ProcRandomHelper`）。

```bash
cd lito伺服器/LiTo381-Server
javac --release 17 -encoding UTF-8 \
  -cp reference/base-0.6.71.jar:/tmp/a01-stubs-classes \
  -d /tmp/a01-out-classes \
  merge-inputs/formal-0.6.71/staged/local/lito381/server/*.java
# 輸出：Note: Some input files use unchecked or unsafe operations.
#       Note: Recompile with -Xlint:unchecked for details.
# exit 0
# 產出 185 個 .class，無清單外、無清單缺

jar cf /tmp/a01-left.jar -C /tmp/a01-out-classes .
# left.jar 不含 com.lineage / ProcRandomHelper

bash tools/semantic-diff/semantic-diff \
  --left /tmp/a01-left.jar \
  --right reference/base-0.6.71.jar \
  --classes <A01 verify_entries 185 行> \
  --json scratch/t020-a01/result.json --md scratch/t020-a01/result.md
```

本次實測：

```
SUMMARY SAME=123 DIFF=62 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=18
classSelection.missingLeft []
classSelection.extraLeft []
```

## 剩餘 DIFF（已驗證原因；javac 無法 SAME）

62 筆 firstDiff 依種類：

| 筆數 | 種類 | 原因（已驗證） |
|---:|---|---|
| 30 | `class innerClasses` | javac 只要常數池出現巢狀類（含 lambda 的 `MethodHandles$Lookup`、參數型別 `NativeDeDeletionPorts$ObjectRef`）就會寫 InnerClasses。正式 JAR 多處為空或旗標不同（0x8 vs 0x18），屬 ASM 搬遷／剝屬性後的形狀；Java 原始碼無法叫 javac 省略。例：`NativeDeChatBroadcast` 正式 InnerClasses 為空，方法描述子仍用 `$ObjectRef`。 |
| 16 | 區域變數槽 `ASTORE n` vs `n±1`／`ALOAD` | 剩餘額外區域、try-with-resources／synchronized 重建造成槽編號不同。語意等價，指令編號對不上。 |
| 7 | try-catch 標籤範圍 | 例外表 `TRY Lx Ly Lz` 差一格。CFR 重建的 try 邊界與原 javac 不同；未改驗收方式、未後處理 class。 |
| 3 | `IFEQ`／`IFNE`／`IF_ACMPNE` vs `IF_ACMPEQ` | 正／反條件跳躍。`NativeDeDeletionOwner.delete` 無論巢狀 `if` 或 labeled break 都對不上正式的 `IF_ACMPEQ` 成功路徑。 |
| 2 | `DUP` vs `ASTORE`／`DUP2_X1` vs `PUTFIELD` | monitor 複本或 `x = x + 1`（`DUP2_X1`）的 javac 形狀與正式不同。`NativeDeAiTimeline.nextTimer` 正式用 `DUP2_X1` 寫 `timerSequence`。 |
| 2 | enum switch map 名稱 | `NativeDeDeletionVisibility$1` 正式欄位 `$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$Phase`，`<clinit>` 卻呼叫 `NativeDeDeletionVisibility$Phase.values()`。SourceFile 仍為 `NpcDeletionVisibilityState.java`。這是類名搬遷後**沒改 switch map 字串**的 ASM 產物：javac 若 switch 在 `Phase` 上只會發出 `$SwitchMap$local$lito381$server$NativeDeDeletionVisibility$Phase`；若 switch 在 lineage enum 上則 `values()` 擁有者會錯。`$1` 的 `ACC_SYNTHETIC` 也只有 javac enum-switch 輔助類會設。 |
| 2 | 匿名類 `val$` | `NativeDeAiTimers$2` 正式多 `val$generation`（我們仍捕捉 `val$ref`／`object`）；其餘捕捉名已對齊的仍被後續 firstDiff 擋住。 |

完整 62 筆 firstDiff 見本機 `scratch/t020-a01/result.json`（不進 git）。代表例：

- `NativeDeChatBroadcast` innerClasses：left 有 Lookup＋ObjectRef＋Viewer，right 空。
- `NativeDeDeletionVisibility$1`：right-only `$SwitchMap$com$lineage$...Phase`。
- `NativeDeCharacterFields$CombatSkillDerived.compute`：`ASTORE 13` vs `ASTORE 9`。
- `NativeDeChatJobs.call`：`ALOAD 4` vs `LABEL L2`（控制流重建）。
- `NativeDeCorpseCore.run`：RuntimeException 的 try 範圍 `L12` vs `L7`。

**禁止**用正式 JAR 的 class bytes 覆蓋驗收產物，也禁止 javac 後 ASM 後處理（主控補充 #72）。因此這 62 筆維持 DIFF。

## 未驗證／待驗證

- 實機登入／戰鬥（本批規定不啟動遊戲、不連正式 DB）。
- 單元測試／回歸（未動 `src/main`，A-land 再跑）。
- 用 Microsoft JDK 17.0.17 重編是否能消掉部分 try 標籤／槽編號差（B01 曾對此做推測）。
- Vineflower 原始輸出在修完 29 個編譯錯之後，是否能比 CFR 底再多 SAME（本批 VF 未當成 staged 來源）。

## 產出路徑

- `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDe*.java`（A01 的 49 個外層類）
- `docs/reports/issue-72.md`
- 本機驗證產物（不進 git）：`/tmp/a01-left.jar`、`scratch/t020-a01/{result.json,result.md,javac.log,verify_entries.txt}`

## 給下一棒的提示

1. **不要從 CFR／VF 重頭整批反編譯覆蓋暫存區**；已對齊 123 個 SAME，含 ActorState／AiState／Equipment／ItemEquipProc／CharacterControls 整組。
2. 要 185 SAME，主控需裁定以下其一：
   - 驗收允許 InnerClasses／switch map 字串／try 標籤的正規化例外；或
   - 允許 javac 之後對這 62 個 entry 做等價 ASM（重寫 InnerClasses、改 `$SwitchMap$com$lineage$...` 欄位名、對齊例外表）——**這一步目前被主控補充明文禁止**。
3. 調度器若只跑 javac + semantic-diff、不改工具，會看到本報告的 62 DIFF，不是漏檔。
4. A02／A03 也是 NativeDe* DECOMPILED，會再遇到 InnerClasses＋lineage switch map。可沿用：`synchronized (param)`、`val$` 改名、enum switch 在原始碼裡的**第一個** switch 決定 `$1` clinit 順序。
5. 請主控在 Issue #72 加上 `needs:claude`（本環境 `gh` 唯讀，無法貼標）。
6. 不要改 `docs/REPORT.md`。

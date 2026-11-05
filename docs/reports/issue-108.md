# Issue #108 — T020h 舊伺服器編譯期 stub

## 結論／做了什麼

已完成純 Python 掃描器、簽名清單、9 份 stub Java、PowerShell／Bash 建置入口與隔離自測。依 Issue #108 主控調整後的驗收裁定，**本 Issue 驗收已達成，可交閘門與審查**：兩個指定樣本編譯成功、`NativeDeItemEquipProc` 語意比對 SAME，且移除 stub 的負向對照確實編譯失敗。原版 semantic-diff 結果仍為 **SAME=1、DIFF=1**；`NativeDeVisibilitySwitch` 的合成類別結構差異與 stub 無關，由 #135 的「metadata 豁免類別」清單於 A03 處理，不屬於本 Issue 驗收範圍。

工作分支沿用 `task/108`，基於既有 worktree 修改，沒有 checkout／新建分支。只提交本 Issue 範圍；`pom.xml`、`src/main`、`docs/REPORT.md` 均未改。

本次裁定同步（2026-11-05）僅更新本報告，沿用 `0f8abf3` 的程式與下列既有驗證證據，未修改程式或重跑編譯／語意測試。Bash／Linux 入口實跑由 #134 的 CI 驗證，不阻擋本 Issue 交付。

## 已驗證

### 輸入與掃描結果

| 輸入 | SHA-256 |
|---|---|
| `reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `reference/candidate-50635f7e.jar` | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |

兩個 JAR 各有 9 個直接 `com/lineage/` Class 常數；直接成員引用僅 `ProcRandomHelper.addOneToInclusive`／`addZeroToInclusive`，描述子皆 `(IILjava/util/Random;)I`。其餘直接類別主要由 InnerClasses metadata 引入。

按任務要求另掃 `$SwitchMap$`，找到 5 個舊 enum 名稱及 26 個實際 `getstatic` 常數。再補必要外層宣告後，共 **16 個型別、9 個 Java 檔、26 個 enum 常數**。比背景清單額外包含 `NpcDeletionVisibilityState$Phase`、`com/lineage/server/world/PlayerFilter`、`World$SpatialReconcileResult`；來源依序是 `NativeDeDeletionVisibility$1`、`NativeDeWorldPlayerFilter$1`、`NativeDeWorldCore$1`，兩個 JAR 都有相同證據。

`NativeDeVisibilitySwitch` 實際呼叫 `NativeDeathLifecycleGate.Phase` 與 `NativeDeDeletionVisibility.RepairKind`；舊名稱只保留於 switch map 欄位及部分 metadata。JSON 將這些列標記 `switch-map-alias`，不冒充直接舊類別引用。只有類別引用、沒有成員證據者不憑空補方法；例如 `ItemGenerationContext.Source` 暫按 class 輸出。

生成清單記錄每筆簽名、opcode、JAR／entry／offset 或 CP 索引、兩個輸入雜湊。重跑生成後 JSON 與全部 Java 的 SHA-256 逐一相同，`--check` 通過。

### 建置及隔離自測

環境：Python 3.12.10、Microsoft JDK 17.0.17。系統預設 `java` 是 8，因此以下一律明確使用 JDK 17。一般命令沙箱初始化失敗，後續命令使用核准的提升權限模式；工作檔仍只寫入指定 worktree。

repo 根目錄指令：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot'
py -3 -B -X utf8 lito伺服器/LiTo381-Server/tools/legacy-stubs/scan.py
py -3 -B -X utf8 lito伺服器/LiTo381-Server/tools/legacy-stubs/scan.py --check
& lito伺服器/LiTo381-Server/tools/legacy-stubs/build.ps1
py -3 -B -X utf8 lito伺服器/LiTo381-Server/tools/legacy-stubs/selftest.py
```

實際輸出摘要：

```text
GENERATED: 16 types, 9 Java files, 26 enum constants
CHECK PASS: 16 types, 9 Java files, 26 enum constants
Compiled 9 compile-only sources -> target/legacy-stubs (never packaged)
PASS regenerated JSON/Java bytes identical
PASS enum/interface/class, arrays, fields, static/instance, method handles, switch decoding; consumer recompiles
PASS wide/tableswitch/lookupswitch alignment and truncated-input rejection
PASS compile-only method throws UnsupportedOperationException
SELFTEST PASS
```

產生 16 個 stub `.class`，只放 `target/legacy-stubs`。工具沒有打包步驟。所有明確宣告的方法／防誤用建構子都拋錯；enum 的必要隱含 API 由 javac／JDK 提供，見工具 README 的限制。

### 兩個正式樣本反編譯、編譯與 semantic-diff

CFR 0.152 唯讀使用原專案其他 worktree 已有工具：
`D:/lineage開發/3-lito開發/scratch/wt/task-70/scratch/issue-70/cfr-0.152.jar`，SHA-256 `f686e8f3ded377d7bc87d216a90e9e9512df4156e75b06c655a16648ae8765b2`。沒有複製舊伺服器來源或 JAR。

以下在 repo 根目錄重現；`$cfr` 可替換為同雜湊工具路徑：

```powershell
$jdk = 'C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin'
$server = 'lito伺服器/LiTo381-Server'
$cfr = 'D:\lineage開發\3-lito開發\scratch\wt\task-70\scratch\issue-70\cfr-0.152.jar'
& "$jdk/java.exe" -jar $cfr "$server/reference/base-0.6.71.jar" `
  --jarfilter '.*(NativeDeVisibilitySwitch|NativeDeItemEquipProc).*' `
  --outputdir scratch/issue-108/decompiled --silent true
```

CFR 批次略過合成 switch 類，需唯讀擷取該單一 class 到 scratch 下的 target，再單獨反編譯：

```python
# 將這段存入 scratch/issue-108/extract.py，再以 py -3 -B -X utf8 執行。
from pathlib import Path
import zipfile
with zipfile.ZipFile('lito伺服器/LiTo381-Server/reference/base-0.6.71.jar') as z:
    p = Path('scratch/issue-108/target/input/NativeDeVisibilitySwitch.class')
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_bytes(z.read('local/lito381/server/NativeDeVisibilitySwitch.class'))
```

```powershell
& "$jdk/java.exe" -jar $cfr scratch/issue-108/target/input/NativeDeVisibilitySwitch.class `
  --extraclasspath "$server/reference/base-0.6.71.jar" `
  --outputdir scratch/issue-108/decompiled --silent true
```

CFR 輸出非法頂層 `static class NativeDeVisibilitySwitch`；只在 scratch 將它改為 `class NativeDeVisibilitySwitch`，其他來源內容未修改。這是 javac 所需語法修正，不修改方法或 switch 常數。

```powershell
& "$jdk/javac.exe" --release 17 -encoding UTF-8 `
  -cp "$server/reference/base-0.6.71.jar;$server/target/legacy-stubs;$server/lib/*" `
  -d scratch/issue-108/target/samples `
  scratch/issue-108/decompiled/local/lito381/server/NativeDeVisibilitySwitch.java `
  scratch/issue-108/decompiled/local/lito381/server/NativeDeItemEquipProc.java
& "$jdk/jar.exe" cf scratch/issue-108/target/samples.jar -C scratch/issue-108/target/samples .
```

javac exit 0。負向控制移除 `target/legacy-stubs` 後，ItemEquipProc 編譯確實失敗：`package com.lineage.server.model.proc does not exist`，不是被其他 classpath 意外滿足。

以現有 ASM 9.7 cache 建置原版 semantic-diff（工具來源未改）：

```powershell
$asm = 'D:\dev-cache\maven\repository\org\ow2\asm\asm\9.7\asm-9.7.jar;D:\dev-cache\maven\repository\org\ow2\asm\asm-tree\9.7\asm-tree-9.7.jar'
$sources = @(Get-ChildItem "$server/tools/semantic-diff/src/main/java" -Recurse -Filter '*.java' | ForEach-Object FullName)
& "$jdk/javac.exe" --release 17 -encoding UTF-8 -cp $asm `
  -d "$server/tools/semantic-diff/target/classes" @sources
& "$jdk/java.exe" -cp "$server/tools/semantic-diff/target/classes;$asm" `
  local.lito381.semanticdiff.SemanticDiff `
  --left scratch/issue-108/target/samples.jar --right "$server/reference/base-0.6.71.jar" `
  --classes 'local/lito381/server/NativeDeVisibilitySwitch,local/lito381/server/NativeDeItemEquipProc' `
  --json scratch/issue-108/semantic.json --md scratch/issue-108/semantic.md
```

```text
SUMMARY SAME=1 DIFF=1 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0 ALLOW_METHODS=0
NativeDeItemEquipProc: SAME
NativeDeVisibilitySwitch: DIFF class access left=0x20 right=0x1020
classSelection.missingLeft=[]; classSelection.extraLeft=[]
```

### switch 剩餘差異的精確診斷

`scratch/issue-108/Diagnose.java` 使用原工具 `ClassComparer`，在記憶體逐項排除已列出的結構差異，以找出下一處；不寫回 JAR，不更改 semantic-diff，不把診斷 SAME 當驗收 SAME。逐項內容可用 `javap -v -p -classpath <JAR> local.lito381.server.NativeDeVisibilitySwitch` 獨立核對：

| 項目 | javac 樣本 | 正式 class |
|---|---|---|
| 類別 access | `0x20` | `0x1020`（synthetic） |
| EnclosingMethod 的 owner | 無 | `NativeDeDeletionPorts$Viewer` |
| NestHost | 無 | `NativeDeDeletionPorts$Viewer` |
| Phase 的 InnerClasses outer | `local/.../NativeDeathLifecycleGate` | `com/lineage/server/model/Instance/DeathLifecycleGate` |
| 自身匿名 InnerClasses 列 | 無 | `NativeDeVisibilitySwitch`、outer/name null、flags `0x1008` |
| 兩個 switch 欄位 access | `0x18` | `0x1018` |
| `<init>()V` | javac 自動產生 | 無 |

其餘 InnerClasses 的 RepairKind 列相同。依序診斷輸出：

```text
raw: class access: left=0x20 right=0x1020
diagnostic: mask class ACC_SYNTHETIC: class outerClass: left= right=local/lito381/server/NativeDeDeletionPorts$Viewer
diagnostic: align class metadata: field ...DeathLifecycleGate$Phase:[I: left=0x18 right=0x1018
diagnostic: mask field ACC_SYNTHETIC: method <init>()V: only left
diagnostic: omit javac implicit constructor: SAME
negative control: change ACTIVE field reference: method <clinit>()V insn[6]: ...WRONG_CONSTANT... vs ...ACTIVE...
```

這證明在明確分離上述結構後，原工具比較 `<clinit>` 仍 SAME；負向改一個 enum 欄位引用會 DIFF，沒有略過方法指令。不能據此假設 nest metadata／額外建構子在所有情境都無行為影響。

## 推測

合成 switch 類的差異來自既有 ASM 重映射 metadata 與 javac 對獨立 Java 類的輸出規則；這與 stub 方法簽名無關。此原因符合觀察，但本輪未重播歷史 patch 鏈，不能宣稱已驗證歷史寫入來源。

## 待驗證／後續任務（不阻擋本 Issue 驗收）

- 原版工具的 `NativeDeVisibilitySwitch` DIFF 保留為實際證據；主控已裁定這是 javac 合成 switch-map 類別經 ASM 改名為頂層類別所留下的結構差異，Java 原始碼無法直接表達相同 metadata。由 [#135](https://github.com/chaoren0418-arch/lito-launcher-server/issues/135) 新增「metadata 豁免類別」清單，屬於 A03 工作，不要求本 Issue 達成兩類完整 SAME，也不再等待 #132 擴充裁定。
- 本機沒有 Bash；`build.sh` 已撰寫但尚未在 Bash／Linux 執行，PowerShell 入口已實跑。依主控裁定，Bash／Linux 驗證交由 #134 的 CI 執行。
- Maven 編譯接線／成品排除交 #134；本輪未執行完整 Maven package 或驗證其最終 artifact。
- 未執行伺服器、登入器、遊戲或正式 DB；未驗證 stub 的執行期等價性，stub 本來就禁止執行或部署。
- 未讀機密設定、封存、客戶端；未修改原始專案內容、固定 JAR 或硬連結。

## 產出路徑

- `lito伺服器/LiTo381-Server/tools/legacy-stubs/`：`scan.py`、`build.ps1`、`build.sh`、`selftest.py`、`README.md`。
- `lito伺服器/LiTo381-Server/compile-stubs/`：9 份 Java，含必要的 nested types。
- `docs/combat-merge/legacy-stubs.json`：可重跑簽名與來源證據清單。
- `docs/reports/issue-108.md`：本報告。
- 不入 git 的本機證據：`scratch/issue-108/`（反編譯來源、semantic JSON/Markdown、診斷、自測）；所有 class/JAR 均在 `target/` 子目錄。

## 給下一棒的提示

- **Claude／主控審查**：依已給定的主控裁定，本 Issue 調整後驗收已達成，請以既有 `0f8abf3` 程式及本次報告更新交閘門與審查；無須再改 stub 程式。原始 DIFF 與診斷證據均保留，未將人工排除結構後的診斷冒充原版工具 SAME。
- **#134 執行器**：使用 `compile-stubs` 與 `target/legacy-stubs` 為獨立編譯依賴；不要加入一般 runtime source root，也不要打進成品。CI 實跑 Bash 入口並檢查最終 JAR 不含 `com/lineage/`。
- **A／B 批次執行器**：可立即依 README 的 classpath 編譯。生成清單有直接引用與 switch map 推導兩種證據；不要把 metadata 推導當成部署端的完整 API。
- **#135／A03 承接者**：依主控裁定新增「metadata 豁免類別」清單，以上表及 `<clinit>` 診斷、負向對照作為證據；後續驗證須保留方法指令的比對能力。本 Issue 不再以此差異列為未完成，也不需使用者做實機操作。

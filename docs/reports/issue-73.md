# Issue #73 — T020-A02 正式 0.6.71 暫存修正與待裁定差異

## 結論

**未達成整批驗收，請主控裁定，不可標記完成或送 A-land。** 已在既有 task/73 保留遠端 fde9f6819e86eacf45c905c0b400e877101da3ff 歷史，以 33f3f5c 的主線（含 #108、#132）繼續修正。沒有 checkout、新建分支或 force push。

本輪用指定正式 JAR＋repo #108 stub 實跑：49 檔整批 javac **exit 1**。阻擋為 NativeDeItemGreaterTierTable 呼叫同一正式 JAR 中不存在的 NativeDeWorldLog.debug(String,Object,Object)。未建立私人 local.lito381.* stub，也未把呼叫改成 emit 或 reflection 來假裝 SAME。

其餘 48 檔可直接 javac，產出 **102 entry**；仍使用完整 A02 **104 entry** 清單比較，結果 **SAME=71、DIFF=31、ONLY_RIGHT=2、ONLY_LEFT=0、extraLeft=[]**。這是診斷子集，不是本批驗收通過。#73 已實際加上 needs:claude。

本報告取代前次 53/51 的敘述與私人 stub 重現方式；不沿用其「全數只是 javac 雜訊／全部無法表達」的判斷。後附 31 DIFF 的完整正規化 unified diff，及 2 ONLY_RIGHT 的完整正式正規化內容；不是只列 firstDiff。

## 做了什麼

- A02 清單的 49 檔皆為 DECOMPILED；重新執行 CFR 0.152 與 Vineflower 1.11.1，對照正式 JAR、前次 staged 與逐方法正規化輸出，保留 DECOMPILED 標頭。較接近正式版者採 Vineflower；保留已通過方法，修正 Java 泛型與作用域。其餘來源保留前次版本，不宣稱未修正部分已等價。
- 改善 22 個 staged Java。從同 classpath 的前次來源基線 **52 SAME／50 DIFF／2 ONLY_RIGHT**，提升至 **71／31／2**；原先 52 個 SAME 均維持 SAME。
- 修正同步 monitor 額外別名、finally 返回暫存、模式比對及區域變數槽；修正 NativeDeItemTemplatesScope.Classes 的陣列建構、NativeDeNpcMobSkill.isTriggerDistance 的分支。
- 修正兩處真正的反編譯缺失：NativeDeItemTemplatesResources.read 與 NativeDeNpcTemplateResources.owned 的收尾例外，第一個失敗應存下，已有失敗才 addSuppressed。兩個外層類最終均 SAME。
- NativeDePerceptionJobs.stop 在 admission 鎖內擷取工作清單，出鎖後取消；修正 Task.run 重複布林賦值。外層及 Task 均 SAME。
- 使用 #108 的 9 份 compile-stubs Java 產生 16 個 class，只放 classpath，不打包。原 semantic-diff 來源、正式 JAR、src/main、pom.xml、docs/REPORT.md 均未修改。

## 已驗證

環境：Windows、Python 3.12.10、Microsoft JDK 17.0.17。一般 exec 沙箱初始化失敗，命令使用核准的提升權限管道；所有任務檔案與產物仍僅寫指定 worktree。原專案僅唯讀取用既有 CFR JAR。

| 步驟 | 退出碼／結果 |
|---|---|
| #108 stub 編譯 | 0；9 Java → 16 class |
| A02 整批 49 Java | **1**；兩個 debug 呼叫無法解析 |
| 48 Java 診斷子集 | 0；102 class |
| 原版 semantic-diff 編譯 | 0 |
| semantic-diff 執行 | 0；工具即使 DIFF 也回傳 0，不能據此判通過 |
| class 集合與每個 JAR entry 的 bytes | 額外 0、缺少 2；JAR 每筆等於本輪 javac 輸出 |
| 完整正規化輸出交叉核對 | 71 筆完全相同、31 筆不同、2 筆左側缺少；與原工具逐 entry 狀態一致 |
| 批次驗收 | **失敗／待裁定**：整批不能編譯，且並非 104 SAME |

~~~text
SUMMARY SAME=71 SAME_EFFECTS=0 DIFF=31 ONLY_LEFT=0 ONLY_RIGHT=2 BYTE_SAME=9 ALLOW_METHODS=0
classSelection.missingLeft=[
  local/lito381/server/NativeDeItemGreaterTierTable,
  local/lito381/server/NativeDeItemGreaterTierTable$TierChance
]
classSelection.extraLeft=[]
~~~

沒有使用 --effects-methods、--allow-methods、metadata 豁免、class 覆蓋或 javac 後 ASM。診斷輸出器只讀取兩個 JAR，以既有 ClassComparer 的正規化函式列出所有比較欄位；不修改 class，不作為放寬驗收的替代工具。

### 輸入與產物 SHA-256

| 項目 | SHA-256 |
|---|---|
| lito伺服器/LiTo381-Server/reference/base-0.6.71.jar | e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498 |
| scratch/issue-73/repro/target/a02.jar | 1686ee5b9f97c76a1f87758df5db6abfdfc954dceb48f3634a62a78c35e18707 |
| scratch/issue-73/repro/verify_entries.txt | 0820ebf06520d8b142b0907016f1cc03458560215fe27c6a7f6e714cb78c0462 |
| scratch/issue-73/vineflower-1.11.1.jar | a615d07ddbbcd489369674f40e42df639c32be95410890b38f173d5c1e2ea39c |
| D:\lineage開發\3-lito開發\scratch\wt\task-70\scratch\issue-70\cfr-0.152.jar | f686e8f3ded377d7bc87d216a90e9e9512df4156e75b06c655a16648ae8765b2 |

### 新增通過的 19 個 entry

NativeDeItemInventory$BoundItem, NativeDeItemTemplatesResources, NativeDeItemTemplatesResources$Owner, NativeDeItemTemplatesScope, NativeDeItemTemplatesScope$Classes, NativeDeItemTransaction, NativeDeLiveAiPorts$Config, NativeDeLiveAiScope, NativeDeNpcMobSkill, NativeDeNpcTemplateLoadScope, NativeDeNpcTemplateResources, NativeDeObjectBindings, NativeDeObjectScope, NativeDeObjectScope$World, NativeDePerceptionJobs, NativeDePerceptionJobs$Task, NativeDePerceptionOutput, NativeDePerceptionScope, NativeDePerceptionSpeed。

## 未完成事項、推測與給下一棒的提示

1. **Claude 主控先處理正式 API 缺口**：正式 GreaterTierTable 兩處 INVOKEINTERFACE 指向 NativeDeWorldLog.debug(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V，而正式介面沒有 debug。#108 僅提供 com.lineage.*，不能解決此問題。請另行裁定該缺失契約的來源／合法修補與驗收方式；本任務沒有權限改 A03 的介面，也不自行新增私人 stub。未執行這兩個分支，不推斷部署端實際結果。
2. **Claude 主控裁定正式結構差異**：ConstructionState 缺 ACC_SUPER、NpcVitalState 的 protected getX/getY 覆寫父類 public 方法，無法由一般 javac 直接輸出相同結構。其他 InnerClasses、欄位及方法差異都完整列於附錄；不可只豁免第一個 metadata 差異就宣稱剩下相同。
3. **後續 code 執行器**：仍有可望繼續移植的方法／控制流程差異，包含 Inventory、StateCopy、LiveAiJobs、NpcKnowledge、ProfileKernel，以及 metadata 遮住的方法。CFR/Vineflower 與本輪修正尚未達到 SAME，不代表已證明它們一律無法寫成等價 Java。請以完整差異逐項繼續，任何驗收例外須先由主控核准；本輪未修改核准清單。
4. **A-land**：待編譯契約與 104 entry 全部處理後，才搬入 src/main 並執行整合測試。不得把 compile-stubs 打進成品；不得把本輪 102-entry 診斷 JAR 部署。
5. **調度器／審查者**：保留 needs:claude，不要將本次交接解讀為完成。可沿既有 PR #101 檢查本分支；使用下列腳本與附錄證據，不需要使用者操作真機。

已驗證與推測的界線：已驗證是編譯錯誤、正規化輸出及集合／雜湊；未證明剩餘不同方法的行為等價。正式 JAR 可能經 ASM 重寫導致非 javac 結構，是符合現象的推測，本輪沒有重播歷史 patch 鏈。

未執行伺服器、登入器、遊戲、正式 DB 或業務回歸。依本批範圍，單元測試／回歸不適用，留待 A-land。沒有讀取機密設定、封存、客戶端；沒有修改硬連結或原專案檔案。

## 產出路徑

- lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/：本批 49 Java，只有清單內檔案。
- docs/reports/issue-73.md：本報告及完整差異；沒有修改 docs/REPORT.md。
- 不入 git：scratch/issue-73/repro/（收據、日誌、正規化文字），所有 class/JAR 放 target/ 子目錄。
- 實際標籤：[Issue #73](https://github.com/chaoren0418-arch/lito-launcher-server/issues/73) 的 needs:claude。
- 裁定依據：[#72 主控留言](https://github.com/chaoren0418-arch/lito-launcher-server/issues/72#issuecomment-6638799507)、[#73 主控留言](https://github.com/chaoren0418-arch/lito-launcher-server/issues/73#issuecomment-6638800258)。

## 可直接重跑的編譯、打包與語意比對

在 repo 根目錄建立 scratch/issue-73/reproduce.py，貼入下列完整 Python 程式，再執行：

~~~powershell
py -3 -B -X utf8 scratch/issue-73/reproduce.py
~~~

腳本使用全新 scratch/issue-73/repro，存在時直接停止，避免殘留 class。第二次重跑請把腳本的 work 改為另一個全新的本 worktree scratch 子目錄。JDK 可由 JAVA_HOME 指定；ASM 依賴採既有 Maven cache，實際 SHA 列於後表。退出碼 1 的整批編譯是待裁定證據；腳本最後成功表示重現了未通過結果，**不代表驗收通過**。

<details>
<summary>完整重現腳本</summary>

~~~python
from pathlib import Path
import os, sys, json, hashlib, subprocess, zipfile
root = Path.cwd()
server = root / "lito伺服器/LiTo381-Server"
work = root / "scratch/issue-73/repro"
if work.exists():
    raise SystemExit("請選擇新的 scratch/issue-73/repro 輸出目錄，禁止混入前次 class")
work.mkdir(parents=True)
jdk = Path(os.environ.get("JAVA_HOME", "C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot")) / "bin"
batch = next(b for b in json.loads((root / "docs/combat-merge/a-batches.json").read_text(encoding="utf-8"))["batches"] if b["id"] == "A02")
staged = server / "merge-inputs/formal-0.6.71/staged/local/lito381/server"
sources = [staged / (name + ".java") for name in batch["classes"]]
expected = set(batch["verify_entries"])
assert len(sources) == 49 and len(expected) == 104
formal = server / "reference/base-0.6.71.jar"
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
assert sha(formal) == "e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498"
steps = []
def execute(name, args, expected_exit=0):
    args = [str(v) for v in args]
    result = subprocess.run(args, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    (work / (name + ".log")).write_bytes(result.stdout)
    steps.append({"name": name, "exit": result.returncode, "command": args})
    print(name, "exit", result.returncode)
    assert result.returncode == expected_exit, result.stdout.decode("utf-8", errors="replace")
    return result.stdout.decode("utf-8", errors="replace")
javac = [jdk / "javac.exe", "-J-Dfile.encoding=UTF-8", "--release", "17", "-encoding", "UTF-8"]
java = [jdk / "java.exe", "-Dfile.encoding=UTF-8"]
stubs = work / "target/legacy-stubs"
stubs.mkdir(parents=True)
execute("stubs-javac", [*javac, "-d", stubs, *sorted((server / "compile-stubs").rglob("*.java"))])
assert len(list(stubs.rglob("*.class"))) == 16
cp = f"{formal};{stubs};{server}/lib/*"
full = work / "target/full"
full.mkdir()
execute("full-javac", [*javac, "-cp", cp, "-d", full, *sources], 1)
# 子集只是診斷；仍以完整 104-entry 清單比對並保留 ONLY_RIGHT。
classes = work / "target/classes"
classes.mkdir()
selected = [p for p in sources if p.stem != "NativeDeItemGreaterTierTable"]
execute("subset-javac", [*javac, "-cp", cp, "-d", classes, *selected])
actual = {p.relative_to(classes).as_posix() for p in classes.rglob("*.class")}
missing = sorted(expected - actual)
extra = sorted(actual - expected)
assert len(actual) == 102 and not extra
assert missing == ["local/lito381/server/NativeDeItemGreaterTierTable$TierChance.class",
                   "local/lito381/server/NativeDeItemGreaterTierTable.class"]
assert not any(e.startswith("com/lineage/") for e in actual)
jar = work / "target/a02.jar"
with zipfile.ZipFile(jar, "w", zipfile.ZIP_DEFLATED) as z:
    for entry in sorted(actual):
        info = zipfile.ZipInfo(entry, (2000, 1, 1, 0, 0, 0))
        info.compress_type = zipfile.ZIP_DEFLATED
        z.writestr(info, (classes / entry).read_bytes())
# 打包只讀取本次 javac 輸出，逐 entry 證實未覆蓋或後處理 bytes。
with zipfile.ZipFile(jar) as z:
    assert set(z.namelist()) == actual
    assert all(z.read(e) == (classes / e).read_bytes() for e in actual)
entries = work / "verify_entries.txt"
entries.write_text("\n".join(batch["verify_entries"]) + "\n", encoding="utf-8")
asm = [Path("D:/dev-cache/maven/repository/org/ow2/asm") / a / "9.7" / (a + "-9.7.jar") for a in ("asm", "asm-tree")]
tool = work / "target/tool"
tool.mkdir()
tool_sources = sorted((server / "tools/semantic-diff/src/main/java").rglob("*.java"))
execute("tool-javac", [*javac, "-cp", ";".join(map(str, asm)), "-d", tool, *tool_sources])
tool_cp = ";".join(map(str, [tool, *asm]))
execute("semantic", [*java, "-cp", tool_cp, "local.lito381.semanticdiff.SemanticDiff",
    "--left", jar, "--right", formal, "--classes", entries,
    "--json", work / "semantic.json", "--md", work / "semantic.md"])
result = json.loads((work / "semantic.json").read_text(encoding="utf-8"))
assert result["counts"] == {"SAME": 71, "SAME_EFFECTS": 0, "DIFF": 31, "ONLY_LEFT": 0, "ONLY_RIGHT": 2, "BYTE_SAME": 9}
assert result["classSelection"]["extraLeft"] == []
assert result["classSelection"]["missingLeft"] == [
    "local/lito381/server/NativeDeItemGreaterTierTable",
    "local/lito381/server/NativeDeItemGreaterTierTable$TierChance"]
receipt = {
    "counts": result["counts"], "classSelection": result["classSelection"],
    "entries": len(actual), "missing": missing, "extra": extra,
    "hashes": {p.relative_to(root).as_posix() if p.is_relative_to(root) else str(p): sha(p)
               for p in [formal, jar, entries, *sources, *sorted((server / "compile-stubs").rglob("*.java")), *tool_sources, *asm]},
    "steps": steps
}
(work / "receipt.json").write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps({k: receipt[k] for k in ("counts", "classSelection", "entries", "missing", "extra")}, ensure_ascii=False, indent=2))
print("A02_JAR_SHA256", sha(jar))


~~~
</details>

### 本輪完整編譯錯誤

~~~text
D:\lineage開發\3-lito開發\scratch\wt\task-73\lito伺服器\LiTo381-Server\merge-inputs\formal-0.6.71\staged\local\lito381\server\NativeDeItemGreaterTierTable.java:213: error: cannot find symbol
            this.nativeDefinitions.log.debug("Optional item greater tier column read failed: {}", string, sQLException);
                                      ^
  symbol:   method debug(String,String,SQLException)
  location: variable log of type NativeDeWorldLog
D:\lineage開發\3-lito開發\scratch\wt\task-73\lito伺服器\LiTo381-Server\merge-inputs\formal-0.6.71\staged\local\lito381\server\NativeDeItemGreaterTierTable.java:223: error: cannot find symbol
            this.nativeDefinitions.log.debug("Optional item greater tier column read failed: {}", string, sQLException);
                                      ^
  symbol:   method debug(String,String,SQLException)
  location: variable log of type NativeDeWorldLog
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
2 errors

~~~

## 完整正規化差異的產生方式

DumpNormalized 逐項輸出原 ClassComparer 實際比較的 class／field／method 屬性、try-catch、指令，使用相同合成名稱及常數正規化；反射僅呼叫 private 純格式化 helper。沒有 early-return、字串 clip 或省略方法。每個 entry 的左／右完整輸出與原工具狀態一致；下方 unified diff 包含所有差異 hunk，只壓縮相同上下文。

將以下 Java 存於 scratch/issue-73/DumpNormalized.java（UTF-8、無 BOM），於上列重現流程後執行：

~~~powershell
$jdk = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin'
$w = 'scratch/issue-73/repro'
$asm = 'D:/dev-cache/maven/repository/org/ow2/asm/asm/9.7/asm-9.7.jar;D:/dev-cache/maven/repository/org/ow2/asm/asm-tree/9.7/asm-tree-9.7.jar'
$cp = "$w/target/tool;$asm"
& "$jdk/javac.exe" --release 17 -encoding UTF-8 -cp $cp -d "$w/target/tool" scratch/issue-73/DumpNormalized.java
& "$jdk/java.exe" -cp $cp local.lito381.semanticdiff.DumpNormalized "$w/target/a02.jar" "$w/verify_entries.txt" "$w/normalized-left"
& "$jdk/java.exe" -cp $cp local.lito381.semanticdiff.DumpNormalized lito伺服器/LiTo381-Server/reference/base-0.6.71.jar "$w/verify_entries.txt" "$w/normalized-right"
~~~

三個指令本輪退出碼均為 0。以 Python difflib.unified_diff(left.splitlines(keepends=True), right.splitlines(keepends=True), n=3) 產生下列內容。ONLY_RIGHT 的 left 為空，不以其他產物補洞。可用 javap -p -v -classpath reference/base-0.6.71.jar 類名 獨立核對原始屬性。

<details>
<summary>唯讀正規化輸出器（完整原始碼）</summary>

~~~java
package local.lito381.semanticdiff;
import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import org.objectweb.asm.tree.*;
public final class DumpNormalized {
  static Object helper(String name,Object... args) throws Exception {
    for(Method m:ClassComparer.class.getDeclaredMethods()) if(m.getName().equals(name)&&m.getParameterCount()==args.length){
      m.setAccessible(true); return m.invoke(null,args);
    }
    throw new IllegalArgumentException(name);
  }
  static void put(List<String> out,String name,Object value){out.add(name+"="+Objects.toString(value,""));}
  static List<String> normalized(ClassNode c,JarIndex j) throws Exception {
    List<String> a=new ArrayList<>();
    put(a,"class",c.name);put(a,"access",Integer.toHexString(c.access));put(a,"super",c.superName);
    put(a,"interfaces",helper("joinSorted",c.interfaces));put(a,"signature",c.signature);
    put(a,"outerClass",c.outerClass);put(a,"outerMethod",Objects.toString(c.outerMethod,"")+Objects.toString(c.outerMethodDesc,""));
    put(a,"nestHost",c.nestHostClass);put(a,"nestMembers",helper("joinSorted",c.nestMembers));
    put(a,"permittedSubclasses",helper("joinSorted",c.permittedSubclasses));
    put(a,"annotations",helper("annotations",c));
    String inner=(String)helper("innerClasses",c); for(String line:inner.split("; ",-1))put(a,"innerClass",line);
    put(a,"recordComponents",helper("records",c,j));
    Map<String,FieldNode> fs=(Map<String,FieldNode>)helper("fieldMap",c);
    for(var e:fs.entrySet()){
      FieldNode f=e.getValue();put(a,"field",e.getKey());put(a,"access",helper("memberAccess",f.access));
      put(a,"signature",f.signature);put(a,"value",helper("value",f.value));put(a,"annotations",helper("fieldAnns",f));
    }
    Map<String,MethodNode> ms=(Map<String,MethodNode>)helper("methodMap",c,j);
    for(var e:ms.entrySet()){
      MethodNode m=e.getValue();put(a,"method",e.getKey());put(a,"access",helper("memberAccess",m.access));
      put(a,"signature",m.signature);put(a,"exceptions",helper("joinSorted",m.exceptions));
      put(a,"annotations",helper("methodAnns",m,helper("labelIds",m),j));put(a,"annotationDefault",helper("defaultValue",m.annotationDefault,j));
      a.addAll((List<String>)helper("tryCatch",m,j));a.addAll(ClassComparer.instructions(m,j));
    }
    return a;
  }
  public static void main(String[] args)throws Exception{
    JarIndex j=JarIndex.load(Path.of(args[0]));Path out=Path.of(args[2]);Files.createDirectories(out);
    for(String name:Files.readAllLines(Path.of(args[1]))){
      name=name.trim().replace(".class","");if(name.isEmpty())continue;
      ClassNode c=j.readTree(name);if(c==null)continue;
      Files.write(out.resolve(name.substring(name.lastIndexOf('/')+1)+".txt"),normalized(c,j));
    }
  }
}


~~~
</details>

## 逐 entry 結果與差異分類

下表 class 欄為 class metadata 是否有差異，field／method 為全部不同成員數，包含存取旗標；不是只計 firstDiff。ONLY_RIGHT 的成員數列為未編譯，不能推斷其方法相同。

| Entry（省略 local/lito381/server/） | 結果 | class／field／method 差異 |
|---|---|---|
| NativeDeItemGreaterTierTable | ONLY_RIGHT | 未編譯 |
| NativeDeItemGreaterTierTable$TierChance | ONLY_RIGHT | 未編譯 |
| NativeDeItemIdStatsTable | DIFF | 1／0／2 |
| NativeDeItemInlayCardTable | DIFF | 1／0／0 |
| NativeDeItemInventory | DIFF | 0／0／3 |
| NativeDeItemInventory$BoundItem | SAME | 0／0／0 |
| NativeDeItemInventory$EntryState | SAME | 0／0／0 |
| NativeDeItemMagicStoneTable | DIFF | 1／0／0 |
| NativeDeItemPublication | DIFF | 1／0／1 |
| NativeDeItemPublication$Ticket | SAME | 0／0／0 |
| NativeDeItemStateCopy | DIFF | 0／0／3 |
| NativeDeItemTable | DIFF | 1／6／15 |
| NativeDeItemTemplatesResources | SAME | 0／0／0 |
| NativeDeItemTemplatesResources$Owner | SAME | 0／0／0 |
| NativeDeItemTemplatesResources$Snapshot | SAME | 0／0／0 |
| NativeDeItemTemplatesScope | SAME | 0／0／0 |
| NativeDeItemTemplatesScope$Binding | SAME | 0／0／0 |
| NativeDeItemTemplatesScope$Classes | SAME | 0／0／0 |
| NativeDeItemTemplatesScope$Context | SAME | 0／0／0 |
| NativeDeItemTemplatesScope$Data | SAME | 0／0／0 |
| NativeDeItemTemplatesScope$Database | SAME | 0／0／0 |
| NativeDeItemTemplatesScope$Logs | SAME | 0／0／0 |
| NativeDeItemTransaction | SAME | 0／0／0 |
| NativeDeItemTransaction$Change | SAME | 0／0／0 |
| NativeDeItemTransaction$Operation | SAME | 0／0／0 |
| NativeDeItemWeaponAttrTable | DIFF | 1／0／0 |
| NativeDeItemWeaponProc | SAME | 0／0／0 |
| NativeDeItemWeaponProcTable | DIFF | 1／1／1 |
| NativeDeKnownState | DIFF | 1／0／0 |
| NativeDeLiveAiJobs | DIFF | 0／0／6 |
| NativeDeLiveAiJobs$Binding | SAME | 0／0／0 |
| NativeDeLiveAiJobs$Credit | SAME | 0／0／0 |
| NativeDeLiveAiJobs$Owner | SAME | 0／0／0 |
| NativeDeLiveAiJobs$PulseCredit | SAME | 0／0／0 |
| NativeDeLiveAiJobs$Snapshot | SAME | 0／0／0 |
| NativeDeLiveAiJobs$Ticket | SAME | 0／0／0 |
| NativeDeLiveAiPorts | SAME | 0／0／0 |
| NativeDeLiveAiPorts$Combat | SAME | 0／0／0 |
| NativeDeLiveAiPorts$Config | SAME | 0／0／0 |
| NativeDeLiveAiPorts$Doll | SAME | 0／0／0 |
| NativeDeLiveAiPorts$Environment | SAME | 0／0／0 |
| NativeDeLiveAiPorts$Fishing | SAME | 0／0／0 |
| NativeDeLiveAiScope | SAME | 0／0／0 |
| NativeDeLiveAiScope$Clock | SAME | 0／0／0 |
| NativeDeLiveAiScope$Manager | SAME | 0／0／0 |
| NativeDeLiveAiScope$Pool | SAME | 0／0／0 |
| NativeDeLiveAiScope$Pool$ShutdownDiscardable | SAME | 0／0／0 |
| NativeDeLiveAiScope$Sprites | SAME | 0／0／0 |
| NativeDeLiveAiScope$World | SAME | 0／0／0 |
| NativeDeNpcConstructionState | DIFF | 1／0／190 |
| NativeDeNpcExecutor | DIFF | 1／0／0 |
| NativeDeNpcHateList | DIFF | 1／0／1 |
| NativeDeNpcInventory | DIFF | 1／0／1 |
| NativeDeNpcKnowledge | DIFF | 0／0／1 |
| NativeDeNpcMobSkill | SAME | 0／0／0 |
| NativeDeNpcMobSkillUse | DIFF | 1／0／1 |
| NativeDeNpcMoveState | DIFF | 1／0／0 |
| NativeDeNpcMoveState$PathFailure | SAME | 0／0／0 |
| NativeDeNpcMoveState$PathState | SAME | 0／0／0 |
| NativeDeNpcTemplate | DIFF | 1／0／0 |
| NativeDeNpcTemplateLoadScope | SAME | 0／0／0 |
| NativeDeNpcTemplateLoadScope$Context | SAME | 0／0／0 |
| NativeDeNpcTemplateLoadScope$Database | SAME | 0／0／0 |
| NativeDeNpcTemplateLoadScope$Npcs | SAME | 0／0／0 |
| NativeDeNpcTemplateLoadScope$Resolver | SAME | 0／0／0 |
| NativeDeNpcTemplateLoadScope$Skill | SAME | 0／0／0 |
| NativeDeNpcTemplateLoadScope$Skills | SAME | 0／0／0 |
| NativeDeNpcTemplateLoadScope$Sql | SAME | 0／0／0 |
| NativeDeNpcTemplateLoadScope$Timer | SAME | 0／0／0 |
| NativeDeNpcTemplateMobLoader | DIFF | 1／0／1 |
| NativeDeNpcTemplateNpcLoader | DIFF | 1／0／2 |
| NativeDeNpcTemplateResources | SAME | 0／0／0 |
| NativeDeNpcTemplateResources$Counts | SAME | 0／0／0 |
| NativeDeNpcTemplateResources$Snapshot | SAME | 0／0／0 |
| NativeDeNpcTemplateState | DIFF | 1／0／4 |
| NativeDeNpcVitalState | DIFF | 0／0／7 |
| NativeDeNpcVitalState$1 | DIFF | 0／0／1 |
| NativeDeObjectBindings | SAME | 0／0／0 |
| NativeDeObjectFields | DIFF | 1／0／1 |
| NativeDeObjectScope | SAME | 0／0／0 |
| NativeDeObjectScope$Maps | SAME | 0／0／0 |
| NativeDeObjectScope$World | SAME | 0／0／0 |
| NativeDePerceptionAccess | SAME | 0／0／0 |
| NativeDePerceptionBadText | DIFF | 1／0／2 |
| NativeDePerceptionControls | SAME | 0／0／0 |
| NativeDePerceptionControls$Data | SAME | 0／0／0 |
| NativeDePerceptionEmblem | DIFF | 1／0／0 |
| NativeDePerceptionJobs | SAME | 0／0／0 |
| NativeDePerceptionJobs$Key | SAME | 0／0／0 |
| NativeDePerceptionJobs$Owner | SAME | 0／0／0 |
| NativeDePerceptionJobs$Snapshot | SAME | 0／0／0 |
| NativeDePerceptionJobs$Task | SAME | 0／0／0 |
| NativeDePerceptionNPCPack_De | DIFF | 1／0／0 |
| NativeDePerceptionOutput | SAME | 0／0／0 |
| NativeDePerceptionPacket | DIFF | 1／0／2 |
| NativeDePerceptionScope | SAME | 0／0／0 |
| NativeDePerceptionSpeed | SAME | 0／0／0 |
| NativeDePerceptionSpeed$ActionType | SAME | 0／0／0 |
| NativeDePerceptionSpeed$SpeedState | SAME | 0／0／0 |
| NativeDePerceptionState | DIFF | 1／0／3 |
| NativeDeProfileKernel | DIFF | 0／0／1 |
| NativeDeProfileKernel$Combat | SAME | 0／0／0 |
| NativeDeProfileKernel$Target | SAME | 0／0／0 |
| NativeDeShopData | SAME | 0／0／0 |

## 完整差異附錄（staged → formal）

對 DIFF 逐 entry 展開完整 hunks；對 ONLY_RIGHT 列出左側缺少時的完整正式正規化內容。以下沒有截斷，未以第一個 metadata 差異代替方法比對。正規化文字雜湊以 UTF-8／LF 計算；diff 的空白亦保留原工具輸出。

<details>
<summary>NativeDeItemGreaterTierTable — ONLY_RIGHT</summary>

整個外層 Java 因缺失 debug API 無法編譯；TierChance 隨外層缺少。以下是正式端完整內容，沒有可比較的 javac 產物。

右側正規化 SHA-256：4358e9e374830e5413c68b83f96efdbc475d39bae4a98a600dd3a85e74fca8dc。

~~~diff
--- staged/NativeDeItemGreaterTierTable
+++ formal/NativeDeItemGreaterTierTable
@@ -0,0 +1,1355 @@
+class=local/lito381/server/NativeDeItemGreaterTierTable
+access=21
+super=java/lang/Object
+interfaces=
+signature=
+outerClass=
+outerMethod=
+nestHost=
+nestMembers=local/lito381/server/NativeDeItemGreaterTierTable$TierChance
+permittedSubclasses=
+annotations=|||
+innerClass=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
+innerClass=local/lito381/server/NativeDeItemAbility$AbilityType|local/lito381/server/NativeDeItemAbility|AbilityType|0x4019
+innerClass=local/lito381/server/NativeDeItemGreaterTierTable$TierChance|local/lito381/server/NativeDeItemGreaterTierTable|TierChance|0xa
+recordComponents=
+field=_armorChances:Ljava/util/Map;
+access=0x12
+signature=Ljava/util/Map<Ljava/lang/Integer;Ljava/util/List<Llocal/lito381/server/NativeDeItemGreaterTierTable$TierChance;>;>;
+value=
+annotations=|||
+field=_armorTierNotes:Ljava/util/Map;
+access=0x12
+signature=Ljava/util/Map<Ljava/lang/Integer;Ljava/util/Map<Ljava/lang/Integer;Ljava/lang/String;>;>;
+value=
+annotations=|||
+field=_armorTiers:Ljava/util/Map;
+access=0x12
+signature=Ljava/util/Map<Ljava/lang/Integer;Ljava/util/Map<Ljava/lang/Integer;Llocal/lito381/server/NativeDeItemAbility;>;>;
+value=
+annotations=|||
+field=_log:Llocal/lito381/server/NativeDeWorldLog;
+access=0x1a
+signature=
+value=
+annotations=|||
+field=_random:Ljava/util/Random;
+access=0x12
+signature=
+value=
+annotations=|||
+field=_tierNames:Ljava/util/Map;
+access=0x12
+signature=Ljava/util/Map<Ljava/lang/Integer;Ljava/lang/String;>;
+value=
+annotations=|||
+field=_weaponChances:Ljava/util/Map;
+access=0x12
+signature=Ljava/util/Map<Ljava/lang/Integer;Ljava/util/List<Llocal/lito381/server/NativeDeItemGreaterTierTable$TierChance;>;>;
+value=
+annotations=|||
+field=_weaponTierNotes:Ljava/util/Map;
+access=0x12
+signature=Ljava/util/Map<Ljava/lang/Integer;Ljava/util/Map<Ljava/lang/Integer;Ljava/lang/String;>;>;
+value=
+annotations=|||
+field=_weaponTiers:Ljava/util/Map;
+access=0x12
+signature=Ljava/util/Map<Ljava/lang/Integer;Ljava/util/Map<Ljava/lang/Integer;Llocal/lito381/server/NativeDeItemAbility;>;>;
+value=
+annotations=|||
+field=nativeDefinitions:Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;
+access=0x10
+signature=
+value=
+annotations=|||
+method=<clinit>()V
+access=0x8
+signature=
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+CONST type:Llocal/lito381/server/NativeDeItemGreaterTierTable;
+INVOKESTATIC local/lito381/server/NativeDeItemDefinitionsScope$Logs.getLogger(Ljava/lang/Class;)Llocal/lito381/server/NativeDeWorldLog;
+PUTSTATIC local/lito381/server/NativeDeItemGreaterTierTable._log:Llocal/lito381/server/NativeDeWorldLog;
+RETURN
+method=<init>()V
+access=0x2
+signature=
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+ALOAD 0
+INVOKESPECIAL java/lang/Object.<init>()V
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeDeItemDefinitionsScope.loadingData()Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;
+PUTFIELD local/lito381/server/NativeDeItemGreaterTierTable.nativeDefinitions:Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;
+ALOAD 0
+NEW java/util/HashMap
+DUP
+INVOKESPECIAL java/util/HashMap.<init>()V
+PUTFIELD local/lito381/server/NativeDeItemGreaterTierTable._weaponTiers:Ljava/util/Map;
+ALOAD 0
+NEW java/util/HashMap
+DUP
+INVOKESPECIAL java/util/HashMap.<init>()V
+PUTFIELD local/lito381/server/NativeDeItemGreaterTierTable._armorTiers:Ljava/util/Map;
+ALOAD 0
+NEW java/util/HashMap
+DUP
+INVOKESPECIAL java/util/HashMap.<init>()V
+PUTFIELD local/lito381/server/NativeDeItemGreaterTierTable._weaponTierNotes:Ljava/util/Map;
+ALOAD 0
+NEW java/util/HashMap
+DUP
+INVOKESPECIAL java/util/HashMap.<init>()V
+PUTFIELD local/lito381/server/NativeDeItemGreaterTierTable._armorTierNotes:Ljava/util/Map;
+ALOAD 0
+NEW java/util/HashMap
+DUP
+INVOKESPECIAL java/util/HashMap.<init>()V
+PUTFIELD local/lito381/server/NativeDeItemGreaterTierTable._tierNames:Ljava/util/Map;
+ALOAD 0
+NEW java/util/HashMap
+DUP
+INVOKESPECIAL java/util/HashMap.<init>()V
+PUTFIELD local/lito381/server/NativeDeItemGreaterTierTable._weaponChances:Ljava/util/Map;
+ALOAD 0
+NEW java/util/HashMap
+DUP
+INVOKESPECIAL java/util/HashMap.<init>()V
+PUTFIELD local/lito381/server/NativeDeItemGreaterTierTable._armorChances:Ljava/util/Map;
+ALOAD 0
+NEW java/util/Random
+DUP
+INVOKESPECIAL java/util/Random.<init>()V
+PUTFIELD local/lito381/server/NativeDeItemGreaterTierTable._random:Ljava/util/Random;
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.load()V
+RETURN
+method=fillCommonAbility(Llocal/lito381/server/NativeDeItemAbility;Ljava/sql/ResultSet;)V
+access=0x2
+signature=
+exceptions=java/sql/SQLException
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.STR:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_str"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.DEX:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_dex"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.CON:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_con"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.INT:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_int"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.WIS:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_wis"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.CHA:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_cha"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.HP:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_hp"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.MP:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_mp"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.HP_REGEN:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_hpr"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.MP_REGEN:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_mpr"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.MR:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_mr"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.SP:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_sp"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.HIT:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_hit"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.DMG:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_dmg"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.BOW_HIT:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_bow_hit"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.BOW_DMG:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_bow_dmg"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.MAGIC_HIT:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_magic_hit"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.MAGIC_DMG_MODIFIER:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_magic_dmg"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.DMG_REDUX:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_reduction"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.IGNORE_REDUCTION:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_ignore_reduction"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.DODGE:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_dodge"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.ER:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_er"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.DEF_FIRE:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_fire"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.DEF_WATER:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_water"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.DEF_WIND:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_wind"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.DEF_EARTH:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_earth"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.PVP_DMG:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_pvp_dmg"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.PVP_REDUX:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_pvp_redux"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.DMG_BOSS:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_boss_dmg"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.BOSS_REDUX:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_boss_redux"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.PIERCE:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_pierce"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.STUN_SUCCESS:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_stun_success"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.REGIST_STUN:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"regist_stun"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.POTION_RECOVERY:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_potion_recovery"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.POISON_RESIST:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_poison_resist"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.EXP_RATE:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_exp_rate"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.DROP_RATE:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_drop_rate"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 1
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.ADENA_RATE:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 2
+CONST str:"add_adena_rate"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+RETURN
+method=getArmorTier(II)Llocal/lito381/server/NativeDeItemAbility;
+access=0x1
+signature=
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable._armorTiers:Ljava/util/Map;
+ILOAD 1
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+INVOKEINTERFACE java/util/Map.get(Ljava/lang/Object;)Ljava/lang/Object; itf
+CHECKCAST java/util/Map
+ASTORE 3
+ALOAD 3
+IFNULL L0
+ALOAD 3
+ILOAD 2
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+INVOKEINTERFACE java/util/Map.get(Ljava/lang/Object;)Ljava/lang/Object; itf
+CHECKCAST local/lito381/server/NativeDeItemAbility
+GOTO L1
+LABEL L0
+CONST null
+LABEL L1
+ARETURN
+method=getDefaultTierName(I)Ljava/lang/String;
+access=0x2
+signature=
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+ILOAD 1
+TABLESWITCH min=0 max=5 default=L6 labels=L0,L1,L2,L3,L4,L5
+LABEL L0
+CONST str:"一般"
+ARETURN
+LABEL L1
+CONST str:"普通"
+ARETURN
+LABEL L2
+CONST str:"精良"
+ARETURN
+LABEL L3
+CONST str:"史詩"
+ARETURN
+LABEL L4
+CONST str:"傳說"
+ARETURN
+LABEL L5
+CONST str:"神話"
+ARETURN
+LABEL L6
+ILOAD 1
+CONST Integer:5
+IF_ICMPLT L7
+CONST str:"神話"
+ARETURN
+LABEL L7
+ILOAD 1
+INVOKEDYNAMIC makeConcatWithConstants(I)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"階級 \u0001"]
+ARETURN
+method=getInstance()Llocal/lito381/server/NativeDeItemGreaterTierTable;
+access=0x9
+signature=
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+CONST type:Llocal/lito381/server/NativeDeItemGreaterTierTable;
+INVOKESTATIC local/lito381/server/NativeDeItemDefinitionsScope.table(Ljava/lang/Class;)Ljava/lang/Object;
+CHECKCAST local/lito381/server/NativeDeItemGreaterTierTable
+ARETURN
+method=getRandomArmorTier(I)I
+access=0x1
+signature=
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable._armorChances:Ljava/util/Map;
+ILOAD 1
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+INVOKEINTERFACE java/util/Map.get(Ljava/lang/Object;)Ljava/lang/Object; itf
+CHECKCAST java/util/List
+ASTORE 2
+ALOAD 0
+ALOAD 2
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.pickRandomTier(Ljava/util/List;)I
+IRETURN
+method=getRandomWeaponTier(I)I
+access=0x1
+signature=
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable._weaponChances:Ljava/util/Map;
+ILOAD 1
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+INVOKEINTERFACE java/util/Map.get(Ljava/lang/Object;)Ljava/lang/Object; itf
+CHECKCAST java/util/List
+ASTORE 2
+ALOAD 0
+ALOAD 2
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.pickRandomTier(Ljava/util/List;)I
+IRETURN
+method=getTierName(I)Ljava/lang/String;
+access=0x1
+signature=
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable._tierNames:Ljava/util/Map;
+ILOAD 1
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+ALOAD 0
+ILOAD 1
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.getDefaultTierName(I)Ljava/lang/String;
+INVOKEINTERFACE java/util/Map.getOrDefault(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; itf
+CHECKCAST java/lang/String
+ARETURN
+method=getTierNote(IIZ)Ljava/lang/String;
+access=0x1
+signature=
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+CONST null
+ASTORE 4
+ILOAD 3
+IFEQ L1
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable._weaponTierNotes:Ljava/util/Map;
+ILOAD 1
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+INVOKEINTERFACE java/util/Map.get(Ljava/lang/Object;)Ljava/lang/Object; itf
+CHECKCAST java/util/Map
+ASTORE 5
+ALOAD 5
+IFNULL L0
+ALOAD 5
+ILOAD 2
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+INVOKEINTERFACE java/util/Map.get(Ljava/lang/Object;)Ljava/lang/Object; itf
+CHECKCAST java/lang/String
+ASTORE 4
+LABEL L0
+GOTO L2
+LABEL L1
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable._armorTierNotes:Ljava/util/Map;
+ILOAD 1
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+INVOKEINTERFACE java/util/Map.get(Ljava/lang/Object;)Ljava/lang/Object; itf
+CHECKCAST java/util/Map
+ASTORE 5
+ALOAD 5
+IFNULL L2
+ALOAD 5
+ILOAD 2
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+INVOKEINTERFACE java/util/Map.get(Ljava/lang/Object;)Ljava/lang/Object; itf
+CHECKCAST java/lang/String
+ASTORE 4
+LABEL L2
+ALOAD 4
+IFNONNULL L3
+ALOAD 0
+ILOAD 1
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.getDefaultTierName(I)Ljava/lang/String;
+ARETURN
+LABEL L3
+ALOAD 4
+ARETURN
+method=getWeaponTier(II)Llocal/lito381/server/NativeDeItemAbility;
+access=0x1
+signature=
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable._weaponTiers:Ljava/util/Map;
+ILOAD 1
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+INVOKEINTERFACE java/util/Map.get(Ljava/lang/Object;)Ljava/lang/Object; itf
+CHECKCAST java/util/Map
+ASTORE 3
+ALOAD 3
+IFNULL L0
+ALOAD 3
+ILOAD 2
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+INVOKEINTERFACE java/util/Map.get(Ljava/lang/Object;)Ljava/lang/Object; itf
+CHECKCAST local/lito381/server/NativeDeItemAbility
+GOTO L1
+LABEL L0
+CONST null
+LABEL L1
+ARETURN
+method=hasColumn(Ljava/sql/ResultSet;Ljava/lang/String;)Z
+access=0x2
+signature=
+exceptions=java/sql/SQLException
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+TRY L0 L1 L2 java/sql/SQLException  
+LABEL L0
+ALOAD 1
+ALOAD 2
+INVOKEINTERFACE java/sql/ResultSet.findColumn(Ljava/lang/String;)I itf
+POP
+CONST Integer:1
+LABEL L1
+IRETURN
+LABEL L2
+ASTORE 3
+CONST Integer:0
+IRETURN
+method=lambda$loadArmorTiers$#0(Ljava/lang/Integer;)Ljava/util/List;
+access=0xa
+signature=
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+NEW java/util/ArrayList
+DUP
+INVOKESPECIAL java/util/ArrayList.<init>()V
+ARETURN
+method=lambda$loadArmorTiers$#1(Ljava/lang/Integer;)Ljava/util/Map;
+access=0xa
+signature=
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+NEW java/util/HashMap
+DUP
+INVOKESPECIAL java/util/HashMap.<init>()V
+ARETURN
+method=lambda$loadArmorTiers$#2(Ljava/lang/Integer;)Ljava/util/Map;
+access=0xa
+signature=
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+NEW java/util/HashMap
+DUP
+INVOKESPECIAL java/util/HashMap.<init>()V
+ARETURN
+method=lambda$loadWeaponTiers$#0(Ljava/lang/Integer;)Ljava/util/List;
+access=0xa
+signature=
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+NEW java/util/ArrayList
+DUP
+INVOKESPECIAL java/util/ArrayList.<init>()V
+ARETURN
+method=lambda$loadWeaponTiers$#1(Ljava/lang/Integer;)Ljava/util/Map;
+access=0xa
+signature=
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+NEW java/util/HashMap
+DUP
+INVOKESPECIAL java/util/HashMap.<init>()V
+ARETURN
+method=lambda$loadWeaponTiers$#2(Ljava/lang/Integer;)Ljava/util/Map;
+access=0xa
+signature=
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+NEW java/util/HashMap
+DUP
+INVOKESPECIAL java/util/HashMap.<init>()V
+ARETURN
+method=load()V
+access=0x2
+signature=
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable.nativeDefinitions:Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;
+INVOKESTATIC local/lito381/server/NativeDeItemDefinitionsScope.requireLoading(Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;)V
+NEW local/lito381/server/NativeDeTerrainTimer
+DUP
+INVOKESPECIAL local/lito381/server/NativeDeTerrainTimer.<init>()V
+ASTORE 1
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.loadWeaponTiers()V
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.loadArmorTiers()V
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable.nativeDefinitions:Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;
+GETFIELD local/lito381/server/NativeDeItemDefinitionsScope$Data.log:Llocal/lito381/server/NativeDeWorldLog;
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable._weaponTiers:Ljava/util/Map;
+INVOKEINTERFACE java/util/Map.size()I itf
+ALOAD 1
+INVOKEVIRTUAL local/lito381/server/NativeDeTerrainTimer.get()J
+INVOKEDYNAMIC makeConcatWithConstants(IJ)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"載入 裝備_weapon_greater_類型 ok: \u0001 (\u0001ms)"]
+INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.info(Ljava/lang/String;)V itf
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable.nativeDefinitions:Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;
+GETFIELD local/lito381/server/NativeDeItemDefinitionsScope$Data.log:Llocal/lito381/server/NativeDeWorldLog;
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable._armorTiers:Ljava/util/Map;
+INVOKEINTERFACE java/util/Map.size()I itf
+ALOAD 1
+INVOKEVIRTUAL local/lito381/server/NativeDeTerrainTimer.get()J
+INVOKEDYNAMIC makeConcatWithConstants(IJ)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"載入 裝備_armor_greater_類型 ok: \u0001 (\u0001ms)"]
+INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.info(Ljava/lang/String;)V itf
+RETURN
+method=loadArmorTiers()V
+access=0x2
+signature=
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+TRY L4 L7 L8 java/lang/Exception  
+TRY L3 L10 L11 java/lang/Throwable  
+TRY L12 L13 L14 java/lang/Throwable  
+TRY L2 L16 L17 java/lang/Throwable  
+TRY L18 L19 L20 java/lang/Throwable  
+TRY L1 L22 L23 java/lang/Throwable  
+TRY L24 L25 L26 java/lang/Throwable  
+TRY L0 L28 L29 java/sql/SQLException  
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable.nativeDefinitions:Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;
+INVOKESTATIC local/lito381/server/NativeDeItemDefinitionsScope.requireLoading(Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;)V
+LABEL L0
+INVOKESTATIC local/lito381/server/NativeDeItemDefinitionsScope$Database.get()Llocal/lito381/server/NativeDeItemDefinitionsScope$Database;
+INVOKEVIRTUAL local/lito381/server/NativeDeItemDefinitionsScope$Database.getConnection()Ljava/sql/Connection;
+ASTORE 1
+LABEL L1
+ALOAD 1
+CONST str:"SELECT * FROM `裝備_armor_greater_類型`"
+INVOKEINTERFACE java/sql/Connection.prepareStatement(Ljava/lang/String;)Ljava/sql/PreparedStatement; itf
+ASTORE 2
+LABEL L2
+ALOAD 2
+INVOKEINTERFACE java/sql/PreparedStatement.executeQuery()Ljava/sql/ResultSet; itf
+ASTORE 3
+LABEL L3
+ALOAD 3
+INVOKEINTERFACE java/sql/ResultSet.next()Z itf
+IFEQ L10
+CONST Integer:-1
+ISTORE 4
+LABEL L4
+ALOAD 3
+CONST str:"tier_id"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ISTORE 4
+ALOAD 3
+CONST str:"armor_type"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ISTORE 5
+NEW local/lito381/server/NativeDeItemAbility
+DUP
+INVOKESPECIAL local/lito381/server/NativeDeItemAbility.<init>()V
+ASTORE 6
+ALOAD 0
+ALOAD 6
+ALOAD 3
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.fillCommonAbility(Llocal/lito381/server/NativeDeItemAbility;Ljava/sql/ResultSet;)V
+ALOAD 0
+ALOAD 3
+CONST str:"add_ac"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.hasColumn(Ljava/sql/ResultSet;Ljava/lang/String;)Z
+IFEQ L5
+ALOAD 3
+CONST str:"add_ac"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ISTORE 7
+ILOAD 7
+IFEQ L5
+ALOAD 6
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.AC:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ILOAD 7
+INEG
+INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.set(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
+LABEL L5
+ALOAD 0
+ALOAD 6
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.MELEE_REDUX:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 3
+CONST str:"add_melee_redux"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 6
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.BOW_REDUX:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 3
+CONST str:"add_bow_redux"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 6
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.REGIST_FREEZE:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 3
+CONST str:"regist_freeze"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 6
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.REGIST_STONE:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 3
+CONST str:"regist_stone"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 6
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.REGIST_SLEEP:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 3
+CONST str:"regist_sleep"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 6
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.REGIST_BLIND:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 3
+CONST str:"regist_blind"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 6
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.REGIST_SUSTAIN:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 3
+CONST str:"regist_sustain"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable._armorTiers:Ljava/util/Map;
+ILOAD 4
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+INVOKEDYNAMIC apply()Ljava/util/function/Function; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeItemGreaterTierTable.lambda$loadArmorTiers$#2(Ljava/lang/Integer;)Ljava/util/Map;, type:(Ljava/lang/Integer;)Ljava/util/Map;]
+INVOKEINTERFACE java/util/Map.computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object; itf
+CHECKCAST java/util/Map
+ILOAD 5
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+ALOAD 6
+INVOKEINTERFACE java/util/Map.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; itf
+POP
+ALOAD 0
+ALOAD 3
+CONST str:"note"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.readOptionalString(Ljava/sql/ResultSet;Ljava/lang/String;)Ljava/lang/String;
+ASTORE 7
+ALOAD 7
+IFNULL L6
+ALOAD 7
+INVOKEVIRTUAL java/lang/String.isEmpty()Z
+IFNE L6
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable._armorTierNotes:Ljava/util/Map;
+ILOAD 4
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+INVOKEDYNAMIC apply()Ljava/util/function/Function; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeItemGreaterTierTable.lambda$loadArmorTiers$#1(Ljava/lang/Integer;)Ljava/util/Map;, type:(Ljava/lang/Integer;)Ljava/util/Map;]
+INVOKEINTERFACE java/util/Map.computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object; itf
+CHECKCAST java/util/Map
+ILOAD 5
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+ALOAD 7
+INVOKEINTERFACE java/util/Map.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; itf
+POP
+LABEL L6
+ALOAD 0
+ALOAD 3
+CONST str:"chance"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.readOptionalInt(Ljava/sql/ResultSet;Ljava/lang/String;)I
+ISTORE 8
+ILOAD 8
+IFLE L7
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable._armorChances:Ljava/util/Map;
+ILOAD 5
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+INVOKEDYNAMIC apply()Ljava/util/function/Function; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeItemGreaterTierTable.lambda$loadArmorTiers$#0(Ljava/lang/Integer;)Ljava/util/List;, type:(Ljava/lang/Integer;)Ljava/util/List;]
+INVOKEINTERFACE java/util/Map.computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object; itf
+CHECKCAST java/util/List
+NEW local/lito381/server/NativeDeItemGreaterTierTable$TierChance
+DUP
+ILOAD 4
+ILOAD 8
+INVOKESPECIAL local/lito381/server/NativeDeItemGreaterTierTable$TierChance.<init>(II)V
+INVOKEINTERFACE java/util/List.add(Ljava/lang/Object;)Z itf
+POP
+LABEL L7
+GOTO L9
+LABEL L8
+ASTORE 5
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable.nativeDefinitions:Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;
+GETFIELD local/lito381/server/NativeDeItemDefinitionsScope$Data.log:Llocal/lito381/server/NativeDeWorldLog;
+ILOAD 4
+INVOKEDYNAMIC makeConcatWithConstants(I)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"載入資料失敗，已略過該筆 (tier_id: \u0001)"]
+ALOAD 5
+INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
+GOTO L3
+LABEL L9
+GOTO L3
+LABEL L10
+ALOAD 3
+IFNULL L16
+ALOAD 3
+INVOKEINTERFACE java/sql/ResultSet.close()V itf
+GOTO L16
+LABEL L11
+ASTORE 4
+ALOAD 3
+IFNULL L15
+LABEL L12
+ALOAD 3
+INVOKEINTERFACE java/sql/ResultSet.close()V itf
+LABEL L13
+GOTO L15
+LABEL L14
+ASTORE 5
+ALOAD 4
+ALOAD 5
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+LABEL L15
+ALOAD 4
+ATHROW
+LABEL L16
+ALOAD 2
+IFNULL L22
+ALOAD 2
+INVOKEINTERFACE java/sql/PreparedStatement.close()V itf
+GOTO L22
+LABEL L17
+ASTORE 3
+ALOAD 2
+IFNULL L21
+LABEL L18
+ALOAD 2
+INVOKEINTERFACE java/sql/PreparedStatement.close()V itf
+LABEL L19
+GOTO L21
+LABEL L20
+ASTORE 4
+ALOAD 3
+ALOAD 4
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+LABEL L21
+ALOAD 3
+ATHROW
+LABEL L22
+ALOAD 1
+IFNULL L28
+ALOAD 1
+INVOKEINTERFACE java/sql/Connection.close()V itf
+GOTO L28
+LABEL L23
+ASTORE 2
+ALOAD 1
+IFNULL L27
+LABEL L24
+ALOAD 1
+INVOKEINTERFACE java/sql/Connection.close()V itf
+LABEL L25
+GOTO L27
+LABEL L26
+ASTORE 3
+ALOAD 2
+ALOAD 3
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+LABEL L27
+ALOAD 2
+ATHROW
+LABEL L28
+GOTO L30
+LABEL L29
+ASTORE 1
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable.nativeDefinitions:Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;
+GETFIELD local/lito381/server/NativeDeItemDefinitionsScope$Data.log:Llocal/lito381/server/NativeDeWorldLog;
+CONST str:"載入 裝備_armor_greater_類型 失敗! 請檢查資料表!"
+ALOAD 1
+INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
+LABEL L30
+RETURN
+method=loadWeaponTiers()V
+access=0x2
+signature=
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+TRY L4 L6 L7 java/lang/Exception  
+TRY L3 L9 L10 java/lang/Throwable  
+TRY L11 L12 L13 java/lang/Throwable  
+TRY L2 L15 L16 java/lang/Throwable  
+TRY L17 L18 L19 java/lang/Throwable  
+TRY L1 L21 L22 java/lang/Throwable  
+TRY L23 L24 L25 java/lang/Throwable  
+TRY L0 L27 L28 java/sql/SQLException  
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable.nativeDefinitions:Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;
+INVOKESTATIC local/lito381/server/NativeDeItemDefinitionsScope.requireLoading(Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;)V
+LABEL L0
+INVOKESTATIC local/lito381/server/NativeDeItemDefinitionsScope$Database.get()Llocal/lito381/server/NativeDeItemDefinitionsScope$Database;
+INVOKEVIRTUAL local/lito381/server/NativeDeItemDefinitionsScope$Database.getConnection()Ljava/sql/Connection;
+ASTORE 1
+LABEL L1
+ALOAD 1
+CONST str:"SELECT * FROM `裝備_weapon_greater_類型`"
+INVOKEINTERFACE java/sql/Connection.prepareStatement(Ljava/lang/String;)Ljava/sql/PreparedStatement; itf
+ASTORE 2
+LABEL L2
+ALOAD 2
+INVOKEINTERFACE java/sql/PreparedStatement.executeQuery()Ljava/sql/ResultSet; itf
+ASTORE 3
+LABEL L3
+ALOAD 3
+INVOKEINTERFACE java/sql/ResultSet.next()Z itf
+IFEQ L9
+CONST Integer:-1
+ISTORE 4
+LABEL L4
+ALOAD 3
+CONST str:"tier_id"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ISTORE 4
+ALOAD 3
+CONST str:"weapon_type"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ISTORE 5
+NEW local/lito381/server/NativeDeItemAbility
+DUP
+INVOKESPECIAL local/lito381/server/NativeDeItemAbility.<init>()V
+ASTORE 6
+ALOAD 0
+ALOAD 6
+ALOAD 3
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.fillCommonAbility(Llocal/lito381/server/NativeDeItemAbility;Ljava/sql/ResultSet;)V
+ALOAD 0
+ALOAD 6
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.BASE_DMG_SMALL:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 3
+CONST str:"add_dmg_small"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+ALOAD 6
+GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.BASE_DMG_LARGE:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+ALOAD 3
+CONST str:"add_dmg_large"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable._weaponTiers:Ljava/util/Map;
+ILOAD 4
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+INVOKEDYNAMIC apply()Ljava/util/function/Function; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeItemGreaterTierTable.lambda$loadWeaponTiers$#2(Ljava/lang/Integer;)Ljava/util/Map;, type:(Ljava/lang/Integer;)Ljava/util/Map;]
+INVOKEINTERFACE java/util/Map.computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object; itf
+CHECKCAST java/util/Map
+ILOAD 5
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+ALOAD 6
+INVOKEINTERFACE java/util/Map.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; itf
+POP
+ALOAD 0
+ALOAD 3
+CONST str:"note"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.readOptionalString(Ljava/sql/ResultSet;Ljava/lang/String;)Ljava/lang/String;
+ASTORE 7
+ALOAD 7
+IFNULL L5
+ALOAD 7
+INVOKEVIRTUAL java/lang/String.isEmpty()Z
+IFNE L5
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable._weaponTierNotes:Ljava/util/Map;
+ILOAD 4
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+INVOKEDYNAMIC apply()Ljava/util/function/Function; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeItemGreaterTierTable.lambda$loadWeaponTiers$#1(Ljava/lang/Integer;)Ljava/util/Map;, type:(Ljava/lang/Integer;)Ljava/util/Map;]
+INVOKEINTERFACE java/util/Map.computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object; itf
+CHECKCAST java/util/Map
+ILOAD 5
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+ALOAD 7
+INVOKEINTERFACE java/util/Map.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; itf
+POP
+LABEL L5
+ALOAD 0
+ALOAD 3
+CONST str:"chance"
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.readOptionalInt(Ljava/sql/ResultSet;Ljava/lang/String;)I
+ISTORE 8
+ILOAD 8
+IFLE L6
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable._weaponChances:Ljava/util/Map;
+ILOAD 5
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+INVOKEDYNAMIC apply()Ljava/util/function/Function; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeItemGreaterTierTable.lambda$loadWeaponTiers$#0(Ljava/lang/Integer;)Ljava/util/List;, type:(Ljava/lang/Integer;)Ljava/util/List;]
+INVOKEINTERFACE java/util/Map.computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object; itf
+CHECKCAST java/util/List
+NEW local/lito381/server/NativeDeItemGreaterTierTable$TierChance
+DUP
+ILOAD 4
+ILOAD 8
+INVOKESPECIAL local/lito381/server/NativeDeItemGreaterTierTable$TierChance.<init>(II)V
+INVOKEINTERFACE java/util/List.add(Ljava/lang/Object;)Z itf
+POP
+LABEL L6
+GOTO L8
+LABEL L7
+ASTORE 5
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable.nativeDefinitions:Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;
+GETFIELD local/lito381/server/NativeDeItemDefinitionsScope$Data.log:Llocal/lito381/server/NativeDeWorldLog;
+ILOAD 4
+INVOKEDYNAMIC makeConcatWithConstants(I)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"載入資料失敗，已略過該筆 (tier_id: \u0001)"]
+ALOAD 5
+INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
+GOTO L3
+LABEL L8
+GOTO L3
+LABEL L9
+ALOAD 3
+IFNULL L15
+ALOAD 3
+INVOKEINTERFACE java/sql/ResultSet.close()V itf
+GOTO L15
+LABEL L10
+ASTORE 4
+ALOAD 3
+IFNULL L14
+LABEL L11
+ALOAD 3
+INVOKEINTERFACE java/sql/ResultSet.close()V itf
+LABEL L12
+GOTO L14
+LABEL L13
+ASTORE 5
+ALOAD 4
+ALOAD 5
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+LABEL L14
+ALOAD 4
+ATHROW
+LABEL L15
+ALOAD 2
+IFNULL L21
+ALOAD 2
+INVOKEINTERFACE java/sql/PreparedStatement.close()V itf
+GOTO L21
+LABEL L16
+ASTORE 3
+ALOAD 2
+IFNULL L20
+LABEL L17
+ALOAD 2
+INVOKEINTERFACE java/sql/PreparedStatement.close()V itf
+LABEL L18
+GOTO L20
+LABEL L19
+ASTORE 4
+ALOAD 3
+ALOAD 4
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+LABEL L20
+ALOAD 3
+ATHROW
+LABEL L21
+ALOAD 1
+IFNULL L27
+ALOAD 1
+INVOKEINTERFACE java/sql/Connection.close()V itf
+GOTO L27
+LABEL L22
+ASTORE 2
+ALOAD 1
+IFNULL L26
+LABEL L23
+ALOAD 1
+INVOKEINTERFACE java/sql/Connection.close()V itf
+LABEL L24
+GOTO L26
+LABEL L25
+ASTORE 3
+ALOAD 2
+ALOAD 3
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+LABEL L26
+ALOAD 2
+ATHROW
+LABEL L27
+GOTO L29
+LABEL L28
+ASTORE 1
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable.nativeDefinitions:Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;
+GETFIELD local/lito381/server/NativeDeItemDefinitionsScope$Data.log:Llocal/lito381/server/NativeDeWorldLog;
+CONST str:"載入 裝備_weapon_greater_類型 失敗! 請檢查資料表!"
+ALOAD 1
+INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
+LABEL L29
+RETURN
+method=pickRandomTier(Ljava/util/List;)I
+access=0x2
+signature=(Ljava/util/List<Llocal/lito381/server/NativeDeItemGreaterTierTable$TierChance;>;)I
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+ALOAD 1
+IFNULL L0
+ALOAD 1
+INVOKEINTERFACE java/util/List.isEmpty()Z itf
+IFEQ L1
+LABEL L0
+CONST Integer:0
+IRETURN
+LABEL L1
+CONST Integer:0
+ISTORE 2
+ALOAD 1
+INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
+ASTORE 3
+LABEL L2
+ALOAD 3
+INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
+IFEQ L3
+ALOAD 3
+INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
+CHECKCAST local/lito381/server/NativeDeItemGreaterTierTable$TierChance
+ASTORE 4
+ILOAD 2
+ALOAD 4
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable$TierChance.chance:I
+IADD
+ISTORE 2
+GOTO L2
+LABEL L3
+ILOAD 2
+IFGT L4
+CONST Integer:0
+IRETURN
+LABEL L4
+CONST Integer:100
+ISTORE 3
+ILOAD 2
+CONST Integer:100
+IF_ICMPLE L5
+ILOAD 2
+ISTORE 3
+LABEL L5
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable._random:Ljava/util/Random;
+ILOAD 3
+INVOKEVIRTUAL java/util/Random.nextInt(I)I
+ISTORE 4
+ALOAD 1
+INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
+ASTORE 5
+LABEL L6
+ALOAD 5
+INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
+IFEQ L8
+ALOAD 5
+INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
+CHECKCAST local/lito381/server/NativeDeItemGreaterTierTable$TierChance
+ASTORE 6
+ILOAD 4
+ALOAD 6
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable$TierChance.chance:I
+ISUB
+ISTORE 4
+ILOAD 4
+IFGE L7
+ALOAD 6
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable$TierChance.tierId:I
+IRETURN
+LABEL L7
+GOTO L6
+LABEL L8
+CONST Integer:0
+IRETURN
+method=readOptionalInt(Ljava/sql/ResultSet;Ljava/lang/String;)I
+access=0x2
+signature=
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+TRY L0 L2 L3 java/sql/SQLException  
+LABEL L0
+ALOAD 0
+ALOAD 1
+ALOAD 2
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.hasColumn(Ljava/sql/ResultSet;Ljava/lang/String;)Z
+IFEQ L1
+ALOAD 1
+ALOAD 2
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+GOTO L2
+LABEL L1
+CONST Integer:0
+LABEL L2
+IRETURN
+LABEL L3
+ASTORE 3
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable.nativeDefinitions:Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;
+GETFIELD local/lito381/server/NativeDeItemDefinitionsScope$Data.log:Llocal/lito381/server/NativeDeWorldLog;
+CONST str:"Optional item greater tier column read failed: {}"
+ALOAD 2
+ALOAD 3
+INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.debug(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V itf
+CONST Integer:0
+IRETURN
+method=readOptionalString(Ljava/sql/ResultSet;Ljava/lang/String;)Ljava/lang/String;
+access=0x2
+signature=
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+TRY L0 L2 L3 java/sql/SQLException  
+LABEL L0
+ALOAD 0
+ALOAD 1
+ALOAD 2
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.hasColumn(Ljava/sql/ResultSet;Ljava/lang/String;)Z
+IFEQ L1
+ALOAD 1
+ALOAD 2
+INVOKEINTERFACE java/sql/ResultSet.getString(Ljava/lang/String;)Ljava/lang/String; itf
+GOTO L2
+LABEL L1
+CONST null
+LABEL L2
+ARETURN
+LABEL L3
+ASTORE 3
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable.nativeDefinitions:Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;
+GETFIELD local/lito381/server/NativeDeItemDefinitionsScope$Data.log:Llocal/lito381/server/NativeDeWorldLog;
+CONST str:"Optional item greater tier column read failed: {}"
+ALOAD 2
+ALOAD 3
+INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.debug(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V itf
+CONST null
+ARETURN
+method=reload()V
+access=0x1
+signature=
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable.nativeDefinitions:Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;
+INVOKESTATIC local/lito381/server/NativeDeItemDefinitionsScope.requireLoading(Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;)V
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable._weaponTiers:Ljava/util/Map;
+INVOKEINTERFACE java/util/Map.clear()V itf
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable._armorTiers:Ljava/util/Map;
+INVOKEINTERFACE java/util/Map.clear()V itf
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable._weaponChances:Ljava/util/Map;
+INVOKEINTERFACE java/util/Map.clear()V itf
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemGreaterTierTable._armorChances:Ljava/util/Map;
+INVOKEINTERFACE java/util/Map.clear()V itf
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.load()V
+RETURN
+method=safeAdd(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility$AbilityType;Ljava/sql/ResultSet;Ljava/lang/String;)V
+access=0x2
+signature=
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+TRY L0 L1 L2 java/sql/SQLException  
+LABEL L0
+ALOAD 0
+ALOAD 3
+ALOAD 4
+INVOKEVIRTUAL local/lito381/server/NativeDeItemGreaterTierTable.hasColumn(Ljava/sql/ResultSet;Ljava/lang/String;)Z
+IFEQ L1
+ALOAD 3
+ALOAD 4
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ISTORE 5
+ILOAD 5
+IFEQ L1
+ALOAD 1
+ALOAD 2
+ILOAD 5
+INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.set(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
+LABEL L1
+GOTO L3
+LABEL L2
+ASTORE 5
+LABEL L3
+RETURN
~~~

</details>

<details>
<summary>NativeDeItemGreaterTierTable$TierChance — ONLY_RIGHT</summary>

整個外層 Java 因缺失 debug API 無法編譯；TierChance 隨外層缺少。以下是正式端完整內容，沒有可比較的 javac 產物。

右側正規化 SHA-256：98715f1a160f48f01d84bc33ce28bdac8fc83123805859ff332b9cc41955d544。

~~~diff
--- staged/NativeDeItemGreaterTierTable$TierChance
+++ formal/NativeDeItemGreaterTierTable$TierChance
@@ -0,0 +1,38 @@
+class=local/lito381/server/NativeDeItemGreaterTierTable$TierChance
+access=20
+super=java/lang/Object
+interfaces=
+signature=
+outerClass=
+outerMethod=
+nestHost=local/lito381/server/NativeDeItemGreaterTierTable
+nestMembers=
+permittedSubclasses=
+annotations=|||
+innerClass=local/lito381/server/NativeDeItemGreaterTierTable$TierChance|local/lito381/server/NativeDeItemGreaterTierTable|TierChance|0xa
+recordComponents=
+field=chance:I
+access=0x10
+signature=
+value=
+annotations=|||
+field=tierId:I
+access=0x10
+signature=
+value=
+annotations=|||
+method=<init>(II)V
+access=0x0
+signature=
+exceptions=
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+annotationDefault=
+ALOAD 0
+INVOKESPECIAL java/lang/Object.<init>()V
+ALOAD 0
+ILOAD 1
+PUTFIELD local/lito381/server/NativeDeItemGreaterTierTable$TierChance.tierId:I
+ALOAD 0
+ILOAD 2
+PUTFIELD local/lito381/server/NativeDeItemGreaterTierTable$TierChance.chance:I
+RETURN
~~~

</details>

<details>
<summary>NativeDeItemIdStatsTable — DIFF</summary>

待繼續移植／主控裁定：以下包含全部 metadata、欄位及方法差異；尚未證明不同方法等價，也未證明全部無法用 Java 表達。

左側正規化 SHA-256：2fcac10695ee86abb6ae03ea9d24b127e8e2172fb40462a5632ad2d5deab0595。  
右側正規化 SHA-256：5966caf9553e5c5de26388c42302f045a7e82aa32376a488975b56dac35cc830。

~~~diff
--- staged/NativeDeItemIdStatsTable
+++ formal/NativeDeItemIdStatsTable
@@ -11,9 +11,6 @@
 annotations=|||
 innerClass=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
 innerClass=local/lito381/server/NativeDeItemAbility$AbilityType|local/lito381/server/NativeDeItemAbility|AbilityType|0x4019
-innerClass=local/lito381/server/NativeDeItemDefinitionsScope$Database|local/lito381/server/NativeDeItemDefinitionsScope|Database|0x18
-innerClass=local/lito381/server/NativeDeItemDefinitionsScope$Data|local/lito381/server/NativeDeItemDefinitionsScope|Data|0x18
-innerClass=local/lito381/server/NativeDeItemDefinitionsScope$Logs|local/lito381/server/NativeDeItemDefinitionsScope|Logs|0x18
 recordComponents=
 field=_itemIdStats:Ljava/util/Map;
 access=0x12
@@ -544,6 +541,7 @@
 INVOKEDYNAMIC makeConcatWithConstants(I)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"載入資料失敗，已略過該筆 (item_id: \u0001)"]
 ALOAD 7
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
+GOTO L3
 LABEL L8
 GOTO L3
 LABEL L9
@@ -748,6 +746,7 @@
 INVOKEDYNAMIC makeConcatWithConstants(I)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"載入資料失敗，已略過該筆 (item_id: \u0001)"]
 ALOAD 7
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
+GOTO L3
 LABEL L7
 GOTO L3
 LABEL L8
~~~

</details>

<details>
<summary>NativeDeItemInlayCardTable — DIFF</summary>

已驗證目前差異只出現在 class metadata；欄位及全部方法的本工具正規化輸出一致。metadata 是否可豁免仍待主控核准。

左側正規化 SHA-256：4290576924c393f58198faba8e35bb4adc8aa1fa9b018fc535c84d8e21e9f2b6。  
右側正規化 SHA-256：aad3c2a90c6db5765f5bd5fde8b37ae4f0c7eed7b27a9302344a853de98d27e9。

~~~diff
--- staged/NativeDeItemInlayCardTable
+++ formal/NativeDeItemInlayCardTable
@@ -10,9 +10,6 @@
 permittedSubclasses=
 annotations=|||
 innerClass=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
-innerClass=local/lito381/server/NativeDeItemDefinitionsScope$Database|local/lito381/server/NativeDeItemDefinitionsScope|Database|0x18
-innerClass=local/lito381/server/NativeDeItemDefinitionsScope$Data|local/lito381/server/NativeDeItemDefinitionsScope|Data|0x18
-innerClass=local/lito381/server/NativeDeItemDefinitionsScope$Logs|local/lito381/server/NativeDeItemDefinitionsScope|Logs|0x18
 recordComponents=
 field=_cardMap:Ljava/util/Map;
 access=0x12
~~~

</details>

<details>
<summary>NativeDeItemInventory — DIFF</summary>

待繼續移植／主控裁定：以下包含全部 metadata、欄位及方法差異；尚未證明不同方法等價，也未證明全部無法用 Java 表達。

左側正規化 SHA-256：9ce3a5fd0f1341c584da320dd2e8a9b037cd23246779913c5c114b8cbd7c9960。  
右側正規化 SHA-256：aaceeccf7a7cf429a997195c31d309b45b0deaea5238bb9710b2e865229629f8。

~~~diff
--- staged/NativeDeItemInventory
+++ formal/NativeDeItemInventory
@@ -140,9 +140,9 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault=
-TRY L0 L1 L7 java/lang/Throwable  
-TRY L2 L6 L7 java/lang/Throwable  
-TRY L7 L8 L7 java/lang/Throwable  
+TRY L0 L3 L8 java/lang/Throwable  
+TRY L4 L7 L8 java/lang/Throwable  
+TRY L8 L9 L8 java/lang/Throwable  
 ALOAD 1
 INVOKESTATIC java/util/Objects.requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
 POP
@@ -159,72 +159,73 @@
 ASTORE 5
 ALOAD 5
 INSTANCEOF local/lito381/server/NativeDeItemInventory
-IFEQ L3
+IFEQ L4
 ALOAD 5
 CHECKCAST local/lito381/server/NativeDeItemInventory
 ASTORE 4
 ALOAD 4
 GETFIELD local/lito381/server/NativeDeItemInventory.templates:Llocal/lito381/server/NativeDeItemTemplatesResources$Snapshot;
 ALOAD 1
-IF_ACMPNE L2
+IF_ACMPNE L1
 ALOAD 4
 GETFIELD local/lito381/server/NativeDeItemInventory.definitions:Llocal/lito381/server/NativeDeItemDefinitionsResources$Snapshot;
 ALOAD 2
-IF_ACMPNE L2
-ALOAD 4
-ALOAD 3
-MONITOREXIT
-LABEL L1
-ARETURN
-LABEL L2
+IF_ACMPEQ L2
+LABEL L1
 NEW java/lang/IllegalStateException
 DUP
 CONST str:"inventory already captures another source generation"
 INVOKESPECIAL java/lang/IllegalStateException.<init>(Ljava/lang/String;)V
 ATHROW
-LABEL L3
+LABEL L2
+ALOAD 4
+ALOAD 3
+MONITOREXIT
+LABEL L3
+ARETURN
+LABEL L4
 NEW local/lito381/server/NativeDeItemInventory
 DUP
 ALOAD 0
 ALOAD 1
 ALOAD 2
 INVOKESPECIAL local/lito381/server/NativeDeItemInventory.<init>(Llocal/lito381/server/RuntimeInventory;Llocal/lito381/server/NativeDeItemTemplatesResources$Snapshot;Llocal/lito381/server/NativeDeItemDefinitionsResources$Snapshot;)V
-ASTORE 5
+ASTORE 4
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/RuntimeInventory.items()Ljava/util/List;
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
-ASTORE 6
-LABEL L4
-ALOAD 6
+ASTORE 5
+LABEL L5
+ALOAD 5
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L5
-ALOAD 6
+IFEQ L6
+ALOAD 5
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/RuntimeInventory$Item
-ASTORE 7
-ALOAD 5
-ALOAD 7
+ASTORE 6
+ALOAD 4
+ALOAD 6
 INVOKEVIRTUAL local/lito381/server/RuntimeInventory$Item.objectId()J
 INVOKESTATIC java/lang/Long.valueOf(J)Ljava/lang/Long;
-ALOAD 7
+ALOAD 6
 INVOKEVIRTUAL local/lito381/server/NativeDeItemInventory.put(Ljava/lang/Long;Llocal/lito381/server/RuntimeInventory$Item;)Llocal/lito381/server/RuntimeInventory$Item;
 POP
-GOTO L4
-LABEL L5
-ALOAD 0
-ALOAD 5
+GOTO L5
+LABEL L6
+ALOAD 0
+ALOAD 4
 INVOKEVIRTUAL local/lito381/server/RuntimeInventory.nativeDeBind(Ljava/util/Map;)V
-ALOAD 5
-ALOAD 3
-MONITOREXIT
-LABEL L6
-ARETURN
+ALOAD 4
+ALOAD 3
+MONITOREXIT
 LABEL L7
-ASTORE 8
-ALOAD 3
-MONITOREXIT
+ARETURN
 LABEL L8
-ALOAD 8
+ASTORE 7
+ALOAD 3
+MONITOREXIT
+LABEL L9
+ALOAD 7
 ATHROW
 method=changed(Llocal/lito381/server/NativeDeItemInventory$BoundItem;)V
 access=0x2
@@ -563,9 +564,9 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault=
-TRY L0 L5 L8 java/lang/Throwable  
-TRY L6 L7 L8 java/lang/Throwable  
-TRY L8 L9 L8 java/lang/Throwable  
+TRY L0 L2 L9 java/lang/Throwable  
+TRY L3 L8 L9 java/lang/Throwable  
+TRY L9 L10 L9 java/lang/Throwable  
 ALOAD 0
 DUP
 ASTORE 2
@@ -576,10 +577,18 @@
 ASTORE 4
 ALOAD 4
 INSTANCEOF local/lito381/server/NativeDeItemInventory
-IFEQ L6
+IFEQ L1
 ALOAD 4
 CHECKCAST local/lito381/server/NativeDeItemInventory
 ASTORE 3
+GOTO L3
+LABEL L1
+ALOAD 1
+ALOAD 2
+MONITOREXIT
+LABEL L2
+ARETURN
+LABEL L3
 NEW local/lito381/server/NativeDeItemInventory
 DUP
 ALOAD 1
@@ -615,10 +624,10 @@
 INVOKEVIRTUAL local/lito381/server/RuntimeInventory.items()Ljava/util/List;
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
 ASTORE 5
-LABEL L1
+LABEL L4
 ALOAD 5
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L4
+IFEQ L7
 ALOAD 5
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/RuntimeInventory$Item
@@ -632,18 +641,18 @@
 CHECKCAST local/lito381/server/NativeDeItemInventory$EntryState
 ASTORE 7
 ALOAD 7
-IFNONNULL L2
+IFNONNULL L5
 NEW java/lang/IllegalStateException
 DUP
 CONST str:"copy introduced an unknown object"
 INVOKESPECIAL java/lang/IllegalStateException.<init>(Ljava/lang/String;)V
 ATHROW
-LABEL L2
+LABEL L5
 CONST null
 ASTORE 8
 ALOAD 7
 GETFIELD local/lito381/server/NativeDeItemInventory$EntryState.actor:Llocal/lito381/server/NativeDeItemInventory$BoundItem;
-IFNULL L3
+IFNULL L6
 NEW local/lito381/server/NativeDeItemInventory$BoundItem
 DUP
 ALOAD 4
@@ -654,7 +663,7 @@
 GETFIELD local/lito381/server/NativeDeItemInventory$EntryState.actor:Llocal/lito381/server/NativeDeItemInventory$BoundItem;
 ALOAD 8
 INVOKESTATIC local/lito381/server/NativeDeItemStateCopy.copy(Llocal/lito381/server/NativeDeWorldKinds$Item;Llocal/lito381/server/NativeDeWorldKinds$Item;)V
-LABEL L3
+LABEL L6
 ALOAD 4
 GETFIELD local/lito381/server/NativeDeItemInventory.entries:Ljava/util/LinkedHashMap;
 ALOAD 6
@@ -667,27 +676,21 @@
 INVOKESPECIAL local/lito381/server/NativeDeItemInventory$EntryState.<init>(Llocal/lito381/server/RuntimeInventory$Item;Llocal/lito381/server/NativeDeItemInventory$BoundItem;)V
 INVOKEVIRTUAL java/util/LinkedHashMap.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
 POP
-GOTO L1
-LABEL L4
+GOTO L4
+LABEL L7
 ALOAD 1
 ALOAD 4
 INVOKEVIRTUAL local/lito381/server/RuntimeInventory.nativeDeBind(Ljava/util/Map;)V
 ALOAD 1
 ALOAD 2
 MONITOREXIT
-LABEL L5
-ARETURN
-LABEL L6
-ALOAD 1
-ALOAD 2
-MONITOREXIT
-LABEL L7
-ARETURN
 LABEL L8
+ARETURN
+LABEL L9
 ASTORE 9
 ALOAD 2
 MONITOREXIT
-LABEL L9
+LABEL L10
 ALOAD 9
 ATHROW
 method=put(Ljava/lang/Long;Llocal/lito381/server/RuntimeInventory$Item;)Llocal/lito381/server/RuntimeInventory$Item;
@@ -696,25 +699,32 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault=
-TRY L0 L10 L11 java/lang/Throwable  
-TRY L11 L12 L11 java/lang/Throwable  
+TRY L2 L12 L13 java/lang/Throwable  
+TRY L13 L14 L13 java/lang/Throwable  
 ALOAD 2
 INVOKESTATIC java/util/Objects.requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
 POP
 ALOAD 1
-IFNULL L13
+IFNULL L0
 ALOAD 1
 INVOKEVIRTUAL java/lang/Long.longValue()J
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/RuntimeInventory$Item.objectId()J
 LCMP
-IFNE L13
+IFEQ L1
+LABEL L0
+NEW java/lang/IllegalArgumentException
+DUP
+CONST str:"inventory object identity mismatch"
+INVOKESPECIAL java/lang/IllegalArgumentException.<init>(Ljava/lang/String;)V
+ATHROW
+LABEL L1
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeItemInventory.inventory:Llocal/lito381/server/RuntimeInventory;
 DUP
 ASTORE 3
 MONITORENTER
-LABEL L0
+LABEL L2
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeItemInventory.entries:Ljava/util/LinkedHashMap;
 ALOAD 1
@@ -722,14 +732,14 @@
 CHECKCAST local/lito381/server/NativeDeItemInventory$EntryState
 ASTORE 4
 ALOAD 4
-IFNONNULL L1
+IFNONNULL L3
 CONST null
-GOTO L2
-LABEL L1
+GOTO L4
+LABEL L3
 ALOAD 0
 ALOAD 4
 INVOKEVIRTUAL local/lito381/server/NativeDeItemInventory.snapshot(Llocal/lito381/server/NativeDeItemInventory$EntryState;)Llocal/lito381/server/RuntimeInventory$Item;
-LABEL L2
+LABEL L4
 ASTORE 5
 ALOAD 0
 ALOAD 2
@@ -737,18 +747,18 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeItemInventory.template(Llocal/lito381/server/InventoryCatalog$Definition;)Llocal/lito381/server/NativeDeItemTemplate;
 ASTORE 6
 ALOAD 4
+IFNONNULL L7
+ALOAD 6
 IFNONNULL L5
-ALOAD 6
-IFNONNULL L3
 CONST null
-GOTO L4
-LABEL L3
+GOTO L6
+LABEL L5
 NEW local/lito381/server/NativeDeItemInventory$BoundItem
 DUP
 ALOAD 0
 ALOAD 2
 INVOKESPECIAL local/lito381/server/NativeDeItemInventory$BoundItem.<init>(Llocal/lito381/server/NativeDeItemInventory;Llocal/lito381/server/RuntimeInventory$Item;)V
-LABEL L4
+LABEL L6
 ASTORE 7
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeItemInventory.entries:Ljava/util/LinkedHashMap;
@@ -760,24 +770,24 @@
 INVOKESPECIAL local/lito381/server/NativeDeItemInventory$EntryState.<init>(Llocal/lito381/server/RuntimeInventory$Item;Llocal/lito381/server/NativeDeItemInventory$BoundItem;)V
 INVOKEVIRTUAL java/util/LinkedHashMap.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
 POP
-GOTO L9
-LABEL L5
-ALOAD 4
-GETFIELD local/lito381/server/NativeDeItemInventory$EntryState.actor:Llocal/lito381/server/NativeDeItemInventory$BoundItem;
-IFNULL L6
-ALOAD 6
-IFNONNULL L6
+GOTO L11
+LABEL L7
+ALOAD 4
+GETFIELD local/lito381/server/NativeDeItemInventory$EntryState.actor:Llocal/lito381/server/NativeDeItemInventory$BoundItem;
+IFNULL L8
+ALOAD 6
+IFNONNULL L8
 NEW java/lang/IllegalArgumentException
 DUP
 CONST str:"bound item morph has no captured source template"
 INVOKESPECIAL java/lang/IllegalArgumentException.<init>(Ljava/lang/String;)V
 ATHROW
-LABEL L6
-ALOAD 4
-GETFIELD local/lito381/server/NativeDeItemInventory$EntryState.actor:Llocal/lito381/server/NativeDeItemInventory$BoundItem;
-IFNONNULL L7
-ALOAD 6
-IFNULL L7
+LABEL L8
+ALOAD 4
+GETFIELD local/lito381/server/NativeDeItemInventory$EntryState.actor:Llocal/lito381/server/NativeDeItemInventory$BoundItem;
+IFNONNULL L9
+ALOAD 6
+IFNULL L9
 ALOAD 4
 NEW local/lito381/server/NativeDeItemInventory$BoundItem
 DUP
@@ -785,21 +795,21 @@
 ALOAD 2
 INVOKESPECIAL local/lito381/server/NativeDeItemInventory$BoundItem.<init>(Llocal/lito381/server/NativeDeItemInventory;Llocal/lito381/server/RuntimeInventory$Item;)V
 PUTFIELD local/lito381/server/NativeDeItemInventory$EntryState.actor:Llocal/lito381/server/NativeDeItemInventory$BoundItem;
-GOTO L8
-LABEL L7
-ALOAD 4
-GETFIELD local/lito381/server/NativeDeItemInventory$EntryState.actor:Llocal/lito381/server/NativeDeItemInventory$BoundItem;
-IFNULL L8
+GOTO L10
+LABEL L9
+ALOAD 4
+GETFIELD local/lito381/server/NativeDeItemInventory$EntryState.actor:Llocal/lito381/server/NativeDeItemInventory$BoundItem;
+IFNULL L10
 ALOAD 4
 GETFIELD local/lito381/server/NativeDeItemInventory$EntryState.actor:Llocal/lito381/server/NativeDeItemInventory$BoundItem;
 ALOAD 2
 ALOAD 6
 INVOKEVIRTUAL local/lito381/server/NativeDeItemInventory$BoundItem.apply(Llocal/lito381/server/RuntimeInventory$Item;Llocal/lito381/server/NativeDeItemTemplate;)V
-LABEL L8
+LABEL L10
 ALOAD 4
 ALOAD 2
 PUTFIELD local/lito381/server/NativeDeItemInventory$EntryState.view:Llocal/lito381/server/RuntimeInventory$Item;
-LABEL L9
+LABEL L11
 ALOAD 0
 ALOAD 1
 INVOKEVIRTUAL java/lang/Long.longValue()J
@@ -815,20 +825,14 @@
 ALOAD 5
 ALOAD 3
 MONITOREXIT
-LABEL L10
-ARETURN
-LABEL L11
+LABEL L12
+ARETURN
+LABEL L13
 ASTORE 8
 ALOAD 3
 MONITOREXIT
-LABEL L12
+LABEL L14
 ALOAD 8
-ATHROW
-LABEL L13
-NEW java/lang/IllegalArgumentException
-DUP
-CONST str:"inventory object identity mismatch"
-INVOKESPECIAL java/lang/IllegalArgumentException.<init>(Ljava/lang/String;)V
 ATHROW
 method=put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
 access=0x41
~~~

</details>

<details>
<summary>NativeDeItemMagicStoneTable — DIFF</summary>

已驗證目前差異只出現在 class metadata；欄位及全部方法的本工具正規化輸出一致。metadata 是否可豁免仍待主控核准。

左側正規化 SHA-256：d093936f99def21506c03d35dee7e97bf19cba05c9cbb282faa57c08f69ff29b。  
右側正規化 SHA-256：b706ab02498af1dc35788d8dc87c30ef60b5f7d8fc1fd36511e212338ac565e3。

~~~diff
--- staged/NativeDeItemMagicStoneTable
+++ formal/NativeDeItemMagicStoneTable
@@ -10,10 +10,6 @@
 permittedSubclasses=
 annotations=|||
 innerClass=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
-innerClass=local/lito381/server/NativeDeItemDefinitionsScope$Database|local/lito381/server/NativeDeItemDefinitionsScope|Database|0x18
-innerClass=local/lito381/server/NativeDeItemDefinitionsScope$Data|local/lito381/server/NativeDeItemDefinitionsScope|Data|0x18
-innerClass=local/lito381/server/NativeDeItemDefinitionsScope$Logs|local/lito381/server/NativeDeItemDefinitionsScope|Logs|0x18
-innerClass=local/lito381/server/NativeDeItemDefinitionsScope$TableLog|local/lito381/server/NativeDeItemDefinitionsScope|TableLog|0x18
 recordComponents=
 field=_log:Llocal/lito381/server/NativeDeWorldLog;
 access=0x1a
~~~

</details>

<details>
<summary>NativeDeItemPublication — DIFF</summary>

待繼續移植／主控裁定：以下包含全部 metadata、欄位及方法差異；尚未證明不同方法等價，也未證明全部無法用 Java 表達。

左側正規化 SHA-256：8b0a641d540eadcc76dc2a16823a1ea1e81e953c0b552f0a0587a538252c5254。  
右側正規化 SHA-256：ab6d4bdd78e12794278daac55a62e78de16a35aab7d720222ec6c66b583e2deb。

~~~diff
--- staged/NativeDeItemPublication
+++ formal/NativeDeItemPublication
@@ -9,7 +9,6 @@
 nestMembers=local/lito381/server/NativeDeItemPublication$Ticket
 permittedSubclasses=
 annotations=|||
-innerClass=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
 innerClass=local/lito381/server/NativeDeItemPublication$Ticket|local/lito381/server/NativeDeItemPublication|Ticket|0x1a
 innerClass=local/lito381/server/NativeDeItemTransaction$Change|local/lito381/server/NativeDeItemTransaction|Change|0x18
 recordComponents=
@@ -154,8 +153,8 @@
 ALOAD 0
 CONST str:"sourceItemAdmissionGate"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
-DUP
 ASTORE 7
+ALOAD 7
 DUP
 ASTORE 8
 MONITORENTER
~~~

</details>

<details>
<summary>NativeDeItemStateCopy — DIFF</summary>

待繼續移植／主控裁定：以下包含全部 metadata、欄位及方法差異；尚未證明不同方法等價，也未證明全部無法用 Java 表達。

左側正規化 SHA-256：a8a9a505ae1acd6b50d512f715b077507cb2d56bcd79507ec1ea7123b72c3d6f。  
右側正規化 SHA-256：0cdd1d97e9d84e1e588ab3cc4dc68815718f4ecd4823f3e3fe1336ce814ddbba。

~~~diff
--- staged/NativeDeItemStateCopy
+++ formal/NativeDeItemStateCopy
@@ -103,8 +103,7 @@
 INVOKEVIRTUAL java/lang/Long.longValue()J
 CONST Long:0
 LCMP
-IFGE L7
-GOTO L8
+IFLT L8
 LABEL L7
 NEW java/lang/IllegalStateException
 DUP
@@ -242,20 +241,20 @@
 IFEQ L4
 ALOAD 8
 CHECKCAST java/util/concurrent/atomic/AtomicInteger
-ASTORE 10
+ASTORE 12
 ALOAD 9
 INSTANCEOF java/util/concurrent/atomic/AtomicInteger
 IFEQ L4
 ALOAD 9
 CHECKCAST java/util/concurrent/atomic/AtomicInteger
-ASTORE 11
-ALOAD 11
-ALOAD 10
+ASTORE 13
+ALOAD 13
+ALOAD 12
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicInteger.get()I
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicInteger.set(I)V
 ALOAD 3
-ALOAD 10
-ALOAD 11
+ALOAD 12
+ALOAD 13
 INVOKEVIRTUAL java/util/IdentityHashMap.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
 POP
 GOTO L9
@@ -265,22 +264,22 @@
 IFEQ L5
 ALOAD 8
 CHECKCAST java/util/concurrent/atomic/AtomicReference
-ASTORE 10
+ASTORE 14
 ALOAD 9
 INSTANCEOF java/util/concurrent/atomic/AtomicReference
 IFEQ L5
 ALOAD 9
 CHECKCAST java/util/concurrent/atomic/AtomicReference
-ASTORE 11
-ALOAD 11
-ALOAD 10
+ASTORE 15
+ALOAD 15
+ALOAD 14
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicReference.get()Ljava/lang/Object;
 ALOAD 3
 INVOKESTATIC local/lito381/server/NativeDeItemStateCopy.deep(Ljava/lang/Object;Ljava/util/IdentityHashMap;)Ljava/lang/Object;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicReference.set(Ljava/lang/Object;)V
 ALOAD 3
-ALOAD 10
-ALOAD 11
+ALOAD 14
+ALOAD 15
 INVOKEVIRTUAL java/util/IdentityHashMap.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
 POP
 GOTO L9
@@ -290,19 +289,19 @@
 IFEQ L6
 ALOAD 8
 CHECKCAST local/lito381/server/NativeDeItemAbility
-ASTORE 10
+ASTORE 16
 ALOAD 9
 INSTANCEOF local/lito381/server/NativeDeItemAbility
 IFEQ L6
 ALOAD 9
 CHECKCAST local/lito381/server/NativeDeItemAbility
-ASTORE 11
-ALOAD 10
-ALOAD 11
+ASTORE 17
+ALOAD 16
+ALOAD 17
 INVOKESTATIC local/lito381/server/NativeDeItemStateCopy.copyAbility(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility;)V
 ALOAD 3
-ALOAD 10
-ALOAD 11
+ALOAD 16
+ALOAD 17
 INVOKEVIRTUAL java/util/IdentityHashMap.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
 POP
 GOTO L9
@@ -340,17 +339,17 @@
 IFEQ L8
 ALOAD 8
 CHECKCAST java/util/List
-ASTORE 10
+ASTORE 18
 ALOAD 9
 INSTANCEOF java/util/List
 IFEQ L8
 ALOAD 9
 CHECKCAST java/util/List
-ASTORE 11
-ALOAD 11
+ASTORE 19
+ALOAD 19
 INVOKEINTERFACE java/util/List.clear()V itf
-ALOAD 11
-ALOAD 10
+ALOAD 19
+ALOAD 18
 INVOKEINTERFACE java/util/List.addAll(Ljava/util/Collection;)Z itf
 POP
 ALOAD 3
@@ -415,18 +414,18 @@
 IFEQ L3
 ALOAD 0
 CHECKCAST java/sql/Timestamp
-ASTORE 3
+ASTORE 4
 NEW java/sql/Timestamp
 DUP
-ALOAD 3
+ALOAD 4
 INVOKEVIRTUAL java/sql/Timestamp.getTime()J
 INVOKESPECIAL java/sql/Timestamp.<init>(J)V
-ASTORE 4
+ASTORE 9
+ALOAD 9
 ALOAD 4
-ALOAD 3
 INVOKEVIRTUAL java/sql/Timestamp.getNanos()I
 INVOKEVIRTUAL java/sql/Timestamp.setNanos(I)V
-ALOAD 4
+ALOAD 9
 ASTORE 2
 GOTO L9
 LABEL L3
@@ -435,8 +434,8 @@
 IFEQ L4
 ALOAD 0
 CHECKCAST [I
-ASTORE 3
-ALOAD 3
+ASTORE 5
+ALOAD 5
 INVOKEVIRTUAL [I.clone()Ljava/lang/Object;
 ASTORE 2
 GOTO L9
@@ -446,8 +445,8 @@
 IFEQ L5
 ALOAD 0
 CHECKCAST [B
-ASTORE 3
-ALOAD 3
+ASTORE 6
+ALOAD 6
 INVOKEVIRTUAL [B.clone()Ljava/lang/Object;
 ASTORE 2
 GOTO L9
@@ -457,23 +456,23 @@
 IFEQ L6
 ALOAD 0
 CHECKCAST local/lito381/server/NativeDeDarkItem
-ASTORE 3
+ASTORE 7
 NEW local/lito381/server/NativeDeDarkItem
 DUP
-ALOAD 3
+ALOAD 7
 INVOKEVIRTUAL local/lito381/server/NativeDeDarkItem.getItemObjId()I
-ALOAD 3
+ALOAD 7
 INVOKEVIRTUAL local/lito381/server/NativeDeDarkItem.getConfigId()I
-ALOAD 3
+ALOAD 7
 INVOKEVIRTUAL local/lito381/server/NativeDeDarkItem.getTitle()Ljava/lang/String;
 INVOKESPECIAL local/lito381/server/NativeDeDarkItem.<init>(IILjava/lang/String;)V
-ASTORE 4
-ALOAD 3
+ASTORE 9
+ALOAD 7
 INVOKEVIRTUAL local/lito381/server/NativeDeDarkItem.getAbility()Llocal/lito381/server/NativeDeItemAbility;
-ALOAD 4
+ALOAD 9
 INVOKEVIRTUAL local/lito381/server/NativeDeDarkItem.getAbility()Llocal/lito381/server/NativeDeItemAbility;
 INVOKESTATIC local/lito381/server/NativeDeItemStateCopy.copyAbility(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility;)V
-ALOAD 4
+ALOAD 9
 ASTORE 2
 GOTO L9
 LABEL L6
@@ -482,29 +481,29 @@
 IFEQ L7
 ALOAD 0
 CHECKCAST local/lito381/server/NativeDeItemVipTemplate
-ASTORE 3
+ASTORE 8
 NEW local/lito381/server/NativeDeItemVipTemplate
 DUP
 INVOKESPECIAL local/lito381/server/NativeDeItemVipTemplate.<init>()V
-ASTORE 4
-ALOAD 4
-ALOAD 3
+ASTORE 9
+ALOAD 9
+ALOAD 8
 INVOKEVIRTUAL local/lito381/server/NativeDeItemVipTemplate.get_type()I
 INVOKEVIRTUAL local/lito381/server/NativeDeItemVipTemplate.set_type(I)V
-ALOAD 4
-ALOAD 3
+ALOAD 9
+ALOAD 8
 INVOKEVIRTUAL local/lito381/server/NativeDeItemVipTemplate.get_effect_icon()I
 INVOKEVIRTUAL local/lito381/server/NativeDeItemVipTemplate.set_effect_icon(I)V
-ALOAD 4
-ALOAD 3
+ALOAD 9
+ALOAD 8
 INVOKEVIRTUAL local/lito381/server/NativeDeItemVipTemplate.get_polyId()I
 INVOKEVIRTUAL local/lito381/server/NativeDeItemVipTemplate.set_polyId(I)V
-ALOAD 3
+ALOAD 8
 INVOKEVIRTUAL local/lito381/server/NativeDeItemVipTemplate.getAbility()Llocal/lito381/server/NativeDeItemAbility;
-ALOAD 4
+ALOAD 9
 INVOKEVIRTUAL local/lito381/server/NativeDeItemVipTemplate.getAbility()Llocal/lito381/server/NativeDeItemAbility;
 INVOKESTATIC local/lito381/server/NativeDeItemStateCopy.copyAbility(Llocal/lito381/server/NativeDeItemAbility;Llocal/lito381/server/NativeDeItemAbility;)V
-ALOAD 4
+ALOAD 9
 ASTORE 2
 GOTO L9
 LABEL L7
@@ -518,19 +517,19 @@
 INVOKEVIRTUAL java/lang/Object.getClass()Ljava/lang/Class;
 CONST str:"objectId"
 INVOKEVIRTUAL java/lang/Class.getDeclaredField(Ljava/lang/String;)Ljava/lang/reflect/Field;
-ASTORE 4
-ALOAD 4
+ASTORE 9
+ALOAD 9
 CONST Integer:1
 INVOKEVIRTUAL java/lang/reflect/Field.setAccessible(Z)V
-ALOAD 4
+ALOAD 9
 ALOAD 3
 INVOKEVIRTUAL java/lang/reflect/Field.getInt(Ljava/lang/Object;)I
-ISTORE 5
+ISTORE 10
 NEW local/lito381/server/NativeDeItemMetadata
 DUP
-ILOAD 5
-ALOAD 3
-ILOAD 5
+ILOAD 10
+ALOAD 3
+ILOAD 10
 INVOKEVIRTUAL local/lito381/server/NativeDeItemMetadata.getDarkItem(I)Llocal/lito381/server/NativeDeDarkItem;
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeDeItemStateCopy.deep(Ljava/lang/Object;Ljava/util/IdentityHashMap;)Ljava/lang/Object;
~~~

</details>

<details>
<summary>NativeDeItemTable — DIFF</summary>

待繼續移植／主控裁定：以下包含全部 metadata、欄位及方法差異；尚未證明不同方法等價，也未證明全部無法用 Java 表達。

左側正規化 SHA-256：ab01546bf11a14ce6b0f87e1199cfc42a0353ff50d2a9a3165a8a73ee7fed21b。  
右側正規化 SHA-256：0ad23e828a4f7c0eceb2c1036824972c9ceac9a5caa5e6dd207a3dabc700f9ac。

~~~diff
--- staged/NativeDeItemTable
+++ formal/NativeDeItemTable
@@ -9,11 +9,8 @@
 nestMembers=
 permittedSubclasses=
 annotations=|||
+innerClass=com/lineage/server/model/item/ItemGenerationContext$Source|com/lineage/server/model/item/ItemGenerationContext|Source|0x4019
 innerClass=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
-innerClass=local/lito381/server/NativeDeItemTemplatesScope$Classes|local/lito381/server/NativeDeItemTemplatesScope|Classes|0x18
-innerClass=local/lito381/server/NativeDeItemTemplatesScope$Database|local/lito381/server/NativeDeItemTemplatesScope|Database|0x18
-innerClass=local/lito381/server/NativeDeItemTemplatesScope$Data|local/lito381/server/NativeDeItemTemplatesScope|Data|0x18
-innerClass=local/lito381/server/NativeDeItemTemplatesScope$Logs|local/lito381/server/NativeDeItemTemplatesScope|Logs|0x18
 recordComponents=
 field=_allTemplates:[Llocal/lito381/server/NativeDeItemTemplate;
 access=0x2
@@ -21,7 +18,7 @@
 value=
 annotations=|||
 field=_armorTypes:Ljava/util/Map;
-access=0xa
+access=0x1a
 signature=Ljava/util/Map<Ljava/lang/String;Ljava/lang/Integer;>;
 value=
 annotations=|||
@@ -31,7 +28,7 @@
 value=
 annotations=|||
 field=_etcItemTypes:Ljava/util/Map;
-access=0xa
+access=0x1a
 signature=Ljava/util/Map<Ljava/lang/String;Ljava/lang/Integer;>;
 value=
 annotations=|||
@@ -46,22 +43,22 @@
 value=
 annotations=|||
 field=_materialTypes:Ljava/util/Map;
-access=0xa
+access=0x1a
 signature=Ljava/util/Map<Ljava/lang/String;Ljava/lang/Integer;>;
 value=
 annotations=|||
 field=_useTypes:Ljava/util/Map;
-access=0xa
+access=0x1a
 signature=Ljava/util/Map<Ljava/lang/String;Ljava/lang/Integer;>;
 value=
 annotations=|||
 field=_weaponId:Ljava/util/Map;
-access=0xa
+access=0x1a
 signature=Ljava/util/Map<Ljava/lang/String;Ljava/lang/Integer;>;
 value=
 annotations=|||
 field=_weaponTypes:Ljava/util/Map;
-access=0xa
+access=0x1a
 signature=Ljava/util/Map<Ljava/lang/String;Ljava/lang/Integer;>;
 value=
 annotations=|||
@@ -1162,13 +1159,13 @@
 TRY L25 L26 L27 java/sql/SQLException  
 TRY L28 L29 L30 java/sql/SQLException  
 TRY L4 L32 L33 java/lang/Exception  
-TRY L3 L34 L35 java/lang/Throwable  
-TRY L35 L36 L35 java/lang/Throwable  
-TRY L2 L38 L39 java/lang/Throwable  
-TRY L40 L41 L42 java/lang/Throwable  
-TRY L1 L44 L45 java/lang/Throwable  
-TRY L46 L47 L48 java/lang/Throwable  
-TRY L0 L50 L51 java/sql/SQLException  
+TRY L3 L35 L36 java/lang/Throwable  
+TRY L37 L38 L39 java/lang/Throwable  
+TRY L2 L41 L42 java/lang/Throwable  
+TRY L43 L44 L45 java/lang/Throwable  
+TRY L1 L47 L48 java/lang/Throwable  
+TRY L49 L50 L51 java/lang/Throwable  
+TRY L0 L53 L54 java/sql/SQLException  
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeItemTable.nativeTemplates:Llocal/lito381/server/NativeDeItemTemplatesScope$Data;
 INVOKESTATIC local/lito381/server/NativeDeItemTemplatesScope.requireLoading(Llocal/lito381/server/NativeDeItemTemplatesScope$Data;)V
@@ -1192,7 +1189,7 @@
 LABEL L3
 ALOAD 4
 INVOKEINTERFACE java/sql/ResultSet.next()Z itf
-IFEQ L34
+IFEQ L35
 CONST Integer:-1
 ISTORE 5
 LABEL L4
@@ -1288,77 +1285,77 @@
 ALOAD 4
 CONST str:"use_royal"
 INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-IFEQ L5
-CONST Integer:1
+IFNE L5
+CONST Integer:0
 GOTO L6
 LABEL L5
-CONST Integer:0
+CONST Integer:1
 LABEL L6
 INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.setUseRoyal(Z)V
 ALOAD 6
 ALOAD 4
 CONST str:"use_knight"
 INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-IFEQ L7
-CONST Integer:1
+IFNE L7
+CONST Integer:0
 GOTO L8
 LABEL L7
-CONST Integer:0
+CONST Integer:1
 LABEL L8
 INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.setUseKnight(Z)V
 ALOAD 6
 ALOAD 4
 CONST str:"use_elf"
 INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-IFEQ L9
-CONST Integer:1
+IFNE L9
+CONST Integer:0
 GOTO L10
 LABEL L9
-CONST Integer:0
+CONST Integer:1
 LABEL L10
 INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.setUseElf(Z)V
 ALOAD 6
 ALOAD 4
 CONST str:"use_mage"
 INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-IFEQ L11
-CONST Integer:1
+IFNE L11
+CONST Integer:0
 GOTO L12
 LABEL L11
-CONST Integer:0
+CONST Integer:1
 LABEL L12
 INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.setUseMage(Z)V
 ALOAD 6
 ALOAD 4
 CONST str:"use_darkelf"
 INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-IFEQ L13
-CONST Integer:1
+IFNE L13
+CONST Integer:0
 GOTO L14
 LABEL L13
-CONST Integer:0
+CONST Integer:1
 LABEL L14
 INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.setUseDarkelf(Z)V
 ALOAD 6
 ALOAD 4
 CONST str:"use_dragonknight"
 INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-IFEQ L15
-CONST Integer:1
+IFNE L15
+CONST Integer:0
 GOTO L16
 LABEL L15
-CONST Integer:0
+CONST Integer:1
 LABEL L16
 INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.setUseDragonknight(Z)V
 ALOAD 6
 ALOAD 4
 CONST str:"use_illusionist"
 INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-IFEQ L17
-CONST Integer:1
+IFNE L17
+CONST Integer:0
 GOTO L18
 LABEL L17
-CONST Integer:0
+CONST Integer:1
 LABEL L18
 INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.setUseIllusionist(Z)V
 ALOAD 6
@@ -1465,11 +1462,11 @@
 ALOAD 4
 CONST str:"haste_item"
 INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-IFEQ L19
-CONST Integer:1
+IFNE L19
+CONST Integer:0
 GOTO L20
 LABEL L19
-CONST Integer:0
+CONST Integer:1
 LABEL L20
 INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.setHasteItem(Z)V
 ALOAD 6
@@ -1704,10 +1701,10 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getItemId()I
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 ALOAD 6
-INVOKEVIRTUAL java/util/HashMap.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
+INVOKEINTERFACE java/util/Map.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; itf
 POP
 LABEL L32
-GOTO L3
+GOTO L34
 LABEL L33
 ASTORE 6
 ALOAD 0
@@ -1719,70 +1716,79 @@
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
 GOTO L3
 LABEL L34
-ALOAD 4
-IFNULL L38
+GOTO L3
+LABEL L35
+ALOAD 4
+IFNULL L41
 ALOAD 4
 INVOKEINTERFACE java/sql/ResultSet.close()V itf
-GOTO L38
-LABEL L35
-ASTORE 9
+GOTO L41
 LABEL L36
-ALOAD 4
-IFNULL L37
+ASTORE 5
+ALOAD 4
+IFNULL L40
+LABEL L37
 ALOAD 4
 INVOKEINTERFACE java/sql/ResultSet.close()V itf
-LABEL L37
-ALOAD 9
+LABEL L38
+GOTO L40
+LABEL L39
+ASTORE 6
+ALOAD 5
+ALOAD 6
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+LABEL L40
+ALOAD 5
 ATHROW
-LABEL L38
+LABEL L41
 ALOAD 3
-IFNULL L44
+IFNULL L47
 ALOAD 3
 INVOKEINTERFACE java/sql/PreparedStatement.close()V itf
-GOTO L44
-LABEL L39
+GOTO L47
+LABEL L42
 ASTORE 4
 ALOAD 3
-IFNULL L43
-LABEL L40
+IFNULL L46
+LABEL L43
 ALOAD 3
 INVOKEINTERFACE java/sql/PreparedStatement.close()V itf
-LABEL L41
-GOTO L43
-LABEL L42
+LABEL L44
+GOTO L46
+LABEL L45
 ASTORE 5
 ALOAD 4
 ALOAD 5
 INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
-LABEL L43
+LABEL L46
 ALOAD 4
 ATHROW
-LABEL L44
-ALOAD 2
-IFNULL L50
+LABEL L47
+ALOAD 2
+IFNULL L53
 ALOAD 2
 INVOKEINTERFACE java/sql/Connection.close()V itf
-GOTO L50
-LABEL L45
+GOTO L53
+LABEL L48
 ASTORE 3
 ALOAD 2
-IFNULL L49
-LABEL L46
+IFNULL L52
+LABEL L49
 ALOAD 2
 INVOKEINTERFACE java/sql/Connection.close()V itf
-LABEL L47
-GOTO L49
-LABEL L48
-ASTORE 4
-ALOAD 3
-ALOAD 4
-INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
-LABEL L49
-ALOAD 3
-ATHROW
 LABEL L50
 GOTO L52
 LABEL L51
+ASTORE 4
+ALOAD 3
+ALOAD 4
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+LABEL L52
+ALOAD 3
+ATHROW
+LABEL L53
+GOTO L55
+LABEL L54
 ASTORE 2
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeItemTable.nativeTemplates:Llocal/lito381/server/NativeDeItemTemplatesScope$Data;
@@ -1790,7 +1796,7 @@
 CONST str:"載入 armor 失敗! 請檢查資料表!"
 ALOAD 2
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
-LABEL L52
+LABEL L55
 ALOAD 1
 ARETURN
 method=allEtcItem()Ljava/util/Map;
@@ -1801,13 +1807,13 @@
 annotationDefault=
 TRY L4 L13 L16 java/lang/Exception  
 TRY L14 L15 L16 java/lang/Exception  
-TRY L3 L17 L18 java/lang/Throwable  
-TRY L18 L19 L18 java/lang/Throwable  
-TRY L2 L21 L22 java/lang/Throwable  
-TRY L23 L24 L25 java/lang/Throwable  
-TRY L1 L27 L28 java/lang/Throwable  
-TRY L29 L30 L31 java/lang/Throwable  
-TRY L0 L33 L34 java/sql/SQLException  
+TRY L3 L18 L19 java/lang/Throwable  
+TRY L20 L21 L22 java/lang/Throwable  
+TRY L2 L24 L25 java/lang/Throwable  
+TRY L26 L27 L28 java/lang/Throwable  
+TRY L1 L30 L31 java/lang/Throwable  
+TRY L32 L33 L34 java/lang/Throwable  
+TRY L0 L36 L37 java/sql/SQLException  
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeItemTable.nativeTemplates:Llocal/lito381/server/NativeDeItemTemplatesScope$Data;
 INVOKESTATIC local/lito381/server/NativeDeItemTemplatesScope.requireLoading(Llocal/lito381/server/NativeDeItemTemplatesScope$Data;)V
@@ -1831,7 +1837,7 @@
 LABEL L3
 ALOAD 4
 INVOKEINTERFACE java/sql/ResultSet.next()Z itf
-IFEQ L17
+IFEQ L18
 CONST Integer:-1
 ISTORE 5
 LABEL L4
@@ -2023,10 +2029,10 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeEtcItemTemplate.getItemId()I
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 ALOAD 6
-INVOKEVIRTUAL java/util/HashMap.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
+INVOKEINTERFACE java/util/Map.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; itf
 POP
 LABEL L15
-GOTO L3
+GOTO L17
 LABEL L16
 ASTORE 6
 ALOAD 0
@@ -2038,70 +2044,79 @@
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
 GOTO L3
 LABEL L17
-ALOAD 4
-IFNULL L21
+GOTO L3
+LABEL L18
+ALOAD 4
+IFNULL L24
 ALOAD 4
 INVOKEINTERFACE java/sql/ResultSet.close()V itf
-GOTO L21
-LABEL L18
-ASTORE 8
+GOTO L24
 LABEL L19
-ALOAD 4
-IFNULL L20
+ASTORE 5
+ALOAD 4
+IFNULL L23
+LABEL L20
 ALOAD 4
 INVOKEINTERFACE java/sql/ResultSet.close()V itf
-LABEL L20
-ALOAD 8
+LABEL L21
+GOTO L23
+LABEL L22
+ASTORE 6
+ALOAD 5
+ALOAD 6
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+LABEL L23
+ALOAD 5
 ATHROW
-LABEL L21
+LABEL L24
 ALOAD 3
-IFNULL L27
+IFNULL L30
 ALOAD 3
 INVOKEINTERFACE java/sql/PreparedStatement.close()V itf
-GOTO L27
-LABEL L22
+GOTO L30
+LABEL L25
 ASTORE 4
 ALOAD 3
-IFNULL L26
-LABEL L23
+IFNULL L29
+LABEL L26
 ALOAD 3
 INVOKEINTERFACE java/sql/PreparedStatement.close()V itf
-LABEL L24
-GOTO L26
-LABEL L25
+LABEL L27
+GOTO L29
+LABEL L28
 ASTORE 5
 ALOAD 4
 ALOAD 5
 INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
-LABEL L26
+LABEL L29
 ALOAD 4
 ATHROW
-LABEL L27
-ALOAD 2
-IFNULL L33
+LABEL L30
+ALOAD 2
+IFNULL L36
 ALOAD 2
 INVOKEINTERFACE java/sql/Connection.close()V itf
-GOTO L33
-LABEL L28
+GOTO L36
+LABEL L31
 ASTORE 3
 ALOAD 2
-IFNULL L32
-LABEL L29
+IFNULL L35
+LABEL L32
 ALOAD 2
 INVOKEINTERFACE java/sql/Connection.close()V itf
-LABEL L30
-GOTO L32
-LABEL L31
-ASTORE 4
-ALOAD 3
-ALOAD 4
-INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
-LABEL L32
-ALOAD 3
-ATHROW
 LABEL L33
 GOTO L35
 LABEL L34
+ASTORE 4
+ALOAD 3
+ALOAD 4
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+LABEL L35
+ALOAD 3
+ATHROW
+LABEL L36
+GOTO L38
+LABEL L37
 ASTORE 2
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeItemTable.nativeTemplates:Llocal/lito381/server/NativeDeItemTemplatesScope$Data;
@@ -2109,7 +2124,7 @@
 CONST str:"載入 etcitem 失敗! 請檢查資料表!"
 ALOAD 2
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
-LABEL L35
+LABEL L38
 ALOAD 1
 ARETURN
 method=allWeapon()Ljava/util/Map;
@@ -2121,13 +2136,13 @@
 TRY L29 L30 L31 java/sql/SQLException  
 TRY L32 L33 L34 java/sql/SQLException  
 TRY L4 L36 L37 java/lang/Exception  
-TRY L3 L38 L39 java/lang/Throwable  
-TRY L39 L40 L39 java/lang/Throwable  
-TRY L2 L42 L43 java/lang/Throwable  
-TRY L44 L45 L46 java/lang/Throwable  
-TRY L1 L48 L49 java/lang/Throwable  
-TRY L50 L51 L52 java/lang/Throwable  
-TRY L0 L54 L55 java/sql/SQLException  
+TRY L3 L39 L40 java/lang/Throwable  
+TRY L41 L42 L43 java/lang/Throwable  
+TRY L2 L45 L46 java/lang/Throwable  
+TRY L47 L48 L49 java/lang/Throwable  
+TRY L1 L51 L52 java/lang/Throwable  
+TRY L53 L54 L55 java/lang/Throwable  
+TRY L0 L57 L58 java/sql/SQLException  
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeItemTable.nativeTemplates:Llocal/lito381/server/NativeDeItemTemplatesScope$Data;
 INVOKESTATIC local/lito381/server/NativeDeItemTemplatesScope.requireLoading(Llocal/lito381/server/NativeDeItemTemplatesScope$Data;)V
@@ -2151,7 +2166,7 @@
 LABEL L3
 ALOAD 4
 INVOKEINTERFACE java/sql/ResultSet.next()Z itf
-IFEQ L38
+IFEQ L39
 CONST Integer:-1
 ISTORE 5
 LABEL L4
@@ -2260,77 +2275,77 @@
 ALOAD 4
 CONST str:"use_royal"
 INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-IFEQ L5
-CONST Integer:1
+IFNE L5
+CONST Integer:0
 GOTO L6
 LABEL L5
-CONST Integer:0
+CONST Integer:1
 LABEL L6
 INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.setUseRoyal(Z)V
 ALOAD 6
 ALOAD 4
 CONST str:"use_knight"
 INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-IFEQ L7
-CONST Integer:1
+IFNE L7
+CONST Integer:0
 GOTO L8
 LABEL L7
-CONST Integer:0
+CONST Integer:1
 LABEL L8
 INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.setUseKnight(Z)V
 ALOAD 6
 ALOAD 4
 CONST str:"use_elf"
 INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-IFEQ L9
-CONST Integer:1
+IFNE L9
+CONST Integer:0
 GOTO L10
 LABEL L9
-CONST Integer:0
+CONST Integer:1
 LABEL L10
 INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.setUseElf(Z)V
 ALOAD 6
 ALOAD 4
 CONST str:"use_mage"
 INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-IFEQ L11
-CONST Integer:1
+IFNE L11
+CONST Integer:0
 GOTO L12
 LABEL L11
-CONST Integer:0
+CONST Integer:1
 LABEL L12
 INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.setUseMage(Z)V
 ALOAD 6
 ALOAD 4
 CONST str:"use_darkelf"
 INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-IFEQ L13
-CONST Integer:1
+IFNE L13
+CONST Integer:0
 GOTO L14
 LABEL L13
-CONST Integer:0
+CONST Integer:1
 LABEL L14
 INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.setUseDarkelf(Z)V
 ALOAD 6
 ALOAD 4
 CONST str:"use_dragonknight"
 INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-IFEQ L15
-CONST Integer:1
+IFNE L15
+CONST Integer:0
 GOTO L16
 LABEL L15
-CONST Integer:0
+CONST Integer:1
 LABEL L16
 INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.setUseDragonknight(Z)V
 ALOAD 6
 ALOAD 4
 CONST str:"use_illusionist"
 INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-IFEQ L17
-CONST Integer:1
+IFNE L17
+CONST Integer:0
 GOTO L18
 LABEL L17
-CONST Integer:0
+CONST Integer:1
 LABEL L18
 INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.setUseIllusionist(Z)V
 ALOAD 6
@@ -2494,11 +2509,11 @@
 ALOAD 4
 CONST str:"haste_item"
 INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-IFEQ L27
-CONST Integer:1
+IFNE L27
+CONST Integer:0
 GOTO L28
 LABEL L27
-CONST Integer:0
+CONST Integer:1
 LABEL L28
 INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.setHasteItem(Z)V
 ALOAD 6
@@ -2660,10 +2675,10 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.getItemId()I
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 ALOAD 6
-INVOKEVIRTUAL java/util/HashMap.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
+INVOKEINTERFACE java/util/Map.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; itf
 POP
 LABEL L36
-GOTO L3
+GOTO L38
 LABEL L37
 ASTORE 6
 ALOAD 0
@@ -2675,70 +2690,79 @@
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
 GOTO L3
 LABEL L38
-ALOAD 4
-IFNULL L42
+GOTO L3
+LABEL L39
+ALOAD 4
+IFNULL L45
 ALOAD 4
 INVOKEINTERFACE java/sql/ResultSet.close()V itf
-GOTO L42
-LABEL L39
-ASTORE 9
+GOTO L45
 LABEL L40
-ALOAD 4
-IFNULL L41
+ASTORE 5
+ALOAD 4
+IFNULL L44
+LABEL L41
 ALOAD 4
 INVOKEINTERFACE java/sql/ResultSet.close()V itf
-LABEL L41
-ALOAD 9
+LABEL L42
+GOTO L44
+LABEL L43
+ASTORE 6
+ALOAD 5
+ALOAD 6
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+LABEL L44
+ALOAD 5
 ATHROW
-LABEL L42
+LABEL L45
 ALOAD 3
-IFNULL L48
+IFNULL L51
 ALOAD 3
 INVOKEINTERFACE java/sql/PreparedStatement.close()V itf
-GOTO L48
-LABEL L43
+GOTO L51
+LABEL L46
 ASTORE 4
 ALOAD 3
-IFNULL L47
-LABEL L44
+IFNULL L50
+LABEL L47
 ALOAD 3
 INVOKEINTERFACE java/sql/PreparedStatement.close()V itf
-LABEL L45
-GOTO L47
-LABEL L46
+LABEL L48
+GOTO L50
+LABEL L49
 ASTORE 5
 ALOAD 4
 ALOAD 5
 INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
-LABEL L47
+LABEL L50
 ALOAD 4
 ATHROW
-LABEL L48
-ALOAD 2
-IFNULL L54
+LABEL L51
+ALOAD 2
+IFNULL L57
 ALOAD 2
 INVOKEINTERFACE java/sql/Connection.close()V itf
-GOTO L54
-LABEL L49
+GOTO L57
+LABEL L52
 ASTORE 3
 ALOAD 2
-IFNULL L53
-LABEL L50
+IFNULL L56
+LABEL L53
 ALOAD 2
 INVOKEINTERFACE java/sql/Connection.close()V itf
-LABEL L51
-GOTO L53
-LABEL L52
-ASTORE 4
-ALOAD 3
-ALOAD 4
-INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
-LABEL L53
-ALOAD 3
-ATHROW
 LABEL L54
 GOTO L56
 LABEL L55
+ASTORE 4
+ALOAD 3
+ALOAD 4
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+LABEL L56
+ALOAD 3
+ATHROW
+LABEL L57
+GOTO L59
+LABEL L58
 ASTORE 2
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeItemTable.nativeTemplates:Llocal/lito381/server/NativeDeItemTemplatesScope$Data;
@@ -2746,7 +2770,7 @@
 CONST str:"載入 weapon 失敗! 請檢查資料表!"
 ALOAD 2
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
-LABEL L56
+LABEL L59
 ALOAD 1
 ARETURN
 method=applyArrowConfig(Ljava/sql/ResultSet;Llocal/lito381/server/NativeDeEtcItemTemplate;)Z
@@ -2758,7 +2782,7 @@
 ALOAD 1
 CONST str:"arrow_config_id"
 INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ISTORE 4
+ISTORE 3
 ALOAD 1
 INVOKEINTERFACE java/sql/ResultSet.wasNull()Z itf
 IFNE L0
@@ -2767,14 +2791,14 @@
 LABEL L0
 CONST Integer:0
 LABEL L1
+ISTORE 4
+ALOAD 2
+INVOKEVIRTUAL local/lito381/server/NativeDeEtcItemTemplate.getUseType()I
 ISTORE 5
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/NativeDeEtcItemTemplate.getUseType()I
-ISTORE 6
-ILOAD 6
+ILOAD 5
 CONST Integer:-2
 IF_ICMPEQ L2
-ILOAD 6
+ILOAD 5
 CONST Integer:-3
 IF_ICMPNE L3
 LABEL L2
@@ -2783,12 +2807,10 @@
 LABEL L3
 CONST Integer:0
 LABEL L4
-DUP
-ISTORE 3
-ISTORE 7
-ILOAD 3
+ISTORE 6
+ILOAD 6
 IFNE L6
-ILOAD 5
+ILOAD 4
 IFEQ L5
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeItemTable.nativeTemplates:Llocal/lito381/server/NativeDeItemTemplatesScope$Data;
@@ -2797,16 +2819,16 @@
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeEtcItemTemplate.getItemId()I
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
-ILOAD 6
+ILOAD 5
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V itf
 LABEL L5
 CONST Integer:1
 IRETURN
 LABEL L6
-ILOAD 5
+ILOAD 4
 IFEQ L7
-ILOAD 4
+ILOAD 3
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeEtcItemTemplate.getItemId()I
 IF_ICMPEQ L8
@@ -2827,33 +2849,33 @@
 ALOAD 1
 CONST str:"arrow_consume_on_attack"
 INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ISTORE 7
+ALOAD 1
+CONST str:"arrow_dmg_small"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
 ISTORE 8
 ALOAD 1
-CONST str:"arrow_dmg_small"
+CONST str:"arrow_dmg_large"
 INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
 ISTORE 9
 ALOAD 1
-CONST str:"arrow_dmg_large"
+CONST str:"arrow_config_spr"
 INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
 ISTORE 10
-ALOAD 1
-CONST str:"arrow_config_spr"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ISTORE 11
-ILOAD 8
+ILOAD 7
 IFEQ L9
-ILOAD 8
+ILOAD 7
 CONST Integer:1
 IF_ICMPNE L10
 LABEL L9
+ILOAD 8
+IFLT L10
 ILOAD 9
 IFLT L10
 ILOAD 10
-IFLT L10
-ILOAD 11
 CONST Integer:-1
 IF_ICMPLT L10
-ILOAD 11
+ILOAD 10
 CONST Integer:65535
 IF_ICMPLE L11
 LABEL L10
@@ -2864,20 +2886,20 @@
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeEtcItemTemplate.getItemId()I
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+ILOAD 7
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 ILOAD 8
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 ILOAD 9
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 ILOAD 10
-INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
-ILOAD 11
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
 CONST Integer:0
 IRETURN
 LABEL L11
 ALOAD 2
-ILOAD 8
+ILOAD 7
 CONST Integer:1
 IF_ICMPNE L12
 CONST Integer:1
@@ -2887,13 +2909,13 @@
 LABEL L13
 INVOKEVIRTUAL local/lito381/server/NativeDeEtcItemTemplate.setConsumeOnAttack(Z)V
 ALOAD 2
+ILOAD 8
+INVOKEVIRTUAL local/lito381/server/NativeDeEtcItemTemplate.setDmgSmall(I)V
+ALOAD 2
 ILOAD 9
-INVOKEVIRTUAL local/lito381/server/NativeDeEtcItemTemplate.setDmgSmall(I)V
+INVOKEVIRTUAL local/lito381/server/NativeDeEtcItemTemplate.setDmgLarge(I)V
 ALOAD 2
 ILOAD 10
-INVOKEVIRTUAL local/lito381/server/NativeDeEtcItemTemplate.setDmgLarge(I)V
-ALOAD 2
-ILOAD 11
 INVOKEVIRTUAL local/lito381/server/NativeDeEtcItemTemplate.setArrowSpr(I)V
 CONST Integer:1
 IRETURN
@@ -2907,87 +2929,84 @@
 GETFIELD local/lito381/server/NativeDeItemTable.nativeTemplates:Llocal/lito381/server/NativeDeItemTemplatesScope$Data;
 INVOKESTATIC local/lito381/server/NativeDeItemTemplatesScope.requireLoading(Llocal/lito381/server/NativeDeItemTemplatesScope$Data;)V
 CONST Integer:0
-ISTORE 2
+ISTORE 1
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeItemTable._etcitems:Ljava/util/Map;
+INVOKEINTERFACE java/util/Map.values()Ljava/util/Collection; itf
+ASTORE 2
+ALOAD 2
+INVOKEINTERFACE java/util/Collection.iterator()Ljava/util/Iterator; itf
+ASTORE 3
+LABEL L0
+ALOAD 3
+INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
+IFEQ L2
+ALOAD 3
+INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
+CHECKCAST local/lito381/server/NativeDeEtcItemTemplate
+ASTORE 4
+ALOAD 4
+INVOKEVIRTUAL local/lito381/server/NativeDeEtcItemTemplate.getItemId()I
+ILOAD 1
+IF_ICMPLE L1
+ALOAD 4
+INVOKEVIRTUAL local/lito381/server/NativeDeEtcItemTemplate.getItemId()I
+ISTORE 1
+LABEL L1
+GOTO L0
+LABEL L2
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemTable._weapons:Ljava/util/Map;
 INVOKEINTERFACE java/util/Map.values()Ljava/util/Collection; itf
 ASTORE 3
 ALOAD 3
 INVOKEINTERFACE java/util/Collection.iterator()Ljava/util/Iterator; itf
 ASTORE 4
-LABEL L0
+LABEL L3
 ALOAD 4
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L2
+IFEQ L5
 ALOAD 4
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
-CHECKCAST local/lito381/server/NativeDeEtcItemTemplate
+CHECKCAST local/lito381/server/NativeDeWeaponTemplate
 ASTORE 5
 ALOAD 5
-INVOKEVIRTUAL local/lito381/server/NativeDeEtcItemTemplate.getItemId()I
-ILOAD 2
-IF_ICMPGT L1
-GOTO L0
-LABEL L1
+INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.getItemId()I
+ILOAD 1
+IF_ICMPLE L4
 ALOAD 5
-INVOKEVIRTUAL local/lito381/server/NativeDeEtcItemTemplate.getItemId()I
-ISTORE 2
-GOTO L0
-LABEL L2
-ALOAD 0
-GETFIELD local/lito381/server/NativeDeItemTable._weapons:Ljava/util/Map;
+INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.getItemId()I
+ISTORE 1
+LABEL L4
+GOTO L3
+LABEL L5
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeItemTable._armors:Ljava/util/Map;
 INVOKEINTERFACE java/util/Map.values()Ljava/util/Collection; itf
 ASTORE 4
 ALOAD 4
 INVOKEINTERFACE java/util/Collection.iterator()Ljava/util/Iterator; itf
 ASTORE 5
-LABEL L3
+LABEL L6
 ALOAD 5
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L5
+IFEQ L8
 ALOAD 5
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
-CHECKCAST local/lito381/server/NativeDeWeaponTemplate
+CHECKCAST local/lito381/server/NativeDeArmorTemplate
 ASTORE 6
 ALOAD 6
-INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.getItemId()I
-ILOAD 2
-IF_ICMPGT L4
-GOTO L3
-LABEL L4
-ALOAD 6
-INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.getItemId()I
-ISTORE 2
-GOTO L3
-LABEL L5
-ALOAD 0
-GETFIELD local/lito381/server/NativeDeItemTable._armors:Ljava/util/Map;
-INVOKEINTERFACE java/util/Map.values()Ljava/util/Collection; itf
-ASTORE 6
-ALOAD 6
-INVOKEINTERFACE java/util/Collection.iterator()Ljava/util/Iterator; itf
-ASTORE 7
-LABEL L6
-ALOAD 7
-INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L8
-ALOAD 7
-INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
-CHECKCAST local/lito381/server/NativeDeArmorTemplate
-ASTORE 8
-ALOAD 8
 INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getItemId()I
-ILOAD 2
-IF_ICMPGT L7
-GOTO L6
+ILOAD 1
+IF_ICMPLE L7
+ALOAD 6
+INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getItemId()I
+ISTORE 1
 LABEL L7
-ALOAD 8
-INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getItemId()I
-ISTORE 2
 GOTO L6
 LABEL L8
 ALOAD 0
-ILOAD 2
+ILOAD 1
 CONST Integer:1
 IADD
 ANEWARRAY local/lito381/server/NativeDeItemTemplate
@@ -2996,26 +3015,26 @@
 GETFIELD local/lito381/server/NativeDeItemTable._etcitems:Ljava/util/Map;
 INVOKEINTERFACE java/util/Map.keySet()Ljava/util/Set; itf
 INVOKEINTERFACE java/util/Set.iterator()Ljava/util/Iterator; itf
-ASTORE 7
+ASTORE 5
 LABEL L9
-ALOAD 7
+ALOAD 5
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
 IFEQ L10
-ALOAD 7
+ALOAD 5
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST java/lang/Integer
-ASTORE 8
+ASTORE 6
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeItemTable._etcitems:Ljava/util/Map;
-ALOAD 8
+ALOAD 6
 INVOKEINTERFACE java/util/Map.get(Ljava/lang/Object;)Ljava/lang/Object; itf
-CHECKCAST local/lito381/server/NativeDeItemTemplate
-ASTORE 1
+CHECKCAST local/lito381/server/NativeDeEtcItemTemplate
+ASTORE 7
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeItemTable._allTemplates:[Llocal/lito381/server/NativeDeItemTemplate;
-ALOAD 8
+ALOAD 6
 INVOKEVIRTUAL java/lang/Integer.intValue()I
-ALOAD 1
+ALOAD 7
 AASTORE
 GOTO L9
 LABEL L10
@@ -3023,26 +3042,26 @@
 GETFIELD local/lito381/server/NativeDeItemTable._weapons:Ljava/util/Map;
 INVOKEINTERFACE java/util/Map.keySet()Ljava/util/Set; itf
 INVOKEINTERFACE java/util/Set.iterator()Ljava/util/Iterator; itf
-ASTORE 7
+ASTORE 5
 LABEL L11
-ALOAD 7
+ALOAD 5
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
 IFEQ L12
-ALOAD 7
+ALOAD 5
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST java/lang/Integer
-ASTORE 8
+ASTORE 6
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeItemTable._weapons:Ljava/util/Map;
-ALOAD 8
+ALOAD 6
 INVOKEINTERFACE java/util/Map.get(Ljava/lang/Object;)Ljava/lang/Object; itf
-CHECKCAST local/lito381/server/NativeDeItemTemplate
-ASTORE 1
+CHECKCAST local/lito381/server/NativeDeWeaponTemplate
+ASTORE 7
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeItemTable._allTemplates:[Llocal/lito381/server/NativeDeItemTemplate;
-ALOAD 8
+ALOAD 6
 INVOKEVIRTUAL java/lang/Integer.intValue()I
-ALOAD 1
+ALOAD 7
 AASTORE
 GOTO L11
 LABEL L12
@@ -3050,26 +3069,26 @@
 GETFIELD local/lito381/server/NativeDeItemTable._armors:Ljava/util/Map;
 INVOKEINTERFACE java/util/Map.keySet()Ljava/util/Set; itf
 INVOKEINTERFACE java/util/Set.iterator()Ljava/util/Iterator; itf
-ASTORE 7
+ASTORE 5
 LABEL L13
-ALOAD 7
+ALOAD 5
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
 IFEQ L14
-ALOAD 7
+ALOAD 5
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST java/lang/Integer
-ASTORE 8
+ASTORE 6
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeItemTable._armors:Ljava/util/Map;
-ALOAD 8
+ALOAD 6
 INVOKEINTERFACE java/util/Map.get(Ljava/lang/Object;)Ljava/lang/Object; itf
-CHECKCAST local/lito381/server/NativeDeItemTemplate
-ASTORE 1
+CHECKCAST local/lito381/server/NativeDeArmorTemplate
+ASTORE 7
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeItemTable._allTemplates:[Llocal/lito381/server/NativeDeItemTemplate;
-ALOAD 8
+ALOAD 6
 INVOKEVIRTUAL java/lang/Integer.intValue()I
-ALOAD 1
+ALOAD 7
 AASTORE
 GOTO L13
 LABEL L14
@@ -3101,13 +3120,13 @@
 ALOAD 7
 INVOKEVIRTUAL local/lito381/server/NativeDeItemTemplate.getType2()I
 ILOAD 3
-IF_ICMPNE L2
+IF_ICMPEQ L1
+GOTO L2
+LABEL L1
 ALOAD 7
 ILOAD 2
 INVOKESTATIC local/lito381/server/NativeDeItemTable.hasSetId(Llocal/lito381/server/NativeDeItemTemplate;I)Z
-IFNE L1
-GOTO L2
-LABEL L1
+IFEQ L2
 ALOAD 1
 ALOAD 7
 INVOKEVIRTUAL local/lito381/server/NativeDeItemTemplate.getItemId()I
@@ -3158,28 +3177,26 @@
 LABEL L0
 ILOAD 5
 ILOAD 4
-IF_ICMPGE L3
+IF_ICMPGE L2
 ALOAD 3
 ILOAD 5
 AALOAD
 ASTORE 6
 ALOAD 6
-IFNULL L2
+IFNULL L1
 ALOAD 6
 INVOKEVIRTUAL local/lito381/server/NativeDeItemTemplate.getNameId()Ljava/lang/String;
 ALOAD 1
 INVOKEVIRTUAL java/lang/String.equals(Ljava/lang/Object;)Z
-IFNE L1
+IFEQ L1
+ALOAD 6
+INVOKEVIRTUAL local/lito381/server/NativeDeItemTemplate.getItemId()I
+ISTORE 2
 GOTO L2
 LABEL L1
-ALOAD 6
-INVOKEVIRTUAL local/lito381/server/NativeDeItemTemplate.getItemId()I
-ISTORE 2
-GOTO L3
-LABEL L2
 IINC var=5 incr=1
 GOTO L0
-LABEL L3
+LABEL L2
 ILOAD 2
 IRETURN
 method=findItemIdByNameWithoutSpace(Ljava/lang/String;)I
@@ -3201,13 +3218,13 @@
 LABEL L0
 ILOAD 5
 ILOAD 4
-IF_ICMPGE L3
+IF_ICMPGE L2
 ALOAD 3
 ILOAD 5
 AALOAD
 ASTORE 6
 ALOAD 6
-IFNULL L2
+IFNULL L1
 ALOAD 6
 INVOKEVIRTUAL local/lito381/server/NativeDeItemTemplate.getNameId()Ljava/lang/String;
 CONST str:" "
@@ -3215,17 +3232,15 @@
 INVOKEVIRTUAL java/lang/String.replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;
 ALOAD 1
 INVOKEVIRTUAL java/lang/String.equals(Ljava/lang/Object;)Z
-IFNE L1
+IFEQ L1
+ALOAD 6
+INVOKEVIRTUAL local/lito381/server/NativeDeItemTemplate.getItemId()I
+ISTORE 2
 GOTO L2
 LABEL L1
-ALOAD 6
-INVOKEVIRTUAL local/lito381/server/NativeDeItemTemplate.getItemId()I
-ISTORE 2
-GOTO L3
-LABEL L2
 IINC var=5 incr=1
 GOTO L0
-LABEL L3
+LABEL L2
 ILOAD 2
 IRETURN
 method=findItemIdsBySetId(I)Ljava/util/List;
@@ -3300,17 +3315,17 @@
 ALOAD 8
 INVOKEVIRTUAL local/lito381/server/NativeDeItemTemplate.getType2()I
 ILOAD 1
-IF_ICMPNE L4
+IF_ICMPEQ L3
+GOTO L4
+LABEL L3
 ALOAD 8
 INVOKEVIRTUAL local/lito381/server/NativeDeItemTemplate.getName()Ljava/lang/String;
 ALOAD 4
 INVOKESTATIC local/lito381/server/NativeDeItemTable.containsName(Ljava/lang/String;Ljava/lang/String;)Z
-IFNE L3
-GOTO L4
-LABEL L3
+IFEQ L4
 ALOAD 3
 ALOAD 8
-INVOKEVIRTUAL java/util/ArrayList.add(Ljava/lang/Object;)Z
+INVOKEINTERFACE java/util/List.add(Ljava/lang/Object;)Z itf
 POP
 LABEL L4
 IINC var=7 incr=1
@@ -3361,26 +3376,24 @@
 LABEL L0
 ILOAD 4
 ILOAD 3
-IF_ICMPGE L3
+IF_ICMPGE L2
 ALOAD 2
 ILOAD 4
 AALOAD
 ASTORE 5
 ALOAD 5
-IFNULL L2
+IFNULL L1
 ALOAD 5
 INVOKEVIRTUAL local/lito381/server/NativeDeItemTemplate.getNameId()Ljava/lang/String;
 ALOAD 1
 INVOKEVIRTUAL java/lang/String.equals(Ljava/lang/Object;)Z
-IFNE L1
-GOTO L2
-LABEL L1
+IFEQ L1
 ALOAD 5
 ARETURN
-LABEL L2
+LABEL L1
 IINC var=4 incr=1
 GOTO L0
-LABEL L3
+LABEL L2
 CONST null
 ARETURN
 method=hasSetId(Llocal/lito381/server/NativeDeItemTemplate;I)Z
@@ -3400,22 +3413,20 @@
 LABEL L0
 ILOAD 4
 ILOAD 3
-IF_ICMPGE L3
+IF_ICMPGE L2
 ALOAD 2
 ILOAD 4
 IALOAD
 ISTORE 5
 ILOAD 5
 ILOAD 1
-IF_ICMPEQ L1
-GOTO L2
+IF_ICMPNE L1
+CONST Integer:1
+IRETURN
 LABEL L1
-CONST Integer:1
-IRETURN
-LABEL L2
 IINC var=4 incr=1
 GOTO L0
-LABEL L3
+LABEL L2
 CONST Integer:0
 IRETURN
 method=load()V
@@ -3432,16 +3443,19 @@
 INVOKESPECIAL local/lito381/server/NativeDeTerrainTimer.<init>()V
 ASTORE 1
 ALOAD 0
-ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeItemTable.allEtcItem()Ljava/util/Map;
+ALOAD 0
+SWAP
 PUTFIELD local/lito381/server/NativeDeItemTable._etcitems:Ljava/util/Map;
 ALOAD 0
-ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeItemTable.allWeapon()Ljava/util/Map;
+ALOAD 0
+SWAP
 PUTFIELD local/lito381/server/NativeDeItemTable._weapons:Ljava/util/Map;
 ALOAD 0
-ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeItemTable.allArmor()Ljava/util/Map;
+ALOAD 0
+SWAP
 PUTFIELD local/lito381/server/NativeDeItemTable._armors:Ljava/util/Map;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeItemTable.buildFastLookupTable()V
@@ -3508,8 +3522,9 @@
 GETFIELD local/lito381/server/NativeDeItemTable.nativeTemplates:Llocal/lito381/server/NativeDeItemTemplatesScope$Data;
 INVOKESTATIC local/lito381/server/NativeDeItemTemplatesScope.requireLoading(Llocal/lito381/server/NativeDeItemTemplatesScope$Data;)V
 ALOAD 0
-ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeItemTable.allArmor()Ljava/util/Map;
+ALOAD 0
+SWAP
 PUTFIELD local/lito381/server/NativeDeItemTable._armors:Ljava/util/Map;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeItemTable.buildFastLookupTable()V
@@ -3532,8 +3547,9 @@
 GETFIELD local/lito381/server/NativeDeItemTable.nativeTemplates:Llocal/lito381/server/NativeDeItemTemplatesScope$Data;
 INVOKESTATIC local/lito381/server/NativeDeItemTemplatesScope.requireLoading(Llocal/lito381/server/NativeDeItemTemplatesScope$Data;)V
 ALOAD 0
-ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeItemTable.allEtcItem()Ljava/util/Map;
+ALOAD 0
+SWAP
 PUTFIELD local/lito381/server/NativeDeItemTable._etcitems:Ljava/util/Map;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeItemTable.buildFastLookupTable()V
@@ -3556,8 +3572,9 @@
 GETFIELD local/lito381/server/NativeDeItemTable.nativeTemplates:Llocal/lito381/server/NativeDeItemTemplatesScope$Data;
 INVOKESTATIC local/lito381/server/NativeDeItemTemplatesScope.requireLoading(Llocal/lito381/server/NativeDeItemTemplatesScope$Data;)V
 ALOAD 0
-ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeItemTable.allWeapon()Ljava/util/Map;
+ALOAD 0
+SWAP
 PUTFIELD local/lito381/server/NativeDeItemTable._weapons:Ljava/util/Map;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeItemTable.buildFastLookupTable()V
~~~

</details>

<details>
<summary>NativeDeItemWeaponAttrTable — DIFF</summary>

已驗證目前差異只出現在 class metadata；欄位及全部方法的本工具正規化輸出一致。metadata 是否可豁免仍待主控核准。

左側正規化 SHA-256：e1f8d1ad0facfcbe812336f254226f5661afb27ae9f6d7a2a5091aa8bf938bfb。  
右側正規化 SHA-256：c164a707228ecf28d5030af778112ec0d5bddce106eaae508c9c0c635043d4b4。

~~~diff
--- staged/NativeDeItemWeaponAttrTable
+++ formal/NativeDeItemWeaponAttrTable
@@ -10,9 +10,6 @@
 permittedSubclasses=
 annotations=|||
 innerClass=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
-innerClass=local/lito381/server/NativeDeItemDefinitionsScope$Database|local/lito381/server/NativeDeItemDefinitionsScope|Database|0x18
-innerClass=local/lito381/server/NativeDeItemDefinitionsScope$Data|local/lito381/server/NativeDeItemDefinitionsScope|Data|0x18
-innerClass=local/lito381/server/NativeDeItemDefinitionsScope$Logs|local/lito381/server/NativeDeItemDefinitionsScope|Logs|0x18
 recordComponents=
 field=_attrList:Ljava/util/Map;
 access=0x12
~~~

</details>

<details>
<summary>NativeDeItemWeaponProcTable — DIFF</summary>

待繼續移植／主控裁定：以下包含全部 metadata、欄位及方法差異；尚未證明不同方法等價，也未證明全部無法用 Java 表達。

左側正規化 SHA-256：d5c6423b895e5623337745db8371d84ec1ed1f4271e035b3f2ca1f5f95286a16。  
右側正規化 SHA-256：8b8244ee6fd40d0480af0dd3d1010ed23f3174218c69e8a47ddc0f36af5fc757。

~~~diff
--- staged/NativeDeItemWeaponProcTable
+++ formal/NativeDeItemWeaponProcTable
@@ -10,15 +10,7 @@
 permittedSubclasses=
 annotations=|||
 innerClass=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
-innerClass=local/lito381/server/NativeDeItemDefinitionsScope$Database|local/lito381/server/NativeDeItemDefinitionsScope|Database|0x18
-innerClass=local/lito381/server/NativeDeItemDefinitionsScope$Data|local/lito381/server/NativeDeItemDefinitionsScope|Data|0x18
-innerClass=local/lito381/server/NativeDeItemDefinitionsScope$Logs|local/lito381/server/NativeDeItemDefinitionsScope|Logs|0x18
 recordComponents=
-field=_instance:Llocal/lito381/server/NativeDeItemWeaponProcTable;
-access=0xa
-signature=
-value=
-annotations=|||
 field=_log:Llocal/lito381/server/NativeDeWorldLog;
 access=0x1a
 signature=
@@ -232,7 +224,7 @@
 ILOAD 8
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 INVOKEDYNAMIC apply()Ljava/util/function/Function; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeItemWeaponProcTable.lambda$load$#0(Ljava/lang/Integer;)Ljava/util/List;, type:(Ljava/lang/Integer;)Ljava/util/List;]
-INVOKEVIRTUAL java/util/HashMap.computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;
+INVOKEINTERFACE java/util/Map.computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object; itf
 CHECKCAST java/util/List
 ALOAD 7
 INVOKEINTERFACE java/util/List.add(Ljava/lang/Object;)Z itf
~~~

</details>

<details>
<summary>NativeDeKnownState — DIFF</summary>

已驗證目前差異只出現在 class metadata；欄位及全部方法的本工具正規化輸出一致。metadata 是否可豁免仍待主控核准。

左側正規化 SHA-256：7cdaad4203516eec07ee3d83deb5df3ef82c2e97820c9ea164cc40b7a62a730b。  
右側正規化 SHA-256：fbb444da721ced25bfa2e8f8de971e0da1a7b8b6e7949338c66859b43bafdaa8。

~~~diff
--- staged/NativeDeKnownState
+++ formal/NativeDeKnownState
@@ -9,6 +9,7 @@
 nestMembers=
 permittedSubclasses=
 annotations=|||
+innerClass=local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608
 innerClass=local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408
 recordComponents=
 method=<init>(I)V
~~~

</details>

<details>
<summary>NativeDeLiveAiJobs — DIFF</summary>

待繼續移植／主控裁定：以下包含全部 metadata、欄位及方法差異；尚未證明不同方法等價，也未證明全部無法用 Java 表達。

左側正規化 SHA-256：8ee17f47fee70ca399b7b40f9f750ec2c1f17d55ccf1abf83c4f0064155584be。  
右側正規化 SHA-256：912eed83e6117271427bc3d99c96d81f3b971f04ca3c5670af35c68b7cb0c991。

~~~diff
--- staged/NativeDeLiveAiJobs
+++ formal/NativeDeLiveAiJobs
@@ -124,16 +124,18 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault=
-TRY L4 L5 L6 java/lang/RuntimeException  
-TRY L4 L5 L6 java/lang/Error  
-TRY L0 L2 L11 java/lang/Throwable  
-TRY L3 L10 L11 java/lang/Throwable  
-TRY L11 L12 L11 java/lang/Throwable  
+TRY L5 L6 L7 java/lang/RuntimeException  
+TRY L5 L6 L7 java/lang/Error  
+TRY L1 L3 L12 java/lang/Throwable  
+TRY L4 L11 L12 java/lang/Throwable  
+TRY L12 L13 L12 java/lang/Throwable  
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeDeLiveAiJobs.find(Llocal/lito381/server/NativeDeInstanceState;)Llocal/lito381/server/NativeDeLiveAiJobs$Binding;
 ASTORE 2
 ALOAD 2
-IFNULL L15
+IFNONNULL L0
+RETURN
+LABEL L0
 ALOAD 2
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Binding.owner:Llocal/lito381/server/NativeDeLiveAiJobs$Owner;
 ASTORE 3
@@ -147,25 +149,25 @@
 DUP
 ASTORE 6
 MONITORENTER
-LABEL L0
+LABEL L1
 ILOAD 1
-IFEQ L8
+IFEQ L9
 ALOAD 2
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Binding.closed:Z
-IFNE L1
+IFNE L2
 ALOAD 3
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Owner.closed:Z
-IFNE L1
+IFNE L2
 ALOAD 3
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Owner.life:Llocal/lito381/server/NativePlayerLifetime;
 INVOKEVIRTUAL local/lito381/server/NativePlayerLifetime.admits()Z
-IFNE L3
-LABEL L1
-ALOAD 6
-MONITOREXIT
+IFNE L4
 LABEL L2
-RETURN
+ALOAD 6
+MONITOREXIT
 LABEL L3
+RETURN
+LABEL L4
 ALOAD 3
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Owner.background:Ljava/util/Set;
 ALOAD 2
@@ -173,12 +175,12 @@
 POP
 ALOAD 3
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Owner.heartbeat:Ljava/util/concurrent/ScheduledFuture;
-IFNONNULL L9
+IFNONNULL L10
 ALOAD 2
 INVOKESTATIC local/lito381/server/NativeDeLiveAiJobs.credit(Llocal/lito381/server/NativeDeLiveAiJobs$Binding;)Llocal/lito381/server/NativeDeLiveAiJobs$Credit;
 ASTORE 7
 ALOAD 7
-IFNONNULL L4
+IFNONNULL L5
 ALOAD 3
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Owner.background:Ljava/util/Set;
 ALOAD 2
@@ -189,7 +191,7 @@
 CONST str:"De AI heartbeat budget exhausted"
 INVOKESPECIAL java/util/concurrent/RejectedExecutionException.<init>(Ljava/lang/String;)V
 ATHROW
-LABEL L4
+LABEL L5
 ALOAD 3
 ALOAD 3
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Owner.life:Llocal/lito381/server/NativePlayerLifetime;
@@ -204,9 +206,9 @@
 ALOAD 3
 ALOAD 7
 PUTFIELD local/lito381/server/NativeDeLiveAiJobs$Owner.heartbeatCredit:Llocal/lito381/server/NativeDeLiveAiJobs$Credit;
-LABEL L5
-GOTO L7
 LABEL L6
+GOTO L8
+LABEL L7
 ASTORE 8
 ALOAD 3
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Owner.background:Ljava/util/Set;
@@ -217,9 +219,9 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeLiveAiJobs$Credit.release()V
 ALOAD 8
 ATHROW
-LABEL L7
-GOTO L9
 LABEL L8
+GOTO L10
+LABEL L9
 ALOAD 3
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Owner.background:Ljava/util/Set;
 ALOAD 2
@@ -228,7 +230,7 @@
 ALOAD 3
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Owner.background:Ljava/util/Set;
 INVOKEINTERFACE java/util/Set.isEmpty()Z itf
-IFEQ L9
+IFEQ L10
 ALOAD 3
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Owner.heartbeat:Ljava/util/concurrent/ScheduledFuture;
 ASTORE 4
@@ -241,31 +243,31 @@
 ALOAD 3
 CONST null
 PUTFIELD local/lito381/server/NativeDeLiveAiJobs$Owner.heartbeatCredit:Llocal/lito381/server/NativeDeLiveAiJobs$Credit;
-LABEL L9
-ALOAD 6
-MONITOREXIT
 LABEL L10
-GOTO L13
+ALOAD 6
+MONITOREXIT
 LABEL L11
+GOTO L14
+LABEL L12
 ASTORE 9
 ALOAD 6
 MONITOREXIT
-LABEL L12
+LABEL L13
 ALOAD 9
 ATHROW
-LABEL L13
+LABEL L14
 ALOAD 4
-IFNULL L14
+IFNULL L15
 ALOAD 4
 CONST Integer:0
 INVOKEINTERFACE java/util/concurrent/ScheduledFuture.cancel(Z)Z itf
 POP
-LABEL L14
+LABEL L15
 ALOAD 5
-IFNULL L15
+IFNULL L16
 ALOAD 5
 INVOKEVIRTUAL local/lito381/server/NativeDeLiveAiJobs$Credit.release()V
-LABEL L15
+LABEL L16
 RETURN
 method=binding(Llocal/lito381/server/NativeDeInstanceState;)Llocal/lito381/server/NativeDeLiveAiJobs$Binding;
 access=0x8
@@ -285,24 +287,26 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault=
-TRY L0 L3 L4 java/lang/Throwable  
-TRY L4 L5 L4 java/lang/Throwable  
-TRY L13 L17 L18 java/lang/Throwable  
-TRY L18 L19 L18 java/lang/Throwable  
+TRY L1 L4 L5 java/lang/Throwable  
+TRY L5 L6 L5 java/lang/Throwable  
+TRY L14 L18 L19 java/lang/Throwable  
+TRY L19 L20 L19 java/lang/Throwable  
 ALOAD 0
 CONST str:"sourceDeLiveAiJobs"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST local/lito381/server/NativeDeLiveAiJobs$Owner
 ASTORE 1
 ALOAD 1
-IFNULL L21
+IFNONNULL L0
+RETURN
+LABEL L0
 ALOAD 1
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Owner.life:Llocal/lito381/server/NativePlayerLifetime;
 GETFIELD local/lito381/server/NativePlayerLifetime.admission:Ljava/lang/Object;
 DUP
 ASTORE 6
 MONITORENTER
-LABEL L0
+LABEL L1
 ALOAD 1
 CONST Integer:1
 PUTFIELD local/lito381/server/NativeDeLiveAiJobs$Owner.closed:Z
@@ -319,10 +323,10 @@
 ISTORE 8
 CONST Integer:0
 ISTORE 9
-LABEL L1
+LABEL L2
 ILOAD 9
 ILOAD 8
-IF_ICMPGE L2
+IF_ICMPGE L3
 ALOAD 7
 ILOAD 9
 AALOAD
@@ -331,8 +335,8 @@
 CONST Integer:1
 PUTFIELD local/lito381/server/NativeDeLiveAiJobs$Binding.closed:Z
 IINC var=9 incr=1
-GOTO L1
-LABEL L2
+GOTO L2
+LABEL L3
 ALOAD 1
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Owner.tickets:Ljava/util/Set;
 INVOKEDYNAMIC apply()Ljava/util/function/IntFunction; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(I)Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeLiveAiJobs.lambda$close$#1(I)[Llocal/lito381/server/NativeDeLiveAiJobs$Ticket;, type:(I)[Llocal/lito381/server/NativeDeLiveAiJobs$Ticket;]
@@ -359,28 +363,28 @@
 PUTFIELD local/lito381/server/NativeDeLiveAiJobs$Owner.heartbeatCredit:Llocal/lito381/server/NativeDeLiveAiJobs$Credit;
 ALOAD 6
 MONITOREXIT
-LABEL L3
-GOTO L6
 LABEL L4
+GOTO L7
+LABEL L5
 ASTORE 11
 ALOAD 6
 MONITOREXIT
-LABEL L5
+LABEL L6
 ALOAD 11
 ATHROW
-LABEL L6
+LABEL L7
 ALOAD 4
-IFNULL L7
+IFNULL L8
 ALOAD 4
 CONST Integer:0
 INVOKEINTERFACE java/util/concurrent/ScheduledFuture.cancel(Z)Z itf
 POP
-LABEL L7
+LABEL L8
 ALOAD 5
-IFNULL L8
+IFNULL L9
 ALOAD 5
 INVOKEVIRTUAL local/lito381/server/NativeDeLiveAiJobs$Credit.release()V
-LABEL L8
+LABEL L9
 ALOAD 2
 ASTORE 6
 ALOAD 6
@@ -388,10 +392,10 @@
 ISTORE 7
 CONST Integer:0
 ISTORE 8
-LABEL L9
+LABEL L10
 ILOAD 8
 ILOAD 7
-IF_ICMPGE L10
+IF_ICMPGE L11
 ALOAD 6
 ILOAD 8
 AALOAD
@@ -403,8 +407,8 @@
 INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 POP
 IINC var=8 incr=1
-GOTO L9
-LABEL L10
+GOTO L10
+LABEL L11
 ALOAD 3
 ASTORE 6
 ALOAD 6
@@ -412,10 +416,10 @@
 ISTORE 7
 CONST Integer:0
 ISTORE 8
-LABEL L11
+LABEL L12
 ILOAD 8
 ILOAD 7
-IF_ICMPGE L12
+IF_ICMPGE L13
 ALOAD 6
 ILOAD 8
 AALOAD
@@ -425,15 +429,15 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeLiveAiJobs$Ticket.cancel(Z)Z
 POP
 IINC var=8 incr=1
-GOTO L11
-LABEL L12
+GOTO L12
+LABEL L13
 ALOAD 1
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Owner.life:Llocal/lito381/server/NativePlayerLifetime;
 GETFIELD local/lito381/server/NativePlayerLifetime.admission:Ljava/lang/Object;
 DUP
 ASTORE 6
 MONITORENTER
-LABEL L13
+LABEL L14
 ALOAD 2
 ASTORE 7
 ALOAD 7
@@ -441,43 +445,42 @@
 ISTORE 8
 CONST Integer:0
 ISTORE 9
-LABEL L14
+LABEL L15
 ILOAD 9
 ILOAD 8
-IF_ICMPGE L16
+IF_ICMPGE L17
 ALOAD 7
 ILOAD 9
 AALOAD
 ASTORE 10
 ALOAD 10
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Binding.tasks:I
-IFNE L15
+IFNE L16
 ALOAD 10
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Binding.actor:Llocal/lito381/server/NativeDeInstanceState;
 CONST str:"sourceDeLiveAiBinding"
 CONST null
 INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
-LABEL L15
+LABEL L16
 IINC var=9 incr=1
-GOTO L14
-LABEL L16
-ALOAD 6
-MONITOREXIT
+GOTO L15
 LABEL L17
-GOTO L20
+ALOAD 6
+MONITOREXIT
 LABEL L18
+GOTO L21
+LABEL L19
 ASTORE 12
 ALOAD 6
 MONITOREXIT
-LABEL L19
+LABEL L20
 ALOAD 12
 ATHROW
-LABEL L20
+LABEL L21
 ALOAD 0
 CONST str:"sourceDeLiveAiJobs"
 CONST null
 INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
-LABEL L21
 RETURN
 method=configure(Llocal/lito381/server/NativeDeInstanceState;Llocal/lito381/server/NativeDeLiveAiPorts$Environment;Llocal/lito381/server/NativeDeLiveAiPorts$Config;)V
 access=0x8
@@ -485,10 +488,10 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault=
-TRY L1 L5 L6 java/lang/Throwable  
-TRY L6 L7 L6 java/lang/Throwable  
-TRY L0 L8 L9 java/lang/Throwable  
-TRY L9 L10 L9 java/lang/Throwable  
+TRY L3 L7 L8 java/lang/Throwable  
+TRY L8 L9 L8 java/lang/Throwable  
+TRY L2 L10 L11 java/lang/Throwable  
+TRY L11 L12 L11 java/lang/Throwable  
 ALOAD 1
 INVOKESTATIC java/util/Objects.requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
 POP
@@ -501,17 +504,25 @@
 ASTORE 4
 ALOAD 4
 INSTANCEOF local/lito381/server/NativeDeWorldContext
-IFEQ L12
+IFEQ L0
 ALOAD 4
 CHECKCAST local/lito381/server/NativeDeWorldContext
 ASTORE 3
+GOTO L1
+LABEL L0
+NEW java/lang/IllegalArgumentException
+DUP
+CONST str:"concrete World required"
+INVOKESPECIAL java/lang/IllegalArgumentException.<init>(Ljava/lang/String;)V
+ATHROW
+LABEL L1
 ALOAD 3
 GETFIELD local/lito381/server/NativeDeWorldContext.admission:Ljava/util/concurrent/locks/ReentrantReadWriteLock;
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantReadWriteLock.readLock()Ljava/util/concurrent/locks/ReentrantReadWriteLock$ReadLock;
 ASTORE 4
 ALOAD 4
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock.lock()V
-LABEL L0
+LABEL L2
 ALOAD 3
 INVOKEVIRTUAL local/lito381/server/NativeDeWorldContext.admit()V
 ALOAD 3
@@ -520,23 +531,23 @@
 DUP
 ASTORE 5
 MONITORENTER
-LABEL L1
+LABEL L3
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeDeLiveAiJobs.find(Llocal/lito381/server/NativeDeInstanceState;)Llocal/lito381/server/NativeDeLiveAiJobs$Binding;
-IFNULL L2
+IFNULL L4
 NEW java/lang/IllegalStateException
 DUP
 CONST str:"De AI binding is immutable"
 INVOKESPECIAL java/lang/IllegalStateException.<init>(Ljava/lang/String;)V
 ATHROW
-LABEL L2
+LABEL L4
 ALOAD 3
 CONST str:"sourceDeLiveAiJobs"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST local/lito381/server/NativeDeLiveAiJobs$Owner
 ASTORE 6
 ALOAD 6
-IFNONNULL L3
+IFNONNULL L5
 NEW local/lito381/server/NativeDeLiveAiJobs$Owner
 DUP
 ALOAD 3
@@ -546,16 +557,16 @@
 CONST str:"sourceDeLiveAiJobs"
 ALOAD 6
 INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
-LABEL L3
+LABEL L5
 ALOAD 6
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Owner.closed:Z
-IFEQ L4
+IFEQ L6
 NEW java/util/concurrent/RejectedExecutionException
 DUP
 CONST str:"De AI closed"
 INVOKESPECIAL java/util/concurrent/RejectedExecutionException.<init>(Ljava/lang/String;)V
 ATHROW
-LABEL L4
+LABEL L6
 NEW local/lito381/server/NativeDeLiveAiJobs$Binding
 DUP
 ALOAD 0
@@ -575,33 +586,25 @@
 INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
 ALOAD 5
 MONITOREXIT
-LABEL L5
-GOTO L8
-LABEL L6
+LABEL L7
+GOTO L10
+LABEL L8
 ASTORE 8
 ALOAD 5
 MONITOREXIT
-LABEL L7
+LABEL L9
 ALOAD 8
 ATHROW
-LABEL L8
-ALOAD 4
-INVOKEVIRTUAL java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock.unlock()V
-GOTO L11
-LABEL L9
-ASTORE 9
 LABEL L10
 ALOAD 4
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock.unlock()V
+GOTO L13
+LABEL L11
+ASTORE 9
+LABEL L12
+ALOAD 4
+INVOKEVIRTUAL java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock.unlock()V
 ALOAD 9
-ATHROW
-LABEL L11
-GOTO L13
-LABEL L12
-NEW java/lang/IllegalArgumentException
-DUP
-CONST str:"concrete World required"
-INVOKESPECIAL java/lang/IllegalArgumentException.<init>(Ljava/lang/String;)V
 ATHROW
 LABEL L13
 RETURN
@@ -687,13 +690,13 @@
 annotationDefault=
 TRY L4 L5 L6 java/lang/Throwable  
 TRY L6 L7 L6 java/lang/Throwable  
-TRY L3 L9 L10 java/util/concurrent/RejectedExecutionException  
-TRY L3 L9 L12 java/lang/RuntimeException  
-TRY L3 L9 L12 java/lang/Error  
-TRY L1 L2 L15 java/lang/Throwable  
-TRY L3 L11 L15 java/lang/Throwable  
-TRY L12 L14 L15 java/lang/Throwable  
-TRY L15 L16 L15 java/lang/Throwable  
+TRY L3 L9 L11 java/util/concurrent/RejectedExecutionException  
+TRY L3 L9 L13 java/lang/RuntimeException  
+TRY L3 L9 L13 java/lang/Error  
+TRY L1 L2 L14 java/lang/Throwable  
+TRY L3 L10 L14 java/lang/Throwable  
+TRY L11 L12 L14 java/lang/Throwable  
+TRY L13 L15 L14 java/lang/Throwable  
 ALOAD 0
 ALOAD 1
 CONST Integer:0
@@ -732,7 +735,7 @@
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Owner.life:Llocal/lito381/server/NativePlayerLifetime;
 GETFIELD local/lito381/server/NativePlayerLifetime.admission:Ljava/lang/Object;
 DUP
-ASTORE 5
+ASTORE 4
 MONITORENTER
 LABEL L4
 ALOAD 2
@@ -743,49 +746,45 @@
 CONST Long:1
 LADD
 PUTFIELD local/lito381/server/NativeDeLiveAiJobs$Owner.submitted:J
-ALOAD 5
+ALOAD 4
 MONITOREXIT
 LABEL L5
 GOTO L8
 LABEL L6
-ASTORE 6
+ASTORE 5
+ALOAD 4
+MONITOREXIT
+LABEL L7
 ALOAD 5
-MONITOREXIT
-LABEL L7
-ALOAD 6
 ATHROW
 LABEL L8
 CONST Integer:1
-ISTORE 4
 LABEL L9
-GOTO L13
+ALOAD 3
+MONITOREXIT
 LABEL L10
-ASTORE 5
+IRETURN
+LABEL L11
+ASTORE 4
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeLiveAiJobs$Ticket.finish()V
 CONST Integer:0
 ALOAD 3
 MONITOREXIT
-LABEL L11
+LABEL L12
 IRETURN
-LABEL L12
-ASTORE 5
+LABEL L13
+ASTORE 4
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeLiveAiJobs$Ticket.finish()V
-ALOAD 5
-ATHROW
-LABEL L13
-ILOAD 4
-ALOAD 3
-MONITOREXIT
+ALOAD 4
+ATHROW
 LABEL L14
-IRETURN
+ASTORE 6
+ALOAD 3
+MONITOREXIT
 LABEL L15
-ASTORE 7
-ALOAD 3
-MONITOREXIT
-LABEL L16
-ALOAD 7
+ALOAD 6
 ATHROW
 method=find(Llocal/lito381/server/NativeDeInstanceState;)Llocal/lito381/server/NativeDeLiveAiJobs$Binding;
 access=0x8
@@ -880,15 +879,15 @@
 TRY L3 L4 L7 java/lang/Throwable  
 TRY L5 L6 L7 java/lang/Throwable  
 TRY L7 L8 L7 java/lang/Throwable  
-TRY L11 L13 L16 java/lang/Throwable  
-TRY L14 L15 L16 java/lang/Throwable  
-TRY L16 L17 L16 java/lang/Throwable  
-TRY L19 L20 L23 java/lang/Exception  
-TRY L19 L20 L27 java/lang/Throwable  
-TRY L23 L24 L27 java/lang/Throwable  
-TRY L27 L28 L27 java/lang/Throwable  
-TRY L9 L34 L35 java/lang/Throwable  
-TRY L35 L36 L35 java/lang/Throwable  
+TRY L13 L15 L18 java/lang/Throwable  
+TRY L16 L17 L18 java/lang/Throwable  
+TRY L18 L19 L18 java/lang/Throwable  
+TRY L22 L23 L26 java/lang/Exception  
+TRY L22 L23 L30 java/lang/Throwable  
+TRY L26 L27 L30 java/lang/Throwable  
+TRY L30 L31 L30 java/lang/Throwable  
+TRY L9 L35 L36 java/lang/Throwable  
+TRY L36 L37 L36 java/lang/Throwable  
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Owner.life:Llocal/lito381/server/NativePlayerLifetime;
 GETFIELD local/lito381/server/NativePlayerLifetime.admission:Ljava/lang/Object;
@@ -953,7 +952,7 @@
 LABEL L10
 ILOAD 5
 ILOAD 4
-IF_ICMPGE L34
+IF_ICMPGE L35
 ALOAD 3
 ILOAD 5
 AALOAD
@@ -961,55 +960,64 @@
 ALOAD 6
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Binding.actor:Llocal/lito381/server/NativeDeInstanceState;
 INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.isDead()Z
-IFNE L32
+IFNE L11
 ALOAD 6
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Binding.actor:Llocal/lito381/server/NativeDeInstanceState;
 INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.getMap()Llocal/lito381/server/NativeDeWorldMap;
-IFNULL L32
+IFNULL L11
 ALOAD 6
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Binding.actor:Llocal/lito381/server/NativeDeInstanceState;
 INVOKESTATIC local/lito381/server/NativeDeLiveAiJobs.admits(Llocal/lito381/server/NativeDeInstanceState;)Z
-IFEQ L32
+IFNE L12
+LABEL L11
+ALOAD 6
+GETFIELD local/lito381/server/NativeDeLiveAiJobs$Binding.actor:Llocal/lito381/server/NativeDeInstanceState;
+CONST Integer:0
+INVOKESTATIC local/lito381/server/NativeDeLiveAiJobs.background(Llocal/lito381/server/NativeDeInstanceState;Z)V
+GOTO L34
+LABEL L12
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Owner.life:Llocal/lito381/server/NativePlayerLifetime;
 GETFIELD local/lito381/server/NativePlayerLifetime.admission:Ljava/lang/Object;
 DUP
 ASTORE 8
 MONITORENTER
-LABEL L11
+LABEL L13
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Owner.closed:Z
-IFNE L12
+IFNE L14
 ALOAD 6
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Binding.closed:Z
-IFNE L12
+IFNE L14
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Owner.life:Llocal/lito381/server/NativePlayerLifetime;
 INVOKEVIRTUAL local/lito381/server/NativePlayerLifetime.admits()Z
-IFNE L14
-LABEL L12
+IFNE L16
+LABEL L14
 ALOAD 8
 MONITOREXIT
-LABEL L13
-GOTO L33
-LABEL L14
+LABEL L15
+GOTO L34
+LABEL L16
 ALOAD 6
 INVOKESTATIC local/lito381/server/NativeDeLiveAiJobs.credit(Llocal/lito381/server/NativeDeLiveAiJobs$Binding;)Llocal/lito381/server/NativeDeLiveAiJobs$Credit;
 ASTORE 7
 ALOAD 8
 MONITOREXIT
-LABEL L15
-GOTO L18
-LABEL L16
+LABEL L17
+GOTO L20
+LABEL L18
 ASTORE 9
 ALOAD 8
 MONITOREXIT
-LABEL L17
+LABEL L19
 ALOAD 9
 ATHROW
-LABEL L18
-ALOAD 7
-IFNULL L31
+LABEL L20
+ALOAD 7
+IFNONNULL L21
+GOTO L34
+LABEL L21
 GETSTATIC local/lito381/server/NativeDeLiveAiJobs.pulseCredit:Ljava/lang/ThreadLocal;
 INVOKEVIRTUAL java/lang/ThreadLocal.get()Ljava/lang/Object;
 CHECKCAST local/lito381/server/NativeDeLiveAiJobs$PulseCredit
@@ -1021,81 +1029,74 @@
 ALOAD 7
 INVOKESPECIAL local/lito381/server/NativeDeLiveAiJobs$PulseCredit.<init>(Llocal/lito381/server/NativeDeLiveAiJobs$Binding;Llocal/lito381/server/NativeDeLiveAiJobs$Credit;)V
 INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
-LABEL L19
+LABEL L22
 ALOAD 6
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Binding.actor:Llocal/lito381/server/NativeDeInstanceState;
 INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.onBackgroundTick()V
-LABEL L20
+LABEL L23
 ALOAD 8
-IFNONNULL L21
+IFNONNULL L24
 GETSTATIC local/lito381/server/NativeDeLiveAiJobs.pulseCredit:Ljava/lang/ThreadLocal;
 INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L22
-LABEL L21
+GOTO L25
+LABEL L24
 GETSTATIC local/lito381/server/NativeDeLiveAiJobs.pulseCredit:Ljava/lang/ThreadLocal;
 ALOAD 8
 INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
-LABEL L22
+LABEL L25
 ALOAD 7
 INVOKEVIRTUAL local/lito381/server/NativeDeLiveAiJobs$Credit.release()V
-GOTO L31
-LABEL L23
+GOTO L34
+LABEL L26
 ASTORE 9
 ALOAD 9
 INVOKESTATIC local/lito381/server/CombatNpcDamageQueue.failed(Ljava/lang/Throwable;)V
-LABEL L24
+LABEL L27
 ALOAD 8
-IFNONNULL L25
+IFNONNULL L28
 GETSTATIC local/lito381/server/NativeDeLiveAiJobs.pulseCredit:Ljava/lang/ThreadLocal;
 INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L26
-LABEL L25
+GOTO L29
+LABEL L28
 GETSTATIC local/lito381/server/NativeDeLiveAiJobs.pulseCredit:Ljava/lang/ThreadLocal;
 ALOAD 8
 INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
-LABEL L26
+LABEL L29
 ALOAD 7
 INVOKEVIRTUAL local/lito381/server/NativeDeLiveAiJobs$Credit.release()V
-GOTO L31
-LABEL L27
+GOTO L34
+LABEL L30
 ASTORE 10
-LABEL L28
+LABEL L31
 ALOAD 8
-IFNONNULL L29
+IFNONNULL L32
 GETSTATIC local/lito381/server/NativeDeLiveAiJobs.pulseCredit:Ljava/lang/ThreadLocal;
 INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L30
-LABEL L29
+GOTO L33
+LABEL L32
 GETSTATIC local/lito381/server/NativeDeLiveAiJobs.pulseCredit:Ljava/lang/ThreadLocal;
 ALOAD 8
 INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
-LABEL L30
+LABEL L33
 ALOAD 7
 INVOKEVIRTUAL local/lito381/server/NativeDeLiveAiJobs$Credit.release()V
 ALOAD 10
 ATHROW
-LABEL L31
-GOTO L33
-LABEL L32
-ALOAD 6
-GETFIELD local/lito381/server/NativeDeLiveAiJobs$Binding.actor:Llocal/lito381/server/NativeDeInstanceState;
-CONST Integer:0
-INVOKESTATIC local/lito381/server/NativeDeLiveAiJobs.background(Llocal/lito381/server/NativeDeInstanceState;Z)V
-LABEL L33
+LABEL L34
 IINC var=5 incr=1
 GOTO L10
-LABEL L34
+LABEL L35
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeLiveAiJobs$Credit.release()V
-GOTO L37
-LABEL L35
+GOTO L38
+LABEL L36
 ASTORE 11
-LABEL L36
+LABEL L37
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeLiveAiJobs$Credit.release()V
 ALOAD 11
 ATHROW
-LABEL L37
+LABEL L38
 RETURN
 method=reserve(Llocal/lito381/server/NativeDeInstanceState;Ljava/lang/Runnable;ZJ)Llocal/lito381/server/NativeDeLiveAiJobs$Ticket;
 access=0xa
@@ -1196,17 +1197,19 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault=
-TRY L0 L1 L2 java/lang/Throwable  
-TRY L2 L3 L2 java/lang/Throwable  
-TRY L7 L9 L10 java/lang/Throwable  
-TRY L10 L11 L10 java/lang/Throwable  
+TRY L1 L2 L3 java/lang/Throwable  
+TRY L3 L4 L3 java/lang/Throwable  
+TRY L8 L10 L11 java/lang/Throwable  
+TRY L11 L12 L11 java/lang/Throwable  
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.stopAI()V
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeDeLiveAiJobs.find(Llocal/lito381/server/NativeDeInstanceState;)Llocal/lito381/server/NativeDeLiveAiJobs$Binding;
 ASTORE 1
 ALOAD 1
-IFNULL L12
+IFNONNULL L0
+RETURN
+LABEL L0
 ALOAD 0
 CONST Integer:0
 INVOKESTATIC local/lito381/server/NativeDeLiveAiJobs.background(Llocal/lito381/server/NativeDeInstanceState;Z)V
@@ -1217,7 +1220,7 @@
 DUP
 ASTORE 3
 MONITORENTER
-LABEL L0
+LABEL L1
 ALOAD 1
 CONST Integer:1
 PUTFIELD local/lito381/server/NativeDeLiveAiJobs$Binding.closed:Z
@@ -1240,16 +1243,16 @@
 ASTORE 2
 ALOAD 3
 MONITOREXIT
-LABEL L1
-GOTO L4
 LABEL L2
+GOTO L5
+LABEL L3
 ASTORE 4
 ALOAD 3
 MONITOREXIT
-LABEL L3
+LABEL L4
 ALOAD 4
 ATHROW
-LABEL L4
+LABEL L5
 ALOAD 2
 ASTORE 3
 ALOAD 3
@@ -1257,10 +1260,10 @@
 ISTORE 4
 CONST Integer:0
 ISTORE 5
-LABEL L5
+LABEL L6
 ILOAD 5
 ILOAD 4
-IF_ICMPGE L6
+IF_ICMPGE L7
 ALOAD 3
 ILOAD 5
 AALOAD
@@ -1270,8 +1273,8 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeLiveAiJobs$Ticket.cancel(Z)Z
 POP
 IINC var=5 incr=1
-GOTO L5
-LABEL L6
+GOTO L6
+LABEL L7
 ALOAD 1
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Binding.owner:Llocal/lito381/server/NativeDeLiveAiJobs$Owner;
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Owner.life:Llocal/lito381/server/NativePlayerLifetime;
@@ -1279,27 +1282,27 @@
 DUP
 ASTORE 3
 MONITORENTER
-LABEL L7
+LABEL L8
 ALOAD 1
 GETFIELD local/lito381/server/NativeDeLiveAiJobs$Binding.tasks:I
-IFNE L8
+IFNE L9
 ALOAD 0
 CONST str:"sourceDeLiveAiBinding"
 CONST null
 INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
-LABEL L8
-ALOAD 3
-MONITOREXIT
 LABEL L9
-GOTO L12
+ALOAD 3
+MONITOREXIT
 LABEL L10
+GOTO L13
+LABEL L11
 ASTORE 7
 ALOAD 3
 MONITOREXIT
-LABEL L11
-ALOAD 7
-ATHROW
 LABEL L12
+ALOAD 7
+ATHROW
+LABEL L13
 RETURN
 method=retireDeleted(Llocal/lito381/server/NativeDeInstanceState;)V
 access=0x8
~~~

</details>

<details>
<summary>NativeDeNpcConstructionState — DIFF</summary>

待主控裁定：class access 缺 ACC_SUPER，另有 190 個不同方法，不能只忽略 class 旗標；完整方法差異如下。

左側正規化 SHA-256：540c0750ce364d950ca65797e122a2513e6b4ca1d9a60c7f340a027211475546。  
右側正規化 SHA-256：efa17f70490f86f5d483d361e75b797f6f25028816a520b58de3a738b531f24d。

~~~diff
--- staged/NativeDeNpcConstructionState
+++ formal/NativeDeNpcConstructionState
@@ -1,5 +1,5 @@
 class=local/lito381/server/NativeDeNpcConstructionState
-access=21
+access=1
 super=local/lito381/server/NativeDeNpcTemplateState
 interfaces=
 signature=
@@ -9,18 +9,7 @@
 nestMembers=
 permittedSubclasses=
 annotations=|||
-innerClass=local/lito381/server/NativeDeChatScope$Clock|local/lito381/server/NativeDeChatScope|Clock|0x18
-innerClass=local/lito381/server/NativeDeDeletionPorts$Hate|local/lito381/server/NativeDeDeletionPorts|Hate|0x608
-innerClass=local/lito381/server/NativeDeDeletionPorts$Inventory|local/lito381/server/NativeDeDeletionPorts|Inventory|0x608
-innerClass=local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608
-innerClass=local/lito381/server/NativeDeDeletionPorts$Template|local/lito381/server/NativeDeDeletionPorts|Template|0x608
-innerClass=local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408
-innerClass=local/lito381/server/NativeDeNpcConstructionPorts$Services|local/lito381/server/NativeDeNpcConstructionPorts|Services|0x608
-innerClass=local/lito381/server/NativeDeNpcTemplatePorts$Services|local/lito381/server/NativeDeNpcTemplatePorts|Services|0x608
-innerClass=local/lito381/server/NativeDePerceptionPorts$Poison|local/lito381/server/NativeDePerceptionPorts|Poison|0x408
-innerClass=local/lito381/server/NativeDePerceptionPorts$Services|local/lito381/server/NativeDePerceptionPorts|Services|0x608
-innerClass=local/lito381/server/NativeDePerceptionSpeed$ActionType|local/lito381/server/NativeDePerceptionSpeed|ActionType|0x4019
-innerClass=local/lito381/server/NativeDeWorldKinds$Item|local/lito381/server/NativeDeWorldKinds|Item|0x8
+innerClass=
 recordComponents=
 field=ISASCAPE:Z
 access=0x4
@@ -220,7 +209,7 @@
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState.nativeServices:Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;
 INVOKESPECIAL local/lito381/server/NativeDeNpcInventory.<init>(Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;)V
-PUTFIELD local/lito381/server/NativeDeNpcConstructionState.inventory:Llocal/lito381/server/NativeDeDeletionPorts$Inventory;
+PUTFIELD local/lito381/server/NativeDeDeletionState.inventory:Llocal/lito381/server/NativeDeDeletionPorts$Inventory;
 ALOAD 0
 CONST Integer:1
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState.firstFound:Z
@@ -264,12 +253,12 @@
 NEW local/lito381/server/NativeDeNpcHateList
 DUP
 INVOKESPECIAL local/lito381/server/NativeDeNpcHateList.<init>()V
-PUTFIELD local/lito381/server/NativeDeNpcConstructionState._hateList:Llocal/lito381/server/NativeDeDeletionPorts$Hate;
+PUTFIELD local/lito381/server/NativeDeDeletionState._hateList:Llocal/lito381/server/NativeDeDeletionPorts$Hate;
 ALOAD 0
 NEW local/lito381/server/NativeDeNpcHateList
 DUP
 INVOKESPECIAL local/lito381/server/NativeDeNpcHateList.<init>()V
-PUTFIELD local/lito381/server/NativeDeNpcConstructionState._dropHateList:Llocal/lito381/server/NativeDeDeletionPorts$Hate;
+PUTFIELD local/lito381/server/NativeDeDeletionState._dropHateList:Llocal/lito381/server/NativeDeDeletionPorts$Hate;
 ALOAD 0
 CONST null
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._target:Llocal/lito381/server/NativeDeCharacterFields;
@@ -342,285 +331,6 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcConstructionState.setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V
 LABEL L0
 RETURN
-method=addAc(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addAc(I)V
-RETURN
-method=addBowDmgup(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addBowDmgup(I)V
-RETURN
-method=addBowHitup(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addBowHitup(I)V
-RETURN
-method=addCha(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addCha(I)V
-RETURN
-method=addCon(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addCon(I)V
-RETURN
-method=addDex(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addDex(I)V
-RETURN
-method=addDmgup(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addDmgup(I)V
-RETURN
-method=addEarth(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addEarth(I)V
-RETURN
-method=addFire(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addFire(I)V
-RETURN
-method=addHitup(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addHitup(I)V
-RETURN
-method=addInt(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addInt(I)V
-RETURN
-method=addKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
-RETURN
-method=addMaxHp(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addMaxHp(I)V
-RETURN
-method=addMaxMp(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addMaxMp(I)V
-RETURN
-method=addMr(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addMr(I)V
-RETURN
-method=addRegistBlind(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addRegistBlind(I)V
-RETURN
-method=addRegistSleep(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addRegistSleep(I)V
-RETURN
-method=addRegistStone(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addRegistStone(I)V
-RETURN
-method=addRegistStun(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addRegistStun(I)V
-RETURN
-method=addRegistSustain(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addRegistSustain(I)V
-RETURN
-method=addSp(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addSp(I)V
-RETURN
-method=addStr(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addStr(I)V
-RETURN
-method=addStunSuccess(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addStunSuccess(I)V
-RETURN
-method=addWater(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addWater(I)V
-RETURN
-method=addWeaponBreakStack()V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addWeaponBreakStack()V
-RETURN
-method=addWind(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addWind(I)V
-RETURN
-method=addWis(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addWis(I)V
-RETURN
-method=add_regist_freeze(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.add_regist_freeze(I)V
-RETURN
 method=allTargetClear()V
 access=0x1
 signature=
@@ -642,50 +352,40 @@
 CONST null
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._pendingFamilyTarget:Llocal/lito381/server/NativeDeCharacterFields;
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeNpcConstructionState._hateList:Llocal/lito381/server/NativeDeDeletionPorts$Hate;
+GETFIELD local/lito381/server/NativeDeDeletionState._hateList:Llocal/lito381/server/NativeDeDeletionPorts$Hate;
 CHECKCAST local/lito381/server/NativeDeNpcHateList
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcHateList.clear()V
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeNpcConstructionState._dropHateList:Llocal/lito381/server/NativeDeDeletionPorts$Hate;
+GETFIELD local/lito381/server/NativeDeDeletionState._dropHateList:Llocal/lito381/server/NativeDeDeletionPorts$Hate;
 CHECKCAST local/lito381/server/NativeDeNpcHateList
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcHateList.clear()V
 ALOAD 0
 CONST null
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._target:Llocal/lito381/server/NativeDeCharacterFields;
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeNpcConstructionState._targetItemLock:Ljava/util/concurrent/locks/ReentrantLock;
+GETFIELD local/lito381/server/NativeDeDeletionState._targetItemLock:Ljava/util/concurrent/locks/ReentrantLock;
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.lock()V
 LABEL L1
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeNpcConstructionState._targetItemList:Ljava/util/List;
+GETFIELD local/lito381/server/NativeDeDeletionState._targetItemList:Ljava/util/List;
 INVOKEINTERFACE java/util/List.clear()V itf
 ALOAD 0
 CONST null
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._targetItem:Llocal/lito381/server/NativeDeWorldKinds$Item;
 LABEL L2
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeNpcConstructionState._targetItemLock:Ljava/util/concurrent/locks/ReentrantLock;
+GETFIELD local/lito381/server/NativeDeDeletionState._targetItemLock:Ljava/util/concurrent/locks/ReentrantLock;
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
 GOTO L4
 LABEL L3
 ASTORE 1
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeNpcConstructionState._targetItemLock:Ljava/util/concurrent/locks/ReentrantLock;
+GETFIELD local/lito381/server/NativeDeDeletionState._targetItemLock:Ljava/util/concurrent/locks/ReentrantLock;
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
 ALOAD 1
 ATHROW
 LABEL L4
 RETURN
-method=broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
-RETURN
 method=calcSleepTime(II)I
 access=0x1
 signature=
@@ -697,23 +397,27 @@
 CONST Integer:960
 ISTORE 1
 LABEL L0
-ALOAD 0
-ILOAD 1
 ILOAD 2
-LOOKUPSWITCH default=L3 keys=0,1 labels=L1,L2
+TABLESWITCH min=0 max=2 default=L3 labels=L1,L2,L3
 LABEL L1
 GETSTATIC local/lito381/server/NativeDePerceptionSpeed$ActionType.MOVE:Llocal/lito381/server/NativeDePerceptionSpeed$ActionType;
+ASTORE 3
 GOTO L4
 LABEL L2
 GETSTATIC local/lito381/server/NativeDePerceptionSpeed$ActionType.ATTACK:Llocal/lito381/server/NativeDePerceptionSpeed$ActionType;
+ASTORE 3
 GOTO L4
 LABEL L3
 GETSTATIC local/lito381/server/NativeDePerceptionSpeed$ActionType.SPELL:Llocal/lito381/server/NativeDePerceptionSpeed$ActionType;
+ASTORE 3
 LABEL L4
+ALOAD 0
+ILOAD 1
+ALOAD 3
 INVOKESTATIC local/lito381/server/NativeDePerceptionSpeed.calculate(Llocal/lito381/server/NativeDePerceptionState;ILlocal/lito381/server/NativeDePerceptionSpeed$ActionType;)I
 IRETURN
 method=cancelNpcChatTimer()V
-access=0x0
+access=0x2
 signature=
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
@@ -721,16 +425,6 @@
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeDeChatRuntime.cancel(Llocal/lito381/server/NativeDeNpcConstructionState;)V
 RETURN
-method=claimExpiredCorpseDeletion(J)Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-LLOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.claimExpiredCorpseDeletion(J)Z
-IRETURN
 method=commitManaDamage(Llocal/lito381/server/NativeDeCharacterFields;I)I
 access=0x1
 signature=
@@ -746,7 +440,7 @@
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault=
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeNpcConstructionState._aiOwnerToken:Ljava/util/concurrent/atomic/AtomicLong;
+GETFIELD local/lito381/server/NativeDePerceptionState._aiOwnerToken:Ljava/util/concurrent/atomic/AtomicLong;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.get()J
 LRETURN
 method=deferFamilyLinkFromPcAttacker(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
@@ -762,223 +456,6 @@
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._pendingFamilyTarget:Llocal/lito381/server/NativeDeCharacterFields;
 LABEL L0
 RETURN
-method=deleteMe()V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.deleteMe()V
-RETURN
-method=destroyed()Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.destroyed()Z
-IRETURN
-method=forward(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.forward(I)V
-RETURN
-method=getAc()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getAc()I
-IRETURN
-method=getAddAttrKind()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getAddAttrKind()I
-IRETURN
-method=getAtkspeed()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getAtkspeed()I
-IRETURN
-method=getBowDmgup()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getBowDmgup()I
-IRETURN
-method=getBowHitup()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getBowHitup()I
-IRETURN
-method=getBraveSpeed()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getBraveSpeed()I
-IRETURN
-method=getCha()S
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getCha()S
-IRETURN
-method=getChaLightSize()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getChaLightSize()I
-IRETURN
-method=getClanid()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getClanid()I
-IRETURN
-method=getClanname()Ljava/lang/String;
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getClanname()Ljava/lang/String;
-ARETURN
-method=getCon()S
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getCon()S
-IRETURN
-method=getCurrentHp()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getCurrentHp()I
-IRETURN
-method=getCurrentMp()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getCurrentMp()I
-IRETURN
-method=getDex()S
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getDex()S
-IRETURN
-method=getDmgup()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getDmgup()I
-IRETURN
-method=getDolls()Ljava/util/Map;
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getDolls()Ljava/util/Map;
-ARETURN
-method=getEarth()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getEarth()I
-IRETURN
-method=getExp()J
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getExp()J
-LRETURN
-method=getFire()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getFire()I
-IRETURN
-method=getGfxId()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getGfxId()I
-IRETURN
-method=getHeading()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getHeading()I
-IRETURN
 method=getHiddenStatus()I
 access=0x1
 signature=
@@ -988,15 +465,6 @@
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._hiddenStatus:I
 IRETURN
-method=getHitup()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getHitup()I
-IRETURN
 method=getHomeX()I
 access=0x1
 signature=
@@ -1014,24 +482,6 @@
 annotationDefault=
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._homeY:I
-IRETURN
-method=getId()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getId()I
-IRETURN
-method=getInt()S
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getInt()S
 IRETURN
 method=getInventory()Llocal/lito381/server/NativeDeDeletionPorts$Inventory;
 access=0x41
@@ -1049,36 +499,9 @@
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault=
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeNpcConstructionState.inventory:Llocal/lito381/server/NativeDeDeletionPorts$Inventory;
+GETFIELD local/lito381/server/NativeDeDeletionState.inventory:Llocal/lito381/server/NativeDeDeletionPorts$Inventory;
 CHECKCAST local/lito381/server/NativeDeNpcInventory
 ARETURN
-method=getKarma()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getKarma()I
-IRETURN
-method=getKnownObjects()Ljava/util/List;
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getKnownObjects()Ljava/util/List;
-ARETURN
-method=getKnownPlayers()Ljava/util/List;
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getKnownPlayers()Ljava/util/List;
-ARETURN
 method=getLastMoveTimeMs()J
 access=0x1
 signature=
@@ -1088,79 +511,6 @@
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._lastMoveTimeMs:J
 LRETURN
-method=getLawful()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getLawful()I
-IRETURN
-method=getLevel()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getLevel()I
-IRETURN
-method=getLightSize()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getLightSize()I
-IRETURN
-method=getLineDistance(Llocal/lito381/server/NativeDeObjectFields;)D
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getLineDistance(Llocal/lito381/server/NativeDeObjectFields;)D
-DRETURN
-method=getLocation()Llocal/lito381/server/NativeDeWorldLocation;
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getLocation()Llocal/lito381/server/NativeDeWorldLocation;
-ARETURN
-method=getMagicBonus()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getMagicBonus()I
-IRETURN
-method=getMagicLevel()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getMagicLevel()I
-IRETURN
-method=getMap()Llocal/lito381/server/NativeDeWorldMap;
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getMap()Llocal/lito381/server/NativeDeWorldMap;
-ARETURN
 method=getMapId()S
 access=0x1
 signature=
@@ -1169,24 +519,6 @@
 annotationDefault=
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeDeObjectBindings.mapId(Llocal/lito381/server/NativeDeObjectFields;)S
-IRETURN
-method=getMaxHp()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getMaxHp()I
-IRETURN
-method=getMaxMp()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getMaxMp()I
 IRETURN
 method=getMobGroupId()I
 access=0x1
@@ -1206,15 +538,6 @@
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._npcMove:Llocal/lito381/server/NativeDeNpcMoveExecutor;
 ARETURN
-method=getMoveSpeed()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getMoveSpeed()I
-IRETURN
 method=getMovementDistance()I
 access=0x1
 signature=
@@ -1224,33 +547,6 @@
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._movementDistance:I
 IRETURN
-method=getMr()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getMr()I
-IRETURN
-method=getName()Ljava/lang/String;
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getName()Ljava/lang/String;
-ARETURN
-method=getNameId()Ljava/lang/String;
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getNameId()Ljava/lang/String;
-ARETURN
 method=getNpcId()I
 access=0x1
 signature=
@@ -1258,46 +554,10 @@
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault=
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeNpcConstructionState.template:Llocal/lito381/server/NativeDeDeletionPorts$Template;
+GETFIELD local/lito381/server/NativeDeDeletionState.template:Llocal/lito381/server/NativeDeDeletionPorts$Template;
 CHECKCAST local/lito381/server/NativeDeNpcTemplate
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_npcId()I
 IRETURN
-method=getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;
-ARETURN
-method=getOwnLightSize()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getOwnLightSize()I
-IRETURN
-method=getPassispeed()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getPassispeed()I
-IRETURN
-method=getPetList()Ljava/util/Map;
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getPetList()Ljava/util/Map;
-ARETURN
 method=getPetcost()I
 access=0x1
 signature=
@@ -1307,15 +567,6 @@
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._petcost:I
 IRETURN
-method=getPoison()Llocal/lito381/server/NativeDePerceptionPorts$Poison;
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getPoison()Llocal/lito381/server/NativeDePerceptionPorts$Poison;
-ARETURN
 method=getPrevX()I
 access=0x1
 signature=
@@ -1333,60 +584,6 @@
 annotationDefault=
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._prevY:I
-IRETURN
-method=getRegistBlind()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getRegistBlind()I
-IRETURN
-method=getRegistFreeze()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getRegistFreeze()I
-IRETURN
-method=getRegistSleep()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getRegistSleep()I
-IRETURN
-method=getRegistStone()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getRegistStone()I
-IRETURN
-method=getRegistStun()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getRegistStun()I
-IRETURN
-method=getRegistSustain()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getRegistSustain()I
 IRETURN
 method=getSleepTime()I
 access=0x4
@@ -1397,143 +594,6 @@
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._sleep_time:I
 IRETURN
-method=getSp()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getSp()I
-IRETURN
-method=getStatus()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getStatus()I
-IRETURN
-method=getStr()S
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getStr()S
-IRETURN
-method=getStunSuccess()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getStunSuccess()I
-IRETURN
-method=getTempCharGfx()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getTempCharGfx()I
-IRETURN
-method=getTempLawful()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getTempLawful()I
-IRETURN
-method=getTileDistance(Llocal/lito381/server/NativeDeObjectFields;)I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getTileDistance(Llocal/lito381/server/NativeDeObjectFields;)I
-IRETURN
-method=getTileLineDistance(Llocal/lito381/server/NativeDeObjectFields;)I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getTileLineDistance(Llocal/lito381/server/NativeDeObjectFields;)I
-IRETURN
-method=getTitle()Ljava/lang/String;
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getTitle()Ljava/lang/String;
-ARETURN
-method=getTrueMr()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getTrueMr()I
-IRETURN
-method=getTrueSp()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getTrueSp()I
-IRETURN
-method=getWater()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getWater()I
-IRETURN
-method=getWeaponBreakStack()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getWeaponBreakStack()I
-IRETURN
-method=getWind()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getWind()I
-IRETURN
-method=getWis()S
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getWis()S
-IRETURN
 method=getX()I
 access=0x1
 signature=
@@ -1552,33 +612,6 @@
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeDeObjectBindings.y(Llocal/lito381/server/NativeDeObjectFields;)I
 IRETURN
-method=get_deadTimerTemp()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.get_deadTimerTemp()I
-IRETURN
-method=get_fishX()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.get_fishX()I
-IRETURN
-method=get_fishY()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.get_fishY()I
-IRETURN
 method=get_stop_time()I
 access=0x1
 signature=
@@ -1596,16 +629,6 @@
 annotationDefault=
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._work_time:I
-IRETURN
-method=hasSkillEffect(I)Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.hasSkillEffect(I)Z
 IRETURN
 method=isActived()Z
 access=0x4
@@ -1616,33 +639,6 @@
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._actived:Z
 IRETURN
-method=isAgro()Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.isAgro()Z
-IRETURN
-method=isAgrocoi()Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.isAgrocoi()Z
-IRETURN
-method=isAgrososc()Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.isAgrososc()Z
-IRETURN
 method=isAiOwner(J)Z
 access=0x14
 signature=
@@ -1654,7 +650,7 @@
 LCMP
 IFEQ L0
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeNpcConstructionState._aiOwnerToken:Ljava/util/concurrent/atomic/AtomicLong;
+GETFIELD local/lito381/server/NativeDePerceptionState._aiOwnerToken:Ljava/util/concurrent/atomic/AtomicLong;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.get()J
 LLOAD 1
 LCMP
@@ -1665,24 +661,6 @@
 CONST Integer:0
 LABEL L1
 IRETURN
-method=isAiRunning()Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.isAiRunning()Z
-IRETURN
-method=isDead()Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.isDead()Z
-IRETURN
 method=isDeathProcessing()Z
 access=0x4
 signature=
@@ -1701,52 +679,6 @@
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._firstAttack:Z
 IRETURN
-method=isFishing()Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.isFishing()Z
-IRETURN
-method=isInvisble()Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.isInvisble()Z
-IRETURN
-method=isKnownNpcViewerConsistent(Llocal/lito381/server/NativeDeDeletionState;)Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.isKnownNpcViewerConsistent(Llocal/lito381/server/NativeDeDeletionState;)Z
-IRETURN
-method=isParalyzed()Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.isParalyzed()Z
-IRETURN
-method=isPickupItem()Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.isPickupItem()Z
-IRETURN
 method=isRest()Z
 access=0x1
 signature=
@@ -1764,24 +696,6 @@
 annotationDefault=
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._isResurrect:Z
-IRETURN
-method=isShop()Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.isShop()Z
-IRETURN
-method=isSleeped()Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.isSleeped()Z
 IRETURN
 method=is_now_target()Llocal/lito381/server/NativeDeCharacterFields;
 access=0x1
@@ -1809,16 +723,6 @@
 annotationDefault=
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._isremovearmor:Z
-IRETURN
-method=knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
 IRETURN
 method=lambda$setDeathProcessing$#0(Z)V
 access=0x2
@@ -1840,16 +744,6 @@
 ASTORE 2
 ALOAD 2
 ARETURN
-method=onAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.onAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
-RETURN
 method=onDeleteClaimed()V
 access=0x4
 signature=
@@ -1878,36 +772,6 @@
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault=
 RETURN
-method=onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
-RETURN
-method=onTalkAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.onTalkAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
-RETURN
-method=rebindWorldMap(S)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.rebindWorldMap(S)V
-RETURN
 method=receiveDamage(Llocal/lito381/server/NativeDeCharacterFields;I)V
 access=0x1
 signature=
@@ -1926,7 +790,7 @@
 LCMP
 IFEQ L0
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeNpcConstructionState._aiOwnerToken:Ljava/util/concurrent/atomic/AtomicLong;
+GETFIELD local/lito381/server/NativeDePerceptionState._aiOwnerToken:Ljava/util/concurrent/atomic/AtomicLong;
 LLOAD 1
 CONST Long:0
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.compareAndSet(JJ)Z
@@ -1937,35 +801,6 @@
 CONST Integer:0
 LABEL L1
 IRETURN
-method=removeAllKnownObjects()V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.removeAllKnownObjects()V
-RETURN
-method=removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
-RETURN
-method=removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
-IRETURN
 method=resetPathFailureState()V
 access=0x14
 signature=
@@ -1978,16 +813,6 @@
 ALOAD 0
 CONST null
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._pendingFamilyTarget:Llocal/lito381/server/NativeDeCharacterFields;
-RETURN
-method=resurrect(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.resurrect(I)V
 RETURN
 method=scheduleNpcChatTimer(Llocal/lito381/server/NativeDeChatTimer;JJ)Ljava/util/concurrent/ScheduledFuture;
 access=0x2
@@ -2025,6 +850,7 @@
 ALOAD 1
 LLOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeChatScope$Clock.aiSchedule(Ljava/lang/Runnable;J)Ljava/util/concurrent/ScheduledFuture;
+ASTORE 6
 GOTO L4
 LABEL L3
 INVOKESTATIC local/lito381/server/NativeDeChatScope$Clock.get()Llocal/lito381/server/NativeDeChatScope$Clock;
@@ -2032,8 +858,8 @@
 LLOAD 2
 LLOAD 4
 INVOKEVIRTUAL local/lito381/server/NativeDeChatScope$Clock.aiScheduleAtFixedRate(Ljava/lang/Runnable;JJ)Ljava/util/concurrent/ScheduledFuture;
+ASTORE 6
 LABEL L4
-ASTORE 6
 ALOAD 6
 IFNULL L6
 ALOAD 6
@@ -2098,16 +924,6 @@
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault=
 RETURN
-method=setAc(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setAc(I)V
-RETURN
 method=setActived(Z)V
 access=0x4
 signature=
@@ -2118,56 +934,6 @@
 ILOAD 1
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._actived:Z
 RETURN
-method=setAddAttrKind(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setAddAttrKind(I)V
-RETURN
-method=setAgro(Z)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setAgro(Z)V
-RETURN
-method=setAgrocoi(Z)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setAgrocoi(Z)V
-RETURN
-method=setAgrososc(Z)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setAgrososc(Z)V
-RETURN
-method=setAtkspeed(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setAtkspeed(I)V
-RETURN
 method=setBowActId(I)V
 access=0x1
 signature=
@@ -2177,76 +943,6 @@
 ALOAD 0
 ILOAD 1
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._bowActId:I
-RETURN
-method=setBraveSpeed(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setBraveSpeed(I)V
-RETURN
-method=setCha(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setCha(I)V
-RETURN
-method=setChaLightSize(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setChaLightSize(I)V
-RETURN
-method=setCon(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setCon(I)V
-RETURN
-method=setDead(Z)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setDead(Z)V
-RETURN
-method=setDex(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setDex(I)V
-RETURN
-method=setExp(J)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-LLOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setExp(J)V
 RETURN
 method=setFirstAttack(Z)V
 access=0x4
@@ -2258,36 +954,6 @@
 ILOAD 1
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._firstAttack:Z
 RETURN
-method=setGfxId(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setGfxId(I)V
-RETURN
-method=setGfxidInStatus(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setGfxidInStatus(I)V
-RETURN
-method=setHeading(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setHeading(I)V
-RETURN
 method=setHiddenStatus(I)V
 access=0x1
 signature=
@@ -2318,26 +984,6 @@
 ILOAD 1
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._homeY:I
 RETURN
-method=setId(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setId(I)V
-RETURN
-method=setInt(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setInt(I)V
-RETURN
 method=setInventory(Llocal/lito381/server/NativeDeNpcInventory;)V
 access=0x1
 signature=
@@ -2346,47 +992,7 @@
 annotationDefault=
 ALOAD 0
 ALOAD 1
-PUTFIELD local/lito381/server/NativeDeNpcConstructionState.inventory:Llocal/lito381/server/NativeDeDeletionPorts$Inventory;
-RETURN
-method=setKarma(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setKarma(I)V
-RETURN
-method=setLawful(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setLawful(I)V
-RETURN
-method=setLevel(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setLevel(I)V
-RETURN
-method=setLightSize(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setLightSize(I)V
+PUTFIELD local/lito381/server/NativeDeDeletionState.inventory:Llocal/lito381/server/NativeDeDeletionPorts$Inventory;
 RETURN
 method=setLink(Llocal/lito381/server/NativeDeCharacterFields;)V
 access=0x1
@@ -2395,68 +1001,6 @@
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault=
 RETURN
-method=setLocation(III)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-ILOAD 2
-ILOAD 3
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setLocation(III)V
-RETURN
-method=setLocation(Llocal/lito381/server/NativeDeWorldLocation;)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setLocation(Llocal/lito381/server/NativeDeWorldLocation;)V
-RETURN
-method=setMap(Llocal/lito381/server/NativeDeWorldMap;)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setMap(Llocal/lito381/server/NativeDeWorldMap;)V
-RETURN
-method=setMap(S)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setMap(S)V
-RETURN
-method=setMaxHp(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setMaxHp(I)V
-RETURN
-method=setMaxMp(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setMaxMp(I)V
-RETURN
 method=setMobGroupId(I)V
 access=0x1
 signature=
@@ -2467,16 +1011,6 @@
 ILOAD 1
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._mobGroupId:I
 RETURN
-method=setMoveSpeed(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setMoveSpeed(I)V
-RETURN
 method=setMovementDistance(I)V
 access=0x1
 signature=
@@ -2487,56 +1021,6 @@
 ILOAD 1
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._movementDistance:I
 RETURN
-method=setMr(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setMr(I)V
-RETURN
-method=setName(Ljava/lang/String;)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setName(Ljava/lang/String;)V
-RETURN
-method=setNameId(Ljava/lang/String;)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setNameId(Ljava/lang/String;)V
-RETURN
-method=setOwnLightSize(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setOwnLightSize(I)V
-RETURN
-method=setPassispeed(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setPassispeed(I)V
-RETURN
 method=setPetcost(I)V
 access=0x1
 signature=
@@ -2547,16 +1031,6 @@
 ILOAD 1
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._petcost:I
 RETURN
-method=setPickupItem(Z)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setPickupItem(Z)V
-RETURN
 method=setRest(Z)V
 access=0x1
 signature=
@@ -2586,117 +1060,6 @@
 ALOAD 0
 ILOAD 1
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._sleep_time:I
-RETURN
-method=setStatus(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setStatus(I)V
-RETURN
-method=setStr(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setStr(I)V
-RETURN
-method=setTempCharGfx(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setTempCharGfx(I)V
-RETURN
-method=setTempLawful(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setTempLawful(I)V
-RETURN
-method=setTitle(Ljava/lang/String;)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setTitle(Ljava/lang/String;)V
-RETURN
-method=setWeaponBreakStack(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setWeaponBreakStack(I)V
-RETURN
-method=setWis(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setWis(I)V
-RETURN
-method=setX(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setX(I)V
-RETURN
-method=setXY(II)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-ILOAD 2
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setXY(II)V
-RETURN
-method=setY(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setY(I)V
-RETURN
-method=set_deadTimerTemp(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.set_deadTimerTemp(I)V
 RETURN
 method=set_now_targetItem(Llocal/lito381/server/NativeDeWorldKinds$Item;)V
 access=0x1
@@ -2728,16 +1091,6 @@
 ILOAD 1
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._isremovearmor:Z
 RETURN
-method=set_showId(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.set_showId(I)V
-RETURN
 method=set_work_time(I)V
 access=0x1
 signature=
@@ -2756,17 +1109,7 @@
 annotationDefault=
 ALOAD 0
 ILOAD 1
-PUTFIELD local/lito381/server/NativeDeNpcConstructionState.respawn:Z
-RETURN
-method=setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V
+PUTFIELD local/lito381/server/NativeDeDeletionState.respawn:Z
 RETURN
 method=sourceChat$cancelNpcChatTimer()V
 access=0x2
@@ -2872,10 +1215,10 @@
 ALOAD 3
 INVOKESPECIAL local/lito381/server/NativeDeChatTimer.<init>(Llocal/lito381/server/NativeDeNpcConstructionState;Llocal/lito381/server/NativeDeChatData;)V
 ASTORE 4
-ALOAD 0
 ALOAD 3
 INVOKEVIRTUAL local/lito381/server/NativeDeChatData.isRepeat()Z
 IFNE L10
+ALOAD 0
 ALOAD 0
 ALOAD 4
 ALOAD 3
@@ -2883,8 +1226,10 @@
 I2L
 CONST Long:0
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcConstructionState.scheduleNpcChatTimer(Llocal/lito381/server/NativeDeChatTimer;JJ)Ljava/util/concurrent/ScheduledFuture;
+PUTFIELD local/lito381/server/NativeDeNpcConstructionState._monitorFuture:Ljava/util/concurrent/ScheduledFuture;
 GOTO L11
 LABEL L10
+ALOAD 0
 ALOAD 0
 ALOAD 4
 ALOAD 3
@@ -2894,17 +1239,8 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeChatData.getRepeatInterval()I
 I2L
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcConstructionState.scheduleNpcChatTimer(Llocal/lito381/server/NativeDeChatTimer;JJ)Ljava/util/concurrent/ScheduledFuture;
+PUTFIELD local/lito381/server/NativeDeNpcConstructionState._monitorFuture:Ljava/util/concurrent/ScheduledFuture;
 LABEL L11
-PUTFIELD local/lito381/server/NativeDeNpcConstructionState._monitorFuture:Ljava/util/concurrent/ScheduledFuture;
-RETURN
-method=sourceDeleteDe()V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.sourceDeleteDe()V
 RETURN
 method=startChat(I)V
 access=0x1
@@ -2915,15 +1251,6 @@
 ALOAD 0
 ILOAD 1
 INVOKESTATIC local/lito381/server/NativeDeChatRuntime.start(Llocal/lito381/server/NativeDeNpcConstructionState;I)V
-RETURN
-method=start_shop()V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.start_shop()V
 RETURN
 method=tryAcquireAiOwner()J
 access=0x14
@@ -2932,7 +1259,7 @@
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault=
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeNpcConstructionState._aiOwnerToken:Ljava/util/concurrent/atomic/AtomicLong;
+GETFIELD local/lito381/server/NativeDePerceptionState._aiOwnerToken:Ljava/util/concurrent/atomic/AtomicLong;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.get()J
 CONST Long:0
 LCMP
@@ -2954,7 +1281,7 @@
 LSTORE 1
 LABEL L1
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeNpcConstructionState._aiOwnerToken:Ljava/util/concurrent/atomic/AtomicLong;
+GETFIELD local/lito381/server/NativeDePerceptionState._aiOwnerToken:Ljava/util/concurrent/atomic/AtomicLong;
 CONST Long:0
 LLOAD 1
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.compareAndSet(JJ)Z
@@ -2965,13 +1292,3 @@
 CONST Long:0
 LABEL L3
 LRETURN
-method=tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
-IRETURN
~~~

</details>

<details>
<summary>NativeDeNpcExecutor — DIFF</summary>

已驗證目前差異只出現在 class metadata；欄位及全部方法的本工具正規化輸出一致。metadata 是否可豁免仍待主控核准。

左側正規化 SHA-256：a9cedc47d53828aa065645ccecf35a8d10ffe797c7420c6896b733c504554f05。  
右側正規化 SHA-256：d1361df79cff3b6540f5473b3846b1b5c2ec6d4a2e5f834ab118a0583377ef55。

~~~diff
--- staged/NativeDeNpcExecutor
+++ formal/NativeDeNpcExecutor
@@ -9,7 +9,7 @@
 nestMembers=
 permittedSubclasses=
 annotations=|||
-innerClass=local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408
+innerClass=
 recordComponents=
 method=<init>()V
 access=0x1
~~~

</details>

<details>
<summary>NativeDeNpcHateList — DIFF</summary>

待繼續移植／主控裁定：以下包含全部 metadata、欄位及方法差異；尚未證明不同方法等價，也未證明全部無法用 Java 表達。

左側正規化 SHA-256：a398a012a3319bcaa4da135f1c4ab6aa09d0034779991c60ae40c1b61cbce768。  
右側正規化 SHA-256：73d0f44f56372ebfddc7ffd7d8edcc926b3c4c37fae8f2f330f9634312b79d74。

~~~diff
--- staged/NativeDeNpcHateList
+++ formal/NativeDeNpcHateList
@@ -11,13 +11,6 @@
 annotations=|||
 innerClass=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
 innerClass=java/util/Map$Entry|java/util/Map|Entry|0x609
-innerClass=local/lito381/server/NativeDeDeletionPorts$Hate|local/lito381/server/NativeDeDeletionPorts|Hate|0x608
-innerClass=local/lito381/server/NativeDeDeletionPorts$Monster|local/lito381/server/NativeDeDeletionPorts|Monster|0x408
-innerClass=local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608
-innerClass=local/lito381/server/NativeDeDeletionPorts$Summon|local/lito381/server/NativeDeDeletionPorts|Summon|0x408
-innerClass=local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408
-innerClass=local/lito381/server/NativeDeNpcConstructionPorts$Party|local/lito381/server/NativeDeNpcConstructionPorts|Party|0x408
-innerClass=local/lito381/server/NativeDeWorldKinds$Pet|local/lito381/server/NativeDeWorldKinds|Pet|0x8
 recordComponents=
 field=_hateMap:Ljava/util/Map;
 access=0x12
@@ -479,148 +472,149 @@
 LABEL L0
 ALOAD 2
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L12
+IFEQ L13
 ALOAD 2
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/NativeDeCharacterFields
 ASTORE 3
 ALOAD 3
-IFNULL L10
+IFNULL L1
 ALOAD 3
 INVOKEVIRTUAL local/lito381/server/NativeDeCharacterFields.isDead()Z
-IFNE L10
+IFEQ L2
+LABEL L1
+ALOAD 0
+GETFIELD local/lito381/server/NativeDeNpcHateList._hateMap:Ljava/util/Map;
+ALOAD 3
+INVOKEINTERFACE java/util/Map.remove(Ljava/lang/Object;)Ljava/lang/Object; itf
+POP
+GOTO L0
+LABEL L2
 ALOAD 1
 INSTANCEOF local/lito381/server/NativeDeWorldKinds$Pet
-IFNE L1
+IFNE L3
 ALOAD 1
 INSTANCEOF local/lito381/server/NativeDeDeletionPorts$Summon
-IFEQ L3
-LABEL L1
+IFEQ L5
+LABEL L3
 ALOAD 3
 INSTANCEOF local/lito381/server/NativeDeDeletionPorts$Monster
-IFEQ L3
+IFEQ L5
 ALOAD 3
 CHECKCAST local/lito381/server/NativeDeDeletionPorts$Monster
 ASTORE 4
 ALOAD 4
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionPorts$Monster.destroyed()Z
-IFNE L2
-ALOAD 4
-INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.mapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.mapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S
-IF_ICMPNE L2
-ALOAD 4
-INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
-IF_ICMPNE L2
+IFNE L4
+ALOAD 4
+INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.mapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S
+ALOAD 1
+INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.mapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S
+IF_ICMPNE L4
+ALOAD 4
+INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
+ALOAD 1
+INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
+IF_ICMPNE L4
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.location(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Llocal/lito381/server/NativeDeWorldLocation;
 ALOAD 4
 INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.location(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Llocal/lito381/server/NativeDeWorldLocation;
 INVOKEVIRTUAL local/lito381/server/NativeDeWorldLocation.getTileDistance(Llocal/lito381/server/NativeDeWorldPoint;)I
 CONST Integer:30
-IF_ICMPLE L11
-LABEL L2
+IF_ICMPLE L0
+LABEL L4
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcHateList._hateMap:Ljava/util/Map;
 ALOAD 3
 INVOKEINTERFACE java/util/Map.remove(Ljava/lang/Object;)Ljava/lang/Object; itf
 POP
-GOTO L11
-LABEL L3
+GOTO L0
+LABEL L5
 ALOAD 3
 INSTANCEOF local/lito381/server/NativeDeDeletionPorts$Viewer
-IFEQ L5
+IFEQ L7
 ALOAD 3
 CHECKCAST local/lito381/server/NativeDeDeletionPorts$Viewer
-ASTORE 5
-ALOAD 5
-INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.mapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.mapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S
-IF_ICMPNE L4
-ALOAD 5
-INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
-IF_ICMPEQ L11
-LABEL L4
+ASTORE 4
+ALOAD 4
+INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.mapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S
+ALOAD 1
+INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.mapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S
+IF_ICMPNE L6
+ALOAD 4
+INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
+ALOAD 1
+INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
+IF_ICMPEQ L0
+LABEL L6
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcHateList._hateMap:Ljava/util/Map;
 ALOAD 3
 INVOKEINTERFACE java/util/Map.remove(Ljava/lang/Object;)Ljava/lang/Object; itf
 POP
-GOTO L11
-LABEL L5
+GOTO L0
+LABEL L7
 ALOAD 3
 INSTANCEOF local/lito381/server/NativeDeWorldKinds$Pet
-IFEQ L7
+IFEQ L9
 ALOAD 3
 CHECKCAST local/lito381/server/NativeDeWorldKinds$Pet
-ASTORE 6
-ALOAD 6
-INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.mapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.mapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S
-IF_ICMPNE L6
-ALOAD 6
-INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
-IF_ICMPEQ L11
-LABEL L6
+ASTORE 4
+ALOAD 4
+INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.mapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S
+ALOAD 1
+INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.mapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S
+IF_ICMPNE L8
+ALOAD 4
+INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
+ALOAD 1
+INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
+IF_ICMPEQ L0
+LABEL L8
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcHateList._hateMap:Ljava/util/Map;
 ALOAD 3
 INVOKEINTERFACE java/util/Map.remove(Ljava/lang/Object;)Ljava/lang/Object; itf
 POP
-GOTO L11
-LABEL L7
+GOTO L0
+LABEL L9
 ALOAD 3
 INSTANCEOF local/lito381/server/NativeDeDeletionPorts$Summon
-IFEQ L9
+IFEQ L11
 ALOAD 3
 CHECKCAST local/lito381/server/NativeDeDeletionPorts$Summon
-ASTORE 7
-ALOAD 7
-INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.mapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.mapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S
-IF_ICMPNE L8
-ALOAD 7
-INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
-IF_ICMPEQ L11
-LABEL L8
+ASTORE 4
+ALOAD 4
+INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.mapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S
+ALOAD 1
+INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.mapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S
+IF_ICMPNE L10
+ALOAD 4
+INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
+ALOAD 1
+INVOKESTATIC local/lito381/server/NativeDeNpcConstructionGeometry.showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
+IF_ICMPEQ L0
+LABEL L10
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcHateList._hateMap:Ljava/util/Map;
 ALOAD 3
 INVOKEINTERFACE java/util/Map.remove(Ljava/lang/Object;)Ljava/lang/Object; itf
 POP
-GOTO L11
-LABEL L9
+GOTO L0
+LABEL L11
 ALOAD 1
 ALOAD 3
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionState.knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
-IFNE L11
+IFNE L12
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcHateList._hateMap:Ljava/util/Map;
 ALOAD 3
 INVOKEINTERFACE java/util/Map.remove(Ljava/lang/Object;)Ljava/lang/Object; itf
 POP
-GOTO L11
-LABEL L10
-ALOAD 0
-GETFIELD local/lito381/server/NativeDeNpcHateList._hateMap:Ljava/util/Map;
-ALOAD 3
-INVOKEINTERFACE java/util/Map.remove(Ljava/lang/Object;)Ljava/lang/Object; itf
-POP
-LABEL L11
-GOTO L0
 LABEL L12
+GOTO L0
+LABEL L13
 RETURN
 method=toHateArrayList()Ljava/util/ArrayList;
 access=0x1
~~~

</details>

<details>
<summary>NativeDeNpcInventory — DIFF</summary>

待繼續移植／主控裁定：以下包含全部 metadata、欄位及方法差異；尚未證明不同方法等價，也未證明全部無法用 Java 表達。

左側正規化 SHA-256：83a739dd03c9d9675e4c32e49c38e2b5f6ca1a8f86a64d3e52aeb383ae7d8112。  
右側正規化 SHA-256：3f3d928d658aab3dfe1a4e1f8d056fa9994f1c8cef8d4b1f0ffe00f93ea77f8d。

~~~diff
--- staged/NativeDeNpcInventory
+++ formal/NativeDeNpcInventory
@@ -12,7 +12,6 @@
 innerClass=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
 innerClass=local/lito381/server/NativeDeDeletionPorts$Inventory|local/lito381/server/NativeDeDeletionPorts|Inventory|0x608
 innerClass=local/lito381/server/NativeDeNpcConstructionPorts$Services|local/lito381/server/NativeDeNpcConstructionPorts|Services|0x608
-innerClass=local/lito381/server/NativeDeNpcInventoryScope$World|local/lito381/server/NativeDeNpcInventoryScope|World|0x18
 innerClass=local/lito381/server/NativeDeWorldKinds$Item|local/lito381/server/NativeDeWorldKinds|Item|0x8
 recordComponents=
 field=_cachedWeight:I
@@ -225,9 +224,9 @@
 IFEQ L5
 ALOAD 1
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
+ASTORE 2
+ALOAD 2
 CHECKCAST local/lito381/server/NativeDeWorldKinds$Item
-DUP
-ASTORE 2
 ASTORE 3
 INVOKESTATIC local/lito381/server/NativeDeNpcInventoryScope$World.get()Llocal/lito381/server/NativeDeNpcInventoryScope$World;
 ALOAD 3
~~~

</details>

<details>
<summary>NativeDeNpcKnowledge — DIFF</summary>

待繼續移植／主控裁定：以下包含全部 metadata、欄位及方法差異；尚未證明不同方法等價，也未證明全部無法用 Java 表達。

左側正規化 SHA-256：6361e2ce8693985db5fdb613c0d8f8c474b66ddae2c58636115fc4a85231a0ff。  
右側正規化 SHA-256：3825264ba8884471ab489fc0ca9e271350703a249ffdd2a26f5d799848ae7932。

~~~diff
--- staged/NativeDeNpcKnowledge
+++ formal/NativeDeNpcKnowledge
@@ -136,7 +136,7 @@
 LABEL L1
 ALOAD 0
 INSTANCEOF local/lito381/server/NativeDeDeletionState
-IFEQ L3
+IFEQ L2
 ALOAD 0
 CHECKCAST local/lito381/server/NativeDeDeletionState
 ASTORE 4
@@ -144,53 +144,52 @@
 GETFIELD local/lito381/server/NativeDeDeletionState.deletionOwner:Llocal/lito381/server/NativeDeDeletionOwner;
 ALOAD 1
 GETFIELD local/lito381/server/NativeDeWorldContext.deletion:Llocal/lito381/server/NativeDeDeletionOwner;
-IF_ACMPNE L2
+IF_ACMPNE L6
 ALOAD 3
 ALOAD 4
 INVOKEINTERFACE java/util/Set.add(Ljava/lang/Object;)Z itf
 POP
+GOTO L6
 LABEL L2
-GOTO L7
-LABEL L3
 ALOAD 0
 INSTANCEOF java/util/Map
-IFEQ L6
+IFEQ L5
 ALOAD 0
 CHECKCAST java/util/Map
-ASTORE 4
-ALOAD 4
+ASTORE 5
+ALOAD 5
 INVOKEINTERFACE java/util/Map.values()Ljava/util/Collection; itf
 INVOKEINTERFACE java/util/Collection.iterator()Ljava/util/Iterator; itf
-ASTORE 5
+ASTORE 6
+LABEL L3
+ALOAD 6
+INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
+IFEQ L4
+ALOAD 6
+INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
+ASTORE 7
+ALOAD 7
+ALOAD 1
+ALOAD 2
+ALOAD 3
+INVOKESTATIC local/lito381/server/NativeDeNpcKnowledge.collect(Ljava/lang/Object;Llocal/lito381/server/NativeDeWorldContext;Ljava/util/Set;Ljava/util/Set;)V
+GOTO L3
 LABEL L4
-ALOAD 5
-INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L5
-ALOAD 5
-INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
-ASTORE 6
-ALOAD 6
-ALOAD 1
-ALOAD 2
-ALOAD 3
-INVOKESTATIC local/lito381/server/NativeDeNpcKnowledge.collect(Ljava/lang/Object;Llocal/lito381/server/NativeDeWorldContext;Ljava/util/Set;Ljava/util/Set;)V
-GOTO L4
+GOTO L6
 LABEL L5
-GOTO L7
-LABEL L6
 ALOAD 0
 INVOKEVIRTUAL java/lang/Object.getClass()Ljava/lang/Class;
 INVOKEVIRTUAL java/lang/Class.getEnclosingClass()Ljava/lang/Class;
 ALOAD 1
 GETFIELD local/lito381/server/NativeDeWorldContext.core:Ljava/lang/Object;
 INVOKEVIRTUAL java/lang/Object.getClass()Ljava/lang/Class;
-IF_ACMPNE L7
+IF_ACMPNE L6
 ALOAD 0
 INVOKEVIRTUAL java/lang/Object.getClass()Ljava/lang/Class;
 INVOKEVIRTUAL java/lang/Class.getSimpleName()Ljava/lang/String;
 CONST str:"VisibleBinding"
 INVOKEVIRTUAL java/lang/String.equals(Ljava/lang/Object;)Z
-IFEQ L7
+IFEQ L6
 ALOAD 0
 CONST str:"object"
 CONST str:"()Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;"
@@ -201,7 +200,7 @@
 ALOAD 2
 ALOAD 3
 INVOKESTATIC local/lito381/server/NativeDeNpcKnowledge.collect(Ljava/lang/Object;Llocal/lito381/server/NativeDeWorldContext;Ljava/util/Set;Ljava/util/Set;)V
-LABEL L7
+LABEL L6
 RETURN
 method=retireWorld(Llocal/lito381/server/NativeDeWorldContext;)V
 access=0x8
~~~

</details>

<details>
<summary>NativeDeNpcMobSkillUse — DIFF</summary>

待繼續移植／主控裁定：以下包含全部 metadata、欄位及方法差異；尚未證明不同方法等價，也未證明全部無法用 Java 表達。

左側正規化 SHA-256：34f6b827b987a0749b9a178350db17f3a5d58c74ced9a1090fb0f5d92ca55dd7。  
右側正規化 SHA-256：cbe60b7f3fe29bdc0bfdd151be92043ea51879455d97a6785df73c63cb03de2b。

~~~diff
--- staged/NativeDeNpcMobSkillUse
+++ formal/NativeDeNpcMobSkillUse
@@ -9,7 +9,7 @@
 nestMembers=
 permittedSubclasses=
 annotations=|||
-innerClass=local/lito381/server/NativeDeNpcTemplatePorts$MobSkills|local/lito381/server/NativeDeNpcTemplatePorts|MobSkills|0x18
+innerClass=
 recordComponents=
 field=_attacker:Llocal/lito381/server/NativeDeNpcTemplateState;
 access=0x2
@@ -322,14 +322,10 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault=
-ILOAD 1
-ISTORE 2
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcMobSkillUse._skillUseCount:[I
-ILOAD 2
-ALOAD 0
-GETFIELD local/lito381/server/NativeDeNpcMobSkillUse._skillUseCount:[I
-ILOAD 2
+ILOAD 1
+DUP2
 IALOAD
 CONST Integer:1
 IADD
~~~

</details>

<details>
<summary>NativeDeNpcMoveState — DIFF</summary>

已驗證目前差異只出現在 class metadata；欄位及全部方法的本工具正規化輸出一致。metadata 是否可豁免仍待主控核准。

左側正規化 SHA-256：258946a995928f840d059593f3da795512df0284ea26c687a02ec595f0636d46。  
右側正規化 SHA-256：c40d451da427be97e4d02bf6f24bd09877c6e5efbd20b147d6a435d5a61ffea0。

~~~diff
--- staged/NativeDeNpcMoveState
+++ formal/NativeDeNpcMoveState
@@ -9,8 +9,6 @@
 nestMembers=local/lito381/server/NativeDeNpcMoveState$PathFailure,local/lito381/server/NativeDeNpcMoveState$PathState
 permittedSubclasses=
 annotations=|||
-innerClass=local/lito381/server/NativeDeNpcConstructionPorts$Movement|local/lito381/server/NativeDeNpcConstructionPorts|Movement|0x608
-innerClass=local/lito381/server/NativeDeNpcConstructionPorts$Services|local/lito381/server/NativeDeNpcConstructionPorts|Services|0x608
 innerClass=local/lito381/server/NativeDeNpcMoveState$PathFailure|local/lito381/server/NativeDeNpcMoveState|PathFailure|0x4019
 innerClass=local/lito381/server/NativeDeNpcMoveState$PathState|local/lito381/server/NativeDeNpcMoveState|PathState|0x1a
 recordComponents=
~~~

</details>

<details>
<summary>NativeDeNpcTemplate — DIFF</summary>

已驗證目前差異只出現在 class metadata；欄位及全部方法的本工具正規化輸出一致。metadata 是否可豁免仍待主控核准。

左側正規化 SHA-256：868c23114bf492dc24ab781267a33e3f08d9a2aa360fe57fbe87f67796041ce5。  
右側正規化 SHA-256：5c6cc93d231d6c70d7e1f2eeaf10b6c92abcd69faf30a56572e21a14047ed765。

~~~diff
--- staged/NativeDeNpcTemplate
+++ formal/NativeDeNpcTemplate
@@ -10,7 +10,6 @@
 permittedSubclasses=
 annotations=|||
 innerClass=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
-innerClass=local/lito381/server/NativeDeDeletionPorts$Template|local/lito381/server/NativeDeDeletionPorts|Template|0x608
 recordComponents=
 field=_ac:I
 access=0x2
~~~

</details>

<details>
<summary>NativeDeNpcTemplateMobLoader — DIFF</summary>

待繼續移植／主控裁定：以下包含全部 metadata、欄位及方法差異；尚未證明不同方法等價，也未證明全部無法用 Java 表達。

左側正規化 SHA-256：6eb84996dbbfbe86084efde36832a2c71b9b0fb44e11723a15da56fb6c774cd8。  
右側正規化 SHA-256：c5bb2454330d747691bde74c032ff06601260c268b8d4b8edbb2cd7ebe91b515。

~~~diff
--- staged/NativeDeNpcTemplateMobLoader
+++ formal/NativeDeNpcTemplateMobLoader
@@ -9,12 +9,7 @@
 nestMembers=
 permittedSubclasses=
 annotations=|||
-innerClass=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
-innerClass=local/lito381/server/NativeDeNpcTemplateLoadScope$Database|local/lito381/server/NativeDeNpcTemplateLoadScope|Database|0x18
-innerClass=local/lito381/server/NativeDeNpcTemplateLoadScope$Npcs|local/lito381/server/NativeDeNpcTemplateLoadScope|Npcs|0x18
-innerClass=local/lito381/server/NativeDeNpcTemplateLoadScope$Skills|local/lito381/server/NativeDeNpcTemplateLoadScope|Skills|0x18
-innerClass=local/lito381/server/NativeDeNpcTemplateLoadScope$Skill|local/lito381/server/NativeDeNpcTemplateLoadScope|Skill|0x18
-innerClass=local/lito381/server/NativeDeNpcTemplateLoadScope$Timer|local/lito381/server/NativeDeNpcTemplateLoadScope|Timer|0x18
+innerClass=
 recordComponents=
 method=<init>()V
 access=0x0
@@ -34,10 +29,10 @@
 INVOKESTATIC local/lito381/server/NativeDeNpcTemplateLoadScope$Npcs.get()Llocal/lito381/server/NativeDeNpcTemplateLoadScope$Npcs;
 ILOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplateLoadScope$Npcs.getTemplate(I)Llocal/lito381/server/NativeDeNpcTemplate;
-ASTORE 4
-ALOAD 4
+ASTORE 3
+ALOAD 3
 IFNULL L0
-ALOAD 4
+ALOAD 3
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_name()Ljava/lang/String;
 INVOKESTATIC local/lito381/server/NativeDeNpcTemplateMobLoader.isBlank(Ljava/lang/String;)Z
 IFEQ L1
@@ -46,50 +41,46 @@
 ARETURN
 LABEL L1
 CONST null
-ASTORE 5
+ASTORE 4
 ILOAD 1
 IFLE L2
 INVOKESTATIC local/lito381/server/NativeDeNpcTemplateLoadScope$Skills.get()Llocal/lito381/server/NativeDeNpcTemplateLoadScope$Skills;
 ILOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplateLoadScope$Skills.getTemplate(I)Llocal/lito381/server/NativeDeNpcTemplateLoadScope$Skill;
-DUP
-ASTORE 3
+ASTORE 5
+ALOAD 5
 IFNULL L2
-ALOAD 3
-CHECKCAST local/lito381/server/NativeDeNpcTemplateLoadScope$Skill
+ALOAD 5
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplateLoadScope$Skill.getName()Ljava/lang/String;
-ASTORE 5
+ASTORE 4
 LABEL L2
-ALOAD 5
+ALOAD 4
 INVOKESTATIC local/lito381/server/NativeDeNpcTemplateMobLoader.isBlank(Ljava/lang/String;)Z
 IFEQ L3
 ILOAD 2
 ILOAD 1
 INVOKESTATIC local/lito381/server/NativeDeNpcTemplateMobLoader.fallbackSkillName(II)Ljava/lang/String;
-ASTORE 5
+ASTORE 4
 LABEL L3
-ALOAD 5
+ALOAD 4
 INVOKESTATIC local/lito381/server/NativeDeNpcTemplateMobLoader.isBlank(Ljava/lang/String;)Z
 IFEQ L4
 CONST null
 ARETURN
 LABEL L4
-ALOAD 4
+ALOAD 3
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_name()Ljava/lang/String;
-ALOAD 5
+ALOAD 4
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"\u0001>\u0001"]
-ASTORE 3
-ALOAD 3
-CHECKCAST java/lang/String
+ASTORE 5
+ALOAD 5
 INVOKEVIRTUAL java/lang/String.length()I
 CONST Integer:45
 IF_ICMPGT L5
-ALOAD 3
-CHECKCAST java/lang/String
+ALOAD 5
 GOTO L6
 LABEL L5
-ALOAD 3
-CHECKCAST java/lang/String
+ALOAD 5
 CONST Integer:0
 CONST Integer:45
 INVOKEVIRTUAL java/lang/String.substring(II)Ljava/lang/String;
~~~

</details>

<details>
<summary>NativeDeNpcTemplateNpcLoader — DIFF</summary>

待繼續移植／主控裁定：以下包含全部 metadata、欄位及方法差異；尚未證明不同方法等價，也未證明全部無法用 Java 表達。

左側正規化 SHA-256：b29cc7cad1c3535765216e99cf2e689bfa91b561fc87fea65f7c05c39cdb4a7b。  
右側正規化 SHA-256：87543177f5c471432384e867d2b58de16dde3d82a5e8e116506aa4c5dc99d6f0。

~~~diff
--- staged/NativeDeNpcTemplateNpcLoader
+++ formal/NativeDeNpcTemplateNpcLoader
@@ -9,9 +9,7 @@
 nestMembers=
 permittedSubclasses=
 annotations=|||
-innerClass=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
 innerClass=local/lito381/server/NativeDeNpcTemplateLoadScope$Context|local/lito381/server/NativeDeNpcTemplateLoadScope|Context|0x18
-innerClass=local/lito381/server/NativeDeNpcTemplateLoadScope$Database|local/lito381/server/NativeDeNpcTemplateLoadScope|Database|0x18
 recordComponents=
 method=<init>()V
 access=0x0
@@ -204,7 +202,7 @@
 ALOAD 5
 ILOAD 6
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
-INVOKEVIRTUAL java/util/HashMap.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
+INVOKEINTERFACE java/util/Map.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; itf
 POP
 LABEL L7
 GOTO L9
@@ -216,6 +214,7 @@
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;I)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"載入資料失敗，已略過該筆 (family: \u0001, id: \u0001)"]
 ALOAD 6
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
+GOTO L4
 LABEL L9
 GOTO L4
 LABEL L10
@@ -716,6 +715,7 @@
 INVOKEDYNAMIC makeConcatWithConstants(I)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"載入資料失敗，已略過該筆 (npcid: \u0001)"]
 ALOAD 5
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
+GOTO L3
 LABEL L11
 GOTO L3
 LABEL L12
~~~

</details>

<details>
<summary>NativeDeNpcTemplateState — DIFF</summary>

待繼續移植／主控裁定：以下包含全部 metadata、欄位及方法差異；尚未證明不同方法等價，也未證明全部無法用 Java 表達。

左側正規化 SHA-256：58d23bb66c2e9845b137238589dd0395387168dd8c1d52ac7ca5d42e13c255dc。  
右側正規化 SHA-256：5b3673444389d3279c23b622db06d74a382fc8ab85990898a3a848d86d0f9f6d。

~~~diff
--- staged/NativeDeNpcTemplateState
+++ formal/NativeDeNpcTemplateState
@@ -12,7 +12,6 @@
 innerClass=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
 innerClass=local/lito381/server/NativeDeDeletionPorts$Template|local/lito381/server/NativeDeDeletionPorts|Template|0x608
 innerClass=local/lito381/server/NativeDeNpcTemplatePorts$Services|local/lito381/server/NativeDeNpcTemplatePorts|Services|0x608
-innerClass=local/lito381/server/NativeDeNpcTemplatePorts$WorkTimer|local/lito381/server/NativeDeNpcTemplatePorts|WorkTimer|0x18
 innerClass=local/lito381/server/NativeDePerceptionPorts$Services|local/lito381/server/NativeDePerceptionPorts|Services|0x608
 recordComponents=
 field=ACTION:Llocal/lito381/server/NativeDeNpcExecutor;
@@ -165,7 +164,7 @@
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault=
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeNpcTemplateState.nameId:Ljava/lang/String;
+GETFIELD local/lito381/server/NativeDeDeletionState.nameId:Ljava/lang/String;
 ARETURN
 method=getNpcTemplate()Llocal/lito381/server/NativeDeDeletionPorts$Template;
 access=0x41
@@ -183,7 +182,7 @@
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault=
 ALOAD 0
-GETFIELD local/lito381/server/NativeDeNpcTemplateState.template:Llocal/lito381/server/NativeDeDeletionPorts$Template;
+GETFIELD local/lito381/server/NativeDeDeletionState.template:Llocal/lito381/server/NativeDeDeletionPorts$Template;
 CHECKCAST local/lito381/server/NativeDeNpcTemplate
 ARETURN
 method=getPassispeed()I
@@ -462,7 +461,7 @@
 annotationDefault=
 ALOAD 0
 ALOAD 1
-PUTFIELD local/lito381/server/NativeDeNpcTemplateState.nameId:Ljava/lang/String;
+PUTFIELD local/lito381/server/NativeDeDeletionState.nameId:Ljava/lang/String;
 RETURN
 method=setPassispeed(I)V
 access=0x1
@@ -514,13 +513,13 @@
 annotationDefault=
 ALOAD 0
 ALOAD 1
-PUTFIELD local/lito381/server/NativeDeNpcTemplateState.template:Llocal/lito381/server/NativeDeDeletionPorts$Template;
+PUTFIELD local/lito381/server/NativeDeDeletionState.template:Llocal/lito381/server/NativeDeDeletionPorts$Template;
 CONST Integer:0
-ISTORE 5
+ISTORE 2
 CONST Double:0x0
-DSTORE 6
+DSTORE 3
 CONST Double:0x0
-DSTORE 8
+DSTORE 5
 ALOAD 0
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_name()Ljava/lang/String;
@@ -532,9 +531,9 @@
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_nameid()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"\u0001\u0001"]
-ASTORE 10
-ALOAD 0
-ALOAD 10
+ASTORE 7
+ALOAD 0
+ALOAD 7
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplateState.setNameId(Ljava/lang/String;)V
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_randomlevel()I
@@ -554,26 +553,26 @@
 CONST Integer:1
 IADD
 INVOKEVIRTUAL java/util/Random.nextInt(I)I
-ISTORE 5
+ISTORE 2
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_randomlevel()I
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_level()I
 ISUB
 I2D
-DSTORE 8
-ILOAD 5
-I2D
-DLOAD 8
+DSTORE 5
+ILOAD 2
+I2D
+DLOAD 5
 DDIV
-DSTORE 6
-ALOAD 0
-ILOAD 5
+DSTORE 3
+ILOAD 2
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_level()I
 IADD
-DUP
-ISTORE 5
+ISTORE 2
+ALOAD 0
+ILOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplateState.setLevel(I)V
 LABEL L1
 ALOAD 1
@@ -589,7 +588,7 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplateState.setCurrentHpDirect(I)V
 GOTO L3
 LABEL L2
-DLOAD 6
+DLOAD 3
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_randomhp()I
 ALOAD 1
@@ -597,19 +596,19 @@
 ISUB
 I2D
 DMUL
-DSTORE 3
+DSTORE 8
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_hp()I
 I2D
-DLOAD 3
+DLOAD 8
 DADD
 D2I
-ISTORE 2
-ALOAD 0
-ILOAD 2
+ISTORE 10
+ALOAD 0
+ILOAD 10
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplateState.setMaxHp(I)V
 ALOAD 0
-ILOAD 2
+ILOAD 10
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplateState.setCurrentHpDirect(I)V
 LABEL L3
 ALOAD 1
@@ -625,7 +624,7 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplateState.setCurrentMpDirect(I)V
 GOTO L5
 LABEL L4
-DLOAD 6
+DLOAD 3
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_randommp()I
 ALOAD 1
@@ -633,19 +632,19 @@
 ISUB
 I2D
 DMUL
-DSTORE 3
+DSTORE 8
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_mp()I
 I2D
-DLOAD 3
+DLOAD 8
 DADD
 D2I
-ISTORE 2
-ALOAD 0
-ILOAD 2
+ISTORE 10
+ALOAD 0
+ILOAD 10
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplateState.setMaxMp(I)V
 ALOAD 0
-ILOAD 2
+ILOAD 10
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplateState.setCurrentMpDirect(I)V
 LABEL L5
 ALOAD 1
@@ -657,7 +656,7 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplateState.setAc(I)V
 GOTO L7
 LABEL L6
-DLOAD 6
+DLOAD 3
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_randomac()I
 ALOAD 1
@@ -665,16 +664,16 @@
 ISUB
 I2D
 DMUL
-DSTORE 3
+DSTORE 8
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_ac()I
 I2D
-DLOAD 3
+DLOAD 8
 DADD
 D2I
-ISTORE 2
-ALOAD 0
-ILOAD 2
+ISTORE 10
+ALOAD 0
+ILOAD 10
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplateState.setAc(I)V
 LABEL L7
 ALOAD 1
@@ -710,7 +709,7 @@
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_str()I
 I2D
-DLOAD 8
+DLOAD 5
 DADD
 CONST Double:0x405fc00000000000
 INVOKESTATIC java/lang/Math.min(DD)D
@@ -721,7 +720,7 @@
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_con()I
 I2D
-DLOAD 8
+DLOAD 5
 DADD
 CONST Double:0x405fc00000000000
 INVOKESTATIC java/lang/Math.min(DD)D
@@ -732,7 +731,7 @@
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_dex()I
 I2D
-DLOAD 8
+DLOAD 5
 DADD
 CONST Double:0x405fc00000000000
 INVOKESTATIC java/lang/Math.min(DD)D
@@ -743,7 +742,7 @@
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_int()I
 I2D
-DLOAD 8
+DLOAD 5
 DADD
 CONST Double:0x405fc00000000000
 INVOKESTATIC java/lang/Math.min(DD)D
@@ -754,7 +753,7 @@
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_wis()I
 I2D
-DLOAD 8
+DLOAD 5
 DADD
 CONST Double:0x405fc00000000000
 INVOKESTATIC java/lang/Math.min(DD)D
@@ -765,7 +764,7 @@
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_mr()I
 I2D
-DLOAD 8
+DLOAD 5
 DADD
 CONST Double:0x405fc00000000000
 INVOKESTATIC java/lang/Math.min(DD)D
@@ -773,13 +772,13 @@
 I2B
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplateState.setMr(I)V
 ALOAD 0
-DLOAD 8
+DLOAD 5
 D2I
 CONST Integer:2
 IMUL
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplateState.addHitup(I)V
 ALOAD 0
-DLOAD 8
+DLOAD 5
 D2I
 CONST Integer:2
 IMUL
@@ -857,15 +856,15 @@
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_gfxid()I
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplateState.newGfx(I)I
-ISTORE 11
-ALOAD 0
-ILOAD 11
+ISTORE 8
+ALOAD 0
+ILOAD 8
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplateState.setTempCharGfx(I)V
 ALOAD 0
-ILOAD 11
+ILOAD 8
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplateState.setGfxId(I)V
 ALOAD 0
-ILOAD 11
+ILOAD 8
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplateState.setGfxidInStatus(I)V
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_randomexp()I
@@ -884,9 +883,9 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_randomexp()I
 INVOKEVIRTUAL java/util/Random.nextInt(I)I
 IADD
-ISTORE 12
-ALOAD 0
-ILOAD 12
+ISTORE 9
+ALOAD 0
+ILOAD 9
 I2L
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplateState.setExp(J)V
 LABEL L20
@@ -903,7 +902,7 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplateState.setTempLawful(I)V
 GOTO L22
 LABEL L21
-DLOAD 6
+DLOAD 3
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_randomlawful()I
 ALOAD 1
@@ -911,19 +910,19 @@
 ISUB
 I2D
 DMUL
-DSTORE 12
+DSTORE 9
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_lawful()I
 I2D
-DLOAD 12
+DLOAD 9
 DADD
 D2I
-ISTORE 14
-ALOAD 0
-ILOAD 14
+ISTORE 11
+ALOAD 0
+ILOAD 11
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplateState.setLawful(I)V
 ALOAD 0
-ILOAD 14
+ILOAD 11
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplateState.setTempLawful(I)V
 LABEL L22
 ALOAD 0
~~~

</details>

<details>
<summary>NativeDeNpcVitalState — DIFF</summary>

待主控裁定：getX/getY 的正式 protected 覆寫 public 父方法，javac 拒絕；仍有其他方法差異，未宣告等價。

左側正規化 SHA-256：98623ac90cbb94059820ced5238224f9637297f0f84aee0a1c2994a03fc2f834。  
右側正規化 SHA-256：519d5a73d4d3c4215ee4161e7255230ed5c6115a91b49acbe9c352d40ab3101b。

~~~diff
--- staged/NativeDeNpcVitalState
+++ formal/NativeDeNpcVitalState
@@ -329,7 +329,7 @@
 GETFIELD local/lito381/server/NativeDeNpcVitalState._weaponBreakStack:I
 IRETURN
 method=getX()I
-access=0x1
+access=0x4
 signature=
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
@@ -340,7 +340,7 @@
 INVOKEINTERFACE local/lito381/server/NativeDeResurrectionEffects.x(Llocal/lito381/server/NativeDeNpcVitalState;)I itf
 IRETURN
 method=getY()I
-access=0x1
+access=0x4
 signature=
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
@@ -433,12 +433,12 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault=
+TRY L3 L4 L9 java/lang/RuntimeException  
 TRY L3 L4 L9 java/lang/Error  
-TRY L3 L4 L9 java/lang/RuntimeException  
+TRY L5 L6 L9 java/lang/RuntimeException  
 TRY L5 L6 L9 java/lang/Error  
-TRY L5 L6 L9 java/lang/RuntimeException  
+TRY L7 L8 L9 java/lang/RuntimeException  
 TRY L7 L8 L9 java/lang/Error  
-TRY L7 L8 L9 java/lang/RuntimeException  
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcVitalState._asyncDeathLifecycle:Llocal/lito381/server/NativeDeathLifecycleGate;
 INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate.isManaged()Z
@@ -558,92 +558,23 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate$State.phase()Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
 INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate$Phase.ordinal()I
 IALOAD
-TABLESWITCH min=1 max=5 default=L7 labels=L4,L4,L5,L5,L5
+TABLESWITCH min=1 max=7 default=L6 labels=L4,L4,L5,L5,L5,L6,L6
 LABEL L4
 ALOAD 0
 CONST Integer:0
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcVitalState.setCharacterDead(Z)V
-GOTO L7
+GOTO L6
 LABEL L5
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcVitalState._asyncDeathLifecycle:Llocal/lito381/server/NativeDeathLifecycleGate;
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate.reviveDirect(Llocal/lito381/server/NativeDeathLifecycleGate$State;)Z
-IFNE L6
-GOTO L7
+IFEQ L6
+ALOAD 0
+CONST Integer:0
+INVOKEVIRTUAL local/lito381/server/NativeDeNpcVitalState.setCharacterDead(Z)V
+GOTO L6
 LABEL L6
-ALOAD 0
-CONST Integer:0
-INVOKEVIRTUAL local/lito381/server/NativeDeNpcVitalState.setCharacterDead(Z)V
-LABEL L7
-RETURN
-method=lambda$sourceNpcSetDead$#0(Z)V
-access=0x2
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-GETFIELD local/lito381/server/NativeDeNpcVitalState._asyncDeathLifecycle:Llocal/lito381/server/NativeDeathLifecycleGate;
-INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate.isManaged()Z
-IFNE L1
-ALOAD 0
-ILOAD 1
-INVOKEVIRTUAL local/lito381/server/NativeDeNpcVitalState.setCharacterDead(Z)V
-ILOAD 1
-IFEQ L0
-ALOAD 0
-CONST Integer:0
-INVOKEVIRTUAL local/lito381/server/NativeDeNpcVitalState.setWeaponBreakStack(I)V
-LABEL L0
-RETURN
-LABEL L1
-ALOAD 0
-GETFIELD local/lito381/server/NativeDeNpcVitalState._asyncDeathLifecycle:Llocal/lito381/server/NativeDeathLifecycleGate;
-INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate.snapshot()Llocal/lito381/server/NativeDeathLifecycleGate$State;
-ASTORE 2
-ILOAD 1
-IFEQ L3
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate$State.phase()Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
-GETSTATIC local/lito381/server/NativeDeathLifecycleGate$Phase.RESURRECTING:Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
-IF_ACMPEQ L2
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate$State.phase()Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
-GETSTATIC local/lito381/server/NativeDeathLifecycleGate$Phase.TERMINATED:Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
-IF_ACMPEQ L2
-ALOAD 0
-CONST Integer:1
-INVOKEVIRTUAL local/lito381/server/NativeDeNpcVitalState.setCharacterDead(Z)V
-ALOAD 0
-CONST Integer:0
-INVOKEVIRTUAL local/lito381/server/NativeDeNpcVitalState.setWeaponBreakStack(I)V
-LABEL L2
-RETURN
-LABEL L3
-GETSTATIC local/lito381/server/NativeDeNpcVitalState$1.$SwitchMap$local$lito381$server$NativeDeathLifecycleGate$Phase:[I
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate$State.phase()Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
-INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate$Phase.ordinal()I
-IALOAD
-TABLESWITCH min=1 max=5 default=L7 labels=L4,L4,L5,L5,L5
-LABEL L4
-ALOAD 0
-CONST Integer:0
-INVOKEVIRTUAL local/lito381/server/NativeDeNpcVitalState.setCharacterDead(Z)V
-GOTO L7
-LABEL L5
-ALOAD 0
-GETFIELD local/lito381/server/NativeDeNpcVitalState._asyncDeathLifecycle:Llocal/lito381/server/NativeDeathLifecycleGate;
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate.reviveDirect(Llocal/lito381/server/NativeDeathLifecycleGate$State;)Z
-IFNE L6
-GOTO L7
-LABEL L6
-ALOAD 0
-CONST Integer:0
-INVOKEVIRTUAL local/lito381/server/NativeDeNpcVitalState.setCharacterDead(Z)V
-LABEL L7
 RETURN
 method=lifecycleSnapshot()Llocal/lito381/server/NativeDeathLifecycleGate$State;
 access=0x0
@@ -685,7 +616,7 @@
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault=
 ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeNpcVitalState.releaseCorpseDeletionClaimForPairedResurrection()V
+INVOKESPECIAL local/lito381/server/NativeDeNpcVitalState.releaseCorpseDeletionClaimForPairedResurrection()V
 RETURN
 method=resurrect(I)V
 access=0x21
@@ -840,7 +771,7 @@
 ALOAD 0
 ALOAD 0
 ILOAD 1
-INVOKEDYNAMIC run(Llocal/lito381/server/NativeDeNpcVitalState;Z)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H5 local/lito381/server/NativeDeNpcVitalState.lambda$sourceNpcSetDead$#0(Z)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/NativeDeNpcVitalState;Z)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H5 local/lito381/server/NativeDeNpcVitalState.lambda$setDead$#0(Z)V, type:()V]
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcVitalState.withVitalMutation(Ljava/lang/Runnable;)V
 RETURN
 method=startDeleteTimer(I)V
~~~

</details>

<details>
<summary>NativeDeNpcVitalState$1 — DIFF</summary>

待繼續移植／主控裁定：以下包含全部 metadata、欄位及方法差異；尚未證明不同方法等價，也未證明全部無法用 Java 表達。

左側正規化 SHA-256：cc66f562a2201fc7945396f5d7ca225bfdef50aefaa97663530ab0f829602416。  
右側正規化 SHA-256：84ced4616a3d8a13d79d2bbad46181378802883a06fdff3e923c3ee802529983。

~~~diff
--- staged/NativeDeNpcVitalState$1
+++ formal/NativeDeNpcVitalState$1
@@ -28,6 +28,8 @@
 TRY L6 L7 L8 java/lang/NoSuchFieldError  
 TRY L9 L10 L11 java/lang/NoSuchFieldError  
 TRY L12 L13 L14 java/lang/NoSuchFieldError  
+TRY L15 L16 L17 java/lang/NoSuchFieldError  
+TRY L18 L19 L20 java/lang/NoSuchFieldError  
 INVOKESTATIC local/lito381/server/NativeDeathLifecycleGate$Phase.values()[Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
 ARRAYLENGTH
 NEWARRAY 10
@@ -83,4 +85,24 @@
 LABEL L14
 ASTORE 0
 LABEL L15
+GETSTATIC local/lito381/server/NativeDeNpcVitalState$1.$SwitchMap$local$lito381$server$NativeDeathLifecycleGate$Phase:[I
+GETSTATIC local/lito381/server/NativeDeathLifecycleGate$Phase.FINALIZING:Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
+INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate$Phase.ordinal()I
+CONST Integer:6
+IASTORE
+LABEL L16
+GOTO L18
+LABEL L17
+ASTORE 0
+LABEL L18
+GETSTATIC local/lito381/server/NativeDeNpcVitalState$1.$SwitchMap$local$lito381$server$NativeDeathLifecycleGate$Phase:[I
+GETSTATIC local/lito381/server/NativeDeathLifecycleGate$Phase.TERMINATED:Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
+INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate$Phase.ordinal()I
+CONST Integer:7
+IASTORE
+LABEL L19
+GOTO L21
+LABEL L20
+ASTORE 0
+LABEL L21
 RETURN
~~~

</details>

<details>
<summary>NativeDeObjectFields — DIFF</summary>

待繼續移植／主控裁定：以下包含全部 metadata、欄位及方法差異；尚未證明不同方法等價，也未證明全部無法用 Java 表達。

左側正規化 SHA-256：e0c75f9d72cc6a9a24d176a0a7ae83c157898103482fb7c321a78ebce8986e05。  
右側正規化 SHA-256：efffdfb1dbd617aa418d6b4829cb5821f2108f125b5a597ec5411b8bc979e6f7。

~~~diff
--- staged/NativeDeObjectFields
+++ formal/NativeDeObjectFields
@@ -12,8 +12,6 @@
 innerClass=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
 innerClass=local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608
 innerClass=local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408
-innerClass=local/lito381/server/NativeDeObjectScope$Maps|local/lito381/server/NativeDeObjectScope|Maps|0x18
-innerClass=local/lito381/server/NativeDeObjectScope$World|local/lito381/server/NativeDeObjectScope|World|0x18
 recordComponents=
 field=_loc:Llocal/lito381/server/NativeDeWorldLocation;
 access=0x12
@@ -41,7 +39,7 @@
 value=
 annotations=|||
 method=<init>()V
-access=0x0
+access=0x1
 signature=
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
~~~

</details>

<details>
<summary>NativeDePerceptionBadText — DIFF</summary>

待繼續移植／主控裁定：以下包含全部 metadata、欄位及方法差異；尚未證明不同方法等價，也未證明全部無法用 Java 表達。

左側正規化 SHA-256：16a2a8ece9c0c69c04b358894d0d215ae7d4610af7763d438dde890f21bf8871。  
右側正規化 SHA-256：a42ee69a540ca07984dc36a72783e9cef0fd28a9bc24273183cd981ac32975f9。

~~~diff
--- staged/NativeDePerceptionBadText
+++ formal/NativeDePerceptionBadText
@@ -10,18 +10,7 @@
 permittedSubclasses=
 annotations=|||
 innerClass=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
-innerClass=local/lito381/server/NativeDePerceptionResource$File|local/lito381/server/NativeDePerceptionResource|File|0x18
-innerClass=local/lito381/server/NativeDePerceptionResource$Stream|local/lito381/server/NativeDePerceptionResource|Stream|0x18
 recordComponents=
-method=<init>()V
-access=0x1
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL java/lang/Object.<init>()V
-RETURN
 method=load(Ljava/lang/String;Ljava/util/ArrayList;Z)V
 access=0x9
 signature=
@@ -70,59 +59,58 @@
 ASTORE 4
 LABEL L1
 CONST Integer:1
-ISTORE 6
+ISTORE 5
 LABEL L2
 ALOAD 4
 INVOKEVIRTUAL java/io/BufferedReader.readLine()Ljava/lang/String;
 DUP
-ASTORE 5
+ASTORE 6
 IFNULL L7
-ILOAD 6
+ILOAD 5
 IFEQ L3
 CONST Integer:0
-ISTORE 6
+ISTORE 5
 GOTO L2
 LABEL L3
-ALOAD 5
+ALOAD 6
 INVOKEVIRTUAL java/lang/String.trim()Ljava/lang/String;
-DUP
-ASTORE 5
+ASTORE 6
+ALOAD 6
 INVOKEVIRTUAL java/lang/String.isEmpty()Z
 IFNE L2
-ALOAD 5
+ALOAD 6
 CONST str:"#"
 INVOKEVIRTUAL java/lang/String.startsWith(Ljava/lang/String;)Z
 IFEQ L4
 GOTO L2
 LABEL L4
-ALOAD 5
-ASTORE 8
+ALOAD 6
+ASTORE 7
 ILOAD 2
 IFEQ L5
 NEW java/util/StringTokenizer
 DUP
-ALOAD 5
+ALOAD 6
 CONST str:";"
 INVOKESPECIAL java/util/StringTokenizer.<init>(Ljava/lang/String;Ljava/lang/String;)V
-DUP
-ASTORE 7
+ASTORE 8
+ALOAD 8
 INVOKEVIRTUAL java/util/StringTokenizer.hasMoreTokens()Z
 IFEQ L5
-ALOAD 7
+ALOAD 8
 INVOKEVIRTUAL java/util/StringTokenizer.nextToken()Ljava/lang/String;
 INVOKEVIRTUAL java/lang/String.trim()Ljava/lang/String;
-ASTORE 8
+ASTORE 7
 LABEL L5
 ALOAD 1
-ALOAD 8
+ALOAD 7
 INVOKEVIRTUAL java/util/ArrayList.contains(Ljava/lang/Object;)Z
-IFEQ L6
-GOTO L2
-LABEL L6
+IFNE L6
 ALOAD 1
-ALOAD 8
+ALOAD 7
 INVOKEVIRTUAL java/util/ArrayList.add(Ljava/lang/Object;)Z
 POP
+LABEL L6
 GOTO L2
 LABEL L7
 INVOKESTATIC local/lito381/server/NativeDePerceptionControls.log()Llocal/lito381/server/NativeDeWorldLog;
~~~

</details>

<details>
<summary>NativeDePerceptionEmblem — DIFF</summary>

已驗證目前差異只出現在 class metadata；欄位及全部方法的本工具正規化輸出一致。metadata 是否可豁免仍待主控核准。

左側正規化 SHA-256：4634b7810704650652aa0849a907684e91a8e68baae92369c08ee2e6c95f638a。  
右側正規化 SHA-256：8a0ac179e11b6b720fd4868ee983bb0803955e0b2d727b731307806b1cd4c107。

~~~diff
--- staged/NativeDePerceptionEmblem
+++ formal/NativeDePerceptionEmblem
@@ -9,7 +9,7 @@
 nestMembers=
 permittedSubclasses=
 annotations=|||
-innerClass=
+innerClass=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
 recordComponents=
 field=_byte:[B
 access=0x2
~~~

</details>

<details>
<summary>NativeDePerceptionNPCPack_De — DIFF</summary>

已驗證目前差異只出現在 class metadata；欄位及全部方法的本工具正規化輸出一致。metadata 是否可豁免仍待主控核准。

左側正規化 SHA-256：129a936fdabe6e73d092c767f515bd3a2dcbb8c348eb9595f66e416d9b5c2aab。  
右側正規化 SHA-256：81024e70c3822b95da7b3c3188ee07fd17dc77f5f477adedcdc09a464069be84。

~~~diff
--- staged/NativeDePerceptionNPCPack_De
+++ formal/NativeDePerceptionNPCPack_De
@@ -9,7 +9,6 @@
 nestMembers=
 permittedSubclasses=
 annotations=|||
-innerClass=local/lito381/server/NativeDePerceptionPorts$Poison|local/lito381/server/NativeDePerceptionPorts|Poison|0x408
 innerClass=local/lito381/server/NativeDePerceptionSpeed$SpeedState|local/lito381/server/NativeDePerceptionSpeed|SpeedState|0x19
 recordComponents=
 field=STATUS_BRAVE:I
~~~

</details>

<details>
<summary>NativeDePerceptionPacket — DIFF</summary>

待繼續移植／主控裁定：以下包含全部 metadata、欄位及方法差異；尚未證明不同方法等價，也未證明全部無法用 Java 表達。

左側正規化 SHA-256：f8490b8319afb8664dcbe8d95fa48636c24e84594234e6a66f3cb7148a7d41e0。  
右側正規化 SHA-256：20dddc97ec97e526f477dee82c9feff474e9b61c41682516a019b193b0e58486。

~~~diff
--- staged/NativeDePerceptionPacket
+++ formal/NativeDePerceptionPacket
@@ -9,10 +9,8 @@
 nestMembers=
 permittedSubclasses=
 annotations=|||
+innerClass=com/lineage/server/utils/TeleportTrace$PacketTrace|com/lineage/server/utils/TeleportTrace|PacketTrace|0x19
 innerClass=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
-innerClass=local/lito381/server/NativeDeDeletionPorts$Log|local/lito381/server/NativeDeDeletionPorts|Log|0x608
-innerClass=local/lito381/server/NativeDeDeletionPorts$Packet|local/lito381/server/NativeDeDeletionPorts|Packet|0x8
-innerClass=local/lito381/server/NativeDePerceptionPorts$Services|local/lito381/server/NativeDePerceptionPorts|Services|0x608
 recordComponents=
 field=_bao:Ljava/io/ByteArrayOutputStream;
 access=0x14
@@ -48,15 +46,6 @@
 ALOAD 0
 INVOKESPECIAL local/lito381/server/NativeDeDeletionPorts$Packet.<init>()V
 ALOAD 0
-CONST null
-PUTFIELD local/lito381/server/NativeDePerceptionPacket._byteCache:[B
-ALOAD 0
-CONST null
-PUTFIELD local/lito381/server/NativeDePerceptionPacket._preparedPayload:[B
-ALOAD 0
-CONST null
-PUTFIELD local/lito381/server/NativeDePerceptionPacket._constructionFailure:Ljava/lang/IllegalStateException;
-ALOAD 0
 INVOKESTATIC local/lito381/server/NativeDePerceptionScope.actor()Llocal/lito381/server/NativeDePerceptionState;
 PUTFIELD local/lito381/server/NativeDePerceptionPacket.subject:Llocal/lito381/server/NativeDePerceptionState;
 ALOAD 0
@@ -64,6 +53,15 @@
 DUP
 INVOKESPECIAL java/io/ByteArrayOutputStream.<init>()V
 PUTFIELD local/lito381/server/NativeDePerceptionPacket._bao:Ljava/io/ByteArrayOutputStream;
+ALOAD 0
+CONST null
+PUTFIELD local/lito381/server/NativeDePerceptionPacket._byteCache:[B
+ALOAD 0
+CONST null
+PUTFIELD local/lito381/server/NativeDePerceptionPacket._preparedPayload:[B
+ALOAD 0
+CONST null
+PUTFIELD local/lito381/server/NativeDePerceptionPacket._constructionFailure:Ljava/lang/IllegalStateException;
 RETURN
 method=assertPayloadMutable()V
 access=0x2
@@ -120,42 +118,42 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault=
+TRY L2 L5 L6 java/lang/RuntimeException  
 TRY L2 L5 L6 java/lang/Error  
-TRY L2 L5 L6 java/lang/RuntimeException  
 ALOAD 0
 GETFIELD local/lito381/server/NativeDePerceptionPacket._constructionFailure:Ljava/lang/IllegalStateException;
-ASTORE 2
-ALOAD 2
+ASTORE 1
+ALOAD 1
 IFNULL L0
-ALOAD 2
+ALOAD 1
 ATHROW
 LABEL L0
 ALOAD 0
 GETFIELD local/lito381/server/NativeDePerceptionPacket._byteCache:[B
-ASTORE 3
-ALOAD 3
+ASTORE 2
+ALOAD 2
 IFNULL L1
-ALOAD 3
+ALOAD 2
 ARETURN
 LABEL L1
 ALOAD 0
 GETFIELD local/lito381/server/NativeDePerceptionPacket._bao:Ljava/io/ByteArrayOutputStream;
 INVOKEVIRTUAL java/io/ByteArrayOutputStream.size()I
-ISTORE 4
+ISTORE 3
 CONST Integer:8
-ILOAD 4
+ILOAD 3
 CONST Integer:7
 IAND
 ISUB
 CONST Integer:7
 IAND
-ISTORE 5
+ISTORE 4
 LABEL L2
 CONST Integer:0
 ISTORE 6
 LABEL L3
 ILOAD 6
-ILOAD 5
+ILOAD 4
 IF_ICMPGE L4
 ALOAD 0
 GETFIELD local/lito381/server/NativeDePerceptionPacket._bao:Ljava/io/ByteArrayOutputStream;
@@ -167,7 +165,7 @@
 ALOAD 0
 GETFIELD local/lito381/server/NativeDePerceptionPacket._bao:Ljava/io/ByteArrayOutputStream;
 INVOKEVIRTUAL java/io/ByteArrayOutputStream.toByteArray()[B
-ASTORE 1
+ASTORE 5
 LABEL L5
 GOTO L7
 LABEL L6
@@ -179,7 +177,7 @@
 CONST str:"[PacketSafety] payload finalization failed: packet={} rawLength={} thread={} failures={}"
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDePerceptionPacket.getDiagnosticContext()Ljava/lang/String;
-ILOAD 4
+ILOAD 3
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 INVOKESTATIC java/lang/Thread.currentThread()Ljava/lang/Thread;
 INVOKEVIRTUAL java/lang/Thread.getName()Ljava/lang/String;
@@ -190,15 +188,15 @@
 ALOAD 6
 ATHROW
 LABEL L7
+ILOAD 3
 ILOAD 4
-ILOAD 5
 IADD
 ISTORE 6
-ALOAD 1
+ALOAD 5
 ARRAYLENGTH
 ILOAD 6
 IF_ICMPNE L8
-ALOAD 1
+ALOAD 5
 ARRAYLENGTH
 CONST Integer:7
 IAND
@@ -211,11 +209,11 @@
 CONST str:"[PacketSafety] payload finalization invariant failed: packet={} rawLength={} padding={} actualLength={} thread={} violations={}"
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDePerceptionPacket.getDiagnosticContext()Ljava/lang/String;
+ILOAD 3
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 ILOAD 4
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
-ILOAD 5
-INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
-ALOAD 1
+ALOAD 5
 ARRAYLENGTH
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 INVOKESTATIC java/lang/Thread.currentThread()Ljava/lang/Thread;
@@ -225,16 +223,16 @@
 INVOKEINTERFACE local/lito381/server/NativeDeDeletionPorts$Log.warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
 NEW java/lang/IllegalStateException
 DUP
-ALOAD 1
+ALOAD 5
 ARRAYLENGTH
 INVOKEDYNAMIC makeConcatWithConstants(I)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"Invalid finalized packet length: \u0001"]
 INVOKESPECIAL java/lang/IllegalStateException.<init>(Ljava/lang/String;)V
 ATHROW
 LABEL L9
 ALOAD 0
-ALOAD 1
+ALOAD 5
 PUTFIELD local/lito381/server/NativeDePerceptionPacket._byteCache:[B
-ALOAD 1
+ALOAD 5
 ARETURN
 method=getContent()[B
 access=0x401
~~~

</details>

<details>
<summary>NativeDePerceptionState — DIFF</summary>

待繼續移植／主控裁定：以下包含全部 metadata、欄位及方法差異；尚未證明不同方法等價，也未證明全部無法用 Java 表達。

左側正規化 SHA-256：c03bada4c332b24e2531fbe8eea149f0d13d23b3e53ff176e945adbd7d695cff。  
右側正規化 SHA-256：1e5e395130e1185cd53a3b63c7e9097e03a4c48fc17902104c19a61ecbf75e86。

~~~diff
--- staged/NativeDePerceptionState
+++ formal/NativeDePerceptionState
@@ -10,16 +10,12 @@
 permittedSubclasses=
 annotations=|||
 innerClass=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
-innerClass=local/lito381/server/NativeDeDeletionPorts$Log|local/lito381/server/NativeDeDeletionPorts|Log|0x608
-innerClass=local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608
-innerClass=local/lito381/server/NativeDeDeletionPorts$Packet|local/lito381/server/NativeDeDeletionPorts|Packet|0x8
 innerClass=local/lito381/server/NativeDeDeletionPorts$Services|local/lito381/server/NativeDeDeletionPorts|Services|0x608
 innerClass=local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408
-innerClass=local/lito381/server/NativeDePerceptionPorts$Emblems|local/lito381/server/NativeDePerceptionPorts|Emblems|0x18
 innerClass=local/lito381/server/NativeDePerceptionPorts$Fishing|local/lito381/server/NativeDePerceptionPorts|Fishing|0x608
+innerClass=local/lito381/server/NativeDePerceptionPorts$Poison|local/lito381/server/NativeDePerceptionPorts|Poison|0x408
 innerClass=local/lito381/server/NativeDePerceptionPorts$Services|local/lito381/server/NativeDePerceptionPorts|Services|0x608
 innerClass=local/lito381/server/NativeDePerceptionPorts$Shop|local/lito381/server/NativeDePerceptionPorts|Shop|0x608
-innerClass=local/lito381/server/NativeDePerceptionSpeed$SpeedState|local/lito381/server/NativeDePerceptionSpeed|SpeedState|0x19
 recordComponents=
 field=_aiOwnerToken:Ljava/util/concurrent/atomic/AtomicLong;
 access=0x10
@@ -121,20 +117,22 @@
 ALOAD 0
 GETFIELD local/lito381/server/NativeDePerceptionState.deletionOwner:Llocal/lito381/server/NativeDeDeletionOwner;
 GETFIELD local/lito381/server/NativeDeDeletionOwner.services:Llocal/lito381/server/NativeDeDeletionPorts$Services;
+ASTORE 2
+ALOAD 2
+INSTANCEOF local/lito381/server/NativeDeWorldContext
+IFEQ L0
+ALOAD 2
+CHECKCAST local/lito381/server/NativeDeWorldContext
 ASTORE 1
-ALOAD 1
-INSTANCEOF local/lito381/server/NativeDeWorldContext
-IFNE L0
+GOTO L1
+LABEL L0
 NEW java/lang/IllegalStateException
 DUP
 CONST str:"perception requires concrete application World"
 INVOKESPECIAL java/lang/IllegalStateException.<init>(Ljava/lang/String;)V
 ATHROW
-LABEL L0
-ALOAD 1
-CHECKCAST local/lito381/server/NativeDeWorldContext
-ASTORE 2
-ALOAD 2
+LABEL L1
+ALOAD 1
 ARETURN
 method=getClanid()I
 access=0x1
@@ -294,35 +292,39 @@
 LABEL L1
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeDePerceptionSpeed.stateOf(Llocal/lito381/server/NativeDePerceptionState;)Llocal/lito381/server/NativeDePerceptionSpeed$SpeedState;
-DUP
 ASTORE 2
+ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDePerceptionSpeed$SpeedState.fastAttackable()Z
 IFEQ L2
 CONST Integer:6
+ISTORE 3
 GOTO L6
 LABEL L2
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDePerceptionSpeed$SpeedState.fastMovable()Z
 IFEQ L3
 CONST Integer:4
+ISTORE 3
 GOTO L6
 LABEL L3
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDePerceptionSpeed$SpeedState.elfBrave()Z
 IFEQ L4
 CONST Integer:3
+ISTORE 3
 GOTO L6
 LABEL L4
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/NativeDePerceptionSpeed$SpeedState.brave()Z
 IFEQ L5
 CONST Integer:1
+ISTORE 3
 GOTO L6
 LABEL L5
 CONST Integer:0
+ISTORE 3
 LABEL L6
-DUP
-ISTORE 3
+ILOAD 3
 IFEQ L7
 ALOAD 1
 NEW local/lito381/server/NativeDePerceptionSkillBrave
@@ -399,14 +401,13 @@
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeDePerceptionState.getClanid()I
 INVOKEVIRTUAL local/lito381/server/NativeDePerceptionPorts$Emblems.get(I)Llocal/lito381/server/NativeDePerceptionEmblemData;
-DUP
 ASTORE 2
+ALOAD 2
 IFNULL L4
 ALOAD 1
 NEW local/lito381/server/NativeDePerceptionEmblem
 DUP
 ALOAD 2
-CHECKCAST local/lito381/server/NativeDePerceptionEmblemData
 INVOKESPECIAL local/lito381/server/NativeDePerceptionEmblem.<init>(Llocal/lito381/server/NativeDePerceptionEmblemData;)V
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionPorts$Viewer.sendPackets(Llocal/lito381/server/NativeDeDeletionPorts$Packet;)V
 LABEL L4
@@ -432,7 +433,6 @@
 LABEL L5
 CONST str:""
 LABEL L6
-CHECKCAST java/lang/String
 INVOKESPECIAL local/lito381/server/NativeDePerceptionDoActionShop.<init>(ILjava/lang/String;)V
 INVOKEVIRTUAL local/lito381/server/NativeDeDeletionPorts$Viewer.sendPackets(Llocal/lito381/server/NativeDeDeletionPorts$Packet;)V
 ALOAD 1
~~~

</details>

<details>
<summary>NativeDeProfileKernel — DIFF</summary>

待繼續移植／主控裁定：以下包含全部 metadata、欄位及方法差異；尚未證明不同方法等價，也未證明全部無法用 Java 表達。

左側正規化 SHA-256：99debd1dfb88ab09d8a5d4bba1cdf0fa0fa1dced63906242207558258ceff8c1。  
右側正規化 SHA-256：44cff7431a7f003e5a36860bfc281b9a60ae38129fa1bf6e6d8c1a5e777eb057。

~~~diff
--- staged/NativeDeProfileKernel
+++ formal/NativeDeProfileKernel
@@ -90,6 +90,7 @@
 LABEL L14
 ALOAD 0
 INVOKEINTERFACE local/lito381/server/NativeDeProfileKernel$Target.startAi()V itf
+GOTO L15
 LABEL L15
 GOTO L17
 LABEL L16
~~~

</details>

## 編譯輸入雜湊清單

以下記錄本輪最終來源、工具與 #108 stub，便於與重跑收據核對。路徑以 repo 根為準；外部 ASM 只讀。

<details>
<summary>全部來源與依賴 SHA-256</summary>

| 路徑 | SHA-256 |
|---|---|
| lito伺服器/LiTo381-Server/reference/base-0.6.71.jar | e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498 |
| scratch/issue-73/repro/target/a02.jar | 1686ee5b9f97c76a1f87758df5db6abfdfc954dceb48f3634a62a78c35e18707 |
| scratch/issue-73/repro/verify_entries.txt | 0820ebf06520d8b142b0907016f1cc03458560215fe27c6a7f6e714cb78c0462 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeItemGreaterTierTable.java | bf957904635115e00760cf1850deedf5e77400cc089567e38f4beb0c7c13db83 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeItemIdStatsTable.java | ef6b1ed14e0a6cd71a1872a3b59e28d2a57bee6a61cd38be76b4f6a57ed11377 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeItemInlayCardTable.java | 1bd48402798dc358e6a64c6bf1c8f0110004031e95f5a56ea91d2804176327d5 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeItemInventory.java | ab7ec6356c048faa1a66a3574fd794e57f6156c8860d73fd40de59fd0efa3c0b |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeItemMagicStoneTable.java | e27e61e6e331343fb05278589f11e39b1feb8ff897f1c9446e2ebdd7d9880534 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeItemPublication.java | 2e807a0969f06ffec0c6bc26ae8313157d4ec4662a909d46e5d27024f8375f3a |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeItemStateCopy.java | 601a6cdeee66badd27bc3cb9adfbcad4388ff33a1dc2bb54a00aecf9f6f665ff |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeItemTable.java | 9616dc51ce4e548787bddb2f185124d06f4eb61a80859f5854854cacda977aad |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeItemTemplatesResources.java | 1e101bfbb1b87fa16cd8d72c1786c5a221a0781aa7d8369f09d513171dec44e3 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeItemTemplatesScope.java | 9bb8be8e2e8748d92878833f6991cf29654df48da3d899bf5335ebf8796bd545 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeItemTransaction.java | 80289edc06c9a93110e0d23eb0af46e567928064b0b4b9496d7d6dc3d0a8c92c |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeItemWeaponAttrTable.java | 28c93ef7c0ed37c62a8ce169a4b5991772958958a5674ad7c87164a26be39b42 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeItemWeaponProc.java | 7df86cd1fdfe705f9048aab27bbaf7f7b9e1e727db0a5426ebbadf467dd1fa91 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeItemWeaponProcTable.java | 046e1e09c2f99f9dd5fa7b8c749a07a92700539ad1c2b135d1174e731f52837c |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeKnownState.java | a76c98f95929b6a8ada6d13e2cae825bb87408d6b4613878156f667d02edb3fe |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeLiveAiJobs.java | 5b70b394d99d0e5eed8e4e1445ca42b17f0594d30649a703dd3bd6e29dfa2094 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeLiveAiPorts.java | 0dff19bc82839ac2d41b8c4e35e265a86f5bae8d7e61f752d85303179e69cadb |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeLiveAiScope.java | d1db55623420f199ead4904fdde691db3635f397bd9db5d99246baaee5478806 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeNpcConstructionState.java | 72b8e75620358356c5d0fb67a833f7d777a4abb41edf5ff802fe3847bcccb82d |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeNpcExecutor.java | 062d4d825da74fa0d5534a7e0de919a910d7fad83780b7e48d4881c6e944e536 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeNpcHateList.java | 790d95aea4ab9d63cf7858bf28dad9efcf1eb74ca60332b96e95be807aa05998 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeNpcInventory.java | 58418656991a8ba1e8b096a720f8f4ad8addaa3ee7be9b92800823bb8726bbb3 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeNpcKnowledge.java | 37216cd40af93665c69c5406d224a87280952e18e5dfd76ec2604d1e9ff83ea8 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeNpcMobSkill.java | e399c19066b846c44f5e10a7013d3547b6ef6e55d04480a82e68828bd669261e |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeNpcMobSkillUse.java | f8254acd0b9d5cf1f8b384f6d916093b306642d8870505f5d38c7b515b7ebbf0 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeNpcMoveState.java | ff7795aba76a173c2e0c10dd44bfcc495eb2b965eb68395a052be77baa4b35cd |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeNpcTemplate.java | 58632d6eae4a2a6cfb73dc693adf53f08b3377d79b3e69c2c677e1fdc4a82f31 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeNpcTemplateLoadScope.java | 49bdeb04ff767046597907af6f47758b9ef01e83c05a4e424ebac7b42a43eb14 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeNpcTemplateMobLoader.java | 2c0af4ec5513c76e4234fceb2f3c8709527dd44a47a88534711efaacc37a853c |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeNpcTemplateNpcLoader.java | abb7d9ee32be65fd53527ab14e4407c3579504fa292a56c23c7f69640a21a260 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeNpcTemplateResources.java | d92e4f24df4321df2fcab2bd8b3e5ed5acd132674e7fb6b788f23a333dad2685 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeNpcTemplateState.java | 80276935d81a25264c7d4360c907fe206802cd868257355d8f0e18b5c5d059b1 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeNpcVitalState.java | 6a20b67370088ac12abd12e88302456389c1335db45ce81c97e2be5b8756004e |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeObjectBindings.java | 176d7380251e49acfab16853e0310432de2b54dfe59008ff56c8cd6157f3d424 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeObjectFields.java | 0103a5d31e6dcb5b4a853724d516a5a0a36d377c1ac91822a129643ff9c3704c |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeObjectScope.java | 8da829b1164e36802f4cbdfc82b4847cf690667c34f94dc2c7d18ddb7506fedf |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDePerceptionAccess.java | 64933df0243246f25e32f66c64e33233ccd9928dc7d94e7207f8a0fe8e946a15 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDePerceptionBadText.java | f9473923be3a7fab04b4b8a75ef6da85908cdefe5a5fb88a54db20ab57d05297 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDePerceptionControls.java | 0fd8cf454ec05fe2333d91ee3ac9245b9d45a7acfa7d2ebec01e4a5a24dbef7f |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDePerceptionEmblem.java | 4d35de3041ecf5f3fe68edbed3975c78d903c6c3f272ccda55c2e9dbeeddc981 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDePerceptionJobs.java | ca5867d2b4c8ab9652860070c33a69f24594a027718485ffc3c16f8d6e68fb67 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDePerceptionNPCPack_De.java | e8748815e87da79a5037a0a6df8e2925da18b1b5df5db6fd983d51b55889d1af |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDePerceptionOutput.java | 73afef8d295ed7429c3ab82ed7ff042fa652c963cd72c1695f0ea88f3fb48bcb |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDePerceptionPacket.java | c8711ad60dab2fcb46fc36a365290a7a22b7c80ffbd6fcbee48d35645ebb3e44 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDePerceptionScope.java | b3ed603d497e37f6ae3bd941b6611aa22a63719869970cb0d0357d494098f8a2 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDePerceptionSpeed.java | c713181eeda3683ea43dad1e2180473b754c1c0f4c14796955a29cffc1ac43f3 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDePerceptionState.java | f2b4913f18a206c310bfdc44ceccbc0c752f098f1d64db85d48e1758792df15e |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeProfileKernel.java | 45418636db0e15897488eb3a86609b97b0d067cbad387439550c63b19394e160 |
| lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/NativeDeShopData.java | bf6d279fcd9dd1c79a38b8df099fa842931afa4ac4946df94919f533f32f5c51 |
| lito伺服器/LiTo381-Server/compile-stubs/com/lineage/server/datatables/QuestInstanceTable.java | 97fd50dbf62cf5237dd14ec1d5d818e98cba171c99f4ab414c51dc73f4bfb206 |
| lito伺服器/LiTo381-Server/compile-stubs/com/lineage/server/model/Instance/DeathLifecycleGate.java | dc13d60f07c7deee584165f628afcc600918fc15807777220b70e317df0f9d99 |
| lito伺服器/LiTo381-Server/compile-stubs/com/lineage/server/model/Instance/NpcDeletionVisibilityState.java | bcda282d398000a4829b0b7c40c75ea86f87b56cb99f3a7f1e99db91436c1a5f |
| lito伺服器/LiTo381-Server/compile-stubs/com/lineage/server/model/item/ItemGenerationContext.java | 660a183d6e88adc894cf818209d7d97f0ad57001e16dd31896b00994376c7a29 |
| lito伺服器/LiTo381-Server/compile-stubs/com/lineage/server/model/proc/ProcRandomHelper.java | ff460779c9f3cbbbe9b9daaa5e88e83b823523b596a7a3121b17bc80723fbcf5 |
| lito伺服器/LiTo381-Server/compile-stubs/com/lineage/server/model/skill/L1SkillId.java | 7352c513fc749b8bb09efc2605b8b21e9911e67e7ca8c9b6496ac5b03a79d59f |
| lito伺服器/LiTo381-Server/compile-stubs/com/lineage/server/utils/TeleportTrace.java | d2e9a46d54ccab8e8e2eeac7572ace154971f6e624a0d38d1131334c9b3fdcbd |
| lito伺服器/LiTo381-Server/compile-stubs/com/lineage/server/world/PlayerFilter.java | 861c5e1b4e090b4148a8b5a019737ccfe6a8d44a67ee8fdc881332ba3e62b435 |
| lito伺服器/LiTo381-Server/compile-stubs/com/lineage/server/world/World.java | ea679d91873e2a54dde87aca99349a25f17b08f89fc020b738ef59ff3210cbd0 |
| lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/ClassComparer.java | 40d6e0bb103120e454e3de2edaf312625001b361dc331673ff2c35c3610253b6 |
| lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/JarIndex.java | ef77691858a40872f31330cb96f17385223f6c6da9046db87c3acd8f4dc5c747 |
| lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/SemanticDiff.java | 9a38e7ab94df69ce59f7a1bd14f934229c8b12df1db0931b5a35d31cd5ceb30e |
| D:\dev-cache\maven\repository\org\ow2\asm\asm\9.7\asm-9.7.jar | adf46d5e34940bdf148ecdd26a9ee8eea94496a72034ff7141066b3eea5c4e9d |
| D:\dev-cache\maven\repository\org\ow2\asm\asm-tree\9.7\asm-tree-9.7.jar | 62f4b3bc436045c1acb5c3ba2d8ec556ec3369093d7f5d06c747eb04b56d52b1 |

</details>

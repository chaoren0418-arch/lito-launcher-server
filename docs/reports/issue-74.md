# Issue #74 — T020-A03 正式 0.6.71 暫存

## 結論／做了什麼

**本輪完成可編譯修補與完整證據，未達成最終語意驗收，請勿落地或標 done。** 沿用既有 `task/74`，沒有 checkout／建分支；保留遠端原提交 `28655ba1793f5db59a4dfd6ae2422ec36d7b1f03` 歷史，合入此 worktree 的主線基底 `33f3f5c`，不使用強制推送。

47 份 Java 直接編譯產生 **88 個 entry**，missing／extra 皆空。原版 T020d／T020i（本基底的 #132 版本）最終為 **47 SAME、41 DIFF、0 ONLY_LEFT、0 ONLY_RIGHT**。41 DIFF 中：**31 筆全部欄位與方法相同、僅類別 metadata 不同；9 筆仍有欄位／方法差異；1 筆為主控新增的 debug 刻意偏離**。這個分類由附錄完整正規化內容得出，不是依 firstDiff 推測。

執行中重新讀取 [Issue #74](https://github.com/chaoren0418-arch/lito-launcher-server/issues/74)，主控已將依賴改為 **#108、#135、#153 全部合併後才派工**，並要求新增唯一的 `NativeDeWorldLog.debug(String,Object,Object)`。#108 已在本基底；#135／#153 尚未在本基底，#153 仍排隊。本輪保留現行工具的真實 DIFF，不套未合併比較器，不宣稱新版核准規則已通過。

- 重新用 CFR 0.152 反編譯正式 JAR 的 A03，並以 Vineflower 1.11.2 交叉還原；在前次 staged 來源上修正，47 檔均保留 DECOMPILED 標頭。
- 修正商店載入的 Map 呼叫型別、資源關閉結構與分支方向；修正地形／可見性作用域的 try/finally 返回範圍、switch 出口、例外順序及區域變數配置。
- 修正匿名 Reader 關閉失敗時的 addSuppressed 分支：前版在首次關閉例外時也對同一例外呼叫 addSuppressed，正式版使用 else。本輪 `NativeDeTerrainResources$1` 完整 SAME。
- 修正 WorldCore 初始化順序與查詢 lambda 捕獲型別，並採用重新反編譯的部分世界物件及能力載入實作；未解方法完整列在附錄。
- `NativeDeVisibilitySwitch` 為獨立 Java 定義。javap 已確認正式 class 無 SourceFile、flags 為 `0x1020`、EnclosingMethod／NestHost 均為 `NativeDeDeletionPorts$Viewer`。其 clinit 完整相同，但 Java 隱含建構子及 metadata 仍有差異。
- 依最新 #74／#153 裁定只加入 `default void debug(String,Object,Object)`，內容為 `emit("debug", message, new Object[]{a,b})`，未加入其他 debug 多載。舊工具仍列 DIFF，須由 #153 deviations 驗收。
- 未改 src/main、pom.xml、docs/REPORT.md、核准清單、比較器或正式 JAR。未覆蓋 class bytes；驗收 JAR 全由 staged Java 直接編譯，不含 com/lineage stub。

## 已驗證

環境：Windows PowerShell、Python 3.12.10、Microsoft OpenJDK **17.0.17+10-LTS**。一般沙箱初始化失敗，命令使用核准提升權限模式；工作輸出仍只在此 worktree。原專案僅唯讀取用反編譯工具；Git 共用儲存庫 metadata 寫入限本次 merge／commit／push。

反編譯工具唯讀位置與 SHA-256：

- CFR：`D:/lineage開發/3-lito開發/scratch/wt/task-70/scratch/issue-70/cfr-0.152.jar`，`f686e8f3ded377d7bc87d216a90e9e9512df4156e75b06c655a16648ae8765b2`。
- Vineflower：`D:/lineage開發/3-lito開發/scratch/wt/task-72/scratch/issue-72/vineflower-1.11.2.jar`，`e1e2415e7f78b34960402c4beddfc88e033d7842a23ecd132a8ec2eadd54f6bf`。

原始輸出摘要：

```text
Compiled 9 compile-only sources -> target/legacy-stubs (never packaged)
tool-build exit 0
javac exit 0
Note: Some input files use unchecked or unsafe operations.
jar exit 0
entries 88 missing [] extra []
semantic exit 0
SUMMARY SAME=47 SAME_EFFECTS=0 DIFF=41 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=3 ALLOW_METHODS=0
classSelection.missingLeft=[]
classSelection.extraLeft=[]
```

各步 exit 0；semantic-diff 有 DIFF 也會回傳 0，不能當作驗收通過。原前版結果 35 SAME／53 DIFF，本輪 Java 修正後原為 48／40；補上主控要求的 debug 偏離後為 47／41。打包固定 `--date 2026-11-05T00:00:00Z`，便於重現雜湊，此值只是 ZIP timestamp，不表示發布或驗收時間。`git diff --check` 通過。精確範圍核對為 47 Java 加本報告，共 48 檔。

## 推測／待驗證／給下一棒的提示

- **調度器／主控**：按最新依賴等待 #135／#153 合併，不要把本次交付當作 88 entry 已通過。以 `needs:claude` 提交剩餘完整差異供裁定，不需使用者實機介入。
- **A03 執行器**：更新到含 #135／#153 的主線後，裁切本批核准清單再重跑 88 entry。metadata、switch-map、lambda 前綴及隱含建構子等結果以新版工具實測為準，不能推定全部會消失。
- **仍需 Java 修正／審查**：`NativeDeVisibilityCoordinator`（try 邊界、局部槽位、lambda 名稱）、`NativeDeVisibilityPc`（額外建構子、例外範圍、布林返回）、`NativeDeWorldContext`（鎖、返回與 try 邊界）、`NativeDeWorldCore`（查詢分支、lambda 本體、鎖與局部槽位）、`NativeDeWorldKinds$Item.hydrateAllAbilities`（剩餘局部槽位／作用域）。本輪未證明它們完全等價，也不宣稱所有剩餘差異皆無法用 Java 修正；完整 diff 可直接定位。
- `NativeDeVisibilitySwitch` 的 metadata／隱含建構子、`NativeDeWorldCore$1`／`NativeDeWorldPlayerFilter$1` 的舊 switch-map 名稱，需依 #135／#153 核准規則重驗。本輪未自行放寬比較器或覆蓋 class。
- `NativeDeWorldLog#debug(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V` 是 #74／#153 明確要求的偏離；尚未以 `--deviations` 驗收。A-land 要模擬 A02 選用欄位讀取失敗，確認只記 debug、不丟 NoSuchMethodError。
- **A-land**：整批依新版規則驗收通過後才能搬進 src/main，並統一跑單元測試／回歸。本輪未跑單元測試／回歸（本任務不適用），未驗證真機行為。
- 未啟停伺服器／登入器／遊戲、未連正式 DB、未讀機密設定／封存／客戶端、未修改硬連結或正式部署。

## 產出路徑

- `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/`：A03 精確 47 份 Java。
- `docs/reports/issue-74.md`：唯一提交報告，包含完整指令、雜湊與全部 41 筆 normalized unified diff。
- `scratch/issue-74/`：不入 Git 的反編譯來源、腳本、原始 log、JSON／Markdown、兩側 normalized 全文；`target/a03.jar` 是本輪驗收產物。所有 class／JAR 均在 target 子目錄。

## 雜湊與可重現驗證

| 檔案 | SHA-256 |
|---|---|
| lito伺服器/LiTo381-Server/reference/base-0.6.71.jar | e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498 |
| scratch/issue-74/target/a03.jar | 005212c48e531d4edd9979597531c6131269e4597d90124456256fdc4483822b |
| scratch/issue-74/all-diffs.md | 377ed58bcc0d3e8e24590801130a2a6a623b697ecbf0bb030f2b337e7315a6fb |

先從 worktree 根目錄執行：

```powershell
$env:JAVA_HOME = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot'
& lito伺服器/LiTo381-Server/tools/legacy-stubs/build.ps1
New-Item -ItemType Directory -Force scratch/issue-74 | Out-Null
py -3 -B -X utf8 scratch/issue-74/verify.py
```

下段存成 UTF-8 的 `scratch/issue-74/verify.py`，本機已有該檔。無佔位參數；換機只需替換 JDK／ASM cache 的安裝路徑。只選 A03 的 47 檔，產物恰為 88 entry。腳本只刪除此任務 target/classes 內自己產生的 class。

```python
from pathlib import Path
import json, subprocess, zipfile, hashlib
root=Path.cwd();s=root/'lito伺服器/LiTo381-Server';w=root/'scratch/issue-74';jdk=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
b=next(b for b in json.loads((root/'docs/combat-merge/a-batches.json').read_text(encoding='utf-8'))['batches'] if b['id']=='A03')
w.mkdir(parents=True,exist_ok=True)
(w/'entries.txt').write_text('\n'.join(b['verify_entries'])+'\n',encoding='utf-8')
asm=';'.join(str(Path('D:/dev-cache/maven/repository/org/ow2/asm')/a/'9.7'/f'{a}-9.7.jar') for a in ['asm','asm-tree'])
staged=s/'merge-inputs/formal-0.6.71/staged/local/lito381/server'
def run(name,args):
 r=subprocess.run([str(x) for x in args],capture_output=True);(w/(name+'.log')).write_bytes(r.stdout+r.stderr);print(name,'exit',r.returncode);print((r.stdout+r.stderr).decode('utf-8',errors='replace')[:9000]);return r.returncode
if run('tool-build',[jdk/'javac.exe','-J-Dfile.encoding=UTF-8','--release','17','-encoding','UTF-8','-cp',asm,'-d',w/'target/tool',*sorted((s/'tools/semantic-diff/src/main/java').rglob('*.java'))]): raise SystemExit(1)
# Empty only previously generated class files at this exact task-local destination.
out=w/'target/classes';out.mkdir(parents=True,exist_ok=True)
for p in out.rglob('*.class'):p.unlink()
if run('javac',[jdk/'javac.exe','-J-Dfile.encoding=UTF-8','--release','17','-encoding','UTF-8','-cp',str(s/'reference/base-0.6.71.jar')+';'+str(s/'target/legacy-stubs')+';'+str(s/'lib/*'),'-d',out,*[staged/(n+'.java') for n in b['classes']]]): raise SystemExit(1)
if run('jar',[jdk/'jar.exe','--create','--file',w/'target/a03.jar','--date','2026-11-05T00:00:00Z','-C',out,'.']):raise SystemExit(1)
with zipfile.ZipFile(w/'target/a03.jar') as z: actual={n for n in z.namelist() if n.endswith('.class')}
expected=set(b['verify_entries']);print('entries',len(actual),'missing',sorted(expected-actual),'extra',sorted(actual-expected));assert actual==expected
run('semantic',[jdk/'java.exe','-Dfile.encoding=UTF-8','-cp',str(w/'target/tool')+';'+asm,'local.lito381.semanticdiff.SemanticDiff','--left',w/'target/a03.jar','--right',s/'reference/base-0.6.71.jar','--classes',w/'entries.txt','--json',w/'semantic.json','--md',w/'semantic.md'])
r=json.loads((w/'semantic.json').read_text());print(r.keys())
print('jar SHA256',hashlib.sha256((w/'target/a03.jar').read_bytes()).hexdigest())
```

## 完整內容分類

下列 metadata-only 仍為原工具 DIFF，不是豁免；此分類逐一比過全部欄位和方法，沒有以 firstDiff 代替。

- 僅 metadata（31）：`NativeDeShopTable`、`NativeDeTerrainTable`、`NativeDeTerrainText`、`NativeDeTerrainV1`、`NativeDeVisibilityCheck`、`NativeDeVisibilityHp`、`NativeDeVisibilityPacket`、`NativeDeVisibilityTimer`、`NativeDeWorldContext$Entry`、`NativeDeWorldCore$VisibleBinding`、`NativeDeWorldKinds`、`NativeDeWorldKinds$Item$EquipmentTimerLease`、`NativeDeWorldKinds$Item$LastStatus`、`NativeDeWorldLocation`、`NativeDeWorldRegistryBow`、`NativeDeWorldRegistryCrown`、`NativeDeWorldRegistryDarkelf`、`NativeDeWorldRegistryDoll`、`NativeDeWorldRegistryDragonKnight`、`NativeDeWorldRegistryEffect`、`NativeDeWorldRegistryElf`、`NativeDeWorldRegistryIllusionist`、`NativeDeWorldRegistryItem`、`NativeDeWorldRegistryKnight`、`NativeDeWorldRegistryMob`、`NativeDeWorldRegistryPet`、`NativeDeWorldRegistryQuest`、`NativeDeWorldRegistrySummons`、`NativeDeWorldRegistryTrap`、`NativeDeWorldRegistryTrap$TrapCell`、`NativeDeWorldRegistryWizard`。
- 仍有欄位／方法差異（含核准偏離，共 10）：`NativeDeVisibilityCoordinator`、`NativeDeVisibilityPc`、`NativeDeVisibilitySwitch`、`NativeDeWorldContext`、`NativeDeWorldCore`、`NativeDeWorldCore$1`、`NativeDeWorldKinds$Item`、`NativeDeWorldLog`、`NativeDeWorldPlayerFilter`、`NativeDeWorldPlayerFilter$1`。

## 附錄：完整正規化證據

診斷器唯讀載入兩個 JAR，呼叫原版 ClassComparer 的正規化函式，輸出所有 class 屬性、欄位、方法、annotationDefault、指令與 try-catch。不在第一處差異返回、不截斷運算元、不修改任何 ClassNode 或 JAR；集合依原工具排序。以下 unified diff 包含全部差異 hunks，相同內容依 diff 標準省略。診斷不代替原版驗收摘要。

重現：把 Java 區塊存成 UTF-8 無 BOM 的 `scratch/issue-74/Dump.java`，Python 區塊存成 `scratch/issue-74/dump.py`，先跑 verify，再執行 `py -3 -B -X utf8 scratch/issue-74/dump.py`。

```java
package local.lito381.semanticdiff;
import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import org.objectweb.asm.tree.*;
public class Dump {
 static Object invoke(String name,Object...args) throws Exception {
  for(Method m:ClassComparer.class.getDeclaredMethods()) if(m.getName().equals(name)&&m.getParameterCount()==args.length){m.setAccessible(true);return m.invoke(null,args);}throw new Exception(name);
 }
 static void line(StringBuilder b,String key,Object v){b.append((key+" "+Objects.toString(v, "")).stripTrailing()).append('\n');}
 static Object sorted(Object o){if(o instanceof List){var list=new ArrayList<>((List<String>)o);Collections.sort(list);return list;}return o;}
 static String dump(ClassNode c,JarIndex j)throws Exception {
  StringBuilder b=new StringBuilder();
  for(String k:List.of("access","superName","interfaces","signature","outerClass","outerMethod","outerMethodDesc","nestHostClass","nestMembers","permittedSubclasses"))line(b,k,sorted(ClassNode.class.getField(k).get(c)));
  line(b,"annotations",invoke("annotations",c));
  line(b,"innerClasses",invoke("innerClasses",c));line(b,"recordComponents",invoke("records",c,j));
  var fields=new TreeMap<String,FieldNode>();for(var f:c.fields)fields.put(f.name+":"+f.desc,f);
  for(var e:fields.entrySet()){var f=e.getValue();line(b,"FIELD",e.getKey());line(b,"access",invoke("memberAccess",f.access));line(b,"signature",f.signature);line(b,"value",invoke("value",f.value));line(b,"annotations",invoke("fieldAnns",f));}
  var methods=new TreeMap<String,MethodNode>();for(var m:c.methods)methods.put(j.remapMethod(c.name,m.name)+m.desc,m);
  for(var e:methods.entrySet()){var m=e.getValue();line(b,"METHOD",e.getKey());line(b,"access",invoke("memberAccess",m.access));line(b,"signature",m.signature);line(b,"exceptions",sorted(m.exceptions));line(b,"annotations",invoke("methodAnns",m,invoke("labelIds",m),j));line(b,"annotationDefault",invoke("defaultValue",m.annotationDefault,j));
   for(String insn:ClassComparer.instructions(m,j))line(b,"  ",insn);
   line(b,"tryCatch",invoke("tryCatch",m,j));}
  return b.toString();
 }
 public static void main(String[]args)throws Exception{
  JarIndex l=JarIndex.load(Path.of(args[0])),r=JarIndex.load(Path.of(args[1]));Path out=Path.of(args[2]);Files.createDirectories(out);
  for(String n:l.classes.keySet()){
   ClassNode a=l.readTree(n),b=r.readTree(n);if(b==null)continue;String simple=n.substring(n.lastIndexOf('/')+1);
   Files.writeString(out.resolve(simple+".left.txt"),dump(a,l));Files.writeString(out.resolve(simple+".right.txt"),dump(b,r));
   var bm=new TreeMap<String,MethodNode>();for(var m:b.methods)bm.put(r.remapMethod(n,m.name)+m.desc,m);
   for(var m:a.methods){String k=l.remapMethod(n,m.name)+m.desc;var rm=bm.remove(k);var d=rm==null?"only left":ClassComparer.compareMethod(k,m,rm,l,r,false);if(d!=null)System.out.println(simple+"#"+k+" "+(d instanceof ClassComparer.FirstDiff ? ((ClassComparer.FirstDiff)d).oneLine():d));}
   for(String k:bm.keySet())System.out.println(simple+"#"+k+" only right");
  }
 }
}
```

```python
from pathlib import Path
exec((Path.cwd()/'scratch/issue-74/verify.py').read_text(encoding='utf-8-sig').split("if run('tool-build'")[0])
run('dump-build',[jdk/'javac.exe','--release','17','-encoding','UTF-8','-cp',str(w/'target/tool')+';'+asm,'-d',w/'target/tool',w/'Dump.java'])
run('dump',[jdk/'java.exe','-Dfile.encoding=UTF-8','-cp',str(w/'target/tool')+';'+asm,'local.lito381.semanticdiff.Dump',w/'target/a03.jar',s/'reference/base-0.6.71.jar',w/'normalized'])
import difflib
parts=[]
for p in sorted((w/'normalized').glob('*.left.txt')):
 q=p.with_name(p.name.replace('.left.','.right.'))
 diff=''.join(difflib.unified_diff(p.read_text().splitlines(True),q.read_text().splitlines(True),fromfile=p.name,tofile=q.name))
 if diff:parts.append((p.stem.removesuffix('.left'),diff))
(w/'all-diffs.md').write_text('\n'.join('### '+n+'\n\n```diff\n'+d+'```\n' for n,d in parts),encoding='utf-8')
print('full diff chars',sum(len(d) for n,d in parts),'entries',len(parts))
```

### NativeDeShopTable

```diff
--- NativeDeShopTable.left.txt
+++ NativeDeShopTable.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; java/util/Map$Entry|java/util/Map|Entry|0x609; local/lito381/server/NativeDeTerrainScope$Database|local/lito381/server/NativeDeTerrainScope|Database|0x18; local/lito381/server/NativeDeTerrainScope$Logs|local/lito381/server/NativeDeTerrainScope|Logs|0x18
+innerClasses java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; java/util/Map$Entry|java/util/Map|Entry|0x609
 recordComponents
 FIELD _log:Llocal/lito381/server/NativeDeWorldLog;
 access 0x1a
```

### NativeDeTerrainTable

```diff
--- NativeDeTerrainTable.left.txt
+++ NativeDeTerrainTable.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeTerrainScope$Database|local/lito381/server/NativeDeTerrainScope|Database|0x18; local/lito381/server/NativeDeTerrainScope$Logs|local/lito381/server/NativeDeTerrainScope|Logs|0x18
+innerClasses java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
 recordComponents
 FIELD _log:Llocal/lito381/server/NativeDeWorldLog;
 access 0x1a
```

### NativeDeTerrainText

```diff
--- NativeDeTerrainText.left.txt
+++ NativeDeTerrainText.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeTerrainScope$Logs|local/lito381/server/NativeDeTerrainScope|Logs|0x18
+innerClasses java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
 recordComponents
 FIELD _log:Llocal/lito381/server/NativeDeWorldLog;
 access 0x1a
```

### NativeDeTerrainV1

```diff
--- NativeDeTerrainV1.left.txt
+++ NativeDeTerrainV1.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeTerrainScope$Logs|local/lito381/server/NativeDeTerrainScope|Logs|0x18
+innerClasses java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
 recordComponents
 FIELD BITFLAG_IS_IMPASSABLE:B
 access 0x1a
```

### NativeDeVisibilityCheck

```diff
--- NativeDeVisibilityCheck.left.txt
+++ NativeDeVisibilityCheck.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608; local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408; local/lito381/server/NativeDeVisibilityScope$Logs|local/lito381/server/NativeDeVisibilityScope|Logs|0x18
+innerClasses
 recordComponents
 FIELD _log:Llocal/lito381/server/NativeDeWorldLog;
 access 0x1a
```

### NativeDeVisibilityCoordinator

```diff
--- NativeDeVisibilityCoordinator.left.txt
+++ NativeDeVisibilityCoordinator.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608; local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408; local/lito381/server/NativeDeVisibilityPorts$FieldObject|local/lito381/server/NativeDeVisibilityPorts|FieldObject|0x608; local/lito381/server/NativeDeVisibilityScope$Logs|local/lito381/server/NativeDeVisibilityScope|Logs|0x18; local/lito381/server/NativeDeWorldKinds$Dwarf|local/lito381/server/NativeDeWorldKinds|Dwarf|0x8; local/lito381/server/NativeDeWorldKinds$Merchant|local/lito381/server/NativeDeWorldKinds|Merchant|0x8
+innerClasses java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
 recordComponents
 FIELD _log:Llocal/lito381/server/NativeDeWorldLog;
 access 0x1a
@@ -87,7 +87,7 @@
    INVOKESTATIC local/lito381/server/NativeDeVisibilityScope._tickCount()J
    LRETURN
 tryCatch []
-METHOD lambda$visibilitySource$updateForPc$#0(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
+METHOD lambda$updateForPc$#0(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
 access 0xa
 signature
 exceptions []
@@ -118,12 +118,12 @@
    IFEQ L3
    ALOAD 1
    CHECKCAST local/lito381/server/NativeDeWorldKinds$Merchant
-   ASTORE 3
+   ASTORE 2
    ALOAD 0
    ALOAD 1
    INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1PcInstance$knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
    IFNE L2
-   ALOAD 3
+   ALOAD 2
    ALOAD 0
    INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1MerchantInstance$onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
    LABEL L2
@@ -134,12 +134,12 @@
    IFEQ L5
    ALOAD 1
    CHECKCAST local/lito381/server/NativeDeWorldKinds$Dwarf
-   ASTORE 3
+   ASTORE 2
    ALOAD 0
    ALOAD 1
    INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1PcInstance$knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
    IFNE L4
-   ALOAD 3
+   ALOAD 2
    ALOAD 0
    INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1DwarfInstance$onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
    LABEL L4
@@ -150,12 +150,12 @@
    IFEQ L7
    ALOAD 1
    CHECKCAST local/lito381/server/NativeDeVisibilityPorts$FieldObject
-   ASTORE 3
+   ASTORE 2
    ALOAD 0
    ALOAD 1
    INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1PcInstance$knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
    IFNE L6
-   ALOAD 3
+   ALOAD 2
    ALOAD 0
    INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1FieldObjectInstance$onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
    LABEL L6
@@ -163,17 +163,17 @@
    LABEL L7
    ALOAD 0
    INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1PcInstance$getMapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S
+   ISTORE 2
+   ALOAD 0
+   INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1PcInstance$get_showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
    ISTORE 3
-   ALOAD 0
-   INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1PcInstance$get_showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
-   ISTORE 4
    ALOAD 1
    INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1Object$getMapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S
+   ILOAD 2
+   IF_ICMPNE L8
+   ALOAD 1
+   INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1Object$get_showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
    ILOAD 3
-   IF_ICMPNE L8
-   ALOAD 1
-   INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1Object$get_showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
-   ILOAD 4
    IF_ICMPEQ L9
    LABEL L8
    RETURN
@@ -192,11 +192,11 @@
    IFEQ L11
    ALOAD 1
    CHECKCAST local/lito381/server/NativeDeDeletionState
-   DUP
-   ASTORE 2
+   ASTORE 4
+   ALOAD 4
    INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1NpcInstance$getHiddenStatus(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
    IFEQ L11
-   ALOAD 2
+   ALOAD 4
    ALOAD 0
    INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1NpcInstance$approachPlayer(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
    LABEL L11
@@ -209,8 +209,8 @@
    IFEQ L12
    ALOAD 1
    CHECKCAST local/lito381/server/NativeDeCharacterFields
-   ASTORE 2
-   ALOAD 2
+   ASTORE 4
+   ALOAD 4
    ALOAD 0
    INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1Character$broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
    LABEL L12
@@ -227,8 +227,8 @@
    IFEQ L13
    ALOAD 1
    CHECKCAST local/lito381/server/NativeDeCharacterFields
-   ASTORE 2
-   ALOAD 2
+   ASTORE 4
+   ALOAD 4
    ALOAD 0
    INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1Character$broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
    LABEL L13
@@ -262,7 +262,7 @@
    LABEL L3
    ALOAD 1
    INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-   IFEQ L10
+   IFEQ L8
    ALOAD 1
    INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
    CHECKCAST local/lito381/server/NativeDeDeletionPorts$Viewer
@@ -273,19 +273,16 @@
    LABEL L4
    ALOAD 2
    INVOKESTATIC local/lito381/server/NativeDeVisibilityCheck.check(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)Z
-   IFNE L6
-   LABEL L5
-   GOTO L3
-   LABEL L6
+   IFEQ L5
    ALOAD 2
    INVOKESTATIC local/lito381/server/NativeDeVisibilityCoordinator.updateForPc(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
    INVOKESTATIC local/lito381/server/NativeDeVisibilityScope._pcUpdateCount()J
    CONST Long:1
    LADD
    INVOKESTATIC local/lito381/server/NativeDeVisibilityScope._pcUpdateCount$set(J)V
-   LABEL L7
-   GOTO L9
-   LABEL L8
+   LABEL L5
+   GOTO L7
+   LABEL L6
    ASTORE 3
    INVOKESTATIC local/lito381/server/NativeDeVisibilityScope.log()Llocal/lito381/server/NativeDeWorldLog;
    CONST str:"VisibilityCoordinator 處理玩家 {} 異常"
@@ -293,11 +290,11 @@
    INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1PcInstance$getName(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Ljava/lang/String;
    ALOAD 3
    INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V itf
+   LABEL L7
+   GOTO L3
+   LABEL L8
+   GOTO L10
    LABEL L9
-   GOTO L3
-   LABEL L10
-   GOTO L12
-   LABEL L11
    ASTORE 0
    INVOKESTATIC local/lito381/server/NativeDeVisibilityScope.log()Llocal/lito381/server/NativeDeWorldLog;
    CONST str:"VisibilityCoordinator tick 異常"
@@ -305,9 +302,9 @@
    INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
    ALOAD 0
    ATHROW
-   LABEL L12
-   RETURN
-tryCatch [TRY L4 L5 L8 java/lang/Exception  , TRY L6 L7 L8 java/lang/Exception  , TRY L0 L1 L11 java/lang/Exception  , TRY L2 L10 L11 java/lang/Exception  ]
+   LABEL L10
+   RETURN
+tryCatch [TRY L4 L5 L6 java/lang/Exception  , TRY L0 L1 L9 java/lang/Exception  , TRY L2 L8 L9 java/lang/Exception  ]
 METHOD updateForPc(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
 access 0x9
 signature
@@ -349,7 +346,7 @@
    INVOKESTATIC local/lito381/server/NativeDeVisibilityScope.range()I
    CONST null
    ALOAD 0
-   INVOKEDYNAMIC accept(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)Ljava/util/function/Consumer; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)V, handle:H6 local/lito381/server/NativeDeVisibilityCoordinator.lambda$visibilitySource$updateForPc$#0(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V, type:(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V]
+   INVOKEDYNAMIC accept(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)Ljava/util/function/Consumer; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)V, handle:H6 local/lito381/server/NativeDeVisibilityCoordinator.lambda$updateForPc$#0(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V, type:(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V]
    INVOKEVIRTUAL local/lito381/server/NativeDeVisibilityWorld.forEachNearbyObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;ILjava/util/function/Predicate;Ljava/util/function/Consumer;)V
    RETURN
 tryCatch []
```

### NativeDeVisibilityHp

```diff
--- NativeDeVisibilityHp.left.txt
+++ NativeDeVisibilityHp.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608
+innerClasses
 recordComponents
 FIELD _byte:[B
 access 0x2
```

### NativeDeVisibilityPacket

```diff
--- NativeDeVisibilityPacket.left.txt
+++ NativeDeVisibilityPacket.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeDeletionPorts$Packet|local/lito381/server/NativeDeDeletionPorts|Packet|0x8
+innerClasses com/lineage/server/utils/TeleportTrace$PacketTrace|com/lineage/server/utils/TeleportTrace|PacketTrace|0x19; java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
 recordComponents
 FIELD _bao:Ljava/io/ByteArrayOutputStream;
 access 0x14
```

### NativeDeVisibilityPc

```diff
--- NativeDeVisibilityPc.left.txt
+++ NativeDeVisibilityPc.right.txt
@@ -9,18 +9,8 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeDeletionPorts$Monster|local/lito381/server/NativeDeDeletionPorts|Monster|0x408; local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608; local/lito381/server/NativeDeDeletionPorts$Packet|local/lito381/server/NativeDeDeletionPorts|Packet|0x8; local/lito381/server/NativeDeDeletionPorts$RemoveObject|local/lito381/server/NativeDeDeletionPorts|RemoveObject|0x18; local/lito381/server/NativeDeDeletionPorts$Summon|local/lito381/server/NativeDeDeletionPorts|Summon|0x408; local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408; local/lito381/server/NativeDeDeletionVisibility$Phase|local/lito381/server/NativeDeDeletionVisibility|Phase|0x4019; local/lito381/server/NativeDeDeletionVisibility$RepairKind|local/lito381/server/NativeDeDeletionVisibility|RepairKind|0x4019; local/lito381/server/NativeDeDeletionVisibility$Snapshot|local/lito381/server/NativeDeDeletionVisibility|Snapshot|0x19; local/lito381/server/NativeDeVisibilityPorts$Follower|local/lito381/server/NativeDeVisibilityPorts|Follower|0x608; local/lito381/server/NativeDeWorldKinds$Pet|local/lito381/server/NativeDeWorldKinds|Pet|0x8
+innerClasses
 recordComponents
-METHOD <init>()V
-access 0x1
-signature
-exceptions []
-annotations ||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault
-   ALOAD 0
-   INVOKESPECIAL java/lang/Object.<init>()V
-   RETURN
-tryCatch []
 METHOD isGmHpBarTarget(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
 access 0x9
 signature
@@ -59,6 +49,11 @@
    LABEL L4
    ALOAD 1
    INSTANCEOF local/lito381/server/NativeDeVisibilityPorts$Follower
+   IFEQ L5
+   CONST Integer:1
+   IRETURN
+   LABEL L5
+   CONST Integer:0
    IRETURN
 tryCatch []
 METHOD isHpBarTarget(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
@@ -108,12 +103,12 @@
    INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1PcInstance$get_showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
    ISTORE 3
    ALOAD 1
-   INVOKEVIRTUAL java/util/ArrayList.iterator()Ljava/util/Iterator;
+   INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
    ASTORE 4
    LABEL L1
    ALOAD 4
    INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-   IFEQ L15
+   IFEQ L14
    ALOAD 4
    INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
    CHECKCAST local/lito381/server/NativeDeDeletionPorts$ObjectRef
@@ -364,15 +359,13 @@
    LABEL L11
    INVOKESTATIC local/lito381/server/NativeDeVisibilityScope.range()I
    CONST Integer:-1
-   IF_ICMPNE L13
+   IF_ICMPNE L12
    ALOAD 0
    INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1PcInstance$getLocation(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Llocal/lito381/server/NativeDeWorldLocation;
    ALOAD 5
    INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1Object$getLocation(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Llocal/lito381/server/NativeDeWorldLocation;
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldLocation.isInScreen(Llocal/lito381/server/NativeDeWorldPoint;)Z
-   IFEQ L12
-   GOTO L1
-   LABEL L12
+   IFNE L13
    ALOAD 0
    ALOAD 5
    INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1PcInstance$removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
@@ -382,30 +375,29 @@
    ALOAD 5
    INVOKESPECIAL local/lito381/server/NativeDeDeletionPorts$RemoveObject.<init>(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
    INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1PcInstance$sendPackets(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$Packet;)V
-   GOTO L1
-   LABEL L13
+   GOTO L13
+   LABEL L12
    ALOAD 0
    INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1PcInstance$getLocation(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Llocal/lito381/server/NativeDeWorldLocation;
    ALOAD 5
    INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1Object$getLocation(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Llocal/lito381/server/NativeDeWorldLocation;
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldLocation.getTileLineDistance(Llocal/lito381/server/NativeDeWorldPoint;)I
    INVOKESTATIC local/lito381/server/NativeDeVisibilityScope.range()I
-   IF_ICMPGT L14
+   IF_ICMPLE L13
+   ALOAD 0
+   ALOAD 5
+   INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1PcInstance$removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
+   ALOAD 0
+   NEW local/lito381/server/NativeDeDeletionPorts$RemoveObject
+   DUP
+   ALOAD 5
+   INVOKESPECIAL local/lito381/server/NativeDeDeletionPorts$RemoveObject.<init>(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
+   INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1PcInstance$sendPackets(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$Packet;)V
+   LABEL L13
    GOTO L1
    LABEL L14
-   ALOAD 0
-   ALOAD 5
-   INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1PcInstance$removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
-   ALOAD 0
-   NEW local/lito381/server/NativeDeDeletionPorts$RemoveObject
-   DUP
-   ALOAD 5
-   INVOKESPECIAL local/lito381/server/NativeDeDeletionPorts$RemoveObject.<init>(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
-   INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1PcInstance$sendPackets(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$Packet;)V
-   GOTO L1
+   GOTO L16
    LABEL L15
-   GOTO L17
-   LABEL L16
    ASTORE 1
    INVOKESTATIC local/lito381/server/NativeDeVisibilityScope.log()Llocal/lito381/server/NativeDeWorldLog;
    ALOAD 0
@@ -413,9 +405,9 @@
    INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"視野移除處理異常: \u0001"]
    ALOAD 1
    INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
-   LABEL L17
+   LABEL L16
    RETURN
-tryCatch [TRY L0 L15 L16 java/lang/Exception  ]
+tryCatch [TRY L0 L14 L15 java/lang/Exception  ]
 METHOD updateObject(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
 access 0x9
 signature
```

### NativeDeVisibilitySwitch

```diff
--- NativeDeVisibilitySwitch.left.txt
+++ NativeDeVisibilitySwitch.right.txt
@@ -1,15 +1,15 @@
-access 32
+access 4128
 superName java/lang/Object
 interfaces []
 signature
-outerClass
+outerClass local/lito381/server/NativeDeDeletionPorts$Viewer
 outerMethod
 outerMethodDesc
-nestHostClass
+nestHostClass local/lito381/server/NativeDeDeletionPorts$Viewer
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses local/lito381/server/NativeDeDeletionVisibility$RepairKind|local/lito381/server/NativeDeDeletionVisibility|RepairKind|0x4019; local/lito381/server/NativeDeathLifecycleGate$Phase|local/lito381/server/NativeDeathLifecycleGate|Phase|0x4019
+innerClasses local/lito381/server/NativeDeDeletionVisibility$RepairKind|local/lito381/server/NativeDeDeletionVisibility|RepairKind|0x4019; local/lito381/server/NativeDeVisibilitySwitch|||0x1008; local/lito381/server/NativeDeathLifecycleGate$Phase|com/lineage/server/model/Instance/DeathLifecycleGate|Phase|0x4019
 recordComponents
 FIELD $SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase:[I
 access 0x18
@@ -139,13 +139,3 @@
    LABEL L31
    RETURN
 tryCatch [TRY L0 L1 L2 java/lang/NoSuchFieldError  , TRY L3 L4 L5 java/lang/NoSuchFieldError  , TRY L6 L7 L8 java/lang/NoSuchFieldError  , TRY L9 L10 L11 java/lang/NoSuchFieldError  , TRY L12 L13 L14 java/lang/NoSuchFieldError  , TRY L15 L16 L17 java/lang/NoSuchFieldError  , TRY L18 L19 L20 java/lang/NoSuchFieldError  , TRY L22 L23 L24 java/lang/NoSuchFieldError  , TRY L25 L26 L27 java/lang/NoSuchFieldError  , TRY L28 L29 L30 java/lang/NoSuchFieldError  ]
-METHOD <init>()V
-access 0x0
-signature
-exceptions []
-annotations ||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault
-   ALOAD 0
-   INVOKESPECIAL java/lang/Object.<init>()V
-   RETURN
-tryCatch []
```

### NativeDeVisibilityTimer

```diff
--- NativeDeVisibilityTimer.left.txt
+++ NativeDeVisibilityTimer.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeVisibilityScope$Logs|local/lito381/server/NativeDeVisibilityScope|Logs|0x18
+innerClasses java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
 recordComponents
 FIELD RESTART_COUNT:Ljava/util/concurrent/atomic/AtomicLong;
 access 0x1a
```

### NativeDeWorldContext$Entry

```diff
--- NativeDeWorldContext$Entry.left.txt
+++ NativeDeWorldContext$Entry.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608; local/lito381/server/NativeDeWorldContext$Entry|local/lito381/server/NativeDeWorldContext|Entry|0x18
+innerClasses local/lito381/server/NativeDeWorldContext$Entry|local/lito381/server/NativeDeWorldContext|Entry|0x18
 recordComponents
 FIELD classType:I
 access 0x40
```

### NativeDeWorldContext

```diff
--- NativeDeWorldContext.left.txt
+++ NativeDeWorldContext.right.txt
@@ -9,7 +9,7 @@
 nestMembers [local/lito381/server/NativeDeWorldContext$1, local/lito381/server/NativeDeWorldContext$2, local/lito381/server/NativeDeWorldContext$Entry]
 permittedSubclasses
 annotations |||
-innerClasses java/lang/System$Logger$Level|java/lang/System$Logger|Level|0x4019; java/lang/System$Logger|java/lang/System|Logger|0x609; java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock|java/util/concurrent/locks/ReentrantReadWriteLock|ReadLock|0x9; java/util/concurrent/locks/ReentrantReadWriteLock$WriteLock|java/util/concurrent/locks/ReentrantReadWriteLock|WriteLock|0x9; local/lito381/server/BattleRules$MatchState|local/lito381/server/BattleRules|MatchState|0x18; local/lito381/server/BattleRules$RuleSet|local/lito381/server/BattleRules|RuleSet|0x18; local/lito381/server/NativeDeDeletionPorts$Boss|local/lito381/server/NativeDeDeletionPorts|Boss|0x408; local/lito381/server/NativeDeDeletionPorts$DeWorld|local/lito381/server/NativeDeDeletionPorts|DeWorld|0x408; local/lito381/server/NativeDeDeletionPorts$Log|local/lito381/server/NativeDeDeletionPorts|Log|0x608; local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608; local/lito381/server/NativeDeDeletionPorts$Quest|local/lito381/server/NativeDeDeletionPorts|Quest|0x408; local/lito381/server/NativeDeDeletionPorts$Services|local/lito381/server/NativeDeDeletionPorts|Services|0x608; local/lito381/server/NativeDeDeletionPorts$World|local/lito381/server/NativeDeDeletionPorts|World|0x408; local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x8; local/lito381/server/NativeDeWorldContext$1|||0x0; local/lito381/server/NativeDeWorldContext$2|||0x0; local/lito381/server/NativeDeWorldContext$Entry|local/lito381/server/NativeDeWorldContext|Entry|0x18; local/lito381/server/NativeLitoMapTableCatalog$Entry|local/lito381/server/NativeLitoMapTableCatalog|Entry|0x18; local/lito381/server/NativePlayerCommit$Binding|local/lito381/server/NativePlayerCommit|Binding|0x18
+innerClasses java/lang/System$Logger$Level|java/lang/System$Logger|Level|0x4019; java/lang/System$Logger|java/lang/System|Logger|0x609; java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock|java/util/concurrent/locks/ReentrantReadWriteLock|ReadLock|0x9; java/util/concurrent/locks/ReentrantReadWriteLock$WriteLock|java/util/concurrent/locks/ReentrantReadWriteLock|WriteLock|0x9; local/lito381/server/BattleRules$MatchState|local/lito381/server/BattleRules|MatchState|0x18; local/lito381/server/BattleRules$RuleSet|local/lito381/server/BattleRules|RuleSet|0x18; local/lito381/server/NativeDeDeletionPorts$Boss|local/lito381/server/NativeDeDeletionPorts|Boss|0x408; local/lito381/server/NativeDeDeletionPorts$DeWorld|local/lito381/server/NativeDeDeletionPorts|DeWorld|0x408; local/lito381/server/NativeDeDeletionPorts$Log|local/lito381/server/NativeDeDeletionPorts|Log|0x608; local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608; local/lito381/server/NativeDeDeletionPorts$Quest|local/lito381/server/NativeDeDeletionPorts|Quest|0x408; local/lito381/server/NativeDeDeletionPorts$Services|local/lito381/server/NativeDeDeletionPorts|Services|0x608; local/lito381/server/NativeDeDeletionPorts$World|local/lito381/server/NativeDeDeletionPorts|World|0x408; local/lito381/server/NativeDeInitializerKernel$Name|local/lito381/server/NativeDeInitializerKernel|Name|0x18; local/lito381/server/NativeDeWorldContext$1|||0x0; local/lito381/server/NativeDeWorldContext$2|||0x0; local/lito381/server/NativeDeWorldContext$Entry|local/lito381/server/NativeDeWorldContext|Entry|0x18; local/lito381/server/NativeLitoMapTableCatalog$Entry|local/lito381/server/NativeLitoMapTableCatalog|Entry|0x18; local/lito381/server/NativePlayerCommit$Binding|local/lito381/server/NativePlayerCommit|Binding|0x18
 recordComponents
 FIELD ENTRY:Ljava/lang/String;
 access 0x18
@@ -258,8 +258,8 @@
    ALOAD 0
    GETFIELD local/lito381/server/NativeDeWorldContext.admission:Ljava/util/concurrent/locks/ReentrantReadWriteLock;
    INVOKEVIRTUAL java/util/concurrent/locks/ReentrantReadWriteLock.readLock()Ljava/util/concurrent/locks/ReentrantReadWriteLock$ReadLock;
-   ASTORE 12
-   ALOAD 12
+   ASTORE 10
+   ALOAD 10
    INVOKEVIRTUAL java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock.lock()V
    LABEL L0
    ALOAD 0
@@ -280,8 +280,8 @@
    IFEQ L2
    ALOAD 1
    CHECKCAST local/lito381/server/NativeDeDeletionState
-   ASTORE 14
-   ALOAD 14
+   ASTORE 11
+   ALOAD 11
    GETFIELD local/lito381/server/NativeDeDeletionState.deletionOwner:Llocal/lito381/server/NativeDeDeletionOwner;
    ALOAD 0
    GETFIELD local/lito381/server/NativeDeWorldContext.deletion:Llocal/lito381/server/NativeDeDeletionOwner;
@@ -297,15 +297,13 @@
    IFEQ L4
    ALOAD 1
    CHECKCAST local/lito381/server/NativeDeClientViewer
-   ASTORE 13
-   ALOAD 13
-   CHECKCAST local/lito381/server/NativeDeClientViewer
+   ASTORE 11
+   ALOAD 11
    GETFIELD local/lito381/server/NativeDeClientViewer.life:Llocal/lito381/server/NativePlayerLifetime;
    ALOAD 0
    GETFIELD local/lito381/server/NativeDeWorldContext.life:Llocal/lito381/server/NativePlayerLifetime;
    IF_ACMPNE L3
-   ALOAD 13
-   CHECKCAST local/lito381/server/NativeDeClientViewer
+   ALOAD 11
    GETFIELD local/lito381/server/NativeDeClientViewer.retired:Z
    IFEQ L4
    LABEL L3
@@ -316,20 +314,18 @@
    ATHROW
    LABEL L4
    ALOAD 1
-   ASTORE 13
-   ALOAD 13
-   DUP
-   ASTORE 14
+   DUP
+   ASTORE 11
    MONITORENTER
    LABEL L5
    ALOAD 1
    CONST str:"sourceDeWorldEntry"
    INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
    CHECKCAST local/lito381/server/NativeDeWorldContext$Entry
-   ASTORE 15
-   ALOAD 15
+   ASTORE 12
+   ALOAD 12
    IFNULL L8
-   ALOAD 15
+   ALOAD 12
    GETFIELD local/lito381/server/NativeDeWorldContext$Entry.context:Llocal/lito381/server/NativeDeWorldContext;
    ALOAD 0
    IF_ACMPEQ L6
@@ -339,14 +335,14 @@
    INVOKESPECIAL java/lang/IllegalArgumentException.<init>(Ljava/lang/String;)V
    ATHROW
    LABEL L6
-   ALOAD 15
-   ASTORE 16
    ALOAD 12
-   INVOKEVIRTUAL java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock.unlock()V
-   ALOAD 16
-   ALOAD 14
+   ASTORE 13
+   ALOAD 11
    MONITOREXIT
    LABEL L7
+   ALOAD 10
+   INVOKEVIRTUAL java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock.unlock()V
+   ALOAD 13
    ARETURN
    LABEL L8
    NEW local/lito381/server/NativeDeWorldContext$Entry
@@ -362,50 +358,45 @@
    ILOAD 9
    ALOAD 1
    INVOKESPECIAL local/lito381/server/NativeDeWorldContext$Entry.<init>(Llocal/lito381/server/NativeDeWorldContext;IIIILjava/lang/String;IIZLlocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
-   ASTORE 11
+   ASTORE 13
+   ALOAD 1
+   CONST str:"sourceDeWorldEntry"
+   ALOAD 13
+   INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
+   ALOAD 1
+   INSTANCEOF local/lito381/server/NativeDeDeletionState
+   IFEQ L9
+   ALOAD 1
+   CHECKCAST local/lito381/server/NativeDeDeletionState
+   ASTORE 14
    ALOAD 14
-   MONITOREXIT
-   LABEL L9
-   GOTO L12
-   LABEL L10
-   ASTORE 17
-   ALOAD 14
-   MONITOREXIT
-   LABEL L11
-   ALOAD 17
-   ATHROW
-   LABEL L12
-   GOTO L14
-   LABEL L13
-   ASTORE 13
-   ALOAD 12
-   INVOKEVIRTUAL java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock.unlock()V
-   ALOAD 13
-   ATHROW
-   LABEL L14
-   ALOAD 1
-   CONST str:"sourceDeWorldEntry"
-   ALOAD 11
-   INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
-   ALOAD 1
-   INSTANCEOF local/lito381/server/NativeDeDeletionState
-   IFEQ L15
-   ALOAD 1
-   CHECKCAST local/lito381/server/NativeDeDeletionState
-   ASTORE 10
-   ALOAD 10
-   CHECKCAST local/lito381/server/NativeDeDeletionState
    ILOAD 5
    PUTFIELD local/lito381/server/NativeDeDeletionState.showId:I
-   LABEL L15
+   LABEL L9
+   ALOAD 13
+   ASTORE 14
    ALOAD 11
-   ASTORE 10
-   ALOAD 12
+   MONITOREXIT
+   LABEL L10
+   ALOAD 10
    INVOKEVIRTUAL java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock.unlock()V
+   ALOAD 14
+   ARETURN
+   LABEL L11
+   ASTORE 15
+   ALOAD 11
+   MONITOREXIT
+   LABEL L12
+   ALOAD 15
+   ATHROW
+   LABEL L13
+   ASTORE 16
+   LABEL L14
    ALOAD 10
-   CHECKCAST local/lito381/server/NativeDeWorldContext$Entry
-   ARETURN
-tryCatch [TRY L5 L7 L10 java/lang/Throwable  , TRY L8 L9 L10 java/lang/Throwable  , TRY L10 L11 L10 java/lang/Throwable  , TRY L0 L7 L13 java/lang/Throwable  , TRY L8 L12 L13 java/lang/Throwable  ]
+   INVOKEVIRTUAL java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock.unlock()V
+   ALOAD 16
+   ATHROW
+tryCatch [TRY L5 L7 L11 java/lang/Throwable  , TRY L8 L10 L11 java/lang/Throwable  , TRY L11 L12 L11 java/lang/Throwable  , TRY L0 L7 L13 java/lang/Throwable  , TRY L8 L10 L13 java/lang/Throwable  , TRY L11 L14 L13 java/lang/Throwable  ]
 METHOD bindViewer(Llocal/lito381/server/ClientSession;I)Llocal/lito381/server/NativeDeClientViewer;
 access 0x0
 signature
@@ -429,26 +420,22 @@
    ATHROW
    LABEL L0
    ALOAD 3
+   DUP
    ASTORE 4
-   ALOAD 4
+   MONITORENTER
+   LABEL L1
+   ALOAD 1
+   INVOKESTATIC local/lito381/server/CombatPoisonAtomic.status(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/BattleTimedStatus;
    DUP
    ASTORE 5
    MONITORENTER
-   LABEL L1
-   ALOAD 1
-   INVOKESTATIC local/lito381/server/CombatPoisonAtomic.status(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/BattleTimedStatus;
+   LABEL L2
+   ALOAD 0
+   INVOKEVIRTUAL local/lito381/server/NativeDeWorldContext.admit()V
+   ALOAD 1
+   INVOKESTATIC local/lito381/server/NativeDeClientViewer.of(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/NativeDeClientViewer;
    ASTORE 6
    ALOAD 6
-   DUP
-   ASTORE 7
-   MONITORENTER
-   LABEL L2
-   ALOAD 0
-   INVOKEVIRTUAL local/lito381/server/NativeDeWorldContext.admit()V
-   ALOAD 1
-   INVOKESTATIC local/lito381/server/NativeDeClientViewer.of(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/NativeDeClientViewer;
-   ASTORE 9
-   ALOAD 9
    GETFIELD local/lito381/server/NativeDeClientViewer.binding:Llocal/lito381/server/NativePlayerCommit$Binding;
    INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.phase()Ljava/lang/String;
    CONST str:"WORLD"
@@ -459,16 +446,16 @@
    INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
    CHECKCAST java/lang/Integer
    INVOKEVIRTUAL java/lang/Integer.intValue()I
-   ISTORE 8
+   ISTORE 7
    GOTO L5
    LABEL L3
    ALOAD 3
-   ALOAD 9
+   ALOAD 6
    GETFIELD local/lito381/server/NativeDeClientViewer.binding:Llocal/lito381/server/NativePlayerCommit$Binding;
    INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.room()J
    INVOKESTATIC local/lito381/server/NativeProcHealingEncounter.room(Llocal/lito381/server/GameHub;J)Ljava/lang/Object;
-   ASTORE 10
-   ALOAD 10
+   ASTORE 8
+   ALOAD 8
    IFNONNULL L4
    NEW java/lang/IllegalStateException
    DUP
@@ -476,34 +463,34 @@
    INVOKESPECIAL java/lang/IllegalStateException.<init>(Ljava/lang/String;)V
    ATHROW
    LABEL L4
-   ALOAD 10
+   ALOAD 8
    CONST str:"match"
    INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
    CHECKCAST local/lito381/server/BattleRules$MatchState
    INVOKEVIRTUAL local/lito381/server/BattleRules$MatchState.rules()Llocal/lito381/server/BattleRules$RuleSet;
    INVOKEVIRTUAL local/lito381/server/BattleRules$RuleSet.mapId()I
-   ISTORE 8
+   ISTORE 7
    LABEL L5
    ALOAD 1
    CONST str:"positionX"
    INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
    CHECKCAST java/lang/Integer
    INVOKEVIRTUAL java/lang/Integer.intValue()I
-   ISTORE 10
+   ISTORE 8
    ALOAD 1
    CONST str:"positionY"
    INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
    CHECKCAST java/lang/Integer
    INVOKEVIRTUAL java/lang/Integer.intValue()I
-   ISTORE 11
+   ISTORE 9
    ALOAD 1
    CONST str:"selectedCharacterSummary"
    CONST str:"()Llocal/lito381/server/CharacterSummary;"
    CONST Integer:0
    ANEWARRAY java/lang/Object
    INVOKESTATIC local/lito381/server/CombatAccess.call(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
-   ASTORE 12
-   ALOAD 12
+   ASTORE 10
+   ALOAD 10
    IFNONNULL L6
    NEW java/lang/IllegalStateException
    DUP
@@ -511,7 +498,7 @@
    INVOKESPECIAL java/lang/IllegalStateException.<init>(Ljava/lang/String;)V
    ATHROW
    LABEL L6
-   ALOAD 12
+   ALOAD 10
    CONST str:"type"
    CONST str:"()I"
    CONST Integer:0
@@ -519,21 +506,21 @@
    INVOKESTATIC local/lito381/server/CombatAccess.call(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
    CHECKCAST java/lang/Integer
    INVOKEVIRTUAL java/lang/Integer.intValue()I
-   ISTORE 13
-   ALOAD 0
-   ALOAD 9
-   ILOAD 10
+   ISTORE 11
+   ALOAD 0
+   ALOAD 6
+   ILOAD 8
+   ILOAD 9
+   ILOAD 7
+   ILOAD 2
+   ALOAD 1
+   INVOKEVIRTUAL local/lito381/server/ClientSession.playerName()Ljava/lang/String;
    ILOAD 11
-   ILOAD 8
-   ILOAD 2
-   ALOAD 1
-   INVOKEVIRTUAL local/lito381/server/ClientSession.playerName()Ljava/lang/String;
-   ILOAD 13
    CONST Integer:1
    CONST Integer:1
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldContext.attach(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;IIIILjava/lang/String;IIZ)Llocal/lito381/server/NativeDeWorldContext$Entry;
-   ASTORE 14
-   ALOAD 14
+   ASTORE 12
+   ALOAD 12
    GETFIELD local/lito381/server/NativeDeWorldContext$Entry.showId:I
    ILOAD 2
    IF_ICMPEQ L7
@@ -543,15 +530,15 @@
    INVOKESPECIAL java/lang/IllegalArgumentException.<init>(Ljava/lang/String;)V
    ATHROW
    LABEL L7
-   ALOAD 14
+   ALOAD 12
    ALOAD 1
    INVOKEVIRTUAL local/lito381/server/ClientSession.playerName()Ljava/lang/String;
    PUTFIELD local/lito381/server/NativeDeWorldContext$Entry.name:Ljava/lang/String;
-   ALOAD 14
-   ILOAD 13
+   ALOAD 12
+   ILOAD 11
    PUTFIELD local/lito381/server/NativeDeWorldContext$Entry.classType:I
    ALOAD 0
-   ALOAD 9
+   ALOAD 6
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldContext.publish(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
    IFNE L8
    NEW java/lang/IllegalStateException
@@ -567,21 +554,21 @@
    ANEWARRAY java/lang/Object
    DUP
    CONST Integer:0
-   ALOAD 9
+   ALOAD 6
    AASTORE
    DUP
    CONST Integer:1
-   ILOAD 10
+   ILOAD 8
    INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
    AASTORE
    DUP
    CONST Integer:2
-   ILOAD 11
+   ILOAD 9
    INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
    AASTORE
    DUP
    CONST Integer:3
-   ILOAD 8
+   ILOAD 7
    INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
    AASTORE
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldContext.call(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
@@ -594,27 +581,27 @@
    INVOKESPECIAL java/lang/IllegalStateException.<init>(Ljava/lang/String;)V
    ATHROW
    LABEL L9
-   ALOAD 9
-   ALOAD 7
+   ALOAD 6
+   ALOAD 5
    MONITOREXIT
    LABEL L10
+   ALOAD 4
+   MONITOREXIT
+   LABEL L11
+   ARETURN
+   LABEL L12
+   ASTORE 13
    ALOAD 5
    MONITOREXIT
-   LABEL L11
-   ARETURN
-   LABEL L12
-   ASTORE 15
-   ALOAD 7
-   MONITOREXIT
    LABEL L13
-   ALOAD 15
+   ALOAD 13
    ATHROW
    LABEL L14
-   ASTORE 16
-   ALOAD 5
+   ASTORE 14
+   ALOAD 4
    MONITOREXIT
    LABEL L15
-   ALOAD 16
+   ALOAD 14
    ATHROW
 tryCatch [TRY L2 L10 L12 java/lang/Throwable  , TRY L12 L13 L12 java/lang/Throwable  , TRY L1 L11 L14 java/lang/Throwable  , TRY L12 L15 L14 java/lang/Throwable  ]
 METHOD boss()Llocal/lito381/server/NativeDeDeletionPorts$Boss;
@@ -1433,19 +1420,17 @@
    CHECKCAST java/lang/Boolean
    INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
    ISTORE 3
+   LABEL L1
+   ALOAD 2
+   INVOKEVIRTUAL java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock.unlock()V
    ILOAD 3
-   ISTORE 4
-   LABEL L1
-   ALOAD 2
-   INVOKEVIRTUAL java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock.unlock()V
-   ILOAD 4
    IRETURN
    LABEL L2
-   ASTORE 5
+   ASTORE 4
    LABEL L3
    ALOAD 2
    INVOKEVIRTUAL java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock.unlock()V
-   ALOAD 5
+   ALOAD 4
    ATHROW
 tryCatch [TRY L0 L1 L2 java/lang/Throwable  , TRY L2 L3 L2 java/lang/Throwable  ]
 METHOD quest()Llocal/lito381/server/NativeDeDeletionPorts$Quest;
```

### NativeDeWorldCore$1

```diff
--- NativeDeWorldCore$1.left.txt
+++ NativeDeWorldCore$1.right.txt
@@ -11,7 +11,7 @@
 annotations |||
 innerClasses local/lito381/server/NativeDeWorldCore$1|||0x1008; local/lito381/server/NativeDeWorldCore$SpatialReconcileResult|local/lito381/server/NativeDeWorldCore|SpatialReconcileResult|0x401a
 recordComponents
-FIELD $SwitchMap$local$lito381$server$NativeDeWorldCore$SpatialReconcileResult:[I
+FIELD $SwitchMap$com$lineage$server$world$World$SpatialReconcileResult:[I
 access 0x18
 signature
 value
@@ -25,9 +25,9 @@
    INVOKESTATIC local/lito381/server/NativeDeWorldCore$SpatialReconcileResult.values()[Llocal/lito381/server/NativeDeWorldCore$SpatialReconcileResult;
    ARRAYLENGTH
    NEWARRAY 10
-   PUTSTATIC local/lito381/server/NativeDeWorldCore$1.$SwitchMap$local$lito381$server$NativeDeWorldCore$SpatialReconcileResult:[I
+   PUTSTATIC local/lito381/server/NativeDeWorldCore$1.$SwitchMap$com$lineage$server$world$World$SpatialReconcileResult:[I
    LABEL L0
-   GETSTATIC local/lito381/server/NativeDeWorldCore$1.$SwitchMap$local$lito381$server$NativeDeWorldCore$SpatialReconcileResult:[I
+   GETSTATIC local/lito381/server/NativeDeWorldCore$1.$SwitchMap$com$lineage$server$world$World$SpatialReconcileResult:[I
    GETSTATIC local/lito381/server/NativeDeWorldCore$SpatialReconcileResult.REPAIRED:Llocal/lito381/server/NativeDeWorldCore$SpatialReconcileResult;
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore$SpatialReconcileResult.ordinal()I
    CONST Integer:1
@@ -37,7 +37,7 @@
    LABEL L2
    ASTORE 0
    LABEL L3
-   GETSTATIC local/lito381/server/NativeDeWorldCore$1.$SwitchMap$local$lito381$server$NativeDeWorldCore$SpatialReconcileResult:[I
+   GETSTATIC local/lito381/server/NativeDeWorldCore$1.$SwitchMap$com$lineage$server$world$World$SpatialReconcileResult:[I
    GETSTATIC local/lito381/server/NativeDeWorldCore$SpatialReconcileResult.ALREADY_CONSISTENT:Llocal/lito381/server/NativeDeWorldCore$SpatialReconcileResult;
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore$SpatialReconcileResult.ordinal()I
    CONST Integer:2
@@ -47,7 +47,7 @@
    LABEL L5
    ASTORE 0
    LABEL L6
-   GETSTATIC local/lito381/server/NativeDeWorldCore$1.$SwitchMap$local$lito381$server$NativeDeWorldCore$SpatialReconcileResult:[I
+   GETSTATIC local/lito381/server/NativeDeWorldCore$1.$SwitchMap$com$lineage$server$world$World$SpatialReconcileResult:[I
    GETSTATIC local/lito381/server/NativeDeWorldCore$SpatialReconcileResult.NOT_REGISTERED:Llocal/lito381/server/NativeDeWorldCore$SpatialReconcileResult;
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore$SpatialReconcileResult.ordinal()I
    CONST Integer:3
@@ -57,7 +57,7 @@
    LABEL L8
    ASTORE 0
    LABEL L9
-   GETSTATIC local/lito381/server/NativeDeWorldCore$1.$SwitchMap$local$lito381$server$NativeDeWorldCore$SpatialReconcileResult:[I
+   GETSTATIC local/lito381/server/NativeDeWorldCore$1.$SwitchMap$com$lineage$server$world$World$SpatialReconcileResult:[I
    GETSTATIC local/lito381/server/NativeDeWorldCore$SpatialReconcileResult.FAILED:Llocal/lito381/server/NativeDeWorldCore$SpatialReconcileResult;
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore$SpatialReconcileResult.ordinal()I
    CONST Integer:4
```

### NativeDeWorldCore$VisibleBinding

```diff
--- NativeDeWorldCore$VisibleBinding.left.txt
+++ NativeDeWorldCore$VisibleBinding.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608; local/lito381/server/NativeDeWorldCore$VisibleBinding|local/lito381/server/NativeDeWorldCore|VisibleBinding|0x1a
+innerClasses java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeWorldCore$VisibleBinding|local/lito381/server/NativeDeWorldCore|VisibleBinding|0x1a
 recordComponents object:Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef; sig= anns=| typeAnns=|; mapId:I sig= anns=| typeAnns=|; sectorId:I sig= anns=| typeAnns=|
 FIELD mapId:I
 access 0x12
```

### NativeDeWorldCore

```diff
--- NativeDeWorldCore.left.txt
+++ NativeDeWorldCore.right.txt
@@ -9,7 +9,7 @@
 nestMembers [local/lito381/server/NativeDeWorldCore$1, local/lito381/server/NativeDeWorldCore$SpatialIndexAuditSnapshot, local/lito381/server/NativeDeWorldCore$SpatialReconcileResult, local/lito381/server/NativeDeWorldCore$VisibleBinding]
 permittedSubclasses
 annotations |||
-innerClasses java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; java/util/Map$Entry|java/util/Map|Entry|0x609; local/lito381/server/NativeDeDeletionPorts$Monster|local/lito381/server/NativeDeDeletionPorts|Monster|0x408; local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608; local/lito381/server/NativeDeDeletionPorts$Summon|local/lito381/server/NativeDeDeletionPorts|Summon|0x408; local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408; local/lito381/server/NativeDeWorldCore$1|||0x1008; local/lito381/server/NativeDeWorldCore$SpatialIndexAuditSnapshot|local/lito381/server/NativeDeWorldCore|SpatialIndexAuditSnapshot|0x19; local/lito381/server/NativeDeWorldCore$SpatialReconcileResult|local/lito381/server/NativeDeWorldCore|SpatialReconcileResult|0x401a; local/lito381/server/NativeDeWorldCore$VisibleBinding|local/lito381/server/NativeDeWorldCore|VisibleBinding|0x1a; local/lito381/server/NativeDeWorldKinds$Bow|local/lito381/server/NativeDeWorldKinds|Bow|0x8; local/lito381/server/NativeDeWorldKinds$Doll|local/lito381/server/NativeDeWorldKinds|Doll|0x8; local/lito381/server/NativeDeWorldKinds$Dwarf|local/lito381/server/NativeDeWorldKinds|Dwarf|0x8; local/lito381/server/NativeDeWorldKinds$Effect|local/lito381/server/NativeDeWorldKinds|Effect|0x8; local/lito381/server/NativeDeWorldKinds$Item|local/lito381/server/NativeDeWorldKinds|Item|0x8; local/lito381/server/NativeDeWorldKinds$Merchant|local/lito381/server/NativeDeWorldKinds|Merchant|0x8; local/lito381/server/NativeDeWorldKinds$Pet|local/lito381/server/NativeDeWorldKinds|Pet|0x8; local/lito381/server/NativeDeWorldKinds$Trap|local/lito381/server/NativeDeWorldKinds|Trap|0x8
+innerClasses java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; java/util/Map$Entry|java/util/Map|Entry|0x609; local/lito381/server/NativeDeWorldCore$1|||0x1008; local/lito381/server/NativeDeWorldCore$SpatialIndexAuditSnapshot|local/lito381/server/NativeDeWorldCore|SpatialIndexAuditSnapshot|0x19; local/lito381/server/NativeDeWorldCore$SpatialReconcileResult|local/lito381/server/NativeDeWorldCore|SpatialReconcileResult|0x401a; local/lito381/server/NativeDeWorldCore$VisibleBinding|local/lito381/server/NativeDeWorldCore|VisibleBinding|0x1a
 recordComponents
 FIELD MAX_SECTOR_RING:I
 access 0x1a
@@ -258,10 +258,10 @@
    INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
    INVOKEVIRTUAL java/util/concurrent/ConcurrentHashMap.get(Ljava/lang/Object;)Ljava/lang/Object;
    CHECKCAST local/lito381/server/NativeDeWorldCore$VisibleBinding
-   ASTORE 10
-   ALOAD 10
+   ASTORE 9
+   ALOAD 9
    IFNULL L3
-   ALOAD 10
+   ALOAD 9
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore$VisibleBinding.object()Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;
    ALOAD 7
    IF_ACMPEQ L5
@@ -274,31 +274,31 @@
    IINC var=3 incr=1
    ALOAD 7
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1Object$getMapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S
-   ISTORE 11
+   ISTORE 10
    ALOAD 7
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1Object$getX(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
    ALOAD 7
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1Object$getY(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
    INVOKESTATIC local/lito381/server/NativeDeWorldCore.getSectorId(II)I
-   ISTORE 12
+   ISTORE 11
    ALOAD 0
    GETFIELD local/lito381/server/NativeDeWorldCore._visibleObjects:Ljava/util/concurrent/ConcurrentHashMap;
-   ILOAD 11
-   INVOKESTATIC java/lang/Short.valueOf(S)Ljava/lang/Short;
+   ILOAD 10
+   INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+   INVOKEVIRTUAL java/util/concurrent/ConcurrentHashMap.get(Ljava/lang/Object;)Ljava/lang/Object;
+   CHECKCAST java/util/concurrent/ConcurrentHashMap
+   ASTORE 12
+   ALOAD 0
+   GETFIELD local/lito381/server/NativeDeWorldCore._spatialIndex:Ljava/util/concurrent/ConcurrentHashMap;
+   ILOAD 10
+   INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
    INVOKEVIRTUAL java/util/concurrent/ConcurrentHashMap.get(Ljava/lang/Object;)Ljava/lang/Object;
    CHECKCAST java/util/concurrent/ConcurrentHashMap
    ASTORE 13
-   ALOAD 0
-   GETFIELD local/lito381/server/NativeDeWorldCore._spatialIndex:Ljava/util/concurrent/ConcurrentHashMap;
+   ALOAD 13
+   IFNULL L6
+   ALOAD 13
    ILOAD 11
-   INVOKESTATIC java/lang/Short.valueOf(S)Ljava/lang/Short;
-   INVOKEVIRTUAL java/util/concurrent/ConcurrentHashMap.get(Ljava/lang/Object;)Ljava/lang/Object;
-   CHECKCAST java/util/concurrent/ConcurrentHashMap
-   ASTORE 14
-   ALOAD 14
-   IFNULL L6
-   ALOAD 14
-   ILOAD 12
    INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
    INVOKEVIRTUAL java/util/concurrent/ConcurrentHashMap.get(Ljava/lang/Object;)Ljava/lang/Object;
    CHECKCAST java/util/concurrent/ConcurrentHashMap
@@ -306,29 +306,27 @@
    LABEL L6
    CONST null
    LABEL L7
-   DUP
-   ASTORE 9
-   ASTORE 15
-   ALOAD 10
+   ASTORE 14
+   ALOAD 9
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore$VisibleBinding.mapId()I
+   ILOAD 10
+   IF_ICMPNE L8
+   ALOAD 9
+   INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore$VisibleBinding.sectorId()I
    ILOAD 11
    IF_ICMPNE L8
-   ALOAD 10
-   INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore$VisibleBinding.sectorId()I
-   ILOAD 12
-   IF_ICMPNE L8
-   ALOAD 13
+   ALOAD 12
    IFNULL L8
-   ALOAD 13
+   ALOAD 12
    ALOAD 7
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1Object$getId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
    INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
    INVOKEVIRTUAL java/util/concurrent/ConcurrentHashMap.get(Ljava/lang/Object;)Ljava/lang/Object;
    ALOAD 7
    IF_ACMPNE L8
-   ALOAD 9
+   ALOAD 14
    IFNULL L8
-   ALOAD 9
+   ALOAD 14
    ALOAD 7
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1Object$getId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
    INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
@@ -338,33 +336,33 @@
    LABEL L8
    ALOAD 0
    ALOAD 7
-   ALOAD 10
+   ALOAD 9
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore$VisibleBinding.mapId()I
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore.reconcileVisibleObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;I)Llocal/lito381/server/NativeDeWorldCore$SpatialReconcileResult;
-   ASTORE 16
-   ALOAD 16
+   ASTORE 15
+   ALOAD 15
    GETSTATIC local/lito381/server/NativeDeWorldCore$SpatialReconcileResult.REPAIRED:Llocal/lito381/server/NativeDeWorldCore$SpatialReconcileResult;
    IF_ACMPNE L9
    IINC var=4 incr=1
    ALOAD 0
    CONST str:"full-audit-binding"
    ALOAD 7
+   ILOAD 10
    ILOAD 11
-   ILOAD 12
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore.logBindingRepair(Ljava/lang/String;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;II)V
    GOTO L10
    LABEL L9
-   ALOAD 16
+   ALOAD 15
    GETSTATIC local/lito381/server/NativeDeWorldCore$SpatialReconcileResult.FAILED:Llocal/lito381/server/NativeDeWorldCore$SpatialReconcileResult;
    IF_ACMPNE L10
    ALOAD 0
    ALOAD 7
-   ALOAD 10
+   ALOAD 9
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore$VisibleBinding.mapId()I
-   ALOAD 10
+   ALOAD 9
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore$VisibleBinding.sectorId()I
+   ILOAD 10
    ILOAD 11
-   ILOAD 12
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore.logReconcileFailure(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;IIII)V
    LABEL L10
    ALOAD 8
@@ -372,11 +370,11 @@
    LABEL L11
    GOTO L14
    LABEL L12
-   ASTORE 17
+   ASTORE 16
    ALOAD 8
    MONITOREXIT
    LABEL L13
-   ALOAD 17
+   ALOAD 16
    ATHROW
    LABEL L14
    GOTO L0
@@ -452,7 +450,7 @@
    ALOAD 9
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1Object$getY(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
    INVOKESTATIC local/lito381/server/NativeDeWorldCore.getSectorId(II)I
-   ISTORE 14
+   ISTORE 13
    ILOAD 12
    ILOAD 7
    IF_ICMPNE L24
@@ -460,8 +458,8 @@
    ALOAD 9
    ILOAD 7
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore.reconcileVisibleObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;I)Llocal/lito381/server/NativeDeWorldCore$SpatialReconcileResult;
-   ASTORE 13
-   ALOAD 13
+   ASTORE 14
+   ALOAD 14
    GETSTATIC local/lito381/server/NativeDeWorldCore$SpatialReconcileResult.REPAIRED:Llocal/lito381/server/NativeDeWorldCore$SpatialReconcileResult;
    IF_ACMPNE L21
    IINC var=4 incr=1
@@ -469,19 +467,19 @@
    CONST str:"full-audit-visible"
    ALOAD 9
    ILOAD 12
-   ILOAD 14
+   ILOAD 13
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore.logBindingRepair(Ljava/lang/String;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;II)V
    GOTO L22
    LABEL L21
-   ALOAD 13
+   ALOAD 14
    GETSTATIC local/lito381/server/NativeDeWorldCore$SpatialReconcileResult.FAILED:Llocal/lito381/server/NativeDeWorldCore$SpatialReconcileResult;
    IF_ACMPNE L22
    ALOAD 0
    ALOAD 9
    ILOAD 7
-   ILOAD 14
+   ILOAD 13
    ILOAD 12
-   ILOAD 14
+   ILOAD 13
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore.logReconcileFailure(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;IIII)V
    LABEL L22
    ALOAD 10
@@ -504,8 +502,8 @@
    ALOAD 9
    ILOAD 7
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore.reconcileVisibleObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;I)Llocal/lito381/server/NativeDeWorldCore$SpatialReconcileResult;
-   DUP
-   ASTORE 13
+   ASTORE 14
+   ALOAD 14
    GETSTATIC local/lito381/server/NativeDeWorldCore$SpatialReconcileResult.REPAIRED:Llocal/lito381/server/NativeDeWorldCore$SpatialReconcileResult;
    IF_ACMPNE L26
    IINC var=4 incr=1
@@ -513,19 +511,19 @@
    CONST str:"full-audit-visible"
    ALOAD 9
    ILOAD 12
-   ILOAD 14
+   ILOAD 13
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore.logBindingRepair(Ljava/lang/String;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;II)V
    GOTO L27
    LABEL L26
-   ALOAD 13
+   ALOAD 14
    GETSTATIC local/lito381/server/NativeDeWorldCore$SpatialReconcileResult.FAILED:Llocal/lito381/server/NativeDeWorldCore$SpatialReconcileResult;
    IF_ACMPNE L27
    ALOAD 0
    ALOAD 9
    ILOAD 7
-   ILOAD 14
+   ILOAD 13
    ILOAD 12
-   ILOAD 14
+   ILOAD 13
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore.logReconcileFailure(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;IIII)V
    LABEL L27
    ALOAD 10
@@ -533,11 +531,11 @@
    LABEL L28
    GOTO L31
    LABEL L29
-   ASTORE 18
+   ASTORE 17
    ALOAD 10
    MONITOREXIT
    LABEL L30
-   ALOAD 18
+   ALOAD 17
    ATHROW
    LABEL L31
    GOTO L17
@@ -696,8 +694,8 @@
    ALOAD 12
    ILOAD 7
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore.reconcileVisibleObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;I)Llocal/lito381/server/NativeDeWorldCore$SpatialReconcileResult;
-   DUP
    ASTORE 17
+   ALOAD 17
    GETSTATIC local/lito381/server/NativeDeWorldCore$SpatialReconcileResult.REPAIRED:Llocal/lito381/server/NativeDeWorldCore$SpatialReconcileResult;
    IF_ACMPNE L45
    IINC var=4 incr=1
@@ -725,11 +723,11 @@
    LABEL L47
    GOTO L50
    LABEL L48
-   ASTORE 19
+   ASTORE 18
    ALOAD 13
    MONITOREXIT
    LABEL L49
-   ALOAD 19
+   ALOAD 18
    ATHROW
    LABEL L50
    GOTO L36
@@ -979,7 +977,7 @@
    LABEL L2
    ALOAD 5
    INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-   IFEQ L4
+   IFEQ L7
    ALOAD 5
    INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
    CHECKCAST local/lito381/server/NativeDeDeletionPorts$ObjectRef
@@ -989,7 +987,9 @@
    ALOAD 6
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1Object$getMapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S
    ILOAD 1
-   IF_ICMPNE L2
+   IF_ICMPEQ L3
+   GOTO L2
+   LABEL L3
    ALOAD 0
    GETFIELD local/lito381/server/NativeDeWorldCore._visibleBindings:Ljava/util/concurrent/ConcurrentHashMap;
    ALOAD 6
@@ -997,8 +997,8 @@
    INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
    INVOKEVIRTUAL java/util/concurrent/ConcurrentHashMap.get(Ljava/lang/Object;)Ljava/lang/Object;
    CHECKCAST local/lito381/server/NativeDeWorldCore$VisibleBinding
-   DUP
    ASTORE 7
+   ALOAD 7
    IFNULL L2
    ALOAD 7
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore$VisibleBinding.object()Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;
@@ -1007,18 +1007,20 @@
    ALOAD 7
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore$VisibleBinding.mapId()I
    ILOAD 1
-   IF_ICMPNE L2
-   ALOAD 2
-   IFNULL L3
-   ALOAD 2
-   ALOAD 6
-   INVOKEINTERFACE java/util/function/Predicate.test(Ljava/lang/Object;)Z itf
-   IFNE L3
-   GOTO L2
-   LABEL L3
-   IINC var=4 incr=1
+   IF_ICMPEQ L4
    GOTO L2
    LABEL L4
+   ALOAD 2
+   IFNULL L5
+   ALOAD 2
+   ALOAD 6
+   INVOKEINTERFACE java/util/function/Predicate.test(Ljava/lang/Object;)Z itf
+   IFEQ L6
+   LABEL L5
+   IINC var=4 incr=1
+   LABEL L6
+   GOTO L2
+   LABEL L7
    ILOAD 4
    IRETURN
 tryCatch []
@@ -1055,7 +1057,7 @@
    LABEL L2
    ALOAD 8
    INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-   IFEQ L21
+   IFEQ L20
    ALOAD 8
    INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
    CHECKCAST local/lito381/server/NativeDeDeletionPorts$ObjectRef
@@ -1094,10 +1096,8 @@
    CONST Integer:0
    ISTORE 11
    ALOAD 9
+   DUP
    ASTORE 12
-   ALOAD 12
-   DUP
-   ASTORE 13
    MONITORENTER
    LABEL L5
    ALOAD 0
@@ -1107,43 +1107,43 @@
    INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
    INVOKEVIRTUAL java/util/concurrent/ConcurrentHashMap.get(Ljava/lang/Object;)Ljava/lang/Object;
    CHECKCAST local/lito381/server/NativeDeWorldCore$VisibleBinding
-   ASTORE 14
-   ALOAD 14
+   ASTORE 13
+   ALOAD 13
    IFNULL L6
-   ALOAD 14
+   ALOAD 13
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore$VisibleBinding.object()Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;
    ALOAD 9
    IF_ACMPNE L6
-   ALOAD 14
+   ALOAD 13
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore$VisibleBinding.mapId()I
    ILOAD 1
    IF_ICMPNE L6
-   ALOAD 14
+   ALOAD 13
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore$VisibleBinding.sectorId()I
    ILOAD 6
-   IF_ICMPEQ L15
+   IF_ICMPEQ L14
    LABEL L6
    ALOAD 9
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1Object$getMapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S
-   ISTORE 15
+   ISTORE 14
    ALOAD 9
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1Object$getX(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
    ALOAD 9
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1Object$getY(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
    INVOKESTATIC local/lito381/server/NativeDeWorldCore.getSectorId(II)I
+   ISTORE 15
+   ALOAD 7
+   ALOAD 9
+   INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1Object$getId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
+   INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+   ALOAD 9
+   INVOKEVIRTUAL java/util/concurrent/ConcurrentHashMap.remove(Ljava/lang/Object;Ljava/lang/Object;)Z
    ISTORE 16
-   ALOAD 7
-   ALOAD 9
-   INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1Object$getId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
-   INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
-   ALOAD 9
-   INVOKEVIRTUAL java/util/concurrent/ConcurrentHashMap.remove(Ljava/lang/Object;Ljava/lang/Object;)Z
-   ISTORE 17
-   ILOAD 17
+   ILOAD 16
    IFEQ L8
    INVOKESTATIC local/lito381/server/NativeDeWorldScope.SPATIAL_STALE_ENTRY_COUNT()Ljava/util/concurrent/atomic/AtomicLong;
    INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.incrementAndGet()J
-   LSTORE 18
+   LSTORE 17
    ALOAD 9
    INSTANCEOF local/lito381/server/NativeDeDeletionPorts$Viewer
    IFEQ L7
@@ -1151,8 +1151,8 @@
    INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.incrementAndGet()J
    POP2
    LABEL L7
-   LLOAD 18
-   LLOAD 18
+   LLOAD 17
+   LLOAD 17
    CONST Long:1
    LSUB
    LAND
@@ -1173,18 +1173,18 @@
    INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
    ILOAD 4
    INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+   ILOAD 14
+   INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
    ILOAD 15
-   INVOKESTATIC java/lang/Short.valueOf(S)Ljava/lang/Short;
-   ILOAD 16
    CONST Integer:16
    ISHR
    INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
-   ILOAD 16
+   ILOAD 15
    CONST Integer:65535
    IAND
    I2S
    INVOKESTATIC java/lang/Short.valueOf(S)Ljava/lang/Short;
-   LLOAD 18
+   LLOAD 17
    INVOKESTATIC java/lang/Long.valueOf(J)Ljava/lang/Long;
    INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
    LABEL L8
@@ -1192,73 +1192,71 @@
    ALOAD 9
    ILOAD 1
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore.reconcileVisibleObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;I)Llocal/lito381/server/NativeDeWorldCore$SpatialReconcileResult;
-   ASTORE 18
-   GETSTATIC local/lito381/server/NativeDeWorldCore$1.$SwitchMap$local$lito381$server$NativeDeWorldCore$SpatialReconcileResult:[I
-   ALOAD 18
+   ASTORE 17
+   GETSTATIC local/lito381/server/NativeDeWorldCore$1.$SwitchMap$com$lineage$server$world$World$SpatialReconcileResult:[I
+   ALOAD 17
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore$SpatialReconcileResult.ordinal()I
    IALOAD
-   TABLESWITCH min=1 max=4 default=L13 labels=L9,L10,L10,L12
+   TABLESWITCH min=1 max=4 default=L12 labels=L9,L10,L10,L11
    LABEL L9
    ALOAD 0
    CONST str:"traversal-reconcile"
    ALOAD 9
+   ILOAD 14
    ILOAD 15
+   INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore.logBindingRepair(Ljava/lang/String;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;II)V
+   GOTO L13
+   LABEL L10
    ILOAD 16
-   INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore.logBindingRepair(Ljava/lang/String;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;II)V
-   GOTO L14
-   LABEL L10
-   ILOAD 17
-   IFEQ L11
-   GOTO L14
-   LABEL L11
+   IFNE L13
    INVOKESTATIC local/lito381/server/NativeDeWorldScope.SPATIAL_TRAVERSAL_STALE_READ_COUNT()Ljava/util/concurrent/atomic/AtomicLong;
    INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.incrementAndGet()J
    POP2
-   GOTO L14
-   LABEL L12
+   GOTO L13
+   LABEL L11
    ALOAD 0
    ALOAD 9
    ILOAD 1
    ILOAD 6
+   ILOAD 14
    ILOAD 15
-   ILOAD 16
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore.logReconcileFailure(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;IIII)V
-   GOTO L14
-   LABEL L13
+   GOTO L13
+   LABEL L12
    NEW java/lang/IllegalStateException
    DUP
-   ALOAD 18
+   ALOAD 17
    INVOKESTATIC java/lang/String.valueOf(Ljava/lang/Object;)Ljava/lang/String;
    INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"Unhandled spatial reconcile result: \u0001"]
    INVOKESPECIAL java/lang/IllegalStateException.<init>(Ljava/lang/String;)V
    ATHROW
-   LABEL L14
+   LABEL L13
    CONST Integer:1
    ISTORE 11
+   LABEL L14
+   ALOAD 12
+   MONITOREXIT
    LABEL L15
-   ALOAD 13
+   GOTO L18
+   LABEL L16
+   ASTORE 19
+   ALOAD 12
    MONITOREXIT
-   LABEL L16
-   GOTO L19
    LABEL L17
-   ASTORE 20
-   ALOAD 13
-   MONITOREXIT
+   ALOAD 19
+   ATHROW
    LABEL L18
-   ALOAD 20
-   ATHROW
+   ILOAD 11
+   IFEQ L19
+   GOTO L2
    LABEL L19
-   ILOAD 11
-   IFEQ L20
+   ALOAD 5
+   ALOAD 9
+   INVOKEINTERFACE java/util/function/Consumer.accept(Ljava/lang/Object;)V itf
    GOTO L2
    LABEL L20
-   ALOAD 5
-   ALOAD 9
-   INVOKEINTERFACE java/util/function/Consumer.accept(Ljava/lang/Object;)V itf
-   GOTO L2
-   LABEL L21
-   RETURN
-tryCatch [TRY L5 L16 L17 java/lang/Throwable  , TRY L17 L18 L17 java/lang/Throwable  ]
+   RETURN
+tryCatch [TRY L5 L15 L16 java/lang/Throwable  , TRY L16 L17 L16 java/lang/Throwable  ]
 METHOD findObject(I)Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;
 access 0x1
 signature
@@ -1417,8 +1415,8 @@
    INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
    INVOKEVIRTUAL java/util/concurrent/ConcurrentHashMap.get(Ljava/lang/Object;)Ljava/lang/Object;
    CHECKCAST java/util/concurrent/ConcurrentHashMap
-   DUP
    ASTORE 6
+   ALOAD 6
    IFNULL L2
    ALOAD 6
    INVOKEVIRTUAL java/util/concurrent/ConcurrentHashMap.isEmpty()Z
@@ -1820,9 +1818,9 @@
    ALOAD 0
    ILOAD 6
    ALOAD 4
-   INVOKEVIRTUAL local/lito381/server/NativeDeWorldLocation.getX()I
-   ALOAD 4
-   INVOKEVIRTUAL local/lito381/server/NativeDeWorldLocation.getY()I
+   INVOKEVIRTUAL local/lito381/server/NativeDeWorldPoint.getX()I
+   ALOAD 4
+   INVOKEVIRTUAL local/lito381/server/NativeDeWorldPoint.getY()I
    ALOAD 0
    ILOAD 2
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore.resolveSectorRing(I)I
@@ -1835,7 +1833,7 @@
    LABEL L3
    ALOAD 9
    INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-   IFEQ L15
+   IFEQ L13
    ALOAD 9
    INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
    CHECKCAST local/lito381/server/NativeDeDeletionPorts$ObjectRef
@@ -1874,59 +1872,54 @@
    GOTO L3
    LABEL L8
    ILOAD 2
-   LOOKUPSWITCH default=L13 keys=-1,0 labels=L9,L11
+   LOOKUPSWITCH default=L11 keys=-1,0 labels=L9,L10
    LABEL L9
    ALOAD 4
    ALOAD 10
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1Object$getLocation(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Llocal/lito381/server/NativeDeWorldLocation;
-   INVOKEVIRTUAL local/lito381/server/NativeDeWorldLocation.isInScreen(Llocal/lito381/server/NativeDeWorldPoint;)Z
-   IFNE L10
-   GOTO L3
-   LABEL L10
+   INVOKEVIRTUAL local/lito381/server/NativeDeWorldPoint.isInScreen(Llocal/lito381/server/NativeDeWorldPoint;)Z
+   IFEQ L12
    ALOAD 5
    ALOAD 10
    INVOKEVIRTUAL java/util/ArrayList.add(Ljava/lang/Object;)Z
    POP
-   GOTO L3
-   LABEL L11
+   GOTO L12
+   LABEL L10
    ALOAD 4
    ALOAD 10
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1Object$getLocation(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Llocal/lito381/server/NativeDeWorldLocation;
-   INVOKEVIRTUAL local/lito381/server/NativeDeWorldLocation.isSamePoint(Llocal/lito381/server/NativeDeWorldPoint;)Z
-   IFNE L12
-   GOTO L3
-   LABEL L12
+   INVOKEVIRTUAL local/lito381/server/NativeDeWorldPoint.isSamePoint(Llocal/lito381/server/NativeDeWorldPoint;)Z
+   IFEQ L12
    ALOAD 5
    ALOAD 10
    INVOKEVIRTUAL java/util/ArrayList.add(Ljava/lang/Object;)Z
    POP
-   GOTO L3
-   LABEL L13
+   GOTO L12
+   LABEL L11
    ALOAD 4
    ALOAD 10
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1Object$getLocation(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Llocal/lito381/server/NativeDeWorldLocation;
-   INVOKEVIRTUAL local/lito381/server/NativeDeWorldLocation.getTileLineDistance(Llocal/lito381/server/NativeDeWorldPoint;)I
-   ILOAD 2
-   IF_ICMPLE L14
-   GOTO L3
-   LABEL L14
+   INVOKEVIRTUAL local/lito381/server/NativeDeWorldPoint.getTileLineDistance(Llocal/lito381/server/NativeDeWorldPoint;)I
+   ILOAD 2
+   IF_ICMPGT L12
    ALOAD 5
    ALOAD 10
    INVOKEVIRTUAL java/util/ArrayList.add(Ljava/lang/Object;)Z
    POP
+   LABEL L12
    GOTO L3
-   LABEL L15
-   GOTO L17
-   LABEL L16
+   LABEL L13
+   GOTO L15
+   LABEL L14
    ASTORE 9
    INVOKESTATIC local/lito381/server/NativeDeWorldScope._log()Llocal/lito381/server/NativeDeWorldLog;
    CONST str:"[World] getVisibleObjects 迴圈異常"
    ALOAD 9
    INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
-   LABEL L17
-   ALOAD 5
-   ARETURN
-tryCatch [TRY L2 L15 L16 java/lang/Exception  ]
+   LABEL L15
+   ALOAD 5
+   ARETURN
+tryCatch [TRY L2 L13 L14 java/lang/Exception  ]
 METHOD getVisibleObjects(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
 access 0x1
 signature
@@ -1954,6 +1947,11 @@
    ALOAD 2
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1Object$getLocation(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Llocal/lito381/server/NativeDeWorldLocation;
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldLocation.isInScreen(Llocal/lito381/server/NativeDeWorldPoint;)Z
+   IFEQ L2
+   CONST Integer:1
+   IRETURN
+   LABEL L2
+   CONST Integer:0
    IRETURN
 tryCatch []
 METHOD getVisiblePc(Llocal/lito381/server/NativeDeWorldLocation;)Ljava/util/ArrayList;
@@ -2070,7 +2068,7 @@
    LABEL L1
    ALOAD 7
    INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-   IFEQ L3
+   IFEQ L5
    ALOAD 7
    INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
    CHECKCAST local/lito381/server/NativeDeDeletionPorts$ObjectRef
@@ -2078,25 +2076,28 @@
    ILOAD 5
    ALOAD 8
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1Object$getMapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S
-   IF_ICMPNE L1
+   IF_ICMPEQ L2
+   GOTO L1
+   LABEL L2
    ILOAD 3
    ALOAD 8
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1Object$get_showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
-   IF_ICMPNE L1
+   IF_ICMPEQ L3
+   GOTO L1
+   LABEL L3
    ALOAD 1
    ALOAD 8
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1Object$getLocation(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Llocal/lito381/server/NativeDeWorldLocation;
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldLocation.getTileLineDistance(Llocal/lito381/server/NativeDeWorldPoint;)I
    ILOAD 2
-   IF_ICMPLE L2
-   GOTO L1
-   LABEL L2
+   IF_ICMPGT L4
    ALOAD 4
    ALOAD 8
    INVOKEVIRTUAL java/util/ArrayList.add(Ljava/lang/Object;)Z
    POP
+   LABEL L4
    GOTO L1
-   LABEL L3
+   LABEL L5
    ALOAD 4
    ARETURN
 tryCatch []
@@ -2563,8 +2564,7 @@
    LABEL L2
    ALOAD 5
    CONST Integer:0
-   ALOAD 5
-   CONST Integer:0
+   DUP2
    IALOAD
    CONST Integer:1
    IADD
@@ -2625,45 +2625,47 @@
 annotationDefault
    ALOAD 6
    INSTANCEOF local/lito381/server/NativeDeDeletionPorts$Viewer
-   IFNE L0
-   RETURN
-   LABEL L0
+   IFEQ L0
    ALOAD 6
    CHECKCAST local/lito381/server/NativeDeDeletionPorts$Viewer
    ASTORE 7
+   GOTO L1
+   LABEL L0
+   RETURN
+   LABEL L1
    ALOAD 6
    ALOAD 0
    INVOKEVIRTUAL java/lang/Object.equals(Ljava/lang/Object;)Z
-   IFEQ L1
-   RETURN
-   LABEL L1
+   IFEQ L2
+   RETURN
+   LABEL L2
    ILOAD 1
    ALOAD 6
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1Object$getMapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S
-   IF_ICMPEQ L2
-   RETURN
-   LABEL L2
+   IF_ICMPEQ L3
+   RETURN
+   LABEL L3
    ALOAD 0
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1Object$get_showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
    ALOAD 6
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1Object$get_showId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
-   IF_ICMPEQ L3
-   RETURN
-   LABEL L3
+   IF_ICMPEQ L4
+   RETURN
+   LABEL L4
    ALOAD 2
    ALOAD 6
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1Object$getLocation(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Llocal/lito381/server/NativeDeWorldLocation;
    ILOAD 3
    INVOKESTATIC local/lito381/server/NativeDeWorldCore.matchesNearbyRadius(Llocal/lito381/server/NativeDeWorldPoint;Llocal/lito381/server/NativeDeWorldPoint;I)Z
-   IFNE L4
-   RETURN
-   LABEL L4
+   IFNE L5
+   RETURN
+   LABEL L5
    ALOAD 4
    ALOAD 7
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldPlayerFilter.accepts(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)Z
-   IFNE L5
-   RETURN
-   LABEL L5
+   IFNE L6
+   RETURN
+   LABEL L6
    ALOAD 5
    ALOAD 7
    INVOKEINTERFACE java/util/function/Consumer.accept(Ljava/lang/Object;)V itf
@@ -2677,39 +2679,41 @@
 annotationDefault
    ALOAD 3
    INSTANCEOF local/lito381/server/NativeDeDeletionPorts$Viewer
-   IFNE L0
-   RETURN
-   LABEL L0
+   IFEQ L0
    ALOAD 3
    CHECKCAST local/lito381/server/NativeDeDeletionPorts$Viewer
    ASTORE 4
+   GOTO L1
+   LABEL L0
+   RETURN
+   LABEL L1
    ALOAD 4
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1PcInstance$getOnlineStatus(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
    CONST Integer:2
-   IF_ICMPEQ L1
+   IF_ICMPEQ L2
    ALOAD 4
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1PcInstance$getOnlineStatus(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I
    CONST Integer:3
-   IF_ICMPNE L2
-   LABEL L1
-   RETURN
+   IF_ICMPNE L3
    LABEL L2
+   RETURN
+   LABEL L3
    ILOAD 0
    ALOAD 4
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1PcInstance$getMapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S
-   IF_ICMPEQ L3
-   RETURN
-   LABEL L3
+   IF_ICMPEQ L4
+   RETURN
+   LABEL L4
    ALOAD 1
    ALOAD 4
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1PcInstance$getLocation(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Llocal/lito381/server/NativeDeWorldLocation;
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldLocation.isInScreen(Llocal/lito381/server/NativeDeWorldPoint;)Z
-   IFEQ L4
+   IFEQ L5
    ALOAD 2
    ALOAD 4
    INVOKEVIRTUAL java/util/ArrayList.add(Ljava/lang/Object;)Z
    POP
-   LABEL L4
+   LABEL L5
    RETURN
 tryCatch []
 METHOD lambda$hasVisibleObjectAt$#0([ZIIILjava/util/function/Predicate;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
@@ -2757,33 +2761,41 @@
    ALOAD 6
    ALOAD 0
    INVOKEVIRTUAL java/lang/Object.equals(Ljava/lang/Object;)Z
-   IFNE L1
+   IFEQ L0
+   RETURN
+   LABEL L0
    ILOAD 1
    ALOAD 6
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1Object$getMapId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)S
-   IF_ICMPNE L1
+   IF_ICMPEQ L1
+   RETURN
+   LABEL L1
    ALOAD 0
    ALOAD 6
    INVOKESTATIC local/lito381/server/NativeDeWorldCore.passesShowIdForNearbyQuery(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
-   IFEQ L1
+   IFNE L2
+   RETURN
+   LABEL L2
    ALOAD 2
    ALOAD 6
    INVOKESTATIC local/lito381/server/NativeDeWorldAccess.L1Object$getLocation(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Llocal/lito381/server/NativeDeWorldLocation;
    ILOAD 3
    INVOKESTATIC local/lito381/server/NativeDeWorldCore.matchesNearbyRadius(Llocal/lito381/server/NativeDeWorldPoint;Llocal/lito381/server/NativeDeWorldPoint;I)Z
-   IFEQ L1
-   ALOAD 4
-   IFNULL L0
+   IFNE L3
+   RETURN
+   LABEL L3
+   ALOAD 4
+   IFNULL L4
    ALOAD 4
    ALOAD 6
    INVOKEINTERFACE java/util/function/Predicate.test(Ljava/lang/Object;)Z itf
-   IFEQ L1
-   LABEL L0
+   IFNE L4
+   RETURN
+   LABEL L4
    ALOAD 5
    ALOAD 6
    INVOKEVIRTUAL java/util/ArrayList.add(Ljava/lang/Object;)Z
    POP
-   LABEL L1
    RETURN
 tryCatch []
 METHOD lambda$removeVisibleObject$#0(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;[ZLjava/lang/Integer;Llocal/lito381/server/NativeDeWorldCore$VisibleBinding;)Llocal/lito381/server/NativeDeWorldCore$VisibleBinding;
@@ -2797,7 +2809,11 @@
    ALOAD 4
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore$VisibleBinding.object()Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;
    ALOAD 1
-   IF_ACMPNE L0
+   IF_ACMPEQ L1
+   LABEL L0
+   ALOAD 4
+   ARETURN
+   LABEL L1
    ALOAD 0
    ALOAD 4
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldCore$VisibleBinding.mapId()I
@@ -2815,9 +2831,6 @@
    CONST Integer:1
    BASTORE
    CONST null
-   ARETURN
-   LABEL L0
-   ALOAD 4
    ARETURN
 tryCatch []
 METHOD lambda$sliceForStructuralAccess$#0(Ljava/lang/Integer;)Ljava/util/concurrent/ConcurrentHashMap;
```

### NativeDeWorldKinds$Item$EquipmentTimerLease

```diff
--- NativeDeWorldKinds$Item$EquipmentTimerLease.left.txt
+++ NativeDeWorldKinds$Item$EquipmentTimerLease.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408; local/lito381/server/NativeDeWorldKinds$Item$EquipmentTimerLease|local/lito381/server/NativeDeWorldKinds$Item|EquipmentTimerLease|0x1a; local/lito381/server/NativeDeWorldKinds$Item|local/lito381/server/NativeDeWorldKinds|Item|0x8
+innerClasses local/lito381/server/NativeDeWorldKinds$Item$EquipmentTimerLease|local/lito381/server/NativeDeWorldKinds$Item|EquipmentTimerLease|0x1a
 recordComponents
 FIELD expiryRetryScheduled:Z
 access 0x2
```

### NativeDeWorldKinds$Item$LastStatus

```diff
--- NativeDeWorldKinds$Item$LastStatus.left.txt
+++ NativeDeWorldKinds$Item$LastStatus.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses local/lito381/server/NativeDeWorldKinds$Item$LastStatus|local/lito381/server/NativeDeWorldKinds$Item|LastStatus|0x1; local/lito381/server/NativeDeWorldKinds$Item|local/lito381/server/NativeDeWorldKinds|Item|0x8
+innerClasses local/lito381/server/NativeDeWorldKinds$Item$LastStatus|local/lito381/server/NativeDeWorldKinds$Item|LastStatus|0x1
 recordComponents
 FIELD attrEnchantKind:I
 access 0x1
```

### NativeDeWorldKinds$Item

```diff
--- NativeDeWorldKinds$Item.left.txt
+++ NativeDeWorldKinds$Item.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408; local/lito381/server/NativeDeItemAbility$AbilityType|local/lito381/server/NativeDeItemAbility|AbilityType|0x4019; local/lito381/server/NativeDeItemMetadata$Logs|local/lito381/server/NativeDeItemMetadata|Logs|0x18; local/lito381/server/NativeDeWorldKinds$Item$EquipmentTimerLease|local/lito381/server/NativeDeWorldKinds$Item|EquipmentTimerLease|0x1a; local/lito381/server/NativeDeWorldKinds$Item$LastStatus|local/lito381/server/NativeDeWorldKinds$Item|LastStatus|0x1; local/lito381/server/NativeDeWorldKinds$Item|local/lito381/server/NativeDeWorldKinds|Item|0x8
+innerClasses local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608; local/lito381/server/NativeDeWorldKinds$Item$EquipmentTimerLease|local/lito381/server/NativeDeWorldKinds$Item|EquipmentTimerLease|0x1a; local/lito381/server/NativeDeWorldKinds$Item$LastStatus|local/lito381/server/NativeDeWorldKinds$Item|LastStatus|0x1; local/lito381/server/NativeDeWorldKinds$Item|local/lito381/server/NativeDeWorldKinds|Item|0x8
 recordComponents
 FIELD ACTIVE_COUNT:Ljava/util/concurrent/atomic/AtomicLong;
 access 0x1a
@@ -2097,73 +2097,73 @@
    ASTORE 11
    ALOAD 11
    INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.getHitModifier()I
+   ISTORE 12
+   ALOAD 11
+   INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.getDmgModifier()I
    ISTORE 13
    ALOAD 11
-   INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.getDmgModifier()I
+   INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.getAddHitup()I
    ISTORE 14
    ALOAD 11
-   INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.getAddHitup()I
+   INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.getAddDmgup()I
    ISTORE 15
    ALOAD 11
-   INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.getAddDmgup()I
+   INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.getAddBowHitup()I
    ISTORE 16
    ALOAD 11
-   INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.getAddBowHitup()I
+   INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.getAddBowDmgup()I
    ISTORE 17
-   ALOAD 11
-   INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.getAddBowDmgup()I
-   ISTORE 18
-   ILOAD 13
+   ILOAD 12
    IFNE L22
-   ILOAD 15
+   ILOAD 14
    IFEQ L23
    LABEL L22
    ALOAD 0
    GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
    GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.HIT:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+   ILOAD 12
+   ILOAD 14
+   IADD
+   INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.add(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
+   LABEL L23
+   ILOAD 13
+   IFNE L24
+   ILOAD 15
+   IFEQ L25
+   LABEL L24
+   ALOAD 0
+   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
+   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.DMG:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
    ILOAD 13
    ILOAD 15
    IADD
    INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.add(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
-   LABEL L23
-   ILOAD 14
-   IFNE L24
+   LABEL L25
+   ILOAD 12
+   IFNE L26
    ILOAD 16
-   IFEQ L25
-   LABEL L24
-   ALOAD 0
-   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
-   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.DMG:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
-   ILOAD 14
+   IFEQ L27
+   LABEL L26
+   ALOAD 0
+   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
+   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.BOW_HIT:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+   ILOAD 12
    ILOAD 16
    IADD
    INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.add(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
-   LABEL L25
+   LABEL L27
    ILOAD 13
-   IFNE L26
+   IFNE L28
    ILOAD 17
-   IFEQ L27
-   LABEL L26
-   ALOAD 0
-   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
-   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.BOW_HIT:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+   IFEQ L29
+   LABEL L28
+   ALOAD 0
+   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
+   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.BOW_DMG:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
    ILOAD 13
    ILOAD 17
    IADD
    INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.add(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
-   LABEL L27
-   ILOAD 14
-   IFNE L28
-   ILOAD 18
-   IFEQ L29
-   LABEL L28
-   ALOAD 0
-   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
-   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.BOW_DMG:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
-   ILOAD 14
-   ILOAD 18
-   IADD
-   INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.add(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
    LABEL L29
    GOTO L36
    LABEL L30
@@ -2172,71 +2172,71 @@
    IFEQ L36
    ALOAD 1
    CHECKCAST local/lito381/server/NativeDeArmorTemplate
-   ASTORE 12
-   ALOAD 12
+   ASTORE 11
+   ALOAD 11
    INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getHitModifierByArmor()I
+   ISTORE 12
+   ALOAD 11
+   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getDmgModifierByArmor()I
    ISTORE 13
-   ALOAD 12
-   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getDmgModifierByArmor()I
+   ALOAD 11
+   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getAddHitup()I
    ISTORE 14
-   ALOAD 12
-   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getAddHitup()I
+   ALOAD 11
+   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getAddDmgup()I
    ISTORE 15
-   ALOAD 12
-   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getAddDmgup()I
-   ISTORE 16
-   ILOAD 13
+   ILOAD 12
    IFNE L31
-   ILOAD 15
+   ILOAD 14
    IFEQ L32
    LABEL L31
    ALOAD 0
    GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
    GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.HIT:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+   ILOAD 12
+   ILOAD 14
+   IADD
+   INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.add(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
+   LABEL L32
+   ILOAD 13
+   IFNE L33
+   ILOAD 15
+   IFEQ L34
+   LABEL L33
+   ALOAD 0
+   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
+   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.DMG:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
    ILOAD 13
    ILOAD 15
    IADD
    INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.add(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
-   LABEL L32
-   ILOAD 14
-   IFNE L33
+   LABEL L34
+   ALOAD 11
+   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getBowHitModifierByArmor()I
+   ALOAD 11
+   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getAddBowHitup()I
+   IADD
+   ISTORE 16
    ILOAD 16
-   IFEQ L34
-   LABEL L33
-   ALOAD 0
-   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
-   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.DMG:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
-   ILOAD 14
+   IFEQ L35
+   ALOAD 0
+   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
+   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.BOW_HIT:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
    ILOAD 16
-   IADD
-   INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.add(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
-   LABEL L34
-   ALOAD 12
-   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getBowHitModifierByArmor()I
-   ALOAD 12
-   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getAddBowHitup()I
+   INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.add(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
+   LABEL L35
+   ALOAD 11
+   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getBowDmgModifierByArmor()I
+   ALOAD 11
+   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getAddBowDmgup()I
    IADD
    ISTORE 17
    ILOAD 17
-   IFEQ L35
-   ALOAD 0
-   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
-   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.BOW_HIT:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+   IFEQ L36
+   ALOAD 0
+   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
+   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.BOW_DMG:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
    ILOAD 17
-   INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.add(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
-   LABEL L35
-   ALOAD 12
-   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getBowDmgModifierByArmor()I
-   ALOAD 12
-   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getAddBowDmgup()I
-   IADD
-   ISTORE 18
-   ILOAD 18
-   IFEQ L36
-   ALOAD 0
-   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
-   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.BOW_DMG:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
-   ILOAD 18
    INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.add(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
    LABEL L36
    ALOAD 1
@@ -2343,101 +2343,101 @@
    ASTORE 20
    ALOAD 20
    INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.getAddIgnoreReduction()I
-   ISTORE 22
-   ILOAD 22
+   ISTORE 21
+   ILOAD 21
    IFEQ L45
    ALOAD 0
    GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
    GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.IGNORE_REDUCTION:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
-   ILOAD 22
+   ILOAD 21
    INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.set(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
    LABEL L45
    ALOAD 20
    INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.getAddPvpDmg()I
-   ISTORE 23
-   ILOAD 23
+   ISTORE 22
+   ILOAD 22
    IFEQ L46
    ALOAD 0
    GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
    GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.PVP_DMG:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
-   ILOAD 23
+   ILOAD 22
    INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.set(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
    LABEL L46
    ALOAD 20
    INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.getAddPvpRedux()I
-   ISTORE 24
-   ILOAD 24
+   ISTORE 23
+   ILOAD 23
    IFEQ L47
    ALOAD 0
    GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
    GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.PVP_REDUX:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
-   ILOAD 24
+   ILOAD 23
    INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.set(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
    LABEL L47
    ALOAD 20
    INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.getAddBossDmg()I
-   ISTORE 25
-   ILOAD 25
+   ISTORE 24
+   ILOAD 24
    IFEQ L48
    ALOAD 0
    GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
    GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.DMG_BOSS:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
-   ILOAD 25
+   ILOAD 24
    INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.set(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
    LABEL L48
    ALOAD 20
    INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.getAddBossRedux()I
-   ISTORE 26
-   ILOAD 26
+   ISTORE 25
+   ILOAD 25
    IFEQ L49
    ALOAD 0
    GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
    GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.BOSS_REDUX:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
-   ILOAD 26
+   ILOAD 25
    INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.set(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
    LABEL L49
    ALOAD 20
    INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.getAddDodge()I
-   ISTORE 27
-   ILOAD 27
+   ISTORE 26
+   ILOAD 26
    IFEQ L50
    ALOAD 0
    GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
    GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.DODGE:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
-   ILOAD 27
+   ILOAD 26
    INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.set(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
    LABEL L50
    ALOAD 20
    INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.getAddEr()I
-   ISTORE 28
-   ILOAD 28
+   ISTORE 27
+   ILOAD 27
    IFEQ L51
    ALOAD 0
    GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
    GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.ER:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
-   ILOAD 28
+   ILOAD 27
    INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.set(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
    LABEL L51
    ALOAD 20
    INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.getAddAdenaRate()I
-   ISTORE 29
-   ILOAD 29
+   ISTORE 28
+   ILOAD 28
    IFEQ L52
    ALOAD 0
    GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
    GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.ADENA_RATE:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
-   ILOAD 29
+   ILOAD 28
    INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.set(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
    LABEL L52
    ALOAD 20
    INVOKEVIRTUAL local/lito381/server/NativeDeWeaponTemplate.getAddDropRate()I
-   ISTORE 30
-   ILOAD 30
+   ISTORE 29
+   ILOAD 29
    IFEQ L53
    ALOAD 0
    GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
    GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.DROP_RATE:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
-   ILOAD 30
+   ILOAD 29
    INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.set(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
    LABEL L53
    GOTO L65
@@ -2447,126 +2447,126 @@
    IFEQ L65
    ALOAD 1
    CHECKCAST local/lito381/server/NativeDeArmorTemplate
-   ASTORE 21
-   ALOAD 21
+   ASTORE 20
+   ALOAD 20
    INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getAddIgnoreReduction()I
+   ISTORE 21
+   ILOAD 21
+   IFEQ L55
+   ALOAD 0
+   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
+   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.IGNORE_REDUCTION:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+   ILOAD 21
+   INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.set(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
+   LABEL L55
+   ALOAD 20
+   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getAddPvpRedux()I
    ISTORE 22
    ILOAD 22
-   IFEQ L55
-   ALOAD 0
-   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
-   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.IGNORE_REDUCTION:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+   IFEQ L56
+   ALOAD 0
+   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
+   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.PVP_REDUX:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
    ILOAD 22
    INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.set(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
-   LABEL L55
-   ALOAD 21
-   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getAddPvpRedux()I
+   LABEL L56
+   ALOAD 20
+   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getAddPvpDmg()I
    ISTORE 23
    ILOAD 23
-   IFEQ L56
-   ALOAD 0
-   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
-   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.PVP_REDUX:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+   IFEQ L57
+   ALOAD 0
+   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
+   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.PVP_DMG:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
    ILOAD 23
    INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.set(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
-   LABEL L56
-   ALOAD 21
-   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getAddPvpDmg()I
+   LABEL L57
+   ALOAD 20
+   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getAddBossDmg()I
    ISTORE 24
    ILOAD 24
-   IFEQ L57
-   ALOAD 0
-   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
-   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.PVP_DMG:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+   IFEQ L58
+   ALOAD 0
+   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
+   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.DMG_BOSS:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
    ILOAD 24
    INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.set(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
-   LABEL L57
-   ALOAD 21
-   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getAddBossDmg()I
+   LABEL L58
+   ALOAD 20
+   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getAddBossRedux()I
    ISTORE 25
    ILOAD 25
-   IFEQ L58
-   ALOAD 0
-   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
-   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.DMG_BOSS:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+   IFEQ L59
+   ALOAD 0
+   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
+   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.BOSS_REDUX:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
    ILOAD 25
    INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.set(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
-   LABEL L58
-   ALOAD 21
-   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getAddBossRedux()I
+   LABEL L59
+   ALOAD 1
+   INVOKEVIRTUAL local/lito381/server/NativeDeItemTemplate.getAddMeleeRedux()I
    ISTORE 26
    ILOAD 26
-   IFEQ L59
-   ALOAD 0
-   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
-   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.BOSS_REDUX:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+   IFEQ L60
+   ALOAD 0
+   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
+   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.MELEE_REDUX:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
    ILOAD 26
-   INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.set(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
-   LABEL L59
-   ALOAD 1
-   INVOKEVIRTUAL local/lito381/server/NativeDeItemTemplate.getAddMeleeRedux()I
+   INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.add(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
+   LABEL L60
+   ALOAD 1
+   INVOKEVIRTUAL local/lito381/server/NativeDeItemTemplate.getAddBowRedux()I
    ISTORE 27
    ILOAD 27
-   IFEQ L60
-   ALOAD 0
-   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
-   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.MELEE_REDUX:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+   IFEQ L61
+   ALOAD 0
+   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
+   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.BOW_REDUX:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
    ILOAD 27
    INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.add(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
-   LABEL L60
-   ALOAD 1
-   INVOKEVIRTUAL local/lito381/server/NativeDeItemTemplate.getAddBowRedux()I
+   LABEL L61
+   ALOAD 20
+   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getAddDodge()I
    ISTORE 28
    ILOAD 28
-   IFEQ L61
-   ALOAD 0
-   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
-   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.BOW_REDUX:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+   IFEQ L62
+   ALOAD 0
+   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
+   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.DODGE:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
    ILOAD 28
-   INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.add(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
-   LABEL L61
-   ALOAD 21
-   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getAddDodge()I
+   INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.set(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
+   LABEL L62
+   ALOAD 20
+   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getAddEr()I
    ISTORE 29
    ILOAD 29
-   IFEQ L62
-   ALOAD 0
-   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
-   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.DODGE:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+   IFEQ L63
+   ALOAD 0
+   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
+   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.ER:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
    ILOAD 29
    INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.set(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
-   LABEL L62
-   ALOAD 21
-   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getAddEr()I
+   LABEL L63
+   ALOAD 20
+   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getAddAdenaRate()I
    ISTORE 30
    ILOAD 30
-   IFEQ L63
-   ALOAD 0
-   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
-   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.ER:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+   IFEQ L64
+   ALOAD 0
+   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
+   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.ADENA_RATE:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
    ILOAD 30
    INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.set(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
-   LABEL L63
-   ALOAD 21
-   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getAddAdenaRate()I
+   LABEL L64
+   ALOAD 20
+   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getAddDropRate()I
    ISTORE 31
    ILOAD 31
-   IFEQ L64
-   ALOAD 0
-   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
-   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.ADENA_RATE:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
+   IFEQ L65
+   ALOAD 0
+   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
+   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.DROP_RATE:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
    ILOAD 31
-   INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.set(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
-   LABEL L64
-   ALOAD 21
-   INVOKEVIRTUAL local/lito381/server/NativeDeArmorTemplate.getAddDropRate()I
-   ISTORE 32
-   ILOAD 32
-   IFEQ L65
-   ALOAD 0
-   GETFIELD local/lito381/server/NativeDeWorldKinds$Item._ability:Llocal/lito381/server/NativeDeItemAbility;
-   GETSTATIC local/lito381/server/NativeDeItemAbility$AbilityType.DROP_RATE:Llocal/lito381/server/NativeDeItemAbility$AbilityType;
-   ILOAD 32
    INVOKEVIRTUAL local/lito381/server/NativeDeItemAbility.set(Llocal/lito381/server/NativeDeItemAbility$AbilityType;I)V
    LABEL L65
    RETURN
```

### NativeDeWorldKinds

```diff
--- NativeDeWorldKinds.left.txt
+++ NativeDeWorldKinds.right.txt
@@ -9,7 +9,7 @@
 nestMembers [local/lito381/server/NativeDeWorldKinds$Bow, local/lito381/server/NativeDeWorldKinds$Doll, local/lito381/server/NativeDeWorldKinds$Dwarf, local/lito381/server/NativeDeWorldKinds$Effect, local/lito381/server/NativeDeWorldKinds$Item, local/lito381/server/NativeDeWorldKinds$Item$EquipmentTimerLease, local/lito381/server/NativeDeWorldKinds$Item$LastStatus, local/lito381/server/NativeDeWorldKinds$Merchant, local/lito381/server/NativeDeWorldKinds$Npc, local/lito381/server/NativeDeWorldKinds$Pet, local/lito381/server/NativeDeWorldKinds$Trap]
 permittedSubclasses
 annotations |||
-innerClasses local/lito381/server/NativeDeWorldKinds$Bow|local/lito381/server/NativeDeWorldKinds|Bow|0x8; local/lito381/server/NativeDeWorldKinds$Doll|local/lito381/server/NativeDeWorldKinds|Doll|0x8; local/lito381/server/NativeDeWorldKinds$Dwarf|local/lito381/server/NativeDeWorldKinds|Dwarf|0x8; local/lito381/server/NativeDeWorldKinds$Effect|local/lito381/server/NativeDeWorldKinds|Effect|0x8; local/lito381/server/NativeDeWorldKinds$Item$EquipmentTimerLease|local/lito381/server/NativeDeWorldKinds$Item|EquipmentTimerLease|0x1a; local/lito381/server/NativeDeWorldKinds$Item$LastStatus|local/lito381/server/NativeDeWorldKinds$Item|LastStatus|0x1; local/lito381/server/NativeDeWorldKinds$Item|local/lito381/server/NativeDeWorldKinds|Item|0x8; local/lito381/server/NativeDeWorldKinds$Merchant|local/lito381/server/NativeDeWorldKinds|Merchant|0x8; local/lito381/server/NativeDeWorldKinds$Npc|local/lito381/server/NativeDeWorldKinds|Npc|0x408; local/lito381/server/NativeDeWorldKinds$Pet|local/lito381/server/NativeDeWorldKinds|Pet|0x8; local/lito381/server/NativeDeWorldKinds$Trap|local/lito381/server/NativeDeWorldKinds|Trap|0x8
+innerClasses local/lito381/server/NativeDeWorldKinds$Bow|local/lito381/server/NativeDeWorldKinds|Bow|0x8; local/lito381/server/NativeDeWorldKinds$Doll|local/lito381/server/NativeDeWorldKinds|Doll|0x8; local/lito381/server/NativeDeWorldKinds$Dwarf|local/lito381/server/NativeDeWorldKinds|Dwarf|0x8; local/lito381/server/NativeDeWorldKinds$Effect|local/lito381/server/NativeDeWorldKinds|Effect|0x8; local/lito381/server/NativeDeWorldKinds$Item|local/lito381/server/NativeDeWorldKinds|Item|0x8; local/lito381/server/NativeDeWorldKinds$Merchant|local/lito381/server/NativeDeWorldKinds|Merchant|0x8; local/lito381/server/NativeDeWorldKinds$Npc|local/lito381/server/NativeDeWorldKinds|Npc|0x408; local/lito381/server/NativeDeWorldKinds$Pet|local/lito381/server/NativeDeWorldKinds|Pet|0x8; local/lito381/server/NativeDeWorldKinds$Trap|local/lito381/server/NativeDeWorldKinds|Trap|0x8
 recordComponents
 METHOD <init>()V
 access 0x0
```

### NativeDeWorldLocation

```diff
--- NativeDeWorldLocation.left.txt
+++ NativeDeWorldLocation.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses local/lito381/server/NativeDeObjectScope$Maps|local/lito381/server/NativeDeObjectScope|Maps|0x18
+innerClasses
 recordComponents
 FIELD _map:Llocal/lito381/server/NativeDeWorldMap;
 access 0x44
```

### NativeDeWorldLog

```diff
--- NativeDeWorldLog.left.txt
+++ NativeDeWorldLog.right.txt
@@ -11,28 +11,6 @@
 annotations |||
 innerClasses
 recordComponents
-METHOD debug(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V
-access 0x1
-signature
-exceptions []
-annotations ||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault
-   ALOAD 0
-   CONST str:"debug"
-   ALOAD 1
-   CONST Integer:2
-   ANEWARRAY java/lang/Object
-   DUP
-   CONST Integer:0
-   ALOAD 2
-   AASTORE
-   DUP
-   CONST Integer:1
-   ALOAD 3
-   AASTORE
-   INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.emit(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V itf
-   RETURN
-tryCatch []
 METHOD emit(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V
 access 0x401
 signature
```

### NativeDeWorldPlayerFilter$1

```diff
--- NativeDeWorldPlayerFilter$1.left.txt
+++ NativeDeWorldPlayerFilter$1.right.txt
@@ -11,7 +11,7 @@
 annotations |||
 innerClasses local/lito381/server/NativeDeWorldPlayerFilter$1|||0x1008
 recordComponents
-FIELD $SwitchMap$local$lito381$server$NativeDeWorldPlayerFilter:[I
+FIELD $SwitchMap$com$lineage$server$world$PlayerFilter:[I
 access 0x18
 signature
 value
@@ -25,9 +25,9 @@
    INVOKESTATIC local/lito381/server/NativeDeWorldPlayerFilter.values()[Llocal/lito381/server/NativeDeWorldPlayerFilter;
    ARRAYLENGTH
    NEWARRAY 10
-   PUTSTATIC local/lito381/server/NativeDeWorldPlayerFilter$1.$SwitchMap$local$lito381$server$NativeDeWorldPlayerFilter:[I
+   PUTSTATIC local/lito381/server/NativeDeWorldPlayerFilter$1.$SwitchMap$com$lineage$server$world$PlayerFilter:[I
    LABEL L0
-   GETSTATIC local/lito381/server/NativeDeWorldPlayerFilter$1.$SwitchMap$local$lito381$server$NativeDeWorldPlayerFilter:[I
+   GETSTATIC local/lito381/server/NativeDeWorldPlayerFilter$1.$SwitchMap$com$lineage$server$world$PlayerFilter:[I
    GETSTATIC local/lito381/server/NativeDeWorldPlayerFilter.NONE:Llocal/lito381/server/NativeDeWorldPlayerFilter;
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldPlayerFilter.ordinal()I
    CONST Integer:1
@@ -37,7 +37,7 @@
    LABEL L2
    ASTORE 0
    LABEL L3
-   GETSTATIC local/lito381/server/NativeDeWorldPlayerFilter$1.$SwitchMap$local$lito381$server$NativeDeWorldPlayerFilter:[I
+   GETSTATIC local/lito381/server/NativeDeWorldPlayerFilter$1.$SwitchMap$com$lineage$server$world$PlayerFilter:[I
    GETSTATIC local/lito381/server/NativeDeWorldPlayerFilter.BROADCAST:Llocal/lito381/server/NativeDeWorldPlayerFilter;
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldPlayerFilter.ordinal()I
    CONST Integer:2
@@ -47,7 +47,7 @@
    LABEL L5
    ASTORE 0
    LABEL L6
-   GETSTATIC local/lito381/server/NativeDeWorldPlayerFilter$1.$SwitchMap$local$lito381$server$NativeDeWorldPlayerFilter:[I
+   GETSTATIC local/lito381/server/NativeDeWorldPlayerFilter$1.$SwitchMap$com$lineage$server$world$PlayerFilter:[I
    GETSTATIC local/lito381/server/NativeDeWorldPlayerFilter.ACTIVE_ONLY:Llocal/lito381/server/NativeDeWorldPlayerFilter;
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldPlayerFilter.ordinal()I
    CONST Integer:3
```

### NativeDeWorldPlayerFilter

```diff
--- NativeDeWorldPlayerFilter.left.txt
+++ NativeDeWorldPlayerFilter.right.txt
@@ -9,7 +9,7 @@
 nestMembers [local/lito381/server/NativeDeWorldPlayerFilter$1]
 permittedSubclasses
 annotations |||
-innerClasses local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608; local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408; local/lito381/server/NativeDeWorldPlayerFilter$1|||0x1008
+innerClasses local/lito381/server/NativeDeWorldPlayerFilter$1|||0x1008
 recordComponents
 FIELD $VALUES:[Llocal/lito381/server/NativeDeWorldPlayerFilter;
 access 0x1a
@@ -104,7 +104,7 @@
    CONST Integer:0
    IRETURN
    LABEL L0
-   GETSTATIC local/lito381/server/NativeDeWorldPlayerFilter$1.$SwitchMap$local$lito381$server$NativeDeWorldPlayerFilter:[I
+   GETSTATIC local/lito381/server/NativeDeWorldPlayerFilter$1.$SwitchMap$com$lineage$server$world$PlayerFilter:[I
    ALOAD 0
    INVOKEVIRTUAL local/lito381/server/NativeDeWorldPlayerFilter.ordinal()I
    IALOAD
```

### NativeDeWorldRegistryBow

```diff
--- NativeDeWorldRegistryBow.left.txt
+++ NativeDeWorldRegistryBow.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses local/lito381/server/NativeDeWorldKinds$Bow|local/lito381/server/NativeDeWorldKinds|Bow|0x8
+innerClasses
 recordComponents
 FIELD _isBow:Ljava/util/HashMap;
 access 0x12
```

### NativeDeWorldRegistryCrown

```diff
--- NativeDeWorldRegistryCrown.left.txt
+++ NativeDeWorldRegistryCrown.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408
+innerClasses
 recordComponents
 FIELD _allPlayer:Ljava/util/Collection;
 access 0x2
```

### NativeDeWorldRegistryDarkelf

```diff
--- NativeDeWorldRegistryDarkelf.left.txt
+++ NativeDeWorldRegistryDarkelf.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408
+innerClasses
 recordComponents
 FIELD _allPlayer:Ljava/util/Collection;
 access 0x2
```

### NativeDeWorldRegistryDoll

```diff
--- NativeDeWorldRegistryDoll.left.txt
+++ NativeDeWorldRegistryDoll.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses local/lito381/server/NativeDeWorldKinds$Doll|local/lito381/server/NativeDeWorldKinds|Doll|0x8
+innerClasses
 recordComponents
 FIELD _allDollValues:Ljava/util/Collection;
 access 0x2
```

### NativeDeWorldRegistryDragonKnight

```diff
--- NativeDeWorldRegistryDragonKnight.left.txt
+++ NativeDeWorldRegistryDragonKnight.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408
+innerClasses
 recordComponents
 FIELD _allPlayer:Ljava/util/Collection;
 access 0x2
```

### NativeDeWorldRegistryEffect

```diff
--- NativeDeWorldRegistryEffect.left.txt
+++ NativeDeWorldRegistryEffect.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses local/lito381/server/NativeDeWorldKinds$Effect|local/lito381/server/NativeDeWorldKinds|Effect|0x8
+innerClasses java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19
 recordComponents
 FIELD _allEffValues:Ljava/util/Collection;
 access 0x2
```

### NativeDeWorldRegistryElf

```diff
--- NativeDeWorldRegistryElf.left.txt
+++ NativeDeWorldRegistryElf.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408
+innerClasses
 recordComponents
 FIELD _allPlayer:Ljava/util/Collection;
 access 0x2
```

### NativeDeWorldRegistryIllusionist

```diff
--- NativeDeWorldRegistryIllusionist.left.txt
+++ NativeDeWorldRegistryIllusionist.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408
+innerClasses
 recordComponents
 FIELD _allPlayer:Ljava/util/Collection;
 access 0x2
```

### NativeDeWorldRegistryItem

```diff
--- NativeDeWorldRegistryItem.left.txt
+++ NativeDeWorldRegistryItem.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses local/lito381/server/NativeDeWorldKinds$Item|local/lito381/server/NativeDeWorldKinds|Item|0x8
+innerClasses
 recordComponents
 FIELD _allItemValues:Ljava/util/Collection;
 access 0x2
```

### NativeDeWorldRegistryKnight

```diff
--- NativeDeWorldRegistryKnight.left.txt
+++ NativeDeWorldRegistryKnight.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408
+innerClasses
 recordComponents
 FIELD _allPlayer:Ljava/util/Collection;
 access 0x2
```

### NativeDeWorldRegistryMob

```diff
--- NativeDeWorldRegistryMob.left.txt
+++ NativeDeWorldRegistryMob.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses local/lito381/server/NativeDeDeletionPorts$Monster|local/lito381/server/NativeDeDeletionPorts|Monster|0x408
+innerClasses
 recordComponents
 FIELD _allMobValues:Ljava/util/Collection;
 access 0x2
```

### NativeDeWorldRegistryPet

```diff
--- NativeDeWorldRegistryPet.left.txt
+++ NativeDeWorldRegistryPet.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses local/lito381/server/NativeDeWorldKinds$Pet|local/lito381/server/NativeDeWorldKinds|Pet|0x8
+innerClasses
 recordComponents
 FIELD _allPetValues:Ljava/util/Collection;
 access 0x2
```

### NativeDeWorldRegistryQuest

```diff
--- NativeDeWorldRegistryQuest.left.txt
+++ NativeDeWorldRegistryQuest.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608
+innerClasses com/lineage/server/datatables/QuestInstanceTable$Definition|com/lineage/server/datatables/QuestInstanceTable|Definition|0x19
 recordComponents
 FIELD _isQuest:Ljava/util/concurrent/ConcurrentHashMap;
 access 0x12
```

### NativeDeWorldRegistrySummons

```diff
--- NativeDeWorldRegistrySummons.left.txt
+++ NativeDeWorldRegistrySummons.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses local/lito381/server/NativeDeDeletionPorts$Summon|local/lito381/server/NativeDeDeletionPorts|Summon|0x408
+innerClasses
 recordComponents
 FIELD _allSummonValues:Ljava/util/Collection;
 access 0x2
```

### NativeDeWorldRegistryTrap$TrapCell

```diff
--- NativeDeWorldRegistryTrap$TrapCell.left.txt
+++ NativeDeWorldRegistryTrap$TrapCell.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608; local/lito381/server/NativeDeWorldKinds$Trap|local/lito381/server/NativeDeWorldKinds|Trap|0x8; local/lito381/server/NativeDeWorldRegistryTrap$TrapCell|local/lito381/server/NativeDeWorldRegistryTrap|TrapCell|0x1a
+innerClasses java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeDeWorldRegistryTrap$TrapCell|local/lito381/server/NativeDeWorldRegistryTrap|TrapCell|0x1a
 recordComponents mapId:I sig= anns=| typeAnns=|; x:I sig= anns=| typeAnns=|; y:I sig= anns=| typeAnns=|
 FIELD mapId:I
 access 0x12
```

### NativeDeWorldRegistryTrap

```diff
--- NativeDeWorldRegistryTrap.left.txt
+++ NativeDeWorldRegistryTrap.right.txt
@@ -9,7 +9,7 @@
 nestMembers [local/lito381/server/NativeDeWorldRegistryTrap$TrapCell]
 permittedSubclasses
 annotations |||
-innerClasses java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; java/util/concurrent/ConcurrentHashMap$KeySetView|java/util/concurrent/ConcurrentHashMap|KeySetView|0x9; local/lito381/server/NativeDeDeletionPorts$ObjectRef|local/lito381/server/NativeDeDeletionPorts|ObjectRef|0x608; local/lito381/server/NativeDeWorldKinds$Trap|local/lito381/server/NativeDeWorldKinds|Trap|0x8; local/lito381/server/NativeDeWorldRegistryTrap$TrapCell|local/lito381/server/NativeDeWorldRegistryTrap|TrapCell|0x1a
+innerClasses java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; java/util/concurrent/ConcurrentHashMap$KeySetView|java/util/concurrent/ConcurrentHashMap|KeySetView|0x9; local/lito381/server/NativeDeWorldRegistryTrap$TrapCell|local/lito381/server/NativeDeWorldRegistryTrap|TrapCell|0x1a
 recordComponents
 FIELD _byCell:Ljava/util/concurrent/ConcurrentHashMap;
 access 0x12
```

### NativeDeWorldRegistryWizard

```diff
--- NativeDeWorldRegistryWizard.left.txt
+++ NativeDeWorldRegistryWizard.right.txt
@@ -9,7 +9,7 @@
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses local/lito381/server/NativeDeDeletionPorts$Viewer|local/lito381/server/NativeDeDeletionPorts|Viewer|0x408
+innerClasses
 recordComponents
 FIELD _allPlayer:Ljava/util/Collection;
 access 0x2
```

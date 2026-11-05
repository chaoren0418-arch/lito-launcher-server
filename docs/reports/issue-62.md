# Issue #62／T020-B01 本輪修正與完整驗證

## 結論

**B01 尚待主控裁定。最終為 15 SAME、1 SAME_EFFECTS、6 DIFF。** Java 17 編譯成功，`@AsRecord` CLI 轉換 1 類，產出恰好 22 個 class entry，`missingLeft`、`extraLeft`、`ONLY_LEFT`、`ONLY_RIGHT` 皆空或 0。

本輪依指定裁定補 Blind／Forest／Wind 的 null 檢查，並讓 DollAccepted 各讀取一次 `plan.caster`／`plan.target`。`captureOutput` 移除 throws 的實際編譯失敗，最終保留 `throws IOException` 以供完整批次編譯，該項屬未完成。另有候選欄位讀取次數與裁定文字落差，以及工具仍保留的差異，詳見下文及附錄。

工作目錄為 `D:\lineage開發\3-lito開發\scratch\wt\task-62`，分支為既有 `task/62`。先 fetch 並合併當時最新 main `5089b6bc1a0c5ce421a7d6308cbb5b29970331f2`，合併提交為 `a5dcae363d1a0c59417b1918a7e6f8b2228fc60a`，含 #102、#132、#135、#153、#166。Git worktree 的 fetch／merge 經提權寫入共用 `.git` 中繼資料，任務來源與產物均在本 worktree。

本報告取代前輪狀態。主控指定裁定與 B 類通用規則見 Issue #62。[指定裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/62#issuecomment-6647804482)

## 做了什麼（已驗證）

| 檔案／項目 | 本輪處理 |
|---|---|
| `NativeBlindSceneAction.java` | 先取得 player stream，在建立 proof lambda 前呼叫 `Objects.requireNonNull(c)`，保留 `NativeSourceIndirectPlayerProof.player(c,p)`。 |
| `NativeCompanionForestArea.java` | 同上，`apply` 通過 SAME_EFFECTS，依 B 類規則追加唯一一筆核准。 |
| `NativeCompanionWindAction.java` | 同上。將原 varargs 的 6 格陣列明寫為區域變數，保留候選的陣列配置、stream、null 檢查、filter、各參數求值與反射呼叫順序。此表達方式由本輪選定，完整差異仍列為 DIFF。 |
| `NativeSourceDollAccepted.java` | `casterActor` 與 `targetActor` 各從 plan 讀取一次，供 binding／actor／life 引數使用，依指定要求實作。 |
| `NativeWeaponBreakTargetLifecycle.java` | 實測移除 `throws IOException` 會編譯失敗，恢復宣告並標記 TODO #62，保留原 IOException 直接傳播。 |
| `docs/combat-merge/effects-approved.txt` | 僅新增 `NativeCompanionForestArea#apply`，註明 batch B01、category dup-pop-local、PR #79。 |

17 檔均保有指定 `DECOMPILED` 標頭。CFR 0.152 本輪重新反編譯候選全部 17 個外層類，17 次退出碼均為 0。`NativeSourceCubePlacement$Draft` 維持 `@AsRecord`，轉換後為 SAME。

## 怎麼驗證（已驗證）

| 階段 | SAME | SAME_EFFECTS | DIFF | 說明 |
|---|---:|---:|---:|---|
| 前輪 `4f0d709` 報告 | 13 | 0 | 9 | 歷史結果 |
| 本輪更新工具後、Java 修改前 | 15 | 0 | 7 | `main-baseline-v2.json`，本輪實跑 |
| 本輪 Java 修改後、未套 effects | 15 | 0 | 7 | `ruling-v2.json` |
| 最終重新編譯與核准清單 | 15 | 1 | 6 | `verified.json` |

相較前輪 DIFF 減少 3。工具基線更新使 DollNpc 與 WeaponBreakBattleCompanion 成為 SAME，Forest 的精確核准使它成為 SAME_EFFECTS。Java 修改後其餘類別仍需裁定。

使用 Microsoft JDK `C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot`。javac 使用 `--release 17 -encoding UTF-8`，classpath 依序為候選 JAR、正式 JAR、獨立編譯的 AsRecord 註解 classes、`lib/*`。完整程式與命令收據附於本文。

semantic-diff 從本輪合併的工具 Java 重新編譯，含 ASM 9.7 的 asm／asm-tree／asm-analysis。左側另傳 `--left-classpath candidate-50635f7e.jar`，供 #166 解析外部欄位宣告與參照，左側驗收 JAR 的 22 個 entry 逐項核對。核准檔裁切到 B01，僅含 Forest 一個方法。

AsRecord 與 semantic-diff 使用相同 JDK。程式產物由 staged javac 與 AsRecord CLI 產生。CFR 讀取的候選 class 抽出目錄獨立於驗收輸出。

### JDK 與最終原始輸出

#### `jdk.log`

```text
openjdk version "17.0.17" 2025-10-21 LTS

OpenJDK Runtime Environment Microsoft-12574423 (build 17.0.17+10-LTS)

OpenJDK 64-Bit Server VM Microsoft-12574423 (build 17.0.17+10-LTS, mixed mode, sharing)


exit=0
```

#### `verified-annotation.log`

```text

exit=0
```

#### `verified-javac.log`

```text
Note: Some input files use unchecked or unsafe operations.

Note: Recompile with -Xlint:unchecked for details.


exit=0
```

#### `verified-as-record.log`

```text
as-record: scanned=22 converted=1


exit=0
```

#### `verified-jar.log`

```text

exit=0
```

#### `verified-tool.log`

```text

exit=0
```

#### `verified-semantic.log`

```text
SUMMARY SAME=15 SAME_EFFECTS=1 SAME_LAYOUT=0 DEVIATION=0 DIFF=6 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0 ALLOW_METHODS=0

wrote D:\lineage開發\3-lito開發\scratch\wt\task-62\scratch\issue-62\verified.json

wrote D:\lineage開發\3-lito開發\scratch\wt\task-62\scratch\issue-62\verified.md


exit=0
```

### 22 個 entry 的最終狀態

各名稱均位於 `local/lito381/server/`，檔尾均為 `.class`。

| Entry | 狀態 |
|---|---|
| `NativeBlindSceneAction.class` | DIFF |
| `NativeCompanionForestArea.class` | SAME_EFFECTS |
| `NativeCompanionRefScene.class` | SAME |
| `NativeCompanionWindAction.class` | DIFF |
| `NativeControlledNegativeMaterial.class` | DIFF |
| `NativePairedCastSafety.class` | SAME |
| `NativeSourceBatchTerminal.class` | SAME |
| `NativeSourceCubePlacement.class` | SAME |
| `NativeSourceCubePlacement$Draft.class` | SAME |
| `NativeSourceDollAccepted.class` | DIFF |
| `NativeSourceDollAccepted$Pending.class` | SAME |
| `NativeSourceDollNpc.class` | SAME |
| `NativeSourceDollNpc$Scope.class` | SAME |
| `NativeSourceDollSkills.class` | SAME |
| `NativeSourceDollSkills$View.class` | SAME |
| `NativeSourceDollSlow.class` | SAME |
| `NativeSourceHealingCast.class` | DIFF |
| `NativeSourceHealingCast$Cast.class` | SAME |
| `NativeSourcePendingAdmission.class` | SAME |
| `NativeSummonTerminalLifecycle.class` | SAME |
| `NativeWeaponBreakBattleCompanion.class` | SAME |
| `NativeWeaponBreakTargetLifecycle.class` | DIFF |

### 固定輸入與產物 SHA-256

| 路徑 | SHA-256 |
|---|---|
| `lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar` | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| `lito伺服器/LiTo381-Server/reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `lito伺服器/LiTo381-Server/target/issue-62/dependency/cfr-0.152.jar` | `f686e8f3ded377d7bc87d216a90e9e9512df4156e75b06c655a16648ae8765b2` |
| `lito伺服器/LiTo381-Server/target/issue-62/dependency/asm-analysis-9.7.jar` | `7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f` |
| `lito伺服器/LiTo381-Server/target/issue-62/verified/staged.jar` | `4ba5c47dadda30b4a7f2f1be66c0ca501ed0d453ad7ac19c618eda7e4456b0ea` |
| `scratch/issue-62/verified.json` | `0296981479e9c63eb12d7d3c8a5d4f6f16772a5a9af20fddea5f0887ab659005` |
| `scratch/issue-62/verified-full.diff` | `8b097a82310c3b520922cda036afe085bf9812b9050cc09ccff272eabed7f875` |

## 仍需主控裁定的六個 entry

| Entry | 已驗證差異與處理狀態 |
|---|---|
| `NativeBlindSceneAction` | `companion` 的 stream ASTORE／ALOAD 與候選 DUP／POP 仍不同。候選 proof handle 已由 #166 正規化匹配。暫時 effects 探測也回 DIFF，故未加入核准清單。 |
| `NativeCompanionWindAction` | `publish` 的 varargs 陣列與 stream 區域暫存仍與候選 stack 形狀不同。effects 仍回 DIFF，故未加入核准清單。 |
| `NativeControlledNegativeMaterial` | `cast` 的 try 表仍為 6 段對 2 段，另有 capture boolean 暫存與 local 槽位差異。依本輪裁定保留 Java，完整差異見附錄，需工具／主控確認保護範圍。 |
| `NativeSourceDollAccepted` | 依指示改成每欄讀取一次，但候選在 binding 與 actor/life 兩組引數各有一次 GETFIELD，合計各 2 次。此差異涉及欄位讀取次數，需主控修訂裁定。 |
| `NativeSourceHealingCast` | ready／body 的 ASTORE／ALOAD 暫存及 owned 的 stack 載入位置仍 DIFF，effects 探測亦未通過。依本輪裁定保留 Java。 |
| `NativeWeaponBreakTargetLifecycle` | `captureOutput` Exceptions 屬性仍差 IOException。完整方法比對另顯示輸出 Runnable 的兩條 allMatch 分支各缺一次候選 `Objects.requireNonNull(context)`，導致 lambda 內容識別連帶不同。其餘捕獲排列已由工具處理。 |

### throws 的編譯證據

候選 javap 宣告為 `static boolean captureOutput(ClientSession, byte[], String)`，所呼叫的方法宣告為 `static boolean NativeSourceWeaponFinalFrame.capture(ClientSession, byte[], String) throws IOException`。本輪曾直接移除 staged 的 throws，完整 17 檔 javac 結果如下。此試驗產物未用於最終驗收。

```text
D:\lineage開發\3-lito開發\scratch\wt\task-62\lito伺服器\LiTo381-Server\merge-inputs\50635f7e\staged\local\lito381\server\NativeWeaponBreakTargetLifecycle.java:41: error: unreported exception IOException; must be caught or declared to be thrown

  if(NativeSourceWeaponFinalFrame.capture(receiver,packet,note))return true;

                                         ^

Note: Some input files use unchecked or unsafe operations.

Note: Recompile with -Xlint:unchecked for details.

1 error


exit=1
```


最終恢復可編譯的 `throws IOException`，此決定由本輪採取，須由主控裁定後續表達方式。仍需保留同一例外物件的直接傳播，並滿足候選的 Exceptions 屬性。

### DollAccepted 的候選讀取證據

`physical` 的候選片段如下。`caster.binding()` 之前一次 GETFIELD，actor/life 共用的 DUP 之前又一次 GETFIELD，target 同樣各兩次。本輪實作依使用者指定各讀一次，完整差異留給主控核對。

```text
GETFIELD local/lito381/server/NativeSourceDamageDisplay$Plan.caster:Llocal/lito381/server/NativeSourceDamageDisplay$Actor;
INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.binding()Llocal/lito381/server/NativePlayerCommit$Binding;
ALOAD 8
GETFIELD local/lito381/server/NativeSourceDamageDisplay$Plan.caster:Llocal/lito381/server/NativeSourceDamageDisplay$Actor;
DUP
INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.life()Ljava/lang/Object;
INVOKESTATIC local/lito381/server/NativeSourceAcceptedPlayers.fromActor(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeSourceDamageDisplay$Actor;Ljava/lang/Object;)Llocal/lito381/server/NativeSourceRawDamage$Member;
ASTORE 9
ALOAD 0
ALOAD 8
GETFIELD local/lito381/server/NativeSourceDamageDisplay$Plan.target:Llocal/lito381/server/NativeSourceDamageDisplay$Actor;
INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.binding()Llocal/lito381/server/NativePlayerCommit$Binding;
ALOAD 8
GETFIELD local/lito381/server/NativeSourceDamageDisplay$Plan.target:Llocal/lito381/server/NativeSourceDamageDisplay$Actor;
DUP
INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.life()Ljava/lang/Object;
INVOKESTATIC local/lito381/server/NativeSourceAcceptedPlayers.fromActor(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeSourceDamageDisplay$Actor;Ljava/lang/Object;)Llocal/lito381/server/NativeSourceRawDamage$Member;
ASTORE 10
```


## B 類核准與探測

唯一追加的方法為 `NativeCompanionForestArea#apply(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleSpellCatalog$AreaSilenceSpell;IIIIJ)V`。完整正規化差異在附錄 C 的 Forest 區段，共 76 行，涉及 stream 暫存、context 的 DUP／POP 與後續本機槽位編號。呼叫目標與順序、欄位操作、常數、分支及 try／monitor 結構均通過工具的核准比較，最終逐方法結果為 SAME_EFFECTS。

Blind、Wind、Healing 曾用 `scratch/issue-62/trial-effects.txt` 做探測，皆回 DIFF，正式核准檔僅保留通過的 Forest。探測清單與最終核准清單已分開保存。

## 推測

Blind、Wind、Healing 的剩餘差異可能受 #166 保守的暫存／資料流規則限制。現有證據只確認正規化指令差異與工具結果，需主控或工具任務確認能否安全擴充。ControlledNegativeMaterial 的 try 範圍需另行核對。

## 待驗證與給下一棒的提示

1. Claude 主控接手 `needs:claude`，核對上述六個 entry，優先裁定 throws 表達與 DollAccepted 讀取次數落差。
2. TargetLifecycle 輸出 Runnable 的兩次 null 檢查也需納入後續修正，完整證據已列在附錄 C，後續應保持短路條件與 monitor 範圍。
3. 工具負責人可使用附錄程式重建全批資料，針對尚未正規化的 stack／local 與 try 表差異判斷。取得裁定後，由本機執行器在同一分支重驗全部 22 個 entry。
4. 單元測試與行為回歸依任務留到 B-land 統一處理。實機驗收由使用者在該階段執行。

## 產出路徑與範圍核對

- 來源位於 `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/`，本輪修改上述五檔。
- 核准條目位於 `docs/combat-merge/effects-approved.txt`，報告位於 `docs/reports/issue-62.md`。
- 最終 classes 與 JAR 位於 `lito伺服器/LiTo381-Server/target/issue-62/verified/`。
- 原始日誌、22-entry 清單、JSON、Markdown、完整正規化差異與再現腳本位於 `scratch/issue-62/`。CFR 輸出位於其中的 `cfr/`。
- 提交差異範圍檢查以 main 合併提交為基線。`src/main`、`pom.xml` 與 `docs/REPORT.md` 的歷史差異來自明確要求的 main 合併，任務提交只含五檔 staged、核准檔與此報告。執行範圍為離線編譯與 class 分析。

## 附錄 A　完整再現程式與指令

下列程式即本輪 scratch 的實際內容。Python 位置為 `C:/Users/CHAOREN/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe`。在 repo 根目錄將對應區塊存到標示路徑後，使用新的驗證名稱即可從乾淨輸出重跑，`exist_ok=False` 防止沿用過期 class。AsRecord 的依賴由工具自行驗證 SHA-256，asm-analysis 的雜湊在腳本中驗證。工具編譯已按 #166 補入 asm-analysis，初次沿用前輪兩個 ASM 依賴的腳本編譯失敗已修正。

```powershell
& 'C:/Users/CHAOREN/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 scratch/issue-62/verify.py reproduce convert
& 'C:/Users/CHAOREN/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 scratch/issue-62/dump.py reproduce
```

### `scratch/issue-62/verify.py`

```python
import os, sys, json, subprocess, pathlib, hashlib, zipfile, urllib.request
ROOT=pathlib.Path(__file__).resolve().parents[2]
SERVER=ROOT/'lito伺服器/LiTo381-Server'
WORK=ROOT/'scratch/issue-62'
TARGET=SERVER/'target/issue-62'
JDK=pathlib.Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot')
os.environ['JAVA_HOME']=str(JDK)
os.environ['AS_RECORD_MAVEN_REPO']='D:/dev-cache/maven/repository'
os.environ['PYTHONDONTWRITEBYTECODE']='1'
sys.path.insert(0,str(SERVER/'tools/as-record'))
import as_record
BATCH=next(b for b in json.loads((ROOT/'docs/combat-merge/batches.json').read_text('utf-8'))['batches'] if b['id']=='B01')
STAGED=SERVER/'merge-inputs/50635f7e/staged/local/lito381/server'
TARGET.mkdir(parents=True,exist_ok=True)
def run(args,log):
    p=subprocess.run([str(x) for x in args],cwd=SERVER,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    out=p.stdout.decode('utf-8',errors='replace')
    (WORK/log).write_text(out+'\nexit='+str(p.returncode)+'\n',encoding='utf-8')
    with (WORK/'commands.jsonl').open('a', encoding='utf-8') as receipt:
        receipt.write(json.dumps({'cwd':str(SERVER),'argv':[str(x) for x in args], 'log':log,'exit':p.returncode}, ensure_ascii=False)+'\n')
    print(log+': '+out+'exit='+str(p.returncode),flush=True)
    if p.returncode: raise SystemExit(p.returncode)
    return out
def java(*args): return [JDK/'bin/java.exe','-Dfile.encoding=UTF-8',*args]
def javac(*args): return [JDK/'bin/javac.exe','-J-Dfile.encoding=UTF-8','--release','17','-encoding','UTF-8',*args]

def semantic_dependencies():
    analysis = TARGET/'dependency/asm-analysis-9.7.jar'
    if not analysis.exists():
        analysis.write_bytes(urllib.request.urlopen('https://repo.maven.apache.org/maven2/org/ow2/asm/asm-analysis/9.7/asm-analysis-9.7.jar', timeout=45).read())
    assert hashlib.sha256(analysis.read_bytes()).hexdigest() == '7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f'
    return as_record.dependencies()+';'+str(analysis)
def verify(name,convert=False):
    for file,expected in [('candidate-50635f7e.jar','50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729'),('base-0.6.71.jar','e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498')]:
        assert hashlib.sha256((SERVER/'reference'/file).read_bytes()).hexdigest()==expected,file
    dest=TARGET/name/'classes'
    dest.mkdir(parents=True,exist_ok=False)
    annotation=TARGET/'annotation'
    annotation.mkdir(exist_ok=True)
    run(javac('-d',annotation,SERVER/'src/main/java/local/lito381/server/AsRecord.java'),name+'-annotation.log')
    cp=';'.join(str(x) for x in [SERVER/'reference/candidate-50635f7e.jar',SERVER/'reference/base-0.6.71.jar',annotation,SERVER/'lib/*'])
    run(javac('-cp',cp,'-d',dest,*[STAGED/(c+'.java') for c in BATCH['classes']]),name+'-javac.log')
    if convert: run([sys.executable,'-B',SERVER/'tools/as-record/as_record.py',dest],name+'-as-record.log')
    entries=sorted(p.relative_to(dest).as_posix() for p in dest.rglob('*.class'))
    assert entries==sorted(BATCH['verify_entries']),entries
    (WORK/'entries.txt').write_text('\n'.join(BATCH['verify_entries'])+'\n',encoding='utf-8')
    jar=TARGET/name/'staged.jar'
    run([JDK/'bin/jar.exe','--create','--file',jar,'--no-manifest','-C',dest,'.'],name+'-jar.log')
    cp=semantic_dependencies()
    sd=SERVER/'tools/semantic-diff/target/classes'
    sd.mkdir(parents=True,exist_ok=True)
    run(javac('-cp',cp,'-d',sd,*sorted((SERVER/'tools/semantic-diff/src/main/java').rglob('*.java'))),name+'-tool.log')
    args=java('-cp',str(sd)+';'+cp,'local.lito381.semanticdiff.SemanticDiff','--left',jar,'--right',SERVER/'reference/candidate-50635f7e.jar','--left-classpath',SERVER/'reference/candidate-50635f7e.jar','--classes',WORK/'entries.txt','--json',WORK/(name+'.json'),'--md',WORK/(name+'.md'))
    for flag,file in [('effects-methods','effects-approved'),('extra-methods','extra-methods-approved'),('metadata-exempt','metadata-exempt')]:
        lines=[line for line in (ROOT/('docs/combat-merge/'+file+'.txt')).read_text('utf-8-sig').splitlines() if line.strip() and not line.lstrip().startswith('#') and line.split('#')[0].strip()+'.class' in BATCH['verify_entries']]
        if lines:
            selected=WORK/(file+'.txt'); selected.write_text('\n'.join(lines)+'\n',encoding='utf-8');args.extend(['--'+flag,selected])
    run(args,name+'-semantic.log')
    result=json.loads((WORK/(name+'.json')).read_text('utf-8'))
    print(json.dumps({'counts':result['counts'],'selection':result['classSelection']},ensure_ascii=False),flush=True)
if __name__=='__main__':
    run([JDK/'bin/java.exe','-version'],'jdk.log')
    verify(sys.argv[1],len(sys.argv)>2)
```

### `scratch/issue-62/decompile.py`

```python
from verify import *
cfr=TARGET/'dependency/cfr-0.152.jar'; cfr.parent.mkdir(exist_ok=True)
if not cfr.exists(): cfr.write_bytes(urllib.request.urlopen('https://repo.maven.apache.org/maven2/org/benf/cfr/0.152/cfr-0.152.jar',timeout=45).read())
print('CFR SHA256',hashlib.sha256(cfr.read_bytes()).hexdigest(),flush=True)
source=SERVER/'reference/candidate-50635f7e.jar'
with zipfile.ZipFile(source) as z:
    for entry in BATCH['verify_entries']:
        out=TARGET/'candidate-classes'/entry; out.parent.mkdir(parents=True,exist_ok=True);out.write_bytes(z.read(entry))
for name in BATCH['classes']:
    run(java('-jar',cfr,TARGET/('candidate-classes/local/lito381/server/'+name+'.class'),'--extraclasspath',source,'--outputdir',WORK/'cfr','--silent','true'),name+'-cfr.log')
```

### `scratch/issue-62/DumpNormalized.java`

```java
package local.lito381.semanticdiff;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import org.objectweb.asm.tree.*;
public class DumpNormalized {
 static Object call(String name,Class<?>[] types,Object... args)throws Exception {var m=ClassComparer.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(null,args);}
 static String dump(ClassNode n,JarIndex j)throws Exception {
  var out=new StringBuilder();
  out.append("CLASS ").append(n.name).append(" access=").append(n.access & ~32).append(" super=").append(n.superName).append(" signature=").append(n.signature).append(" interfaces=").append(n.interfaces).append('\n');
  out.append("outer=").append(n.outerClass).append('#').append(n.outerMethod).append(n.outerMethodDesc).append(" nestHost=").append(n.nestHostClass).append(" nestMembers=").append(n.nestMembers==null?null:new TreeSet<>(n.nestMembers)).append(" permitted=").append(n.permittedSubclasses==null?null:new TreeSet<>(n.permittedSubclasses)).append('\n');
  out.append("annotations=").append(call("annotations",new Class<?>[]{ClassNode.class},n)).append('\n');
  out.append("innerClasses=").append(call("innerClasses",new Class<?>[]{ClassNode.class,boolean.class},n,false)).append('\n');
  out.append("records=").append(call("records",new Class<?>[]{ClassNode.class,JarIndex.class},n,j)).append('\n');
  var fields=new TreeMap<String,FieldNode>();for(var f:n.fields)fields.put(f.name+f.desc,f);
  for(var f:fields.values())out.append("FIELD ").append(f.name).append(f.desc).append(" access=").append(f.access & ~4096).append(" signature=").append(f.signature).append(" value=").append(call("value",new Class<?>[]{Object.class},f.value)).append(" annotations=").append(call("fieldAnns",new Class<?>[]{FieldNode.class},f)).append('\n');
  var methods=new TreeMap<String,MethodNode>();for(var m:n.methods)methods.put(j.remapMethod(n.name,m.name,m.desc)+m.desc,m);
  for(var e:methods.entrySet()) {var m=e.getValue();out.append("\nMETHOD ").append(e.getKey()).append(" access=").append(m.access & ~4096).append(" signature=").append(m.signature).append(" exceptions=").append(new TreeSet<>(m.exceptions)).append('\n');
   var labels=call("labelIds",new Class<?>[]{MethodNode.class},m);
   out.append("annotations=").append(call("methodAnns",new Class<?>[]{MethodNode.class,Map.class,JarIndex.class},m,labels,j)).append(" default=").append(call("defaultValue",new Class<?>[]{Object.class,JarIndex.class},m.annotationDefault,j)).append('\n');
   for(var line:(List<?>)call("tryCatch",new Class<?>[]{MethodNode.class,JarIndex.class},m,j))out.append(line).append('\n');
   for(var line:ClassComparer.instructions(m,j))out.append(line).append('\n');
  }
  return out.toString();
 }
 public static void main(String[] args)throws Exception {var j=JarIndex.load(Path.of(args[0]));j.addClasspath(Path.of(args[3]));var dest=Path.of(args[2]);Files.createDirectories(dest);for(var line:Files.readAllLines(Path.of(args[1]))){var name=line.trim().replace(".class","");if(!name.isBlank())Files.writeString(dest.resolve(name.substring(name.lastIndexOf('/')+1)+".txt"),dump(j.readTree(name),j));}}
}
```

### `scratch/issue-62/dump.py`

```python
from verify import *
import difflib
name=sys.argv[1]
cp=str(SERVER/'tools/semantic-diff/target/classes')+';'+semantic_dependencies()
out=TARGET/'dump-tool';out.mkdir(exist_ok=True)
run(javac('-cp',cp,'-d',out,WORK/'DumpNormalized.java'),name+'-dump-build.log')
for side,jar in [('left',TARGET/name/'staged.jar'),('right',SERVER/'reference/candidate-50635f7e.jar')]:
    run(java('-cp',str(out)+';'+cp,'local.lito381.semanticdiff.DumpNormalized',jar,WORK/'entries.txt',WORK/name/side,SERVER/'reference/candidate-50635f7e.jar'),name+'-dump-'+side+'.log')
all_diff=[]
for e in BATCH['verify_entries']:
    f=e.split('/')[-1].replace('.class','.txt')
    a=(WORK/name/'left'/f).read_text('utf-8').splitlines(True);b=(WORK/name/'right'/f).read_text('utf-8').splitlines(True)
    diff=''.join(difflib.unified_diff(a,b,fromfile='staged/'+f,tofile='candidate/'+f,n=3))
    if diff:
        (WORK/name/(f+'.diff')).write_text(diff,encoding='utf-8');all_diff.append(diff);print(f,len(diff.splitlines()))
(WORK/(name+'-full.diff')).write_text('\n'.join(all_diff),encoding='utf-8')
```


## 附錄 B　最終實際命令收據

每列含 cwd、argv、輸出日誌與退出碼。完整日誌的主要輸出已附於本文，所有編譯及工具命令均為 exit 0。批次是否通過仍依 JSON 各狀態判斷。

```jsonl
{"cwd": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server", "argv": ["C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\javac.exe", "-J-Dfile.encoding=UTF-8", "--release", "17", "-encoding", "UTF-8", "-d", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\annotation", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\src\\main\\java\\local\\lito381\\server\\AsRecord.java"], "log": "verified-annotation.log", "exit": 0}
{"cwd": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server", "argv": ["C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\javac.exe", "-J-Dfile.encoding=UTF-8", "--release", "17", "-encoding", "UTF-8", "-cp", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\reference\\candidate-50635f7e.jar;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\reference\\base-0.6.71.jar;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\annotation;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\lib\\*", "-d", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\verified\\classes", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeBlindSceneAction.java", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeCompanionForestArea.java", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeCompanionRefScene.java", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeCompanionWindAction.java", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeControlledNegativeMaterial.java", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativePairedCastSafety.java", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeSourceBatchTerminal.java", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeSourceCubePlacement.java", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeSourceDollAccepted.java", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeSourceDollNpc.java", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeSourceDollSkills.java", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeSourceDollSlow.java", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeSourceHealingCast.java", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeSourcePendingAdmission.java", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeSummonTerminalLifecycle.java", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeWeaponBreakBattleCompanion.java", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\merge-inputs\\50635f7e\\staged\\local\\lito381\\server\\NativeWeaponBreakTargetLifecycle.java"], "log": "verified-javac.log", "exit": 0}
{"cwd": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server", "argv": ["C:\\Users\\CHAOREN\\.cache\\codex-runtimes\\codex-primary-runtime\\dependencies\\python\\python.exe", "-B", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\as-record\\as_record.py", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\verified\\classes"], "log": "verified-as-record.log", "exit": 0}
{"cwd": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server", "argv": ["C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\jar.exe", "--create", "--file", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\verified\\staged.jar", "--no-manifest", "-C", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\verified\\classes", "."], "log": "verified-jar.log", "exit": 0}
{"cwd": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server", "argv": ["C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\javac.exe", "-J-Dfile.encoding=UTF-8", "--release", "17", "-encoding", "UTF-8", "-cp", "D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm\\9.7\\asm-9.7.jar;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm-tree\\9.7\\asm-tree-9.7.jar;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\dependency\\asm-analysis-9.7.jar", "-d", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\target\\classes", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\ClassComparer.java", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\FourthNormalizations.java", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\JarIndex.java", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\LayoutComparison.java", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\LocalTransport.java", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\LocalWebs.java", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\Normalizations.java", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\SemanticDiff.java", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\TransportComparison.java"], "log": "verified-tool.log", "exit": 0}
{"cwd": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server", "argv": ["C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\java.exe", "-Dfile.encoding=UTF-8", "-cp", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\target\\classes;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm\\9.7\\asm-9.7.jar;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm-tree\\9.7\\asm-tree-9.7.jar;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\dependency\\asm-analysis-9.7.jar", "local.lito381.semanticdiff.SemanticDiff", "--left", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\verified\\staged.jar", "--right", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\reference\\candidate-50635f7e.jar", "--left-classpath", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\reference\\candidate-50635f7e.jar", "--classes", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\scratch\\issue-62\\entries.txt", "--json", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\scratch\\issue-62\\verified.json", "--md", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\scratch\\issue-62\\verified.md", "--effects-methods", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\scratch\\issue-62\\effects-approved.txt"], "log": "verified-semantic.log", "exit": 0}
{"cwd": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server", "argv": ["C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\javac.exe", "-J-Dfile.encoding=UTF-8", "--release", "17", "-encoding", "UTF-8", "-cp", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\target\\classes;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm\\9.7\\asm-9.7.jar;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm-tree\\9.7\\asm-tree-9.7.jar;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\dependency\\asm-analysis-9.7.jar", "-d", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\dump-tool", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\scratch\\issue-62\\DumpNormalized.java"], "log": "verified-dump-build.log", "exit": 0}
{"cwd": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server", "argv": ["C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\java.exe", "-Dfile.encoding=UTF-8", "-cp", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\dump-tool;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\target\\classes;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm\\9.7\\asm-9.7.jar;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm-tree\\9.7\\asm-tree-9.7.jar;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\dependency\\asm-analysis-9.7.jar", "local.lito381.semanticdiff.DumpNormalized", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\verified\\staged.jar", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\scratch\\issue-62\\entries.txt", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\scratch\\issue-62\\verified\\left", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\reference\\candidate-50635f7e.jar"], "log": "verified-dump-left.log", "exit": 0}
{"cwd": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server", "argv": ["C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\java.exe", "-Dfile.encoding=UTF-8", "-cp", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\dump-tool;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\target\\classes;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm\\9.7\\asm-9.7.jar;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm-tree\\9.7\\asm-tree-9.7.jar;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\dependency\\asm-analysis-9.7.jar", "local.lito381.semanticdiff.DumpNormalized", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\reference\\candidate-50635f7e.jar", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\scratch\\issue-62\\entries.txt", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\scratch\\issue-62\\verified\\right", "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\reference\\candidate-50635f7e.jar"], "log": "verified-dump-right.log", "exit": 0}
```


## 附錄 C　全部正規化差異

以下為全部 22 entries 的 class／field／method metadata、try 表與完整指令序列 dump 所產生的 unified diff，左側 staged、右側候選。使用工具本身的 JarIndex、ClassComparer 與正規化，僅讀取驗收 JAR。每一個差異 hunk 均完整保留，本文展示時移除行尾空白，SHA-256 對應 scratch 原始檔。

此線性 dump 位於額外 transport 證明與 effects 摘要前，因此包含最終為 SAME 的 WeaponBreakBattleCompanion，以及最終為 SAME_EFFECTS 的 Forest。最終判定以附錄 D 與主表為準。內容雜湊不同的 lambda 會同時列出兩個本體，並可能因排序出現跨方法 hunk。

```diff
--- staged/NativeBlindSceneAction.txt
+++ candidate/NativeBlindSceneAction.txt
@@ -402,28 +402,26 @@
 ALOAD 0
 GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.players:Ljava/util/List;
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
-ASTORE 24
-ALOAD 0
+ALOAD 0
+DUP
 INVOKESTATIC java/util/Objects.requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
 POP
-ALOAD 24
-ALOAD 0
 INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeSourceIndirectPlayerProof.player(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
 INVOKEINTERFACE java/util/stream/Stream.filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream; itf
 INVOKEINTERFACE java/util/stream/Stream.toList()Ljava/util/List; itf
+ASTORE 24
+ALOAD 24
+INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
 ASTORE 25
+LABEL L15
 ALOAD 25
-INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
-ASTORE 26
-LABEL L15
-ALOAD 26
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
 IFEQ L16
-ALOAD 26
+ALOAD 25
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/ClientSession
-ASTORE 27
-ALOAD 27
+ASTORE 26
+ALOAD 26
 ALOAD 4
 INVOKEVIRTUAL local/lito381/server/ClientSession.playerObjectId()J
 ALOAD 3
@@ -436,7 +434,7 @@
 GOTO L15
 LABEL L16
 CONST Integer:0
-ISTORE 28
+ISTORE 27
 ALOAD 0
 ILOAD 6
 INVOKESTATIC local/lito381/server/NativeBlindSceneAction.selected(Llocal/lito381/server/NativeAreaMaterialAction$Context;I)Z
@@ -445,7 +443,7 @@
 CONST str:"timedStatus"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST local/lito381/server/BattleTimedStatus
-ASTORE 29
+ASTORE 28
 ALOAD 4
 CONST str:"dragonEyeStatus"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
@@ -460,33 +458,33 @@
 AASTORE
 INVOKESTATIC local/lito381/server/CombatAccess.call(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
 CHECKCAST local/lito381/server/BattleDragonEyeStatus$Snapshot
-ASTORE 30
+ASTORE 29
 CONST Integer:1
 ALOAD 11
 INVOKEVIRTUAL local/lito381/server/CharacterSummary.level()I
 INVOKESTATIC java/lang/Math.max(II)I
-ISTORE 31
+ISTORE 30
 ALOAD 3
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$BlindSpell.includeMagicLevel()Z
 IFEQ L17
 ALOAD 11
 INVOKEVIRTUAL local/lito381/server/CharacterSummary.type()I
-ILOAD 31
+ILOAD 30
 INVOKESTATIC local/lito381/server/BattleTurnUndead.magicLevel(II)I
 GOTO L18
 LABEL L17
 CONST Integer:0
 LABEL L18
-ISTORE 32
-ALOAD 3
-ILOAD 31
+ISTORE 31
+ALOAD 3
+ILOAD 30
 ALOAD 5
 INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.level()I
 ILOAD 13
-ILOAD 32
+ILOAD 31
 ALOAD 12
 INVOKEVIRTUAL local/lito381/server/SourceEquipmentMagicStats.totalMagicHit()I
-ALOAD 30
+ALOAD 29
 INVOKEVIRTUAL local/lito381/server/BattleDragonEyeStatus$Snapshot.magicHit()I
 INVOKESTATIC java/lang/Math.addExact(II)I
 ALOAD 2
@@ -507,16 +505,16 @@
 ALOAD 2
 AASTORE
 INVOKESTATIC local/lito381/server/ClientSession.nativeCompanionBlindChance(Llocal/lito381/server/BattleSpellCatalog$BlindSpell;IIIIIIIZLlocal/lito381/server/ClientSession;[Ljava/lang/Object;)I
-ISTORE 33
+ISTORE 32
 ALOAD 4
 CONST str:"config"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST local/lito381/server/ServerConfig
 INVOKEVIRTUAL local/lito381/server/ServerConfig.darkBlindFixtureRoll()I
-ISTORE 34
-ILOAD 34
+ISTORE 33
+ILOAD 33
 IFLE L19
-ILOAD 34
+ILOAD 33
 GOTO L20
 LABEL L19
 INVOKESTATIC java/util/concurrent/ThreadLocalRandom.current()Ljava/util/concurrent/ThreadLocalRandom;
@@ -524,14 +522,14 @@
 CONST Integer:101
 INVOKEVIRTUAL java/util/concurrent/ThreadLocalRandom.nextInt(II)I
 LABEL L20
-ISTORE 35
+ISTORE 34
 ALOAD 2
 LLOAD 19/20
 INVOKEVIRTUAL local/lito381/server/BattleCompanion.earthBindActive(J)Z
 ILOAD 6
 INVOKESTATIC local/lito381/server/BattleEarthBindRules.blocksProbabilityMagic(ZI)Z
 IFNE L21
-ALOAD 29
+ALOAD 28
 LLOAD 19/20
 INVOKEVIRTUAL local/lito381/server/BattleTimedStatus.absoluteBarrierActive(J)Z
 ILOAD 6
@@ -543,7 +541,7 @@
 LABEL L22
 CONST Integer:0
 LABEL L23
-ISTORE 36
+ISTORE 35
 ALOAD 2
 LLOAD 19/20
 INVOKEVIRTUAL local/lito381/server/BattleCompanion.consumeEraseMagic(J)Z
@@ -551,29 +549,29 @@
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleCompanion.cancelSleep()Z
 POP
-ILOAD 36
+ILOAD 35
 IFNE L28
-ILOAD 33
-ILOAD 35
+ILOAD 32
+ILOAD 34
 IF_ICMPLT L28
-ALOAD 29
+ALOAD 28
 LLOAD 19/20
 ALOAD 4
 ILOAD 6
 INVOKESTATIC local/lito381/server/NativeBlindSceneAction.counter(Llocal/lito381/server/BattleTimedStatus;JLlocal/lito381/server/ClientSession;I)Z
 IFEQ L26
-ALOAD 25
+ALOAD 24
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
-ASTORE 37
+ASTORE 36
 LABEL L24
-ALOAD 37
+ALOAD 36
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
 IFEQ L25
-ALOAD 37
+ALOAD 36
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/ClientSession
-ASTORE 38
-ALOAD 38
+ASTORE 37
+ALOAD 37
 ALOAD 5
 INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.objectId()J
 CONST Integer:31
@@ -588,25 +586,25 @@
 LABEL L25
 GOTO L28
 LABEL L26
-ALOAD 29
+ALOAD 28
 LLOAD 19/20
 ALOAD 3
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$BlindSpell.durationSeconds()I
 INVOKEVIRTUAL local/lito381/server/BattleTimedStatus.activateBlind(JI)V
 CONST Integer:1
-ISTORE 28
-ALOAD 25
+ISTORE 27
+ALOAD 24
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
-ASTORE 39
+ASTORE 38
 LABEL L27
-ALOAD 39
+ALOAD 38
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
 IFEQ L28
-ALOAD 39
+ALOAD 38
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/ClientSession
-ASTORE 40
-ALOAD 40
+ASTORE 39
+ALOAD 39
 ALOAD 5
 INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.objectId()J
 ALOAD 3
@@ -627,7 +625,7 @@
 ILOAD 21
 INVOKEDYNAMIC makeConcatWithConstants(I)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"S_MANA_POINT Blind=\u0001"]
 INVOKESTATIC local/lito381/server/NativeBlindSceneAction.send(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)V
-ILOAD 28
+ILOAD 27
 IFNE L29
 ALOAD 4
 ALOAD 3

--- staged/NativeCompanionForestArea.txt
+++ candidate/NativeCompanionForestArea.txt
@@ -189,16 +189,14 @@
 ALOAD 8
 GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.players:Ljava/util/List;
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
-ASTORE 21
-ALOAD 8
+ALOAD 8
+DUP
 INVOKESTATIC java/util/Objects.requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
 POP
-ALOAD 21
-ALOAD 8
 INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeSourceIndirectPlayerProof.player(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
 INVOKEINTERFACE java/util/stream/Stream.filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream; itf
 INVOKEINTERFACE java/util/stream/Stream.toList()Ljava/util/List; itf
-ASTORE 22
+ASTORE 21
 ALOAD 0
 CONST str:"publishAreaSilenceEffect"
 CONST str:"(Ljava/util/List;JILlocal/lito381/server/ClientSession$Phase;II)V"
@@ -206,7 +204,7 @@
 ANEWARRAY java/lang/Object
 DUP
 CONST Integer:0
-ALOAD 22
+ALOAD 21
 AASTORE
 DUP
 CONST Integer:1
@@ -245,12 +243,12 @@
 LABEL L19
 GOTO L23
 LABEL L20
-ASTORE 23
+ASTORE 22
 ALOAD 16
 LABEL L21
 MONITOREXIT
 LABEL L22
-ALOAD 23
+ALOAD 22
 ATHROW
 LABEL L23
 ALOAD 14
@@ -258,12 +256,12 @@
 LABEL L24
 GOTO L28
 LABEL L25
-ASTORE 24
+ASTORE 23
 ALOAD 14
 LABEL L26
 MONITOREXIT
 LABEL L27
-ALOAD 24
+ALOAD 23
 ATHROW
 LABEL L28
 ALOAD 12
@@ -271,12 +269,12 @@
 LABEL L29
 GOTO L33
 LABEL L30
-ASTORE 25
+ASTORE 24
 ALOAD 12
 LABEL L31
 MONITOREXIT
 LABEL L32
-ALOAD 25
+ALOAD 24
 ATHROW
 LABEL L33
 GOTO L2

--- staged/NativeCompanionWindAction.txt
+++ candidate/NativeCompanionWindAction.txt
@@ -612,56 +612,52 @@

 METHOD publish(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeAreaMaterialAction$Context;JI)V access=8 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
+ALOAD 0
+CONST str:"publishWindShackleEffect"
+CONST str:"(Ljava/util/List;JILlocal/lito381/server/ClientSession$Phase;II)V"
 CONST Integer:6
 ANEWARRAY java/lang/Object
-ASTORE 5
+DUP
+CONST Integer:0
 ALOAD 1
 GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.players:Ljava/util/List;
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
-ASTORE 6
-ALOAD 1
+ALOAD 1
+DUP
 INVOKESTATIC java/util/Objects.requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
 POP
-ALOAD 5
-CONST Integer:0
-ALOAD 6
-ALOAD 1
 INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeSourceIndirectPlayerProof.player(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
 INVOKEINTERFACE java/util/stream/Stream.filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream; itf
 INVOKEINTERFACE java/util/stream/Stream.toList()Ljava/util/List; itf
 AASTORE
-ALOAD 5
+DUP
 CONST Integer:1
 LLOAD 2/3
 INVOKESTATIC java/lang/Long.valueOf(J)Ljava/lang/Long;
 AASTORE
-ALOAD 5
+DUP
 CONST Integer:2
 ILOAD 4
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 AASTORE
-ALOAD 5
+DUP
 CONST Integer:3
 ALOAD 0
 CONST str:"phase"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 AASTORE
-ALOAD 5
+DUP
 CONST Integer:4
 ALOAD 1
 GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.map:I
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 AASTORE
-ALOAD 5
+DUP
 CONST Integer:5
 ALOAD 1
 GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.instance:I
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 AASTORE
-ALOAD 0
-CONST str:"publishWindShackleEffect"
-CONST str:"(Ljava/util/List;JILlocal/lito381/server/ClientSession$Phase;II)V"
-ALOAD 5
 INVOKESTATIC local/lito381/server/CombatAccess.call(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
 POP
 RETURN

--- staged/NativeControlledNegativeMaterial.txt
+++ candidate/NativeControlledNegativeMaterial.txt
@@ -103,12 +103,8 @@

 METHOD cast(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/NativeSelfCastAction$Task;)V access=8 signature=null exceptions=[java/io/IOException]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-TRY L19 L20 L21 java/lang/Throwable
-TRY L1 L2 L25 java/lang/Throwable
-TRY L4 L5 L25 java/lang/Throwable
-TRY L7 L8 L25 java/lang/Throwable
-TRY L10 L16 L25 java/lang/Throwable
-TRY L18 L24 L25 java/lang/Throwable
+TRY L11 L12 L13 java/lang/Throwable
+TRY L1 L16 L17 java/lang/Throwable
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.spellSelector([B)Llocal/lito381/server/Protocol$SpellSelector;
 INVOKEVIRTUAL local/lito381/server/Protocol$SpellSelector.skillId()I
@@ -135,19 +131,17 @@
 GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.phase:Ljava/lang/String;
 CONST str:"WORLD"
 INVOKEVIRTUAL java/lang/String.equals(Ljava/lang/Object;)Z
-IFNE L3
+IFNE L2
 ALOAD 4
 GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.phase:Ljava/lang/String;
 CONST str:"BATTLE"
 INVOKEVIRTUAL java/lang/String.equals(Ljava/lang/Object;)Z
-IFNE L3
+IFNE L2
 ALOAD 4
 INVOKESTATIC local/lito381/server/NativeSourceTargetNegativeLife.close(Llocal/lito381/server/NativeAreaMaterialAction$Context;)V
+RETURN
 LABEL L2
-RETURN
-LABEL L3
-ALOAD 1
-LABEL L4
+ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.attackSpellRequest([B)Llocal/lito381/server/Protocol$AttackSpellRequest;
 ALOAD 4
 INVOKESTATIC local/lito381/server/NativeSourceTargetNegativeLife.request(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/NativeAreaMaterialAction$Context;)Llocal/lito381/server/Protocol$AttackSpellRequest;
@@ -156,18 +150,14 @@
 ALOAD 4
 LLOAD 5/6
 INVOKESTATIC local/lito381/server/NativeSourceTargetNegativeLife.capture(Llocal/lito381/server/NativeAreaMaterialAction$Context;J)Z
-ISTORE 7
 ALOAD 4
 INVOKESTATIC local/lito381/server/NativeSourceControlledNegativeFinal.install(Llocal/lito381/server/NativeAreaMaterialAction$Context;)V
-ILOAD 7
-IFNE L6
+IFNE L3
 ALOAD 4
 INVOKESTATIC local/lito381/server/NativeSourceTargetNegativeLife.close(Llocal/lito381/server/NativeAreaMaterialAction$Context;)V
-LABEL L5
-RETURN
-LABEL L6
-ALOAD 4
-LABEL L7
+RETURN
+LABEL L3
+ALOAD 4
 GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.players:Ljava/util/List;
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
 ALOAD 4
@@ -178,66 +168,62 @@
 CONST null
 INVOKEVIRTUAL java/util/Optional.orElse(Ljava/lang/Object;)Ljava/lang/Object;
 CHECKCAST local/lito381/server/ClientSession
-ASTORE 8
+ASTORE 7
 ALOAD 4
 GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.npcs:Ljava/util/Map;
 LLOAD 5/6
 INVOKESTATIC java/lang/Long.valueOf(J)Ljava/lang/Long;
 INVOKEINTERFACE java/util/Map.get(Ljava/lang/Object;)Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/NativeAreaMaterialAction$Npc
-ASTORE 9
+ASTORE 8
+ALOAD 7
+IFNONNULL L4
 ALOAD 8
-IFNONNULL L9
-ALOAD 9
-IFNONNULL L9
+IFNONNULL L4
 ALOAD 4
 INVOKESTATIC local/lito381/server/NativeSourceTargetNegativeLife.close(Llocal/lito381/server/NativeAreaMaterialAction$Context;)V
-LABEL L8
-RETURN
-LABEL L9
+RETURN
+LABEL L4
+ALOAD 7
+IFNULL L5
+ALOAD 7
+GOTO L7
+LABEL L5
 ALOAD 8
-LABEL L10
-IFNULL L11
-ALOAD 8
-GOTO L13
-LABEL L11
-ALOAD 9
 INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Npc.kind()I
 CONST Integer:3
-IF_ICMPNE L12
-ALOAD 9
+IF_ICMPNE L6
+ALOAD 8
 INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Npc.owner()Ljava/lang/Object;
 CHECKCAST local/lito381/server/ClientSession
-GOTO L13
-LABEL L12
-ALOAD 0
-LABEL L13
+GOTO L7
+LABEL L6
+ALOAD 0
+LABEL L7
+ASTORE 9
+ALOAD 9
+ALOAD 0
+IF_ACMPNE L8
+CONST null
+GOTO L9
+LABEL L8
+ALOAD 9
+ALOAD 4
+INVOKESTATIC local/lito381/server/NativeSourceTargetNegativeReserve.reserve(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeAreaMaterialAction$Context;)Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
+LABEL L9
 ASTORE 10
-ALOAD 10
-ALOAD 0
-IF_ACMPNE L14
-CONST null
-GOTO L15
-LABEL L14
-ALOAD 10
-ALOAD 4
-INVOKESTATIC local/lito381/server/NativeSourceTargetNegativeReserve.reserve(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeAreaMaterialAction$Context;)Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
-LABEL L15
-ASTORE 11
-ALOAD 10
-ALOAD 0
-IF_ACMPEQ L17
-ALOAD 11
-IFNONNULL L17
+ALOAD 9
+ALOAD 0
+IF_ACMPEQ L10
+ALOAD 10
+IFNONNULL L10
 ALOAD 4
 INVOKESTATIC local/lito381/server/NativeSourceTargetNegativeLife.close(Llocal/lito381/server/NativeAreaMaterialAction$Context;)V
-LABEL L16
-RETURN
-LABEL L17
+RETURN
+LABEL L10
 ALOAD 4
 CONST str:"nativeWeaponBreakTarget"
 CONST Integer:1
-LABEL L18
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
 ALOAD 4
@@ -247,49 +233,49 @@
 INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
 ALOAD 4
 CONST str:"nativeWeaponBreakPeerReceipt"
-ALOAD 11
+ALOAD 10
 INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
 ALOAD 4
 ALOAD 0
-ALOAD 10
-ALOAD 4
-ALOAD 2
-LABEL L19
+ALOAD 9
+ALOAD 4
+ALOAD 2
+LABEL L11
 INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativeSelfCastAction$Task;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeControlledNegativeMaterial.lambda$content$d56d5caa352e59cd6097f0494585a6a9c0df1fa2e00a96337df10bf23c9bfa8b(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativeSelfCastAction$Task;)V, type:()V]
 INVOKESTATIC local/lito381/server/NativeAreaMaterialAction.owned(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativeSelfCastAction$Task;)V
-LABEL L20
-ALOAD 11
-IFNULL L23
-ALOAD 11
+LABEL L12
+ALOAD 10
+IFNULL L15
+ALOAD 10
 GETFIELD local/lito381/server/NativePlayerVitalCompletions$Receipt.released:Ljava/util/concurrent/atomic/AtomicBoolean;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicBoolean.get()Z
-IFNE L23
-ALOAD 11
+IFNE L15
+ALOAD 10
 INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
-GOTO L23
-LABEL L21
-ASTORE 12
-ALOAD 11
-IFNULL L22
-ALOAD 11
+GOTO L15
+LABEL L13
+ASTORE 11
+ALOAD 10
+IFNULL L14
+ALOAD 10
 GETFIELD local/lito381/server/NativePlayerVitalCompletions$Receipt.released:Ljava/util/concurrent/atomic/AtomicBoolean;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicBoolean.get()Z
-IFNE L22
+IFNE L14
+ALOAD 10
+INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
+LABEL L14
 ALOAD 11
-INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
-LABEL L22
+ATHROW
+LABEL L15
+ALOAD 4
+INVOKESTATIC local/lito381/server/NativeSourceTargetNegativeLife.close(Llocal/lito381/server/NativeAreaMaterialAction$Context;)V
+LABEL L16
+RETURN
+LABEL L17
+ASTORE 12
+ALOAD 4
+INVOKESTATIC local/lito381/server/NativeSourceTargetNegativeLife.close(Llocal/lito381/server/NativeAreaMaterialAction$Context;)V
 ALOAD 12
-ATHROW
-LABEL L23
-ALOAD 4
-INVOKESTATIC local/lito381/server/NativeSourceTargetNegativeLife.close(Llocal/lito381/server/NativeAreaMaterialAction$Context;)V
-LABEL L24
-RETURN
-LABEL L25
-ASTORE 13
-ALOAD 4
-INVOKESTATIC local/lito381/server/NativeSourceTargetNegativeLife.close(Llocal/lito381/server/NativeAreaMaterialAction$Context;)V
-ALOAD 13
 ATHROW

 METHOD lambda$content$1a0a4704b6ed13a0486f3046f536bbdda19b248687f322940ebb0e30fb7402fe([ZLlocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell;I)V access=10 signature=null exceptions=[java/io/IOException]

--- staged/NativeSourceDollAccepted.txt
+++ candidate/NativeSourceDollAccepted.txt
@@ -351,39 +351,37 @@
 INVOKESPECIAL local/lito381/server/NativeSourceDamageDisplay$Plan.<init>(Llocal/lito381/server/NativeSourceDamageDisplay$Actor;Llocal/lito381/server/NativeSourceDamageDisplay$Actor;Ljava/util/List;)V
 ASTORE 8
 LABEL L1
+ALOAD 2
 ALOAD 8
 GETFIELD local/lito381/server/NativeSourceDamageDisplay$Plan.caster:Llocal/lito381/server/NativeSourceDamageDisplay$Actor;
+INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.binding()Llocal/lito381/server/NativePlayerCommit$Binding;
+ALOAD 8
+GETFIELD local/lito381/server/NativeSourceDamageDisplay$Plan.caster:Llocal/lito381/server/NativeSourceDamageDisplay$Actor;
+DUP
+INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.life()Ljava/lang/Object;
+INVOKESTATIC local/lito381/server/NativeSourceAcceptedPlayers.fromActor(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeSourceDamageDisplay$Actor;Ljava/lang/Object;)Llocal/lito381/server/NativeSourceRawDamage$Member;
 ASTORE 9
-ALOAD 2
-ALOAD 9
+ALOAD 0
+ALOAD 8
+GETFIELD local/lito381/server/NativeSourceDamageDisplay$Plan.target:Llocal/lito381/server/NativeSourceDamageDisplay$Actor;
 INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.binding()Llocal/lito381/server/NativePlayerCommit$Binding;
-ALOAD 9
-ALOAD 9
+ALOAD 8
+GETFIELD local/lito381/server/NativeSourceDamageDisplay$Plan.target:Llocal/lito381/server/NativeSourceDamageDisplay$Actor;
+DUP
 INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.life()Ljava/lang/Object;
 INVOKESTATIC local/lito381/server/NativeSourceAcceptedPlayers.fromActor(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeSourceDamageDisplay$Actor;Ljava/lang/Object;)Llocal/lito381/server/NativeSourceRawDamage$Member;
 ASTORE 10
-ALOAD 8
-GETFIELD local/lito381/server/NativeSourceDamageDisplay$Plan.target:Llocal/lito381/server/NativeSourceDamageDisplay$Actor;
-ASTORE 11
-ALOAD 0
-ALOAD 11
-INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.binding()Llocal/lito381/server/NativePlayerCommit$Binding;
-ALOAD 11
-ALOAD 11
-INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.life()Ljava/lang/Object;
-INVOKESTATIC local/lito381/server/NativeSourceAcceptedPlayers.fromActor(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeSourceDamageDisplay$Actor;Ljava/lang/Object;)Llocal/lito381/server/NativeSourceRawDamage$Member;
-ASTORE 12
 NEW local/lito381/server/NativeSourceRawDamage$Guard
 DUP
-ALOAD 12
 ALOAD 10
+ALOAD 9
 CONST Integer:1
 ALOAD 8
 GETFIELD local/lito381/server/NativeSourceDamageDisplay$Plan.owner:Llocal/lito381/server/NativePlayerLifetime;
 INVOKESPECIAL local/lito381/server/NativeSourceRawDamage$Guard.<init>(Llocal/lito381/server/NativeSourceRawDamage$Member;Llocal/lito381/server/NativeSourceRawDamage$Member;ZLlocal/lito381/server/NativePlayerLifetime;)V
-ASTORE 13
-ALOAD 8
-ALOAD 13
+ASTORE 11
+ALOAD 8
+ALOAD 11
 PUTFIELD local/lito381/server/NativeSourceDamageDisplay$Plan.nativeRawGuard:Llocal/lito381/server/NativeSourceRawDamage$Guard;
 ALOAD 8
 NEW local/lito381/server/NativeSourceDollAccepted$Pending
@@ -391,15 +389,15 @@
 ALOAD 2
 ALOAD 0
 ALOAD 7
-ALOAD 13
+ALOAD 11
 INVOKESPECIAL local/lito381/server/NativeSourceDollAccepted$Pending.<init>(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeMagicDollEffects$AttackProc;Llocal/lito381/server/NativeSourceRawDamage$Guard;)V
 PUTFIELD local/lito381/server/NativeSourceDamageDisplay$Plan.nativeSourceDollPending:Ljava/lang/Object;
 ALOAD 8
-ASTORE 14
-ALOAD 2
-ALOAD 0
-ALOAD 14
-ALOAD 13
+ASTORE 12
+ALOAD 2
+ALOAD 0
+ALOAD 12
+ALOAD 11
 ALOAD 0
 ALOAD 2
 ALOAD 3
@@ -410,8 +408,8 @@
 LABEL L3
 GOTO L5
 LABEL L4
-ASTORE 15
-ALOAD 15
+ASTORE 13
+ALOAD 13
 INVOKEVIRTUAL java/io/UncheckedIOException.getCause()Ljava/io/IOException;
 ATHROW
 LABEL L5

--- staged/NativeSourceHealingCast.txt
+++ candidate/NativeSourceHealingCast.txt
@@ -788,26 +788,22 @@
 INVOKEVIRTUAL java/util/ArrayList.add(Ljava/lang/Object;)Z
 POP
 LABEL L18
+ALOAD 15
 ALOAD 13
 LABEL L19
 DUP
 INVOKESTATIC java/util/Objects.requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
 POP
 INVOKEDYNAMIC getAsBoolean(Llocal/lito381/server/NativeSourceHealingCast$Cast;)Ljava/util/function/BooleanSupplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Z, handle:H5 local/lito381/server/NativeSourceHealingCast$Cast.before()Z, type:()Z]
-ASTORE 16
 ALOAD 0
 ALOAD 13
 ALOAD 7
 ALOAD 1
 ILOAD 2
 INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSourceHealingCast$Cast;Llocal/lito381/server/NativeAreaMaterialAction$Context;[Ljava/lang/Object;I)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeSourceHealingCast.lambda$content$793657edf8b83689df451308b022d4e085f40f33b6b8804e33c67951d57d8872(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSourceHealingCast$Cast;Llocal/lito381/server/NativeAreaMaterialAction$Context;[Ljava/lang/Object;I)V, type:()V]
-ASTORE 17
 ALOAD 7
 ALOAD 13
 INVOKESTATIC local/lito381/server/NativeSourceControlledHealing.capture(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativeSourceHealingCast$Cast;)V
-ALOAD 15
-ALOAD 16
-ALOAD 17
 INVOKESTATIC local/lito381/server/NativeSourceHealingCast.owned(Ljava/util/List;Ljava/util/function/BooleanSupplier;Llocal/lito381/server/NativeSelfCastAction$Task;)V
 LABEL L20
 ALOAD 7
@@ -820,7 +816,7 @@
 INVOKEVIRTUAL local/lito381/server/NativeSourceHealingCast$Cast.close()V
 GOTO L23
 LABEL L21
-ASTORE 18
+ASTORE 16
 ALOAD 7
 ALOAD 14
 PUTFIELD local/lito381/server/NativeAreaMaterialAction$Context.nativeSourceHealingCast:Ljava/lang/Object;
@@ -830,7 +826,7 @@
 ALOAD 13
 INVOKEVIRTUAL local/lito381/server/NativeSourceHealingCast$Cast.close()V
 LABEL L22
-ALOAD 18
+ALOAD 16
 ATHROW
 LABEL L23
 RETURN

--- staged/NativeWeaponBreakBattleCompanion.txt
+++ candidate/NativeWeaponBreakBattleCompanion.txt
@@ -569,8 +569,7 @@

 METHOD nativeSourceWeaponFirstPrepare(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/NativeAreaMaterialAction$Context;)Llocal/lito381/server/SourceNpcPhysicalStats; access=8 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-TRY L0 L1 L5 java/lang/Exception
-TRY L3 L4 L5 java/lang/Exception
+TRY L0 L2 L3 java/lang/Exception
 ALOAD 0
 CONST str:"selectedSourceAccounts"
 CONST str:"()Llocal/lito381/server/AccountRepository;"
@@ -579,30 +578,26 @@
 ANEWARRAY java/lang/Object
 INVOKESTATIC local/lito381/server/CombatAccess.call(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
 CHECKCAST local/lito381/server/AccountRepository
-ASTORE 3
 ALOAD 1
 ALOAD 2
 INVOKESTATIC local/lito381/server/NativeSourceWeaponTargetLife.snapshot(Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/NativeAreaMaterialAction$Context;)Llocal/lito381/server/BattleCompanion$Snapshot;
-ASTORE 4
-ALOAD 4
-IFNONNULL L2
-LABEL L1
+DUP
+IFNONNULL L1
+POP
+POP
 CONST null
 ARETURN
-LABEL L2
-ALOAD 3
-ALOAD 4
-LABEL L3
+LABEL L1
 INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.npcId()I
 INVOKEINTERFACE local/lito381/server/AccountRepository.npcPhysicalStats(I)Llocal/lito381/server/SourceNpcPhysicalStats; itf
-LABEL L4
+LABEL L2
 ARETURN
-LABEL L5
-ASTORE 5
+LABEL L3
+ASTORE 3
 CONST type:Llocal/lito381/server/NativeWeaponBreakBattleCompanion;
 INVOKESTATIC local/lito381/server/NativeReuseLog.getLogger(Ljava/lang/Class;)Llocal/lito381/server/NativeReuseLog$Logger;
 CONST str:"Weapon Break companion Source lookup failed"
-ALOAD 5
+ALOAD 3
 INVOKEINTERFACE local/lito381/server/NativeReuseLog$Logger.error(Ljava/lang/String;Ljava/lang/Throwable;)V itf
 CONST null
 ARETURN

--- staged/NativeWeaponBreakTargetLifecycle.txt
+++ candidate/NativeWeaponBreakTargetLifecycle.txt
@@ -90,7 +90,7 @@
 INVOKESTATIC local/lito381/server/NativeWeaponBreakMaterialAction.nativeTargetOriginalAdmit(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;III)Z
 IRETURN

-METHOD captureOutput(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)Z access=8 signature=null exceptions=[java/io/IOException]
+METHOD captureOutput(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)Z access=8 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
 ALOAD 0
 ALOAD 1
@@ -236,7 +236,7 @@
 LLOAD 10/11
 ALOAD 6
 ALOAD 2
-INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$content$482c7574b07009ec2ff18e498dc26beb9cd40dd0504924d9cc5a37cffa554d12(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$content$d1f722aff69c65c1a6981c88ee904ac033a0971cd4b310057baf8024caf72cc3(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)V, type:()V]
 INVOKESTATIC local/lito381/server/NativeLightAction.deferWeaponTargetOutput(Llocal/lito381/server/NativeSelfCastAction$Task;)V
 CONST Integer:1
 IRETURN
@@ -249,61 +249,30 @@
 INVOKESTATIC local/lito381/server/NativeSourceWeaponTargetLife.dispatch(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/NativeSelfCastAction$Task;)V
 RETURN

-METHOD lambda$content$36d8eb2d9f66ea204de1e76a61df45d227e5bc5de56babecc93f81a93d7d8f4a(Llocal/lito381/server/NativeAreaMaterialAction$Context;ZLlocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;Llocal/lito381/server/NativeAreaMaterialAction$Npc;Llocal/lito381/server/ClientSession;Llocal/lito381/server/SourceNpcPhysicalStats;)V access=10 signature=null exceptions=[java/io/IOException]
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-TRY L0 L3 L4 java/lang/Throwable
-ALOAD 0
-CONST str:"nativeWeaponBreakTarget"
-CONST Integer:2
-INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
-INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
-ILOAD 1
-LABEL L0
-IFEQ L1
-ALOAD 2
-ALOAD 3
+METHOD lambda$content$0e18a5aea09afd396b455edcb844d9267857664e7597cbb4c249b06908e02776(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)V access=10 signature=null exceptions=[]
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
+ALOAD 0
+ALOAD 1
+INVOKESTATIC local/lito381/server/NativeSourceControlledRoleFrame.current(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;)Z
+IFEQ L0
+ALOAD 2
+CONST Integer:0
+ALOAD 1
+ALOAD 3
+ALOAD 0
+ALOAD 1
 ALOAD 4
-INVOKEVIRTUAL local/lito381/server/ClientSession.nativeMaterialWorldWeaponBreakBody([BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;)V
-GOTO L3
-LABEL L1
-ALOAD 5
-INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Npc.kind()I
-CONST Integer:3
-IF_ICMPNE L2
-ALOAD 2
-ALOAD 6
-ALOAD 5
-INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Npc.actor()Ljava/lang/Object;
-CHECKCAST local/lito381/server/BattleCompanion
-ALOAD 4
-ALOAD 7
-INVOKESTATIC local/lito381/server/NativeWeaponBreakBattleCompanion.cast(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;Llocal/lito381/server/SourceNpcPhysicalStats;)V
-GOTO L3
-LABEL L2
-ALOAD 2
-ALOAD 3
-ALOAD 4
-INVOKEVIRTUAL local/lito381/server/ClientSession.nativeMaterialWeaponBreakBody([BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;)V
-LABEL L3
-ALOAD 0
-CONST str:"nativeWeaponBreakTarget"
-CONST Integer:1
-INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
-INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
-GOTO L5
-LABEL L4
-ASTORE 8
-ALOAD 0
-CONST str:"nativeWeaponBreakTarget"
-CONST Integer:1
-INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
-INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
+ILOAD 5
+LLOAD 6/7
 ALOAD 8
-ATHROW
-LABEL L5
-RETURN
-
-METHOD lambda$content$3ab9781bb6ab73c934c06ad67dac8b34cb830110d269c73acca8a611fa6c75d8(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/util/List;ZJ[BLjava/lang/String;)V access=10 signature=null exceptions=[]
+ALOAD 9
+INVOKEDYNAMIC run(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/util/List;ZJ[BLjava/lang/String;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$content$266899dd89994cc09b96b322ff9ab64427013dea7adecca9a4d55fd66e3e36bc(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/util/List;ZJ[BLjava/lang/String;)V, type:()V]
+INVOKESTATIC local/lito381/server/NativeRolePublication.completion(Llocal/lito381/server/ClientSession;Ljava/lang/Runnable;)Ljava/lang/Runnable;
+AASTORE
+LABEL L0
+RETURN
+
+METHOD lambda$content$266899dd89994cc09b96b322ff9ab64427013dea7adecca9a4d55fd66e3e36bc(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/util/List;ZJ[BLjava/lang/String;)V access=10 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
 TRY L1 L5 L6 java/lang/Throwable
 TRY L7 L8 L6 java/lang/Throwable
@@ -336,6 +305,9 @@
 ALOAD 3
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
 ALOAD 0
+DUP
+INVOKESTATIC java/util/Objects.requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
+POP
 INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeSourceIndirectPlayerProof.player(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
 INVOKEINTERFACE java/util/stream/Stream.allMatch(Ljava/util/function/Predicate;)Z itf
 IFEQ L3
@@ -378,6 +350,9 @@
 ALOAD 3
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
 ALOAD 0
+DUP
+INVOKESTATIC java/util/Objects.requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
+POP
 INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeSourceIndirectPlayerProof.player(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
 INVOKEINTERFACE java/util/stream/Stream.allMatch(Ljava/util/function/Predicate;)Z itf
 IFEQ L12
@@ -427,32 +402,58 @@
 LABEL L23
 RETURN

-METHOD lambda$content$482c7574b07009ec2ff18e498dc26beb9cd40dd0504924d9cc5a37cffa554d12(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)V access=10 signature=null exceptions=[java/io/IOException]
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-CONST Integer:1
-ANEWARRAY java/lang/Runnable
-ASTORE 9
-ALOAD 0
-ALOAD 1
-ALOAD 0
-ALOAD 9
-ALOAD 2
-ALOAD 3
-ILOAD 4
-LLOAD 5/6
+METHOD lambda$content$36d8eb2d9f66ea204de1e76a61df45d227e5bc5de56babecc93f81a93d7d8f4a(Llocal/lito381/server/NativeAreaMaterialAction$Context;ZLlocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;Llocal/lito381/server/NativeAreaMaterialAction$Npc;Llocal/lito381/server/ClientSession;Llocal/lito381/server/SourceNpcPhysicalStats;)V access=10 signature=null exceptions=[java/io/IOException]
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
+TRY L0 L3 L4 java/lang/Throwable
+ALOAD 0
+CONST str:"nativeWeaponBreakTarget"
+CONST Integer:2
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
+ILOAD 1
+LABEL L0
+IFEQ L1
+ALOAD 2
+ALOAD 3
+ALOAD 4
+INVOKEVIRTUAL local/lito381/server/ClientSession.nativeMaterialWorldWeaponBreakBody([BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;)V
+GOTO L3
+LABEL L1
+ALOAD 5
+INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Npc.kind()I
+CONST Integer:3
+IF_ICMPNE L2
+ALOAD 2
+ALOAD 6
+ALOAD 5
+INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Npc.actor()Ljava/lang/Object;
+CHECKCAST local/lito381/server/BattleCompanion
+ALOAD 4
 ALOAD 7
+INVOKESTATIC local/lito381/server/NativeWeaponBreakBattleCompanion.cast(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;Llocal/lito381/server/SourceNpcPhysicalStats;)V
+GOTO L3
+LABEL L2
+ALOAD 2
+ALOAD 3
+ALOAD 4
+INVOKEVIRTUAL local/lito381/server/ClientSession.nativeMaterialWeaponBreakBody([BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;)V
+LABEL L3
+ALOAD 0
+CONST str:"nativeWeaponBreakTarget"
+CONST Integer:1
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
+GOTO L5
+LABEL L4
+ASTORE 8
+ALOAD 0
+CONST str:"nativeWeaponBreakTarget"
+CONST Integer:1
+INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
 ALOAD 8
-INVOKEDYNAMIC run(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$content$d04de0c030c69ba5b18bba77bd821b479cee943a350fb0251d0e60c82cfbf234(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)V, type:()V]
-INVOKESTATIC local/lito381/server/NativeSourcePlayerActionLock.run(Llocal/lito381/server/ClientSession;Ljava/lang/Runnable;)V
-ALOAD 9
-CONST Integer:0
-AALOAD
-IFNULL L0
-ALOAD 9
-CONST Integer:0
-AALOAD
-INVOKEINTERFACE java/lang/Runnable.run()V itf
-LABEL L0
+ATHROW
+LABEL L5
 RETURN

 METHOD lambda$content$6f8c0971c6de90fb51fbe3e12f5a8d0134d9be19148286315bde7a8b182611fa(ZLlocal/lito381/server/NativeAreaMaterialAction$Npc;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/GameHub;JLjava/lang/Object;[BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;Llocal/lito381/server/ClientSession;)V access=10 signature=null exceptions=[java/io/IOException]
@@ -594,26 +595,31 @@
 LABEL L1
 IRETURN

-METHOD lambda$content$d04de0c030c69ba5b18bba77bd821b479cee943a350fb0251d0e60c82cfbf234(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)V access=10 signature=null exceptions=[]
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-ALOAD 0
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativeSourceControlledRoleFrame.current(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;)Z
-IFEQ L0
-ALOAD 2
+METHOD lambda$content$d1f722aff69c65c1a6981c88ee904ac033a0971cd4b310057baf8024caf72cc3(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)V access=10 signature=null exceptions=[java/io/IOException]
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
+CONST Integer:1
+ANEWARRAY java/lang/Runnable
+ASTORE 9
+ALOAD 0
+ALOAD 1
+ALOAD 0
+ALOAD 9
+ALOAD 2
+ALOAD 3
+ILOAD 4
+LLOAD 5/6
+ALOAD 7
+ALOAD 8
+INVOKEDYNAMIC run(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$content$0e18a5aea09afd396b455edcb844d9267857664e7597cbb4c249b06908e02776(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/NativeAreaMaterialAction$Context;Ljava/util/List;ZJ[BLjava/lang/String;)V, type:()V]
+INVOKESTATIC local/lito381/server/NativeSourcePlayerActionLock.run(Llocal/lito381/server/ClientSession;Ljava/lang/Runnable;)V
+ALOAD 9
 CONST Integer:0
-ALOAD 1
-ALOAD 3
-ALOAD 0
-ALOAD 1
-ALOAD 4
-ILOAD 5
-LLOAD 6/7
-ALOAD 8
-ALOAD 9
-INVOKEDYNAMIC run(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/util/List;ZJ[BLjava/lang/String;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$content$3ab9781bb6ab73c934c06ad67dac8b34cb830110d269c73acca8a611fa6c75d8(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/util/List;ZJ[BLjava/lang/String;)V, type:()V]
-INVOKESTATIC local/lito381/server/NativeRolePublication.completion(Llocal/lito381/server/ClientSession;Ljava/lang/Runnable;)Ljava/lang/Runnable;
-AASTORE
+AALOAD
+IFNULL L0
+ALOAD 9
+CONST Integer:0
+AALOAD
+INVOKEINTERFACE java/lang/Runnable.run()V itf
 LABEL L0
 RETURN
```

## 附錄 D　最終工具結果

```json
{
  "tool": "semantic-diff",
  "generatedAt": "2026-11-05T09:23:30Z",
  "left": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\verified\\staged.jar",
  "right": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\reference\\candidate-50635f7e.jar",
  "leftClasspath": [
    "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\reference\\candidate-50635f7e.jar"
  ],
  "rightClasspath": [],
  "allowRight": null,
  "effectsMethodsFile": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\scratch\\issue-62\\effects-approved.txt",
  "extraMethodsFile": null,
  "metadataExemptFile": null,
  "deviations": [],
  "ctorPrologueOrder": [],
  "accessWiden": [],
  "layoutMethods": [],
  "extraMethods": [],
  "metadataExempt": [],
  "leftClassCount": 22,
  "rightClassCount": 3033,
  "comparedClassCount": 22,
  "classSelection": {
    "missingLeft": [],
    "extraLeft": []
  },
  "byteSameCount": 0,
  "counts": {
    "SAME": 15,
    "SAME_EFFECTS": 1,
    "SAME_LAYOUT": 0,
    "DEVIATION": 0,
    "DIFF": 6,
    "ONLY_LEFT": 0,
    "ONLY_RIGHT": 0,
    "BYTE_SAME": 0
  },
  "classes": [
    {
      "name": "local/lito381/server/NativeBlindSceneAction",
      "status": "DIFF",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": [],
      "firstDiff": {
        "where": "method",
        "member": "companion(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleSpellCatalog$BlindSpell;)V",
        "insnIndex": 329,
        "detail": "left=ASTORE 24 right=ALOAD 0"
      }
    },
    {
      "name": "local/lito381/server/NativeCompanionForestArea",
      "status": "SAME_EFFECTS",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [
        {
          "method": "apply(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleSpellCatalog$AreaSilenceSpell;IIIIJ)V",
          "status": "SAME_EFFECTS",
          "detail": null
        }
      ],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeCompanionRefScene",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeCompanionWindAction",
      "status": "DIFF",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": [],
      "firstDiff": {
        "where": "method",
        "member": "publish(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeAreaMaterialAction$Context;JI)V",
        "insnIndex": 0,
        "detail": "left=CONST Integer:6 right=ALOAD 0"
      }
    },
    {
      "name": "local/lito381/server/NativeControlledNegativeMaterial",
      "status": "DIFF",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": [],
      "firstDiff": {
        "where": "method",
        "member": "cast(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/NativeSelfCastAction$Task;)V",
        "insnIndex": null,
        "detail": "left=TRY L19 L20 L21 java/lang/Throwable   right=TRY L11 L12 L13 java/lang/Throwable  "
      }
    },
    {
      "name": "local/lito381/server/NativePairedCastSafety",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceBatchTerminal",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceCubePlacement",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceCubePlacement$Draft",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceDollAccepted",
      "status": "DIFF",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": [],
      "firstDiff": {
        "where": "method",
        "member": "physical(Llocal/lito381/server/ClientSession;ILlocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSourceKiringkuAdmission$Aim;Llocal/lito381/server/SourceWeaponPhysicalStats;Llocal/lito381/server/BattleWeaponCritical$RollOutcome;Llocal/lito381/server/BattleWeaponCritical$DamageOutcome;Llocal/lito381/server/NativeMagicDollEffects$AttackProc;)V",
        "insnIndex": 39,
        "detail": "left=ALOAD 8 right=ALOAD 2"
      }
    },
    {
      "name": "local/lito381/server/NativeSourceDollAccepted$Pending",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceDollNpc",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceDollNpc$Scope",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceDollSkills",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceDollSkills$View",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceDollSlow",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceHealingCast",
      "status": "DIFF",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": [],
      "firstDiff": {
        "where": "method",
        "member": "run(Llocal/lito381/server/ClientSession;[Ljava/lang/Object;I)V",
        "insnIndex": 172,
        "detail": "left=ALOAD 13 right=ALOAD 15"
      }
    },
    {
      "name": "local/lito381/server/NativeSourceHealingCast$Cast",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourcePendingAdmission",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSummonTerminalLifecycle",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeWeaponBreakBattleCompanion",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeWeaponBreakTargetLifecycle",
      "status": "DIFF",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": [],
      "firstDiff": {
        "where": "method",
        "member": "captureOutput(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)Z",
        "insnIndex": null,
        "detail": "left=java/io/IOException right="
      }
    }
  ]
}
```

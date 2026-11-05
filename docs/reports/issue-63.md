# Issue #63：T020-B02 戰鬥併入暫存

## 結論

**尚未達驗收，needs:claude。** 51 份 staged Java 已一次編譯成功，轉換後恰為 108 個驗收 entry；新版 semantic-diff 結果 **106 SAME／2 DIFF**，沒有 missing／extra／ONLY_LEFT／ONLY_RIGHT。兩個剩餘 entry 的完整正規化差異及限制列於下方，未自行豁免、修改比較器或覆蓋候選 bytes。

工作分支 `task/63`，工作基準 `450178d`（包含 #102、#153、#166；由初始 `a87645e` 快轉取得）。本輪取代 PR #81 過去使用候選 bytes 後處理的驗證說法；舊報告的 108 SAME 不屬於本輪證據。遠端舊提交 `00a0d7c836aad3830f062ee123aa601e2921f068` 保留為合併父提交，工作檔採本輪由凍結來源重建的版本，不使用強制推送。

## 做了什麼

- 51 份 Java 均由 `merge-inputs/50635f7e/sources/local/lito381/server/` 起步；用 CFR 0.152 反編譯候選中題述 22 個 entry 所屬的 11 個外層類，逐項移植。另比對後補齊 `NativeSourceAcceptedPlayers`、`NativeSourceAreaClassSelection`（含 Cursor）、`NativeSourceControlledHealing` 的後加 ASM 語意；這三檔也在 B02。
- 補回 freeze／shock／角色識別／controlled display 的原方法及轉呼叫、`playerLife`／`petLife`／NPC first-life、Placement 的 volatile 欄位、受眾快照、技能 metadata 載入與 Cube NPC 佇列／傳輸入口。
- `NativeSourceArenaPoison$Viewer`、`NativeSourceControlledDisplay$Output` 改用 `@AsRecord`，保留元件順序、accessors、額外 transient 欄位與兩組建構子；`finalFields` 由既有轉換器還原，沒有手寫 Object 方法。
- `NativeSourceAreaClassSelection$Cursor` 的舊建構子原本不賦值新 final 欄位，因此採 #153 已合併的 `@AsmFinal({"nativeSourceTargetMask"})`，仍由同一 #102 CLI 處理。沒有改轉換器。該 entry 實測 SAME。
- 候選保留的未引用 `lambda$deliver$0` 以實際 Java 方法保留；它呼叫 `complete`，並非 stub。其餘 lambda 交新版比較器正規化。
- 提交範圍只有 B02 的 51 份 staged `.java` 與本報告。`src/main`、`pom.xml`、`docs/REPORT.md` 的上游更新僅來自快轉，本輪相對 `450178d` 無修改。

## 已驗證

Windows、Microsoft OpenJDK 17.0.17、CFR 0.152、ASM 9.7。沒有啟動伺服器／登入器／遊戲、連 DB、讀寫機密設定、封存或正式部署產物。

| 項目 | 結果 |
|---|---|
| 一次 javac 編譯 51 份 Java | exit 0；僅 unchecked 提示 |
| 既有 as-record CLI | exit 0；scanned=108 converted=3（2 AsRecord＋1 AsmFinal） |
| classes 集合與 B02.verify_entries 比對 | 108／108；缺少 0、多出 0 |
| semantic-diff | SAME=106、DIFF=2；其餘結果種類皆 0 |
| 獨立重建 | 下方完整驗證腳本由全新目錄重建，結果相同 |
| 額外診斷 | 遍歷全部 entry／全部方法，只有下方兩個方法不同 |

實際輸出：

```text
javac.log exit=0
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
as-record.log exit=0
as-record: scanned=108 converted=3
entries=108 missing=[] extra=[]
semantic.log exit=0
SUMMARY SAME=106 SAME_EFFECTS=0 SAME_LAYOUT=0 DEVIATION=0 DIFF=2 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0 ALLOW_METHODS=0
```

semantic-diff 的 exit 0 只代表報告成功產生。下方驗證腳本另檢查 DIFF，遇此結果會非零結束；**不可據 CLI exit 0 或一般 CI 綠燈宣告本批通過**。

固定輸入 SHA-256：

- candidate：`50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`
- base：`e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`
- CFR：`f686e8f3ded377d7bc87d216a90e9e9512df4156e75b06c655a16648ae8765b2`
- 本輪重建 JAR：`4e6ea515e960e1679b22c937aa61673d2e754b39b9aea5a733c4d5ac22d9b75f`（ZIP 時間戳可能使另次打包雜湊不同）
- 51 份 staged 集合摘要：`c3820786459d0d92039d300201d7caf3e40ea8d2568fddcafa6ea32f128a92d4`（依檔名排序，串接 UTF-8 檔名＋換行＋檔案 SHA-256 原始 32 bytes，再 SHA-256）。

## 未完成差異與理由

兩個 entry 均位於 `local/lito381/server/`：

1. `NativeAcceptedAreaFreezeMutation.class`：`players(Ljava/util/List;)Ljava/util/List;`。
2. `NativeBattleControlledCancellation.class`：`members(Llocal/lito381/server/ClientSession;I)Ljava/util/List;`。

候選在既有 `c::player` 的 invokedynamic 上將 handle 改成 static `NativeSourceIndirectPlayerProof.player(Context, ClientSession)`，卻保留建立 bound method reference 時的 `DUP; Objects.requireNonNull; POP`。Java 不能直接寫對 static 二參數方法的「綁定第一參數」方法參照；本輪採 lambda，明確先呼叫 `stream()`、再檢查 Context、最後建立 Predicate，以保留呼叫及例外時機。不能直接刪掉 null 檢查，也不能改回 `Context.player`。

#166 已把 lambda 轉呼叫收斂到候選同一個 static handle，但仍未接受 local／DUP 運送形式。額外使用未修改比較器的套件內診斷確認：兩方法的 LocalTransport 結果仍不同；players 的 singleton view 為 left=true/right=false，members 為 left=false/right=false。候選 players 的回傳值有兩條分支來源，members 的受眾清單也由條件分支匯合，現有全方法唯一來源守門無法證明。這是本輪無法讓現有驗收變成 SAME 的具體限制，**不是已證明所有等價 Java 都不可能**。

已嘗試捕獲 Group 改為固定 Context、保留顯式 null 檢查、stream 單次使用暫存、players 共同回傳變數；避免省略例外或增加 callback 以換取結果。`memberFilter` 已 SAME；metadata 的單次使用暫存調整亦已 SAME。剩餘兩方法交主控判斷 Java 寫法或另立比較器修補，未新增任何核准例外。

以下是全部剩餘方法差異，直接取自既有 JarIndex／ClassComparer 的正規化指令（非 javap 常數池 diff）；沒有截斷指令或 handle。`-` 是 staged，`+` 是 candidate，未顯示的上下文一致。兩者方法 access/signature/exceptions 與 try 表相同；其他 entry／方法無差異。

```diff
--- NativeAcceptedAreaFreezeMutation-0-left.txt
+++ NativeAcceptedAreaFreezeMutation-0-right.txt
@@ -4,25 +4,18 @@
 ASTORE 1
 ALOAD 1
 IFNONNULL L0
 ALOAD 0
-ASTORE 2
 GOTO L1
 LABEL L0
 ALOAD 0
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
-ASTORE 3
 ALOAD 1
 GETFIELD local/lito381/server/NativeAcceptedAreaFreezeMutation$Group.c:Llocal/lito381/server/NativeAreaMaterialAction$Context;
-ASTORE 4
-ALOAD 4
+DUP
 INVOKESTATIC java/util/Objects.requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
 POP
-ALOAD 3
-ALOAD 4
 INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeSourceIndirectPlayerProof.player(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
 INVOKEINTERFACE java/util/stream/Stream.filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream; itf
 INVOKEINTERFACE java/util/stream/Stream.toList()Ljava/util/List; itf
-ASTORE 2
 LABEL L1
-ALOAD 2
 ARETURN
--- NativeBattleControlledCancellation-0-left.txt
+++ NativeBattleControlledCancellation-0-right.txt
@@ -38,14 +38,12 @@
 LABEL L2
 ASTORE 3
 ALOAD 3
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
-ASTORE 4
 ALOAD 2
+DUP
 INVOKESTATIC java/util/Objects.requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
 POP
-ALOAD 4
-ALOAD 2
 INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeSourceIndirectPlayerProof.player(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
 INVOKEINTERFACE java/util/stream/Stream.filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream; itf
 INVOKEINTERFACE java/util/stream/Stream.toList()Ljava/util/List; itf
 ARETURN
```

## 怎麼重現

從 worktree 根目錄執行。把以下 Python 存成 `scratch/issue-63/verify.py`，再執行：

```powershell
& D:/dev-orchestrator/tools/python/python.exe -B -X utf8 scratch/issue-63/verify.py
```

腳本會先將既有兩種 annotation 編到獨立 `annotations/`，只作 javac classpath，不放入本批 108 個輸出；B02 一次編譯、只由既有轉換器後處理，再完整打包所有 class。沒有以 subset 隱藏多出 entry。`--left-classpath` 僅供新版比較器解析欄位 owner，未把依賴混入比對 JAR。沒有 effects／layout／deviation／allow-methods 等豁免。

此腳本拒絕覆用已存在的 classes 目錄。第二次執行請將腳本中的 `reproduce` 改成另一個全新目錄名稱，無須刪除前次證據。其他機器可調整 JDK／Python／只讀 Maven 快取位置。

```python
import hashlib, json, os, subprocess, sys, urllib.request, zipfile
from pathlib import Path
root = Path.cwd()
server = root / 'lito伺服器/LiTo381-Server'
scratch = root / 'scratch/issue-63/reproduce'
target = server / 'target/issue-63/reproduce'
scratch.mkdir(parents=True, exist_ok=True)
target.mkdir(parents=True, exist_ok=True)
(target / 'tmp').mkdir(exist_ok=True)
os.environ['JAVA_HOME'] = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot'
os.environ['AS_RECORD_MAVEN_REPO'] = 'D:/dev-cache/maven/repository'
os.environ['TMP'] = os.environ['TEMP'] = str(target / 'tmp')
sys.dont_write_bytecode = True
sys.path.insert(0, str(server / 'tools/as-record'))
import as_record as tool
batch = next(x for x in json.loads((root / 'docs/combat-merge/batches.json').read_text('utf-8'))['batches'] if x['id'] == 'B02')
def run(args, name):
    p = subprocess.run([str(a) for a in args], capture_output=True, encoding='utf-8', errors='replace')
    text = p.stdout + p.stderr
    (scratch / name).write_text(text, 'utf-8')
    print(name, 'exit=', p.returncode, text)
    p.check_returncode()
java = [tool.jdk('java'), '-Dfile.encoding=UTF-8', '-Djava.io.tmpdir=' + str(target / 'tmp')]
annotations = target / 'annotations'
tool.compile_java([server / 'src/main/java/local/lito381/server/AsRecord.java', server / 'src/main/java/local/lito381/server/AsmFinal.java'], annotations, '')
classes = target / 'classes'
classes.mkdir()  # Refuse stale output; use a new target path for another run.
cp = os.pathsep.join(str(server / x) for x in ['reference/candidate-50635f7e.jar', 'reference/base-0.6.71.jar', 'lib/*']) + os.pathsep + str(annotations)
staged = server / 'merge-inputs/50635f7e/staged/local/lito381/server'
args = ['--release', '17', '-encoding', 'UTF-8', '-cp', cp, '-d', str(classes)] + [str(staged / (c + '.java')) for c in batch['classes']]
argfile = scratch / 'javac.args'
argfile.write_text('\n'.join('"' + a.replace('\\', '/') + '"' for a in args), 'utf-8')
run([tool.jdk('javac'), '-J-Dfile.encoding=UTF-8', '@' + str(argfile)], 'javac.log')
run([*java, '-cp', tool.build(), 'local.lito381.asrecord.AsRecord', classes], 'as-record.log')
expected = set(batch['verify_entries'])
actual = {p.relative_to(classes).as_posix() for p in classes.rglob('*.class')}
assert expected == actual, (sorted(expected - actual), sorted(actual - expected))
print('entries=108 missing=[] extra=[]')
jar = target / 'compiled.jar'
with zipfile.ZipFile(jar, 'w', zipfile.ZIP_DEFLATED) as z:
    for p in sorted(classes.rglob('*.class')): z.write(p, p.relative_to(classes).as_posix())
analysis = server / 'target/issue-63/dependency/asm-analysis-9.7.jar'
analysis.parent.mkdir(parents=True, exist_ok=True)
if not analysis.exists():
    analysis.write_bytes(urllib.request.urlopen('https://repo.maven.apache.org/maven2/org/ow2/asm/asm-analysis/9.7/asm-analysis-9.7.jar').read())
assert hashlib.sha256(analysis.read_bytes()).hexdigest() == '7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f'
dependencies = tool.dependencies() + os.pathsep + str(analysis)
toolclasses = target / 'semantic-tool'
tool.compile_java(sorted((server / 'tools/semantic-diff/src/main/java').rglob('*.java')), toolclasses, dependencies)
entries = scratch / 'entries.txt'
entries.write_text('\n'.join(batch['verify_entries']) + '\n', 'utf-8')
run([*java, '-cp', str(toolclasses) + os.pathsep + dependencies, 'local.lito381.semanticdiff.SemanticDiff',
     '--left', jar, '--right', server / 'reference/candidate-50635f7e.jar',
     '--left-classpath', server / 'reference/candidate-50635f7e.jar', '--left-classpath', server / 'reference/base-0.6.71.jar',
     '--classes', entries, '--json', scratch / 'semantic.json', '--md', scratch / 'semantic.md'], 'semantic.log')
data = json.loads((scratch / 'semantic.json').read_text('utf-8'))
assert not data['classSelection']['missingLeft'] and not data['classSelection']['extraLeft']
print(data['counts'])
# A successful comparator process does not mean the batch meets acceptance.
sys.exit(2 if any(data['counts'][k] for k in ['DIFF', 'ONLY_LEFT', 'ONLY_RIGHT']) else 0)
```

## 逐 entry 結果

全部名稱以前綴 `local/lito381/server/`、後綴 `.class` 表示；下表直接從獨立重建的 JSON 產生。

| Entry | 結果 |
|---|---|
| `NativeAcceptedAreaFreezeMutation` | DIFF |
| `NativeAcceptedAreaFreezeMutation$Group` | SAME |
| `NativeAcceptedAreaFreezeMutation$Npc` | SAME |
| `NativeAcceptedAreaFreezeMutation$Output` | SAME |
| `NativeAcceptedAreaFreezeMutation$Player` | SAME |
| `NativeAcceptedFreezeMutation` | SAME |
| `NativeAcceptedFreezeMutation$Batch` | SAME |
| `NativeAcceptedFreezeMutation$Linked` | SAME |
| `NativeAcceptedFreezeMutation$Output` | SAME |
| `NativeAcceptedShockMutation` | SAME |
| `NativeAcceptedShockMutation$Output` | SAME |
| `NativeAcceptedShockMutation$Plan` | SAME |
| `NativeBattleControlledCancellation` | DIFF |
| `NativeBattleControlledCancellation$Output` | SAME |
| `NativeBattleControlledCancellation$Scope` | SAME |
| `NativeBattleTamedLifecycle` | SAME |
| `NativeBattleTamedLifecycle$CaptureRollback` | SAME |
| `NativeBlindSourceState` | SAME |
| `NativeCompanionArea` | SAME |
| `NativeCompanionCombatScene` | SAME |
| `NativeCompanionEarth` | SAME |
| `NativeCompanionExpiry` | SAME |
| `NativeCompanionExpiry$Due` | SAME |
| `NativeCompanionForestCancellation` | SAME |
| `NativeCompanionLanding` | SAME |
| `NativeCompanionLanding$Terrain` | SAME |
| `NativeCompanionManaTransfer` | SAME |
| `NativeCompanionNature` | SAME |
| `NativeCompanionNegative` | SAME |
| `NativeCompanionPlacement` | SAME |
| `NativeCompanionPlacement$Removal` | SAME |
| `NativeCompanionPlacement$Scene` | SAME |
| `NativeCompanionPlacement$State` | SAME |
| `NativeCompanionPlacement$Transition` | SAME |
| `NativeCompanionPlacement$Viewer` | SAME |
| `NativeCompanionProcScene` | SAME |
| `NativeCompanionReflectionScene` | SAME |
| `NativeCompanionResurrection` | SAME |
| `NativeCompanionResurrection$Corpse` | SAME |
| `NativeCompanionResurrection$NpcCorpse` | SAME |
| `NativeCompanionRoleIdentity` | SAME |
| `NativeCompanionRoleIdentity$Epoch` | SAME |
| `NativeCompanionRoleIdentity$Role` | SAME |
| `NativeCompanionTargets` | SAME |
| `NativeCompanionTargets$Death` | SAME |
| `NativeCompanionTargets$Scope` | SAME |
| `NativeCompanionWindState` | SAME |
| `NativeCreateZombieLifecycle` | SAME |
| `NativeCreateZombieLifecycle$Stage` | SAME |
| `NativeHpCompletionAdmission` | SAME |
| `NativeNpcWeaponBreakActions` | SAME |
| `NativeNpcWeaponBreakAreaActions` | SAME |
| `NativeNpcWeaponBreakDamage` | SAME |
| `NativeResurrectionQueueLife` | SAME |
| `NativeRetiredReuseTimer` | SAME |
| `NativeRetiredReuseTimer$Retired` | SAME |
| `NativeSelectedPlayerIntent` | SAME |
| `NativeSelectorOnlyCtrl` | SAME |
| `NativeShockCompanionAction` | SAME |
| `NativeShockCompanionAction$Death` | SAME |
| `NativeShockPlayerAction` | SAME |
| `NativeShockPlayerAction$Linked` | SAME |
| `NativeShockPlayerAction$Scope` | SAME |
| `NativeSkillTargetMetadata` | SAME |
| `NativeSkillTargetMetadata$Row` | SAME |
| `NativeSourceAcceptedPlayers` | SAME |
| `NativeSourceAcceptedPlayers$Participant` | SAME |
| `NativeSourceAcceptedPlayers$Proof` | SAME |
| `NativeSourceAreaCasterTargets` | SAME |
| `NativeSourceAreaCasterTargets$Cursor` | SAME |
| `NativeSourceAreaCasterTargets$Selection` | SAME |
| `NativeSourceAreaClassSelection` | SAME |
| `NativeSourceAreaClassSelection$Cursor` | SAME |
| `NativeSourceAreaPlayerProof` | SAME |
| `NativeSourceArenaPoison` | SAME |
| `NativeSourceArenaPoison$Lease` | SAME |
| `NativeSourceArenaPoison$Output` | SAME |
| `NativeSourceArenaPoison$Proof` | SAME |
| `NativeSourceArenaPoison$Viewer` | SAME |
| `NativeSourceBarrierTargets` | SAME |
| `NativeSourceCompanionClock` | SAME |
| `NativeSourceCompanionElemental` | SAME |
| `NativeSourceCompanionElemental$Lease` | SAME |
| `NativeSourceCompanionFirstActor` | SAME |
| `NativeSourceCompanionFirstActor$View` | SAME |
| `NativeSourceControlledAudience` | SAME |
| `NativeSourceControlledAudience$Proof` | SAME |
| `NativeSourceControlledDisplay` | SAME |
| `NativeSourceControlledDisplay$Action` | SAME |
| `NativeSourceControlledDisplay$Output` | SAME |
| `NativeSourceControlledDisplay$Proof` | SAME |
| `NativeSourceControlledDisplay$Target` | SAME |
| `NativeSourceControlledHealing` | SAME |
| `NativeSourceControlledHealing$Frozen` | SAME |
| `NativeSourceControlledHealing$Output` | SAME |
| `NativeSourceControlledHealing$Scope` | SAME |
| `NativeSourceControlledNegativeFinal` | SAME |
| `NativeSourceControlledNegativeFinal$Scope` | SAME |
| `NativeSourceControlledNegativeFinal$Value` | SAME |
| `NativeSourceControlledRoleFrame` | SAME |
| `NativeSourceCubeClock` | SAME |
| `NativeSourceCubeClock$Phase` | SAME |
| `NativeSourceCubeNpc` | SAME |
| `NativeSourceCubeNpc$Commit` | SAME |
| `NativeSourceCubeNpc$Effect` | SAME |
| `NativeSourceCubeNpc$Output` | SAME |
| `NativeSourceCubeNpc$Spawn` | SAME |
| `NativeSourceCubeNpcLease` | SAME |

## 推測

兩個剩餘方法的明確呼叫順序與資料傳遞應與候選一致，但目前工具沒有完成等價證明。本報告不把人工判讀推廣成 108 SAME，也不以此授權 B-land。

## 待驗證／未驗證什麼

- 兩個 DIFF 的裁定與修復後完整 108 SAME 驗收。
- 單元測試、整合與回歸依 §6-5 不適用本批暫存；由 B-land 統一處理。
- 未跑遊戲／登入真機測試，未部署。本輪一般 CI 不視為 staged 驗收證據。

## 產出路徑與給下一棒的提示

- 交付：`lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/` 中 B02 清單的 51 檔；`docs/reports/issue-63.md`。
- 本機證據（不入 git）：`scratch/issue-63/reproduce/` 的 `javac.args`、三份 log、`entries.txt`、`semantic.json`／`.md`；`scratch/issue-63/full.diff`、`all-methods.log`、`normalized-compiled-7/`、`cfr/`。
- 產物（不入 git）：`lito伺服器/LiTo381-Server/target/issue-63/reproduce/` 的 classes／compiled.jar／annotations／semantic-tool；`target/issue-63/reference-input/` 只供 CFR 讀取，從未複製到編譯輸出。
- 下一棒由 Claude 主控依上方兩方法差異裁定；在工具或 Java 寫法修妥前保持 `needs:claude`，勿標 done 或併入 src/main。若修比較器，須保留多來源分支互換的反例守門，不能直接恢復來源 union 放行。
- 合併與正式驗收仍由既有流程執行；不需使用者手動排查本機編譯環境。

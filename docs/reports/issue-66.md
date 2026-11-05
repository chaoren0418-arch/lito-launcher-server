# Issue #66：T020-B05 戰鬥併入暫存

**結論：尚未通過驗收，需 `needs:claude` 裁定。** 2026-11-05 依 #135／#153／#166 合併後工具重新編譯全部 48 份 Java，轉換後的 120 個 entry 為 **115 SAME／3 SAME_EFFECTS／2 DIFF**，無缺少或多出。剩下 `NativeSourcePoisonNpc` 與 `NativeStaticNpcScene`，不得據此落地 B-land。

## 做了什麼

- 在既有 `task/66` worktree 合併 `origin/main`（`450178d59b0157fddf2b9456b7c03d20207993fc`），合併提交 `79cb933902486882afb4682b7e8dea8b8ae0d9f1`；沒有 checkout 或建立分支。
- 沿用本分支以前由凍結 `merge-inputs/50635f7e/sources/` 起步、對照 CFR 移植的 48 份 staged Java。本輪重跑 CFR 0.152 分析指定 25 個 asm-only entry 所屬 13 個外層家族，輸出與驗收 classes 分開。
- 依 [#66 主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/66#issuecomment-6643955347)，`NativeSourceRawDamage.Member` 加 `@AsmFinal({"nativeSourceNpcHpObserver"})`，來源欄位改為非 final transient；三／五參數建構子移除額外 null 寫入，六參數改回 `this(五參數)` 後寫入 observer。轉換後本 entry 為 SAME。
- 依同一裁定移除 `NativeSourcePoisonNpc.status` 的常數 false 死分支，不再為保留合成方法加入不可執行的 lambda。候選留存的孤立舊方法因此顯示為 DIFF，詳見下文及完整差異。
- 保留既有 `NativeSourceRawDamage.Output` 的 `@AsRecord` 表達。沒有改工具、核准清單、src/main、pom.xml 或 docs/REPORT.md；這些檔案的上游變更僅隨 main 合併進來。

整批 staged 與凍結來源逐檔文字比較（統一 CRLF／LF）：30 檔相同、18 檔有移植差異。本分支相對 main 的交付仍限 48 份 staged Java 與本報告。

最終範圍檢查精確符合上述 49 檔；`git diff origin/main --check` 通過，交付檔案沒有命中 ignore 規則。診斷完成後重核 classes／JAR 雜湊，仍與驗收收據一致。

## 已驗證

工作目錄：`D:/lineage開發/3-lito開發/scratch/wt/task-66`。Windows、Microsoft OpenJDK／javac 17.0.17、ASM 9.7、CFR 0.152。比較器與 converter 均由合併後 Java 來源重建；不用舊版工具 classes。ASM 的三份 JAR 均核對工具規定 SHA-256。

| 階段 | SAME | SAME_EFFECTS | DIFF | 缺少／多出 |
|---|---:|---:|---:|---:|
| 修改前，同一新版工具，預設 | 117 | 0 | 3 | 0／0 |
| 修改前，同一新版工具，既有核准 | 115 | 3 | 2 | 0／0 |
| 修改後，預設 | 117 | 0 | 3 | 0／0 |
| 修改後，既有核准（正式本輪驗收） | 115 | 3 | 2 | 0／0 |

總數不變但差異內容不同：`Member` 由 DIFF → SAME；移除禁止的死分支後 `PoisonNpc` 由 SAME → DIFF。`Scene` 一直為 DIFF。不能只看計數推定未修正。

```text
javac --release 17 -encoding UTF-8 -cp <candidate;base;lib/*;annotation> -d <classes> <全部 48 份 staged Java>
exit 0
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
as-record: scanned=120 converted=2
as-record exit 0
PROVENANCE PASS; missing=0 extra=0
SUMMARY SAME=115 SAME_EFFECTS=3 SAME_LAYOUT=0 DEVIATION=0 DIFF=2 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0 ALLOW_METHODS=0
semantic-diff exit 0（CLI 有 DIFF 仍回傳 0；驗收不通過）
CFR exit 0; families 13
```

逐 entry 比較轉換前後 SHA-256，只有 `NativeSourceRawDamage$Member.class` 與 `$Output.class` 改變，其餘 118 個 javac 產物 bytes 不變。沒有複製候選 class 進驗收輸出、改名或篩掉 entry。JAR 打包 classes 目錄全部 120 個檔案，逐一核對 JAR bytes 與目錄 bytes 相等；轉換前、後、JAR 三份集合都精確等於 B05 `verify_entries`。兩份註解先單獨編到 annotation classpath，沒有混進本批 classes。

| 證據 | SHA-256 |
|---|---|
| candidate-50635f7e.jar | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| base-0.6.71.jar | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| 本輪 batch.jar | `b8d54f8315093327943495503edb9e7dbb0a725d73ea10da012d37738f1d52e4` |
| approved.json | `9b0e77d04399b50cb903205a286919085c5f9b58cb956c92bd364f6fd3bf59cb` |
| provenance.json | `c983e88a84c6c0b858f46347cd1a1d956110687ac7ac5bf48f1bac5234518259` |

`--effects-methods` 僅使用 main 現有清單與 B05 精確交集（7 個方法），沒有新增授權。SAME_EFFECTS 三個 entry 為 `NativeSourceProcScalarSkills`、`NativeSourceSignedRecovery`、`NativeSourceWindTargets`；其中 ProcScalarSkills／WindTargets 在預設比對已 SAME，核准模式按工具規則標為 SAME_EFFECTS。其餘四個核准方法屬 `NativeStaticNpcScene`，仍全部 DIFF。未使用 layout、deviation、額外方法或 metadata 豁免。

## 剩餘差異：已驗證事實與待裁定

### NativeSourcePoisonNpc

唯一完整正規化差異是候選獨有的 private static 方法：

```text
lambda$status$1(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativeSourcePoisonNpc$Status;Llocal/lito381/server/ClientSession;[BLjava/lang/String;)V
access=0x100a；Exceptions=[java/io/IOException]
正規化名=lambda$content$c4e8b60c73c86ed4ed39d482ee598f2b8112af59d91160352508a44376723fb8
```

以新版 `JarIndex.otherReference` 掃描完整候選 JAR 的直接呼叫與 method-handle／bootstrap／常數參照，結果 `referenced=false`。這不代表證明不存在反射；沒有執行伺服器。原始 `status` 已改為 `NativeSourceElementTransport.task(...)`；其餘正規化內容沒有差異。

**待裁定**：#153 的 lambda 內容合併不會刪除右側獨有且內容不同的孤立方法；既有 `extra-methods` 規則只處理左側額外方法，也未核准本方法。本輪遵照主控移除死分支，不自行把這個方法重新加入 Java 或刪掉候選方法。請主控決定右側孤立私有方法的精確驗收方式。這是「完整方法集合與禁止死碼」的衝突，不是聲稱 Java 無法書寫該方法本體。

### NativeStaticNpcScene

完整差異僅在 `heading`、`transform`、`restore`、`turn`；`prune`／`nativeStaticRespawnOriginalPrune` 已無差異。CFR 對這些 monitor 方法警告 `Removed try catching itself - possible behaviour change`；transform／restore 還出現 `MONITOREXIT @DISABLED`。本輪沒有照抄會把 callback 移入鎖內的反編譯語法。

| 方法 | 候選與 Java 的形狀／本輪比較器第一差異 |
|---|---|
| heading | 候選把讀到的 int 留在 operand stack，MONITOREXIT 後立即呼叫 Motion.heading，再 return；Java 用 result 暫存，跳過編譯器的 monitor catch handler 後呼叫。effects 線性序列先遇左側 handler ASTORE，右側先遇 callback。 |
| turn | 候選 true 留在 stack，MONITOREXIT 後 callback／return 在 handler 前；Java 正常路徑跳過 handler 後 callback。effects 同樣在 ASTORE 對 callback 處失敗。 |
| transform | 候選失敗與成功路徑各有 MONITOREXIT → changed → IRETURN，Java 在鎖內記 result、鎖外共用 changed。effects 在左側成功路徑的 instance 呼叫對右側失敗路徑的 MONITOREXIT 處失敗；try 範圍亦分段不同。 |
| restore | 同 transform，另外成功路徑有 actor 清除與 trim；候選每個正常 return 都在解鎖後呼叫 changed，Java 在鎖外共同返回。 |

**推測**：現有 Java 的正常路徑仍維持「鎖內判定／更新，解鎖後 callback」；例外路徑不做 callback。差異可能由帶 monitor 的返回值運送與 CFG 排列造成，但目前 #166 的保守證明與既有 effects 序列未通過，不能宣告等價。

**待裁定**：Java 沒有在一般方法內直接發出 MONITOREXIT 的語法；把 return callback 寫進 synchronized 會在解鎖前呼叫，改用無條件 finally 又會在例外時額外呼叫，兩者均不採用。為保持既有鎖外語意，保留可讀的 result 暫存版本，未新增 helper／死分支或修改工具。本差異包含 monitor／try 與 CFG，超出 [#62 通用規則 B](https://github.com/chaoren0418-arch/lito-launcher-server/issues/62#issuecomment-6644529572) 自列 effects 的範圍；請主控提供可接受的 Java 表達或工具後續裁定。

## 未驗證

- 兩個 DIFF entry 尚未得到新裁定，120 entry 全 SAME／SAME_EFFECTS 的驗收未完成。
- 不適用本批的單元／行為回歸未執行，交 B-land／T020-close 統一處理；沒有啟動遊戲、登入器、伺服器或連接 DB。
- 本輪沒有覆蓋正式 JAR、資源、封存、機密設定或原始 repo 工作檔；Git 共用中繼資料依 worktree 合併／提交需要寫入。

## 產出路徑與給下一棒的提示

- 交付來源：`lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/` 的 B05 48 檔。
- 交付報告：`docs/reports/issue-66.md`；完整差異直接收入本檔，不依賴未追蹤 scratch 才能審查。
- 本機重編證據：`lito伺服器/LiTo381-Server/target/issue-66-corrected-current/`（batch.jar、commands.json、javac.log、as-record.log、approved.json／md、default.json／md、provenance.json）。
- 暫存分析：`scratch/issue-66/verify-current.py`、`inspect-current.py`、`DumpCurrent.java`、`cfr-current/`、`current-left/`、`current-right/`；不提交。
- 下一棒由 Claude 主控裁定孤立 lambda 與四個 Scene 方法。主控裁定或工具依賴合併後，本機執行器以新的 target 子目錄重編全部 48 檔、轉換、重驗全部 120 entry，才可交 B-land。不可用 CI 綠或 semantic-diff exit 0 代替本批驗收。

## 可重現驗收

將以下腳本存為本 worktree `scratch/issue-66/verify-current.py`，執行：

```powershell
& D:/dev-orchestrator/tools/python/python.exe -B -X utf8 scratch/issue-66/verify-current.py review-current
```

最後參數決定新的 `target/issue-66-<參數>` 目錄；既有目錄會拒絕，不覆寫或刪除產物。`run` 的 exit 0 只代表指令成功，必須檢查輸出的 counts 與 classSelection；本提交預期核准結果為 115／3／2，不能當通過。

```python
import hashlib, json, os, subprocess, sys, urllib.request, zipfile
from pathlib import Path
sys.dont_write_bytecode = True
root = Path(__file__).resolve().parents[2]
server = root / 'lito伺服器/LiTo381-Server'
work = root / 'scratch/issue-66'
out = server / 'target' / ('issue-66-' + sys.argv[1])
out.mkdir(parents=True, exist_ok=False)
os.environ['JAVA_HOME'] = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot'
os.environ['AS_RECORD_MAVEN_REPO'] = 'D:/dev-cache/maven/repository'
os.environ['TEMP'] = os.environ['TMP'] = str(out)
sys.path.insert(0, str(server / 'tools/as-record'))
import as_record as tool
b = next(b for b in json.loads((root / 'docs/combat-merge/batches.json').read_text('utf-8'))['batches'] if b['id'] == 'B05')
candidate = server / 'reference/candidate-50635f7e.jar'
expected = set(b['verify_entries'])
entries = work / 'entries-current.txt'
entries.write_text('\n'.join(b['verify_entries']) + '\n', encoding='utf-8')
staged = server / 'merge-inputs/50635f7e/staged/local/lito381/server'
classes = out / 'classes'
commands = []
def run(args, name):
    args = list(map(str, args))
    commands.append(args)
    p = subprocess.run(args, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    log = p.stdout.decode('utf-8', errors='replace')
    (out / (name + '.log')).write_text(log, encoding='utf-8')
    print(name, 'exit', p.returncode, log[-3000:])
    (out / 'commands.json').write_text(json.dumps(commands, ensure_ascii=False, indent=2), encoding='utf-8')
    assert p.returncode == 0
def compile(sources, dest, cp, name):
    dest.mkdir(parents=True, exist_ok=False)
    run([tool.jdk('javac'), '--release', '17', '-encoding', 'UTF-8', '-cp', cp, '-d', dest, *sources], name)
compile([server / ('src/main/java/local/lito381/server/' + n + '.java') for n in ['AsRecord','AsmFinal']], out / 'annotation', '', 'annotation')
cp = os.pathsep.join(map(str, [candidate, server / 'reference/base-0.6.71.jar', server / 'lib/*', out / 'annotation']))
compile([staged / (n + '.java') for n in b['classes']], classes, cp, 'javac')
def hashes():
    return {p.relative_to(classes).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in classes.rglob('*.class')}
before = hashes()
run([tool.jdk('java'), '-cp', tool.build(), 'local.lito381.asrecord.AsRecord', classes], 'as-record')
after = hashes()
changed = sorted(e for e in before if before[e] != after[e])
assert set(before) == set(after) == expected
assert set(changed) <= {'local/lito381/server/NativeSourceRawDamage$Output.class', 'local/lito381/server/NativeSourceRawDamage$Member.class'}
print('PROVENANCE PASS; missing=0 extra=0; transformed', changed)
(out / 'provenance.json').write_text(json.dumps({'before':before,'after':after,'converted':changed},indent=2),encoding='utf-8')
jar = out / 'batch.jar'
with zipfile.ZipFile(jar, 'w') as z:
    for p in sorted(classes.rglob('*.class')):
        info = zipfile.ZipInfo(p.relative_to(classes).as_posix(), (2026, 11, 5, 0, 0, 0))
        z.writestr(info, p.read_bytes())
with zipfile.ZipFile(jar) as z:
    assert set(z.namelist()) == expected
    assert all(z.read(e) == (classes / e).read_bytes() for e in expected)
analysis = server / 'tools/semantic-diff/target/dependency/asm-analysis-9.7.jar'
if not analysis.exists():
    data = urllib.request.urlopen('https://repo.maven.apache.org/maven2/org/ow2/asm/asm-analysis/9.7/asm-analysis-9.7.jar', timeout=30).read()
    analysis.parent.mkdir(parents=True, exist_ok=True)
    analysis.write_bytes(data)
assert hashlib.sha256(analysis.read_bytes()).hexdigest() == '7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f'
deps = tool.dependencies() + os.pathsep + str(analysis)
compile(sorted((server / 'tools/semantic-diff/src/main/java').rglob('*.java')), out / 'semantic-diff', deps, 'semantic-compile')
effects = work / 'effects-current.txt'
selected = [line for line in (root / 'docs/combat-merge/effects-approved.txt').read_text('utf-8-sig').splitlines() if line.split('#')[0] + '.class' in expected]
effects.write_text('\n'.join(selected) + '\n', encoding='utf-8')
for mode in ['default', 'approved']:
    args = [tool.jdk('java'), '-cp', str(out / 'semantic-diff') + os.pathsep + deps, 'local.lito381.semanticdiff.SemanticDiff', '--left', jar, '--right', candidate, '--classes', entries, '--left-classpath', candidate, '--json', out / (mode + '.json'), '--md', out / (mode + '.md')]
    if mode == 'approved': args += ['--effects-methods', effects]
    run(args, mode)
    result = json.loads((out / (mode + '.json')).read_text('utf-8'))
    print(mode, result['counts'], result['classSelection'])
print('JAR SHA256', hashlib.sha256(jar.read_bytes()).hexdigest())
```

## 完整正規化差異

以下由本輪 classes 打包 JAR（左）對完整候選 JAR（右）重新產生，未裁掉任何差異區塊。使用同版 `JarIndex.readTree`、`ClassComparer.methodMap`／`instructions`／`tryCatch`，含 #135／#153／#166 正規化及同一 effects 精確方法名設定；同內容 lambda 已合併，SYNTHETIC 已遮罩，仍完整保留 monitor、try 表與 callback 順序。每個 diff 的標籤與 local 編號是正規化輸出，不是原始 bytecode offset。此處列出全部結構／指令差異，不把 effects 摘要當成完整指令。

### NativeSourcePoisonNpc

```diff
--- staged/NativeSourcePoisonNpc
+++ candidate/NativeSourcePoisonNpc
@@ -1878,6 +1878,60 @@
 ATHROW
 LABEL L6
 RETURN
+METHOD lambda$content$c4e8b60c73c86ed4ed39d482ee598f2b8112af59d91160352508a44376723fb8(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativeSourcePoisonNpc$Status;Llocal/lito381/server/ClientSession;[BLjava/lang/String;)V
+access=0xa signature=null exceptions=[java/io/IOException]
+annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
+TRY L1 L3 L4 java/lang/Throwable
+TRY L5 L6 L4 java/lang/Throwable
+TRY L0 L7 L8 java/lang/Throwable
+ALOAD 0
+GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.nativeSourcePoisonNpcOutput:Ljava/lang/Object;
+ASTORE 5
+ALOAD 0
+ALOAD 1
+LABEL L0
+PUTFIELD local/lito381/server/NativeAreaMaterialAction$Context.nativeSourcePoisonNpcOutput:Ljava/lang/Object;
+ALOAD 2
+DUP
+ASTORE 6
+MONITORENTER
+ALOAD 1
+LABEL L1
+INVOKESTATIC local/lito381/server/NativeSourcePoisonNpc.current(Llocal/lito381/server/NativeSourcePoisonNpc$Status;)Z
+IFEQ L2
+ALOAD 0
+ALOAD 2
+ALOAD 3
+ALOAD 4
+INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H5 local/lito381/server/ClientSession.nativeSelfOriginalSend([BLjava/lang/String;)V, type:()V]
+INVOKESTATIC local/lito381/server/NativeAreaMaterialAction.owned(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativeSelfCastAction$Task;)V
+LABEL L2
+ALOAD 6
+MONITOREXIT
+LABEL L3
+GOTO L7
+LABEL L4
+ASTORE 7
+ALOAD 6
+LABEL L5
+MONITOREXIT
+LABEL L6
+ALOAD 7
+ATHROW
+LABEL L7
+ALOAD 0
+ALOAD 5
+PUTFIELD local/lito381/server/NativeAreaMaterialAction$Context.nativeSourcePoisonNpcOutput:Ljava/lang/Object;
+GOTO L9
+LABEL L8
+ASTORE 8
+ALOAD 0
+ALOAD 5
+PUTFIELD local/lito381/server/NativeAreaMaterialAction$Context.nativeSourcePoisonNpcOutput:Ljava/lang/Object;
+ALOAD 8
+ATHROW
+LABEL L9
+RETURN
 METHOD lawful(Llocal/lito381/server/ClientSession;ILjava/lang/String;Llocal/lito381/server/NativeSourcePoisonNpc$Commit;)V
 access=0x8 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
```

### NativeStaticNpcScene

```diff
--- staged/NativeStaticNpcScene
+++ candidate/NativeStaticNpcScene
@@ -244,26 +244,22 @@
 GETFIELD local/lito381/server/NativeStaticNpcScene$Actor.heading:Ljava/lang/Integer;
 INVOKEVIRTUAL java/lang/Integer.intValue()I
 LABEL L4
-ISTORE 5
 ALOAD 3
 MONITOREXIT
 LABEL L5
-GOTO L9
+ALOAD 0
+ALOAD 1
+ALOAD 2
+INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.heading(ILlocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClientSession;)I
+IRETURN
 LABEL L6
-ASTORE 6
+ASTORE 5
 ALOAD 3
 LABEL L7
 MONITOREXIT
 LABEL L8
-ALOAD 6
-ATHROW
-LABEL L9
-ILOAD 5
-ALOAD 0
-ALOAD 1
-ALOAD 2
-INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.heading(ILlocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClientSession;)I
-IRETURN
+ALOAD 5
+ATHROW
 METHOD inScene(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;II)Z
 access=0x8 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
@@ -463,8 +459,9 @@
 METHOD restore(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/ClientSession;)Z
 access=0x8 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-TRY L1 L6 L7 java/lang/Throwable
-TRY L8 L9 L7 java/lang/Throwable
+TRY L1 L3 L8 java/lang/Throwable
+TRY L5 L7 L8 java/lang/Throwable
+TRY L9 L10 L8 java/lang/Throwable
 ALOAD 1
 ALOAD 3
 INVOKESTATIC local/lito381/server/NativeStaticNpcScene.scoped(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClientSession;)Z
@@ -473,15 +470,13 @@
 ALOAD 1
 ILOAD 2
 INVOKEVIRTUAL local/lito381/server/GameHub.restoreClassicWorldNpcGfx(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;I)Z
-ISTORE 4
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.changed(Llocal/lito381/server/GameHub;)V
-ILOAD 4
-IRETURN
-LABEL L0
-ALOAD 0
-DUP
-ASTORE 5
+IRETURN
+LABEL L0
+ALOAD 0
+DUP
+ASTORE 4
 MONITORENTER
 ALOAD 1
 LABEL L1
@@ -505,54 +500,54 @@
 ALOAD 3
 INVOKESTATIC local/lito381/server/NativeStaticNpcScene.gfx(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClientSession;)I
 ILOAD 2
-IF_ICMPEQ L3
-LABEL L2
-CONST Integer:0
-ISTORE 6
-GOTO L5
-LABEL L3
-ALOAD 0
-ALOAD 1
-ALOAD 3
+IF_ICMPEQ L4
+LABEL L2
+CONST Integer:0
+ALOAD 4
+MONITOREXIT
+LABEL L3
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.changed(Llocal/lito381/server/GameHub;)V
+IRETURN
+LABEL L4
+ALOAD 0
+ALOAD 1
+ALOAD 3
+LABEL L5
 INVOKESTATIC local/lito381/server/NativeStaticNpcScene.instance(Llocal/lito381/server/ClientSession;)I
 CONST Integer:0
 INVOKESTATIC local/lito381/server/NativeStaticNpcScene.actor(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;IZ)Llocal/lito381/server/NativeStaticNpcScene$Actor;
-ASTORE 7
-ALOAD 7
-IFNULL L4
-ALOAD 7
+ASTORE 5
+ALOAD 5
+IFNULL L6
+ALOAD 5
 CONST null
 PUTFIELD local/lito381/server/NativeStaticNpcScene$Actor.gfx:Ljava/lang/Integer;
-ALOAD 7
+ALOAD 5
 CONST null
 PUTFIELD local/lito381/server/NativeStaticNpcScene$Actor.appearance:Llocal/lito381/server/NativeCancellationNpcStatic$Appearance;
 ALOAD 0
 ALOAD 1
 ALOAD 3
 INVOKESTATIC local/lito381/server/NativeStaticNpcScene.instance(Llocal/lito381/server/ClientSession;)I
-ALOAD 7
+ALOAD 5
 INVOKESTATIC local/lito381/server/NativeStaticNpcScene.trim(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/NativeStaticNpcScene$Actor;)V
-LABEL L4
+LABEL L6
 CONST Integer:1
-ISTORE 6
-LABEL L5
-ALOAD 5
-MONITOREXIT
-LABEL L6
-GOTO L10
+ALOAD 4
+MONITOREXIT
 LABEL L7
-ASTORE 8
-ALOAD 5
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.changed(Llocal/lito381/server/GameHub;)V
+IRETURN
 LABEL L8
-MONITOREXIT
+ASTORE 6
+ALOAD 4
 LABEL L9
-ALOAD 8
-ATHROW
+MONITOREXIT
 LABEL L10
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.changed(Llocal/lito381/server/GameHub;)V
-ILOAD 6
-IRETURN
+ALOAD 6
+ATHROW
 METHOD scoped(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClientSession;)Z
 access=0x8 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
@@ -607,8 +602,9 @@
 METHOD transform(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;IILlocal/lito381/server/ClientSession;)Z
 access=0x8 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-TRY L1 L5 L6 java/lang/Throwable
-TRY L7 L8 L6 java/lang/Throwable
+TRY L1 L3 L7 java/lang/Throwable
+TRY L5 L6 L7 java/lang/Throwable
+TRY L8 L9 L7 java/lang/Throwable
 ALOAD 1
 ALOAD 4
 INVOKESTATIC local/lito381/server/NativeStaticNpcScene.scoped(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClientSession;)Z
@@ -618,15 +614,13 @@
 ILOAD 2
 ILOAD 3
 INVOKEVIRTUAL local/lito381/server/GameHub.transformClassicWorldNpcGfx(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;II)Z
-ISTORE 5
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.changed(Llocal/lito381/server/GameHub;)V
-ILOAD 5
-IRETURN
-LABEL L0
-ALOAD 0
-DUP
-ASTORE 6
+IRETURN
+LABEL L0
+ALOAD 0
+DUP
+ASTORE 5
 MONITORENTER
 ALOAD 1
 LABEL L1
@@ -653,15 +647,20 @@
 ALOAD 4
 INVOKESTATIC local/lito381/server/NativeStaticNpcScene.gfx(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClientSession;)I
 ILOAD 2
-IF_ICMPEQ L3
-LABEL L2
-CONST Integer:0
-ISTORE 7
-GOTO L4
-LABEL L3
-ALOAD 0
-ALOAD 1
-ALOAD 4
+IF_ICMPEQ L4
+LABEL L2
+CONST Integer:0
+ALOAD 5
+MONITOREXIT
+LABEL L3
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.changed(Llocal/lito381/server/GameHub;)V
+IRETURN
+LABEL L4
+ALOAD 0
+ALOAD 1
+ALOAD 4
+LABEL L5
 INVOKESTATIC local/lito381/server/NativeStaticNpcScene.instance(Llocal/lito381/server/ClientSession;)I
 CONST Integer:1
 INVOKESTATIC local/lito381/server/NativeStaticNpcScene.actor(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;IZ)Llocal/lito381/server/NativeStaticNpcScene$Actor;
@@ -669,25 +668,20 @@
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 PUTFIELD local/lito381/server/NativeStaticNpcScene$Actor.gfx:Ljava/lang/Integer;
 CONST Integer:1
-ISTORE 7
-LABEL L4
+ALOAD 5
+MONITOREXIT
+LABEL L6
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.changed(Llocal/lito381/server/GameHub;)V
+IRETURN
+LABEL L7
+ASTORE 6
+ALOAD 5
+LABEL L8
+MONITOREXIT
+LABEL L9
 ALOAD 6
-MONITOREXIT
-LABEL L5
-GOTO L9
-LABEL L6
-ASTORE 8
-ALOAD 6
-LABEL L7
-MONITOREXIT
-LABEL L8
-ALOAD 8
-ATHROW
-LABEL L9
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.changed(Llocal/lito381/server/GameHub;)V
-ILOAD 7
-IRETURN
+ATHROW
 METHOD trim(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/NativeStaticNpcScene$Actor;)V
 access=0x8 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
@@ -786,10 +780,16 @@
 ILOAD 2
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 PUTFIELD local/lito381/server/NativeStaticNpcScene$Actor.heading:Ljava/lang/Integer;
-ALOAD 4
-MONITOREXIT
-LABEL L4
-GOTO L8
+CONST Integer:1
+ALOAD 4
+MONITOREXIT
+LABEL L4
+ALOAD 0
+ALOAD 1
+ILOAD 2
+ALOAD 3
+INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.turned(ZLlocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/ClientSession;)Z
+IRETURN
 LABEL L5
 ASTORE 5
 ALOAD 4
@@ -798,11 +798,3 @@
 LABEL L7
 ALOAD 5
 ATHROW
-LABEL L8
-CONST Integer:1
-ALOAD 0
-ALOAD 1
-ILOAD 2
-ALOAD 3
-INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.turned(ZLlocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/ClientSession;)Z
-IRETURN
```

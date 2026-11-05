# Issue #63：T020-B02 戰鬥併入暫存

## 結論

**未通過驗收；等待 #174 合併，不能標 done 或交 B-land。** 本次從 `task/63` @ `8313f571f1ebe669d2fae150257cb28c13b9f613` 的既有 51 份 staged Java 乾淨重建，結果仍為 **106 SAME／2 DIFF**；108 個 entry 無缺漏、多出。本次只更新本報告，沒有修改 Java 或比較器，也沒有覆蓋、改名驗收產物。

### 本次重派發現的依賴缺口（2026-11-05）

- [主控最新裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/63#issuecomment-6654689575) 已將兩個方法的區塊內暫存／DUP 差異交給 **#174 第 3(b) 項**；要求 #174 合併後再派工，並處理 `players` 的條件運算式。
- 本次讀取 [#174](https://github.com/chaoren0418-arch/lito-launcher-server/issues/174) 時，它仍為 `open`、`task:running`，只有派工留言；尚未合併。遠端 `main` 仍為 `450178d59b0157fddf2b9456b7c03d20207993fc`，與本地工具基準相同。
- #63 本文的 `depends_on` 仍是 `[102, 135, 153, 166]`，沒有 174；這次派工發生在新增前置完成之前。主控裁定已拿掉 `needs:claude`，本次沒有出現新的語意差異，故不重複要求同一項裁定。
- 本次先完成可獨立進行的乾淨重建、逐方法診斷及證據更新；`players` 的 Java 改寫與新版工具驗收依主控指定，留待 #174 合併後一起完成。未使用其他 worktree 的未合併工具或自行豁免。

## 本次做了什麼

1. 讀取根目錄與伺服器規則、`docs/REPORT.md`、設計 §6-4／§6-5、B02 清單、前次報告及 GitHub 最新裁定。
2. 在全新 `target/issue-63/retry-2/classes/` 一次編譯 B02 的 51 檔；執行既有 #102 轉換器，再打包全部 108 個 class。
3. 從本分支來源重新編譯 semantic-diff，使用完整 `verify_entries` 比較；未啟用 effects／layout／deviation／allow-methods 豁免。
4. 遍歷全部 entry 的全部方法，重新輸出完整正規化差異；與前次差異逐字相同，沒有新增差異。

未修改 `src/main`、`pom.xml`、`docs/REPORT.md`、正式程式或設定；未啟動伺服器／登入器／遊戲、連 DB、讀寫機密設定或封存。本次沿用 `task/63`，沒有 checkout、另建分支、合併其他分支或強制推送。

## 已有 Java 移植內容（前次提交保留）

- 51 份 Java 均由 `merge-inputs/50635f7e/sources/local/lito381/server/` 起步；用 CFR 0.152 反編譯候選中題述 22 個 entry 所屬的 11 個外層類，逐項移植。另比對後補齊 `NativeSourceAcceptedPlayers`、`NativeSourceAreaClassSelection`（含 Cursor）、`NativeSourceControlledHealing` 的後加 ASM 語意；這三檔也在 B02。
- 補回 freeze／shock／角色識別／controlled display 的原方法及轉呼叫、`playerLife`／`petLife`／NPC first-life、Placement 的 volatile 欄位、受眾快照、技能 metadata 載入與 Cube NPC 佇列／傳輸入口。
- `NativeSourceArenaPoison$Viewer`、`NativeSourceControlledDisplay$Output` 改用 `@AsRecord`，保留元件順序、accessors、額外 transient 欄位與兩組建構子；`finalFields` 由既有轉換器還原，沒有手寫 Object 方法。
- `NativeSourceAreaClassSelection$Cursor` 的舊建構子原本不賦值新 final 欄位，因此採 #153 已合併的 `@AsmFinal({"nativeSourceTargetMask"})`，仍由同一 #102 CLI 處理。沒有改轉換器。該 entry 實測 SAME。
- 候選保留的未引用 `lambda$deliver$0` 以實際 Java 方法保留；它呼叫 `complete`，並非 stub。其餘 lambda 交新版比較器正規化。
- 提交範圍只有 B02 的 51 份 staged `.java` 與本報告。`src/main`、`pom.xml`、`docs/REPORT.md` 的上游更新僅來自快轉，前次相對 `450178d` 無修改。

## 已驗證

環境：Windows、Microsoft OpenJDK 17.0.17、ASM 9.7。工具來源基準已包含 #102、#153、#166，尚不含 #174。前次 CFR 0.152 的移植及原始碼保留；本次沒有重新以反編譯產物覆蓋原始碼。

| 項目 | 本次結果 |
|---|---|
| 一次 javac 編譯 51 份 Java | exit 0；僅 unchecked 提示 |
| 既有 as-record CLI | exit 0；scanned=108 converted=3（2 AsRecord＋1 AsmFinal） |
| class 集合對 B02.verify_entries | 108／108；缺少 0、多出 0 |
| semantic-diff | SAME=106、DIFF=2；其餘結果種類皆 0 |
| 全部方法診斷 | 只有下列兩方法不同 |
| 完整正規化 diff 對前次 | 逐字相同 |
| staged 集合 SHA-256 對前次 | 相同，51 份 Java 均未改動 |

實際輸出（`semantic.json.generatedAt=2026-11-05T09:54:55Z`）：

```text
javac.log exit=0
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
as-record.log exit=0
as-record: scanned=108 converted=3
entries=108 missing=[] extra=[]
semantic.log exit=0
SUMMARY SAME=106 SAME_EFFECTS=0 SAME_LAYOUT=0 DEVIATION=0 DIFF=2 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0 ALLOW_METHODS=0
normalized_diff_identical=True
```

semantic-diff 的 exit 0 只代表報告成功產生；驗證腳本最後另外檢查結果，這次因 DIFF 非零結束。不能據 CLI exit 0 或一般 CI 綠燈宣告 staged 通過。

固定輸入／輸出 SHA-256：

- candidate：`50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`
- base：`e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`
- 本次重建 JAR：`5c945982ef9f9bb9e778aea16e87ffebfed70c9ca691f55edec88936bb580e38`（ZIP 時間戳可能使另次打包雜湊不同）
- 51 份 staged 集合：`c3820786459d0d92039d300201d7caf3e40ea8d2568fddcafa6ea32f128a92d4`（依檔名排序，串接 UTF-8 檔名＋換行＋檔案 SHA-256 原始 32 bytes，再 SHA-256）。

## 未完成差異與理由

剩餘兩個 entry 均位於 `local/lito381/server/`：

1. `NativeAcceptedAreaFreezeMutation.class`：`players(Ljava/util/List;)Ljava/util/List;`。
2. `NativeBattleControlledCancellation.class`：`members(Llocal/lito381/server/ClientSession;I)Ljava/util/List;`。

候選將 `c::player` 的 invokedynamic handle 改為 static `NativeSourceIndirectPlayerProof.player(Context, ClientSession)`，卻保留 bound method reference 的 `DUP; Objects.requireNonNull; POP`。Java 使用 lambda，先執行 `stream()`、再檢查 Context、最後建立 Predicate，以保留呼叫及例外時機。不能刪掉 players 的 null 檢查或改回 `Context.player`。

#166 已把轉呼叫 lambda 收斂到同一 static handle，但尚無法證明這裡的 local／DUP 搬運等價。`players` 另有跨分支 result 暫存，主控要求改為條件運算式；兩方法其餘區塊內暫存由 #174 處理。現在尚未取得 #174 合併後的工具，因此本次不是「新版工具仍失敗」，也不是已證明所有等價 Java 都不可能。

診斷結果：players `localEqual=false singletonLeft=true singletonRight=false`；members `localEqual=false singletonLeft=false singletonRight=false`。沒有恢復多來源 union 放行。

以下為全部剩餘正規化指令差異，由本次重新編譯的 JarIndex／ClassComparer 診斷取得。`-` 是 staged，`+` 是 candidate，未顯示上下文一致；兩方法 access/signature/exceptions 與 try 表一致。其他 106 entry 及其方法均 SAME。

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

從 worktree 根目錄執行；把以下 Python 存成 `scratch/issue-63/verify-retry.py`：

```powershell
& D:/dev-orchestrator/tools/python/python.exe -B -X utf8 scratch/issue-63/verify-retry.py
```

腳本拒絕覆用已存在的 classes 目錄；本機本次目錄已存在，再跑時請把兩個 `retry-2` 路徑改成同一個全新名稱。annotations 只加入 classpath，未混入本批輸出；只有既有 #102 CLI 進行後處理。`--left-classpath` 只供欄位 owner 解析，沒有把依賴混入比對 JAR。

```python
import hashlib, json, os, subprocess, sys, urllib.request, zipfile
from pathlib import Path
root = Path.cwd()
server = root / 'lito伺服器/LiTo381-Server'
scratch = root / 'scratch/issue-63/retry-2'
target = server / 'target/issue-63/retry-2'
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
assert data['comparedClassCount'] == len(expected)
assert len(data['classes']) == len(expected)
sys.exit(0 if all(c['status'] == 'SAME' for c in data['classes']) else 2)

```

## 推測

兩個方法目前的明確呼叫順序與資料傳遞應與候選一致，但現行工具沒有完成等價證明。#174 預期可處理區塊內暫存，並不代表本批已通過；players 改寫後仍須完整重驗。

## 待驗證／未驗證什麼

- #174 合併後，依主控裁定調整 `players`，保留 `stream()` → Context 讀取／null 檢查 → Predicate 建立的順序，重驗完整 108 entries。
- 單元測試、整合與回歸依 §6-5 不適用本批暫存；由 B-land 統一處理。
- 未進行遊戲／登入真機測試，未部署；本次報告提交不以一般 CI 代替 staged 驗收。

## 產出路徑與給下一棒的提示

- 既有交付：`lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/` 中 B02 的 51 份 Java。
- 本次變更：`docs/reports/issue-63.md`。
- 本機證據（不入 git）：`scratch/issue-63/retry-2/` 的 javac.args、javac.log、as-record.log、semantic.log、entries.txt、semantic.json／semantic.md、all-methods.log、normalized/、full.diff；腳本 `scratch/issue-63/verify-retry.py`、`audit-retry.py`。
- 本次建置產物（不入 git）：`lito伺服器/LiTo381-Server/target/issue-63/retry-2/`。
- **調度器／Claude 主控**：請依既有裁定補上 `depends_on: 174`，在 #174 合併前停止重派本批；這是前置尚未完成，不是本機環境偶發或新增 Java 編譯錯誤。
- **#174 合併後的本機執行器**：取得已合併工具版本，完成 players 條件運算式改寫，再從全新目錄一次編譯 51 檔、轉換、核對集合並比較。若仍有差異，附新的完整正規化差異並標 `needs:claude`。不得使用候選 bytes 覆蓋輸出，不得放寬驗收。
- PR #81 保持草稿；達到 108 SAME 前不要標 done、合併或交 B-land。使用者無須手動排查本機環境。

# Issue #63：T020-B02 戰鬥併入暫存

## 結論

本輪 51 份 staged Java 乾淨編譯後，完整 108 個 entry 為 **107 SAME、1 SAME_PATHS、0 DIFF**，missing／extra 皆空，符合本輪指定的語意門檻。`NativeBattleControlledCancellation` 為 SAME；`NativeAcceptedAreaFreezeMutation#players` 為 SAME_PATHS。

**待主控審查一項實作形式差異**：本輪用 null 分支提前回傳取代跨分支 `result` 暫存，沒有使用[主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/63#issuecomment-6654689575)示意的單一條件運算式。直接把右運算元改成 `values.stream().filter(...)` 無法容納現有 `c` 區域變數宣告及 Predicate 建立前的獨立 null 檢查；把檢查塞進 lambda 會延後到串流求值，改用 `c::player` 也會失去 proof。故提交可審查且已通過逐路徑證明的最小替代寫法，不宣稱已逐字完成指定語法。請 Claude 裁定是否接受，再標 done／交 B-land。

## 做了什麼

- 沿用 `task/63`，未 checkout 或另建分支。先合併最新 `origin/main` `c72453957ebe47e00ad024e874bbe31859457799`，合併提交 `c57d301ca43bc0756eb40867c695b800c85b388a`，無衝突；含 #174（PR #178）與 #176（PR #193）。
- 本輪只改 `NativeAcceptedAreaFreezeMutation.java` 的 `players` 與本報告。`docs/REPORT.md`、工具及其他批次的變動僅來自 main 合併，本輪未編輯它們。
- 保留既有 51 檔的候選移植、`@AsRecord`／`@AsmFinal`；`members` 的 Java 完全未改。proof 仍為 `NativeSourceIndirectPlayerProof.player(c,p)`。
- 從全新 `target/issue-63/final-174-176/classes` 編譯，唯一 class 後處理是既有 `tools/as-record` 轉換器。未以 candidate class bytes 覆蓋輸出、未替換或改名驗收產物。
- 未修改 `src/main`、`pom.xml`、設定、封存、正式程式、遊戲資源或玩家資料；未啟動任何伺服器／登入器／遊戲或連 DB。

實際修改：

```java
static List<ClientSession> players(List<ClientSession> values) {
 var g=group();
 if(g==null)return values;
 // 保留候選方法參照在 stream() 之後、建立 Predicate 之前的 null 檢查。
 var stream=values.stream();var c=g.c;Objects.requireNonNull(c);
 return stream.filter(p->NativeSourceIndirectPlayerProof.player(c,p)).toList();
}
```

## 已驗證／怎麼驗證

環境：Windows、Microsoft OpenJDK 17.0.17、ASM 9.7。semantic-diff 從上述已合併來源重新編譯；輸入固定為 `batches.json` B02 的 51 個 `classes` 與完整 108 個 `verify_entries`。

| 項目 | 結果 |
|---|---|
| javac --release 17 -encoding UTF-8 | exit 0；僅 unchecked 提示 |
| as-record CLI | exit 0；scanned=108 converted=3（2 AsRecord＋1 AsmFinal） |
| 實際 class entry 對 verify_entries | 108／108；missing=[]、extra=[] |
| semantic-diff | SAME=107、SAME_PATHS=1；其餘種類皆 0 |
| players 逐路徑證明 | path multisets equal (9) |
| members | SAME；原始碼未改 |
| 核准清單裁切 | 8 份清單對 B02 均 0 條；未傳入空清單、未新增任何豁免 |
| 產物來源 | 全由 javac 與既有 as-record 產生 |

核准清單：effects-approved、extra-methods-approved、metadata-exempt、layout-approved、access-widen-approved、deviations、exceptions-exempt、t029-methods。均依精確類別裁切，沒有適用 B02 的條目；access-widen 的巢狀 Output 屬其他批次。

實際輸出：

```text
javac.log exit=0
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
as-record.log exit=0
as-record: scanned=108 converted=3
entries=108 missing=[] extra=[]
SUMMARY SAME=107 SAME_EFFECTS=0 SAME_PATHS=1 ORPHAN_IGNORED=0 SAME_LAYOUT=0 DEVIATION=0 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0 ALLOW_METHODS=0
ACCEPTED 108 entries
```

驗證腳本另行斷言 comparedClassCount 與 classes 長度皆為 108、class 集合精確一致、每個 status 只可為 SAME／SAME_EFFECTS／SAME_PATHS；沒有只看 semantic-diff exit 0。

javap 的 players：offset 11 為 `List.stream`、18 為 `GETFIELD Group.c`、23 為 `Objects.requireNonNull`、29 為 Predicate invokedynamic、34 為 filter、39 為 toList；null 分支 offset 9 直接 ARETURN，非 null 分支 offset 44 ARETURN。沒有跨分支 result STORE／LOAD。這是 #176 的逐路徑證明，不將 SAME_PATHS 冒稱 SAME。

固定輸入／輸出 SHA-256：

- candidate：`50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`
- base：`e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`
- 重建 JAR：`9cc4d4cbac153d7f13ffea5864fb7ac34994618bd7717c4a2900bd36080f38ef`（ZIP 時間戳可能使另次雜湊不同）
- staged 集合：`45eb934fa8b24913f0f419682ff7081a13c766d1aa64c9741083f3203cff82dc`（檔名排序後串接 UTF-8 檔名＋換行＋各檔 SHA-256 原始 32 bytes，再雜湊）

## 推測

不以人工推測補足語意差異。提前回傳符合移除跨分支 result 暫存的目的，但是否接受它取代指定條件運算式，仍由 Claude 主控審查。

## 待驗證／未驗證什麼

- 主控對上述實作形式差異的裁定；目前未自行標 done。
- 單元測試、整合與回歸依設計 §6-5 不適用本批暫存區，交 B-land 統一執行。
- 未做真機戰鬥／登入驗收，未部署；SAME_PATHS 是有界語意模型證明。

## 產出路徑／給下一棒的提示

- 交付：`lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/` 的既有 B02 51 檔；本輪只改 `NativeAcceptedAreaFreezeMutation.java`。
- 報告：`docs/reports/issue-63.md`（本檔）。
- 本機證據（不入 git）：`scratch/issue-63/final-174-176/` 的 javac.args、javac.log、as-record.log、entries.txt、semantic.log、semantic.json／semantic.md。
- 重現腳本（不入 git）：`scratch/issue-63/verify-final.py`；建置產物：`lito伺服器/LiTo381-Server/target/issue-63/final-174-176/`。
- Claude：請審核提前回傳替代寫法及 107 SAME＋1 SAME_PATHS 收據。若接受，B02 已無語意差異，可由調度器接續合併及 B-land；無需擴大核准清單。

## 重現指令

在 worktree 根目錄執行下列腳本。classes 目錄必須全新；再次執行請把腳本中的 `final-174-176` 改為另一個全新名稱，避免混用既有輸出。annotations 僅供 classpath；兩個 reference 也只供編譯及比對解析，不打進本批 JAR。

```powershell
& D:/dev-orchestrator/tools/python/python.exe -B -X utf8 scratch/issue-63/verify-final.py
```

下列為本次實際驗證腳本（可存回上述 scratch 路徑）：

```python
import hashlib, json, os, subprocess, sys, urllib.request, zipfile
from pathlib import Path
root = Path.cwd()
server = root / 'lito伺服器/LiTo381-Server'
scratch = root / 'scratch/issue-63/final-174-176'
target = server / 'target/issue-63/final-174-176'
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
approved = []
for flag, filename in [
    ('--effects-methods', 'effects-approved.txt'), ('--extra-methods', 'extra-methods-approved.txt'),
    ('--metadata-exempt', 'metadata-exempt.txt'), ('--layout-methods', 'layout-approved.txt'),
    ('--access-widen', 'access-widen-approved.txt'), ('--deviations', 'deviations.txt'),
    ('--exceptions-exempt', 'exceptions-exempt.txt'), ('--allow-methods', 't029-methods.txt')]:
    src = root / 'docs/combat-merge' / filename
    selected = []
    for line in src.read_text('utf-8-sig').splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith('#'): continue
        key = stripped.split()[0]
        if key == 'ctor-prologue-order': key = stripped.split()[1]
        owner = key.split('#')[0].removesuffix('.class')
        if owner + '.class' in expected: selected.append(line)
    print(filename, 'B02 entries=', len(selected))
    if selected:
        out = scratch / filename
        out.write_text('\n'.join(selected) + '\n', 'utf-8')
        approved.extend([flag, out])
        if flag == '--allow-methods': approved.extend(['--allow-right', server / 'reference/base-0.6.71.jar'])
run([*java, '-cp', str(toolclasses) + os.pathsep + dependencies, 'local.lito381.semanticdiff.SemanticDiff',
     '--left', jar, '--right', server / 'reference/candidate-50635f7e.jar',
     '--left-classpath', server / 'reference/candidate-50635f7e.jar', '--left-classpath', server / 'reference/base-0.6.71.jar',
     '--classes', entries, *approved, '--json', scratch / 'semantic.json', '--md', scratch / 'semantic.md'], 'semantic.log')
data = json.loads((scratch / 'semantic.json').read_text('utf-8'))
assert not data['classSelection']['missingLeft'] and not data['classSelection']['extraLeft']
print(data['counts'])
# A successful comparator process does not mean the batch meets acceptance.
assert data['comparedClassCount'] == len(expected) == 108
assert len(data['classes']) == len(expected)
assert all(c['status'] in {'SAME', 'SAME_EFFECTS', 'SAME_PATHS'} for c in data['classes']), data['counts']
print('ACCEPTED 108 entries')
```

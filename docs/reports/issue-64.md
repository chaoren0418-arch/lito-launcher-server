# Issue #64 — T020-B03 戰鬥併入暫存

## 結論

**本批暫存驗收完成：51 份 B03 Java 加上 B08 的 staged `NativeLightAction.java` 一次編譯成功；B03 全部 135 個 entry 均為 SAME，missing／extra 均為空。** 前輪 private Output 阻塞已解除。本輪不需修改任何 Java，保留 `36d3039` 的完整移植成果，只更新本報告。

## 做了什麼

- 於既有 `task/64` 工作，從 `36d3039` fetch 並合併最新 `origin/main` `e8871cd`，產生無衝突合併提交 `7983873`。沒有 checkout、重建分支或改寫歷史。
- main 已包含 #69（PR #94）、#174（PR #178）、#176（PR #193）以及後續比較器修正至 #259。兩個工具均從合併後的來源重新編譯，沒有沿用舊工具 classes。
- 遵循 [主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/64#issuecomment-6654963452)，把 #69 已放寬 Output 的 `NativeLightAction.java` 加進同一次 javac，僅作編譯依賴，未修改該檔。
- 保留前輪 23 個 asm-only entry 的移植、6 個 `@AsRecord`、1 個 `@AsmFinal` 及舊 lambda Java 實作。以 git bytes 核對，51 份 B03 staged Java 與 `36d3039` 完全相同。
- 本輪只直接修改 `docs/reports/issue-64.md`；整個分支相對合併基準的交付仍是 B03 的 51 份 staged Java 與本報告。main 帶入的其他檔案保持原樣；`docs/REPORT.md` 沒有本批修改。

## 已驗證

### 環境與產物來源

- 日期：2026-11-06。Windows、Microsoft OpenJDK 17.0.17、Python 3；工作目錄 `D:/lineage開發/3-lito開發/scratch/wt/task-64`。
- 使用 `javac --release 17 -encoding UTF-8`；classpath 順序為 candidate、base、獨立的 AsRecord／AsmFinal 註解 classes、`lib/*`。本 worktree 的 `lib/*` 沒有額外 JAR。
- 每次執行在本 worktree `target/issue-64/complete-*` 建立全新輸出目錄，避免殘留 class。暫存腳本、參數及 log 只寫入 `scratch/issue-64/`。
- 52 個輸入 Java 產出 140 個 class。唯一 class 後處理為現有 `tools/as-record`，其中本批 7 個類別與 B08 的 `NativeLightAction$Ingress` 共轉換 8 個。
- 轉換完成後，只排除下列 5 個 B08 依賴 class，再將剩餘 135 個原路徑 class 打包。以集合精確核對 `verify_entries`，並核對每個 JAR entry 與轉換器輸出的 SHA-256 相同。
- 沒有把候選 class bytes 寫入產物，沒有替換或重新命名任何驗收 class。工具只讀取固定 reference JAR；沒有啟動伺服器、遊戲或連接 DB。

排除的編譯依賴（共同前綴 `local/lito381/server/`）：

```text
NativeLightAction.class
NativeLightAction$Batch.class
NativeLightAction$Completion.class
NativeLightAction$Ingress.class
NativeLightAction$Output.class
```

### 編譯與語意比對輸出

註解、應用來源、as-record 工具、semantic-diff 工具的 javac 退出碼皆為 0。應用編譯唯一輸出如下，沒有 error；這是既有 CubeVisibility 的 unchecked 提示。

```text
Note: .../NativeSourceCubeVisibility.java uses unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
```

轉換器與比較器退出碼皆為 0：

```text
as-record: scanned=140 converted=8
SUMMARY SAME=135 SAME_EFFECTS=0 SAME_PATHS=0 ORPHAN_IGNORED=0 SAME_CFG=0 LAMBDA_PAIRED_BY_CALLSITE=0 VISIBILITY_BRIDGE=0 SAME_LAYOUT=0 DEVIATION=0 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0 ALLOW_METHODS=0
ACCEPTED: 135 entries; missing=[]; extra=[]; source and class hashes preserved
```

`classSelection.missingLeft=[]`、`extraLeft=[]`。前輪尚未產出的 `NativeSourceCubeVisibility`、`$Cache`、`$Rebuild`、`$Work` 這次全部 SAME。SAME 是比較器正規化後的結果，不代表 class bytes 相同；本輪 BYTE_SAME=0。

### 核准清單

依比較器 README 的批次規則，從 `docs/combat-merge/access-widen-approved.txt` 保留本批會引用的精確條目，寫成 `scratch/issue-64/access-widen-approved.txt`，以 `--access-widen` 傳入：

```text
local/lito381/server/NativeLightAction$Output # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/64#issuecomment-6654963452
```

原清單另有 A 批次的三個方法，直接傳整份會被工具以 outside comparison scope 拒絕；因此使用裁切副本，沒有修改核准原檔。JSON `innerAccessWiden` 精確包含 `local/lito381/server/NativeLightAction$Output`。放寬僅作用於核准的 InnerClasses 條目，其餘比較照常執行。

同時逐一檢查 effects、layout、extra-methods、metadata-exempt、deviations、exceptions-exempt 核准清單，與 B03 的精確類別交集全部為空，因此不傳入空清單，也不啟用額外豁免。T029 的 allow-methods 只適用 B09，本批不使用。

## 怎麼驗證

將下方完整腳本存成本 worktree 的 `scratch/issue-64/verify-complete.py`，於 worktree 根目錄執行。腳本會重新編譯工具、裁切核准清單、檢查全部 135 個 entry 與 missing／extra，遇到失敗即非零退出；不執行應用程式。

```powershell
& D:/dev-orchestrator/tools/python/python.exe -X utf8 scratch/issue-64/verify-complete.py
```

等價的應用編譯與語意比對核心指令（完整絕對路徑及 52 份來源由腳本產生）：

```text
javac -J-Dfile.encoding=UTF-8 --release 17 -encoding UTF-8 -cp "reference/candidate-50635f7e.jar;reference/base-0.6.71.jar;<annotations>;lib/*" -d <classes> <51 份 B03 staged Java> <B08 staged NativeLightAction.java>
java -cp <as-record-tool及ASM> local.lito381.asrecord.AsRecord <classes>
# 排除 5 個 B08 依賴 class，核對剩餘集合恰好為 135 個 verify_entries，再打包 B03.jar。
java -cp <semantic-diff-tool及ASM> local.lito381.semanticdiff.SemanticDiff --left <B03.jar> --right reference/candidate-50635f7e.jar --classes scratch/issue-64/entries.txt --access-widen scratch/issue-64/access-widen-approved.txt --left-classpath reference/candidate-50635f7e.jar --left-classpath reference/base-0.6.71.jar --right-classpath reference/candidate-50635f7e.jar --right-classpath reference/base-0.6.71.jar --json scratch/issue-64/complete.json --md scratch/issue-64/complete.md
```

<details>
<summary>可重現的完整驗證腳本</summary>

```python
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[2]
SERVER = ROOT / 'lito伺服器/LiTo381-Server'
WORK = ROOT / 'scratch/issue-64'
TARGET = SERVER / 'target/issue-64'
TARGET.mkdir(parents=True, exist_ok=True)
BUILD = Path(tempfile.mkdtemp(prefix='complete-', dir=TARGET))
JDK = Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
STAGED = SERVER / 'merge-inputs/50635f7e/staged/local/lito381/server'
BATCH = next(b for b in json.loads((ROOT / 'docs/combat-merge/batches.json').read_text('utf-8'))['batches'] if b['id'] == 'B03')
ENTRIES = set(BATCH['verify_entries'])
assert len(BATCH['classes']) == 51 and len(ENTRIES) == 135
(WORK / 'entries.txt').write_text('\n'.join(BATCH['verify_entries']) + '\n', 'utf-8')
audit = {'build': str(BUILD), 'commands': [], 'policies': {}}

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def run(args, log):
    args = list(map(str, args))
    if args[0].endswith('javac.exe'):
        args.insert(1, '-J-Dfile.encoding=UTF-8')
    if args[0].endswith('java.exe'):
        args.insert(1, '-Dfile.encoding=UTF-8')
    result = subprocess.run(args, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    output = result.stdout.decode('utf-8', errors='replace')
    (WORK / log).write_text(output, 'utf-8')
    audit['commands'].append({'args': args, 'exit': result.returncode, 'log': log})
    print(log, 'exit', result.returncode, flush=True)
    print(output, end='', flush=True)
    (WORK / 'complete-audit.json').write_text(json.dumps(audit, ensure_ascii=False, indent=2), 'utf-8')
    if result.returncode:
        sys.exit(result.returncode)

analysis = TARGET / 'asm-analysis-9.7.jar'
if not analysis.exists():
    analysis.write_bytes(urllib.request.urlopen('https://repo.maven.apache.org/maven2/org/ow2/asm/asm-analysis/9.7/asm-analysis-9.7.jar').read())
assert sha(analysis) == '7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f'
asm_paths = [Path('D:/dev-cache/maven/repository/org/ow2/asm') / a / '9.7' / (a + '-9.7.jar') for a in ['asm', 'asm-tree']] + [analysis]
ASM = ';'.join(map(str, asm_paths))
sources = [STAGED / (name + '.java') for name in BATCH['classes']] + [STAGED / 'NativeLightAction.java']
audit['sources'] = {str(p.relative_to(ROOT)): sha(p) for p in sources}
audit['inputs'] = {str(p): sha(p) for p in [SERVER / 'reference/candidate-50635f7e.jar', SERVER / 'reference/base-0.6.71.jar', ROOT / 'docs/combat-merge/batches.json', *asm_paths]}
annotations = BUILD / 'annotations'
classes = BUILD / 'classes'
annotations.mkdir()
classes.mkdir()
run([JDK / 'javac.exe', '--release', '17', '-encoding', 'UTF-8', '-d', annotations, SERVER / 'src/main/java/local/lito381/server/AsRecord.java', SERVER / 'src/main/java/local/lito381/server/AsmFinal.java'], 'complete-annotations.log')
CP = ';'.join(map(str, [SERVER / 'reference/candidate-50635f7e.jar', SERVER / 'reference/base-0.6.71.jar', annotations, SERVER / 'lib/*']))
args = ['--release', '17', '-encoding', 'UTF-8', '-cp', CP, '-d', str(classes), *map(str, sources)]
(WORK / 'complete-javac.args').write_text('\n'.join('"' + arg.replace('\\', '/') + '"' for arg in args), 'utf-8')
run([JDK / 'javac.exe', '@' + str(WORK / 'complete-javac.args')], 'complete-javac.log')
audit['javacClasses'] = {p.relative_to(classes).as_posix(): sha(p) for p in sorted(classes.rglob('*.class'))}
for name in ['as-record', 'semantic-diff']:
    out = BUILD / name
    out.mkdir()
    tool_sources = sorted((SERVER / 'tools' / name / 'src/main/java').rglob('*.java'))
    audit[name + 'Sources'] = {str(p.relative_to(ROOT)): sha(p) for p in tool_sources}
    run([JDK / 'javac.exe', '--release', '17', '-encoding', 'UTF-8', '-cp', ASM, '-d', out, *tool_sources], 'complete-' + name + '-build.log')
run([JDK / 'java.exe', '-cp', str(BUILD / 'as-record') + ';' + ASM, 'local.lito381.asrecord.AsRecord', classes], 'complete-as-record.log')
audit['convertedClasses'] = {p.relative_to(classes).as_posix(): sha(p) for p in sorted(classes.rglob('*.class'))}
extra = set(audit['convertedClasses']) - ENTRIES
assert ENTRIES <= set(audit['convertedClasses'])
assert extra and all(n == 'local/lito381/server/NativeLightAction.class' or n.startswith('local/lito381/server/NativeLightAction$') for n in extra), sorted(extra)
audit['excludedDependencyEntries'] = sorted(extra)
for name in extra:
    path = classes / name
    assert path.resolve().is_relative_to(BUILD.resolve()) and not path.is_symlink()
    path.unlink()
assert {p.relative_to(classes).as_posix() for p in classes.rglob('*.class')} == ENTRIES
jar = BUILD / 'B03.jar'
with zipfile.ZipFile(jar, 'w') as archive:
    for name in sorted(ENTRIES):
        archive.writestr(zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0)), (classes / name).read_bytes())
with zipfile.ZipFile(jar) as archive:
    assert set(archive.namelist()) == ENTRIES
    assert all(hashlib.sha256(archive.read(n)).hexdigest() == audit['convertedClasses'][n] for n in ENTRIES)
audit['jarSha256'] = sha(jar)
policy_flags = {'access-widen-approved.txt': '--access-widen', 'effects-approved.txt': '--effects-methods', 'layout-approved.txt': '--layout-methods', 'extra-methods-approved.txt': '--extra-methods', 'metadata-exempt.txt': '--metadata-exempt', 'deviations.txt': '--deviations', 'exceptions-exempt.txt': '--exceptions-exempt'}
flags = []
owners = {n.removesuffix('.class') for n in ENTRIES}
for filename, flag in policy_flags.items():
    original = ROOT / 'docs/combat-merge' / filename
    selected = []
    for line in original.read_text('utf-8-sig').splitlines():
        if not line.strip() or line.lstrip().startswith('#'):
            continue
        match = re.search(r'local/lito381/server/[^#\s]+', line)
        assert match, line
        owner = match.group()
        if owner in owners or (flag == '--access-widen' and owner == 'local/lito381/server/NativeLightAction$Output'):
            selected.append(line)
    audit['policies'][filename] = {'sourceSha256': sha(original), 'entries': selected}
    if selected:
        filtered = WORK / filename
        filtered.write_text('\n'.join(selected) + '\n', 'utf-8')
        flags.extend([flag, str(filtered)])
assert '--access-widen' in flags
cmd = [JDK / 'java.exe', '-cp', str(BUILD / 'semantic-diff') + ';' + ASM, 'local.lito381.semanticdiff.SemanticDiff', '--left', jar, '--right', SERVER / 'reference/candidate-50635f7e.jar', '--classes', WORK / 'entries.txt', *flags]
for side in ['--left-classpath', '--right-classpath']:
    for cp in [SERVER / 'reference/candidate-50635f7e.jar', SERVER / 'reference/base-0.6.71.jar', *sorted((SERVER / 'lib').glob('*.jar'))]:
        cmd.extend([side, cp])
run([*cmd, '--json', WORK / 'complete.json', '--md', WORK / 'complete.md'], 'complete-semantic.log')
data = json.loads((WORK / 'complete.json').read_text('utf-8'))
assert len(data['classes']) == 135
assert not data['classSelection']['missingLeft'] and not data['classSelection']['extraLeft']
bad = [c for c in data['classes'] if c['status'] not in {'SAME', 'SAME_EFFECTS', 'SAME_PATHS'}]
print(json.dumps({'counts': data['counts'], 'bad': bad}, ensure_ascii=False), flush=True)
assert not bad
assert audit['sources'] == {str(p.relative_to(ROOT)): sha(p) for p in sources}
assert audit['convertedClasses'] == {**{n: sha(classes / n) for n in ENTRIES}, **{n: audit['convertedClasses'][n] for n in extra}}
audit['resultSha256'] = sha(WORK / 'complete.json')
audit['accepted'] = True
(WORK / 'complete-audit.json').write_text(json.dumps(audit, ensure_ascii=False, indent=2), 'utf-8')
print('ACCEPTED: 135 entries; missing=[]; extra=[]; source and class hashes preserved', flush=True)

```

</details>

### 輸入／輸出 SHA-256

| 檔案 | SHA-256 |
| --- | --- |
| `lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar` | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| `lito伺服器/LiTo381-Server/reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `docs/combat-merge/batches.json` | `985ce87719338034c9b9d42834de2ecdb41fc2e7a9ab3894fc5a23622d03802e` |
| `docs/combat-merge/access-widen-approved.txt` | `4bf5066b4ba996472337b173330e9fb5306b4378847512d15fe510d9b666ac74` |
| `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/NativeLightAction.java` | `8561ad87c710bb8ce8faccbf634f221e8dad128a498bcfee3654802a1ed60ca8` |
| `lito伺服器/LiTo381-Server/target/issue-64/complete-fiys36h5/B03.jar` | `6fb731a14313ba316b8d9777691baf9e5f3da7ea81146d5e2b15f99d6d221db7` |
| `scratch/issue-64/complete.json` | `7e21a7691aecfcd5c0aeda187a567f1e27d197903e78cff68ad55ad3379fe003` |

51 份 B03 staged 與凍結 sources 比較：20 份已移植、31 份 bytes 相同；本輪均保留。移植檔名：

```text
NativeSourceCubeOrigin
NativeSourceCubePlayer
NativeSourceCubeRemainder
NativeSourceCubeStatus
NativeSourceDamageDisplay
NativeSourceDamageHealingSelection
NativeSourceDollAction
NativeSourceDollCommit
NativeSourceDollDefense
NativeSourceDollOutput
NativeSourceEquipmentOutput
NativeSourceEquipmentProcLife
NativeSourceFatalityDamage
NativeSourceGroundOrigin
NativeSourceHealingHp
NativeSourceIcons1BraveOrigin
NativeSourceIcons1Output
NativeSourceIcons1Projection
NativeSourceKiringkuAdmission
NativeSourceLightPairLife
```

### 完整逐 entry 結果

共同前綴為 `local/lito381/server/`，下列名稱均對應 `.class`。

| Entry | 結果 |
| --- | --- |
| `NativeSourceCubeNpcQueue` | SAME |
| `NativeSourceCubeNpcQueue$Ticket` | SAME |
| `NativeSourceCubeOrigin` | SAME |
| `NativeSourceCubeOrigin$Creator` | SAME |
| `NativeSourceCubeOrigin$Entity` | SAME |
| `NativeSourceCubeOrigin$Event` | SAME |
| `NativeSourceCubeOrigin$Lease` | SAME |
| `NativeSourceCubeOrigin$Scan` | SAME |
| `NativeSourceCubeOrigin$Scene` | SAME |
| `NativeSourceCubeOrigin$Target` | SAME |
| `NativeSourceCubePlayer` | SAME |
| `NativeSourceCubePlayer$Effect` | SAME |
| `NativeSourceCubePlayer$Proof` | SAME |
| `NativeSourceCubePlayer$Reaction` | SAME |
| `NativeSourceCubePresentation` | SAME |
| `NativeSourceCubeRelation` | SAME |
| `NativeSourceCubeRemainder` | SAME |
| `NativeSourceCubeRemainder$Notice` | SAME |
| `NativeSourceCubeRemainder$Publication` | SAME |
| `NativeSourceCubeStatus` | SAME |
| `NativeSourceCubeStatus$Emission` | SAME |
| `NativeSourceCubeStatus$Link` | SAME |
| `NativeSourceCubeTemplate` | SAME |
| `NativeSourceCubeTemplate$Presentation` | SAME |
| `NativeSourceCubeTemplate$Template` | SAME |
| `NativeSourceCubeVisibility` | SAME |
| `NativeSourceCubeVisibility$Cache` | SAME |
| `NativeSourceCubeVisibility$Rebuild` | SAME |
| `NativeSourceCubeVisibility$Work` | SAME |
| `NativeSourceDamageCapacity` | SAME |
| `NativeSourceDamageDisplay` | SAME |
| `NativeSourceDamageDisplay$Actor` | SAME |
| `NativeSourceDamageDisplay$Job` | SAME |
| `NativeSourceDamageDisplay$Jobs` | SAME |
| `NativeSourceDamageDisplay$Plan` | SAME |
| `NativeSourceDamageHealingSelection` | SAME |
| `NativeSourceDamageHealingSelection$Cursor` | SAME |
| `NativeSourceDisplayPlan` | SAME |
| `NativeSourceDisplayPlan$Effect` | SAME |
| `NativeSourceDisplayPlan$Settings` | SAME |
| `NativeSourceDollAction` | SAME |
| `NativeSourceDollAction$Action` | SAME |
| `NativeSourceDollAction$Stamp` | SAME |
| `NativeSourceDollAudience` | SAME |
| `NativeSourceDollAudience$Audience` | SAME |
| `NativeSourceDollAudience$Life` | SAME |
| `NativeSourceDollCommit` | SAME |
| `NativeSourceDollCommit$NpcProof` | SAME |
| `NativeSourceDollCommit$PreVisual` | SAME |
| `NativeSourceDollDefense` | SAME |
| `NativeSourceDollDefense$Output` | SAME |
| `NativeSourceDollDefense$Proof` | SAME |
| `NativeSourceDollDefense$Receiver` | SAME |
| `NativeSourceDollDefense$Snapshot` | SAME |
| `NativeSourceDollDefenseAudience` | SAME |
| `NativeSourceDollDetachedOutput` | SAME |
| `NativeSourceDollOutput` | SAME |
| `NativeSourceDollOutput$Output` | SAME |
| `NativeSourceDollOutput$Proof` | SAME |
| `NativeSourceDollOutput$Receiver` | SAME |
| `NativeSourceDollOutput$Snapshot` | SAME |
| `NativeSourceDollOutput$Speed` | SAME |
| `NativeSourceDollPassive` | SAME |
| `NativeSourceDollPassive$Choice` | SAME |
| `NativeSourceDollSkillData` | SAME |
| `NativeSourceDollSkillData$Row` | SAME |
| `NativeSourceDollSlowWire` | SAME |
| `NativeSourceElementAudience` | SAME |
| `NativeSourceElementAudience$Audience` | SAME |
| `NativeSourceElementAudience$Receiver` | SAME |
| `NativeSourceElementTransport` | SAME |
| `NativeSourceElementTransport$Inherited` | SAME |
| `NativeSourceEquipmentColumns` | SAME |
| `NativeSourceEquipmentFinalOutput` | SAME |
| `NativeSourceEquipmentFinalOutput$First` | SAME |
| `NativeSourceEquipmentOutput` | SAME |
| `NativeSourceEquipmentOutput$Detached` | SAME |
| `NativeSourceEquipmentOutput$Link` | SAME |
| `NativeSourceEquipmentOutput$Output` | SAME |
| `NativeSourceEquipmentOutput$Scope` | SAME |
| `NativeSourceEquipmentOutput$Viewer` | SAME |
| `NativeSourceEquipmentProcLife` | SAME |
| `NativeSourceEquipmentProcLife$Proof` | SAME |
| `NativeSourceFatalityDamage` | SAME |
| `NativeSourceFatalityDamage$Output` | SAME |
| `NativeSourceFatalityDamage$Proof` | SAME |
| `NativeSourceFatalityDamage$Root` | SAME |
| `NativeSourceFirstPlayers` | SAME |
| `NativeSourceFirstPlayers$Builder` | SAME |
| `NativeSourceFirstPlayers$Snapshot` | SAME |
| `NativeSourceFreezeTarget` | SAME |
| `NativeSourceGroundNpc` | SAME |
| `NativeSourceGroundNpc$Audience` | SAME |
| `NativeSourceGroundNpc$Proof` | SAME |
| `NativeSourceGroundNpc$Selected` | SAME |
| `NativeSourceGroundOrigin` | SAME |
| `NativeSourceGroundOrigin$Actor` | SAME |
| `NativeSourceGroundOrigin$Creator` | SAME |
| `NativeSourceGroundOrigin$Entity` | SAME |
| `NativeSourceGroundOrigin$PulseProof` | SAME |
| `NativeSourceGroundOrigin$Scene` | SAME |
| `NativeSourceGroundPlacement` | SAME |
| `NativeSourceGroundPlacement$Draft` | SAME |
| `NativeSourceGroundPlayer` | SAME |
| `NativeSourceGroundQueued` | SAME |
| `NativeSourceHealingFormula` | SAME |
| `NativeSourceHealingFormula$Growth` | SAME |
| `NativeSourceHealingHp` | SAME |
| `NativeSourceHealingHp$Recovery` | SAME |
| `NativeSourceHealingType` | SAME |
| `NativeSourceHitReaction` | SAME |
| `NativeSourceIcons1` | SAME |
| `NativeSourceIcons1$Rule` | SAME |
| `NativeSourceIcons1$Snapshot` | SAME |
| `NativeSourceIcons1BraveOrigin` | SAME |
| `NativeSourceIcons1Epoch` | SAME |
| `NativeSourceIcons1Epoch$1` | SAME |
| `NativeSourceIcons1Epoch$Cell` | SAME |
| `NativeSourceIcons1Output` | SAME |
| `NativeSourceIcons1Output$Output` | SAME |
| `NativeSourceIcons1Output$Patience` | SAME |
| `NativeSourceIcons1Policy` | SAME |
| `NativeSourceIcons1Policy$Columns` | SAME |
| `NativeSourceIcons1Policy$Policy` | SAME |
| `NativeSourceIcons1Projection` | SAME |
| `NativeSourceIcons1Projection$Entry` | SAME |
| `NativeSourceIndirectPlayerProof` | SAME |
| `NativeSourceKiringkuAdmission` | SAME |
| `NativeSourceKiringkuAdmission$Aim` | SAME |
| `NativeSourceKiringkuPresentation` | SAME |
| `NativeSourceKiringkuPresentation$Output` | SAME |
| `NativeSourceKiringkuPresentation$Viewer` | SAME |
| `NativeSourceLightPairLife` | SAME |
| `NativeSourceLightPairLife$Actor` | SAME |
| `NativeSourceLightPairLife$Proof` | SAME |

## 推測

無需推測本批驗收結果；上述結論以整批實際編譯、class 集合與語意比對為據。跨批整合後是否仍通過全部驗收，須由 B-land 實跑確認。

## 待驗證／未驗證什麼

- 單元測試及執行期回歸依任務規格不適用於本輪 staged 驗收，留 B-land 統一執行。
- 沒有宣告 src/main、Maven 整體建置、正式發布或真機遊戲驗收完成。
- 本批沒有剩餘語意差異、missing、extra 或需新增主控裁定的阻塞。

## 產出路徑

- 交付來源：`lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/` 的 B03 51 份 `.java`（既有提交保留）。
- 本輪報告：`docs/reports/issue-64.md`。
- 本機驗證證據：`scratch/issue-64/complete-javac.args`、`complete-*.log`、`complete.json`、`complete.md`、`complete-audit.json`、核准清單裁切副本與 `verify-complete.py`。audit 含全部工具／輸入來源雜湊、實際命令、退出碼、轉換前後 class 雜湊及排除清單。
- 本機建置產物：`lito伺服器/LiTo381-Server/target/issue-64/complete-fiys36h5/`，其中 `B03.jar` 精確包含 135 個本批 entry；註解與工具 classes 分開存放。scratch 與 target 都不提交，乾淨 checkout 可使用本報告內腳本重建。

## 給下一棒的提示

由 Claude 主控審查既有 PR #80 的更新與本報告，通過後交使用者合併。B03 已達 135 SAME、missing／extra 空；不用再等待 Output 可見性裁定，也不需要標 `needs:claude` 阻塞。

B-land（#138）合入時，保留本批 6 個 AsRecord 與 1 個 AsmFinal，使用 #69 的 package-private Output 來源及同一條核准 access-widen；這次只排除獨立驗證 JAR 的 B08 依賴 entry，最終全批 JAR 仍須包含 B08。再統一執行 1,384 個 B entry 語意比對、單元測試與回歸。

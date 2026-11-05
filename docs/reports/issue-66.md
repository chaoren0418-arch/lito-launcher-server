# Issue #66：T020-B05 戰鬥併入暫存

**結論：B05 本批語意驗收通過。** 2026-11-06（臺北時間）重新編譯全部 48 份 staged Java，使用已合併 #174／#176 的工具驗收全部 120 個 entry，結果 **115 SAME／3 SAME_EFFECTS／1 SAME_PATHS／1 ORPHAN_IGNORED／0 DIFF**；缺少、多出、ONLY_LEFT、ONLY_RIGHT 均為 0。未執行應用程式回歸，尚未落地 B-land。

## 做了什麼

- 在現有 `task/66` worktree 操作，沒有 checkout 或建立分支。依賴 #174（PR #178）、#176（PR #193）均已合併；先 fetch，再合併 `origin/main`。
- 合併基線：`c72453957ebe47e00ad024e874bbe31859457799`；合併提交：`7d0137d8da952a5703449a9ce8f36aefe85be746`，無衝突。
- 本輪只更新本報告，48 份 Java 與先前 `8ae6d3c` 完全相同。`NativeSourcePoisonNpc` 未加回孤立 lambda，`NativeStaticNpcScene` 保持鎖內判定／更新、鎖外 callback 的寫法。
- 從本 worktree 的工具 Java 來源重新編譯 converter 與 semantic-diff，沒有使用舊版工具 classes。兩份註解編入獨立 annotation classpath，再以 `javac --release 17 -encoding UTF-8` 編譯本批，執行唯一核准的 as-record 轉換後打包驗收。
- 保留既有 `NativeSourceRawDamage.Output` 的 `@AsRecord` 與 `Member` 的 `@AsmFinal`。沒有修改核准清單、工具、`src/main`、`pom.xml` 或 `docs/REPORT.md`；上游變更僅由 main 合併帶入。
- 相對本次 main 的交付範圍精確為 B05 的 48 份 Java 與本報告，共 49 檔；`git diff origin/main --check` 通過。

## 已驗證

### 介面與核准依據

[本批主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/66#issuecomment-6655873600) 原本提到 `--path-methods docs/combat-merge/path-approved.txt`；後續 [#176 最終規格](https://github.com/chaoren0418-arch/lito-launcher-server/issues/176) 已將第 2 項改為「預設開啟的後備比較」。已合併工具沒有此旗標，也沒有此清單；本輪使用預設逐路徑證明，未自行新增假的旗標或核准檔。#176 明確將本批四個 Scene 方法及 PoisonNpc 孤立方法列為案例。

七份現有核准清單均檢查與本批的精確交集：`effects-approved.txt` 有 7 個方法；`extra-methods-approved.txt`、`metadata-exempt.txt`、`access-widen-approved.txt`、`layout-approved.txt`、`exceptions-exempt.txt`、`deviations.txt` 均為 0。正式驗收傳入裁切後的 `--effects-methods`，沒有使用 layout、deviation、metadata 或額外方法豁免。空交集不傳入工具，避免將其他批次條目擴大到本批。

### 編譯、轉換與產物完整性

工作目錄：`D:/lineage開發/3-lito開發/scratch/wt/task-66`。
JDK：`C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot`，Microsoft OpenJDK／javac 17.0.17；Python 3.12.10；ASM 9.7 三份依賴均通過工具指定 SHA-256 校驗。

```text
javac --release 17 -encoding UTF-8 -cp <candidate;base;annotation;lib/*> -d <classes> <B05 全部 48 份 staged Java>
exit 0
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
as-record: scanned=120 converted=2
as-record exit 0
PROVENANCE PASS; missing=0 extra=0
```

轉換前、轉換後及 JAR 的 class 集合都精確等於 `batches.json` B05 的 120 個 `verify_entries`。逐 entry 雜湊確認 converter 只改 `NativeSourceRawDamage$Member.class` 與 `$Output.class`，其餘 118 個 javac 產物 bytes 未改；打包全部 class，逐一確認 JAR bytes 與目錄相同。沒有候選 class 覆蓋、改名、刪除 entry 或其他後處理。

比較結束後再核對 classes、JAR、48 份來源及核准清單雜湊，均未改變。本輪 batch.jar 與前次 `8ae6d3c` 報告的 SHA-256 相同，證明此次結果變化來自新版比較器，非替換驗收產物。

### 全批驗收結果

| 模式 | SAME | SAME_EFFECTS | SAME_PATHS | ORPHAN_IGNORED | DIFF | 缺少／多出 |
|---|---:|---:|---:|---:|---:|---:|
| 預設，無核准清單 | 117 | 0 | 2 | 1 | 0 | 0／0 |
| 核准模式，本輪正式驗收 | 115 | 3 | 1 | 1 | 0 | 0／0 |

兩模式 SAME_LAYOUT、DEVIATION、ONLY_LEFT、ONLY_RIGHT、BYTE_SAME 均為 0。工具 exit 0 只代表有產生報告；重現腳本另外斷言每筆狀態只屬於 SAME、SAME_EFFECTS、SAME_PATHS、ORPHAN_IGNORED，並檢查全部 120 筆與無缺漏／多出後，才印出 FINAL PASS。

```text
SUMMARY SAME=115 SAME_EFFECTS=3 SAME_PATHS=1 ORPHAN_IGNORED=1 SAME_LAYOUT=0 DEVIATION=0 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0 ALLOW_METHODS=0
```

### 先前兩個 DIFF 的驗收證據

- `NativeSourcePoisonNpc`：ORPHAN_IGNORED。完整右側候選 JAR 掃描確認 `lambda$status$1` 為 private synthetic，沒有直接／handle／bootstrap／condy 參照或同名 LDC 字串。這是主控核准的孤立方法規則，未補回 Java 死碼。
- `NativeStaticNpcScene`：SAME_PATHS。四個方法的路徑多重集合含正常及例外出口，均相同：heading 17、restore 30、transform 28、turn 20。鎖與 callback 事件順序由新版工具逐路徑檢查。
- 三個 SAME_EFFECTS entry 為 `NativeSourceProcScalarSkills`、`NativeSourceSignedRecovery`、`NativeSourceWindTargets`。無核准模式下 SignedRecovery 為 SAME_PATHS，其餘兩個為 SAME；核准模式按工具既有規則分列 SAME_EFFECTS。
- Scene 的 `effectsMethods` 欄亦列 SAME_EFFECTS，但實際後備證明在 `methodResults`，類別依優先序列為 SAME_PATHS；不得將這四個方法解讀成只做線性 effects 摘要。

以下直接摘錄正式 JSON 的逐方法證據（含精確描述子）：

```json
[
  {
    "name": "local/lito381/server/NativeSourcePoisonNpc",
    "status": "ORPHAN_IGNORED",
    "methodResults": [
      {
        "method": "lambda$status$1(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativeSourcePoisonNpc$Status;Llocal/lito381/server/ClientSession;[BLjava/lang/String;)V",
        "status": "ORPHAN_IGNORED",
        "detail": "private synthetic; no JAR references or matching LDC string"
      }
    ],
    "effectsMethods": []
  },
  {
    "name": "local/lito381/server/NativeSourceProcScalarSkills",
    "status": "SAME_EFFECTS",
    "methodResults": [],
    "effectsMethods": [
      {
        "method": "expiry(I)Ljava/lang/String;",
        "status": "SAME_EFFECTS",
        "detail": null
      }
    ]
  },
  {
    "name": "local/lito381/server/NativeSourceSignedRecovery",
    "status": "SAME_EFFECTS",
    "methodResults": [],
    "effectsMethods": [
      {
        "method": "member(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeAreaMaterialAction$Context;)Llocal/lito381/server/NativeSourceRawDamage$Member;",
        "status": "SAME_EFFECTS",
        "detail": null
      }
    ]
  },
  {
    "name": "local/lito381/server/NativeSourceWindTargets",
    "status": "SAME_EFFECTS",
    "methodResults": [],
    "effectsMethods": [
      {
        "method": "run(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$WindShackleSpell;I)V",
        "status": "SAME_EFFECTS",
        "detail": null
      }
    ]
  },
  {
    "name": "local/lito381/server/NativeStaticNpcScene",
    "status": "SAME_PATHS",
    "methodResults": [
      {
        "method": "heading(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClientSession;)I",
        "status": "SAME_PATHS",
        "detail": "path multisets equal (17)"
      },
      {
        "method": "restore(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/ClientSession;)Z",
        "status": "SAME_PATHS",
        "detail": "path multisets equal (30)"
      },
      {
        "method": "transform(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;IILlocal/lito381/server/ClientSession;)Z",
        "status": "SAME_PATHS",
        "detail": "path multisets equal (28)"
      },
      {
        "method": "turn(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/ClientSession;)Z",
        "status": "SAME_PATHS",
        "detail": "path multisets equal (20)"
      }
    ],
    "effectsMethods": [
      {
        "method": "heading(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClientSession;)I",
        "status": "SAME_EFFECTS",
        "detail": null
      },
      {
        "method": "transform(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;IILlocal/lito381/server/ClientSession;)Z",
        "status": "SAME_EFFECTS",
        "detail": null
      },
      {
        "method": "restore(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/ClientSession;)Z",
        "status": "SAME_EFFECTS",
        "detail": null
      },
      {
        "method": "turn(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/ClientSession;)Z",
        "status": "SAME_EFFECTS",
        "detail": null
      }
    ]
  }
]
```

### 證據雜湊

| 檔案 | SHA-256 |
|---|---|
| `candidate-50635f7e.jar` | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| `base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `batch.jar` | `b8d54f8315093327943495503edb9e7dbb0a725d73ea10da012d37738f1d52e4` |
| `approved.json` | `8ef7d398e4d1874355fad066def97f2b86cebb5e20260993831cf41acf3f91f0` |
| `default.json` | `ca7468bf52308043ccf9e1e26f1ea6a456ae82c390a06f8a54ecc75e009e5386` |
| `provenance.json` | `c983e88a84c6c0b858f46347cd1a1d956110687ac7ac5bf48f1bac5234518259` |
| `commands.json` | `7657115bd41f688373e4ae35f863d30722c5d21b9c5ea8630c66d29c6c6c16f2` |
| `inputs.json` | `d925168372c3e943443365f11956a7359593901d74e56248016073687b2bfd75` |
| `approvals.json` | `72230ff3799f0624e87e3da03e04e06278ccf43392d42aaa990d64c14cef0ac9` |

## 推測

本輪沒有依推測放行的 entry。比較器的有界證明及孤立方法規則屬本任務核准的靜態驗收，沒有宣稱完整 JVM／並行行為或所有反射情境均經實機驗證。

## 待驗證／未驗證

- 本批 Java 與語意驗收已完成，無剩餘 DIFF，無新增 `needs:claude` 裁定需求。
- 遠端交付尚未完成：正常 `git push -u origin task/66` 重試後仍被 GitHub 以 `remote: fatal error in commit_refs_rules` 拒絕。遠端仍為 `8ae6d3c`；本機保留合併與驗收報告提交，未 force push 或變更遠端規則。這是遠端推送錯誤，不是語意驗收失敗或本機自動核准拒絕。
- 單元測試及應用程式回歸依任務不適用：暫存區不執行，交 B-land／T020-close 統一處理。沒有啟動遊戲、登入器、伺服器或連接 DB。
- 未部署、未變更正式 JAR、機密設定、封存、release(s)、客戶端或玩家資料。原始 repo 工作檔未寫入；Git 共用中繼資料僅因授權的 fetch／merge／commit／push 更新。

## 產出路徑與給下一棒的提示

- 交付來源：`lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/` 下 B05 48 檔。
- 交付報告：`docs/reports/issue-66.md`。
- 本機驗收證據：`lito伺服器/LiTo381-Server/target/issue-66-verified-176/`，含 classes、batch.jar、全部命令與退出碼（commands.json）、javac／converter／semantic 日誌、default／approved JSON 與 Markdown、provenance.json、inputs.json、approvals.json。
- 暫存腳本：`scratch/issue-66/verify-final.py`、`report-final.py`，不提交。以下內嵌 verifier 與 120-entry 結果讓審查者不必依賴未追蹤腳本才能重現或核對範圍。
- 下一棒先由調度器／本機執行器重試推送本機 `task/66`；遠端收到新提交後由 Claude 審查既有 PR #88，合併決策依專案流程。B-land 執行者待其他 B 批次完成後統一落地並重驗，不要以本批通過代替 Maven 整合／回歸。
- 重驗必須從 48 份 staged Java 新編譯，執行 as-record（含 AsmFinal），使用 #176 或更新工具與完整 120-entry 清單；保留 SAME_PATHS、ORPHAN_IGNORED 的獨立計數。

## 可重現驗收

把下段存到本 worktree 的 `scratch/issue-66/verify-final.py`，在 worktree 根目錄執行：

```powershell
& D:/dev-orchestrator/tools/python/python.exe -B -X utf8 scratch/issue-66/verify-final.py review-176
```

輸出會建立在新的 `target/issue-66-review-176/`；若目錄已存在則拒絕，避免覆蓋先前產物。本輪實跑參數為 `verified-176`。候選／正式 reference、ASM 依賴沿用本機既有資源，只讀取；兩工具的 classes 每次由合併後來源新建到本輪 target。

```python
import hashlib, json, os, subprocess, sys, zipfile
from pathlib import Path

sys.dont_write_bytecode = True
root = Path(__file__).resolve().parents[2]
server = root / 'lito伺服器/LiTo381-Server'
out = server / 'target' / ('issue-66-' + sys.argv[1])
out.mkdir(parents=True, exist_ok=False)
os.environ['JAVA_HOME'] = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot'
os.environ['AS_RECORD_MAVEN_REPO'] = 'D:/dev-cache/maven/repository'
os.environ['TEMP'] = os.environ['TMP'] = str(out)
sys.path.insert(0, str(server / 'tools/as-record'))
import as_record as tool

batch = next(b for b in json.loads((root / 'docs/combat-merge/batches.json').read_text('utf-8'))['batches'] if b['id'] == 'B05')
candidate = server / 'reference/candidate-50635f7e.jar'
base = server / 'reference/base-0.6.71.jar'
expected = set(batch['verify_entries'])
assert len(batch['classes']) == 48 and len(expected) == 120
entries = out / 'entries.txt'
entries.write_text('\n'.join(batch['verify_entries']) + '\n', encoding='utf-8')
staged = server / 'merge-inputs/50635f7e/staged/local/lito381/server'
sources = [staged / (n + '.java') for n in batch['classes']]
classes = out / 'classes'
commands = []

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def run(args, name):
    args = list(map(str, args))
    p = subprocess.run(args, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    log = p.stdout.decode('utf-8', errors='replace')
    (out / (name + '.log')).write_text(log, encoding='utf-8')
    commands.append({'name': name, 'args': args, 'exit': p.returncode})
    (out / 'commands.json').write_text(json.dumps(commands, ensure_ascii=False, indent=2), encoding='utf-8')
    print(name, 'exit', p.returncode, log[-3000:], flush=True)
    assert p.returncode == 0, name

def compile(source_files, dest, cp, name):
    dest.mkdir(parents=True, exist_ok=False)
    run([tool.jdk('javac'), '--release', '17', '-encoding', 'UTF-8', '-cp', cp, '-d', dest, *source_files], name)

assert sha(candidate) == '50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729'
assert sha(base) == 'e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498'
inputs = {p.relative_to(root).as_posix(): sha(p) for p in [candidate, base, *sources]}
run([tool.jdk('java'), '-version'], 'java-version')
run([tool.jdk('javac'), '-version'], 'javac-version')
compile([server / ('src/main/java/local/lito381/server/' + n + '.java') for n in ['AsRecord', 'AsmFinal']], out / 'annotation', '', 'annotation')
cp = os.pathsep.join(map(str, [candidate, base, out / 'annotation', server / 'lib/*']))
compile(sources, classes, cp, 'javac')

def hashes():
    return {p.relative_to(classes).as_posix(): sha(p) for p in classes.rglob('*.class')}

before = hashes()
assert set(before) == expected
deps = tool.dependencies()
compile(sorted((server / 'tools/as-record/src/main/java').rglob('*.java')), out / 'converter', deps, 'converter-compile')
run([tool.jdk('java'), '-cp', str(out / 'converter') + os.pathsep + deps, 'local.lito381.asrecord.AsRecord', classes], 'as-record')
after = hashes()
changed = sorted(e for e in before if before[e] != after[e])
assert set(after) == expected
assert set(changed) == {'local/lito381/server/NativeSourceRawDamage$Output.class', 'local/lito381/server/NativeSourceRawDamage$Member.class'}
print('PROVENANCE PASS; missing=0 extra=0; transformed', changed, flush=True)
(out / 'provenance.json').write_text(json.dumps({'before': before, 'after': after, 'converted': changed}, indent=2), encoding='utf-8')
jar = out / 'batch.jar'
with zipfile.ZipFile(jar, 'w') as z:
    for p in sorted(classes.rglob('*.class')):
        info = zipfile.ZipInfo(p.relative_to(classes).as_posix(), (2026, 11, 5, 0, 0, 0))
        z.writestr(info, p.read_bytes())
with zipfile.ZipFile(jar) as z:
    assert len(z.namelist()) == len(expected) and set(z.namelist()) == expected
    assert all(z.read(e) == (classes / e).read_bytes() for e in expected)
jar_hash = sha(jar)
analysis = server / 'tools/semantic-diff/target/dependency/asm-analysis-9.7.jar'
assert sha(analysis) == '7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f'
deps += os.pathsep + str(analysis)
compile(sorted((server / 'tools/semantic-diff/src/main/java').rglob('*.java')), out / 'semantic-diff', deps, 'semantic-compile')

flags = []
approvals = {}
for name, flag in [('effects-approved.txt', '--effects-methods'), ('extra-methods-approved.txt', '--extra-methods'), ('metadata-exempt.txt', '--metadata-exempt'), ('access-widen-approved.txt', '--access-widen'), ('layout-approved.txt', '--layout-methods'), ('exceptions-exempt.txt', '--exceptions-exempt'), ('deviations.txt', '--deviations')]:
    source = root / 'docs/combat-merge' / name
    inputs[source.relative_to(root).as_posix()] = sha(source)
    selected = []
    for line in source.read_text('utf-8-sig').splitlines():
        text = line.strip()
        if not text or text.startswith('#'):
            continue
        if text.startswith('ctor-prologue-order '):
            text = text.split(' ', 1)[1]
        owner = text.split('#', 1)[0].strip()
        if owner + '.class' in expected:
            selected.append(line)
    approvals[name] = selected
    if selected:
        target = out / name
        target.write_text('\n'.join(selected) + '\n', encoding='utf-8')
        flags += [flag, target]
(out / 'approvals.json').write_text(json.dumps(approvals, ensure_ascii=False, indent=2), encoding='utf-8')
print('APPROVALS', {name: len(lines) for name, lines in approvals.items()}, flush=True)

for mode in ['default', 'approved']:
    args = [tool.jdk('java'), '-Xmx2g', '-cp', str(out / 'semantic-diff') + os.pathsep + deps, 'local.lito381.semanticdiff.SemanticDiff', '--left', jar, '--right', candidate, '--classes', entries, '--left-classpath', candidate, '--json', out / (mode + '.json'), '--md', out / (mode + '.md')]
    if mode == 'approved':
        args += flags
    run(args, mode)
    result = json.loads((out / (mode + '.json')).read_text('utf-8'))
    print(mode, result['counts'], result['classSelection'], flush=True)
    assert not result['classSelection']['missingLeft'] and not result['classSelection']['extraLeft']
    assert len(result['classes']) == 120
    assert all(c['status'] in {'SAME', 'SAME_EFFECTS', 'SAME_PATHS', 'ORPHAN_IGNORED'} for c in result['classes'])

assert hashes() == after and sha(jar) == jar_hash
assert all(sha(root / p) == h for p, h in inputs.items())
(out / 'inputs.json').write_text(json.dumps(inputs, ensure_ascii=False, indent=2), encoding='utf-8')
print('FINAL PASS; JAR SHA256', jar_hash, flush=True)

```

## 全部 120 個 entry

名稱省略共同前綴 `local/lito381/server/`，均帶 `.class`；以下直接由正式驗收 JSON 產生。

| Entry | 結果 |
|---|---|
| `NativeSourcePoisonNpc.class` | ORPHAN_IGNORED |
| `NativeSourcePoisonNpc$Commit.class` | SAME |
| `NativeSourcePoisonNpc$Origin.class` | SAME |
| `NativeSourcePoisonNpc$Output.class` | SAME |
| `NativeSourcePoisonNpc$Spawn.class` | SAME |
| `NativeSourcePoisonNpc$Status.class` | SAME |
| `NativeSourcePoisonNpc$Ticket.class` | SAME |
| `NativeSourcePoisonPc.class` | SAME |
| `NativeSourceProc68FirstQueue.class` | SAME |
| `NativeSourceProc68Graphics.class` | SAME |
| `NativeSourceProc68Graphics$Actors.class` | SAME |
| `NativeSourceProc68Graphics$Output.class` | SAME |
| `NativeSourceProc68Icon.class` | SAME |
| `NativeSourceProc68Icon$Output.class` | SAME |
| `NativeSourceProc68Icon$Proof.class` | SAME |
| `NativeSourceProcScalarExtras.class` | SAME |
| `NativeSourceProcScalarOutput.class` | SAME |
| `NativeSourceProcScalarOutput$Output.class` | SAME |
| `NativeSourceProcScalarOutput$Target.class` | SAME |
| `NativeSourceProcScalarSkills.class` | SAME_EFFECTS |
| `NativeSourceProcTimedRefresh.class` | SAME |
| `NativeSourceRawAudience.class` | SAME |
| `NativeSourceRawAudience$Proof.class` | SAME |
| `NativeSourceRawDamage.class` | SAME |
| `NativeSourceRawDamage$Action.class` | SAME |
| `NativeSourceRawDamage$Guard.class` | SAME |
| `NativeSourceRawDamage$Member.class` | SAME |
| `NativeSourceRawDamage$Npc.class` | SAME |
| `NativeSourceRawDamage$Output.class` | SAME |
| `NativeSourceRawNpc.class` | SAME |
| `NativeSourceRawNpc$Admission.class` | SAME |
| `NativeSourceRawNpc$Source.class` | SAME |
| `NativeSourceRefRawNpc.class` | SAME |
| `NativeSourceRemainingAreaLists.class` | SAME |
| `NativeSourceRemainingAreaLists$Cursor.class` | SAME |
| `NativeSourceRemainingAreaLists$FrozenCursor.class` | SAME |
| `NativeSourceRemainingAreaLists$Targets.class` | SAME |
| `NativeSourceRemainingMaterials.class` | SAME |
| `NativeSourceRemainingMaterials$Aim.class` | SAME |
| `NativeSourceRemainingMaterials$Output.class` | SAME |
| `NativeSourceSelfFinalFrame.class` | SAME |
| `NativeSourceSelfFinalFrame$First.class` | SAME |
| `NativeSourceSelfFinalFrame$Output.class` | SAME |
| `NativeSourceSelfFinalFrame$Participant.class` | SAME |
| `NativeSourceSelfFinalFrame$Scope.class` | SAME |
| `NativeSourceSelfFinalFrame$SelfTask.class` | SAME |
| `NativeSourceSignedRecovery.class` | SAME_EFFECTS |
| `NativeSourceSignedRecovery$Intent.class` | SAME |
| `NativeSourceSingleNpc.class` | SAME |
| `NativeSourceSingleNpc$Output.class` | SAME |
| `NativeSourceSingleNpc$Single.class` | SAME |
| `NativeSourceSkillDefaults.class` | SAME |
| `NativeSourceSleepConstruction.class` | SAME |
| `NativeSourceSleepConstruction$Cursor.class` | SAME |
| `NativeSourceSleepConstruction$Frozen.class` | SAME |
| `NativeSourceSleepConstruction$Selection.class` | SAME |
| `NativeSourceSlowFailureMetadata.class` | SAME |
| `NativeSourceSpecialNpc.class` | SAME |
| `NativeSourceSpecialNpc$Bone.class` | SAME |
| `NativeSourceSpecialNpc$Output.class` | SAME |
| `NativeSourceSpecialNpc$Stage.class` | SAME |
| `NativeSourceTargetConstruction.class` | SAME |
| `NativeSourceTargetIconBatch.class` | SAME |
| `NativeSourceTargetIconBatch$Work.class` | SAME |
| `NativeSourceTargetLos.class` | SAME |
| `NativeSourceTargetMask.class` | SAME |
| `NativeSourceTargetNegativeLife.class` | SAME |
| `NativeSourceTargetNegativeLife$Proof.class` | SAME |
| `NativeSourceTargetNegativeReserve.class` | SAME |
| `NativeSourceTargetNpc.class` | SAME |
| `NativeSourceTargetNpc$Mutation.class` | SAME |
| `NativeSourceTargetNpc$Output.class` | SAME |
| `NativeSourceTargetNpc$Status.class` | SAME |
| `NativeSourceVisibilityRange.class` | SAME |
| `NativeSourceWandReaction.class` | SAME |
| `NativeSourceWeaponBuffTargets.class` | SAME |
| `NativeSourceWeaponBuffTargets$Aim.class` | SAME |
| `NativeSourceWeaponBuffTargets$Output.class` | SAME |
| `NativeSourceWeaponCounterMagic.class` | SAME |
| `NativeSourceWeaponFinalFrame.class` | SAME |
| `NativeSourceWeaponFinalFrame$Npc.class` | SAME |
| `NativeSourceWeaponFinalFrame$Output.class` | SAME |
| `NativeSourceWeaponFinalFrame$Participant.class` | SAME |
| `NativeSourceWeaponFinalFrame$Proof.class` | SAME |
| `NativeSourceWeaponPlayerCounter.class` | SAME |
| `NativeSourceWeaponTargetLife.class` | SAME |
| `NativeSourceWindTargets.class` | SAME_EFFECTS |
| `NativeSourceWindTargets$Aim.class` | SAME |
| `NativeSourceWindTargets$Output.class` | SAME |
| `NativeStaticCancellationReplacement.class` | SAME |
| `NativeStaticCancellationReplacement$Allocation.class` | SAME |
| `NativeStaticCancellationReplacement$Retired.class` | SAME |
| `NativeStaticNpcMotion.class` | SAME |
| `NativeStaticNpcMotion$1.class` | SAME |
| `NativeStaticNpcMotion$Epoch.class` | SAME |
| `NativeStaticNpcMotion$Motion.class` | SAME |
| `NativeStaticNpcMotion$Pose.class` | SAME |
| `NativeStaticNpcMotion$Scene.class` | SAME |
| `NativeStaticNpcMotion$State.class` | SAME |
| `NativeStaticNpcMotion$Terrain.class` | SAME |
| `NativeStaticNpcMotion$View.class` | SAME |
| `NativeStaticNpcRespawn.class` | SAME |
| `NativeStaticNpcRespawn$Plan.class` | SAME |
| `NativeStaticNpcRespawn$Respawn.class` | SAME |
| `NativeStaticNpcScene.class` | SAME_PATHS |
| `NativeStaticNpcScene$Actor.class` | SAME |
| `NativeStaticNpcScene$Key.class` | SAME |
| `NativeStaticNpcScene$State.class` | SAME |
| `NativeStaticSpawnMetadata.class` | SAME |
| `NativeStaticSpawnMetadata$Row.class` | SAME |
| `NativeSummonGroundTransfer.class` | SAME |
| `NativeSummonGroundTransfer$Stage.class` | SAME |
| `NativeSummonOwnerLifecycle.class` | SAME |
| `NativeSummonRetirement.class` | SAME |
| `NativeSummonRetirement$Work.class` | SAME |
| `NativeTamedCaptureLifecycle.class` | SAME |
| `NativeTamedCaptureLifecycle$Preparation.class` | SAME |
| `NativeTamedWorldLifecycle.class` | SAME |
| `NativeTamedWorldLifecycle$Respawn.class` | SAME |
| `NativeWindSourceTargets.class` | SAME |

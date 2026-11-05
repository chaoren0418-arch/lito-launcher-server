# Issue #67 — T020-B06 戰鬥併入暫存

## 結論

2026-11-06，#203 合併後以最新 main 的工具重新編譯、轉換並驗收 B06 全部 29 份 staged Java／323 個 entry：**311 SAME、0 SAME_EFFECTS、10 SAME_PATHS、2 SAME_CFG；DIFF、ONLY_LEFT、ONLY_RIGHT、missing、extra 均為 0。本批 staged 語意驗收通過，可交 Claude 審查。**

`BattleGroundEffects#cast` 與 `BattleTimedStatus#cancelMagicEffects` 均由 CFG 後備證明通過；所有 Java 與 `b900eae` 完全相同，保留三方移植、14 個 `@AsRecord`、7 個 `@AsmFinal`。4096 路徑上限未調整，沒有新增豁免。這是暫存來源的驗收，不是實機戰鬥或 B-land 回歸完成。

## 做了什麼

- 沿用既有 `task/67`，本輪起點 `40c803d`；未 checkout／建立分支。fetch 後合併 `origin/main` 的 `567b0cb`，合併提交 `a00bd33`，無衝突。
- GitHub REST API 已確認 [PR #215](https://github.com/chaoren0418-arch/lito-launcher-server/pull/215) 的 `merged=true`，合併時間 `2026-11-05T20:19:36Z`，merge commit `5ae1eac20d75b75b2bf9fbf302e8d878a32c62bd`；#174、#176、#186、#203 均已在合併後基線。
- 依[主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/67#issuecomment-6663017511)，兩個指定方法保持 Java 不變。從全新 `target/issue-67/cfg-final` 編譯同版工具、註解與本批來源；唯一 class 後處理為 `tools/as-record` 的官方轉換器。
- 未以候選 class bytes 覆蓋輸出，未替換或改名驗收產物。annotation classes 與本批 classes 分開；依 B06.verify_entries 核對實際輸出及 CLI 結果，323 條完整清單全部驗證。
- 核對全部 8 份核准清單（access-widen、deviations、effects、exceptions-exempt、extra-methods、layout、metadata-exempt、T029），B06 精確 owner 命中 **0 條**，因此不傳工具會拒絕的空清單，也沒有借用別批豁免。裁切結果在 `approved-scope.json`。
- 本輪手動修改僅本報告；分支相對 main 的任務範圍恰為既有 29 份 B06 Java 與本報告。`docs/REPORT.md` 僅由合併 main 帶入，相對 main 無差異；未修改 `src/main`、`pom.xml`。

## 已驗證

### 輸入、編譯與產物

| 項目 | SHA-256／結果 |
|---|---|
| `reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `reference/candidate-50635f7e.jar` | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| 本輪 `staged.jar` | `8820abf5684b6fd6c9c55826372a21bad10f5f965880d4a954e57fe22d1640c6` |
| 編譯 | Microsoft JDK 17.0.17；`--release 17 -encoding UTF-8`；29 份來源成功 |
| 輸出集合 | 323／323；missing=[]、extra=[]，與 B06.verify_entries 完全一致 |
| 註解與轉換 | 原始碼 14 個 AsRecord、7 個 AsmFinal；第一次 scanned=323 converted=21 |
| 冪等性 | 第二次 scanned=323 converted=0，全部 class SHA-256 不變 |
| record | 14／14 entry 均為 SAME |
| Java 保留 | 29 份檔案逐 byte 與 `b900eae` 相同，並核對編譯收據的來源 hash |
| 分支範圍 | 相對 main 恰 30 檔；無本批以外的任務修改 |

本 worktree 的 `lib/*` 為空；編譯依賴來自候選、正式 reference 與獨立註解 classes。`--left-classpath` 僅提供候選依賴的 metadata，不覆蓋左側主 JAR。重建 JAR hash 與前輪一致；兩個 DIFF 消除來自已合併比較器，而非改動產物。

```text
build-as-record exit 0
build-semantic-diff exit 0
annotations exit 0
javac exit 0
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
as-record exit 0
as-record: scanned=323 converted=21
entries=323 missing=[] extra=[]
as-record-idempotent exit 0
as-record: scanned=323 converted=0
B06 applicable approval entries=0
semantic exit 0
SUMMARY SAME=311 SAME_EFFECTS=0 SAME_PATHS=10 ORPHAN_IGNORED=0 SAME_CFG=2 LAMBDA_PAIRED_BY_CALLSITE=0 VISIBILITY_BRIDGE=0 SAME_LAYOUT=0 DEVIATION=0 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=6 ALLOW_METHODS=0
reproduce-cfg.py exit 0
```

semantic-diff 本身遇 DIFF 仍可能 exit 0；本輪另外逐條檢查完整集合與可接受狀態，不以 CLI 退出碼單獨判定。

### 全部 entry 分布

以下各列包含外層與其全部 `$` entry。SAME_EFFECTS、DIFF、ONLY_LEFT、ONLY_RIGHT 全部為零。

| 外層類別 | SAME | SAME_PATHS | SAME_CFG |
|---|---:|---:|---:|
| `BattleAbilityStatus` | 21 | 2 | 0 |
| `BattleBurningWeaponStatus` | 2 | 0 | 0 |
| `BattleCompanion` | 18 | 1 | 0 |
| `BattleCounterMirrorStatus` | 1 | 1 | 0 |
| `BattleCubeHarmonyEffects` | 11 | 0 | 0 |
| `BattleCubeIgnitionEffects` | 13 | 0 | 0 |
| `BattleCubeQuakeEffects` | 15 | 0 | 0 |
| `BattleCubeShockEffects` | 14 | 0 | 0 |
| `BattleDragonSkinStatus` | 2 | 0 | 0 |
| `BattleElementalFallDownStatus` | 3 | 1 | 0 |
| `BattleEraseMagicStatus` | 2 | 0 | 0 |
| `BattleFireBlessStatus` | 2 | 0 | 0 |
| `BattleFireWeaponStatus` | 2 | 0 | 0 |
| `BattleGroundEffects` | 4 | 0 | 1 |
| `BattleGuardBrakeStatus` | 2 | 0 | 0 |
| `BattleHealingRecovery` | 1 | 0 | 0 |
| `BattleHitReactionSuppressCatalog` | 1 | 0 | 0 |
| `BattleNaturesTouchStatus` | 2 | 0 | 0 |
| `BattleQuestEncounter` | 19 | 0 | 0 |
| `BattleResistFearStatus` | 1 | 1 | 0 |
| `BattleSpellCatalog` | 132 | 2 | 0 |
| `BattleStormEyeStatus` | 2 | 0 | 0 |
| `BattleStormShotStatus` | 2 | 0 | 0 |
| `BattleTimeAttackEncounter` | 19 | 0 | 0 |
| `BattleTimedStatus` | 14 | 0 | 1 |
| `BattleTournamentNpcPhysicalDamage` | 2 | 0 | 0 |
| `BattleWeightStatus` | 1 | 1 | 0 |
| `BattleWindShackleStatus` | 1 | 1 | 0 |
| `BattleWindShotStatus` | 2 | 0 | 0 |

### 最後兩個方法的證明

正式 CLI JSON 的 `proofMethodCounts` 為 `SAME_CFG=2`、`LAMBDA_PAIRED_BY_CALLSITE=0`、`VISIBILITY_BRIDGE=0`。以下直接來自 `methodResults`；類別與方法計數分開核對。

```text
local/lito381/server/BattleGroundEffects#cast(Llocal/lito381/server/BattleSpellCatalog$GroundSpell;JIIIILlocal/lito381/server/ArenaMapCatalog$Arena;J)Llocal/lito381/server/BattleGroundEffects$CastResult;
SAME_CFG: entry-rooted labelled CFGs equal after local transport

local/lito381/server/BattleTimedStatus#cancelMagicEffects()Llocal/lito381/server/BattleTimedStatus$MagicCancellation;
SAME_CFG: entry-rooted labelled CFGs equal
```

`cast` 的跨區塊重複保存由 #203 的精確 local transport 規則處理；`cancelMagicEffects` 由 CFG 直接證明，不再列舉超過 4096 的路徑。`BattleTimedStatus` 的其他四個方法仍列為 SAME_PATHS，沒有因類別顯示 SAME_CFG 而省略。

前輪指定的 `BattleQuestEncounter#pollPoisons`、`BattleTimeAttackEncounter#pollPoisons`、`BattleCubeHarmonyEffects#advance` 所屬外層類別仍全部為 SAME。完整逐 entry／逐方法結果可由下方腳本重現。歷史 DIFF 全文保留於 git，不再列作本輪未解差異。

## 重現指令

於 worktree 根目錄，將下列 Python 存為 `scratch/issue-67/reproduce-cfg.py`。首次使用空白的 target；若該目錄已有前次產物，改 `target` 為本 worktree 下另一個全新 target 子目錄，腳本不清除或替換舊產物。ASM 9.7 先讀本機 Maven cache，缺少時下載至本 worktree target，並核對固定 SHA-256。

```powershell
& 'C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe' -B -X utf8 scratch/issue-67/reproduce-cfg.py
# 本輪退出碼 0；323 個 entry 全部通過，缺漏／多出皆空。
```

```python
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import urllib.request
import zipfile

root = Path.cwd()
server = root / 'lito伺服器/LiTo381-Server'
scratch = root / 'scratch/issue-67/cfg-final'
target = server / 'target/issue-67/cfg-final'
scratch.mkdir(parents=True, exist_ok=True)
# 每次使用空白的 target 目錄，避免舊 class 混入；重跑時改用新的 target 子目錄。
assert not target.exists(), 'Choose a fresh target directory'
target.mkdir(parents=True)
batch = next(b for b in json.loads((root / 'docs/combat-merge/batches.json').read_text('utf-8'))['batches'] if b['id'] == 'B06')
candidate = server / 'reference/candidate-50635f7e.jar'
formal = server / 'reference/base-0.6.71.jar'
assert hashlib.sha256(candidate.read_bytes()).hexdigest() == '50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729'
assert hashlib.sha256(formal.read_bytes()).hexdigest() == 'e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498'
jdk = Path(shutil.which('javac')).parent
suffix = '.exe' if os.name == 'nt' else ''
java = jdk / ('java' + suffix)
javac = jdk / ('javac' + suffix)

def run(args, name):
    result = subprocess.run([str(a) for a in args], stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    (scratch / (name + '.log')).write_bytes(result.stdout)
    print(name, 'exit', result.returncode)
    print(result.stdout.decode('utf-8', errors='replace'))
    assert result.returncode == 0, name

hashes = {
    'asm': 'adf46d5e34940bdf148ecdd26a9ee8eea94496a72034ff7141066b3eea5c4e9d',
    'asm-tree': '62f4b3bc436045c1acb5c3ba2d8ec556ec3369093d7f5d06c747eb04b56d52b1',
    'asm-analysis': '7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f',
}
jars = []
for name, expected in hashes.items():
    relative = f'org/ow2/asm/{name}/9.7/{name}-9.7.jar'
    jar = Path('D:/dev-cache/maven/repository') / relative
    if not jar.is_file():
        jar = target / (name + '-9.7.jar')
        if not jar.is_file():
            jar.write_bytes(urllib.request.urlopen('https://repo.maven.apache.org/maven2/' + relative).read())
    assert hashlib.sha256(jar.read_bytes()).hexdigest() == expected
    jars.append(str(jar))
asm_cp = os.pathsep.join(jars)
for name in ['as-record', 'semantic-diff']:
    output = target / name
    output.mkdir(exist_ok=True)
    sources = sorted((server / 'tools' / name / 'src/main/java').rglob('*.java'))
    run([javac, '-J-Dfile.encoding=UTF-8', '--release', '17', '-encoding', 'UTF-8', '-cp', asm_cp, '-d', output, *sources], 'build-' + name)
annotations = target / 'annotations'
annotations.mkdir(exist_ok=True)
source = server / 'src/main/java/local/lito381/server'
run([javac, '-J-Dfile.encoding=UTF-8', '--release', '17', '-encoding', 'UTF-8', '-d', annotations, source / 'AsRecord.java', source / 'AsmFinal.java'], 'annotations')
classes = target / 'classes'
classes.mkdir(exist_ok=True)
staged = server / 'merge-inputs/50635f7e/staged/local/lito381/server'
sources = [staged / (name + '.java') for name in batch['classes']]
cp = os.pathsep.join(map(str, [candidate, formal, server / 'lib/*', annotations]))
run([javac, '-J-Dfile.encoding=UTF-8', '--release', '17', '-encoding', 'UTF-8', '-cp', cp, '-d', classes, *sources], 'javac')
converter_cp = str(target / 'as-record') + os.pathsep + asm_cp
run([java, '-Dfile.encoding=UTF-8', '-cp', converter_cp, 'local.lito381.asrecord.AsRecord', classes], 'as-record')
entries = sorted(p.relative_to(classes).as_posix() for p in classes.rglob('*.class'))
missing = sorted(set(batch['verify_entries']) - set(entries))
extra = sorted(set(entries) - set(batch['verify_entries']))
assert len(sources) == 29 and len(entries) == 323 and not missing and not extra
print('entries=323 missing=[] extra=[]')
before = {entry: hashlib.sha256((classes / entry).read_bytes()).hexdigest() for entry in entries}
run([java, '-Dfile.encoding=UTF-8', '-cp', converter_cp, 'local.lito381.asrecord.AsRecord', classes], 'as-record-idempotent')
assert before == {entry: hashlib.sha256((classes / entry).read_bytes()).hexdigest() for entry in entries}
jar = target / 'staged.jar'
with zipfile.ZipFile(jar, 'w') as archive:
    for entry in entries:
        archive.writestr(zipfile.ZipInfo(entry, (1980, 1, 1, 0, 0, 0)), (classes / entry).read_bytes())
selection = scratch / 'verify_entries.txt'
selection.write_text('\n'.join(batch['verify_entries']) + '\n', encoding='utf-8')
selected = {e.removesuffix('.class') for e in batch['verify_entries']}
approval_scope = {}
for name in ['access-widen-approved', 'deviations', 'effects-approved', 'exceptions-exempt', 'extra-methods-approved', 'layout-approved', 'metadata-exempt', 't029-methods']:
    applicable = []
    for raw in (root / 'docs/combat-merge' / (name + '.txt')).read_text('utf-8-sig').splitlines():
        raw = raw.strip()
        if not raw or raw.startswith('#'):
            continue
        parts = raw.split()
        key = parts[1] if parts[0] in ('method-diff', 'ctor-prologue-order') else parts[0]
        if key.split('#')[0].removesuffix('.class') in selected:
            applicable.append(raw)
    approval_scope[name] = applicable
(scratch / 'approved-scope.json').write_text(json.dumps(approval_scope, indent=2), encoding='utf-8')
assert all(not entries for entries in approval_scope.values()), 'Approval lists changed; review and clip before running'
print('B06 applicable approval entries=0')
semantic_cp = str(target / 'semantic-diff') + os.pathsep + asm_cp
run([java, '-Dfile.encoding=UTF-8', '-Xmx2g', '-cp', semantic_cp, 'local.lito381.semanticdiff.SemanticDiff', '--left', jar, '--right', candidate, '--left-classpath', candidate, '--classes', selection, '--json', scratch / 'semantic.json', '--md', scratch / 'semantic.md'], 'semantic')
data = json.loads((scratch / 'semantic.json').read_text('utf-8'))
record_entries = [r['entry'][:-6] for r in json.loads((root / 'docs/combat-merge/record-asm-fields.json').read_text('utf-8'))['records'] if r['class'].split('/')[-1].split('$')[0] in batch['classes']]
assert len(record_entries) == 14
records = [c for c in data['classes'] if c['name'] in record_entries]
assert len(records) == 14 and all(c['status'] == 'SAME' for c in records)
assert data['classSelection']['missingLeft'] == [] and data['classSelection']['extraLeft'] == []
assert {c['name'] + '.class' for c in data['classes']} == set(batch['verify_entries'])
receipt = {'counts': data['counts'], 'recordSame': 14, 'entries': len(entries), 'jarSha256': hashlib.sha256(jar.read_bytes()).hexdigest(), 'sources': {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in sources}}
(scratch / 'receipt.json').write_text(json.dumps(receipt, indent=2), encoding='utf-8')
print(json.dumps(receipt, ensure_ascii=False))
if len(data['classes']) != 323 or any(c['status'] not in {'SAME', 'SAME_EFFECTS', 'SAME_PATHS', 'SAME_CFG'} for c in data['classes']):
    raise SystemExit(2)
```

## 產出路徑

- 交付報告：`docs/reports/issue-67.md`。
- 保留來源：`lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/`，精確 29 檔見 B06.classes。
- 本輪本機證據：`scratch/issue-67/cfg-final/{semantic.json,semantic.md,verify_entries.txt,receipt.json,approved-scope.json,audit.json}` 與各步驟 `.log`；重現／核對腳本在 `scratch/issue-67/`。
- 編譯與轉換產物：`lito伺服器/LiTo381-Server/target/issue-67/cfg-final/{classes,staged.jar,as-record,semantic-diff,annotations}`。生成檔不提交。

## 推測

無需以未驗證推測支持本批通過；結果由已合併工具對本輪重建產物取得。SAME_CFG 是比較器支援模型內的語意證明，不能推論成真機流程已驗收。

## 待驗證／未驗證什麼

- 本批 staged 驗收沒有未完成項；單元測試與回歸依 §6-5 不適用，留待 B-land 統一處理。
- 未啟動伺服器、登入器、遊戲，未連正式 DB、部署或修改正式資源；未讀取機密設定、封存與硬連結資源。
- javac 的 unchecked 警告仍存在。一般 Maven CI 不能取代本報告的 B06 語意驗收；本輪沒有執行整包 Maven 測試。

## 給下一棒的提示

由 Claude 審查既有 PR #89 的 29 份來源與本報告，確認 311 SAME＋10 SAME_PATHS＋2 SAME_CFG、323 entry 集合完整，以及 Java／註解與產物 hash 保留。#203 依賴阻塞已解除，無剩餘 DIFF，不觸發 `needs:claude` 差異裁定分支。審查通過後由使用者合併，再由 B-land 統一落地與回歸；本輪不自行合併 PR 或關閉 Issue。

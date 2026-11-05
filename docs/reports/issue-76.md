# Issue #76 — A05 正式 0.6.71 暫存重驗

## 結論

2026-11-05 已完成本批驗收。27 個 staged Java 重新直接編譯，產生的 160 個 class entry 與 A05 `verify_entries` 完全相同。含已合併 #135、#153、#166 的比較器及既有核准清單得到 **SAME 158／SAME_EFFECTS 1／SAME_LAYOUT 1／DIFF 0**。

本輪承接 `task/76` 的 `e62d2137a95299f0e1fd573f098599d5b9ee138b`，於同一分支合併最新 `origin/main`，沿用主控指定的 27 個 Java。歷史 156 SAME／4 DIFF 與完整原始差異可由該提交的本報告追溯。本輪證據全部由新編譯產物取得。[主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/76#issuecomment-6646188016)

## 做了什麼

- 合併 `origin/main` `450178d59b0157fddf2b9456b7c03d20207993fc`，合併提交為 `a29ce5dcd492e1fbd250e114709782ece8886340`。
- `effects-approved.txt` 的同一筆 forMap 核准條目發生格式衝突，採用 main 完整版本解決。合併後該檔與 main 完全相同。
- 重新編譯本批 Java、獨立重建比較器、核對 class 目錄及 JAR 內的 entry 集合，執行預設與核准兩組比較。
- 比較前後逐一核對 27 個來源、正式 JAR、staged JAR 與 160 個 class 的 SHA-256，確認編譯產物完整性。JAR 內每一個 class 的雜湊亦與 javac 輸出相同。
- 更新本報告與既有 PR #107 描述，交由 Claude 複審。

### 依賴與工具版本

| 項目 | 已合併提交 |
|---|---|
| #135／PR #152 | `4a8d7e5a0c2b1307723f422fba06afcbd16d4caf` |
| #153／PR #164 | `bf50dd7c30b678cf3551d1a1bd511e70f14c33b8` |
| #166／PR #170 | `5089b6bc1a0c5ce421a7d6308cbb5b29970331f2` |
| 本輪比較器最後來源變更 | `c98b4e8e552742ffad3f624dfee646dcb1f4218d` |

工具來源為合併後 worktree 的 `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java`。使用 Microsoft OpenJDK 17.0.17，ASM／asm-tree／asm-analysis 9.7，三個依賴均按工具 `build-support.sh` 的 SHA-256 驗證。asm 與 asm-tree 只讀 `D:/dev-cache/maven/repository`，asm-analysis 從 Maven Central 下載至本 worktree 的 `target/issue-76/revalidated/dependency/`。

## 已驗證

```text
javac --release 17 -encoding UTF-8: exit 0
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
外層 Java = 27
expected = 160, actual = 160, missing = [], extra = []
class 目錄與打包 JAR 的 entry 集合均完全符合 verify_entries
比較器重新編譯 = exit 0
兩次 semantic-diff = exit 0
驗收閘門 = PASS，160 個 entry 逐列均為允許狀態
```

| 模式 | SAME | SAME_EFFECTS | SAME_LAYOUT | DEVIATION | DIFF | ONLY_LEFT | ONLY_RIGHT | BYTE_SAME |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 預設正規化，無核准清單 | 159 | 0 | 0 | 0 | 1 | 0 | 0 | 4 |
| A05 核准清單 | 158 | 1 | 1 | 0 | 0 | 0 | 0 | 4 |

兩組 `classSelection.missingLeft`、`extraLeft` 皆空，`leftClassCount`、`comparedClassCount` 均為 160。驗收同時檢查逐列狀態、entry 集合與總數。

| 原差異 | 本輪結果與處理 |
|---|---|
| `Main#accept` | 核准模式下其餘方法全部通過完整比較。#153 的保護區間邊界收斂與 #166 的 local webs 處理 RETURN／Throwable 槽位差異。 |
| `Main#main` | `SAME_LAYOUT`。#166 處理 System.out 單次暫存，#153 使用已核准 CFG 比對處理 cleanup 排列。 |
| `ClassicWorldMapCatalog#forMap` | 核准模式 `SAME_EFFECTS`，預設模式亦為 `SAME`。#166 處理 null 檢查後回傳的 stack／local 暫存。 |
| `NativeAcceptedState` | `SAME`，#135 正規化引用型 InnerClasses。 |
| `NativeCastTimeline` | `SAME`，#153 依 lambda 內容比對與去重。 |

上述歸因依已合併工具實作及 #166 報告，本輪直接驗證兩組結果。預設組唯一 DIFF 為 `Main#main([Ljava/lang/String;)V` 的 try 表排列，第一差異如下。

```text
left=TRY L10 L36 L37 java/lang/Throwable
right=TRY L10 L35 L36 java/lang/Throwable
```

核准組無剩餘差異。`Main.main` 的 `approvedMethods` 明確為 `SAME_LAYOUT`，`forMap` 的 `effectsMethods` 明確為 `SAME_EFFECTS`。

### 核准清單裁切

按完整 160 個驗收類名裁切 main 的六份清單，只傳入有命中條目的清單。空清單會被 CLI 拒絕，故其餘四項省略旗標。#166 的六類正規化採預設啟用，並以 `--left-classpath reference/base-0.6.71.jar` 提供實際編譯依賴。

| 原始清單 | A05 命中 | 原始檔 SHA-256 |
|---|---:|---|
| `effects-approved.txt` | 1 | `b8ecf18b0595beadd278534735b6df6279ae8aa9cc3d8a256d7cd72e7a29c11f` |
| `layout-approved.txt` | 1 | `280a45a28d8e636db43b94b21d67213c865b667bbb738c891422b7b459eb4c11` |
| `extra-methods-approved.txt` | 0 | `8151304a004b559589a33f1fb003443e6a21ec8840e247ce19276cf53220e48d` |
| `metadata-exempt.txt` | 0 | `f6289ee43b323de1cc91ba7860ccc782e114d03391f6ab5f3a01c413be0fb456` |
| `access-widen-approved.txt` | 0 | `73f9da45dfd7ab0a20fc082f7e8282dd8d97a691da2569a8b0b6ae16bddea46b` |
| `deviations.txt` | 0 | `54ccc1cf1f8c34816a510ceb18736df4581a16c1af503e09a6b1c7aa59b40526` |

實際傳入的兩筆精確方法如下，核准連結保留於裁切檔。

```text
--effects-methods
local/lito381/server/ClassicWorldMapCatalog#forMap(I)Llocal/lito381/server/ClassicWorldMapCatalog$ClassicMap;
--layout-methods
local/lito381/server/Main#main([Ljava/lang/String;)V
```

## 編譯與比較指令

從本 worktree 根目錄執行，下列為實際腳本內的指令形狀。`<27 sources>` 依 `a-batches.json` 的 A05 `classes` 展開，`<tool sources>` 取比較器全部主來源，`$jdk` 為 `C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin`。

```powershell
& "$jdk/javac.exe" --release 17 -encoding UTF-8 `
  -cp "$server/reference/base-0.6.71.jar;$server/lib/*" `
  -d "$out/classes" <27 sources>
& "$jdk/jar.exe" --create --date=2026-11-05T00:00:00Z `
  --file "$out/a05.jar" -C "$out/classes" .
& "$jdk/javac.exe" --release 17 -encoding UTF-8 `
  -cp "$asm;$asmTree;$asmAnalysis" -d "$out/tool" <tool sources>
& "$jdk/java.exe" '-Dfile.encoding=UTF-8' `
  -cp "$out/tool;$asm;$asmTree;$asmAnalysis" `
  local.lito381.semanticdiff.SemanticDiff `
  --left "$out/a05.jar" --right "$server/reference/base-0.6.71.jar" `
  --left-classpath "$server/reference/base-0.6.71.jar" `
  --classes "$evidence/entries.txt" `
  --effects-methods "$evidence/effects-approved.txt" `
  --layout-methods "$evidence/layout-approved.txt" `
  --json "$evidence/approved.json" --md "$evidence/approved.md"
```

預設組省略 effects／layout 兩個旗標，輸出為 `strict.json`、`strict.md`。完整 argv、stdout 與退出碼位於 `commands.json` 和各 `.log`。

### 可重跑腳本

本輪實際執行 `scratch/issue-76/revalidated/verify.py`。以下附相同內容，供在另一 worktree 重建證據。先備妥上述三個 ASM 依賴，再以 Python 3.12 的 `-B -X utf8` 執行。`classes`／`tool` 須為全新目錄，可把腳本內兩處 `revalidated` 改成新的驗證目錄。JAR 時間固定，報表內時間與絕對路徑會隨重跑改變。

```python
import hashlib
import json
from pathlib import Path
import subprocess
import zipfile

root = Path.cwd()
server = root / 'lito伺服器/LiTo381-Server'
out = server / 'target/issue-76/revalidated'
evidence = root / 'scratch/issue-76/revalidated'
jdk = Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
batch_file = root / 'docs/combat-merge/a-batches.json'
batch = next(b for b in json.loads(batch_file.read_text(encoding='utf-8'))['batches'] if b['id'] == 'A05')
reference = server / 'reference/base-0.6.71.jar'
stage = server / 'merge-inputs/formal-0.6.71/staged/local/lito381/server'
sources = [stage / (name + '.java') for name in batch['classes']]
entries = batch['verify_entries']
assert len(sources) == len(set(sources)) == 27
assert len(entries) == len(set(entries)) == 160
commands = []

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def save(name, value):
    (evidence / name).write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def run(name, args):
    command = [str(arg) for arg in args]
    result = subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, encoding='utf-8', errors='replace')
    (evidence / (name + '.log')).write_text(result.stdout, encoding='utf-8')
    commands.append({'name': name, 'argv': command, 'exit': result.returncode})
    save('commands.json', commands)
    print(name, 'exit', result.returncode, flush=True)
    if result.returncode:
        raise RuntimeError(result.stdout)

def manifest(paths):
    return {str(p.relative_to(root)).replace('\\', '/'): sha(p) for p in sorted(paths)}

for directory in ['classes', 'tool']:
    (out / directory).mkdir(exist_ok=False)
assert sha(reference) == 'e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498'
asm = [Path('D:/dev-cache/maven/repository/org/ow2/asm') / artifact / '9.7' / (artifact + '-9.7.jar') for artifact in ['asm', 'asm-tree']]
asm.append(out / 'dependency/asm-analysis-9.7.jar')
assert [sha(p) for p in asm] == [
    'adf46d5e34940bdf148ecdd26a9ee8eea94496a72034ff7141066b3eea5c4e9d',
    '62f4b3bc436045c1acb5c3ba2d8ec556ec3369093d7f5d06c747eb04b56d52b1',
    '7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f']
save('source-hashes.json', manifest(sources))
(evidence / 'entries.txt').write_text('\n'.join(entries) + '\n', encoding='utf-8')
run('javac-version', [jdk / 'javac.exe', '-version'])
run('javac', [jdk / 'javac.exe', '--release', '17', '-encoding', 'UTF-8', '-cp', str(reference) + ';' + str(server / 'lib/*'), '-d', out / 'classes', *sources])
actual = sorted(p.relative_to(out / 'classes').as_posix() for p in (out / 'classes').rglob('*.class'))
selection = {'expected': len(entries), 'actual': len(actual), 'missing': sorted(set(entries) - set(actual)), 'extra': sorted(set(actual) - set(entries))}
save('entry-audit.json', selection)
assert actual == sorted(entries), selection
class_hashes = manifest((out / 'classes').rglob('*.class'))
save('class-hashes.json', class_hashes)
run('jar', [jdk / 'jar.exe', '--create', '--date=2026-11-05T00:00:00Z', '--file', out / 'a05.jar', '-C', out / 'classes', '.'])
with zipfile.ZipFile(out / 'a05.jar') as jar:
    assert sorted(n for n in jar.namelist() if n.endswith('.class')) == sorted(entries)
    for entry in entries:
        assert hashlib.sha256(jar.read(entry)).hexdigest() == sha(out / 'classes' / entry)
tool_sources = sorted((server / 'tools/semantic-diff/src/main/java').rglob('*.java'))
save('tool-source-hashes.json', manifest(tool_sources))
asm_cp = ';'.join(str(p) for p in asm)
run('tool-javac', [jdk / 'javac.exe', '--release', '17', '-encoding', 'UTF-8', '-cp', asm_cp, '-d', out / 'tool', *tool_sources])
selection_names = {e.removesuffix('.class') for e in entries}
options = {'effects-approved.txt': '--effects-methods', 'layout-approved.txt': '--layout-methods', 'extra-methods-approved.txt': '--extra-methods', 'metadata-exempt.txt': '--metadata-exempt', 'access-widen-approved.txt': '--access-widen', 'deviations.txt': '--deviations'}
approved_args = []
list_audit = {}
for filename, flag in options.items():
    source = root / 'docs/combat-merge' / filename
    selected = []
    for line in source.read_text(encoding='utf-8-sig').splitlines():
        text = line.strip()
        if not text or text.startswith('#'):
            continue
        owner = text.removeprefix('ctor-prologue-order ').split('#')[0].strip()
        if owner in selection_names:
            selected.append(line)
    list_audit[filename] = {'source_sha256': sha(source), 'selected': selected}
    if selected:
        target = evidence / filename
        target.write_text('\n'.join(selected) + '\n', encoding='utf-8')
        approved_args += [flag, target]
save('approval-audit.json', list_audit)
base = [jdk / 'java.exe', '-Dfile.encoding=UTF-8', '-cp', str(out / 'tool') + ';' + asm_cp, 'local.lito381.semanticdiff.SemanticDiff', '--left', out / 'a05.jar', '--right', reference, '--left-classpath', reference, '--classes', evidence / 'entries.txt']
inputs_before = manifest(sources + [reference, out / 'a05.jar'])
for mode, flags in [('strict', []), ('approved', approved_args)]:
    run(mode, base + flags + ['--json', evidence / (mode + '.json'), '--md', evidence / (mode + '.md')])
    result = json.loads((evidence / (mode + '.json')).read_text(encoding='utf-8'))
    print(mode, result['counts'], flush=True)
assert inputs_before == manifest(sources + [reference, out / 'a05.jar'])
assert class_hashes == manifest((out / 'classes').rglob('*.class'))
assert not result['classSelection']['missingLeft'] and not result['classSelection']['extraLeft']
assert len(result['classes']) == 160
assert all(c['status'] in ['SAME', 'SAME_EFFECTS', 'SAME_LAYOUT'] for c in result['classes'])
save('hashes.json', manifest([batch_file, reference, out / 'a05.jar', evidence / 'verify.py', *evidence.glob('*.txt'), *evidence.glob('*.json'), *evidence.glob('*.md'), *evidence.glob('*.log')]))
print('PASS 160 entries; application class hashes unchanged', flush=True)
```

## 輸入與輸出 SHA-256

| 路徑 | SHA-256 |
|---|---|
| `docs/combat-merge/a-batches.json` | `ac1c17ec1a446564c6fb912235ecd9222d06bef1a3a91cdcb3836fda158ad65d` |
| `lito伺服器/LiTo381-Server/reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `lito伺服器/LiTo381-Server/target/issue-76/revalidated/a05.jar` | `e347988dcf41b60cb915195ffa98dc2eff74303ae3c9f0400c2800bf5bc2b553` |
| `scratch/issue-76/revalidated/approval-audit.json` | `59d7b5f37fbc0dd39f47e01fd1288f73a3536ff5b44e3065ca0d1f80350d55f2` |
| `scratch/issue-76/revalidated/approved.json` | `a0cf4be1a28c049d9b9067a91ce68b3a3197a65d382b1505d07d889cc6deef2f` |
| `scratch/issue-76/revalidated/approved.md` | `267ec54e1282abee182db5c989a08cb0798f967a5fa0d3faa4acc2627ed9dd74` |
| `scratch/issue-76/revalidated/class-hashes.json` | `2ce964c548d25ec178557a04813857b565986364afee924a34470565314f6349` |
| `scratch/issue-76/revalidated/commands.json` | `b3ea7db89c95015e75afbfcd67fe762f24acde46fe9786bd3b67b8e2e8a8e6a1` |
| `scratch/issue-76/revalidated/effects-approved.txt` | `3c0602f111d67bd3dc5b188cfa5fe8f4faaa6ab80864f7daa56a78abca56ff9c` |
| `scratch/issue-76/revalidated/entries.txt` | `3916f41ecd5fb67979ac5522d564fcf193e7b0689c7320f77dfabe3e4756a462` |
| `scratch/issue-76/revalidated/entry-audit.json` | `b9ac9b65f7cab8d8eb61906fb2ee179ceb6ae1daeb6e0d385f9eb67773c55a56` |
| `scratch/issue-76/revalidated/layout-approved.txt` | `8732439b75b790cb637d92dc4f5b69ac3bf4295655ea6965e26096da72ea1ba4` |
| `scratch/issue-76/revalidated/source-hashes.json` | `d9165e178382a4be53d13077216d66d80528d114146139b03ad6919ce769e74a` |
| `scratch/issue-76/revalidated/strict.json` | `6625c0e04666e6ba9417dbc0b14fbb1a3e63ead94170caa2b58c7df004e448c2` |
| `scratch/issue-76/revalidated/tool-source-hashes.json` | `045ab410a8a840bfae623b4bd2880f4f3605419403c24913507bd343e809d075` |
| `scratch/issue-76/revalidated/verify.py` | `447fbae43b9e2bac107623074dfc10b7ae5625e0bc1f5bf675c57cdb186fd6c2` |

### 27 個 staged Java

共同前綴為 `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/`。

| 檔案 | SHA-256 |
|---|---|
| `AccountRepository.java` | `1d742440ceb0df5c305618be4a372b469a98fee8ee2b2d47609955bdcb97a71f` |
| `CaptureLog.java` | `0bd4a0cf6d9be4bf618a7ede4280e0131cf7afb3c2bc2f3bf2e17e223e04e78d` |
| `ClassicWorldAdenToiCatalog.java` | `0f0b14e11eb597f99b52e4febd90cfbbebf255b03d83495f857044b84a4103f2` |
| `ClassicWorldIvoryTowerCatalog.java` | `21f400e7a913ff8a6839df8590edd6d1b023d53e04e0af2273975b4d0cc6f7d8` |
| `ClassicWorldMapCatalog.java` | `6cb0c83d585cb5248d9593063caaa83b3df0015cafb83c82c659462cb951f015` |
| `ClassicWorldNpcCatalog.java` | `894ddaab3e7dcf0ec0f0ab5245e6f646606748c05e11adf8e922d69112e1a6ca` |
| `GameStateStore.java` | `f25943a7827aeb97d39d2eb2db99bb3e0649f17d27d6878850a54a06d47d68e2` |
| `InventoryCatalog.java` | `3c6ca6669bd8d39c64a6dee6c092d259ca9fc02781372e4b0ee3dc3ef4f0013a` |
| `LiToGmCommands.java` | `60356b72a8f3f9833dbaf24d1df584688535a69a15442f4365b56ee27c19ae52` |
| `LiToItemDetails.java` | `91ef0d74c55ce75a6a82ac3423383b06409bb73629f5fa8f623ad6d8e792f271` |
| `LitoMapRepository.java` | `9ff57ff0e431a0a07a4e15f56fd49edb7dd4bb943c179ee42df00c657543b4d9` |
| `Main.java` | `631500932dfd41588aa14771fd45ccb3df4e1792e6290c25c5f80408c09c92c3` |
| `MemoryAccountRepository.java` | `a08bf4a0129b89b3c2a58a70eefdb63ed631ad8ce5d9fb5389a7a90369c80a35` |
| `NativeAcceptedHpContext.java` | `a948e7e0e8d9b984c2882fee293d3e33f0121b9eea8770be7621ad5831907f79` |
| `NativeAcceptedState.java` | `fed22c9aa6493d1dcb59a232359e208f5f9170fabe1812e4034657edb8412651` |
| `NativeCancellationSpeedImmunity.java` | `e2661344b31efc7376c2aba916edf0dabb0acdaddfead76f61604d992e9b2192` |
| `NativeCastMaterialPolicy.java` | `aab7c9deda973af8a9c81675e3f6de68ce8b82cca144f47948e9e2b401e7ddb7` |
| `NativeCastTimeline.java` | `4c3e498bd4af18d81da437448c36df242dd1507e60f01e80e3b2d44ac95ea109` |
| `NativeCompanionCallbacks.java` | `41e1e9c3842bbac537b4649274860fc976a00c3da482f66396e0a5d0b365f05b` |
| `NativeCompanionCommands.java` | `801a919ffd53461e8e87ec7e83079f6c77f89a87a5fec64cb8bafce0b8b2f378` |
| `NativeCompanionCommit.java` | `37115b883a0e8f39e2b47dcc3a16b52fe4575526850a758475598c4281c2b3d6` |
| `NativeCompanionMutation.java` | `405b1d5a000d41d70c971c9bf538a710e4e6d4bc4661409a27f626e82dbeb44b` |
| `NativeLitoMapTableCatalog.java` | `f300f2aa08d6e48264a20d58b1f4323803ceeda5642e4c20c4ed55580ae3f4fd` |
| `NativeLitoRentalTownNpcCatalog.java` | `b603100771340aa974b9173ae3860eef561b75133ef7a2bcf3252d2739d9ef83` |
| `NativeLitoTownPortalCatalog.java` | `eb49c5da738b828a8290c28e8368955d12f981576ec26ac25d782920025d13fa` |
| `NativeLitoZoneCatalog.java` | `c1a351d151fa601c3ff3fdb172d6a4dbb76b53cbb3235ddd07a97874022a02e0` |
| `NativeMagicalWeaponAction.java` | `8d516b22f50adc861f45da15ae8c1656a038890c184bcd2936c68fe516e6cfde` |

## 產出路徑

- 追蹤報告為 `docs/reports/issue-76.md`，PR #107 包含本批既有 27 個 staged Java。
- 編譯輸出與打包 JAR 位於 `lito伺服器/LiTo381-Server/target/issue-76/revalidated/`。
- 本機證據位於 `scratch/issue-76/revalidated/`，含重跑腳本、完整 argv、逐 entry JSON／Markdown、核准清單裁切、來源／class／工具來源雜湊收據及 log。
- 本輪自行編輯的追蹤檔為本報告。PR 相對最新 main 的檔案範圍為本報告與 A05 的 27 個 staged Java。main 帶入的建置接線、比較器、索引等由合併提交保留。

## 推測

本輪驗收直接採實測結果。歷史 ASM 改寫來源的推測留於先前提交供追溯。

## 未驗證什麼

- 單元測試、完整 Maven／執行時回歸依任務安排留待 A-land。
- 本輪驗證範圍為 A05 的直接編譯與語意比對。比較器 709 項自測與 CI 證據由已合併 #166 報告提供，本輪未重跑工具自測。
- 尚待使用者於後續整合階段進行真機驗收，本輪未執行伺服器、登入器、遊戲或 DB 操作。

## 給下一棒的提示

Claude 審查 PR #107，核對 main 合併、27 個來源雜湊、160 個完整 entry 與核准結果。審查通過後由使用者合併，調度器更新索引與任務狀態。A-land 執行器接手整批落地及單元／回歸測試，沿用精確 effects／layout 核准並再次核對集合。A05 本輪驗收已全數通過。

# Issue #64 — T020-B03 戰鬥併入暫存

## 結論

**部分完成，需 `needs:claude` 裁定跨批次 private 介面。** 已交付 B03 的 51 份 staged Java。完整整批 javac 失敗，唯一錯誤為 `NativeSourceCubeVisibility.Work implements NativeLightAction.Output` 的 private 存取。排除該外層檔案的診斷編譯成功，131 個產物全部 SAME，完整 135 個驗收 entry 中仍缺 4 個。

## 做了什麼

- 從凍結 `sources/local/lito381/server/` 複製 B03 51 檔作起點，使用候選 JAR 的 CFR 0.152 反編譯內容逐項移植。比對也涵蓋題述 23 個 asm-only entry 以外的實際差異，補上 target mask、裝備輸出與 Icons1 等後續改寫。
- 補齊傷害 Plan 的全部欄位、接受 HP 後的生命週期回呼、玩家來源保留、cube 顯示與模板守門、娃娃觀眾／被動能力／慢速 epoch、裝備觀眾狀態，以及 ground、fatality、healing、kiringku、light-pair 的轉呼叫。
- 依 #64 主控裁定，6 個 record 使用 `@AsRecord`。`DollDefense.Receiver`、`DollOutput.Receiver`、`EquipmentOutput.Viewer` 另用 `finalFields` 保留舊建構子的預設欄位值。元件、建構子、額外欄位與 accessor 均由 staged Java 編譯。
- `NativeSourceDamageHealingSelection.Cursor.nativeSourceTargetMask` 在候選是 final，但舊建構子沒有賦值，新增建構子才賦值。因此使用 #153 擴充的 `@AsmFinal`，交同一個 #102 轉換器補回旗標。該 entry 已驗證 SAME。
- 候選在換用 detached task 後仍保留 DollOutput 與 EquipmentOutput 的舊 private lambda 方法。本批將保留的方法體直接寫為 Java `lambda$send$1`／`lambda$send$3`，其內部 lambda 仍由 javac 產生。兩個外層 entry 均 SAME。

## 已驗證

### 工作基準與範圍

- 日期為 2026-11-05，Windows，Microsoft OpenJDK 17.0.17，Python 3。工作目錄為 `D:/lineage開發/3-lito開發/scratch/wt/task-64`，分支 `task/64`。
- 初始 HEAD 為 `a87645e`，執行 fetch 後快轉至 `450178d5`，包含 #102、#153、#166。驗證工具直接從此基準的 Java 來源編譯。
- 提交範圍為 B03 的 51 份 staged Java 與本報告。原始專案資源採唯讀存取。此次工作區變更以快轉後的 `origin/main` 為基準核對。
- 編譯 classpath 使用候選、正式 JAR、`lib/*`，外加獨立目錄中的 `AsRecord`／`AsmFinal` 編譯期註解。應用產物只收集本批 javac 輸出，註解 class 留在獨立目錄。
- 唯一應用 class 後處理為現有 AsRecord CLI。CFR 輸入用的抽取 JAR 放在 target，與編譯及比對產物分開。語意比較採預設嚴格模式及真實左側 classpath。

### 編譯與比對結果

初始凍結來源整批編譯有 36 個錯誤。完成本批移植後，完整 51 檔重跑輸出如下，退出碼 1。

```text
NativeSourceCubeVisibility.java:7: error: Output has private access in NativeLightAction
 static final class Work implements NativeLightAction.Output {
                                                     ^
Note: NativeSourceCubeVisibility.java uses unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
1 error
```

診斷編譯只排除 `NativeSourceCubeVisibility.java`，其餘 50 檔同時交給一次 javac，退出碼 0，stdout／stderr 均空。轉換器與比較器輸出如下。

```text
as-record: scanned=131 converted=7
SUMMARY SAME=131 SAME_EFFECTS=0 SAME_LAYOUT=0 DEVIATION=0 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=4 BYTE_SAME=0 ALLOW_METHODS=0
```

轉換數 7 包含 6 個 AsRecord 與 1 個 AsmFinal。完整 `verify_entries` 照常傳入比較器，`missingLeft` 恰有下列 4 個，`extraLeft=[]`。它們的狀態均為 ONLY_RIGHT。

```text
local/lito381/server/NativeSourceCubeVisibility.class
local/lito381/server/NativeSourceCubeVisibility$Cache.class
local/lito381/server/NativeSourceCubeVisibility$Rebuild.class
local/lito381/server/NativeSourceCubeVisibility$Work.class
```

### 唯一剩餘阻塞與主控裁定需求

候選的 `NativeLightAction$Output` 在 InnerClasses metadata 宣告為 private static，NestHost 為 NativeLightAction。B03 的 Work 屬於 NativeSourceCubeVisibility，Java 存取檢查會拒絕直接實作。候選的 Work 介面清單要求 `local/lito381/server/NativeLightAction$Output`，此處改用其他介面會造成類別屬性差異。

```text
javap -p -v -classpath reference/candidate-50635f7e.jar 'local.lito381.server.NativeLightAction$Output'
NestHost: class local/lito381/server/NativeLightAction
InnerClasses:
  private static ... // Output=class local/lito381/server/NativeLightAction$Output of class local/lito381/server/NativeLightAction
```

`NativeLightAction` 屬 B08，超出本次檔案範圍。請主控裁定其 Java 可見性與 B 批次編譯依賴安排，並明確指定可重現的驗收方式。完整批次尚未生成可比對的 JAR，因此這 4 個 entry 的阻塞證據是 javac 存取錯誤及候選 metadata。其餘 131 個 entry 正規化後完整比較皆 SAME，剩餘 DIFF 清單為空。

## 怎麼驗證

下列完整腳本存為本 worktree 的 `scratch/issue-64/verify.py`，在 worktree 根目錄執行。JDK 與本機 ASM 快取路徑已明列，`asm-analysis` 缺少時下載至本 worktree target 並驗 SHA-256。應用 classes、工具 classes 與 JAR 都在 target，參數檔及輸出都在 scratch。使用乾淨 worktree 執行可重新取得下列結果。

```powershell
$env:PYTHONIOENCODING = 'utf-8'
& D:/dev-orchestrator/tools/python/python.exe scratch/issue-64/verify.py all
# 預期 exit 1，唯一 private Output 錯誤。
& D:/dev-orchestrator/tools/python/python.exe scratch/issue-64/verify.py diagnostic
# 預期 javac exit 0，131 SAME、4 ONLY_RIGHT，extraLeft 空。
```

```python
import json
import hashlib
import os
from pathlib import Path
import subprocess
import sys
import zipfile
import urllib.request

ROOT = Path(__file__).resolve().parents[2]
SERVER = ROOT / 'lito伺服器/LiTo381-Server'
WORK = ROOT / 'scratch/issue-64'
TARGET = SERVER / 'target/issue-64'
TARGET.mkdir(parents=True, exist_ok=True)
JDK = Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
BATCH = next(b for b in json.loads((ROOT / 'docs/combat-merge/batches.json').read_text('utf-8'))['batches'] if b['id'] == 'B03')
WORK.mkdir(parents=True, exist_ok=True)
(WORK / 'entries.txt').write_text('\n'.join(BATCH['verify_entries']) + '\n', 'utf-8')
analysis = TARGET / 'asm-analysis-9.7.jar'
if not analysis.exists():
    analysis.write_bytes(urllib.request.urlopen('https://repo.maven.apache.org/maven2/org/ow2/asm/asm-analysis/9.7/asm-analysis-9.7.jar').read())
assert hashlib.sha256(analysis.read_bytes()).hexdigest() == '7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f'
STAGED = SERVER / 'merge-inputs/50635f7e/staged/local/lito381/server'
CP = ';'.join(str(SERVER / p) for p in ['reference/candidate-50635f7e.jar', 'reference/base-0.6.71.jar', 'lib/*'])
ASM = ';'.join(str(Path('D:/dev-cache/maven/repository/org/ow2/asm') / a / '9.7' / (a + '-9.7.jar')) for a in ['asm', 'asm-tree']) + ';' + str(TARGET / 'asm-analysis-9.7.jar')

def run(args, log):
    args = [args[0], '-J-Dfile.encoding=UTF-8', *args[1:]] if str(args[0]).endswith('javac.exe') else args
    args = [args[0], '-Dfile.encoding=UTF-8', *args[1:]] if str(args[0]).endswith('java.exe') else args
    result = subprocess.run([str(a) for a in args], stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    output = result.stdout.decode('utf-8', errors='replace')
    (WORK / log).write_text(output, 'utf-8')
    print(output, end='')
    print('exit', result.returncode)
    return result.returncode

mode = sys.argv[1] if len(sys.argv) > 1 else 'all'
classes = TARGET / mode
classes.mkdir(parents=True, exist_ok=True)
annotations = TARGET / 'annotations'
annotations.mkdir(exist_ok=True)
run([JDK / 'javac.exe', '--release', '17', '-encoding', 'UTF-8', '-d', annotations, SERVER / 'src/main/java/local/lito381/server/AsRecord.java', SERVER / 'src/main/java/local/lito381/server/AsmFinal.java'], 'annotations.log')
sources = [STAGED / (name + '.java') for name in BATCH['classes'] if mode == 'all' or name != 'NativeSourceCubeVisibility']
args = ['--release', '17', '-encoding', 'UTF-8', '-cp', CP + ';' + str(annotations), '-d', str(classes), *map(str, sources)]
(WORK / (mode + '-javac.args')).write_text('\n'.join('"' + arg.replace('\\', '/') + '"' for arg in args), 'utf-8')
if run([JDK / 'javac.exe', '@' + str(WORK / (mode + '-javac.args'))], mode + '-javac.log'):
    sys.exit(1)
for name, folder in [('as-record', 'local.lito381.asrecord.AsRecord'), ('semantic-diff', 'local.lito381.semanticdiff.SemanticDiff')]:
    out = TARGET / name
    out.mkdir(exist_ok=True)
    if run([JDK / 'javac.exe', '--release', '17', '-encoding', 'UTF-8', '-cp', ASM, '-d', out, *sorted((SERVER / 'tools' / name / 'src/main/java').rglob('*.java'))], name + '-build.log'):
        sys.exit(1)
if run([JDK / 'java.exe', '-cp', str(TARGET / 'as-record') + ';' + ASM, 'local.lito381.asrecord.AsRecord', classes], mode + '-as-record.log'):
    sys.exit(1)
jar = TARGET / (mode + '.jar')
with zipfile.ZipFile(jar, 'w') as archive:
    for path in sorted(classes.rglob('*.class')):
        archive.write(path, path.relative_to(classes).as_posix())
run([JDK / 'java.exe', '-cp', str(TARGET / 'semantic-diff') + ';' + ASM, 'local.lito381.semanticdiff.SemanticDiff', '--left', jar, '--right', SERVER / 'reference/candidate-50635f7e.jar', '--left-classpath', SERVER / 'reference/candidate-50635f7e.jar', '--classes', WORK / 'entries.txt', '--json', WORK / (mode + '.json'), '--md', WORK / (mode + '.md')], mode + '-semantic.log')
data = json.loads((WORK / (mode + '.json')).read_text('utf-8'))
print(json.dumps(data['counts']))
for entry in data['classes']:
    if entry['status'] != 'SAME':
        print(json.dumps(entry, ensure_ascii=False))

```

### 輸入與輸出 SHA-256

| 檔案 | SHA-256 |
| --- | --- |
| `candidate-50635f7e.jar` | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| `base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `batches.json` | `985ce87719338034c9b9d42834de2ecdb41fc2e7a9ab3894fc5a23622d03802e` |
| `CFR 0.152` | `f686e8f3ded377d7bc87d216a90e9e9512df4156e75b06c655a16648ae8765b2` |
| `diagnostic.jar` | `87c6f81cc7afa225c8423c6a7297e9f83d3477a49caa81f82c90fdddabd48437` |
| `diagnostic.json` | `a97d30c10c0f8ac224c245ff3d5519238410339f0e5aa15cc8ba132ccb048e85` |

### 凍結來源差異

20 份 Java 已移植，其餘 31 份與凍結來源 bytes 相同。移植檔案如下。

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

下列名稱共同前綴為 `local/lito381/server/`，對應 `.class`。

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
| `NativeSourceCubeVisibility` | ONLY_RIGHT |
| `NativeSourceCubeVisibility$Cache` | ONLY_RIGHT |
| `NativeSourceCubeVisibility$Rebuild` | ONLY_RIGHT |
| `NativeSourceCubeVisibility$Work` | ONLY_RIGHT |
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

主控若提供可編譯且符合候選介面契約的 NativeLightAction 依賴，應可繼續驗證 CubeVisibility 4 個 entry。這部分仍需重新整批編譯及比對。

## 待驗證／未驗證什麼

- 51 檔整批 javac 成功、135 個 entry 全 SAME 與 missingLeft 清空，均待跨批次 private 介面裁定。
- 單元測試與執行期回歸依任務規格留 B-land。真機登入、遊戲與正式 DB 驗收留後續階段。

## 產出路徑

- `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/` 的 B03 51 份 Java。
- `docs/reports/issue-64.md`。
- 本機重建證據在 `scratch/issue-64/`，含 CFR、`all-javac.log`、`diagnostic-javac.log`、`diagnostic.json`／`.md` 及完整參數檔。
- 本機產物在 `lito伺服器/LiTo381-Server/target/issue-64/`。上述暫存及產物由重現指令生成。

## 給下一棒的提示

由 Claude 主控接手 private Output 的跨批次裁定。B08 需協調 NativeLightAction 的可見性與本批編譯依賴，裁定後再由本機執行器對全部 51 檔重跑。請以本報告的 131 SAME／4 ONLY_RIGHT 作為診斷基準，完整批次驗收仍待完成。

遠端 task/64 原有退回提交 `c79e9e0d00dc846bc49aad87907a3c67912674b7`。本輪以最新 main 重新開始，交付時保留舊分支提交歷史，以本輪檔案內容建立合併提交，再使用一般 push 更新 task/64。

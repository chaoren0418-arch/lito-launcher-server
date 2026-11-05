# Issue #74 — A03 正式 0.6.71 暫存重驗

## 結論

2026-11-05 續跑已完成編譯與全部 88 entry 的語意檢查，結果為 **85 SAME、1 DEVIATION、2 DIFF**，missing／extra 均空，`acceptancePass=false`。本批最終語意驗收仍待主控裁定 switch 欄位正規化問題。交付分支 `task/74`，PR #100。

本次額外採用已合併的 #174 工具與核准清單，從固定主線提交 `18c80178f579b36aa227786e9b79638dc6af6db8` 讀取到 scratch 後編譯。工具 Java 最後變更提交為 `65be0a1d5dc12b63b6c7cc51d171eff3332cb858`。staged 來源提交為 `1b38344b678c889de2e89759139f37f6dc18fb1d`，本次唯一追蹤檔變更為本報告。此方式讓工具版本可重現，同時遵守本批檔案範圍。

## 做了什麼

既有提交已完成下列來源修正，本次以重新建立的直接 javac 產物逐項核對。

- `NativeDeVisibilityCoordinator.tick` 恢復正式版 check 成功後才更新玩家的條件區塊。
- `NativeDeVisibilityPc` 的已知物件快照採用 `List` 呼叫型別，對齊 range 分支與 GM HP bar 的明確布林返回。
- `NativeDeWorldContext.attach` 將欄位發佈與 showId 寫入置於物件 monitor 內，返回時先離開 monitor，再由 finally 釋放 admission read lock。
- `NativeDeWorldCore` 將 `actualMapId` 對齊為 int，讓 Map key 使用 `Integer.valueOf`，並對齊 log 裝箱、呼叫型別、switch 出口、guard、pattern variable、lambda、陣列自增與暫存。
- `NativeDeVisibilitySwitch` 欄位宣告順序依正式 class 為 RepairKind、Phase，初始化順序為 Phase、RepairKind。
- `NativeDeWorldLog` 僅有一個指定的 `default debug(String,Object,Object)`，內文為 `emit("debug", message, new Object[]{a, b})`，依既有清單列為 DEVIATION。
- 47 份來源均保有正式版 DECOMPILED 標頭。前輪 CFR 0.152／Vineflower 1.11.2 還原紀錄可由既有提交追溯。

## 已驗證

本 worktree 已含 #108、#135、#153、#166 的合併提交。此次從主線讀取的 #174 快照也包含這些依賴。

| 依賴 | PR | 合併提交 |
|---|---|---|
| #108 | #147 | `a5f9f66` |
| #135 | #152 | `4a8d7e5` |
| #153 | #164 | `bf50dd7` |
| #166 | #170 | `5089b6b` |
| #174 | #178 | `6b769a4` |

使用 Microsoft OpenJDK 17.0.17+10-LTS、Python 3.12.10、ASM 9.7。重建 9 份 compile-only stub、11 份工具來源，再直接編譯 A03 的 47 Java。輸出 JAR 恰為 88 個指定 class，逐 entry bytes 與 javac 輸出一致。打包日期固定為 `2026-11-05T00:00:00Z`。JAR SHA-256 與前輪直接產物相同。

`VisibilityCoordinator`、`WorldContext`、`WorldCore`、`WorldKinds`、`WorldPlayerFilter` 及本批相關內部類均為 SAME。DIFF 限於 `VisibilityPc`、`VisibilitySwitch`。以下保留原始命令結果，完整 argv 與輸出分別在 `*-command.json`、`*.log`。
```text
openjdk version "17.0.17" 2025-10-21 LTS
OpenJDK Runtime Environment Microsoft-12574423 (build 17.0.17+10-LTS)
OpenJDK 64-Bit Server VM Microsoft-12574423 (build 17.0.17+10-LTS, mixed mode, sharing)
Compiled 9 compile-only sources -> target/legacy-stubs (never packaged)
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
SUMMARY SAME=85 SAME_EFFECTS=0 SAME_LAYOUT=0 DEVIATION=1 DIFF=2 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=3 ALLOW_METHODS=0
wrote D:\lineage開發\3-lito開發\scratch\wt\task-74\scratch\issue-74\latest\semantic.json
wrote D:\lineage開發\3-lito開發\scratch\wt\task-74\scratch\issue-74\latest\semantic.md
{
  "entries": 88,
  "missing": [],
  "extra": []
}
acceptancePass false
```
所有子程序退出碼如下。semantic-diff 以成功產生報表回傳 0，驗收由逐 entry 狀態及集合另行判定。

```json
{
  "jdk-version": 0,
  "stubs": 0,
  "tool-build": 0,
  "javac": 0,
  "jar": 0,
  "semantic": 0,
  "diagnostic-build": 0,
  "dump": 0,
  "switch-diagnostic": 0
}
```
## 核准清單

七份清單從同一主線快照讀取，再依本批 verify_entries 裁切。實際交集只有一筆 metadata 豁免與一筆 deviation，其餘五份交集為空。完整裁切結果如下。
```json
{
  "effects-approved.txt": [],
  "extra-methods-approved.txt": [],
  "metadata-exempt.txt": [
    "local/lito381/server/NativeDeVisibilitySwitch # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/108#issuecomment-6643057522"
  ],
  "access-widen-approved.txt": [],
  "layout-approved.txt": [],
  "deviations.txt": [
    "local/lito381/server/NativeDeWorldLog#debug(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/153"
  ],
  "exceptions-exempt.txt": []
}
```
## 剩餘差異與 A／B／C 分類

1. `NativeDeVisibilitySwitch` 兩欄位在兩側的原名、描述子、宣告順序及初始化內容一致，flags 分別為明寫 Java 的 `0x18`、正式合成欄位的 `0x1018`。#174 快照中的 `JarIndex.java` 第 68–70 行仍僅替帶有 ACC_SYNTHETIC 的 `$SwitchMap$` 欄位建立映射，造成右側名稱成為 `$SwitchMap$#0/#1`，左側保留原名。metadata 豁免目前僅涵蓋該類別的 metadata。
2. `NativeDeVisibilityPc.removeOutOfRangeKnownObjects` 的一個 GETSTATIC 引用受到同一映射影響。其餘正規化指令與例外保護表已以唯讀診斷核對。

通用 A 類的其餘差異已由正式 CLI 處理。以上差異超出 B 類四種堆疊／暫存類型，因此本批以 C 類送主控裁定，並依任務要求加 `needs:claude`。既有正式 caller 依賴這些欄位原名，Java 原始碼也無法直接指定 ACC_SYNTHETIC 或包含 `#` 的欄位識別字。

完整兩側 dump 的 unified diff 附於本報告後段，其中也保留工具已豁免的類別 metadata 與隱式建構子差異。正式 CLI 的兩筆 DIFF 與唯讀診斷分開保存。唯讀診斷只把右側顯示名稱還原，再比較完整指令及保護表，其結果如下。
```text
DIAGNOSTIC ONLY - semantic.json remains authoritative
LEFT fields in declaration order
$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$RepairKind [I flags=0x18
$SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase [I flags=0x18
RIGHT fields in declaration order
$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$RepairKind [I flags=0x1018
$SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase [I flags=0x1018
local/lito381/server/NativeDeVisibilitySwitch#<clinit>
instructionsEqualAfterRestoringRawFieldNames=true
normalizedTryCatchEqual=true
local/lito381/server/NativeDeVisibilityPc#removeOutOfRangeKnownObjects
instructionsEqualAfterRestoringRawFieldNames=true
normalizedTryCatchEqual=true
```
## 推測／未驗證什麼／給下一棒的提示

- **主控 Claude** 接手兩筆 C 類差異，裁定精確 switch 欄位正規化方案。可考慮對 metadata-exempt 的明寫 switch holder，以原名、描述子及欄位宣告順序建立雙側一致映射，並保留「交換兩個 map 的使用點」仍為 DIFF 的反例。這項方案目前屬待實作、待驗證。
- **工具執行器** 依裁定修正工具並完成正反例。合併後由 **A03 本機執行器** 更新固定工具提交，重跑全部 88 entry，確認所有 entry 為 SAME／SAME_EFFECTS／DEVIATION，且 DIFF、ONLY_LEFT、ONLY_RIGHT、missing、extra 均為 0。
- **A-land 執行器** 統一執行單元測試與回歸，並依 `deviations.md` 模擬選用欄位讀取失敗，驗證 debug 的訊息、參數及 NoSuchMethodError 修正。此次驗證限於 staged 編譯與靜態語意比較，執行期及真機驗收仍待後續階段。
- 分支相對固定主線的三點差異恰為 A03 47 份 staged Java 加本報告。編譯及診斷產物保存於 worktree 的 scratch／target。GitHub 操作使用既有認證，Git 共用 metadata 操作依本任務授權提升權限。

## 產出路徑

- `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/` 的 A03 47 Java。
- `docs/reports/issue-74.md` 為交付報告。
- `scratch/issue-74/latest.py` 為本次完整重驗腳本。
- `scratch/issue-74/latest/` 保存原始命令、log、核准清單快照與交集、semantic JSON／Markdown、176 份兩側 normalized 全文、完整差異、驗收結果及雜湊收據。
- `scratch/issue-74/latest/target/` 保存工具、診斷及 A03 classes／JAR。
- `scratch/issue-74/reverify/Dump.java` 與 `SwitchDiagnostic.java` 為唯讀診斷來源，原文附於後段。

## 輸入輸出 SHA-256

`inputs-sha256.json` 含 47 Java、工具快照、七份核准清單、stub、reference、lib、ASM 與驗證腳本逐檔雜湊。`outputs-sha256.json` 含本次產物與原始證據的逐檔雜湊。主要收據如下。

| 路徑 | SHA-256 |
|---|---|
| `lito伺服器/LiTo381-Server/reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `scratch/issue-74/latest/target/a03.jar` | `9f3555fed2db3e6d61db3213189f74f0b5d283e21997774982be2bd92ac3ea45` |
| `scratch/issue-74/latest/provenance.json` | `7e2c6408728f74b35b8f89b6081890676b60e6d712ccc955ce2d8b3b3c7b1bbc` |
| `scratch/issue-74/latest/inputs-sha256.json` | `614f2734fb2aba56e200cd32a5c24b0397f7369528071b355adf94076d01a0c1` |
| `scratch/issue-74/latest/outputs-sha256.json` | `95e492e57b4c4bb26a9ca52899aaa478f5fbdb616b53bf1fa42a0db3fee841ba` |
| `scratch/issue-74/latest/semantic.json` | `95fe26e6c158667c8ee0d00a309d73071b6106621a7312ec5f506d87760a7446` |
| `scratch/issue-74/latest/semantic.log` | `cffa4cbe3c9d71bbd272b7c2d9a2760b1a4e31c7d24330e14defedc260be0db9` |
| `scratch/issue-74/latest/acceptance.json` | `847d43079fc4c503c6865782f65afbeb6c30f0749611fdfd51632d7dfc1aabd3` |
| `scratch/issue-74/latest/remaining-diffs.md` | `4476ddccf639e05e1e2512bb8ee1aa0ef116dbc5ae1275d73025cbe81614fc49` |
| `scratch/issue-74/latest/switch-diagnostic.log` | `97235ebdb247c3baeb088542beee89fc3a454cfc1fb6dbd16c62755dd473af26` |
| `scratch/issue-74/latest.py` | `2534fcab1eedbf8011b2933c121a596bc422a8536bf7b54cc4d356a1d2090463` |

47 份 staged Java 的雜湊如下。

| 檔名 | SHA-256 |
|---|---|
| `NativeDeShopResources.java` | `ac972234c96157f29d7307c0766bc31482be88cb930b9cedcd1ce803940c7011` |
| `NativeDeShopTable.java` | `173cb0b3f3201a5fbde342c715e2ad076f83d3e0acda2dafaaa687cc795298a0` |
| `NativeDeSpriteCatalog.java` | `8f5c5a3291e28e5ac48c2ce83f01abac360fd5f9cb55a63bb0ce8a13fc8bf105` |
| `NativeDeTerrainPoint.java` | `5018e4bf4c98d601359d34fa64d9b7fef1053b111f338e325e5ae14915c1f7ec` |
| `NativeDeTerrainResources.java` | `4593df58395cd7f982945c5ecc2fa382ce15576c62b9c33952dfe839fb5c2c55` |
| `NativeDeTerrainScope.java` | `4ba6445e31b39c2ad835daced209abe79c3a1b3c0a6655abb87bd62a5eb2a3dd` |
| `NativeDeTerrainTable.java` | `dc406c5dfc6605f7c46bfd8ca59994c1627dd9079654405dd213bb7d1371476f` |
| `NativeDeTerrainText.java` | `fe2c557ea3c8c436a0e0006d4fe293a572b8eea55f6434a35a1d08ed8effc316` |
| `NativeDeTerrainV1.java` | `e2c2281817ee738b7943b959ceba23b09eff2ddbe6301ddd3b888ce6fd741eb0` |
| `NativeDeTerrainWorld.java` | `287e53f2c91208107111b3f92d98c2b9881d8ac5b9935d2b0190a73ae7b2d72d` |
| `NativeDeVisibilityAccess.java` | `3d8ca4a1ab1db6a611477cc5eb4e37dfb146c58d9040093cb4d10d63abaf6011` |
| `NativeDeVisibilityCheck.java` | `ef0748f836bfab2e2c97d910a97a21109ceae997db7422ef5a7991bdb69bb396` |
| `NativeDeVisibilityCoordinator.java` | `f2267c076437840726294f4d689f0636f4833a20fd02297e423873971279727b` |
| `NativeDeVisibilityHp.java` | `d006c884294c0dd1093278250cc0b44a317e787bb1f053324e63b221aff765c6` |
| `NativeDeVisibilityJobs.java` | `4e89fbd8461ea7b873c3c87dfbd1f5791ac54373aa845f51a3d8e14a6d555946` |
| `NativeDeVisibilityOutput.java` | `f4ddebfe8d2f2e16b76d336cb439ad9930be8c504df3dc2185e3479b5e977d6f` |
| `NativeDeVisibilityPacket.java` | `80e76c56b2a083c11d9e5e283c82a80855c5435e3958d267b62bb1bd9975e3d8` |
| `NativeDeVisibilityPc.java` | `11b67c4ba0185428aabb8e28aba5043bd7a2226e7265f9e9fdb1bd879e4ac212` |
| `NativeDeVisibilityRuntime.java` | `ff0f66f25877bad45cf948a121fa82183363a58b57901a3aff1a99773b133e4f` |
| `NativeDeVisibilityScope.java` | `a4860a8734ea1bc8d6bb9f5dfb54ea499fbe408aecee470338ddf27197c61405` |
| `NativeDeVisibilitySwitch.java` | `4b1b555a9f8719a9fc722c6eb046fffdacf11ebce92fffb638164ca1c14d130a` |
| `NativeDeVisibilityTimer.java` | `43c4c6924856a19812f261b9026dfd2c2e6573008c80042336452f0ebd46476c` |
| `NativeDeWorldContext.java` | `1e08258fd283a59d9f30ec16830f0a102f5f5860310e9dff31851e070795f070` |
| `NativeDeWorldControls.java` | `b5927165f252e62e0e484f64ffde25aab515852f21c32a01642ce64d8416e97f` |
| `NativeDeWorldCore.java` | `7c5144bb1f22f95d600fd894f9109977527c1d0cf484371e32d44e2e3415c689` |
| `NativeDeWorldKinds.java` | `8fea521f8a4cf62b85930671c4f54edf6af37c800bf6461ed6c84657bf22ad01` |
| `NativeDeWorldLocation.java` | `f9ba0782118fbc1f80411cd0ca2d45f81bdd1b5954266fa7f10e75c4e395ad02` |
| `NativeDeWorldLog.java` | `8aae89d6f7344bcdb4e606b681b5bd356efda8cf3ee6ea3868448624c3f7c055` |
| `NativeDeWorldPlayerFilter.java` | `ad0cccc18e2b9670e9a4eca44d7affc1870489c9d465e67c604e6a551321c9d1` |
| `NativeDeWorldPoint.java` | `366fc6a5b0fa25953893225b42b41f6dd5fbef9ad4d9692247d29c6cd92ea889` |
| `NativeDeWorldRegistryBow.java` | `9bf9f00880292a1643b5627d5ad9c33129839ac23ebbc0dd3e44933cef9c6686` |
| `NativeDeWorldRegistryCrown.java` | `63197dce293dd8981b4510d54e23e1cd1c84d7ea5ae27a12894975321a036eeb` |
| `NativeDeWorldRegistryDarkelf.java` | `1a5e861c35bce8e222611701ac21c917ccc764e55b58055a158ec7f514a5c0dc` |
| `NativeDeWorldRegistryDoll.java` | `7f546f46ef970faa3594a51ced87ffaf72c46af5528c2e81a62165db9fe1aab4` |
| `NativeDeWorldRegistryDragonKnight.java` | `e6433895ddca247771fe263594dcb789b1118665ba7b957db53e92a52dd02cff` |
| `NativeDeWorldRegistryEffect.java` | `e97d587c18554bd0b077cf77dbf4b79f697f8395f6f75f05b595d3adf321c438` |
| `NativeDeWorldRegistryElf.java` | `6a643f22d1551947a3fcfa161a2532876f267b0d931bf0bd5dd3e3af2fc5dd5e` |
| `NativeDeWorldRegistryIllusionist.java` | `62aefc56b99282cc15cd4a3cbf55ed5ac85be583bf603f0fba640ea0c5a044ef` |
| `NativeDeWorldRegistryItem.java` | `9f63cff5808432ab12af18d59a31617bbd0242e0ae66e7d1c676ca5fc8432225` |
| `NativeDeWorldRegistryKnight.java` | `d033eaa43d629efdb08b04f3eda689c62d1f179d01a5f790234f2b7781ef08d1` |
| `NativeDeWorldRegistryMob.java` | `cfe67e07368d98e91c76e172d546ca90f6e0dcc2a141115cd2ff35011706ea49` |
| `NativeDeWorldRegistryPet.java` | `eda1a1b4f391416bde99021dde304065f8fa39923c331d663b2083683c9c086e` |
| `NativeDeWorldRegistryQuest.java` | `d10e2724243178914929a0aa5667f0cb78ce3e357e7511140ea1a9f863d739f5` |
| `NativeDeWorldRegistrySummons.java` | `b7f43c9f966fce58a66ed84c1cb5520a71d0104e06e5a14d5f31d8aa456633c7` |
| `NativeDeWorldRegistryTrap.java` | `52ac8490a9f0f911b06ca34acb3c193332cae04418c64c2bcff9f63d99556491` |
| `NativeDeWorldRegistryWizard.java` | `0f0ef9bb06fed6369d6e09501a1112294349e41b41a437db636e2880e6b32e33` |
| `NativeDeWorldScope.java` | `00f942ab8eceadafc5b4ae8510066b45630ffa8d4013eaa73aeaf1f814c0c0e0` |

## 可重現指令

從本 worktree 根目錄執行。工具及清單由腳本的固定 `TOOL_REF` 讀取。ASM／asm-tree 使用既有 Maven cache，asm-analysis 使用前輪已校驗的本任務檔案。重跑時腳本先清除其自身 classes 輸出內的 class，再編譯並驗證集合與逐 entry bytes。

```powershell
& 'C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe' -B -X utf8 scratch/issue-74/latest.py
```

以下為實際執行腳本全文。

```python
from pathlib import Path
import difflib
import hashlib
import json
import os
import subprocess
import zipfile

ROOT = Path.cwd()
SERVER = ROOT / 'lito伺服器/LiTo381-Server'
WORK = ROOT / 'scratch/issue-74/latest'
JDK = Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
TOOL_REF = '18c80178f579b36aa227786e9b79638dc6af6db8'
TOOL_PREFIX = 'lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/'
APPROVAL_FLAGS = {
    'effects-approved.txt': '--effects-methods',
    'extra-methods-approved.txt': '--extra-methods',
    'metadata-exempt.txt': '--metadata-exempt',
    'access-widen-approved.txt': '--access-widen',
    'layout-approved.txt': '--layout-methods',
    'deviations.txt': '--deviations',
    'exceptions-exempt.txt': '--exceptions-exempt',
}
WORK.mkdir(parents=True, exist_ok=True)
COMMAND_RESULTS = {}


def git(*arguments):
    return subprocess.check_output(['git', *arguments], cwd=ROOT)


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def save_json(name, value):
    (WORK / name).write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def run(name, arguments, env=None):
    command = [str(argument) for argument in arguments]
    save_json(name + '-command.json', command)
    result = subprocess.run(command, cwd=ROOT, capture_output=True, env=env)
    (WORK / (name + '.log')).write_bytes(result.stdout + result.stderr)
    COMMAND_RESULTS[name] = result.returncode
    save_json('exit-codes.json', COMMAND_RESULTS)
    print(name, 'exit', result.returncode)
    print((result.stdout + result.stderr).decode('utf-8', errors='replace'))
    result.check_returncode()


def main():
    tool_ref = git('rev-parse', TOOL_REF).decode().strip()
    input_files = []
    tool_sources = []
    tracked = git('ls-tree', '-r', '--name-only', '-z', tool_ref, '--', TOOL_PREFIX).decode('utf-8').split('\0')
    for name in filter(None, tracked):
        destination = WORK / 'tool-source' / name.removeprefix(TOOL_PREFIX)
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(git('show', tool_ref + ':' + name))
        tool_sources.append(destination)
        input_files.append(destination)
    source_commit = git('log', '-1', '--format=%H', tool_ref, '--', TOOL_PREFIX).decode().strip()
    save_json('provenance.json', {
        'stagedCommit': git('rev-parse', 'HEAD').decode().strip(),
        'toolSnapshotCommit': tool_ref,
        'toolSourceCommit': source_commit,
        'toolSourceCount': len(tool_sources),
    })
    asm_files = [Path('D:/dev-cache/maven/repository/org/ow2/asm') / name / '9.7' / (name + '-9.7.jar') for name in ['asm', 'asm-tree']]
    asm_files.append(ROOT / 'scratch/issue-74/reverify/target/dependency/asm-analysis-9.7.jar')
    assert sha(asm_files[-1]) == '7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f'
    asm = ';'.join(map(str, asm_files))
    input_files.extend(asm_files)
    environment = dict(os.environ, JAVA_HOME=str(JDK.parent))
    run('jdk-version', [JDK / 'java.exe', '-version'])
    run('stubs', ['powershell', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', SERVER / 'tools/legacy-stubs/build.ps1'], env=environment)
    run('tool-build', [JDK / 'javac.exe', '--release', '17', '-encoding', 'UTF-8', '-cp', asm, '-d', WORK / 'target/tool', *tool_sources])
    batches_path = ROOT / 'docs/combat-merge/a-batches.json'
    batch = next(batch for batch in json.loads(batches_path.read_text(encoding='utf-8'))['batches'] if batch['id'] == 'A03')
    staged = SERVER / 'merge-inputs/formal-0.6.71/staged/local/lito381/server'
    sources = [staged / (name + '.java') for name in batch['classes']]
    assert len(sources) == 47 and all('DECOMPILED from base-0.6.71.jar' in path.read_text(encoding='utf-8') for path in sources)
    reference = SERVER / 'reference/base-0.6.71.jar'
    output = WORK / 'target/classes'
    assert output.resolve().is_relative_to(WORK.resolve())
    existing_classes = list(output.rglob('*.class'))
    assert all(path.resolve().is_relative_to(output.resolve()) for path in existing_classes)
    for path in existing_classes:
        path.unlink()
    output.mkdir(parents=True, exist_ok=True)
    run('javac', [JDK / 'javac.exe', '--release', '17', '-encoding', 'UTF-8', '-cp', f'{reference};{SERVER / "target/legacy-stubs"};{SERVER / "lib/*"}', '-d', output, *sources])
    jar = WORK / 'target/a03.jar'
    run('jar', [JDK / 'jar.exe', '--create', '--file', jar, '--date', '2026-11-05T00:00:00Z', '-C', output, '.'])
    with zipfile.ZipFile(jar) as archive:
        actual = {name for name in archive.namelist() if name.endswith('.class')}
        assert all(archive.read(name) == (output / name).read_bytes() for name in actual)
    expected = set(batch['verify_entries'])
    selection = {'entries': len(actual), 'missing': sorted(expected - actual), 'extra': sorted(actual - expected)}
    save_json('selection.json', selection)
    print(selection)
    assert actual == expected and len(actual) == 88
    (WORK / 'entries.txt').write_text('\n'.join(batch['verify_entries']) + '\n', encoding='utf-8')
    approvals = []
    intersections = {}
    for filename, flag in APPROVAL_FLAGS.items():
        snapshot = WORK / 'approvals-source' / filename
        snapshot.parent.mkdir(exist_ok=True)
        snapshot.write_bytes(git('show', tool_ref + ':docs/combat-merge/' + filename))
        input_files.append(snapshot)
        selected = []
        for line in snapshot.read_text(encoding='utf-8-sig').splitlines():
            entry = line.strip()
            if not entry or entry.startswith('#'):
                continue
            owner = entry.removeprefix('ctor-prologue-order ').split('#')[0].strip()
            if owner + '.class' in expected:
                selected.append(line)
        intersections[filename] = selected
        if selected:
            path = WORK / filename
            path.write_text('\n'.join(selected) + '\n', encoding='utf-8')
            approvals.extend([flag, path])
    save_json('approval-intersections.json', intersections)
    classpath = f'{WORK / "target/tool"};{asm}'
    run('semantic', [JDK / 'java.exe', '-Dfile.encoding=UTF-8', '-cp', classpath, 'local.lito381.semanticdiff.SemanticDiff', '--left', jar, '--right', reference, '--left-classpath', reference, '--classes', WORK / 'entries.txt', *approvals, '--json', WORK / 'semantic.json', '--md', WORK / 'semantic.md'])
    result = json.loads((WORK / 'semantic.json').read_text(encoding='utf-8'))
    accepted = all(entry['status'] in {'SAME', 'SAME_EFFECTS', 'DEVIATION'} for entry in result['classes'])
    save_json('acceptance.json', {'acceptancePass': accepted and actual == expected, 'counts': result['counts'], 'classSelection': result['classSelection']})
    print('acceptancePass', accepted)
    helpers = [ROOT / 'scratch/issue-74/reverify' / name for name in ['Dump.java', 'SwitchDiagnostic.java']]
    run('diagnostic-build', [JDK / 'javac.exe', '--release', '17', '-encoding', 'UTF-8', '-cp', classpath, '-d', WORK / 'target/diagnostics', *helpers])
    diagnostic_classpath = f'{WORK / "target/diagnostics"};{classpath}'
    run('dump', [JDK / 'java.exe', '-Dfile.encoding=UTF-8', '-cp', diagnostic_classpath, 'local.lito381.semanticdiff.Dump', jar, reference, WORK / 'normalized'])
    run('switch-diagnostic', [JDK / 'java.exe', '-Dfile.encoding=UTF-8', '-cp', diagnostic_classpath, 'local.lito381.semanticdiff.SwitchDiagnostic', jar, reference])
    diffs = []
    for entry in result['classes']:
        if entry['status'] != 'DIFF':
            continue
        name = entry['name'].split('/')[-1]
        left = (WORK / 'normalized' / (name + '.left.txt')).read_text(encoding='utf-8').splitlines(True)
        right = (WORK / 'normalized' / (name + '.right.txt')).read_text(encoding='utf-8').splitlines(True)
        diff = ''.join(difflib.unified_diff(left, right, fromfile=name + '.left', tofile=name + '.right'))
        diffs.append('### ' + name + '\n\n```diff\n' + diff + '```\n')
    (WORK / 'remaining-diffs.md').write_text('\n'.join(diffs), encoding='utf-8')
    input_files.extend([*sources, reference, batches_path, *helpers, Path(__file__).resolve(), SERVER / 'tools/legacy-stubs/build.ps1'])
    input_files.extend(sorted((SERVER / 'compile-stubs').rglob('*.java')))
    input_files.extend(sorted((SERVER / 'lib').glob('*.jar')))
    save_json('inputs-sha256.json', [{'path': str(path.relative_to(ROOT)) if path.is_relative_to(ROOT) else str(path), 'sha256': sha(path)} for path in input_files])
    save_json('outputs-sha256.json', [{'path': path.relative_to(WORK).as_posix(), 'sha256': sha(path)} for path in sorted(WORK.rglob('*')) if path.is_file() and path.name != 'outputs-sha256.json'])
    print('jar SHA256', sha(jar))


if __name__ == '__main__':
    main()
```

## 88 entry 原始結果

| Entry（前綴 local/lito381/server/） | 結果 |
|---|---|
| `NativeDeShopResources` | SAME |
| `NativeDeShopResources$Snapshot` | SAME |
| `NativeDeShopTable` | SAME |
| `NativeDeSpriteCatalog` | SAME |
| `NativeDeSpriteCatalog$Catalog` | SAME |
| `NativeDeSpriteCatalog$Mutable` | SAME |
| `NativeDeSpriteCatalog$Reporter` | SAME |
| `NativeDeSpriteCatalog$Sprite` | SAME |
| `NativeDeTerrainPoint` | SAME |
| `NativeDeTerrainResources` | SAME |
| `NativeDeTerrainResources$1` | SAME |
| `NativeDeTerrainResources$Definition` | SAME |
| `NativeDeTerrainResources$Snapshot` | SAME |
| `NativeDeTerrainScope` | SAME |
| `NativeDeTerrainScope$Asset` | SAME |
| `NativeDeTerrainScope$Context` | SAME |
| `NativeDeTerrainScope$Database` | SAME |
| `NativeDeTerrainScope$Logs` | SAME |
| `NativeDeTerrainScope$Sql` | SAME |
| `NativeDeTerrainTable` | SAME |
| `NativeDeTerrainText` | SAME |
| `NativeDeTerrainV1` | SAME |
| `NativeDeTerrainWorld` | SAME |
| `NativeDeTerrainWorld$Domain` | SAME |
| `NativeDeTerrainWorld$Key` | SAME |
| `NativeDeTerrainWorld$Owner` | SAME |
| `NativeDeVisibilityAccess` | SAME |
| `NativeDeVisibilityCheck` | SAME |
| `NativeDeVisibilityCoordinator` | SAME |
| `NativeDeVisibilityHp` | SAME |
| `NativeDeVisibilityJobs` | SAME |
| `NativeDeVisibilityJobs$Credit` | SAME |
| `NativeDeVisibilityJobs$Dispatch` | SAME |
| `NativeDeVisibilityJobs$Ticket` | SAME |
| `NativeDeVisibilityOutput` | SAME |
| `NativeDeVisibilityPacket` | SAME |
| `NativeDeVisibilityPc` | DIFF |
| `NativeDeVisibilityRuntime` | SAME |
| `NativeDeVisibilityRuntime$Owner` | SAME |
| `NativeDeVisibilityScope` | SAME |
| `NativeDeVisibilityScope$Logs` | SAME |
| `NativeDeVisibilitySwitch` | DIFF |
| `NativeDeVisibilityTimer` | SAME |
| `NativeDeWorldContext` | SAME |
| `NativeDeWorldContext$1` | SAME |
| `NativeDeWorldContext$2` | SAME |
| `NativeDeWorldContext$Entry` | SAME |
| `NativeDeWorldControls` | SAME |
| `NativeDeWorldCore` | SAME |
| `NativeDeWorldCore$1` | SAME |
| `NativeDeWorldCore$SpatialIndexAuditSnapshot` | SAME |
| `NativeDeWorldCore$SpatialReconcileResult` | SAME |
| `NativeDeWorldCore$VisibleBinding` | SAME |
| `NativeDeWorldKinds` | SAME |
| `NativeDeWorldKinds$Bow` | SAME |
| `NativeDeWorldKinds$Doll` | SAME |
| `NativeDeWorldKinds$Dwarf` | SAME |
| `NativeDeWorldKinds$Effect` | SAME |
| `NativeDeWorldKinds$Item` | SAME |
| `NativeDeWorldKinds$Item$EquipmentTimerLease` | SAME |
| `NativeDeWorldKinds$Item$LastStatus` | SAME |
| `NativeDeWorldKinds$Merchant` | SAME |
| `NativeDeWorldKinds$Npc` | SAME |
| `NativeDeWorldKinds$Pet` | SAME |
| `NativeDeWorldKinds$Trap` | SAME |
| `NativeDeWorldLocation` | SAME |
| `NativeDeWorldLog` | DEVIATION |
| `NativeDeWorldPlayerFilter` | SAME |
| `NativeDeWorldPlayerFilter$1` | SAME |
| `NativeDeWorldPoint` | SAME |
| `NativeDeWorldRegistryBow` | SAME |
| `NativeDeWorldRegistryCrown` | SAME |
| `NativeDeWorldRegistryDarkelf` | SAME |
| `NativeDeWorldRegistryDoll` | SAME |
| `NativeDeWorldRegistryDragonKnight` | SAME |
| `NativeDeWorldRegistryEffect` | SAME |
| `NativeDeWorldRegistryElf` | SAME |
| `NativeDeWorldRegistryIllusionist` | SAME |
| `NativeDeWorldRegistryItem` | SAME |
| `NativeDeWorldRegistryKnight` | SAME |
| `NativeDeWorldRegistryMob` | SAME |
| `NativeDeWorldRegistryPet` | SAME |
| `NativeDeWorldRegistryQuest` | SAME |
| `NativeDeWorldRegistrySummons` | SAME |
| `NativeDeWorldRegistryTrap` | SAME |
| `NativeDeWorldRegistryTrap$TrapCell` | SAME |
| `NativeDeWorldRegistryWizard` | SAME |
| `NativeDeWorldScope` | SAME |

## 兩筆剩餘 entry 的完整正規化差異

### NativeDeVisibilityPc

```diff
--- NativeDeVisibilityPc.left
+++ NativeDeVisibilityPc.right
@@ -11,16 +11,6 @@
 annotations |||
 innerClasses
 recordComponents
-METHOD <init>()V
-access 0x1
-signature
-exceptions []
-annotations ||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault
-   ALOAD 0
-   INVOKESPECIAL java/lang/Object.<init>()V
-   RETURN
-tryCatch []
 METHOD isGmHpBarTarget(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
 access 0x9
 signature
@@ -155,7 +145,7 @@
    ALOAD 5
    INVOKESPECIAL local/lito381/server/NativeDeDeletionPorts$RemoveObject.<init>(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
    INVOKESTATIC local/lito381/server/NativeDeVisibilityAccess.L1PcInstance$sendPackets(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;Llocal/lito381/server/NativeDeDeletionPorts$Packet;)V
-   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$RepairKind:[I
+   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#0:[I
    ALOAD 10
    INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$RepairKind.ordinal()I
    IALOAD
```

### NativeDeVisibilitySwitch

```diff
--- NativeDeVisibilitySwitch.left
+++ NativeDeVisibilitySwitch.right
@@ -1,22 +1,22 @@
-access 32
+access 4128
 superName java/lang/Object
 interfaces []
 signature
-outerClass
+outerClass local/lito381/server/NativeDeDeletionPorts$Viewer
 outerMethod
 outerMethodDesc
-nestHostClass
+nestHostClass local/lito381/server/NativeDeDeletionPorts$Viewer
 nestMembers
 permittedSubclasses
 annotations |||
-innerClasses
+innerClasses local/lito381/server/NativeDeVisibilitySwitch|||0x1008
 recordComponents
-FIELD $SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase:[I
+FIELD $SwitchMap$#0:[I
 access 0x18
 signature
 value
 annotations |||
-FIELD $SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$RepairKind:[I
+FIELD $SwitchMap$#1:[I
 access 0x18
 signature
 value
@@ -30,50 +30,50 @@
    INVOKESTATIC local/lito381/server/NativeDeathLifecycleGate$Phase.values()[Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
    ARRAYLENGTH
    NEWARRAY 10
-   PUTSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase:[I
-   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase:[I
+   PUTSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#1:[I
+   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#1:[I
    GETSTATIC local/lito381/server/NativeDeathLifecycleGate$Phase.ACTIVE:Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
    INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate$Phase.ordinal()I
    CONST Integer:1
    IASTORE
    GOTO L11
    ASTORE 0
-   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase:[I
+   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#1:[I
    GETSTATIC local/lito381/server/NativeDeathLifecycleGate$Phase.RESURRECTING:Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
    INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate$Phase.ordinal()I
    CONST Integer:2
    IASTORE
    GOTO L18
    ASTORE 1
-   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase:[I
+   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#1:[I
    GETSTATIC local/lito381/server/NativeDeathLifecycleGate$Phase.DEAD_PENDING:Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
    INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate$Phase.ordinal()I
    CONST Integer:3
    IASTORE
    GOTO L25
    ASTORE 2
-   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase:[I
+   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#1:[I
    GETSTATIC local/lito381/server/NativeDeathLifecycleGate$Phase.DEAD_SETTLED:Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
    INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate$Phase.ordinal()I
    CONST Integer:4
    IASTORE
    GOTO L32
    ASTORE 3
-   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase:[I
+   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#1:[I
    GETSTATIC local/lito381/server/NativeDeathLifecycleGate$Phase.DEATH_DROPPED:Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
    INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate$Phase.ordinal()I
    CONST Integer:5
    IASTORE
    GOTO L39
    ASTORE 4
-   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase:[I
+   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#1:[I
    GETSTATIC local/lito381/server/NativeDeathLifecycleGate$Phase.FINALIZING:Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
    INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate$Phase.ordinal()I
    CONST Integer:6
    IASTORE
    GOTO L46
    ASTORE 5
-   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase:[I
+   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#1:[I
    GETSTATIC local/lito381/server/NativeDeathLifecycleGate$Phase.TERMINATED:Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
    INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate$Phase.ordinal()I
    CONST Integer:7
@@ -83,22 +83,22 @@
    INVOKESTATIC local/lito381/server/NativeDeDeletionVisibility$RepairKind.values()[Llocal/lito381/server/NativeDeDeletionVisibility$RepairKind;
    ARRAYLENGTH
    NEWARRAY 10
-   PUTSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$RepairKind:[I
-   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$RepairKind:[I
+   PUTSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#0:[I
+   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#0:[I
    GETSTATIC local/lito381/server/NativeDeDeletionVisibility$RepairKind.NORMAL_RACE:Llocal/lito381/server/NativeDeDeletionVisibility$RepairKind;
    INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$RepairKind.ordinal()I
    CONST Integer:1
    IASTORE
    GOTO L64
    ASTORE 7
-   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$RepairKind:[I
+   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#0:[I
    GETSTATIC local/lito381/server/NativeDeDeletionVisibility$RepairKind.STALL:Llocal/lito381/server/NativeDeDeletionVisibility$RepairKind;
    INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$RepairKind.ordinal()I
    CONST Integer:2
    IASTORE
    GOTO L71
    ASTORE 8
-   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$RepairKind:[I
+   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#0:[I
    GETSTATIC local/lito381/server/NativeDeDeletionVisibility$RepairKind.LEAK:Llocal/lito381/server/NativeDeDeletionVisibility$RepairKind;
    INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$RepairKind.ordinal()I
    CONST Integer:3
@@ -107,13 +107,3 @@
    ASTORE 9
    RETURN
 tryCatch [4:[10:java/lang/NoSuchFieldError  ], 5:[10:java/lang/NoSuchFieldError  ], 6:[10:java/lang/NoSuchFieldError  ], 8:[10:java/lang/NoSuchFieldError  ], 11:[17:java/lang/NoSuchFieldError  ], 12:[17:java/lang/NoSuchFieldError  ], 13:[17:java/lang/NoSuchFieldError  ], 15:[17:java/lang/NoSuchFieldError  ], 18:[24:java/lang/NoSuchFieldError  ], 19:[24:java/lang/NoSuchFieldError  ], 20:[24:java/lang/NoSuchFieldError  ], 22:[24:java/lang/NoSuchFieldError  ], 25:[31:java/lang/NoSuchFieldError  ], 26:[31:java/lang/NoSuchFieldError  ], 27:[31:java/lang/NoSuchFieldError  ], 29:[31:java/lang/NoSuchFieldError  ], 32:[38:java/lang/NoSuchFieldError  ], 33:[38:java/lang/NoSuchFieldError  ], 34:[38:java/lang/NoSuchFieldError  ], 36:[38:java/lang/NoSuchFieldError  ], 39:[45:java/lang/NoSuchFieldError  ], 40:[45:java/lang/NoSuchFieldError  ], 41:[45:java/lang/NoSuchFieldError  ], 43:[45:java/lang/NoSuchFieldError  ], 46:[52:java/lang/NoSuchFieldError  ], 47:[52:java/lang/NoSuchFieldError  ], 48:[52:java/lang/NoSuchFieldError  ], 50:[52:java/lang/NoSuchFieldError  ], 57:[63:java/lang/NoSuchFieldError  ], 58:[63:java/lang/NoSuchFieldError  ], 59:[63:java/lang/NoSuchFieldError  ], 61:[63:java/lang/NoSuchFieldError  ], 64:[70:java/lang/NoSuchFieldError  ], 65:[70:java/lang/NoSuchFieldError  ], 66:[70:java/lang/NoSuchFieldError  ], 68:[70:java/lang/NoSuchFieldError  ], 71:[77:java/lang/NoSuchFieldError  ], 72:[77:java/lang/NoSuchFieldError  ], 73:[77:java/lang/NoSuchFieldError  ], 75:[77:java/lang/NoSuchFieldError  ]]
-METHOD <init>()V
-access 0x0
-signature
-exceptions []
-annotations ||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault
-   ALOAD 0
-   INVOKESPECIAL java/lang/Object.<init>()V
-   RETURN
-tryCatch []
```

## 唯讀診斷來源

依序存為 `scratch/issue-74/reverify/Dump.java`、`SwitchDiagnostic.java`。完整編譯及執行命令已包含於重驗腳本，診斷 class 使用獨立的 `target/diagnostics`。

```java
package local.lito381.semanticdiff;
import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import org.objectweb.asm.tree.*;
public class Dump {
 static Object invoke(String name,Object...args) throws Exception {
  for(Method m:ClassComparer.class.getDeclaredMethods()) if(m.getName().equals(name)&&m.getParameterCount()==args.length){m.setAccessible(true);return m.invoke(null,args);}throw new Exception(name);
 }
 static void line(StringBuilder b,String key,Object v){b.append((key+" "+Objects.toString(v, "")).stripTrailing()).append('\n');}
 static Object sorted(Object o){if(o instanceof List){var list=new ArrayList<>((List<String>)o);Collections.sort(list);return list;}return o;}
 static String dump(ClassNode c,JarIndex j)throws Exception {
  StringBuilder b=new StringBuilder();
  for(String k:List.of("access","superName","interfaces","signature","outerClass","outerMethod","outerMethodDesc","nestHostClass","nestMembers","permittedSubclasses"))line(b,k,sorted(ClassNode.class.getField(k).get(c)));
  line(b,"annotations",invoke("annotations",c));
  line(b,"innerClasses",invoke("innerClasses",c,false));line(b,"recordComponents",invoke("records",c,j));
  var fields=new TreeMap<String,FieldNode>();for(var f:c.fields)fields.put(j.remapField(c.name,f.name)+":"+f.desc,f);
  for(var e:fields.entrySet()){var f=e.getValue();line(b,"FIELD",e.getKey());line(b,"access",invoke("memberAccess",f.access));line(b,"signature",f.signature);line(b,"value",invoke("value",f.value));line(b,"annotations",invoke("fieldAnns",f));}
  var methods=new TreeMap<String,MethodNode>();for(var m:c.methods)methods.put(j.remapMethod(c.name,m.name,m.desc)+m.desc,m);
  for(var e:methods.entrySet()){var m=e.getValue();line(b,"METHOD",e.getKey());line(b,"access",invoke("memberAccess",m.access));line(b,"signature",m.signature);line(b,"exceptions",sorted(m.exceptions));line(b,"annotations",invoke("methodAnns",m,invoke("labelIds",m),j));line(b,"annotationDefault",invoke("defaultValue",m.annotationDefault,j));
   for(String insn:ClassComparer.instructions(m,j))line(b,"  ",insn);
   line(b,"tryCatch",invoke("tryCatch",m,j));}
  return b.toString();
 }
 public static void main(String[]args)throws Exception{
  JarIndex l=JarIndex.load(Path.of(args[0])),r=JarIndex.load(Path.of(args[1]));l.addClasspath(Path.of(args[1]));Path out=Path.of(args[2]);Files.createDirectories(out);
  for(String n:l.classes.keySet()){
   ClassNode a=l.readTree(n),b=r.readTree(n);if(b==null)continue;String simple=n.substring(n.lastIndexOf('/')+1);
   Files.writeString(out.resolve(simple+".left.txt"),dump(a,l));Files.writeString(out.resolve(simple+".right.txt"),dump(b,r));
   var bm=new TreeMap<String,MethodNode>();for(var m:b.methods)bm.put(r.remapMethod(n,m.name,m.desc)+m.desc,m);
   for(var m:a.methods){String k=l.remapMethod(n,m.name,m.desc)+m.desc;var rm=bm.remove(k);var d=rm==null?"only left":ClassComparer.compareMethod(k,m,rm,l,r,false);if(d!=null)System.out.println(simple+"#"+k+" "+(d instanceof ClassComparer.FirstDiff ? ((ClassComparer.FirstDiff)d).oneLine():d));}
   for(String k:bm.keySet())System.out.println(simple+"#"+k+" only right");
  }
 }
}

```

```java
package local.lito381.semanticdiff;

import java.nio.file.Path;
import java.util.List;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/** Read-only diagnosis. Does not change the acceptance result or write class files. */
public class SwitchDiagnostic {
    private static final String OWNER = "local/lito381/server/NativeDeVisibilitySwitch";
    private static final String REPAIR = "$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$RepairKind";
    private static final String PHASE = "$SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase";

    private static List<String> restoreRawNames(List<String> instructions) {
        return instructions.stream().map(line -> line
                .replace(OWNER + ".$SwitchMap$#0:", OWNER + "." + REPAIR + ":")
                .replace(OWNER + ".$SwitchMap$#1:", OWNER + "." + PHASE + ":")).toList();
    }

    private static MethodNode method(ClassNode node, String name) {
        return node.methods.stream().filter(method -> method.name.equals(name)).findFirst().orElseThrow();
    }

    public static void main(String[] args) throws Exception {
        JarIndex left = JarIndex.load(Path.of(args[0]));
        JarIndex right = JarIndex.load(Path.of(args[1]));
        left.addClasspath(Path.of(args[1]));
        System.out.println("DIAGNOSTIC ONLY - semantic.json remains authoritative");
        for (JarIndex jar : List.of(left, right)) {
            System.out.println(jar == left ? "LEFT fields in declaration order" : "RIGHT fields in declaration order");
            for (var field : jar.readTree(OWNER).fields) {
                System.out.printf("%s %s flags=0x%x%n", field.name, field.desc, field.access);
            }
        }
        for (String[] target : List.of(
                new String[]{OWNER, "<clinit>"},
                new String[]{"local/lito381/server/NativeDeVisibilityPc", "removeOutOfRangeKnownObjects"})) {
            MethodNode lm = method(left.readTree(target[0]), target[1]);
            MethodNode rm = method(right.readTree(target[0]), target[1]);
            boolean instructions = ClassComparer.instructions(lm, left).equals(
                    restoreRawNames(ClassComparer.instructions(rm, right)));
            boolean handlers = ClassComparer.tryCatch(lm, left).equals(ClassComparer.tryCatch(rm, right));
            System.out.println(target[0] + "#" + target[1]);
            System.out.println("instructionsEqualAfterRestoringRawFieldNames=" + instructions);
            System.out.println("normalizedTryCatchEqual=" + handlers);
            if (!instructions || !handlers) throw new AssertionError("Additional difference found");
        }
    }
}

```

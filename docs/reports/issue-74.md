# Issue #74 — A03 正式 0.6.71 暫存重驗

## 結論

本輪使用已合併的 #108、#135、#153、#166 重驗全部 88 entry，結果為 **85 SAME、1 DEVIATION、2 DIFF**，missing／extra 均為空。仍待主控裁定兩筆 switch 欄位正規化差異，最終語意驗收尚未通過。交付分支 `task/74`，PR #100。

初次新版重驗為 82 SAME、1 DEVIATION、5 DIFF。本輪修正五份 staged Java，其中 `NativeDeVisibilityCoordinator`、`NativeDeWorldContext`、`NativeDeWorldCore` 已完整 SAME，`NativeDeVisibilityPc` 僅剩對 switch 欄位的引用差異。`NativeDeWorldKinds` 與其內部類、`NativeDeWorldPlayerFilter` 與 `$1` 均 SAME。

## 做了什麼

- 沿用 `task/74`，合入已合併依賴的 `origin/main`。本輪工作前 head 為 `ee6ab35c5eb56334dd0e155ab24a8a3d89d3126f`，依賴基底為 `450178d59b0157fddf2b9456b7c03d20207993fc`。合併帶入主線既有變更，分支相對主線的任務範圍仍精確為 47 Java 與本報告。
- `NativeDeVisibilityCoordinator.tick` 恢復正式版 check 成功後才更新玩家的條件區塊，消除 continue 造成的例外表分段差異。
- `NativeDeVisibilityPc` 的已知物件快照採用 `List` 呼叫型別，恢復 range 條件分支及 GM HP bar 的明確布林返回。
- `NativeDeWorldContext.attach` 將欄位發佈與 showId 寫入放回物件 monitor 內，返回時先離開 monitor，再由 finally 釋放 admission read lock。前輪反編譯內容曾把新 entry 的發佈放到 monitor 外，並在既有 entry 返回時提前 unlock。本輪已依正式版修正，完整 SAME。
- `NativeDeWorldCore` 修正 Map 查詢的 key 裝箱型別，將 `short actualMapId` 改為 `int`，使 `_visibleObjects.get`／`_spatialIndex.get` 使用正式版的 `Integer.valueOf`。同時對齊 log 裝箱、`NativeDeWorldPoint` 呼叫型別、switch 出口、查詢 guard、pattern variable、lambda 提早返回、陣列自增與區域暫存，完整 SAME。
- `NativeDeVisibilitySwitch` 的兩個欄位宣告順序對齊正式 class 欄位表，依序為 RepairKind、Phase。初始化仍先 Phase、後 RepairKind。這為後續依欄位順序比對提供一致輸入。
- 保留唯一的 `NativeDeWorldLog.debug(String,Object,Object)`，其 `emit("debug", message, new Object[]{a,b})` 實作經來源檢查，CLI 以既有 `--deviations` 列為 DEVIATION。
- 本輪沿用前輪 CFR 0.152／Vineflower 1.11.2 的正式版還原結果，結合新版完整正規化輸出修正來源。47 檔的 DECOMPILED 標頭均保留。

## 已驗證

環境為 Windows、Microsoft OpenJDK 17.0.17+10-LTS、Python 3.12.10、ASM 9.7。新版工具從本 worktree 的 Java 來源重新編譯，工具最後變更 commit 為 `c98b4e8e552742ffad3f624dfee646dcb1f4218d`。四項依賴的主線合併提交如下。

| 依賴 | PR | 合併提交 |
|---|---|---|
| #108 | #147 | `a5f9f66` |
| #135 | #152 | `4a8d7e5` |
| #153 | #164 | `bf50dd7` |
| #166 | #170 | `5089b6b` |

原始摘要如下。semantic-diff 產生報表時即回傳 0，驗收另以每個 entry 的狀態判定，`acceptance.json` 的 `acceptancePass` 為 false。

```text
Compiled 9 compile-only sources -> target/legacy-stubs (never packaged)
tool-build exit 0
javac exit 0
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
jar exit 0
{'entries': 88, 'missing': [], 'extra': []}
semantic exit 0
SUMMARY SAME=85 SAME_EFFECTS=0 SAME_LAYOUT=0 DEVIATION=1 DIFF=2 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=3 ALLOW_METHODS=0
classSelection.missingLeft=[]
classSelection.extraLeft=[]
acceptancePass False
```

只將本批 47 Java 直接 javac 編譯，產物恰為清單內 88 class。驗收 JAR 全部來自該輸出目錄，沒有 class bytes 覆蓋、編譯後處理或 com/lineage stub 打包。打包固定 `--date 2026-11-05T00:00:00Z` 供重現。

裁切六份核准清單後，本批實際使用一條 metadata 豁免及一條 deviation。effects／extra-methods／access-widen／layout 與本批的交集為空，因此未傳空檔。左側以正式 reference JAR 作明確 classpath，供 #166 的父類欄位解析。核准內容如下。

```text
local/lito381/server/NativeDeVisibilitySwitch # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/108#issuecomment-6643057522
```

```text
local/lito381/server/NativeDeWorldLog#debug(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/153
```

## 剩餘差異與 A／B／C 分類

1. `NativeDeVisibilitySwitch` 的兩個欄位在兩側具有相同原名、描述子、宣告順序與初始化內容。右側 flags 為 `0x1018`，Java 明寫欄位為 `0x18`。`JarIndex.load` 目前只對具有 ACC_SYNTHETIC 的 `$SwitchMap$` 建立改名表，右側因此改為 `$SwitchMap$#0/#1`，左側保留原名，導致欄位及 clinit DIFF。既有 metadata 豁免處理類別 metadata，未涵蓋此欄位名稱映射。
2. `NativeDeVisibilityPc.removeOutOfRangeKnownObjects` 只剩一個 GETSTATIC 引用相同 switch 欄位時受到上述不對稱改名影響。其餘完整方法指令與 try-catch 表已核對一致。

A 類的其餘 metadata、lambda、local webs、stack/local 與 try 邊界差異已由新版工具驗收。B 類本輪沒有新增條目，剩餘欄位名稱映射不在可自行追加 effects 的四種範圍。以上兩筆按 C 類未涵蓋情形送主控，需加 `needs:claude`。程式碼欄位原名須保留供既有 reference caller 使用，Java 原始碼也無法直接指定 ACC_SYNTHETIC 或含 `#` 的欄位識別字。

獨立唯讀診斷將右側正規化顯示名稱還原為正式原名後，對兩個完整方法逐指令與 try-catch 表比較。此診斷沒有寫入 class，也沒有改變正式 CLI 的 DIFF 結果。原始輸出如下。

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

## 推測／待驗證／給下一棒的提示

- **主控 Claude** 接手兩筆 C 類結果，裁定精確 switch 欄位正規化方案。建議工具工作範圍檢查 metadata-exempt 的明寫 switch holder，以實際欄位原名與宣告順序建立雙側一致映射，同時保留交換兩個 map 使用點仍為 DIFF 的反例。這項建議尚未實作或驗證。
- **工具修正合併後的 A03 執行器** 用本報告指令重跑全部 88 entry，確認 87 SAME 加 1 DEVIATION 或其餘核准通過狀態，並確認缺漏、多出及 DIFF 皆為 0。這次提交須保留待驗收狀態。
- **A-land** 再統一跑單元測試及回歸，特別依 `deviations.md` 模擬選用欄位讀取失敗，確認 default debug 的訊息／參數與 NoSuchMethodError 修正。本批單元／回歸不適用，真機執行尚待 A-land 後安排。
- 本輪程式碼及報告修改限 staged 與本報告。src/main、pom.xml、docs/REPORT.md 的主線差異由依賴合併帶入，本任務相對 origin/main 對這些路徑的差異為空。未啟動遊戲／伺服器／登入器，未連 DB，未存取機密設定、封存與客戶端，未改正式部署或硬連結。
- Git 共用 metadata 的 fetch／merge／commit／push 依任務使用權限提升。GitHub CLI 在沙箱內回傳 401，提升後可使用既有認證。ASM analysis 下載在提升模式成功，SHA-256 已與 repo 的 `build-support.sh` 常數核對。未寫入原始專案工作檔。

## 產出路徑

- `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/` 的 A03 47 Java。本輪另修正其中五份。
- `docs/reports/issue-74.md` 為本輪提交報告。
- `scratch/issue-74/reverify.py` 為重驗腳本。
- `scratch/issue-74/reverify/` 保存 `*-command.json`、原始 log、semantic JSON／Markdown、88 對 normalized 全文、差異、核准交集、雜湊 manifest 與唯讀診斷。
- `scratch/issue-74/reverify/target/a03.jar`、`target/classes/`、`target/tool/`、`target/dependency/` 為本輪建置輸出，均不入 Git。
- `scratch/issue-74/reverify/initial/` 保存本輪第一次新版重驗的 82／1／5 結果及完整差異。前輪紀錄仍可由 git 追溯。

## 輸入輸出 SHA-256

`inputs-sha256.json` 列出 47 Java、工具 Java、核准清單、stub 來源、reference／lib 與 ASM 依賴的逐檔雜湊。以下是主要收據。

| 路徑 | SHA-256 |
|---|---|
| `lito伺服器/LiTo381-Server/reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `scratch/issue-74/reverify/target/a03.jar` | `9f3555fed2db3e6d61db3213189f74f0b5d283e21997774982be2bd92ac3ea45` |
| `scratch/issue-74/reverify/inputs-sha256.json` | `27861ce7c3dd7d34a176e0850d25e621e7fc69743561e5b967fb9ef388a443de` |
| `scratch/issue-74/reverify/entries.txt` | `6e5f2f766d5158a5ebc2dc028c5c0e47d2e9cad2dcebe23bc8b9ca30a0f79831` |
| `scratch/issue-74/reverify/metadata-exempt.txt` | `846f881adfea193300b358c53066738cef25e4f568adb7db392294a1cf6ff5b6` |
| `scratch/issue-74/reverify/deviations.txt` | `58efe0b86b4e6fad0eb3e213411fd53b07e45236c15dbc32d830ee0a448d45f2` |
| `scratch/issue-74/reverify/semantic.json` | `e2622219b2cea2fde5fb710470fb98a890c91972fab3de0f5499a5eb39cc1357` |
| `scratch/issue-74/reverify/semantic.log` | `d19ba2d7e727f5a0ced4da2c08f53e09670e516d2214a562c6c716d340387847` |
| `scratch/issue-74/reverify/javac.log` | `373c917b895fa8f625556c0b4b359c0d4fcb2a78b8bd66390f8a0a0f001bfe3a` |
| `scratch/issue-74/reverify/remaining-diffs.md` | `d534179af7ac16ad1aa13bf459070e62a0847a2ddd17e9f7b97d1a01179ed4c7` |
| `scratch/issue-74/reverify/switch-diagnostic.log` | `97235ebdb247c3baeb088542beee89fc3a454cfc1fb6dbd16c62755dd473af26` |
| `scratch/issue-74/reverify/acceptance.json` | `f0cec68f4963aece923d9d181f7e825dd798de9f593c8f3636caafb73040e23f` |
| `scratch/issue-74/reverify.py` | `fa0db0bd585515f2eacc1e14d527d2ef7e5c88f90973e912107a4b4bed3259e6` |
| `scratch/issue-74/reverify/Dump.java` | `757ab5f33a54d9c702c6ce08189473d34b23b292d920a99b54e5d5654718dcb2` |
| `scratch/issue-74/reverify/SwitchDiagnostic.java` | `461804acbd603aa11c0923b088269f02425df9d8cb44617ab08ece166940d4b2` |

### 47 份 staged 輸入

| Java | SHA-256 |
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

從 worktree 根目錄執行。JDK 與 Python 路徑為本機實測位置。ASM／asm-tree 使用既有 Maven cache，asm-analysis 放此任務的 target/dependency。

```powershell
$env:JAVA_HOME = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot'
powershell -NoProfile -ExecutionPolicy Bypass -File lito伺服器/LiTo381-Server/tools/legacy-stubs/build.ps1
New-Item -ItemType Directory -Force scratch/issue-74/reverify/target/dependency | Out-Null
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
Invoke-WebRequest -UseBasicParsing 'https://repo.maven.apache.org/maven2/org/ow2/asm/asm-analysis/9.7/asm-analysis-9.7.jar' -OutFile scratch/issue-74/reverify/target/dependency/asm-analysis-9.7.jar
if ((Get-FileHash scratch/issue-74/reverify/target/dependency/asm-analysis-9.7.jar).Hash -ne '7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f') { throw 'ASM hash mismatch' }
& 'C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe' -B -X utf8 scratch/issue-74/reverify.py
```

下列為實際重驗腳本全文。每個 subprocess 的完整 argv 與 stdout／stderr 分別保存為 `*-command.json` 及 `*.log`。

```python
from pathlib import Path
import hashlib
import json
import subprocess
import zipfile

ROOT = Path.cwd()
SERVER = ROOT / 'lito伺服器/LiTo381-Server'
WORK = ROOT / 'scratch/issue-74/reverify'
JDK = Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
BATCH = next(b for b in json.loads((ROOT / 'docs/combat-merge/a-batches.json').read_text(encoding='utf-8'))['batches'] if b['id'] == 'A03')
WORK.mkdir(parents=True, exist_ok=True)
ASM = ';'.join(str(p) for p in [
    Path('D:/dev-cache/maven/repository/org/ow2/asm') / name / '9.7' / f'{name}-9.7.jar'
    for name in ['asm', 'asm-tree']
] + [WORK / 'target/dependency/asm-analysis-9.7.jar'])

def run(name, arguments):
    command = [str(arg) for arg in arguments]
    (WORK / f'{name}-command.json').write_text(json.dumps(command, ensure_ascii=False, indent=2), encoding='utf-8')
    result = subprocess.run(command, capture_output=True)
    (WORK / f'{name}.log').write_bytes(result.stdout + result.stderr)
    print(name, 'exit', result.returncode)
    print((result.stdout + result.stderr).decode('utf-8', errors='replace'))
    if result.returncode:
        raise SystemExit(result.returncode)

def main():
    run('tool-build', [JDK / 'javac.exe', '--release', '17', '-encoding', 'UTF-8', '-cp', ASM, '-d', WORK / 'target/tool', *sorted((SERVER / 'tools/semantic-diff/src/main/java').rglob('*.java'))])
    output = WORK / 'target/classes'
    output.mkdir(parents=True, exist_ok=True)
    assert output.resolve().is_relative_to(WORK.resolve())
    for path in output.rglob('*.class'):
        path.unlink()
    staged = SERVER / 'merge-inputs/formal-0.6.71/staged/local/lito381/server'
    reference = SERVER / 'reference/base-0.6.71.jar'
    run('javac', [JDK / 'javac.exe', '--release', '17', '-encoding', 'UTF-8', '-cp', f'{reference};{SERVER / "target/legacy-stubs"};{SERVER / "lib/*"}', '-d', output, *[staged / (name + '.java') for name in BATCH['classes']]])
    jar = WORK / 'target/a03.jar'
    run('jar', [JDK / 'jar.exe', '--create', '--file', jar, '--date', '2026-11-05T00:00:00Z', '-C', output, '.'])
    with zipfile.ZipFile(jar) as archive:
        actual = {name for name in archive.namelist() if name.endswith('.class')}
    expected = set(BATCH['verify_entries'])
    selection = {'entries': len(actual), 'missing': sorted(expected - actual), 'extra': sorted(actual - expected)}
    print(selection)
    (WORK / 'selection.json').write_text(json.dumps(selection, indent=2), encoding='utf-8')
    assert actual == expected
    (WORK / 'entries.txt').write_text('\n'.join(BATCH['verify_entries']) + '\n', encoding='utf-8')
    approvals = []
    for filename, flag in [('effects-approved.txt', '--effects-methods'), ('extra-methods-approved.txt', '--extra-methods'), ('metadata-exempt.txt', '--metadata-exempt'), ('access-widen-approved.txt', '--access-widen'), ('layout-approved.txt', '--layout-methods'), ('deviations.txt', '--deviations')]:
        lines = []
        for line in (ROOT / 'docs/combat-merge' / filename).read_text(encoding='utf-8-sig').splitlines():
            entry = line.strip()
            if not entry or entry.startswith('#'):
                continue
            owner = entry.removeprefix('ctor-prologue-order ').split('#')[0].strip()
            if owner + '.class' in expected:
                lines.append(line)
        if lines:
            path = WORK / filename
            path.write_text('\n'.join(lines) + '\n', encoding='utf-8')
            approvals.extend([flag, path])
    run('semantic', [JDK / 'java.exe', '-Dfile.encoding=UTF-8', '-cp', f'{WORK / "target/tool"};{ASM}', 'local.lito381.semanticdiff.SemanticDiff', '--left', jar, '--right', reference, '--left-classpath', reference, '--classes', WORK / 'entries.txt', *approvals, '--json', WORK / 'semantic.json', '--md', WORK / 'semantic.md'])
    result = json.loads((WORK / 'semantic.json').read_text(encoding='utf-8'))
    print('counts', result['counts'])
    for entry in result['classes']:
        if entry['status'] == 'DIFF':
            print(json.dumps(entry, ensure_ascii=False))
    print('jar SHA256', hashlib.sha256(jar.read_bytes()).hexdigest())

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

### CLI 原始 DIFF 結果

```json
[
  {
    "name": "local/lito381/server/NativeDeVisibilityPc",
    "status": "DIFF",
    "byteSame": false,
    "allowMethodsApplied": false,
    "effectsMethods": [],
    "approvedMethods": [],
    "firstDiff": {
      "where": "method",
      "member": "removeOutOfRangeKnownObjects(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V",
      "insnIndex": 69,
      "detail": "left=GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$RepairKind:[I right=GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#0:[I"
    }
  },
  {
    "name": "local/lito381/server/NativeDeVisibilitySwitch",
    "status": "DIFF",
    "byteSame": false,
    "allowMethodsApplied": false,
    "effectsMethods": [],
    "approvedMethods": [],
    "firstDiff": {
      "where": "field",
      "member": "$SwitchMap$#0:[I",
      "insnIndex": null,
      "detail": "only right"
    }
  }
]
```

## 兩筆剩餘 entry 的完整正規化差異

以下 unified diff 以完整類別 dump 產生，未截斷指令。`-` 為 staged，`+` 為正式 reference。Dump 保留類別 metadata 與隱含建構子的可見證據，這些項目已由既有 metadata 豁免／預設建構子規則處理。欄位宣告與引用採用比較器的實際改名結果。正式驗收以 CLI JSON 為準。

### NativeDeVisibilityPc

```diff
--- NativeDeVisibilityPc.left.txt
+++ NativeDeVisibilityPc.right.txt
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
@@ -168,7 +158,7 @@
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
--- NativeDeVisibilitySwitch.left.txt
+++ NativeDeVisibilitySwitch.right.txt
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
@@ -30,9 +30,9 @@
    INVOKESTATIC local/lito381/server/NativeDeathLifecycleGate$Phase.values()[Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
    ARRAYLENGTH
    NEWARRAY 10
-   PUTSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase:[I
+   PUTSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#1:[I
    LABEL L0
-   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase:[I
+   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#1:[I
    GETSTATIC local/lito381/server/NativeDeathLifecycleGate$Phase.ACTIVE:Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
    INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate$Phase.ordinal()I
    CONST Integer:1
@@ -42,7 +42,7 @@
    LABEL L2
    ASTORE 0
    LABEL L3
-   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase:[I
+   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#1:[I
    GETSTATIC local/lito381/server/NativeDeathLifecycleGate$Phase.RESURRECTING:Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
    INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate$Phase.ordinal()I
    CONST Integer:2
@@ -52,7 +52,7 @@
    LABEL L5
    ASTORE 1
    LABEL L6
-   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase:[I
+   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#1:[I
    GETSTATIC local/lito381/server/NativeDeathLifecycleGate$Phase.DEAD_PENDING:Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
    INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate$Phase.ordinal()I
    CONST Integer:3
@@ -62,7 +62,7 @@
    LABEL L8
    ASTORE 2
    LABEL L9
-   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase:[I
+   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#1:[I
    GETSTATIC local/lito381/server/NativeDeathLifecycleGate$Phase.DEAD_SETTLED:Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
    INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate$Phase.ordinal()I
    CONST Integer:4
@@ -72,7 +72,7 @@
    LABEL L11
    ASTORE 3
    LABEL L12
-   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase:[I
+   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#1:[I
    GETSTATIC local/lito381/server/NativeDeathLifecycleGate$Phase.DEATH_DROPPED:Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
    INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate$Phase.ordinal()I
    CONST Integer:5
@@ -82,7 +82,7 @@
    LABEL L14
    ASTORE 4
    LABEL L15
-   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase:[I
+   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#1:[I
    GETSTATIC local/lito381/server/NativeDeathLifecycleGate$Phase.FINALIZING:Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
    INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate$Phase.ordinal()I
    CONST Integer:6
@@ -92,7 +92,7 @@
    LABEL L17
    ASTORE 5
    LABEL L18
-   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$DeathLifecycleGate$Phase:[I
+   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#1:[I
    GETSTATIC local/lito381/server/NativeDeathLifecycleGate$Phase.TERMINATED:Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
    INVOKEVIRTUAL local/lito381/server/NativeDeathLifecycleGate$Phase.ordinal()I
    CONST Integer:7
@@ -105,9 +105,9 @@
    INVOKESTATIC local/lito381/server/NativeDeDeletionVisibility$RepairKind.values()[Llocal/lito381/server/NativeDeDeletionVisibility$RepairKind;
    ARRAYLENGTH
    NEWARRAY 10
-   PUTSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$RepairKind:[I
+   PUTSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#0:[I
    LABEL L22
-   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$RepairKind:[I
+   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#0:[I
    GETSTATIC local/lito381/server/NativeDeDeletionVisibility$RepairKind.NORMAL_RACE:Llocal/lito381/server/NativeDeDeletionVisibility$RepairKind;
    INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$RepairKind.ordinal()I
    CONST Integer:1
@@ -117,7 +117,7 @@
    LABEL L24
    ASTORE 7
    LABEL L25
-   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$RepairKind:[I
+   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#0:[I
    GETSTATIC local/lito381/server/NativeDeDeletionVisibility$RepairKind.STALL:Llocal/lito381/server/NativeDeDeletionVisibility$RepairKind;
    INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$RepairKind.ordinal()I
    CONST Integer:2
@@ -127,7 +127,7 @@
    LABEL L27
    ASTORE 8
    LABEL L28
-   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$RepairKind:[I
+   GETSTATIC local/lito381/server/NativeDeVisibilitySwitch.$SwitchMap$#0:[I
    GETSTATIC local/lito381/server/NativeDeDeletionVisibility$RepairKind.LEAK:Llocal/lito381/server/NativeDeDeletionVisibility$RepairKind;
    INVOKEVIRTUAL local/lito381/server/NativeDeDeletionVisibility$RepairKind.ordinal()I
    CONST Integer:3
@@ -139,13 +139,3 @@
    LABEL L31
    RETURN
 tryCatch [TRY L0 L1 L2 java/lang/NoSuchFieldError  , TRY L3 L4 L5 java/lang/NoSuchFieldError  , TRY L6 L7 L8 java/lang/NoSuchFieldError  , TRY L9 L10 L11 java/lang/NoSuchFieldError  , TRY L12 L13 L14 java/lang/NoSuchFieldError  , TRY L15 L16 L17 java/lang/NoSuchFieldError  , TRY L18 L19 L20 java/lang/NoSuchFieldError  , TRY L22 L23 L24 java/lang/NoSuchFieldError  , TRY L25 L26 L27 java/lang/NoSuchFieldError  , TRY L28 L29 L30 java/lang/NoSuchFieldError  ]
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


## 差異與診斷重現

將下列 Java 原文儲存為 `scratch/issue-74/reverify/Dump.java` 與 `SwitchDiagnostic.java`。它們只呼叫工具的讀取／比較 API，將輔助程式編譯到獨立 target/tool。

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

```powershell
$jdk = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin'
$work = 'scratch/issue-74/reverify'
$reference = 'lito伺服器/LiTo381-Server/reference/base-0.6.71.jar'
$asm = 'D:/dev-cache/maven/repository/org/ow2/asm/asm/9.7/asm-9.7.jar;D:/dev-cache/maven/repository/org/ow2/asm/asm-tree/9.7/asm-tree-9.7.jar;scratch/issue-74/reverify/target/dependency/asm-analysis-9.7.jar'
& "$jdk/javac.exe" --release 17 -encoding UTF-8 -cp "$work/target/tool;$asm" -d "$work/target/tool" "$work/Dump.java" "$work/SwitchDiagnostic.java"
& "$jdk/java.exe" -Dfile.encoding=UTF-8 -cp "$work/target/tool;$asm" local.lito381.semanticdiff.Dump "$work/target/a03.jar" $reference "$work/normalized"
& "$jdk/java.exe" -Dfile.encoding=UTF-8 -cp "$work/target/tool;$asm" local.lito381.semanticdiff.SwitchDiagnostic "$work/target/a03.jar" $reference
```

Python `difflib.unified_diff` 對各 entry 的 `.left.txt`／`.right.txt` 全文產生上述差異。`SwitchDiagnostic` 的 raw-name 還原只供定位工具缺口，最終重驗仍必須使用主線正式 CLI。

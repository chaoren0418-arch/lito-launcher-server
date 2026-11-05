# Issue #67 — T020-B06 戰鬥併入暫存

## 結論

本輪已更新 B06 的 29 個外層類別，Java 17 編譯成功，輸出精確包含 323 個驗收 entry。全新重建的語意結果為 **298 SAME、25 DIFF、0 ONLY_LEFT、0 ONLY_RIGHT**，missingLeft 與 extraLeft 皆空。**本批驗收仍未完成，交主控以 needs:claude 裁定剩餘差異。**

14 個受裁定影響的 record 全部使用 `@AsRecord`，轉換後逐 entry 均 SAME。7 個一般內部類別使用 #153 同一轉換器提供的 `@AsmFinal` 還原欄位旗標。第一次轉換 21 類，第二次轉換 0 類，323 個 class 的 SHA-256 完全相同。

## 做了什麼

- 以目前 main `450178d` 為基底，讀取前次遠端 `task/67` 的 `7cbde033` staged 內容，再對照本輪 CFR 0.152 的正式與候選反編譯結果修正。正式、候選兩份 SHA-256 均核對設計文件。原始專案的工作檔維持唯讀，Git commit／push 依任務授權寫入共用 Git 中繼資料。
- 補齊 14 個 record 的額外欄位、順序、建構鏈與 accessor。Cube 的四個 StatusLease、Ground 的 Lease、Quest／TimeAttack 的 Monster，合計七類，以 `@AsmFinal` 表達候選在舊建構子保留 JVM 預設值的 final 欄位，避免額外初始化指令。
- 補齊 `ItemWeaponEnchantSpell.consumesItem`、GroupHealing／NaturesBlessing 的治療骰面、WindShackle 初始 area 及完整資料庫 accessor。這些修正涵蓋前次材料消耗與治療骰面的審查意見。
- 補齊 Quest／TimeAttack `moveForAi` 的 `NativeSourceNpcMovement.allowed` 與 `NativeSourceNpcMotion.moving`，以及一般攻擊入口的距離／朝向、怪物初始化、復活與施法排程。先求值、再 callback、最後寫欄位／呼叫的方法順序均依候選核對。
- 補齊 companion 馴服來源、zombie 類型、callback、取消／死亡清理、snapshot 來源記憶，以及 Cube／Ground 的 owner、lease、來源狀態、事件證據與狀態讀取守門。修正 CFR 將 Harmony 同一 lease 的 DUP 展開成兩次 new 的情況，保留單一實體。
- 額外編譯 `src/main` 的這 29 類並逐方法比對候選。將 1,005 個同名無差異方法恢復為現有來源的內容與註解，另恢復 154 個修改方法的原參數名稱。較大型方法保留原迴圈與條件布局後插入候選行為。
- 僅交付指定 staged Java 與本報告。暫存腳本、CFR、清單、診斷與收據留在本 worktree 的 scratch／target。伺服器、登入器、遊戲與 DB 均未執行。

## 已驗證

### 固定輸入與本輪輸出

| 項目 | SHA-256／結果 |
|---|---|
| 正式 `reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| 候選 `reference/candidate-50635f7e.jar` | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| 重建 staged JAR | `8820abf5684b6fd6c9c55826372a21bad10f5f965880d4a954e57fe22d1640c6` |
| JDK | Microsoft JDK 17.0.17，`--release 17 -encoding UTF-8` |
| 編譯來源 | B06.classes 指定的 29 個 staged 檔 |
| 輸出集合 | 323／323，缺少 0、多出 0 |
| record | 14／14 SAME |
| 語意 | 298 SAME、25 DIFF，其餘語意狀態皆 0，BYTE_SAME 6 |
| 後處理 | repo 的 AsRecord CLI，包含其 AsmFinal 支援 |
| 明確核准的語意豁免 | 本輪使用預設比較，未傳入 effects／layout／deviations |

編譯 annotation 的 class 放在獨立 classpath 目錄，與 B06 輸出分離。`lib/*` 本 worktree 為空，這 29 類的依賴由兩份 reference 及 annotation classpath 滿足。`--left-classpath` 提供候選的依賴 metadata 給 #166 欄位 owner 解析，左側 JAR 的內容仍完全取自本輪 staged 編譯與核准轉換。

本輪從乾淨輸出目錄重建的實際輸出如下。檢查器在發現 25 DIFF 後以退出碼 2 結束，semantic-diff 本身的退出碼為 0。

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
semantic exit 0
SUMMARY SAME=298 SAME_EFFECTS=0 SAME_LAYOUT=0 DEVIATION=0 DIFF=25 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=6 ALLOW_METHODS=0
```

### 29 個外層類別的完整驗收分布

同一外層的全部 `$` entry 一併計入。每列 SAME 與 DIFF 相加即為該外層的完整 entry 數，合計 323。

| 外層類別 | SAME | DIFF |
|---|---:|---:|
| `BattleAbilityStatus` | 21 | 2 |
| `BattleBurningWeaponStatus` | 1 | 1 |
| `BattleCompanion` | 18 | 1 |
| `BattleCounterMirrorStatus` | 1 | 1 |
| `BattleCubeHarmonyEffects` | 10 | 1 |
| `BattleCubeIgnitionEffects` | 13 | 0 |
| `BattleCubeQuakeEffects` | 15 | 0 |
| `BattleCubeShockEffects` | 14 | 0 |
| `BattleDragonSkinStatus` | 1 | 1 |
| `BattleElementalFallDownStatus` | 3 | 1 |
| `BattleEraseMagicStatus` | 2 | 0 |
| `BattleFireBlessStatus` | 1 | 1 |
| `BattleFireWeaponStatus` | 1 | 1 |
| `BattleGroundEffects` | 4 | 1 |
| `BattleGuardBrakeStatus` | 1 | 1 |
| `BattleHealingRecovery` | 1 | 0 |
| `BattleHitReactionSuppressCatalog` | 1 | 0 |
| `BattleNaturesTouchStatus` | 1 | 1 |
| `BattleQuestEncounter` | 18 | 1 |
| `BattleResistFearStatus` | 1 | 1 |
| `BattleSpellCatalog` | 132 | 2 |
| `BattleStormEyeStatus` | 1 | 1 |
| `BattleStormShotStatus` | 1 | 1 |
| `BattleTimeAttackEncounter` | 18 | 1 |
| `BattleTimedStatus` | 14 | 1 |
| `BattleTournamentNpcPhysicalDamage` | 1 | 1 |
| `BattleWeightStatus` | 1 | 1 |
| `BattleWindShackleStatus` | 1 | 1 |
| `BattleWindShotStatus` | 1 | 1 |

### 仍待裁定的 25 個 entry

下表直接取本輪 JSON 的第一處差異，位置是正規化指令的 0-based index。每個名稱前綴均為 `local/lito381/server/`。這些 entry 維持 DIFF，驗收仍要求逐一消除。

| Entry | 方法 | 位置與最小差異 |
|---|---|---|
| `BattleAbilityStatus` | `activateDexterity(IJII)I` | 24，`left=ALOAD 0 right=ILOAD 6` |
| `BattleAbilityStatus$Protection` | `applyTo(I)I` | 15，`left=INVOKESTATIC local/lito381/server/CombatRules.protectedDamage(I)I right=ALOAD 0` |
| `BattleBurningWeaponStatus` | `activate(JIII)Z` | 22，`left=ASTORE 7 right=LLOAD 1/2` |
| `BattleCompanion` | `applyDamage(I)Llocal/lito381/server/BattleCompanion$Damage;` | 52，`left=DUP right=ISTORE 4` |
| `BattleCounterMirrorStatus` | `resolve(JIIIZ)Llocal/lito381/server/BattleCounterMirrorStatus$Decision;` | 39，`left=CONST Long:0 right=ALOAD 0` |
| `BattleCubeHarmonyEffects` | `advance(JLjava/util/List;Llocal/lito381/server/BattleCubeHarmonyEffects$Alliance;)Ljava/util/List;` | 317，`left=ASTORE 25 right=ALOAD 18` |
| `BattleDragonSkinStatus` | `activate(JII)Z` | 38，`left=DUP right=LLOAD 1/2` |
| `BattleElementalFallDownStatus` | `activate(JII)Llocal/lito381/server/BattleElementalFallDownStatus$Activation;` | 36，`left=DUP right=ISTORE 6` |
| `BattleFireBlessStatus` | `activate(JII)Z` | 21，`left=DUP right=LLOAD 1/2` |
| `BattleFireWeaponStatus` | `activate(JII)Z` | 21，`left=DUP right=LLOAD 1/2` |
| `BattleGroundEffects` | `cast(Llocal/lito381/server/BattleSpellCatalog$GroundSpell;JIIIILlocal/lito381/server/ArenaMapCatalog$Arena;J)Llocal/lito381/server/BattleGroundEffects$CastResult;` | 43，`left=DUP right=ASTORE 11` |
| `BattleGuardBrakeStatus` | `activate(JII)Z` | 38，`left=DUP right=LLOAD 1/2` |
| `BattleNaturesTouchStatus` | `activate(JI)Z` | 20，`left=DUP right=LLOAD 1/2` |
| `BattleQuestEncounter` | `pollPoisons(JII)Ljava/util/List;` | 120，`left=NEW local/lito381/server/BattleQuestEncounter$PoisonUpdate right=ALOAD 5` |
| `BattleResistFearStatus` | `activate(JII)Z` | 36，`left=DUP right=ISTORE 5` |
| `BattleSpellCatalog$HealingSpell` | `npcHpDelta(II)I` | 4，`left=IF_ICMPLE L1 right=IF_ICMPGT L0` |
| `BattleSpellCatalog$WindShackleSpell` | `<init>(IIIIIIIIIIIIIIIIIIIIIIIIIDDZZIZJILjava/lang/String;)V` | 53，`left=IFNE L0 right=CONST Integer:0` |
| `BattleStormEyeStatus` | `activate(JIII)Z` | 22，`left=DUP right=LLOAD 1/2` |
| `BattleStormShotStatus` | `activate(JIII)Z` | 22，`left=DUP right=LLOAD 1/2` |
| `BattleTimeAttackEncounter` | `pollPoisons(JII)Ljava/util/List;` | 109，`left=NEW local/lito381/server/BattleTimeAttackEncounter$PoisonUpdate right=ALOAD 5` |
| `BattleTimedStatus` | `activateBrave(JII)V` | 38，`left=DUP right=LLOAD 1/2` |
| `BattleTournamentNpcPhysicalDamage` | `againstPlayer(ILlocal/lito381/server/SourceNpcPhysicalStats;Llocal/lito381/server/SourcePhysicalGrowthStats;IIIIIIIJI)Llocal/lito381/server/BattleTournamentNpcPhysicalDamage$Outcome;` | 66，`left=DSTORE 15/16 right=DUP2` |
| `BattleWeightStatus` | `activate(IJI)V` | 26，`left=LLOAD 5/6 right=ALOAD 0` |
| `BattleWindShackleStatus` | `cancel()Z` | 18，`left=DUP right=CONST Long:0` |
| `BattleWindShotStatus` | `activate(JII)Z` | 21，`left=DUP right=LLOAD 1/2` |

### 逐方法補充與限制

- Icons1 更新會在計算到期值後保留 receiver 的 DUP，再 PUTFIELD，最後 bump。Java 的 receiver 暫存／賦值運算式與候選的複製位置不同，#166 在包含多來源 boolean／switch 的整個方法中仍保守回報 DIFF。
- AbilityStatus 的 replace 在候選用 SWAP 把已算好的回傳值與 sourceBuffLeases receiver 重排。Protection.applyTo 還有讀取 divisor 後 POP 的候選指令，CFR 將該次丟棄讀取省略。這部分仍需主控裁定表達或比對工具的證明方式。
- HealingSpell.npcHpDelta 的候選在合法範圍路徑保留 `ILOAD healing; POP`。WindShackle record 建構子有明確 `ICONST_0; IF_ICMPNE`，javac 對相同零比較產生單運算元跳躍。兩者仍未通過預設比較。
- Companion、ElementalFallDown／ResistFear、Ground、Harmony、Quest／TimeAttack poison publication、TimedStatus 與 Tournament 的剩餘項包含 DUP／DUP2、參數或 receiver 暫存的時序差異。全方法的控制流程、複本與來源對應尚未獲比較器證明。

下列列出本輪診斷的全部剩餘一般方法。診斷沿用 repo 的 JarIndex 與 ClassComparer.compareMethod，供定位用途。合成 lambda 仍由正式 CLI 按內容對齊，診斷的原名差異不列為額外缺漏。

```text
BattleAbilityStatus#activateStrength(IJII)I left=ALOAD 0 right=ILOAD 6
BattleAbilityStatus#activateDexterity(IJII)I left=ALOAD 0 right=ILOAD 6
BattleAbilityStatus#activateRoyalAura(IJIIIII)I left=ALOAD 0 right=ILOAD 9
BattleAbilityStatus#removeSkill(I)Z left=ALOAD 0 right=CONST Integer:1
BattleAbilityStatus$Protection#applyTo(I)I left=INVOKESTATIC local/lito381/server/CombatRules.protectedDamage(I)I right=ALOAD 0
BattleBurningWeaponStatus#activate(JIII)Z left=ASTORE 7 right=LLOAD 1/2
BattleCompanion#applyDamage(I)Llocal/lito381/server/BattleCompanion$Damage; left=DUP right=ISTORE 4
BattleCompanion#receiveItemAndOnGetItemPersisted(Llocal/lito381/server/RuntimeInventory$Item;JJLlocal/lito381/server/DurableCommit;)Llocal/lito381/server/BattleCompanion$GivenItemReceipt; left=DUP right=ASTORE 8
BattleCounterMirrorStatus#resolve(JIIIZ)Llocal/lito381/server/BattleCounterMirrorStatus$Decision; left=CONST Long:0 right=ALOAD 0
BattleCubeHarmonyEffects#advance(JLjava/util/List;Llocal/lito381/server/BattleCubeHarmonyEffects$Alliance;)Ljava/util/List; left=ASTORE 25 right=ALOAD 18
BattleDragonSkinStatus#activate(JII)Z left=DUP right=LLOAD 1/2
BattleDragonSkinStatus#clear()V left=DUP right=CONST Long:0
BattleElementalFallDownStatus#activate(JII)Llocal/lito381/server/BattleElementalFallDownStatus$Activation; left=DUP right=ISTORE 6
BattleFireBlessStatus#activate(JII)Z left=DUP right=LLOAD 1/2
BattleFireWeaponStatus#activate(JII)Z left=DUP right=LLOAD 1/2
BattleGroundEffects#cast(Llocal/lito381/server/BattleSpellCatalog$GroundSpell;JIIIILlocal/lito381/server/ArenaMapCatalog$Arena;J)Llocal/lito381/server/BattleGroundEffects$CastResult; left=DUP right=ASTORE 11
BattleGuardBrakeStatus#activate(JII)Z left=DUP right=LLOAD 1/2
BattleNaturesTouchStatus#activate(JI)Z left=DUP right=LLOAD 1/2
BattleQuestEncounter#pollPoisons(JII)Ljava/util/List; left=NEW local/lito381/server/BattleQuestEncounter$PoisonUpdate right=ALOAD 5
BattleResistFearStatus#activate(JII)Z left=DUP right=ISTORE 5
BattleSpellCatalog$HealingSpell#npcHpDelta(II)I left=IF_ICMPLE L1 right=IF_ICMPGT L0
BattleSpellCatalog$WindShackleSpell#<init>(IIIIIIIIIIIIIIIIIIIIIIIIIDDZZIZJILjava/lang/String;)V left=IFNE L0 right=CONST Integer:0
BattleStormEyeStatus#activate(JIII)Z left=DUP right=LLOAD 1/2
BattleStormShotStatus#activate(JIII)Z left=DUP right=LLOAD 1/2
BattleTimeAttackEncounter#pollPoisons(JII)Ljava/util/List; left=NEW local/lito381/server/BattleTimeAttackEncounter$PoisonUpdate right=ALOAD 5
BattleTimedStatus#activateBrave(JII)V left=DUP right=LLOAD 1/2
BattleTimedStatus#expireBrave(J)Z left=DUP right=CONST Long:0
BattleTimedStatus#consumeCounterMagic(J)Z left=CONST Long:0 right=ALOAD 0
BattleTimedStatus#activateElementalFire(JI)Z left=DUP right=LLOAD 1/2
BattleTimedStatus#activateSoulOfFlame(JI)Z left=DUP right=LLOAD 1/2
BattleTimedStatus#cancelMagicEffects()Llocal/lito381/server/BattleTimedStatus$MagicCancellation; left=DUP right=CONST Long:0
BattleTimedStatus#clear()V left=DUP right=CONST Long:0
BattleTournamentNpcPhysicalDamage#againstPlayer(ILlocal/lito381/server/SourceNpcPhysicalStats;Llocal/lito381/server/SourcePhysicalGrowthStats;IIIIIIIJI)Llocal/lito381/server/BattleTournamentNpcPhysicalDamage$Outcome; left=DSTORE 15/16 right=DUP2
BattleWeightStatus#activate(IJI)V left=LLOAD 5/6 right=ALOAD 0
BattleWeightStatus#expire(J)Z left=CONST Long:0 right=ALOAD 0
BattleWeightStatus#clear()V left=DUP right=CONST Long:0
BattleWindShackleStatus#cancel()Z left=DUP right=CONST Long:0
BattleWindShackleStatus#snapshot(J)Llocal/lito381/server/BattleWindShackleStatus$Snapshot; left=CONST Long:0 right=ALOAD 0
BattleWindShotStatus#activate(JII)Z left=DUP right=LLOAD 1/2
```

## 可直接重跑的指令

在本 worktree 根目錄執行下列 Python 程式。可將此段另存為 `scratch/issue-67/reproduce.py`，再執行下方 PowerShell。程式自行產生完整 323 條清單，重新編譯兩個 repo 工具、annotation 與 staged，轉換、驗證冪等性、打包，輸出逐 entry JSON／Markdown。ASM 依賴使用 repo 工具同版的 9.7 與固定 SHA-256，缺少時下載至本 worktree target。

```powershell
Set-Location 'D:\lineage開發\3-lito開發\scratch\wt\task-67'
& 'C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe' -B -X utf8 scratch/issue-67/reproduce.py
exit $LASTEXITCODE
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
scratch = root / 'scratch/issue-67/replay'
target = server / 'target/issue-67/replay'
scratch.mkdir(parents=True, exist_ok=True)
target.mkdir(parents=True, exist_ok=True)
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
for path in classes.rglob('*.class'):
    path.unlink()
staged = server / 'merge-inputs/50635f7e/staged/local/lito381/server'
sources = [staged / (name + '.java') for name in batch['classes']]
cp = os.pathsep.join(map(str, [candidate, formal, server / 'lib/*', annotations]))
run([javac, '-J-Dfile.encoding=UTF-8', '--release', '17', '-encoding', 'UTF-8', '-cp', cp, '-d', classes, *sources], 'javac')
converter_cp = str(target / 'as-record') + os.pathsep + asm_cp
run([java, '-Dfile.encoding=UTF-8', '-cp', converter_cp, 'local.lito381.asrecord.AsRecord', classes], 'as-record')
entries = sorted(p.relative_to(classes).as_posix() for p in classes.rglob('*.class'))
missing = sorted(set(batch['verify_entries']) - set(entries))
extra = sorted(set(entries) - set(batch['verify_entries']))
assert not missing and not extra
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
semantic_cp = str(target / 'semantic-diff') + os.pathsep + asm_cp
run([java, '-Dfile.encoding=UTF-8', '-Xmx2g', '-cp', semantic_cp, 'local.lito381.semanticdiff.SemanticDiff', '--left', jar, '--right', candidate, '--left-classpath', candidate, '--classes', selection, '--json', scratch / 'semantic.json', '--md', scratch / 'semantic.md'], 'semantic')
data = json.loads((scratch / 'semantic.json').read_text('utf-8'))
record_entries = [r['entry'][:-6] for r in json.loads((root / 'docs/combat-merge/record-asm-fields.json').read_text('utf-8'))['records'] if r['class'].split('/')[-1].split('$')[0] in batch['classes']]
assert len(record_entries) == 14
assert all(c['status'] == 'SAME' for c in data['classes'] if c['name'] in record_entries)
assert data['classSelection']['missingLeft'] == [] and data['classSelection']['extraLeft'] == []
receipt = {'counts': data['counts'], 'recordSame': 14, 'entries': len(entries), 'jarSha256': hashlib.sha256(jar.read_bytes()).hexdigest(), 'sources': {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in sources}}
(scratch / 'receipt.json').write_text(json.dumps(receipt, indent=2), encoding='utf-8')
print(json.dumps(receipt, ensure_ascii=False))
if data['counts']['DIFF'] or data['counts']['ONLY_LEFT'] or data['counts']['ONLY_RIGHT']:
    raise SystemExit(2)
```

## 產出路徑

- 交付 Java 位於 `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/`，精確包含 B06.classes 的 29 檔。
- 交付報告為 `docs/reports/issue-67.md`。
- 本機可重建產物位於 `lito伺服器/LiTo381-Server/target/issue-67/replay/`，其中 `classes/` 是轉換後 B06 classes，`staged.jar` 是驗收輸入。
- 本機逐 entry 證據位於 `scratch/issue-67/replay/{semantic.json,semantic.md,verify_entries.txt,receipt.json}`，同目錄各步驟 `.log` 是本輪實際輸出。
- 三方對照與診斷位於 `scratch/issue-67/{formal,candidate,*.diff,three-way-audit.json,original-methods.log,restored-methods.json,methods.log,methods/}`。這些為本機 scratch 資料，完整重跑程式已內嵌於本報告，可在新的 checkout 重建驗收證據。

## 推測

大部分剩餘 DIFF 可能可透過保持來源對應的局部 stack／控制流程正規化證明等價。這一推測尚未經新正反例驗證，不能據此將 25 項改記 SAME，也不能推導整批執行行為等價。

## 待驗證

- 25 個 DIFF 的修復或主控裁定。現有來源仍有上表列出的指令層差異，B06 完成條件尚未達成。
- javac 的 unchecked 警告主要來自候選泛型擦除與 owner 回傳的 List<?> 轉型，本輪以語意比對核對 runtime cast 與方法 signature。
- 本任務的單元測試與行為回歸依設計交 B-land 統一處理，尚待執行。實機登入／戰鬥、正式資料庫與部署亦待後續驗收。
- 本輪 staged 的 323 entries 才是本批證據。後續一般 Maven CI 的結果應依其實際輸入分開解讀。

## 給下一棒的提示

請 Claude 主控先審閱 25 個 DIFF 與逐方法最小差異，再指派可證明安全的 Java 改寫或 semantic-diff 擴充。尤其保留欄位讀取、callback 先後、資料來源與 DUP 複本數，擴充時加入相反行為的負例。等待預設驗收達到 323 SAME 後再交 B-land。

交付沿用 `task/67`。遠端原有 `7cbde033` 是前次已退回的版本，本次以 ours merge 納入其祖先，再提交本次 staged 修正，讓一般 `git push -u origin task/67` 能快轉並保留舊提交可追溯。

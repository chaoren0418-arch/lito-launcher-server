# Issue #73 — A02 完整 49 類／104 entry 驗收通過

## 結論／做了什麼

2026-11-06 已完成本批 code 階段：**49 份 staged Java 全部重新編譯成功，104 個 entry 為 86 SAME、16 SAME_PATHS、2 DEVIATION；DIFF、ONLY_LEFT、ONLY_RIGHT、missing、extra 全為 0。** 176 個方法級 VISIBILITY_BRIDGE 通過 #203 的證明。可交 Claude 審查 PR #101，再交 #78 A-land；本報告取代前輪「48 檔／103 entry、private 編譯阻擋」結論。

- 沿用 worktree `scratch/wt/task-73` 與分支 `task/73`，沒有 checkout 或建立分支。從 `d0a1550` 合併最新 main `efdeec134ddc4d7e7b7c7c507ef21a3accae6077`，合併提交 `14258f3f6910a59724ef051e501ccc90a4ace57c`，包含 #203、#216、#235 與 A03。兩份 deviations 文件發生追加位置衝突，解決時保留雙方全部核准。
- 依 [最新主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/73#issuecomment-6668602650)，`NativeDeNpcConstructionState` 只改兩處：private 方法改名 `cancelNpcChatTimer$own()`，`sourceChat$startChat(I)V` 呼叫新名。維持 private，未加 `@Override`。`sourceChat$cancelNpcChatTimer()` 保持原名。
- 保留 `NativeDeItemWeaponProcTable.reload()` 既有明確 `NoSuchFieldError("_instance")` 與 method-diff 核准；未修改該檔。private-rename 核准由 #235 合併帶入，未新增其他核准。
- 整個 PR 相對 main 僅有 A02 的 49 份 staged Java、deviations.txt／deviations.md 的本批 reload 項目及本報告，共 52 檔。本輪合併後只修改一份 Java 及本報告。`src/main`、`pom.xml`、`docs/REPORT.md` 無本任務修改；main 已合併的變更照原樣帶入。

## 已驗證／怎麼驗證

環境：Microsoft OpenJDK 17.0.17、Python 3.12.10、ASM／asm-tree／asm-analysis 9.7。正式 reference SHA-256 固定為 `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`。

1. 從 `a-batches.json` 的 A02 精確選出 49 份來源與完整 104 行 verify_entries，不排除任何類別。在全新 `scratch/issue-73/private-rename-final/target/` 重編，不沿用舊 class。
2. 先編 #108 compile-stubs、A03 的 `NativeDeWorldLog.java` 與既有 `AsmFinal` 註解作編譯相依。A02 classpath 沿用此前成功流程：A03／註解 classes、正式 reference、legacy-stubs、`lib/*`。這些相依皆不打入 A02 JAR。
3. 對全部 49 份 staged 執行 `javac --release 17 -encoding UTF-8`，exit 0；僅有 unchecked／unsafe operations 的提示。執行既有 AsRecord 轉換器，`scanned=104 converted=1`；逐 class 雜湊確認只修改 `NativeDeItemTable.class` 的既有 @AsmFinal，沒有修改本次改名 class。
4. 直接將這 104 個編譯 class 打包；斷言 class 目錄、JAR entry 集合、a-batches.json.verify_entries 完全相同，且 JAR 中每份 bytes 等於對應輸出。未把正式 class 或舊 class 補入產物。
5. 從合併後 main 重新編譯 semantic-diff。掃描全部七份核准清單，僅選本批精確條目：access-widen 2 條（NpcVitalState.getX/getY）、deviations 2 條（reload method-diff、cancel private-rename）；其他清單沒有本批條目。工具依規則拒絕範圍外條目，因此不直接傳其他批的完整清單。沒有使用 allow-right 或整類跳過。
6. semantic-diff exit 0；再獨立檢查 JSON 計數、每類狀態、完整 entry 集合及 javap。最終重編腳本與獨立稽核皆 exit 0。

| 類別狀態 | 數量 |
|---|---:|
| SAME | 86 |
| SAME_PATHS | 16 |
| DEVIATION | 2 |
| DIFF／ONLY_LEFT／ONLY_RIGHT | 各 0 |
| missingLeft／extraLeft | 各 0 |

`BYTE_SAME=9` 是上述 104 類的子集合；`proofMethodCounts.VISIBILITY_BRIDGE=176` 是方法數，兩者都不可另加到類別總數。

### 改名與派送證據

`NativeDeNpcConstructionState` 整類為 DEVIATION，方法結果如下：

| 方法 | 結果與證據 |
|---|---|
| `cancelNpcChatTimer$own()V` → `cancelNpcChatTimer()V` | DEVIATION；private shadow guards 與完整本體比較通過，並非 method-diff 豁免 |
| `sourceChat$startChat(I)V` | SAME_PATHS；1310 個路徑多重集合相等 |
| `scheduleNpcChatTimer(...)` | SAME_PATHS；655 個路徑多重集合相等 |
| 類別的可見性 bridge | 176 個 VISIBILITY_BRIDGE；Java 無新增人工 bridge |

javap `-c -p -s` 核對：左側為 `private void cancelNpcChatTimer$own()`，右側為 `private void cancelNpcChatTimer()`；兩側本體都呼叫 `NativeDeChatRuntime.cancel(this)`。`sourceChat$startChat` 的 offset 139 兩側均為 invokevirtual，左側目標 `cancelNpcChatTimer$own:()V`，右側目標 `cancelNpcChatTimer:()V`。左側沒有原名方法宣告，`sourceChat$cancelNpcChatTimer()` 仍 private 且原名保留。

祖先 `NativeDeDeletionState` 未修改，刪除／註冊失敗路徑仍呼叫祖先的 `cancelNpcChatTimer()`；子類新名 private 不構成覆寫。此為來源與 bytecode 的靜態結論，應用層 chat retire／remaining.cancelChat 執行驗收留給 #78。#235 的隔離 JVM 正反例證據見 [issue-235](issue-235.md)，本輪沒有把工具 fixture 當作應用層測試。

### reload 核准偏離

本輪 javap 再確認：`requireLoading`（offset 4）與 `new NativeDeItemWeaponProcTable()`（offset 7–11）先執行；offset 14 起 POP、NEW NoSuchFieldError、LDC `_instance`、建構錯誤、ATHROW。正式版原本同一位置為引用不存在欄位的 PUTSTATIC／GETSTATIC。其他欄位、方法與 metadata 仍完整比較。

完整核准方法差異（本輪 semantic.json）：

```text
method reload()V insn[6]: left=POP right=PUTSTATIC unresolved(local/lito381/server/NativeDeItemWeaponProcTable)._instance:Llocal/lito381/server/NativeDeItemWeaponProcTable;
LEFT
()V
0x1


||||pvis=|pinv=|lvtvis=|lvtinv=

[]
[ALOAD 0, GETFIELD local/lito381/server/NativeDeItemWeaponProcTable.nativeDefinitions:Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;, INVOKESTATIC local/lito381/server/NativeDeItemDefinitionsScope.requireLoading(Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;)V, NEW local/lito381/server/NativeDeItemWeaponProcTable, DUP, INVOKESPECIAL local/lito381/server/NativeDeItemWeaponProcTable.<init>()V, POP, NEW java/lang/NoSuchFieldError, DUP, CONST str:"_instance", INVOKESPECIAL java/lang/NoSuchFieldError.<init>(Ljava/lang/String;)V, ATHROW]
RIGHT
()V
0x1


||||pvis=|pinv=|lvtvis=|lvtinv=

[]
[ALOAD 0, GETFIELD local/lito381/server/NativeDeItemWeaponProcTable.nativeDefinitions:Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;, INVOKESTATIC local/lito381/server/NativeDeItemDefinitionsScope.requireLoading(Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;)V, NEW local/lito381/server/NativeDeItemWeaponProcTable, DUP, INVOKESPECIAL local/lito381/server/NativeDeItemWeaponProcTable.<init>()V, PUTSTATIC unresolved(local/lito381/server/NativeDeItemWeaponProcTable)._instance:Llocal/lito381/server/NativeDeItemWeaponProcTable;, GETSTATIC unresolved(local/lito381/server/NativeDeItemWeaponProcTable)._instance:Llocal/lito381/server/NativeDeItemWeaponProcTable;, INVOKEVIRTUAL local/lito381/server/NativeDeItemWeaponProcTable.load()V, RETURN]
```

前輪 `d0a1550` 已掃描同一正式 JAR 全部 2,554 class，reload 的直接呼叫與 method handle 均為 0；資料表正常載入經反射固定字串 `load`。這份既有靜態調查保留於 git 歷史及 deviations.md，本輪未重跑全 JAR 呼叫掃描，也不將外部呼叫／動態反射排除。缺欄位根因仍由 #184 處理。

## 推測

在固定 reference、來源與工具模型下已滿足 A02 語意驗收；這不表示遊戲、並行或外部反射情境已通過。private 改名能保留祖先派送的靜態證據如上，實際 NPC 刪除流程仍須在 A-land 測試。

## 未驗證什麼／待驗證

- 本批不跑應用單元測試或回歸，依指定範圍交 #78 統一執行。未執行伺服器、登入器、遊戲、正式 DB，未部署。
- #78 必須驗證 NPC 刪除時 chat retire 與 remaining.cancelChat 均仍執行；有效 loading context 下 reload 拋 `NoSuchFieldError("_instance")`。前置 `requireLoading` 或建構失敗可能先拋其他錯誤，不能忽略前置順序。
- CI 結果以 PR #101 當前 checks 為準；本報告的本機驗收是 staged 的完整 javac／semantic-diff，不宣稱 Maven 產品整合已完成。

## 產出路徑／重現

- 交付：`lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/` 下 A02 49 份 `.java`、`docs/combat-merge/deviations.txt`／`deviations.md`、`docs/reports/issue-73.md`。
- 本機證據：`scratch/issue-73/private-rename-final/` 的 `commands.json`、`semantic.json`／`semantic.md`、`receipt.json`、`conversion.json`、核准子清單與四份 javap 輸出。
- 建置產物：該目錄的 `target/a02.jar`、`target/classes/`、`target/tool/` 等；全部不進 git。
- 收據包含 49 份來源、A03 相依、stubs、註解、轉換器與比較器全部來源、ASM 依賴、核准清單、命令與產物雜湊。

| 檔案 | SHA-256 |
|---|---|
| A02 JAR | `9abe4b5bb21225eeec649e218f9c48be8918437770877ea1bd0263d8d5eea5da` |
| 改名 Java | `c2bce1412a147fc8b526d7062298b6d2a63d62337d6d7c259e36d70b9bb73d81` |
| semantic.json | `d28935efa1f49f801e6f4b520dc5d5bded1bd02cacef6d0b4e8a2c2e273cdae9` |
| receipt.json | `c8a29b446ab97bdaf1446fa12a8f4560b8f1aea1b089caf825ecd4fe9535c52e` |

從 worktree 根目錄執行以下腳本；參數必須是全新目錄名稱（腳本拒絕重用輸出）。Windows classpath 使用分號；所有具體 argv 同時保存在 commands.json。asm-analysis 9.7 沿用本 worktree 已下載依賴並核對 SHA-256，其餘 ASM 讀取本機 Maven cache。

```powershell
& D:/dev-orchestrator/tools/python/python.exe -B -X utf8 scratch/issue-73/verify_full.py <全新目錄名稱>
```

重建腳本全文（儲存為上述 scratch 路徑）：

```python
from pathlib import Path
import sys, subprocess, json, zipfile, hashlib
root=Path.cwd()
server=root/'lito伺服器/LiTo381-Server'
stage=server/'merge-inputs/formal-0.6.71/staged/local/lito381/server'
work=root/'scratch/issue-73'/sys.argv[1]
work.mkdir(parents=True,exist_ok=False)
target=work/'target'; target.mkdir()
jdk=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
batch=next(b for b in json.loads((root/'docs/combat-merge/a-batches.json').read_text(encoding='utf-8'))['batches'] if b['id']=='A02')
assert len(batch['classes']) == 49 and len(batch['verify_entries']) == 104
formal=server/'reference/base-0.6.71.jar'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
assert sha(formal)=='e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498'
steps=[]
def run(name,args):
    args=list(map(str,args)); p=subprocess.run(args,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    (work/(name+'.log')).write_bytes(p.stdout)
    steps.append(dict(name=name,command=args,exit=p.returncode))
    (work/'commands.json').write_text(json.dumps(steps,ensure_ascii=False,indent=2),encoding='utf-8')
    print(name,p.returncode,flush=True)
    if p.returncode: raise SystemExit(p.stdout.decode('utf-8',errors='replace'))
javac=[jdk/'javac.exe','-J-Dfile.encoding=UTF-8','--release','17','-encoding','UTF-8']
java=[jdk/'java.exe','-Dfile.encoding=UTF-8']
stubs=target/'legacy-stubs'; stubs.mkdir()
run('stubs-javac',[*javac,'-d',stubs,*sorted((server/'compile-stubs').rglob('*.java'))])
a03=target/'a03'; a03.mkdir()
run('a03-javac',[*javac,'-cp',str(formal),' -d'.strip(),a03,stage/'NativeDeWorldLog.java'])
run('annotations-javac',[*javac,'-d',a03,server/'src/main/java/local/lito381/server/AsmFinal.java'])
classes=target/'classes'; classes.mkdir()
sources=[stage/(n+'.java') for n in batch['classes']]
run('a02-javac',[*javac,'-cp',f'{a03};{formal};{stubs};{server}/lib/*','-d',classes,*sources])
asm=[Path('D:/dev-cache/maven/repository/org/ow2/asm')/a/'9.7'/(a+'-9.7.jar') for a in ('asm','asm-tree','asm-analysis')]
asm[-1]=root/'scratch/issue-73/target/dependency/asm-analysis-9.7.jar'
converter=target/'as-record'; converter.mkdir()
run('converter-javac',[*javac,'-cp',';'.join(map(str,asm)),'-d',converter,*sorted((server/'tools/as-record/src/main/java').rglob('*.java'))])
before={p.relative_to(classes).as_posix():sha(p) for p in classes.rglob('*.class')}
raw=target/'javac-raw';raw.mkdir()
(raw/'NativeDeItemTable.class').write_bytes((classes/'local/lito381/server/NativeDeItemTable.class').read_bytes())
run('asm-final',[*java,'-cp',';'.join(map(str,[converter,*asm])),'local.lito381.asrecord.AsRecord',classes])
changed=[p.relative_to(classes).as_posix() for p in classes.rglob('*.class') if sha(p)!=before[p.relative_to(classes).as_posix()]]
assert changed==['local/lito381/server/NativeDeItemTable.class'],changed
(work/'conversion.json').write_text(json.dumps({'changed':changed,'rawClassHashes':before},indent=2),encoding='utf-8')
actual={p.relative_to(classes).as_posix() for p in classes.rglob('*.class')}
assert actual==set(batch['verify_entries']), (actual-set(batch['verify_entries']),set(batch['verify_entries'])-actual)
jar=target/'a02.jar'
with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED) as z:
    for e in sorted(actual):
        info=zipfile.ZipInfo(e,(2000,1,1,0,0,0)); info.compress_type=zipfile.ZIP_DEFLATED
        z.writestr(info,(classes/e).read_bytes())
with zipfile.ZipFile(jar) as z:
    assert set(z.namelist())==actual and all(z.read(e)==(classes/e).read_bytes() for e in actual)
entries=work/'verify_entries.txt'; entries.write_text('\n'.join(batch['verify_entries'])+'\n',encoding='utf-8')
asm=[Path('D:/dev-cache/maven/repository/org/ow2/asm')/a/'9.7'/(a+'-9.7.jar') for a in ('asm','asm-tree','asm-analysis')]
asm[-1]=root/'scratch/issue-73/target/dependency/asm-analysis-9.7.jar'
assert sha(asm[-1])=='7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f'
tool=target/'tool'; tool.mkdir()
toolsrc=sorted((server/'tools/semantic-diff/src/main/java').rglob('*.java'))
run('tool-javac',[*javac,'-cp',';'.join(map(str,asm)),'-d',tool,*toolsrc])
cp=';'.join(map(str,[tool,*asm]))
flags=[]
for filename,flag in [('access-widen-approved.txt','--access-widen'),('effects-approved.txt','--effects-methods'),('layout-approved.txt','--layout-methods'),('deviations.txt','--deviations'),('metadata-exempt.txt','--metadata-exempt'),('extra-methods-approved.txt','--extra-methods'),('exceptions-exempt.txt','--exceptions-exempt')]:
    lines=[s for s in (root/'docs/combat-merge'/filename).read_text(encoding='utf-8-sig').splitlines() if s.strip() and not s.startswith('#') and s.split('#')[0].strip().removeprefix('ctor-prologue-order ')+'.class' in actual]
    if lines:
        out=work/filename; out.write_text('\n'.join(lines)+'\n',encoding='utf-8'); flags += [flag,out]
run('semantic',[*java,'-cp',cp,'local.lito381.semanticdiff.SemanticDiff','--left',jar,'--right',formal,'--left-classpath',formal,'--classes',entries,*flags,'--json',work/'semantic.json','--md',work/'semantic.md'])
result=json.loads((work/'semantic.json').read_text(encoding='utf-8'))
receipt={'counts':result['counts'],'classSelection':result['classSelection'],'hashes':{str(p.relative_to(root)) if p.is_relative_to(root) else str(p):sha(p) for p in [formal,jar,*sources,stage/'NativeDeWorldLog.java',*toolsrc,*asm]},'entries':len(actual)}
for p in [*sorted((server/'compile-stubs').rglob('*.java')),*sorted((server/'tools/as-record/src/main/java').rglob('*.java')),server/'src/main/java/local/lito381/server/AsmFinal.java',*sorted((root/'docs/combat-merge').glob('*.txt')),root/'docs/combat-merge/a-batches.json',Path(__file__),work/'conversion.json',work/'commands.json',entries,work/'semantic.json',raw/'NativeDeItemTable.class']:
    receipt['hashes'][p.relative_to(root).as_posix() if p.is_absolute() else p.as_posix()]=sha(p)
(work/'receipt.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2),encoding='utf-8')
assert result['classSelection'] == {'missingLeft': [], 'extraLeft': []}, result['classSelection']
assert len(result['classes']) == 104
allowed = {'SAME', 'SAME_EFFECTS', 'SAME_PATHS', 'SAME_CFG', 'DEVIATION', 'VISIBILITY_BRIDGE'}
assert all(c['status'] in allowed for c in result['classes'])
assert sum(v for k, v in result['counts'].items() if k != 'BYTE_SAME') == 104
print(json.dumps(result['counts']))
for c in result['classes']:
    if c['status'] not in ('SAME','SAME_EFFECTS'): print(c['name'],c['status'],json.dumps(c.get('firstDiff',{}),ensure_ascii=False))
```

## 給下一棒的提示

Claude 審查 PR #101 的兩處 private 改名、合併時保留的核准清單與本次 104 entry 證據。A02 已無待裁定差異；SAME_PATHS 與 176 個 bridge 不必再裁定。審查通過後由使用者合併，調度器交 #78 整合與應用回歸；#184 續追 reload 所涉缺欄位根因。不要把改名方法改回 package／protected 或移除 reload 前置檢查與建構。

# Issue #73 — A02 主控裁定修正與 private 繼承編譯阻擋

## 結論

**尚未通過 A02，不能送 A-land。** 2026-11-06 已合併最新 main（`567b0cb7762864b10ef876f90749e74a66e9bb01`，整合提交 `b5a78c52c57760b9008f23acf755b8c0b68e5b1e`），包含 A03 與已合併 #203。按裁定把 `cancelNpcChatTimer()V` 改回 private 後，49 檔整批 javac exit 1：Java 不允許子類 private 方法與同 package 祖先的非 private 同簽名方法並存。這不是其他類別呼叫 private 方法的錯誤，也不是 INVOKEVIRTUAL／INVOKESPECIAL 正規化能解決的問題。

獨立編譯其餘 **48 檔／103 entry**：86 SAME、16 SAME_PATHS、1 DEVIATION、0 DIFF。再對同一診斷 JAR 使用完整 **104 entry** 清單：同上加 **1 ONLY_RIGHT／missingLeft**（`NativeDeNpcConstructionState`），extraLeft 為空。這是明確缺一類的診斷結果，**不是整批驗收通過**；沒有將上一輪 class 或正式 class 補進 JAR。

SAME_PATHS 已獲核准，無須再次裁定；176 個 bridge 已由 #203 支援，無須另行裁定或修改 Java。唯一待主控處理的是下述 private 繼承表達限制。已實際為 Issue #73、PR #101 加上 `needs:claude`，並以 `gh issue/pr view --json labels` 讀回確認；這是本輪觀測，不沿用前輪錯誤的標籤敘述。

## 做了什麼

- 沿用 `task/73`，未 checkout／另建分支，合併 main 無衝突。
- `NativeDeNpcConstructionState.cancelNpcChatTimer()` 改回 private，移除 `@Override` 與過期註解。`sourceChat$startChat(I)V` 仍在同類呼叫 `this.cancelNpcChatTimer()`，因此來源指向 private 方法；但因整類無法編譯，**尚無本輪 class 可驗證其呼叫指令及完整語意**，不宣稱此要求已驗收。
- 移除 `NativeDeItemWeaponProcTable._instance`；`reload()` 依序保留 `requireLoading(this.nativeDefinitions)`、`new NativeDeItemWeaponProcTable()`，在原本 PUTSTATIC 的位置明確拋出 `NoSuchFieldError("_instance")`。沒有略過新物件建構。
- 僅追加 `reload()V` 的 `method-diff` 至 deviations.txt／deviations.md，依 [主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/73#issuecomment-6668320896)。其他核准清單未變動。
- 重寫本報告，移除舊報告對 SAME_PATHS、bridge 與標籤狀態的不正確要求；舊 86／16／2 DIFF 證據由 git 保留。
- 本輪只改兩份 A02 Java、兩份 deviations 文件及本報告。`src/main`、`pom.xml`、`docs/REPORT.md` 相對 main 無本任務變更；合併帶入的主線檔案不算本批自行修改。

## 已驗證：編譯阻擋與正式行為證據

環境為 Microsoft OpenJDK 17.0.17、Python 3.12.10、ASM 9.7。使用全新 `scratch/issue-73/ruling-full/target/`，從 a-batches.json 精確選取 49 份來源；#108 的 9 份 compile-stubs、A03 NativeDeWorldLog 與 AsmFinal 註解先分別編譯成功。49 檔編譯完整錯誤如下：

```text
D:\lineage開發\3-lito開發\scratch\wt\task-73\lito伺服器\LiTo381-Server\merge-inputs\formal-0.6.71\staged\local\lito381\server\NativeDeNpcConstructionState.java:464: error: cancelNpcChatTimer() in NativeDeNpcConstructionState cannot override cancelNpcChatTimer() in NativeDeDeletionState
    private void cancelNpcChatTimer() {
                 ^
  attempting to assign weaker access privileges; was package
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
1 error
```

`NativeDeDeletionState` 來自未修改的正式 JAR，非本批來源，也沒有私人 stub 遮蔽它。`NativeDeNpcConstructionState` 雖經 `NativeDeNpcTemplateState` 等類繼承，javac 仍辨識其祖先同名 package 方法。現有編譯錯誤只有這一項，沒有外部呼叫 private 方法的錯誤。

下列為正式 JAR 的 javap `-c -p -s` 節錄：

```text
  private void cancelNpcChatTimer();
    descriptor: ()V
    Code:
       0: aload_0
       1: invokestatic  #487                // Method local/lito381/server/NativeDeChatRuntime.cancel:(Llocal/lito381/server/NativeDeNpcConstructionState;)V
       4: return

  void cancelNpcChatTimer();
    descriptor: ()V
    Code:
       0: aload_0
       1: getfield      #145                // Field deletionOwner:Llocal/lito381/server/NativeDeDeletionOwner;
       4: getfield      #228                // Field local/lito381/server/NativeDeDeletionOwner.services:Llocal/lito381/server/NativeDeDeletionPorts$Services;
       7: aload_0
       8: invokeinterface #235,  2          // InterfaceMethod local/lito381/server/NativeDeDeletionPorts$Services.cancelChat:(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
      13: return
```

全 JAR 掃描對 `cancelNpcChatTimer` 找到且只找到三個直接呼叫，無 method handle；javap 原始 offset 如下。沒有其他類別直接呼叫子類 private 方法。

| 呼叫端（共同前綴 local.lito381.server） | offset | opcode／owner |
|---|---:|---|
| NativeDeNpcConstructionState.sourceChat$startChat(I)V | 139 | INVOKEVIRTUAL NativeDeNpcConstructionState.cancelNpcChatTimer()V |
| NativeDeDeletionState.sourceDeleteNpc()V | 546 | INVOKEVIRTUAL NativeDeDeletionState.cancelNpcChatTimer()V |
| NativeDeDeletionState.sourceAbortFailedWorldRegistration()V | 88 | INVOKEVIRTUAL NativeDeDeletionState.cancelNpcChatTimer()V |

**行為差異的靜態判讀**：正式子類 private 不覆寫祖先方法，所以後兩個呼叫保留 `services.cancelChat(this)`；`NativeDeWorldContext.cancelChat` 先呼叫 `NativeDeChatRuntime.cancelObject`（其中 `NativeDeChatJobs.retire` 在 offset 17），再呼叫 `remaining.cancelChat`（offset 9）。前輪 package override 會改走子類 `NativeDeChatRuntime.cancel`，漏掉 retire 與 remaining，不能用 access-widen 當作等價。這是實際行為差異，不只是可見性表示。

**處置**：保留主控要求的 private，不改名、不改 package／protected、不修改祖先來源、正式 JAR 或編譯 classpath bytes，也不自行引入 ASM 修補。需 Claude 裁定可以保留正式派送行為的 Java／建置表達方案及範圍，再重驗整批。不能僅重新派同一份 private 指令期待 javac 通過。

## 已驗證：reload 偏離與全部呼叫點調查

正式類繼承 Object，完整欄位表為 `_log`、`_weaponProcs`、`nativeDefinitions`，沒有 `_instance`。正式 reload 全部指令如下：

```text
  public void reload();
    descriptor: ()V
    Code:
       0: aload_0
       1: getfield      #28                 // Field nativeDefinitions:Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;
       4: invokestatic  #42                 // Method local/lito381/server/NativeDeItemDefinitionsScope.requireLoading:(Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;)V
       7: new           #2                  // class local/lito381/server/NativeDeItemWeaponProcTable
      10: dup
      11: invokespecial #247                // Method "<init>":()V
      14: putstatic     #251                // Field _instance:Llocal/lito381/server/NativeDeItemWeaponProcTable;
      17: getstatic     #251                // Field _instance:Llocal/lito381/server/NativeDeItemWeaponProcTable;
      20: invokevirtual #253                // Method load:()V
      23: return
```

本輪 javap 顯示 offset 0–11 的操作與順序保持一致；offset 14 起是 POP、NEW NoSuchFieldError、DUP、LDC "_instance"、INVOKESPECIAL 建構錯誤、ATHROW。`load()` 不再放在不可達尾端。semantic-diff 的完整方法差異如下（其他方法與欄位均通過比較）：

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

**正式 JAR 中所有呼叫 `NativeDeItemWeaponProcTable.reload()V` 的地方：空集合。** 掃描 2,554 個 class，包含方法指令、子類 owner、LDC Handle、invokedynamic bootstrap／參數、遞迴 ConstantDynamic；直接呼叫 0、handle 0。另掃描字串常數：沒有精確 `reload` 字串，沒有包含 `NativeDeItemWeaponProcTable` 的字串。這項字串檢查僅是反射線索檢查，不能排除外部輸入或動態組合名稱。

正向載入證據：正式 `NativeDeItemDefinitionsResources.lambda$read$0(Data)` 在 offset 51 選入 WeaponProcTable、66–67 呼叫 `invokeLoad(Object)`；後者 offset 4 是固定字串 `load`，10 呼叫 `Class.getMethod`、18 `Method.invoke`。因此資料表正常載入使用 `load()`。`reload()` 本體對自己的 `<init>`／`load` 呼叫不是外部呼叫 reload 的證據。

**推測／界線**：依上述正式 JAR 靜態證據，正常啟動與遊玩不會進入這個 reload；不把未知的外部程式或動態反射納入已證明範圍。未發現正常流程會觸發此潛伏 bug；本輪 `needs:claude` 是為 private 繼承編譯阻擋。根因調查歸 #184，不另開 Issue。

`requireLoading` 仍可先丟 `IllegalStateException`：current context 不存在、data 不同、loading=false 或 SQL context 缺少時，尚未到缺欄位位置。因此 A-land 的 NoSuchFieldError 測試必須建立有效 loading context；不能宣稱任何狀態下呼叫 reload 都會得到相同錯誤。

## 已驗證：診斷編譯與 104 項完整清單檢查

為確認其餘工作，不沿用失敗編譯留下的零散 class，而是另以全新 `ruling-partial/target/` 編譯明確排除 ConstructionState 的 48 份 Java。產物恰為 103 entry。AsmFinal scanned=103、converted=1；額外 ASM tree 稽核確認只變更 ItemTable 六個 FINAL 旗標並移除註解，其餘內容相同。每個 JAR entry 均核對等於本輪 class bytes。

| 驗證 | 結果 |
|---|---|
| 49 份原清單編譯 | exit 1；private 覆寫可見性衝突 |
| 48 份獨立診斷編譯 | exit 0；103 class |
| 103 entry 比較 | 86 SAME／16 SAME_PATHS／1 DEVIATION；DIFF／ONLY_LEFT／ONLY_RIGHT 均 0 |
| 原始 104 entry 比較 | 86 SAME／16 SAME_PATHS／1 DEVIATION／1 ONLY_RIGHT；DIFF 0 |
| 完整清單 missingLeft | local/lito381/server/NativeDeNpcConstructionState |
| 完整清單 extraLeft | 空 |
| bridge | 使用 #203 正式工具；本輪缺少 ConstructionState class，沒有新的 176 bridge 產物可驗收 |

全部核准清單先與本批精確取交集：access-widen 的 `NativeDeNpcVitalState#getX()I`、`getY()I`，以及新增的 `NativeDeItemWeaponProcTable#reload()V method-diff`；effects、layout、metadata-exempt、extra-methods、exceptions-exempt 的交集均空，未傳入空清單。使用 `--left-classpath reference/base-0.6.71.jar` 僅解析依賴，沒有補入缺失的 ConstructionState 比較 entry。

## 未驗證什麼／給下一棒的提示

1. **Claude 主控**：處理 private 與祖先 package 同簽名方法的 Java 編譯衝突；本輪已附真實 javac、正式 javap 與所有呼叫端，不需要重裁 SAME_PATHS 或 bridge。不要由 CLI exit 0、DIFF=0 或 CI 綠推斷整批完成，missing=1 仍是阻擋。
2. **本機執行器**：取得新裁定後在 task/73 重編 49 檔、重驗完整 104 entry，包含 sourceChat$startChat 的呼叫指令與 176 個 bridge。48 檔診斷不可代替此驗收。
3. **#78 A-land**：補 NPC 刪除時 `NativeDeChatJobs.retire`、`remaining.cancelChat` 都被呼叫的測試；以及有效 loading context 下 reload 拋 `NoSuchFieldError("_instance")` 的測試，保留前置守門行為。落地保留 AsmFinal 步驟與 A03 WorldLog.debug。
4. 未跑單元測試、Maven 回歸、應用類別執行或真機驗收；依派工留待 A-land。未啟動伺服器／登入器／遊戲、未連 DB、未讀機密設定、未碰封存、硬連結或客戶端。

## 產出路徑與可重現證據

追蹤產出：本批 staged 兩份修正 Java、`docs/combat-merge/deviations.txt`／`deviations.md`、本報告。分支 `task/73`；既有 [PR #101](https://github.com/chaoren0418-arch/lito-launcher-server/pull/101)。

本機不追蹤證據均在 worktree 的 `scratch/issue-73/`：`ruling-full/commands.json`／`a02-javac.log`；`ruling-partial/commands.json`、`receipt.json`、`conversion.json`、`semantic.json`／`.md`、`full-104-semantic.json`／`.md`、`full-104-command.json`、`formal-*.log`、`staged-NativeDeItemWeaponProcTable.log`、`audit-six.log`、`target/a02.jar`。所有生成 class／JAR 只在 target 子目錄，未入 git。

| 證據 | SHA-256 |
|---|---|
| lito伺服器/LiTo381-Server/reference/base-0.6.71.jar | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| scratch/issue-73/ruling-partial/target/a02.jar | `00d500a741a763699e0bd0da15f2f7499315b17d34a4c26707fd8562d7270744` |
| scratch/issue-73/ruling-partial/semantic.json | `c52c29ef05db74a8867353ec261a8ea10e776a97806545cd31cd1ace0498aa5d` |
| scratch/issue-73/ruling-partial/full-104-semantic.json | `107d81a042ad66977bdbb537175dfcaa0af0e774d723f2b3ad341b7f048b3fd8` |
| scratch/issue-73/ruling-partial/formal-scan.log | `33725e653ba3e46698e9fe5ee098496507f3601772ff6d39e49d739722f197f3` |
| scratch/issue-73/ruling-partial/receipt.json | `4728d650061403790ff01e30307c3641aa06c71ba973ef1d738d27fbc7fdb09f` |

以下是本輪實際的 48 檔診斷腳本（存成 scratch/issue-73/verify_ruling_partial.py）。從 worktree 根目錄以 Python 3.12 執行，參數為未使用的輸出資料夾名稱。**要重現 49 檔失敗，移除緊接 batch 選取後的兩行 ConstructionState 過濾**，其餘不變；原實跑使用 verify_current.py，完整 49 檔命令已保留在 ruling-full/commands.json。

```powershell
& 'C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe' -B -X utf8 scratch/issue-73/verify_ruling_partial.py <全新輸出名稱>
```

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
batch['classes']=[n for n in batch['classes'] if n!='NativeDeNpcConstructionState']
batch['verify_entries']=[e for e in batch['verify_entries'] if not e.startswith('local/lito381/server/NativeDeNpcConstructionState')]
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
print(json.dumps(result['counts']))
for c in result['classes']:
    if c['status'] not in ('SAME','SAME_EFFECTS'): print(c['name'],c['status'],json.dumps(c.get('firstDiff',{}),ensure_ascii=False))
```

對產生的同一診斷 JAR，將 semantic 命令的 `--classes` 換為 **未過濾的** A02.verify_entries（104 行），另外指定 JSON／Markdown 輸出，即重現 full-104 的 missing=1。實際完整命令保存在 full-104-command.json。

全 JAR 呼叫掃描程式（scratch/issue-73/ScanRuling.java）；以相同 ASM、asm-tree 9.7 classpath 編譯至 target/tool，再用 `java -cp <target/tool;asm;asm-tree> ScanRuling <reference/base-0.6.71.jar>`，只讀 class tree，不載入應用類別：

```java
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public class ScanRuling {
  static final String T="local/lito381/server/NativeDeItemWeaponProcTable";
  static Map<String,ClassNode> classes=new TreeMap<>();
  static int reloadCalls, reloadHandles;
  static boolean inherits(String owner) {
    Set<String> seen=new HashSet<>();
    while(owner!=null && seen.add(owner)) {
      if(owner.equals(T)) return true;
      ClassNode c=classes.get(owner); if(c==null)return false; owner=c.superName;
    }
    return false;
  }
  static void constant(Object o,String at) {
    if(o instanceof Handle h) {
      if(inherits(h.getOwner()) && h.getName().equals("reload") && h.getDesc().equals("()V")) {
        reloadHandles++;System.out.println("RELOAD_HANDLE "+at+" "+h);
      }
      if(h.getName().equals("cancelNpcChatTimer"))System.out.println("CANCEL_HANDLE "+at+" "+h);
    } else if(o instanceof ConstantDynamic d) {
      constant(d.getBootstrapMethod(),at+" condy-bootstrap");
      for(int i=0;i<d.getBootstrapMethodArgumentCount();i++)constant(d.getBootstrapMethodArgument(i),at+" condy-arg"+i);
    } else if(o instanceof String s && (s.equals("reload") || s.contains("NativeDeItemWeaponProcTable"))) {
      System.out.println("REFLECTION_STRING "+at+" "+s);
    }
  }
  public static void main(String[] args)throws Exception {
    try(ZipFile z=new ZipFile(args[0])) {
      for(ZipEntry e:Collections.list(z.entries()))if(e.getName().endsWith(".class")) {
        ClassNode c=new ClassNode();new ClassReader(z.getInputStream(e)).accept(c,0);classes.put(c.name,c);
      }
    }
    for(ClassNode c:classes.values()) {
      for(FieldNode f:c.fields)constant(f.value,c.name+"#field:"+f.name);
      for(MethodNode m:c.methods) {
        int n=0;
        for(AbstractInsnNode i:m.instructions) {
          if(i.getOpcode()<0)continue;
          String at=c.name+"#"+m.name+m.desc+" insn="+n++;
          if(i instanceof MethodInsnNode x) {
            if(inherits(x.owner) && x.name.equals("reload") && x.desc.equals("()V")) {
              reloadCalls++;System.out.println("RELOAD_CALL "+at+" opcode="+x.getOpcode()+" "+x.owner+"#"+x.name+x.desc);
            }
            if(x.name.equals("cancelNpcChatTimer"))System.out.println("CANCEL_CALL "+at+" opcode="+x.getOpcode()+" "+x.owner+"#"+x.name+x.desc);
            if(x.owner.equals(T))System.out.println("TABLE_USE "+at+" "+x.name+x.desc);
          } else if(i instanceof LdcInsnNode x)constant(x.cst,at);
          else if(i instanceof InvokeDynamicInsnNode x) {
            constant(x.bsm,at+" bootstrap");for(Object a:x.bsmArgs)constant(a,at+" bootstrap-arg");
          }
        }
      }
    }
    System.out.println("SUMMARY classes="+classes.size()+" reloadCalls="+reloadCalls+" reloadHandles="+reloadHandles);
  }
}
```

掃描原始輸出：

```text
CANCEL_CALL local/lito381/server/NativeDeDeletionState#sourceDeleteNpc()V insn=231 opcode=182 local/lito381/server/NativeDeDeletionState#cancelNpcChatTimer()V
CANCEL_CALL local/lito381/server/NativeDeDeletionState#sourceAbortFailedWorldRegistration()V insn=44 opcode=182 local/lito381/server/NativeDeDeletionState#cancelNpcChatTimer()V
TABLE_USE local/lito381/server/NativeDeItemWeaponProcTable#reload()V insn=5 <init>()V
TABLE_USE local/lito381/server/NativeDeItemWeaponProcTable#reload()V insn=8 load()V
CANCEL_CALL local/lito381/server/NativeDeNpcConstructionState#sourceChat$startChat(I)V insn=58 opcode=182 local/lito381/server/NativeDeNpcConstructionState#cancelNpcChatTimer()V
SUMMARY classes=2554 reloadCalls=0 reloadHandles=0
```

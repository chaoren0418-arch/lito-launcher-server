# Issue #72 — A01 正式 0.6.71 暫存：新版工具重驗

## 結論

2026-11-05，在既有 `task/72` worktree 從 `c460ce9` 續修，合入 `origin/main @ 450178d`（含 #108、#135、#153、#166）。49 份 staged Java 直接以 Java 17 編譯，185 個 entry 全數存在且沒有額外 entry。

**182 SAME／3 DIFF／0 SAME_EFFECTS／0 DEVIATION／0 ONLY_LEFT／0 ONLY_RIGHT**，BYTE_SAME=18。**驗收尚未通過，不可進 A-land 或部署**。類別 DEVIATION=0 不表示沒有核准方法：Instance 的基本建構子及新增 private 建構子各列方法級 DEVIATION，但該類仍有其他 DIFF。

本輪可修正的分支、呼叫 owner 與求值順序已處理；剩餘 C 類問題須由 Claude 主控裁定。不是 GitHub Actions 帳務問題，也不再等待已合併的 #135／#153／#166。前輪 152 SAME／33 DIFF 是舊工具與舊來源的結果，不當作本輪最終結果。

依據：[建構子裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/72#issuecomment-6646156935)、[#62 通用規則](https://github.com/chaoren0418-arch/lito-launcher-server/issues/62)、`docs/design/T020-combat-merge.md` §7。

## 做了什麼

- `NativeDeInstanceState`：四個 Name overload 使用 `this(construction, NativeDeInstanceNames.name(construction, name), nativeDeNpcTemplate)`，在父建構子前求值 Name；共用一個 private 建構子，super 返回後寫 construction、執行共用初始化。公開 overload 保留原來各自的旗標寫入與 startNpc 順序。
- `NativeDeInstanceState`：修正 set_deName、下一次動作時間及 AI timer token 的先求值後寫欄位順序；setDead 內的 sourceNpcSetDead 指向正式宣告 owner；修正 isBusy、setNpcSpeed、checkSpawnLocation 正反分支及 Collection.iterator 呼叫。去掉多餘的 monitor 區域別名，保留同步與 try 範圍。
- 35 個 `NativeDeLiveAiScope.call` adapter 改用明確捕獲 actor，使 javac 產生與正式描述子相符的 static lambda；新增的區域暫存仍被比較器判 DIFF，未豁免。巢狀排程 lambda 仍有 H7 instance 與 H6 static 形狀差，見完整附錄。
- `NativeDeDeletionState.sourceDeleteNpc`：重複刪除／世界移除失敗改回正式版的 early return、成對 NPC 的 id 不符用 continue、保留可見物件集合暫存，releasePairedCorpseClaim 指向 NativeDeNpcVitalState。整個 entry 現為 SAME。
- `NativeDeClientViewer.sendPackets`：依正式 operand stack 順序先準備接收者、方法名、描述子與參數陣列，再做 requirePublication，最後送出。整個 entry 現為 SAME，不需 effects 例外。
- `deviations.txt`／`deviations.md` 只追加同一裁定核准的 private 建構子；原五筆 `ctor-prologue-order` 保留。未新增 effects 核准。

本輪手動修改只有上述 3 份 A01 staged Java、兩份 deviation 文件與本報告。主線同步帶入既有依賴；相對 origin/main 的交付差異仍限 A01 暫存與核准文件。未自行修改 src/main、pom.xml、docs/REPORT.md 或比較工具。

## 已驗證：審查點名的方法

| Entry／方法 | 最新結果 |
|---|---|
| NativeDeInstanceScene.publish | 整類 SAME |
| NativeDeItemApplication.install | 整類 SAME |
| NativeDeItemApplication$Owner.refreshFields | 整類 SAME |
| NativeDeItemDefinitionsResources.freeze | 整類 SAME |
| NativeDeCharacterFields$CombatSkillDerived.compute | 分支／呼叫次數已一致；只剩五個 GETSTATIC 的欄位解析差異，見下節 |
| NativeDeDeletionState.sourceDeleteNpc | 整類 SAME，使用 access-widen 清單 |
| NativeDeClientViewer.sendPackets | 整類 SAME，無 effects 豁免 |

已核准放寬存取的精確方法：`local/lito381/server/NativeDeDeletionState#get_showId()I`，staged public、正式 package-private。PR 描述亦須保留此識別。

## 已驗證：父建構子鏈與 construction

對 SHA 固定正式 JAR 用 ASM 讀取全部 2,554 個 class header，沿 superName 搜尋直接／間接子類：**NativeDeInstanceState 子類共 0 個，完整清單為 []**。不是只查 Java 原始碼，也未把內部類名稱當子類。

完整鏈（12 層，含 Object）：

```text
NativeDeNpcConstructionState
→ NativeDeNpcTemplateState
→ NativeDePerceptionState
→ NativeDeDeletionDeState
→ NativeDeDeletionState
→ NativeDeCorpseState
→ NativeDeResurrectableState
→ NativeDeVitalState
→ NativeDeNpcVitalState
→ NativeDeCharacterFields
→ NativeDeObjectFields
→ java.lang.Object
```

掃描這條鏈的全部 18 個應用程式建構子（包含其他 overload，範圍較實際呼叫鏈保守）。其中可能派送到 Instance 的直接虛擬呼叫只有以下六種；其餘如 Snapshot.clientType、Objects.requireNonNull、集合／AtomicLong 建構均不以 Instance 作該方法的 receiver。

| 呼叫來源 | 虛擬方法 | Instance 的實際實作 |
|---|---|---|
| NpcConstructionState | setNameId(String) | NpcTemplateState.setNameId |
| NpcConstructionState（2 處） | setStatus(int) | CharacterFields.setStatus |
| NpcConstructionState | setMoveSpeed(int) | CharacterFields.setMoveSpeed |
| NpcConstructionState | setDead(boolean) | InstanceState.setDead |
| NpcConstructionState | setreSpawn(boolean) | NpcConstructionState.setreSpawn |
| NpcConstructionState、NpcTemplateState | setting_template(Template) | NpcTemplateState.setting_template |

同一 ASM 腳本再沿父鏈方法呼叫與 invokedynamic implementation handle 展開，虛擬呼叫以 Instance 實際類型解析；保守包含 95 個本類／祖先方法，**沒有 construction GETFIELD**。因此不只看 setDead 的外層：其 lambda 讀 isDead／_deathGeneration，呼叫 sourceNpcSetDead／withVitalMutation；setting_template 的 sourceSettingTemplate 及其虛擬 setter、getName 路徑也包含在附錄清單。

正式 Instance 自身讀 construction 的方法只有 set_emblem(Emblem)、broadcastPacketAll(Packet)、set_deName(Name)，均不在上述父建構子可達本類方法集合。正式 JAR 外部另有 InstanceInitialization、InstanceNames.services、InstanceScope.log 三個讀取點，完整列於附錄；它們不是這六個虛擬方法的覆寫實作。Object.<init> 只有 return，不呼叫虛擬方法。

**證明界線**：這是指定正式 JAR 的建構子／虛擬派送與本類呼叫圖靜態證據，滿足主控要求的「沒有被覆寫成讀 construction」條件；不宣稱涵蓋外部插件、反射、任意服務回呼或執行期排程競態。圖中外部呼叫也完整保留；例如 sourceSettingTemplate 可呼叫 NpcExecutor／WorkTimer，未把父建構子說成沒有副作用。實際執行期安全性仍由 #78 測試。

四個公開 Name 建構子的第一次建構呼叫都在 `NativeDeInstanceNames.name` 返回之後；新增 private 建構子描述子如下：

```text
local/lito381/server/NativeDeInstanceState#<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V
```

## 剩餘差異與主控裁定需求

| Entry | 完整剩餘問題與界線 |
|---|---|
| NativeDeCharacterFields | 前輪來源已有 SKU1、SKU2、SKD1、SKD2、SKD3 五個空 HashMap 宣告及 clinit 初始化；正式類與父類沒有宣告它們。本輪未追加或認可這個補法。空 map 不能冒充正式技能資料，仍為阻擋合併的 C 類。請指定真正欄位來源，或裁定應保留正式 NoSuchFieldError 的 Java 表達／刻意偏離。 |
| NativeDeCharacterFields$CombatSkillDerived | 正式 compute 仍 GETSTATIC 引用上述不存在的欄位。#166 正確把正式 owner 標 unresolved，staged 為已宣告欄位，所以 5 個引用均 DIFF；其餘指令一致。不能靠移除 classpath 或放寬 field owner 規則掩蓋。 |
| NativeDeInstanceState | javac 產生 169 個正式缺少的 public bridge；新增 private 建構子已核准 DEVIATION，四個公開 Name overload 的委派仍不是工具可展開的 ctor-prologue-order；35 個 static lambda adapter 的 actor 暫存仍 DIFF；排程中巢狀 lambda 的 instance/static handle 亦不同。所有差異收錄於附錄。 |

對 35 個 adapter 使用 scratch 診斷清單實測 `--effects-methods`：**35 個全部 DIFF**，第一個摘要差異為額外 ASTORE。未符合 #62 B 類的 SAME_EFFECTS 條件，故沒有追加 effects-approved.txt，更沒有把診斷清單當正式核准清單。一般驗收將共用清單裁切至 A01；目前沒有可套用的 effects 條目，工具拒絕空清單，所以略去該空旗標。`--access-widen` 與 `--deviations` 均使用裁切後的正式清單。

請 Claude 主控處理技能表欄位、public bridge、constructor delegation 與 lambda 暫存比較界線；不得直接把整類加入豁免。未宣稱三個 DIFF 都已證明等價。

## 怎麼驗證與重現

Microsoft JDK 17.0.17（`C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot`），Python 3.12。編譯期 stub 由 repo `compile-stubs` 的 9 份來源重建，僅供 javac，未打入 A01 JAR。直接編譯、打包，沒有以正式 class 覆蓋或做 class 後處理。

兩個乾淨輸出目錄獨立編譯，JAR bytes 完全相同。所有產物只在此 worktree 的 target／scratch，沒有寫原始專案資源；未啟動伺服器、登入器、遊戲或 DB，未讀機密設定／封存。

以下腳本存成 `scratch/issue-72/final-reproduce.py`，於 repo 根目錄執行。ASM asm／asm-tree 使用既有 Maven cache；asm-analysis 依工具 build-support.sh 的固定版本下載與 SHA-256 校驗：

```powershell
New-Item -ItemType Directory -Force scratch/issue-72/target/dependency | Out-Null
Invoke-WebRequest -UseBasicParsing -Uri https://repo.maven.apache.org/maven2/org/ow2/asm/asm-analysis/9.7/asm-analysis-9.7.jar -OutFile scratch/issue-72/target/dependency/asm-analysis-9.7.jar
if ((Get-FileHash scratch/issue-72/target/dependency/asm-analysis-9.7.jar).Hash -ne '7BC6BCBC21379948A0C8C467FB0F864206E5B818F6BC0B546872F5C9F941556F') { throw 'ASM checksum mismatch' }
& 'C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe' -B -X utf8 scratch/issue-72/final-reproduce.py
```

腳本內現況斷言是 182／3；`ACCEPTANCE False` 明確表示未通過本任務驗收，CLI exit 0 只表示報告產生成功。

```python
from pathlib import Path
import subprocess, json, zipfile, hashlib
root=Path(__file__).resolve().parents[2]
s=root/'lito伺服器/LiTo381-Server'; w=root/'scratch/issue-72'
j=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
batch=next(b for b in json.loads((root/'docs/combat-merge/a-batches.json').read_text(encoding='utf8'))['batches'] if b['id']=='A01')
st=s/'merge-inputs/formal-0.6.71/staged/local/lito381/server'
asm=';'.join(str(Path('D:/dev-cache/maven/repository/org/ow2/asm')/n/'9.7'/f'{n}-9.7.jar') for n in ['asm','asm-tree'])+';'+str(w/'target/dependency/asm-analysis-9.7.jar')
tc=s/'tools/semantic-diff/target/classes'
def run(args,name):
    r=subprocess.run([str(x) for x in args],cwd=root,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    (w/name).write_bytes(r.stdout)
    print(name,'exit',r.returncode)
    assert r.returncode==0, r.stdout
run([j/'javac.exe','--release','17','-encoding','UTF-8','-d',s/'target/legacy-stubs',*sorted((s/'compile-stubs').rglob('*.java'))],'final-stubs.log')
run([j/'javac.exe','--release','17','-encoding','UTF-8','-cp',asm,'-d',tc,*sorted((s/'tools/semantic-diff/src/main/java').rglob('*.java'))],'final-tool.log')
out=w/'target/final-reproduce'; out.mkdir(parents=True,exist_ok=True)
assert out.resolve().is_relative_to(w.resolve())
for p in out.rglob('*.class'): p.unlink()
run([j/'javac.exe','--release','17','-encoding','UTF-8','-cp',f'{s}/reference/base-0.6.71.jar;{s}/target/legacy-stubs;{s}/lib/*','-d',out,*[st/(n+'.java') for n in batch['classes']]],'final-javac.log')
jar=w/'target/final-reproduce.jar'
with zipfile.ZipFile(jar,'w') as z:
    for p in sorted(out.rglob('*.class')):
        z.writestr(zipfile.ZipInfo(p.relative_to(out).as_posix(),(2026,1,1,0,0,0)),p.read_bytes())
with zipfile.ZipFile(jar) as z: actual=set(z.namelist())
expected=set(batch['verify_entries'])
assert actual==expected,(expected-actual,actual-expected)
assert not any(n.startswith('com/lineage/') for n in actual)
entries=w/'verify_entries.txt'; entries.write_bytes(('\n'.join(batch['verify_entries'])+'\n').encode())
flags=[]
for flag,file in [('effects-methods','effects-approved.txt'),('access-widen','access-widen-approved.txt'),('deviations','deviations.txt')]:
    lines=[]
    for line in (root/'docs/combat-merge'/file).read_text(encoding='utf-8-sig').splitlines():
        key=line.split(' #')[0].strip()
        if not key or key.startswith('#'): continue
        if key.split()[-1].split('#')[0]+'.class' in expected: lines.append(line)
    if lines:
        path=w/file; path.write_bytes(('\n'.join(lines)+'\n').encode());flags+=['--'+flag,path]
run([j/'java.exe','-Dfile.encoding=UTF-8','-cp',f'{tc};{asm}','local.lito381.semanticdiff.SemanticDiff','--left',jar,'--right',s/'reference/base-0.6.71.jar','--left-classpath',s/'reference/base-0.6.71.jar','--classes',entries,*flags,'--json',w/'final-reproduce.json','--md',w/'final-reproduce.md'],'final-semantic.log')
r=json.loads((w/'final-reproduce.json').read_text(encoding='utf8'))
assert r['classSelection']=={'missingLeft':[],'extraLeft':[]}
assert r['counts']['SAME']==182 and r['counts']['DIFF']==3 # 現況再現；不是驗收通過。
assert jar.read_bytes()==(w/'target/a01.jar').read_bytes()
print(json.dumps(r['counts']));print('sha256',hashlib.sha256(jar.read_bytes()).hexdigest())
print('ACCEPTANCE',all(c['status'] in {'SAME','SAME_EFFECTS','DEVIATION'} for c in r['classes']))
```

### 原始最終輸出

```text
final-stubs.log exit 0
final-tool.log exit 0
final-javac.log exit 0
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.

SUMMARY SAME=182 SAME_EFFECTS=0 SAME_LAYOUT=0 DEVIATION=0 DIFF=3 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=18 ALLOW_METHODS=0
wrote D:\lineage開發\3-lito開發\scratch\wt\task-72\scratch\issue-72\final-reproduce.json
wrote D:\lineage開發\3-lito開發\scratch\wt\task-72\scratch\issue-72\final-reproduce.md
ACCEPTANCE False
```

### 輸入與輸出雜湊

| 檔案 | SHA-256 |
|---|---|
| `lito伺服器/LiTo381-Server/reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `scratch/issue-72/target/a01.jar` | `6b0f198773388779c8ea56d39836b33bc55716eff5858febc878d1e030760894` |
| `scratch/issue-72/target/final-reproduce.jar` | `6b0f198773388779c8ea56d39836b33bc55716eff5858febc878d1e030760894` |
| `scratch/issue-72/verify_entries.txt` | `8c883895819b752ae9c9b70b2cef771ce7f44bc23cacfc25c19c197109c79499` |
| `docs/combat-merge/deviations.txt` | `ac6dd4e6de736cd1424f6395502ec8920e39a6e1c0bef2208c472bab05a9702f` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/ClassComparer.java` | `33be9bec93e1deb3ceefd01ea5b7415688e1327b26f27d4468a59dd01fdd3179` |
| `lito伺服器/LiTo381-Server/tools/semantic-diff/src/main/java/local/lito381/semanticdiff/FourthNormalizations.java` | `f2994f27816d427575b1d65d7e33502dd0006b49938a46d5e1633396e3e0a0ce` |

## 未驗證／推測

- 未做單元或實機回歸；本批未改 src/main，依任務在 A-land 統一執行。
- 「actor 暫存與正式直接載入 this 等價」是來源與指令觀察，不是現有比較器的驗證結果；35 個 effects 診斷均未通過。
- 正式技能欄位解析失敗是靜態已驗證；未執行 compute 重現 NoSuchFieldError，也不推測真正資料來源。
- 本輪尚未取得新提交的 GitHub CI 結果；歷史 #158 帳務上限不作程式碼失敗或通過證據。

## 產出路徑／給下一棒的提示

- A01 staged：`lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/`。
- 核准紀錄：`docs/combat-merge/deviations.txt`／`deviations.md`；完整報告為本檔。
- 本機可重跑的原始收據：`scratch/issue-72/final-reproduce.{json,md}`、`final-*.log`、`constructor-audit.txt`、`left/`、`right/`、`diff-index.json`；生成產物不進 git，必要證據已內嵌本報告。
- Claude 主控先裁定上列 C 類與工具表達差異，再由本機執行器重驗全部 185 entry；本批未達完成，不交 A-land 當作已通過。
- A-land #78 必須補兩個已裁定測試：(a) owner services 非 NativeDeWorldContext 時，Name 建構丟 IllegalArgumentException，且無外部登錄／排程；(b) 正常建構後 construction 非 null，set_deName、broadcastPacketAll 可用。另保留各 overload 旗標組合的驗證。
- PR #99 保留 `local/lito381/server/NativeDeDeletionState#get_showId()I` 存取放寬資訊；數字以本報告為準。

## 附錄 A：185 entry 完整結果

| Entry（省略 local/lito381/server/） | 狀態 |
|---|---|
| `NativeDeActorState` | SAME |
| `NativeDeActorState$1` | SAME |
| `NativeDeActorState$Effect` | SAME |
| `NativeDeActorState$Lease` | SAME |
| `NativeDeActorState$Profile` | SAME |
| `NativeDeActorState$Snapshot` | SAME |
| `NativeDeActorState$Transition` | SAME |
| `NativeDeAiState` | SAME |
| `NativeDeAiState$1` | SAME |
| `NativeDeAiState$2` | SAME |
| `NativeDeAiState$Effect` | SAME |
| `NativeDeAiState$Facts` | SAME |
| `NativeDeAiState$Holder` | SAME |
| `NativeDeAiState$Kind` | SAME |
| `NativeDeAiState$Lease` | SAME |
| `NativeDeAiState$Op` | SAME |
| `NativeDeAiState$Request` | SAME |
| `NativeDeAiState$Result` | SAME |
| `NativeDeAiState$View` | SAME |
| `NativeDeAiTimeline` | SAME |
| `NativeDeAiTimeline$Port` | SAME |
| `NativeDeAiTimeline$State` | SAME |
| `NativeDeAiTimerProtocol` | SAME |
| `NativeDeAiTimerProtocol$Failure` | SAME |
| `NativeDeAiTimerProtocol$Port` | SAME |
| `NativeDeAiTimers` | SAME |
| `NativeDeAiTimers$1` | SAME |
| `NativeDeAiTimers$2` | SAME |
| `NativeDeAiTimers$Credit` | SAME |
| `NativeDeAiTimers$Entry` | SAME |
| `NativeDeAiTimers$Live` | SAME |
| `NativeDeAiTimers$Registry` | SAME |
| `NativeDeAiTimers$Snapshot` | SAME |
| `NativeDeAiTimers$Work` | SAME |
| `NativeDeAiWorkerProtocol` | SAME |
| `NativeDeAiWorkerProtocol$Port` | SAME |
| `NativeDeAiWorkers` | SAME |
| `NativeDeAiWorkers$1` | SAME |
| `NativeDeAiWorkers$Snapshot` | SAME |
| `NativeDeAiWorkers$Task` | SAME |
| `NativeDeAiWorkers$Ticket` | SAME |
| `NativeDeCachedFactoryServices` | SAME |
| `NativeDeCharacterActor` | SAME |
| `NativeDeCharacterControls` | SAME |
| `NativeDeCharacterFields` | DIFF |
| `NativeDeCharacterFields$CombatSkillDerived` | DIFF |
| `NativeDeCharacterFields$CurrentHpMutation` | SAME |
| `NativeDeCharacterFields$CurrentMpMutation` | SAME |
| `NativeDeCharacterFields$DamageMutationReceipt` | SAME |
| `NativeDeCharacterFields$ErrorLog` | SAME |
| `NativeDeCharacterFields$MpDebitMutation` | SAME |
| `NativeDeCharacterFields$SkillEffectSnapshot` | SAME |
| `NativeDeCharacterFields$Snapshot` | SAME |
| `NativeDeCharacterFields$ViewerHpState` | SAME |
| `NativeDeChatBroadcast` | SAME |
| `NativeDeChatChat` | SAME |
| `NativeDeChatChatGlobal` | SAME |
| `NativeDeChatChatShouting` | SAME |
| `NativeDeChatJobs` | SAME |
| `NativeDeChatJobs$Owner` | SAME |
| `NativeDeChatJobs$Ticket` | SAME |
| `NativeDeChatResources` | SAME |
| `NativeDeChatResources$Snapshot` | SAME |
| `NativeDeChatScope` | SAME |
| `NativeDeChatScope$Clock` | SAME |
| `NativeDeChatScope$Data` | SAME |
| `NativeDeChatScope$Logs` | SAME |
| `NativeDeChatScope$Pool` | SAME |
| `NativeDeChatScope$Pool$ShutdownDiscardable` | SAME |
| `NativeDeChatTable` | SAME |
| `NativeDeChatTimer` | SAME |
| `NativeDeChatTimer$RunSequenceTask` | SAME |
| `NativeDeChatWire` | SAME |
| `NativeDeClientViewer` | SAME |
| `NativeDeControlCatalog` | SAME |
| `NativeDeControlCatalog$Abilities` | SAME |
| `NativeDeControlCatalog$Catalog` | SAME |
| `NativeDeControlCatalog$Definition` | SAME |
| `NativeDeControlCatalog$Names` | SAME |
| `NativeDeControlCatalog$Reporter` | SAME |
| `NativeDeControlCatalog$Settings` | SAME |
| `NativeDeCorpseCore` | SAME |
| `NativeDeCorpseCore$Npc` | SAME |
| `NativeDeCorpseCore$Port` | SAME |
| `NativeDeCorpseCore$Snapshot` | SAME |
| `NativeDeCorpseCore$Task` | SAME |
| `NativeDeCorpseTimers` | SAME |
| `NativeDeCorpseTimers$Deletion` | SAME |
| `NativeDeCorpseTimers$Job` | SAME |
| `NativeDeCorpseTimers$Owner` | SAME |
| `NativeDeCorpseTimers$Owner$1` | SAME |
| `NativeDeCorpseTimers$Snapshot` | SAME |
| `NativeDeCorpseTimers$Ticket` | SAME |
| `NativeDeDeletionDeState` | SAME |
| `NativeDeDeletionOwner` | SAME |
| `NativeDeDeletionPorts` | SAME |
| `NativeDeDeletionPorts$Boss` | SAME |
| `NativeDeDeletionPorts$DeWorld` | SAME |
| `NativeDeDeletionPorts$DeadTimer` | SAME |
| `NativeDeDeletionPorts$DeleteTimer` | SAME |
| `NativeDeDeletionPorts$DigestTimer` | SAME |
| `NativeDeDeletionPorts$Doll` | SAME |
| `NativeDeDeletionPorts$Hate` | SAME |
| `NativeDeDeletionPorts$HprTimer` | SAME |
| `NativeDeDeletionPorts$Illusory` | SAME |
| `NativeDeDeletionPorts$Inventory` | SAME |
| `NativeDeDeletionPorts$Log` | SAME |
| `NativeDeDeletionPorts$MobGroup` | SAME |
| `NativeDeDeletionPorts$Monster` | SAME |
| `NativeDeDeletionPorts$MprTimer` | SAME |
| `NativeDeDeletionPorts$ObjectRef` | SAME |
| `NativeDeDeletionPorts$OtherList` | SAME |
| `NativeDeDeletionPorts$Packet` | SAME |
| `NativeDeDeletionPorts$Quest` | SAME |
| `NativeDeDeletionPorts$RemoveObject` | SAME |
| `NativeDeDeletionPorts$RestTimer` | SAME |
| `NativeDeDeletionPorts$Services` | SAME |
| `NativeDeDeletionPorts$Spawn` | SAME |
| `NativeDeDeletionPorts$Summon` | SAME |
| `NativeDeDeletionPorts$Template` | SAME |
| `NativeDeDeletionPorts$Viewer` | SAME |
| `NativeDeDeletionPorts$WorkTimer` | SAME |
| `NativeDeDeletionPorts$World` | SAME |
| `NativeDeDeletionScope` | SAME |
| `NativeDeDeletionState` | SAME |
| `NativeDeDeletionVisibility` | SAME |
| `NativeDeDeletionVisibility$1` | SAME |
| `NativeDeDeletionVisibility$Marker` | SAME |
| `NativeDeDeletionVisibility$Phase` | SAME |
| `NativeDeDeletionVisibility$RepairKind` | SAME |
| `NativeDeDeletionVisibility$Snapshot` | SAME |
| `NativeDeEquipment` | SAME |
| `NativeDeEquipment$1` | SAME |
| `NativeDeEquipment$Holder` | SAME |
| `NativeDeEquipment$Lease` | SAME |
| `NativeDeEquipment$Op` | SAME |
| `NativeDeEquipment$Request` | SAME |
| `NativeDeEquipment$Template` | SAME |
| `NativeDeEquipment$Values` | SAME |
| `NativeDeEquipment$View` | SAME |
| `NativeDeFactoryControls` | SAME |
| `NativeDeFactoryControls$1` | SAME |
| `NativeDeFactoryControls$Snapshot` | SAME |
| `NativeDeInitializerKernel` | SAME |
| `NativeDeInitializerKernel$Ability` | SAME |
| `NativeDeInitializerKernel$Actor` | SAME |
| `NativeDeInitializerKernel$Clan` | SAME |
| `NativeDeInitializerKernel$Emblem` | SAME |
| `NativeDeInitializerKernel$Log` | SAME |
| `NativeDeInitializerKernel$Name` | SAME |
| `NativeDeInitializerKernel$Packet` | SAME |
| `NativeDeInitializerKernel$Pet` | SAME |
| `NativeDeInitializerKernel$PetInfo` | SAME |
| `NativeDeInitializerKernel$Services` | SAME |
| `NativeDeInitializerKernel$Setting` | SAME |
| `NativeDeInitializerKernel$Template` | SAME |
| `NativeDeInstanceBroadcast` | SAME |
| `NativeDeInstanceName` | SAME |
| `NativeDeInstanceNames` | SAME |
| `NativeDeInstanceNames$Services` | SAME |
| `NativeDeInstanceScene` | SAME |
| `NativeDeInstanceScene$Visual` | SAME |
| `NativeDeInstanceScope` | SAME |
| `NativeDeInstanceScope$World` | SAME |
| `NativeDeInstanceState` | DIFF |
| `NativeDeInstanceState$DeAiWorkerTask` | SAME |
| `NativeDeInstanceState$DeRepathThrottle` | SAME |
| `NativeDeItemAbility` | SAME |
| `NativeDeItemAbility$AbilityType` | SAME |
| `NativeDeItemApplication` | SAME |
| `NativeDeItemApplication$Owner` | SAME |
| `NativeDeItemBlessStatsTable` | SAME |
| `NativeDeItemCommonEnchantTable` | SAME |
| `NativeDeItemDefinitionsResources` | SAME |
| `NativeDeItemDefinitionsResources$Owner` | SAME |
| `NativeDeItemDefinitionsResources$Snapshot` | SAME |
| `NativeDeItemDefinitionsScope` | SAME |
| `NativeDeItemDefinitionsScope$Context` | SAME |
| `NativeDeItemDefinitionsScope$Data` | SAME |
| `NativeDeItemDefinitionsScope$Database` | SAME |
| `NativeDeItemDefinitionsScope$Logs` | SAME |
| `NativeDeItemDefinitionsScope$TableLog` | SAME |
| `NativeDeItemEnchantBonusTable` | SAME |
| `NativeDeItemEquipProc` | SAME |
| `NativeDeItemEquipProcTable` | SAME |

Instance 方法級核准結果（保留原始 JSON）：

```json
[
  {
    "method": "<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V",
    "status": "DEVIATION",
    "detail": null
  },
  {
    "method": "<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;)V",
    "status": "DEVIATION",
    "detail": null
  },
  {
    "method": "<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V",
    "status": "DIFF",
    "detail": "method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V insn[0]: left=ALOAD 0 right=ALOAD 1"
  },
  {
    "method": "<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V",
    "status": "DIFF",
    "detail": "method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V insn[0]: left=ALOAD 0 right=ALOAD 1"
  },
  {
    "method": "<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V",
    "status": "DIFF",
    "detail": "method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V insn[0]: left=ALOAD 0 right=ALOAD 1"
  },
  {
    "method": "<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V",
    "status": "DIFF",
    "detail": "method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V insn[0]: left=ALOAD 0 right=ALOAD 1"
  }
]
```

## 附錄 B：建構子與虛擬派送的完整 ASM 輸出

```text
SUBCLASSES 0 []
CHAIN [local/lito381/server/NativeDeNpcConstructionState, local/lito381/server/NativeDeNpcTemplateState, local/lito381/server/NativeDePerceptionState, local/lito381/server/NativeDeDeletionDeState, local/lito381/server/NativeDeDeletionState, local/lito381/server/NativeDeCorpseState, local/lito381/server/NativeDeResurrectableState, local/lito381/server/NativeDeVitalState, local/lito381/server/NativeDeNpcVitalState, local/lito381/server/NativeDeCharacterFields, local/lito381/server/NativeDeObjectFields, java/lang/Object]
CTOR local/lito381/server/NativeDeNpcConstructionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V
  CALL 183 local/lito381/server/NativeDeNpcTemplateState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V
  CALL 184 java/util/Objects#requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
  CALL 182 local/lito381/server/NativeDeNpcConstructionState#setNameId(Ljava/lang/String;)V
    DISPATCH local/lito381/server/NativeDeInstanceState -> local/lito381/server/NativeDeNpcTemplateState#setNameId(Ljava/lang/String;)V
  CALL 183 local/lito381/server/NativeDeNpcInventory#<init>(Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;)V
  CALL 183 java/util/concurrent/atomic/AtomicLong#<init>(J)V
  CALL 183 local/lito381/server/NativeDeNpcHateList#<init>()V
  CALL 183 local/lito381/server/NativeDeNpcHateList#<init>()V
  CALL 182 local/lito381/server/NativeDeNpcConstructionState#setStatus(I)V
    DISPATCH local/lito381/server/NativeDeInstanceState -> local/lito381/server/NativeDeCharacterFields#setStatus(I)V
  CALL 182 local/lito381/server/NativeDeNpcConstructionState#setMoveSpeed(I)V
    DISPATCH local/lito381/server/NativeDeInstanceState -> local/lito381/server/NativeDeCharacterFields#setMoveSpeed(I)V
  CALL 182 local/lito381/server/NativeDeNpcConstructionState#setDead(Z)V
    DISPATCH local/lito381/server/NativeDeInstanceState -> local/lito381/server/NativeDeInstanceState#setDead(Z)V
  CALL 182 local/lito381/server/NativeDeNpcConstructionState#setStatus(I)V
    DISPATCH local/lito381/server/NativeDeInstanceState -> local/lito381/server/NativeDeCharacterFields#setStatus(I)V
  CALL 182 local/lito381/server/NativeDeNpcConstructionState#setreSpawn(Z)V
    DISPATCH local/lito381/server/NativeDeInstanceState -> local/lito381/server/NativeDeNpcConstructionState#setreSpawn(Z)V
  CALL 183 local/lito381/server/NativeDeNpcMoveState#<init>(Llocal/lito381/server/NativeDeNpcTemplateState;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;)V
  CALL 182 local/lito381/server/NativeDeNpcConstructionState#setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V
    DISPATCH local/lito381/server/NativeDeInstanceState -> local/lito381/server/NativeDeNpcTemplateState#setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V
CTOR local/lito381/server/NativeDeNpcTemplateState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V
  CALL 183 local/lito381/server/NativeDePerceptionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;)V
  CALL 184 java/util/Objects#requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
  CALL 182 local/lito381/server/NativeDeNpcTemplateState#setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V
    DISPATCH local/lito381/server/NativeDeInstanceState -> local/lito381/server/NativeDeNpcTemplateState#setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V
CTOR local/lito381/server/NativeDePerceptionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;)V
  CALL 183 local/lito381/server/NativeDeDeletionDeState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;)V
  CALL 183 java/util/concurrent/atomic/AtomicLong#<init>(J)V
  CALL 184 java/util/Set#of()Ljava/util/Set;
  CALL 184 java/util/Objects#requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
CTOR local/lito381/server/NativeDeDeletionDeState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;)V
  CALL 183 local/lito381/server/NativeDeDeletionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;)V
CTOR local/lito381/server/NativeDeDeletionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;)V
  CALL 183 local/lito381/server/NativeDeCorpseState#<init>(IILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeCorpseTimers$Owner;)V
  CALL 183 java/util/concurrent/ConcurrentHashMap#<init>()V
  CALL 183 local/lito381/server/NativeDeDeletionVisibility#<init>()V
  CALL 183 java/util/concurrent/atomic/AtomicBoolean#<init>()V
  CALL 183 java/util/concurrent/locks/ReentrantLock#<init>()V
  CALL 183 java/util/concurrent/locks/ReentrantLock#<init>()V
  CALL 183 java/util/ArrayList#<init>()V
  CALL 183 java/util/HashMap#<init>()V
CTOR local/lito381/server/NativeDeDeletionState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;)V
  CALL 183 local/lito381/server/NativeDeCorpseState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeCorpseTimers$Owner;)V
  CALL 183 java/util/concurrent/ConcurrentHashMap#<init>()V
  CALL 183 local/lito381/server/NativeDeDeletionVisibility#<init>()V
  CALL 183 java/util/concurrent/atomic/AtomicBoolean#<init>()V
  CALL 183 java/util/concurrent/locks/ReentrantLock#<init>()V
  CALL 183 java/util/concurrent/locks/ReentrantLock#<init>()V
  CALL 183 java/util/ArrayList#<init>()V
  CALL 183 java/util/HashMap#<init>()V
CTOR local/lito381/server/NativeDeCorpseState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeCorpseTimers$Owner;)V
  CALL 183 local/lito381/server/NativeDeCorpseState$Effects#<init>(Llocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeCorpseTimers$Owner;)V
  CALL 183 local/lito381/server/NativeDeResurrectableState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;)V
  CALL 184 java/util/Objects#requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
CTOR local/lito381/server/NativeDeCorpseState#<init>(IILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeCorpseTimers$Owner;)V
  CALL 183 local/lito381/server/NativeDeCorpseState$Effects#<init>(Llocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeCorpseTimers$Owner;)V
  CALL 183 local/lito381/server/NativeDeResurrectableState#<init>(IILlocal/lito381/server/NativeDeResurrectionEffects;)V
  CALL 184 java/util/Objects#requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
CTOR local/lito381/server/NativeDeResurrectableState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)V
  CALL 183 local/lito381/server/NativeDeResurrectableState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;)V
CTOR local/lito381/server/NativeDeResurrectableState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;)V
  CALL 183 local/lito381/server/NativeDeVitalState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)V
  CALL 184 java/util/Objects#requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
CTOR local/lito381/server/NativeDeResurrectableState#<init>(IILlocal/lito381/server/NativeDeResurrectionEffects;)V
  CALL 183 local/lito381/server/NativeDeVitalState#<init>(II)V
  CALL 184 java/util/Objects#requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
CTOR local/lito381/server/NativeDeVitalState#<init>(II)V
  CALL 183 local/lito381/server/NativeDeNpcVitalState#<init>(II)V
  CALL 183 java/util/concurrent/atomic/AtomicLong#<init>()V
CTOR local/lito381/server/NativeDeVitalState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)V
  CALL 183 local/lito381/server/NativeDeNpcVitalState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)V
  CALL 183 java/util/concurrent/atomic/AtomicLong#<init>()V
CTOR local/lito381/server/NativeDeNpcVitalState#<init>(II)V
  CALL 183 local/lito381/server/NativeDeCharacterFields#<init>(II)V
  CALL 183 local/lito381/server/NativeDeathLifecycleGate#<init>()V
  CALL 183 java/util/concurrent/atomic/AtomicBoolean#<init>()V
CTOR local/lito381/server/NativeDeNpcVitalState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)V
  CALL 183 local/lito381/server/NativeDeCharacterFields#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)V
  CALL 183 local/lito381/server/NativeDeathLifecycleGate#<init>()V
  CALL 183 java/util/concurrent/atomic/AtomicBoolean#<init>()V
CTOR local/lito381/server/NativeDeCharacterFields#<init>(II)V
  CALL 183 local/lito381/server/NativeDeObjectFields#<init>()V
  CALL 183 java/util/concurrent/ConcurrentHashMap#<init>()V
  CALL 183 java/util/concurrent/ConcurrentHashMap#<init>()V
  CALL 183 java/util/concurrent/locks/ReentrantLock#<init>()V
  CALL 183 java/util/concurrent/ConcurrentHashMap#<init>()V
  CALL 183 java/util/concurrent/ConcurrentHashMap#<init>()V
  CALL 183 java/util/concurrent/ConcurrentHashMap#<init>()V
  CALL 183 java/util/concurrent/ConcurrentHashMap#<init>()V
  CALL 183 java/util/concurrent/locks/ReentrantLock#<init>()V
  CALL 183 java/util/concurrent/ConcurrentLinkedQueue#<init>()V
  CALL 183 java/util/concurrent/atomic/AtomicBoolean#<init>()V
  CALL 183 java/util/concurrent/locks/ReentrantLock#<init>()V
  CALL 183 java/util/concurrent/atomic/AtomicReference#<init>()V
  CALL 183 java/util/concurrent/atomic/AtomicLong#<init>(J)V
  CALL 183 java/util/concurrent/ConcurrentHashMap#<init>()V
  CALL 183 java/util/concurrent/CopyOnWriteArrayList#<init>()V
  CALL 183 java/util/concurrent/CopyOnWriteArrayList#<init>()V
  CALL 183 java/util/concurrent/locks/ReentrantLock#<init>()V
CTOR local/lito381/server/NativeDeCharacterFields#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)V
  CALL 182 local/lito381/server/NativeDeCharacterFields$Snapshot#clientType()I
  CALL 183 local/lito381/server/NativeDeCharacterFields#<init>(II)V
  CALL 184 local/lito381/server/NativeDeCharacterFields#fromInitializedNpc(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)Llocal/lito381/server/NativeDeCharacterFields;
CTOR local/lito381/server/NativeDeObjectFields#<init>()V
  CALL 183 java/lang/Object#<init>()V
  CALL 183 local/lito381/server/NativeDeWorldLocation#<init>()V
READER local/lito381/server/NativeDeInstanceInitialization#lambda$init$0(Llocal/lito381/server/NativeDeInstanceState;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V
READER local/lito381/server/NativeDeInstanceNames#services(Llocal/lito381/server/NativeDeInstanceState;)Llocal/lito381/server/NativeDeInitializerKernel$Services;
READER local/lito381/server/NativeDeInstanceScope#log()Llocal/lito381/server/NativeDeInitializerKernel$Log;
READER local/lito381/server/NativeDeInstanceState#set_emblem(Llocal/lito381/server/NativeDeInitializerKernel$Emblem;)V
READER local/lito381/server/NativeDeInstanceState#broadcastPacketAll(Llocal/lito381/server/NativeDeInitializerKernel$Packet;)V
READER local/lito381/server/NativeDeInstanceState#set_deName(Llocal/lito381/server/NativeDeInitializerKernel$Name;)V
REACHABLE_SELF_METHODS 95
  local/lito381/server/NativeDeCharacterFields#<init>(II)V
  local/lito381/server/NativeDeCharacterFields#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)V
  local/lito381/server/NativeDeCharacterFields#addDmgup(I)V
  local/lito381/server/NativeDeCharacterFields#addEarth(I)V
  local/lito381/server/NativeDeCharacterFields#addFire(I)V
  local/lito381/server/NativeDeCharacterFields#addHitup(I)V
  local/lito381/server/NativeDeCharacterFields#addWater(I)V
  local/lito381/server/NativeDeCharacterFields#addWind(I)V
  local/lito381/server/NativeDeCharacterFields#afterCurrentHpMutation(Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation;)V
  local/lito381/server/NativeDeCharacterFields#afterCurrentMpMutation(Llocal/lito381/server/NativeDeCharacterFields$CurrentMpMutation;)V
  local/lito381/server/NativeDeCharacterFields#drainVitalCompletionsAfterActionUnlock()V
  local/lito381/server/NativeDeCharacterFields#enqueueCurrentHpCompletionLocked(Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation;)Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation;
  local/lito381/server/NativeDeCharacterFields#enqueueCurrentMpCompletionLocked(Llocal/lito381/server/NativeDeCharacterFields$CurrentMpMutation;)Llocal/lito381/server/NativeDeCharacterFields$CurrentMpMutation;
  local/lito381/server/NativeDeCharacterFields#ensure(III)I
  local/lito381/server/NativeDeCharacterFields#fromInitializedNpc(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)Llocal/lito381/server/NativeDeCharacterFields;
  local/lito381/server/NativeDeCharacterFields#getId()I
  local/lito381/server/NativeDeCharacterFields#getMinAcLimit()I
  local/lito381/server/NativeDeCharacterFields#isDead()Z
  local/lito381/server/NativeDeCharacterFields#isROClient()Z
  local/lito381/server/NativeDeCharacterFields#lambda$enqueueCurrentHpCompletionLocked$1(Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation;)V
  local/lito381/server/NativeDeCharacterFields#lambda$enqueueCurrentMpCompletionLocked$2(Llocal/lito381/server/NativeDeCharacterFields$CurrentMpMutation;)V
  local/lito381/server/NativeDeCharacterFields#lambda$setCharacterDead$16(Z)V
  local/lito381/server/NativeDeCharacterFields#lambda$setCurrentHpDirect$9(I)V
  local/lito381/server/NativeDeCharacterFields#lambda$setCurrentMpDirect$11(I)Ljava/lang/Integer;
  local/lito381/server/NativeDeCharacterFields#lambda$setMaxHp$12(I)V
  local/lito381/server/NativeDeCharacterFields#lambda$setMaxMp$14(I)V
  local/lito381/server/NativeDeCharacterFields#lambda$setStatus$17(I)V
  local/lito381/server/NativeDeCharacterFields#lambda$withVitalMutation$0(Ljava/lang/Runnable;)Ljava/lang/Object;
  local/lito381/server/NativeDeCharacterFields#recordDamageDeathMutation(ZZ)V
  local/lito381/server/NativeDeCharacterFields#recordDamageHpMutation(II)V
  local/lito381/server/NativeDeCharacterFields#reportCompletionFailure(Ljava/lang/Throwable;)V
  local/lito381/server/NativeDeCharacterFields#setAc(I)V
  local/lito381/server/NativeDeCharacterFields#setBraveSpeed(I)V
  local/lito381/server/NativeDeCharacterFields#setCharacterDead(Z)V
  local/lito381/server/NativeDeCharacterFields#setCon(I)V
  local/lito381/server/NativeDeCharacterFields#setCurrentHpDirect(I)V
  local/lito381/server/NativeDeCharacterFields#setCurrentMpDirect(I)V
  local/lito381/server/NativeDeCharacterFields#setDex(I)V
  local/lito381/server/NativeDeCharacterFields#setExp(J)V
  local/lito381/server/NativeDeCharacterFields#setGfxId(I)V
  local/lito381/server/NativeDeCharacterFields#setInt(I)V
  local/lito381/server/NativeDeCharacterFields#setKarma(I)V
  local/lito381/server/NativeDeCharacterFields#setLawful(I)V
  local/lito381/server/NativeDeCharacterFields#setLevel(I)V
  local/lito381/server/NativeDeCharacterFields#setMaxHp(I)V
  local/lito381/server/NativeDeCharacterFields#setMaxMp(I)V
  local/lito381/server/NativeDeCharacterFields#setMoveSpeed(I)V
  local/lito381/server/NativeDeCharacterFields#setMr(I)V
  local/lito381/server/NativeDeCharacterFields#setName(Ljava/lang/String;)V
  local/lito381/server/NativeDeCharacterFields#setStatus(I)V
  local/lito381/server/NativeDeCharacterFields#setStr(I)V
  local/lito381/server/NativeDeCharacterFields#setTempCharGfx(I)V
  local/lito381/server/NativeDeCharacterFields#setWis(I)V
  local/lito381/server/NativeDeCharacterFields#vitalMutationLock()Ljava/util/concurrent/locks/ReentrantLock;
  local/lito381/server/NativeDeCharacterFields#withVitalMutation(Ljava/lang/Runnable;)V
  local/lito381/server/NativeDeCharacterFields#withVitalMutation(Ljava/util/function/Supplier;)Ljava/lang/Object;
  local/lito381/server/NativeDeCorpseState#<init>(IILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeCorpseTimers$Owner;)V
  local/lito381/server/NativeDeCorpseState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeCorpseTimers$Owner;)V
  local/lito381/server/NativeDeCorpseState#getId()I
  local/lito381/server/NativeDeDeletionDeState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;)V
  local/lito381/server/NativeDeDeletionState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;)V
  local/lito381/server/NativeDeDeletionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;)V
  local/lito381/server/NativeDeInstanceState#lambda$setDead$2(Z)V
  local/lito381/server/NativeDeInstanceState#setDead(Z)V
  local/lito381/server/NativeDeNpcConstructionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V
  local/lito381/server/NativeDeNpcConstructionState#newName(I)Ljava/lang/String;
  local/lito381/server/NativeDeNpcConstructionState#setreSpawn(Z)V
  local/lito381/server/NativeDeNpcTemplateState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V
  local/lito381/server/NativeDeNpcTemplateState#getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;
  local/lito381/server/NativeDeNpcTemplateState#lambda$setting_template$0(Llocal/lito381/server/NativeDeNpcTemplate;)V
  local/lito381/server/NativeDeNpcTemplateState#newGfx(I)I
  local/lito381/server/NativeDeNpcTemplateState#setAgro(Z)V
  local/lito381/server/NativeDeNpcTemplateState#setAgrocoi(Z)V
  local/lito381/server/NativeDeNpcTemplateState#setAgrososc(Z)V
  local/lito381/server/NativeDeNpcTemplateState#setAtkspeed(I)V
  local/lito381/server/NativeDeNpcTemplateState#setGfxidInStatus(I)V
  local/lito381/server/NativeDeNpcTemplateState#setLightSize(I)V
  local/lito381/server/NativeDeNpcTemplateState#setNameId(Ljava/lang/String;)V
  local/lito381/server/NativeDeNpcTemplateState#setPassispeed(I)V
  local/lito381/server/NativeDeNpcTemplateState#setPickupItem(Z)V
  local/lito381/server/NativeDeNpcTemplateState#setTempLawful(I)V
  local/lito381/server/NativeDeNpcTemplateState#setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V
  local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V
  local/lito381/server/NativeDeNpcVitalState#<init>(II)V
  local/lito381/server/NativeDeNpcVitalState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)V
  local/lito381/server/NativeDeNpcVitalState#lambda$setDead$0(Z)V
  local/lito381/server/NativeDeNpcVitalState#setWeaponBreakStack(I)V
  local/lito381/server/NativeDeNpcVitalState#sourceNpcSetDead(Z)V
  local/lito381/server/NativeDeObjectFields#<init>()V
  local/lito381/server/NativeDePerceptionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;)V
  local/lito381/server/NativeDeResurrectableState#<init>(IILlocal/lito381/server/NativeDeResurrectionEffects;)V
  local/lito381/server/NativeDeResurrectableState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)V
  local/lito381/server/NativeDeResurrectableState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;)V
  local/lito381/server/NativeDeVitalState#<init>(II)V
  local/lito381/server/NativeDeVitalState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)V
CALL_GRAPH
local/lito381/server/NativeDeCharacterFields#<init>(II)V -> EXTERNAL java/util/concurrent/ConcurrentHashMap#<init>()V
local/lito381/server/NativeDeCharacterFields#<init>(II)V -> EXTERNAL java/util/concurrent/ConcurrentLinkedQueue#<init>()V
local/lito381/server/NativeDeCharacterFields#<init>(II)V -> EXTERNAL java/util/concurrent/CopyOnWriteArrayList#<init>()V
local/lito381/server/NativeDeCharacterFields#<init>(II)V -> EXTERNAL java/util/concurrent/atomic/AtomicBoolean#<init>()V
local/lito381/server/NativeDeCharacterFields#<init>(II)V -> EXTERNAL java/util/concurrent/atomic/AtomicLong#<init>(J)V
local/lito381/server/NativeDeCharacterFields#<init>(II)V -> EXTERNAL java/util/concurrent/atomic/AtomicReference#<init>()V
local/lito381/server/NativeDeCharacterFields#<init>(II)V -> EXTERNAL java/util/concurrent/locks/ReentrantLock#<init>()V
local/lito381/server/NativeDeCharacterFields#<init>(II)V -> local/lito381/server/NativeDeObjectFields#<init>()V
local/lito381/server/NativeDeCharacterFields#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)V -> EXTERNAL local/lito381/server/NativeDeCharacterFields$Snapshot#clientType()I
local/lito381/server/NativeDeCharacterFields#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)V -> local/lito381/server/NativeDeCharacterFields#<init>(II)V
local/lito381/server/NativeDeCharacterFields#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)V -> local/lito381/server/NativeDeCharacterFields#fromInitializedNpc(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)Llocal/lito381/server/NativeDeCharacterFields;
local/lito381/server/NativeDeCharacterFields#drainVitalCompletionsAfterActionUnlock()V -> EXTERNAL java/lang/Runnable#run()V
local/lito381/server/NativeDeCharacterFields#drainVitalCompletionsAfterActionUnlock()V -> EXTERNAL java/util/ArrayDeque#<init>()V
local/lito381/server/NativeDeCharacterFields#drainVitalCompletionsAfterActionUnlock()V -> EXTERNAL java/util/ArrayDeque#addLast(Ljava/lang/Object;)V
local/lito381/server/NativeDeCharacterFields#drainVitalCompletionsAfterActionUnlock()V -> EXTERNAL java/util/ArrayDeque#isEmpty()Z
local/lito381/server/NativeDeCharacterFields#drainVitalCompletionsAfterActionUnlock()V -> EXTERNAL java/util/ArrayDeque#pollFirst()Ljava/lang/Object;
local/lito381/server/NativeDeCharacterFields#drainVitalCompletionsAfterActionUnlock()V -> EXTERNAL java/util/concurrent/ConcurrentLinkedQueue#isEmpty()Z
local/lito381/server/NativeDeCharacterFields#drainVitalCompletionsAfterActionUnlock()V -> EXTERNAL java/util/concurrent/ConcurrentLinkedQueue#poll()Ljava/lang/Object;
local/lito381/server/NativeDeCharacterFields#drainVitalCompletionsAfterActionUnlock()V -> EXTERNAL java/util/concurrent/atomic/AtomicBoolean#compareAndSet(ZZ)Z
local/lito381/server/NativeDeCharacterFields#drainVitalCompletionsAfterActionUnlock()V -> EXTERNAL java/util/concurrent/atomic/AtomicBoolean#set(Z)V
local/lito381/server/NativeDeCharacterFields#drainVitalCompletionsAfterActionUnlock()V -> EXTERNAL java/util/concurrent/locks/ReentrantLock#isHeldByCurrentThread()Z
local/lito381/server/NativeDeCharacterFields#drainVitalCompletionsAfterActionUnlock()V -> EXTERNAL java/util/concurrent/locks/ReentrantLock#lock()V
local/lito381/server/NativeDeCharacterFields#drainVitalCompletionsAfterActionUnlock()V -> EXTERNAL java/util/concurrent/locks/ReentrantLock#unlock()V
local/lito381/server/NativeDeCharacterFields#drainVitalCompletionsAfterActionUnlock()V -> local/lito381/server/NativeDeCharacterFields#reportCompletionFailure(Ljava/lang/Throwable;)V
local/lito381/server/NativeDeCharacterFields#drainVitalCompletionsAfterActionUnlock()V -> local/lito381/server/NativeDeCharacterFields#vitalMutationLock()Ljava/util/concurrent/locks/ReentrantLock;
local/lito381/server/NativeDeCharacterFields#enqueueCurrentHpCompletionLocked(Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation;)Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation; -> EXTERNAL java/util/concurrent/ConcurrentLinkedQueue#add(Ljava/lang/Object;)Z
local/lito381/server/NativeDeCharacterFields#enqueueCurrentHpCompletionLocked(Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation;)Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation; -> EXTERNAL local/lito381/server/NativeDeCharacterFields$CurrentHpMutation#completionRequired()Z
local/lito381/server/NativeDeCharacterFields#enqueueCurrentHpCompletionLocked(Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation;)Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation; -> LAMBDA local/lito381/server/NativeDeCharacterFields#lambda$enqueueCurrentHpCompletionLocked$1(Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation;)V
local/lito381/server/NativeDeCharacterFields#enqueueCurrentMpCompletionLocked(Llocal/lito381/server/NativeDeCharacterFields$CurrentMpMutation;)Llocal/lito381/server/NativeDeCharacterFields$CurrentMpMutation; -> EXTERNAL java/util/concurrent/ConcurrentLinkedQueue#add(Ljava/lang/Object;)Z
local/lito381/server/NativeDeCharacterFields#enqueueCurrentMpCompletionLocked(Llocal/lito381/server/NativeDeCharacterFields$CurrentMpMutation;)Llocal/lito381/server/NativeDeCharacterFields$CurrentMpMutation; -> EXTERNAL local/lito381/server/NativeDeCharacterFields$CurrentMpMutation#completionRequired()Z
local/lito381/server/NativeDeCharacterFields#enqueueCurrentMpCompletionLocked(Llocal/lito381/server/NativeDeCharacterFields$CurrentMpMutation;)Llocal/lito381/server/NativeDeCharacterFields$CurrentMpMutation; -> LAMBDA local/lito381/server/NativeDeCharacterFields#lambda$enqueueCurrentMpCompletionLocked$2(Llocal/lito381/server/NativeDeCharacterFields$CurrentMpMutation;)V
local/lito381/server/NativeDeCharacterFields#fromInitializedNpc(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)Llocal/lito381/server/NativeDeCharacterFields; -> EXTERNAL java/lang/IllegalArgumentException#<init>(Ljava/lang/String;)V
local/lito381/server/NativeDeCharacterFields#fromInitializedNpc(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)Llocal/lito381/server/NativeDeCharacterFields; -> EXTERNAL java/lang/Number#intValue()I
local/lito381/server/NativeDeCharacterFields#fromInitializedNpc(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)Llocal/lito381/server/NativeDeCharacterFields; -> EXTERNAL java/lang/Number#shortValue()S
local/lito381/server/NativeDeCharacterFields#fromInitializedNpc(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)Llocal/lito381/server/NativeDeCharacterFields; -> EXTERNAL java/lang/Object#getClass()Ljava/lang/Class;
local/lito381/server/NativeDeCharacterFields#fromInitializedNpc(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)Llocal/lito381/server/NativeDeCharacterFields; -> EXTERNAL java/util/Iterator#hasNext()Z
local/lito381/server/NativeDeCharacterFields#fromInitializedNpc(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)Llocal/lito381/server/NativeDeCharacterFields; -> EXTERNAL java/util/Iterator#next()Ljava/lang/Object;
local/lito381/server/NativeDeCharacterFields#fromInitializedNpc(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)Llocal/lito381/server/NativeDeCharacterFields; -> EXTERNAL java/util/Map#entrySet()Ljava/util/Set;
local/lito381/server/NativeDeCharacterFields#fromInitializedNpc(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)Llocal/lito381/server/NativeDeCharacterFields; -> EXTERNAL java/util/Map#get(Ljava/lang/Object;)Ljava/lang/Object;
local/lito381/server/NativeDeCharacterFields#fromInitializedNpc(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)Llocal/lito381/server/NativeDeCharacterFields; -> EXTERNAL java/util/Map#keySet()Ljava/util/Set;
local/lito381/server/NativeDeCharacterFields#fromInitializedNpc(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)Llocal/lito381/server/NativeDeCharacterFields; -> EXTERNAL java/util/Map$Entry#getKey()Ljava/lang/Object;
local/lito381/server/NativeDeCharacterFields#fromInitializedNpc(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)Llocal/lito381/server/NativeDeCharacterFields; -> EXTERNAL java/util/Map$Entry#getValue()Ljava/lang/Object;
local/lito381/server/NativeDeCharacterFields#fromInitializedNpc(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)Llocal/lito381/server/NativeDeCharacterFields; -> EXTERNAL java/util/Objects#requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
local/lito381/server/NativeDeCharacterFields#fromInitializedNpc(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)Llocal/lito381/server/NativeDeCharacterFields; -> EXTERNAL java/util/Set#contains(Ljava/lang/Object;)Z
local/lito381/server/NativeDeCharacterFields#fromInitializedNpc(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)Llocal/lito381/server/NativeDeCharacterFields; -> EXTERNAL java/util/Set#equals(Ljava/lang/Object;)Z
local/lito381/server/NativeDeCharacterFields#fromInitializedNpc(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)Llocal/lito381/server/NativeDeCharacterFields; -> EXTERNAL java/util/Set#iterator()Ljava/util/Iterator;
local/lito381/server/NativeDeCharacterFields#fromInitializedNpc(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)Llocal/lito381/server/NativeDeCharacterFields; -> EXTERNAL java/util/Set#of([Ljava/lang/Object;)Ljava/util/Set;
local/lito381/server/NativeDeCharacterFields#fromInitializedNpc(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)Llocal/lito381/server/NativeDeCharacterFields; -> EXTERNAL local/lito381/server/NativeDeCharacterControls#clientType()I
local/lito381/server/NativeDeCharacterFields#fromInitializedNpc(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)Llocal/lito381/server/NativeDeCharacterFields; -> EXTERNAL local/lito381/server/NativeDeCharacterFields$Snapshot#clientType()I
local/lito381/server/NativeDeCharacterFields#fromInitializedNpc(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)Llocal/lito381/server/NativeDeCharacterFields; -> EXTERNAL local/lito381/server/NativeDeCharacterFields$Snapshot#dead()Z
local/lito381/server/NativeDeCharacterFields#fromInitializedNpc(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)Llocal/lito381/server/NativeDeCharacterFields; -> EXTERNAL local/lito381/server/NativeDeCharacterFields$Snapshot#fields()Ljava/util/Map;
local/lito381/server/NativeDeCharacterFields#fromInitializedNpc(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)Llocal/lito381/server/NativeDeCharacterFields; -> local/lito381/server/NativeDeCharacterFields#<init>(II)V
local/lito381/server/NativeDeCharacterFields#getMinAcLimit()I -> local/lito381/server/NativeDeCharacterFields#isROClient()Z
local/lito381/server/NativeDeCharacterFields#lambda$enqueueCurrentHpCompletionLocked$1(Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation;)V -> local/lito381/server/NativeDeCharacterFields#afterCurrentHpMutation(Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation;)V
local/lito381/server/NativeDeCharacterFields#lambda$enqueueCurrentMpCompletionLocked$2(Llocal/lito381/server/NativeDeCharacterFields$CurrentMpMutation;)V -> local/lito381/server/NativeDeCharacterFields#afterCurrentMpMutation(Llocal/lito381/server/NativeDeCharacterFields$CurrentMpMutation;)V
local/lito381/server/NativeDeCharacterFields#lambda$setCharacterDead$16(Z)V -> local/lito381/server/NativeDeCharacterFields#recordDamageDeathMutation(ZZ)V
local/lito381/server/NativeDeCharacterFields#lambda$setCurrentHpDirect$9(I)V -> local/lito381/server/NativeDeCharacterFields#recordDamageHpMutation(II)V
local/lito381/server/NativeDeCharacterFields#lambda$setCurrentMpDirect$11(I)Ljava/lang/Integer; -> EXTERNAL java/lang/Integer#valueOf(I)Ljava/lang/Integer;
local/lito381/server/NativeDeCharacterFields#lambda$setMaxHp$12(I)V -> EXTERNAL java/lang/Math#min(II)I
local/lito381/server/NativeDeCharacterFields#lambda$setMaxHp$12(I)V -> EXTERNAL local/lito381/server/NativeDeCharacterFields$CurrentHpMutation#<init>(II)V
local/lito381/server/NativeDeCharacterFields#lambda$setMaxHp$12(I)V -> local/lito381/server/NativeDeCharacterFields#enqueueCurrentHpCompletionLocked(Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation;)Llocal/lito381/server/NativeDeCharacterFields$CurrentHpMutation;
local/lito381/server/NativeDeCharacterFields#lambda$setMaxHp$12(I)V -> local/lito381/server/NativeDeCharacterFields#ensure(III)I
local/lito381/server/NativeDeCharacterFields#lambda$setMaxHp$12(I)V -> local/lito381/server/NativeDeCharacterFields#isROClient()Z
local/lito381/server/NativeDeCharacterFields#lambda$setMaxMp$14(I)V -> EXTERNAL java/lang/Math#min(II)I
local/lito381/server/NativeDeCharacterFields#lambda$setMaxMp$14(I)V -> EXTERNAL local/lito381/server/NativeDeCharacterFields$CurrentMpMutation#<init>(II)V
local/lito381/server/NativeDeCharacterFields#lambda$setMaxMp$14(I)V -> local/lito381/server/NativeDeCharacterFields#enqueueCurrentMpCompletionLocked(Llocal/lito381/server/NativeDeCharacterFields$CurrentMpMutation;)Llocal/lito381/server/NativeDeCharacterFields$CurrentMpMutation;
local/lito381/server/NativeDeCharacterFields#lambda$setMaxMp$14(I)V -> local/lito381/server/NativeDeCharacterFields#ensure(III)I
local/lito381/server/NativeDeCharacterFields#lambda$setMaxMp$14(I)V -> local/lito381/server/NativeDeCharacterFields#isROClient()Z
local/lito381/server/NativeDeCharacterFields#lambda$withVitalMutation$0(Ljava/lang/Runnable;)Ljava/lang/Object; -> EXTERNAL java/lang/Runnable#run()V
local/lito381/server/NativeDeCharacterFields#recordDamageDeathMutation(ZZ)V -> EXTERNAL java/lang/ThreadLocal#get()Ljava/lang/Object;
local/lito381/server/NativeDeCharacterFields#recordDamageDeathMutation(ZZ)V -> EXTERNAL java/util/ArrayDeque#iterator()Ljava/util/Iterator;
local/lito381/server/NativeDeCharacterFields#recordDamageDeathMutation(ZZ)V -> EXTERNAL java/util/Iterator#hasNext()Z
local/lito381/server/NativeDeCharacterFields#recordDamageDeathMutation(ZZ)V -> EXTERNAL java/util/Iterator#next()Ljava/lang/Object;
local/lito381/server/NativeDeCharacterFields#recordDamageDeathMutation(ZZ)V -> EXTERNAL local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt#recordDead(ZZI)V
local/lito381/server/NativeDeCharacterFields#recordDamageHpMutation(II)V -> EXTERNAL java/lang/ThreadLocal#get()Ljava/lang/Object;
local/lito381/server/NativeDeCharacterFields#recordDamageHpMutation(II)V -> EXTERNAL java/util/ArrayDeque#iterator()Ljava/util/Iterator;
local/lito381/server/NativeDeCharacterFields#recordDamageHpMutation(II)V -> EXTERNAL java/util/Iterator#hasNext()Z
local/lito381/server/NativeDeCharacterFields#recordDamageHpMutation(II)V -> EXTERNAL java/util/Iterator#next()Ljava/lang/Object;
local/lito381/server/NativeDeCharacterFields#recordDamageHpMutation(II)V -> EXTERNAL local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt#recordHp(II)V
local/lito381/server/NativeDeCharacterFields#reportCompletionFailure(Ljava/lang/Throwable;)V -> EXTERNAL java/lang/Class#getName()Ljava/lang/String;
local/lito381/server/NativeDeCharacterFields#reportCompletionFailure(Ljava/lang/Throwable;)V -> EXTERNAL java/lang/Integer#valueOf(I)Ljava/lang/Integer;
local/lito381/server/NativeDeCharacterFields#reportCompletionFailure(Ljava/lang/Throwable;)V -> EXTERNAL java/lang/Object#getClass()Ljava/lang/Class;
local/lito381/server/NativeDeCharacterFields#reportCompletionFailure(Ljava/lang/Throwable;)V -> EXTERNAL local/lito381/server/NativeDeCharacterFields$ErrorLog#error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
local/lito381/server/NativeDeCharacterFields#reportCompletionFailure(Ljava/lang/Throwable;)V -> local/lito381/server/NativeDeCorpseState#getId()I
local/lito381/server/NativeDeCharacterFields#setAc(I)V -> local/lito381/server/NativeDeCharacterFields#ensure(III)I
local/lito381/server/NativeDeCharacterFields#setAc(I)V -> local/lito381/server/NativeDeCharacterFields#getMinAcLimit()I
local/lito381/server/NativeDeCharacterFields#setCharacterDead(Z)V -> LAMBDA local/lito381/server/NativeDeCharacterFields#lambda$setCharacterDead$16(Z)V
local/lito381/server/NativeDeCharacterFields#setCharacterDead(Z)V -> local/lito381/server/NativeDeCharacterFields#withVitalMutation(Ljava/lang/Runnable;)V
local/lito381/server/NativeDeCharacterFields#setCon(I)V -> local/lito381/server/NativeDeCharacterFields#ensure(III)I
local/lito381/server/NativeDeCharacterFields#setCurrentHpDirect(I)V -> LAMBDA local/lito381/server/NativeDeCharacterFields#lambda$setCurrentHpDirect$9(I)V
local/lito381/server/NativeDeCharacterFields#setCurrentHpDirect(I)V -> local/lito381/server/NativeDeCharacterFields#withVitalMutation(Ljava/lang/Runnable;)V
local/lito381/server/NativeDeCharacterFields#setCurrentMpDirect(I)V -> LAMBDA local/lito381/server/NativeDeCharacterFields#lambda$setCurrentMpDirect$11(I)Ljava/lang/Integer;
local/lito381/server/NativeDeCharacterFields#setCurrentMpDirect(I)V -> local/lito381/server/NativeDeCharacterFields#withVitalMutation(Ljava/util/function/Supplier;)Ljava/lang/Object;
local/lito381/server/NativeDeCharacterFields#setDex(I)V -> local/lito381/server/NativeDeCharacterFields#ensure(III)I
local/lito381/server/NativeDeCharacterFields#setInt(I)V -> local/lito381/server/NativeDeCharacterFields#ensure(III)I
local/lito381/server/NativeDeCharacterFields#setMaxHp(I)V -> LAMBDA local/lito381/server/NativeDeCharacterFields#lambda$setMaxHp$12(I)V
local/lito381/server/NativeDeCharacterFields#setMaxHp(I)V -> local/lito381/server/NativeDeCharacterFields#withVitalMutation(Ljava/lang/Runnable;)V
local/lito381/server/NativeDeCharacterFields#setMaxMp(I)V -> LAMBDA local/lito381/server/NativeDeCharacterFields#lambda$setMaxMp$14(I)V
local/lito381/server/NativeDeCharacterFields#setMaxMp(I)V -> local/lito381/server/NativeDeCharacterFields#withVitalMutation(Ljava/lang/Runnable;)V
local/lito381/server/NativeDeCharacterFields#setStatus(I)V -> LAMBDA local/lito381/server/NativeDeCharacterFields#lambda$setStatus$17(I)V
local/lito381/server/NativeDeCharacterFields#setStatus(I)V -> local/lito381/server/NativeDeCharacterFields#withVitalMutation(Ljava/lang/Runnable;)V
local/lito381/server/NativeDeCharacterFields#setStr(I)V -> local/lito381/server/NativeDeCharacterFields#ensure(III)I
local/lito381/server/NativeDeCharacterFields#setWis(I)V -> local/lito381/server/NativeDeCharacterFields#ensure(III)I
local/lito381/server/NativeDeCharacterFields#withVitalMutation(Ljava/lang/Runnable;)V -> LAMBDA local/lito381/server/NativeDeCharacterFields#lambda$withVitalMutation$0(Ljava/lang/Runnable;)Ljava/lang/Object;
local/lito381/server/NativeDeCharacterFields#withVitalMutation(Ljava/lang/Runnable;)V -> local/lito381/server/NativeDeCharacterFields#withVitalMutation(Ljava/util/function/Supplier;)Ljava/lang/Object;
local/lito381/server/NativeDeCharacterFields#withVitalMutation(Ljava/util/function/Supplier;)Ljava/lang/Object; -> EXTERNAL java/lang/NullPointerException#<init>(Ljava/lang/String;)V
local/lito381/server/NativeDeCharacterFields#withVitalMutation(Ljava/util/function/Supplier;)Ljava/lang/Object; -> EXTERNAL java/util/concurrent/locks/ReentrantLock#isHeldByCurrentThread()Z
local/lito381/server/NativeDeCharacterFields#withVitalMutation(Ljava/util/function/Supplier;)Ljava/lang/Object; -> EXTERNAL java/util/concurrent/locks/ReentrantLock#lock()V
local/lito381/server/NativeDeCharacterFields#withVitalMutation(Ljava/util/function/Supplier;)Ljava/lang/Object; -> EXTERNAL java/util/concurrent/locks/ReentrantLock#unlock()V
local/lito381/server/NativeDeCharacterFields#withVitalMutation(Ljava/util/function/Supplier;)Ljava/lang/Object; -> EXTERNAL java/util/function/Supplier#get()Ljava/lang/Object;
local/lito381/server/NativeDeCharacterFields#withVitalMutation(Ljava/util/function/Supplier;)Ljava/lang/Object; -> local/lito381/server/NativeDeCharacterFields#drainVitalCompletionsAfterActionUnlock()V
local/lito381/server/NativeDeCharacterFields#withVitalMutation(Ljava/util/function/Supplier;)Ljava/lang/Object; -> local/lito381/server/NativeDeCharacterFields#vitalMutationLock()Ljava/util/concurrent/locks/ReentrantLock;
local/lito381/server/NativeDeCorpseState#<init>(IILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeCorpseTimers$Owner;)V -> EXTERNAL java/util/Objects#requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
local/lito381/server/NativeDeCorpseState#<init>(IILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeCorpseTimers$Owner;)V -> EXTERNAL local/lito381/server/NativeDeCorpseState$Effects#<init>(Llocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeCorpseTimers$Owner;)V
local/lito381/server/NativeDeCorpseState#<init>(IILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeCorpseTimers$Owner;)V -> local/lito381/server/NativeDeResurrectableState#<init>(IILlocal/lito381/server/NativeDeResurrectionEffects;)V
local/lito381/server/NativeDeCorpseState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeCorpseTimers$Owner;)V -> EXTERNAL java/util/Objects#requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
local/lito381/server/NativeDeCorpseState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeCorpseTimers$Owner;)V -> EXTERNAL local/lito381/server/NativeDeCorpseState$Effects#<init>(Llocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeCorpseTimers$Owner;)V
local/lito381/server/NativeDeCorpseState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeCorpseTimers$Owner;)V -> local/lito381/server/NativeDeResurrectableState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;)V
local/lito381/server/NativeDeCorpseState#getId()I -> local/lito381/server/NativeDeCharacterFields#getId()I
local/lito381/server/NativeDeDeletionDeState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;)V -> local/lito381/server/NativeDeDeletionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;)V
local/lito381/server/NativeDeDeletionState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;)V -> EXTERNAL java/util/ArrayList#<init>()V
local/lito381/server/NativeDeDeletionState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;)V -> EXTERNAL java/util/HashMap#<init>()V
local/lito381/server/NativeDeDeletionState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;)V -> EXTERNAL java/util/concurrent/ConcurrentHashMap#<init>()V
local/lito381/server/NativeDeDeletionState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;)V -> EXTERNAL java/util/concurrent/atomic/AtomicBoolean#<init>()V
local/lito381/server/NativeDeDeletionState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;)V -> EXTERNAL java/util/concurrent/locks/ReentrantLock#<init>()V
local/lito381/server/NativeDeDeletionState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;)V -> EXTERNAL local/lito381/server/NativeDeDeletionVisibility#<init>()V
local/lito381/server/NativeDeDeletionState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;)V -> local/lito381/server/NativeDeCorpseState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeCorpseTimers$Owner;)V
local/lito381/server/NativeDeDeletionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;)V -> EXTERNAL java/util/ArrayList#<init>()V
local/lito381/server/NativeDeDeletionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;)V -> EXTERNAL java/util/HashMap#<init>()V
local/lito381/server/NativeDeDeletionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;)V -> EXTERNAL java/util/concurrent/ConcurrentHashMap#<init>()V
local/lito381/server/NativeDeDeletionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;)V -> EXTERNAL java/util/concurrent/atomic/AtomicBoolean#<init>()V
local/lito381/server/NativeDeDeletionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;)V -> EXTERNAL java/util/concurrent/locks/ReentrantLock#<init>()V
local/lito381/server/NativeDeDeletionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;)V -> EXTERNAL local/lito381/server/NativeDeDeletionVisibility#<init>()V
local/lito381/server/NativeDeDeletionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;)V -> local/lito381/server/NativeDeCorpseState#<init>(IILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeCorpseTimers$Owner;)V
local/lito381/server/NativeDeInstanceState#lambda$setDead$2(Z)V -> EXTERNAL java/util/concurrent/atomic/AtomicLong#incrementAndGet()J
local/lito381/server/NativeDeInstanceState#lambda$setDead$2(Z)V -> local/lito381/server/NativeDeCharacterFields#isDead()Z
local/lito381/server/NativeDeInstanceState#lambda$setDead$2(Z)V -> local/lito381/server/NativeDeNpcVitalState#sourceNpcSetDead(Z)V
local/lito381/server/NativeDeInstanceState#setDead(Z)V -> LAMBDA local/lito381/server/NativeDeInstanceState#lambda$setDead$2(Z)V
local/lito381/server/NativeDeInstanceState#setDead(Z)V -> local/lito381/server/NativeDeCharacterFields#withVitalMutation(Ljava/lang/Runnable;)V
local/lito381/server/NativeDeNpcConstructionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL java/util/Objects#requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
local/lito381/server/NativeDeNpcConstructionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL java/util/concurrent/atomic/AtomicLong#<init>(J)V
local/lito381/server/NativeDeNpcConstructionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcHateList#<init>()V
local/lito381/server/NativeDeNpcConstructionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcInventory#<init>(Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;)V
local/lito381/server/NativeDeNpcConstructionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcMoveState#<init>(Llocal/lito381/server/NativeDeNpcTemplateState;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;)V
local/lito381/server/NativeDeNpcConstructionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#setMoveSpeed(I)V
local/lito381/server/NativeDeNpcConstructionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#setStatus(I)V
local/lito381/server/NativeDeNpcConstructionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeInstanceState#setDead(Z)V
local/lito381/server/NativeDeNpcConstructionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeNpcConstructionState#setreSpawn(Z)V
local/lito381/server/NativeDeNpcConstructionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeNpcTemplateState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V
local/lito381/server/NativeDeNpcConstructionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeNpcTemplateState#setNameId(Ljava/lang/String;)V
local/lito381/server/NativeDeNpcConstructionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeNpcTemplateState#setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V
local/lito381/server/NativeDeNpcTemplateState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL java/util/Objects#requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
local/lito381/server/NativeDeNpcTemplateState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeNpcTemplateState#setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V
local/lito381/server/NativeDeNpcTemplateState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDePerceptionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;)V
local/lito381/server/NativeDeNpcTemplateState#lambda$setting_template$0(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V
local/lito381/server/NativeDeNpcTemplateState#newGfx(I)I -> EXTERNAL java/util/Random#nextInt(I)I
local/lito381/server/NativeDeNpcTemplateState#newGfx(I)I -> EXTERNAL local/lito381/server/NativeDeNpcTemplateScope#random()Ljava/util/Random;
local/lito381/server/NativeDeNpcTemplateState#setGfxidInStatus(I)V -> local/lito381/server/NativeDeCharacterFields#setStatus(I)V
local/lito381/server/NativeDeNpcTemplateState#setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplateScope#run(Llocal/lito381/server/NativeDeNpcTemplateState;Ljava/lang/Runnable;)V
local/lito381/server/NativeDeNpcTemplateState#setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V -> LAMBDA local/lito381/server/NativeDeNpcTemplateState#lambda$setting_template$0(Llocal/lito381/server/NativeDeNpcTemplate;)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL java/lang/Integer#valueOf(I)Ljava/lang/Integer;
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL java/lang/Math#min(DD)D
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL java/util/Random#nextInt(I)I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcExecutor#spawn(Llocal/lito381/server/NativeDeNpcTemplateState;)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcExecutor#work(Llocal/lito381/server/NativeDeNpcTemplateState;)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcExecutor#workTime()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcMobSkillUse#<init>(Llocal/lito381/server/NativeDeNpcTemplateState;)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#action()Z
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#attack()Z
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#death()Z
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#getKarma()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#getLightSize()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#getNpcExecutor()Llocal/lito381/server/NativeDeNpcExecutor;
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#get_ac()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#get_atkspeed()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#get_con()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#get_dex()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#get_exp()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#get_gfxid()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#get_hp()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#get_int()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#get_lawful()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#get_level()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#get_mp()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#get_mr()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#get_name()Ljava/lang/String;
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#get_nameid()Ljava/lang/String;
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#get_npcId()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#get_passispeed()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#get_randomac()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#get_randomexp()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#get_randomhp()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#get_randomlawful()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#get_randomlevel()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#get_randommp()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#get_str()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#get_weakAttr()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#get_wis()I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#is_agro()Z
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#is_agrocoi()Z
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#is_agrososc()Z
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#is_bravespeed()Z
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#is_picupitem()Z
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#spawn()Z
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#talk()Z
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplate#work()Z
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplatePorts$WorkTimer#put(Llocal/lito381/server/NativeDeNpcTemplateState;Ljava/lang/Integer;)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> EXTERNAL local/lito381/server/NativeDeNpcTemplateScope#random()Ljava/util/Random;
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#addDmgup(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#addEarth(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#addFire(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#addHitup(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#addWater(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#addWind(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#setAc(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#setBraveSpeed(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#setCon(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#setCurrentHpDirect(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#setCurrentMpDirect(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#setDex(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#setExp(J)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#setGfxId(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#setInt(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#setKarma(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#setLawful(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#setLevel(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#setMaxHp(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#setMaxMp(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#setMr(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#setName(Ljava/lang/String;)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#setStr(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#setTempCharGfx(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeCharacterFields#setWis(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeNpcConstructionState#newName(I)Ljava/lang/String;
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeNpcTemplateState#getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeNpcTemplateState#newGfx(I)I
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeNpcTemplateState#setAgro(Z)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeNpcTemplateState#setAgrocoi(Z)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeNpcTemplateState#setAgrososc(Z)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeNpcTemplateState#setAtkspeed(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeNpcTemplateState#setGfxidInStatus(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeNpcTemplateState#setLightSize(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeNpcTemplateState#setNameId(Ljava/lang/String;)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeNpcTemplateState#setPassispeed(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeNpcTemplateState#setPickupItem(Z)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeNpcTemplateState#setTempLawful(I)V
local/lito381/server/NativeDeNpcTemplateState#sourceSettingTemplate(Llocal/lito381/server/NativeDeNpcTemplate;)V -> local/lito381/server/NativeDeNpcVitalState#setWeaponBreakStack(I)V
local/lito381/server/NativeDeNpcVitalState#<init>(II)V -> EXTERNAL java/util/concurrent/atomic/AtomicBoolean#<init>()V
local/lito381/server/NativeDeNpcVitalState#<init>(II)V -> EXTERNAL local/lito381/server/NativeDeathLifecycleGate#<init>()V
local/lito381/server/NativeDeNpcVitalState#<init>(II)V -> local/lito381/server/NativeDeCharacterFields#<init>(II)V
local/lito381/server/NativeDeNpcVitalState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)V -> EXTERNAL java/util/concurrent/atomic/AtomicBoolean#<init>()V
local/lito381/server/NativeDeNpcVitalState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)V -> EXTERNAL local/lito381/server/NativeDeathLifecycleGate#<init>()V
local/lito381/server/NativeDeNpcVitalState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)V -> local/lito381/server/NativeDeCharacterFields#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)V
local/lito381/server/NativeDeNpcVitalState#lambda$setDead$0(Z)V -> EXTERNAL local/lito381/server/NativeDeathLifecycleGate#isManaged()Z
local/lito381/server/NativeDeNpcVitalState#lambda$setDead$0(Z)V -> EXTERNAL local/lito381/server/NativeDeathLifecycleGate#reviveDirect(Llocal/lito381/server/NativeDeathLifecycleGate$State;)Z
local/lito381/server/NativeDeNpcVitalState#lambda$setDead$0(Z)V -> EXTERNAL local/lito381/server/NativeDeathLifecycleGate#snapshot()Llocal/lito381/server/NativeDeathLifecycleGate$State;
local/lito381/server/NativeDeNpcVitalState#lambda$setDead$0(Z)V -> EXTERNAL local/lito381/server/NativeDeathLifecycleGate$Phase#ordinal()I
local/lito381/server/NativeDeNpcVitalState#lambda$setDead$0(Z)V -> EXTERNAL local/lito381/server/NativeDeathLifecycleGate$State#phase()Llocal/lito381/server/NativeDeathLifecycleGate$Phase;
local/lito381/server/NativeDeNpcVitalState#lambda$setDead$0(Z)V -> local/lito381/server/NativeDeCharacterFields#setCharacterDead(Z)V
local/lito381/server/NativeDeNpcVitalState#lambda$setDead$0(Z)V -> local/lito381/server/NativeDeNpcVitalState#setWeaponBreakStack(I)V
local/lito381/server/NativeDeNpcVitalState#setWeaponBreakStack(I)V -> EXTERNAL java/lang/Math#max(II)I
local/lito381/server/NativeDeNpcVitalState#setWeaponBreakStack(I)V -> EXTERNAL java/lang/Math#min(II)I
local/lito381/server/NativeDeNpcVitalState#sourceNpcSetDead(Z)V -> LAMBDA local/lito381/server/NativeDeNpcVitalState#lambda$setDead$0(Z)V
local/lito381/server/NativeDeNpcVitalState#sourceNpcSetDead(Z)V -> local/lito381/server/NativeDeCharacterFields#withVitalMutation(Ljava/lang/Runnable;)V
local/lito381/server/NativeDeObjectFields#<init>()V -> EXTERNAL java/lang/Object#<init>()V
local/lito381/server/NativeDeObjectFields#<init>()V -> EXTERNAL local/lito381/server/NativeDeWorldLocation#<init>()V
local/lito381/server/NativeDePerceptionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;)V -> EXTERNAL java/util/Objects#requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
local/lito381/server/NativeDePerceptionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;)V -> EXTERNAL java/util/Set#of()Ljava/util/Set;
local/lito381/server/NativeDePerceptionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;)V -> EXTERNAL java/util/concurrent/atomic/AtomicLong#<init>(J)V
local/lito381/server/NativeDePerceptionState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;)V -> local/lito381/server/NativeDeDeletionDeState#<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;)V
local/lito381/server/NativeDeResurrectableState#<init>(IILlocal/lito381/server/NativeDeResurrectionEffects;)V -> EXTERNAL java/util/Objects#requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
local/lito381/server/NativeDeResurrectableState#<init>(IILlocal/lito381/server/NativeDeResurrectionEffects;)V -> local/lito381/server/NativeDeVitalState#<init>(II)V
local/lito381/server/NativeDeResurrectableState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)V -> local/lito381/server/NativeDeResurrectableState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;)V
local/lito381/server/NativeDeResurrectableState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;)V -> EXTERNAL java/util/Objects#requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
local/lito381/server/NativeDeResurrectableState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;Llocal/lito381/server/NativeDeResurrectionEffects;)V -> local/lito381/server/NativeDeVitalState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)V
local/lito381/server/NativeDeVitalState#<init>(II)V -> EXTERNAL java/util/concurrent/atomic/AtomicLong#<init>()V
local/lito381/server/NativeDeVitalState#<init>(II)V -> local/lito381/server/NativeDeNpcVitalState#<init>(II)V
local/lito381/server/NativeDeVitalState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)V -> EXTERNAL java/util/concurrent/atomic/AtomicLong#<init>()V
local/lito381/server/NativeDeVitalState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)V -> local/lito381/server/NativeDeNpcVitalState#<init>(ILlocal/lito381/server/NativeDeCharacterFields$Snapshot;)V
```

### 證據產生程式

以 ASM 9.7 編譯此程式（`javac --release 17 -cp <asm;asm-tree> -d scratch/issue-72/target/diagnostics scratch/issue-72/ConstructorAudit.java`），執行 `java -cp <diagnostics;asm;asm-tree> ConstructorAudit lito伺服器/LiTo381-Server/reference/base-0.6.71.jar`。只解析 class bytes，不載入應用類別。

```java
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public class ConstructorAudit {
 static final String ROOT="local/lito381/server/NativeDeInstanceState";
 static Map<String,ClassNode> classes=new TreeMap<>();
 static String resolve(String owner,String name,String desc){
  for(ClassNode c=classes.get(owner);c!=null;c=classes.get(c.superName))
   for(MethodNode m:c.methods)if(m.name.equals(name)&&m.desc.equals(desc))return c.name+"#"+name+desc;
  return null;
 }
 static MethodNode method(String key){int h=key.indexOf('#');for(MethodNode m:classes.get(key.substring(0,h)).methods)if((m.name+m.desc).equals(key.substring(h+1)))return m;throw new IllegalStateException(key);}
 static boolean subtype(String n,String parent){for(ClassNode c=classes.get(n);c!=null;c=classes.get(c.superName))if(c.name.equals(parent))return true;return false;}
 public static void main(String[] args)throws Exception{
  try(ZipFile z=new ZipFile(args[0])){for(var e:Collections.list(z.entries()))if(e.getName().endsWith(".class")){ClassNode c=new ClassNode();new ClassReader(z.getInputStream(e).readAllBytes()).accept(c,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);classes.put(c.name,c);}}
  List<String> descendants=classes.keySet().stream().filter(n->!n.equals(ROOT)&&subtype(n,ROOT)).toList();
  System.out.println("SUBCLASSES "+descendants.size()+" "+descendants);
  Set<String> chain=new LinkedHashSet<>();for(ClassNode c=classes.get(classes.get(ROOT).superName);c!=null;c=classes.get(c.superName))chain.add(c.name);chain.add("java/lang/Object");
  System.out.println("CHAIN "+chain);
  List<String> receivers=new ArrayList<>(descendants);receivers.add(ROOT);
  Set<String> roots=new TreeSet<>();
  for(String n:chain){ClassNode c=classes.get(n);if(c==null)continue;for(MethodNode m:c.methods)if(m.name.equals("<init>")){
   String key=n+"#"+m.name+m.desc;roots.add(key);System.out.println("CTOR "+key);
   for(AbstractInsnNode ins:m.instructions)if(ins instanceof MethodInsnNode call){
    System.out.println("  CALL "+call.getOpcode()+" "+call.owner+"#"+call.name+call.desc);
    if((call.getOpcode()==182||call.getOpcode()==185)&&subtype(ROOT,call.owner))for(String r:receivers)System.out.println("    DISPATCH "+r+" -> "+resolve(r,call.name,call.desc));
   }
  }}
  for(var c:classes.values())for(var m:c.methods)for(var ins:m.instructions)if(ins instanceof FieldInsnNode f&&f.name.equals("construction")&&f.owner.equals(ROOT)&&f.getOpcode()==180)System.out.println("READER "+c.name+"#"+m.name+m.desc);
  Set<String> seen=new TreeSet<>();Deque<String> q=new ArrayDeque<>(roots);Set<String> calls=new TreeSet<>();
  while(!q.isEmpty()){
   String key=q.remove();if(!seen.add(key))continue;
   for(var ins:method(key).instructions){
    if(ins instanceof FieldInsnNode f&&f.name.equals("construction")&&f.owner.equals(ROOT)&&f.getOpcode()==180)System.out.println("REACHABLE_READ "+key);
    if(ins instanceof MethodInsnNode call){
     if(subtype(ROOT,call.owner)){
      for(String receiver:receivers){String target=resolve((call.getOpcode()==182||call.getOpcode()==185)?receiver:call.owner,call.name,call.desc);if(target!=null){q.add(target);calls.add(key+" -> "+target);}}
     }else calls.add(key+" -> EXTERNAL "+call.owner+"#"+call.name+call.desc);
    }
    if(ins instanceof InvokeDynamicInsnNode d)for(Object a:d.bsmArgs)if(a instanceof Handle h&&subtype(ROOT,h.getOwner())){String target=resolve(h.getOwner(),h.getName(),h.getDesc());if(target!=null){q.add(target);calls.add(key+" -> LAMBDA "+target);}}
   }
  }
  System.out.println("REACHABLE_SELF_METHODS "+seen.size());for(String s:seen)System.out.println("  "+s);
  System.out.println("CALL_GRAPH");for(String s:calls)System.out.println(s);
 }
}
```

## 附錄 C：三個 DIFF entry 的完整正規化差異

方向為 staged → formal；保留每個差異 hunk，無省略號截斷，包含所有多出 bridge、lambda 與 private 建構子。此診斷列印完整指令差異，尚未套用方法級豁免／transport 證明；最終判定仍以附錄 A 與 CLI JSON 為準。

### NativeDeCharacterFields

```diff
--- staged/NativeDeCharacterFields
+++ formal/NativeDeCharacterFields
@@ -24,24 +24,4 @@
 field MAX_HP_MP_STANDARD:I value=Integer:32767
 field MAX_HP_MP_STANDARD:I annotations=|||
-field SKD1:Ljava/util/HashMap; access=0x18
-field SKD1:Ljava/util/HashMap; signature=Ljava/util/HashMap<Ljava/lang/Integer;Ljava/lang/Integer;>;
-field SKD1:Ljava/util/HashMap; value=
-field SKD1:Ljava/util/HashMap; annotations=|||
-field SKD2:Ljava/util/HashMap; access=0x18
-field SKD2:Ljava/util/HashMap; signature=Ljava/util/HashMap<Ljava/lang/Integer;Ljava/lang/Integer;>;
-field SKD2:Ljava/util/HashMap; value=
-field SKD2:Ljava/util/HashMap; annotations=|||
-field SKD3:Ljava/util/HashMap; access=0x18
-field SKD3:Ljava/util/HashMap; signature=Ljava/util/HashMap<Ljava/lang/Integer;Ljava/lang/Integer;>;
-field SKD3:Ljava/util/HashMap; value=
-field SKD3:Ljava/util/HashMap; annotations=|||
-field SKU1:Ljava/util/HashMap; access=0x18
-field SKU1:Ljava/util/HashMap; signature=Ljava/util/HashMap<Ljava/lang/Integer;Ljava/lang/Integer;>;
-field SKU1:Ljava/util/HashMap; value=
-field SKU1:Ljava/util/HashMap; annotations=|||
-field SKU2:Ljava/util/HashMap; access=0x18
-field SKU2:Ljava/util/HashMap; signature=Ljava/util/HashMap<Ljava/lang/Integer;Ljava/lang/Integer;>;
-field SKU2:Ljava/util/HashMap; value=
-field SKU2:Ljava/util/HashMap; annotations=|||
 field _ac:I access=0x2
 field _ac:I signature=null
@@ -469,24 +449,4 @@
 method <clinit>()V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method <clinit>()V default=
-method <clinit>()V NEW java/util/HashMap
-method <clinit>()V DUP
-method <clinit>()V INVOKESPECIAL java/util/HashMap.<init>()V
-method <clinit>()V PUTSTATIC local/lito381/server/NativeDeCharacterFields.SKU1:Ljava/util/HashMap;
-method <clinit>()V NEW java/util/HashMap
-method <clinit>()V DUP
-method <clinit>()V INVOKESPECIAL java/util/HashMap.<init>()V
-method <clinit>()V PUTSTATIC local/lito381/server/NativeDeCharacterFields.SKU2:Ljava/util/HashMap;
-method <clinit>()V NEW java/util/HashMap
-method <clinit>()V DUP
-method <clinit>()V INVOKESPECIAL java/util/HashMap.<init>()V
-method <clinit>()V PUTSTATIC local/lito381/server/NativeDeCharacterFields.SKD1:Ljava/util/HashMap;
-method <clinit>()V NEW java/util/HashMap
-method <clinit>()V DUP
-method <clinit>()V INVOKESPECIAL java/util/HashMap.<init>()V
-method <clinit>()V PUTSTATIC local/lito381/server/NativeDeCharacterFields.SKD2:Ljava/util/HashMap;
-method <clinit>()V NEW java/util/HashMap
-method <clinit>()V DUP
-method <clinit>()V INVOKESPECIAL java/util/HashMap.<init>()V
-method <clinit>()V PUTSTATIC local/lito381/server/NativeDeCharacterFields.SKD3:Ljava/util/HashMap;
 method <clinit>()V NEW java/lang/ThreadLocal
 method <clinit>()V DUP
```

### NativeDeCharacterFields$CombatSkillDerived

```diff
--- staged/NativeDeCharacterFields$CombatSkillDerived
+++ formal/NativeDeCharacterFields$CombatSkillDerived
@@ -94,5 +94,5 @@
 method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; GOTO L0
 method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; LABEL L1
-method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; GETSTATIC local/lito381/server/NativeDeCharacterFields.SKU1:Ljava/util/HashMap;
+method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; GETSTATIC unresolved(local/lito381/server/NativeDeCharacterFields).SKU1:Ljava/util/HashMap;
 method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; ALOAD 8
 method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; INVOKEVIRTUAL java/util/HashMap.get(Ljava/lang/Object;)Ljava/lang/Object;
@@ -107,5 +107,5 @@
 method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; ISTORE 1
 method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; LABEL L2
-method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; GETSTATIC local/lito381/server/NativeDeCharacterFields.SKU2:Ljava/util/HashMap;
+method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; GETSTATIC unresolved(local/lito381/server/NativeDeCharacterFields).SKU2:Ljava/util/HashMap;
 method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; ALOAD 8
 method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; INVOKEVIRTUAL java/util/HashMap.get(Ljava/lang/Object;)Ljava/lang/Object;
@@ -120,5 +120,5 @@
 method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; ISTORE 2
 method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; LABEL L3
-method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; GETSTATIC local/lito381/server/NativeDeCharacterFields.SKD1:Ljava/util/HashMap;
+method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; GETSTATIC unresolved(local/lito381/server/NativeDeCharacterFields).SKD1:Ljava/util/HashMap;
 method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; ALOAD 8
 method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; INVOKEVIRTUAL java/util/HashMap.get(Ljava/lang/Object;)Ljava/lang/Object;
@@ -133,5 +133,5 @@
 method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; ISTORE 3
 method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; LABEL L4
-method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; GETSTATIC local/lito381/server/NativeDeCharacterFields.SKD2:Ljava/util/HashMap;
+method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; GETSTATIC unresolved(local/lito381/server/NativeDeCharacterFields).SKD2:Ljava/util/HashMap;
 method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; ALOAD 8
 method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; INVOKEVIRTUAL java/util/HashMap.get(Ljava/lang/Object;)Ljava/lang/Object;
@@ -146,5 +146,5 @@
 method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; ISTORE 4
 method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; LABEL L5
-method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; GETSTATIC local/lito381/server/NativeDeCharacterFields.SKD3:Ljava/util/HashMap;
+method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; GETSTATIC unresolved(local/lito381/server/NativeDeCharacterFields).SKD3:Ljava/util/HashMap;
 method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; ALOAD 8
 method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; INVOKEVIRTUAL java/util/HashMap.get(Ljava/lang/Object;)Ljava/lang/Object;
```

### NativeDeInstanceState

```diff
--- staged/NativeDeInstanceState
+++ formal/NativeDeInstanceState
@@ -164,148 +164,4 @@
 field sourceDeLiveAiBinding:Ljava/lang/Object; value=
 field sourceDeLiveAiBinding:Ljava/lang/Object; annotations=|||
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V access=0x2
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V signature=null
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V exceptions=
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V default=
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 1
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.id()I
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 1
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.effects()Llocal/lito381/server/NativeDeResurrectionEffects;
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 1
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.owner()Llocal/lito381/server/NativeDeDeletionOwner;
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 1
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.perception()Llocal/lito381/server/NativeDePerceptionPorts$Services;
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 1
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.template()Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 1
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.npc()Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 3
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 1
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState.construction:Llocal/lito381/server/NativeDeInstancePorts$Construction;
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V NEW java/lang/Object
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V DUP
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V INVOKESPECIAL java/lang/Object.<init>()V
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiScheduleLock:Ljava/lang/Object;
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Long:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiChainToken:J
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Integer:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTickPending:Z
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Integer:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTickExecuting:Z
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Integer:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiStopRequested:Z
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Integer:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWakeupRequested:Z
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Integer:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiRestartRequested:Z
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Long:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTimerSequence:J
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Long:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiActiveTimerToken:J
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Integer:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiPendingBackground:Z
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Integer:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWorkerRetryAttempt:I
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Integer:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDePerceptionState._isBackgroundMode:Z
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V INVOKESTATIC java/lang/System.currentTimeMillis()J
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._lastWakeupTime:J
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST null
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._runFuture:Ljava/util/concurrent/ScheduledFuture;
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Long:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveGenerationSequence:J
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Long:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveGeneration:J
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Long:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveTimerSequence:J
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Long:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveActiveTimerToken:J
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST null
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveFuture:Ljava/util/concurrent/ScheduledFuture;
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Long:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deNextActionUntilMs:J
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V NEW local/lito381/server/NativeDeInstanceState$DeRepathThrottle
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V DUP
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V INVOKESPECIAL local/lito381/server/NativeDeInstanceState$DeRepathThrottle.<init>()V
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState.deRepathThrottle:Llocal/lito381/server/NativeDeInstanceState$DeRepathThrottle;
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Integer:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._spawnLocationChecked:Z
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Integer:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDePerceptionState._clanid:I
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Integer:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDePerceptionState._aiType:I
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST null
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDePerceptionState._clan_name:Ljava/lang/String;
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST null
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._emblem:Llocal/lito381/server/NativeDePerceptionEmblemData;
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST null
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Integer:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Integer:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._isDollEnable:Z
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST null
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._classRestrict:Ljava/lang/String;
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Integer:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._initFailed:Z
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Integer:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._currentWeapon:I
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Integer:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._isAutoBuffEnabled:Z
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Integer:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._autoBuffInterval:I
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Integer:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._autoBuffChance:I
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V CONST Long:0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._lastAutoBuffTime:J
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V NEW java/util/concurrent/atomic/AtomicLong
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V DUP
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V INVOKESPECIAL java/util/concurrent/atomic/AtomicLong.<init>()V
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deathGeneration:Ljava/util/concurrent/atomic/AtomicLong;
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 2
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V RETURN
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;)V access=0x1
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;)V signature=null
@@ -313,4 +169,7 @@
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;)V default=
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState.construction:Llocal/lito381/server/NativeDeInstancePorts$Construction;
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 1
@@ -328,7 +187,4 @@
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 2
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 1
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;)V PUTFIELD local/lito381/server/NativeDeInstanceState.construction:Llocal/lito381/server/NativeDeInstancePorts$Construction;
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;)V NEW java/lang/Object
@@ -456,11 +312,146 @@
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V default=
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 1
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 1
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 3
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V INVOKESTATIC local/lito381/server/NativeDeInstanceNames.name(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;)Llocal/lito381/server/NativeDeInitializerKernel$Name;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ASTORE 3
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState.construction:Llocal/lito381/server/NativeDeInstancePorts$Construction;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.id()I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.effects()Llocal/lito381/server/NativeDeResurrectionEffects;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.owner()Llocal/lito381/server/NativeDeDeletionOwner;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.perception()Llocal/lito381/server/NativeDePerceptionPorts$Services;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.template()Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.npc()Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 2
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V INVOKESPECIAL local/lito381/server/NativeDeInstanceState.<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V NEW java/lang/Object
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V DUP
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V INVOKESPECIAL java/lang/Object.<init>()V
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiScheduleLock:Ljava/lang/Object;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiChainToken:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTickPending:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTickExecuting:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiStopRequested:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWakeupRequested:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiRestartRequested:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTimerSequence:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiActiveTimerToken:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiPendingBackground:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWorkerRetryAttempt:I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDePerceptionState._isBackgroundMode:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V INVOKESTATIC java/lang/System.currentTimeMillis()J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._lastWakeupTime:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST null
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._runFuture:Ljava/util/concurrent/ScheduledFuture;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveGenerationSequence:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveGeneration:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveTimerSequence:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveActiveTimerToken:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST null
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveFuture:Ljava/util/concurrent/ScheduledFuture;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deNextActionUntilMs:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V NEW local/lito381/server/NativeDeInstanceState$DeRepathThrottle
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V DUP
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V INVOKESPECIAL local/lito381/server/NativeDeInstanceState$DeRepathThrottle.<init>()V
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState.deRepathThrottle:Llocal/lito381/server/NativeDeInstanceState$DeRepathThrottle;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._spawnLocationChecked:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDePerceptionState._clanid:I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDePerceptionState._aiType:I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST null
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDePerceptionState._clan_name:Ljava/lang/String;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST null
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._emblem:Llocal/lito381/server/NativeDePerceptionEmblemData;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST null
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._isDollEnable:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST null
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._classRestrict:Ljava/lang/String;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._initFailed:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._currentWeapon:I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._isAutoBuffEnabled:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._autoBuffInterval:I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._autoBuffChance:I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._lastAutoBuffTime:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V NEW java/util/concurrent/atomic/AtomicLong
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V DUP
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V INVOKESPECIAL java/util/concurrent/atomic/AtomicLong.<init>()V
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deathGeneration:Ljava/util/concurrent/atomic/AtomicLong;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 3
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V ALOAD 0
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.startNpc()V
@@ -471,11 +462,146 @@
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V default=
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 1
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 1
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 3
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V INVOKESTATIC local/lito381/server/NativeDeInstanceNames.name(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;)Llocal/lito381/server/NativeDeInitializerKernel$Name;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ASTORE 3
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState.construction:Llocal/lito381/server/NativeDeInstancePorts$Construction;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.id()I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.effects()Llocal/lito381/server/NativeDeResurrectionEffects;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.owner()Llocal/lito381/server/NativeDeDeletionOwner;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.perception()Llocal/lito381/server/NativeDePerceptionPorts$Services;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.template()Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.npc()Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 2
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V INVOKESPECIAL local/lito381/server/NativeDeInstanceState.<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V NEW java/lang/Object
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V DUP
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V INVOKESPECIAL java/lang/Object.<init>()V
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiScheduleLock:Ljava/lang/Object;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiChainToken:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTickPending:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTickExecuting:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiStopRequested:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWakeupRequested:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiRestartRequested:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTimerSequence:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiActiveTimerToken:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiPendingBackground:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWorkerRetryAttempt:I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDePerceptionState._isBackgroundMode:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V INVOKESTATIC java/lang/System.currentTimeMillis()J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._lastWakeupTime:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST null
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._runFuture:Ljava/util/concurrent/ScheduledFuture;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveGenerationSequence:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveGeneration:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveTimerSequence:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveActiveTimerToken:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST null
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveFuture:Ljava/util/concurrent/ScheduledFuture;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._deNextActionUntilMs:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V NEW local/lito381/server/NativeDeInstanceState$DeRepathThrottle
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V DUP
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V INVOKESPECIAL local/lito381/server/NativeDeInstanceState$DeRepathThrottle.<init>()V
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState.deRepathThrottle:Llocal/lito381/server/NativeDeInstanceState$DeRepathThrottle;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._spawnLocationChecked:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDePerceptionState._clanid:I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDePerceptionState._aiType:I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST null
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDePerceptionState._clan_name:Ljava/lang/String;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST null
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._emblem:Llocal/lito381/server/NativeDePerceptionEmblemData;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST null
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._isDollEnable:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST null
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._classRestrict:Ljava/lang/String;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._initFailed:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._currentWeapon:I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._isAutoBuffEnabled:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._autoBuffInterval:I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._autoBuffChance:I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._lastAutoBuffTime:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V NEW java/util/concurrent/atomic/AtomicLong
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V DUP
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V INVOKESPECIAL java/util/concurrent/atomic/AtomicLong.<init>()V
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._deathGeneration:Ljava/util/concurrent/atomic/AtomicLong;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 3
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ALOAD 0
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;Z)V ILOAD 4
@@ -489,11 +615,146 @@
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V default=
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 1
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 1
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 3
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V INVOKESTATIC local/lito381/server/NativeDeInstanceNames.name(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;)Llocal/lito381/server/NativeDeInitializerKernel$Name;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ASTORE 3
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState.construction:Llocal/lito381/server/NativeDeInstancePorts$Construction;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.id()I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.effects()Llocal/lito381/server/NativeDeResurrectionEffects;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.owner()Llocal/lito381/server/NativeDeDeletionOwner;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.perception()Llocal/lito381/server/NativeDePerceptionPorts$Services;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.template()Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.npc()Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 2
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V INVOKESPECIAL local/lito381/server/NativeDeInstanceState.<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V NEW java/lang/Object
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V DUP
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V INVOKESPECIAL java/lang/Object.<init>()V
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiScheduleLock:Ljava/lang/Object;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiChainToken:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTickPending:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTickExecuting:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiStopRequested:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWakeupRequested:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiRestartRequested:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTimerSequence:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiActiveTimerToken:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiPendingBackground:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWorkerRetryAttempt:I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDePerceptionState._isBackgroundMode:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V INVOKESTATIC java/lang/System.currentTimeMillis()J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._lastWakeupTime:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST null
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._runFuture:Ljava/util/concurrent/ScheduledFuture;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveGenerationSequence:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveGeneration:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveTimerSequence:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveActiveTimerToken:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST null
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveFuture:Ljava/util/concurrent/ScheduledFuture;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deNextActionUntilMs:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V NEW local/lito381/server/NativeDeInstanceState$DeRepathThrottle
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V DUP
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V INVOKESPECIAL local/lito381/server/NativeDeInstanceState$DeRepathThrottle.<init>()V
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState.deRepathThrottle:Llocal/lito381/server/NativeDeInstanceState$DeRepathThrottle;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._spawnLocationChecked:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDePerceptionState._clanid:I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDePerceptionState._aiType:I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST null
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDePerceptionState._clan_name:Ljava/lang/String;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST null
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._emblem:Llocal/lito381/server/NativeDePerceptionEmblemData;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST null
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._isDollEnable:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST null
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._classRestrict:Ljava/lang/String;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._initFailed:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._currentWeapon:I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._isAutoBuffEnabled:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._autoBuffInterval:I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._autoBuffChance:I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._lastAutoBuffTime:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V NEW java/util/concurrent/atomic/AtomicLong
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V DUP
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V INVOKESPECIAL java/util/concurrent/atomic/AtomicLong.<init>()V
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deathGeneration:Ljava/util/concurrent/atomic/AtomicLong;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 3
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ALOAD 0
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V ILOAD 4
@@ -513,11 +774,146 @@
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V default=
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 1
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 1
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 3
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V INVOKESTATIC local/lito381/server/NativeDeInstanceNames.name(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;)Llocal/lito381/server/NativeDeInitializerKernel$Name;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ASTORE 3
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState.construction:Llocal/lito381/server/NativeDeInstancePorts$Construction;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.id()I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.effects()Llocal/lito381/server/NativeDeResurrectionEffects;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.owner()Llocal/lito381/server/NativeDeDeletionOwner;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.perception()Llocal/lito381/server/NativeDePerceptionPorts$Services;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.template()Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 1
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstancePorts$Construction.npc()Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 2
-method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V INVOKESPECIAL local/lito381/server/NativeDeInstanceState.<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.<init>(ILlocal/lito381/server/NativeDeResurrectionEffects;Llocal/lito381/server/NativeDeDeletionOwner;Llocal/lito381/server/NativeDePerceptionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplatePorts$Services;Llocal/lito381/server/NativeDeNpcConstructionPorts$Services;Llocal/lito381/server/NativeDeNpcTemplate;)V
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V NEW java/lang/Object
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V DUP
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V INVOKESPECIAL java/lang/Object.<init>()V
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiScheduleLock:Ljava/lang/Object;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiChainToken:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTickPending:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTickExecuting:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiStopRequested:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWakeupRequested:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiRestartRequested:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiTimerSequence:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiActiveTimerToken:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiPendingBackground:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deAiWorkerRetryAttempt:I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDePerceptionState._isBackgroundMode:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V INVOKESTATIC java/lang/System.currentTimeMillis()J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._lastWakeupTime:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST null
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._runFuture:Ljava/util/concurrent/ScheduledFuture;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveGenerationSequence:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveGeneration:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveTimerSequence:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveActiveTimerToken:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST null
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._delayedMoveFuture:Ljava/util/concurrent/ScheduledFuture;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deNextActionUntilMs:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V NEW local/lito381/server/NativeDeInstanceState$DeRepathThrottle
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V DUP
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V INVOKESPECIAL local/lito381/server/NativeDeInstanceState$DeRepathThrottle.<init>()V
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState.deRepathThrottle:Llocal/lito381/server/NativeDeInstanceState$DeRepathThrottle;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._spawnLocationChecked:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDePerceptionState._clanid:I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDePerceptionState._aiType:I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST null
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDePerceptionState._clan_name:Ljava/lang/String;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST null
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._emblem:Llocal/lito381/server/NativeDePerceptionEmblemData;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST null
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._isDollEnable:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST null
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._classRestrict:Ljava/lang/String;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._initFailed:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._currentWeapon:I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._isAutoBuffEnabled:Z
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._autoBuffInterval:I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Integer:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._autoBuffChance:I
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V CONST Long:0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._lastAutoBuffTime:J
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V NEW java/util/concurrent/atomic/AtomicLong
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V DUP
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V INVOKESPECIAL java/util/concurrent/atomic/AtomicLong.<init>()V
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeInstanceState._deathGeneration:Ljava/util/concurrent/atomic/AtomicLong;
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 3
+method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ALOAD 0
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V ILOAD 4
@@ -532,255 +928,4 @@
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.startNpc()V
 method <init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeNpcTemplate;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZZLjava/lang/String;)V RETURN
-method addAc(I)V access=0x41
-method addAc(I)V signature=null
-method addAc(I)V exceptions=
-method addAc(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addAc(I)V default=
-method addAc(I)V ALOAD 0
-method addAc(I)V ILOAD 1
-method addAc(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addAc(I)V
-method addAc(I)V RETURN
-method addBowDmgup(I)V access=0x41
-method addBowDmgup(I)V signature=null
-method addBowDmgup(I)V exceptions=
-method addBowDmgup(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addBowDmgup(I)V default=
-method addBowDmgup(I)V ALOAD 0
-method addBowDmgup(I)V ILOAD 1
-method addBowDmgup(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addBowDmgup(I)V
-method addBowDmgup(I)V RETURN
-method addBowHitup(I)V access=0x41
-method addBowHitup(I)V signature=null
-method addBowHitup(I)V exceptions=
-method addBowHitup(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addBowHitup(I)V default=
-method addBowHitup(I)V ALOAD 0
-method addBowHitup(I)V ILOAD 1
-method addBowHitup(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addBowHitup(I)V
-method addBowHitup(I)V RETURN
-method addCha(I)V access=0x41
-method addCha(I)V signature=null
-method addCha(I)V exceptions=
-method addCha(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addCha(I)V default=
-method addCha(I)V ALOAD 0
-method addCha(I)V ILOAD 1
-method addCha(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addCha(I)V
-method addCha(I)V RETURN
-method addCon(I)V access=0x41
-method addCon(I)V signature=null
-method addCon(I)V exceptions=
-method addCon(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addCon(I)V default=
-method addCon(I)V ALOAD 0
-method addCon(I)V ILOAD 1
-method addCon(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addCon(I)V
-method addCon(I)V RETURN
-method addDex(I)V access=0x41
-method addDex(I)V signature=null
-method addDex(I)V exceptions=
-method addDex(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addDex(I)V default=
-method addDex(I)V ALOAD 0
-method addDex(I)V ILOAD 1
-method addDex(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addDex(I)V
-method addDex(I)V RETURN
-method addDmgup(I)V access=0x41
-method addDmgup(I)V signature=null
-method addDmgup(I)V exceptions=
-method addDmgup(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addDmgup(I)V default=
-method addDmgup(I)V ALOAD 0
-method addDmgup(I)V ILOAD 1
-method addDmgup(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addDmgup(I)V
-method addDmgup(I)V RETURN
-method addEarth(I)V access=0x41
-method addEarth(I)V signature=null
-method addEarth(I)V exceptions=
-method addEarth(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addEarth(I)V default=
-method addEarth(I)V ALOAD 0
-method addEarth(I)V ILOAD 1
-method addEarth(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addEarth(I)V
-method addEarth(I)V RETURN
-method addFire(I)V access=0x41
-method addFire(I)V signature=null
-method addFire(I)V exceptions=
-method addFire(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addFire(I)V default=
-method addFire(I)V ALOAD 0
-method addFire(I)V ILOAD 1
-method addFire(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addFire(I)V
-method addFire(I)V RETURN
-method addHitup(I)V access=0x41
-method addHitup(I)V signature=null
-method addHitup(I)V exceptions=
-method addHitup(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addHitup(I)V default=
-method addHitup(I)V ALOAD 0
-method addHitup(I)V ILOAD 1
-method addHitup(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addHitup(I)V
-method addHitup(I)V RETURN
-method addInt(I)V access=0x41
-method addInt(I)V signature=null
-method addInt(I)V exceptions=
-method addInt(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addInt(I)V default=
-method addInt(I)V ALOAD 0
-method addInt(I)V ILOAD 1
-method addInt(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addInt(I)V
-method addInt(I)V RETURN
-method addKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V access=0x41
-method addKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V signature=null
-method addKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V exceptions=
-method addKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V default=
-method addKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V ALOAD 0
-method addKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V ALOAD 1
-method addKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
-method addKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V RETURN
-method addMaxHp(I)V access=0x41
-method addMaxHp(I)V signature=null
-method addMaxHp(I)V exceptions=
-method addMaxHp(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addMaxHp(I)V default=
-method addMaxHp(I)V ALOAD 0
-method addMaxHp(I)V ILOAD 1
-method addMaxHp(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addMaxHp(I)V
-method addMaxHp(I)V RETURN
-method addMaxMp(I)V access=0x41
-method addMaxMp(I)V signature=null
-method addMaxMp(I)V exceptions=
-method addMaxMp(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addMaxMp(I)V default=
-method addMaxMp(I)V ALOAD 0
-method addMaxMp(I)V ILOAD 1
-method addMaxMp(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addMaxMp(I)V
-method addMaxMp(I)V RETURN
-method addMr(I)V access=0x41
-method addMr(I)V signature=null
-method addMr(I)V exceptions=
-method addMr(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addMr(I)V default=
-method addMr(I)V ALOAD 0
-method addMr(I)V ILOAD 1
-method addMr(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addMr(I)V
-method addMr(I)V RETURN
-method addRegistBlind(I)V access=0x41
-method addRegistBlind(I)V signature=null
-method addRegistBlind(I)V exceptions=
-method addRegistBlind(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addRegistBlind(I)V default=
-method addRegistBlind(I)V ALOAD 0
-method addRegistBlind(I)V ILOAD 1
-method addRegistBlind(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addRegistBlind(I)V
-method addRegistBlind(I)V RETURN
-method addRegistSleep(I)V access=0x41
-method addRegistSleep(I)V signature=null
-method addRegistSleep(I)V exceptions=
-method addRegistSleep(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addRegistSleep(I)V default=
-method addRegistSleep(I)V ALOAD 0
-method addRegistSleep(I)V ILOAD 1
-method addRegistSleep(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addRegistSleep(I)V
-method addRegistSleep(I)V RETURN
-method addRegistStone(I)V access=0x41
-method addRegistStone(I)V signature=null
-method addRegistStone(I)V exceptions=
-method addRegistStone(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addRegistStone(I)V default=
-method addRegistStone(I)V ALOAD 0
-method addRegistStone(I)V ILOAD 1
-method addRegistStone(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addRegistStone(I)V
-method addRegistStone(I)V RETURN
-method addRegistStun(I)V access=0x41
-method addRegistStun(I)V signature=null
-method addRegistStun(I)V exceptions=
-method addRegistStun(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addRegistStun(I)V default=
-method addRegistStun(I)V ALOAD 0
-method addRegistStun(I)V ILOAD 1
-method addRegistStun(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addRegistStun(I)V
-method addRegistStun(I)V RETURN
-method addRegistSustain(I)V access=0x41
-method addRegistSustain(I)V signature=null
-method addRegistSustain(I)V exceptions=
-method addRegistSustain(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addRegistSustain(I)V default=
-method addRegistSustain(I)V ALOAD 0
-method addRegistSustain(I)V ILOAD 1
-method addRegistSustain(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addRegistSustain(I)V
-method addRegistSustain(I)V RETURN
-method addSp(I)V access=0x41
-method addSp(I)V signature=null
-method addSp(I)V exceptions=
-method addSp(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addSp(I)V default=
-method addSp(I)V ALOAD 0
-method addSp(I)V ILOAD 1
-method addSp(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addSp(I)V
-method addSp(I)V RETURN
-method addStr(I)V access=0x41
-method addStr(I)V signature=null
-method addStr(I)V exceptions=
-method addStr(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addStr(I)V default=
-method addStr(I)V ALOAD 0
-method addStr(I)V ILOAD 1
-method addStr(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addStr(I)V
-method addStr(I)V RETURN
-method addStunSuccess(I)V access=0x41
-method addStunSuccess(I)V signature=null
-method addStunSuccess(I)V exceptions=
-method addStunSuccess(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addStunSuccess(I)V default=
-method addStunSuccess(I)V ALOAD 0
-method addStunSuccess(I)V ILOAD 1
-method addStunSuccess(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addStunSuccess(I)V
-method addStunSuccess(I)V RETURN
-method addWater(I)V access=0x41
-method addWater(I)V signature=null
-method addWater(I)V exceptions=
-method addWater(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addWater(I)V default=
-method addWater(I)V ALOAD 0
-method addWater(I)V ILOAD 1
-method addWater(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addWater(I)V
-method addWater(I)V RETURN
-method addWeaponBreakStack()V access=0x41
-method addWeaponBreakStack()V signature=null
-method addWeaponBreakStack()V exceptions=
-method addWeaponBreakStack()V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addWeaponBreakStack()V default=
-method addWeaponBreakStack()V ALOAD 0
-method addWeaponBreakStack()V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addWeaponBreakStack()V
-method addWeaponBreakStack()V RETURN
-method addWind(I)V access=0x41
-method addWind(I)V signature=null
-method addWind(I)V exceptions=
-method addWind(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addWind(I)V default=
-method addWind(I)V ALOAD 0
-method addWind(I)V ILOAD 1
-method addWind(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addWind(I)V
-method addWind(I)V RETURN
-method addWis(I)V access=0x41
-method addWis(I)V signature=null
-method addWis(I)V exceptions=
-method addWis(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method addWis(I)V default=
-method addWis(I)V ALOAD 0
-method addWis(I)V ILOAD 1
-method addWis(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.addWis(I)V
-method addWis(I)V RETURN
-method add_regist_freeze(I)V access=0x41
-method add_regist_freeze(I)V signature=null
-method add_regist_freeze(I)V exceptions=
-method add_regist_freeze(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method add_regist_freeze(I)V default=
-method add_regist_freeze(I)V ALOAD 0
-method add_regist_freeze(I)V ILOAD 1
-method add_regist_freeze(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.add_regist_freeze(I)V
-method add_regist_freeze(I)V RETURN
 method beginAiTick(J)Z access=0x2
 method beginAiTick(J)Z signature=null
@@ -789,7 +934,5 @@
 method beginAiTick(J)Z default=
 method beginAiTick(J)Z ALOAD 0
-method beginAiTick(J)Z ASTORE 3
 method beginAiTick(J)Z ALOAD 0
-method beginAiTick(J)Z ALOAD 3
 method beginAiTick(J)Z LLOAD 1/2
 method beginAiTick(J)Z INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$0aec4a63a23900f28867605d4437057a49ddbed73e6b0114d32a6492859e1f5f(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
@@ -804,7 +947,5 @@
 method beginBackgroundTick(J)Z default=
 method beginBackgroundTick(J)Z ALOAD 0
-method beginBackgroundTick(J)Z ASTORE 3
 method beginBackgroundTick(J)Z ALOAD 0
-method beginBackgroundTick(J)Z ALOAD 3
 method beginBackgroundTick(J)Z LLOAD 1/2
 method beginBackgroundTick(J)Z INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$c67a63d5c7acd01eeb85e91928eb2fd866c5d319507bda775e0759f14074efd8(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
@@ -825,13 +966,4 @@
 method broadcastPacketAll(Llocal/lito381/server/NativeDeInitializerKernel$Packet;)V INVOKEINTERFACE local/lito381/server/NativeDeInstancePorts$Scene.publish(Llocal/lito381/server/NativeDeInstanceState;Llocal/lito381/server/NativeDeInitializerKernel$Packet;)V itf
 method broadcastPacketAll(Llocal/lito381/server/NativeDeInitializerKernel$Packet;)V RETURN
-method broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V access=0x41
-method broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V signature=null
-method broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V exceptions=
-method broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V default=
-method broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V ALOAD 0
-method broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V ALOAD 1
-method broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
-method broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V RETURN
 method calcSleepTime(II)I access=0x1
 method calcSleepTime(II)I signature=null
@@ -840,7 +972,5 @@
 method calcSleepTime(II)I default=
 method calcSleepTime(II)I ALOAD 0
-method calcSleepTime(II)I ASTORE 3
 method calcSleepTime(II)I ALOAD 0
-method calcSleepTime(II)I ALOAD 3
 method calcSleepTime(II)I ILOAD 1
 method calcSleepTime(II)I ILOAD 2
@@ -856,7 +986,5 @@
 method checkNeedActive()Z default=
 method checkNeedActive()Z ALOAD 0
-method checkNeedActive()Z ASTORE 1
 method checkNeedActive()Z ALOAD 0
-method checkNeedActive()Z ALOAD 1
 method checkNeedActive()Z INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$3e50641aa1ab0102beb4864083197cc242cde7dc380ef4282de0db90b13ebcbb(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method checkNeedActive()Z INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
@@ -870,20 +998,9 @@
 method checkSpawnLocation()V default=
 method checkSpawnLocation()V ALOAD 0
-method checkSpawnLocation()V ASTORE 1
 method checkSpawnLocation()V ALOAD 0
-method checkSpawnLocation()V ALOAD 1
 method checkSpawnLocation()V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$d8070b41a40457ccc97186b92c04f1b11b0a0ae10beef000451c622a0d773ec6(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method checkSpawnLocation()V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 method checkSpawnLocation()V POP
 method checkSpawnLocation()V RETURN
-method claimExpiredCorpseDeletion(J)Z access=0x41
-method claimExpiredCorpseDeletion(J)Z signature=null
-method claimExpiredCorpseDeletion(J)Z exceptions=
-method claimExpiredCorpseDeletion(J)Z annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method claimExpiredCorpseDeletion(J)Z default=
-method claimExpiredCorpseDeletion(J)Z ALOAD 0
-method claimExpiredCorpseDeletion(J)Z LLOAD 1/2
-method claimExpiredCorpseDeletion(J)Z INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.claimExpiredCorpseDeletion(J)Z
-method claimExpiredCorpseDeletion(J)Z IRETURN
 method clearFootprintBlockAggroApplied()V access=0x1
 method clearFootprintBlockAggroApplied()V signature=null
@@ -923,7 +1040,5 @@
 method completeAiStopLocked(J)Z default=
 method completeAiStopLocked(J)Z ALOAD 0
-method completeAiStopLocked(J)Z ASTORE 3
 method completeAiStopLocked(J)Z ALOAD 0
-method completeAiStopLocked(J)Z ALOAD 3
 method completeAiStopLocked(J)Z LLOAD 1/2
 method completeAiStopLocked(J)Z INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$6d02416b33308c9d6589d9dc06a06174d8da49089ce2f137db9c8b07cb487537(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
@@ -985,12 +1100,4 @@
 method deleteMe()V ALOAD 1
 method deleteMe()V ATHROW
-method destroyed()Z access=0x41
-method destroyed()Z signature=null
-method destroyed()Z exceptions=
-method destroyed()Z annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method destroyed()Z default=
-method destroyed()Z ALOAD 0
-method destroyed()Z INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.destroyed()Z
-method destroyed()Z IRETURN
 method dispatchAiWork(JZ)V access=0x2
 method dispatchAiWork(JZ)V signature=null
@@ -999,7 +1106,5 @@
 method dispatchAiWork(JZ)V default=
 method dispatchAiWork(JZ)V ALOAD 0
-method dispatchAiWork(JZ)V ASTORE 4
 method dispatchAiWork(JZ)V ALOAD 0
-method dispatchAiWork(JZ)V ALOAD 4
 method dispatchAiWork(JZ)V LLOAD 1/2
 method dispatchAiWork(JZ)V ILOAD 3
@@ -1014,7 +1119,5 @@
 method ensureAiScheduled()V default=
 method ensureAiScheduled()V ALOAD 0
-method ensureAiScheduled()V ASTORE 1
 method ensureAiScheduled()V ALOAD 0
-method ensureAiScheduled()V ALOAD 1
 method ensureAiScheduled()V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$6732b770fd6477193d9d92d5f6c09ca067e6200aef32d4933526361e2446e9b7(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method ensureAiScheduled()V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
@@ -1027,7 +1130,5 @@
 method enterBackgroundMode(J)V default=
 method enterBackgroundMode(J)V ALOAD 0
-method enterBackgroundMode(J)V ASTORE 3
 method enterBackgroundMode(J)V ALOAD 0
-method enterBackgroundMode(J)V ALOAD 3
 method enterBackgroundMode(J)V LLOAD 1/2
 method enterBackgroundMode(J)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$98684a9aa8bb5063731367e7c3750dc32668fa63ae02ea4e11fea0a608bf4c2a(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
@@ -1059,7 +1160,5 @@
 method failAiTimer(JJ)V default=
 method failAiTimer(JJ)V ALOAD 0
-method failAiTimer(JJ)V ASTORE 5
 method failAiTimer(JJ)V ALOAD 0
-method failAiTimer(JJ)V ALOAD 5
 method failAiTimer(JJ)V LLOAD 1/2
 method failAiTimer(JJ)V LLOAD 3/4
@@ -1074,7 +1173,5 @@
 method finishAiTick(J)V default=
 method finishAiTick(J)V ALOAD 0
-method finishAiTick(J)V ASTORE 3
 method finishAiTick(J)V ALOAD 0
-method finishAiTick(J)V ALOAD 3
 method finishAiTick(J)V LLOAD 1/2
 method finishAiTick(J)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$7564ba95bfa4f830ca5d54a5d82b624e8e155bbfecd75eb17ba8f12f34d5cf75(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
@@ -1082,29 +1179,4 @@
 method finishAiTick(J)V POP
 method finishAiTick(J)V RETURN
-method forward(I)V access=0x41
-method forward(I)V signature=null
-method forward(I)V exceptions=
-method forward(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method forward(I)V default=
-method forward(I)V ALOAD 0
-method forward(I)V ILOAD 1
-method forward(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.forward(I)V
-method forward(I)V RETURN
-method getAc()I access=0x41
-method getAc()I signature=null
-method getAc()I exceptions=
-method getAc()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getAc()I default=
-method getAc()I ALOAD 0
-method getAc()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getAc()I
-method getAc()I IRETURN
-method getAddAttrKind()I access=0x41
-method getAddAttrKind()I signature=null
-method getAddAttrKind()I exceptions=
-method getAddAttrKind()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getAddAttrKind()I default=
-method getAddAttrKind()I ALOAD 0
-method getAddAttrKind()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getAddAttrKind()I
-method getAddAttrKind()I IRETURN
 method getAiType()I access=0x1
 method getAiType()I signature=null
@@ -1115,12 +1187,4 @@
 method getAiType()I GETFIELD local/lito381/server/NativeDePerceptionState._aiType:I
 method getAiType()I IRETURN
-method getAtkspeed()I access=0x41
-method getAtkspeed()I signature=null
-method getAtkspeed()I exceptions=
-method getAtkspeed()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getAtkspeed()I default=
-method getAtkspeed()I ALOAD 0
-method getAtkspeed()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getAtkspeed()I
-method getAtkspeed()I IRETURN
 method getAutoBuffChance()I access=0x1
 method getAutoBuffChance()I signature=null
@@ -1139,44 +1203,4 @@
 method getAutoBuffInterval()I GETFIELD local/lito381/server/NativeDeInstanceState._autoBuffInterval:I
 method getAutoBuffInterval()I IRETURN
-method getBowDmgup()I access=0x41
-method getBowDmgup()I signature=null
-method getBowDmgup()I exceptions=
-method getBowDmgup()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getBowDmgup()I default=
-method getBowDmgup()I ALOAD 0
-method getBowDmgup()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getBowDmgup()I
-method getBowDmgup()I IRETURN
-method getBowHitup()I access=0x41
-method getBowHitup()I signature=null
-method getBowHitup()I exceptions=
-method getBowHitup()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getBowHitup()I default=
-method getBowHitup()I ALOAD 0
-method getBowHitup()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getBowHitup()I
-method getBowHitup()I IRETURN
-method getBraveSpeed()I access=0x41
-method getBraveSpeed()I signature=null
-method getBraveSpeed()I exceptions=
-method getBraveSpeed()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getBraveSpeed()I default=
-method getBraveSpeed()I ALOAD 0
-method getBraveSpeed()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getBraveSpeed()I
-method getBraveSpeed()I IRETURN
-method getCha()S access=0x41
-method getCha()S signature=null
-method getCha()S exceptions=
-method getCha()S annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getCha()S default=
-method getCha()S ALOAD 0
-method getCha()S INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getCha()S
-method getCha()S IRETURN
-method getChaLightSize()I access=0x41
-method getChaLightSize()I signature=null
-method getChaLightSize()I exceptions=
-method getChaLightSize()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getChaLightSize()I default=
-method getChaLightSize()I ALOAD 0
-method getChaLightSize()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getChaLightSize()I
-method getChaLightSize()I IRETURN
 method getClanid()I access=0x1
 method getClanid()I signature=null
@@ -1211,28 +1235,4 @@
 method getCombatBehavior()Llocal/lito381/server/NativeDeInstancePorts$Combat; GETFIELD local/lito381/server/NativeDeInstanceState._combatBehavior:Llocal/lito381/server/NativeDeInstancePorts$Combat;
 method getCombatBehavior()Llocal/lito381/server/NativeDeInstancePorts$Combat; ARETURN
-method getCon()S access=0x41
-method getCon()S signature=null
-method getCon()S exceptions=
-method getCon()S annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getCon()S default=
-method getCon()S ALOAD 0
-method getCon()S INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getCon()S
-method getCon()S IRETURN
-method getCurrentHp()I access=0x41
-method getCurrentHp()I signature=null
-method getCurrentHp()I exceptions=
-method getCurrentHp()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getCurrentHp()I default=
-method getCurrentHp()I ALOAD 0
-method getCurrentHp()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getCurrentHp()I
-method getCurrentHp()I IRETURN
-method getCurrentMp()I access=0x41
-method getCurrentMp()I signature=null
-method getCurrentMp()I exceptions=
-method getCurrentMp()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getCurrentMp()I default=
-method getCurrentMp()I ALOAD 0
-method getCurrentMp()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getCurrentMp()I
-method getCurrentMp()I IRETURN
 method getCurrentWeapon()I access=0x1
 method getCurrentWeapon()I signature=null
@@ -1243,36 +1243,4 @@
 method getCurrentWeapon()I GETFIELD local/lito381/server/NativeDeInstanceState._currentWeapon:I
 method getCurrentWeapon()I IRETURN
-method getDex()S access=0x41
-method getDex()S signature=null
-method getDex()S exceptions=
-method getDex()S annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getDex()S default=
-method getDex()S ALOAD 0
-method getDex()S INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getDex()S
-method getDex()S IRETURN
-method getDmgup()I access=0x41
-method getDmgup()I signature=null
-method getDmgup()I exceptions=
-method getDmgup()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getDmgup()I default=
-method getDmgup()I ALOAD 0
-method getDmgup()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getDmgup()I
-method getDmgup()I IRETURN
-method getDolls()Ljava/util/Map; access=0x41
-method getDolls()Ljava/util/Map; signature=null
-method getDolls()Ljava/util/Map; exceptions=
-method getDolls()Ljava/util/Map; annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getDolls()Ljava/util/Map; default=
-method getDolls()Ljava/util/Map; ALOAD 0
-method getDolls()Ljava/util/Map; INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getDolls()Ljava/util/Map;
-method getDolls()Ljava/util/Map; ARETURN
-method getEarth()I access=0x41
-method getEarth()I signature=null
-method getEarth()I exceptions=
-method getEarth()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getEarth()I default=
-method getEarth()I ALOAD 0
-method getEarth()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getEarth()I
-method getEarth()I IRETURN
 method getEmblem()Llocal/lito381/server/NativeDePerceptionEmblemData; access=0x1
 method getEmblem()Llocal/lito381/server/NativeDePerceptionEmblemData; signature=null
@@ -1283,84 +1251,4 @@
 method getEmblem()Llocal/lito381/server/NativeDePerceptionEmblemData; GETFIELD local/lito381/server/NativeDeInstanceState._emblem:Llocal/lito381/server/NativeDePerceptionEmblemData;
 method getEmblem()Llocal/lito381/server/NativeDePerceptionEmblemData; ARETURN
-method getExp()J access=0x41
-method getExp()J signature=null
-method getExp()J exceptions=
-method getExp()J annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getExp()J default=
-method getExp()J ALOAD 0
-method getExp()J INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getExp()J
-method getExp()J LRETURN
-method getFire()I access=0x41
-method getFire()I signature=null
-method getFire()I exceptions=
-method getFire()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getFire()I default=
-method getFire()I ALOAD 0
-method getFire()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getFire()I
-method getFire()I IRETURN
-method getGfxId()I access=0x41
-method getGfxId()I signature=null
-method getGfxId()I exceptions=
-method getGfxId()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getGfxId()I default=
-method getGfxId()I ALOAD 0
-method getGfxId()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getGfxId()I
-method getGfxId()I IRETURN
-method getHeading()I access=0x41
-method getHeading()I signature=null
-method getHeading()I exceptions=
-method getHeading()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getHeading()I default=
-method getHeading()I ALOAD 0
-method getHeading()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getHeading()I
-method getHeading()I IRETURN
-method getHitup()I access=0x41
-method getHitup()I signature=null
-method getHitup()I exceptions=
-method getHitup()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getHitup()I default=
-method getHitup()I ALOAD 0
-method getHitup()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getHitup()I
-method getHitup()I IRETURN
-method getId()I access=0x41
-method getId()I signature=null
-method getId()I exceptions=
-method getId()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getId()I default=
-method getId()I ALOAD 0
-method getId()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getId()I
-method getId()I IRETURN
-method getInt()S access=0x41
-method getInt()S signature=null
-method getInt()S exceptions=
-method getInt()S annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getInt()S default=
-method getInt()S ALOAD 0
-method getInt()S INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getInt()S
-method getInt()S IRETURN
-method getKarma()I access=0x41
-method getKarma()I signature=null
-method getKarma()I exceptions=
-method getKarma()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getKarma()I default=
-method getKarma()I ALOAD 0
-method getKarma()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getKarma()I
-method getKarma()I IRETURN
-method getKnownObjects()Ljava/util/List; access=0x41
-method getKnownObjects()Ljava/util/List; signature=null
-method getKnownObjects()Ljava/util/List; exceptions=
-method getKnownObjects()Ljava/util/List; annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getKnownObjects()Ljava/util/List; default=
-method getKnownObjects()Ljava/util/List; ALOAD 0
-method getKnownObjects()Ljava/util/List; INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getKnownObjects()Ljava/util/List;
-method getKnownObjects()Ljava/util/List; ARETURN
-method getKnownPlayers()Ljava/util/List; access=0x41
-method getKnownPlayers()Ljava/util/List; signature=null
-method getKnownPlayers()Ljava/util/List; exceptions=
-method getKnownPlayers()Ljava/util/List; annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getKnownPlayers()Ljava/util/List; default=
-method getKnownPlayers()Ljava/util/List; ALOAD 0
-method getKnownPlayers()Ljava/util/List; INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getKnownPlayers()Ljava/util/List;
-method getKnownPlayers()Ljava/util/List; ARETURN
 method getLastAutoBuffTime()J access=0x1
 method getLastAutoBuffTime()J signature=null
@@ -1371,45 +1259,4 @@
 method getLastAutoBuffTime()J GETFIELD local/lito381/server/NativeDeInstanceState._lastAutoBuffTime:J
 method getLastAutoBuffTime()J LRETURN
-method getLawful()I access=0x41
-method getLawful()I signature=null
-method getLawful()I exceptions=
-method getLawful()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getLawful()I default=
-method getLawful()I ALOAD 0
-method getLawful()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getLawful()I
-method getLawful()I IRETURN
-method getLevel()I access=0x41
-method getLevel()I signature=null
-method getLevel()I exceptions=
-method getLevel()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getLevel()I default=
-method getLevel()I ALOAD 0
-method getLevel()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getLevel()I
-method getLevel()I IRETURN
-method getLightSize()I access=0x41
-method getLightSize()I signature=null
-method getLightSize()I exceptions=
-method getLightSize()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getLightSize()I default=
-method getLightSize()I ALOAD 0
-method getLightSize()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getLightSize()I
-method getLightSize()I IRETURN
-method getLineDistance(Llocal/lito381/server/NativeDeObjectFields;)D access=0x41
-method getLineDistance(Llocal/lito381/server/NativeDeObjectFields;)D signature=null
-method getLineDistance(Llocal/lito381/server/NativeDeObjectFields;)D exceptions=
-method getLineDistance(Llocal/lito381/server/NativeDeObjectFields;)D annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getLineDistance(Llocal/lito381/server/NativeDeObjectFields;)D default=
-method getLineDistance(Llocal/lito381/server/NativeDeObjectFields;)D ALOAD 0
-method getLineDistance(Llocal/lito381/server/NativeDeObjectFields;)D ALOAD 1
-method getLineDistance(Llocal/lito381/server/NativeDeObjectFields;)D INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getLineDistance(Llocal/lito381/server/NativeDeObjectFields;)D
-method getLineDistance(Llocal/lito381/server/NativeDeObjectFields;)D DRETURN
-method getLocation()Llocal/lito381/server/NativeDeWorldLocation; access=0x41
-method getLocation()Llocal/lito381/server/NativeDeWorldLocation; signature=null
-method getLocation()Llocal/lito381/server/NativeDeWorldLocation; exceptions=
-method getLocation()Llocal/lito381/server/NativeDeWorldLocation; annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getLocation()Llocal/lito381/server/NativeDeWorldLocation; default=
-method getLocation()Llocal/lito381/server/NativeDeWorldLocation; ALOAD 0
-method getLocation()Llocal/lito381/server/NativeDeWorldLocation; INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getLocation()Llocal/lito381/server/NativeDeWorldLocation;
-method getLocation()Llocal/lito381/server/NativeDeWorldLocation; ARETURN
 method getMagicBonus()I access=0x1
 method getMagicBonus()I signature=null
@@ -1436,44 +1283,4 @@
 method getMagicLevel()I INVOKESTATIC java/lang/Math.max(II)I
 method getMagicLevel()I IRETURN
-method getMap()Llocal/lito381/server/NativeDeWorldMap; access=0x41
-method getMap()Llocal/lito381/server/NativeDeWorldMap; signature=null
-method getMap()Llocal/lito381/server/NativeDeWorldMap; exceptions=
-method getMap()Llocal/lito381/server/NativeDeWorldMap; annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getMap()Llocal/lito381/server/NativeDeWorldMap; default=
-method getMap()Llocal/lito381/server/NativeDeWorldMap; ALOAD 0
-method getMap()Llocal/lito381/server/NativeDeWorldMap; INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getMap()Llocal/lito381/server/NativeDeWorldMap;
-method getMap()Llocal/lito381/server/NativeDeWorldMap; ARETURN
-method getMaxHp()I access=0x41
-method getMaxHp()I signature=null
-method getMaxHp()I exceptions=
-method getMaxHp()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getMaxHp()I default=
-method getMaxHp()I ALOAD 0
-method getMaxHp()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getMaxHp()I
-method getMaxHp()I IRETURN
-method getMaxMp()I access=0x41
-method getMaxMp()I signature=null
-method getMaxMp()I exceptions=
-method getMaxMp()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getMaxMp()I default=
-method getMaxMp()I ALOAD 0
-method getMaxMp()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getMaxMp()I
-method getMaxMp()I IRETURN
-method getMoveSpeed()I access=0x41
-method getMoveSpeed()I signature=null
-method getMoveSpeed()I exceptions=
-method getMoveSpeed()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getMoveSpeed()I default=
-method getMoveSpeed()I ALOAD 0
-method getMoveSpeed()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getMoveSpeed()I
-method getMoveSpeed()I IRETURN
-method getMr()I access=0x41
-method getMr()I signature=null
-method getMr()I exceptions=
-method getMr()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getMr()I default=
-method getMr()I ALOAD 0
-method getMr()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getMr()I
-method getMr()I IRETURN
 method getName()Ljava/lang/String; access=0x1
 method getName()Ljava/lang/String; signature=null
@@ -1482,107 +1289,9 @@
 method getName()Ljava/lang/String; default=
 method getName()Ljava/lang/String; ALOAD 0
-method getName()Ljava/lang/String; ASTORE 1
 method getName()Ljava/lang/String; ALOAD 0
-method getName()Ljava/lang/String; ALOAD 1
 method getName()Ljava/lang/String; INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$f1a04170b5bccd722b023c52222802103bae45b884dc2cf1ae2482be8b428d12(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method getName()Ljava/lang/String; INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 method getName()Ljava/lang/String; CHECKCAST java/lang/String
 method getName()Ljava/lang/String; ARETURN
-method getNameId()Ljava/lang/String; access=0x41
-method getNameId()Ljava/lang/String; signature=null
-method getNameId()Ljava/lang/String; exceptions=
-method getNameId()Ljava/lang/String; annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getNameId()Ljava/lang/String; default=
-method getNameId()Ljava/lang/String; ALOAD 0
-method getNameId()Ljava/lang/String; INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getNameId()Ljava/lang/String;
-method getNameId()Ljava/lang/String; ARETURN
-method getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate; access=0x41
-method getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate; signature=null
-method getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate; exceptions=
-method getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate; annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate; default=
-method getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate; ALOAD 0
-method getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate; INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;
-method getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate; ARETURN
-method getOwnLightSize()I access=0x41
-method getOwnLightSize()I signature=null
-method getOwnLightSize()I exceptions=
-method getOwnLightSize()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getOwnLightSize()I default=
-method getOwnLightSize()I ALOAD 0
-method getOwnLightSize()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getOwnLightSize()I
-method getOwnLightSize()I IRETURN
-method getPassispeed()I access=0x41
-method getPassispeed()I signature=null
-method getPassispeed()I exceptions=
-method getPassispeed()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getPassispeed()I default=
-method getPassispeed()I ALOAD 0
-method getPassispeed()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getPassispeed()I
-method getPassispeed()I IRETURN
-method getPetList()Ljava/util/Map; access=0x41
-method getPetList()Ljava/util/Map; signature=null
-method getPetList()Ljava/util/Map; exceptions=
-method getPetList()Ljava/util/Map; annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getPetList()Ljava/util/Map; default=
-method getPetList()Ljava/util/Map; ALOAD 0
-method getPetList()Ljava/util/Map; INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getPetList()Ljava/util/Map;
-method getPetList()Ljava/util/Map; ARETURN
-method getPoison()Llocal/lito381/server/NativeDePerceptionPorts$Poison; access=0x41
-method getPoison()Llocal/lito381/server/NativeDePerceptionPorts$Poison; signature=null
-method getPoison()Llocal/lito381/server/NativeDePerceptionPorts$Poison; exceptions=
-method getPoison()Llocal/lito381/server/NativeDePerceptionPorts$Poison; annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getPoison()Llocal/lito381/server/NativeDePerceptionPorts$Poison; default=
-method getPoison()Llocal/lito381/server/NativeDePerceptionPorts$Poison; ALOAD 0
-method getPoison()Llocal/lito381/server/NativeDePerceptionPorts$Poison; INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getPoison()Llocal/lito381/server/NativeDePerceptionPorts$Poison;
-method getPoison()Llocal/lito381/server/NativeDePerceptionPorts$Poison; ARETURN
-method getRegistBlind()I access=0x41
-method getRegistBlind()I signature=null
-method getRegistBlind()I exceptions=
-method getRegistBlind()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getRegistBlind()I default=
-method getRegistBlind()I ALOAD 0
-method getRegistBlind()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getRegistBlind()I
-method getRegistBlind()I IRETURN
-method getRegistFreeze()I access=0x41
-method getRegistFreeze()I signature=null
-method getRegistFreeze()I exceptions=
-method getRegistFreeze()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getRegistFreeze()I default=
-method getRegistFreeze()I ALOAD 0
-method getRegistFreeze()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getRegistFreeze()I
-method getRegistFreeze()I IRETURN
-method getRegistSleep()I access=0x41
-method getRegistSleep()I signature=null
-method getRegistSleep()I exceptions=
-method getRegistSleep()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getRegistSleep()I default=
-method getRegistSleep()I ALOAD 0
-method getRegistSleep()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getRegistSleep()I
-method getRegistSleep()I IRETURN
-method getRegistStone()I access=0x41
-method getRegistStone()I signature=null
-method getRegistStone()I exceptions=
-method getRegistStone()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getRegistStone()I default=
-method getRegistStone()I ALOAD 0
-method getRegistStone()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getRegistStone()I
-method getRegistStone()I IRETURN
-method getRegistStun()I access=0x41
-method getRegistStun()I signature=null
-method getRegistStun()I exceptions=
-method getRegistStun()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getRegistStun()I default=
-method getRegistStun()I ALOAD 0
-method getRegistStun()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getRegistStun()I
-method getRegistStun()I IRETURN
-method getRegistSustain()I access=0x41
-method getRegistSustain()I signature=null
-method getRegistSustain()I exceptions=
-method getRegistSustain()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getRegistSustain()I default=
-method getRegistSustain()I ALOAD 0
-method getRegistSustain()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getRegistSustain()I
-method getRegistSustain()I IRETURN
 method getSleepTime()I access=0x1
 method getSleepTime()I signature=null
@@ -1591,7 +1300,5 @@
 method getSleepTime()I default=
 method getSleepTime()I ALOAD 0
-method getSleepTime()I ASTORE 1
 method getSleepTime()I ALOAD 0
-method getSleepTime()I ALOAD 1
 method getSleepTime()I INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$0704c677d2b1e3069243221854187d85f85568638a16c4a31c687353c0811e83(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method getSleepTime()I INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
@@ -1599,126 +1306,4 @@
 method getSleepTime()I INVOKEVIRTUAL java/lang/Integer.intValue()I
 method getSleepTime()I IRETURN
-method getSp()I access=0x41
-method getSp()I signature=null
-method getSp()I exceptions=
-method getSp()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getSp()I default=
-method getSp()I ALOAD 0
-method getSp()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getSp()I
-method getSp()I IRETURN
-method getStatus()I access=0x41
-method getStatus()I signature=null
-method getStatus()I exceptions=
-method getStatus()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getStatus()I default=
-method getStatus()I ALOAD 0
-method getStatus()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getStatus()I
-method getStatus()I IRETURN
-method getStr()S access=0x41
-method getStr()S signature=null
-method getStr()S exceptions=
-method getStr()S annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getStr()S default=
-method getStr()S ALOAD 0
-method getStr()S INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getStr()S
-method getStr()S IRETURN
-method getStunSuccess()I access=0x41
-method getStunSuccess()I signature=null
-method getStunSuccess()I exceptions=
-method getStunSuccess()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getStunSuccess()I default=
-method getStunSuccess()I ALOAD 0
-method getStunSuccess()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getStunSuccess()I
-method getStunSuccess()I IRETURN
-method getTempCharGfx()I access=0x41
-method getTempCharGfx()I signature=null
-method getTempCharGfx()I exceptions=
-method getTempCharGfx()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getTempCharGfx()I default=
-method getTempCharGfx()I ALOAD 0
-method getTempCharGfx()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getTempCharGfx()I
-method getTempCharGfx()I IRETURN
-method getTempLawful()I access=0x41
-method getTempLawful()I signature=null
-method getTempLawful()I exceptions=
-method getTempLawful()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getTempLawful()I default=
-method getTempLawful()I ALOAD 0
-method getTempLawful()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getTempLawful()I
-method getTempLawful()I IRETURN
-method getTileDistance(Llocal/lito381/server/NativeDeObjectFields;)I access=0x41
-method getTileDistance(Llocal/lito381/server/NativeDeObjectFields;)I signature=null
-method getTileDistance(Llocal/lito381/server/NativeDeObjectFields;)I exceptions=
-method getTileDistance(Llocal/lito381/server/NativeDeObjectFields;)I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getTileDistance(Llocal/lito381/server/NativeDeObjectFields;)I default=
-method getTileDistance(Llocal/lito381/server/NativeDeObjectFields;)I ALOAD 0
-method getTileDistance(Llocal/lito381/server/NativeDeObjectFields;)I ALOAD 1
-method getTileDistance(Llocal/lito381/server/NativeDeObjectFields;)I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getTileDistance(Llocal/lito381/server/NativeDeObjectFields;)I
-method getTileDistance(Llocal/lito381/server/NativeDeObjectFields;)I IRETURN
-method getTileLineDistance(Llocal/lito381/server/NativeDeObjectFields;)I access=0x41
-method getTileLineDistance(Llocal/lito381/server/NativeDeObjectFields;)I signature=null
-method getTileLineDistance(Llocal/lito381/server/NativeDeObjectFields;)I exceptions=
-method getTileLineDistance(Llocal/lito381/server/NativeDeObjectFields;)I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getTileLineDistance(Llocal/lito381/server/NativeDeObjectFields;)I default=
-method getTileLineDistance(Llocal/lito381/server/NativeDeObjectFields;)I ALOAD 0
-method getTileLineDistance(Llocal/lito381/server/NativeDeObjectFields;)I ALOAD 1
-method getTileLineDistance(Llocal/lito381/server/NativeDeObjectFields;)I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getTileLineDistance(Llocal/lito381/server/NativeDeObjectFields;)I
-method getTileLineDistance(Llocal/lito381/server/NativeDeObjectFields;)I IRETURN
-method getTitle()Ljava/lang/String; access=0x41
-method getTitle()Ljava/lang/String; signature=null
-method getTitle()Ljava/lang/String; exceptions=
-method getTitle()Ljava/lang/String; annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getTitle()Ljava/lang/String; default=
-method getTitle()Ljava/lang/String; ALOAD 0
-method getTitle()Ljava/lang/String; INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getTitle()Ljava/lang/String;
-method getTitle()Ljava/lang/String; ARETURN
-method getTrueMr()I access=0x41
-method getTrueMr()I signature=null
-method getTrueMr()I exceptions=
-method getTrueMr()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getTrueMr()I default=
-method getTrueMr()I ALOAD 0
-method getTrueMr()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getTrueMr()I
-method getTrueMr()I IRETURN
-method getTrueSp()I access=0x41
-method getTrueSp()I signature=null
-method getTrueSp()I exceptions=
-method getTrueSp()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getTrueSp()I default=
-method getTrueSp()I ALOAD 0
-method getTrueSp()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getTrueSp()I
-method getTrueSp()I IRETURN
-method getWater()I access=0x41
-method getWater()I signature=null
-method getWater()I exceptions=
-method getWater()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getWater()I default=
-method getWater()I ALOAD 0
-method getWater()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getWater()I
-method getWater()I IRETURN
-method getWeaponBreakStack()I access=0x41
-method getWeaponBreakStack()I signature=null
-method getWeaponBreakStack()I exceptions=
-method getWeaponBreakStack()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getWeaponBreakStack()I default=
-method getWeaponBreakStack()I ALOAD 0
-method getWeaponBreakStack()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getWeaponBreakStack()I
-method getWeaponBreakStack()I IRETURN
-method getWind()I access=0x41
-method getWind()I signature=null
-method getWind()I exceptions=
-method getWind()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getWind()I default=
-method getWind()I ALOAD 0
-method getWind()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getWind()I
-method getWind()I IRETURN
-method getWis()S access=0x41
-method getWis()S signature=null
-method getWis()S exceptions=
-method getWis()S annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method getWis()S default=
-method getWis()S ALOAD 0
-method getWis()S INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.getWis()S
-method getWis()S IRETURN
 method get_deName()Llocal/lito381/server/NativeDeInitializerKernel$Name; access=0x1
 method get_deName()Llocal/lito381/server/NativeDeInitializerKernel$Name; signature=null
@@ -1729,77 +1314,4 @@
 method get_deName()Llocal/lito381/server/NativeDeInitializerKernel$Name; GETFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
 method get_deName()Llocal/lito381/server/NativeDeInitializerKernel$Name; ARETURN
-method get_deadTimerTemp()I access=0x41
-method get_deadTimerTemp()I signature=null
-method get_deadTimerTemp()I exceptions=
-method get_deadTimerTemp()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method get_deadTimerTemp()I default=
-method get_deadTimerTemp()I ALOAD 0
-method get_deadTimerTemp()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.get_deadTimerTemp()I
-method get_deadTimerTemp()I IRETURN
-method get_fishX()I access=0x41
-method get_fishX()I signature=null
-method get_fishX()I exceptions=
-method get_fishX()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method get_fishX()I default=
-method get_fishX()I ALOAD 0
-method get_fishX()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.get_fishX()I
-method get_fishX()I IRETURN
-method get_fishY()I access=0x41
-method get_fishY()I signature=null
-method get_fishY()I exceptions=
-method get_fishY()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method get_fishY()I default=
-method get_fishY()I ALOAD 0
-method get_fishY()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.get_fishY()I
-method get_fishY()I IRETURN
-method get_showId()I access=0x41
-method get_showId()I signature=null
-method get_showId()I exceptions=
-method get_showId()I annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method get_showId()I default=
-method get_showId()I ALOAD 0
-method get_showId()I INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.get_showId()I
-method get_showId()I IRETURN
-method hasSkillEffect(I)Z access=0x41
-method hasSkillEffect(I)Z signature=null
-method hasSkillEffect(I)Z exceptions=
-method hasSkillEffect(I)Z annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method hasSkillEffect(I)Z default=
-method hasSkillEffect(I)Z ALOAD 0
-method hasSkillEffect(I)Z ILOAD 1
-method hasSkillEffect(I)Z INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.hasSkillEffect(I)Z
-method hasSkillEffect(I)Z IRETURN
-method isAgro()Z access=0x41
-method isAgro()Z signature=null
-method isAgro()Z exceptions=
-method isAgro()Z annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method isAgro()Z default=
-method isAgro()Z ALOAD 0
-method isAgro()Z INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.isAgro()Z
-method isAgro()Z IRETURN
-method isAgrocoi()Z access=0x41
-method isAgrocoi()Z signature=null
-method isAgrocoi()Z exceptions=
-method isAgrocoi()Z annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method isAgrocoi()Z default=
-method isAgrocoi()Z ALOAD 0
-method isAgrocoi()Z INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.isAgrocoi()Z
-method isAgrocoi()Z IRETURN
-method isAgrososc()Z access=0x41
-method isAgrososc()Z signature=null
-method isAgrososc()Z exceptions=
-method isAgrososc()Z annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method isAgrososc()Z default=
-method isAgrososc()Z ALOAD 0
-method isAgrososc()Z INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.isAgrososc()Z
-method isAgrososc()Z IRETURN
-method isAiRunning()Z access=0x41
-method isAiRunning()Z signature=null
-method isAiRunning()Z exceptions=
-method isAiRunning()Z annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method isAiRunning()Z default=
-method isAiRunning()Z ALOAD 0
-method isAiRunning()Z INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.isAiRunning()Z
-method isAiRunning()Z IRETURN
 method isAutoBuffEnabled()Z access=0x1
 method isAutoBuffEnabled()Z signature=null
@@ -1816,7 +1328,5 @@
 method isBusy()Z default=
 method isBusy()Z ALOAD 0
-method isBusy()Z ASTORE 1
 method isBusy()Z ALOAD 0
-method isBusy()Z ALOAD 1
 method isBusy()Z INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$936e20649f8705e7cc7dbc41f9ddbd1399993ea8a1eac1d43577c1b42efb0e3b(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method isBusy()Z INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
@@ -1857,7 +1367,5 @@
 method isDeAiChain(J)Z default=
 method isDeAiChain(J)Z ALOAD 0
-method isDeAiChain(J)Z ASTORE 3
 method isDeAiChain(J)Z ALOAD 0
-method isDeAiChain(J)Z ALOAD 3
 method isDeAiChain(J)Z LLOAD 1/2
 method isDeAiChain(J)Z INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$85ee9f204e29e44faa49d2aeca343b5935b37e313cf88755970f34d4f4a552ae(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
@@ -1866,12 +1374,4 @@
 method isDeAiChain(J)Z INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
 method isDeAiChain(J)Z IRETURN
-method isDead()Z access=0x41
-method isDead()Z signature=null
-method isDead()Z exceptions=
-method isDead()Z annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method isDead()Z default=
-method isDead()Z ALOAD 0
-method isDead()Z INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.isDead()Z
-method isDead()Z IRETURN
 method isDeathMatchArenaCombatEnabled()Z access=0x1
 method isDeathMatchArenaCombatEnabled()Z signature=null
@@ -1882,12 +1382,4 @@
 method isDeathMatchArenaCombatEnabled()Z GETFIELD local/lito381/server/NativeDeInstanceState._deathMatchArenaCombatEnabled:Z
 method isDeathMatchArenaCombatEnabled()Z IRETURN
-method isFishing()Z access=0x41
-method isFishing()Z signature=null
-method isFishing()Z exceptions=
-method isFishing()Z annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method isFishing()Z default=
-method isFishing()Z ALOAD 0
-method isFishing()Z INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.isFishing()Z
-method isFishing()Z IRETURN
 method isInitFailed()Z access=0x1
 method isInitFailed()Z signature=null
@@ -1898,21 +1390,4 @@
 method isInitFailed()Z GETFIELD local/lito381/server/NativeDeInstanceState._initFailed:Z
 method isInitFailed()Z IRETURN
-method isInvisble()Z access=0x41
-method isInvisble()Z signature=null
-method isInvisble()Z exceptions=
-method isInvisble()Z annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method isInvisble()Z default=
-method isInvisble()Z ALOAD 0
-method isInvisble()Z INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.isInvisble()Z
-method isInvisble()Z IRETURN
-method isKnownNpcViewerConsistent(Llocal/lito381/server/NativeDeDeletionState;)Z access=0x41
-method isKnownNpcViewerConsistent(Llocal/lito381/server/NativeDeDeletionState;)Z signature=null
-method isKnownNpcViewerConsistent(Llocal/lito381/server/NativeDeDeletionState;)Z exceptions=
-method isKnownNpcViewerConsistent(Llocal/lito381/server/NativeDeDeletionState;)Z annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method isKnownNpcViewerConsistent(Llocal/lito381/server/NativeDeDeletionState;)Z default=
-method isKnownNpcViewerConsistent(Llocal/lito381/server/NativeDeDeletionState;)Z ALOAD 0
-method isKnownNpcViewerConsistent(Llocal/lito381/server/NativeDeDeletionState;)Z ALOAD 1
-method isKnownNpcViewerConsistent(Llocal/lito381/server/NativeDeDeletionState;)Z INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.isKnownNpcViewerConsistent(Llocal/lito381/server/NativeDeDeletionState;)Z
-method isKnownNpcViewerConsistent(Llocal/lito381/server/NativeDeDeletionState;)Z IRETURN
 method isMorphEnable()Z access=0x1
 method isMorphEnable()Z signature=null
@@ -1923,12 +1398,4 @@
 method isMorphEnable()Z GETFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z
 method isMorphEnable()Z IRETURN
-method isParalyzed()Z access=0x41
-method isParalyzed()Z signature=null
-method isParalyzed()Z exceptions=
-method isParalyzed()Z annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method isParalyzed()Z default=
-method isParalyzed()Z ALOAD 0
-method isParalyzed()Z INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.isParalyzed()Z
-method isParalyzed()Z IRETURN
 method isPickupItem()Z access=0x1
 method isPickupItem()Z signature=null
@@ -1938,29 +1405,4 @@
 method isPickupItem()Z CONST Integer:1
 method isPickupItem()Z IRETURN
-method isShop()Z access=0x41
-method isShop()Z signature=null
-method isShop()Z exceptions=
-method isShop()Z annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method isShop()Z default=
-method isShop()Z ALOAD 0
-method isShop()Z INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.isShop()Z
-method isShop()Z IRETURN
-method isSleeped()Z access=0x41
-method isSleeped()Z signature=null
-method isSleeped()Z exceptions=
-method isSleeped()Z annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method isSleeped()Z default=
-method isSleeped()Z ALOAD 0
-method isSleeped()Z INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.isSleeped()Z
-method isSleeped()Z IRETURN
-method knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z access=0x41
-method knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z signature=null
-method knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z exceptions=
-method knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z default=
-method knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z ALOAD 0
-method knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z ALOAD 1
-method knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
-method knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z IRETURN
 method lambda$content$03ab986d274dab0b583dff5bea1373a2c1738c4fbb65343cf52f51908e018f02(Llocal/lito381/server/NativeDeInstanceState;Z)Ljava/lang/Object; access=0xa
 method lambda$content$03ab986d274dab0b583dff5bea1373a2c1738c4fbb65343cf52f51908e018f02(Llocal/lito381/server/NativeDeInstanceState;Z)Ljava/lang/Object; signature=null
@@ -2032,16 +1474,4 @@
 method lambda$content$11013fa846631f85a6067673b91405b29fb63055f117caeb01b8aa77652fafd0(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object; INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 method lambda$content$11013fa846631f85a6067673b91405b29fb63055f117caeb01b8aa77652fafd0(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object; ARETURN
-method lambda$content$16b20c1235750a4df7a404ccaba5c9614ed65dd2ff271572c8f859735f6b492d(JJZ)Ljava/lang/Object; access=0x2
-method lambda$content$16b20c1235750a4df7a404ccaba5c9614ed65dd2ff271572c8f859735f6b492d(JJZ)Ljava/lang/Object; signature=null
-method lambda$content$16b20c1235750a4df7a404ccaba5c9614ed65dd2ff271572c8f859735f6b492d(JJZ)Ljava/lang/Object; exceptions=
-method lambda$content$16b20c1235750a4df7a404ccaba5c9614ed65dd2ff271572c8f859735f6b492d(JJZ)Ljava/lang/Object; annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method lambda$content$16b20c1235750a4df7a404ccaba5c9614ed65dd2ff271572c8f859735f6b492d(JJZ)Ljava/lang/Object; default=
-method lambda$content$16b20c1235750a4df7a404ccaba5c9614ed65dd2ff271572c8f859735f6b492d(JJZ)Ljava/lang/Object; ALOAD 0
-method lambda$content$16b20c1235750a4df7a404ccaba5c9614ed65dd2ff271572c8f859735f6b492d(JJZ)Ljava/lang/Object; LLOAD 1/2
-method lambda$content$16b20c1235750a4df7a404ccaba5c9614ed65dd2ff271572c8f859735f6b492d(JJZ)Ljava/lang/Object; LLOAD 3/4
-method lambda$content$16b20c1235750a4df7a404ccaba5c9614ed65dd2ff271572c8f859735f6b492d(JJZ)Ljava/lang/Object; ILOAD 5
-method lambda$content$16b20c1235750a4df7a404ccaba5c9614ed65dd2ff271572c8f859735f6b492d(JJZ)Ljava/lang/Object; INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$lambda$scheduleAiTick$0(JJZ)V
-method lambda$content$16b20c1235750a4df7a404ccaba5c9614ed65dd2ff271572c8f859735f6b492d(JJZ)Ljava/lang/Object; CONST null
-method lambda$content$16b20c1235750a4df7a404ccaba5c9614ed65dd2ff271572c8f859735f6b492d(JJZ)Ljava/lang/Object; ARETURN
 method lambda$content$22cc4f88a6c05bc6b6afb21cfb61b62f85d9393b8f0b85c0c1c6c4ea34a58402(Llocal/lito381/server/NativeDeInstanceState;JIZ)Ljava/lang/Object; access=0xa
 method lambda$content$22cc4f88a6c05bc6b6afb21cfb61b62f85d9393b8f0b85c0c1c6c4ea34a58402(Llocal/lito381/server/NativeDeInstanceState;JIZ)Ljava/lang/Object; signature=null
@@ -2113,18 +1543,16 @@
 method lambda$content$4fc1f089622da1cda0d2f54fc9ba0eac5b0b61de6580474227f7c3b36965c09d(Llocal/lito381/server/NativeDeInstanceState;JZ)Ljava/lang/Object; CONST null
 method lambda$content$4fc1f089622da1cda0d2f54fc9ba0eac5b0b61de6580474227f7c3b36965c09d(Llocal/lito381/server/NativeDeInstanceState;JZ)Ljava/lang/Object; ARETURN
-method lambda$content$5feeacb59af1588ceb051f44c110141b2d20ee22111af27780a45a783943a928(JJZ)V access=0x2
-method lambda$content$5feeacb59af1588ceb051f44c110141b2d20ee22111af27780a45a783943a928(JJZ)V signature=null
-method lambda$content$5feeacb59af1588ceb051f44c110141b2d20ee22111af27780a45a783943a928(JJZ)V exceptions=
-method lambda$content$5feeacb59af1588ceb051f44c110141b2d20ee22111af27780a45a783943a928(JJZ)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method lambda$content$5feeacb59af1588ceb051f44c110141b2d20ee22111af27780a45a783943a928(JJZ)V default=
-method lambda$content$5feeacb59af1588ceb051f44c110141b2d20ee22111af27780a45a783943a928(JJZ)V ALOAD 0
-method lambda$content$5feeacb59af1588ceb051f44c110141b2d20ee22111af27780a45a783943a928(JJZ)V ALOAD 0
-method lambda$content$5feeacb59af1588ceb051f44c110141b2d20ee22111af27780a45a783943a928(JJZ)V LLOAD 1/2
-method lambda$content$5feeacb59af1588ceb051f44c110141b2d20ee22111af27780a45a783943a928(JJZ)V LLOAD 3/4
-method lambda$content$5feeacb59af1588ceb051f44c110141b2d20ee22111af27780a45a783943a928(JJZ)V ILOAD 5
-method lambda$content$5feeacb59af1588ceb051f44c110141b2d20ee22111af27780a45a783943a928(JJZ)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H7 local/lito381/server/NativeDeInstanceState.lambda$content$16b20c1235750a4df7a404ccaba5c9614ed65dd2ff271572c8f859735f6b492d(JJZ)Ljava/lang/Object;, type:()Ljava/lang/Object;]
-method lambda$content$5feeacb59af1588ceb051f44c110141b2d20ee22111af27780a45a783943a928(JJZ)V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
-method lambda$content$5feeacb59af1588ceb051f44c110141b2d20ee22111af27780a45a783943a928(JJZ)V POP
-method lambda$content$5feeacb59af1588ceb051f44c110141b2d20ee22111af27780a45a783943a928(JJZ)V RETURN
+method lambda$content$55f34d9be55581ac35a1f796e5b24e62589e0beb5a01900e11f81d0e28322baf(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Object; access=0xa
+method lambda$content$55f34d9be55581ac35a1f796e5b24e62589e0beb5a01900e11f81d0e28322baf(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Object; signature=null
+method lambda$content$55f34d9be55581ac35a1f796e5b24e62589e0beb5a01900e11f81d0e28322baf(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Object; exceptions=
+method lambda$content$55f34d9be55581ac35a1f796e5b24e62589e0beb5a01900e11f81d0e28322baf(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Object; annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+method lambda$content$55f34d9be55581ac35a1f796e5b24e62589e0beb5a01900e11f81d0e28322baf(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Object; default=
+method lambda$content$55f34d9be55581ac35a1f796e5b24e62589e0beb5a01900e11f81d0e28322baf(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Object; ALOAD 0
+method lambda$content$55f34d9be55581ac35a1f796e5b24e62589e0beb5a01900e11f81d0e28322baf(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Object; LLOAD 1/2
+method lambda$content$55f34d9be55581ac35a1f796e5b24e62589e0beb5a01900e11f81d0e28322baf(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Object; LLOAD 3/4
+method lambda$content$55f34d9be55581ac35a1f796e5b24e62589e0beb5a01900e11f81d0e28322baf(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Object; ILOAD 5
+method lambda$content$55f34d9be55581ac35a1f796e5b24e62589e0beb5a01900e11f81d0e28322baf(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Object; INVOKEVIRTUAL local/lito381/server/NativeDeInstanceState.sourceAi$lambda$scheduleAiTick$0(JJZ)V
+method lambda$content$55f34d9be55581ac35a1f796e5b24e62589e0beb5a01900e11f81d0e28322baf(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Object; CONST null
+method lambda$content$55f34d9be55581ac35a1f796e5b24e62589e0beb5a01900e11f81d0e28322baf(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Object; ARETURN
 method lambda$content$6732b770fd6477193d9d92d5f6c09ca067e6200aef32d4933526361e2446e9b7(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object; access=0xa
 method lambda$content$6732b770fd6477193d9d92d5f6c09ca067e6200aef32d4933526361e2446e9b7(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object; signature=null
@@ -2178,4 +1606,18 @@
 method lambda$content$6d02416b33308c9d6589d9dc06a06174d8da49089ce2f137db9c8b07cb487537(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object; INVOKESTATIC java/lang/Boolean.valueOf(Z)Ljava/lang/Boolean;
 method lambda$content$6d02416b33308c9d6589d9dc06a06174d8da49089ce2f137db9c8b07cb487537(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object; ARETURN
+method lambda$content$6d279e5f5068f203c47c0d54162625e71322da60a5575507b438db26eb83f5e0(JJZ)V access=0x2
+method lambda$content$6d279e5f5068f203c47c0d54162625e71322da60a5575507b438db26eb83f5e0(JJZ)V signature=null
+method lambda$content$6d279e5f5068f203c47c0d54162625e71322da60a5575507b438db26eb83f5e0(JJZ)V exceptions=
+method lambda$content$6d279e5f5068f203c47c0d54162625e71322da60a5575507b438db26eb83f5e0(JJZ)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
+method lambda$content$6d279e5f5068f203c47c0d54162625e71322da60a5575507b438db26eb83f5e0(JJZ)V default=
+method lambda$content$6d279e5f5068f203c47c0d54162625e71322da60a5575507b438db26eb83f5e0(JJZ)V ALOAD 0
+method lambda$content$6d279e5f5068f203c47c0d54162625e71322da60a5575507b438db26eb83f5e0(JJZ)V ALOAD 0
+method lambda$content$6d279e5f5068f203c47c0d54162625e71322da60a5575507b438db26eb83f5e0(JJZ)V LLOAD 1/2
+method lambda$content$6d279e5f5068f203c47c0d54162625e71322da60a5575507b438db26eb83f5e0(JJZ)V LLOAD 3/4
+method lambda$content$6d279e5f5068f203c47c0d54162625e71322da60a5575507b438db26eb83f5e0(JJZ)V ILOAD 5
+method lambda$content$6d279e5f5068f203c47c0d54162625e71322da60a5575507b438db26eb83f5e0(JJZ)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$55f34d9be55581ac35a1f796e5b24e62589e0beb5a01900e11f81d0e28322baf(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Object;, type:()Ljava/lang/Object;]
+method lambda$content$6d279e5f5068f203c47c0d54162625e71322da60a5575507b438db26eb83f5e0(JJZ)V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
+method lambda$content$6d279e5f5068f203c47c0d54162625e71322da60a5575507b438db26eb83f5e0(JJZ)V POP
+method lambda$content$6d279e5f5068f203c47c0d54162625e71322da60a5575507b438db26eb83f5e0(JJZ)V RETURN
 method lambda$content$7564ba95bfa4f830ca5d54a5d82b624e8e155bbfecd75eb17ba8f12f34d5cf75(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object; access=0xa
 method lambda$content$7564ba95bfa4f830ca5d54a5d82b624e8e155bbfecd75eb17ba8f12f34d5cf75(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object; signature=null
@@ -2469,7 +1911,5 @@
 method nextAiRetryDelay(JZ)I default=
 method nextAiRetryDelay(JZ)I ALOAD 0
-method nextAiRetryDelay(JZ)I ASTORE 4
 method nextAiRetryDelay(JZ)I ALOAD 0
-method nextAiRetryDelay(JZ)I ALOAD 4
 method nextAiRetryDelay(JZ)I LLOAD 1/2
 method nextAiRetryDelay(JZ)I ILOAD 3
@@ -2485,7 +1925,5 @@
 method nextAiTimerTokenLocked()J default=
 method nextAiTimerTokenLocked()J ALOAD 0
-method nextAiTimerTokenLocked()J ASTORE 1
 method nextAiTimerTokenLocked()J ALOAD 0
-method nextAiTimerTokenLocked()J ALOAD 1
 method nextAiTimerTokenLocked()J INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$6784d6560a00c78a3361cfc2b7d12d2cfbe9831a4dfab273dda2d6d218e874d6(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method nextAiTimerTokenLocked()J INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
@@ -2499,7 +1937,5 @@
 method normalizeDeSleepTime(J)I default=
 method normalizeDeSleepTime(J)I ALOAD 0
-method normalizeDeSleepTime(J)I ASTORE 3
 method normalizeDeSleepTime(J)I ALOAD 0
-method normalizeDeSleepTime(J)I ALOAD 3
 method normalizeDeSleepTime(J)I LLOAD 1/2
 method normalizeDeSleepTime(J)I INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$11013fa846631f85a6067673b91405b29fb63055f117caeb01b8aa77652fafd0(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
@@ -2508,13 +1944,4 @@
 method normalizeDeSleepTime(J)I INVOKEVIRTUAL java/lang/Integer.intValue()I
 method normalizeDeSleepTime(J)I IRETURN
-method onAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V access=0x41
-method onAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V signature=null
-method onAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V exceptions=
-method onAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method onAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V default=
-method onAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V ALOAD 0
-method onAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V ALOAD 1
-method onAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.onAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
-method onAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V RETURN
 method onAiTimer(JJZ)V access=0x2
 method onAiTimer(JJZ)V signature=null
@@ -2523,7 +1950,5 @@
 method onAiTimer(JJZ)V default=
 method onAiTimer(JJZ)V ALOAD 0
-method onAiTimer(JJZ)V ASTORE 6
 method onAiTimer(JJZ)V ALOAD 0
-method onAiTimer(JJZ)V ALOAD 6
 method onAiTimer(JJZ)V LLOAD 1/2
 method onAiTimer(JJZ)V LLOAD 3/4
@@ -2539,46 +1964,9 @@
 method onBackgroundTick()V default=
 method onBackgroundTick()V ALOAD 0
-method onBackgroundTick()V ASTORE 1
 method onBackgroundTick()V ALOAD 0
-method onBackgroundTick()V ALOAD 1
 method onBackgroundTick()V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$4f8beabd3ff88fcd1ccde59bbcbe16d00c7633360a9a4524c71f944761bc2437(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method onBackgroundTick()V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 method onBackgroundTick()V POP
 method onBackgroundTick()V RETURN
-method onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V access=0x41
-method onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V signature=null
-method onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V exceptions=
-method onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V default=
-method onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V ALOAD 0
-method onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V ALOAD 1
-method onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
-method onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V RETURN
-method onTalkAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V access=0x41
-method onTalkAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V signature=null
-method onTalkAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V exceptions=
-method onTalkAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method onTalkAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V default=
-method onTalkAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V ALOAD 0
-method onTalkAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V ALOAD 1
-method onTalkAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.onTalkAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
-method onTalkAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V RETURN
-method rebindWorldMap(S)V access=0x41
-method rebindWorldMap(S)V signature=null
-method rebindWorldMap(S)V exceptions=
-method rebindWorldMap(S)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method rebindWorldMap(S)V default=
-method rebindWorldMap(S)V ALOAD 0
-method rebindWorldMap(S)V ILOAD 1
-method rebindWorldMap(S)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.rebindWorldMap(S)V
-method rebindWorldMap(S)V RETURN
-method removeAllKnownObjects()V access=0x41
-method removeAllKnownObjects()V signature=null
-method removeAllKnownObjects()V exceptions=
-method removeAllKnownObjects()V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method removeAllKnownObjects()V default=
-method removeAllKnownObjects()V ALOAD 0
-method removeAllKnownObjects()V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.removeAllKnownObjects()V
-method removeAllKnownObjects()V RETURN
 method removeFromDeNameList()V access=0x1
 method removeFromDeNameList()V signature=null
@@ -2614,31 +2002,4 @@
 method removeFromDeNameList()V LABEL L3
 method removeFromDeNameList()V RETURN
-method removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V access=0x41
-method removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V signature=null
-method removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V exceptions=
-method removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V default=
-method removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V ALOAD 0
-method removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V ALOAD 1
-method removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
-method removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V RETURN
-method removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z access=0x41
-method removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z signature=null
-method removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z exceptions=
-method removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z default=
-method removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z ALOAD 0
-method removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z ALOAD 1
-method removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
-method removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z IRETURN
-method resurrect(I)V access=0x41
-method resurrect(I)V signature=null
-method resurrect(I)V exceptions=
-method resurrect(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method resurrect(I)V default=
-method resurrect(I)V ALOAD 0
-method resurrect(I)V ILOAD 1
-method resurrect(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.resurrect(I)V
-method resurrect(I)V RETURN
 method run()V access=0x1
 method run()V signature=null
@@ -2647,7 +2008,5 @@
 method run()V default=
 method run()V ALOAD 0
-method run()V ASTORE 1
 method run()V ALOAD 0
-method run()V ALOAD 1
 method run()V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$29f3f3c7f5a8c2e93ee9f4d479292a805648f797e66af39b149f74a69e66301d(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method run()V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
@@ -2660,7 +2019,5 @@
 method runAiTick(J)V default=
 method runAiTick(J)V ALOAD 0
-method runAiTick(J)V ASTORE 3
 method runAiTick(J)V ALOAD 0
-method runAiTick(J)V ALOAD 3
 method runAiTick(J)V LLOAD 1/2
 method runAiTick(J)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$e82873d66d920423a3e23a0f790f2c26c537b159307e4aa3d5a64233fe446ba0(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
@@ -2674,7 +2031,5 @@
 method runBackgroundTick(J)V default=
 method runBackgroundTick(J)V ALOAD 0
-method runBackgroundTick(J)V ASTORE 3
 method runBackgroundTick(J)V ALOAD 0
-method runBackgroundTick(J)V ALOAD 3
 method runBackgroundTick(J)V LLOAD 1/2
 method runBackgroundTick(J)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$266210398aadeefd0252fcae301d0b18a0bfcdb60d62bdb62bb87ad247008ee4(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
@@ -2688,7 +2043,5 @@
 method scheduleAiTick(JI)Ljava/util/concurrent/ScheduledFuture; default=
 method scheduleAiTick(JI)Ljava/util/concurrent/ScheduledFuture; ALOAD 0
-method scheduleAiTick(JI)Ljava/util/concurrent/ScheduledFuture; ASTORE 4
 method scheduleAiTick(JI)Ljava/util/concurrent/ScheduledFuture; ALOAD 0
-method scheduleAiTick(JI)Ljava/util/concurrent/ScheduledFuture; ALOAD 4
 method scheduleAiTick(JI)Ljava/util/concurrent/ScheduledFuture; LLOAD 1/2
 method scheduleAiTick(JI)Ljava/util/concurrent/ScheduledFuture; ILOAD 3
@@ -2703,7 +2056,5 @@
 method scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; default=
 method scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; ALOAD 0
-method scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; ASTORE 5
 method scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; ALOAD 0
-method scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; ALOAD 5
 method scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; LLOAD 1/2
 method scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; ILOAD 3
@@ -2719,7 +2070,5 @@
 method scheduleNextAiTick(J)V default=
 method scheduleNextAiTick(J)V ALOAD 0
-method scheduleNextAiTick(J)V ASTORE 3
 method scheduleNextAiTick(J)V ALOAD 0
-method scheduleNextAiTick(J)V ALOAD 3
 method scheduleNextAiTick(J)V LLOAD 1/2
 method scheduleNextAiTick(J)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$8138e935c5a0eb8baca8c7df44b1df5d6696d55f21c0f24af39f8d3773d90b2e(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
@@ -2727,49 +2076,4 @@
 method scheduleNextAiTick(J)V POP
 method scheduleNextAiTick(J)V RETURN
-method setAc(I)V access=0x41
-method setAc(I)V signature=null
-method setAc(I)V exceptions=
-method setAc(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setAc(I)V default=
-method setAc(I)V ALOAD 0
-method setAc(I)V ILOAD 1
-method setAc(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setAc(I)V
-method setAc(I)V RETURN
-method setAddAttrKind(I)V access=0x41
-method setAddAttrKind(I)V signature=null
-method setAddAttrKind(I)V exceptions=
-method setAddAttrKind(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setAddAttrKind(I)V default=
-method setAddAttrKind(I)V ALOAD 0
-method setAddAttrKind(I)V ILOAD 1
-method setAddAttrKind(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setAddAttrKind(I)V
-method setAddAttrKind(I)V RETURN
-method setAgro(Z)V access=0x41
-method setAgro(Z)V signature=null
-method setAgro(Z)V exceptions=
-method setAgro(Z)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setAgro(Z)V default=
-method setAgro(Z)V ALOAD 0
-method setAgro(Z)V ILOAD 1
-method setAgro(Z)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setAgro(Z)V
-method setAgro(Z)V RETURN
-method setAgrocoi(Z)V access=0x41
-method setAgrocoi(Z)V signature=null
-method setAgrocoi(Z)V exceptions=
-method setAgrocoi(Z)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setAgrocoi(Z)V default=
-method setAgrocoi(Z)V ALOAD 0
-method setAgrocoi(Z)V ILOAD 1
-method setAgrocoi(Z)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setAgrocoi(Z)V
-method setAgrocoi(Z)V RETURN
-method setAgrososc(Z)V access=0x41
-method setAgrososc(Z)V signature=null
-method setAgrososc(Z)V exceptions=
-method setAgrososc(Z)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setAgrososc(Z)V default=
-method setAgrososc(Z)V ALOAD 0
-method setAgrososc(Z)V ILOAD 1
-method setAgrososc(Z)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setAgrososc(Z)V
-method setAgrososc(Z)V RETURN
 method setAiRunning(Z)V access=0x20001
 method setAiRunning(Z)V signature=null
@@ -2778,7 +2082,5 @@
 method setAiRunning(Z)V default=
 method setAiRunning(Z)V ALOAD 0
-method setAiRunning(Z)V ASTORE 2
 method setAiRunning(Z)V ALOAD 0
-method setAiRunning(Z)V ALOAD 2
 method setAiRunning(Z)V ILOAD 1
 method setAiRunning(Z)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;Z)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$03ab986d274dab0b583dff5bea1373a2c1738c4fbb65343cf52f51908e018f02(Llocal/lito381/server/NativeDeInstanceState;Z)Ljava/lang/Object;, type:()Ljava/lang/Object;]
@@ -2795,13 +2097,4 @@
 method setAiType(I)V PUTFIELD local/lito381/server/NativeDePerceptionState._aiType:I
 method setAiType(I)V RETURN
-method setAtkspeed(I)V access=0x41
-method setAtkspeed(I)V signature=null
-method setAtkspeed(I)V exceptions=
-method setAtkspeed(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setAtkspeed(I)V default=
-method setAtkspeed(I)V ALOAD 0
-method setAtkspeed(I)V ILOAD 1
-method setAtkspeed(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setAtkspeed(I)V
-method setAtkspeed(I)V RETURN
 method setAutoBuffChance(I)V access=0x1
 method setAutoBuffChance(I)V signature=null
@@ -2831,31 +2124,4 @@
 method setAutoBuffInterval(I)V PUTFIELD local/lito381/server/NativeDeInstanceState._autoBuffInterval:I
 method setAutoBuffInterval(I)V RETURN
-method setBraveSpeed(I)V access=0x41
-method setBraveSpeed(I)V signature=null
-method setBraveSpeed(I)V exceptions=
-method setBraveSpeed(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setBraveSpeed(I)V default=
-method setBraveSpeed(I)V ALOAD 0
-method setBraveSpeed(I)V ILOAD 1
-method setBraveSpeed(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setBraveSpeed(I)V
-method setBraveSpeed(I)V RETURN
-method setCha(I)V access=0x41
-method setCha(I)V signature=null
-method setCha(I)V exceptions=
-method setCha(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setCha(I)V default=
-method setCha(I)V ALOAD 0
-method setCha(I)V ILOAD 1
-method setCha(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setCha(I)V
-method setCha(I)V RETURN
-method setChaLightSize(I)V access=0x41
-method setChaLightSize(I)V signature=null
-method setChaLightSize(I)V exceptions=
-method setChaLightSize(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setChaLightSize(I)V default=
-method setChaLightSize(I)V ALOAD 0
-method setChaLightSize(I)V ILOAD 1
-method setChaLightSize(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setChaLightSize(I)V
-method setChaLightSize(I)V RETURN
 method setClanid(I)V access=0x1
 method setClanid(I)V signature=null
@@ -2894,13 +2160,4 @@
 method setCombatBehavior(Llocal/lito381/server/NativeDeInstancePorts$Combat;)V PUTFIELD local/lito381/server/NativeDeInstanceState._combatBehavior:Llocal/lito381/server/NativeDeInstancePorts$Combat;
 method setCombatBehavior(Llocal/lito381/server/NativeDeInstancePorts$Combat;)V RETURN
-method setCon(I)V access=0x41
-method setCon(I)V signature=null
-method setCon(I)V exceptions=
-method setCon(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setCon(I)V default=
-method setCon(I)V ALOAD 0
-method setCon(I)V ILOAD 1
-method setCon(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setCon(I)V
-method setCon(I)V RETURN
 method setCurrentWeapon(I)V access=0x1
 method setCurrentWeapon(I)V signature=null
@@ -2932,58 +2189,4 @@
 method setDeathMatchArenaCombatEnabled(Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._deathMatchArenaCombatEnabled:Z
 method setDeathMatchArenaCombatEnabled(Z)V RETURN
-method setDex(I)V access=0x41
-method setDex(I)V signature=null
-method setDex(I)V exceptions=
-method setDex(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setDex(I)V default=
-method setDex(I)V ALOAD 0
-method setDex(I)V ILOAD 1
-method setDex(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setDex(I)V
-method setDex(I)V RETURN
-method setExp(J)V access=0x41
-method setExp(J)V signature=null
-method setExp(J)V exceptions=
-method setExp(J)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setExp(J)V default=
-method setExp(J)V ALOAD 0
-method setExp(J)V LLOAD 1/2
-method setExp(J)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setExp(J)V
-method setExp(J)V RETURN
-method setGfxId(I)V access=0x41
-method setGfxId(I)V signature=null
-method setGfxId(I)V exceptions=
-method setGfxId(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setGfxId(I)V default=
-method setGfxId(I)V ALOAD 0
-method setGfxId(I)V ILOAD 1
-method setGfxId(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setGfxId(I)V
-method setGfxId(I)V RETURN
-method setGfxidInStatus(I)V access=0x41
-method setGfxidInStatus(I)V signature=null
-method setGfxidInStatus(I)V exceptions=
-method setGfxidInStatus(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setGfxidInStatus(I)V default=
-method setGfxidInStatus(I)V ALOAD 0
-method setGfxidInStatus(I)V ILOAD 1
-method setGfxidInStatus(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setGfxidInStatus(I)V
-method setGfxidInStatus(I)V RETURN
-method setHeading(I)V access=0x41
-method setHeading(I)V signature=null
-method setHeading(I)V exceptions=
-method setHeading(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setHeading(I)V default=
-method setHeading(I)V ALOAD 0
-method setHeading(I)V ILOAD 1
-method setHeading(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setHeading(I)V
-method setHeading(I)V RETURN
-method setId(I)V access=0x41
-method setId(I)V signature=null
-method setId(I)V exceptions=
-method setId(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setId(I)V default=
-method setId(I)V ALOAD 0
-method setId(I)V ILOAD 1
-method setId(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setId(I)V
-method setId(I)V RETURN
 method setInitFailed(Z)V access=0x1
 method setInitFailed(Z)V signature=null
@@ -2995,22 +2198,4 @@
 method setInitFailed(Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._initFailed:Z
 method setInitFailed(Z)V RETURN
-method setInt(I)V access=0x41
-method setInt(I)V signature=null
-method setInt(I)V exceptions=
-method setInt(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setInt(I)V default=
-method setInt(I)V ALOAD 0
-method setInt(I)V ILOAD 1
-method setInt(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setInt(I)V
-method setInt(I)V RETURN
-method setKarma(I)V access=0x41
-method setKarma(I)V signature=null
-method setKarma(I)V exceptions=
-method setKarma(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setKarma(I)V default=
-method setKarma(I)V ALOAD 0
-method setKarma(I)V ILOAD 1
-method setKarma(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setKarma(I)V
-method setKarma(I)V RETURN
 method setLastAutoBuffTime(J)V access=0x1
 method setLastAutoBuffTime(J)V signature=null
@@ -3022,87 +2207,4 @@
 method setLastAutoBuffTime(J)V PUTFIELD local/lito381/server/NativeDeInstanceState._lastAutoBuffTime:J
 method setLastAutoBuffTime(J)V RETURN
-method setLawful(I)V access=0x41
-method setLawful(I)V signature=null
-method setLawful(I)V exceptions=
-method setLawful(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setLawful(I)V default=
-method setLawful(I)V ALOAD 0
-method setLawful(I)V ILOAD 1
-method setLawful(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setLawful(I)V
-method setLawful(I)V RETURN
-method setLevel(I)V access=0x41
-method setLevel(I)V signature=null
-method setLevel(I)V exceptions=
-method setLevel(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setLevel(I)V default=
-method setLevel(I)V ALOAD 0
-method setLevel(I)V ILOAD 1
-method setLevel(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setLevel(I)V
-method setLevel(I)V RETURN
-method setLightSize(I)V access=0x41
-method setLightSize(I)V signature=null
-method setLightSize(I)V exceptions=
-method setLightSize(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setLightSize(I)V default=
-method setLightSize(I)V ALOAD 0
-method setLightSize(I)V ILOAD 1
-method setLightSize(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setLightSize(I)V
-method setLightSize(I)V RETURN
-method setLocation(III)V access=0x41
-method setLocation(III)V signature=null
-method setLocation(III)V exceptions=
-method setLocation(III)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setLocation(III)V default=
-method setLocation(III)V ALOAD 0
-method setLocation(III)V ILOAD 1
-method setLocation(III)V ILOAD 2
-method setLocation(III)V ILOAD 3
-method setLocation(III)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setLocation(III)V
-method setLocation(III)V RETURN
-method setLocation(Llocal/lito381/server/NativeDeWorldLocation;)V access=0x41
-method setLocation(Llocal/lito381/server/NativeDeWorldLocation;)V signature=null
-method setLocation(Llocal/lito381/server/NativeDeWorldLocation;)V exceptions=
-method setLocation(Llocal/lito381/server/NativeDeWorldLocation;)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setLocation(Llocal/lito381/server/NativeDeWorldLocation;)V default=
-method setLocation(Llocal/lito381/server/NativeDeWorldLocation;)V ALOAD 0
-method setLocation(Llocal/lito381/server/NativeDeWorldLocation;)V ALOAD 1
-method setLocation(Llocal/lito381/server/NativeDeWorldLocation;)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setLocation(Llocal/lito381/server/NativeDeWorldLocation;)V
-method setLocation(Llocal/lito381/server/NativeDeWorldLocation;)V RETURN
-method setMap(Llocal/lito381/server/NativeDeWorldMap;)V access=0x41
-method setMap(Llocal/lito381/server/NativeDeWorldMap;)V signature=null
-method setMap(Llocal/lito381/server/NativeDeWorldMap;)V exceptions=
-method setMap(Llocal/lito381/server/NativeDeWorldMap;)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setMap(Llocal/lito381/server/NativeDeWorldMap;)V default=
-method setMap(Llocal/lito381/server/NativeDeWorldMap;)V ALOAD 0
-method setMap(Llocal/lito381/server/NativeDeWorldMap;)V ALOAD 1
-method setMap(Llocal/lito381/server/NativeDeWorldMap;)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setMap(Llocal/lito381/server/NativeDeWorldMap;)V
-method setMap(Llocal/lito381/server/NativeDeWorldMap;)V RETURN
-method setMap(S)V access=0x41
-method setMap(S)V signature=null
-method setMap(S)V exceptions=
-method setMap(S)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setMap(S)V default=
-method setMap(S)V ALOAD 0
-method setMap(S)V ILOAD 1
-method setMap(S)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setMap(S)V
-method setMap(S)V RETURN
-method setMaxHp(I)V access=0x41
-method setMaxHp(I)V signature=null
-method setMaxHp(I)V exceptions=
-method setMaxHp(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setMaxHp(I)V default=
-method setMaxHp(I)V ALOAD 0
-method setMaxHp(I)V ILOAD 1
-method setMaxHp(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setMaxHp(I)V
-method setMaxHp(I)V RETURN
-method setMaxMp(I)V access=0x41
-method setMaxMp(I)V signature=null
-method setMaxMp(I)V exceptions=
-method setMaxMp(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setMaxMp(I)V default=
-method setMaxMp(I)V ALOAD 0
-method setMaxMp(I)V ILOAD 1
-method setMaxMp(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setMaxMp(I)V
-method setMaxMp(I)V RETURN
 method setMorphEnable(Z)V access=0x1
 method setMorphEnable(Z)V signature=null
@@ -3114,40 +2216,4 @@
 method setMorphEnable(Z)V PUTFIELD local/lito381/server/NativeDeInstanceState._isMorphEnable:Z
 method setMorphEnable(Z)V RETURN
-method setMoveSpeed(I)V access=0x41
-method setMoveSpeed(I)V signature=null
-method setMoveSpeed(I)V exceptions=
-method setMoveSpeed(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setMoveSpeed(I)V default=
-method setMoveSpeed(I)V ALOAD 0
-method setMoveSpeed(I)V ILOAD 1
-method setMoveSpeed(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setMoveSpeed(I)V
-method setMoveSpeed(I)V RETURN
-method setMr(I)V access=0x41
-method setMr(I)V signature=null
-method setMr(I)V exceptions=
-method setMr(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setMr(I)V default=
-method setMr(I)V ALOAD 0
-method setMr(I)V ILOAD 1
-method setMr(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setMr(I)V
-method setMr(I)V RETURN
-method setName(Ljava/lang/String;)V access=0x41
-method setName(Ljava/lang/String;)V signature=null
-method setName(Ljava/lang/String;)V exceptions=
-method setName(Ljava/lang/String;)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setName(Ljava/lang/String;)V default=
-method setName(Ljava/lang/String;)V ALOAD 0
-method setName(Ljava/lang/String;)V ALOAD 1
-method setName(Ljava/lang/String;)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setName(Ljava/lang/String;)V
-method setName(Ljava/lang/String;)V RETURN
-method setNameId(Ljava/lang/String;)V access=0x41
-method setNameId(Ljava/lang/String;)V signature=null
-method setNameId(Ljava/lang/String;)V exceptions=
-method setNameId(Ljava/lang/String;)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setNameId(Ljava/lang/String;)V default=
-method setNameId(Ljava/lang/String;)V ALOAD 0
-method setNameId(Ljava/lang/String;)V ALOAD 1
-method setNameId(Ljava/lang/String;)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setNameId(Ljava/lang/String;)V
-method setNameId(Ljava/lang/String;)V RETURN
 method setNpcSpeed()V access=0x1
 method setNpcSpeed()V signature=null
@@ -3156,56 +2222,9 @@
 method setNpcSpeed()V default=
 method setNpcSpeed()V ALOAD 0
-method setNpcSpeed()V ASTORE 1
 method setNpcSpeed()V ALOAD 0
-method setNpcSpeed()V ALOAD 1
 method setNpcSpeed()V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$ef695f35fdb5da1ac6acdb7398855142a8ea8de87efbf3dfae1ef69668275116(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method setNpcSpeed()V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
 method setNpcSpeed()V POP
 method setNpcSpeed()V RETURN
-method setOwnLightSize(I)V access=0x41
-method setOwnLightSize(I)V signature=null
-method setOwnLightSize(I)V exceptions=
-method setOwnLightSize(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setOwnLightSize(I)V default=
-method setOwnLightSize(I)V ALOAD 0
-method setOwnLightSize(I)V ILOAD 1
-method setOwnLightSize(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setOwnLightSize(I)V
-method setOwnLightSize(I)V RETURN
-method setPassispeed(I)V access=0x41
-method setPassispeed(I)V signature=null
-method setPassispeed(I)V exceptions=
-method setPassispeed(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setPassispeed(I)V default=
-method setPassispeed(I)V ALOAD 0
-method setPassispeed(I)V ILOAD 1
-method setPassispeed(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setPassispeed(I)V
-method setPassispeed(I)V RETURN
-method setPickupItem(Z)V access=0x41
-method setPickupItem(Z)V signature=null
-method setPickupItem(Z)V exceptions=
-method setPickupItem(Z)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setPickupItem(Z)V default=
-method setPickupItem(Z)V ALOAD 0
-method setPickupItem(Z)V ILOAD 1
-method setPickupItem(Z)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setPickupItem(Z)V
-method setPickupItem(Z)V RETURN
-method setStatus(I)V access=0x41
-method setStatus(I)V signature=null
-method setStatus(I)V exceptions=
-method setStatus(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setStatus(I)V default=
-method setStatus(I)V ALOAD 0
-method setStatus(I)V ILOAD 1
-method setStatus(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setStatus(I)V
-method setStatus(I)V RETURN
-method setStr(I)V access=0x41
-method setStr(I)V signature=null
-method setStr(I)V exceptions=
-method setStr(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setStr(I)V default=
-method setStr(I)V ALOAD 0
-method setStr(I)V ILOAD 1
-method setStr(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setStr(I)V
-method setStr(I)V RETURN
 method setTargetItem(Llocal/lito381/server/NativeDeWorldKinds$Item;)V access=0x1
 method setTargetItem(Llocal/lito381/server/NativeDeWorldKinds$Item;)V signature=null
@@ -3217,77 +2236,4 @@
 method setTargetItem(Llocal/lito381/server/NativeDeWorldKinds$Item;)V PUTFIELD local/lito381/server/NativeDeNpcConstructionState._targetItem:Llocal/lito381/server/NativeDeWorldKinds$Item;
 method setTargetItem(Llocal/lito381/server/NativeDeWorldKinds$Item;)V RETURN
-method setTempCharGfx(I)V access=0x41
-method setTempCharGfx(I)V signature=null
-method setTempCharGfx(I)V exceptions=
-method setTempCharGfx(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setTempCharGfx(I)V default=
-method setTempCharGfx(I)V ALOAD 0
-method setTempCharGfx(I)V ILOAD 1
-method setTempCharGfx(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setTempCharGfx(I)V
-method setTempCharGfx(I)V RETURN
-method setTempLawful(I)V access=0x41
-method setTempLawful(I)V signature=null
-method setTempLawful(I)V exceptions=
-method setTempLawful(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setTempLawful(I)V default=
-method setTempLawful(I)V ALOAD 0
-method setTempLawful(I)V ILOAD 1
-method setTempLawful(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setTempLawful(I)V
-method setTempLawful(I)V RETURN
-method setTitle(Ljava/lang/String;)V access=0x41
-method setTitle(Ljava/lang/String;)V signature=null
-method setTitle(Ljava/lang/String;)V exceptions=
-method setTitle(Ljava/lang/String;)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setTitle(Ljava/lang/String;)V default=
-method setTitle(Ljava/lang/String;)V ALOAD 0
-method setTitle(Ljava/lang/String;)V ALOAD 1
-method setTitle(Ljava/lang/String;)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setTitle(Ljava/lang/String;)V
-method setTitle(Ljava/lang/String;)V RETURN
-method setWeaponBreakStack(I)V access=0x41
-method setWeaponBreakStack(I)V signature=null
-method setWeaponBreakStack(I)V exceptions=
-method setWeaponBreakStack(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setWeaponBreakStack(I)V default=
-method setWeaponBreakStack(I)V ALOAD 0
-method setWeaponBreakStack(I)V ILOAD 1
-method setWeaponBreakStack(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setWeaponBreakStack(I)V
-method setWeaponBreakStack(I)V RETURN
-method setWis(I)V access=0x41
-method setWis(I)V signature=null
-method setWis(I)V exceptions=
-method setWis(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setWis(I)V default=
-method setWis(I)V ALOAD 0
-method setWis(I)V ILOAD 1
-method setWis(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setWis(I)V
-method setWis(I)V RETURN
-method setX(I)V access=0x41
-method setX(I)V signature=null
-method setX(I)V exceptions=
-method setX(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setX(I)V default=
-method setX(I)V ALOAD 0
-method setX(I)V ILOAD 1
-method setX(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setX(I)V
-method setX(I)V RETURN
-method setXY(II)V access=0x41
-method setXY(II)V signature=null
-method setXY(II)V exceptions=
-method setXY(II)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setXY(II)V default=
-method setXY(II)V ALOAD 0
-method setXY(II)V ILOAD 1
-method setXY(II)V ILOAD 2
-method setXY(II)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setXY(II)V
-method setXY(II)V RETURN
-method setY(I)V access=0x41
-method setY(I)V signature=null
-method setY(I)V exceptions=
-method setY(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setY(I)V default=
-method setY(I)V ALOAD 0
-method setY(I)V ILOAD 1
-method setY(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setY(I)V
-method setY(I)V RETURN
 method set_clanid(I)V access=0x1
 method set_clanid(I)V signature=null
@@ -3322,13 +2268,4 @@
 method set_deName(Llocal/lito381/server/NativeDeInitializerKernel$Name;)V PUTFIELD local/lito381/server/NativeDeDeletionState.deName:Llocal/lito381/server/NativeDeInitializerKernel$Name;
 method set_deName(Llocal/lito381/server/NativeDeInitializerKernel$Name;)V RETURN
-method set_deadTimerTemp(I)V access=0x41
-method set_deadTimerTemp(I)V signature=null
-method set_deadTimerTemp(I)V exceptions=
-method set_deadTimerTemp(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method set_deadTimerTemp(I)V default=
-method set_deadTimerTemp(I)V ALOAD 0
-method set_deadTimerTemp(I)V ILOAD 1
-method set_deadTimerTemp(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.set_deadTimerTemp(I)V
-method set_deadTimerTemp(I)V RETURN
 method set_emblem(Llocal/lito381/server/NativeDeInitializerKernel$Emblem;)V access=0x1
 method set_emblem(Llocal/lito381/server/NativeDeInitializerKernel$Emblem;)V signature=null
@@ -3359,22 +2296,4 @@
 method set_emblem(Llocal/lito381/server/NativeDePerceptionEmblemData;)V PUTFIELD local/lito381/server/NativeDeInstanceState._emblem:Llocal/lito381/server/NativeDePerceptionEmblemData;
 method set_emblem(Llocal/lito381/server/NativeDePerceptionEmblemData;)V RETURN
-method set_showId(I)V access=0x41
-method set_showId(I)V signature=null
-method set_showId(I)V exceptions=
-method set_showId(I)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method set_showId(I)V default=
-method set_showId(I)V ALOAD 0
-method set_showId(I)V ILOAD 1
-method set_showId(I)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.set_showId(I)V
-method set_showId(I)V RETURN
-method setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V access=0x41
-method setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V signature=null
-method setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V exceptions=
-method setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V default=
-method setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 0
-method setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V ALOAD 1
-method setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V
-method setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V RETURN
 method sourceAi$beginAiTick(J)Z access=0x2
 method sourceAi$beginAiTick(J)Z signature=null
@@ -5099,5 +4018,5 @@
 method sourceAi$scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; LLOAD 6/7
 method sourceAi$scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; ILOAD 4
-method sourceAi$scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; INVOKEDYNAMIC run(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H7 local/lito381/server/NativeDeInstanceState.lambda$content$5feeacb59af1588ceb051f44c110141b2d20ee22111af27780a45a783943a928(JJZ)V, type:()V]
+method sourceAi$scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; INVOKEDYNAMIC run(Llocal/lito381/server/NativeDeInstanceState;JJZ)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H7 local/lito381/server/NativeDeInstanceState.lambda$content$6d279e5f5068f203c47c0d54162625e71322da60a5575507b438db26eb83f5e0(JJZ)V, type:()V]
 method sourceAi$scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; CONST Integer:0
 method sourceAi$scheduleAiTick(JIZ)Ljava/util/concurrent/ScheduledFuture; ILOAD 3
@@ -5765,12 +4684,4 @@
 method sourceAi$wakeup(J)V LABEL L17
 method sourceAi$wakeup(J)V RETURN
-method sourceDeleteDe()V access=0x41
-method sourceDeleteDe()V signature=null
-method sourceDeleteDe()V exceptions=
-method sourceDeleteDe()V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method sourceDeleteDe()V default=
-method sourceDeleteDe()V ALOAD 0
-method sourceDeleteDe()V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.sourceDeleteDe()V
-method sourceDeleteDe()V RETURN
 method startAI()V access=0x1
 method startAI()V signature=null
@@ -5779,7 +4690,5 @@
 method startAI()V default=
 method startAI()V ALOAD 0
-method startAI()V ASTORE 1
 method startAI()V ALOAD 0
-method startAI()V ALOAD 1
 method startAI()V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$fd1ba82fa5bc2d3af41ae9b55c3c9cb9056616c975a4888758aeeed3363fe0c9(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method startAI()V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
@@ -5840,12 +4749,4 @@
 method startNpc()V LABEL L1
 method startNpc()V RETURN
-method start_shop()V access=0x41
-method start_shop()V signature=null
-method start_shop()V exceptions=
-method start_shop()V annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method start_shop()V default=
-method start_shop()V ALOAD 0
-method start_shop()V INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.start_shop()V
-method start_shop()V RETURN
 method stopAI()V access=0x1
 method stopAI()V signature=null
@@ -5854,7 +4755,5 @@
 method stopAI()V default=
 method stopAI()V ALOAD 0
-method stopAI()V ASTORE 1
 method stopAI()V ALOAD 0
-method stopAI()V ALOAD 1
 method stopAI()V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$7d57c00e9c6f7cbe762d09ac22a110a83cfde57b1ae8f532d8e7537081e20621(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method stopAI()V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
@@ -5867,7 +4766,5 @@
 method stopAiOwner(J)V default=
 method stopAiOwner(J)V ALOAD 0
-method stopAiOwner(J)V ASTORE 3
 method stopAiOwner(J)V ALOAD 0
-method stopAiOwner(J)V ALOAD 3
 method stopAiOwner(J)V LLOAD 1/2
 method stopAiOwner(J)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$f04c709ad951306a250d2bfbb46e2f198ca65818d23e88b3e7a5b669730bfec9(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
@@ -5881,7 +4778,5 @@
 method submitAiTick(J)V default=
 method submitAiTick(J)V ALOAD 0
-method submitAiTick(J)V ASTORE 3
 method submitAiTick(J)V ALOAD 0
-method submitAiTick(J)V ALOAD 3
 method submitAiTick(J)V LLOAD 1/2
 method submitAiTick(J)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$e35014986baf8453b7ce6346aa779329f2e452f7dc6925e7d27a61bbdb49368f(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
@@ -5889,13 +4784,4 @@
 method submitAiTick(J)V POP
 method submitAiTick(J)V RETURN
-method tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z access=0x41
-method tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z signature=null
-method tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z exceptions=
-method tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-method tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z default=
-method tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z ALOAD 0
-method tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z ALOAD 1
-method tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
-method tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z IRETURN
 method updateNextActionTime(I)V access=0x1
 method updateNextActionTime(I)V signature=null
@@ -5904,7 +4790,5 @@
 method updateNextActionTime(I)V default=
 method updateNextActionTime(I)V ALOAD 0
-method updateNextActionTime(I)V ASTORE 2
 method updateNextActionTime(I)V ALOAD 0
-method updateNextActionTime(I)V ALOAD 2
 method updateNextActionTime(I)V ILOAD 1
 method updateNextActionTime(I)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;I)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$798fb3755f799fed2904209a1203df8a88d7779f257160cb1e6124192e4ab099(Llocal/lito381/server/NativeDeInstanceState;I)Ljava/lang/Object;, type:()Ljava/lang/Object;]
@@ -5918,7 +4802,5 @@
 method wakeup()V default=
 method wakeup()V ALOAD 0
-method wakeup()V ASTORE 1
 method wakeup()V ALOAD 0
-method wakeup()V ALOAD 1
 method wakeup()V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$b867129d95dec223ced7e0c959814ebf79437ebe7e890195ed46b5795a4487db(Llocal/lito381/server/NativeDeInstanceState;)Ljava/lang/Object;, type:()Ljava/lang/Object;]
 method wakeup()V INVOKESTATIC local/lito381/server/NativeDeLiveAiScope.call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object;
@@ -5931,7 +4813,5 @@
 method wakeup(J)V default=
 method wakeup(J)V ALOAD 0
-method wakeup(J)V ASTORE 3
 method wakeup(J)V ALOAD 0
-method wakeup(J)V ALOAD 3
 method wakeup(J)V LLOAD 1/2
 method wakeup(J)V INVOKEDYNAMIC get(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/util/function/Supplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Ljava/lang/Object;, handle:H6 local/lito381/server/NativeDeInstanceState.lambda$content$c27f2dd7a058f134cd8e387e16d3326f5958f89ee9ab731a3da66cd159336584(Llocal/lito381/server/NativeDeInstanceState;J)Ljava/lang/Object;, type:()Ljava/lang/Object;]
```

### 正規化列印程式

使用同版 semantic-diff 類別與 ASM classpath，編譯下列診斷類別；兩側皆提供正式 JAR 作為父類 classpath，並 preserve deviations 精確方法名稱。呼叫參數為 `JAR verify_entries.txt 輸出目錄 base-0.6.71.jar 裁切的deviations.txt`。差異以 Python difflib.unified_diff 全文產生，未使用 firstDiff 取代完整差異。

```java
package local.lito381.semanticdiff;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.lang.reflect.*;
import org.objectweb.asm.tree.*;
public class Dump {
  static Object call(String name, Object... args) throws Exception {
    for (Method m:ClassComparer.class.getDeclaredMethods()) if(m.getName().equals(name) && m.getParameterCount()==args.length) {
      m.setAccessible(true); return m.invoke(null,args);
    }
    throw new IllegalArgumentException(name);
  }
  static void add(List<String> out,String key,Object value) {out.add(key+"="+String.valueOf(value));}
  public static void main(String[] args) throws Exception {
    JarIndex j=JarIndex.load(Path.of(args[0])); j.addClasspath(Path.of(args[3]));
    Map<String,Set<String>> exact=new TreeMap<>();
    for(String line:Files.readAllLines(Path.of(args[4]))) {
      String key=line.split(" #")[0].trim(); if(key.isEmpty()||key.startsWith("#"))continue;
      key=key.substring(key.lastIndexOf(' ')+1); int hash=key.indexOf('#');
      exact.computeIfAbsent(key.substring(0,hash),k->new TreeSet<>()).add(key.substring(hash+1));
    }
    j.preserveExactMethods(exact);
    Path out=Path.of(args[2]); Files.createDirectories(out);
    for(String entry:Files.readAllLines(Path.of(args[1]))) {
      String name=entry.replace(".class",""); ClassNode c=j.readTree(name); if(c==null)continue;
      List<String> lines=new ArrayList<>();
      for(String key:List.of("access","superName","signature","outerClass","outerMethod","outerMethodDesc","nestHostClass"))add(lines,"class "+key,ClassNode.class.getField(key).get(c));
      for(String key:List.of("interfaces","nestMembers","permittedSubclasses"))add(lines,"class "+key,call("joinSorted",ClassNode.class.getField(key).get(c)));
      add(lines,"class annotations",call("annotations",c));
      add(lines,"class innerClasses",call("innerClasses",c,false));
      add(lines,"class recordComponents",call("records",c,j));
      for(FieldNode f:new TreeMap<String,FieldNode>((Map<String,FieldNode>)call("fieldMap",c,j)).values()) {
        String k="field "+f.name+":"+f.desc+" ";
        add(lines,k+"access",call("memberAccess",f.access));add(lines,k+"signature",f.signature);
        add(lines,k+"value",call("value",f.value));add(lines,k+"annotations",call("fieldAnns",f));
      }
      for(var e:new TreeMap<String,MethodNode>((Map<String,MethodNode>)call("methodMap",c,j)).entrySet()) {
        MethodNode m=e.getValue();String k="method "+e.getKey()+" ";
        add(lines,k+"access",call("memberAccess",m.access));add(lines,k+"signature",m.signature);
        add(lines,k+"exceptions",call("joinSorted",m.exceptions));add(lines,k+"annotations",call("methodAnns",m,call("labelIds",m),j));
        add(lines,k+"default",call("defaultValue",m.annotationDefault,j));
        for(String t:(List<String>)call("tryCatch",m,j))lines.add(k+t);
        for(String i:ClassComparer.instructions(m,j))lines.add(k+i);
      }
      Files.write(out.resolve(name.substring(name.lastIndexOf('/')+1)+".txt"),lines,StandardCharsets.UTF_8);
    }
  }
}
```

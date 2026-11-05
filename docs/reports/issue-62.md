# Issue #62／T020-B01 — 本機修復與完整語意差異

## 結論

**尚未通過 B01 驗收，交 `needs:claude` 裁定。** 本機 Microsoft JDK 17.0.17 編譯成功，`@AsRecord` CLI 成功轉換 `Draft`；產出恰好 22 個 entries。套用 main 已合併的 #132／#135 正規化後：**13 SAME、0 SAME_EFFECTS、9 DIFF、0 ONLY_LEFT、0 ONLY_RIGHT**，`missingLeft`／`extraLeft` 皆空。

本報告取代前輪「10 筆全部只是編譯器形狀差」的結論：完整差異證實前輪還漏了候選呼叫及兩個方法。本輪已補回，剩餘差異完整附於本文，沒有把未通過結果寫成完成。`src/main`、`pom.xml`、正式設定、封存、發布產物、`docs/REPORT.md` 都未修改；未啟動伺服器／遊戲或連 DB。

## 做了什麼（已驗證）

1. 在既有 `task/62` worktree 執行 `git fetch origin main`、`git merge --no-edit origin/main`（Already up to date），再合併遠端前輪 `82e01adc69f2d0fe502bb735f6c5826fd4b400ee`，保留兩次歷史提交，未 checkout、未重建分支、未 force push。
2. 先以未改的前輪 17 檔重編並重跑新工具：仍是 **12 SAME／10 DIFF**，不是沿用舊報告。
3. CFR 0.152 對候選 B01 全部 17 個外層類重新反編譯到 `scratch/issue-62/cfr/`，並使用原有 semantic-diff 正規化函式輸出全類／全方法差異；不只依賴 `firstDiff`。
4. 修正下表實際缺漏，再對 22 entries 完整重驗。保持所有 17 檔指定 DECOMPILED 標頭。

| 類別 | 本輪修正 |
|---|---|
| `NativeSourceCubePlacement.Draft` | 加 `@AsRecord(components={engine,epoch,previousScene,scene,creator})`；移除手寫 equals／hashCode／toString，保留 component、額外 final 欄位與 `freeze(engine)` 初始化；CLI 產生 record metadata／ObjectMethods，該 entry 已 SAME。 |
| `NativeBlindSceneAction` | 補 `NativeSourceNoneTargetRequest.request`、`NativeSourceTargetLos.line`，保留 `NativeSourceIndirectPlayerProof.player`。 |
| `NativeControlledNegativeMaterial` | 補 `NativeSourceTargetNegativeLife.request/capture/close/current/proof/ready/firstTimed/lifetime`、`NativeSourceTargetNegativeReserve.reserve`、`NativeSourceControlledNegativeFinal.install/produce`。依候選順序在 `capture` 後、判斷其結果前呼叫 `install`；正常離開及 catch 都 close，monitor 順序保持 hub→pet→timed→admission。 |
| `NativeWeaponBreakBattleCompanion` | `addWeaponBreakStack` 改回候選的 `NativeSourceWeaponCounterMagic.stack`；firstPrepare 先取 accounts、再 snapshot，兩者都在 catch 保護內，避免前輪改變呼叫順序和保護範圍。 |
| `NativeWeaponBreakTargetLifecycle` | 補候選原有的 `nativeSourceWeaponFirstDispatch`、`nativeSourceMaterialPairTarget`；補 `runRequest`、`prepare`、`scene`、`objectPacket`、`NativeSourceControlledRoleFrame.current`。移除對 `NativeSourceWeaponFinalFrame.capture` 的 UncheckedIOException 包裝，改直接傳播 IOException。 |
| `NativeSourceHealingCast` | 在 `capture(c,cast)` 之前建立 ready／body，保留候選「引數求值→capture→owned」的呼叫順序；Java 需以區域變數保留兩個函式物件。 |

`NativeWeaponBreakTargetLifecycle.captureOutput` 的 `throws IOException` 是目前來源與候選的明確差異：候選沒有 Exceptions 屬性，但直接呼叫會拋 IOException 的方法。新增宣告保持原例外物件往外傳，不再改成 UncheckedIOException；其來源 API 相容性與 B-land 呼叫端影響仍待主控決定，不能當作驗收豁免。

## 怎麼驗證（已驗證）

工作目錄：`D:\lineage開發\3-lito開發\scratch\wt\task-62`。工具基線：`origin/main @ a87645ef0232d36ef0d594d1a50021ca0a86fa1b`（含 #102、#132、#135；沒有借用未合併的 #153）。

JDK 原始輸出：

```text
openjdk version "17.0.17" 2025-10-21 LTS

OpenJDK Runtime Environment Microsoft-12574423 (build 17.0.17+10-LTS)

OpenJDK 64-Bit Server VM Microsoft-12574423 (build 17.0.17+10-LTS, mixed mode, sharing)


exit=0
```
固定輸入／輸出 SHA-256：

| 檔案 | SHA-256 |
|---|---|
| `lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar` | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| `lito伺服器/LiTo381-Server/reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `lito伺服器/LiTo381-Server/target/issue-62/dependency/cfr-0.152.jar` | `f686e8f3ded377d7bc87d216a90e9e9512df4156e75b06c655a16648ae8765b2` |
| `lito伺服器/LiTo381-Server/target/issue-62/final/staged.jar` | `b2787953574abe3f134efcb8416cc66127643f567144f98fbc7b404344e2ac29` |
| `scratch/issue-62/final.json` | `82e23bbda925b01829b314cccde3e7b70d4dcf3719285c9ac15429849e3f589f` |
| `scratch/issue-62/final-full.diff` | `6a160992cc568875cb6e29f82cd700f9adf71ef32948aba8ccf40233bfcea51d` |

CFR 來源：`https://repo.maven.apache.org/maven2/org/benf/cfr/0.152/cfr-0.152.jar`。候選 entries 僅抽出到 `target/issue-62/candidate-classes` 作反編譯輸入；**驗收左側 JAR 只包含 javac 輸出及核准的 AsRecord 轉換結果**，沒有候選／正式 class overlay，也沒有其他 class 後處理。

三份主控清單 `effects-approved.txt`、`extra-methods-approved.txt`、`metadata-exempt.txt` 已按 B01 精確篩選，交集皆為 0，因此沒有傳入空清單、沒有套用其他批次豁免。本輪未修改這些清單。#62 的通用規則雖允許特定堆疊差異在 SAME_EFFECTS 後自列清單，但本批仍有外部靜態 handle、合成方法、Exceptions 屬性及 try 範圍差異，不符合逕行宣告全批通過的條件。

完整驗證入口為本文附錄 A 的 `scratch/issue-62/verify.py`。它依 batches.json 產生 17 個 Java 的精確命令引數與 22 行 entries，編譯 AsRecord 註解到獨立目錄，使用以下 classpath（Windows 分隔為 `;`）：

```text
reference/candidate-50635f7e.jar;reference/base-0.6.71.jar;target/issue-62/annotation;lib/*
```

17 檔 `javac --release 17 -encoding UTF-8` 原始輸出：

```text
Note: Some input files use unchecked or unsafe operations.

Note: Recompile with -Xlint:unchecked for details.


exit=0
```
AsRecord CLI 原始輸出：

```text
as-record: scanned=22 converted=1


exit=0
```
semantic-diff CLI 原始輸出（exit 0 只代表工具成功，**不代表 DIFF 已通過**）：

```text
SUMMARY SAME=13 SAME_EFFECTS=0 DIFF=9 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0 ALLOW_METHODS=0

wrote D:\lineage開發\3-lito開發\scratch\wt\task-62\scratch\issue-62\final.json

wrote D:\lineage開發\3-lito開發\scratch\wt\task-62\scratch\issue-62\final.md


exit=0
```
註解編譯、工具編譯、jar 打包、正規化輸出器編譯及左右輸出皆 exit 0，無其他輸出。ASM 9.7 從 `D:/dev-cache/maven/repository` 唯讀取得，使用 AsRecord 現有函式校驗 SHA-256；沒有執行 Maven 或寫入其共用快取。

### 22 entries 完整驗收結果

| Entry（`local/lito381/server/`） | 結果 |
|---|---|
| `NativeBlindSceneAction.class` | DIFF |
| `NativeCompanionForestArea.class` | DIFF |
| `NativeCompanionRefScene.class` | SAME |
| `NativeCompanionWindAction.class` | DIFF |
| `NativeControlledNegativeMaterial.class` | DIFF |
| `NativePairedCastSafety.class` | SAME |
| `NativeSourceBatchTerminal.class` | SAME |
| `NativeSourceCubePlacement.class` | SAME |
| `NativeSourceCubePlacement$Draft.class` | SAME |
| `NativeSourceDollAccepted.class` | DIFF |
| `NativeSourceDollAccepted$Pending.class` | SAME |
| `NativeSourceDollNpc.class` | DIFF |
| `NativeSourceDollNpc$Scope.class` | SAME |
| `NativeSourceDollSkills.class` | SAME |
| `NativeSourceDollSkills$View.class` | SAME |
| `NativeSourceDollSlow.class` | SAME |
| `NativeSourceHealingCast.class` | DIFF |
| `NativeSourceHealingCast$Cast.class` | SAME |
| `NativeSourcePendingAdmission.class` | SAME |
| `NativeSummonTerminalLifecycle.class` | SAME |
| `NativeWeaponBreakBattleCompanion.class` | DIFF |
| `NativeWeaponBreakTargetLifecycle.class` | DIFF |

另用本文重現入口於全新 `target/issue-62/recheck/classes` 重跑，22 個 class bytes 與 `final/classes` 逐一相同。

`jar` 實際 class 集合已在包裝前逐項比較，和 `verify_entries` 完全一致；semantic-diff 再次確認 `missingLeft=[]`、`extraLeft=[]`。annotation class 沒有包入左側 JAR。

## 剩餘差異與裁定請求

| 類別 | 已觀察到的完整差異範圍／下一步 |
|---|---|
| Blind／Forest／Wind | 候選把捕獲 Context 的 method-ref handle 改為外部靜態 `NativeSourceIndirectPlayerProof.player(Context,ClientSession)`，還留有 requireNonNull；Java lambda 多一層私有 helper。Blind 另有內容相同 lambda 被候選合併。保留生命週期 proof，不能退回 `c::player`。 |
| ControlledNegativeMaterial | `cast` 的 boolean 暫存、try 表分割／標籤／槽位，加上 `valid` 與 `cast` 的重複 lambda。其餘方法（包含完整 negative monitor 本體）正規化一致。catch 的 protected return 邊界仍須裁定，不能以原來的「JDK 版本差異」解釋漏移植。 |
| DollAccepted | `fromActor` 第三／四引數：候選用 DUP，來源再讀一次 `plan.caster`／`plan.target`。完整 diff 只剩這兩處；目前 effects 比較也會觀察欄位讀取，不能直接假定核准清單能讓它通過。 |
| DollNpc | 3 個私有 lambda 的前綴為 `nativeSourcePhysicalOriginalNatural`／`natural`，方法本體一致；#153 的內容識別預期相關，但尚未合併及實測。 |
| HealingCast | 候選用 operand stack 暫存 ready／body，Java 使用 ASTORE／ALOAD；現在呼叫順序相同。現行 effects 仍保留 ASTORE，不在本任務擅改工具。 |
| WeaponBreakBattleCompanion | `nativeSourceWeaponFirstPrepare` 的 accounts／snapshot 區域變數對堆疊 DUP／POP，以及 try 表被 javac 拆開（return 邊界）。呼叫與錯誤處理順序已修正；須主控核對保護範圍。 |
| WeaponBreakTargetLifecycle | `captureOutput` 的 Exceptions 屬性；間接 proof lambda；同內容 lambda 合併；ASM 插入 Context 引數後，候選仍保留舊的 lambda 捕獲參數順序，而 javac 依首次引用順序產生 GameHub／Context 不同排列。不是 #135 的非參數槽位重編號能解決。 |

所有 9 個 DIFF 的全部 unified diff 放在附錄 C，包含類別屬性、欄位、方法宣告、try-catch、全運算元與 bootstrap handle；未裁切長行，未只貼 firstDiff。相同區塊用標準 unified diff 上下文省略，所有差異區塊完整保留。完整左右正規化文件在本機 `scratch/issue-62/final/{left,right}`；重現程式在附錄 B。展示只去除每行結尾空白，不額外正規化比對內容。

## 推測

- #153 正在處理 lambda 依內容識別、try 邊界與 effects 暫存等類別，可能消除部分差異；查詢時仍 OPEN／task:running，未使用其未合併程式碼，不能預先承諾 0 DIFF。
- 外部靜態 handle、lambda 捕獲參數順序、checked exception 宣告不一定受 #153 涵蓋，需主控按附錄逐項裁定。

## 未驗證／待驗證

- B01 全部 SAME／SAME_EFFECTS：**未達成**。不得合併為已驗收，也不得進入 B-land。
- 尚未做來源 API 整體編譯、單元測試及回歸；本任務規定只編 staged，統一交 B-land。
- 未做任何實機登入、技能、戰鬥或資料庫驗證。
- 新 head 的 GitHub CI 尚待調度器處理；不能引用前輪 head 的狀態。

## 產出路徑／給下一棒的提示

- 17 個 Java：`lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/`，精確清單取 B01 `classes`。
- 本報告：`docs/reports/issue-62.md`，為唯一提交的報告文件。
- 本機非提交證據：`scratch/issue-62/`（CFR、原始日誌、基線／最終 JSON、完整正規化與腳本）；生成 class／JAR：`lito伺服器/LiTo381-Server/target/issue-62/`，工具編譯在各自 `tools/*/target/`。
- Claude 主控先裁定上列 9 類，不要再把前輪實際漏移植稱為編譯器差異。#153 合併後，本機執行器再用本文入口重跑；任何新例外須有精確方法與證據。
- 保留已修復的 request／LOS／生命週期／Counter Magic／final-frame 行為；不要改回直接 player method-ref、刪 Draft freeze、包裝原 IOException，或 overlay 候選 class。
- 既有 PR #79 沿用 `task/62`，由 Claude 審查、使用者決定合併；不改 `docs/REPORT.md`。

## 附錄 A：可重現編譯、轉換與驗收

下列 Python／Java 程式內容都保存在本報告，可在另一個相同版本 worktree 重建指定 `scratch/issue-62/` 檔案。工作目錄設為本報告上列 worktree；先將下面 verify.py 另存，再執行（`recheck` 必須是尚不存在的 target 子目錄，避免混入舊 class）：

```powershell
$py = 'C:\Users\CHAOREN\AppData\Local\Programs\Python\Python312\python.exe'
& $py -B -X utf8 scratch/issue-62/verify.py recheck convert
```

`scratch/issue-62/verify.py`：

```python
import os, sys, json, subprocess, pathlib, hashlib, zipfile, urllib.request
ROOT=pathlib.Path(__file__).resolve().parents[2]
SERVER=ROOT/'lito伺服器/LiTo381-Server'
WORK=ROOT/'scratch/issue-62'
TARGET=SERVER/'target/issue-62'
JDK=pathlib.Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot')
os.environ['JAVA_HOME']=str(JDK)
os.environ['AS_RECORD_MAVEN_REPO']='D:/dev-cache/maven/repository'
os.environ['PYTHONDONTWRITEBYTECODE']='1'
sys.path.insert(0,str(SERVER/'tools/as-record'))
import as_record
BATCH=next(b for b in json.loads((ROOT/'docs/combat-merge/batches.json').read_text('utf-8'))['batches'] if b['id']=='B01')
STAGED=SERVER/'merge-inputs/50635f7e/staged/local/lito381/server'
TARGET.mkdir(parents=True,exist_ok=True)
def run(args,log):
    p=subprocess.run([str(x) for x in args],cwd=SERVER,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    out=p.stdout.decode('utf-8',errors='replace')
    (WORK/log).write_text(out+'\nexit='+str(p.returncode)+'\n',encoding='utf-8')
    print(log+': '+out+'exit='+str(p.returncode),flush=True)
    if p.returncode: raise SystemExit(p.returncode)
    return out
def java(*args): return [JDK/'bin/java.exe','-Dfile.encoding=UTF-8',*args]
def javac(*args): return [JDK/'bin/javac.exe','--release','17','-encoding','UTF-8',*args]
def verify(name,convert=False):
    for file,expected in [('candidate-50635f7e.jar','50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729'),('base-0.6.71.jar','e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498')]:
        assert hashlib.sha256((SERVER/'reference'/file).read_bytes()).hexdigest()==expected,file
    dest=TARGET/name/'classes'
    dest.mkdir(parents=True,exist_ok=False)
    annotation=TARGET/'annotation'
    annotation.mkdir(exist_ok=True)
    run(javac('-d',annotation,SERVER/'src/main/java/local/lito381/server/AsRecord.java'),name+'-annotation.log')
    cp=';'.join(str(x) for x in [SERVER/'reference/candidate-50635f7e.jar',SERVER/'reference/base-0.6.71.jar',annotation,SERVER/'lib/*'])
    run(javac('-cp',cp,'-d',dest,*[STAGED/(c+'.java') for c in BATCH['classes']]),name+'-javac.log')
    if convert: run([sys.executable,'-B',SERVER/'tools/as-record/as_record.py',dest],name+'-as-record.log')
    entries=sorted(p.relative_to(dest).as_posix() for p in dest.rglob('*.class'))
    assert entries==sorted(BATCH['verify_entries']),entries
    (WORK/'entries.txt').write_text('\n'.join(BATCH['verify_entries'])+'\n',encoding='utf-8')
    jar=TARGET/name/'staged.jar'
    run([JDK/'bin/jar.exe','--create','--file',jar,'--no-manifest','-C',dest,'.'],name+'-jar.log')
    cp=as_record.dependencies()
    sd=SERVER/'tools/semantic-diff/target/classes'
    sd.mkdir(parents=True,exist_ok=True)
    run(javac('-cp',cp,'-d',sd,*sorted((SERVER/'tools/semantic-diff/src/main/java').rglob('*.java'))),name+'-tool.log')
    args=java('-cp',str(sd)+';'+cp,'local.lito381.semanticdiff.SemanticDiff','--left',jar,'--right',SERVER/'reference/candidate-50635f7e.jar','--classes',WORK/'entries.txt','--json',WORK/(name+'.json'),'--md',WORK/(name+'.md'))
    for flag,file in [('effects-methods','effects-approved'),('extra-methods','extra-methods-approved'),('metadata-exempt','metadata-exempt')]:
        lines=[line for line in (ROOT/('docs/combat-merge/'+file+'.txt')).read_text('utf-8-sig').splitlines() if line.strip() and not line.lstrip().startswith('#') and line.split('#')[0].strip()+'.class' in BATCH['verify_entries']]
        if lines:
            selected=WORK/(file+'.txt'); selected.write_text('\n'.join(lines)+'\n',encoding='utf-8');args.extend(['--'+flag,selected])
    run(args,name+'-semantic.log')
    result=json.loads((WORK/(name+'.json')).read_text('utf-8'))
    print(json.dumps({'counts':result['counts'],'selection':result['classSelection']},ensure_ascii=False),flush=True)
if __name__=='__main__':
    run([JDK/'bin/java.exe','-version'],'jdk.log')
    verify(sys.argv[1],len(sys.argv)>2)
```

CFR 原始操作 `scratch/issue-62/decompile.py`，執行 `& $py -B -X utf8 scratch/issue-62/decompile.py`；17 次退出碼均為 0：

```python
from verify import *
cfr=TARGET/'dependency/cfr-0.152.jar'; cfr.parent.mkdir(exist_ok=True)
if not cfr.exists(): cfr.write_bytes(urllib.request.urlopen('https://repo.maven.apache.org/maven2/org/benf/cfr/0.152/cfr-0.152.jar',timeout=45).read())
print('CFR SHA256',hashlib.sha256(cfr.read_bytes()).hexdigest(),flush=True)
source=SERVER/'reference/candidate-50635f7e.jar'
with zipfile.ZipFile(source) as z:
    for entry in BATCH['verify_entries']:
        out=TARGET/'candidate-classes'/entry; out.parent.mkdir(parents=True,exist_ok=True);out.write_bytes(z.read(entry))
for name in BATCH['classes']:
    run(java('-jar',cfr,TARGET/('candidate-classes/local/lito381/server/'+name+'.class'),'--extraclasspath',source,'--outputdir',WORK/'cfr','--silent','true'),name+'-cfr.log')
```

CFR 輸出與提交來源的 SHA-256 對照（來源是前輪成果依本輪 CFR／位元碼修復，不宣稱 CFR 原文可直接編譯）：

| 類別 | CFR Java SHA-256 | staged Java SHA-256 |
|---|---|---|
| `NativeBlindSceneAction` | `707ee294a14fe4129e579f2266f8c03b4bdf81cdfb0829443688a2987ddcaf26` | `7d525f7b9935127da5a9e0f6a3d1708fe87e3766b812b453c0c86488270fd51d` |
| `NativeCompanionForestArea` | `7c6c1d0c626977e1d75abc49a4313ce2e0c762780c8dfd4dd2c1642f16ffed25` | `b04bfb4d40048cca06476a6e8b079c22ffe56032fd625aef504fccb354e85fa8` |
| `NativeCompanionRefScene` | `1d4ea4012e3263b06b044b7f750b974db24b6001281695ea330290f0891d3ccb` | `056297dc885e37b00234e2be7cf6f3ef81debc65735546d7e4d505f7b7a5d859` |
| `NativeCompanionWindAction` | `5247be1dd5213c98d1098cbe692910e1a5de544415cd4e8ba52241c2f8ce6b20` | `7951f65ddb2e5987fc4e4a248359c31ee539e71be0e8b83b64bcf0a62b947857` |
| `NativeControlledNegativeMaterial` | `bc22eb4fcd8d103139eaee7aae0eb14ac69268d6bf363853b45dd20d54ed5f46` | `618afb1e519e9d18a971b4777e521963963dcf99564c08cf8aeef631f752912a` |
| `NativePairedCastSafety` | `6edb4e3c322c40f437e9c2b3156cca2b5bc9a43bd4954b27c96b8f07f1842954` | `2686e9735e0c636ded604d3b2728c63d89d8a22a11a150db4ff31f2d9f9a3b5e` |
| `NativeSourceBatchTerminal` | `0e0a55f17928436644c08dd83b16f275ac488fcfddaae7f78e180fb9b4f1cc19` | `c33e1bba38ed7dad762cdc9b451dd449af07ed3fa0c03616e5beedc207863d68` |
| `NativeSourceCubePlacement` | `07e945a692918ae9056e0099cf4550b044e0f6b6c23ca64e000df7254e8b5f73` | `0d085e9be7ed95ac29b24c5780f84f95b575e411b66b71ed60a810153efcd77c` |
| `NativeSourceDollAccepted` | `921fe6c17f2ffb6a53ead01677823d385368c5b2a38fa4d1c53efa0aca3c6318` | `560af88642b22b7c7f42a62cf6ef6df591d03c9f36790ac2555a755d5e171084` |
| `NativeSourceDollNpc` | `d8a8a649853161a5f2c783de103bf2e32d638d49d4f9c41001f73e474a596594` | `08e021f4d985a354a9a70c9015182c1d659f5c94e1224c284e8f325d8c4210cb` |
| `NativeSourceDollSkills` | `e54a5a3af00e623692e0fca655876af31a1d4c091fa251c29c79548675737c78` | `e0e93b399a04c820252153318bddc573d4ad36ef16d156fc12a5c6d0b08962f1` |
| `NativeSourceDollSlow` | `488f9252a439d70b66b63cc278c6f75182c03044b13d435c35f7d1ba36b1c4c1` | `93b6bf41701446ae806ef9ac7de0e063f3b282cf6e6d9b6ab7a6162b19bc71d0` |
| `NativeSourceHealingCast` | `a0f10697821087c414a49d5bdda42c1e83d68735bd1ab35c33af04c253c164d2` | `9573139baf7a8b97494fba93ae0c6e75fe1e7c79508e294b9f135434ac9cd080` |
| `NativeSourcePendingAdmission` | `3b4226034251c915048cd4d4e529acf78081826e05c582c7afb640594d2ce6cc` | `24f968912197efe3c29a2244f5009c67650cc558105e9281627eddddd4519e79` |
| `NativeSummonTerminalLifecycle` | `21bcd9141657dfd066bf7d9edb6197d0118f3f3134738369372ffff9074d1b30` | `afcbbb2592710521c925df2a2160c5df7af9141cbd4466bf3c268b355eb665f6` |
| `NativeWeaponBreakBattleCompanion` | `194e6c5f3a8dde82df36459a42b1a9c0e91e4a84e63457caaf53424b1f9293f8` | `0ec0772cc063bd262f1c88e3726edd216aac41efeba026474ee31e6e87351beb` |
| `NativeWeaponBreakTargetLifecycle` | `37cca66c1d6df6a5ca8710c1c9785f718d78049d76cdb4faadd75402f2b33640` | `1a79541dc2faef2fb9b8926b2cb99fde3924c39a5dac65f27dfce964d00a75e5` |

## 附錄 B：完整正規化差異重現

輸出器只讀取 JAR，直接呼叫本次 main 的 `ClassComparer`／`JarIndex`，不改它們或 class bytes。完整 class/field/method 屬性均輸出；方法按工具的 canonical name＋descriptor 排序。

`scratch/issue-62/DumpNormalized.java`：

```java
package local.lito381.semanticdiff;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import org.objectweb.asm.tree.*;
public class DumpNormalized {
 static Object call(String name,Class<?>[] types,Object... args)throws Exception {var m=ClassComparer.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(null,args);}
 static String dump(ClassNode n,JarIndex j)throws Exception {
  var out=new StringBuilder();
  out.append("CLASS ").append(n.name).append(" access=").append(n.access & ~32).append(" super=").append(n.superName).append(" signature=").append(n.signature).append(" interfaces=").append(n.interfaces).append('\n');
  out.append("outer=").append(n.outerClass).append('#').append(n.outerMethod).append(n.outerMethodDesc).append(" nestHost=").append(n.nestHostClass).append(" nestMembers=").append(n.nestMembers==null?null:new TreeSet<>(n.nestMembers)).append(" permitted=").append(n.permittedSubclasses==null?null:new TreeSet<>(n.permittedSubclasses)).append('\n');
  out.append("annotations=").append(call("annotations",new Class<?>[]{ClassNode.class},n)).append('\n');
  out.append("innerClasses=").append(call("innerClasses",new Class<?>[]{ClassNode.class,boolean.class},n,false)).append('\n');
  out.append("records=").append(call("records",new Class<?>[]{ClassNode.class,JarIndex.class},n,j)).append('\n');
  var fields=new TreeMap<String,FieldNode>();for(var f:n.fields)fields.put(f.name+f.desc,f);
  for(var f:fields.values())out.append("FIELD ").append(f.name).append(f.desc).append(" access=").append(f.access & ~4096).append(" signature=").append(f.signature).append(" value=").append(call("value",new Class<?>[]{Object.class},f.value)).append(" annotations=").append(call("fieldAnns",new Class<?>[]{FieldNode.class},f)).append('\n');
  var methods=new TreeMap<String,MethodNode>();for(var m:n.methods)methods.put(j.remapMethod(n.name,m.name,m.desc)+m.desc,m);
  for(var e:methods.entrySet()) {var m=e.getValue();out.append("\nMETHOD ").append(e.getKey()).append(" access=").append(m.access & ~4096).append(" signature=").append(m.signature).append(" exceptions=").append(new TreeSet<>(m.exceptions)).append('\n');
   var labels=call("labelIds",new Class<?>[]{MethodNode.class},m);
   out.append("annotations=").append(call("methodAnns",new Class<?>[]{MethodNode.class,Map.class,JarIndex.class},m,labels,j)).append(" default=").append(call("defaultValue",new Class<?>[]{Object.class,JarIndex.class},m.annotationDefault,j)).append('\n');
   for(var line:(List<?>)call("tryCatch",new Class<?>[]{MethodNode.class,JarIndex.class},m,j))out.append(line).append('\n');
   for(var line:ClassComparer.instructions(m,j))out.append(line).append('\n');
  }
  return out.toString();
 }
 public static void main(String[] args)throws Exception {var j=JarIndex.load(Path.of(args[0]));var dest=Path.of(args[2]);Files.createDirectories(dest);for(var line:Files.readAllLines(Path.of(args[1]))){var name=line.trim().replace(".class","");if(!name.isBlank())Files.writeString(dest.resolve(name.substring(name.lastIndexOf('/')+1)+".txt"),dump(j.readTree(name),j));}}
}
```

`scratch/issue-62/dump.py`：

```python
from verify import *
import difflib
name=sys.argv[1]
cp=str(SERVER/'tools/semantic-diff/target/classes')+';'+as_record.dependencies()
out=TARGET/'dump-tool';out.mkdir(exist_ok=True)
run(javac('-cp',cp,'-d',out,WORK/'DumpNormalized.java'),name+'-dump-build.log')
for side,jar in [('left',TARGET/name/'staged.jar'),('right',SERVER/'reference/candidate-50635f7e.jar')]:
    run(java('-cp',str(out)+';'+cp,'local.lito381.semanticdiff.DumpNormalized',jar,WORK/'entries.txt',WORK/name/side),name+'-dump-'+side+'.log')
all_diff=[]
for e in BATCH['verify_entries']:
    f=e.split('/')[-1].replace('.class','.txt')
    a=(WORK/name/'left'/f).read_text('utf-8').splitlines(True);b=(WORK/name/'right'/f).read_text('utf-8').splitlines(True)
    diff=''.join(difflib.unified_diff(a,b,fromfile='staged/'+f,tofile='candidate/'+f,n=3))
    if diff:
        (WORK/name/(f+'.diff')).write_text(diff,encoding='utf-8');all_diff.append(diff);print(f,len(diff.splitlines()))
(WORK/(name+'-full.diff')).write_text('\n'.join(all_diff),encoding='utf-8')
```

執行：

```powershell
& $py -B -X utf8 scratch/issue-62/dump.py recheck
```

## 附錄 C：最終全部正規化差異

方向：`-` 為 staged，`+` 為 candidate。

### NativeBlindSceneAction

```diff
--- staged/NativeBlindSceneAction.txt
+++ candidate/NativeBlindSceneAction.txt
@@ -403,7 +403,10 @@
 GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.players:Ljava/util/List;
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
 ALOAD 0
-INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeBlindSceneAction.lambda$companion$#0(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
+DUP
+INVOKESTATIC java/util/Objects.requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
+POP
+INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeSourceIndirectPlayerProof.player(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
 INVOKEINTERFACE java/util/stream/Stream.filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream; itf
 INVOKEINTERFACE java/util/stream/Stream.toList()Ljava/util/List; itf
 ASTORE 19
@@ -757,13 +760,6 @@
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 INVOKEVIRTUAL java/lang/Integer.intValue()I
-IRETURN
-
-METHOD lambda$companion$#0(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z access=10 signature=null exceptions=[]
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-ALOAD 0
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativeSourceIndirectPlayerProof.player(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z
 IRETURN

 METHOD lambda$run$#0(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativeAreaMaterialAction$Npc;Llocal/lito381/server/BattleSpellCatalog$BlindSpell;Z[B)V access=10 signature=null exceptions=[java/io/IOException]
@@ -1013,28 +1009,6 @@
 LABEL L1
 IRETURN

-METHOD lambda$valid$#0(Llocal/lito381/server/NativeAreaMaterialAction$Context;JLlocal/lito381/server/ClientSession;)Z access=10 signature=null exceptions=[]
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-ALOAD 0
-GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.bindings:Ljava/util/IdentityHashMap;
-ALOAD 3
-INVOKEVIRTUAL java/util/IdentityHashMap.get(Ljava/lang/Object;)Ljava/lang/Object;
-CHECKCAST local/lito381/server/NativePlayerCommit$Binding
-INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.object()J
-LLOAD 1/2
-LCMP
-IFNE L0
-ALOAD 0
-ALOAD 3
-INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Context.player(Llocal/lito381/server/ClientSession;)Z
-IFEQ L0
-CONST Integer:1
-GOTO L1
-LABEL L0
-CONST Integer:0
-LABEL L1
-IRETURN
-
 METHOD position(Llocal/lito381/server/NativeAreaMaterialAction$Context;)[I access=8 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
 ALOAD 0
@@ -1557,7 +1531,7 @@
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
 ALOAD 0
 LLOAD 1/2
-INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;J)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeBlindSceneAction.lambda$valid$#0(Llocal/lito381/server/NativeAreaMaterialAction$Context;JLlocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
+INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;J)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeBlindSceneAction.lambda$run$#3(Llocal/lito381/server/NativeAreaMaterialAction$Context;JLlocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
 INVOKEINTERFACE java/util/stream/Stream.anyMatch(Ljava/util/function/Predicate;)Z itf
 IRETURN

```

### NativeCompanionForestArea

```diff
--- staged/NativeCompanionForestArea.txt
+++ candidate/NativeCompanionForestArea.txt
@@ -188,7 +188,10 @@
 GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.players:Ljava/util/List;
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
 ALOAD 8
-INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeCompanionForestArea.lambda$apply$#0(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
+DUP
+INVOKESTATIC java/util/Objects.requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
+POP
+INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeSourceIndirectPlayerProof.player(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
 INVOKEINTERFACE java/util/stream/Stream.filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream; itf
 INVOKEINTERFACE java/util/stream/Stream.toList()Ljava/util/List; itf
 ASTORE 21
@@ -463,13 +466,6 @@
 LABEL L2
 CONST Integer:0
 LABEL L3
-IRETURN
-
-METHOD lambda$apply$#0(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z access=10 signature=null exceptions=[]
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-ALOAD 0
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativeSourceIndirectPlayerProof.player(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z
 IRETURN

 METHOD lambda$empty$#0(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/BattleSpellCatalog$AreaSilenceSpell;Llocal/lito381/server/NativeAreaMaterialAction$Npc;)Z access=10 signature=null exceptions=[]
```

### NativeCompanionWindAction

```diff
--- staged/NativeCompanionWindAction.txt
+++ candidate/NativeCompanionWindAction.txt
@@ -521,13 +521,6 @@
 ALOAD 28
 ATHROW

-METHOD lambda$publish$#0(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z access=10 signature=null exceptions=[]
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-ALOAD 0
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativeSourceIndirectPlayerProof.player(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z
-IRETURN
-
 METHOD lambda$run$#0(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativeCompanionTargets$Scope;Llocal/lito381/server/NativeSelfCastAction$Task;)V access=10 signature=null exceptions=[java/io/IOException]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
 TRY L2 L3 L4 java/lang/Throwable
@@ -622,7 +615,10 @@
 GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.players:Ljava/util/List;
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
 ALOAD 1
-INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeCompanionWindAction.lambda$publish$#0(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
+DUP
+INVOKESTATIC java/util/Objects.requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
+POP
+INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeSourceIndirectPlayerProof.player(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
 INVOKEINTERFACE java/util/stream/Stream.filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream; itf
 INVOKEINTERFACE java/util/stream/Stream.toList()Ljava/util/List; itf
 AASTORE
```

### NativeControlledNegativeMaterial

```diff
--- staged/NativeControlledNegativeMaterial.txt
+++ candidate/NativeControlledNegativeMaterial.txt
@@ -103,13 +103,9 @@

 METHOD cast(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/NativeSelfCastAction$Task;)V access=8 signature=null exceptions=[java/io/IOException]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-TRY L15 L16 L17 java/lang/Throwable
-TRY L17 L18 L17 java/lang/Throwable
-TRY L1 L2 L22 java/lang/Throwable
-TRY L3 L4 L22 java/lang/Throwable
-TRY L5 L6 L22 java/lang/Throwable
-TRY L7 L13 L22 java/lang/Throwable
-TRY L14 L21 L22 java/lang/Throwable
+TRY L11 L12 L13 java/lang/Throwable
+TRY L13 L14 L13 java/lang/Throwable
+TRY L1 L17 L17 java/lang/Throwable
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.spellSelector([B)Llocal/lito381/server/Protocol$SpellSelector;
 INVOKEVIRTUAL local/lito381/server/Protocol$SpellSelector.skillId()I
@@ -136,17 +132,16 @@
 GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.phase:Ljava/lang/String;
 CONST str:"WORLD"
 INVOKEVIRTUAL java/lang/String.equals(Ljava/lang/Object;)Z
-IFNE L3
+IFNE L2
 ALOAD 4
 GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.phase:Ljava/lang/String;
 CONST str:"BATTLE"
 INVOKEVIRTUAL java/lang/String.equals(Ljava/lang/Object;)Z
-IFNE L3
+IFNE L2
 ALOAD 4
 INVOKESTATIC local/lito381/server/NativeSourceTargetNegativeLife.close(Llocal/lito381/server/NativeAreaMaterialAction$Context;)V
+RETURN
 LABEL L2
-RETURN
-LABEL L3
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.attackSpellRequest([B)Llocal/lito381/server/Protocol$AttackSpellRequest;
 ALOAD 4
@@ -156,16 +151,13 @@
 ALOAD 4
 LLOAD 5/6
 INVOKESTATIC local/lito381/server/NativeSourceTargetNegativeLife.capture(Llocal/lito381/server/NativeAreaMaterialAction$Context;J)Z
-ISTORE 7
 ALOAD 4
 INVOKESTATIC local/lito381/server/NativeSourceControlledNegativeFinal.install(Llocal/lito381/server/NativeAreaMaterialAction$Context;)V
-ILOAD 7
-IFNE L5
+IFNE L3
 ALOAD 4
 INVOKESTATIC local/lito381/server/NativeSourceTargetNegativeLife.close(Llocal/lito381/server/NativeAreaMaterialAction$Context;)V
-LABEL L4
-RETURN
-LABEL L5
+RETURN
+LABEL L3
 ALOAD 4
 GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.players:Ljava/util/List;
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
@@ -177,61 +169,59 @@
 CONST null
 INVOKEVIRTUAL java/util/Optional.orElse(Ljava/lang/Object;)Ljava/lang/Object;
 CHECKCAST local/lito381/server/ClientSession
-ASTORE 8
+ASTORE 7
 ALOAD 4
 GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.npcs:Ljava/util/Map;
 LLOAD 5/6
 INVOKESTATIC java/lang/Long.valueOf(J)Ljava/lang/Long;
 INVOKEINTERFACE java/util/Map.get(Ljava/lang/Object;)Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/NativeAreaMaterialAction$Npc
+ASTORE 8
+ALOAD 7
+IFNONNULL L4
+ALOAD 8
+IFNONNULL L4
+ALOAD 4
+INVOKESTATIC local/lito381/server/NativeSourceTargetNegativeLife.close(Llocal/lito381/server/NativeAreaMaterialAction$Context;)V
+RETURN
+LABEL L4
+ALOAD 7
+IFNULL L5
+ALOAD 7
+GOTO L7
+LABEL L5
+ALOAD 8
+INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Npc.kind()I
+CONST Integer:3
+IF_ICMPNE L6
+ALOAD 8
+INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Npc.owner()Ljava/lang/Object;
+CHECKCAST local/lito381/server/ClientSession
+GOTO L7
+LABEL L6
+ALOAD 0
+LABEL L7
 ASTORE 9
-ALOAD 8
-IFNONNULL L7
 ALOAD 9
-IFNONNULL L7
-ALOAD 4
-INVOKESTATIC local/lito381/server/NativeSourceTargetNegativeLife.close(Llocal/lito381/server/NativeAreaMaterialAction$Context;)V
-LABEL L6
-RETURN
-LABEL L7
-ALOAD 8
-IFNULL L8
-ALOAD 8
-GOTO L10
+ALOAD 0
+IF_ACMPNE L8
+CONST null
+GOTO L9
 LABEL L8
 ALOAD 9
-INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Npc.kind()I
-CONST Integer:3
-IF_ICMPNE L9
+ALOAD 4
+INVOKESTATIC local/lito381/server/NativeSourceTargetNegativeReserve.reserve(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeAreaMaterialAction$Context;)Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
+LABEL L9
+ASTORE 10
 ALOAD 9
-INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Npc.owner()Ljava/lang/Object;
-CHECKCAST local/lito381/server/ClientSession
-GOTO L10
-LABEL L9
-ALOAD 0
+ALOAD 0
+IF_ACMPEQ L10
+ALOAD 10
+IFNONNULL L10
+ALOAD 4
+INVOKESTATIC local/lito381/server/NativeSourceTargetNegativeLife.close(Llocal/lito381/server/NativeAreaMaterialAction$Context;)V
+RETURN
 LABEL L10
-ASTORE 10
-ALOAD 10
-ALOAD 0
-IF_ACMPNE L11
-CONST null
-GOTO L12
-LABEL L11
-ALOAD 10
-ALOAD 4
-INVOKESTATIC local/lito381/server/NativeSourceTargetNegativeReserve.reserve(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeAreaMaterialAction$Context;)Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
-LABEL L12
-ASTORE 11
-ALOAD 10
-ALOAD 0
-IF_ACMPEQ L14
-ALOAD 11
-IFNONNULL L14
-ALOAD 4
-INVOKESTATIC local/lito381/server/NativeSourceTargetNegativeLife.close(Llocal/lito381/server/NativeAreaMaterialAction$Context;)V
-LABEL L13
-RETURN
-LABEL L14
 ALOAD 4
 CONST str:"nativeWeaponBreakTarget"
 CONST Integer:1
@@ -244,50 +234,49 @@
 INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
 ALOAD 4
 CONST str:"nativeWeaponBreakPeerReceipt"
-ALOAD 11
+ALOAD 10
 INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
-LABEL L15
-ALOAD 4
-ALOAD 0
-ALOAD 10
+LABEL L11
+ALOAD 4
+ALOAD 0
+ALOAD 9
 ALOAD 4
 ALOAD 2
 INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativeSelfCastAction$Task;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeControlledNegativeMaterial.lambda$cast$#0(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativeSelfCastAction$Task;)V, type:()V]
 INVOKESTATIC local/lito381/server/NativeAreaMaterialAction.owned(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativeSelfCastAction$Task;)V
-LABEL L16
-ALOAD 11
-IFNULL L20
-ALOAD 11
+LABEL L12
+ALOAD 10
+IFNULL L16
+ALOAD 10
 GETFIELD local/lito381/server/NativePlayerVitalCompletions$Receipt.released:Ljava/util/concurrent/atomic/AtomicBoolean;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicBoolean.get()Z
-IFNE L20
+IFNE L16
+ALOAD 10
+INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
+GOTO L16
+LABEL L13
+ASTORE 11
+LABEL L14
+ALOAD 10
+IFNULL L15
+ALOAD 10
+GETFIELD local/lito381/server/NativePlayerVitalCompletions$Receipt.released:Ljava/util/concurrent/atomic/AtomicBoolean;
+INVOKEVIRTUAL java/util/concurrent/atomic/AtomicBoolean.get()Z
+IFNE L15
+ALOAD 10
+INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
+LABEL L15
 ALOAD 11
-INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
-GOTO L20
+ATHROW
+LABEL L16
+ALOAD 4
+INVOKESTATIC local/lito381/server/NativeSourceTargetNegativeLife.close(Llocal/lito381/server/NativeAreaMaterialAction$Context;)V
+RETURN
 LABEL L17
 ASTORE 12
-LABEL L18
-ALOAD 11
-IFNULL L19
-ALOAD 11
-GETFIELD local/lito381/server/NativePlayerVitalCompletions$Receipt.released:Ljava/util/concurrent/atomic/AtomicBoolean;
-INVOKEVIRTUAL java/util/concurrent/atomic/AtomicBoolean.get()Z
-IFNE L19
-ALOAD 11
-INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
-LABEL L19
+ALOAD 4
+INVOKESTATIC local/lito381/server/NativeSourceTargetNegativeLife.close(Llocal/lito381/server/NativeAreaMaterialAction$Context;)V
 ALOAD 12
-ATHROW
-LABEL L20
-ALOAD 4
-INVOKESTATIC local/lito381/server/NativeSourceTargetNegativeLife.close(Llocal/lito381/server/NativeAreaMaterialAction$Context;)V
-LABEL L21
-RETURN
-LABEL L22
-ASTORE 5
-ALOAD 4
-INVOKESTATIC local/lito381/server/NativeSourceTargetNegativeLife.close(Llocal/lito381/server/NativeAreaMaterialAction$Context;)V
-ALOAD 5
 ATHROW

 METHOD lambda$cast$#0(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativeSelfCastAction$Task;)V access=10 signature=null exceptions=[java/io/IOException]
@@ -519,28 +508,6 @@
 BASTORE
 RETURN

-METHOD lambda$valid$#0(Llocal/lito381/server/NativeAreaMaterialAction$Context;JLlocal/lito381/server/ClientSession;)Z access=10 signature=null exceptions=[]
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-ALOAD 0
-GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.bindings:Ljava/util/IdentityHashMap;
-ALOAD 3
-INVOKEVIRTUAL java/util/IdentityHashMap.get(Ljava/lang/Object;)Ljava/lang/Object;
-CHECKCAST local/lito381/server/NativePlayerCommit$Binding
-INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.object()J
-LLOAD 1/2
-LCMP
-IFNE L0
-ALOAD 0
-ALOAD 3
-INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Context.player(Llocal/lito381/server/ClientSession;)Z
-IFEQ L0
-CONST Integer:1
-GOTO L1
-LABEL L0
-CONST Integer:0
-LABEL L1
-IRETURN
-
 METHOD negative(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell;I)Z access=8 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
 TRY L4 L5 L6 java/io/IOException
@@ -799,7 +766,7 @@
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
 ALOAD 0
 LLOAD 1/2
-INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;J)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeControlledNegativeMaterial.lambda$valid$#0(Llocal/lito381/server/NativeAreaMaterialAction$Context;JLlocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
+INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;J)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeControlledNegativeMaterial.lambda$cast$#2(Llocal/lito381/server/NativeAreaMaterialAction$Context;JLlocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
 INVOKEINTERFACE java/util/stream/Stream.anyMatch(Ljava/util/function/Predicate;)Z itf
 LABEL L4
 IRETURN
```

### NativeSourceDollAccepted

```diff
--- staged/NativeSourceDollAccepted.txt
+++ candidate/NativeSourceDollAccepted.txt
@@ -352,8 +352,7 @@
 INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.binding()Llocal/lito381/server/NativePlayerCommit$Binding;
 ALOAD 8
 GETFIELD local/lito381/server/NativeSourceDamageDisplay$Plan.caster:Llocal/lito381/server/NativeSourceDamageDisplay$Actor;
-ALOAD 8
-GETFIELD local/lito381/server/NativeSourceDamageDisplay$Plan.caster:Llocal/lito381/server/NativeSourceDamageDisplay$Actor;
+DUP
 INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.life()Ljava/lang/Object;
 INVOKESTATIC local/lito381/server/NativeSourceAcceptedPlayers.fromActor(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeSourceDamageDisplay$Actor;Ljava/lang/Object;)Llocal/lito381/server/NativeSourceRawDamage$Member;
 ASTORE 9
@@ -363,8 +362,7 @@
 INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.binding()Llocal/lito381/server/NativePlayerCommit$Binding;
 ALOAD 8
 GETFIELD local/lito381/server/NativeSourceDamageDisplay$Plan.target:Llocal/lito381/server/NativeSourceDamageDisplay$Actor;
-ALOAD 8
-GETFIELD local/lito381/server/NativeSourceDamageDisplay$Plan.target:Llocal/lito381/server/NativeSourceDamageDisplay$Actor;
+DUP
 INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.life()Ljava/lang/Object;
 INVOKESTATIC local/lito381/server/NativeSourceAcceptedPlayers.fromActor(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeSourceDamageDisplay$Actor;Ljava/lang/Object;)Llocal/lito381/server/NativeSourceRawDamage$Member;
 ASTORE 10
```

### NativeSourceDollNpc

```diff
--- staged/NativeSourceDollNpc.txt
+++ candidate/NativeSourceDollNpc.txt
@@ -451,7 +451,7 @@
 LABEL L0
 RETURN

-METHOD lambda$nativeSourcePhysicalOriginalNatural$#0(Llocal/lito381/server/NativePlayerLifetime;Llocal/lito381/server/NativeSourceMonsterHp$Aim;Llocal/lito381/server/ClientSession;ILlocal/lito381/server/SourceWeaponPhysicalStats;Llocal/lito381/server/BattleWeaponCritical$RollOutcome;Llocal/lito381/server/BattleWeaponCritical$DamageOutcome;Llocal/lito381/server/SourceNpcPhysicalStats;Llocal/lito381/server/NativeMagicDollEffects$AttackProc;[IZJLjava/lang/String;)V access=10 signature=null exceptions=[]
+METHOD lambda$natural$#0(Llocal/lito381/server/NativePlayerLifetime;Llocal/lito381/server/NativeSourceMonsterHp$Aim;Llocal/lito381/server/ClientSession;ILlocal/lito381/server/SourceWeaponPhysicalStats;Llocal/lito381/server/BattleWeaponCritical$RollOutcome;Llocal/lito381/server/BattleWeaponCritical$DamageOutcome;Llocal/lito381/server/SourceNpcPhysicalStats;Llocal/lito381/server/NativeMagicDollEffects$AttackProc;[IZJLjava/lang/String;)V access=10 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
 TRY L3 L4 L5 java/io/IOException
 TRY L0 L2 L8 java/lang/Throwable
@@ -493,7 +493,7 @@
 ILOAD 10
 LLOAD 11/12
 ALOAD 13
-INVOKEDYNAMIC run(Llocal/lito381/server/NativeSourceMonsterHp$Aim;Llocal/lito381/server/ClientSession;ILlocal/lito381/server/SourceWeaponPhysicalStats;Llocal/lito381/server/BattleWeaponCritical$RollOutcome;Llocal/lito381/server/BattleWeaponCritical$DamageOutcome;Llocal/lito381/server/SourceNpcPhysicalStats;Llocal/lito381/server/NativeMagicDollEffects$AttackProc;[IZJLjava/lang/String;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeSourceDollNpc.lambda$nativeSourcePhysicalOriginalNatural$#1(Llocal/lito381/server/NativeSourceMonsterHp$Aim;Llocal/lito381/server/ClientSession;ILlocal/lito381/server/SourceWeaponPhysicalStats;Llocal/lito381/server/BattleWeaponCritical$RollOutcome;Llocal/lito381/server/BattleWeaponCritical$DamageOutcome;Llocal/lito381/server/SourceNpcPhysicalStats;Llocal/lito381/server/NativeMagicDollEffects$AttackProc;[IZJLjava/lang/String;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/NativeSourceMonsterHp$Aim;Llocal/lito381/server/ClientSession;ILlocal/lito381/server/SourceWeaponPhysicalStats;Llocal/lito381/server/BattleWeaponCritical$RollOutcome;Llocal/lito381/server/BattleWeaponCritical$DamageOutcome;Llocal/lito381/server/SourceNpcPhysicalStats;Llocal/lito381/server/NativeMagicDollEffects$AttackProc;[IZJLjava/lang/String;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeSourceDollNpc.lambda$natural$#1(Llocal/lito381/server/NativeSourceMonsterHp$Aim;Llocal/lito381/server/ClientSession;ILlocal/lito381/server/SourceWeaponPhysicalStats;Llocal/lito381/server/BattleWeaponCritical$RollOutcome;Llocal/lito381/server/BattleWeaponCritical$DamageOutcome;Llocal/lito381/server/SourceNpcPhysicalStats;Llocal/lito381/server/NativeMagicDollEffects$AttackProc;[IZJLjava/lang/String;)V, type:()V]
 INVOKESTATIC local/lito381/server/NativeLightAction.commitSelf(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)V
 LABEL L4
 GOTO L6
@@ -519,7 +519,7 @@
 LABEL L10
 RETURN

-METHOD lambda$nativeSourcePhysicalOriginalNatural$#1(Llocal/lito381/server/NativeSourceMonsterHp$Aim;Llocal/lito381/server/ClientSession;ILlocal/lito381/server/SourceWeaponPhysicalStats;Llocal/lito381/server/BattleWeaponCritical$RollOutcome;Llocal/lito381/server/BattleWeaponCritical$DamageOutcome;Llocal/lito381/server/SourceNpcPhysicalStats;Llocal/lito381/server/NativeMagicDollEffects$AttackProc;[IZJLjava/lang/String;)V access=10 signature=null exceptions=[java/io/IOException]
+METHOD lambda$natural$#1(Llocal/lito381/server/NativeSourceMonsterHp$Aim;Llocal/lito381/server/ClientSession;ILlocal/lito381/server/SourceWeaponPhysicalStats;Llocal/lito381/server/BattleWeaponCritical$RollOutcome;Llocal/lito381/server/BattleWeaponCritical$DamageOutcome;Llocal/lito381/server/SourceNpcPhysicalStats;Llocal/lito381/server/NativeMagicDollEffects$AttackProc;[IZJLjava/lang/String;)V access=10 signature=null exceptions=[java/io/IOException]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
 TRY L3 L4 L5 java/io/IOException
 TRY L3 L4 L5 java/lang/RuntimeException
@@ -581,7 +581,7 @@
 ILOAD 2
 ALOAD 12
 ALOAD 13
-INVOKEDYNAMIC run([ILlocal/lito381/server/ClientSession;ZJILjava/lang/String;Llocal/lito381/server/NativeSourceNpcDamageDisplay$Receipt;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeSourceDollNpc.lambda$nativeSourcePhysicalOriginalNatural$#2([ILlocal/lito381/server/ClientSession;ZJILjava/lang/String;Llocal/lito381/server/NativeSourceNpcDamageDisplay$Receipt;)V, type:()V]
+INVOKEDYNAMIC run([ILlocal/lito381/server/ClientSession;ZJILjava/lang/String;Llocal/lito381/server/NativeSourceNpcDamageDisplay$Receipt;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeSourceDollNpc.lambda$natural$#2([ILlocal/lito381/server/ClientSession;ZJILjava/lang/String;Llocal/lito381/server/NativeSourceNpcDamageDisplay$Receipt;)V, type:()V]
 INVOKESTATIC local/lito381/server/NativeSourceDollNpc.owned(Llocal/lito381/server/NativeSourceDollNpc$Scope;Llocal/lito381/server/NativeSelfCastAction$Task;)V
 LABEL L2
 ALOAD 13
@@ -746,7 +746,7 @@
 LABEL L32
 RETURN

-METHOD lambda$nativeSourcePhysicalOriginalNatural$#2([ILlocal/lito381/server/ClientSession;ZJILjava/lang/String;Llocal/lito381/server/NativeSourceNpcDamageDisplay$Receipt;)V access=10 signature=null exceptions=[java/io/IOException]
+METHOD lambda$natural$#2([ILlocal/lito381/server/ClientSession;ZJILjava/lang/String;Llocal/lito381/server/NativeSourceNpcDamageDisplay$Receipt;)V access=10 signature=null exceptions=[java/io/IOException]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
 ALOAD 0
 CONST Integer:0
@@ -867,7 +867,7 @@
 ILOAD 11
 LLOAD 1/2
 ALOAD 4
-INVOKEDYNAMIC run(Llocal/lito381/server/NativePlayerLifetime;Llocal/lito381/server/NativeSourceMonsterHp$Aim;Llocal/lito381/server/ClientSession;ILlocal/lito381/server/SourceWeaponPhysicalStats;Llocal/lito381/server/BattleWeaponCritical$RollOutcome;Llocal/lito381/server/BattleWeaponCritical$DamageOutcome;Llocal/lito381/server/SourceNpcPhysicalStats;Llocal/lito381/server/NativeMagicDollEffects$AttackProc;[IZJLjava/lang/String;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeSourceDollNpc.lambda$nativeSourcePhysicalOriginalNatural$#0(Llocal/lito381/server/NativePlayerLifetime;Llocal/lito381/server/NativeSourceMonsterHp$Aim;Llocal/lito381/server/ClientSession;ILlocal/lito381/server/SourceWeaponPhysicalStats;Llocal/lito381/server/BattleWeaponCritical$RollOutcome;Llocal/lito381/server/BattleWeaponCritical$DamageOutcome;Llocal/lito381/server/SourceNpcPhysicalStats;Llocal/lito381/server/NativeMagicDollEffects$AttackProc;[IZJLjava/lang/String;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/NativePlayerLifetime;Llocal/lito381/server/NativeSourceMonsterHp$Aim;Llocal/lito381/server/ClientSession;ILlocal/lito381/server/SourceWeaponPhysicalStats;Llocal/lito381/server/BattleWeaponCritical$RollOutcome;Llocal/lito381/server/BattleWeaponCritical$DamageOutcome;Llocal/lito381/server/SourceNpcPhysicalStats;Llocal/lito381/server/NativeMagicDollEffects$AttackProc;[IZJLjava/lang/String;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeSourceDollNpc.lambda$natural$#0(Llocal/lito381/server/NativePlayerLifetime;Llocal/lito381/server/NativeSourceMonsterHp$Aim;Llocal/lito381/server/ClientSession;ILlocal/lito381/server/SourceWeaponPhysicalStats;Llocal/lito381/server/BattleWeaponCritical$RollOutcome;Llocal/lito381/server/BattleWeaponCritical$DamageOutcome;Llocal/lito381/server/SourceNpcPhysicalStats;Llocal/lito381/server/NativeMagicDollEffects$AttackProc;[IZJLjava/lang/String;)V, type:()V]
 INVOKESTATIC local/lito381/server/NativeSourcePlayerActionLock.runPairOrdered(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Ljava/lang/Runnable;)V
 LABEL L8
 GOTO L10
```

### NativeSourceHealingCast

```diff
--- staged/NativeSourceHealingCast.txt
+++ candidate/NativeSourceHealingCast.txt
@@ -788,25 +788,21 @@
 INVOKEVIRTUAL java/util/ArrayList.add(Ljava/lang/Object;)Z
 POP
 LABEL L18
+ALOAD 11
 ALOAD 9
 DUP
 INVOKESTATIC java/util/Objects.requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
 POP
 INVOKEDYNAMIC getAsBoolean(Llocal/lito381/server/NativeSourceHealingCast$Cast;)Ljava/util/function/BooleanSupplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Z, handle:H5 local/lito381/server/NativeSourceHealingCast$Cast.before()Z, type:()Z]
-ASTORE 12
 ALOAD 0
 ALOAD 9
 ALOAD 7
 ALOAD 1
 ILOAD 2
 INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSourceHealingCast$Cast;Llocal/lito381/server/NativeAreaMaterialAction$Context;[Ljava/lang/Object;I)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeSourceHealingCast.lambda$run$#0(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSourceHealingCast$Cast;Llocal/lito381/server/NativeAreaMaterialAction$Context;[Ljava/lang/Object;I)V, type:()V]
-ASTORE 13
 ALOAD 7
 ALOAD 9
 INVOKESTATIC local/lito381/server/NativeSourceControlledHealing.capture(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativeSourceHealingCast$Cast;)V
-ALOAD 11
-ALOAD 12
-ALOAD 13
 INVOKESTATIC local/lito381/server/NativeSourceHealingCast.owned(Ljava/util/List;Ljava/util/function/BooleanSupplier;Llocal/lito381/server/NativeSelfCastAction$Task;)V
 LABEL L19
 ALOAD 7
@@ -819,7 +815,7 @@
 INVOKEVIRTUAL local/lito381/server/NativeSourceHealingCast$Cast.close()V
 GOTO L23
 LABEL L20
-ASTORE 14
+ASTORE 13
 LABEL L21
 ALOAD 7
 ALOAD 10
@@ -830,7 +826,7 @@
 ALOAD 9
 INVOKEVIRTUAL local/lito381/server/NativeSourceHealingCast$Cast.close()V
 LABEL L22
-ALOAD 14
+ALOAD 13
 ATHROW
 LABEL L23
 RETURN
```

### NativeWeaponBreakBattleCompanion

```diff
--- staged/NativeWeaponBreakBattleCompanion.txt
+++ candidate/NativeWeaponBreakBattleCompanion.txt
@@ -569,8 +569,7 @@

 METHOD nativeSourceWeaponFirstPrepare(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/NativeAreaMaterialAction$Context;)Llocal/lito381/server/SourceNpcPhysicalStats; access=8 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-TRY L0 L1 L4 java/lang/Exception
-TRY L2 L3 L4 java/lang/Exception
+TRY L0 L2 L3 java/lang/Exception
 LABEL L0
 ALOAD 0
 CONST str:"selectedSourceAccounts"
@@ -579,24 +578,21 @@
 ANEWARRAY java/lang/Object
 INVOKESTATIC local/lito381/server/CombatAccess.call(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
 CHECKCAST local/lito381/server/AccountRepository
-ASTORE 3
 ALOAD 1
 ALOAD 2
 INVOKESTATIC local/lito381/server/NativeSourceWeaponTargetLife.snapshot(Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/NativeAreaMaterialAction$Context;)Llocal/lito381/server/BattleCompanion$Snapshot;
-ASTORE 4
-ALOAD 4
-IFNONNULL L2
+DUP
+IFNONNULL L1
+POP
+POP
 CONST null
+ARETURN
 LABEL L1
-ARETURN
-LABEL L2
-ALOAD 3
-ALOAD 4
 INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.npcId()I
 INVOKEINTERFACE local/lito381/server/AccountRepository.npcPhysicalStats(I)Llocal/lito381/server/SourceNpcPhysicalStats; itf
+LABEL L2
+ARETURN
 LABEL L3
-ARETURN
-LABEL L4
 ASTORE 3
 CONST type:Llocal/lito381/server/NativeWeaponBreakBattleCompanion;
 INVOKESTATIC local/lito381/server/NativeReuseLog.getLogger(Ljava/lang/Class;)Llocal/lito381/server/NativeReuseLog$Logger;
```

### NativeWeaponBreakTargetLifecycle

```diff
--- staged/NativeWeaponBreakTargetLifecycle.txt
+++ candidate/NativeWeaponBreakTargetLifecycle.txt
@@ -90,7 +90,7 @@
 INVOKESTATIC local/lito381/server/NativeWeaponBreakMaterialAction.nativeTargetOriginalAdmit(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;III)Z
 IRETURN

-METHOD captureOutput(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)Z access=8 signature=null exceptions=[java/io/IOException]
+METHOD captureOutput(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)Z access=8 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
 ALOAD 0
 ALOAD 1
@@ -224,7 +224,7 @@
 GETFIELD local/lito381/server/NativeAreaMaterialAction$Context.players:Ljava/util/List;
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
 LLOAD 9/10
-INVOKEDYNAMIC test(J)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$captureOutput$#5(JLlocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
+INVOKEDYNAMIC test(J)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$dispatch$#0(JLlocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
 INVOKEINTERFACE java/util/stream/Stream.filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream; itf
 INVOKEINTERFACE java/util/stream/Stream.toList()Ljava/util/List; itf
 ASTORE 12
@@ -333,7 +333,10 @@
 ALOAD 3
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
 ALOAD 0
-INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$captureOutput$#4(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
+DUP
+INVOKESTATIC java/util/Objects.requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
+POP
+INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeSourceIndirectPlayerProof.player(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
 INVOKEINTERFACE java/util/stream/Stream.allMatch(Ljava/util/function/Predicate;)Z itf
 IFEQ L3
 ILOAD 4
@@ -374,7 +377,10 @@
 ALOAD 3
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
 ALOAD 0
-INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$captureOutput$#3(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
+DUP
+INVOKESTATIC java/util/Objects.requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;
+POP
+INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeSourceIndirectPlayerProof.player(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
 INVOKEINTERFACE java/util/stream/Stream.allMatch(Ljava/util/function/Predicate;)Z itf
 IFEQ L11
 ILOAD 4
@@ -422,21 +428,7 @@
 LABEL L21
 RETURN

-METHOD lambda$captureOutput$#3(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z access=10 signature=null exceptions=[]
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-ALOAD 0
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativeSourceIndirectPlayerProof.player(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z
-IRETURN
-
-METHOD lambda$captureOutput$#4(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z access=10 signature=null exceptions=[]
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-ALOAD 0
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativeSourceIndirectPlayerProof.player(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z
-IRETURN
-
-METHOD lambda$captureOutput$#5(JLlocal/lito381/server/ClientSession;)Z access=10 signature=null exceptions=[]
+METHOD lambda$dispatch$#0(JLlocal/lito381/server/ClientSession;)Z access=10 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/ClientSession.playerObjectId()J
@@ -450,35 +442,7 @@
 LABEL L1
 IRETURN

-METHOD lambda$nativeSourceWeaponFirstDispatch$#0(JLlocal/lito381/server/ClientSession;)Z access=10 signature=null exceptions=[]
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/ClientSession.playerObjectId()J
-LLOAD 0/1
-LCMP
-IFNE L0
-CONST Integer:1
-GOTO L1
-LABEL L0
-CONST Integer:0
-LABEL L1
-IRETURN
-
-METHOD lambda$nativeSourceWeaponOriginalDispatch$#0(JLlocal/lito381/server/ClientSession;)Z access=10 signature=null exceptions=[]
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/ClientSession.playerObjectId()J
-LLOAD 0/1
-LCMP
-IFNE L0
-CONST Integer:1
-GOTO L1
-LABEL L0
-CONST Integer:0
-LABEL L1
-IRETURN
-
-METHOD lambda$run$#0(Llocal/lito381/server/ClientSession;ZLlocal/lito381/server/NativeAreaMaterialAction$Npc;Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/GameHub;JLjava/lang/Object;[BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;Llocal/lito381/server/ClientSession;)V access=10 signature=null exceptions=[java/io/IOException]
+METHOD lambda$run$#0(Llocal/lito381/server/ClientSession;ZLlocal/lito381/server/NativeAreaMaterialAction$Npc;Llocal/lito381/server/GameHub;Llocal/lito381/server/NativeAreaMaterialAction$Context;JLjava/lang/Object;[BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;Llocal/lito381/server/ClientSession;)V access=10 signature=null exceptions=[java/io/IOException]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
 ALOAD 0
 CONST Integer:1
@@ -493,11 +457,11 @@
 ALOAD 8
 ALOAD 9
 ALOAD 10
-INVOKEDYNAMIC run(ZLlocal/lito381/server/NativeAreaMaterialAction$Npc;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/GameHub;JLjava/lang/Object;[BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;Llocal/lito381/server/ClientSession;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$run$#1(ZLlocal/lito381/server/NativeAreaMaterialAction$Npc;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/GameHub;JLjava/lang/Object;[BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;Llocal/lito381/server/ClientSession;)V, type:()V]
+INVOKEDYNAMIC run(ZLlocal/lito381/server/NativeAreaMaterialAction$Npc;Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/NativeAreaMaterialAction$Context;JLjava/lang/Object;[BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;Llocal/lito381/server/ClientSession;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$run$#1(ZLlocal/lito381/server/NativeAreaMaterialAction$Npc;Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/NativeAreaMaterialAction$Context;JLjava/lang/Object;[BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;Llocal/lito381/server/ClientSession;)V, type:()V]
 INVOKESTATIC local/lito381/server/NativeWorldMaterialPolicy.run(Llocal/lito381/server/ClientSession;ZILlocal/lito381/server/NativeSelfCastAction$Task;)V
 RETURN

-METHOD lambda$run$#1(ZLlocal/lito381/server/NativeAreaMaterialAction$Npc;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/GameHub;JLjava/lang/Object;[BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;Llocal/lito381/server/ClientSession;)V access=10 signature=null exceptions=[java/io/IOException]
+METHOD lambda$run$#1(ZLlocal/lito381/server/NativeAreaMaterialAction$Npc;Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/NativeAreaMaterialAction$Context;JLjava/lang/Object;[BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;Llocal/lito381/server/ClientSession;)V access=10 signature=null exceptions=[java/io/IOException]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
 TRY L6 L8 L12 java/lang/Throwable
 TRY L10 L11 L12 java/lang/Throwable
@@ -516,22 +480,22 @@
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Npc.actor()Ljava/lang/Object;
 CHECKCAST local/lito381/server/BattleCompanion
-ALOAD 3
+ALOAD 4
 INVOKESTATIC local/lito381/server/NativeSourceWeaponTargetLife.prepare(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/NativeAreaMaterialAction$Context;)Llocal/lito381/server/SourceNpcPhysicalStats;
 GOTO L1
 LABEL L0
 CONST null
 LABEL L1
 ASTORE 11
-ALOAD 4
+ALOAD 3
 DUP
 ASTORE 12
 MONITORENTER
 LABEL L2
-ALOAD 3
+ALOAD 4
 INVOKESTATIC local/lito381/server/NativeSourceWeaponTargetLife.scene(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Z
 IFEQ L3
-ALOAD 3
+ALOAD 4
 LLOAD 5/6
 INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Context.npc(J)Z
 IFNE L5
@@ -546,10 +510,10 @@
 ASTORE 13
 MONITORENTER
 LABEL L6
-ALOAD 3
+ALOAD 4
 INVOKESTATIC local/lito381/server/NativeSourceWeaponTargetLife.scene(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Z
 IFEQ L7
-ALOAD 3
+ALOAD 4
 LLOAD 5/6
 INVOKEVIRTUAL local/lito381/server/NativeAreaMaterialAction$Context.npc(J)Z
 IFNE L10
@@ -562,8 +526,8 @@
 LABEL L9
 RETURN
 LABEL L10
-ALOAD 3
-ALOAD 3
+ALOAD 4
+ALOAD 4
 ILOAD 0
 ALOAD 2
 ALOAD 8
@@ -655,20 +619,6 @@
 LABEL L6
 RETURN

-METHOD lambda$run$#3(JLlocal/lito381/server/ClientSession;)Z access=10 signature=null exceptions=[]
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/ClientSession.playerObjectId()J
-LLOAD 0/1
-LCMP
-IFNE L0
-CONST Integer:1
-GOTO L1
-LABEL L0
-CONST Integer:0
-LABEL L1
-IRETURN
-
 METHOD nativeSourceMaterialPairTarget(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)Llocal/lito381/server/NativePlayerVitalCompletions$Receipt; access=8 signature=null exceptions=[]
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv= default=
 GETSTATIC local/lito381/server/NativeAreaMaterialAction.current:Ljava/lang/ThreadLocal;
@@ -791,7 +741,7 @@
 ALOAD 9
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
 LLOAD 6/7
-INVOKEDYNAMIC test(J)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$nativeSourceWeaponFirstDispatch$#0(JLlocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
+INVOKEDYNAMIC test(J)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$dispatch$#0(JLlocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
 INVOKEINTERFACE java/util/stream/Stream.filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream; itf
 INVOKEINTERFACE java/util/stream/Stream.findFirst()Ljava/util/Optional; itf
 ALOAD 0
@@ -999,7 +949,7 @@
 ALOAD 7
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
 LLOAD 4/5
-INVOKEDYNAMIC test(J)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$nativeSourceWeaponOriginalDispatch$#0(JLlocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
+INVOKEDYNAMIC test(J)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$dispatch$#0(JLlocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
 INVOKEINTERFACE java/util/stream/Stream.filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream; itf
 INVOKEINTERFACE java/util/stream/Stream.findFirst()Ljava/util/Optional; itf
 ALOAD 0
@@ -1241,7 +1191,7 @@
 ALOAD 8
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
 LLOAD 5/6
-INVOKEDYNAMIC test(J)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$run$#3(JLlocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
+INVOKEDYNAMIC test(J)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$dispatch$#0(JLlocal/lito381/server/ClientSession;)Z, type:(Llocal/lito381/server/ClientSession;)Z]
 INVOKEINTERFACE java/util/stream/Stream.anyMatch(Ljava/util/function/Predicate;)Z itf
 IFNE L5
 ALOAD 0
@@ -1344,14 +1294,14 @@
 ALOAD 0
 ILOAD 4
 ALOAD 11
+ALOAD 7
 ALOAD 10
-ALOAD 7
 LLOAD 5/6
 ALOAD 13
 ALOAD 1
 ALOAD 2
 ALOAD 12
-INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;ZLlocal/lito381/server/NativeAreaMaterialAction$Npc;Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/GameHub;JLjava/lang/Object;[BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;Llocal/lito381/server/ClientSession;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$run$#0(Llocal/lito381/server/ClientSession;ZLlocal/lito381/server/NativeAreaMaterialAction$Npc;Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/GameHub;JLjava/lang/Object;[BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;Llocal/lito381/server/ClientSession;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;ZLlocal/lito381/server/NativeAreaMaterialAction$Npc;Llocal/lito381/server/GameHub;Llocal/lito381/server/NativeAreaMaterialAction$Context;JLjava/lang/Object;[BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;Llocal/lito381/server/ClientSession;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeWeaponBreakTargetLifecycle.lambda$run$#0(Llocal/lito381/server/ClientSession;ZLlocal/lito381/server/NativeAreaMaterialAction$Npc;Llocal/lito381/server/GameHub;Llocal/lito381/server/NativeAreaMaterialAction$Context;JLjava/lang/Object;[BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;Llocal/lito381/server/ClientSession;)V, type:()V]
 INVOKESTATIC local/lito381/server/NativeLightAction.commitPair(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)V
 RETURN

```

## 附錄 D：原始 semantic-diff JSON

工具的 firstDiff 本身會截短過長字串；完整未截短內容以上列附錄 C 為準。

```json
{
  "tool": "semantic-diff",
  "generatedAt": "2026-11-05T02:09:45Z",
  "left": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\target\\issue-62\\final\\staged.jar",
  "right": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-62\\lito伺服器\\LiTo381-Server\\reference\\candidate-50635f7e.jar",
  "allowRight": null,
  "effectsMethodsFile": null,
  "extraMethodsFile": null,
  "metadataExemptFile": null,
  "extraMethods": [],
  "metadataExempt": [],
  "leftClassCount": 22,
  "rightClassCount": 3033,
  "comparedClassCount": 22,
  "classSelection": {
    "missingLeft": [],
    "extraLeft": []
  },
  "byteSameCount": 0,
  "counts": {
    "SAME": 13,
    "SAME_EFFECTS": 0,
    "DIFF": 9,
    "ONLY_LEFT": 0,
    "ONLY_RIGHT": 0,
    "BYTE_SAME": 0
  },
  "classes": [
    {"name": "local/lito381/server/NativeBlindSceneAction", "status": "DIFF", "byteSame": false, "allowMethodsApplied": false, "effectsMethods": [], "firstDiff": {"where": "method", "member": "companion(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleSpellCatalog$BlindSpell;)V", "insnIndex": 330, "detail": "left=INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/Metho… right=DUP"}},
    {"name": "local/lito381/server/NativeCompanionForestArea", "status": "DIFF", "byteSame": false, "allowMethodsApplied": false, "effectsMethods": [], "firstDiff": {"where": "method", "member": "apply(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleSpellCatalog$AreaSilenceSpell;IIIIJ)V", "insnIndex": 164, "detail": "left=INVOKEDYNAMIC test(Llocal/lito381/server/NativeAreaMaterialAction$Context;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/Metho… right=DUP"}},
    {"name": "local/lito381/server/NativeCompanionRefScene", "status": "SAME", "byteSame": false, "allowMethodsApplied": false, "effectsMethods": []},
    {"name": "local/lito381/server/NativeCompanionWindAction", "status": "DIFF", "byteSame": false, "allowMethodsApplied": false, "effectsMethods": [], "firstDiff": {"where": "method", "member": "lambda$publish$#0(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/ClientSession;)Z", "insnIndex": null, "detail": "only left"}},
    {"name": "local/lito381/server/NativeControlledNegativeMaterial", "status": "DIFF", "byteSame": false, "allowMethodsApplied": false, "effectsMethods": [], "firstDiff": {"where": "method", "member": "cast(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/NativeSelfCastAction$Task;)V", "insnIndex": null, "detail": "left=TRY L15 L16 L17 java/lang/Throwable   right=TRY L11 L12 L13 java/lang/Throwable  "}},
    {"name": "local/lito381/server/NativePairedCastSafety", "status": "SAME", "byteSame": false, "allowMethodsApplied": false, "effectsMethods": []},
    {"name": "local/lito381/server/NativeSourceBatchTerminal", "status": "SAME", "byteSame": false, "allowMethodsApplied": false, "effectsMethods": []},
    {"name": "local/lito381/server/NativeSourceCubePlacement", "status": "SAME", "byteSame": false, "allowMethodsApplied": false, "effectsMethods": []},
    {"name": "local/lito381/server/NativeSourceCubePlacement$Draft", "status": "SAME", "byteSame": false, "allowMethodsApplied": false, "effectsMethods": []},
    {"name": "local/lito381/server/NativeSourceDollAccepted", "status": "DIFF", "byteSame": false, "allowMethodsApplied": false, "effectsMethods": [], "firstDiff": {"where": "method", "member": "physical(Llocal/lito381/server/ClientSession;ILlocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSourceKiringkuAdmission$Aim;Llocal/lito381/server/SourceWeaponPhysicalStats;Llocal/lito381/server/BattleWeaponCritical$RollOutcome;Llocal/lito381/server/BattleWeaponCritical$DamageOutcome;Llocal/lito381/server/NativeMagicDollEffects$AttackProc;)V", "insnIndex": 45, "detail": "left=ALOAD 8 right=DUP"}},
    {"name": "local/lito381/server/NativeSourceDollAccepted$Pending", "status": "SAME", "byteSame": false, "allowMethodsApplied": false, "effectsMethods": []},
    {"name": "local/lito381/server/NativeSourceDollNpc", "status": "DIFF", "byteSame": false, "allowMethodsApplied": false, "effectsMethods": [], "firstDiff": {"where": "method", "member": "lambda$nativeSourcePhysicalOriginalNatural$#0(Llocal/lito381/server/NativePlayerLifetime;Llocal/lito381/server/NativeSourceMonsterHp$Aim;Llocal/lito381/server/ClientSession;ILlocal/lito381/server/SourceWeaponPhysicalStats;Llocal/lito381/server/BattleWeaponCritical$RollOutcome;Llocal/lito381/server/BattleWeaponCritical$DamageOutcome;Llocal/lito381/server/SourceNpcPhysicalStats;Llocal/lito381/server/NativeMagicDollEffects$AttackProc;[IZJLjava/lang/String;)V", "insnIndex": null, "detail": "only left"}},
    {"name": "local/lito381/server/NativeSourceDollNpc$Scope", "status": "SAME", "byteSame": false, "allowMethodsApplied": false, "effectsMethods": []},
    {"name": "local/lito381/server/NativeSourceDollSkills", "status": "SAME", "byteSame": false, "allowMethodsApplied": false, "effectsMethods": []},
    {"name": "local/lito381/server/NativeSourceDollSkills$View", "status": "SAME", "byteSame": false, "allowMethodsApplied": false, "effectsMethods": []},
    {"name": "local/lito381/server/NativeSourceDollSlow", "status": "SAME", "byteSame": false, "allowMethodsApplied": false, "effectsMethods": []},
    {"name": "local/lito381/server/NativeSourceHealingCast", "status": "DIFF", "byteSame": false, "allowMethodsApplied": false, "effectsMethods": [], "firstDiff": {"where": "method", "member": "run(Llocal/lito381/server/ClientSession;[Ljava/lang/Object;I)V", "insnIndex": 172, "detail": "left=ALOAD 9 right=ALOAD 11"}},
    {"name": "local/lito381/server/NativeSourceHealingCast$Cast", "status": "SAME", "byteSame": false, "allowMethodsApplied": false, "effectsMethods": []},
    {"name": "local/lito381/server/NativeSourcePendingAdmission", "status": "SAME", "byteSame": false, "allowMethodsApplied": false, "effectsMethods": []},
    {"name": "local/lito381/server/NativeSummonTerminalLifecycle", "status": "SAME", "byteSame": false, "allowMethodsApplied": false, "effectsMethods": []},
    {"name": "local/lito381/server/NativeWeaponBreakBattleCompanion", "status": "DIFF", "byteSame": false, "allowMethodsApplied": false, "effectsMethods": [], "firstDiff": {"where": "method", "member": "nativeSourceWeaponFirstPrepare(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/NativeAreaMaterialAction$Context;)Llocal/lito381/server/SourceNpcPhysicalStats;", "insnIndex": null, "detail": "left=TRY L0 L1 L4 java/lang/Exception   right=TRY L0 L2 L3 java/lang/Exception  "}},
    {"name": "local/lito381/server/NativeWeaponBreakTargetLifecycle", "status": "DIFF", "byteSame": false, "allowMethodsApplied": false, "effectsMethods": [], "firstDiff": {"where": "method", "member": "captureOutput(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)Z", "insnIndex": null, "detail": "left=java/io/IOException right="}}
  ]
}
```

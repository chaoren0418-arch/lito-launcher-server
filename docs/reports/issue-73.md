# Issue #73 — A02 正式 0.6.71 暫存移植與剩餘裁定

## 結論

**尚未完成整批驗收，不可送 A-land。** 2026-11-06 在既有 `task/73` 整合已合併依賴後，49 份 staged Java 全數編譯成功；104 個驗收 entry 為 **86 SAME、16 SAME_PATHS、2 DIFF**。ONLY_LEFT／ONLY_RIGHT 都為 0，missingLeft／extraLeft 都為空。SAME_PATHS 是已合併 #176 的逐路徑比較結果，未改稱 SAME 或 SAME_EFFECTS；原派工只列 SAME／SAME_EFFECTS，主控也須確認採用目前工具的狀態語彙。即使接受 SAME_PATHS，仍有兩個實際 DIFF。

Issue #73 與 PR #101 已加 `needs:claude`。本報告取代上一輪 71 SAME／31 DIFF／2 缺漏的報告，歷史證據保留在 git。兩個剩餘類別是 `NativeDeItemWeaponProcTable` 與 `NativeDeNpcConstructionState`；完整正規化差異、編譯反證及逐方法結果附於下方。

## 做了什麼

- 保留 `task/73` 歷史，未 checkout／建分支／force push。合併 main `c0ba98f`，整合提交 `6a2873e6447e04c0556a328fe6e1d3d9a8dceca9`。正式工具 Java 最後變更 `7554762bd0267d937d798ab7aff39b9d3b623384`。#108、#135、#153、#166、A03 #74 均已包含；另包含主線已合併的 #174／#176／#186。
- 以 Vineflower 1.11.1 重新反編譯正式 JAR 中本批的 104 entry，對照前輪 CFR／Vineflower 來源修正 19 個 staged Java；49 份來源均保留 `DECOMPILED from base-0.6.71.jar` 標頭。
- 修正 ItemInventory 的綁定／複製分支、StateCopy 的 expiry 分支、LiveAiJobs 的提早返回與背景 tick 分支、NpcKnowledge／HateList 的迴圈跳過路徑、NPC 模板 switch 順序、VitalState 兩處 lambda 的提早返回與 enum case、PerceptionPacket 初始化順序及 catch 順序、ObjectFields 的 public 建構子。
- 各資料表載入器保留每筆資料失敗後繼續讀取的流程，修正 Map／List 呼叫型別與布林表示；GreaterTierTable 保留正式 `debug(String,Object,Object)` 呼叫，使用 A03 真實 staged API，未改為 emit。
- ItemTable 六個 map 在 `<clinit>` 先建立後包成不可變 map，Java final 確定賦值規則無法直接表達兩次賦值。依 [#62 通用規則 A](https://github.com/chaoren0418-arch/lito-launcher-server/issues/62#issuecomment-6644529572) 與 #153，使用 `@AsmFinal` 還原 `_armorTypes`、`_weaponTypes`、`_weaponId`、`_materialTypes`、`_etcItemTypes`、`_useTypes` 的 FINAL 旗標。正式轉換器只改 `NativeDeItemTable.class`；額外 ASM tree 稽核證實恰為六個 FINAL 旗標加註解移除，其他內容完全相同。
- 沒有新增 effects／access／deviation 核准，沒有改比較工具。完整 PR 範圍恰為 A02 的 49 份 staged Java 與本報告；本輪修改其中 19 份 Java。`src/main`、`pom.xml`、`docs/REPORT.md` 相對 main 無本批差異；合併帶入的主線檔案不是本批自行修改。

## 已驗證

環境：Microsoft OpenJDK 17.0.17、Python 3.12.10、ASM 9.7。每輪使用全新輸出目錄，沒有混入上一輪 class。最終證據位於 `scratch/issue-73/final-current/`。

| 驗證 | 結果 |
|---|---|
| #108 compile-stubs | 9 份 Java 編譯成功，僅供 classpath |
| A03 NativeDeWorldLog、AsmFinal 註解 | 分開編譯至依賴目錄，不打入 A02 JAR |
| A02 49 份 Java | javac exit 0，104 class |
| 核准 AsmFinal 轉換 | exit 0，scanned=104 converted=1 |
| 正式 semantic-diff | exit 0；86 SAME／16 SAME_PATHS／2 DIFF |
| class 集合 | 精確等於 A02.verify_entries；missing=[]、extra=[] |
| 打包 bytes | 每個 JAR entry 等於本輪轉換後 class；103 個 class 與 javac 原始 bytes 相同 |
| 轉換差異稽核 | 僅 ItemTable 六個 FINAL 旗標與註解移除 |
| 原始碼／差異範圍 | 49 標頭齊全；git diff --check 通過；無忽略檔強加 |

使用 `--left-classpath reference/base-0.6.71.jar` 讓比較器解析本批以外的欄位擁有者與 volatile 屬性。這只提供中繼資料，不擴大左側驗收 entry，也沒有把正式 class bytes 放進 A02 產物。編譯 classpath 由 A03 依賴目錄優先，才接正式 JAR、#108 stub、lib/*，避免正式 JAR 中缺少 debug 的介面遮蔽已核准 API。

核准清單交集只有 `--access-widen` 的下列兩條，其餘清單在 A02 無條目；未啟用 effects、layout、deviations 或例外豁免。

```text
local/lito381/server/NativeDeNpcVitalState#getX()I
local/lito381/server/NativeDeNpcVitalState#getY()I
```

兩者都是左側 public、正式 protected，依 #73／#153 裁定；內容仍正常比較。

## 待主控裁定的 C 類差異

### 1. NativeDeItemWeaponProcTable

- 正式 class 的父類是 Object，欄位表不存在 `_instance:Llocal/lito381/server/NativeDeItemWeaponProcTable;`，但 `reload()V` 仍以 PUTSTATIC／GETSTATIC 引用它。
- staged 延續前輪的 private static `_instance`，故可編譯，但這是**尚未核准的新增欄位**；本輪沒有隱藏它或宣告等價。其餘方法已對齊，`load` 改用正式的 Map 介面呼叫。
- 在 scratch 複本移除 `_instance`，以相同 49 檔及依賴重編，javac exit 1，兩處 cannot find symbol，原始輸出如下。推測實際執行正式 reload 會有欄位解析失敗；本輪沒有執行它，不將此推測當執行期實測。
- 請主控決定此欄位／reload 的正式修補或刻意偏離政策；目前清單沒有欄位豁免，不能自行加入 effects 了事。

### 2. NativeDeNpcConstructionState

- class 的 ACC_SUPER 已由既有規則處理，**不再是阻擋**。本輪已修正 `calcSleepTime(II)I` 的 ActionType 暫存與 switch；該方法現在通過 SAME_PATHS。
- javac 自動產生 **176 個正式版沒有的 public synthetic bridge 方法**。額外讀取 class 的稽核證明這 176 個方法僅載入參數、對父類同名同描述子執行一次 INVOKESPECIAL、再返回；完整描述子與本體全部在附錄。這是編譯器為 public 子類／非 public 父類鏈產生的方法，不是手寫新增。現行工具沒有此核准，不刪除產物方法或改比較器。
- `cancelNpcChatTimer()V`：正式 private，staged package；改回 private 的 scratch 複本會因 `NativeDeDeletionState.cancelNpcChatTimer()V` 已為 package 而 javac exit 1（attempting to assign weaker access privileges）。同時 `sourceChat$startChat(I)V` 的對應呼叫仍為 INVOKEVIRTUAL／INVOKESPECIAL 差異，涉及方法解析，不能假定只放寬 access 就足夠。
- 請主控裁定橋接方法的受控比較方式，以及這個方法與父類的可見性／呼叫策略，再交後續實作者與工具任務處理。

## 未驗證什麼／給下一棒的提示

- **未通過整批驗收**；不要由 CLI exit 0、CI 綠或 102 個非 DIFF entry 推斷 A02 已完成。
- 未跑單元測試、Maven 回歸及實機驗收，依派工留待 A-land；未啟動伺服器／遊戲／登入器，未連 DB、未讀機密設定，未碰封存與客戶端。
- Claude 主控先處理上述兩類 C 問題與 SAME_PATHS 狀態政策，再補相依任務及核准清單。之後本機執行器在既有 task/73 整合核准依賴，沿用此版來源與乾淨重現流程重驗；不要從零反編譯，也不要重提已解決的 debug 或 ACC_SUPER。
- 若採用本版 ItemTable，A-land 必須保留既有 Maven AsmFinal 建置步驟；A03 NativeDeWorldLog.debug 必須一起落地。使用者只在落地後做實機驗收，合併仍依既有流程由使用者處理。

## 產出與重現

- 追蹤：`lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/` 的 A02 49 份 Java、`docs/reports/issue-73.md`。
- 不追蹤：`scratch/issue-73/final-current/`（JSON、日誌、104 entry 清單、正規化 dump、target/classes 與 a02.jar）、`scratch/issue-73/blockers/`（兩個編譯反證）。
- 分支：`task/73`；既有 [PR #101](https://github.com/chaoren0418-arch/lito-launcher-server/pull/101)。

下列 Python 為本輪實際驗證程式，存成 worktree 的 `scratch/issue-73/verify_current.py`，在 worktree 根目錄用 Python 3.12 執行 `scratch/issue-73/verify_current.py <未使用的輸出名稱>`。它從 a-batches.json 精確選 49 檔／104 entry，不使用 NativeDe*.java 萬用字元。

ASM analysis 9.7 若本機未備妥，下載到 `scratch/issue-73/target/dependency/asm-analysis-9.7.jar`；程式驗證 SHA-256 `7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f`。其餘 ASM 依賴使用下方收據指定的本機快取，只讀取不改寫。

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
for filename,flag in [('access-widen-approved.txt','--access-widen'),('effects-approved.txt','--effects-methods')]:
    lines=[s for s in (root/'docs/combat-merge'/filename).read_text(encoding='utf-8-sig').splitlines() if s.strip() and not s.startswith('#') and s.split('#')[0].strip()+'.class' in actual]
    if lines:
        out=work/filename; out.write_text('\n'.join(lines)+'\n',encoding='utf-8'); flags += [flag,out]
run('semantic',[*java,'-cp',cp,'local.lito381.semanticdiff.SemanticDiff','--left',jar,'--right',formal,'--left-classpath',formal,'--classes',entries,*flags,'--json',work/'semantic.json','--md',work/'semantic.md'])
result=json.loads((work/'semantic.json').read_text(encoding='utf-8'))
receipt={'counts':result['counts'],'classSelection':result['classSelection'],'hashes':{str(p.relative_to(root)) if p.is_relative_to(root) else str(p):sha(p) for p in [formal,jar,*sources,stage/'NativeDeWorldLog.java',*toolsrc,*asm]},'entries':len(actual)}
for p in [*sorted((server/'compile-stubs').rglob('*.java')),*sorted((server/'tools/as-record/src/main/java').rglob('*.java')),server/'src/main/java/local/lito381/server/AsmFinal.java',*sorted((root/'docs/combat-merge').glob('*approved.txt')),root/'docs/combat-merge/a-batches.json',Path(__file__),work/'conversion.json',work/'commands.json',entries,work/'semantic.json',raw/'NativeDeItemTable.class']:
    receipt['hashes'][p.relative_to(root).as_posix() if p.is_absolute() else p.as_posix()]=sha(p)
(work/'receipt.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(result['counts']))
for c in result['classes']:
    if c['status'] not in ('SAME','SAME_EFFECTS'): print(c['name'],c['status'],json.dumps(c.get('firstDiff',{}),ensure_ascii=False))

```

## 原始執行輸出與完整命令

### stubs-javac（exit 0）

```json
[
  "C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\javac.exe",
  "-J-Dfile.encoding=UTF-8",
  "--release",
  "17",
  "-encoding",
  "UTF-8",
  "-d",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\scratch\\issue-73\\final-current\\target\\legacy-stubs",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\compile-stubs\\com\\lineage\\server\\datatables\\QuestInstanceTable.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\compile-stubs\\com\\lineage\\server\\model\\Instance\\DeathLifecycleGate.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\compile-stubs\\com\\lineage\\server\\model\\Instance\\NpcDeletionVisibilityState.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\compile-stubs\\com\\lineage\\server\\model\\item\\ItemGenerationContext.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\compile-stubs\\com\\lineage\\server\\model\\proc\\ProcRandomHelper.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\compile-stubs\\com\\lineage\\server\\model\\skill\\L1SkillId.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\compile-stubs\\com\\lineage\\server\\utils\\TeleportTrace.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\compile-stubs\\com\\lineage\\server\\world\\PlayerFilter.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\compile-stubs\\com\\lineage\\server\\world\\World.java"
]
```

```text

```

### a03-javac（exit 0）

```json
[
  "C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\javac.exe",
  "-J-Dfile.encoding=UTF-8",
  "--release",
  "17",
  "-encoding",
  "UTF-8",
  "-cp",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\reference\\base-0.6.71.jar",
  "-d",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\scratch\\issue-73\\final-current\\target\\a03",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldLog.java"
]
```

```text

```

### annotations-javac（exit 0）

```json
[
  "C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\javac.exe",
  "-J-Dfile.encoding=UTF-8",
  "--release",
  "17",
  "-encoding",
  "UTF-8",
  "-d",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\scratch\\issue-73\\final-current\\target\\a03",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\src\\main\\java\\local\\lito381\\server\\AsmFinal.java"
]
```

```text

```

### a02-javac（exit 0）

```json
[
  "C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\javac.exe",
  "-J-Dfile.encoding=UTF-8",
  "--release",
  "17",
  "-encoding",
  "UTF-8",
  "-cp",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\scratch\\issue-73\\final-current\\target\\a03;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\reference\\base-0.6.71.jar;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\scratch\\issue-73\\final-current\\target\\legacy-stubs;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server/lib/*",
  "-d",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\scratch\\issue-73\\final-current\\target\\classes",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemGreaterTierTable.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemIdStatsTable.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemInlayCardTable.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemInventory.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemMagicStoneTable.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemPublication.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemStateCopy.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemTable.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemTemplatesResources.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemTemplatesScope.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemTransaction.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemWeaponAttrTable.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemWeaponProc.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemWeaponProcTable.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeKnownState.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeLiveAiJobs.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeLiveAiPorts.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeLiveAiScope.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcConstructionState.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcExecutor.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcHateList.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcInventory.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcKnowledge.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcMobSkill.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcMobSkillUse.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcMoveState.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcTemplate.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcTemplateLoadScope.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcTemplateMobLoader.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcTemplateNpcLoader.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcTemplateResources.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcTemplateState.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcVitalState.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeObjectBindings.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeObjectFields.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeObjectScope.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDePerceptionAccess.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDePerceptionBadText.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDePerceptionControls.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDePerceptionEmblem.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDePerceptionJobs.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDePerceptionNPCPack_De.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDePerceptionOutput.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDePerceptionPacket.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDePerceptionScope.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDePerceptionSpeed.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDePerceptionState.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeProfileKernel.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeShopData.java"
]
```

```text
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
```

### converter-javac（exit 0）

```json
[
  "C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\javac.exe",
  "-J-Dfile.encoding=UTF-8",
  "--release",
  "17",
  "-encoding",
  "UTF-8",
  "-cp",
  "D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm\\9.7\\asm-9.7.jar;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm-tree\\9.7\\asm-tree-9.7.jar;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\scratch\\issue-73\\target\\dependency\\asm-analysis-9.7.jar",
  "-d",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\scratch\\issue-73\\final-current\\target\\as-record",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\tools\\as-record\\src\\main\\java\\local\\lito381\\asrecord\\AsRecord.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\tools\\as-record\\src\\main\\java\\local\\lito381\\asrecord\\ScanRecordFields.java"
]
```

```text

```

### asm-final（exit 0）

```json
[
  "C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\java.exe",
  "-Dfile.encoding=UTF-8",
  "-cp",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\scratch\\issue-73\\final-current\\target\\as-record;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm\\9.7\\asm-9.7.jar;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm-tree\\9.7\\asm-tree-9.7.jar;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\scratch\\issue-73\\target\\dependency\\asm-analysis-9.7.jar",
  "local.lito381.asrecord.AsRecord",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\scratch\\issue-73\\final-current\\target\\classes"
]
```

```text
as-record: scanned=104 converted=1
```

### tool-javac（exit 0）

```json
[
  "C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\javac.exe",
  "-J-Dfile.encoding=UTF-8",
  "--release",
  "17",
  "-encoding",
  "UTF-8",
  "-cp",
  "D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm\\9.7\\asm-9.7.jar;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm-tree\\9.7\\asm-tree-9.7.jar;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\scratch\\issue-73\\target\\dependency\\asm-analysis-9.7.jar",
  "-d",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\scratch\\issue-73\\final-current\\target\\tool",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\BlockExpressions.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\ClassComparer.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\FourthNormalizations.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\JarIndex.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\LayoutComparison.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\LocalTransport.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\LocalWebs.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\NonNullChecks.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\Normalizations.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\PathComparison.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\RethrowHandlers.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\SemanticDiff.java",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\TransportComparison.java"
]
```

```text

```

### semantic（exit 0）

```json
[
  "C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\java.exe",
  "-Dfile.encoding=UTF-8",
  "-cp",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\scratch\\issue-73\\final-current\\target\\tool;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm\\9.7\\asm-9.7.jar;D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm-tree\\9.7\\asm-tree-9.7.jar;D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\scratch\\issue-73\\target\\dependency\\asm-analysis-9.7.jar",
  "local.lito381.semanticdiff.SemanticDiff",
  "--left",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\scratch\\issue-73\\final-current\\target\\a02.jar",
  "--right",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\reference\\base-0.6.71.jar",
  "--left-classpath",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\lito伺服器\\LiTo381-Server\\reference\\base-0.6.71.jar",
  "--classes",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\scratch\\issue-73\\final-current\\verify_entries.txt",
  "--access-widen",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\scratch\\issue-73\\final-current\\access-widen-approved.txt",
  "--json",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\scratch\\issue-73\\final-current\\semantic.json",
  "--md",
  "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-73\\scratch\\issue-73\\final-current\\semantic.md"
]
```

```text
SUMMARY SAME=86 SAME_EFFECTS=0 SAME_PATHS=16 ORPHAN_IGNORED=0 SAME_LAYOUT=0 DEVIATION=0 DIFF=2 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=9 ALLOW_METHODS=0
wrote D:\lineage開發\3-lito開發\scratch\wt\task-73\scratch\issue-73\final-current\semantic.json
wrote D:\lineage開發\3-lito開發\scratch\wt\task-73\scratch\issue-73\final-current\semantic.md
```

### 集合與轉換稽核

```json
{
  "missingLeft": [],
  "extraLeft": []
}
```

```text
AsmFinal: exactly six ACC_FINAL flags plus annotation removal; all other ASM tree content identical
ConstructionState compiler bridges: 176; each only loads arguments, invokes same superclass method once, returns
```

### 編譯反證：NativeDeNpcConstructionState

```text
D:\lineage開發\3-lito開發\scratch\wt\task-73\scratch\issue-73\blockers\NativeDeNpcConstructionState.java:465: error: cancelNpcChatTimer() in NativeDeNpcConstructionState cannot override cancelNpcChatTimer() in NativeDeDeletionState
    private void cancelNpcChatTimer() {
                 ^
  attempting to assign weaker access privileges; was package
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
1 error
```

### 編譯反證：NativeDeItemWeaponProcTable

```text
D:\lineage開發\3-lito開發\scratch\wt\task-73\scratch\issue-73\blockers\NativeDeItemWeaponProcTable.java:65: error: cannot find symbol
        _instance = new NativeDeItemWeaponProcTable();
        ^
  symbol:   variable _instance
  location: class NativeDeItemWeaponProcTable
D:\lineage開發\3-lito開發\scratch\wt\task-73\scratch\issue-73\blockers\NativeDeItemWeaponProcTable.java:66: error: cannot find symbol
        _instance.load();
        ^
  symbol:   variable _instance
  location: class NativeDeItemWeaponProcTable
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
2 errors
```

反證程式（只修改 scratch 複本、原 staged 不變）：

```python
from pathlib import Path
import json,subprocess
r=Path.cwd();w=r/'scratch/issue-73';s=r/'lito伺服器/LiTo381-Server';st=s/'merge-inputs/formal-0.6.71/staged/local/lito381/server';work=w/'blockers';work.mkdir(exist_ok=True)
steps=[]
base=json.loads((w/'table2/commands.json').read_text(encoding='utf-8'))
original=next(x['command'] for x in base if x['name']=='a02-javac')
for name,old,new in [('NativeDeNpcConstructionState','    void cancelNpcChatTimer()', '    private void cancelNpcChatTimer()'),('NativeDeItemWeaponProcTable','    private static NativeDeItemWeaponProcTable _instance;','')]:
 t=(st/(name+'.java')).read_text(encoding='utf-8');assert old in t,(name,old)
 p=work/(name+'.java');p.write_text(t.replace(old,new),encoding='utf-8')
 target=work/'target'/name;target.mkdir(parents=True,exist_ok=True)
 cmd=original.copy();cmd[cmd.index('-d')+1]=str(target)
 cmd[cmd.index(str(st/(name+'.java')))]=str(p)
 result=subprocess.run(cmd,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 (work/(name+'.log')).write_bytes(result.stdout)
 steps.append(dict(name=name,command=cmd,exit=result.returncode))
 print(name,result.returncode,result.stdout.decode('utf-8',errors='replace'))
 assert result.returncode==1
(work/'commands.json').write_text(json.dumps(steps,ensure_ascii=False,indent=2),encoding='utf-8')

```

## 逐 entry 結果

| Entry（省略 local/lito381/server/） | 狀態 |
|---|---|
| NativeDeItemGreaterTierTable.class | SAME_PATHS |
| NativeDeItemGreaterTierTable$TierChance.class | SAME |
| NativeDeItemIdStatsTable.class | SAME |
| NativeDeItemInlayCardTable.class | SAME |
| NativeDeItemInventory.class | SAME_PATHS |
| NativeDeItemInventory$BoundItem.class | SAME_PATHS |
| NativeDeItemInventory$EntryState.class | SAME |
| NativeDeItemMagicStoneTable.class | SAME |
| NativeDeItemPublication.class | SAME_PATHS |
| NativeDeItemPublication$Ticket.class | SAME |
| NativeDeItemStateCopy.class | SAME |
| NativeDeItemTable.class | SAME_PATHS |
| NativeDeItemTemplatesResources.class | SAME |
| NativeDeItemTemplatesResources$Owner.class | SAME |
| NativeDeItemTemplatesResources$Snapshot.class | SAME |
| NativeDeItemTemplatesScope.class | SAME |
| NativeDeItemTemplatesScope$Binding.class | SAME |
| NativeDeItemTemplatesScope$Classes.class | SAME |
| NativeDeItemTemplatesScope$Context.class | SAME |
| NativeDeItemTemplatesScope$Data.class | SAME |
| NativeDeItemTemplatesScope$Database.class | SAME |
| NativeDeItemTemplatesScope$Logs.class | SAME |
| NativeDeItemTransaction.class | SAME |
| NativeDeItemTransaction$Change.class | SAME |
| NativeDeItemTransaction$Operation.class | SAME |
| NativeDeItemWeaponAttrTable.class | SAME |
| NativeDeItemWeaponProc.class | SAME |
| NativeDeItemWeaponProcTable.class | DIFF |
| NativeDeKnownState.class | SAME |
| NativeDeLiveAiJobs.class | SAME_PATHS |
| NativeDeLiveAiJobs$Binding.class | SAME |
| NativeDeLiveAiJobs$Credit.class | SAME |
| NativeDeLiveAiJobs$Owner.class | SAME |
| NativeDeLiveAiJobs$PulseCredit.class | SAME |
| NativeDeLiveAiJobs$Snapshot.class | SAME |
| NativeDeLiveAiJobs$Ticket.class | SAME |
| NativeDeLiveAiPorts.class | SAME |
| NativeDeLiveAiPorts$Combat.class | SAME |
| NativeDeLiveAiPorts$Config.class | SAME |
| NativeDeLiveAiPorts$Doll.class | SAME |
| NativeDeLiveAiPorts$Environment.class | SAME |
| NativeDeLiveAiPorts$Fishing.class | SAME |
| NativeDeLiveAiScope.class | SAME |
| NativeDeLiveAiScope$Clock.class | SAME |
| NativeDeLiveAiScope$Manager.class | SAME |
| NativeDeLiveAiScope$Pool.class | SAME |
| NativeDeLiveAiScope$Pool$ShutdownDiscardable.class | SAME |
| NativeDeLiveAiScope$Sprites.class | SAME |
| NativeDeLiveAiScope$World.class | SAME |
| NativeDeNpcConstructionState.class | DIFF |
| NativeDeNpcExecutor.class | SAME |
| NativeDeNpcHateList.class | SAME_PATHS |
| NativeDeNpcInventory.class | SAME_PATHS |
| NativeDeNpcKnowledge.class | SAME_PATHS |
| NativeDeNpcMobSkill.class | SAME |
| NativeDeNpcMobSkillUse.class | SAME_PATHS |
| NativeDeNpcMoveState.class | SAME |
| NativeDeNpcMoveState$PathFailure.class | SAME |
| NativeDeNpcMoveState$PathState.class | SAME |
| NativeDeNpcTemplate.class | SAME |
| NativeDeNpcTemplateLoadScope.class | SAME |
| NativeDeNpcTemplateLoadScope$Context.class | SAME |
| NativeDeNpcTemplateLoadScope$Database.class | SAME |
| NativeDeNpcTemplateLoadScope$Npcs.class | SAME |
| NativeDeNpcTemplateLoadScope$Resolver.class | SAME |
| NativeDeNpcTemplateLoadScope$Skill.class | SAME |
| NativeDeNpcTemplateLoadScope$Skills.class | SAME |
| NativeDeNpcTemplateLoadScope$Sql.class | SAME |
| NativeDeNpcTemplateLoadScope$Timer.class | SAME |
| NativeDeNpcTemplateMobLoader.class | SAME_PATHS |
| NativeDeNpcTemplateNpcLoader.class | SAME |
| NativeDeNpcTemplateResources.class | SAME |
| NativeDeNpcTemplateResources$Counts.class | SAME |
| NativeDeNpcTemplateResources$Snapshot.class | SAME |
| NativeDeNpcTemplateState.class | SAME_PATHS |
| NativeDeNpcVitalState.class | SAME_PATHS |
| NativeDeNpcVitalState$1.class | SAME |
| NativeDeObjectBindings.class | SAME |
| NativeDeObjectFields.class | SAME |
| NativeDeObjectScope.class | SAME |
| NativeDeObjectScope$Maps.class | SAME |
| NativeDeObjectScope$World.class | SAME |
| NativeDePerceptionAccess.class | SAME |
| NativeDePerceptionBadText.class | SAME |
| NativeDePerceptionControls.class | SAME |
| NativeDePerceptionControls$Data.class | SAME |
| NativeDePerceptionEmblem.class | SAME |
| NativeDePerceptionJobs.class | SAME |
| NativeDePerceptionJobs$Key.class | SAME |
| NativeDePerceptionJobs$Owner.class | SAME |
| NativeDePerceptionJobs$Snapshot.class | SAME |
| NativeDePerceptionJobs$Task.class | SAME |
| NativeDePerceptionNPCPack_De.class | SAME |
| NativeDePerceptionOutput.class | SAME |
| NativeDePerceptionPacket.class | SAME_PATHS |
| NativeDePerceptionScope.class | SAME |
| NativeDePerceptionSpeed.class | SAME |
| NativeDePerceptionSpeed$ActionType.class | SAME |
| NativeDePerceptionSpeed$SpeedState.class | SAME |
| NativeDePerceptionState.class | SAME_PATHS |
| NativeDeProfileKernel.class | SAME_PATHS |
| NativeDeProfileKernel$Combat.class | SAME |
| NativeDeProfileKernel$Target.class | SAME |
| NativeDeShopData.class | SAME |

## 完整剩餘正規化差異

下列只讀輸出器呼叫同版 ClassComparer／JarIndex，輸出全部方法與欄位。類別 access 的原始數字也保留，因此 ConstructionState 的 ACC_SUPER 顯示於 dump，但正式比較器已忽略該旗標；不能把該行再次列成待裁定。未縮短方法內容或只截取 firstDiff。沒有新增 B 類 effects 條目。

### NativeDeItemWeaponProcTable

```diff
--- staged/NativeDeItemWeaponProcTable.txt
+++ formal/NativeDeItemWeaponProcTable.txt
@@ -11,11 +11,6 @@
 annotations=|||
 innerClass=
 recordComponents=
-field=_instance:Llocal/lito381/server/NativeDeItemWeaponProcTable;
-access=0xa
-signature=
-value=
-annotations=|||
 field=_log:Llocal/lito381/server/NativeDeWorldLog;
 access=0x1a
 signature=
@@ -381,7 +376,7 @@
 NEW local/lito381/server/NativeDeItemWeaponProcTable
 DUP
 INVOKESPECIAL local/lito381/server/NativeDeItemWeaponProcTable.<init>()V
-PUTSTATIC local/lito381/server/NativeDeItemWeaponProcTable._instance:Llocal/lito381/server/NativeDeItemWeaponProcTable;
-GETSTATIC local/lito381/server/NativeDeItemWeaponProcTable._instance:Llocal/lito381/server/NativeDeItemWeaponProcTable;
+PUTSTATIC unresolved(local/lito381/server/NativeDeItemWeaponProcTable)._instance:Llocal/lito381/server/NativeDeItemWeaponProcTable;
+GETSTATIC unresolved(local/lito381/server/NativeDeItemWeaponProcTable)._instance:Llocal/lito381/server/NativeDeItemWeaponProcTable;
 INVOKEVIRTUAL local/lito381/server/NativeDeItemWeaponProcTable.load()V
 RETURN
```

```json
{
  "name": "local/lito381/server/NativeDeItemWeaponProcTable",
  "status": "DIFF",
  "byteSame": false,
  "allowMethodsApplied": false,
  "methodResults": [
    {
      "method": "reload()V",
      "status": "DIFF",
      "detail": "method reload()V insn[6]: left=PUTSTATIC local/lito381/server/NativeDeItemWeaponProcTable._instance:Llocal/lito381/server/NativeDeItemWeaponProcTable; right=PUTSTATIC unresolved(local/lito381/server/NativeDeItemWeaponProcTable)._instance:Llocal/lito381/server/NativeDeItemWeaponProcTable;; paths rejected: unresolved field volatility: local/lito381/server/NativeDeItemWeaponProcTable._instance"
    }
  ],
  "effectsMethods": [],
  "approvedMethods": [],
  "firstDiff": {
    "where": "field",
    "member": "_instance:Llocal/lito381/server/NativeDeItemWeaponProcTable;",
    "insnIndex": null,
    "detail": "only left"
  }
}
```

### NativeDeNpcConstructionState

```diff
--- staged/NativeDeNpcConstructionState.txt
+++ formal/NativeDeNpcConstructionState.txt
@@ -1,5 +1,5 @@
 class=local/lito381/server/NativeDeNpcConstructionState
-access=21
+access=1
 super=local/lito381/server/NativeDeNpcTemplateState
 interfaces=
 signature=
@@ -330,285 +330,6 @@
 ALOAD 7
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcConstructionState.setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V
 RETURN
-method=addAc(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addAc(I)V
-RETURN
-method=addBowDmgup(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addBowDmgup(I)V
-RETURN
-method=addBowHitup(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addBowHitup(I)V
-RETURN
-method=addCha(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addCha(I)V
-RETURN
-method=addCon(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addCon(I)V
-RETURN
-method=addDex(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addDex(I)V
-RETURN
-method=addDmgup(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addDmgup(I)V
-RETURN
-method=addEarth(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addEarth(I)V
-RETURN
-method=addFire(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addFire(I)V
-RETURN
-method=addHitup(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addHitup(I)V
-RETURN
-method=addInt(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addInt(I)V
-RETURN
-method=addKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
-RETURN
-method=addMaxHp(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addMaxHp(I)V
-RETURN
-method=addMaxMp(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addMaxMp(I)V
-RETURN
-method=addMr(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addMr(I)V
-RETURN
-method=addRegistBlind(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addRegistBlind(I)V
-RETURN
-method=addRegistSleep(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addRegistSleep(I)V
-RETURN
-method=addRegistStone(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addRegistStone(I)V
-RETURN
-method=addRegistStun(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addRegistStun(I)V
-RETURN
-method=addRegistSustain(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addRegistSustain(I)V
-RETURN
-method=addSp(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addSp(I)V
-RETURN
-method=addStr(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addStr(I)V
-RETURN
-method=addStunSuccess(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addStunSuccess(I)V
-RETURN
-method=addWater(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addWater(I)V
-RETURN
-method=addWeaponBreakStack()V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addWeaponBreakStack()V
-RETURN
-method=addWind(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addWind(I)V
-RETURN
-method=addWis(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.addWis(I)V
-RETURN
-method=add_regist_freeze(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.add_regist_freeze(I)V
-RETURN
 method=allTargetClear()V
 access=0x1
 signature=
@@ -660,16 +381,6 @@
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
 ALOAD 1
 ATHROW
-RETURN
-method=broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
 RETURN
 method=calcSleepTime(II)I
 access=0x1
@@ -697,7 +408,7 @@
 INVOKESTATIC local/lito381/server/NativeDePerceptionSpeed.calculate(Llocal/lito381/server/NativeDePerceptionState;ILlocal/lito381/server/NativeDePerceptionSpeed$ActionType;)I
 IRETURN
 method=cancelNpcChatTimer()V
-access=0x0
+access=0x2
 signature=
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
@@ -705,16 +416,6 @@
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeDeChatRuntime.cancel(Llocal/lito381/server/NativeDeNpcConstructionState;)V
 RETURN
-method=claimExpiredCorpseDeletion(J)Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-LLOAD 1/2
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.claimExpiredCorpseDeletion(J)Z
-IRETURN
 method=commitManaDamage(Llocal/lito381/server/NativeDeCharacterFields;I)I
 access=0x1
 signature=
@@ -745,223 +446,6 @@
 ALOAD 1
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._pendingFamilyTarget:Llocal/lito381/server/NativeDeCharacterFields;
 RETURN
-method=deleteMe()V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.deleteMe()V
-RETURN
-method=destroyed()Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.destroyed()Z
-IRETURN
-method=forward(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.forward(I)V
-RETURN
-method=getAc()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getAc()I
-IRETURN
-method=getAddAttrKind()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getAddAttrKind()I
-IRETURN
-method=getAtkspeed()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getAtkspeed()I
-IRETURN
-method=getBowDmgup()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getBowDmgup()I
-IRETURN
-method=getBowHitup()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getBowHitup()I
-IRETURN
-method=getBraveSpeed()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getBraveSpeed()I
-IRETURN
-method=getCha()S
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getCha()S
-IRETURN
-method=getChaLightSize()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getChaLightSize()I
-IRETURN
-method=getClanid()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getClanid()I
-IRETURN
-method=getClanname()Ljava/lang/String;
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getClanname()Ljava/lang/String;
-ARETURN
-method=getCon()S
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getCon()S
-IRETURN
-method=getCurrentHp()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getCurrentHp()I
-IRETURN
-method=getCurrentMp()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getCurrentMp()I
-IRETURN
-method=getDex()S
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getDex()S
-IRETURN
-method=getDmgup()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getDmgup()I
-IRETURN
-method=getDolls()Ljava/util/Map;
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getDolls()Ljava/util/Map;
-ARETURN
-method=getEarth()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getEarth()I
-IRETURN
-method=getExp()J
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getExp()J
-LRETURN
-method=getFire()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getFire()I
-IRETURN
-method=getGfxId()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getGfxId()I
-IRETURN
-method=getHeading()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getHeading()I
-IRETURN
 method=getHiddenStatus()I
 access=0x1
 signature=
@@ -971,15 +455,6 @@
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._hiddenStatus:I
 IRETURN
-method=getHitup()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getHitup()I
-IRETURN
 method=getHomeX()I
 access=0x1
 signature=
@@ -997,24 +472,6 @@
 annotationDefault=
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._homeY:I
-IRETURN
-method=getId()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getId()I
-IRETURN
-method=getInt()S
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getInt()S
 IRETURN
 method=getInventory()Llocal/lito381/server/NativeDeDeletionPorts$Inventory;
 access=0x41
@@ -1035,33 +492,6 @@
 GETFIELD local/lito381/server/NativeDeDeletionState.inventory:Llocal/lito381/server/NativeDeDeletionPorts$Inventory;
 CHECKCAST local/lito381/server/NativeDeNpcInventory
 ARETURN
-method=getKarma()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getKarma()I
-IRETURN
-method=getKnownObjects()Ljava/util/List;
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getKnownObjects()Ljava/util/List;
-ARETURN
-method=getKnownPlayers()Ljava/util/List;
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getKnownPlayers()Ljava/util/List;
-ARETURN
 method=getLastMoveTimeMs()J
 access=0x1
 signature=
@@ -1071,79 +501,6 @@
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._lastMoveTimeMs:J
 LRETURN
-method=getLawful()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getLawful()I
-IRETURN
-method=getLevel()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getLevel()I
-IRETURN
-method=getLightSize()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getLightSize()I
-IRETURN
-method=getLineDistance(Llocal/lito381/server/NativeDeObjectFields;)D
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getLineDistance(Llocal/lito381/server/NativeDeObjectFields;)D
-DRETURN
-method=getLocation()Llocal/lito381/server/NativeDeWorldLocation;
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getLocation()Llocal/lito381/server/NativeDeWorldLocation;
-ARETURN
-method=getMagicBonus()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getMagicBonus()I
-IRETURN
-method=getMagicLevel()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getMagicLevel()I
-IRETURN
-method=getMap()Llocal/lito381/server/NativeDeWorldMap;
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getMap()Llocal/lito381/server/NativeDeWorldMap;
-ARETURN
 method=getMapId()S
 access=0x1
 signature=
@@ -1152,24 +509,6 @@
 annotationDefault=
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeDeObjectBindings.mapId(Llocal/lito381/server/NativeDeObjectFields;)S
-IRETURN
-method=getMaxHp()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getMaxHp()I
-IRETURN
-method=getMaxMp()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getMaxMp()I
 IRETURN
 method=getMobGroupId()I
 access=0x1
@@ -1189,15 +528,6 @@
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._npcMove:Llocal/lito381/server/NativeDeNpcMoveExecutor;
 ARETURN
-method=getMoveSpeed()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getMoveSpeed()I
-IRETURN
 method=getMovementDistance()I
 access=0x1
 signature=
@@ -1207,33 +537,6 @@
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._movementDistance:I
 IRETURN
-method=getMr()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getMr()I
-IRETURN
-method=getName()Ljava/lang/String;
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getName()Ljava/lang/String;
-ARETURN
-method=getNameId()Ljava/lang/String;
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getNameId()Ljava/lang/String;
-ARETURN
 method=getNpcId()I
 access=0x1
 signature=
@@ -1245,42 +548,6 @@
 CHECKCAST local/lito381/server/NativeDeNpcTemplate
 INVOKEVIRTUAL local/lito381/server/NativeDeNpcTemplate.get_npcId()I
 IRETURN
-method=getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;
-ARETURN
-method=getOwnLightSize()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getOwnLightSize()I
-IRETURN
-method=getPassispeed()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getPassispeed()I
-IRETURN
-method=getPetList()Ljava/util/Map;
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getPetList()Ljava/util/Map;
-ARETURN
 method=getPetcost()I
 access=0x1
 signature=
@@ -1290,15 +557,6 @@
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._petcost:I
 IRETURN
-method=getPoison()Llocal/lito381/server/NativeDePerceptionPorts$Poison;
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getPoison()Llocal/lito381/server/NativeDePerceptionPorts$Poison;
-ARETURN
 method=getPrevX()I
 access=0x1
 signature=
@@ -1316,60 +574,6 @@
 annotationDefault=
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._prevY:I
-IRETURN
-method=getRegistBlind()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getRegistBlind()I
-IRETURN
-method=getRegistFreeze()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getRegistFreeze()I
-IRETURN
-method=getRegistSleep()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getRegistSleep()I
-IRETURN
-method=getRegistStone()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getRegistStone()I
-IRETURN
-method=getRegistStun()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getRegistStun()I
-IRETURN
-method=getRegistSustain()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getRegistSustain()I
 IRETURN
 method=getSleepTime()I
 access=0x4
@@ -1380,143 +584,6 @@
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._sleep_time:I
 IRETURN
-method=getSp()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getSp()I
-IRETURN
-method=getStatus()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getStatus()I
-IRETURN
-method=getStr()S
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getStr()S
-IRETURN
-method=getStunSuccess()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getStunSuccess()I
-IRETURN
-method=getTempCharGfx()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getTempCharGfx()I
-IRETURN
-method=getTempLawful()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getTempLawful()I
-IRETURN
-method=getTileDistance(Llocal/lito381/server/NativeDeObjectFields;)I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getTileDistance(Llocal/lito381/server/NativeDeObjectFields;)I
-IRETURN
-method=getTileLineDistance(Llocal/lito381/server/NativeDeObjectFields;)I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getTileLineDistance(Llocal/lito381/server/NativeDeObjectFields;)I
-IRETURN
-method=getTitle()Ljava/lang/String;
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getTitle()Ljava/lang/String;
-ARETURN
-method=getTrueMr()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getTrueMr()I
-IRETURN
-method=getTrueSp()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getTrueSp()I
-IRETURN
-method=getWater()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getWater()I
-IRETURN
-method=getWeaponBreakStack()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getWeaponBreakStack()I
-IRETURN
-method=getWind()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getWind()I
-IRETURN
-method=getWis()S
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.getWis()S
-IRETURN
 method=getX()I
 access=0x1
 signature=
@@ -1535,33 +602,6 @@
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeDeObjectBindings.y(Llocal/lito381/server/NativeDeObjectFields;)I
 IRETURN
-method=get_deadTimerTemp()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.get_deadTimerTemp()I
-IRETURN
-method=get_fishX()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.get_fishX()I
-IRETURN
-method=get_fishY()I
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.get_fishY()I
-IRETURN
 method=get_stop_time()I
 access=0x1
 signature=
@@ -1579,16 +619,6 @@
 annotationDefault=
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._work_time:I
-IRETURN
-method=hasSkillEffect(I)Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.hasSkillEffect(I)Z
 IRETURN
 method=isActived()Z
 access=0x4
@@ -1598,33 +628,6 @@
 annotationDefault=
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._actived:Z
-IRETURN
-method=isAgro()Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.isAgro()Z
-IRETURN
-method=isAgrocoi()Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.isAgrocoi()Z
-IRETURN
-method=isAgrososc()Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.isAgrososc()Z
 IRETURN
 method=isAiOwner(J)Z
 access=0x14
@@ -1646,24 +649,6 @@
 GOTO L13
 CONST Integer:0
 IRETURN
-method=isAiRunning()Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.isAiRunning()Z
-IRETURN
-method=isDead()Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.isDead()Z
-IRETURN
 method=isDeathProcessing()Z
 access=0x4
 signature=
@@ -1682,52 +667,6 @@
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._firstAttack:Z
 IRETURN
-method=isFishing()Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.isFishing()Z
-IRETURN
-method=isInvisble()Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.isInvisble()Z
-IRETURN
-method=isKnownNpcViewerConsistent(Llocal/lito381/server/NativeDeDeletionState;)Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.isKnownNpcViewerConsistent(Llocal/lito381/server/NativeDeDeletionState;)Z
-IRETURN
-method=isParalyzed()Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.isParalyzed()Z
-IRETURN
-method=isPickupItem()Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.isPickupItem()Z
-IRETURN
 method=isRest()Z
 access=0x1
 signature=
@@ -1745,24 +684,6 @@
 annotationDefault=
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._isResurrect:Z
-IRETURN
-method=isShop()Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.isShop()Z
-IRETURN
-method=isSleeped()Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.isSleeped()Z
 IRETURN
 method=is_now_target()Llocal/lito381/server/NativeDeCharacterFields;
 access=0x1
@@ -1790,16 +711,6 @@
 annotationDefault=
 ALOAD 0
 GETFIELD local/lito381/server/NativeDeNpcConstructionState._isremovearmor:Z
-IRETURN
-method=knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
 IRETURN
 method=lambda$content$c61af697d645d0700aa30c1b735fac9f1cf547368a2112074bdbf7c5c15bb823(Z)V
 access=0x2
@@ -1821,16 +732,6 @@
 ASTORE 2
 ALOAD 2
 ARETURN
-method=onAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.onAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
-RETURN
 method=onDeleteClaimed()V
 access=0x4
 signature=
@@ -1858,36 +759,6 @@
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault=
-RETURN
-method=onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
-RETURN
-method=onTalkAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.onTalkAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V
-RETURN
-method=rebindWorldMap(S)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.rebindWorldMap(S)V
 RETURN
 method=receiveDamage(Llocal/lito381/server/NativeDeCharacterFields;I)V
 access=0x1
@@ -1916,35 +787,6 @@
 GOTO L13
 CONST Integer:0
 IRETURN
-method=removeAllKnownObjects()V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.removeAllKnownObjects()V
-RETURN
-method=removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V
-RETURN
-method=removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
-IRETURN
 method=resetPathFailureState()V
 access=0x14
 signature=
@@ -1958,35 +800,25 @@
 CONST null
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._pendingFamilyTarget:Llocal/lito381/server/NativeDeCharacterFields;
 RETURN
-method=resurrect(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.resurrect(I)V
-RETURN
 method=scheduleNpcChatTimer(Llocal/lito381/server/NativeDeChatTimer;JJ)Ljava/util/concurrent/ScheduledFuture;
 access=0x2
 signature=(Llocal/lito381/server/NativeDeChatTimer;JJ)Ljava/util/concurrent/ScheduledFuture<*>;
 exceptions=
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault=
-8:[46:java/util/concurrent/RejectedExecutionException  , 62:java/lang/RuntimeException  ]
-10:[46:java/util/concurrent/RejectedExecutionException  , 62:java/lang/RuntimeException  ]
-17:[46:java/util/concurrent/RejectedExecutionException  , 62:java/lang/RuntimeException  ]
-20:[46:java/util/concurrent/RejectedExecutionException  , 62:java/lang/RuntimeException  ]
-22:[46:java/util/concurrent/RejectedExecutionException  , 62:java/lang/RuntimeException  ]
-26:[46:java/util/concurrent/RejectedExecutionException  , 62:java/lang/RuntimeException  ]
-33:[46:java/util/concurrent/RejectedExecutionException  , 62:java/lang/RuntimeException  ]
-34:[46:java/util/concurrent/RejectedExecutionException  , 62:java/lang/RuntimeException  ]
-36:[46:java/util/concurrent/RejectedExecutionException  , 62:java/lang/RuntimeException  ]
-39:[46:java/util/concurrent/RejectedExecutionException  , 62:java/lang/RuntimeException  ]
-41:[46:java/util/concurrent/RejectedExecutionException  , 62:java/lang/RuntimeException  ]
-43:[46:java/util/concurrent/RejectedExecutionException  , 62:java/lang/RuntimeException  ]
-44:[46:java/util/concurrent/RejectedExecutionException  , 62:java/lang/RuntimeException  ]
+8:[47:java/util/concurrent/RejectedExecutionException  , 63:java/lang/RuntimeException  ]
+10:[47:java/util/concurrent/RejectedExecutionException  , 63:java/lang/RuntimeException  ]
+17:[47:java/util/concurrent/RejectedExecutionException  , 63:java/lang/RuntimeException  ]
+20:[47:java/util/concurrent/RejectedExecutionException  , 63:java/lang/RuntimeException  ]
+23:[47:java/util/concurrent/RejectedExecutionException  , 63:java/lang/RuntimeException  ]
+27:[47:java/util/concurrent/RejectedExecutionException  , 63:java/lang/RuntimeException  ]
+34:[47:java/util/concurrent/RejectedExecutionException  , 63:java/lang/RuntimeException  ]
+35:[47:java/util/concurrent/RejectedExecutionException  , 63:java/lang/RuntimeException  ]
+37:[47:java/util/concurrent/RejectedExecutionException  , 63:java/lang/RuntimeException  ]
+40:[47:java/util/concurrent/RejectedExecutionException  , 63:java/lang/RuntimeException  ]
+42:[47:java/util/concurrent/RejectedExecutionException  , 63:java/lang/RuntimeException  ]
+44:[47:java/util/concurrent/RejectedExecutionException  , 63:java/lang/RuntimeException  ]
+45:[47:java/util/concurrent/RejectedExecutionException  , 63:java/lang/RuntimeException  ]
 LLOAD 4/5
 CONST Long:0
 LCMP
@@ -2003,12 +835,13 @@
 LLOAD 4/5
 CONST Long:0
 LCMP
-IFGT L22
+IFGT L23
 INVOKESTATIC local/lito381/server/NativeDeChatScope$Clock.get()Llocal/lito381/server/NativeDeChatScope$Clock;
 ALOAD 1
 LLOAD 2/3
 INVOKEVIRTUAL local/lito381/server/NativeDeChatScope$Clock.aiSchedule(Ljava/lang/Runnable;J)Ljava/util/concurrent/ScheduledFuture;
-GOTO L27
+ASTORE 6
+GOTO L29
 INVOKESTATIC local/lito381/server/NativeDeChatScope$Clock.get()Llocal/lito381/server/NativeDeChatScope$Clock;
 ALOAD 1
 LLOAD 2/3
@@ -2016,7 +849,7 @@
 INVOKEVIRTUAL local/lito381/server/NativeDeChatScope$Clock.aiScheduleAtFixedRate(Ljava/lang/Runnable;JJ)Ljava/util/concurrent/ScheduledFuture;
 ASTORE 6
 ALOAD 6
-IFNULL L32
+IFNULL L33
 ALOAD 6
 ARETURN
 CONST str:"com/lineage/server/model/Instance/L1NpcInstance.NPC_CHAT_SCHEDULE_ERROR_COUNT"
@@ -2032,7 +865,7 @@
 LLOAD 4/5
 INVOKESTATIC java/lang/Long.valueOf(J)Ljava/lang/Long;
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
-GOTO L77
+GOTO L78
 ASTORE 7
 CONST str:"com/lineage/server/model/Instance/L1NpcInstance.NPC_CHAT_SCHEDULE_REJECTED_COUNT"
 INVOKESTATIC local/lito381/server/NativeDeChatScope.counter(Ljava/lang/String;)Ljava/util/concurrent/atomic/AtomicLong;
@@ -2048,7 +881,7 @@
 INVOKESTATIC java/lang/Long.valueOf(J)Ljava/lang/Long;
 ALOAD 7
 INVOKEINTERFACE local/lito381/server/NativeDeWorldLog.warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V itf
-GOTO L77
+GOTO L78
 ASTORE 8
 CONST str:"com/lineage/server/model/Instance/L1NpcInstance.NPC_CHAT_SCHEDULE_ERROR_COUNT"
 INVOKESTATIC local/lito381/server/NativeDeChatScope.counter(Ljava/lang/String;)Ljava/util/concurrent/atomic/AtomicLong;
@@ -2073,16 +906,6 @@
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault=
 RETURN
-method=setAc(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setAc(I)V
-RETURN
 method=setActived(Z)V
 access=0x4
 signature=
@@ -2093,56 +916,6 @@
 ILOAD 1
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._actived:Z
 RETURN
-method=setAddAttrKind(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setAddAttrKind(I)V
-RETURN
-method=setAgro(Z)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setAgro(Z)V
-RETURN
-method=setAgrocoi(Z)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setAgrocoi(Z)V
-RETURN
-method=setAgrososc(Z)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setAgrososc(Z)V
-RETURN
-method=setAtkspeed(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setAtkspeed(I)V
-RETURN
 method=setBowActId(I)V
 access=0x1
 signature=
@@ -2152,76 +925,6 @@
 ALOAD 0
 ILOAD 1
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._bowActId:I
-RETURN
-method=setBraveSpeed(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setBraveSpeed(I)V
-RETURN
-method=setCha(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setCha(I)V
-RETURN
-method=setChaLightSize(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setChaLightSize(I)V
-RETURN
-method=setCon(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setCon(I)V
-RETURN
-method=setDead(Z)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setDead(Z)V
-RETURN
-method=setDex(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setDex(I)V
-RETURN
-method=setExp(J)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-LLOAD 1/2
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setExp(J)V
 RETURN
 method=setFirstAttack(Z)V
 access=0x4
@@ -2233,36 +936,6 @@
 ILOAD 1
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._firstAttack:Z
 RETURN
-method=setGfxId(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setGfxId(I)V
-RETURN
-method=setGfxidInStatus(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setGfxidInStatus(I)V
-RETURN
-method=setHeading(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setHeading(I)V
-RETURN
 method=setHiddenStatus(I)V
 access=0x1
 signature=
@@ -2292,26 +965,6 @@
 ALOAD 0
 ILOAD 1
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._homeY:I
-RETURN
-method=setId(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setId(I)V
-RETURN
-method=setInt(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setInt(I)V
 RETURN
 method=setInventory(Llocal/lito381/server/NativeDeNpcInventory;)V
 access=0x1
@@ -2323,46 +976,6 @@
 ALOAD 1
 PUTFIELD local/lito381/server/NativeDeDeletionState.inventory:Llocal/lito381/server/NativeDeDeletionPorts$Inventory;
 RETURN
-method=setKarma(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setKarma(I)V
-RETURN
-method=setLawful(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setLawful(I)V
-RETURN
-method=setLevel(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setLevel(I)V
-RETURN
-method=setLightSize(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setLightSize(I)V
-RETURN
 method=setLink(Llocal/lito381/server/NativeDeCharacterFields;)V
 access=0x1
 signature=
@@ -2370,68 +983,6 @@
 annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault=
 RETURN
-method=setLocation(III)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-ILOAD 2
-ILOAD 3
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setLocation(III)V
-RETURN
-method=setLocation(Llocal/lito381/server/NativeDeWorldLocation;)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setLocation(Llocal/lito381/server/NativeDeWorldLocation;)V
-RETURN
-method=setMap(Llocal/lito381/server/NativeDeWorldMap;)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setMap(Llocal/lito381/server/NativeDeWorldMap;)V
-RETURN
-method=setMap(S)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setMap(S)V
-RETURN
-method=setMaxHp(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setMaxHp(I)V
-RETURN
-method=setMaxMp(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setMaxMp(I)V
-RETURN
 method=setMobGroupId(I)V
 access=0x1
 signature=
@@ -2442,16 +993,6 @@
 ILOAD 1
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._mobGroupId:I
 RETURN
-method=setMoveSpeed(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setMoveSpeed(I)V
-RETURN
 method=setMovementDistance(I)V
 access=0x1
 signature=
@@ -2462,56 +1003,6 @@
 ILOAD 1
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._movementDistance:I
 RETURN
-method=setMr(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setMr(I)V
-RETURN
-method=setName(Ljava/lang/String;)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setName(Ljava/lang/String;)V
-RETURN
-method=setNameId(Ljava/lang/String;)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setNameId(Ljava/lang/String;)V
-RETURN
-method=setOwnLightSize(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setOwnLightSize(I)V
-RETURN
-method=setPassispeed(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setPassispeed(I)V
-RETURN
 method=setPetcost(I)V
 access=0x1
 signature=
@@ -2522,16 +1013,6 @@
 ILOAD 1
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._petcost:I
 RETURN
-method=setPickupItem(Z)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setPickupItem(Z)V
-RETURN
 method=setRest(Z)V
 access=0x1
 signature=
@@ -2561,117 +1042,6 @@
 ALOAD 0
 ILOAD 1
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._sleep_time:I
-RETURN
-method=setStatus(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setStatus(I)V
-RETURN
-method=setStr(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setStr(I)V
-RETURN
-method=setTempCharGfx(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setTempCharGfx(I)V
-RETURN
-method=setTempLawful(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setTempLawful(I)V
-RETURN
-method=setTitle(Ljava/lang/String;)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setTitle(Ljava/lang/String;)V
-RETURN
-method=setWeaponBreakStack(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setWeaponBreakStack(I)V
-RETURN
-method=setWis(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setWis(I)V
-RETURN
-method=setX(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setX(I)V
-RETURN
-method=setXY(II)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-ILOAD 2
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setXY(II)V
-RETURN
-method=setY(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setY(I)V
-RETURN
-method=set_deadTimerTemp(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.set_deadTimerTemp(I)V
 RETURN
 method=set_now_targetItem(Llocal/lito381/server/NativeDeWorldKinds$Item;)V
 access=0x1
@@ -2703,16 +1073,6 @@
 ILOAD 1
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._isremovearmor:Z
 RETURN
-method=set_showId(I)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ILOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.set_showId(I)V
-RETURN
 method=set_work_time(I)V
 access=0x1
 signature=
@@ -2732,16 +1092,6 @@
 ALOAD 0
 ILOAD 1
 PUTFIELD local/lito381/server/NativeDeDeletionState.respawn:Z
-RETURN
-method=setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V
 RETURN
 method=sourceChat$cancelNpcChatTimer()V
 access=0x2
@@ -2829,17 +1179,17 @@
 IFNONNULL L57
 RETURN
 ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeDeNpcConstructionState.cancelNpcChatTimer()V
+INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.cancelNpcChatTimer()V
 NEW local/lito381/server/NativeDeChatTimer
 DUP
 ALOAD 0
 ALOAD 3
 INVOKESPECIAL local/lito381/server/NativeDeChatTimer.<init>(Llocal/lito381/server/NativeDeNpcConstructionState;Llocal/lito381/server/NativeDeChatData;)V
 ASTORE 4
-ALOAD 0
 ALOAD 3
 INVOKEVIRTUAL local/lito381/server/NativeDeChatData.isRepeat()Z
-IFNE L77
+IFNE L78
+ALOAD 0
 ALOAD 0
 ALOAD 4
 ALOAD 3
@@ -2847,7 +1197,9 @@
 I2L
 CONST Long:0
 INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.scheduleNpcChatTimer(Llocal/lito381/server/NativeDeChatTimer;JJ)Ljava/util/concurrent/ScheduledFuture;
-GOTO L86
+PUTFIELD local/lito381/server/NativeDeNpcConstructionState._monitorFuture:Ljava/util/concurrent/ScheduledFuture;
+GOTO L89
+ALOAD 0
 ALOAD 0
 ALOAD 4
 ALOAD 3
@@ -2859,15 +1211,6 @@
 INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.scheduleNpcChatTimer(Llocal/lito381/server/NativeDeChatTimer;JJ)Ljava/util/concurrent/ScheduledFuture;
 PUTFIELD local/lito381/server/NativeDeNpcConstructionState._monitorFuture:Ljava/util/concurrent/ScheduledFuture;
 RETURN
-method=sourceDeleteDe()V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.sourceDeleteDe()V
-RETURN
 method=startChat(I)V
 access=0x1
 signature=
@@ -2877,15 +1220,6 @@
 ALOAD 0
 ILOAD 1
 INVOKESTATIC local/lito381/server/NativeDeChatRuntime.start(Llocal/lito381/server/NativeDeNpcConstructionState;I)V
-RETURN
-method=start_shop()V
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.start_shop()V
 RETURN
 method=tryAcquireAiOwner()J
 access=0x14
@@ -2923,13 +1257,3 @@
 GOTO L29
 CONST Long:0
 LRETURN
-method=tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
-access=0x41
-signature=
-exceptions=
-annotations=||||pvis=|pinv=|lvtvis=|lvtinv=
-annotationDefault=
-ALOAD 0
-ALOAD 1
-INVOKESPECIAL local/lito381/server/NativeDeNpcTemplateState.tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z
-IRETURN
```

```json
{
  "name": "local/lito381/server/NativeDeNpcConstructionState",
  "status": "DIFF",
  "byteSame": false,
  "allowMethodsApplied": false,
  "methodResults": [
    {
      "method": "addAc(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addBowDmgup(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addBowHitup(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addCha(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addCon(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addDex(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addDmgup(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addEarth(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addFire(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addHitup(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addInt(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addMaxHp(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addMaxMp(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addMr(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addRegistBlind(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addRegistSleep(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addRegistStone(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addRegistStun(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addRegistSustain(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addSp(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addStr(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addStunSuccess(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addWater(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addWeaponBreakStack()V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addWind(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "addWis(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "add_regist_freeze(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "broadcastPacketHP(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "cancelNpcChatTimer()V",
      "status": "DIFF",
      "detail": "method cancelNpcChatTimer()V: left=0x0 right=0x2"
    },
    {
      "method": "claimExpiredCorpseDeletion(J)Z",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "deleteMe()V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "destroyed()Z",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "forward(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getAc()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getAddAttrKind()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getAtkspeed()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getBowDmgup()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getBowHitup()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getBraveSpeed()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getCha()S",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getChaLightSize()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getClanid()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getClanname()Ljava/lang/String;",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getCon()S",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getCurrentHp()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getCurrentMp()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getDex()S",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getDmgup()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getDolls()Ljava/util/Map;",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getEarth()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getExp()J",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getFire()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getGfxId()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getHeading()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getHitup()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getId()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getInt()S",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getKarma()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getKnownObjects()Ljava/util/List;",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getKnownPlayers()Ljava/util/List;",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getLawful()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getLevel()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getLightSize()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getLineDistance(Llocal/lito381/server/NativeDeObjectFields;)D",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getLocation()Llocal/lito381/server/NativeDeWorldLocation;",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getMagicBonus()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getMagicLevel()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getMap()Llocal/lito381/server/NativeDeWorldMap;",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getMaxHp()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getMaxMp()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getMoveSpeed()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getMr()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getName()Ljava/lang/String;",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getNameId()Ljava/lang/String;",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getNpcTemplate()Llocal/lito381/server/NativeDeNpcTemplate;",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getOwnLightSize()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getPassispeed()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getPetList()Ljava/util/Map;",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getPoison()Llocal/lito381/server/NativeDePerceptionPorts$Poison;",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getRegistBlind()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getRegistFreeze()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getRegistSleep()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getRegistStone()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getRegistStun()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getRegistSustain()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getSp()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getStatus()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getStr()S",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getStunSuccess()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getTempCharGfx()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getTempLawful()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getTileDistance(Llocal/lito381/server/NativeDeObjectFields;)I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getTileLineDistance(Llocal/lito381/server/NativeDeObjectFields;)I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getTitle()Ljava/lang/String;",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getTrueMr()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getTrueSp()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getWater()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getWeaponBreakStack()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getWind()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "getWis()S",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "get_deadTimerTemp()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "get_fishX()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "get_fishY()I",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "hasSkillEffect(I)Z",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "isAgro()Z",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "isAgrocoi()Z",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "isAgrososc()Z",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "isAiRunning()Z",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "isDead()Z",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "isFishing()Z",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "isInvisble()Z",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "isKnownNpcViewerConsistent(Llocal/lito381/server/NativeDeDeletionState;)Z",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "isParalyzed()Z",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "isPickupItem()Z",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "isShop()Z",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "isSleeped()Z",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "knownsObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "onAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "onPerceive(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "onTalkAction(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "rebindWorldMap(S)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "removeAllKnownObjects()V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "removeKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "removeKnownObjectIfPresent(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "resurrect(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "scheduleNpcChatTimer(Llocal/lito381/server/NativeDeChatTimer;JJ)Ljava/util/concurrent/ScheduledFuture;",
      "status": "SAME_PATHS",
      "detail": "path multisets equal (655)"
    },
    {
      "method": "setAc(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setAddAttrKind(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setAgro(Z)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setAgrocoi(Z)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setAgrososc(Z)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setAtkspeed(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setBraveSpeed(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setCha(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setChaLightSize(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setCon(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setDead(Z)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setDex(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setExp(J)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setGfxId(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setGfxidInStatus(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setHeading(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setId(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setInt(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setKarma(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setLawful(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setLevel(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setLightSize(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setLocation(III)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setLocation(Llocal/lito381/server/NativeDeWorldLocation;)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setMap(Llocal/lito381/server/NativeDeWorldMap;)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setMap(S)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setMaxHp(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setMaxMp(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setMoveSpeed(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setMr(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setName(Ljava/lang/String;)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setNameId(Ljava/lang/String;)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setOwnLightSize(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setPassispeed(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setPickupItem(Z)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setStatus(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setStr(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setTempCharGfx(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setTempLawful(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setTitle(Ljava/lang/String;)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setWeaponBreakStack(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setWis(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setX(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setXY(II)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setY(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "set_deadTimerTemp(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "set_showId(I)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "setting_template(Llocal/lito381/server/NativeDeNpcTemplate;)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "sourceChat$startChat(I)V",
      "status": "DIFF",
      "detail": "method sourceChat$startChat(I)V insn[58]: left=INVOKEVIRTUAL local/lito381/server/NativeDeNpcConstructionState.cancelNpcChatTimer()V right=INVOKESPECIAL local/lito381/server/NativeDeNpcConstructionState.cancelNpcChatTimer()V; path event multisets differ (left=1310, right=1310)"
    },
    {
      "method": "sourceDeleteDe()V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "start_shop()V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z",
      "status": "ONLY_LEFT",
      "detail": "only left"
    }
  ],
  "effectsMethods": [],
  "approvedMethods": [],
  "firstDiff": {
    "where": "method",
    "member": "addAc(I)V",
    "insnIndex": null,
    "detail": "only left"
  }
}
```

## 輸入／輸出與診斷 SHA-256

完整收據包括每份 staged、stub、工具來源、核准清單與輸出雜湊。

```json
{
  "counts": {
    "SAME": 86,
    "SAME_EFFECTS": 0,
    "SAME_LAYOUT": 0,
    "SAME_PATHS": 16,
    "ORPHAN_IGNORED": 0,
    "DEVIATION": 0,
    "DIFF": 2,
    "ONLY_LEFT": 0,
    "ONLY_RIGHT": 0,
    "BYTE_SAME": 9
  },
  "classSelection": {
    "missingLeft": [],
    "extraLeft": []
  },
  "hashes": {
    "lito伺服器\\LiTo381-Server\\reference\\base-0.6.71.jar": "e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498",
    "scratch\\issue-73\\final-current\\target\\a02.jar": "c12ece29d89d3fcbf1b26c241f66d7f3237dd6dd3cb1702e23bf74049fbc3134",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemGreaterTierTable.java": "ede2583095b2e0354399aa46a9f56785d5ff8957832ed5164f5913ec584526f2",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemIdStatsTable.java": "1029973b5e5604e5646ebc3b25725fad96d8fd13e1f645d6f2c0d15745a38d59",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemInlayCardTable.java": "1bd48402798dc358e6a64c6bf1c8f0110004031e95f5a56ea91d2804176327d5",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemInventory.java": "e8190672c767e372692abd625bd93f3725dddb59ffd0ee37fce10b7efff8628b",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemMagicStoneTable.java": "e27e61e6e331343fb05278589f11e39b1feb8ff897f1c9446e2ebdd7d9880534",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemPublication.java": "2e807a0969f06ffec0c6bc26ae8313157d4ec4662a909d46e5d27024f8375f3a",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemStateCopy.java": "1b6c95f5860b7cbcd769cfab05c3bf5908308d99ddfa4e8efb15df6e4e254f23",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemTable.java": "8a0f28930c4642f2987b3fad4841d8db22396766717a338b0e5787f5de5de712",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemTemplatesResources.java": "1e101bfbb1b87fa16cd8d72c1786c5a221a0781aa7d8369f09d513171dec44e3",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemTemplatesScope.java": "9bb8be8e2e8748d92878833f6991cf29654df48da3d899bf5335ebf8796bd545",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemTransaction.java": "80289edc06c9a93110e0d23eb0af46e567928064b0b4b9496d7d6dc3d0a8c92c",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemWeaponAttrTable.java": "28c93ef7c0ed37c62a8ce169a4b5991772958958a5674ad7c87164a26be39b42",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemWeaponProc.java": "7df86cd1fdfe705f9048aab27bbaf7f7b9e1e727db0a5426ebbadf467dd1fa91",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeItemWeaponProcTable.java": "7ad62ed0fd2e5a325ad6e420d4fc78fd7c31255b80cc5b52517327b7d5dc01aa",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeKnownState.java": "a76c98f95929b6a8ada6d13e2cae825bb87408d6b4613878156f667d02edb3fe",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeLiveAiJobs.java": "4871a84e01a6ab5f7e14186083eb5ff397fe459fee156ba433e8f9a97b4f228f",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeLiveAiPorts.java": "0dff19bc82839ac2d41b8c4e35e265a86f5bae8d7e61f752d85303179e69cadb",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeLiveAiScope.java": "d1db55623420f199ead4904fdde691db3635f397bd9db5d99246baaee5478806",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcConstructionState.java": "33e0c42935cc7dc8d7f0758704f3a54264b56b426f9715d5b9dadd21b6c72e1a",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcExecutor.java": "062d4d825da74fa0d5534a7e0de919a910d7fad83780b7e48d4881c6e944e536",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcHateList.java": "5490289db70fc77765a31060c1e50e9fc6456ddc7ab9871f3e862ebeeb7f492d",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcInventory.java": "1456f0627fbe0930636853c0923088f95d77c588257ef402dc818c18e8ef5656",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcKnowledge.java": "375659a1c91105edcc58171c729d982f828337eb5574672145f537a3c9b5b324",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcMobSkill.java": "e399c19066b846c44f5e10a7013d3547b6ef6e55d04480a82e68828bd669261e",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcMobSkillUse.java": "f8254acd0b9d5cf1f8b384f6d916093b306642d8870505f5d38c7b515b7ebbf0",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcMoveState.java": "ff7795aba76a173c2e0c10dd44bfcc495eb2b965eb68395a052be77baa4b35cd",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcTemplate.java": "58632d6eae4a2a6cfb73dc693adf53f08b3377d79b3e69c2c677e1fdc4a82f31",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcTemplateLoadScope.java": "49bdeb04ff767046597907af6f47758b9ef01e83c05a4e424ebac7b42a43eb14",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcTemplateMobLoader.java": "361823ad9129c724aa7ba7246aaa70fd612e317defbf2bd269c568a9f0091715",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcTemplateNpcLoader.java": "d57acbbce95a252cafff79ccf5d43e1fab12c891f501f5ed4d1b87d1b308add1",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcTemplateResources.java": "d92e4f24df4321df2fcab2bd8b3e5ed5acd132674e7fb6b788f23a333dad2685",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcTemplateState.java": "682568be19897fa018b7983c73135fb9aef8e5f650f8c0a8705dbbff9fa11d06",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeNpcVitalState.java": "b85d1d72e3fa21a7f61b7b30e4cd521280f80ab8318c7f4d2a4b67c01a119769",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeObjectBindings.java": "176d7380251e49acfab16853e0310432de2b54dfe59008ff56c8cd6157f3d424",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeObjectFields.java": "39863735695f84675af87bfa0187b61f683de84a3bbfed718627d87973e1c624",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeObjectScope.java": "8da829b1164e36802f4cbdfc82b4847cf690667c34f94dc2c7d18ddb7506fedf",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDePerceptionAccess.java": "64933df0243246f25e32f66c64e33233ccd9928dc7d94e7207f8a0fe8e946a15",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDePerceptionBadText.java": "a0c23ead38a8d80c84bc505ffd7d3abcccdec2c0c2e48aca46f91a44e335267c",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDePerceptionControls.java": "0fd8cf454ec05fe2333d91ee3ac9245b9d45a7acfa7d2ebec01e4a5a24dbef7f",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDePerceptionEmblem.java": "4d35de3041ecf5f3fe68edbed3975c78d903c6c3f272ccda55c2e9dbeeddc981",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDePerceptionJobs.java": "ca5867d2b4c8ab9652860070c33a69f24594a027718485ffc3c16f8d6e68fb67",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDePerceptionNPCPack_De.java": "e8748815e87da79a5037a0a6df8e2925da18b1b5df5db6fd983d51b55889d1af",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDePerceptionOutput.java": "73afef8d295ed7429c3ab82ed7ff042fa652c963cd72c1695f0ea88f3fb48bcb",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDePerceptionPacket.java": "393a772c6da98b0a68d24c551950d055d096abc3188c107170470323218e2964",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDePerceptionScope.java": "b3ed603d497e37f6ae3bd941b6611aa22a63719869970cb0d0357d494098f8a2",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDePerceptionSpeed.java": "c713181eeda3683ea43dad1e2180473b754c1c0f4c14796955a29cffc1ac43f3",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDePerceptionState.java": "57418e1b4d0428278a505e534ed251c6702b79e3153caeac620ce7dfffc7ada3",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeProfileKernel.java": "45418636db0e15897488eb3a86609b97b0d067cbad387439550c63b19394e160",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeShopData.java": "bf6d279fcd9dd1c79a38b8df099fa842931afa4ac4946df94919f533f32f5c51",
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeDeWorldLog.java": "8aae89d6f7344bcdb4e606b681b5bd356efda8cf3ee6ea3868448624c3f7c055",
    "lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\BlockExpressions.java": "f5d93c6a34f3d3b9b1cb769749b7be6cc6df598d53e106ef34c04f3e3e13a5dc",
    "lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\ClassComparer.java": "32fe3a8dce53cbcc2955dbcab27634f719736a71343641195bd2ddfb468ea6d1",
    "lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\FourthNormalizations.java": "f2994f27816d427575b1d65d7e33502dd0006b49938a46d5e1633396e3e0a0ce",
    "lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\JarIndex.java": "78219bb9c1aca74e275450046313b4dd3eacfaa26e6140aead2f9259911d73f5",
    "lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\LayoutComparison.java": "c3c31be673b6088eacb2fcbb4da71d6523951be2bcc9d2e7c2325778178c6078",
    "lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\LocalTransport.java": "bd98cfc3303dbb170595bc63143c5be95c3483b720a912861708c5448d4643d8",
    "lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\LocalWebs.java": "564d8dbc0256890d64520c46815bfa00b5c93075b9e65052121488bdcf9fd608",
    "lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\NonNullChecks.java": "2dbd1a43a1c2324dda5af4d3de510f8472daf880676892bf2788ff106578d923",
    "lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\Normalizations.java": "6094fa299e2b17538a6a7050464e624ffec75c7cecf8566d667fc11058e79dec",
    "lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\PathComparison.java": "a36626687f3177ef1fe38ce5aa68d5ecf9f27927fc3fca0788add0098d1162ad",
    "lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\RethrowHandlers.java": "29f7f291cd6bf5cecfe77653fc3267e80decea857d3ea782878802727a37215e",
    "lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\SemanticDiff.java": "29234a4a092712401e85245e73b95bd5c48d85fbe82c8810b52ceb90233c4bfd",
    "lito伺服器\\LiTo381-Server\\tools\\semantic-diff\\src\\main\\java\\local\\lito381\\semanticdiff\\TransportComparison.java": "fa05cf2ea262c8d6b23a2fcbdcf0fb1d76057519978e081e5433067679df4fa2",
    "D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm\\9.7\\asm-9.7.jar": "adf46d5e34940bdf148ecdd26a9ee8eea94496a72034ff7141066b3eea5c4e9d",
    "D:\\dev-cache\\maven\\repository\\org\\ow2\\asm\\asm-tree\\9.7\\asm-tree-9.7.jar": "62f4b3bc436045c1acb5c3ba2d8ec556ec3369093d7f5d06c747eb04b56d52b1",
    "scratch\\issue-73\\target\\dependency\\asm-analysis-9.7.jar": "7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f",
    "lito伺服器/LiTo381-Server/compile-stubs/com/lineage/server/datatables/QuestInstanceTable.java": "97fd50dbf62cf5237dd14ec1d5d818e98cba171c99f4ab414c51dc73f4bfb206",
    "lito伺服器/LiTo381-Server/compile-stubs/com/lineage/server/model/Instance/DeathLifecycleGate.java": "dc13d60f07c7deee584165f628afcc600918fc15807777220b70e317df0f9d99",
    "lito伺服器/LiTo381-Server/compile-stubs/com/lineage/server/model/Instance/NpcDeletionVisibilityState.java": "bcda282d398000a4829b0b7c40c75ea86f87b56cb99f3a7f1e99db91436c1a5f",
    "lito伺服器/LiTo381-Server/compile-stubs/com/lineage/server/model/item/ItemGenerationContext.java": "660a183d6e88adc894cf818209d7d97f0ad57001e16dd31896b00994376c7a29",
    "lito伺服器/LiTo381-Server/compile-stubs/com/lineage/server/model/proc/ProcRandomHelper.java": "ff460779c9f3cbbbe9b9daaa5e88e83b823523b596a7a3121b17bc80723fbcf5",
    "lito伺服器/LiTo381-Server/compile-stubs/com/lineage/server/model/skill/L1SkillId.java": "7352c513fc749b8bb09efc2605b8b21e9911e67e7ca8c9b6496ac5b03a79d59f",
    "lito伺服器/LiTo381-Server/compile-stubs/com/lineage/server/utils/TeleportTrace.java": "d2e9a46d54ccab8e8e2eeac7572ace154971f6e624a0d38d1131334c9b3fdcbd",
    "lito伺服器/LiTo381-Server/compile-stubs/com/lineage/server/world/PlayerFilter.java": "861c5e1b4e090b4148a8b5a019737ccfe6a8d44a67ee8fdc881332ba3e62b435",
    "lito伺服器/LiTo381-Server/compile-stubs/com/lineage/server/world/World.java": "ea679d91873e2a54dde87aca99349a25f17b08f89fc020b738ef59ff3210cbd0",
    "lito伺服器/LiTo381-Server/tools/as-record/src/main/java/local/lito381/asrecord/AsRecord.java": "22dc67fe896d8dc2c6ee3bbb4aef0ec9273c65a183dab1e31bd62ce71a032c42",
    "lito伺服器/LiTo381-Server/tools/as-record/src/main/java/local/lito381/asrecord/ScanRecordFields.java": "22fcf570f6146a345d500930cea5d5bd00b25621ed1301ac9e93fb6679c3cd52",
    "lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/AsmFinal.java": "29526c8b9e9737ecdd12d553d651d134c8ee2ef2e754486bc69381851b8e4de2",
    "docs/combat-merge/access-widen-approved.txt": "4bf5066b4ba996472337b173330e9fb5306b4378847512d15fe510d9b666ac74",
    "docs/combat-merge/effects-approved.txt": "b48431e80e508f795e2dd9c26c316a335e2149f82bbb4269326f132e85953e58",
    "docs/combat-merge/extra-methods-approved.txt": "8151304a004b559589a33f1fb003443e6a21ec8840e247ce19276cf53220e48d",
    "docs/combat-merge/layout-approved.txt": "280a45a28d8e636db43b94b21d67213c865b667bbb738c891422b7b459eb4c11",
    "docs/combat-merge/a-batches.json": "ac1c17ec1a446564c6fb912235ecd9222d06bef1a3a91cdcb3836fda158ad65d",
    "scratch/issue-73/verify_current.py": "bb63c3ba6cd835ee1b4dd5ce245d8db20fc3bb3d0015f2a2b536dc48191a8945",
    "scratch/issue-73/final-current/conversion.json": "384d340b56e617bf794871226e553972c87b4e6399fa475f2422a3f262a40b67",
    "scratch/issue-73/final-current/commands.json": "6b06d1e9a8ec509a27e0567b6914dae4fb6f7221b7da9c004ed67fc2ed55b925",
    "scratch/issue-73/final-current/verify_entries.txt": "0820ebf06520d8b142b0907016f1cc03458560215fe27c6a7f6e714cb78c0462",
    "scratch/issue-73/final-current/semantic.json": "b675c456011bad4fdf4369b808914f50eea91bf4fb47410e8aec52e38bdda0d0",
    "scratch/issue-73/final-current/target/javac-raw/NativeDeItemTable.class": "820cfd3bbfc23146a9ca7593f206eac95eb063cada57092a63d1e1ec07d9ba13",
    "scratch/issue-73/decompile_current.py": "57bf921466dd05565bb8490f67e269c0f10453bc03dd20d3853667b5fc5b78a9",
    "scratch/issue-73/vineflower-1.11.1.jar": "a615d07ddbbcd489369674f40e42df639c32be95410890b38f173d5c1e2ea39c",
    "scratch/issue-73/fresh-vf.log": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/DumpNormalized.java": "7b47934a31b3d4a07c5db8866eadbf32fc97471fe0e624d1b724b1b348adc468",
    "scratch/issue-73/dump_current.py": "63b23a0cec96a81ba6c3ade447b1cc655a680829ff667d721632e7bc194ad8a9",
    "scratch/issue-73/Audit.java": "fe151e6cb78c18e9c9dfdbf75eac63914002cf645f99fd964aa924f32aaad503",
    "scratch/issue-73/audit_final.py": "bc8f159e040cb4a9d0236d801d679bc8010e63e1a0fb74c2e84e266406f6858b",
    "scratch/issue-73/prove_blockers.py": "91735bd7af3bbf078a5d443daa1fcebf30ba46e48129cb1fea9d62efd1bf4a90",
    "scratch/issue-73/final-current/audit.log": "f8c4b97d962840a06d8a5d41dce09c38418fbdb469202447cc1edd5d24ad5ab9",
    "scratch/issue-73/final-current/audit-commands.json": "c4813ac3f479767ed14e4fbc35df93e66e657272be9bc012df36a98cb69b1aec",
    "scratch/issue-73/blockers/NativeDeItemWeaponProcTable.log": "e6e7969da6c0c10e6177e093218a6a7d890d0d7245aedd2e3dad179abba4f435",
    "scratch/issue-73/blockers/NativeDeNpcConstructionState.log": "8099d30640ef78c9602da9e8e4b750d11b28f6464b8031a3201074e5207440ef",
    "scratch/issue-73/final-current/NativeDeItemGreaterTierTable$TierChance.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeItemGreaterTierTable.diff": "50d1d83b44ee06914fea0e3664afdfd8e3ce836d65133baf5caef5f88dd5cef3",
    "scratch/issue-73/final-current/NativeDeItemIdStatsTable.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeItemInlayCardTable.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeItemInventory$BoundItem.diff": "94bad3ce78b41d79ef7aed9f63bf1e491ce17f6b25c42416beff4c51630a8e3f",
    "scratch/issue-73/final-current/NativeDeItemInventory$EntryState.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeItemInventory.diff": "966d8a8a68596a5e65bd699cfe50ff0f693f6a5e7fb49c9400101cdd3a46ed05",
    "scratch/issue-73/final-current/NativeDeItemMagicStoneTable.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeItemPublication$Ticket.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeItemPublication.diff": "911bd9c356932e4ee6c1ba5b98bb8a146ddea6b33d55c5adc705566529bb2b75",
    "scratch/issue-73/final-current/NativeDeItemStateCopy.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeItemTable.diff": "49beaa91a64ee857247b86d6c853a772bcc3240fc2a46f08b175797e716a5d7d",
    "scratch/issue-73/final-current/NativeDeItemTemplatesResources$Owner.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeItemTemplatesResources$Snapshot.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeItemTemplatesResources.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeItemTemplatesScope$Binding.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeItemTemplatesScope$Classes.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeItemTemplatesScope$Context.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeItemTemplatesScope$Data.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeItemTemplatesScope$Database.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeItemTemplatesScope$Logs.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeItemTemplatesScope.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeItemTransaction$Change.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeItemTransaction$Operation.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeItemTransaction.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeItemWeaponAttrTable.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeItemWeaponProc.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeItemWeaponProcTable.diff": "248c2c47b4110dbaedaae36bd6e21db2ea5613c36b0915a8783677fd6fa4f940",
    "scratch/issue-73/final-current/NativeDeKnownState.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeLiveAiJobs$Binding.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeLiveAiJobs$Credit.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeLiveAiJobs$Owner.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeLiveAiJobs$PulseCredit.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeLiveAiJobs$Snapshot.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeLiveAiJobs$Ticket.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeLiveAiJobs.diff": "5752bd02837a48b6aa825ceaff38d5ef002d84c8ebc0e73d55f992fcbf9dedc6",
    "scratch/issue-73/final-current/NativeDeLiveAiPorts$Combat.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeLiveAiPorts$Config.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeLiveAiPorts$Doll.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeLiveAiPorts$Environment.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeLiveAiPorts$Fishing.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeLiveAiPorts.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeLiveAiScope$Clock.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeLiveAiScope$Manager.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeLiveAiScope$Pool$ShutdownDiscardable.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeLiveAiScope$Pool.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeLiveAiScope$Sprites.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeLiveAiScope$World.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeLiveAiScope.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeNpcConstructionState.diff": "47d55aa8dad50aa1f6c5a3bcaf1d383c1a46ba31ac68d49af131e9cd3354aa51",
    "scratch/issue-73/final-current/NativeDeNpcExecutor.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeNpcHateList.diff": "5cadaccb88743d85aac971a75b4b27b44e3d31cf25dd64a97b136709f76429f4",
    "scratch/issue-73/final-current/NativeDeNpcInventory.diff": "c6f3593deb44645b617464552e4ab6d50e48a9463615a1654ed304f6e8890a0d",
    "scratch/issue-73/final-current/NativeDeNpcKnowledge.diff": "ad79bd02c9679dbd6bb5bff7f44d583094e58dec5e5cc1f28a29c81fbe6d5049",
    "scratch/issue-73/final-current/NativeDeNpcMobSkill.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeNpcMobSkillUse.diff": "0ad859d0d15f49625b580ba3a53dbf64768212a924c289058f0e040d78250bc1",
    "scratch/issue-73/final-current/NativeDeNpcMoveState$PathFailure.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeNpcMoveState$PathState.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeNpcMoveState.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeNpcTemplate.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeNpcTemplateLoadScope$Context.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeNpcTemplateLoadScope$Database.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeNpcTemplateLoadScope$Npcs.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeNpcTemplateLoadScope$Resolver.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeNpcTemplateLoadScope$Skill.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeNpcTemplateLoadScope$Skills.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeNpcTemplateLoadScope$Sql.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeNpcTemplateLoadScope$Timer.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeNpcTemplateLoadScope.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeNpcTemplateMobLoader.diff": "5939f400c3d66dd3b7f93952b460afc896f847271b1100e25e573f7e8484600c",
    "scratch/issue-73/final-current/NativeDeNpcTemplateNpcLoader.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeNpcTemplateResources$Counts.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeNpcTemplateResources$Snapshot.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeNpcTemplateResources.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeNpcTemplateState.diff": "ff399f680d3034391e0d763d732f429296e5675add528d368c14aaf9f016b604",
    "scratch/issue-73/final-current/NativeDeNpcVitalState$1.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeNpcVitalState.diff": "3bc5174cecfa111791d57647961cf8c84716ba32eb5df21164ac63487cf1d5c0",
    "scratch/issue-73/final-current/NativeDeObjectBindings.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeObjectFields.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeObjectScope$Maps.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeObjectScope$World.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeObjectScope.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDePerceptionAccess.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDePerceptionBadText.diff": "4776b93266b6674f22cabd4e0343f0e87e6652374dc8cc715c4b7918cf7c423e",
    "scratch/issue-73/final-current/NativeDePerceptionControls$Data.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDePerceptionControls.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDePerceptionEmblem.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDePerceptionJobs$Key.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDePerceptionJobs$Owner.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDePerceptionJobs$Snapshot.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDePerceptionJobs$Task.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDePerceptionJobs.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDePerceptionNPCPack_De.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDePerceptionOutput.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDePerceptionPacket.diff": "bb4a62f80683cac1e2a9f7bbb5c6fa99d10657bf9bb2dde5b079b4f64a797b55",
    "scratch/issue-73/final-current/NativeDePerceptionScope.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDePerceptionSpeed$ActionType.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDePerceptionSpeed$SpeedState.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDePerceptionSpeed.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDePerceptionState.diff": "64780e0d59c9b9c0f986bfeb9d5a2195d010d23f12a7f596f2f98596d49b1fac",
    "scratch/issue-73/final-current/NativeDeProfileKernel$Combat.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeProfileKernel$Target.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "scratch/issue-73/final-current/NativeDeProfileKernel.diff": "9565e7fe37bf2b975f823a73c6343cdcd9f6cb18e1c58682bd2ab69a32a2301e",
    "scratch/issue-73/final-current/NativeDeShopData.diff": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
  },
  "entries": 104
}
```

### 只讀診斷程式

`DumpNormalized.java`

```java
package local.lito381.semanticdiff;
import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import org.objectweb.asm.tree.*;
public final class DumpNormalized {
  static Object helper(String name,Object... args) throws Exception {
    for(Method m:ClassComparer.class.getDeclaredMethods()) if(m.getName().equals(name)&&m.getParameterCount()==args.length){
      m.setAccessible(true); return m.invoke(null,args);
    }
    throw new IllegalArgumentException(name);
  }
  static void put(List<String> out,String name,Object value){out.add(name+"="+Objects.toString(value,""));}
  static List<String> normalized(ClassNode c,JarIndex j) throws Exception {
    List<String> a=new ArrayList<>();
    put(a,"class",c.name);put(a,"access",Integer.toHexString(c.access));put(a,"super",c.superName);
    put(a,"interfaces",helper("joinSorted",c.interfaces));put(a,"signature",c.signature);
    put(a,"outerClass",c.outerClass);put(a,"outerMethod",Objects.toString(c.outerMethod,"")+Objects.toString(c.outerMethodDesc,""));
    put(a,"nestHost",c.nestHostClass);put(a,"nestMembers",helper("joinSorted",c.nestMembers));
    put(a,"permittedSubclasses",helper("joinSorted",c.permittedSubclasses));
    put(a,"annotations",helper("annotations",c));
    String inner=(String)helper("innerClasses",c,false,Set.of(),c); for(String line:inner.split("; ",-1))put(a,"innerClass",line);
    put(a,"recordComponents",helper("records",c,j));
    Map<String,FieldNode> fs=(Map<String,FieldNode>)helper("fieldMap",c,j);
    for(var e:fs.entrySet()){
      FieldNode f=e.getValue();put(a,"field",e.getKey());put(a,"access",helper("memberAccess",f.access));
      put(a,"signature",f.signature);put(a,"value",helper("value",f.value));put(a,"annotations",helper("fieldAnns",f));
    }
    Map<String,MethodNode> ms=(Map<String,MethodNode>)helper("methodMap",c,j);
    for(var e:ms.entrySet()){
      MethodNode m=e.getValue();put(a,"method",e.getKey());put(a,"access",helper("memberAccess",m.access));
      put(a,"signature",m.signature);put(a,"exceptions",helper("joinSorted",m.exceptions));
      put(a,"annotations",helper("methodAnns",m,helper("labelIds",m),j));put(a,"annotationDefault",helper("defaultValue",m.annotationDefault,j));
      a.addAll((List<String>)helper("tryCatch",m,j));a.addAll(ClassComparer.instructions(m,j));
    }
    return a;
  }
  public static void main(String[] args)throws Exception{
    JarIndex j=JarIndex.load(Path.of(args[0])); if(args.length>3)j.addClasspath(Path.of(args[3])); Path out=Path.of(args[2]);Files.createDirectories(out);
    for(String name:Files.readAllLines(Path.of(args[1]))){
      name=name.trim().replace(".class","");if(name.isEmpty())continue;
      ClassNode c=j.readTree(name);if(c==null)continue;
      Files.write(out.resolve(name.substring(name.lastIndexOf('/')+1)+".txt"),normalized(c,j));
    }
  }
}


```

`dump_current.py`

```python
from pathlib import Path
import subprocess,sys,json,difflib
r=Path.cwd(); w=r/'scratch/issue-73'; p=w/sys.argv[1]; s=r/'lito伺服器/LiTo381-Server'; j=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
cp=';'.join([str(p/'target/tool'),str(w/'target/dependency/asm-analysis-9.7.jar'),*[f'D:/dev-cache/maven/repository/org/ow2/asm/{a}/9.7/{a}-9.7.jar' for a in ('asm','asm-tree')]])
subprocess.run([str(j/'javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(p/'target/tool'),str(w/'DumpNormalized.java')],check=True)
for side,jar in [('left',p/'target/a02.jar'),('right',s/'reference/base-0.6.71.jar')]:
    subprocess.run([str(j/'java.exe'),'-cp',cp,'local.lito381.semanticdiff.DumpNormalized',str(jar),str(p/'verify_entries.txt'),str(p/side),str(s/'reference/base-0.6.71.jar')],check=True)
for f in (p/'left').glob('*.txt'):
    a=f.read_text(encoding='utf-8').splitlines(True); b=(p/'right'/f.name).read_text(encoding='utf-8').splitlines(True)
    diff=''.join(difflib.unified_diff(a,b,fromfile='staged/'+f.name,tofile='formal/'+f.name))
    (p/(f.stem+'.diff')).write_text(diff,encoding='utf-8')
print('dumps complete')

```

`Audit.java`

```java
import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public class Audit {
  static ClassNode read(Path p)throws Exception {ClassNode n=new ClassNode();new ClassReader(Files.readAllBytes(p)).accept(n,0);return n;}
  static byte[] serialize(ClassNode n){ClassWriter w=new ClassWriter(0);n.accept(w);return w.toByteArray();}
  public static void main(String[] args)throws Exception {
    ClassNode raw=read(Path.of(args[0])), converted=read(Path.of(args[1]));
    Set<String> names=Set.of("_armorTypes","_weaponTypes","_weaponId","_materialTypes","_etcItemTypes","_useTypes");
    if(raw.invisibleAnnotations==null||!raw.invisibleAnnotations.removeIf(a->a.desc.equals("Llocal/lito381/server/AsmFinal;")))throw new AssertionError("missing annotation");
    for(FieldNode f:raw.fields)if(names.contains(f.name)){if((f.access&Opcodes.ACC_FINAL)!=0)throw new AssertionError(f.name);f.access|=Opcodes.ACC_FINAL;}
    if(!Arrays.equals(serialize(raw),serialize(converted)))throw new AssertionError("Unexpected conversion change");
    System.out.println("AsmFinal: exactly six ACC_FINAL flags plus annotation removal; all other ASM tree content identical");
    ClassNode c=read(Path.of(args[2]));int count=0;
    ClassNode formal=new ClassNode();
    try(java.util.zip.ZipFile z=new java.util.zip.ZipFile(args[3])){new ClassReader(z.getInputStream(z.getEntry(c.name+".class"))).accept(formal,0);}
    Set<String> formalMethods=new HashSet<>();for(MethodNode m:formal.methods)formalMethods.add(m.name+m.desc);
    for(MethodNode m:c.methods)if((m.access&Opcodes.ACC_BRIDGE)!=0){
      if(formalMethods.contains(m.name+m.desc))continue;
      if((m.access&(Opcodes.ACC_PUBLIC|Opcodes.ACC_SYNTHETIC))!=(Opcodes.ACC_PUBLIC|Opcodes.ACC_SYNTHETIC))throw new AssertionError(m.name);
      int invokes=0;
      for(AbstractInsnNode i:m.instructions){
        if(i.getOpcode()<0)continue;
        if(i instanceof VarInsnNode v && v.getOpcode()>=Opcodes.ILOAD && v.getOpcode()<=Opcodes.ALOAD)continue;
        if(i instanceof MethodInsnNode call && call.getOpcode()==Opcodes.INVOKESPECIAL&&call.owner.equals(c.superName)&&call.name.equals(m.name)&&call.desc.equals(m.desc)){invokes++;continue;}
        if(i.getOpcode()>=Opcodes.IRETURN&&i.getOpcode()<=Opcodes.RETURN)continue;
        throw new AssertionError(m.name+" unexpected instruction "+i.getOpcode());
      }
      if(invokes!=1)throw new AssertionError(m.name);count++;
    }
    System.out.println("ConstructionState compiler bridges: "+count+"; each only loads arguments, invokes same superclass method once, returns");
  }
}

```

`audit_final.py`

```python
from pathlib import Path
import json,subprocess,hashlib
r=Path.cwd();w=r/'scratch/issue-73';p=w/'final-current';j=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
cp=';'.join([str(p/'target/tool'),*[f'D:/dev-cache/maven/repository/org/ow2/asm/{a}/9.7/{a}-9.7.jar' for a in ('asm','asm-tree')]])
commands=[('audit-javac',[j/'javac.exe','-encoding','UTF-8','-cp',cp,'-d',p/'target/tool',w/'Audit.java']),('audit',[j/'java.exe','-cp',cp,'Audit',p/'target/javac-raw/NativeDeItemTable.class',p/'target/classes/local/lito381/server/NativeDeItemTable.class',p/'target/classes/local/lito381/server/NativeDeNpcConstructionState.class'])]
steps=[]
commands[-1][1].append(r/'lito伺服器/LiTo381-Server/reference/base-0.6.71.jar')
for name,args in commands:
 args=list(map(str,args));z=subprocess.run(args,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(p/(name+'.log')).write_bytes(z.stdout);print(z.stdout.decode('utf-8',errors='replace'));assert z.returncode==0;steps.append(dict(name=name,command=args,exit=z.returncode))
(p/'audit-commands.json').write_text(json.dumps(steps,ensure_ascii=False,indent=2),encoding='utf-8')
d=json.loads((p/'semantic.json').read_text(encoding='utf-8'))
for c in d['classes']:
 if c['status']=='DIFF':print(c['name'],{status:sum(m['status']==status for m in c['methodResults']) for status in ['DIFF','ONLY_LEFT','SAME_PATHS']})

```

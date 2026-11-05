# Issue #78 — A-land 整合與驗證（待 Claude 裁定）

## 結論

**尚未達成 A-land 驗收，不可交 #225 啟動驗收或部署。** Maven package 成功，必補執行期測試全數通過；完整測試仍因正式版地圖雜湊與 src/main/resources 不一致而失敗，A 範圍仍有 CaptureLog 差異。以 `needs:claude` 交主控裁定，不把工具 exit 0 或 package 成功當成語意驗收通過。

工作分支 `task/78`，基底 `7b6a97fa84ca0408e426df4d9f70df738d218a88`。本輪未切換或建立分支。唯讀核對遠端 main `8df7c89c3a9b4dbdb8eb01b40a5b53462dbac0f8`：新增 #254 工具修正，B-land 尚未合入；本輪未 rebase，使用本分支已合入的 #235 比較器（最後工具來源提交 `2ea0999be1b1a570b315b77ec40dc7d5300019a4`）。後續合入 B-land／新版工具時必須重驗，不能沿用本表。

## 做了什麼

- 依 A01–A06 的 224 個外層類別整合：223 份 staged 定義已落入 src/main；**CaptureLog 1 份因與現行 ClientSession／來源專有 Connection API 衝突，保留 HEAD，未假稱已覆蓋全部 224 份。**
- `Accounts.java` 移除三個重複共置定義，改用獨立 AccountRepository、MemoryAccountRepository、SourceEquipmentPhysicalStats；`BattleAnimationCatalog.java` 保留原共置 BattleActionCodes。
- 補入 A 呼叫所需、正式 JAR 已存在的三項相依契約：LegacyMySqlRepository.nativeMaterialCharacterConnection()、CombatPoisonLifecycle.State 的三個 source* 欄位、NativeCastMaterial.Effect／Cast.effect 描述子。這些位於 B 的 src/main；未搬入其他 B 候選邏輯，也未修改任何 B staged。NativeCastMaterial 的 interface 回傳 primitive boolean 並宣告 IOException，已由正式 javap 核對，避免 A lambda 被編成 Boolean 版本。
- TournamentRoomPolicy 與相關測試改用正式 BattleRules.decode(RoomSettings, ...) 入口。GM 測試的帳號權限改由 AccountAccessFixture 明確提供，因正式 MemoryAccountRepository 沒有測試曾依賴的 grantAccessLevel API。
- `pom.xml` 的 #134 process-classes／process-test-classes 接線保持原樣；新增對實際正式類別的 record／final 欄位驗證。沒有將正式 class 複製進 Maven 成品，也沒有停用測試或新增 skip。

## 已驗證

### 建置與測試

Microsoft OpenJDK 17.0.17、Maven 3.9.16。所有建置產物在 target；最後驗證的 Maven 快取、測試暫存在此 worktree 的 scratch/issue-78。最後完整測試先執行 clean，排除增量編譯殘留。Windows 測試 JVM 在沙箱內啟動約 8.3 秒，會碰到既有 3–5 秒期限；改用獲准的本機執行後重跑，沒有放寬測試期限。

```powershell
# 在本 worktree 根目錄；JAVA_HOME／PATH 指向上述 JDK 與 Maven。
New-Item -ItemType Directory -Force scratch/issue-78/tmp,scratch/issue-78/m2 | Out-Null
$env:TEMP = $env:TMP = (Resolve-Path scratch/issue-78/tmp).Path
$taskM2 = (Resolve-Path scratch/issue-78/m2).Path
$env:MAVEN_OPTS = "-Dmaven.repo.local=$taskM2 -Djava.io.tmpdir=$env:TEMP -Dfile.encoding=UTF-8"
mvn -f lito伺服器/LiTo381-Server/pom.xml -B test
mvn -f lito伺服器/LiTo381-Server/pom.xml -B -DskipTests package
```

本機執行另傳 `-Dmaven.repo.local=<worktree>/scratch/issue-78/m2`、`-Djava.io.tmpdir=<worktree>/scratch/issue-78/tmp` 與同值 `-DargLine=-Djava.io.tmpdir=...`，確保 JVM 啟動時即採用 worktree 暫存；完整測試實際 goals 為 `clean test`。這些參數不過濾測試。精確命令與原始輸出保留於 scratch/issue-78 的 `full-final-command.json`／`full-final.log`、`package-final-command.json`／`package-final.log`。

| 指令 | 預期 | 實際 |
|---|---|---|
| mvn -B test | Tests run: N, Failures: 0, Errors: 0 | Tests run: 6048, Failures: 3, Errors: 635, Skipped: 110；BUILD FAILURE |
| mvn -B -DskipTests package | BUILD SUCCESS | BUILD SUCCESS；產生 target/LiTo381-Server.jar |

Skipped 來自原有環境／資源 assumptions；本任務沒有新增任何停用、跳過或忽略失敗設定。不得將此結果寫成全綠。

| 必補測試類別 | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| ALandInstanceStateTest | 4 | 0 | 0 | 0 |
| ALandItemDefinitionsTest | 4 | 0 | 0 | 0 |
| ALandLifecycleTest | 2 | 0 | 0 | 0 |
| AsRecordBuildTest | 3 | 0 | 0 | 0 |

逐條對照 deviations.md 的 A 要求：

| 要求 | 測試與觀察 |
|---|---|
| WorldLog.debug／選用欄位失敗 | ALandItemDefinitionsTest 以 JDBC proxy 驅動真實 GreaterTierTable 載入，確認 default debug 的訊息／參數、正常路徑無誤記、必要欄位失敗仍記錯並拒絕該 row |
| PoisonAtomic monitor | ALandLifecycleTest 同時測 player／npc，注入 leaveInstall 的 ThreadLocal 失敗；確認正常出口和 catch 均執行清理，離開時不持有 monitor，另一執行緒於原期限內取得同一鎖 |
| InstanceState 五個建構子 | ALandInstanceStateTest 覆蓋 construction、父鏈虛擬回呼、四個 Name overload 成功／失敗、非 concrete World 拒絕、closed World、布林組合、null／空白／有效／無效字串；失敗前不呼叫外部 ports，不登錄或排程；成功後 set_deName／broadcastPacketAll 可呼叫 |
| CombatSkillDerived.compute | 空集合／全 null 回傳六個零值；遇非 null 元素拋 NoSuchFieldError("SKU1") |
| NPC 刪除與 private 改名 | 驗證 cancelNpcChatTimer$own 為 private 且原名未宣告；sourceDeleteNpc／sourceAbortFailedWorldRegistration 進祖先清理，真實 ChatJobs ticket 被 retire，credit/pending 歸零後才呼叫 remaining.cancelChat |
| WeaponProcTable.reload | 有效 loading context 拋 NoSuchFieldError("_instance")；owner 不符、無 scope、已發布、缺 SQL context 均先被 requireLoading 拒絕 |
| @AsRecord／@AsmFinal | 實際 CombatCurseLifecycle$Target 為 record（8 components），nativeRoleOwner 非 final；NativeDeItemTable 六個指定欄位為 final |

測試邊界如實保留：以 Unsafe 建立不含排程執行緒的 owner fixture，受測 NPC 的完整 Java 建構鏈照常執行；PoisonAtomic 的 NPC_STATE method handle 只替換尚待 B-land 的來源 callback，真正 A 交易／monitor／leaveInstall 均執行，測試結束復原靜態欄位；不宣稱涵蓋 B 的完整 NPC 遊戲流程。刪除測試在 remaining.cancelChat 用 sentinel 結束，沒有跑後續世界清理。這些都是隔離單元測試，不冒充實機驗收。

### 既有測試逐案判斷

- BattleTurnUndead、BattleManaDrain、BattleSpellCatalog、ProtocolBattle 的法師案例原用 Type=1；依正式 0.6.71，法師為 Type=3。修正輸入並保留原概率期望，另修正 magicLevel 的職業對應。
- ModernTlsIdentity 原要求 Main 分派 --modern-config，與正式 Main 不符。現在直接斷言正式入口拒絕未知參數，並保留 ModernMain 專用入口的輸入拒絕及路徑不洩漏測試；沒有刪除 Modern 類別。
- Windows 跨程序 timeout：未修改原測試，最後本機重跑結果列入完整報告。
- 尚未解決的每一個 failure/error 全列於 [機器證據](issue-78-evidence.json) 的 test_failures。根因統計如下（連鎖 NoClassDefFoundError 保留原訊息，不偽裝成獨立程式缺陷）：

- 479：`java.lang.ExceptionInInitializerError: Exception java.lang.IllegalStateException: Can't overwrite cause with java.io.IOException: classic Ivory-Tower map source hash mismatch 77 [in thread "main"]`
- 132：`java.lang.ExceptionInInitializerError: Exception java.lang.ExceptionInInitializerError [in thread "main"]`
- 17：`java.lang.ExceptionInInitializerError: Exception java.lang.NoClassDefFoundError: Could not initialize class local.lito381.server.ClassicWorldNpcCatalog [in thread "main"]`
- 9：`java.lang.ExceptionInInitializerError: Exception java.lang.NoClassDefFoundError: Could not initialize class local.lito381.server.ClassicWorldMapCatalog [in thread "main"]`
- 1：`java.lang.ExceptionInInitializerError: cannot load classic map resource /maps/77.l38.gz: classic Ivory-Tower map source hash mismatch 77`

地圖 77 實際資源 SHA-256 為 `b829a6b81e4155a0eedfa8fc65d48ce6341863fd23b6a79333ba595cd5c4d4d8`，正式資源為 `a31c660396c455db07a115fcc15b2294db4139191f33c80ee96045948ba9d6bc`。正式 A 的 ClassicWorldMapCatalog 會檢查正式雜湊，導致初始化失敗。唯讀比對找到 11 份不同地圖：77、78、79、80、81、82、104、105、108、109、110；逐份雜湊見 resource_differences。**沒有改 maps 資源、跳過 hash check 或放寬測試。**

### 語意比對：三類結果

左側是本次 Maven 的 target/LiTo381-Server.jar，右側 reference/base-0.6.71.jar，SHA-256 `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`。ASM 9.7，由目前 tools/semantic-diff 原始碼重新編譯，未使用正式 JAR 作左側 fallback。比較器 exit 0 只表示報告成功生成，以下獨立檢查 statuses。

下表的整顆 JAR 比較範圍是全部 `.class`；前述 11 份地圖屬非 class 資源差異，另列驗收阻塞，不混入 class DIFF 計數。

| 狀態 | A 692 entries | 整顆 JAR |
|---|---:|---:|
| SAME | 663 | 2331 |
| SAME_EFFECTS | 4 | 4 |
| SAME_LAYOUT | 1 | 1 |
| SAME_PATHS | 17 | 22 |
| SAME_CFG | 0 | 2 |
| LAMBDA_PAIRED_BY_CALLSITE | 0 | 0 |
| VISIBILITY_BRIDGE | 0 | 0 |
| ORPHAN_IGNORED | 0 | 0 |
| DEVIATION | 6 | 6 |
| DIFF | 1 | 187 |
| ONLY_LEFT | 0 | 1381 |
| ONLY_RIGHT | 0 | 1 |

1. **A 範圍**：全部 692 entry 的類別名稱、status、逐方法差異見 evidence.a_classes；DIFF=1、ONLY_LEFT=0、ONLY_RIGHT=0，未達標。Main 為既有精確核准的 SAME_LAYOUT；任務列舉的可接受 status 未含 SAME_LAYOUT，因此另請主控確認，未擅自改列 SAME_CFG 或新增核准。
2. **整顆 JAR**：B 差異 `{'DIFF': 186, 'ONLY_RIGHT': 1}`；來源專有 `{'ONLY_LEFT': 1381}`。完整類別清單在 evidence.difference_groups.B／SOURCE_ONLY。B 差異均可對回現有 server-semantic-gap 的既有差異，新增 B 差異清單為 `[]`；來源專有新增清單為 `[]`。原清單中的 NativeDeVisibilityPc$1 已被 A staged 的新實作取代，沒有再產生這個舊匿名類別。
3. **其他差異**：`{'DIFF': 1}`，不為 0，須 needs:claude。以下列出全部類別，完整 methodResults／firstDiff 在 evidence.difference_groups.OTHER：

- `local/lito381/server/CaptureLog`：DIFF；`{"where": "class", "member": "nestMembers", "insnIndex": null, "detail": "left=local/lito381/server/CaptureLog$Connection right="}`

CaptureLog 保留現行 capture_id／sequence／connection_id／endpoint／phase／observation 與 Connection API；ClientSession 有三處 frame 呼叫、建構時 connection 呼叫與欄位型別依賴。直接替換 staged 會使目前 B 的 ClientSession 編譯失敗；直接刪除擴充又會撤回目前的記錄契約及來源專有 Connection 類別。請 Claude 裁定如何協調 A 等正式的要求與這項現有功能；本輪沒有自行增加整類豁免或修改 B staged。

使用全部適用核准清單，依比較 class 集合及兩側實際存在的 owner 取交集。其餘候選專有核准無適用目標，逐條記錄於 evidence.unused_approvals；沒有擴張核准。

| 清單 | A 適用條數 | 整顆適用條數 |
|---|---:|---:|
| effects-approved.txt | 7 | 9 |
| extra-methods-approved.txt | 1 | 1 |
| metadata-exempt.txt | 1 | 5 |
| access-widen-approved.txt | 3 | 4 |
| deviations.txt | 15 | 15 |
| exceptions-exempt.txt | 0 | 0 |
| layout-approved.txt | 1 | 1 |

### semantic-diff 重現腳本

以下存為 `scratch/issue-78/verify.py`，在 repo 根目錄以 Python 3 執行 `python -X utf8 scratch/issue-78/verify.py`。先完成上面的 Maven package；m2 中 ASM 依賴不足時腳本從 Maven Central 下載至 worktree scratch。輸出 a.json、full.json、classification.json 與完整 CLI argv。工具預期的 A 閘門為 DIFF／ONLY_* 全為 0，其他差異也須為 0；本輪實際未滿足。

```python
from pathlib import Path
import json,subprocess,zipfile,hashlib,urllib.request,collections,sys
r=Path.cwd(); s=r/'lito伺服器/LiTo381-Server'; w=r/'scratch/issue-78'; j=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
target=w/'target'; target.mkdir(exist_ok=True)
commands=[]
def run(name,args):
 args=list(map(str,args)); p=subprocess.run(args,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 (w/(name+'.log')).write_bytes(p.stdout);commands.append(dict(name=name,argv=args,exit=p.returncode))
 (w/'semantic-commands.json').write_text(json.dumps(commands,ensure_ascii=False,indent=2),encoding='utf8')
 print(name,p.returncode,flush=True)
 return p.returncode
deps=[w/'m2/org/ow2/asm'/n/'9.7'/f'{n}-9.7.jar' for n in ['asm','asm-tree','asm-analysis']]
for p in deps:
 if not p.exists():
  p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(urllib.request.urlopen('https://repo.maven.apache.org/maven2/org/ow2/asm/'+p.parent.parent.name+'/9.7/'+p.name).read())
for p,expected in zip(deps,['adf46d5e34940bdf148ecdd26a9ee8eea94496a72034ff7141066b3eea5c4e9d','62f4b3bc436045c1acb5c3ba2d8ec556ec3369093d7f5d06c747eb04b56d52b1','7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f']):
 assert hashlib.sha256(p.read_bytes()).hexdigest()==expected
cp=';'.join(map(str,deps)); tool=target/'tool';tool.mkdir(exist_ok=True)
assert run('tool-javac',[j/'javac.exe','--release','17','-encoding','UTF-8','-cp',cp,'-d',tool,*sorted((s/'tools/semantic-diff/src/main/java').rglob('*.java'))])==0
if len(sys.argv)>1 and sys.argv[1]=='tool':sys.exit(0)
left=s/'target/LiTo381-Server.jar'; right=s/'reference/base-0.6.71.jar'
with zipfile.ZipFile(left) as z: le={n.removesuffix('.class') for n in z.namelist() if n.endswith('.class')}
with zipfile.ZipFile(right) as z: re={n.removesuffix('.class') for n in z.namelist() if n.endswith('.class')}
a={n.removesuffix('.class') for b in json.loads((r/'docs/combat-merge/a-batches.json').read_text(encoding='utf8'))['batches'] for n in b['verify_entries']}
b={n.removesuffix('.class') for b in json.loads((r/'docs/combat-merge/batches.json').read_text(encoding='utf8'))['batches'] for n in b['verify_entries']}
(w/'a-entries.txt').write_text('\n'.join(sorted(a))+'\n',encoding='utf8')
for label,selection in [('a',a),('full',le|re)]:
 flags=[]; skipped={}
 for flag,file in [('effects-methods','effects-approved.txt'),('access-widen','access-widen-approved.txt'),('deviations','deviations.txt'),('exceptions-exempt','exceptions-exempt.txt'),('extra-methods','extra-methods-approved.txt'),('metadata-exempt','metadata-exempt.txt'),('layout-methods','layout-approved.txt')]:
  lines=[];skipped[file]=[]
  for line in (r/'docs/combat-merge'/file).read_text(encoding='utf-8-sig').splitlines():
   key=line.split(' #')[0].strip()
   if not key or key.startswith('#'):continue
   owner=next((v for v in key.split() if '#' in v),key.split()[0]).split('#')[0].removesuffix('.class')
   if owner in selection and owner in le and owner in re:lines.append(line)
   else:skipped[file].append(line)
  if lines:
   p=w/(label+'-'+file);p.write_text('\n'.join(lines)+'\n',encoding='utf8');flags+=['--'+flag,p]
 (w/(label+'-unused-approvals.json')).write_text(json.dumps(skipped,ensure_ascii=False,indent=2),encoding='utf8')
 args=[j/'java.exe','-Xmx2g','-Dfile.encoding=UTF-8','-cp',str(tool)+';'+cp,'local.lito381.semanticdiff.SemanticDiff','--left',left,'--right',right,*flags,'--json',w/(label+'.json'),'--md',w/(label+'.md')]
 if label=='a':args+=['--classes',w/'a-entries.txt']
 run('semantic-'+label,args)
 if (w/(label+'.json')).exists():print(label,json.loads((w/(label+'.json')).read_text(encoding='utf8'))['counts'],flush=True)
full=json.loads((w/'full.json').read_text(encoding='utf8'))
groups=collections.defaultdict(list)
for c in full['classes']:
 if c['status'] not in {'DIFF','ONLY_LEFT','ONLY_RIGHT'}:continue
 name=c['name']
 group='B' if name in b else 'SOURCE_ONLY' if c['status']=='ONLY_LEFT' else 'OTHER'
 groups[group].append(c)
(w/'classification.json').write_text(json.dumps(groups,ensure_ascii=False,indent=2),encoding='utf8')
print('classification',{k:dict(collections.Counter(c['status'] for c in v)) for k,v in groups.items()})
print('SHA256',hashlib.sha256(left.read_bytes()).hexdigest())
```

## 推測

- 地圖雜湊失配是大量初始化連鎖失敗的共同原因；只修復資源不代表後續所有整合測試必然通過，仍需完整重跑。
- A 的固定比較模型與核准只覆蓋類別語意；不能由此推定 B 尚未落地的遊戲流程或資源內容正確。

## 待驗證／給下一棒的提示

1. Claude 裁定 CaptureLog 的相容策略、Main SAME_LAYOUT 是否符合 #78 的 status 閘門，以及 map 資源由哪個任務對齊正式版。不可直接合併作為 A-land 完成。
2. 本機執行器依裁定修正後，重跑完整 test／package 及兩次 semantic-diff；B-land 若先合入，先 rebase 並重跑 A／B 比對。
3. #225 僅能使用最終通過版本；本輪 JAR 不供登入／截圖驗收。沒有啟動正式服務、遊戲或連正式 DB，沒有驗證玩家流程。

## 產出路徑

- 來源與測試：`lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`、`src/test/java/local/lito381/server/`。
- 本報告：`docs/reports/issue-78.md`；完整分類、類別清單、失敗案例、資源雜湊、精確比較命令：`docs/reports/issue-78-evidence.json`。
- 未入 git 的本機產物／原始日誌：`lito伺服器/LiTo381-Server/target/`、`scratch/issue-78/`。正式 JAR、B staged、機密設定、封存、releases、docs/REPORT.md 均未修改。

`target/LiTo381-Server.jar` SHA-256：**`45a3f5e778f2652a6bbad9ffe0b27762447e58ef17a965bf1cf0604fe615aeb8`**。

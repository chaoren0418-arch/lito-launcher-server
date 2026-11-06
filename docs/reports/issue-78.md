# Issue #78 — A-land 整合與驗證

## 結論

**程式、單元測試及 class 語意驗收已通過；三份建置中繼資料、CaptureLog adapter 格式邊界及補回的正式資源均已獲主控核准。PR #264 須待 CI 全綠後轉為 ready for review，再由 Claude 審查、使用者合併。**

完整測試 `6050` 項，Failures `0`、Errors `0`、Skipped `110`。A 692 entries 的 DIFF／ONLY_LEFT／ONLY_RIGHT 均為 0，CaptureLog 為 SAME；整顆 JAR 的其他 class 差異為 0。全部 1,366 份遊戲資源逐位元組相同，DIFF／ONLY_LEFT／ONLY_RIGHT 均為 0；三份 META-INF 差異另列為「建置中繼資料（已核准）」。

本輪依 [主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/78#issuecomment-6672249950) 完成後續整合；[中繼資料裁定請求](https://github.com/chaoren0418-arch/lito-launcher-server/issues/78#issuecomment-6672434402)。沿用 task/78，合併 main `8df7c89c3a9b4dbdb8eb01b40a5b53462dbac0f8`（含 #254），B-land 尚未合入；不切分支、不改 B staged。

本次短輪依 [主控收尾裁定（依 fa21a92c）](https://github.com/chaoren0418-arch/lito-launcher-server/issues/78#issuecomment-6672896199) 更新本報告，唯讀核對兩側 Manifest 與 JAR SHA-256。未修改程式、POM、測試或資源，因此沿用 `fa21a92c` 的完整 Maven／semantic-diff 證據，依裁定不重跑 semantic-diff。下列 Maven 與語意比對數字均為前輪已完成的結果。

## 做了什麼

- A01–A06 全部 224 個外層類別定義已落入 src/main；223 份 Java 與 staged 文字相同，BattleAnimationCatalog 額外保留共置的來源專有 BattleActionCodes。保留前輪的 Accounts 共置拆分、三項相依契約、職業 Type 修正及 13 項必要測試。
- CaptureLog 換回 A staged；新增 ModernCaptureLog 與其 Connection，保留擴充記錄的 capture_id／sequence／connection_id／endpoint／phase／observation。ClientSession 的內部型別與相關測試改為 ModernCaptureLog。
- Main／SourceLogin／NativeServerLifetime 都屬 A，保留正式 CaptureLog 方法描述子；ClientSession 提供相容建構子，以 ModernCaptureLog adapter 委派正式 frame／close。**正式入口沿用正式記錄格式；明確傳入 ModernCaptureLog 的入口才產生擴充欄位。**沒有反射取用 CaptureLog 的 writer、額外開檔或改動 A 入口。新增兩個測試覆蓋擴充欄位／連線識別／遮罩及正式 adapter 的 close 邊界。
- 從唯讀 reference 取出 11 份 maps（77–82、104、105、108–110），核對 SHA-256 與前輪 formal_sha256 完全相同。另補回 725 份正式資源：battle 2、combat 723，4,076,189 bytes。全部是建置輸入；未修改正式 JAR、客戶端、機密設定、封存或部署成品。
- pom.xml 的 #134 轉換接線保持原樣。成品 3,935 份 class 已逐份核對與最後完整測試的 target/classes 位元組相同，且不含 compile-stubs／ASM／轉換器 class；沒有從正式 JAR 補入任何 class。
- 用與 CI 相同的 jar tf 比對程式重建 docs/server-source-gap.md，列出兩個新增 Modern 類別及舊 CaptureLog$Connection 的移除。

## 已驗證／怎麼驗證

### Maven 與必要測試

Microsoft OpenJDK 17.0.17、Maven 3.9.16；所有 cache／TEMP／java.io.tmpdir 在本 worktree 的 scratch/issue-78，建置產物在 target。未啟動伺服器、遊戲或正式 DB；單元測試使用原有本機 fixture。先 clean 執行全套，逐案修正失敗後再執行完整 test，沒有刪除測試、增加 skip、放寬 timeout 或改 hash check。

```powershell
# 從 task/78 根目錄執行；使用 Java 17 與 Maven 3.9。
New-Item -ItemType Directory -Force scratch/issue-78/tmp,scratch/issue-78/m2 | Out-Null
$taskTmp = (Resolve-Path scratch/issue-78/tmp).Path
$taskM2 = (Resolve-Path scratch/issue-78/m2).Path
$env:TEMP = $env:TMP = $taskTmp
$env:MAVEN_OPTS = "-Dmaven.repo.local=$taskM2 -Djava.io.tmpdir=$taskTmp -Dfile.encoding=UTF-8"
mvn -f lito伺服器/LiTo381-Server/pom.xml -B "-DargLine=-Djava.io.tmpdir=$taskTmp" test
mvn -f lito伺服器/LiTo381-Server/pom.xml -B -DskipTests package
```

預期及本輪實際：`Tests run: 6050, Failures: 0, Errors: 0, Skipped: 110`；兩次 Maven 均 BUILD SUCCESS。Skipped 是既有本機資源守門，未新增。完整 argv、exit、日誌 SHA 在 [機器證據](issue-78-evidence.json) 的 build_commands／log_sha256。

必補三組 ALand 與 AsRecordBuildTest 共 13 項，本輪全數通過；新增 ModernCaptureLogTest 2 項亦通過。轉換器實際將 CombatCurseLifecycle$Target 建成 8 components 的 record；NativeDeItemTable 六個指定欄位為 final。

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

### 既有測試逐案處理

- 保留前輪已核准的 BattleTurnUndead／ManaDrain／SpellCatalog／ProtocolBattle 職業 Type、ModernTlsIdentity 正式入口及 AccountAccessFixture 修正。
- ClassicWorldDungeonCatalogTest、ClassicWorldIvoryTowerCatalogTest：以 platform-parent 隔離 ClassLoader 直接載入正式 JAR，正式 `destinationLandingSafe` 回傳 false 的固定集合為 **287–294、299–302、307–310**。測試現在逐條核對這 16 條及其餘安全路線；不再假設先前修正版 maps 的全部落點安全。原始 probe 在 scratch/issue-78/FormalRoutes.java。
- ClientSessionGmDeIntegrationTest 與 GameHubVirtualPlayerTest：正式 BattleRules、NativeRoomRules 均經語意比對為 SAME，真人最低人數不含 .de 假人。測試現在檢查假人不能觸發倒數；40 組模式／地圖矩陣仍完整保留，補足真人及明確分隊後繼續驗證倒數、戰鬥、封包及出生點。
- ClientSessionLitoChannelSelectionIntegrationTest：三處 null capture 改用既有 discardCapture fixture；ClientSession 原本就要求有效 capture。ClientSessionLitoRoomInvitationTest 的反射欄位型別與建構 fixture 改為 ModernCaptureLog，保留原邀請流程斷言。
- 所有修改後的測試均包含在最後完整 test，test_failures 為空。

### 語意比對：三類與核准項

左側 target/LiTo381-Server.jar，右側 reference/base-0.6.71.jar（SHA-256 `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`）。由合併後 tools/semantic-diff 原始碼重編，帶 effects／extra-methods／metadata-exempt／access-widen／deviations／exceptions-exempt／layout 全部適用精確核准，範圍外條目另存 unused_approvals。沒有新增豁免或以正式 class 當左側 fallback。

| 狀態 | A 692 entries | 整顆 JAR |
|---|---:|---:|
| SAME | 664 | 2332 |
| SAME_EFFECTS | 4 | 4 |
| SAME_LAYOUT | 1 | 1 |
| SAME_PATHS | 17 | 22 |
| SAME_CFG | 0 | 2 |
| LAMBDA_PAIRED_BY_CALLSITE | 0 | 0 |
| VISIBILITY_BRIDGE | 0 | 0 |
| ORPHAN_IGNORED | 0 | 0 |
| DEVIATION | 6 | 6 |
| DIFF | 0 | 186 |
| ONLY_LEFT | 0 | 1382 |
| ONLY_RIGHT | 0 | 1 |

1. **A**：692 個 entry 全部可接受；核准項為 6 個 DEVIATION 與 1 個 SAME_LAYOUT（Main，主控明確接受），另外列出而不混入 SAME。DIFF=0、ONLY_LEFT=0、ONLY_RIGHT=0。
2. **整顆 JAR**：B 差異 `{'DIFF': 186, 'ONLY_RIGHT': 1}`；來源專有 `{'ONLY_LEFT': 1382}`。完整類別清單及 firstDiff 在 evidence.difference_groups；A 全清單在 a_classes。相對前輪 B 新增 `[]`、已消除 `[]`。新增來源專有 2 個 ModernCaptureLog 類別，移除 CaptureLog$Connection，因此淨增 1，並非在原 1,381 個上直接加 2。
3. **其他 class 差異**：0；evidence.difference_groups.OTHER 為空或不存在。

可重現比較腳本如下（保存至 scratch/issue-78/verify.py，先 package，再以 Python 3 執行；腳本將 ASM 依賴保存在 worktree scratch，Windows classpath 用分號）。CLI 預期 exit 0；另檢查 A statuses 及 OTHER 清單，不能只看工具 exit。

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
 assert run('semantic-'+label,args)==0
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

### 遊戲資源與建置中繼資料（已核准）

原始非 class 比對未排除 META-INF，忽略 ZIP 目錄項，逐份比對 bytes：**SAME=1366、DIFF=3、ONLY_LEFT=0、ONLY_RIGHT=0**。依上述主控收尾裁定，分類為：

| 分類 | SAME | DIFF | ONLY_LEFT | ONLY_RIGHT |
|---|---:|---:|---:|---:|
| 遊戲資源 | 1366 | 0 | 0 | 0 |
| 建置中繼資料（已核准，僅下列三份） | 0 | 3 | 0 | 0 |

來源專有資源清單為空。全部 entry、左右 SHA 在 evidence.nonclass_entries；725 份補回資源及 11 maps 的前後 SHA 分別在 restored_resources／map_resources。保留 evidence 的原始計數，沒有將核准差異改寫為 SAME。

全部不同 entry（完整內容差異在 evidence.metadata_differences）：

- `META-INF/MANIFEST.MF`
- `META-INF/maven/local.lito381/lito381-server/pom.properties`
- `META-INF/maven/local.lito381/lito381-server/pom.xml`

reference manifest／pom.properties 的版本為 **0.7.864**，現行建置為 **0.6.71**；嵌入 POM 也不同，現行 POM 包含必需的轉換器與 stub 排除接線。主控已接受保留本次真實建置資訊，不嵌入 reference 的舊中繼資料。核准僅適用上述三個精確 entry，其餘 1,366 份全部相同，缺少項為 0。

本次從兩顆 JAR 直接讀出 Manifest，展開續行後逐項核對：

| 屬性 | target/LiTo381-Server.jar | reference/base-0.6.71.jar |
|---|---|---|
| Main-Class | `local.lito381.server.SharedResourceMain` | `local.lito381.server.SharedResourceMain` |
| Implementation-Version | `0.6.71` | `0.7.864` |
| Manifest-Version | `1.0` | `1.0` |
| Created-By | `Maven JAR Plugin 3.3.0` | `Maven JAR Plugin 3.3.0` |
| Build-Jdk-Spec | `17` | `17` |
| Class-Path | `lib/mysql-connector-j-8.0.33.jar lib/protobuf-java-3.21.9.jar lib/HikariCP-5.1.0.jar lib/slf4j-api-1.7.36.jar` | 與左側相同 |

Manifest 唯一差異為 `Implementation-Version`；建置工具欄位相同，其他屬性差異 **0**，沒有單側專有屬性。兩側 `Main-Class` 相同，符合裁定條件。pom.properties 僅 version 不同，groupId／artifactId 相同；嵌入 POM 的完整差異保留於 evidence.metadata_differences。

## 推測

class 語意與資源檢查支持 A 範圍等於正式（含精確核准偏離），但不代表整顆 JAR 等於正式。B 尚待 M2 落地；單元測試及靜態比對不能替代實機登入／畫面驗收。

## 待驗證／給下一棒的提示

1. 三份 Maven META-INF 中繼資料、正式 CaptureLog adapter 的格式邊界及補回 725 份正式資源已由主控核准，沒有待裁定項。Claude 接續審查 PR #264，使用者合併。
2. CI 閘門：確認 `fa21a92c` 的 [build](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42134166554)／[package](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42134166709) 及本次文件提交的最新 checks 全綠後，再把 PR #264 轉為 ready for review；最終查核結果回覆於 Issue #78。CI 若失敗須逐案列出根因並加 needs:claude。本機結果不代替 GitHub CI。
3. B-land 若先合入，需依規則整合並重跑 A／B 語意比對；候選 ClientSession 換入後，如 ModernCaptureLog 無呼叫者，依主控裁定一併移除。
4. A-land 通過審查並合併後，#225 的前置條件只剩 #83（實驗 DB）；屆時使用下列 SHA 的成品驗收。未執行伺服器／遊戲啟動、正式 DB 操作或部署。

## 產出路徑

- src/main/java、src/main/resources、src/test/java 的上述來源、資源與測試。
- docs/reports/issue-78.md、docs/reports/issue-78-evidence.json、docs/server-source-gap.md。
- 本機證據 scratch/issue-78/；成品 lito伺服器/LiTo381-Server/target/。docs/REPORT.md 僅帶入 main 既有變更，沒有本任務直接修改。

`target/LiTo381-Server.jar` SHA-256：**`b56e85f570f6ae9cfe947b3a3e1afb452ed93beadfd1e76d40b3537bfc1e7861`**。

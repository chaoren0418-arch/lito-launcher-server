"""Checkpoint two separately scoped SHAs and static807 inventory. Never claim full Source, WORLD or release qualification."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil,subprocess,csv
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';C=R.parent/'continuation-current.json'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def verify(v):
 if isinstance(v,dict):
  if 'path'in v and 'sha256'in v:assert sha(Path(v['path']))==v['sha256'],v['path']
  for x in v.values():verify(x)
 elif isinstance(v,list):
  for x in v:verify(x)
proofpath=D/'Source-lifecycle-progress-verified-current.json';auditpath=D/'source-summon-lifetime-audit-current.json';proof=read(proofpath);audit=read(auditpath);continuation=read(C)
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
keys=['source_wind_target_progress','selected_player_intent_progress','all_targeted_source_paths'];assert all(k not in continuation for k in keys)
anchors={p:sha(p)for p in [R/'current.json',D/'source-mass-slow-pipeline-progress-current.json',D/'source-mass-slow-lifecycle-progress-current.json',D/'all-targeted-skill-self-server-diagnostic-current.json']}
query="@(Get-CimInstance Win32_Process -Filter \"Name = 'java.exe'\" | Where-Object {$_.CommandLine -like '*integration-lifecycle*'}).Count";assert subprocess.run(['powershell','-NoProfile','-Command',query],capture_output=True,text=True,check=True).stdout.strip()=='0'
wind=read(D/'source-wind-target-build-current.json');intent=read(D/'selected-player-intent-build-current.json');verify(wind);verify(intent)
assert wind['runtime']['sha256']=='df82862d3751b872fc29eef07d1d047a42816b4c03efcc277d76ec7e775bb944'and intent['runtime']['sha256']=='323d708e69f74557aa2af1e9279fa0e59514a85d6058f59ebd03d139b8619a67'
groups={
 'wind': [('source167-state-after',72),('source167-boundary-after',696),('source167-player-boundary-after',96),('source167-io-after',144),('source167-player-io-after',48),('source167-faults-after',48),('source167-retained-prior-prior-retained-gates',352),('source167-retained-native-own',64),('source167-retained-native-player-io',48),('source167-retained-native-io',144)],
 'intent':[('selected-player-intent-native',76),('selected-intent-source167-player-boundary-after',96),('selected-intent-source167-player-io-after',36),('selected-intent-source167-boundary-after',696),('selected-intent-source167-io-after',144),('selected-intent-source167-faults-after',48)]}
run=D/'source-wind-and-intent-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'editable-input-snapshot';inputs.mkdir(parents=True)
doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';current=S/'docs/maintenance/combat-continuation-current.zh-TW.md'
for p,n in [(proofpath,'previous-proof.json'),(auditpath,'previous-audit.json'),(C,'previous-continuation.json'),(doc,'previous-lifecycle.md'),(current,'previous-current.md')]:shutil.copyfile(p,run/n)
results={};refs={}
for scope,spec in groups.items():
 runtime=(wind if scope=='wind'else intent)['runtime'];results[scope]=[]
 for name,n in spec:
  i=read(D/(name+'-current.json'));verify(i);m=read(Path(i['manifest']['path']));verify(m);o=m.get('observations',m);assert i['status'].startswith('passed')and i['runtime']==runtime and o['cases']==n
  frozen=run/(name+'-current.json');shutil.copyfile(D/frozen.name,frozen);refs[name]=pin(frozen);results[scope].append(dict(name=name,cases=n,known_checks=o.get('checks',o.get('known_checks'))or 0,receipt=pin(frozen),manifest=i['manifest']))
assert sum(x['cases']for x in results['wind'])==1712 and sum(x['cases']for x in results['intent'])==1096
for name in ['source-wind-target-build','source-wind-target-reproduction','source-wind-scope-audit','source167-state-before','selected-player-intent-build','selected-player-intent-reproduction','all-targeted-source-paths']:
 i=read(D/(name+'-current.json'));verify(i);verify(read(Path(i['manifest']['path'])));frozen=run/(name+'-current.json');shutil.copyfile(D/frozen.name,frozen);refs[name]=pin(frozen)
before=read(Path(read(D/'source167-state-before-current.json')['manifest']['path']))['observations'];assert before['cases']==72 and before['Source_controlled_effect_mutations']==66
matrixdata=read(Path(read(D/'all-targeted-source-paths-current.json')['manifest']['path']));assert matrixdata['observations']['rows']==807
for name in ['NativeSourceWindTargets.java','SourceWindTargetMethods.java','SourceWindTargetDelta.java','SourceWindTargetLoad.java','SourceWindScopeAudit.java','build_source_wind_targets.py','reproduce_source_wind_targets.py','verify_source_wind_scope.py','test_source_wind_native.py','version_source_wind_fixtures.py','test_source_wind_retained.py','test_source_wind_prior_native.py','NativeSelectedPlayerIntent.java','SelectedPlayerIntentMethods.java','SelectedPlayerIntentDelta.java','SelectedPlayerIntentLoad.java','build_selected_player_intent.py','reproduce_selected_player_intent.py','SelectedPlayerIntentProbe.java','test_selected_player_intent.py','test_selected_intent_source167.py','TargetParserCallInventory.java','audit_target_source_paths.py','checkpoint_source_wind_and_intent.py']:shutil.copyfile(R/name,inputs/name)
for folder in ['source167-fixtures','selected-intent-fixtures']:
 out=inputs/folder;out.mkdir();basis=read(R/folder/'manifest.json');verify(basis)
 for row in basis['rows']:shutil.copyfile(row['versioned']['path'],out/Path(row['versioned']['path']).name)
 shutil.copyfile(R/folder/'manifest.json',out/'manifest.json')
dll=Path('D:/lineage開發/3-lito開發/lito登入器/LiTo381-Launcher/app/compat/litoddraw.dll');assert sha(dll)=='e47bd6c6ec9979c8747a523e4457133802d19f673c8dc1e67707abfa96216a8a'
remaining=[
 'Full historical34 material/Source semantics remain open; the807 static rows,180 native models and targeted native roots are different denominators. No full-goal or release completion.',
 'Competition selected-player gate is implemented at81 exact parser/decoded-argument ports, with76 native self/friend/enemy/Ctrl cases for7 skills (1/4/26/42/50/76/167). Other skills/modes and all target types require per-skill qualification; WORLD policy was deliberately left unchanged while actual hostility/neutral relationships are unresolved. This is not an all-player-is-enemy rule.',
 'The new gate does not grant permission; Ctrl still passes through original skill kind, Source clan/party, life, death, observer, range, LOS, immunity and room rules. NPC/ArenaBot/pet/summon and Source area secondaries keep their original rules. All older native suites use the previous hostility policy and cannot qualify the new gate.',
 'Source167 PC/controlled primary lifetime and controlled-class failure are scoped: screen-range admission, pre-probability state preservation, pair waits, late native frame retirement and material rollback/retry. Natural NPC range/cost/probability producers and wider Source0-target-list/visual semantics remain open; no claim of complete167 executor semantics.',
 'Source18/29/39/152/153 controlled failure ordering, Source17 area3, other remaining target/range/actor/timer/AI/constructor/freeze zero-negative consumers and complete Source external-control consumers remain open.',
 'Actual client cursor/no-packet versus field packet/server rejection for26 and all professions remains unverified. Formal0.6.71 integration, final same-SHA full regression/startup/Main shutdown and one concentrated client/foreground multiwindow/lobby-return acceptance remain open. No deployment/new baseline/Source or player SQL/WAL writes.'
]
for scope,build in [('wind',wind),('intent',intent)]:
 v=dict(status='passed-scoped-Source167-1712-native-cases-Source-and-new-policy-open'if scope=='wind'else'passed-scoped-competition-selected-player-intent1096-native-cases-WORLD-and-full-matrix-open',runtime=build['runtime'],build=build['manifest'],receipts=refs,native_groups=results[scope],native_cases=sum(x['cases']for x in results[scope]),known_checks=sum(x['known_checks']for x in results[scope]),check_count_complete=False,all_owned_native_fixtures_terminal=True,static_inventory=refs['all-targeted-source-paths'],Source167_before_state_gaps=before,formal_DLL=pin(dll),live_helpers_sealed=False,input_snapshots=[pin(p)for p in inputs.rglob('*')if p.is_file()],remaining=remaining,qualified_for_new_target_intent_policy=False,WORLD_policy_changed=False,all_Source_consumers_wired=False,all_skill_target_coverage_complete=False,full_goal_achieved=False,qualified_for_release=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,new_baseline_created=False,Source_or_player_SQL_or_WAL_written=False)
 name='source-wind-target-progress'if scope=='wind'else'selected-player-intent-progress';path=run/(name+'.json');write(path,v);write(D/(name+'-current.json'),dict(status=v['status'],runtime=build['runtime'],manifest=pin(path),full_goal_achieved=False,qualified_for_release=False))
# Preserve the limited26/42 self diagnostic while enriching static evidence and the seven actually tested rows.
with Path(matrixdata['matrix']['path']).open(encoding='utf-8',newline='')as f:rows=list(csv.DictReader(f,delimiter='\t'))
for row in rows:
 if int(row['skill_id'])in [1,4,26,42,50,76,167]:row['server_evidence']='323d708e69f7: actual selected-PC BATTLE normal/Ctrl admission/cost and7-root effect checks; scoped96 pair/36 player-frame and controlled NPC regression; WORLD/cursor/other modes still pending'
 if int(row['skill_id'])==167:row['result_or_gap']='Source controlled class failure/life/material proofs:df82862d; PC primary explicit intent scope:323d708e; natural actors/full semantics/cursor remain open'
with (run/'target-matrix.tsv').open('w',encoding='utf-8',newline='')as f:w=csv.DictWriter(f,fieldnames=list(rows[0]),delimiter='\t');w.writeheader();w.writerows(rows)
inventory=dict(status='807-static-Source-parser-rows-with-limited-seven-skill-native-scope-full-target-cursor-matrix-open',previous=read(D/'all-targeted-source-paths-current.json')['manifest'],matrix=pin(run/'target-matrix.tsv'),native_runtimes_separate=[wind['runtime'],intent['runtime']],rows=807,all_skill_target_coverage_complete=False,full_goal_achieved=False);write(run/'target-matrix-manifest.json',inventory)
lines=current.read_text('utf-8').splitlines();lines=[f'| 本次更新 | Source167同SHA1,712原生案；另競技玩家Ctrl候選同SHA1,096案（7技能76案＋成對／送包／NPC／故障），兩SHA不混加。807列Source與封包路徑補齊靜態定位；WORLD／其餘材料與完整Source／實際游標及正式整合仍開放。未部署。 |'if l.startswith('| 本次更新 |')else l for l in lines];at=next(i for i,l in enumerate(lines)if l.startswith('| 76主目標計算與範圍buff顯示 |'))
lines[at+1:at+1]=[
 '| 167受控失敗與成對生命週期 | **`df82862d3751…` 同SHA1,712原生案／已計數56,205檢查**；72狀態案重現66個舊消耗／效果差異後歸0，392成對等待、192原生Frame等待、48材料故障重試與Main68／73回歸通過。PC／受控主目標採Source畫面範圍；自然NPC及完整167／Source仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/source-wind-target-progress-current.json)。 |',
 '| 競技玩家主目標明確Ctrl | **`323d708e69f7…` 同SHA1,096原生案／已計數12,955檢查**；81個實際解碼端點、7技能76個自身／友方／敵方正常Ctrl施法；正常合法輔助、敵方需明確意圖、原條件及NPC規則保留。56成對、180原生Frame等待及48材料故障重試通過；WORLD／其餘技能模式／客戶端游標尚未驗收。[收據](../../tools/combat-continuation/integration-lifecycle/build/selected-player-intent-progress-current.json)。 |',
 '| 全技能Source與封包路徑 | 807列加入185列Source符號、48列註冊執行器、180列候選封包解析路徑；234條靜態路徑屬可能呼叫圖，反射、ID分支及逐技能准入仍須驗證，不能當完整覆蓋。[矩陣](../../tools/combat-continuation/integration-lifecycle/build/all-targeted-source-paths-current.json)。 |']
current.write_text('\n'.join(lines)+'\n','utf-8')
doc.write_text(doc.read_text('utf-8')+'\n\nSource167／全部指定技能目標續作：`df82862d3751…`同SHA1,712原生案／已計數56,205檢查，72案重現66個受控NPC狀態消耗差異並修正；保留原一次扣款，Source類型失敗於機率／消魔／睡眠／屏障前返回。補PC與受控主目標的Source畫面範圍、392真實成對等待、192原生同步Frame等待及48材料故障／立即重試；Main68／73與Source76自身及送包回歸通過。只改3個既有class及新增3個bounded cast class，原數值／扣費／其他技能方法完整逆向，相同位元重建、13個class無初始化及非阻塞Frame稽核通過。自然NPC、其餘Source控制／目標／普通建構與計時語意仍待補。\n\n另一份`323d708e69f7…`候選只更動PC／ForcedCombat及一個無狀態helper；在81個真實物件／攻擊解析或已解碼參數端點加入競技選定PC的明確意圖讀取，只讀原request-scoped Ctrl且不建立新TLS／actor名冊。當前房間模式／隊伍規則辨識敵方；不是以另一玩家或同來源當敵我。正常自身／友方輔助沿用原Source類型與血盟／隊伍條件，Ctrl仍不能繞過死亡／旁觀／距離／LOS／免疫等原門檻，NPC／ArenaBot／寵召及範圍次目標保留原規則。同SHA1,096案／已計數12,955檢查（7技能1/4/26/42/50/76/167的76個真實封包與效果案、96PC邊界／36PCFrame／696NPC邊界／144NPCFrame／48材料故障）；56個玩家成對等待、180個原生Frame等待通過。獨立全archive／schema／原方法逆向、相同位元重建及16class無初始化通過。WORLD敵我／中立關係、其他技能模式及實際游標尚未驗收，不能把此SHA當完整新政策或把舊1712／3814案相加。\n\n807列另補Source符號185列／註冊執行器48列／180列原生解析候選、234靜態呼叫路徑；明確標示靜態過度近似、反射與ID條件未證。僅7個已測技能加入限定伺服器證據。通暢氣脈術26仍無實際游標／現場封包證據，不宣稱使用者回報已修復。正式0.6.71JAR／特殊載入DLL／基準未動，Source與玩家SQL／WAL寫入0；沒有部署或新基準。收據：`source-wind-target-progress-current.json`、`selected-player-intent-progress-current.json`及`all-targeted-source-paths-current.json`。\n','utf-8')
audit.update(previous=pin(run/'previous-audit.json'),pending_source_wind_target=pin(run/'source-wind-target-progress.json'),pending_selected_player_intent=pin(run/'selected-player-intent-progress.json'),all_targeted_source_paths=pin(run/'target-matrix-manifest.json'));write(auditpath,audit)
proof.update(status='integrated1206-preserved-separate1712-Source167-and1096-competition-PC-intent-full-Source-open',previous_progress=pin(run/'previous-proof.json'),pending_source_wind_target=pin(run/'source-wind-target-progress.json'),pending_selected_player_intent=pin(run/'selected-player-intent-progress.json'),all_targeted_source_paths=pin(run/'target-matrix-manifest.json'),audit=pin(auditpath),current_audit=pin(auditpath),docs=[pin(doc),pin(current)]);write(proofpath,proof)
following=dict(continuation);following.update(source_wind_target_progress=pin(D/'source-wind-target-progress-current.json'),selected_player_intent_progress=pin(D/'selected-player-intent-progress-current.json'),all_targeted_source_paths=pin(D/'all-targeted-source-paths-current.json'));write(C,following);assert {k:v for k,v in read(C).items()if k not in keys}==continuation
for p,h in anchors.items():assert sha(p)==h
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
print(json.dumps(dict(status='checkpointed-separate1712-and1096-native-scopes',wind_native=1712,intent_native=1096,full_goal_achieved=False,formal_unchanged=True,manifest_directory=str(run))))

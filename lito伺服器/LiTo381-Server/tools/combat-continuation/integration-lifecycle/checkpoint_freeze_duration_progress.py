"""Record the additional frozen-duration regression without conflating earlier JAR hashes."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil,zipfile,subprocess
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';C=R.parent/'continuation-current.json';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def verify(v):
 if isinstance(v,dict):
  if 'path'in v and 'sha256'in v:assert sha(Path(v['path']))==v['sha256'],v['path']
  for x in v.values():verify(x)
 elif isinstance(v,list):
  for x in v:verify(x)
proof=read(D/'Source-lifecycle-progress-verified-current.json');audit=read(D/'source-summon-lifetime-audit-current.json');continuation=read(C)
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
assert 'freeze_duration_progress'not in continuation
anchors={p:sha(p)for p in [R/'current.json',D/'static-respawn-progress-current.json',D/'freeze-accepted-progress-current.json']};prior=read(D/'freeze-accepted-progress-current.json');verify(prior);build=read(D/'freeze-accepted-build-current.json');verify(build);runtime=build['runtime'];assert runtime['sha256']=='f4d1e83ab9a14ff482f4f8f0cdee9938e188c5689306a7a9288c0d3c22b4f6f5'
bm=read(Path(build['manifest']['path']));verify(bm)
for ref in bm['inputs']:assert sha(R/Path(ref['path']).name)==ref['sha256']
names=['freeze-accepted-duration-after','freeze-accepted-stage','freeze-accepted-boundaries','freeze-accepted-completion','freeze-accepted-IO-after','freeze-accepted-relations']+['freeze-accepted-retained-'+n for n in ['hp','hp-main','shock-mutation','shock-publication','shock-death','gates','area']]+['freeze-accepted-NPC-originals'];cases=checks=0;rows=[]
for name in names:
 i=read(D/(name+'-current.json'));verify(i);assert i['runtime']==runtime and i['status'].startswith('passed');m=read(Path(i['manifest']['path']));verify(m);o=m.get('observations',m);cases+=o['cases'];checks+=o.get('checks',o.get('known_checks'))or 0;rows.append(dict(name=name,cases=o['cases'],manifest=i['manifest']))
assert(cases,checks)==(1074,51174)
before=read(D/'freeze-accepted-duration-before-current.json');verify(before);assert before['runtime']==prior['runtime'];bo=read(Path(before['manifest']['path']))['observations'];assert bo['cases']==8 and bo['wrong_snapshot_duration_cases']==8
after=read(Path(read(D/'freeze-accepted-duration-after-current.json')['manifest']['path']))['observations'];assert after['wrong_snapshot_duration_cases']==0
repro=read(D/'freeze-accepted-reproduction-current.json');verify(repro);assert repro['runtime']==runtime and repro['status'].startswith('passed-byte-identical');verify(read(Path(repro['manifest']['path'])))
with zipfile.ZipFile(prior['runtime']['path'])as a,zipfile.ZipFile(runtime['path'])as b:
 assert a.namelist()==b.namelist();changed=[n for n in a.namelist()if a.read(n)!=b.read(n)]
assert changed==['local/lito381/server/ClientSession.class','local/lito381/server/NativeAcceptedFreezeMutation$Batch.class','local/lito381/server/NativeAcceptedFreezeMutation.class']
query="@(Get-CimInstance Win32_Process -Filter \"Name = 'java.exe'\" | Where-Object {$_.CommandLine -like '*integration-lifecycle*'}).Count";assert subprocess.run(['powershell','-NoProfile','-Command',query],capture_output=True,text=True,check=True).stdout.strip()=='0'
run=D/'freeze-duration-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'editable-input-snapshot';inputs.mkdir(parents=True);refs={}
for name in names+['freeze-accepted-build','freeze-accepted-duration-before','freeze-accepted-reproduction','source-freeze-accepted-oracle']:
 p=run/(name+'-current.json');shutil.copyfile(D/p.name,p);refs[name]=pin(p)
for name in ['NativeAcceptedFreezeMutation.java','FreezeAcceptedMethods.java','FreezeAcceptedDelta.java','FreezeAcceptedDurationProbe.java','build_freeze_accepted.py','reproduce_freeze_accepted.py','test_freeze_accepted_duration_before.py','test_freeze_accepted_duration.py','checkpoint_freeze_duration_progress.py']:shutil.copyfile(R/name,inputs/name)
doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';current=S/'docs/maintenance/combat-continuation-current.zh-TW.md'
for p,name in [(D/'Source-lifecycle-progress-verified-current.json','previous-proof.json'),(D/'source-summon-lifetime-audit-current.json','previous-audit.json'),(C,'previous-continuation.json'),(doc,'previous-lifecycle.md'),(current,'previous-current.md')]:shutil.copyfile(p,run/name)
remaining=read(Path(prior['manifest']['path']))['remaining'];remaining[1]=remaining[1].replace('full Source timer/addMagicList/effect ordering','full Source timer/addMagicList/effect ordering beyond the now-frozen positive PC50 duration')
v=dict(status='passed-scoped-positive-PC50-frozen-duration-and-accepted-lifecycle-full-Source-open',runtime=runtime,build=build['manifest'],previous_PC50_scope=prior['manifest'],receipts=refs,native_groups=rows,qualified_scope=dict(native_cases=cases,known_checks=checks,check_count_complete=False,new_duration_cases=8,before_wrong_duration_cases=8,after_wrong_duration_cases=0,original_status_factory_and_paralysis_duration_reads=6,captured_in_same_existing_plan=True,Source_fragment_cases=320,Source_fragment_cases_included=False),relative_archive_changed=changed,all_other_archive_entries_identical=True,original_method_and_replay_inverse_passed=True,byte_identical_reproduction_passed=True,input_snapshots=[pin(p)for p in inputs.iterdir()],live_helpers_sealed=False,remaining=remaining,current_external_blockers=[],all_owned_native_fixtures_terminal=True,full_goal_achieved=False,same_SHA_full_regression_passed=False,all_Source_consumers_wired=False,qualified_for_release=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
write(run/'manifest.json',v);write(D/'freeze-duration-progress-current.json',dict(status=v['status'],runtime=runtime,manifest=pin(run/'manifest.json'),full_goal_achieved=False,qualified_for_release=False))
text=doc.read_text('utf-8');marker='\n\nPC50正傷害接受後狀態續作';at=text.index(marker);tail=text[at:];assert tail.count(prior['runtime']['sha256'][:12])==1;tail=tail.replace(prior['runtime']['sha256'][:12],runtime['sha256'][:12]).replace('1,066原生案／已計數51,001項檢查','1,074原生案／已計數51,174項檢查').replace('新增192案含','新增200案含').replace('原HP26＋Main2','另8案在HP等待期間把記憶體技能列期限7秒重載為1秒，錯讀8→0；狀態、特效及原麻痺封包統一採同一份已捕捉期限。原HP26＋Main2').replace('integration-lifecycle/build/freeze-accepted-progress-current.json','integration-lifecycle/build/freeze-duration-progress-current.json');doc.write_text(text[:at]+tail,'utf-8')
lines=current.read_text('utf-8').splitlines();lines=[l.replace(prior['runtime']['sha256'][:12],runtime['sha256'][:12]).replace('1,066原生案','1,074原生案').replace('保留友敵／Ctrl及Main68／73；','期限重載錯讀8→0，保留友敵／Ctrl及Main68／73；').replace('build/freeze-accepted-progress-current.json','build/freeze-duration-progress-current.json')if l.startswith('| PC50正傷害接受後冰凍／消魔與最終送包 |')else f'| 本次更新 | PC50接受後／原生送包與固定期限限定驗證：同SHA1,074原生案＋320獨立Source片段，HP等待期間期限7→1秒錯讀8→0。下一步PC80／194及其餘材料／Source；正式0.6.71未變，尚未到集中實測／發布／新基準門檻。 |'if l.startswith('| 本次更新 |')else l for l in lines];current.write_text('\n'.join(lines)+'\n','utf-8')
audit.update(previous=pin(run/'previous-audit.json'),pending_freeze_duration=pin(run/'manifest.json'));write(D/'source-summon-lifetime-audit-current.json',audit);proof.update(status='passed-integrated1206-unchanged-plus-separate1074-positive-PC50-frozen-duration-scope',previous_progress=pin(run/'previous-proof.json'),pending_freeze_duration=pin(run/'manifest.json'),audit=pin(D/'source-summon-lifetime-audit-current.json'),current_audit=pin(D/'source-summon-lifetime-audit-current.json'),docs=[pin(doc),pin(current)]);write(D/'Source-lifecycle-progress-verified-current.json',proof);following=dict(continuation);following['freeze_duration_progress']=pin(D/'freeze-duration-progress-current.json');write(C,following);assert{k:v for k,v in read(C).items()if k!='freeze_duration_progress'}==continuation
for p,h in anchors.items():assert sha(p)==h
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
write(D/'freeze-accepted-wip-current.json',dict(status='checkpointed-PC50-frozen-duration-full-Source-open',runtime=runtime,progress=pin(D/'freeze-duration-progress-current.json'),remaining=remaining,full_goal_achieved=False,qualified_for_release=False))
print(json.dumps(dict(status=v['status'],native_cases=cases,known_checks=checks,formal_unchanged=True,full_goal_achieved=False)))

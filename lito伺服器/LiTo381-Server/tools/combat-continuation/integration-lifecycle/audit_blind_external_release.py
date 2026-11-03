"""Readonly reconciliation of a separately published formal runtime. Does not deploy, restore or modify it."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil,zipfile
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';L=Path('D:/lineage開發/3-lito開發/lito登入器/LiTo381-Launcher')
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
proof=read(D/'Source-lifecycle-progress-verified-current.json');candidate=read(D/'blind-scene-build-current.json')['runtime'];changed=[r for r in proof['protected']if sha(Path(r['path']))!=r['sha256']];assert len(changed)==1;prior=changed[0];formal=L/'app/server/LiTo381-SharedResources-candidate.jar';assert Path(prior['path']).resolve()==formal.resolve()and prior['sha256']=='75da69b0d2f7f9d394ee6bf2fdb396b153c75d9b938069b0999ed8dac4f50371'
assert sha(formal)=='e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498'and sha(formal)!=candidate['sha256'];release=L/'docs/maintenance/release-0.6.71.json';record=read(release);assert record['version']=='0.6.71'and record['status']=='published-and-verified'and record['build']['candidate_sha256']==sha(formal)
inputs=S/'deployment/release-inputs.json';assert read(inputs)['server_sha256']==sha(formal);assert next(r for r in record['runtime_and_evidence']if Path(r['path']).resolve()==formal.resolve())['sha256']==sha(formal)
with zipfile.ZipFile(formal)as z:assert all('local/lito381/server/'+n+'.class'not in z.namelist()for n in ['NativeBlindSceneAction','NativeBlindSourceState'])
run=D/'blind-external-formal-release-audit'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);refs=[]
for p in [release,inputs,L/'README.zh-TW.md']:
 q=run/p.name;shutil.copyfile(p,q);refs.append(dict(original=pin(p),frozen=pin(q)))
for ref in proof['protected']:
 if ref!=prior:assert sha(Path(ref['path']))==ref['sha256']
v=dict(status='readonly-reconciled-separate-formal0.6.71-release-not-this-candidate',prior_protected=prior,current_protected=pin(formal),runtime=candidate,release_evidence=refs,release_version='0.6.71',published_at=record['at'],formal_missing_this_candidates_two_new_helpers=True,other_four_protected_inputs_unchanged=True,action='Read only; no formal file, release input, Source/player SQL or WAL write, restore, publish or launch. The prior75da anchor is retained here and in the previous proof; subsequent current-state protection records the independently evidenced e149 formal runtime.',formal_runtime_changed_externally=True,formal_deployment_changed_by_this_task=False,Source_or_player_SQL_or_WAL_written_by_this_task=False,full_goal_achieved=False);write(run/'manifest.json',v);write(D/'blind-external-formal-release-audit-current.json',dict(status=v['status'],runtime=candidate,manifest=pin(run/'manifest.json')));print(json.dumps(dict(status=v['status'],formal_sha256=sha(formal),candidate_deployed=False)))

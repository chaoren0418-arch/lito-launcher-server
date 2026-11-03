"""Pin actual earlier final-viewer failures and current closed/native output scope without completion claims."""
from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,csv,collections,shutil
R=Path(__file__).resolve().parent;D=R/'build'
def read(p):return json.loads(Path(p).read_text('utf-8'))
def pin(p):
 p=Path(p).resolve();return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def checked(ref):assert pin(ref['path'])==ref;return Path(ref['path'])
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
new=read(D/'source-self-final-frame-build-current.json');m=read(checked(new['manifest']));checked(new['runtime']);assert m['pure_self_final_frame_contract_scenarios']==324
gap=read(D/'source-self-final-frame-gap-current.json');g=read(checked(gap['manifest']));assert g['runtime']==m['base'];assert len(g['results'])==3
for result in g['results']:
 s=checked(result['log']).read_text('utf-8');assert 'actual73 final viewer rejects stale role/status/life/Main before cipher'in s and ('bytes=8'if result['mode']=='role'else'bytes=17')in s
with checked(m['material_native_ingress_outcomes']).open(encoding='utf-8')as f:rows=list(csv.DictReader(f,delimiter='\t'))
assert len(rows)==232 and all(not r['cleanup_error']for r in rows)
closed=[r for r in rows if r['mode']in {'Main','STOPPED'}];assert len(closed)==116 and all(r['status']=='rejected-or-no-output' and not r['error_type']and r['encrypted_memory_bytes']=='0' and r['remaining_mp']=='2000'for r in closed)
positive=[r for r in rows if r['skill_id']=='73'and r['mode']in {'peer','self'}];assert len(positive)==2 and all(r['remaining_mp']=='1806'and r['remaining_material']=='20'and r['encrypted_memory_bytes']=='225'and not r['error_type']for r in positive)
errors=[r for r in rows if r['mode']in {'peer','self'}and r['error_type']]
run=D/'source-self-final-outcomes'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);shutil.copyfile(Path(__file__),inputs/Path(__file__).name)
with (run/'normal-unqualified-errors.tsv').open('w',encoding='utf-8',newline='')as f:w=csv.DictWriter(f,fieldnames=list(rows[0]),delimiter='\t');w.writeheader();w.writerows(errors)
v=dict(status='verified-self-final-native73-role-life-Main-output-and-all58-closed-ingress-only',runtime=new['runtime'],candidate_build=new['manifest'],original_final_viewer_failure=gap['manifest'],original_failure_logs=[r['log']for r in g['results']],current_material_outcomes=m['material_native_ingress_outcomes'],inputs=[pin(p)for p in inputs.iterdir()],current_closed_material_ingress=116,native73_positive_frame_bytes=225,original_native73_viewer_observer_waits=32,detached_original_final_waits=5,original_native73_source_lock_waits=6,normal_unqualified_error_count=len(errors),normal_error_types=dict(collections.Counter(r['error_type']for r in errors)),artifacts=[pin(run/'normal-unqualified-errors.tsv')],scope='First self ingress/role/life and declared real memory final-receiver waits only. Generic non-self/area paths, all skill effects and actual Source/Main/backend/MySQL/cursor remain open. Historical SQL costs and native durations are fixture data.',full_material_semantics_qualified=False,full_Source_semantics_qualified=False,actual_Ctrl_or_cursor_qualified=False,new_SQL_connections=0,formal_deployment_changed=False,full_goal_achieved=False)
write(run/'manifest.json',v);write(D/'source-self-final-outcomes-current.json',dict(status=v['status'],runtime=new['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],normal_unqualified_errors=len(errors),manifest=pin(run/'manifest.json')),ensure_ascii=False))

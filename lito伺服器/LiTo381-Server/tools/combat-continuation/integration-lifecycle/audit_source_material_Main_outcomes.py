"""Compare frozen failed Main ingress to current results; keep normal fixture/semantic gaps open."""
from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,csv,collections,shutil
R=Path(__file__).resolve().parent;D=R/'build'
def read(p):return json.loads(Path(p).read_text('utf-8'))
def pin(p):
 p=Path(p).resolve();return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def checked(ref):assert pin(ref['path'])==ref;return Path(ref['path'])
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def rows(ref):
 with checked(ref).open(encoding='utf-8')as f:return list(csv.DictReader(f,delimiter='\t'))
new=read(D/'source-material-Main-build-current.json');m=read(checked(new['manifest']));checked(new['runtime']);assert m['pure_material_Main_contract_scenarios']==144
old=read(D/'source-material-broad-ingress-current.json');o=read(checked(old['manifest']));assert old['runtime']==m['base'];old_results=next(x for x in o['artifacts']if Path(x['path']).name=='results.tsv')
before=rows(old_results);after=rows(m['material_native_ingress_outcomes']);assert len(before)==len(after)==232
assert {(x['skill_id'],x['mode'])for x in before}=={(x['skill_id'],x['mode'])for x in after}
closed=[x for x in after if x['mode']in {'Main','STOPPED'}];assert len(closed)==116
assert all(x['status']=='rejected-or-no-output' and x['remaining_mp']=='2000' and x['encrypted_memory_bytes']=='0' and not x['error_type'] and not x['cleanup_error']for x in closed)
assert all(not x['cleanup_error']for x in after)
failed=[x for x in before if x['mode']=='Main' and x['error_type']];assert len(failed)==32
f73=next(x for x in failed if x['skill_id']=='73');assert f73['remaining_mp']=='1806' and f73['remaining_material']=='20' and int(f73['encrypted_memory_bytes'])>0
positive=[x for x in after if x['skill_id']=='73' and x['mode']in {'peer','self'}];assert len(positive)==2 and all(x['status']=='cost-observed' and x['remaining_mp']=='1806' and x['remaining_material']=='20' and not x['error_type']for x in positive)
normal=[x for x in after if x['mode']in {'peer','self'}];unresolved=[x for x in normal if x['error_type']]
run=D/'source-material-Main-outcomes'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);shutil.copyfile(Path(__file__),inputs/Path(__file__).name)
with (run/'normal-unqualified-errors.tsv').open('w',encoding='utf-8',newline='')as f:
 w=csv.DictWriter(f,fieldnames=list(after[0]),delimiter='\t');w.writeheader();w.writerows(unresolved)
with (run/'Main-before-after.tsv').open('w',encoding='utf-8',newline='')as f:
 fields=['skill_id','prior_status','prior_mp','prior_material','prior_bytes','prior_error','current_closed_status'];w=csv.DictWriter(f,fieldnames=fields,delimiter='\t');w.writeheader()
 for row in (x for x in before if x['mode']=='Main'):w.writerow(dict(skill_id=row['skill_id'],prior_status=row['status'],prior_mp=row['remaining_mp'],prior_material=row['remaining_material'],prior_bytes=row['encrypted_memory_bytes'],prior_error=row['error_message'],current_closed_status=next(x['status']for x in closed if x['skill_id']==row['skill_id']and x['mode']=='Main')))
v=dict(status='verified-all58-initial-Main-STOPPED-ingress-only-normal-effects-and-final-delivery-open',runtime=new['runtime'],candidate_build=new['manifest'],historical_failed_ingress=old['manifest'],historical_results=old_results,current_results=m['material_native_ingress_outcomes'],material_cases=m['material_cases'],inputs=[pin(p)for p in inputs.iterdir()],prior_Main_failures=32,closed_current_ingress=116,native73_positive_controls=2,normal_ingress_attempts=116,normal_unqualified_error_count=len(unresolved),normal_outcomes=dict(collections.Counter(r['status']for r in normal)),normal_error_types=dict(collections.Counter(r['error_type']for r in unresolved)),artifacts=[pin(run/'Main-before-after.tsv'),pin(run/'normal-unqualified-errors.tsv')],scope='Historical reagent/MP snapshot and native model durations are detached fixture data, not current MySQL qualification. Cost observed is not proof of accepted effect. Main ingress and source-lock checks do not qualify final viewer/observer delivery or actor life/Source semantics.',full_material_semantics_qualified=False,full_Source_semantics_qualified=False,final_receiver_qualified=False,actual_Ctrl_or_cursor_qualified=False,new_SQL_connections=0,formal_deployment_changed=False,full_goal_achieved=False)
write(run/'manifest.json',v);write(D/'source-material-Main-outcomes-current.json',dict(status=v['status'],runtime=new['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],prior_Main_failures=32,current_closed_ingress=116,normal_unqualified_errors=len(unresolved),manifest=pin(run/'manifest.json')),ensure_ascii=False))

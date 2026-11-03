"""Append verified same-program decoded Ctrl evidence without changing runtime or completion gates."""
from pathlib import Path
from datetime import datetime,timezone
import copy,csv,hashlib,json,re,shutil
R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2];C=R.parent/'continuation-current.json'
read=lambda p:json.loads(Path(p).read_text('utf-8'))
sha=lambda p:hashlib.sha256(Path(p).read_bytes()).hexdigest()
pin=lambda p:dict(path=str(Path(p).resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def checked(ref):assert pin(ref['path'])==ref,ref['path'];return Path(ref['path'])
def rows(ref):
 with checked(ref).open(encoding='utf-8')as f:return list(csv.DictReader(f,delimiter='\t'))
index=D/'source-program-update-progress-current.json';previous=read(index);old=read(checked(previous['manifest']));checked(previous['runtime']);assert previous['runtime']['sha256']=='c4d8bdaf436050e7ea9e64bf1702256e7b529a67892b0c03b10baa32c695a1b6'
ingress_ref=read(D/'source-material-control-ingress-current.json');life_ref=read(D/'source-material-control-lifecycle-current.json')
for ref in [ingress_ref,life_ref]:assert ref['runtime']==previous['runtime'];checked(ref['manifest'])
ingress=read(ingress_ref['manifest']['path']);life=read(life_ref['manifest']['path'])
for m in [ingress,life]:
 for ref in m['inputs']+m['artifacts']:checked(ref)
ingress_rows=rows(next(x for x in ingress['artifacts']if Path(x['path']).name=='results.tsv'));life_rows=rows(next(x for x in life['artifacts']if Path(x['path']).name=='results.tsv'))
assert len(ingress_rows)==464 and len(life_rows)==42
assert all(not r['error_type']and not r['cleanup_error']for r in ingress_rows+life_rows)
assert len([r for r in ingress_rows if r['status']=='ctrl-not-object-layout'])==87
closed=[r for r in ingress_rows if r['mode']in ('Main','STOPPED')];assert len(closed)==116 and all(r['remaining_mp']=='2000'and r['encrypted_memory_bytes']=='0'and r['status']=='rejected-or-no-output'for r in closed)
life_log=checked(next(x for x in life['artifacts']if Path(x['path']).name=='probe.log')).read_text('utf-8');assert 'PAIR LIFECYCLE checks=312, actual-decoded-Ctrl-pair-waits=42'in life_log
assert {r['skill_id']for r in life_rows}=={'27','173','174'}
for r in life_rows:
 accepted=r['mode']in ('pair:normal','pair:DRAINING');assert r['status']==('cost-observed'if accepted else'rejected-or-no-output')
 if not accepted:assert r['remaining_mp']=='2000'and r['encrypted_memory_bytes']=='0'
protected=old['protected']+old['additional_protected']+[pin(R/'current.json')]
for ref in protected:checked(ref)
run=D/'source-completion-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');snapshot=run/'editable-input-snapshot';snapshot.mkdir(parents=True)
proofpath=D/'Source-lifecycle-progress-verified-current.json';auditpath=D/'source-summon-lifetime-audit-current.json';proof=read(proofpath);audit=read(auditpath)
current=S/'docs/maintenance/combat-continuation-current.zh-TW.md';doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';work=S/'docs/maintenance/source-completion-work-current.zh-TW.md';detail=S/'docs/maintenance/source-material-control-ingress.zh-TW.md'
for p,name in [(index,'previous-program-progress.json'),(proofpath,'previous-proof.json'),(auditpath,'previous-audit.json'),(C,'previous-continuation.json'),(current,'previous-current.md'),(doc,'previous-lifecycle.md'),(work,'previous-work.md'),(detail,'previous-control-detail.md')]:shutil.copyfile(p,run/name)
old_index=pin(index);frozen_index=pin(run/'previous-program-progress.json');editable={Path(x['path']).name:x for x in old['editable_inputs']}
for name in ['SourceMaterialControlIngressPlan.java','probe_source_material_control_ingress.py','SourceMaterialControlLifecyclePlan.java','probe_source_material_control_lifecycle.py','SourceMaterialGateTrace.java','probe_source_material_target_gate.py','record_source_material_control_coverage.py','verify_source_mass_teleport_exit_progress.py']:
 shutil.copyfile(R/name,snapshot/name);editable[name]=pin(snapshot/name)
v=copy.deepcopy(old);v.update(status='program-updated-material69-and-verified-decoded-Ctrl-pair-paths-full-Source-open',previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=list(editable.values()),decoded_material_control_ingress=ingress_ref['manifest'],decoded_material_control_pair_lifecycle=life_ref['manifest'])
v['program_checks'].update(additional_decoded_material_Ctrl_lifecycle_checks=312,additional_decoded_Ctrl_original_pair_waits=42,decoded_normal_Ctrl_material_ingress_attempts=464,Ctrl_non_object_layouts_explicitly_unrepresentable=87,Ctrl_request_authority_restored=True,Ctrl_field_decoded_only_for_object_request_parsers=True,full_material_effects_qualified=False)
v['implemented_scope']=[s for s in v['implemented_scope']if not s.startswith('Additional unchanged-candidate decoded-request evidence:')]
v['implemented_scope'].append('Additional unchanged-candidate decoded-request evidence:58 material rows/464 normal,self,Ctrl,enemy,native-Honor-friendly,Main,STOPPED attempts;87 non-object Ctrl layouts explicitly not qualified.312 assertions/42 actual Ctrl pair waits on27,173,174 retain exact normal/DRAINING cost and reject stale role/status/life/lifetime/application/Main without outputs or authority renewal. This supplements the16103 ordinary common-build checks, not full Source or actual cursor qualification.')
for g in v['remaining_work_groups']:
 for marker in [' Decoded normal/Ctrl/enemy/Honor-friendly464',' Actual Ctrl request wrappers now cover42']:
  if marker in g['work']:g['work']=g['work'].split(marker,1)[0]
 if g['id']=='materials':g['work']+=' Decoded normal/Ctrl/enemy/Honor-friendly464 calls now run without unexpected failures;87 non-object Ctrl cases are explicit not-applicable layouts. Complete effects and Source intent fidelity remain open.'
 if g['id']=='paired-lifecycle':g['work']+=' Actual Ctrl request wrappers now cover42 original peer waits on27/173/174 with312 additional assertions; all other role/NPC/area paths remain open.'
v['remaining']=[g['work']for g in v['remaining_work_groups']];v['historical_remaining_preserved_in']=previous['manifest']
for k in ['all_material_skills_complete','all_Source_consumers_wired','same_SHA_full_regression_passed','actual_client_cursor_verified','user_report_fixed','full_goal_achieved','qualified_for_release','formal_deployment_changed','Source_or_player_SQL_or_WAL_written']:assert v[k]is False
assert len(v['remaining_work_groups'])==12
write(run/'manifest.json',v);write(index,dict(previous,status=v['status'],manifest=pin(run/'manifest.json')))
note='\n\n追加decoded-request驗證：58材料列464次normal／Ctrl／self／enemy／Honor同隊／Main／STOPPED入口無未預期例外；87個非物件Ctrl layout明確不適用。27／173／174另有42次原Ctrl成對等待、312斷言，切角／status／life／lifetime／application／Main退役不扣款或送包，DRAINING保留。此為同候選附加證據；完整效果、Source意圖及實機仍未完成。'
w=work.read_text('utf-8').replace(note+'\n\n','');at=w.index('## 尚未完成與完成門檻');work.write_text(w[:at]+note+'\n\n'+w[at:],'utf-8')
for p in [current,doc]:
 text=re.sub(re.escape(note)+r' 證據：[^\n]*\n','',p.read_text('utf-8'));p.write_text(text+note+f" 證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n",'utf-8')
text=detail.read_text('utf-8')
for name,ref in [('source-material-control-ingress',ingress_ref),('source-material-control-lifecycle',life_ref)]:
 text=re.sub('('+name+r'/)[0-9TZ]+(?=/manifest.json)',lambda m:m[1]+Path(ref['manifest']['path']).parent.name,text)
detail.write_text(text,'utf-8')
def freeze(obj):
 if isinstance(obj,dict):
  if obj.get('path')==old_index['path']and obj.get('sha256')==old_index['sha256']:obj.update(frozen_index)
  else:
   for x in obj.values():freeze(x)
 elif isinstance(obj,list):
  for x in obj:freeze(x)
continuation=read(C);freeze(continuation);fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='material69-exit-and-additional-decoded-Ctrl-pair-evidence',full_goal_achieved=False)
continuation['source_mass_teleport_exit_progress']=fresh;continuation['source_material_control_progress']=fresh;proof['source_program_update_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['docs']=[pin(p)for p in [current,doc,work,S/'docs/maintenance/source-mass-teleport-exit.zh-TW.md',detail]]
write(C,continuation);write(proofpath,proof);write(auditpath,audit)
for ref in protected:checked(ref)
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=protected,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],additional_Ctrl_checks=312,decoded_entry_attempts=464,manifest=pin(run/'manifest.json')),ensure_ascii=False))

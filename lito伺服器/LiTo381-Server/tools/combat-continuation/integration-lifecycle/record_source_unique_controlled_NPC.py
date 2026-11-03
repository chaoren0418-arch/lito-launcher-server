"""Supersede duplicate-ID fixture outcomes with exact-runtime unique native actor evidence."""
from pathlib import Path
from datetime import datetime,timezone
import copy,csv,hashlib,json,shutil
R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2];C=R.parent/'continuation-current.json'
read=lambda p:json.loads(Path(p).read_text('utf-8'))
sha=lambda p:hashlib.sha256(Path(p).read_bytes()).hexdigest()
pin=lambda p:dict(path=str(Path(p).resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def checked(v):assert pin(v['path'])==v,v['path'];return Path(v['path'])
index=D/'source-program-update-progress-current.json';previous=read(index);old=read(checked(previous['manifest']));checked(previous['runtime'])
assert previous['runtime']['sha256']=='e7701e624f503b7078714efc346c5481e6778acd6dd1ce5ce9e2399fa773bc4a'
evidence=read(D/'source-unique-controlled-NPC-current.json');assert evidence['runtime']==previous['runtime'];m=read(checked(evidence['manifest']));assert m['status']=='passed-unique-controlled-NPC-native-pair-final-and-material-ingress'
for ref in m['inputs']+[m['log']]:checked(ref)
all_rows={}
for file,expected in [('owner-pair-results.tsv',60),('final-results.tsv',78),('elemental-material-results.tsv',464)]:
 ref=m['outcomes'][file]['artifact']
 with checked(ref).open(encoding='utf-8')as f:rows=list(csv.DictReader(f,delimiter='\t'))
 assert len(rows)==expected and all(not r['error_type']and not r['cleanup_error']for r in rows)
 all_rows[file]=rows
assert m['new_final_assertions']==695 and m['actual_final_monitor_waits']==78 and m['NPC_pair_assertions']==456 and m['actual_NPC_pair_waits']==60
element=all_rows['elemental-material-results.tsv'];closed=[r for r in element if r['mode']in ['Main','STOPPED']]
assert len(closed)==116 and all(r['remaining_mp']=='2000'and r['encrypted_memory_bytes']=='0'for r in closed)
for r in element:
 if r['skill_id']in ['29','152','153']:
  allowed=r['mode']in ['enemy-pet','ctrl-enemy-pet','own-pet','ctrl-own-pet','ctrl-friendly-pet']
  assert r['status']==('cost-observed'if allowed else'rejected-or-no-output')
  assert r['remaining_mp']==(dict({'29':'1983','152':'1980','153':'1990'})[r['skill_id']]if allowed else'2000')
  assert r['remaining_material']==('2'if allowed else'3')
protected=old['protected']+old['additional_protected']+[pin(R/'current.json')]
for ref in protected:checked(ref)
run=D/'source-completion-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');snapshot=run/'editable-input-snapshot';snapshot.mkdir(parents=True)
proofpath=D/'Source-lifecycle-progress-verified-current.json';auditpath=D/'source-summon-lifetime-audit-current.json';proof=read(proofpath);audit=read(auditpath)
current=S/'docs/maintenance/combat-continuation-current.zh-TW.md';doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';work=S/'docs/maintenance/source-completion-work-current.zh-TW.md';detail=S/'docs/maintenance/source-controlled-negative-final.zh-TW.md'
for p,name in [(index,'previous-program-progress.json'),(proofpath,'previous-proof.json'),(auditpath,'previous-audit.json'),(C,'previous-continuation.json'),(current,'previous-current.md'),(doc,'previous-lifecycle.md'),(work,'previous-work.md'),(detail,'previous-controlled-negative.md')]:shutil.copyfile(p,run/name)
old_index=pin(index);frozen_index=pin(run/'previous-program-progress.json');editable={Path(x['path']).name:x for x in old['editable_inputs']}
for name in ['SourceMaterialElementalIngressPlan.java','probe_source_unique_controlled_NPC.py','record_source_unique_controlled_NPC.py','SourceNegativeGeometryTrace.java','SourceNegativeGeometryTraceMethods.java','probe_source_negative_geometry.py']:
 shutil.copyfile(R/name,snapshot/name);editable[name]=pin(snapshot/name)
v=copy.deepcopy(old);v.update(status='program-updated-controlled-NPC-final-with-unique-native-fixtures-full-Source-open',previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=list(editable.values()),unique_controlled_NPC_revalidation=evidence['manifest'])
v['superseded_duplicate_NPC_fixture_evidence']=dict(artifacts=old['controlled_negative_final_outcomes'],replacement=evidence['manifest'],reason='Previous base fixture retained a different owner\'s active NPC123 with object777 while installing real NPC45306 with the same object777. Context proof selected the real actor, but original29/152/153 body scanned the stale earlier actor. Unique actor fixture now revalidates all60 pair waits,78 final waits and464 material outcomes. Do not reuse old NPC material outcome counts as canonical coverage.',production_gap_claim=False,production_selection_or_geometry_changed=False)
v['controlled_negative_final_outcomes']=dict(old['controlled_negative_final_outcomes']);v['controlled_negative_final_outcomes'].update({file:m['outcomes'][file]['artifact']for file in ['final-results.tsv','owner-pair-results.tsv','elemental-material-results.tsv']})
v['program_checks'].update(unique_controlled_NPC_active_ID_assertion=True,unique_controlled_NPC_material_attempts=464,unique_controlled_NPC_material_cost_observations=64,unique_controlled_NPC_material_normal_rejections=146,unique_controlled_NPC_material_layout_skips=248,unique_controlled_NPC_material_cost_free_responses=6,unique_controlled_NPC_closed_ingress_attempts=116,controlled_negative_final_assertions=695,controlled_negative_final_monitor_waits=78,controlled_NPC_pair_assertions=456,controlled_NPC_pair_waits=60)
v['implemented_scope'].append('Same exact candidate revalidated on corrected globally unique actor fixtures: all four roles have inactive distinct primary placeholders, only original-factory45306/object777 is active.60 real NPC owner pair waits/456 assertions,78 real final waits/695 assertions and464 decoded NPC material outcomes pass. Material29/152/153 now reach original cost paths with17/20/10MP and one material for enemy/own/explicit Ctrl-friendly; normal friendly/Main/STOPPED remain inert. Full status/failure/RNG and per-skill lifecycle remain open. Earlier duplicate-ID outcome counts are superseded, not a production missing-entry/geometry defect.')
for g in v['remaining_work_groups']:
 if g['id']=='materials':g['work']+=' Corrected unique real45306 roster now yields464 outcomes:64 cost observations,146 rejections,248 non-NPC layouts and6 cost-free responses;29/152/153 original entries/costs execute. Next verify Source class-failure timing/no mutation/RNG and their distinct paired/final paths; no gate bypass.'
v['remaining']=[g['work']for g in v['remaining_work_groups']];v['historical_remaining_preserved_in']=previous['manifest'];assert len(v['remaining_work_groups'])==12
for k in ['all_material_skills_complete','all_Source_consumers_wired','same_SHA_full_regression_passed','actual_client_cursor_verified','user_report_fixed','full_goal_achieved','qualified_for_release','formal_deployment_changed','Source_or_player_SQL_or_WAL_written']:assert v[k]is False
write(run/'manifest.json',v);write(index,dict(previous,status=v['status'],manifest=pin(run/'manifest.json')))
note='\n\n唯一NPC物件ID重驗：前版場景同時保留舊NPC123與真實元素NPC45306，兩者都為777，造成29／152／153原body先選舊actor並誤報距離；這是測試缺陷，未改生產選取／距離／競技規則。現以四角色各自不同且停用的primary、唯一真實45306重驗，60次成對／456斷言、78次最終等待／695斷言仍通過。464材料入口為64成本觀察、146拒絕、248非NPC封包layout、6無成本回應；116次Main／STOPPED無扣款送包。29／152／153敵方、自有及Ctrl同隊分別保留17／20／10MP和1材料；一般同隊仍拒絕。舊NPC入口統計由本證據取代，完整類別失敗／RNG／狀態／各技能生命週期仍待補齊。'
for p in [current,doc,detail]:p.write_text(p.read_text('utf-8')+note+f' 證據：[{run.name}]({(run/"manifest.json").as_posix()})。\n','utf-8')
w=work.read_text('utf-8');at=w.index('## 尚未完成與完成門檻');work.write_text(w[:at]+note+'\n\n'+w[at:],'utf-8')
def freeze(obj):
 if isinstance(obj,dict):
  if obj.get('path')==old_index['path']and obj.get('sha256')==old_index['sha256']:obj.update(frozen_index)
  else:
   for x in obj.values():freeze(x)
 elif isinstance(obj,list):
  for x in obj:freeze(x)
continuation=read(C);freeze(continuation);fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='controlled-NPC-final-unique-native-actor-fixtures',full_goal_achieved=False)
continuation['source_controlled_negative_final_progress']=fresh;proof['source_program_update_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['docs']=[pin(p)for p in [current,doc,work,detail]]
write(C,continuation);write(proofpath,proof);write(auditpath,audit)
for ref in protected:checked(ref)
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=protected,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')),ensure_ascii=False))

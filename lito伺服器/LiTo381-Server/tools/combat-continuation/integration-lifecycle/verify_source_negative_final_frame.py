"""Verify exact staged final-frame/runtime/cleanup proof without promoting the common program."""
from pathlib import Path
from datetime import datetime,timezone
import csv,hashlib,json,os,re,shutil,subprocess,zipfile
R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2];J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
def read(p):return json.loads(Path(p).read_text('utf-8'))
def pin(p):
 p=Path(p).resolve();return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def checked(v):assert pin(v['path'])==v,v['path'];return Path(v['path'])
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
canonical_index=pin(D/'source-program-update-progress-current.json');canonical=read(canonical_index['path']);progress=read(checked(canonical['manifest']));parent=read(checked(progress['build']))
stage_ref=read(D/'source-negative-final-frame-probe-current.json')['manifest'];stage=read(checked(stage_ref));replay_ref=read(D/'source-negative-final-frame-reproduction-current.json')['manifest'];replay=read(checked(replay_ref));cleanup_ref=read(D/'source-negative-final-cleanup-current.json')['manifest'];cleanup=read(checked(cleanup_ref))
assert canonical['runtime']['sha256']=='bf4aa2a61d488bd68c2cdaa0d9a7df3221f2e7848443f225c584ea137ddcbaf2'
for m in [stage,replay]:
 assert m['runtime']['sha256']=='50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729'and m['base']==canonical['runtime']and m['attempts']==60 and not m['errors']and m['full_regression']and len(m['inherited_regular_plan_runs'])==124
 checked(m['runtime'])
 for ref in m['inputs']+m['artifacts']:checked(ref)
 for filename,count in [('final-results.tsv',60),('self-final-results.tsv',30),('negative-first-results.tsv',150),('owner-pair-results.tsv',60),('PC-pair-results.tsv',42),('decoded-material-results.tsv',464),('elemental-material-results.tsv',464),('controlled-final-results.tsv',78)]:
  with checked(next(x for x in m['artifacts']if Path(x['path']).name==filename)).open(encoding='utf-8')as f:rows=list(csv.DictReader(f,delimiter='\t'))
  assert len(rows)==count and all(not r['error_type']and not r['cleanup_error']for r in rows),filename
 text=checked(next(x for x in m['artifacts']if Path(x['path']).name=='probe.log')).read_text('utf-8')
 for marker in ['34 complete original methods','FINAL LIFECYCLE checks=570','FINAL LIFECYCLE checks=240','PAIR LIFECYCLE checks=1878','NPC FINAL LIFECYCLE checks=695','PAIR LIFECYCLE checks=456','PAIR LIFECYCLE checks=312','PASS193 mass-teleport exit original-lock']:assert marker in text,marker
 assert sum(map(int,re.findall(r'^PASS(\d+) ',text,re.M)))==16103
assert [(Path(x['path']).name,x['sha256'])for x in stage['inputs']]==[(Path(x['path']).name,x['sha256'])for x in replay['inputs']]
assert cleanup['runtime']['sha256']==stage['runtime']['sha256']and cleanup['status'].startswith('passed-')
for ref in cleanup['inputs']+[cleanup['log'],cleanup['javac_args']]:checked(ref)
assert 'PASS17 negative final cleanup memory-only faults;3 cases'in checked(cleanup['log']).read_text('utf-8')
protected=[*progress['protected'],*progress['additional_protected'],pin(R/'current.json')]
for ref in protected:checked(ref)
with zipfile.ZipFile(progress['readonly_SQL_consumers_byte_retained']['base']['path'])as old,zipfile.ZipFile(stage['runtime']['path'])as new:
 for name in progress['readonly_SQL_consumers_byte_retained']['entries']:assert old.read(name)==new.read(name)
 assert not any('GeometryTrace'in n or 'SelfGateDiagnostic'in n or 'CleanupPlan'in n for n in new.namelist())
run=D/'source-negative-final-frame-validation'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for name in ['SourceNegativeFinalFrameLoad.java',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
cp=';'.join(map(str,[run/'tools',Path(canonical['runtime']['path']).parent/'tools',*[checked(x)for x in parent['asm']]]));args=['-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',str(run/'tools'),str(inputs/'SourceNegativeFinalFrameLoad.java')];arg=run/'javac.args';arg.write_text('\n'.join(json.dumps(x,ensure_ascii=False)for x in args),'utf-8');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
with (run/'verify.log').open('w',encoding='utf-8')as out:
 subprocess.run([str(J/'javac.exe'),'@'+str(arg)],env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=45)
 subprocess.run([str(J/'java.exe'),'-Xverify:all','-cp',cp,'SourceNegativeFinalFrameLoad',stage['runtime']['path'],str(S/'lib')],env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=45)
assert pin(canonical_index['path'])==canonical_index
for ref in protected:checked(ref)
v=dict(status='verified-controlled-negative-final-frame-staged-common-integration-open',runtime=stage['runtime'],build=stage_ref,frozen_byte_identical_replay=replay_ref,memory_cleanup_faults=cleanup_ref,canonical_runtime=canonical['runtime'],canonical_program_unchanged=canonical_index,original_methods_independently_inverted=34,original_changed_owners=3,explicit_call_ports=7,complete_original_producer_handles=1,complete_original_producer_siblings=1,transient_Aim_fields=1,new_runtime_classes=3,new_global_roots=0,actual_viewer_final_waits=60,viewer_final_assertions=570,actual153_self_final_waits=30,self_final_assertions=240,cleanup_fault_cases=3,cleanup_fault_assertions=17,producing_Aim_First_viewer_map_and_original_companion_receipts_released_before_final_IO=True,queued_values_contain_no_Aim_Context_Frame_receipt_Member_Scope_Task_collection=True,inherited_negative_first_assertions=1878,inherited_negative_first_waits=150,inherited_ordinary_checks=16103,inherited_regular_plans=124,inherited_NPC_final_waits=78,inherited_NPC_final_assertions=695,inherited_NPC_pair_waits=60,inherited_NPC_pair_assertions=456,inherited_PC_pair_waits=42,inherited_PC_pair_assertions=312,readonly_SQL_consumers_byte_retained=progress['readonly_SQL_consumers_byte_retained'],protected=protected,inputs=[pin(p)for p in sorted(inputs.iterdir())],artifacts=[pin(run/'verify.log'),pin(arg)],full_Source_qualified=False,common_progress_integration_complete=False,new_SQL_connections=0,new_game_runtime_cases=0,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False,full_goal_achieved=False,qualified_for_release=False)
write(run/'manifest.json',v);write(D/'source-negative-final-frame-verification-current.json',dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
write(D/'source-negative-final-frame-pending-integration-current.json',dict(status='verified-staged-final-frames-common-assembly-next',runtime=v['runtime'],verification=pin(run/'manifest.json'),next=['Assemble a self-contained common tools/plan/compiler closure for this runtime, including actual153 post-cost self receiver and cleanup faults.','Run same-SHA inventory/ABI/weapon overlay and isolated checkpoint; preserve6 aliases and12 open work groups.','Continue normal/Ctrl, dead-observer/own-impact/nested/area and remaining Source/material paths; no deployment or SQL writes.'],full_goal_achieved=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')),ensure_ascii=False))

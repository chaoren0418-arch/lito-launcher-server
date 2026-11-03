"""Verify first controlled76/167 primaries and76 secondary identity/RNG exclusion, not final delivery."""
from pathlib import Path
from datetime import datetime,timezone
import csv,hashlib,json,os,re,shutil,subprocess,zipfile
R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2];J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
def read(p):return json.loads(Path(p).read_text('utf-8'))
def pin(p):
 p=Path(p).resolve();return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def checked(v):assert pin(v['path'])==v,v['path'];return Path(v['path'])
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
index=pin(D/'source-program-update-progress-current.json');canonical=read(index['path']);progress=read(checked(canonical['manifest']));parent=read(checked(progress['build']));stage_ref=read(D/'source-area-pair-first-probe-current.json')['manifest'];stage=read(checked(stage_ref));replay_ref=read(D/'source-area-pair-first-reproduction-current.json')['manifest'];replay=read(checked(replay_ref))
assert canonical['runtime']['sha256']=='50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729'
for m in [stage,replay]:
 assert m['runtime']['sha256']=='3ccbb6f5957436cc0ba9bd67d373f9dfcbedad16553009842eaf5db6d0e88e9a'and m['base']==canonical['runtime']and m['attempts']==100 and not m['errors']and m['full_regression']and len(m['inherited_regular_plan_runs'])==124
 checked(m['runtime'])
 for ref in m['inputs']+m['artifacts']:checked(ref)
 for name,count in [('final-results.tsv',100),('secondary-results.tsv',14),('negative-first-results.tsv',150),('negative-final-results.tsv',240),('negative-self-results.tsv',60),('owner-pair-results.tsv',60),('PC-pair-results.tsv',42),('decoded-material-results.tsv',464),('elemental-material-results.tsv',464),('controlled-final-results.tsv',78)]:
  with checked(next(x for x in m['artifacts']if Path(x['path']).name==name)).open(encoding='utf-8')as f:rows=list(csv.DictReader(f,delimiter='\t'))
  assert len(rows)==count and all(not r['error_type']and not r['cleanup_error']for r in rows),name
 text=checked(next(x for x in m['artifacts']if Path(x['path']).name=='probe.log')).read_text('utf-8')
 for marker in ['2573 complete original methods','PAIR LIFECYCLE checks=1402','PAIR LIFECYCLE checks=224','FINAL LIFECYCLE checks=2280','FINAL LIFECYCLE checks=480','PAIR LIFECYCLE checks=1878','NPC FINAL LIFECYCLE checks=695','PAIR LIFECYCLE checks=456','PAIR LIFECYCLE checks=312','PASS17 negative final cleanup memory-only faults;3 cases','PASS193 mass-teleport exit original-lock']:assert marker in text,marker
 assert sum(map(int,re.findall(r'^PASS(\d+) ',text,re.M)))==16120
 rng=re.findall(r'^SECONDARY-RNG (\S+) seed-advances=(\d+)$',text,re.M);assert len(rng)==14 and all(int(count)==(30 if mode in ['normal','DRAINING']else 15)for mode,count in rng)
assert [(Path(x['path']).name,x['sha256'])for x in stage['inputs']]==[(Path(x['path']).name,x['sha256'])for x in replay['inputs']]
protected=[*progress['protected'],*progress['additional_protected'],pin(R/'current.json')]
for ref in protected:checked(ref)
with zipfile.ZipFile(progress['readonly_SQL_consumers_byte_retained']['base']['path'])as old,zipfile.ZipFile(stage['runtime']['path'])as new:
 for name in progress['readonly_SQL_consumers_byte_retained']['entries']:assert old.read(name)==new.read(name)
 assert not any('GeometryTrace'in n or 'CleanupPlan'in n for n in new.namelist())
run=D/'source-area-pair-first-validation'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for name in ['SourceAreaPairFirstLoad.java',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
cp=';'.join(map(str,[run/'tools',Path(canonical['runtime']['path']).parent/'tools',*[checked(x)for x in parent['asm']]]));args=['-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',str(run/'tools'),str(inputs/'SourceAreaPairFirstLoad.java')];arg=run/'javac.args';arg.write_text('\n'.join(json.dumps(x,ensure_ascii=False)for x in args),'utf-8');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
with (run/'verify.log').open('w',encoding='utf-8')as out:
 subprocess.run([str(J/'javac.exe'),'@'+str(arg)],env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=45)
 subprocess.run([str(J/'java.exe'),'-Xverify:all','-cp',cp,'SourceAreaPairFirstLoad',stage['runtime']['path'],str(S/'lib')],env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=45)
assert pin(index['path'])==index
for ref in protected:checked(ref)
v=dict(status='verified-controlled-area-first-primary-and-secondary-staged-common-integration-open',runtime=stage['runtime'],build=stage_ref,frozen_byte_identical_replay=replay_ref,canonical_runtime=canonical['runtime'],canonical_program_unchanged=index,original_methods_independently_inverted=2573,original_changed_owners=6,explicit_call_ports=10,transient_Context_fields=1,new_runtime_classes=3,new_global_roots=0,actual_primary_owner_waits=100,primary_assertions=1402,actual_secondary_waits=14,secondary_assertions=224,stale_secondary_excluded_before_wake_probability_dice_and_percentile=True,original_Source_caster_membership_retained=True,unchanged_caster_and_secondary_seed_advances=30,retired_secondary_caster_only_seed_advances=15,original_cost_retained_when_secondary_retired=True,bounded_first_primary_and_secondary_map_released_by_original_finish=True,inherited_negative_final_viewer_waits=240,inherited_negative_final_self_waits=60,inherited_negative_first_waits=150,inherited_NPC_final_waits=78,inherited_NPC_pair_waits=60,inherited_PC_pair_waits=42,inherited_ordinary_checks=16103,inherited_regular_plans=124,inherited_memory_cleanup_fault_assertions=17,readonly_SQL_consumers_byte_retained=progress['readonly_SQL_consumers_byte_retained'],protected=protected,inputs=[pin(p)for p in sorted(inputs.iterdir())],artifacts=[pin(run/'verify.log'),pin(arg)],area_final_receiver_complete=False,all_secondary_NPC_paths_qualified=False,common_progress_integration_complete=False,full_Source_qualified=False,new_SQL_connections=0,new_game_runtime_cases=0,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False,full_goal_achieved=False,qualified_for_release=False)
write(run/'manifest.json',v);write(D/'source-area-pair-first-verification-current.json',dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
write(D/'source-area-pair-first-pending-integration-current.json',dict(status='verified-staged-area-first-primary-and-secondary-common-assembly-next',runtime=v['runtime'],verification=pin(run/'manifest.json'),next=['Assemble self-contained tools/plan/compiler closure and rerun exact-SHA inventory/ABI/weapon overlay before isolated checkpoint.','Preserve6 historical aliases,12 open groups and all safety gates; no deployment or SQL writes.','Continue independent accepted secondary/final receiver proof, controlled and natural secondary NPCs, PC primaries and remaining Source/material paths.'],full_goal_achieved=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')),ensure_ascii=False))

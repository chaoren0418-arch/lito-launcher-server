"""Verify the staged runtime, exact frozen replay, no-init/DTO graph and formal/data protections."""
from pathlib import Path
from datetime import datetime,timezone
import csv,hashlib,json,os,re,shutil,subprocess,zipfile
R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2];J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
def read(p):return json.loads(Path(p).read_text('utf-8'))
def pin(p):
 p=Path(p).resolve();return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def checked(ref):assert pin(ref['path'])==ref,ref['path'];return Path(ref['path'])
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def refs(v):
 if isinstance(v,dict):
  if 'path'in v and 'sha256'in v:checked(v)
  for x in v.values():refs(x)
 elif isinstance(v,list):
  for x in v:refs(x)
idx=read(D/'source-controlled-negative-final-probe-current.json');repidx=read(D/'source-controlled-negative-final-reproduction-current.json');build=read(checked(idx['manifest']));rep=read(checked(repidx['manifest']));refs(build);refs(rep)
assert build['full_regression']and rep['full_regression']and not build['errors']and not rep['errors']
assert build['runtime']['sha256']==rep['runtime']['sha256']=='e7701e624f503b7078714efc346c5481e6778acd6dd1ce5ce9e2399fa773bc4a'
assert [x['sha256']for x in build['inputs']]==[x['sha256']for x in rep['inputs']]
assert build['attempts']==rep['attempts']==78 and len(build['inherited_regular_plan_runs'])==len(rep['inherited_regular_plan_runs'])==124
for m in [build,rep]:
 log=checked(next(x for x in m['artifacts']if Path(x['path']).name=='probe.log')).read_text('utf-8')
 for marker in ['51 complete original methods,4 original owners,7 explicit call ports','NPC FINAL LIFECYCLE checks=695, actual-decoded-NPC-final-monitor-waits=78','PAIR LIFECYCLE checks=456, actual-decoded-Ctrl-NPC-owner-pair-waits=60','PAIR LIFECYCLE checks=312, actual-decoded-Ctrl-pair-waits=42','PASS193 mass-teleport exit']:assert marker in log,marker
 pass_counts=[int(x)for x in re.findall(r'^PASS(\d+) ',log,re.M)];assert sum(pass_counts)==16103
canonical_index=D/'source-program-update-progress-current.json';canonical_pin=pin(canonical_index);canonical=read(canonical_index);progress=read(checked(canonical['manifest']));assert canonical['runtime']==build['base']
protected=progress['protected']+progress['additional_protected']+[pin(R/'current.json')]
for ref in protected:checked(ref)
sql=progress['readonly_SQL_consumers_byte_retained'];assert len(sql['entries'])==20
with zipfile.ZipFile(checked(sql['base']))as old,zipfile.ZipFile(checked(idx['runtime']))as jar:
 for name in sql['entries']:assert old.read(name)==jar.read(name),name
run=D/'source-controlled-negative-final-validation'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True)
for name in [Path(__file__).name,'SourceControlledNegativeFinalLoad.java']:shutil.copyfile(R/name,run/name)
parent=Path(build['base']['path']);parentbuild=read(checked(progress['build']));cp=';'.join(map(str,[run/'tools',parent.parent/'tools',*[checked(x)for x in parentbuild['asm']]]));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
args=['-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',str(run/'tools'),str(run/'SourceControlledNegativeFinalLoad.java')];argfile=run/'javac.args';argfile.write_text('\n'.join(json.dumps(x,ensure_ascii=False)for x in args),'utf-8')
with (run/'verify.log').open('w',encoding='utf-8')as log:
 for cmd in [[J/'javac.exe','@'+str(argfile)],[J/'java.exe','-Xverify:all','-cp',cp,'SourceControlledNegativeFinalLoad',idx['runtime']['path'],S/'lib']]:subprocess.run(list(map(str,cmd)),env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=60)
assert pin(canonical_index)==canonical_pin
for ref in protected:checked(ref)
rejected=read(D/'source-battle-negative-entry-rejection-current.json')
for item in rejected['mutable_prototypes_to_remove']:assert not Path(item['path']).exists();checked(item['frozen'])
old_experiment=read(D/'source-battle-negative-entry-probe-current.json');old_experiment['status']='rejected-fallback-entry-experiment-do-not-promote';old_experiment['rejection']=pin(D/'source-battle-negative-entry-rejection-current.json');write(D/'source-battle-negative-entry-probe-current.json',old_experiment)
v=dict(status='passed-controlled-negative-final-staged-program-verification-common-integration-open',runtime=idx['runtime'],build=idx['manifest'],frozen_byte_identical_replay=repidx['manifest'],canonical_program_unchanged=canonical_pin,canonical_runtime=canonical['runtime'],original_methods_independently_inverted=51,original_changed_owners=4,explicit_call_ports=7,transient_producing_fields=1,new_runtime_classes=3,original_PC_motion_sender_bytes_retained=True,first_receiver_scope_released_before_final_wait=True,queued_values_no_producing_Context_Frame_receipt_Member_Scope_Task_collection=True,new_final_assertions=695,actual_final_monitor_waits=78,actual_controlled_NPC_owner_pair_waits=60,controlled_NPC_pair_assertions=456,actual_PC_pair_waits=42,PC_pair_assertions=312,inherited_regular_plan_runs=124,inherited_regular_checks=16103,readonly_SQL_readers_byte_retained=sql['entries'],protected=protected,inputs=[pin(run/Path(__file__).name),pin(run/'SourceControlledNegativeFinalLoad.java')],artifacts=[pin(run/'verify.log')],common_progress_integration_complete=False,full_Source_qualified=False,full_goal_achieved=False,qualified_for_release=False,new_SQL_connections=0,new_game_runtime_cases=0,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
write(run/'manifest.json',v);write(D/'source-controlled-negative-final-verification-current.json',dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
write(D/'source-controlled-negative-final-pending-integration-current.json',dict(status='verified-frozen-runtime-common-assembly-and-audits-next',runtime=v['runtime'],verification=pin(run/'manifest.json'),canonical_parent=canonical['runtime'],next=['Assemble a self-contained common tools/plan/compiler closure for this exact runtime and carry its inherited contract census; do not compile old runtime helpers.','Run same-SHA inventory/ABI/weapon overlay and checkpoint the common candidate; preserve six legacy aliases and all12 open work groups.','Continue per-skill NPC/Source target-class/cost/effect semantics; the rejected entry experiment must not be reapplied.'],full_goal_achieved=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')),ensure_ascii=False))

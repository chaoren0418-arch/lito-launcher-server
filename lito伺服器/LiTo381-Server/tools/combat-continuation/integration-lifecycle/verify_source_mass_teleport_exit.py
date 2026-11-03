"""Verify the staged exit candidate, frozen replay, original byte boundaries and data protections."""
from pathlib import Path
from datetime import datetime,timezone
import csv,hashlib,json,os,shutil,subprocess,sys,zipfile
R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2];J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
def read(p):return json.loads(Path(p).read_text('utf-8'))
def pin(p):
 p=Path(p).resolve();return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def checked(ref):assert pin(ref['path'])==ref;return Path(ref['path'])
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def refs(v):
 if isinstance(v,dict):
  if 'path'in v and 'sha256'in v:checked(v)
  for x in v.values():refs(x)
 elif isinstance(v,list):
  for x in v:refs(x)
index=read(D/'source-mass-teleport-exit-probe-current.json');build=read(checked(index['manifest']));refs(build)
replay_path=Path(sys.argv[1]);replay=read(replay_path);refs(replay)
assert build['runtime']==index['runtime'] and build['status']==replay['status']=='passed-cumulative-mass-teleport-exit-program-probe-not-promoted'
assert build['runtime']['sha256']==replay['runtime']['sha256']=='c4d8bdaf436050e7ea9e64bf1702256e7b529a67892b0c03b10baa32c695a1b6'
assert Path(build['runtime']['path'])!=Path(replay['runtime']['path'])
assert [x['sha256']for x in build['inputs']]==[x['sha256']for x in replay['inputs']]
assert len(build['inherited_plan_runs'])==len(replay['inherited_plan_runs'])==124
assert build['new_contract_checks']==replay['new_contract_checks']==193
canonical_index=D/'source-program-update-progress-current.json';canonical_before=pin(canonical_index);canonical=read(canonical_index);progress=read(checked(canonical['manifest']));assert canonical['runtime']==build['parent_runtime']
for k in ['protected','additional_protected']:refs(progress[k])
base=checked(build['parent_runtime']);candidate=checked(index['runtime']);sql=progress['readonly_SQL_consumers_byte_retained'];assert len(sql['entries'])==20
with zipfile.ZipFile(checked(sql['base']))as original,zipfile.ZipFile(candidate)as jar,zipfile.ZipFile(base)as parent:
 for name in sql['entries']:assert original.read(name)==jar.read(name),'readonly SQL reader changed: '+name
 changed={name for name in parent.namelist()if parent.read(name)!=jar.read(name)}
 assert changed=={'local/lito381/server/NativeAreaMaterialAction.class','local/lito381/server/NativeLightAction.class'}
 assert set(jar.namelist())-set(parent.namelist())=={'local/lito381/server/NativeSourceMassTeleportExit.class','local/lito381/server/NativeSourceMassTeleportExit$Proof.class'}
run=D/'source-mass-teleport-exit-validation'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True)
for name in [Path(__file__).name,'SourceMassTeleportExitLoad.java']:shutil.copyfile(R/name,run/name)
parent_build=read(checked(build['parent_build']));cp=';'.join(map(str,[run/'tools',base.parent/'tools',*[checked(x)for x in parent_build['asm']]]));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
args=['-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',str(run/'tools'),str(run/'SourceMassTeleportExitLoad.java')];argfile=run/'javac.args';argfile.write_text('\n'.join(json.dumps(x,ensure_ascii=False)for x in args),'utf-8')
with (run/'verify.log').open('w',encoding='utf-8')as log:
 for cmd in [[J/'javac.exe','@'+str(argfile)],[J/'java.exe','-Xverify:all','-cp',cp,'SourceMassTeleportExitLoad',candidate,S/'lib'],[J/'java.exe','-Xverify:all','-cp',cp,'SourceManualActionLockCensus',candidate,run/'direct-lock-census.tsv']]:subprocess.run(list(map(str,cmd)),env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=60)
log=(run/'verify.log').read_text('utf-8');assert 'PASS404 inherited runtime no-init coverage plus13 explicit self ingress/final owners'in log and 'plus4 explicit mass-teleport exit owners'in log
assert pin(canonical_index)==canonical_before
for k in ['protected','additional_protected']:refs(progress[k])
v=dict(status='passed-staged-program-exit-verification-canonical-integration-open',runtime=index['runtime'],build=index['manifest'],frozen_byte_identical_replay=pin(replay_path),canonical_program_unchanged=canonical_before,canonical_program_runtime=canonical['runtime'],protected=progress['protected'],additional_protected=progress['additional_protected'],readonly_SQL_reader_entries_unchanged=sql['entries'],new_contract_checks=193,inherited_plan_runs=124,inherited_contract_counts_not_recounted=True,configured_material_attempts=232,closed_material_attempts=116,configured_material_outcomes=build['configured_native_outcomes'],native_exception_exit=build['native_exception_exit'],inputs=[pin(run/Path(__file__).name),pin(run/'SourceMassTeleportExitLoad.java')],artifacts=[pin(run/'verify.log'),pin(run/'direct-lock-census.tsv')],no_new_global_registry_TLS_queue_or_worker=True,full_material_semantics_qualified=False,actual_Ctrl_or_cursor_qualified=False,canonical_progress_integration_complete=False,new_SQL_connections=0,Source_or_player_SQL_or_WAL_written=False,formal_deployment_changed=False,new_baseline_created=False,full_goal_achieved=False)
write(run/'manifest.json',v);write(D/'source-mass-teleport-exit-verification-current.json',dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
write(D/'source-mass-teleport-exit-pending-integration-current.json',dict(status='program-and-replay-verified-canonical-progress-integration-next',runtime=v['runtime'],verification=pin(run/'manifest.json'),canonical_parent=canonical['runtime'],next=['Integrate this exact cumulative candidate into the common builder/checkpoint/inventory and same-SHA ABI/weapon audits; preserve six legacy alias mismatches.','Continue real normal/Ctrl/friendly/NPC/area material effects; configured cost or rejection is not full semantic proof.','Keep all12 goal groups open, defer actual backend/MySQL/game validation, never deploy.'],full_goal_achieved=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')),ensure_ascii=False))

"""Verify scoped negative29/152/153 first-intent repair, not full Source or release qualification."""
from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,os,shutil,subprocess,zipfile,csv,re
R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2];J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
def read(p):return json.loads(Path(p).read_text('utf-8'))
def pin(p):
 p=Path(p).resolve();return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def checked(v):assert pin(v['path'])==v,v['path'];return Path(v['path'])
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
canonical_index=pin(D/'source-program-update-progress-current.json');canonical=read(canonical_index['path']);progress=read(checked(canonical['manifest']));parent=read(checked(progress['build']))
stage_ref=read(D/'source-negative-pair-first-probe-current.json')['manifest'];stage=read(checked(stage_ref));replay_ref=read(D/'source-negative-pair-first-reproduction-current.json')['manifest'];replay=read(checked(replay_ref))
full_ref=pin(D/'source-negative-pair-first-probes/20261027T052743352340Z/manifest.json');assert full_ref['sha256']=='f2af11d424ed4b8a2af0112b9924b9a3d12c591787bdb08fa427c308a09c62be';full=read(checked(full_ref))
assert canonical['runtime']['sha256']=='e7701e624f503b7078714efc346c5481e6778acd6dd1ce5ce9e2399fa773bc4a'
for m in [stage,replay,full]:
 assert m['runtime']['sha256']=='bf4aa2a61d488bd68c2cdaa0d9a7df3221f2e7848443f225c584ea137ddcbaf2'and m['base']==canonical['runtime']and m['attempts']==150 and not m['errors']
 checked(m['runtime'])
 for ref in m['inputs']+m['artifacts']:checked(ref)
 with checked(next(x for x in m['artifacts']if Path(x['path']).name=='final-results.tsv')).open(encoding='utf-8')as f:rows=list(csv.DictReader(f,delimiter='\t'))
 assert len(rows)==150 and all(not r['error_type']and not r['cleanup_error']for r in rows)
 assert len([r for r in rows if r['mode'].startswith('plain-pair:')])==75
 assert {r['skill_id']for r in rows}=={'29','152','153'}
assert [(Path(x['path']).name,x['sha256'])for x in stage['inputs']]==[(Path(x['path']).name,x['sha256'])for x in replay['inputs']]and not stage['full_regression']and not replay['full_regression']and full['full_regression']and len(full['inherited_regular_plan_runs'])==124
text=checked(next(x for x in full['artifacts']if Path(x['path']).name=='probe.log')).read_text('utf-8')
for marker in ['69 complete original methods','PAIR LIFECYCLE checks=1578','actual-decoded-normal-or-Ctrl-negative-NPC-owner-pair-waits=150','NPC FINAL LIFECYCLE checks=695','PAIR LIFECYCLE checks=456','PAIR LIFECYCLE checks=312','PASS193 mass-teleport exit original-lock']:assert marker in text,marker
assert sum(map(int,re.findall(r'^PASS(\d+) ',text,re.M)))==16103
for m in [stage,replay]:assert 'PAIR LIFECYCLE checks=1878'in checked(next(x for x in m['artifacts']if Path(x['path']).name=='probe.log')).read_text('utf-8')
protected=[*progress['protected'],*progress['additional_protected'],pin(R/'current.json')]
for ref in protected:checked(ref)
with zipfile.ZipFile(progress['readonly_SQL_consumers_byte_retained']['base']['path'])as old,zipfile.ZipFile(stage['runtime']['path'])as new:
 for name in progress['readonly_SQL_consumers_byte_retained']['entries']:assert old.read(name)==new.read(name)
 assert not any('GeometryTrace'in n for n in new.namelist())
run=D/'source-negative-pair-first-validation'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for name in ['SourceNegativePairFirstLoad.java',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
cp=';'.join(map(str,[run/'tools',Path(canonical['runtime']['path']).parent/'tools',*[checked(x)for x in parent['asm']]]));args=['-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',str(run/'tools'),str(inputs/'SourceNegativePairFirstLoad.java')];arg=run/'javac.args';arg.write_text('\n'.join(json.dumps(x,ensure_ascii=False)for x in args),'utf-8');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
with (run/'verify.log').open('w',encoding='utf-8')as out:
 subprocess.run([str(J/'javac.exe'),'@'+str(arg)],env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=45)
 subprocess.run([str(J/'java.exe'),'-Xverify:all','-cp',cp,'SourceNegativePairFirstLoad',stage['runtime']['path'],str(S/'lib')],env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=45)
assert pin(canonical_index['path'])==canonical_index
for ref in protected:checked(ref)
v=dict(status='verified-negative29-152-153-first-intent-staged-common-integration-open',runtime=stage['runtime'],build=stage_ref,frozen_byte_identical_replay=replay_ref,same_SHA_full_regression=full_ref,canonical_runtime=canonical['runtime'],canonical_program_unchanged=canonical_index,original_methods_independently_inverted=69,original_changed_owners=2,explicit_call_ports=5,transient_Aim_fields=1,new_runtime_classes=2,new_global_roots=0,source_failure_no_probability_RNG_and_no_counter_erase_sleep_move_mutation=True,first_proof_release_and_original_shared_self_pair_slot_unchanged=True,normal_and_Ctrl_NPC_owner_pair_waits=150,normal_and_Ctrl_pair_assertions=1878,full_regression_pair_assertions_before_added_cleanup_checks=1578,inherited_ordinary_checks=16103,inherited_regular_plans=124,inherited_NPC_final_waits=78,inherited_NPC_final_assertions=695,inherited_NPC_pair_waits=60,inherited_NPC_pair_assertions=456,inherited_PC_pair_waits=42,inherited_PC_pair_assertions=312,readonly_SQL_consumers_byte_retained=progress['readonly_SQL_consumers_byte_retained'],protected=protected,inputs=[pin(p)for p in sorted(inputs.iterdir())],artifacts=[pin(run/'verify.log'),pin(arg)],full_Source_qualified=False,common_progress_integration_complete=False,new_SQL_connections=0,new_game_runtime_cases=0,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False,full_goal_achieved=False,qualified_for_release=False)
write(run/'manifest.json',v);write(D/'source-negative-pair-first-verification-current.json',dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
write(D/'source-negative-pair-first-pending-integration-current.json',dict(status='verified-staged-first-intent-common-assembly-next',runtime=v['runtime'],verification=pin(run/'manifest.json'),next=['Assemble a self-contained common tools/plan/compiler closure for this exact runtime, preserving the unique elemental fixture.','Run same-SHA inventory/ABI/weapon overlay and advance only isolated program checkpoint; preserve6 legacy aliases and12 open work groups.','Continue final receiver/value-only lifetime proof and other remaining material/Source paths; no deployment or SQL writes.'],full_goal_achieved=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')),ensure_ascii=False))

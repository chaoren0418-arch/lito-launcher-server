"""Resume a terminal failed acceptance, proving every retained pass and input hash.

The ordinary runner remains the reproducible full entry. This driver injects
only a successful-prefix cache and provenance into that exact saved runner.
The failed command and every later command execute normally, once, in order.
"""
from pathlib import Path
import hashlib
import json

R=Path(__file__).resolve().parent
S=R.parents[1]
B=R/'build'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
read=lambda p:json.loads(p.read_text('utf-8'))
def binding(p):return {'path':str(p.relative_to(S)),'sha256':sha(p)}
def archive(p,kind):
 target=B/(kind+'-'+sha(p)+p.suffix)
 if target.exists():assert target.read_bytes()==p.read_bytes()
 else:target.write_bytes(p.read_bytes())
 return binding(target)

prior_path=B/'combat-acceptance-current.json'
prior=read(prior_path)
base=R/'run_combat_acceptance.py'
assert prior['status']=='failed' and prior.get('finished_utc'),'only terminal failed acceptances may resume'
assert prior['runner_sha256']==sha(base),'base runner changed; run full acceptance'
assert prior['deterministic_rebuild'],'two identical complete builds must precede reuse'
assert prior['runners'] and prior['runners'][-1]['exit_code']!=0
prefix=prior['runners'][:-1]
assert prefix and all(row['exit_code']==0 for row in prefix)
candidate=B/'candidate.jar'
assert sha(candidate)==prior['candidate_sha256']
manifest=read(B/'build-manifest.json')
assert manifest['candidate_sha256']==prior['candidate_sha256']
assert sha(Path(manifest['base']))==manifest['base_sha256']
for name,digest in manifest['builder_inputs'].items():assert sha(S/name)==digest,name
for item in [*prior['verification_inputs'],*prior['control_inputs'],*prior['source_map_inputs']]:
 assert sha(S/item['path'])==item['sha256'],item['path']
known={str((S/item['path']).resolve()).casefold() for item in prior['verification_inputs']}
current={str(p.resolve()).casefold() for p in [*R.glob('*.java'),*R.glob('*.py'),*[S/name for name in ['tools/dragon-combat/Regression.java','tools/shared-database/SourceLoginRegression.java','tools/durable-settlement/FlowRegression.java','tools/native-room-state/NativeDirectoryRegression.java','tools/native-room-state/MovementRegression.java','tools/room-rules/RoomRulesRegression.java']]]}
assert current==known|{str(Path(__file__).resolve()).casefold()},'verification input inventory changed'
for row in prefix:assert sha(S/row['log'])==row['log_sha256'],row['log']

previous=archive(prior_path,'resume-previous-acceptance')
diagnostics=[]
failed_log=S/prior['runners'][-1]['log']
diagnostics.append(archive(failed_log,'resume-failure-runner'))
if prior['runners'][-1]['runner']=='run_blessing_npc_test.py':
 diagnostics.append(archive(B/'BlessingNpcRegression.log','resume-failure-native'))
 if (S/'hs_err_pid23284.log').exists():diagnostics.append(archive(S/'hs_err_pid23284.log','resume-failure-jvm'))
source=base.read_text('utf-8')
run_anchor='def run(name, *arguments):\n'
save_anchor='def save():\n'
assert source.count(run_anchor)==source.count(save_anchor)==1
cache='''    index = len(receipt['runners'])
    if index < len(_resume_prefix):
        row = dict(_resume_prefix[index])
        if row['runner'] != name or row['arguments'] != list(arguments) or row['exit_code'] != 0 or sha(S / row['log']) != row['log_sha256']:
            raise RuntimeError('Verified acceptance prefix/command/log drift at ' + name)
        receipt['runners'].append(row)
        save()
        print('REUSE verified PASS ' + name, flush=True)
        return
'''
effective=source.replace(run_anchor,run_anchor+cache).replace(save_anchor,save_anchor+"    receipt['resume'] = _resume_metadata\n")
effective_path=B/('resume-effective-runner-'+hashlib.sha256(effective.encode('utf-8')).hexdigest()+'.py')
if effective_path.exists():assert effective_path.read_text('utf-8')==effective
else:effective_path.write_text(effective,encoding='utf-8')
metadata={'mode':'verified successful-prefix reuse; failed and later runners execute normally','previous_receipt':previous,'driver':binding(Path(__file__).resolve()),'effective_runner':binding(effective_path),'base_runner_sha256':sha(base),'retained_pass_count':len(prefix),'previous_started_utc':prior['started_utc'],'previous_finished_utc':prior['finished_utc'],'previous_failure':prior.get('failure'),'failure_diagnostics':diagnostics,'candidate_and_all_prior_inputs_unchanged':True,'only_new_verification_input_is_this_driver':str(Path(__file__).relative_to(S)) not in {item['path'] for item in prior['verification_inputs']}}
print('Resume verified pass prefix='+str(len(prefix))+'; rerun '+prior['runners'][-1]['runner'],flush=True)
exec(compile(effective,str(base),'exec'),{'__name__':'__main__','__file__':str(base),'_resume_prefix':prefix,'_resume_metadata':metadata})

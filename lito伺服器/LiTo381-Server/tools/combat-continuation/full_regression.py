from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,os,shutil,sys
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
from regression_support import formal_protection, verify_formal_protection, Options, adapt_driver, prepare_regression
options=Options();S=options.server;B=S/'tools/resource-database/build';D=prepare_regression(options,options.args.stage or 'cancellation-battle-runtime',None if options.args.stage else Path(__file__).resolve().parent/'build/integration-regression') if options.args.jar or options.out else Path(__file__).resolve().parent/'build/integration-regression'
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
build=read(D/'build-current.json');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256']
write(D/'prepare-current.json',build)
names=['replay-native-de-item-state-source.py','replay-native-de-item-state-perception.py','replay-native-de-item-state-state-world.py','replay-native-de-item-state-components.py','replay-native-de-item-state-known.py','replay-native-de-item-state-corpse-application.py','run-native-de-item-state-regressions.py','replay-native-de-item-state-gear.py','run-native-de-item-state-sprite-startup.py','run-native-de-item-state-startup.py','replay-native-de-item-state.py','replay-native-de-item-state-companion.py','probe-player-action-authority.py','replay-player-action-publication.py','replay-player-action-main-owned.py','replay-player-action-transaction.py','replay-player-action-admission.py','replay-player-action-roots.py','replay-player-action-main-interrupt.py','replay-player-action-main.py']
previous=[]
if (D/'qualification-current.json').exists():
 prior=read(D/'qualification-current.json'); prior_manifest=Path(prior['manifest']['path']); assert sha(prior_manifest)==prior['manifest']['sha256']; prior_data=read(prior_manifest)
 if prior_data['runtime']==build['runtime']:previous=prior_data['results']
run=D/'qualification-runs'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);shutil.copyfile(Path(__file__).resolve(),inputs/Path(__file__).name)
env=dict(os.environ,PYTHONPATH=str(B));v=dict(status='running',build=build['manifest'],runtime=build['runtime'],driver=pin(Path(__file__).resolve()),frozen_driver=pin(inputs/Path(__file__).name),results=[],assertions_or_deadlines_changed=False,sequential_to_limit_existing_timeout_interference=True)
limits='-Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64'
write(run/'manifest.json',v);write(D/'qualification-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json')))
try:
 with formal_protection(options,v) as guard:
  for index,name in enumerate(names):
   original=B/name;text=original.read_text('utf-8');old="D=B/'de-item-state-development'"if index<12 else"D=B/'de-player-action-development'";assert text.count(old)==1;text=text.replace(old,"D=Path("+repr(str(D))+")")
   prelude='S=Path(__file__).resolve().parents[3];B=Path(__file__).resolve().parent'
   if prelude in text:assert text.count(prelude)==1;text=text.replace(prelude,'S=Path('+repr(str(S))+');B=Path('+repr(str(B))+')')
   if limits not in text:assert text.count('-XX:ReservedCodeCacheSize=64m')==1;text=text.replace('-XX:ReservedCodeCacheSize=64m','-XX:ReservedCodeCacheSize=64m '+limits)
   text=adapt_driver(text,options.config,D/'runtime');adapted=inputs/name;adapted.write_text(text,encoding='utf-8');log=run/(name+'.log');print('RUN '+str(index+1)+'/'+str(len(names))+' '+name,flush=True)
   if index==10 and (D/'replay-reconciliation.json').exists() and (not options.out or read(D/'replay-reconciliation.json')['status']=='passed-behavior-and-current-formal-guard'):
    receipt=read(D/'replay-reconciliation.json');assert receipt['status']=='passed-behavior-and-current-formal-guard' and receipt['runtime']==build['runtime']
    if options.out:verify_formal_protection(receipt,guard)
    for ref in [receipt['original_failed_manifest'],*receipt['protected_files']]:assert sha(Path(ref['path']))==ref['sha256']
    for result in receipt['results']:
     for key in ['observations','log']:assert sha(Path(result[key]['path']))==result[key]['sha256']
    old=previous[index];assert old['adapted_driver']['sha256']==sha(adapted)
    row=dict(old);row['accepted_reconciliation']=pin(D/'replay-reconciliation.json');v['results'].append(row);write(run/'manifest.json',v);print('REUSE 1728 completed behavioral cases; explicit formal guard reconciliation',flush=True);continue
   # An explicit output requests a new run, not cached qualification.
   if not options.out and index<len(previous) and previous[index]['exit_code']==0 and previous[index]['adapted_driver']['sha256']==sha(adapted):
    old=previous[index]; assert sha(Path(old['log']['path']))==old['log']['sha256'];v['results'].append(old);write(run/'manifest.json',v);print('REUSE passed same runtime and driver '+name,flush=True);continue
   with log.open('w',encoding='utf-8')as out:result=subprocess.run([sys.executable,'-B','-X','utf8',str(adapted)],cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,timeout=900)
   row=dict(name=name,exit_code=result.returncode,original_driver=pin(original),adapted_driver=pin(adapted),adaptation='evidence destination and absolute workspace paths; four explicit bounded-worker flags if absent; fixture behavior/assertions/deadlines unchanged',log=pin(log));v['results'].append(row);write(run/'manifest.json',v)
   if result.returncode:print(log.read_text('utf-8',errors='replace')[-11000:]);raise RuntimeError('frozen qualification failed: '+name)
  v['status']='passed-twelve-native-regression-drivers-and-eight-authority-item-Main-replays';print(json.dumps(dict(status=v['status'],drivers=len(v['results'])),ensure_ascii=False))
except BaseException as error:v.update(status='failed',error=str(error));raise
finally:write(run/'manifest.json',v);write(D/'qualification-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json')))


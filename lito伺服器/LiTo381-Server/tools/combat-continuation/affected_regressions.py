"""Run existing affected material tests against the composed immutable candidate."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,os,shutil,sys
from regression_support import formal_protection, Options, adapt_driver, prepare_regression
options=Options();R=options.root;S=options.server;B=S/'tools/resource-database/build';D=prepare_regression(options,options.args.stage or 'cancellation-battle-runtime',None if options.args.stage else Path(__file__).resolve().parent/'build/integration-regression') if options.args.jar or options.out else Path(__file__).resolve().parent/'build/integration-regression'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
build=read(D/'build-current.json');q=read(D/'qualification-current.json');assert q['status'].startswith('passed-');assert sha(Path(build['runtime']['path']))==build['runtime']['sha256']
for prototype in (B/'de-native-item-weapon-clock-development').glob('*prototype*.json'):
 for ref in read(prototype).values():
  if isinstance(ref,dict) and 'path' in ref and 'sha256' in ref:assert sha(Path(ref['path']))==ref['sha256']
 shutil.copyfile(prototype,D/prototype.name)
previous=[]
if not options.out and (D/'affected-current.json').exists():
 p=read(D/'affected-current.json');assert sha(Path(p['manifest']['path']))==p['manifest']['sha256'];old=read(Path(p['manifest']['path']))
 if old['runtime']==build['runtime']:previous=old['results']
names=['timed-weapon-probe-native-cast-material-plan.py','timed-weapon-probe-native-cast-material-policy.py','timed-weapon-probe-native-cast-material-items.py','timed-weapon-probe-native-cast-material-preflight.py','timed-weapon-probe-native-cast-material-authority.py','timed-weapon-probe-native-cast-material.py','timed-weapon-probe-native-cast-material-fault.py','timed-weapon-probe-native-cast-material-stopped.py','timed-weapon-probe-native-self-material-casts.py','timed-weapon-probe-native-dragon-material-casts.py','timed-weapon-probe-native-cube-material-casts.py','timed-weapon-probe-native-cube-material-fault.py','timed-weapon-probe-native-cube-material-stopped.py','timed-weapon-probe-native-cube-material-edges.py','timed-probe-native-item-weapon-material.py','timed-probe-native-item-weapon-fault.py','timed-probe-native-item-weapon-stopped.py','timed-probe-native-item-weapon-edges.py']
run=D/'affected'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);inputs=run/'inputs';inputs.mkdir();rows=[];v={'status':'running','runtime':build['runtime'],'results':rows,'formal_deployment_changed':False};env=dict(os.environ,PYTHONPATH=str(B))
try:
 with formal_protection(options,v):
  for i,name in enumerate(names):
   text=(B/name).read_text('utf-8');old="S=Path(__file__).resolve().parents[3];B=Path(__file__).resolve().parent;D=B/'de-native-item-weapon-clock-development'";assert text.count(old)==1
   text=text.replace(old,'S=Path('+repr(str(S))+');B=Path('+repr(str(B))+');D=Path('+repr(str(D))+')');text=adapt_driver(text,options.config,D/'runtime');adapted=inputs/name;adapted.write_text(text,encoding='utf-8');log=run/(name+'.log');print(f'RUN affected {i+1}/{len(names)} {name}',flush=True)
   if i<len(previous) and previous[i]['exit_code']==0 and previous[i]['driver']['sha256']==sha(adapted):
    old=previous[i];assert sha(Path(old['log']['path']))==old['log']['sha256'];rows.append(old);write(run/'manifest.json',v);print('REUSE same candidate and fixture',flush=True);continue
   with log.open('w',encoding='utf-8') as out:r=subprocess.run([sys.executable,'-B','-X','utf8',str(adapted)],cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,timeout=600)
   rows.append({'driver':pin(adapted),'original':pin(B/name),'log':pin(log),'exit_code':r.returncode});write(run/'manifest.json',v)
   if r.returncode:print(log.read_text('utf-8',errors='replace')[-5000:]);raise RuntimeError(name+' failed')
  v['status']='passed-existing-material-regressions'
except BaseException as e:v.update(status='failed',error=str(e));raise
finally:write(run/'manifest.json',v);write(D/'affected-current.json',{'status':v['status'],'runtime':build['runtime'],'manifest':pin(run/'manifest.json')})

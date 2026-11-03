from pathlib import Path
import importlib.util,json,os,subprocess,sys,time,hashlib
L=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('startup',L/'src/development_startup.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
run=L/'app/runs'/('additional-diagnostic-'+str(time.time_ns()));run.mkdir()
profile=json.loads((L/'app/runs/development-9bec8141440a4b61bc639381a7830196/server-profile.json').read_text());(run/'server-profile.json').write_text(json.dumps(profile),encoding='utf8')
env=dict(os.environ,LITO_NATIVE_LOGIN_HOST=profile['host'],LITO_NATIVE_SOURCES=';'.join(str(i)+':'+str(profile['nativeSourcePortBase']+i)+':'+name.encode('cp950').hex() for i,name in zip(profile['nativeSourceIds'],profile['classicSourceNames'])))
for k in ('LITO381_ACCOUNT','LITO381_PASSWORD','LITO381_CHARACTER','LITO381_CHARACTER_NAME'):env.pop(k,None)
cmd=m.native_args('StandardArena',run);pos=cmd.index('--conditional-patch');mutex=('LiTo'+hashlib.sha256(str(run).encode()).hexdigest()[:12]).encode()
patches=[(0x62d320,b'Xineage491284632',mutex),(0x480326,bytes.fromhex('7409'),bytes.fromhex('eb09')),(0x480ab3,bytes.fromhex('7407'),bytes.fromhex('eb07'))]
args=[]
for a,b,c in patches:args+=['--conditional-patch',f'{a:#x}:{b.hex()}:{c.hex()}']
cmd[pos:pos]=args
cmd+=['--multi-instance-startup-gate']

cmd+=['--timeout','30']
print(str(run),flush=True)
with (run/'stderr.log').open('w') as err:
 p=subprocess.run(cmd,cwd=m.CLIENT,env=env,stdout=subprocess.DEVNULL,stderr=err,creationflags=subprocess.CREATE_NO_WINDOW,timeout=45)
print(p.returncode)
import json
d=json.loads((run/'native-probe.json').read_text())
print(json.dumps({'status':d['status'],'error':d.get('error'),'gate':d.get('startup_gate'),'resources':d.get('shared_resources',{}).get('ready_receipt_written')}))
assert d.get('startup_gate',{}).get('status')=='held' and not d.get('shared_resources') and d['status']=='probe_error'
print('PASS: bad original bytes leave startup gate closed; owned job cleaned')

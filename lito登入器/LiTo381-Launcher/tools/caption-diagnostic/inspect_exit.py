from pathlib import Path
import importlib.util,json,os,subprocess,sys,time,hashlib
L=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('startup',L/'src/development_startup.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
run=L/'app/runs'/('additional-diagnostic-'+str(time.time_ns()));run.mkdir()
profile=json.loads((L/'app/runs/development-9bec8141440a4b61bc639381a7830196/server-profile.json').read_text());(run/'server-profile.json').write_text(json.dumps(profile),encoding='utf8')
env=dict(os.environ,LITO_NATIVE_LOGIN_HOST=profile['host'],LITO_NATIVE_SOURCES=';'.join(str(i)+':'+str(profile['nativeSourcePortBase']+i)+':'+name.encode('cp950').hex() for i,name in zip(profile['nativeSourceIds'],profile['classicSourceNames'])))
for k in ('LITO381_ACCOUNT','LITO381_PASSWORD','LITO381_CHARACTER','LITO381_CHARACTER_NAME'):env.pop(k,None)
cmd=m.native_args('StandardArena',run);pos=cmd.index('--conditional-patch');mutex=('LiTo'+hashlib.sha256(str(run).encode()).hexdigest()[:12]).encode()
patches=[(0x62d320,b'Lineage491284632',mutex),(0x480326,bytes.fromhex('7409'),bytes.fromhex('eb09')),(0x480ab3,bytes.fromhex('7407'),bytes.fromhex('eb07')),(0x48253c,bytes.fromhex('8945e4eb15'),bytes.fromhex('ebfe909090')),(0x48254a,bytes.fromhex('e8e167faff'),bytes.fromhex('ebfe909090'))]
for code,a in enumerate((0x480ab5,0x4817ae,0x4817fc,0x48182a,0x481841,0x48195b,0x481b66,0x481c30,0x481c7b,0x481ce8,0x481f3b,0x481f90,0x481fc4,0x481fee,0x482081,0x482091),1):patches.append((a,bytes.fromhex('33c0'),bytes([0xb0,code])))
args=[]
for a,b,c in patches:args+=['--conditional-patch',f'{a:#x}:{b.hex()}:{c.hex()}']
cmd[pos:pos]=args
cmd[4]=str(Path(__file__).with_name('exit_probe.py'))
cmd+=['--trace-process-exit']
cmd+=['--timeout','8','--capture-contexts','--dump-image',str(run/'stack.bin'),'--dump-base','0x190000','--dump-size','0x10000','--dump-at','3']
print(str(run),flush=True)
with (run/'stderr.log').open('w') as err:
 p=subprocess.run(cmd,cwd=m.CLIENT,env=env,stdout=subprocess.DEVNULL,stderr=err,creationflags=subprocess.CREATE_NO_WINDOW,timeout=45)
print(p.returncode)

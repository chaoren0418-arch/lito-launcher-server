"""Owned native UI exercise with synthetic accounts on isolated ports; no SQL writes."""
from pathlib import Path
import ctypes, importlib.util, json, os, shutil, struct, subprocess, sys, time
L=Path(__file__).resolve().parents[2];S=L.parent.parent/'lito伺服器/LiTo381-Server'
defs=(L/'tools/native-flow-build/verify_native_order.py').read_text('utf-8').split("run='native-order-'")[0]
exec(compile(defs,'native-test-helpers','exec'))
spec=importlib.util.spec_from_file_location('helper',L/'src/development_startup.py');h=importlib.util.module_from_spec(spec);spec.loader.exec_module(h)
run=L/'app/runs'/('same-window-fixture-'+str(time.time_ns()));run.mkdir()
config=(S/'config/server.properties').read_text('utf-8-sig')
config+='\nlogin.source.port.base=52000\nport=52000\nadvertised.port=52000\nchannel.ports=52000\n'
(run/'server.properties').write_text(config,encoding='utf-8')
sources=[(1,'太陽神','380_ac'),(2,'月亮神','380_ac_1'),(3,'伺服器3','380_ac_2')]
profile={'schemaVersion':4,'classicSourceNames':[x[1] for x in sources], 'staticDatabase':'380_db',
 'dynamicDatabases':[x[2] for x in sources],'profileId':'native-fixture','displayName':'Native fixture',
 'host':'127.0.0.1','port':52001,'localServerConfig':'server.properties','nativeSourceIds':[1,2,3],'nativeSourcePortBase':52000}
(run/'server-profile.json').write_text(json.dumps(profile,ensure_ascii=False),encoding='utf-8')
env=dict(os.environ,LITO_NATIVE_LOGIN_HOST='127.0.0.1',LITO_NATIVE_SOURCES=';'.join(f'{i}:{52000+i}:{name.encode("cp950").hex()}' for i,name,db in sources))
for key in ('LITO381_ACCOUNT','LITO381_PASSWORD','LITO381_CHARACTER'):env.pop(key,None)
shutil.copyfile(L/'native/ddraw-source-test.dll',h.CLIENT/'ddraw.dll')
streams=[];server=probe=None;child_handle=None
def output(name):
 f=(run/name).open('wb');streams.append(f);return f
def mem(pid,address,size):
 handle=k.OpenProcess(0x410,False,pid)
 try:
  b=ctypes.create_string_buffer(size)
  if not k.ReadProcessMemory(handle,address,b,size,None):raise ctypes.WinError(ctypes.get_last_error())
  return b.raw
 finally:k.CloseHandle(handle)
def integer(pid,address):return int.from_bytes(mem(pid,address,4),'little')
def key(hwnd,value):u.PostMessageW(hwnd,0x100,value,0);u.PostMessageW(hwnd,0x101,value,0)
def wait_state(pid,predicate,timeout=25):
 end=time.monotonic()+timeout;last=None
 while time.monotonic()<end:
  if probe.poll() is not None:raise RuntimeError('Native ended during fixture')
  state=inspect(pid)
  if state!=last:print('UI',state,flush=True);last=state
  if predicate(state):return state
  time.sleep(.15)
 raise AssertionError('Native state timed out: '+str(last))
def blank(pid,state):
 slots=integer(pid,state['root'])
 assert [integer(pid,integer(pid,slots+i*4)+0x38) for i in range(2)]==[0,0]
 assert mem(pid,0x75f755,1)==b'\0'
def server_locks(expected):
 server.stdin.write('state\n');server.stdin.flush();time.sleep(.4)
 lines=(run/'server.stdout.log').read_text('utf-8',errors='replace').splitlines()
 got=[row for row in lines if row.startswith('NATIVE_FIXTURE_LOCKS ')][-1]
 assert got==f'NATIVE_FIXTURE_LOCKS {expected}',got
try:
 server=subprocess.Popen([r'C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin\java.exe',
  '-cp',str(S/'tools/shared-database/build/test')+';'+str(L/'app/server/LiTo381-SharedResources-candidate.jar'),
  'local.lito381.server.NativeSourceFixture',str(run/'server.properties'),str(run/'capture')],
  cwd=S,stdin=subprocess.PIPE,stdout=output('server.stdout.log'),stderr=output('server.stderr.log'),text=True,creationflags=subprocess.CREATE_NO_WINDOW)
 end=time.monotonic()+20
 while 'NATIVE_FIXTURE_READY' not in (run/'server.stdout.log').read_text('utf-8',errors='replace'):
  if server.poll() is not None or time.monotonic()>end:raise RuntimeError('Fixture server startup failed')
  time.sleep(.1)
 probe=subprocess.Popen(h.native_args('StandardArena',run),cwd=h.CLIENT,env=env,stdin=subprocess.DEVNULL,
  stdout=output('probe.stdout.log'),stderr=output('probe.stderr.log'),creationflags=subprocess.CREATE_NO_WINDOW)
 end=time.monotonic()+40
 while time.monotonic()<end:
  child=h.find_child(probe.pid,h.CLIENT/'LiTo.bin')
  if child:child_handle,identity=child;break
  if probe.poll() is not None:raise RuntimeError('Probe startup failed')
  time.sleep(.1)
 else:raise RuntimeError('Owned native process missing')
 pid=identity['pid'];state=wait_state(pid,lambda s:s['modal'] and s['key']==0x499a40,45);hwnd=window(pid)
 key(hwnd,13)
 state=wait_state(pid,lambda s:s['page']==0 and not s['modal']);blank(pid,state)
 time.sleep(3);blank(pid,inspect(pid))
 assert 'C_LOGIN ' not in (run/'server.stdout.log').read_text('utf-8',errors='replace')
 print('PASS same PID/HWND, blank login, no unsolicited C_LOGIN',flush=True)
 u.PostMessageW(hwnd,0x8390,0,0)
 state=wait_state(pid,lambda s:s['page']==7 and s['count']==8,30)
 server_locks(1)
 # Invoke the actual Return control callback, then reauthenticate on the same source.
 u.PostMessageW(hwnd,0x8393,0,0)
 state=wait_state(pid,lambda s:s['page']==0 and not s['modal'],15);blank(pid,state)
 assert window(pid)==hwnd
 server_locks(0)
 before=(run/'server.stdout.log').read_text('utf-8',errors='replace').count('C_LOGIN ')
 time.sleep(2);blank(pid,inspect(pid))
 assert (run/'server.stdout.log').read_text('utf-8',errors='replace').count('C_LOGIN ')==before
 u.PostMessageW(hwnd,0x8390,0,0)
 state=wait_state(pid,lambda s:s['page']==7 and s['count']==8,30);server_locks(1)
 rows=integer(pid,0x75f758);assert mem(pid,rows,len('太陽神'.encode('cp950')))=='太陽神'.encode('cp950')
 print('PASS Return button: blank login on same source, lock released, login restored',flush=True)
 u.PostMessageW(hwnd,0x8391,0,0)
 state=wait_state(pid,lambda s:s['modal'] and s['key']==0x499a40,15)
 assert window(pid)==hwnd
 server_locks(0)
 u.PostMessageW(hwnd,0x8392,0,0);state=wait_state(pid,lambda s:s['page']==0 and not s['modal']);blank(pid,state)
 time.sleep(2);blank(pid,inspect(pid))
 u.PostMessageW(hwnd,0x8390,0,0)
 state=wait_state(pid,lambda s:s['page']==7 and s['count']==8,30);server_locks(1)
 time.sleep(.5)
 rows=integer(pid,0x75f758);assert mem(pid,rows,len('月亮神'.encode('cp950')))=='月亮神'.encode('cp950')
 result={'return_button_blank_login':True,'return_preserves_source':True,'selected_source_after_relogin':2,'run':run.name,'pid':pid,'hwnd':hwnd,'same_window':True,'blank_login':True,
  'native_login_callback':True,'test_only_ui_driver':True,'character_server_logout':True,'account_lock_released':True,'relogin':True,'no_sql_writes':True}
 (L/'docs/same-window-source-verification.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
 print('PASS',json.dumps(result),flush=True)
finally:
 if probe:
  if probe.poll() is None:probe.terminate()
  probe.wait(timeout=10)
 if child_handle:
  assert h.kernel.WaitForSingleObject(child_handle,5000)==0
  h.kernel.CloseHandle(child_handle)
 if server:
  if server.poll() is None:server.stdin.write('stop\n');server.stdin.flush()
  try:server.wait(timeout=10)
  except subprocess.TimeoutExpired:server.terminate();server.wait(timeout=5)
 shutil.copyfile(L/'app/compat/ddraw.dll',h.CLIENT/'ddraw.dll')
 for stream in streams:stream.close()
 print('Owned native and fixture server stopped; run='+str(run),flush=True)

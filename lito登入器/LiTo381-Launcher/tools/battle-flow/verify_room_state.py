"""Owned native UI exercise with synthetic accounts on isolated ports; no SQL writes."""
from pathlib import Path
import ctypes, importlib.util, json, os, shutil, struct, subprocess, sys, time, re
L=Path(__file__).resolve().parents[2];S=L.parent.parent/'lito伺服器/LiTo381-Server'
if '--location' in sys.argv:sys.argv.append('--targets')
if '--targets' in sys.argv:sys.argv.append('--presentation')
defs=(L/'tools/native-flow-build/verify_native_order.py').read_text('utf-8').split("run='native-order-'")[0]
exec(compile(defs,'native-test-helpers','exec'))
spec=importlib.util.spec_from_file_location('helper',L/'src/development_startup.py');h=importlib.util.module_from_spec(spec);spec.loader.exec_module(h)
run=L/'app/runs'/('room-state-fixture-'+str(time.time_ns()));run.mkdir()
config=(S/'config/server.properties').read_text('utf-8-sig')
config=re.sub(r'^port=.*$', 'port=52000', config, flags=re.M)
(run/'server.properties').write_text(config,encoding='utf-8')
sources=[(1,'公牛','380_ac_1'),(2,'天使','380_ac_2')]
profile={'schemaVersion':4,'classicSourceNames':[x[1] for x in sources], 'staticDatabase':'380_db',
 'dynamicDatabases':[x[2] for x in sources],'profileId':'native-fixture','displayName':'Native fixture',
 'host':'127.0.0.1','port':52001,'localServerConfig':'server.properties','nativeSourceIds':[1,2],'nativeSourcePortBase':52000}
(run/'server-profile.json').write_text(json.dumps(profile,ensure_ascii=False),encoding='utf-8')
env=dict(os.environ,LITO_NATIVE_LOGIN_HOST='127.0.0.1',LITO_NATIVE_SOURCES=';'.join(f'{i}:{52000+i}:{name.encode("cp950").hex()}' for i,name,db in sources))
for key in ('LITO381_ACCOUNT','LITO381_PASSWORD','LITO381_CHARACTER'):env.pop(key,None)
shutil.copyfile(L/'native/ddraw-source-test.dll',h.CLIENT/'ddraw.dll')
streams=[];server=probe=None;child_handle=None
def output(name):
 f=(run/name).open('wb');streams.append(f);return f
def window(pid):
 found=[]
 @callback
 def visit(hwnd,param):
  owner=W.DWORD();u.GetWindowThreadProcessId(hwnd,ctypes.byref(owner))
  if owner.value==pid and u.IsWindowVisible(hwnd):
   title=ctypes.create_unicode_buffer(256);u.GetWindowTextW(hwnd,title,256)
   if title.value=='Lineage Windows Client':found.append(hwnd)
  return True
 u.EnumWindows(visit,0)
 assert len(found)==1,('owned main window',found)
 return found[0]

def mem(pid,address,size):
 handle=k.OpenProcess(0x410,False,pid)
 try:
  b=ctypes.create_string_buffer(size)
  if not k.ReadProcessMemory(handle,address,b,size,None):raise ctypes.WinError(ctypes.get_last_error())
  return b.raw
 finally:k.CloseHandle(handle)
def integer(pid,address):return int.from_bytes(mem(pid,address,4),'little')
u.SendMessageTimeoutW.argtypes=[W.HWND,W.UINT,W.WPARAM,W.LPARAM,W.UINT,W.UINT,ctypes.POINTER(ctypes.c_size_t)]
def test_message(hwnd,message):
 result=ctypes.c_size_t()
 assert u.SendMessageTimeoutW(hwnd,message,0,0,2,5000,ctypes.byref(result)),('native test message',hex(message),ctypes.get_last_error())
 return True

def key(hwnd,value):u.PostMessageW(hwnd,0x100,value,0);u.PostMessageW(hwnd,0x101,value,0)
locations=[]
def check_location(pid,hwnd,lobby):
 assert mem(pid,0x45a62d,1)==b'\xe9','location draw hook missing'
 raw=integer(pid,0x64abf4);result=ctypes.c_size_t()
 assert u.SendMessageTimeoutW(hwnd,0x839f,0,0,2,5000,ctypes.byref(result))
 expected=0 if lobby else raw
 assert result.value==expected and (lobby or raw>0),(raw,result.value,lobby)
 assert integer(pid,0x64abf4)==raw,'presentation changed protocol room context'
 assert u.SendMessageTimeoutW(hwnd,0x839f,1,0,2,5000,ctypes.byref(result))
 assert result.value==37,'other player location was overwritten'
 locations.append({'phase':integer(pid,0x64af98),'context_room':raw,'display_room':expected,'peer_room':37})
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
probes=[];children=[];snapshots=[]
def wait_until(fn,label,seconds=30):
 end=time.monotonic()+seconds
 while time.monotonic()<end:
  if fn():return
  if any(p.poll() is not None for p in probes):raise AssertionError('Owned probe ended '+label)
  time.sleep(.1)
 raise AssertionError('Timed out '+label)
def command(text):
 server.stdin.write(text+'\n');server.stdin.flush()
def snapshot(pid,label):
 row={'pid':pid,'label':label,'phase':integer(pid,0x64af98),
  'callbacks':[hex(integer(pid,x)) for x in (0x62c9b8,0x62c9bc,0x62c9c0)],
  'actor':hex(integer(pid,0x834ee4)), 'ready':mem(pid,0x64afbc,16).hex(),
  'room_local':mem(pid,0x64abf8,28).split(b'\0')[0].hex()}
 row['camera']={hex(a):hex(integer(pid,a)) for a in (0x75aa48,0x75a998,0x75a99c,0x75abf0,0x75abf4,0x75a898,0x834eb4,0x641498,0x64bbe8,0x64b8fc,0x75ac34)}
 actor=integer(pid,0x834ee4)
 if actor:
  row['actor_bytes']=mem(pid,actor,0x13c).hex()
  if label in ('battle','second-battle'):
   markers=[]
   for i in range(integer(pid,0x834f08)):
    obj=integer(pid,integer(pid,0x834f10)+i*4)
    if integer(pid,obj+8) in (1,100001):
     marker=mem(pid,obj+0x13a,1)[0];markers.append({'object':integer(pid,obj+8),'marker':marker});assert marker==0,markers
   assert len(markers)==2,markers
   row['markers']=markers
 import pefile
 sys.path.insert(0,str(L/'app'));import lito_runtime_probe_shared as probe_module
 from PIL import Image
 dll=next(m for m in probe_module.module_snapshot(pid) if m['name'].lower()=='ddraw.dll')
 pe=pefile.PE(str(L/'native/ddraw-source-test.dll'));data=next(s.VirtualAddress for s in pe.sections if s.Name.startswith(b'.data'))
 bits=integer(pid,int(dll['base'],16)+data+0xfcc)
 Image.frombytes('RGB',(640,480),mem(pid,bits,640*480*4),'raw','BGRX').save(run/(str(pid)+'-'+label+'.png'))
 snapshots.append(row);print('SNAPSHOT',row,flush=True)
def room_rows(pid):
 root=integer(pid,0x64a958);count=integer(pid,root+0x2c);array=integer(pid,root+0x58);assert count<=256
 rows=[]
 for i in range(count):
  ptr=integer(pid,array+i*4);text=integer(pid,ptr+0x10)
  rows.append({'id':integer(pid,ptr+4),'name':mem(pid,text,80).split(b'\0')[0].decode('cp950'),'capacity':integer(pid,ptr+0x6c),'users':integer(pid,ptr+0x70)})
 assert len({r['id'] for r in rows})==len(rows),rows
 return rows

def directory(command_text):
 command(command_text)
 wait_until(lambda:('DIRECTORY_DONE '+command_text) in (run/'server.stdout.log').read_text('utf-8',errors='replace'),'directory command')
 time.sleep(.5)

try:
 server=subprocess.Popen([r'C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin\java.exe',
  '-Dfile.encoding=UTF-8','-cp',str(L/'tools/battle-flow/build')+';'+str(S/('tools/durable-settlement/build/candidate.jar' if '--settlement' in sys.argv else 'tools/native-room-admission/build/candidate.jar' if '--admission' in sys.argv else 'tools/native-room-state/build/candidate.jar'))+';'+str(S/'lib/*'),
  'local.lito381.server.NativeBattleFlowFixture',str(run/'server.properties'),str(run/'capture')],
  cwd=S,stdin=subprocess.PIPE,stdout=output('server.stdout.log'),stderr=output('server.stderr.log'),text=True,creationflags=subprocess.CREATE_NO_WINDOW)
 wait_until(lambda:'NATIVE_FIXTURE_READY' in (run/'server.stdout.log').read_text('utf-8',errors='replace'),'server')
 for index in range(2):
  childrun=run/str(index);childrun.mkdir();(childrun/'server-profile.json').write_text(json.dumps(profile,ensure_ascii=False),'utf-8')
  args=h.native_args('StandardArena',childrun)
  if index:
   args+=['--multi-instance-startup-gate','--conditional-patch','0x0062D320:'+b'Lineage491284632'.hex()+':'+b'LiToPairTest0001'.hex(),'--conditional-patch','0x00480326:7409:EB09','--conditional-patch','0x00480AB3:7407:EB07']
  probe=subprocess.Popen(args,cwd=h.CLIENT,env=env,stdin=subprocess.DEVNULL,stdout=output(str(index)+'.stdout.log'),stderr=output(str(index)+'.stderr.log'),creationflags=subprocess.CREATE_NO_WINDOW);probes.append(probe)
  child=None
  end=time.monotonic()+40
  while time.monotonic()<end:
   child=h.find_child(probe.pid,h.CLIENT/'LiTo.bin')
   if child:break
   if probe.poll() is not None:raise RuntimeError('Probe ended')
   time.sleep(.1)
  assert child;handle,identity=child;children.append((handle,identity['pid']));pid=identity['pid']
  wait_until(lambda:inspect(pid)['modal'] and inspect(pid)['key']==0x499a40,'source',90)
  hwnd=window(pid)
  if index:test_message(hwnd,0x8392)
  else:key(hwnd,13)
  wait_until(lambda:inspect(pid)['page']==0 and not inspect(pid)['modal'],'login')
  time.sleep(1);test_message(hwnd,0x8390)
  wait_until(lambda:inspect(pid)['page']==7 and inspect(pid)['count']==8,'characters')
  wait_until(lambda:mem(pid,0x62c21c,1)!=b'\0','character initialization');time.sleep(1)
  test_message(hwnd,0x8394);wait_until(lambda:inspect(pid)['page']==40,'channel')
  test_message(hwnd,0x8395);wait_until(lambda:integer(pid,0x64af98)==1,'lobby');time.sleep(1)
  snapshot(pid,'lobby')
 directory('directory create 0 FirstRoom')
 lobby=next(pid for _,pid in children if integer(pid,0x64af98)==1)
 rows=room_rows(lobby);first=next(r for r in rows if r['name']=='FirstRoom');assert first['users']==1 and first['capacity']==4,rows
 directory('directory rename 0 RenamedRoom')
 rows=room_rows(lobby);assert next(r for r in rows if r['id']==first['id'])==dict(first,name='RenamedRoom',capacity=6),rows
 directory('directory leave 0')
 assert all(first['id'] not in {r['id'] for r in room_rows(pid)} for _,pid in children)
 directory('directory create 1 SecondRoom')
 lobby=next(pid for _,pid in children if integer(pid,0x64af98)==1)
 second=next(r for r in room_rows(lobby) if r['name']=='SecondRoom');assert second['id']!=first['id']
 directory('directory leave 1')
 assert all(second['id'] not in {r['id'] for r in room_rows(pid)} for _,pid in children)
 print('PASS native room creation, rename, capacity, independent IDs and both-client removal',flush=True)
 command('pair');wait_until(lambda:all(integer(p,0x64af98)==2 for _,p in children),'pair room')
 command('startpair');wait_until(lambda:all(integer(p,0x64af98)==4 for _,p in children),'both battle',35)
 time.sleep(3)
 for _,pid in children:snapshot(pid,'battle')
 def exercise_step(heading, allow_hidden=False):
  token='step '+str(heading)
  offset=len((run/'server.stdout.log').read_text('utf-8',errors='replace'))
  command(token)
  wait_until(lambda:('MOVEMENT_DONE '+token) in (run/'server.stdout.log').read_text('utf-8',errors='replace')[offset:],'server movement')
  lines=(run/'server.stdout.log').read_text('utf-8',errors='replace')[offset:].splitlines();end=lines.index('MOVEMENT_DONE '+token)
  expected={int(t[1]):(int(t[2]),int(t[3])) for row in lines[:end][-2:] if (t:=row.split())[0]=='POSITION'}
  assert len(expected)==2,expected
  distance=sum(abs(a-b) for a,b in zip(*expected.values()))
  def native_positions_match():
   for _,pid in children:
    own=integer(pid,0x75aa48);seen={}
    for i in range(integer(pid,0x834f08)):
     obj=integer(pid,integer(pid,0x834f10)+i*4);oid=integer(pid,obj+8)
     if oid in expected:seen[oid]=((integer(pid,obj+0x2c)+32768)//2,integer(pid,obj+0x30))
    wanted={own:expected[own]} if allow_hidden and distance>19 else expected
    if allow_hidden and distance==19:
     if seen.get(own)!=expected[own] or any(expected[k]!=v for k,v in seen.items()):return False
    elif seen!=wanted:return False
   return True
  wait_until(native_positions_match,'native coordinates/visibility heading '+str(heading)+' expected '+str(expected),12)
  return distance
 for heading in range(8):exercise_step(heading)
 print('PASS native 8-heading server movement/corrections: both client actor coordinates equal server',flush=True)
 distances=[exercise_step(6,True) for _ in range(6)]
 assert max(distances)>19,distances
 distances += [exercise_step(2,True) for _ in range(6)]
 assert distances[-1]<=18,distances
 print('PASS native bilateral screen exit/removal and reentry/rebuild '+str(distances),flush=True)

 command('draw');wait_until(lambda:all(integer(p,0x64af98)==5 for _,p in children),'both result')
 time.sleep(3)
 for _,pid in children:snapshot(pid,'result')
 if '--admission' in sys.argv or '--settlement' in sys.argv:
  command('summaryone');wait_until(lambda:sorted(integer(p,0x64af98) for _,p in children)==[1,5],'one returned, peer still result')
  early=next(pid for _,pid in children if integer(pid,0x64af98)==1)
  assert all(r['name']!='NativePair' for r in room_rows(early)),room_rows(early)
  snapshot(early,'early-return-lobby')
  print('PASS native A lobby excludes room while B remains on result screen',flush=True)
 command('summary');wait_until(lambda:all(integer(p,0x64af98)==1 for _,p in children),'both returned lobby')
 for _,pid in children:
  snapshot(pid,'returned-lobby');assert all(r['name'] not in ('NativePair','FirstRoom','RenamedRoom','SecondRoom') for r in room_rows(pid)),room_rows(pid)
 command('pair');wait_until(lambda:all(integer(p,0x64af98)==2 for _,p in children),'second room')
 command('startpair');wait_until(lambda:all(integer(p,0x64af98)==4 for _,p in children),'second battle',35)
 time.sleep(3)
 for _,pid in children:snapshot(pid,'second-battle')
 print('PASS: two source players ready/cancel/start; draw F0 -> D0 -> lobby; reenter battle; no player SQL writes',flush=True)
finally:
 (L/'docs/room-state-native.json').write_text(json.dumps({'run':run.name,'snapshots':snapshots},indent=2),'utf-8')
 for p in probes:
  if p.poll() is None:p.terminate()
  p.wait(timeout=10)
 for handle,pid in children:
  assert h.kernel.WaitForSingleObject(handle,5000)==0;h.kernel.CloseHandle(handle)
 if server:
  if server.poll() is None:
   try:command('stop')
   except OSError:server.terminate()
  try:server.wait(timeout=10)
  except subprocess.TimeoutExpired:server.terminate();server.wait(timeout=5)
 shutil.copyfile(L/'app/compat/ddraw.dll',h.CLIENT/'ddraw.dll')
 for stream in streams:stream.close()
 print('Owned fixture closed',run,flush=True)

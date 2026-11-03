"""Owned native UI exercise with synthetic accounts on isolated ports; no SQL writes."""
from pathlib import Path
import ctypes, importlib.util, json, os, shutil, struct, subprocess, sys, time, re
L=Path(__file__).resolve().parents[2];S=L.parent.parent/'lito伺服器/LiTo381-Server'
if '--location' in sys.argv:sys.argv.append('--targets')
if '--targets' in sys.argv:sys.argv.append('--presentation')
defs=(L/'tools/native-flow-build/verify_native_order.py').read_text('utf-8').split("run='native-order-'")[0]
exec(compile(defs,'native-test-helpers','exec'))
spec=importlib.util.spec_from_file_location('helper',L/'src/development_startup.py');h=importlib.util.module_from_spec(spec);spec.loader.exec_module(h)
run=L/'app/runs'/('class-lobby-fixture-'+str(time.time_ns()));run.mkdir()
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
try:
 server=subprocess.Popen([r'C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin\java.exe',
  '-Dfile.encoding=UTF-8', '-cp',str(L/'tools/class_compat/build')+';'+os.environ.get('LITO_TEST_JAR',str(L/'app/server/LiTo381-SharedResources-candidate.jar'))+';'+str(S/'lib/*'),
  'local.lito381.server.NativeClassFixture',str(run/'server.properties'),str(run/'capture')],
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
 pid=identity['pid'];state=wait_state(pid,lambda s:s['modal'] and s['key']==0x499a40,90);hwnd=window(pid)
 key(hwnd,13)
 state=wait_state(pid,lambda s:s['page']==0 and not s['modal'])
 time.sleep(1)
 assert 'C_LOGIN ' not in (run/'server.stdout.log').read_text('utf-8',errors='replace')
 print('Fixture login ready; no unsolicited C_LOGIN',flush=True)
 outcomes=[]
 for job,sex in ([(6,0)] if '--presentation' in sys.argv else [(5,0)] if '--dragon' in sys.argv else [(3,0),(5,0)] if '--legacy' in sys.argv else [(5,0),(5,1),(6,0),(6,1),(3,0),(5,0)]):
  server.stdin.write(f'class {job} {sex}\n');server.stdin.flush()
  deadline=time.monotonic()+5
  while f'CLASS_READY {job} {sex}' not in (run/'server.stdout.log').read_text('utf-8',errors='replace'):
   assert time.monotonic()<deadline;time.sleep(.1)
  test_message(hwnd,0x8390)
  state=wait_state(pid,lambda s:s['page']==7 and s['count']==8,30)
  deadline=time.monotonic()+10
  while mem(pid,0x62FACD,2)!=bytes([job,sex]) and time.monotonic()<deadline:time.sleep(.1)
  assert mem(pid,0x62FACD,2)==bytes([job,sex]),mem(pid,0x62FACD,2).hex()
  deadline=time.monotonic()+15
  while mem(pid,0x62C21C,1)==b'\0' and time.monotonic()<deadline:time.sleep(.1)
  assert mem(pid,0x62C21C,1)!=b'\0','Native page-7 handoff not ready'
  test_message(hwnd,0x8394)
  wait_state(pid,lambda s:s['page']==40,20)
  test_message(hwnd,0x8395)
  deadline=time.monotonic()+15
  while integer(pid,0x64af98)!=1 and time.monotonic()<deadline:time.sleep(.1)
  assert integer(pid,0x64af98)==1
  time.sleep(2)
  assert mem(pid,0x64AC26,2)==bytes([job,sex])
  descriptions=integer(pid,0x647E20)
  assert descriptions>=220,('shared spell description count',descriptions)
  skill_list=integer(pid,0x834888);slots=integer(pid,skill_list+0x58)
  loaded=[]
  for i in range(integer(pid,skill_list+0x2C)):
   obj=integer(pid,slots+4*i)
   if obj and integer(pid,obj+4)!=0xffffffff:loaded.append(integer(pid,obj+4)+1)
  expected=set(range(1,9) if job<5 else range(181,196) if job==5 else range(201,221))
  print('SKILLS',job,sorted(loaded),'descriptions',descriptions,flush=True)
  if not expected.issubset(loaded):
   print('PALETTE',mem(pid,integer(pid,0x635B80),64).hex(),'ICONS',mem(pid,integer(pid,0x4AEEE5),32).hex(),'ORIGINAL',mem(pid,0x508648,32).hex(),flush=True)
  assert expected.issubset(loaded),(job,sorted(loaded))
  assert set(loaded)==expected,(job,loaded)
  names=integer(pid,0x647E24);icon_table=integer(pid,0x4AEEE5)
  for skill in expected:
   text=mem(pid,integer(pid,names+(skill-1)*4),96).split(b'\0')[0]
   assert text and b'?' not in text
   icon=int.from_bytes(mem(pid,icon_table+(skill-1)*2,2),'little')
   assert 0<icon<integer(pid,0x648ECC)
   assert integer(pid,integer(pid,0x648EC8)+icon*4)

  hwnd=window(pid)
  marker='fixture lobby all 14 class portraits rendered=1'
  renderlog=h.CLIENT/'lito381-compat.log'
  before=renderlog.read_text('utf-8',errors='replace').count(marker)
  assert test_message(hwnd,0x8396)
  assert test_message(hwnd,0x8399)
  if job==5 or '--presentation' in sys.argv:
   combo_before=renderlog.read_text('utf-8',errors='replace').count('fixture dragon combo all passed=1')
   assert test_message(hwnd,0x839c)
   assert renderlog.read_text('utf-8',errors='replace').count('fixture dragon combo all passed=1')==combo_before+1
   status_before=renderlog.read_text('utf-8',errors='replace').count('fixture dragon native status all passed=1')
   assert test_message(hwnd,0x839b)
   assert renderlog.read_text('utf-8',errors='replace').count('fixture dragon native status all passed=1')==status_before+1
   assert test_message(hwnd,0x839a)
   assert 'fixture dragon target picker all passed=1' in renderlog.read_text('utf-8',errors='replace')
  deadline=time.monotonic()+5
  while renderlog.read_text('utf-8',errors='replace').count(marker)==before and time.monotonic()<deadline:time.sleep(.1)
  assert probe.poll() is None
  assert renderlog.read_text('utf-8',errors='replace').count(marker)==before+1
  assert ('fixture skill native icons rendered='+str(len(expected))) in renderlog.read_text('utf-8',errors='replace')
  outcomes.append({'type':job,'sex':sex,'lobby_rendered':True,'loaded_skill_ids':sorted(loaded),'native_icons_rendered':True})
  print('PASS native lobby',outcomes[-1],flush=True)
  if '--presentation' in sys.argv:
   if '--location' in sys.argv:
    check_location(pid,hwnd,True)
    server.stdin.write('room\n');server.stdin.flush()
    deadline=time.monotonic()+15
    while integer(pid,0x64af98)!=2 and time.monotonic()<deadline:time.sleep(.1)
    assert integer(pid,0x64af98)==2
    check_location(pid,hwnd,False)
   test_message(hwnd,0x839d)
   assert 'fixture self-name installed branch absent/other/self/null passed=1' in renderlog.read_text('utf-8',errors='replace')
   assert 'fixture nine illusion native status icons/render/tooltips/remove passed=1' in renderlog.read_text('utf-8',errors='replace')
   server.stdin.write('world\n');server.stdin.flush()
   deadline=time.monotonic()+25
   while not integer(pid,0x834ee4) and time.monotonic()<deadline:time.sleep(.1)
   actor=integer(pid,0x834ee4);assert actor,'world actor missing'
   time.sleep(2)
   animation=[]
   for walk in (() if '--targets' in sys.argv else (0,58)):
    server.stdin.write(f'empty {walk}\n');server.stdin.flush()
    samples=[];deadline=time.monotonic()+4
    while time.monotonic()<deadline:
     samples.append(list(mem(pid,actor+0x10,12)));time.sleep(.01)
    actions=sorted(set(x[0] for x in samples));frames=sorted(set((x[0],x[3]) for x in samples))
    print('EMPTY_NATIVE',walk,'actions',actions,'frames',frames,flush=True)
    animation.append({'walk':walk,'actions':actions,'action_frames':frames})
    assert walk+1 in actions,('attack action missing',walk,actions)
    assert len(set(x[3] for x in samples if x[0]==walk+1))>1,('attack frames not advancing',walk,frames)
   if '--targets' in sys.argv:
    test_message(hwnd,0x839e)
    deadline=time.monotonic()+8
    while 'Illusion explicit target skill=204' not in (run/'server.stdout.log').read_text('utf-8',errors='replace') and time.monotonic()<deadline:time.sleep(.1)
    assert 'Illusion explicit target skill=204' in (run/'server.stdout.log').read_text('utf-8',errors='replace')
    packets=[json.loads(row) for p in (run/'capture').rglob('frames.jsonl') for row in p.read_text('utf-8').splitlines()]
    casts=[r for r in packets if r.get('plain','').startswith('14 19 03 ')]
    assert any(len(bytes.fromhex(r['plain']))==11 for r in casts),casts
    cycles=[]
    for cycle in range(2):
     server.stdin.write('result\n');server.stdin.flush()
     deadline=time.monotonic()+15
     while integer(pid,0x64af98)!=5 and time.monotonic()<deadline:time.sleep(.1)
     assert integer(pid,0x64af98)==5,'result phase missing'
     server.stdin.write('summary\n');server.stdin.flush()
     deadline=time.monotonic()+15
     while integer(pid,0x64af98)!=2 and time.monotonic()<deadline:time.sleep(.1)
     assert integer(pid,0x64af98)==2,'summary phase missing'
     time.sleep(1)
     rect=W.RECT();u.GetClientRect(hwnd,ctypes.byref(rect))
     x=560*rect.right//640;y=452*rect.bottom//480;point=x|(y<<16)
     u.PostMessageW(hwnd,0x200,0,point);u.PostMessageW(hwnd,0x201,1,point);u.PostMessageW(hwnd,0x202,0,point)
     deadline=time.monotonic()+15
     while integer(pid,0x64af98)!=1 and time.monotonic()<deadline:time.sleep(.1)
     assert integer(pid,0x64af98)==1,'native summary Confirm failed to return lobby'
     if '--location' in sys.argv:check_location(pid,hwnd,True)
     server.stdin.write('world\n');server.stdin.flush()
     deadline=time.monotonic()+25
     while integer(pid,0x64af98)!=4 and time.monotonic()<deadline:time.sleep(.1)
     assert integer(pid,0x64af98)==4,'world reentry failed'
     cycles.append({'result':5,'summary':2,'native_confirm_lobby':1,'world':4})
    (L/'docs/illusion-targets-native.json').write_text(json.dumps({'run':run.name,'picker_ids':21,'native_target_packet_bytes':11,'summary_cycles':cycles,'animation':animation,'no_player_sql_writes':True},indent=2),encoding='utf-8')
    if '--location' in sys.argv:
     (L/'docs/profile-location-native.json').write_text(json.dumps({'run':run.name,'locations':locations,'summary_cycles':cycles,'native_draw_hook':True,'no_player_sql_writes':True},indent=2),encoding='utf-8')
   else:
    (L/'docs/illusion-presentation-native.json').write_text(json.dumps({'run':run.name,'animation':animation,'nine_buff_icons':True,'self_name_hover_branch':True,'no_player_sql_writes':True},indent=2),encoding='utf-8')
   break
  test_message(hwnd,0x8397)
  wait_state(pid,lambda s:s['page']==40 and integer(pid,0x64af98)==0,20)
  test_message(hwnd,0x8398)
  wait_state(pid,lambda s:s['page']==7,20)
  test_message(hwnd,0x8393)
  state=wait_state(pid,lambda s:s['page']==0 and not s['modal'],20)
 result={'variants':outcomes,'all_14_lobby_portraits':True,'same_window':window(pid)==hwnd,'no_player_sql_writes':True,'run':run.name}
 (L/'docs/class-skills-native-verification.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
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

"""Owned native processes, shared formal JVM, no account login or player writes."""
from pathlib import Path
import ctypes,json,queue,subprocess,sys,threading,time
from ctypes import wintypes as W
L=Path(__file__).resolve().parents[2]
run='multi-window-'+str(time.time_ns());q=queue.Queue();events=[]
err=(L/'docs/multi-window.stderr.log').open('w',encoding='utf-8')
entry=L/'tools/caption-diagnostic/development_startup.py'
p=subprocess.Popen([sys.executable,'-B','-X','utf8',str(entry),'--serve','--run-id',run,'--client-directory','D:/lineage開發/0-客戶端','--scale','100','--service','StandardArena','--source-id','1'],stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=err,text=True,encoding='utf-8',creationflags=subprocess.CREATE_NO_WINDOW)
def reader():
 for line in p.stdout:q.put(json.loads(line))
 q.put(None)
threading.Thread(target=reader,daemon=True).start()
def event(kind):
 deadline=time.monotonic()+145
 while time.monotonic()<deadline:
  try:e=q.get(timeout=1)
  except queue.Empty:continue
  assert e is not None,'helper EOF'
  events.append(e);print(e.get('event'),e.get('pid',''),flush=True)
  assert e['event'] not in ('failed','finished'),e
  if e['event']==kind:return e
 raise TimeoutError(kind)
u=ctypes.WinDLL('user32');k=ctypes.WinDLL('kernel32')
k.OpenProcess.argtypes=[W.DWORD,W.BOOL,W.DWORD];k.OpenProcess.restype=W.HANDLE
k.WaitForSingleObject.argtypes=[W.HANDLE,W.DWORD];k.CloseHandle.argtypes=[W.HANDLE]
u.IsWindowVisible.argtypes=[W.HWND];u.IsWindowVisible.restype=W.BOOL
u.PostMessageW.argtypes=[W.HWND,W.UINT,W.WPARAM,W.LPARAM]
callback=ctypes.WINFUNCTYPE(W.BOOL,W.HWND,W.LPARAM);u.EnumWindows.argtypes=[callback,W.LPARAM]
u.GetWindowThreadProcessId.argtypes=[W.HWND,ctypes.POINTER(W.DWORD)]
def close_window(pid):
 found=[]
 @callback
 def visit(hwnd,param):
  owner=W.DWORD();u.GetWindowThreadProcessId(hwnd,ctypes.byref(owner))
  if owner.value==pid and u.IsWindowVisible(hwnd):found.append(hwnd)
  return True
 u.EnumWindows(visit,0);assert found,('no owned window',pid)
 for hwnd in found:u.PostMessageW(hwnd,0x10,0,0)
handles=[];result={'run':run,'no_player_sql_writes':True}
try:
 ready=event('ready');first=event('native_created')['identity']['pid'];event('resources_ready')
 p.stdin.write('{"command":"add_client"}\n');p.stdin.flush()
 second=event('additional_client_result');assert second['success'],second
 assert first!=second['pid']
 for pid in (first,second['pid'],ready['instances'][0]['identity']['pid']):
  h=k.OpenProcess(0x100000,False,pid);assert h;handles.append(h)
 assert all(k.WaitForSingleObject(h,0)==258 for h in handles)
 result.update(first_pid=first,second_pid=second['pid'],single_server_pid=ready['instances'][0]['identity']['pid'],both_resources_ready=True)
 if '--caption-check' in sys.argv:
  receipt=L/'app/runs'/run/'caption-check.json'
  print('CAPTION_CHECK_READY '+str(receipt),flush=True)
  deadline=time.monotonic()+300
  while not receipt.exists():
   assert p.poll() is None,'helper stopped during caption check'
   if time.monotonic()>deadline:raise TimeoutError('caption verification receipt')
   time.sleep(.25)
  result['caption_check']=json.loads(receipt.read_text('utf-8'))
 if '--stop-all' in sys.argv:
  p.stdin.write('{"command":"stop"}\n');p.stdin.flush();p.wait(timeout=35)
  assert all(k.WaitForSingleObject(h,1000)==0 for h in handles),'owned game/server survived stop'
  result['stop_all_closed_both_clients_and_server']=True
 else:
  close_window(first)
  assert k.WaitForSingleObject(handles[0],15000)==0,'first window did not close'
  time.sleep(1)
  assert p.poll() is None and all(k.WaitForSingleObject(h,0)==258 for h in handles[1:]),'closing first stopped peer/server'
  result['first_close_preserved_peer_and_server']=True
  close_window(second['pid'])
  assert k.WaitForSingleObject(handles[1],15000)==0,'second window did not close'
  p.wait(timeout=25)
  result['all_closed_auto_cleanup']=True
finally:
 if p.poll() is None:p.stdin.write('{"command":"stop"}\n');p.stdin.flush()
 p.wait(timeout=35)
 for h in handles:k.CloseHandle(h)
 err.close()
 terminal=json.loads((L/'app/runs'/run/'terminal.json').read_text('utf-8'))
 result.update(helper_exit=p.returncode,cleanup_verified=terminal['cleanup_verified'],additional_clients=terminal.get('additional_clients',[]))
 (L/('docs/caption-multi-window-test.json' if '--caption-check' in sys.argv else 'docs/multi-window-stop-test.json' if '--stop-all' in sys.argv else 'docs/caption-exit-diagnostic.json')).write_text(json.dumps(result,indent=2),encoding='utf-8')
assert p.returncode==0 and result['cleanup_verified'],result
print('PASS',json.dumps(result),flush=True)

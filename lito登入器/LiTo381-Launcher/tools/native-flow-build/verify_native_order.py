"""Exercise only the owned candidate window; never type account credentials."""
from pathlib import Path
import ctypes, json, queue, subprocess, sys, threading, time
from ctypes import wintypes as W
L=Path(__file__).resolve().parents[2]
k=ctypes.WinDLL('kernel32',use_last_error=True);u=ctypes.WinDLL('user32',use_last_error=True)
k.OpenProcess.argtypes=[W.DWORD,W.BOOL,W.DWORD];k.OpenProcess.restype=W.HANDLE
k.ReadProcessMemory.argtypes=[W.HANDLE,ctypes.c_void_p,ctypes.c_void_p,ctypes.c_size_t,ctypes.c_void_p]
k.CloseHandle.argtypes=[W.HANDLE]
u.PostMessageW.argtypes=[W.HWND,W.UINT,W.WPARAM,W.LPARAM]
callback=ctypes.WINFUNCTYPE(W.BOOL,W.HWND,W.LPARAM)
def inspect(pid):
    h=k.OpenProcess(0x410,False,pid)
    if not h: raise ctypes.WinError(ctypes.get_last_error())
    def read(a,n=4):
        b=ctypes.create_string_buffer(n)
        if not k.ReadProcessMemory(h,a,b,n,None):return 0
        return int.from_bytes(b.raw,'little')
    try:
        root=read(0x75f6e8)
        return {'root':root,'page':read(root+12) if root else None,'count':read(root+4) if root else 0,
                'modal':read(0x75ff7c),'callback':read(0x75ff78),'key':read(0x62c9b8),
                'source_count':read(0x75f6e4,1)}
    finally:k.CloseHandle(h)
def window(pid):
    found=[]
    @callback
    def visit(hwnd,param):
        owner=W.DWORD();u.GetWindowThreadProcessId(hwnd,ctypes.byref(owner))
        if owner.value==pid and u.IsWindowVisible(hwnd):found.append(hwnd)
        return True
    u.EnumWindows(visit,0)
    if not found:raise RuntimeError('Owned visible game window missing')
    return found[0]
run='native-order-'+str(time.time_ns());q=queue.Queue();events=[]
with (L/'tools/native-flow-build/candidate.stderr.log').open('w',encoding='utf-8') as err:
    p=subprocess.Popen([sys.executable,'-B','-X','utf8',str(L/'release/current/development_startup.py'),
        '--serve','--run-id',run,'--client-directory',r'D:\lineage開發\0-客戶端',
        '--scale','200','--service','StandardArena','--source-id','1'],stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,stderr=err,text=True,encoding='utf-8',creationflags=subprocess.CREATE_NO_WINDOW)
    def reader():
        for line in p.stdout:q.put(line)
        q.put(None)
    threading.Thread(target=reader,daemon=True).start()
    pid=None;selected=False;passed=False;deadline=time.monotonic()+100;last=None
    try:
        while time.monotonic()<deadline:
            try:line=q.get(timeout=.25)
            except queue.Empty:line=''
            if line is None:break
            if line:
                event=json.loads(line);events.append(event);print(event,flush=True)
                if event['event'] in ('native_created','session_native_created'):pid=event['identity']['pid']
                if event['event']=='failed':break
            if pid:
                state=inspect(pid)
                if state!=last:print('native UI',state,flush=True);last=state
                if not selected and state['modal'] and state['key']==0x499a40 and state['source_count']==3:
                    hwnd=window(pid)
                    # Native source selector handles Enter using its focused row.
                    u.PostMessageW(hwnd,0x100,13,0);u.PostMessageW(hwnd,0x101,13,0)
                    selected=True;print('Selected native source row with Enter (PID retained)',flush=True)
                elif selected and state['root'] and state['page']==0 and state['count']==5 and not state['modal']:
                    passed=True;break
    finally:
        if p.poll() is None:p.stdin.write('{"command":"stop"}\n');p.stdin.flush()
        p.wait(timeout=35)
    terminal=json.loads((L/'app/runs'/run/'terminal.json').read_text('utf-8'))
    result={'run':run,'native_source_first':selected,'blank_login_after_source':passed,
        'cleanup_verified':terminal['cleanup_verified'],'native_pid':pid,'process_count':sum(e.get('event') in ('native_created','session_native_created') for e in events)}
    (L/'tools/native-flow-build/verification.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
    print(json.dumps(result),flush=True)
    assert passed and result['cleanup_verified'] and result['process_count']==1,result

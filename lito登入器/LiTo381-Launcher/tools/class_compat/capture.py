"""Capture owned, unpacked LiTo code for class UI analysis; no login or SQL writes."""
from pathlib import Path
import ctypes, hashlib, json, queue, subprocess, sys, threading, time
from ctypes import wintypes as W
L=Path(__file__).resolve().parents[2]
out=L/'app/runs/class-compat-audit'
out.mkdir(exist_ok=True)
run='class-audit-'+str(time.time_ns())
q=queue.Queue()
with (out/'stderr.log').open('w',encoding='utf-8') as err:
    p=subprocess.Popen([sys.executable,'-B','-X','utf8',str(L/'release/current/development_startup.py'),
        '--serve','--run-id',run,'--client-directory',r'D:\lineage開發\0-客戶端',
        '--scale','200','--service','StandardArena','--source-id','1'],
        stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=err,text=True,encoding='utf-8',creationflags=subprocess.CREATE_NO_WINDOW)
    def reader():
        for line in p.stdout:q.put(line)
        q.put(None)
    threading.Thread(target=reader,daemon=True).start()
    try:
        deadline=time.monotonic()+90
        while time.monotonic()<deadline:
            line=q.get(timeout=30)
            if line is None:raise RuntimeError('Startup ended')
            e=json.loads(line);print(e.get('event'),flush=True)
            if e.get('event')=='failed':raise RuntimeError(e['reason'])
            if e.get('event')!='resources_ready':continue
            k=ctypes.WinDLL('kernel32',use_last_error=True)
            k.OpenProcess.argtypes=[W.DWORD,W.BOOL,W.DWORD];k.OpenProcess.restype=W.HANDLE
            k.CloseHandle.argtypes=[W.HANDLE]
            k.ReadProcessMemory.argtypes=[W.HANDLE,W.LPCVOID,W.LPVOID,ctypes.c_size_t,ctypes.POINTER(ctypes.c_size_t)]
            h=k.OpenProcess(0x1010,False,e['pid'])
            if not h:raise ctypes.WinError(ctypes.get_last_error())
            try:
                records=[]
                for address,size in [(0x400000,0xF8000),(0x4F8000,0x148000),(0x834000,0xC000)]:
                    b=ctypes.create_string_buffer(size);n=ctypes.c_size_t()
                    if not k.ReadProcessMemory(h,address,b,size,ctypes.byref(n)):raise ctypes.WinError(ctypes.get_last_error())
                    data=b.raw[:n.value];file=out/f'{address:08x}.bin';file.write_bytes(data)
                    records.append({'address':address,'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest()})
                (out/'capture.json').write_text(json.dumps({'run':run,'pid':e['pid'],'regions':records},indent=2),encoding='utf-8')
                print('Captured owned code and UI tables',flush=True)
            finally:k.CloseHandle(h)
            break
        else:raise TimeoutError('No resources_ready')
    finally:
        if p.poll() is None:p.stdin.write('{"command":"stop"}\n');p.stdin.flush()
        p.wait(timeout=30)
        terminal=json.loads((L/'app/runs'/run/'terminal.json').read_text('utf-8'))
        assert terminal['cleanup_verified'],terminal
        print('Owned cleanup verified',flush=True)

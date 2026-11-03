"""Read-only, identity-checked snapshot of the owned native UI test."""
from pathlib import Path
import ctypes, hashlib, json, sys
from ctypes import wintypes as W

R = Path(__file__).resolve().parent
L = R.parents[3] / 'lito登入器/LiTo381-Launcher'
run = sys.argv[1]
assert Path(run).name == run and run.startswith('buff-schema-native-')
identity = json.loads((L/'app/runs'/run/'native-created.json').read_text(encoding='utf8'))['native_identity']
k = ctypes.WinDLL('kernel32', use_last_error=True)
k.OpenProcess.argtypes = [W.DWORD,W.BOOL,W.DWORD]; k.OpenProcess.restype = W.HANDLE
k.CloseHandle.argtypes = [W.HANDLE]
k.GetProcessTimes.argtypes = [W.HANDLE,*([ctypes.POINTER(W.FILETIME)]*4)]
k.QueryFullProcessImageNameW.argtypes = [W.HANDLE,W.DWORD,W.LPWSTR,ctypes.POINTER(W.DWORD)]
k.ReadProcessMemory.argtypes = [W.HANDLE,W.LPCVOID,W.LPVOID,ctypes.c_size_t,ctypes.POINTER(ctypes.c_size_t)]
h = k.OpenProcess(0x1010,False,identity['pid'])
if not h: raise ctypes.WinError(ctypes.get_last_error())
def read(a,n):
    b=ctypes.create_string_buffer(n); count=ctypes.c_size_t()
    if not k.ReadProcessMemory(h,a,b,n,ctypes.byref(count)): raise ctypes.WinError(ctypes.get_last_error())
    assert count.value==n
    return b.raw
def u32(a): return int.from_bytes(read(a,4),'little')
try:
    times=[W.FILETIME() for _ in range(4)]
    assert k.GetProcessTimes(h,*map(ctypes.byref,times))
    assert times[0].dwLowDateTime+(times[0].dwHighDateTime<<32)==identity['filetime']
    image=ctypes.create_unicode_buffer(32768); length=W.DWORD(len(image))
    assert k.QueryFullProcessImageNameW(h,0,image,ctypes.byref(length))
    assert image.value.casefold()==identity['path'].casefold()
    assert hashlib.sha256(Path(image.value).read_bytes()).hexdigest().upper()==identity['image_sha256']
    root=u32(0x75F6E8)
    data={'run':run,'pid':identity['pid'],'page':u32(root+12) if root else None,
          'portrait_sha256':hashlib.sha256(read(0x62FD70,0x420)).hexdigest(),
          'native_rows':[{'name_bytes':len(read(0x62FAA0+i*180,28).split(b'\0')[0]),
                          'type':read(0x62FAA0+i*180+0x2D,1)[0]} for i in range(4)],
          'count_call':read(0x4B3177,5).hex(),'row_call':read(0x4B3095,5).hex()}
    baseline=R/'build'/(run+'-portraits-before.json')
    if not baseline.exists():
        assert data['page']!=7,'Capture portrait baseline before character selection'
        baseline.write_text(json.dumps(data,indent=2),encoding='utf8')
    before=json.loads(baseline.read_text(encoding='utf8'))
    data['portraits_preserved']=data['portrait_sha256']==before['portrait_sha256']
    (R/'build'/(run+'-native-latest.json')).write_text(json.dumps(data,indent=2),encoding='utf8')
    print(json.dumps(data))
    assert data['portraits_preserved']
finally:k.CloseHandle(h)

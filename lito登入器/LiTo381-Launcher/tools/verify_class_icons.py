"""Read-only check of the published archive and pixels in an owned game."""
import ctypes,hashlib,json,struct
from pathlib import Path
L=Path(__file__).resolve().parent.parent

def verify(pid):
    root=L/'app/resources/class-icons'
    receipt=json.loads((root/'resources.json').read_text('utf-8'))
    for item in receipt['package']+receipt['original']:
        assert hashlib.sha256(Path(item['path']).read_bytes()).hexdigest()==item['sha256']
    idx=(root/'LTicon.idx').read_bytes();pak=(root/'LTicon.pak').read_bytes();members={}
    for i in range(struct.unpack_from('<I',idx)[0]):
        start,name,size=struct.unpack_from('<I20sI',idx,4+28*i)
        members[name.split(b'\0')[0].lower()]=pak[start:start+size]
    k=ctypes.WinDLL('kernel32',use_last_error=True);k.OpenProcess.restype=ctypes.c_void_p
    k.ReadProcessMemory.argtypes=[ctypes.c_void_p,ctypes.c_void_p,ctypes.c_void_p,ctypes.c_size_t,ctypes.c_void_p]
    k.CloseHandle.argtypes=[ctypes.c_void_p];h=k.OpenProcess(0x410,False,pid)
    if not h:raise ctypes.WinError(ctypes.get_last_error())
    def read(a,n):
        b=ctypes.create_string_buffer(n);done=ctypes.c_size_t()
        if not k.ReadProcessMemory(h,a,b,n,ctypes.byref(done)) or done.value!=n:raise RuntimeError('Native read failed')
        return b.raw
    def word(a):return struct.unpack('<I',read(a,4))[0]
    try:
        assert word(0x64a95c)==243,'Class icon table not ready'
        table=word(0x64a964);assert table
        convert=bool(read(0x641298,1)[0])
        for id in range(233,243):
            raw=bytearray(members[f'{id}.lbt'.encode()]);pos=4
            if convert:
                for y in range(raw[3]):
                    spans=raw[pos];pos+=1
                    for _ in range(spans):
                        count=raw[pos+1];pos+=2
                        for _ in range(count):
                            v=struct.unpack_from('<H',raw,pos)[0]
                            struct.pack_into('<H',raw,pos,(v+(v&0x7fe0))&65535);pos+=2
            assert read(word(table+id*4),len(raw))==bytes(raw),f'Wrong icon pixels {id}'
        for site in (0x4633b0,0x463646,0x463b01):assert read(site,1)==b'\xe9'
        path=read(word(0x42ef23),230).split(b'\0')[0].decode('cp950')
        client=Path('D:/lineage開發/0-客戶端')
        assert (client/path).resolve()==(root/'LTicon').resolve()
        assert not (client/'ddraw.dll').exists()
        assert hashlib.sha256((client/'litoddraw.dll').read_bytes()).digest()==hashlib.sha256((L/'app/compat/litoddraw.dll').read_bytes()).digest()
        return {'count':243,'exact_new_icon_pixels':10,'render_hooks':3,'formal_archive':True,'no_generic_client_ddraw':True}
    finally:k.CloseHandle(h)

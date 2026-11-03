"""Read-only inspection of the owned game, including exact native icon pixels."""
from pathlib import Path
import ctypes,hashlib,json,struct,sys
R=Path(__file__).resolve().parent;L=R.parents[1];C=R/'build/class-icons'

def converted(raw):
    data=bytearray(raw);pos=4
    for y in range(data[3]):
        spans=data[pos];pos+=1
        for _ in range(spans):
            count=data[pos+1];pos+=2
            for _ in range(count):
                v=struct.unpack_from('<H',data,pos)[0]
                struct.pack_into('<H',data,pos,(v+(v&0x7fe0))&65535);pos+=2
    assert pos==len(data)
    return bytes(data)

def main():
    run=sys.argv[1];meta=json.loads((L/'app/runs'/run/'native-created.json').read_text('utf-8'))
    pid=meta['native_identity']['pid'];k=ctypes.WinDLL('kernel32',use_last_error=True)
    k.OpenProcess.restype=ctypes.c_void_p
    k.ReadProcessMemory.argtypes=[ctypes.c_void_p,ctypes.c_void_p,ctypes.c_void_p,ctypes.c_size_t,ctypes.c_void_p]
    k.CloseHandle.argtypes=[ctypes.c_void_p]
    h=k.OpenProcess(0x410,False,pid)
    if not h:raise RuntimeError('Owned game not running')
    def read(a,n):
        b=ctypes.create_string_buffer(n);done=ctypes.c_size_t()
        if not k.ReadProcessMemory(h,a,b,n,ctypes.byref(done)) or done.value!=n:
            raise RuntimeError('Native read failed '+hex(a))
        return b.raw
    def word(a):return struct.unpack('<I',read(a,4))[0]
    try:
        count=word(0x64a95c);table=word(0x64a964)
        if count!=243 or not table:raise RuntimeError(f'Native LTicon table not ready: count={count} table={table:x}')
        conversion=bool(read(0x641298,1)[0]);assets=json.loads((C/'resources.json').read_text('utf-8'))
        checks=[]
        for asset in assets['assets']:
            raw=Path(asset['lbt']['path']).read_bytes();expected=converted(raw) if conversion else raw
            pointer=word(table+asset['id']*4)
            assert pointer and read(pointer,len(expected))==expected, f"Wrong native pixels: {asset['id']}"
            checks.append({'id':asset['id'],'pointer':pointer,'pixel_sha256':hashlib.sha256(expected).hexdigest()})
        for address in (0x4633b0,0x463646,0x463b01):assert read(address,1)==b'\xe9'
        archive_path=read(word(0x42ef23),230).split(b'\0')[0].decode('cp950')
        assert (Path(meta['runtime_client']).parent/archive_path).resolve()==(C/'resources/LTicon').resolve()
        assert not (Path(meta['runtime_client']).parent/'ddraw.dll').exists()
        result={'status':'native-icon-pixels-and-hooks-verified','run':run,'pid':pid,'count':count,
                'rgb565_conversion':conversion,'assets':checks,'archive_path':archive_path,
                'formal_updated':False,'onscreen_roster_verified':False}
        (C/(run+'-live.json')).write_text(json.dumps(result,indent=2),'utf-8')
        print('PASS native table 243; all 10 new icon pixels exact; 3 rendering hooks; no client ddraw.dll; pid='+str(pid))
    finally:k.CloseHandle(h)
if __name__=='__main__':main()

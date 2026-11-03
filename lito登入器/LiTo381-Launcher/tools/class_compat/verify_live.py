"""Read-only verification of the explicitly owned smoke-test process."""
import ctypes, struct
from ctypes import wintypes as W

def verify(pid):
    k=ctypes.WinDLL('kernel32',use_last_error=True)
    k.OpenProcess.argtypes=[W.DWORD,W.BOOL,W.DWORD];k.OpenProcess.restype=W.HANDLE
    k.CloseHandle.argtypes=[W.HANDLE]
    k.ReadProcessMemory.argtypes=[W.HANDLE,W.LPCVOID,W.LPVOID,ctypes.c_size_t,ctypes.POINTER(ctypes.c_size_t)]
    handle=k.OpenProcess(0x1010,False,pid)
    if not handle:raise ctypes.WinError(ctypes.get_last_error())
    def read(address,size):
        b=ctypes.create_string_buffer(size);count=ctypes.c_size_t()
        if not k.ReadProcessMemory(handle,address,b,size,ctypes.byref(count)):raise ctypes.WinError(ctypes.get_last_error())
        assert count.value==size
        return b.raw
    def u32(address):return struct.unpack('<I',read(address,4))[0]
    try:
        assert read(0x493DF0,1)==b'\xe9' and read(0x494E00,1)==b'\xe9'
        assert read(0x459E84,1)==b'\xe9','Missing seven-class lobby draw bridge'
        assert read(0x45A62D,1)==b'\xe9','Missing native profile location presentation'
        forced_send=read(0x4c6b27,5)
        assert forced_send[0]==0xe8 and forced_send!=bytes.fromhex('e844fcffff'),'Missing explicit Ctrl combat sender'
        forced_target=0x4c6b2c+struct.unpack('<i',forced_send[1:])[0]
        assert not 0x400000<=forced_target<0x900000 and read(forced_target,1)!=b'\0','Invalid Ctrl send bridge'
        for address in (0x415E4E,0x415EBB,0x4D4AB7):assert u32(address)==1128
        for address in (0x4D4A7E,0x4D4B2A):assert u32(address)==1128*4
        assert read(0x492B2E,1)==b'\xe9','Missing new-class skill palette bridge'
        assert u32(0x4AEEE5)!=0x508648,'Missing extended skill icon table'
        # Legacy palette copy loops retain both their native begin and end.
        for a in (0x43F74D,0x43F785,0x43F827,0x43F85F):assert u32(a)==0x508648
        table=u32(0x834BD0);assert table
        for i in range(1128):
            obj=u32(table+i*4);assert obj
            assert u32(obj)==0x4FA738 and u32(obj+8)==i,(i,hex(obj))
        count=u32(0x834BC0);text=u32(0x834BC4);assert count>1305
        names=[]
        for i in (1302,1303,1304,1305):
            names.append(read(u32(text+i*4),32).split(b'\0')[0].decode('cp950'))
        assert names==['龍騎士','龍騎士','幻術士','幻術士'],names
        return {'png_objects':1128,'seven_class_hooks':True,'lobby_draw_hook':True,'new_class_names':names,'class_skill_hooks':True,'forced_combat_send_hook':hex(forced_target)}
    finally:k.CloseHandle(handle)

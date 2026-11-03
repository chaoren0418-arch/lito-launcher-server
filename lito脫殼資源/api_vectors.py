"""Pure address-conversion tests in this owned Python subprocess. No socket/network/game."""
from audit import *
import ctypes, sys
libpath=Path('C:/Windows/System32/ws2_32.dll')
lib=ctypes.WinDLL(str(libpath))
lib.inet_addr.argtypes=[ctypes.c_char_p];lib.inet_addr.restype=ctypes.c_uint32
lib.inet_ntoa.argtypes=[ctypes.c_uint32];lib.inet_ntoa.restype=ctypes.c_void_p
inputs=[b'127.0.0.1',b'127.0.0.2',b'1.2.3.4',b'',b' ',b'255.255.255.255',b'256.0.0.1',b'invalid',b'127.1',b'0177.0.0.1',b'0x7f.0.0.1',None]
rows=[]
for text in inputs:
    val=lib.inet_addr(text);ptr=lib.inet_ntoa(val)
    rows.append({'input':text.decode()if text is not None else None,'returned_u32':f'{val:08X}','memory_network_bytes':struct.pack('<I',val).hex(),'inet_ntoa_copy':ctypes.string_at(ptr).decode('ascii')if ptr else None})
assert rows[0]['returned_u32']=='0100007F' and rows[1]['returned_u32']=='0200007F' and rows[2]['returned_u32']=='04030201'
assert rows[3]['returned_u32']=='FFFFFFFF' and rows[6]['returned_u32']=='FFFFFFFF'
write('api-vectors.json',{'updated_at':stamp(),'test_process':{'pid':os.getpid(),'image':sys.executable,'architecture_bits':struct.calcsize('P')*8,'owned_script':record(Path(__file__))},'system_DLL':record(libpath),'vectors':rows,'checks_passed':True,'scope':'Pure system-library conversion in owned Python; 64-bit current OS ABI/semantics only; NOT historical32-bit DLL body identity or original runtime evidence. No original game/provider process opened, no socket created, no DNS call, no GUI.','inet_ntoa_lifetime':'Returned buffer must be copied immediately; thread-local storage valid until next Windows Sockets call on same thread (Microsoft docs).','official_sources':['https://learn.microsoft.com/en-us/windows/win32/api/winsock/nf-winsock-inet_addr','https://learn.microsoft.com/en-us/windows/win32/api/winsock/nf-winsock-inet_ntoa']})
print('12 pure current system API conversion vectors PASS; game/socket/DNS/GUI0')

from pathlib import Path
import pefile,struct,json,re,hashlib
from unicorn import *
from unicorn.x86_const import *
R=Path(__file__).resolve().parent
L=R.parents[3]/'lito登入器/LiTo381-Launcher'
native=pefile.PE(str(L.parent/'舊版分析與驗收/runtime/LiTo.unpacked-oep-current.exe'))
dll=pefile.PE(str(R/'build/native/ddraw.dll'))
maptext=(R/'build/native/ddraw.map').read_text(errors='replace')
def symbol(name):
 line=next(l for l in maptext.splitlines() if '?'+name+'@' in l)
 return int(re.search(r'\b([0-9a-f]{8}) f ',line).group(1),16)
u=Uc(UC_ARCH_X86,UC_MODE_32)
for p in [native,dll]:
 base=p.OPTIONAL_HEADER.ImageBase;u.mem_map(base,(p.OPTIONAL_HEADER.SizeOfImage+4095)&~4095);u.mem_write(base,p.get_memory_mapped_image())
u.mem_map(0x100000,0x100000);u.mem_map(0x20000000,0x10000);u.mem_map(0xdead0000,4096)
for a in [0x4D6970,0x49A510,0x497010,0x496EB0,0x4C6AE0,symbol('Log'),symbol('RefreshCharacterPageCaption')]:u.mem_write(a,b'\xc3')
u.mem_write(0x46EA30,b'\xc2\x04\x00')
# Install the existing count-observer trampoline, exactly as used by the probe.
tramp=0x20001000;expected=0x20001100
code=b'\x50\x0f\xb6\x41\x01\xa3'+struct.pack('<I',expected)+b'\x58'+native.get_data(0xa7450,7)
code+=b'\xe9'+struct.pack('<i',0x4a7457-(tramp+len(code)+5))
u.mem_write(tramp,code);u.mem_write(0x4a7450,b'\xe9'+struct.pack('<i',tramp-0x4a7455)+b'\x90\x90')
vquery=0x20002000
u.mem_write(vquery,b'\xc2\x0c\x00')
for entry in dll.DIRECTORY_ENTRY_IMPORT:
 for imp in entry.imports:
  if imp.name==b'VirtualQuery':u.mem_write(imp.address,struct.pack('<I',vquery))
u.mem_map(0x77409000,4096);u.mem_write(0x77409b70,b'\xc3');u.mem_write(0x774095e0,b'\xc3')
trace=[]
def step(uc,address,size,user):
 if address in (0x77409b70,0x774095e0):
  sp=uc.reg_read(UC_X86_REG_ESP);args=struct.unpack('<3I',uc.mem_read(sp+4,12))
  if address==0x77409b70:
   dst,src,n=args;raw=bytes(uc.mem_read(src,n)).split(b'\0')[0];uc.mem_write(dst,raw[:n].ljust(n,b'\0'));uc.reg_write(UC_X86_REG_EAX,dst)
  else:
   src=args[0];off=0
   while uc.mem_read(src+off,1)!=b'\0':off+=1
   uc.reg_write(UC_X86_REG_EAX,src+off)
 if address==vquery:
  sp=uc.reg_read(UC_X86_REG_ESP)
  p=int.from_bytes(uc.mem_read(sp+8,4),'little')
  uc.mem_write(p,struct.pack('<7I',0x20000000,0x20000000,4,0x10000,0x1000,4,0x20000));uc.reg_write(UC_X86_REG_EAX,28)
 trace.append(address)
 if len(trace)>30:trace.pop(0)
def invalid(uc,access,address,size,value,user):
 print('INVALID',access,hex(address),size,'trace',[hex(x) for x in trace]);return False
u.hook_add(UC_HOOK_CODE,step);u.hook_add(UC_HOOK_MEM_INVALID,invalid)
def call(a,packet=None,stackargs=b'',saved_esi=0xabc003):
 if packet is not None:u.mem_write(0x20000000,bytes(packet))
 sp=0x19ff00
 u.mem_write(sp,struct.pack('<I',0xdead0000)+stackargs)
 for reg,value in [(UC_X86_REG_ESP,sp),(UC_X86_REG_ECX,0x20000000),(UC_X86_REG_EDX,0),(UC_X86_REG_EBP,0xabc001),(UC_X86_REG_EBX,0xabc002),(UC_X86_REG_ESI,saved_esi),(UC_X86_REG_EDI,0xabc004)]:u.reg_write(reg,value)
 u.emu_start(a,0xdead0000,count=1000000)
 assert u.reg_read(UC_X86_REG_EIP)==0xdead0000,hex(u.reg_read(UC_X86_REG_EIP))
 assert u.reg_read(UC_X86_REG_ESP)==sp+4,hex(u.reg_read(UC_X86_REG_ESP))
 for reg,value in [(UC_X86_REG_EBP,0xabc001),(UC_X86_REG_EBX,0xabc002),(UC_X86_REG_ESI,saved_esi),(UC_X86_REG_EDI,0xabc004)]:assert u.reg_read(reg)==value,(reg,hex(u.reg_read(reg)))
portrait=bytes(u.mem_read(0x62fd70,0x420))
# Negative control: ordinary fastcall alone violates the receiver's hidden ESI ABI.
call(symbol('ReceivePagedCharacterCount'),[3,1])
try:
 call(0x4a7550,bytearray([0xcb,65,0,0])+bytearray(79),saved_esi=0)
 raise AssertionError('Missing native ABI failure')
except UcError:
 assert u.reg_read(UC_X86_REG_EIP)==0x4a2a93
 assert u.reg_read(UC_X86_REG_EDX)==0
print('PASS negative control: native checksum destination consumed saved caller ESI')
# Negative control: even with the correct ABI, passing all eight rows directly
# to the original buffer overwrites the following portrait table.
call(0x4a7450,[3,8])
for i in range(8):
 row=bytearray([0xcb,65+i,0,0])+bytearray(79);row[4]=1
 call(symbol('InvokeNativeCharacterRow'),row)
assert bytes(u.mem_read(0x62fd70,0x420))!=portrait
u.mem_write(0x62fd70,portrait)
print('PASS negative control: unpaged eight-row directory overwrites portraits')
for count in range(0,9):
 print('count',count,flush=True);call(symbol('ReceivePagedCharacterCount'),[3,count])
 assert int.from_bytes(u.mem_read(0x7607a0,4),'little')==min(count,4)
 for i in range(count):
  row=bytearray([0xcb,65+i,0,0])+bytearray(79);row[4]=1
  call(symbol('ReceivePagedCharacterRow'),row)
 assert bytes(u.mem_read(0x62fd70,0x420))==portrait
 if count: assert u.mem_read(0x62faa0,1)==b'A'
# Actual captured directory, then real compiled page navigation in both directions.
frameRoot=R/'build/staging/runs/buff-schema-native-20261018-b/outputs/captures'
frames=[json.loads(line) for f in frameRoot.glob('*/frames.jsonl') for line in f.read_text(encoding='utf8').splitlines()]
rows=[bytes.fromhex(f['plain']) for f in frames if f.get('opcode')=='0xCB' and f.get('direction')=='server_to_client']
assert len(rows)==8
call(symbol('ReceivePagedCharacterCount'),[3,8])
for row in rows:call(symbol('ReceivePagedCharacterRow'),row)
def verifyPage(start,n):
 for i in range(n):
  expectedName=rows[start+i][1:].split(b'\0')[0]
  actualName=bytes(u.mem_read(0x62faa0+i*180,28)).split(b'\0')[0]
  assert expectedName==actualName
 assert bytes(u.mem_read(0x62fd70,0x420))==portrait
verifyPage(0,4)
u.mem_write(symbol('IsVerifiedLiToClient'),b'\xb0\x01\xc3');u.mem_write(0x62c21c,b'\x01')
u.mem_write(0x75f6e8,struct.pack('<I',0x20003000));u.mem_write(0x2000300c,struct.pack('<I',7))
u.mem_write(0x64bbd4,struct.pack('<I',4));u.mem_write(0x64af98,bytes(4));u.mem_write(0x64afbc,bytes(2))
for _ in range(3):
 call(symbol('TurnCharacterPage'),stackargs=struct.pack('<i',1));assert u.reg_read(UC_X86_REG_EAX)&255;verifyPage(4,4)
 call(symbol('TurnCharacterPage'),stackargs=struct.pack('<i',-1));assert u.reg_read(UC_X86_REG_EAX)&255;verifyPage(0,4)
u.mem_write(0x64afbc,b'\x01\x00');u.mem_write(0x64afbe,b'\x00')
call(symbol('ReceivePagedCharacterRow'),rows[0]);assert u.mem_read(0x64afbe,1)==b'\x01'
receipt={'passed':True,'dll_sha256':hashlib.sha256((R/'build/native/ddraw.dll').read_bytes()).hexdigest(),'native_image_sha256':hashlib.sha256(native.__data__).hexdigest(),'cases':['negative control: missing ESI contract faults at native checksum write','negative control: unpaged eight rows overwrite portraits','compiled DLL count 0..8','native varargs row parser and observed-count trampoline','ESP and EBX/EBP/ESI/EDI preserved','captured eight real rows','three round trips of both pages','portrait table byte-for-byte unchanged','selected-character handoff still sends native request'],'limits':'timer, UI rendering, derived descriptions and OS string/API calls stubbed; manual UI test still required'}
(R/'build/native-emulation.json').write_text(json.dumps(receipt,indent=2),encoding='utf8')
print(json.dumps(receipt))

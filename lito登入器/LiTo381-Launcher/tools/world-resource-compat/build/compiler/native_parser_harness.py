"""Execute LiTo's real sprite parser in an isolated x86 emulator.

File IO, allocation, isdigit and the reversible text decoder are host adapters.
This validates parser/table writes, not the live game's renderer or startup.
"""
from pathlib import Path
import argparse, json, struct, sys, time
ROOT=Path(__file__).resolve().parent
sys.path.insert(0,str(ROOT/'vendor'))
import pefile
from unicorn import Uc, UC_ARCH_X86, UC_MODE_32, UC_HOOK_CODE, UC_HOOK_MEM_INVALID
from unicorn.x86_const import UC_X86_REG_ESP, UC_X86_REG_EIP, UC_X86_REG_EAX, UC_X86_REG_ECX, UC_X86_REG_ESI, UC_X86_REG_EBP

PE=ROOT.parents[2]/'舊版分析與驗收/runtime/LiTo.unpacked-oep-current.exe'
def encode(data):return bytes((((v+1)<<1)^0xB1)&255 for v in data)
def decode(data):return bytes((((v^0xB1)>>1)-1)&255 for v in data)

class Harness:
 def __init__(self, records, patches=(), lt_records=(), native_base_loader=False):
  self.uc=Uc(UC_ARCH_X86,UC_MODE_32);self.heap=0x20000000;self.heap_end=0x30000000
  self.allocations=[];self.completed=False;self.records=records;self.lt_records=lt_records;self.started=time.monotonic()
  self.stop_before_checksum=True
  pe=pefile.PE(str(PE)); image=pe.get_memory_mapped_image();size=(pe.OPTIONAL_HEADER.SizeOfImage+4095)&~4095
  self.uc.mem_map(0x400000,size);self.uc.mem_write(0x400000,image)
  self.uc.mem_map(0,4096);self.uc.mem_map(0x10000000,0x200000);self.uc.mem_map(0x20000000,0x10000000)
  self.uc.mem_map(0x773AF000,0x1000)
  self.uc.mem_write(0x773AFFA0,b'\xC3')
  for address,data in patches:self.uc.mem_write(address,data)
  for address in [0x4D3FF0,0x4D4170,0x4F118C,0x4F103C,0x4F12E0,0x773AFFA0,0x43DCC0,0x43E297,0x43EC56]:
   if native_base_loader and address==0x4D3FF0:continue
   self.uc.hook_add(UC_HOOK_CODE,self.hook,begin=address,end=address)
  self.uc.hook_add(UC_HOOK_MEM_INVALID,self.invalid)
  self.uc.reg_write(UC_X86_REG_ESP,0x101FFF00)
  self.uc.mem_write(0x101FFF00,struct.pack('<I',0x10000000))
 def d(self,address):return struct.unpack('<I',self.uc.mem_read(address,4))[0]
 def alloc(self,size):
  if not 0<=size<=0x8000000:raise ValueError(f'invalid allocation {size}')
  address=self.heap;self.heap+=((max(size,1)+15)&~15)+16
  if self.heap>=self.heap_end:raise ValueError('emulated heap exhausted')
  self.allocations.append((address,max(size,1)))
  self.uc.mem_write(address+max(size,1),b'\xA5'*16)
  return address
 def ret(self,value=0,pop=0):
  sp=self.uc.reg_read(UC_X86_REG_ESP);target=self.d(sp)
  self.uc.reg_write(UC_X86_REG_EAX,value);self.uc.reg_write(UC_X86_REG_ESP,sp+4+pop);self.uc.reg_write(UC_X86_REG_EIP,target)
 def string(self,address):
  b=bytearray()
  while True:
   block=bytes(self.uc.mem_read(address+len(b),256));at=block.find(b'\0')
   if at>=0:return bytes(b)+block[:at]
   b.extend(block)
   if len(b)>1000000:raise ValueError('unterminated native text')
 def hook(self,uc,address,size,unused):
  sp=uc.reg_read(UC_X86_REG_ESP)
  if address in (0x4D3FF0,0x4D4170):
   records=self.records if address==0x4D3FF0 else self.lt_records
   pointers=self.alloc(len(records)*4);start=self.heap
   for i,record in enumerate(records):
    encoded=encode(record) if address==0x4D3FF0 else record
    if address==0x4D3FF0:assert decode(encoded)==record
    ptr=self.alloc(len(encoded)+1);uc.mem_write(ptr,encoded+b'\0');uc.mem_write(pointers+i*4,struct.pack('<I',ptr))
   uc.mem_write(uc.reg_read(UC_X86_REG_ECX),struct.pack('<III',len(records),pointers,start));self.ret(1,8)
  elif address==0x4F118C:self.ret(self.alloc(self.d(sp+4)))
  elif address==0x4F103C:self.ret()
  elif address==0x4F12E0:self.ret(0,20) # zeroed two-dword vector elements
  elif address==0x773AFFA0:self.ret(int(48<=self.d(sp+4)<=57))
  elif address==0x43DCC0:
   holder=uc.reg_read(UC_X86_REG_ECX);ptr=self.d(holder);negative=False;value=0;digits=False
   while True:
    block=bytes(uc.mem_read(ptr,64));done=False
    for byte in block:
     if digits:
      if not 48<=byte<=57:done=True;break
      value=(value*10+byte-48)&0xFFFFFFFF
     elif 48<=byte<=57:digits=True;value=byte-48
     elif byte==0:done=True;break
     elif byte==45:negative=True
     elif byte!=32:negative=False
     ptr+=1
    if done:break
   uc.mem_write(holder,struct.pack('<I',ptr));self.ret((-value if negative else value)&0xFFFFFFFF)
  elif address==0x43E297:
   ptr=uc.reg_read(UC_X86_REG_ESI);data=self.string(ptr)
   if data:uc.mem_write(ptr,decode(data))
   uc.reg_write(UC_X86_REG_EIP,0x43E2C4)
  elif address==0x43EC56 and self.stop_before_checksum:self.completed=True;uc.emu_stop()
 def invalid(self,uc,access,address,size,value,unused):
  raise RuntimeError(f'native invalid memory access type={access} address={address:08X} EIP={uc.reg_read(UC_X86_REG_EIP):08X}')
 def run(self,max_slices=1):
  entry=0x43DE10
  for slice_index in range(max_slices):
   self.uc.emu_start(entry,0x10000000,timeout=40_000_000,count=150_000_000)
   if self.completed:break
   entry=self.uc.reg_read(UC_X86_REG_EIP)
   print(json.dumps({'emulator_slice':slice_index+1,'eip':hex(entry),
                     'parser_row':self.d(self.uc.reg_read(UC_X86_REG_EBP)-0x14),
                     'allocated_bytes':self.heap-0x20000000}),flush=True)
  damaged=[hex(p) for p,n in self.allocations if bytes(self.uc.mem_read(p+n,16))!=b'\xA5'*16]
  if damaged:raise RuntimeError('native parser allocation guard overwritten: '+str(damaged[:10]))
  return {'completed':self.completed,'seconds':round(time.monotonic()-self.started,3),'count':self.d(0x647E7C),'base_count':self.d(0x647E90),'heap_bytes':self.heap-0x20000000,'allocation_guards':len(self.allocations),'damaged_guards':damaged,'table':hex(self.d(0x834E70)), 'eip':hex(self.uc.reg_read(UC_X86_REG_EIP))}

def main():
 ap=argparse.ArgumentParser();ap.add_argument('--output',type=Path,required=True);args=ap.parse_args()
 raw=(ROOT.parent/'heal-target-crash-20261011/native-embedded-list.spr').read_bytes()
 records=raw.split(b'#')[1:];records[-1]=records[-1].split(b'\0')[0]
 # Encoded B3 terminator decodes to byte 0; keep only the real 5,894 records.
 records=[r.split(b'\0')[0] for r in records]
 h=Harness(records);report=h.run();args.output.write_text(json.dumps(report,indent=2));print(report)
 if not report['completed']:raise RuntimeError('native parser did not complete within bounded emulation')

if __name__=='__main__':main()

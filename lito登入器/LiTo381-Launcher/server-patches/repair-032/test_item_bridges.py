from pathlib import Path
import sys,re,struct,json
r=Path(__file__).resolve().parent;c=r.parent/'compat-gate';sys.path.insert(0,str(r.parent/'vendor'))
import pefile
from unicorn import Uc,UC_ARCH_X86,UC_MODE_32,UC_HOOK_CODE
from unicorn.x86_const import *
pe=pefile.PE(str(c/'item_details_native_test.exe'));image=pe.get_memory_mapped_image();base=pe.OPTIONAL_HEADER.ImageBase
mapping=(c/'item_details_native_test.map').read_text()
def symbol(name):return int(re.search(r'\?'+name+r'@@\S+\s+([0-9a-fA-F]+) f',mapping)[1],16)
regs=[UC_X86_REG_EAX,UC_X86_REG_ECX,UC_X86_REG_EDX,UC_X86_REG_EBX,UC_X86_REG_EBP,UC_X86_REG_ESI,UC_X86_REG_EDI,UC_X86_REG_ESP]
report=[]
for name,legacy,handled in [('LiToItemGridBridge',0x4496bd,0x449b14),('LiToItemFixedBridge',0x449cc6,0x44a0b5),('LiToItemEquipmentBridge',0x4d931e,0x4d96ba)]:
 for accept in [False,True]:
  u=Uc(UC_ARCH_X86,UC_MODE_32);u.mem_map(base,(len(image)+4095)&~4095);u.mem_write(base,image);u.mem_map(0x400000,0x100000);u.mem_map(0x30000000,0x10000)
  initial=[0x30001000,0x30002000,0xabcdef,455,0x30008000,0x456789,200,0x30007000]
  for reg,value in zip(regs,initial):u.reg_write(reg,value)
  u.reg_write(UC_X86_REG_EFLAGS,0x246)
  u.mem_write(initial[0]+0x18,struct.pack('<I',3));u.mem_write(initial[1]+0x14,struct.pack('<II',0x88,3));u.mem_write(initial[4]-0x20,struct.pack('<I',100))
  calls=[]
  def hook(uc,address,size,data):
   if address==symbol('DrawLiToItemDetail'):
    esp=uc.reg_read(UC_X86_REG_ESP);ret,obj,x,y=struct.unpack('<IIII',uc.mem_read(esp,16));calls.append((obj,x,y));uc.reg_write(UC_X86_REG_EAX,int(accept));uc.reg_write(UC_X86_REG_ECX,0x111);uc.reg_write(UC_X86_REG_EDX,0x222);uc.reg_write(UC_X86_REG_EFLAGS,0x202);uc.reg_write(UC_X86_REG_ESP,esp+4);uc.reg_write(UC_X86_REG_EIP,ret)
   if address in [legacy,handled]:uc.emu_stop()
  u.hook_add(UC_HOOK_CODE,hook);u.emu_start(symbol(name),0,count=100)
  assert u.reg_read(UC_X86_REG_EIP)==(handled if accept else legacy)
  expected=initial.copy()
  if not accept:
   expected[1]=3
   if name=='LiToItemEquipmentBridge':expected[0]=0x88
  assert [u.reg_read(reg) for reg in regs]==expected,(name,accept)
  if accept:assert u.reg_read(UC_X86_REG_EFLAGS)==0x246
  expectedArgs=(initial[1],455,200) if name=='LiToItemEquipmentBridge' else (initial[0],48 if name=='LiToItemFixedBridge' else 100,455)
  assert calls==[expectedArgs],calls
  report.append({'bridge':name,'handled':accept,'registers_stack_and_native_continuation':True})
(r/'item-bridge-regression.json').write_text(json.dumps(report,indent=2));print('PASS: six real compiled x86 bridge paths, arguments, registers, stack, flags and original epilogues')

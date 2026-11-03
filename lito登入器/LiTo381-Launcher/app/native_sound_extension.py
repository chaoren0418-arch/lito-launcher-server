"""Relocatable x86 sound-choice adapters for the unchanged 20-byte frame ABI.

Extra sound choices live in an explicit side table. Never let index3/4 read the
legacy frame's count or duration fields. These are uninstalled candidate stubs.
"""
import struct

class Code:
 def __init__(self,base):self.base=base;self.data=bytearray();self.labels={};self.fixups=[]
 def put(self,data):self.data.extend(data)
 def label(self,name):self.labels[name]=len(self.data)
 def branch(self,opcode,label):
  self.put(opcode+b'\0'*4);self.fixups.append((len(self.data)-4,label))
 def call(self,address):self.put(b'\xE8'+struct.pack('<i',address-(self.base+len(self.data)+5)))
 def jump(self,address):self.put(b'\xE9'+struct.pack('<i',address-(self.base+len(self.data)+5)))
 def finish(self):
  for offset,label in self.fixups:struct.pack_into('<i',self.data,offset,self.labels[label]-(offset+4))
  return bytes(self.data)

def reader(base,table,count):
 # int __stdcall read_sound(void* action_frames, unsigned frame_times10_plus_choice)
 c=Code(base);c.put(b'\x53\x56') # preserve EBX, ESI
 c.put(b'\x8B\x44\x24\x10\x33\xD2\xB9\x0A\0\0\0\xF7\xF1') # div10
 c.put(b'\x6B\xC0\x14\x03\x44\x24\x0C') # frame*20 + action_frames
 c.put(b'\x83\xFA\x03');c.branch(b'\x0F\x82','native')
 c.put(b'\x83\xFA\x05');c.branch(b'\x0F\x83','missing')
 c.put(b'\xBE'+struct.pack('<I',table)+b'\xB9'+struct.pack('<I',count))
 c.label('loop');c.put(b'\x85\xC9');c.branch(b'\x0F\x84','missing')
 c.put(b'\x39\x06');c.branch(b'\x0F\x84','found')
 c.put(b'\x83\xC6\x10\x49');c.branch(b'\xE9','loop')
 c.label('found');c.put(b'\x0F\xBF\x44\x56\x04');c.branch(b'\xE9','return')
 c.label('native');c.put(b'\x0F\xBF\x44\x50\x02');c.branch(b'\xE9','return')
 c.label('missing');c.put(b'\x33\xC0')
 c.label('return');c.put(b'\x5E\x5B\xC2\x08\x00');return c.finish()

def load_hook(base,helper,resume):
 # Replace MOVSX EDX,word [EAX+ECX*2+2], preserving other registers AND flags.
 c=Code(base);c.put(b'\x9C\x60\x51\x50');c.call(helper)
 c.put(b'\x89\x44\x24\x14\x61\x9D');c.jump(resume);return c.finish()

def compare_hook(base,helper,resume):
 # Replace CMP word [EDX+EAX*2+2],0, preserving registers and setting flags as
 # the original 16-bit CMP. LEA discards scratch storage without changing flags.
 c=Code(base);c.put(b'\x8D\x64\x24\xFC\x60\x50\x52');c.call(helper)
 c.put(b'\x89\x44\x24\x20\x61\x66\x83\x3C\x24\x00\x8D\x64\x24\x04')
 c.jump(resume);return c.finish()

READ_SITES=(0x4E6187,0x4E61A9,0x4E61C5)
COMPARE_SITE=0x4E60FA
COUNT_SITE=0x4E60AE

def plan(base,table,count,read_bytes):
 expected={**{p:bytes.fromhex('0fbf544802') for p in READ_SITES},
           COMPARE_SITE:bytes.fromhex('66837c420200'),COUNT_SITE:bytes.fromhex('83ff03')}
 for address,data in expected.items():
  if read_bytes(address,len(data))!=data:raise ValueError(f'sound-site pin mismatch at {address:08X}')
 helper=base;blobs=[(helper,reader(helper,table,count))];patches=[]
 for i,address in enumerate(READ_SITES):
  stub=base+0x100+i*0x80;blobs.append((stub,load_hook(stub,helper,address+5)))
  patches.append((address,b'\xE9'+struct.pack('<i',stub-(address+5))))
 stub=base+0x300;blobs.append((stub,compare_hook(stub,helper,COMPARE_SITE+6)))
 patches.append((COMPARE_SITE,b'\xE9'+struct.pack('<i',stub-(COMPARE_SITE+5))+b'\x90'))
 patches.append((COUNT_SITE,bytes.fromhex('83ff05')))
 return blobs,patches

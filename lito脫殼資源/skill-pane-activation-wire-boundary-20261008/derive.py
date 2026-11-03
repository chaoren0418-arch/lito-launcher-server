import hashlib, json, struct, sys
from pathlib import Path
import capstone
sys.stdout.reconfigure(encoding='utf-8')
ROOT = Path(__file__).resolve().parent.parent
OUT = Path(__file__).resolve().parent
SOURCES = [
 (ROOT.parent/'lito登入器'/'舊版分析與驗收'/'runtime'/'LiTo.unpacked-oep-current.exe', '4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'),
 (ROOT.parent/'lito登入器'/'舊版分析與驗收'/'ida-client-oep-v2'/'full-disassembly.asm', '2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A'),
 (ROOT/'skill-pane-frame-coordinate-contract.json','8B4AFC6C4C884099AD092459B1608B2586E4A4EE6DB22F1FBBA642EE4DC6A7CE'),
 (ROOT/'S1E-initialization-gates.json','841FAE9D1234919AE6051490596AF5EB75629107D4D4A101AC93C6A25153C2DC'),
 (ROOT/'S1E-receive-selection-boundary.json','3B5357B9D193EC9A93F93611FE5C33AC8761F7BB9B6CBA584FD87D517114440E')]
def sha(b): return hashlib.sha256(b).hexdigest().upper()
snapshots=[]; pins=[]
for p,expected in SOURCES:
 b=p.read_bytes(); assert sha(b)==expected, str(p)
 snapshots.append(b); pins.append({'path':str(p),'bytes':len(b),'sha256':sha(b),'match':True})
pe=snapshots[0]; nt=struct.unpack_from('<I',pe,0x3c)[0]
assert pe[:2]==b'MZ' and pe[nt:nt+4]==b'PE\0\0'
machine,nsec=struct.unpack_from('<HH',pe,nt+4)
optsz=struct.unpack_from('<H',pe,nt+20)[0]; opt=nt+24
assert machine==0x14c and struct.unpack_from('<H',pe,opt)[0]==0x10b
base=struct.unpack_from('<I',pe,opt+28)[0]; assert base==0x400000
sections=[]
for j in range(nsec):
 off=opt+optsz+40*j; name=pe[off:off+8]; vs,rva,rs,rp=struct.unpack_from('<IIII',pe,off+8)
 sections.append({'index':j,'name_hex':name.hex(),'virtual_size':vs,'rva':rva,'raw_size':rs,'raw_pointer':rp})
def mapped(va,n):
 rv=va-base
 matches=[s for s in sections if s['rva']<=rv and rv+n<=s['rva']+s['raw_size']]
 assert len(matches)==1,(hex(va),n)
 s=matches[0]; off=s['raw_pointer']+rv-s['rva']; assert off+n<=len(pe)
 return pe[off:off+n],off,s['index']
md=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);md.detail=True
ranges=[]
def take(label,va,n,code=True):
 b,off,section=mapped(va,n); p=OUT/f'{va:08X}-{n:X}.bin';p.write_bytes(b)
 ins=[]
 if code:
  for x in md.disasm(b,va):
   ins.append({'va':f'{x.address:08X}','rva':f'{x.address-base:08X}','file_offset':f'{off+x.address-va:08X}','raw_hex':x.bytes.hex(),'mnemonic':x.mnemonic,'operands':x.op_str})
  assert sum(len(bytes.fromhex(x['raw_hex'])) for x in ins)==n,(label,'partial decode')
 d={'label':label,'va':f'{va:08X}','rva':f'{va-base:08X}','end_exclusive':f'{va+n:08X}','file_offset':f'{off:08X}','section_index':section,'bytes':n,'raw_hex':b.hex(),'output_path':str(p),'sha256':sha(b),'instructions':ins}
 ranges.append(d);return d
for spec in [
 ('action_dispatch_entry_prefix',0x46D640,0xEA),
 ('action19_first_dispatch_block',0x46D749,0x1F),
 ('action_dispatch_second_entry_prefix',0x47BB10,0xF3),
 ('action19_second_dispatch_block',0x47BC9B,0x29),
 ('toolbar_skill_control_construction',0x47DC06,0x62),
 ('registry_contains',0x4EBB30,0x19),
 ('registry_toggle_or_clear',0x4EBBD0,0xD5),
 ('registry_toggle_thunk',0x4EBEB0,5),
 ('toolbar_toggle_callback',0x4EBEC0,0x14)]:take(*spec)
first_index=take('action19_first_map_byte',0x46D884+18,1,False)
slot=int(first_index['raw_hex'],16)
first_target=take('action19_first_jump_table_slot',0x46D86C+4*slot,4,False)
second_target=take('action19_second_jump_table_slot',0x47C2E4+4*18,4,False)
assert int.from_bytes(bytes.fromhex(first_target['raw_hex']),'little')==0x46D749
assert int.from_bytes(bytes.fromhex(second_target['raw_hex']),'little')==0x47BC9B
inherited_path=ROOT/'skill-pane-frame-coordinate'/'ranges.json'
inherited_bytes=inherited_path.read_bytes()
assert sha(inherited_bytes)=='0F20732D197F85B790D8BA93FA0E2A03186DA08BBE760A75A71D30F2D0985D20'
inherited=json.loads(inherited_bytes)
index_range=next(x for x in inherited['ranges'] if x['va']=='004EBB50')
index_bytes=Path(index_range['output']['path']).read_bytes()
assert sha(index_bytes)==index_range['output']['sha256'] and index_bytes==mapped(0x4EBB50,27)[0]
inherited_support={'qualification':'Existing accepted registry index helper evidence only; no new target expansion','manifest':{'path':str(inherited_path),'bytes':len(inherited_bytes),'sha256':sha(inherited_bytes)},'range':index_range}
(OUT/'inherited-registry-index-support.json').write_text(json.dumps(inherited_support,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
asm=snapshots[1].decode('utf-8',errors='replace').splitlines()
excerpts=[{'label':'ASM header: different source; navigation only','line':1,'text':'\n'.join(asm[:20])}]
for name in ['sub_46D640 proc','sub_47BB10 proc','sub_4EBB30 proc','sub_4EBBD0 proc','sub_4EBEB0 proc','sub_4EBEC0 proc']:
 for i,line in enumerate(asm):
  if name in line:
   end=next(j for j in range(i,len(asm)) if 'endp' in asm[j])
   excerpts.append({'label':name,'line':i+1,'text':'\n'.join(asm[i:end+1])});break
(OUT/'asm-navigation.json').write_text(json.dumps(excerpts,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
result={'input_pins':pins,'PE':{'image_base':f'{base:08X}','machine':machine,'sections':sections},'tools':{'python':sys.version,'capstone':capstone.__version__},'ranges':ranges,'action19_mapping':{'input_value':19,'first_eax_index':18,'first_byte_slot':slot,'first_target':'0046D749','second_eax_index':18,'second_target':'0047BC9B'},'instruction_boundary_qualification':'Entry prefixes begin at named function entries and decode completely to switch jump. Case entries are independently corroborated by raw table targets. Toolbar start is supplied block boundary; complete instructions retained through instruction at 0047DC62 (ends 0047DC68), without expanding its consumer. Helper entries and bounds match accepted prior evidence. No arbitrary raw E8 scan.'}
(OUT/'raw-evidence.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
for r in ranges:
 print(r['label'],r['va'],r['bytes'],r['sha256'])
 for x in r['instructions']:print(x['va'],x['raw_hex'],x['mnemonic'],x['operands'])

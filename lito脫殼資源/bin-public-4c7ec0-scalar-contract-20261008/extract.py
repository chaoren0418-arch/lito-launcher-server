import sys,json,hashlib,struct,capstone
from pathlib import Path
sys.stdout.reconfigure(encoding='utf-8');O=Path(__file__).resolve().parent;R=O.parent
assert not (O/'raw-evidence.json').exists(),'New one-body extraction only; no replay.'
P=R.parent/'lito登入器/舊版分析與驗收/runtime/LiTo.unpacked-oep-current.exe';A=R.parent/'lito登入器/舊版分析與驗收/ida-client-oep-v2/full-disassembly.asm';H=R/'bin-public-4c8790-effects-20261008/handoff.json'
def sha(b):return hashlib.sha256(b).hexdigest().upper()
def pin(p):
 b=p.read_bytes();return {'path':str(p),'bytes':len(b),'sha256':sha(b)}
def write(p,x):p.write_text(json.dumps(x,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
pe=P.read_bytes();asm=A.read_bytes();hb=H.read_bytes()
assert len(pe)==7094272 and sha(pe)=='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
assert sha(asm)=='2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A'
assert sha(hb)=='702A1A987D572A8D04FB3F24FBA3BC4217D18D97C6B14F80E1C604F848CFDFB2'
prior=json.loads(hb);assert not prior['direct_call_boundary']['expanded_helper_targets'] and '004C7EC0' in prior['direct_call_boundary']['helper_targets']
write(O/'caller-contract-input.json',{'pin':pin(H),'question_gap':prior['new_gap'],'direct_call_boundary':prior['direct_call_boundary'],'unknowns':prior['unknowns_and_blockers'],'prior_F2':prior['findings'][1]['conclusion'],'prior_F4':prior['findings'][3]['conclusion'],'scope':'Existing accepted contract input only; old caller bytes not extracted or disassembled again.'})
nt=struct.unpack_from('<I',pe,0x3c)[0];assert pe[:2]==b'MZ' and pe[nt:nt+4]==b'PE\0\0'
machine,nsec=struct.unpack_from('<HH',pe,nt+4);optsz=struct.unpack_from('<H',pe,nt+20)[0];opt=nt+24
assert machine==0x14c and struct.unpack_from('<H',pe,opt)[0]==0x10b
base=struct.unpack_from('<I',pe,opt+28)[0];assert base==0x400000
secs=[]
for k in range(nsec):
 s=opt+optsz+40*k;vs,rva,rs,rp=struct.unpack_from('<IIII',pe,s+8);chars=struct.unpack_from('<I',pe,s+36)[0];secs.append({'index':k,'name_hex':pe[s:s+8].hex(),'virtual_size':vs,'rva':rva,'raw_size':rs,'raw_pointer':rp,'characteristics':chars})
va=0x4C7EC0;n=0x31;rva=va-base;ss=[s for s in secs if s['rva']<=rva and rva+n<=s['rva']+s['raw_size']];assert len(ss)==1
sec=ss[0];assert sec['characteristics']&0x20000000;off=sec['raw_pointer']+rva-sec['rva'];assert off+n<=len(pe);body=pe[off:off+n];bp=O/'004C7EC0-31.bin';bp.write_bytes(body)
md=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);md.detail=True;ins=[]
for x in md.disasm(body,va):
 d={'va':f'{x.address:08X}','rva':f'{x.address-base:08X}','file_offset':f'{off+x.address-va:08X}','size':x.size,'raw_hex':x.bytes.hex(),'mnemonic':x.mnemonic,'operands':x.op_str}
 if x.mnemonic=='call' or x.mnemonic.startswith('j'):
  if x.operands[0].type==capstone.x86.X86_OP_IMM:d['direct_target']=f'{x.operands[0].imm:08X}'
  else:d['indirect_edge']=True
 ins.append(d)
assert sum(i['size'] for i in ins)==n and ins[-1]['va']=='004C7EEE' and ins[-1]['raw_hex']=='c20400'
starts={i['va'] for i in ins};branches=[i for i in ins if i['mnemonic'].startswith('j')];assert all(i['direct_target'] in starts for i in branches)
calls=[i for i in ins if i['mnemonic']=='call'];assert [i['direct_target'] for i in calls]==['00419DB0','00419E00','004C7DC0']
assert all(i['direct_target'] not in ['0042E550','0042E6D0','0042EA50','05AD0986'] for i in calls)
ls=asm.decode('utf-8',errors='replace').splitlines();start=next(i for i,l in enumerate(ls) if l.startswith('sub_4C7EC0 proc'));end=next(i for i in range(start,len(ls)) if ls[i].startswith('sub_4C7EC0 endp'))
write(O/'asm-navigation.json',{'ASM_pin':pin(A),'different_input_SHA':'AEB05C78FE029EA6D3F2EF349245669E47E1E83B1C005E32D4663C780627B5A6','authority':'Navigation only, all conclusions from fixed-PE bytes.','start_line':start+1,'end_line':end+1,'function_text':'\n'.join(ls[start:end+1])})
raw={'TASK_ID':O.name,'inputs':[{'path':str(P),'bytes':len(pe),'sha256':sha(pe)},pin(A),pin(H)],'same_PE_snapshot_hashed_and_decoded':True,'tools':{'python':sys.version,'capstone':capstone.__version__},'PE':{'machine':machine,'imagebase':f'{base:08X}','sections':secs},'body':{'va':f'{va:08X}','rva':f'{rva:08X}','end_exclusive':f'{va+n:08X}','file_offset':f'{off:08X}','section_index':sec['index'],'bytes':n,'raw_hex':body.hex(),'artifact':pin(bp)},'instructions':ins,'local_branches':branches,'direct_call_boundary':calls,'raw_body_complete':True,'call_target_bodies_expanded':[],'caller_bytes_redecoded':False}
write(O/'raw-evidence.json',raw)
lines=[f"{i['va']} RVA={i['rva']} RAW={i['file_offset']} {i['raw_hex']} {i['mnemonic']} {i['operands']}" for i in ins];(O/'disassembly.txt').write_text('\n'.join(lines)+'\n',encoding='utf-8')
assert bp.read_bytes()==body
print('\n'.join(lines));print(json.dumps({'raw_evidence':pin(O/'raw-evidence.json'),'body':raw['body'],'tools':raw['tools'],'only_new_public_body_decoded':True},ensure_ascii=False))

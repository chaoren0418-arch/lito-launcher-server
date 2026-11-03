import json,hashlib,sys,struct
from pathlib import Path
from datetime import datetime,timezone,timedelta
import capstone
sys.stdout.reconfigure(encoding='utf-8')
O=Path(__file__).resolve().parent;R=O.parent;T='bin-public-4c83a0-guard-boundary-20261008'
def sha(b):return hashlib.sha256(b).hexdigest().upper()
def pin(p):
 b=p.read_bytes();return {'path':str(p),'bytes':len(b),'sha256':sha(b)}
def write(p,x):p.write_text(json.dumps(x,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
hp=R/'bin-public-4c8790-effects-20261008'/'handoff.json';hb=hp.read_bytes();assert sha(hb)=='702A1A987D572A8D04FB3F24FBA3BC4217D18D97C6B14F80E1C604F848CFDFB2';h=json.loads(hb)
ep=Path(h['raw_evidence']['path']);eb=ep.read_bytes();assert sha(eb)==h['raw_evidence']['sha256'];e=json.loads(eb)
pp=Path(h['input_pins'][0]['path']);pe=pp.read_bytes();assert sha(pe)=='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
nt=struct.unpack_from('<I',pe,0x3c)[0];assert pe[nt:nt+4]==b'PE\0\0';opt=nt+24;assert struct.unpack_from('<I',pe,opt+28)[0]==0x400000
sec=e['PE']['sections'];base=0x400000;va=0x4C83A0;n=0x4D;rva=va-base
ss=[s for s in sec if s['rva']<=rva and rva+n<=s['rva']+s['raw_size']];assert len(ss)==1
s=ss[0];off=s['raw_pointer']+rva-s['rva'];b=pe[off:off+n];assert len(b)==n
bp=O/'004C83A0-4D.bin';bp.write_bytes(b)
md=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);md.detail=True;ins=[];lines=[]
for x in md.disasm(b,va):
 z={'va':f'{x.address:08X}','rva':f'{x.address-base:08X}','file_offset':f'{off+x.address-va:08X}','size':x.size,'raw_hex':x.bytes.hex(),'mnemonic':x.mnemonic,'operands':x.op_str}
 if x.mnemonic=='call' and x.operands[0].type==capstone.x86.X86_OP_IMM:z['direct_target']=f'{x.operands[0].imm:08X}'
 ins.append(z);lines.append(f"{z['va']} RVA={z['rva']} RAW={z['file_offset']} {z['raw_hex']} {z['mnemonic']} {z['operands']}")
assert sum(x['size'] for x in ins)==n and ins[-1]['va']=='004C83EC' and ins[-1]['mnemonic']=='ret'
calls=[x for x in ins if x['mnemonic']=='call'];assert len(calls)==2 and calls[0]['direct_target']=='2111A650' and calls[1]['operands']=='dword ptr [eax + 0x24]'
external=int(calls[0]['direct_target'],16);external_rva=external-base
assert not any(s['rva']<=external_rva<s['rva']+s['raw_size'] for s in sec)
assert not any(x.get('direct_target') in ['004C6AE0','004C6770','0042E550','0042E6D0','0042EA50'] for x in calls)
ap=Path(h['input_pins'][1]['path']);ab=ap.read_bytes();assert sha(ab)==h['input_pins'][1]['sha256'];ls=ab.decode('utf-8',errors='replace').splitlines();i=next(i for i,l in enumerate(ls) if l.startswith('sub_4C83A0 proc'));j=next(j for j in range(i,len(ls)) if ls[j].startswith('sub_4C83A0 endp'))
write(O/'asm-navigation.json',{'authority':False,'source':pin(ap),'header_different_input':True,'line':i+1,'end_line':j+1,'text':'\n'.join(ls[i:j+1])})
(O/'disassembly.txt').write_text('\n'.join(lines)+'\n',encoding='utf-8')
raw={'task_id':T,'source':{'path':str(pp),'bytes':len(pe),'sha256':sha(pe)},'mapping':{'imagebase':'00400000','va':f'{va:08X}','rva':f'{rva:08X}','end_exclusive':'004C83ED','file_offset':f'{off:08X}','section':s,'bytes':n},'artifact':pin(bp),'instructions':ins,'calls':calls,'unbacked_external_boundary':{'va':f'{external:08X}','arithmetic_rva':f'{external_rva:08X}','section':None,'file_offset':None,'raw_backed_in_pinned_PE':False,'followed':False},'unknown_virtual_target':{'call_instruction':calls[1],'current_vtable':None,'destination':None,'followed':False},'fully_decoded':True,'tool_versions':{'python':sys.version,'capstone':capstone.__version__}}
write(O/'raw-evidence.json',raw)
def w(*vas):return [x for x in ins if x['va'] in vas]
now=datetime.now(timezone(timedelta(hours=8))).isoformat()
d={'schema':'lito.public-local-guard-boundary.v1','task_id':T,'owner':'LITO BIN Analysis','thread_id':'01a11705-96c0-74a0-b56d-ed486c318c95','state':'review','generated_at':now,
 'question':'Which local return conditions in public004C83A0 can suppress the later004C8240/004C83F0 calls reached from the skill-toolbar tail chain?',
 'scope':'Newly reached guard body only; no external/private/virtual body expansion. Parent004C8790 evidence already delivered and unchanged.',
 'source':raw['source'],'parent_handoff':pin(hp),'raw_evidence':pin(O/'raw-evidence.json'),'VA_RVA_raw_mapping':raw['mapping'],
 'findings':[
  {'conclusion':'IncomingECX is copied toESI. If dword[ESI] is nonzero, its value is pushed to a direct call2111A650, outside pinned PE backing. Assuming normal return, EAX==3 producesAL=1 and an early return. If[ESI] is zero, or returnedEAX differs from3, control reaches the second branch.','witnesses':w('004C83A6','004C83A8','004C83AA','004C83AC','004C83AE','004C83B4','004C83B7','004C83B9'),'unknown':'External callee identity/effects, input object meaning, and validity of its pointer. No API/library/status name inferred.','falsified_by':'Different code/object, nonreturn/fault/reentry or independently supplied qualified callee behavior.'},
  {'conclusion':'Second branch reads[originalESI+4] as a new object pointer. NULL returnsAL=0. Nonnull invokes virtual slot+0x24 with pushed pointer to stack local[EBP-4], then pushed object pointer. Nonzero returnedEAX returnsAL=0. WithEAX zero, local low byte bit1 set returnsAL=0; otherwise AL=bool(lowbyte & 0x05).','witnesses':w('004C83C0','004C83C3','004C83C5','004C83C7','004C83C9','004C83CC','004C83CD','004C83CE','004C83D1','004C83D3','004C83D5','004C83D8','004C83DA','004C83DC','004C83DE','004C83E6'),'qualification':'Local predicate is mechanically proved, conditional on normal virtual return. The byte is read from a stack local; that virtual call initializing it is not proved without target evidence. Bit/status meanings and upper bytes are unqualified. Bit1 dominates; bits0/2 set produce true only whenbit1clear.','falsified_by':'Unknown virtual does not return, does not initialize the local, reenters/mutates state, or source bytes differ.'},
  {'conclusion':'Parent004C85E0 tests returnedAL and skips004C8240/004C83F0 whenAL!=0. This new local predicate qualifies the guard controlling those calls; it does not name the operations or establish a skill request.','parent_witness':'Parent raw004C8606 call004C83A0;004C860B testAL;004C860D jne004C8623. Parent handoff/source pins above.','falsified_by':'Different parent/helper implementation, nonnormal return, or different live object/epoch.'}
 ],
 'direct_send_observation':{'no_direct_004C6AE0_or_004C6770_in_this_body':True,'no_transitive_network_effects_proved':False,'wire_opcode_fields_direction':None,'actual_wire_obligation':False},
 'stops':{'external':raw['unbacked_external_boundary'],'virtual':raw['unknown_virtual_target'],'private_or_next_layer_followed':False,'timer_followed':False},
 'consumer_action':'After Root review, distinguish guard returningtrue from registry activation and row population. If later authorized same-session evidence is collected, establish actual object/backing and returnedAL before interpreting missing downstream effects. Do not label the predicate skills-ready/playing, inject a packet, or infer packet timing from these bytes.',
 'unknown_blocker_supplier_resume':{'unknown':'External2111A650 backing/identity/effects and actual virtual+24 implementation/output-byte provenance; runtime object epochs and session phases.','supplier':'Root-qualified lawful public helper/object evidence supplier or genuinely fresh Launcher evidence session. Private/next-layer restoration and refusal routes remain prohibited.','resume':'Only a new permitted question with qualified backing or exact fresh-instance evidence; no generic-process attachment or human_ready fabrication.','next_input_complete_task':None},
 'runtime':'No fresh runtime instance supplied; no attach,monitor,process,GUI test or instrumentation. Human automation authorization does not fabricate instance/READY/nonce/claim/lease/cleanup.',
 'native_goal_status':'blocked per actualget_goal this turn; full product objective unchanged','product_accepted':False,'GUI_accepted':False,'supervisor_review_pending':True,'consumer_adopted':False,
 'verification':{'source_SHA_matches':True,'77raw_bytes_fully_decoded':True,'artifact_matches_same_source_slice':Path(bp).read_bytes()==b,'external_raw_backing_absent':True,'exact_call_boundary_asserted':True,'old_PASS_replays':0},
 'command':'C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe -B bin-public-4c83a0-guard-boundary-20261008/derive.py','canonical_workdir':str(R)}
write(O/'handoff.json',d)
m={'task_id':T,'artifacts':[pin(p) for p in sorted(O.iterdir()) if p.is_file() and p.name not in ['delivery-manifest.json','command.stdout.txt','command.stderr.txt']],'command_logs':'Pinned after shell closes them; manifest itself externally pinned.'};write(O/'delivery-manifest.json',m)
for z in m['artifacts']:assert pin(Path(z['path']))==z
f={'task_id':T,'state':'review','delivery':pin(O/'handoff.json'),'delivery_manifest':pin(O/'delivery-manifest.json'),'evidence_level':'new public static local AL guard; unbacked external and virtual targets stopped','reported_to_supervisor':False,'supervisor_review_pending':True,'product_accepted':False,'actual_native_goal_status':'blocked','next_input_complete_task':None}
for name in ['監製進度.json','工作佇列.json']:
 p=R/name;q=json.loads(p.read_text(encoding='utf-8-sig'));q['bin_public_4c83a0_guard_boundary_20261008']=f;q['bin_public_4c8790_effects_20261008']['reported_to_supervisor']=True;write(p,q)
print('\n'.join(lines));print(json.dumps({'handoff':pin(O/'handoff.json'),'manifest':pin(O/'delivery-manifest.json'),'external_boundary_stopped':True,'virtual_boundary_stopped':True},ensure_ascii=False))

import json,struct,hashlib,sys
from pathlib import Path
from datetime import datetime,timezone,timedelta
import capstone
sys.stdout.reconfigure(encoding='utf-8')
O=Path(__file__).resolve().parent;R=O.parent;T='bin-public-toolbar-constructor-fields-20261008'
def sha(b):return hashlib.sha256(b).hexdigest().upper()
def pin(p):
 b=p.read_bytes();return {'path':str(p),'bytes':len(b),'sha256':sha(b)}
def write(p,x):p.write_text(json.dumps(x,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
cp=R/'skill-pane-activation-wire-boundary-20261008-contract.json';cb=cp.read_bytes();assert sha(cb)=='8D133F72AA4F9E05175B1F463E962EFD0495C9A23AB966A8CEC8C25E485D5229';prior=json.loads(cb)
pp=Path(prior['input_pins'][0]['path']);pe=pp.read_bytes();assert sha(pe)=='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
nt=struct.unpack_from('<I',pe,0x3c)[0];assert pe[nt:nt+4]==b'PE\0\0';opt=nt+24;assert struct.unpack_from('<I',pe,opt+28)[0]==0x400000
nsec=struct.unpack_from('<H',pe,nt+6)[0];optsz=struct.unpack_from('<H',pe,nt+20)[0];secs=[]
for i in range(nsec):
 p=opt+optsz+40*i;vs,rv,rs,rp=struct.unpack_from('<IIII',pe,p+8);secs.append({'index':i,'name_hex':pe[p:p+8].hex(),'virtual_size':vs,'rva':rv,'raw_size':rs,'raw_pointer':rp})
def mapped(va,n):
 rv=va-0x400000;ss=[s for s in secs if s['rva']<=rv and rv+n<=s['rva']+s['raw_size']];assert len(ss)==1
 s=ss[0];f=s['raw_pointer']+rv-s['rva'];assert f+n<=len(pe);return pe[f:f+n],f,s
md=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);md.detail=True;ranges=[];lines=[]
for label,va,n in [('public_specialized_constructor',0x49E570,0x47),('public_eight_argument_forwarder',0x49E110,0x41),('public_base_constructor_prefix_until_private_boundary',0x49CD70,0x94)]:
 b,f,s=mapped(va,n);bp=O/f'{va:08X}-{n:X}.bin';bp.write_bytes(b);ins=[]
 for x in md.disasm(b,va):
  z={'va':f'{x.address:08X}','rva':f'{x.address-0x400000:08X}','file_offset':f'{f+x.address-va:08X}','raw_hex':x.bytes.hex(),'size':x.size,'mnemonic':x.mnemonic,'operands':x.op_str}
  if x.mnemonic=='call' and x.operands[0].type==capstone.x86.X86_OP_IMM:z['direct_target']=f'{x.operands[0].imm:08X}'
  ins.append(z);lines.append(f"{z['va']} RVA={z['rva']} RAW={z['file_offset']} {z['raw_hex']} {z['mnemonic']} {z['operands']}")
 assert sum(x['size'] for x in ins)==n
 ranges.append({'label':label,'va':f'{va:08X}','rva':f'{va-0x400000:08X}','end_exclusive':f'{va+n:08X}','file_offset':f'{f:08X}','bytes':n,'section':s,'artifact':pin(bp),'instructions':ins})
assert ranges[0]['instructions'][-1]['mnemonic']=='ret' and ranges[0]['instructions'][-1]['operands']=='0x24'
assert ranges[1]['instructions'][-1]['mnemonic']=='ret' and ranges[1]['instructions'][-1]['operands']=='0x20'
assert ranges[2]['instructions'][-1]['va']=='0049CDFF' and ranges[2]['instructions'][-1]['direct_target']=='05AD0986'
prv=0x05AD0986-0x400000;assert not any(s['rva']<=prv<s['rva']+s['raw_size'] for s in secs)
allins={x['va']:x for r in ranges for x in r['instructions']}
def w(*vas):
 assert set(vas).issubset(allins)
 return [allins[v] for v in vas]
ap=Path(prior['input_pins'][1]['path']);ab=ap.read_bytes();assert sha(ab)==prior['input_pins'][1]['sha256'];ls=ab.decode('utf-8',errors='replace').splitlines();nav=[]
for name in ['sub_49E570','sub_49E110']:
 i=next(i for i,l in enumerate(ls) if l.startswith(name+' proc'));j=next(j for j in range(i,len(ls)) if ls[j].startswith(name+' endp'));nav.append({'label':name,'line':i+1,'text':'\n'.join(ls[i:j+1])})
write(O/'asm-navigation.json',{'source':pin(ap),'authority':False,'different_input_header':True,'procedure_boundaries_in_ASM_do_not_cover_raw_epilogues':True,'excerpts':nav})
(O/'disassembly.txt').write_text('\n'.join(lines)+'\n',encoding='utf-8')
raw={'source':{'path':str(pp),'bytes':len(pe),'sha256':sha(pe)},'imagebase':'00400000','sections':secs,'ranges':ranges,'tools':{'python':sys.version,'capstone':capstone.__version__}}
write(O/'raw-evidence.json',raw)
argmap=[{'outer_arg_index':0,'toolbar_value':'0x3ED','base_arg_index':0,'local_store':'object+0x20','store_va':'0049CDA5'}, {'outer_arg_index':1,'toolbar_value':'0x3EE','base_arg_index':None,'local_store':'object+0x48 after base returns','store_va':'0049E5A6'}, {'outer_arg_index':2,'toolbar_value':'536','base_arg_index':1,'local_store':'object+0x04','store_va':'0049CDBA'}, {'outer_arg_index':3,'toolbar_value':'460','base_arg_index':2,'local_store':'object+0x08','store_va':'0049CDBD'}, {'outer_arg_index':4,'toolbar_value':'0x004EBEC0','base_arg_index':3,'local_store':'object+0x2C','store_va':'0049CDB1'}, {'outer_arg_index':5,'toolbar_value':'captured dword[00834884]','base_arg_index':4,'local_store':'object+0x30','store_va':'0049CDB4'}, {'outer_arg_index':6,'toolbar_value':'3','base_arg_index':5,'local_store':None,'qualification':'forwarded only; later base stores beyond stopped prefix not claimed'}, {'outer_arg_index':7,'toolbar_value':'0','base_arg_index':6,'local_store':'object+0x39 low byte','store_va':'0049CDB7'}, {'outer_arg_index':8,'toolbar_value':'0','base_arg_index':7,'local_store':None,'qualification':'forwarded only; later base stores beyond stopped prefix not claimed'}]
for x in argmap:
 if 'store_va' in x:assert x['store_va'] in allins
now=datetime.now(timezone(timedelta(hours=8))).isoformat()
h={'schema':'lito.public-toolbar-constructor-field-association.v1','task_id':T,'owner':'LITO BIN Analysis','thread_id':'01a11705-96c0-74a0-b56d-ed486c318c95','state':'review','generated_at':now,
 'question':'Do the public constructor forwarding/local-store instructions associate the toolbar callback004EBEC0 and captured pane00834884 with concrete object fields, without assuming a live click binding?',
 'scope':'0049E570..0049E5B6 and0049E110..0049E150 complete public wrappers;0049CD70..0049CE03 prefix only, stopping after the private error call instruction0049CDFF. No private,resource virtual,vtable dispatch or deeper constructor expansion.',
 'source':raw['source'],'prior_activation_contract':pin(cp),'raw_evidence':pin(O/'raw-evidence.json'),'raw_ranges':[{k:r[k] for k in ['label','va','rva','end_exclusive','file_offset','bytes','artifact']} for r in ranges],
 'argument_store_map':argmap,
 'findings':[
  {'conclusion':'0049E570 accepts9 DWORD stack args and forwards8 to0049E110 with the originalECX object. It omits outerarg1, otherwise preserves ordering. On normal base return, it sets object vtable pointer004F9AD0, outerarg1 at+48, +4C=-1, returns object inEAX andret0x24.','witnesses':w('0049E573','0049E576','0049E57A','0049E598','0049E59D','0049E5A0','0049E5A6','0049E5A9','0049E5B0','0049E5B4'),'falsified_by':'Changed version, invalid original object, nonnormal base return, reentry or callee ABI changes.'},
  {'conclusion':'0049E110 forwards all8 DWORD args unchanged to0049CD70. On normal return it writes vtable004F99F8 and object+44=0x1F, returns original object andret0x20.','witnesses':w('0049E113','0049E11A','0049E138','0049E13D','0049E143','0049E14A','0049E14E'),'falsified_by':'Changed forwarding/store instructions, object identity or nonnormal callee return.'},
  {'conclusion':'Before any resource/private-error branch in captured0049CD70 prefix, and assuming prior0041AA20 returns normally, basearg3 is written toobject+2C andbasearg4 toobject+30. Composing raw toolbar stack pushes with these two wrappers therefore yields stores callback004EBEC0 at+2C and captured pane value at+30. Coordinates536/460 go to+4/+8; base ID3ED to+20; outer3EE is separately stored by outer wrapper at+48 after base returns. These are instruction-level associations, not proof that final live object fields remain those values or that any click invokes them.','witnesses':w('0049CD91','0049CD96','0049CD99','0049CD9C','0049CD9F','0049CDA2','0049CDA5','0049CDB1','0049CDB4','0049CDBA','0049CDBD'),'falsified_by':'Different toolbar argument order/ECX pointer, failing or state-mutating constructor call, later overwrites beyond captured prefix, wrong control instance/version, or different live callback dispatch ABI.'},
  {'conclusion':'Resource validation follows these stores in the captured prefix: object+20==-1 takes a branch outside prefix; signed>=0x1EC2 or missing834BD4 table takes an error branch; a nonnull table entry branches to0049CE64 outside prefix. Error branch calls00497C60 and then unbacked private05AD0986. No resource loading, error helper or later virtual target was followed.','witnesses':w('0049CDC0','0049CDC3','0049CDCD','0049CDCF','0049CDD4','0049CDD6','0049CDDC','0049CDDE','0049CDE0','0049CDE3','0049CDE5','0049CDF0','0049CDF7','0049CDFF'),'falsified_by':'Different resource table/ID/pointers or source bytes. Static branches do not prove resource success.'}
 ],
 'stopped_boundaries':{'public_setup0041AA20':'Observed direct call only; complete effects not expanded.','private05AD0986':{'va':'05AD0986','arithmetic_rva':'056D0986','raw_backed':False,'section':None,'file_offset':None,'followed':False},'resource_continuation0049CE64':'Outside captured prefix; not followed.','final_vtable004F9AD0':'Written pointer observed; no slot resolved or virtual method expanded.','no_guess':'No constructor/UI/scene binding beyond exact conditional local-store composition.'},
 'consumer_adoption_action':'After Root review, Launcher can use+2C/+30 as source-backed conditional callback/parameter store witnesses for this specific0049E570 control family, rather than assuming+24/+28 from another UI family. Verify exact live instance, constructor completion and post-construction values in a future separately qualified automated session. This does not authorize new hooks or prove physicalG/click/room semantics.',
 'wire_boundary':{'no_direct_004C6AE0_or_004C6770_in_captured_ranges':True,'whole_chain_network_absence':False,'wire_opcode_direction_fields':None,'skill_request_obligation':False},
 'unknowns_supplier_resume':{'unknown':'Remaining base constructor/resource success, final callback+parameter stability, actual virtual click-dispatch ABI/object registration, physical-key action mapping and actual same-session population/reset/draw.','supplier':'Root/Launcher for exact fresh session and qualified public control-dispatch question; private backing remains stopped.','resume':'New input-complete lawful public scope or genuinely fresh exact runtime session with all retained source/instance/READY/nonce/claim/lease/cleanup/tool gates.','next_input_complete_task':None},
 'runtime':{'fresh_instance_received':False,'attached_or_monitoring':False,'human_click_or_human_ready_fabricated':False,'live_or_instrumentation_actions':False},'native_goal_status':'blocked, per actual tool observation; no replacement Goal','product_accepted':False,'GUI_or_first_lobby_accepted':False,'supervisor_review_pending':True,'consumer_adopted':False,
 'validation':{'supplied_source_pins_match':True,'all_ranges_raw_backed_and_fully_decoded':True,'all_witness_addresses_valid':True,'unknown_private_boundary_not_followed':True,'old_PASS_replay':False},
 'command':'Python312 python.exe -B bin-public-toolbar-constructor-fields-20261008/derive.py; canonical BIN workdir; exact exit in sealed manifest','tools':raw['tools']}
assert not any(x.get('direct_target') in ['004C6AE0','004C6770','0042E550','0042E6D0','0042EA50'] for r in ranges for x in r['instructions'])
write(O/'handoff.json',h)
print('\n'.join(lines));print(json.dumps({'handoff_preseal':pin(O/'handoff.json'),'manifest_published':False,'private_destination_followed':False},ensure_ascii=False))

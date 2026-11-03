import hashlib,json,struct,sys
from pathlib import Path
from datetime import datetime,timezone,timedelta
import capstone
sys.stdout.reconfigure(encoding='utf-8')
OUT=Path(__file__).resolve().parent;ROOT=OUT.parent
TASK='bin-public-4c8790-effects-20261008'
PE_PATH=ROOT.parent/'lito登入器'/'舊版分析與驗收'/'runtime'/'LiTo.unpacked-oep-current.exe'
ASM_PATH=ROOT.parent/'lito登入器'/'舊版分析與驗收'/'ida-client-oep-v2'/'full-disassembly.asm'
PRIOR_PATH=ROOT/'skill-pane-activation-wire-boundary-20261008-contract.json'
def sha(b):return hashlib.sha256(b).hexdigest().upper()
def pin(p):
 b=p.read_bytes();return {'path':str(p),'bytes':len(b),'sha256':sha(b)}
def write(p,x):p.write_text(json.dumps(x,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
expected=[(PE_PATH,'4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'),(ASM_PATH,'2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A'),(PRIOR_PATH,'8D133F72AA4F9E05175B1F463E962EFD0495C9A23AB966A8CEC8C25E485D5229')]
inputs=[];snapshots=[]
for p,s in expected:
 b=p.read_bytes();assert sha(b)==s,('input mismatch',str(p));snapshots.append(b);inputs.append({'path':str(p),'bytes':len(b),'sha256':s})
pe,asm,prior=snapshots;prior=json.loads(prior)
assert prior['source_applicability']['fixed_PE']['sha256']==inputs[0]['sha256']
nt=struct.unpack_from('<I',pe,0x3c)[0];assert pe[:2]==b'MZ' and pe[nt:nt+4]==b'PE\0\0'
machine,nsec=struct.unpack_from('<HH',pe,nt+4);optsz=struct.unpack_from('<H',pe,nt+20)[0];opt=nt+24
assert machine==0x14c and struct.unpack_from('<H',pe,opt)[0]==0x10b
base=struct.unpack_from('<I',pe,opt+28)[0];assert base==0x400000
sections=[]
for k in range(nsec):
 o=opt+optsz+40*k;vs,rva,rs,rp=struct.unpack_from('<IIII',pe,o+8)
 sections.append({'index':k,'name_hex':pe[o:o+8].hex(),'virtual_size':vs,'rva':rva,'raw_size':rs,'raw_pointer':rp})
def raw(va,n):
 rva=va-base;matches=[s for s in sections if s['rva']<=rva and rva+n<=s['rva']+s['raw_size']]
 assert len(matches)==1,('missing or ambiguous raw backing',hex(va),n)
 s=matches[0];off=s['raw_pointer']+rva-s['rva'];assert off+n<=len(pe)
 return pe[off:off+n],off,s['index']
md=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);md.detail=True
ranges=[];lines=[]
for label,va,n in [('public_tail_thunk',0x4C8790,5),('public_direct_tail_target_body',0x4C85E0,0x45)]:
 b,off,sec=raw(va,n);bp=OUT/f'{va:08X}-{n:X}.bin';bp.write_bytes(b)
 instructions=[]
 for x in md.disasm(b,va):
  v={'va':f'{x.address:08X}','rva':f'{x.address-base:08X}','file_offset':f'{off+x.address-va:08X}','size':x.size,'raw_hex':x.bytes.hex(),'mnemonic':x.mnemonic,'operands':x.op_str}
  if x.mnemonic in ['call','jmp'] and x.operands and x.operands[0].type==capstone.x86.X86_OP_IMM:v['direct_target']=f'{x.operands[0].imm:08X}'
  instructions.append(v);lines.append(f"{v['va']} RVA={v['rva']} RAW={v['file_offset']} {v['raw_hex']} {v['mnemonic']} {v['operands']}")
 assert sum(x['size'] for x in instructions)==n,('incomplete decode',label)
 ranges.append({'label':label,'va':f'{va:08X}','rva':f'{va-base:08X}','end_exclusive':f'{va+n:08X}','file_offset':f'{off:08X}','section_index':sec,'bytes':n,'raw_hex':b.hex(),'artifact':pin(bp),'instructions':instructions})
assert ranges[0]['instructions'][0]['direct_target']=='004C85E0'
assert ranges[1]['instructions'][-1]['mnemonic']=='ret' and ranges[1]['instructions'][-1]['va']=='004C8624'
transfers=[dict(x,range_label=r['label']) for r in ranges for x in r['instructions'] if x['mnemonic'] in ['call','jmp']]
assert [x['direct_target'] for x in transfers]==['004C85E0','004C7EC0','004C83A0','004C8240','004C83F0']
assert not any(x.get('direct_target') in ['004C6AE0','004C6770','0042E550','0042E6D0','0042EA50'] for x in transfers)
assert all(x.get('direct_target') is not None for x in transfers)
nav=[];als=asm.decode('utf-8',errors='replace').splitlines()
nav.append({'label':'different ASM input header; navigation only','line':1,'text':'\n'.join(als[:20])})
for name in ['sub_4C8790','sub_4C85E0']:
 start=next(i for i,l in enumerate(als) if l.startswith(name+' proc'))
 end=next(i for i in range(start,len(als)) if als[i].startswith(name+' endp'))
 nav.append({'label':name,'line':start+1,'end_line':end+1,'text':'\n'.join(als[start:end+1])})
write(OUT/'asm-navigation.json',nav)
(OUT/'disassembly.txt').write_text('\n'.join(lines)+'\n',encoding='utf-8')
evidence={'task_id':TASK,'input_pins':inputs,'source_same_snapshot_for_SHA_and_parser':True,'PE':{'machine':machine,'image_base':f'{base:08X}','sections':sections},'tools':{'python':sys.version,'capstone':capstone.__version__},'ranges':ranges,'direct_transfers':transfers,'all_code_fully_decoded':True,'raw_bytes_authoritative':True}
write(OUT/'raw-evidence.json',evidence)
for r in ranges:assert raw(int(r['va'],16),r['bytes'])[0]==Path(r['artifact']['path']).read_bytes()
now=datetime.now(timezone(timedelta(hours=8))).isoformat()
def witness(*vas):return [x for r in ranges for x in r['instructions'] if x['va'] in vas]
handoff={
 'schema':'lito.bounded-public-4c8790-effects.v1','task_id':TASK,'owner':'LITO BIN Analysis','thread_id':'01a11705-96c0-74a0-b56d-ed486c318c95','state':'review','generated_at':now,
 'question':'What observable direct effects and direct call boundary does public skill-toolbar callback tail target VA004C8790 have in the pinned fixed PE?',
 'new_gap':'Previous activation contract explicitly stopped at004EBECF->004C8790. This task qualifies that public thunk and its immediate public tail target only; previous action19/packet population analysis is not replayed.',
 'input_pins':inputs,'source_version_applicability':'Exact fixed PE SHA4271D92F... at imagebase00400000; VA/RVA mapping from its parsed section headers. Same source snapshot hashed and decoded. Other versions and live memory unqualified.',
 'ASM_navigation_boundary':'ASM file SHA2A3A6E10... has header input SHA AEB05C78FE029EA6D3F2EF349245669E47E1E83B1C005E32D4663C780627B5A6. It suggests function bounds only; all instruction conclusions derive from fixed-PE raw bytes.',
 'raw_evidence':pin(OUT/'raw-evidence.json'),'ranges':[{k:r[k] for k in ['label','va','rva','end_exclusive','file_offset','section_index','bytes','artifact']} for r in ranges],
 'instruction_boundaries':'004C8790 decodes as one5-byteE9 with raw target004C85E0. The immediate target decodes sequentially from its public entry through its shared popESI/ret ending004C8625; all internal short branches land on decoded instruction004C8623. No arbitrary byte-pattern scan or unknown target body expansion.',
 'findings':[
  {'id':'F1','conclusion':'004C8790 is a pure direct tail jump to004C85E0: no local memory write, stack change or packet assembly in the5-byte thunk. The immediate body is included to make the tail effect meaningful.','witnesses':witness('004C8790'),'falsified_by':'Different source bytes or runtime patch at this thunk.'},
  {'id':'F2','conclusion':'004C85E0 saves incomingECX inEAX, savesESI on the stack and loadsECX from global008343EC. Zero008343EC returns via004C8623. With nonzeroECX it loadsEDX from00635630; zeroEDX also returns. If both are nonzero, it pushes saved incomingECX and directly calls004C7EC0 withECX still008343EC.','witnesses':witness('004C85E0','004C85E3','004C85E9','004C85EB','004C85ED','004C85F3','004C85F5','004C85F7','004C85F8'),'unknown':'Global field semantics, initialized/live pointers and004C7EC0 resource/object lookup meaning are not qualified. Stack argument presence is proved; its callee encoding/ABI is not independently expanded.','falsified_by':'Zero guards, different live values/callee ABI, modified version or call failing/throwing.'},
  {'id':'F3','conclusion':'004C7EC0 resultEAX becomesESI; NULL returns. Otherwise the body reads[ESI+8] intoECX and calls004C83A0. If returnedAL is nonzero it returns. WithAL zero it reads[ESI+8] again, pushes0x7F and calls004C8240, then reads[ESI+8] again, pushes1 and calls004C83F0, finally restoresESI and returns.','witnesses':witness('004C85FD','004C85FF','004C8601','004C8603','004C8606','004C860B','004C860D','004C860F','004C8612','004C8614','004C8619','004C861C','004C861E','004C8623','004C8624'),'conditional_effects':'Normal return and valid object backing assumed. Four direct helper invocations are observable code effects; their transitive writes, virtual/API calls and runtime success remain unresolved. No helper expansion.','falsified_by':'NULL lookup result, nonzeroAL guard, invalid or mutated[ESI+8], exception/nonreturn/reentry or changed helper semantics.'},
  {'id':'F4','conclusion':'For the prior toolbar callback inputECX=0xED andEDX=0x7F, the body passes saved0xED to004C7EC0 when both global guards succeed. IncomingEDX is not read before being overwritten from00635630. The later pushed0x7F is an immediate constant in004C85E0, not data propagated from incomingEDX.','context_source':inputs[2],'witnesses':witness('004C85E0','004C85ED','004C85F7','004C8612'),'unknown':'0xED/0x7F/1 and these globals are not proved packet opcodes, skills IDs, sound/resource IDs, volume values or scene states. No guessed naming is supplied.','falsified_by':'Different callback inputs/version, guards preventing calls or independently qualified callee semantics requiring a revised interpretation.'},
  {'id':'F5','conclusion':'Neither the thunk nor complete immediate target contains an explicit global/object memory store, direct packet formatter/send call to004C6AE0/004C6770, or an indirect call instruction. Stack pushes/call/return are present. All call-out boundaries in this scope are direct public addresses.','witnesses':transfers,'qualification':'This is a local instruction observation, not proof of no external/network effects. Helpers may have transitive effects, including unknown virtual targets; no such bodies were inspected here. No packet obligation or wire direction/fields/encoding is established.','falsified_by':'Different raw instructions or independently qualified helper evidence identifying transitive network behavior.'}
 ],
 'direct_call_boundary':{'tail_target':'004C85E0','helper_targets':['004C7EC0','004C83A0','004C8240','004C83F0'],'expanded_helper_targets':[],'private_or_refused_targets_followed':[],'direct_send_found':False,'no_transitive_send_proved':False,'actual_wire_obligation_proved':False,'direction':None,'opcode':None,'fields':None,'encoding':None,'trigger_state':None,'null_wire_fields_mean':'Unqualified, not whole-program absence.'},
 'consumer_adoption_action':{'consumer':'Launcher through Root review','action':'Refine the skill-toolbar evidence model: registry toggle is followed by this guarded helper chain, with lookup input0xED and immediate arguments0x7F/1. Do not interpret incomingEDX=0x7F as packet content or add an on-open skills request. Continue distinguishing local activation from same-session skill population/reset and visible draw.','implementation_change':'No concrete native repair or packet change is justified by these local bytes.','first_lobby_joined_room_WORLD':'None of the guarded globals, result objects or constants is assigned a login/room/WORLD meaning; first-lobby reachability and actual pane contents remain separate.'},
 'unknowns_and_blockers':{'unknown':'Direct helper implementations and any transitive virtual effects; global00635630/008343EC semantics/live state; object[ESI+8] backing/epoch; actual first-lobby/room/WORLD execution and skill pane population/draw.','supplier':'Root-qualified public fixed-PE task for a specific helper semantics question, or fresh Launcher same-session evidence with exact instance/session provenance.','resume':'A new bounded input-complete public question outside retained stops, or Root notification of a genuinely fresh qualified runtime instance. Generic process name or exited fresh2 evidence cannot authorize attachment.','next_input_complete_task':None},
 'runtime_and_authorization':{'new_human_automation_authorization_recorded':True,'former_manual_G_Start_prerequisite_not_assumed':True,'human_click_or_human_ready_fabricated':False,'fresh_instance_received':False,'attached_or_monitoring':False,'process_started':False,'runtime_evidence_collected':False,'native_tool_restrictions_preserved':True,'PID_creation_READY_nonce_claim_lease_cleanup_preserved':True},
 'goal_status':{'actual_native_status':'blocked','actual_get_goal_observation':'This turn returnedblocked, updatedAt1791409406; new task execution does not invent an active Goal.','exact_full_product_objective_retained':True,'product_complete':False},
 'validation':{'supplied_input_SHA_match':True,'all_ranges_raw_backed':True,'all_code_bytes_decoded':True,'exact_direct_transfer_sequence_asserted':True,'bounded_raw_artifacts_match_source':True,'old_PASS_replays':0,'product_accepted':False,'supervisor_review_pending':True,'consumer_adopted':False},
 'preserved_stops':['No private backing/next layer','No0042E550/0042E6D0/0042EA50 expansion','No ServerT291/T27/refund/rebid/castle route','No refusal bypass/rephrase/tool substitution/owner transfer','No cross-owner writes or Root index/workflow writes'],
 'commands':{'workdir':str(ROOT),'execution':'C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe -B bin-public-4c8790-effects-20261008/derive.py','exit_report':'Exit code comes from original shell tool receipt; no old tests executed.','tools':evidence['tools']},
 'owned_files':'This task namespace and only new bin_public_4c8790_effects_20261008 fields in existing owned progress/queue.'
}
write(OUT/'handoff.json',handoff)
check={'input_pins_match':True,'raw_ranges_source_match':True,'fully_decoded':True,'exact_direct_transfer_sequence':[x['direct_target'] for x in transfers],'all_private_stop_targets_unvisited':True,'derived_handoff_SHA':pin(OUT/'handoff.json'),'checked_at':now}
write(OUT/'verification.json',check)
manifest={'task_id':TASK,'generated_at':now,'artifacts':[pin(p) for p in sorted(OUT.iterdir()) if p.is_file() and p.name not in ['delivery-manifest.json','command.stdout.txt','command.stderr.txt']],'excluded_live_output_logs':'command stdout/stderr are captured by shell after this program exits and separately pinned in owned metadata. Manifest does not self-hash.'}
write(OUT/'delivery-manifest.json',manifest)
for x in manifest['artifacts']:assert pin(Path(x['path']))==x
field={'task_id':TASK,'state':'review','updated_at':now,'delivery':pin(OUT/'handoff.json'),'delivery_manifest':pin(OUT/'delivery-manifest.json'),'input_pins':inputs,'evidence_level':'new bounded fixed-PE static thunk and immediate public target; transitive helper effects unknown','supervisor_review_pending':True,'reported_to_supervisor':False,'consumer_adopted':False,'product_accepted':False,'actual_native_goal_status':'blocked','next_input_complete_task':None}
for name in ['監製進度.json','工作佇列.json']:
 p=ROOT/name;d=json.loads(p.read_text(encoding='utf-8-sig'));d['bin_public_4c8790_effects_20261008']=field;write(p,d)
print('\n'.join(lines));print(json.dumps({'handoff':pin(OUT/'handoff.json'),'manifest':pin(OUT/'delivery-manifest.json'),'validation':pin(OUT/'verification.json'),'new_qualified_ranges':len(ranges),'product_accepted':False},ensure_ascii=False))

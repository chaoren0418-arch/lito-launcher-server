"""Batch11 finite S4D5 fixed-PE contract; reads existing captures, never runs game/Java."""
from audit import *

OUT=ROOT/'S4D-mana-point'
FIXED='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
BATCH10='EC24D44166CB1E738B6FF22C9B1AFE2E606AB6DD4B3CFECEF20FE42704074C92'
SERVER=ROOT.parent/'lito伺服器/LiTo381-Server/src/main/java/local/lito381/server'
HANDOFF=ROOT.parent/'lito重建版/LiTo381-Modern/logs/continuation-20261003/r509-character-selection-channel-handoff.json'
ORIGIN=ROOT.parent/'lito伺服器/server-skill-publication-origin-comparison.json'

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    OUT.mkdir(exist_ok=True)
    expected_sources=[(BIN,'CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292'),
      (PE,FIXED),(ASM,'2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A')]
    sources=[]
    for p,h in expected_sources:
        pin=record(p);assert pin['sha256']==h;sources.append(pin)
    previous=load('S0C-status-owner-contract.json');assert record(ROOT/'S0C-status-owner-contract.json')['sha256']==BATCH10
    preserved=previous['prior_primary_pins']+[{**record(ROOT/'S0C-status-owner-contract.json'),'expected_sha256':BATCH10,'match':True}]
    for p in preserved:assert record(Path(p['path']))['sha256']==p['expected_sha256']
    origin=record(ORIGIN);assert origin['sha256']=='A16B1FA66350D50056B78CF5B62F8ABECBC6D399D048B3409A66779C565B6963'
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    ranges=[];inherited=[];ins={};owners={}
    def install(row):
        raw=Path(row['output']['path']).read_bytes();va=int(row['va'],16)
        assert raw==pe.get_data(va-0x400000,len(raw))
        for x in row.get('instructions',[]):
            item=dict(x);item['raw_hex']=item.pop('bytes',item.get('raw_hex'));at=int(item['va'],16)
            assert bytes.fromhex(item['raw_hex'])==pe.get_data(at-0x400000,len(bytes.fromhex(item['raw_hex'])))
            ins[at]=item;owners[at]=row['output']
    for file,va in [('skill-gates/ranges.json',0x4B29DB),('skill-gates/ranges.json',0x4B313F),
      ('S0C-status-owner/ranges.json',0x4B2970),('S0C-status-owner/ranges.json',0x4C3E90),
      ('S0C-status-owner/ranges.json',0x8315E0),('S0C-status-owner/ranges.json',0x4A36F0),
      ('receive-boundary/ranges.json',0x4B37C0)]:
        row=next(x for x in load(file)['ranges']if int(x['va'],16)==va)
        assert record(Path(row['output']['path']))==row['output'];install(row)
        inherited.append({'index':record(ROOT/file),'range':row['output'],'va':row['va'],
          'bytes_compared_to_fixed_PE':True,'old_producer_or_case_replayed':False})
    def capture(label,va,n,kind='code'):
        m=mapping(pe,va);assert m['raw_backed'];raw=pe.get_data(va-0x400000,n)
        ds=[]if kind=='data'else list(cs.disasm(raw,va))
        assert len(raw)==n and(kind=='data'or sum(i.size for i in ds)==n)
        path=OUT/f'ranges/{va:08X}-{n:X}.bin';path.parent.mkdir(exist_ok=True);path.write_bytes(raw)
        row={**m,'label':label,'kind':kind,'bytes':n,'output':record(path),'decoded_bytes':sum(i.size for i in ds),
          'instructions':[{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in ds]}
        ranges.append(row);install(row)
    capture('S4D_complete_handler',0x4AEFB0,0x1F)
    capture('S4D_single_direct_caller_case',0x4B2D07,0xC)
    for x in [('primary_opcode4D_map',0x4B34BD,1),('fallback_opcode4D_map',0x4B3701,1),
      ('primary_jump_slot67',0x4B3314,4),('fallback_jump_slot27',0x4B35DC,4)]:capture(*x,'data')
    def w(va):return {'fixed_PE_sha256':FIXED,**mapping(pe,va),**{k:v for k,v in ins[va].items()if k!='va'},'range':owners[va]}
    expected_new=[(0x4AEFB0,'push','esi'),(0x4AEFB1,'mov','esi, ecx'),(0x4AEFB3,'inc','esi'),
      (0x4AEFB4,'mov','ecx, 0x8315e0'),(0x4AEFB9,'movsx','eax, word ptr [esi]'),(0x4AEFBC,'push','eax'),
      (0x4AEFBD,'call','0x4c3e90'),(0x4AEFC2,'mov','cx, word ptr [esi + 2]'),
      (0x4AEFC6,'mov','word ptr [0x8348ba], cx'),(0x4AEFCD,'pop','esi'),(0x4AEFCE,'ret',''),
      (0x4B2D07,'mov','ecx, esi'),(0x4B2D09,'call','0x4aefb0'),(0x4B2D0E,'pop','esi'),
      (0x4B2D0F,'mov','esp, ebp'),(0x4B2D11,'pop','ebp'),(0x4B2D12,'ret','')]
    expected_inherited=[(0x4B29DB,'cmp','dword ptr [0x64bbd4], 4'),(0x4B29E8,'cmp','dword ptr [0x64af98], 3'),
      (0x4B29EF,'jl','0x4b313f'),(0x4B2A06,'mov','cl, byte ptr [eax + 0x4b3470]'),
      (0x4B2A0C,'jmp','dword ptr [ecx*4 + 0x4b3208]'),(0x4B3150,'mov','dl, byte ptr [eax + 0x4b36b4]'),
      (0x4B3156,'jmp','dword ptr [edx*4 + 0x4b3570]'),(0x4C3E97,'call','0x4c3ca0'),
      (0x4C3EA1,'call','0x4c3ca0'),(0x4C3EB6,'call','0x773dc650'),(0x4C3ECA,'mov','edx, dword ptr [ebp + 8]'),
      (0x4C3ECD,'xor','edx, ecx'),(0x4C3ED7,'mov','dword ptr [ecx + eax*4], edx'),
      (0x4C3EE8,'ret','4'),(0x4A37B2,'and','edx, 0xffff'),(0x4A37B9,'call','0x4c3e90')]
    assertions=[]
    for fresh,entries in [(True,expected_new),(False,expected_inherited)]:
        for va,mn,op in entries:
            assert(ins[va]['mnemonic'],ins[va]['operands'])==(mn,op),(hex(va),ins[va])
            assertions.append({**w(va),'expected_mnemonic':mn,'expected_operands':op,'new_instruction':fresh,'match':True})
    dispatch=[]
    for label,at,entry,slot in [('mode4_signed_phase_ge3',0x4B34BD,0x4B3314,67),('fallback',0x4B3701,0x4B35DC,27)]:
        byte=pe.get_data(at-0x400000,1);target=pe.get_data(entry-0x400000,4)
        assert byte==bytes([slot]) and target==struct.pack('<I',0x4B2D07)
        dispatch.append({'branch':label,'opcode':'0x4D','opcode_decimal':77,'map':mapping(pe,at),'map_raw_hex':byte.hex(),
          'slot':slot,'jump_entry':mapping(pe,entry),'jump_raw_hex':target.hex(),'direct_case':'004B2D07','handler':'004AEFB0'})
    fields=[{'name':'current_mp','body_offset':1,'width':2,'endian':'LE','wire_storage':'raw16bits',
      'native_interpretation':'i16 sign-extended to i32','signed_range':[-32768,32767],
      'native_destination':'protected object 008315E0 via 004C3E90; not direct plain DWORD at object base',
      'load_witness':w(0x4AEFB9),'argument_witness':w(0x4AEFBC),'setter_witness':w(0x4AEFBD)},
      {'name':'maximum_mp','body_offset':3,'width':2,'endian':'LE','wire_storage':'raw16bits',
       'native_interpretation':'word load/store; no sign or zero extension establishes downstream arithmetic interpretation',
       'raw_unsigned_range':[0,65535],'native_destination':'WORD008348BA; exactly2 bytes written',
       'load_witness':w(0x4AEFC2),'store_witness':w(0x4AEFC6)}]
    wire={'direction':'server_to_client','opcode':'0x4D','name_from_existing_server':'S_MANA_POINT','body_including_opcode':5,
      'parameter_bytes':4,'total_wire_bytes_in_existing_capture':7,'length_header_bytes':2,'fields':fields,
      'not_a_format_string_reader':True,'string_fields':0,'object_or_owner_identity_fields':0,
      'length_semantics':'Body pointer ECX, then ESI=body+1. Direct WORD reads at+1/+3. No length argument, no exact-size guard, no consumed-pointer return; expected5 is structural, not a native rejection guarantee.',
      'ordering':'Current protected setter call occurs before maximum WORD load/store. Whole native update requires that call to return.',
      'S0C_distinction':{'evidence':record(ROOT/'S0C-status-owner-contract.json'),
        'S0C_current_mp':'u16 maskedFFFF at004A37B2 before same004C3E90 setter',
        'S4D_current_mp':'i16 sign-extension at004AEFB9; no maskFFFF',
        'same_high_bit_payload_example':{'raw_word':'8000','S0C_argument_i32':32768,'S4D_argument_i32':-32768},
        'same_current_destination':'008315E0','same_maximum_destination':'WORD008348BA'}}
    write('S4D-mana-point/fields.json',wire)
    def decode(body):
        if len(body)!=5:raise ValueError(f'Audit exact5 required, actual{len(body)}; not native length guard')
        if body[0]!=0x4D:raise ValueError('Audit opcode mismatch; native body assumes dispatcher')
        bits=int.from_bytes(body[1:3],'little');current=bits-65536 if bits&0x8000 else bits
        maximum=int.from_bytes(body[3:5],'little')
        return {'current_mp_raw_u16':bits,'current_mp_native_i32':current,'current_mp_setter_argument_hex':f'{current&0xFFFFFFFF:08X}',
          'maximum_mp_raw_u16':maximum,'maximum_mp_native_store_hex':body[3:5].hex(),
          'maximum_mp_downstream_signedness':'not established by this handler','fields_consumed_structurally':4,
          'body_bytes_required_structurally':5,'native_protected_storage_success_proven':False}
    mh=json.loads(HANDOFF.read_text(encoding='utf-8'));cp=next(x for x in mh['owned_runtime_evidence']if x['path'].endswith('frames.jsonl'))
    assert record(Path(cp['path']))==cp
    records=[json.loads(s)for s in Path(cp['path']).read_text(encoding='utf-8').splitlines()];actual=[]
    for i,r in enumerate(records):
        if r['direction']=='server_to_client' and r['opcode'].lower()=='0x4d':
            body=bytes.fromhex(r['plain']);assert len(body)==5 and r['wire_length']==7
            assert records[i-1]['direction']=='server_to_client'and records[i-1]['opcode'].lower()=='0x0c'
            previous_body=bytes.fromhex(records[i-1]['plain']);assert len(previous_body)==37
            decoded=decode(body);assert decoded['current_mp_native_i32']==decoded['maximum_mp_raw_u16']==100
            assert int.from_bytes(previous_body[20:22],'little')==100 and int.from_bytes(previous_body[22:24],'little')==100
            path=OUT/f'frames/AD368-r509-line{i+1:03}.plain.bin';path.parent.mkdir(exist_ok=True);path.write_bytes(body)
            actual.append({'capture_line':i+1,'capture_record':r,'body':record(path),'decode':decoded,
              'immediately_previous_capture_line':i,'immediately_previous_record':records[i-1],
              'previous_S0C_same_MP':True,'wire_schema_difference':False,
              'scope':'Existing sealed capture only; no native handler or Modern consumer execution/readiness proof'})
    assert [x['capture_line']for x in actual]==[44,74]
    write('S4D-mana-point/AD368-sealed-frame-comparison.json',{'source_handoff':record(HANDOFF),'sealed_capture':cp,
      'R509_reported_state':mh['state'],'actual_existing_records':actual,'new_capture':False,'wire_difference_found':False,
      'R509_boundary':'S4D appears immediately after retained first unsupportedS0C37; this static batch supplies next contract, does not assert Modern has reached or accepted it.'})
    checks=[]
    for name,current,maxbits,expected in [('zero_current',0,1,0),('baseline_signed_ceiling',0x7FFF,0x7FFF,32767),
      ('sign_bit_set',0x8000,0xFFFF,-32768),('negative_one',0xFFFF,0x8000,-1),('nonsymmetric_endian_order',0x1234,0xABCD,4660)]:
        body=b'\x4d'+struct.pack('<HH',current,maxbits);d=decode(body);assert d['current_mp_native_i32']==expected and d['maximum_mp_raw_u16']==maxbits
        checks.append({'case':name,'plain_hex':body.hex(),'decoded':d,'expected_native_current_i32':expected,
          'expected_maximum_raw_u16':maxbits,'synthetic_code_derived_not_real_wire_or_native_execution':True,'match':True})
    for name,body in [('truncated4',bytes.fromhex('4d640064')),('trailing6',bytes.fromhex('4d6400640000')),('wrong_opcode',bytes.fromhex('4c64006400'))]:
        try:decode(body);raise AssertionError('Audit strict model should reject')
        except ValueError as e:checks.append({'case':name,'plain_hex':body.hex(),'audit_rejection':str(e),'native_rejection_not_claimed':True,'match':True})
    write('S4D-mana-point/new-code-derived-cases.json',{'checks':checks,'new_static_checks':8,'new_native_Java_or_producer_runs':0,
      'high_bit_values_not_claimed_as_real_AD368_frames':True,'GUI_or_owner_qualification':False})
    setterrow=next(x for x in load('S0C-status-owner/ranges.json')['ranges']if int(x['va'],16)==0x4C3E90)
    calls=[{'body':'S4D_complete_handler','call':w(0x4AEFBD),'destination':mapping(pe,0x4C3E90),'complete_direct_body_reused':setterrow['output']}]
    for at,target,reason in [(0x4C3E97,0x4C3CA0,'Second-layer helper raw-backed, deliberately not expanded; key generation semantics unknown'),
      (0x4C3EA1,0x4C3CA0,'Same second-layer helper used to populate16DWORD storage'),
      (0x4C3EB6,0x773DC650,'Private/external opaque random-like target; outside fixedPE raw, no body or return-value qualification')]:
        calls.append({'body':'protected_setter_reused','call':w(at),'destination':mapping(pe,target),'boundary':reason,
          'target_body_sha256':None,'target_body_status':'Raw-backed second layer intentionally not captured'if target==0x4C3CA0 else'No body in fixedPE; body SHA unavailable',
          'deeper_expansion':False})
    negative=[]
    for row in [ranges[0],ranges[1],setterrow]:
        assert not any('0x834888'in x['operands']or(x['mnemonic']=='call'and x['operands']=='0x4c9190')for x in row['instructions'])
        negative.append({'range':row['output'],'explicit834888_operands':0,'explicit_owner_constructor4C9190_calls':0,
          'scope':'Only this complete bounded body; arbitrary aliases and deeper helpers not excluded'})
    boundaries={'calls':calls,'globals':{'current_protected_object':mapping(pe,0x8315E0),'maximum_word':mapping(pe,0x8348BA)},
      'protected_setter':{'body':setterrow['output'],'state_layout':'object+4 points to16DWORD storage; object+8 is XORkey; object+0 is encoded index (XOR B104FA87). Not a plain MP global.',
        'visible_order':'Calls4C3CA0 for key; initializes16 slots; external773DC650 result selects opaque index; XORs exact32bit signed-extended input with key into selected slot; encoded index stored; returns object pointer, ret4.',
        'live_preconditions':'Protected object and object+4 storage valid; no NULL or allocation/init guard in this setter. External/helper completion and valid index unproven.',
        'maximum_update_dependency':'4AEFC2/C6 reached only after setter returns; static decode of both fields is not native whole-message completion.'},
      'owner':{'explicit_skill_owner_create_test_require':False,'bounded_absence':negative,'entire_program_or_alias_absence_proven':False,
        'owner_or_player_ready':False,'S4D_has_object_identity_field':False,
        'server_origin_correction':{'source':origin,'status':json.loads(ORIGIN.read_text(encoding='utf-8'))['status'],
          'adopted':'Ordinary first/reentry S1E is test-only overlay hook, absent from originalB1/AD368. Do not use that hook or S4D/S0C decode as native owner or publication timing evidence.'}},
      'receive_and_dispatch':'Inherited complete-body receive pump synchronous dispatch. Both selectors share sameS4D target. Optional diagnostics64AFA2 can invoke private/external logging before selectors; prior boundary retained.',
      'no_other_direct_effects':'Complete11-instruction S4D handler has one direct call and one maximum word store. No direct packet send, actor lookup, effect allocation, skill owner publication or readiness gate.'}
    write('S4D-mana-point/direct-call-state-boundaries.json',boundaries)
    def excerpt(p,a,b):
        lines=p.read_text(encoding='utf-8').splitlines();return {'source':record(p),'first_line':a,'last_line':b,'text':'\n'.join(f'{j+1}: {lines[j]}'for j in range(a-1,b))}
    protocol=SERVER/'Protocol.java';numeric=SERVER/'NumericBreakthrough.java';session=SERVER/'ClientSession.java'
    provider={'sources':[record(x)for x in [protocol,numeric,session]],'read_only_excerpts':[excerpt(protocol,293,307),
      excerpt(protocol,1024,1044),excerpt(protocol,5340,5348),excerpt(numeric,1,40),excerpt(session,12291,12298)],
      'native_baseline':'ExistingmanaPoint opcode4D,h(current),h(maximum) exactly matches5-byte baseline shape. Validators require0<=current<=maximum,1<=maximum<=32767 when numeric disabled.',
      'native_domain_vs_provider':'Fixed native current sign-extends all16bit patterns including negatives; maximum copied as raw16bits, no positivity/order validation. Provider restricts gameplay domain; synthetic high-bit native examples are not new real wire differences.',
      'patched_profile':'Writer opcode S_HIT_POINT orS_MANA_POINT with NumericBreakthrough.enabled (schema>=1) widens nexttwoh to LE dwords; resultingS4D body9, patched receiver required. Outside this fixed-native5 contract; no current environment setting inferred.',
      'source_sequence':'ExistingClientSession fragment calls sendNativeBattleStatus thenmanaPoint. Sealedrecords lines43/44 and73/74 corroborate current sequence for those captures only.',
      'classification':'KNOWN_CONTRACT_CORROBORATION','new_wire_differences':0,'new_opcodes':0,'server_product_change_required_by_this_batch':False,
      'Java_SQL_or_old_PASS_executed':False,'server_origin_context':origin}
    write('S4D-mana-point/server-method-comparison.json',provider)
    lines=ASM.read_text(encoding='utf-8').splitlines()
    navigation=[]
    for label,a,b in [('completeS4Dhandler',365192,365206),('singleS4Ddispatchcase',373207,373218),
      ('primaryselector',372374,372391),('fallbackselector',374420,374435),('map_navigation_only',374646,374687)]:
        navigation.append({'label':label,'first_line':a,'last_line':b,'text':'\n'.join(f'{j+1}: {lines[j]}'for j in range(a-1,b))})
    write('S4D-mana-point/ASM-navigation.json',{'source':sources[2],'navigation':navigation,
      'authority':'IDA navigation labels only; inputfingerprint differs. Every claimed instruction/data is verified against fixedPE raw, no neighboringopcode expansion.'})
    write('S4D-mana-point/ranges.json',{'fixed_PE':sources[1],'ranges':ranges,'inherited':inherited,'all_new_code_fully_decoded':True})
    verification={'fixed_PE':sources[1],'new_raw_ranges':len(ranges),'new_code_ranges':2,'new_data_ranges':4,
      'new_instructions_asserted':len(expected_new),'inherited_instruction_assertions':len(expected_inherited),'instruction_assertions':assertions,
      'prior_primary_pins':preserved,'new_and_inherited_bytes_match_fixed_PE':True,'dispatch_data_assertions':dispatch,
      'actual_existing_frame_count':2,'actual_field_values':4,'new_static_checks':8,'all_static_checks_match':True,
      'previous_delivery':record(ROOT/'delivery-verification.json'),'previous_manifest':record(ROOT/'artifact-manifest.json')}
    write('S4D-mana-point/new-evidence-verification.json',verification)
    reusable=[record(p)for p in OUT.rglob('*')if p.is_file()]
    report={'schema':'lito.native.S4D5.mana-point.contract.batch11.v1','updated_at':stamp(),'status':'FINITE_STATIC_S4D5_CONTRACT_COMPLETE',
      'sources':sources,'scope':'S4D only: two opcode entries, complete handler, single direct caller case, necessary direct setter reused; stop at setter second-layer/external targets. No50/51/52/56 or wholeprogram expansion.',
      'prior_primary_pins':preserved,'inherited_evidence':inherited,'reusable_outputs':sorted(reusable,key=lambda x:x['path']),
      'dispatch':dispatch,'wire':wire,'state_and_call_boundaries':boundaries,'AD368_comparison':record(OUT/'AD368-sealed-frame-comparison.json'),
      'server_comparison':record(OUT/'server-method-comparison.json'),'server_handoff':{'classification':'KNOWN_CONTRACT_CORROBORATION','new_wire_differences':0,'new_opcodes':0,'packet_delta_unchanged':True},
      'runtime_acceptance':{'live_original_handler_completed':False,'protected_storage_initialized':False,'skill_owner_ready':False,
        'actor_ready':False,'player_ready':False,'GUI_accepted':False,'overall_unpacking_accepted':False},
      'constraints':{'writes_only_own_directory':True,'native_observer_live_process_input_injection_screenshot_runs':0,
        'Java_SQL_or_producer_runs':0,'new_capture_or_archive_extraction':0,'other_group_changes':0,'S0_policy_or_blocked_server_work':False}}
    write('S4D-mana-point-contract.json',report)
    print(json.dumps({'report':record(ROOT/'S4D-mana-point-contract.json'),'new_raw_ranges':len(ranges),
      'new_instructions':len(expected_new),'inherited_assertions':len(expected_inherited),'actual_frames':len(actual),'new_static_checks':len(checks)},ensure_ascii=False))

if __name__=='__main__':main()

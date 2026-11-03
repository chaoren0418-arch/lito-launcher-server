"""Batch30 direct source xrefs only; unknown callees are never opened."""
import sys
sys.dont_write_bytecode=True
from audit import *
OUT=ROOT/'CD2-ready-guard-source'
SERVER=ROOT.parent/'lito伺服器'
def load(p):return json.loads(Path(p).read_text('utf-8'))
def main():
    prev=load(ROOT/'CD2-ready-outbound-contract.json');prevpin=record(ROOT/'CD2-ready-outbound-contract.json')
    assert prevpin['sha256']=='F3867EC1B86DA3DF66A7493E92CD239E40AEC59B658EC0B588F463EFF13A2667'
    sources=[record(p)for p in (PE,BIN,ASM)];assert sources==prev['sources']
    prior=prev['prior_primary_pins']+[{**prevpin,'expected_sha256':prevpin['sha256'],'match':True}];assert len(prior)==31
    for q in prior:
        a=record(q['path']);assert a['bytes']==q['bytes']and a['sha256']==q['expected_sha256']
    OUT.mkdir(exist_ok=True);pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);ins={};ranges=[];reuse=[]
    def decode(raw,va):
        rows=[{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in cs.disasm(raw,va)]
        assert sum(len(bytes.fromhex(i['raw_hex']))for i in rows)==len(raw)
        ins.update({int(i['va'],16):i for i in rows});return rows
    targets=[(0x480C8D,20,'switch_token_pointer_to_local_first_indirect_unknown'),(0x481410,67,'inline_lteventsvr_compare_and_BYTE_set1'),
      (0x4C3760,5,'direct_init_thunk_caller_invocation_unqualified'),(0x4C3770,10,'selector_init_object_arg_first_private_STOP4C3CC0'),
      (0x4C3780,10,'callback_registration_first_private_STOP4F10E0'),(0x4C3790,10,'selector_cleanup_object_arg_first_private_STOP4C3D20'),
      (0x64AF9D,1,'fixed_snapshot_BYTE_zero'),(0x8316B0,12,'fixed_snapshot_threeDWORD_zero_not_live_decode'),(0x62D25C,11,'literal_lteventsvr_NUL')]
    for va,n,label in targets:
        raw=pe.get_data(va-0x400000,n);path=OUT/f'{va:08X}-{n:X}.bin';path.write_bytes(raw)
        ranges.append({**mapping(pe,va),'bytes':n,'kind':'code'if va<0x500000 else'data','label':label,'raw_hex':raw.hex(),'output':record(path),'instructions':decode(raw,va)if va<0x500000 else[]})
    assert pe.get_data(0x64AF9D-0x400000,1)==b'\0'and pe.get_data(0x8316B0-0x400000,12)==b'\0'*12
    assert pe.get_data(0x62D25C-0x400000,11)==b'lteventsvr\0'
    reused=[('lifecycle/ranges.json',0x480C03,0x480C03,15,'tokenizer_prefix_STOP47EC50'),
      ('lifecycle/ranges.json',0x480C03,0x480C4D,12,'conditional_slash_hyphen_token_gate'),
      ('receive-boundary/ranges.json',0x4928C0,0x4928C0,45,'selected_callback_prefix_STOP4C8790'),
      ('receive-boundary/ranges.json',0x4928C0,0x492914,5,'selected_row_query_STOP494120'),
      ('receive-boundary/ranges.json',0x4928C0,0x492919,68,'conditional_selected_record_address_STOP49A510'),
      ('receive-boundary/ranges.json',0x4928C0,0x492E6B,12,'conditional_setter0_arg'),
      ('receive-boundary/ranges.json',0x4928C0,0x492E85,30,'conditional_recordBYTE3E_setter1_arg'),
      ('CD2-ready-outbound/ranges.json',0x46B6F0,0x46B6F0,59,'accepted_ready_state1_read_and_first_unknown'),
      ('S0C-status-owner/ranges.json',0x4C3E90,0x4C3E90,91,'accepted_protected_setter_no_new_body_expansion')]
    for index,base,va,n,label in reused:
        j=load(ROOT/index);q=next(x for x in j.get('ranges',j.get('new_ranges'))if int(x['va'],16)==base)
        assert record(q['output']['path'])==q['output'];full=Path(q['output']['path']).read_bytes();offset=va-base;raw=full[offset:offset+n]
        assert raw==pe.get_data(va-0x400000,n)and len(raw)==n
        reuse.append({**mapping(pe,va),'bytes':n,'kind':'code','label':label,'source_pin':q['output'],'source_index':record(ROOT/index),'source_offset':offset,'slice_sha256':sha(raw),'raw_hex':raw.hex(),'instructions':decode(raw,va),'old_PASS_replayed':False})
    expected=[(0x480C0D,'call','0x47ec50'),(0x480C51,'cmp','al, 0x2f'),(0x480C53,'je','0x480c8d'),(0x480C55,'cmp','al, 0x2d'),(0x480C57,'je','0x480c8d'),
      (0x480C8D,'lea','esi, [edi + 1]'),(0x480C90,'mov','edi, dword ptr [0x4f72e4]'),(0x480C9C,'mov','dword ptr [ebp - 0x1c], esi'),(0x480C9F,'call','edi'),
      (0x481410,'mov','ecx, dword ptr [ebp - 0x1c]'),(0x481413,'mov','esi, 0x62d25c'),(0x481420,'jne','0x48143e'),(0x481424,'je','0x48143a'),
      (0x48142E,'jne','0x48143e'),(0x481438,'jne','0x48141a'),(0x48143A,'xor','eax, eax'),(0x48143E,'sbb','eax, eax'),(0x481440,'sbb','eax, -1'),
      (0x481445,'jne','0x481453'),(0x481447,'mov','byte ptr [0x64af9d], 1'),(0x48144E,'jmp','0x481610'),
      (0x4C3760,'call','0x4c3770'),(0x4C3770,'mov','ecx, 0x8316b0'),(0x4C3775,'jmp','0x4c3cc0'),
      (0x4C3780,'push','0x4c3790'),(0x4C3785,'call','0x4f10e0'),(0x4C3790,'mov','ecx, 0x8316b0'),(0x4C3795,'jmp','0x4c3d20'),
      (0x4928E8,'call','0x4c8790'),(0x492914,'call','0x494120'),(0x492922,'lea','eax, [eax + eax*4]'),(0x492925,'lea','edx, [eax + eax*8]'),
      (0x492931,'lea','ebx, [edx*4 + 0x62faa0]'),(0x492958,'call','0x49a510'),(0x492E6B,'push','0'),(0x492E6D,'mov','ecx, 0x8316b0'),(0x492E72,'call','0x4c3e90'),
      (0x492E85,'mov','al, byte ptr [ebx + 0x3e]'),(0x492E8A,'je','0x492eb0'),(0x492E8C,'cmp','al, 7'),(0x492E8E,'push','1'),(0x492E93,'mov','ecx, 0x8316b0'),(0x492E9E,'call','0x4c3e90'),
      (0x46B6F0,'mov','al, byte ptr [0x64af9d]'),(0x46B6F7,'je','0x46b74a'),(0x46B6F9,'mov','eax, dword ptr [0x8316b0]'),
      (0x46B6FE,'mov','ecx, dword ptr [0x8316b4]'),(0x46B704,'xor','eax, 0xb104fa87'),(0x46B709,'mov','edx, dword ptr [ecx + eax*4]'),
      (0x46B70C,'mov','eax, dword ptr [0x8316b8]'),(0x46B711,'xor','edx, eax'),(0x46B713,'je','0x46b74a'),(0x46B726,'call','0x458bb0'),
      (0x4C3E97,'call','0x4c3ca0'),(0x4C3EA1,'call','0x4c3ca0'),(0x4C3EB6,'call','0x773dc650'),
      (0x4C3ECA,'mov','edx, dword ptr [ebp + 8]'),(0x4C3ECD,'xor','edx, ecx'),(0x4C3ED7,'mov','dword ptr [ecx + eax*4], edx'),(0x4C3EE1,'mov','dword ptr [edi], eax')]
    assertions=[]
    for va,mn,op in expected:
        q=ins[va];assert(q['mnemonic'],q['operands'])==(mn,op),(hex(va),q)
        assertions.append({**mapping(pe,va),**q,'expected_mnemonic':mn,'expected_operands':op,'match':True})
    # Navigation-only direct literal/symbol inventory. No reader bodies followed.
    lines=ASM.read_text('utf-8',errors='replace').splitlines();nav={};counts={}
    for name in ['byte_64AF9D','dword_8316B0','dword_8316B4','dword_8316B8']:
        pat=re.compile(r'\b'+name+r'\b',re.I);rows=[]
        for idx,line in enumerate(lines):
            if not pat.search(line)or line.strip().startswith(';'):continue
            text=line.strip()
            kind='definition'if text.lower().startswith(name.lower())else'address_argument'if 'offset '+name in text else'direct_write'if re.match(r'mov\s+'+name+r'\s*,',text,re.I)else'direct_read'
            rows.append({'line':idx+1,'text':line,'kind':kind})
        nav[name]=rows;counts[name]={kind:sum(x['kind']==kind for x in rows)for kind in ['direct_read','direct_write','address_argument','definition']}
    assert counts['byte_64AF9D']=={'direct_read':11,'direct_write':1,'address_argument':0,'definition':1}
    assert counts['dword_8316B0']=={'direct_read':45,'direct_write':0,'address_argument':4,'definition':1}
    assert counts['dword_8316B4']==counts['dword_8316B8']=={'direct_read':47,'direct_write':0,'address_argument':0,'definition':1}
    refs={n:record(ROOT/n)for n in ['CD2-ready-outbound-contract.json','CDD-outbound-contract.json','SCE-navigation-contract.json','D1-E1-prestart-contract.json','D5-settings-receive-contract.json','D5-mode-map-receive-contract.json','D5-scalar-receive-contract.json','SD0-receive-contract.json','S4D-mana-point-contract.json','host-alias-lifecycle.json','CD1-mode-step/scope-write-correction.json']}
    netpath=SERVER/'server-ordinary-room-settings-candidate-network.json';netpin=record(netpath);assert netpin['sha256']=='8C1776AA503098E09B42EFE8CA45E07E651DD4570A07CB6A8994006421CCEF41'
    net=load(netpath);refs.update(settings_485F_network_report=netpin,settings_485F_JAR=net['actual_JAR'],settings_485F_unit_report=net['accepted_settings_unit_repair'],settings_485F_product_classes=net['product_classes'])
    for q in refs.values():assert record(q['path'])==q
    setter=load(ROOT/'S4D-mana-point-contract.json')['state_and_call_boundaries']['protected_setter']
    summary={
      'BYTE64AF9D':{'location':mapping(pe,0x64AF9D),'width_bytes':1,'snapshot_hex':'00','snapshot_level':'raw fixedPE snapshot only, not proof executed initialization/loader default/current state',
        'direct_write':{'at':mapping(pe,0x481447),'value':1,'condition':'inline case-sensitive bytewise NUL equality with literal62D25C lteventsvr; equal EAX0 at481443, else branch481453','phase':'startup ClientApplication_Run conditional switch-token island; no numeric64AF98/room/owner gate here',
          'source':'EBP-1C pointer; earlier480C9C stores EDI+1 after slash/hyphen leading-char branch. tokenizer480C0D→47EC50 unknown STOP;480C9F indirectEDI loaded[4F72E4] firstunknown STOP. Laterinlinecompare independent, no success bridge asserted.'},
        'direct_clear_found':False,'direct_inventory':counts['byte_64AF9D'],'lifetime':'No named direct clear in finiteASMliteral inventory. Does not exclude alias/bulk writes or reinitialization; do not reset per-room/ready automatically.',
        'qualified_meaning':'local startup literal-match flag / local guard input only; actual tokenizer/input/entry execution unknown. Not owner/mode/ready authority, serverevent status or wireversion.'},
      'protected_selector':{'object':mapping(pe,0x8316B0),'encoded_index':mapping(pe,0x8316B0),'storage_pointer':mapping(pe,0x8316B4),'xor_key':mapping(pe,0x8316B8),'widths_bytes':[4,4,4],
        'reader_formula':'index=DWORD[8316B0] XOR B104FA87; result=DWORD[DWORD[8316B4]+index*4] XOR DWORD[8316B8] (32bits); ready guard only tests result==0',
        'snapshot_hex':'000000000000000000000000','fixed_snapshot_decodable':False,'snapshot_warning':'Pointer0 and encoded index0 would produce XORindexB104FA87, not valid initialized storage. Do not dereference/synthesize a live value from snapshot.',
        'ASM_definition_only':{'encoded':'B104FA84','pointer':'057E0D48','key':'2FE3681D','fixed_PE_equal':False,'authority':'different recovered initialization inASM, no currentPE runtime adoption'},
        'initialization':{'caller':mapping(pe,0x4C3760),'object_arg':mapping(pe,0x4C3770),'first_unknown':{'at':mapping(pe,0x4C3775),'target':'004C3CC0'},'helper_body_opened':False,'runtime_invocation_allocation_storage_lifetime':'unknown'},
        'cleanup':{'callback_arg':mapping(pe,0x4C3780),'registration_first_unknown':{'at':mapping(pe,0x4C3785),'target':'004F10E0'},'object_arg':mapping(pe,0x4C3790),'first_unknown':{'at':mapping(pe,0x4C3795),'target':'004C3D20'},'helper_body_opened':False,'registered_executed_or_cleared':'not qualified'},
        'overwrite_calls':[{'at':mapping(pe,0x492E72),'argument_u32':0,'object_arg':'008316B0','condition':'independent join island492E6B within existing character selection callback; prior unknown dependencies retained'},
          {'at':mapping(pe,0x492E9E),'argument_u32':1,'object_arg':'008316B0','condition':'selected record BYTE[EBX+3E]!=0; compareBYTE7 only writes separate64AFBF viaSETZ, does not gate setter1'}],
        'selected_record_source':'Conditional EAX index after unknown494120; inline LEAs EAX*5 thenEDX*9 thenEBX=62FAA0+index*180. Existing characterselection/source, not currentroom orserverD1/D5 record. BYTE+3E field meaning/provenance unresolved; recordproducer not expanded.',
        'selection_first_unknown':'4928E8→4C8790 entry firstunknown; row query492914→494120 STOP; afterrow conditionalsource島492958→49A510 STOP. Laterconstantsetters independent islands, no fullUI/ownercreation bridge.',
        'setter_adoption':setter,'setter_conditional':'Existing4C3E90 code accepts full32bitarg and stores keyedslot + encodedindex only if validobject/storage/keyhelpers/externalindex return. Firstunknown4C3E97→4C3CA0 (repeat4C3EA1);external4C3EB6→773DC650 remainsSTOP; no new bodies/PASS or execution.',
        'direct_inventory':{k:v for k,v in counts.items()if k!='byte_64AF9D'},'direct_global_store_found':False,'alias_or_other_object_setter_callers_complete':False,
        'qualified_meaning':'local protected selector and character-record-conditioned setter request0/1 only; not owner identity, room mode, version, ready membership or server admission.'},
      'ready_writer_use':{'existing_state1_guard':'64AF9D==0 skips selector dereference and reachesD201 island; nonzero requires validselector memory. decoded0→sameD201; decodednonzero→458BB0 firstunknown then independent virtualcallbackregistration; no executed acceptance.',
        'phase':'No64AF98/version/room/owner numericgate in this59byteguard; caller eligibility unknown, not wholeclientabsence.',
        'Modern':'Do not manufacture XOR object/flag or map either toowner/ready authorization. Explicit authorized localready intent and currentepoch/room/join/alias state remain independent of servercommit/D1/E1 receipts and GUI acceptance.',
        'existing_CE_D1_D5':'CE/SD0/CDD already read same selector for notification/layout; these are consumption proofs, not producer initialization. D1/E1 selfphase and D5 stores remain their exact prior contracts; none grants a semantic name tothese inputs. No transitivealias absence claimed.',
        'no_new_wire':'Known state1D201 body2/frame4 retained from Batch29; Batch30 adds directsource evidence only.'}}
    versions={'Batch29_frozen':{'primary':prevpin,'at_that_seal':'485Funit-only networknotyetaccepted; keeporiginalSHA andtext unchanged'},
      'Batch30_485F_accepted_supplement':{'report':netpin,'JAR':net['actual_JAR'],'level':'supervisor accepted single ordinary authenticated WAITING extended→compact→nonowner settings network','settings_result':net['settings_result'],'findings':net['findings'][:4],
        'limited_order':'each owneredit afterGuestready:perpeerGuestD1state0→D5summary0/config1;compact preservesoptions[2,1,7,9]/selections[3,6,8]/mode1/map514;nonowner unchangedstate andselfcurrent0/1',
        'not_qualified':['485F ready state-machine dynamic','COUNTDOWN/RUNNING failures','WORLD','Native GUI/numericphase','general race/atomic sends/ACK','othermode/codecs'],
        'new_frame_adoption':0,'network_or_cipher_verification_replayed':False},
      'prior_versions':'AD368 finite ready/cancel testclient, C7 WAITINGmapnetwork, 485Funitorderfix remain separately scoped. Newsettingsnetwork cannot transfer to Nativeguard value orreadydynamic.'}
    unknowns=['Tokenizer47EC50 andindirect[4F72E4] response/input/entry execution unknown; no startup/runtime command run.',
      'Protected object initialization4C3CC0,cleanup4C3D20 andregistration4F10E0 firstunknown STOP; noallocation/helperbody/virtual/external expansion.',
      'Selectedrecord+3E BYTEmeaning/origin unresolved;row query494120/selectionentry4C8790 and49A510 firstunknown retained; no owner/player/ready semantics fabricated.',
      'FixedPEzero snapshots differASMconstants; no validpointer/key/index/runtime value grant. No alias/bulk/directxref global completeness.',
      'CurrentNativeopcodeonly/UI/transport/owner/mode authority andD1/E1/WORLDgap fromBatch29 retained; no JavaSQL/Native/GUI/oldPASS.',
      'Externalpyc cleanup priorpolicyrejection remainsopen; no retry/reframe/alternate mechanism/delegation. Python-B,dont_write_bytecode used.']
    write('CD2-ready-guard-source/ranges.json',{'source':sources[0],'new_ranges':ranges,'necessary_reused_raw':reuse,'instruction_assertions':assertions})
    write('CD2-ready-guard-source/direct-source-xrefs.json',{'source_ASM':sources[2],'navigation_only':True,'symbol_rows':nav,'counts':counts,'scope':'Finite exactnamed directoperand inventory, not alias/bulk/runtime/callee closure; otherreaderbodies (includingWORLD) not expanded.'})
    write('CD2-ready-guard-source/source-width-trigger-and-unknown-matrix.json',{'source_PE':sources[0],'sources':summary,'unknowns':unknowns,'new_wire_differences':0})
    write('CD2-ready-guard-source/version-evidence-supplement.json',versions)
    packet=record(ROOT/'server-packet-delta.json');assert packet==prev['verification']['server_packet_unchanged']
    primary={'schema':'finite-native-CD2-state1-guard-direct-source-v1','updated_at':stamp(),'status':'DIRECT_LITERAL_SOURCES_SEALED_RUNTIME_SELECTOR_UI_AUTHORITY_OPEN','sources':sources,'prior_primary_pins':prior,'existing_evidence':refs,
      'outputs':[record(q)for q in sorted(OUT.iterdir())if q.is_file()],'source_contract':summary,'unknowns':unknowns,
      'preflight':'ExistingCE/SD0/CDDguard reads andS0C/S4Dprotectedsetter were already qualified; no oldbody/model/PASS regenerated. Added only directflagwrite,initialization/cleanup arguments, rawsnapshot differences andnecessaryselectedcaller slices.',
      'verification':{'new_code_ranges':6,'new_data_ranges':3,'necessary_reused_raw_ranges':9,'instruction_assertions':len(assertions),'direct_xref_code_rows':155,'direct_xref_definition_rows':4,'prior_primary_pins_preserved':31,'new_capture_frames':0,'new_frame_rows_adopted':0,'new_wire_differences':0,'new_opcodes':0,'wire_corrections':0,'unknown_bodies_expanded':0,'Native_GUI_input_injection_SQL_provider_JVM_Java_Modern_BIN_actions':0,'server_packet_unchanged':packet,'scope_write_correction':refs['CD1-mode-step/scope-write-correction.json'],'cache_cleanup_completed':False,'485F_limited_settings_network_adopted':True,'485F_ready_COUNTDOWN_WORLD_GUI_qualification':False}}
    write('CD2-ready-guard-source-contract.json',primary);print(json.dumps({'primary':record(ROOT/'CD2-ready-guard-source-contract.json'),'verification':primary['verification']},ensure_ascii=False))
if __name__=='__main__':main()

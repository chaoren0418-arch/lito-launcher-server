"""Batch24: existing raw only, finite D5 mode/map effects; no dynamic witness."""
from audit import *
OUT=ROOT/'D5-mode-map-receive'
FIXED='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
PREVIOUS='8A6C74A34CFAC1BC1AFF04215F80E347853069ACFA611C076269443B4671DFD8'
SERVER='3D0B5BAA46238ED5589C193FBDE22B966B945EFDEAB70D2A2C3933A62CA8E15F'

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    OUT.mkdir(exist_ok=True)
    sources=[record(p)for p in (PE,BIN,ASM)]
    assert [p['sha256']for p in sources]==[FIXED,'CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292','2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A']
    previous=load('D5-settings-receive-contract.json');prevpin=record(ROOT/'D5-settings-receive-contract.json')
    assert prevpin['sha256']==PREVIOUS
    prior=previous['prior_primary_pins']+[{**prevpin,'expected_sha256':PREVIOUS,'match':True}]
    assert len(prior)==25
    for p in prior:
        q=record(Path(p['path']));assert q['bytes']==p['bytes'] and q['sha256']==p['expected_sha256']
    b18=load('D5-CF-receive/ranges.json');b23=load('D5-settings-receive/ranges.json')
    b18pin=record(ROOT/'D5-CF-receive/ranges.json');b23pin=record(ROOT/'D5-settings-receive/ranges.json')
    assert b18pin['sha256']=='75D5F21B7CADA8A8AD5ECA6660B6C97A11821A940FF2E33DA80F0C8D22AE40B4'
    assert b23pin in previous['outputs']
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    slices=[];ins={};owners={}
    def use(va,n,label,kind='code'):
        # Choose an existing encompassing range, with exact offset/size. Never
        # write a new .bin or pull bytes beyond an already sealed source pin.
        found=None
        for idx,group,collection,key in [(b18pin,'Batch18 new raw',b18['ranges'],'output'),
                                         (b18pin,'Batch18 inherited raw',b18['inherited_raw'],'range'),
                                         (b23pin,'Batch23 gap raw',b23['new_ranges'],'output')]:
            for r in collection:
                base=int(r['va'],16);pin=r[key]
                if base<=va and va+n<=base+pin['bytes']:
                    found=(idx,group,base,pin);break
            if found:break
        assert found,(hex(va),n)
        idx,group,base,pin=found
        raw=Path(pin['path']).read_bytes()[va-base:va-base+n]
        assert record(Path(pin['path']))==pin and raw==pe.get_data(va-0x400000,n)
        instructions=[]
        if kind=='code':
            for i in cs.disasm(raw,va):
                row={'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}
                instructions.append(row);ins[i.address]=row;owners[i.address]=pin
            assert sum(len(bytes.fromhex(x['raw_hex']))for x in instructions)==n
        slices.append({**mapping(pe,va),'used_bytes':n,'source_byte_offset':va-base,
                       'source_base_VA':f'{base:08X}','source_pin':pin,'source_index':idx,
                       'inherited_group':group,'kind':kind,'label':label,'raw_hex':raw.hex(),
                       'instructions':instructions,'old_PASS_replayed':False})
    specs=[
        (0x4A9E50,91,'inherited_header_phase_room_subtype_gate','code'),
        (0x4AA074,40,'subtype_jump_table','data'),
        (0x4A9F6A,10,'subtype2_mode_destination_and_shared_tail_jump','code'),
        (0x4A9F74,21,'subtype3_localmap_WORD_reader','code'),
        (0x4AA05B,22,'subtype2_shared_BYTE_reader_and_return_only','code'),
        (0x4A9F89,28,'Batch23_shared_mapprojection_and_return','code'),
        (0x5025A8,2,'c_literal','data'),(0x632594,2,'h_literal','data'),(0x5028F8,3,'cd_literal','data'),
        (0x4A2A50,58,'typedreader_common_dispatch_prefix','code'),
        (0x4A2A8A,16,'c_directBYTE_store','code'),(0x4A2A9A,31,'h_directWORD_LE_store','code'),
        (0x4A2B56,34,'typedreader_format_loop_cursor_and_returns','code'),
        (0x4A2B78,24,'typedreader_dispatch_targets','data'),(0x4A2B90,33,'typedreader_char_map','data'),
    ]
    for va,n,label,kind in specs:use(va,n,label,kind)
    def w(va):return {'fixed_PE_sha256':FIXED,**mapping(pe,va),**ins[va],'range':owners[va]}
    expected=[
      (0x4A9E6A,'call','0x4a2a50'),(0x4A9E77,'cmp','eax, 6'),(0x4A9E7F,'mov','dword ptr [0x64abf4], eax'),
      (0x4A9E8F,'cmp','ecx, eax'),(0x4A9E91,'jne','0x4aa06c'),(0x4A9E97,'movsx','eax, byte ptr [ebp - 1]'),
      (0x4A9E9E,'ja','0x4aa06c'),(0x4A9EA4,'jmp','dword ptr [eax*4 + 0x4aa074]'),
      (0x4A9F6A,'push','0x64acca'),(0x4A9F6F,'jmp','0x4aa05b'),
      (0x4AA05B,'add','esi, 6'),(0x4AA05E,'push','0x5025a8'),(0x4AA063,'push','esi'),
      (0x4AA064,'call','0x4a2a50'),(0x4AA069,'add','esp, 0xc'),(0x4AA070,'ret',''),
      (0x4A9F74,'lea','eax, [ebp - 4]'),(0x4A9F77,'add','esi, 6'),(0x4A9F7A,'push','eax'),
      (0x4A9F7B,'push','0x632594'),(0x4A9F80,'push','esi'),(0x4A9F81,'call','0x4a2a50'),
      (0x4A9F86,'add','esp, 0xc'),(0x4A9F89,'mov','ax, word ptr [ebp - 4]'),
      (0x4A9F8D,'cmp','ax, 0xffff'),(0x4A9F91,'je','0x4a9f9b'),
      (0x4A9F93,'cmp','ax, 0xff9c'),(0x4A9F97,'je','0x4a9f9b'),
      (0x4A9F99,'add','al, 0xc'),(0x4A9F9B,'mov','byte ptr [0x64acc9], al'),(0x4A9FA4,'ret',''),
      (0x4A2A8D,'mov','al, byte ptr [ebx]'),(0x4A2A92,'inc','ebx'),(0x4A2A93,'mov','byte ptr [edx], al'),
      (0x4A2A9A,'movzx','cx, byte ptr [ebx + 1]'),(0x4A2A9F,'movzx','dx, byte ptr [ebx]'),
      (0x4A2AA9,'shl','ecx, 8'),(0x4A2AAC,'add','ecx, edx'),(0x4A2AAE,'add','ebx, 2'),
      (0x4A2AB1,'mov','word ptr [eax], cx'),(0x4A2B69,'mov','eax, ebx'),(0x4A2B70,'ret','')]
    assertions=[]
    for va,mn,op in expected:
        assert(ins[va]['mnemonic'],ins[va]['operands'])==(mn,op),(hex(va),ins[va])
        assertions.append({**w(va),'expected_mnemonic':mn,'expected_operands':op,'match':True})
    jump=struct.unpack('<10I',pe.get_data(0xAA074,40));assert jump[2:4]==(0x4A9F6A,0x4A9F74)
    assert pe.get_data(0x1025A8,2)==b'c\0' and pe.get_data(0x232594,2)==b'h\0'
    branches=[]
    for va in [0x4A9E7A,0x4A9E84,0x4A9E91,0x4A9E9E,0x4A9F6F,0x4A9F91,0x4A9F97]:
        target=int(ins[va]['operands'],16);assert target in ins
        branches.append({'va':f'{va:08X}','target':f'{target:08X}','target_is_instruction_boundary':True})
    server_path=ROOT.parent/'lito伺服器/server-ordinary-room-map-selection-contract.json'
    serverpin=record(server_path);assert serverpin['sha256']==SERVER
    server=json.loads(server_path.read_text('utf-8'));detailpin=server['contract'];assert record(Path(detailpin['path']))==detailpin
    detail=json.loads(Path(detailpin['path']).read_text('utf-8'))
    supplement=record(ROOT.parent/'lito伺服器/server-room-D5-CF-consumer-supplement.json')
    assert supplement['sha256']=='7774EA7546EEF33CB5240A58A36FA9FFF7A465303049C3C3E238081887279BEF'
    assert detail['D5sub3']['body_bytes']==8 and detail['D5sub3']['frame_bytes']==10
    assert server['verification']['S_D5_sub3_hits']==0
    gate={
      'qualified_by':prevpin,'dispatch_and_header':'Batch18/23 mode4/signedphase>=3 primary/fallback D5→4B2F67→4A9E50; cd header unchanged, subtype localEBP-1 / roomDWORD localEBP-8.',
      'phase6':'4A9E7F incomingroom→64ABF4 before subtype validation. No priorroom equality.',
      'other_phases':'4A9E8F incomingroom compared64ABF4; mismatch→4AA06Creturn; match maydispatch without another phasewhitelist.',
      'valid_subtype':'MOVSX i8 then unsignedJA>9; 0..9 valid. Both subtype2 and3 use this same gate independently.',
      'Native_version_or_owner_gate':'No explicitversion63D54C/owner/self/readycheck in these finite paths; upstream/runtime dispatch remains inherited andconditional.',
      'Modern_requirement':'Validate independent ownedepoch/currentCEroom before applying; Nativephase6 overwrite is not authority.',
    }
    preserved=[
      'room_name64AB70','owner_display64ABCA','attributes64ABE6','password64ABE8','capacity64ABF0',
      'summary0_64ACCC','summary1..4_64ACD0..D3','options64ACD4..D7','selections64ACD8..DF',
      'adjacentmode_highBYTE64ACCB','ready/member/source/physical/session identity',
      'catalogUI64A958 rows/row+74','selectedpreviewUI64A98C WORD+0C andBYTE+0F',
    ]
    effects={
      'subtype2':{
        'VA':'004A9F6A','RVA':'000A9F6A','format':'c',
        'path':['4A9F6A push64ACCA','4A9F6F jump4AA05B','4AA064 call4A2A50 c(body+6,64ACCA)','4AA070return'],
        'wire_mode_destination':'64ACCA BYTE only; typedreader4A2A93 BYTEstore. No sign conversion/domainmask/valueclamp here.',
        'retained':preserved+['map_projection64ACC9'],
        'UI':'No4A9030/UI/private/external/virtual call in subtype2 path. Sharedtail begins4AA05B, not earlier4AA03B/4AA04C UIedge used by other subtypes.',
        'map_reprojection':'None: changing mode does not reproject existing64ACC9 or load/default/validate a map.',
        'qualification':'No explicit state/ready/owner/session/source writes beyond inheritedroomgate andtyped modeBYTE destination; runtime state unknown.',
      },
      'subtype3':{
        'VA':'004A9F74','RVA':'000A9F74','format':'h',
        'path':['4A9F74 localEBP-4','4A9F81 call4A2A50 h(body+6,localWORD)','fallthrough4A9F89 sharedBatch23projection','4A9F9B store64ACC9 BYTE','4A9FA4return'],
        'wire_map_destination':'u16LE into localEBP-4 WORD via4A2AB1; no global rawWORD copy here.',
        'projection':'Compare fullWORD==FFFF orFF9C then keeplowBYTE; otherwise AL+0C mod256. SharedBatch23 subtype0 tail, no tablelookup/version/locale/mode branch.',
        'retained':preserved+['mode_lowBYTE64ACCA'],
        'UI':'No4A9030/UI/private/external/virtual call in subtype3 path. No catalog/preview write or map resource/provider activation qualified.',
        'qualification':'64ACC9 BYTE is lossy internal projection, not rawmapWORD/runtime recordindex/resourceID. ActualGUI/bounds/currentcontext unknown.',
      },
      'preserved_scope':'No explicit destination in these finite c/h receivepaths; no full-image absence/actualruntime outcome claim.',
      'reader':'Existingc/h/formatdispatch/return slices only. No strings/s/private/external/virtual body expansion. Nativeinputbounds/exactendvalidation not supplied by handler.',
      'last_stores':['4A9E7F conditionalroomDWORD','4A2A93 modeBYTE','4A2AB1 localmapWORD','4A9F9B projectedmapBYTE'],
    }
    separation={
      'mode':'D5sub2 writes64ACCA lowBYTE, adjacent64ACCB retained. Batch21 UIselector reads zeroextendedWORD64ACCA conditionally; actual highBYTE value notqualified. Do not normalize BYTEinto an assumedWORDzero.',
      'wire_map':'D5sub3 u16LE word in packet/local; Modern keepsrawWORD andpacketprovenance independently.',
      'D5_map_projection':'64ACC9 BYTE via+0C/specialfullWORDtests. NotCC row+74 orpreview+0F store.',
      'catalog':'Batch20 CC recordindex/legacyprojection endsrow+74; UI64A958 catalogs andwireCCmapWORD context separate. TheseD5branches do not append/reset/rewrite catalogrows.',
      'preview':'Batch21 selection4658B0→4687D0 row+74→UI64A98C+0F; preview+0C roomWORD separate. NeitherD5sub2/3 directly touches them or calls that path.',
      'table_resource':'64AFD0/64AFE0 runtimecount/order, LTmapstr numericfilekeys/physicalrows/labels/codecs andactualmap/WORLDresourcebinding remainunknown. No new use or readerlookup here.',
      'readiness_owner_source':'D5mode/map has no readyreset/owner/result/ACK fields or corresponding finitebranch stores; retain independently qualifiedD1/owner/session/source/physicalstate.',
      'independence':'Each D5mode/map packet gates/returns separately, no pair/transaction/rollbackflag. Modechanges can retainpreviousprojectedmap; mapchanges retainmode; sourcecoherence notguaranteed.',
      'guard_sources':{n:record(ROOT/n)for n in ('SCC-catalog-lifecycle-contract.json','map-table-source-contract.json','map-candidate-index-contract.json')},
    }
    server_comparison={
      'source_contract':serverpin,'detail_contract':detailpin,'D5_layout_authority':supplement,
      'S_D5_sub3_success':'Source/archivedIL only: acceptedmap update→selectedMapId/roomready/reconfigure→sessionready D1reset→D5sub3 requestedword roommembers. No universal atomic/allpeer delivery guarantee.',
      'S_D5_sub3_illegal_restore':'Source/archivedIL only: invalidpool/FFFF→selfD5sub3 BattleRules.decode(settings).mapId(firstpoolmap). Same layout, no outcome/flowrole field; nohubupdate orreadyreset on this path.',
      'valid_request_hub_false':'SelfD5sub0/1 snapshot instead ofsub3; Native receive does not know C_D1admission outcome.',
      'inherited_candidate_value_mismatch':detail['difference'],
      'mismatch_classification':'Existingserver conditionalrestore-value discrepancy, not a newly discovered wirelayout/opcode correction. No candidate/server/JAR changes requested or performed in this batch.',
      'Native_response':'Bothacceptedbroadcast andillegalrestore bytes enter sameNativegate/sub3projection. No requestcorrelation/success/reject/rollback test; preserve unresolvedreceived-vs-expected conflict without automaticrewrite/retry/hubrollback inference.',
      'S_D5_sub2':'7774 layout source/actualIL only: d502+roomDWORD+modeBYTE; mapcontract excludesC_D1sub2mode-step union arm. No mode runtime/trigger/serveraction expansion granted here.',
      'dynamic':'No newcapture; serverfinite7selectedcaptures reportsC_D1sub3=0/S_D5sub3=0. Thisbatchdoesnotre-scanthosecaptures ormanufacturewitness; examples belowarecomputedbytesonly.',
      'unknowns':server['unknowns'],
    }
    # New finite effect examples, not replaying old seven-map/gate test matrices.
    # Use raw byte stores to demonstrate independent fields/highBYTE retention.
    cases=[]
    def mode_case(mode):
        before=bytes([0x40,0x01,0xA5,0xCC]);after=bytearray(before);after[1]=mode
        return {'input_mode_BYTE':mode,'before64ACC9..CC_hex':before.hex(),'after64ACC9..CC_hex':bytes(after).hex(),
                'mapBYTE_preserved':after[0]==before[0],'highmodeBYTE_preserved':after[2]==before[2],
                'summary0_preserved':after[3]==before[3],'Native_executed':False}
    for x in (2,128,255):cases.append(mode_case(x))
    map_cases=[]
    for word in (514,564,0xFF9C):
        before=bytes([0x40,0x02,0xA5,0xCC]);after=bytearray(before)
        after[0]=(word&255)if word in (0xFFFF,0xFF9C)else((word&255)+12)&255
        map_cases.append({'input_map_WORD':word,'wordLE_hex':word.to_bytes(2,'little').hex(),
                          'before64ACC9..CC_hex':before.hex(),'after64ACC9..CC_hex':bytes(after).hex(),
                          'modeBYTE_and_highBYTE_preserved':after[1:3]==before[1:3],
                          'summary0_preserved':after[3]==before[3],'Native_executed':False})
    computed={
      'qualification':'Computed layout/operand examples only, not captured/network/Native/runtime/hubrollback witnesses.',
      'mode_effect_cases':cases,'map_effect_cases':map_cases,
      'subtype2_computed_room1_mode2':{'body_hex':'d5020100000002','body_bytes':7,'frame_bytes':9,'actual_trace':False},
      'subtype3_current514_computed':{'body_hex':'d503010000000202','body_bytes':8,'frame_bytes':10,'mapWORD':514,'projectedBYTE':14,'actual_trace':False},
      'subtype3_restore564_computed':{'body_hex':'d503010000003402','body_bytes':8,'frame_bytes':10,'mapWORD':564,'projectedBYTE':64,'actual_trace':False,
                                     'source_of_value':'Serverexistingconditionalcounterexample only; does notproveNative/serverexecution orhubrollback.'},
    }
    wire=[{
      'classification':'KNOWN_LAYOUT_CORROBORATION_NEW_FINITE_NATIVE_STORE_EFFECT_NO_WIRE_DIFFERENCE',
      'direction':'server-to-client','opcode_hex':'D5','opcode_decimal':213,'subtype':subtype,
      'VA':f'{va:08X}','RVA':f'{va-0x400000:08X}','fixed_PE_sha256':FIXED,
      'fields':[{'body_offset':1,'type':'u8','bytes':1,'semantic':'subtype','destination':'localEBP-1'},
                {'body_offset':2,'type':'u32LE','bytes':4,'semantic':'roomcontext','destination':'localEBP-8 /phase6conditional64ABF4'},
                {'body_offset':6,'type':typ,'bytes':n,'semantic':semantic,'destination':dest}],
      'body_bytes':6+n,'frame_bytes':8+n,'endianness':'little-endian u16/u32; u8singlebyte','encoding':'no strings/no text codec',
      'trigger_state':'Knownservermodehelperlayout/sub3acceptedbroadcastorillegalrestore sources; inheritedNativephase6/otherroom-equalitygate. No success/ACK/transactionqualification.',
      'evidence_level':'FixedPE reusedraw static completefinitebranches; serverexisting source/archivedIL; zero dynamic subtype3 witness',
      'unknown':['actualNativephase/room/GUI/globalvalidity','Nativebounds/exactend','runtime table/resourcebinding','atomicity/allpeer/sendfailure','modeproducertrigger','owner/ready/source/physical/sessionauthority'],
    }for subtype,va,typ,n,semantic,dest in [(2,0x4A9F6A,'u8',1,'mode','64ACCA BYTE'),
                                          (3,0x4A9F74,'u16LE',2,'rawmapWORD','localEBP-4 WORD→64ACC9 BYTE projection')]]
    check={'new_code_ranges':0,'new_data_ranges':0,'inherited_necessary_raw_slices':len(slices),
           'distinct_reused_raw_pins':len({s['source_pin']['path']for s in slices}),
           'instruction_assertions':len(assertions),'aligned_in_slice_branch_targets':len(branches),
           'mode_complete_path_bytes':32,'map_complete_path_bytes':49,'mode_effect_cases':3,'map_effect_cases':3,
           'new_capture_frames':0,'dynamic_subtype3_witnesses':0,'saved_frames_replayed':False,
           'prior_primary_pins_preserved':25,'all_match':True,'server_packet_unchanged':record(ROOT/'server-packet-delta.json'),
           'Batch18_20_21_23_or_old_PASS_replayed':False}
    outputs={
      'ranges.json':{'fixed_source':sources[0],'new_ranges':[],'inherited_necessary_raw_slices':slices},
      'instruction-witnesses.json':{'assertions':assertions,'aligned_branches':branches},
      'gate-and-branch-effects.json':{'gate':gate,'effects':effects},
      'field-separation-boundary.json':separation,'server-source-comparison.json':server_comparison,
      'computed-effect-examples.json':computed,'wire-contracts.json':{'known_contracts':wire,'new_wire':[],'wire_corrections':[]},
      'finite-verification.json':check,
    }
    for n,o in outputs.items():write('D5-mode-map-receive/'+n,o)
    report={
      'schema':'lito.native-D5-subtype2-3-mode-map-effects.v1','updated_at':stamp(),
      'status':'FINITE_D5_MODE_MAP_RECEIVE_SEALED_RUNTIME_UI_RESOURCE_ACK_ROLLBACK_OPEN',
      'sources':sources,'prior_primary_pins':prior,'prior_D5_settings_contract':prevpin,
      'existing_evidence':{'Batch18_ranges':b18pin,'Batch23_ranges':b23pin,'server_map_contract':serverpin,
                           'server_map_detail':detailpin,'existing_D5_layout_contract':supplement},
      'outputs':[record(p)for p in sorted(OUT.iterdir())if p.is_file()and p.name not in ('READ-ME.txt','independent-review.json')],
      'wire':wire,'gate':gate,'effects':effects,'field_separation':separation,'server_comparison':server_comparison,
      'verification':check,
      'delta':{'new_wire':[],'new_opcodes':[],'wire_corrections':[],
               'known_corroboration':['D5sub2 modeBYTE andD5sub3 mapWORD layout','sameS_D5sub3layout foracceptedbroadcast/illegalrestore'],
               'new_static_qualification':['subtype2sharedscalarreturn noUI/mapreprojection','subtype3sharedprojection noUI/catalog/preview store','adjacentBYTE/ready/owner/sourcefields retained in finitepaths'],
               'inherited_server_candidate_correction_only':serverpin,'packet_modified':False},
      'scope':{'writes':'own resource directory only','finite_batch_complete':True,
               'Native_GUI_provider_SQL_process_or_new_capture':False,'Java_Modern_BIN_modified':False,
               'private_external_virtual_WORLD_body_expansion':False,'savedcaptures_rescanned_or_old_PASS_replayed':False,
               'runtime_UI_table_resource_ACK_or_automatic_rollback_qualified':False,
               'stop':'Allneededrawalreadysealed; citeonlyfiniteeffects complete. Runtime/bounds/UI/resources/ACK/rollback/sourceauthority unknown. No server intercepted work resumed.'}}
    write('D5-mode-map-receive-contract.json',report)
    print(json.dumps({'primary':record(ROOT/'D5-mode-map-receive-contract.json'),'verification':check},ensure_ascii=False))

if __name__=='__main__':main()

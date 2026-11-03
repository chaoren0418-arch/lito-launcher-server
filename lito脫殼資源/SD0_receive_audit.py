"""Batch16 finite Native S_D0 receive boundary. Static reads only."""
from audit import *

OUT=ROOT/'SD0-receive'
FIXED='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
PREVIOUS='1C2519F9F1B88C9E68FDF963FFAFF85366CEC755254613C1926A68E7ED9BC192'
SUPPLEMENT=ROOT.parent/'lito伺服器/server-room-D0-consumer-routing-supplement.json'

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    OUT.mkdir(exist_ok=True); pe=pefile.PE(str(PE)); cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    sources=[record(p)for p in [PE,BIN,ASM]]
    assert [p['sha256']for p in sources]==[FIXED,'CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292','2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A']
    prev=load('SCD-member-comparison-contract.json'); pin=record(ROOT/'SCD-member-comparison-contract.json'); assert pin['sha256']==PREVIOUS
    prior=prev['prior_primary_pins']+[{**pin,'expected_sha256':PREVIOUS,'match':True}]
    assert len(prior)==17
    for r in prior: assert record(Path(r['path']))['sha256']==r['expected_sha256']
    sup=record(SUPPLEMENT); assert sup['sha256']=='7BF90D85A297E8AAA9D90D3BD4AF11DFD13992D685FF7211CBD1A9433CA898E6'
    supplement=json.loads(SUPPLEMENT.read_text(encoding='utf-8')); route=Path(supplement['contract']['path']); assert record(route)==supplement['contract']
    route_obj=json.loads(route.read_text(encoding='utf-8'))
    existing={'server_routing_supplement':sup,'server_route_contract':record(route),'server_route_manifest':supplement['manifest'],
      'native_dispatch':record(LEGACY/'analysis/NATIVE-S-DISPATCH.json'),'Batch15_comparison_contract':pin}
    assert record(Path(existing['server_route_manifest']['path']))==existing['server_route_manifest']
    packet=record(ROOT/'server-packet-delta.json'); assert packet['sha256']=='B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C'
    ranges=[]; inherited=[]; ins={}; owners={}
    def decode(raw,va,q):
        rows=[]
        for i in cs.disasm(raw,va):
            r={'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}; rows.append(r); ins[i.address]=r; owners[i.address]=q
        assert sum(len(bytes.fromhex(r['raw_hex']))for r in rows)==len(raw),(hex(va),'alignment')
        return rows
    def reuse(index,va,n,kind='code'):
        d=load(index);r=next(r for r in d['ranges']if int(r['va'],16)==va); q=r['output'];raw=Path(q['path']).read_bytes()
        assert record(Path(q['path']))==q and raw==pe.get_data(va-0x400000,len(raw)) and n<=len(raw)
        inherited.append({'index':record(ROOT/index),'range':q,**mapping(pe,va),'used_bytes':n,'kind':kind,'raw_matches_fixed_PE':True,'old_tests_replayed':False})
        if kind=='code':decode(raw[:n],va,q)
    for index,va,n,kind in [
      ('priority-complete-tails.json',0x4A2A50,0x128,'code'),
      ('S0C-status-owner/ranges.json',0x4A2B78,24,'data'),('S0C-status-owner/ranges.json',0x4A2B90,33,'data'),
      ('SCE-navigation/ranges.json',0x4B29DB,56,'code'),('SCE-navigation/ranges.json',0x4B313F,30,'code'),
      ('skill-pane-frame-coordinate/ranges.json',0x469CA0,24,'code'),('SCE-navigation/ranges.json',0x469CB8,17,'code'),
      ('SCE-navigation/ranges.json',0x46A244,20,'data'),('SCE-navigation/ranges.json',0x46A520,30,'code'),
      ('SCE-navigation/ranges.json',0x4B3B80,3,'code'),('SCD-member-comparison/ranges.json',0x450B90,37,'code'),
      ('skill-pane-layout/ranges.json',0x450230,48,'code'),('SCE-navigation/ranges.json',0x41B410,15,'code'),
      ('SCE-navigation/ranges.json',0x41AA90,5,'code'),('SCE-navigation/ranges.json',0x41AAA0,5,'code'),
      ('SCE-navigation/ranges.json',0x41B6C0,17,'code'),('SCE-navigation/ranges.json',0x41B6E0,53,'code'),
      ('SCE-navigation/ranges.json',0x4765F0,41,'code'),('SCE-navigation/ranges.json',0x458830,85,'code'),
      ('receive-boundary/ranges.json',0x4C6AE0,80,'code')]:reuse(index,va,n,kind)
    # Comparison implementation and initialization remain under unchanged Batch15 authority.
    specs=[(0x4A9090,0x4C0,'code','complete_S_D0_handler'),(0x4B2F2B,12,'code','shared_S_D0_dispatch'),
      (0x465370,0xBF,'code','UI_exact_record_name_remove'),(0x46A540,0x84,'code','selected_preview_reset_and_optional_C_CD'),
      (0x469CC9,0x1FA,'code','scene_mode1_direct_branch_only'),
      (0x4B3540,1,'data','D0_primary_map_slot'),(0x4B33E0,4,'data','D0_primary_jump'),
      (0x4B3784,1,'data','D0_fallback_map_slot'),(0x4B3638,4,'data','D0_fallback_jump'),
      (0x634370,3,'data','D0_ds_literal_both_versions'),(0x5028F8,3,'data','known_C_CD_cd_literal'),
      (0x4F7314,4,'data','dump_time_formatting_pointer_NOT_runtime')]
    for va,n,kind,label in specs:
        raw=pe.get_data(va-0x400000,n); assert len(raw)==n;p=OUT/f'{va:08X}-{n:X}.bin';p.write_bytes(raw);q=record(p)
        ranges.append({**mapping(pe,va),'bytes':n,'kind':kind,'label':label,'raw_hex':raw.hex(),'output':q,
          'instructions':decode(raw,va,q)if kind=='code'else[]})
    def w(at):return {'fixed_PE_sha256':FIXED,**mapping(pe,at),**ins[at],'range':owners[at]}
    expected=[
      (0x4B2F2D,'call','0x4a9090'),(0x4A9099,'cmp','dword ptr [0x63d54c], 0x78b9'),(0x4A90A3,'jl','0x4a90c4'),
      (0x4A90AB,'push','eax'),(0x4A90B0,'push','0x64'),(0x4A90B4,'push','0x634370'),(0x4A90BA,'call','0x4a2a50'),
      (0x4A90CB,'push','0x64'),(0x4A90CF,'push','0x634370'),(0x4A90D5,'call','0x4a2a50'),
      (0x4A90DD,'mov','eax, dword ptr [0x64af98]'),(0x4A90E6,'je','0x4a9472'),(0x4A90EC,'sub','eax, 5'),(0x4A90EF,'je','0x4a9472'),
      (0x4A90F5,'mov','ecx, dword ptr [0x64abf4]'),(0x4A9100,'jne','0x4a9549'),(0x4A910A,'push','0x64abf8'),
      (0x4A910F,'call','0x4d3a80'),(0x4A9116,'jne','0x4a9166'),(0x4A9118,'mov','ecx, 1'),(0x4A911D,'call','0x469ca0'),
      (0x4A9128,'call','0x450b90'),(0x4A9148,'mov','cx, word ptr [eax + 4]'),(0x4A914C,'call','0x46a540'),(0x4A915A,'call','0x46a540'),
      (0x4A91A8,'call','0x4d3a80'),(0x4A91AF,'je','0x4a91ba'),(0x4A91DA,'call','0x465370'),
      (0x4A9204,'mov','ecx, 0x23'),(0x4A9211,'rep movsd','dword ptr es:[edi], dword ptr [esi]'),(0x4A9228,'mov','dword ptr [0x64ab68], eax'),
      (0x4A926B,'call','0x4d3a80'),(0x4A929C,'call','0x465370'),(0x4A92D3,'rep movsd','dword ptr es:[edi], dword ptr [esi]'),
      (0x4A92E6,'mov','dword ptr [0x64ab58], eax'),(0x4A92F2,'je','0x4a9549'),(0x4A92FD,'cmp','eax, 4'),(0x4A9302,'cmp','eax, 5'),
      (0x4A938C,'cmp','dword ptr [0x63d54c], 0x78b9'),(0x4A939C,'lea','edx, [ebp - 0x14]'),
      (0x4A93A5,'call','0x4b4b00'),(0x4A93B5,'call','0x4b4a10'),(0x4A945F,'call','0x46f380'),
      (0x4A9475,'test','eax, eax'),(0x4A9477,'jne','0x4a9549'),(0x4A94BB,'call','0x4d3a80'),(0x4A94EC,'call','0x465370'),
      (0x4A953A,'call','0x4b3b80'),(0x4A953F,'sub','dword ptr [0x64acf8], 0x8c'),
      (0x465399,'cmp','bl, byte ptr [esi]'),(0x4653A6,'cmp','bl, byte ptr [esi + 1]'),(0x4653CA,'call','dword ptr [edx]'),
      (0x4653EA,'call','0x77409100'),(0x4653FB,'mov','dword ptr [edi + 0x2c], edx'),(0x465414,'jmp','0x46541d'),
      (0x469CB1,'mov','dword ptr [0x64af98], eax'),(0x469CC2,'jmp','dword ptr [eax*4 + 0x46a244]'),
      (0x469CDA,'mov','dword ptr [0x64af98], 1'),(0x469E24,'mov','dword ptr [0x62c9b8], 0x46d640'),
      (0x469E2E,'mov','dword ptr [0x62c9bc], 0x46d8a0'),(0x469E38,'mov','dword ptr [0x62c9c0], 0x46d630'),
      (0x469E9D,'call','0x46a520'),(0x469EAA,'call','dword ptr [eax + 0x4c]'),(0x469EAF,'call','0x49bb40'),
      (0x46A551,'call','0x450230'),(0x46A563,'cmp','eax, ebx'),(0x46A595,'mov','dword ptr [0x64acf8], edx'),
      (0x46A59B,'call','0x4687b0'),(0x46A5A3,'je','0x46a5be'),(0x46A5A5,'and','esi, 0xffff'),
      (0x46A5AC,'push','0xcd'),(0x46A5B1,'push','0x5028f8'),(0x46A5B6,'call','0x4c6ae0'),
      (0x46A52B,'push','0xce'),(0x4A2B62,'jne','0x4a2a6f')]
    assertions=[]
    for at,mn,op in expected:
        assert (ins[at]['mnemonic'],ins[at]['operands'])==(mn,op),(hex(at),ins.get(at))
        assertions.append({**w(at),'expected_mnemonic':mn,'expected_operands':op,'match':True})
    assert pe.get_data(0xB3540,1)==bytes([0x76]) and pe.get_data(0xB3784,1)==bytes([0x32])
    assert struct.unpack('<I',pe.get_data(0xB33E0,4))[0]==struct.unpack('<I',pe.get_data(0xB3638,4))[0]==0x4B2F2B
    assert pe.get_data(0x234370,3)==b'ds\0' and pe.get_data(0x1028F8,3)==b'cd\0'
    assert struct.unpack('<I',pe.get_data(0x6A244,4))[0]==0x469CC9
    captures=[]
    for trace in route_obj['wire']:
        q=trace['frames'];p=Path(q['path']);assert record(p)==q;rows=json.loads(p.read_text(encoding='utf-8'))
        d0=[(i,r)for i,r in enumerate(rows)if r.get('direction')=='S'and r.get('body_hex','').startswith('d0')]
        assert len(d0)==len(trace['D0_packets'])
        for (i,r),expected_frame in zip(d0,trace['D0_packets']):
            body=bytes.fromhex(r['body_hex']);assert i==expected_frame['global_array_index_zero_based'] and body.hex()==expected_frame['body_hex']
            assert body[0]==0xD0 and int.from_bytes(body[1:5],'little')==1 and body[-1]==0 and 0 not in body[5:-1]
            name=body[5:-1];assert name.decode('ascii')==expected_frame['alias'] and r['frame_bytes']==len(body)+2
            preceding=[j+1 for j,x in enumerate(rows[:i])if x.get('epoch')==r['epoch']and x.get('direction')=='S'and x.get('body_hex','').startswith('cd')]
            cds=[j+1 for j,x in enumerate(rows[i+1:],i+1)if x.get('epoch')==r['epoch']and x.get('direction')=='S'and x.get('body_hex','').startswith('cd')]
            has_leave=any(x.get('direction')=='C'and x.get('body_hex','').startswith('d0')for x in rows)
            assert not has_leave
            if name==b'ArenaBot':assert not preceding and len(body)==14 and r['frame_bytes']==16 and len(cds)==2
            captures.append({'source':q,'capture':trace['capture'],'array_index_zero_based':i,'frame_1based':i+1,'epoch':r['epoch'],
              'direction':'S','opcode':208,'body_hex':body.hex(),'body_bytes':len(body),'frame_bytes':r['frame_bytes'],
              'fields':[{'index':1,'type':'u32LE','body_offset':1,'bytes':4,'value':1},{'index':2,'type':'NUL terminated opaque 8-bit bytes','body_offset':5,'bytes':len(name)+1,'raw_hex':name.hex(),'ASCII_capture_label':name.decode('ascii')}],
              'consumed':len(body),'remaining':0,'prior_same_epoch_CD_ordinals':preceding,'following_same_epoch_CD_ordinals':cds,
              'C_D0_in_trace':False,'native_execution_or_table_authority':False,'classification':'Known sealed S_D0 corroboration; no new packet/capture'})
    assert [(r['capture'],r['frame_1based'],r['body_bytes'])for r in captures]==[('5ED',69,14),('5ED',137,28),('49A',70,14)]
    wire={'classification':'Existing S_D0 wire corroboration only; no new/corrected wire or opcode',
      'direction':'server-to-client','opcode_decimal':208,'opcode_hex':'D0','native_format':'ds',
      'fields':[{'index':1,'type':'DWORD','endianness':'little','semantic':'room context DWORD, not physical/source ID','body_offset':1,'bytes':4,'destination':'EBP-0C'},
        {'index':2,'type':'NUL terminated 8-bit bytes','encoding':'Native has no fixed codec declaration here; comparison authority follows Batch15 initialized lead/fold tables. Captures ASCII; Java MS950 producer is separate evidence.',
         'body_offset':5,'bytes':'A+1 including NUL','destination':'EBP-7C','destination_capacity':100}],
      'body_bytes':'6+A','transport_frame_bytes':'8+A; sealed framing only','native_read_boundaries':'reader skips opcode, d little endian, s capacity100 including forcedNUL; externalcopy/seek and inputlength validation unqualified; no exact-end test in handler',
      'version_gate':{'signed_version':'DWORD63D54C','threshold_hex':'78B9','below':{'format_pointer':'00634370','format':'ds','reader':w(0x4A90D5)},
        'at_or_above':{'same_format_pointer':'00634370','format':'ds','reader':w(0x4A90BA),'extra_variadic_pointer':'EBP-14','reader_consumes_extra_pointer':False},
        'warning':'Additional pointer at >=78B9 does NOT establish third field. Format ds ends before it; later phase4/5 uses EBP-14 without a visible local initialization/write. No invented dsd/tail or node identity.'},
      'absent_fields':['self/peer flag','success/result byte','kind/category','physical/source ID','epoch/generation','request or leave-intent ID','roster terminator'],
      'trigger':'Existing server reports: ArenaBot replacement/peer departure/self notification. Native receive branch itself tests no pending C_D0 intent. Producer/postcommit remains Java group.',
      'states':'64AF98==1 or6 use room0 preview branch; every other numeric phase passes common currentroom comparison, then self-vs-peer. No general2/3/6 whitelist here.',
      'evidence_grade':'Pinned fixedPE exact instructions/literals + three existing sealed wire examples; no original runtime execution',
      'VA':'004A9090','RVA':'000A9090','fixed_PE_sha256':FIXED,'unknown':['runtime table/locale authority','input bounds and native exact-end handling','nonASCII bytes/actual encoding','true native version/state','private/virtual callbacks','ordinary C_D0/self-D0 dynamic acceptance and server postcommit']}
    routing={'phase_split':{'read':w(0x4A90DD),'preview_phases':[1,6],'preview_target':'004A9472','common_phases':'all other numeric phases; no local allowed-phase whitelist'},
      'common_room_gate':{'incoming':'EBP-0C','current':'64ABF4','reject':w(0x4A9100),'occurs_before_name_compare':True},
      'self_name_gate':{'left':'64ABF8 global selected/member display name','right':'incoming EBP-7C','comparator':'004D3A80','call':w(0x4A910F),'zero_means':'native self branch only under actual qualified tables/state',
        'pending_C_D0_checked':False,'room_list_presence_required':False,'identity_note':'64ABF8 is presentation state also writable by S_CD before its gate; not independent physical/source/owner authority.'},
      'self_branch':{'phase_call':w(0x4A911D),'argument':1,'next':'450B90 lookup selected row64A958, twice if index>=0; row+4 WORD to46A540 else argument0',
        'order':'469CA0(1) returns before selected-row lookup and46A540; its callbacks may alter UI/selection first','row_value':'low16bits zeroextended outboundDWORD; do not assume selected row exists or is owned',
        'native_atomic_commit_proven':False,'ordinary_exit_intent_correlation':'not a native check; server adapter policy requires separate emitted intent and qualified own display bytes'},
      'peer_branch':{'combatant':'begin64AB64/end64AB68; ECX UI64A950','other':'begin64AB54/end64AB58; ECX UI64A94C',
        'key':'4D3A80(incoming alias, record+4)==0 within each vector; no kind carried by D0',
        'record_stride':140,'record_removal':'first match per vector, call465370(record+4), forward compact following140byte records using35DWORD copies, end-=140; no allocator/free in common roster compaction',
        'both_vectors_scanned':True,'cross_category_same_name':'may remove first match in BOTH vectors; not globally stop after combatant match',
        'absent_name':'no vector writes/UI calls/navigation; returns before phase4/5 effects when neither vector matches; requires coherent initialized vector boundaries and comparison authority',
        'navigation_on_peer_match':False,'additional_effects':'if any removal succeeded and phase==4/5, separate formatting/opaque collection branch occurs; depth stopped'},
      'preview_branch':{'room_gate':'incoming DWORD must0; ignores64ABF4 here','compare':'same4D3A80 incoming name vs64ACF4 record+4','UI':'64A954 via465370','remove':'first previewrecord; compact140bytes,4B3B80RET8 then end64ACF8-=140',
        'room1_ArenaBot_in_phase6':'rejected by room0 gate, not ordinaryroom peer no-op; actualnativephase not observed in5ED/49A','self_or_navigation_test':False},
      'comparison_authority':{'inherited_contract':pin,'implementation_bytes':93,'table_guard_retained':True,'actual_initialized_tables_qualified':False,'zero_disk_or_ASCII_equality_is_authority':False}}
    helpers={'UI_remove_465370':{'complete_bytes':191,'comparison':'raw byte exact NUL comparison with item+10, NOT4D3A80; argument from stored record+4',
        'removes':'all matching UIitems by rechecking same index after deletion','calls':[w(0x4653CA),w(0x4653EA)],
        'mutates':'count+2C decremented, pointerarray+58 compacted, selection+28 clamped; runtime UItype/vtable/externalcopy unqualified'},
      'scene_469CA0_argument1':{'existing_prefix_and_jump_table':True,'new_mode1_branch_bytes':506,
        '62C21C_zero':'only stores signedWORDargument1 to64AF98 then returns; noUI/callback branch',
        '62C21C_nonzero':'mode1 jump469CC9; firstvirtual+50 precedes64AF98=1, then UIvisibility/selectionhelpers, callbacks62C9B8/BC/C0 installed46D640/46D8A0/46D630,458830reset, conditionalprotected8316B0/B4/B8 notification,4687B0(0),46A520 existingC_CE listrequest, virtual+4C,49BB40(0),75A998/9C zero',
        'order_warning':'phase/callback globals after dynamic callbacks are not assured to remain as just written; no atomic transition or servercommit inferred',
        'deep_scope':'virtual/private/scene effects stop here; no battle/results/ready reconstruction'},
      'selected_preview_46A540':{'complete_bytes':132,'steps':'450230(UI64A954) first; subsequent source/destination clear range uses ECX==oldend so copy loop skipped; set64ACF8=64ACF4;4687B0(UI64A98C,WORDarg); if SI!=0 send existingC_CD opcode205 formatcd withu32 zeroextendedWORD; if0 noemit',
        'UI_before_pointer_reads':'450230 callback may affect state; no init/liveness proof','not_C_D0_ack':'outbound C_CD is preview request, not ordinary leave confirmation'},
      'phase4_5_extra':{'condition':'nonself commonbranch actualremoval; phase4or5 only','formatting_pointer':'disk*[4F7314]=773F5200, actualprovider/API/runtime not qualified',
        'version78B9_extra':'EBP-14 passed to4B4B00/4B4A10 on64B008 despite ds only; local initialization/data provenance unresolved. Do not invent thirdwirefield/node identity.',
        'opaque_calls':'4B4B00,4B4A10,46F7E0,46F7A0,4F103C,46F380; no deep tree/world analysis',
        'notification':'417860 only if64AF9F==0; subsequent834F08/834F10 formatting loop has opaque scene data. No physical removal completion claim'},
      'all_direct_call_sites':[{**w(at),'scope':'existing direct helper qualified above or unchanged priorcontract; otherwise explicit depthboundary'}for at in sorted(ins)if ins[at]['mnemonic']=='call'and ((0x4A9090<=at<0x4A9550)or(0x469CC9<=at<0x469EC3)or(0x465370<=at<0x46542F)or(0x46A540<=at<0x46A5C4))],
      'unknowns':['UI object/vtable lifetime and dispatch bodies','external77409100 memory operation actual body/provider','4687B0 widget body and initializedstate','49BB40 scene callback body','protected8316B0/B4/B8 runtime arrays/data','phase4/5 opaque collection/tree/private effects','Native current room/name/vector/table authority','server postcommit and ordinary leave roundtrip']}
    consumer={'known_ArenaBot':'On explicit AD368 adapter owned epoch/CEroom1, valid ArenaBot14/16 may precede all CD. Remove-if-present or absent-row no-op, stayROOM and consume laterCD; native runtime comparison not claimed.',
      'peer':'Current room/epoch, independently identified otherpresentationrow -> remove room-local row/projection only. Native D0 scansboth categories; owner/source/player/skill state not inferred.',
      'self':'Native selectedname equality triggers469CA0(1) withoutpendingintent, but cannot use that asadapter proof. Adapter needs independent ownrawalias plus actuallyemitted samecontext C_D0 intent; consume once, label localnavigation policy, noatomicservercommit.',
      'freshness':'D0 hasnoroomgeneration/nonce/epoch. Adaptertransportepoch/localjoingeneration metadata required; do notcarryabsentremoval into futureCD/newjoin. Sameepoch sameidreuse ambiguity remains.',
      'no_blanket_phase_mirroring':'Nativephase1/6 room0 preview vsothercommonbranch distinct. 5ED/49A noactualnativephase. Do notmap everyD0 toCHANNEL.',
      'guard':'Batch15 lead/fold/private initialization qualifications unchanged; noASCII/casefold/zero-table substitute, no tableinjection API.'}
    outputs={'ranges.json':{'source':sources[0],'ranges':ranges,'inherited_raw':inherited},'instruction-witnesses.json':{'assertions':assertions},
      'wire-and-version-boundary.json':wire,'peer-self-preview-routing.json':routing,'direct-helper-boundaries.json':helpers,'sealed-D0-frames.json':{'captures':captures},'consumer-gap.json':consumer}
    for name,obj in outputs.items():write('SD0-receive/'+name,obj)
    verification={'instruction_assertions':len(assertions),'new_code_ranges':sum(r['kind']=='code'for r in ranges),'new_data_ranges':sum(r['kind']=='data'for r in ranges),
      'inherited_raw_ranges':len(inherited),'complete_D0_handler_bytes':1216,'UI_remove_bytes':191,'scene_mode1_bytes':506,'selected_preview_bytes':132,
      'sealed_D0_frames':len(captures),'prior_primary_pins':17,'all_match':True,'server_packet_unchanged':packet,'old_tests_replayed':False}
    write('SD0-receive/new-evidence-verification.json',verification)
    primary={'schema':'lito.native-SD0-receive-boundary.v1','updated_at':stamp(),'status':'FINITE_STATIC_BOUNDARY_SEALED_NATIVE_TABLE_SCENE_AND_POSTCOMMIT_UNQUALIFIED',
      'sources':sources,'prior_primary_pins':prior,'existing_evidence':existing,'reusable_outputs':[record(p)for p in sorted(OUT.iterdir())if p.is_file()and p.name not in ['independent-review.json','READ-ME.txt']],
      'dispatch':{'selectors':'reuseprimarymode64BBD4==4/signedphase>=3; elsefallback','primary':{'map_VA':'004B3540','slot_hex':'76','jump_VA':'004B33E0','target':'004B2F2B'},
        'fallback':{'map_VA':'004B3784','slot_hex':'32','jump_VA':'004B3638','target':'004B2F2B'},'call':w(0x4B2F2D)},
      'wire':wire,'routing':routing,'direct_helpers':helpers,'consumer_gap':consumer,'verification':verification,
      'delta':{'new_wire':[],'wire_corrections':[],'new_opcodes':[],'known_corroboration':['ExistingS_D0 ds receive/threeframes','existingC_CE andC_CD calls withinselfhelpers'],
        'internal_data_qualification':'>=78B9 extraunusedpointer isnot thirdfield; laterEBP-14dataauthority unresolved; no wirecorrection'},
      'scope':{'writes':'ownresourcefolderonly','native_process_started':False,'GUI_input':False,'new_capture':False,'SQL':False,'provider_called_or_installed':False,
        'Java_producer_or_postcommit_taken_over':False,'battle_results_ready_expanded':False,'intercepted_work_reinterpreted':False,'physical_source_player_owner_ready':False,'overall_complete':False}}
    write('SD0-receive-contract.json',primary)
    print(json.dumps({'primary':record(ROOT/'SD0-receive-contract.json'),'verification':verification},ensure_ascii=False))

if __name__=='__main__':main()

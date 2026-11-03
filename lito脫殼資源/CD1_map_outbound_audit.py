"""Batch27 finite Native map request islands; no execution or old PASS replay."""
from audit import *
OUT=ROOT/'CD1-map-outbound'
FIXED='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
def load(n):return json.loads((ROOT/n).read_text('utf-8'))
def main():
    OUT.mkdir(exist_ok=True)
    sources=[record(q) for q in (PE,BIN,ASM)]
    assert sources[0]['sha256']==FIXED
    prev=load('CDD-outbound-contract.json');prevpin=record(ROOT/'CDD-outbound-contract.json')
    assert prevpin['sha256']=='7E09849B4F0D442E04457AEE7E6DBEEB3D106437D9689AE8060323CE36FCB59E'
    prior=prev['prior_primary_pins']+[{**prevpin,'expected_sha256':prevpin['sha256'],'match':True}]
    assert len(prior)==28
    for q in prior:
        a=record(q['path']);assert a['bytes']==q['bytes'] and a['sha256']==q['expected_sha256']
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    ranges=[];reuse=[];ins={};owners={}
    def decode(raw,va,pin):
        rows=[{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str} for i in cs.disasm(raw,va)]
        assert sum(len(bytes.fromhex(i['raw_hex'])) for i in rows)==len(raw)
        for i in rows:ins[int(i['va'],16)]=i;owners[int(i['va'],16)]=pin
        return rows
    for va,n,kind,label in [
      (0x464AD0,24,'code','inputobject_kind_dispatch_only_case7_followed'),
      (0x464DF1,63,'code','case7_selected_item_to_D1sub3_call_andcleanup'),
      (0x464E30,18,'code','postrequest_STOP_firstvirtual_slot0'),
      (0x465110,82,'code','item_add_prefix_STOP_allocation4F118C'),
      (0x465173,12,'code','conditional_postallocation_item_ctor_args'),
      (0x4630F0,81,'code','item_ctor_key_store_then_STOP_unknown463B70'),
      (0x46B7D0,52,'code','mode_input0_or1_branch_dispatch_otherarms_notqualified'),
      (0x46B804,38,'code','menu_prefix_knownlookup_then_STOP_firstvirtual0'),
      (0x46B847,7,'code','alternative_menu_creation_STOP_allocation4F118C'),
      (0x46B9E2,26,'code','adjacent_D1sub2_step1_or2_args_sendercall'),
      (0x464EE4,56,'data','kind_dispatch_14DWORD_case7_target'),
      (0x62C078,4,'data','map_format_cch'),(0x5028CC,4,'data','mode_step_format_ccc'),
    ]:
        raw=pe.get_data(va-0x400000,n);path=OUT/f'{va:08X}-{n:X}.bin';path.write_bytes(raw);pin=record(path)
        ranges.append({**mapping(pe,va),'bytes':n,'kind':kind,'label':label,'raw_hex':raw.hex(),'output':pin,
                       'instructions':decode(raw,va,pin) if kind=='code' else []})
    for index,va,n in [('map-table-source/ranges.json',0x46B926,171),
      ('SCD-member-comparison/ranges.json',0x450B90,37),('skill-pane-layout/ranges.json',0x450BC0,25),
      ('receive-boundary/ranges.json',0x4C6AE0,80),('CDD-outbound/ranges.json',0x4C6916,26),('CDD-outbound/ranges.json',0x4C6930,33)]:
        data=load(index);row=next(q for q in data.get('ranges',data.get('new_ranges',[])) if int(q['va'],16)==va)
        pin=row['output'];assert record(pin['path'])==pin
        raw=Path(pin['path']).read_bytes()[:n];assert raw==pe.get_data(va-0x400000,n)
        reuse.append({**mapping(pe,va),'used_bytes':n,'source_pin':pin,'index':record(ROOT/index),'raw_hex':raw.hex(),
                      'instructions':decode(raw,va,pin),'old_PASS_replayed':False})
    expected=[
      (0x464AD5,'mov','eax, dword ptr [esi + 0x68]'),(0x464ADB,'ja','0x464ee0'),
      (0x464AE1,'jmp','dword ptr [eax*4 + 0x464ee4]'),
      (0x464DF3,'call','0x450b90'),(0x464DFA,'jl','0x464e30'),(0x464DFE,'call','0x450b90'),
      (0x464E03,'mov','ecx, dword ptr [esi + 0x58]'),(0x464E06,'mov','edx, dword ptr [ecx + eax*4]'),
      (0x464E09,'mov','eax, dword ptr [edx + 4]'),(0x464E0C,'cmp','eax, -1'),(0x464E0F,'je','0x464e1b'),
      (0x464E11,'cmp','eax, -0x64'),(0x464E14,'je','0x464e1b'),(0x464E16,'add','eax, 0x1f4'),
      (0x464E1B,'push','eax'),(0x464E1C,'push','3'),(0x464E1E,'push','0xd1'),
      (0x464E23,'push','0x62c078'),(0x464E28,'call','0x4c6ae0'),(0x464E40,'call','dword ptr [eax]'),
      (0x450BA0,'mov','bl, byte ptr [esi + 0xc]'),(0x450BA5,'jne','0x450bb2'),(0x450BAF,'or','eax, 0xffffffff'),
      (0x46B955,'mov','ecx, dword ptr [esi + eax + 0x20]'),(0x46B996,'add','ecx, 0xfffffe0c'),
      (0x46B9A7,'sub','edx, 0x1f4'),(0x46B9B5,'call','0x465110'),
      (0x46512D,'mov','edi, dword ptr [ebp + 8]'),(0x465145,'cmp','dword ptr [ebx + 4], edi'),
      (0x465152,'cmp','dword ptr [esi + 0x2c], 0x100'),(0x46515D,'call','0x4f118c'),
      (0x465177,'push','edi'),(0x46517A,'call','0x4630f0'),(0x463114,'call','0x450bc0'),
      (0x450BCE,'mov','dword ptr [eax + 4], ecx'),(0x450BD1,'mov','byte ptr [eax + 0xc], 0'),
      (0x46313C,'call','0x463b70'),(0x46B828,'call','dword ptr [eax]'),(0x46B849,'call','0x4f118c'),
      (0x46B7F0,'je','0x46b9e6'),(0x46B7F7,'je','0x46b9e2'),
      (0x46B9E2,'push','1'),(0x46B9E6,'push','2'),(0x46B9E8,'push','2'),
      (0x46B9EA,'push','0xd1'),(0x46B9EF,'push','0x5028cc'),(0x46B9F4,'call','0x4c6ae0'),
    ]
    assertions=[]
    for va,mn,op in expected:
        assert (ins[va]['mnemonic'],ins[va]['operands'])==(mn,op),(hex(va),ins[va])
        assertions.append({**mapping(pe,va),**ins[va],'fixed_PE_sha256':FIXED,'range':owners[va],
          'expected_mnemonic':mn,'expected_operands':op,'match':True})
    targets=list(struct.unpack('<14I',pe.get_data(0x64EE4,56)));assert targets[7]==0x464DF1
    assert pe.get_data(0x22C078,4)==b'cch\0' and pe.get_data(0x1028CC,4)==b'ccc\0'
    serverpath=ROOT.parent/'lito伺服器/server-ordinary-room-map-selection-contract.json'
    serverpin=record(serverpath);assert serverpin['sha256']=='3D0B5BAA46238ED5589C193FBDE22B966B945EFDEAB70D2A2C3933A62CA8E15F'
    server=json.loads(serverpath.read_text('utf-8'));detailpin=server['contract'];assert record(detailpin['path'])==detailpin
    existing={n:record(ROOT/n) for n in ['map-table-source-contract.json','map-candidate-index-contract.json','D5-mode-map-receive-contract.json','CDD-outbound-contract.json']}
    existing.update(server_map_contract=serverpin,server_map_detail=detailpin)
    preflight={'scope':'Owned prior contracts/READ-ME/audit references plus fixedASM exact push0D1h navigation, two hits. Not global caller completeness.',
      'complete_native_C_D1_map_outbound_equivalent_found':False,'known_selected_lookup_reused':True,
      'new_gap':'Prior map UIselector stops465110; prior D5 map contracts explicitly do not qualify Native producer. New case7 selected-item-to-send evidence.',
      'exact_push_D1_hits':2,'map_call_VA':'00464E28','adjacent_mode_call_VA':'0046B9F4',
      'no_resource_table_or_capture_replayed':True}
    boundary={
      'handler':'464AD0 inputECX→ESI, objectDWORD+68 unsigned0..13 dispatch, table464EE4 case7→464DF1. Actual UI event/callback, objectidentity and upstream mode7 assignment unknown; otherarms not expanded.',
      'selected_to_call':'Case7 calls qualified450B90 twice. First nonnegative check; second rescans current rows using count+2C,array+58,firstitemBYTE+0C!=0; read selected itemDWORD+4. No unknown call between case7 entry and sender boundary.',
      'selection_unknown':'No actual selection flag+0C producer/click, valid memory/count/list-generation/bounds, stable rows between two scans, currentroom/owner/version/phase gate qualified. Selected lookup is conditional static, not event acceptance.',
      'map_transform':'If itemDWORD ==FFFFFFFF orFFFFFF9C, preserve bits; else ADD500 wrapping32bit. PushDWORD, qualified h serializer writes low16 LE. FFFF andFF9C are actual possible request bytes in this path, not merely D5 projections.',
      'row_to_item':'Existing36stride table+20 DWORD key conditional filtered andminus500 →465110 arg1. Prefix checks duplicate itemDWORD+4 and cap256 then firstunknown allocation46515D→4F118C STOP. Do not bridge allocation/insert/runtime ownership.',
      'conditional_ctor':'Independent postallocation465173 pusheslabel+key arg then4630F0. Prefix calls existing450BC0 which setsitem+4=argDWORD andselection+0C=0; firstunknown46313C→463B70 STOP. No constructor completion/insert/selection claim.',
      'row_key_composition':'Conditional algebra (key-500)+500=key modulo32 for nonsentinel itemkeys, then low16; sentinel collisions key499→itemFFFFFFFF→wireFFFF andkey400→itemFFFFFF9C→wireFF9C. Not actual runtime domains, tableproducer, rowordinal or resourcebinding.',
      'menu_prefix':'Input2 branch46B804 known41B410 lookup then firstvirtual46B828 FF10 slot0 STOP; alternative branch46B847 allocation46B849→4F118C STOP. Later tableisland not bridged to these paths.',
      'postsend':'Afterrequest or no-selectioncleanup464E30 firstvirtual464E40 FF10 STOP; no cleanup effects/UI result or delivery/ACK inferred.',
      'serializer':'Reuse accepted Batch26 c/h andframequalification, no oldPASS rerun. Format cch body4/frame6, flagsender1; actualtransport remains external4EFEA4→759058A0 boundary inherited. No strings/codec or roomID on this request.',
      'mode_adjacent':'Only inputECX0/1 branchvisible in46B7D0 producesstep2/1 respectively at46B9F4, formatccc paramsD1,2,step; body3/frame5. No modelabel, absolute mode value, actualUI trigger, serverunion semantics beyond mapcontract exclusion.',
      'receive_separation':'D5sub3 incomingWORD→BYTE64ACC9 low+12 exceptFFFF/FF9C is independent projection. Neither that BYTE norCCrow+74 norpreview+0F nor offlinecandidateordinal is read by this requestisland.',
      'server':'Existing parser C_D1sub3 exact4body/6frame, unsignedu16map inclFFFF parses then membership rejectsFFFF; currentroom/ROOM/owner/battle/ready semantics belongserver. Existing conditionalrestore564vs514 mismatch unchanged, no newcorrection or Java request.',
      'runtime':'Server selected7captures reportsC_D1sub3/S_D5sub3hits0; adopt report only, no recapture/rescan. No Native outbound dynamicwitness, no ACK/rollback/physical/source authority.',
    }
    wire={
      'direction':'C','opcode':209,'subtype':3,'call_VA':'00464E28','call_RVA':'00064E28','format_VA':'0062C078','format_RVA':'0022C078','format':'cch',
      'fixed_PE':sources[0],'fields':[{'offset':0,'type':'u8','source':'literalD1 at464E1E'},
        {'offset':1,'type':'u8','source':'literal3 at464E1C'},
        {'offset':2,'type':'u16LE','source':'selected itemDWORD+4 transformed at464E0C..16, low16 serializer'}],
      'body_bytes':4,'frame_bytes':6,'frame_prefix':'u16LE total6 inherited4C6AE0',
      'encoding':'No string fields; numeric low16bits littleendian, sourceDWORD bit pattern.',
      'trigger_state':'Conditional objectkind+68==7 with selectedlookup nonnegative; runtime UI/phase/owner/epoch/caller unknown.',
      'source_level':'FixedPE raw static newcallsite plus accepted serializer/selectedlookup; existing server parser corroboration only.',
      'unknowns':boundary,'adjacent_mode':{'direction':'C','opcode':209,'subtype':2,'format':'ccc','format_VA':'005028CC','call_VA':'0046B9F4','call_RVA':'0006B9F4',
        'input0_step':2,'input1_step':1,'body_bytes':3,'frame_bytes':5,'qualification':'Adjacent Native branch only; absolute mode/serverstep semantics/triggerunknown.'}}
    comparison={'server_parser':server['parser'],'restore_difference':server['restore_difference'],
      'new_wire_difference':False,'known_layout_corroboration':True,'server_subtype2_union_outside_map_contract':True,
      'Native_FFFF_request_possible_static':True,'server_FFFF_parser_valid_but_membership_rejected':True,
      'D5_BYTE_CC_row74_preview0F_candidateordinal_not_outbound_source':True,
      'new_dynamic_witnesses':0,'existing_capture_hits_adopted_only':{'C_D1_sub3':0,'S_D5_sub3':0,'finite_roots':7},
      'ACK_rollback_or_resource_binding_qualified':False,'Java_change_or_extra_packet_requested':False}
    packet=record(ROOT/'server-packet-delta.json');assert packet==prev['verification']['server_packet_unchanged']
    verification={'new_code_ranges':10,'new_data_ranges':3,'necessary_reused_raw_ranges':6,
      'instruction_assertions':len(assertions),'map_callsites':1,'adjacent_mode_callsites':1,
      'prior_primary_pins_preserved':28,'all_match':True,'new_capture_frames':0,'Native_outbound_witnesses':0,
      'server_packet_unchanged':packet,'old_PASS_replayed':False}
    for n,obj in [('ranges.json',{'source':sources[0],'new_ranges':ranges,'necessary_reused_raw':reuse}),
      ('instruction-witnesses.json',assertions),('equivalence-search-preflight.json',preflight),
      ('selected-key-and-unknown-matrix.json',boundary),('wire-contract.json',wire),
      ('server-comparison.json',comparison),('finite-verification.json',verification)]:write('CD1-map-outbound/'+n,obj)
    outputs=[record(q) for q in sorted(OUT.iterdir()) if q.is_file()]
    write('CD1-map-outbound-contract.json',{'schema':'lito.native-C_D1-map-outbound-boundary.v1','updated_at':stamp(),
      'status':'FINITE_STATIC_SELECTED_KEY_TO_MAP_CALL_SEALED_UI_TABLE_PRODUCER_TRANSPORT_AUTHORITY_OPEN',
      'sources':sources,'prior_primary_pins':prior,'existing_evidence':existing,'outputs':outputs,
      'wire':wire,'conditional_boundaries':boundary,'server_comparison':comparison,'verification':verification,
      'delta':{'new_wire':[],'new_opcodes':[],'wire_corrections':[],'known_corroboration':'Native selected-item key maprequest and adjacent mode-step callsite static proof; accepted server layout unchanged.'},
      'scope':{'writes':'ownROOTonly','Native_GUI_SQL_provider_Java_Modern_BIN':False,'unknown_bodies_expanded':False,
        'offline_candidates_runtime_table_D5_projection_resource_binding':False,'overall_complete':False}})
    print(json.dumps({'all_match':True,'primary':record(ROOT/'CD1-map-outbound-contract.json'),'verification':verification},ensure_ascii=False))
if __name__=='__main__':main()

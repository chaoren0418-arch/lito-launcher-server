"""Batch25 finite D5 scalars 4/6/7/8/9: existing bytes only, producer unknown."""
from audit import *
OUT=ROOT/'D5-scalar-receive'
FIXED='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
PREVIOUS='9F1B9863C7F0316A4C580BA83DCD14CA92A366418555073FB4FB1181F544ABAE'
SUBTYPES=[4,6,7,8,9]
ENTRIES=[0x4A9FA5,0x4AA028,0x4AA02F,0x4AA036,0x4AA056]
DESTS=[0x64ACCC,0x64ACD0,0x64ACD1,0x64ACD2,0x64ACD3]

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    OUT.mkdir(exist_ok=True)
    sources=[record(p)for p in (PE,BIN,ASM)]
    assert [p['sha256']for p in sources]==[FIXED,'CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292','2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A']
    prev=load('D5-mode-map-receive-contract.json');prevpin=record(ROOT/'D5-mode-map-receive-contract.json')
    assert prevpin['sha256']==PREVIOUS
    prior=prev['prior_primary_pins']+[{**prevpin,'expected_sha256':PREVIOUS,'match':True}];assert len(prior)==26
    for p in prior:
        q=record(Path(p['path']));assert q['bytes']==p['bytes'] and q['sha256']==p['expected_sha256']
    b18=load('D5-CF-receive/ranges.json');b18pin=record(ROOT/'D5-CF-receive/ranges.json')
    assert b18pin['sha256']=='75D5F21B7CADA8A8AD5ECA6660B6C97A11821A940FF2E33DA80F0C8D22AE40B4'
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    slices=[];ins={};owners={}
    def use(va,n,label,kind='code'):
        found=None
        for collection,key in [(b18['ranges'],'output'),(b18['inherited_raw'],'range')]:
            for r in collection:
                base=int(r['va'],16);p=r[key]
                if base<=va and va+n<=base+p['bytes']:found=(base,p);break
            if found:break
        assert found,(hex(va),n)
        base,p=found;raw=Path(p['path']).read_bytes()[va-base:va-base+n]
        assert record(Path(p['path']))==p and raw==pe.get_data(va-0x400000,n)
        instructions=[]
        if kind=='code':
            for i in cs.disasm(raw,va):
                row={'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}
                instructions.append(row);ins[i.address]=row;owners[i.address]=p
            assert sum(len(bytes.fromhex(i['raw_hex']))for i in instructions)==n
        slices.append({**mapping(pe,va),'used_bytes':n,'source_byte_offset':va-base,
                       'source_base_VA':f'{base:08X}','source_pin':p,'source_index':b18pin,
                       'kind':kind,'label':label,'raw_hex':raw.hex(),'instructions':instructions,'old_PASS_replayed':False})
    for va,n,label,kind in [
      (0x4A9E50,91,'inherited_D5_header_gate','code'),(0x4AA074,40,'D5_subtype_jump_table','data'),
      (0x4A9FA5,10,'subtype4_entry_destination_and_noUI_tail_jump','code'),
      (0x4AA028,7,'subtype6_entry_destination_and_UI_tail_jump','code'),
      (0x4AA02F,7,'subtype7_entry_destination_and_UI_tail_jump','code'),
      (0x4AA036,5,'subtype8_entry_destination_UI_tail_fallthrough','code'),
      (0x4AA03B,27,'shared_6_7_8_c_read_RET_UIedge_and_return','code'),
      (0x4AA056,5,'subtype9_entry_destination_noUI_fallthrough','code'),
      (0x4AA05B,22,'shared_4_9_c_read_and_noUI_return','code'),
      (0x4A9030,1,'existing_direct_UIcallee_is_single_RET','code'),
      (0x5025A8,2,'c_literal','data'),(0x4A2A50,58,'existing_reader_dispatch_prefix','code'),
      (0x4A2A8A,16,'existing_c_BYTEstore','code'),(0x4A2B56,34,'existing_reader_cursor_return','code'),
      (0x4A2B78,24,'reader_jump_targets','data'),(0x4A2B90,33,'reader_char_map','data'),
    ]:use(va,n,label,kind)
    def w(va):return {'fixed_PE_sha256':FIXED,**mapping(pe,va),**ins[va],'range':owners[va]}
    expected=[
      (0x4A9E77,'cmp','eax, 6'),(0x4A9E7F,'mov','dword ptr [0x64abf4], eax'),
      (0x4A9E8F,'cmp','ecx, eax'),(0x4A9E91,'jne','0x4aa06c'),(0x4A9E97,'movsx','eax, byte ptr [ebp - 1]'),(0x4A9E9E,'ja','0x4aa06c'),
      (0x4A9FA5,'push','0x64accc'),(0x4A9FAA,'jmp','0x4aa05b'),
      (0x4AA028,'push','0x64acd0'),(0x4AA02D,'jmp','0x4aa03b'),
      (0x4AA02F,'push','0x64acd1'),(0x4AA034,'jmp','0x4aa03b'),
      (0x4AA036,'push','0x64acd2'),(0x4AA03B,'add','esi, 6'),
      (0x4AA03E,'push','0x5025a8'),(0x4AA043,'push','esi'),(0x4AA044,'call','0x4a2a50'),
      (0x4AA049,'add','esp, 0xc'),(0x4AA04C,'call','0x4a9030'),(0x4A9030,'ret',''),(0x4AA055,'ret',''),
      (0x4AA056,'push','0x64acd3'),(0x4AA05B,'add','esi, 6'),(0x4AA05E,'push','0x5025a8'),
      (0x4AA063,'push','esi'),(0x4AA064,'call','0x4a2a50'),(0x4AA069,'add','esp, 0xc'),(0x4AA070,'ret',''),
      (0x4A2A8D,'mov','al, byte ptr [ebx]'),(0x4A2A92,'inc','ebx'),(0x4A2A93,'mov','byte ptr [edx], al'),
      (0x4A2B69,'mov','eax, ebx'),(0x4A2B70,'ret','')]
    assertions=[]
    for va,mn,op in expected:
        assert(ins[va]['mnemonic'],ins[va]['operands'])==(mn,op),(hex(va),ins[va])
        assertions.append({**w(va),'expected_mnemonic':mn,'expected_operands':op,'match':True})
    jumps=struct.unpack('<10I',pe.get_data(0xAA074,40));assert [jumps[x]for x in SUBTYPES]==ENTRIES
    branches=[]
    for va in (0x4A9E7A,0x4A9E84,0x4A9E91,0x4A9E9E,0x4A9FAA,0x4AA02D,0x4AA034):
        target=int(ins[va]['operands'],16);assert target in ins
        branches.append({'va':f'{va:08X}','target':f'{target:08X}','target_is_instruction_boundary':True})
    serverpath=ROOT.parent/'lito伺服器/server-room-D5-CF-consumer-supplement.json';serverpin=record(serverpath)
    assert serverpin['sha256']=='7774EA7546EEF33CB5240A58A36FA9FFF7A465303049C3C3E238081887279BEF'
    server=json.loads(serverpath.read_text('utf-8'))
    layout=next(x for x in server['D5_layouts']if x['subtypes']==SUBTYPES)
    assert(layout['body_bytes'],layout['frame_bytes'])==(7,9)
    gate={'inherited_contract':prevpin,
          'header':'Existingcd subtypeBYTE localEBP-1 /roomDWORD localEBP-8; payloadbody+6. Existingprimary/fallbackdispatchunchanged.',
          'phase6':'4A9E7F roomDWORD→64ABF4 before subtype validity; no priorroom equality.',
          'other_phases':'Incomingroom!=64ABF4→4AA06Creturn; match maydispatch without furtherphasewhitelist.',
          'subtype':'MOVSX i8 thenunsignedJA>9 allows0..9, eachscalarpacketindependentlygated.',
          'additional_gates':'No explicitversion/owner/self/ready/sourcecheck in these finitepaths. Upstream/runtime inputs remainconditional.',
          'Modern':'Validateownedepoch/currentCEroom beforepublish; noNativephase6room/sourceauthority.'}
    matrix=[];wire=[]
    for index,(subtype,entry,dest)in enumerate(zip(SUBTYPES,ENTRIES,DESTS)):
        ui=subtype in (6,7,8)
        common=0x4AA03B if ui else 0x4AA05B
        entrybytes={4:10,6:7,7:7,8:5,9:5}[subtype];pathbytes=entrybytes+(27 if ui else 22)
        matrix.append({
          'subtype':subtype,'summary_index_candidate':index,'entry_VA':f'{entry:08X}','entry_RVA':f'{entry-0x400000:08X}',
          'value_body_offset':6,'value_type':'u8 raw BYTE','destination_VA':f'{dest:08X}','destination_width':1,
          'common_tail_VA':f'{common:08X}','complete_path_bytes':pathbytes,
          'reader_call_VA':'004AA044'if ui else'004AA064',
          'typed_reader_store_VA':'004A2A93','value_mask_or_boolean_conversion':False,
          'direct_UI_edge_VA':'004AA04C'if ui else None,
          'direct_UI_callee_VA':'004A9030'if ui else None,
          'direct_UI_callee_effect':'FixedPE singleC3 RET; noredraw/store/callback qualified'if ui else'NoUIcalleeedge in thispath',
          'first_unknown_private_external_virtual_callee':None,
          'return_VA':'004AA055'if ui else'004AA070',
          'adjacent_bytes_retained':[f'{dest-1:08X}',f'{dest+1:08X}'],
          'other_summary_bytes_retained':[f'{d:08X}'for d in DESTS if d!=dest],
          'producer_builder_layout_qualified':True,'actual_production_caller_or_trigger_qualified':False,
          'dynamic_scalar_witness_qualified':False,
          'field_semantic_beyond_source_summary_index':'UNKNOWN: not ready/mode/map/owner orbool/bitdomain byguess',
          'ACK_ready_owner_source_GUI_qualified':False,
          'unknowns':['actualNativephase/currentroom/inputavailability','Nativebounds/exactend','semanticvalue/domain/controlmeaning',
                      'serverproductioncaller/trigger/delivery','actualGUI/redraw/runtimeobserver','owner/ready/source/physical/sessionauthority'],
        })
        wire.append({
          'classification':'KNOWN_SCALAR_LAYOUT_NEW_FINITE_NATIVE_DESTINATION_UI_PATH_QUALIFICATION',
          'direction':'server-to-client','opcode_hex':'D5','opcode_decimal':213,'subtype':subtype,
          'VA':f'{entry:08X}','RVA':f'{entry-0x400000:08X}','fixed_PE_sha256':FIXED,
          'fields':[{'body_offset':1,'type':'u8','bytes':1,'semantic':'subtype'},
                    {'body_offset':2,'type':'u32LE','bytes':4,'semantic':'roomcontext'},
                    {'body_offset':6,'type':'u8','bytes':1,'semantic':f'source-summary-index{index} candidate, value meaningunknown','destination':f'{dest:08X} BYTE'}],
          'body_bytes':7,'frame_bytes':9,'endianness':'u32LE; u8singlebyte','encoding':'no strings/no textcodec',
          'trigger_state':'AD368 builder/helper layout exists; actualproducer caller/trigger NOTqualified. InheritedNativeD5roomgateonly.',
          'evidence_level':'FixedPE existingraw completefinitebranch/staticBYTEstore + existingserverbuilder/source/archivedILlayout; no dynamicwitness',
          'unknowns':matrix[-1]['unknowns'],
        })
    preserved={
      'mode_map':'No64ACCA modeBYTE/64ACCB highBYTE/64ACC9 mapprojectionstore, no rawmapWORD/table/resource lookup.',
      'configuration':'No64ACD4..D7 options or64ACD8..DF selectionsstore; attributes64ABE6/password64ABE8/capacity64ABF0 unchanged byfinitepaths.',
      'name_owner':'No64AB70 roomname/64ABCA ownerstring write; noalias/member/selflink.',
      'catalog_preview':'NoUI64A958 catalogrow/row+74 orUI64A98C previewWORD+0C/BYTE+0F directstore/callpath.',
      'adjacent_gap':'Sub4only64ACCC;64ACCD..CF remainwithoutexplicitstores. Sub6..9one64ACD0..D3BYTE each; siblings andnextoption64ACD4retained.',
      'ready_authority':'NoD1/ready/phase/member/owner/source/physical/sessionauthority writes; inheritedphase6roomDWORDgate remains distinct.',
      'event_granularity':'EachscalarwritesonesummaryBYTE andreturns; no whole-summary replacement, atomicpair/generation/requestresult/rollbackqualified. Noautomatic0/1/2/3/5followup.',
      'scope':'Noexplicitdestination in finitepaths, notwholeimageabsence oractualruntimeGUIacceptance.',
    }
    comparison={
      'server_contract':serverpin,'adopted_scalar_layout':layout,
      'summary_index_mapping':dict(zip(map(str,range(5)),SUBTYPES)),
      'builder_qualification':'ExistingAD368 helper/source/archivedIL supports7body/9frame layouts andindex→subtype map only.',
      'caller_qualification':'Existing7774explicitlywithholds productioncaller/effect pendingtrigger evidence. Thisbatchdoesnotsearch/executeJava orrequestnewpackets.',
      'selected_wire_scope':{'source_scope':'Existing7774selectedordinaryexit only','observed_D5_subtypes':server['verification']['observed_D5_subtypes'],
                             'scalar_dynamic_witness_adopted':False,'capture_census_rerun':False},
      'Modern_controls':'Recordsubtype/value/destination asfutureguardedmetadata; noaction/controlmeaning/produceravailability/ACK/ready/GUIfrombuilderexistence. Anyfutureoutboundoperation needsitsownCprotocol/caller/triggerqualification.',
      'do_not_infer':['Java mustsendscalarpackets','valuebool/bitmask/domain','aggregate settings accepted','ownermembership/sourceauthority','NativeGUIredraw','ACK/ready/start/rollback'],
    }
    initial=bytes(range(0xA0,0xA0+(0x64ACE0-0x64ACC9)))
    models=[]
    for subtype,dest,value in zip(SUBTYPES,DESTS,(255,128,1,0,7)):
        after=bytearray(initial);after[dest-0x64ACC9]=value
        body=bytes([0xD5,subtype,1,0,0,0,value])
        models.append({'subtype':subtype,'input_BYTE':value,'destination_VA':f'{dest:08X}',
                       'interval_VA':['0064ACC9','0064ACE0 exclusive'],'before_hex':initial.hex(),'after_hex':bytes(after).hex(),
                       'computed_body_hex':body.hex(),'body_bytes':7,'frame_bytes':9,
                       'actual_trace':False,'Native_executed':False,
                       'qualification':'Computedbytefootprint/layoutonly; noNative/server/GUI/deliverywitness'})
    verification={'new_code_ranges':0,'new_data_ranges':0,'necessary_reused_raw_slices':len(slices),
                  'distinct_reused_raw_pins':len({s['source_pin']['path']for s in slices}),
                  'instruction_assertions':len(assertions),'aligned_in_slice_branch_targets':len(branches),
                  'scalar_branches_complete':5,'direct_RET_UI_edge_subtypes':[6,7,8],'no_UI_edge_subtypes':[4,9],
                  'offline_bytefootprint_examples':5,'dynamic_scalar_witnesses':0,'new_capture_frames':0,
                  'prior_primary_pins_preserved':26,'all_match':True,
                  'server_packet_unchanged':record(ROOT/'server-packet-delta.json'),'old_PASS_replayed':False}
    outputs={
      'ranges.json':{'fixed_source':sources[0],'new_ranges':[],'necessary_reused_raw_slices':slices},
      'instruction-witnesses.json':{'assertions':assertions,'aligned_branches':branches},
      'scalar-effect-unknown-matrix.json':{'gate':gate,'rows':matrix,'retained_fields':preserved},
      'server-builder-caller-boundary.json':comparison,
      'computed-bytefootprint-examples.json':{'qualification':'computedonly/nocapture/noexecution','examples':models},
      'wire-contracts.json':{'known_contracts':wire,'new_wire':[],'wire_corrections':[]},
      'finite-verification.json':verification,
    }
    for n,o in outputs.items():write('D5-scalar-receive/'+n,o)
    report={
      'schema':'lito.native-D5-scalar-subtypes4-6-7-8-9.v1','updated_at':stamp(),
      'status':'FINITE_D5_SCALAR_STORES_UI_RET_EDGES_SEALED_PRODUCER_RUNTIME_GUI_AUTHORITY_OPEN',
      'sources':sources,'prior_primary_pins':prior,'inherited_D5_mode_map_contract':prevpin,
      'existing_evidence':{'Batch18_ranges':b18pin,'server_D5_contract':serverpin},
      'outputs':[record(p)for p in sorted(OUT.iterdir())if p.is_file()and p.name not in ('READ-ME.txt','independent-review.json')],
      'gate':gate,'effect_unknown_matrix':matrix,'preserved_fields':preserved,'server_builder_caller_boundary':comparison,
      'wire':wire,'verification':verification,
      'delta':{'new_wire':[],'new_opcodes':[],'wire_corrections':[],
               'known_corroboration':['D5scalar7body9frame/summaryindex-subtype layouts'],
               'new_static_qualification':['fiveexactBYTEdestinations/fullfinitebranches','only6/7/8directUIedge4AA04C→fixedRET4A9030','4/9sharednoUItail; adjacent/siblingfieldretention'],
               'Java_new_packets_requested':False,'packet_modified':False},
      'scope':{'writes':'ownresource directory only','finite_batch_complete':True,
               'Native_SQL_provider_GUI_process_or_new_capture':False,'Java_Modern_BIN_modified':False,
               'private_external_virtual_WORLD_body_expansion':False,'old_PASS_or_capture_replay':False,
               'subtypes0_1_2_3_5_redone':False,'producer_caller_ACK_ready_owner_source_GUI_authority':False,
               'first_unknown_private_external_virtual_callee_in_finite_paths':None,
               'stop':'FixedtypedBYTEreader andRET edges complete; actualproducer/meaning/bounds/runtime/UI/delivery/authority unknown. No extraJavawire request orinterceptedserverwork.'}}
    write('D5-scalar-receive-contract.json',report)
    print(json.dumps({'primary':record(ROOT/'D5-scalar-receive-contract.json'),'verification':verification},ensure_ascii=False))

if __name__=='__main__':main()

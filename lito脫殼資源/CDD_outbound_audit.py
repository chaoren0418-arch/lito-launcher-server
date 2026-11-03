"""Batch26 C_DD conditional argument/serializer boundary; UI virtual STOP."""
from audit import *
OUT=ROOT/'CDD-outbound'
FIXED='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
PREVIOUS='4BBBEEB521ECF20E6B27D78F03B25F503A5766412DA9E3FBEE3DC11A7C389BAA'

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    OUT.mkdir(exist_ok=True)
    sources=[record(p)for p in (PE,BIN,ASM)]
    assert [p['sha256']for p in sources]==[FIXED,'CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292','2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A']
    prev=load('D5-scalar-receive-contract.json');prevpin=record(ROOT/'D5-scalar-receive-contract.json');assert prevpin['sha256']==PREVIOUS
    prior=prev['prior_primary_pins']+[{**prevpin,'expected_sha256':PREVIOUS,'match':True}];assert len(prior)==27
    for p in prior:
        q=record(Path(p['path']));assert q['bytes']==p['bytes'] and q['sha256']==p['expected_sha256']
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    ranges=[];reused=[];ins={};owners={}
    def decode(raw,va,p):
        out=[]
        for i in cs.disasm(raw,va):
            r={'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}
            out.append(r);ins[i.address]=r;owners[i.address]=p
        assert sum(len(bytes.fromhex(x['raw_hex']))for x in out)==len(raw),(hex(va),len(raw))
        return out
    def new(va,n,kind,label):
        raw=pe.get_data(va-0x400000,n);p=OUT/f'{va:08X}-{n:X}.bin';p.write_bytes(raw);q=record(p)
        ranges.append({**mapping(pe,va),'bytes':n,'kind':kind,'label':label,'raw_hex':raw.hex(),'output':q,
                       'instructions':decode(raw,va,q)if kind=='code'else[]})
    for va,n,kind,label in [
      (0x461050,5,'code','UI_candidate_thunk_to461060'),(0x461060,30,'code','UI_prefix_STOP_firstvirtual54'),
      (0x462910,5,'code','UI_candidate_thunk_to462620'),(0x462620,43,'code','UI_prefix_STOP_firstvirtual54_secondcandidate'),
      (0x461158,41,'code','conditional_objectBYTE_and_runtimeXOR_selector'),
      (0x461181,38,'code','DD_zeroextended_tail_argument_island_andcall'),
      (0x4611B8,20,'code','DD_compact_arguments_jump_sharedformat'),(0x461231,10,'code','sharedcompactformat_sendercall'),
      (0x462874,102,'code','conditional_selection_compaction_DD_CC_opcode_andextendedargs'),
      (0x461119,48,'code','conditional_capacity2to200_andvirtualreject_STOP'),
      (0x462835,48,'code','conditional_capacity2to200_secondcandidate_andvirtualreject_STOP'),
      (0x461149,15,'code','presend_unqualifieddirecthelper_call_boundary'),
      (0x462865,15,'code','presend_unqualifieddirecthelper_secondboundary'),
      (0x4C8790,5,'code','presend_helper_thunk_STOP_unknown4C85E0_body'),
      (0x4F11A0,47,'code','sender_stack_probe_complete_noexternalcall'),
      (0x4C68D0,70,'code','serializer_common_dispatch_prefix'),
      (0x4C6916,26,'code','serializer_c_BYTE'),(0x4C6930,33,'code','serializer_h_lowWORD_LE'),
      (0x4C6989,77,'code','serializer_s_rawNULbyte_scan_andcopy_noencoding'),
      (0x4C69D6,47,'code','serializer_b_count_andbytebuffer_copy'),
      (0x4C6A3F,53,'code','serializer_common_cursor_formatloop_andreturns'),
      (0x4EFEA4,5,'code','transport_thunk_STOP_fixedexternal759058A0_notruntimeAPI'),
      (0x62BF90,6,'data','compact_cscsh'),(0x62BF98,11,'data','zeroextended_cscshccccc'),
      (0x62BFA4,12,'data','extended_cscshcccccb'),
      (0x4C6A74,28,'data','serializer_format_jump_targets'),(0x4C6A90,33,'data','serializer_format_char_map'),
    ]:new(va,n,kind,label)
    for index,va,n in [('SCE-navigation/ranges.json',0x41B410,15),
                       ('receive-boundary/ranges.json',0x4C6AE0,80),
                       ('receive-boundary/ranges.json',0x4C6770,101),
                       ('receive-boundary/ranges.json',0x4C6700,55)]:
        row=next(x for x in load(index)['ranges']if int(x['va'],16)==va);pin=row['output'];raw=Path(pin['path']).read_bytes()[:n]
        assert record(Path(pin['path']))==pin and raw==pe.get_data(va-0x400000,n)
        reused.append({**mapping(pe,va),'used_bytes':n,'source_pin':pin,'index':record(ROOT/index),'raw_hex':raw.hex(),
                       'instructions':decode(raw,va,pin),'old_PASS_replayed':False})
    def w(va):return {'fixed_PE_sha256':FIXED,**mapping(pe,va),**ins[va],'range':owners[va]}
    expected=[
      (0x461050,'jmp','0x461060'),(0x462910,'jmp','0x462620'),
      (0x461072,'call','0x41b410'),(0x46107B,'call','dword ptr [edx + 0x54]'),
      (0x46263F,'call','0x41b410'),(0x462648,'call','dword ptr [edx + 0x54]'),
      (0x461158,'mov','al, byte ptr [esi + 8]'),(0x46116B,'xor','ecx, 0xb104fa87'),(0x46117C,'cmp','eax, 1'),
      (0x461189,'movsx','edx, bl'),(0x461198,'push','0xdd'),(0x46119D,'push','0x62bf98'),(0x4611A2,'call','0x4c6ae0'),
      (0x4611C5,'push','0xdd'),(0x4611CA,'jmp','0x461231'),(0x461231,'push','0x62bf90'),(0x461236,'call','0x4c6ae0'),
      (0x462878,'mov','cl, byte ptr [ebp + esi - 0x14]'),(0x462885,'mov','byte ptr [ebp + edx - 0x2c], cl'),
      (0x46288A,'cmp','esi, 8'),(0x46288F,'mov','cl, byte ptr [ebx + 0x4c]'),
      (0x46289F,'push','eax'),(0x4628A4,'push','eax'),(0x4628C4,'push','0xdd'),(0x4628CB,'push','0xcc'),
      (0x4628D0,'push','0x62bfa4'),(0x4628D5,'call','0x4c6ae0'),
      (0x46111E,'cmp','edi, 1'),(0x461132,'cmp','edi, 0xc8'),(0x46283A,'cmp','edi, 1'),(0x46284E,'cmp','edi, 0xc8'),
      (0x461153,'call','0x4c8790'),(0x46286F,'call','0x4c8790'),(0x4C8790,'jmp','0x4c85e0'),
      (0x4C6AE8,'call','0x4f11a0'),(0x4F11CE,'ret',''),(0x4C6B00,'call','0x4c68d0'),
      (0x4C6928,'mov','byte ptr [ebx], cl'),(0x4C6943,'mov','byte ptr [ebx], cl'),(0x4C6949,'mov','byte ptr [ebx], cl'),
      (0x4C699B,'repne scasb','al, byte ptr es:[edi]'),(0x4C69CB,'rep movsd','dword ptr es:[edi], dword ptr [esi]'),
      (0x4C69E6,'add','edi, ebx'),(0x4C69ED,'jge','0x4c6a69'),(0x4C69FA,'rep movsd','dword ptr es:[edi], dword ptr [esi]'),
      (0x4C6B19,'mov','byte ptr [ebp - 0x13ff], dl'),(0x4C6B21,'mov','byte ptr [ebp - 0x1400], al'),
      (0x4C6B27,'call','0x4c6770'),(0x4C679E,'call','0x4c6700'),
      (0x4C67D0,'call','0x4efea4'),(0x4EFEA4,'jmp','0x759058a0')]
    assertions=[]
    for va,mn,op in expected:
        assert(ins[va]['mnemonic'],ins[va]['operands'])==(mn,op),(hex(va),ins[va])
        assertions.append({**w(va),'expected_mnemonic':mn,'expected_operands':op,'match':True})
    charmap=pe.get_data(0xC6A90,33);jumps=struct.unpack('<7I',pe.get_data(0xC6A74,28))
    assert [jumps[charmap[ord(c)-ord('S')]]for c in 'cshb']==[0x4C6916,0x4C6989,0x4C6930,0x4C69D6]
    for va,text in [(0x62BF90,b'cscsh\0'),(0x62BF98,b'cscshccccc\0'),(0x62BFA4,b'cscshcccccb\0')]:assert pe.get_data(va-0x400000,len(text))==text
    serverpath=ROOT.parent/'lito伺服器/server-ordinary-room-DD-edit-network.json';serverpin=record(serverpath)
    assert serverpin['sha256']=='A13ECBFE9DC9BFCBA1D32649C829FB960DA995591C59C8613BB403FA4D62C168'
    server=json.loads(serverpath.read_text('utf-8'));staticpin=server['static_contract'];assert record(Path(staticpin['path']))==staticpin
    static=json.loads(Path(staticpin['path']).read_text('utf-8'));framespin=server['new_network']['frames'];assert record(Path(framespin['path']))==framespin
    rows=json.loads(Path(framespin['path']).read_text('utf-8'));saved=[]
    for index in (76,83):
        r=rows[index];body=bytes.fromhex(r['body_hex']);assert r['direction']=='C' and body[0]==0xDD
        e=body.index(0,1);name=body[1:e];attrs=body[e+1];p=e+2;q=body.index(0,p);password=body[p:q];cap=int.from_bytes(body[q+1:q+3],'little')
        assert q+3==len(body) and r['frame_bytes']==len(body)+2
        saved.append({'source':framespin,'array_index_zero_based':index,'direction':'C','epoch':r['epoch'],
                      'body_hex':body.hex(),'body_bytes':len(body),'frame_bytes':r['frame_bytes'],
                      'name_hex':name.hex(),'attributes':attrs,'password_hex':password.hex(),'capacity_u16LE':cap,
                      'layout':'compact','consumed':q+3,'remaining':0,
                      'qualification':'Existingservertest-clientASCII witness only; NOT Native UI/outbound execution orACK/extended qualification.'})
    preflight={'search_scope':'Owned JSON/Python/TXT/Markdown C_DD/cscsh references + fixedASM exactpush0DDh navigation; no all-UI/body census.',
               'complete_Native_CDD_equivalent_found':False,
               'existing_source_pins':[record(ROOT/n)for n in ('D5-settings-receive-contract.json','reconnect-source-index/private-external-boundaries.json','receive-boundary/ranges.json')],
               'gap':'ExistingD5describesnon-ACKsnapshot, reconnect leavesformatter/stackhelper unqualified, receiveboundary recordsframe/sendprefix. NoCDDUIargument/serializerfield qualification found.',
               'fixedASM_push0DDh_hits':5,'conditional_CDD_callsites':3,
               'navigation_false_candidates':['45FFADregion/430D20 UI coordinate argument','46E6DAregion/430D20 UI coordinate argument'],
               'qualification':'FiniteASMnavigation counts, not wholePE direct/indirectcaller completeness. Rawbytesdecide.'}
    callsites=[
      {'id':'compact','call_VA':'00461236','call_RVA':'00061236','format':'cscsh','opcode_source_VA':'004611C5',
       'condition':'Conditionalisland only: object[ESI+8]!=0 and runtimeXORdecoded!=1; no currentUI/phase/owner proof.',
       'args':['DD','pointerEBP-70','signextendBL (serializedlowBYTE)','pointerEBP-14','EDI (serializedlowWORDLE)'],
       'body_bytes':'6+N+P','frame_bytes':'8+N+P'},
      {'id':'legacy_zero_options','call_VA':'004611A2','call_RVA':'000611A2','format':'cscshccccc','opcode_source_VA':'00461198',
       'condition':'Conditionalisland: object[ESI+8]!=0 andruntimeXORdecoded==1, notqualifiedversionselector.',
       'args':['DD','pointerEBP-70','signextendBL (lowBYTE)','pointerEBP-14','EDI (lowWORDLE)','0','0','0','0','0(K)'],
       'body_bytes':'11+N+P','frame_bytes':'13+N+P'},
      {'id':'options_and_selection','call_VA':'004628D5','call_RVA':'000628D5','format':'cscshcccccb','opcode_source_VA':'004628C4',
       'condition':'Conditionalisland object[EBX+4C]!=0→DD, zero→CC (counteropcode only, noC_CCtaskexpansion).',
       'args':['DD','pointerEBP-90','signextendBYTEEBP-2 (lowBYTE)','pointerEBP-24','EDI (lowWORDLE)',
               'signextendBYTEEBP-3','signextendBYTEEBP-4','signextendBYTEEBP-5','signextendBYTEEBP-1','K(c BYTE)','K(b DWORDbytecount)','pointerEBP-2C'],
       'selection':'Scan8localsEBP-14..-D, compactnonzeroBYTEsinorder→EBP-2C, K0..8; duplicateKforwirec andbcopylength.',
       'body_bytes':'11+N+P+K','frame_bytes':'13+N+P+K'},
    ]
    boundary={
      'UI_entry1':'461050jmp461060; knownchildlookup41B410 index8 fromobject+0C, firstunknownvirtual46107B FF5254 STOP.',
      'UI_entry2':'462910jmp462620; knownchildlookup41B410 index4 fromobject+08, firstunknownvirtual462648 FF5254 STOP.',
      'UI_continuity':'No complete UI-submit→send qualification. Laterargs/capacity/selectionislands areconditional independentstatic evidence; unknownvirtualgetters/converters/contexts notbridged.',
      'field_sources':'NativeactualUIstrings/BL/localoptionbytes/EDI comeafterunknownvirtual/externalinputs. Onlystackpointer/registersourcesqualified; fieldnamesbasedonservergrammar, notactualgettersemantics/codec.',
      'presend_helper':'461153/46286F call4C8790→4C85E0 bodyunqualified STOP. No helpercompletion orsideeffectclaim.',
      'selectors':'ObjectBYTE choosesDDvsCC; runtime8316B0/B4/B8 XORindex decode==1 chooseslegacyzerooptionlayout. Not63D54C wireversion,64AF98phase orowner/sessionidentity qualification.',
      'capacity':'Visibleconditionalnumericislands require signedEDI>1 and<=200; rejectedbranchescallunknownvirtual+70 STOP. Numericconversion/output/runtimeadmissionunknown; serverordinary2..100 distinct.',
      'serializer':'4C6AE0 callsnewlyqualifiednoexternalstackprobe4F11A0 then4C68D0. Onlyformatsc/s/h/b neededbytheseislandsqualified; unusedwideS/private/externalbranchesnotexpanded.',
      'c':'writeslowBYTE ofDWORDvararg; h writeslowWORDLE; signedpushesdo notchangeoriginallowBYTE.',
      's':'Raw8bit NULscan/REPcopyincludesNUL; noencodingconversion. SourceNUL/bufferavailabilityunknown; NativecodecnotMS950byassumption.',
      'b':'DWORDbytecount+pointer, rawbytecopywithoutterminator. Destinationguardused+K>=0x13FE returns0 (strictlessthanlimit); no sourceboundsqualification.',
      'frame':'Nonzeroformatterlength+2 writtenu16LE atstackEBP-1400, call4C6770(...,1). Formatter0skipssend; actualresult/bounds/inputunknown.',
      'transport':'Inherited4C6770prefixguard8317D0/positivecount,flag1callsfixedencodeleaf4C6700; then4C67D0→4EFEA4 E9F7594175→fixedexternal759058A0 STOP. NotactualruntimeIAT/API/socket/delivery/ACK.',
      'state':'No provenphase/ownergate/callertrigger beforeUISTOP; no extraidentity/ready/source/physical/sessionauthority. D5sub0/1independentandidenticalsuccess/rejectbodies fromBatch23notACK.',
    }
    fields=[{'order':0,'type':'u8','bytes':1,'semantic':'C_DD opcodeDD'},
            {'order':1,'type':'NUL8bitstring','bytes':'N+1','semantic':'room_name candidate','source':'EBP-70 orEBP-90'},
            {'order':2,'type':'u8','bytes':1,'semantic':'attributes candidate','source':'BL orBYTEEBP-2'},
            {'order':3,'type':'NUL8bitstring','bytes':'P+1','semantic':'password candidate','source':'EBP-14 orEBP-24'},
            {'order':4,'type':'u16LE','bytes':2,'semantic':'capacity candidate','source':'EDI lowWORD'}]
    wire={'classification':'KNOWN_SERVER_CDD_LAYOUT_NEW_NATIVE_CONDITIONAL_CALLSITE_SERIALIZER_CORROBORATION',
          'direction':'client-to-server','opcode_hex':'DD','opcode_decimal':221,'room_id_on_wire':False,
          'fixed_PE_sha256':FIXED,'fields':fields,
          'extended_fields':[{'type':'fouru8','bytes':4,'semantic':'options1..4'},
                             {'type':'u8','bytes':1,'semantic':'K selectioncount'},
                             {'type':'Krawbytes','bytes':'K','semantic':'nonzero selections in Native8slotcompaction'}],
          'encoding':'Native raw8bitNULstrings no codecdeclared; AD368 MS950roundtrip/controlbounds qualification separate.',
          'endianness':'h lowWORDlittleendian, cBYTE, frameu16LE; no requestroomDWORD',
          'callsites':callsites,'trigger_state':'UIentry firstvirtualSTOP; onlyconditionalobjectflag/runtimeXORislands. Actualsubmit/phase/owner/ready/authinputsunknown.',
          'evidence_level':'FixedPE staticrawtypedserializer/callsites + twoexistingservertest-clientcompactASCIIframes; zeroNativeoutbound/dynamicextendedqualification',
          'unknowns':list(boundary.values())}
    server_comparison={'source':serverpin,'static_contract':staticpin,
                       'grammar':static['new_static_grammar'],'guards':static['guards'],'publication':static['publication'],
                       'Native_match':'Threeconditionalislands serializeknowncompact/extended shapes; legacynativefivezeroBYTES equalsserverall-zerooptionsK0 acceptedtail. No newlayout/opcode correction.',
                       'source_only_domain':'name1..89/password0..4 MS950roundtrip/nocontrol; attrs&~3C=0 andpasswordbit4matchesnonempty; parsercap2..200/ordinary2..100; K0..8/exacttail/nonzero; allzerooptiontailallowed elseoptionsdomains7/2/1..127/31.',
                       'Native_limits':'Observedconditionalcapacity2..200 only, notserverordinary2..100. UpstreamUIvirtualreturns/stringbounds/optionsdomainunknown; do notturnthemintoNativevalidatedsourceauthority.',
                       'ACK':'InheritedBatch23D5success/rejectidenticalsnapshot bodies areindependentreceives, no editACK. Sendattempt doesnotprovehubcommit/readyreset/broadcast/delivery.',
                       'actualNative_or_extended_test':False}
    verification={'new_code_ranges':sum(x['kind']=='code'for x in ranges),'new_data_ranges':sum(x['kind']=='data'for x in ranges),
                  'necessary_reused_raw_ranges':len(reused),'instruction_assertions':len(assertions),
                  'conditional_CDD_callsites':3,'first_UI_virtual_boundaries':2,'saved_compact_field_witnesses':2,
                  'Native_outbound_witnesses':0,'extended_dynamic_witnesses':0,'prior_primary_pins_preserved':27,
                  'all_match':True,'server_packet_unchanged':record(ROOT/'server-packet-delta.json'),'old_PASS_replayed':False}
    outputs={'ranges.json':{'source':sources[0],'new_ranges':ranges,'necessary_reused_raw':reused},
             'instruction-witnesses.json':{'assertions':assertions},'equivalence-search-preflight.json':preflight,
             'conditional-callsite-and-source-matrix.json':{'callsites':callsites,'boundaries':boundary},
             'server-comparison.json':server_comparison,'saved-compact-field-witnesses.json':{'frames_source':framespin,'frames':saved},
             'wire-contract.json':wire,'finite-verification.json':verification}
    for n,o in outputs.items():write('CDD-outbound/'+n,o)
    report={'schema':'lito.native-CDD-conditional-outbound-boundary.v1','updated_at':stamp(),
            'status':'FINITE_CDD_CALLSITE_SERIALIZER_SEALED_UI_VIRTUAL_SUBMIT_PHASE_OWNER_CODEC_DELIVERY_OPEN',
            'sources':sources,'prior_primary_pins':prior,'existing_evidence':{'server_DD_contract':serverpin,'server_DD_static':staticpin,'saved_frames':framespin,'D5_settings_contract':record(ROOT/'D5-settings-receive-contract.json')},
            'outputs':[record(q)for q in sorted(OUT.iterdir())if q.is_file()and q.name not in ('READ-ME.txt','independent-review.json')],
            'wire':wire,'conditional_boundaries':boundary,'server_comparison':server_comparison,'verification':verification,
            'delta':{'new_wire':[],'new_opcodes':[],'wire_corrections':[],'known_corroboration':['compact/extendedCDDgrammar','Nativeconditionalcallsitec/s/h/bserializer','twoexistingservercompactASCIIrequests'],
                     'packet_modified':False,'Java_new_packets_requested':False},
            'scope':{'writes':'ownresource directory only','finite_batch_complete':True,
                     'Native_SQL_provider_GUI_process_newcapture':False,'Java_Modern_BIN_modified':False,
                     'private_external_virtual_WORLD_body_expansion':False,'old_PASS_replayed':False,
                     'complete_UI_submit_to_send_or_owner_phase_codec_delivery_ACK_qualified':False,
                     'stop':'UIvirtual54; unqualifiedpresend4C85E0; transportfixedexternal759058A0. Conditionalserializationcomplete, noactualsubmit/serverauthority.'}}
    write('CDD-outbound-contract.json',report)
    print(json.dumps({'primary':record(ROOT/'CDD-outbound-contract.json'),'verification':verification},ensure_ascii=False))

if __name__=='__main__':main()

"""Batch31 selected-record direct producer/copy/marker writes; bounded raw only."""
import sys
sys.dont_write_bytecode=True
from audit import *
OUT=ROOT/'CD2-selected-record-source'
SERVER=ROOT.parent/'lito伺服器'
def load(p):return json.loads(Path(p).read_text('utf-8'))
def main():
    prev=load(ROOT/'CD2-ready-guard-source-contract.json');prevpin=record(ROOT/'CD2-ready-guard-source-contract.json')
    assert prevpin['sha256']=='FCD54F0D67C11EC6BE3E3BDFD03C579F117A205773B67F8A29A850BB99501AB2'
    sources=[record(p)for p in (PE,BIN,ASM)];assert sources==prev['sources']
    prior=prev['prior_primary_pins']+[{**prevpin,'expected_sha256':prevpin['sha256'],'match':True}];assert len(prior)==32
    for q in prior:
        a=record(q['path']);assert a['bytes']==q['bytes']and a['sha256']==q['expected_sha256']
    OUT.mkdir(exist_ok=True);pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);ins={};ranges=[];reuse=[]
    def decode(raw,va):
        rows=[{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in cs.disasm(raw,va)]
        assert sum(len(bytes.fromhex(i['raw_hex']))for i in rows)==len(raw),(hex(va),len(raw),rows[-1]if rows else None)
        ins.update({int(i['va'],16):i for i in rows});return rows
    targets=[(0x4B3093,7,'known_CB_dispatch_call'),(0x4B3175,7,'case03_dispatch_call'),
      (0x4A7450,82,'count_c_reader_and_four_marker_pairs_first_unknown49A510'),(0x492394,24,'independent_four_marker_pairs_reset_island'),
      (0x4940D0,12,'selected_marker_query_first_unknown494120'),(0x4940DC,30,'conditional_marker2D_write_first_virtualSTOP'),
      (0x497150,29,'marker_read_consumer_first_unknown494E00'),(0x497600,170,'direct_table_xref_call_free_alias_matched_clan_copy_and_scalar_writes'),
      (0x6343B4,34,'CB_format_NUL'),(0x5025A8,2,'count_c_format_NUL'),(0x62FACD,2,'first_slot_markers_snapshot'),(0x62FADE,1,'first_slot_field3E_snapshot')]
    for va,n,label in targets:
        raw=pe.get_data(va-0x400000,n);path=OUT/f'{va:08X}-{n:X}.bin';path.write_bytes(raw)
        ranges.append({**mapping(pe,va),'bytes':n,'kind':'code'if va<0x500000 else'data','label':label,'raw_hex':raw.hex(),'output':record(path),'instructions':decode(raw,va)if va<0x500000 else[]})
    fmt=pe.get_data(0x6343B4-0x400000,34);assert fmt==b'sscchhhcccccccccddddddddddcdddddc\0'
    assert pe.get_data(0x5025A8-0x400000,2)==b'c\0'and pe.get_data(0x62FADE-0x400000,1)==b'\0'
    specs=[('lifecycle/ranges.json',0x4A7550,0x4A7550,39,'CB_entry_first_unknown4D6970','code'),
      ('lifecycle/ranges.json',0x4A7550,0x4A7577,21,'conditional_count_index_gate_alt_unknown496E80','code'),
      ('lifecycle/ranges.json',0x4A7550,0x4A758C,170,'conditional_record_stride_and_parser_arg_island','code'),
      ('priority-complete-tails.json',0x4A2A50,0x4A2A50,58,'accepted_reader_format_dispatch_prefix','code'),
      ('priority-complete-tails.json',0x4A2A50,0x4A2A8A,16,'accepted_c_oneBYTE_store_branch','code'),
      ('priority-complete-tails.json',0x4A2A50,0x4A2AE7,23,'accepted_s_branch_first_externalcopySTOP','code'),
      ('priority-complete-tails.json',0x4A2A50,0x4A2B7C,4,'accepted_c_jump_entry','data'),
      ('priority-complete-tails.json',0x4A2A50,0x4A2BA0,1,'accepted_c_charmap_entry','data'),
      ('receive-boundary/ranges.json',0x4928C0,0x492919,68,'accepted_selected_record_stride_island','code'),
      ('receive-boundary/ranges.json',0x4928C0,0x492E6B,12,'accepted_setter0_request','code'),
      ('receive-boundary/ranges.json',0x4928C0,0x492E85,30,'accepted_field3E_nonzero_setter1_request','code')]
    for index,base,va,n,label,kind in specs:
        j=load(ROOT/index);q=next(x for x in j['ranges']if int(x['va'],16)==base);pin=q['output'];assert record(pin['path'])==pin
        full=Path(pin['path']).read_bytes();offset=va-base;raw=full[offset:offset+n];assert len(raw)==n and raw==pe.get_data(va-0x400000,n)
        reuse.append({**mapping(pe,va),'bytes':n,'kind':kind,'label':label,'source_pin':pin,'source_index':record(ROOT/index),'source_offset':offset,'slice_sha256':sha(raw),'raw_hex':raw.hex(),'instructions':decode(raw,va)if kind=='code'else[],'old_PASS_replayed':False})
    # Caller stack reconstruction, stopping at the known parser invocation (no helper bodies followed).
    regs={};pushed=[]
    for va,q in sorted(ins.items()):
        if not 0x4A759B<=va<0x4A7631:continue
        if q['mnemonic']=='lea'and '[esi +'in q['operands']:
            reg,op=q['operands'].split(', ');regs[reg]={'record_offset':int(op.split('+ ')[1].split(']')[0],0)}
        if q['mnemonic']=='push':
            op=q['operands'];value=regs[op]if op in regs else{'record_offset':0}if op=='esi'else{'input_cursor':'EDI'}if op=='edi'else{'literal':int(op,0)}
            pushed.append({'at':mapping(pe,va),'value':value})
    stack=[q['value']for q in reversed(pushed)];assert len(stack)==36 and stack[:2]==[{'input_cursor':'EDI'},{'literal':0x6343B4}]
    pos=2;mapped=[]
    for ordinal,token in enumerate(fmt[:-1].decode('ascii')[:16],1):
        capacity=None
        if token=='s':capacity=stack[pos]['literal'];pos+=1
        dest=stack[pos]['record_offset'];pos+=1;mapped.append({'format_token_ordinal_excluding_opcode':ordinal,'token':token,'destination_record_offset':dest,'string_capacity_including_NUL':capacity})
    assert mapped[-1]=={'format_token_ordinal_excluding_opcode':16,'token':'c','destination_record_offset':0x3E,'string_capacity_including_NUL':None}
    expected=[(0x4B3095,'call','0x4a7550'),(0x4B3177,'call','0x4a7450'),(0x4A745F,'call','0x4a2a50'),(0x4A7464,'movsx','edx, byte ptr [ebp - 1]'),
      (0x4A746B,'mov','dword ptr [0x7607a0], edx'),(0x4A7471,'mov','dword ptr [0x76079c], 0'),(0x4A747B,'mov','eax, 0x62face'),
      (0x4A7483,'mov','byte ptr [eax - 1], cl'),(0x4A7486,'mov','byte ptr [eax], cl'),(0x4A7488,'add','eax, 0xb4'),(0x4A748D,'cmp','eax, 0x62fd9e'),(0x4A749D,'call','0x49a510'),
      (0x492394,'mov','eax, 0x62face'),(0x492399,'mov','byte ptr [eax - 1], 0xff'),(0x49239D,'mov','byte ptr [eax], 0xff'),(0x4923A0,'add','eax, 0xb4'),(0x4923A5,'cmp','eax, 0x62fd9e'),
      (0x4940D7,'call','0x494120'),(0x4940EA,'mov','byte ptr [eax*4 + 0x62facd], 0xff'),(0x4940F8,'call','dword ptr [edx]'),(0x497168,'call','0x494e00'),
      (0x497612,'mov','ebx, 0x62fad0'),(0x49761A,'lea','esi, [ebx - 0x30]'),(0x497648,'jne','0x49768f'),(0x497654,'lea','esi, [ebx - 0x14]'),
      (0x497666,'rep movsd','dword ptr es:[edi], dword ptr [esi]'),(0x497671,'rep movsb','byte ptr es:[edi], byte ptr [esi]'),
      (0x497677,'mov','word ptr [ebx + 2], ax'),(0x49767E,'mov','word ptr [ebx], cx'),(0x497685,'mov','byte ptr [ebx + 6], al'),(0x497688,'mov','word ptr [ebx + 4], cx'),
      (0x49768C,'mov','byte ptr [ebx + 7], dl'),(0x49768F,'add','ebx, 0xb4'),(0x497695,'cmp','ebx, 0x62fda0'),(0x4976A7,'ret','0x14'),
      (0x4A7572,'call','0x4d6970'),(0x4A7584,'jl','0x4a758c'),(0x4A7587,'jmp','0x496e80'),(0x4A758C,'lea','eax, [eax + eax*4]'),
      (0x4A7591,'lea','eax, [eax + eax*8]'),(0x4A7594,'lea','esi, [eax*4 + 0x62faa0]'),(0x4A75E9,'lea','edx, [esi + 0x3e]'),
      (0x4A75F0,'push','edx'),(0x4A7626,'push','0x11'),(0x4A7629,'push','0x1c'),(0x4A762B,'push','0x6343b4'),(0x4A7631,'call','0x4a2a50'),
      (0x4A2A83,'jmp','dword ptr [ecx*4 + 0x4a2b78]'),(0x4A2A8D,'mov','al, byte ptr [ebx]'),(0x4A2A93,'mov','byte ptr [edx], al'),(0x4A2AF9,'call','0x77409b70'),
      (0x492931,'lea','ebx, [edx*4 + 0x62faa0]'),(0x492E85,'mov','al, byte ptr [ebx + 0x3e]'),(0x492E8E,'push','1'),(0x492E9E,'call','0x4c3e90')]
    assertions=[]
    for va,mn,op in expected:
        q=ins[va];assert(q['mnemonic'],q['operands'])==(mn,op),(hex(va),q)
        assertions.append({**mapping(pe,va),**q,'expected_mnemonic':mn,'expected_operands':op,'match':True})
    assert pe.get_data(0x4A2BA0-0x400000,1)==b'\1'and struct.unpack('<I',pe.get_data(0x4A2B7C-0x400000,4))[0]==0x4A2A8A
    refs={n:record(ROOT/n)for n in ['CD2-ready-outbound-contract.json','CD2-ready-guard-source-contract.json','D1-E1-prestart-contract.json','SCD-member-comparison-contract.json','reconnect-source-index-contract.json','host-alias-lifecycle.json','CD1-mode-step/scope-write-correction.json']}
    refs['native_dispatch']=record(LEGACY/'analysis/NATIVE-S-DISPATCH.json')
    reportpath=SERVER/'server-character-directory-consumer-contract.json';reportpin=record(reportpath);assert reportpin['sha256']=='412748DE715782AEC88D00251DEE791A472EF7C180F905EDF43AA3A36B9464C7'
    report=load(reportpath);refs['server_character_directory']=reportpin
    for k in ['actual_JAR','complete_fields','Protocol_source_reference','finite_version_comparison']:refs['server_'+k]=report[k]
    for q in refs.values():
        a=record(q['path']);assert all(a[k]==q[k]for k in ['path','bytes','sha256'])
    fields=load(report['complete_fields']['path']);field=next(q for q in fields['fields']if q['name']=='reserved_byte');assert field['suffix_offset']==16 and field['bytes']==1 and field['bounds']=='always0'
    protocol=load(report['Protocol_source_reference']['path']);method=next(q for q in protocol['methods']if q['method']=='litoCharacterDirectoryRow');assert '.c(0)'in method['text']
    lines=ASM.read_text('utf-8',errors='replace').splitlines();queries=['62FAA0','byte_62FACD','word_62FACE','dword_62FAD0'];nav={}
    for term in queries:
        nav[term]=[{'line':i+1,'text':s}for i,s in enumerate(lines)if term.lower()in s.lower()and not s.strip().startswith(';')]
    marker_slots=[{'slot':i,'record_VA':f'{0x62FAA0+i*180:08X}','writes':[f'{0x62FACD+i*180:08X}',f'{0x62FACE+i*180:08X}'],'field3E_VA':f'{0x62FADE+i*180:08X}'}for i in range(4)]
    producer={'source_PE':sources[0],'record_base':mapping(pe,0x62FAA0),'stride_bytes':180,'target_field_offset':62,'target_field_width_bytes':1,
      'S_CB':{'direction':'S','opcode':'CB','dispatch_call':mapping(pe,0x4B3095),'handler':mapping(pe,0x4A7550),'known_existing_row_format_after_opcode':fmt[:-1].decode(),'format':mapping(pe,0x6343B4),
        'handler_first_unknown':{'at':mapping(pe,0x4A7572),'target':'004D6970'},'alternative_first_unknown':'4A7587 tail496E80','qualified_argument_island':'4A758C..4A7636; indexDWORD76079C<countDWORD7607A0, inputECX copiedEDI then++ skipsopcode; ESI=62FAA0+index*180. No localcapacity/phase/owner qualification.',
        'parser_call':mapping(pe,0x4A7631),'field_pointer':{'lea':mapping(pe,0x4A75E9),'push':mapping(pe,0x4A75F0),'dest':'ESI+3E'},'field_token_ordinal_after_opcode_including_strings':16,'numeric_field_ordinal':14,'numeric_suffix_offset_bytes':16,
        'typed_store':'c dispatchcharmap1→4A2A8A;4A2A8D readsBYTE;4A2A93 writesBYTE toargumentpointer. Conditional only, earlierstwo strings hitexternal77409B70 then priorseek774095E0 unknownSTOP; no runtimeparsecompletion orcodec qualification.',
        'row_after_opcode_arg_map_through_field16':mapped,'caller_stack_pushes':36,'other_tail_fields':'Outside selectedfield qualification; remainingdestinations/extra frame slots/validation not requalified; useacceptedserver wire contract, not wholeNativeargumentcompletion.',
        'AD368_reference':{'report':reportpin,'fields':report['complete_fields'],'field':field,'Protocol_method':method,'Protocol_reference':report['Protocol_source_reference'],'actual_JAR':report['actual_JAR'],
          'level':'accepted preexisting AD368 producer/source/finiteIL equality reference adopted asdata; reservedbyte always0. Nativeconditionaldestination mapping new; no runtime/matrix/golden replay.'},
        'body_lengths':'B=3+N+M aftertwoMS950CStrings; targetoffset B+16; body82+N+M,frame84+N+M (u16LEtotallength); numeric suffix79. N1..27/M0..16 producerpolicy; Native strings capacities28/17 inclNUL,externalcopy/seek unknown.'},
      'S03_marker_reset':{'direction':'S','opcode':'03','dispatch_call':mapping(pe,0x4B3177),'handler':mapping(pe,0x4A7450),'format':'c afteropcode','body_bytes':2,'frame_bytes':4,
        'native_count':'readBYTE local thenMOVSX signedDWORD7607A0; resetindex76079C=0. Nativeboundedprefixhasno0..8guard; serverAD368policy0..8 separate.',
        'clear_loop':'4A747B..4A7494: writesFF atrecord+2D/+2E for4slots(start62FACE,stepB4,stop62FD9E). No+3Eclear orwhole180bytezeroing.',
        'marker_slots':marker_slots,'first_following_unknown':'4A749D→49A510 incountzero branch;countnonzero alternative4A74E0 notexpanded','capacity_limit':'Fourmarkerpairscan isnotproofcompleteNativecapacity4 or8. Acceptedserver8-rowpolicy/annotation cannot establish thisfixedPE storage/index safety.'},
      'other_direct_marker_islands':[{'at':mapping(pe,0x492394),'effect':'same4pairsFF at+2D/+2E;upstreamUI/resetinvoker unknown;no+3Eclear'},
        {'at':mapping(pe,0x4940EA),'effect':'conditionalselectedrecord+2D=FF only;query4940D7→494120 firstunknown;afterreturnindependentmarkerisland virtual4940F8 [EDX]STOP'},
        {'at':mapping(pe,0x497150),'effect':'reads+2D/+2E markerpair;497168→494E00 firstunknownSTOP;notaclear/write proof'}],
      'direct_copy_update':{'at':mapping(pe,0x497600),'discovery':'directtableaddressxref62FAD0;actualECXsourcecaller unknown;bodyindependenttablexref, no callee traversal','code_bytes':170,'calls_in_body':0,
        'record':'EBX=62FAD0+slot*180=recordbase+30;compareinputECXalias torecordbase NUL bytewise;looptillEBX62FDA0,4slots;mayupdateeachmatchingalias,notphysicalID/uniqueness proof',
        'copy':'EDXnewclanpointer→localEBP-8;aliasmatch strlen+NUL countedwithREPNE SCASB,copytoEBX-14=record+1C usingREP MOVSD/MOVSB. No localcapacityclamp;validsource/destmemory required.',
        'scalar_stores':[{'record_offset':0x30,'bytes':2,'arg':'EBP+8'},{'record_offset':0x32,'bytes':2,'arg':'EBP+C'},{'record_offset':0x34,'bytes':2,'arg':'EBP+10'},{'record_offset':0x36,'bytes':1,'arg':'EBP+14'},{'record_offset':0x37,'bytes':1,'arg':'EBP+18 lowBYTE'}],
        'field3E':'Scalarsstop+37. StringcopyincludesNUL,starts+1C;strlen>=34 canreach+3E. UnderAD368clan<=16 policy copywouldstop<=2C andpreserve3E;othercaller/inputbounds unqualified, no universalpreservation/clear claim.',
        'trigger_phase':'aliasmatched localcallargs only;caller/opcode/epoch/phase unknown andnotexpanded;noNativeinvocation/class/owner/readylabel'},
      'snapshot':{'first_record_markers_hex':pe.get_data(0x62FACD-0x400000,2).hex(),'first_record_field3E_hex':'00','level':'fixedPEsnapshot only,notloader/startup/currentrow/clearedafterselection'},
      'lifetime':'KnownS_CBconditionalwriteoverwrites+3E;knownmarkerresetsretainit;variablecopymayoverwriteifunbounded. Alias/bulk/otherrecordproducers,actualcountindex safety andcurrentselectedepoch remainopen.'}
    # Keep this handoff concise; details and raw pins are in primary/source contract.
    dependencies={'references':{'Batch29':refs['CD2-ready-outbound-contract.json'],'Batch30':prevpin,'Batch31_primary':'../CD2-selected-record-source-contract.json','D1_E1':refs['D1-E1-prestart-contract.json']},
      'rows':[{'stage':'localrecord','known':'S_CB token16 c→selected180byte record+3E;AD368reservedbyte0 producer;markerresetdoesnotclear3E','need':'earlierstrings/helpers/currentselectedrow/epoch,otherclancopybounds,recordindex/storage safety','authority':'none'},
        {'stage':'localguard','known':'Batch30 localstartupBYTE64AF9D;selectionrequestsetter0 then+3Enonzero setter1;readyguardflag0 ordecoded0→D201','need':'validprotectedobject/helperreturns/currentvalue;fieldorflag cannotbeowner/mode/readypermission','authority':'conditional localbranch only'},
        {'stage':'userintent','known':'Batch29 sevenconditionalD2+statecalls;currentopcodeonlycallsiteunknown','need':'authorizeduserintent/currenttransport/room/join;actualNativeUIeventnotqualified','authority':'explicituserauthorization independent ofrecord'},
        {'stage':'servercommit','known':'AD368readyhandlerchecksandmembershipcommit beforepublish;C_D2states0/1/2 vsownerseparateopcodeonly','need':'servercurrentepoch/session/room/ownerconditions;485Fsettingsnetworknotready-machinevalidation','authority':'serverowned,notlocalrecord'},
        {'stage':'D1_E1_receipts','known':'D1projectedaliasstate;E1countdown/cancel receipts scopedperpeer;partialsend/racesopen','need':'currentepoch/room/joingeneration/qualifiedalias;noexpiry→WORLDgrant','authority':'receipt scope only'},
        {'stage':'GUI','known':'noNative/GUIrun;callback/constructor/virtual/privategaps retained','need':'actualUIevent/controlacceptance separatelyqualified','authority':'none'}],
      'adoption':'Modern mayretainserverrow reservedbyteasmetadata underaccepted AD368 row contract; mustnot derive ready/owner permission orNativeprotectedobject fromit. Batch29/30 SHAunchanged;no runtime/GUIgrant.',
      'remaining_necessary_work':'Runtime/helper/selectedepoch/index/storage/copycaller boundaries remainopen;no autonomousunknownbody orNative/JavaSQLrun requested. CurrentModernexplicitreadywriter/receipt acceptance belongs toitsownauthorized integration.'}
    unknowns=['FirstunknownCB4D6970;rowstrings77409B70/774095E0;countzero49A510;selectedmarker494120/virtual[EDX];markerconsumer494E00 allSTOP,no bodiesopened.',
      '+3E isunnamedNativeBYTE mappedtoAD368reservedbyte0,notowner/mode/ready/classqualification. Conditionalwriteonly,notruntimeproof.',
      'Fourmarkerreset slots !=completeNativecapacity;AD368count0..8producerpolicy doesnotqualify nativeindex/storage safe bounds orzero/multiGUI.',
      'Call-freeclancopy canoverwrite+3E ifstrlen>=34;boundedAD368clanpolicy only,othercaller/source/lifetime unknown. No blanket+3Eclear/preserve/uniquematch guarantee.',
      'No newwire/opcode/fieldcorrection;knownCB/03typedmapping补證. Otherrowtailargumentdestinations/codec/fullparsecompletion outsidequalification.',
      'Userintent/servercommit/D1/E1/GUI/WORLDseparate;Batch29opcodeonly/UI/transport andBatch30runtimeprotectedselectorunknown retained;noJavaSQL/Native/GUI oroldPASS.',
      'Prior externalpyc cleanup automaticreviewrejection retained,no retry/reframe/alternate/delegation;onlyownedROOTwrites,Python-B/dont_write_bytecode.']
    write('CD2-selected-record-source/ranges.json',{'source':sources[0],'new_ranges':ranges,'necessary_reused_raw':reuse,'instruction_assertions':assertions})
    write('CD2-selected-record-source/direct-table-xrefs.json',{'source_ASM':sources[2],'navigation_only':True,'literal_queries':nav,'scope':'Directliteral/symbolnavigationonly;doesnotclosealias/bulk/privatehelper/caller/runtimewriters.'})
    write('CD2-selected-record-source/producer-copy-clear-and-field-contract.json',producer)
    write('CD2-selected-record-source/Modern-ready-dependency-matrix.json',dependencies)
    write('CD2-selected-record-source/known-evidence-delta.json',{'classification':'knownS_CB/03producer-field/nativeinternalmapping补證','new_wire_differences':0,'new_opcodes':0,'wire_corrections':0,'server_packet_delta_modified':False,'field':{'direction':'S','opcode':'CB','token_ordinal':16,'type':'u8','bytes':1,'body_offset':'3+N+M+16','encoding':'rawBYTE;N/M MS950perAD368producer','trigger':'acceptedordinarydirectoryrow;Nativeconditionalindex<countargumentisland','handler':mapping(pe,0x4A7550),'source_SHA256':sources[0]['sha256'],'evidence_level':'conditionalNativepointer/BYTEstore plusacceptedAD368c0producer,notexecution/GUI/owner','unknowns':unknowns}})
    packet=record(ROOT/'server-packet-delta.json');assert packet==prev['verification']['server_packet_unchanged']
    primary={'schema':'finite-native-selected-character-record3E-source-v1','updated_at':stamp(),'status':'FIELD_PRODUCER_COPY_MARKER_BOUNDS_SEALED_SELECTED_RUNTIME_UI_AUTHORITY_OPEN','sources':sources,'prior_primary_pins':prior,'existing_evidence':refs,'outputs':[record(q)for q in sorted(OUT.iterdir())if q.is_file()],
      'producer_copy_clear':producer,'Modern_ready_dependencies':dependencies,'unknowns':unknowns,'preflight':'Priorlifecycle/selectedcallback/parserrawandacceptedserverCBfieldcontract reused. Newonlyminimaldispatch/count/marker/tablexrefcopy/snapshot bytes;no unknowncallee/private/externalbody oroldPASS/generator/review replay.',
      'verification':{'new_code_ranges':8,'new_data_ranges':4,'necessary_reused_raw_ranges':11,'instruction_assertions':len(assertions),'prior_primary_pins_preserved':32,'new_capture_frames':0,'new_frame_rows_adopted':0,'new_wire_differences':0,'new_opcodes':0,'wire_corrections':0,'unknown_callee_bodies_expanded':0,'Native_GUI_input_injection_SQL_provider_JVM_Java_Modern_launcher_BIN_actions':0,'server_packet_unchanged':packet,'scope_write_correction':refs['CD1-mode-step/scope-write-correction.json'],'cache_cleanup_completed':False}}
    write('CD2-selected-record-source-contract.json',primary);print(json.dumps({'primary':record(ROOT/'CD2-selected-record-source-contract.json'),'verification':primary['verification']},ensure_ascii=False))
if __name__=='__main__':main()

"""Batch32 finite count/cursor/selected-index and Modern adaptation table."""
import sys
sys.dont_write_bytecode=True
from audit import *
OUT=ROOT/'CD2-directory-index-storage'
def load(p):return json.loads(Path(p).read_text('utf-8'))
def main():
    prev=load(ROOT/'CD2-selected-record-source-contract.json');prevpin=record(ROOT/'CD2-selected-record-source-contract.json')
    assert prevpin['sha256']=='9238357B9079A14F91132143E2FDED79B0F6F0733DAC716A6925AD7C44F19EF7'
    sources=[record(p)for p in(PE,BIN,ASM)];assert sources==prev['sources']
    prior=prev['prior_primary_pins']+[{**prevpin,'expected_sha256':prevpin['sha256'],'match':True}];assert len(prior)==33
    for q in prior:
        a=record(q['path']);assert a['bytes']==q['bytes']and a['sha256']==q['expected_sha256']
    OUT.mkdir(exist_ok=True);pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);ins={};reuse=[]
    va=0x76079C;raw=pe.get_data(va-0x400000,8);assert raw==b'\0'*8;path=OUT/'0076079C-8.bin';path.write_bytes(raw)
    new=[{**mapping(pe,va),'kind':'data','bytes':8,'label':'fixed_cursor_count_snapshot_only','raw_hex':raw.hex(),'output':record(path),'instructions':[]}]
    specs=[('CD2-selected-record-source/ranges.json',0x4A7450,0x4A7450,82,'countBYTE_signed_store_index_reset_markerloop_firstunknown'),
      ('lifecycle/ranges.json',0x4A7550,0x4A7550,39,'CBentry_firstunknown4D6970'),
      ('lifecycle/ranges.json',0x4A7550,0x4A7577,21,'global_signed_index_count_gate_altSTOP496E80'),
      ('lifecycle/ranges.json',0x4A7550,0x4A758C,15,'conditional_index_stride180_recordbase'),
      ('lifecycle/ranges.json',0x4A7550,0x4A763C,16,'postparser_recordargs_firstunknown46EA30'),
      ('lifecycle/ranges.json',0x4A7550,0x4A764C,52,'independent_cursor_reread_increment_compare_clear_firstunknown49A510'),
      ('receive-boundary/ranges.json',0x4928C0,0x49290D,12,'selectedquery_ECX0_EDX3_firstunknown494120'),
      ('receive-boundary/ranges.json',0x4928C0,0x492919,68,'conditional_queryresult_minus1gate_stride_markerchecks_firstunknown49A510')]
    for index,base,va,n,label in specs:
        j=load(ROOT/index);q=next(x for x in j.get('ranges',j.get('new_ranges'))if int(x['va'],16)==base);pin=q['output'];assert record(pin['path'])==pin
        raw=Path(pin['path']).read_bytes()[va-base:va-base+n];assert len(raw)==n and raw==pe.get_data(va-0x400000,n)
        rows=[{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in cs.disasm(raw,va)]
        assert sum(len(bytes.fromhex(i['raw_hex']))for i in rows)==n;ins.update({int(i['va'],16):i for i in rows})
        reuse.append({**mapping(pe,va),'kind':'code','bytes':n,'label':label,'source_pin':pin,'source_index':record(ROOT/index),'source_offset':va-base,'slice_sha256':sha(raw),'raw_hex':raw.hex(),'instructions':rows,'old_PASS_replayed':False})
    expected=[(0x4A7464,'movsx','edx, byte ptr [ebp - 1]'),(0x4A746B,'mov','dword ptr [0x7607a0], edx'),(0x4A7471,'mov','dword ptr [0x76079c], 0'),
      (0x4A7572,'call','0x4d6970'),(0x4A7577,'mov','eax, dword ptr [0x76079c]'),(0x4A757C,'mov','ecx, dword ptr [0x7607a0]'),(0x4A7582,'cmp','eax, ecx'),
      (0x4A7584,'jl','0x4a758c'),(0x4A7587,'jmp','0x496e80'),(0x4A758C,'lea','eax, [eax + eax*4]'),(0x4A7591,'lea','eax, [eax + eax*8]'),(0x4A7594,'lea','esi, [eax*4 + 0x62faa0]'),
      (0x4A763C,'lea','edx, [esi + 0x95]'),(0x4A7642,'mov','ecx, esi'),(0x4A7645,'xor','edx, edx'),(0x4A7647,'call','0x46ea30'),
      (0x4A764C,'mov','eax, dword ptr [0x76079c]'),(0x4A7651,'inc','eax'),(0x4A7652,'mov','ecx, dword ptr [0x7607a0]'),(0x4A7658,'mov','dword ptr [0x76079c], eax'),
      (0x4A765D,'cmp','eax, ecx'),(0x4A7660,'jl','0x4a76c1'),(0x4A7667,'mov','dword ptr [0x7607a0], 0'),(0x4A7671,'mov','dword ptr [0x76079c], 0'),(0x4A767B,'call','0x49a510'),
      (0x49290D,'mov','edx, 3'),(0x492912,'xor','ecx, ecx'),(0x492914,'call','0x494120'),(0x492919,'cmp','eax, -1'),(0x49291C,'je','0x492f81'),
      (0x492922,'lea','eax, [eax + eax*4]'),(0x492925,'lea','edx, [eax + eax*8]'),(0x492928,'mov','al, byte ptr [edx*4 + 0x62facd]'),(0x492931,'lea','ebx, [edx*4 + 0x62faa0]'),
      (0x492958,'call','0x49a510')]
    assertions=[]
    for va,mn,op in expected:
        q=ins[va];assert(q['mnemonic'],q['operands'])==(mn,op),(hex(va),q)
        assertions.append({**mapping(pe,va),**q,'expected_mnemonic':mn,'expected_operands':op,'match':True})
    lines=ASM.read_text('utf-8',errors='replace').splitlines();nav={}
    for term in ['dword_76079C','dword_7607A0']:
        nav[term]=[{'line':i+1,'text':s}for i,s in enumerate(lines)if term.lower()in s.lower()and not s.strip().startswith(';')]
    assert len(nav['dword_76079C'])==6 and len(nav['dword_7607A0'])==5
    refs={n:record(ROOT/n)for n in ['CD2-selected-record-source-contract.json','CD2-ready-guard-source-contract.json','CD2-ready-outbound-contract.json','D1-E1-prestart-contract.json','CD1-mode-step/scope-write-correction.json']}
    for k in ['server_character_directory','server_complete_fields','server_Protocol_source_reference','server_actual_JAR']:refs[k]=prev['existing_evidence'][k]
    for q in refs.values():
        a=record(q['path']);assert all(a[k]==q[k]for k in ['path','bytes','sha256'])
    report=load(refs['server_character_directory']['path'])
    spans=[]
    for i in range(8):
        start=0x62FAA0+i*180;end=start+179;spans.append({'ordinal':i,'start':mapping(pe,start),'end':mapping(pe,end),'bytes':180,'raw_backed_endpoints':mapping(pe,start)['raw_backed']and mapping(pe,end)['raw_backed'],'runtime_array_capacity_or_validity':False})
    contract={'source_PE':sources[0],'count':{'at':mapping(pe,0x7607A0),'width_bytes':4,'source':'S03 c→localBYTE→MOVSX signedDWORD at4A7464,store4A746B','native_raw_byte_domain':'0..127 maps0..127;128..255 maps-128..-1;no local0..8 validation in82byteprefix','writes':'S03 replacescount;CBcompletion4A7667 sets0','initial_snapshot_hex':'00000000','meaning':'localexpectedrowcounter only,notepoch/serverowner/selectedcharacter grant'},
      'cursor':{'at':mapping(pe,0x76079C),'width_bytes':4,'writes':'S03 4A7471 zero;posthelper4A7651 INC rereadglobal and4A7658 store;completion4A7671 zero','reads':'CBindex4A7577 andposthelper4A764C','gate':'CBsignedDWORD index<count at4A7582/84. No nonnegative guard orknownstoragebound inthisconditionalisland;alternate496E80 unknownSTOP.',
        'post_helper':'Afterrowparserstringsunknown,4A7647→46EA30 firstunknown withECXrecord/EDX0/stackrecord+95. Laterislandrereads globalcursor/count,not frozenrowindex. Ifreturned andstable:increment,ifnext<count return;elsestoreboth0 before49A510 firstunknown.',
        'completion':'Localcount/cursor0 andUIcallrequestnotwireACK,atomicdirectorycompletion,storedphysicalselection orGUIacceptance. Nested/reentry/helperwrites/partialparse unknown.'},
      'selected_index':{'query':mapping(pe,0x492914),'inputs':{'ECX':0,'EDX':3},'first_unknown':'494120 privatebodynotopened;inputsemanticsandEAXreturndomain unknown','output':'EAX conditional aftersuccessfulreturn;cmp-1 rejects onlyFFFFFFFF. No index<count orindex>=0 check in68byteisland;markerchecks+2DFF/+2EFF/+37nonzero notownership/bounds.',
        'address':'32bitLEAs*5,*9,*4:EBX=62FAA0+(EAX*180 mod2^32) mod2^32;flags/helperreturn maychange source. Valuesotherthan-1 notqualified safe.',
        'comparison':'Receivecursor76079C distinctfromprivatequeryEAX. NeitherisphysicalID/sourceID/currentroom/readypermission. ECX0/EDX3 requestisnotproof4slotcapacity;othermarkerqueryECX0/EDX4 inBatch31 likewise.',
        'following_unknown':'492958→49A510;do nottraceUIlookup/constructor/ownergraph/privatebody'},
      'storage':{'base':mapping(pe,0x62FAA0),'stride_bytes':180,'raw_endpoint_examples_for_server_policy_8':spans,'fixed_raw_span_only':'62FAA0..63003F (1440bytes) rawbacked inonefixedPEsection. Backingmappedbytes isnotarrayallocation/initializedrow/ownership/runtimecapacity.',
        'known_marker_span':'Batch31 exactwrites4pairs+2D/+2E inrecords0..3,notwhole180zeroing;stop62FD9E. Aliasupdate62FAD0..62FDA0 also4-slotloop,notuniversalstoragecapacity.',
        'capacity':'Neither4nor8Nativecapacity established;no targetarrayend symbol/allocatedextent/safequerydomain/runtimeownership qualification. Do not requestserverpolicyreduction.',
        'scope':'No new1440byteextract/heap/probe;onlysourcePE sectionmapping endpointmetadata andsealedraw slices.'},
      'version':'FixedPE4271D92F/rawsnapshot only. ServeracceptedAD368 directorycount0..8 /CBrows policy separately;otherJARs/nativeGUI/runtime notnewlyqualified.',
      'wire':'KnownS03u8count body2/frame4 andCB79bytesnumericsuffix+twoMS950CStrings unchanged;no extraendmarker orindexwirefield;no newfield/opcode/correction.'}
    safety={'evidence':{'Batch29':refs['CD2-ready-outbound-contract.json'],'Batch30':refs['CD2-ready-guard-source-contract.json'],'Batch31':prevpin,'server_AD368':refs['server_character_directory']},'qualification':'ProposedModernstrictadaptation table, notimplemented/testedModern orprooforiginalNativebehavior.',
      'rows':[{'concern':'count decode','Modern_rule':'Decodewireu8;validate0..8 underpinnedAD368producerpolicy;boundedownedlist allocation. PreserveexpectedCount independently ofNative signedglobals/completionzero. No reducingserverpolicy basedonfourmarkers.','Native_limit':'MOVSXsignedcount/no local0..8 check;runtime/storagecapacityunknown'},
        {'concern':'row consistency','Modern_rule':'AcceptCBonlywithactivecurrentdirectorybuilder andsame transport/source/directorygeneration;parseboundedCStrings/exact79bytesuffix/exactbodyend,commitrowonlyaftersuccess;require received<expectedCount.','Native_limit':'private/externalstrings/UIhelpers;partialstorespossible;globalcursorrereadafterunknownhelper'},
        {'concern':'completion','Modern_rule':'CompleteexactlyNvalidatedrows;N0emptycomplete/no defaultrow0;extraCB refuse asout-of-sequence;missingrows remainincomplete. No guessedS04/S05endmarker orlocaltimercompletion.','Native_limit':'count/cursorclearedafterincrement,notwireACK orwholeGUIcompletion'},
        {'concern':'selected row','Modern_rule':'Selectonlyfromcompleteownedimmutablecurrentrows:0<=index<actualRows andmatchingdirectorygeneration;resolve server-issuedname/currentcanonicalsource. Revalidatehandleatintentdispatch;do notacceptunknownNativeEAX asauthority.','Native_limit':'494120 inputs0/3 unknown;only-1check before32bitstride/markerreads;no certifiedrange orphysicalID'},
        {'concern':'epoch invalidation','Modern_rule':'Invalidatebuilder andpendingrow/UIselectionhandles onownedtransport reconnect/reauth/sourcegeneration change ornewdirectory S03 snapshot. Discardlateoldgenerationreceipts;localdirectoryinvalidationdoesnotassertserverphysicalselection/readywascleared.','Native_limit':'noepoch/generationfields inS03/CB andglobalcountersnotepoch;originalreset/invalidation semantics incomplete'},
        {'concern':'ready intent','Modern_rule':'Useexplicituserintent andcurrentownedroom/join/session eligibility separately. CBreserved3E/nativeindex/markerflag/protectedselector cannotauthorize ready;D1/E1receiptandGUIacceptance remainindependent.','Native_limit':'Batch29opcodeonly/UI/transport andBatch30protectedruntime/helper gaps retained'}],
      'codec_identity':'MS950/bounds deriveacceptedserverversion. NeverderivephysicalcharacterObjectId/sourceId fromindex/displayalias;servercanonicalsource plusmatchedobjecttuple remainsserverowned.',
      'remaining_necessary_work':'Modernownauthorizedintegrationmustverifybuilder/selection/epoch refusalpolicy. Nativequerydomain/helpercompletion/storage/GUI remainopen;no unknownbody/run orotherchatmessage requested.'}
    write('CD2-directory-index-storage/ranges.json',{'source':sources[0],'new_ranges':new,'necessary_reused_raw':reuse,'instruction_assertions':assertions})
    write('CD2-directory-index-storage/direct-counter-xrefs.json',{'source_ASM':sources[2],'navigation_only':True,'literal_queries':nav,'scope':'Exactnamedglobalrefs only;notalias/privatecallee/fullprogramwriteclosure.'})
    write('CD2-directory-index-storage/count-cursor-selected-index-contract.json',contract)
    write('CD2-directory-index-storage/Modern-safe-directory-adaptation-table.json',safety)
    packet=record(ROOT/'server-packet-delta.json');assert packet==prev['verification']['server_packet_unchanged']
    primary={'schema':'finite-native-directory-count-index-storage-v1','updated_at':stamp(),'status':'COUNTER_SELECTED_QUERY_AND_RAW_MAPPING_SEALED_RUNTIME_STORAGE_GUI_AUTHORITY_OPEN','sources':sources,'prior_primary_pins':prior,'existing_evidence':refs,'outputs':[record(q)for q in sorted(OUT.iterdir())if q.is_file()],'contract':contract,'Modern_safe_adaptation':safety,
      'preflight':'Allnecessarycodealreadysealed inBatch31/lifecycle/receive-boundary. No newcodebody needed;reuse8minimal slices,only8bytecounterrawsnapshot added;newnamedxref/conditionalcounteranalysis,notoldPASS/generator/review replay.',
      'unknowns':['494120selectedquerydomain/ECX0EDX3semanticsunknownSTOP;do notinfer4or8bound.','4D6970,496E80,rowstringexternal,46EA30,49A510 firstunknownSTOP;cursorreread afterhelper isnotatomicfrozenrowtransaction.','Rawbacked1440byteendpoint span andfourmarkerloopnotcapacity/storage/runtime proof;no server8policychange.','Moderntable isstrictadaptationproposal,notoriginalNativeproof/liveModernacceptance;epoch/currentrow/owner/readydistinct.','Existingexternalpyc policycleanuprejectionopen;no retry/reframe/alternate/delegation.'],
      'verification':{'new_code_ranges':0,'new_data_ranges':1,'necessary_reused_raw_ranges':8,'instruction_assertions':len(assertions),'prior_primary_pins_preserved':33,'new_capture_frames':0,'new_frame_rows_adopted':0,'new_wire_differences':0,'new_opcodes':0,'wire_corrections':0,'server_8row_policy_reduction_requested':False,'unknown_callee_bodies_expanded':0,'Native_GUI_Java_SQL_Modern_launcher_BIN_actions':0,'server_packet_unchanged':packet,'scope_write_correction':refs['CD1-mode-step/scope-write-correction.json'],'cache_cleanup_completed':False}}
    write('CD2-directory-index-storage-contract.json',primary);print(json.dumps({'primary':record(ROOT/'CD2-directory-index-storage-contract.json'),'verification':primary['verification']},ensure_ascii=False))
if __name__=='__main__':main()

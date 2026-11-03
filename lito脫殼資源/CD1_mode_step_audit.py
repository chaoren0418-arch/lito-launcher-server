"""Batch28 finite mode-step caller/source/IL boundary; C7 version supplement."""
from audit import *
import zipfile,importlib.util,sys
OUT=ROOT/'CD1-mode-step'
SERVER=ROOT.parent/'lito伺服器'
MAPROOT=Path(r'D:\lito-dev-runtime\20261003-supervision\server\room-map-selection-contract-65877892a76c4333a56190888c47d6fc')
def load(n):return json.loads((ROOT/n).read_text('utf-8'))
def classreader():
    sys.dont_write_bytecode=True
    path=MAPROOT/'source/ClassFileReadOnly.py'
    spec=importlib.util.spec_from_file_location('bounded_classreader',path);mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)
    return mod,record(path)
def main():
    OUT.mkdir(exist_ok=True)
    prev=load('CD1-map-outbound-contract.json');prevpin=record(ROOT/'CD1-map-outbound-contract.json')
    assert prevpin['sha256']=='9A2BA5AB692E92088E5B9BCA3C45A449EB04D7B3D2AD7AACB88EE7AAF43D598D'
    sources=[record(p) for p in (PE,BIN,ASM)];assert sources==prev['sources']
    prior=prev['prior_primary_pins']+[{**prevpin,'expected_sha256':prevpin['sha256'],'match':True}];assert len(prior)==29
    for q in prior:
        a=record(q['path']);assert a['bytes']==q['bytes'] and a['sha256']==q['expected_sha256']
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);ins={};owners={};ranges=[];reuse=[]
    def decode(raw,va,pin):
        rows=[{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in cs.disasm(raw,va)]
        assert sum(len(bytes.fromhex(i['raw_hex']))for i in rows)==len(raw)
        for i in rows:ins[int(i['va'],16)]=i;owners[int(i['va'],16)]=pin
        return rows
    for va,n,label in [
      (0x46D07E,15,'first_allocation_STOP_4F118C'),(0x46D0D4,7,'second_allocation_STOP_4F118C'),
      (0x46D09E,33,'conditional_control0_callback_ctor_args'),(0x46D0EC,33,'conditional_control1_callback_ctor_args'),
      (0x4154E0,35,'ctor_prefix_forwards_callback_control_STOP414910'),
      (0x46D0C3,17,'conditional_parent_child0_insert_call_STOP41B3F0'),
      (0x46D111,17,'conditional_parent_child1_insert_call_STOP41B3F0')]:
        raw=pe.get_data(va-0x400000,n);path=OUT/f'{va:08X}-{n:X}.bin';path.write_bytes(raw);pin=record(path)
        ranges.append({**mapping(pe,va),'bytes':n,'kind':'code','label':label,'raw_hex':raw.hex(),'output':pin,'instructions':decode(raw,va,pin)})
    data=load('CD1-map-outbound/ranges.json')
    for va,n in [(0x46B7D0,52),(0x46B9E2,26),(0x5028CC,4)]:
        q=next(x for x in data['new_ranges']if int(x['va'],16)==va);pin=q['output'];assert record(pin['path'])==pin
        raw=Path(pin['path']).read_bytes();assert raw==pe.get_data(va-0x400000,n)
        reuse.append({**mapping(pe,va),'used_bytes':n,'kind':q['kind'],'source_pin':pin,'index':record(ROOT/'CD1-map-outbound/ranges.json'),
          'raw_hex':raw.hex(),'instructions':decode(raw,va,pin)if q['kind']=='code'else[],'old_PASS_replayed':False})
    expected=[(0x46D088,'call','0x4f118c'),(0x46D0D6,'call','0x4f118c'),
      (0x46D0A5,'push','0'),(0x46D0AC,'push','0x46b7d0'),(0x46D0BA,'call','0x4154e0'),
      (0x46D0F3,'push','1'),(0x46D0FA,'push','0x46b7d0'),(0x46D108,'call','0x4154e0'),
      (0x4154E3,'mov','eax, dword ptr [ebp + 0x18]'),(0x4154EC,'mov','ecx, dword ptr [ebp + 0x14]'),
      (0x4154FE,'call','0x414910'),(0x46D0CF,'call','0x41b3f0'),(0x46D11D,'call','0x41b3f0'),
      (0x46B7F0,'je','0x46b9e6'),(0x46B7F7,'je','0x46b9e2'),
      (0x46B9E2,'push','1'),(0x46B9E6,'push','2'),(0x46B9E8,'push','2'),
      (0x46B9EA,'push','0xd1'),(0x46B9EF,'push','0x5028cc'),(0x46B9F4,'call','0x4c6ae0')]
    assertions=[]
    for va,mn,op in expected:
        assert (ins[va]['mnemonic'],ins[va]['operands'])==(mn,op),(hex(va),ins[va])
        assertions.append({**mapping(pe,va),**ins[va],'range':owners[va],'fixed_PE_sha256':sources[0]['sha256'],
          'expected_mnemonic':mn,'expected_operands':op,'match':True})
    oldmap=json.loads((SERVER/'server-ordinary-room-map-selection-contract.json').read_text('utf-8'))
    evpin=oldmap['evidence'];assert record(evpin['path'])==evpin
    ev=json.loads(Path(evpin['path']).read_text('utf-8'));pins=ev['selected_reference_pins']
    netpath=SERVER/'server-ordinary-room-map-network.json';netpin=record(netpath)
    assert netpin['sha256']=='3BBAA920A2B379B7562A38C3FF2CADC8842B98F7033C3A5C103174F4A28A8545'
    net=json.loads(netpath.read_text('utf-8'));mod,readerpin=classreader()
    existing={n:record(ROOT/n) for n in ['CD1-map-outbound-contract.json','CDD-outbound-contract.json','D5-mode-map-receive-contract.json','D5-settings-receive-contract.json']}
    existing.update(old_server_map_report=record(SERVER/'server-ordinary-room-map-selection-contract.json'),
      old_server_map_evidence=evpin,C7_network_report=netpin,C7_frames=net['new_network']['frames'],
      C7_poststates=net['poststates'],C7_wire_state=net['wire_state'],C7_product_classes=net['product_classes'],
      class_readonly_parser=readerpin,AD368_JAR=pins['actual-JAR'],C7_JAR=net['actual_JAR'])
    selectedpins=['actual-CS-source','actual-CS-archived-IL','actual-GameHub-archived-IL','actual-Protocol-archived-IL',
      'Protocol-source-reference','GameHub-source-reference','BattleRules-source-reference']
    existing.update({n:pins[n]for n in selectedpins})
    for q in existing.values():assert record(q['path'])==q
    methods={'Protocol':['litoGameInfoRequest','litoRoomMode','litoRoomMap'],
      'Protocol$LitoModeStepDirection':['fromWire','<clinit>'],'BattleRules$Mode':['stepNative'],
      'ClientSession':['gameInfo','changeRoomMode','sendGameInfoSnapshot','resetRoomReadyAfterSettingsChange'],
      'GameHub':['updateSettings'],'BattleRules$1':['<clinit>']}
    bytecode=[]
    with zipfile.ZipFile(pins['actual-JAR']['path'])as oz,zipfile.ZipFile(net['actual_JAR']['path'])as nz:
        for cl,names in methods.items():
            entry='local/lito381/server/'+cl+'.class';a=oz.read(entry);b=nz.read(entry);ca=mod.parse_class(a);cb=mod.parse_class(b)
            rows=[]
            for name in names:
                ma=next(x for x in ca['methods']if x['name']==name);mb=next(x for x in cb['methods']if x['name']==name)
                assert ma['descriptor']==mb['descriptor'] and ma['code_hex']==mb['code_hex'] and ma['instructions']==mb['instructions']
                raw=bytes.fromhex(ma['code_hex']);start=0;end=len(raw)
                if cl=='ClientSession' and name=='changeRoomMode':end=256 # stop before bot/matchmaking continuation
                if cl=='BattleRules$1':
                    start=next(x['offset']for x in ma['instructions']if 'Protocol$LitoModeStepDirection.values:' in x.get('cp_symbol',''))
                rows.append({'name':name,'descriptor':ma['descriptor'],'full_method_bytes':len(raw),'full_code_sha256':sha(raw),
                  'AD368_C7_exact_code_and_CP_symbols_equal':True,'bounded_interval':[start,end],
                  'bounded_code_hex':raw[start:end].hex(),'bounded_code_sha256':sha(raw[start:end]),
                  'instructions':[x for x in ma['instructions']if start<=x['offset']<end],
                  'qualification':'Offline read as data; no JVM/classloading/newJava execution. Method equality, not all-class/lifecycle equivalence.'})
            bytecode.append({'class':cl,'entry':entry,'AD368_class':{'bytes':len(a),'sha256':sha(a)},'C7_class':{'bytes':len(b),'sha256':sha(b)},
              'whole_class_equal':a==b,'AD368_source':pins['actual-JAR'],'C7_source':net['actual_JAR'],'methods':rows})
    # Exact saved source and archived IL slices; no Java execution or inferred dynamic mode result.
    text=[]
    def slice_text(key,needle,before=0,lines=0,endneedle=None):
        pin=pins[key];source=Path(pin['path']).read_text('utf-8').splitlines();start=next(k for k,l in enumerate(source)if needle in l)-before
        if endneedle:end=next(k for k in range(start+1,len(source))if endneedle in source[k])
        else:end=start+lines
        out='\n'.join(source[start:end])+'\n'
        text.append({'source_key':key,'source':pin,'lines_one_based':[start+1,end],'text':out,'slice_utf8_sha256':sha(out.encode('utf-8'))})
    slice_text('actual-CS-source','private void gameInfo(byte[]',lines=20)
    slice_text('actual-CS-source','private void sendGameInfoSnapshot()',lines=12)
    slice_text('actual-CS-source','private void changeRoomMode(',lines=35)
    slice_text('actual-CS-source','private void resetRoomReadyAfterSettingsChange()',lines=12)
    slice_text('Protocol-source-reference','enum LitoModeStepDirection',lines=33)
    slice_text('Protocol-source-reference','static LitoGameInfoRequest litoGameInfoRequest(',lines=20)
    slice_text('BattleRules-source-reference','static Mode stepNative(',lines=25)
    slice_text('GameHub-source-reference','synchronized boolean updateSettings(',lines=21)
    slice_text('actual-CS-archived-IL','private void gameInfo(byte[])',endneedle='private void handleRentalTownGameInfo(')
    slice_text('actual-CS-archived-IL','private void sendGameInfoSnapshot()',endneedle='private void broadcastGameInfoSnapshot(')
    slice_text('actual-CS-archived-IL','private void changeRoomMode(',endneedle='     256: aload_0')
    slice_text('actual-GameHub-archived-IL','synchronized boolean updateSettings(',endneedle='private void countdownTickSafely(')
    slice_text('actual-Protocol-archived-IL','static local.lito381.server.Protocol$LitoGameInfoRequest litoGameInfoRequest(',endneedle='static java.lang.String litoCharacterSelectRequest(')
    rows=json.loads(Path(net['new_network']['frames']['path']).read_text('utf-8'));saved=[]
    for k in range(73,91):
        q=rows[k];body=bytes.fromhex(q['body_hex']);assert q['frame_bytes']==len(body)+2
        recordrow={'source':net['new_network']['frames'],'array_index_zero_based':k,'epoch':q['epoch'],'direction':q['direction'],
          'body_hex':body.hex(),'body_bytes':len(body),'frame_bytes':q['frame_bytes'],'qualification':'Existing accepted C7 WAITING server test-client network; no new capture/replay/NativeGUI/mode-step witness.'}
        if body[:2]in (b'\xd1\x03',b'\xd5\x03'):
            recordrow['map_u16LE']=int.from_bytes(body[-2:],'little')
        saved.append(recordrow)
    versions=[
      {'version':'AD368 original','JAR':pins['actual-JAR'],'source_report':record(SERVER/'server-ordinary-room-map-selection-contract.json'),
       'level':'Old finite static/archivedIL and7selectedcaptures only','map_scope':'Illegalmap restore firstmode map564 can differselected514; prior7capturehits0 applies onlyoldscope, not currentoverallabsence.',
       'mode_scope':'Mode/parser/updateSettings selected methods exact code andCPsymbols matchC7; static qualification only.'},
      {'version':'0612833B restore parent','JAR':net['restore_parent'],'source_report':netpin,
       'level':'Accepted parentsource/eightunits as reported, not launchedinC7networkbatch','map_scope':'Restore fixed currenthubrules; updateMap failureordering unchanged, parent notexecutedhere.'},
      {'version':'C7B169C0','JAR':net['actual_JAR'],'source_report':netpin,'frames':net['new_network']['frames'],'indices':[73,90],
       'level':'Accepted singleWAITING ordinarymap/auth network plusnewoffline mode methodequality','map_scope':'Host514→bothD5WORD514;Guestready1;Host509→perpeerD1ready0thenD5WORD509;Host/GuestFFFF→selfD5current509;Guestvalid514→selfD5summary0/config1 current509.',
       'mode_scope':'These savedframes haveC_D1sub3,notC_D1sub2. Do not upgrade mode-step semantics/publication/failedmode changes to dynamic acceptance.',
       'unknowns':net['unknowns']},
    ]
    boundary={
      'conditional_callers':'46D09E/46D0EC prepare4154E0 stack[sprite187F/1881,x1A7/246,yF2,callback46B7D0,control0/1,0]. Independentpostallocationislands, notactualcontrolcreation/click.',
      'first_upstream_unknown':'46D088 and46D0D6 call4F118C allocationSTOP; no allocationbody/success bridge.',
      'constructor':'4154E0 forwards callbackargEBP+14/controlEBP+18 along6args to4154FE call414910 firstunknownSTOP. No callbackstorage/virtualinvocation or ECX delivery proved. Onlyargumentroles/positions, noleft/right sprite interpretation.',
      'insertion':'46D0CF/46D11D call41B3F0 withparent64AD04,index0/1,conditionalobjectEAX, firstdirecthelperunknownSTOP; noinsertbody/runtimeparent/ownership qualification.',
      'mode_request_path':'AcceptedBatch27 handlerinputECX0→step2;input1→step1;46B9F4 ccc D1/subtype2/step. Bounded0/1branches contain noNative phase64AF98/owner/versionread orunknownpre-sendercall. Upstreamcaller/click unknown, notglobalabsence ofauthoritychecks.',
      'serializer_transport':'AcceptedBatch26/27 sender,c serializer/frame onlyreferences; nooldPASS. Cbody3/frame5 u16LEprefix5; no roomID/absoluteMode/string onrequest. External759058A0 deliveryboundary retained.',
      'Java_parser':'ActualProtocol method:opcodeD1/sub2/exactremaining1, directionfromWire1NEXT/2PREVIOUS; otherbytesincluding0/3 rejected, malformed beforeROOMguard. Mode currentvalue notcarried.',
      'relative_step':'ActualBattleRules.Mode.stepNative guard0..5; presentationorder0,1,5,2,4,3; NEXT+1,PREVIOUS-1 floorMod6→fromWire. BoundedsyntheticSwitchMapdirection segment proves enum-to-delta mapping. Town6 excluded byordinarygate, notmappedtoabsolute request2.',
      'ordinary_gate':'ActualCS gameInfo parse first; phaseROOM/currentRoomId!=0 thenexcludeRentalbranch; mode request.direction→changeRoomMode. Currentoption0&7F→Mode; !isBattleScene selfsnapshot0/1 return.',
      'update_guard':'ActualupdateSettings missingroom/exactowner/battleStarted/capacity guard; replacement retainsname/password/cap/options1..4/selections, setsmode andEQUAL_SIDESbit32 bymode.teamBased. NoNativeowner authority from thisserverguard.',
      'success_publication':'AfterupdateSettings true→resetreadyhelper→litoRoomModeD5/2absoluteBYTE→decode(replacement)map→litoRoomMapD5/3WORD; perpeer2then3. No D5/0/1 in this boundednormal prefix. Laterbot/matchmaking continuation notexpanded/qualified.',
      'reject_publication':'NonbattleTown/currentmode orupdateSettingsfalse→selfsendGameInfoSnapshot summary0/config1. Eachpacket/getter/send independent; noACK/result/atomicpair/allpeer guarantee.',
      'failure_order':'updateSettings writes settings/selectedmap/ready beforedecode/reconfigure; known failure/races/partialsend remainopen. C7map-specificupdateMaprepair doesnotautomatically repairmode/updateSettings ornetworkqualifymode failures.',
      'receive':'ReuseBatch24D5sub2 writesonly64ACCA lowBYTE preserves64ACCB/map64ACC9,noUI;sub3independentWORD→BYTEprojection. Batch23sub0/1independentmixedsnapshot effects; noaggregate ACK/owner/ready inferred.',
      'C7_version':'C7savedmapnetwork hasrealD1sub3/D5sub3 witnesses; priorAD368restore564 and7capturehits0 areoldscope only. NoNative ormode-step dynamicwitness supplied; do not generalize currentmap qualification.',
      'handoff_gap':'For dynamicmode qualifications needversionpinned server C_D1/2 NEXT/PREVIOUS and before/aftercurrentmode/map,readyperpeer thenD5/2/3;Town/nonowner/rejection0/1/failedgate distinctions. This is exact missing evidence for supervisor, notrequest tolaunch/newJava packet orSQL.',
    }
    wire={'direction':'C','opcode':209,'subtype':2,'fields':[{'offset':0,'type':'u8','value':209},{'offset':1,'type':'u8','value':2},
      {'offset':2,'type':'u8','domain':[1,2],'meaning':'1NEXT/2PREVIOUS relative server permutation; noabsolute mode value.'}],
      'format':'ccc','format_VA':'005028CC','format_RVA':'001028CC','call_VA':'0046B9F4','call_RVA':'0006B9F4',
      'body_bytes':3,'frame_bytes':5,'prefix':'u16LE total5 inheritedsender','encoding':'NumericBYTE only, no string/codec/roomID',
      'source_PE':sources[0],'trigger':'Native input0/1 staticbranch, conditionalcallercontrol0/1; actualeventchainblockedbeforeconstructor414910.',
      'server_evidence':'Exact AD368/C7 selectedclass-method code andCPsymbol match, frozenCSsource/archivedIL, supportingclasses readasdata only.',
      'receive_dependencies':{'D5sub2':existing['D5-mode-map-receive-contract.json'],'D5sub0_1':existing['D5-settings-receive-contract.json']},
      'known_response_layouts':{'D5sub2':'S D5,u8sub2,u32LEroom,u8absoluteMode body7/frame9','D5sub3':'S D5,u8sub3,u32LEroom,u16LEmap body8/frame10',
        'D5sub0_1':'ExistingBatch23 summary/config independent records, versionpinned; noresponsecorrelation/ACK.'},
      'qualification':'NewfiniteNativecallers/Java mode semanticcorroboration; existingwire unchanged; C7 dynamicmap supplement only.',
      'unknowns':boundary}
    preflight={'existingNative_mode_input_sender':record(ROOT/'CD1-map-outbound-contract.json'),'sender_serializer_replayed':False,
      'fixedASM_callbackpointer_hits':3,'finite_callers_selected':2,'thirdcontrol2_menu_candidate_notexpanded':True,
      'existingJava_step_semantics_references_available':True,'broadcaller_UI_census':False,
      'existingC7_network_attached_without_replay':netpin,'priorreport_mutated':False}
    packet=record(ROOT/'server-packet-delta.json');assert packet==prev['verification']['server_packet_unchanged']
    verification={'new_code_ranges':7,'new_data_ranges':0,'necessary_reused_raw_ranges':3,'instruction_assertions':len(assertions),
      'bounded_Java_classes':6,'bounded_Java_methods':sum(len(x['methods'])for x in bytecode),'source_IL_slices':len(text),
      'saved_C7_frame_rows_adopted':18,'new_capture_frames':0,'Native_dynamic_witnesses':0,'mode_step_dynamic_witnesses':0,
      'prior_primary_pins_preserved':29,'server_packet_unchanged':packet,'old_PASS_replayed':False,'all_match':True}
    for n,o in [('ranges.json',{'source':sources[0],'new_ranges':ranges,'necessary_reused_raw':reuse}),('instruction-witnesses.json',assertions),
      ('native-caller-and-unknown-matrix.json',boundary),('Java-bounded-class-methods.json',bytecode),('source-IL-witnesses.json',text),
      ('C7-saved-map-frame-witnesses.json',saved),('version-evidence-table.json',versions),('wire-contract.json',wire),
      ('equivalence-search-preflight.json',preflight),('finite-verification.json',verification)]:write('CD1-mode-step/'+n,o)
    write('CD1-mode-step-contract.json',{'schema':'lito.native-C_D1-mode-step-boundary.v1','updated_at':stamp(),
      'status':'FINITE_STATIC_MODE_STEP_CALLER_JAVA_SEMANTICS_SEALED_C7_MAP_ADOPTED_UI_MODE_DYNAMIC_OPEN',
      'sources':sources,'prior_primary_pins':prior,'existing_evidence':existing,'outputs':[record(q)for q in sorted(OUT.iterdir())if q.is_file()],
      'wire':wire,'conditional_boundaries':boundary,'version_evidence':versions,'verification':verification,
      'delta':{'new_wire':[],'new_opcodes':[],'wire_corrections':[],'known_corroboration':'Native conditionalcontrol0/1 caller arguments, Java relativecycle semantics, C7 acceptedmapversion evidence.'},
      'scope':{'writes':'ownROOTonly','Native_GUI_SQL_provider_Java_execution_Modern_BIN':False,'unknownNativebodiesexpanded':False,
        'old_PASS_capture_replayed':False,'historical_intercepted_or_Modern_scratch_work_resumed':False,'overall_complete':False}})
    print(json.dumps({'all_match':True,'primary':record(ROOT/'CD1-mode-step-contract.json'),'verification':verification},ensure_ascii=False))
if __name__=='__main__':main()

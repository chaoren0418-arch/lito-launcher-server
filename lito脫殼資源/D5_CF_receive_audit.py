"""Batch18 owner subtype5/catalog delete; other D5 layouts only."""
from audit import *
OUT=ROOT/'D5-CF-receive'
FIXED='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
PREVIOUS='5DD6E760A08D3CD3B096FC6B2F25BF053FB51EA12E9141887A7473423EFC7AB1'
def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    OUT.mkdir(exist_ok=True);pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    sources=[record(p)for p in [PE,BIN,ASM]];assert [p['sha256']for p in sources]==[FIXED,'CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292','2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A']
    prev=load('D1-E1-prestart-contract.json');pin=record(ROOT/'D1-E1-prestart-contract.json');assert pin['sha256']==PREVIOUS
    prior=prev['prior_primary_pins']+[{**pin,'expected_sha256':PREVIOUS,'match':True}];assert len(prior)==19
    for r in prior:assert record(Path(r['path']))['sha256']==r['expected_sha256']
    sup_path=ROOT.parent/'lito伺服器/server-room-D5-CF-consumer-supplement.json';sup=record(sup_path);assert sup['sha256']=='7774EA7546EEF33CB5240A58A36FA9FFF7A465303049C3C3E238081887279BEF'
    server=json.loads(sup_path.read_text(encoding='utf-8'));existing={'server_D5_CF_supplement':sup,'reference_index':server['reference_index'],'server_manifest':server['manifest'],'native_dispatch':record(LEGACY/'analysis/NATIVE-S-DISPATCH.json')}
    for r in existing.values():assert record(Path(r['path']))==r
    packet=record(ROOT/'server-packet-delta.json');assert packet['sha256']=='B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C'
    ins={};owners={};ranges=[];inherited=[]
    def decode(raw,va,q):
        rows=[]
        for i in cs.disasm(raw,va):
            r={'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str};rows.append(r);ins[i.address]=r;owners[i.address]=q
        assert sum(len(bytes.fromhex(r['raw_hex']))for r in rows)==len(raw),(hex(va),'alignment')
        return rows
    def reuse(index,va,n,kind='code'):
        row=next(r for r in load(index)['ranges']if int(r['va'],16)==va);q=row['output'];raw=Path(q['path']).read_bytes()
        assert record(Path(q['path']))==q and raw==pe.get_data(va-0x400000,len(raw))and n<=len(raw)
        inherited.append({**mapping(pe,va),'index':record(ROOT/index),'range':q,'used_bytes':n,'kind':kind,'raw_matches_fixed_PE':True,'old_test_replayed':False})
        if kind=='code':decode(raw[:n],va,q)
    for index,va,n,kind in [
      ('priority-complete-tails.json',0x4A2A50,296,'code'),('SCE-navigation/ranges.json',0x4B29DB,56,'code'),('SCE-navigation/ranges.json',0x4B313F,30,'code'),
      ('SCE-navigation/ranges.json',0x4B3123,28,'code'),('SCE-navigation/ranges.json',0x4B346C,4,'data'),
      ('SD0-receive/ranges.json',0x46A540,132,'code'),('skill-pane-layout/ranges.json',0x450230,48,'code'),
      ('server-packet-evidence/ranges.json',0x5025A8,2,'data'),('SD0-receive/ranges.json',0x5028F8,3,'data'),('SCE-navigation/ranges.json',0x4A9030,1,'code'),
      ('receive-boundary/ranges.json',0x4C6AE0,80,'code'),('S0C-status-owner/ranges.json',0x4A2B78,24,'data'),('S0C-status-owner/ranges.json',0x4A2B90,33,'data')]:reuse(index,va,n,kind)
    specs=[(0x4A9E50,91,'code','D5_header_room_subtype_gate'),(0x4A9FAF,121,'code','D5_subtype5_owner_complete_branch'),(0x4AA06C,5,'code','D5_shared_return'),
      (0x4A9EAB,62,'code','D5_subtype0_layout_calls_only'),(0x4A9EF6,116,'code','D5_subtype1_layout_and_count_bytes_only'),
      (0x4A9F6A,31,'code','D5_subtype2_3_layout_only'),(0x4A9FA5,10,'code','D5_subtype4_scalar_entry_only'),(0x4AA028,73,'code','D5_subtype6_9_scalar_layout_only'),
      (0x4A9040,66,'code','CF_complete_fallback_delete_handler'),(0x4659F0,16,'code','CF_row_remove_wrapper'),(0x450290,131,'code','UI_DWORD_catalog_remove_complete'),
      (0x4EBB30,25,'code','owner_notification_four_pointer_membership'),(0x4B2F67,12,'code','D5_shared_dispatch'),(0x4B31A5,12,'code','CF_fallback_dispatch'),
      (0x4AA074,40,'data','D5_subtype_jump_table'),(0x634470,10,'data','D5_subtype0_format'),(0x634464,10,'data','D5_subtype1_format'),
      (0x634300,2,'data','D5_subtype5_s_literal'),(0x63424C,2,'data','CF_d_literal'),(0x632594,2,'data','D5_subtype3_h_literal'),
      (0x4F7228,4,'data','dump_copy_pointer_NOT_runtime'),(0x4B3545,1,'data','D5_primary_map'),(0x4B33F4,4,'data','D5_primary_jump'),
      (0x4B3789,1,'data','D5_fallback_map'),(0x4B3644,4,'data','D5_fallback_jump'),(0x4B353F,1,'data','CF_primary_default_map'),
      (0x4B3783,1,'data','CF_fallback_map'),(0x4B3634,4,'data','CF_fallback_jump')]
    for va,n,kind,label in specs:
        raw=pe.get_data(va-0x400000,n);assert len(raw)==n;p=OUT/f'{va:08X}-{n:X}.bin';p.write_bytes(raw);q=record(p)
        ranges.append({**mapping(pe,va),'bytes':n,'kind':kind,'label':label,'raw_hex':raw.hex(),'output':q,'instructions':decode(raw,va,q)if kind=='code'else[]})
    def w(at):return {'fixed_PE_sha256':FIXED,**mapping(pe,at),**ins[at],'range':owners[at]}
    expected=[(0x4B2F69,'call','0x4a9e50'),(0x4B31A7,'call','0x4a9040'),(0x4B3123,'push','0xf'),(0x4B312A,'call','0x4c6ae0'),(0x4B3135,'call','0x5ad0986'),
      (0x4A9E64,'push','0x5028f8'),(0x4A9E6A,'call','0x4a2a50'),(0x4A9E77,'cmp','eax, 6'),(0x4A9E7F,'mov','dword ptr [0x64abf4], eax'),
      (0x4A9E91,'jne','0x4aa06c'),(0x4A9E97,'movsx','eax, byte ptr [ebp - 1]'),(0x4A9E9B,'cmp','eax, 9'),(0x4A9E9E,'ja','0x4aa06c'),
      (0x4A9EA4,'jmp','dword ptr [eax*4 + 0x4aa074]'),(0x4A9FAF,'push','0x64abca'),(0x4A9FB4,'push','0x1c'),
      (0x4A9FB6,'add','esi, 6'),(0x4A9FB9,'push','0x634300'),(0x4A9FBF,'call','0x4a2a50'),(0x4A9FCC,'cmp','eax, 0x4b'),
      (0x4A9FD7,'mov','eax, dword ptr [ecx + 0x12c]'),(0x4A9FEF,'call','0x773f5200'),(0x4A9FF4,'mov','ecx, dword ptr [0x648f18]'),
      (0x4A9FFD,'call','0x4ebb30'),(0x4AA004,'jne','0x4aa06c'),(0x4AA006,'mov','al, byte ptr [0x648f1c]'),(0x4AA00D,'jne','0x4aa06c'),(0x4AA01E,'call','0x458bb0'),
      (0x4EBB30,'mov','eax, 0x836b54'),(0x4EBB35,'cmp','dword ptr [eax], ecx'),(0x4EBB3C,'cmp','eax, 0x836b64'),
      (0x4A9EDE,'push','0x634470'),(0x4A9F29,'push','0x634464'),(0x4A9F39,'movsx','ecx, byte ptr [ebp - 2]'),
      (0x4A9F60,'cmp','esi, 8'),(0x4A9F6F,'jmp','0x4aa05b'),(0x4A9F7B,'push','0x632594'),(0x4AA03E,'push','0x5025a8'),(0x4AA05E,'push','0x5025a8'),
      (0x4A9049,'push','0x63424c'),(0x4A904F,'call','0x4a2a50'),(0x4A9061,'call','0x4659f0'),
      (0x4A906F,'movsx','eax, word ptr [edx + 0xc]'),(0x4A9073,'cmp','eax, ecx'),(0x4A9075,'jne','0x4a907e'),(0x4A9079,'call','0x46a540'),
      (0x4659F7,'call','0x450290'),(0x4502B0,'cmp','dword ptr [ecx + 4], edx'),(0x4502BD,'call','dword ptr [eax]'),
      (0x4502DD,'call','ebx'),(0x4502EB,'mov','dword ptr [esi + 0x2c], edx'),(0x450304,'jmp','0x450307'),
      (0x46A5A3,'je','0x46a5be'),(0x46A5AC,'push','0xcd')]
    assertions=[]
    for at,mn,op in expected:
        assert (ins[at]['mnemonic'],ins[at]['operands'])==(mn,op),(hex(at),ins.get(at));assertions.append({**w(at),'expected_mnemonic':mn,'expected_operands':op,'match':True})
    for mapva,b,jva,target in [(0x4B3545,0x7B,0x4B33F4,0x4B2F67),(0x4B3789,0x35,0x4B3644,0x4B2F67),(0x4B353F,0x99,0x4B346C,0x4B3123),(0x4B3783,0x31,0x4B3634,0x4B31A5)]:
        assert pe.get_data(mapva-0x400000,1)==bytes([b])and int.from_bytes(pe.get_data(jva-0x400000,4),'little')==target
    targets=struct.unpack('<10I',pe.get_data(0xAA074,40));assert list(targets)==[0x4A9EAB,0x4A9EF6,0x4A9F6A,0x4A9F74,0x4A9FA5,0x4A9FAF,0x4AA028,0x4AA02F,0x4AA036,0x4AA056]
    for va,s in [(0x634300,b's\0'),(0x63424C,b'd\0'),(0x632594,b'h\0'),(0x634470,b'schcscccc\0'),(0x634464,b'ssccccccc\0')]:assert pe.get_data(va-0x400000,len(s))==s
    assert pe.get_data(0xF7228,4)==bytes.fromhex('00914077')
    q=server['wire']['original_frames'];assert record(Path(q['path']))==q;rows=json.loads(Path(q['path']).read_text(encoding='utf-8'))
    owner=server['wire']['owner_D5'];i=owner['frame_index_zero_based'];r=rows[i];body=bytes.fromhex(r['body_hex']);assert i==77 and body.hex()==owner['body_hex']
    assert r['direction']=='S'and r['epoch']=='Guest-GAME'and body[:6]==bytes.fromhex('d50501000000')and body[-1]==0 and 0 not in body[6:-1]
    alias=body[6:-1];assert len(alias)==27 and alias.decode('ascii')==owner['owner_alias']and len(body)==34 and r['frame_bytes']==36
    cd=rows[72];cdb=bytes.fromhex(cd['body_hex']);assert cd['direction']=='S'and cd['epoch']==r['epoch']and cdb[:5]==bytes.fromhex('cd01000000')and cdb[5:cdb.index(0,5)]==alias
    peer=rows[75];assert peer['direction']=='S'and peer['epoch']==r['epoch']and peer['body_hex'].startswith('d001000000')and 75<77
    join=rows[66];assert join['direction']=='C'and join['body_hex']=='cf0100000000'and join['frame_bytes']==8
    assert not any(x.get('direction')=='S'and x.get('body_hex','').startswith('cf')for x in rows[73:78])
    capture={'source':q,'array_index_zero_based':77,'frame_1based':78,'epoch':r['epoch'],'direction':'S','opcode':213,'subtype':5,'body_hex':body.hex(),'body_bytes':34,'frame_bytes':36,
      'fields':[{'index':1,'type':'u8subtype','body_offset':1,'bytes':1,'value':5},{'index':2,'type':'u32LEroom','body_offset':2,'bytes':4,'value':1},
        {'index':3,'type':'NUL8bitownerdisplay','body_offset':6,'bytes':28,'raw_hex':alias.hex(),'ASCII_capture_label':alias.decode('ascii')}],
      'consumed':34,'remaining':0,'matched_prior_CD_array_index':72,'prior_peer_D0_array_index':75,'S_CF_in_ordinary_exit_range':False,
      'classification':'Existing ordinaryROOM exit wire corroboration; noNative/UI/session authority or own-exitACK',
      'C_CF_counterdirection_example':{'source':q,'array_index_zero_based':66,'frame_1based':67,'direction':'C','body_hex':join['body_hex'],'body_bytes':6,'frame_bytes':8,'qualification':'Knownsealedjoinempty-password, notdelete'}}
    layouts=[]
    for subtype,target in enumerate(targets):
        fmt='schcscccc'if subtype==0 else'ssccccccc+min(8,max(0,signedK))c'if subtype==1 else's'if subtype==5 else'h'if subtype==3 else'c'
        fields=next(x for x in server['D5_layouts']if subtype in x['subtypes'])
        layouts.append({'subtype':subtype,**mapping(pe,target),'native_payload_format_after_cd_header':fmt,'known_server_layout':fields['body'],
          'scope':'subtype5 ownerdisplayeffects qualified below'if subtype==5 else'dispatch/layout only; triggers/settings/UIeffects notadopted',
          'count_qualification':'Native signedK loopalways8iterations; negativeKreadsnone/zeroes8, K>8onlyreads8 anddoesnotexactendvalidate. AD368producer0..8 separate.'if subtype==1 else None})
    wire=[{'direction':'server-to-client','opcode_hex':'D5','opcode_decimal':213,'schema':'c subtype + d room, then subtypepayload; subtype5 s','VA':'004A9E50','RVA':'000A9E50','fixed_PE_sha256':FIXED,
      'owner_fields':[{'index':1,'type':'u8','endianness':'singlebyte','body_offset':1,'semantic':'subtype5'}, {'index':2,'type':'DWORD','endianness':'little','body_offset':2,'semantic':'roomcontext'},
        {'index':3,'type':'NULterminated8bitstring','endianness':'byte stream','body_offset':6,'capacity_including_NUL':28,'encoding':'Nativebyteparsernotfixedcodecdeclaration; capturedASCII/JavaMS950producerseparate','semantic':'ownerpresentationalias, notphysical/source/sessionauthority'}],
      'body_bytes':'7+O','frame_bytes':'9+O','trigger':'Existing ordinaryROOM owner transfer publication; notown-exitACK/ready/start','native_state':'phase6assignsroomglobalbeforetype; otherphasesrequireincomingroom==64ABF4; noadditionalphasewhitelist',
      'evidence':'fixedPEheader/type5/UIpredicate/literal plus1sealed34/36ownerpacket','unknown':['runtimecurrentroom/global/UI/resources','otherD5subtypeeffects/triggercoverage','nonASCIIencoding','nativeinputlength/exactendvalidation','serverpublicationdelivery/atomiccommitnotpacketproof']},
      {'direction':'server-to-client','opcode_hex':'CF','opcode_decimal':207,'schema':'d','VA':'004A9040','RVA':'000A9040','fixed_PE_sha256':FIXED,
       'fields':[{'index':1,'type':'u32LEroomcatalogID','body_offset':1,'bytes':4,'encoding':'none'}],'body_bytes':5,'frame_bytes':7,'trigger':'Java nondefaultemptyroomdeletion source/ILonly; noS_CFexitframehere',
       'native_state':'fallbackhandler noexplicitphase/currentjoinedroomgate; primarydefaultC_QUIT/privatecall instead','evidence':'fixedPEliteral/handler/directUIdelete +serversealedsourcecontract, zeroS_CFdynamicrecords',
       'unknown':['NativeUIcataloglifetime/rowkeys/selectionWORD','privateprimarydefaulteffect','CFdelete/resetruntimebranch','nativecheckedread/exactend','epoch/channel/roomgeneration']},
      {'direction':'client-to-server','opcode_hex':'CF','schema':'d+s(password)','body_bytes':'6+P','frame_bytes':'8+P','classification':'Knowncounterdirectionjoin, notnewwire/newNativeproducerqualification','source':q,'example_array_index':66},
      {'direction':'server-to-client','opcode_hex':'CC','schema':'existingroomrowreplacement/list','classification':'Knowndefaultreset isS_CC, notS_CF/delete-recreate; nocardinality/terminator/ACKinferred','source':sup}]
    d5={'header':'4A2A50 cd reads subtype→EBP-1 androom→EBP-8, payloadstartsbody+6','phase_room_gate':{'phase6':w(0x4A9E7F),'meaning':'unconditionallyassign incomingroom64ABF4 beforevalidsubtype; invalidsubtypecanstilloverwriteglobal',
      'otherphases':'incomingroomcompared64ABF4, mismatchreturn4AA06C; noadditionalallowedphasecheck','adapter_requirement':'independentlyownedepoch/CEroom validateBEFOREpublishing; Nativepostassignmentnotatomicroomauthority'},
      'subtype_gate':'MOVSXi8 thenunsignedJA>9 excludesnegative/10..255; fixedjumptable0..9','layouts':layouts,
      'subtype5':'s writesownerdisplay64ABCA cap28includingNUL; no4D3A80self/owner/member compare, row lookup, sessiontransfer orscenephasechange',
      'notification_order':'aliasglobalwritten beforeformat/predicate. signed834B90>4B selectsformat*[834B94]+12C else63D618; external773F5200 formatsaliasintostackEBP-6C(108bytes). Formatresult notchecked here.',
      'UI_predicate':'4EBB30 checksECX648F18 exactlyagainst4DWORD836B54..836B63; ifpresentreturnwithout458BB0. Else if648F1C!=0 alsoreturn. Otherwise458BB0(stackmessage,5034A0 color,503478) notification.',
      'predicate_semantics':'Onlyfour-pointermembership known; do notlabelitself/owner/modeauthority orassumeinitializedUI. NoGUIacceptance.',
      'owner_linkage':'Displayaliasupdate independentofmemberrow; capturedaliasmatchespriorCDbutNativehandlerdoesnotlinkit. Absentrow cannotmanufactureparticipant/physical/source/sessionauthority.'}
    cf={'dispatch_primary':'mode4/signedphase>=3 CFslot99hex default4B3123 attemptC_QUIT0F/c literal→4C6AE0, private05AD0986(0). Rawdoesnotproveprivate termination/execution orworldeffects; legacylabelclientquit keptasnavigationonly.',
      'dispatch_fallback':'CFslot31hex→4B31A5 call4A9040; readsDWORDd, call4659F0(UI64A958,roomID)',
      'catalog_delete':'4659F0 complete16bytes calls450290 complete131. ScanUIcount+2C, array+58, rowDWORD+4 equalsincomingDWORD; allmatchingrows deleted throughvirtual[vtbl](1), shiftpointers via*[4F7228], count-- andselection+28clamp; noaliascomparison.',
      'copy_snapshot':'disk*[4F7228]=77409100, actualruntimeprovider/body/operation/lifetimeunknown. UIrowdereferenceprecedesNULLguard.',
      'after_delete':'readUI64A98C+0C WORD withMOVSXi16 andcomparetoincomingDWORD. Ifequal call46A540(0); ifnotreturn. No64ABF4activejoinedroomgate/clear andno469CA0selfexitcall.',
      'preview_reset':'ReuseBatch16 46A540(0):450230(UI64A954), clearpreviewend64ACF8=begin64ACF4,4687B0(0); SI0skipsknownC_CDemit. UIcallbackeffects unknown.',
      'signed_width':'SelectedpreviewWORDsignedextension meanscatalogDWORD>32767doesnotgenerallymatchsameunsigned16bits; internalUIqualification, notwirecorrection.',
      'no_current_room_assumption':'Catalogmaydeleteanotherroom/currentchannelcontext; Nativefallbacknotroom-equalityguarded. AdapterinvalidatesactivejoinedroomseparatelywithoutownleaveACK/auth/sourcechanges.',
      'CF_vs_CC':'NoS_CCdefaultresetcalledbyCFhandler; serverdefaultreset remainsseparateS_CCproducer. NoCFpayloadstring/result/subtypequalified.',
      'strict_length':'Native dreader itselfnoexactendtest here. AD368S_CFexact5bytes peracceptedproducer; do notguessnon5S payloadasC_CFpassword/reset/ACK.'}
    helpers={'bounded_direct_calls':[{**w(at),'scope':'boundedneworunchangedreusedhelper; allprivate/virtual/externaldepthunknown'}for at in sorted(ins)if ins[at]['mnemonic']=='call'and any(lo<=at<hi for lo,hi in [(0x4A9E50,0x4A9EAB),(0x4A9FAF,0x4AA028),(0x4A9040,0x4A9082),(0x4659F0,0x465A00),(0x450290,0x450313),(0x4B3123,0x4B313F)])],
      'unknowns':['actualNativephase/64ABF4/ownerdisplaylifetime','resource834B90/94format/backend','external773F5200formatbounds/body/module','648F18/836B54four-pointerstate/648F1Csuppressionauthority','458BB0notificationUI','catalogrow/vtable/arraycount/selectedsignedWORD','external77409100actualcopy/provider','450230/4687B0deepUIeffects','private05AD0986primarydefaultterminationnotproved','otherD5subtypeUI/triggers','CFdelete/defaultresetdynamicwire/epochreuse','physical/source/sessionownerandself-exitcommitunknown']}
    consumer={'owner':'ExplicitAD368 currentownedepoch/CEroom D505->ownerpresentationalias, noowner/session/physical/sourceauthority; matchknownqualifiedrawrowifpresent, absentlinkunresolved. D0peer thenD5doesnotleaveROOM.',
      'catalog':'AD368S_CF5byte delete currentchannel/epochcatalogremove-ifpresent; unknowndeleteidno-op. CFmaytargetotherroom; activejoinedroominvalidationseparate, noown-exitACK.',
      'direction':'C_CFjoinpassword, S_CFdelete, S_CCreset/listseparate; notsameopcode-bodydefaulting ordelete/recreatepair.',
      'guard':'NativeD5phase6roomoverwrite andCFprimarydefaultcannotbeusedasadapterauthority. UI/runtime/table/source/physicalguardsretained; D5/CFeffectsdo notinstallNativeactor/sessionauthority.',
      'newwire':'Knowncorroborationonly; noactualdifference/newopcode/correction, noJavachange requested.'}
    verification={'instruction_assertions':len(assertions),'new_code_ranges':14,'new_data_ranges':14,'inherited_raw_ranges':13,'D5_header_bytes':91,'D5_owner_branch_bytes':121,'CF_handler_bytes':66,'CF_UI_remove_bytes':131,
      'sealed_D5_owner_frames':1,'sealed_C_CF_counterdirection_examples':1,'sealed_S_CF_frames':0,'prior_primary_pins':19,'all_match':True,'server_packet_unchanged':packet,'old_test_replayed':False}
    outputs={'ranges.json':{'source':sources[0],'ranges':ranges,'inherited_raw':inherited},'instruction-witnesses.json':{'assertions':assertions},'wire-and-direction-contracts.json':{'contracts':wire},
      'D5-owner-and-layouts.json':d5,'CF-delete-and-preview.json':cf,'sealed-owner-frame.json':capture,'direct-helper-boundaries.json':helpers,'consumer-gap.json':consumer,'new-evidence-verification.json':verification}
    for n,o in outputs.items():write('D5-CF-receive/'+n,o)
    primary={'schema':'lito.native-D5-CF-receive-boundary.v1','updated_at':stamp(),'status':'FINITE_STATIC_OWNER_DELETE_BOUNDARY_SEALED_NATIVE_UI_SOURCE_AUTHORITY_OPEN',
      'sources':sources,'prior_primary_pins':prior,'existing_evidence':existing,'reusable_outputs':[record(p)for p in sorted(OUT.iterdir())if p.is_file()and p.name not in ['independent-review.json','READ-ME.txt']],
      'wire':wire,'D5_owner':d5,'CF_delete':cf,'direct_helpers':helpers,'consumer_gap':consumer,'verification':verification,
      'delta':{'new_wire':[],'wire_corrections':[],'new_opcodes':[],'known_corroboration':['D5subtype5ownerdisplay/otherlayoutdiscrimination','S_CFdddelete/C_CFjoin/S_CCresetdistinction','ordinaryROOMsealedownerpacketarray77'],
        'internal_boundaries':['D5phase6roomassignmentbeforetype','CFprimarydefaultvsfallbackdelete','selectedpreviewWORDsignedvsroomDWORD']},
      'scope':{'writes':'ownresourcefolderonly','native_started':False,'GUI_input':False,'SQL':False,'provider_or_new_capture':False,'Java_taken_over':False,'WORLD_or_intercepted_work_followed':False,
        'other_D5_settings_UI_fully_expanded':False,'physical_source_session_owner_or_selfexit_authority':False,'overall_complete':False}}
    write('D5-CF-receive-contract.json',primary);print(json.dumps({'primary':record(ROOT/'D5-CF-receive-contract.json'),'verification':verification},ensure_ascii=False))

if __name__=='__main__':main()

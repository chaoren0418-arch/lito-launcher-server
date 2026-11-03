"""Batch19 finite S_CC catalog rows and sealed CF/reset corroboration."""
from audit import *
OUT=ROOT/'SCC-catalog'
FIXED='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
PREVIOUS='C8E20CD020392C0D26E21F8FB1B830B9009998B942CABF65EBDF59AA355EDA2C'
def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    OUT.mkdir(exist_ok=True);pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    sources=[record(p)for p in [PE,BIN,ASM]];assert [p['sha256']for p in sources]==[FIXED,'CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292','2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A']
    prev=load('D5-CF-receive-contract.json');pin=record(ROOT/'D5-CF-receive-contract.json');assert pin['sha256']==PREVIOUS
    prior=prev['prior_primary_pins']+[{**pin,'expected_sha256':PREVIOUS,'match':True}];assert len(prior)==20
    for r in prior:assert record(Path(r['path']))['sha256']==r['expected_sha256']
    sup_path=ROOT.parent/'lito伺服器/server-room-last-member-delete-reset-network.json';sup=record(sup_path);assert sup['sha256']=='4CB29A6DDCB95B4EEE9389AA1B36BF179F3415878F7361E7A7149ABE7F066517'
    server=json.loads(sup_path.read_text(encoding='utf-8'));existing={'server_delete_reset_report':sup,'server_manifest':server['manifest'],'server_wire_poststate':server['wire_poststate'],
      'native_dispatch':record(LEGACY/'analysis/NATIVE-S-DISPATCH.json'),'Batch18_historical_contract':pin}
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
      ('priority-complete-tails.json',0x4A2A50,296,'code'),('S0C-status-owner/ranges.json',0x4A2B78,24,'data'),('S0C-status-owner/ranges.json',0x4A2B90,33,'data'),
      ('SCE-navigation/ranges.json',0x4B29DB,56,'code'),('SCE-navigation/ranges.json',0x4B313F,30,'code'),('skill-pane-layout/ranges.json',0x450BC0,25,'code'),
      ('SCD-member-comparison/ranges.json',0x450B90,37,'code'),('D5-CF-receive/ranges.json',0x4A9040,66,'code'),('D5-CF-receive/ranges.json',0x450290,131,'code'),
      ('D5-CF-receive/ranges.json',0x4659F0,16,'code'),('SD0-receive/ranges.json',0x46A540,132,'code'),('skill-pane-layout/ranges.json',0x450230,48,'code'),
      ('SCE-navigation/ranges.json',0x4B3123,28,'code'),('SCE-navigation/ranges.json',0x4B346C,4,'data'),('D5-CF-receive/ranges.json',0x4B353F,1,'data'),
      ('D5-CF-receive/ranges.json',0x4B3783,1,'data'),('D5-CF-receive/ranges.json',0x4B3634,4,'data'),('receive-boundary/ranges.json',0x4C6AE0,80,'code'),
      ('server-packet-evidence/ranges.json',0x5025A8,2,'data'),('SD0-receive/ranges.json',0x634370,3,'data'),('S0C-status-owner/ranges.json',0x4F118C,6,'code')]:reuse(index,va,n,kind)
    specs=[(0x4A7A30,1143,'code','complete_S_CC_catalog_handler'),(0x4B2F13,12,'code','shared_CC_dispatch'),(0x465940,173,'code','full_catalog_row_add_not_replace'),
      (0x463C10,120,'code','catalog_row_constructor'),(0x450260,35,'code','pointer_append_capacity_guard'),(0x4A7EC0,24,'code','catalog_icon_by_attributes'),
      (0x4A7EE0,12,'code','catalog_flags_low_by_attributes'),(0x4A7EF0,80,'code','catalog_mode_flags_default_uninitialized'),
      (0x465B00,55,'code','rename_matching_rows_virtual_text'),(0x465AC0,55,'code','matching_rows_icon'),(0x465B40,55,'code','matching_rows_display_users'),
      (0x465B80,55,'code','matching_rows_capacity'),(0x465C50,55,'code','matching_rows_map_byte'),(0x465C10,55,'code','matching_rows_attributes'),
      (0x465C90,55,'code','matching_rows_flags'),(0x465CD0,61,'code','first_row_flags_or_roomID_if_absent'),(0x463D10,44,'code','catalog_text_external_heap_operation'),
      (0x463D00,13,'code','row_icon8_setter'),(0x463D40,13,'code','row_display_users6C_setter'),(0x463D50,13,'code','row_capacity70_setter'),
      (0x463D70,13,'code','row_map74_byte_setter'),(0x463D60,13,'code','row_attributes75_byte_setter'),(0x463D80,13,'code','row_flags78_setter'),
      (0x4B353C,1,'data','CC_primary_map'),(0x4B33D8,4,'data','CC_primary_jump'),(0x4B3780,1,'data','CC_fallback_map'),(0x4B3628,4,'data','CC_fallback_jump'),
      (0x4A7EA8,24,'data','CC_subtype_table'),(0x4A7F40,24,'data','modeflag_table'),(0x6343DC,8,'data','old_fullrow_dsccchc'),(0x6343E4,8,'data','new_fullrow_dschhhc'),
      (0x4F8C20,4,'data','catalog_text_virtual_slot_dump_NOT_runtime'),(0x6342E4,3,'data','partial_dc_format'),(0x634328,3,'data','partial_dh_format'),(0x6343AC,4,'data','partial_dhc_format')]
    for va,n,kind,label in specs:
        raw=pe.get_data(va-0x400000,n);assert len(raw)==n;p=OUT/f'{va:08X}-{n:X}.bin';p.write_bytes(raw);q=record(p)
        ranges.append({**mapping(pe,va),'bytes':n,'kind':kind,'label':label,'raw_hex':raw.hex(),'output':q,'instructions':decode(raw,va,q)if kind=='code'else[]})
    def w(at):return {'fixed_PE_sha256':FIXED,**mapping(pe,at),**ins[at],'range':owners[at]}
    expected=[(0x4B2F15,'call','0x4a7a30'),(0x4A7A4A,'call','0x4a2a50'),(0x4A7A4F,'movsx','eax, byte ptr [ebp - 2]'),(0x4A7A59,'ja','0x4a7ea1'),
      (0x4A7A5F,'jmp','dword ptr [eax*4 + 0x4a7ea8]'),(0x4A7A66,'cmp','dword ptr [0x63d54c], 0x9ea8'),(0x4A7A70,'jl','0x4a7b68'),
      (0x4A7A91,'push','0x64'),(0x4A7A97,'push','0x6343e4'),(0x4A7A9D,'call','0x4a2a50'),(0x4A7B89,'push','0x6343dc'),
      (0x4A7AA9,'cmp','cx, -1'),(0x4A7AAF,'cmp','cx, -0x64'),(0x4A7AC4,'movsx','esi, cx'),(0x4A7AD0,'cmp','dword ptr [ecx], esi'),
      (0x4A7AD5,'add','ecx, 0x24'),(0x4A7ADE,'mov','edi, eax'),(0x4A7B21,'call','0x465940'),(0x4A7C18,'call','0x465940'),(0x4A7C62,'call','0x465940'),
      (0x4A7B45,'sub','cl, dl'),(0x4A7B4F,'dec','cl'),(0x4A7BBF,'add','dl, 0xc'),(0x4A7BE7,'mov','dl, 0xfe'),(0x4A7BEA,'sub','dl, bl'),
      (0x465960,'mov','eax, dword ptr [esi + 0x68]'),(0x465965,'jne','0x465982'),(0x465975,'cmp','dword ptr [ebx + 4], edi'),(0x465978,'je','0x4659da'),
      (0x465982,'cmp','dword ptr [esi + 0x2c], 0x100'),(0x465989,'jge','0x4659da'),(0x46598B,'push','0x7c'),(0x46598D,'call','0x4f118c'),
      (0x4659C2,'call','0x463c10'),(0x4659D5,'call','0x450260'),(0x450BCe,'mov','dword ptr [eax + 4], ecx'),
      (0x463C3E,'mov','dword ptr [esi + 8], ecx'),(0x463C44,'mov','dword ptr [esi + 0x6c], edx'),(0x463C4A,'mov','dword ptr [esi + 0x70], ecx'),
      (0x463C50,'mov','byte ptr [esi + 0x74], dl'),(0x463C58,'mov','byte ptr [esi + 0x75], cl'),(0x463C75,'mov','dword ptr [esi + 0x78], eax'),
      (0x45026B,'jge','0x45027f'),(0x450274,'mov','dword ptr [edx + eax*4], esi'),(0x45027C,'mov','dword ptr [ecx + 0x2c], eax'),
      (0x4A7EF4,'movsx','eax, cl'),(0x4A7EFA,'ja','0x4a7f39'),(0x4A7F39,'mov','eax, dword ptr [ebp - 4]'),
      (0x4A7C96,'call','0x465b00'),(0x4A7D55,'call','0x465b80'),(0x4A7DD0,'call','0x465b40'),(0x4A7E6B,'call','0x465c50'),
      (0x465B25,'call','dword ptr [edx + 0xc]'),(0x465CF6,'mov','eax, dword ptr [ebp + 8]'),(0x465D06,'mov','eax, dword ptr [eax + 0x78]'),
      (0x463D1F,'call','0x773c7310'),(0x463D2B,'call','0x77406880'),(0x463D34,'mov','dword ptr [esi + 0x10], eax'),
      (0x4A906F,'movsx','eax, word ptr [edx + 0xc]'),(0x4A9079,'call','0x46a540'),(0x4B3135,'call','0x5ad0986')]
    assertions=[]
    for at,mn,op in expected:
        assert (ins[at]['mnemonic'],ins[at]['operands'])==(mn,op),(hex(at),ins.get(at));assertions.append({**w(at),'expected_mnemonic':mn,'expected_operands':op,'match':True})
    for va,b,jva in [(0x4B353C,0x74,0x4B33D8),(0x4B3780,0x2E,0x4B3628)]:assert pe.get_data(va-0x400000,1)==bytes([b])and int.from_bytes(pe.get_data(jva-0x400000,4),'little')==0x4B2F13
    assert list(struct.unpack('<6I',pe.get_data(0xA7EA8,24)))==[0x4A7A66,0x4A7C6D,0x4A7CA1,0x4A7D1C,0x4A7D97,0x4A7E13]
    assert int.from_bytes(pe.get_data(0xF8C20,4),'little')==0x463D10
    fmt_new='dschhhc';fmt_old='dsccchc';assert pe.get_data(0x2343E4,8)==fmt_new.encode()+b'\0'and pe.get_data(0x2343DC,8)==fmt_old.encode()+b'\0'
    frame_pin=server['new_network']['frames'];assert record(Path(frame_pin['path']))==frame_pin;assert frame_pin['sha256']=='626B77E8C057A34CAF01871B5DBEC45F151C930CAAFC9AC0411829E51BEC8914'
    rows=json.loads(Path(frame_pin['path']).read_text(encoding='utf-8'));captures=[]
    for index in [74,75,79,80]:
        r=rows[index];body=bytes.fromhex(r['body_hex']);fields=[];version=None
        if body[0]==0xCF:
            assert body==bytes.fromhex('cf02000000')and r['frame_bytes']==7;fields=[{'index':1,'type':'u32LEcatalogroom','body_offset':1,'bytes':4,'value':2}];role='Existingnondefaultroom2 catalogdelete tocurrentchannel; nativeUI/ownleaveACK notproved'
        else:
            assert body.hex()==server['new_network']['default_reset']['CC_body_hex']and r['frame_bytes']==28 and len(body)==26 and body[1]==0
            sameversion=next(x for x in rows[:index]if x.get('epoch')==r['epoch']and x.get('direction')=='S'and x.get('body_hex','').startswith('3a'))
            version=int.from_bytes(bytes.fromhex(sameversion['body_hex'])[3:7],'little',signed=True);assert version>=0x9EA8
            pos=2
            for k,code in enumerate(fmt_new,1):
                start=pos
                if code=='s':end=body.index(0,pos);value=body[pos:end].hex();pos=end+1
                else:n=4 if code=='d'else 2 if code=='h'else 1;value=int.from_bytes(body[pos:pos+n],'little');pos+=n
                fields.append({'index':k,'type':code,'body_offset':start,'bytes':pos-start,'value':value,'wire_hex':body[start:pos].hex()})
            assert pos==26 and [fields[k]['value']for k in [0,2,3,4,5,6]]==[1,0,1,8,0,1]and bytes.fromhex(fields[1]['value'])==b'Local Lobby'
            role='Hostpeerdefaultreset'if index==79 else'Guestselfroomlist';role+=' basedonacceptedservercontext, notpayloadrolebit'
        assert r['direction']=='S'
        captures.append({'source':frame_pin,'array_index_zero_based':index,'frame_1based':index+1,'epoch':r['epoch'],'direction':'S','opcode':body[0],
          'body_hex':body.hex(),'body_bytes':len(body),'frame_bytes':r['frame_bytes'],'fields':fields,'consumed':len(body),'remaining':0,'source58_version':version,
          'role_reference':role,'classification':'Knownwire newsealedtrace corroboration, nonewpacket/schema correction/nativeexecution'})
    assert captures[2]['body_hex']==captures[3]['body_hex']and not any(x.get('direction')=='S'and x.get('body_hex','').startswith('cf')for x in rows[77:81])
    wire={'direction':'server-to-client','opcode_hex':'CC','opcode_decimal':204,'handler_VA':'004A7A30','handler_RVA':'000A7A30','fixed_PE_sha256':FIXED,
      'header':'c signeddecoded subtypebody+1; 0..5 valid,unsignedJA excludesnegative/>5','full_row_subtype':0,
      'version_formats':[{'signed63D54C_min':None,'format':fmt_old,'literal_VA':'006343DC','fields':'droom,sname,cattributes,cdisplayusers,ccapacity,hmapword,cmode'},
        {'signed63D54C_min_hex':'9EA8','format':fmt_new,'literal_VA':'006343E4','fields':'droom,sname,cattributes,hdisplayusers,hcapacity,hmapword,cmode'}],
      'endianness':'d/u32 andh/u16 little-endian; c singlebyte; s opaque8bitNUL','string_capacity_including_NUL':100,
      'encoding':'Nativebyteparserdoesnotnamecodec; tracesASCII, JavaMS950authorityseparate','known_new_format_body_bytes':'15+N','frame_bytes':'17+N','old_format_body_bytes':'13+N',
      'field_offsets_new':'opcode0, subtype1,room2..5,name6..NUL;attributesafterNUL,usersnextu16,capacitynextu16,mapnextu16,modebyte',
      'absent_fields':['channel/epoch/generation','joinedroomadmission','owner/physical/sourceID','full-listcount/terminator','reset/self-listrolebit','ownexitACK'],
      'phase_room_gate':'Neither64AF98phasewhitelistnor64ABF4joinedroomcheck inhandler; bothdispatch selectorscall; UI64A958catalogcontext/runtimeguardunknown',
      'trigger':'Existingdefaultreset/list samepayload, rolesfromacceptedserverrecipients/triggerreference; othercatalogpartialupdatesknownrawonly','evidence':'1143bytecompletehandler/directUIhelpers +2sealedCC26/28; noNative/UIacceptance',
      'unknown':['actualnativeversion/phase/UI/catalog/currentchannel/epoch','externalstringbounds/checkedread/exactend','mapindexresource/invalidmodeflags','defaultresetrowreplacementNativevsadapterpolicy','physical/source/owner/session/ownleaveauthority']}
    row={'helper':'00465940 complete173bytes','room_key':'fullDWORDrow+4==incomingDWORD, base450BC0proven; notWORD','existing_row_when_UI68_zero':'firstmatchingkey→4659DAreturn, noconstructor/append/fieldoverwrite/textrefresh; subtype0NOTreplace',
      'when_UI68_nonzero':'skipdedup, attemptappendduplicateifcount<256; flagmeaning/lifetimeunknown','absent_row':'ifcount>=256return; otherwiseallocate124bytes,463C10constructor,450260append. UI+5Ccapacityindependentlyguardsappend; callerdoesnotensureUIinitialized/capacity.',
      'allocation_failure':'constructor skippedandNULLpassedtoappendhelper; noacceptednonnull/append/ownership proof','selection':'rowadd/constructor/append do notwriteUI+28 selectedindex or64A98CselectedpreviewWORD; defaultresetreceiptdoesnotguaranteeoldselectedrowrefresh',
      'constructor_fields':{'roomDWORD':'+4 via450BC0','iconDWORD':'+8','titlepointer':'+10 via463D10','displayusersDWORD':'+6C','capacityDWORD':'+70','mapBYTE':'+74','attributesBYTE':'+75','flagsDWORD':'+78','row_vtable':'4F8C14; diskvirtual+0C→463D10notruntimeproof'},
      'title_helper':'463D10 complete44bytes calls external773C7310 ifoldtitle!=0 thenexternal77406880(inputNULstring)storesreturn+10; provider/heapoperation/stringcopy success notqualified.',
      'fullrow_UI_maps':'4A7EC0 attrsbit2=>icon7 elsebit4=>6 else5;4A7EE0 attrsbit2=>flagslow2 else1;4A7EF0 mode0/1/2/3/4/5=>2000/200/400/1000/800/100hex;invalidmode signednegative/>5 returnsuninitializedlocalDWORD, noauthority.',
      'map_projection':'new>=9EA8 ordinarymapWORD signextendscan64AFD0count/64AFE0stride36+20key, firstmatchindexor0ifabsent; mapFFFF/FF9C uses lowbyte(map)-mode-1 modulo256. Olderordinarymaplowbyte+0C;special>=7794 FE-modeelse-1. RawWORD!=mapbinding/UIindexauthority.',
      'defaultreset_adapter':'ExplicitAD368consumer mayreplacecatalogrowbyid withreceivedfullrow underownedchannel/epoch policy. This isadapterprojection, notNative465940 replacement orjoinedroomcommit.'}
    patches={'scope':'Onlycatalogpartialrows nojoinedsettings/UI/workflow expansion','subtypes':[{'subtype':1,'schema':'ds','effect':'matchingrowname via465B00 virtual+0C; fullDWORDkey; allmatches, absentnoappend'},
      {'subtype':2,'schema':'dc','effect':'icon/attributes/lowflags matchingrows via465AC0/465C10/465CD0/465C90; getterfirst+78 returnsinputroomIDifabsent, nofallbackrowcreation'},
      {'subtype':3,'schema':'dh if>=9EA8 else dc','effect':'matchingcapacity+70 via465B80/463D50'},
      {'subtype':4,'schema':'dh if>=9EA8 else dc','effect':'matchingdisplayusers+6C via465B40/463D40'},
      {'subtype':5,'schema':'dhc','effect':'matchingmapBYTE+74 andmodeflags+78 via465C50/465CD0/465C90; absentnoappend'}],
      'not_full_snapshot':'Noend/count/clear ofmissingrows; fullrowsareincrementalappend/dedupNative, partialsnotfullreplacement. RepeatednumericID/staleepoch ambiguity remains.',
      'no_deltas_adopted_as_actions':'NoJavachange/extrauserroomsettingsaction qualification; partialbranchesonlycloseddirectcatalogdataflow, runtimeUIunknown.'}
    separation={'catalog_vs_joined':'CompleteCCmainaddressesUI64A958, not64ABF4/64ABF8/roommembervectors/ownerdisplay/readytransition; no469CA0 or46A540call inCChandler. Catalogrowdoesnotgrantjoin/session/owner/source/physicalauthority.',
      'DWORD_vs_WORD':'Catalogkey+4 fullDWORD/450B90 returnsselectedindex. ExistingCFselectedpreviewUI64A98C+0C MOVSXi16comparison isdifferentstorage, notcatalogIDnormalization. Do nottruncate/replace32bitIDwithsignedWORD; selectedwidgetproducer/runtimeunknown.',
      'CF_new_reference':'TwoS_CFcf02000000nowsealedin626B77E8 array74Host/75Guest, body5/frame7; corroborateknownBatch18deleteformat. Batch18C8E20CD0 historicalzeroCFcapturestatementremainsunchanged.',
      'CF_guard':'Primarymode4/phase>=3 defaultC_QUIT/private05AD0986effectstillunknown; fallbackcatalogdelete/selectedpreview0 knownstaticonly. Guestjoinedroom1 receivesdelete2, catalogdeleteisnotownleaveACK.',
      'CC_reset_list_roles':'Array79Hostreset and80Guestselflist identicalbody; absenceofCFdefaultreset androom1preserved LocalLobby/cap8 referencesJava source/trace context. NeitherCCalone normergedpeerindicesprovesownleavecommit.',
      'server_poststate':'4CB29A6D report referencesindependentGETpoststate, remainsservergroupsqualification. Thisgroupreadssealedfilesonly, noSQL/GET/native ornewacceptance.'}
    helpers={'bounded_direct_calls':[{**w(at),'scope':'directcataloghelperorunchangedexistinghelper; external/virtualruntimeeffectsunknown'}for at in sorted(ins)if ins[at]['mnemonic']=='call'and any(lo<=at<hi for lo,hi in [(0x4A7A30,0x4A7EA7),(0x465940,0x4659ED),(0x463C10,0x463C88),(0x450260,0x450283),(0x463D10,0x463D3C)])],
      'unknowns':['UI64A958/flag+68/count/capacity/pointerarray initialization/lifetime','allocationNULL, externaltitleoperation/module/body','virtualrow+0C actualvtable andwindowcallbacks','maplookup64AFD0/64AFE0generation/currentversion','invalidmodeuninitializedflags','selectedindex/previewWORDproducer andstaleprojection','Nativeexactend/codec/epoch/channelscope','CFprimaryprivateexecution/termination','races/sendfailures/numericroomreuse','joinedroom/source/physical/owner/session/ownexitcommitnotcatalogauthority']}
    consumer={'catalog':'ExplicitAD368 fullCC0 row upsert/replace currentchannel+transportepoch catalog byu32ID, preservingjoinedcontext; defineprojectionpolicyseparatefromNative duplicateignore. Unknownpartialidsnoimplicitrow unlessseparatelyqualified.',
      'reset':'Owner/member/ready/currentjoinedstate absentfromCC. ResetpeerCC79/selflist80 samedata, rolescontextonly; nofixedrowcount/terminator/ownleaveACK. MissingrowsnotdeletedbyCCfullrow.',
      'delete':'KnownCF2 removesonlycatalog2 currentchannel/epoch, keepGuest joinedroom1; sameIDreusegenerationunknown. Nativeprimaryprivatepath notadapterauthority.',
      'guard':'KeepUI/private/currentcontext/source/physical/ownerguards; nozero/ASCIIcomparison orNativeUIinjection, noJavawirechange demanded.'}
    verification={'instruction_assertions':len(assertions),'new_code_ranges':23,'new_data_ranges':12,'inherited_raw_ranges':len(inherited),'CC_handler_bytes':1143,'row_add_helper_bytes':173,'row_constructor_bytes':120,
      'sealed_CC_frames':2,'sealed_CF_additional_frames':2,'prior_primary_pins':20,'all_match':True,'server_packet_unchanged':packet,'old_test_replayed':False}
    outputs={'ranges.json':{'source':sources[0],'ranges':ranges,'inherited_raw':inherited},'instruction-witnesses.json':{'assertions':assertions},'CC-wire-and-version.json':wire,
      'catalog-existing-absent-row.json':row,'catalog-partial-row-fields.json':patches,'catalog-joined-selection-separation.json':separation,
      'sealed-CF-CC-frames.json':{'captures':captures,'same_CC_payload_roles_differ_by_context':True},'direct-helper-boundaries.json':helpers,'consumer-gap.json':consumer,'new-evidence-verification.json':verification}
    for n,o in outputs.items():write('SCC-catalog/'+n,o)
    primary={'schema':'lito.native-SCC-catalog-boundary.v1','updated_at':stamp(),'status':'FINITE_STATIC_CATALOG_BOUNDARY_SEALED_NATIVE_UI_ROWREPLACEMENT_AND_JOINED_AUTHORITY_OPEN',
      'sources':sources,'prior_primary_pins':prior,'existing_evidence':existing,'reusable_outputs':[record(p)for p in sorted(OUT.iterdir())if p.is_file()and p.name not in ['independent-review.json','READ-ME.txt']],
      'dispatch':{'primary':'4B353C=74hexslot116→4B33D8→4B2F13','fallback':'4B3780=2Ehexslot46→4B3628→4B2F13','call':w(0x4B2F15)},
      'wire':wire,'catalog_row':row,'catalog_patches':patches,'separation':separation,'direct_helpers':helpers,'consumer_gap':consumer,'verification':verification,
      'delta':{'new_wire':[],'wire_corrections':[],'new_opcodes':[],'known_corroboration':['CC0versionedcatalogrow/defaultreset/list','2CFdeleteframesaddreferencetoBatch18withoutrewritingit'],
        'internal_boundary':'NativeUI68==0 duplicateCC0 ignored, notrowreplacement; adapterdefaultresetupsert isexplicitpolicy'},
      'scope':{'writes':'ownresourcefolderonly','native_started':False,'GUI_input':False,'SQL_or_provider_or_capture':False,'Java_taken_over':False,'WORLD_or_intercepted_work_followed':False,
        'other_room_settings_expanded':False,'physical_source_owner_session_or_ownexit_authority':False,'overall_complete':False}}
    write('SCC-catalog-contract.json',primary);print(json.dumps({'primary':record(ROOT/'SCC-catalog-contract.json'),'verification':verification},ensure_ascii=False))

if __name__=='__main__':main()

"""Batch15 finite S_CD member admission/comparator qualification; no execution."""
from audit import *
OUT=ROOT/'SCD-member-comparison'
FIXED='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
PREVIOUS='2256D57DC068C669E41BFFACB9F39D0E26E83C652D8B987F7524B3AAA2B178B4'
MODERN=ROOT.parent/'lito重建版/LiTo381-Modern'
LOG=MODERN/'logs/continuation-20261003'

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    OUT.mkdir(exist_ok=True);pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    sources=[record(p)for p in [PE,BIN,ASM]]
    assert [x['sha256']for x in sources]==[FIXED,'CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292','2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A']
    previous=load('SCE-navigation-contract.json');pin=record(ROOT/'SCE-navigation-contract.json');assert pin['sha256']==PREVIOUS
    prior=previous['prior_primary_pins']+[{**pin,'expected_sha256':PREVIOUS,'match':True}]
    for p in prior:assert record(Path(p['path']))['sha256']==p['expected_sha256']
    assert len(prior)==16
    existing={};native=LEGACY/'analysis'
    for key,path in [
      ('native_dispatch',native/'NATIVE-S-DISPATCH.json'),
      ('participant_state',native/'NATIVE-LITO-PARTICIPANT-STATE-DOMAIN-EVIDENCE-0.7.689.json'),
      ('primary_tail',native/'NATIVE-LITO-ROOM-PARTICIPANT-PRIMARY-TAIL-EVIDENCE-0.7.637.json'),
      ('ready_grade',native/'NATIVE-LITO-ROOM-PARTICIPANT-STATUS-GRADE-EVIDENCE-0.7.635.json'),
      ('Modern_R496_evidence',LOG/'r496-source-evidence.json'),
      ('Modern_R496_handoff',LOG/'r496-native-version-actor-admission-selection-controls-handoff.json'),
      ('Modern_R513_frozen_index',LOG/'r513-production-source-snapshot.json')]:existing[key]=record(path)
    assert existing['Modern_R496_evidence']['sha256']=='36937E8CD135D2F355236A9F442D10EC7DD4E7279B016443F653620965C65A91'
    frozen=json.loads((LOG/'r513-production-source-snapshot.json').read_text(encoding='utf-8'))
    for filename in ['native_source_actor.rs','native_city_runtime.rs']:
        r=next(x for x in frozen['source_pins']if x['path'].endswith(filename));p=Path(r['snapshot']);q=record(p)
        assert q['sha256']==r['sha256'];existing['frozen_R513_'+filename]=q
    packet=record(ROOT/'server-packet-delta.json');assert packet['sha256']=='B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C'
    historical={'manifest':record(ROOT/'artifact-manifest.json'),'delivery':record(ROOT/'delivery-verification.json'),'scope':'Historical pre-batch aggregates, not continuing core dependencies'}
    ins={};owners={};ranges=[];inherited=[]
    def decode(raw,va,q):
        rows=[]
        for i in cs.disasm(raw,va):
            r={'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str};rows.append(r);ins[i.address]=r;owners[i.address]=q
        assert sum(len(bytes.fromhex(x['raw_hex']))for x in rows)==len(raw),(hex(va),'decode boundary')
        return rows
    def reuse(va,q,n,kind,index,offset=0):
        raw=Path(q['path']).read_bytes();assert record(Path(q['path']))==q and raw==pe.get_data(va-0x400000,len(raw))
        inherited.append({'va':f'{va:08X}','range':q,'index':index,'used_bytes':n,'used_offset':offset,'raw_matches_fixed_PE':True,'old_test_or_capture_replayed':False})
        if kind=='code':decode(raw[offset:offset+n],va+offset,q)
    old=json.loads((LOG/'r496-source-evidence.json').read_text(encoding='utf-8'))
    for va,n in [(0x4A7F60,0x34B),(0x4A83ED,0x24),(0x4D39F0,0x87),(0x4D3A80,0x5A),(0x4A33E0,0x76),(0x4A9550,0x58)]:
        q=next(x for x in old['PE_fragments']if Path(x['path']).name==f'pe-{va:X}.bin');reuse(va,q,n,'code',existing['Modern_R496_evidence'],0x127 if va==0x4A33E0 else 0)
    for name,va,n,kind in [('lifecycle/ranges.json',0x480A60,0x2D,'code'),('priority-complete-tails.json',0x4A2A50,0x128,'code'),
      ('SCE-navigation/ranges.json',0x4B29DB,0x38,'code'),('SCE-navigation/ranges.json',0x4B313F,0x1E,'code'),
      ('SCE-navigation/ranges.json',0x4B3B80,3,'code'),('S0C-status-owner/ranges.json',0x4F118C,6,'code')]:
        q=next(x['output']for x in load(name)['ranges']if int(x['va'],16)==va);reuse(va,q,n,kind,record(ROOT/name))
    specs=[(0x4A82AB,0x142,'code','S_CD_record_build_gap'),(0x4A8411,0x78A,'code','S_CD_phase_and_incremental_member_tail'),
      (0x4B2F1F,0xC,'code','S_CD_shared_dispatch_call'),(0x4B4C90,0x14,'code','copy140byte_record'),
      (0x4B38F0,0x23,'code','record_vector_size'),(0x4B4660,0x40,'code','record_range_copy'),
      (0x4B46A0,0x33,'code','record_repeat_copy'),(0x450B90,0x25,'code','phase1_selected_row_lookup'),
      (0x4A966D,0xF4,'code','S_D1_member_update_only_no_self_world_branch'),
      (0x465530,0x46,'code','UI_name_match_state_update'),(0x463BD0,0xD,'code','UI_state6C_setter'),
      (0x4D3ADA,3,'code','complete_comparator_final_RET8_gap'),(0x4F103C,0xB,'code','free_wrapper'),(0x4F1186,6,'code','free_external_thunk'),
      (0x4B353D,1,'data','CD_primary_map'),(0x4B33DC,4,'data','CD_primary_jump'),
      (0x4B3781,1,'data','CD_fallback_map'),(0x4B362C,4,'data','CD_fallback_jump'),
      (0x4F727C,4,'data','fold_function_pointer_snapshot'),(0x834960,256,'data','disk_fold_snapshot_NOT_runtime'),
      (0x834A60,256,'data','disk_lead_snapshot_NOT_runtime'),
      (0x6343EC,15,'data','lowest_version_format'),(0x6343FC,16,'data','version52E0_format'),
      (0x63440C,17,'data','version75A4_format'),(0x634420,17,'data','version7726_format'),(0x634434,18,'data','version9D7C_format')]
    for va,n,kind,label in specs:
        raw=pe.get_data(va-0x400000,n);assert len(raw)==n;p=OUT/f'{va:08X}-{n:X}.bin';p.write_bytes(raw);q=record(p)
        ranges.append({**mapping(pe,va),'bytes':n,'kind':kind,'label':label,'output':q,'raw_hex':raw.hex(),'instructions':decode(raw,va,q)if kind=='code'else[]})
    def w(at):return {'fixed_PE_sha256':FIXED,**mapping(pe,at),**ins[at],'range':owners[at]}
    expected=[(0x480A83,'xor','ecx, ecx'),(0x480A88,'call','0x4d39f0'),
      (0x4D39FE,'call','0x5010b41'),(0x4D3A03,'mov','ecx, 0x40'),(0x4D3A0A,'mov','edi, 0x834a60'),
      (0x4D3A0F,'rep stosd','dword ptr es:[edi], eax'),(0x4D3A11,'cmp','dword ptr [ebp - 0x14], 1'),
      (0x4D3A15,'jbe','0x4d3a54'),(0x4D3A21,'cmp','byte ptr [edx], 0'),(0x4D3A35,'lea','edi, [eax + 0x834a60]'),
      (0x4D3A3C,'mov','eax, 0x1010101'),(0x4D3A4F,'add','edx, 2'),(0x4D3A54,'mov','edi, dword ptr [0x4f727c]'),
      (0x4D3A5D,'call','edi'),(0x4D3A62,'mov','byte ptr [esi + 0x834960], al'),(0x4D3A69,'cmp','esi, 0x100'),
      (0x4A3511,'mov','al, byte ptr [0x64b6dc]'),(0x4A3516,'mov','ebx, 0x3b6'),(0x4A351D,'mov','esi, 0x3a4'),
      (0x4A3522,'mov','edi, 0x3a8'),(0x4A352B,'call','0x4d39f0'),(0x4A353E,'call','0x4d39f0'),
      (0x4A354F,'mov','ecx, 0x3b5'),(0x4A355C,'call','0x4d39f0'),(0x4A3569,'mov','ecx, 0x4e4'),
      (0x4A356E,'call','0x4d39f0'),(0x4A3573,'mov','dword ptr [0x64b6d8], 0'),
      (0x4D3A96,'mov','dl, byte ptr [eax + 0x834a60]'),(0x4D3A9D,'test','dl, dl'),(0x4D3AA1,'sub','eax, edi'),
      (0x4D3AAF,'sub','eax, edx'),(0x4D3AB7,'mov','dl, byte ptr [edi + 0x834960]'),
      (0x4D3AC1,'mov','dl, byte ptr [eax + 0x834960]'),(0x4D3AC7,'sub','edx, edi'),
      (0x4D3ACB,'test','eax, eax'),(0x4D3AD2,'ret','8'),(0x4B2F21,'call','0x4a7f60'),
      (0x4A82B1,'mov','dword ptr [ebp - 0x104], eax'),(0x4A8384,'mov','byte ptr [ebp - 0xc0], dl'),
      (0x4A83F0,'push','0x64abf8'),(0x4A83F6,'call','0x4d3a80'),(0x4A83FD,'jne','0x4a8411'),
      (0x4A840A,'mov','edi, 0x64abf4'),(0x4A840F,'rep movsd','dword ptr es:[edi], dword ptr [esi]'),
      (0x4A8411,'mov','eax, dword ptr [0x64af98]'),(0x4A8416,'cmp','eax, 1')]
    extra=[(0x4A8592,'mov','edx, dword ptr [0x64abf4]'),(0x4A8598,'mov','eax, dword ptr [ebp - 0x14]'),
      (0x4A859B,'cmp','edx, eax'),(0x4A859D,'jne','0x4a8b94'),
      (0x4A860C,'mov','edi, dword ptr [0x64ab54]'),(0x4A8643,'call','0x4d3a80'),(0x4A864A,'je','0x4a8b94'),
      (0x4A88EC,'mov','edi, dword ptr [0x64ab64]'),(0x4A8923,'call','0x4d3a80'),(0x4A892A,'je','0x4a8b94'),
      (0x4A857F,'cmp','eax, 2'),(0x4A8584,'cmp','eax, 3'),(0x4A8589,'cmp','eax, 6'),
      (0x4A86FD,'call','0x4f118c'),(0x4A876C,'call','0x4f103c'),(0x4A87E3,'call','0x4b4660'),
      (0x4A881A,'call','0x4b46a0'),(0x4A88DB,'call','0x4651b0'),(0x4A8B8F,'call','0x4651b0'),
      (0x4B4C98,'mov','ecx, 0x23'),(0x4B4C9F,'rep movsd','dword ptr es:[edi], dword ptr [esi]'),
      (0x4B4677,'mov','ecx, 0x23'),(0x4B4680,'rep movsd','dword ptr es:[edi], dword ptr [esi]'),
      (0x4B46B9,'mov','ecx, 0x23'),(0x4B46C2,'rep movsd','dword ptr es:[edi], dword ptr [esi]'),
      (0x4A96C6,'mov','byte ptr [edx + eax*4 + 0x44], cl'),(0x4A96D9,'call','0x465530'),
      (0x4A9742,'mov','byte ptr [edx + eax*4 + 0x44], cl'),(0x4A9755,'call','0x465530'),
      (0x465552,'call','0x4d3a80'),(0x465562,'call','0x463bd0'),(0x463BD6,'mov','dword ptr [ecx + 0x6c], eax'),
      (0x4F118D,'jmp','0x773c4c20'),(0x4F1040,'call','0x4f1186'),(0x4D3ADA,'ret','8')]
    expected+=extra
    assertions=[]
    for at,mn,op in expected:
        assert (ins[at]['mnemonic'],ins[at]['operands'])==(mn,op),(hex(at),ins.get(at))
        assertions.append({**w(at),'expected_mnemonic':mn,'expected_operands':op,'match':True})
    rawdata={int(x['va'],16):bytes.fromhex(x['raw_hex'])for x in ranges if x['kind']=='data'}
    assert rawdata[0x4B353D]==b'\x75' and rawdata[0x4B3781]==b'\x2f'
    assert struct.unpack('<I',rawdata[0x4B33DC])[0]==struct.unpack('<I',rawdata[0x4B362C])[0]==0x4B2F1F
    assert struct.unpack('<I',rawdata[0x4F727C])[0]==0x773B2A40
    assert rawdata[0x834960]==bytes(256) and rawdata[0x834A60]==bytes(256)
    # Indexed existing raw establishes format pointers; do not recapture those strings.
    versions=[]
    for value,at,va,fmt in [(None,0x4A8295,0x6343EC,'dscccdddcscsdc'),(0x52E0,0x4A8246,0x6343FC,'dscccdddcscsdcc'),
      (0x75A4,0x4A81D6,0x63440C,'dscccdddcscsdccc'),(0x7726,0x4A8124,0x634420,'dscccdddcscsdccd'),(0x9D7C,0x4A7FCB,0x634434,'dscccdddcscsdccdc')]:
        assert ins[at]['operands']==hex(va) and rawdata[va]==fmt.encode()+b'\0'
        versions.append({'signed_version_min':value,'format':fmt,'format_data_va':f'{va:08X}','push_witness':w(at)})
    fmt='dscccdddcscsdccdc';captures=[]
    paths=[Path(r'D:\lito-dev-runtime\20261003-supervision\server\battle-skill-network-f0bd3a9dfbf741628b39792b065ff7ba\evidence\frames.json'),
      Path(r'D:\lito-dev-runtime\20261003-supervision\server\battle-return-skill-network-c66ddebd66a74679bcb88a01516eff5f\evidence\frames.json')]
    expected_hashes=['A9BCA0ECC611F76EE1BD2D6BE6F3B512DC5ABD169C60DACEB6FC74C6D7E0D69F','283A3DBFA77668FDAA0D55CF67239218CF34D546B53968DFFFBD973A8D093004']
    for path,h in zip(paths,expected_hashes):
        q=record(path);assert q['sha256']==h;rows=json.loads(path.read_text(encoding='utf-8'))
        cds=[(i+1,r)for i,r in enumerate(rows)if r.get('direction')=='S'and r.get('body_hex','').startswith('cd')]
        assert len(cds)==4
        for index,r in cds:
            body=bytes.fromhex(r['body_hex']);pos=1;fields=[]
            same_epoch_version=next((v for v in rows[:index-1]if v.get('direction')=='S'and v.get('epoch')==r['epoch']and v.get('body_hex','').startswith('3a')),None)
            assert same_epoch_version is not None
            version=int.from_bytes(bytes.fromhex(same_epoch_version['body_hex'])[3:7],'little',signed=True);assert version>=0x9F0A
            for k,code in enumerate(fmt,1):
                start=pos
                if code=='s':end=body.index(0,pos);value=body[pos:end].hex();pos=end+1
                else:n=4 if code=='d'else 1;value=int.from_bytes(body[pos:pos+n],'little');pos+=n
                fields.append({'index':k,'format':code,'body_offset':start,'wire_bytes':pos-start,'value':value,'wire_hex':body[start:pos].hex()})
            for k in range(12+5):fields.append({'tail_c_index':k,'body_offset':pos,'value':body[pos]});pos+=1
            tail=int.from_bytes(body[pos:pos+4],'little');pos+=4
            assert pos==len(body) and len(body) in [84,85] and r['frame_bytes']==len(body)+2
            assert fields[0]['value']==1 and fields[2]['value']==0 and fields[8]['value']==0
            assert fields[9]['value']=='' and fields[10]['value']==0 and fields[11]['value']==b'Ready'.hex()
            captures.append({'source':q,'frame_1based':index,'epoch':r['epoch'],'direction':'S','opcode':'CD','body_bytes':len(body),
              'body_hex':r['body_hex'],'source58_version':version,'fields':fields,'tail_dword':tail,'consumed':pos,'remaining':0,
              'native_dispatch_or_table_execution_observed':False,'same_dword_all_members':1,'classification':'Known S_CD frozen wire corroboration; not new producer run'})
    assert len(captures)==8
    comparison={'comparator_entry':'004D3A80','lead_table':'00834A60[256]','fold_table':'00834960[256]',
      'algorithm':'NUL-terminated 8-bit compare. If left byte leadtable!=0, compare raw left-right lead and next raw byte, then continue. Otherwise compare fold[left]-fold[right]; if same and left rawbyte0 return0; nonzero continue. No local length/max or validity gate.',
      'initializer':'004D39F0 completeexistingraw: private05010B41 receivesECXnumericparameter and20-byte stackarea; then clear256leadbytes, ifstackDWORD>1 walk inclusivepairs fromarea+6 until00/00; foldloop0..255 calls*[4F727C],storesAL.',
      'private_fixed_target':'05010B41','ASM_target_differs':'05A10EEA islabel-navigationonly; fixedbytes target05010B41 wins',
      'fold_pointer_disk':'773B2A40','dump_time_module_export_body_SHA':None,
      'private_result_checked':False,'table_ready_flag_found_in_bounded_producer':False,
      'startup':{'call':w(0x480A88),'parameter':0},
      'known_S3A_reinitialization':{'selector':'BYTE64B6DC','branches':[{'selector':3,'ECX':950,'call':w(0x4A352B)},{'selector':4,'ECX':932,'call':w(0x4A353E)},
        {'selector':5,'ECX':936,'call':w(0x4A355C)},{'selector':0,'ECX':949,'call':w(0x4A356E)},{'selector':'other','ECX':1252,'call':w(0x4A356E)}],
        'scope':'Necessary tableproducer boundary inexistingS3A raw only; no auth/login effect ornew opcode expansion',
        'lifecycle':'Globaltablecontents may changeafterstartup/onS3A. Tableauthority cannot be retained blindly across sourceversion/locale changes ornewowned connection.'},
      'disk_snapshots':'Both256byte tables arezero in fixedPE. This isnot a valid runtime comparison authority andmustnever be installed as it.',
      'validity_minimum':['Privatecallactuallyreturns boundedinitializedarea/terminator/ranges','All256 foldfunctioncalls finish withqualifiedprovider/module/codepage/locale outputs',
        'Immutable lead/fold snapshot boundto its initialization event and currentownedepoch/locale generation','Wellformed boundedNUL strings; multibyte lead neverhasmissingtrail; no guessedencoding/ASCII/plain-equality substitution'],
      'native_enforces_validity':False,'actual_runtime_tables_available':False,'Modern_comparison_guard_retained':True,
      'limits':'No originalDLLbody,privateimplementation orlocale-change execution. Nameddirectwriter evidence doesnot provealias/write completeness orlifetime synchronization.'}
    identity={'first_dword':'Wire d isroom_id in these room-phase frames: everymemberhas1, notphysicalactor/object/source identifier. Preserve context ofoldR496 actoridentifier label; no wiretype change.',
      'global_match_order':'Beforephase/roomgate, incomingname compares64ABF8; zero copies140byteslocalrecord→64ABF4 includingfirstdword/name. This canreplace64ABF4 before laterroomguard; no global-readiness claim.',
      'phase1':'Selected64A958 row via450B90; incomingfirstd mustmatchselectedrow+4, withoriginalsentinel empty-name path. Incrementalpreview64ACF4/F8 stride8C, duplicatecompareentry+4 returnswithoutadd. UI/list prerequisites remainunknown.',
      'room_phases':'Only64AF98 2/3/6 admitted; then firstd vs64ABF4; kindc3=0 routes64AB60 begin64AB64/end64AB68/cap64AB6C;kind1 routes64AB50 begin64AB54/end64AB58/cap64AB5C;kind2notificationonly; otherreturn.',
      'identity_key':'Currentroomcontext + selectedkindvector + comparator(name,existingrecord+4)==0. Firstd sameforallmembers in capture; string notphysical/source ID. No member GUID/owned epoch/opaque revision in nativebody.',
      'duplicate_S_CD':'Bothkindvectors scanbeforeappend; comparatorzero returns4A8B94. Existingrecord andUIinitialready NOTupdated by repeatedS_CD. Exception: matchingselectednameglobal140bytecopy occurred earlier, so cannot claim allglobalsunchanged.',
      'new_S_CD':'Append140-byte record ifnonduplicate; inlinecapacity/growth branchuses4F118C allocator and4F103C free withunqualifiedexternalbodies;4B4C90/4B4660/4B46A0 local140bytecopies;then4651B0 UI append depthboundary.',
      'ready_record':'S_CD field9 c goeslocalrecord+44;field15 previewUIstate separate. Existingparticipantstate0/1/2 includesSHOP_LIST2 (not boolean-ready1).',
      'record_representation':'140bytes isinternalstride/copysize, notnetworkbodylength. Strings copiedthroughNUL andsomepadding/tails mayretainuninitializedstackbytes; do notserializeinternals aswire orclaimall140bytes arevalidownedstate. CapturedASCII Ready isfield12string, distinctfromfield9numericinitialstate0.',
      'later_ready':'Existing S_D1 sc handler4A9550; onlymembertail4A966D..4A9760 added here. Comparatorzero writesrecord+44 forfirstmatchingcombatant thenreturn, otherwisefirstmatchotherkind. 465530 scansallmatchingUItext item+10 and463BD0 writesDWORD item+6C withMOVSXstatus. Self-name earlierphase/world/callback effects remainunqualified andnotexpanded.',
      'ready_helper_boundary':'S_D1 selfmatchescaninvokephase/world effectsbeforemembertail; minimalmembertail isnotfullS_D1 admission qualification. No countdown S_D0/results examined.',
      'Modern_minimum_gap':'A structural AD368 roomadapter may retainopaque roommembers/displaynamebytes independently. Exactnativeidentity/dedup/statuseffects requirequalifiedtables+initializedvector/UI/currentroomstate. Require ownedepoch/revision androommatch BEFORE publication; do not emulate nativepre-gate globaloverwrite as authorization.',
      'physical_source_owner_player_ready':False,'runtime_object_vectors_or_callbacks_accepted':False}
    calls=[]
    for r in ranges:
        for i in r['instructions']:
            if i['mnemonic']=='call':calls.append({'callsite':w(int(i['va'],16)),'classification':'Direct helper rawincluded/reused or explicit depthboundary; no execution qualification','target':i['operands']})
    boundaries={'initializer_private':w(0x4D39FE),'fold_dynamic':w(0x4D3A5D),'bodies_unavailable':['05010B41','773B2A40'],
      'handler_depth_stops':['4F118C allocation','4F103C free','4F1592 floating conversion','4651B0 UI append','4B4020 previewvector insertion','773F5200 formatting','458BB0 notification'],
      'S_D1_scope':'Membertail plusUIstate helper only. Existingprefix reused; self/world branches areknown boundary, no newworld/readytransition completion claim.',
      'calls_in_new_slices':calls,'comparison':comparison,'identity':identity}
    # Counterexample demonstrates why the fixed disk table cannot authorize equality.
    zero_fold_equal=sum(1 for a,b in zip(b'Alice\0',b'BOBBY\0')if bytes(256)[a]!=bytes(256)[b])==0
    assert zero_fold_equal
    models=[{'case':'zero_disk_fold_false_equality','left_hex':b'Alice\0'.hex(),'right_hex':b'BOBBY\0'.hex(),'all_nonlead_foldbytes_zero':True,'modeled_equal':True,'runtime_authority':False},
      {'case':'allmembers_same_firstDWORD','capture_count':len(captures),'unique_firstDWORDs':sorted(set(x['fields'][0]['value']for x in captures)),'expected':[1],'physicalID_inferred':False},
      {'case':'140byte_stride','recordDWORDs':35,'record_bytes':35*4,'expected':140},
      {'case':'CD_initial_state0_vs_later_state1','CD_field9_values':sorted(set(x['fields'][8]['value']for x in captures)),'CD_repeatedname_updates_ready':False,'later_S_D1_effect_qualified_only_in_membertail':True}]
    write('SCD-member-comparison/ranges.json',{'source':sources[0],'ranges':ranges,'inherited_raw':inherited,'scope':'CompleteS_CD assembledfromexistingprefix/globalcopy andnewgaps; comparisonproducer reused; onlynecessarymemberreadytail/directhelpers'})
    write('SCD-member-comparison/instruction-witnesses.json',{'assertions':assertions,'count':len(assertions)})
    write('SCD-member-comparison/table-qualification.json',comparison)
    write('SCD-member-comparison/incremental-identity-and-ready.json',identity)
    write('SCD-member-comparison/sealed-CD-frames.json',{'captures':captures,'new_capture':False,'S_D0_countdown_results_expanded':False})
    write('SCD-member-comparison/direct-helper-boundaries.json',boundaries)
    write('SCD-member-comparison/new-code-derived-models.json',{'models':models,'old_tests_replayed':False,'native_runs':0})
    verification={'instruction_assertions':len(assertions),'new_code_ranges':sum(x['kind']=='code'for x in ranges),'new_data_ranges':sum(x['kind']=='data'for x in ranges),'inherited_raw_ranges':len(inherited),
      'sealed_CD_frames':8,'primary_fields_per_frame':17,'tail_c_per_frame':17,'tail_d_per_frame':1,'prior_primary_pins':prior,
      'server_packet_unchanged':packet,'historical_prebatch_aggregates':historical,'all_match':True,'original_native_GUI_player_accepted':False,'other_group_writes':0}
    write('SCD-member-comparison/new-evidence-verification.json',verification)
    for p in existing.values():assert record(Path(p['path']))==p
    report={'schema':'lito-unpack/SCD-member-comparison/v1','updated_at':stamp(),'status':'FINITE_SCD_MEMBER_TABLE_QUALIFICATION_BOUNDARIES_SEALED_GUARD_RETAINED',
      'sources':sources,'prior_primary_pins':prior,'existing_evidence':existing,'inherited_raw':inherited,'reusable_outputs':[record(p)for p in sorted(OUT.iterdir())if p.is_file()],
      'dispatch':{'primary':'4B353D=75hex/117→4B33DC→4B2F1F','fallback':'4B3781=2Fhex/47→4B362C→4B2F1F','call':w(0x4B2F21),'both_dispatch_tables_handle_S_CD':True},
      'wire':'Alreadyknownversioned dscccdddcscsdc family; signedversion>=9D7C adds12c, >=9F0A adds5c+d. 8sealedoriginalframes confirm17primaryfields+17tailc+taild,body84/85. Exactboundeddecoder isModern strictadaptation, notnative inputlength validation.',
      'comparison':comparison,'version_formats':versions,'identity_and_incremental_ready':identity,'direct_boundaries':boundaries,'verification':verification,
      'Modern_frozen_comparison':'R513 snapshot hashverified. Originalsrc paths in frozenindex arehistoricalmetadata; notmutablecoredependencies. Dynamiccomparison guard unchanged by thisbatch.',
      'classification':'KNOWN_S_CD_WIRE_CORROBORATION_AND_NATIVE_IDENTITY_TABLE_BOUNDARY_QUALIFICATION','new_wire_differences':0,'new_opcodes':0,'wire_corrections':0,
      'server_handoff':{'packet_delta':packet,'modified':False,'Java_producer_work_taken_over':False},
      'future_boundary':['Countdown/battleS_D0 andresults notexpanded','S_D1 self/world transitions,UI/vector initialization,privateproviders remainunqualified'],
      'constraints':{'write_root':str(ROOT),'other_group_writes':0,'native_live_capture_SQL_GUI_input_injection':0,'old_PASS_replays':0,'physical_source_owner_ready_claims':0}}
    write('SCD-member-comparison-contract.json',report)
    print(json.dumps({'primary':record(ROOT/'SCD-member-comparison-contract.json'),'verification':verification},ensure_ascii=False))

if __name__=='__main__':main()

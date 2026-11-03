"""Batch10 bounded S0C numeric decoder/owner boundary; no native or Java runs."""
from audit import *

OUT=ROOT/'S0C-status-owner'
FIXED='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
BATCH9='E288BB0EA62C8C014E50BB87542B3F800E30B18B10D96D073DFCF27EC7FF81C6'
MODERN=ROOT.parent/'lito重建版/LiTo381-Modern'
SERVER=ROOT.parent/'lito伺服器/LiTo381-Server/src/main/java/local/lito381/server'
HANDOFF=MODERN/'logs/continuation-20261003/r509-character-selection-channel-handoff.json'
FMT='dcdcccccchhhhcdcchcccc'
def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    OUT.mkdir(exist_ok=True);fixed=record(PE);assert fixed['sha256']==FIXED;pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    previous=load('skill-pane-layer-composition-contract.json');assert record(ROOT/'skill-pane-layer-composition-contract.json')['sha256']==BATCH9
    preserved=previous['prior_primary_pins']+[{**record(ROOT/'skill-pane-layer-composition-contract.json'),'expected_sha256':BATCH9,'match':True}]
    assert all(record(Path(p['path']))['sha256']==p['expected_sha256']for p in preserved)
    ranges=[];ins={};owners={};inherited=[]
    def install(row):
        for x in row['instructions']:
            item=dict(x);item['raw_hex']=item.pop('bytes',item.get('raw_hex'));va=int(item['va'],16);ins[va]=item;owners[va]=row['output']
    def capture(label,va,n,kind='code'):
        m=mapping(pe,va);assert m['raw_backed'];raw=pe.get_data(va-0x400000,n);ds=[]if kind=='data'else list(cs.disasm(raw,va))
        assert len(raw)==n and(kind=='data'or sum(i.size for i in ds)==n),(label,hex(va),n)
        p=OUT/f'ranges/{va:08X}-{n:X}.bin';p.parent.mkdir(exist_ok=True);p.write_bytes(raw)
        row={**m,'label':label,'kind':kind,'bytes':n,'output':record(p),'decoded_bytes':sum(i.size for i in ds),
          'instructions':[{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in ds]}
        ranges.append(row);install(row);return row
    for file,va in [('skill-gates/ranges.json',0x4B29DB),('skill-gates/ranges.json',0x4B313F),('skill-gates/ranges.json',0x492D3A),
      ('skill-gates/ranges.json',0x492EDA),('skill-gates/ranges.json',0x4AEE70),('skill-gates/ranges.json',0x4C9190),('receive-boundary/ranges.json',0x4B37C0),('receive-boundary/ranges.json',0x4928C0)]:
        row=next(x for x in load(file)['ranges']if int(x['va'],16)==va);assert record(Path(row['output']['path']))==row['output'];install(row)
        inherited.append({'index':record(ROOT/file),'range':row['output'],'va':row['va'],'bytes_reused_no_old_case_replay':True})
    for x in [('S0C_complete_handler',0x4A36F0,0x17C),('numeric_reader_dispatch_c_h_d',0x4A2A50,0x97),('numeric_reader_loop_return',0x4A2B56,0x22),
      ('protected_value_setter_direct_body',0x4C3E90,0x5B),('clock_direct_thunk',0x4D0570,5),('clock_thunk_target_body',0x4D0470,0xF7),
      ('clock_dependent_direct_helper',0x4C51B0,0x4C1),('level_change_effect_direct_constructor',0x44EDE0,0xD3),
      ('allocator_external_thunk',0x4F118C,6),('dispatcher_optional_diagnostics_prefix',0x4B2970,0x6B),('S0C_direct_dispatch_case',0x4B2A13,0xC)]:capture(*x)
    for x in [('S0C_numeric_format',0x6342CC,len(FMT)+1),('numeric_reader_jump_table',0x4A2B78,0x18),('numeric_reader_character_map',0x4A2B90,0x21),
      ('S0C_primary_opcode_map',0x4B347C,1),('S0C_fallback_opcode_map',0x4B36C0,1),('S0C_primary_target',0x4B3218,4),('S0C_fallback_target',0x4B3584,4),
      ('protected_identity_object_snapshot',0x831738,12),('protected_current_HP_object_snapshot',0x8315F0,12),('protected_current_MP_object_snapshot',0x8315E0,12)]:capture(*x,'data')
    def w(va):return {'fixed_PE_sha256':FIXED,**mapping(pe,va),**{k:v for k,v in ins[va].items()if k!='va'},'range':owners[va]}
    expected=[(0x4B2A13,'mov','ecx, esi'),(0x4B2A15,'call','0x4a36f0'),(0x4A370B,'mov','eax, dword ptr [ecx + 1]'),
      (0x4A3718,'call','0x4c3e90'),(0x4A3788,'push','0x6342cc'),(0x4A378E,'call','0x4a2a50'),(0x4A3796,'add','esp, 0x60'),
      (0x4A3799,'and','ecx, 0xffff'),(0x4A37A0,'mov','ecx, 0x8315f0'),(0x4A37A5,'call','0x4c3e90'),
      (0x4A37B2,'and','edx, 0xffff'),(0x4A37AD,'mov','ecx, 0x8315e0'),(0x4A37B9,'call','0x4c3e90'),
      (0x4A37BE,'mov','eax, dword ptr [0x834ee4]'),(0x4A37CB,'cmp','al, 1'),(0x4A37CF,'cmp','al, byte ptr [0x8348ca]'),
      (0x4A37DC,'mov','byte ptr [0x8348ca], al'),(0x4A37E1,'call','0x4f118c'),(0x4A380D,'push','0x84f'),(0x4A3816,'call','0x44ede0'),
      (0x4A3826,'call','dword ptr [edx + 4]'),(0x4A383D,'mov','byte ptr [0x8348ca], al'),(0x4A3848,'call','0x4d0570'),
      (0x4A384D,'mov','ecx, dword ptr [0x831790]'),(0x4A3859,'call','0x4c51b0'),
      (0x4A2A93,'mov','byte ptr [edx], al'),(0x4A2AAE,'add','ebx, 2'),(0x4A2AB1,'mov','word ptr [eax], cx'),
      (0x4A2AE0,'add','ebx, 4'),(0x4A2AE3,'mov','dword ptr [eax], ecx'),(0x4A2B69,'mov','eax, ebx'),
      (0x4C3E97,'call','0x4c3ca0'),(0x4C3EA6,'mov','ecx, dword ptr [edi + 4]'),(0x4C3EB6,'call','0x773dc650'),
      (0x4C3ECD,'xor','edx, ecx'),(0x4C3ED7,'mov','dword ptr [ecx + eax*4], edx'),(0x4F118D,'jmp','0x773c4c20'),
      (0x4D0570,'jmp','0x4d0470'),(0x4D0484,'call','0x4d02f0'),(0x44EE01,'call','0x4dfdd0'),(0x44EE12,'mov','dword ptr [esi], 0x4f84c0'),(0x44EE55,'call','0x77406880'),
      (0x492D80,'call','0x4c9190'),(0x492D91,'mov','dword ptr [0x834888], eax'),(0x492F02,'call','0x4c6ae0')]
    assertions=[]
    for va,mn,op in expected:
        assert(ins[va]['mnemonic'],ins[va]['operands'])==(mn,op),(hex(va),ins[va]);assertions.append({**w(va),'match':True})
    assert pe.get_data(0x2342CC,len(FMT)+1)==FMT.encode()+b'\0'
    dispatch=[]
    for label,m,j,slot in [('mode4_phase_ge3',0x4B347C,0x4B3218,4),('fallback',0x4B36C0,0x4B3584,5)]:
        assert pe.get_data(m-0x400000,1)==bytes([slot])and struct.unpack('<I',pe.get_data(j-0x400000,4))[0]==0x4B2A13
        dispatch.append({'branch':label,'opcode':12,'map':mapping(pe,m),'map_raw':f'{slot:02X}','jump_entry':mapping(pe,j),'target':'004B2A13','handler':'004A36F0'})
    reader_cases={}
    for char,target in [('c',0x4A2A8A),('h',0x4A2A9A),('d',0x4A2AB9)]:
        at=0x4A2B90+ord(char)-0x53;slot=pe.get_data(at-0x400000,1)[0];entry=0x4A2B78+4*slot
        assert struct.unpack('<I',pe.get_data(entry-0x400000,4))[0]==target
        reader_cases[char]={'map':mapping(pe,at),'slot':slot,'jump':mapping(pe,entry),'target':f'{target:08X}','store_witness':w({'c':0x4A2A93,'h':0x4A2AB1,'d':0x4A2AE3}[char])}
    specs_fields=[
      ('object_id','75AA48',0x4A3783,'u32'),('level','local ebp-0D→8348CA',0x4A3782,'u8'),('experience','8348C4',0x4A377D,'u32'),
      ('strength','8348B0',0x4A3775,'u8'),('intelligence','8348B1',0x4A3770,'u8'),('wisdom','8348B2',0x4A376B,'u8'),
      ('dexterity','8348B3',0x4A3766,'u8'),('constitution','8348B4',0x4A3761,'u8'),('charisma','8348B5',0x4A375C,'u8'),
      ('current_hp','local ebp-14→protected8315F0',0x4A375B,'u16'),('maximum_hp','8348BC',0x4A3756,'u16'),
      ('current_mp','local ebp-18→protected8315E0',0x4A3755,'u16'),('maximum_mp','8348BA',0x4A3750,'u16'),
      ('armor_class','8348B6',0x4A374B,'i8'),('game_time','8348AC',0x4A3746,'u32'),('food_gauge','8348CC lowbyte only',0x4A3741,'u8'),
      ('weight_gauge','8348CB',0x4A373C,'u8'),('lawful','8348B8',0x4A3737,'i16'),
      ('fire_resistance','8348C1',0x4A3732,'i8'),('water_resistance','8348C0',0x4A372D,'i8'),
      ('wind_resistance','8348BF',0x4A3728,'i8'),('earth_resistance','8348BE',0x4A3723,'i8')]
    fields=[];offset=1
    for idx,(char,(name,dest,va,semantic))in enumerate(zip(FMT,specs_fields)):
        n={'c':1,'h':2,'d':4}[char];fields.append({'index':idx,'body_offset':offset,'width':n,'reader_format':char,'endian':'LE'if n>1 else'byte',
          'name_from_existing_server_method':name,'destination':dest,'destination_argument_push':w(va),'native_storage_type':f'raw{8*n}bits',
          'signedness_native':'bit copy; current HP/MP additionally maskFFFF; no primitive sign extension','server_semantic_type':semantic,
          'semantic_signedness_evidence':'Existing Protocol.characterStatus validation/domain; not proof of all downstream native consumers','reader_case':reader_cases[char]});offset+=n
        operand={'level':'eax','current_hp':'edx','current_mp':'ecx'}.get(name)
        if operand is None:operand='0x'+dest.split()[0].lower()
        assert(ins[va]['mnemonic'],ins[va]['operands'])==('push',operand),(name,hex(va),ins[va])
        assertions.append({**w(va),'match':True,'expected_destination_push':operand})
    for va,op in [(0x4A371D,'ecx, [ebp - 0x18]'),(0x4A3720,'edx, [ebp - 0x14]'),(0x4A377A,'eax, [ebp - 0xd]')]:
        assert(ins[va]['mnemonic'],ins[va]['operands'])==('lea',op)
        assertions.append({**w(va),'match':True,'expected_local_address':op})
    assert offset==37 and len(fields)==22
    write('S0C-status-owner/fields.json',{'opcode':12,'format':FMT,'parameter_bytes':36,'body_including_opcode':37,'string_fields':0,'fields':fields,
      'Native_pointer_return':'4A2A50 returns input_pointer+36, but handler ignores return; native exact/truncated/trailing bounds validation not present in numeric reader.',
      'partial_width_stores':'c/h write exactly1/2 bytes. Food8348CC upper3 bytes are not overwritten; h local upper16 initially unspecified until handler masksFFFF.'})
    def decode(body):
        if len(body)!=37:raise ValueError(f'Audit strict length: wanted37 actual{len(body)}; not native guard')
        if body[0]!=12:raise ValueError('Audit opcode mismatch; native handler assumes dispatcher')
        result=[]
        for f in fields:
            b=body[f['body_offset']:f['body_offset']+f['width']];u=int.from_bytes(b,'little');s=u-(1<<(8*f['width']))if u&(1<<(8*f['width']-1))else u
            result.append({'name':f['name_from_existing_server_method'],'body_offset':f['body_offset'],'raw_hex':b.hex(),'unsigned_bits':u,
              'server_semantic_value':s if f['server_semantic_type'].startswith('i')else u,'native_destination':f['destination']})
        return result
    mh=json.loads(HANDOFF.read_text(encoding='utf-8'));capture_pin=next(x for x in mh['owned_runtime_evidence']if x['path'].endswith('frames.jsonl'))
    capture_path=Path(capture_pin['path']);assert record(capture_path)==capture_pin
    records=[json.loads(s)for s in capture_path.read_text(encoding='utf-8').splitlines()];actual=[]
    for i,r in enumerate(records):
        if r['direction']=='server_to_client'and r['opcode'].lower()=='0x0c':
            body=bytes.fromhex(r['plain']);assert len(body)==37 and r['wire_length']==39
            path=OUT/f'frames/AD368-r509-line{i+1:03}.plain.bin';path.parent.mkdir(exist_ok=True);path.write_bytes(body)
            actual.append({'capture_line':i+1,'capture_record':r,'body':record(path),'decode':decode(body),'preceding_records':records[max(0,i-4):i],
              'original_native_decoder_executed':False,'Modern_R509_state':'First unsupported boundary; no status or skill owner qualification'})
    assert len(actual)==2 and [x['decode'][0]['unsigned_bits']for x in actual]==[10201,10001]
    write('S0C-status-owner/AD368-sealed-frame-comparison.json',{'source_handoff':record(HANDOFF),'sealed_capture':capture_pin,'actual_existing_records':actual,
      'new_capture_or_fixture_execution':False,'schema0_layout_matches':True,'wire_difference_found':False})
    sentinel=bytearray([12]);values=[]
    for i,f in enumerate(fields):
        u=[0xFF,0x8001,0xFEDCBA98][{1:0,2:1,4:2}[f['width']]];sentinel+=u.to_bytes(f['width'],'little');values.append(u)
    decoded=decode(sentinel);assert [x['unsigned_bits']for x in decoded]==values
    assert next(x for x in decoded if x['name']=='armor_class')['server_semantic_value']==-1
    assert next(x for x in decoded if x['name']=='lawful')['server_semantic_value']==-32767
    path=OUT/'cases/sentinel-all-fields.plain.bin';path.parent.mkdir(exist_ok=True);path.write_bytes(sentinel)
    checks=[{'case':'all_fields_sentinel','body':record(path),'decode':decoded,'match':True}]
    for name,body in [('truncated_36',sentinel[:-1]),('trailing_38',sentinel+b'\0'),('wrong_opcode',b'\x0D'+sentinel[1:])]:
        try:decode(body);raise AssertionError('Strict audit model should reject')
        except ValueError as e:checks.append({'case':name,'audit_rejection':str(e),'native_rejection_not_claimed':True,'match':True})
    write('S0C-status-owner/new-code-derived-cases.json',{'checks':checks,'status':'STATIC_FIELD_MODELS_ONLY','actual_frames_decoded':2,'new_synthetic_checks':4,
      'new_Java_native_or_producer_PASS_runs':0,'ready_owner_inference':False})
    calls=[];globalrefs=[];none_owner=[]
    for row in ranges:
        if row['kind']=='code':
            if row['label']!='dispatcher_optional_diagnostics_prefix':
                hits=[x for x in row['instructions']if '0x834888'in x['operands']];assert not hits
                assert not any(x['mnemonic']=='call'and x['operands']=='0x4c9190'for x in row['instructions'])
                none_owner.append({'range':row['output'],'exact834888_operand_references':0,'negative_scope':'These complete/explicit bounded bodies only, not aliases or downstream callbacks'})
            for x in row['instructions']:
                if x['mnemonic']=='call'or(row['label'].endswith('_thunk')and x['mnemonic']=='jmp'):
                    target=int(x['operands'],16)if x['operands'].startswith('0x')else None
                    calls.append({'body':row['label'],**w(int(x['va'],16)),'destination':mapping(pe,target)if target is not None else None,
                      'scope_boundary':'Captured direct body'if target is not None and target in {int(z['va'],16)for z in ranges if z['kind']=='code'}else'No recursive expansion; external/private/indirect or second-layer target remains unobserved'})
                if any(k in x['operands']for k in ['0x8348','0x8317','0x8315','0x75aa','0x6352','0x834ee4']):globalrefs.append({'body':row['label'],**w(int(x['va'],16))})
    write('S0C-status-owner/direct-call-and-state-boundaries.json',{'calls':calls,'named_state_operands':globalrefs,'bounded834888_absence':none_owner,
      'owner_negative_statement':'No explicit834888 operand or owner constructor4C9190 call in S0C handler/direct bodies. Does not prove arbitrary aliases/downstream virtual/private callbacks never touch owner.'})
    protocol=SERVER/'Protocol.java';session=SERVER/'ClientSession.java';numeric=SERVER/'NumericBreakthrough.java';serverpins=[record(p)for p in [protocol,session,numeric]]
    def excerpt(p,a,b):
        lines=p.read_text(encoding='utf-8').splitlines();return {'source':record(p),'first_line':a,'last_line':b,'text':'\n'.join(f'{j+1}: {lines[j]}'for j in range(a-1,b))}
    provider={'sources':serverpins,'existing_methods_read_only':[excerpt(protocol,5351,5460),excerpt(protocol,5565,5579),excerpt(session,12334,12352),excerpt(numeric,1,49)],
      'baseline':'ExistingS_STATUS=0C andcharacterStatus writesexactnative22fieldlayout; STR/INT/WIS/DEX/CON/CHA andHP/maxHP/MP/maxMP order alreadycorrect. No producer changes/newopcodes required.',
      'profiles':'Schema0 nativebody37. Existingopt-in numeric schemas1/2/4 widenfields to48/66/78bodybytes; require matched patched client and are outside fixednative37 contract. SealedR509actual37 usesbaseline shape; no globalprofileconfiguration inferred.',
      'food_weight':'Existingprovider projectsretainedfood225/weight240 with(clamp(v,0,max)*29+max//2)//max. These two wirebytes aregauge values,not raw retainedunits.',
      'game_time':'ExistingJava methodwritesd0 atgame_time; nativeparserstillhasrealDWORDfield andcantriggerclocksideeffects. Do not relabelreserved or inferready.',
      'comparison':'KNOWN_CONTRACT_CORROBORATION; nativehandler/schema0provider/actual37byteframesagree. No truewire delta established.',
      'new_wire_differences':0,'new_opcodes':0,'producer_PASS_rerun':False,'server_files_modified':False}
    write('S0C-status-owner/server-method-comparison.json',provider)
    lines=ASM.read_text(encoding='utf-8').splitlines();write('S0C-status-owner/ASM-navigation.json',{'source':record(ASM),'role':'Labels/navigation only; rawfixedPE governsactualtargets',
      'excerpts':[{'first_line':a,'last_line':b,'text':'\n'.join(f'{j+1}: {lines[j]}'for j in range(a-1,b))}for a,b in [(341781,341931),(340420,340601),(372328,372406),(409725,409786),(166775,166806),(435466,435469)]]})
    for x in assertions:
        va=int(x['va'],16);raw=bytes.fromhex(x['raw_hex']);ds=list(cs.disasm(pe.get_data(va-0x400000,len(raw)),va))
        assert pe.get_data(va-0x400000,len(raw))==raw and len(ds)==1 and(ds[0].mnemonic,ds[0].op_str)==(x['mnemonic'],x['operands'])
    write('S0C-status-owner/ranges.json',{'fixed_PE':fixed,'ranges':ranges,'inherited':inherited,'all_code_fully_decoded':True})
    verify={'raw_ranges':len(ranges),'code_ranges':sum(x['kind']=='code'for x in ranges),'data_ranges':sum(x['kind']=='data'for x in ranges),'instruction_assertions':assertions,
      'range_bytes_and_pins_match':all(record(Path(x['output']['path']))==x['output']and Path(x['output']['path']).read_bytes()==pe.get_data(int(x['rva'],16),x['bytes'])for x in ranges),
      'prior_primary_pins':preserved,'actual_frame_pin_matches':True,'new_synthetic_checks':4,'new_capture_native_Java_or_producer_runs':0,'new_wire_differences':0}
    assert verify['range_bytes_and_pins_match'];write('S0C-status-owner/new-evidence-verification.json',verify)
    report={'schema':'lito.finite.S0C-status-owner.v1','updated_at':stamp(),'status':'FINITE_STATIC_S0C37_FIELDS_AND_OWNER_BOUNDARY_COMPLETE',
      'fixed_PE':fixed,'ASM':record(ASM),'scope':'CompleteS0C handler,numericreadernecessaryc/h/d cases,one-layerdispatch caller,directstatussetter/effect/timehelper bodies; one thinclockthunktarget. No nested helper/private/virtual recursion.',
      'prior_primary_pins':preserved,'inherited_evidence':inherited,
      'reusable_outputs':[record(OUT/n)for n in ['fields.json','ranges.json','AD368-sealed-frame-comparison.json','new-code-derived-cases.json','direct-call-and-state-boundaries.json','server-method-comparison.json','ASM-navigation.json','new-evidence-verification.json']]+[a['body']for a in actual],
      'dispatch':{'routes':dispatch,'case_witnesses':[w(0x4B2A13),w(0x4B2A15)],'primary_condition':'64BBD4==4 andsigned64AF98>=3;otherwisefallback. Both admitopcode0C tosamehandler. No peropcodeowner/readiness gate.',
        'receive_inherited':'4B37C0 completebody→4B3868→4B2970 synchronously. No newreceive or nativeexecution.',
        'optional_diagnostics_boundary':'If64AFA2!=0,dispatcher prefixcallsprivate/external logging helpers beforeselector; source-onlycode route isconditionalonreturn, not proofactualdecoder ran.'},
      'wire':{'direction':'S server→client','opcode':12,'classification':'KNOWN_CONTRACT_CORROBORATION','handler':mapping(pe,0x4A36F0),'handler_range':next(x['output']for x in ranges if x['label']=='S0C_complete_handler'),
        'format':FMT,'parameter_bytes':36,'body_including_opcode':37,'field_count':22,'field_contract':record(OUT/'fields.json'),'endian':'h/d LE; c byte, storesbitpatternswithoutprimitive signextension','string_encoding':'not applicable',
        'native_length_behavior':'No packetlength argument/guard in numericreader, no consumed-pointer check inhandler.37 isstructuralexpectedshape; native mayreadtruncatedbuffer orignoretrailingbytes. Auditstrictrejectcases are not native malformed-frame behavior.',
        'trigger_evidence':'SealedR509records43/73 immediatelyfollowmatchingS_CA after userC_CA2/C_CA1; sourceprotocolalreadyused bysendNativeBattleStatus. No universal firstlobby delivery/readiness claim.',
        'evidence_level':'FixedPEexactfieldpushes+numericreaderraw+twohashpinnedexistingAD36837byteframes+existingJavaproviderread-only. No liveoriginaldecoder/owner acceptance.'},
      'state_updates':{'identity':'Beforevarargsparse,[body+1] DWORD passedto4C3E90 withECX831738. Numericd fieldwrites75AA48 separately.',
        'HP_MP':'Currenthwordsreadintolocal low16,maskFFFF then4C3E90 to8315F0/8315E0. MaxHP/MaxMPstoredWORD8348BC/BA. Protectedvalueobject isnot rawvalueatbase.',
        'protected_setter':'4C3E90 requiresobject+4 storage,updates+8 key,16DWORDstorageentries andencodedindex+0;finalinputXORkey storedindynamicselectedslot.4C3CA0 andexternal773DC650 unexpanded;no livepointer/randomvalue/success inferred.',
        'attributes_and_aux':'STR/INT/WIS/DEX/CON/CHA byte8348B0..B5;AC byteB6;lawful WORDB8;EXP DWORDC4;time DWORDAC;food lowbyteCC;weightbyteCB;fire/water/wind/earth C1/C0/BF/BE.',
        'level_effect':'LevellocalAL storedbyte8348CA. Ifexistingactor834EE4 nonNULL,AL!=1 andAL!=previous8348CA,allocate140bytes→44EDE0 usingactor+2C/+30 position,sprite084F,thenvirtual+4. This isdifferenttype/vtable4F84C0; notskillownercreation.',
        'time_effect':'4A3848→4D0570→4D0470;directbodywrites834898/9C/A0/A4 andcanupdateprotected8316A0,dependsunexpanded4D02F0.4A3859 calls4C51B0 onlyif831790==0;bodyupdates831790/798/79C/6352F8/FC/831748 andcontainsconditionalmap/time C59sendcalls. No extraC59producerreverse/wirechangeclaim.'},
      'skill_owner_834888':{'created_by_S0C_explicitly':False,'tested_or_required_by_S0C_explicitly':False,'direct_accessor_count':0,
        'negative_scope':'Allnewdirecthandler/body operandlistschecked; no834888 reference or4C9190 call. Aliasstores,privatehelpers,nested4C4B10/485800/447B50/4DFDD0/4D02F0 andvirtualeffectcallbacksnotexpanded,so do notclaimcompletelynoindirectinvolvement.',
        'known_owner_source':'Acceptedordinary4928C0 selection:492D80→4C9190 constructsowner,492D91 publishes834888;later492F02 sendsC_C9. Existingreuse492B61→4CD1C0. SourceorderpredatesresponseS0C;liveepoch/successunobserved.',
        'owner_witnesses':[w(0x492D80),w(0x492D91),w(0x492F02)],'accepted_owner_contract':record(ROOT/'S1E-initialization-gates.json'),
        'S1E_contrast':'AcceptedS1Ehandlerrequires834888 viaownerupdate; S0C statusdecode doesnot replaceownerinitialization/table/class/order prerequisites.',
        'decodable_status_implies_owner_or_ready':False,'same_object_id_does_not_establish_actor_or_skill_owner_identity':True},
      'external_private_and_depth_boundaries':calls,'AD368_comparison':record(OUT/'AD368-sealed-frame-comparison.json'),'server_comparison':provider,
      'server_handoff':{'new_wire_differences':0,'new_opcodes':0,'packet_delta_updated':False,'reason':'AlreadyknownS_STATUS37 corroborated. Supervisor may forwardnewreceiver/globals/owner boundary toModern/server withoutproductchange.',
        'direction':'S','opcode':12,'field_order':FMT,'trigger':'existingcaptureduserC_CA1/2 receipts; no universalS0 policy','handler_VA_RVA':mapping(pe,0x4A36F0),'fixed_PE_sha256':FIXED,'classification':'KNOWN_CONTRACT_CORROBORATION'},
      'runtime_acceptance':{'live_original_handler_completed':False,'skill_owner_ready':False,'actor_ready':False,'player_ready':False,'GUI_accepted':False,'overall_unpacking_accepted':False},
      'constraints':{'owned_directory_only':True,'other_group_files_modified':False,'native_observer_or_input_injection_screenshot':False,'Java_SQL_or_producer_PASS_runs':0,'S0_tail_policy_or_restricted_server_takeover':False,'new_archive_member_reads':0,'finite_branch_expansion_stopped':True}}
    write('S0C-status-owner-contract.json',report)
    print(json.dumps({'report':record(ROOT/'S0C-status-owner-contract.json'),'ranges':len(ranges),'instruction_assertions':len(assertions),'fields':len(fields),'bytes':offset,'actualframes':len(actual),'newchecks':4,'newwire':0},ensure_ascii=False))

if __name__=='__main__':main()

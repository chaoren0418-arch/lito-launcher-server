"""Batch12 finite S50/S51/S52/S56 static contracts. No runtime/producer/archive runs."""
from audit import *

OUT=ROOT/'status-family-50-51-52-56'
FIXED='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
BATCH11='AD09F778515F5AEBBEFAD7E4FF047A34F28BCC67365B8D96DE84653758CB01F5'
SERVER=ROOT.parent/'lito伺服器/LiTo381-Server/src/main/java/local/lito381/server'
HANDOFF=ROOT.parent/'lito重建版/LiTo381-Modern/logs/continuation-20261003/r509-character-selection-channel-handoff.json'
SPECS=[(0x50,0x4AF0A0,0x3B,0x4B2D2B,8,'S_ABILITY_SCORES'),
       (0x51,0x4AF0E0,0x1A,0x4B2D37,6,'S_EXP'),
       (0x52,0x4AF100,0x29,0x4B2D43,6,'S_AC'),
       (0x56,0x4AF320,0x1A,0x4B2D71,3,'S_MAGIC_STATUS')]

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    OUT.mkdir(exist_ok=True);sources=[]
    for p,h in [(BIN,'CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292'),
      (PE,FIXED),(ASM,'2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A')]:
        pin=record(p);assert pin['sha256']==h,(p,pin['sha256'],h);sources.append(pin)
    old=load('S4D-mana-point-contract.json');assert record(ROOT/'S4D-mana-point-contract.json')['sha256']==BATCH11
    preserved=old['prior_primary_pins']+[{**record(ROOT/'S4D-mana-point-contract.json'),'expected_sha256':BATCH11,'match':True}]
    for p in preserved:assert record(Path(p['path']))['sha256']==p['expected_sha256']
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    ranges=[];inherited=[];ins={};owners={}
    def install(row):
        va=int(row['va'],16);raw=Path(row['output']['path']).read_bytes();assert raw==pe.get_data(va-0x400000,len(raw))
        for x in row.get('instructions',[]):
            item=dict(x);item['raw_hex']=item.pop('bytes',item.get('raw_hex'));at=int(item['va'],16)
            b=bytes.fromhex(item['raw_hex']);assert b==pe.get_data(at-0x400000,len(b));ins[at]=item;owners[at]=row['output']
    for file,va in [('skill-gates/ranges.json',0x4B29DB),('skill-gates/ranges.json',0x4B313F),
      ('S0C-status-owner/ranges.json',0x4B2970),('S0C-status-owner/ranges.json',0x4A2A50),
      ('S0C-status-owner/ranges.json',0x4A2B56),('S0C-status-owner/ranges.json',0x4A2B78),
      ('S0C-status-owner/ranges.json',0x4A2B90),('receive-boundary/ranges.json',0x4B37C0)]:
        row=next(x for x in load(file)['ranges']if int(x['va'],16)==va);assert record(Path(row['output']['path']))==row['output'];install(row)
        inherited.append({'index':record(ROOT/file),'range':row['output'],'va':row['va'],'compared_to_fixed_PE':True,'old_cases_or_producer_replayed':False})
    def capture(label,va,n,kind='code'):
        m=mapping(pe,va);assert m['raw_backed'];raw=pe.get_data(va-0x400000,n);ds=[]if kind=='data'else list(cs.disasm(raw,va))
        assert len(raw)==n and(kind=='data'or sum(x.size for x in ds)==n)
        path=OUT/f'ranges/{va:08X}-{n:X}.bin';path.parent.mkdir(exist_ok=True);path.write_bytes(raw)
        row={**m,'label':label,'kind':kind,'bytes':n,'output':record(path),'decoded_bytes':sum(x.size for x in ds),
          'instructions':[{'va':f'{x.address:08X}','raw_hex':x.bytes.hex(),'mnemonic':x.mnemonic,'operands':x.op_str}for x in ds]}
        ranges.append(row);install(row)
    for op,va,n,caller,size,name in SPECS:
        capture(f'S{op:02X}_complete_handler',va,n);capture(f'S{op:02X}_single_direct_caller',caller,0xC)
    for args in [('primary_maps50_51_52',0x4B34C0,3),('primary_map56',0x4B34C6,1),
      ('fallback_maps50_51_52',0x4B3704,3),('fallback_map56',0x4B370A,1),
      ('primary_targets50_51_52',0x4B3320,0xC),('primary_target56',0x4B3338,4),
      ('fallback_targets50_51_52_56',0x4B35E0,0x10),('format_S51_cd',0x5028F8,3),
      ('format_S52_ccccc',0x62B9FC,6),('format_S56_cc',0x502778,3)]:capture(*args,'data')
    def w(at):return {'fixed_PE_sha256':FIXED,**mapping(pe,at),**{k:v for k,v in ins[at].items()if k!='va'},'range':owners[at]}
    # Expected instructions transcribed from complete handlers, independent of decoder output.
    expected_text='''4AF0A0|mov|al, byte ptr [ecx + 1]
4AF0A3|inc|ecx
4AF0A4|inc|ecx
4AF0A5|mov|byte ptr [0x8348b0], al
4AF0AA|mov|al, byte ptr [ecx]
4AF0AC|inc|ecx
4AF0AD|mov|byte ptr [0x8348b1], al
4AF0B2|mov|al, byte ptr [ecx]
4AF0B4|inc|ecx
4AF0B5|mov|byte ptr [0x8348b2], al
4AF0BA|mov|al, byte ptr [ecx]
4AF0BC|inc|ecx
4AF0BD|mov|byte ptr [0x8348b3], al
4AF0C2|mov|al, byte ptr [ecx]
4AF0C4|inc|ecx
4AF0C5|mov|byte ptr [0x8348b4], al
4AF0CA|mov|al, byte ptr [ecx]
4AF0CC|mov|byte ptr [0x8348b5], al
4AF0D1|mov|cl, byte ptr [ecx + 1]
4AF0D4|mov|byte ptr [0x8348cb], cl
4AF0DA|ret|
4AF0E0|push|0x8348c4
4AF0E5|push|0x8348ca
4AF0EA|inc|ecx
4AF0EB|push|0x5028f8
4AF0F0|push|ecx
4AF0F1|call|0x4a2a50
4AF0F6|add|esp, 0x10
4AF0F9|ret|
4AF100|push|0x8348be
4AF105|push|0x8348bf
4AF10A|push|0x8348c0
4AF10F|push|0x8348c1
4AF114|push|0x8348b6
4AF119|inc|ecx
4AF11A|push|0x62b9fc
4AF11F|push|ecx
4AF120|call|0x4a2a50
4AF125|add|esp, 0x1c
4AF128|ret|
4AF320|push|0x8348e0
4AF325|push|0x8348df
4AF32A|inc|ecx
4AF32B|push|0x502778
4AF330|push|ecx
4AF331|call|0x4a2a50
4AF336|add|esp, 0x10
4AF339|ret|'''
    expected_new=[(int(a,16),m,o)for a,m,o in [line.split('|')for line in expected_text.splitlines()]]
    for op,va,n,caller,size,name in SPECS:
        expected_new.extend([(caller,'mov','ecx, esi'),(caller+2,'call',hex(va)),(caller+7,'pop','esi'),
          (caller+8,'mov','esp, ebp'),(caller+10,'pop','ebp'),(caller+11,'ret','')])
    expected_old=[(0x4B29DB,'cmp','dword ptr [0x64bbd4], 4'),(0x4B29E8,'cmp','dword ptr [0x64af98], 3'),
      (0x4B29EF,'jl','0x4b313f'),(0x4B2A06,'mov','cl, byte ptr [eax + 0x4b3470]'),
      (0x4B2A0C,'jmp','dword ptr [ecx*4 + 0x4b3208]'),(0x4B3150,'mov','dl, byte ptr [eax + 0x4b36b4]'),
      (0x4B3156,'jmp','dword ptr [edx*4 + 0x4b3570]'),(0x4A2A7D,'mov','cl, byte ptr [eax + 0x4a2b90]'),
      (0x4A2A83,'jmp','dword ptr [ecx*4 + 0x4a2b78]'),(0x4A2A8D,'mov','al, byte ptr [ebx]'),
      (0x4A2A92,'inc','ebx'),(0x4A2A93,'mov','byte ptr [edx], al'),(0x4A2ACD,'add','esi, 4'),
      (0x4A2AE0,'add','ebx, 4'),(0x4A2AE3,'mov','dword ptr [eax], ecx'),(0x4A2B69,'mov','eax, ebx')]
    assertions=[]
    for fresh,entries in [(True,expected_new),(False,expected_old)]:
        for va,mn,op in entries:
            assert(ins[va]['mnemonic'],ins[va]['operands'])==(mn,op),(hex(va),ins[va])
            assertions.append({**w(va),'expected_mnemonic':mn,'expected_operands':op,'new_instruction':fresh,'match':True})
    assert len(expected_new)==72
    assert {int(x['va'],16)for row in ranges if row['kind']=='code'for x in row['instructions']}=={x[0]for x in expected_new}
    dispatch=[]
    for opcode,handler,n,caller,size,name in SPECS:
        for label,base,jump,expected_slot in [('mode4_signed_phase_ge3',0x4B3470,0x4B3208,{0x50:70,0x51:71,0x52:72,0x56:76}[opcode]),
          ('fallback',0x4B36B4,0x4B3570,{0x50:28,0x51:29,0x52:30,0x56:31}[opcode])]:
            at=base+opcode;slot=pe.get_data(at-0x400000,1)[0];entry=jump+slot*4;raw=pe.get_data(entry-0x400000,4)
            assert slot==expected_slot and raw==struct.pack('<I',caller)
            dispatch.append({'opcode':f'0x{opcode:02X}','branch':label,'map':mapping(pe,at),'slot':slot,
              'map_raw_hex':f'{slot:02x}','jump_entry':mapping(pe,entry),'jump_raw_hex':raw.hex(),
              'case':mapping(pe,caller),'handler':mapping(pe,handler),'call_witness':w(caller+2)})
    formats={0x51:('cd',0x5028F8),0x52:('ccccc',0x62B9FC),0x56:('cc',0x502778)}
    reader_cases={}
    for ch,target,store in [('c',0x4A2A8A,0x4A2A93),('d',0x4A2AB9,0x4A2AE3)]:
        at=0x4A2B90+ord(ch)-0x53;slot=pe.get_data(at-0x400000,1)[0];entry=0x4A2B78+slot*4
        assert int.from_bytes(pe.get_data(entry-0x400000,4),'little')==target
        reader_cases[ch]={'map':mapping(pe,at),'slot':slot,'jump':mapping(pe,entry),'target':mapping(pe,target),'store_witness':w(store)}
    field_specs={
      0x50:[('strength',1,1,0x8348B0,0x4AF0A0,0x4AF0A5,'u8'),('intelligence',2,1,0x8348B1,0x4AF0AA,0x4AF0AD,'u8'),
        ('wisdom',3,1,0x8348B2,0x4AF0B2,0x4AF0B5,'u8'),('dexterity',4,1,0x8348B3,0x4AF0BA,0x4AF0BD,'u8'),
        ('constitution',5,1,0x8348B4,0x4AF0C2,0x4AF0C5,'u8'),('charisma',6,1,0x8348B5,0x4AF0CA,0x4AF0CC,'u8'),
        ('weight_gauge',7,1,0x8348CB,0x4AF0D1,0x4AF0D4,'u8')],
      0x51:[('level',1,1,0x8348CA,None,0x4AF0E5,'u8'),('experience',2,4,0x8348C4,None,0x4AF0E0,'u32')],
      0x52:[('armor_class',1,1,0x8348B6,None,0x4AF114,'i8'),('fire_resistance',2,1,0x8348C1,None,0x4AF10F,'i8'),
        ('water_resistance',3,1,0x8348C0,None,0x4AF10A,'i8'),('wind_resistance',4,1,0x8348BF,None,0x4AF105,'i8'),
        ('earth_resistance',5,1,0x8348BE,None,0x4AF100,'i8')],
      0x56:[('spell_power_modifier',1,1,0x8348DF,None,0x4AF325,'i8'),('magic_resistance_modifier',2,1,0x8348E0,None,0x4AF320,'i8')]}
    contracts={}
    for opcode,va,n,caller,body_size,name in SPECS:
        fields=[]
        for key,offset,width,dest,read_at,write_at,semantic in field_specs[opcode]:
            f={'name_from_existing_Java':key,'body_offset':offset,'width':width,'endian':'LE'if width==4 else'byte',
              'native_storage':f'raw{width*8}bits','native_destination':mapping(pe,dest),'write_width_bytes':width,
              'server_semantic_type':semantic,'native_signedness':'Exact bit copy, no value sign extension; downstream arithmetic not established',
              'evidence_levels':['FIXED_PE_RAW_FOR_OFFSET_WIDTH_DESTINATION','EXISTING_JAVA_SOURCE_FOR_SEMANTIC_NAME_AND_DOMAIN']}
            if opcode==0x50:f.update(load_witness=w(read_at),store_witness=w(write_at))
            else:f.update(destination_argument=w(write_at),reader_case=reader_cases['d'if width==4 else'c'])
            fields.append(f)
        assert 1+sum(x['width']for x in fields)==body_size
        fmt,fmtva=formats.get(opcode,('ccccccc',None))
        if fmtva is not None:assert pe.get_data(fmtva-0x400000,len(fmt)+1)==fmt.encode()+b'\0'
        row=next(x for x in ranges if x['label']==f'S{opcode:02X}_complete_handler')
        contracts[f'S{opcode:02X}']={'direction':'server_to_client','opcode':opcode,'name_from_existing_server':name,
          'handler':mapping(pe,va),'handler_range':row['output'],'body_including_opcode':body_size,'parameter_bytes':body_size-1,
          'field_count':len(fields),'fields':fields,'layout':fmt,'format_string':mapping(pe,fmtva)if fmtva is not None else None,
          'format_status':'Actual raw-backed ASCII literal+NUL'if fmtva else'Conceptual layout only; native manually loads bytes, no format string',
          'direct_call_count':0 if opcode==0x50 else 1,'callee':'004A2A50 numericc/d only'if opcode!=0x50 else None,
          'consumption':'Numeric reader returns input+parameter bytes = body+expected body size in EAX. Handler and direct dispatcher case leave EAX unchanged through ret, without validating it or any length.'if opcode!=0x50 else'Manual exact7 byte loads; no consumed-pointer return. Lastloadweight overwritesCL, so no final ECX pointer contract.',
          'length_validation':'No body length parameter/check in these handlers/numeric cases. Structural expected length is not native exact/truncated/trailing rejection.',
          'direct_side_effects':'Only listed global stores. No protected setter/level-effect/actor lookup/packet send/owner create/test.'}
        write(f'status-family-50-51-52-56/S{opcode:02X}-fields.json',contracts[f'S{opcode:02X}'])
    def decode(body):
        if not body or f'S{body[0]:02X}'not in contracts:raise ValueError('Audit opcode outside exactly50/51/52/56; not native dispatch guard')
        c=contracts[f'S{body[0]:02X}']
        if len(body)!=c['body_including_opcode']:raise ValueError(f'Audit wanted{c["body_including_opcode"]} actual{len(body)}; not native guard')
        values=[]
        for f in c['fields']:
            b=body[f['body_offset']:f['body_offset']+f['width']];u=int.from_bytes(b,'little');value=u
            if f['server_semantic_type'].startswith('i')and u&(1<<(8*f['width']-1)):value-=1<<(8*f['width'])
            values.append({'name':f['name_from_existing_Java'],'body_offset':f['body_offset'],'raw_hex':b.hex(),'native_raw_bits':u,
              'Java_semantic_value':value,'destination_va':f['native_destination']['va'],'write_width':f['width']})
        return values
    mh=json.loads(HANDOFF.read_text(encoding='utf-8'));cp=next(x for x in mh['owned_runtime_evidence']if x['path'].endswith('frames.jsonl'))
    assert record(Path(cp['path']))==cp;records=[json.loads(s)for s in Path(cp['path']).read_text(encoding='utf-8').splitlines()]
    expected_values={0x50:[18,8,9,13,18,12,0],0x51:[52,0],0x52:[10,0,0,0,0],0x56:[0,0]};actual=[]
    for i,r in enumerate(records):
        if r['direction']=='server_to_client'and r['opcode'].lower()in ['0x50','0x51','0x52','0x56']:
            body=bytes.fromhex(r['plain']);opcode=body[0];d=decode(body)
            assert r['wire_length']==len(body)+2 and[x['Java_semantic_value']for x in d]==expected_values[opcode]
            anchor_index=next(j for j in range(i-1,max(-1,i-7),-1)if records[j]['direction']=='server_to_client'and records[j]['opcode'].lower()=='0x0c')
            anchor_body=bytes.fromhex(records[anchor_index]['plain']);assert len(anchor_body)==37
            common_payload={0x50:anchor_body[10:16]+anchor_body[30:31],0x51:anchor_body[5:10],
              0x52:anchor_body[24:25]+anchor_body[33:37]}.get(opcode)
            if common_payload is not None:assert body[1:]==common_payload
            p=OUT/f'frames/AD368-r509-line{i+1:03}-S{opcode:02X}.plain.bin';p.parent.mkdir(exist_ok=True);p.write_bytes(body)
            actual.append({'capture_line':i+1,'opcode':opcode,'record':r,'body':record(p),'decode':d,
              'earlier_S0C_capture_line':anchor_index+1,'earlier_S0C_shared_values_match':True if common_payload is not None else None,
              'S0C_overlap_scope':'S50/S51/S52 share14listedstatdestinations withacceptedS0C. S56 hasnoS0C SP/MR fields; no equality inferred.',
              'evidence_level':'SEALED_CAPTURE_ONLY','native_or_Modern_handler_execution_proven':False})
    assert[x['capture_line']for x in actual]==[45,46,47,48,75,76,77,78]and sum(len(x['decode'])for x in actual)==32
    write('status-family-50-51-52-56/AD368-sealed-frame-comparison.json',{'source_handoff':record(HANDOFF),'capture':cp,
      'R509_reported_state':mh['state'],'frames':actual,'actual_frames':8,'actual_fields':32,
      'sealed_sequence':[{'lines':[43,44,45,46,47,48],'opcodes':['0C','4D','50','51','52','56']},
        {'lines':[73,74,75,76,77,78],'opcodes':['0C','4D','50','51','52','56']}],
      'new_capture':False,'native_or_player_ready':False,'wire_difference_found':False})
    checks=[]
    for opcode,va,n,caller,body_size,name in SPECS:
        c=contracts[f'S{opcode:02X}'];b=bytearray([opcode]);expected=[]
        for idx,f in enumerate(c['fields']):
            value=0xFEDCBA98 if f['width']==4 else[0x80,0xFF,0x7F,0x12,0x34,0x56,0xA5][idx]
            b+=value.to_bytes(f['width'],'little');expected.append(value)
        d=decode(b);assert[x['native_raw_bits']for x in d]==expected
        if opcode==0x51:assert d[1]['raw_hex']=='98badcfe'and d[1]['Java_semantic_value']==0xFEDCBA98
        if opcode in [0x52,0x56]:assert d[0]['Java_semantic_value']==-128 and d[1]['Java_semantic_value']==-1
        checks.append({'case':f'S{opcode:02X}_nonsymmetric_highbit_fields','plain_hex':b.hex(),'decode':d,'expected_raw_bits':expected,
          'synthetic_code_derived_not_real_frame_or_native_execution':True,'match':True})
        for label,bad in [('truncated',b[:-1]),('trailing',b+b'\0'),('opcode_outside_scope',b'\x4f'+b[1:])]:
            try:decode(bad);raise AssertionError('audit must reject')
            except ValueError as e:checks.append({'case':f'S{opcode:02X}_{label}','audit_rejection':str(e),'native_rejection_not_claimed':True,'match':True})
    assert len(checks)==16
    write('status-family-50-51-52-56/new-code-derived-cases.json',{'new_static_checks':16,'checks':checks,'runtime_or_producer_runs':0,
      'highbit_signed_views':'Signed views are Java-domain annotations; reader onlycopiesraw bits, unlike S4D movsx.',
      'owner_or_player_or_GUI_qualification':False})
    calls=[];absence=[]
    for row in ranges:
        if row['kind']=='code':
            assert not any('0x834888'in x['operands']or(x['mnemonic']=='call'and x['operands']=='0x4c9190')for x in row['instructions'])
            absence.append({'range':row['output'],'explicit834888_or_owner_constructor_references':0,'scope':'Only this completehandler/singlecaller body; no wholeprogram/alias absence proof'})
            for x in row['instructions']:
                if x['mnemonic']=='call':calls.append({'source':row['label'],'witness':w(int(x['va'],16)),'target':mapping(pe,int(x['operands'],16))})
    boundary={'calls':calls,'bounded_owner_absence':absence,
      'numeric_direct_callee':'Reuse4A2A50 dispatchprefix/numericc/d cases plus4A2B56 looptail/map tables. Fixed formats select onlyc/d; no string/external/private/virtual path on these fixed formats. h code in inherited slice is not used here.',
      'no_deeper_calls_required':'Reachable c/d numeric cases/loop contain no calls. Complete4handlers have no other callee or private/virtual body to fill.',
      'inherited_dispatch_diagnostics':'Beforeselector optional64AFA2 logging still reaches private/external targets perbatch10; no actual logging/dispatch return proof.',
      'runtime_gaps':['Globals and body pointer accessibility/live initialization unobserved','Original/Modern handler completion unobserved',
        'Downstream stat arithmetic semantic interpretation not expanded','Owner epoch/actor/player/GUI never qualified'],
      'S51_level_distinction':'S51 merely copieslevel→8348CA andexperience→8348C4; does not invoke S0C level-change effect, protected value setter or clock helpers.',
      'accepted_server_skill_publication_origin_report':old['state_and_call_boundaries']['owner']['server_origin_correction']['source'],
      'skill_publication_origin':'A16B1FA66350D50056B78CF5B62F8ABECBC6D399D048B3409A66779C565B6963 ordinaryfirst/reentryS1E is test-only overlay; originalB1/AD368 lacks hook. Retain accepted correction, no source/IL rerun.'}
    write('status-family-50-51-52-56/direct-call-state-boundaries.json',boundary)
    def excerpt(p,a,b):
        lines=p.read_text(encoding='utf-8').splitlines();return {'source':record(p),'first_line':a,'last_line':b,'text':'\n'.join(f'{i+1}: {lines[i]}'for i in range(a-1,b))}
    protocol=SERVER/'Protocol.java';numeric=SERVER/'NumericBreakthrough.java'
    profile_rows=[{'opcode':'S50','baseline_body':8,'patched_schema_threshold':2,'patched_body':26,'change':'6abilitiesc→d; weightgauge remainsc'},
      {'opcode':'S51','baseline_body':6,'patched_schema_threshold':2,'patched_body':9,'change':'levelc→d; experience remainsd'},
      {'opcode':'S52','baseline_body':6,'patched_schema_threshold':1,'patched_body':9,'change':'ACc→d,4resists remainc forschema1..3'},
      {'opcode':'S52','baseline_body':6,'patched_schema_threshold':4,'patched_body':21,'change':'AC and4resists alldwords'},
      {'opcode':'S56','baseline_body':3,'patched_schema_threshold':3,'patched_body':9,'change':'SP/MR modifierc→d'}]
    provider={'sources':[record(protocol),record(numeric)],'read_only_excerpts':[excerpt(protocol,302,308),excerpt(protocol,1029,1044),
      excerpt(protocol,5472,5490),excerpt(protocol,5529,5579),excerpt(protocol,5580,5631),excerpt(protocol,6540,6542),
      excerpt(protocol,6562,6566),excerpt(numeric,1,49)],'baseline_by_opcode':{
        'S50':'abilityScores writer reorders Javaargumentlist toSTR/INT/WIS/DEX/CON/CHA exactlynative,thenweightgauge; nativeGauge29 clamp/round fromweight240 to0..29.',
        'S51':'experience writerc(level),d(totalEXP); level1..255 validated,EXP0..FFFFFFFF; nativecopiesfullc/dbits includingout-of-domainlevel0.',
        'S52':'armorClass writerc(AC),c(fire),c(water),c(wind),c(earth); signedbyte validation; primitiveNative reader copyraw8.',
        'S56':'magicStatus writerc(SPmodifier),c(MRmodifier); signedbyte validation andsame-family naming areJava semantic evidence,not expandednative downstream arithmetic.'},
      'numeric_patched_profiles':profile_rows,'profile_boundary':'Existingnumeric opt-ins needmatchedpatched client; all8sealedframes arebaseline lengths. No currentserver environment/profile or patched receiver runtime proven.',
      'sequence_boundary':'Sealedcapture sequence is proven for8records. Comparing existing method wire layout does not establish every current source caller or replay originalAD368 entryflow.',
      'classification':'KNOWN_CONTRACT_CORROBORATION','new_wire_differences':0,'new_opcodes':0,'wire_corrections_required':0,
      'Java_SQL_tests_producer_oldPASS_executed':False,'server_product_or_packet_delta_modified':False}
    write('status-family-50-51-52-56/server-method-comparison.json',provider)
    lines=ASM.read_text(encoding='utf-8').splitlines();nav=[]
    for label,a,b in [('S50',365321,365344),('S51',365352,365363),('S52',365371,365385),('S56',365609,365620),
      ('caller50',373246,373257),('caller51',373261,373272),('caller52',373276,373287),('caller56',373327,373338)]:
        nav.append({'label':label,'first_line':a,'last_line':b,'text':'\n'.join(f'{i+1}: {lines[i]}'for i in range(a-1,b))})
    write('status-family-50-51-52-56/ASM-navigation.json',{'source':sources[2],'navigation':nav,'authority':'ASM labels/navigation only; claimedinstructions/format/maps checked againstfixedPE raw'})
    write('status-family-50-51-52-56/ranges.json',{'fixed_PE':sources[1],'ranges':ranges,'inherited':inherited,'new_code_complete':True})
    verification={'sources':sources,'new_raw_ranges':18,'new_code_ranges':8,'new_data_ranges':10,'new_instructions_asserted':72,
      'inherited_instruction_assertions':16,'instruction_assertions':assertions,'prior_primary_pins':preserved,
      'new_and_inherited_bytes_match_fixed_PE':True,'dispatch_assertions':8,'actual_frames':8,'actual_fields':32,'new_static_checks':16,
      'historical_pre_batch_seal_metadata':{'verification':record(ROOT/'delivery-verification.json'),'manifest':record(ROOT/'artifact-manifest.json'),
        'meaning':'Historical prebatch pins, expected to differ from postpublication currentaggregate. Not currentcore drift.'}}
    write('status-family-50-51-52-56/new-evidence-verification.json',verification)
    reusable=[record(p)for p in OUT.rglob('*')if p.is_file()]
    write('status-family-50-51-52-56-contract.json',{'schema':'lito.native.status-family50-51-52-56.batch12.v1','updated_at':stamp(),
      'status':'FINITE_FOUR_STATUS_HANDLERS_STATIC_CONTRACT_COMPLETE','sources':sources,
      'scope':'ExactlyS50/S51/S52/S56; completehandler/twodispatch/singledirectcaller each andneedednumericc/d directcallee reused. Nootheropcode/deeperprivate/wholeprogram expansion.',
      'prior_primary_pins':preserved,'inherited_evidence':inherited,'reusable_outputs':sorted(reusable,key=lambda x:x['path']),
      'contracts':contracts,'dispatch':dispatch,'state_and_call_boundaries':boundary,'AD368_comparison':record(OUT/'AD368-sealed-frame-comparison.json'),
      'server_comparison':record(OUT/'server-method-comparison.json'),'evidence_levels':{
        'FIXED_PE_RAW':'VA/RVA,rawbytes,formats,dispatch,offsets,copywidth,endian,destinations,directcalls',
        'SEALED_CAPTURE':'Actual8bodies32values/order inexistinghash-pinnedframefile; nooriginaldecoderexecution claim',
        'EXISTING_JAVA_SOURCE':'Names/domain/signedviews/numericpatchedprofiles; noJavaexecution',
        'UNOBSERVED_RUNTIME':'Livebuffers/globals/helpercompletion/owner/actor/player/GUI allunqualified'},
      'server_handoff':{'classification':'KNOWN_CONTRACT_CORROBORATION','new_wire_differences':0,'new_opcodes':0,'wire_corrections':0,'packet_delta_unchanged':True},
      'runtime_acceptance':{'live_original_or_Modern_handler_completed':False,'skill_owner_ready':False,'actor_ready':False,
        'player_ready':False,'GUI_accepted':False,'overall_unpacking_accepted':False},
      'constraints':{'writes_only_own_directory':True,'native_observer_live_process_input_injection_screenshot_runs':0,
        'Java_SQL_tests_producer_runs':0,'new_capture_or_archive_extraction':0,'other_group_changes':0,'blocked_server_work_or_S0_policy':False}})
    print(json.dumps({'report':record(ROOT/'status-family-50-51-52-56-contract.json'),'new_raw_ranges':len(ranges),'new_instructions':len(expected_new),
      'inherited_assertions':len(expected_old),'actual_frames':len(actual),'actual_fields':32,'new_static_checks':len(checks)},ensure_ascii=False))

if __name__=='__main__':main()

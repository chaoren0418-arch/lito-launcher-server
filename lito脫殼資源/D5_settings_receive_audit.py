"""Batch23: finite D5 subtype0/1 stores, direct RET helper, independent snapshots."""
from audit import *
OUT=ROOT/'D5-settings-receive'
FIXED='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
PREVIOUS='04EBECA473FF0BD0FC6A0EEDDE36AF8819EC2FE59F037D08367BF7EF38B36307'
SERVER='A13ECBFE9DC9BFCBA1D32649C829FB960DA995591C59C8613BB403FA4D62C168'

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def project_map(word):
    return (word&255) if word in (0xFFFF,0xFF9C) else ((word&255)+12)&255

def selection_projection(count_byte, values, previous):
    signed=count_byte if count_byte<128 else count_byte-256
    reads=max(0,min(8,signed))
    assert len(values)>=reads and len(previous)==8
    return list(values[:reads])+[0]*(8-reads),reads

def gate_projection(phase, previous_room, incoming_room, subtype):
    assigned_room=incoming_room if phase==6 else previous_room
    room_pass=phase==6 or previous_room==incoming_room
    signed_type=subtype if subtype<128 else subtype-256
    dispatch=room_pass and (signed_type&0xFFFFFFFF)<=9
    return {'room_after_gate':assigned_room,'payload_dispatch':dispatch}

def main():
    OUT.mkdir(exist_ok=True)
    sources=[record(p)for p in (PE,BIN,ASM)]
    assert [p['sha256']for p in sources]==[FIXED,'CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292','2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A']
    previous=load('map-candidate-index-contract.json');previous_pin=record(ROOT/'map-candidate-index-contract.json')
    assert previous_pin['sha256']==PREVIOUS
    prior=previous['prior_primary_pins']+[{**previous_pin,'expected_sha256':PREVIOUS,'match':True}]
    assert len(prior)==24
    for p in prior:
        q=record(Path(p['path']));assert q['sha256']==p['expected_sha256'] and q['bytes']==p['bytes']
    b18pin=record(ROOT/'D5-CF-receive-contract.json')
    assert b18pin['sha256']=='C8E20CD020392C0D26E21F8FB1B830B9009998B942CABF65EBDF59AA355EDA2C'
    old=load('D5-CF-receive/ranges.json');oldpin=record(ROOT/'D5-CF-receive/ranges.json')
    assert oldpin['sha256']=='75D5F21B7CADA8A8AD5ECA6660B6C97A11821A940FF2E33DA80F0C8D22AE40B4'
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    ins={};owners={};ranges=[];reused=[]
    def decode(raw,va,pin):
        rows=[]
        for i in cs.disasm(raw,va):
            r={'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}
            rows.append(r);ins[i.address]=r;owners[i.address]=pin
        assert sum(len(bytes.fromhex(x['raw_hex']))for x in rows)==len(raw)
        return rows
    def reuse(va,n,kind,inherited=False):
        row=next(x for x in old['inherited_raw' if inherited else 'ranges']if int(x['va'],16)==va)
        pin=row['range' if inherited else 'output'];raw=Path(pin['path']).read_bytes()[:n]
        assert record(Path(pin['path']))==pin and raw==pe.get_data(va-0x400000,n)
        r={**mapping(pe,va),'used_bytes':n,'kind':kind,'source_pin':pin,
           'Batch18_index':oldpin,'raw_hex':raw.hex(),'old_PASS_replayed':False,
           'instructions':decode(raw,va,pin)if kind=='code'else[]}
        reused.append(r)
    for va,n,kind in [(0x4A9E50,91,'code'),(0x4A9EAB,62,'code'),(0x4A9EF6,116,'code'),
                      (0x4AA06C,5,'code'),(0x4AA074,40,'data'),(0x634470,10,'data'),(0x634464,10,'data')]:reuse(va,n,kind)
    for va,n,kind in [(0x4A2A50,296,'code'),(0x5028F8,3,'data'),(0x5025A8,2,'data'),
                      (0x4A9030,1,'code'),(0x4A2B78,24,'data'),(0x4A2B90,33,'data')]:reuse(va,n,kind,True)
    # Only the two holes in already sealed subtype0/control flow are new raw.
    for va,n,label in [(0x4A9EE9,13,'subtype0_postparse_noop_call_and_projection_jump'),
                       (0x4A9F89,28,'subtype0_shared_map_WORD_to_lowBYTE_projection_complete_tail')]:
        raw=pe.get_data(va-0x400000,n);p=OUT/f'{va:08X}-{n:X}.bin';p.write_bytes(raw);pin=record(p)
        ranges.append({**mapping(pe,va),'bytes':n,'kind':'code','label':label,
                       'raw_hex':raw.hex(),'output':pin,'instructions':decode(raw,va,pin)})
    def w(at):return {'fixed_PE_sha256':FIXED,**mapping(pe,at),**ins[at],'range':owners[at]}
    expected=[
      (0x4A9E77,'cmp','eax, 6'),(0x4A9E7F,'mov','dword ptr [0x64abf4], eax'),
      (0x4A9E8F,'cmp','ecx, eax'),(0x4A9E91,'jne','0x4aa06c'),
      (0x4A9E97,'movsx','eax, byte ptr [ebp - 1]'),(0x4A9E9E,'ja','0x4aa06c'),
      (0x4A9EAB,'push','0x64acd3'),(0x4A9EB0,'push','0x64acd2'),(0x4A9EB5,'push','0x64acd1'),(0x4A9EBA,'push','0x64acd0'),
      (0x4A9EBF,'push','0x64abca'),(0x4A9EC4,'push','0x1c'),(0x4A9EC6,'lea','edx, [ebp - 4]'),
      (0x4A9EC9,'push','0x64accc'),(0x4A9ECE,'push','edx'),(0x4A9ECF,'push','0x64acca'),
      (0x4A9ED4,'push','0x64ab70'),(0x4A9ED9,'push','0x5a'),(0x4A9EDB,'add','esi, 6'),
      (0x4A9EDE,'push','0x634470'),(0x4A9EE4,'call','0x4a2a50'),
      (0x4A9EE9,'add','esp, 0x34'),(0x4A9EEC,'call','0x4a9030'),(0x4A9030,'ret',''),(0x4A9EF1,'jmp','0x4a9f89'),
      (0x4A9F89,'mov','ax, word ptr [ebp - 4]'),(0x4A9F8D,'cmp','ax, 0xffff'),(0x4A9F91,'je','0x4a9f9b'),
      (0x4A9F93,'cmp','ax, 0xff9c'),(0x4A9F97,'je','0x4a9f9b'),(0x4A9F99,'add','al, 0xc'),
      (0x4A9F9B,'mov','byte ptr [0x64acc9], al'),(0x4A9FA4,'ret',''),
      (0x4A9EF6,'lea','eax, [ebp - 2]'),(0x4A9EFC,'push','eax'),
      (0x4A9EFD,'push','0x64acd7'),(0x4A9F02,'push','0x64acd6'),(0x4A9F07,'push','0x64acd5'),(0x4A9F0C,'push','0x64acd4'),
      (0x4A9F11,'push','0x64abf0'),(0x4A9F16,'push','0x64abe6'),(0x4A9F1B,'push','0x64abe8'),
      (0x4A9F20,'push','5'),(0x4A9F22,'push','0x64ab70'),(0x4A9F27,'push','0x5a'),
      (0x4A9F29,'push','0x634464'),(0x4A9F2F,'call','0x4a2a50'),(0x4A9F37,'xor','esi, esi'),
      (0x4A9F39,'movsx','ecx, byte ptr [ebp - 2]'),(0x4A9F3D,'cmp','esi, ecx'),(0x4A9F3F,'jge','0x4a9f58'),
      (0x4A9F41,'lea','edx, [esi + 0x64acd8]'),(0x4A9F48,'push','0x5025a8'),(0x4A9F4D,'push','eax'),
      (0x4A9F4E,'call','0x4a2a50'),(0x4A9F58,'mov','byte ptr [esi + 0x64acd8], 0'),
      (0x4A9F60,'cmp','esi, 8'),(0x4A9F63,'jl','0x4a9f39'),(0x4A9F69,'ret',''),
      (0x4A2A93,'mov','byte ptr [edx], al'),(0x4A2AB1,'mov','word ptr [eax], cx'),
      (0x4A2AE3,'mov','dword ptr [eax], ecx'),(0x4A2AF9,'call','0x77409b70'),
      (0x4A2B04,'mov','byte ptr [ecx + edi - 1], 0'),(0x4A2B09,'call','0x774095e0'),
      (0x4A2B69,'mov','eax, ebx')]
    assertions=[]
    for va,mn,op in expected:
        assert (ins[va]['mnemonic'],ins[va]['operands'])==(mn,op),(hex(va),ins[va])
        assertions.append({**w(va),'expected_mnemonic':mn,'expected_operands':op,'match':True})
    branches=[]
    # Only subtype0/1/gate/projection conditional CFG, not unrelated parser types.
    for lo,hi in [(0x4A9E50,0x4A9EA4),(0x4A9EE9,0x4A9EF6),(0x4A9EF6,0x4A9F6A),(0x4A9F89,0x4A9FA5)]:
        for va,i in sorted(ins.items()):
            if lo<=va<hi and i['mnemonic'].startswith('j') and i['operands'].startswith('0x'):
                target=int(i['operands'],16);assert target in ins
                branches.append({'va':f'{va:08X}','target':f'{target:08X}','target_is_instruction_boundary':True})
    assert pe.get_data(0x234470,10)==b'schcscccc\0' and pe.get_data(0x234464,10)==b'ssccccccc\0'
    jump=struct.unpack('<10I',pe.get_data(0xAA074,40));assert jump[:2]==(0x4A9EAB,0x4A9EF6)
    # Existing reader dispatch maps c/d/h/s to their saved typed-reader targets.
    parserjump=struct.unpack('<6I',pe.get_data(0xA2B78,24));parsermap=pe.get_data(0xA2B90,33)
    for char,target in [('c',0x4A2A8A),('d',0x4A2AB9),('h',0x4A2A9A),('s',0x4A2AE7)]:
        assert parserjump[parsermap[ord(char)-ord('S')]]==target
    server_path=ROOT.parent/'lito伺服器/server-ordinary-room-DD-edit-network.json'
    server_pin=record(server_path);assert server_pin['sha256']==SERVER
    server=json.loads(server_path.read_text(encoding='utf-8'));frames_pin=server['new_network']['frames']
    assert record(Path(frames_pin['path']))==frames_pin
    rows=json.loads(Path(frames_pin['path']).read_text(encoding='utf-8'))
    def field(cursor,typ,value,raw,semantic,destination):
        return {'body_offset':cursor,'type':typ,'byte_length':len(raw),'raw_hex':raw.hex(),
                'value':value,'semantic':semantic,'Native_destination':destination}
    def parse_frame(index,subtype):
        row=rows[index];body=bytes.fromhex(row['body_hex']);assert row['direction']=='S' and body[:6]==bytes([0xD5,subtype,1,0,0,0])
        cursor=6;fields=[field(1,'u8',subtype,body[1:2],'subtype','EBP-1'),field(2,'u32LE',1,body[2:6],'roomcontext','EBP-8 / conditional64ABF4')]
        def string(semantic,dest):
            nonlocal cursor
            end=body.index(0,cursor)+1;raw=body[cursor:end]
            fields.append(field(cursor,'NUL8bit-string',raw[:-1].decode('ascii'),raw,semantic,dest));cursor=end
        def scalar(n,typ,semantic,dest):
            nonlocal cursor
            raw=body[cursor:cursor+n];assert len(raw)==n
            fields.append(field(cursor,typ,int.from_bytes(raw,'little'),raw,semantic,dest));cursor+=n
        string('room_name','64AB70, capacity90')
        if subtype==0:
            scalar(1,'u8','option0 / mode byte (existing server designation)','64ACCA BYTE')
            scalar(2,'u16LE','map_word','EBP-4 WORD; projected64ACC9 BYTE')
            scalar(1,'u8','summary0','64ACCC BYTE')
            string('owner_display_alias','64ABCA, capacity28')
            for n in range(4):scalar(1,'u8','summary'+str(n+1),f'{0x64ACD0+n:08X} BYTE')
        else:
            string('password','64ABE8, capacity5')
            scalar(1,'u8','attributes','64ABE6 BYTE')
            scalar(1,'u8','capacity','64ABF0 BYTE')
            for n in range(4):scalar(1,'u8','option'+str(n+1),f'{0x64ACD4+n:08X} BYTE')
            k=body[cursor];scalar(1,'u8','selection_count K','EBP-2 BYTE, signed comparison')
            for n in range(k):scalar(1,'u8','selection'+str(n),f'{0x64ACD8+n:08X} BYTE')
        assert cursor==len(body) and row['frame_bytes']==len(body)+2
        return {'frames_source':frames_pin,'array_index_zero_based':index,'frame_1based':index+1,
                'epoch':row['epoch'],'direction':'S','opcode_hex':'D5','subtype':subtype,
                'body_hex':body.hex(),'body_bytes':len(body),'frame_bytes':row['frame_bytes'],
                'fields':fields,'consumed':cursor,'remaining':0,
                'witness_qualification':'Existing server saved ASCII field witness only; not Native execution, ACK, semantic outcome, codec or atomicity proof.'}
    captures=[parse_frame(i,s)for i,s in [(78,0),(79,1),(84,0),(85,1)]]
    assert captures[0]['body_hex']==captures[2]['body_hex'] and captures[1]['body_hex']==captures[3]['body_hex']
    assert [x['body_bytes']for x in captures]==[47,28,47,28]
    fields0=[
      {'name':'room_name','type':'NUL8bit-string','destination':'0064AB70','capacity_including_NUL':90},
      {'name':'option0/mode candidate byte','type':'u8','destination':'0064ACCA','width':1,'preserved_adjacent_byte':'0064ACCB'},
      {'name':'map_word','type':'u16LE','destination':'EBP-4','projection_destination':'0064ACC9','projection_width':1},
      {'name':'summary0','type':'u8','destination':'0064ACCC','width':1},
      {'name':'owner_display_alias','type':'NUL8bit-string','destination':'0064ABCA','capacity_including_NUL':28},
      *[{'name':f'summary{n+1}','type':'u8','destination':f'{0x64ACD0+n:08X}','width':1}for n in range(4)],
    ]
    fields1=[
      {'name':'room_name','type':'NUL8bit-string','destination':'0064AB70','capacity_including_NUL':90},
      {'name':'password','type':'NUL8bit-string','destination':'0064ABE8','capacity_including_NUL':5},
      {'name':'attributes','type':'u8','destination':'0064ABE6','width':1},
      {'name':'capacity','type':'u8','destination':'0064ABF0','width':1,'preserved_adjacent_bytes':'0064ABF1..0064ABF3; no DWORD capacity store in this branch'},
      *[{'name':f'option{n+1}','type':'u8','destination':f'{0x64ACD4+n:08X}','width':1}for n in range(4)],
      {'name':'selection_count K','type':'u8 wire / signed i8 local','destination':'EBP-2','persistent_count_store':False},
      {'name':'selections','type':'8 BYTE slots','destination':'0064ACD8..0064ACDF',
       'reads':'min(8,max(0,signextend_i8(K))); unread slots explicitly zeroed',
       'exact_end_or_capacity_validation':'none in finite handler; known AD368 producer constraints separate'},
    ]
    gate={
        'inheritance':b18pin,'dispatch_qualification':'Batch18 mode4/signed phase>=3 primary and fallback D5 dispatch→4B2F67→4A9E50 retained; no whole-dispatch replay.',
        'header':'cd from body+1; subtype localEBP-1, roomDWORD localEBP-8; payload body+6',
        'phase6':'At4A9E7F incoming room overwrites64ABF4 before subtype validity; no prior room equality.',
        'other_phases':'At4A9E8F compare incoming room to64ABF4; mismatch returns4AA06C, match may dispatch without further phase whitelist.',
        'subtype':'MOVSX i8, unsigned JA >9; valid0..9, negative bytes rejected after possible phase6 room overwrite.',
        'version_gate':'No63D54C or version predicate in header/subtype0/subtype1/projection/stub; actual upstream/runtime version still unknown.',
        'owner_gate':'No owner/self/member identity check in these finite paths; server admission outcome not encoded by D5.',
    }
    effects={
        'subtype0':{'entry_VA':'004A9EAB','entry_RVA':'000A9EAB','fields':fields0,
          'order':'4A9EE4 parser(schcscccc) → 4A9EEC call4A9030(RET) → 4A9EF1 jump4A9F89 → 4A9F9B mapBYTE store → return4A9FA4',
          'map_projection':'Full WORD compared exactlyFFFF/FF9C; these preserve lowBYTE FF/9C. Every other WORD adds0C to AL modulo256. No lookup/table/count/locale/version/mode subtraction.',
          'raw_map_WORD_persistence':'LocalEBP-4 only in this path; no global rawWORD copy. Projected64ACC9 not runtime index/map resource ID.',
          'retained_known_fields':['64ABE6 attributes','64ABE8 password buffer','64ABF0 capacity','64ACD4..64ACD7 options','64ACD8..64ACDF selections','64ACCB adjacent mode highBYTE'],
          'UI_effect':'Only direct4A9030, fixedPE single RET. No redraw/catalog mutation/ready clear/phase change qualified by this branch.',
          'qualification':'Stores and ordering conditional on existing parser/external string operations returning; actual runtime string copy/cursor/bounds and UI state unknown.'},
        'subtype1':{'entry_VA':'004A9EF6','entry_RVA':'000A9EF6','fields':fields1,
          'order':'4A9F2F parser(ssccccccc) → fixed8-iteration signedK loop → own return4A9F69',
          'count_effect':'K is stack BYTE only, not count64AFD0 or persistent selection count. K80..FF signednegative consumes0/zeros8; K09..7F consumes8 and leaves excess sourcebytes unknown/unvalidated.',
          'retained_known_fields':['64ABCA owner display','64ACCA mode BYTE','64ACC9 projected map BYTE','64ACCC summary0','64ACD0..64ACD3 summaries','64ACCB mode highBYTE','64ABF1..64ABF3 capacity highbytes'],
          'UI_effect':'No4A9030 call or other directUI call in this complete branch; only typed reader calls and directzero writes. No ready clear/phase change/ACK qualified.',
          'qualification':'No attribute/capacity clamp or exact-end check in finite path; server u8producer/capacity policy separate. Actual buffers/runtime validity unknown.'},
        'preserved_scope':'Retained means no explicit destination in these finite handler paths, excluding unknown string backend effects; not a whole-image writer-absence claim.',
    }
    boundary={
        'reader_inheritance':'Existing4A2A50 typed c/h/d direct stores and EAX cursor return reused. No body expansion for other format types.',
        'strings':'s receives capacity/destination args; first unknown external call4A2AF9→77409B70 STOP. Conditional continuation4A2B04 writes dest+capacity-1=0, then4A2B09→774095E0 STOP; no inferred strcpy/strchr names/codec or actual copied-byte count.',
        'direct_UI':'4A9EEC→4A9030 complete1byteRET reused; no unknown virtual/private callee on this UI edge.',
        'no_execution':'No Native process/GUI/input/provider/SQL/new capture; no actual phase/global/string/UI result qualified.',
        'last_direct_stores':['4A9E7F roomDWORD (gate)','4A2A93 typed BYTE','4A2AB1 local mapWORD','4A9F9B projectedBYTE','4A9F58 unread selectionBYTE zero'],
    }
    snapshots={
        'independent_receive_paths':True,
        'finite_path_evidence':'Each packet re-parses its own cd header and room gate; subtype0 returns after projection, subtype1 returns afterselectionloop. No pending-pair/generation/result field, subtype wait, rollback or combined commit in these finite paths.',
        'shared_field':'Both s parsers target64AB70 room-name buffer; later accepted message may overwrite it, actual external copy unknown.',
        'phase6_cross_room':'Each invocation can overwrite64ABF4 independently; 0(A) then1(B) may leave summaryfieldsA/configfieldsB if providers return. Not current-room authority.',
        'other_phase_cross_room':'Room mismatch returns before subtypepayload, retaining previous settings; Native lacks adapter epoch/ownedroom qualification.',
        'atomicity':'No atomic pair guarantee. Separate wire frames/readers/globalstores are not coherent source snapshots, all-peer delivery or concurrency proof.',
        'ACK':'No success/reject/result field. Server report labels owner success and nonowner rejection; saved0/1 bodies identical. D5 alone cannot acknowledge C_DD or distinguish those outcomes.',
        'ready':'D5 finite branches do not perform D1 state0 /ready reset. Server reported D1 beforeD5 is a separate packet/state event.',
        'Modern_requirement':'Keep each independently validated snapshot tagged by own connection/epoch/current room; preserve rawmapWORD separately from Native64ACC9 projection; keep summary/config independently qualified and do not invent pair/ACK/owner/source/physical authority.',
    }
    map_models=[{'input_mapWORD':x,'output_BYTE':project_map(x),'Native_executed':False}for x in (0,564,0xFFFF,0xFF9C,0xFFFE,0x8000,0x00F8)]
    count_models=[]
    for k in (0,1,7,8,9,127,128,255):
        out,n=selection_projection(k,list(range(1,9)),[0xA5]*8)
        count_models.append({'K_unsigned':k,'K_signed':k if k<128 else k-256,'reads':n,'stores8':out,
                             'bytes_after_8_not_adopted':max(0,k-n)if k<128 else 'signednegative/unknownsuffix','Native_executed':False})
    # Byte-destination model only; string contents/backend effects deliberately unknown.
    footprint0={0x64ACCA:1,0x64ACCC:0,**{0x64ACD0+n:n for n in range(4)},0x64ACC9:project_map(564)}
    footprint1={0x64ABE6:40,0x64ABF0:4,**{0x64ACD4+n:0 for n in range(4)},**{0x64ACD8+n:0 for n in range(8)}}
    sentinel={at:0xA5 for at in range(0x64ABE6,0x64ABF4)}
    sentinel.update({at:0xA5 for at in range(0x64ACC9,0x64ACE0)})
    state0={**sentinel,**footprint0};state01={**state0,**footprint1};state10={**sentinel,**footprint1,**footprint0}
    gate_models=[]
    for phase,current,incoming,subtype in [(6,1,2,0),(6,1,2,1),(6,1,2,10),(6,1,2,255),
                                           (5,1,2,0),(5,1,1,1),(2,1,1,0),(5,1,1,255)]:
        gate_models.append({'phase':phase,'previous_room':current,'incoming_room':incoming,'subtype_byte':subtype,
                            **gate_projection(phase,current,incoming,subtype),'Native_executed':False})
    models={'qualification':'Offline conditional operand models only; no Native/string/backend/runtime execution.',
            'phase_room_subtype_models':gate_models,
            'map_models':map_models,'count_models':count_models,
            'numeric_store_models':{'subtype0_byte_destinations':{f'{k:08X}':v for k,v in footprint0.items()},
                                   'subtype1_byte_destinations':{f'{k:08X}':v for k,v in footprint1.items()},
                                   'subtype0_preserves_config_numeric_fields':all(state0[k]==sentinel[k] for k in footprint1),
                                   'two_orders_numeric_disjoint_fields_equal':state01==state10,
                                   'high_byte64ACCB_preserved':state01[0x64ACCB]==0xA5,
                                   'capacity_highbytes_preserved':[state01[x] for x in range(0x64ABF1,0x64ABF4)],
                                   'strings_and_coherent_pair_not_modeled':True}}
    wire=[{
        'classification':'KNOWN_LAYOUT_CORROBORATION_NEW_NATIVE_STORE_QUALIFICATION_NO_WIRE_DIFFERENCE',
        'direction':'server-to-client','opcode_hex':'D5','subtype':s,
        'VA':f'{va:08X}','RVA':f'{va-0x400000:08X}','fixed_PE_sha256':FIXED,
        'schema':fmt,'fields':fields0 if s==0 else fields1,
        'body_bytes':body,'frame_bytes':frame,
        'conditions':'Inherited Nativephase6assignment /otherphase room-equality; subtypevalid; parser/externalreturns conditional. Server owner/nonowner outcomes separate.',
        'evidence_level':'FixedPE static operands/completefinitebranch + fourexistingserverASCIIframes; noNativeexecution',
        'unknown':['actualepoch/phase/currentroom','stringbackend/codec/bounds/cursor','snapshotcoherence/partialsend/concurrency','NativeGUI/physical/source/owner/sessionauthority'],
    }for s,va,fmt,body,frame in [(0,0x4A9EAB,'cd + schcscccc','16+N+O','18+N+O'),
                                (1,0x4A9EF6,'cd + ssccccccc + selections','15+N+P+K','17+N+P+K')]]
    # Header6 + subtype0 strings2 terminators + eight fixed bytes =16+N+O.
    assert captures[0]['body_bytes']==16+len('EditAfterRoom')+len('EditHost(EditMoon)')
    assert captures[1]['body_bytes']==15+len('EditAfterRoom')
    verification={'new_code_ranges':2,'new_data_ranges':0,'inherited_necessary_raw_ranges':len(reused),
                  'instruction_assertions':len(assertions),'aligned_in_slice_branch_targets':len(branches),
                  'complete_subtype0_control_after_header':True,'complete_subtype1_branch':True,
                  'saved_field_witness_frames':4,'new_capture_frames':0,'map_models':len(map_models),'count_models':len(count_models),'gate_models':len(gate_models),
                  'prior_primary_pins_preserved':24,'all_match':True,'server_packet_unchanged':record(ROOT/'server-packet-delta.json'),
                  'Batch18_Batch22_or_old_PASS_replayed':False}
    outputs={
        'ranges.json':{'source':sources[0],'new_ranges':ranges,'inherited_necessary_raw':reused},
        'instruction-witnesses.json':{'assertions':assertions,'aligned_branches':branches},
        'gate-and-branch-effects.json':{'gate':gate,'effects':effects,'boundaries':boundary},
        'independent-snapshot-boundary.json':snapshots,
        'saved-field-witnesses.json':{'server_contract':server_pin,'frames_source':frames_pin,'frames':captures,
                                    'success_reject_bodies_equal':True,'ACK_qualified':False},
        'operand-models.json':models,'wire-contracts.json':{'known_contracts':wire,'new_wire':[],'wire_corrections':[]},
        'finite-verification.json':verification,
    }
    for n,o in outputs.items():write('D5-settings-receive/'+n,o)
    primary={'schema':'lito.native-D5-subtype0-1-settings-effects.v1','updated_at':stamp(),
             'status':'FINITE_D5_SUBTYPE0_1_BRANCHES_SEALED_RUNTIME_GUI_ATOMICITY_ACK_OPEN',
             'sources':sources,'prior_primary_pins':prior,'Batch18_source_contract':b18pin,
             'existing_evidence':{'Batch18_ranges':oldpin,'server_DD_edit_contract':server_pin,'saved_frames':frames_pin},
             'outputs':[record(p)for p in sorted(OUT.iterdir())if p.is_file()and p.name not in ('READ-ME.txt','independent-review.json')],
             'wire':wire,'gate':gate,'effects':effects,'boundaries':boundary,'snapshots':snapshots,'verification':verification,
             'delta':{'new_wire':[],'new_opcodes':[],'wire_corrections':[],
                      'known_corroboration':['D5subtype0/1fields andowner-success/nonowner-reject identicalsavedbodies'],
                      'new_static_qualification':['subtype0no-op UIcall andunconditionalmapBYTEtail','subtype1directfieldwidths/selection8stores/retainedfields','separatefinitepathsnoatomicpair/ACK'],
                      'packet_modified':False},
             'scope':{'writes':'own resource directory only','Native_GUI_provider_SQL_executed':False,
                      'Java_Modern_BIN_modified':False,'WORLD_private_external_virtual_body_expansion':False,
                      'actual_UI_runtime_resource_or_joined_authority':False,
                      'Batch18_Batch22_or_old_PASS_replayed':False,'finite_batch_complete':True,
                      'stop':'Both finite branch effects complete; string externals/runtime/atomicity/ACK authority remain unknown. No new wire requested.'}}
    write('D5-settings-receive-contract.json',primary)
    print(json.dumps({'primary':record(ROOT/'D5-settings-receive-contract.json'),'verification':verification},ensure_ascii=False))

if __name__=='__main__':main()

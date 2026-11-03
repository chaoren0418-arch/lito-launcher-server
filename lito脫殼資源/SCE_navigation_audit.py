"""Batch14: bounded S_CE dispatch, room stores and direct navigation helpers.
Static PE reads and the already sealed frames only. Writes stay under ROOT.
"""
from audit import *

OUT=ROOT/'SCE-navigation'
FIXED='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
PREVIOUS='004F07E864E160E78D7B87133F45B95739D01497DAD327C13DD36D52F94D0372'
CAPTURE=Path(r'D:\lito-dev-runtime\20261003-supervision\server\battle-skill-network-f0bd3a9dfbf741628b39792b065ff7ba\evidence\frames.json')
MODERN=ROOT.parent/'lito重建版'/'LiTo381-Modern'
SERVER=ROOT.parent/'lito伺服器'/'LiTo381-Server'/'src/main/java/local/lito381/server'

def load(n): return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    OUT.mkdir(exist_ok=True)
    sources=[record(p) for p in [PE,BIN,ASM]]
    assert [x['sha256'] for x in sources]==[FIXED,'CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292','2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A']
    previous=load('reconnect-source-index-contract.json');p=record(ROOT/'reconnect-source-index-contract.json');assert p['sha256']==PREVIOUS
    preserved=previous['prior_primary_pins']+[{**p,'expected_sha256':PREVIOUS,'match':True}]
    assert len(preserved)==15
    for p in preserved: assert record(Path(p['path']))['sha256']==p['expected_sha256']
    packet=record(ROOT/'server-packet-delta.json');assert packet['sha256']=='B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C'
    historical={'manifest':record(ROOT/'artifact-manifest.json'),'delivery':record(ROOT/'delivery-verification.json'),'scope':'Historical pre-batch aggregate pins, not post-publication core dependencies'}
    existing={}
    for key,path,h in [
      ('native_shape',LEGACY/'analysis/NATIVE-JOIN-RESULT-EVIDENCE.json','06EC28B5226C13FC7ABAFF9C6D63DC871B04DC630C68F0FE88F4228AC1248EC1'),
      ('native_result_types',LEGACY/'analysis/NATIVE-LITO-JOIN-RESULT-TYPE-EVIDENCE-0.7.651.json','A9BBF3B1115B0C46A08F7FA0E78FDEC4A37278158FEF30B3EF5BEBBD6640F8D1'),
      ('result_origin_boundary',LEGACY/'analysis/NATIVE-LITO-JOIN-RESULT-ORIGIN-BOUNDARY-EVIDENCE-0.7.652.json','363A70188C3D974989D329C8A14C33CE04EE075489377B7019A28397FA68E999'),
      ('sealed_frames',CAPTURE,'A9BCA0ECC611F76EE1BD2D6BE6F3B512DC5ABD169C60DACEB6FC74C6D7E0D69F'),
      ('Modern_R513_handoff',MODERN/'logs/continuation-20261003/r513-status-family-consumer-handoff.json','1B7476A674C31C5A7D4F158F292B0D55F8DD37F58F5E06A8D4E1A46A668A0386'),
      ('Modern_R513_inputs',MODERN/'logs/continuation-20261003/r513-test-inputs.json','392CD964D27B7B999BCC74587ACA106744A8390A2B676DAA582F37922A47C567')]:
        q=record(path);assert q['sha256']==h;existing[key]=q
    # Current source is a read-only comparison, never authority for the frozen JAR.
    for key,path in [('current_Java_ClientSession',SERVER/'ClientSession.java'),('current_Java_Protocol',SERVER/'Protocol.java'),('current_Modern_guard',MODERN/'src/modern_lito/native_city_runtime.rs')]:existing[key]=record(path)
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    ins={};owners={};ranges=[];inherited=[]
    def decode(raw,va,pin):
        rows=[]
        for i in cs.disasm(raw,va):
            row={'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}
            rows.append(row);ins[i.address]=row;owners[i.address]=pin
        return rows
    reused_data={}
    for name,va in [('priority-complete-tails.json',0x4A2A50),('receive-boundary/ranges.json',0x4C6AE0),('S0C-status-owner/ranges.json',0x4A2B78),('S0C-status-owner/ranges.json',0x4A2B90),
      ('skill-pane-layout/ranges.json',0x450230),('skill-pane-frame-coordinate/ranges.json',0x469CA0),('server-packet-evidence/ranges.json',0x5025A8)]:
        r=next(x for x in load(name)['ranges'] if int(x['va'],16)==va);q=r['output'];raw=Path(q['path']).read_bytes()
        assert record(Path(q['path']))==q and raw==pe.get_data(va-0x400000,len(raw))
        inherited.append({'index':record(ROOT/name),'range':q,'va':f'{va:08X}','raw_matches_fixed_PE':True,'old_tests_or_producer_replayed':False})
        if va in [0x4A2A50,0x4C6AE0,0x450230,0x469CA0]:
            # Decode only necessary function bytes, excluding reader jump/map tables and unrelated tails.
            selected=raw[:0x128] if va==0x4A2A50 else raw
            decode(selected,va,q)
        else:reused_data[va]=raw
    specs=[
      (0x4A8BA0,0x43E,'code','S_CE_complete_handler'),
      (0x4B29DB,0x38,'code','primary_dispatch_selector'),(0x4B313F,0x1E,'code','fallback_dispatch_selector'),
      (0x4B3199,0xC,'code','fallback_S_CE_call'),(0x4B3123,0x1C,'code','primary_default_private_boundary'),
      (0x469CB8,0x11,'code','scene_prefix_dispatch_gap_only'),(0x469EC3,0x108,'code','scene_mode2_only'),
      (0x458830,0x55,'code','scene_record_reset'),
      (0x4B4BB0,0x38,'code','record_copy_helper_complete'),(0x4B3B80,3,'code','fixed_RET8_helper'),
      (0x4A9030,1,'code','fixed_RET_helper'),(0x41B410,0xF,'code','UI_child_lookup'),
      (0x41AA90,5,'code','UI_flag14_set'),(0x41AAA0,5,'code','UI_flag14_clear'),
      (0x41B6C0,0x11,'code','UI_flag8_set'),(0x41B6E0,0x35,'code','UI_flag8_clear_virtual_boundaries'),
      (0x4765F0,0x29,'code','UI_selection_flag_direct_helper'),
      (0x46A520,0x1E,'code','failure_cleanup_and_room_list_request'),
      (0x46A650,0x22,'code','failure_alert_and_embedded_callback'),
      (0x4B353E,1,'data','CE_primary_map_byte'),(0x4B346C,4,'data','CE_primary_target'),
      (0x4B3782,1,'data','CE_fallback_map_byte'),(0x4B3630,4,'data','CE_fallback_target'),
      (0x4A8FE0,0x34,'data','result_jump_table'),(0x4A9014,0x16,'data','result_slot_map'),
      (0x46A244,0x14,'data','scene_mode_jump_table'),(0x634448,0x15,'data','S_CE_success_format')]
    for va,n,kind,label in specs:
        raw=pe.get_data(va-0x400000,n);assert len(raw)==n
        out=OUT/f'{va:08X}-{n:X}.bin';out.write_bytes(raw);pin=record(out)
        decoded=decode(raw,va,pin) if kind=='code' else []
        if kind=='code':assert sum(len(bytes.fromhex(x['raw_hex'])) for x in decoded)==n,(hex(va),'unaligned ending')
        ranges.append({**mapping(pe,va),'bytes':n,'kind':kind,'label':label,'raw_hex':raw.hex(),'output':pin,'instructions':decoded})
    def w(va):return {'source_sha256':FIXED,**mapping(pe,va),**ins[va],'range':owners[va]}
    # Fixed, manually reviewed expectations independent of freshly decoded output.
    expected=[
      (0x4B29DB,'cmp','dword ptr [0x64bbd4], 4'),(0x4B29E8,'cmp','dword ptr [0x64af98], 3'),(0x4B29EF,'jl','0x4b313f'),
      (0x4B2A06,'mov','cl, byte ptr [eax + 0x4b3470]'),(0x4B2A0C,'jmp','dword ptr [ecx*4 + 0x4b3208]'),
      (0x4B3150,'mov','dl, byte ptr [eax + 0x4b36b4]'),(0x4B3156,'jmp','dword ptr [edx*4 + 0x4b3570]'),
      (0x4B3199,'mov','ecx, esi'),(0x4B319B,'call','0x4a8ba0'),(0x4B3135,'call','0x5ad0986'),
      (0x4A8BAD,'lea','edi, [ecx + 1]'),(0x4A8BB7,'call','0x4a2a50'),(0x4A8BBC,'movsx','eax, byte ptr [ebp - 2]'),
      (0x4A8BC3,'cmp','eax, 0x15'),(0x4A8BCA,'ja','0x4a8fd4'),(0x4A8BD2,'mov','cl, byte ptr [eax + 0x4a9014]'),
      (0x4A8BD8,'jmp','dword ptr [ecx*4 + 0x4a8fe0]'),
      (0x4A8BEE,'mov','ecx, dword ptr [0x64ab68]'),(0x4A8BFB,'mov','edx, ecx'),(0x4A8BFD,'call','0x4b4bb0'),
      (0x4A8C11,'call','0x4b3b80'),(0x4A8C1C,'mov','dword ptr [0x64ab68], esi'),
      (0x4A8C49,'mov','ecx, dword ptr [0x64ab58]'),(0x4A8C55,'mov','edx, ecx'),(0x4A8CDC,'mov','dword ptr [0x64ab58], esi'),
      (0x4A8C8F,'push','0x1c'),(0x4A8CBF,'push','5'),(0x4A8CCB,'push','0x5a'),(0x4A8CD6,'push','0x634448'),
      (0x4A8CE2,'call','0x4a2a50'),(0x4A8CE7,'add','esp, 0x64'),(0x4A8CEC,'movsx','ecx, byte ptr [ebp - 1]'),
      (0x4A8CF2,'jge','0x4a8d0b'),(0x4A8D01,'call','0x4a2a50'),(0x4A8D0B,'mov','byte ptr [esi + 0x64acd8], 0'),
      (0x4A8D13,'cmp','esi, 8'),(0x4A8D18,'call','0x4a9030'),(0x4A9030,'ret',''),
      (0x4A8D21,'cmp','ax, 0xffff'),(0x4A8D28,'cmp','ax, 0xff9c'),(0x4A8D2E,'add','al, 0xc'),
      (0x4A8D30,'mov','byte ptr [0x64acc9], al'),(0x4A8D35,'mov','al, byte ptr [0x64afc7]'),
      (0x4A8D3C,'mov','ecx, 2'),(0x4A8D43,'call','0x469ca0'),(0x4A8D8A,'call','0x469ca0'),
      (0x4A8D59,'mov','dword ptr [0x64a9b4], eax'),(0x4A8D5E,'mov','dword ptr [0x64a9b8], ecx'),
      (0x4A8D64,'mov','dword ptr [0x64a9bc], edx'),(0x4A8D6A,'mov','dword ptr [0x62c9c0], 0x469bd0'),
      (0x4A8D74,'mov','dword ptr [0x62c9bc], 0x47b700'),(0x4A8D7E,'mov','dword ptr [0x62c9b8], 0x47b6f0'),
      (0x4A8D8F,'call','0x458830'),(0x4A8D9F,'xor','eax, 0xb104fa87'),(0x4A8DAC,'xor','edx, eax'),
      (0x4A8DAE,'jne','0x4a8fd9'),(0x4A8DDB,'call','0x773f5200'),(0x4A8DF6,'call','0x458bb0'),
      (0x4A8E4F,'test','eax, eax'),(0x4A8E57,'mov','al, byte ptr [0x64afbd]'),(0x4A8E64,'mov','al, byte ptr [0x64afbc]'),
      (0x4A8FD4,'call','0x46a520'),(0x469CA0,'mov','al, byte ptr [0x62c21c]'),(0x469CAB,'movsx','eax, cx'),
      (0x469CB1,'mov','dword ptr [0x64af98], eax'),(0x469CC2,'jmp','dword ptr [eax*4 + 0x46a244]'),
      (0x469EC9,'mov','dword ptr [0x64af98], 2'),(0x469ED5,'call','dword ptr [edx + 0x50]'),
      (0x469F51,'cmp','esi, 8'),(0x469F99,'mov','dword ptr [0x62c9b8], 0x46d640'),
      (0x469FA3,'mov','dword ptr [0x62c9bc], 0x46d8a0'),(0x469FAD,'mov','dword ptr [0x62c9c0], 0x46d630'),
      (0x469FB9,'call','dword ptr [edx + 0x4c]'),(0x469FBC,'mov','dword ptr [0x75a998], ebx'),
      (0x469FC2,'mov','dword ptr [0x75a99c], ebx'),(0x458831,'mov','ecx, 0x514'),
      (0x458838,'mov','edi, 0x6494a0'),(0x45883D,'rep stosd','dword ptr es:[edi], eax'),
      (0x45884B,'mov','word ptr [eax], cx'),(0x45884E,'add','eax, 0x68'),(0x458851,'cmp','eax, 0x64a956'),
      (0x45886B,'idiv','ecx'),(0x45886D,'mov','dword ptr [0x64949c], 0'),(0x458877,'mov','byte ptr [0x649490], 1'),
      (0x45887E,'mov','dword ptr [0x649498], edx'),(0x4B4BB6,'cmp','ebx, edx'),(0x4B4BB8,'je','0x4b4be0'),
      (0x4B4BE0,'mov','eax, dword ptr [ebp + 8]'),(0x4B3B80,'ret','8'),
      (0x45024C,'call','dword ptr [edx]'),(0x450256,'mov','dword ptr [esi + 0x2c], ebx'),
      (0x450259,'mov','dword ptr [esi + 0x28], ebx'),(0x41AA90,'mov','byte ptr [ecx + 0x14], 1'),
      (0x41AAA0,'mov','byte ptr [ecx + 0x14], 0'),(0x41B418,'mov','eax, dword ptr [eax + ecx*4]'),
      (0x41B6CC,'mov','byte ptr [ecx + 8], 1'),(0x41B6E0,'mov','byte ptr [ecx + 8], 0'),
      (0x4765F8,'mov','byte ptr [ecx + 0x30], al'),(0x47660B,'mov','eax, dword ptr [ecx + 0x38]'),
      (0x46A52B,'push','0xce'),(0x46A535,'call','0x4c6ae0'),(0x46A65A,'call','0x4980f0'),
      (0x4A2AF9,'call','0x77409b70'),(0x4A2B04,'mov','byte ptr [ecx + edi - 1], 0'),
      (0x4A2B09,'call','0x774095e0'),(0x4A2B12,'inc','eax'),(0x4A2B13,'mov','ebx, eax')]
    assertions=[]
    for va,mn,op in expected:
        assert (ins[va]['mnemonic'],ins[va]['operands'])==(mn,op),(hex(va),ins.get(va))
        assertions.append({**w(va),'expected_mnemonic':mn,'expected_operands':op,'match':True})
    raw_at={int(r['va'],16):bytes.fromhex(r['raw_hex']) for r in ranges};raw_at.update(reused_data)
    assert raw_at[0x4B353E]==b'\x99' and struct.unpack('<I',raw_at[0x4B346C])[0]==0x4B3123
    assert raw_at[0x4B3782]==b'\x30' and struct.unpack('<I',raw_at[0x4B3630])[0]==0x4B3199
    assert 0x4B3470+0xCE==0x4B353E and 0x4B3208+153*4==0x4B346C
    assert 0x4B36B4+0xCE==0x4B3782 and 0x4B3570+48*4==0x4B3630
    slots=list(raw_at[0x4A9014]);assert slots==[0,12,12,1,2,3,12,4,12,2,5,6,7,8,9,12,12,12,12,12,10,11]
    targets=list(struct.unpack('<13I',raw_at[0x4A8FE0]));assert targets==[0x4A8BDF,0x4A8E00,0x4A8E25,0x4A8E4A,0x4A8E95,0x4A8EBD,0x4A8EE2,0x4A8F09,0x4A8F31,0x4A8F59,0x4A8F7C,0x4A8FA3,0x4A8FD4]
    assert struct.unpack('<5I',raw_at[0x46A244])[1]==0x469EC3
    fmt=raw_at[0x634448];assert fmt==b'cdscshhchcccccsccccc\0' and raw_at[0x5025A8]==b'c\0'
    fields=[('result','local EBP-2',None),('room_id','0064ABF4',None),('room_name','0064AB70',90),
      ('attributes','0064ABE6',None),('password','0064ABE8',5),('users','0064ABEE',None),('capacity','0064ABF0',None),
      ('mode','0064ACCA',None),('map_wire','local EBP-4',None),('summary0','0064ACCC',None),
      ('summary1','0064ACD0',None),('summary2','0064ACD3',None),('summary3','0064ACD1',None),('summary4','0064ACD2',None),
      ('native_label','0064ABCA',28),('option1','0064ACD4',None),('option2','0064ACD5',None),
      ('option3','0064ACD6',None),('option4','0064ACD7',None),('selection_count','local EBP-1',None)]
    pushsites=[0x4A8CD5,0x4A8CD0,0x4A8CC6,0x4A8CC1,0x4A8CBA,0x4A8CB5,0x4A8CB0,0x4A8CAB,0x4A8CAA,
      0x4A8CA5,0x4A8CA0,0x4A8C9B,0x4A8C96,0x4A8C91,0x4A8C8A,0x4A8C85,0x4A8C80,0x4A8C7B,0x4A8C76,0x4A8C75]
    wire=[]
    for i,((name,dest,cap),code,at) in enumerate(zip(fields,fmt[:-1].decode(),pushsites)):
        assert ins[at]['mnemonic']=='push'
        if dest.startswith('0064'):assert ins[at]['operands']==hex(int(dest,16))
        wire.append({'index':i,'name':name,'format':code,'native_destination':dest,'write_bytes':{'c':1,'h':2,'d':4}.get(code),
          'destination_capacity_bytes':cap,'argument_witness':w(at),'interpretation':'Raw bit copy; signed/domain views separate' if code!='s' else 'Opaque NUL-terminated bytes; local destination capacity does not prove frame bound or encoding'})
    assert len(wire)==20
    frame_rows=json.loads(CAPTURE.read_text(encoding='utf-8'));request=frame_rows[64];reply=frame_rows[65]
    assert (request['direction'],reply['direction'],request['epoch'],reply['epoch'])==('C','S','Host-GAME','Host-GAME')
    body=bytes.fromhex(reply['body_hex']);assert len(body)==64 and body[0]==0xCE and reply['frame_bytes']==66
    pos=1;decoded=[]
    for f in wire:
        start=pos
        if f['format']=='s':
            end=body.index(0,pos);value=body[pos:end].hex();pos=end+1
        else:
            n=f['write_bytes'];value=int.from_bytes(body[pos:pos+n],'little');pos+=n
        decoded.append({**f,'body_offset':start,'wire_bytes':pos-start,'value':value,'wire_hex':body[start:pos].hex()})
    assert pos==64
    actual={x['name']:x['value'] for x in decoded}
    assert actual=={'result':0,'room_id':1,'room_name':b'BattleSkillRoom'.hex(),'attributes':40,'password':'','users':2,'capacity':2,
      'mode':1,'map_wire':564,'summary0':0,'summary1':0,'summary2':0,'summary3':0,'summary4':0,
      'native_label':b'BattleHost(BattleMoon)'.hex(),'option1':0,'option2':0,'option3':0,'option4':0,'selection_count':0}
    assert [x['body_offset']for x in decoded]==[1,2,6,22,23,24,26,28,29,31,32,33,34,35,36,59,60,61,62,63]
    req=bytes.fromhex(request['body_hex']);assert req==bytes.fromhex('cc426174746c65536b696c6c526f6f6d0008000200') and len(req)==21
    request_decode={'opcode':204,'room_name_hex':req[1:16].hex(),'attributes':req[17],'password_hex':'','capacity':int.from_bytes(req[19:21],'little'),'tail':'COMPACT absent option/count tail'}
    assert request_decode['room_name_hex']==b'BattleSkillRoom'.hex() and request_decode['attributes']==8 and request_decode['capacity']==2
    # Carefully distinguish labels: Java puts room owner display name in ABCA, not a proven actor identity.
    capture={'source':existing['sealed_frames'],'new_capture':False,'frame_index_convention':'1-based','request_frame65':request,
      'reply_frame66':reply,'request_decode':request_decode,'reply_decoded_fields':decoded,'reply_body_bytes':64,
      'reply_consumed_by_bounded_shape_model':64,'remaining_bytes':0,'map_native_low_byte':64,
      'identity_boundary':'ABCA is only a native string destination. Frozen capture contains a display label; no owner/player object binding inferred.',
      'intent_boundary':'This sealed pair witnesses C_CC create intent and S_CE room_id1/name reply. No C_CF frame substituted or claimed in this pair.'}
    write('SCE-navigation/sealed-frame65-66-comparison.json',capture)
    dispatch={'eligible_condition':'Fallback: NOT (mode64BBD4==4 AND signed phase64AF98>=3)',
      'primary':{'condition':'mode64BBD4==4 AND signed phase64AF98>=3','map_byte_va':'004B353E','slot':153,'jump_entry_va':'004B346C','target':'004B3123','calls_S_CE_handler':False},
      'fallback':{'map_byte_va':'004B3782','slot':48,'jump_entry_va':'004B3630','target':'004B3199','callsite':w(0x4B319B),'ECX':'ESI body pointer'},
      'selectors':[w(x) for x in [0x4B29DB,0x4B29E8,0x4B29EF,0x4B2A06,0x4B2A0C,0x4B3150,0x4B3156]],
      'default_private_boundary':w(0x4B3135),'bound':'Debug prefix, private restart and sender completion remain unexecuted; dispatch compatibility is not owned intent authorization.'}
    result={'result_byte':'Read c from body+1, MOVSX i8 then unsigned JA>21; negatives/out-of-range go cleanup',
      'map_slots':slots,'targets':[f'{x:08X}'for x in targets],'success_full_parse_only':True,
      'nonzero_room_tail_not_consumed':True,'original_server_failure_predicates_unknown':True,
      'result5_guard':'64AFB8!=0 AND 64AFBD==0 AND 64AFBC==0 for indexed string30; otherwise cleanup',
      'failure_cleanup':'46A520 calls450230 on64A958 then sender4C6AE0 c opcodeCE: client C_CE room-list refresh, distinct direction from server S_CE',
      'sender_completion_unknown':True,'existing_branch_contract':existing['native_shape']}
    scene={'ordered_success_effects':[
      'Optional cleanup of64A950,64A94C,64A968,64A96C through450230; virtual child destruction remains unknown.',
      'Reset list end64AB68 to begin64AB64 and end64AB58 to begin64AB54: at both4B4BB0 calls ECX==EDX==old end, so helper takes empty range branch and returns supplied begin. No record copy in these two calls;4B3B80 is fixed RET8.',
      'Re-read full20-field format from body+1 into explicit native slots. String destination budgets90/5/28; private external string copy/seek completion unqualified.',
      'Signed selection count loop always initializes8 slots64ACD8..DF; reads only min(max(i8(count),0),8) bytes; count>8 leaves tail unread, count>=128 signednegative readsnone. No native frame-length rejection shown.',
      '4A9030 fixed RET; mapWORDFFFF/FF9C preserveAL, allotherwords add0C toAL with8bitwrap; writeBYTE64ACC9.',
      'Call469CA0 withECX2. If62C21C==0 only storephase64AF98=2. Otherwise mode2 directUI/callback/virtual operations precede return.',
      'When64AFC7!=0, after469CA0 returns save then-current62C9B8/BC/C0 to64A9B4/B8/BC and install47B6F0/47B700/469BD0. It saves post-helper callbacks, not necessarily pre-S_CE callbacks.',
      'Call458830: clear5200bytes[6494A0,64A8F0), write50words at649506+i*68 fori0..49 from503478 low16, clear64949C,set649490=1, store signed remainder ((50-[62BCC0]) wrapped32)/50 in649498.',
      'Read protected8316B0/B4/B8 decodedvalue; ifnonzero return. Ifzero notification formatsroom_id through external773F5200 then458BB0. No player/owner creation instruction in bounded handler/reset helpers.'
      ],
      'mode2_direct_globals':{'64AF98':2,'62C9B8':'0046D640','62C9BC':'0046D8A0','62C9C0':'0046D630','75A998':0,'75A99C':0},
      'mode2_direct_UI':[
        '41B6E0 on64AD0C: object+8=0;6414A0=0; may call64149C vtable+18 and6414A4 vtable+10.',
        '41B6C0 on64AD08,64AD10,64AD14: object+8=1;6414A4=0;6414A0=0.',
        '41B410 returns [ [object] +child_index*4 ] without local null/bounds checks.',
        '64AD08 children0..7 get+14=1 thenchildren1,5 get+14=0.',
        '64AD04 child2→4765F0 argument0: child+30=0,+16=1,+20=[child+38].'
      ],
      'mode2_virtual_boundaries':['64AD1C vtable+50 beforeotherUI effects','64A990 and64A98C vtable+24','64B09C,64B098,64A904 vtable+20','64AD1C vtable+4C aftercallbackstores'],
      'prerequisites_unobserved':['Initialized and valid64AD1C/UI object graphs and childarrays','Runtime62C21C,64AFC7,protected8316B0/B4/B8 and callback targets','Virtual implementations and private/external successful returns','Owned connection/intent/control revision and room admission epoch'],
      'source_scene_phase_vs_server_phase':'Native64AF98=2 numeric UI phase is not Java Phase.ROOM enum identity.',
      'Modern_navigation_guard_must_remain':True,'all_navigation_side_effects_qualified':False,'owner_player_GUI_ready':False}
    # Every call in the newly captured bounded code is recorded, including unknown virtual/depth stops.
    calls=[]
    bounded_complete={0x4A8BA0,0x469CA0,0x458830,0x450230,0x4B4BB0,0x4B3B80,0x4A9030,0x41B410,0x41AA90,0x41AAA0,0x41B6C0,0x41B6E0,0x4765F0,0x46A520,0x46A650}
    for r in ranges:
        if r['kind']!='code':continue
        for row in r['instructions']:
            if row['mnemonic']!='call':continue
            op=row['operands'];target=int(op,16) if op.startswith('0x') else None
            classification=('VIRTUAL_TARGET_UNQUALIFIED' if target is None else
              'EXTERNAL_OR_PRIVATE_BODY_UNAVAILABLE' if not 0x400000<=target<0xAC4000 else
              'BOUNDED_MODE2_SLICE_INCLUDED_OTHER_CASES_EXCLUDED' if target==0x469CA0 else
              'EXISTING_DIRECT_HELPER_RAW_REUSED' if target==0x450230 else
              'BOUNDED_DIRECT_BODY_INCLUDED' if target in bounded_complete else
              'EXISTING_RAW_REUSED_NO_EXECUTION' if target in [0x4A2A50,0x4C6AE0] else 'DIRECT_DEPTH_STOP_NOT_EXPANDED')
            calls.append({'callsite':w(int(row['va'],16)),'target':f'{target:08X}' if target is not None else op,'classification':classification,
              'target_mapping':mapping(pe,target) if target is not None else None,'runtime_effect_accepted':False})
    intent={'native_handler':'No request nonce, original C_CC/C_CF opcode, pending-write revision or owned connection identity occurs in this body; it consumes result and room record after eligible dispatch.',
      'request65':'C_CC compactroom settings: nameBattleSkillRoom,attributes08,passwordempty,capacity2. Reply66 name matches; attributes28 adds20 per serverwriter comparison. S_CE room_id1 is allocated replydata absent from C_CC.',
      'C_CF_relation':'Current Java directjoin parser cds: opcodeCF, roomIdDWORD,passwordNULstring, requiresserver CHANNEL. Bothcreateandjoin route toshared S_CE writer; only C_CC is witnessed in this sealedpair.',
      'current_Java_create':{'source':existing['current_Java_ClientSession'],'lines':[11423,11459],'logic':'CHANNEL guard; createRoom then synthetic CF cdroomId+spassword callsjoinRoom(create-room). Equal-sides20 ORattributes;defaultmodeindependentofC_CC.'},
      'current_Java_join':{'source':existing['current_Java_ClientSession'],'lines':[10182,10327],'logic':'CF cd+s parsed;CHANNEL guard; full S_CE writer; Java phaseROOM/currentRoomId after send. Current source only, not frozenJAR provenance proof.'},
      'current_Java_request_shape':{'source':existing['current_Java_Protocol'],'lines':[7083,7144],'logic':'C_CC cs+c+s+h compact or4optionc+countc+selections; no room_id, replysummary/mode not synonymouswithrequestoptions.'},
      'name_domains':'room_name64AB70 identifiesdisplayroom;native_label64ABCA isserverownerdisplaylabelinthiswriter. Neither string proves actorobject/global834888 ownership; C_CFroom_idintent mustmatchreply independently.',
      'Modern_handoff':'Keep owned epoch, explicit create/join intent, completedwrite andopaquecontrol revisions. CorrelateC_CC name/settings orC_CF requestedroom_id withreply; stale/unsolicited/mismatched/controlrevision replies remain refused. This is consumerpolicy, not a discovered native nonce.',
      'current_guard':{'source':existing['current_Modern_guard'],'line':273,'reason':'navigation mode side effects unqualified; retire source epoch','guard_removed':False},
      'sealed_R513_guard_result':'Existing R513 structurallydecodesframe66 thenrefusesnavigation andretiresownedepoch;checkpointpreserved. No new runtime run.'}
    models=[]
    def model(label,actual,expected):assert actual==expected;models.append({'label':label,'actual':actual,'expected':expected,'match':True,'kind':'New code-derived bounded model; not native execution'})
    model('map564_to_lowbyte64',(564+12)&255,64)
    model('mapFFFF_sentinel',0xFFFF&255,255);model('mapFF9C_sentinel',0xFF9C&255,156)
    model('map00FF_lowbyte_wrap',(255+12)&255,11)
    model('signed_count_0_8_9_128_255',[min(max(c if c<128 else c-256,0),8)for c in [0,8,9,128,255]],[0,8,8,0,0])
    model('reset_span_bytes',0x514*4,5200);model('reset_word_count',len(range(0x649506,0x64A956,0x68)),50)
    model('last_reset_word_end',list(range(0x649506,0x64A956,0x68))[-1]+2,0x64A8F0)
    model('dispatch_mode4_phases2_3',[not(m==4 and phase>=3) for m,phase in [(4,2),(4,3),(3,3),(4,-1)]],[True,False,True,True])
    model('result_signed_default',[slots[c] if c<22 else 12 for c in [0,9,21,22,127,128,255]],[0,2,11,12,12,12,12])
    write('SCE-navigation/ranges.json',{'source':sources[0],'ranges':ranges,'inherited_raw':inherited,'scope':'Only completeS_CE/directhelpers andscene prefix+mode2; no other scene cases, S_D0 orworld callbacks expanded'})
    write('SCE-navigation/instruction-witnesses.json',{'source':sources[0],'assertions':assertions,'assertion_count':len(assertions),'wire_argument_witnesses':wire})
    write('SCE-navigation/dispatch-and-room-stores.json',{'dispatch':dispatch,'result_branches':result,'wire_fields':wire,'scene':scene})
    write('SCE-navigation/direct-call-boundaries.json',{'calls':calls,'scene':scene,'intent':intent,'failure_refresh_direction':'C_CE roomlist request; S_CE receivedjoinresult are distinct direction contracts',
      'external_string_names':'Targets77409B70/774095E0 only raw-qualified here; destinationforcedNUL andcursorEAX+1 shown. Names, originalDLLSHA, success andbounded inputseek notproved.'})
    write('SCE-navigation/new-code-derived-models.json',{'models':models,'old_tests_replayed':False,'native_process_or_capture_started':False})
    verification={'sources':sources,'instruction_assertions':len(assertions),'field_argument_witnesses':len(wire),
      'new_code_ranges':sum(x['kind']=='code'for x in ranges),'new_data_ranges':sum(x['kind']=='data'for x in ranges),'inherited_raw_ranges':len(inherited),
      'sealed_frames_compared':2,'S_CE_decoded_fields':20,'code_derived_models':len(models),'prior_primary_pins':preserved,
      'server_packet_unchanged':packet,'historical_prebatch_aggregates':historical,'all_new_checks_match':True,
      'raw_matches_fixedPE':True,'other_group_writes':0,'native_runs':0,'Java_SQL_writes':0,'old_PASS_replays':0}
    write('SCE-navigation/new-evidence-verification.json',verification)
    outputs=[record(p)for p in sorted(OUT.iterdir())if p.is_file()]
    report={'schema':'lito-unpack/SCE-navigation/v1','updated_at':stamp(),'status':'FINITE_NATIVE_SCE_DISPATCH_STORES_HELPER_BOUNDARIES_SEALED_GUARD_RETAINED',
      'sources':sources,'prior_primary_pins':preserved,'existing_evidence':existing,'inherited_raw':inherited,'reusable_outputs':outputs,
      'dispatch':dispatch,'result':result,'wire_fields':wire,'scene':scene,'intent_and_phase':intent,'verification':verification,
      'classification':'KNOWN_S_CE_CONTRACT_CORROBORATION_AND_BOUNDED_NAVIGATION_GAP','new_wire_differences':0,'new_opcodes':0,'wire_corrections':0,
      'server_handoff':{'packet_delta':packet,'modified':False,'success_shape':'Already known20fields+upto8selections; body64 sealedcorroboration','new_producer_validation':False},
      'future_boundary':['S_D0frame69 roster/participantcallbacks, followingroom/world/results are futureonly; no decodes orhandlerexpansion thisbatch'],
      'runtime_acceptance':{'original_game':False,'observer':False,'GUI':False,'owner_player':False,'Modern_guard_removed':False,'private_virtual_effects_closed':False},
      'constraints':{'write_root':str(ROOT),'other_group_writes':0,'native_live_input_injection_screenshots':0,'Java_SQL_BIN_writes':0,'old_PASS_or_capture_replay':0,'S0_policy_expansion':0}}
    for q in existing.values():assert record(Path(q['path']))==q
    assert record(ROOT/'server-packet-delta.json')==packet
    write('SCE-navigation-contract.json',report)
    print(json.dumps({'primary':record(ROOT/'SCE-navigation-contract.json'),'verification':verification},ensure_ascii=False))

if __name__=='__main__':main()

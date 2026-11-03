"""Review conditional CDD varargs/serializer/raw/pins; no Native or old PASS."""
from audit import *

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    p=load('CDD-outbound-contract.json');checks=[]
    def check(name,ok):checks.append({'check':name,'match':bool(ok)});assert ok,name
    for q in p['sources']+list(p['existing_evidence'].values())+p['outputs']:check('pin '+Path(q['path']).name,record(Path(q['path']))==q)
    for q in p['prior_primary_pins']:
        actual=record(Path(q['path']));check('prior '+Path(q['path']).name,actual['bytes']==q['bytes'] and actual['sha256']==q['expected_sha256'])
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);ins={}
    ranges=load('CDD-outbound/ranges.json')
    for r in ranges['new_ranges']+ranges['necessary_reused_raw']:
        q=r.get('output',r.get('source_pin'));va=int(r['va'],16);n=r.get('bytes',r.get('used_bytes'));raw=Path(q['path']).read_bytes()[:n]
        check('source/raw '+r['va'],record(Path(q['path']))==q and raw==bytes.fromhex(r['raw_hex'])==pe.get_data(va-0x400000,n))
        check('VA/RVA/offset '+r['va'],int(r['rva'],16)==va-0x400000 and int(r['file_offset'],16)==pe.get_offset_from_rva(va-0x400000))
        if r.get('kind','code')=='code':
            decoded=[{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in cs.disasm(raw,va)]
            check('alignment '+r['va'],decoded==r['instructions'] and sum(len(bytes.fromhex(i['raw_hex']))for i in decoded)==n)
            ins.update({int(i['va'],16):i for i in decoded})
    w=load('CDD-outbound/instruction-witnesses.json')
    for row in w['assertions']:
        i=ins[int(row['va'],16)];check('operand '+row['va'],(i['mnemonic'],i['operands'],i['raw_hex'])==(row['expected_mnemonic'],row['expected_operands'],row['raw_hex']))
    check('first UI STOP1 virtual raw',ins[0x46107B]['raw_hex']=='ff5254')
    check('first UI STOP2 virtual raw',ins[0x462648]['raw_hex']=='ff5254')
    check('unqualifiedhelper STOP target',ins[0x4C8790]['operands']=='0x4c85e0')
    check('transport STOP fixedexternal raw',ins[0x4EFEA4]['raw_hex']=='e9f7594175' and ins[0x4EFEA4]['operands']=='0x759058a0')
    check('stackprobe no calls',not any(i['mnemonic']=='call'for va,i in ins.items()if 0x4F11A0<=va<0x4F11CF) and ins[0x4F11CE]['mnemonic']=='ret')
    jump=struct.unpack('<7I',pe.get_data(0xC6A74,28));maps=pe.get_data(0xC6A90,33)
    check('serializer c/s/h/b dispatch',[jump[maps[ord(c)-ord('S')]]for c in 'cshb']==[0x4C6916,0x4C6989,0x4C6930,0x4C69D6])
    for lo,hi,name in [(0x4C6916,0x4C6930,'c'),(0x4C6930,0x4C6951,'h'),(0x4C6989,0x4C69D6,'s'),(0x4C69D6,0x4C6A05,'b')]:
        check(name+' path no private/external/virtualcalls',not any(i['mnemonic']=='call'for va,i in ins.items()if lo<=va<hi))
    # Symbolic vararg reconstruction from actual pushes/moves; no UI execution.
    def args(segments,regs):
        stack=[]
        for lo,hi in segments:
            for va,i in sorted(ins.items()):
                if not lo<=va<hi:continue
                mn,op=i['mnemonic'],i['operands']
                if mn=='lea':reg,value=op.split(', ',1);regs[reg]=value
                elif mn=='movsx':reg,value=op.split(', ',1);regs[reg]='sx('+value+')'
                elif mn=='push':stack.append(regs.get(op,op))
                elif mn=='call':return list(reversed(stack))
        return list(reversed(stack))
    compact=args([(0x4611B8,0x4611CC),(0x461231,0x46123B)],{'edi':'EDI'})
    zero=args([(0x461181,0x4611A7)],{'edi':'EDI'})
    extended=args([(0x462898,0x4628CB),(0x4628D0,0x4628DA)],{'edi':'EDI','edx':'sx(byte ptr [ebp - 1])'})
    # Compact/zero format and stack source order must exactly match parser.
    check('compact six stackargs',compact==['0x62bf90','0xdd','[ebp - 0x70]','sx(bl)','[ebp - 0x14]','EDI'])
    check('zeroextended eleven stackargs',zero==['0x62bf98','0xdd','[ebp - 0x70]','sx(bl)','[ebp - 0x14]','EDI','0','0','0','0','0'])
    check('extended thirteen stackargs',extended==['0x62bfa4','0xdd','[ebp - 0x90]','sx(byte ptr [ebp - 2])','[ebp - 0x24]','EDI',
                                                  'sx(byte ptr [ebp - 3])','sx(byte ptr [ebp - 4])','sx(byte ptr [ebp - 5])','sx(byte ptr [ebp - 1])','sx(al)','sx(al)','[ebp - 0x2c]'])
    check('K repeated for ccount and bcopy',extended[-4:]==['sx(byte ptr [ebp - 1])','sx(al)','sx(al)','[ebp - 0x2c]'])
    check('Native conditional capacity2..200',ins[0x46111E]['operands']==ins[0x46283A]['operands']=='edi, 1' and ins[0x461132]['operands']==ins[0x46284E]['operands']=='edi, 0xc8')
    # Offline operand model: only these qualified c/s/h/b paths, valid input
    # pointers represented as bytes; not Native/provider or encoder execution.
    def format_bytes(fmt,values,limit=0x13FE):
        out=b'';cursor=0
        for c in fmt:
            value=values[cursor];cursor+=1
            if c=='c':piece=bytes([value&255]);reject=len(out)>=limit
            elif c=='h':piece=(value&65535).to_bytes(2,'little');reject=len(out)+1>=limit
            elif c=='s':end=value.index(0);piece=value[:end+1];reject=len(out)+end>=limit
            elif c=='b':
                count=value;buffer=values[cursor];cursor+=1
                assert 0<=count<=len(buffer)
                piece=buffer[:count];reject=len(out)+count>=limit
            else:raise ValueError(c)
            if reject:return None
            out+=piece
        assert cursor==len(values)
        return out
    cases=[]
    def model(name,fmt,values,expected):
        raw=format_bytes(fmt,values);check('serializer model '+name,raw==expected)
        cases.append({'name':name,'format':fmt,'body_hex':raw.hex(),'body_bytes':len(raw),
                      'frame_total_u16LE_hex':(len(raw)+2).to_bytes(2,'little').hex(),
                      'frame_bytes':len(raw)+2,'actual_trace':False,'Native_executed':False,
                      'qualification':'Computedrawoperandmodel only; noNativecodec/UI/encoder/send/serveracceptance.'})
    compact_body=bytes.fromhex('dd456469744166746572526f6f6d0028000400')
    model('compact_ASCII_savedshape','cscsh',[221,b'EditAfterRoom\0',40,b'\0',4],compact_body)
    model('legacy_five_zero_tail','cscshccccc',[221,b'EditAfterRoom\0',40,b'\0',4,0,0,0,0,0],compact_body+b'\0'*5)
    model('extended_two_selections','cscshcccccb',[221,b'Cfg\0',40,b'\0',4,3,2,5,7,2,2,b'\x01\x07'],bytes.fromhex('dd436667002800040003020507020107'))
    model('opaque_source_bytes_and_widths','cscsh',[221,b'\x81\xff\0',-128,b'\0',0x123456],bytes.fromhex('dd81ff0080005634'))
    check('formatter c limit exactly1',format_bytes('c',[255],1)==b'\xff')
    check('formatter h limit exactly2',format_bytes('h',[0x0204],2)==b'\x04\x02')
    check('formatter s includesNUL',format_bytes('s',[b'\0'],1)==b'\0')
    check('formatter b strictlimit fails at1',format_bytes('b',[1,b'x'],1)is None)
    check('formatter b strictlimit passes at2',format_bytes('b',[1,b'x'],2)==b'x')
    # Eight-slot compaction models verify nonzero order/count, not its unknown
    # UI/converter producers. No previous PASS matrix is rerun.
    compaction=[]
    for inputs,expected in [([0]*8,[]),([1,0,7,0,0,0,0,0],[1,7]),([255,128,1,2,3,4,5,6],[255,128,1,2,3,4,5,6])]:
        result=[x for x in inputs if x!=0]
        check('selection compaction '+str(inputs),result==expected and len(result)<=8)
        compaction.append({'inputs8':inputs,'ordered_nonzero':result,'K':len(result),'Native_executed':False})
    saved=load('CDD-outbound/saved-compact-field-witnesses.json');frames=json.loads(Path(saved['frames_source']['path']).read_text('utf-8'))
    for f in saved['frames']:
        r=frames[f['array_index_zero_based']];body=bytes.fromhex(r['body_hex'])
        rebuilt=format_bytes('cscsh',[221,bytes.fromhex(f['name_hex'])+b'\0',f['attributes'],bytes.fromhex(f['password_hex'])+b'\0',f['capacity_u16LE']])
        check('savedcompact conservation '+str(f['array_index_zero_based']),body==rebuilt==bytes.fromhex(f['body_hex']) and r['direction']=='C' and len(body)==6+len(bytes.fromhex(f['name_hex']))+len(bytes.fromhex(f['password_hex'])) and f['remaining']==0 and r['frame_bytes']==len(body)+2)
    check('frameheader high/low stores proven',ins[0x4C6B19]['operands']=='byte ptr [ebp - 0x13ff], dl' and ins[0x4C6B21]['operands']=='byte ptr [ebp - 0x1400], al')
    check('no complete UI/phase/owner/codec qualification',p['scope']['complete_UI_submit_to_send_or_owner_phase_codec_delivery_ACK_qualified']is False)
    check('newwire/opcode/correction empty',p['delta']['new_wire']==p['delta']['new_opcodes']==p['delta']['wire_corrections']==[])
    check('packet unchanged',record(ROOT/'server-packet-delta.json')==p['verification']['server_packet_unchanged'])
    write('CDD-outbound/computed-serializer-models.json',{'qualification':'offlineconditional/rawbytes only','serializer_examples':cases,'selection_compaction_examples':compaction})
    result={'primary':record(ROOT/'CDD-outbound-contract.json'),'all_match':True,'check_count':len(checks),'checks':checks,
            'new_code_ranges':22,'new_data_ranges':5,'necessary_reused_raw_ranges':4,'instruction_assertions':51,
            'conditional_CDD_callsites':3,'reconstructed_callsite_varargs':3,'serializer_models':4,'selection_compaction_models':3,
            'saved_compact_field_witnesses':2,'Native_or_extended_dynamic_witnesses':0,'prior_primary_pins_preserved':27,
            'Native_SQL_provider_GUI_Java_or_old_PASS':False,'UI_submit_phase_owner_codec_delivery_ACK_qualified':False}
    write('CDD-outbound/independent-review.json',result)
    print(json.dumps({k:v for k,v in result.items()if k!='checks'},ensure_ascii=False))

if __name__=='__main__':main()

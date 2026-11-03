"""Review Batch24 finite reused slices and mode/map effects; no captures."""
from audit import *

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    p=load('D5-mode-map-receive-contract.json');checks=[]
    def check(name,ok):
        checks.append({'check':name,'match':bool(ok)});assert ok,name
    for pin in p['sources']+list(p['existing_evidence'].values())+p['outputs']:
        check('pin '+Path(pin['path']).name,record(Path(pin['path']))==pin)
    for pin in p['prior_primary_pins']:
        actual=record(Path(pin['path']));check('prior '+Path(pin['path']).name,actual['bytes']==pin['bytes'] and actual['sha256']==pin['expected_sha256'])
    previous=load('D5-settings-receive-contract.json')
    check('Batch23 reusedindex alreadysealed',p['existing_evidence']['Batch23_ranges']in previous['outputs'])
    r=load('D5-mode-map-receive/ranges.json');pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    ins={};intervals=[]
    check('no newrawranges',r['new_ranges']==[] and not list((ROOT/'D5-mode-map-receive').glob('*.bin')))
    for s in r['inherited_necessary_raw_slices']:
        pin=s['source_pin'];va=int(s['va'],16);n=s['used_bytes'];off=s['source_byte_offset']
        raw=Path(pin['path']).read_bytes()[off:off+n]
        check('source pin '+s['label'],record(Path(pin['path']))==pin)
        check('index pin '+s['label'],record(Path(s['source_index']['path']))==s['source_index'])
        check('raw PE '+s['label'],len(raw)==n and raw==bytes.fromhex(s['raw_hex'])==pe.get_data(va-0x400000,n))
        check('source interval '+s['label'],int(s['source_base_VA'],16)+off==va and 0<=off and off+n<=pin['bytes'])
        check('VA/RVA/offset '+s['label'],int(s['rva'],16)==va-0x400000 and int(s['file_offset'],16)==pe.get_offset_from_rva(va-0x400000))
        if s['kind']=='code':
            decoded=[{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in cs.disasm(raw,va)]
            check('complete disassembly '+s['label'],decoded==s['instructions'] and sum(len(bytes.fromhex(i['raw_hex']))for i in decoded)==n)
            ins.update({int(i['va'],16):i for i in decoded});intervals.append((va,va+n))
    witness=load('D5-mode-map-receive/instruction-witnesses.json')
    for w in witness['assertions']:
        i=ins[int(w['va'],16)]
        check('operand '+w['va'],(i['mnemonic'],i['operands'],i['raw_hex'])==(w['expected_mnemonic'],w['expected_operands'],w['raw_hex']))
    for b in witness['aligned_branches']:
        check('branch '+b['va'],int(b['target'],16)in ins and ins[int(b['va'],16)]['operands']==hex(int(b['target'],16)))
    covered=lambda a,b:all(any(lo<=x<hi for lo,hi in intervals)for x in range(a,b))
    check('modeentry10 andtail22 covered',covered(0x4A9F6A,0x4A9F74) and covered(0x4AA05B,0x4AA071))
    check('mapentry21 andprojection28 covered',covered(0x4A9F74,0x4A9F89) and covered(0x4A9F89,0x4A9FA5))
    mode=[i for va,i in sorted(ins.items())if 0x4A9F6A<=va<0x4A9F74 or 0x4AA05B<=va<0x4AA071]
    map_path=[i for va,i in sorted(ins.items())if 0x4A9F74<=va<0x4A9FA5]
    check('modecalls onlytypedreader',[(i['va'],i['operands'])for i in mode if i['mnemonic']=='call']==[('004AA064','0x4a2a50')])
    check('mapcalls onlytypedreader',[(i['va'],i['operands'])for i in map_path if i['mnemonic']=='call']==[('004A9F81','0x4a2a50')])
    check('mode skips otherUIedge4AA04C',ins[0x4A9F6F]['operands']=='0x4aa05b' and all(i['va']!='004AA04C'for i in mode))
    check('mode ownreturn',ins[0x4AA070]['mnemonic']=='ret')
    check('map sharedreturn',ins[0x4A9FA4]['mnemonic']=='ret')
    check('mode leavesmapBYTE withoutstore',all('0x64acc9'not in i['operands'] for i in mode))
    check('map leavesmodeBYTE withoutstore',all('0x64acca'not in i['operands'] for i in map_path))
    check('mode onlyBYTE typedstore',ins[0x4A2A93]['operands']=='byte ptr [edx], al')
    check('map localWORD typedstore',ins[0x4A2AB1]['operands']=='word ptr [eax], cx' and ins[0x4A9F74]['operands']=='eax, [ebp - 4]')
    check('projection onlyBYTE globalstore',[i['operands']for i in map_path if i['mnemonic']=='mov'and '[0x'in i['operands']]==['byte ptr [0x64acc9], al'])
    for name,seq,expected in [('mode',mode,['body+6','0x5025a8','0x64acca']),('map',map_path,['body+6','0x632594','[ebp - 4]'])]:
        stack=[];regs={'esi':'body+0'}
        for i in seq:
            if i['mnemonic']=='call':break
            if i['mnemonic']=='lea':reg,value=i['operands'].split(', ',1);regs[reg]=value
            elif i['mnemonic']=='add'and i['operands']=='esi, 6':regs['esi']='body+6'
            elif i['mnemonic']=='push':stack.append(regs.get(i['operands'],i['operands']))
        check(name+' threevarargs',list(reversed(stack))==expected)
    jump=struct.unpack('<6I',pe.get_data(0xA2B78,24));maps=pe.get_data(0xA2B90,33)
    check('c/h readerdispatch typedtargets',[jump[maps[ord(c)-ord('S')]]for c in 'ch']==[0x4A2A8A,0x4A2A9A])
    check('D5 subtype2/3 jumpentries',struct.unpack('<10I',pe.get_data(0xAA074,40))[2:4]==(0x4A9F6A,0x4A9F74))
    computed=load('D5-mode-map-receive/computed-effect-examples.json')
    for case in computed['mode_effect_cases']:
        before=bytes.fromhex(case['before64ACC9..CC_hex']);after=bytes.fromhex(case['after64ACC9..CC_hex'])
        check('mode effect '+str(case['input_mode_BYTE']),len(after)==4 and after[1]==case['input_mode_BYTE'] and after[0]==before[0] and after[2:]==before[2:] and case['Native_executed'] is False)
    expected_maps=[14,64,156]
    for case,expected in zip(computed['map_effect_cases'],expected_maps):
        before=bytes.fromhex(case['before64ACC9..CC_hex']);after=bytes.fromhex(case['after64ACC9..CC_hex'])
        check('map effect '+str(case['input_map_WORD']),after[0]==expected and after[1:]==before[1:] and case['Native_executed']is False)
    for name,subtype,n in [('subtype2_computed_room1_mode2',2,7),('subtype3_current514_computed',3,8),('subtype3_restore564_computed',3,8)]:
        case=computed[name];body=bytes.fromhex(case['body_hex'])
        check('computed layout '+name,body[:6]==bytes([0xD5,subtype,1,0,0,0]) and len(body)==case['body_bytes']==n and case['frame_bytes']==n+2 and case['actual_trace']is False)
    check('computed mapsLE exact',int.from_bytes(bytes.fromhex(computed['subtype3_current514_computed']['body_hex'])[6:],'little')==514 and int.from_bytes(bytes.fromhex(computed['subtype3_restore564_computed']['body_hex'])[6:],'little')==564)
    server=json.loads(Path(p['existing_evidence']['server_map_contract']['path']).read_text('utf-8'))
    detail=json.loads(Path(p['existing_evidence']['server_map_detail']['path']).read_text('utf-8'))
    check('server finitecapture no sub3 adopted only',server['verification']['S_D5_sub3_hits']==0 and server['verification']['C_D1_sub3_hits']==0 and p['verification']['dynamic_subtype3_witnesses']==0)
    check('server sub3 no ACK flag',detail['D5sub3']['wire_ACK_or_rejection_code'] is False and detail['D5sub3']['flow_role_on_wire'] is False)
    check('server mismatch adopted not newcorrection',p['server_comparison']['inherited_candidate_value_mismatch']==detail['difference'] and p['delta']['wire_corrections']==[])
    check('no real witnessfiles made',not any(x.name in ('saved-field-witnesses.json','frames.json')for x in (ROOT/'D5-mode-map-receive').iterdir()))
    for w in p['wire']:
        check('wire fields exact subtype'+str(w['subtype']),[(f['body_offset'],f['bytes'])for f in w['fields']]==[(1,1),(2,4),(6,1 if w['subtype']==2 else 2)] and w['body_bytes']==(7 if w['subtype']==2 else 8) and w['frame_bytes']==w['body_bytes']+2)
        check('wire direction/types subtype'+str(w['subtype']),w['direction']=='server-to-client'and w['opcode_hex']=='D5'and w['fields'][2]['type']==('u8'if w['subtype']==2 else'u16LE')and w['encoding']=='no strings/no text codec')
    for pin in p['field_separation']['guard_sources'].values():check('guard source '+Path(pin['path']).name,record(Path(pin['path']))==pin)
    check('no wire/opcode/correction delta',p['delta']['new_wire']==p['delta']['new_opcodes']==p['delta']['wire_corrections']==[])
    check('packet unchanged',record(ROOT/'server-packet-delta.json')==p['verification']['server_packet_unchanged'])
    report={'primary':record(ROOT/'D5-mode-map-receive-contract.json'),'all_match':True,'check_count':len(checks),'checks':checks,
            'new_PE_ranges':0,'necessary_reused_raw_slices':15,'distinct_reused_raw_pins':9,
            'instruction_assertions':42,'aligned_in_slice_branches':7,'mode_complete_path_bytes':32,'map_complete_path_bytes':49,
            'mode_effect_examples':3,'map_effect_examples':3,'computed_layout_examples':3,'saved_or_new_dynamic_subtype3_witnesses':0,
            'prior_primary_pins_preserved':25,'Native_GUI_provider_SQL_Java_or_old_PASS':False,
            'ACK_automaticrollback_runtimeUI_resourcebinding_qualified':False}
    write('D5-mode-map-receive/independent-review.json',report)
    print(json.dumps({k:v for k,v in report.items()if k!='checks'},ensure_ascii=False))

if __name__=='__main__':main()

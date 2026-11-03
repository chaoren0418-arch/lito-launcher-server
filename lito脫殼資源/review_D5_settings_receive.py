"""Independent Batch23 raw/CFG/vararg/field conservation review; no old PASS."""
from audit import *

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    primary=load('D5-settings-receive-contract.json');pin=record(ROOT/'D5-settings-receive-contract.json')
    checks=[]
    def check(name,ok):
        checks.append({'check':name,'match':bool(ok)});assert ok,name
    for p in primary['sources']+list(primary['existing_evidence'].values())+primary['outputs']:
        check('pin '+Path(p['path']).name,record(Path(p['path']))==p)
    for p in primary['prior_primary_pins']:
        q=record(Path(p['path']));check('prior '+Path(p['path']).name,q['sha256']==p['expected_sha256'] and q['bytes']==p['bytes'])
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    ranges=load('D5-settings-receive/ranges.json');instructions={};byte_intervals=[]
    for r in ranges['new_ranges']+ranges['inherited_necessary_raw']:
        q=r.get('output',r.get('source_pin'));va=int(r['va'],16);n=r.get('bytes',r.get('used_bytes'))
        raw=Path(q['path']).read_bytes()[:n]
        check('raw PE '+r['va'],record(Path(q['path']))==q and raw==bytes.fromhex(r['raw_hex'])==pe.get_data(va-0x400000,n))
        check('VA RVA offset '+r['va'],int(r['rva'],16)==va-0x400000 and int(r['file_offset'],16)==pe.get_offset_from_rva(va-0x400000))
        if r['kind']=='code':
            decoded=[{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in cs.disasm(raw,va)]
            check('decode alignment '+r['va'],decoded==r['instructions'] and sum(len(bytes.fromhex(i['raw_hex']))for i in decoded)==n)
            instructions.update({int(i['va'],16):i for i in decoded})
            byte_intervals.append((va,va+n))
    witness=load('D5-settings-receive/instruction-witnesses.json')
    for w in witness['assertions']:
        i=instructions[int(w['va'],16)]
        check('operand '+w['va'],(i['mnemonic'],i['operands'],i['raw_hex'])==(w['expected_mnemonic'],w['expected_operands'],w['raw_hex']))
    for b in witness['aligned_branches']:
        check('branch target '+b['va'],int(b['target'],16)in instructions)
    covered=lambda lo,hi:all(any(a<=x<b for a,b in byte_intervals)for x in range(lo,hi))
    check('subtype0 contiguous75-byte primary path',covered(0x4A9EAB,0x4A9EF6))
    check('subtype0 complete28-byte projection tail',covered(0x4A9F89,0x4A9FA5))
    check('subtype1 complete116-byte own return',covered(0x4A9EF6,0x4A9F6A) and instructions[0x4A9F69]['mnemonic']=='ret')
    check('stub onlyRET',pe.get_data(0xA9030,1)==b'\xc3')
    check('subtype0 calltargets onlyreader andRET',[(i['va'],i['operands'])for va,i in sorted(instructions.items())if 0x4A9EAB<=va<0x4A9EF6 and i['mnemonic']=='call']==[('004A9EE4','0x4a2a50'),('004A9EEC','0x4a9030')])
    check('subtype1 calls onlyreader',[(i['va'],i['operands'])for va,i in sorted(instructions.items())if 0x4A9EF6<=va<0x4A9F6A and i['mnemonic']=='call']==[('004A9F2F','0x4a2a50'),('004A9F4E','0x4a2a50')])
    # Independently reconstruct arguments from actual pushes/LEAs, rather than
    # trusting semantic destination lists or generator's field ordering.
    def arguments(lo,call):
        stack=[];regs={'esi':'body+0'}
        for va,i in sorted(instructions.items()):
            if not lo<=va<call:continue
            op=i['operands']
            if i['mnemonic']=='lea':
                reg,value=op.split(', ',1);regs[reg]=value
            elif i['mnemonic']=='add' and op=='esi, 6':regs['esi']='body+6'
            elif i['mnemonic']=='push':stack.append(regs.get(op,op))
        return list(reversed(stack))
    a0=arguments(0x4A9EAB,0x4A9EE4)
    a1=arguments(0x4A9EF6,0x4A9F2F)
    check('subtype0 full13 varargs',a0==['body+6','0x634470','0x5a','0x64ab70','0x64acca','[ebp - 4]','0x64accc','0x1c','0x64abca','0x64acd0','0x64acd1','0x64acd2','0x64acd3'])
    check('subtype1 full13 varargs',a1==['body+6','0x634464','0x5a','0x64ab70','5','0x64abe8','0x64abe6','0x64abf0','0x64acd4','0x64acd5','0x64acd6','0x64acd7','[ebp - 2]'])
    jump=struct.unpack('<6I',pe.get_data(0xA2B78,24));maps=pe.get_data(0xA2B90,33)
    check('typed store format dispatch c/d/h/s',[jump[maps[ord(c)-ord('S')]]for c in 'cdhs']==[0x4A2A8A,0x4A2AB9,0x4A2A9A,0x4A2AE7])
    check('string firstexternal STOP preserved',instructions[0x4A2AF9]['operands']=='0x77409b70' and instructions[0x4A2B09]['operands']=='0x774095e0')
    check('mode/attrs/capacity cstore byte notDWORD',instructions[0x4A2A93]['operands']=='byte ptr [edx], al')
    check('map parsed into localWORD',instructions[0x4A2AB1]['operands']=='word ptr [eax], cx' and a0[5]=='[ebp - 4]')
    effects=primary['effects'];f0=effects['subtype0']['fields'];f1=effects['subtype1']['fields']
    check('subtype0 stringcaps90/28',[f['capacity_including_NUL']for f in f0 if 'capacity_including_NUL'in f]==[90,28])
    check('subtype1 stringcaps90/5',[f['capacity_including_NUL']for f in f1 if 'capacity_including_NUL'in f]==[90,5])
    check('mode highbyte explicitly retained','64ACCB adjacent mode highBYTE'in effects['subtype0']['retained_known_fields'])
    models=load('D5-settings-receive/operand-models.json')
    check('seven map projections exact',[x['output_BYTE']for x in models['map_models']]==[12,64,255,156,10,12,4])
    expected_counts=[(0,[0]*8),(1,[1]+[0]*7),(7,[1,2,3,4,5,6,7,0]),(8,list(range(1,9))),
                     (8,list(range(1,9))),(8,list(range(1,9))),(0,[0]*8),(0,[0]*8)]
    check('eight signedK models exact',[(x['reads'],x['stores8'])for x in models['count_models']]==expected_counts)
    expected_gate=[(2,True),(2,True),(2,False),(2,False),(1,False),(1,True),(1,True),(1,False)]
    check('eight phase/room/type models exact',[(x['room_after_gate'],x['payload_dispatch'])for x in models['phase_room_subtype_models']]==expected_gate)
    ns=models['numeric_store_models']
    check('finite numeric footprints disjoint',set(ns['subtype0_byte_destinations']).isdisjoint(ns['subtype1_byte_destinations']))
    check('preserve config numeric and highbytes',ns['subtype0_preserves_config_numeric_fields'] and ns['high_byte64ACCB_preserved'] and ns['capacity_highbytes_preserved']==[0xA5]*3)
    saved=load('D5-settings-receive/saved-field-witnesses.json')
    frames=json.loads(Path(saved['frames_source']['path']).read_text(encoding='utf-8'))
    for capture in saved['frames']:
        row=frames[capture['array_index_zero_based']];body=bytes.fromhex(row['body_hex'])
        check('saved field witness '+str(capture['array_index_zero_based']),row['direction']=='S' and row['body_hex']==capture['body_hex'] and row['frame_bytes']==capture['frame_bytes'] and len(body)==capture['body_bytes'])
        reconstructed=bytes([0xD5]);end=1
        for f in sorted(capture['fields'],key=lambda f:f['body_offset']):
            raw=bytes.fromhex(f['raw_hex']);assert f['body_offset']==end and f['byte_length']==len(raw)
            assert raw==body[end:end+len(raw)];end+=len(raw);reconstructed+=raw
        check('full saved field conservation '+str(capture['array_index_zero_based']),reconstructed==body and end==len(body) and capture['remaining']==0)
        strings=[f for f in capture['fields']if f['type']=='NUL8bit-string']
        n,p_or_o=[len(bytes.fromhex(f['raw_hex']))-1 for f in strings]
        expected_size=16+n+p_or_o if capture['subtype']==0 else 15+n+p_or_o+next(f['value']for f in capture['fields']if f['semantic']=='selection_count K')
        check('wire length formula '+str(capture['array_index_zero_based']),len(body)==expected_size and row['frame_bytes']==expected_size+2)
    check('success/reject subtype0 indistinguishable body',saved['frames'][0]['body_hex']==saved['frames'][2]['body_hex'])
    check('success/reject subtype1 indistinguishable body',saved['frames'][1]['body_hex']==saved['frames'][3]['body_hex'])
    fields=saved['frames'][0]['fields']
    check('saved map564 projected64',next(f['value']for f in fields if f['semantic']=='map_word')==564 and ns['subtype0_byte_destinations']['0064ACC9']==64)
    fields=saved['frames'][1]['fields']
    check('saved attrs40 capacity4 K0',[(f['semantic'],f['value'])for f in fields if f['semantic']in ('attributes','capacity','selection_count K')]==[('attributes',40),('capacity',4),('selection_count K',0)])
    check('no newwire/opcodes/corrections',primary['delta']['new_wire']==primary['delta']['new_opcodes']==primary['delta']['wire_corrections']==[])
    check('packet immutable',record(ROOT/'server-packet-delta.json')==primary['verification']['server_packet_unchanged'])
    report={'primary':pin,'all_match':True,'check_count':len(checks),'checks':checks,
            'new_code_ranges':2,'new_data_ranges':0,'inherited_necessary_raw_ranges':13,
            'instruction_assertions':len(witness['assertions']),'aligned_in_slice_branches':len(witness['aligned_branches']),
            'saved_field_witness_frames':4,'prior_primary_pins_preserved':24,
            'complete_subtype0_and_subtype1_finite_control':True,
            'map_models':7,'count_models':8,'gate_models':8,
            'Native_GUI_provider_SQL_Java_or_old_PASS':False,'ACK_atomicpair_runtimeGUI_qualified':False}
    write('D5-settings-receive/independent-review.json',report)
    print(json.dumps({k:v for k,v in report.items()if k!='checks'},ensure_ascii=False))

if __name__=='__main__':main()

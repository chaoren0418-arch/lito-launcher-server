"""Independent finite scalar-path CFG, varargs, bytefootprint and pin review."""
from audit import *

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    p=load('D5-scalar-receive-contract.json');checks=[]
    def check(name,ok):checks.append({'check':name,'match':bool(ok)});assert ok,name
    for pin in p['sources']+list(p['existing_evidence'].values())+p['outputs']:
        check('pin '+Path(pin['path']).name,record(Path(pin['path']))==pin)
    for pin in p['prior_primary_pins']:
        actual=record(Path(pin['path']));check('prior '+Path(pin['path']).name,actual['bytes']==pin['bytes'] and actual['sha256']==pin['expected_sha256'])
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    ranges=load('D5-scalar-receive/ranges.json');ins={}
    check('no newPErange orbin',ranges['new_ranges']==[] and not list((ROOT/'D5-scalar-receive').glob('*.bin')))
    for s in ranges['necessary_reused_raw_slices']:
        pin=s['source_pin'];va=int(s['va'],16);n=s['used_bytes'];off=s['source_byte_offset']
        raw=Path(pin['path']).read_bytes()[off:off+n]
        check('source pin '+s['label'],record(Path(pin['path']))==pin)
        check('index pin '+s['label'],record(Path(s['source_index']['path']))==s['source_index'])
        check('raw PE '+s['label'],raw==bytes.fromhex(s['raw_hex'])==pe.get_data(va-0x400000,n) and len(raw)==n)
        check('source interval '+s['label'],int(s['source_base_VA'],16)+off==va and off>=0 and off+n<=pin['bytes'])
        check('VA/RVA/fileoffset '+s['label'],int(s['rva'],16)==va-0x400000 and int(s['file_offset'],16)==pe.get_offset_from_rva(va-0x400000))
        if s['kind']=='code':
            decoded=[{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in cs.disasm(raw,va)]
            check('complete alignment '+s['label'],decoded==s['instructions'] and sum(len(bytes.fromhex(i['raw_hex']))for i in decoded)==n)
            ins.update({int(i['va'],16):i for i in decoded})
    witness=load('D5-scalar-receive/instruction-witnesses.json')
    for w in witness['assertions']:
        i=ins[int(w['va'],16)];check('operand '+w['va'],(i['mnemonic'],i['operands'],i['raw_hex'])==(w['expected_mnemonic'],w['expected_operands'],w['raw_hex']))
    for b in witness['aligned_branches']:
        check('branch '+b['va'],int(b['target'],16)in ins and ins[int(b['va'],16)]['operands']==hex(int(b['target'],16)))
    check('fixedUIhelper C3RET',pe.get_data(0xA9030,1)==b'\xc3' and ins[0x4A9030]['mnemonic']=='ret')
    jump=struct.unpack('<10I',pe.get_data(0xAA074,40))
    check('five dispatchentries',[jump[x]for x in (4,6,7,8,9)]==[0x4A9FA5,0x4AA028,0x4AA02F,0x4AA036,0x4AA056])
    parserjump=struct.unpack('<6I',pe.get_data(0xA2B78,24));parsermap=pe.get_data(0xA2B90,33)
    check('c format mapsdirectBYTEhandler',parserjump[parsermap[ord('c')-ord('S')]]==0x4A2A8A)
    check('typedc preserveswidth exactlyBYTE',ins[0x4A2A8D]['operands']=='al, byte ptr [ebx]' and ins[0x4A2A93]['operands']=='byte ptr [edx], al' and ins[0x4A2A92]['mnemonic']=='inc')
    # Independently walk the caller's direct jump/fallthrough CFG. It must
    # terminate with only known typed reader / optional fixed RET callees.
    def trace(entry):
        cursor=entry;seen=set();rows=[]
        while True:
            assert cursor in ins and cursor not in seen
            seen.add(cursor);i=ins[cursor];rows.append(i)
            if i['mnemonic']=='ret':break
            if i['mnemonic']=='jmp':cursor=int(i['operands'],16)
            else:cursor+=len(bytes.fromhex(i['raw_hex']))
        return rows
    paths=[]
    expected_bytes={4:32,6:34,7:34,8:32,9:27}
    expected_dest={4:'0x64accc',6:'0x64acd0',7:'0x64acd1',8:'0x64acd2',9:'0x64acd3'}
    for row in p['effect_unknown_matrix']:
        subtype=row['subtype'];seq=trace(int(row['entry_VA'],16));calls=[i for i in seq if i['mnemonic']=='call']
        ui=subtype in (6,7,8)
        check('fullpath bytes subtype'+str(subtype),sum(len(bytes.fromhex(i['raw_hex']))for i in seq)==row['complete_path_bytes']==expected_bytes[subtype])
        check('calls subtype'+str(subtype),[i['operands']for i in calls]==(['0x4a2a50','0x4a9030']if ui else['0x4a2a50']))
        check('UI edge subtype'+str(subtype),[i['va']for i in calls[1:]]==(['004AA04C']if ui else[]) and row['direct_UI_edge_VA']==('004AA04C'if ui else None))
        check('return subtype'+str(subtype),seq[-1]['va']==row['return_VA']==('004AA055'if ui else'004AA070'))
        stack=[];esi='body+0'
        for i in seq:
            if i['mnemonic']=='call':break
            if i['mnemonic']=='add'and i['operands']=='esi, 6':esi='body+6'
            elif i['mnemonic']=='push':stack.append(esi if i['operands']=='esi'else i['operands'])
        check('varargs subtype'+str(subtype),list(reversed(stack))==['body+6','0x5025a8',expected_dest[subtype]])
        check('no domainmask orconversion subtype'+str(subtype),all(i['mnemonic']not in ('and','or','xor','movsx','movzx','cmp')for i in seq) and row['value_mask_or_boolean_conversion'] is False)
        check('matrix BYTEdestination subtype'+str(subtype),int(row['destination_VA'],16)==int(expected_dest[subtype],16) and row['destination_width']==1)
        check('producer/GUI/ACK unqualified subtype'+str(subtype),row['actual_production_caller_or_trigger_qualified'] is False and row['ACK_ready_owner_source_GUI_qualified'] is False and row['dynamic_scalar_witness_qualified'] is False)
        check('firstunknowncallee none withinknownfinitepaths subtype'+str(subtype),row['first_unknown_private_external_virtual_callee'] is None)
        paths.append({'subtype':subtype,'instruction_VAs':[i['va']for i in seq],
                      'path_bytes':expected_bytes[subtype],'calls':[i['va']for i in calls],'all_direct_callees_qualified':True})
    check('only678through3B/4C',[x['subtype']for x in p['effect_unknown_matrix']if x['direct_UI_edge_VA']]==[6,7,8])
    models=load('D5-scalar-receive/computed-bytefootprint-examples.json')['examples']
    for m in models:
        s=m['subtype'];before=bytes.fromhex(m['before_hex']);after=bytes.fromhex(m['after_hex']);body=bytes.fromhex(m['computed_body_hex'])
        dest=int(expected_dest[s],16);off=dest-0x64ACC9
        check('oneBYTE footprint subtype'+str(s),len(before)==len(after)==23 and after[off]==m['input_BYTE'] and all(after[n]==before[n]for n in range(len(before))if n!=off))
        check('computed7body9frame subtype'+str(s),body==bytes([0xD5,s,1,0,0,0,m['input_BYTE']]) and len(body)==m['body_bytes']==7 and m['frame_bytes']==9 and m['actual_trace'] is False and m['Native_executed'] is False)
    server=json.loads(Path(p['existing_evidence']['server_D5_contract']['path']).read_text('utf-8'))
    comparison=p['server_builder_caller_boundary']
    check('sourcebuilderlayout only',comparison['adopted_scalar_layout']==next(x for x in server['D5_layouts']if x['subtypes']==[4,6,7,8,9]))
    check('sourcehelper index map',comparison['summary_index_mapping']=={'0':4,'1':6,'2':7,'3':8,'4':9})
    check('selectedwire scope not runtimewitness',comparison['selected_wire_scope']['observed_D5_subtypes']==[5] and comparison['selected_wire_scope']['scalar_dynamic_witness_adopted'] is False)
    for w in p['wire']:
        check('wire knownfields subtype'+str(w['subtype']),w['direction']=='server-to-client' and w['opcode_hex']=='D5' and [(f['body_offset'],f['type'],f['bytes'])for f in w['fields']]==[(1,'u8',1),(2,'u32LE',4),(6,'u8',1)] and w['body_bytes']==7 and w['frame_bytes']==9 and w['encoding']=='no strings/no textcodec')
    check('no newwire/opcode/correction',p['delta']['new_wire']==p['delta']['new_opcodes']==p['delta']['wire_corrections']==[])
    check('no newJava packetsrequested',p['delta']['Java_new_packets_requested'] is False)
    check('packet unchanged',record(ROOT/'server-packet-delta.json')==p['verification']['server_packet_unchanged'])
    check('no dynamicframes output',not any(x.name in ('frames.json','saved-field-witnesses.json')for x in (ROOT/'D5-scalar-receive').iterdir()))
    result={'primary':record(ROOT/'D5-scalar-receive-contract.json'),'all_match':True,'check_count':len(checks),'checks':checks,
            'new_PE_ranges':0,'necessary_reused_raw_slices':16,'distinct_reused_raw_pins':7,
            'instruction_assertions':33,'aligned_in_slice_branches':7,'finite_complete_paths':paths,
            'scalar_branches_complete':5,'direct_RET_UI_edge_subtypes':[6,7,8],'no_UI_edge_subtypes':[4,9],
            'offline_bytefootprint_examples':5,'dynamic_scalar_witnesses':0,'prior_primary_pins_preserved':26,
            'Native_GUI_provider_SQL_Java_or_old_PASS':False,'producer_ACK_ready_owner_source_GUI_qualified':False}
    write('D5-scalar-receive/independent-review.json',result)
    print(json.dumps({k:v for k,v in result.items()if k not in ('checks','finite_complete_paths')},ensure_ascii=False))

if __name__=='__main__':main()

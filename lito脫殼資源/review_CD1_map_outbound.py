"""Independent raw review of new map callsites and transforms; reuse serializer contract."""
from audit import *
def load(n):return json.loads((ROOT/n).read_text('utf-8'))
def main():
    p=load('CD1-map-outbound-contract.json');r=load('CD1-map-outbound/ranges.json');checks=[]
    def ck(name,ok):
        assert ok,name
        checks.append({'check':name,'match':True})
    pin=record(ROOT/'CD1-map-outbound-contract.json')
    for q in p['sources']+list(p['existing_evidence'].values())+p['outputs']:
        ck('pin '+Path(q['path']).name,record(q['path'])==q)
    for q in p['prior_primary_pins']:
        a=record(q['path']);ck('prior '+Path(q['path']).name,a['bytes']==q['bytes'] and a['sha256']==q['expected_sha256'])
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);ins={}
    for q in r['new_ranges']+r['necessary_reused_raw']:
        va=int(q['va'],16);n=q.get('bytes',q.get('used_bytes'));raw=bytes.fromhex(q['raw_hex'])
        ck('raw PE '+q['va'],raw==pe.get_data(va-0x400000,n))
        ck('offset '+q['va'],q['rva']==q['file_offset']==f'{va-0x400000:08X}' and q['raw_backed'])
        if 'source_pin' in q:
            ck('reused pin '+q['va'],record(q['source_pin']['path'])==q['source_pin'])
            ck('reused index '+q['va'],record(q['index']['path'])==q['index'])
        if q.get('kind','code')=='code':
            decoded=[{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str} for i in cs.disasm(raw,va)]
            ck('complete aligned disasm '+q['va'],decoded==q['instructions'] and sum(len(bytes.fromhex(i['raw_hex'])) for i in decoded)==n)
            ins.update({int(i['va'],16):i for i in decoded})
    assertions=load('CD1-map-outbound/instruction-witnesses.json')
    for q in assertions:
        actual=ins[int(q['va'],16)]
        ck('operand '+q['va'],actual['raw_hex']==q['raw_hex'] and (actual['mnemonic'],actual['operands'])==(q['expected_mnemonic'],q['expected_operands']))
    table=struct.unpack('<14I',pe.get_data(0x64EE4,56))
    ck('case7 dispatch',table[7]==0x464DF1 and ins[0x464AE1]['operands']=='dword ptr [eax*4 + 0x464ee4]')
    ck('map format literal',pe.get_data(0x22C078,4)==b'cch\0')
    ck('mode format literal',pe.get_data(0x1028CC,4)==b'ccc\0')
    # Walk only the selected-reader instructions, using synthetic valid memory.
    # This new model qualifies request-source composition, not live selection or old PASS.
    def selected(flags):
        regs={'ecx':0x1000,'eax':0,'edx':0,'ebx':0,'esi':0};mem={0x102C:len(flags),0x1058:0x2000}
        for k,b in enumerate(flags):mem[0x2000+k*4]=0x3000+k*0x100;mem[0x300C+k*0x100]=b
        stack=[];pc=0x450B90;zf=False;less=False;trace=[]
        while True:
            i=ins[pc];trace.append(f'{pc:08X}');op=i['operands'];mn=i['mnemonic'];nxt=pc+len(bytes.fromhex(i['raw_hex']))
            if mn=='mov':
                dest,src=op.split(', ')
                if src.startswith(('dword ptr [','byte ptr [')):
                    addr=src[src.index('[')+1:-1];parts=addr.split(' + ');a=regs[parts[0]]+(int(parts[1],16) if len(parts)>1 else 0);value=mem[a]
                else:value=regs[src]
                if dest=='bl':regs['ebx']=(regs['ebx']&0xFFFFFF00)|(value&255)
                else:regs[dest]=value
            elif mn=='xor':a,b=op.split(', ');regs[a]^=regs[b]
            elif mn=='test':a,b=op.split(', ');v=(regs['ebx']&255) if a=='bl' else regs[a];zf=v==0;less=bool(v&0x80000000)
            elif mn=='cmp':a,b=op.split(', ');zf=regs[a]==regs[b];less=regs[a]<regs[b]
            elif mn=='inc':regs[op]=(regs[op]+1)&0xFFFFFFFF
            elif mn=='add':a,b=op.split(', ');regs[a]=(regs[a]+int(b,16))&0xFFFFFFFF
            elif mn=='or':a,b=op.split(', ');regs[a]|=int(b,16)
            elif mn=='push':stack.append(regs[op])
            elif mn=='pop':regs[op]=stack.pop()
            elif mn in ('jle','jl','jne'):
                take=(less or zf) if mn=='jle' else less if mn=='jl' else not zf
                if take:nxt=int(op,16)
            elif mn=='ret':return regs['eax'],trace
            else:raise AssertionError(i)
            pc=nxt
            assert len(trace)<300
    select_models=[]
    for flags,expected in [([],0xFFFFFFFF),([0,0],0xFFFFFFFF),([0,1,1],1),([128,0],0),([0,255],1)]:
        index,trace=selected(flags);ck('selected model '+str(flags),index==expected)
        select_models.append({'flags':flags,'selected_index_u32':index,'instruction_trace':trace,'computed_only':True,'Native_executed':False})
    # Execute map arithmetic and push islands from independently decoded instructions.
    def map_call(item):
        eax=item&0xFFFFFFFF;zf=False;pc=0x464E0C;stack=[];trace=[]
        while True:
            i=ins[pc];mn=i['mnemonic'];op=i['operands'];trace.append(f'{pc:08X}');nxt=pc+len(bytes.fromhex(i['raw_hex']))
            if mn=='cmp':zf=eax==(int(op.split(', ')[1],0)&0xFFFFFFFF)
            elif mn=='je':
                if zf:nxt=int(op,16)
            elif mn=='add':eax=(eax+int(op.split(', ')[1],0))&0xFFFFFFFF
            elif mn=='push':stack.append(eax if op=='eax' else int(op,0))
            elif mn=='call':
                assert op=='0x4c6ae0';return list(reversed(stack)),trace
            else:raise AssertionError(i)
            pc=nxt
    models=[]
    for item,mapword,hexbody in [(14,514,'d1030202'),(64,564,'d1033402'),(0xFFFFFFFF,65535,'d103ffff'),
      (0xFFFFFF9C,65436,'d1039cff'),(0xFFFFFFFE,498,'d103f201'),(0x0000FFFF,499,'d103f301'),(0x12345678,0x586C,'d1036c58')]:
        args,trace=map_call(item);ck('map varargs '+hex(item),args[:3]==[0x62C078,209,3] and args[3]&65535==mapword)
        body=bytes([args[1],args[2]])+(args[3]&65535).to_bytes(2,'little')
        ck('map composed wire '+hex(item),body.hex()==hexbody and len(body)==4)
        models.append({'item_key_u32':item,'stack_order_format_opcode_subtype_value':args,'map_u16LE':mapword,'body_hex':body.hex(),
          'body_bytes':4,'frame_bytes':6,'clear_frame_hex':'0600'+body.hex(),'trace':trace,'computed_only':True,'Native_executed':False,
          'serialization_basis':'Accepted Batch26 c/h contract; no serializer/cipher PASS replay.'})
    ck('selected source offsets',[(ins[a]['mnemonic'],ins[a]['operands']) for a in (0x464E03,0x464E06,0x464E09)]==[
      ('mov','ecx, dword ptr [esi + 0x58]'),('mov','edx, dword ptr [ecx + eax*4]'),('mov','eax, dword ptr [edx + 4]')])
    ck('two qualified selected reads',ins[0x464DF3]['operands']==ins[0x464DFE]['operands']=='0x450b90')
    ck('no unknown before map sender',[(a,i['operands']) for a,i in ins.items() if 0x464DF1<=a<0x464E30 and i['mnemonic']=='call']==[
      (0x464DF3,'0x450b90'),(0x464DFE,'0x450b90'),(0x464E28,'0x4c6ae0')])
    ck('map first postrequest virtual',ins[0x464E40]['raw_hex']=='ff10')
    mode=[]
    for inputval,entry,step in [(0,0x46B9E6,2),(1,0x46B9E2,1)]:
        pc=entry;stack=[];trace=[]
        while True:
            i=ins[pc];trace.append(f'{pc:08X}');nxt=pc+len(bytes.fromhex(i['raw_hex']))
            if i['mnemonic']=='push':stack.append(int(i['operands'],0))
            elif i['mnemonic']=='jmp':nxt=int(i['operands'],16)
            elif i['mnemonic']=='call':break
            else:raise AssertionError(i)
            pc=nxt
        args=list(reversed(stack));ck('mode args input'+str(inputval),pc==0x46B9F4 and args==[0x5028CC,209,2,step])
        mode.append({'input_ECX':inputval,'args':args,'body_hex':bytes([209,2,step]).hex(),'body_bytes':3,'frame_bytes':5,'trace':trace,'computed_only':True,'Native_executed':False})
    composition=[]
    for key,expected in [(514,514),(564,564),(499,65535),(400,65436),(65535,65535),(0x80000001,1)]:
        item=(key-500)&0xFFFFFFFF;args,trace=map_call(item)
        ck('conditional table composition '+hex(key),args[3]&65535==expected)
        composition.append({'table_key_u32':key,'conditional_item_key_u32':item,'map_u16LE':args[3]&65535,
          'computed_only':True,'allocation_ctor_insert_selection_not_bridged':True})
    ck('first allocation STOP',ins[0x46515D]['operands']=='0x4f118c' and ins[0x46B849]['operands']=='0x4f118c')
    ck('conditional ctor base store',ins[0x465177]['operands']=='edi' and ins[0x463114]['operands']=='0x450bc0' and ins[0x450BCE]['operands']=='dword ptr [eax + 4], ecx')
    ck('ctor first remaining unknown',ins[0x46313C]['operands']=='0x463b70')
    ck('menu first virtual STOP',ins[0x46B828]['raw_hex']=='ff10')
    server=load('../lito伺服器/server-ordinary-room-map-selection-contract.json')
    ck('known server layout',server['parser']['subtype']==3 and server['parser']['body_bytes']==4 and server['parser']['frame_bytes']==6)
    ck('no new dynamic witness',p['verification']['Native_outbound_witnesses']==p['verification']['new_capture_frames']==0)
    ck('packet unchanged',record(ROOT/'server-packet-delta.json')==p['verification']['server_packet_unchanged'])
    ck('separate projections explicitly open',p['server_comparison']['D5_BYTE_CC_row74_preview0F_candidateordinal_not_outbound_source'])
    ck('no newwire correction',p['delta']['new_wire']==p['delta']['new_opcodes']==p['delta']['wire_corrections']==[])
    ck('new static sizes',len(r['new_ranges'])==13 and len(r['necessary_reused_raw'])==6 and len(assertions)==47)
    output={'selected_lookup':select_models,'map_request':models,'conditional_table_composition':composition,'adjacent_mode':mode,
      'qualification':'Only new request-source arithmetic/varargs models; accepted serializer referenced, no GUI/runtime/capture/old PASS replay.'}
    write('CD1-map-outbound/computed-request-source-models.json',output)
    review={'primary':pin,'all_match':True,'check_count':len(checks),'checks':checks,
      'new_code_ranges':10,'new_data_ranges':3,'necessary_reused_raw_ranges':6,'instruction_assertions':47,
      'map_request_models':7,'selected_lookup_models':5,'conditional_table_composition_models':6,'adjacent_mode_models':2,
      'prior_primary_pins_preserved':28,'Native_or_dynamic_witnesses':0,'old_PASS_replayed':False,
      'computed_models':record(ROOT/'CD1-map-outbound/computed-request-source-models.json'),
      'UI_tableproducer_ctor_insert_selection_transport_phase_owner_ACK_qualified':False}
    write('CD1-map-outbound/independent-review.json',review)
    print(json.dumps({k:v for k,v in review.items() if k!='checks'},ensure_ascii=False))
if __name__=='__main__':main()

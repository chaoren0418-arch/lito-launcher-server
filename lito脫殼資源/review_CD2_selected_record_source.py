"""Independent finite field16/record3E source and copy-boundary verification."""
import sys
sys.dont_write_bytecode=True
from audit import *
def load(n):return json.loads((ROOT/n).read_text('utf-8'))
def main():
    p=load('CD2-selected-record-source-contract.json');r=load('CD2-selected-record-source/ranges.json');checks=[];models=[]
    def check(name,result):
        checks.append({'check':name,'match':bool(result)});assert result,name
    for q in p['sources']+list(p['existing_evidence'].values())+p['outputs']:
        a=record(q['path']);check('pin '+q['path'],all(a[k]==q[k]for k in ['path','bytes','sha256']))
    for q in p['prior_primary_pins']:
        a=record(q['path']);check('prior '+q['path'],a['bytes']==q['bytes']and a['sha256']==q['expected_sha256'])
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);ins={}
    for q in r['new_ranges']+r['necessary_reused_raw']:
        va=int(q['va'],16)
        if 'output'in q:
            raw=Path(q['output']['path']).read_bytes();check('raw file '+q['va'],record(q['output']['path'])==q['output'])
        else:
            check('reuse '+q['va'],record(q['source_pin']['path'])==q['source_pin']and record(q['source_index']['path'])==q['source_index'])
            raw=Path(q['source_pin']['path']).read_bytes()[q['source_offset']:q['source_offset']+q['bytes']];check('slice hash '+q['va'],sha(raw)==q['slice_sha256'])
        check('fixedPE '+q['va'],raw==pe.get_data(va-0x400000,q['bytes'])and len(raw)==q['bytes'])
        if q['kind']=='code':
            rows=[{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in cs.disasm(raw,va)]
            check('aligned '+q['va'],rows==q['instructions']and sum(len(bytes.fromhex(i['raw_hex']))for i in rows)==len(raw));ins.update({int(i['va'],16):i for i in rows})
    for q in r['instruction_assertions']:check('operand '+q['va'],(ins[int(q['va'],16)]['mnemonic'],ins[int(q['va'],16)]['operands'])==(q['expected_mnemonic'],q['expected_operands']))
    # Independently reconstruct stack words with symbolic record-base registers.
    regs={};words=[]
    for va,i in sorted(ins.items()):
        if not 0x4A759B<=va<0x4A7631:continue
        if i['mnemonic']=='lea':
            reg,op=i['operands'].split(', ')
            if op.startswith('[esi + '):regs[reg]=('record',int(op[7:-1],0))
        if i['mnemonic']=='push':
            op=i['operands'];words.append(regs[op]if op in regs else('record',0)if op=='esi'else('cursor','EDI')if op=='edi'else('literal',int(op,0)))
    args=list(reversed(words));expected=[('cursor','EDI'),('literal',0x6343B4),('literal',28),('record',0),('literal',17),('record',28)]+[('record',n)for n in [45,46,48,50,52,54,55,56,57,58,59,60,61,62]]
    check('record field16 stack',args[:len(expected)]==expected and len(args)==36)
    producer=p['producer_copy_clear'];field=producer['S_CB']['AD368_reference']['field']
    check('field16 is BYTE3E',producer['S_CB']['field_token_ordinal_after_opcode_including_strings']==16 and producer['S_CB']['numeric_field_ordinal']==14 and producer['S_CB']['row_after_opcode_arg_map_through_field16'][-1]['destination_record_offset']==62)
    check('c dispatch',pe.get_data(0x4A2BA0-0x400000,1)==b'\1'and struct.unpack('<I',pe.get_data(0x4A2B7C-0x400000,4))[0]==0x4A2A8A and ins[0x4A2A93]['operands']=='byte ptr [edx], al')
    fields=json.loads(Path(p['existing_evidence']['server_complete_fields']['path']).read_text('utf-8'));check('accepted reserved field',field==next(q for q in fields['fields']if q['name']=='reserved_byte')and field['suffix_offset']==16 and field['bytes']==1 and field['bounds']=='always0')
    protocol=json.loads(Path(p['existing_evidence']['server_Protocol_source_reference']['path']).read_text('utf-8'));method=next(q for q in protocol['methods']if q['method']=='litoCharacterDirectoryRow')
    check('exact producer excerpt',method==producer['S_CB']['AD368_reference']['Protocol_method']and '.c(0)'in method['text']and sha(method['text'].encode())==method['sha256'])
    comp=json.loads(Path(p['existing_evidence']['server_finite_version_comparison']['path']).read_text('utf-8'))
    check('adopted archivedIL equality only',any('litoCharacterDirectoryRow'in q['member']and q['AD368_equals_archived_ordinary_IL']for q in comp['AD368_vs_archived_ordinary']))
    for n,m in [(1,0),(27,16),(5,3)]:
        body=82+n+m;frame=body+2;offset=3+n+m+16
        check(f'known row length {n}/{m}',body==fields['fixed_suffix_bytes']+3+n+m and offset<body and frame==84+n+m)
        models.append({'kind':'known_CB_variable_string_layout','name_bytes':n,'clan_bytes':m,'field3E_body_offset':offset,'body_bytes':body,'frame_bytes':frame,'reserved_AD368_value':0,'qualification':'pure layout arithmetic; no newwire/capture/Nativeexecution'})
    # Model exact marker-loop write addresses; no actual local memory touched.
    eax=0x62FACE;writes=[]
    while True:
        writes.extend([eax-1,eax]);eax+=0xB4
        if eax>=0x62FD9E:break
    expectedwrites=[0x62FAA0+i*180+off for i in range(4)for off in[0x2D,0x2E]]
    check('four markerpair addresses',writes==expectedwrites)
    check('marker reset excludes3E',not set(writes)&{0x62FAA0+i*180+0x3E for i in range(4)})
    models.append({'kind':'four_slot_marker_reset','writes_VA':[f'{a:08X}'for a in writes],'values_hex':'FF','field3E_preserved_in_this_loop':True,'native_capacity_inferred':False,'qualification':'conditional exactloop only, notbulk/reset/runtime guarantee'})
    for count in [0,1,8,255]:
        signed=count if count<128 else count-256
        check('count projection '+str(count),signed==({0:0,1:1,8:8,255:-1}[count]))
        models.append({'kind':'countBYTE_projection','input_u8':count,'native_signed_DWORD':signed,'marker_reset_slots':4,'AD368_producer_allows':count<=8,'qualification':'nativeMOVSX source only; no validruntime/capacity conclusion'})
    for strlen in [0,16,33,34,35]:
        # Call-free matchingalias copy includes NUL and has no local cap.
        end=0x1C+strlen;touch=end>=0x3E;at3e=0 if strlen==34 else 65 if strlen>34 else 'preserved'
        check('clan copy reach '+str(strlen),touch==(strlen>=34))
        models.append({'kind':'alias_matched_clan_copy_span','strlen_bytes':strlen,'copy_includes_NUL':True,'destination_start_offset':28,'last_written_offset':end,'field3E_touched':touch,'example_field3E_after':at3e,'AD368_clan_bound_satisfied':strlen<=16,'qualification':'symbolicvalidmemory only;caller/epoch/inputvalidation unknown,noNative invocation'})
    local=[i for a,i in ins.items()if 0x497600<=a<0x4976AA];check('copybody no calls',not any(i['mnemonic']=='call'for i in local)and ins[0x4976A7]['operands']=='0x14')
    scalar=[0x497677,0x49767E,0x497685,0x497688,0x49768C];check('scalar spans stop37',all('[ebx'in ins[a]['operands']for a in scalar)and max(q['record_offset']+q['bytes']-1 for q in producer['direct_copy_update']['scalar_stores'])==0x37)
    unknown={0x4D6970,0x496E80,0x49A510,0x494120,0x494E00,0x77409B70,0x774095E0}
    for q in r['new_ranges']:check('unknowncallee notcaptured '+q['va'],int(q['va'],16)not in unknown)
    nav=load('CD2-selected-record-source/direct-table-xrefs.json');lines=ASM.read_text('utf-8',errors='replace').splitlines()
    for term,rows in nav['literal_queries'].items():
        check('exact navigation '+term,rows==[{'line':i+1,'text':s}for i,s in enumerate(lines)if term.lower()in s.lower()and not s.strip().startswith(';')])
    check('snapshot notruntime',pe.get_data(0x62FADE-0x400000,1)==b'\0'and producer['snapshot']['first_record_field3E_hex']=='00')
    check('dependency stages distinct',[q['stage']for q in p['Modern_ready_dependencies']['rows']]==['localrecord','localguard','userintent','servercommit','D1_E1_receipts','GUI'])
    check('Batch29 immutable',record(ROOT/'CD2-ready-outbound-contract.json')['sha256']=='F3867EC1B86DA3DF66A7493E92CD239E40AEC59B658EC0B588F463EFF13A2667')
    check('Batch30 immutable',record(ROOT/'CD2-ready-guard-source-contract.json')['sha256']=='FCD54F0D67C11EC6BE3E3BDFD03C579F117A205773B67F8A29A850BB99501AB2')
    incident=load('CD1-mode-step/scope-write-correction.json');check('cleanup rejection remainsopen',not incident['cleanup_completed']and incident['automatic_approval_review_rejected_cleanup'])
    check('packet unchanged',record(ROOT/'server-packet-delta.json')==p['verification']['server_packet_unchanged'])
    check('finite counts',(len(r['new_ranges']),len(r['necessary_reused_raw']),len(r['instruction_assertions']),len(models))==(12,11,55,13))
    write('CD2-selected-record-source/computed-layout-copy-and-marker-models.json',{'models':models,'count':len(models),'oldmodels_or_PASS_replayed':False,'Native_runtime_or_memory_written':False})
    review={'primary':record(ROOT/'CD2-selected-record-source-contract.json'),'checks':checks,'check_count':len(checks),'all_match':all(q['match']for q in checks),'decoded_instruction_rows':len(ins),'model_count':len(models),'models':record(ROOT/'CD2-selected-record-source/computed-layout-copy-and-marker-models.json'),'scope_write_correction':p['verification']['scope_write_correction']}
    write('CD2-selected-record-source/independent-review.json',review);print(json.dumps({'checks':len(checks),'decoded_instruction_rows':len(ins),'models':len(models),'all_match':True},ensure_ascii=False))
if __name__=='__main__':main()

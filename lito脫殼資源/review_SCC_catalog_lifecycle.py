"""Independent finite Batch20 readback; never invokes Native/helper code."""
from audit import *
def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))
def main():
    report=load('SCC-catalog-lifecycle-contract.json');bundle=load('SCC-catalog-lifecycle/ranges.json');assertions=load('SCC-catalog-lifecycle/instruction-witnesses.json')['assertions']
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);ins={};coverage=set()
    for r in bundle['ranges']:
        va=int(r['va'],16);raw=Path(r['output']['path']).read_bytes();assert record(Path(r['output']['path']))==r['output']and raw==pe.get_data(va-0x400000,r['bytes'])
        assert raw.hex()==r['raw_hex']and mapping(pe,va)['file_offset']==r['file_offset']
        if r['kind']=='code':
            items=list(cs.disasm(raw,va));assert sum(i.size for i in items)==len(raw)
            assert [{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in items]==r['instructions']
            ins.update({i.address:i for i in items});coverage.update(range(va,va+len(raw)))
    for r in bundle['inherited_raw']:
        q=r['range'];va=int(r['va'],16);raw=Path(q['path']).read_bytes();assert record(Path(q['path']))==q and raw==pe.get_data(va-0x400000,len(raw))
        assert record(Path(r['index']['path']))==r['index']
        if r['kind']=='code':
            items=list(cs.disasm(raw[:r['used_bytes']],va));assert sum(i.size for i in items)==r['used_bytes'];ins.update({i.address:i for i in items})
    for r in assertions:
        i=ins[int(r['va'],16)];assert (i.bytes.hex(),i.mnemonic,i.op_str)==(r['raw_hex'],r['expected_mnemonic'],r['expected_operands'])
    for lo,hi in [(0x465610,0x465680),(0x450090,0x450135),(0x465680,0x4656A1),(0x4656B0,0x4656FE),(0x465D10,0x465D22),(0x452C20,0x452C98)]:
        assert set(range(lo,hi))<=coverage
        last=next(i for at,i in ins.items()if at+i.size==hi);assert last.mnemonic in ['ret','jmp']
    # Independent reconstruction of caller stack arguments, using only this bounded initializer slice.
    pushed=[int(ins[at].op_str,0)for at in [0x46BCA0,0x46BCA5,0x46BCA7,0x46BCA9]]
    assert pushed==[0xFFFF,0,12,12]
    assert [(ins[at].mnemonic,ins[at].op_str)for at in [0x46BCAE,0x46BCB5,0x46BCBA,0x46BCBF]]==[
      ('mov','ecx, 0x21'),('mov','edx, 0x38'),('mov','esi, 0x17c'),('mov','edi, 0x150')]
    args=[0x21,0x38,0x17C,0x150]+list(reversed(pushed));assert args==[33,56,380,336,12,12,0,65535]
    assert ins[0x465649].op_str=='ecx, dword ptr [ebp + 0x20]'and ins[0x46565B].op_str=='dword ptr [esi + 0x68], ecx'
    assert ins[0x46564C].op_str=='edx, dword ptr [ebp + 0x24]'and ins[0x46565E].op_str=='dword ptr [esi + 0x6c], edx'
    # Only qualified destination writes in the finite decoded class/caller scope, not a whole-image absence claim.
    scope=[(0x465610,0x465680),(0x450090,0x450135),(0x465680,0x4656A1),(0x4656B0,0x4656FE),(0x465D10,0x465D22),
      (0x46BA10,0x46BA72),(0x46BC88,0x46BCE7),(0x452C20,0x452C98),(0x4A7850,0x4A794C),(0x465940,0x4659ED),(0x450230,0x450260)]
    writes=[at for at,i in ins.items()if any(lo<=at<hi for lo,hi in scope)and i.mnemonic=='mov'and 'ptr ['in i.op_str.split(',')[0]and '+ 0x68]'in i.op_str.split(',')[0]]
    assert writes==[0x46565B]
    assert all(ins[at].op_str=='0x46bc88'for at in [0x46BA47,0x46BA54,0x46BA61])
    assert [ins[at].op_str for at in [0x46BA29,0x46BA4D,0x46BA5A]]==['eax, dword ptr [0x64a990]','eax, dword ptr [0x64a98c]','eax, dword ptr [0x64a930]']
    assert not any(i.mnemonic.startswith('j')for at,i in ins.items()if 0x46BA67<=at<0x46BA72)
    clear=[i for at,i in ins.items()if 0x450230<=at<0x450260]
    assert [i.op_str for i in clear if i.mnemonic=='mov'and i.op_str.startswith('dword ptr [esi')]==['dword ptr [esi + 0x2c], ebx','dword ptr [esi + 0x28], ebx']
    assert ins[0x45024A].op_str=='1'and ins[0x45024C].op_str=='dword ptr [edx]'
    # C9 known enter-channel result: result0 only. Other result bodies deliberately remain outside the slice.
    for va,b,jva in [(0x4B3539,0x71,0x4B33CC),(0x4B377D,0x2B,0x4B361C)]:assert pe.get_data(va-0x400000,1)==bytes([b])and int.from_bytes(pe.get_data(jva-0x400000,4),'little')==0x4B309F
    assert pe.get_data(0xA79AC,1)==b'\0'and int.from_bytes(pe.get_data(0xA7998,4),'little')==0x4A7897
    assert ins[0x4A7941].op_str=='0x46ba10'
    scene_table=struct.unpack('<5I',pe.get_data(0x6A244,20));assert scene_table[0]==0x469CC9
    assert ins[0x469CB7].mnemonic=='ret'and ins[0x469E9D].op_str=='0x46a520'
    # Map receiving representation -> register argument -> low byte row store; no executing map-resource provider.
    assert ins[0x4659A6].op_str=='edx, dword ptr [ebp + 0x20]'and ins[0x4659AD].op_str=='edx'
    assert ins[0x463C47].op_str=='dl, byte ptr [ebp + 0x20]'and ins[0x463C50].op_str=='byte ptr [esi + 0x74], dl'
    assert ins[0x463D73].op_str=='al, byte ptr [ebp + 8]'and ins[0x463D76].op_str=='byte ptr [ecx + 0x74], al'
    assert not any('0x64afd0'in i.op_str or'0x64afe0'in i.op_str for at,i in ins.items()if 0x4A7E13<=at<0x4A7EA7)
    assert ins[0x4A7ABC].op_str=='edi, edi'and ins[0x4A7AC4].mnemonic=='movsx'and ins[0x4A7AD5].op_str=='ecx, 0x24'
    table=struct.unpack('<6I',pe.get_data(0xA7F40,24));flags=[int.from_bytes(pe.get_data(va-0x400000+1,4),'little')for va in table]
    assert flags==[0x2000,0x200,0x400,0x1000,0x800,0x100]
    models=[{'label':'newfull_special_FF9C_mode1','computed':(0x9C-1-1)&255,'expected':0x9A},
      {'label':'old_or_partial_special_FF9C_mode1','computed':(0xFE-1)&255,'expected':0xFD},
      {'label':'partial_ordinary_WORD0101','computed':(0x0101+12)&255,'expected':13},
      {'label':'newfull_lookupindex256_truncation','computed':256&255,'expected':0},
      {'label':'validattrs0_mode1_flags','computed':1|flags[1],'expected':0x201},
      {'label':'partial_flags_preserve_only_low8','computed':(0x1234AB02&255)|flags[2],'expected':0x402}]
    assert all(r['computed']==r['expected']for r in models)
    branches=[]
    for lo,hi in [(0x465680,0x4656A1),(0x4656B0,0x4656FE),(0x46BC88,0x46BCE7),(0x452C20,0x452C98),(0x451514,0x451547),(0x4A7850,0x4A794C)]:
        for at,i in sorted(ins.items()):
            if lo<=at<hi and i.mnemonic.startswith('j')and i.op_str.startswith('0x'):
                target=int(i.op_str,16)
                if lo<=target<hi:assert target in ins;branches.append({'va':f'{at:08X}','target':f'{target:08X}','aligned':True})
    for r in report['prior_primary_pins']:assert record(Path(r['path']))['sha256']==r['expected_sha256']
    for r in report['existing_evidence'].values():assert record(Path(r['path']))==r
    for r in report['reusable_outputs']:assert record(Path(r['path']))==r
    assert report['delta']['new_wire']==report['delta']['new_opcodes']==report['delta']['wire_corrections']==[]
    assert record(ROOT/'server-packet-delta.json')==report['verification']['server_packet_unchanged']
    result={'primary':record(ROOT/'SCC-catalog-lifecycle-contract.json'),'all_match':True,'new_ranges':19,'new_code_ranges':11,'new_data_ranges':8,
      'inherited_raw_ranges':19,'instruction_assertions':len(assertions),'aligned_in_slice_branch_targets':len(branches),'branches':branches,
      'constructor_stack_arguments':args,'qualified_UI68_writes_in_finite_scope':[f'{v:08X}'for v in writes],'map_arithmetic_models':models,
      'complete_UI_constructor_bytes':112,'complete_base_constructor_bytes':165,'prior_immutable_pins':21,
      'whole_image_UI68_setter_absence_claim':False,'actual_Native_UI_object_lifecycle_or_map_resource_qualified':False,'old_tests_replayed':False}
    write('SCC-catalog-lifecycle/independent-review.json',result);print(json.dumps(result,ensure_ascii=False))
if __name__=='__main__':main()

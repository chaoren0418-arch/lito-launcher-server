"""Read back only new Batch21 map-source slices and resource candidates."""
from audit import *
def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))
def main():
    report=load('map-table-source-contract.json');bundle=load('map-table-source/ranges.json');assertions=load('map-table-source/instruction-witnesses.json')['assertions']
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);ins={};coverage=set()
    for r in bundle['ranges']:
        va=int(r['va'],16);raw=Path(r['output']['path']).read_bytes();assert record(Path(r['output']['path']))==r['output']and raw==pe.get_data(va-0x400000,r['bytes'])and raw.hex()==r['raw_hex']
        assert mapping(pe,va)['file_offset']==r['file_offset']
        if r['kind']=='code':
            items=list(cs.disasm(raw,va));assert sum(i.size for i in items)==len(raw)
            assert [{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in items]==r['instructions']
            ins.update({i.address:i for i in items});coverage.update(range(va,va+len(raw)))
    for r in bundle['inherited_raw']:
        q=r['range'];base=int(r['base_va'],16);va=int(r['va'],16);raw=Path(q['path']).read_bytes();assert record(Path(q['path']))==q and raw==pe.get_data(base-0x400000,len(raw))
        assert record(Path(r['index']['path']))==r['index'];off=r['range_offset'];assert va==base+off
        items=list(cs.disasm(raw[off:off+r['used_bytes']],va));assert sum(i.size for i in items)==r['used_bytes'];ins.update({i.address:i for i in items})
    for r in assertions:
        i=ins[int(r['va'],16)];assert(i.bytes.hex(),i.mnemonic,i.op_str)==(r['raw_hex'],r['expected_mnemonic'],r['expected_operands'])
    complete=[(0x46F870,0x46F87A),(0x46F880,0x46F88A),(0x46F890,0x46F89C),(0x46F8A0,0x46F8AA),
      (0x46F8D0,0x46F8E6),(0x4D3DB0,0x4D3DBD),(0x46F8B0,0x46F8BD),(0x46F8C0,0x46F8CE),
      (0x46F8F0,0x46F95A),(0x4D3DC0,0x4D3DF3),(0x4D43D0,0x4D43FD),(0x4658B0,0x46591C),(0x4687D0,0x4687DD)]
    for lo,hi in complete:
        assert set(range(lo,hi))<=coverage
        assert next(i for at,i in ins.items()if at+i.size==hi).mnemonic in ['ret','jmp']
    # Constructor and destructor do not create an empty, reusable post-cleanup main table state.
    ctor=[i for at,i in ins.items()if 0x46F8D0<=at<0x46F8E6]
    assert [i.op_str for i in ctor if i.mnemonic=='mov'and i.op_str.startswith('dword ptr [esi')]==['dword ptr [esi + 0x10], 0']
    dtor=[i for at,i in ins.items()if 0x46F8F0<=at<0x46F95A]
    assert not any(i.mnemonic=='mov'and i.op_str.split(',')[0]in ['dword ptr [edi]','dword ptr [edi + 0x10]']for i in dtor)
    assert ins[0x46F91D].op_str=='ecx, dword ptr [eax - 4]'and ins[0x46F92A].op_str=='0x24'
    assert ins[0x46F924].op_str=='0x46f8c0'and ins[0x46F946].op_str=='0x4d3dc0'
    assert ins[0x4D3DB4].op_str=='dword ptr [eax], ecx'and ins[0x4D3DB6].op_str=='dword ptr [eax + 8], ecx'and ins[0x4D3DB9].op_str=='dword ptr [eax + 4], ecx'
    # Qualified chains stop at their first private/external call. No parser/provider body is included.
    assert ins[0x4D4879].bytes.hex()=='e89fc5b604'and ins[0x4D4879].op_str=='0x5040e1d'
    assert next(r for r in bundle['ranges']if r['va']=='004D4870')['bytes']==14
    assert ins[0x4D4476].op_str=='0x773f5200'and next(r for r in bundle['ranges']if r['va']=='004D4450')['bytes']==43
    assert 0x4D4489 not in ins and 0x46FB1E not in ins # not parser/stream continuation
    assert pe.get_data(0x102070,4)==struct.pack('<I',0x46F870)
    assert pe.get_data(0x108900,10)==b'%s-%c.tbl\0'and pe.get_data(0x236590,9)==b'LTmapstr\0'
    assert ins[0x4D48DB].op_str=='ecx, 0x64afd0'and ins[0x4D48E0].op_str=='0x46f9c0'
    assert ins[0x46F9E9].op_str=='ebx, [esi + 4]'and ins[0x46F9EE].op_str=='0x4d4450'
    # Locale selector is a deterministic local function of an unqualified runtime value, not a known API name.
    jump=struct.unpack('<5I',pe.get_data(0xD4400,20));slots=pe.get_data(0xD4414,19)
    def suffix(value):
        k=(value-932)&0xFFFFFFFF;at=0x4D43FA if k>18 else jump[slots[k]]
        return chr(int(ins[at].op_str.split(', ')[1],16))
    locale_models=[{'selector':v,'expected':e,'computed':suffix(v)}for v,e in [(932,'j'),(936,'h'),(949,'k'),(950,'c'),(933,'e'),(-1,'e'),(951,'e')]]
    assert all(r['computed']==r['expected']for r in locale_models)
    # Conditional direct stores are explicitly an island after unknown inputs; no whole-builder completion assertion.
    assert ins[0x46FA6F].op_str=='dword ptr [esi + 0x10], eax'and ins[0x46FA72].op_str=='edi, [esi + 0x14]'and ins[0x46FA75].op_str=='ecx, 7'
    assert ins[0x46FA8C].op_str=='dword ptr [esi], eax'and ins[0x46FA7E].op_str=='eax, dword ptr [esi + 4]'
    assert ins[0x46B9C6].op_str=='esi, 0x24'and ins[0x4A7AD5].op_str=='ecx, 0x24'
    assert ins[0x465908].op_str=='al, byte ptr [edx + 0x74]'and ins[0x4687D6].op_str=='byte ptr [ecx + 0xf], al'
    assert ins[0x4681BA].mnemonic=='jle'and ins[0x4681DB].mnemonic=='movsx'
    def preview_path(b):return'negative-version-branch'if (b if b<128 else b-256)<=-1 else'conditional-table-title-index'
    byte_models=[{'BYTE_hex':f'{b:02X}','computed':preview_path(b),'expected':e}for b,e in [(0,'conditional-table-title-index'),(127,'conditional-table-title-index'),(128,'negative-version-branch'),(154,'negative-version-branch'),(255,'negative-version-branch')]]
    assert all(r['computed']==r['expected']for r in byte_models)
    island=[i for at,i in ins.items()if 0x4681AF<=at<0x4681F6]
    assert not any('0x64afd0'in i.op_str for i in island)
    assert ins[0x4683F6].op_str=='edi'and pe.get_data(0xF7314,4)==struct.pack('<I',0x773F5200)
    assert ins[0x46804F].op_str=='0x5ad0986'
    index=load('resources/archive-manifest.json');assert record(ROOT/'resources/archive-manifest.json')==report['resource_candidates']['archive_manifest']
    candidates=report['resource_candidates']['candidates'];assert len(candidates)==5
    for r in candidates:
        match=next(q for q in index['selected_members']if q.get('namespace')=='native'and q.get('archive')=='LTtext'and q.get('name')==r['name'])
        for k in ['raw','decoded']:assert r[k]==match[k]and record(Path(r[k]['path']))==r[k]
        lines=Path(r['decoded']['path']).read_bytes().splitlines();nonempty=[(i+1,b)for i,b in enumerate(lines)if b.strip()]
        assert [{'physical_line_1based':i,'raw_hex':b.hex()}for i,b in nonempty[:6]]==r['first_6_nonempty_lines']
        assert nonempty[0][1].hex()==r['first_nonempty_line_hex']
    branches=[]
    for lo,hi in complete+[(0x46B926,0x46B9D1),(0x4681AF,0x4681F6)]:
        for at,i in sorted(ins.items()):
            if lo<=at<hi and i.mnemonic.startswith('j')and i.op_str.startswith('0x'):
                target=int(i.op_str,16)
                if lo<=target<hi:assert target in ins;branches.append({'va':f'{at:08X}','target':f'{target:08X}','aligned':True})
    for r in report['prior_primary_pins']:assert record(Path(r['path']))['sha256']==r['expected_sha256']
    for r in report['existing_evidence'].values():assert record(Path(r['path']))==r
    for r in report['reusable_outputs']:assert record(Path(r['path']))==r
    assert report['delta']['new_wire']==report['delta']['new_opcodes']==report['delta']['wire_corrections']==[]
    assert record(ROOT/'server-packet-delta.json')==report['verification']['server_packet_unchanged']
    result={'primary':record(ROOT/'map-table-source-contract.json'),'all_match':True,'new_code_ranges':24,'new_data_ranges':7,'inherited_raw_slices':3,
      'instruction_assertions':len(assertions),'aligned_in_slice_branch_targets':len(branches),'branches':branches,'locale_models':locale_models,'BYTE_predicate_models':byte_models,
      'existing_resources':5,'raw_decoded_asset_pins':10,'prior_immutable_pins':22,'first_external_private_boundaries_stopped':True,
      'actual_table_generation_locale_loaded_resource_or_NativeUI_qualified':False,'Batch20_PASS_replayed':False}
    write('map-table-source/independent-review.json',result);print(json.dumps(result,ensure_ascii=False))
if __name__=='__main__':main()

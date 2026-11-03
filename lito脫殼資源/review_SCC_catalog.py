"""Independent Batch19 exact raw/row dedup/DWORD key/sealed reset review."""
from audit import *

def main():
    report=json.loads((ROOT/'SCC-catalog-contract.json').read_text(encoding='utf-8'))
    bundle=json.loads((ROOT/'SCC-catalog/ranges.json').read_text(encoding='utf-8'))
    assertions=json.loads((ROOT/'SCC-catalog/instruction-witnesses.json').read_text(encoding='utf-8'))['assertions']
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);ins={};covered=set()
    for r in bundle['ranges']:
        va=int(r['va'],16);b=Path(r['output']['path']).read_bytes();assert record(Path(r['output']['path']))==r['output']and b==pe.get_data(va-0x400000,r['bytes'])
        if r['kind']=='code':
            items=list(cs.disasm(b,va));assert sum(i.size for i in items)==len(b)
            assert [{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in items]==r['instructions']
            ins.update({i.address:i for i in items});covered.update(range(va,va+len(b)))
    for r in bundle['inherited_raw']:
        p=r['range'];va=int(r['va'],16);b=Path(p['path']).read_bytes();assert record(Path(p['path']))==p and b==pe.get_data(va-0x400000,len(b))
        assert record(Path(r['index']['path']))==r['index']
        if r['kind']=='code':
            items=list(cs.disasm(b[:r['used_bytes']],va));assert sum(i.size for i in items)==r['used_bytes'];ins.update({i.address:i for i in items})
    for lo,hi in [(0x4A7A30,0x4A7EA7),(0x465940,0x4659ED),(0x463C10,0x463C88),(0x450260,0x450283),(0x463D10,0x463D3C),(0x4A7EF0,0x4A7F40)]:assert set(range(lo,hi))<=covered
    for r in assertions:
        i=ins[int(r['va'],16)];assert (i.bytes.hex(),i.mnemonic,i.op_str)==(r['raw_hex'],r['expected_mnemonic'],r['expected_operands'])
    branches=[]
    for lo,hi in [(0x4A7A30,0x4A7EA7),(0x465940,0x4659ED),(0x450260,0x450283),(0x4A7EF0,0x4A7F40)]:
        for at,i in sorted(ins.items()):
            if lo<=at<hi and i.mnemonic.startswith('j')and i.op_str.startswith('0x'):
                target=int(i.op_str,16);assert lo<=target<hi and target in ins;branches.append((at,target))
    for va,b,jva in [(0x4B353C,0x74,0x4B33D8),(0x4B3780,0x2E,0x4B3628)]:assert pe.get_data(va-0x400000,1)==bytes([b])and int.from_bytes(pe.get_data(jva-0x400000,4),'little')==0x4B2F13
    assert list(struct.unpack('<6I',pe.get_data(0xA7EA8,24)))==[0x4A7A66,0x4A7C6D,0x4A7CA1,0x4A7D1C,0x4A7D97,0x4A7E13]
    assert pe.get_data(0x2343E4,8)==b'dschhhc\0'and pe.get_data(0x2343DC,8)==b'dsccchc\0'
    assert int.from_bytes(pe.get_data(0xF8C20,4),'little')==0x463D10
    mode_table=struct.unpack('<6I',pe.get_data(0xA7F40,24));assert [int.from_bytes(pe.get_data(va-0x400000+1,4),'little')for va in mode_table]==[0x2000,0x200,0x400,0x1000,0x800,0x100]
    assert ins[0x465965].op_str=='0x465982'and ins[0x465978].op_str=='0x4659da'
    # Duplicate target is only the epilogue, with no record replacement instructions.
    assert [i.mnemonic for at,i in sorted(ins.items())if 0x4659DA<=at<0x4659ED]==['mov','pop','pop','mov','pop','mov','pop','ret']
    assert ins[0x465975].op_str=='dword ptr [ebx + 4], edi'and ins[0x450BCE].op_str=='dword ptr [eax + 4], ecx'
    cc=[i for at,i in ins.items()if 0x4A7A30<=at<0x4A7EA7]
    assert not any(any(x in i.op_str for x in ['0x64abf4','0x64abf8','0x64af98'])for i in cc)
    assert not any(i.mnemonic=='call'and i.op_str in ['0x469ca0','0x46a540','0x4c6ae0']for i in cc)
    assert ins[0x4A906F].op_str=='eax, word ptr [edx + 0xc]'and ins[0x4A906F].mnemonic=='movsx'
    assert ins[0x4B3135].op_str=='0x5ad0986'
    frames=json.loads((ROOT/'SCC-catalog/sealed-CF-CC-frames.json').read_text(encoding='utf-8'))['captures'];assert len(frames)==4
    assert [(f['array_index_zero_based'],f['frame_1based'],f['body_bytes'],f['frame_bytes'])for f in frames]==[(74,75,5,7),(75,76,5,7),(79,80,26,28),(80,81,26,28)]
    expected_CC=bytes.fromhex('cc00010000004c6f63616c204c6f626279000001000800000001')
    for f in frames:
        q=f['source'];assert record(Path(q['path']))==q;rows=json.loads(Path(q['path']).read_text(encoding='utf-8'));r=rows[f['array_index_zero_based']];b=bytes.fromhex(r['body_hex'])
        assert r['direction']=='S'and r['epoch']==f['epoch']and b.hex()==f['body_hex']and r['frame_bytes']==f['frame_bytes']==len(b)+2
        assert f['consumed']==len(b)and f['remaining']==0
        if b[0]==207:assert b==bytes.fromhex('cf02000000')
        else:
            assert b==expected_CC and b[2:6]==bytes.fromhex('01000000')and b[6:18]==b'Local Lobby\0'
            assert b[18:]==bytes.fromhex('0001000800000001') # attr0,users1,cap8,map0,mode1
            assert f['source58_version']==0x734FD33
            assert not any(x.get('direction')=='S'and x.get('body_hex','').startswith('cf')for x in rows[77:81])
    assert frames[2]['body_hex']==frames[3]['body_hex']and frames[2]['epoch']=='Host-GAME'and frames[3]['epoch']=='Guest-GAME'
    for r in report['prior_primary_pins']:assert record(Path(r['path']))['sha256']==r['expected_sha256']
    for r in report['existing_evidence'].values():assert record(Path(r['path']))==r
    for r in report['reusable_outputs']:assert record(Path(r['path']))==r
    assert report['delta']['new_wire']==report['delta']['wire_corrections']==report['delta']['new_opcodes']==[]
    result={'scope':'Newfinitecatalog raw/coverage/branch/knowntrace readback only; noNativeGUI/oldmatrix','primary':record(ROOT/'SCC-catalog-contract.json'),'all_match':True,
      'new_ranges':35,'inherited_raw_ranges':21,'instruction_assertions':len(assertions),'aligned_core_branch_targets':len(branches),'complete_CC_bytes':1143,
      'row_add_helper_bytes':173,'constructor_bytes':120,'sealed_CC_frames':2,'sealed_additional_CF_frames':2,'prior_immutable_pins':20,
      'Native_duplicate_fullrow_ignored_UI68zero':True,'catalog_DWORD_key_separate_from_CF_selected_signedWORD':True,'CC_nojoinedroom_phase_navigation_write':True,
      'Batch18_primary_unchanged_and_private_default_unknown':True,'actual_Native_UI_rowreplacement_or_ownleave_qualified':False}
    write('SCC-catalog/independent-review.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()

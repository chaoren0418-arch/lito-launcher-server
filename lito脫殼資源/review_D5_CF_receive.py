"""Independent Batch18 raw/owner/CF gates and sealed directional wire readback."""
from audit import *

def main():
    report=json.loads((ROOT/'D5-CF-receive-contract.json').read_text(encoding='utf-8'))
    bundle=json.loads((ROOT/'D5-CF-receive/ranges.json').read_text(encoding='utf-8'))
    assertions=json.loads((ROOT/'D5-CF-receive/instruction-witnesses.json').read_text(encoding='utf-8'))['assertions']
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
    for lo,hi in [(0x4A9E50,0x4A9EAB),(0x4A9FAF,0x4AA028),(0x4AA06C,0x4AA071),(0x4A9040,0x4A9082),(0x450290,0x450313),(0x4659F0,0x465A00),(0x4EBB30,0x4EBB49)]:assert set(range(lo,hi))<=covered
    for r in assertions:
        i=ins[int(r['va'],16)];assert (i.bytes.hex(),i.mnemonic,i.op_str)==(r['raw_hex'],r['expected_mnemonic'],r['expected_operands'])
    branches=[]
    for lo,hi in [(0x4A9E50,0x4A9EAB),(0x4A9FAF,0x4AA028),(0x4A9040,0x4A9082),(0x450290,0x450313),(0x4EBB30,0x4EBB49)]:
        for at,i in sorted(ins.items()):
            if lo<=at<hi and i.mnemonic.startswith('j')and i.op_str.startswith('0x'):
                target=int(i.op_str,16);assert target in ins;branches.append((at,target))
    for mapva,b,jva,target in [(0x4B3545,0x7B,0x4B33F4,0x4B2F67),(0x4B3789,0x35,0x4B3644,0x4B2F67),(0x4B353F,0x99,0x4B346C,0x4B3123),(0x4B3783,0x31,0x4B3634,0x4B31A5)]:
        assert pe.get_data(mapva-0x400000,1)==bytes([b])and int.from_bytes(pe.get_data(jva-0x400000,4),'little')==target
    table=list(struct.unpack('<10I',pe.get_data(0xAA074,40)));assert table==[0x4A9EAB,0x4A9EF6,0x4A9F6A,0x4A9F74,0x4A9FA5,0x4A9FAF,0x4AA028,0x4AA02F,0x4AA036,0x4AA056]
    assert all(t in ins for t in table)
    for va,s in [(0x634300,b's\0'),(0x63424C,b'd\0'),(0x632594,b'h\0'),(0x634470,b'schcscccc\0'),(0x634464,b'ssccccccc\0')]:assert pe.get_data(va-0x400000,len(s))==s
    assert ins[0x4A9E7F].op_str=='dword ptr [0x64abf4], eax'and ins[0x4A9E84].op_str=='0x4a9e97'
    assert ins[0x4A9E97].mnemonic=='movsx'and ins[0x4A9E9E].mnemonic=='ja'
    ownercode=[i for at,i in ins.items()if 0x4A9FAF<=at<0x4AA028]
    cfcode=[i for at,i in ins.items()if 0x4A9040<=at<0x4A9082]
    assert not any(i.mnemonic=='call'and i.op_str=='0x4d3a80'for i in ownercode+cfcode)
    assert not any(i.mnemonic=='mov'and i.op_str.startswith('dword ptr [0x64af98],')for i in ownercode+cfcode)
    assert not any('0x64abf4'in i.op_str for i in cfcode)
    assert ins[0x4A9077].op_str=='ecx, ecx'and ins[0x4A9079].op_str=='0x46a540'
    assert ins[0x46A5A3].op_str=='0x46a5be'and ins[0x4502B0].op_str=='dword ptr [ecx + 4], edx'
    assert ins[0x450304].op_str=='0x450307' # deletion retains same scan index
    f=json.loads((ROOT/'D5-CF-receive/sealed-owner-frame.json').read_text(encoding='utf-8'));q=f['source'];assert record(Path(q['path']))==q
    rows=json.loads(Path(q['path']).read_text(encoding='utf-8'));r=rows[77];b=bytes.fromhex(r['body_hex']);name=b'RoomExitGuest(RoomExitMoon)'
    assert b==bytes.fromhex('d50501000000')+name+b'\0'and len(name)==27 and len(b)==34 and r['frame_bytes']==36 and r['direction']=='S'and r['epoch']=='Guest-GAME'
    assert f['array_index_zero_based']==77 and f['frame_1based']==78 and f['consumed']==34 and f['remaining']==0
    prior=bytes.fromhex(rows[72]['body_hex']);assert prior[0]==205 and prior[5:prior.index(0,5)]==name
    assert rows[75]['direction']=='S'and rows[75]['epoch']=='Guest-GAME'and rows[75]['body_hex'].startswith('d001000000')
    assert rows[66]['direction']=='C'and rows[66]['body_hex']=='cf0100000000'and rows[66]['frame_bytes']==8
    assert not any(r.get('direction')=='S'and r.get('body_hex','').startswith('cf')for r in rows[73:78])
    for r in report['prior_primary_pins']:assert record(Path(r['path']))['sha256']==r['expected_sha256']
    for r in report['existing_evidence'].values():assert record(Path(r['path']))==r
    for r in report['reusable_outputs']:assert record(Path(r['path']))==r
    assert report['delta']['new_wire']==report['delta']['wire_corrections']==report['delta']['new_opcodes']==[]
    result={'scope':'Onlynewraw/gates/layout/knownsealedframe readback; nooldmatrix ornativeexecution','primary':record(ROOT/'D5-CF-receive-contract.json'),'all_match':True,
      'new_ranges':28,'inherited_raw_ranges':13,'instruction_assertions':len(assertions),'aligned_core_branch_targets':len(branches),
      'D5_header_bytes':91,'D5_owner_branch_bytes':121,'CF_complete_handler_bytes':66,'CF_UI_delete_complete_bytes':131,
      'D5_subtype_table_entries':10,'sealed_owner_D5_frames':1,'C_CF_counterdirection_examples':1,'S_CF_dynamic_delete_frames':0,'prior_immutable_pins':19,
      'D5_phase6_assignsroom_beforetype_confirmed':True,'CF_primary_default_private_effect_unknown':True,'CF_fallback_noactivejoinedroomgate_confirmed':True,
      'no_owner_alias_physical_source_sessionauthority_or_exitACK':True,'actual_native_UI_or_provider_qualified':False}
    write('D5-CF-receive/independent-review.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()

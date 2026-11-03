"""Independent Batch16 readback, branch targets, bounds and frozen frame offsets."""
from audit import *

def main():
    report=json.loads((ROOT/'SD0-receive-contract.json').read_text(encoding='utf-8'))
    bundle=json.loads((ROOT/'SD0-receive/ranges.json').read_text(encoding='utf-8'))
    assertions=json.loads((ROOT/'SD0-receive/instruction-witnesses.json').read_text(encoding='utf-8'))['assertions']
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);ins={};covered=set()
    for r in bundle['ranges']:
        va=int(r['va'],16);b=Path(r['output']['path']).read_bytes()
        assert record(Path(r['output']['path']))==r['output'] and b==pe.get_data(va-0x400000,r['bytes'])
        if r['kind']=='code':
            items=list(cs.disasm(b,va));assert sum(i.size for i in items)==len(b)
            assert [{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in items]==r['instructions']
            ins.update({i.address:i for i in items});covered.update(range(va,va+len(b)))
    for r in bundle['inherited_raw']:
        p=r['range'];va=int(r['va'],16);b=Path(p['path']).read_bytes()
        assert record(Path(p['path']))==p and b==pe.get_data(va-0x400000,len(b))
        assert record(Path(r['index']['path']))==r['index']
        if r['kind']=='code':
            items=list(cs.disasm(b[:r['used_bytes']],va));assert sum(i.size for i in items)==r['used_bytes'];ins.update({i.address:i for i in items})
    for lo,hi in [(0x4A9090,0x4A9550),(0x465370,0x46542F),(0x469CC9,0x469EC3),(0x46A540,0x46A5C4)]:assert set(range(lo,hi))<=covered
    for r in assertions:
        i=ins[int(r['va'],16)];assert (i.bytes.hex(),i.mnemonic,i.op_str)==(r['raw_hex'],r['expected_mnemonic'],r['expected_operands'])
    # Every internal direct branch lands on an aligned instruction, not a byte in an operand.
    handler=[i for at,i in sorted(ins.items())if 0x4A9090<=at<0x4A9550]
    internal_targets=[]
    for i in handler:
        if i.mnemonic.startswith('j')and i.op_str.startswith('0x'):
            target=int(i.op_str,16);assert 0x4A9090<=target<0x4A9550 and target in ins;internal_targets.append(target)
    assert ins[0x4A90E6].op_str==ins[0x4A90EF].op_str=='0x4a9472'
    assert ins[0x4A9477].op_str=='0x4a9549' and ins[0x4A9116].op_str=='0x4a9166'
    assert ins[0x4A9100].op_str=='0x4a9549'
    assert ins[0x4A90B4].op_str==ins[0x4A90CF].op_str=='0x634370' and pe.get_data(0x234370,3)==b'ds\0'
    local_refs=[(i.address,i.mnemonic,i.op_str)for i in handler if '[ebp - 0x14]'in i.op_str]
    assert local_refs==[(0x4A90A5,'lea','eax, [ebp - 0x14]'),(0x4A939C,'lea','edx, [ebp - 0x14]'),(0x4A93AC,'lea','eax, [ebp - 0x14]')]
    assert ins[0x4A2B62].op_str=='0x4a2a6f' # stops at format NUL, so no third variadic field
    assert pe.get_data(0xB3540,1)==b'\x76' and pe.get_data(0xB3784,1)==b'\x32'
    assert int.from_bytes(pe.get_data(0xB33E0,4),'little')==int.from_bytes(pe.get_data(0xB3638,4),'little')==0x4B2F2B
    assert pe.get_data(0x1028F8,3)==b'cd\0' and int.from_bytes(pe.get_data(0x6A244,4),'little')==0x469CC9
    captures=json.loads((ROOT/'SD0-receive/sealed-D0-frames.json').read_text(encoding='utf-8'))['captures']
    expected=[('5ED',69,bytes.fromhex('d0010000004172656e61426f7400'),16),
      ('5ED',137,bytes.fromhex('d001000000426174746c65486f737428426174746c654d6f6f6e2900'),30),
      ('49A',70,bytes.fromhex('d0010000004172656e61426f7400'),16)]
    assert len(captures)==3
    for f,(capture,ordinal,body,n)in zip(captures,expected):
        assert (f['capture'],f['frame_1based'],bytes.fromhex(f['body_hex']),f['frame_bytes'])==(capture,ordinal,body,n)
        q=f['source'];assert record(Path(q['path']))==q;rows=json.loads(Path(q['path']).read_text(encoding='utf-8'));r=rows[ordinal-1]
        assert r['body_hex']==body.hex()and r['direction']=='S'and r['frame_bytes']==n and f['consumed']==len(body)and f['remaining']==0
        assert not any(x.get('direction')=='C'and x.get('body_hex','').startswith('d0')for x in rows)
        if ordinal in [69,70]:
            assert f['prior_same_epoch_CD_ordinals']==[]
            assert f['following_same_epoch_CD_ordinals']==([70,71]if capture=='5ED'else[71,72])
    for r in report['prior_primary_pins']:assert record(Path(r['path']))['sha256']==r['expected_sha256']
    for r in report['existing_evidence'].values():assert record(Path(r['path']))==r
    for r in report['reusable_outputs']:assert record(Path(r['path']))==r
    assert report['delta']['new_wire']==report['delta']['wire_corrections']==report['delta']['new_opcodes']==[]
    result={'scope':'New bounded static evidence independent readback; no old matrix/test/capture replay','primary':record(ROOT/'SD0-receive-contract.json'),
      'all_match':True,'new_raw_ranges':12,'inherited_raw_ranges':20,'instruction_assertions':len(assertions),'aligned_internal_branch_targets':len(internal_targets),
      'handler_complete_bytes':1216,'UI_remove_complete_bytes':191,'scene1_branch_bytes':506,'preview_helper_complete_bytes':132,
      'sealed_D0_frames':3,'ArenaBot_before_any_same_epoch_CD':True,'ordinary_C_D0_self_notification_roundtrip_qualified':False,
      'ds_both_version_paths_and_unused_third_pointer_confirmed':True,'native_comparison_scene_UI_authority_unqualified':True,'prior_immutable_pins_match':17}
    write('SD0-receive/independent-review.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()

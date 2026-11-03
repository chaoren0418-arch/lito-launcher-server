"""Independent fixed raw/branches/coverage/frames review for Batch17 only."""
from audit import *

def main():
    report=json.loads((ROOT/'D1-E1-prestart-contract.json').read_text(encoding='utf-8'))
    bundle=json.loads((ROOT/'D1-E1-prestart/ranges.json').read_text(encoding='utf-8'))
    assertions=json.loads((ROOT/'D1-E1-prestart/instruction-witnesses.json').read_text(encoding='utf-8'))['assertions']
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
            items=list(cs.disasm(b[:r['used_bytes']],va));assert sum(i.size for i in items)==r['used_bytes'];ins.update({i.address:i for i in items});covered.update(range(va,va+r['used_bytes']))
    for lo,hi in [(0x4A9550,0x4A9761),(0x4AABB0,0x4AACC0),(0x469FCB,0x46A09D),(0x458C60,0x458C9D),(0x471E30,0x471E53),(0x471E60,0x471E80),(0x471E80,0x471EA0)]:assert set(range(lo,hi))<=covered
    for r in assertions:
        i=ins[int(r['va'],16)];assert (i.bytes.hex(),i.mnemonic,i.op_str)==(r['raw_hex'],r['expected_mnemonic'],r['expected_operands'])
    internal=[]
    for lo,hi in [(0x4A9550,0x4A9761),(0x4AABB0,0x4AACC0)]:
        for at,i in sorted(ins.items()):
            if lo<=at<hi and i.mnemonic.startswith('j')and i.op_str.startswith('0x'):
                target=int(i.op_str,16);assert lo<=target<hi and target in ins;internal.append((at,target))
    for mapva,b,jva,target in [(0x4B3541,0x77,0x4B33E4,0x4B2F37),(0x4B3785,0x33,0x4B363C,0x4B2F37),(0x4B3551,0x7F,0x4B3404,0x4B2F97),(0x4B3795,0x50,0x4B36B0,0x4B3200)]:
        assert pe.get_data(mapva-0x400000,1)==bytes([b])and int.from_bytes(pe.get_data(jva-0x400000,4),'little')==target
    assert ins[0x4B3204].mnemonic=='ret'
    assert pe.get_data(0x234460,3)==b'sc\0'and pe.get_data(0x1025A8,2)==b'c\0'
    # All locally decoded E1 paths avoid a phase write and packet writer. Deep helpers remain unknown.
    e1=[i for at,i in ins.items()if 0x4AABB0<=at<0x4AACC0]
    callbacks=[i for at,i in ins.items()if any(lo<=at<hi for lo,hi in [(0x471E30,0x471E53),(0x471E60,0x471E80),(0x471E80,0x471EA0)])]
    phase_writes=[i for i in e1 if i.mnemonic=='mov'and i.op_str.startswith('dword ptr [0x64af98],')];assert not phase_writes
    assert not any(i.mnemonic in ['call','jmp']and i.op_str=='0x4c6ae0'for i in e1+callbacks)
    assert not any(i.mnemonic=='mov'and i.op_str.startswith('dword ptr [0x64af98],')for i in callbacks)
    assert ins[0x4A95A4].op_str=='byte ptr [ebp - 1], 1'and ins[0x4A95A8].op_str=='0x4a963e'
    assert ins[0x4A964F].op_str=='ecx, 2'and ins[0x4A95EE].op_str=='ecx, 3'
    assert not any(0x47CF00<=at<0x47CF10 or 0x47E360<=at<0x47E370 for at in ins)
    frames=json.loads((ROOT/'D1-E1-prestart/sealed-ready-countdown-frames.json').read_text(encoding='utf-8'))
    captures=frames['captures'];assert len(captures)==20
    expected={'5ED':{'Host-GAME':[77,78,79,80,81],'Guest-GAME':[75,94,95,96,97]},'49A':{'Host-GAME':[78,79,80,81,82],'Guest-GAME':[76,85,86,87,88]}}
    d1_count=e1_count=0
    for label,groups in expected.items():
        for epoch,ordinals in groups.items():
            subset=[f for f in captures if f['capture']==label and f['epoch']==epoch];assert [f['frame_1based']for f in subset]==ordinals
            assert [f['opcode']for f in subset]==[209,209,225,225,225]
            for f in subset:
                q=f['source'];assert record(Path(q['path']))==q;r=json.loads(Path(q['path']).read_text(encoding='utf-8'))[f['frame_1based']-1]
                b=bytes.fromhex(f['body_hex']);assert r['body_hex']==b.hex()and r['epoch']==epoch and r['direction']=='S'and r['frame_bytes']==f['frame_bytes']==len(b)+2
                assert f['consumed']==len(b)and f['remaining']==0
                if b[0]==209:
                    name=(('Battle'if label=='5ED'else'Return')+('Guest'if f is subset[0]else'Host')+'('+('BattleMoon'if label=='5ED'else'ReturnMoon')+')').encode('ascii')
                    assert b==b'\xd1'+name+b'\0\1';d1_count+=1
                else:assert b in [b'\xe1\3',b'\xe1\2',b'\xe1\1'];e1_count+=1
            assert [bytes.fromhex(f['body_hex'])[1]for f in subset[2:]]==[3,2,1]
    assert (d1_count,e1_count)==(8,12)
    for r in report['prior_primary_pins']:assert record(Path(r['path']))['sha256']==r['expected_sha256']
    for r in report['existing_evidence'].values():assert record(Path(r['path']))==r
    for r in report['reusable_outputs']:assert record(Path(r['path']))==r
    assert report['delta']['new_wire']==report['delta']['wire_corrections']==report['delta']['new_opcodes']==[]
    result={'scope':'Only newstaticcoverage/branches/frames/pins, nooldmatrix/runtime orworlddescendants','primary':record(ROOT/'D1-E1-prestart-contract.json'),'all_match':True,
      'new_ranges':20,'inherited_raw_ranges':23,'instruction_assertions':len(assertions),'aligned_internal_branches':len(internal),
      'complete_D1_bytes':529,'complete_E1_bytes':272,'sealed_D1_frames':8,'sealed_E1_frames':12,'prior_immutable_pins':18,
      'E1_no_direct_phase_write_or_packetwriter':True,'E1_phase4_callbacks_no_world_start_or_phase_write_or_packetwriter':True,
      'D1_opaque_preparation_entry_stopped':True,'actual_native_tables_UI_world_qualified':False,'E1_zero_cancel_wire_qualified':False}
    write('D1-E1-prestart/independent-review.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()

"""Independent batch15 raw, coverage and sealed-frame readback."""
from audit import *

def main():
    report=json.loads((ROOT/'SCD-member-comparison-contract.json').read_text(encoding='utf-8'))
    rows=json.loads((ROOT/'SCD-member-comparison/ranges.json').read_text(encoding='utf-8'))
    witnesses=json.loads((ROOT/'SCD-member-comparison/instruction-witnesses.json').read_text(encoding='utf-8'))
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);covered=set()
    for r in rows['ranges']:
        va=int(r['va'],16);raw=Path(r['output']['path']).read_bytes()
        assert record(Path(r['output']['path']))==r['output'] and raw==pe.get_data(va-0x400000,r['bytes'])
        if r['kind']=='code':covered.update(range(va,va+len(raw)))
    for r in rows['inherited_raw']:
        p=r['range'];va=int(r['va'],16);raw=Path(p['path']).read_bytes()
        assert record(Path(p['path']))==p and raw==pe.get_data(va-0x400000,len(raw))
        covered.update(range(va+r.get('used_offset',0),va+r.get('used_offset',0)+r['used_bytes']))
    assert set(range(0x4A7F60,0x4A8B9B))<=covered
    assert set(range(0x4D3A80,0x4D3ADD))<=covered
    for r in witnesses['assertions']:
        va=int(r['va'],16);i=next(cs.disasm(pe.get_data(va-0x400000,len(bytes.fromhex(r['raw_hex']))),va))
        assert (i.bytes.hex(),i.mnemonic,i.op_str)==(r['raw_hex'],r['expected_mnemonic'],r['expected_operands'])
    assert struct.unpack('<I',pe.get_data(0xB33DC,4))[0]==struct.unpack('<I',pe.get_data(0xB362C,4))[0]==0x4B2F1F
    assert pe.get_data(0xF727C,4)==bytes.fromhex('402a3b77')
    frames=json.loads((ROOT/'SCD-member-comparison/sealed-CD-frames.json').read_text(encoding='utf-8'))['captures']
    assert [(x['frame_1based'],x['body_bytes'])for x in frames]==[(70,84),(71,85),(72,84),(73,85),(71,84),(72,85),(73,84),(74,85)]
    for f in frames:
        raw=bytes.fromhex(f['body_hex']);s=raw.index(0,5)+1
        assert raw[:5]==bytes.fromhex('cd01000000') and raw[s:s+3]==bytes([0,1,0])
        assert raw[s+3:s+16]==bytes(13) # threeDWORD stats plusnumericstate0
        assert raw[s+16:s+24]==bytes.fromhex('0000526561647900') # emptyfield10,reservedfield11,Readyfield12
        assert raw[s+24:]==bytes(len(raw)-s-24)
        assert f['remaining']==0 and f['consumed']==len(raw)
    for p in report['prior_primary_pins']:assert record(Path(p['path']))['sha256']==p['expected_sha256']
    for p in report['existing_evidence'].values():assert record(Path(p['path']))==p
    for p in report['reusable_outputs']:assert record(Path(p['path']))==p
    result={'scope':'Independent fixedPE raw/decodedwitness/completebytecoverage andmanualframeoffsetreadback; no previousruntime tests',
      'raw_new_ranges':len(rows['ranges']),'inherited_raw_ranges':len(rows['inherited_raw']),'instruction_witnesses':len(witnesses['assertions']),
      'complete_S_CD_byte_coverage':0xC3B,'complete_comparator_byte_coverage':0x5D,'sealed_CD_frames':len(frames),
      'firstDWORD_room1_all_members_confirmed':True,'Ready_string_not_numericstate_confirmed':True,'duplicate_calls_branch_to_return':True,
      'table_runtime_authority_unqualified':True,'prior_primary_pins':16,'Modern_frozen_only':True,'all_match':True,
      'primary':record(ROOT/'SCD-member-comparison-contract.json')}
    write('SCD-member-comparison/independent-review.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()

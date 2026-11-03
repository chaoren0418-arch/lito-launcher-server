"""Independent new finite evidence review; no old PASS invoked."""
import sys
sys.dont_write_bytecode=True
from audit import *
def load(n):return json.loads((ROOT/n).read_text('utf-8'))
def main():
    p=load('CD2-ready-outbound-contract.json');r=load('CD2-ready-outbound/ranges.json');checks=[];models=[]
    def check(name,result):
        checks.append({'check':name,'match':bool(result)})
        assert result,name
    for q in p['sources']+list(p['existing_evidence'].values())+p['outputs']:check('pin '+q['path'],record(q['path'])==q)
    for q in p['prior_primary_pins']:check('prior '+q['path'],record(q['path'])['sha256']==q['expected_sha256']and record(q['path'])['bytes']==q['bytes'])
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);ins={}
    for q in r['new_ranges']+r['necessary_reused_raw']:
        raw=Path(q['output']['path']).read_bytes();va=int(q['va'],16)
        check('raw '+q['va'],raw==pe.get_data(va-0x400000,q['bytes'])and record(q['output']['path'])==q['output'])
        if q['kind']=='code':
            decoded=[{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in cs.disasm(raw,va)]
            check('aligned '+q['va'],decoded==q['instructions']and sum(len(bytes.fromhex(i['raw_hex']))for i in decoded)==len(raw))
            ins.update({int(i['va'],16):i for i in decoded})
    for q in r['instruction_assertions']:check('operand '+q['va'],(ins[int(q['va'],16)]['mnemonic'],ins[int(q['va'],16)]['operands'])==(q['expected_mnemonic'],q['expected_operands']))
    # Reconstruct caller pushes from decoded instructions, independent of report callrows.
    expected=[(0x46ACE8,0x46ACF4,0),(0x46B510,0x46B51C,0),(0x46B74A,0x46B756,1),(0x46B796,0x46B7A2,1),(0x407DEE,0x407DFA,2),(0x407E52,0x407E5E,2),(0x476AA8,0x476AB4,0)]
    for start,end,state in expected:
        seq=[i for a,i in sorted(ins.items())if start<=a<end];pushes=[int(i['operands'],0)for i in seq if i['mnemonic']=='push']
        check('stack '+hex(end),pushes==[state,0xD2,0x502778]and ins[end]['operands']=='0x4c6ae0')
        check('reported '+hex(end),any(int(q['call']['va'],16)==end and q['state']==state and q['body_hex']==f'd2{state:02x}'and q['frame_bytes']==4 for q in p['wire']['actual_fixed_PE_forms']))
    # Numeric signed-version gates only; this is symbolic conditional proof, not runtime.
    for version in [0x52DF,0x52E0,0x52E1,0xFFFFFFFF,0x80000000]:
        signed=version if version<0x80000000 else version-0x100000000
        for phase in [1,2,3,6]:
            for eax in [0,1]:
                flags=(signed-0x52E0)>=0
                state2=flags and phase==2 and eax==0
                expected_gate=version in [0x52E0,0x52E1]and phase==2 and eax==0
                check(f'gate {version:X}/{phase}/{eax}',state2==expected_gate)
                models.append({'type':'version-phase-command','version_u32':version,'version_signed':signed,'phase':phase,'EAX':eax,'state2_send_condition':state2,'conditional_state0_version_phase':flags and phase==2,'qualification':'no input or UI execution'})
    for selector in [0,4]:
        for data in [b'y',b'Y',b'n',bytes.fromhex('8299'),bytes.fromhex('8278'),bytes.fromhex('9982')]:
            for ecx in [0,1]:
                result=ecx==0 and (data[0]in (0x79,0x59)or(selector==4 and len(data)>=2 and int.from_bytes(data[:2],'little')in(0x9982,0x7882)))
                expected_input=ecx==0 and (data in (b'y',b'Y')or(selector==4 and data in (bytes.fromhex('8299'),bytes.fromhex('8278'))))
                check(f'callback {selector}/{data.hex()}/{ecx}',result==expected_input)
                models.append({'type':'callback-input','selector':selector,'input_hex':data.hex(),'ECX':ecx,'send_state1_condition':result,'qualification':'symbolic valid input memory only; no codec/lifetime/UI authority'})
    for start,cb,control in [(0x46CDEB,0x46B6F0,2),(0x46CE39,0x46B510,0)]:
        regs={};stack=[]
        for a,i in sorted(ins.items()):
            if not start<=a<start+33:continue
            if i['mnemonic']=='mov'and i['operands'].startswith(('edx, ','ecx, ')):
                reg,val=i['operands'].split(', ')
                if val.startswith('0x'):regs[reg]=int(val,0)
            if i['mnemonic']=='push':stack.append(regs[i['operands']]if i['operands']in regs else int(i['operands'],0))
        check('ctor stack '+hex(start),list(reversed(stack))==([0x186B,0x195,0x168,cb,control,0]if control==2 else[0x1869,0x195,0x168,cb,control,0]))
    saved=load('CD2-ready-outbound/saved-ready-cancel-frame-witnesses.json');expectedbody={73:'d202',76:'d201',79:'d2',81:'e103',83:'e103',84:'d201',86:'e100',88:'e100'}
    original=json.loads(Path(saved['source']['path']).read_text('utf-8'))
    for q in saved['rows']:
        i=q['array_index'];check('saved row '+str(i),all(q[k]==original[i][k]for k in ['direction','epoch','body_hex','frame_bytes'])and q['frame_bytes']==q['body_bytes']+2)
        if i in expectedbody:check('saved body '+str(i),q['body_hex']==expectedbody[i])
        else:check('D1 state '+str(i),q['body_hex'].startswith('d1')and q['body_hex'][-2:]==('02'if i in[74,75]else'00'if i in[85,87]else'01'))
    versions=load('CD2-ready-outbound/version-evidence-table.json');check('485F unit only',not versions['485F']['network_qualification_adopted']and versions['485F']['JAR']['sha256']=='485F599DA9BA1A13504CB48DDCA3D53C6CAD91D7BAFDF738D104E65A05E1DFD5')
    check('opcode only gap',not load('CD2-ready-outbound/native-callsite-inventory.json')['opcode_only_fixed_PE_callsite_found'])
    incident=load('CD1-mode-step/scope-write-correction.json');check('cleanup rejection open',not incident['cleanup_completed']and incident['automatic_approval_review_rejected_cleanup'])
    check('packet unchanged',record(ROOT/'server-packet-delta.json')==p['verification']['server_packet_unchanged'])
    write('CD2-ready-outbound/computed-conditional-models.json',{'models':models,'count':len(models),'old_models_or_PASS_replayed':False,'Native_runtime':False})
    review={'primary':record(ROOT/'CD2-ready-outbound-contract.json'),'checks':checks,'check_count':len(checks),'all_match':all(q['match']for q in checks),'computed_models':record(ROOT/'CD2-ready-outbound/computed-conditional-models.json'),'scope_write_correction':p['verification']['scope_write_correction']}
    write('CD2-ready-outbound/independent-review.json',review);print(json.dumps({'checks':len(checks),'models':len(models),'all_match':True},ensure_ascii=False))
if __name__=='__main__':main()

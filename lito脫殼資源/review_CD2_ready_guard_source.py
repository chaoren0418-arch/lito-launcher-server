"""Independent Batch30 raw/operand/source qualifications; no execution/replay."""
import sys
sys.dont_write_bytecode=True
from audit import *
def load(n):return json.loads((ROOT/n).read_text('utf-8'))
def main():
    p=load('CD2-ready-guard-source-contract.json');r=load('CD2-ready-guard-source/ranges.json');checks=[];models=[]
    def check(name,result):
        checks.append({'check':name,'match':bool(result)});assert result,name
    for q in p['sources']+list(p['existing_evidence'].values())+p['outputs']:check('pin '+q['path'],record(q['path'])==q)
    for q in p['prior_primary_pins']:
        a=record(q['path']);check('prior '+q['path'],a['bytes']==q['bytes']and a['sha256']==q['expected_sha256'])
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);ins={}
    for q in r['new_ranges']+r['necessary_reused_raw']:
        va=int(q['va'],16)
        if 'output'in q:
            raw=Path(q['output']['path']).read_bytes();check('output '+q['va'],record(q['output']['path'])==q['output'])
        else:
            check('reuse pin '+q['va'],record(q['source_pin']['path'])==q['source_pin']and record(q['source_index']['path'])==q['source_index'])
            full=Path(q['source_pin']['path']).read_bytes();o=q['source_offset'];raw=full[o:o+q['bytes']];check('reuse slice '+q['va'],sha(raw)==q['slice_sha256'])
        check('PE bytes '+q['va'],raw==pe.get_data(va-0x400000,q['bytes'])and len(raw)==q['bytes'])
        if q['kind']=='code':
            decoded=[{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in cs.disasm(raw,va)]
            check('aligned '+q['va'],decoded==q['instructions']and sum(len(bytes.fromhex(i['raw_hex']))for i in decoded)==len(raw));ins.update({int(i['va'],16):i for i in decoded})
    for q in r['instruction_assertions']:check('operand '+q['va'],(ins[int(q['va'],16)]['mnemonic'],ins[int(q['va'],16)]['operands'])==(q['expected_mnemonic'],q['expected_operands']))
    nav=load('CD2-ready-guard-source/direct-source-xrefs.json');lines=ASM.read_text('utf-8',errors='replace').splitlines()
    for name,rows in nav['symbol_rows'].items():
        pat=re.compile(r'\b'+name+r'\b',re.I);actual=[(i+1,line)for i,line in enumerate(lines)if name.lower()in line.lower()and pat.search(line)and not line.strip().startswith(';')]
        check('exact xref inventory '+name,actual==[(q['line'],q['text'])for q in rows])
        for q in rows:check('navigation line '+str(q['line']),lines[q['line']-1]==q['text'])
        check('xref count '+name,sum(nav['counts'][name].values())==len(rows))
    check('one flag write only',nav['counts']['byte_64AF9D']['direct_write']==1 and nav['counts']['byte_64AF9D']['direct_read']==11)
    check('four selector address args',nav['counts']['dword_8316B0']['address_argument']==4 and nav['counts']['dword_8316B0']['direct_write']==0)
    # Independent finite leaf interpreter: two-byte inline strcmp loop and SBB return.
    literal=pe.get_data(0x62D25C-0x400000,11)
    def compare_leaf(data):
        offset=0
        for step in range(32):
            for part in [0,1]:
                a=data[offset+part];b=literal[offset+part]
                if a!=b:return -1 if a<b else 1
                if a==0:return 0
            offset+=2
        raise AssertionError('unterminated model')
    for data,equal in [(b'lteventsvr\0',True),(b'Lteventsvr\0',False),(b'lteventsvrX\0',False),(b'ltevent\0',False),(b'\0',False),(b'lteventsvr\0ignored',True)]:
        cmp=compare_leaf(data)
        for before in [0,1]:
            after=1 if cmp==0 else before
            check('flag leaf '+data.hex()+'/'+str(before),(cmp==0)==equal and after==(1 if equal else before))
            models.append({'kind':'inline_literal_flag_leaf','input_hex':data.hex(),'before_BYTE':before,'comparison':cmp,'after_BYTE':after,'qualification':'conditional validinputmemory; tokenizer/earlierindirectcallee execution not assumed'})
    for flag in [0,1,255]:
        for decoded in [0,1,None]:
            decision='direct_D201'if flag==0 or decoded==0 else'memory_source_open'if decoded is None else'notice_unknown_458BB0'
            check('ready guard '+str(flag)+'/'+str(decoded),decision==('direct_D201'if flag==0 else'direct_D201'if decoded==0 else'memory_source_open'if decoded is None else'notice_unknown_458BB0'))
            models.append({'kind':'ready_guard_conditional','flag_BYTE':flag,'selector_decoded_u32':decoded,'decision':decision,'selector_read_required':flag!=0,'owner_mode_ready_authority':False,'qualification':'guard leaf only; no Native execution or memory/delivery grant'})
    for field in [0,1,7,255]:
        args=[0]+([1]if field else[])
        check('record field '+str(field),args==([0]if field==0 else[0,1]))
        models.append({'kind':'recordBYTE3E_constant_args','field_BYTE':field,'setter_args_u32':args,'separate_64AFBF_BYTE':int(field==7)if field else'branch skipped','actual_setter_completion_or_value':'unknown','qualification':'literalrequest sequence only, no recordmeaning/owner/UI'} )
    # New evidence must stop at unknown targets, with no captured callee body.
    unknown_targets={0x47EC50,0x4C3CC0,0x4C3D20,0x4F10E0,0x494120,0x49A510,0x4C3CA0,0x773DC650}
    for q in r['new_ranges']:check('no new unknownbody '+q['va'],int(q['va'],16)not in unknown_targets)
    check('fixed flag snapshot zero',pe.get_data(0x64AF9D-0x400000,1)==b'\0')
    check('fixed selector snapshot zero invalidstorage',pe.get_data(0x8316B0-0x400000,12)==b'\0'*12 and not p['source_contract']['protected_selector']['fixed_snapshot_decodable'])
    check('ASM is different initialization',p['source_contract']['protected_selector']['ASM_definition_only']['encoded']=='B104FA84'and not p['source_contract']['protected_selector']['ASM_definition_only']['fixed_PE_equal'])
    versions=load('CD2-ready-guard-source/version-evidence-supplement.json');net=json.loads(Path(p['existing_evidence']['settings_485F_network_report']['path']).read_text('utf-8'));new=versions['Batch30_485F_accepted_supplement']
    check('485F report acceptance',new['report']['sha256']=='8C1776AA503098E09B42EFE8CA45E07E651DD4570A07CB6A8994006421CCEF41'and new['JAR']==net['actual_JAR'])
    check('485F exact result adoption',new['settings_result']==net['settings_result']and new['findings']==net['findings'][:4])
    check('485F limited WAITING settings only',new['settings_result']['match_phase']=='WAITING'and not new['settings_result']['battle_started']and not p['verification']['485F_ready_COUNTDOWN_WORLD_GUI_qualification'])
    check('Batch29 immutable',record(ROOT/'CD2-ready-outbound-contract.json')==versions['Batch29_frozen']['primary'])
    incident=load('CD1-mode-step/scope-write-correction.json');check('cleanup rejection remains open',not incident['cleanup_completed']and incident['automatic_approval_review_rejected_cleanup'])
    check('packet unchanged',record(ROOT/'server-packet-delta.json')==p['verification']['server_packet_unchanged'])
    check('finite counts',(len(r['new_ranges']),len(r['necessary_reused_raw']),len(r['instruction_assertions']),len(models))==(9,9,60,25))
    write('CD2-ready-guard-source/computed-source-conditional-models.json',{'models':models,'count':len(models),'Native_execution':False,'oldmodels_or_PASS_replayed':False})
    review={'primary':record(ROOT/'CD2-ready-guard-source-contract.json'),'checks':checks,'check_count':len(checks),'all_match':all(q['match']for q in checks),'decoded_instruction_rows':len(ins),'models':record(ROOT/'CD2-ready-guard-source/computed-source-conditional-models.json'),'model_count':len(models),'scope_write_correction':p['verification']['scope_write_correction']}
    write('CD2-ready-guard-source/independent-review.json',review);print(json.dumps({'checks':len(checks),'decoded_instruction_rows':len(ins),'models':len(models),'all_match':True},ensure_ascii=False))
if __name__=='__main__':main()

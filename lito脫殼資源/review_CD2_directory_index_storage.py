"""New finite counter/query and mapping review; no old PASS or live actions."""
import sys
sys.dont_write_bytecode=True
from audit import *
def load(n):return json.loads((ROOT/n).read_text('utf-8'))
def main():
    p=load('CD2-directory-index-storage-contract.json');r=load('CD2-directory-index-storage/ranges.json');checks=[];models=[]
    def check(n,v):checks.append({'check':n,'match':bool(v)});assert v,n
    for q in p['sources']+list(p['existing_evidence'].values())+p['outputs']:
        a=record(q['path']);check('pin '+q['path'],all(a[k]==q[k]for k in ['path','bytes','sha256']))
    for q in p['prior_primary_pins']:
        a=record(q['path']);check('prior '+q['path'],a['bytes']==q['bytes']and a['sha256']==q['expected_sha256'])
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);ins={}
    for q in r['new_ranges']+r['necessary_reused_raw']:
        va=int(q['va'],16)
        if 'output'in q:raw=Path(q['output']['path']).read_bytes();check('rawfile '+q['va'],record(q['output']['path'])==q['output'])
        else:
            check('reusepin '+q['va'],record(q['source_pin']['path'])==q['source_pin']and record(q['source_index']['path'])==q['source_index'])
            raw=Path(q['source_pin']['path']).read_bytes()[q['source_offset']:q['source_offset']+q['bytes']];check('slicehash '+q['va'],sha(raw)==q['slice_sha256'])
        check('fixedPE '+q['va'],raw==pe.get_data(va-0x400000,q['bytes'])and len(raw)==q['bytes'])
        if q['kind']=='code':
            rows=[{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in cs.disasm(raw,va)]
            check('aligned '+q['va'],rows==q['instructions']and sum(len(bytes.fromhex(i['raw_hex']))for i in rows)==len(raw));ins.update({int(i['va'],16):i for i in rows})
    for q in r['instruction_assertions']:check('operand '+q['va'],(ins[int(q['va'],16)]['mnemonic'],ins[int(q['va'],16)]['operands'])==(q['expected_mnemonic'],q['expected_operands']))
    nav=load('CD2-directory-index-storage/direct-counter-xrefs.json');lines=ASM.read_text('utf-8',errors='replace').splitlines()
    for term,rows in nav['literal_queries'].items():check('namedxref '+term,rows==[{'line':i+1,'text':s}for i,s in enumerate(lines)if term.lower()in s.lower()and not s.strip().startswith(';')])
    def signed(v):return v if v<0x80000000 else v-0x100000000
    for count in [1,4,8]:
        cursor=0;trace=[]
        while cursor<count:
            old=cursor;cursor+=1;done=cursor>=count
            trace.append({'row_index':old,'record_VA':f'{0x62FAA0+old*180:08X}','after_increment':cursor,'completion_clear_both':done})
        check('stable counter sequence '+str(count),len(trace)==count and trace[-1]['completion_clear_both'])
        models.append({'kind':'conditional_stable_global_counter_sequence','count':count,'trace':trace,'after_completion_count_cursor':[0,0],'assumptions':'allprivate/externalhelpers returned;globalvalues stable acrosscalls;notactualNative completion/GUI/capacity proof'})
    for idx,count in [(-2,1),(0,1),(8,8),(0,-1)]:
        gate=idx<count
        check('signedgate '+str(idx)+'/'+str(count),gate==({(-2,1):True,(0,1):True,(8,8):False,(0,-1):False}[(idx,count)]))
        models.append({'kind':'CB_signed_gate','index_signed':idx,'count_signed':count,'branch_to_row':gate,'address_u32_hex':f'{(0x62FAA0+(idx&0xFFFFFFFF)*180)&0xFFFFFFFF:08X}'if gate else None,'valid_owned_array_proven':False,'qualification':'numericbranch only;negative indexexample hypothetical/noNative memory access'})
    for afterhelper in [0,5]:
        nxt=afterhelper+1;check('reread '+str(afterhelper),nxt==(1 if afterhelper==0 else 6))
        models.append({'kind':'posthelper_global_reread','row_used_index':0,'hypothetical_cursor_after_unknown_helper':afterhelper,'count_after_helper':8,'next_global_cursor':nxt,'same_original_index_assumed':afterhelper==0,'qualification':'illustrateswhyatomicfrozenindexcannotbeproved;noactualhelpermutation asserted'})
    for result in [-1,-2,0,7,8]:
        native_reject=result==-1;modern_own_bound=0<=result<8
        check('querydomain '+str(result),native_reject==(result==-1))
        models.append({'kind':'selected_return_branch','hypothetical_EAX_signed':result,'caller_minus1_reject':native_reject,'address_if_not_rejected':f'{(0x62FAA0+(result&0xFFFFFFFF)*180)&0xFFFFFFFF:08X}'if not native_reject else None,'proposed_Modern_owned_8row_index_bound':modern_own_bound,'qualification':'EAXdomainunknown;Modernboundisadaptation andalso requirescompletecurrentgeneration,notNativeproof'})
    for q in p['contract']['storage']['raw_endpoint_examples_for_server_policy_8']:
        i=q['ordinal'];check('mapping '+str(i),q['start']==mapping(pe,0x62FAA0+i*180)and q['end']==mapping(pe,0x62FAA0+(i+1)*180-1)and q['raw_backed_endpoints']and not q['runtime_array_capacity_or_validity'])
    check('endpoints not capacity',p['contract']['storage']['raw_endpoint_examples_for_server_policy_8'][-1]['end']['va']=='0063003F'and not p['verification']['server_8row_policy_reduction_requested'])
    check('counter snapshot zero only',pe.get_data(0x76079C-0x400000,8)==b'\0'*8)
    check('unknownhelper no body',not any(int(q['va'],16)in [0x494120,0x46EA30,0x4D6970,0x49A510]for q in r['new_ranges']))
    concerns=[q['concern']for q in p['Modern_safe_adaptation']['rows']];check('six adaptation concerns',concerns==['count decode','row consistency','completion','selected row','epoch invalidation','ready intent'])
    check('adaptation not implementation','notimplemented/testedModern'in p['Modern_safe_adaptation']['qualification'])
    incident=load('CD1-mode-step/scope-write-correction.json');check('cleanup rejection remainsopen',not incident['cleanup_completed']and incident['automatic_approval_review_rejected_cleanup'])
    check('packet unchanged',record(ROOT/'server-packet-delta.json')==p['verification']['server_packet_unchanged'])
    check('finite counts',(len(r['new_ranges']),len(r['necessary_reused_raw']),len(r['instruction_assertions']),len(models))==(1,8,35,14))
    write('CD2-directory-index-storage/computed-counter-and-query-models.json',{'models':models,'count':len(models),'oldmodels_or_PASS_replayed':False,'Native_GUI_runtime_actions':0})
    out={'primary':record(ROOT/'CD2-directory-index-storage-contract.json'),'check_count':len(checks),'checks':checks,'all_match':all(q['match']for q in checks),'decoded_instruction_rows':len(ins),'model_count':len(models),'models':record(ROOT/'CD2-directory-index-storage/computed-counter-and-query-models.json'),'scope_write_correction':p['verification']['scope_write_correction']}
    write('CD2-directory-index-storage/independent-review.json',out);print(json.dumps({'checks':len(checks),'decoded_instruction_rows':len(ins),'models':len(models),'all_match':True},ensure_ascii=False))
if __name__=='__main__':main()

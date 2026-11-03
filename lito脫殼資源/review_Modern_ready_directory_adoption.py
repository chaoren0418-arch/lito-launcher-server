"""Finite own-capture structural review only: no Rust tests, imports or build."""
import sys
sys.dont_write_bytecode=True
from audit import ROOT, record, write, stamp, Path, json
def main():
    primary=ROOT/'Modern-ready-directory-adoption-contract.json';p=json.loads(primary.read_text('utf-8'));checks=[]
    def check(name,condition):
        checks.append({'name':name,'match':bool(condition)})
        assert condition,name
    for q in p['outputs']:
        check('own output '+Path(q['path']).name,Path(q['path']).resolve().is_relative_to(ROOT) and record(Path(q['path']))==q)
    for r in p['volatile_source_reads']:
        j=json.loads(Path(r['owned_capture']['path']).read_text('utf-8'))
        check('stable read metadata '+Path(r['before']['path']).name,j['source_before']==r['before'] and j['source_after']==r['after'] and j['captured_content_sha256']==r['captured_content_sha256'] and r['stable_during_read']==(r['before']==r['after'] and r['before']['sha256']==r['captured_content_sha256']))
        check('runtime/test/import not performed '+Path(r['before']['path']).name,j.get('runtime_or_test_execution',False)==False and j.get('script_executed_or_imported',False)==False)
    anchors=0
    for r in p['findings']:
        check('finding boundaries '+r['id'],r['status'] in ['implemented','concrete_gap','product_decision_or_new_evidence'] and all(r[k] for k in ['trigger','current_source_behavior','expected_refusal_or_retention','qualification_limit']) and r['tests_run']==False)
        for a in r['anchors']:
            j=json.loads(Path(a['owned_capture']['path']).read_text('utf-8'));lines={x['line']:x['text'] for x in j['implementation_lines']}
            check(r['id']+' '+a['function']+':'+str(a['start_line']),record(Path(a['owned_capture']['path']))==a['owned_capture'] and j['stable_during_read'] and a['source']==j['source_before'] and a['required_start_token'] in lines[a['start_line']] and a['end_line']>=a['start_line'] and all(i in lines for i in range(a['start_line'],a['end_line']+1)))
            anchors+=1
    for q in p['prior_primary_pins']:
        check('prior pin '+Path(q['path']).name,record(Path(q['path']))['sha256']==q['expected_sha256'] and record(Path(q['path']))['bytes']==q['bytes'])
    for q in p['sources']+list(p['existing_evidence'].values()):
        check('accepted immutable evidence '+Path(q['path']).name,record(Path(q['path']))=={k:q[k] for k in ['path','bytes','sha256']})
    v=p['verification'];rows=p['findings'];tests=[t for r in rows for t in r['existing_test_name_evidence']]
    check('classification counts',v['status_counts']=={s:sum(r['status']==s for r in rows) for s in v['status_counts']}=={'implemented':8,'concrete_gap':2,'product_decision_or_new_evidence':3})
    check('26 per-file reads;14 implementation files;3 existing tests',v['source_read_snapshots']==26 and v['implementation_files']==14 and v['stable_during_read']==26 and v['tentative_unadopted']==0 and len(tests)==3)
    check('test names exact',['count_or_multirow_cannot_supply_singleton_alias','complete_full_frame_required_once','failed_partial_or_changed_context_cannot_complete']==[t['name'] for t in tests])
    check('34 prior primary pins',len(p['prior_primary_pins'])==34==v['prior_primary_pins_preserved'])
    check('all action/newwire counters zero',all(v[k]==0 for k in ['new_code_ranges','new_data_ranges','necessary_reused_raw_ranges','instruction_assertions','new_capture_frames','new_frame_rows_adopted','new_wire_differences','new_opcodes','wire_corrections','Modern_code_edits','tests_builds_compiles_or_runtime_runs','external_helper_imports','unknown_callee_bodies_expanded']))
    check('packet unchanged',record(ROOT/'server-packet-delta.json')==v['server_packet_unchanged'])
    check('scope incident unchanged/open',record(ROOT/'CD1-mode-step/scope-write-correction.json')==v['scope_write_correction'] and v['cache_cleanup_completed']==False)
    check('485F ready not promoted',p['server_version_status']['ready_comparison_485F_active_unaccepted'] and not p['server_version_status']['adoption_of_new_ready_comparison'] and p['server_version_status']['Modern_R525_active_not_interrupted'])
    check('minimum task exactly M09+M10','M09/M10' in p['minimal_adoption_order']['steps'][2]['action'])
    result={'updated_at':stamp(),'primary':record(primary),'all_match':True,'check_count':len(checks),'anchor_count':anchors,'checks':checks,'scope':'Only new owned capture structure, exact source tokens/line links, classifications and immutable accepted pins. No Modern test/build/run. External mutable Modern files deliberately not reread or required to remain frozen.','runtime_acceptance':False}
    write('Modern-ready-directory-adoption/finite-static-review.json',result)
    print(json.dumps({k:result[k] for k in ['primary','all_match','check_count','anchor_count']},ensure_ascii=False))
if __name__=='__main__':main()

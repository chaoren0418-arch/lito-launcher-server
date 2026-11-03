"""Validate the owned proposal and add workflow fields without replacing history."""
import sys
sys.dont_write_bytecode=True
from workflow_v2_queue import ROOT, TASK_ID, load, pin, write, stamp
from pathlib import Path
import json
import subprocess


def main():
    validator=Path(r'C:\Users\CHAOREN\.codex\skills\lito-supervisor\scripts\supervise.py')
    queue_path=ROOT/'工作佇列.json'
    before=pin(queue_path)
    # This publication validation is the only schema check of the final file;
    # the earlier direct invocation checked the just-created proposal once.
    command=[sys.executable,'-B',str(validator),'validate','--queue',str(queue_path)]
    completed=subprocess.run(command,capture_output=True,text=True,encoding='utf-8',check=True)
    official=json.loads(completed.stdout)
    assert official=={'valid':True,'errors':[],'task_count':12}
    q=load(queue_path)
    ids={t['id'] for t in q['items']}
    assert q['version']==2 and q['owner']=='bin_resources'
    assert q['ready_count']==0 and q['execution_state']=='waiting'
    assert q['implementation_selected'] is None and q['overall_product_complete'] is False
    assert len(ids)==12 and all(t['owner']=='bin_resources' and t['id'].startswith('bin_resources-') for t in q['items'])
    assert all(t['state']=='blocked' and t['blockers'] and t['gates'] for t in q['items'])
    assert all(t['dependencies']==[] or set(t['dependencies'])<=ids for t in q['items'])
    assert all(all(g['state']=='missing' for g in t['gates']) for t in q['items'])
    for t in q['items']:
        assert isinstance(t['priority'],int) and t['handoff_to']
        assert Path(t['exact_deliverable']['path']).is_relative_to(ROOT)
        assert t['next_deliverable'].startswith(t['exact_deliverable']['path'])
        for b in t['blockers']:
            assert all(b.get(k) for k in ['reason','supplier','missing_input','resume_when','consumer'])
        assert all(e['path'] and e['sha256'] and len(e['sha256'])==64 for e in t['evidence'])
    assert pin(queue_path)==before
    wire=load(ROOT/'workflow-v2/wire-semantic-review.json')
    assert not wire['notification_requested'] and wire['new_opcodes']==wire['corrected_wire_contracts']==0
    assert wire['baseline']==pin(ROOT/'server-packet-delta.json')
    assert q['retained_boundaries']['scope_write_incident']['sha256']=='B51ECB6CC2AD37DA279E03F71B92C6B75DA52F00027737A6AB8AC2A02C97DFE4'
    write('workflow-v2/queue-validation.json',{
        'validated_at':stamp(),'queue':before,'validator':pin(validator),'command':command,
        'official_result':official,'returncode':completed.returncode,
        'targeted_checks':{'twelve_existing_categories_preserved':True,'all_task_ids_namespaced':True,
                           'internal_dependencies_exist':True,'all_external_gates_explicitly_missing':True,
                           'all_blockers_have_supplier_input_trigger_consumer':True,
                           'all_outputs_stay_under_owned_root':True,'ready_count_zero':True,
                           'no_unblocked_implementation_invented':True,'no_new_wire_or_renotification':True},
        'acceptance_scope':'Queue schema and executable contract consistency only, not product/GUI acceptance',
        'old_source_matrix_tests_replayed':False,'old_artifact_bodies_reaudited':False})
    progress_path=ROOT/'監製進度.json'
    progress=load(progress_path)
    previous=dict(progress)
    assert not any(k.startswith('workflow_') for k in progress), 'Preserve an existing workflow handoff instead of resetting it'
    handoff={
        'proposal_state':'review','execution_state':'waiting','queue':before,
        'validation':pin(ROOT/'workflow-v2/queue-validation.json'),
        'wire_review':pin(ROOT/'workflow-v2/wire-semantic-review.json'),
        'delivery_path':str(ROOT/'workflow-v2/delivery.json'),
        'ready_count':0,'blocked_count':12,'implementation_selected':None,
        'overall_product_complete':False,'next_task_id_if_unblocked':q['next_task_id_if_unblocked'],
        'minimum_external_unblock':q['minimum_external_unblock'],
        'handoff_to':['supervisor'],'server_notification_requested':False,
        'wait_reason':'All existing remaining product tasks need qualified external inputs; one-time workflow conversion awaits review. No stopped branch resumed.'}
    progress.update(workflow_queue_path=str(queue_path),workflow_task_id=TASK_ID,
                    workflow_task_state='waiting',workflow_handoff=handoff,updated_at=stamp())
    assert all(progress[k]==v for k,v in previous.items() if k!='updated_at')
    write('監製進度.json',progress)
    assert all(load(progress_path)[k]==v for k,v in previous.items() if k!='updated_at')
    names=['工作佇列.json','workflow_v2_queue.py','publish_workflow_v2.py',
           'workflow-v2/wire-semantic-review.json','workflow-v2/queue-validation.json','監製進度.json']
    write('workflow-v2/delivery.json',{
        'updated_at':stamp(),'workflow_task_id':TASK_ID,'proposal_state':'review','execution_state':'waiting',
        'files':[pin(ROOT/n) for n in names],'all_current_files_match':True,
        'legacy_delivery_verification':'Previous artifact-manifest/delivery-verification retained unchanged; this finite workflow delta has a separate receipt, no old hashes/matrices rerun',
        'changed_legacy_files':['監製進度.json: only required workflow fields and updated_at added'],
        'queue_ready_count':0,'overall_product_complete':False,
        'stop':'Wait for the accepted member request or another named external gate; do not repeatedly convert/validate the unchanged queue'})
    print(json.dumps({'queue':before,'validation':pin(ROOT/'workflow-v2/queue-validation.json'),
                      'delivery':pin(ROOT/'workflow-v2/delivery.json'),'workflow_task_state':'waiting',
                      'proposal_state':'review','ready_count':0,'next_task_id_if_unblocked':q['next_task_id_if_unblocked']},ensure_ascii=True))


if __name__=='__main__':
    main()

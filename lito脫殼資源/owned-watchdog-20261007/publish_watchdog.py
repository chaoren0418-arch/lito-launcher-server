"""Finite publication and workflow metadata validation; no fixture reruns."""
import sys
sys.dont_write_bytecode=True
import json
import marshal
from pathlib import Path
from datetime import datetime,timezone,timedelta
import subprocess
import hashlib
from owned_process import HERE,digest,save
ROOT=HERE.parent;TASK='bin_resources-owned-process-watchdog-20261007'
def pin(p):return {'path':str(p),'bytes':p.stat().st_size,'sha256':digest(p)}
def main():
    first=json.loads((HERE/'new-real-process-tests.json').read_text('utf-8'))
    recheck=json.loads((HERE/'affected-case-recheck.json').read_text('utf-8'))
    assert first['status']=='FAIL' and recheck['status']=='PASS'
    matrix={t['name']:{**t,'execution_generation':'initial','module_source_archive':pin(HERE/'attempt1-owned_process.py.txt')}
            for t in first['new_real_process_cases']}
    for t in recheck['checks']:
        matrix[t['name']]={**t,'execution_generation':'affected-case-recheck','module_source':recheck['module_at_execution']}
    assert len(matrix)==9 and all(t['passed'] for t in matrix.values())
    assert recheck['module_at_execution']==pin(HERE/'owned_process.py')
    prior=json.loads((HERE/'immutable-prior-adapter-pins.json').read_text('utf-8'))
    assert all(pin(Path(p['path']))==p for p in prior['pins'])
    assert pin(Path(prior['supervisor_assessment']['path']))==prior['supervisor_assessment']
    source_files=sorted(HERE.glob('*.py'))
    compiled=[]
    for p in source_files:
        source=p.read_text('utf-8');code=compile(source,str(p),'exec')
        compiled.append({'source':pin(p),'in_memory_compilation':'PASS',
               'code_object_sha256':hashlib.sha256(marshal.dumps(code)).hexdigest().upper(),
               'code_executed_by_compilation':False,'pyc_written':False})
    compile_receipt={'schema':'owned-python-compilation-v1','compiler_runtime':sys.version,
               'runtime_executable':pin(Path(sys.executable)),'method':'Python built-in compile in memory; no package/compiler installation, no pyc',
               'sources':compiled,'status':'PASS'}
    save(HERE/'compile-receipt-final.json',compile_receipt)
    identity_checks=[];all_identities={};runtimes={}
    modes={'normal':'normal','hang':'hang','spawned_child':'tree','disk_poll':'disk','controller_exception':'tree'}
    for name,t in matrix.items():
        receipt_path=Path(t['receipt']['path']);assert pin(receipt_path)==t['receipt']
        d=json.loads(receipt_path.read_text('utf-8'));runtimes[name]=d
        if name in modes:
            assert d['owned_job_empty'] and all(m['exit']['signaled'] for m in d['post_exit_members'])
            event=json.loads((receipt_path.parent/(modes[name]+'-start.json')).read_text('utf-8'))
            expected=d['primary_identity'];actual=event['identity']
            passed=all(actual[k]==expected[k] for k in ['pid','creation_filetime_100ns','image_path'])
            assert passed and event['source_sha256']==digest(HERE/'fixture_helper.py')
            identity_checks.append({'case':name,'self_start_matches_controller_handle':passed})
            if name in ['spawned_child','controller_exception']:
                child=json.loads((receipt_path.parent/'leaf-start.json').read_text('utf-8'))
                assert any(all(child['identity'][k]==m[k] for k in ['pid','creation_filetime_100ns','image_path']) for m in d['post_exit_members'])
            for m in d['post_exit_members']:all_identities[(m['pid'],m['creation_filetime_100ns'])]=m
        elif name=='stdout_stderr_flood':
            assert all(c['observed_bytes']==2097152 and c['retained_bytes']==8192 and c['EOF_observed'] for c in d['capture'].values())
            for m in d['post_exit_members']:all_identities[(m['pid'],m['creation_filetime_100ns'])]=m
        elif name=='controller_exit':
            assert d['passed'] and d['all_job_members_exited_before_outer_cleanup'] and not d['fallback_outer_cleanup_needed']
            for m in d['observed_members_before_outer_cleanup']:all_identities[(m['pid'],m['creation_filetime_100ns'])]=m
        elif name=='failed_containment':
            assert not d['process_created'] and not d['child_resumed'] and d['failure']['winerror']!=0
        else:
            assert d['normal_exit_code_zero'] and not d['sentinel_force_cleanup_needed']
            for m in d['final_members']:all_identities[(m['pid'],m['creation_filetime_100ns'])]=m
    # Initial unaffected flood assertion already included all handles signaled.
    assert all(m['exit']['signaled'] for m in all_identities.values())
    save(HERE/'post-exit-audit-final.json',{'status':'PASS','identity_checks':identity_checks,
         'exact_retained_handle_exit_observations':list(all_identities.values()),
         'queried_processes':'Only handles of this task\'s freshly created primaries or members verified in its unnamed jobs',
         'global_PID_enumeration':False,'PID_name_based_kills':False,'process_memory_read_or_attach':False,
         'initial_failed_immediate_exit_audits_preserved':True,'old_process_retry_or_cleanup':False})
    primary=ROOT/'Owned-Process-Watchdog-20261007.json';now=datetime.now(timezone(timedelta(hours=8))).isoformat()
    disk=runtimes['disk_poll'];flood=runtimes['stdout_stderr_flood'];crash=runtimes['controller_exit']
    report={'schema':'owned-process-watchdog-review-v1','task_id':TASK,'created_at':now,
       'proposal_state':'review','component_test_outcome':'NEW_REQUIRED_CASES_HAVE_PASS_EVIDENCE',
       'full_IDA_export_environment_qualified':False,'overall_product_complete':False,
       'sources_and_compilation':{'sources':[pin(p) for p in source_files],'compile_receipt':pin(HERE/'compile-receipt-final.json'),
               'Python_version_tested':first['runtime_python'],'Windows_version_tested':first['windows_version'],
               'runtime_executable':first['runtime_executable'],'external_compiler_or_package_installation':False},
       'task_authority':pin(HERE/'authorized-task-at-read.json'),
       'runtime_evidence':{'first_execution':pin(HERE/'new-real-process-tests.json'),'first_exit_code':1,
               'first_execution_status':'FAIL; 4/9 passed, retained unchanged',
               'affected_case_recheck':pin(HERE/'affected-case-recheck.json'),'recheck_exit_code':0,
               'recheck_checks':'6/6 PASS; five affected cases plus a fresh independent sentinel',
               'selected_distinct_case_matrix':list(matrix.values()),
               'commands':[first['test_command'],recheck['command']],
               'test_all_pass_on_one_source_version_claimed':False,
               'version_reason':'Exit-wait and directory entry-count correction affected cleanup audit cases. Capture, prelaunch invalid-limit refusal and abrupt-exit primitive retained their passing first-execution evidence and source archive; they were not rerun.'},
       'implementation_failures':[pin(HERE/'implementation-failure-01.json'),pin(HERE/'implementation-failure-02.json'),pin(HERE/'implementation-failure-03.json')],
       'ownership_containment':{'mechanism':'Unnamed Windows Job Object; atomic JOB_LIST in CreateProcessW STARTUPINFOEX',
              'before_execution':'CREATE_SUSPENDED + job membership and executable-path checks + receipt identity, then ResumeThread',
              'flags':8200,'kill_on_last_handle_close':True,'breakaway_flags':False,'job_handle_inherited':False,
              'handle_inheritance':'Only explicit NUL/stdout/stderr handles via HANDLE_LIST',
              'process_count_limit':16,'hidden_windows':True,
              'cleanup':'TerminateJobObject on owned retained handle; no PID scans or process-name termination',
              'cleanup_fallback_on_controller_exit':'Kernel closes non-inherited sole inner job handle',
              'cleanup_audit':'Bounded wait for both empty job list and all observed retained process handles signaled',
              'OS_console_hosts':'Receipts include OS-created conhost.exe members only after explicit membership verification in the owned job.'},
       'deadline':{'clock':'time.monotonic','external_to_child':True,'watchdog_thread_independent_of_capture_and_disk_scan':True,
              'watchdog_requested_wakeup_seconds':.01,'cleanup_wait_seconds':2,
              'test_timeout_seconds':2,'hang_receipt':matrix['hang']['receipt'],'tree_receipt':matrix['spawned_child']['receipt'],
              'hang_trigger_overshoot_seconds':runtimes['hang']['trigger'][0]['deadline_overshoot_seconds'],
              'tree_trigger_overshoot_seconds':runtimes['spawned_child']['trigger'][0]['deadline_overshoot_seconds'],
              'hang_total_elapsed_seconds_including_cleanup':runtimes['hang']['elapsed_seconds'],
              'tree_total_elapsed_seconds_including_cleanup':runtimes['spawned_child']['elapsed_seconds'],
              'limit':'OS scheduling, API calls and teardown can add latency; no real-time zero-overshoot guarantee or CPU-time-only limit is claimed.'},
       'bounded_capture':{'cap_bytes_per_stream':8192,'always_drain_after_cap':True,'flood_fixture_bytes_per_stream':2097152,
              'actual_stdout':flood['capture']['stdout'],'actual_stderr':flood['capture']['stderr'],
              'memory_buffer':'One fixed 8192-byte read buffer and at most cap bytes retained per stream',
              'reader_join_limit_seconds_per_stream':1},
       'abrupt_controller_exit':{'controller_exit_code':73,'checkpoint':crash['checkpoint'],
              'outer_job_handle_held_open_until_audit':True,'outer_cleanup_before_inner_exit':False,
              'all_inner_identities_matched_exited':True,'fallback_outer_cleanup_needed':False,
              'receipt':matrix['controller_exit']['receipt']},
       'containment_failure_test':{'real_API_rejection':'SetInformationJobObject with deliberately invalid size 1 before CreateProcessW',
              'winerror':runtimes['failed_containment']['failure']['winerror'],'process_created':False,'child_resumed':False,
              'receipt':matrix['failed_containment']['receipt'],'no_downgrade_or_fallback':True},
       'disk_monitor':{'requested_poll_seconds':.05,'actual_max_sample_gap_seconds':disk['disk_monitor']['max_observed_sample_gap_seconds'],
              'logical_size_threshold_bytes':65536,'observed_bytes_at_detection':disk['trigger'][0]['observed_bytes'],
              'observed_detection_overshoot_bytes':disk['trigger'][0]['observed_overshoot_bytes'],
              'final_bytes_before_runtime_receipt':disk['disk_monitor']['final_bytes_before_receipt'],
              'receipt_and_controller_metadata_written_after_monitor_not_quota_enforced':True,
              'scan_entry_cap':256,'reparse_points_refused':True,'hard_disk_quota':False,
              'precise_gap':'Polling cannot cap writes between checks, during scan/termination, after runtime exit or outside this job directory. Logical file length is measured, not physical allocated/compressed space. No accepted hard storage containment exists.'},
       'post_exit_audit':pin(HERE/'post-exit-audit-final.json'),
       'sentinel':{'target_job_membership':False,'identity_preserved_and_running_during_other_cases':True,
              'normal_shutdown_exit_code':0,'forced_cleanup_needed':False,
              'initial_receipt':next(t['receipt'] for t in first['new_real_process_cases'] if t['name']=='sentinel_not_miskilled'),
              'recheck_receipt':matrix['sentinel_not_miskilled']['receipt']},
       'prior_adapter_preservation':{'baseline_receipt':pin(HERE/'immutable-prior-adapter-pins.json'),
              'immutable_files_verified':len(prior['pins']),'all_preserved':True,'old_PASS_or_IDA_qualification_replayed':False},
       'remaining_qualification_gaps':[
           'Hard disk containment/quota absent; polling is detection plus tree termination only.',
           'Plugin/egress/owned-write isolation is still unqualified; original IDA refusal remains sealed and active.',
           'This module is runtime-tested only with harmless owned Python fixtures; no IDA/native export execution was attempted.',
           'Job objects cover associated CreateProcess descendants, not hostile privileged/broker-created external work; not a security sandbox.'],
       'scope':{'write_root':str(ROOT),'runtime_tree_root':str(HERE),'external_ASCII_runtime_created':False,
              'IDA_launches':0,'game_launches':0,'installation_plugins_global_config_or_security_settings_changed':False,
              'external_AI_or_network_payload_calls':False,'other_owner_processes_targeted':False,
              'private_backing_next_layer_and_old_automatic_review_stops_preserved':True,
              'shared_BIN_TW_LHZ_provider_SQL_GUI_MCP_memory_touched':False,
              'old_cache_cleanup_retried':False},
       'documentation':[{'title':'Microsoft Job Objects','url':'https://learn.microsoft.com/en-us/windows/win32/procthread/job-objects'},
              {'title':'Microsoft atomic job creation','url':'https://devblogs.microsoft.com/oldnewthing/20230209-00/?p=107812/'},
              {'title':'UpdateProcThreadAttribute','url':'https://learn.microsoft.com/en-us/windows/win32/api/processthreadsapi/nf-processthreadsapi-updateprocthreadattribute'}],
       'handoff':'Review this process-control component and the explicitly absent disk/security controls separately; it cannot qualify the IDA environment or unlock any LITO analysis.'}
    # Runtime writer intentionally permits HERE only. Publication has this one
    # explicitly requested, fixed BIN report path; do not widen runtime scope.
    assert primary.parent==ROOT and primary.name=='Owned-Process-Watchdog-20261007.json'
    with primary.open('xb') as f:f.write(json.dumps(report,ensure_ascii=False,indent=2).encode('utf-8'))
    ppath=ROOT/'監製進度.json';qpath=ROOT/'工作佇列.json'
    before={'progress':pin(ppath),'queue':pin(qpath)}
    p=json.loads(ppath.read_text('utf-8'));q=json.loads(qpath.read_text('utf-8'))
    assert p['workflow_task_id']==TASK and p['workflow_task_state']=='running'
    item=next(i for i in q['items'] if i['id']==TASK);item['state']='review'
    item['evidence']=[pin(primary),pin(HERE/'affected-case-recheck.json'),pin(HERE/'post-exit-audit-final.json')]
    item['remaining_qualification_gaps']=report['remaining_qualification_gaps']
    q.update(updated_at=now,proposal_state='review',execution_state='review',overall_product_complete=False,
             ready_count=sum(i['state']=='ready' for i in q['items']),blocked_count=sum(i['state']=='blocked' for i in q['items']))
    p.update(updated_at=now,phase='OWNED_PROCESS_WATCHDOG_COMPONENT_REVIEW_NO_HARD_DISK_OR_IDA_ISOLATION',
         current_task='自有程序樹watchdog交review；首輪FAIL封存、6項受影響重測PASS，硬磁碟配額與IDA隔離資格仍缺。',workflow_task_state='review',
         workflow_handoff={'proposal_state':'review','execution_state':'review','primary':pin(primary),
               'post_exit_audit':pin(HERE/'post-exit-audit-final.json'),'recheck':pin(HERE/'affected-case-recheck.json'),
               'delivery_path':str(HERE/'delivery.json'),'handoff_to':['supervisor'],
               'hard_disk_quota':False,'full_IDA_export_qualified':False,'IDA_launches':0,
               'remaining_qualification_gaps':report['remaining_qualification_gaps'],'server_notification_requested':False})
    p['owned_process_watchdog_summary']={'primary':pin(primary),'distinct_selected_cases':9,'affected_recheck_passed':6,
             'initial_failed_receipts_preserved':True,'known_retained_process_handles_signaled':len(all_identities),
             'hard_disk_quota':False,'IDA_environment_qualified':False,'old_adapter_immutable':True}
    artifacts=p.setdefault('actual_artifacts',[]);known={a['path'] for a in artifacts}
    for entry in [pin(primary),pin(HERE/'post-exit-audit-final.json'),pin(HERE/'compile-receipt-final.json'),pin(HERE/'affected-case-recheck.json'),*[pin(f) for f in source_files]]:
        if entry['path'] not in known:artifacts.append(entry);known.add(entry['path'])
    for path,value in [(ppath,p),(qpath,q)]:
        temporary=path.with_name(path.stem+'.watchdog-review.tmp.json')
        with temporary.open('xb') as f:f.write(json.dumps(value,ensure_ascii=False,indent=2).encode('utf-8'))
        temporary.replace(path)
    validator=Path(r'C:\Users\CHAOREN\.codex\skills\lito-supervisor\scripts\supervise.py')
    command=[sys.executable,'-B',str(validator),'validate','--queue',str(qpath)]
    validation=subprocess.run(command,cwd=str(HERE),capture_output=True,text=True,timeout=15)
    save(HERE/'queue-validation.json',{'command':command,'exit_code':validation.returncode,
             'stdout':validation.stdout,'stderr':validation.stderr,'queue_at_validation':pin(qpath),
             'metadata_only':True,'old_PASS_or_fixture_tests_replayed':False})
    assert validation.returncode==0
    assert all(pin(Path(x['path']))==x for x in prior['pins'])
    files=[pin(f) for f in sorted(HERE.rglob('*')) if f.is_file()]
    assert not any(f.suffix in ['.pyc','.idb','.i64'] for f in HERE.rglob('*'))
    save(HERE/'delivery.json',{'schema':'owned-watchdog-finite-delivery-v1','task_id':TASK,'proposal_state':'review',
         'primary':pin(primary),'owned_files':files,'progress_and_queue_before':before,
         'progress_and_queue_after':{'progress':pin(ppath),'queue':pin(qpath)},
         'queue_validation':pin(HERE/'queue-validation.json'),'immutable_prior_pins_preserved':33,
         'IDA_export_or_security_environment_qualified':False,'hard_disk_quota':False})
    print(json.dumps({'primary':pin(primary),'delivery':pin(HERE/'delivery.json'),'state':'review',
         'distinct_cases_with_PASS_evidence':9,'affected_recheck_PASS':6,
         'known_exact_identities_exited':len(all_identities),'queue_validation_exit_code':validation.returncode,
         'hard_disk_quota':False,'IDA_export_qualified':False},ensure_ascii=True))
if __name__=='__main__':main()

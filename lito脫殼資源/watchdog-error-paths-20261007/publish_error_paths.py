"""Publish the focused successor without executing prior tests or processes."""
import sys
sys.dont_write_bytecode=True
import difflib,hashlib,json,marshal,subprocess
from datetime import datetime,timezone,timedelta
from pathlib import Path
from owned_process import HERE,digest,save
ROOT=HERE.parent;TASK='bin_resources-watchdog-error-paths-20261007'
def pin(p):return {'path':str(p),'bytes':p.stat().st_size,'sha256':digest(p)}
def verified(p):return pin(Path(p['path']))==p
def main():
    tests=json.loads((HERE/'new-error-path-tests.json').read_text('utf-8'))
    assert tests['status']=='PASS' and len(tests['cases'])==11 and all(c['passed'] for c in tests['cases'])
    assert verified(tests['module_at_execution']) and verified(tests['fixture_at_execution']) and verified(tests['driver_at_execution'])
    baseline=json.loads((HERE/'immutable-baseline.json').read_text('utf-8'))
    assert len(baseline['pins'])==130 and all(verified(p) for p in baseline['pins'])
    task=json.loads((HERE/'task-at-read.json').read_text('utf-8'))
    assert verified(task['assessment'])
    old=ROOT/'owned-watchdog-20261007'/'owned_process.py';new=HERE/'owned_process.py'
    diff=''.join(difflib.unified_diff(old.read_text('utf-8').splitlines(True),new.read_text('utf-8').splitlines(True),
                         fromfile=str(old),tofile=str(new)))
    with (HERE/'source.diff').open('xb') as f:f.write(diff.encode('utf-8'))
    compile_checks=[]
    for p in sorted(HERE.glob('*.py')):
        code=compile(p.read_text('utf-8'),str(p),'exec')
        compile_checks.append({'source':pin(p),'in_memory_compile':'PASS',
                    'code_object_sha256':hashlib.sha256(marshal.dumps(code)).hexdigest().upper()})
    save(HERE/'compile-receipt.json',{'status':'PASS','runtime':sys.version,'executable':pin(Path(sys.executable)),
         'method':'compile in memory only; no execution by compile, no pyc','sources':compile_checks})
    matrix=[];all_exits=[]
    for c in tests['cases']:
        if 'runtime' not in c:continue
        assert verified(c['runtime']) and verified(c['independent_audit'])
        r=json.loads(Path(c['runtime']['path']).read_text('utf-8'))
        observer=json.loads(Path(c['independent_audit']['path']).read_text('utf-8'))
        assert observer['no_cleanup_performed_by_observer'] and all(p['exit']['signaled'] for p in observer['members'])
        assert r['receipt_persistence']['status']=='WRITTEN' and r['process_created'] is True
        if c['name'] in ['constructor_wait_unknown','cleanup_wait_unknown']:assert r['owned_job_empty'] is None
        matrix.append({'case':c['name'],'status':r['status'],'process_created':r['process_created'],
            'creation_call_returned_success':r.get('creation_call_returned_success'),
            'child_resumed':r['child_resumed'],'owned_job_empty':r['owned_job_empty'],
            'primary_failure':r['failure'],'secondary_errors':r['observation_failures'],
            'fault_events':r['fault_events'],'cleanup_exit_audit':r['cleanup'].get('exit_audit'),
            'close':r['close'],'final_disk_scan':r['disk_monitor']['final_scan'],
            'runtime':c['runtime'],'independent_audit':c['independent_audit']})
        all_exits.extend(observer['members'])
    sentinel=tests['cases'][-1];assert verified(sentinel['receipt'])
    s=json.loads(Path(sentinel['receipt']['path']).read_text('utf-8'))
    assert s['primary_natural_exit']['exit_code']==0 and s['primary_not_miskilled']
    assert all(x['same_identity'] and x['still_running'] for x in s['checks'])
    save(HERE/'finite-verification.json',{'status':'PASS','new_error_path_checks':11,
        'exact_independent_retained_handle_exits':all_exits,'observed_exit_count':len(all_exits),
        'candidate_unknown_outcomes_preserved':['constructor_wait_unknown','cleanup_wait_unknown'],
        'immutable_baseline_pins_verified':130,'runtime_receipts_persisted_for_all_10_error_cases':True,
        'old_tests_or_new_passes_reexecuted_by_publication':False,'IDA_launches':0,'game_launches':0})
    source_lines=new.read_text('utf-8').splitlines()
    anchors={key:next(i+1 for i,s in enumerate(source_lines) if pattern in s) for key,pattern in {
        'authoritative_create_state':'self.process_created=True;self.creation_call_success=True',
        'constructor_error':'except BaseException as error:',
        'raw_wait_audit':'def bounded_exit_audit(',
        'unconditional_cleanup':'def cleanup(self):',
        'guarded_capture':'def finish_captures(self):',
        'no_object_inference':'elif ctor_evidence is not None:',
        'guarded_final_disk_scan':"final_scan=attempt('final_disk_scan'",
        'durable_runtime_writer':"persisted=attempt('runtime_receipt_write'"}.items()}
    now=datetime.now(timezone(timedelta(hours=8))).isoformat()
    primary=ROOT/'Owned-Watchdog-Error-Paths-20261007.json'
    report={'schema':'owned-watchdog-error-path-successor-review-v1','task_id':TASK,'created_at':now,
      'proposal_state':'review','component_result':'11_NEW_AFFECTED_CHECKS_PASS','overall_product_complete':False,
      'corrected_module':pin(new),'sources':[pin(p) for p in sorted(HERE.glob('*.py'))],
      'source_diff':pin(HERE/'source.diff'),'compile_receipt':pin(HERE/'compile-receipt.json'),
      'module_source_anchors':anchors,'task_at_read':pin(HERE/'task-at-read.json'),'supervisor_blocked_assessment':task['assessment'],
      'immutable_baseline':{'manifest':pin(HERE/'immutable-baseline.json'),'files':130,'all_unchanged':True,
               'prior_watchdog_and_adapter_test_execution':False},
      'corrections':[
         {'finding':'constructor_post_create_false_empty',
          'behavior':'Record CreateProcess success and retain primary handle immediately; fallible membership/identity/snapshot/capture setup cannot gate cleanup. Constructor packages actual cleanup/close/capture evidence in ConstructorFailure for caller.',
          'cleanup':'Each snapshot is guarded, TerminateJobObject targets only the retained owned job, bounded raw job-query and retained-handle waits run before unconditional close. Close results are recorded separately.',
          'unknown_policy':'Caller does not infer created=false, resumed=false or empty=true from a missing process object. Missing attached evidence remains null/unknown; failed wait observation remains null even when independent test observer confirms exit.'},
         {'finding':'final_disk_scan_receipt_loss',
          'behavior':'Final scan, cleanup snapshots/audit and per-stream capture finalization are guarded individually. Original failure remains separate from secondary observation errors. Unknown disk size/overshoot are null. Runtime receipt is written after all observation phases.',
          'durability':'Fresh runtime.json writer uses exclusive create, bounded JSON bytes, flush and fsync. An actual receipt-storage failure is returned as an explicit persistence error with in-memory evidence, never reported written.',
          'limit':'Complete physical storage failure cannot guarantee a durable file. Receipt-write failure branch is source-handled but not fault-tested in this task; no external fallback path is used.'}],
      'focused_new_tests':{'summary':pin(HERE/'new-error-path-tests.json'),'actual_command':tests['command'],'actual_exit_code':0,
          'total':11,'passed':11,'runtime_python':tests['runtime_python'],'runtime_executable':tests['runtime_executable'],
          'error_case_matrix':matrix,'sentinel':sentinel,'finite_verification':pin(HERE/'finite-verification.json'),
          'old_normal_hang_flood_controller_exit_PASS_matrix_replayed':False},
      'fault_evidence_policy':{'membership_identity_snapshot_wait_and_I/O_injections':'Explicit FaultPlan errors immediately before candidate observations; no claim that the Windows API or filesystem actually failed.',
          'real_noninjected_scan_refusal':'A new helper writes only 300 one-byte owned files; the real 256-entry scanner rejects the directory in both polling and final scan.',
          'cleanup_actions':'Job termination and CloseHandle are real operations and are not replaced by injected successes.',
          'independent_exit_observer':'Holds noninherited duplicate process handles only after verifying the new job membership. No job handle duplicate, process-name scan, PID-based kill or cleanup by observer.',
          'unknowns_preserved':'Independent exit evidence is separately pinned and never rewrites candidate null outcomes.'},
      'preserved_controls':{'atomic_job_list':True,'create_suspended_until_membership_and_identity_checks_pass':True,
          'kill_on_job_close':True,'noninherited_sole_unnamed_job_handle':True,'no_breakaway':True,'active_process_limit':16,
          'capture_cap_bytes_per_stream_in_tests':8192,'read_buffer_bytes':8192,'capture_always_drains':True,
          'monotonic_wall_deadline_independent_of_child_and_disk_scan':True,'deadline_thread_wakeup_seconds':.01,
          'bounded_cleanup_wait_seconds':2,'reader_join_limit_seconds':1,'disk_poll_seconds':.05,
          'monitor_samples_retained_cap':512,'observed_retained_member_cap':64,'receipt_byte_cap':262144,
          'hidden_windows':True,'fixture_only_installed_Python_scope':True,
          'evidence_limit':'Inherited successful component evidence is preserved by source diff and immutable pins; unchanged old paths were not requalified by replay.'},
      'known_gaps':{'hard_disk_quota':False,'security_sandbox':False,'plugin_egress_owned_write_isolation_qualified':False,
          'IDA_export_qualified':False,'physical_receipt_storage_failure_can_be_guaranteed_written':False,
          'unknown_state_is_success':False,'real_time_zero_deadline_overshoot_guarantee':False},
      'scope':{'write_root':str(ROOT),'new_successor':str(HERE),'old_sources_and_receipts_modified':False,
          'IDA_launches':0,'game_launches':0,'external_ASCII_runtime_created':False,'process_memory_or_attach':False,
          'global_config_plugins_firewall_security_settings_changed':False,'other_owner_processes_targeted':False,
          'provider_SQL_live_claim_GUI_MCP_shared_BIN_TW_LHZ_private_backing_actions':False,
          'external_AI_calls':False,'prior_automatic_review_refusals_and_unsafe_plugin_guard_preserved':True,
          'old_rejected_cache_access_or_cleanup_retry':False},
      'documentation':[{'title':'Microsoft DuplicateHandle','url':'https://learn.microsoft.com/en-us/windows/win32/api/handleapi/nf-handleapi-duplicatehandle'},
          {'title':'Microsoft TerminateJobObject','url':'https://learn.microsoft.com/en-us/windows/win32/api/jobapi2/nf-jobapi2-terminatejobobject'}],
      'handoff':'Review this successor against the two findings. Passing injected error paths does not qualify IDA execution, hard quota or a security sandbox.'}
    assert primary.parent==ROOT and primary.name=='Owned-Watchdog-Error-Paths-20261007.json'
    with primary.open('xb') as f:f.write(json.dumps(report,ensure_ascii=False,indent=2).encode('utf-8'))
    progress_path=ROOT/'監製進度.json';queue_path=ROOT/'工作佇列.json'
    before={'progress':pin(progress_path),'queue':pin(queue_path)}
    p=json.loads(progress_path.read_text('utf-8'));q=json.loads(queue_path.read_text('utf-8'))
    assert p['workflow_task_id']==TASK and p['workflow_task_state']=='running'
    current=next(i for i in q['items'] if i['id']==TASK)
    current.update(state='review',evidence=[pin(primary),pin(HERE/'source.diff'),pin(HERE/'new-error-path-tests.json'),pin(HERE/'finite-verification.json')])
    old_task=next(i for i in q['items'] if i['id']=='bin_resources-owned-process-watchdog-20261007')
    old_task['state']='blocked';old_task['supervisor_assessment']=task['assessment']
    old_task['blockers']=[{'reason':'Original delivered baseline remains blocked by the two reviewed error-path defects.',
          'supplier':'bin_resources successor / supervisor review','missing_input':'Accepted review of the separately delivered corrected successor.',
          'resume_when':'Supervisor evaluates the new successor; preserve old faulty module immutable.','consumer':'owned watchdog component / future exporter'}]
    q.update(updated_at=now,proposal_state='review',execution_state='review',overall_product_complete=False,
             ready_count=sum(t['state']=='ready' for t in q['items']),blocked_count=sum(t['state']=='blocked' for t in q['items']))
    p.update(updated_at=now,phase='WATCHDOG_ERROR_PATH_SUCCESSOR_REVIEW_UNKNOWN_OUTCOMES_PRESERVED',
         current_task='constructor與final observation錯誤路徑successor交review；11項新檢查PASS，unknown保留，舊watchdog/adapter未改。',
         workflow_task_state='review',workflow_handoff={'proposal_state':'review','primary':pin(primary),
             'source_diff':pin(HERE/'source.diff'),'verification':pin(HERE/'finite-verification.json'),'tests':pin(HERE/'new-error-path-tests.json'),
             'delivery_path':str(HERE/'delivery.json'),'handoff_to':['supervisor'],'parent_assessment':task['assessment'],
             'hard_disk_quota':False,'IDA_export_qualified':False,'unknown_outcomes_preserved':True,'server_notification_requested':False})
    p['watchdog_error_paths_successor_summary']={'primary':pin(primary),'new_checks_passed':11,'independent_exact_handle_exits':len(all_exits),
             'candidate_owned_job_empty_null_cases':2,'old_immutable_pins_preserved':130,'full_IDA_export_or_sandbox_qualified':False}
    entries=p.setdefault('actual_artifacts',[]);known={x['path'] for x in entries}
    for item in [pin(primary),pin(HERE/'source.diff'),pin(HERE/'new-error-path-tests.json'),pin(HERE/'finite-verification.json'),pin(new)]:
        if item['path'] not in known:entries.append(item);known.add(item['path'])
    for path,value in [(progress_path,p),(queue_path,q)]:
        tmp=path.with_name(path.stem+'.error-paths-review.tmp.json')
        with tmp.open('xb') as f:f.write(json.dumps(value,ensure_ascii=False,indent=2).encode('utf-8'))
        tmp.replace(path)
    validator=Path(r'C:\Users\CHAOREN\.codex\skills\lito-supervisor\scripts\supervise.py')
    command=[sys.executable,'-B',str(validator),'validate','--queue',str(queue_path)]
    result=subprocess.run(command,cwd=str(HERE),capture_output=True,text=True,timeout=15)
    save(HERE/'queue-validation.json',{'command':command,'exit_code':result.returncode,'stdout':result.stdout,
             'stderr':result.stderr,'queue_at_validation':pin(queue_path),'metadata_validation_only':True,'fixture_or_old_PASS_replay':False})
    assert result.returncode==0
    assert all(verified(x) for x in baseline['pins']) and verified(tests['module_at_execution'])
    files=[pin(f) for f in sorted(HERE.rglob('*')) if f.is_file()]
    assert not any(f.suffix in ['.pyc','.idb','.i64'] for f in HERE.rglob('*'))
    save(HERE/'delivery.json',{'schema':'owned-watchdog-error-path-delivery-v1','task_id':TASK,'proposal_state':'review',
        'primary':pin(primary),'successor_files':files,'workflow_before':before,
        'workflow_after':{'progress':pin(progress_path),'queue':pin(queue_path)},'queue_validation':pin(HERE/'queue-validation.json'),
        'old_immutable_pins_preserved':130,'source_version_matches_all_new_error_path_tests':True,
        'candidate_unknowns_not_upgraded':True,'IDA_export_or_environment_qualified':False})
    print(json.dumps({'primary':pin(primary),'delivery':pin(HERE/'delivery.json'),'state':'review','new_checks_PASS':11,
          'candidate_unknown_cases':2,'old_immutable_pins_preserved':130,'queue_validation_exit_code':result.returncode},ensure_ascii=True))
if __name__=='__main__':main()

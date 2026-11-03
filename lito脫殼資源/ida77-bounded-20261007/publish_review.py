"""Publish only this finite tool task. Does not run IDA or any old matrix."""
import sys
sys.dont_write_bytecode=True
import ast
import json
from datetime import datetime, timezone, timedelta
from pathlib import Path
from ida77_request import sha, CAPS, DEFAULTS
HERE=Path(__file__).resolve().parent
ROOT=HERE.parent
INSTALL=Path(r'D:\IDA_Pro_7.7')
TASK='bin_resources-ida77-bounded-export-adapter-20261007'

def record(path):
    raw=path.read_bytes()
    return {'path':str(path),'bytes':len(raw),'sha256':sha(raw)}

def save(path,value):
    with path.open('xb') as f:
        f.write(json.dumps(value,ensure_ascii=False,indent=2).encode('utf-8'))

def main():
    tests=json.loads((HERE/'new-tests.json').read_text('utf-8'))
    assert tests['status']=='PASS' and tests['IDA_launches']==0 and tests['total']==15
    qualification=Path(r'D:\lineage開發\3-lito開發\監製工作流\ida77-offline-qualification-20261007.json')
    qpin=record(qualification)
    assert qpin['sha256']=='C3580C78F234C5648531652486A59F532D5BDA8807BEE35B2BFE4DD13668B346'
    audit_sources=[]
    selections=[('plugins/string_batch_analyzer_v2.py',[40,42,45,87,318,320,321,375,468,469,471,484]),
                ('plugins/keypatch.py',[123,1667,1736,1745]),
                ('plugins/plugins.cfg',[4,5,6])]
    for rel,lines in selections:
        p=INSTALL/rel
        pin=record(p)
        text=p.read_text('utf-8-sig',errors='replace').splitlines()
        audit_sources.append({'source':pin,'anchors':[{'line':n,'text':text[n-1]} for n in lines]})
    gate=json.loads((HERE/'receipts'/'positive_execute_gate.json').read_text('utf-8'))
    assert gate['status']=='STOPPED_UNSAFE_AUTOPLUGINS' and not gate['ida_started']
    audit={'schema':'ida77-readonly-environment-audit-v1','installation':str(INSTALL),
           'evidence_level':'Selected installed plugin source read; code was not imported or executed',
           'selected_sources':audit_sources,'idat_executable':record(INSTALL/'idat.exe'),
           'inventory':gate['environment'],
           'findings':[
             {'code':'TIMER_CAN_START_OPENAI_ANALYSIS','source':'plugins/string_batch_analyzer_v2.py',
              'lines':[45,468,471,318,321,375],
              'conclusion':'init registers a timer whose callback starts run_analysis; analysis constructs an OpenAI client and calls it. A 30s timeout is not an isolation certificate.'},
             {'code':'PLUGIN_OUTPUT_OUTSIDE_OWNER_ROOT','source':'plugins/string_batch_analyzer_v2.py','lines':[40,320],
              'conclusion':'Analysis creates/writes its fixed installation-side directory, outside BIN/resources ownership.'},
             {'code':'TERMINATION_CONFIG_WRITE','source':'plugins/keypatch.py','lines':[123,1736,1745],
              'conclusion':'Normal plugin termination can write keypatch.cfg under IDA user directory; no accepted owned-write redirection exists.'},
             {'code':'OMITTING_PLUGINS_CFG_IS_NOT_DISABLE','source':'plugins/plugins.cfg','lines':[4,5,6],
              'conclusion':'Installed comments explicitly describe automatic discovery/use of plugins absent from the config list.'}],
           'secrets_or_gepetto_config_read':False,'installation_modified_by_this_task':False,
           'global_configuration_or_plugins_installed':False,'profile_isolation_claimed':False,
           'all_plugin_behaviors_audited':False,
           'decision':'STOP_BEFORE_IDA_LAUNCH',
           'minimum_resume_input':'An accepted isolated plugin/egress/owned-write runtime profile plus an independently tested process-tree watchdog and disk budget. This requires a separate tool qualification; it does not authorize LITO re-analysis.'}
    save(HERE/'environment-audit.json',audit)
    now=datetime.now(timezone(timedelta(hours=8))).isoformat()
    sources=[record(HERE/n) for n in ['ida77_request.py','ida77_runner.py','ida77_export.py','prepare_and_test.py','publish_review.py','request.schema.json','result.schema.json']]
    report={'schema':'ida77-bounded-export-adapter-review-v1','task_id':TASK,'created_at':now,
            'proposal_state':'review','delivery_status':'PARTIAL_TOOL_DRAFT_EXECUTION_STOPPED',
            'execution_state':'blocked','qualified_for_IDA_export':False,
            'primary_outcome':'SHA/PE32/function range validation and fail-closed preflight delivered. Current IDA installation is refused before launch. IDAPython exporter is an unexecuted draft.',
            'scope':{'owner_write_root':str(ROOT),'synthetic_fixture_only':True,
                     'new_LITO_source_analysis':False,'game_or_fixture_execution':False,
                     'process_attach_or_real_game_inspection':False,
                     'installation_readonly':str(INSTALL),'writes_outside_owner_root_by_this_tool_route':0,
                     'supervisor_or_other_owner_writes':0,'reverse_skill_installer_start_watchdog_invoked':False,
                     'new_wire_or_server_SQL_provider_work':False,
                     'stopped_private_backing_and_next_layer_boundaries_preserved':True,
                     'STOP_boundaries':['42E550','42E6D0','42EA50','495520_constructor_and_unknown_owner'],
                     'Native_GUI_and_server_refused_actions_preserved':True},
            'sources_and_schemas':sources,
            'prior_ROOT_tool_qualification':{'pin':qpin,'decision':'accepted_component_only',
                 'historical_IDA_version':'7.7.22.0118','historical_Python_version':'3.8.10 64bit',
                 'historical_fixture_result':'disassembly + expected direct call + Hex-Rays pseudocode + exit 0',
                 'replayed':False,'isolation_certificate':False,
                 'limit':'Prior fixture functionality does not qualify auto-plugin egress/write safety.'},
            'fixture':{'source':tests['fixture_source'],'owned_copy':tests['fixture_owned_copy'],
                       'image_base':4194304,'allowlist':[{'rva':4096,'end_rva':4109,'va':4198400,'end_va':4198413},
                                                        {'rva':4112,'end_rva':4121,'va':4198416,'end_va':4198425}],
                       'synthetic_fixture_executions':0},
            'actual_new_test_evidence':record(HERE/'new-tests.json'),
            'actual_receipts':[c['receipt'] for c in tests['checks'] if 'receipt' in c],
            'actual_commands':{'test_driver_argv':[sys.executable,'-B',str(HERE/'prepare_and_test.py')],
                               'test_driver_exit_code':0,'runner_commands':tests['commands'],
                               'IDA_commands_executed':[],'IDA_launches':0},
            'required_case_results':{
               'positive_request_validation':'PASS',
               'positive_IDA_export':'NOT_RUN_UNSAFE_AUTOPLUGINS; not counted as an export PASS',
               'positive_execute_preflight':'STOPPED_UNSAFE_AUTOPLUGINS; exit 2, PID null, no DB',
               'wrong_SHA':'REFUSED/HASH_MISMATCH before IDA',
               'out_of_range':'REFUSED/OUT_OF_RANGE before IDA',
               'empty_allowlist':'REFUSED/EMPTY_ALLOWLIST before IDA',
               'missing_decompiler':'Controlled classification tests PASS; installed IDA availability not retested'},
            'environment_audit':record(HERE/'environment-audit.json'),
            'budgets':{'declared_caps':CAPS,'default_limits':DEFAULTS,
                       'validated_before_launch':['file <=16MiB','PE32 i386 image <=64MiB','SHA256','imagebase','1..8 half-open non-overlapping executable raw-backed function ranges','allowlist byte cap','budget values'],
                       'receipt_output_limit_enforced_bytes':262144,
                       'draft_exporter_limits':['cooperative time check','function/chunk/item/xref caps','pseudocode character cap','serialized output cap','loaded byte fingerprint checks'],
                       'external_hard_wall_timeout':'NOT_IMPLEMENTED_OR_TESTED; runner never launches IDA',
                       'max_job_disk_bytes':'DECLARED_NOT_ENFORCED; runner never creates a DB/job',
                       'native_autoanalysis_or_decompiler_call_duration_bound':'UNQUALIFIED',
                       'network_and_external_write_isolation':'UNQUALIFIED; no launch'},
            'export_policy':{'only_listed_functions':True,'xref_endpoints_metadata_only':True,
                             'incoming_xrefs':'direct references to listed function starts only',
                             'outgoing_xrefs':'direct references from listed items, excluding fallthrough',
                             'xref_target_function_body_traversal':False,
                             'row_provenance':['source_sha256','VA','RVA','IDA_version','evidence_level'],
                             'evidence_level':'synthetic_static_IDA_analysis_not_execution_or_GUI',
                             'IDA_API_and_HexRays_compatibility':'UNVERIFIED_DRAFT_NOT_RUN'},
            'fresh_database_policy':{'old_IDB_overwrite':False,'databases_created':0,
                       'implementation':'Launch route absent; accepted future runner must create a fresh owned job/DB using -c/-o and refuse existing outputs.',
                       'candidate_argv_template_not_executed':['D:/IDA_Pro_7.7/idat.exe','-A','-c','-L<fresh-owned-job>/ida.log','-S<owned-exporter>','-o<fresh-owned-job>/fixture.idb','<owned-pinned-fixture>'],
                       'template_is_not_an_approved_launch_command':True},
            'documentation_context':[{'title':'Hex-Rays environment variables (8.4; not installed 7.7 isolation proof)',
                         'url':'https://docs.hex-rays.com/8.4/user-guide/user-interface/menu-bar/windows/environment-variables'},
                        {'title':'Hex-Rays command switches (9.0sp1; not installed 7.7 isolation proof)',
                         'url':'https://docs.hex-rays.com/9.0sp1/user-guide/configuration/command-line-switches'}],
            'minimal_next_input':audit['minimum_resume_input'],
            'acceptance_limit':'Review the finite preflight/code handoff only. Do not mark the bounded IDA export tool or LITO analysis accepted/complete.',
            'scope_write_correction_retained':{'path':str(ROOT/'CD1-mode-step'/'scope-write-correction.json'),
                    'sha256':'B51ECB6CC2AD37DA279E03F71B92C6B75DA52F00027737A6AB8AC2A02C97DFE4',
                    'prior_automatic_cleanup_rejection':'blocked by policy','retry_or_external_cache_access':False}}
    primary=ROOT/'IDA77-Bounded-Export-Adapter-20261007.json'
    save(primary,report)
    # Finite publication validation, not another historical test run.
    all_pins=sources+[tests['fixture_owned_copy']]+report['actual_receipts']+[report['environment_audit'],report['actual_new_test_evidence']]
    assert all(record(Path(p['path']))==p for p in all_pins)
    assert all(record(Path(p['source']['path']))==p['source'] for p in audit_sources)
    assert record(qualification)==qpin
    assert not any(HERE.rglob('*.pyc')) and not any(HERE.rglob('*.idb')) and not any(HERE.rglob('*.i64'))
    for p in HERE.glob('*.py'):
        ast.parse(p.read_text('utf-8'),feature_version=(3,8))
    finite={'status':'PASS','task_id':TASK,'validation_level':'finite new artifact pins and syntax only',
            'new_preflight_test_checks':15,'artifact_pins_verified':len(all_pins),
            'selected_installation_sources_unchanged':3,'qualification_pin_unchanged':True,
            'IDA_export_acceptance':False,'old_ROOT_PASS_matrix_replayed':False,
            'new_DB_or_pyc_in_owned_tool_directory':False}
    save(HERE/'finite-delivery-verification.json',finite)
    progress_path=ROOT/'監製進度.json'
    before=record(progress_path)
    progress=json.loads(progress_path.read_text('utf-8-sig'))
    assert progress['workflow_task_id']==TASK and progress['workflow_task_state']=='running'
    progress['updated_at']=now
    progress['phase']='IDA77_TOOL_PREFLIGHT_REVIEW_EXPORT_STOPPED_UNSAFE_AUTOPLUGINS'
    progress['current_task']='IDA77工具前置驗證與停用匯出草稿已交review；15項新檢查PASS，實際IDA匯出／硬性watchdog未驗證，外掛隔離資格仍blocked。'
    progress['workflow_task_state']='review'
    progress['workflow_handoff']={'proposal_state':'review','execution_state':'blocked',
         'primary':record(primary),'verification':record(HERE/'finite-delivery-verification.json'),
         'new_tests':record(HERE/'new-tests.json'),'environment_audit':record(HERE/'environment-audit.json'),
         'delivery_path':str(HERE/'delivery.json'),'handoff_to':['supervisor'],
         'qualified_for_IDA_export':False,'IDA_launches':0,'new_LITO_analysis':False,
         'minimum_external_unblock':report['minimal_next_input'],'server_notification_requested':False}
    progress['IDA77_bounded_adapter_summary']={'primary':record(primary),'preflight_tests_passed':15,
         'actual_IDA_export':'NOT_RUN_UNSAFE_AUTOPLUGINS','IDA_launches':0,'qualified_for_export':False,
         'old_matrices_replayed':False,'cache_cleanup_retry':False,'new_private_backing_analysis':False}
    artifacts=progress.setdefault('actual_artifacts',[])
    existing={p['path'] for p in artifacts}
    for pin in [record(primary),record(HERE/'finite-delivery-verification.json'),*all_pins]:
        if pin['path'] not in existing:
            artifacts.append(pin);existing.add(pin['path'])
    temporary=ROOT/'監製進度.ida77.tmp.json'
    save(temporary,progress)
    temporary.replace(progress_path)
    files=[record(p) for p in sorted(HERE.rglob('*')) if p.is_file()]
    delivery={'schema':'ida77-finite-delivery-v1','task_id':TASK,'proposal_state':'review',
          'execution_state':'blocked','primary':record(primary),'owned_tool_files':files,
          'progress_before':before,'progress_after':record(progress_path),
          'validation':record(HERE/'finite-delivery-verification.json'),
          'actual_IDA_launches':0,'qualified_for_IDA_export':False,'old_seal_or_matrix_replayed':False,
          'external_runtime_or_installation_writes_by_this_tool_route':0,
          'scope_write_correction_retained':report['scope_write_correction_retained']}
    save(HERE/'delivery.json',delivery)
    print(json.dumps({'primary':record(primary),'delivery':record(HERE/'delivery.json'),
                      'verification':record(HERE/'finite-delivery-verification.json'),
                      'new_tests':record(HERE/'new-tests.json'),'workflow_task_state':'review',
                      'execution_state':'blocked','qualified_for_IDA_export':False},ensure_ascii=False))

if __name__=='__main__':main()

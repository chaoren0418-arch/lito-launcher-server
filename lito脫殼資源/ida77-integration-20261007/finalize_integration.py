import sys
sys.dont_write_bytecode=True
import json,hashlib,ast
from pathlib import Path
from datetime import datetime,timezone,timedelta
HERE=Path(__file__).resolve().parent;ROOT=HERE.parent
TASK='bin_resources-ida77-isolated-export-integration-20261007'
PIN='AD639F124F953C4992605E8BAC98E282E6B9F96C71EABFCF5B8B1101DD223C4B'
def read(p):return json.loads(p.read_text('utf-8-sig'))
def pin(p):
    b=p.read_bytes();return {'path':str(p),'bytes':len(b),'sha256':hashlib.sha256(b).hexdigest().upper()}
def save(p,v):
    with p.open('xb') as f:f.write(json.dumps(v,ensure_ascii=False,indent=2).encode('utf-8'))
def replace(p,v):
    t=p.with_name(p.stem+'.integration-final.tmp.json');save(t,v);t.replace(p)
now=datetime.now(timezone(timedelta(hours=8))).isoformat()
native=read(HERE/'runs/probe/native-launch.json');runtime=read(HERE/'runs/probe/runtime.json')
diagnostic=read(HERE/'platform-diagnostic.json');profile=read(HERE/'private-profile-candidate.json')
assert native['created'] is False and native['resumed'] is False and native['error']['winerror']==2
assert runtime['owned_job_empty'] is True and runtime['failure'] is None
assert runtime['cleanup']['exit_audit']['value']['owned_job_empty'] is True
assert diagnostic['registration_state_unchanged'] and all(r['matching'] for r in diagnostic['all_twelve_pinned_copies'])
assert pin(HERE/'owned_process.py')['sha256']==PIN
# Preserve provenance of an ambiguous planned-boundary label before correcting it.
old_qualification=pin(HERE/'isolation-qualification.json')
qualification=read(HERE/'isolation-qualification.json')
qualification['boundary']='Requested empty-capability LPAC isolation; no native probe created, therefore no token, file-denial or network-denial observation exists.'
qualification['native_process_created']=False;qualification['native_probe_executed']=False
qualification['missing_probe_result_is_expected']=True
qualification['planned_test_not_observed']=['actual LPAC child token','private write success','outside read/write denial','localhost connection denial']
qualification['previous_receipt_pin_before_boundary_wording_correction']=old_qualification
replace(HERE/'isolation-qualification.json',qualification)
syntax=[]
for p in sorted(HERE.glob('*.py'))+list((HERE/'private-runtime').glob('*.py')):
    ast.parse(p.read_text('utf-8-sig'),filename=str(p));syntax.append(pin(p))
save(HERE/'syntax-verification.json',{'method':'ast.parse only; no imports, bytecode generation or old test-suite execution','sources':syntax,'pass':True})
save(HERE/'prelaunch-authoring-failures.json',{'failures':[
    {'source':'private_profile.py','stage':'Python parse','error':'raw string ended with a backslash','native_process_created':False,'fixed':True},
    {'source':'run_integration.py','stage':'Python argument binding','error':'incorrect poll_seconds keyword','accepted_watchdog_entered':False,'fixed':True},
    {'source':'run_integration.py','stage':'Python argument binding','error':'incorrect poll_interval keyword','accepted_watchdog_entered':False,'fixed':True}],
    'actual_native_probe_launch_attempt_count':1,'ordinary_or_unisolated_native_launch_attempt_count':0})
baseline=read(HERE/'immutable-baseline.json');checked=[]
for old in baseline['pins']:
    current=pin(Path(old['path']));assert current==old;checked.append(current)
install=read(HERE/'dependency-inventory.json');core=[]
for old in install['core_binaries']:
    current=pin(Path(old['path']));assert current['sha256']==old['sha256'] and current['bytes']==old['bytes'];core.append(current)
fixture=ROOT/'ida77-bounded-20261007/fixtures/fixture.exe';fixturepin=pin(fixture)
assert fixturepin['sha256']=='5E8BD11307A3F9F6E323062E9C7FC7F9FF536471196613A931D3F94A2F8FF1E3'
save(HERE/'finite-verification.json',{'old_immutable_pin_count':len(checked),'old_immutable_pins_unchanged':True,
    'accepted_watchdog_byte_identical':True,'accepted_watchdog_pin':pin(HERE/'owned_process.py'),
    'original_selected_core_pins_unchanged':core,'twelve_original_Python_dependency_pins_unchanged':True,
    'fixture':fixturepin,'fixture_executed':False,'old_15_preflight_or_11_watchdog_suite_replayed':False})
blocker={'id':'private-unregistered-LPAC-native-createprocess-winerror2',
    'stage':'CreateProcessW_private_LPAC_suspended','actual_winerror':2,'winerror_name':'ERROR_FILE_NOT_FOUND',
    'native_probe_process_created':False,'native_probe_resumed':False,'IDA_process_created':False,
    'request':'New derived SID, zero capabilities, ALL_APPLICATION_PACKAGES_OPT_OUT, explicit stdio handles, suspended native launch under accepted owned Job',
    'direct_evidence':[pin(HERE/'runs/probe/native-launch.json'),pin(HERE/'platform-diagnostic.json')],
    'known':'Private Python executable exists as PE x64; all 12 copied dependencies match. Attribute-list setup succeeded. Native CreateProcessW rejected the LPAC launch. Own SID folder lookup separately returns 0x80070002; selected registration/package entries stay absent.',
    'unknown':'Internal missing object responsible for CreateProcessW error 2 is not identified. No actual child token was obtained. Do not claim that missing profile registration is a proven sole cause.',
    'restriction':'A standard registered AppContainer profile uses user/global package and registry state outside the authorized BIN directory. Registration, existing profile reuse, global ACL/firewall edits and unisolated fallback were not attempted.',
    'resume_when':'Supervisor supplies or authorizes a qualified isolation path whose persistent writes remain within the task ownership boundary; continue from this failed native creation stage without replaying accepted watchdog suites.'}
sources=[pin(HERE/n) for n in ['owned_process.py','appcontainer_launcher.py','private_profile.py','run_integration.py','platform_diagnostic.py','ida77_export_draft.py','ida77_request.py']]
report={'schema':'ida77-isolated-export-integration-v1','task_id':TASK,'owner':'bin_resources','created_at':now,
    'decision':'BLOCKED_PLATFORM_NATIVE_LPAC_CREATION','supervisor_review_required':True,
    'summary':'已實際接上 byte-pinned accepted watchdog 並嘗試未註冊私人 SID 的 LPAC probe；native CreateProcessW 回傳 2，未建立 probe／IDA。隔離資格未通過，未偽造反組譯或 pseudocode。',
    'task_at_read':pin(HERE/'task-at-read.json'),'accepted_component':{'source_sha256':PIN,'reuse':'Exact accepted bytes copied and imported, no framework modification','sources':sources},
    'private_runtime':{'candidate':pin(HERE/'private-profile-candidate.json'),'probe_only':True,'private_directory_and_12_dependencies_created':True,
        'private_IDA_allowlist_built':False,'IDA_loaded_modules_observed':False,'IDA_startup_plugin_configuration_observed':False,
        'IDA_configuration_readiness':'Not qualified; stopped before copying or starting IDA. No claim based on IDAUSR/plugins.cfg.',
        'original_IDA_or_Python_entrypoint_executed':False,'original_auto_AI_Keypatch_or_unknown_plugin_executed':False,
        'AppContainer_registered':False,'existing_security_settings_modified':False,'firewall_modified':False,
        'requested_new_private_directory_SD_at_creation':profile['root_acl_at_creation'],
        'readonly_diagnosis':pin(HERE/'platform-diagnostic.json'),'isolation_qualification':pin(HERE/'isolation-qualification.json')},
    'blocker':blocker,
    'actual_new_execution':{'trusted_launcher_run_count':1,'native_probe_launch_attempt_count':1,'native_probe_run_count':0,'IDA_run_count':0,
        'runtime_receipt':pin(HERE/'runs/probe/runtime.json'),'native_creation_receipt':pin(HERE/'runs/probe/native-launch.json'),
        'launcher_identity':runtime['primary_identity'],'launcher_exit_code':81,
        'owned_job_member_cleanup':runtime['cleanup'],'all_captures_EOF':all(v['value']['EOF_observed'] for v in runtime['capture'].values()),
        'deadline_seconds':runtime['timeout_seconds'],'elapsed_seconds':runtime['elapsed_seconds'],'deadline_triggered':False,
        'disk_observation':runtime['disk_monitor'],'disk_hard_quota':False,
        'disk_scope':'All private probe output, IDAUSR/TEMP/HOME candidate paths lie in the accepted monitored job. Stable private runtime copies are outside monitored job and were never native-executed.',
        'egress':'No native probe or IDA ran; own trusted launcher opened one local listener, no upload/client payload and no external AI call. Network denial remains untested.',
        'outside_private_sentinel_unchanged':qualification['sentinel_before_sha256']==qualification['sentinel_after_sha256'],
        'sentinel_result_is_not_file_isolation_proof':True},
    'required_fixture':{'source':fixturepin,'image_base':'0x00400000','functions':[{'va_start':'0x00401000','va_end_exclusive':'0x0040100D','rva_start':'0x1000','rva_end_exclusive':'0x100D'},
        {'va_start':'0x00401010','va_end_exclusive':'0x00401019','rva_start':'0x1010','rva_end_exclusive':'0x1019'}],
        'fixture_executed':False,'loaded_byte_fingerprints':None,'real_IDA_disassembly':None,'real_IDA_direct_xrefs':None,'real_IDA_pseudocode':None,
        'exporter_integration_status':'Draft bytes preserved; not connected/executed because native isolation failed first.'},
    'new_consumer_required_static_LITO_request':{'proposal':'none','count':0,'reason':'Current finite task supplies only inert fixture/environment scope, no independent unfulfilled consumer question with an authorized source SHA/range. S4F/sub_4AEFD0 d+s and actor-owner deliveries are not duplicated.',
        'LITO_target_analysis_performed':False,'preserved_stops':['42E550','42E6D0','42EA50','050104CD next layer','495520 constructor','unknown owner','private backing/provider/SQL/shared BIN/TW/LHZ']},
    'verification':{'finite_receipt':pin(HERE/'finite-verification.json'),'syntax_only':pin(HERE/'syntax-verification.json'),
        'prelaunch_authoring_failures':pin(HERE/'prelaunch-authoring-failures.json'),'old_immutable_pins_verified':len(checked),
        'no_old_PASS_suite_replay':True,'no_original_unsafe_route_or_alternative_analyzer':True},
    'primary_documentation':[
        {'url':'https://learn.microsoft.com/en-us/windows/win32/secauthz/implementing-an-appcontainer','use':'Profile creation writes a user package location; capability/LPAC launch API reference, not proof of successful isolation.'},
        {'url':'https://learn.microsoft.com/en-us/windows/win32/api/userenv/nf-userenv-getappcontainerfolderpath','use':'Read-only folder path API signature; observed HRESULT retained separately.'}]}
reportpath=ROOT/'IDA77-Isolated-Export-Integration-20261007.json';save(reportpath,report)
pp=ROOT/'監製進度.json';qp=ROOT/'工作佇列.json';before={'progress':pin(pp),'queue':pin(qp)};p=read(pp);q=read(qp)
p.update(updated_at=now,workflow_task_id=TASK,workflow_task_state='review',
    current_task='IDA77隔離整合已交付精確platform blocker待ROOT驗收：私人未註冊LPAC native建立失敗winerror=2；沒有IDA/export輸出。',
    workflow_handoff={'proposal_state':'review','execution_result':'blocked_platform','delivery':pin(reportpath),'handoff_to':['supervisor'],'next_static_LITO_request':'none'})
item=next(x for x in q['items'] if x['id']==TASK);item.update(state='review',delivery=pin(reportpath),execution_result='blocked_platform',blockers=[{
    'reason':'Actual CreateProcessW LPAC probe failure ERROR_FILE_NOT_FOUND (2); no native child token or IDA run.',
    'supplier':'supervisor','missing_input':'Qualified compliant isolation path with no persistent writes outside BIN ownership',
    'resume_when':blocker['resume_when'],'consumer':['bin_resources','supervisor']}])
q.update(updated_at=now,workflow_task_id=TASK,proposal_state='review',execution_state='blocked_platform',implementation_selected=TASK,
    ready_count=sum(x['state']=='ready' for x in q['items']),blocked_count=sum(x['state']=='blocked' for x in q['items']),
    review_count=sum(x['state']=='review' for x in q['items']))
for path,v in [(pp,p),(qp,q)]:replace(path,v)
save(HERE/'workflow-update-receipt.json',{'before':before,'after':{'progress':pin(pp),'queue':pin(qp)},'task_state':'review','execution_result':'blocked_platform'})
print(json.dumps({'report':pin(reportpath),'old_pins_unchanged':len(checked),'IDA_run_count':0,'blocker_winerror':2},ensure_ascii=True))

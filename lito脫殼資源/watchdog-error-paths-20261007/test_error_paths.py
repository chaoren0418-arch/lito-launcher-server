"""Only new affected error paths, with independent retained-handle audits.

Observer duplicates process handles, never the job handle. It cannot keep the
containment job alive or terminate any process. It verifies exit independently
when the candidate's injected observation is unknown.
"""
import sys
sys.dont_write_bytecode=True
import ctypes as C
from ctypes import wintypes as W
import json,time
from pathlib import Path
from owned_process import HERE,api,ContainedProcess,run_owned,identity,exit_observation,InJob,Close,Wait,digest,save
Duplicate=api('DuplicateHandle',[W.HANDLE,W.HANDLE,W.HANDLE,C.POINTER(W.HANDLE),W.DWORD,W.BOOL,W.DWORD])
def pin(p):return {'path':str(p),'bytes':p.stat().st_size,'sha256':digest(p)}
class Observer:
    def __init__(self):self.handles={}
    def collect(self,p):
        for pid,v in p.members.items():
            if pid in self.handles:continue
            inside=W.BOOL();assert InJob(v['handle'],p.job,C.byref(inside)) and inside.value
            h=W.HANDLE();assert Duplicate(-1,v['handle'],-1,C.byref(h),0,False,2)
            self.handles[pid]={'handle':h.value,'identity':identity(h.value,pid),'verified_owned_job_member':True}
    def audit(self):
        end=time.monotonic()+2
        while time.monotonic()<end and not all(Wait(v['handle'],0)==0 for v in self.handles.values()):time.sleep(.01)
        return [{**v['identity'],'verified_owned_job_member':True,'noninherited_process_handle_only':True,
                 'exit':exit_observation(v['handle'])} for v in self.handles.values()]
    def close(self):
        for v in self.handles.values():Close(v['handle'])
def main():
    fixture=HERE/'error_fixture.py';cases=[];sentinel=None;sentinel_checks=[];failure=None
    sdir=HERE/'runs'/'sentinel';sdir.mkdir(parents=True)
    try:
        sentinel=ContainedProcess([sys.executable,'-B',str(fixture),'sentinel'],sdir)
        sentinel.resume();initial=sentinel.snapshots()[0]
        deadline=time.monotonic()+1
        while not (sdir/'sentinel-start.json').exists() and time.monotonic()<deadline:time.sleep(.01)
        assert (sdir/'sentinel-start.json').exists()
        def sentinel_check(case):
            now=sentinel.snapshots()[0]
            same=all(now[k]==initial[k] for k in ['pid','creation_filetime_100ns','image_path'])
            value={'case':case,'identity':now,'same_identity':same,'still_running':not now['exit']['signaled']}
            sentinel_checks.append(value);assert same and value['still_running']
        spec=[
          ('post_create_membership','hang',{'post_create_membership':'membership-query-failure'},'CONSTRUCTOR_FAILED'),
          ('post_create_identity','hang',{'post_create_identity':'creation-time/path-query-failure'},'CONSTRUCTOR_FAILED'),
          ('post_create_snapshot','hang',{'constructor_snapshot':'snapshot-failure','constructor_error_snapshot':'secondary-snapshot-failure'},'CONSTRUCTOR_FAILED'),
          ('post_create_capture_setup','hang',{'capture_setup':'setup-failure','constructor_error_snapshot':'exception-snapshot-failure'},'CONSTRUCTOR_FAILED'),
          ('constructor_wait_unknown','hang',{'post_create_identity':'identity-failure','cleanup_wait':'wait-observation-failure'},'CONSTRUCTOR_FAILED'),
          ('final_scan_entrycap','entrycap',{},'CONTROL_REFUSED'),
          ('poll_and_final_scan_io','hang',{'disk_poll_scan':'injected-I/O-refusal','final_disk_scan':'injected-I/O-refusal'},'CONTROLLER_EXCEPTION'),
          ('cleanup_wait_unknown','tree',{'cleanup_wait':'wait-observation-failure'},'TIMEOUT'),
          ('cleanup_snapshots','tree',{'cleanup_snapshot_before':'pre-snapshot-failure','cleanup_snapshot_after':'post-snapshot-failure'},'TIMEOUT'),
          ('capture_observation_failure','tree',{'capture_stdout':'injected-capture-write-I/O-refusal'},'TIMEOUT')]
        for name,mode,plan,expected in spec:
            sentinel_check(name+'_before');observer=Observer();directory=HERE/'runs'/name
            def created(p):
                observer.collect(p)
                b=W.BOOL();assert InJob(sentinel.pi.hProcess,p.job,C.byref(b)) and not b.value
            def started(p):
                end=time.monotonic()+1
                marker='entries-ready.json' if mode=='entrycap' else 'leaf-start.json' if mode=='tree' else 'hang-start.json'
                while time.monotonic()<end:
                    p.observe();observer.collect(p)
                    if (directory/marker).exists():return
                    time.sleep(.01)
                raise RuntimeError('New fixture did not reach its bounded marker')
            try:
                result=run_owned([sys.executable,'-B',str(fixture),mode],directory,timeout_seconds=1.5,
                      capture_bytes=8192,disk_poll_seconds=.05,fault_plan=plan,on_created=created,on_started=started)
                audit=observer.audit()
                runtime=directory/'runtime.json'
                durable=json.loads(runtime.read_text('utf-8'))
                passed=durable==result and result['status']==expected and result['process_created'] is True
                passed=passed and bool(audit) and all(v['exit']['signaled'] for v in audit)
                passed=passed and all(c['status']=='OBSERVED' for c in result['close'])
                if name.startswith('post_create') or name=='constructor_wait_unknown':
                    passed=passed and result['child_resumed'] is False and not (directory/'hang-start.json').exists()
                else:passed=passed and result['child_resumed'] is True
                if name in ['constructor_wait_unknown','cleanup_wait_unknown']:
                    passed=passed and result['owned_job_empty'] is None and result['cleanup']['exit_audit']['status']=='ERROR'
                else:passed=passed and result['owned_job_empty'] is True
                if name in ['post_create_membership','post_create_identity','constructor_wait_unknown']:
                    snapshots=result['constructor_failure_evidence']['constructor_error_snapshot']
                    passed=passed and snapshots['status']=='OBSERVED' and snapshots['value'][0]['identity_status']=='UNKNOWN'
                if name in ['post_create_snapshot','post_create_capture_setup']:
                    passed=passed and result['constructor_failure_evidence']['constructor_error_snapshot']['status']=='ERROR' and result['failure']['stage'] in ['constructor_snapshot','capture_setup']
                if name=='final_scan_entrycap':
                    passed=passed and result['failure']['exception_stage']=='DISK_SCAN_ENTRY_CAP' and result['disk_monitor']['final_scan']['status']=='ERROR'
                    passed=passed and result['disk_monitor']['final_scan']['error']['exception_stage']=='DISK_SCAN_ENTRY_CAP' and result['disk_monitor']['final_bytes_before_receipt'] is None
                if name=='poll_and_final_scan_io':
                    passed=passed and result['failure']['stage']=='disk_poll_scan' and result['disk_monitor']['final_scan']['status']=='ERROR' and result['disk_monitor']['final_overshoot_bytes'] is None
                if name=='cleanup_snapshots':
                    passed=passed and result['cleanup']['pre_snapshot']['status']=='ERROR' and result['cleanup']['post_snapshot']['status']=='ERROR' and result['cleanup']['exit_audit']['status']=='OBSERVED'
                if name=='capture_observation_failure':
                    passed=passed and result['capture']['stdout']['status']=='ERROR' and result['capture']['stdout']['persisted'] is False and result['capture']['stderr']['status']=='OBSERVED'
                    passed=passed and result['capture']['stdout']['partial_summary']['value']['EOF_observed']
                events={e['stage'] for e in result['fault_events']}
                passed=passed and events==set(plan)
                save(directory/'independent-handle-audit.json',{'case':name,'observer':'Noninherited process-handle duplicates held by test only; no duplicated job handle',
                     'members':audit,'module_owned_job_empty':result['owned_job_empty'],'no_cleanup_performed_by_observer':True,
                     'candidate_unknown_does_not_become_true_from_observer':True,'passed':passed})
                cases.append({'name':name,'passed':passed,'expected_status':expected,'actual_status':result['status'],
                     'fault_plan':plan,'fault_injection_is_not_actual_platform_failure':bool(plan),
                     'runtime':pin(runtime),'independent_audit':pin(directory/'independent-handle-audit.json'),
                     'module_process_created':result['process_created'],'module_child_resumed':result['child_resumed'],
                     'module_owned_job_empty':result['owned_job_empty'],'observer_exact_identities_exited':len(audit)})
            finally:observer.close()
            sentinel_check(name+'_after')
    except BaseException as e:failure={'type':type(e).__name__,'message':str(e)}
    finally:
        if sentinel:
            save(sdir/'stop.json',{'normal_sentinel_shutdown':True})
            end=time.monotonic()+2
            while not exit_observation(sentinel.pi.hProcess)['signaled'] and time.monotonic()<end:time.sleep(.01)
            primary_exit=exit_observation(sentinel.pi.hProcess)
            remaining=sentinel.ids()
            # Console hosts can outlive the primary briefly; cleanup is own job.
            audit=sentinel.cleanup()
            capture=sentinel.finish_captures()
            clean=primary_exit['signaled'] and primary_exit['exit_code']==0
            save(sdir/'sentinel-audit.json',{'initial_identity':initial,'checks':sentinel_checks,
                'primary_natural_exit':primary_exit,'remaining_owned_members_before_final_cleanup':remaining,
                'cleanup':audit,'capture':capture,'primary_not_miskilled':clean})
            cases.append({'name':'sentinel_not_miskilled','passed':clean and all(x['same_identity'] and x['still_running'] for x in sentinel_checks),
                    'receipt':pin(sdir/'sentinel-audit.json')})
        result={'schema':'new-successor-error-path-fixtures-v1','status':'PASS' if failure is None and len(cases)==11 and all(x['passed'] for x in cases) else 'FAIL',
               'cases':cases,'failure':failure,'module_at_execution':pin(HERE/'owned_process.py'),
               'fixture_at_execution':pin(fixture),'driver_at_execution':pin(Path(__file__).resolve()),
               'command':[sys.executable,'-B',str(Path(__file__).resolve())],
               'runtime_python':sys.version,'runtime_executable':pin(Path(sys.executable)),
               'old_whole_PASS_or_affected_old_fixture_reexecuted':False,'IDA_launches':0,'game_launches':0}
        save(HERE/'new-error-path-tests.json',result)
    print(json.dumps({'status':result['status'],'passed':sum(c['passed'] for c in cases),'total':len(cases),'failure':failure},ensure_ascii=True))
    return 0 if result['status']=='PASS' else 1
if __name__=='__main__':raise SystemExit(main())

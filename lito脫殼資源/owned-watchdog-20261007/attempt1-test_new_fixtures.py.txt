"""One-shot new real-process tests; no old PASS or IDA invocation."""
import sys
sys.dont_write_bytecode=True
import ctypes as C
from ctypes import wintypes as W
import json
import time
from pathlib import Path
from owned_process import HERE,ContainedProcess,run_owned,ControlError,InJob,exit_observation,digest,save

def pin(p):return {'path':str(p),'bytes':p.stat().st_size,'sha256':digest(p)}
def key(i):return (i['pid'],i['creation_filetime_100ns'],i['image_path'].casefold())
def main():
    fixture=HERE/'fixture_helper.py';start=time.monotonic()
    sentinel_dir=HERE/'runs'/'sentinel';sentinel_dir.mkdir(parents=True)
    sentinel=None;tests=[];checks=[];failure=None
    try:
        sentinel=ContainedProcess([sys.executable,'-B',str(fixture),'sentinel'],sentinel_dir)
        sentinel.resume();sentinel_identity=sentinel.snapshots()[0]
        end=time.monotonic()+2
        while not (sentinel_dir/'sentinel-start.json').exists() and time.monotonic()<end:time.sleep(.01)
        assert (sentinel_dir/'sentinel-start.json').exists()
        def sentinel_check(label):
            snap=sentinel.snapshots()[0]
            value={'case':label,'identity':snap,'same_identity':key(snap)==key(sentinel_identity),
                   'still_running':not snap['exit']['signaled']}
            checks.append(value)
            assert value['same_identity'] and value['still_running']
        def callback(process):
            inside=W.BOOL()
            assert InJob(sentinel.pi.hProcess,process.job,C.byref(inside))
            assert not inside.value
        cases=[('normal','normal','PRIMARY_EXITED',False),('hang','hang','TIMEOUT',False),
               ('spawned_child','tree','TIMEOUT',False),('stdout_stderr_flood','flood','PRIMARY_EXITED',False),
               ('disk_poll','disk','DISK_THRESHOLD',False),('failed_containment','normal','CONTAINMENT_OR_CONTROL_REFUSED',True)]
        for name,mode,status,forced in cases:
            sentinel_check(name+'_before')
            directory=HERE/'runs'/name
            r=run_owned([sys.executable,'-B',str(fixture),mode],directory,timeout_seconds=2,
                  capture_bytes=8192,disk_limit_bytes=65536 if mode=='disk' else 1048576,
                  disk_poll_seconds=.05,fail_containment=forced,on_started=callback)
            passed=r['status']==status and r['owned_job_empty']
            if not forced:
                if r['status']=='CONTAINMENT_OR_CONTROL_REFUSED':
                    tests.append({'name':name,'passed':False,'receipt':pin(directory/'runtime.json')})
                    raise ControlError('PLATFORM_CONTAINMENT_UNAVAILABLE_NO_FALLBACK',r['failure']['winerror'])
                passed=passed and all(p['exit']['signaled'] for p in r['post_exit_members']) and all(c['EOF_observed'] for c in r['capture'].values())
            if name=='normal':passed=passed and r['post_exit_members'][0]['exit']['exit_code']==0
            if name=='spawned_child':passed=passed and len(r['post_exit_members'])>=2 and (directory/'leaf-start.json').exists()
            if name=='stdout_stderr_flood':
                passed=passed and all(c['observed_bytes']==2097152 and c['retained_bytes']==8192 and c['discarded_bytes']==2088960 for c in r['capture'].values())
            if name=='failed_containment':
                passed=passed and not r['child_resumed'] and not r['process_created'] and not (directory/'normal-start.json').exists() and r['failure']['stage']=='SetInformationJobObject' and r['failure']['winerror']!=0
            if name=='disk_poll':passed=passed and r['trigger'][0]['observed_overshoot_bytes']>0 and r['disk_monitor']['hard_quota'] is False
            tests.append({'name':name,'passed':passed,'expected_status':status,'actual_status':r['status'],
                          'receipt':pin(directory/'runtime.json'),'sentinel_outside_target_job':not forced})
            sentinel_check(name+'_after')
        # A Python controller exception must also clean its live fixture tree.
        directory=HERE/'runs'/'controller_exception'
        def exception_callback(process):
            callback(process)
            end=time.monotonic()+1
            while time.monotonic()<end:
                process.observe()
                if len(process.members)>=2 and (directory/'leaf-start.json').exists():break
                time.sleep(.01)
            raise RuntimeError('controlled fixture controller exception')
        r=run_owned([sys.executable,'-B',str(fixture),'tree'],directory,timeout_seconds=3,on_started=exception_callback)
        tests.append({'name':'controller_exception','passed':r['status']=='CONTROLLER_EXCEPTION' and r['owned_job_empty'] and len(r['post_exit_members'])>=2 and all(p['exit']['signaled'] for p in r['post_exit_members']),
                      'receipt':pin(directory/'runtime.json')})
        sentinel_check('controller_exception_after')
        # Hold the OUTER job handle open while the abrupt controller exit closes
        # its INNER job. Observe all descendants exit BEFORE any outer cleanup.
        directory=HERE/'runs'/'controller_exit';directory.mkdir()
        controller=ContainedProcess([sys.executable,'-B',str(HERE/'crash_controller.py')],directory)
        crash={};cleanup_called=False
        try:
            callback(controller);controller.resume()
            deadline=time.monotonic()+4
            while time.monotonic()<deadline:
                controller.observe()
                if exit_observation(controller.pi.hProcess)['signaled']:break
                time.sleep(.005)
            exited=exit_observation(controller.pi.hProcess)
            checkpoint=json.loads((directory/'crash-checkpoint.json').read_text('utf-8'))
            # Give kernel close-on-exit termination time while keeping outer job
            # alive. The observer has exact handles; no broad PID scan/kill.
            end=time.monotonic()+1
            while controller.observe() and time.monotonic()<end:time.sleep(.005)
            observed=controller.snapshots();inner=checkpoint['inner_members']
            matched=all(any(key(i)==key(o) and o['exit']['signaled'] for o in observed) for i in inner)
            no_active=not controller.ids()
            crash={'schema':'owned-controller-abrupt-exit-v1','controller_identity':controller.members[int(controller.pi.dwProcessId)]['identity'],
                   'controller_exit':exited,'checkpoint':checkpoint,'observed_members_before_outer_cleanup':observed,
                   'outer_job_handle_still_open':True,'outer_cleanup_before_exit_audit':False,
                   'inner_identities_matched':matched,'all_job_members_exited_before_outer_cleanup':no_active,
                   'passed':exited['signaled'] and exited['exit_code']==73 and len(inner)>=2 and matched and no_active}
        finally:
            if controller.ids():controller.kill();cleanup_called=True;controller.await_empty()
            crash['fallback_outer_cleanup_needed']=cleanup_called
            crash['capture']={n:c.finish(directory/(n+'.bin')) for n,c in controller.captures.items()}
            controller.close()
            save(directory/'abrupt-exit-audit.json',crash)
        tests.append({'name':'controller_exit','passed':crash.get('passed',False) and not cleanup_called,
                      'receipt':pin(directory/'abrupt-exit-audit.json')})
        sentinel_check('controller_exit_after')
    except BaseException as e:
        failure={'type':type(e).__name__,'message':str(e)}
    finally:
        if sentinel:
            save(sentinel_dir/'stop',{'reason':'normal fixture sentinel shutdown'})
            end=time.monotonic()+2
            while not exit_observation(sentinel.pi.hProcess)['signaled'] and time.monotonic()<end:time.sleep(.01)
            sentinel_end=sentinel.snapshots()
            clean=not sentinel.ids() and sentinel_end[0]['exit']['exit_code']==0
            if not clean:sentinel.kill();sentinel.await_empty()
            audit={'initial_identity':sentinel_identity,'observations':checks,'final_members':sentinel_end,
                   'normal_exit_code_zero':clean,'sentinel_force_cleanup_needed':not clean,
                   'capture':{n:c.finish(sentinel_dir/(n+'.bin')) for n,c in sentinel.captures.items()}}
            sentinel.close();save(sentinel_dir/'sentinel-audit.json',audit)
            tests.append({'name':'sentinel_not_miskilled','passed':clean and all(c['same_identity'] and c['still_running'] for c in checks),
                          'receipt':pin(sentinel_dir/'sentinel-audit.json')})
        result={'schema':'new-owned-process-fixture-tests-v1','status':'PASS' if failure is None and len(tests)==9 and all(t['passed'] for t in tests) else 'FAIL',
                'new_real_process_cases':tests,'failure':failure,'runtime_python':sys.version,
                'runtime_executable':pin(Path(sys.executable)),'windows_version':list(sys.getwindowsversion()),
                'test_command':[sys.executable,'-B',str(Path(__file__).resolve())],
                'compile':'Not applicable: Python standard-library source, no local compiler or installation needed',
                'elapsed_seconds':time.monotonic()-start,'IDA_launches':0,'game_launches':0,'old_PASS_replayed':False}
        save(HERE/'new-real-process-tests.json',result)
    print(json.dumps({'status':result['status'],'cases':len(tests),'passed':sum(t['passed'] for t in tests),'failure':failure}))
    return 0 if result['status']=='PASS' else 1

if __name__=='__main__':raise SystemExit(main())

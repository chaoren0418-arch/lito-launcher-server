"""Affected cleanup/exit-audit cases only; preserve first execution receipts."""
import sys
sys.dont_write_bytecode=True
import ctypes as C
from ctypes import wintypes as W
import json
from pathlib import Path
import time
from owned_process import HERE,ContainedProcess,run_owned,InJob,exit_observation,digest,save
def pin(p):return {'path':str(p),'bytes':p.stat().st_size,'sha256':digest(p)}
def key(i):return (i['pid'],i['creation_filetime_100ns'],i['image_path'].casefold())
def main():
    fixture=HERE/'fixture_helper.py';base=HERE/'runs-recheck';base.mkdir()
    directory=base/'sentinel';directory.mkdir();tests=[];observations=[];sentinel=None;failure=None
    try:
        sentinel=ContainedProcess([sys.executable,'-B',str(fixture),'sentinel'],directory)
        sentinel.resume();first=sentinel.snapshots()[0]
        deadline=time.monotonic()+1
        while not (directory/'sentinel-start.json').exists() and time.monotonic()<deadline:time.sleep(.01)
        assert (directory/'sentinel-start.json').exists()
        def sentinel_check(label):
            s=sentinel.snapshots()[0]
            observation={'case':label,'identity':s,'same_identity':key(s)==key(first),'still_running':not s['exit']['signaled']}
            observations.append(observation);assert observation['same_identity'] and observation['still_running']
        def callback(process):
            b=W.BOOL();assert InJob(sentinel.pi.hProcess,process.job,C.byref(b)) and not b.value
        for name,mode,status in [('normal','normal','PRIMARY_EXITED'),('hang','hang','TIMEOUT'),
                                  ('spawned_child','tree','TIMEOUT'),('disk_poll','disk','DISK_THRESHOLD')]:
            sentinel_check(name+'_before')
            r=run_owned([sys.executable,'-B',str(fixture),mode],base/name,timeout_seconds=2,
               disk_limit_bytes=65536 if mode=='disk' else 1048576,on_started=callback)
            passed=r['status']==status and r['owned_job_empty'] and all(p['exit']['signaled'] for p in r['post_exit_members']) and all(c['EOF_observed'] for c in r['capture'].values())
            if name=='normal':passed=passed and r['post_exit_members'][0]['exit']['exit_code']==0
            if name=='spawned_child':passed=passed and (base/name/'leaf-start.json').exists() and any(not p['primary'] and Path(p['image_path']).name.lower()=='python.exe' for p in r['post_exit_members'])
            if name=='disk_poll':passed=passed and r['trigger'][0]['observed_overshoot_bytes']>0 and not r['disk_monitor']['hard_quota']
            tests.append({'name':name,'passed':passed,'receipt':pin(base/name/'runtime.json')})
            sentinel_check(name+'_after')
        directory=base/'controller_exception'
        def exception_callback(process):
            callback(process);end=time.monotonic()+1
            while time.monotonic()<end:
                process.observe()
                if (directory/'leaf-start.json').exists():break
                time.sleep(.01)
            raise RuntimeError('controlled fixture controller exception')
        r=run_owned([sys.executable,'-B',str(fixture),'tree'],directory,timeout_seconds=3,on_started=exception_callback)
        tests.append({'name':'controller_exception','passed':r['status']=='CONTROLLER_EXCEPTION' and r['owned_job_empty'] and (directory/'leaf-start.json').exists() and all(p['exit']['signaled'] for p in r['post_exit_members']),
                      'receipt':pin(directory/'runtime.json')})
        sentinel_check('controller_exception_after')
    except BaseException as e:failure={'type':type(e).__name__,'message':str(e)}
    finally:
        if sentinel:
            directory=base/'sentinel';save(directory/'stop',{'reason':'normal shutdown'})
            exited=sentinel.await_empty(2);final=sentinel.snapshots()
            normal=exited and final[0]['exit']['exit_code']==0
            if not exited:sentinel.kill();sentinel.await_empty()
            audit={'initial_identity':first,'observations':observations,'final_members':final,
                   'normal_exit_code_zero':normal,'sentinel_force_cleanup_needed':not exited,
                   'capture':{n:c.finish(directory/(n+'.bin')) for n,c in sentinel.captures.items()}}
            sentinel.close();save(directory/'sentinel-audit.json',audit)
            tests.append({'name':'sentinel_not_miskilled','passed':normal and all(o['same_identity'] and o['still_running'] for o in observations),'receipt':pin(directory/'sentinel-audit.json')})
        result={'schema':'owned-process-affected-case-recheck-v1','status':'PASS' if failure is None and len(tests)==6 and all(t['passed'] for t in tests) else 'FAIL',
                'checks':tests,'failure':failure,'module_at_execution':pin(HERE/'owned_process.py'),
                'helper_at_execution':pin(fixture),'driver_at_execution':pin(Path(__file__).resolve()),
                'command':[sys.executable,'-B',str(Path(__file__).resolve())],'exit_code_expected':0,
                'unaffected_initial_PASS_cases_not_rerun':['stdout_stderr_flood','failed_containment','controller_exit'],
                'old_LITO_PASS_replayed':False,'IDA_launches':0,'game_launches':0}
        save(HERE/'affected-case-recheck.json',result)
    print(json.dumps({'status':result['status'],'passed':sum(t['passed'] for t in tests),'total':len(tests),'failure':failure}))
    return 0 if result['status']=='PASS' else 1
if __name__=='__main__':raise SystemExit(main())

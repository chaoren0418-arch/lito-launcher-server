"""Finite Root-bound isolated provider host; no launch without exact final release."""
import sys
sys.dont_write_bytecode = True
import argparse
import ctypes
import hashlib
import importlib
import json
import math
import os
import time
from pathlib import Path

SERVER = Path(__file__).resolve().parents[2]
TASK = 'server-provider-lifetime-recovery-20261008'
ROOT = SERVER / 'runtime' / TASK
RUNTIME = Path('D:/lito-dev-runtime/20261008-current-human-test') / TASK
FROZEN = SERVER / 'runtime/supplier-constructor-close-summary-20261007-a58f2e522c584f1abb3bb4025f4a87c1'
PRODUCER = SERVER / 'runtime/provider-reservation-20261007-15ac7fe2552d4a0abb4bdabecd5a3ed7/provider_reservation.py'
REVIEW = SERVER.parent.parent / '監製工作流' / (TASK + '-prelaunch-assessment.json')

def need(ok, reason):
    if not ok:
        raise RuntimeError(reason)

def plain(path):
    path = Path(path)
    need(path.is_absolute() and '..' not in path.parts and not str(path).startswith('\\\\'), 'absolute-local-path')
    for node in (path, *path.parents):
        need(not node.lstat().st_file_attributes & 0x400, 'reparse-refused')
    return path.resolve(strict=True)

def pin(path):
    path = plain(path)
    raw = path.read_bytes()
    return dict(path=str(path), bytes=len(raw), sha256=hashlib.sha256(raw).hexdigest().upper())

def verify(ref):
    need(set(ref) == {'path', 'bytes', 'sha256'} and pin(ref['path']) == ref, 'exact-input-pin-moved')

def read(path):
    raw = plain(path).read_bytes()
    need(0 < len(raw) <= 16*1024*1024, 'bounded-json')
    def pairs(items):
        result = {}
        for key, value in items:
            need(key not in result, 'duplicate-json-key')
            result[key] = value
        return result
    return json.loads(raw.decode('utf-8-sig'), object_pairs_hook=pairs,
                      parse_constant=lambda value: (_ for _ in ()).throw(RuntimeError('nonfinite-json')))

def save(name, value):
    need(Path(name).name == name, 'owned-output-name')
    target = RUNTIME / name
    temporary = target.with_suffix(target.suffix + '.' + str(os.getpid()) + '.tmp')
    with temporary.open('xb') as stream:
        stream.write(json.dumps(value, ensure_ascii=False, separators=(',', ':'), allow_nan=False).encode('utf-8'))
        stream.flush()
        os.fsync(stream.fileno())
    os.replace(temporary, target)

def tick_ms():
    api = ctypes.WinDLL('kernel32', use_last_error=True).GetTickCount64
    api.argtypes = []
    api.restype = ctypes.c_ulonglong
    return int(api())

def common_identity(value):
    return dict(pid=value['pid'], created_filetime=value['filetime'], path=value['path'],
        image_sha256=value['image_sha256'], command_sha256=value['command_sha256'], parent_pid=value['parent_pid'])

def decision(*, terminal, exited, stop, guardian_ok, now_tick, end_tick, ready, start_tick, startup_ms):
    # Priority makes a real terminal error immediately actionable before cleanup.
    if terminal:
        return 'provider_terminal'
    if exited:
        return 'owner_exit'
    if not guardian_ok:
        return 'root_guardian_lost'
    if now_tick >= end_tick:
        return 'provider_lifetime_expired'
    if stop:
        return 'requested_stop'
    if not ready and now_tick-start_tick >= startup_ms:
        return 'provider_startup_deadline'
    return None

def supervise(plan, binding, job, child, *, read_json=read, write=save, exists=lambda name:(RUNTIME/name).exists(),
              check_guardian=None, qualify_ready=None, tick=tick_ms, epoch=time.time, pause=time.sleep):
    ready = False
    reason = None
    resident_end_tick = binding['lifetime_end_tick_ms']
    resident_end_epoch = binding['not_after_epoch']
    while reason is None:
        try:
            check_guardian()
            guardian_ok = True
        except Exception:
            guardian_ok = False
        reason = decision(terminal=exists('provider-terminal.json'), exited=job.exited(child),
            stop=exists('stop-owner.request'), guardian_ok=guardian_ok, now_tick=tick(),
            end_tick=binding['lifetime_end_tick_ms'], ready=ready, start_tick=binding['started_tick_ms'],
            startup_ms=plan['lifetime_budget']['startup_ms'])
        if reason is None and epoch() >= binding['not_after_epoch']:
            reason = 'provider_release_expired'
        if reason is None and ready and (tick() >= resident_end_tick or epoch() >= resident_end_epoch):
            reason = 'provider_residency_expired'
        if reason is None and exists('ready-provider.json'):
            try:
                provider=read_json(RUNTIME/'ready-provider.json')
                qualify_ready(provider)
                if not ready:
                    ready_tick=provider['provider_ready_tick_ms']
                    ready_epoch=provider['provider_ready_epoch']
                    need(type(ready_tick) is int and binding['started_tick_ms'] <= ready_tick <= tick(), 'actual-first-READY-tick')
                    need(type(ready_epoch) in (int,float) and math.isfinite(ready_epoch) and ready_epoch <= epoch(), 'actual-first-READY-epoch')
                    resident_end_tick=min(binding['lifetime_end_tick_ms'],ready_tick+plan['lifetime_budget']['resident_ms'])
                    resident_end_epoch=min(binding['not_after_epoch'],ready_epoch+plan['lifetime_budget']['resident_ms']/1000)
                    need(provider['provider_resident_end_tick_ms']==resident_end_tick
                         and provider['provider_not_after_epoch']==resident_end_epoch, 'READY-fixed-residency-cap')
                    write('retained-host-state.json', dict(schema='SERVER_RETAINED_HOST_STATE_1', status='READY',
                        owner_pid=child['pid'], root_guardian=binding['root_guardian'],
                        provider_ready_tick_ms=ready_tick,provider_resident_end_tick_ms=resident_end_tick,
                        provider_not_after_epoch=resident_end_epoch,native_started=False))
                ready = True
            except Exception:
                reason = 'ready_live_identity_or_listener_refused'
        if reason is None:
            pause(.25)
    if not exists('provider-terminal.json'):
        write('provider-terminal.json', dict(schema='SERVER_PROVIDER_TERMINAL_1',task_id=TASK,status='TERMINAL',
            reason=reason, phase='retained-host', at_epoch=epoch(),cleanup_complete=None,native_started=False,product_accepted=False))
    write('retained-host-state.json', dict(schema='SERVER_RETAINED_HOST_STATE_1',status='TERMINATING',reason=reason,
        owner_pid=child['pid'],at_epoch=epoch(),cleanup_complete=None))
    # Graceful original owner shutdown is always attempted first.
    if not job.exited(child):
        write('stop-owner.request', dict(reason=reason, exact_retained_owner_pid=child['pid']))
    cleanup_end = tick() + plan['cleanup_budget']['outer_ms']
    while not job.exited(child) and tick() < cleanup_end:
        pause(.1)
    emergency = not job.exited(child)
    if emergency:
        # Only the atomic non-breakaway owned Job; no PID reacquisition/SQL kill.
        # This is containment, never an original helper SQL-retirement receipt.
        job.stop()
    rows = job.records(500)
    cleanup = read_json(RUNTIME/'cleanup.json') if exists('cleanup.json') else None
    qualified = (not emergency and len(rows)==1 and rows[0].get('exited') is True
                 and rows[0].get('exit_code') == 0 and cleanup is not None
                 and cleanup.get('status') == 'owned_processes_retired')
    answer = dict(schema='SERVER_RETAINED_PROVIDER_EXIT_1', task_id=TASK, reason=reason,
        READY_observed=ready, owned_child_exit=rows, original_cleanup=cleanup,
        containment_emergency_used=emergency, original_SQL_retirement_qualified=qualified,
        cleanup_complete=qualified, native_started=False, product_accepted=False, at_epoch=epoch())
    write('retained-host-exit.json', answer)
    return answer

def retire_after_host_error(plan, job, child, *, write=save, tick=tick_ms, pause=time.sleep):
    """Preserve primary error; attempt original owner stop before containment close."""
    report=dict(graceful_stop_requested=False, child_exit_known=None, containment_emergency_used=False)
    if child is None:
        return report
    try:
        if not job.exited(child):
            write('stop-owner.request',dict(reason='retained_host_exception',exact_retained_owner_pid=child['pid']))
            report['graceful_stop_requested']=True
        end=tick()+plan['cleanup_budget']['outer_ms']
        while not job.exited(child) and tick()<end:
            pause(.1)
        report['child_exit_known']=job.exited(child)
        if not report['child_exit_known']:
            job.stop()
            report['containment_emergency_used']=True
        report['exit_observations']=job.records(500)
    except BaseException as error:
        report['secondary_cleanup_error']=str(error)
    return report

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--review', required=True)
    parser.add_argument('--review-sha256', required=True)
    args = parser.parse_args()
    job = None
    answer = None
    authorized = False
    exit_code = 2
    bounded = None
    child = None
    plan = None
    try:
        need(plain(args.review) == REVIEW.resolve(), 'exact-final-review-path')
        need(pin(args.review)['sha256'] == args.review_sha256, 'final-review-command-pin')
        review = read(args.review)
        plan = read(ROOT/'startup-plan.json')
        need(review['task_id']==TASK and review['reviewer']=='supervisor' and review['decision']=='accepted'
             and review['provider_sql_launch_authorized'] is True and review['kernel_job_containment_cleanup_authorized'] is True,
             'no-exact-provider-lifecycle-release')
        need(review['prepared_plan_sha256']==pin(ROOT/'startup-plan.json')['sha256']
             and review['launch_input_manifest_sha256']==pin(ROOT/'launch-input-manifest.json')['sha256'], 'exact-reviewed-inputs')
        for ref in read(ROOT/'launch-input-manifest.json'):
            verify(ref)
        need(pin(PRODUCER)['sha256']=='E46C57933FB895534D97A808F9997F46203E2A0B3D550E802BFE36F2C6E328BB', 'original-producer-moved')
        need(pin(FROZEN/'windows_execution.py')['sha256']=='D9E5E54E34DA74D8173383AC9C748C24055D68B6B97093CA06AB4C630236532E', 'original-job-moved')
        need(pin(FROZEN/'deadline_clock.py')['sha256']=='472599A7A093B6D797A94655CBE9CD5E87C08C790B40FF59479675FD6D528174', 'original-clock-moved')
        sys.path.insert(0, str(PRODUCER.parent))
        r = importlib.import_module('provider_reservation')
        sys.path.insert(0, str(FROZEN))
        windows = importlib.import_module('windows_execution')
        clock = importlib.import_module('deadline_clock')
        bounded = importlib.import_module('bounded_supplier')
        need(str(Path(sys.executable).resolve()) == plan['python']['path'], 'pinned-host-Python')
        guardian = review['root_guardian']
        need(guardian['pid']==os.getppid(), 'Root-guardian-must-be-real-parent')
        r.check_identity(guardian)
        now = time.time()
        before, end = review['not_before_epoch'], review['not_after_epoch']
        need(type(before) in (int,float) and type(end) in (int,float) and math.isfinite(before) and math.isfinite(end)
             and before <= now < end, 'finite-current-release-window')
        total_ms = plan['lifetime_budget']['startup_ms']+plan['lifetime_budget']['resident_ms']+plan['cleanup_budget']['outer_ms']
        need(total_ms==2_240_000 and now+total_ms/1000 <= end, 'full-reviewed-lifetime-and-cleanup-window')
        need(plain(plan['ascii_runtime_root'])==RUNTIME.resolve(), 'exact-fresh-runtime')
        for name in ('host-issued.once','retained-lifetime-owner.json','start.once','owner.json','ready-provider.json',
                     'provider-terminal.json','native-fixture.json','stop.request','stop-owner.request','lito-modern-mysql-fixtures'):
            need(not (RUNTIME/name).exists(), 'used-runtime-no-retry:'+name)
        with (RUNTIME/'host-issued.once').open('xb'):
            pass
        authorized = True
        start_tick = tick_ms()
        job = windows.Job()
        binding = dict(schema='SERVER_RETAINED_PROVIDER_LIFETIME_1',task_id=TASK,runtime_root=str(RUNTIME),
            host=common_identity(r.identity(os.getpid())),root_guardian=common_identity(guardian),
            reviewed_plan_sha256=review['prepared_plan_sha256'],review_sha256=args.review_sha256,
            started_tick_ms=start_tick,lifetime_end_tick_ms=start_tick+total_ms-plan['cleanup_budget']['outer_ms'],
            not_after_epoch=now+(total_ms-plan['cleanup_budget']['outer_ms'])/1000,job_policy=job.policy)
        save('retained-lifetime-owner.json', binding)
        binding_ref = pin(RUNTIME/'retained-lifetime-owner.json')
        argv = [plan['powershell']['path'],'-NoProfile','-File',str(ROOT/'Start-Provider.ps1'),
                '-ReviewPath',str(REVIEW),'-ReviewSha256',args.review_sha256,
                '-LifetimeOwnerPath',binding_ref['path'],'-LifetimeOwnerSha256',binding_ref['sha256']]
        child = job.spawn(argv, clock.Deadline(time.monotonic_ns(),time.monotonic_ns()+10_000_000_000))
        actual = r.identity(child['pid'])
        need(actual['filetime']==child['filetime'] and actual['path']==argv[0]
             and actual['parent_pid']==os.getpid() and actual['command_argv_digest']==r.digest(argv), 'actual-retained-owner-vector')
        save('retained-owner-launch.json',dict(owner=actual,argv=argv,job_policy=job.policy,retained_raw_handle=True))
        def qualify(provider):
            need(provider['task_id']==TASK and provider['provider_root']==str(RUNTIME)
                 and provider['retained_lifetime_owner_sha256']==binding_ref['sha256'], 'fresh-ready-lifetime-pin')
            for role in ('owner','server','helper','mysql'):
                expected=provider['exact_ownership'][role]
                need(common_identity(r.identity(expected['pid'])) == {key:expected[key] for key in binding['host']}, 'provider-full-identity:'+role)
            r.live_listeners(dict(listeners=[dict(address=row['LocalAddress'],port=row['LocalPort'],pid=row['OwningProcess']) for row in provider['listeners']]))
        answer = supervise(plan,binding,job,child,check_guardian=lambda:r.check_identity(guardian),qualify_ready=qualify)
        exit_code = 0 if answer['cleanup_complete'] and answer['reason']=='requested_stop' else 2
    except BaseException as error:
        answer = dict(schema='SERVER_RETAINED_PROVIDER_HOST_FAILURE_1',reason=str(error),cleanup_complete=False,native_started=False,product_accepted=False)
        if authorized:
            try:
                if not (RUNTIME/'provider-terminal.json').exists():
                    save('provider-terminal.json',dict(schema='SERVER_PROVIDER_TERMINAL_1',task_id=TASK,status='TERMINAL',reason='retained_host_exception',detail=str(error),cleanup_complete=None))
                save('retained-host-failure.json',answer)
            except Exception:
                pass
            if job is not None and plan is not None:
                answer['primary_error_cleanup']=retire_after_host_error(plan,job,child)
        exit_code = 2
    finally:
        if job is not None:
            try:
                closed=job.close()
                handles_complete = bounded.close_complete(closed)
            except BaseException as error:
                closed=dict(error=str(error))
                handles_complete = False
            if handles_complete is not True:
                exit_code = 2
                if answer is not None:
                    answer['cleanup_complete']=False
                    answer['resource_close_complete']=handles_complete
            if authorized:
                try:
                    save('retained-job-close.json',dict(close=closed,resource_close_complete=handles_complete,cleanup_complete=answer.get('cleanup_complete',False) if answer else False,
                        note='Closed handles never independently qualify child/SQL retirement. Kernel Job close contains all inherited non-breakaway descendants.'))
                    save('retained-host-final.json',answer)
                except Exception:
                    pass
    print(json.dumps(answer,ensure_ascii=True),flush=True)
    return exit_code

if __name__ == '__main__':
    sys.exit(main())

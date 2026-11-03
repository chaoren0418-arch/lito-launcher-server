"""Windows fixture execution control. No IDA route or security sandbox.

Python 3.12, standard library. Atomic JOB_LIST at CreateProcessW prevents
the suspended/unassigned crash window. KILL_ON_JOB_CLOSE, no breakaway,
non-inherited unnamed job handle; only an explicit stdio handle list.
All cleanup uses that job or retained handles to its verified members.
"""
import sys
sys.dont_write_bytecode=True
import ctypes as C
from ctypes import wintypes as W
import hashlib
import json
import os
from pathlib import Path
import subprocess
import threading
import time
from datetime import datetime, timezone

HERE=Path(__file__).resolve().parent
if os.name!='nt':raise RuntimeError('Windows atomic Job Object required; no fallback')
K=C.WinDLL('kernel32',use_last_error=True)
PTR=C.c_void_p
SIZE=C.c_size_t

class SECURITY_ATTRIBUTES(C.Structure):
    _fields_=[('nLength',W.DWORD),('lpSecurityDescriptor',PTR),('bInheritHandle',W.BOOL)]
class BASIC_LIMIT(C.Structure):
    _fields_=[('PerProcessUserTimeLimit',C.c_longlong),('PerJobUserTimeLimit',C.c_longlong),
              ('LimitFlags',W.DWORD),('MinimumWorkingSetSize',SIZE),('MaximumWorkingSetSize',SIZE),
              ('ActiveProcessLimit',W.DWORD),('Affinity',SIZE),('PriorityClass',W.DWORD),('SchedulingClass',W.DWORD)]
class IO_COUNTERS(C.Structure):
    _fields_=[(n,C.c_ulonglong) for n in ['ReadOperationCount','WriteOperationCount','OtherOperationCount','ReadTransferCount','WriteTransferCount','OtherTransferCount']]
class EXTENDED_LIMIT(C.Structure):
    _fields_=[('BasicLimitInformation',BASIC_LIMIT),('IoInfo',IO_COUNTERS),
              ('ProcessMemoryLimit',SIZE),('JobMemoryLimit',SIZE),('PeakProcessMemoryUsed',SIZE),('PeakJobMemoryUsed',SIZE)]
class STARTUPINFO(C.Structure):
    _fields_=[('cb',W.DWORD),('lpReserved',W.LPWSTR),('lpDesktop',W.LPWSTR),('lpTitle',W.LPWSTR),
              *[(n,W.DWORD) for n in ['dwX','dwY','dwXSize','dwYSize','dwXCountChars','dwYCountChars','dwFillAttribute','dwFlags']],
              ('wShowWindow',W.WORD),('cbReserved2',W.WORD),('lpReserved2',PTR),
              ('hStdInput',W.HANDLE),('hStdOutput',W.HANDLE),('hStdError',W.HANDLE)]
class STARTUPINFOEX(C.Structure):
    _fields_=[('StartupInfo',STARTUPINFO),('lpAttributeList',PTR)]
class PROCESS_INFORMATION(C.Structure):
    _fields_=[('hProcess',W.HANDLE),('hThread',W.HANDLE),('dwProcessId',W.DWORD),('dwThreadId',W.DWORD)]
class FILETIME(C.Structure):
    _fields_=[('low',W.DWORD),('high',W.DWORD)]

def api(name,args,result=W.BOOL):
    f=getattr(K,name);f.argtypes=args;f.restype=result;return f
CreateJob=api('CreateJobObjectW',[PTR,W.LPCWSTR],W.HANDLE)
SetJob=api('SetInformationJobObject',[W.HANDLE,C.c_int,PTR,W.DWORD])
QueryJob=api('QueryInformationJobObject',[W.HANDLE,C.c_int,PTR,W.DWORD,PTR])
TerminateJob=api('TerminateJobObject',[W.HANDLE,W.UINT])
Close=api('CloseHandle',[W.HANDLE])
HandleFlags=api('GetHandleInformation',[W.HANDLE,C.POINTER(W.DWORD)])
SetHandleFlags=api('SetHandleInformation',[W.HANDLE,W.DWORD,W.DWORD])
CreatePipe=api('CreatePipe',[C.POINTER(W.HANDLE),C.POINTER(W.HANDLE),C.POINTER(SECURITY_ATTRIBUTES),W.DWORD])
CreateFile=api('CreateFileW',[W.LPCWSTR,W.DWORD,W.DWORD,C.POINTER(SECURITY_ATTRIBUTES),W.DWORD,W.DWORD,W.HANDLE],W.HANDLE)
InitAttrs=api('InitializeProcThreadAttributeList',[PTR,W.DWORD,W.DWORD,C.POINTER(SIZE)])
UpdateAttrs=api('UpdateProcThreadAttribute',[PTR,W.DWORD,SIZE,PTR,SIZE,PTR,PTR])
DeleteAttrs=api('DeleteProcThreadAttributeList',[PTR],None)
CreateProcess=api('CreateProcessW',[W.LPCWSTR,W.LPWSTR,PTR,PTR,W.BOOL,W.DWORD,PTR,W.LPCWSTR,PTR,C.POINTER(PROCESS_INFORMATION)])
ResumeThread=api('ResumeThread',[W.HANDLE],W.DWORD)
Wait=api('WaitForSingleObject',[W.HANDLE,W.DWORD],W.DWORD)
GetExit=api('GetExitCodeProcess',[W.HANDLE,C.POINTER(W.DWORD)])
GetTimes=api('GetProcessTimes',[W.HANDLE,*[C.POINTER(FILETIME)]*4])
GetPath=api('QueryFullProcessImageNameW',[W.HANDLE,W.DWORD,W.LPWSTR,C.POINTER(W.DWORD)])
InJob=api('IsProcessInJob',[W.HANDLE,W.HANDLE,C.POINTER(W.BOOL)])
OpenProcess=api('OpenProcess',[W.DWORD,W.BOOL,W.DWORD],W.HANDLE)
ReadFile=api('ReadFile',[W.HANDLE,PTR,W.DWORD,C.POINTER(W.DWORD),PTR])

class ControlError(RuntimeError):
    def __init__(self,stage,winerror=None):
        self.stage=stage;self.winerror=C.get_last_error() if winerror is None else winerror
        super().__init__(stage+': winerror='+str(self.winerror))
class InjectedFailure(RuntimeError):
    def __init__(self,stage,kind):
        self.stage=stage;self.kind=kind
        super().__init__('Explicit harmless-fixture fault injection: '+stage+'/'+kind)
class FaultPlan:
    """Tests may fail observations, never substitute cleanup/exit success."""
    def __init__(self,plan=None):self.plan=dict(plan or {});self.events=[]
    def hit(self,stage):
        if stage in self.plan:
            kind=self.plan.pop(stage)
            self.events.append({'stage':stage,'kind':kind,'monotonic':time.monotonic(),
                                'origin':'controlled_test_injection','actual_API_failure_claimed':False})
            raise InjectedFailure(stage,kind)
def error_record(e,stage=None):
    return {'stage':stage or getattr(e,'stage',None),'exception_stage':getattr(e,'stage',None),'type':type(e).__name__,
            'message':str(e)[:1024],'winerror':getattr(e,'winerror',None),
            'origin':'controlled_test_injection' if isinstance(e,InjectedFailure) else 'actual_exception_or_refusal'}
def attempt(stage,fn,faults=None):
    try:
        if faults:faults.hit(stage)
        return {'status':'OBSERVED','value':fn()}
    except BaseException as e:return {'status':'ERROR','error':error_record(e,stage)}
class ConstructorFailure(RuntimeError):
    def __init__(self,cause,evidence):
        self.cause=error_record(cause);self.process_evidence=evidence
        super().__init__('Constructor failed; cleanup evidence is attached')
def check(ok,stage):
    if not ok:raise ControlError(stage)
def digest(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest().upper()
def owned(path):
    p=Path(path).resolve()
    if HERE not in p.parents:raise ControlError('OWNED_PATH_REQUIRED',0)
    # Reject reparse points on the lexical path too; no redirected job output.
    for a in [Path(path),*Path(path).parents]:
        if a.exists() and a.stat(follow_symlinks=False).st_file_attributes&0x400:
            raise ControlError('REPARSE_PATH_REFUSED',0)
        if a==HERE:break
    return p
def save(path,value):
    p=owned(path)
    blob=json.dumps(value,ensure_ascii=False,indent=2).encode('utf-8')
    if len(blob)>262144:raise ControlError('RECEIPT_BYTE_CAP',0)
    with p.open('xb') as f:
        f.write(blob);f.flush();os.fsync(f.fileno())
def identity(handle,pid):
    fs=[FILETIME() for _ in range(4)]
    check(GetTimes(handle,*[C.byref(x) for x in fs]),'GetProcessTimes')
    ticks=(fs[0].high<<32)|fs[0].low
    buf=C.create_unicode_buffer(32768);n=W.DWORD(len(buf))
    check(GetPath(handle,0,buf,C.byref(n)),'QueryFullProcessImageNameW')
    return {'pid':int(pid),'creation_filetime_100ns':ticks,
            'creation_time_utc':datetime.fromtimestamp((ticks-116444736000000000)/10000000,timezone.utc).isoformat(),
            'image_path':buf.value}
def exit_observation(handle):
    state=Wait(handle,0);code=W.DWORD()
    check(GetExit(handle,C.byref(code)),'GetExitCodeProcess')
    return {'signaled':state==0,'wait_result':int(state),'exit_code':int(code.value)}

class Capture:
    """Always drain; retain at most cap bytes per stream in memory and disk."""
    def __init__(self,handle,cap):
        self.handle=handle;self.cap=cap;self.kept=bytearray();self.total=0;self.error=None
        self.thread=threading.Thread(target=self.drain,daemon=True)
        self.thread.start()
    def drain(self):
        buf=C.create_string_buffer(8192);n=W.DWORD()
        try:
            while True:
                if not ReadFile(self.handle,buf,len(buf),C.byref(n),None):
                    error=C.get_last_error()
                    if error!=109:self.error=error
                    break
                if not n.value:break
                self.total+=n.value
                remaining=self.cap-len(self.kept)
                if remaining>0:self.kept.extend(buf.raw[:min(n.value,remaining)])
        finally:Close(self.handle)
    def summary(self):
        return {'observed_bytes':self.total,'retained_bytes':len(self.kept),
                'discarded_bytes':self.total-len(self.kept),'cap_bytes':self.cap,
                'EOF_observed':not self.thread.is_alive() and self.error is None,'reader_error':self.error}
    def finish(self,path,faults=None,stage=None):
        self.thread.join(1)
        if faults:faults.hit(stage)
        with owned(path).open('xb') as f:f.write(self.kept)
        return {**self.summary(),'path':str(path),'sha256':digest(path),'persisted':True}

class ContainedProcess:
    """Atomic containment primitive. Opening a job/process does not resume it."""
    def __init__(self,argv,job_dir,capture_bytes=8192,fail_containment=False,faults=None,on_created=None):
        self.job=None;self.pi=None;self.members={};self.resumed=False;self.closed=False
        self.process_created=False;self.creation_call_success=False;self.close_result=None
        self.faults=faults or FaultPlan();self.policy=None;self.source=None
        self.job_dir=owned(job_dir);self.argv=list(argv);self.captures={}
        if not self.job_dir.is_dir():raise ControlError('JOB_DIR_REQUIRED',0)
        if not 1<=capture_bytes<=65536:raise ControlError('CAPTURE_BUDGET',0)
        # Delivery is fixture-only: installed Python, -B and an owned source.
        if len(argv)<3 or Path(argv[0]).resolve()!=Path(sys.executable).resolve() or argv[1]!='-B':
            raise ControlError('FIXTURE_PYTHON_ONLY',0)
        source=owned(argv[2])
        if source.suffix!='.py' or not source.is_file():raise ControlError('OWNED_FIXTURE_SOURCE_REQUIRED',0)
        self.source={'path':str(source),'sha256':digest(source)}
        self.started_monotonic=time.monotonic()
        attr=None;attr_initialized=False;handles=[];readers=[]
        try:
            self.job=CreateJob(None,None);check(self.job,'CreateJobObjectW')
            info=EXTENDED_LIMIT();info.BasicLimitInformation.LimitFlags=0x2000|0x8
            info.BasicLimitInformation.ActiveProcessLimit=16
            # Real API rejection before CreateProcess, injected only for test.
            size=1 if fail_containment else C.sizeof(info)
            check(SetJob(self.job,9,C.byref(info),size),'SetInformationJobObject')
            verify=EXTENDED_LIMIT()
            check(QueryJob(self.job,9,C.byref(verify),C.sizeof(verify),None),'QueryExtendedLimits')
            flags=W.DWORD();check(HandleFlags(self.job,C.byref(flags)),'GetJobHandleFlags')
            if verify.BasicLimitInformation.LimitFlags!=0x2008 or flags.value&1:
                raise ControlError('JOB_POLICY_MISMATCH',0)
            self.policy={'atomic_job_assignment':'PROC_THREAD_ATTRIBUTE_JOB_LIST',
                         'limit_flags':int(verify.BasicLimitInformation.LimitFlags),
                         'kill_on_last_handle_close':True,'breakaway_allowed':False,
                         'job_handle_inheritable':False,'active_process_limit':16,
                         'hidden_window':True,'only_explicit_stdio_handles_inherited':True}
            sa=SECURITY_ATTRIBUTES(C.sizeof(SECURITY_ATTRIBUTES),None,True)
            writes=[]
            for _ in range(2):
                r=W.HANDLE();w=W.HANDLE()
                check(CreatePipe(C.byref(r),C.byref(w),C.byref(sa),0),'CreatePipe')
                readers.append(r.value);handles.append(w.value);writes.append(w.value)
                check(SetHandleFlags(r,1,0),'NoninheritablePipeReader')
            null=CreateFile('NUL',0x80000000,3,C.byref(sa),3,0,None)
            check(null and null!=C.c_void_p(-1).value,'CreateFileNUL');handles.append(null)
            n=SIZE();InitAttrs(None,2,0,C.byref(n))
            if not n.value:raise ControlError('AttributeListSize')
            attr=C.create_string_buffer(n.value)
            check(InitAttrs(attr,2,0,C.byref(n)),'InitializeAttributes');attr_initialized=True
            stdios=(W.HANDLE*3)(null,*writes);jobs=(W.HANDLE*1)(self.job)
            check(UpdateAttrs(attr,0,0x20002,stdios,C.sizeof(stdios),None,None),'StdioHandleList')
            check(UpdateAttrs(attr,0,0x2000D,jobs,C.sizeof(jobs),None,None),'AtomicJobList')
            si=STARTUPINFOEX();si.StartupInfo.cb=C.sizeof(si);si.lpAttributeList=C.cast(attr,PTR)
            si.StartupInfo.dwFlags=0x100|1;si.StartupInfo.wShowWindow=0
            si.StartupInfo.hStdInput=null;si.StartupInfo.hStdOutput=writes[0];si.StartupInfo.hStdError=writes[1]
            pi=PROCESS_INFORMATION();command=C.create_unicode_buffer(subprocess.list2cmdline(argv))
            check(CreateProcess(str(Path(argv[0]).resolve()),command,None,None,True,
                                0x08000000|0x80000|4,None,str(self.job_dir),C.byref(si),C.byref(pi)),
                  'CreateProcessW_atomic_job_suspended')
            self.pi=pi
            self.process_created=True;self.creation_call_success=True
            # Retain the authoritative CreateProcess handle before fallible
            # membership/path/identity observations. Never reacquire it by PID.
            self.members[int(pi.dwProcessId)]={'handle':pi.hProcess,'identity':None,'pid':int(pi.dwProcessId),
                'first_seen_monotonic':time.monotonic(),'membership_verified':None,'primary':True}
            if on_created:on_created(self)  # explicit own test observer only
            self.faults.hit('post_create_membership')
            inside=W.BOOL();check(InJob(pi.hProcess,self.job,C.byref(inside)),'VerifyPrimaryInJob')
            if not inside.value:raise ControlError('PRIMARY_OUTSIDE_JOB',0)
            self.members[int(pi.dwProcessId)]['membership_verified']=True
            self.faults.hit('post_create_identity')
            item=identity(pi.hProcess,pi.dwProcessId)
            if Path(item['image_path']).resolve()!=Path(argv[0]).resolve():raise ControlError('PRIMARY_IMAGE_MISMATCH',0)
            self.members[int(pi.dwProcessId)]['identity']=item
            self.faults.hit('constructor_snapshot')
            self.creation_snapshot=self.snapshots()
            self.faults.hit('capture_setup')
            for h in handles:Close(h)
            handles=[]
            for name in ['stdout','stderr']:
                h=readers.pop(0)
                try:self.captures[name]=Capture(h,capture_bytes)
                except BaseException:Close(h);raise
        except BaseException as error:
            # Snapshot errors are secondary observations, not cleanup gates.
            observed=attempt('constructor_error_snapshot',self.snapshots,self.faults)
            cleanup=None
            try:
                cleanup=self.cleanup()
            except BaseException as secondary:
                cleanup={'status':'ERROR','owned_job_empty':None,'error':error_record(secondary,'constructor_cleanup'),
                         'exit_evidence_status':'UNKNOWN'}
            finally:
                closed=self.close()  # raw ownership cleanup, never fault-injected
            capture=self.finish_captures()
            evidence={'process_created':self.process_created,'creation_call_returned_success':self.creation_call_success,
                      'child_resumed':self.resumed,'atomic_containment_requested':True,
                      'source':self.source,'containment':self.policy,'constructor_error_snapshot':observed,
                      'cleanup':cleanup,'close':closed,'capture':capture}
            raise ConstructorFailure(error,evidence) from error
        finally:
            if attr_initialized:DeleteAttrs(attr)
            for h in handles+readers:Close(h)
    def resume(self):
        check(ResumeThread(self.pi.hThread)!=0xffffffff,'ResumeThread')
        self.resumed=True;self.resumed_monotonic=time.monotonic()
        Close(self.pi.hThread);self.pi.hThread=None
    def ids(self):
        buf=C.create_string_buffer(8+16*C.sizeof(SIZE))
        check(QueryJob(self.job,3,buf,len(buf),None),'QueryOwnedJobProcessList')
        count=W.DWORD.from_buffer(buf,4).value
        if count>16:raise ControlError('JOB_MEMBER_CAP',0)
        return list((SIZE*count).from_buffer(buf,8))
    def observe(self):
        ids=self.ids()
        for pid in ids:
            if pid in self.members:continue
            if len(self.members)>=64:raise ControlError('OBSERVED_MEMBER_CAP',0)
            h=OpenProcess(0x1000|0x100000,False,pid)
            if not h:continue  # exited before observation; never cleanup by PID
            try:
                inside=W.BOOL();check(InJob(h,self.job,C.byref(inside)),'VerifyMemberInJob')
                if not inside.value:continue
                self.members[pid]={'handle':h,'identity':identity(h,pid),'pid':pid,'first_seen_monotonic':time.monotonic(),
                                   'membership_verified':True,'primary':False};h=None
            finally:
                if h:Close(h)
        return ids
    def snapshots(self):
        return [{**(v['identity'] or {'pid':v['pid'],'creation_filetime_100ns':None,'creation_time_utc':None,'image_path':None}),
                 'identity_status':'KNOWN' if v['identity'] else 'UNKNOWN','primary':v['primary'],'membership_verified':v['membership_verified'],
                 'first_seen_monotonic':v['first_seen_monotonic'],'exit':exit_observation(v['handle'])}
                for v in self.members.values()]
    def kill(self,code=0xE0000001):check(TerminateJob(self.job,code),'TerminateOwnedJobObject')
    def bounded_exit_audit(self,seconds=2):
        start=time.monotonic();end=start+seconds;last_job=None;job_error=None;waits={}
        while True:
            q=attempt('raw_owned_job_query',self.ids)
            if q['status']=='OBSERVED':last_job=q['value'];job_error=None
            else:last_job=None;job_error=q['error']
            for pid,v in self.members.items():
                w=Wait(v['handle'],0)
                waits[pid]={'wait_result':int(w),'signaled':True if w==0 else False if w==258 else None}
                if w==0xffffffff:waits[pid]['error']=error_record(ControlError('WaitForSingleObject'),'retained_handle_wait')
            complete=last_job==[] and all(w['signaled'] is True for w in waits.values())
            if complete or time.monotonic()>=end:break
            time.sleep(.01)
        records=[]
        for pid,v in self.members.items():
            result={**(v['identity'] or {'pid':pid,'creation_filetime_100ns':None,'image_path':None}),
                    'identity_status':'KNOWN' if v['identity'] else 'UNKNOWN','primary':v['primary'],
                    'handle_origin':'retained CreateProcess result' if v['primary'] else 'verified owned job member',
                    **waits[pid]}
            code=W.DWORD();ok=GetExit(v['handle'],C.byref(code))
            result['exit_code']=int(code.value) if ok else None
            if not ok:result['exit_code_error']=error_record(ControlError('GetExitCodeProcess'))
            records.append(result)
        value=True if complete else None if last_job is None or any(w['signaled'] is None for w in waits.values()) else False
        return {'owned_job_empty':value,'active_job_PIDs':last_job,'job_query_error':job_error,
                'retained_handle_waits':records,'deadline_monotonic':end,'elapsed_seconds':time.monotonic()-start,
                'exit_evidence_status':'CONFIRMED' if complete else 'UNKNOWN' if value is None else 'NOT_CONFIRMED_WITHIN_BOUND'}
    def cleanup(self):
        result={'status':'AUDITED','method':'Owned unnamed job only; unconditional close in finally','owned_job_empty':None}
        try:
            result['pre_snapshot']=attempt('cleanup_snapshot_before',self.snapshots,self.faults)
            result['member_observation']=attempt('cleanup_member_observation',self.observe,self.faults) if self.job else {'status':'NOT_CREATED'}
            if self.job:
                # Observation errors cannot block termination of the job we
                # own. This action and close are never fault-injected.
                result['termination']=attempt('TerminateJobObject',lambda:self.kill(0xE0000004))
                result['exit_audit']=attempt('cleanup_wait',self.bounded_exit_audit,self.faults)
                if result['exit_audit']['status']=='OBSERVED':
                    result['owned_job_empty']=result['exit_audit']['value']['owned_job_empty']
                result['post_snapshot']=attempt('cleanup_snapshot_after',self.snapshots,self.faults)
            else:
                result['exit_audit']={'status':'NOT_CREATED' if not self.process_created else 'UNKNOWN'}
                result['owned_job_empty']=None
        finally:
            result['close']=self.close()
        return result
    def finish_captures(self):
        result={}
        for name,capture in self.captures.items():
            r=attempt('capture_'+name,lambda c=capture,n=name:c.finish(self.job_dir/(n+'.bin'),self.faults,'capture_'+n))
            if r['status']=='ERROR':
                # Reader join/summary is separately guarded; no write retry.
                joined=attempt('capture_reader_join',lambda c=capture:c.thread.join(1))
                summary=attempt('capture_partial_summary',capture.summary)
                r.update(persisted=False,partial_summary=summary,reader_join=joined)
            result[name]=r
        return result
    def close(self):
        if self.closed:return self.close_result
        self.closed=True
        result=[];self.close_result=result
        # Each close is guarded independently. A metadata error cannot skip
        # the sole job handle, primary thread or other retained process handles.
        handles=[]
        if self.job:handles.append(('sole_noninherited_job',self.job));self.job=None
        if self.pi and self.pi.hThread:handles.append(('primary_thread',self.pi.hThread));self.pi.hThread=None
        unique=set()
        for v in self.members.values():
            if v['handle'] not in unique:handles.append(('retained_process',v['handle']));unique.add(v['handle'])
        if self.pi and self.pi.hProcess not in unique:handles.append(('primary_process',self.pi.hProcess))
        for kind,h in handles:
            def close_one(handle=h):
                check(Close(handle),'CloseHandle');return True
            result.append({'kind':kind,**attempt('CloseHandle',close_one)})
        return result

def disk_bytes(directory,max_files=256):
    total=0;count=0
    for base,dirs,files in os.walk(directory,followlinks=False):
        for name in dirs+files:
            count+=1
            if count>max_files:raise ControlError('DISK_SCAN_ENTRY_CAP',0)
            path=Path(base)/name
            try:stat=path.stat(follow_symlinks=False)
            except FileNotFoundError:continue
            if stat.st_file_attributes&0x400:raise ControlError('JOB_REPARSE_REFUSED',0)
        for name in files:
            try:total+=(Path(base)/name).stat(follow_symlinks=False).st_size
            except FileNotFoundError:pass
    return total

def run_owned(argv,job_dir,timeout_seconds=2,capture_bytes=8192,disk_limit_bytes=1048576,
              disk_poll_seconds=.05,fail_containment=False,on_started=None,fault_plan=None,on_created=None):
    """The deadline thread is independent of stdout draining/disk scans.

    Disk is polling, never a hard quota. Own directory only, bounded scans.
    It cannot constrain writes elsewhere, egress, hostile privileged children
    or external broker-created processes. No exporter qualification is implied.
    """
    job_dir=owned(job_dir)
    if not .1<=timeout_seconds<=30 or not .01<=disk_poll_seconds<=.5 or not 1<=disk_limit_bytes<=16*1024*1024:
        raise ControlError('INVALID_BUDGET',0)
    if job_dir.exists():raise ControlError('FRESH_JOB_REQUIRED',0)
    job_dir.mkdir(parents=True)
    start=time.monotonic();deadline=start+timeout_seconds
    receipt={'schema':'owned-process-error-safe-runtime-v2','argv':argv,'job_dir':str(job_dir),
             'controller_pid':os.getpid(),'start_monotonic':start,'deadline_monotonic':deadline,
             'timeout_seconds':timeout_seconds,'child_resumed':None,'process_created':None,'status':'NOT_STARTED',
             'owned_job_empty':None,'disk_limit_is_hard_quota':False,'disk_poll_seconds':disk_poll_seconds,
             'failure':None,'observation_failures':[],'fault_plan_declared':dict(fault_plan or {})}
    faults=FaultPlan(fault_plan);ctor_evidence=None
    process=None;stop=threading.Event();trigger=[];watcher=None;samples=[];next_disk=start;sample_count=0
    def timeout_watchdog():
        while not stop.wait(max(0,min(.01,deadline-time.monotonic()))):
            now=time.monotonic()
            if now>=deadline:
                trigger.append({'reason':'WALL_DEADLINE','trigger_monotonic':now,'deadline_overshoot_seconds':now-deadline})
                try:process.kill()
                except Exception as e:trigger[-1]['kill_error']=str(e)
                return
    try:
        process=ContainedProcess(argv,job_dir,capture_bytes,fail_containment,faults,on_created)
        receipt['process_created']=process.process_created;receipt['creation_call_returned_success']=process.creation_call_success
        receipt['child_resumed']=process.resumed
        receipt['source']=process.source;receipt['containment']=process.policy
        receipt['primary_identity']=process.members[int(process.pi.dwProcessId)]['identity']
        if time.monotonic()>=deadline:raise ControlError('DEADLINE_BEFORE_RESUME',0)
        watcher=threading.Thread(target=timeout_watchdog,daemon=True);watcher.start()
        process.resume();receipt['child_resumed']=True;receipt['resume_monotonic']=process.resumed_monotonic
        if on_started:on_started(process)
        while True:
            process.observe()
            if trigger:receipt['status']='TIMEOUT';break
            if exit_observation(process.pi.hProcess)['signaled']:
                receipt['status']='PRIMARY_EXITED';break
            now=time.monotonic()
            if now>=next_disk:
                faults.hit('disk_poll_scan')
                size=disk_bytes(job_dir);finished=time.monotonic()
                samples.append({'scan_start_monotonic':now,'scan_end_monotonic':finished,'bytes':size})
                sample_count+=1
                if len(samples)>512:samples.pop(0)  # bounded observation history
                next_disk=finished+disk_poll_seconds
                if size>disk_limit_bytes:
                    trigger.append({'reason':'DISK_POLL_THRESHOLD','trigger_monotonic':finished,
                                    'observed_bytes':size,'observed_overshoot_bytes':size-disk_limit_bytes})
                    receipt['status']='DISK_THRESHOLD';break
            time.sleep(.005)
    except ConstructorFailure as e:
        receipt.update(status='CONSTRUCTOR_FAILED',failure=e.cause)
        ctor_evidence=e.process_evidence
        receipt['constructor_failure_evidence']=ctor_evidence
        for k in ['process_created','creation_call_returned_success','child_resumed','source','containment']:
            receipt[k]=ctor_evidence.get(k)
    except ControlError as e:
        receipt.update(status='CONTROL_REFUSED',failure=error_record(e))
    except BaseException as e:
        receipt.update(status='CONTROLLER_EXCEPTION',failure=error_record(e))
    finally:
        stop.set()
        if watcher:
            receipt['watcher_join']=attempt('watcher_join',lambda:watcher.join(1))
            receipt['watcher_stopped']=not watcher.is_alive()
        receipt['trigger']=trigger
        if process:
            receipt['process_created']=process.process_created;receipt['child_resumed']=process.resumed
            try:
                receipt['cleanup']=process.cleanup()
            except BaseException as e:
                receipt['cleanup']={'status':'ERROR','owned_job_empty':None,'error':error_record(e,'outer_cleanup'),
                                    'exit_evidence_status':'UNKNOWN'}
            finally:
                receipt['close']=process.close()
            receipt['owned_job_empty']=receipt['cleanup'].get('owned_job_empty')
            capture=attempt('capture_finalization',process.finish_captures)
            receipt['capture']=capture['value'] if capture['status']=='OBSERVED' else {'finalization':capture}
        elif ctor_evidence is not None:
            # Evidence from constructor's own unconditional cleanup. The
            # caller's missing object reference proves no lifecycle outcome.
            receipt['cleanup']=ctor_evidence['cleanup'];receipt['close']=ctor_evidence['close']
            receipt['owned_job_empty']=ctor_evidence['cleanup'].get('owned_job_empty')
            receipt['capture']=ctor_evidence['capture']
        else:
            receipt['cleanup']={'status':'UNKNOWN','owned_job_empty':None,
                    'reason':'No object or attached constructor evidence; no exit inference permitted'}
            receipt['capture']={}
        final_scan=attempt('final_disk_scan',lambda:disk_bytes(job_dir),faults)
        final_size=final_scan['value'] if final_scan['status']=='OBSERVED' else None
        receipt['disk_monitor']={'samples':samples,'requested_poll_seconds':disk_poll_seconds,
              'sample_count_total':sample_count,'retained_sample_cap':512,'final_scan':final_scan,
              'max_observed_sample_gap_seconds':max([samples[i]['scan_start_monotonic']-samples[i-1]['scan_start_monotonic'] for i in range(1,len(samples))] or [0]),
              'final_bytes_before_receipt':final_size,'threshold_bytes':disk_limit_bytes,
              'final_overshoot_bytes':max(0,final_size-disk_limit_bytes) if final_size is not None else None,
              'post_cleanup_capture_bytes_included':True,'hard_quota':False,
              'limit':'Writes may exceed threshold between polls, during scan and cleanup; other directories are not monitored.'}
        # Preserve original failure separately from all secondary observation
        # failures. Missing metadata never overwrites known exit proof.
        def collect(x,path):
            if isinstance(x,dict):
                if x.get('status')=='ERROR' and 'error' in x:
                    receipt['observation_failures'].append({'path':path,'error':x['error']})
                for k,v in x.items():
                    if k!='observation_failures':collect(v,path+'/'+str(k))
            elif isinstance(x,list):
                for i,v in enumerate(x):collect(v,path+'/'+str(i))
        for k in ['cleanup','constructor_failure_evidence','capture','disk_monitor','watcher_join']:
            if k in receipt:collect(receipt[k],k)
        receipt['fault_events']=faults.events
        receipt['evidence_state']='PARTIAL_WITH_ERRORS' if receipt['failure'] or receipt['observation_failures'] else 'OBSERVED'
        receipt['finished_monotonic']=time.monotonic();receipt['elapsed_seconds']=receipt['finished_monotonic']-start
        receipt['receipt_persistence']={'status':'WRITTEN','path':str(job_dir/'runtime.json'),'flush_and_fsync':True}
        persisted=attempt('runtime_receipt_write',lambda:save(job_dir/'runtime.json',receipt))
        if persisted['status']=='ERROR':
            # Physical storage failure cannot be called a durable receipt.
            # Return the complete in-memory evidence to the original caller.
            receipt['receipt_persistence']={'status':'ERROR','error':persisted['error'],'in_memory_evidence_returned':True}
    return receipt

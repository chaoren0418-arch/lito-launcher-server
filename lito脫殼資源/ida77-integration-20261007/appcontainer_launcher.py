"""Trusted descendant launcher; accepted watchdog owns all lifetime limits.

No registration, fallback, new job, external process lookup or security edits.
"""
import sys
sys.dont_write_bytecode=True
import ctypes as C
from ctypes import wintypes as W
import json,os,subprocess,socket,time
from pathlib import Path
from owned_process import (HERE,owned,save,check,api,InitAttrs,UpdateAttrs,DeleteAttrs,
    SIZE,PTR,STARTUPINFOEX,PROCESS_INFORMATION,CreateProcess,ResumeThread,InJob,
    identity,Wait,GetExit,Close,error_record,digest)
from private_profile import derive,FreeSid,OpenToken,token_value,sid_text,create_private_directory,registration_state
GetStd=api('GetStdHandle',[W.DWORD],W.HANDLE)
Terminate=api('TerminateProcess',[W.HANDLE,W.UINT])
class CAPS(C.Structure):
    _fields_=[('AppContainerSid',PTR),('Capabilities',PTR),('CapabilityCount',W.DWORD),('Reserved',W.DWORD)]

def token_receipt(process):
    token=PTR();check(OpenToken(process,8,C.byref(token)),'OpenOwnedChildToken')
    try:
        values={}
        for key,kind in [('is_appcontainer',29),('is_lpac',46),('capability_count',30)]:
            buf=token_value(token,kind);values[key]=W.DWORD.from_buffer(buf).value
        for key,kind in [('appcontainer_sid',31),('integrity_sid',25)]:
            buf=token_value(token,kind);values[key]=sid_text(PTR.from_buffer(buf).value)
        return values
    finally:Close(token)

def main():
    mode=sys.argv[1];job=owned(sys.argv[2]);profile=json.loads((HERE/'private-profile-candidate.json').read_text('utf-8'))
    runtime=owned(profile['runtime_root']);sid=derive(profile['moniker']);attr=None;initialized=False;pi=PROCESS_INFORMATION()
    record={'mode':mode,'created':False,'resumed':False,'source_watchdog_sha256':digest(HERE/'owned_process.py'),
        'fresh_profile_directory':str(job/'profile-output'),'app_profile_registration_called':False}
    listener=None
    try:
        parent_in_job=W.BOOL();check(InJob(-1,None,C.byref(parent_in_job)),'LauncherInJob')
        if not parent_in_job.value:raise RuntimeError('Accepted watchdog job required')
        output=job/'profile-output';record['new_directories']=[create_private_directory(output,profile['owner_sid'],profile['app_sid'],True)]
        for name in ['usr','temp','home']:
            record['new_directories'].append(create_private_directory(output/name,profile['owner_sid'],profile['app_sid'],True))
        environ={k:os.environ[k] for k in ['SYSTEMROOT','WINDIR'] if k in os.environ}
        environ.update({'PATH':str(runtime)+';'+os.environ['SYSTEMROOT']+'\\System32',
            'IDADIR':str(runtime),'IDAUSR':str(output/'usr'),'PYTHONHOME':str(runtime),'PYTHONDONTWRITEBYTECODE':'1',
            'IDA_NO_HISTORY':'1','APPDATA':str(output/'home'),'LOCALAPPDATA':str(output/'home'),
            'USERPROFILE':str(output/'home'),'HOME':str(output/'home'),'TMP':str(output/'temp'),'TEMP':str(output/'temp'),
            'LITO_IDA_JOB':str(output),'LITO_RUNTIME':str(runtime),'LITO_APP_SID':profile['app_sid'],
            'LITO_CANARY':str(HERE/'outside-private-canary.txt')})
        if mode=='probe':
            listener=socket.socket();listener.bind(('127.0.0.1',0));listener.listen(1);listener.settimeout(.05)
            environ['LITO_LOCAL_PORT']=str(listener.getsockname()[1]);exe=runtime/'python.exe';args=[str(exe),'-B','isolation_probe.py']
        elif mode=='ida':
            qualification=json.loads((HERE/'isolation-qualification.json').read_text('utf-8'))
            if qualification.get('decision')!='QUALIFIED_PRIVATE_LPAC':raise RuntimeError('Isolation has not qualified')
            exe=runtime/'idat.exe';args=[str(exe),'-A','-L'+str(output/'ida.log'),'-o'+str(output/'fixture.i64'),'-Sexport_fixture.py','fixture.exe']
        else:raise ValueError('Only probe or ida modes permitted')
        record['environment']=environ;record['argv']=args
        n=SIZE();InitAttrs(None,3,0,C.byref(n));check(n.value,'AttributeSize')
        attr=C.create_string_buffer(n.value);check(InitAttrs(attr,3,0,C.byref(n)),'InitializeLPACAttributes');initialized=True
        std=[GetStd(W.DWORD(v)) for v in [-10,-11,-12]];handles=(W.HANDLE*3)(*std)
        caps=CAPS(sid,None,0,0);policy=W.DWORD(1)
        check(UpdateAttrs(attr,0,0x20002,handles,C.sizeof(handles),None,None),'ExplicitStdioHandles')
        check(UpdateAttrs(attr,0,0x20009,C.byref(caps),C.sizeof(caps),None,None),'EmptySecurityCapabilities')
        check(UpdateAttrs(attr,0,0x2000F,C.byref(policy),C.sizeof(policy),None,None),'LPACAllApplicationsOptOut')
        si=STARTUPINFOEX();si.StartupInfo.cb=C.sizeof(si);si.lpAttributeList=C.cast(attr,PTR)
        si.StartupInfo.dwFlags=0x101;si.StartupInfo.wShowWindow=0
        si.StartupInfo.hStdInput,si.StartupInfo.hStdOutput,si.StartupInfo.hStdError=std
        env=C.create_unicode_buffer('\0'.join(k+'='+v for k,v in sorted(environ.items()))+'\0\0')
        command=C.create_unicode_buffer(subprocess.list2cmdline(args))
        check(CreateProcess(str(exe),command,None,None,True,0x08000000|0x80000|4|0x400,env,str(runtime),C.byref(si),C.byref(pi)),
            'CreateProcessW_private_LPAC_suspended')
        record['created']=True;record['identity']=identity(pi.hProcess,pi.dwProcessId)
        inside=W.BOOL();check(InJob(pi.hProcess,None,C.byref(inside)),'NativeChildInInheritedJob');record['in_inherited_job']=bool(inside.value)
        record['token_before_resume']=token_receipt(pi.hProcess)
        tok=record['token_before_resume']
        if not inside.value or tok!={'is_appcontainer':1,'is_lpac':1,'capability_count':0,'appcontainer_sid':profile['app_sid'],'integrity_sid':'S-1-16-4096'}:
            raise RuntimeError('Actual suspended child isolation token mismatch; no resume')
        value=ResumeThread(pi.hThread)
        if value==0xFFFFFFFF:raise OSError(C.get_last_error(),'ResumeOwnedLPACChild')
        record['resumed']=True;record['resume_monotonic']=time.monotonic()
        wait=Wait(pi.hProcess,30000);record['wait_result']=int(wait)
        code=W.DWORD();check(GetExit(pi.hProcess,C.byref(code)),'GetOwnedNativeChildExit');record['exit_code']=code.value
        if wait!=0:raise RuntimeError('Child not signaled within bound; outer watchdog remains authoritative')
        if listener:
            try:
                conn,_=listener.accept();conn.close();record['listener_accepted_connection']=True
            except socket.timeout:record['listener_accepted_connection']=False
        record['registration_state_after']=registration_state(profile['app_sid'],profile['moniker'])
    except BaseException as e:
        record['error']=error_record(e)
        if pi.hProcess:
            record['owned_native_terminate_result']=bool(Terminate(pi.hProcess,80));record['owned_native_cleanup_wait']=int(Wait(pi.hProcess,2000))
    finally:
        if listener:listener.close()
        if pi.hThread:Close(pi.hThread)
        if pi.hProcess:Close(pi.hProcess)
        if initialized:DeleteAttrs(attr)
        FreeSid(sid)
        save(job/'native-launch.json',record)
    print(json.dumps({'mode':mode,'created':record['created'],'resumed':record['resumed'],'exit_code':record.get('exit_code'),'error':record.get('error')}))
    return 0 if record.get('exit_code')==0 and 'error' not in record else 81
if __name__=='__main__':sys.exit(main())

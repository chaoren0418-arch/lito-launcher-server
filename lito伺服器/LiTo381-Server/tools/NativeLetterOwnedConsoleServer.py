from pathlib import Path
import sys
sys.dont_write_bytecode=True
import subprocess,ctypes,json,time,os,re
from ctypes import wintypes

k=ctypes.WinDLL('kernel32',use_last_error=True)
k.OpenProcess.argtypes=[wintypes.DWORD,wintypes.BOOL,wintypes.DWORD];k.OpenProcess.restype=wintypes.HANDLE
k.GetProcessTimes.argtypes=[wintypes.HANDLE,*([ctypes.POINTER(wintypes.FILETIME)]*4)];k.GetProcessTimes.restype=wintypes.BOOL
k.CloseHandle.argtypes=[wintypes.HANDLE];k.CloseHandle.restype=wintypes.BOOL
def filetime(pid):
    h=k.OpenProcess(0x1000,False,pid)
    assert h,ctypes.get_last_error()
    try:
        values=[wintypes.FILETIME() for _ in range(4)]
        assert k.GetProcessTimes(h,*[ctypes.byref(v) for v in values]),ctypes.get_last_error()
        return str((values[0].dwHighDateTime<<32)|values[0].dwLowDateTime)
    finally:k.CloseHandle(h)
def write(p,o):p.write_text(json.dumps(o,indent=2),encoding='utf-8')

if sys.argv[1]=='--datadir-id':
    directory=Path(sys.argv[2]).resolve()
    assert directory.name=='data' and any(re.fullmatch('nw-[a-f0-9]{32}',p.name) for p in directory.parents)
    assert str(directory).isascii() and directory.is_dir()
    class NativeFileInfo(ctypes.Structure):
        _fields_=[(name,wintypes.DWORD) for name in ['attributes','creation_low','creation_high','access_low','access_high','write_low','write_high','volume_serial','size_high','size_low','links','index_high','index_low']]
    k.CreateFileW.argtypes=[wintypes.LPCWSTR,wintypes.DWORD,wintypes.DWORD,ctypes.c_void_p,wintypes.DWORD,wintypes.DWORD,wintypes.HANDLE];k.CreateFileW.restype=wintypes.HANDLE
    k.GetFileInformationByHandle.argtypes=[wintypes.HANDLE,ctypes.POINTER(NativeFileInfo)];k.GetFileInformationByHandle.restype=wintypes.BOOL
    h=k.CreateFileW(str(directory),0x80,7,None,3,0x02000000,None)
    assert h and h!=ctypes.c_void_p(-1).value,ctypes.get_last_error()
    try:
        info=NativeFileInfo();assert k.GetFileInformationByHandle(h,ctypes.byref(info)),ctypes.get_last_error()
        print(f'{info.volume_serial:08X}:{info.index_high:08X}{info.index_low:08X}:{(info.creation_high<<32)|info.creation_low}')
    finally:k.CloseHandle(h)
    sys.exit(0)

if sys.argv[1]=='--signal':
    pid=int(sys.argv[2]);expected=sys.argv[3]
    assert filetime(pid)==expected,'Owned Java identity changed'
    k.FreeConsole()
    k.AttachConsole.argtypes=[wintypes.DWORD];k.AttachConsole.restype=wintypes.BOOL
    assert k.AttachConsole(pid),ctypes.get_last_error()
    try:
        k.SetConsoleCtrlHandler.argtypes=[ctypes.c_void_p,wintypes.BOOL];k.SetConsoleCtrlHandler.restype=wintypes.BOOL
        assert k.SetConsoleCtrlHandler(None,True),ctypes.get_last_error()
        ids=(wintypes.DWORD*8)();k.GetConsoleProcessList.argtypes=[ctypes.POINTER(wintypes.DWORD),wintypes.DWORD]
        n=k.GetConsoleProcessList(ids,8)
        assert n==2 and set(ids[:n])=={pid,os.getpid()},'Console is not exclusive to owned Java and signal helper'
        k.GenerateConsoleCtrlEvent.argtypes=[wintypes.DWORD,wintypes.DWORD];k.GenerateConsoleCtrlEvent.restype=wintypes.BOOL
        assert filetime(pid)==expected
        assert k.GenerateConsoleCtrlEvent(0,0),ctypes.get_last_error()
        time.sleep(.5)
    finally:k.FreeConsole()
    sys.exit(0)

phase=Path(sys.argv[1]).resolve();jar=Path(sys.argv[2]).resolve();profile=Path(sys.argv[3]).resolve()
runtime=phase.parent
assert runtime.name.startswith('nw-') and phase.name in {'generation-1','generation-2','generation-3'}
assert profile.parent==runtime and jar.name=='LiTo381-Server-letter-auth-wire-candidate.jar'
java=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin/java.exe')
info=subprocess.STARTUPINFO();info.dwFlags|=subprocess.STARTF_USESHOWWINDOW;info.wShowWindow=subprocess.SW_HIDE
with (phase/'server.stdout.log').open('w',encoding='utf-8') as out,(phase/'server.stderr.log').open('w',encoding='utf-8') as err:
    jvm=[str(java),'-Xmx512m','-Dfile.encoding=UTF-8']
    if (phase/'transport-exceptions.jfc').exists():
        jvm.append('-XX:StartFlightRecording=name=transport,settings='+str(phase/'transport-exceptions.jfc')+',filename='+str(phase/'transport.jfr')+',dumponexit=true')
    child=subprocess.Popen([*jvm,'-jar',str(jar),str(profile)],cwd=runtime,stdout=out,stderr=err,creationflags=subprocess.CREATE_NEW_CONSOLE,startupinfo=info)
    expected=filetime(child.pid)
    write(phase/'server-console-owner.json',{'pid':child.pid,'creation_filetime_utc':expected,'exclusive_new_console':True,'hidden_window':True,'launcher_pid':os.getpid(),'launcher_creation_filetime_utc':filetime(os.getpid())})
    (phase/'server.pid.tmp').write_text(str(child.pid),encoding='ascii')
    (phase/'server.pid.tmp').replace(phase/'server.pid')
    passed=False
    try:
        deadline=time.monotonic()+180
        while not (phase/'normal-stop.request').exists():
            assert child.poll() is None,'Owned Java exited before requested normal stop'
            assert time.monotonic()<deadline,'Owned console lifecycle deadline'
            time.sleep(.05)
        # A peer EOF precedes final SQL/disconnect work. Observe actual server
        # threads before stopping, without changing the Hub or selection.
        deadline=time.monotonic()+20
        probes=0
        while True:
            assert filetime(child.pid)==expected
            q=subprocess.run([str(java.with_name('jcmd.exe')),str(child.pid),'Thread.print'],capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=6)
            probes+=1
            assert q.returncode==0,q.stderr
            (phase/'pre-stop-threads.txt').write_text(q.stdout,encoding='utf-8')
            if 'local.lito381.server.ClientSession.run(' not in q.stdout:break
            assert time.monotonic()<deadline,'Client disconnect/persistence did not quiesce'
            time.sleep(.1)
        write(phase/'client-quiescence.json',{'all_ClientSession_run_stacks_absent':True,'jcmd_probes':probes,'target_pid':child.pid,'target_filetime':expected,'read_only_diagnostic':'Thread.print'})
        q=subprocess.run([sys.executable,'-B','-X','utf8',str(Path(__file__).resolve()),'--signal',str(child.pid),expected],capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=5)
        assert q.returncode==0,q.stdout+'\n'+q.stderr
        code=child.wait(timeout=10)
        # HotSpot handles CTRL_C_EVENT as SIGINT, rather than TerminateProcess.
        assert code==130,('Unexpected JVM Ctrl+C exit',code)
        write(phase/'normal-stop-result.json',{'pid':child.pid,'creation_filetime_utc':expected,'exclusive_console_CTRL_C_EVENT':True,'exit_code':code,'forced_termination':False,'client_sessions_quiescent':True,'crash_recovery_qualified':False})
        passed=True
    finally:
        if child.poll() is None:
            # Owned fallback retirement is failure cleanup, never a normal-stop proof.
            assert filetime(child.pid)==expected
            child.terminate();child.wait(timeout=10)
            write(phase/'forced-failure-cleanup.json',{'pid':child.pid,'creation_filetime_utc':expected,'forced_termination':True})
        if not passed:sys.stderr.write('Normal restart boundary not qualified; owned failure cleanup only.\n')

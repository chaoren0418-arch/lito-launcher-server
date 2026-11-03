"""Formal standard Main with read-only shared Lineage sources; Windows only, no native client.
CLI --experiment: one bounded startup/READY/endpoint/cleanup run.
CLI --serve: future authorized Launcher adapter, READY JSON then stdin {"command":"stop"}
or EOF or 900-second ceiling; retains JVM handles until reverse cleanup.
Import Pair(run_id) as a context manager for an in-process Python caller.
"""
from pathlib import Path
import argparse, ctypes, hashlib, json, os, queue, re, socket, struct
import subprocess, sys, threading, time, traceback
from ctypes import wintypes as W
from datetime import datetime, timezone

CANDIDATE_ROOT = Path(__file__).resolve().parent
PROJECT = CANDIDATE_ROOT.parents[3] / 'lito伺服器/LiTo381-Server'
JAVA = Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin/java.exe')
JAR = CANDIDATE_ROOT / 'LiTo381-SharedResources-candidate.jar'
CATALOG = CANDIDATE_ROOT.parent / 'common_catalog.bin'
JAR_SHA256 = 'e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498'
CATALOG_SHA256 = 'b64ed920792805c3380ad0e50b1ae089a8779b4adddec2fbf3780c2fa88a3c01'
ROLES = [('standard', '127.0.0.1', '127.0.0.2', 32000)]
OUTPUT_KEYS = ('lito.rental.handoff.file', 'capture.directory', 'state.file')
TEMPLATE_HASHES = {'rental-source':'55158AA8D575985B4D1F3131BA6EB4D058189CE2B39CC38AEA70B53266D02097',
                   'rental-battle':'13A8B485FC3A8ACC4EA4D958CA0A6B1D3534B82375FBD52918C1C7C45C8A5A95'}
kernel = ctypes.WinDLL('kernel32', use_last_error=True)
iph = ctypes.WinDLL('iphlpapi', use_last_error=True)
kernel.GetProcessId.argtypes=[W.HANDLE];kernel.GetProcessId.restype=W.DWORD
kernel.GetProcessTimes.argtypes=[W.HANDLE]+[ctypes.POINTER(W.FILETIME)]*4
kernel.GetProcessTimes.restype=W.BOOL
kernel.QueryFullProcessImageNameW.argtypes=[W.HANDLE,W.DWORD,W.LPWSTR,ctypes.POINTER(W.DWORD)]
kernel.QueryFullProcessImageNameW.restype=W.BOOL
iph.GetExtendedTcpTable.argtypes=[W.LPVOID,ctypes.POINTER(W.DWORD),W.BOOL,W.ULONG,W.ULONG,W.ULONG]
iph.GetExtendedTcpTable.restype=W.DWORD
ntdll = ctypes.WinDLL('ntdll')
shell = ctypes.WinDLL('shell32',use_last_error=True)
ntdll.NtQueryInformationProcess.argtypes=[W.HANDLE,W.ULONG,W.LPVOID,W.ULONG,ctypes.POINTER(W.ULONG)]
ntdll.NtQueryInformationProcess.restype=W.LONG
shell.CommandLineToArgvW.argtypes=[W.LPCWSTR,ctypes.POINTER(ctypes.c_int)]
shell.CommandLineToArgvW.restype=ctypes.POINTER(ctypes.c_void_p)
kernel.LocalFree.argtypes=[ctypes.c_void_p];kernel.LocalFree.restype=ctypes.c_void_p

class ProcessBasicInformation(ctypes.Structure):
    _fields_=[('ExitStatus',W.LONG),('PebBaseAddress',ctypes.c_void_p),
              ('AffinityMask',ctypes.c_size_t),('BasePriority',W.LONG),
              ('UniqueProcessId',ctypes.c_size_t),('InheritedFromUniqueProcessId',ctypes.c_size_t)]
class UnicodeString(ctypes.Structure):
    _fields_=[('Length',W.USHORT),('MaximumLength',W.USHORT),('Buffer',ctypes.c_void_p)]

def utc(): return datetime.now(timezone.utc).isoformat()
def pin(path):
    path=Path(path);data=path.read_bytes()
    return {'path':str(path.resolve()),'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest().upper()}
def save(path, obj):
    Path(path).write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
def lifetime_identity(process):
    handle=W.HANDLE(int(process._handle))
    pid=kernel.GetProcessId(handle)
    if not pid: raise ctypes.WinError(ctypes.get_last_error())
    times=[W.FILETIME() for _ in range(4)]
    if not kernel.GetProcessTimes(handle,*(ctypes.byref(t) for t in times)):
        raise ctypes.WinError(ctypes.get_last_error())
    return {'pid':pid,'birth_filetime':(times[0].dwHighDateTime<<32)|times[0].dwLowDateTime,
            'identity_source':'retained Popen process handle'}
def identity(process):
    result=lifetime_identity(process);handle=W.HANDLE(int(process._handle))
    size=W.DWORD(32768);image=ctypes.create_unicode_buffer(size.value)
    if not kernel.QueryFullProcessImageNameW(handle,0,image,ctypes.byref(size)):
        raise ctypes.WinError(ctypes.get_last_error())
    result['image']=image.value
    return result
def same_identity(a,b):
    return a['pid']==b['pid'] and a['birth_filetime']==b['birth_filetime'] and os.path.normcase(a['image'])==os.path.normcase(b['image'])
def full7_identity(process, creation_identity, expected_image_pin, expected_argv, expected_parent_pid):
    """Observe only this retained live handle; expected inputs validate, never supply observed values."""
    if process.poll() is not None: raise RuntimeError('Full7 capture requires a live owned process')
    current=identity(process)
    if not same_identity(current,creation_identity): raise RuntimeError('Full7 creation identity mismatch')
    handle=W.HANDLE(int(process._handle));basic=ProcessBasicInformation();returned=W.ULONG()
    status=ntdll.NtQueryInformationProcess(handle,0,ctypes.byref(basic),ctypes.sizeof(basic),ctypes.byref(returned))
    if status!=0: raise RuntimeError('Full7 basic information query failed: '+str(status))
    parent=int(basic.InheritedFromUniqueProcessId)
    if int(basic.UniqueProcessId)!=current['pid'] or parent<=0 or parent!=expected_parent_pid:
        raise RuntimeError('Full7 observed parent/PID mismatch')
    needed=W.ULONG()
    ntdll.NtQueryInformationProcess(handle,60,None,0,ctypes.byref(needed))
    if needed.value<ctypes.sizeof(UnicodeString) or needed.value>65536:
        raise RuntimeError('Full7 observed command buffer unavailable')
    buffer=ctypes.create_string_buffer(needed.value)
    status=ntdll.NtQueryInformationProcess(handle,60,buffer,needed.value,ctypes.byref(returned))
    if status!=0: raise RuntimeError('Full7 command query failed: '+str(status))
    command_info=UnicodeString.from_buffer(buffer);address=ctypes.addressof(buffer)
    pointer=int(command_info.Buffer or 0);length=int(command_info.Length)
    if not length or length%2 or length>command_info.MaximumLength or pointer<address+ctypes.sizeof(UnicodeString) or pointer+length>address+needed.value:
        raise RuntimeError('Full7 command buffer range invalid')
    command=ctypes.string_at(pointer,length).decode('utf-16-le')
    count=ctypes.c_int();vector=shell.CommandLineToArgvW(command,ctypes.byref(count))
    if not vector: raise ctypes.WinError(ctypes.get_last_error())
    try:
        if count.value<=0 or count.value>32768: raise RuntimeError('Full7 observed argv unavailable')
        argv=[ctypes.wstring_at(vector[i]) for i in range(count.value)]
    finally: kernel.LocalFree(ctypes.cast(vector,ctypes.c_void_p))
    if argv!=list(expected_argv): raise RuntimeError('Full7 observed argv mismatch')
    if not Path(current['image']).is_absolute(): raise RuntimeError('Full7 observed image is not absolute')
    image=pin(current['image'])
    if os.path.normcase(image['path'])!=os.path.normcase(expected_image_pin['path']) or image['bytes']!=expected_image_pin['bytes'] or image['sha256']!=expected_image_pin['sha256']:
        raise RuntimeError('Full7 observed image pin mismatch')
    if process.poll() is not None: raise RuntimeError('Full7 process exited during capture')
    if not same_identity(identity(process),current): raise RuntimeError('Full7 identity changed during capture')
    after_basic=ProcessBasicInformation()
    status=ntdll.NtQueryInformationProcess(handle,0,ctypes.byref(after_basic),ctypes.sizeof(after_basic),ctypes.byref(returned))
    if status!=0 or int(after_basic.InheritedFromUniqueProcessId)!=parent or int(after_basic.UniqueProcessId)!=current['pid']:
        raise RuntimeError('Full7 observed parent/PID changed during capture')
    return {'pid':current['pid'],'filetime':str(current['birth_filetime']),'path':current['image'],
            'image_sha256':image['sha256'],
            'command_sha256':hashlib.sha256(command.encode('utf-8')).hexdigest().upper(),
            'command_argv_digest':hashlib.sha256(json.dumps(argv,ensure_ascii=False,separators=(',',':')).encode('utf-8')).hexdigest().upper(),
            'parent_pid':parent}

def tcp_rows():
    result=[]
    for family in (socket.AF_INET,socket.AF_INET6):
        size=W.DWORD();err=iph.GetExtendedTcpTable(None,ctypes.byref(size),False,family,5,0)
        if err not in (0,122): raise OSError(err,'GetExtendedTcpTable size')
        buf=ctypes.create_string_buffer(size.value)
        err=iph.GetExtendedTcpTable(buf,ctypes.byref(size),False,family,5,0)
        if err: raise OSError(err,'GetExtendedTcpTable snapshot')
        data=buf.raw[:size.value];count=struct.unpack_from('<I',data)[0]
        stride=24 if family==socket.AF_INET else 56
        for n in range(count):
            offset=4+n*stride
            if family==socket.AF_INET:
                state,la,lp,ra,rp,pid=struct.unpack_from('<6I',data,offset)
                local=socket.inet_ntop(family,struct.pack('<I',la));remote=socket.inet_ntop(family,struct.pack('<I',ra))
            else:
                la,ls,lp,ra,rs,rp,state,pid=struct.unpack_from('<16sII16sIIII',data,offset)
                local=socket.inet_ntop(family,la);remote=socket.inet_ntop(family,ra)
            result.append({'family':family,'local_host':local,'local_port':socket.ntohs(lp&65535),
                           'remote_host':remote,'remote_port':socket.ntohs(rp&65535),'state':state,'pid':pid})
    return result
def props(data):
    result={}
    for line in data.decode('utf-8').splitlines():
        s=line.strip()
        if not s or s.startswith(('#','!')): continue
        if '=' not in s or s.endswith('\\'): raise ValueError('Unexpected property syntax')
        key,value=s.split('=',1)
        if key in result: raise ValueError('Duplicate property '+key)
        result[key]=value
    return result

def shared_configuration():
    config_path=PROJECT/'config/server.properties'
    cp=str(JAR)+';'+str(PROJECT/'lib/*')
    result=subprocess.run([str(JAVA),'-Dfile.encoding=UTF-8','-cp',cp,'local.lito381.server.UnifiedConfiguration',str(config_path)],
                          capture_output=True,text=True,encoding='utf-8',timeout=20,creationflags=subprocess.CREATE_NO_WINDOW)
    if result.returncode: raise ValueError('單一設定／資料庫來源清單驗證失敗：'+result.stderr.strip())
    return json.loads(result.stdout)['settings']

def verify_release_inputs():
    if hashlib.sha256(JAR.read_bytes()).hexdigest()!=JAR_SHA256.lower(): raise ValueError('Candidate JAR changed')
    if hashlib.sha256(CATALOG.read_bytes()).hexdigest()!=CATALOG_SHA256.lower(): raise ValueError('Candidate common catalog changed')

def prepare(run_id):
    verify_release_inputs()
    if not re.fullmatch(r'[a-zA-Z0-9][a-zA-Z0-9_-]{0,63}',run_id): raise ValueError('Invalid run id')
    values=shared_configuration()
    if values['lito.rental.town.directory.enabled']!='false' or values.get('lito.channel.ports') or values.get('lito.rental.teleport.routes'):
        raise ValueError('Shared standard arena only')
    (CANDIDATE_ROOT/'runs').mkdir(exist_ok=True)
    run=CANDIDATE_ROOT/'runs'/run_id;run.mkdir(exist_ok=False)
    (run/'config').mkdir();(run/'outputs').mkdir()
    values['capture.directory']=str(run/'outputs/captures').replace('\\','/')
    path=run/'config/server.dev.standard.properties'
    path.write_text('configuration.base.file='+str((PROJECT/'config/server.properties').resolve()).replace('\\','/')+'\ncapture.directory='+values['capture.directory']+'\n',encoding='utf-8')
    config={'role':'standard','config':pin(path),'outputs':{'state.file':values['state.file']},
            'login':values['bind'],'game':values['game.bind'],'port':int(values['port']),'persistence_enabled':'false','effective':values,'jvm_options':['-Xms'+values['memory.initial'],'-Xmx'+values['memory.maximum'],'-XX:ActiveProcessorCount='+values['cpu.limit']]}
    save(run/'config-audit.json',{'service_mode':'standard','base_config':pin(PROJECT/'config/server.properties'),
         'runtime_changes':['capture.directory','absolute paths'],'database_mode':'lito_mysql','database_read_only':True,
         'dynamic_databases':values['database.dynamic.databases'].split(','),'instances':[config]})
    return run,[config]

class Pair:
    def __init__(self,run_id):
        self.run,self.configs=prepare(run_id);self.owned=[];self.start=time.monotonic()
        self.report={'task_id':'development-sql-login-repair-20261011',
                     'accepted_source_task_id':'server-memory-entry-terminal-cleanup-fix-20261010',
                     'source_task_id':'server-isolated-memory-rental-startup-restore-20261010',
                     'started_at':utc(),'run_id':run_id,'product_complete':False,
                     'scope':'formal standard Main; shared read-only source databases; character_lito results only; single JVM; retained owned cleanup',
                     'state':'prepared','instances':[],'endpoint_checks':[],'cleanup':[],
                     'sql_started':False,'sql_connected':False,'native_started':False,'payload_bytes_sent':0}
        self.persist()
    def persist(self): save(self.run/'result.json',self.report)
    def record_failure(self, error, phase):
        failure={'type':type(error).__name__,'message':str(error),'phase':phase,'at':utc()}
        self.report.setdefault('failure',failure)
        self.report.setdefault('execution_errors',[]).append(failure)
        self.report.update(accepted=False,state='sealed-failed')
    def publication_failed(self, error, phase):
        self.report.setdefault('publication_errors',[]).append(
            {'type':type(error).__name__,'message':str(error),'phase':phase,'at':utc()})
        self.report.update(accepted=False,state='sealed-failed')
    def publish_result(self, phase):
        try:
            self.persist()
            return True
        except BaseException as error:
            self.publication_failed(error,phase)
            return False
    def has_failure(self):
        return bool(self.report.get('failure') or self.report.get('execution_errors')
                    or self.report.get('publication_errors') or self.report.get('cleanup_errors'))
    def preflight(self):
        rows=tcp_rows();targets=[]
        for c in self.configs:
            effective=c['effective']
            base=int(effective.get('login.source.port.base','32000'))
            ports=[c['port']]+[base+int(v) for v in effective['login.classic.server.ids'].split(',')]
            targets += [(ip,port) for ip in (c['login'],c['game']) for port in ports]
        relevant=[x for x in rows if x['state']==2 and any(x['local_port']==port and x['local_host'] in (ip,'0.0.0.0','::') for ip,port in targets)]
        self.report['preflight']={'at':utc(),'listeners':relevant,'exclusive_bind_checks':[]}
        if relevant: self.persist();raise RuntimeError('Endpoint occupied; no JVM started; no retry')
        for ip,port in targets:
            try:
                with socket.socket() as sock:
                    sock.setsockopt(socket.SOL_SOCKET,socket.SO_EXCLUSIVEADDRUSE,1);sock.bind((ip,port))
                row={'host':ip,'port':port,'exclusive_bind_available':True}
            except OSError as e:
                row={'host':ip,'port':port,'exclusive_bind_available':False,'error':str(e),'winerror':e.winerror}
                self.report['preflight']['exclusive_bind_checks'].append(row);self.persist()
                raise RuntimeError('Endpoint occupied/unavailable; no retry') from e
            self.report['preflight']['exclusive_bind_checks'].append(row)
        self.persist()
    def check_alive(self):
        for owned in self.owned:
            p=owned['process']
            def exited():
                detail=''
                log=self.run/(owned['role']+'.stderr.log')
                if log.exists():
                    text=log.read_text(encoding='utf-8',errors='replace')
                    match=re.search(r"Access denied for user '[^']+'@'[^']+' to database '[^']+'",text)
                    if match: detail='; '+match.group(0)
                return RuntimeError('JVM exited early: '+owned['role']+' (exit='+str(p.returncode)+')'+detail+'; log='+str(log))
            if p.poll() is not None: raise exited()
            try: current=identity(p)
            except OSError:
                # Windows can deny image lookup during teardown before the exit handle signals.
                try: p.wait(timeout=1)
                except subprocess.TimeoutExpired: raise
                raise exited() from None
            if not same_identity(current,owned['identity']): raise RuntimeError('Owned identity changed')
    def start_pair(self):
        self.preflight();self.report['state']='starting';self.persist()
        expected_java=json.loads((PROJECT/'deployment/release-inputs.json').read_bytes())['java_runtime']
        owner_pid=int(kernel.GetProcessId(W.HANDLE(-1)))
        if not owner_pid: raise RuntimeError('Creation owner kernel PID unavailable')
        for config in self.configs:
            self.check_alive()
            if pin(config['config']['path'])!=config['config']: raise RuntimeError('Config drift')
            role=config['role'];stdout=self.run/(role+'.stdout.log');stderr=self.run/(role+'.stderr.log')
            out=stdout.open('xb');err=stderr.open('xb')
            args=[str(JAVA),*config['jvm_options'],'-Dlito.resourceProfile=tw13081901-v1','-Dlito.extendedItemDetails=true','-Dlito.commonCatalog='+str(CATALOG),'-jar',str(JAR),config['config']['path'],'--service-mode=standard']
            # Original formal Main, database/config/authentication retained; common resource profile is explicit.
            env=dict(os.environ)
            for key in ('JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS','CLASSPATH'):
                if env.get(key):
                    out.close();err.close();raise RuntimeError('External Java injection variable set: '+key)
            deadline=time.monotonic()+15
            try: p=subprocess.Popen(args,cwd=PROJECT,stdin=subprocess.DEVNULL,stdout=out,stderr=err,
                                    creationflags=subprocess.CREATE_NO_WINDOW,env=env)
            except BaseException: out.close();err.close();raise
            owned={'process':p,'role':role,'identity':None,'streams':(out,err)};self.owned.append(owned)
            owned['identity']=identity(p)
            owned['identity']['full7']=full7_identity(p,owned['identity'],expected_java,args,owner_pid)
            assert owned['identity']['pid']==p.pid and os.path.normcase(owned['identity']['image'])==os.path.normcase(str(JAVA))
            record={'role':role,'argv':args,'cwd':str(PROJECT),'identity':owned['identity'],
                    'config':config['config'],'launch_at':utc(),'ready_timeout_seconds':15,
                    'stdout':str(stdout),'stderr':str(stderr),'ready':False}
            self.report['instances'].append(record);self.persist()
            while time.monotonic()<deadline:
                self.check_alive();data=stdout.read_bytes()
                if b'[LiTo] ready' in data.splitlines():
                    texts=[data.decode(encoding,errors='replace') for encoding in ('utf-8','cp950','cp936')]
                    required=['[LiTo] resource-profile=tw13081901-v1 catalog-sha256='+CATALOG_SHA256+' sprites=30285','LiTo381 Server 0.7.864','[LiTo] login-listen=127.0.0.1:32000',
                              '[LiTo] game-listen=127.0.0.2:[32000] shared-channel-endpoint',
                              '[LiTo] state=memory(non-persistent)']
                    effective=config['effective']
                    required += ['dynamic-database='+db+', resource-database='+effective['database.static.database']+', read-only=true' for db in effective['database.dynamic.databases'].split(',')]
                    required += ['shared-source='+db+' controls='+effective['database.lito.database']+' source-read-only=true write-table=character_lito' for db in effective['database.dynamic.databases'].split(',')]
                    if not any(all(s in text for s in required) for text in texts): raise RuntimeError('Formal READY descriptors did not match')
                    record.update(ready=True,ready_at=utc(),ready_elapsed_seconds=round(time.monotonic()-(deadline-15),4),
                                  ready_marker='[LiTo] ready',descriptors_required=required)
                    self.persist();break
                time.sleep(.03)
            else: raise TimeoutError(role+' READY deadline exceeded')
        self.check_alive()
        rows=tcp_rows();save(self.run/'ready-tcp-snapshot.json',rows)
        for config,owned in zip(self.configs,self.owned):
            for kind,host in [('LOGIN',config['login']),('GAME',config['game'])]:
                self.check_alive()
                found=[x for x in rows if x['state']==2 and x['local_host']==host and x['local_port']==config['port']]
                if len(found)!=1 or found[0]['pid']!=owned['identity']['pid']: raise RuntimeError('Endpoint owner mismatch')
                began=time.monotonic()
                with socket.create_connection((host,config['port']),timeout=2): pass
                self.check_alive()
                check={'role':config['role'],'kind':kind,'host':host,'port':config['port'],
                       'owner':found[0],'connect':True,'payload_bytes_sent':0,
                       'elapsed_seconds':round(time.monotonic()-began,4),'at':utc()}
                self.report['endpoint_checks'].append(check);self.persist()
        self.report['source_endpoint_checks']=[]
        for config,owned in zip(self.configs,self.owned):
            effective=config['effective']
            base=int(effective.get('login.source.port.base','32000'))
            for source_id in map(int,effective['login.classic.server.ids'].split(',')):
                for kind,host in [('LOGIN',config['login']),('GAME',config['game'])]:
                    port=base+source_id
                    found=[x for x in rows if x['state']==2 and x['local_host']==host and x['local_port']==port]
                    if len(found)!=1 or found[0]['pid']!=owned['identity']['pid']: raise RuntimeError('Source endpoint owner mismatch')
                    with socket.create_connection((host,port),timeout=2) as connection:
                        if not connection.recv(2): raise RuntimeError('Source endpoint missing native key')
                    self.report['source_endpoint_checks'].append({'source_id':source_id,'kind':kind,'host':host,'port':port,'owner':found[0],'native_key_received':True})
        for owned,record in zip(self.owned,self.report['instances']):
            current_full7=full7_identity(owned['process'],owned['identity'],expected_java,record['argv'],owner_pid)
            if current_full7!=owned['identity']['full7']: raise RuntimeError('Full7 changed before pair READY')
        self.report.update(state='ready',pair_ready_at=utc(),startup_ready_verified=True,sql_connected=True,
                           startup_elapsed_seconds=round(time.monotonic()-self.start,4))
        self.persist();return self
    def cleanup(self):
        errors=[]
        for owned in reversed(self.owned):
            p=owned['process'];row={'role':owned['role'],'at':utc(),'retained_handle':True,
                                  'expected_identity':owned['identity'],'terminated_by_owner':False}
            try:
                now=lifetime_identity(p);row['observed_lifetime_identity']=now
                if owned['identity'] is None:
                    owned['identity']=identity(p);row['expected_identity']=owned['identity']
                if any(now[k]!=owned['identity'][k] for k in ('pid','birth_filetime')): raise RuntimeError('Cleanup lifetime identity mismatch')
                if p.poll() is None:
                    try:
                        row['observed_identity']=identity(p)
                        if not same_identity(row['observed_identity'],owned['identity']): raise RuntimeError('Cleanup image identity mismatch')
                        # Retained handle only; the process can exit during the image check.
                        p.terminate();row['terminated_by_owner']=True
                    except OSError:
                        # Let an already exiting process signal; never act on a reused PID.
                        p.wait(timeout=1)
                row['exit_code']=p.wait(timeout=3);row['exited']=True
                row['post_exit_lifetime_identity']=lifetime_identity(p)
                assert all(row['post_exit_lifetime_identity'][k]==owned['identity'][k] for k in ('pid','birth_filetime'))
                for stream in owned['streams']: stream.close()
                p._handle.Close();row['handle_closed']=True
            except BaseException as e:
                row['error']=f'{type(e).__name__}: {e}';errors.append(row['error'])
            self.report['cleanup'].append(row)
            self.publish_result('cleanup-child-'+owned['role'])
        self.report['cleanup_errors']=errors
        rows=tcp_rows()
        try: save(self.run/'after-cleanup-tcp-snapshot.json',rows)
        except BaseException as error: self.publication_failed(error,'cleanup-tcp-snapshot')
        pids={o['identity']['pid'] for o in self.owned if o['identity']}
        listeners=[x for x in rows if x['state']==2 and x['pid'] in pids]
        relevant=[x for x in rows if x['state']==2 and any(x['local_host'] in (ip,'0.0.0.0','::') and x['local_port']==c['port'] for c in self.configs for ip in (c['login'],c['game']))]
        self.report.update(cleanup_order=[x['role'] for x in self.report['cleanup']],
                           owned_listeners_remaining=listeners,target_listeners_after_cleanup=relevant,
                           cleanup_verified=not errors and not listeners and all(x.get('exited') and x.get('handle_closed') for x in self.report['cleanup']),
                           finished_at=utc(),elapsed_seconds=round(time.monotonic()-self.start,4))
        self.report['accepted']=bool(self.report.get('startup_ready_verified') and self.report['cleanup_verified']
                                     and not relevant and not self.has_failure())
        self.report['state']='sealed-success' if self.report['accepted'] else 'sealed-failed'
        self.publish_result('cleanup-terminal-result')
        if errors or self.report.get('publication_errors'):
            raise RuntimeError('Owned cleanup/publication failure: '+json.dumps(
                {'cleanup_errors':errors,'publication_errors':self.report.get('publication_errors',[])},
                ensure_ascii=False))
    def __enter__(self):
        try: return self.start_pair()
        except BaseException as e:
            self.record_failure(e,'enter')
            self.publish_result('enter-failure-before-cleanup')
            self.cleanup()
            raise
    def __exit__(self,typ,value,tb):
        if typ is not None: self.record_failure(value,'context-body')
        self.cleanup()

def serve(pair):
    commands=queue.Queue()
    def reader():
        for line in sys.stdin: commands.put(line)
        commands.put(None)
    threading.Thread(target=reader,daemon=True).start()
    print(json.dumps({'event':'ready','run':str(pair.run),'instances':pair.report['instances'],
                      'endpoints':pair.report['endpoint_checks'],'product_complete':False},ensure_ascii=False),flush=True)
    limit=time.monotonic()+900
    while time.monotonic()<limit:
        pair.check_alive()
        try: line=commands.get(timeout=.25)
        except queue.Empty: continue
        if line is None: return
        if json.loads(line).get('command')=='stop': return
        raise ValueError('Unsupported control command')
    raise TimeoutError('900-second session ceiling')

def finish(pair, exit_code):
    if exit_code or pair.has_failure() or not pair.report.get('accepted'):
        pair.report.update(accepted=False,state='sealed-failed')
        exit_code=1
    if not pair.publish_result('entry-terminal-result'): exit_code=1
    try: save(pair.run/'entry-exit.json',{'exit_code':exit_code,'at':utc()})
    except BaseException as error:
        pair.publication_failed(error,'entry-exit-receipt')
        exit_code=1
        pair.publish_result('entry-exit-failure-result')
    event={'event':'finished','exit_code':exit_code,'result':str(pair.run/'result.json'),
           'accepted':bool(exit_code==0 and pair.report.get('accepted') and not pair.has_failure()),
           'product_complete':False}
    try: print(json.dumps(event,ensure_ascii=False),flush=True)
    except BaseException as error:
        pair.publication_failed(error,'finished-event')
        exit_code=1
        pair.publish_result('finished-event-failure-result')
        try: save(pair.run/'entry-exit.json',{'exit_code':exit_code,'at':utc()})
        except BaseException as receipt_error: pair.publication_failed(receipt_error,'finished-event-exit-receipt')
    return exit_code

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--run-id',required=True)
    modes=parser.add_mutually_exclusive_group(required=True)
    modes.add_argument('--prepare-only',action='store_true');modes.add_argument('--experiment',action='store_true');modes.add_argument('--serve',action='store_true')
    args=parser.parse_args()
    if args.prepare_only:
        run,configs=prepare(args.run_id);print(json.dumps({'event':'prepared','run':str(run),'configs':configs},ensure_ascii=False));return 0
    pair=None;exit_code=1
    try:
        pair=Pair(args.run_id)
        with pair:
            if args.serve: serve(pair)
        exit_code=0 if pair.report['accepted'] else 1
    except BaseException as error:
        if pair: pair.record_failure(error,'main')
        traceback.print_exc()
    finally:
        if pair: exit_code=finish(pair,exit_code)
    return exit_code
if __name__=='__main__': sys.exit(main())

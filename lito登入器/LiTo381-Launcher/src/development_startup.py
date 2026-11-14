"""Direct development Server -> original native launcher flow. Source-selected authentication; shared read-only Lineage databases."""
from pathlib import Path
import argparse, ctypes, importlib.util, json, os, queue, shutil, subprocess, sys, threading, time, traceback
from ctypes import wintypes as W

PROJECT = Path(__file__).resolve().parent
# Published helper is beside the EXE; source helper lives under src.
LAUNCHER = Path('D:/lineage開發/3-lito開發/lito登入器/LiTo381-Launcher')
LITO = LAUNCHER.parent.parent
SERVER = LITO / 'lito伺服器/LiTo381-Server'
DELIVERY = Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app')
SERVER_DELIVERY = DELIVERY / 'server'
CLIENT = Path('D:\\lineage開發\\0-客戶端')
SELECTED = CLIENT
ENTRY = SERVER_DELIVERY / 'Start-Shared-World.py'
PROBE = DELIVERY / 'lito_runtime_probe_named.py'
DLL = DELIVERY / 'compat/litoddraw.dll'
PINS = {
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\shared_bootstrap.py'): 'E500B0D628079892768AA218C7FE98D678C019D4726A7114A88A0CD4170FD098',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\native_arrow_interruption.py'): '184ECE834D22607E2F5B29A8123CFCC92BFF890417F4A43B5DD5D1EBB291A95E',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\native_bounds_guards.py'): 'A078E6FDE5D3A6FB779081E971405934DDD8600315302AC36E70FCFB6F7CAA0A',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\native_resource_trace.py'): '9171F12112562B52776982E560CD2796F08EAF5BCC8B77037BD20FABDD61A413',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\native_tbt_hooks.py'): '9FDE6589A0F3AB2AA41FC75868C47088E62C7CACB4EFF22BFFE905D136EB6FE5',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\native_daynight_extension.py'): 'C4DDE41F4C02A8FC40E64CD1C97A19C3CE69FFABF65F94B430D87FADBEC2061F',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\native_fade_hooks.py'): '09C62481ABF6F8444BBB4BAA4DDAA74AD318FD08F31ED1839E806686D8366BDA',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\native_furniture_extension.py'): '8A7C1931C49AD43F4E99FC1232A24DBCD7AD3030A8790C1FB6E93B34C221FCCC',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\native_loader_hooks.py'): '7C489BBDD926C1CBD713D9F2E5C2BD192D277D5483804DE4488D438BF1248EBF',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\native_morph_extension.py'): '510DDDF8DBD2852FE2804C977DD253D59964D6562E76D1DDEAB94EA40F534F51',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\native_sound_extension.py'): '94D7DEA645586723CDA05BF13829A23DB5B609FF2195D1CE372EB4CEA7C9CDD2',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\native-layout-patches.json'): '6C084998C8792B530F79B6FDB020B501A9D8859E03918310D975003789F3890E',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\native_fade_extension.dll'): '54B5152C2169688C129DCCEC1945DDAF693F3FAF137F4C8C87B51CCB8D320148',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\common_catalog.bin'): 'B64ED920792805C3380AD0E50B1AE089A8779B4ADDDEC2FBF3780C2FA88A3C01',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\common_catalog_manifest.json'): '94D31E15E4556E590DA9E22A49794AC89D044B6B6F5762AE90C1EEDC0721C2E1',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\server\\LiTo381-SharedResources-candidate.jar'): 'E1498871C7BE6B287821C57EF5D95933DB1740D9B4D4A80F6C3465F21D99D498',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\compat\\litoddraw.dll'): 'E47BD6C6EC9979C8747A523E4457133802D19F673C8DC1E67707ABFA96216A8A',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\compat\\shared_status.bin'): 'BA91B436034CE04C5A77BC626AF1355EDE8766DD72A353B0AD0EC5AAF069B198',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\server\\lib\\mysql-connector-j-8.0.33.jar'): 'E2A3B2FC726A1AC64E998585DB86B30FA8BF3F706195B78BB77C5F99BF877BD9',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\server\\lib\\protobuf-java-3.21.9.jar'): '1B78B4A76A71512DEBFDFF8F8FC5AEF6BFD459F65758FECF7AFF245E6E6301E4',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\server\\lib\\HikariCP-5.1.0.jar'): 'A47A6EE62379694EE52C30036F0931B72F9AEE2A801D590341ED82BD839E2134',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\server\\lib\\slf4j-api-1.7.36.jar'): 'D3EF575E3E4979678DC01BF1DCCE51021493B4D11FB7F1BE8AD982877C16A1C0',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\resources\\class-icons\\LTicon.idx'): 'F7FC014FB3C2C723FA11B86E1929C24D325995055CB0E2889EA61C3CDC25AD9F',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\resources\\class-icons\\LTicon.pak'): '23DB693B33E03B576483ED572A58350DFC7A6CAF31D10AC364C223341EFF37B7',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\resources\\class-icons\\resources.json'): '6F777EF0CD1D94BD1009F4E601F81B131ECB53D2C80CF867439ABD6A86EE5A15',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\named_ddraw.py'): '4DE00027A104804695F25FD26E3DBD1D9A6EA6AAF3DE7E887AB177CE1BAC13BA',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\class_icon_bootstrap.py'): '345E9194A25AA2237D878CC1FEEBFA4AE5C123CAACA960C6DC909FF8592133D7',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\lito_runtime_probe_named.py'): '16311C07DDA9FD06118D628609E58FAA20D352FDF5EC65CFF29A16CF3C78F136',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\resources\\world\\inputs\\effectlist2.xml.extraction.json'): '12F86969AD93C69E13AF0A0A871CE655B9F82AE1FA403F5387BE053B22716AD0',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\resources\\world\\icon-assets.json'): 'BBFE020CEF51B190E0B0CF996320640BEDB86EE06C28308A84543C501BC17889',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\world_resource_preflight.py'): '353C362EC12D3443C790D707FBC26B50944081BC2D1DA9967A75D471CB289F34',
    Path('D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app\\server\\Start-Shared-World.py'): '971A99971F52BDFAA7E7BB916AB325EA8500E79C8F5F03C237E171F5A8BBE3C5',
}

# Issue #448 (user ruling 2026-11-14, option C): TW13081901.txt is the client's sprite
# action/rate data file and may be updated under the agreed rules, so its content is
# NOT pinned. Only existence + readability is checked. Do not add it back to PINS.
UNPINNED = (CLIENT/'TW13081901.txt',)

BIN_SHA = 'CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292'

def no_reparse(path):
    path = Path(path)
    for node in (path, *path.parents):
        if node.is_symlink() or node.is_junction(): raise ValueError('Reparse startup path refused: '+str(node))
    return path

def exact(path, sha):
    import hashlib
    path = no_reparse(path)
    if hashlib.sha256(path.read_bytes()).hexdigest().upper() != sha: raise ValueError('Startup file changed: '+str(path))

def readable(path):
    path = no_reparse(path)
    if not path.is_file(): raise ValueError('Startup file missing: '+str(path))
    try:
        with path.open('rb') as stream: stream.read(1)
    except OSError as error: raise ValueError('Startup file unreadable: '+str(path)+' ('+str(error)+')') from error

def emit(value): print(json.dumps(value, ensure_ascii=False), flush=True)
def save(path, value):
    with Path(path).open('x', encoding='utf-8') as stream: json.dump(value, stream, ensure_ascii=False, indent=2)

def check_source(source):
    if Path(source).resolve() != SELECTED.resolve(): raise ValueError('共用資源版只接受 D:/lineage開發/0-客戶端。')
    for path, sha in PINS.items(): exact(path, sha)
    for path in UNPINNED: readable(path)
    exact(SELECTED/'LiTo.bin', BIN_SHA); exact(CLIENT/'LiTo.bin', BIN_SHA)
    if str(DELIVERY) not in sys.path: sys.path.insert(0,str(DELIVERY))
    import world_resource_preflight
    world_resource_preflight.configure(CLIENT,DLL)

def native_args(service, run):
    values = [sys.executable, '-B', '-X', 'utf8', str(PROBE), '--executable', str(CLIENT/'LiTo.bin'),
              '--cwd', str(CLIENT), '--output', str(run/'native-probe.json'), '--timeout', '0',
              '--interval', '0.5', '--display-mode', 'windowed', '--install-gap-veh',
              '--conditional-patch', '0x00A4827C:75F6:9090', '--manual-native-selection',
              '--native-arena-transition', '--compat-layer', 'RunAsInvoker DWM8And16BitMitigation HIGHDPIAWARE',
              '--server-profile', str(run/'server-profile.json'),
              '--shared-resource-candidate', str(DELIVERY), '--multi-instance-startup-gate']
    if service == 'RentalServer': values.append('/ltpremiumsvr')
    return values

def select_source(effective, source_id):
    ids=[int(value) for value in effective['login.classic.server.ids'].split(',')]
    if source_id not in ids: raise ValueError('Selected source is not enabled')
    index=ids.index(source_id)
    name=effective['login.classic.server.names'].split(',')[index]
    database=effective['database.dynamic.databases'].split(',')[index]
    port=int(effective.get('login.source.port.base','32000'))+source_id
    if not 1025<=port<=65535: raise ValueError('Invalid selected source port')
    return {'schemaVersion':4,'classicSourceNames':[name],'staticDatabase':effective['database.static.database'],
            'dynamicDatabases':[database],'profileId':'lito-source-'+str(source_id),'displayName':name,
            'host':effective['bind'],'port':port,'originSourceId':'source-'+str(source_id),'originSourceName':name}

def source_catalog(effective, source_ids):
    rows=[]
    for source_id in source_ids:
        source=select_source(effective,source_id)
        name=source['displayName'].encode('cp950')
        if not 1 <= len(name) <= 63 or b'\0' in name: raise ValueError('Native source name exceeds 63 bytes')
        rows.append(str(source_id)+':'+str(source['port'])+':'+name.hex())
    return ';'.join(rows)

class StopSession(Exception): pass

class AdditionalNative:
    """One retained probe/job and native child; never owns the shared JVM."""
    def __init__(self,module,run):
        self.module=module;self.run=run;self.probe=None;self.handle=None;self.streams=[]
        self.report={'run':str(run),'cleanup_verified':False};self.cleaned=False
    def alive(self):return self.probe is not None and self.probe.poll() is None
    def start(self,service,profile,env,check):
        self.run.mkdir();save(self.run/'server-profile.json',profile)
        for name in ('native.stdout.log','native.stderr.log'):self.streams.append((self.run/name).open('wb'))
        # Native WinMain creates the fixed Lineage491284632 mutex, then checks
        # the Lineage window class/title. Give this owned child a unique mutex
        # and bypass its two singleton early-return branches. Keep the mutex
        # creation/cleanup and CreateWindow intact; patches are memory-only.
        import hashlib
        mutex=('LiTo'+hashlib.sha256(str(self.run).encode()).hexdigest()[:12]).encode('ascii')
        assert len(mutex)==len(b'Lineage491284632')
        command=native_args(service,self.run)
        if '--multi-instance-startup-gate' not in command: command.append('--multi-instance-startup-gate')
        insertion=command.index('--conditional-patch')
        command[insertion:insertion]=['--conditional-patch',
            '0x0062D320:'+b'Lineage491284632'.hex()+':'+mutex.hex(),
            '--conditional-patch','0x00480326:7409:EB09',
            '--conditional-patch','0x00480AB3:7407:EB07']
        self.report['unique_native_mutex']=mutex.decode();self.report['existing_window_guard']='0x480326:7409->EB09';self.report['mutex_guard']='0x480AB3:7407->EB07'
        self.probe=subprocess.Popen(command,cwd=CLIENT,stdin=subprocess.DEVNULL,
            stdout=self.streams[0],stderr=self.streams[1],creationflags=subprocess.CREATE_NO_WINDOW,env=env)
        self.report['probe_lifetime_identity']=self.module.lifetime_identity(self.probe)
        self.report['probe_identity']=self.module.identity(self.probe)
        deadline=time.monotonic()+40
        while time.monotonic()<deadline:
            check()
            if not self.alive():raise RuntimeError('Additional native launcher exited: '+str(self.probe.returncode))
            child=find_child(self.probe.pid,CLIENT/'LiTo.bin')
            if child:
                self.handle,identity=child;self.report['native_identity']=identity
                emit({'event':'session_native_created','identity':identity});break
            time.sleep(.1)
        else:raise TimeoutError('Additional native process missing after 40 seconds')
        deadline=time.monotonic()+90;receipt=self.run/'native-probe.shared-ready.json'
        while not receipt.exists():
            check()
            if not self.alive():raise RuntimeError('Additional native exited before resources loaded')
            if time.monotonic()>=deadline:raise TimeoutError('Additional native resources exceeded 90 seconds')
            time.sleep(.1)
        resources=json.loads(receipt.read_text('utf-8'))
        expected={'pid':identity['pid'],'profile':'tw13081901-v1','base_count':30000,'total_count':30285,'native_cleanup_complete':True}
        if resources!=expected:raise RuntimeError('Additional native resource identity/count mismatch')
        self.report['shared_resources_ready']=resources;save(self.run/'native-created.json',self.report)
        return identity['pid']
    def cleanup(self):
        if self.cleaned:return
        if self.probe:
            if self.probe.poll() is None:
                expected=self.report.get('probe_lifetime_identity')
                if expected and self.module.lifetime_identity(self.probe)!=expected:raise RuntimeError('Additional probe lifetime drift')
                self.probe.terminate() # Retained probe handle; kill-on-close job contains only its own BIN.
            self.report['exit_code']=self.probe.wait(timeout=5)
            if self.handle:
                if kernel.WaitForSingleObject(self.handle,5000)!=0:raise RuntimeError('Additional BIN did not exit')
                kernel.CloseHandle(self.handle);self.handle=None
            self.probe._handle.Close()
        for stream in self.streams:stream.close()
        self.cleaned=True;self.report['cleanup_verified']=True
        save(self.run/'terminal.json',self.report)

def main():
    parser=argparse.ArgumentParser(); parser.add_argument('--serve', action='store_true', required=True)
    parser.add_argument('--run-id', required=True); parser.add_argument('--client-directory', required=True)
    parser.add_argument('--scale', type=int, choices=(100,125,150,200), required=True)
    parser.add_argument('--service', choices=('StandardArena',), required=True)
    parser.add_argument('--source-id', type=int, choices=range(1,9), required=True)
    args=parser.parse_args(); check_source(args.client_directory)
    spec=importlib.util.spec_from_file_location('development_server_pair', ENTRY)
    module=importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
    selected=select_source(module.shared_configuration(), args.source_id)
    # Normal Main with real SQL repositories, retained ownership checks and fresh run outputs.
    SERVER_DELIVERY.mkdir(exist_ok=True); (SERVER_DELIVERY/'runs').mkdir(exist_ok=True)
    run=DELIVERY/'runs'/args.run_id; run.mkdir(parents=True, exist_ok=False)
    commands=queue.Queue()
    def reader():
        for line in sys.stdin: commands.put(line)
        commands.put(None)
    threading.Thread(target=reader, daemon=True).start()
    session_end=float("inf")
    def before_native():
        if time.monotonic()>=session_end: raise TimeoutError('Original development session ceiling reached')
        try: line=commands.get_nowait()
        except queue.Empty: return
        if line is None or json.loads(line).get('command')=='stop': raise StopSession()
        raise ValueError('Unknown owned control command')
    pair=None; probe=None; native_handle=None; report={'run_id':args.run_id,'game_window_verified':False,
        'source_client':str(SELECTED/'LiTo.bin'),'runtime_client':str(CLIENT/'LiTo.bin'),
        'bin_sha256':BIN_SHA,'service':args.service,'scale':args.scale,'cleanup_verified':False}
    native_streams=[]; exit_code=0; additional=[]; additional_receipts=[]; next_client=2
    try:
        os.environ['LITO_NUMERIC_BREAKTHROUGH']='4'
        os.environ['LITO_EXTENDED_STATS_SCHEMA']='2'
        pair=module.Pair(args.run_id)
        pair.start_pair()
        ready={'event':'ready','run':str(pair.run),'instances':pair.report['instances'],
               'endpoints':pair.report['endpoint_checks'],'source_endpoints':pair.report['source_endpoint_checks'],'product_complete':False,
               'database_mode':'lito_mysql','database_read_only':True,'selected_source_id':args.source_id,'service_mode':'standard','sql_connected':pair.report['sql_connected']}
        save(run/'server-ready.json',ready); emit(ready)
        pair.check_alive()
        before_native()
        owner=current_identity(module)
        save(run/'lease.json',{'run_id':args.run_id,'owner':owner,'created_at':module.utc(),
                              'server_instances':pair.report['instances'],'session_ceiling_seconds':None,
                              'expires_at_unix':None})
        # An exclusive fresh claim is consumed once, only after actual READY and current identities qualify.
        with (run/'native-claim.used').open('x',encoding='utf-8') as claim:
            json.dump({'run_id':args.run_id,'owner':owner,'server_ready':str(run/'server-ready.json')},claim)
        shutil.copyfile(DLL,CLIENT/'litoddraw.dll')
        (CLIENT/'lito381-compat.ini').write_text('[Window]\nWidth=640\nHeight=480\nScalePercent='+str(args.scale)+
            '\n[Diagnostics]\nLogging=1\n[NumericBreakthrough]\nEnabled=1\nSchema=4\n',encoding='ascii')
        render_log=CLIENT/'lito381-compat.log'
        report['render_log_initial_bytes']=render_log.stat().st_size if render_log.exists() else 0
        effective=pair.configs[0]['effective']
        current_selection=select_source(effective,args.source_id)
        if current_selection!=selected: raise ValueError('Selected source changed during startup')
        source_ids=[int(value) for value in effective['login.classic.server.ids'].split(',')]
        profile=dict(selected,localServerConfig=Path(pair.configs[0]['config']['path']).name,
            classicSourceNames=effective['login.classic.server.names'].split(','),
            dynamicDatabases=effective['database.dynamic.databases'].split(','),
            nativeSourceIds=source_ids,nativeSourcePortBase=int(effective.get('login.source.port.base','32000')))
        save(run/'server-profile.json',profile)
        native_env=dict(os.environ,LITO_NATIVE_LOGIN_HOST=effective['bind'],LITO_NATIVE_SOURCES=source_catalog(effective,source_ids))
        for key in ('LITO381_ACCOUNT','LITO381_PASSWORD','LITO381_CHARACTER','LITO381_CHARACTER_NAME'):
            native_env.pop(key,None)

        check_source(args.client_directory); pair.check_alive(); before_native()
        command=native_args(args.service,run); report['native_command']=command
        for name in ('native.stdout.log','native.stderr.log'): native_streams.append((run/name).open('wb'))
        probe=subprocess.Popen(command,cwd=CLIENT,stdin=subprocess.DEVNULL,stdout=native_streams[0],
                               stderr=native_streams[1],creationflags=subprocess.CREATE_NO_WINDOW,env=native_env)
        report['probe_lifetime_identity']=module.lifetime_identity(probe)
        report['probe_identity']=module.identity(probe)
        telemetry_stop=threading.Event()
        def telemetry():
            positions={record['stdout']:0 for record in pair.report['instances']};buffers={name:b'' for name in positions}
            while not telemetry_stop.wait(.25):
                for name in positions:
                    with Path(name).open('rb') as stream:
                        stream.seek(positions[name]);data=stream.read();positions[name]=stream.tell()
                    rows=(buffers[name]+data).split(b'\n');buffers[name]=rows.pop()
                    for line in rows:
                        if line.startswith(b'[LiTo-Extended-Stats] '): print(line.decode('cp950').rstrip(),file=sys.stderr,flush=True)
        threading.Thread(target=telemetry,daemon=True).start()
        # Observe the exact child via the standard process snapshot; do not invent a GUI acceptance result.
        deadline=min(time.monotonic()+40,session_end)
        while time.monotonic()<deadline:
            pair.check_alive()
            if probe.poll() is not None: raise RuntimeError('Native launcher exited: '+str(probe.returncode))
            child=find_child(probe.pid,CLIENT/'LiTo.bin')
            if child:
                native_handle, native_id=child; report['native_identity']=native_id
                save(run/'native-created.json',report)
                emit({'event':'native_created','identity':native_id,'run':str(run),'window_verified':False}); break
            time.sleep(.1)
        else: raise TimeoutError('Actual LiTo.bin child was not observed within 40 seconds')
        deadline=time.monotonic()+90
        resource_receipt=run/'native-probe.shared-ready.json'
        while not resource_receipt.exists():
            pair.check_alive(); before_native()
            if probe.poll() is not None: raise RuntimeError('Native exited before common resources became ready')
            if time.monotonic() >= deadline: raise TimeoutError('Common resources initialization exceeded 90 seconds')
            time.sleep(.1)
        resources=json.loads(resource_receipt.read_text(encoding='utf-8'))
        if resources != {'pid':native_id['pid'],'profile':'tw13081901-v1',
                         'base_count':30000,'total_count':30285,'native_cleanup_complete':True}:
            raise RuntimeError('Common resource readiness identity/count mismatch')
        report['shared_resources_ready']=resources
        emit({'event':'resources_ready', **resources})
        ceiling=session_end
        while probe.poll() is None or any(client.alive() for client in additional):
            for client in list(additional):
                if not client.alive():
                    client.cleanup();additional_receipts.append(client.report);additional.remove(client)
            pair.check_alive()
            if time.monotonic()>=ceiling: raise TimeoutError('Original development session ceiling reached')
            try: line=commands.get(timeout=.25)
            except queue.Empty: continue
            if line is None or json.loads(line).get('command')=='stop': break
            if json.loads(line).get('command')=='add_client':
                client=AdditionalNative(module,run/('client-'+str(next_client)));next_client+=1
                additional.append(client)
                try:
                    check_source(args.client_directory)
                    def check_extra():pair.check_alive();before_native()
                    pid=client.start(args.service,profile,native_env,check_extra)
                    emit({'event':'additional_client_result','success':True,'pid':pid,'run':str(client.run)})
                except StopSession:break
                except Exception as error:
                    client.report['failure']=str(error);client.cleanup()
                    additional_receipts.append(client.report);additional.remove(client)
                    emit({'event':'additional_client_result','success':False,'reason':str(error)})
                continue
            raise ValueError('Unknown owned control command')
        if probe.poll() is not None:
            report['native_probe_exit_code']=probe.returncode
            if probe.returncode != 0: raise RuntimeError('Native launcher ended with failure: '+str(probe.returncode))
    except StopSession:
        pass
    except BaseException as error:
        exit_code=1; report['failure']=str(error); traceback.print_exc(file=sys.stderr)
        emit({'event':'failed','reason':str(error)})
    finally:
        errors=[]
        for client in reversed(additional):
            try:client.cleanup();additional_receipts.append(client.report)
            except BaseException as error:errors.append('additional client cleanup: '+str(error))
        report['additional_clients']=additional_receipts
        if 'telemetry_stop' in locals(): telemetry_stop.set()
        try:
            if probe:
                if probe.poll() is None:
                    lifetime=module.lifetime_identity(probe)
                    if report.get('probe_lifetime_identity') and lifetime!=report['probe_lifetime_identity']: raise RuntimeError('Probe lifetime identity drift')
                    if report.get('probe_identity') and not module.same_identity(module.identity(probe),report['probe_identity']): raise RuntimeError('Probe identity drift')
                    probe.terminate() # Retained handle; its original kill-on-close Job owns the native child.
                report['probe_exit_code']=probe.wait(timeout=5)
                if native_handle:
                    if kernel.WaitForSingleObject(native_handle,5000)!=0: raise RuntimeError('Owned BIN did not exit')
                    kernel.CloseHandle(native_handle); native_handle=None
                    report['native_exit_verified']=True
                probe._handle.Close();report['probe_handle_closed']=True
        except BaseException as error: errors.append(str(error))
        for stream in native_streams:
            try: stream.close()
            except BaseException as error: errors.append('native stream close: '+str(error))
        try:
            if 'render_log' in locals() and render_log.exists():
                with render_log.open('rb') as stream:
                    stream.seek(report['render_log_initial_bytes'])
                    (run/'native-render.log').write_bytes(stream.read())
        except BaseException as error: report['render_capture_failure']=str(error)
        try:
            if pair: pair.cleanup()
        except BaseException as error: errors.append(str(error))
        report['cleanup_errors']=errors
        report['cleanup_verified']=bool(not errors and (pair.report.get('cleanup_verified') if pair else probe is None and native_handle is None))
        if errors: exit_code=1
        save(run/'terminal.json',report)
        emit({'event':'finished','exit_code':exit_code,'accepted':exit_code==0,'cleanup_verified':report['cleanup_verified'],
              'result':str(run/'terminal.json'),'product_complete':False})
    return exit_code

kernel=ctypes.WinDLL('kernel32',use_last_error=True)
kernel.OpenProcess.argtypes=[W.DWORD,W.BOOL,W.DWORD];kernel.OpenProcess.restype=W.HANDLE
kernel.CloseHandle.argtypes=[W.HANDLE];kernel.CloseHandle.restype=W.BOOL
kernel.WaitForSingleObject.argtypes=[W.HANDLE,W.DWORD];kernel.WaitForSingleObject.restype=W.DWORD
kernel.QueryFullProcessImageNameW.argtypes=[W.HANDLE,W.DWORD,W.LPWSTR,ctypes.POINTER(W.DWORD)]
kernel.GetProcessTimes.argtypes=[W.HANDLE]+[ctypes.POINTER(W.FILETIME)]*4
class PE(ctypes.Structure):
    _fields_=[('size',W.DWORD),('usage',W.DWORD),('pid',W.DWORD),('heap',ctypes.c_size_t),('module',W.DWORD),
              ('threads',W.DWORD),('parent',W.DWORD),('priority',W.LONG),('flags',W.DWORD),('exe',W.WCHAR*260)]
kernel.CreateToolhelp32Snapshot.argtypes=[W.DWORD,W.DWORD];kernel.CreateToolhelp32Snapshot.restype=W.HANDLE
kernel.Process32FirstW.argtypes=[W.HANDLE,ctypes.POINTER(PE)];kernel.Process32NextW.argtypes=[W.HANDLE,ctypes.POINTER(PE)]
def find_child(parent,expected):
    snapshot=kernel.CreateToolhelp32Snapshot(2,0); row=PE();row.size=ctypes.sizeof(row)
    try:
        ok=kernel.Process32FirstW(snapshot,ctypes.byref(row))
        while ok:
            if row.parent==parent and row.exe.lower()=='lito.bin':
                handle=kernel.OpenProcess(0x100000|0x1000,False,row.pid)
                if not handle: raise ctypes.WinError(ctypes.get_last_error())
                name=ctypes.create_unicode_buffer(32768);length=W.DWORD(len(name));times=[W.FILETIME() for _ in range(4)]
                if not kernel.QueryFullProcessImageNameW(handle,0,name,ctypes.byref(length)) or not kernel.GetProcessTimes(handle,*(ctypes.byref(t) for t in times)):
                    kernel.CloseHandle(handle);raise ctypes.WinError(ctypes.get_last_error())
                if os.path.normcase(name.value)!=os.path.normcase(str(expected)):
                    kernel.CloseHandle(handle);raise RuntimeError('Observed native image path mismatch')
                return handle,{'pid':row.pid,'parent_pid':parent,'path':name.value,
                    'filetime':(times[0].dwHighDateTime<<32)|times[0].dwLowDateTime,'image_sha256':BIN_SHA}
            ok=kernel.Process32NextW(snapshot,ctypes.byref(row))
    finally: kernel.CloseHandle(snapshot)

def current_identity(module):
    class Current: pass
    value=Current();value._handle=kernel.OpenProcess(0x1000,False,os.getpid())
    try: return module.identity(value)
    finally: kernel.CloseHandle(value._handle)

if __name__=='__main__': sys.exit(main())

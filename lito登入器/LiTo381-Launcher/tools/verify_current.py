from pathlib import Path
import hashlib,importlib.util,json,subprocess,sys,threading,queue,time
L=Path(__file__).resolve().parent.parent
REPO=L.parent.parent
def repo_path(value):
    path=Path(value)
    return path if path.is_absolute() else REPO/path
helper=L/'release/current/development_startup.py'
spec=importlib.util.spec_from_file_location('current_helper',helper);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
m.check_source(str(m.SELECTED))
release_inputs=REPO/'lito伺服器/LiTo381-Server/deployment/release-inputs.json'
pins=json.loads(release_inputs.read_text('utf-8-sig'))
expected_server=pins['server_sha256'].lower()
jar=repo_path(pins['server_jar']) if pins.get('server_jar') else L/'app/server/LiTo381-SharedResources-candidate.jar'
assert hashlib.sha256(jar.read_bytes()).hexdigest()==expected_server
formal_spec=importlib.util.spec_from_file_location('formal_helper',m.ENTRY)
formal=importlib.util.module_from_spec(formal_spec);formal_spec.loader.exec_module(formal)
formal.verify_release_inputs()
original_pins=(formal.JAR_SHA256,formal.CATALOG_SHA256)
try:
    for case in (str.lower,str.upper):
        formal.JAR_SHA256,formal.CATALOG_SHA256=map(case,original_pins)
        formal.verify_release_inputs()
    for field,message in [('JAR_SHA256','Candidate JAR changed'),('CATALOG_SHA256','Candidate common catalog changed')]:
        formal.JAR_SHA256,formal.CATALOG_SHA256=original_pins
        setattr(formal,field,'0'*64)
        try: formal.verify_release_inputs()
        except ValueError as error: assert str(error)==message
        else: raise AssertionError('Mismatched input accepted: '+field)
finally:
    formal.JAR_SHA256,formal.CATALOG_SHA256=original_pins
assert 'sprintf_s(info' not in (L/'native/item_details.inc').read_text('utf-8')
result={'pins':True,'server_release_verified':True,'formal_input_gate_verified':True,'hash_case_and_mismatch_regression':True,'footer_removed':True}
if '--native' in sys.argv:
    run='cleanup-smoke-'+str(time.time_ns());events=[];q=queue.Queue()
    with (L/'docs/startup-smoke.stderr.log').open('w',encoding='utf-8') as err:
        p=subprocess.Popen([sys.executable,'-B','-X','utf8',str(helper),'--serve','--run-id',run,'--client-directory',str(m.SELECTED),'--scale','200','--service','StandardArena','--source-id','1'],stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=err,text=True,encoding='utf-8',creationflags=subprocess.CREATE_NO_WINDOW)
        def reader():
            for line in p.stdout:q.put(line)
            q.put(None)
        threading.Thread(target=reader,daemon=True).start();deadline=time.monotonic()+120;ready=False
        try:
            while time.monotonic()<deadline:
                try:line=q.get(timeout=1)
                except queue.Empty:continue
                if line is None:break
                e=json.loads(line);events.append(e)
                print(e.get('event'),flush=True)
                if e.get('event')=='resources_ready':
                    from class_compat.verify_live import verify
                    result['class_compatibility']=verify(e['pid'])
                    from verify_class_icons import verify as verify_icons
                    icon_deadline=time.monotonic()+20
                    while True:
                        try:result['class_icons']=verify_icons(e['pid']);break
                        except AssertionError:
                            if time.monotonic()>=icon_deadline:raise
                            time.sleep(.25)
                    ready=True;break
                if e.get('event')=='failed':break
        finally:
            if p.poll() is None:
                p.stdin.write('{"command":"stop"}\n');p.stdin.flush()
            p.wait(timeout=30)
        terminal=json.loads((L/'app/runs'/run/'terminal.json').read_text('utf-8'))
        result.update(native_ready=ready,exit_code=p.returncode,cleanup_verified=terminal['cleanup_verified'],run=run)
        (L/'docs/startup-smoke.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
        assert ready and p.returncode==0 and terminal['cleanup_verified'],result
print(json.dumps(result))

"""Publish the user-accepted candidate with an exact-file rollback archive."""
from pathlib import Path
import hashlib,json,re,zipfile
R=Path(__file__).resolve().parent;L=R.parents[1];C=R/'build/class-icons'
S=L.parent.parent/'lito伺服器/LiTo381-Server'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def once(s,a,b):
    if s.count(a)!=1:raise ValueError('Publication seam changed: '+a)
    return s.replace(a,b)
def main():
    q=json.loads((C/'qualification.json').read_text('utf-8'))
    for item in [q['dll'],q['jar'],q['resources']]+q['dependencies']+q['tests']+q['sources']:
        if sha(Path(item['path']))!=item['sha256']:raise ValueError('Candidate evidence changed: '+item['path'])
    changes={}
    def data(p,b):changes[p]=b
    def text(p,t):data(p,t.encode('utf-8'))
    def copy(p,dest):data(dest,p.read_bytes())
    # Keep the exact original helper available to reproduce sealed candidates.
    frozen=R/'inputs/development_startup-0.6.69.py'
    oldjar=L/'app/server/LiTo381-SharedResources-candidate.jar'
    fixedjar=R/'inputs/formal-0.6.69.jar';copy(oldjar,fixedjar)
    frozen_text=(L/'release/current/development_startup.py').read_text('utf-8')
    frozen_text=once(frozen_text,repr(str(oldjar)),repr(str(fixedjar)))
    text(frozen,frozen_text)
    p=R/'candidate_startup.py';t=p.read_text('utf-8')
    text(p,once(t,'L / "release/current/development_startup.py"','R / "inputs/development_startup-0.6.69.py"'))
    # Promote byte-identical runtime artifacts; never rebuild the approved DLL/JAR here.
    copy(Path(q['dll']['path']),L/'app/compat/litoddraw.dll')
    copy(Path(q['jar']['path']),L/'app/server/LiTo381-SharedResources-candidate.jar')
    copy(C/'candidate/shared_status.bin',L/'app/compat/shared_status.bin')
    for item in q['dependencies']:copy(Path(item['path']),L/'app/server/lib'/Path(item['path']).name)
    for p in (C/'native').iterdir():
        if p.suffix in ('.inc','.h','.cpp','.def'):copy(p,L/'native'/p.name)
    resource=json.loads((C/'resources.json').read_text('utf-8'))
    for item in resource['package']:
        dest=L/'app/resources/class-icons'/Path(item['path']).name
        copy(Path(item['path']),dest);item['path']=str(dest)
    text(L/'app/resources/class-icons/resources.json',json.dumps(resource,ensure_ascii=False,indent=2))
    for name in ('named_ddraw.py','class_icon_bootstrap.py'):copy(R/name,L/'app'/name)
    probe=(C/'lito_runtime_probe_class_icons.py').read_text('utf-8')
    probe=once(probe,'sys.path[:0]='+repr([str(R),str(L/'app')]),'sys.path[:0]='+repr([str(L/'app')]))
    text(L/'app/lito_runtime_probe_named.py',probe)
    for src,dest in [('build/inputs/effectlist2.xml.extraction.json','inputs/effectlist2.xml.extraction.json'),
                     ('build/icon-assets.json','icon-assets.json')]:
        copy(R/src,L/'app/resources/world'/dest)
    pre=(R/'candidate_startup.py').read_text('utf-8').split('\ndef configure():')[0]
    pre=once(pre,'R = Path(__file__).resolve().parent','R = Path(__file__).resolve().parent / "resources/world"')
    pre=pre.replace('"build/inputs/','"inputs/').replace('"build/icon-assets.json"','"icon-assets.json"')
    pre+='''
def configure(client,dll):
    option=os.environ.get('LITO_SHARED_EXPIRED_ICONS','')
    if option not in ('','0','1'):raise ValueError('LITO_SHARED_EXPIRED_ICONS must be 0 or 1')
    verify_resource_sources()
    app=Path(__file__).resolve().parent
    if (Path(client)/'ddraw.dll').exists():raise ValueError('Unexpected client ddraw.dll; use the LiTo-specific DLL')
    receipt=app/'resources/class-icons/resources.json'
    os.environ['LITO_NAMED_DDRAW_SHA256']=hashlib.sha256(Path(dll).read_bytes()).hexdigest()
    os.environ['LITO_CLASS_ICON_RECEIPT']=str(receipt)
    os.environ['LITO_CLASS_ICON_RECEIPT_SHA256']=hashlib.sha256(receipt.read_bytes()).hexdigest()
    os.environ['LITO_SHARED_STATUS_CATALOG']=str(app/'compat/shared_status.bin')
'''
    text(L/'app/world_resource_preflight.py',pre)
    server=(L/'app/server/Start-Shared-Formal.py').read_text('utf-8')
    server=re.sub(r"JAR_SHA256 = '[0-9a-f]+'","JAR_SHA256 = '"+q['jar']['sha256']+"'",server,count=1)
    text(L/'app/server/Start-Shared-World.py',server)
    p=S/'deployment/release-inputs.json';d=json.loads(p.read_text('utf-8-sig'));d['server_sha256']=q['jar']['sha256'];text(p,json.dumps(d,ensure_ascii=False,indent=2)+'\n')
    # Source parity for the two class-domain changes; other combat source work stays untouched.
    p=S/'src/main/java/local/lito381/server/Protocol.java';t=p.read_text('utf-8')
    text(p,once(t,'characterType > 4 || sex','characterType > 6 || sex'))
    p=S/'src/main/java/local/lito381/server/ClientSession.java';t=p.read_text('utf-8')
    text(p,once(t,'characterType < 0 || characterType > 4','characterType < 0 || characterType > 6'))
    # New formal entry preserves all owned lifetime logic and routes both first/additional clients.
    p=L/'src/development_startup.py';t=p.read_text('utf-8')
    for a,b in [("ENTRY = SERVER_DELIVERY / 'Start-Shared-Formal.py'","ENTRY = SERVER_DELIVERY / 'Start-Shared-World.py'"),
                ("PROBE = DELIVERY / 'lito_runtime_probe_shared.py'","PROBE = DELIVERY / 'lito_runtime_probe_named.py'"),
                ("DLL = DELIVERY / 'compat/ddraw.dll'","DLL = DELIVERY / 'compat/litoddraw.dll'"),
                ("shutil.copyfile(DLL,CLIENT/'ddraw.dll')","shutil.copyfile(DLL,CLIENT/'litoddraw.dll')"),
                ("'--shared-resource-candidate', str(DELIVERY)]","'--shared-resource-candidate', str(DELIVERY), '--multi-instance-startup-gate']"),
                ("        command.append('--multi-instance-startup-gate')","        if '--multi-instance-startup-gate' not in command: command.append('--multi-instance-startup-gate')")]:t=once(t,a,b)
    marker="    exact(SELECTED/'LiTo.bin', BIN_SHA); exact(CLIENT/'LiTo.bin', BIN_SHA)"
    t=once(t,marker,marker+"\n    if str(DELIVERY) not in sys.path: sys.path.insert(0,str(DELIVERY))\n    import world_resource_preflight\n    world_resource_preflight.configure(CLIENT,DLL)")
    # Remove old runtime targets from the live pin list; preserve originals as reconstruction inputs.
    a=t.index('PINS = {');b=t.index('\nBIN_SHA = ',a)
    import ast
    original=ast.parse(t[a:b]);pins=[]
    for key in original.body[0].value.keys:pins.append(Path(ast.literal_eval(key.args[0])))
    pins=[p for p in pins if p not in [L/'app/compat/ddraw.dll',L/'app/lito_runtime_probe_shared.py',L/'app/server/Start-Shared-Formal.py']]
    pins += [p for p in changes if p.is_relative_to(L/'app')]
    pins=list(dict.fromkeys(pins))
    body='PINS = {\n'
    for p in pins:
        content=changes[p] if p in changes else p.read_bytes()
        body+=f'    Path({str(p)!r}): {hashlib.sha256(content).hexdigest().upper()!r},\n'
    t=t[:a]+body+'}\n'+t[b:];text(L/'src/development_startup.py',t)
    for relative in ['src/LiTo381Launcher.csproj','src/MainForm.cs','tools/build_current.py']:
        p=L/relative;text(p,p.read_text('utf-8').replace('0.6.69','0.6.70'))
    p=L/'src/FormalStandardSources.cs';text(p,p.read_text('utf-8').replace('Start-Shared-Formal.py','Start-Shared-World.py'))
    p=L/'tools/verify_current.py';text(p,once(p.read_text('utf-8'),"L/'app/server/Start-Shared-Formal.py'",'m.ENTRY'))
    # This verified script is the rebuild entry for the promoted native source.
    t=(C/'compile.cmd').read_text('utf-8').replace('../candidate/','../build/native-current/')
    t=t.replace('@echo off','@echo off\ncd /d "%~dp0"\nif not exist "..\\build\\native-current" mkdir "..\\build\\native-current"',1)
    text(L/'native/build.cmd',t)
    backup=L/'backups/pre-0.6.70-changed-files.zip'
    if backup.exists():raise ValueError('Rollback archive already exists; do not overwrite')
    extra=[L/'src/DevelopmentStartup.cs',L/'app/runtime-pins.json']
    extra += [L/'release/current'/n for n in ['LiTo381Launcher.exe','LiTo381Launcher.dll','LiTo381Launcher.pdb','LiTo381Launcher.deps.json','LiTo381Launcher.runtimeconfig.json','development_startup.py']]
    targets=list(dict.fromkeys(list(changes)+extra));manifest=[]
    with zipfile.ZipFile(backup,'x',compression=zipfile.ZIP_DEFLATED) as z:
        for i,p in enumerate(targets):
            row={'path':str(p),'existed':p.exists()}
            if p.exists():
                row.update(member=f'files/{i}',sha256=sha(p));z.writestr(row['member'],p.read_bytes())
            manifest.append(row)
        z.writestr('manifest.json',json.dumps(manifest,ensure_ascii=False,indent=2))
    with zipfile.ZipFile(backup) as z:
        for row in manifest:
            if row['existed']:assert hashlib.sha256(z.read(row['member'])).hexdigest()==row['sha256']
    for p,content in changes.items():p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(content)
    report={'version':'0.6.70','status':'staged-awaiting-release-verification','candidate_qualification':sha(C/'qualification.json'),
            'user_verified':['dragon knight lobby and room icon','illusionist lobby and room icon'],
            'backup':str(backup),'source_database_modified':False,'changes':[{'path':str(p),'sha256':sha(p)} for p in changes]}
    (C/'publication.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),'utf-8')
    print('Staged 0.6.70; rollback archive verified; files='+str(len(changes)))
if __name__=='__main__':main()

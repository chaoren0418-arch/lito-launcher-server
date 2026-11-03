"""Create/verify the joint native 0.6.60 baseline; deletion is a separate scoped PowerShell step."""
from pathlib import Path
import hashlib, json, os, stat, sys, zipfile
L=Path(__file__).resolve().parents[1]
S=L.parent.parent/'lito伺服器/LiTo381-Server'
ROOTS={'launcher':L,'server':S}
D=L/'docs'; ARCHIVE=L/'backups/baseline-0.6.60.zip'
HISTORY=L/'backups/history/through-0.6.60-evidence.zip'
RECEIPT=D/'baseline-0.6.60.json'; PLAN=D/'baseline-0.6.60-cleanup-plan.json'
def sha(p):
    h=hashlib.sha256()
    with p.open('rb') as f:
        for chunk in iter(lambda:f.read(1024*1024),b''):h.update(chunk)
    return h.hexdigest()
def walk(root):
    for e in os.scandir(root):
        p=Path(e.path);st=e.stat(follow_symlinks=False)
        if st.st_file_attributes & stat.FILE_ATTRIBUTE_REPARSE_POINT:raise RuntimeError('Reparse point refused: '+str(p))
        if e.is_dir(follow_symlinks=False):yield from walk(p)
        else:yield p
def origin(p):
    for name,root in ROOTS.items():
        if p.is_relative_to(root):return name+'/'+p.relative_to(root).as_posix()
    raise ValueError(p)
def verify_archive(path):
    with zipfile.ZipFile(path) as z:
        manifest=json.loads(z.read('MANIFEST.json'))
        assert len(z.namelist())==len(set(z.namelist()))
        for row in manifest['files']:
            assert hashlib.sha256(z.read(row['path'])).hexdigest()==row['sha256'],row['path']
        assert z.testzip() is None
    return manifest
def write_archive(path,entries,metadata):
    assert not path.exists(),f'Archive exists; refusing overwrite: {path}'
    path.parent.mkdir(parents=True,exist_ok=True)
    rows=[]
    with zipfile.ZipFile(path,'x',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for name,data in entries:
            z.writestr(name,data);rows.append({'path':name,'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest()})
        z.writestr('MANIFEST.json',json.dumps(dict(metadata,files=rows),ensure_ascii=False,indent=2))
    verify_archive(path)
    return {'path':str(path),'sha256':sha(path),'bytes':path.stat().st_size,'files':len(rows)}
def prepare():
    assert not ARCHIVE.exists() and not HISTORY.exists(),'0.6.60 already sealed'
    pins=json.loads((S/'deployment/release-inputs.json').read_text('utf-8-sig'))
    assert pins['server_sha256']==sha(L/'app/server/LiTo381-SharedResources-candidate.jar')
    assert pins['server_sha256']=='bbc59308c43df89ee5029d3b3c802be8ad55fee65c41faca99a76912dd5dce43'
    files=[p for root in ROOTS.values() for p in walk(root)]
    protected_runs={json.loads((D/n).read_text('utf-8'))['run'] for n in ['startup-smoke.json','durable-settlement-native.json']}
    for root in [L/'app/runs',L/'app/server/runs']:
        protected_runs.update(p.name for p in sorted(root.glob('development-*'),key=lambda p:p.stat().st_mtime,reverse=True)[:2])
    latest_flow=max((S/'runtime').glob('durable-flow-*'),key=lambda p:p.stat().st_mtime).name
    latest_gui=max((L/'app/gui-runs').iterdir(),key=lambda p:p.stat().st_mtime).name
    targets={};backup_zips=[]
    for p in files:
        rel=p.relative_to(L if p.is_relative_to(L) else S);parts=rel.parts;reason=None
        if parts[0]=='backups' and len(parts)>2:continue
        if len(parts)==2 and parts[0]=='backups' and p.suffix=='.zip':reason='superseded version backup';backup_zips.append(p)
        elif parts==('backups','current-0.6.35.verified.json'):reason='superseded baseline receipt'
        elif p.is_relative_to(L/'src/bin') or p.is_relative_to(L/'src/obj'):reason='reproducible .NET build cache'
        elif '__pycache__' in parts:reason='reproducible Python build cache'
        elif 'backups' not in parts and (p.name.endswith('.javap.txt') or p.suffix in {'.obj','.pdb'}):reason='reproducible native/disassembly output'
        elif p.is_relative_to(D/'baseline-0.6.60-previous-tools'):reason='superseded cleanup/verification code archived'
        elif p.is_relative_to(L/'app/runs') and p.relative_to(L/'app/runs').parts[0] not in protected_runs:reason='completed client test/run evidence'
        elif p.is_relative_to(L/'app/server/runs') and p.relative_to(L/'app/server/runs').parts[0] not in protected_runs:reason='completed server test/run evidence'
        elif p.is_relative_to(L/'app/gui-runs') and p.relative_to(L/'app/gui-runs').parts[0]!=latest_gui:reason='completed launcher log'
        elif p.is_relative_to(S/'runtime') and p.relative_to(S/'runtime').parts[0] not in {'battle-settlements','captures','memory-checks',latest_flow}:reason='completed server regression evidence'
        if reason:targets[p]=reason
    # Preserve unique source/docs from old rollback zips, plus all retired run evidence.
    text_ext={'.py','.cs','.java','.cpp','.c','.h','.inc','.def','.bat','.cmd','.ps1','.md','.json','.sql','.properties','.ini','.xml','.csproj','.txt'}
    def history_entries():
        for p,reason in targets.items():
            if reason in {'completed client test/run evidence','completed server test/run evidence','completed launcher log','completed server regression evidence','superseded cleanup/verification code archived'}:
                yield 'retired-files/'+origin(p),p.read_bytes()
        for p in backup_zips:
            with zipfile.ZipFile(p) as z:
                info={'original':origin(p),'sha256':sha(p),'entries':[{'path':x.filename,'bytes':x.file_size,'crc32':x.CRC} for x in z.infolist()]}
                yield 'retired-backups/'+origin(p)+'.manifest.json',json.dumps(info,ensure_ascii=False,indent=2).encode()
                for n in z.namelist():
                    if not n.endswith('/') and Path(n).suffix.lower() in text_ext and not any(x in Path(n).parts for x in ['bin','obj','node_modules']):
                        yield 'retired-backups/'+origin(p)+'/'+n.replace('\\','/'),z.read(n)
    history=write_archive(HISTORY,history_entries(),{'version':'0.6.60','purpose':'Unique historical sources and completed run evidence; obsolete runtime binaries omitted.'})
    # No transient sessions, databases, rollback copies or live settlement journal in the code baseline.
    exclusions={'backups','runtime','loginfo'}
    baseline_files=[p for p in files if p not in targets and p.relative_to(L if p.is_relative_to(L) else S).parts[0] not in exclusions
      and not any(p.is_relative_to(q) for q in [L/'app/runs',L/'app/server/runs',L/'app/gui-runs'])
      and not p.name.startswith('baseline-0.6.60-')]
    baseline=write_archive(ARCHIVE,((origin(p),p.read_bytes()) for p in baseline_files),{'version':'0.6.60','roots':{k:str(v) for k,v in ROOTS.items()},'external_client':r'D:\lineage開發\0-客戶端','database_backup_included':False,'settlement_journal_included':False,'pins':pins})
    rows=[{'path':str(p),'scope':str(L if p.is_relative_to(L) else S),'bytes':p.stat().st_size,'sha256':sha(p),'reason':reason} for p,reason in targets.items()]
    PLAN.write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf-8')
    receipt={'version':'0.6.60','baseline':baseline,'history':history,'plan_sha256':sha(PLAN),'target_files':len(rows),'target_bytes':sum(x['bytes'] for x in rows),'protected_runs':sorted(protected_runs),'retained_legacy':'backups/0.6.26-before-shared-resources-20261011','runtime_pins':pins}
    RECEIPT.write_text(json.dumps(receipt,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps({'baseline_MiB':round(baseline['bytes']/1048576,1),'history_MiB':round(history['bytes']/1048576,1),'remove_files':len(rows),'remove_MiB':round(receipt['target_bytes']/1048576,1)}))
def verify():
    r=json.loads(RECEIPT.read_text('utf-8'))
    baseline=None
    for key in ['baseline','history']:
        p=Path(r[key]['path']);assert sha(p)==r[key]['sha256'];verify_archive(p)
        if key=='baseline':baseline=verify_archive(p)
    assert sha(PLAN)==r['plan_sha256']
    pins=json.loads((S/'deployment/release-inputs.json').read_text('utf-8-sig'))
    assert sha(L/'app/server/LiTo381-SharedResources-candidate.jar')==pins['server_sha256'].lower()==r['runtime_pins']['server_sha256']
    assert sha(L/'app/common_catalog.bin')==pins['catalog_sha256'].lower()
    assert (S/'runtime/battle-settlements').is_dir()
    for row in baseline['files']:
        rel=Path(row['path']);root=ROOTS[rel.parts[0]];parts=rel.parts[1:]
        if parts[0] in {'src','native','release','app','config','lib','deployment'}:
            p=root.joinpath(*parts);assert p.is_file() and sha(p)==row['sha256'],'Baseline input changed: '+str(p)
    print('PASS: 0.6.60 baseline and history archive contents/hashes, cleanup plan, production JAR/catalog pins, protected journal')
if __name__=='__main__':
    if sys.argv[1:] == ['prepare']:prepare()
    elif sys.argv[1:] == ['verify']:verify()
    else:raise SystemExit('Usage: baseline_060.py prepare|verify')

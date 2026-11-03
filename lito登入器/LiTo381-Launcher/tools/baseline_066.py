"""Seal the joint 0.6.66 baseline; remove only hash-verified, explicitly planned files."""
from pathlib import Path
import json, sys, zipfile
from baseline_060 import L, S, ROOTS, sha, walk, origin, verify_archive, write_archive

D=L/'docs'
ARCHIVE=L/'backups/baseline-0.6.66.zip'
HISTORY=L/'backups/history/0.6.61-0.6.66-evidence.zip'
RECEIPT=D/'baseline-0.6.66.json'
PLAN=D/'baseline-0.6.66-cleanup-plan.json'
JAR=L/'app/server/LiTo381-SharedResources-candidate.jar'
EXPECTED='6928a147df1e784304b9080632c56121b173bf60b172023ebd263e82fd448628'
INPUT=S/'tools/resource-database/inputs/base-0.6.61.jar'
INPUT_SHA='9719f45db324206617726ce06d1a9e2184e9a3b3aefab3d50f3ebbdfbf09cfac'

def target_reason(p):
    rel=p.relative_to(L if p.is_relative_to(L) else S)
    if p==L/'backups/baseline-0.6.60.zip':return 'superseded joint baseline'
    if rel.parts[0]=='backups':return None
    if p.is_relative_to(L/'src/bin') or p.is_relative_to(L/'src/obj'):return 'reproducible dotnet cache'
    if '__pycache__' in rel.parts:return 'reproducible python cache'
    if p==S/'tools/resource-database/build/base-061.jar':return 'relocated pinned build input'
    if p.is_relative_to(S/'tools/resource-database/build/compact-063'):
        if p.name in {'data.jsonl','schema.jsonl','snapshot.tsv','original-counts.tsv','restore.sql'}:return 'verified migration backup duplicate'
        if p.suffix=='.zip':return 'superseded deployment rollback'
        if p.suffix=='.class':return 'reproducible java class'
        return 'retired migration evidence'
    if p.parent==S/'tools/resource-database/build' and p.name.startswith('deployment-before-') and p.suffix=='.zip':return 'superseded deployment rollback'
    if p.is_relative_to(S/'tools') and 'build' in rel.parts:
        if p.name=='candidate.jar' and (p.parent.parent/'build.py').is_file():return 'reproducible candidate jar'
        if p.suffix=='.class':return 'reproducible java class'
    if p.name.endswith('.javap.txt') or p.name.endswith('.javap'):return 'reproducible disassembly'
    return None

def prepare():
    assert not ARCHIVE.exists() and not HISTORY.exists(), 'Baseline already sealed'
    pins=json.loads((S/'deployment/release-inputs.json').read_text('utf-8-sig'))
    assert sha(JAR)==pins['server_sha256'].lower()==EXPECTED
    assert sha(INPUT)==INPUT_SHA
    migration=S/'backups/resource-database-before-0.6.63.zip'
    assert sha(migration)=='ffa06a7c9a48f50d1c72f764382c8a4e4dfc5490cf52445f4cb4c89e8e157ed1'
    with zipfile.ZipFile(migration) as z:
        assert z.testzip() is None
        for n in z.namelist():
            p=S/'tools/resource-database/build/compact-063'/n
            assert p.is_file() and p.read_bytes()==z.read(n), 'Migration backup mismatch: '+n
    files=sorted(p for root in ROOTS.values() for p in walk(root))
    targets={p:reason for p in files if (reason:=target_reason(p))}
    text_ext={'.py','.cs','.java','.cpp','.c','.h','.inc','.def','.bat','.cmd','.ps1','.md','.json','.sql','.properties','.ini','.xml','.csproj','.txt','.log','.tsv'}
    def evidence():
        for p,reason in targets.items():
            if reason=='retired migration evidence':
                yield 'retired-files/'+origin(p),p.read_bytes()
            if reason in {'superseded joint baseline','superseded deployment rollback'}:
                with zipfile.ZipFile(p) as z:
                    assert z.testzip() is None
                    info={'original':origin(p),'sha256':sha(p),'entries':[{'path':x.filename,'bytes':x.file_size,'crc32':x.CRC} for x in z.infolist()]}
                    prefix='retired-backups/'+origin(p)
                    yield prefix+'.manifest.json',json.dumps(info,ensure_ascii=False,indent=2).encode()
                    for n in z.namelist():
                        if n.endswith('/') or Path(n).suffix.lower() not in text_ext:continue
                        if any(x in Path(n).parts for x in ['bin','obj','__pycache__']):continue
                        # Keep historical revisions only; identical current source is in the new baseline.
                        parts=Path(n).parts
                        current=ROOTS.get(parts[0]) if parts else None
                        current=current.joinpath(*parts[1:]) if current else None
                        data=z.read(n)
                        if current and current.is_file() and current.read_bytes()==data:continue
                        yield prefix+'/'+n.replace('\\','/'),data
    history=write_archive(HISTORY,evidence(),{'version':'0.6.66','purpose':'Superseded backup manifests and unique text/source revisions; obsolete binaries omitted.'})
    def included(p):
        rel=p.relative_to(L if p.is_relative_to(L) else S)
        if p in targets or rel.parts[0] in {'backups','runtime','loginfo'}:return False
        if any(p.is_relative_to(q) for q in [L/'app/runs',L/'app/server/runs',L/'app/gui-runs']):return False
        if p.name.startswith('baseline-0.6.66-') or p in {PLAN,RECEIPT}:return False
        return True
    baseline=write_archive(ARCHIVE,((origin(p),p.read_bytes()) for p in files if included(p)),{'version':'0.6.66','roots':{k:str(v) for k,v in ROOTS.items()},'pins':pins,'database_backup_included':False,'settlement_journal_included':False,'external_client':r'D:\lineage開發\0-客戶端'})
    rows=[{'path':str(p),'scope':str(L if p.is_relative_to(L) else S),'bytes':p.stat().st_size,'sha256':sha(p),'reason':reason} for p,reason in targets.items()]
    PLAN.write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf-8')
    protected=[p for p in files if p.is_relative_to(S/'runtime/battle-settlements') or (p.is_relative_to(L/'backups') or p.is_relative_to(S/'backups')) and p not in targets]
    receipt={'version':'0.6.66','baseline':baseline,'history':history,'plan_sha256':sha(PLAN),'target_files':len(rows),'target_bytes':sum(x['bytes'] for x in rows),'runtime_pins':pins,'protected_files':[{'path':str(p),'sha256':sha(p)} for p in protected]}
    RECEIPT.write_text(json.dumps(receipt,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps({'baseline_MiB':round(baseline['bytes']/1048576,1),'history_MiB':round(history['bytes']/1048576,1),'remove_files':len(rows),'remove_MiB':round(receipt['target_bytes']/1048576,1)}))

def verify():
    r=json.loads(RECEIPT.read_text('utf-8'))
    for key in ['baseline','history']:
        p=Path(r[key]['path']);assert sha(p)==r[key]['sha256'];verify_archive(p)
    assert sha(PLAN)==r['plan_sha256']
    pins=json.loads((S/'deployment/release-inputs.json').read_text('utf-8-sig'))
    assert sha(JAR)==pins['server_sha256'].lower()==EXPECTED
    assert sha(INPUT)==INPUT_SHA
    assert sha(L/'app/common_catalog.bin')==pins['catalog_sha256'].lower()
    retired={}
    hygiene_receipt=D/'baseline-0.6.66-hygiene-receipt.json'
    if hygiene_receipt.exists():
        h=json.loads(hygiene_receipt.read_text('utf-8'))
        hp=D/'baseline-0.6.66-hygiene-plan.json'
        assert sha(hp)==h['plan_sha256']
        assert sha(Path(h['archive']))==h['archive_sha256']
        retired={str(Path(x['path'])):x for x in json.loads(hp.read_text('utf-8'))}
    for row in verify_archive(ARCHIVE)['files']:
        parts=Path(row['path']).parts;p=ROOTS[parts[0]].joinpath(*parts[1:])
        # A sealed baseline is an immutable recovery snapshot. Current documentation
        # and its verifier evolve separately; executable/source/config inputs remain pinned.
        if parts[1]=='docs' or (len(parts)==2 and p.suffix=='.md') or p==L/'tools/baseline_066.py':continue
        if not p.exists() and str(p) in retired:
            assert retired[str(p)]['sha256']==row['sha256'],'Retired baseline evidence changed: '+str(p)
            continue
        assert p.is_file() and sha(p)==row['sha256'],'Baseline file changed: '+str(p)
    # WAL may legitimately grow during later gameplay; never delete or restore it through this tool.
    assert (S/'runtime/battle-settlements').is_dir()
    for row in r['protected_files']:
        p=Path(row['path'])
        if not p.is_relative_to(S/'runtime/battle-settlements'):
            assert p.is_file() and sha(p)==row['sha256'],'Protected backup changed: '+str(p)
    print('PASS: immutable 0.6.66 archives, live source/config/runtime/build inputs, retired evidence receipts and protected backups')

if __name__=='__main__':
    if sys.argv[1:]==['prepare']:prepare()
    elif sys.argv[1:]==['verify']:verify()
    else:raise SystemExit('Usage: baseline_066.py prepare|verify')

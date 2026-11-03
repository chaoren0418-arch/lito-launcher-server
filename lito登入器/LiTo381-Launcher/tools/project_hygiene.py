"""Archive retired references/logs by content hash; never delete through Python."""
from pathlib import Path
import argparse, hashlib, json, os, stat, zipfile

L=Path(__file__).resolve().parents[1]
P=L.parent.parent
S=P/'lito伺服器/LiTo381-Server'
ROOTS=[L.parent,S.parent]
ARCHIVE=L.parent/'docs/history/retired-references-and-runs-20261014.zip'
PLAN=L/'docs/baseline-0.6.66-hygiene-plan.json'
RECEIPT=L/'docs/baseline-0.6.66-hygiene-receipt.json'

def sha(p):
    h=hashlib.sha256()
    with p.open('rb') as f:
        for c in iter(lambda:f.read(1024*1024),b''):h.update(c)
    return h.hexdigest()

def safe(p):
    p=p.absolute()
    assert any(p.is_relative_to(r) and p!=r for r in ROOTS),p
    assert not p.is_relative_to(S/'runtime/battle-settlements'),p
    assert not any(p.is_relative_to(q) for q in [L/'backups',S/'backups']),p
    for q in [p,*p.parents]:
        if q.exists():assert not q.stat(follow_symlinks=False).st_file_attributes & stat.FILE_ATTRIBUTE_REPARSE_POINT,q
        if q in ROOTS:break
    return p

def candidates():
    inv=json.loads((L/'docs/baseline-0.6.66-hygiene-inventory.json').read_text('utf-8'))
    old=L.parent/'舊版分析與驗收'
    runroots=[L/'app/runs',L/'app/server/runs',L/'app/gui-runs']
    keep={'movement-lock-066-smoke','development-f022810e10294764bc09a4f7f2686fdc'}
    for root in runroots:
        keep.update(p.name for p in sorted(root.glob('development-*'),key=lambda x:x.stat().st_mtime,reverse=True)[:2])
    for name in ['startup-smoke.json','durable-settlement-native.json']:
        p=L/'docs'/name
        if p.exists():
            run=json.loads(p.read_text('utf-8')).get('run')
            if run:keep.add(run)
    runtime_keep={'battle-settlements','captures','memory-checks'}
    groups={}
    for p in (S/'runtime').iterdir():
        if not p.is_dir() or p.name in runtime_keep:continue
        prefix=p.name.rsplit('-',1)[0]
        if prefix not in groups or p.stat().st_mtime>groups[prefix].stat().st_mtime:groups[prefix]=p
    runtime_keep.update(p.name for p in groups.values())
    for rel,size,mtime in inv['files']:
        p=P/rel;reason=None
        if any(p.is_relative_to(q) for q in [L/'backups',S/'backups']):continue
        if not p.is_file():continue
        if p.is_relative_to(old):
            r=p.relative_to(old)
            if r.parts[0] in {'public-source-audit-0.7.660','public-source-audit-0.7.805'}:
                if r.parts[:2]!=('public-source-audit-0.7.660','tw_tpai'):reason='historical public reference; content-deduplicated archive'
            elif r.parts[0]=='acceptance':reason='completed historical acceptance evidence'
            elif len(r.parts)>2 and r.parts[0]=='analysis' and r.parts[1].startswith(('NINE-ISSUE-','FOUR-ISSUE-')):reason='completed historical debug sessions'
        elif any(p.is_relative_to(root) and p.relative_to(root).parts[0] not in keep for root in runroots):reason='retired native run; latest two and incident evidence retained'
        elif p.is_relative_to(S/'runtime') and p.relative_to(S/'runtime').parts[0] not in runtime_keep:reason='superseded server regression run'
        elif p.is_relative_to(L/'tools') or p.is_relative_to(S/'tools'):
            if p.suffix=='.log' and not p.is_relative_to(S/'tools/resource-database'):reason='completed historical diagnostic log'
        if '__pycache__' in p.parts and any(p.is_relative_to(q) for q in [L,S]):reason='reproducible python cache'
        if reason:yield safe(p),reason

def verify_archive():
    with zipfile.ZipFile(ARCHIVE) as z:
        m=json.loads(z.read('MANIFEST.json'));names=z.namelist()
        assert len(names)==len(set(names))
        blobs={r['sha256'] for r in m['files']}
        assert set(names)=={'MANIFEST.json'}|{'blobs/'+h for h in blobs}
        for h in blobs:assert hashlib.sha256(z.read('blobs/'+h)).hexdigest()==h
    return m

def prepare():
    assert not ARCHIVE.exists(),'Refusing to overwrite sealed history'
    rows=[];seen=set();ARCHIVE.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(ARCHIVE,'x',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for i,(p,reason) in enumerate(candidates(),1):
            data=p.read_bytes();h=hashlib.sha256(data).hexdigest()
            if h not in seen:z.writestr('blobs/'+h,data);seen.add(h)
            rows.append({'path':str(p),'relative':p.relative_to(P).as_posix(),'scope':str(next(r for r in ROOTS if p.is_relative_to(r))),'bytes':len(data),'sha256':h,'reason':reason})
            if i%5000==0:print('Archived',i,'paths;',len(seen),'unique contents',flush=True)
        z.writestr('MANIFEST.json',json.dumps({'format':'sha256-blobs-v1','files':rows},ensure_ascii=False))
    verify_archive()
    PLAN.write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf-8')
    receipt={'archive':str(ARCHIVE),'archive_sha256':sha(ARCHIVE),'plan_sha256':sha(PLAN),'files':len(rows),'unique_contents':len(seen),'source_bytes':sum(r['bytes'] for r in rows),'archive_bytes':ARCHIVE.stat().st_size}
    RECEIPT.write_text(json.dumps(receipt,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(receipt,ensure_ascii=False),flush=True)

def verify():
    r=json.loads(RECEIPT.read_text('utf-8'));assert sha(ARCHIVE)==r['archive_sha256'];assert sha(PLAN)==r['plan_sha256']
    assert verify_archive()['files']==json.loads(PLAN.read_text('utf-8'))
    print('PASS: retired history archive, every unique content, manifest and deletion plan')

def extract(fragment,destination):
    verify();destination=destination.absolute();assert not destination.exists(),'Use a new output directory'
    for parent in destination.parents:
        if parent.exists():assert not parent.stat(follow_symlinks=False).st_file_attributes & stat.FILE_ATTRIBUTE_REPARSE_POINT
    with zipfile.ZipFile(ARCHIVE) as z:
        rows=[r for r in json.loads(z.read('MANIFEST.json'))['files'] if fragment.casefold() in r['relative'].casefold()]
        assert rows,'No matching archived path'
        for r in rows:
            rel=Path(r['relative']);assert not rel.is_absolute() and '..' not in rel.parts
            p=destination/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(z.read('blobs/'+r['sha256']))
    print('Extracted',len(rows),'files into',destination)

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('action',choices=['prepare','verify','find','extract']);p.add_argument('--match',default='');p.add_argument('--to',type=Path);a=p.parse_args()
    if a.action=='prepare':prepare()
    elif a.action=='verify':verify()
    elif a.action=='find':
        for r in json.loads(PLAN.read_text('utf-8')):
            if a.match.casefold() in r['relative'].casefold():print(r['relative'],r['bytes'],r['sha256'])
    else:
        assert a.match and a.to,'extract requires --match and --to'
        extract(a.match,a.to)

"""Small documentation/tool supplement to the immutable 0.6.66 game baseline."""
from pathlib import Path
import hashlib,json,sys,zipfile
from project_hygiene import P,L,S,sha,safe
ARCHIVE=L.parent/'docs/history/maintenance-20261014.zip'
RECEIPT=L/'docs/baseline-0.6.66-maintenance-receipt.json'
EXTRA=L/'docs/baseline-0.6.66-hygiene-extra-log-plan.json'

def verify_archive():
    with zipfile.ZipFile(ARCHIVE) as z:
        m=json.loads(z.read('MANIFEST.json'))
        assert len(z.namelist())==len(m['files'])+1==len(set(z.namelist()))
        for row in m['files']:assert hashlib.sha256(z.read(row['path'])).hexdigest()==row['sha256']
    return m

def verify():
    r=json.loads(RECEIPT.read_text('utf-8'));assert sha(ARCHIVE)==r['sha256'];assert sha(EXTRA)==r['extra_plan_sha256']
    m=verify_archive();indexed={x['path']:x for x in m['files']}
    for row in json.loads(EXTRA.read_text('utf-8')):
        assert indexed['retired/'+row['relative']]['sha256']==row['sha256']
    print('PASS: maintenance supplement and all additional retired log contents')

def create():
    assert not ARCHIVE.exists(),'Refusing to overwrite supplement'
    paths=[L.parent/'README.md',L.parent/'AGENTS.md',S.parent/'README.md',S.parent/'AGENTS.md',L.parent/'舊版分析與驗收/README.md',L.parent/'docs/history/README.md',L/'README.zh-TW.md',S/'README.zh-TW.md',S/'VALIDATION.md',S/'docs/HISTORY.md',L/'docs/PROJECT-MAINTENANCE.zh-TW.md',L/'docs/BASELINE-0.6.66.md',L/'docs/baseline-0.6.66.json',L/'docs/baseline-0.6.66-hygiene-plan.json',L/'docs/baseline-0.6.66-hygiene-receipt.json',EXTRA]
    paths += [L/'docs'/n for n in ['baseline-0.6.66-hygiene-result.json','baseline-0.6.66-hygiene-exceptions.json','baseline-0.6.66-hygiene-restore-check.json']]
    paths += [L/'tools'/n for n in ['baseline_066.py','project_hygiene.py','Apply-ProjectHygiene.ps1','maintenance_backup.py']]
    rows=[]
    with zipfile.ZipFile(ARCHIVE,'x',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        def add(name,p):
            data=p.read_bytes();z.writestr(name,data);rows.append({'path':name,'sha256':hashlib.sha256(data).hexdigest(),'bytes':len(data)})
        for p in paths:add('current/'+p.relative_to(P).as_posix(),p)
        for row in json.loads(EXTRA.read_text('utf-8')):
            p=safe(Path(row['path']));assert sha(p)==row['sha256'];add('retired/'+row['relative'],p)
        z.writestr('MANIFEST.json',json.dumps({'files':rows,'purpose':'Maintenance overlay; game baseline remains unchanged'},ensure_ascii=False,indent=2))
    verify_archive()
    RECEIPT.write_text(json.dumps({'archive':str(ARCHIVE),'sha256':sha(ARCHIVE),'extra_plan_sha256':sha(EXTRA),'bytes':ARCHIVE.stat().st_size,'files':len(rows)},ensure_ascii=False,indent=2),encoding='utf-8')
    verify()

if __name__=='__main__':
    if sys.argv[1:]==['create']:create()
    elif sys.argv[1:]==['verify']:verify()
    else:raise SystemExit('Usage: maintenance_backup.py create|verify')

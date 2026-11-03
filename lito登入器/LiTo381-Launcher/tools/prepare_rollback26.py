"""Record a thin, hash-verified recipe before deleting the obsolete staged client."""
from pathlib import Path
import os,json,hashlib,zipfile
L=Path(__file__).resolve().parent.parent
B=L/'backups/0.6.26-before-shared-resources-20261011'
S=Path('D:/380/天堂(Lineage 3.81C) (1)')
T=L/'runtime/development-startup-repair-20261011/client-runtime'
def sha(p):
    h=hashlib.sha256()
    with p.open('rb') as f:
        for b in iter(lambda:f.read(4*1024*1024),b''):h.update(b)
    return h.hexdigest()
rows=[]
with zipfile.ZipFile(B/'client-delta.zip','w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
    for root,ds,fs in os.walk(T):
        for d in ds:
            assert not (Path(root)/d).is_junction()
        for f in fs:
            p=Path(root)/f;rel=p.relative_to(T);h=sha(p);source=S/rel
            same=source.is_file() and source.stat().st_size==p.stat().st_size and sha(source)==h
            row={'path':rel.as_posix(),'bytes':p.stat().st_size,'sha256':h,'from':'original' if same else 'delta'}
            if not same:z.write(p,rel.as_posix())
            rows.append(row)
with zipfile.ZipFile(B/'client-delta.zip') as z:
    assert z.testzip() is None
    for r in rows:
        if r['from']=='delta':assert hashlib.sha256(z.read(r['path'])).hexdigest()==r['sha256']
result={'source':str(S),'target':str(T),'files':rows,'delta_sha256':sha(B/'client-delta.zip')}
(B/'client-recipe.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({'files':len(rows),'shared_files':sum(r['from']=='original' for r in rows),'delta_bytes':(B/'client-delta.zip').stat().st_size}))

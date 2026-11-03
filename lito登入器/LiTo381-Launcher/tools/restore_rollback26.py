"""Verify the .26 offline backup; --apply restores its exact files and staged client."""
from pathlib import Path
import hashlib,json,shutil,sys,zipfile,subprocess
L=Path(__file__).resolve().parent.parent
B=L/'backups/0.6.26-before-shared-resources-20261011'
def digest(p):
    h=hashlib.sha256()
    with p.open('rb') as f:
        for data in iter(lambda:f.read(4*1024*1024),b''):h.update(data)
    return h.hexdigest()
recipe=json.loads((B/'client-recipe.json').read_text('utf-8'))
manifest=json.loads((B/'manifest.json').read_text('utf-8'))
assert digest(B/'client-delta.zip')==recipe['delta_sha256']
target=Path(recipe['target']);source=Path(recipe['source'])
assert target==L/'runtime/development-startup-repair-20261011/client-runtime'
for r in manifest['files']:assert digest(B/r['backup'])==r['sha256'],r['backup']
with zipfile.ZipFile(B/'client-delta.zip') as z:
    for r in recipe['files']:
        rel=Path(r['path']);assert not rel.is_absolute() and '..' not in rel.parts
        p=source/rel
        actual=digest(p) if r['from']=='original' else hashlib.sha256(z.read(r['path'])).hexdigest()
        assert actual==r['sha256'],r['path']
    if '--apply' in sys.argv:
        server = L.parent.parent/'lito伺服器/LiTo381-Server'
        subprocess.run([sys.executable, '-B', '-X', 'utf8', str(server/'tools/Restore-LegacyRuntime.py'), '--apply'], check=True)
        for p in (target,*target.parents):assert not p.is_symlink() and not p.is_junction(),p
        for r in recipe['files']:
            p=target/r['path'];p.parent.mkdir(parents=True,exist_ok=True)
            if r['from']=='original':shutil.copy2(source/r['path'],p)
            else:p.write_bytes(z.read(r['path']))
        # Restore launcher/probe artifacts within this project only. Server/external client
        # dependencies are checked and never silently rolled back over another project.
        for r in manifest['files']:
            p=Path(r['original'])
            if p.is_relative_to(L):
                for parent in (p,*p.parents):assert not parent.is_symlink() and not parent.is_junction()
                p.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(B/r['backup'],p)
            else:assert p.is_file() and digest(p)==r['sha256'],f'External .26 dependency changed; saved copy: {B/r["backup"]}'
result={'backup_files':len(manifest['files']),'client_recipe_files':len(recipe['files']),'verified':True,'restored':'--apply' in sys.argv}
(L/'docs/rollback26-verified.json').write_text(json.dumps(result,indent=2),encoding='utf-8');print(json.dumps(result))

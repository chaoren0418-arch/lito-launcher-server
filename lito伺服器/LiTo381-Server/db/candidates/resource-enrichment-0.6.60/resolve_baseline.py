"""未匯入、未接線。Resolve exact0.6.60 inputs after concurrent live release drift.
Reads existing protected baseline ZIP. Copies only the6MiB required JAR into
owned candidate input directory, never launches it or overwrites live files.
"""
from pathlib import Path
import hashlib,json,zipfile
P=Path(__file__).resolve().parent;R=P.parents[5]
LIVE=R/'3-lito開發/lito登入器/LiTo381-Launcher/app/server/LiTo381-SharedResources-candidate.jar'
ARCHIVE=R/'3-lito開發/lito登入器/LiTo381-Launcher/backups/baseline-0.6.60.zip'
ENTRY='launcher/app/server/LiTo381-SharedResources-candidate.jar'
PIN='bbc59308c43df89ee5029d3b3c802be8ad55fee65c41faca99a76912dd5dce43'
def hashfile(p):
    h=hashlib.sha256()
    with Path(p).open('rb') as f:
        for x in iter(lambda:f.read(4194304),b''):h.update(x)
    return h.hexdigest()
def resolve():
    actual=hashfile(LIVE)
    if actual==PIN:return LIVE
    dest=P/'pinned-inputs/LiTo381-SharedResources-0.6.60.jar'
    with zipfile.ZipFile(ARCHIVE) as z:raw=z.read(ENTRY)
    assert hashlib.sha256(raw).hexdigest()==PIN,'protected baseline does not match; do not change pin'
    if dest.exists():assert hashfile(dest)==PIN
    else:dest.parent.mkdir(exist_ok=True);dest.write_bytes(raw)
    d=dict(package_status='未匯入、未接線',baseline='0.6.60',original_observed_path=str(LIVE),original_pin=PIN,currently_observed_formal_sha256=actual,archive=str(ARCHIVE),archive_member=ENTRY,archive_sha256=hashfile(ARCHIVE),resolved_input=str(dest),resolved_sha256=PIN,scope='archived published0.6.60 snapshot; latest formal JAR is a different input, not analyzed as0.6.60; no Main/network/deployment')
    (P/'baseline-resolution.json').write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    return dest
def repair(sources):
    resolved=resolve();changes=[]
    with zipfile.ZipFile(ARCHIVE) as z:
        names=set(z.namelist())
        for row in sources:
            path=Path(row['path'])
            if row.get('original_observed_path') and path!=LIVE:
                changes.append(dict(source_id=row['source_id'],original_path=row['original_observed_path'],resolved_path=str(path),resolved_member=row['member'],pin_retained=row['sha256'],already_restored=True))
            if path==LIVE:
                row['original_observed_path']=str(path);row['path']=str(resolved);row['version']=row['version']+' / archived input after formal drift';row['restored_archive']=str(ARCHIVE);row['restored_member']=ENTRY;changes.append(dict(source_id=row['source_id'],original_path=str(path),resolved_path=str(resolved),pin_retained=row['sha256']))
                continue
            if path.is_relative_to(P):continue
            if row['member']:actual=hashlib.sha256(zipfile.ZipFile(path).read(row['member'])).hexdigest()
            else:actual=hashfile(path)
            if actual==row['sha256']:continue
            roots=[(R/'3-lito開發/lito伺服器/LiTo381-Server','server'),(R/'3-lito開發/lito登入器/LiTo381-Launcher','launcher')]
            entry=next((prefix+'/'+path.relative_to(root).as_posix() for root,prefix in roots if path.is_relative_to(root)),None)
            assert entry in names,('changed original input has no exact baseline member',path)
            raw=z.read(entry);assert hashlib.sha256(raw).hexdigest()==row['sha256'],('changed source does not match original archived pin',path)
            row['original_observed_path']=str(path);row['path']=str(ARCHIVE);row['member']=entry;row['version']=row['version']+' / archived input after source drift';changes.append(dict(source_id=row['source_id'],original_path=str(path),archive_member=entry,pin_retained=row['sha256']))
    (P/'restored-source-locations.json').write_text(json.dumps(dict(package_status='未匯入、未接線',changes=changes,old_hashes_preserved=True),ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    return sources

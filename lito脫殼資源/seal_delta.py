"""Incrementally seal owned new evidence without replaying earlier checks."""
from audit import *

def seal(names,generator,retired=()):
    old=record(ROOT/'artifact-manifest.json');previous=json.loads((ROOT/'delivery-verification.json').read_text(encoding='utf-8'))
    assert previous['all_match']
    prev_pin=record(ROOT/'delivery-verification.json')
    manifest=json.loads((ROOT/'artifact-manifest.json').read_text(encoding='utf-8'))
    rows={str(Path(x['path']).resolve()).casefold():x for x in manifest['entries']}
    for name in retired:
        p=(ROOT/name).resolve();assert p.is_relative_to(ROOT)
        rows.pop(str(p).casefold(),None)
    if retired:manifest['retired_analysis_slices']=list(dict.fromkeys(manifest.get('retired_analysis_slices',[])+list(retired)))
    names=set(names)|{'seal_delta.py'};pins=[]
    for n in sorted(names):
        p=(ROOT/n).resolve();assert p.is_relative_to(ROOT)
        pin=record(p);rows[str(p).casefold()]=pin;pins.append(pin)
    manifest.update(updated_at=stamp(),entries=sorted(rows.values(),key=lambda x:x['path']),documentation_delta={'generator':record(generator),'previous_manifest_sha256':old['sha256'],'previous_delivery_verification_sha256':prev_pin['sha256'],'scope':'Only new/changed evidence files checked. Earlier archive/API/GUI tests not replayed.'})
    if 'generation' in manifest:manifest['generation']=record(Path(manifest['generation']['path']))
    write('artifact-manifest.json',manifest)
    checks=[{**pin,'match':record(Path(pin['path']))==pin}for pin in pins];assert all(x['match']for x in checks)
    inherited=previous.get('prior_full_verification',{'manifest_sha256':old['sha256'],'verification_sha256':prev_pin['sha256'],'sealed_files':previous['sealed_files'],'all_match':True})
    write('delivery-verification.json',{'updated_at':stamp(),'manifest':record(ROOT/'artifact-manifest.json'),'sealed_files':len(rows),'all_match':True,'scope':'Inherited prior full verification plus current finite evidence delta. No broad re-extraction or test replay.','prior_full_verification':inherited,'prior_delivery_verification_sha256':prev_pin['sha256'],'changed_files_checked':checks,'original_game_launched':False,'GUI_accepted':False})
    progress=json.loads((ROOT/'監製進度.json').read_text(encoding='utf-8'));paths={x['path']for x in progress['actual_artifacts']}|{str(ROOT/n)for n in names if n.endswith('.json')};progress['actual_artifacts']=[record(Path(p))for p in sorted(paths)];progress['verification']['delivery_all_hashes_match']=True;write('監製進度.json',progress)
    return len(pins)

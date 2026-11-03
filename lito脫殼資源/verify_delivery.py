"""Read-only verification of sealed delivery files and cited source pins."""
from audit import *
import sys

def verify():
    manifest=json.loads((ROOT/'artifact-manifest.json').read_text(encoding='utf-8'))
    cache={};failures=[];references=0
    def check(pin,context):
        nonlocal references
        references+=1;p=Path(pin['path']);key=str(p).casefold()
        if key not in cache:
            try:cache[key]=record(p)
            except (OSError,ValueError) as e:cache[key]={'error':str(e)}
        actual=cache[key]
        if actual.get('bytes')!=pin['bytes'] or actual.get('sha256')!=pin['sha256']:
            failures.append({'context':context,'expected':pin,'actual':actual})
    def walk(obj,context):
        if isinstance(obj,dict):
            if all(k in obj for k in ['path','bytes','sha256']) and isinstance(obj['path'],str):check(obj,context)
            for k,v in obj.items():walk(v,context+'/'+k)
        elif isinstance(obj,list):
            for n,v in enumerate(obj):walk(v,context+f'/{n}')
    for n,pin in enumerate(manifest['entries']):
        check(pin,'artifact-manifest')
        if n%500==0:print(f'Verified sealed files: {n}/{len(manifest["entries"])}',flush=True)
    # Examine authoritative manifests, not retired partial captures or mutable progress.
    for pin in manifest['entries']:
        p=Path(pin['path'])
        if p.suffix=='.json':walk(json.loads(p.read_text(encoding='utf-8')),str(p.relative_to(ROOT)))
    baseline=json.loads((ROOT/'baseline.json').read_text(encoding='utf-8'))
    for pin in baseline['sources']:
        assert pin['verified'];check(pin,'fixed baseline')
    result={'updated_at':stamp(),'manifest':record(ROOT/'artifact-manifest.json'),'sealed_files':len(manifest['entries']),'distinct_files_checked':len(cache),'pin_references_checked':references,'failures':failures,'all_match':not failures,'original_game_launched':False,'GUI_accepted':False,'scope':'Current hashes match saved source/delivery pins; no executable, runtime or native rendering proof.'}
    write('delivery-verification.json',result)
    progress=json.loads((ROOT/'監製進度.json').read_text(encoding='utf-8'));progress['verification']['delivery_all_hashes_match']=not failures;progress['actual_artifacts'].append(record(ROOT/'delivery-verification.json'));write('監製進度.json',progress)
    print(json.dumps({'sealed_files':len(manifest['entries']),'distinct_files_checked':len(cache),'all_match':not failures,'failure_count':len(failures)}),flush=True)
    return 1 if failures else 0

if __name__=='__main__':sys.exit(verify())

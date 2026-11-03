from pathlib import Path
import os,json,collections
L=Path(__file__).resolve().parent.parent;P=L.parent
sizes=collections.defaultdict(lambda:[0,0])
for top,ds,fs in os.walk(P):
    ds[:]=[d for d in ds if not (Path(top)/d).is_junction() and not (Path(top)/d).is_symlink()]
    for name in fs:
        p=Path(top)/name
        if p.is_symlink():continue
        n=p.stat().st_size
        key=p.relative_to(P).parts[0];sizes[key][0]+=n;sizes[key][1]+=1
        sizes['TOTAL'][0]+=n;sizes['TOTAL'][1]+=1
before=json.loads((L/'docs/cleanup-before.json').read_text('utf-8'))['TOTAL']
result={'before_bytes':before[0],'after_bytes':sizes['TOTAL'][0],'removed_logical_bytes':before[0]-sizes['TOTAL'][0],'after_files':sizes['TOTAL'][1],'by_top_level':dict(sizes),'measurement':'logical bytes, no reparse traversal, not physical disk reclaimed'}
(L/'docs/size-current.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(result,ensure_ascii=False))

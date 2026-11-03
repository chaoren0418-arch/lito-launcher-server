"""Project legacy clan-friendly fixtures onto actual friendly room teams; assert whole-file inverse."""
from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent;B=R.parents[2]/'tools/resource-database/build';out=R/'competition-pair-fixtures';out.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
spec={
 'NativePairMaterialProbe.java': [('exercise(p.session,m.session,hub,state,world,mode)','exercise(p.session,o.session,hub,state,world,mode)')],
 'NativePairMaterialBoundaryProbe.java': [('exerciseBoundary(p.session,m.session,f.session,hub,memory,world,app,mode)','exerciseBoundary(p.session,o.session,f.session,hub,memory,world,app,mode)')],
 'NativePairMaterialMainProbe.java': [('var peer=m.session;','var peer=o.session;')],
}
rows=[]
for name,changes in spec.items():
 source=B/name;text=source.read_text('utf-8');original=text
 for old,new in changes:assert text.count(old)==1;text=text.replace(old,new)
 inverse=text
 for old,new in reversed(changes):assert inverse.count(new)==1;inverse=inverse.replace(new,old)
 assert inverse==original
 target=out/name;target.write_text(text,'utf-8');rows.append(dict(original=pin(source),versioned=pin(target),changes=changes))
(out/'manifest.json').write_text(json.dumps(dict(status='whole-file-inverse-actual-room-friend-projection',rows=rows,semantic_change='Select the existing same-team fourth session instead of opposing third session. Source clan, normal/Ctrl packets, lifecycle, costs, wire and all assertions remain. Independent intent probe retains opposing-team rejection cases.',originals_modified=False),ensure_ascii=False,indent=2)+'\n','utf-8')
print('PASS three inverse friendly-room fixtures')

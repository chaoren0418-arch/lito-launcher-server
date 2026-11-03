"""Two explicit source projections: PC capture before locks and same outer boundary admission."""
from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent;source=R/'NativeSourceNegativeTargets.java';s=source.read_text('utf-8');changes=[
 ('if(n==null||n.kind()!=3){original(pc,packet,spell,fixture);return;}var aim=new Aim(c,n,null,skill);','ClientSession player=null;for(var candidate:c.players)if(c.bindings.get(candidate).object()==request.targetObjectId()){player=candidate;break;}if(player==null&&(n==null||n.kind()!=3)){original(pc,packet,spell,fixture);return;}if(player!=null)n=null;var aim=new Aim(c,n,player,skill);'),
 ('if(a.c.pc!=pc||a.skill!=skill||player!=null||owner!=a.owner||!a.valid())return false;','if(a.c.pc!=pc||a.skill!=skill||a.player!=null?(a.c.pc!=pc||a.skill!=skill||player!=a.player||owner!=null||!a.valid()):(player!=null||owner!=a.owner||!a.valid()))return false;'),
]
# Keep validation explicit; conditional precedence must not change which caster/skill gates apply.
changes[1]=(changes[1][0],'if(a.c.pc!=pc||a.skill!=skill||!a.valid()||(a.player!=null?(player!=a.player||owner!=null):(player!=null||owner!=a.owner)))return false;')
for old,new in changes:assert s.count(old)==1;s=s.replace(old,new)
inverse=s
for old,new in reversed(changes):assert inverse.count(new)==1;inverse=inverse.replace(new,old)
assert inverse==source.read_text('utf-8')
out=R/'source-negative-player-src';out.mkdir(exist_ok=True);target=out/source.name;target.write_text(s,'utf-8');pin=lambda p:dict(path=str(p.resolve()),sha256=hashlib.sha256(p.read_bytes()).hexdigest());(out/'manifest.json').write_text(json.dumps(dict(status='two-whole-file-inverse-PC-capture-and-admission-projections',original=pin(source),versioned=pin(target),changes=changes),ensure_ascii=False,indent=2)+'\n','utf-8');print('PASS negative PC source inverse')

"""Freeze actual ground-spell roots before changing placement/material ordering."""
from pathlib import Path
import json,subprocess,hashlib,re
R=Path(__file__).resolve().parent;D=R/'build/ground-material-audit';D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p),sha256=sha(p))
b=read(R/'weakness-material/build/build.json');jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256']
cp=';'.join(map(str,[D,*[p['path'] for p in read(R/'build/integration-regression/preparation.json')['asm']]]));J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(D),str(R/'DumpMethod.java')],check=True)
outputs=[]
names=['useGroundSpell','useClassicWorldGroundSpell']
for name in names:
 p=D/(name+'.txt')
 with p.open('w',encoding='utf-8') as out:subprocess.run([str(J/'java.exe'),'-Xmx128m','-cp',cp,'DumpMethod',str(jar),name],stdout=out,check=True)
 outputs.append(pin(p))
 if not name.startswith('lambda$'):
  matches=re.findall(r'ClientSession\.(lambda\$[^\(]+)\(',p.read_text('utf-8'))
  assert len(matches)==1
  names.extend(matches)
generic=D/'DumpClassMethod.java'
text=(R/'DumpMethod.java').read_text('utf-8').replace('class DumpMethod','class DumpClassMethod').replace('"local/lito381/server/ClientSession.class"','"local/lito381/server/"+args[2]+".class"')
generic.write_text(text,encoding='utf-8')
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(D),str(generic)],check=True)
for owner,name in [('GameHub','placeGroundSpell'),('GameHub','placeClassicWorldGroundSpell'),('BattleGroundEffects','cast'),('ClassicWorldGroundEffects','cast')]:
 p=D/(owner+'-'+name+'.txt')
 with p.open('w',encoding='utf-8') as out:subprocess.run([str(J/'java.exe'),'-Xmx128m','-cp',cp,'DumpClassMethod',str(jar),name,owner],stdout=out,check=True)
 assert p.stat().st_size>0
 outputs.append(pin(p))
(D/'inputs.json').write_text(json.dumps(dict(runtime=b['runtime'],inspector=pin(R/'DumpMethod.java'),outputs=outputs,scope='static actual candidate inspection only; lambda names resolved from invokedynamic handles; no behavior qualification',required_invariants=['Reject invalid placement without consuming material, MP, HP or reuse.','Placement callback currently installs the inventory and MP/reuse after the ground-effects cast. A transactional adaptation must reject or compensate failed material admission before publishing a live ground lease.','Material preparation must use the canonical NativeCastMaterial authority and existing Source action timeline; do not restore a RuntimeInventory-only path.','Preserve independent WORLD and BATTLE geometry, visual points, duration and pulse behavior.','Audit Source action lock / GameHub monitor ordering and room/map retirement before installing the callback; a generic self-skill wrapper is insufficient.'],formal_deployment_changed=False),indent=2),encoding='utf-8')

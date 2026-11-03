"""Pin actual item-use gates behind the WORLD healing rejection, not mixed src."""
from pathlib import Path
import json,subprocess,hashlib
R=Path(__file__).resolve().parent;D=R/'build/decay-consumer-audit';D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(R/'decay-material/build/build.json');jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256']
inspector=R/'build/ground-material-audit/DumpClassMethod.java';cp=';'.join(map(str,[D,*[p['path']for p in read(R/'build/integration-regression/preparation.json')['asm']]]));J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
subprocess.run([str(J/'javac.exe'),'-g:none','-encoding','UTF-8','-cp',cp,'-d',str(D),str(inspector)],check=True)
outputs=[]
for name in ['useInventoryItem','useInventoryItemBeforeTravel','sourcePoisonBody$useInventoryItemBeforeTravel']:
 p=D/(name+'.txt')
 with p.open('w',encoding='utf-8')as out:subprocess.run([str(J/'java.exe'),'-Xmx128m','-cp',cp,'DumpClassMethod',str(jar),name,'ClientSession'],stdout=out,check=True)
 assert p.stat().st_size>0
 outputs.append(pin(p))
(D/'inputs.json').write_text(json.dumps(dict(runtime=b['runtime'],inspector=pin(inspector),outputs=outputs,scope='static inspection of actual potion dispatch and consumer; no candidate mutation',formal_deployment_changed=False),indent=2),encoding='utf-8')
print('Pinned item dispatch and original poison-scoped item consumer')

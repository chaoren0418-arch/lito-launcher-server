"""Run unchanged sealed packet checks on the composed ring candidate, then targeted native replay."""
from pathlib import Path
import shutil,json,hashlib,subprocess,sys
R=Path(__file__).resolve().parent;N=R.parent/'summon-parity';D=R/'build';sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
changes=[]
for p in [*N.glob('Summon*Probe.java'),N/'test_choices.py',N/'test_boundaries.py',N/'test_packets.py']:
    dest=R/p.name;shutil.copyfile(p,dest);changes.append(dict(source=str(p),destination=str(dest),sha256=sha(p)))
source=N/'test.py';dest=R/'test_material.py';shutil.copyfile(source,dest);changes.append(dict(source=str(source),destination=str(dest),sha256=sha(source)))
(D/'test-inputs.json').write_text(json.dumps(changes,indent=2),encoding='utf-8')
names=['test_packets.py'] if '--packets-only' in sys.argv else ['test_packets.py','test_choices.py','test_material.py','test_boundaries.py']
for name in names:
    if name=='test_packets.py' and (D/'packets/result.json').exists():
        receipt=json.loads((D/'packets/result.json').read_text('utf-8'));build=json.loads((D/'build.json').read_text('utf-8'))
        pins={p['sha256'] for p in receipt.get('inputs',[])};required=[R/'SummonPacketProbe.java',R/'SummonMaterialProbe.java',R/'test_packets.py']
        output=receipt.get('output',{})
        if receipt['status']=='passed' and receipt['runtime']==build['runtime'] and all(sha(p) in pins for p in required) and output and sha(Path(output['path']))==output['sha256']:
            print('REUSE exact packet inputs/runtime/output',flush=True);continue
    print('RUN '+name,flush=True);subprocess.run([sys.executable,str(R/name)],check=True)

"""Retain all regression assertions while selecting the composed potion candidate."""
from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent;P=R.parent;sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
source=P/'decay_regression.py';out=P/'world_potion_regression.py'
t=source.read_text('utf-8').replace('decay-material','world-potion').replace('decay-regression','world-potion-regression').replace('decay-candidate','world-potion-candidate').replace('exact decay candidate','exact world-potion candidate')
out.write_text(t,encoding='utf-8')
census=P/'decay-material/census.py';(R/'census.py').write_text(census.read_text('utf-8').replace('decay-material','world-potion'),encoding='utf-8')
(R/'regression-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in[source,census,Path(__file__)]],outputs=[dict(path=str(p),sha256=sha(p))for p in[out,R/'census.py']],scope='exact candidate and evidence destination only; no assertion or timeout changes; material census unchanged by HEAL gate'),indent=2),encoding='utf-8')

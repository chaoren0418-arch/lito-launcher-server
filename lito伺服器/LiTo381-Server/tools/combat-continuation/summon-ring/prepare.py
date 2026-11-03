from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent;N=R.parent/'summon-parity'
source=N/'build.py';text=source.read_text('utf-8')
changes=[('summon-material/build/candidate-summon-material.jar','summon-parity/build/candidate-summon-parity.jar'),
 ('0efdc606c0d5fe1fc5511cabcac9c8b0f227a72ba34fd4183abe323dcb302a54','a90494520095ce7b8be36a150437b6be9490864d36b6a9693eda67fcfd86f6c4'),
 ('SummonSelectionMethods','SummonRingMethods'),("['ClientSession','Protocol']","['InventoryCatalog$Definition']"),
 ('candidate-summon-parity.jar\'\n        with','candidate-summon-ring.jar\'\n        with')]
for old,new in changes:assert old in text,old;text=text.replace(old,new)
(R/'build.py').write_text(text,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(R/'adaptation.json').write_text(json.dumps(dict(source=dict(path=str(source),sha256=sha(source)),driver=dict(path=str(R/'build.py'),sha256=sha(R/'build.py')),changes=changes),indent=2),encoding='utf-8')

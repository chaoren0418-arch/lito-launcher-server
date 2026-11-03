"""Adapt only the two inspected Blind roots, retaining original target/probability rules."""
from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent;P=R.parent;D=P/'decay-material'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
t=(D/'DecayPotionMaterialMethods.java').read_text('utf-8').replace('DecayPotion','Blind')
t=t.replace('46,54,40,42,10,10','42,45,38,40,7,6').replace('45,53,39,41,10,10','46,51,42,44,10,5')
assert '42,45,38,40,7,6' in t and '46,51,42,44,10,5' in t
(R/'BlindMaterialMethods.java').write_text(t,encoding='utf-8')
t=(D/'NativeDecayPotionMaterialAction.java').read_text('utf-8').replace('DecayPotion','Blind').replace('skill!=71','skill!=40&&skill!=103')
(R/'NativeBlindMaterialAction.java').write_text(t,encoding='utf-8')
t=(D/'build.py').read_text('utf-8').replace('silence-material/build/candidate-silence-material.jar','target-negative-material/build/candidate-target-negative-material.jar').replace('51d02d4dae284d440024956adafb5e1c69a3c3c7f4c89fb3400c90b145afe889','c3c4c84f7efcf8ab9c8513f10b58457f5c7056c436a13872f50e049dfeedf882').replace('silence-material/build/inputs/sources/NativeLightAction.java','target-negative-material/build/inputs/sources/NativeLightAction.java')
t=t.replace("needle='&&skill!=64'","needle='&&skill!=174'").replace("needle+'&&skill!=71'","needle+'&&skill!=40&&skill!=103'")
t=t.replace('note.contains("Silence target=")','note.contains("Striker Gale target=")').replace('||note.contains("Decay Potion target=")','||note.contains("Blind target=")||note.contains("blind target=")')
t=t.replace('DecayPotion','Blind').replace('candidate-decay-material.jar','candidate-blind-material.jar')
(R/'build.py').write_text(t,encoding='utf-8')
inputs=[D/'DecayPotionMaterialMethods.java',D/'NativeDecayPotionMaterialAction.java',D/'build.py',P/'build/blind-material-audit/inputs.json',Path(__file__)]
(R/'adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in inputs],scope='two existing Blind40/103 roots; exact monitor/local layout from candidate bytecode; paired Source admission and transactional materials; no target eligibility redesign'),indent=2),encoding='utf-8')

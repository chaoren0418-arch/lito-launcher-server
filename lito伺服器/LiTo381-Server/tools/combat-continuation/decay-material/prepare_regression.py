"""Adapt pinned regression destinations; keep behavioral assertions unchanged."""
from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent;P=R.parent
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
source=P/'silence_regression.py'
t=source.read_text('utf-8').replace('silence-material','decay-material').replace('silence-regression','decay-regression').replace('silence-candidate','decay-candidate').replace('exact silence candidate','exact decay candidate')
out=P/'decay_regression.py';out.write_text(t,encoding='utf-8')
census=P/'silence-material/census.py'
t=census.read_text('utf-8').replace('silence-material','decay-material').replace('32\\t33','30\\t31').replace('remaining_methods=32,remaining_legacy_calls=33','remaining_methods=30,remaining_legacy_calls=31')
t=t.replace("'nativeMaterialWorldSilenceBody']","'nativeMaterialWorldSilenceBody','useDecayPotionSpell','useClassicWorldDecayPotionSpell','nativeMaterialDecayPotionBody','nativeMaterialWorldDecayPotionBody']")
(R/'census.py').write_text(t,encoding='utf-8')
(R/'regression-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in[source,census,Path(__file__)]],outputs=[dict(path=str(p),sha256=sha(p))for p in[out,R/'census.py']],scope='regression evidence destination and exact candidate only; static census removes two Decay Potion roots'),indent=2),encoding='utf-8')

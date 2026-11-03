from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent;P=R.parent;sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
source=P/'world_potion_regression.py';out=P/'target_negative_regression.py'
t=source.read_text('utf-8').replace('world-potion','target-negative-material').replace('target-negative-material-regression','target-negative-regression')
out.write_text(t,encoding='utf-8')
census=P/'decay-material/census.py'
t=census.read_text('utf-8').replace('decay-material','target-negative-material').replace('30\\t31','29\\t30').replace('remaining_methods=30,remaining_legacy_calls=31','remaining_methods=29,remaining_legacy_calls=30')
t=t.replace("'nativeMaterialWorldDecayPotionBody']","'nativeMaterialWorldDecayPotionBody','useTargetNegativeStatusSpell','nativeMaterialTargetNegativeBody']")
(R/'census.py').write_text(t,encoding='utf-8')
(R/'regression-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in[source,census,Path(__file__)]],outputs=[dict(path=str(p),sha256=sha(p))for p in[out,R/'census.py']],scope='candidate and evidence destination only; removes one shared material root in census'),indent=2),encoding='utf-8')

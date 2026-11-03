"""Adapt pinned regression destinations; keep every behavioral assertion."""
from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent;P=R.parent
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
source=P/'ground_regression.py'
t=source.read_text('utf-8').replace('ground-material','silence-material').replace('ground-regression','silence-regression').replace('ground-candidate','silence-candidate').replace('exact ground candidate','exact silence candidate')
out=P/'silence_regression.py';out.write_text(t,encoding='utf-8')
census=P/'ground-material/census.py'
t=census.read_text('utf-8').replace('ground-material','silence-material').replace('34\\t35','32\\t33').replace('remaining_methods=34,remaining_legacy_calls=35','remaining_methods=32,remaining_legacy_calls=33')
t=t.replace("'nativeMaterialWorldGroundBody']","'nativeMaterialWorldGroundBody','useSilenceSpell','useClassicWorldSilenceSpell','nativeMaterialSilenceBody','nativeMaterialWorldSilenceBody']")
(R/'census.py').write_text(t,encoding='utf-8')
(R/'regression-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in[source,census,Path(__file__)]],outputs=[dict(path=str(p),sha256=sha(p))for p in[out,R/'census.py']],scope='regression evidence destination and exact candidate only; static census removes two single-target Silence roots; not Area Silence qualification'),indent=2),encoding='utf-8')

from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent;P=R.parent;sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
source=P/'target_negative_regression.py';out=P/'blind_regression.py'
t=source.read_text('utf-8').replace('target-negative-material','blind-material').replace('target-negative-regression','blind-regression')
out.write_text(t,encoding='utf-8')
source_census=P/'target-negative-material/census.py'
t=source_census.read_text('utf-8').replace('target-negative-material','blind-material').replace('29\\t30','27\\t28').replace('remaining_methods=29,remaining_legacy_calls=30','remaining_methods=27,remaining_legacy_calls=28')
t=t.replace("'nativeMaterialTargetNegativeBody']","'nativeMaterialTargetNegativeBody','useBlindSpell','useClassicWorldBlindSpell','nativeMaterialBlindBody','nativeMaterialWorldBlindBody']")
(R/'census.py').write_text(t,encoding='utf-8')
(R/'regression-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in[source,source_census,Path(__file__)]],outputs=[dict(path=str(p),sha256=sha(p))for p in[out,R/'census.py']],scope='candidate/evidence destination only; two remaining legacy material roots removed from census; no behavioral assertions or deadlines changed'),indent=2),encoding='utf-8')

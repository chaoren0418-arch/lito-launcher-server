from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent;P=R.parent
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
t=(P/'negative_regression.py').read_text('utf-8').replace('three-negative-material','ground-material').replace('negative-regression','ground-regression').replace('negative-material','ground-material').replace('negative-candidate','ground-candidate').replace('exact negative candidate','exact ground candidate')
out=P/'ground_regression.py';out.write_text(t,encoding='utf-8')
t=(P/'weakness-material/census.py').read_text('utf-8').replace('weakness-material','ground-material').replace('36\\t37','34\\t35').replace('remaining_methods=36,remaining_legacy_calls=37','remaining_methods=34,remaining_legacy_calls=35')
t=t.replace("'nativeMaterialWorldWeaknessBody']","'nativeMaterialWorldWeaknessBody','useGroundSpell','useClassicWorldGroundSpell','nativeMaterialGroundBody','nativeMaterialWorldGroundBody']")
(R/'census.py').write_text(t,encoding='utf-8')
(R/'regression-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in[P/'negative_regression.py',P/'weakness-material/census.py',Path(__file__)]],outputs=[dict(path=str(p),sha256=sha(p))for p in[out,R/'census.py']],scope='full regression destination and candidate only; census removes exactly the two ground roots'),indent=2),encoding='utf-8')

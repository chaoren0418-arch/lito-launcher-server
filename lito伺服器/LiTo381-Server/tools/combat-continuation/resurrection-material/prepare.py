from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent;P=R.parent;B=P/'blind-material';sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
t=(B/'NativeBlindMaterialAction.java').read_text('utf-8').replace('Blind','Resurrection').replace('Protocol.attackSpellRequest','Protocol.objectSpellRequest').replace('skill!=40&&skill!=103','skill!=61&&skill!=75')
(R/'NativeResurrectionMaterialAction.java').write_text(t,encoding='utf-8')
t=(B/'build.py').read_text('utf-8').replace('target-negative-material/build/candidate-target-negative-material.jar','blind-material/build/candidate-blind-material.jar').replace('c3c4c84f7efcf8ab9c8513f10b58457f5c7056c436a13872f50e049dfeedf882','dd9c336f14976831ac8c7a91b660a0fb2e4e775f66385688a59848fcbe774b91').replace('target-negative-material/build/inputs/sources/NativeLightAction.java','blind-material/build/inputs/sources/NativeLightAction.java')
t=t.replace("needle='&&skill!=174'","needle='&&skill!=103'").replace("needle+'&&skill!=40&&skill!=103'","needle+'&&skill!=61&&skill!=75'")
a=t.index("needle='note.contains(");b=t.index('(src/light.name)',a)
t=t[:a]+'''needle='note.contains("Striker Gale target=")';assert t.count(needle)==1
t=t.replace(needle,'('+needle+'||note.contains("resurrection target="))')
'''+t[b:]
t=t.replace('Blind','Resurrection').replace('candidate-blind-material.jar', 'candidate-resurrection-material.jar')
# The replacement above applies only to output naming; preserve the pinned Blind parent.
t=t.replace("jar=R.parent/'blind-material/build/candidate-resurrection-material.jar'","jar=R.parent/'blind-material/build/candidate-blind-material.jar'")
(R/'build.py').write_text(t,encoding='utf-8')
(R/'adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in[B/'build.py',B/'NativeBlindMaterialAction.java',P/'build/resurrection-material-audit/inputs.json',R/'ResurrectionMaterialMethods.java',Path(__file__)]],scope='resource boundary for skill61/75 only; original resurrection ask/answer and target rules retained; behavioral qualification pending'),indent=2),encoding='utf-8')

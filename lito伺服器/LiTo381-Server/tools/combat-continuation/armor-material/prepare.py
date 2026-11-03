from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent;P=R.parent/'protection-material'
t=(P/'build.py').read_text('utf-8')
for a,b in [("jar=R.parent/'water-life-material/build/candidate-water-life-material.jar'","jar=R.parent/'protection-material/build/candidate-protection-material.jar'"),('c83d82a84676a984a3d5e63e41a52f4e564359b3ba93c0ff6923e2748276fed7','aa8b7c64a9f0adc07f1b32a216445c6d9686f017c2142418b7d86ea87ff729ba'),('water-life-material/build/inputs/sources/NativeLightAction.java','protection-material/build/inputs/sources/NativeLightAction.java'),('ProtectionMethods','ArmorMaterialMethods'),('NativeProtectionCastAction','NativeArmorMaterialAction'),("candidate=D/'candidate-protection-material.jar'","candidate=D/'candidate-armor-material.jar'")]:t=t.replace(a,b)
start=t.index("t=light.read_text('utf-8');");end=t.index("shutil.copyfile(R/'NativeArmorMaterialAction.java'",start)
t=t[:start]+'''t=light.read_text('utf-8');needle='&&skill!=68';assert t.count(needle)==1;t=t.replace(needle,needle+'&&skill!=112')
needle='note.startsWith("S_EFFECT immune-to-harm target=")';assert t.count(needle)==1
t=t.replace(needle,'('+needle+'||note.startsWith("S_EFFECT Armor Break target="))')
(src/light.name).write_text(t,encoding='utf-8')
'''+t[end:]
(R/'build.py').write_text(t,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(R/'adaptation.json').write_text(json.dumps(dict(parent=dict(path=str(P/'build.py'),sha256=sha(P/'build.py')),scope='skill112 material and ordered PC pair; original Source PC-only target filter retained; behavior unqualified'),indent=2),encoding='utf-8')

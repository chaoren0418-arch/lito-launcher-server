"""Adapt only the inspected single-target Decay Potion roots; retain evidence pins."""
from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent;P=R.parent;I=P/'silence-material';W=P/'weakness-material'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def adapt(name):return (I/name).read_text('utf-8').replace('Silence','DecayPotion').replace('silence','decayPotion')
t=adapt('SilenceMaterialMethods.java')
t=t.replace('48,56,42,44,10,10','46,54,40,42,10,10').replace('47,55,41,43,10,10','45,53,39,41,10,10')
(R/'DecayPotionMaterialMethods.java').write_text(t,encoding='utf-8')
t=adapt('NativeSilenceMaterialAction.java').replace('skill!=64','skill!=71')
(R/'NativeDecayPotionMaterialAction.java').write_text(t,encoding='utf-8')
t=adapt('build.py').replace("ground-material/build/candidate-ground-material.jar","silence-material/build/candidate-silence-material.jar").replace('9c5e60af1702d4e99d6a25da0fad1ce4ad66b1eeeb62db6637423b8145d7572d','51d02d4dae284d440024956adafb5e1c69a3c3c7f4c89fb3400c90b145afe889').replace("ground-material/build/inputs/sources/NativeLightAction.java","silence-material/build/inputs/sources/NativeLightAction.java")
t=t.replace("needle='&&skill!=63'","needle='&&skill!=64'").replace("needle+'&&skill!=64'","needle+'&&skill!=71'").replace('note.contains("Disease target=")','note.contains("Silence target=")').replace('note.contains("DecayPotion target=")','note.contains("Decay Potion target=")').replace('candidate-decayPotion-material.jar','candidate-decay-material.jar')
(R/'build.py').write_text(t,encoding='utf-8')
t=adapt('SilenceMaterialProbe.java').replace('SKILLS={64}','SKILLS={71}').replace('Set.of(2,64)','Set.of(2,71)').replace('skill64','skill71')
start=t.index('   if(mode==12){');end=t.index('   var self=collect(wire);',start)
t=t[:start]+t[end:]
t='\n'.join(line for line in t.splitlines() if 'if(mode==12)need(peers.contains' not in line)+'\n'
# The retained 104-case failure exposed the independent WORLD HEAL phase gate.
# That real consumer/expiry assertion now lives in world-potion/WorldPotionProbe,
# with explicit before/after evidence; do not mark the unpatched consumer passed.
t=t.replace('mode<13','mode<12')
(R/'DecayPotionMaterialProbe.java').write_text(t,encoding='utf-8')
t=adapt('test.py').replace("v[mode]['cases']==104","v[mode]['cases']==96")
(R/'test.py').write_text(t,encoding='utf-8')
(R/'DecayPotionFaultProbe.java').write_text(adapt('SilenceFaultProbe.java'),encoding='utf-8')
t=(W/'WeaknessLifecycleProbe.java').read_text('utf-8').replace('Weakness','DecayPotion').replace('weaknessFixturePercentileRoll','decayPotionFixturePercentileRoll')
(R/'DecayPotionLifecycleProbe.java').write_text(t,encoding='utf-8')
(R/'test_boundaries.py').write_text(adapt('test_boundaries.py'),encoding='utf-8')
(R/'DecayPotionWorldNpcProbe.java').write_text(adapt('SilenceWorldNpcProbe.java'),encoding='utf-8')
(R/'test_world_npcs.py').write_text(adapt('test_world_npcs.py'),encoding='utf-8')
inputs=[*I.glob('*.java'),I/'build.py',I/'test.py',I/'test_boundaries.py',I/'test_world_npcs.py',W/'WeaknessLifecycleProbe.java',P/'build/sleep-decay-audit/inputs.json',Path(__file__)]
(R/'adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in inputs],scope='skill71 only; original eligibility/probability/counter/status bodies retained; potion consumer and full target qualification remain separate'),indent=2),encoding='utf-8')

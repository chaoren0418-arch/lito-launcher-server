from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent;N=R.parent/'negative-material'
t=(N/'NegativeWorldNpcProbe.java').read_text('utf-8')
t=t.replace('NegativeWorldNpcProbe extends NegativeMaterialProbe','WeaknessWorldNpcProbe extends WeaknessMaterialProbe')
old='return skill==153?hub.classicWorldMonsterEffectiveMagicResistance(map,instance,object,100,System.nanoTime())==25:hub.classicWorldMonsterSlowActive(map,instance,object,System.nanoTime());'
new='return skill==56?hub.classicWorldMonsterDiseaseActive(map,instance,object,System.nanoTime()):hub.classicWorldMonsterWeaknessActive(map,instance,object,System.nanoTime());'
assert old in t;t=t.replace(old,new)
t=t.replace('if(mode!=7)need(active(hub,pc,object,skill)==(accepted&&mode!=1),','need(active(hub,pc,object,skill)==(accepted&&mode!=1),')
t=t.replace('Map.of("battleNaturalHpRegenEnabled",false,','Map.of("weaknessFixturePercentileRoll",mode==1?100:1,"battleNaturalHpRegenEnabled",false,')
(R/'WeaknessWorldNpcProbe.java').write_text(t,encoding='utf-8')
t=(N/'test_world_npcs.py').read_text('utf-8').replace('NegativeWorldNpcProbe','WeaknessWorldNpcProbe').replace("v[mode]['cases']==96","v[mode]['cases']==64").replace("(12 if mode=='before' else 0)","(8 if mode=='before' else 0)")
(R/'test_world_npcs.py').write_text(t,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(R/'world-npc-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in[N/'NegativeWorldNpcProbe.java',N/'test_world_npcs.py',Path(__file__)]],scope='actual WORLD population, native Weakness/Disease packets and existing percentile fixture control; no companion or Quest/Time Attack qualification'),indent=2),encoding='utf-8')

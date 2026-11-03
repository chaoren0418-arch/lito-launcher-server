from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent;M=R.parent/'summon-material'
source=M/'test.py';text=source.read_text('utf-8')
text=text.replace('SummonMaterialProbe','WaterLifeMaterialProbe').replace("v[mode]['cases']==60","v[mode]['cases']==64")
(R/'test.py').write_text(text,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(R/'test-adaptation.json').write_text(json.dumps(dict(source=dict(path=str(source),sha256=sha(source)),driver=dict(path=str(R/'test.py'),sha256=sha(R/'test.py')),changes='probe class and expected case count only; own build base retained'),indent=2),encoding='utf-8')
text=(M/'SummonFaultProbe.java').read_text('utf-8').replace('Summon','WaterLife')
text=text.replace('static ClientSession active;','static ClientSession active,recipient;')
start=text.index('  reset(pc,peer,hub,world);');end=text.index('material=CombatDatabase.value',start)
text=text[:start]+'  setup(pc,peer,hub,world);recipient=peer;'+text[end:]
text=text.replace('set(pc,"positionX",100);set(pc,"positionY",120);','')
text=text.replace('need(((List<?>)get(active,"tamedCompanions")).isEmpty(),"no summoned effect before material save");','need(((BattleWaterLifeStatus)get(recipient,"waterLifeStatus")).active(System.nanoTime())==before,"target effect waits for material save success");')
text=text.replace('need(((List<?>)get(pc,"tamedCompanions")).isEmpty(),"failed material cannot create summon");','need(((BattleWaterLifeStatus)get(peer,"waterLifeStatus")).active(System.nanoTime())==before,"failed material cannot activate target status");')
(R/'WaterLifeFaultProbe.java').write_text(text,encoding='utf-8')
text=(M/'test_boundaries.py').read_text('utf-8').replace('Summon','WaterLife')
text=text.replace("('Stopped','after',jar,8),('Edges','after',jar,32)","('Lifecycle','after',jar,56)")
(R/'test_boundaries.py').write_text(text,encoding='utf-8')

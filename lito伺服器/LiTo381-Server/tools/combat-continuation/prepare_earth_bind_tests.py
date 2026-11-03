"""Adapt frozen test shapes to Earth's own status, native lock packets and fixture."""
from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent;src=R/'blind-material';dest=R/'earth-bind-material'
inputs=[]
for name in ['BlindMaterialProbe.java','BlindFaultProbe.java','BlindLifecycleProbe.java','test.py','test_boundaries.py']:
 p=src/name;inputs.append({'path':str(p),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
 t=p.read_text('utf-8').replace('Blind','EarthBind').replace('darkEarthBindFixtureRoll','earthBindFixturePercentileRoll')
 t=t.replace('SKILLS={40,103}','SKILLS={157}').replace('BattleSpellCatalog.blindSpell(skill)','BattleSpellCatalog.bindSpell(skill)').replace('selectedSkill==103?4:3','2').replace('Set.of(2,40,103)','Set.of(2,157)')
 t=t.replace('.blindActive(','.earthBindActive(').replace('"blindExpiresAtNanos"','"earthBindExpiresAtNanos"').replace('.expireEarthBind(','.expireEarthBind(').replace('.expireBlind(','.expireEarthBind(')
 if name=='BlindMaterialProbe.java':
  t=t.replace('Protocol.blind(1)','Protocol.paralysis(Protocol.ParalysisMode.FREEZE_APPLY)').replace('actual blind mode1 packet','actual Earth Bind mode12 packet')
  t=t.replace('Native skills40/103','Native skill157')
 if name=='BlindFaultProbe.java':t=t.replace('faults==16','faults==8')
 if name=='test.py':t=t.replace("['cases']==208","['cases']==104").replace('(16 if mode==\'before\' else 0)','(8 if mode==\'before\' else 0)')
 if name=='test_boundaries.py':
  t=t.replace("('Fault','before',old,16)","('Fault','before',old,8)").replace("('Fault','after',jar,16)","('Fault','after',jar,8)").replace("('Lifecycle','after',jar,112)","('Lifecycle','after',jar,56)")
  t=t.replace("['actual_memory_faults']==16","['actual_memory_faults']==8").replace("(16 if mode=='before' else 0)","(8 if mode=='before' else 0)")
 q=dest/name.replace('Blind','EarthBind');assert not q.exists(),q;q.write_text(t,encoding='utf-8')
(dest/'test-adaptation.json').write_text(json.dumps({'status':'prepared-not-run','inputs':inputs,'changes':'skill157 BindSpell, native earth-bind fixture and status, FREEZE_APPLY packet, one-skill case counts; all preconditions and fault/lifetime assertions retained pending execution'},ensure_ascii=False,indent=2),encoding='utf-8')

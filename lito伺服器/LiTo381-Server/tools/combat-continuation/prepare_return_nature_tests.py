"""Reuse the narrow fault fixture; do not reuse PC-status assumptions for a retired actor."""
from pathlib import Path
R=Path(__file__).resolve().parent;P=R/'return-nature-material';E=R/'earth-bind-material'
def rep(t,a,b):
 assert t.count(a)==1,(a,t.count(a));return t.replace(a,b)
def put(n,t):
 p=P/n;assert not p.exists(),p;p.write_text(t,encoding='utf-8')
t=(E/'EarthBindFaultProbe.java').read_text('utf-8').replace('EarthBind','ReturnNature')
t=rep(t,'static int expectedCost,','static final int[] SKILLS={145};static BattleCompanion pet;static int expectedCost,')
t=rep(t,'skill=selectedSkill;ReturnNatureMaterialProbe.selectedSkill=skill;ReturnNatureMaterialProbe.selectedMode=0;probability(skill);','skill=selectedSkill;probability();')
t=rep(t,'setup(pc,peer,hub,world);recipient=peer;','setup(pc,peer,hub,world);pet=companion(peer,(int)get(pc,"positionX")+2,(int)get(pc,"positionY")+2,false);targetId=pet.snapshot().objectId();recipient=peer;')
t=t.replace('!active(recipient,skill)','pet.snapshot().active()').replace('!active(peer,skill)','pet.snapshot().active()')
t=t.replace('ReturnNatureMaterialProbe.cast(pc,skill,spell(skill))','ReturnNatureMaterialProbe.cast(pc)')
t=t.replace('"earthBindFixturePercentileRoll",1','"returnToNatureFixturePercentileRoll",1')
put('ReturnNatureFaultProbe.java',t)
t=(E/'test_boundaries.py').read_text('utf-8').replace('EarthBind','ReturnNature')
t=rep(t,"[('Fault','before',old,8),('Fault','after',jar,8),('Lifecycle','after',jar,56)]","[('Fault','before',old,8),('Fault','after',jar,8)]")
put('test_boundaries.py',t)

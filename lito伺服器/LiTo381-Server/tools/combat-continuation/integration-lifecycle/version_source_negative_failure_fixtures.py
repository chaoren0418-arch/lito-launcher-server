"""Explicit three-skill projections from prior Source-state/pair fixtures, with whole-file inverses."""
from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent;out=R/'source-negative-failure-fixtures';out.mkdir(exist_ok=True)
pin=lambda p:dict(path=str(p.resolve()),sha256=hashlib.sha256(p.read_bytes()).hexdigest());rows=[]
for suffix in ['State','Boundary','Io','Fault']:
 p=R/('SourceMassSlow'+suffix+'Probe.java');s=p.read_text('utf-8');changes=[]
 def change(old,new):
  global s
  assert old in s,(suffix,old);changes.append([old,new]);s=s.replace(old,new)
 change('SourceMassSlow'+suffix+'Probe','SourceNegativeFailure'+suffix+'Probe')
 change('"type",selectedSkill==29?2:3','"type",selectedSkill==29?3:2')if '"type",selectedSkill==29?2:3'in s else change('"type",3','"type",selectedSkill==29?3:2')
 change('set(pc,"learnedSkills",','set(pc,"elfAttribute",8);set(pc,"learnedSkills",')
 change('new int[]{76}','new int[]{29,152,153}')
 if suffix=='State':
  change('Set.of(76)','Set.of(selectedSkill)');change('x+12','x+6');change('var spell=BattleSpellCatalog.massSlowSpell(76);','Object spell=selectedSkill==153?BattleSpellCatalog.eraseMagicSpell(selectedSkill):BattleSpellCatalog.entangleSpell(selectedSkill);');change('NativeSkillTargetMetadata.row(76)','NativeSkillTargetMetadata.row(selectedSkill)');change('metadata.area()==3','metadata.area()==0');change('metadata.ranged()==-1','metadata.ranged()==(selectedSkill==152?10:-1)');change('probabilities.put(76,new SourceSkillControls.Probability(76,','probabilities.put(selectedSkill,new SourceSkillControls.Probability(selectedSkill,');change('data.skills().get(76)','data.skills().get(selectedSkill)');change('.c(9).c(3)','.c((selectedSkill-1)>>3).c((selectedSkill-1)&7)');change('cost(pc,76,spell)','cost(pc,selectedSkill,spell)');change('spell.effectGfx()','value(spell,"effectGfx")');change('need(pet.captureEraseMagic(System.nanoTime()).equals(erase)&&timed.sleepActive(System.nanoTime())==sleep&&timed.counterMagicActive(System.nanoTime())==counter,','if(!before)need(pet.captureEraseMagic(System.nanoTime()).equals(erase)&&timed.sleepActive(System.nanoTime())==sleep&&timed.counterMagicActive(System.nanoTime())==counter,');change('if(changed)mutationGaps++;','if(changed||!pet.captureEraseMagic(System.nanoTime()).equals(erase)||timed.sleepActive(System.nanoTime())!=sleep||timed.counterMagicActive(System.nanoTime())!=counter)mutationGaps++;');change('cases==72&&(before?admissionGaps>0&&mutationGaps>0:admissionGaps==0&&mutationGaps==0)','cases==216&&(before?admissionGaps>0||mutationGaps>0:admissionGaps==0&&mutationGaps==0)')
 elif suffix=='Boundary':
  change('mode==1?18:12','mode==1?18:6');change('NativeSkillTargetMetadata.row(selectedSkill).ranged()==-1','NativeSkillTargetMetadata.row(selectedSkill).ranged()==(selectedSkill==152?10:-1)');change('boolean eligible=selectedSkill!=29||SourceNpcPhysicalStats.memoryFixture(pet.snapshot().npcId()).eraseEligible();','boolean eligible=true;');change('SourceMaterialRangeProbe.selected(selectedSkill)','(selectedSkill==153?BattleSpellCatalog.eraseMagicSpell(selectedSkill):BattleSpellCatalog.entangleSpell(selectedSkill))');change('cases==696&&waits==336','cases==1044&&waits==504');change('for(boolean phase:new boolean[]{false,true})','for(boolean phase:new boolean[]{false})')
 elif suffix in ['Io','Fault']:
  change('Set.of(76)','Set.of(selectedSkill)');change('x+12','x+6')if 'x+12'in s else None;change('x+13','x+7')if 'x+13'in s else None;change('var spell=BattleSpellCatalog.massSlowSpell(76);','Object spell=selectedSkill==153?BattleSpellCatalog.eraseMagicSpell(selectedSkill):BattleSpellCatalog.entangleSpell(selectedSkill);');change('.c(9).c(3)','.c((selectedSkill-1)>>3).c((selectedSkill-1)&7)');change('cost(pc,76,spell)','cost(pc,selectedSkill,spell)')if 'cost(pc,76,spell)'in s else None
  for name in ['itemConsumeId','itemConsumeCount']:change('spell.'+name+'()','value(spell,"'+name+'")')
  change('for(boolean phase:new boolean[]{false,true})','for(boolean phase:new boolean[]{false})')
  if suffix=='Io':change('new int[]{29,152,153}','new int[]{29,152}')
  else:
   change('return actor.snapshot().moveSpeed()!=BattleTimedStatus.HASTE;','return actor.snapshot().moveSpeed()!=BattleTimedStatus.HASTE||actor.captureEraseMagic(System.nanoTime()).present();');change('faultCases==48&&faults==48','faultCases==72&&faults==72')
 q=out/('SourceNegativeFailure'+suffix+'Probe.java');q.write_text(s,'utf-8');inv=s
 for old,new in reversed(changes):assert new in inv;inv=inv.replace(new,old)
 assert inv==p.read_text('utf-8');rows.append(dict(original=pin(p),versioned=pin(q),changes=changes))
(out/'manifest.json').write_text(json.dumps(dict(status='versioned-only-three-Source-probability-skills-and-guaranteed-controlled-failure',rows=rows,native_cases=0),ensure_ascii=False,indent=2)+'\n','utf-8')
print('PASS two inverse projections; original fixtures unchanged')

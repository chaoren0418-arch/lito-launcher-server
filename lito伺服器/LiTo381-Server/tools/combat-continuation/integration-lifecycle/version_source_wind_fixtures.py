"""Explicit auditable167 projections; preserve prior sealed fixtures byte for byte."""
from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent;out=R/'source167-fixtures';out.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
rows=[]
for suffix in ['State','Boundary','PlayerBoundary','Io','PlayerIo','Fault']:
 p=R/('SourceMassSlow'+suffix+'Probe.java');text=p.read_text('utf-8');changes=[]
 def change(old,new):
  global text
  assert old in text,(suffix,old);changes.append([old,new]);text=text.replace(old,new)
 change('SourceMassSlow'+suffix+'Probe','SourceWind'+suffix+'Probe')
 change('76','167')
 change('BattleSpellCatalog.massSlowSpell','BattleSpellCatalog.windShackleSpell') if 'BattleSpellCatalog.massSlowSpell' in text else None
 change('"type",selectedSkill==29?2:3','"type",2') if '"type",selectedSkill==29?2:3' in text else change('"type",3','"type",2')
 change('set(pc,"learnedSkills",','set(pc,"elfAttribute",8);set(pc,"learnedSkills",')
 if '.c(9).c(3)' in text:change('.c(9).c(3)','.c(20).c(6)')
 if 'SourceMaterialRangeProbe.selected(selectedSkill)' in text:change('SourceMaterialRangeProbe.selected(selectedSkill)','BattleSpellCatalog.windShackleSpell(selectedSkill)')
 if suffix=='Fault':
  change('return actor.snapshot().moveSpeed()!=BattleTimedStatus.HASTE;','return NativeCompanionWindState.active(actor,System.nanoTime());')
 if suffix=='State':
  change('x+12','x+6')
  change('metadata.area()==3','metadata.area()==0')
  change('boolean changed=pet.snapshot().moveSpeed()!=BattleTimedStatus.HASTE;','boolean changed=pet.snapshot().moveSpeed()!=BattleTimedStatus.HASTE||NativeCompanionWindState.active(pet,System.nanoTime());')
  change('need(pet.captureEraseMagic(System.nanoTime()).equals(erase)&&timed.sleepActive(System.nanoTime())==sleep&&timed.counterMagicActive(System.nanoTime())==counter,','if(!before)need(pet.captureEraseMagic(System.nanoTime()).equals(erase)&&timed.sleepActive(System.nanoTime())==sleep&&timed.counterMagicActive(System.nanoTime())==counter,')
  change('if(changed)mutationGaps++;','if(changed||!pet.captureEraseMagic(System.nanoTime()).equals(erase)||timed.sleepActive(System.nanoTime())!=sleep||timed.counterMagicActive(System.nanoTime())!=counter)mutationGaps++;')
  change('(before?admissionGaps>0&&mutationGaps>0:admissionGaps==0&&mutationGaps==0)','(before?mutationGaps>0:admissionGaps==0&&mutationGaps==0)')
 q=out/('SourceWind'+suffix+'Probe.java');q.write_text(text,'utf-8');inverse=text
 for old,new in reversed(changes):assert new in inverse;inverse=inverse.replace(new,old)
 assert inverse==p.read_text('utf-8'),suffix
 rows.append(dict(original=pin(p),versioned=pin(q),changes=changes))
(out/'manifest.json').write_text(json.dumps(dict(status='versioned-only-explicit167-metadata-and-Source-class-failure',rows=rows,native_cases=0,all_target_intent_policy_qualified=False),ensure_ascii=False,indent=2)+'\n','utf-8')
print('PASS six whole-file inverse fixture projections; original fixtures unchanged')

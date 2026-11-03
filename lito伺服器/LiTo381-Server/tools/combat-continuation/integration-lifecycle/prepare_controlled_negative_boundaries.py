"""New owned probes derived from sealed blind fixtures; sealed sources and their assertions are retained."""
from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent
for kind in ['Lifecycle','Publication','Fault']:
 source=R/('BlindNpcFaultProbe.java'if kind=='Fault'else f'BlindScene{kind}Probe.java');text=source.read_text('utf-8');name=f'ControlledNegative{kind}Probe';old=source.stem;text=text.replace(old,name).replace('new int[]{40,103}','new int[]{173,174}').replace('BattleSpellCatalog.blindSpell(skill)','ControlledNegativeActorProbe.row(skill)').replace('BattleSpellCatalog.blindSpell(selectedSkill)','ControlledNegativeActorProbe.row(selectedSkill)').replace('"type",skill==103?4:3','"type",2').replace('set(caster,"elfAttribute",8)','set(caster,"elfAttribute",skill==173?4:8)').replace('if(skill==103){set(caster,"positionX",pet.snapshot().x());set(caster,"positionY",pet.snapshot().y());}','').replace('BlindSceneProbe.active(pet)','ControlledNegativeActorProbe.status(pet,skill)')
 if kind=='Fault':
  text=text.replace('prepare(pc,peer,hub);','ControlledNegativeActorProbe.prepareNegative(pc,peer,hub);').replace('!active(pet)','!ControlledNegativeActorProbe.status(pet,selectedSkill)').replace('need(active(pet)&&','if(!ControlledNegativeActorProbe.status(pet,selectedSkill))retryMisses++;need(').replace('static int faults,faultCases;','static int faults,faultCases,retryMisses;')
  text=text.replace('\\"actual_memory_faults\\":','\\"retry_probability_misses\\":'+ '\"+retryMisses+\",\\"actual_memory_faults\\":')
 elif kind=='Lifecycle':text=text.replace('checked==64&&scenarios==64&&queued==48','checked>=64&&scenarios==64&&queued>=48')
 else:text=text.replace('checked==32&&scenarios==32','checked>=32&&scenarios==32')
 (R/(name+'.java')).write_text(text,'utf-8');print(json.dumps(dict(original=str(source),sha256=hashlib.sha256(source.read_bytes()).hexdigest(),new=name)))

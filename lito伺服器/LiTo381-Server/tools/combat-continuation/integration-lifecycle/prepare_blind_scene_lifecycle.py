"""Create separately owned probes from the sealed failed-portal scenario, retaining its assertions."""
from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent;D=R/'build'
for suffix in ['Lifecycle','Publication']:
 source=R/f'CompanionWind{suffix}Probe.java';text=source.read_text('utf-8');name=f'BlindScene{suffix}Probe'
 # No sealed fixture is edited. These new probes use the same real failed Source landing.
 text=text.replace(f'CompanionWind{suffix}Probe',name).replace('CompanionWindProbe.active(pet,System.nanoTime())','BlindSceneProbe.active(pet)').replace('BattleSpellCatalog.windShackleSpell(skill)','BattleSpellCatalog.blindSpell(skill)').replace('new int[]{167}','new int[]{40,103}').replace('"type",2,"level",99','"type",skill==103?4:3,"level",99')
 old='hub.transitionClassicWorld(caster,scene==0?0:4,x,y,5,-1);'
 assert text.count(old)==1
 text=text.replace(old,old+'if(skill==103){set(caster,"positionX",pet.snapshot().x());set(caster,"positionY",pet.snapshot().y());}')
 old='"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,'
 assert text.count(old)==1;text=text.replace(old,'"darkBlindFixtureRoll",1,'+old)
 if suffix=='Lifecycle':
  start=text.index('boolean waiting=false;');end=text.index('queued++;',start)
  text=text[:start]+'long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(!owner.nativePlayerActionLock().hasQueuedThread(worker)&&worker.isAlive()&&System.nanoTime()<until)Thread.yield();need(owner.nativePlayerActionLock().hasQueuedThread(worker),"actual selected-owner pair waits after frozen target capture");'+text[end:]
  assert text.count('checked>=32&&scenarios==32&&queued>=24')==1;text=text.replace('checked>=32&&scenarios==32&&queued>=24','checked==64&&scenarios==64&&queued==48')
 else:
  assert text.count('checked>=16&&scenarios==16')==1;text=text.replace('checked>=16&&scenarios==16','checked==32&&scenarios==32')
 text=text.replace('/** Actual raw167 paired companion lifecycle after a failed owner portal. */','/** Actual raw40/103 selected-owner pair after the real failed owner portal. */').replace('/** Actual raw 161 deferred companion publication after a failed owner portal. */','/** Actual raw40/103 deferred blind publication after the real failed owner portal. */')
 (R/f'{name}.java').write_text(text,'utf-8')
 print(json.dumps(dict(original=str(source),sha256=hashlib.sha256(source.read_bytes()).hexdigest(),new=str(R/f'{name}.java'))))

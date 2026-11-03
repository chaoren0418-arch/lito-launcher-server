from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent;W=R.parent/'water-life-material'
for name in ['Fault','Lifecycle']:
 t=(W/('WaterLife'+name+'Probe.java')).read_text('utf-8').replace('WaterLife','Armor').replace('170','112').replace('waterLifeSpell','armorBreakSpell').replace('water-life','armor-break').replace('Water-Life','Armor Break')
 t=t.replace('BattleArmorStatus','BattleWaterLifeStatus')
 t=t.replace('var status=(BattleWaterLifeStatus)get(peer,"waterLifeStatus");','')
 t=t.replace('status.active(System.nanoTime())','active(peer,hub,world)')
 t=t.replace('((BattleWaterLifeStatus)get(pc,"waterLifeStatus")).active(System.nanoTime())','active(pc,hub,world)')
 if name=='Fault':
  t=t.replace('need(((BattleWaterLifeStatus)get(recipient,"waterLifeStatus")).active(System.nanoTime())==before,"target effect waits for material save success");','need(!active(recipient,(GameHub)get(active,"hub"),String.valueOf(get(active,"phase")).equals("WORLD")),"original and candidate target effect follow successful material save");')
  t=t.replace('need(((BattleWaterLifeStatus)get(peer,"waterLifeStatus")).active(System.nanoTime())==before,"failed material cannot activate target status");','need(!active(peer,hub,world),"failed material cannot activate target status");')
 else:
  # Both actors own their own remembered target; LiTo skill112 sends only its selector.
  t=t.replace('call(pc,"handle",new Class[]{byte[].class},new Protocol.Writer().c(Protocol.C_USE_SPELL).c(21).c(1).d(target.playerObjectId()).bytes());','set(pc,"lastCombatTargetObjectId",target.playerObjectId());call(pc,"handle",new Class[]{byte[].class},new Protocol.Writer().c(Protocol.C_USE_SPELL).c(13).c(7).bytes());')
  t=t.replace('"each actor pays independent Source cost with original MP bounds");','"each actor pays independent Source cost with original MP bounds");need((int)get(pc,"playerHp")==4000-row.hpCost()&&(int)get(peer,"playerHp")==4000-row.hpCost(),"each accepted actor pays HP exactly once");')
 t=t.replace('Map.of("battleNaturalHpRegenEnabled",false,','Map.of("armorBreakFixtureD100",1,"battleNaturalHpRegenEnabled",false,')
 t=t.replace('exercise(p.session,m.session,hub,','set(f.session,"observer",true);set(o.session,"observer",true);exercise(p.session,m.session,hub,')
 (R/('Armor'+name+'Probe.java')).write_text(t,encoding='utf-8')
t=(W/'test_boundaries.py').read_text('utf-8').replace('WaterLife','Armor')
t=t.replace("old=Path(build['base']['path']);", "old=Path(read(R.parent/'protection-material/build/build.json')['runtime']['path']);")
(R/'test_boundaries.py').write_text(t,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(R/'boundary-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p)) for p in [W/'WaterLifeFaultProbe.java',W/'WaterLifeLifecycleProbe.java',W/'test_boundaries.py',Path(__file__)]],scope='original selector-only Armor Break cost before effect; source HP/MP; per-player last target; deterministic probability; no production fixture overrides'),indent=2),encoding='utf-8')

from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent;N=R.parent/'negative-material'
t=(N/'NegativeMaterialProbe.java').read_text('utf-8').replace('NegativeMaterialProbe','WeaknessMaterialProbe')
t=t.replace('SKILLS={29,152,153}','SKILLS={47,56}').replace('return skill==153?BattleSpellCatalog.eraseMagicSpell(skill):BattleSpellCatalog.entangleSpell(skill);','return BattleSpellCatalog.weaknessSpell(skill);')
t=t.replace('if(selectedMode==12)call(pc,"handle",new Class[]{byte[].class},packet);\n  else call(pc,skill==153?"useEraseMagicSpell":"useEntangleSpell",new Class[]{byte[].class,row.getClass(),int.class},packet,row,selectedMode==1?100:1);','call(pc,"handle",new Class[]{byte[].class},packet);')
t=t.replace('return skill==153?((BattleEraseMagicStatus)get(pc,"eraseMagicStatus")).active(System.nanoTime()):((BattleTimedStatus)get(pc,"timedStatus")).speed(System.nanoTime()).type()==BattleTimedStatus.SLOW;','return skill==56?((BattleTimedStatus)get(pc,"timedStatus")).diseaseActive(System.nanoTime()):((BattleTimedStatus)get(pc,"timedStatus")).weaknessActive(System.nanoTime());')
t=t.replace('selectedSkill==29?3:2','3').replace('Set.of(2,29,152,153)','Set.of(2,47,56)')
t=t.replace('if(mode!=12){boolean applied=accepted&&mode!=1&&(skill==153||mode!=10&&mode!=11);','{boolean applied=accepted&&mode!=1&&mode!=10;')
t=t.replace('Map.of("battleNaturalHpRegenEnabled",false,','Map.of("weaknessFixturePercentileRoll",mode==1?100:1,"battleNaturalHpRegenEnabled",false,')
t=t.replace('Actual selected roots with existing deterministic roll arguments; one native packet case per skill/phase/configuration.','Actual native packets for Weakness/Disease; existing percentile fixture setting controls the roll.')
t=t.replace('setup(pc,peer,hub,world);int skill=selectedSkill;Object row=spell(skill);probability(skill);','''setup(pc,peer,hub,world);int skill=selectedSkill;Object row=spell(skill);probability(skill);
  var field=CombatDatabase.class.getDeclaredField("current");field.setAccessible(true);var original=(CombatDatabase.Snapshot)field.get(null);
  if(mode==12){var abilities=new HashMap<>(original.controls().abilities());abilities.put(skill,Map.of("DMG",-17,"HIT",-9,"AC",31));field.set(null,copy(original,Map.of("controls",copy(original.controls(),Map.of("abilities",abilities)))));}
  int oldAc=peer.battleArmorClass();var weapon=(BattleWeaponBuffStatus)get(peer,"weaponBuffStatus");var oldBonus=CombatWeaponBuffs.weaponModifier(weapon,System.nanoTime(),peer);''')
t=t.replace('need(wire.locked+other.locked==0', '''boolean applied=active(peer,skill);var configured=CombatDatabase.controls().abilities().getOrDefault(skill,Map.of());
   var bonus=CombatWeaponBuffs.weaponModifier(weapon,System.nanoTime(),peer);
   need(bonus.meleeHit()-oldBonus.meleeHit()==(applied?configured.getOrDefault("HIT",0):0)&&bonus.meleeDamage()-oldBonus.meleeDamage()==(applied?configured.getOrDefault("DMG",0):0),"actual combat modifier consumes configured negative HIT/DMG exactly once");
   need(bonus.rangedHit()==oldBonus.rangedHit()&&bonus.rangedDamage()==oldBonus.rangedDamage(),"negative melee controls do not leak into ranged modifiers");
   need(peer.battleArmorClass()-oldAc==(applied&&skill==56?configured.getOrDefault("AC",0):0),"actual player AC follows configured Disease value");
   need(wire.locked+other.locked==0''')
t=t.replace('}cases++;','}field.set(null,original);cases++;')
(R/'WeaknessMaterialProbe.java').write_text(t,encoding='utf-8')
t=(N/'test.py').read_text('utf-8').replace('NegativeMaterialProbe','WeaknessMaterialProbe').replace("v[mode]['cases']==312","v[mode]['cases']==208").replace("(24 if mode=='before' else 0)","(16 if mode=='before' else 0)")
(R/'test.py').write_text(t,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(R/'test-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in[N/'NegativeMaterialProbe.java',N/'test.py',Path(__file__)]],scope='native47/56 packets; both original world/battle roots, probability, resource/material/counter/haste and timing boundaries; not NPC/companion qualification'),indent=2),encoding='utf-8')

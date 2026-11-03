from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent;P=R.parent;N=P/'negative-material';W=P/'weakness-material'
t=(N/'NegativeMaterialProbe.java').read_text('utf-8').replace('Negative','TargetNegative')
t=t.replace('SKILLS={29,152,153}','SKILLS={173,174}').replace('return skill==153?BattleSpellCatalog.eraseMagicSpell(skill):BattleSpellCatalog.entangleSpell(skill);','return skill==173?BattleSpellCatalog.polluteWaterSpell(skill):BattleSpellCatalog.strikerGaleSpell(skill);')
old='if(selectedMode==12)call(pc,"handle",new Class[]{byte[].class},packet);\n  else call(pc,skill==153?"useEraseMagicSpell":"useEntangleSpell",new Class[]{byte[].class,row.getClass(),int.class},packet,row,selectedMode==1?100:1);'
assert old in t;t=t.replace(old,'dispatch(pc,packet,row,skill,selectedMode);')
index=t.index(' static void probability(')
t=t[:index]+''' static void dispatch(ClientSession pc,byte[] packet,Object row,int skill,int mode)throws Exception{
  if(mode==12){call(pc,"handle",new Class[]{byte[].class},packet);return;}
  var kind=Class.forName("local.lito381.server.ClientSession$TargetNegativeStatusKind");Object value=Enum.valueOf((Class)kind,skill==173?"POLLUTE_WATER":"STRIKER_GALE");
  call(pc,"useTargetNegativeStatusSpell",new Class[]{byte[].class,BattleSpellCatalog.TargetNegativeStatusSpell.class,int.class,kind},packet,row,mode==1?100:1,value);
 }
'''+t[index:]
t=t.replace('return skill==153?((BattleEraseMagicStatus)get(pc,"eraseMagicStatus")).active(System.nanoTime()):((BattleTimedStatus)get(pc,"timedStatus")).speed(System.nanoTime()).type()==BattleTimedStatus.SLOW;','return skill==173?((BattleTimedStatus)get(pc,"timedStatus")).polluteWaterActive(System.nanoTime()):((BattleTimedStatus)get(pc,"timedStatus")).strikerGaleActive(System.nanoTime());')
t=t.replace('selectedSkill==29?3:2','2').replace('Set.of(2,29,152,153)','Set.of(2,173,174)')
t=t.replace('reset(pc,peer,hub,world);','reset(pc,peer,hub,world);for(var target:List.of(pc,peer))set(target,"elfAttribute",(int)call(spell(selectedSkill),"attribute",new Class[]{}));')
t=t.replace('if(mode!=12){boolean applied=accepted&&mode!=1&&(skill==153||mode!=10&&mode!=11);','if(mode!=12){boolean applied=accepted&&mode!=1&&mode!=10;')
t=t.replace('mode<13','mode<14')
t=t.replace('   var self=collect(wire);var peers=collect(other);','''   if(mode==13){
    var timed=(BattleTimedStatus)get(peer,"timedStatus");
    if(skill==173){
     NativeDeItemPublication.grant(peer,40010,3);var potion=peer.nativeDeInventory(0).items().stream().filter(i->i.definition().catalogId()==40010).findFirst().orElseThrow();int initial=(int)get(peer,"playerHp");long quantity=peer.nativeDeInventory(0).countCatalog(40010);
     var use=new Protocol.Writer().c(Protocol.C_USE_ITEM).d(potion.objectId());if(potion.definition().useType()==10)use.d(12345);else use.s("");call(peer,"handle",new Class[]{byte[].class},use.bytes());
     int restored=(int)get(peer,"playerHp")-initial;int[] range=InventoryCatalog.healingRange(40010);need(restored>=range[0]/2&&restored<=range[1]/2&&peer.nativeDeInventory(0).countCatalog(40010)==quantity-1,"real native healing potion follows active Pollute Water half-recovery and consumes once");
     need(timed.expirePolluteWater((long)get(timed,"polluteWaterExpiresAtNanos"))&&!active(peer,skill),"real Pollute Water deadline expires");
    }else{
     var field=CombatRules.class.getDeclaredField("current");field.setAccessible(true);var original=(CombatRules.Snapshot)field.get(null);var values=new HashMap<>(original.values());values.put("STRIKER_DMG2",2.75);
     try{field.set(null,copy(original,Map.of("values",values)));for(int type:new int[]{4,13,10,1,17})need(BattleStrikerGale.damageAfterStatus(101,active(peer,skill),BattleStrikerGale.playerWeaponEligible(type))==(type==1||type==17?101:277),"actual status feeds Source weapon categories and configured ranged multiplier; melee unchanged");
      need(timed.expireStrikerGale((long)get(timed,"strikerGaleExpiresAtNanos"))&&!active(peer,skill)&&BattleStrikerGale.damageAfterStatus(101,active(peer,skill),true)==101,"expired status stops configured ranged multiplier");
     }finally{field.set(null,original);}
    }
   }
   var self=collect(wire);var peers=collect(other);''')
(R/'TargetNegativeMaterialProbe.java').write_text(t,encoding='utf-8')
t=(N/'test.py').read_text('utf-8').replace('Negative','TargetNegative').replace("v[mode]['cases']==312","v[mode]['cases']==224").replace("(24 if mode=='before' else 0)","(16 if mode=='before' else 0)")
(R/'test.py').write_text(t,encoding='utf-8')
for name in ['Fault','Lifecycle']:
 t=(W/('Weakness'+name+'Probe.java')).read_text('utf-8').replace('Weakness','TargetNegative')
 if name=='Lifecycle':
  start=t.index('  call(pc,');end=t.index('\n',start);t=t[:start]+'  dispatch(pc,packet,row,selectedSkill,0);'+t[end:]
 (R/('TargetNegative'+name+'Probe.java')).write_text(t,encoding='utf-8')
t=(W/'test_boundaries.py').read_text('utf-8').replace('Weakness','TargetNegative')
(R/'test_boundaries.py').write_text(t,encoding='utf-8')
t=(N/'NegativeWorldNpcProbe.java').read_text('utf-8').replace('Negative','TargetNegative')
t=t.replace('return skill==153?hub.classicWorldMonsterEffectiveMagicResistance(map,instance,object,100,System.nanoTime())==25:hub.classicWorldMonsterSlowActive(map,instance,object,System.nanoTime());','return skill==173?hub.classicWorldMonsterPolluteWaterActive(map,instance,object,System.nanoTime()):hub.classicWorldMonsterStrikerGaleActive(map,instance,object,System.nanoTime());')
t=t.replace('selectedMode=mode==7?12:mode==1?1:0;cast(pc,skill,row);','selectedMode=mode==7?12:mode==1?1:0;probability(skill);cast(pc,skill,row);')
(R/'TargetNegativeWorldNpcProbe.java').write_text(t,encoding='utf-8')
t=(N/'test_world_npcs.py').read_text('utf-8').replace('Negative','TargetNegative').replace("v[mode]['cases']==96","v[mode]['cases']==64").replace("(12 if mode=='before' else 0)","(8 if mode=='before' else 0)")
(R/'test_world_npcs.py').write_text(t,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(R/'test-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in[N/'NegativeMaterialProbe.java',N/'NegativeWorldNpcProbe.java',W/'WeaknessFaultProbe.java',W/'WeaknessLifecycleProbe.java',Path('D:/lineage開發/1-伺服器/src/com/lineage/data/item_etcitem/hp/UserAddHp.java'),Path('D:/lineage開發/1-伺服器/src/com/lineage/server/model/L1AttackPc.java'),Path(__file__)]],scope='real native packet for mode12 retains original random effect outcome and proves resource/admission only; original typed enum root with deterministic roll for other cases, including actual potion half-recovery and Source weapon-category configured ranged helper consumer in mode13; complete native attack path remains separate; initial wrong weapon-category and impossible 100-percent fixture evidence retained'),indent=2),encoding='utf-8')

from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent
t=(R/'ArmorMaterialProbe.java').read_text('utf-8');main=t[t.index(' public static void main('):]
main=main.replace('mode<12','mode<8').replace('exercise(p.session,m.session,hub,world,mode);','exercise(p.session,m.session,f.session,o.session,hub,world,mode);')
main=main.replace('"armorBreakFixtureD100",mode==1?100:1','"armorBreakFixtureD100",1')
body='''package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Selector-only fallback and explicit companion rejection, with actual native packet dispatch. */
public final class ArmorTargetProbe extends ArmorMaterialProbe {
 static void exercise(ClientSession pc,ClientSession peer,ClientSession first,ClientSession other,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);var row=BattleSpellCatalog.armorBreakSpell(112);
  set(first,"observer",false);set(other,"observer",false);
  ClientSession extra=world?first:hub.canAttack((long)get(pc,"currentRoomId"),pc,first)?first:other;
  need(world||hub.canAttack((long)get(pc,"currentRoomId"),pc,extra),"second enemy available for ambiguity fixture");
  set(first,"observer",true);set(other,"observer",true);
  set(pc,"lastCombatTargetObjectId",mode==1?0x7feeeeeeL:mode==7?pc.playerObjectId():0L);
  if(mode==2||mode==3){set(extra,"observer",false);set(extra,"positionX",get(peer,"positionX"));set(extra,"positionY",get(peer,"positionY"));if(mode==3)set(pc,"lastCombatTargetObjectId",peer.playerObjectId());}
  if(mode==4||mode==5){
   var pet=BattleCompanion.lesserElemental(0x5f000003L,SourceSummonTemplate.memoryFixture(45306),6,600,(int)get(peer,"positionX"),(int)get(peer,"positionY"),System.nanoTime());
   ((List<BattleCompanion>)get(mode==4?pc:peer,"tamedCompanions")).add(pet);set(pc,"lastCombatTargetObjectId",pet.snapshot().objectId());
  }
  if(mode==6){
   set(peer,"observer",true);
   var type=Class.forName("local.lito381.server.ClientSession$ArmorBreakTarget");var constructor=type.getDeclaredConstructors()[0];constructor.setAccessible(true);
   var npc=constructor.newInstance(2L,(int)get(pc,"positionX")+1,(int)get(pc,"positionY"),1,null,null,null,null,true);
   need(CombatRules.loaded()&&CombatNpcDebuffs.armorTarget(npc,pc)==null,"Source NPC-only candidate refused by actual Armor Break filter");
  }
  int item=Math.toIntExact(row.itemConsumeId()),count=row.itemConsumeCount();NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var recipient=new SourceActionPairProbe.Wire(peer,pc)){
   boolean accepted=mode==0||mode==1||mode==3||mode==7;cast(pc,112,row);
   need((int)get(pc,"playerMp")==1000-(accepted?cost(pc,112,row):0)&&(int)get(pc,"playerHp")==4000-(accepted?row.hpCost():0),"selector target resource decision mode="+mode+" world="+world);
   need(pc.nativeDeInventory(0).countCatalog(item)==count+3-(accepted?count:0),"only eligible selector consumes materials");
   need(active(peer,hub,world)==accepted,"unique/explicit PC receives effect; companion/NPC/ambiguity rejected");
   need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(112)==accepted,"selector rejection creates no reuse");
   var self=collect(wire);var peers=collect(recipient);if(!accepted)need(self.isEmpty()&&peers.isEmpty(),"rejected selector emits no unrelated effect");
   goldens.add("world="+world+" mode="+mode+" self="+self+" peer="+peers);
   need(wire.locked+recipient.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"selector output unlocked; no receipt leak");
  }cases++;
 }
'''
(R/'ArmorTargetProbe.java').write_text(body+main,encoding='utf-8')
t=(R/'test.py').read_text('utf-8').replace("D=R/'build/tests'","D=R/'build/targets'").replace('ArmorMaterialProbe.java','ArmorTargetProbe.java').replace('server.ArmorMaterialProbe','server.ArmorTargetProbe').replace("v[mode]['cases']==96","v[mode]['cases']==64").replace("(8 if mode=='before' else 0)","0")
t=t.replace('deps=[classes,','deps=[classes,R/\'build/tests/classes\',')
(R/'test_targets.py').write_text(t,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(R/'target-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in[R/'ArmorMaterialProbe.java',R/'test.py',Path(__file__)]],scope='actual native112 packet; unique/unknown/self fallback, two-PC ambiguity, explicit PC, own/enemy companion rejection; direct actual NPC DTO filter; no all-NPC runtime claim'),indent=2),encoding='utf-8')

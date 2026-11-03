from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent
t=(R/'NegativeMaterialProbe.java').read_text('utf-8');main=t[t.index(' public static void main('):]
main=main.replace('world:new boolean[]{false,true}','world:new boolean[]{true}').replace('mode<13','mode<8').replace('selectedMode=mode;','selectedMode=0;')
body='''package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Real world population actor and original spell handler; isolated population replaces no production class. */
public final class NegativeWorldNpcProbe extends NegativeMaterialProbe {
 static ClassicWorldMonsterPopulation.Snapshot monster(ClientSession pc,GameHub hub)throws Exception{
  int map=(int)get(pc,"classicWorldMapId"),x=(int)get(pc,"positionX")+1,y=(int)get(pc,"positionY");
  var template=ClassicWorldMonsterCatalog.definitions(map).stream().map(ClassicWorldMonsterCatalog.SpawnDefinition::template).filter(t->!t.boss()&&SourceNpcPhysicalStats.memoryFixture(t.npcId()).eraseEligible()).findFirst().orElseThrow();
  var definition=new ClassicWorldMonsterCatalog.SpawnDefinition(map,1,"negative-fixture",1,0,x,y,0,0,0,0,0,0,0,0,0,false,30,false,0,template);
  var population=ClassicWorldMonsterPopulation.retainedSourceSlice(7,0x5e000000L,0x5e0000ffL,List.of(definition));set(hub,"classicWorldMonsters",population);
  var actors=population.inScreen(map,x,y);need(actors.size()==1&&actors.get(0).alive(),"one real isolated Source population actor");return actors.get(0);
 }
 static boolean active(GameHub hub,ClientSession pc,long object,int skill)throws Exception{
  int map=(int)get(pc,"classicWorldMapId"),instance=(int)get(pc,"classicWorldInstanceId");
  return skill==153?hub.classicWorldMonsterEffectiveMagicResistance(map,instance,object,100,System.nanoTime())==25:hub.classicWorldMonsterSlowActive(map,instance,object,System.nanoTime());
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,true);int skill=selectedSkill;Object row=spell(skill);probability(skill);var npc=monster(pc,hub);long object=npc.objectId();targetId=object;
  if(mode==2){var population=(ClassicWorldMonsterPopulation)get(hub,"classicWorldMonsters");population.damage((int)get(pc,"classicWorldMapId"),object,Integer.MAX_VALUE);need(!population.actor((int)get(pc,"classicWorldMapId"),object).alive(),"real NPC death transition");}
  if(mode==3)set(pc,"positionX",(int)get(pc,"positionX")-40);if(mode==6)targetId=0x7feeeeeeL;
  int item=Math.toIntExact((long)call(row,"itemConsumeId",new Class[]{})),count=(int)call(row,"itemConsumeCount",new Class[]{});if(mode!=4)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode==5){NativeCastMaterialProbe.cast(pc,2);long ready=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);while(System.nanoTime()-ready<=0)Thread.sleep(1);need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"real prior Light window");}
   int mp=(int)get(pc,"playerMp"),bytes=wire.raw.size();boolean accepted=mode<=1||mode==7||mode==5&&before;
   selectedMode=mode==7?12:mode==1?1:0;cast(pc,skill,row);
   need((int)get(pc,"playerMp")==Math.max(0,Math.min(1000,mp-(accepted?cost(pc,skill,row):0))),"world NPC Source MP mode="+mode+" skill="+skill);
   need(pc.nativeDeInventory(0).countCatalog(item)==(mode==4?0:count+3-(accepted?count:0)),"world NPC material once after admission");
   need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(skill)==accepted,"world NPC reuse");
   if(mode!=7)need(active(hub,pc,object,skill)==(accepted&&mode!=1),"actual population effect follows Source roll");
   if(mode==5){need((wire.raw.size()>bytes)==before,"NPC shared Source window");if(before)gaps++;}
   var self=collect(wire);var peers=collect(other);
   if(mode==4)need(self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&peers.isEmpty(),"world NPC missing material299");
   if(mode<=1)goldens.add("skill="+skill+" mode="+mode+" npc="+npc.npcId()+" mp="+get(pc,"playerMp")+" self="+self+" peer="+peers);
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"world NPC output unlocked and receipt drained");
  }cases++;
 }
'''
(R/'NegativeWorldNpcProbe.java').write_text(body+main,encoding='utf-8')
t=(R/'test.py').read_text('utf-8').replace("D=R/'build/tests'","D=R/'build/world-npcs'")
t=t.replace('NegativeMaterialProbe.java','NegativeWorldNpcProbe.java').replace('server.NegativeMaterialProbe','server.NegativeWorldNpcProbe').replace("v[mode]['cases']==312","v[mode]['cases']==96").replace("(24 if mode=='before' else 0)","(12 if mode=='before' else 0)")
t=t.replace('deps=[classes,','deps=[classes,R/\'build/tests/classes\',')
(R/'test_world_npcs.py').write_text(t,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(R/'world-npc-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in[R/'NegativeMaterialProbe.java',R/'test.py',Path(__file__)]],scope='actual world population, success/failure/dead/range/material/window/unknown/native packet; isolated original Source template; not Quest/Time-Attack or all-NPC lifetime claim'),indent=2),encoding='utf-8')

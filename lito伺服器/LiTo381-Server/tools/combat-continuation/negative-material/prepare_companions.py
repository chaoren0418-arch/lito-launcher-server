from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent
t=(R/'NegativeMaterialProbe.java').read_text('utf-8');main=t[t.index(' public static void main('):]
main=main.replace('mode<13','mode<8').replace('selectedMode=mode;','selectedMode=0;')
body='''package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Real constructed source elemental actor; retained handler resolves its owner in both game phases. */
public final class NegativeCompanionProbe extends NegativeMaterialProbe {
 static BattleCompanion companion(ClientSession owner,int x,int y)throws Exception {
  var pet=BattleCompanion.lesserElemental(0x5f000001L,SourceSummonTemplate.memoryFixture(45306),6,600,x,y,System.nanoTime());
  pet.qualifyNpcDefense(SourceNpcPhysicalStats.memoryFixture(45306));pet.cancelHaste();
  ((List<BattleCompanion>)get(owner,"tamedCompanions")).add(pet);return pet;
 }
 static boolean active(BattleCompanion pet,int skill)throws Exception{return skill==153?((BattleEraseMagicStatus)get(pet,"eraseMagicStatus")).active(System.nanoTime()):pet.snapshot().moveSpeed()==BattleTimedStatus.SLOW;}
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);int skill=selectedSkill;Object row=spell(skill);probability(skill);
  ClientSession owner=mode==0?pc:peer;var pet=companion(owner,(int)get(pc,"positionX")+1,(int)get(pc,"positionY"));targetId=pet.snapshot().objectId();
  if(mode==2)pet.applyDamage(Integer.MAX_VALUE);if(mode==3)pet.deactivate();if(mode==4)pet.relocate((int)get(pc,"positionX")+40,(int)get(pc,"positionY"));
  int item=Math.toIntExact((long)call(row,"itemConsumeId",new Class[]{})),count=(int)call(row,"itemConsumeCount",new Class[]{});if(mode!=5)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode==6){NativeCastMaterialProbe.cast(pc,2);long ready=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);while(System.nanoTime()-ready<=0)Thread.sleep(1);need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"real prior Light window");}
   int mp=(int)get(pc,"playerMp"),bytes=wire.raw.size();boolean accepted=mode<=1||mode==7||mode==6&&before;
   selectedMode=mode==7?12:0;cast(pc,skill,row);
   need((int)get(pc,"playerMp")==Math.max(0,Math.min(1000,mp-(accepted?cost(pc,skill,row):0))),"companion Source MP mode="+mode+" skill="+skill+" world="+world);
   need(pc.nativeDeInventory(0).countCatalog(item)==(mode==5?0:count+3-(accepted?count:0)),"companion material consumed once after admission");
   need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(skill)==accepted,"companion cast reuse");
   if(mode!=7)need(active(pet,skill)==accepted,"actual companion receives accepted negative state");
   need(!active(pc,skill)&&!active(peer,skill),"neither caster nor owner receives companion effect");
   if(mode==6){need((wire.raw.size()>bytes)==before,"companion shared Source window");if(before)gaps++;}
   var self=collect(wire);var peers=collect(other);
   if(mode==5)need(self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&peers.isEmpty(),"companion missing material299");
   if(mode<=1)goldens.add("skill="+skill+" world="+world+" own="+(mode==0)+" mp="+get(pc,"playerMp")+" self="+self+" peer="+peers);
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"companion output unlocked and cast receipt drained");
  }cases++;
 }
'''
(R/'NegativeCompanionProbe.java').write_text(body+main,encoding='utf-8')
t=(R/'test.py').read_text('utf-8').replace("D=R/'build/tests'","D=R/'build/companions'")
t=t.replace('NegativeMaterialProbe.java','NegativeCompanionProbe.java').replace('server.NegativeMaterialProbe','server.NegativeCompanionProbe').replace("v[mode]['cases']==312","v[mode]['cases']==192")
t=t.replace('deps=[classes,','deps=[classes,R/\'build/tests/classes\',')
(R/'test_companions.py').write_text(t,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(R/'companion-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in[R/'NegativeMaterialProbe.java',R/'test.py',Path(__file__)]],scope='actual constructed npc45306 elemental, own/enemy, dead/inactive/range/missing material/Source action window/native packet; no all-NPC lifetime claim'),indent=2),encoding='utf-8')

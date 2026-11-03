"""Native cast and answer packets, including the non-material resurrection61 control."""
from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent;D=R.parent/'decay-material';sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
t=(D/'DecayPotionMaterialProbe.java').read_text('utf-8').replace('DecayPotion','Resurrection').replace('decayPotionSpell','resurrectionSpell').replace('SKILLS={71}','SKILLS={61,75}').replace('Set.of(2,71)','Set.of(2,61,75)')
t=t.replace('.h((int)get(pc,"positionX")+1).h((int)get(pc,"positionY"))','')
t=t.replace('return ((BattleTimedStatus)get(pc,"timedStatus")).decayPotionActive(System.nanoTime());','return get(pc,"pendingResurrection")!=null;')
t=t.replace('targetId=peer.playerObjectId();','targetId=peer.playerObjectId();set(peer,"playerHp",0);set(peer,"pendingResurrection",null);set(pc,"pendingResurrection",null);')
a=t.index(' static void exercise(');b=t.index(' public static void main',a)
t=t[:a]+''' static void answer(ClientSession peer,int question,int accepted)throws Exception{
  call(peer,"handle",new Class[]{byte[].class},new Protocol.Writer().c(Protocol.C_ANSWER).h(question).c(accepted).s("").bytes());
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);int skill=selectedSkill;Object row=spell(skill);
  if(mode==1)set(peer,"playerHp",1);if(mode==3)set(pc,"playerMp",0);if(mode==5)targetId=0x7feeeeeeL;
  if(mode==6)set(peer,"observer",true);if(mode==7)set(peer,"positionX",(int)get(pc,"positionX")+40);if(mode==8)set(pc,"observer",true);if(mode==9)targetId=pc.playerObjectId();
  if(mode==10)((BattleTimedStatus)get(peer,"timedStatus")).activateCounterMagic(System.nanoTime(),60);
  if(mode==11)((BattleTimedStatus)get(peer,"timedStatus")).activateHaste(System.nanoTime(),60,43);
  int item=Math.toIntExact((long)call(row,"itemConsumeId",new Class[]{})),count=(int)call(row,"itemConsumeCount",new Class[]{});if(item>0&&mode!=2)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode==4){NativeCastMaterialProbe.cast(pc,2);long ready=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);while(System.nanoTime()-ready<=0)Thread.sleep(1);need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"genuine prior Light window");}
   int mp=(int)get(pc,"playerMp"),bytes=wire.raw.size();boolean accepted=mode==0||mode==2&&item==0||mode>=10||mode==4&&before;
   cast(pc,skill,row);
   need((int)get(pc,"playerMp")==Math.max(0,Math.min(1000,mp-(accepted?cost(pc,skill,row):0))),"resurrection Source MP mode="+mode+" skill="+skill+" world="+world);
   if(item>0)need(pc.nativeDeInventory(0).countCatalog(item)==(mode==2?0:count+3-(accepted?count:0)),"resurrection material once");
   need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(skill)==accepted,"resurrection reuse follows admission");
   need(active(peer,skill)==accepted&&!active(pc,skill),"only accepted cast creates original target question");
   if(mode==4){need((wire.raw.size()>bytes)==before,"shared Source time window");if(before)gaps++;}
   if(mode>=12){
    int question=(int)call(row,"questionId",new Class[]{}),result=mode==12?0:1;
    answer(peer,mode==14?(question==321?322:321):question,result);
    int expected=mode==13||mode==15?(skill==75?5000:2500):0;
    need((int)get(peer,"playerHp")==expected&&!active(peer,skill),"native reply acceptance/decline and Source HP half/full");
    if(mode==15){int peerBytes=other.raw.size();answer(peer,question,1);need((int)get(peer,"playerHp")==expected&&other.raw.size()==peerBytes,"duplicate answer has no second revival");}
    need((int)get(pc,"playerMp")==Math.max(0,Math.min(1000,mp-cost(pc,skill,row))),"answers do not repeat caster resource debit");
   }
   var self=collect(wire);var peers=collect(other);
   if(mode==2&&item>0)need(self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&peers.isEmpty(),"missing reagent299 exactly once");
   if(accepted)need(peers.contains(HexFormat.of().formatHex(skill==75?Protocol.greaterResurrectionAsk():Protocol.resurrectionAsk())),"real native question321/322 packet");
   if(mode==0||mode>=10)goldens.add("skill="+skill+" world="+world+" mode="+mode+" targetHp="+get(peer,"playerHp")+" mp="+get(pc,"playerMp")+" self="+self+" peer="+peers);
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&SourceActionPairProbe.state(pc).queue.isEmpty(),"unlocked output and drained receipts");
  }cases++;
 }
'''+t[b:]
t=t.replace('mode<12','mode<16')
(R/'ResurrectionMaterialProbe.java').write_text(t,encoding='utf-8')
t=(D/'test.py').read_text('utf-8').replace('DecayPotion','Resurrection').replace("v[mode]['cases']==96","v[mode]['cases']==256").replace("(8 if mode=='before' else 0)","(16 if mode=='before' else 0)")
(R/'test.py').write_text(t,encoding='utf-8')
for name in ['Fault','Lifecycle']:
 t=(D/('DecayPotion'+name+'Probe.java')).read_text('utf-8').replace('DecayPotion','Resurrection')
 t=t.replace('extends ResurrectionMaterialProbe {','extends ResurrectionMaterialProbe {\n static final int[] SKILLS={75};')
 if name=='Lifecycle':
  t=t.replace('.h((int)get(target,"positionX")).h((int)get(target,"positionY"))','')
  a=t.index('   }else{\n    set(peer,"learnedSkills"');b=t.index('   collect(wire);collect(other);',a)
  t=t[:a]+'   }\n'+t[b:]
  t=t.replace('mode<7','mode<6')
 (R/('Resurrection'+name+'Probe.java')).write_text(t,encoding='utf-8')
t=(D/'test_boundaries.py').read_text('utf-8').replace('DecayPotion','Resurrection').replace("('Lifecycle','after',jar,56)","('Lifecycle','after',jar,48)")
(R/'test_boundaries.py').write_text(t,encoding='utf-8')
source=Path('D:/lineage開發/1-伺服器/src/com/lineage/server/clientpackets/C_Attr.java')
(R/'test-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in[D/'DecayPotionMaterialProbe.java',D/'test.py',D/'DecayPotionFaultProbe.java',D/'DecayPotionLifecycleProbe.java',D/'test_boundaries.py',source,Path(__file__)]],scope='256 native player cast/reply cases; 61 non-material control,75 material; 8 material faults/48 lifetime cases for75; no impossible simultaneous casting by two dead players; NPC/companion and stale answer lifecycle not yet qualified'),indent=2),encoding='utf-8')

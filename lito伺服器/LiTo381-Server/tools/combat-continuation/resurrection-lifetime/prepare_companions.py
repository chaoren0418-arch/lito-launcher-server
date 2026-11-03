from pathlib import Path
R=Path(__file__).resolve().parent
t=(R/'ResurrectionWorldNpcProbe.java').read_text('utf-8')
n=(R.parent/'negative-material/NegativeCompanionProbe.java').read_text('utf-8')
helper=n[n.index(' static BattleCompanion companion('):n.index(' static boolean active(')]
helper=helper.replace('static BattleCompanion companion(', 'static BattleCompanion summoned(')
helper+=''' static BattleCompanion companion(ClientSession owner,int x,int y)throws Exception {
  var state=new GameStateStore.PetState("fixture-pet",0x70001652L,0x90001652L,(int)InventoryCatalog.PET_COLLAR,45034,"ResurrectionPet",12,78,17,21375,0);
  var pet=BattleCompanion.pet(state,SourcePetTemplate.memoryFixture(45034),x,y);
  ((List<BattleCompanion>)get(owner,"tamedCompanions")).add(pet);return pet;
 }
'''
start=t.index(' static ClassicWorldMonsterPopulation.Snapshot monster(');end=t.index(' public static void main',start)
body=r''' static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,true);set(peer,"playerHp",4000);int skill=selectedSkill;Object row=spell(skill);
  ClientSession owner=mode==0?pc:peer;int x=(int)get(pc,"positionX")+2,y=(int)get(pc,"positionY")+2;var pet=mode==8?summoned(owner,x,y):companion(owner,x,y);var original=pet.snapshot();targetId=original.objectId();
  if(mode!=2){pet.applyDamage(Integer.MAX_VALUE);need(pet.snapshot().hp()==0,"actual companion corpse before resurrection");}
  if(mode==3)pet.deactivate();if(mode==6)targetId=0x7feeeeeeL;if(mode==7)pet.relocate((int)get(pc,"positionX")+40,(int)get(pc,"positionY"));if(mode==9){set(peer,"positionX",x);set(peer,"positionY",y);}
  int item=Math.toIntExact((long)call(row,"itemConsumeId",new Class[]{})),count=(int)call(row,"itemConsumeCount",new Class[]{});if(item>0&&mode!=4)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode==5){NativeCastMaterialProbe.cast(pc,2);long ready=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);while(System.nanoTime()-ready<=0)Thread.sleep(1);need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"real prior Light window");}
   int mp=(int)get(pc,"playerMp"),bytes=wire.raw.size();boolean accepted=mode<=1||mode==4&&item==0;
   cast(pc,skill,row);
   need((int)get(pc,"playerMp")==Math.max(0,Math.min(1000,mp-(accepted?cost(pc,skill,row):0))),"companion resurrection MP mode="+mode+" skill="+skill);
   if(item>0)need(pc.nativeDeInventory(0).countCatalog(item)==(mode==4?0:count+3-(accepted?count:0)),"companion resurrection material exactly once");
   need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(skill)==accepted,"companion reuse follows admission");
   var after=pet.snapshot();need((after.hp()>0)==(accepted||mode==2),"actual companion revived or unchanged live companion");
   if(accepted)need(after.hp()==(int)call(row,"restoredNpcHp",new Class[]{int.class},original.maxHp()),"restored companion HP rule");
   need(get(pc,"pendingResurrection")==null&&get(peer,"pendingResurrection")==null&&get(pc,"nativeResurrectionAnswerLease")==null&&get(peer,"nativeResurrectionAnswerLease")==null,"companion does not install player confirmation lease");
   if(mode==5)need(wire.raw.size()==bytes,"companion shared Source action window");
   var self=collect(wire);var peers=collect(other);
   if(mode==4&&item>0)need(self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&peers.isEmpty(),"companion missing material299 exactly once");
   if(mode==9)need(self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(592))))&&peers.isEmpty(),"occupied pet corpse keeps original592 rejection");
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"companion outputs unlocked and receipt drained");
  }cases++;
 }
'''
t=t[:start]+helper+body+t[end:]
t=t.replace('ResurrectionWorldNpcProbe','ResurrectionCompanionProbe').replace('/** Real world population actor and original spell handler; isolated population replaces no production class. */','/** Actual own/other companion corpse, native resurrection packet and original filters. */')
t=t.replace('mode<8','mode<10')
(R/'ResurrectionCompanionProbe.java').write_text(t,encoding='utf-8')
d=(R/'test_npc.py').read_text('utf-8').replace('build/world-npc','build/companions').replace('ResurrectionWorldNpcProbe','ResurrectionCompanionProbe')
d=d.replace("v[mode]['cases']==64","v[mode]['cases']==80")
(R/'test_companions.py').write_text(d,encoding='utf-8')

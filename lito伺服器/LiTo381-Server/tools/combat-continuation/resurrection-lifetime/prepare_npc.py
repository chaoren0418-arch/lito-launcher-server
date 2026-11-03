from pathlib import Path
R=Path(__file__).resolve().parent
t=(R.parent/'blind-material/BlindWorldNpcProbe.java').read_text('utf-8')
t=t.replace('BlindWorldNpcProbe','ResurrectionWorldNpcProbe').replace('extends BlindMaterialProbe','extends ResurrectionMaterialProbe')
t=t.replace('SourceNpcPhysicalStats.memoryFixture(t.npcId()).eraseEligible()', 'ClassicWorldResurrectionCatalog.allowsSelectedTemplate(t.npcId(),true)')
start=t.index(' static boolean active(');end=t.index(' public static void main',start)
body=r''' static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,true);int skill=selectedSkill;Object row=spell(skill);var npc=monster(pc,hub);long object=npc.objectId();targetId=object;
  int map=(int)get(pc,"classicWorldMapId");var population=(ClassicWorldMonsterPopulation)get(hub,"classicWorldMonsters");
  if(mode!=1){population.damage(map,object,Integer.MAX_VALUE);need(!population.actor(map,object).alive(),"real NPC corpse before resurrection");}
  if(mode==2)set(pc,"playerMp",0);if(mode==3)set(pc,"positionX",(int)get(pc,"positionX")-40);if(mode==6)targetId=0x7feeeeeeL;if(mode==7)set(pc,"observer",true);
  int item=Math.toIntExact((long)call(row,"itemConsumeId",new Class[]{})),count=(int)call(row,"itemConsumeCount",new Class[]{});if(item>0&&mode!=4)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode==5){NativeCastMaterialProbe.cast(pc,2);long ready=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);while(System.nanoTime()-ready<=0)Thread.sleep(1);need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"real prior Light window");}
   int mp=(int)get(pc,"playerMp"),bytes=wire.raw.size();boolean accepted=mode==0||mode==4&&item==0;
   cast(pc,skill,row);
   need((int)get(pc,"playerMp")==Math.max(0,Math.min(1000,mp-(accepted?cost(pc,skill,row):0))),"world NPC resurrection Source MP mode="+mode+" skill="+skill);
   if(item>0)need(pc.nativeDeInventory(0).countCatalog(item)==(mode==4?0:count+3-(accepted?count:0)),"NPC resurrection material debit once");
   need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(skill)==accepted,"NPC resurrection reuse follows admission");
   var after=population.actor(map,object);need(after.alive()==(accepted||mode==1),"actual population resurrection or unchanged live NPC");
   if(accepted)need(after.currentHp()==(int)call(row,"restoredNpcHp",new Class[]{int.class},npc.maximumHp()),"Source NPC restored HP rule");
   need(get(pc,"pendingResurrection")==null&&get(peer,"pendingResurrection")==null&&get(pc,"nativeResurrectionAnswerLease")==null,"NPC resurrection creates no player question or lease");
   if(mode==5)need(wire.raw.size()==bytes,"NPC shared Source action window");
   var self=collect(wire);var peers=collect(other);
   if(mode==4&&item>0)need(self.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&peers.isEmpty(),"NPC missing material299 exactly once");
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"NPC outputs unlocked and receipt drained");
  }cases++;
 }
'''
t=t[:start]+body+t[end:]
t=t.replace('"darkBlindFixtureRoll"','"decayPotionFixturePercentileRoll"')
(R/'ResurrectionWorldNpcProbe.java').write_text(t,encoding='utf-8')
d=(R/'test_answers.py').read_text('utf-8').replace('build/answers','build/world-npc').replace('ResurrectionAnswerLifetimeProbe','ResurrectionWorldNpcProbe')
d=d.replace("[('before',b['base']),('after',b['runtime'])]","[('after',b['runtime'])]")
d=d.replace(" and v[mode]['stale_native_acceptances']==v[mode]['stale_direct_acceptances']==(16 if mode=='before' else 0)", " and v[mode]['Source_window_bypasses']==0")
(R/'test_npc.py').write_text(d,encoding='utf-8')

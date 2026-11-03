package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
public class CancellationBattleFaultProbe extends CancellationMaterialProbe {
 static boolean armed;static int faults;static ClientSession caster;static Object oldActor;static GameStateStore store;static int reagent,amount;
 public static void beforeSave(){}
 public static void beforeFlush()throws Exception{if(!armed)return;armed=false;faults++;
  need((int)get(caster,"playerMp")==1000&&caster.nativeDeInventory(0).countCatalog(reagent)==amount+3,"no canonical debit before actual material save");
  need((boolean)call(oldActor,"hasteActive",new Class[]{long.class},System.nanoTime()),"NPC unchanged before material save");
  throw new java.io.IOException("battle replacement material fixture failure");
 }
 static final class Clock extends ScheduledThreadPoolExecutor {
  Runnable tick;ScheduledFuture<?> scheduled;final boolean real;
  Clock(boolean real){super(1);this.real=real;setRemoveOnCancelPolicy(true);}
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,long initial,long delay,TimeUnit unit){need(initial==1&&delay==1&&unit==TimeUnit.SECONDS,"bounded one-second cleanup");tick=command;scheduled=super.scheduleWithFixedDelay(real?command:()->{},real?initial:1,real?delay:1,real?unit:TimeUnit.DAYS);return scheduled;}
  void fire(){if(tick!=null)tick.run();}
 }
 static Object stat(Class<?> type,String field)throws Exception{var f=type.getDeclaredField(field);f.setAccessible(true);return f.get(null);}
 static Object create(boolean time,NativeDeNpcTemplate n,int force,int x,int y)throws Exception {
  Object template=time?new BattleTimeAttackEncounter.Template(n.get_npcId(),n.get_gfxid(),n.get_nameid(),n.get_level(),n.get_hp(),n.get_mp(),n.get_int()):new BattleQuestEncounter.Template(n.get_npcId(),n.get_gfxid(),n.get_nameid(),n.get_level(),n.get_hp(),n.get_mp(),n.get_int());
  Class<?> type=Class.forName("local.lito381.server."+(time?"BattleTimeAttackEncounter":"BattleQuestEncounter")+"$Monster");
  var ctor=time?type.getDeclaredConstructor(BattleTimeAttackEncounter.Template.class,int.class,int.class,String.class,String.class):type.getDeclaredConstructor(int.class,int.class,BattleQuestEncounter.Template.class,int.class,int.class,boolean.class,String.class);ctor.setAccessible(true);
  return time?ctor.newInstance(template,x,y,"Source template fixture","fixture-wave"):ctor.newInstance(force,1,template,x,y,false,"Source template fixture");
 }
 @SuppressWarnings("unchecked") static void exercise(ClientSession pc,ClientSession peer,GameHub hub,GameStateStore unused,boolean world,int skill,int mode,int kind)throws Exception {
  boolean time=kind!=0;setup(pc,peer,hub,false);set(pc,"learnedSkills",new HashSet<>(Set.of(2,44)));
  long roomId=(long)get(pc,"currentRoomId");var room=((Map<?,?>)get(hub,"rooms")).get(roomId);var match=(BattleRules.MatchState)get(room,"match");var originalRules=match.rules();var battleMode=time?BattleRules.Mode.TIME_ATTACK:BattleRules.Mode.QUEST_BATTLE;int map=battleMode.maps().get(0);var rules=copy(originalRules,Map.of("mode",battleMode,"mapId",map,"mapPool",battleMode.maps()));set(match,"rules",rules);set(room,"selectedMapId",map);var arena=ArenaMapCatalog.forMap(map);Object encounter=get(room,time?"timeAttackEncounter":"questEncounter");
  if(time){((BattleTimeAttackEncounter)encounter).reset(rules,arena);((BattleTimeAttackEncounter)encounter).startAfterTowerDestroyed();}else ((BattleQuestEncounter)encounter).reset(rules,arena);
  List<?> snapshots=time?hub.visibleTimeAttackActors(roomId,pc):hub.visibleQuestMonsters(roomId,pc);var first=snapshots.get(0);int x=(int)call(first,"x",new Class[]{}),y=(int)call(first,"y",new Class[]{}),force=time?0:(int)call(first,"force",new Class[]{});
  Object oldVisible=CombatAccess.call(encounter,"monster","(J)Llocal/lito381/server/"+(time?"BattleTimeAttackEncounter":"BattleQuestEncounter")+"$Monster;",(long)call(first,"objectId",new Class[]{}));
  List<Object> actors;if(time)actors=(List<Object>)get(encounter,"actors");else {actors=null;for(var lane:(Object[])get(encounter,"lanes")){var list=(List<Object>)get(lane,"monsters");if(list.contains(oldVisible)){actors=list;break;}}}need(actors!=null,"canonical encounter actor storage");actors.clear();
  var source=NativeDeNpcTemplateResources.snapshot().npcs().get(91123);var target=NativeDeNpcTemplateResources.snapshot().npcs().get(45641);var actor=create(time,source,force,x,y);actors.add(actor);long id=(long)get(actor,"objectId");targetId=id;
  if(kind==2){set(match,"rules",originalRules);set(encounter,"phase",Enum.valueOf((Class<Enum>)Class.forName("local.lito381.server.BattleTimeAttackEncounter$Phase"),"FINAL_BOSSES"));((Map<Object,Object>)stat(LiToGmNpcs.class,"GM")).put(encounter,originalRules);need(LiToGmNpcs.active((BattleTimeAttackEncounter)encounter),"GM actor route with ordinary battle rules");}
  for(var member:hub.roomMembers(roomId)){set(member,"positionX",x+1);set(member,"positionY",y);}
  need((boolean)call(actor,"activateHaste",new Class[]{long.class,int.class},System.nanoTime(),60),"old actor has an actual effect");
  var row=BattleSpellCatalog.cancellationSpell(44);int item=(int)row.itemConsumeId(),count=row.itemConsumeCount();if(mode!=1)NativeDeItemPublication.grant(pc,item,count+3);
  var priorRules=(NativeCancellationNpcRules.Snapshot)stat(NativeCancellationNpcRules.class,"current");String classname=target.get_classname();
  if(mode==5||mode==9||mode==18){var rows=new HashMap<>(priorRules.rows());rows.put(91123,new NativeCancellationNpcRules.Rule(91123,2,mode==5?999999:45641,mode==9?0:mode==18?1:300,"",0));NativeCancellationNpcRules.publish(new NativeCancellationNpcRules.Snapshot(priorRules.table(),rows,priorRules.invalid()));}
  var clock=new Clock(mode==18);set(hub,"battleClock",clock);
  if(mode==2)set(pc,"playerMp",0);if(mode==3)set(pc,"positionX",x-40);if(mode==4){if(time)((BattleTimeAttackEncounter)encounter).damage(id,Integer.MAX_VALUE);else ((BattleQuestEncounter)encounter).damage(id,force,Integer.MAX_VALUE);}if(mode==6)target.set_classname("unqualified.fixture.Executor");if(mode==7)clock.shutdownNow();
  try(var self=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode==8){NativeCastMaterialProbe.cast(pc,2);long ready=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);while(System.nanoTime()-ready<=0)Thread.sleep(1);need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"shared prior action window");}
   boolean accepted=mode==0||mode>=5&&mode!=8,converted=mode==0||mode>=9;int mp=(int)get(pc,"playerMp");
   if(mode==16||mode==17)NativeSourcePlayerActionLock.run(pc,()->{try{cast(pc,44,row);var retiring=mode==17?pc:peer;NativePlayerCommit.selected(retiring,retiring.selectedCharacterName());}catch(Exception e){throw new RuntimeException(e);}});
   else if(mode==20){
    set(peer,"learnedSkills",new HashSet<>(Set.of(44)));NativeDeItemPublication.grant(peer,item,count+3);
    var gate=new CountDownLatch(1);var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();var threads=new ArrayList<Thread>();
    for(var caster:List.of(pc,peer)){var worker=new Thread(()->{try{gate.await();cast(caster,44,row);}catch(Throwable e){failure.set(e);}});threads.add(worker);worker.start();}
    gate.countDown();for(var worker:threads){worker.join(3000);need(!worker.isAlive(),"concurrent replacement terminates");}if(failure.get()!=null)throw new AssertionError(failure.get());
    accepted=(int)get(pc,"playerMp")!=mp;need(pc.nativeDeInventory(0).countCatalog(item)+peer.nativeDeInventory(0).countCatalog(item)==count+6,"two casters pay only once");
   }else {
    caster=pc;oldActor=actor;store=unused;reagent=item;amount=count;
    var inventory=pc.nativeDeInventory(0);String key=(String)call(pc,"stateCharacter",new Class[]{});var ledger=store.economy(key);
    armed=true;try{cast(pc,44,row);}finally{armed=false;}
    need(pc.nativeDeInventory(0)==inventory&&pc.nativeDeInventory(0).countCatalog(item)==count+3&&(int)get(pc,"playerMp")==mp,"failed memory save preserves inventory identity, count and MP");
    need(store.economy(key).equals(ledger)&&actors.contains(actor)&&(int)get(actor,"hp")>0,"memory ledger and old actor retained");
    need(hub.nativeCancellationBattleRuntime==null||hub.nativeCancellationBattleRuntime.leases.isEmpty(),"failed material produces no replacement or lease");
    need(collect(self).equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&collect(other).isEmpty(),"exact299 with no peer effect");
    need(pc.nativeCastTimelineState().timeline.canReserveSkillAction()&&!pc.nativeCastTimelineState().reuseActor.isSkillDelay(),"failed material rolls back provisional action/reuse");
    cast(pc,44,row);
   }
   need((int)get(pc,"playerMp")==mp-(accepted?cost(pc,44,row):0),"battle replacement MP kind="+kind+" mode="+mode);
   need(pc.nativeDeInventory(0).countCatalog(item)==(mode==1?0:count+3-(accepted?count:0)),"replacement material once");
   var state=hub.nativeCancellationBattleRuntime;Object replacement=actors.stream().filter(a->(int)CombatAccess.call(CombatAccess.call(a,"snapshot","()Llocal/lito381/server/"+(time?"BattleTimeAttackEncounter":"BattleQuestEncounter")+"$Snapshot;"),"npcId","()I")==45641).findFirst().orElse(null);
   need((replacement!=null)==converted,"actual native replacement kind="+kind+" mode="+mode);
   if(converted){
    long newId=(long)get(replacement,"objectId");var snapshot=time?hub.timeAttackActor(roomId,newId):hub.questMonster(roomId,newId);need(snapshot!=null&&(int)call(snapshot,"npcId",new Class[]{})==45641,"replacement Source identity visible through hub");need((int)get(replacement,"x")==x&&(int)get(replacement,"y")==y,"replacement preserves tile");
    need(!actors.contains(actor)&&(int)get(actor,"hp")==0&&state.leases.size()==1,"old actor retired with exactly one lease");
    need(((List<?>)get(encounter,"corpses")).isEmpty(),"replacement never creates a kill corpse");
    if(time)need(get(replacement,"finalBoss").equals(get(actor,"finalBoss")),"objective role preserved");else need(get(replacement,"force").equals(force)&&get(replacement,"roomNumber").equals(get(actor,"roomNumber")),"quest lane/room preserved");
    if(mode==9){need(state.leases.get(0).deadline==0,"zero means unlimited");clock.fire();need(actors.contains(replacement),"unlimited actor not expired");}
    if(mode==10){set(state.leases.get(0),"deadline",System.nanoTime()-1);clock.fire();need(!actors.contains(replacement)&&state.leases.isEmpty()&&clock.scheduled.isCancelled(),"expiry removes actor and cancels timer");}
    if(mode==11){if(time)((BattleTimeAttackEncounter)encounter).damage(newId,Integer.MAX_VALUE);else ((BattleQuestEncounter)encounter).damage(newId,force,Integer.MAX_VALUE);clock.fire();need(state.leases.isEmpty()&&!( (List<?>)get(encounter,"corpses")).isEmpty(),"death keeps existing corpse while retiring lease");}
    if(mode==12){if(time)((BattleTimeAttackEncounter)encounter).reset(rules,arena);else ((BattleQuestEncounter)encounter).reset(rules,arena);clock.fire();need(state.leases.isEmpty()&&clock.scheduled.isCancelled(),"encounter reset removes stale lease");}
    if(mode==13){((Map<?,?>)get(hub,"rooms")).remove(roomId);clock.fire();need(state.leases.isEmpty()&&!actors.contains(replacement),"room removal retires runtime actor");}
    if(mode==14){set(room,"battleStarted",false);clock.fire();need(state.leases.isEmpty()&&!actors.contains(replacement),"battle end releases runtime actor");}
    if(mode==15){hub.close();need(state.closed&&state.leases.isEmpty()&&state.pending.isEmpty()&&!actors.contains(replacement)&&clock.scheduled.isCancelled(),"hub close removes runtime actor and queue");NativeCancellationBattleRuntime.close(hub);clock.fire();}
    if(mode==17){clock.fire();need(state.pending.isEmpty(),"caster retirement does not discard peer output");}
    if(mode==18){long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);while(System.nanoTime()<until){synchronized(hub){if(state.leases.isEmpty())break;}Thread.sleep(10);}synchronized(hub){need(state.leases.isEmpty()&&!actors.contains(replacement)&&clock.scheduled.isCancelled(),"real scheduled expiry");}}
    if(mode==19){Object corpse=time?hub.timeAttackCorpse(roomId,pc,id,System.nanoTime()):hub.questCorpse(roomId,pc,id,System.nanoTime());need(corpse==null,"deleted original cannot be resurrected");}
    var a=collect(self);var b=collect(other);String removed=HexFormat.of().formatHex(Protocol.removeObject(id));need(a.contains(removed)==(mode!=17),"old remove packet guarded by caster role");boolean peerEligible=time||hub.battleRecipients(roomId,force).contains(peer);if(mode!=13&&mode!=15)need(b.contains(removed)==(mode!=16&&peerEligible),"peer output respects lane and retirement mode="+mode);
   }else if(accepted){need((boolean)call(actor,"alive",new Class[]{})&&!(boolean)call(actor,"hasteActive",new Class[]{long.class},System.nanoTime()),"unsupported spawn keeps original and ordinary cancellation");need(state==null||state.leases.isEmpty(),"fallback leaves no lease");}
   var a=collect(self);var b=collect(other);if(mode==1)need(a.equals(List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&b.isEmpty(),"missing material visible299 only");
   need(self.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&NativeAreaMaterialAction.current.get()==null&&NativeCancellationBattleAction.current.get()==null,"no locked output or leaked scopes");
  }finally{NativeCancellationNpcRules.publish(priorRules);target.set_classname(classname);clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"clock joined");if(kind==2)((Map<?,?>)stat(LiToGmNpcs.class,"GM")).remove(encounter);}cases++;
 }
 static Object getUnchecked(Object target,String name){return CombatAccess.field(target,name);}
 public static void main(String[]args)throws Exception{before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();for(var type:List.of(ClientSession.class,RuntimeInventory.class,NativeCastMaterial.class,NativeDeItemTransaction.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual native handlers and material authority from requested JAR");var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false})for(boolean app:new boolean[]{false,true})for(int skill:new int[]{44})for(int kind=0;kind<3;kind++)for(int mode=0;mode<1;mode++){var r=root.resolve(flag+"-"+world+"-"+app+"-"+skill+"-"+kind+"-"+mode);Files.createDirectories(r);var cfg=copy(base,Map.of("sleepFixturePercentileRoll",mode==7?100:1,"massSlowFixturePercentileRoll",mode==7?100:1,"massSlowFixtureDieRoll",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);exercise(p.session,m.session,hub,state,world,skill,mode,kind);}need(!Files.exists(r.resolve("must-not-write.properties")),"both flags remain memory-only");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}need(faults==12&&NativePlayerLifetime.allIdle(),"all12 actual memory faults reached and native lifetimes idle");var output=Path.of(args[2]);Files.writeString(output.resolveSibling("battle-cancellation-wire.txt"),String.join("\n",wires)+"\n");Files.writeString(output.resolveSibling("battle-cancellation-goldens.txt"),String.join("\n",goldens)+"\n");Files.writeString(output.resolveSibling("battle-cancellation-notes.txt"),String.join("\n",notes)+"\n");String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"self_skills\":2,\"material_skills\":2,\"Source_window_bypasses\":"+gaps+",\"actual_frames\":"+frames+",\"locked_write_calls\":"+lockedWrites+",\"actual_memory_faults\":"+faults+",\"failed_material_casts_with_partial_commit\":0,\"production_PC_overlays\":0,\"full_goal_achieved\":false}";Files.writeString(output,result+"\n");System.out.println(result);}
}

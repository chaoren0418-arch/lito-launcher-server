package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
/** Frozen scenes, paired waiters and the actual synchronized socket publication boundary. */
public final class StaticMotionLifecycleProbe extends ReturnNatureMaterialProbe {
 static int checked;static final List<String> rows=new ArrayList<>();static ClassicWorldNpcCatalog.StaticNpc placed;
 @SuppressWarnings("unchecked") static void catalog()throws Exception{
  var source=StaticMotionProbe.npc(71092);var map=ClassicWorldMapCatalog.forMap(1011);int px=0,py=0;
  outer:for(int x=map.startX()+2;x<map.endX()-2;x++)for(int y=map.startY()+2;y<map.endY()-2;y++){
   boolean free=true;for(int dx=-2;dx<=2;dx++)for(int dy=-2;dy<=2;dy++)if(!map.standingPassable(x+dx,y+dy))free=false;if(free){px=x;py=y;break outer;}
  }need(px!=0,"real instanced map has a passable fixture neighborhood");placed=copy(source,Map.of("mapId",1011,"x",px,"y",py));
  // Only this owned fixture VM changes its immutable catalog root; candidate class bytes stay exact.
  var field=ClassicWorldNpcCatalog.class.getDeclaredField("BY_MAP");field.setAccessible(true);var maps=new HashMap<>((Map<Integer,List<ClassicWorldNpcCatalog.StaticNpc>>)field.get(null));var npcs=new ArrayList<>(maps.getOrDefault(1011,List.of()));npcs.add(placed);maps.put(1011,List.copyOf(npcs));
  var uf=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);var unsafe=(sun.misc.Unsafe)uf.get(null);unsafe.putObjectVolatile(unsafe.staticFieldBase(field),unsafe.staticFieldOffset(field),Map.copyOf(maps));
  need(ClassicWorldNpcCatalog.npc(1011,placed.objectId())==placed,"fixture retains canonical catalog identity");
 }
 static void blocked(Thread worker,String owner)throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(worker.isAlive()&&System.nanoTime()<until){if((worker.getState()==Thread.State.BLOCKED||worker.getState()==Thread.State.WAITING)&&Arrays.stream(worker.getStackTrace()).anyMatch(f->f.getClassName().contains(owner)))return;Thread.yield();}throw new AssertionError("fixture did not reach "+owner+" wait: "+worker.getState());}
 interface Task {void run()throws Exception;}
 static Thread worker(String name,AtomicReference<Throwable> failure,Task task){return new Thread(()->{try{task.run();}catch(Throwable e){failure.compareAndSet(null,e);}},name);}
 static void join(Thread worker,AtomicReference<Throwable> failure)throws Exception{worker.join(5000);need(!worker.isAlive(),"fixture producer terminates");if(failure.get()!=null)throw new AssertionError("fixture producer failed",failure.get());}
 static void exercise(ClientSession pc,ClientSession peer,ClientSession remote,ClientSession publicPc,GameHub hub,int mode)throws Exception{
  setup(remote,publicPc,hub,true);setup(pc,peer,hub,true);var npc=placed;
  StaticMotionProbe.place(pc,hub,npc,11,npc.x()+2,npc.y()+2);StaticMotionProbe.place(peer,hub,npc,11,npc.x()+2,npc.y()+2);StaticMotionProbe.place(remote,hub,npc,22,npc.x()+2,npc.y()+2);StaticMotionProbe.place(publicPc,hub,npc,-1,npc.x()+2,npc.y()+2);
  try(var a=new SourceActionPairProbe.Wire(pc,peer);var b=new SourceActionPairProbe.Wire(peer,pc);var c=new SourceActionPairProbe.Wire(remote,pc);var d=new SourceActionPairProbe.Wire(publicPc,pc)){
   if(mode!=5)StaticMotionProbe.perceive(pc,npc);StaticMotionProbe.perceive(peer,npc);StaticMotionProbe.perceive(remote,npc);StaticMotionProbe.perceive(publicPc,npc);StaticMotionProbe.packets(a);StaticMotionProbe.packets(b);StaticMotionProbe.packets(c);StaticMotionProbe.packets(d);
   NativeStaticNpcMotion.touch(npc,pc);var state=NativeStaticNpcMotion.state(hub,false);var key=new NativeStaticNpcScene.Key(1011,11,npc.objectId());var m=state.actors.get(key);int h=StaticMotionProbe.free(pc,hub,npc,m);int oldX=m.x,oldY=m.y;m.direction=h;m.distance=1;m.due=0;var failure=new AtomicReference<Throwable>();
   if(mode==0){
    var other=state.actors.get(new NativeStaticNpcScene.Key(1011,22,npc.objectId()));var shared=state.actors.get(new NativeStaticNpcScene.Key(1011,-1,npc.objectId()));other.due=Long.MAX_VALUE;shared.due=Long.MAX_VALUE;NativeStaticNpcMotion.tick(hub);
    need(m.x==oldX+NativeStaticNpcMotion.DX[h]&&other.x==oldX&&shared.x==oldX,"public and two private scenes have independent positions");need(!StaticMotionProbe.has(StaticMotionProbe.packets(c),Protocol.S_MOVE_OBJECT,npc.objectId())&&!StaticMotionProbe.has(StaticMotionProbe.packets(d),Protocol.S_MOVE_OBJECT,npc.objectId()),"private motion never crosses scene output");
   }
   if(mode==1||mode==2){
    var lock=pc.nativePlayerActionLock();lock.lock();Thread thread=worker("static-scene-owned-wait",failure,()->{if(mode==1)NativeStaticNpcMotion.dialog(pc,npc.objectId());else NativeStaticNpcMotion.moved(pc,(int)get(pc,"positionX"),(int)get(pc,"positionY"),(int)get(pc,"positionX")+1,(int)get(pc,"positionY"),2);});
    int x=(int)get(pc,"positionX"),y=(int)get(pc,"positionY");try{thread.start();blocked(thread,"NativeSourcePlayerActionLock");NativePlayerCommit.advance(pc);}finally{lock.unlock();}join(thread,failure);
    need(m.rest==0&&(int)get(pc,"positionX")==x&&(int)get(pc,"positionY")==y,"queued dialog/movement cannot enter replacement character");need(StaticMotionProbe.packets(a).isEmpty(),"retired queued producer emits no frame");
   }
   if(mode==3||mode==4){
    Thread thread=worker("static-output-retirement",failure,()->NativeStaticNpcMotion.tick(hub));
    synchronized(pc){thread.start();blocked(thread,"NativeStaticNpcMotion");if(mode==3)NativePlayerCommit.advance(pc);else hub.close();}
    join(thread,failure);need(StaticMotionProbe.packets(a).isEmpty(),"role change/close drops output already waiting on real socket monitor");if(mode==4){NativeStaticNpcMotion.tick(hub);NativeStaticNpcMotion.touch(npc,pc);StaticMotionProbe.perceive(pc,npc);need(StaticMotionProbe.packets(a).isEmpty(),"late put after closed scene cannot bypass cleared state");need(state.closed&&state.poses.isEmpty()&&get(hub,NativeStaticNpcScene.FIELD)==null,"actual hub close releases snapshots and already-admitted ticks cannot recreate them");}
   }
   if(mode==5){
    var first=worker("static-first-perception",failure,()->StaticMotionProbe.perceive(pc,npc));var tick=worker("static-first-tick",failure,()->NativeStaticNpcMotion.tick(hub));
    synchronized(pc){first.start();blocked(first,"NativeStaticNpcMotion");tick.start();blocked(tick,"NativeStaticNpcMotion");}join(first,failure);join(tick,failure);
    var frames=StaticMotionProbe.packets(a);var relevant=frames.stream().filter(p->((p[0]&255)==Protocol.S_PUT_OBJECT||(p[0]&255)==Protocol.S_MOVE_OBJECT)&&NativeStaticNpcMotion.id(p,(p[0]&255)==Protocol.S_PUT_OBJECT?5:1)==npc.objectId()).toList();
    need(relevant.size()==1&&(relevant.get(0)[0]&255)==Protocol.S_PUT_OBJECT,"first PUT and ticking movement reconcile exactly once");need(StaticMotionProbe.word(relevant.get(0),1)==m.x&&StaticMotionProbe.word(relevant.get(0),3)==m.y,"delayed initial PUT publishes current authoritative coordinates");
   }
   if(mode==6){
    NativeStaticNpcMotion.tick(hub);StaticMotionProbe.packets(a);StaticMotionProbe.packets(b);
    int px=(int)get(pc,"positionX"),py=(int)get(pc,"positionY");set(pc,"learnedSkills",new HashSet<>(Set.of(44)));var spell=BattleSpellCatalog.cancellationSpell(44);NativeDeItemPublication.grant(pc,Math.toIntExact(spell.itemConsumeId()),spell.itemConsumeCount()+3);int mp=(int)get(pc,"playerMp");
    call(pc,"handle",new Class[]{byte[].class},new Protocol.Writer().c(Protocol.C_USE_SPELL).c(5).c(3).d(npc.objectId()).bytes());var rule=NativeCancellationNpcRules.lookup(npc.npcId());need(NativeStaticNpcScene.gfx(hub,npc,pc)==rule.targetId()&&(int)get(pc,"playerMp")==mp-cost(pc,44,spell),"actual Cancellation transforms moving target and charges once");var delivered=StaticMotionProbe.packets(a);need(StaticMotionProbe.has(delivered,Protocol.S_POLYMORPH,npc.objectId()),"current visible target gets Source shape mutation");m.due=Long.MAX_VALUE;NativeStaticNpcMotion.tick(hub);need(!StaticMotionProbe.has(StaticMotionProbe.packets(a),Protocol.S_PUT_OBJECT,npc.objectId()),"accepted name/shape output does not trigger redundant first PUT");
   }
   if(mode==7){
    NativeStaticNpcMotion.tick(hub);int x=m.x,y=m.y;set(pc,"positionX",x-NativeStaticNpcMotion.DX[h]);set(pc,"positionY",y-NativeStaticNpcMotion.DY[h]);hub.updateClassicWorld(pc,1011,(int)get(pc,"positionX"),(int)get(pc,"positionY"),h);StaticMotionProbe.packets(a);
    NativeStaticNpcMotion.moved(pc,(int)get(pc,"positionX"),(int)get(pc,"positionY"),x,y,h);need((int)get(pc,"positionX")!=x||(int)get(pc,"positionY")!=y,"actual player move rejects moving NPC current tile");
   }
   if(mode==8){
    NativeStaticNpcMotion.tick(hub);for(var member:List.of(pc,peer))set(member,"positionX",npc.x()+100);m.due=0;NativeStaticNpcMotion.tick(hub);need(m.x==npc.x()&&m.y==npc.y()&&m.due==0,"Source no-player stop teleports to original home");
   }
   if(mode>=9&&mode<=11){
    int x=oldX+NativeStaticNpcMotion.DX[h],y=oldY+NativeStaticNpcMotion.DY[h];var pet=companion(pc,x+NativeStaticNpcMotion.DX[h],y+NativeStaticNpcMotion.DY[h],false);var before=pet.snapshot();int toward=(h+4)&7;
    if(mode==9){NativeStaticNpcMotion.tick(hub);StaticMotionProbe.packets(a);need(m.x==x&&m.y==y,"NPC commits into previously selected companion landing");need(NativeStaticNpcMotion.companionMove(pet,x,y,toward,System.nanoTime(),pc)==null,"actual companion landing rechecks moving NPC atomically");}
    else{var result=new AtomicReference<BattleCompanion.Movement>();var lock=pc.nativePlayerActionLock();lock.lock();var thread=worker("companion-static-landing-life",failure,()->result.set(NativeStaticNpcMotion.companionMove(pet,x,y,toward,System.nanoTime(),pc)));
     try{thread.start();blocked(thread,"NativeSourcePlayerActionLock");if(mode==10)NativePlayerCommit.advance(pc);else{pet.deactivate();((List<BattleCompanion>)get(pc,"tamedCompanions")).remove(pet);var replacement=companion(pc,before.x(),before.y(),false);need(replacement.snapshot().objectId()==before.objectId(),"real same-ID companion replacement");}}
     finally{lock.unlock();}join(thread,failure);need(result.get()==null,"queued companion cannot enter retired role or replaced actor");
    }
    need(pet.snapshot().x()==before.x()&&pet.snapshot().y()==before.y()&&StaticMotionProbe.packets(a).isEmpty(),"rejected landing changes no coordinate/action output");
   }
   need(a.locked+b.locked+c.locked+d.locked==0,"all lifecycle frames occur after paired locks");
   for(var member:new ArrayList<>(((Map<ClientSession,?>)get(hub,"classicWorldMembership")).keySet()))hub.leaveClassicWorld(member);
   need(get(hub,NativeStaticNpcScene.FIELD)==null&&state.closed&&state.poses.isEmpty(),"every transient scene and pose released at last member");
   if(mode==0){long phase=(long)get(hub,NativeStaticNpcMotion.REST);hub.enterClassicWorld(pc,npc.mapId(),npc.x()+2,npc.y()+2,0,11);NativeStaticNpcMotion.touch(npc,pc);need(NativeStaticNpcMotion.state(hub,false).nextRest==phase,"scene reuse preserves server-wide five-second rest phase");hub.leaveClassicWorld(pc);}
   need(NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(peer))==0,"all Source role receipts released");rows.add("mode="+mode+" instance=11 npc="+npc.objectId());checked++;
  }finally{StaticMotionProbe.readPositions.clear();}
 }
 public static void main(String[]args)throws Exception{
  var jar=Path.of(args[3]).toRealPath();for(var type:List.of(ClientSession.class,NativeStaticNpcMotion.class,NativeStaticNpcScene.class,ClassicWorldNpcCatalog.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"single candidate class authority");var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);catalog();var root=Path.of(args[1]);
  try{for(boolean flag:new boolean[]{false,true})for(int mode=0;mode<12;mode++){
   var r=root.resolve(flag+"-"+mode);Files.createDirectories(r);var cfg=copy(base,Map.of("battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);world(fixtures,hub);exercise(p.session,m.session,f.session,o.session,hub,mode);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"persistence setting cannot write memory fixture");
  }}finally{NativePlayerCommit.closeFixture();}need(NativePlayerLifetime.allIdle(),"all runtime lifetimes idle");Files.write(Path.of(args[2]).resolveSibling("cases.txt"),rows);Files.writeString(Path.of(args[2]),"{\"cases\":"+checked+",\"checks\":"+ownChecks+",\"full_goal_achieved\":false}\n");
 }
}

package local.lito381.server;
import java.io.*;import java.lang.ref.WeakReference;import java.util.*;import java.util.concurrent.ThreadLocalRandom;import java.util.function.IntUnaryOperator;

/** Ephemeral L1Quest motion owned by the existing static scene; never a SQL spawn edit. */
final class NativeStaticNpcMotion {
 static final int[] DX={0,1,1,1,0,-1,-1,-1},DY={-1,-1,0,1,1,1,0,-1},SLIDE={0,-1,1,2,-2,-3,3,4};
 static final Set<Integer> NO_AI=Set.of(71075,70957,81209,80012);
 static final String FIELD="nativeStaticMotion";
 static final String CLOSED="nativeStaticMotionClosed";
 static final String REST="nativeStaticMotionRestTick";
 static final Set<Integer> PACKETS=Set.of(Protocol.S_PUT_OBJECT,Protocol.S_REMOVE_OBJECT,Protocol.S_MOVE_OBJECT,Protocol.S_CHANGE_DIRECTION,Protocol.S_POLYMORPH,Protocol.S_CHANGE_DESC);
 record Scene(int map,int instance){}
 record Pose(ClassicWorldNpcCatalog.StaticNpc npc,int instance,int x,int y,int heading,int gfx,String name,int speed,Object actor){}
 static final class Epoch {final Map<Long,Pose> known=new HashMap<>();}
 static final class View {final int map,instance;volatile Epoch epoch=new Epoch();View(int map,int instance){this.map=map;this.instance=instance;}}
 static final class Motion {
  final ClassicWorldNpcCatalog.StaticNpc npc;int x,y,heading,distance,direction,rest;long due;
  volatile boolean nativeStaticCancellationRetired;
  Motion(ClassicWorldNpcCatalog.StaticNpc npc){this.npc=npc;x=npc.x();y=npc.y();heading=npc.heading();}
 }
 static final class State {
  final Map<NativeStaticNpcScene.Key,Motion> actors=new HashMap<>();
  volatile Map<NativeStaticNpcScene.Key,Pose> poses=Map.of();
  final Map<ClientSession,View> viewers=new WeakHashMap<>();
  long nextRest;volatile boolean closed;
 }
 interface Terrain {
  boolean step(int fromX,int fromY,int x,int y);
  boolean occupied(int x,int y);
  boolean door(int x,int y);
  int home(int x,int y,int homeX,int homeY,int heading);
 }
 static boolean eligible(ClassicWorldNpcCatalog.StaticNpc npc){return npc!=null&&npc.hp()>0&&npc.implementation().equals("L1Quest")&&!NO_AI.contains(npc.npcId());}
 /** Source NpcAI resets its default delay to 300ms on each idle iteration. */
 static void idle(Motion m,long now,boolean nearby,int speed,Terrain map,IntUnaryOperator random){
  if(!eligible(m.npc))return;
  if(!nearby){if(m.npc.x()!=0&&m.npc.y()!=0){m.x=m.npc.x();m.y=m.npc.y();}m.due=0;return;}
  if(now<m.due)return;
  m.due=now+300_000_000L;
  if(speed<=0||m.rest>0)return;
  if(m.distance==0){m.distance=random.applyAsInt(3)+1;m.direction=random.applyAsInt(40);
   if(m.direction<8&&m.npc.x()!=0&&m.npc.y()!=0)m.direction=map.home(m.x,m.y,m.npc.x(),m.npc.y(),m.heading);
  }else m.distance--;
  if(m.direction<0||m.direction>=8)return;
  for(int offset:SLIDE){int heading=(m.direction+offset+8)&7,x=m.x+DX[heading],y=m.y+DY[heading];
   if(!map.step(m.x,m.y,x,y)||map.occupied(x,y))continue;
   // Source openDoor runs after checkObject. A closed door does not retry another heading.
   if(map.door(x,y))return;
   m.x=x;m.y=y;m.heading=heading;m.due=now+Math.max(1,speed)*1_000_000L;return;
  }
 }
 static State state(GameHub hub,boolean create){
  if(hub==null)return null;
  if(create&&(boolean)CombatAccess.field(hub,CLOSED))return null;
  var scene=(NativeStaticNpcScene.State)CombatAccess.field(hub,NativeStaticNpcScene.FIELD);
  if(scene==null){if(!create)return null;scene=NativeStaticNpcScene.state(hub,true);}
  var state=(State)CombatAccess.field(scene,FIELD);
  if(state==null&&create){if(!Thread.holdsLock(hub))throw new IllegalStateException("static motion requires hub ownership");state=new State();state.nextRest=restPhase(hub,System.nanoTime());CombatAccess.set(scene,FIELD,state);}return state;
 }
 static long restPhase(GameHub hub,long now){long phase=(long)CombatAccess.field(hub,REST);if(phase==0)phase=now+5_000_000_000L;else if(now>=phase)phase+=(1+(now-phase)/5_000_000_000L)*5_000_000_000L;CombatAccess.set(hub,REST,phase);return phase;}
 static Pose pose(ClassicWorldNpcCatalog.StaticNpc npc,ClientSession pc){
  var hub=(GameHub)CombatAccess.field(pc,"hub");return pose(hub,npc,NativeStaticNpcScene.instance(pc));
 }
 static Pose pose(GameHub hub,ClassicWorldNpcCatalog.StaticNpc npc,int instance){
  var state=state(hub,false);return state==null?null:state.poses.get(new NativeStaticNpcScene.Key(npc.mapId(),instance,npc.objectId()));
 }
 static int x(ClassicWorldNpcCatalog.StaticNpc npc,ClientSession pc){var pose=pose(npc,pc);return pose==null?npc.x():pose.x();}
 static int y(ClassicWorldNpcCatalog.StaticNpc npc,ClientSession pc){var pose=pose(npc,pc);return pose==null?npc.y():pose.y();}
 static int heading(int original,GameHub hub,ClassicWorldNpcCatalog.StaticNpc npc,ClientSession pc){var pose=pose(hub,npc,NativeStaticNpcScene.instance(pc));return pose==null?original:pose.heading();}
 static List<ClassicWorldNpcCatalog.StaticNpc> inScreen(int map,int x,int y,ClientSession pc){
  return ClassicWorldNpcCatalog.all(map).stream().filter(npc->BattleSpellCatalog.isInNativeScreen(x,y,x(npc,pc),y(npc,pc))).toList();
 }
 static boolean occupies(int map,int x,int y,ClientSession pc){return occupies((GameHub)CombatAccess.field(pc,"hub"),NativeStaticNpcScene.instance(pc),map,x,y,0);}
 static boolean occupies(GameHub hub,int instance,int map,int x,int y,long except){
  var state=state(hub,false);var poses=state==null?Map.<NativeStaticNpcScene.Key,Pose>of():state.poses;
  for(var npc:ClassicWorldNpcCatalog.all(map)){
   if(npc.objectId()==except||ClassicWorldGuardianCatalog.isGuardian(npc)||ClassicWorldGuardCatalog.isGuard(npc))continue;
   var pose=poses.get(new NativeStaticNpcScene.Key(map,instance,npc.objectId()));if((pose==null?npc.x():pose.x())==x&&(pose==null?npc.y():pose.y())==y)return true;
  }return false;
 }
 static void publish(GameHub hub,State state){
  if(!Thread.holdsLock(hub))throw new IllegalStateException("static pose publication requires hub ownership");
  var poses=new HashMap<NativeStaticNpcScene.Key,Pose>();for(var entry:state.actors.entrySet()){
   var key=entry.getKey();var m=entry.getValue();var presentation=NativeStaticNpcScene.actor(hub,m.npc,key.instance(),false);
   int gfx=key.instance()>0?(presentation==null||presentation.gfx==null?m.npc.gfxId():presentation.gfx):hub.classicWorldNpcGfx(m.npc);
   var appearance=NativeStaticNpcScene.appearance(hub,m.npc,key.instance());
   poses.put(key,new Pose(m.npc,key.instance(),m.x,m.y,m.heading,gfx,appearance.name(),appearance.speed(),m));
  }state.poses=Map.copyOf(poses);
 }
 /** Kept with presentation state until membership pruning or hub close. */
 static boolean retained(NativeStaticNpcScene.State scene){var state=(State)CombatAccess.field(scene,FIELD);return state!=null&&!state.closed;}
 static boolean empty(Map<?,?> actors,GameHub hub){if(!actors.isEmpty())return false;var state=state(hub,false);if(state!=null&&!state.actors.isEmpty())return false;if(state!=null)close(hub);return true;}
 static List<ClassicWorldNpcCatalog.StaticNpc> projectedAll(int map,ClientSession pc){return ClassicWorldNpcCatalog.all(map).stream().map(npc->projected(npc,pc)).toList();}
 static void prune(GameHub hub){synchronized(hub){var state=state(hub,false);if(state==null)return;
  state.actors.keySet().removeIf(k->hub.classicWorldMembers(k.map(),k.instance()).isEmpty());
  synchronized(state.viewers){state.viewers.entrySet().removeIf(e->hub.classicWorldMembers(e.getValue().map,e.getValue().instance).isEmpty());}
  publish(hub,state);
 }}
 static void close(GameHub hub){synchronized(hub){var state=state(hub,false);if(state==null)return;state.closed=true;state.actors.clear();state.poses=Map.of();synchronized(state.viewers){for(var v:state.viewers.values())v.epoch=new Epoch();state.viewers.clear();}}}
 static void stop(GameHub hub){synchronized(hub){CombatAccess.set(hub,CLOSED,true);close(hub);}}
 static boolean alive(boolean valid,GameHub hub){return valid&&!(boolean)CombatAccess.field(hub,CLOSED);}
 static void touch(ClassicWorldNpcCatalog.StaticNpc npc,ClientSession pc){
  if(!eligible(npc))return;var hub=(GameHub)CombatAccess.field(pc,"hub");int instance=NativeStaticNpcScene.instance(pc);
  synchronized(hub){if(!NativeStaticNpcScene.inScene(pc,hub,npc.mapId(),instance))return;var state=state(hub,true);if(state==null||state.closed)return;
   var key=new NativeStaticNpcScene.Key(npc.mapId(),instance,npc.objectId());if(!state.actors.containsKey(key)){var m=new Motion(npc);m.heading=NativeStaticNpcScene.heading(hub,npc,pc);state.actors.put(key,m);}publish(hub,state);
  }
 }
 static void changed(GameHub hub){synchronized(hub){var state=state(hub,false);if(state!=null&&!state.closed)publish(hub,state);}}
 static boolean turned(boolean success,GameHub hub,ClassicWorldNpcCatalog.StaticNpc npc,int heading,ClientSession pc){
  if(success)synchronized(hub){var state=state(hub,false);if(state!=null){var m=state.actors.get(new NativeStaticNpcScene.Key(npc.mapId(),NativeStaticNpcScene.instance(pc),npc.objectId()));if(m!=null)m.heading=heading;publish(hub,state);}}return success;
 }
 static ClassicWorldNpcCatalog.StaticNpc projection(ClassicWorldNpcCatalog.StaticNpc npc,int x,int y,int heading,String name,int speed){
  return new ClassicWorldNpcCatalog.StaticNpc(npc.mapId(),npc.spawnId(),npc.objectId(),npc.npcId(),x,y,heading,npc.movementDistance(),name,npc.implementation(),npc.gfxId(),npc.level(),npc.hp(),npc.mp(),npc.experience(),npc.lawful(),npc.lightSize(),speed,npc.atkspeed(),npc.changeHead(),npc.normalAction(),npc.chaoticAction());
 }
 static ClassicWorldNpcCatalog.StaticNpc projected(ClassicWorldNpcCatalog.StaticNpc npc,ClientSession pc){var pose=pose(npc,pc);return pose==null?npc:projection(npc,pose.x(),pose.y(),pose.heading(),pose.name(),pose.speed());}
 static byte[] put(Pose pose){return Protocol.classicStaticNpc(projection(pose.npc(),pose.x(),pose.y(),pose.heading(),pose.name(),pose.speed()),pose.heading(),pose.gfx());}
 static View view(State state,ClientSession pc,int map,int instance){synchronized(state.viewers){var view=state.viewers.get(pc);if(view==null||view.map!=map||view.instance!=instance){view=new View(map,instance);state.viewers.put(pc,view);}return view;}}
 static void retire(ClientSession pc){var state=state((GameHub)CombatAccess.field(pc,"hub"),false);if(state==null)return;synchronized(state.viewers){var view=state.viewers.remove(pc);if(view!=null)view.epoch=new Epoch();}}
 static void advance(ClientSession pc){retire(pc);try{NativePlayerCommit.nativeStaticOriginalAdvance(pc);}finally{retire(pc);}}
 static boolean viewer(ClientSession pc,View view,State state,Epoch epoch){return !state.closed&&view.epoch==epoch&&!(boolean)CombatAccess.field(pc,NativeDeItemApplication.DISCONNECTED)&&String.valueOf(CombatAccess.field(pc,"phase")).equals("WORLD")&&(int)CombatAccess.field(pc,"classicWorldMapId")==view.map&&NativeStaticNpcScene.instance(pc)==view.instance;}
 static long id(byte[] packet,int offset){long id=0;for(int i=0;i<4;i++)id|=(long)(packet[offset+i]&255)<<(8*i);return id;}
 /** Called outside the old synchronized frame method. Never acquire hub/status locks under pc. */
 static void frame(ClientSession pc,byte[] packet,String note)throws IOException{
  int op=packet.length==0?-1:packet[0]&255;int offset=op==Protocol.S_PUT_OBJECT?5:1;
  if(!PACKETS.contains(op)||packet.length<offset+4){pc.nativeStaticOriginalFrame(packet,note);return;}
  var hub=(GameHub)CombatAccess.field(pc,"hub");var state=state(hub,false);int map=(int)CombatAccess.field(pc,"classicWorldMapId"),instance=NativeStaticNpcScene.instance(pc);long id=id(packet,offset);
  var npc=ClassicWorldNpcCatalog.npc(map,id);if(eligible(npc)&&(boolean)CombatAccess.field(hub,CLOSED))return;if(state==null||!eligible(npc)){pc.nativeStaticOriginalFrame(packet,note);return;}
  var key=new NativeStaticNpcScene.Key(map,instance,id);if(!state.poses.containsKey(key)){pc.nativeStaticOriginalFrame(packet,note);return;}
  var view=view(state,pc,map,instance);var epoch=view.epoch;
  synchronized(pc){if(!viewer(pc,view,state,epoch))return;var pose=state.poses.get(key);if(pose==null)return;
   boolean visible=BattleSpellCatalog.isInNativeScreen((int)CombatAccess.field(pc,"positionX"),(int)CombatAccess.field(pc,"positionY"),pose.x(),pose.y());
   if(!visible){if(epoch.known.containsKey(id)){pc.nativeStaticOriginalFrame(Protocol.removeObject(id),note);epoch.known.remove(id);}return;}
   if(op==Protocol.S_REMOVE_OBJECT)return;
   if(op==Protocol.S_POLYMORPH||op==Protocol.S_CHANGE_DESC){
    byte[] current=op==Protocol.S_POLYMORPH?Protocol.classicStaticNpcShape(id,pose.gfx(),0):Protocol.changeDescription(id,pose.name());
    if(!Arrays.equals(packet,current))return;var known=epoch.known.get(id);
    if(known==null||known.actor()!=pose.actor()){pc.nativeStaticOriginalFrame(put(pose),note);epoch.known.put(id,pose);return;}
    var position=new Pose(pose.npc(),pose.instance(),pose.x(),pose.y(),pose.heading(),known.gfx(),known.name(),pose.speed(),pose.actor());
    reconcile(pc,state,view,epoch,position,note);pc.nativeStaticOriginalFrame(packet,note);
    epoch.known.put(id,new Pose(pose.npc(),pose.instance(),pose.x(),pose.y(),pose.heading(),op==Protocol.S_POLYMORPH?pose.gfx():known.gfx(),op==Protocol.S_CHANGE_DESC?pose.name():known.name(),pose.speed(),pose.actor()));return;
   }
   reconcile(pc,state,view,epoch,pose,note);
  }
 }
 static void reconcile(ClientSession pc,State state,View view,Epoch epoch,Pose pose,String note)throws IOException{
  long id=pose.npc().objectId();var known=epoch.known.get(id);
  if(known==null||known.actor()!=pose.actor()||known.gfx()!=pose.gfx()||!known.name().equals(pose.name()))pc.nativeStaticOriginalFrame(put(pose),note);
  else if(known.x()!=pose.x()||known.y()!=pose.y()){
   if(Math.max(Math.abs(known.x()-pose.x()),Math.abs(known.y()-pose.y()))==1){int heading=direction(known.x(),known.y(),pose.x(),pose.y());pc.nativeStaticOriginalFrame(new Protocol.Writer().c(Protocol.S_MOVE_OBJECT).d(id).h(known.x()).h(known.y()).c(heading).bytes(),note);
    if(heading!=pose.heading())pc.nativeStaticOriginalFrame(Protocol.changeDirection(id,pose.heading()),note);
   }else{pc.nativeStaticOriginalFrame(Protocol.removeObject(id),note);pc.nativeStaticOriginalFrame(put(pose),note);}
  }else if(known.heading()!=pose.heading())pc.nativeStaticOriginalFrame(Protocol.changeDirection(id,pose.heading()),note);
  epoch.known.put(id,pose);
 }
 static int direction(int x,int y,int toX,int toY){int dx=Integer.signum(toX-x),dy=Integer.signum(toY-y);for(int h=0;h<8;h++)if(DX[h]==dx&&DY[h]==dy)return h;return -1;}
 static void deliver(ClientSession pc,State state){
  if(!pc.nativePlayerActionLock().tryLock())return;Runnable completion=null;
  try{var map=(int)CombatAccess.field(pc,"classicWorldMapId");int instance=NativeStaticNpcScene.instance(pc);var view=view(state,pc,map,instance);var epoch=view.epoch;
   completion=NativeRolePublication.completion(pc,()->{synchronized(pc){if(!viewer(pc,view,state,epoch))return;
    try{var poses=state.poses;for(var known:new ArrayList<>(epoch.known.values())){var pose=poses.get(new NativeStaticNpcScene.Key(map,instance,known.npc().objectId()));if(pose==null||!BattleSpellCatalog.isInNativeScreen((int)CombatAccess.field(pc,"positionX"),(int)CombatAccess.field(pc,"positionY"),pose.x(),pose.y())){pc.nativeStaticOriginalFrame(Protocol.removeObject(known.npc().objectId()),"S_REMOVE_OBJECT static-motion");epoch.known.remove(known.npc().objectId());}}
     for(var pose:poses.values())if(pose.npc().mapId()==map&&pose.instance()==instance&&BattleSpellCatalog.isInNativeScreen((int)CombatAccess.field(pc,"positionX"),(int)CombatAccess.field(pc,"positionY"),pose.x(),pose.y()))reconcile(pc,state,view,epoch,pose,"static-motion perception");
    }catch(IOException e){throw new UncheckedIOException(e);}
   }});
  }finally{pc.nativePlayerActionLock().unlock();NativePlayerActionScope.unlocked(pc);}
  if(completion!=null)completion.run();
 }
 static long tile(int x,int y){return (long)x<<32|Integer.toUnsignedLong(y);}
 @SuppressWarnings("unchecked") static void tick(GameHub hub){
  State state;List<ClientSession> receivers;
  synchronized(hub){
   if((boolean)CombatAccess.field(hub,CLOSED))return;
   var old=state(hub,false);if(old==null||old.closed)restPhase(hub,System.nanoTime());
   var membership=(Map<ClientSession,?>)CombatAccess.field(hub,"classicWorldMembership");receivers=new ArrayList<>(membership.keySet());
   var scenes=new LinkedHashMap<Scene,List<ClientSession>>();
   for(var pc:receivers)if(!(boolean)CombatAccess.field(pc,NativeDeItemApplication.DISCONNECTED)&&String.valueOf(CombatAccess.field(pc,"phase")).equals("WORLD")){
    var key=new Scene((int)CombatAccess.field(pc,"classicWorldMapId"),NativeStaticNpcScene.instance(pc));scenes.computeIfAbsent(key,k->new ArrayList<>()).add(pc);
   }
   for(var entry:scenes.entrySet())for(var npc:ClassicWorldNpcCatalog.all(entry.getKey().map()))if(eligible(npc))touch(npc,entry.getValue().get(0));
   state=state(hub,false);if(state==null||state.closed)return;long now=System.nanoTime();
   if(now>=state.nextRest){long ticks=1+(now-state.nextRest)/5_000_000_000L;for(var m:state.actors.values())m.rest=(int)Math.max(0,m.rest-Math.min(Integer.MAX_VALUE,ticks*5));state.nextRest+=ticks*5_000_000_000L;CombatAccess.set(hub,REST,state.nextRest);}
   for(var entry:state.actors.entrySet()){
    var key=entry.getKey();var m=entry.getValue();var peers=scenes.getOrDefault(new Scene(key.map(),key.instance()),List.of());
    boolean nearby=peers.stream().anyMatch(pc->Math.max(Math.abs((int)CombatAccess.field(pc,"positionX")-m.x),Math.abs((int)CombatAccess.field(pc,"positionY")-m.y))<=20);
    var population=(ClassicWorldMonsterPopulation)CombatAccess.call(hub,"classicWorldMonsterPopulation","(II)Llocal/lito381/server/ClassicWorldMonsterPopulation;",key.map(),key.instance());
    synchronized(population==null?hub:population){
     var footprints=new HashSet<Long>();for(var pc:peers){if((int)CombatAccess.field(pc,"playerHp")>0&&!(boolean)CombatAccess.field(pc,"observer"))footprints.add(tile((int)CombatAccess.field(pc,"positionX"),(int)CombatAccess.field(pc,"positionY")));
      for(var pet:(List<BattleCompanion.Snapshot>)CombatAccess.call(pc,"classicWorldCompanionSnapshots","()Ljava/util/List;"))if(pet.active()&&pet.hp()>0)footprints.add(tile(pet.x(),pet.y()));
     }
     if(key.instance()<=0){for(var f:hub.classicWorldRescueFollowers().inScreen(key.map(),m.x,m.y))footprints.add(tile(f.x(),f.y()));for(var f:hub.classicWorldRoiFollowers().inScreen(key.map(),m.x,m.y))footprints.add(tile(f.x(),f.y()));for(var f:hub.classicWorldDspyEscorts().inScreen(key.map(),m.x,m.y))if(f.alive())footprints.add(tile(f.x(),f.y()));}
     var map=ClassicWorldMapCatalog.forMap(key.map());var terrain=new Terrain(){
      public boolean step(int sx,int sy,int x,int y){return map.canStep(sx,sy,x,y);}
      public boolean occupied(int x,int y){return footprints.contains(tile(x,y))||occupies(hub,key.instance(),key.map(),x,y,key.id())||hub.classicWorldMonsterOccupies(key.map(),key.instance(),x,y);}
      public boolean door(int x,int y){return hub.dwarfCastleDoorBlocks(key.map(),x,y)||hub.adenCastleDoorBlocks(key.map(),x,y)||hub.heineCastleDoorBlocks(key.map(),x,y)||hub.mainlandCastleDoorBlocks(key.map(),x,y)||hub.classicWorldWaterDragonDoorBlocks(key.map(),key.instance(),x,y);}
      public int home(int x,int y,int hx,int hy,int heading){if(x==hx&&y==hy)return heading;int h=direction(x,y,hx,hy);if(Math.max(Math.abs(x-hx),Math.abs(y-hy))<=1)return h;var result=ClassicWorldNpcPathfinder.nextStep(map,x,y,hx,hy,1,this::occupied);return result.moved()?result.heading():-1;}
     };
     idle(m,now,nearby,NativeStaticNpcScene.appearance(hub,m.npc,key.instance()).speed(),terrain,bound->ThreadLocalRandom.current().nextInt(bound));
     if(key.instance()<=0&&hub.classicWorldNpcHeading(m.npc)!=m.heading)hub.changeClassicWorldNpcHeading(m.npc,m.heading);
     // Publish while the population monitor still excludes competing landing commits.
     publish(hub,state);
    }
   }
   state.actors.keySet().removeIf(k->!scenes.containsKey(new Scene(k.map(),k.instance())));publish(hub,state);
  }
  for(var pc:receivers)try{deliver(pc,state);}catch(RuntimeException e){System.err.println("[LiTo] static NPC output recovered: "+e.getClass().getSimpleName());}
 }
 static void tickSafely(GameHub hub){try{tick(hub);}catch(Throwable e){System.err.println("[LiTo] static NPC clock recovered: "+e.getClass().getSimpleName());}}
 static void moved(ClientSession pc,int sx,int sy,int x,int y,int heading)throws IOException{
  var binding=NativePlayerCommit.Binding.capture(pc);var hub=(GameHub)CombatAccess.field(pc,"hub");var location=NativeStaticNpcScene.location(hub,pc);
  NativeLightAction.commitPair(pc,pc,()->{synchronized(hub){if(alive(binding.matches(pc),hub)&&location!=null&&NativeStaticNpcScene.location(hub,pc)==location)pc.nativeStaticOriginalMove(sx,sy,x,y,heading);}});
 }
 static BattleCompanion.Movement companionMove(BattleCompanion pet,int x,int y,int heading,long now,ClientSession pc)throws IOException{
  if(!String.valueOf(CombatAccess.field(pc,"phase")).equals("WORLD"))return pet.moveOneTile(x,y,heading,now);
  var master=NativePlayerCommit.Binding.capture(pc);var stamp=NativeCompanionCallbacks.capture(master,pet);if(stamp==null)return null;
  var hub=(GameHub)CombatAccess.field(pc,"hub");var location=NativeStaticNpcScene.location(hub,pc);var result=new BattleCompanion.Movement[1];
  NativeLightAction.commitPair(pc,pc,()->{synchronized(hub){
   if(!alive(master.matches(pc),hub)||location==null||NativeStaticNpcScene.location(hub,pc)!=location||CombatAccess.call(pc,"ownedCompanion","(J)Llocal/lito381/server/BattleCompanion;",stamp.companion)!=pet)return;
   synchronized(pet){if(!pet.snapshot().active()||pet.snapshot().hp()<=0||stamp.life!=NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(pet,"timedStatus"),false)||occupies((int)CombatAccess.field(pc,"classicWorldMapId"),x,y,pc))return;
    result[0]=pet.moveOneTile(x,y,heading,now);
   }
  }});return result[0];
 }
 static void dialog(ClientSession pc,long id)throws IOException{
  var npc=ClassicWorldNpcCatalog.npc((int)CombatAccess.field(pc,"classicWorldMapId"),id);
  if(npc==null||!npc.implementation().equals("L1Quest")){pc.nativeStaticOriginalDialog(id);return;}
  var binding=NativePlayerCommit.Binding.capture(pc);var hub=(GameHub)CombatAccess.field(pc,"hub");var location=NativeStaticNpcScene.location(hub,pc);
  NativeLightAction.commitPair(pc,pc,()->{synchronized(hub){if(alive(binding.matches(pc),hub)&&location!=null&&NativeStaticNpcScene.location(hub,pc)==location)pc.nativeStaticOriginalDialog(id);}});
 }
 static void talk(ClientSession pc,ClassicWorldNpcCatalog.StaticNpc npc)throws IOException{
  if(!npc.implementation().equals("L1Quest"))return;var hub=(GameHub)CombatAccess.field(pc,"hub");int instance=NativeStaticNpcScene.instance(pc);
  synchronized(hub){if(!NativeStaticNpcScene.inScene(pc,hub,npc.mapId(),instance))return;touch(npc,pc);var state=state(hub,eligible(npc));
   int heading=direction(x(npc,pc),y(npc,pc),(int)CombatAccess.field(pc,"positionX"),(int)CombatAccess.field(pc,"positionY"));if(heading<0)heading=NativeStaticNpcScene.heading(hub,npc,pc);
   NativeStaticNpcScene.turn(hub,npc,heading,pc);
   if(state!=null&&eligible(npc)){var key=new NativeStaticNpcScene.Key(npc.mapId(),instance,npc.objectId());var m=state.actors.computeIfAbsent(key,k->new Motion(npc));m.heading=heading;m.rest=10;publish(hub,state);}
   for(var member:hub.classicWorldMembers(npc.mapId(),instance))if(BattleSpellCatalog.isInNativeScreen((int)CombatAccess.field(member,"positionX"),(int)CombatAccess.field(member,"positionY"),x(npc,pc),y(npc,pc)))NativeCancellationProbability.send(member,Protocol.changeDirection(npc.objectId(),heading),"S_CHANGE_DIRECTION L1Quest-talk");
  }
 }
 static void bind(ClassicWorldMonsterPopulation population,GameHub hub,int instance){if(population!=null){CombatAccess.set(population,"nativeStaticMotionHub",new WeakReference<>(hub));CombatAccess.set(population,"nativeStaticMotionInstance",instance);}}
 static ClassicWorldMonsterPopulation bound(ClassicWorldMonsterPopulation population,GameHub hub,int map,int instance){bind(population,hub,map==ClassicWorldCkewCatalog.MAP_ID||map==ClassicWorldWaterDragonCatalog.MAP_ID?instance:-1);return population;}
 static boolean occupies(int map,int x,int y,ClassicWorldMonsterPopulation population){var ref=(WeakReference<?>)CombatAccess.field(population,"nativeStaticMotionHub");var hub=ref==null?null:(GameHub)ref.get();return hub==null?ClassicWorldNpcCatalog.occupies(map,x,y):occupies(hub,(int)CombatAccess.field(population,"nativeStaticMotionInstance"),map,x,y,0);}
 private NativeStaticNpcMotion(){}
}

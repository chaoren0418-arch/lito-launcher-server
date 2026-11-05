package local.lito381.server;
import java.io.*;import java.lang.ref.WeakReference;import java.util.*;import java.util.concurrent.ThreadLocalRandom;
/** Actor-owned WORLD placement. A master's portal never changes an unmoved actor's scene. */
final class NativeCompanionPlacement {
 static final String FIELD="nativeWorldCompanionPlacement",P="local/lito381/server/";
 static final class Scene {
  final WeakReference<GameHub> hub;final WeakReference<Object> population;final int map,instance;final boolean hadPopulation;
  Scene(GameHub h,int map,int instance){hub=new WeakReference<>(h);this.map=map;this.instance=instance;Object pop=NativeCancellationNpcMaterialAction.population(h,map,instance);population=new WeakReference<>(pop);hadPopulation=pop!=null;}
  boolean alive(GameHub h){Object now=NativeCancellationNpcMaterialAction.population(h,map,instance);return hub.get()==h&&!(boolean)CombatAccess.field(h,"nativeStaticMotionClosed")&&now==population.get()&&(!hadPopulation||now!=null);}
  boolean at(int map,int instance){return this.map==map&&this.instance==instance;}
 }
 static final class State {
  final WeakReference<ClientSession> owner;final BattleCompanion pet;final long id;final Object bag;
  volatile NativePlayerCommit.Binding master;volatile Scene scene;volatile Object pose=new Object();Transition pending;
  State(ClientSession pc,BattleCompanion pet,Scene scene){owner=new WeakReference<>(pc);this.pet=pet;this.scene=scene;id=pet.snapshot().objectId();bag=CombatAccess.field(pet,"carriedItems");master=NativePlayerCommit.Binding.capture(pc);}
  boolean actor(){return pet.snapshot().objectId()==id&&CombatAccess.field(pet,"carriedItems")==bag;}
  boolean owned(ClientSession pc){return owner.get()==pc&&actor()&&CombatAccess.call(pc,"ownedCompanion","(J)L"+P+"BattleCompanion;",id)==pet;}
  boolean role(ClientSession pc){return NativeCompanionRoleIdentity.stateRole(this,pc);}
  boolean nativeRoleOriginalRole(ClientSession pc){return owned(pc)&&master.matches(pc)&&!(boolean)CombatAccess.field(pc,"sourceItemDisconnected");}
 }
 static final class Transition {
  final WeakReference<ClientSession> owner;final Object capability;final NativePlayerCommit.Binding before;final List<State> states=new ArrayList<>();boolean active=true;
  Transition(ClientSession pc,Object capability){owner=new WeakReference<>(pc);this.capability=capability;before=NativePlayerCommit.Binding.capture(pc);}
  boolean character(ClientSession pc){return NativeCompanionRoleIdentity.transitionCharacter(this,pc);}
  boolean nativeRoleOriginalCharacter(ClientSession pc){var now=NativePlayerCommit.Binding.capture(pc);return owner.get()==pc&&before.object()==now.object()&&before.source().equals(now.source())&&before.account().equals(now.account())&&before.selected().equals(now.selected())&&!(boolean)CombatAccess.field(pc,"sourceItemDisconnected");}
 }
 record Viewer(WeakReference<ClientSession> receiver,NativePlayerCommit.Binding binding){}
 record Removal(State state,Scene original,Object pose,List<Viewer> viewers) {
  void publish(){var h=original.hub.get();if(h==null)return;for(var viewer:viewers){var pc=viewer.receiver.get();if(pc==null)continue;Runnable[] completion={null};NativeSourcePlayerActionLock.run(pc,()->{if(viewer.binding.matches(pc))completion[0]=NativeRolePublication.completion(pc,()->{
   boolean valid;synchronized(h){valid=original.alive(h)&&state.actor()&&state.pose==pose&&viewer.binding.matches(pc)&&String.valueOf(CombatAccess.field(pc,"phase")).equals("WORLD")&&(int)CombatAccess.field(pc,"classicWorldMapId")==original.map&&(int)CombatAccess.field(pc,"classicWorldInstanceId")==original.instance;}
   if(valid)try{pc.nativeSelfOriginalSend(Protocol.removeObject(state.id),"S_REMOVE_OBJECT companion-portal="+state.id);}catch(IOException failure){NativeReuseLog.getLogger(NativeCompanionPlacement.class).error("Companion portal removal publication failed",failure);}
  });});if(completion[0]!=null)completion[0].run();}}
 }
 static State state(BattleCompanion pet){return (State)CombatAccess.field(pet,FIELD);}
 static GameHub hub(ClientSession pc){return (GameHub)CombatAccess.field(pc,"hub");}
 static boolean world(ClientSession pc){return String.valueOf(CombatAccess.field(pc,"phase")).equals("WORLD");}
 static int map(ClientSession pc){return (int)CombatAccess.field(pc,"classicWorldMapId");}
 static int instance(ClientSession pc){return (int)CombatAccess.field(pc,"classicWorldInstanceId");}
 static List<BattleCompanion> pets(ClientSession pc){var list=NativeBattleTamedLifecycle.pets(pc);synchronized(list){return new ArrayList<>(list);}}
 static Transition begin(ClientSession pc,Object capability){
  if(!world(pc))return null;var h=hub(pc);Transition[] result={null};NativeSourcePlayerActionLock.run(pc,()->{synchronized(h){
   if((boolean)CombatAccess.field(h,"nativeStaticMotionClosed")||!NativeCompanionCommands.registered(pc)||!Thread.holdsLock(CombatAccess.field(pc,"classicWorldPortalLifecycleLock")))return;
   var location=h.classicWorldLocation(pc);if(location==null||location.mapId()!=map(pc)||location.instanceId()!=instance(pc))return;
   var transition=new Transition(pc,capability);var original=new Scene(h,location.mapId(),location.instanceId());
   for(var pet:pets(pc))synchronized(pet){if(!pet.snapshot().kind().classicWorldEligible())continue;var s=state(pet);if(s==null){s=new State(pc,pet,original);CombatAccess.set(pet,FIELD,s);}if(!s.role(pc)||!s.scene.alive(h))continue;s.pending=transition;transition.states.add(s);}
   result[0]=transition;
  }});return result[0];
 }
 static void portal(ClientSession pc,Protocol.PortalDestination destination,Object capability,NativeSelfCastAction.Task body)throws IOException{
  if(String.valueOf(CombatAccess.field(pc,"phase")).equals("BATTLE"))NativeSourcePlayerActionLock.run(pc,()->{synchronized(hub(pc)){for(var pet:pets(pc))synchronized(pet){CombatAccess.set(pet,FIELD,null);}}});
  var transition=begin(pc,capability);try{body.run();}finally{if(transition!=null)NativeSourcePlayerActionLock.run(pc,()->{synchronized(hub(pc)){transition.active=false;for(var s:transition.states)synchronized(s.pet){if(s.pending!=transition)continue;s.pending=null;if(transition.character(pc)&&s.owned(pc)&&world(pc)&&NativeCompanionCommands.registered(pc))s.master=NativePlayerCommit.Binding.capture(pc);}}});}
 }
 static List<BattleCompanion.Snapshot> snapshots(ClientSession owner,List<BattleCompanion.Snapshot> original){if(original.isEmpty())return original;var h=hub(owner);synchronized(h){return original.stream().filter(snap->{var pet=(BattleCompanion)CombatAccess.call(owner,"ownedCompanion","(J)L"+P+"BattleCompanion;",snap.objectId());if(pet==null)return false;var s=state(pet);return s==null||s.role(owner)&&s.scene.alive(h)&&s.scene.at(map(owner),instance(owner));}).toList();}}
 static List<BattleCompanion.Snapshot> departing(ClientSession owner){return owner.classicWorldCompanionSnapshots().stream().filter(snap->{var pet=(BattleCompanion)CombatAccess.call(owner,"ownedCompanion","(J)L"+P+"BattleCompanion;",snap.objectId());var s=pet==null?null:state(pet);return s==null||s.pending==null;}).toList();}
 static List<BattleCompanion.Snapshot> inScene(ClientSession owner,int map,int instance){var h=hub(owner);synchronized(h){var result=new ArrayList<BattleCompanion.Snapshot>();for(var pet:pets(owner)){var s=state(pet);if(s!=null&&s.role(owner)&&s.scene.at(map,instance)&&s.scene.alive(h))result.add(pet.snapshot());else if(s==null&&world(owner)&&map(owner)==map&&instance(owner)==instance&&pet.snapshot().kind().classicWorldEligible())result.add(pet.snapshot());}return List.copyOf(result);}}
 @SuppressWarnings("unchecked") static List<ClientSession> owners(GameHub h,int map,int instance){synchronized(h){var result=new ArrayList<ClientSession>();for(var pc:(Set<ClientSession>)CombatAccess.field(h,"connectedSessions"))if(!inScene(pc,map,instance).isEmpty())result.add(pc);return result;}}
 static boolean occupied(ClientSession owner,Scene destination,int x,int y,long self){var h=hub(owner);
  if((int)CombatAccess.field(owner,"positionX")==x&&(int)CombatAccess.field(owner,"positionY")==y)return true;
  if((boolean)CombatAccess.call(owner,"classicWorldCompanionTileOccupied","(IIJJ)Z",x,y,self,0L))return true;
  for(var pc:owners(h,destination.map,destination.instance))for(var pet:inScene(pc,destination.map,destination.instance))if(pet.objectId()!=self&&pet.active()&&pet.x()==x&&pet.y()==y)return true;return false;
 }
 static boolean exit(ClassicWorldMapCatalog.ClassicMap map,int x,int y){for(int heading=0;heading<8;heading++)if(map.canStep(x,y,x+NativeStaticNpcMotion.DX[heading],y+NativeStaticNpcMotion.DY[heading]))return true;return false;}
 static ArenaMapCatalog.Point randomCenter(ClassicWorldMapCatalog.ClassicMap map,int x,int y){int loX=Math.max(map.startX(),x-3),hiX=Math.min(map.startX()+map.width(),x+3),loY=Math.max(map.startY(),y-3),hiY=Math.min(map.startY()+map.height(),y+3);for(int trial=0;trial<40;trial++){int nx=loX+ThreadLocalRandom.current().nextInt(hiX-loX+1),ny=loY+ThreadLocalRandom.current().nextInt(hiY-loY+1);if(map.standingPassable(nx,ny))return new ArenaMapCatalog.Point(nx,ny);}return new ArenaMapCatalog.Point(x,y);}
 static void relocate(ClientSession pc,ClassicWorldMapCatalog.ClassicMap map){var h=hub(pc);var removals=new ArrayList<Removal>();boolean[] handled={false};
  NativeSourcePlayerActionLock.run(pc,()->{synchronized(h){var destination=new Scene(h,map(pc),instance(pc));for(var pet:pets(pc))synchronized(pet){var s=state(pet);if(s==null||s.pending==null)continue;handled[0]=true;var t=s.pending;if(!t.active||!t.character(pc)||!Thread.holdsLock(CombatAccess.field(pc,"classicWorldPortalLifecycleLock"))||!s.owned(pc)||!s.scene.alive(h)||!pet.snapshot().active())continue;
   Scene original=s.scene;var viewers=h.classicWorldMembers(original.map,original.instance).stream().map(receiver->new Viewer(new WeakReference<>(receiver),NativePlayerCommit.Binding.capture(receiver))).toList();var center=randomCenter(map,(int)CombatAccess.field(pc,"positionX"),(int)CombatAccess.field(pc,"positionY"));
   var landed=NativeCompanionLanding.find((x,y)->{if(!t.active||!t.character(pc)||!s.owned(pc)||!original.alive(h)||!destination.alive(h)||!map.standingPassable(x,y)||!exit(map,x,y)||occupied(pc,destination,x,y,s.id))return false;pet.relocate(x,y);s.scene=destination;s.master=NativePlayerCommit.Binding.capture(pc);s.pose=new Object();return true;},center.x(),center.y());
   if(landed!=null)removals.add(new Removal(s,original,s.pose,viewers));
  }}});for(var removal:removals)removal.publish();if(!handled[0])CombatAccess.call(pc,"nativePlacementLegacyRelocate","(L"+P+"ClassicWorldMapCatalog$ClassicMap;)V",map);
 }
 static NativeSummonGroundTransfer.Stage ground(ClientSession pc,BattleCompanion pet,NativeCompanionCallbacks.Stamp original){var s=state(pet);if(s==null)return null;var h=hub(pc);synchronized(h){synchronized(pet){if(!pc.nativePlayerActionLock().isHeldByCurrentThread()||!s.owned(pc)||!s.scene.alive(h)||pet.snapshot().active()||pet.snapshot().kind()!=BattleCompanion.Kind.SUMMON||original!=null&&(original.companion!=s.id||original.life!=NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(pet,"timedStatus"),false)))return null;var b=s.master;var origin=new NativePlayerCommit.Binding(b.owner(),b.object(),b.source(),b.account(),b.selected(),"WORLD",0,s.scene.map,s.scene.instance);var stamp=NativeCompanionCallbacks.capture(origin,pet);return new NativeSummonGroundTransfer.Stage(h,pet,stamp,s.scene.population.get(),0,pet.snapshot().x(),pet.snapshot().y());}}}
 static boolean ownerReturn(ClientSession pc,BattleCompanion pet){var s=state(pet);return s==null||s.role(pc);}
 static BattleCompanion inventoryPet(){var target=NativeReturnNatureMaterialAction.ownerSlot().get();return target==null?null:(BattleCompanion)CombatAccess.field(target,"pet");}
 static Object groundField(Object value,String name){if(value instanceof ClientSession pc){var pet=inventoryPet();var s=pet==null?null:state(pet);if(s!=null&&s.owned(pc)&&s.scene.alive(hub(pc))){if(name.equals("classicWorldMapId"))return s.scene.map;if(name.equals("classicWorldInstanceId"))return s.scene.instance;}}return CombatAccess.field(value,name);}
 static NativeSummonGroundTransfer.Stage publicationStage(ClientSession owner,State s){var h=hub(owner);var b=s.master;var binding=new NativePlayerCommit.Binding(b.owner(),b.object(),b.source(),b.account(),b.selected(),"WORLD",0,s.scene.map,s.scene.instance);return new NativeSummonGroundTransfer.Stage(h,s.pet,NativeCompanionCallbacks.capture(binding,s.pet),s.scene.population.get(),0,s.pet.snapshot().x(),s.pet.snapshot().y());}
 static void groundPublication(ClientSession owner,ClassicWorldGroundItems.Item item,String note){var pet=inventoryPet();var s=pet==null?null:state(pet);if(s==null||!world(owner)||s.scene.at(map(owner),instance(owner))){CombatAccess.call(owner,"broadcastClassicWorldGroundItem","(L"+P+"ClassicWorldGroundItems$Item;Ljava/lang/String;)V",item,note);return;}var h=hub(owner);NativeSummonGroundTransfer.Stage stage;synchronized(h){synchronized(pet){if(s.owner.get()!=owner||!s.actor()||!s.master.matches(owner)||!s.scene.alive(h))return;stage=publicationStage(owner,s);}}stage.publish(h,item);}
 static void natureRemoval(ClientSession owner,BattleCompanion.Snapshot snapshot,int gfx,String note){var pet=inventoryPet();var s=pet==null?null:state(pet);if(s==null||!world(owner)||s.scene.at(map(owner),instance(owner))){CombatAccess.call(owner,"nativePlacementLegacyNatureRemoval","(L"+P+"BattleCompanion$Snapshot;ILjava/lang/String;)V",snapshot,gfx,note);return;}var h=hub(owner);NativeSummonGroundTransfer.Stage stage;synchronized(h){synchronized(pet){if(s.owner.get()!=owner||!s.actor()||!s.master.matches(owner)||!s.scene.alive(h)||pet.snapshot().active()||!pet.carriedItems().isEmpty())return;stage=publicationStage(owner,s);stage.deleted=true;stage.deletionLife=NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(pet,"timedStatus"),false);}}if(gfx>0)stage.publishPacket(h,null,Protocol.effectObject(snapshot.objectId(),gfx),true);stage.publishPacket(h,null,Protocol.removeObject(snapshot.objectId()),true);}
 private NativeCompanionPlacement(){}
}

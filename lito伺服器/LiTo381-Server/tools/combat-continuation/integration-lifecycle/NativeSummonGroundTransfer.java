package local.lito381.server;
import java.io.*;import java.lang.ref.WeakReference;import java.util.*;
/** Source worker deletion's no-player-inventory primitive. Retirement ownership is wired separately. */
final class NativeSummonGroundTransfer {
 static final String P="local/lito381/server/";
 static final class Stage {
  final WeakReference<GameHub> hub;final WeakReference<Object> scene,match,rules;final boolean world,hadScene;
  final int map,instance,x,y,force;final long room;final BattleCompanion pet;final Object bag;
  final NativeCompanionCallbacks.Stamp stamp;boolean transferred,deleted;Object deletionLife;
  Stage(GameHub h,ClientSession owner,BattleCompanion pet,NativeCompanionCallbacks.Stamp stamp,Object scene,int force){
   this(h,pet,stamp,scene,force,pet.snapshot().x(),pet.snapshot().y());
  }
  Stage(GameHub h,BattleCompanion pet,NativeCompanionCallbacks.Stamp stamp,Object scene,int force,int x,int y){
   hub=new WeakReference<>(h);this.pet=pet;bag=CombatAccess.field(pet,"carriedItems");this.stamp=stamp;world=stamp.master.phase().equals("WORLD");map=stamp.master.map();instance=stamp.master.instance();room=stamp.master.room();this.x=x;this.y=y;this.force=force;hadScene=scene!=null;this.scene=new WeakReference<>(scene);var m=world?null:CombatAccess.field(scene,"match");match=new WeakReference<>(m);rules=new WeakReference<>(world?null:CombatAccess.field(m,"rules"));
  }
  boolean scene(GameHub h){
   if(h==null||(boolean)CombatAccess.field(h,"nativeStaticMotionClosed"))return false;
   if(world){Object current=NativeCancellationNpcMaterialAction.population(h,map,instance);return current==scene.get()&&(!hadScene||current!=null);}
   Object current=((Map<?,?>)CombatAccess.field(h,"rooms")).get(room),expectedMatch=match.get(),expectedRules=rules.get();if(current==null||expectedMatch==null||expectedRules==null||current!=scene.get()||CombatAccess.field(current,"match")!=expectedMatch||CombatAccess.field(expectedMatch,"rules")!=expectedRules||!(boolean)CombatAccess.field(current,"battleStarted"))return false;
   int mapId=((BattleRules.RuleSet)expectedRules).mapId();return ArenaMapCatalog.forMap(mapId).contains(x,y)
    &&!((BattleDoor)CombatAccess.field(current,"door")).blocks(x,y)&&!((BattleDoor)CombatAccess.field(current,"siegeGate")).blocks(x,y)
    &&!((BattleGuardianTowers)CombatAccess.field(current,"guardianTowers")).blocks(x,y)
    &&!((BattleQuestEncounter)CombatAccess.field(current,"questEncounter")).blocks(force,x,y)&&!((BattleTimeAttackEncounter)CombatAccess.field(current,"timeAttackEncounter")).blocks(x,y);
  }
  boolean actor(){return !pet.snapshot().active()&&pet.snapshot().kind()==BattleCompanion.Kind.SUMMON&&pet.snapshot().objectId()==stamp.companion&&CombatAccess.field(pet,"carriedItems")==bag&&stamp.life==NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(pet,"timedStatus"),false);}
  boolean living(){var h=hub.get();if(h==null)return false;synchronized(h){synchronized(pet){if((boolean)CombatAccess.field(h,"nativeStaticMotionClosed")||!actor())return false;if(world){Object current=NativeCancellationNpcMaterialAction.population(h,map,instance);return current==scene.get()&&(!hadScene||current!=null);}var r=((Map<?,?>)CombatAccess.field(h,"rooms")).get(room);var m=match.get();return r!=null&&r==scene.get()&&m!=null&&CombatAccess.field(r,"match")==m&&rules.get()!=null&&CombatAccess.field(m,"rules")==rules.get()&&(boolean)CombatAccess.field(r,"battleStarted");}}}
  boolean transfer(){
   var h=hub.get();if(h==null)return false;var moved=new ArrayList<Object>();
   synchronized(h){synchronized(pet){
    if(transferred)return true;if(!scene(h)||!actor())return false;var items=pet.carriedItems();
    try{reserve(h,items,0,moved,()->{if(!scene(h)||!actor()||!pet.carriedItems().equals(items))throw new IOException("summon worker ground claim retired");for(var item:items)if(!pet.removeCarriedItem(item.objectId(),item.quantity()))throw new IllegalStateException("validated canonical summon bag changed");transferred=true;});}
    catch(IOException|RuntimeException failure){NativeReuseLog.getLogger(NativeSummonGroundTransfer.class).error("Summon worker ground transfer failed; canonical bag retained",failure);return false;}
   }}
   for(var item:moved)publish(h,item);return true;
  }
  void reserve(GameHub h,List<RuntimeInventory.Item> items,int at,List<Object> moved,DurableCommit commit)throws IOException{
   if(at==items.size()){commit.commit();return;}var item=items.get(at);moved.add(null);DurableCommit next=()->reserve(h,items,at+1,moved,commit);
   Object ground=world?h.putClassicWorldGroundItemPersisted(map,instance,item,item.quantity(),x,y,pet.snapshot().npcId(),next):((BattleGroundItems)CombatAccess.field(scene.get(),"groundItems")).putPersisted(item,item.quantity(),x,y,next);
   if(ground==null)throw new IOException("summon worker ground reservation rejected");moved.set(at,ground);
  }
  long id(Object item){return world?((ClassicWorldGroundItems.Item)item).objectId():((BattleGroundItems.Item)item).objectId();}
  boolean present(GameHub h,Object item){return scene(h)&&(world?((ClassicWorldGroundItems)CombatAccess.field(h,"classicWorldGroundItems")).item(map,instance,id(item))==item:((BattleGroundItems)CombatAccess.field(scene.get(),"groundItems")).item(id(item))==item);}
  boolean deletion(GameHub h){
   if(!scene(h)||!deleted||pet.snapshot().active()||deletionLife!=NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(pet,"timedStatus"),false))return false;
   var members=world?h.classicWorldMembers(map,instance):h.roomMembers(room);for(var pc:members){var current=CombatAccess.call(pc,"ownedCompanion","(J)L"+P+"BattleCompanion;",stamp.companion);if(current!=null&&current!=pet)return false;}
   if(world){var population=(ClassicWorldMonsterPopulation)scene.get();if(population!=null&&NativeCancellationNpcMaterialAction.actor(population,stamp.companion)!=null)return false;}
   else if(NativeBattleTamedLifecycle.actor(CombatAccess.field(scene.get(),"questEncounter"),1,stamp.companion)!=null||NativeBattleTamedLifecycle.actor(CombatAccess.field(scene.get(),"timeAttackEncounter"),2,stamp.companion)!=null)return false;
   return true;
  }
  void finishDeletion(){
   var h=hub.get();if(h==null)return;synchronized(h){synchronized(pet){if(deleted)return;if(!transferred||!scene(h)||!actor()||!pet.carriedItems().isEmpty())return;deleted=true;var pending=NativeSummonTerminalLifecycle.pending(pet);if(pending!=null&&pending.life==stamp.life)CombatAccess.set(pet,NativeSummonTerminalLifecycle.PENDING,null);NativeCompanionCallbacks.advance(pet);deletionLife=NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(pet,"timedStatus"),false);}}
   publishPacket(h,null,Protocol.effectObject(stamp.companion,169),true);publishPacket(h,null,Protocol.removeObject(stamp.companion),true);
  }
  boolean viewer(GameHub h,ClientSession pc){if((boolean)CombatAccess.field(pc,"sourceItemDisconnected"))return false;var phase=String.valueOf(CombatAccess.field(pc,"phase"));return world?phase.equals("WORLD")&&(int)CombatAccess.field(pc,"classicWorldMapId")==map&&(int)CombatAccess.field(pc,"classicWorldInstanceId")==instance&&NativeCompanionCommands.registered(pc):phase.equals("BATTLE")&&(long)CombatAccess.field(pc,"currentRoomId")==room&&h.roomMembers(room).stream().anyMatch(p->p==pc);}
  void publish(GameHub h,Object item){
   publishPacket(h,item,world?packet((ClassicWorldGroundItems.Item)item):packet((BattleGroundItems.Item)item),false);
  }
  void publishPacket(GameHub h,Object item,byte[] packet,boolean remove){
   List<ClientSession> viewers;var bindings=new IdentityHashMap<ClientSession,NativePlayerCommit.Binding>();
   synchronized(h){if(!(remove?deletion(h):present(h,item)))return;viewers=world?h.classicWorldMembers(map,instance):h.roomMembers(room);for(var pc:viewers)bindings.put(pc,NativePlayerCommit.Binding.capture(pc));}
   for(var pc:viewers){var binding=bindings.get(pc);Runnable task=()->{Runnable[] completion={null};NativeSourcePlayerActionLock.run(pc,()->{if(binding.matches(pc))completion[0]=NativeRolePublication.completion(pc,()->{
    boolean valid;synchronized(h){valid=binding.matches(pc)&&viewer(h,pc)&&(remove?deletion(h):present(h,item))&&BattleSpellCatalog.isInNativeScreen((int)CombatAccess.field(pc,"positionX"),(int)CombatAccess.field(pc,"positionY"),x,y);}if(valid)try{pc.nativeSelfOriginalSend(packet,remove?"summon-worker-delete="+stamp.companion:"S_PUT_OBJECT summon-worker-ground="+id(item));}catch(IOException error){NativeReuseLog.getLogger(NativeSummonGroundTransfer.class).error("Summon worker ground publication failed; committed item retained",error);}
   });});if(completion[0]!=null)completion[0].run();};if(NativeLightAction.currentMaterialProducer()!=null)NativeLightAction.deferScoped(task::run);else task.run();}
  }
 }
 static byte[] packet(ClassicWorldGroundItems.Item item){return Protocol.groundItem(item.objectId(),item.x(),item.y(),item.definition().groundGfx(),item.quantity(),item.displayName());}
 static byte[] packet(BattleGroundItems.Item item){return Protocol.groundItem(item.objectId(),item.x(),item.y(),item.definition().groundGfx(),item.quantity(),item.displayName());}
 static Stage capture(ClientSession owner,BattleCompanion pet){
  if(owner==null||pet==null||!owner.nativePlayerActionLock().isHeldByCurrentThread())return null;var h=(GameHub)CombatAccess.field(owner,"hub");
  synchronized(h){synchronized(pet){if((boolean)CombatAccess.field(h,"nativeStaticMotionClosed")||(boolean)CombatAccess.field(owner,"sourceItemDisconnected")||!NativeSummonTerminalLifecycle.registered(owner,h)||pet.snapshot().active()||pet.snapshot().kind()!=BattleCompanion.Kind.SUMMON||CombatAccess.call(owner,"ownedCompanion","(J)L"+P+"BattleCompanion;",pet.snapshot().objectId())!=pet)return null;
   var stamp=NativeCompanionCallbacks.capture(NativePlayerCommit.Binding.capture(owner),pet);boolean world=stamp.master.phase().equals("WORLD");Object scene=world?NativeCancellationNpcMaterialAction.population(h,stamp.master.map(),stamp.master.instance()):CombatAccess.call(h,"validatedGroundRoom","(JL"+P+"ClientSession;II)L"+P+"GameHub$Room;",stamp.master.room(),owner,pet.snapshot().x(),pet.snapshot().y());if(!world&&scene==null)return null;int force=world?0:force(scene,owner);return new Stage(h,owner,pet,stamp,scene,force);
  }}
 }
 static int force(Object room,ClientSession owner){try{var method=GameHub.class.getDeclaredMethod("forceOf",room.getClass(),ClientSession.class);method.setAccessible(true);return (int)method.invoke(null,room,owner);}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}}
 static Stage captureFallback(ClientSession owner,BattleCompanion pet,NativeCompanionCallbacks.Stamp original,boolean atOwner){
  if(owner==null||pet==null||!owner.nativePlayerActionLock().isHeldByCurrentThread()||original==null||!original.master.matches(owner))return null;var h=(GameHub)CombatAccess.field(owner,"hub");
  synchronized(h){synchronized(pet){if((boolean)CombatAccess.field(h,"nativeStaticMotionClosed")||!NativeSummonTerminalLifecycle.registered(owner,h)||pet.snapshot().active()||pet.snapshot().kind()!=BattleCompanion.Kind.SUMMON||original.life!=NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(pet,"timedStatus"),false)||CombatAccess.call(owner,"ownedCompanion","(J)L"+P+"BattleCompanion;",original.companion)!=pet)return null;
   boolean world=original.master.phase().equals("WORLD");Object scene=world?NativeCancellationNpcMaterialAction.population(h,original.master.map(),original.master.instance()):((Map<?,?>)CombatAccess.field(h,"rooms")).get(original.master.room());if(!world&&(scene==null||!(boolean)CombatAccess.field(scene,"battleStarted")||CombatAccess.field(scene,"match")==null))return null;return new Stage(h,pet,original,scene,world?0:force(scene,owner),atOwner?(int)CombatAccess.field(owner,"positionX"):pet.snapshot().x(),atOwner?(int)CombatAccess.field(owner,"positionY"):pet.snapshot().y());
  }}
 }
 private NativeSummonGroundTransfer(){}
}

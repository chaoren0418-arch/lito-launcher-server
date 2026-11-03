package local.lito381.server;
import java.io.*;import java.util.*;
/** Skill145's already-admitted SUMMON return; never authorizes a new producer. */
final class NativeReturnNatureTransfer {
 private record Scope(ClientSession caster,ClientSession owner,BattleCompanion pet,
                      NativePlayerCommit.Binding binding,NativeCastMaterial.Cast cast){}
 private static final ThreadLocal<Scope> current=new ThreadLocal<>();
 static NativeCastMaterial.Cast material(ClientSession pc){var s=current.get();if(s==null){current.remove();return null;}return s.owner==pc?s.cast:null;}
 static boolean retained(ClientSession pc){
  var s=current.get();if(s==null){current.remove();return false;}
  return s.owner==pc&&s.caster!=pc&&s.binding.matches(pc)&&pc.nativePlayerActionLock().isHeldByCurrentThread()
   &&s.caster.nativePlayerActionLock().isHeldByCurrentThread()&&NativeReturnNatureMaterialAction.caster(pc,s.pet)==s.caster
   &&s.cast.operation!=null&&NativeLightAction.retainedMaterial(s.caster);
 }
 static boolean release(ClientSession owner,BattleCompanion pet,BattleCompanion.Snapshot expected,int effect)throws IOException{
  if(pet==null||expected==null)return false;
  var caster=NativeReturnNatureMaterialAction.caster(owner,pet);if(caster==null)return false;
  // TAMED encounter releases have a different origin/ground transaction contract.
  // Preserve those paths here; they remain explicitly outside this SUMMON fix.
  if(expected.kind()!=BattleCompanion.Kind.SUMMON||pet.carriedItems().isEmpty())return original(owner,pet,expected,effect);
  var life=NativePlayerLifetime.of(owner);
  synchronized(life.admission){
   if(!NativeLightAction.retainedMaterial(caster))return false;
   var cast=new NativeCastMaterial.Cast(145);var scope=new Scope(caster,owner,pet,NativePlayerCommit.Binding.capture(owner),cast);
   var previous=current.get();current.set(scope);
   try{
    cast.live=owner.nativeDeInventory(0);cast.owner=NativeDeItemApplication.publicationOwner(owner);
    NativeDeItemTransaction.Operation<Boolean> operation=()->{
     if(!NativeCastMaterial.active(owner,cast)||!scope.binding.matches(owner))throw new IOException("companion return owner retired");
     synchronized(pet){
      if(NativeReturnNatureMaterialAction.caster(owner,pet)!=caster||!pet.snapshot().active())return false;
      transfer(owner,pet,cast);
      return original(owner,pet,expected,effect);
     }
    };cast.operation=operation;
    try{return NativeDeItemTransaction.call(owner,operation);}
    catch(IOException failure){NativeReuseLog.getLogger(NativeReturnNatureTransfer.class).error("Return to Nature owner transfer failed; summon retained",failure);return false;}
    finally{cast.operation=null;cast.transaction=false;cast.live=null;cast.staged=null;cast.owner=null;}
   }finally{if(previous==null)current.remove();else current.set(previous);}
  }
 }
 private static boolean original(ClientSession owner,BattleCompanion pet,BattleCompanion.Snapshot expected,int effect)throws IOException{
  try{return (boolean)CombatAccess.call(owner,"releaseCompanionToNature","(Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;I)Z",pet,expected,effect);}
  catch(RuntimeException failure){Throwable root=failure;while(root.getCause()!=null)root=root.getCause();if(root instanceof IOException io)throw io;throw failure;}
 }
 private record Overflow(RuntimeInventory.Item item,boolean capacity){}
 private static void transfer(ClientSession owner,BattleCompanion pet,NativeCastMaterial.Cast cast)throws IOException{
  var live=owner.nativeDeInventory(0);var next=live.copy();cast.staged=next;
  var carried=pet.carriedItems();var overflow=new ArrayList<Overflow>();boolean added=false;
  for(var item:carried){
   var admitted=next.copy();cast.staged=admitted;boolean capacity=admitted.add(item,item.quantity());
   boolean weight=capacity&&(boolean)CombatAccess.call(owner,"permitsInventoryAddition","(Llocal/lito381/server/RuntimeInventory;Llocal/lito381/server/InventoryCatalog$Definition;J)Z",next,item.definition(),(long)item.quantity());
   if(capacity&&weight){next=admitted;added=true;}else overflow.add(new Overflow(item,capacity));
   cast.staged=next;
  }
  if(next.nativeDeItems() instanceof NativeDeItemInventory inventory)inventory.validateReplacement(live);
  var beforeWire=NativeDeItemTransaction.wireCopy(live);var afterWire=NativeDeItemTransaction.wireCopy(next);
  var state=(GameStateStore)CombatAccess.field(owner,"state");var key=(String)CombatAccess.call(owner,"stateCharacter","()Ljava/lang/String;");long points=((Number)CombatAccess.field(owner,"shopPoints")).longValue();
  var finalNext=next;boolean hasAdded=added;var grounds=new Object[overflow.size()];
  // Each ground reservation rolls back if a nested reservation or memory commit fails.
  // Ground stores keep their own monitor until this callback completes.
  DurableCommit commit=()->{synchronized(state){
   var old=state.economy(key);
   if(hasAdded)state.saveEconomyState(key,points,finalNext.catalogCounts(),finalNext.catalogItemStates());
   try{NativeDeItemApplication.replace(owner,finalNext,0);}
   catch(RuntimeException|Error failure){if(hasAdded)try{state.saveEconomyState(key,old.points(),old.inventory(),old.itemStates());}catch(Throwable rollback){failure.addSuppressed(rollback);}throw failure;}
   for(var item:carried)if(!pet.removeCarriedItem(item.objectId(),item.quantity()))throw new IllegalStateException("validated companion bag changed under its monitor");
  }};
  reserveGround(owner,pet.snapshot(),overflow,grounds,0,commit);
  CombatAccess.call(owner,"sendInventoryDelta","(Llocal/lito381/server/RuntimeInventory;Llocal/lito381/server/RuntimeInventory;)V",beforeWire,afterWire);
  boolean world=String.valueOf(CombatAccess.field(owner,"phase")).equals("WORLD");
  for(int i=0;i<overflow.size();i++){
   var reason=world?"classic-world-pet-collect":"companion-collect";
   CombatAccess.call(owner,overflow.get(i).capacity?"sendInventoryWeightOver":"sendInventoryCapacityOver","(Ljava/lang/String;)V",reason);
   CombatAccess.call(owner,world?"broadcastClassicWorldGroundItem":"broadcastGroundItem",world?"(Llocal/lito381/server/ClassicWorldGroundItems$Item;Ljava/lang/String;)V":"(Llocal/lito381/server/BattleGroundItems$Item;Ljava/lang/String;)V",grounds[i],world?"classic-world-pet-collect-overflow":"companion-getitem-overflow");
  }
 }
 private static void reserveGround(ClientSession owner,BattleCompanion.Snapshot pet,List<Overflow> overflow,Object[] grounds,int index,DurableCommit commit)throws IOException{
  if(index==overflow.size()){commit.commit();return;}
  var hub=(GameHub)CombatAccess.field(owner,"hub");var item=overflow.get(index).item;
  DurableCommit rest=()->reserveGround(owner,pet,overflow,grounds,index+1,commit);
  Object ground=String.valueOf(CombatAccess.field(owner,"phase")).equals("WORLD")
   ?hub.putClassicWorldGroundItemPersisted((int)CombatAccess.field(owner,"classicWorldMapId"),(int)CombatAccess.field(owner,"classicWorldInstanceId"),item,item.quantity(),pet.x(),pet.y(),pet.npcId(),rest)
   :hub.putGroundItemPersisted((long)CombatAccess.field(owner,"currentRoomId"),owner,item,item.quantity(),pet.x(),pet.y(),rest);
  if(ground==null)throw new IOException("companion return ground reservation rejected");grounds[index]=ground;
 }
 private NativeReturnNatureTransfer(){}
}

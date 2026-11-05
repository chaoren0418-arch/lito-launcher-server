// ported from candidate 50635f7e; three-way baseline: base-0.6.71.jar and src/main
// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/build-repro/inputs/original/NativeCastMaterial.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatDatabase;
import local.lito381.server.GameStateStore;
import local.lito381.server.NativeCastMaterialPolicy;
import local.lito381.server.NativeCubeCastAction;
import local.lito381.server.NativeDeItemApplication;
import local.lito381.server.NativeDeItemInventory;
import local.lito381.server.NativeDeItemTransaction;
import local.lito381.server.NativeDeWorldKinds;
import local.lito381.server.NativeLightAction;
import local.lito381.server.NativeMaterialItem;
import local.lito381.server.NativePlayerLifetime;
import local.lito381.server.NativeReuseLog;
import local.lito381.server.NativeSourceMaterialPlan;
import local.lito381.server.Protocol;
import local.lito381.server.RuntimeInventory;
import java.io.*;import java.util.*;
/** Source material ordering through the actual native memory/item transaction authority. */
final class NativeCastMaterial {
    static final class Cast {
        Effect effect;
        boolean effectResult;
        final int skill;
        boolean resolved;
        boolean consumed;
        boolean bypass;
        boolean transaction;
        boolean placement;
        boolean placementBlocked;
        RuntimeInventory before;
        RuntimeInventory after;
        RuntimeInventory live;
        RuntimeInventory staged;
        NativeDeItemApplication.Owner owner;
        NativeDeItemTransaction.Operation<?> operation;

        Cast(int n) {
            this.skill = n;
        }
    }
 static boolean retained(ClientSession pc,NativePlayerLifetime life){return Thread.holdsLock(life.admission)&&pc.nativePlayerActionLock().isHeldByCurrentThread()&&NativeLightAction.retainedMaterial(pc);}
    static boolean active(ClientSession clientSession, Cast cast) {
        return NativeLightAction.validTargets(clientSession) && cast != null && cast.transaction && cast.operation != null && NativeCastMaterial.retained(clientSession, NativePlayerLifetime.of(clientSession)) && cast.owner != null && cast.owner.active && cast.owner.matches(clientSession) && CombatAccess.field(clientSession, "sourceItemOwner") == cast.owner && (clientSession.nativeDeInventory(0) == cast.live || clientSession.nativeDeInventory(0) == cast.staged);
    }
 static boolean ownerLifetime(NativePlayerLifetime life,NativeDeItemApplication app,ClientSession pc){if(life.admits())return true;var cast=NativeLightAction.currentMaterial(pc);return active(pc,cast)&&cast.owner.application.get()==app&&app.life==life&&!app.closed;}
 static boolean adoptLifetime(NativePlayerLifetime life,NativeDeItemApplication.Owner owner,RuntimeInventory inventory){if(life.admits())return true;var pc=owner.session.get();if(pc==null)return false;var cast=NativeLightAction.currentMaterial(pc);return active(pc,cast)&&cast.owner==owner&&cast.staged==inventory&&NativePlayerLifetime.of(pc)==life;}
 static void enterTransaction(ClientSession pc,NativeDeItemTransaction.Operation<?> operation)throws IOException{var cast=NativeLightAction.currentMaterial(pc);if(cast==null)return;if(cast.transaction)throw new IOException("nested item transaction cannot inherit material cast authority");if(cast.operation==operation){if(!retained(pc,NativePlayerLifetime.of(pc)))throw new IOException("material cast receipt retired");cast.transaction=true;}}
 static void exitTransaction(ClientSession pc,NativeDeItemTransaction.Operation<?> operation){var cast=NativeLightAction.currentMaterial(pc);if(cast!=null&&cast.operation==operation)cast.transaction=false;}
 private record ItemView(RuntimeInventory.Item view,NativeDeWorldKinds.Item source,int owner) implements NativeMaterialItem {
  public Object getItem(){return source==null?view.definition():source.getItem();}
  public int getItemId(){return source==null?(int)view.definition().catalogId():source.getItemId();}
  public int get_char_objid(){return source==null?owner:source.get_char_objid();}
  public boolean isEquipped(){return source==null?view.equipped():source.isEquipped();}
  public boolean isSealed(){return source==null?view.sealed():source.isSealed();}
  public long getCount(){return source==null?view.quantity():source.getCount();}
  public boolean isStackable(){return source==null?view.definition().stackable():source.isStackable();}
  public int getEnchantLevel(){return source==null?view.enchantLevel():source.getEnchantLevel();}
  public int getId(){return source==null?(int)view.objectId():source.getId();}
 }
    static boolean commit(ClientSession clientSession, int n) {
        Cast cast = NativeLightAction.material(clientSession, n);
        int n2 = CombatDatabase.value(0, n, "NativeCastMaterial", "itemConsumeId");
        int n3 = CombatDatabase.value(0, n, "NativeCastMaterial", "itemConsumeCount");
        if (!NativeLightAction.validTargets(clientSession)) {
            return false;
        }
        if (cast.resolved) {
            return n2 == 0 || cast.bypass || cast.consumed;
        }
        try {
            boolean bl;
            bl = n2 == 0 || NativeCastMaterialPolicy.gm(clientSession);
            if (!NativeLightAction.validTargets(clientSession)) {
                return false;
            }
            if (bl) {
                cast.bypass = true;
                if (cast.effect == null) {
                    RuntimeInventory runtimeInventory;
                    cast.resolved = true;
                    cast.before = runtimeInventory = NativeDeItemTransaction.wireCopy(clientSession.nativeDeInventory(0));
                    cast.after = runtimeInventory;
                    return true;
                }
            }
            if (!cast.bypass && n3 <= 0) {
                return NativeCastMaterial.reject(clientSession);
            }
            if (cast.placement) {
                if (!NativeCastMaterial.active(clientSession, cast) || !Thread.holdsLock(CombatAccess.field(clientSession, "state"))) {
                    throw new IOException("cube material lost its exact original operation");
                }
                return NativeCastMaterial.consumeLocked(clientSession, cast, n2, n3) || NativeCastMaterial.reject(clientSession);
            }
            NativePlayerLifetime nativePlayerLifetime = NativePlayerLifetime.of(clientSession);
            Object object = nativePlayerLifetime.admission;
            synchronized (object) {
                boolean bl3;
                if (!nativePlayerLifetime.admits() && !NativeCastMaterial.retained(clientSession, nativePlayerLifetime)) {
                    return NativeCastMaterial.reject(clientSession);
                }
                cast.live = clientSession.nativeDeInventory(0);
                cast.owner = NativeDeItemApplication.publicationOwner(clientSession);
                NativeDeItemTransaction.Operation<Boolean> operation = () -> NativeCastMaterial.consumeLocked(clientSession, cast, n2, n3);
                cast.operation = operation;
                try {
                    bl3 = NativeDeItemTransaction.call(clientSession, operation);
                }
                finally {
                    cast.operation = null;
                    cast.live = null;
                    cast.staged = null;
                    cast.owner = null;
                    cast.transaction = false;
                }
                return bl3 || NativeCastMaterial.reject(clientSession);
            }
        }
        catch (IOException iOException) {
            NativeReuseLog.getLogger(NativeCastMaterial.class).error("material memory transaction failed", iOException);
            return NativeCastMaterial.reject(clientSession);
        }
    }
 static boolean reject(ClientSession pc){
  NativeLightAction.materialOutput(pc,Protocol.messageCode(299),"S_MESSAGE_CODE Source-required-material-rejected=299");return false;
 }
 static RuntimeInventory previous(ClientSession pc){var cast=NativeLightAction.currentMaterial(pc);if(cast==null||!cast.resolved||!pc.nativePlayerActionLock().isHeldByCurrentThread())throw new IllegalStateException("material not committed");return cast.before;}
 static RuntimeInventory next(ClientSession pc){var cast=NativeLightAction.currentMaterial(pc);if(cast==null||!cast.resolved||!pc.nativePlayerActionLock().isHeldByCurrentThread())throw new IllegalStateException("material not committed");return cast.after;}

 static boolean rejectPlacement(ClientSession pc,Cast cast){cast.placementBlocked=true;return reject(pc);}
 static <T>T withPlacement(ClientSession pc,int skill,NativeDeItemTransaction.Operation<T> placement,T rejected)throws IOException{
  if(!NativeCubeCastAction.supported(skill)||!pc.nativePlayerActionLock().isHeldByCurrentThread())throw new IllegalStateException("unreviewed cube placement scope");
  var cast=NativeLightAction.material(pc,skill);if(cast.transaction||cast.resolved)throw new IOException("cube placement cannot inherit another material transaction");
  try{
   // The first character-policy read and its complete JDBC cleanup happen before all placement/item/lifetime monitors.
   NativeCastMaterialPolicy.gm(pc);
   var life=NativePlayerLifetime.of(pc);synchronized(life.admission){
    if(!life.admits()&&!retained(pc,life)){rejectPlacement(pc,cast);return rejected;}
    cast.live=pc.nativeDeInventory(0);cast.owner=NativeDeItemApplication.publicationOwner(pc);
    NativeDeItemTransaction.Operation<T> operation=()->{
     if(!active(pc,cast))throw new IOException("cube canonical owner retired before original placement");
     var state=(GameStateStore)CombatAccess.field(pc,"state");synchronized(state){cast.placement=true;try{return placement.call();}finally{cast.placement=false;}}
    };cast.operation=operation;
    try{return NativeDeItemTransaction.call(pc,operation);}finally{cast.operation=null;cast.live=null;cast.staged=null;cast.owner=null;cast.transaction=false;cast.placement=false;}
   }
  }catch(IOException failure){NativeReuseLog.getLogger(NativeCastMaterial.class).error("cube placement memory transaction failed",failure);rejectPlacement(pc,cast);return rejected;}
 }
 static boolean consumeLocked(ClientSession pc,Cast cast,int item,int count)throws IOException{
     if(!active(pc,cast))throw new IOException("material canonical owner retired before transaction");
     var live=pc.nativeDeInventory(0);var views=new ArrayList<NativeMaterialItem>();var source=live.nativeDeItems();
     for(var value:live.items()){NativeDeWorldKinds.Item actor=source instanceof NativeDeItemInventory inventory?inventory.actor(value.objectId()):null;views.add(new ItemView(value,actor,(int)pc.playerObjectId()));}
     var next=live.copy();cast.staged=next;
     if(!cast.bypass){var plan=NativeSourceMaterialPlan.plan(views,(int)pc.playerObjectId(),item,count);if(!plan.allowed())return false;
     for(var debit:plan.debits()){var value=next.item(debit.objectId());if(value==null||value.quantity()!=debit.oldCount()||!next.remove(debit.objectId(),debit.count()))throw new IOException("material canonical plan changed");}}
     if(cast.effect!=null)cast.effectResult=cast.effect.apply(next);
     if(next.nativeDeItems() instanceof NativeDeItemInventory inventory){try{inventory.validateReplacement(live);}catch(IllegalStateException stale){throw new IOException("material inventory copy is stale",stale);}}
     var previousWire=NativeDeItemTransaction.wireCopy(live);var nextWire=NativeDeItemTransaction.wireCopy(next);var state=(GameStateStore)CombatAccess.field(pc,"state");var key=(String)CombatAccess.call(pc,"stateCharacter","()Ljava/lang/String;");long points=((Number)CombatAccess.field(pc,"shopPoints")).longValue();
     synchronized(state){
      var before=state.economy(key);state.saveEconomyState(key,points,next.catalogCounts(),next.catalogItemStates());
      try{if(!NativeLightAction.validTargets(pc))throw new IOException("material pair retired before install");NativeDeItemApplication.replace(pc,next,0);}catch(IOException|RuntimeException|Error failure){try{state.saveEconomyState(key,before.points(),before.inventory(),before.itemStates());}catch(Throwable rollback){failure.addSuppressed(rollback);}throw failure;}
     }
     cast.before=previousWire;cast.after=nextWire;cast.consumed=true;cast.resolved=true;return true;

 }
 private NativeCastMaterial(){}
    static interface Effect {
        public boolean apply(RuntimeInventory var1) throws IOException;
    }
}

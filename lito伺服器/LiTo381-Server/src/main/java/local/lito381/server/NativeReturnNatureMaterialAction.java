// ported from candidate 50635f7e; B07 三方移植（base-0.6.71 / candidate / src/main）。
// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/return-nature-material/NativeReturnNatureMaterialAction.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;
import java.util.List;
/** Bind Return to Nature before the native dispatcher can wait on a player lock. */
final class NativeReturnNatureMaterialAction {
 @AsRecord(components={"caster","owner","pet","stamp"})
    private static final class Target {
        private final ClientSession caster;
        private final ClientSession owner;
        private final BattleCompanion pet;
        private final NativeCompanionCallbacks.Stamp stamp;
        private boolean nativeSummonOwnerContext;
        private Object nativeSummonPublication;
        private boolean nativeSummonDeletionContext;

        private Target(ClientSession clientSession, ClientSession clientSession2, BattleCompanion battleCompanion, NativeCompanionCallbacks.Stamp stamp) {
            this.caster = clientSession;
            this.owner = clientSession2;
            this.pet = battleCompanion;
            this.stamp = stamp;
        }

        BattleCompanion owned() {
            return this.pet == null ? null : (BattleCompanion)CombatAccess.call(this.owner, "ownedCompanion", "(J)Llocal/lito381/server/BattleCompanion;", this.stamp.companion);
        }

        boolean valid() {
            return this.pet == null || this.stamp.master.matches(this.owner) && (Boolean)CombatAccess.field(this.owner, "sourceItemDisconnected") == false && this.owned() == this.pet && NativeCompanionCallbacks.capture((NativePlayerCommit.Binding)this.stamp.master, (BattleCompanion)this.pet).life == this.stamp.life;
        }

        boolean canPublish() {
            if (this.pet == null) {
                return true;
            }
            BattleCompanion battleCompanion = this.owned();
            return this.stamp.master.matches(this.owner) && (Boolean)CombatAccess.field(this.owner, "sourceItemDisconnected") == false && (battleCompanion == null || battleCompanion == this.pet);
        }

        public ClientSession caster() {
            return this.caster;
        }

        public ClientSession owner() {
            return this.owner;
        }

        public BattleCompanion pet() {
            return this.pet;
        }

        public NativeCompanionCallbacks.Stamp stamp() {
            return this.stamp;
        }
    }
 private static final ThreadLocal<Target> ingress=new ThreadLocal<>(),current=new ThreadLocal<>();
 private static Target target(ClientSession pc,byte[] packet){
  var request=Protocol.attackSpellRequest(packet);var hub=(GameHub)CombatAccess.field(pc,"hub");boolean world=String.valueOf(CombatAccess.field(pc,"phase")).equals("WORLD");
  List<ClientSession> players=world?(List<ClientSession>)CombatAccess.call(pc,"classicWorldMembers","(I)Ljava/util/List;",(int)CombatAccess.field(pc,"classicWorldMapId")):hub.roomCombatants((long)CombatAccess.field(pc,"currentRoomId"));
  for(var owner:NativeCompanionNegative.members(players,pc)){List<BattleCompanion.Snapshot> snapshots=world?NativeCompanionTargets.snapshots(owner,pc):owner.controlledCompanionSnapshots();
   for(var snapshot:snapshots)if(snapshot.objectId()==request.targetObjectId()){
    var pet=(BattleCompanion)CombatAccess.call(owner,"ownedCompanion","(J)Llocal/lito381/server/BattleCompanion;",request.targetObjectId());
    if(pet!=null)return new Target(pc,owner,pet,NativeCompanionCallbacks.capture(NativePlayerCommit.Binding.capture(owner),pet));
   }
  }return new Target(pc,pc,null,null);
 }
 static void nativeSceneOriginalDispatch(ClientSession pc,byte[] packet,NativeSelfCastAction.Task original)throws IOException{
  String phase=String.valueOf(CombatAccess.field(pc,"phase"));
  if(Protocol.spellSelector(packet).skillId()!=145||(!phase.equals("BATTLE")&&!phase.equals("WORLD"))||pc.selectedCharacterName().isBlank()){original.run();return;}
  var prior=ingress.get();ingress.set(target(pc,packet));try{original.run();}finally{if(prior==null)ingress.remove();else ingress.set(prior);}
 }
 static void nativeSceneOriginalCast(ClientSession pc,byte[] packet,BattleSpellCatalog.ReturnToNatureSpell spell,int roll)throws IOException{
  var inherited=ingress.get();var target=inherited!=null&&inherited.caster==pc?inherited:target(pc,packet);ingress.remove();
  try{NativeLightAction.commitPair(pc,target.owner,()->{
   if(!target.valid())return;var prior=current.get();current.set(target);
   try{pc.nativeMaterialReturnNatureBody(packet,spell,roll);}finally{if(prior==null)current.remove();else current.set(prior);}
  });}finally{if(inherited==null)ingress.remove();else ingress.set(inherited);}
 }
 static ClientSession caster(ClientSession clientSession, BattleCompanion battleCompanion) {
        Target target = current.get();
        if (target == null) {
            current.remove();
            return null;
        }
        return target.owner == clientSession && target.pet == battleCompanion && target.valid() && clientSession.nativePlayerActionLock().isHeldByCurrentThread() && target.caster.nativePlayerActionLock().isHeldByCurrentThread() ? target.caster : null;
    }
 static boolean nativeSceneOriginalAdmit(ClientSession pc,ClientSession owner,int skill,int action,int reuse){
  if(skill!=145)throw new IllegalStateException("unreviewed Return to Nature skill");var target=current.get();
  if(target==null||target.caster!=pc||target.owner!=owner||!target.valid())return false;
  return NativeLightAction.admitPair(pc,owner,skill,action,reuse);
 }
 static boolean nativeSummonOriginalCaptureOutput(ClientSession receiver,byte[] plain,String note){
  var target=current.get();if(target==null){current.remove();return false;}
  if(!note.contains("Return-to-Nature")||!(note.startsWith("S_EFFECT ")||note.startsWith("S_REMOVE_OBJECT ")||note.startsWith("S_PUT_OBJECT ")))return false;
  byte[] snapshot=plain.clone();NativeLightAction.deferScoped(()->{if(target.canPublish())receiver.nativeSelfOriginalSend(snapshot,note);});return true;
 }
 private NativeReturnNatureMaterialAction(){}

    static void dispatch(ClientSession clientSession, byte[] byArray, NativeSelfCastAction.Task task) throws IOException {
        NativeCompanionNature.dispatch(clientSession, byArray, task);
    }

    static void cast(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.ReturnToNatureSpell returnToNatureSpell, int n) throws IOException {
        NativeCompanionNature.cast(clientSession, byArray, returnToNatureSpell, n);
    }

    static boolean admit(ClientSession clientSession, ClientSession clientSession2, int n, int n2, int n3) {
        return NativeCompanionNature.admit(clientSession, clientSession2, n, n2, n3);
    }

    static boolean captureOutput(ClientSession clientSession, byte[] byArray, String string) {
        return NativeSummonOwnerLifecycle.captureOutput(clientSession, byArray, string);
    }

    static ThreadLocal ownerSlot() {
        return current;
    }

    static boolean ownerCanPublish(Object object) {
        return ((Target)object).canPublish();
    }

    static Object ownerContext(ClientSession caster,BattleCompanion pet,NativeCompanionCallbacks.Stamp stamp) {
        Target context=new Target(caster,caster,pet,stamp);context.nativeSummonOwnerContext=true;return context;
    }
    static Object ownerContext(ClientSession caster,ClientSession owner,BattleCompanion pet,NativeCompanionCallbacks.Stamp stamp) {
        Target context=new Target(caster,owner,pet,stamp);context.nativeSummonOwnerContext=true;return context;
    }
    static Object ownerDeletionContext(ClientSession caster,BattleCompanion pet,NativeCompanionCallbacks.Stamp stamp) {
        Target context=(Target)ownerContext(caster,pet,stamp);context.nativeSummonDeletionContext=true;return context;
    }
}

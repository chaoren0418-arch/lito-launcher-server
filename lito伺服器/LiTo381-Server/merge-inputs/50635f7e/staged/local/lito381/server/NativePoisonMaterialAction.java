// ported from candidate 50635f7e; B07 三方移植（base-0.6.71 / candidate / src/main）。
// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/poison-material/NativePoisonMaterialAction.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.List;
/** Preserve existing Source scope and four bodies and their target checks under ordered player locks. */
final class NativePoisonMaterialAction {
 private static ClientSession target(ClientSession pc,byte[] packet,boolean world){
  var request=Protocol.attackSpellRequest(packet);var hub=(GameHub)CombatAccess.field(pc,"hub");
  List<ClientSession> players=world?(List<ClientSession>)CombatAccess.call(pc,"classicWorldMembers","(I)Ljava/util/List;",(int)CombatAccess.field(pc,"classicWorldMapId")):hub.roomCombatants((long)CombatAccess.field(pc,"currentRoomId"));
  return players.stream().filter(p->p.playerObjectId()==request.targetObjectId()).findFirst().orElse(pc);
 }
 static void nativeSourceArenaOriginalBattle(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.PoisonSpell poisonSpell, CombatPoisonLifecycle.Scope scope) throws IOException {
        NativeLightAction.commitPair(clientSession, NativePoisonMaterialAction.target(clientSession, byArray, false), () -> {
            CombatPoisonLifecycle.Scope ownedScope = NativePoisonMaterialAction.open();
            try {
                clientSession.nativeMaterialPoisonScopeBody(byArray, poisonSpell, ownedScope);
            }
            catch (IOException | RuntimeException | Error throwable) {
                CombatPoisonLifecycle.failed(ownedScope, throwable);
                throw throwable;
            }
        });
    }
 static void poisonWorld(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.PoisonSpell poisonSpell, CombatPoisonLifecycle.Scope scope) throws IOException {
        NativeLightAction.commitPair(clientSession, NativePoisonMaterialAction.target(clientSession, byArray, true), () -> {
            CombatPoisonLifecycle.Scope ownedScope = NativePoisonMaterialAction.open();
            try {
                clientSession.nativeMaterialWorldPoisonScopeBody(byArray, poisonSpell, ownedScope);
            }
            catch (IOException | RuntimeException | Error throwable) {
                CombatPoisonLifecycle.failed(ownedScope, throwable);
                throw throwable;
            }
        });
    }
 static void paralysisBattle(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.ParalysisSpell paralysisSpell, CombatPoisonLifecycle.Scope scope) throws IOException {
        NativeLightAction.commitPair(clientSession, NativePoisonMaterialAction.target(clientSession, byArray, false), () -> {
            CombatPoisonLifecycle.Scope ownedScope = NativePoisonMaterialAction.open();
            try {
                clientSession.nativeMaterialParalysisScopeBody(byArray, paralysisSpell, ownedScope);
            }
            catch (IOException | RuntimeException | Error throwable) {
                CombatPoisonLifecycle.failed(ownedScope, throwable);
                throw throwable;
            }
        });
    }
 static void paralysisWorld(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.ParalysisSpell paralysisSpell, CombatPoisonLifecycle.Scope scope) throws IOException {
        NativeLightAction.commitPair(clientSession, NativePoisonMaterialAction.target(clientSession, byArray, true), () -> {
            CombatPoisonLifecycle.Scope ownedScope = NativePoisonMaterialAction.open();
            try {
                clientSession.nativeMaterialWorldParalysisScopeBody(byArray, paralysisSpell, ownedScope);
            }
            catch (IOException | RuntimeException | Error throwable) {
                CombatPoisonLifecycle.failed(ownedScope, throwable);
                throw throwable;
            }
        });
    }
 static boolean admit(ClientSession pc,ClientSession target,int skill,int action,int reuse){
  if(skill!=11&&skill!=33)throw new IllegalStateException("unreviewed poison/paralysis material skill");
  return NativeLightAction.admitPair(pc,target==null?pc:target,skill,action,reuse);
 }
 private NativePoisonMaterialAction(){}

    static void poisonBattle(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.PoisonSpell poisonSpell, CombatPoisonLifecycle.Scope scope) throws IOException {
        NativeSourceArenaPoison.battle(clientSession, byArray, poisonSpell, scope);
    }

    private static CombatPoisonLifecycle.Scope open() {
        CombatPoisonLifecycle.Scope scope = CombatPoisonLifecycle.open();
        NativeLightAction.finishScoped((bl, throwable) -> {
            if (bl && throwable == null) {
                CombatPoisonLifecycle.finish(scope);
            } else {
                CombatPoisonLifecycle.failed(scope, throwable != null ? throwable : new CancellationException("cast publication retired"));
            }
        });
        return scope;
    }

    private static boolean open(CombatPoisonLifecycle.Scope scope) {
        return scope == null || (Boolean)CombatAccess.field(scope, "closed") == false;
    }

    static void palette(ClientSession clientSession, int n, String string, CombatPoisonLifecycle.Scope scope) {
        NativeLightAction.deferScoped(() -> {
            if (NativePoisonMaterialAction.open(scope)) {
                CombatPoisonLifecycle.palette(clientSession, n, string, scope);
            }
        });
    }

    static void monsterSend(ClientSession clientSession, byte[] byArray, String string, CombatPoisonLifecycle.Scope scope) {
        NativeLightAction.deferScoped(() -> {
            if (NativePoisonMaterialAction.open(scope)) {
                CombatMonsterCurseLifecycle.send(clientSession, byArray, string, scope);
            }
        });
    }

    static void monsterPaletteFinished(byte[] byArray, CombatPoisonLifecycle.Scope scope) {
        NativeLightAction.deferScoped(() -> {
            if (NativePoisonMaterialAction.open(scope)) {
                CombatMonsterCurseLifecycle.paletteFinished(byArray, scope);
            }
        });
    }

    static void poisonPaletteFinished(ClientSession clientSession, CombatPoisonLifecycle.Scope scope, boolean bl, int n) {
        if (clientSession == null || scope == null || !bl) {
            return;
        }
        NativeLightAction.deferScoped(() -> {
            if (!NativePoisonMaterialAction.open(scope)) {
                return;
            }
            var context = scope.find(clientSession);
            if (context == null) {
                return;
            }
            BattleTimedStatus battleTimedStatus = CombatPoisonAtomic.status(clientSession);
            synchronized (battleTimedStatus) {
                CombatPoisonLifecycle.Lease lease = (CombatPoisonLifecycle.Lease)CombatAccess.field(context, "producer");
                CombatPoisonLifecycle.State state = (CombatPoisonLifecycle.State)CombatAccess.field(context, "state");
                long l = System.nanoTime();
                if (CombatAccess.field(context, "timed") == battleTimedStatus && ((Boolean)CombatAccess.field(context, "producerActive")).booleanValue() && state.isCurrent(lease) && battleTimedStatus.poisonActive(l)) {
                    CombatAccess.set(battleTimedStatus, "poisonNextTickAtNanos", Math.addExact(l, TimeUnit.MILLISECONDS.toNanos(n)));
                    lease.started = true;
                }
            }
        });
    }
}

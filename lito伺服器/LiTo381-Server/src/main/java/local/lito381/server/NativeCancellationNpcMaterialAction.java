// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CancellationException;
import local.lito381.server.BattleCompanion;
import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.ClassicWorldMonsterPopulation;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatPoisonLifecycle;
import local.lito381.server.GameHub;
import local.lito381.server.NativeCombatAttacker;
import local.lito381.server.NativeLightAction;
import local.lito381.server.NativePetWorldLifetime;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativeSelfCastAction;
import local.lito381.server.NativeWorldMaterialPolicy;
import local.lito381.server.Protocol;

final class NativeCancellationNpcMaterialAction {
    static final String P = "local/lito381/server/";
    private static final ThreadLocal<Target> current = new ThreadLocal();
    private static final ThreadLocal<Target> ingress = new ThreadLocal();

    static ClassicWorldMonsterPopulation population(GameHub gameHub, int n, int n2) {
        return (ClassicWorldMonsterPopulation)CombatAccess.call(gameHub, "classicWorldMonsterPopulation", "(II)Llocal/lito381/server/ClassicWorldMonsterPopulation;", n, n2);
    }

    static Object actor(ClassicWorldMonsterPopulation classicWorldMonsterPopulation, long l) {
        return classicWorldMonsterPopulation == null ? null : ((Map)CombatAccess.field(classicWorldMonsterPopulation, "actors")).get(l);
    }

    static Object epoch(Object object, boolean bl) {
        return object == null ? null : NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(object, "damagePoisonStatus"), bl);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void dispatch(ClientSession clientSession, byte[] byArray, NativeSelfCastAction.Task task) throws IOException {
        if (Protocol.spellSelector(byArray).skillId() != 44 || !String.valueOf(CombatAccess.field(clientSession, "phase")).equals("WORLD") || clientSession.selectedCharacterName().isBlank()) {
            NativePetWorldLifetime.dispatch(clientSession, byArray, task);
            return;
        }
        long l = Protocol.objectSpellRequest(byArray).targetObjectId();
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        int n = (Integer)CombatAccess.field(clientSession, "classicWorldMapId");
        int n2 = (Integer)CombatAccess.field(clientSession, "classicWorldInstanceId");
        ClientSession clientSession2 = null;
        BattleCompanion battleCompanion = null;
        for (ClientSession clientSession3 : gameHub.classicWorldMembers(n, n2)) {
            BattleCompanion battleCompanion2 = (BattleCompanion)CombatAccess.call(clientSession3, "ownedCompanion", "(J)Llocal/lito381/server/BattleCompanion;", l);
            if (battleCompanion2 == null) continue;
            clientSession2 = clientSession3;
            battleCompanion = battleCompanion2;
            break;
        }
        Target target = ingress.get();
        ingress.set(new Target(clientSession, clientSession2, battleCompanion, l));
        try {
            NativePetWorldLifetime.dispatch(clientSession, byArray, task);
        }
        finally {
            if (target == null) {
                ingress.remove();
            } else {
                ingress.set(target);
            }
        }
    }

    static void worldNpc(ClientSession clientSession, Protocol.ObjectSpellRequest objectSpellRequest, ClientSession clientSession2, BattleCompanion battleCompanion, BattleCompanion.Snapshot snapshot, ClassicWorldMonsterPopulation.Snapshot snapshot2, BattleSpellCatalog.CancellationSpell cancellationSpell, CombatPoisonLifecycle.Scope scope) throws IOException {
        Target target = ingress.get();
        Target target2 = target != null && target.pc == clientSession && target.id == objectSpellRequest.targetObjectId() ? target : new Target(clientSession, clientSession2, battleCompanion, objectSpellRequest.targetObjectId());
        NativeLightAction.commitPair(clientSession, target2.owner, () -> NativeWorldMaterialPolicy.run(clientSession, true, 44, () -> {
            GameHub gameHub = target.hub;
            synchronized (gameHub) {
                if (battleCompanion != null) {
                    BattleCompanion battleCompanion2 = battleCompanion;
                    synchronized (battleCompanion2) {
                        if (target2.valid()) {
                            NativeCancellationNpcMaterialAction.owned(target2, clientSession, objectSpellRequest, clientSession2, battleCompanion, snapshot, snapshot2, cancellationSpell);
                        }
                    }
                }
                if (target.population != null) {
                    ClassicWorldMonsterPopulation classicWorldMonsterPopulation = target.population;
                    synchronized (classicWorldMonsterPopulation) {
                        if (target2.valid()) {
                            NativeCancellationNpcMaterialAction.owned(target2, clientSession, objectSpellRequest, clientSession2, battleCompanion, snapshot, snapshot2, cancellationSpell);
                        }
                    }
                }
            }
        }));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void owned(Target target, ClientSession clientSession, Protocol.ObjectSpellRequest objectSpellRequest, ClientSession clientSession2, BattleCompanion battleCompanion, BattleCompanion.Snapshot snapshot, ClassicWorldMonsterPopulation.Snapshot snapshot2, BattleSpellCatalog.CancellationSpell cancellationSpell) throws IOException {
        Target target2 = current.get();
        current.set(target);
        try {
            NativeCancellationNpcMaterialAction.body(clientSession, objectSpellRequest, clientSession2, battleCompanion, snapshot, snapshot2, cancellationSpell);
        }
        finally {
            target.publicationEpoch = NativeCancellationNpcMaterialAction.epoch(target.actor, true);
            if (target2 == null) {
                current.remove();
            } else {
                current.set(target2);
            }
        }
    }

    static boolean captureOutput(ClientSession clientSession, byte[] byArray, String string) {
        Target target = current.get();
        if (target == null) {
            current.remove();
            return false;
        }
        NativePlayerCommit.Binding binding = NativePlayerCommit.Binding.capture(clientSession);
        byte[] byArray2 = (byte[])byArray.clone();
        long l = byArray2.length < 5 ? -1L : (long)(byArray2[1] & 0xFF) | (long)(byArray2[2] & 0xFF) << 8 | (long)(byArray2[3] & 0xFF) << 16 | (long)(byArray2[4] & 0xFF) << 24;
        boolean bl = l == target.id;
        NativeLightAction.deferScoped(() -> {
            if (binding.matches(clientSession) && (!bl || target.publication())) {
                clientSession.nativeSelfOriginalSend(byArray2, string);
            }
        });
        return true;
    }

    private static void body(ClientSession clientSession, Protocol.ObjectSpellRequest objectSpellRequest, ClientSession clientSession2, BattleCompanion battleCompanion, BattleCompanion.Snapshot snapshot, ClassicWorldMonsterPopulation.Snapshot snapshot2, BattleSpellCatalog.CancellationSpell cancellationSpell) throws IOException {
        CombatPoisonLifecycle.Scope scope = CombatPoisonLifecycle.open();
        NativeLightAction.finishScoped((bl, throwable) -> {
            if (bl && throwable == null) {
                CombatPoisonLifecycle.finish(scope);
            } else {
                CombatPoisonLifecycle.failed(scope, throwable != null ? throwable : new CancellationException("NPC cancellation publication retired"));
            }
        });
        try {
            clientSession.nativeMaterialWorldNpcCancellationBody(objectSpellRequest, clientSession2, battleCompanion, snapshot, snapshot2, cancellationSpell, scope);
        }
        catch (IOException | Error | RuntimeException throwable2) {
            CombatPoisonLifecycle.failed(scope, throwable2);
            throw throwable2;
        }
    }

    static boolean admit(ClientSession clientSession, ClientSession clientSession2, int n, int n2, int n3) {
        if (n != 44) {
            throw new IllegalStateException("unexpected NPC cancellation skill");
        }
        return NativeLightAction.admitPair(clientSession, clientSession2 == null ? clientSession : clientSession2, n, n2, n3);
    }

    private NativeCancellationNpcMaterialAction() {
    }

    static final class Target {
        final ClientSession pc;
        final ClientSession owner;
        final NativePlayerCommit.Binding casterBinding;
        final NativePlayerCommit.Binding ownerBinding;
        final GameHub hub;
        final int map;
        final int instance;
        final long id;
        final ClassicWorldMonsterPopulation population;
        final Object actor;
        final Object epoch;
        final BattleCompanion companion;
        Object publicationEpoch;

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        Target(ClientSession clientSession, ClientSession clientSession2, BattleCompanion battleCompanion, long l) {
            this.pc = clientSession;
            this.owner = clientSession2 == null ? clientSession : clientSession2;
            this.companion = battleCompanion;
            this.id = l;
            this.casterBinding = NativePlayerCommit.Binding.capture(clientSession);
            this.ownerBinding = NativePlayerCommit.Binding.capture(this.owner);
            this.hub = (GameHub)CombatAccess.field(clientSession, "hub");
            this.map = (Integer)CombatAccess.field(clientSession, "classicWorldMapId");
            this.instance = (Integer)CombatAccess.field(clientSession, "classicWorldInstanceId");
            GameHub gameHub = this.hub;
            synchronized (gameHub) {
                ClassicWorldMonsterPopulation classicWorldMonsterPopulation = this.population = battleCompanion == null ? NativeCancellationNpcMaterialAction.population(this.hub, this.map, this.instance) : null;
                if (this.population == null) {
                    this.actor = null;
                    this.epoch = null;
                } else {
                    ClassicWorldMonsterPopulation classicWorldMonsterPopulation2 = this.population;
                    synchronized (classicWorldMonsterPopulation2) {
                        this.actor = NativeCancellationNpcMaterialAction.actor(this.population, l);
                        this.epoch = NativeCancellationNpcMaterialAction.epoch(this.actor, true);
                    }
                }
            }
            this.publicationEpoch = this.epoch;
        }

        boolean scene() {
            return this.casterBinding.matches(this.pc) && this.ownerBinding.matches(this.owner) && (Boolean)CombatAccess.field(this.pc, "sourceItemDisconnected") == false && String.valueOf(CombatAccess.field(this.pc, "phase")).equals("WORLD") && (Integer)CombatAccess.field(this.pc, "classicWorldMapId") == this.map && (Integer)CombatAccess.field(this.pc, "classicWorldInstanceId") == this.instance;
        }

        boolean valid() {
            if (!this.scene()) {
                return false;
            }
            if (this.companion != null) {
                return CombatAccess.call(this.owner, "ownedCompanion", "(J)Llocal/lito381/server/BattleCompanion;", this.id) == this.companion;
            }
            return this.population != null && NativeCancellationNpcMaterialAction.population(this.hub, this.map, this.instance) == this.population && NativeCancellationNpcMaterialAction.actor(this.population, this.id) == this.actor && this.actor != null && NativeCancellationNpcMaterialAction.epoch(this.actor, false) == this.epoch;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        boolean publication() {
            GameHub gameHub = this.hub;
            synchronized (gameHub) {
                if (!this.scene()) {
                    return false;
                }
                if (this.companion != null) {
                    BattleCompanion battleCompanion = this.companion;
                    synchronized (battleCompanion) {
                        BattleCompanion.Snapshot snapshot = this.companion.snapshot();
                        return this.valid() && snapshot.active() && snapshot.hp() > 0;
                    }
                }
                if (this.population == null || NativeCancellationNpcMaterialAction.population(this.hub, this.map, this.instance) != this.population) {
                    return false;
                }
                ClassicWorldMonsterPopulation classicWorldMonsterPopulation = this.population;
                synchronized (classicWorldMonsterPopulation) {
                    return NativeCancellationNpcMaterialAction.actor(this.population, this.id) == this.actor && NativeCancellationNpcMaterialAction.epoch(this.actor, false) == this.publicationEpoch && (Boolean)CombatAccess.field(this.actor, "alive") != false && (Boolean)CombatAccess.field(this.actor, "present") != false;
                }
            }
        }
    }
}

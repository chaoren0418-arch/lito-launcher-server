// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.util.Map;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.ClassicWorldMonsterPopulation;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.GameHub;
import local.lito381.server.NativeCombatAttacker;
import local.lito381.server.NativeLightAction;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativeSelfCastAction;
import local.lito381.server.NativeWorldMaterialPolicy;
import local.lito381.server.Protocol;

final class NativePetWorldLifetime {
    private static final String P = "local/lito381/server/";
    private static final ThreadLocal<Target> ingress = new ThreadLocal();
    private static final ThreadLocal<Target> current = new ThreadLocal();

    private static ClassicWorldMonsterPopulation population(GameHub gameHub, int n, int n2) {
        return (ClassicWorldMonsterPopulation)CombatAccess.call(gameHub, "classicWorldMonsterPopulation", "(II)Llocal/lito381/server/ClassicWorldMonsterPopulation;", n, n2);
    }

    private static Object actor(ClassicWorldMonsterPopulation classicWorldMonsterPopulation, long l) {
        return classicWorldMonsterPopulation == null ? null : ((Map)CombatAccess.field(classicWorldMonsterPopulation, "actors")).get(l);
    }

    private static Object epoch(Object object) {
        return object == null ? null : NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(object, "damagePoisonStatus"), false);
    }

    private static Object bindEpoch(Object object) {
        return object == null ? null : NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(object, "damagePoisonStatus"), true);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static Target take(ClientSession clientSession, byte[] byArray) {
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        int n = (Integer)CombatAccess.field(clientSession, "classicWorldMapId");
        int n2 = (Integer)CombatAccess.field(clientSession, "classicWorldInstanceId");
        long l = Protocol.spellSelector(byArray).skillId() == 36 ? Protocol.attackSpellRequest(byArray).targetObjectId() : Protocol.objectSpellRequest(byArray).targetObjectId();
        GameHub gameHub2 = gameHub;
        synchronized (gameHub2) {
            ClassicWorldMonsterPopulation classicWorldMonsterPopulation = NativePetWorldLifetime.population(gameHub, n, n2);
            if (classicWorldMonsterPopulation == null) {
                return new Target(clientSession, gameHub, n, n2, l, null, null);
            }
            ClassicWorldMonsterPopulation classicWorldMonsterPopulation2 = classicWorldMonsterPopulation;
            synchronized (classicWorldMonsterPopulation2) {
                return new Target(clientSession, gameHub, n, n2, l, classicWorldMonsterPopulation, NativePetWorldLifetime.actor(classicWorldMonsterPopulation, l));
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void dispatch(ClientSession clientSession, byte[] byArray, NativeSelfCastAction.Task task) throws IOException {
        int n = Protocol.spellSelector(byArray).skillId();
        if (n != 36 && n != 41 || !String.valueOf(CombatAccess.field(clientSession, "phase")).equals("WORLD") || clientSession.selectedCharacterName().isBlank()) {
            task.run();
            return;
        }
        Target target = ingress.get();
        ingress.set(NativePetWorldLifetime.take(clientSession, byArray));
        try {
            task.run();
        }
        finally {
            if (target == null) {
                ingress.remove();
            } else {
                ingress.set(target);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void run(ClientSession clientSession, byte[] byArray, NativeSelfCastAction.Task task) throws IOException {
        if (!String.valueOf(CombatAccess.field(clientSession, "phase")).equals("WORLD")) {
            NativeLightAction.commitPair(clientSession, clientSession, task);
            return;
        }
        Target target = ingress.get();
        Target target2 = target != null && target.pc == clientSession ? target : NativePetWorldLifetime.take(clientSession, byArray);
        ingress.remove();
        try {
            NativeLightAction.commitPair(clientSession, clientSession, () -> NativeWorldMaterialPolicy.run(clientSession, target.actor != null, Protocol.spellSelector(byArray).skillId(), () -> {
                GameHub gameHub = target.hub;
                synchronized (gameHub) {
                    if (target.population == null) {
                        if (target2.valid()) {
                            task.run();
                        }
                        return;
                    }
                    ClassicWorldMonsterPopulation classicWorldMonsterPopulation = target.population;
                    synchronized (classicWorldMonsterPopulation) {
                        if (!target2.valid()) {
                            return;
                        }
                        Target target2 = current.get();
                        current.set(target2);
                        try {
                            task.run();
                        }
                        catch (Throwable throwable) {
                            target.publicationEpoch = NativePetWorldLifetime.bindEpoch(target.actor);
                            boolean bl = target.publicationPresent = target.actor != null && (Boolean)CombatAccess.field(target.actor, "present") != false;
                            if (target2 == null) {
                                current.remove();
                            } else {
                                current.set(target2);
                            }
                            throw throwable;
                        }
                        target.publicationEpoch = NativePetWorldLifetime.bindEpoch(target.actor);
                        boolean bl = target.publicationPresent = target.actor != null && (Boolean)CombatAccess.field(target.actor, "present") != false;
                        if (target2 == null) {
                            current.remove();
                        } else {
                            current.set(target2);
                        }
                    }
                }
            }));
        }
        finally {
            if (target == null) {
                ingress.remove();
            } else {
                ingress.set(target);
            }
        }
    }

    static boolean captureOutput(ClientSession clientSession, byte[] byArray, String string) {
        boolean bl;
        Target target = current.get();
        if (target == null) {
            current.remove();
            return false;
        }
        boolean bl2 = string.startsWith("S_EFFECT taming target=") || string.startsWith("S_EFFECT create-zombie corpse=");
        boolean bl3 = bl = string.startsWith("S_REMOVE_OBJECT ") && (string.endsWith(" reason=tamed") || string.endsWith(" reason=create-zombie"));
        if (!bl2 && !bl) {
            return false;
        }
        byte[] byArray2 = (byte[])byArray.clone();
        NativeLightAction.deferScoped(() -> {
            if (target.publication()) {
                clientSession.nativeSelfOriginalSend(byArray2, string);
            }
        });
        return true;
    }

    private NativePetWorldLifetime() {
    }

    private static final class Target {
        final ClientSession pc;
        final GameHub hub;
        final NativePlayerCommit.Binding binding;
        final int map;
        final int instance;
        final long id;
        final Object epoch;
        final ClassicWorldMonsterPopulation population;
        final Object actor;
        Object publicationEpoch;
        boolean publicationPresent;

        Target(ClientSession clientSession, GameHub gameHub, int n, int n2, long l, ClassicWorldMonsterPopulation classicWorldMonsterPopulation, Object object) {
            this.pc = clientSession;
            this.hub = gameHub;
            this.map = n;
            this.instance = n2;
            this.id = l;
            this.population = classicWorldMonsterPopulation;
            this.actor = object;
            this.binding = NativePlayerCommit.Binding.capture(clientSession);
            this.publicationEpoch = this.epoch = NativePetWorldLifetime.bindEpoch(object);
        }

        boolean scene() {
            return this.binding.matches(this.pc) && (Boolean)CombatAccess.field(this.pc, "sourceItemDisconnected") == false && String.valueOf(CombatAccess.field(this.pc, "phase")).equals("WORLD") && (Integer)CombatAccess.field(this.pc, "classicWorldMapId") == this.map && (Integer)CombatAccess.field(this.pc, "classicWorldInstanceId") == this.instance;
        }

        boolean valid() {
            return this.scene() && NativePetWorldLifetime.population(this.hub, this.map, this.instance) == this.population && NativePetWorldLifetime.actor(this.population, this.id) == this.actor && NativePetWorldLifetime.epoch(this.actor) == this.epoch;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        boolean publication() {
            GameHub gameHub = this.hub;
            synchronized (gameHub) {
                if (!this.scene() || NativePetWorldLifetime.population(this.hub, this.map, this.instance) != this.population) {
                    return false;
                }
                if (this.population == null) {
                    return true;
                }
                ClassicWorldMonsterPopulation classicWorldMonsterPopulation = this.population;
                synchronized (classicWorldMonsterPopulation) {
                    Object object = NativePetWorldLifetime.actor(this.population, this.id);
                    return !(object != this.actor && object != null || NativePetWorldLifetime.epoch(this.actor) != this.publicationEpoch || object != null && (Boolean)CombatAccess.field(object, "present") != this.publicationPresent);
                }
            }
        }
    }
}

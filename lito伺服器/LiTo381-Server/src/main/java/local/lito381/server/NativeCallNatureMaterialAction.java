// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import local.lito381.server.BattleCompanion;
import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.ClassicWorldMonsterPopulation;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.GameHub;
import local.lito381.server.NativeAreaMaterialAction;
import local.lito381.server.NativeCancellationNpcMaterialAction;
import local.lito381.server.NativeCombatAttacker;
import local.lito381.server.NativeCompanionCallbacks;
import local.lito381.server.NativeLightAction;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativeRolePublication;
import local.lito381.server.Protocol;

final class NativeCallNatureMaterialAction {
    static final String P = "local/lito381/server/";
    static final ThreadLocal<Target> current = new ThreadLocal();

    static void cast(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.ResurrectionSpell resurrectionSpell) throws IOException {
        if (resurrectionSpell.skillId() != 165) {
            throw new IllegalArgumentException("Call of Nature selector");
        }
        Target target = new Target(clientSession, Protocol.objectSpellRequest(byArray).targetObjectId());
        NativeAreaMaterialAction.run(clientSession, 165, () -> {
            if (!target.scene.scene()) {
                return;
            }
            if (target.kind == 2) {
                Object object = target.actor;
                synchronized (object) {
                    NativeCallNatureMaterialAction.owned(target, clientSession, byArray, resurrectionSpell);
                }
            } else if (target.kind == 4 || target.kind == 5) {
                Object object = target.owner;
                synchronized (object) {
                    NativeCallNatureMaterialAction.owned(target, clientSession, byArray, resurrectionSpell);
                }
            } else {
                NativeCallNatureMaterialAction.owned(target, clientSession, byArray, resurrectionSpell);
            }
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void owned(Target target, ClientSession clientSession, byte[] byArray, BattleSpellCatalog.ResurrectionSpell resurrectionSpell) throws IOException {
        if (target.kind != 0 && !target.valid()) {
            return;
        }
        Target target2 = current.get();
        current.set(target);
        try {
            clientSession.nativeMaterialCallNatureBody(byArray, resurrectionSpell);
        }
        finally {
            target.finish();
            if (target2 == null) {
                current.remove();
            } else {
                current.set(target2);
            }
        }
    }

    static boolean admit(ClientSession clientSession, int n, int n2, int n3) {
        Target target = current.get();
        if (n != 165 || target == null || target.scene.pc != clientSession || !target.valid()) {
            return false;
        }
        return NativeLightAction.admitPair(clientSession, clientSession, n, n2, n3);
    }

    static List<ClientSession> battleRecipients(GameHub gameHub, long l, int n) {
        return NativeAreaMaterialAction.filter(gameHub.battleRecipients(l, n));
    }

    static void publish(ClientSession clientSession, byte[] byArray, String string) {
        Target target = current.get();
        if (target == null) {
            throw new IllegalStateException("Call of Nature output outside owned cast");
        }
        NativePlayerCommit.Binding binding = NativePlayerCommit.Binding.capture(clientSession);
        byte[] byArray2 = (byte[])byArray.clone();
        long l = byArray2.length < 5 ? -1L : (long)(byArray2[1] & 0xFF) | (long)(byArray2[2] & 0xFF) << 8 | (long)(byArray2[3] & 0xFF) << 16 | (long)(byArray2[4] & 0xFF) << 24;
        Runnable runnable = NativeRolePublication.completion(clientSession, () -> {
            if (binding.matches(clientSession) && (l != target.id || target.publication())) {
                try {
                    clientSession.nativeSelfOriginalSend(byArray2, string);
                }
                catch (IOException iOException) {
                    throw new UncheckedIOException(iOException);
                }
            }
        });
        NativeLightAction.finishScoped((bl, throwable) -> runnable.run());
    }

    private NativeCallNatureMaterialAction() {
    }

    static final class Target {
        final NativeAreaMaterialAction.Context scene;
        final long id;
        int kind;
        Object owner;
        Object actor;
        Object epoch;
        Object publicationEpoch;
        Object publicationActor;
        ClientSession player;
        NativePlayerCommit.Binding playerBinding;
        boolean publicationDead;

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        Target(ClientSession clientSession, long l) {
            this.id = l;
            this.scene = new NativeAreaMaterialAction.Context(clientSession);
            GameHub gameHub = this.scene.hub;
            synchronized (gameHub) {
                for (ClientSession clientSession2 : this.scene.players) {
                    if (clientSession2.playerObjectId() == l) {
                        this.kind = 1;
                        this.player = clientSession2;
                        this.playerBinding = this.scene.bindings.get(clientSession2);
                        this.actor = clientSession2;
                        this.owner = clientSession2;
                        break;
                    }
                    BattleCompanion battleCompanion = (BattleCompanion)CombatAccess.call(clientSession2, "ownedCompanion", "(J)Llocal/lito381/server/BattleCompanion;", l);
                    if (battleCompanion == null) continue;
                    this.kind = 2;
                    this.player = clientSession2;
                    this.playerBinding = this.scene.bindings.get(clientSession2);
                    this.owner = clientSession2;
                    this.actor = battleCompanion;
                    break;
                }
                if (this.kind == 0 && this.scene.phase.equals("WORLD") && this.scene.population != null) {
                    ClassicWorldMonsterPopulation classicWorldMonsterPopulation = this.scene.population;
                    synchronized (classicWorldMonsterPopulation) {
                        this.actor = NativeCancellationNpcMaterialAction.actor(this.scene.population, l);
                        if (this.actor != null) {
                            this.kind = 3;
                            this.owner = this.scene.population;
                        }
                    }
                }
                if (this.kind == 0 && this.scene.phase.equals("BATTLE")) {
                    for (Object object : (ClassicWorldMonsterPopulation)new int[]{1, 2}) {
                        Object object2 = this.scene.encounter((int)object);
                        Object object3 = Target.corpse(object2, l);
                        if (object3 == null) continue;
                        this.kind = (int)(object + 3);
                        this.owner = object2;
                        this.actor = object3;
                        break;
                    }
                }
                this.publicationEpoch = this.epoch = this.life(true);
                this.publicationActor = this.actor;
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        static Object corpse(Object object, long l) {
            if (object == null) {
                return null;
            }
            Object object2 = object;
            synchronized (object2) {
                for (Object e : (List)CombatAccess.field(object, "corpses")) {
                    Object object3 = CombatAccess.field(e, "snapshot");
                    if ((Long)CombatAccess.call(object3, "objectId", "()J", new Object[0]) != l) continue;
                    return e;
                }
                return null;
            }
        }

        Object life(boolean bl) {
            if (this.kind == 1) {
                return NativePlayerCommit.Binding.capture(this.player).owner();
            }
            if (this.kind == 2) {
                return NativeCompanionCallbacks.capture((NativePlayerCommit.Binding)this.playerBinding, (BattleCompanion)((BattleCompanion)this.actor)).life;
            }
            if (this.kind == 3) {
                return NativeCancellationNpcMaterialAction.epoch(this.actor, bl);
            }
            if (this.kind == 4 || this.kind == 5) {
                return this.actor;
            }
            return null;
        }

        boolean canonical() {
            if (this.kind == 1) {
                return this.playerBinding.matches(this.player) && this.scene.player(this.player) && (Boolean)CombatAccess.field(this.player, "sourceItemDisconnected") == false;
            }
            if (this.kind == 2) {
                return this.playerBinding.matches(this.player) && this.scene.player(this.player) && (Boolean)CombatAccess.field(this.player, "sourceItemDisconnected") == false && CombatAccess.call(this.player, "ownedCompanion", "(J)Llocal/lito381/server/BattleCompanion;", this.id) == this.actor;
            }
            if (this.kind == 3) {
                return NativeCancellationNpcMaterialAction.population(this.scene.hub, this.scene.map, this.scene.instance) == this.owner && NativeCancellationNpcMaterialAction.actor((ClassicWorldMonsterPopulation)this.owner, this.id) == this.actor && (Boolean)CombatAccess.field(this.actor, "present") != false;
            }
            if (this.kind == 4 || this.kind == 5) {
                return this.scene.encounter(this.kind - 3) == this.owner && Target.corpse(this.owner, this.id) == this.actor && System.nanoTime() - (Long)CombatAccess.field(this.actor, "expiresAtNanos") < 0L;
            }
            return false;
        }

        boolean dead() {
            if (this.kind == 1) {
                return (Integer)CombatAccess.field(this.player, "playerHp") == 0;
            }
            if (this.kind == 2) {
                BattleCompanion.Snapshot snapshot = ((BattleCompanion)this.actor).snapshot();
                return snapshot.active() && snapshot.hp() == 0;
            }
            if (this.kind == 3) {
                return (Boolean)CombatAccess.field(this.actor, "alive") == false;
            }
            return this.kind == 4 || this.kind == 5;
        }

        boolean valid() {
            return this.scene.scene() && this.canonical() && this.life(false) == this.epoch && this.dead();
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        boolean publication() {
            GameHub gameHub = this.scene.hub;
            synchronized (gameHub) {
                if (this.scene.population != null) {
                    ClassicWorldMonsterPopulation classicWorldMonsterPopulation = this.scene.population;
                    synchronized (classicWorldMonsterPopulation) {
                        return this.publicationOwned();
                    }
                }
                return this.publicationOwned();
            }
        }

        void finish() {
            Object object;
            this.publicationActor = this.actor;
            this.publicationEpoch = this.life(true);
            boolean bl = this.publicationDead = this.kind != 0 && this.dead();
            if ((this.kind == 4 || this.kind == 5) && (object = this.scene.monster(this.owner, this.kind - 3, this.id)) != null) {
                this.publicationActor = object;
                this.publicationEpoch = NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(object, "damagePoison"), true);
                this.publicationDead = false;
            }
        }

        boolean publicationOwned() {
            if (!(this.kind != 4 && this.kind != 5 || this.publicationDead)) {
                return this.scene.encounter(this.kind - 3) == this.owner && this.scene.monster(this.owner, this.kind - 3, this.id) == this.publicationActor && NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(this.publicationActor, "damagePoison"), false) == this.publicationEpoch && (Integer)CombatAccess.field(this.publicationActor, "hp") > 0;
            }
            return this.canonical() && this.life(false) == this.publicationEpoch && this.dead() == this.publicationDead;
        }
    }
}

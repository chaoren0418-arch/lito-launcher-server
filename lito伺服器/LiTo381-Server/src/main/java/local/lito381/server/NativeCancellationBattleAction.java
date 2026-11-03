// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.util.List;
import local.lito381.server.BattleQuestEncounter;
import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.BattleTimeAttackEncounter;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatPoisonLifecycle;
import local.lito381.server.GameHub;
import local.lito381.server.NativeAreaMaterialAction;
import local.lito381.server.NativeCombatAttacker;
import local.lito381.server.Protocol;

final class NativeCancellationBattleAction {
    static final ThreadLocal<Target> current = new ThreadLocal();

    static boolean cast(ClientSession clientSession2, byte[] byArray, BattleSpellCatalog.CancellationSpell cancellationSpell, CombatPoisonLifecycle.Scope scope) throws IOException {
        if (cancellationSpell.skillId() != 44 || !String.valueOf(CombatAccess.field(clientSession2, "phase")).equals("BATTLE")) {
            return false;
        }
        long l = Protocol.objectSpellRequest(byArray).targetObjectId();
        NativeAreaMaterialAction.Context context = new NativeAreaMaterialAction.Context(clientSession2);
        if (context.players.stream().anyMatch(clientSession -> clientSession.playerObjectId() == l)) {
            return false;
        }
        NativeAreaMaterialAction.Npc npc = context.npcs.get(l);
        if (npc == null || npc.kind() != 1 && npc.kind() != 2) {
            return false;
        }
        Target target = new Target(context, l, npc);
        NativeAreaMaterialAction.run(clientSession2, 44, () -> {
            Object object = npc.owner();
            synchronized (object) {
                if (!target.valid()) {
                    return;
                }
                Target target2 = current.get();
                current.set(target);
                try {
                    clientSession2.nativeMaterialCancellationBody(byArray, cancellationSpell, scope);
                }
                finally {
                    target.refresh();
                    if (target2 == null) {
                        current.remove();
                    } else {
                        current.set(target2);
                    }
                }
            }
        });
        return true;
    }

    static List<ClientSession> roomCombatants(GameHub gameHub, long l) {
        return current.get() == null ? gameHub.roomCombatants(l) : NativeAreaMaterialAction.roomCombatants(gameHub, l);
    }

    static List<ClientSession> roomMembers(GameHub gameHub, long l) {
        return current.get() == null ? gameHub.roomMembers(l) : NativeAreaMaterialAction.battleMembers(gameHub, l);
    }

    static List<ClientSession> battleRecipients(GameHub gameHub, long l, int n) {
        List<ClientSession> list = gameHub.battleRecipients(l, n);
        return current.get() == null ? list : NativeAreaMaterialAction.filter(list);
    }

    static List<BattleQuestEncounter.Snapshot> visibleQuestMonsters(GameHub gameHub, long l, ClientSession clientSession) {
        return current.get() == null ? gameHub.visibleQuestMonsters(l, clientSession) : NativeAreaMaterialAction.questNpcs(gameHub, l, clientSession);
    }

    static List<BattleTimeAttackEncounter.Snapshot> visibleTimeAttackActors(GameHub gameHub, long l, ClientSession clientSession) {
        return current.get() == null ? gameHub.visibleTimeAttackActors(l, clientSession) : NativeAreaMaterialAction.timeNpcs(gameHub, l, clientSession);
    }

    private NativeCancellationBattleAction() {
    }

    record Target(NativeAreaMaterialAction.Context scene, long id, NativeAreaMaterialAction.Npc npc) {
        boolean valid() {
            return this.scene.scene() && this.scene.npc(this.id);
        }

        void refresh() {
            NativeAreaMaterialAction.Context context = NativeAreaMaterialAction.current.get();
            if (context != null && this.scene.encounter(this.npc.kind()) == this.npc.owner() && this.scene.monster(this.npc.owner(), this.npc.kind(), this.id) == this.npc.actor() && (Integer)CombatAccess.field(this.npc.actor(), "hp") > 0) {
                Object object = NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(this.npc.actor(), "damagePoison"), true);
                context.npcs.put(this.id, new NativeAreaMaterialAction.Npc(this.npc.kind(), this.id, this.npc.owner(), this.npc.actor(), object));
            }
        }
    }
}

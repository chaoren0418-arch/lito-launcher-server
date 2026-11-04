// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import local.lito381.server.BattleGuardianTower;
import local.lito381.server.BattleQuestBoundary;
import local.lito381.server.BattleQuestEncounter;
import local.lito381.server.BattleRules;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.CharacterSummary;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatPoisonAtomic;
import local.lito381.server.GameHub;
import local.lito381.server.GameStateStore;
import local.lito381.server.NativeCombatAttacker;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativeProcHealingEncounter;
import local.lito381.server.Protocol;

final class NativeProcEncounterPublication {
    NativeProcEncounterPublication() {
    }

    static void publish(Context context) {
        block3: {
            if (!context.valid()) {
                return;
            }
            ClientSession clientSession = context.driver;
            String string = context.plan.target().kind() == 3 ? "sourceProcQuestCommitted" : "sourceProcTimeCommitted";
            String string2 = context.plan.target().kind() == 3 ? "BattleQuestEncounter$Hit" : "GameHub$TimeAttackActorHit";
            try {
                CombatAccess.call(clientSession, string, "(Llocal/lito381/server/" + string2 + ";ILjava/lang/String;ILlocal/lito381/server/NativeProcEncounterPublication$Context;)I", context.outcome, context.palette, "equipment-proc-healing skill=" + context.plan.skill(), context.damage, context);
            }
            catch (RuntimeException runtimeException) {
                if (!NativeProcEncounterPublication.hasStale(runtimeException)) throw runtimeException;
            }
        }
    }

    private static boolean hasStale(Throwable throwable) {
        for (Throwable throwable2 = throwable; throwable2 != null; throwable2 = throwable2.getCause()) {
            if (throwable2 instanceof Stale) return true;
        }
        return false;
    }

    static long room(ClientSession clientSession, Context context) {
        return context.plan.target().room();
    }

    static String name(ClientSession clientSession, Context context) {
        return context.plan.caster().selected();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static List<ClientSession> capture(List<ClientSession> list, Context context) {
        if (!context.valid()) {
            throw new Stale();
        }
        ArrayList<ClientSession> arrayList = new ArrayList<ClientSession>();
        synchronized (context.hub) {
            for (ClientSession clientSession : list) {
                NativePlayerCommit.Binding binding = NativePlayerCommit.Binding.capture(clientSession);
                if (binding.room() != context.plan.target().room() || !binding.phase().equals("BATTLE")) continue;
                Viewer viewer = context.viewers.get(clientSession);
                if (viewer != null && !viewer.binding().equals(binding)) continue;
                int n = context.hub.forceOf(binding.room(), clientSession);
                context.viewers.putIfAbsent(clientSession, new Viewer(binding, n));
                arrayList.add(clientSession);
            }
        }
        return arrayList;
    }

    static List<ClientSession> members(GameHub gameHub, long l, Context context) {
        return NativeProcEncounterPublication.capture(gameHub.roomMembers(context.plan.target().room()), context);
    }

    static List<ClientSession> recipients(GameHub gameHub, long l, int n, Context context) {
        return NativeProcEncounterPublication.capture(gameHub.battleRecipients(context.plan.target().room(), n), context);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static boolean current(ClientSession clientSession, Context context) {
        Viewer viewer = context.viewers.get(clientSession);
        if (viewer == null || !viewer.binding().matches(clientSession) || !context.valid()) {
            return false;
        }
        synchronized (context.hub) {
            return context.hub.roomMembers(context.plan.target().room()).contains(clientSession) && context.hub.forceOf(context.plan.target().room(), clientSession) == viewer.force();
        }
    }

    static void send(ClientSession clientSession, byte[] byArray, String string, Context context) {
        if (NativeProcEncounterPublication.current(clientSession, context)) {
            NativePlayerCommit.safeSend(clientSession, byArray, string);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static List<BattleGuardianTower.Snapshot> visible(GameHub gameHub, long l, ClientSession clientSession, Context context) {
        if (!context.valid()) {
            throw new Stale();
        }
        synchronized (gameHub) {
            Object object = CombatAccess.field(context.room, "guardianTowers");
            return ((List<BattleGuardianTower.Snapshot>)CombatAccess.call(object, "snapshots", "()Ljava/util/List;", new Object[0])).stream().filter(snapshot -> snapshot.force() == context.force).toList();
        }
    }

    static List<BattleQuestBoundary.Attribute> opened(GameHub gameHub, long l, int n, int n2, Context context) {
        if (!context.valid()) {
            throw new Stale();
        }
        return gameHub.openedQuestBoundary(context.plan.target().room(), n, n2);
    }

    static void arm(GameHub gameHub, long l, Context context) {
        if (context.valid()) {
            gameHub.armPveAi(context.plan.target().room());
        }
    }

    static boolean cancel(GameHub gameHub, long l, long l2, Context context) {
        return context.valid() && gameHub.cancelArmorBreak(context.plan.target().room(), l2);
    }

    static void end(ClientSession clientSession, BattleRules.Resolution resolution, Context context) {
        if (!NativeProcEncounterPublication.current(clientSession, context)) {
            return;
        }
        try {
            clientSession.serverEndBattle(resolution);
        }
        catch (Exception exception) {
            NativePlayerCommit.secondaryFailure("encounter end publication", new IllegalStateException(exception));
        }
    }

    static void finish(GameHub gameHub, long l, Context context) {
        if (context.valid()) {
            gameHub.endBattle(context.plan.target().room());
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void lawful(ClientSession clientSession, int n, String string, Context context) {
        int n2 = context.plan.npc().lawful();
        if (n2 == 0 || !context.valid()) {
            return;
        }
        ClientSession clientSession2 = NativeProcHealingEncounter.find(context.hub, context.plan.caster());
        if (clientSession2 == null) {
            return;
        }
        GameStateStore.LawfulMutation lawfulMutation;
        synchronized (context.hub) {
            synchronized (CombatPoisonAtomic.status(clientSession2)) {
                synchronized (context.container) {
                    synchronized (context.timed) {
                        if (!(context.plan.caster().matches(clientSession2) && context.valid() && ((Set)CombatAccess.field(context.room, "members")).contains(clientSession2) && ((Set)CombatAccess.field(context.room, "battleRoster")).contains(clientSession2))) {
                            return;
                        }
                        CharacterSummary characterSummary = (CharacterSummary)CombatAccess.call(clientSession2, "selectedCharacterSummary", "()Llocal/lito381/server/CharacterSummary;", new Object[0]);
                        int n3 = characterSummary == null ? 0 : (int)Math.max(-32768L, Math.min(32767L, characterSummary.lawful()));
                        GameStateStore gameStateStore = (GameStateStore)CombatAccess.field(clientSession2, "state");
                        String string2 = (String)CombatAccess.call(clientSession2, "stateCharacter", "()Ljava/lang/String;", new Object[0]);
                        try {
                            lawfulMutation = gameStateStore.addLawful(string2, n3, -n2);
                        }
                        catch (Exception exception) {
                            throw new IllegalStateException("encounter lawful mutation failed", exception);
                        }
                        CombatAccess.set(clientSession2, "playerLawful", lawfulMutation.after());
                    }
                }
            }
        }
        if (!lawfulMutation.changed() || !context.plan.caster().matches(clientSession2)) {
            return;
        }
        byte[] object = Protocol.changeAlignment(context.plan.caster().object(), lawfulMutation.after());
        for (ClientSession clientSession3 : NativeProcEncounterPublication.members(context.hub, context.plan.target().room(), context)) {
            if (!context.plan.caster().matches(clientSession2)) break;
            NativeProcEncounterPublication.send(clientSession3, (byte[])object, "S_CHANGE_ALIGNMENT equipment-encounter-healing npc=" + n + " lawful=" + lawfulMutation.before() + "->" + lawfulMutation.after() + " source=" + string, context);
        }
    }

    static final class Context {
        final GameHub hub;
        final ClientSession driver;
        final NativeProcHealingEncounter.Plan plan;
        final Object room;
        final Object container;
        final Object actor;
        final BattleTimedStatus timed;
        final Object afterToken;
        final Object outcome;
        final int palette;
        final int damage;
        final int force;
        final boolean dead;
        final IdentityHashMap<ClientSession, Viewer> viewers = new IdentityHashMap();

        Context(NativeProcHealingEncounter.Live live, NativeProcHealingEncounter.Plan plan, Object object, int n, int n2, Object object2) {
            this.hub = live.hub();
            this.driver = live.caster();
            this.room = live.room();
            this.container = live.container();
            this.actor = live.actor();
            this.timed = live.timed();
            this.plan = plan;
            this.outcome = object;
            this.palette = n;
            this.damage = n2;
            this.afterToken = object2;
            if (object instanceof BattleQuestEncounter.Hit) {
                BattleQuestEncounter.Hit hit = (BattleQuestEncounter.Hit)object;
                this.force = hit.damage().after().force();
                this.dead = hit.damage().diedNow();
            } else {
                this.force = -1;
                this.dead = ((GameHub.TimeAttackActorHit)object).hit().damage().diedNow();
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        boolean valid() {
            NativeCombatAttacker.Ref ref = this.plan.target();
            synchronized (this.hub) {
                if (NativeProcHealingEncounter.room(this.hub, ref.room()) != this.room || !NativeProcHealingEncounter.mode(this.room, ref) || NativeProcHealingEncounter.container(this.room, ref) != this.container) {
                    return false;
                }
                synchronized (this.container) {
                    synchronized (this.timed) {
                        if (NativeProcHealingEncounter.generation(this.container) != this.plan.generation() || NativeCombatAttacker.token(this.timed, false) != this.afterToken) {
                            return false;
                        }
                        Object object2 = NativeProcHealingEncounter.actor(this.container, ref);
                        return this.dead ? NativePlayerCommit.integer(this.actor, "hp") == 0 && (object2 == null || object2 == this.actor) : object2 == this.actor && NativePlayerCommit.integer(this.actor, "hp") > 0;
                    }
                }
            }
        }
    }

    private static final class Stale
    extends RuntimeException {
        private Stale() {
        }
    }

    record Viewer(NativePlayerCommit.Binding binding, int force) {
    }
}

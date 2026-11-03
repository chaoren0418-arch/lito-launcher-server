// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import local.lito381.server.BattleElementalFallDownStatus;
import local.lito381.server.BattleQuestEncounter;
import local.lito381.server.BattleTimeAttackEncounter;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.ClassicWorldMonsterCatalog;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.GameHub;
import local.lito381.server.NativeCancellationBattleAction;
import local.lito381.server.NativeCancellationNpcRules;
import local.lito381.server.NativeCancellationNpcRuntime;
import local.lito381.server.NativeDeNpcTemplate;
import local.lito381.server.NativeDeNpcTemplateResources;
import local.lito381.server.NativeLightAction;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativeRolePublication;
import local.lito381.server.NativeSourcePlayerActionLock;
import local.lito381.server.Protocol;
import local.lito381.server.RuntimeInventory;

final class NativeCancellationBattleRuntime {
    static final String P = "local.lito381.server.";

    static Object room(GameHub gameHub, long l) {
        return ((Map)CombatAccess.field(gameHub, "rooms")).get(l);
    }

    static Object encounter(Object object, int n) {
        return object == null ? null : CombatAccess.field(object, n == 1 ? "questEncounter" : "timeAttackEncounter");
    }

    static State state(GameHub gameHub) {
        if (gameHub.nativeCancellationBattleRuntime == null) {
            gameHub.nativeCancellationBattleRuntime = new State(gameHub);
        }
        return gameHub.nativeCancellationBattleRuntime;
    }

    static boolean start(State state) {
        if (state.closed) {
            return false;
        }
        if (state.task != null && !state.task.isDone()) {
            return true;
        }
        try {
            state.task = ((ScheduledExecutorService)CombatAccess.field(state.hub, "battleClock")).scheduleWithFixedDelay(() -> {
                try {
                    NativeCancellationBattleRuntime.tick(state);
                }
                catch (RuntimeException runtimeException) {
                    System.err.println("[LiTo] battle NPC expiry failed: " + String.valueOf(runtimeException));
                }
            }, 1L, 1L, TimeUnit.SECONDS);
            return true;
        }
        catch (RejectedExecutionException rejectedExecutionException) {
            return false;
        }
    }

    static void stopEmpty(State state) {
        if (state.leases.isEmpty() && state.pending.isEmpty() && state.task != null) {
            state.task.cancel(false);
            state.task = null;
        }
    }

    static List<Viewer> viewers(State state, Lease lease) {
        return (lease.kind == 1 ? state.hub.battleRecipients(lease.room, lease.force) : state.hub.roomMembers(lease.room)).stream().map(clientSession -> new Viewer((ClientSession)clientSession, NativePlayerCommit.Binding.capture(clientSession))).toList();
    }

    static Notice notice(State state, Lease lease, boolean bl) {
        byte[] byArray = bl ? Protocol.removeObject(lease.id) : (lease.kind == 1 ? Protocol.questMonster((BattleQuestEncounter.Snapshot)NativeCancellationBattleRuntime.snapshot(lease.actor, 1)) : Protocol.timeAttackActor((BattleTimeAttackEncounter.Snapshot)NativeCancellationBattleRuntime.snapshot(lease.actor, 2)));
        return new Notice(state, lease, bl, NativeCancellationBattleRuntime.viewers(state, lease), byArray);
    }

    static Object snapshot(Object object, int n) {
        return CombatAccess.call(object, "snapshot", "()Llocal/lito381/server/" + (n == 1 ? "BattleQuestEncounter" : "BattleTimeAttackEncounter") + "$Snapshot;", new Object[0]);
    }

    static void retire(Object object) {
        ((BattleTimedStatus)CombatAccess.field(object, "damagePoison")).clear();
        ((RuntimeInventory)CombatAccess.field(object, "inventory")).clear();
        ((Map)CombatAccess.field(object, "skillReadyNanos")).clear();
        ((Map)CombatAccess.field(object, "skillUseCounts")).clear();
        CombatAccess.set(object, "hp", 0);
        CombatAccess.set(object, "mp", 0);
        CombatAccess.set(object, "poisonAttackerObjectId", 0L);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void tick(State state) {
        ArrayList<Notice> arrayList = new ArrayList<Notice>();
        GameHub gameHub = state.hub;
        synchronized (gameHub) {
            if (state.closed) {
                return;
            }
            arrayList.addAll(state.pending);
            state.pending.clear();
            long l = System.nanoTime();
            Iterator<Lease> iterator = state.leases.iterator();
            while (iterator.hasNext()) {
                Lease lease = iterator.next();
                Object object = lease.encounter;
                synchronized (object) {
                    boolean bl;
                    boolean bl2 = bl = NativeCancellationBattleRuntime.room(state.hub, lease.room) != lease.roomObject || NativeCancellationBattleRuntime.encounter(lease.roomObject, lease.kind) != lease.encounter || (Boolean)CombatAccess.field(lease.roomObject, "battleStarted") == false;
                    if (bl) {
                        if (lease.actors.remove(lease.actor)) {
                            NativeCancellationBattleRuntime.retire(lease.actor);
                        }
                        iterator.remove();
                        continue;
                    }
                    if (!lease.actors.contains(lease.actor) || (Integer)CombatAccess.field(lease.actor, "hp") <= 0) {
                        iterator.remove();
                        continue;
                    }
                    if (lease.deadline == 0L || l - lease.deadline < 0L) {
                        continue;
                    }
                    lease.actors.remove(lease.actor);
                    NativeCancellationBattleRuntime.retire(lease.actor);
                    iterator.remove();
                    arrayList.add(NativeCancellationBattleRuntime.notice(state, lease, true));
                }
            }
            NativeCancellationBattleRuntime.stopEmpty(state);
        }
        arrayList.forEach(Notice::publish);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void close(GameHub gameHub) {
        GameHub gameHub2 = gameHub;
        synchronized (gameHub2) {
            State state = NativeCancellationBattleRuntime.state(gameHub);
            state.closed = true;
            if (state.task != null) {
                state.task.cancel(false);
                state.task = null;
            }
            for (Lease lease : state.leases) {
                Object object = lease.encounter;
                synchronized (object) {
                    if (lease.actors.remove(lease.actor)) {
                        NativeCancellationBattleRuntime.retire(lease.actor);
                    }
                }
            }
            state.leases.clear();
            state.pending.clear();
        }
    }

    static Object construct(String string, Class<?>[] classArray, Object ... objectArray) {
        try {
            Constructor<?> constructor = Class.forName(P + string).getDeclaredConstructor(classArray);
            constructor.setAccessible(true);
            return constructor.newInstance(objectArray);
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalStateException("battle NPC constructor contract", reflectiveOperationException);
        }
    }

    static List<Object> actors(Object object, int n, Object object2) {
        if (n == 2) {
            return (List)CombatAccess.field(object, "actors");
        }
        for (Object object3 : (Object[])CombatAccess.field(object, "lanes")) {
            List list = (List)CombatAccess.field(object3, "monsters");
            if (!list.contains(object2)) continue;
            return list;
        }
        return null;
    }

    static boolean replace(GameHub gameHub, long l, ClientSession clientSession, long l2, int n) {
        Notice notice;
        Notice notice2;
        Object object2;
        Object object3;
        int n2;
        NativeDeNpcTemplate nativeDeNpcTemplate;
        NativeCancellationBattleAction.Target target = NativeCancellationBattleAction.current.get();
        if (target == null || target.id() != l2 || target.npc().kind() != n || !target.valid()) {
            return false;
        }
        Object object4 = NativeCancellationBattleRuntime.room(gameHub, l);
        Object object5 = target.npc().owner();
        Object object6 = target.npc().actor();
        if (!(object4 != null && Thread.holdsLock(gameHub) && Thread.holdsLock(object5) && ((Boolean)CombatAccess.field(object4, "battleStarted")).booleanValue() && ((Set)CombatAccess.field(object4, "battleRoster")).contains(clientSession))) {
            return false;
        }
        Object object7 = NativeCancellationBattleRuntime.snapshot(object6, n);
        int n3 = n == 1 ? ((BattleQuestEncounter.Snapshot)object7).npcId() : ((BattleTimeAttackEncounter.Snapshot)object7).npcId();
        NativeCancellationNpcRules.Rule rule = NativeCancellationNpcRules.lookup(n3);
        if (rule == null || rule.type() != 2) {
            return false;
        }
        ClassicWorldMonsterCatalog.MonsterTemplate monsterTemplate = NativeCancellationNpcRuntime.template(rule.targetId());
        NativeDeNpcTemplateResources.Snapshot snapshot = NativeDeNpcTemplateResources.snapshot();
        NativeDeNpcTemplate nativeDeNpcTemplate2 = nativeDeNpcTemplate = snapshot == null ? null : snapshot.npcs().get(rule.targetId());
        if (monsterTemplate == null || nativeDeNpcTemplate == null || !"L1Monster".equals(nativeDeNpcTemplate.getImpl())) {
            return false;
        }
        List<Object> list = NativeCancellationBattleRuntime.actors(object5, n, object6);
        if (list == null || !list.contains(object6)) {
            return false;
        }
        int n4 = (Integer)CombatAccess.field(object6, "x");
        int n5 = (Integer)CombatAccess.field(object6, "y");
        int n6 = n2 = n == 1 ? (Integer)CombatAccess.field(object6, "force") : 0;
        if (n == 1) {
            var22_19 = new BattleQuestEncounter.Template(nativeDeNpcTemplate.get_npcId(), nativeDeNpcTemplate.get_gfxid(), nativeDeNpcTemplate.get_nameid(), nativeDeNpcTemplate.get_level(), nativeDeNpcTemplate.get_hp(), nativeDeNpcTemplate.get_mp(), nativeDeNpcTemplate.get_int());
            object3 = NativeCancellationBattleRuntime.construct("BattleQuestEncounter$Monster", new Class[]{Integer.TYPE, Integer.TYPE, BattleQuestEncounter.Template.class, Integer.TYPE, Integer.TYPE, Boolean.TYPE, String.class}, n2, (Integer)CombatAccess.field(object6, "roomNumber"), var22_19, n4, n5, (Boolean)CombatAccess.field(object6, "summoned"), "Source CANCELLATION type2");
        } else {
            var22_19 = new BattleTimeAttackEncounter.Template(nativeDeNpcTemplate.get_npcId(), nativeDeNpcTemplate.get_gfxid(), nativeDeNpcTemplate.get_nameid(), nativeDeNpcTemplate.get_level(), nativeDeNpcTemplate.get_hp(), nativeDeNpcTemplate.get_mp(), nativeDeNpcTemplate.get_int());
            object3 = NativeCancellationBattleRuntime.construct("BattleTimeAttackEncounter$Monster", new Class[]{BattleTimeAttackEncounter.Template.class, Integer.TYPE, Integer.TYPE, String.class, String.class}, var22_19, n4, n5, "Source CANCELLATION type2", CombatAccess.field(object6, "waveSource"));
            BattleTimeAttackEncounter.Snapshot snapshot2 = (BattleTimeAttackEncounter.Snapshot)NativeCancellationBattleRuntime.snapshot(object3, 2);
            object2 = new BattleTimeAttackEncounter.Snapshot(snapshot2.objectId(), snapshot2.npcId(), snapshot2.x(), snapshot2.y(), snapshot2.gfx(), snapshot2.name(), snapshot2.level(), snapshot2.hp(), snapshot2.maxHp(), snapshot2.alive(), (Boolean)CombatAccess.field(object6, "finalBoss"), snapshot2.activeAi(), snapshot2.currentMp(), snapshot2.maximumMp(), snapshot2.intelligence(), snapshot2.identitySource(), snapshot2.ordinaryWaveSource(), snapshot2.placementSource());
            object3 = NativeCancellationBattleRuntime.construct("BattleTimeAttackEncounter$Monster", new Class[]{BattleTimeAttackEncounter.Snapshot.class}, object2);
            CombatAccess.set(object3, "hp", nativeDeNpcTemplate.get_hp());
        }
        long l3 = (Long)CombatAccess.field(object3, "objectId");
        if (l3 <= 0L || l3 > 0xFFFFFFFFL || list.stream().anyMatch(object -> (Long)CombatAccess.field(object, "objectId") == l3)) {
            return false;
        }
        object2 = NativeCancellationBattleRuntime.state(gameHub);
        Lease lease = new Lease(l, object4, object5, object3, n, n2, list, rule.durationSeconds() == 0 ? 0L : System.nanoTime() + TimeUnit.SECONDS.toNanos(rule.durationSeconds()));
        Lease lease2 = new Lease(l, object4, object5, object6, n, n2, list, 0L);
        try {
            notice2 = NativeCancellationBattleRuntime.notice((State)object2, lease, false);
            notice = NativeCancellationBattleRuntime.notice((State)object2, lease2, true);
        }
        catch (IllegalArgumentException | IllegalStateException runtimeException) {
            return false;
        }
        if (!NativeCancellationBattleRuntime.start((State)object2)) {
            return false;
        }
        list.add(object3);
        list.remove(object6);
        NativeCancellationBattleRuntime.retire(object6);
        ((State)object2).leases.add(lease);
        NativeLightAction.finishScoped((arg_0, arg_1) -> NativeCancellationBattleRuntime.lambda$replace$3(clientSession, notice2, notice, gameHub, (State)object2, arg_0, arg_1));
        return true;
    }

    static BattleQuestEncounter.MagicCancellation cancelQuestMonsterMagic(GameHub gameHub, long l, ClientSession clientSession, long l2) {
        BattleQuestEncounter.Snapshot snapshot = gameHub.questMonster(l, l2);
        if (snapshot != null && NativeCancellationBattleRuntime.replace(gameHub, l, clientSession, l2, 1)) {
            return new BattleQuestEncounter.MagicCancellation(snapshot, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, new BattleElementalFallDownStatus.Cancellation(false, 0), false, false);
        }
        return gameHub.cancelQuestMonsterMagic(l, clientSession, l2);
    }

    static BattleTimeAttackEncounter.MagicCancellation cancelTimeAttackActorMagic(GameHub gameHub, long l, ClientSession clientSession, long l2) {
        BattleTimeAttackEncounter.Snapshot snapshot = gameHub.timeAttackActor(l, l2);
        if (snapshot != null && NativeCancellationBattleRuntime.replace(gameHub, l, clientSession, l2, 2)) {
            return new BattleTimeAttackEncounter.MagicCancellation(snapshot, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, new BattleElementalFallDownStatus.Cancellation(false, 0), false, false);
        }
        return gameHub.cancelTimeAttackActorMagic(l, clientSession, l2);
    }

    private NativeCancellationBattleRuntime() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static /* synthetic */ void lambda$replace$3(ClientSession clientSession, Notice notice, Notice notice2, GameHub gameHub, State state, boolean bl, Throwable throwable) {
        if (NativeRolePublication.allowPublication(clientSession)) {
            notice.publish();
            notice2.publish();
        } else {
            GameHub gameHub2 = gameHub;
            synchronized (gameHub2) {
                if (!state.closed) {
                    state.pending.add(notice);
                    state.pending.add(notice2);
                }
            }
        }
    }

    static final class State {
        final GameHub hub;
        final List<Lease> leases = new ArrayList<Lease>();
        final List<Notice> pending = new ArrayList<Notice>();
        ScheduledFuture<?> task;
        boolean closed;

        State(GameHub gameHub) {
            this.hub = gameHub;
        }
    }

    static final class Lease {
        final long room;
        final long id;
        final Object roomObject;
        final Object encounter;
        final Object actor;
        final int kind;
        final int force;
        final List<Object> actors;
        final long deadline;

        Lease(long l, Object object, Object object2, Object object3, int n, int n2, List<Object> list, long l2) {
            this.room = l;
            this.roomObject = object;
            this.encounter = object2;
            this.actor = object3;
            this.kind = n;
            this.force = n2;
            this.actors = list;
            this.deadline = l2;
            this.id = (Long)CombatAccess.field(object3, "objectId");
        }
    }

    record Notice(State state, Lease lease, boolean removed, List<Viewer> viewers, byte[] packet) {
        void publish() {
            if (Thread.holdsLock(this.state.hub) || Thread.holdsLock(this.lease.encounter)) {
                throw new IllegalStateException("battle NPC publication under scene ownership");
            }
            for (Viewer viewer : this.viewers) {
                Runnable[] runnableArray = new Runnable[1];
                NativeSourcePlayerActionLock.run(viewer.pc, () -> {
                    if (viewer.binding.matches(viewer.pc)) {
                        runnableArray[0] = NativeRolePublication.completion(viewer.pc, () -> {
                            boolean bl;
                            GameHub gameHub = this.state.hub;
                            synchronized (gameHub) {
                                Object object = this.lease.encounter;
                                synchronized (object) {
                                    bl = !this.state.closed && NativeCancellationBattleRuntime.room(this.state.hub, this.lease.room) == this.lease.roomObject && NativeCancellationBattleRuntime.encounter(this.lease.roomObject, this.lease.kind) == this.lease.encounter && viewer.binding.matches(viewer.pc) && String.valueOf(CombatAccess.field(viewer.pc, "phase")).equals("BATTLE") && (Long)CombatAccess.field(viewer.pc, "currentRoomId") == this.lease.room && (this.lease.kind == 2 || this.state.hub.battleRecipients(this.lease.room, this.lease.force).contains(viewer.pc)) && (this.removed ? !this.lease.actors.contains(this.lease.actor) : this.lease.actors.contains(this.lease.actor) && (Integer)CombatAccess.field(this.lease.actor, "hp") > 0);
                                }
                            }
                            if (bl) {
                                try {
                                    viewer.pc.nativeSelfOriginalSend(this.packet, "S_NPC battle cancellation " + (this.removed ? "remove" : "spawn") + "=" + this.lease.id);
                                }
                                catch (IOException iOException) {
                                    throw new UncheckedIOException(iOException);
                                }
                            }
                        });
                    }
                });
                if (runnableArray[0] == null) continue;
                runnableArray[0].run();
            }
        }
    }

    record Viewer(ClientSession pc, NativePlayerCommit.Binding binding) {
    }
}

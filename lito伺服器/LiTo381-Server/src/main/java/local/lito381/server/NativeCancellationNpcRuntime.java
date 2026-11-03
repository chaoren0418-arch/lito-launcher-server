// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import local.lito381.server.BattleElementalFallDownStatus;
import local.lito381.server.ClassicWorldMonsterCatalog;
import local.lito381.server.ClassicWorldMonsterPopulation;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatMonsterCurseLifecycle;
import local.lito381.server.CombatPoisonLifecycle;
import local.lito381.server.GameHub;
import local.lito381.server.NativeCancellationNpcMaterialAction;
import local.lito381.server.NativeCancellationNpcRules;
import local.lito381.server.NativeDeNpcTemplate;
import local.lito381.server.NativeDeNpcTemplateResources;
import local.lito381.server.NativeLightAction;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativeRolePublication;

final class NativeCancellationNpcRuntime {
    static final String P = "local/lito381/server/";

    static State state(GameHub gameHub) {
        if (gameHub.nativeCancellationNpcRuntime == null) {
            gameHub.nativeCancellationNpcRuntime = new State(gameHub);
        }
        return gameHub.nativeCancellationNpcRuntime;
    }

    static Notice notice(GameHub gameHub, ClassicWorldMonsterPopulation classicWorldMonsterPopulation, int n, int n2, ClassicWorldMonsterPopulation.Snapshot snapshot, Object object, boolean bl) {
        return new Notice(gameHub, classicWorldMonsterPopulation, n, n2, snapshot, object, bl, gameHub.classicWorldMembers(n, n2).stream().map(clientSession -> new Viewer((ClientSession)clientSession, NativePlayerCommit.Binding.capture(clientSession))).toList());
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
                    NativeCancellationNpcRuntime.tick(state);
                }
                catch (RuntimeException runtimeException) {
                    System.err.println("[LiTo] cancellation NPC cleanup failed: " + String.valueOf(runtimeException));
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
                ClassicWorldMonsterPopulation classicWorldMonsterPopulation;
                Lease lease = iterator.next();
                ClassicWorldMonsterPopulation classicWorldMonsterPopulation2 = classicWorldMonsterPopulation = lease.population;
                synchronized (classicWorldMonsterPopulation2) {
                    Object object = NativeCancellationNpcMaterialAction.actor(classicWorldMonsterPopulation, lease.id);
                    if (NativeCancellationNpcMaterialAction.population(state.hub, lease.map, lease.instance) != classicWorldMonsterPopulation || object != lease.actor) {
                        iterator.remove();
                        continue;
                    }
                    ClassicWorldMonsterPopulation.Snapshot snapshot = classicWorldMonsterPopulation.actor(lease.map, lease.id);
                    if (lease.respawn) {
                        if (snapshot == null || snapshot.present() || snapshot.alive()) {
                            iterator.remove();
                            continue;
                        }
                        if (l - lease.deadline < 0L) {
                            continue;
                        }
                        var11_10 = classicWorldMonsterPopulation.respawn(lease.map, lease.id);
                        iterator.remove();
                        if (var11_10 != null) {
                            arrayList.add(NativeCancellationNpcRuntime.notice(state.hub, classicWorldMonsterPopulation, lease.map, lease.instance, ((ClassicWorldMonsterPopulation.Respawn)var11_10).after(), object, false));
                            for (ClassicWorldMonsterPopulation.Snapshot snapshot2 : ((ClassicWorldMonsterPopulation.Respawn)var11_10).groupMembers()) {
                                arrayList.add(NativeCancellationNpcRuntime.notice(state.hub, classicWorldMonsterPopulation, lease.map, lease.instance, snapshot2, NativeCancellationNpcMaterialAction.actor(classicWorldMonsterPopulation, snapshot2.objectId()), false));
                            }
                        }
                    } else {
                        if (snapshot == null || !snapshot.alive() || !snapshot.present()) {
                            iterator.remove();
                            continue;
                        }
                        if (lease.deadline == 0L || l - lease.deadline < 0L) {
                            continue;
                        }
                        var11_10 = classicWorldMonsterPopulation.nativeCancellationDelete(lease.map, lease.id);
                        iterator.remove();
                        if (var11_10 != null) {
                            arrayList.add(NativeCancellationNpcRuntime.notice(state.hub, classicWorldMonsterPopulation, lease.map, lease.instance, (ClassicWorldMonsterPopulation.Snapshot)var11_10, object, true));
                        }
                    }
                }
            }
            NativeCancellationNpcRuntime.stopEmpty(state);
        }
        arrayList.forEach(Notice::publish);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void close(GameHub gameHub) {
        GameHub gameHub2 = gameHub;
        synchronized (gameHub2) {
            State state = NativeCancellationNpcRuntime.state(gameHub);
            state.closed = true;
            if (state.task != null) {
                state.task.cancel(false);
                state.task = null;
            }
            for (Lease lease : state.leases) {
                if (lease.respawn) continue;
                ClassicWorldMonsterPopulation classicWorldMonsterPopulation = lease.population;
                synchronized (classicWorldMonsterPopulation) {
                    if (NativeCancellationNpcMaterialAction.actor(lease.population, lease.id) == lease.actor) {
                        lease.population.nativeCancellationDelete(lease.map, lease.id);
                    }
                }
            }
            state.leases.clear();
            state.pending.clear();
        }
    }

    static ClassicWorldMonsterCatalog.MonsterTemplate template(int n) {
        NativeDeNpcTemplate nativeDeNpcTemplate;
        NativeDeNpcTemplateResources.Snapshot snapshot = NativeDeNpcTemplateResources.snapshot();
        NativeDeNpcTemplate nativeDeNpcTemplate2 = nativeDeNpcTemplate = snapshot == null ? null : snapshot.npcs().get(n);
        if (nativeDeNpcTemplate == null) {
            return null;
        }
        if (nativeDeNpcTemplate.get_randomlevel() != 0 || nativeDeNpcTemplate.get_randomhp() != 0 || nativeDeNpcTemplate.get_randommp() != 0 || nativeDeNpcTemplate.get_randomac() != 0 || nativeDeNpcTemplate.get_randomexp() != 0 || nativeDeNpcTemplate.get_randomlawful() != 0 || nativeDeNpcTemplate.get_classname() != null && !nativeDeNpcTemplate.get_classname().isBlank() && !nativeDeNpcTemplate.get_classname().equals("0")) {
            return null;
        }
        try {
            return new ClassicWorldMonsterCatalog.MonsterTemplate(nativeDeNpcTemplate.get_npcId(), nativeDeNpcTemplate.get_nameid(), nativeDeNpcTemplate.getImpl(), nativeDeNpcTemplate.get_gfxid(), nativeDeNpcTemplate.get_level(), nativeDeNpcTemplate.get_hp(), nativeDeNpcTemplate.get_mp(), nativeDeNpcTemplate.get_ac(), nativeDeNpcTemplate.get_str(), nativeDeNpcTemplate.get_con(), nativeDeNpcTemplate.get_dex(), nativeDeNpcTemplate.get_wis(), nativeDeNpcTemplate.get_int(), nativeDeNpcTemplate.get_mr(), nativeDeNpcTemplate.get_exp(), nativeDeNpcTemplate.get_lawful(), nativeDeNpcTemplate.get_size(), nativeDeNpcTemplate.get_weakAttr(), nativeDeNpcTemplate.get_ranged(), nativeDeNpcTemplate.get_passispeed(), nativeDeNpcTemplate.get_atkspeed(), nativeDeNpcTemplate.getBowActId(), nativeDeNpcTemplate.get_undead(), nativeDeNpcTemplate.get_poisonatk(), nativeDeNpcTemplate.get_paralysisatk(), nativeDeNpcTemplate.is_agro(), nativeDeNpcTemplate.get_damagereduction(), nativeDeNpcTemplate.is_hard(), nativeDeNpcTemplate.is_doppel(), nativeDeNpcTemplate.isBoss(), nativeDeNpcTemplate.getLightSize(), nativeDeNpcTemplate.getChangeHead());
        }
        catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }

    static ClassicWorldMonsterPopulation.MagicCancellation magicWorld(GameHub gameHub, int n, int n2, long l, ClientSession clientSession, CombatPoisonLifecycle.Scope scope) {
        ClassicWorldMonsterPopulation classicWorldMonsterPopulation = NativeCancellationNpcMaterialAction.population(gameHub, n, n2);
        if (classicWorldMonsterPopulation != null && Thread.holdsLock(gameHub) && Thread.holdsLock(classicWorldMonsterPopulation)) {
            NativeCancellationNpcRules.Rule rule;
            ClassicWorldMonsterPopulation.Snapshot snapshot = classicWorldMonsterPopulation.actor(n, l);
            NativeCancellationNpcRules.Rule rule2 = rule = snapshot == null ? null : NativeCancellationNpcRules.lookup(snapshot.npcId());
            if (snapshot != null && snapshot.alive() && snapshot.present() && rule != null && rule.type() == 2) {
                ClassicWorldMonsterCatalog.MonsterTemplate monsterTemplate = NativeCancellationNpcRuntime.template(rule.targetId());
                State state = NativeCancellationNpcRuntime.state(gameHub);
                if (monsterTemplate != null && NativeCancellationNpcRuntime.start(state)) {
                    Object object = NativeCancellationNpcMaterialAction.actor(classicWorldMonsterPopulation, l);
                    ClassicWorldMonsterPopulation.Snapshot snapshot2 = classicWorldMonsterPopulation.nativeCancellationSpawn(n, l, monsterTemplate);
                    if (snapshot2 != null) {
                        ClassicWorldMonsterPopulation.Snapshot snapshot3 = classicWorldMonsterPopulation.nativeCancellationDelete(n, l);
                        if (snapshot3 == null) {
                            classicWorldMonsterPopulation.nativeCancellationDelete(n, snapshot2.objectId());
                            throw new IllegalStateException("locked replacement lost original NPC");
                        }
                        long l2 = System.nanoTime();
                        state.leases.add(new Lease(classicWorldMonsterPopulation, n, n2, snapshot2.objectId(), rule.durationSeconds() == 0 ? 0L : l2 + TimeUnit.SECONDS.toNanos(rule.durationSeconds()), false));
                        int n3 = classicWorldMonsterPopulation.nextRespawnDelaySeconds(n, l);
                        if (n3 >= 0) {
                            state.leases.add(new Lease(classicWorldMonsterPopulation, n, n2, l, l2 + TimeUnit.SECONDS.toNanos(n3), true));
                        }
                        Notice notice = NativeCancellationNpcRuntime.notice(gameHub, classicWorldMonsterPopulation, n, n2, snapshot2, NativeCancellationNpcMaterialAction.actor(classicWorldMonsterPopulation, snapshot2.objectId()), false);
                        Notice notice2 = NativeCancellationNpcRuntime.notice(gameHub, classicWorldMonsterPopulation, n, n2, snapshot3, object, true);
                        NativeLightAction.finishScoped((bl, throwable) -> {
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
                        });
                        return new ClassicWorldMonsterPopulation.MagicCancellation(snapshot, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, new BattleElementalFallDownStatus.Cancellation(false, 0), false, false, false, false);
                    }
                    NativeCancellationNpcRuntime.stopEmpty(state);
                }
            }
        }
        return CombatMonsterCurseLifecycle.magicWorld(gameHub, n, n2, l, clientSession, scope);
    }

    private NativeCancellationNpcRuntime() {
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

    record Notice(GameHub hub, ClassicWorldMonsterPopulation population, int map, int instance, ClassicWorldMonsterPopulation.Snapshot snapshot, Object actor, boolean removed, List<Viewer> viewers) {
        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void publish() {
            if (Thread.holdsLock(this.hub) || Thread.holdsLock(this.population)) {
                throw new IllegalStateException("NPC publication under scene ownership");
            }
            for (Viewer viewer : this.viewers) {
                boolean bl;
                GameHub gameHub = this.hub;
                synchronized (gameHub) {
                    ClassicWorldMonsterPopulation classicWorldMonsterPopulation = this.population;
                    synchronized (classicWorldMonsterPopulation) {
                        Object object = NativeCancellationNpcMaterialAction.actor(this.population, this.snapshot.objectId());
                        bl = !NativeCancellationNpcRuntime.state((GameHub)this.hub).closed && NativeCancellationNpcMaterialAction.population(this.hub, this.map, this.instance) == this.population && viewer.binding.matches(viewer.pc) && String.valueOf(CombatAccess.field(viewer.pc, "phase")).equals("WORLD") && (Integer)CombatAccess.field(viewer.pc, "classicWorldMapId") == this.map && (Integer)CombatAccess.field(viewer.pc, "classicWorldInstanceId") == this.instance && (this.removed ? object == null || object == this.actor && (Boolean)CombatAccess.field(object, "present") == false : object == this.actor && (Boolean)CombatAccess.field(object, "present") != false && (Boolean)CombatAccess.field(object, "alive") != false);
                    }
                }
                if (!bl) continue;
                try {
                    if (this.removed) {
                        viewer.pc.serverClassicWorldMonsterRemoved(this.snapshot);
                        continue;
                    }
                    viewer.pc.serverClassicWorldMonsterSpawned(this.snapshot);
                }
                catch (IOException iOException) {
                    System.err.println("[LiTo] cancellation NPC publication failed: " + iOException.getClass().getSimpleName());
                }
            }
        }
    }

    static final class Lease {
        final ClassicWorldMonsterPopulation population;
        final int map;
        final int instance;
        final long id;
        final long deadline;
        final Object actor;
        final boolean respawn;

        Lease(ClassicWorldMonsterPopulation classicWorldMonsterPopulation, int n, int n2, long l, long l2, boolean bl) {
            this.population = classicWorldMonsterPopulation;
            this.map = n;
            this.instance = n2;
            this.id = l;
            this.deadline = l2;
            this.respawn = bl;
            this.actor = NativeCancellationNpcMaterialAction.actor(classicWorldMonsterPopulation, l);
        }
    }

    record Viewer(ClientSession pc, NativePlayerCommit.Binding binding) {
    }
}

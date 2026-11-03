package local.lito381.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Source pet/summon recovery; actual identities survive lock waits in BATTLE and WORLD. */
final class NativeCloseoutCompanionRecovery {
    static final String CLOCK = "nativeCloseoutCompanionRecoveryClock";
    static final ThreadLocal<Value> output = new ThreadLocal<>();
    record First(GameHub hub, NativePlayerLifetime lifetime, Object room, ClientSession owner,
                 NativePlayerCommit.Binding binding, BattleCompanion pet,
                 Object petLife, BattleCompanion.Profile profile, NativeSourceNpcFactoryType.Stamp factory,
                 List<BattleCompanion> bag, NativeCompanionPlacement.State placement, Object scene, Object master,
                 long id, NativeCloseoutNpcRecovery.Data data) {}
    /** No Context, admission object or full owner/pet roster is retained for output. */
    record Value(GameHub hub, NativePlayerLifetime lifetime, Object room, ClientSession owner,
                 NativePlayerCommit.Binding binding, BattleCompanion pet,
                 Object petLife, BattleCompanion.Profile profile, NativeSourceNpcFactoryType.Stamp factory,
                 NativeCompanionPlacement.State placement, Object scene, Object master, long id, int ratio) {}
    static final class Clock implements Runnable {
        final GameHub hub;
        final NativePlayerLifetime lifetime;
        ScheduledFuture<?> future;
        int seconds;
        Clock(GameHub hub, NativePlayerLifetime lifetime) { this.hub = hub; this.lifetime = lifetime; }
        @Override public void run() {
            try {
                if (CombatAccess.field(hub, CLOCK) != this) return;
                int tick = ++seconds;
                if (!lifetime.admits()) return;
                var values = collect(this, tick);
                for (int i = 0; i < values.size(); i++) {
                    var value = values.set(i, null);
                    send(value);
                }
            } catch (RuntimeException failure) { NativeSourcePhysicalNpc.failed(lifetime); }
        }
    }
    static void init(GameHub hub) {
        if (!CombatRules.loaded()) return;
        var lifetime = NativePlayerLifetime.of(hub);
        synchronized (lifetime.admission) { synchronized (hub) {
            if (CombatAccess.field(hub, CLOCK) != null) throw new IllegalStateException("companion recovery already installed");
            var clock = new Clock(hub, lifetime); CombatAccess.set(hub, CLOCK, clock);
            try { clock.future = ((ScheduledExecutorService) CombatAccess.field(hub, "battleClock"))
                    .scheduleAtFixedRate(clock, 0, 1, TimeUnit.SECONDS); }
            catch (RuntimeException failure) { CombatAccess.set(hub, CLOCK, null); throw failure; }
        }}
    }
    static void close(GameHub hub) {
        var value = CombatAccess.field(hub, CLOCK);
        if (!(value instanceof Clock clock)) return;
        synchronized (clock.lifetime.admission) { synchronized (hub) {
            if (CombatAccess.field(hub, CLOCK) != clock) return;
            CombatAccess.set(hub, CLOCK, null);
            if (clock.future != null) clock.future.cancel(false);
            clock.future = null;
        }}
    }
    static boolean roomCurrent(Object room) {
        return room != null && (boolean) CombatAccess.field(room, "battleStarted")
                && !(boolean) CombatAccess.field(room, "disconnectResolutionPending")
                && ((BattleRules.MatchState) CombatAccess.field(room, "match")).phase() == BattleRules.Phase.RUNNING;
    }
    static ArrayList<Value> collect(Clock clock, int tick) {
        var result = new ArrayList<Value>();
        for (var owner : clock.hub.connectedSessionsSnapshot()) {
            var binding = NativePlayerCommit.Binding.capture(owner);
            boolean battle = binding.phase().equals("BATTLE");
            if ((!battle && !binding.phase().equals("WORLD")) || CombatAccess.field(owner, "hub") != clock.hub) continue;
            Object room = null;
            synchronized (clock.hub) {
                if (battle) { room = ((Map<?, ?>) CombatAccess.field(clock.hub, "rooms")).get(binding.room()); if (!roomCurrent(room)) continue; }
                else if (!worldRegistered(clock.hub, owner, binding)) continue;
            }
            var bag = NativeBattleTamedLifecycle.pets(owner);
            List<BattleCompanion> pets;
            synchronized (bag) { pets = List.copyOf(bag); }
            for (var pet : pets) {
                First first;
                synchronized (pet) {
                    var factory = NativeSourceNpcFactoryType.stamp(pet);
                    var snapshot = pet.snapshot();
                    if (!snapshot.active() || snapshot.hp() <= 0 || factory == null
                            || (factory.kind() != 5 && factory.kind() != 6) || factory.npc() != snapshot.npcId()
                            || !(factory.nativeCloseoutNpcRecovery instanceof NativeCloseoutNpcRecovery.Data data)) continue;
                    if (tick % data.hpInterval() != 0 && tick % data.mpInterval() != 0) continue;
                    var placement = NativeCompanionPlacement.state(pet);
                    first = new First(clock.hub, clock.lifetime, room, owner, binding, pet,
                            NativeSourceControlledDisplay.rawLife(pet), (BattleCompanion.Profile) CombatAccess.field(pet, "profile"), factory,
                            bag, placement, placement == null ? null : placement.scene, placement == null ? null : placement.master,
                            snapshot.objectId(), data);
                }
                var value = apply(first, tick);
                if (value != null) result.add(value);
            }
        }
        return result;
    }
    /** Called only while holding the original Hub monitor; no lookup is repeated during output. */
    static boolean worldRegistered(GameHub hub, ClientSession owner, NativePlayerCommit.Binding binding) {
        var location = hub.classicWorldLocation(owner);
        return location != null && location.mapId() == binding.map() && location.instanceId() == binding.instance();
    }
    static boolean ownerCurrent(GameHub hub, NativePlayerLifetime lifetime, Object room, ClientSession owner,
                                NativePlayerCommit.Binding binding) {
        return lifetime.admits() && !(boolean) CombatAccess.field(hub, "nativeStaticMotionClosed")
                && (binding.phase().equals("WORLD") || binding.phase().equals("BATTLE") && roomCurrent(room))
                && CombatAccess.field(owner, "hub") == hub
                && NativeAcceptedFreezeMutation.roleFrame(binding, owner)
                && !(boolean) CombatAccess.field(owner, "sourceItemDisconnected")
                && CombatAccess.field(hub, CLOCK) instanceof Clock;
    }
    static boolean petCurrent(BattleCompanion pet, Object life, Object profile, Object factory,
                              NativeCompanionPlacement.State placement, Object scene, Object master, ClientSession owner) {
        if (!(boolean) CombatAccess.field(pet, "active") || NativeSourceRawDamage.number(pet, "hp") <= 0
                || NativeSourceControlledDisplay.rawLife(pet) != life || CombatAccess.field(pet, "profile") != profile
                || NativeSourceNpcFactoryType.stamp(pet) != factory || NativeCompanionPlacement.state(pet) != placement) return false;
        return placement == null || placement.owner.get() == owner && placement.scene == scene && placement.master == master && placement.pending == null;
    }
    static Value apply(First f, int tick) {
        Value[] result = {null};
        NativeSourcePlayerActionLock.runPairOrdered(f.owner(), f.owner(), () -> {
            synchronized (f.lifetime().admission) { synchronized (f.hub()) {
                if (!ownerCurrent(f.hub(), f.lifetime(), f.room(), f.owner(), f.binding())
                        || !f.hub().connectedSessionsSnapshot().contains(f.owner())
                        || NativeBattleTamedLifecycle.pets(f.owner()) != f.bag()) return;
                if (f.binding().phase().equals("BATTLE")) {
                    if (((Map<?, ?>) CombatAccess.field(f.hub(), "rooms")).get(f.binding().room()) != f.room()) return;
                } else if (!worldRegistered(f.hub(), f.owner(), f.binding())) return;
                synchronized (f.bag()) {
                    if (!f.bag().contains(f.pet())) return;
                    synchronized (f.pet()) { synchronized ((BattleTimedStatus) CombatAccess.field(f.pet(), "timedStatus")) {
                        if (f.binding().phase().equals("WORLD") && f.placement() != null
                                && (!f.placement().role(f.owner()) || !f.placement().scene.alive(f.hub())
                                || !f.placement().scene.at(f.binding().map(), f.binding().instance()))) return;
                        if (!petCurrent(f.pet(), f.petLife(), f.profile(), f.factory(), f.placement(), f.scene(), f.master(), f.owner())) return;
                        int hp = NativeSourceRawDamage.number(f.pet(), "hp"), maxHp = f.profile().maxHp();
                        if (maxHp <= 0) return;
                        int nextHp = hp;
                        if (hp < maxHp && tick % f.data().hpInterval() == 0)
                            nextHp = NativeCloseoutNpcRecovery.bounded(hp, f.data().hpAmount(), maxHp);
                        int mp = NativeSourceRawDamage.number(f.pet(), "mp"), maxMp = f.profile().maxMp();
                        if (maxMp > 0 && mp < maxMp && tick % f.data().mpInterval() == 0)
                            CombatAccess.set(f.pet(), "mp", NativeCloseoutNpcRecovery.bounded(mp, f.data().mpAmount(), maxMp));
                        if (nextHp != hp) {
                            CombatAccess.set(f.pet(), "hp", nextHp);
                            result[0] = new Value(f.hub(), f.lifetime(), f.room(), f.owner(), f.binding(), f.pet(),
                                    f.petLife(), f.profile(), f.factory(), f.placement(), f.scene(), f.master(), f.id(), ratio(nextHp, maxHp));
                        }
                    }}
                }
            }}
        });
        return result[0];
    }
    static int ratio(int hp, int maximum) { return maximum <= 0 ? 0 : (int) Math.max(0L, Math.min(100L, 100L * hp / maximum)); }
    static boolean current(Value value) {
        return ownerCurrent(value.hub(), value.lifetime(), value.room(), value.owner(), value.binding())
                && petCurrent(value.pet(), value.petLife(), value.profile(), value.factory(), value.placement(), value.scene(), value.master(), value.owner())
                && ratio(NativeSourceRawDamage.number(value.pet(), "hp"), value.profile().maxHp()) == value.ratio();
    }
    static void send(Value value) {
        if (value == null || !current(value)) return;
        var previous = output.get(); var oldContext = NativeAreaMaterialAction.current.get();
        NativeAreaMaterialAction.current.remove(); output.set(value);
        try { NativePlayerCommit.completionFrame(null, () -> {
            try { synchronized (value.owner()) {
                if (current(value)) value.owner().nativeSelfOriginalSend(Protocol.hitRatio(value.id(), value.ratio(), 100), "S_HIT_RATIO Source companion recovery");
            }} catch (IOException | RuntimeException failure) { NativeSourcePhysicalNpc.failed(value.lifetime()); }
        }); }
        finally {
            if (previous == null) output.remove(); else output.set(previous);
            if (oldContext == null) NativeAreaMaterialAction.current.remove(); else NativeAreaMaterialAction.current.set(oldContext);
        }
    }
    static boolean allowFrame(boolean original, ClientSession pc, byte[] packet) {
        if (!original) return false;
        var value = output.get();
        return value == null || value.owner() == pc && current(value) && packet != null && packet.length == 6
                && (packet[0] & 255) == Protocol.S_HIT_RATIO && NativeSourcePhysicalNpc.object(packet) == value.id()
                && (packet[5] & 255) == value.ratio();
    }
    private NativeCloseoutCompanionRecovery() {}
}

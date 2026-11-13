package local.lito381.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * T135 (#386), ported from candidate 046c58c5: Source pet / summon natural HP and MP recovery, and
 * the owner-only HP meter that follows an HP change.
 *
 * <p>Source ({@code 1-伺服器}): {@code timecontroller/StartTimer_Pet.java:83-92} starts four
 * one-second clocks at delay 0; each run increments its counter first and then calls {@code
 * HprPet.hpUpdate} / {@code MprPet.mpUpdate} for every pet and summon. {@code
 * L1PetInstance.java:1093} / {@code L1SummonInstance.java:1465} send {@code S_HPMeter(id, 100 * hp /
 * maxHp)} to the master only, and only when HP changed.
 *
 * <p>One clock per Hub on the existing {@code battleClock}; no actor registry. The identities
 * captured before the owner / admission / Hub / actor lock waits are compared again under those
 * locks and once more inside the final frame, so a pet that died, was dismissed or changed owner
 * while the tick waited neither recovers nor produces a meter.
 */
final class NativeCloseoutCompanionRecovery {
  static final String CLOCK = "nativeCloseoutCompanionRecoveryClock";
  static final ThreadLocal<Value> output = new ThreadLocal<>();

  record First(
      GameHub hub,
      NativePlayerLifetime lifetime,
      Object room,
      ClientSession owner,
      NativePlayerCommit.Binding binding,
      BattleCompanion pet,
      Object petLife,
      BattleCompanion.Profile profile,
      NativeSourceNpcFactoryType.Stamp factory,
      List<BattleCompanion> bag,
      NativeCompanionPlacement.State placement,
      Object scene,
      Object master,
      long id,
      NativeCloseoutCompanionRecoveryData.Data data) {}

  /** No Context, admission object or full owner / pet roster is retained for output. */
  record Value(
      GameHub hub,
      NativePlayerLifetime lifetime,
      Object room,
      ClientSession owner,
      NativePlayerCommit.Binding binding,
      BattleCompanion pet,
      Object petLife,
      BattleCompanion.Profile profile,
      NativeSourceNpcFactoryType.Stamp factory,
      NativeCompanionPlacement.State placement,
      Object scene,
      Object master,
      long id,
      int ratio) {}

  static final class Clock implements Runnable {
    final GameHub hub;
    final NativePlayerLifetime lifetime;
    ScheduledFuture<?> future;
    int seconds;

    Clock(GameHub hub, NativePlayerLifetime lifetime) {
      this.hub = hub;
      this.lifetime = lifetime;
    }

    @Override
    public void run() {
      try {
        if (CombatAccess.field(hub, CLOCK) != this) return;
        // Source increments before use: the first run is tick 1 (PetHprTimer.java:92).
        int tick = ++seconds;
        if (!lifetime.admits()) return;
        var values = collect(this, tick);
        for (int i = 0; i < values.size(); i++) {
          var value = values.set(i, null);
          send(value);
        }
      } catch (RuntimeException failure) {
        NativeSourcePhysicalNpc.failed(lifetime);
      }
    }
  }

  static void init(GameHub hub) {
    if (!CombatRules.loaded()) return;
    var lifetime = NativePlayerLifetime.of(hub);
    synchronized (lifetime.admission) {
      synchronized (hub) {
        if (CombatAccess.field(hub, CLOCK) != null)
          throw new IllegalStateException("companion recovery already installed");
        var clock = new Clock(hub, lifetime);
        CombatAccess.set(hub, CLOCK, clock);
        try {
          // StartTimer_Pet.java:83-92: initial delay 0, period 1000 ms.
          clock.future =
              ((ScheduledExecutorService) CombatAccess.field(hub, "battleClock"))
                  .scheduleAtFixedRate(clock, 0, 1, TimeUnit.SECONDS);
        } catch (RuntimeException failure) {
          CombatAccess.set(hub, CLOCK, null);
          throw failure;
        }
      }
    }
  }

  static void close(GameHub hub) {
    var value = CombatAccess.field(hub, CLOCK);
    if (!(value instanceof Clock clock)) return;
    synchronized (clock.lifetime.admission) {
      synchronized (hub) {
        if (CombatAccess.field(hub, CLOCK) != clock) return;
        CombatAccess.set(hub, CLOCK, null);
        if (clock.future != null) clock.future.cancel(false);
        clock.future = null;
      }
    }
  }

  static boolean roomCurrent(Object room) {
    return room != null
        && (boolean) CombatAccess.field(room, "battleStarted")
        && !(boolean) CombatAccess.field(room, "disconnectResolutionPending")
        && ((BattleRules.MatchState) CombatAccess.field(room, "match")).phase()
            == BattleRules.Phase.RUNNING;
  }

  static ArrayList<Value> collect(Clock clock, int tick) {
    var result = new ArrayList<Value>();
    for (var owner : clock.hub.connectedSessionsSnapshot()) {
      var binding = NativePlayerCommit.Binding.capture(owner);
      boolean battle = binding.phase().equals("BATTLE");
      if ((!battle && !binding.phase().equals("WORLD"))
          || CombatAccess.field(owner, "hub") != clock.hub) continue;
      Object room = null;
      synchronized (clock.hub) {
        if (battle) {
          room = ((Map<?, ?>) CombatAccess.field(clock.hub, "rooms")).get(binding.room());
          if (!roomCurrent(room)) continue;
        } else if (!worldRegistered(clock.hub, owner, binding)) continue;
      }
      var bag = NativeBattleTamedLifecycle.pets(owner);
      List<BattleCompanion> pets;
      synchronized (bag) {
        pets = List.copyOf(bag);
      }
      for (var pet : pets) {
        First first;
        synchronized (pet) {
          var factory = NativeSourceNpcFactoryType.stamp(pet);
          var snapshot = pet.snapshot();
          // HprPet.java:28-38 / MprPet.java:32-42: dead or removed actors never recover.
          if (!snapshot.active()
              || snapshot.hp() <= 0
              || factory == null
              || (factory.kind() != 5 && factory.kind() != 6)
              || factory.npc() != snapshot.npcId()
              || !(factory.nativeCloseoutNpcRecovery
                  instanceof NativeCloseoutCompanionRecoveryData.Data data)) continue;
          if (tick % data.hpInterval() != 0 && tick % data.mpInterval() != 0) continue;
          var placement = NativeCompanionPlacement.state(pet);
          first =
              new First(
                  clock.hub,
                  clock.lifetime,
                  room,
                  owner,
                  binding,
                  pet,
                  NativeSourceControlledDisplay.rawLife(pet),
                  (BattleCompanion.Profile) CombatAccess.field(pet, "profile"),
                  factory,
                  bag,
                  placement,
                  placement == null ? null : placement.scene,
                  placement == null ? null : placement.master,
                  snapshot.objectId(),
                  data);
        }
        var value = apply(first, tick);
        if (value != null) result.add(value);
      }
    }
    return result;
  }

  /** Called only while holding the original Hub monitor; no lookup is repeated during output. */
  static boolean worldRegistered(
      GameHub hub, ClientSession owner, NativePlayerCommit.Binding binding) {
    var location = hub.classicWorldLocation(owner);
    return location != null
        && location.mapId() == binding.map()
        && location.instanceId() == binding.instance();
  }

  static boolean ownerCurrent(
      GameHub hub,
      NativePlayerLifetime lifetime,
      Object room,
      ClientSession owner,
      NativePlayerCommit.Binding binding) {
    return lifetime.admits()
        && !(boolean) CombatAccess.field(hub, "nativeStaticMotionClosed")
        && (binding.phase().equals("WORLD") || binding.phase().equals("BATTLE") && roomCurrent(room))
        && CombatAccess.field(owner, "hub") == hub
        && NativeAcceptedFreezeMutation.roleFrame(binding, owner)
        && !(boolean) CombatAccess.field(owner, "sourceItemDisconnected")
        && CombatAccess.field(hub, CLOCK) instanceof Clock;
  }

  static boolean petCurrent(
      BattleCompanion pet,
      Object life,
      Object profile,
      Object factory,
      NativeCompanionPlacement.State placement,
      Object scene,
      Object master,
      ClientSession owner) {
    if (!(boolean) CombatAccess.field(pet, "active")
        || NativeSourceRawDamage.number(pet, "hp") <= 0
        || NativeSourceControlledDisplay.rawLife(pet) != life
        || CombatAccess.field(pet, "profile") != profile
        || NativeSourceNpcFactoryType.stamp(pet) != factory
        || NativeCompanionPlacement.state(pet) != placement) return false;
    return placement == null
        || placement.owner.get() == owner
            && placement.scene == scene
            && placement.master == master
            && placement.pending == null;
  }

  static Value apply(First f, int tick) {
    Value[] result = {null};
    NativeSourcePlayerActionLock.runPairOrdered(
        f.owner(),
        f.owner(),
        () -> {
          synchronized (f.lifetime().admission) {
            synchronized (f.hub()) {
              if (!ownerCurrent(f.hub(), f.lifetime(), f.room(), f.owner(), f.binding())
                  || !f.hub().connectedSessionsSnapshot().contains(f.owner())
                  || NativeBattleTamedLifecycle.pets(f.owner()) != f.bag()) return;
              if (f.binding().phase().equals("BATTLE")) {
                if (((Map<?, ?>) CombatAccess.field(f.hub(), "rooms")).get(f.binding().room())
                    != f.room()) return;
              } else if (!worldRegistered(f.hub(), f.owner(), f.binding())) return;
              synchronized (f.bag()) {
                if (!f.bag().contains(f.pet())) return;
                synchronized (f.pet()) {
                  synchronized ((BattleTimedStatus) CombatAccess.field(f.pet(), "timedStatus")) {
                    if (f.binding().phase().equals("WORLD")
                        && f.placement() != null
                        && (!f.placement().role(f.owner())
                            || !f.placement().scene.alive(f.hub())
                            || !f.placement()
                                .scene
                                .at(f.binding().map(), f.binding().instance()))) return;
                    if (!petCurrent(
                        f.pet(),
                        f.petLife(),
                        f.profile(),
                        f.factory(),
                        f.placement(),
                        f.scene(),
                        f.master(),
                        f.owner())) return;
                    int hp = NativeSourceRawDamage.number(f.pet(), "hp");
                    int maxHp = f.profile().maxHp();
                    if (maxHp <= 0) return; // HprPet.java:24 / MprPet.java:24
                    int nextHp = hp;
                    if (hp < maxHp && tick % f.data().hpInterval() == 0)
                      nextHp = bounded(hp, f.data().hpAmount(), maxHp);
                    int mp = NativeSourceRawDamage.number(f.pet(), "mp");
                    int maxMp = f.profile().maxMp();
                    if (maxMp > 0 && mp < maxMp && tick % f.data().mpInterval() == 0)
                      CombatAccess.set(f.pet(), "mp", bounded(mp, f.data().mpAmount(), maxMp));
                    if (nextHp != hp) {
                      CombatAccess.set(f.pet(), "hp", nextHp);
                      result[0] =
                          new Value(
                              f.hub(),
                              f.lifetime(),
                              f.room(),
                              f.owner(),
                              f.binding(),
                              f.pet(),
                              f.petLife(),
                              f.profile(),
                              f.factory(),
                              f.placement(),
                              f.scene(),
                              f.master(),
                              f.id(),
                              ratio(nextHp, maxHp));
                    }
                  }
                }
              }
            }
          }
        });
    return result[0];
  }

  /** Source {@code adjustCurrentHp} / {@code adjustCurrentMp}: never above the maximum. */
  static int bounded(int current, int amount, int maximum) {
    return (int) Math.max(0L, Math.min((long) maximum, (long) current + amount));
  }

  /** Source {@code 100 * hp / maxHp} with integer truncation. */
  static int ratio(int hp, int maximum) {
    return maximum <= 0 ? 0 : (int) Math.max(0L, Math.min(100L, 100L * hp / maximum));
  }

  static boolean current(Value value) {
    return ownerCurrent(value.hub(), value.lifetime(), value.room(), value.owner(), value.binding())
        && petCurrent(
            value.pet(),
            value.petLife(),
            value.profile(),
            value.factory(),
            value.placement(),
            value.scene(),
            value.master(),
            value.owner())
        && ratio(NativeSourceRawDamage.number(value.pet(), "hp"), value.profile().maxHp())
            == value.ratio();
  }

  /** {@code S_HPMeter}: opcode, object id (d), ratio 0..100 (c). */
  static byte[] packet(Value value) {
    return Protocol.hitRatio(value.id(), value.ratio(), 100);
  }

  static void send(Value value) {
    if (value == null || !current(value)) return;
    var previous = output.get();
    var oldContext = NativeAreaMaterialAction.current.get();
    NativeAreaMaterialAction.current.remove();
    output.set(value);
    try {
      NativePlayerCommit.completionFrame(
          null,
          () -> {
            try {
              synchronized (value.owner()) {
                if (current(value))
                  value
                      .owner()
                      .nativeSelfOriginalSend(packet(value), "S_HIT_RATIO Source companion recovery");
              }
            } catch (IOException | RuntimeException failure) {
              NativeSourcePhysicalNpc.failed(value.lifetime());
            }
          });
    } finally {
      if (previous == null) output.remove();
      else output.set(previous);
      if (oldContext == null) NativeAreaMaterialAction.current.remove();
      else NativeAreaMaterialAction.current.set(oldContext);
    }
  }

  /** Post-guard of the existing final-frame predicate; a no-op outside a recovery output. */
  static boolean allowFrame(boolean original, ClientSession pc, byte[] packet) {
    if (!original) return false;
    var value = output.get();
    return value == null
        || value.owner() == pc
            && current(value)
            && packet != null
            && packet.length == 6
            && (packet[0] & 255) == Protocol.S_HIT_RATIO
            && NativeSourcePhysicalNpc.object(packet) == value.id()
            && (packet[5] & 255) == value.ratio();
  }

  private NativeCloseoutCompanionRecovery() {}
}

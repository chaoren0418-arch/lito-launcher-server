package local.lito381.server;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Source monster recovery cadence on the already-owned Hub executor. No actor registry. */
final class NativeCloseoutNpcRecovery {
    static final String CLOCK = "nativeCloseoutNpcRecoveryClock";
    record Data(int hpInterval, int hpAmount, int mpInterval, int mpAmount) {}
    static Object capture(NativeDeNpcTemplate npc) {
        if (!"L1Monster".equals(npc.getImpl())) return null;
        return new Data(npc.get_hprinterval() > 0 ? npc.get_hprinterval() : 10,
                npc.get_hpr() > 0 ? npc.get_hpr() : 2,
                npc.get_mprinterval() > 0 ? npc.get_mprinterval() : 30,
                npc.get_mpr() > 0 ? npc.get_mpr() : 1);
    }
    static final class Clock implements Runnable {
        final GameHub hub;
        final NativePlayerLifetime lifetime;
        ScheduledFuture<?> future;
        int seconds;
        Clock(GameHub hub, NativePlayerLifetime lifetime) { this.hub = hub; this.lifetime = lifetime; }
        @Override public void run() {
            try {
                synchronized (lifetime.admission) {
                    if (!lifetime.admits()) return;
                    synchronized (hub) {
                        if (CombatAccess.field(hub, CLOCK) != this || (boolean) CombatAccess.field(hub, "nativeStaticMotionClosed")) return;
                        int tick = ++seconds;
                        for (var room : ((Map<?, ?>) CombatAccess.field(hub, "rooms")).values()) {
                            if (!(boolean) CombatAccess.field(room, "battleStarted") || (boolean) CombatAccess.field(room, "disconnectResolutionPending")) continue;
                            var match = (BattleRules.MatchState) CombatAccess.field(room, "match");
                            if (match.phase() != BattleRules.Phase.RUNNING) continue;
                            var mode = LiToGmNpcs.combatMode(match.rules());
                            if (mode == BattleRules.Mode.QUEST_BATTLE) encounter(CombatAccess.field(room, "questEncounter"), true, tick);
                            else if (mode == BattleRules.Mode.TIME_ATTACK) encounter(CombatAccess.field(room, "timeAttackEncounter"), false, tick);
                        }
                    }
                }
            } catch (RuntimeException failure) { NativeSourcePhysicalNpc.failed(lifetime); }
        }
    }
    static void init(GameHub hub) {
        if (!CombatRules.loaded()) return;
        var lifetime = NativePlayerLifetime.of(hub);
        synchronized (lifetime.admission) {
            synchronized (hub) {
                if (CombatAccess.field(hub, CLOCK) != null) throw new IllegalStateException("NPC recovery already installed");
                var clock = new Clock(hub, lifetime);
                CombatAccess.set(hub, CLOCK, clock);
                try {
                    clock.future = ((ScheduledExecutorService) CombatAccess.field(hub, "battleClock"))
                            .scheduleAtFixedRate(clock, 1, 1, TimeUnit.SECONDS);
                } catch (RuntimeException failure) { CombatAccess.set(hub, CLOCK, null); throw failure; }
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
    static void encounter(Object owner, boolean quest, int tick) {
        if (owner == null) return;
        synchronized (owner) {
            if (quest) {
                if (!(boolean) CombatAccess.field(owner, "active")) return;
                for (var lane : (Object[]) CombatAccess.field(owner, "lanes"))
                    for (var actor : (List<?>) CombatAccess.field(lane, "monsters")) recover(actor, true, tick);
            } else {
                var phase = CombatAccess.field(owner, "phase").toString();
                if (!phase.equals("ORDINARY_DOCUMENTED_FRAGMENT") && !phase.equals("FINAL_BOSSES")) return;
                for (var actor : (List<?>) CombatAccess.field(owner, "actors")) recover(actor, false, tick);
            }
        }
    }
    static void recover(Object actor, boolean quest, int tick) {
        int hp = NativeSourceRawDamage.number(actor, "hp");
        if (hp <= 0) return;
        var stamp = NativeSourceNpcFactoryType.stamp(actor);
        if (stamp == null || stamp.kind() != 1 || stamp.npc() != NativeSourceNpcFactoryMemory.npc(actor)
                || !(stamp.nativeCloseoutNpcRecovery instanceof Data data)) return;
        int maxHp = quest ? NativeSourceRawDamage.number(CombatAccess.field(actor, "template"), "maxHp") : NativeSourceRawDamage.number(actor, "maxHp");
        if (maxHp <= 0) return;
        if (hp < maxHp && tick % data.hpInterval() == 0)
            CombatAccess.set(actor, "hp", bounded(hp, data.hpAmount(), maxHp));
        int mp = NativeSourceRawDamage.number(actor, "mp"), maxMp = NativeSourceRawDamage.number(actor, "maxMp");
        if (maxMp > 0 && mp < maxMp && tick % data.mpInterval() == 0)
            CombatAccess.set(actor, "mp", bounded(mp, data.mpAmount(), maxMp));
    }
    static int bounded(int current, int amount, int maximum) { return (int) Math.max(0L, Math.min((long) maximum, (long) current + amount)); }
    private NativeCloseoutNpcRecovery() {}
}

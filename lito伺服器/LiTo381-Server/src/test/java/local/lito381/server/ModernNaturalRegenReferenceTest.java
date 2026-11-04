package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Independent retained-LiTo reference, deliberately NOT the current custom
 * 1-伺服器 PcRegenExecutor / fixed64 HP-and-MP scheduler. No source or device I/O.
 * Clock tests compare the closed-form modern implementation both with a literal
 * second-by-second model and with the older BattleNatural* implementation.
 */
final class ModernNaturalRegenReferenceTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
    }

    private static final long SECOND = 1_000_000_000L;

    private static ModernNaturalRegen.Values values(int type, long level, long con,
            long originalCon, long wis, long originalWis, long conMaximum, long hpr,
            long mpr, long maxHp, long maxMp) {
        return new ModernNaturalRegen.Values(level, type, con, originalCon, wis, originalWis,
                225, conMaximum, hpr, mpr, maxHp, maxMp);
    }

    @Test void smallIntegerAmountsMatchRetainedLiToWithoutImportingUnimplementedBuffs() {
        for (int type = 0; type <= 6; type++) {
            for (int con = 0; con <= 60; con++) {
                int maximum = BattleNaturalHpRegen.fallbackConHprMaximum(con);
                assertEquals(maximum, ModernNaturalRegen.fallbackConHprMaximum(con));
                assertEquals(BattleNaturalHpRegen.originalHpRegen(type, con),
                        ModernNaturalRegen.originalHpRegen(type, con));
                for (int equipment : new int[]{-30, -1, 0, 19}) {
                    var modern = values(type, 80, con, con, con, con, maximum, equipment, equipment, 1000, 1000);
                    var oldHp = new BattleNaturalHpRegen.Profile(80, type, con, con, 225, maximum, equipment);
                    var oldMp = new BattleNaturalMpRegen.Profile(type, con, con, equipment);
                    for (int roll = 1; roll <= maximum; roll++)
                        assertEquals(BattleNaturalHpRegen.amount(oldHp, false, roll),
                                ModernNaturalRegen.hpAmount(modern, roll));
                    assertEquals(BattleNaturalMpRegen.amount(oldMp), ModernNaturalRegen.mpAmount(modern));
                }
                assertEquals(BattleNaturalMpRegen.originalMpRegen(type, con),
                        ModernNaturalRegen.originalMpRegen(type, con));
            }
        }
    }

    @Test void independentClassAndLargeStatKnownAnswersPreserveExactBranches() {
        long[][] hp = {
                {0,0,0,0,0,1,1,2,2,3,4,0},
                {0,0,0,0,0,0,0,0,2,2,4,0},
                {0,0,0,0,0,0,1,1,2,3,3,3},
                {0,0,0,0,0,0,0,0,0,1,2,0},
                {0,0,0,1,1,2,2,2,2,2,2,2},
                {0,0,0,0,0,0,0,0,1,1,3,0},
                {0,0,0,0,0,0,1,1,2,2,2,2}
        };
        long[][] mp = {
                {0,0,0,0,0,1,1,2,2,2,2,2},
                {0,0,0,1,1,2,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,1,1,1,2,0},
                {0,0,0,0,0,0,1,1,2,2,3,0},
                {0,0,0,0,0,1,1,1,1,1,1,1},
                {0,0,0,0,0,0,0,1,1,2,2,2},
                {0,0,0,0,0,0,1,1,1,2,2,2}
        };
        int[] stats = {0, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19};
        long[] largeHp = {0,0,3,0,2,0,2}, largeMp = {2,0,0,0,1,2,2};
        for (int type = 0; type < 7; type++) {
            for (int i = 0; i < stats.length; i++) {
                assertEquals(hp[type][i], ModernNaturalRegen.originalHpRegen(type, stats[i]));
                assertEquals(mp[type][i], ModernNaturalRegen.originalMpRegen(type, stats[i]));
            }
            for (long value : new long[]{4_294_967_296L, Long.MAX_VALUE}) {
                assertEquals(largeHp[type], ModernNaturalRegen.originalHpRegen(type, value));
                assertEquals(largeMp[type], ModernNaturalRegen.originalMpRegen(type, value));
                assertEquals(19, ModernNaturalRegen.fallbackConHprMaximum(value));
                assertEquals(3, ModernNaturalRegen.wisdomBase(value));
            }
        }
    }

    @Test void thresholdsKeepRetainedKnightAndWizardRulesWithoutNarrowingLevel() {
        int[] thresholds = {120,100,80,64,56,48,44,40,36,12};
        for (int type = 0; type < 7; type++) {
            for (int level = 1; level <= 10; level++)
                assertEquals(thresholds[level - 1], ModernNaturalRegen.hpThreshold(level, type));
            assertEquals(12, ModernNaturalRegen.hpThreshold(29, type));
            for (long level : new long[]{30, 80, 4_294_967_296L, Long.MAX_VALUE})
                assertEquals(type == 1 ? 8 : 12, ModernNaturalRegen.hpThreshold(level, type));
            assertEquals(type == 3 ? 40 : 64, ModernNaturalRegen.mpThreshold(type));
        }
    }

    /** Literal oracle: no closed-form period arithmetic or modern helper calls. */
    private static final class Seconds {
        long next = SECOND;
        int hp, mp, hs = 4, ms = 4;
        void move() { hs = ms = 2; }
        void attack() { hs = ms = 1; }
        long[] poll(long now, boolean hpActive, boolean mpActive, int hpLimit, int mpLimit) {
            long h = 0, m = 0;
            while (next <= now) {
                next += SECOND;
                if (hpActive) { hp += hs; hs = 4; if (hp >= hpLimit) { hp = 0; h++; } }
                if (mpActive) { mp += ms; ms = 4; if (mp >= mpLimit) { mp = 0; m++; } }
            }
            return new long[]{h, m};
        }
    }
    private static int increment(ModernNaturalRegen.Activity activity) {
        return switch (activity) { case ATTACK -> 1; case MOVE -> 2; case NONE -> 4; };
    }

    @Test void randomizedBoundedClockMatchesBothLiteralSecondsAndRetainedTimers() {
        Random random = new Random(0x381_31L);
        for (int type = 0; type < 7; type++) for (int level : new int[]{1,2,9,10,29,30,80}) {
            var v = values(type, level, 18, 18, 18, 18, 3, 0, 0, 100, 100);
            var clock = ModernNaturalRegen.Clock.initial();
            Seconds oracle = new Seconds();
            BattleNaturalHpRegen hp = new BattleNaturalHpRegen(); hp.reset(0);
            BattleNaturalMpRegen mp = new BattleNaturalMpRegen(); mp.reset(0);
            long now = 0;
            for (int iteration = 0; iteration < 100; iteration++) {
                for (int action = random.nextInt(4); action > 0; action--) {
                    if (random.nextBoolean()) { clock = clock.noteMove(); oracle.move(); hp.noteMove(); mp.noteMove(); }
                    else { clock = clock.noteAttack(); oracle.attack(); hp.noteAttack(); mp.noteAttack(); }
                }
                now = Math.min(ModernNaturalRegen.MAX_ELAPSED_NANOS,
                        now + random.nextInt(20) * SECOND + random.nextInt(2));
                boolean ha = random.nextBoolean(), ma = random.nextBoolean();
                int hpLimit = type == 1 && level >= 30 ? 8
                        : new int[]{120,100,80,64,56,48,44,40,36,12}[Math.min(level,10)-1];
                long[] expected = oracle.poll(now, ha, ma, hpLimit, type == 3 ? 40 : 64);
                var due = ModernNaturalRegen.poll(v, clock, now, ha, ma);
                assertEquals(expected[0], due.hpEvents()); assertEquals(expected[1], due.mpEvents());
                assertEquals(hp.poll(now, ha, level, type), due.hpEvents());
                assertEquals(mp.poll(now, ma, type), due.mpEvents());
                clock = due.clock();
                assertEquals(oracle.next, clock.nextSecondElapsedNanos());
                assertEquals(oracle.hp, clock.hpCounter()); assertEquals(oracle.mp, clock.mpCounter());
                assertEquals(oracle.hs, increment(clock.hpActivity())); assertEquals(oracle.ms, increment(clock.mpActivity()));
                assertSame(clock, ModernNaturalRegen.poll(v, clock, now, ha, ma).clock());
            }
        }
    }

    @Test void inactiveBoundariesPreserveActivityAndLastActivityWinsRatherThanPriority() {
        var v = values(3, 80, 18, 18, 18, 18, 3, 0, 0, 100, 100);
        var c = ModernNaturalRegen.Clock.initial().noteAttack().noteMove();
        var paused = ModernNaturalRegen.poll(v, c, 20 * SECOND, false, false);
        assertEquals(0, paused.hpEvents()); assertEquals(0, paused.mpEvents());
        assertEquals(ModernNaturalRegen.Activity.MOVE, paused.clock().hpActivity());
        var resumed = ModernNaturalRegen.poll(v, paused.clock(), 21 * SECOND, true, false);
        assertEquals(2, resumed.clock().hpCounter());
        assertEquals(0, resumed.clock().mpCounter());
        assertEquals(ModernNaturalRegen.Activity.MOVE, resumed.clock().mpActivity());
        assertEquals(ModernNaturalRegen.Activity.NONE, resumed.clock().hpActivity());
        var due = ModernNaturalRegen.poll(v, resumed.clock(), 24 * SECOND, true, true);
        assertEquals(1, due.hpEvents()); assertEquals(0, due.clock().hpCounter(), "discard threshold overshoot");
        assertEquals(10, due.clock().mpCounter());
        var max = ModernNaturalRegen.poll(values(1,80,18,18,18,18,3,0,0,100,100),
                ModernNaturalRegen.Clock.initial(), 1800 * SECOND, true, true);
        assertEquals(900, max.hpEvents()); assertEquals(112, max.mpEvents());
        assertEquals(1801 * SECOND, max.clock().nextSecondElapsedNanos());
        assertEquals(32, max.clock().mpCounter());
    }

    @Test void exactRollBoundAndLastNonzeroHpApplicationSurviveGainThenLoss() {
        var v = values(1, 80, 18, 0, 14, 0, 3, -2, 0, 10, 10);
        long[] draws = {2,0,1}; AtomicInteger index = new AtomicInteger();
        var due = new ModernNaturalRegen.Due(ModernNaturalRegen.Clock.initial(),3,0);
        var result = ModernNaturalRegen.resolve(v,due,5,5,bound -> {
            assertEquals(3,bound); return draws[index.getAndIncrement()];
        });
        assertEquals(3,index.get()); assertEquals(5,result.hp());
        assertEquals(1,result.hpGain()); assertEquals(1,result.hpLoss());
        assertEquals(6,result.hpBefore()); assertEquals(5,result.hpAfter());
        assertTrue(result.changed(), "zero net is still two real applications");
        assertEquals(0,result.mpGain()); assertEquals(0,result.mpLoss());
    }

    @Test void signedAmountsClipWithoutI64OverflowAndNeverKillThroughHpr() {
        var positive = values(1,80,18,0,14,0,1,Long.MAX_VALUE-1,Long.MAX_VALUE-1,Long.MAX_VALUE,Long.MAX_VALUE);
        var due = new ModernNaturalRegen.Due(ModernNaturalRegen.Clock.initial(),2,180);
        AtomicInteger calls = new AtomicInteger();
        var gain = ModernNaturalRegen.resolve(positive,due,Long.MAX_VALUE-2,Long.MAX_VALUE-3,b -> { calls.incrementAndGet(); return 0; });
        assertEquals(Long.MAX_VALUE,gain.hp()); assertEquals(Long.MAX_VALUE,gain.mp());
        assertEquals(2,gain.hpGain()); assertEquals(3,gain.mpGain());
        assertEquals(1,calls.get(), "stop HP RNG when first recovery fills HP");
        var negative = values(1,80,18,0,14,0,1,Long.MIN_VALUE,Long.MIN_VALUE,Long.MAX_VALUE,Long.MAX_VALUE);
        var loss = ModernNaturalRegen.resolve(negative,due,Long.MAX_VALUE,Long.MAX_VALUE,b -> 0);
        assertEquals(Long.MAX_VALUE,loss.hp(), "full HP is ineligible even with negative HPR");
        assertEquals(0,loss.mp()); assertEquals(Long.MAX_VALUE,loss.mpLoss());
        var damaged = ModernNaturalRegen.resolve(negative,due,Long.MAX_VALUE-1,Long.MAX_VALUE-1,b -> 0);
        assertEquals(1,damaged.hp()); assertEquals(Long.MAX_VALUE-2,damaged.hpLoss());
        assertEquals(0,damaged.mp()); assertEquals(Long.MAX_VALUE-1,damaged.mpLoss());
        // The cumulative ledger counts all applications, but the latest
        // before/after pair describes the last nonzero individual MP tick.
        var threeMpTicks = new ModernNaturalRegen.Due(ModernNaturalRegen.Clock.initial(),0,3);
        var plusThree = values(1,80,18,0,14,0,1,0,2,8,8);
        var up = ModernNaturalRegen.resolve(plusThree,threeMpTicks,8,1,b -> { throw new AssertionError("MP needs no RNG"); });
        assertEquals(8,up.mp()); assertEquals(7,up.mpGain()); assertEquals(0,up.mpLoss());
        assertEquals(7,up.mpBefore()); assertEquals(8,up.mpAfter());
        var minusThree = values(1,80,18,0,14,0,1,0,-4,8,8);
        var down = ModernNaturalRegen.resolve(minusThree,threeMpTicks,8,8,b -> { throw new AssertionError("MP needs no RNG"); });
        assertEquals(0,down.mp()); assertEquals(0,down.mpGain()); assertEquals(8,down.mpLoss());
        assertEquals(2,down.mpBefore()); assertEquals(0,down.mpAfter());
    }

    @Test void invalidSourceAmountsAndHostileRandomFailWithoutMutatingImmutablePlan() {
        assertThrows(ArithmeticException.class, () -> values(1,80,18,0,14,0,1,Long.MAX_VALUE,0,100,100));
        assertThrows(ArithmeticException.class, () -> values(1,80,18,0,14,0,1,0,Long.MAX_VALUE,100,100));
        var v = values(1,80,18,0,14,0,Long.MAX_VALUE,0,0,Long.MAX_VALUE,100);
        var due = new ModernNaturalRegen.Due(ModernNaturalRegen.Clock.initial(),1,0);
        for (long bad : new long[]{-1,Long.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> ModernNaturalRegen.resolve(v,due,2,1,bound -> bad));
            assertEquals(ModernNaturalRegen.Clock.initial(),due.clock());
        }
        var exact = ModernNaturalRegen.resolve(v,due,2,1,bound -> { assertEquals(Long.MAX_VALUE,bound); return bound-1; });
        assertEquals(Long.MAX_VALUE,exact.hp());
        assertEquals(Long.MAX_VALUE-2,exact.hpGain());
        assertThrows(IllegalArgumentException.class, () -> ModernNaturalRegen.hpAmount(v,0));
        assertThrows(IllegalArgumentException.class, () -> ModernNaturalRegen.poll(v,due.clock(),-1,true,true));
        assertThrows(IllegalArgumentException.class, () -> ModernNaturalRegen.poll(v,due.clock(),1800*SECOND+1,true,true));
    }

    @Test void cumulativeGainOverflowRejectsWholePlanEvenWhenEachVitalRemainsInRange() {
        var v = values(1,80,18,0,14,0,Long.MAX_VALUE,-(Long.MAX_VALUE/2),0,Long.MAX_VALUE,100);
        var due = new ModernNaturalRegen.Due(ModernNaturalRegen.Clock.initial(),3,0);
        AtomicInteger calls = new AtomicInteger();
        assertThrows(ArithmeticException.class, () -> ModernNaturalRegen.resolve(v,due,1,1,
                bound -> calls.getAndIncrement()==1 ? 0 : bound-1));
        assertEquals(3,calls.get());
        assertEquals(ModernNaturalRegen.Clock.initial(),due.clock());
    }

    @Test void foodIsRaw225AndZeroThroughTwoAreIneligibleWithoutInventedConversion() {
        for (long food : new long[]{0,1,2,3,224,225}) {
            var v = new ModernNaturalRegen.Values(80,1,18,18,18,18,food,3,0,0,100,100);
            assertEquals(food>=3,v.fed());
        }
        for (long food : new long[]{-1,226,Long.MAX_VALUE})
            assertThrows(IllegalArgumentException.class, () -> new ModernNaturalRegen.Values(80,1,18,18,18,18,food,3,0,0,100,100));
    }
}

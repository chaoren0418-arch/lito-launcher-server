package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleDragonEyeStatusTest {
    private static final long START = TimeUnit.SECONDS.toNanos(5);

    @Test void delegatesEveryLifeEyeAbilityForTheExactSixHundredSecondLease() {
        BattleDragonEyeStatus status = new BattleDragonEyeStatus();
        BattleDragonEyeCatalog.Eye eye = BattleDragonEyeCatalog.require(42_524L);

        BattleDragonEyeStatus.Activation applied = status.activate(0x7000_0687L, eye, START);
        assertTrue(applied.applied());
        assertEquals(600, applied.remainingSeconds());
        BattleDragonEyeStatus.Snapshot snapshot = status.snapshot(START);
        assertEquals(2, snapshot.meleeDamage());
        assertEquals(1, snapshot.magicHit());
        assertEquals(50, snapshot.damageReduction());
        assertEquals(10, snapshot.evasionRate());
        assertEquals(0, snapshot.stoneResistance());
        assertEquals(0, snapshot.sleepResistance());
        assertEquals(0, snapshot.freezeResistance());
        assertEquals(1, snapshot.poisonResistance());
        assertEquals(1, status.snapshot(
                START + TimeUnit.SECONDS.toNanos(599)).remainingSeconds());
        assertFalse(status.snapshot(START + TimeUnit.SECONDS.toNanos(600)).active());
        assertSame(eye, status.expire(START + TimeUnit.SECONDS.toNanos(600)));
        assertNull(status.expire(START + TimeUnit.SECONDS.toNanos(601)));
    }

    @Test void checksSharedDelayThenGroupWithoutCommittingBlockedItemCooldown() {
        BattleDragonEyeStatus status = new BattleDragonEyeStatus();
        BattleDragonEyeCatalog.Eye wind = BattleDragonEyeCatalog.require(42_520L);
        BattleDragonEyeCatalog.Eye earth = BattleDragonEyeCatalog.require(42_518L);
        status.activate(0x7000_0686L, wind, START);

        BattleDragonEyeStatus.Activation sharedDelay = status.activate(
                0x7000_0685L, earth,
                START + TimeUnit.MILLISECONDS.toNanos(500));
        assertEquals(BattleDragonEyeStatus.Outcome.ITEM_COOLDOWN, sharedDelay.outcome());
        assertEquals(100, sharedDelay.remainingCooldownMillis());

        BattleDragonEyeStatus.Activation conflict = status.activate(
                0x7000_0685L, earth,
                START + TimeUnit.MILLISECONDS.toNanos(600));
        assertEquals(BattleDragonEyeStatus.Outcome.GROUP_CONFLICT, conflict.outcome());
        assertEquals(600, conflict.remainingSeconds());

        BattleDragonEyeStatus.Activation ownDelay = status.activate(
                0x7000_0686L, wind, START + TimeUnit.SECONDS.toNanos(1));
        assertEquals(BattleDragonEyeStatus.Outcome.ITEM_COOLDOWN, ownDelay.outcome());
        assertEquals(59_000, ownDelay.remainingCooldownMillis());

        // GenericBuffItem returned before markDelayEffectUsed; the rejected
        // Earth Eye is immediately usable when the active group lease expires.
        BattleDragonEyeStatus.Activation earthAfterExpiry = status.activate(
                0x7000_0685L, earth, START + TimeUnit.SECONDS.toNanos(600));
        assertTrue(earthAfterExpiry.applied());
        assertEquals(10, status.snapshot(
                START + TimeUnit.SECONDS.toNanos(600)).evasionRate());

        status.clear();
        assertFalse(status.snapshot(START + TimeUnit.SECONDS.toNanos(601)).active());
    }
}

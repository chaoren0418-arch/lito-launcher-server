package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleFreezeEffectsTest {
    @Test void placementRefreshRemovalAndExpiryFollowFreezeLease() {
        BattleFreezeEffects effects = new BattleFreezeEffects();
        long now = 10_000L;
        BattleFreezeEffects.Placement first =
                effects.place(100, 32_700, 32_800, now, 3);
        assertNull(first.previous());
        assertEquals(100, first.placed().targetObjectId());
        assertEquals(now + TimeUnit.SECONDS.toNanos(3),
                first.placed().expiresAtNanos());
        assertEquals(1, effects.active(now).size());

        BattleFreezeEffects.Placement refreshed =
                effects.place(100, 32_701, 32_801, now + 1, 16);
        assertSame(first.placed(), refreshed.previous());
        assertNotEquals(first.placed().objectId(), refreshed.placed().objectId());
        assertEquals(32_701, refreshed.placed().x());
        assertSame(refreshed.placed(), effects.remove(100));
        assertEquals(0, effects.active(now + 2).size());

        BattleFreezeEffects.Placement expiring =
                effects.place(101, 32_702, 32_802, now, 3);
        var expired = effects.expire(now + TimeUnit.SECONDS.toNanos(3));
        assertEquals(1, expired.size());
        assertSame(expiring.placed(), expired.get(0));
    }

    @Test void protocolUsesNpcShapedPutObjectWithSelectedGfx176() {
        BattleFreezeEffects effects = new BattleFreezeEffects();
        BattleFreezeEffects.Effect effect =
                effects.place(200, 32_710, 32_810, 1, 3).placed();
        byte[] expected = new Protocol.Writer().c(Protocol.S_PUT_OBJECT)
                .h(32_710).h(32_810).d(effect.objectId())
                .h(BattleFreezeEffects.GFX)
                .c(0).c(0).c(0).c(BattleTimedStatus.NORMAL)
                .d(0).h(0).s("").s("")
                .c(0).d(0).s("").s("")
                .c(0).c(0xFF).c(0).c(0)
                .s("").h(0).c(0).bytes();
        assertArrayEquals(expected, Protocol.freezeEffect(effect));
        assertEquals(81168, BattleFreezeEffects.NPC_ID);
        assertEquals(176, BattleFreezeEffects.GFX);
    }
}

package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleEraseMagicStatusTest {
    @Test
    void quartersMrExpiresAndExactLeaseConsumptionCannotDeleteRefresh() {
        BattleEraseMagicStatus status = new BattleEraseMagicStatus();
        long now = 10_000_000_000L;
        BattleEraseMagicStatus.Lease first = status.activate(now, 32);
        assertEquals(25, status.effectiveMagicResistance(103, now));
        assertTrue(status.active(now + TimeUnit.SECONDS.toNanos(31)));

        BattleEraseMagicStatus.Lease refresh = status.activate(now + 1, 32);
        assertFalse(status.consume(first));
        assertTrue(status.consume(refresh));
        assertEquals(103, status.effectiveMagicResistance(103, now + 2));

        status.activate(now + 3, 32);
        long expiry = now + 3 + TimeUnit.SECONDS.toNanos(32);
        assertFalse(status.expire(expiry - 1));
        assertTrue(status.expire(expiry));
        assertFalse(status.active(expiry));
    }
}

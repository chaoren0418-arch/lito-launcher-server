package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BattleCookingFiresTest {
    @Test void placesDetectsAndExpiresTheExactTenMinuteCookingFire() {
        BattleCookingFires fires = new BattleCookingFires();
        long start = 1_000_000_000L;
        BattleCookingFires.Fire fire = fires.place(100, 200, start);

        assertEquals(81170, BattleCookingFires.NPC_ID);
        assertEquals(5943, BattleCookingFires.GFX);
        assertEquals(600, BattleCookingFires.DURATION_SECONDS);
        assertEquals(start + 600_000_000_000L, fire.expiresAtNanos());
        assertTrue(fires.near(103, 203, start));
        assertFalse(fires.near(104, 203, start));
        assertEquals(1, fires.active(fire.expiresAtNanos() - 1).size());
        assertEquals(1, fires.expire(fire.expiresAtNanos()).size());
        assertFalse(fires.near(100, 200, fire.expiresAtNanos()));
        assertTrue(fires.active(fire.expiresAtNanos()).isEmpty());
    }
}

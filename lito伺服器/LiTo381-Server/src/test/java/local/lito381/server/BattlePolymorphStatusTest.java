package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattlePolymorphStatusTest {
    @Test void deathKnightResourceUsesVerifiedLevelSpriteAndActionFamilies() {
        BattlePolymorphCatalog.Form form = BattlePolymorphCatalog.find("DEATH KNIGHT");
        assertEquals(BattlePolymorphCatalog.DEATH_KNIGHT, form);
        assertEquals(3784, form.gfx());
        assertEquals(52, form.minimumLevel());
        assertEquals(1_800, BattlePolymorphCatalog.scrollDurationSeconds(0));
        assertEquals(2_100, BattlePolymorphCatalog.scrollDurationSeconds(1));
        assertEquals(true, form.supportsWalkAction(BattleActionCodes.WALK));
        assertEquals(true, form.supportsWalkAction(BattleActionCodes.SWORD_WALK));
    }

    @Test void activationRefreshesAndExpiryReturnsTheAppearanceExactlyOnce() {
        BattlePolymorphStatus status = new BattlePolymorphStatus();
        long start = TimeUnit.SECONDS.toNanos(10);
        status.activate(start, BattlePolymorphCatalog.DEATH_KNIGHT, 1_800);

        assertEquals(BattlePolymorphCatalog.DEATH_KNIGHT, status.active(start));
        assertEquals(BattlePolymorphCatalog.DEATH_KNIGHT,
                status.active(start + TimeUnit.SECONDS.toNanos(1_800) - 1));
        assertNull(status.expire(start + TimeUnit.SECONDS.toNanos(1_800) - 1));
        assertNull(status.active(start + TimeUnit.SECONDS.toNanos(1_800)));
        assertEquals(BattlePolymorphCatalog.DEATH_KNIGHT,
                status.expire(start + TimeUnit.SECONDS.toNanos(1_800)));
        assertNull(status.expire(start + TimeUnit.SECONDS.toNanos(1_801)));

        status.activate(start, BattlePolymorphCatalog.DEATH_KNIGHT, 1_800);
        status.activate(start + TimeUnit.SECONDS.toNanos(100),
                BattlePolymorphCatalog.DEATH_KNIGHT, 1_800);
        assertEquals(BattlePolymorphCatalog.DEATH_KNIGHT,
                status.active(start + TimeUnit.SECONDS.toNanos(1_899)));
        assertEquals(BattlePolymorphCatalog.DEATH_KNIGHT, status.clear());
        assertNull(status.active(start));
    }
}

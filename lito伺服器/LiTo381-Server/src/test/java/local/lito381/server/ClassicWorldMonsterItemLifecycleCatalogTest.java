package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class ClassicWorldMonsterItemLifecycleCatalogTest {
    @Test
    void selectedProjectionPinsAllDigestAndGiveRows() {
        assertEquals(430, ClassicWorldMonsterItemLifecycleCatalog.digestSize());
        assertEquals(3, ClassicWorldMonsterItemLifecycleCatalog.digestEnabledCount());
        assertEquals(31, ClassicWorldMonsterItemLifecycleCatalog.giveSize());
        assertEquals(30, ClassicWorldMonsterItemLifecycleCatalog.digestSecondsForNpc(45032));
        assertEquals(30, ClassicWorldMonsterItemLifecycleCatalog.digestSecondsForNpc(45060));
        assertEquals(15, ClassicWorldMonsterItemLifecycleCatalog.digestSecondsForNpc(70984));
        assertEquals(999, ClassicWorldMonsterItemLifecycleCatalog.giveMaximum(40508));
        assertEquals(1, ClassicWorldMonsterItemLifecycleCatalog.giveMaximum(41310));
        assertEquals(-1, ClassicWorldMonsterItemLifecycleCatalog.giveMaximum(40308));
    }
}

package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class BattleDoorTest {
    @Test void resetsClosedAtResourceSafeFeatureAnchorAndTogglesAuthoritatively() {
        BattleDoor door = new BattleDoor();
        assertNull(door.snapshot());

        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(509);
        door.reset(arena);
        BattleDoor.Snapshot closed = door.snapshot();
        assertEquals(arena.featurePoint().x(), closed.x());
        assertEquals(arena.featurePoint().y(), closed.y());
        assertTrue(arena.contains(closed.x(), closed.y()));
        assertTrue(arena.nativeStandingFloor(closed.x(), closed.y()));
        assertFalse(arena.spawns().contains(new ArenaMapCatalog.Point(
                closed.x(), closed.y())));
        assertEquals(BattleDoor.ACTION_CLOSED, closed.action());
        assertEquals(BattleDoor.DIRECTION_DIAGONAL, closed.direction());
        assertEquals(BattleDoor.NOT_PASSABLE, closed.passable());
        assertFalse(closed.open());
        assertTrue(door.blocks(closed.x(), closed.y()));
        assertFalse(door.blocks(closed.x() + 1, closed.y()));

        BattleDoor.Snapshot open = door.toggle();
        assertTrue(open.open());
        assertEquals(BattleDoor.ACTION_OPEN, open.action());
        assertEquals(BattleDoor.PASSABLE, open.passable());
        assertFalse(door.blocks(open.x(), open.y()));

        assertFalse(door.toggle().open());
        assertTrue(door.open().open());
        assertTrue(door.open().open());
        door.deactivate();
        assertNull(door.snapshot());
    }
}

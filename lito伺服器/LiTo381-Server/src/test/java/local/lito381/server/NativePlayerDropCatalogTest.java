package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class NativePlayerDropCatalogTest {
    @Test
    void selectedGlobalGateOnlyExemptsMeatForOrdinaryPlayers() {
        assertFalse(NativePlayerDropCatalog.RETAINED_GLOBAL_DROP_ENABLED);
        assertTrue(NativePlayerDropCatalog.RETAINED_FORCE_DROPPABLE
                .contains(40056L));
        assertFalse(NativePlayerDropCatalog.permits(false, false,
                InventoryCatalog.MAGIC_LANTERN));
        assertTrue(NativePlayerDropCatalog.permits(false, false, 40056L));
        assertTrue(NativePlayerDropCatalog.permits(false, true,
                InventoryCatalog.MAGIC_LANTERN));
        assertTrue(NativePlayerDropCatalog.permits(true, false,
                InventoryCatalog.MAGIC_LANTERN));
    }
}

package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ModernEquipmentProcReadProbeTest {
    @Test void extraCatalogReadRequiresAnExplicitExactSwitch() {
        assertFalse(ModernEquipmentProcReadProbe.protectionCatalog(new String[]{"configuration"}));
        assertTrue(ModernEquipmentProcReadProbe.protectionCatalog(new String[]{"configuration","--protection-catalog"}));
        for(var args:new String[][]{ {}, {"configuration","--unknown"},
                {"configuration","--Protection-Catalog"}, {"configuration","--protection-catalog","extra"} }) {
            assertThrows(IllegalArgumentException.class,()->ModernEquipmentProcReadProbe.protectionCatalog(args));
        }
    }
}

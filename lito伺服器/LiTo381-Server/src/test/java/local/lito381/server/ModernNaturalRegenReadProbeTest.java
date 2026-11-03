package local.lito381.server;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class ModernNaturalRegenReadProbeTest {
    @Test void publicSummaryIsExplicitSourceOnlyAndDoesNotExposeCatalogOrProfile() {
        var row=summary(true);String text=ModernNaturalRegenReadProbe.render(row);
        assertTrue(text.contains("\"policy\":\"NEW_MATCH_NATURAL_REGEN_V1\""));
        assertTrue(text.contains("\"override_mode\":\"SOURCE_ONLY_ABSENT\""));
        assertTrue(text.contains("\"observation\":\"readonly_not_admission\""));
        assertTrue(text.contains("multi_query_not_atomic_generation"));
        assertTrue(text.contains("\"constitution_rows\":50"));
        assertTrue(text.contains("\"hp_max\":4"),text);assertTrue(text.contains("\"mp_amount\":6"),text);
        assertTrue(text.contains("\"mp_threshold\":64"));assertTrue(text.contains("\"hp_threshold\":8"));
        assertFalse(text.contains("catalog"));assertFalse(text.contains("account"));assertFalse(text.contains("password"));
        assertEquals("NaturalRegenProbeSummary[redacted]",row.toString());
    }
    @Test void signedEquipmentRegenerationIsReportedWithoutZeroClamp() {
        String text=ModernNaturalRegenReadProbe.render(summary(false));
        assertTrue(text.contains("\"equipment_hpr\":-4"),text);assertTrue(text.contains("\"equipment_mpr\":-2"),text);
        assertTrue(text.contains("\"hp_min\":-3"));assertTrue(text.contains("\"hp_max\":0"));
        assertTrue(text.contains("\"mp_amount\":-1"));assertTrue(text.contains("\"mp_threshold\":40"));
    }
    private static ModernNaturalRegenReadProbe.Summary summary(boolean sun) {
        int sourceId=sun?7:42;
        var source=ModernCombatTestPeer.regenSource(ModernCombatTestPeer.weightSource(ModernMagicCombatTest.stats(
                ModernPhysicalCombatTest.source(sourceId,sun?1:3,80,List.of()),12,0,0,0,6)),sun);
        var fighter=ModernPhysicalCombatTest.fighter(source);
        return new ModernNaturalRegenReadProbe.Summary(sourceId,fighter.key().objectId(),1,0,
                ModernInventoryWeightReadProbe.OverrideMode.SOURCE_ONLY_ABSENT,fighter.regen());
    }
}

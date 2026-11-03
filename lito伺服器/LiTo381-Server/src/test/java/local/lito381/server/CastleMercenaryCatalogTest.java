package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

final class CastleMercenaryCatalogTest {
    @Test
    void exposesOnlyThePreservedCastleFourCatalog() {
        List<CastleMercenaryCatalog.Entry> rows =
                CastleMercenaryCatalog.forCastle(4);
        assertEquals(List.of(0, 1, 2, 3),
                rows.stream().map(CastleMercenaryCatalog.Entry::id).toList());
        assertEquals(List.of("$4223", "$4222", "$4224", "$4225"),
                rows.stream().map(CastleMercenaryCatalog.Entry::nativeName).toList());
        assertEquals(List.of(10_000, 10_000, 10_000, 15_000),
                rows.stream().map(CastleMercenaryCatalog.Entry::unitPrice).toList());
        assertEquals(List.of(0, 0, 0, 0),
                rows.stream().map(CastleMercenaryCatalog.Entry::npcTemplateId).toList());
        assertEquals(70633, CastleMercenaryCatalog.COMMANDER_NPC_TEMPLATE_ID);
        assertEquals(148, CastleMercenaryCatalog.COMMANDER_GFX);
        assertEquals("$850", CastleMercenaryCatalog.COMMANDER_NAME);
        assertEquals("colbert1", CastleMercenaryCatalog.COMMANDER_HTML);
        assertEquals("colbert2", CastleMercenaryCatalog.NOT_READY_HTML);
        assertEquals("colbert3", CastleMercenaryCatalog.EMPTY_HTML);
        assertEquals(false, CastleMercenaryCatalog.hasSummonTemplates(4,
                new GameStateStore.MercenaryRoster(0, 0, 0, 1)));
        assertEquals(List.of(), CastleMercenaryCatalog.forCastle(1));
        assertEquals(List.of(), CastleMercenaryCatalog.forCastle(5));
        assertNull(CastleMercenaryCatalog.entry(5, 0));
        assertNull(CastleMercenaryCatalog.entry(4, 4));
    }
}

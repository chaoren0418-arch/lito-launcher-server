package local.lito381.server;

import java.util.List;

/**
 * Native castle-mercenary employment rows recovered from the 3.80 lineage.
 *
 * <p>The preserved {@code S_SoldierBuyList} writes four entries with ids
 * 0..3 and prices 10,000/10,000/10,000/15,000.  Its companion SQL dump has
 * exactly one {@code castle_soldier} row, for castle 4, whose native string
 * tokens are $4223/$4222/$4224/$4225.  Do not project that single proven
 * catalog onto other castles.</p>
 */
final class CastleMercenaryCatalog {
    static final int SUPPORTED_CASTLE_ID = 4;
    static final int COMMANDER_NPC_TEMPLATE_ID = 70633;
    static final int COMMANDER_GFX = 148;
    static final String COMMANDER_NAME = "$850";
    static final String COMMANDER_HTML = "colbert1";
    static final String NOT_READY_HTML = "colbert2";
    static final String EMPTY_HTML = "colbert3";

    record Entry(int id, String nativeName, int unitPrice, int npcTemplateId) {
        Entry {
            if (id < 0 || id > 3 || nativeName == null || nativeName.isBlank()
                    || unitPrice <= 0 || unitPrice > 0xFFFF || npcTemplateId < 0) {
                throw new IllegalArgumentException("invalid native castle mercenary entry");
            }
        }

        Protocol.MercenaryEmployRow wireRow() {
            return new Protocol.MercenaryEmployRow(id, nativeName, unitPrice);
        }
    }

    private static final List<Entry> CASTLE_FOUR = List.of(
            new Entry(0, "$4223", 10_000, 0),
            new Entry(1, "$4222", 10_000, 0),
            new Entry(2, "$4224", 10_000, 0),
            new Entry(3, "$4225", 15_000, 0));

    private CastleMercenaryCatalog() {}

    static List<Entry> forCastle(int castleId) {
        return castleId == SUPPORTED_CASTLE_ID ? CASTLE_FOUR : List.of();
    }

    static Entry entry(int castleId, int entryId) {
        if (castleId != SUPPORTED_CASTLE_ID || entryId < 0 || entryId >= CASTLE_FOUR.size()) {
            return null;
        }
        Entry entry = CASTLE_FOUR.get(entryId);
        return entry.id() == entryId ? entry : null;
    }

    static boolean hasSummonTemplates(int castleId, GameStateStore.MercenaryRoster roster) {
        if (roster == null || roster.total() <= 0) return false;
        for (int entryId = 0; entryId < CASTLE_FOUR.size(); entryId++) {
            if (roster.count(entryId) <= 0) continue;
            Entry entry = entry(castleId, entryId);
            if (entry == null || entry.npcTemplateId() == 0) return false;
        }
        return true;
    }
}

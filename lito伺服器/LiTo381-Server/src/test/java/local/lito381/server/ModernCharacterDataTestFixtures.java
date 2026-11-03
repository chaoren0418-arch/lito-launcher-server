package local.lito381.server;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/** Synthetic, deterministic persisted values for verified-TLS interop, never a live/effective stat calculator. */
final class ModernCharacterDataTestFixtures implements ModernCharacterDataRepository {
    private final boolean large;
    final AtomicInteger completedReads = new AtomicInteger();

    ModernCharacterDataTestFixtures(boolean large) { this.large = large; }

    @Override public Optional<ModernCharacterData.Snapshot> character(
            ModernAccountAuthentication.Principal principal, int sourceId, long objectId) throws Exception {
        ModernBackendScope.current().checkActive();
        if (principal == null || !principal.authorizesSource(sourceId)) throw new IllegalArgumentException("fixture source denied");
        String expected = switch (sourceId) {
            case 7 -> ModernApplicationTestBackend.SUN_ACCOUNT;
            case 42 -> ModernApplicationTestBackend.MOON_ACCOUNT;
            default -> throw new IllegalArgumentException("fixture source denied");
        };
        if (!expected.equals(principal.accountForSource(sourceId))) {
            throw new IllegalArgumentException("fixture source owner mismatch");
        }
        if (objectId != 3333) return Optional.empty();
        var snapshot = snapshot(sourceId, large);
        ModernBackendScope.current().checkActive();
        completedReads.incrementAndGet();
        return Optional.of(snapshot);
    }

    static ModernCharacterData.Snapshot snapshot(int sourceId, boolean large) {
        if (sourceId != 7 && sourceId != 42) throw new IllegalArgumentException("fixture source denied");
        // The data read is intentionally fresher than the earlier navigation
        // roster (80/120000). Identity/class/sex/name remain the same.
        var character = new ModernApplicationProtocol.CharacterRow(3333,
                sourceId == 7 ? "Sun Knight" : "Moon Mage", sourceId == 7 ? 1 : 3,
                sourceId == 7 ? 0 : 1, sourceId == 7 ? 99 : 120001);
        long base = 4_000_000_000L + sourceId * 100L;
        var values = new ModernCharacterData.PersistedValues(
                base + 1, base + 2, base + 101, base + 102, -(base + 3),
                base + 4, base + 5, base + 6, base + 7, base + 8, base + 9,
                Long.MAX_VALUE - sourceId, -(base + 10), base + 11, base + 12,
                base + 13, base + 14, base + 15, 225);
        int count = large ? 512 : 3;
        var items = new ArrayList<ModernCharacterData.InventoryItem>(count);
        for (int index = 0; index < count; index++) {
            String token = "$" + sourceId + "-" + index;
            if (large) {
                String prefix = "S" + sourceId + ":" + index + ":";
                token = prefix + "x".repeat(256 - prefix.length());
            }
            var template = new ModernCharacterData.ItemTemplate(6_000_000_001L + index,
                    ModernCharacterData.ItemKind.values()[index % 3], token,
                    11_000_000_000L + sourceId + index, 12_000_000_000L + sourceId + index,
                    13_000_000_000L + sourceId + index);
            items.add(new ModernCharacterData.InventoryItem(5_000_000_001L + index, template,
                    7_000_000_000L + sourceId + index, index < 2,
                    -(8_000_000_000L + sourceId + index), 9_000_000_000L + sourceId + index,
                    index % 2 == 0, index % 256, index % 3 == 0, 10_000_000_000L + sourceId + index));
        }
        return new ModernCharacterData.Snapshot(sourceId, character, values, items);
    }
}

package local.lito381.server;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * Lossless, source-owned persisted character input to the modern game domain.
 * This is NOT an effective/live combat status packet. In the traced source,
 * persisted Ac includes selection-time equipment, while MaxHp/MaxMp are used as
 * base maxima. CurHp/CurMp are saved live values and may exceed those base maxima.
 * The game-domain evaluator must handle these fields individually, not re-add
 * every equipment bonus to a bag ambiguously labelled "base stats".
 */
final class ModernCharacterData {
    static final int MAX_ITEMS = 512;
    static final int MAX_TEXT_BYTES = 256;
    static final long FOOD_FULL = 225;
    private ModernCharacterData() {}

    record PersistedValues(long maxHp, long maxMp, long savedHp, long savedMp,
            long armorClass, long strength, long dexterity, long constitution,
            long intelligence, long wisdom, long charisma, long experience, long lawful,
            long originalStrength, long originalDexterity, long originalConstitution,
            long originalIntelligence, long originalWisdom, long food225) {
        PersistedValues {
            if (maxHp < 1 || maxMp < 0 || savedHp < 0 || savedMp < 0 || strength < 0
                    || dexterity < 0 || constitution < 0 || intelligence < 0 || wisdom < 0
                    || charisma < 0 || experience < 0 || originalStrength < 0
                    || originalDexterity < 0 || originalConstitution < 0
                    || originalIntelligence < 0 || originalWisdom < 0
                    || food225 < 0 || food225 > FOOD_FULL) throw invalid();
            // Signed AC/lawful and the full positive i64 ability/vital/experience
            // range are retained. No byte/short/native gauge saturation occurs.
        }
        int foodPercent() { return (int) (food225 * 100 / FOOD_FULL); }
    }

    enum ItemKind { WEAPON, ARMOR, ETC }
    /** Native resource identifiers are only metadata, never permission to mount LT packs. */
    record ItemTemplate(long catalogId, ItemKind kind, String nameToken,
            long inventoryGraphic, long groundGraphic, long unitWeight) {
        ItemTemplate {
            if (catalogId <= 0 || kind == null || !validText(nameToken)
                    || inventoryGraphic < 0 || groundGraphic < 0 || unitWeight < 0) throw invalid();
        }
        @Override public String toString() { return "ModernItemTemplate[redacted]"; }
    }
    record InventoryItem(long objectId, ItemTemplate template, long quantity,
            boolean equipped, long enchantment, long durability, boolean identified,
            long blessing, boolean sealed, long chargeCount) {
        InventoryItem {
            Objects.requireNonNull(template);
            if (objectId <= 0 || quantity <= 0 || durability < 0 || blessing < 0
                    || blessing > 255 || chargeCount < 0) throw invalid();
            // Preserve signed enchantment; an import does not authorize equip/use.
        }
        @Override public String toString() { return "ModernInventoryItem[redacted]"; }
    }
    record Snapshot(int sourceId, ModernApplicationProtocol.CharacterRow character,
            PersistedValues persisted, List<InventoryItem> inventory) {
        Snapshot {
            Objects.requireNonNull(character); Objects.requireNonNull(persisted);
            if (sourceId <= 0 || character.objectId() <= 0 || !validText(character.name())
                    || character.characterClass() < 0 || character.characterClass() > 65535
                    || character.sex() < 0 || character.sex() > 1 || character.level() < 1
                    || inventory == null || inventory.size() > MAX_ITEMS) throw invalid();
            inventory = List.copyOf(inventory);
            long previous = 0;
            for (var item : inventory) {
                if (item.objectId() <= previous) throw invalid();
                previous = item.objectId();
            }
        }
        @Override public String toString() { return "ModernCharacterSnapshot[redacted]"; }
    }
    static boolean validText(String text) {
        return text != null && !text.isEmpty() && text.length() <= MAX_TEXT_BYTES
                && text.codePoints().anyMatch(point -> !Character.isWhitespace(point) && !Character.isSpaceChar(point))
                && text.chars().noneMatch(Character::isISOControl)
                && StandardCharsets.UTF_8.newEncoder().canEncode(text)
                && text.getBytes(StandardCharsets.UTF_8).length <= MAX_TEXT_BYTES;
    }
    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("invalid modern persisted character data");
    }
}

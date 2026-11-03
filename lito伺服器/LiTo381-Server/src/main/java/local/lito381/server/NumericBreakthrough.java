package local.lito381.server;

/** Shared opt-in contract for the LiTo launcher/runtime 32-bit stat patches. */
final class NumericBreakthrough {
    static final int VITAL_ARMOR_SCHEMA_VERSION = 1;
    static final int LEVEL_ABILITY_SCHEMA_VERSION = 2;
    static final int MAGIC_MODIFIER_SCHEMA_VERSION = 3;
    static final int ELEMENTAL_RESISTANCE_SCHEMA_VERSION = 4;
    static final int MAX_VITAL = 999_999;
    static final int MIN_ARMOR_CLASS = -999_999;
    static final int MAX_ARMOR_CLASS = 999_999;
    static final int MAX_LEVEL = 999_999;
    static final int MAX_ABILITY = 999_999;
    static final int MIN_ABILITY_MODIFIER = -999_999;
    static final int MAX_ABILITY_MODIFIER = 999_999;
    static final int MIN_MAGIC_MODIFIER = -999_999;
    static final int MAX_MAGIC_MODIFIER = 999_999;
    static final int MIN_ELEMENTAL_RESISTANCE = -999_999;
    static final int MAX_ELEMENTAL_RESISTANCE = 999_999;
    private static final String ENVIRONMENT = "LITO_NUMERIC_BREAKTHROUGH";
    private static final String PROPERTY = "lito.numeric.breakthrough";

    private NumericBreakthrough() {}

    static int schemaVersion() {
        String property = System.getProperty(PROPERTY);
        String value = property != null ? property : System.getenv(ENVIRONMENT);
        if (value == null) return 0;
        try {
            int parsed = Integer.parseInt(value);
            return parsed >= VITAL_ARMOR_SCHEMA_VERSION
                    && parsed <= ELEMENTAL_RESISTANCE_SCHEMA_VERSION ? parsed : 0;
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    static boolean enabled() {
        return schemaVersion() >= VITAL_ARMOR_SCHEMA_VERSION;
    }

    static boolean levelAbilityEnabled() {
        return schemaVersion() >= LEVEL_ABILITY_SCHEMA_VERSION;
    }

    static boolean magicModifierEnabled() {
        return schemaVersion() >= MAGIC_MODIFIER_SCHEMA_VERSION;
    }

    static boolean elementalResistanceEnabled() {
        return schemaVersion() >= ELEMENTAL_RESISTANCE_SCHEMA_VERSION;
    }

    static int clampMagicModifier(long value) {
        long minimum = magicModifierEnabled() ? MIN_MAGIC_MODIFIER : Byte.MIN_VALUE;
        long maximum = magicModifierEnabled() ? MAX_MAGIC_MODIFIER : Byte.MAX_VALUE;
        return (int) Math.max(minimum, Math.min(maximum, value));
    }

    static int clampElementalResistance(long value) {
        long minimum = elementalResistanceEnabled()
                ? MIN_ELEMENTAL_RESISTANCE : Byte.MIN_VALUE;
        long maximum = elementalResistanceEnabled()
                ? MAX_ELEMENTAL_RESISTANCE : Byte.MAX_VALUE;
        return (int) Math.max(minimum, Math.min(maximum, value));
    }

    static int clampVital(long value) {
        int maximum = enabled() ? MAX_VITAL : NativeCharacterStats.MAX_RUNTIME_VITAL;
        return (int) Math.max(1L, Math.min((long) maximum, value));
    }

    /**
     * Keeps the authoritative runtime ability value for Schema 2 while
     * retaining the exact one-byte-safe ceiling for unpatched clients.
     */
    static int clampAbility(long value) {
        int maximum = levelAbilityEnabled()
                ? MAX_ABILITY : NativeCharacterStats.MAX_RUNTIME_ABILITY;
        return (int) Math.max(1L, Math.min((long) maximum, value));
    }

    /**
     * Preserves signed equipment ability deltas for the Schema 2 dword
     * receiver.  Legacy clients get a saturated native shadow instead of a
     * modulo-256 wrap, so an out-of-range database value cannot invert sign.
     */
    static int clampAbilityModifier(long value) {
        long minimum = levelAbilityEnabled() ? MIN_ABILITY_MODIFIER : Byte.MIN_VALUE;
        long maximum = levelAbilityEnabled() ? MAX_ABILITY_MODIFIER : Byte.MAX_VALUE;
        return (int) Math.max(minimum, Math.min(maximum, value));
    }

    /**
     * Keeps classic level/experience mechanics independent from the level
     * published to a Schema 2 client.  The database level becomes authoritative
     * only when it is above the classic rules ceiling; native clients continue
     * to receive the gameplay-safe projection.
     */
    static int clientStatusLevel(long sourceLevel, long classicLevel) {
        long selected = levelAbilityEnabled()
                && sourceLevel > ClassicExperienceCatalog.MAXIMUM_LEVEL
                ? sourceLevel : classicLevel;
        return NativeCharacterStats.nativeLevel(selected);
    }

    static int clampArmorClass(long value) {
        if (!enabled()) return NativeCharacterStats.signedByte(value);
        return (int) Math.max((long) MIN_ARMOR_CLASS,
                Math.min((long) MAX_ARMOR_CLASS, value));
    }

    /**
     * Saturates imported database aggregates before they can wrap an int.
     * The same symmetric range is the largest value any extended stat schema
     * can publish, so later native/schema-specific projections remain exact.
     */
    static int clampExtendedValue(long value) {
        return (int) Math.max((long) MIN_ABILITY_MODIFIER,
                Math.min((long) MAX_ABILITY_MODIFIER, value));
    }
}

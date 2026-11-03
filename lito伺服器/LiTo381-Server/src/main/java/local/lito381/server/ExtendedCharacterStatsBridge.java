package local.lito381.server;

/** Versioned server-to-launcher telemetry carried by the managed JVM stdout. */
final class ExtendedCharacterStatsBridge {
    static final String MARKER = "[LiTo-Extended-Stats] ";
    static final int SCHEMA_VERSION = 2;
    private static final String CAPABILITY_ENVIRONMENT = "LITO_EXTENDED_STATS_SCHEMA";

    private ExtendedCharacterStatsBridge() {}

    static boolean enabled() {
        return Integer.toString(SCHEMA_VERSION).equals(
                System.getenv(CAPABILITY_ENVIRONMENT));
    }

    static void publish(
            String event, String source, CharacterSummary character,
            NativeCharacterStats.DirectoryProjection directoryShadow) {
        if (!enabled()) return;
        System.out.println(event(event, source, "", character,
                directoryShadow, null));
    }

    static void publishRuntime(
            String source, String reason, CharacterSummary character,
            RuntimeProjection runtime) {
        if (!enabled()) return;
        System.out.println(event("RUNTIME", source, reason, character,
                NativeCharacterStats.directoryProjection(character), runtime));
    }

    static String event(
            String event, String source, CharacterSummary character,
            NativeCharacterStats.DirectoryProjection directoryShadow) {
        return event(event, source, "", character, directoryShadow, null);
    }

    static String runtimeEvent(
            String source, String reason, CharacterSummary character,
            RuntimeProjection runtime) {
        return event("RUNTIME", source, reason, character,
                NativeCharacterStats.directoryProjection(character), runtime);
    }

    private static String event(
            String event, String source, String reason, CharacterSummary character,
            NativeCharacterStats.DirectoryProjection directoryShadow,
            RuntimeProjection runtime) {
        if (event == null || event.isBlank() || source == null || character == null
                || reason == null || directoryShadow == null) {
            throw new IllegalArgumentException("invalid extended character stats event");
        }
        return MARKER + "{\"schemaVersion\":" + SCHEMA_VERSION
                + ",\"numericBreakthroughSchema\":"
                + NumericBreakthrough.schemaVersion()
                + ",\"event\":" + quote(event)
                + ",\"source\":" + quote(source)
                + ",\"character\":" + quote(character.name())
                + ",\"reason\":" + quote(reason)
                + ",\"database\":{"
                + fields(character.maxHp(), character.maxMp(), character.armorClass(),
                        character.level(), character.strength(), character.dexterity(),
                        character.constitution(), character.wisdom(), character.charisma(),
                        character.intelligence())
                + "},\"directoryShadow\":{"
                + fields(directoryShadow.maxHp(), directoryShadow.maxMp(),
                        directoryShadow.armorClass(), directoryShadow.level(),
                        directoryShadow.strength(), directoryShadow.dexterity(),
                        directoryShadow.constitution(), directoryShadow.wisdom(),
                        directoryShadow.charisma(), directoryShadow.intelligence())
                + "},\"runtime\":" + runtimeFields(runtime)
                + ",\"adjustedDirectory\":"
                + directoryShadow.adjusted(character) + "}";
    }

    private static String runtimeFields(RuntimeProjection runtime) {
        if (runtime == null) return "null";
        return "{" + fields(runtime.maxHp(), runtime.maxMp(), runtime.armorClass(),
                        runtime.level(), runtime.strength(), runtime.dexterity(),
                        runtime.constitution(), runtime.wisdom(), runtime.charisma(),
                        runtime.intelligence())
                + ",\"spellPowerModifier\":" + runtime.spellPowerModifier()
                + ",\"magicResistanceModifier\":"
                + runtime.magicResistanceModifier()
                + ",\"fireResistance\":" + runtime.fireResistance()
                + ",\"waterResistance\":" + runtime.waterResistance()
                + ",\"windResistance\":" + runtime.windResistance()
                + ",\"earthResistance\":" + runtime.earthResistance() + "}";
    }

    private static String fields(
            int maxHp, int maxMp, int armorClass, int level,
            int strength, int dexterity, int constitution, int wisdom,
            int charisma, int intelligence) {
        return "\"maxHp\":" + maxHp + ",\"maxMp\":" + maxMp
                + ",\"armorClass\":" + armorClass + ",\"level\":" + level
                + ",\"strength\":" + strength + ",\"dexterity\":" + dexterity
                + ",\"constitution\":" + constitution + ",\"wisdom\":" + wisdom
                + ",\"charisma\":" + charisma + ",\"intelligence\":" + intelligence;
    }

    private static String quote(String value) {
        String text = value == null ? "" : value;
        StringBuilder escaped = new StringBuilder(text.length() + 2).append('"');
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.append('"').toString();
    }

    record RuntimeProjection(
            int maxHp, int maxMp, int armorClass, int level,
            int strength, int dexterity, int constitution, int wisdom,
            int charisma, int intelligence,
            int spellPowerModifier, int magicResistanceModifier,
            int fireResistance, int waterResistance,
            int windResistance, int earthResistance) {}
}

package local.lito381.server;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Source facts and allow-listed GM syntax. This is not a cached execution grant. */
final class ModernGmCommands {
    static final String NAME = "de", EXECUTOR = "L1SpawnDe";
    static final int MAX_REQUESTED_BOTS = 100;
    private ModernGmCommands() {}

    enum Decision { ALLOWED, ACCOUNT_BANNED, CHARACTER_BANNED, COMMAND_DISABLED, UNSUPPORTED_EXECUTOR, INSUFFICIENT_ACCESS }

    /** Positive adds up to available capacity; zero removes this room's test bots. */
    record SpawnRequest(int count) {
        SpawnRequest { if (count < 0 || count > MAX_REQUESTED_BOTS) throw invalid(); }
        static SpawnRequest parse(String text) {
            if (text == null || text.length() > 32) throw invalid();
            // Do not let Unicode digits, signs, a second command, controls or
            // arbitrary class names become an executable command language.
            if (!text.matches("(?i) *\\.de +(?:0|[1-9][0-9]{0,2}) *")) throw invalid();
            String value = text.trim().substring(3).trim();
            return new SpawnRequest(Integer.parseInt(value));
        }
        @Override public String toString() { return "ModernGmSpawnRequest[redacted]"; }
    }

    record Definition(String name, int requiredAccess, String executor, boolean system) {
        Definition {
            if (!NAME.equals(name) || requiredAccess < 0 || requiredAccess > 200 || !validText(executor, 128)) throw invalid();
        }
        boolean supported() { return EXECUTOR.equals(executor) && !system; }
        @Override public String toString() { return "ModernGmDefinition[redacted]"; }
    }

    /** Fresh single-statement source evidence, not permission retained from AUTH. */
    record Snapshot(int sourceId, String canonicalAccount, ModernApplicationProtocol.CharacterRow character,
                    int accessLevel, boolean accountBanned, boolean characterBanned, Definition definition) {
        Snapshot {
            if (sourceId <= 0 || !validText(canonicalAccount, 64) || accessLevel < 0 || accessLevel > 200) throw invalid();
            Objects.requireNonNull(character);
            if (character.objectId() <= 0 || !validText(character.name(),256) || character.level() < 1
                    || character.characterClass() < 0 || character.characterClass() > 65535
                    || character.sex() < 0 || character.sex() > 1) throw invalid();
        }
        Decision decision() {
            if (accountBanned) return Decision.ACCOUNT_BANNED;
            if (characterBanned) return Decision.CHARACTER_BANNED;
            if (definition == null) return Decision.COMMAND_DISABLED;
            if (!definition.supported()) return Decision.UNSUPPORTED_EXECUTOR;
            return accessLevel >= definition.requiredAccess() ? Decision.ALLOWED : Decision.INSUFFICIENT_ACCESS;
        }
        @Override public String toString() { return "ModernGmSnapshot[redacted]"; }
    }

    static boolean validText(String value, int bytes) {
        return value != null && !value.isBlank() && value.codePoints().noneMatch(Character::isISOControl)
                && StandardCharsets.UTF_8.newEncoder().canEncode(value)
                && value.getBytes(StandardCharsets.UTF_8).length <= bytes;
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("invalid modern GM value"); }
}

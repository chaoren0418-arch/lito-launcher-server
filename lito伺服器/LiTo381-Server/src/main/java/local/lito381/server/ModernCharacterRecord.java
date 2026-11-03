package local.lito381.server;

import java.util.Objects;

/** Confirmed central SQL totals, never an estimate from the current match. */
final class ModernCharacterRecord {
    private ModernCharacterRecord() {}

    record Snapshot(TournamentPresence.CharacterKey key, boolean rowPresent,
                    long wins, long losses, long revision) {
        Snapshot {
            Objects.requireNonNull(key);
            if (wins < 0 || losses < 0 || revision < 0 || Math.addExact(wins, losses) != revision
                    || !rowPresent && (wins != 0 || losses != 0 || revision != 0))
                throw new IllegalArgumentException("invalid modern character record");
        }
        static Snapshot absent(TournamentPresence.CharacterKey key) { return new Snapshot(key, false, 0, 0, 0); }
        @Override public String toString() { return "ModernCharacterRecord.Snapshot[redacted]"; }
    }
}

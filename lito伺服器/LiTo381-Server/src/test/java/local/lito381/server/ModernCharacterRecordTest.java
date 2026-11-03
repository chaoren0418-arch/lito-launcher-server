package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ModernCharacterRecordTest {
    private static final TournamentPresence.CharacterKey KEY = new TournamentPresence.CharacterKey(7, Long.MAX_VALUE);

    @Test void absentAndExistingZeroRowsAreDistinguished() {
        var absent = ModernCharacterRecord.Snapshot.absent(KEY);
        assertFalse(absent.rowPresent()); assertEquals(0, absent.revision());
        assertNotEquals(absent, new ModernCharacterRecord.Snapshot(KEY, true, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ModernCharacterRecord.Snapshot(KEY, false, 1, 0, 1));
    }
    @Test void countersAreExactSignedI64AndSourceObjectIsTheKey() {
        var wide = new ModernCharacterRecord.Snapshot(KEY, true, Long.MAX_VALUE - 1, 1, Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, wide.revision()); assertEquals(KEY, wide.key());
        var other = new ModernCharacterRecord.Snapshot(new TournamentPresence.CharacterKey(42, Long.MAX_VALUE), true,
                Long.MAX_VALUE - 1, 1, Long.MAX_VALUE);
        assertNotEquals(wide, other); assertEquals("ModernCharacterRecord.Snapshot[redacted]", wide.toString());
    }
    @Test void negativeInconsistentAndOverflowValuesAreRejected() {
        for (long[] values : new long[][]{{-1,0,0},{0,-1,0},{0,0,-1},{1,0,0},{1,1,1},{0,0,1}})
            assertThrows(IllegalArgumentException.class, () -> new ModernCharacterRecord.Snapshot(KEY, true, values[0], values[1], values[2]));
        assertThrows(ArithmeticException.class, () -> new ModernCharacterRecord.Snapshot(KEY, true, Long.MAX_VALUE, 1, Long.MAX_VALUE));
        assertThrows(NullPointerException.class, () -> new ModernCharacterRecord.Snapshot(null, false, 0, 0, 0));
    }
}

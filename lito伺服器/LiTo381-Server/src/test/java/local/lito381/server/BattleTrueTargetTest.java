package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

final class BattleTrueTargetTest {
    @Test
    void resolvesEveryRecoveredTournamentCharacterDomain() {
        List<BattleTrueTarget.Candidate> candidates = new ArrayList<>();
        int objectId = 1;
        for (BattleTrueTarget.Kind kind : BattleTrueTarget.Kind.values()) {
            candidates.add(new BattleTrueTarget.Candidate(
                    objectId++, 32740, 32800, kind, true, true, true));
        }
        for (BattleTrueTarget.Candidate expected : candidates) {
            assertEquals(expected,
                    BattleTrueTarget.resolve(expected.objectId(), candidates));
        }
    }

    @Test
    void rejectsDeadHiddenForeignUnknownAndAliasedTargets() {
        assertNull(BattleTrueTarget.resolve(1, List.of(
                new BattleTrueTarget.Candidate(1, 1, 1,
                        BattleTrueTarget.Kind.PLAYER, false, true, true))));
        assertNull(BattleTrueTarget.resolve(2, List.of(
                new BattleTrueTarget.Candidate(2, 1, 1,
                        BattleTrueTarget.Kind.PLAYER, true, false, true))));
        assertNull(BattleTrueTarget.resolve(3, List.of(
                new BattleTrueTarget.Candidate(3, 1, 1,
                        BattleTrueTarget.Kind.QUEST_MONSTER, true, true, false))));
        assertNull(BattleTrueTarget.resolve(4, List.of()));
        assertNull(BattleTrueTarget.resolve(5, List.of(
                new BattleTrueTarget.Candidate(5, 1, 1,
                        BattleTrueTarget.Kind.PLAYER, true, true, true),
                new BattleTrueTarget.Candidate(5, 2, 2,
                        BattleTrueTarget.Kind.ARENA_BOT, true, true, true))));
    }
}

package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class RepositoryBuffBridgeTest {
    @Test void mapsDragonGemBonusAndPreservesUnknownTwRows() throws Exception {
        RepositoryBuff unknown = new RepositoryBuff(9_999, -1, 77);
        RepositoryBuffSnapshot sql = new RepositoryBuffSnapshot(true, 81, List.of(
                new RepositoryBuff(52, 30, -1),
                new RepositoryBuff(148, 800, -1),
                new RepositoryBuff(4026, 500, 3_000),
                unknown));

        GameStateStore.TimedBuffSnapshot lito =
                ClientSession.litoTimedBuffSnapshot(sql);
        assertEquals(3_000, lito.experience().bonusExperience());
        assertEquals(500, lito.experience().remainingSeconds());
        assertEquals(800, lito.fireWeapon().remainingSeconds());
        assertEquals(52, lito.movement().skillId());

        List<RepositoryBuff> merged = ClientSession.mergeRepositoryBuffs(
                sql.buffs(), lito);
        assertTrue(merged.contains(unknown));
        assertTrue(merged.contains(new RepositoryBuff(4026, 500, 3_000)));
        assertTrue(merged.contains(new RepositoryBuff(148, 800, -1)));
    }

    @Test void legacyDragonGemFallsBackToRetainedDefaultBonus() throws Exception {
        GameStateStore.TimedBuffSnapshot lito = ClientSession.litoTimedBuffSnapshot(
                new RepositoryBuffSnapshot(true, 0,
                        List.of(new RepositoryBuff(4026, 400, -1))));
        assertEquals(1_000, lito.experience().bonusExperience());
    }

    @Test void preservesExtraTwRowsThatLitoModelsAsOneExclusiveLease()
            throws Exception {
        RepositoryBuffSnapshot sql = new RepositoryBuffSnapshot(true, 9, List.of(
                new RepositoryBuff(52, 30, -1),
                new RepositoryBuff(101, 200, -1)));
        GameStateStore.TimedBuffSnapshot lito =
                ClientSession.litoTimedBuffSnapshot(sql);
        assertEquals(52, lito.movement().skillId());
        List<RepositoryBuff> merged = ClientSession.mergeRepositoryBuffs(
                sql.buffs(), lito);
        assertTrue(merged.contains(new RepositoryBuff(52, 30, -1)));
        assertTrue(merged.contains(new RepositoryBuff(101, 200, -1)));
    }
}

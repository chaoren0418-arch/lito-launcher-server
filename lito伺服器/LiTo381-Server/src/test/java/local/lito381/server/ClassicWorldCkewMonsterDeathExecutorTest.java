package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ClassicWorldCkewMonsterDeathExecutorTest {
    @Test
    void retainedQuestDropsKeepTheirExactGuardsAndFortyPercentBoundary() {
        for (int npcId : new int[] {87001, 87002, 87003}) {
            assertEquals(49165, ClassicWorldCkewMonsterDeathExecutor.questItem(
                    npcId, false, false, false, false, 39));
            assertEquals(0, ClassicWorldCkewMonsterDeathExecutor.questItem(
                    npcId, false, false, false, false, 40));
            assertEquals(0, ClassicWorldCkewMonsterDeathExecutor.questItem(
                    npcId, false, true, false, false, 0));
        }
        assertEquals(49166, ClassicWorldCkewMonsterDeathExecutor.questItem(
                87007, false, false, false, false, 99));
        assertEquals(0, ClassicWorldCkewMonsterDeathExecutor.questItem(
                87010, false, false, true, false, 0));
        assertEquals(49167, ClassicWorldCkewMonsterDeathExecutor.questItem(
                87011, false, false, false, false, 39));
        assertEquals(49167, ClassicWorldCkewMonsterDeathExecutor.questItem(
                87011, false, false, false, false, 0));
        assertEquals(0, ClassicWorldCkewMonsterDeathExecutor.questItem(
                87011, false, false, false, false, 40));
        for (int npcId : new int[] {87015, 87016, 87017, 87018}) {
            assertEquals(49168, ClassicWorldCkewMonsterDeathExecutor.questItem(
                    npcId, false, false, false, false, 99));
            assertEquals(0, ClassicWorldCkewMonsterDeathExecutor.questItem(
                    npcId, false, false, false, true, 0));
        }
        for (int npcId : new int[] {87004, 87005, 87006, 87012, 87013, 87014}) {
            assertEquals(0, ClassicWorldCkewMonsterDeathExecutor.questItem(
                    npcId, false, false, false, false, 0));
        }
    }

    @Test
    void destructionMedicineSuppressesEveryExecutorDrop() {
        for (int npcId = 87004; npcId <= 87018; npcId++) {
            assertEquals(0, ClassicWorldCkewMonsterDeathExecutor.questItem(
                    npcId, true, false, false, false, 0));
        }
        assertThrows(IllegalArgumentException.class, () ->
                ClassicWorldCkewMonsterDeathExecutor.questItem(
                        87001, false, false, false, false, 100));
    }
}

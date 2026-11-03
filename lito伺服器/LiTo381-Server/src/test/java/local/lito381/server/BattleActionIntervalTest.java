package local.lito381.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BattleActionIntervalTest {
    @Test
    void hasteUsesSelectedClassicServerMultiplierAndIntegerTruncation() {
        assertEquals(640, BattleActionInterval.npcMillis(61, 0, false));
        assertEquals(483, BattleActionInterval.npcMillis(61, 0, true));
        assertEquals(800, BattleActionInterval.npcMillis(61, 1, false));
        assertEquals(604, BattleActionInterval.npcMillis(61, 1, true));
        assertEquals(1_280,
                BattleActionInterval.npcMillis(61, 0, false, true));
        assertEquals(1_280,
                BattleActionInterval.npcMillis(61, 0, true, true));
        assertEquals(640, BattleActionInterval.movementMillis(61, 0, false, false));
        assertEquals(483, BattleActionInterval.movementMillis(61, 0, true, false));
        assertEquals(425, BattleActionInterval.movementMillis(61, 0, false, true));
        assertEquals(321, BattleActionInterval.movementMillis(61, 0, true, true));
        assertEquals(1_280, BattleActionInterval.movementMillis(
                61, 0, false, true, false));
        assertEquals(640,
                BattleActionInterval.npcSourceMillis(640, false, false));
        assertEquals(483,
                BattleActionInterval.npcSourceMillis(640, true, false));
        assertEquals(1_280,
                BattleActionInterval.npcSourceMillis(640, false, true));
        assertEquals(1_280,
                BattleActionInterval.npcSourceMillis(640, true, true));
        assertEquals(800,
                BattleActionInterval.npcSourceMillis(640, false, false, true));
        assertEquals(603,
                BattleActionInterval.npcSourceMillis(640, true, false, true));
        assertEquals(1_600,
                BattleActionInterval.npcSourceMillis(640, false, true, true));
        // Wind Shackle is an attack/spell calculator branch, never movement.
        assertEquals(640,
                BattleActionInterval.movementMillis(61, 0, false, false));
        assertEquals(800,
                BattleActionInterval.attackMillis(61, 1, false, false, false));
        assertEquals(620,
                BattleActionInterval.attackMillis(61, 1, false, false, true));
        assertEquals(468,
                BattleActionInterval.attackMillis(61, 1, true, false, true));
        assertEquals(1_240,
                BattleActionInterval.attackMillis(61, 1, false, true, true));
    }
}

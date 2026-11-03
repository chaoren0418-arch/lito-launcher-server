package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ClassicWorldCastleDoorCombatTest {
    @Test
    void kentGateUsesNativeEntranceEdgeSixthsAndDeathPassability() {
        ClassicWorldKentCastleDoorState state = new ClassicWorldKentCastleDoorState();
        ClassicWorldKentCastleCatalog.Door outer =
                ClassicWorldKentCastleCatalog.doors().stream()
                        .filter(door -> door.doorId() == 2001)
                        .findFirst().orElseThrow();
        ClassicWorldCastleDoorCombat.Target target = state.combatSnapshot(
                4, outer.objectId(), 33_110, 32_770);
        assertEquals(33_110, target.combat().x());
        assertEquals(32_770, target.combat().y());
        assertEquals(20_000, target.combat().maximumHp());
        assertTrue(state.blocks(4, 33_110, 32_770));

        ClassicWorldCastleDoorCombat.Hit crack = state.damage(
                4, outer.objectId(), 3_335);
        assertEquals(BattleActionCodes.DOOR_CRACK_1, crack.action());
        assertEquals(1, crack.after().crackStage());
        ClassicWorldCastleDoorCombat.Hit death = state.damage(
                4, outer.objectId(), 99_999);
        assertTrue(death.diedNow());
        assertEquals(BattleActionCodes.DOOR_DIE, death.action());
        assertTrue(death.after().open());
        assertFalse(state.blocks(4, 33_110, 32_770));
        assertNull(state.combatSnapshot(4, outer.objectId(), 33_110, 32_770));

        state.repairAll();
        assertTrue(state.blocks(4, 33_110, 32_770));
        assertEquals(20_000, state.combatSnapshot(
                4, outer.objectId(), 33_110, 32_770).combat().currentHp());
    }

    @Test
    void allRetainedMainlandFamiliesExposeOnlyDestructibleDoors() {
        ClassicWorldWindawoodCastleDoorState wind =
                new ClassicWorldWindawoodCastleDoorState();
        assertEquals(2, wind.views().stream().filter(
                ClassicWorldCastleDoorCombat.View::destructible).count());
        ClassicWorldCastleDoorState giran = new ClassicWorldCastleDoorState();
        assertEquals(5, giran.views().stream().filter(
                ClassicWorldCastleDoorCombat.View::destructible).count());
        ClassicWorldHeineCastleDoorState heine =
                new ClassicWorldHeineCastleDoorState();
        assertEquals(2, heine.views().stream().filter(
                ClassicWorldCastleDoorCombat.View::destructible).count());

        ClassicWorldWindawoodCastleCatalog.Door fixed =
                ClassicWorldWindawoodCastleCatalog.doors().stream()
                        .filter(door -> door.maximumHp() == 0)
                        .findFirst().orElseThrow();
        assertNull(wind.combatSnapshot(4, fixed.objectId(), fixed.x(), fixed.y()));
        assertNull(wind.damage(4, fixed.objectId(), 1));
    }

    @Test
    void heineDestroyedGateCannotBeClosedUntilRepair() {
        ClassicWorldHeineCastleDoorState state =
                new ClassicWorldHeineCastleDoorState();
        ClassicWorldHeineCatalog.Door door = ClassicWorldHeineCatalog.doors().stream()
                .filter(candidate -> candidate.maximumHp() == 3_000)
                .findFirst().orElseThrow();
        ClassicWorldCastleDoorCombat.Hit death = state.damage(
                4, door.objectId(), 3_000);
        assertTrue(death.diedNow());
        assertTrue(state.snapshots().stream().filter(snapshot ->
                snapshot.definition().doorId() == door.doorId())
                .findFirst().orElseThrow().open());
        state.setOpenForKeeper(door.keeperNpcId(), false);
        assertTrue(state.snapshots().stream().filter(snapshot ->
                snapshot.definition().doorId() == door.doorId())
                .findFirst().orElseThrow().open());
        state.repairAll();
        assertFalse(state.snapshots().stream().filter(snapshot ->
                snapshot.definition().doorId() == door.doorId())
                .findFirst().orElseThrow().open());
    }
}

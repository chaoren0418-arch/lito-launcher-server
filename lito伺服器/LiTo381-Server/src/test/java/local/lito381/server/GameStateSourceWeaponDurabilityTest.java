package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStateSourceWeaponDurabilityTest {
    @TempDir Path temporary;

    @Test void damagePersistsWithoutMutatingTheReadOnlySourceSnapshot() throws Exception {
        Path file = temporary.resolve("state.properties");
        SourceWeaponPhysicalStats source = weapon(0);
        GameStateStore state = new GameStateStore(file, true);

        assertEquals(new GameStateStore.SourceWeaponDurabilityBalance(69, 0, 0),
                state.sourceWeaponDurabilityBalance("TestEdoryu", source));
        assertEquals(new GameStateStore.SourceWeaponDurabilityBalance(69, 0, 1),
                state.damageSourceWeapon("TestEdoryu", source));
        assertEquals(0, source.durability());

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(new GameStateStore.SourceWeaponDurabilityBalance(69, 0, 1),
                reloaded.sourceWeaponDurabilityBalance("TestEdoryu", source));
    }

    @Test void externalSourceDurabilityChangeRebasesTheOverlay() throws Exception {
        Path file = temporary.resolve("state.properties");
        GameStateStore state = new GameStateStore(file, true);
        state.damageSourceWeapon("TestEdoryu", weapon(0));

        assertEquals(new GameStateStore.SourceWeaponDurabilityBalance(69, 2, 2),
                state.sourceWeaponDurabilityBalance("TestEdoryu", weapon(2)));
        assertEquals(new GameStateStore.SourceWeaponDurabilityBalance(69, 2, 3),
                state.damageSourceWeapon("TestEdoryu", weapon(2)));
    }

    @Test void weaponBreakCanCommitMultiplePointsAtomically() throws Exception {
        Path file = temporary.resolve("state.properties");
        GameStateStore state = new GameStateStore(file, true);
        assertEquals(new GameStateStore.SourceWeaponDurabilityBalance(69, 0, 6),
                state.damageSourceWeapon("TestEdoryu", weapon(0), 6));
        assertEquals(new GameStateStore.SourceWeaponDurabilityBalance(69, 0, 11),
                state.damageSourceWeapon("TestEdoryu", weapon(0), 127));
        assertEquals(new GameStateStore.SourceWeaponDurabilityBalance(69, 0, 11),
                state.damageSourceWeapon("TestEdoryu", weapon(0), 1));
    }

    private static SourceWeaponPhysicalStats weapon(int durability) {
        return new SourceWeaponPhysicalStats(
                69, 12, 6, 6, 1, 0, 0, 6, durability);
    }
}

package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStateSpellShopTest {
    @TempDir Path temporary;

    @Test
    void persistsAdenaChargeAndPurchasedSkillsTogether() throws Exception {
        Path file = temporary.resolve("spell-state.properties");
        GameStateStore state = new GameStateStore(file, true);
        Map<Long, Integer> inventory = new LinkedHashMap<>(
                state.economy("TestKnight").inventory());
        inventory.put(InventoryCatalog.ADENA, 94_000);
        state.saveSpellPurchase("TestKnight", 5_000, inventory, Set.of(2, 9));

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(94_000, reloaded.economy("TestKnight").inventory()
                .get(InventoryCatalog.ADENA));
        assertEquals(Set.of(2, 9), reloaded.purchasedSkills("TestKnight"));
        String source = Files.readString(file);
        assertTrue(source.contains("format.version=43"));
    }

    @Test
    void keepsNonPersistentPurchasesOnlyInMemory() throws Exception {
        Path file = temporary.resolve("unused.properties");
        GameStateStore state = new GameStateStore(file, false);
        state.saveSpellPurchase("TestKnight", 5_000,
                Map.of(InventoryCatalog.ADENA, 94_000), Set.of(2));
        assertEquals(Set.of(2), state.purchasedSkills("TestKnight"));
        assertFalse(Files.exists(file));
    }

    @Test
    void refusesToPersistClassicLevelZeroReservedSlots() throws Exception {
        GameStateStore state = new GameStateStore(
                temporary.resolve("reserved.properties"), false);
        for (int skillId = 92; skillId <= 96; skillId++) {
            int reserved = skillId;
            assertThrows(IllegalArgumentException.class,
                    () -> state.saveSpellPurchase("TestKnight", 5_000,
                            Map.of(InventoryCatalog.ADENA, 94_000),
                            Set.of(2, reserved)));
        }
        assertTrue(state.purchasedSkills("TestKnight").isEmpty());
    }
}

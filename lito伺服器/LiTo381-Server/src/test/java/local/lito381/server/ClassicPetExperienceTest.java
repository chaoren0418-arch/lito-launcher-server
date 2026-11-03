package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ClassicPetExperienceTest {
    @Test void sharesServerRateWithoutPlayerPenaltyAndGrowsOncePerLevel() {
        long levelSix = ClassicExperienceCatalog.minimumExperienceForLevel(6);
        GameStateStore.PetState state = new GameStateStore.PetState(
                "legacy\u001f1", 0x7000_0010L, 0x9000_0010L,
                (int) InventoryCatalog.PET_COLLAR, 45034,
                "牧羊犬", 5, 30, 5, levelSix - 10, 0);
        BattleCompanion pet = BattleCompanion.pet(
                state, SourcePetTemplate.memoryFixture(45034), 100, 120);

        ClassicPetExperience.Progress result = ClassicPetExperience.apply(
                pet.snapshot(), 5, 2.0D,
                SourcePetTemplate.memoryFixture(45034), (minimum, maximum) -> maximum);

        assertTrue(result.levelChanged());
        assertEquals(6, result.afterLevel());
        assertEquals(10, result.adjustedExperience());
        assertEquals(38, result.afterMaxHp());
        assertEquals(7, result.afterMaxMp());
    }

    @Test void ordinaryRewardChangesOnlyExperience() {
        GameStateStore.PetState state = new GameStateStore.PetState(
                "legacy\u001f1", 0x7000_0010L, 0x9000_0010L,
                (int) InventoryCatalog.PET_COLLAR, 45034,
                "牧羊犬", 5, 30, 5, 750, 0);
        BattleCompanion pet = BattleCompanion.pet(
                state, SourcePetTemplate.memoryFixture(45034), 100, 120);

        ClassicPetExperience.Progress result = ClassicPetExperience.apply(
                pet.snapshot(), 1, 1.0D,
                SourcePetTemplate.memoryFixture(45034), (minimum, maximum) -> minimum);

        assertFalse(result.levelChanged());
        assertEquals(751, result.afterExperience());
        assertEquals(30, result.afterMaxHp());
        assertEquals(5, result.afterMaxMp());
    }
}

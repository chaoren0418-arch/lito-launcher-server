package local.lito381.server;

import java.util.Set;

/**
 * Explicit server-owned passive room-test policy, NOT a source database character,
 * equipment evaluation, native NPC template, growth-table row or learned-skill grant.
 * Class/sex choose a qualified base appearance only; they do not alter this template's
 * fixed defense. All offensive entry points remain disabled for this passive template.
 */
record ModernBotTemplate(int characterClass, int sex) {
    static final String ROOM_TEST_DUMMY_V1 = "ROOM_TEST_DUMMY_V1";

    ModernBotTemplate {
        if (characterClass < 0 || characterClass > 6 || sex < 0 || sex > 1)
            throw new IllegalArgumentException("invalid modern bot template appearance");
    }

    static ModernBotTemplate roomTestDummy(int characterClass, int sex) {
        return new ModernBotTemplate(characterClass, sex);
    }

    String templateId() { return ROOM_TEST_DUMMY_V1; }
    int templateVersion() { return 1; }
    long level() { return 80; }
    long initialHp() { return 40; }
    long maxHp() { return 40; }
    long initialMp() { return 0; }
    long maxMp() { return 0; }
    long armorClass() { return 10; }
    long magicResistance() { return 40; }
    int weaponType() { return 0; }
    long attackRange() { return 1; }
    long lightArrowMpCost() { return 3; }
    boolean passive() { return true; }
    Set<Integer> learnedSkills() { return Set.of(); }

    ModernPhysicalCombat.Values physicalValues() {
        // Offense is intentionally inert: the typed adapter rejects this BOT as
        // an attacker before RNG. No fictitious STR/original/SQL growth is supplied.
        var offense = new ModernPhysicalCombat.Offense(level(), 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0);
        var defense = new ModernPhysicalCombat.Defense(armorClass(), 0, 0, 0, 0, 0);
        return new ModernPhysicalCombat.Values(true, maxHp(), offense, defense, true, null,
                new ModernPhysicalCombat.Rational(1, 1));
    }

    ModernMagicCombat.Values magicValues() {
        // MR40 is the complete named-template defense, not an inferred WIS bonus.
        // The unused offense cannot grant skill4, MP or a cast command.
        var offense = new ModernMagicCombat.Offense(0, 0, 1, 0, 0, 0, 0);
        var defense = new ModernMagicCombat.Defense(0, 0, 0, 0, 0, magicResistance(), 0);
        return new ModernMagicCombat.Values(true, maxHp(), offense, defense);
    }

    @Override public String toString() { return ROOM_TEST_DUMMY_V1; }
}

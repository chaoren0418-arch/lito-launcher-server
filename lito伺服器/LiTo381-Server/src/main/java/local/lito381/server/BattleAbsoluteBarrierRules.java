package local.lito381.server;

import java.util.Set;

/** Retained 3.80 target and damage rules for skill 78, Absolute Barrier. */
final class BattleAbsoluteBarrierRules {
    private static final Set<Integer> ALLOWED_INCOMING_SKILLS = Set.of(
            BattleSpellCatalog.CURSE_BLIND,
            27, // WEAPON_BREAK
            BattleSpellCatalog.DARKNESS,
            BattleSpellCatalog.WEAKNESS,
            BattleSpellCatalog.DISEASE,
            BattleSpellCatalog.FOG_OF_SLEEPING,
            BattleSpellCatalog.MASS_SLOW,
            29, // SLOW
            BattleSpellCatalog.CANCELLATION,
            BattleSpellCatalog.SILENCE,
            BattleSpellCatalog.DECAY_POTION,
            BattleSpellCatalog.MASS_TELEPORT,
            BattleSpellCatalog.DETECTION,
            BattleSpellCatalog.COUNTER_DETECTION,
            BattleSpellCatalog.ENTANGLE,
            BattleSpellCatalog.ERASE_MAGIC,
            BattleSpellCatalog.PHYSICAL_ENCHANT_DEX,
            BattleSpellCatalog.PHYSICAL_ENCHANT_STR,
            BattleSpellCatalog.BLESS_WEAPON,
            BattleSpellCatalog.EARTH_SKIN,
            BattleSpellCatalog.IMMUNE_TO_HARM,
            BattleSpellCatalog.REMOVE_CURSE);

    private BattleAbsoluteBarrierRules() {}

    static boolean blocksIncomingSpell(boolean active, int skillId) {
        return active && !ALLOWED_INCOMING_SKILLS.contains(skillId);
    }

    static int protectedCombatDamage(boolean active, int damage) {
        return active ? 0 : Math.max(0, damage);
    }
}

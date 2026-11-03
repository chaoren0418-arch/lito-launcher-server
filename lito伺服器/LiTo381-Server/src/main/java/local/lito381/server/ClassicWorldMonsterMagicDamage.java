package local.lito381.server;

import java.util.function.IntUnaryOperator;

/** Deterministic retained arithmetic shared by WORLD NPC magic targets. */
final class ClassicWorldMonsterMagicDamage {
    private ClassicWorldMonsterMagicDamage() {}

    static int rollSourceDamage(
            ClassicWorldMonsterMagicSkillCatalog.Definition skill,
            IntUnaryOperator dieRoll) {
        if (skill == null || dieRoll == null) {
            throw new IllegalArgumentException("mob magic skill and die roller are required");
        }
        int damage = skill.baseDamage();
        for (int index = 0; index < skill.diceCount(); index++) {
            int roll = dieRoll.applyAsInt(skill.dieSides());
            if (roll < 1 || roll > skill.dieSides()) {
                throw new IllegalArgumentException("mob magic die roll outside die range");
            }
            damage = Math.addExact(damage, roll);
        }
        return damage;
    }

    static int afterElementAndLeverage(
            int sourceDamage, int casterIntelligence, int elementalResistance,
            int leverage) {
        if (leverage <= 0) throw new IllegalArgumentException("mob magic leverage must be positive");
        int elemental = BattleElementalResistance.magicDamage(
                sourceDamage, casterIntelligence, elementalResistance);
        return Math.max(0, (int) (elemental * (leverage / 10.0D)));
    }

    static int originalMagicHit(int casterIntelligence) {
        if (casterIntelligence < 0) {
            throw new IllegalArgumentException("negative NPC intelligence");
        }
        if (casterIntelligence < 18) return 1;
        if (casterIntelligence < 36) return 2;
        if (casterIntelligence < 72) return 3;
        return 4;
    }

    static int afterArmorAndResistance(
            int damage, int armorReduction, int targetMagicResistance,
            int casterIntelligence) {
        if (damage < 0 || armorReduction < 0 || targetMagicResistance < 0) {
            throw new IllegalArgumentException("negative mob magic defense input");
        }
        int preMr = Math.max(0, damage - armorReduction);
        return Math.max(0, BattleMagicResistance.damageAfterResistance(
                preMr, targetMagicResistance,
                originalMagicHit(casterIntelligence)));
    }

    static int companionElementalResistance(int weakAttribute, int spellAttribute) {
        return npcElementalResistance(weakAttribute, spellAttribute, 0);
    }

    /**
     * Retained {@code L1NpcInstance} initializes the matching weak attribute
     * to -50 (or the matching resistance marker to +50).  Status modifiers,
     * such as Elemental Fall Down, are additive on that live NPC value.
     */
    static int npcElementalResistance(
            int weakAttribute, int spellAttribute, int statusModifier) {
        if (!switch (weakAttribute) {
            case -8, -4, -2, -1, 0, 1, 2, 4, 8 -> true;
            default -> false;
        }) {
            throw new IllegalArgumentException("invalid NPC weak attribute");
        }
        int base = weakAttribute == 0
                || Math.abs(weakAttribute) != spellAttribute ? 0
                : weakAttribute > 0 ? -50 : 50;
        // Positive weakAttr initializes -50; negative weakAttr initializes +50.
        return Math.addExact(base, statusModifier);
    }
}

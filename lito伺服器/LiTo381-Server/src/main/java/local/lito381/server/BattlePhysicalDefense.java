package local.lito381.server;

/** Exact native PC/NPC AC and retained source physical-defense order. */
final class BattlePhysicalDefense {
    private BattlePhysicalDefense() {}

    record NpcOutcome(
            int damage, int acMaximum, int acRoll, int totalReduction,
            boolean hardRangedHalved) {}

    record PlayerOutcome(
            int damage, int acMaximum, int acRoll, int acReduction,
            int armorReduction, int attackerIgnoreReduction,
            int effectiveArmorReduction, int directionalReduction) {}

    static int npcAcMaximum(SourceNpcPhysicalStats target) {
        if (target == null) throw new IllegalArgumentException("missing NPC physical target");
        return Math.max(0, 10 - target.armorClass()) / 7;
    }

    static NpcOutcome againstNpc(
            int rawDamage, SourceNpcPhysicalStats target, int acRoll,
            boolean ranged, boolean pierce) {
        if (rawDamage < 0 || target == null) {
            throw new IllegalArgumentException("invalid NPC physical damage input");
        }
        int acMaximum = npcAcMaximum(target);
        if ((acMaximum == 0 && acRoll != 0)
                || (acMaximum > 0 && (acRoll < 0 || acRoll >= acMaximum))) {
            throw new IllegalArgumentException("invalid NPC AC reduction roll");
        }
        int acReduction = acMaximum == 0 ? 0 : Math.max(1, acMaximum) + acRoll;
        int totalReduction = Math.addExact(target.damageReduction(), acReduction);
        int damage = Math.max(0, rawDamage - totalReduction);
        boolean hardRangedHalved = ranged && target.hard() && !pierce;
        if (hardRangedHalved) damage /= 2;
        return new NpcOutcome(
                damage, acMaximum, acRoll, totalReduction, hardRangedHalved);
    }

    /** Exact original 3.81 L1ClassFeature.getAcDefenseMax divisors. */
    static int playerAcMaximum(int characterType, int armorClass) {
        int ac = Math.max(0, 10 - armorClass);
        return switch (characterType) {
            case 0, 2, 5 -> ac / 3; // royal, elf, dragon knight
            case 1 -> ac / 2;       // knight
            case 3 -> ac / 5;       // wizard
            case 4, 6 -> ac / 4;    // dark elf, illusionist
            default -> throw new IllegalArgumentException("unsupported player class");
        };
    }

    /** Exact original 3.81 CalcStat.calcAc level/original-DEX base. */
    static int playerBaseArmorClass(int level, int originalDexterity) {
        if (level < 0 || originalDexterity < 0) {
            throw new IllegalArgumentException("invalid player base-AC inputs");
        }
        int divisor;
        if (originalDexterity <= 9) divisor = 8;
        else if (originalDexterity <= 12) divisor = 7;
        else if (originalDexterity <= 15) divisor = 6;
        else if (originalDexterity <= 17) divisor = 5;
        else divisor = 4;
        return 10 - level / divisor;
    }

    static int playerArmorClass(
            int level, int originalDexterity, int equipmentModifier) {
        return Math.addExact(
                playerBaseArmorClass(level, originalDexterity), equipmentModifier);
    }

    /** Exact retained getDamageReductionByArmor random band. */
    static int armorReduction(int baseReduction, int randomOffset) {
        if (baseReduction <= 10) {
            if (randomOffset != 0) {
                throw new IllegalArgumentException("unexpected armor-reduction roll");
            }
            return baseReduction;
        }
        int bound = baseReduction - 10;
        if (randomOffset < 0 || randomOffset >= bound) {
            throw new IllegalArgumentException("invalid armor-reduction roll");
        }
        return 11 + randomOffset;
    }

    /**
     * Replays the selected Pc-to-Pc defense prefix: native AC dice, retained
     * armor reduction after attacker ignore-reduction, then near/far reduction.
     */
    static PlayerOutcome againstPlayer(
            int rawDamage, int characterType, int armorClass, int acRoll,
            int armorReductionBase, int armorReductionRoll,
            int attackerIgnoreReduction, int directionalReduction) {
        if (rawDamage < 0) {
            throw new IllegalArgumentException("invalid player physical damage input");
        }
        int acMaximum = playerAcMaximum(characterType, armorClass);
        if (acRoll < 0 || (acMaximum == 0 ? acRoll != 0 : acRoll >= acMaximum)) {
            throw new IllegalArgumentException("invalid player AC reduction roll");
        }
        int acReduction = acMaximum == 0
                ? 0 : acRoll + Math.max(1, acMaximum >> 3);
        int armorReduction = armorReduction(armorReductionBase, armorReductionRoll);
        int effectiveArmorReduction = Math.max(
                0, armorReduction - attackerIgnoreReduction);
        int effectiveDirectionalReduction = Math.max(0, directionalReduction);
        int totalReduction = Math.addExact(acReduction,
                Math.addExact(effectiveArmorReduction, effectiveDirectionalReduction));
        int damage = Math.max(0, rawDamage - totalReduction);
        damage = CombatRules.damageAfterExtraAc(damage, armorClass);
        return new PlayerOutcome(damage, acMaximum, acRoll, acReduction,
                armorReduction, attackerIgnoreReduction,
                effectiveArmorReduction, effectiveDirectionalReduction);
    }
}

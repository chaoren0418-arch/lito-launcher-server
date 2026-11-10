package local.lito381.server;

/**
 * Qualified same-family PC physical-hit core used by the LiTo battle loop.
 *
 * <p>The retained {@code L1AttackPc} first adds level, class correction,
 * STR/DEX correction, qualified equipment and cooking SKU1/SKU2 to
 * {@code _hitRate}. It then rolls
 * its d20 envelope against the target AC and resolves the result as either a
 * 15-percent fumble branch or a 100-percent hit branch.</p>
 */
final class BattlePhysicalAttack {
    private BattlePhysicalAttack() {}

    /**
     * Exact source-weapon reach used by every LiTo room physical target.
     *
     * <p>The immutable 3.81C {@code weapon.range} column distinguishes normal
     * one-tile weapons, two-tile spears and the independent bow/gauntlet
     * ranges.  An unequipped character retains the native one-tile unarmed
     * reach.</p>
     */
    static int maximumRange(SourceWeaponPhysicalStats weapon) {
        if (weapon == null) {
            throw new IllegalArgumentException("weapon is required");
        }
        return weapon.present() ? weapon.range() == -1 ? 19 : Math.max(1, weapon.range()) : 1;
    }

    static boolean supportsPosition(SourceWeaponPhysicalStats weapon,
            int sourceX, int sourceY, int targetX, int targetY) {
        if (weapon.range() == -1) {
            return BattleSpellCatalog.isInNativeScreen(sourceX, sourceY, targetX, targetY);
        }
        return supportsDistance(weapon, Math.max(Math.abs(sourceX - targetX),
                Math.abs(sourceY - targetY)));
    }

    static boolean supportsDistance(SourceWeaponPhysicalStats weapon, int distance) {
        if (distance < 0) {
            throw new IllegalArgumentException("negative physical-attack distance");
        }
        return distance <= maximumRange(weapon);
    }

    static int hitChance(int characterType, int level, int strength, int dexterity,
                         int targetArmorClass, boolean ranged, int cookingHitBonus,
                         int d20, int negativeAcRoll) {
        return hitChance(characterType, level, strength, dexterity,
                targetArmorClass, ranged, 0, cookingHitBonus, d20, negativeAcRoll);
    }

    static int hitChance(int characterType, int level, int strength, int dexterity,
                         int targetArmorClass, boolean ranged, int equipmentHitBonus,
                         int cookingHitBonus, int d20, int negativeAcRoll) {
        if (strength < 1 || dexterity < 1) {
            throw new IllegalArgumentException("invalid player physical-hit stat");
        }
        return hitChanceWithStatBonus(characterType, level,
                ranged ? dexterityHit(dexterity) : strengthHit(strength),
                targetArmorClass, equipmentHitBonus, cookingHitBonus,
                d20, negativeAcRoll);
    }

    static int hitChanceWithStatBonus(
            int characterType, int level, int statHitBonus,
            int targetArmorClass, int equipmentHitBonus, int cookingHitBonus,
            int d20, int negativeAcRoll) {
        return hitChanceWithStatBonus(characterType, level, statHitBonus,
                targetArmorClass, equipmentHitBonus, cookingHitBonus,
                0, d20, negativeAcRoll);
    }

    static int hitChanceWithStatBonus(
            int characterType, int level, int statHitBonus,
            int targetArmorClass, int equipmentHitBonus, int cookingHitBonus,
            int targetDodgeModifier, int d20, int negativeAcRoll) {
        return hitChanceWithStatBonus(characterType, level, statHitBonus,
                targetArmorClass, equipmentHitBonus, cookingHitBonus,
                targetDodgeModifier, 0, d20, negativeAcRoll);
    }

    static int hitChanceWithStatBonus(
            int characterType, int level, int statHitBonus,
            int targetArmorClass, int equipmentHitBonus, int cookingHitBonus,
            int targetDodgeModifier, int targetDodgeDown,
            int d20, int negativeAcRoll) {
        return hitChanceAgainst(false, characterType, 0, level, statHitBonus,
                targetArmorClass, equipmentHitBonus, cookingHitBonus,
                targetDodgeModifier, targetDodgeDown, d20, negativeAcRoll);
    }

    /**
     * Full PC physical-hit envelope (1-伺服器 L1AttackPc calcPcHit/calcNpcHit).
     *
     * <p>{@code npcTarget} selects the NPC defender die {@code 10 - AC}
     * (no negative-AC roll; L1AttackPc.java:487), while a player target keeps
     * {@code 10 + rnd((int)(AC*1.5)*-1) + 1} for negative AC
     * (L1AttackPc.java:361-368). The dragon-knight 1.01 and fire-elf 1.02
     * attacker-die scales (L1AttackPc.java:372-379 / 491-498) are applied
     * after the dodge modifiers, exactly like the reference compound
     * assignment.</p>
     */
    static int hitChanceAgainst(
            boolean npcTarget, int characterType, int elfAttribute,
            int level, int statHitBonus,
            int targetArmorClass, int equipmentHitBonus, int cookingHitBonus,
            int targetDodgeModifier, int targetDodgeDown,
            int d20, int negativeAcRoll) {
        if (characterType < 0 || characterType > 6 || level < 1
                || cookingHitBonus < 0
                || targetDodgeModifier < 0 || targetDodgeModifier > 10
                || targetDodgeDown < 0 || targetDodgeDown > 10
                || d20 < 1 || d20 > 20) {
            throw new IllegalArgumentException("invalid player physical-hit input");
        }
        int hitRate = Math.addExact(level, classHitCorrection(characterType, level));
        hitRate = Math.addExact(hitRate, statHitBonus);
        hitRate = Math.addExact(hitRate, equipmentHitBonus);
        hitRate = Math.addExact(hitRate, cookingHitBonus);

        int attackerDice = d20 + 1 + hitRate - 10
                - targetDodgeModifier + targetDodgeDown;
        attackerDice = classAttackerDice(characterType, elfAttribute, attackerDice);
        int defenderDice;
        if (npcTarget) {
            if (negativeAcRoll != 0) {
                throw new IllegalArgumentException("NPC AC has no defense roll");
            }
            defenderDice = Math.subtractExact(10, targetArmorClass);
        } else if (targetArmorClass >= 0) {
            if (negativeAcRoll != 0) {
                throw new IllegalArgumentException("positive AC cannot have a defense roll");
            }
            defenderDice = 10 - targetArmorClass;
        } else {
            int maximum = negativeAcRollMaximum(targetArmorClass);
            if (negativeAcRoll < 1 || negativeAcRoll > maximum) {
                throw new IllegalArgumentException("invalid negative-AC defense roll");
            }
            defenderDice = 10 + negativeAcRoll;
        }

        int fumble = hitRate - 9;
        int critical = hitRate + 10;
        if (attackerDice <= fumble) return 15;
        if (attackerDice >= critical) return 100;
        return attackerDice > defenderDice ? 100 : 15;
    }

    static boolean hits(int hitChance, int d100) {
        if ((hitChance != 15 && hitChance != 100) || d100 < 1 || d100 > 100) {
            throw new IllegalArgumentException("invalid player physical-hit roll");
        }
        return d100 <= hitChance;
    }

    /**
     * 1-伺服器 L1AttackPc.java:372-379: {@code attackerDice *= 1.01} for a
     * dragon knight and {@code *= 1.02} for an elf whose ElfAttr carries the
     * fire bit (2).  Java compound assignment truncates toward zero.
     */
    static int classAttackerDice(int characterType, int elfAttribute, int attackerDice) {
        if (characterType == 5) return (int) (attackerDice * 1.01);
        if (characterType == 2 && (elfAttribute & 2) != 0) return (int) (attackerDice * 1.02);
        return attackerDice;
    }

    static int negativeAcRollMaximum(int targetArmorClass) {
        if (targetArmorClass >= 0) return 0;
        // Same-family source casts AC*1.5 to int before negating it.
        return Math.max(1, -((int) (targetArmorClass * 1.5)));
    }

    static int strengthHit(int strength) {
        int value = Math.max(1, Math.min(SourcePhysicalGrowthStats.MAX_STAT, strength));
        if (value <= 8) return -2;
        if (value <= 10) return -1;
        if (value <= 12) return 0;
        if (value <= 14) return 1;
        if (value <= 16) return 2;
        if (value <= 18) return 3;
        if (value <= 20) return 4;
        return 5 + (value - 21) / 3;
    }

    static int dexterityHit(int dexterity) {
        int value = Math.max(1, Math.min(SourcePhysicalGrowthStats.MAX_STAT, dexterity));
        if (value <= 6) return -2;
        if (value <= 8) return -1;
        if (value <= 10) return 0;
        if (value <= 12) return 1;
        if (value <= 14) return 2;
        if (value <= 16) return 3;
        if (value <= 18) return 4;
        if (value <= 32) return value - 14;
        return 19 + (value - 33) / 3;
    }

    static int classHitCorrection(int characterType, int level) {
        return switch (characterType) {
            case 0 -> level / 15; // royal
            case 1 -> level / 10; // knight
            case 2 -> level / 30; // elf
            case 3 -> level / 40; // wizard
            case 4 -> level / 30; // dark elf
            case 5 -> level / 25; // dragon knight
            case 6 -> level / 40; // illusionist
            default -> throw new IllegalArgumentException("unsupported classic class");
        };
    }
}

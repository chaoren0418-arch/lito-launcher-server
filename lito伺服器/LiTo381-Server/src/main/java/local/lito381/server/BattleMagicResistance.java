package local.lito381.server;

/**
 * Exact, deterministic parts of the selected 3.81 classic server's magic
 * resistance calculation.
 *
 * <p>The live classic character can have additional equipment, set, card,
 * talent and timed-effect MR.  Those layers are intentionally not guessed by
 * this helper; callers must pass the qualified target MR snapshot they own.</p>
 */
final class BattleMagicResistance {
    private BattleMagicResistance() {}

    static int classBase(int characterType) {
        return switch (characterType) {
            case 0 -> 10; // crown
            case 1 -> 0;  // knight
            case 2 -> 25; // elf
            case 3 -> 15; // wizard
            case 4 -> 10; // dark elf
            case 5 -> 18; // dragon knight
            case 6 -> 20; // illusionist
            default -> throw new IllegalArgumentException("unsupported classic character type");
        };
    }

    /** Exact {@code 角色_精神成長.魔防} values used by the memory fixture. */
    static int wisdomMagicResistance(int wisdom) {
        int value = Math.max(0, Math.min(50, wisdom));
        if (value <= 14) return 0;
        return switch (value) {
            case 15, 16 -> 3;
            case 17 -> 6;
            case 18 -> 10;
            case 19 -> 15;
            case 20 -> 21;
            case 21 -> 28;
            case 22 -> 37;
            case 23 -> 47;
            case 24, 25, 26, 27, 28, 29 -> 50;
            case 30, 31, 32, 33, 34 -> 52;
            case 35, 36, 37, 38, 39 -> 55;
            case 40, 41, 42, 43 -> 59;
            case 44, 45, 46 -> 62;
            case 47, 48 -> 64;
            case 49, 50 -> 66;
            default -> throw new IllegalStateException("unreachable wisdom range");
        };
    }

    static int originalMrBonus(int characterType, int originalWisdom) {
        if (originalWisdom < 0) {
            throw new IllegalArgumentException("original wisdom must not be negative");
        }
        return switch (characterType) {
            case 0 -> originalWisdom >= 14 ? 2 : originalWisdom >= 12 ? 1 : 0;
            case 1 -> originalWisdom >= 12 ? 2 : originalWisdom >= 10 ? 1 : 0;
            case 2 -> originalWisdom >= 16 ? 2 : originalWisdom >= 13 ? 1 : 0;
            case 3 -> originalWisdom >= 15 ? 1 : 0;
            case 4 -> originalWisdom >= 16 ? 4 : originalWisdom == 15 ? 3
                    : originalWisdom == 14 ? 2 : originalWisdom >= 11 ? 1 : 0;
            case 5 -> originalWisdom >= 14 ? 2 : 0;
            case 6 -> originalWisdom == 18 ? 4
                    : originalWisdom >= 15 && originalWisdom <= 17 ? 2 : 0;
            default -> throw new IllegalArgumentException("unsupported classic character type");
        };
    }

    static int originalMagicHit(int characterType, int originalIntelligence) {
        if (originalIntelligence < 0) {
            throw new IllegalArgumentException("original intelligence must not be negative");
        }
        return switch (characterType) {
            case 0 -> originalIntelligence >= 14 ? 2
                    : originalIntelligence >= 12 ? 1 : 0;
            case 1 -> originalIntelligence == 12 ? 2
                    : originalIntelligence >= 10 && originalIntelligence <= 11 ? 1 : 0;
            case 2 -> originalIntelligence >= 15 ? 2
                    : originalIntelligence >= 13 ? 1 : 0;
            case 3 -> originalIntelligence > 80 ? 2
                    : originalIntelligence > 14 ? 1 : 0;
            case 4 -> originalIntelligence >= 14 ? 2
                    : originalIntelligence >= 12 ? 1 : 0;
            case 5 -> originalIntelligence >= 16 ? 4
                    : originalIntelligence >= 14 ? 3
                    : originalIntelligence >= 12 ? 2 : 0;
            case 6 -> originalIntelligence >= 13 ? 1 : 0;
            default -> throw new IllegalArgumentException("unsupported classic character type");
        };
    }

    static int originalMagicCritical(int characterType, int originalIntelligence) {
        if (originalIntelligence < 0) {
            throw new IllegalArgumentException("original intelligence must not be negative");
        }
        return switch (characterType) {
            case 0, 1, 4, 5, 6 -> 0;
            case 2 -> originalIntelligence >= 16 ? 4
                    : originalIntelligence >= 14 ? 2 : 0;
            case 3 -> switch (originalIntelligence) {
                case 15 -> 2;
                case 16 -> 4;
                case 17 -> 6;
                case 18 -> 8;
                default -> 0;
            };
            default -> throw new IllegalArgumentException("unsupported classic character type");
        };
    }

    static int originalMagicDamage(int characterType, int originalIntelligence) {
        if (originalIntelligence < 0) {
            throw new IllegalArgumentException("original intelligence must not be negative");
        }
        return switch (characterType) {
            case 0, 1, 2, 4 -> 0;
            case 3 -> originalIntelligence >= 13 ? 1 : 0;
            case 5 -> originalIntelligence == 17 ? 3
                    : originalIntelligence >= 15 && originalIntelligence <= 16 ? 2
                    : originalIntelligence >= 13 && originalIntelligence <= 14 ? 1 : 0;
            case 6 -> originalIntelligence == 17 ? 2
                    : originalIntelligence == 16 ? 1 : 0;
            default -> throw new IllegalArgumentException("unsupported classic character type");
        };
    }

    /**
     * skills.skill_level used by the calcMagicDiceDamage critical gate
     * (skill_level <= 6).  Every non-placeholder 380_db skills row satisfies
     * {@code ((skillId - 1) >> 3) + 1}.
     */
    static int skillLevel(int skillId) {
        if (skillId <= 0) throw new IllegalArgumentException("skill id must be positive");
        return ((skillId - 1) >> 3) + 1;
    }

    /** Exact post-elemental attacker additions in calcMagicDiceDamage. */
    static MagicAttackerResult attackerBonuses(
            int elementalDamage, int skillLevel, int criticalRoll,
            int characterType, int originalIntelligence,
            int equipmentMagicDamage) {
        if (elementalDamage < 0 || skillLevel <= 0
                || criticalRoll < 1 || criticalRoll > 100) {
            throw new IllegalArgumentException("invalid magic attacker inputs");
        }
        int criticalBonus = originalMagicCritical(characterType, originalIntelligence);
        boolean critical = skillLevel <= 6 && criticalRoll <= 10 + criticalBonus;
        int damage = critical ? (int) (elementalDamage * 1.5) : elementalDamage;
        int originalDamage = originalMagicDamage(characterType, originalIntelligence);
        damage += originalDamage;
        damage += equipmentMagicDamage;
        return new MagicAttackerResult(
                damage, critical, criticalBonus, originalDamage, equipmentMagicDamage);
    }

    /** Exact getDamageReductionByArmor branch for a preselected random offset. */
    static int armorDamageReduction(int baseReduction, int randomOffset) {
        if (baseReduction <= 10) return baseReduction;
        int bound = baseReduction - 10;
        if (randomOffset < 0 || randomOffset >= bound) {
            throw new IllegalArgumentException("armor reduction random offset outside bound");
        }
        return 11 + randomOffset;
    }

    record MagicAttackerResult(
            int damage, boolean critical, int originalCriticalBonus,
            int originalDamage, int equipmentDamage) {}

    static int damageAfterResistance(
            int sourceDamage, int targetMagicResistance, int attackerOriginalMagicHit) {
        if (sourceDamage < 0 || targetMagicResistance < 0 || attackerOriginalMagicHit < 0) {
            throw new IllegalArgumentException("magic damage, MR and original magic hit must be non-negative");
        }
        int floor;
        double coefficient;
        if (targetMagicResistance == 0) {
            floor = 1;
            coefficient = 1.0;
        } else if (targetMagicResistance <= 50) {
            floor = 2;
            coefficient = 1.0;
        } else if (targetMagicResistance <= 100) {
            floor = 3;
            coefficient = 0.9;
        } else if (targetMagicResistance <= 120) {
            floor = 4;
            coefficient = 0.9;
        } else if (targetMagicResistance <= 140) {
            floor = 5;
            coefficient = 0.8;
        } else if (targetMagicResistance <= 160) {
            floor = 6;
            coefficient = 0.8;
        } else if (targetMagicResistance <= 180) {
            floor = 7;
            coefficient = 0.7;
        } else if (targetMagicResistance <= 200) {
            floor = 8;
            coefficient = 0.7;
        } else if (targetMagicResistance <= 220) {
            floor = 9;
            coefficient = 0.6;
        } else if (targetMagicResistance <= 240) {
            floor = 10;
            coefficient = 0.6;
        } else {
            floor = 11;
            coefficient = 0.5;
        }
        double multiplier = coefficient - 0.01 * Math.floor(
                (targetMagicResistance - attackerOriginalMagicHit) / (double) floor);
        // Java's original compound int *= double assignment truncates toward zero.
        return (int) (sourceDamage * multiplier);
    }
}

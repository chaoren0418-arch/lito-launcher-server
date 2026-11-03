package local.lito381.server;

/**
 * Selected-source maximum-damage and multiplier-damage weapon rules.
 *
 * <p>The retained server uses {@code dmg_max_chance} while choosing the
 * weapon/projectile die and applies {@code dmg_cri_chance/dmg_cri} to the
 * offensive subtotal before target defense.  A claw is the native exception:
 * its untriggered weapon die is zero and its triggered die is the configured
 * maximum.</p>
 */
final class BattleWeaponCritical {
    private BattleWeaponCritical() {}

    record RollOutcome(
            int damage, int maximum, int chance, int chanceRoll,
            boolean triggered, boolean effectEnabled) {
        static RollOutcome none() {
            return new RollOutcome(0, 0, 0, 0, false, false);
        }
    }

    record DamageOutcome(
            int damage, int fixedBonus, int chance, int chanceRoll,
            double multiplier, boolean triggered, boolean effectEnabled) {
        static DamageOutcome none(int damage) {
            return new DamageOutcome(damage, 0, 0, 0, 1.0D, false, false);
        }
    }

    static RollOutcome roll(
            SourceWeaponPhysicalStats weapon, int maximum,
            int ordinaryRoll, int chanceRoll) {
        if (weapon == null || !weapon.present() || maximum < 1) {
            throw new IllegalArgumentException("invalid critical weapon die");
        }
        SourceWeaponCombatTraits traits = weapon.combatTraits();
        int chance = traits.maximumDamageChance();
        validateChanceRoll(chance, chanceRoll);
        boolean triggered = chance > 0 && chanceRoll <= chance;
        int damage;
        if (weapon.ranged()) {
            if (triggered) {
                requireZeroOrdinary(ordinaryRoll);
                damage = maximum;
            } else {
                requireOrdinary(ordinaryRoll, maximum);
                damage = ordinaryRoll;
            }
        } else if (weapon.weaponType() == 11) {
            requireZeroOrdinary(ordinaryRoll);
            damage = triggered ? maximum : 0;
        } else if (triggered) {
            requireZeroOrdinary(ordinaryRoll);
            damage = maximum;
        } else {
            int ordinaryMaximum = chance > 0 && maximum > 1
                    ? maximum - 1 : maximum;
            requireOrdinary(ordinaryRoll, ordinaryMaximum);
            damage = ordinaryRoll;
        }
        return new RollOutcome(
                damage, maximum, chance, chanceRoll, triggered,
                triggered && traits.maximumDamageEffect());
    }

    static DamageOutcome apply(
            SourceWeaponPhysicalStats weapon, int offensiveSubtotal,
            int chanceRoll) {
        return apply(weapon, offensiveSubtotal, chanceRoll, 1.0D);
    }

    /**
     * Retained offensive multiplier effects do not multiply one another.
     * Weapon double damage, Burning Spirit and the adjacent buff family
     * contribute candidates and only the greatest multiplier is applied.
     */
    static DamageOutcome apply(
            SourceWeaponPhysicalStats weapon, int offensiveSubtotal,
            int chanceRoll, double externalMultiplier) {
        if (weapon == null || offensiveSubtotal < 0) {
            throw new IllegalArgumentException("invalid critical damage subtotal");
        }
        if (!Double.isFinite(externalMultiplier) || externalMultiplier < 1.0D) {
            throw new IllegalArgumentException("invalid external damage multiplier");
        }
        if (!weapon.present()) {
            if (chanceRoll != 0) {
                throw new IllegalArgumentException("unarmed damage has a critical roll");
            }
            double result = offensiveSubtotal * externalMultiplier;
            if (!Double.isFinite(result) || result > Integer.MAX_VALUE) {
                throw new ArithmeticException("critical damage overflow");
            }
            return new DamageOutcome(
                    Math.max(0, (int) result), 0, 0, 0,
                    externalMultiplier, false, false);
        }
        SourceWeaponCombatTraits traits = weapon.combatTraits();
        int chance = traits.doubleDamageChance();
        validateChanceRoll(chance, chanceRoll);
        boolean triggered = chance > 0 && chanceRoll <= chance;
        int fixedBonus = weapon.weaponType() == 11 ? 5 : 0;
        int withFixedBonus = Math.addExact(offensiveSubtotal, fixedBonus);
        double multiplier = Math.max(externalMultiplier,
                triggered ? traits.doubleDamageMultiplier() : 1.0D);
        double result = withFixedBonus * multiplier;
        if (!Double.isFinite(result) || result > Integer.MAX_VALUE) {
            throw new ArithmeticException("critical weapon damage overflow");
        }
        int damage = Math.max(0, (int) result);
        return new DamageOutcome(
                damage, fixedBonus, chance, chanceRoll, multiplier, triggered,
                triggered && traits.doubleDamageEffect());
    }

    private static void validateChanceRoll(int chance, int chanceRoll) {
        if ((chance == 0 && chanceRoll != 0)
                || (chance > 0 && (chanceRoll < 1 || chanceRoll > 100))) {
            throw new IllegalArgumentException("invalid critical chance roll");
        }
    }

    private static void requireZeroOrdinary(int ordinaryRoll) {
        if (ordinaryRoll != 0) {
            throw new IllegalArgumentException("triggered critical has an ordinary roll");
        }
    }

    private static void requireOrdinary(int ordinaryRoll, int maximum) {
        if (ordinaryRoll < 1 || ordinaryRoll > maximum) {
            throw new IllegalArgumentException("invalid ordinary weapon roll");
        }
    }
}

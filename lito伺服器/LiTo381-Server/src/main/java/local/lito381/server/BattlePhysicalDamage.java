package local.lito381.server;

/** Qualified same-family base physical damage before target-side reduction. */
final class BattlePhysicalDamage {
    private BattlePhysicalDamage() {}

    static int weaponDieMaximum(
            SourceWeaponPhysicalStats weapon, boolean targetLarge) {
        if (weapon == null || !weapon.present() || weapon.ranged()) return 0;
        return targetLarge ? weapon.damageLarge() : weapon.damageSmall();
    }

    static int sourceDamage(
            SourceWeaponPhysicalStats weapon, boolean ranged, boolean targetLarge,
            int weaponRoll, int unarmedRoll,
            int statDamage, int equipmentDamage, int cookingDamage) {
        return sourceDamage(weapon, ranged, targetLarge,
                weaponRoll, 0, unarmedRoll,
                statDamage, equipmentDamage, cookingDamage);
    }

    static int sourceDamage(
            SourceWeaponPhysicalStats weapon, boolean ranged, boolean targetLarge,
            int weaponRoll, int projectileRoll, int unarmedRoll,
            int statDamage, int equipmentDamage, int cookingDamage) {
        if (weapon == null || cookingDamage < 0) {
            throw new IllegalArgumentException("invalid physical damage source");
        }
        int base;
        if (!weapon.present()) {
            if (unarmedRoll < 1 || unarmedRoll > 2
                    || weaponRoll != 0 || projectileRoll != 0) {
                throw new IllegalArgumentException("invalid unarmed damage roll");
            }
            base = unarmedRoll;
        } else {
            if (unarmedRoll != 0) {
                throw new IllegalArgumentException("armed damage cannot carry an unarmed roll");
            }
            int maximum = weaponDieMaximum(weapon, targetLarge);
            if (ranged) {
                if (!weapon.ranged() || weaponRoll != 0) {
                    throw new IllegalArgumentException("invalid ranged weapon damage roll");
                }
                SourceProjectilePhysicalStats projectile = weapon.projectile();
                if (!projectile.present()) {
                    if (projectileRoll != 0) {
                        throw new IllegalArgumentException("missing ranged projectile");
                    }
                    base = 0;
                } else {
                    int projectileMaximum = projectile.damageMaximum(targetLarge);
                    if (projectileRoll < 1 || projectileRoll > projectileMaximum) {
                        throw new IllegalArgumentException("invalid projectile damage roll");
                    }
                    base = projectileRoll;
                }
            } else {
                boolean clawRoll = weapon.weaponType() == 11;
                if (projectileRoll != 0 || weapon.ranged() || maximum < 1
                        || (clawRoll
                            ? weaponRoll < 0 || weaponRoll > maximum
                            : weaponRoll < 1 || weaponRoll > maximum)) {
                    throw new IllegalArgumentException("invalid melee weapon damage roll");
                }
                base = weaponRoll;
            }
        }
        int damage = Math.addExact(base, statDamage);
        damage = Math.addExact(damage, equipmentDamage);
        damage = Math.addExact(damage, cookingDamage);
        return Math.max(0, damage);
    }

    /**
     * T113 D9: 1-伺服器 {@code L1AttackPc.pcDmgMode} adds
     * {@code getClassFeature().getAttackLevel(level)} to every PC physical
     * hit (PvP and PvE) after {@code weaponDamage2} has applied the weapon
     * multiplier and before any target reduction.  Kiringku (type 17) keeps
     * its own magic-style arithmetic in {@link CombatKiringku#finish}, which
     * already adds the same term, so it is excluded here.
     */
    static int withClassAttackLevel(
            int multipliedDamage, boolean hit, boolean kiringku,
            int characterType, int level) {
        if (multipliedDamage < 0) {
            throw new IllegalArgumentException("negative multiplied physical damage");
        }
        if (!hit || kiringku) return multipliedDamage;
        return Math.addExact(multipliedDamage,
                BattleKiringkuDamage.classAttackLevel(characterType, level));
    }

    /** Exact retained unarmed expression: (random 4..8) >> 2. */
    static int unarmedDamage(int randomFourThroughEight) {
        if (randomFourThroughEight < 4 || randomFourThroughEight > 8) {
            throw new IllegalArgumentException("invalid unarmed source roll");
        }
        return randomFourThroughEight >> 2;
    }
}

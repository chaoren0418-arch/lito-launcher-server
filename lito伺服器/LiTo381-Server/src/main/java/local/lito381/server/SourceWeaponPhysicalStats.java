// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.SourceProjectilePhysicalStats;
import local.lito381.server.SourceWeaponCombatTraits;

record SourceWeaponPhysicalStats(int itemId, int weaponType, int damageSmall, int damageLarge, int range, int hitModifier, int damageModifier, int enchantLevel, int durability, int abilityMeleeHit, int abilityMeleeDamage, int abilityRangedHit, int abilityRangedDamage, SourceWeaponCombatTraits combatTraits, SourceProjectilePhysicalStats projectile, String nameId) {
    SourceWeaponPhysicalStats(int n, int n2, int n3, int n4, int n5, int n6, int n7, int n8, int n9, int n10, int n11, int n12, int n13, SourceWeaponCombatTraits sourceWeaponCombatTraits, SourceProjectilePhysicalStats sourceProjectilePhysicalStats) {
        this(n, n2, n3, n4, n5, n6, n7, n8, n9, n10, n11, n12, n13, sourceWeaponCombatTraits, sourceProjectilePhysicalStats, "");
    }

    SourceWeaponPhysicalStats(int n, int n2, int n3, int n4, int n5, int n6, int n7, int n8, int n9, int n10, int n11, int n12, int n13) {
        this(n, n2, n3, n4, n5, n6, n7, n8, n9, n10, n11, n12, n13, SourceWeaponCombatTraits.none(), SourceProjectilePhysicalStats.none(), "");
    }

    SourceWeaponPhysicalStats(int n, int n2, int n3, int n4, int n5, int n6, int n7, int n8, int n9) {
        this(n, n2, n3, n4, n5, n6, n7, n8, n9, 0, 0, 0, 0);
    }

    SourceWeaponPhysicalStats {
        String string2 = string = string == null ? "" : string;
        if (sourceWeaponCombatTraits == null || sourceProjectilePhysicalStats == null || n < 0 || n2 < 0 || n2 > 18 || n3 < 0 || n4 < 0 || n5 < -1 || n5 == -1 && n2 != 4 && n2 != 10 && n2 != 13 || n8 < -999999 || n8 > 999999 || n9 < 0 || n9 > 127) {
            throw new IllegalArgumentException("invalid source weapon physical snapshot");
        }
        if (n == 0 != (n2 == 0)) {
            throw new IllegalArgumentException("incomplete source weapon physical snapshot");
        }
        if (!(n != 0 || n3 == 0 && n4 == 0 && n5 == 0 && n6 == 0 && n7 == 0 && n8 == 0 && n9 == 0 && n10 == 0 && n11 == 0 && n12 == 0 && n13 == 0 && sourceWeaponCombatTraits.equals(SourceWeaponCombatTraits.none()) && !sourceProjectilePhysicalStats.present() && string.isEmpty())) {
            throw new IllegalArgumentException("empty source weapon carries physical values");
        }
    }

    static SourceWeaponPhysicalStats none() {
        return new SourceWeaponPhysicalStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, SourceWeaponCombatTraits.none(), SourceProjectilePhysicalStats.none(), "");
    }

    boolean present() {
        return this.itemId != 0;
    }

    boolean ranged() {
        return this.weaponType == 4 || this.weaponType == 10 || this.weaponType == 13;
    }

    boolean bow() {
        return this.weaponType == 4 || this.weaponType == 13;
    }

    int walkAction() {
        return switch (this.weaponType) {
            case 0 -> 0;
            case 1 -> 4;
            case 2 -> 46;
            case 3 -> 50;
            case 4, 13 -> 20;
            case 5, 14 -> 24;
            case 6, 15 -> 11;
            case 7, 16 -> 40;
            case 8, 10 -> 62;
            case 11 -> 58;
            case 12 -> 54;
            case 17 -> 58;
            case 18 -> 83;
            default -> throw new IllegalStateException("unsupported source weapon type");
        };
    }

    int effectiveEnchant(boolean bl) {
        return bl ? this.enchantLevel : this.enchantLevel - this.durability;
    }

    int attackMaterial() {
        return this.ranged() && this.projectile.present() ? this.projectile.material() : this.combatTraits.material();
    }

    int attackBlessing() {
        return this.ranged() && this.projectile.present() ? this.projectile.blessing() : this.combatTraits.blessing();
    }

    SourceWeaponPhysicalStats withCombatTraits(SourceWeaponCombatTraits sourceWeaponCombatTraits) {
        if (!this.present() || sourceWeaponCombatTraits == null) {
            throw new IllegalStateException("source weapon cannot accept combat traits");
        }
        return new SourceWeaponPhysicalStats(this.itemId, this.weaponType, this.damageSmall, this.damageLarge, this.range, this.hitModifier, this.damageModifier, this.enchantLevel, this.durability, this.abilityMeleeHit, this.abilityMeleeDamage, this.abilityRangedHit, this.abilityRangedDamage, sourceWeaponCombatTraits, this.projectile, this.nameId);
    }

    SourceWeaponPhysicalStats withAbility(int n, int n2, int n3, int n4) {
        if (!this.present()) {
            throw new IllegalStateException("empty source weapon has no ability");
        }
        return new SourceWeaponPhysicalStats(this.itemId, this.weaponType, this.damageSmall, this.damageLarge, this.range, this.hitModifier, this.damageModifier, this.enchantLevel, this.durability, n, n2, n3, n4, this.combatTraits, this.projectile, this.nameId);
    }

    SourceWeaponPhysicalStats withProjectile(SourceProjectilePhysicalStats sourceProjectilePhysicalStats) {
        int n;
        if (!(this.present() && this.ranged() && sourceProjectilePhysicalStats != null && sourceProjectilePhysicalStats.present())) {
            throw new IllegalStateException("source weapon cannot accept projectile");
        }
        int n2 = n = this.weaponType == 10 ? -3 : -2;
        if (sourceProjectilePhysicalStats.useType() != n) {
            throw new IllegalArgumentException("source projectile does not match weapon type");
        }
        return new SourceWeaponPhysicalStats(this.itemId, this.weaponType, this.damageSmall, this.damageLarge, this.range, this.hitModifier, this.damageModifier, this.enchantLevel, this.durability, this.abilityMeleeHit, this.abilityMeleeDamage, this.abilityRangedHit, this.abilityRangedDamage, this.combatTraits, sourceProjectilePhysicalStats, this.nameId);
    }

    SourceWeaponPhysicalStats withoutProjectile() {
        if (!this.present()) {
            throw new IllegalStateException("empty source weapon has no projectile state");
        }
        return new SourceWeaponPhysicalStats(this.itemId, this.weaponType, this.damageSmall, this.damageLarge, this.range, this.hitModifier, this.damageModifier, this.enchantLevel, this.durability, this.abilityMeleeHit, this.abilityMeleeDamage, this.abilityRangedHit, this.abilityRangedDamage, this.combatTraits, SourceProjectilePhysicalStats.none(), this.nameId);
    }

    SourceWeaponPhysicalStats withNameId(String string) {
        if (!this.present() || string == null || string.isBlank()) {
            throw new IllegalArgumentException("source weapon requires a native name id");
        }
        return new SourceWeaponPhysicalStats(this.itemId, this.weaponType, this.damageSmall, this.damageLarge, this.range, this.hitModifier, this.damageModifier, this.enchantLevel, this.durability, this.abilityMeleeHit, this.abilityMeleeDamage, this.abilityRangedHit, this.abilityRangedDamage, this.combatTraits, this.projectile, string);
    }

    SourceWeaponPhysicalStats withEnchantLevel(int n) {
        return new SourceWeaponPhysicalStats(this.itemId, this.weaponType, this.damageSmall, this.damageLarge, this.range, this.hitModifier, this.damageModifier, n, this.durability, this.abilityMeleeHit, this.abilityMeleeDamage, this.abilityRangedHit, this.abilityRangedDamage, this.combatTraits, this.projectile, this.nameId);
    }

    SourceWeaponPhysicalStats withDurability(int n) {
        if (!this.present() || n < 0 || n > 127) {
            throw new IllegalArgumentException("invalid source weapon durability");
        }
        return new SourceWeaponPhysicalStats(this.itemId, this.weaponType, this.damageSmall, this.damageLarge, this.range, this.hitModifier, this.damageModifier, this.enchantLevel, n, this.abilityMeleeHit, this.abilityMeleeDamage, this.abilityRangedHit, this.abilityRangedDamage, this.combatTraits, this.projectile, this.nameId);
    }
}

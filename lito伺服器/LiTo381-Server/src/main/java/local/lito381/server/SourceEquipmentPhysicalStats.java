// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NumericBreakthrough;
import local.lito381.server.RentalEquipmentBonuses;
import local.lito381.server.SolidCarriageRules;
import local.lito381.server.SourceDefensiveEquipment;
import local.lito381.server.SourceEquipmentElementalStats;
import local.lito381.server.SourceWeaponPhysicalStats;

record SourceEquipmentPhysicalStats(SourceWeaponPhysicalStats weapon, int itemMeleeHit, int itemMeleeDamage, int itemRangedHit, int itemRangedDamage, int armorMeleeHit, int armorMeleeDamage, int armorRangedHit, int armorRangedDamage, int setMeleeHit, int setMeleeDamage, int setRangedHit, int setRangedDamage, int itemDexterity, int setDexterity, int itemPierce, int setPierce, int itemIgnoreReduction, int setIgnoreReduction, int itemMeleeReduction, int itemRangedReduction, int itemArmorClassModifier, int setArmorClassValue, int itemEvasionRate, int setEvasionRate, SourceEquipmentElementalStats elemental, boolean shieldEquipped, boolean guarderEquipped, SourceDefensiveEquipment defensiveEquipment, int pvpReduction) {
    SourceEquipmentPhysicalStats(SourceWeaponPhysicalStats sourceWeaponPhysicalStats, int n, int n2, int n3, int n4, int n5, int n6, int n7, int n8, int n9, int n10, int n11, int n12, int n13, int n14, int n15, int n16, int n17, int n18, int n19, int n20, int n21, int n22, int n23, int n24, SourceEquipmentElementalStats sourceEquipmentElementalStats, boolean bl, boolean bl2, SourceDefensiveEquipment sourceDefensiveEquipment) {
        this(sourceWeaponPhysicalStats, n, n2, n3, n4, n5, n6, n7, n8, n9, n10, n11, n12, n13, n14, n15, n16, n17, n18, n19, n20, n21, n22, n23, n24, sourceEquipmentElementalStats, bl, bl2, sourceDefensiveEquipment, 0);
    }

    SourceEquipmentPhysicalStats(SourceWeaponPhysicalStats sourceWeaponPhysicalStats, int n, int n2, int n3, int n4, int n5, int n6, int n7, int n8, int n9, int n10, int n11, int n12, int n13, int n14, int n15, int n16, int n17, int n18, int n19, int n20, int n21, int n22, int n23, int n24, SourceEquipmentElementalStats sourceEquipmentElementalStats) {
        this(sourceWeaponPhysicalStats, n, n2, n3, n4, n5, n6, n7, n8, n9, n10, n11, n12, n13, n14, n15, n16, n17, n18, n19, n20, n21, n22, n23, n24, sourceEquipmentElementalStats, false, false, SourceDefensiveEquipment.none());
    }

    SourceEquipmentPhysicalStats(SourceWeaponPhysicalStats sourceWeaponPhysicalStats, boolean bl, boolean bl2) {
        this(sourceWeaponPhysicalStats, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, SourceEquipmentElementalStats.none(), bl, bl2, bl ? SourceDefensiveEquipment.memoryShield() : (bl2 ? SourceDefensiveEquipment.memoryGuarder() : SourceDefensiveEquipment.none()));
    }

    SourceEquipmentPhysicalStats(SourceWeaponPhysicalStats sourceWeaponPhysicalStats, int n, int n2, int n3, int n4, int n5, int n6, int n7, int n8, int n9, int n10, int n11, int n12) {
        this(sourceWeaponPhysicalStats, n, n2, n3, n4, n5, n6, n7, n8, n9, n10, n11, n12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, SourceEquipmentElementalStats.none());
    }

    SourceEquipmentPhysicalStats(SourceWeaponPhysicalStats sourceWeaponPhysicalStats, int n, int n2, int n3, int n4, int n5, int n6, int n7, int n8, int n9, int n10, int n11, int n12, int n13, int n14, int n15, int n16) {
        this(sourceWeaponPhysicalStats, n, n2, n3, n4, n5, n6, n7, n8, n9, n10, n11, n12, n13, n14, n15, n16, 0, 0, 0, 0, 0, 0, 0, 0, SourceEquipmentElementalStats.none());
    }

    SourceEquipmentPhysicalStats(SourceWeaponPhysicalStats sourceWeaponPhysicalStats, int n, int n2, int n3, int n4, int n5, int n6, int n7, int n8, int n9, int n10, int n11, int n12, int n13, int n14, int n15, int n16, int n17, int n18, int n19, int n20) {
        this(sourceWeaponPhysicalStats, n, n2, n3, n4, n5, n6, n7, n8, n9, n10, n11, n12, n13, n14, n15, n16, n17, n18, n19, n20, 0, 0, 0, 0, SourceEquipmentElementalStats.none());
    }

    SourceEquipmentPhysicalStats(SourceWeaponPhysicalStats sourceWeaponPhysicalStats, int n, int n2, int n3, int n4, int n5, int n6, int n7, int n8, int n9, int n10, int n11, int n12, int n13, int n14, int n15, int n16, int n17, int n18, int n19, int n20, int n21, int n22) {
        this(sourceWeaponPhysicalStats, n, n2, n3, n4, n5, n6, n7, n8, n9, n10, n11, n12, n13, n14, n15, n16, n17, n18, n19, n20, n21, n22, 0, 0, SourceEquipmentElementalStats.none());
    }

    SourceEquipmentPhysicalStats(SourceWeaponPhysicalStats sourceWeaponPhysicalStats, int n, int n2, int n3, int n4, int n5, int n6, int n7, int n8, int n9, int n10, int n11, int n12, int n13, int n14, int n15, int n16, int n17, int n18, int n19, int n20, int n21, int n22, SourceEquipmentElementalStats sourceEquipmentElementalStats) {
        this(sourceWeaponPhysicalStats, n, n2, n3, n4, n5, n6, n7, n8, n9, n10, n11, n12, n13, n14, n15, n16, n17, n18, n19, n20, n21, n22, 0, 0, sourceEquipmentElementalStats);
    }

    SourceEquipmentPhysicalStats {
        if (sourceWeaponPhysicalStats == null) {
            throw new IllegalArgumentException("source physical snapshot has no weapon state");
        }
        if (sourceEquipmentElementalStats == null) {
            throw new IllegalArgumentException("source physical snapshot has no elemental state");
        }
        if (sourceDefensiveEquipment == null || bl && bl2 || bl != (sourceDefensiveEquipment.present() && sourceDefensiveEquipment.armorType() == 7) || bl2 != (sourceDefensiveEquipment.present() && sourceDefensiveEquipment.armorType() == 13)) {
            throw new IllegalArgumentException("source defensive equipment identity does not match slot state");
        }
        if (n25 < 0) {
            throw new IllegalArgumentException("negative source PvP reduction");
        }
    }

    static SourceEquipmentPhysicalStats none() {
        return new SourceEquipmentPhysicalStats(SourceWeaponPhysicalStats.none(), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    int totalHit(boolean bl) {
        return this.totalHit(bl, true);
    }

    int totalHit(boolean bl, boolean bl2) {
        int n;
        int n2 = n = bl ? this.itemRangedHit + this.armorRangedHit + this.setRangedHit : this.itemMeleeHit + this.armorMeleeHit + this.setMeleeHit;
        if (!bl2 || !this.weapon.present()) {
            return n;
        }
        n = Math.addExact(n, bl ? this.weapon.abilityRangedHit() : this.weapon.abilityMeleeHit());
        return Math.addExact(n, Math.addExact(this.weapon.hitModifier(), (int)((double)this.weapon.enchantLevel() * 0.6)));
    }

    int totalDamageBonus(boolean bl) {
        return this.totalDamageBonus(bl, true);
    }

    int totalDamageBonus(boolean bl, boolean bl2) {
        int n;
        int n2 = n = bl ? this.itemRangedDamage + this.armorRangedDamage + this.setRangedDamage : this.itemMeleeDamage + this.armorMeleeDamage + this.setMeleeDamage;
        if (!bl2 || !this.weapon.present()) {
            return n;
        }
        n = Math.addExact(n, bl ? this.weapon.abilityRangedDamage() : this.weapon.abilityMeleeDamage());
        return Math.addExact(n, Math.addExact(this.weapon.damageModifier(), this.weapon.effectiveEnchant(bl)));
    }

    int effectiveDexterity(int n) {
        return NumericBreakthrough.clampAbility((long)n + (long)this.itemDexterity + (long)this.setDexterity);
    }

    int totalPierce() {
        return Math.addExact(this.itemPierce, this.setPierce);
    }

    int totalIgnoreReduction() {
        return Math.addExact(this.itemIgnoreReduction, this.setIgnoreReduction);
    }

    int directionalReduction(boolean bl) {
        return bl ? this.itemRangedReduction : this.itemMeleeReduction;
    }

    int totalPvpReduction() {
        return this.pvpReduction;
    }

    int totalEvasionRate() {
        return Math.addExact(this.itemEvasionRate, this.setEvasionRate);
    }

    boolean supportsSolidCarriage() {
        return SolidCarriageRules.equipment(this.shieldEquipped, this.guarderEquipped);
    }

    int armorClassModifier() {
        return NumericBreakthrough.clampExtendedValue((long)this.itemArmorClassModifier - (long)this.setArmorClassValue);
    }

    SourceEquipmentPhysicalStats withRentalEquipment(RentalEquipmentBonuses rentalEquipmentBonuses) {
        if (rentalEquipmentBonuses == null || rentalEquipmentBonuses.matchedItems() == 0) {
            return this;
        }
        SourceEquipmentElementalStats sourceEquipmentElementalStats = new SourceEquipmentElementalStats(Math.addExact(this.elemental.itemEarth(), rentalEquipmentBonuses.earthResistance()), Math.addExact(this.elemental.itemFire(), rentalEquipmentBonuses.fireResistance()), Math.addExact(this.elemental.itemWater(), rentalEquipmentBonuses.waterResistance()), Math.addExact(this.elemental.itemWind(), rentalEquipmentBonuses.windResistance()), this.elemental.setEarth(), this.elemental.setFire(), this.elemental.setWater(), this.elemental.setWind());
        return new SourceEquipmentPhysicalStats(this.weapon, this.itemMeleeHit, this.itemMeleeDamage, this.itemRangedHit, this.itemRangedDamage, Math.addExact(this.armorMeleeHit, rentalEquipmentBonuses.meleeHit()), Math.addExact(this.armorMeleeDamage, rentalEquipmentBonuses.meleeDamage()), Math.addExact(this.armorRangedHit, rentalEquipmentBonuses.rangedHit()), Math.addExact(this.armorRangedDamage, rentalEquipmentBonuses.rangedDamage()), this.setMeleeHit, this.setMeleeDamage, this.setRangedHit, this.setRangedDamage, Math.addExact(this.itemDexterity, rentalEquipmentBonuses.dexterity()), this.setDexterity, this.itemPierce, this.setPierce, this.itemIgnoreReduction, this.setIgnoreReduction, Math.addExact(this.itemMeleeReduction, rentalEquipmentBonuses.meleeReduction()), Math.addExact(this.itemRangedReduction, rentalEquipmentBonuses.rangedReduction()), this.itemArmorClassModifier, this.setArmorClassValue, this.itemEvasionRate, this.setEvasionRate, sourceEquipmentElementalStats, this.shieldEquipped, this.guarderEquipped, this.defensiveEquipment, this.pvpReduction);
    }
}

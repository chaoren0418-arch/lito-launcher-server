// ORIGINAL SOURCE extracted from Accounts.java; aligned to base-0.6.71.jar.
package local.lito381.server;
import java.util.Set;

record SourceEquipmentPhysicalStats(
        SourceWeaponPhysicalStats weapon,
        int itemMeleeHit, int itemMeleeDamage,
        int itemRangedHit, int itemRangedDamage,
        int armorMeleeHit, int armorMeleeDamage,
        int armorRangedHit, int armorRangedDamage,
        int setMeleeHit, int setMeleeDamage,
        int setRangedHit, int setRangedDamage,
        int itemDexterity, int setDexterity,
        int itemPierce, int setPierce,
        int itemIgnoreReduction, int setIgnoreReduction,
        int itemMeleeReduction, int itemRangedReduction,
        int itemArmorClassModifier, int setArmorClassValue,
        int itemEvasionRate, int setEvasionRate,
        SourceEquipmentElementalStats elemental,
        boolean shieldEquipped, boolean guarderEquipped,
        SourceDefensiveEquipment defensiveEquipment,
        int pvpReduction) {
    /** Compatibility constructor for snapshots before PvP reduction was retained. */
    SourceEquipmentPhysicalStats(
            SourceWeaponPhysicalStats weapon,
            int itemMeleeHit, int itemMeleeDamage,
            int itemRangedHit, int itemRangedDamage,
            int armorMeleeHit, int armorMeleeDamage,
            int armorRangedHit, int armorRangedDamage,
            int setMeleeHit, int setMeleeDamage,
            int setRangedHit, int setRangedDamage,
            int itemDexterity, int setDexterity,
            int itemPierce, int setPierce,
            int itemIgnoreReduction, int setIgnoreReduction,
            int itemMeleeReduction, int itemRangedReduction,
            int itemArmorClassModifier, int setArmorClassValue,
            int itemEvasionRate, int setEvasionRate,
            SourceEquipmentElementalStats elemental,
            boolean shieldEquipped, boolean guarderEquipped,
            SourceDefensiveEquipment defensiveEquipment) {
        this(weapon, itemMeleeHit, itemMeleeDamage,
                itemRangedHit, itemRangedDamage,
                armorMeleeHit, armorMeleeDamage,
                armorRangedHit, armorRangedDamage,
                setMeleeHit, setMeleeDamage,
                setRangedHit, setRangedDamage,
                itemDexterity, setDexterity, itemPierce, setPierce,
                itemIgnoreReduction, setIgnoreReduction,
                itemMeleeReduction, itemRangedReduction,
                itemArmorClassModifier, setArmorClassValue,
                itemEvasionRate, setEvasionRate, elemental,
                shieldEquipped, guarderEquipped, defensiveEquipment, 0);
    }

    /** Compatibility constructor for snapshots before defensive-slot identity was retained. */
    SourceEquipmentPhysicalStats(
            SourceWeaponPhysicalStats weapon,
            int itemMeleeHit, int itemMeleeDamage,
            int itemRangedHit, int itemRangedDamage,
            int armorMeleeHit, int armorMeleeDamage,
            int armorRangedHit, int armorRangedDamage,
            int setMeleeHit, int setMeleeDamage,
            int setRangedHit, int setRangedDamage,
            int itemDexterity, int setDexterity,
            int itemPierce, int setPierce,
            int itemIgnoreReduction, int setIgnoreReduction,
            int itemMeleeReduction, int itemRangedReduction,
            int itemArmorClassModifier, int setArmorClassValue,
            int itemEvasionRate, int setEvasionRate,
            SourceEquipmentElementalStats elemental) {
        this(weapon, itemMeleeHit, itemMeleeDamage,
                itemRangedHit, itemRangedDamage,
                armorMeleeHit, armorMeleeDamage,
                armorRangedHit, armorRangedDamage,
                setMeleeHit, setMeleeDamage,
                setRangedHit, setRangedDamage,
                itemDexterity, setDexterity, itemPierce, setPierce,
                itemIgnoreReduction, setIgnoreReduction,
                itemMeleeReduction, itemRangedReduction,
                itemArmorClassModifier, setArmorClassValue,
                itemEvasionRate, setEvasionRate, elemental, false, false,
                SourceDefensiveEquipment.none());
    }

    SourceEquipmentPhysicalStats(
            SourceWeaponPhysicalStats weapon,
            boolean shieldEquipped, boolean guarderEquipped) {
        this(weapon,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                SourceEquipmentElementalStats.none(),
                shieldEquipped, guarderEquipped,
                shieldEquipped ? SourceDefensiveEquipment.memoryShield()
                        : guarderEquipped
                                ? SourceDefensiveEquipment.memoryGuarder()
                                : SourceDefensiveEquipment.none());
    }
    SourceEquipmentPhysicalStats(
            SourceWeaponPhysicalStats weapon,
            int itemMeleeHit, int itemMeleeDamage,
            int itemRangedHit, int itemRangedDamage,
            int armorMeleeHit, int armorMeleeDamage,
            int armorRangedHit, int armorRangedDamage,
            int setMeleeHit, int setMeleeDamage,
            int setRangedHit, int setRangedDamage) {
        this(weapon, itemMeleeHit, itemMeleeDamage,
                itemRangedHit, itemRangedDamage,
                armorMeleeHit, armorMeleeDamage,
                armorRangedHit, armorRangedDamage,
                setMeleeHit, setMeleeDamage,
                setRangedHit, setRangedDamage, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                SourceEquipmentElementalStats.none());
    }

    /** Compatibility constructor for snapshots before retained PvP defense was imported. */
    SourceEquipmentPhysicalStats(
            SourceWeaponPhysicalStats weapon,
            int itemMeleeHit, int itemMeleeDamage,
            int itemRangedHit, int itemRangedDamage,
            int armorMeleeHit, int armorMeleeDamage,
            int armorRangedHit, int armorRangedDamage,
            int setMeleeHit, int setMeleeDamage,
            int setRangedHit, int setRangedDamage,
            int itemDexterity, int setDexterity,
            int itemPierce, int setPierce) {
        this(weapon, itemMeleeHit, itemMeleeDamage,
                itemRangedHit, itemRangedDamage,
                armorMeleeHit, armorMeleeDamage,
                armorRangedHit, armorRangedDamage,
                setMeleeHit, setMeleeDamage,
                setRangedHit, setRangedDamage,
                itemDexterity, setDexterity, itemPierce, setPierce,
                0, 0, 0, 0, 0, 0, 0, 0,
                SourceEquipmentElementalStats.none());
    }

    /** Compatibility constructor for snapshots before AC reconstruction was imported. */
    SourceEquipmentPhysicalStats(
            SourceWeaponPhysicalStats weapon,
            int itemMeleeHit, int itemMeleeDamage,
            int itemRangedHit, int itemRangedDamage,
            int armorMeleeHit, int armorMeleeDamage,
            int armorRangedHit, int armorRangedDamage,
            int setMeleeHit, int setMeleeDamage,
            int setRangedHit, int setRangedDamage,
            int itemDexterity, int setDexterity,
            int itemPierce, int setPierce,
            int itemIgnoreReduction, int setIgnoreReduction,
            int itemMeleeReduction, int itemRangedReduction) {
        this(weapon, itemMeleeHit, itemMeleeDamage,
                itemRangedHit, itemRangedDamage,
                armorMeleeHit, armorMeleeDamage,
                armorRangedHit, armorRangedDamage,
                setMeleeHit, setMeleeDamage,
                setRangedHit, setRangedDamage,
                itemDexterity, setDexterity, itemPierce, setPierce,
                itemIgnoreReduction, setIgnoreReduction,
                itemMeleeReduction, itemRangedReduction, 0, 0, 0, 0,
                SourceEquipmentElementalStats.none());
    }

    /** Compatibility constructor for snapshots before four-element defense. */
    SourceEquipmentPhysicalStats(
            SourceWeaponPhysicalStats weapon,
            int itemMeleeHit, int itemMeleeDamage,
            int itemRangedHit, int itemRangedDamage,
            int armorMeleeHit, int armorMeleeDamage,
            int armorRangedHit, int armorRangedDamage,
            int setMeleeHit, int setMeleeDamage,
            int setRangedHit, int setRangedDamage,
            int itemDexterity, int setDexterity,
            int itemPierce, int setPierce,
            int itemIgnoreReduction, int setIgnoreReduction,
            int itemMeleeReduction, int itemRangedReduction,
            int itemArmorClassModifier, int setArmorClassValue) {
        this(weapon, itemMeleeHit, itemMeleeDamage,
                itemRangedHit, itemRangedDamage,
                armorMeleeHit, armorMeleeDamage,
                armorRangedHit, armorRangedDamage,
                setMeleeHit, setMeleeDamage,
                setRangedHit, setRangedDamage,
                itemDexterity, setDexterity, itemPierce, setPierce,
                itemIgnoreReduction, setIgnoreReduction,
                itemMeleeReduction, itemRangedReduction,
                itemArmorClassModifier, setArmorClassValue, 0, 0,
                SourceEquipmentElementalStats.none());
    }

    /** Compatibility constructor for snapshots before equipment ER was qualified. */
    SourceEquipmentPhysicalStats(
            SourceWeaponPhysicalStats weapon,
            int itemMeleeHit, int itemMeleeDamage,
            int itemRangedHit, int itemRangedDamage,
            int armorMeleeHit, int armorMeleeDamage,
            int armorRangedHit, int armorRangedDamage,
            int setMeleeHit, int setMeleeDamage,
            int setRangedHit, int setRangedDamage,
            int itemDexterity, int setDexterity,
            int itemPierce, int setPierce,
            int itemIgnoreReduction, int setIgnoreReduction,
            int itemMeleeReduction, int itemRangedReduction,
            int itemArmorClassModifier, int setArmorClassValue,
            SourceEquipmentElementalStats elemental) {
        this(weapon, itemMeleeHit, itemMeleeDamage,
                itemRangedHit, itemRangedDamage,
                armorMeleeHit, armorMeleeDamage,
                armorRangedHit, armorRangedDamage,
                setMeleeHit, setMeleeDamage,
                setRangedHit, setRangedDamage,
                itemDexterity, setDexterity, itemPierce, setPierce,
                itemIgnoreReduction, setIgnoreReduction,
                itemMeleeReduction, itemRangedReduction,
                itemArmorClassModifier, setArmorClassValue, 0, 0, elemental);
    }

    SourceEquipmentPhysicalStats {
        if (weapon == null) {
            throw new IllegalArgumentException("source physical snapshot has no weapon state");
        }
        if (elemental == null) {
            throw new IllegalArgumentException("source physical snapshot has no elemental state");
        }
        if (defensiveEquipment == null || shieldEquipped && guarderEquipped
                || shieldEquipped != (defensiveEquipment.present()
                        && defensiveEquipment.armorType() == 7)
                || guarderEquipped != (defensiveEquipment.present()
                        && defensiveEquipment.armorType() == 13)) {
            throw new IllegalArgumentException(
                    "source defensive equipment identity does not match slot state");
        }
        if (pvpReduction < 0) {
            throw new IllegalArgumentException("negative source PvP reduction");
        }
    }

    static SourceEquipmentPhysicalStats none() {
        return new SourceEquipmentPhysicalStats(
                SourceWeaponPhysicalStats.none(),
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    int totalHit(boolean rangedAttack) {
        return totalHit(rangedAttack, true);
    }

    int totalHit(boolean rangedAttack, boolean sourceWeaponActive) {
        int equipment = rangedAttack
                ? itemRangedHit + armorRangedHit + setRangedHit
                : itemMeleeHit + armorMeleeHit + setMeleeHit;
        if (!sourceWeaponActive || !weapon.present()) return equipment;
        equipment = Math.addExact(equipment,
                rangedAttack ? weapon.abilityRangedHit() : weapon.abilityMeleeHit());
        return Math.addExact(equipment,
                Math.addExact(weapon.hitModifier(),
                        (int) (weapon.enchantLevel() * 0.6)));
    }

    int totalDamageBonus(boolean rangedAttack) {
        return totalDamageBonus(rangedAttack, true);
    }

    int totalDamageBonus(boolean rangedAttack, boolean sourceWeaponActive) {
        int equipment = rangedAttack
                ? itemRangedDamage + armorRangedDamage + setRangedDamage
                : itemMeleeDamage + armorMeleeDamage + setMeleeDamage;
        if (!sourceWeaponActive || !weapon.present()) return equipment;
        equipment = Math.addExact(equipment,
                rangedAttack ? weapon.abilityRangedDamage() : weapon.abilityMeleeDamage());
        return Math.addExact(equipment,
                Math.addExact(weapon.damageModifier(),
                        weapon.effectiveEnchant(rangedAttack)));
    }

    int effectiveDexterity(int baseDexterity) {
        return NumericBreakthrough.clampAbility(
                (long) baseDexterity + itemDexterity + setDexterity);
    }

    int totalPierce() {
        return Math.addExact(itemPierce, setPierce);
    }

    int totalIgnoreReduction() {
        return Math.addExact(itemIgnoreReduction, setIgnoreReduction);
    }

    int directionalReduction(boolean rangedAttack) {
        return rangedAttack ? itemRangedReduction : itemMeleeReduction;
    }

    int totalPvpReduction() {
        return pvpReduction;
    }

    int totalEvasionRate() {
        return Math.addExact(itemEvasionRate, setEvasionRate);
    }

    /** Selected retained configuration accepts either armor type 7 or 13. */
    boolean supportsSolidCarriage() {
        return SolidCarriageRules.equipment(shieldEquipped, guarderEquipped);
    }

    /** Mirrors L1PcInstance.getAc: direct item AC, then subtract set AC. */
    int armorClassModifier() {
        return NumericBreakthrough.clampExtendedValue(
                (long) itemArmorClassModifier - setArmorClassValue);
    }

    /** Adds only LiTo-owned selected-template effects; source rows stay untouched. */
    SourceEquipmentPhysicalStats withRentalEquipment(RentalEquipmentBonuses bonus) {
        if (bonus == null || bonus.matchedItems() == 0) return this;
        SourceEquipmentElementalStats withElemental =
                new SourceEquipmentElementalStats(
                        Math.addExact(elemental.itemEarth(), bonus.earthResistance()),
                        Math.addExact(elemental.itemFire(), bonus.fireResistance()),
                        Math.addExact(elemental.itemWater(), bonus.waterResistance()),
                        Math.addExact(elemental.itemWind(), bonus.windResistance()),
                        elemental.setEarth(), elemental.setFire(),
                        elemental.setWater(), elemental.setWind());
        return new SourceEquipmentPhysicalStats(
                weapon,
                itemMeleeHit, itemMeleeDamage, itemRangedHit, itemRangedDamage,
                Math.addExact(armorMeleeHit, bonus.meleeHit()),
                Math.addExact(armorMeleeDamage, bonus.meleeDamage()),
                Math.addExact(armorRangedHit, bonus.rangedHit()),
                Math.addExact(armorRangedDamage, bonus.rangedDamage()),
                setMeleeHit, setMeleeDamage, setRangedHit, setRangedDamage,
                Math.addExact(itemDexterity, bonus.dexterity()), setDexterity,
                itemPierce, setPierce, itemIgnoreReduction, setIgnoreReduction,
                Math.addExact(itemMeleeReduction, bonus.meleeReduction()),
                Math.addExact(itemRangedReduction, bonus.rangedReduction()),
                itemArmorClassModifier, setArmorClassValue,
                itemEvasionRate, setEvasionRate, withElemental,
                shieldEquipped, guarderEquipped, defensiveEquipment, pvpReduction);
    }
}

/**
 * Persisted, source-qualified magic-relevant equipment snapshot.
 *
 * <p>Item fields are values that the retained {@code L1EquipmentSlot} applies
 * directly while each equipped row is restored. Set fields are the later
 * {@code _setAbility} aggregate, including active cross-item card combos. Set
 * Set WIS is retained for the independent natural-MP-regeneration path.  It
 * remains deliberately excluded from base MR: the retained source does not
 * re-run {@code resetBaseMr()} after rebuilding the set aggregate.  Set SP is
 * absent because {@code getTargetSp()} does not read it.</p>
 */

// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NumericBreakthrough;
import local.lito381.server.RentalEquipmentBonuses;
import local.lito381.server.SourceEquipmentPhysicalStats;

record SourceEquipmentMagicStats(int itemIntelligence, int itemWisdom, int itemConstitution, int itemSpellPower, int itemMagicResistance, int itemMagicDamage, int itemFireResistance, int armorTemplateDamageReduction, int setIntelligence, int setWisdom, int setConstitution, int setMagicResistance, int setMagicDamage, int setFireResistance, int setDamageReduction, int itemHpRegen, int setHpRegen, int itemMpRegen, int setMpRegen, int matchedEquipmentRows, int matchedArmorRows, int matchedVipRows, int matchedSingleCardRows, int activeSetBonuses, int activeCardCombos, int itemStrength, int itemWeightReduction, int armorTemplateWeightReduction, int setStrength, int setWeightReduction, int itemMagicHit, int setMagicHit, SourceEquipmentPhysicalStats physical, boolean healingHelmEquipped, int itemBlindResistance, int setBlindResistance, int itemSleepResistance, int setSleepResistance, int itemFreezeResistance, int setFreezeResistance, int itemStoneResistance, int setStoneResistance, int itemSustainResistance, int setSustainResistance, boolean hasteCostHalvingHelmEquipped, boolean greaterHasteCostHalvingHelmEquipped, int itemStunResistance, int setStunResistance, int itemStunSuccess, int setStunSuccess) {
    SourceEquipmentMagicStats(int itemIntelligence, int itemWisdom, int itemConstitution, int itemSpellPower, int itemMagicResistance, int itemMagicDamage, int itemFireResistance, int armorTemplateDamageReduction, int setIntelligence, int setWisdom, int setConstitution, int setMagicResistance, int setMagicDamage, int setFireResistance, int setDamageReduction, int itemHpRegen, int setHpRegen, int itemMpRegen, int setMpRegen, int matchedEquipmentRows, int matchedArmorRows, int matchedVipRows, int matchedSingleCardRows, int activeSetBonuses, int activeCardCombos, int itemStrength, int itemWeightReduction, int armorTemplateWeightReduction, int setStrength, int setWeightReduction, int itemMagicHit, int setMagicHit, SourceEquipmentPhysicalStats physical, boolean healingHelmEquipped, int itemBlindResistance, int setBlindResistance, int itemSleepResistance, int setSleepResistance, int itemFreezeResistance, int setFreezeResistance, int itemStoneResistance, int setStoneResistance, int itemSustainResistance, int setSustainResistance, boolean hasteCostHalvingHelmEquipped, boolean greaterHasteCostHalvingHelmEquipped) {
        this(itemIntelligence, itemWisdom, itemConstitution, itemSpellPower, itemMagicResistance, itemMagicDamage, itemFireResistance, armorTemplateDamageReduction, setIntelligence, setWisdom, setConstitution, setMagicResistance, setMagicDamage, setFireResistance, setDamageReduction, itemHpRegen, setHpRegen, itemMpRegen, setMpRegen, matchedEquipmentRows, matchedArmorRows, matchedVipRows, matchedSingleCardRows, activeSetBonuses, activeCardCombos, itemStrength, itemWeightReduction, armorTemplateWeightReduction, setStrength, setWeightReduction, itemMagicHit, setMagicHit, physical, healingHelmEquipped, itemBlindResistance, setBlindResistance, itemSleepResistance, setSleepResistance, itemFreezeResistance, setFreezeResistance, itemStoneResistance, setStoneResistance, itemSustainResistance, setSustainResistance, hasteCostHalvingHelmEquipped, greaterHasteCostHalvingHelmEquipped, 0, 0, 0, 0);
    }

    SourceEquipmentMagicStats(int itemIntelligence, int itemWisdom, int itemConstitution, int itemSpellPower, int itemMagicResistance, int itemMagicDamage, int itemFireResistance, int armorTemplateDamageReduction, int setIntelligence, int setWisdom, int setConstitution, int setMagicResistance, int setMagicDamage, int setFireResistance, int setDamageReduction, int itemHpRegen, int setHpRegen, int itemMpRegen, int setMpRegen, int matchedEquipmentRows, int matchedArmorRows, int matchedVipRows, int matchedSingleCardRows, int activeSetBonuses, int activeCardCombos, int itemStrength, int itemWeightReduction, int armorTemplateWeightReduction, int setStrength, int setWeightReduction, int itemMagicHit, int setMagicHit, SourceEquipmentPhysicalStats physical, boolean healingHelmEquipped, int itemBlindResistance, int setBlindResistance, int itemSleepResistance, int setSleepResistance, int itemFreezeResistance, int setFreezeResistance, int itemStoneResistance, int setStoneResistance, int itemSustainResistance, int setSustainResistance, boolean hasteCostHalvingHelmEquipped) {
        this(itemIntelligence, itemWisdom, itemConstitution, itemSpellPower, itemMagicResistance, itemMagicDamage, itemFireResistance, armorTemplateDamageReduction, setIntelligence, setWisdom, setConstitution, setMagicResistance, setMagicDamage, setFireResistance, setDamageReduction, itemHpRegen, setHpRegen, itemMpRegen, setMpRegen, matchedEquipmentRows, matchedArmorRows, matchedVipRows, matchedSingleCardRows, activeSetBonuses, activeCardCombos, itemStrength, itemWeightReduction, armorTemplateWeightReduction, setStrength, setWeightReduction, itemMagicHit, setMagicHit, physical, healingHelmEquipped, itemBlindResistance, setBlindResistance, itemSleepResistance, setSleepResistance, itemFreezeResistance, setFreezeResistance, itemStoneResistance, setStoneResistance, itemSustainResistance, setSustainResistance, hasteCostHalvingHelmEquipped, false);
    }

    SourceEquipmentMagicStats(int itemIntelligence, int itemWisdom, int itemConstitution, int itemSpellPower, int itemMagicResistance, int itemMagicDamage, int itemFireResistance, int armorTemplateDamageReduction, int setIntelligence, int setWisdom, int setConstitution, int setMagicResistance, int setMagicDamage, int setFireResistance, int setDamageReduction, int itemHpRegen, int setHpRegen, int itemMpRegen, int setMpRegen, int matchedEquipmentRows, int matchedArmorRows, int matchedVipRows, int matchedSingleCardRows, int activeSetBonuses, int activeCardCombos, int itemStrength, int itemWeightReduction, int armorTemplateWeightReduction, int setStrength, int setWeightReduction, int itemMagicHit, int setMagicHit, SourceEquipmentPhysicalStats physical, boolean healingHelmEquipped, int itemBlindResistance, int setBlindResistance, int itemSleepResistance, int setSleepResistance, int itemFreezeResistance, int setFreezeResistance, int itemStoneResistance, int setStoneResistance, int itemSustainResistance, int setSustainResistance) {
        this(itemIntelligence, itemWisdom, itemConstitution, itemSpellPower, itemMagicResistance, itemMagicDamage, itemFireResistance, armorTemplateDamageReduction, setIntelligence, setWisdom, setConstitution, setMagicResistance, setMagicDamage, setFireResistance, setDamageReduction, itemHpRegen, setHpRegen, itemMpRegen, setMpRegen, matchedEquipmentRows, matchedArmorRows, matchedVipRows, matchedSingleCardRows, activeSetBonuses, activeCardCombos, itemStrength, itemWeightReduction, armorTemplateWeightReduction, setStrength, setWeightReduction, itemMagicHit, setMagicHit, physical, healingHelmEquipped, itemBlindResistance, setBlindResistance, itemSleepResistance, setSleepResistance, itemFreezeResistance, setFreezeResistance, itemStoneResistance, setStoneResistance, itemSustainResistance, setSustainResistance, false);
    }

    SourceEquipmentMagicStats(int itemIntelligence, int itemWisdom, int itemConstitution, int itemSpellPower, int itemMagicResistance, int itemMagicDamage, int itemFireResistance, int armorTemplateDamageReduction, int setIntelligence, int setWisdom, int setConstitution, int setMagicResistance, int setMagicDamage, int setFireResistance, int setDamageReduction, int itemHpRegen, int setHpRegen, int itemMpRegen, int setMpRegen, int matchedEquipmentRows, int matchedArmorRows, int matchedVipRows, int matchedSingleCardRows, int activeSetBonuses, int activeCardCombos, int itemStrength, int itemWeightReduction, int armorTemplateWeightReduction, int setStrength, int setWeightReduction, int itemMagicHit, int setMagicHit, SourceEquipmentPhysicalStats physical, boolean healingHelmEquipped, int itemBlindResistance, int setBlindResistance, int itemSleepResistance, int setSleepResistance, int itemFreezeResistance, int setFreezeResistance, int itemStoneResistance, int setStoneResistance) {
        this(itemIntelligence, itemWisdom, itemConstitution, itemSpellPower, itemMagicResistance, itemMagicDamage, itemFireResistance, armorTemplateDamageReduction, setIntelligence, setWisdom, setConstitution, setMagicResistance, setMagicDamage, setFireResistance, setDamageReduction, itemHpRegen, setHpRegen, itemMpRegen, setMpRegen, matchedEquipmentRows, matchedArmorRows, matchedVipRows, matchedSingleCardRows, activeSetBonuses, activeCardCombos, itemStrength, itemWeightReduction, armorTemplateWeightReduction, setStrength, setWeightReduction, itemMagicHit, setMagicHit, physical, healingHelmEquipped, itemBlindResistance, setBlindResistance, itemSleepResistance, setSleepResistance, itemFreezeResistance, setFreezeResistance, itemStoneResistance, setStoneResistance, 0, 0);
    }

    SourceEquipmentMagicStats(int itemIntelligence, int itemWisdom, int itemConstitution, int itemSpellPower, int itemMagicResistance, int itemMagicDamage, int itemFireResistance, int armorTemplateDamageReduction, int setIntelligence, int setWisdom, int setConstitution, int setMagicResistance, int setMagicDamage, int setFireResistance, int setDamageReduction, int itemHpRegen, int setHpRegen, int itemMpRegen, int setMpRegen, int matchedEquipmentRows, int matchedArmorRows, int matchedVipRows, int matchedSingleCardRows, int activeSetBonuses, int activeCardCombos, int itemStrength, int itemWeightReduction, int armorTemplateWeightReduction, int setStrength, int setWeightReduction, int itemMagicHit, int setMagicHit, SourceEquipmentPhysicalStats physical, boolean healingHelmEquipped) {
        this(itemIntelligence, itemWisdom, itemConstitution, itemSpellPower, itemMagicResistance, itemMagicDamage, itemFireResistance, armorTemplateDamageReduction, setIntelligence, setWisdom, setConstitution, setMagicResistance, setMagicDamage, setFireResistance, setDamageReduction, itemHpRegen, setHpRegen, itemMpRegen, setMpRegen, matchedEquipmentRows, matchedArmorRows, matchedVipRows, matchedSingleCardRows, activeSetBonuses, activeCardCombos, itemStrength, itemWeightReduction, armorTemplateWeightReduction, setStrength, setWeightReduction, itemMagicHit, setMagicHit, physical, healingHelmEquipped, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    SourceEquipmentMagicStats(int itemIntelligence, int itemWisdom, int itemConstitution, int itemSpellPower, int itemMagicResistance, int itemMagicDamage, int itemFireResistance, int armorTemplateDamageReduction, int setIntelligence, int setWisdom, int setConstitution, int setMagicResistance, int setMagicDamage, int setFireResistance, int setDamageReduction, int itemHpRegen, int setHpRegen, int itemMpRegen, int setMpRegen, int matchedEquipmentRows, int matchedArmorRows, int matchedVipRows, int matchedSingleCardRows, int activeSetBonuses, int activeCardCombos, int itemStrength, int itemWeightReduction, int armorTemplateWeightReduction, int setStrength, int setWeightReduction, int itemMagicHit, int setMagicHit, SourceEquipmentPhysicalStats physical) {
        this(itemIntelligence, itemWisdom, itemConstitution, itemSpellPower, itemMagicResistance, itemMagicDamage, itemFireResistance, armorTemplateDamageReduction, setIntelligence, setWisdom, setConstitution, setMagicResistance, setMagicDamage, setFireResistance, setDamageReduction, itemHpRegen, setHpRegen, itemMpRegen, setMpRegen, matchedEquipmentRows, matchedArmorRows, matchedVipRows, matchedSingleCardRows, activeSetBonuses, activeCardCombos, itemStrength, itemWeightReduction, armorTemplateWeightReduction, setStrength, setWeightReduction, itemMagicHit, setMagicHit, physical, false);
    }

    SourceEquipmentMagicStats(int itemMagicHit, int setMagicHit) {
        this(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, itemMagicHit, setMagicHit, SourceEquipmentPhysicalStats.none());
    }

    SourceEquipmentMagicStats(int itemMagicHit, int setMagicHit, int itemStunResistance, int setStunResistance, int itemStunSuccess, int setStunSuccess) {
        this(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, itemMagicHit, setMagicHit, SourceEquipmentPhysicalStats.none(), false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, false, itemStunResistance, setStunResistance, itemStunSuccess, setStunSuccess);
    }

    SourceEquipmentMagicStats(int itemMagicHit, int setMagicHit, boolean healingHelmEquipped) {
        this(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, itemMagicHit, setMagicHit, SourceEquipmentPhysicalStats.none(), healingHelmEquipped);
    }

    SourceEquipmentMagicStats(int itemMagicHit, int setMagicHit, boolean healingHelmEquipped, int itemBlindResistance, int setBlindResistance) {
        this(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, itemMagicHit, setMagicHit, SourceEquipmentPhysicalStats.none(), healingHelmEquipped, itemBlindResistance, setBlindResistance, 0, 0, 0, 0, 0, 0);
    }

    SourceEquipmentMagicStats(int itemMagicHit, int setMagicHit, boolean healingHelmEquipped, int itemBlindResistance, int setBlindResistance, int itemSleepResistance, int setSleepResistance) {
        this(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, itemMagicHit, setMagicHit, SourceEquipmentPhysicalStats.none(), healingHelmEquipped, itemBlindResistance, setBlindResistance, itemSleepResistance, setSleepResistance, 0, 0, 0, 0);
    }

    SourceEquipmentMagicStats(int itemMagicHit, int setMagicHit, boolean healingHelmEquipped, int itemBlindResistance, int setBlindResistance, int itemSleepResistance, int setSleepResistance, int itemFreezeResistance, int setFreezeResistance, int itemStoneResistance, int setStoneResistance) {
        this(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, itemMagicHit, setMagicHit, SourceEquipmentPhysicalStats.none(), healingHelmEquipped, itemBlindResistance, setBlindResistance, itemSleepResistance, setSleepResistance, itemFreezeResistance, setFreezeResistance, itemStoneResistance, setStoneResistance);
    }

    SourceEquipmentMagicStats(int itemMagicHit, int setMagicHit, boolean healingHelmEquipped, int itemBlindResistance, int setBlindResistance, int itemSleepResistance, int setSleepResistance, int itemFreezeResistance, int setFreezeResistance, int itemStoneResistance, int setStoneResistance, int itemSustainResistance, int setSustainResistance) {
        this(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, itemMagicHit, setMagicHit, SourceEquipmentPhysicalStats.none(), healingHelmEquipped, itemBlindResistance, setBlindResistance, itemSleepResistance, setSleepResistance, itemFreezeResistance, setFreezeResistance, itemStoneResistance, setStoneResistance, itemSustainResistance, setSustainResistance);
    }

    SourceEquipmentMagicStats(SourceEquipmentPhysicalStats physical, int matchedEquipmentRows, int matchedArmorRows) {
        this(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, matchedEquipmentRows, matchedArmorRows, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, physical);
    }

    SourceEquipmentMagicStats(SourceEquipmentPhysicalStats physical, int matchedEquipmentRows, int matchedArmorRows, int itemStunSuccess) {
        this(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, matchedEquipmentRows, matchedArmorRows, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, physical, false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, false, 0, 0, itemStunSuccess, 0);
    }

    SourceEquipmentMagicStats(SourceEquipmentPhysicalStats physical, int armorTemplateDamageReduction, int setDamageReduction, int matchedEquipmentRows, int matchedArmorRows) {
        this(0, 0, 0, 0, 0, 0, 0, armorTemplateDamageReduction, 0, 0, 0, 0, 0, 0, setDamageReduction, 0, 0, 0, 0, matchedEquipmentRows, matchedArmorRows, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, physical);
    }

    SourceEquipmentMagicStats(int itemIntelligence, int itemWisdom, int itemConstitution, int itemSpellPower, int itemMagicResistance, int itemMagicDamage, int itemFireResistance, int armorTemplateDamageReduction, int setIntelligence, int setWisdom, int setConstitution, int setMagicResistance, int setMagicDamage, int setFireResistance, int setDamageReduction, int itemHpRegen, int setHpRegen, int itemMpRegen, int setMpRegen, int matchedEquipmentRows, int matchedArmorRows, int matchedVipRows, int matchedSingleCardRows, int activeSetBonuses, int activeCardCombos, int itemStrength, int itemWeightReduction, int armorTemplateWeightReduction, int setStrength, int setWeightReduction) {
        this(itemIntelligence, itemWisdom, itemConstitution, itemSpellPower, itemMagicResistance, itemMagicDamage, itemFireResistance, armorTemplateDamageReduction, setIntelligence, setWisdom, setConstitution, setMagicResistance, setMagicDamage, setFireResistance, setDamageReduction, itemHpRegen, setHpRegen, itemMpRegen, setMpRegen, matchedEquipmentRows, matchedArmorRows, matchedVipRows, matchedSingleCardRows, activeSetBonuses, activeCardCombos, itemStrength, itemWeightReduction, armorTemplateWeightReduction, setStrength, setWeightReduction, 0, 0, SourceEquipmentPhysicalStats.none());
    }

    SourceEquipmentMagicStats(int itemIntelligence, int itemWisdom, int itemConstitution, int itemSpellPower, int itemMagicResistance, int itemMagicDamage, int itemFireResistance, int armorTemplateDamageReduction, int setIntelligence, int setConstitution, int setMagicResistance, int setMagicDamage, int setFireResistance, int setDamageReduction, int itemHpRegen, int setHpRegen, int matchedEquipmentRows, int matchedArmorRows, int matchedVipRows, int matchedSingleCardRows, int activeSetBonuses, int activeCardCombos, int itemStrength, int itemWeightReduction, int armorTemplateWeightReduction, int setStrength, int setWeightReduction) {
        this(itemIntelligence, itemWisdom, itemConstitution, itemSpellPower, itemMagicResistance, itemMagicDamage, itemFireResistance, armorTemplateDamageReduction, setIntelligence, 0, setConstitution, setMagicResistance, setMagicDamage, setFireResistance, setDamageReduction, itemHpRegen, setHpRegen, 0, 0, matchedEquipmentRows, matchedArmorRows, matchedVipRows, matchedSingleCardRows, activeSetBonuses, activeCardCombos, itemStrength, itemWeightReduction, armorTemplateWeightReduction, setStrength, setWeightReduction, 0, 0, SourceEquipmentPhysicalStats.none());
    }

    SourceEquipmentMagicStats(int itemIntelligence, int itemWisdom, int itemConstitution, int itemSpellPower, int itemMagicResistance, int itemMagicDamage, int itemFireResistance, int armorTemplateDamageReduction, int setIntelligence, int setConstitution, int setMagicResistance, int setMagicDamage, int setFireResistance, int setDamageReduction, int itemHpRegen, int setHpRegen, int matchedEquipmentRows, int matchedArmorRows, int matchedVipRows, int matchedSingleCardRows, int activeSetBonuses, int activeCardCombos) {
        this(itemIntelligence, itemWisdom, itemConstitution, itemSpellPower, itemMagicResistance, itemMagicDamage, itemFireResistance, armorTemplateDamageReduction, setIntelligence, 0, setConstitution, setMagicResistance, setMagicDamage, setFireResistance, setDamageReduction, itemHpRegen, setHpRegen, 0, 0, matchedEquipmentRows, matchedArmorRows, matchedVipRows, matchedSingleCardRows, activeSetBonuses, activeCardCombos, 0, 0, 0, 0, 0);
    }

    SourceEquipmentMagicStats(int itemIntelligence, int itemWisdom, int itemSpellPower, int itemMagicResistance, int itemMagicDamage, int itemFireResistance, int armorTemplateDamageReduction, int setIntelligence, int setMagicResistance, int setMagicDamage, int setFireResistance, int setDamageReduction, int matchedEquipmentRows, int matchedArmorRows, int matchedVipRows, int matchedSingleCardRows, int activeSetBonuses, int activeCardCombos) {
        this(itemIntelligence, itemWisdom, 0, itemSpellPower, itemMagicResistance, itemMagicDamage, itemFireResistance, armorTemplateDamageReduction, setIntelligence, 0, setMagicResistance, setMagicDamage, setFireResistance, setDamageReduction, 0, 0, matchedEquipmentRows, matchedArmorRows, matchedVipRows, matchedSingleCardRows, activeSetBonuses, activeCardCombos);
    }

    SourceEquipmentMagicStats(int itemIntelligence, int itemWisdom, int itemSpellPower, int itemMagicResistance, int itemMagicDamage, int itemFireResistance, int armorTemplateDamageReduction, int setIntelligence, int setMagicResistance, int setMagicDamage, int setFireResistance, int setDamageReduction, int itemHpRegen, int setHpRegen, int matchedEquipmentRows, int matchedArmorRows, int matchedVipRows, int matchedSingleCardRows, int activeSetBonuses, int activeCardCombos) {
        this(itemIntelligence, itemWisdom, 0, itemSpellPower, itemMagicResistance, itemMagicDamage, itemFireResistance, armorTemplateDamageReduction, setIntelligence, 0, setMagicResistance, setMagicDamage, setFireResistance, setDamageReduction, itemHpRegen, setHpRegen, matchedEquipmentRows, matchedArmorRows, matchedVipRows, matchedSingleCardRows, activeSetBonuses, activeCardCombos);
    }

    SourceEquipmentMagicStats {
        if (physical == null) {
            throw new IllegalArgumentException("source equipment snapshot has no physical layer");
        }
        if (matchedEquipmentRows < 0 || matchedArmorRows < 0 || matchedVipRows < 0 || matchedSingleCardRows < 0 || activeSetBonuses < 0 || activeCardCombos < 0 || matchedArmorRows > matchedEquipmentRows) {
            throw new IllegalArgumentException("invalid source equipment magic snapshot");
        }
        if (matchedVipRows > matchedEquipmentRows || matchedArmorRows + matchedVipRows > matchedEquipmentRows) {
            throw new IllegalArgumentException("invalid source equipment row classification");
        }
    }

    static SourceEquipmentMagicStats none() {
        return new SourceEquipmentMagicStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    int effectiveIntelligence(int baseIntelligence) {
        return NumericBreakthrough.clampAbility((long)baseIntelligence + (long)this.itemIntelligence + (long)this.setIntelligence);
    }

    int magicIntelligenceTerm(int baseIntelligence) {
        return Math.max(1, NumericBreakthrough.clampExtendedValue((long)this.effectiveIntelligence(baseIntelligence) + (long)this.itemSpellPower));
    }

    int totalMagicDamage() {
        return this.itemMagicDamage + this.setMagicDamage;
    }

    int totalMagicHit() {
        return Math.addExact(this.itemMagicHit, this.setMagicHit);
    }

    int totalBlindResistance() {
        return Math.max(-128, Math.min(127, Math.addExact(this.itemBlindResistance, this.setBlindResistance)));
    }

    int totalSleepResistance() {
        return Math.max(-128, Math.min(127, Math.addExact(this.itemSleepResistance, this.setSleepResistance)));
    }

    int totalFreezeResistance() {
        return Math.max(-128, Math.min(127, Math.addExact(this.itemFreezeResistance, this.setFreezeResistance)));
    }

    int totalStoneResistance() {
        return Math.max(-128, Math.min(127, Math.addExact(this.itemStoneResistance, this.setStoneResistance)));
    }

    int totalSustainResistance() {
        return Math.max(-128, Math.min(127, Math.addExact(this.itemSustainResistance, this.setSustainResistance)));
    }

    int totalStunResistance() {
        return Math.max(-128, Math.min(127, Math.addExact(this.itemStunResistance, this.setStunResistance)));
    }

    int totalStunSuccess() {
        return Math.addExact(this.itemStunSuccess, this.setStunSuccess);
    }

    int totalFireResistance() {
        return NumericBreakthrough.clampExtendedValue((long)this.itemFireResistance + (long)this.setFireResistance);
    }

    int totalHpRegen() {
        return this.itemHpRegen + this.setHpRegen;
    }

    int effectiveMprWisdom(int baseWisdom) {
        return NumericBreakthrough.clampAbility((long)baseWisdom + (long)this.itemWisdom + (long)this.setWisdom);
    }

    int totalMpRegen() {
        return this.itemMpRegen + this.setMpRegen;
    }

    int effectiveConstitution(int baseConstitution) {
        return NumericBreakthrough.clampAbility((long)baseConstitution + (long)this.itemConstitution + (long)this.setConstitution);
    }

    int effectiveStrength(int baseStrength) {
        return NumericBreakthrough.clampAbility((long)baseStrength + (long)this.itemStrength + (long)this.setStrength);
    }

    int totalWeightReduction() {
        return this.itemWeightReduction + this.armorTemplateWeightReduction + this.setWeightReduction;
    }

    int totalDirectMagicResistance() {
        return NumericBreakthrough.clampExtendedValue((long)this.itemMagicResistance + (long)this.setMagicResistance);
    }

    int armorReductionBase() {
        return this.armorTemplateDamageReduction + this.setDamageReduction;
    }

    SourceEquipmentMagicStats withRentalEquipment(RentalEquipmentBonuses bonus) {
        if (bonus == null || bonus.matchedItems() == 0) {
            return this;
        }
        return new SourceEquipmentMagicStats(Math.addExact(this.itemIntelligence, bonus.intelligence()), Math.addExact(this.itemWisdom, bonus.wisdom()), Math.addExact(this.itemConstitution, bonus.constitution()), Math.addExact(this.itemSpellPower, bonus.spellPower()), Math.addExact(this.itemMagicResistance, bonus.magicResistance()), Math.addExact(this.itemMagicDamage, bonus.magicDamage()), Math.addExact(this.itemFireResistance, bonus.fireResistance()), Math.addExact(this.armorTemplateDamageReduction, bonus.damageReduction()), this.setIntelligence, this.setWisdom, this.setConstitution, this.setMagicResistance, this.setMagicDamage, this.setFireResistance, this.setDamageReduction, Math.addExact(this.itemHpRegen, bonus.hpRegen()), this.setHpRegen, Math.addExact(this.itemMpRegen, bonus.mpRegen()), this.setMpRegen, this.matchedEquipmentRows, this.matchedArmorRows, this.matchedVipRows, this.matchedSingleCardRows, this.activeSetBonuses, this.activeCardCombos, Math.addExact(this.itemStrength, bonus.strength()), Math.addExact(this.itemWeightReduction, bonus.weightReduction()), this.armorTemplateWeightReduction, this.setStrength, this.setWeightReduction, this.itemMagicHit, this.setMagicHit, this.physical.withRentalEquipment(bonus), this.healingHelmEquipped, Math.addExact(this.itemBlindResistance, bonus.blindResistance()), this.setBlindResistance, Math.addExact(this.itemSleepResistance, bonus.sleepResistance()), this.setSleepResistance, Math.addExact(this.itemFreezeResistance, bonus.freezeResistance()), this.setFreezeResistance, Math.addExact(this.itemStoneResistance, bonus.stoneResistance()), this.setStoneResistance, Math.addExact(this.itemSustainResistance, bonus.sustainResistance()), this.setSustainResistance, this.hasteCostHalvingHelmEquipped, this.greaterHasteCostHalvingHelmEquipped, Math.addExact(this.itemStunResistance, bonus.stunResistance()), this.setStunResistance, this.itemStunSuccess, this.setStunSuccess);
    }
}

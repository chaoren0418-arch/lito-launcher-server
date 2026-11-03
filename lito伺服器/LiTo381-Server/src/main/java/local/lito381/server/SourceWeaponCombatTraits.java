// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

record SourceWeaponCombatTraits(int material, int blessing, int attributeKind, int attributeLevel, int attributeDamage, int doubleDamageChance, double doubleDamageMultiplier, int maximumDamageChance, boolean doubleDamageEffect, boolean maximumDamageEffect, int inventoryGfx) {
    SourceWeaponCombatTraits(int material, int blessing, int attributeKind, int attributeLevel, int attributeDamage) {
        this(material, blessing, attributeKind, attributeLevel, attributeDamage, 0, 0.0, 0, false, false, 0);
    }

    SourceWeaponCombatTraits(int material, int blessing, int attributeKind, int attributeLevel, int attributeDamage, int doubleDamageChance, double doubleDamageMultiplier, int maximumDamageChance, boolean doubleDamageEffect, boolean maximumDamageEffect) {
        this(material, blessing, attributeKind, attributeLevel, attributeDamage, doubleDamageChance, doubleDamageMultiplier, maximumDamageChance, doubleDamageEffect, maximumDamageEffect, 0);
    }

    SourceWeaponCombatTraits {
        block6: {
            block5: {
                if (material < 0 || material > 22 || blessing < 0 || blessing > 2) break block5;
                switch (attributeKind) {
                    case 0: 
                    case 1: 
                    case 2: 
                    case 4: 
                    case 8: 
                    case 16: 
                    case 32: 
                    case 64: 
                    case 128: {
                        break;
                    }
                    default: {
                        break block5;
                    }
                }
                if (attributeLevel >= 0 && attributeDamage >= 0 && attributeKind == 0 == (attributeLevel == 0) && attributeLevel == 0 == (attributeDamage == 0) && doubleDamageChance >= 0 && doubleDamageChance <= 100 && Double.isFinite(doubleDamageMultiplier) && !(doubleDamageMultiplier < 0.0) && maximumDamageChance >= 0 && maximumDamageChance <= 100 && inventoryGfx >= 0 && inventoryGfx <= 65535 && doubleDamageChance == 0 == (doubleDamageMultiplier == 0.0)) break block6;
            }
            throw new IllegalArgumentException("invalid source weapon combat traits");
        }
    }

    static SourceWeaponCombatTraits none() {
        return new SourceWeaponCombatTraits(0, 1, 0, 0, 0, 0, 0.0, 0, false, false, 0);
    }
}

// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.BattleMagicResistance;
import local.lito381.server.CharacterSummary;

record SourceBaseMagicResistance(int value, int classBase, int levelBonus, int wisdomBonus, int originalBonus) {
    SourceBaseMagicResistance {
        if (value < 0 || classBase < 0 || levelBonus < 0 || wisdomBonus < 0 || originalBonus < 0 || value != classBase + levelBonus + wisdomBonus + originalBonus) {
            throw new IllegalArgumentException("invalid source base magic resistance");
        }
    }

    static SourceBaseMagicResistance from(int type, int level, int wisdomBonus, int originalWisdom) {
        if (level < 0 || wisdomBonus < 0) {
            throw new IllegalArgumentException("invalid source base MR inputs");
        }
        int classBase = BattleMagicResistance.classBase(type);
        int levelBonus = level / 2;
        int originalBonus = BattleMagicResistance.originalMrBonus(type, originalWisdom);
        return new SourceBaseMagicResistance(classBase + levelBonus + wisdomBonus + originalBonus, classBase, levelBonus, wisdomBonus, originalBonus);
    }

    static SourceBaseMagicResistance from(CharacterSummary character) {
        return SourceBaseMagicResistance.from(character.type(), character.level(), BattleMagicResistance.wisdomMagicResistance(character.wisdom()), character.originalWisdom());
    }
}

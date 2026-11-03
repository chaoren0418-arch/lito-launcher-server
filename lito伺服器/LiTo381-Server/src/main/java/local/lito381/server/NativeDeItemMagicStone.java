// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDeItemAbility;

public record NativeDeItemMagicStone(int itemId, int tier, String nameSuffix, int probSuccess, int probFailMode, int duration, int procChance, int dmgType, int dmgFix, int dmgRnd, int dmgAttr, int areaSize, int gfxId, int skillId, int hpDrain, int mpDrain, NativeDeItemAbility passiveAbility) {
    public static NativeDeItemAbility buildPassiveAbility(int n, int n2, int n3, int n4, int n5, int n6, int n7, int n8, int n9) {
        NativeDeItemAbility nativeDeItemAbility = new NativeDeItemAbility();
        if (n != 0) {
            nativeDeItemAbility.add(NativeDeItemAbility.AbilityType.STR, n);
        }
        if (n2 != 0) {
            nativeDeItemAbility.add(NativeDeItemAbility.AbilityType.DEX, n2);
        }
        if (n3 != 0) {
            nativeDeItemAbility.add(NativeDeItemAbility.AbilityType.INT, n3);
        }
        if (n4 != 0) {
            nativeDeItemAbility.add(NativeDeItemAbility.AbilityType.WIS, n4);
        }
        if (n5 != 0) {
            nativeDeItemAbility.add(NativeDeItemAbility.AbilityType.CON, n5);
        }
        if (n6 != 0) {
            nativeDeItemAbility.add(NativeDeItemAbility.AbilityType.HP, n6);
        }
        if (n7 != 0) {
            nativeDeItemAbility.add(NativeDeItemAbility.AbilityType.MP, n7);
        }
        if (n8 != 0) {
            nativeDeItemAbility.add(NativeDeItemAbility.AbilityType.MR, n8);
        }
        if (n9 != 0) {
            nativeDeItemAbility.add(NativeDeItemAbility.AbilityType.SP, n9);
        }
        return nativeDeItemAbility;
    }
}

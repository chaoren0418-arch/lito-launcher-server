// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDeItemAbility;

public class NativeDeItemVipTemplate {
    private final NativeDeItemAbility _ability = new NativeDeItemAbility();
    private int _type;
    private int _effectIcon;
    private int _polyId;

    public NativeDeItemAbility getAbility() {
        return this._ability;
    }

    public int get_type() {
        return this._type;
    }

    public void set_type(int n) {
        this._type = n;
    }

    public int get_effect_icon() {
        return this._effectIcon;
    }

    public void set_effect_icon(int n) {
        this._effectIcon = n;
    }

    public int get_polyId() {
        return this._polyId;
    }

    public void set_polyId(int n) {
        this._polyId = n;
    }

    public void setAbilityFromDb(int n, int n2, int n3, int n4, int n5, int n6, int n7, int n8, int n9, int n10, int n11, int n12, int n13, int n14, int n15, int n16, int n17, int n18, int n19, int n20, int n21, int n22, int n23, int n24, int n25, int n26, int n27, int n28, int n29, int n30, int n31) {
        if (n != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.STR, n);
        }
        if (n2 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.DEX, n2);
        }
        if (n3 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.CON, n3);
        }
        if (n4 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.INT, n4);
        }
        if (n5 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.WIS, n5);
        }
        if (n6 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.CHA, n6);
        }
        if (n7 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.AC, n7);
        }
        if (n8 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.HP, n8);
        }
        if (n9 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.MP, n9);
        }
        if (n10 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.HP_REGEN, n10);
        }
        if (n11 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.MP_REGEN, n11);
        }
        if (n12 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.MR, n12);
        }
        if (n13 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.SP, n13);
        }
        if (n14 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.DMG, n14);
        }
        if (n16 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.BOW_DMG, n16);
        }
        if (n15 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.HIT, n15);
        }
        if (n17 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.BOW_HIT, n17);
        }
        if (n18 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.DMG_REDUX, n18);
        }
        if (n19 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.MAGIC_DMG_REDUX, n19);
        }
        if (n20 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.DEF_FIRE, n20);
        }
        if (n21 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.DEF_WIND, n21);
        }
        if (n22 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.DEF_EARTH, n22);
        }
        if (n23 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.DEF_WATER, n23);
        }
        if (n24 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.REGIST_STUN, n24);
        }
        if (n25 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.REGIST_STONE, n25);
        }
        if (n26 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.REGIST_SLEEP, n26);
        }
        if (n27 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.REGIST_FREEZE, n27);
        }
        if (n28 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.REGIST_SUSTAIN, n28);
        }
        if (n29 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.REGIST_BLIND, n29);
        }
        if (n30 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.EXP_RATE, n30);
        }
        if (n31 != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.ADENA_RATE, n31);
        }
    }
}

// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

public class NativeDeItemAttrWeapon {
    private final String _name;
    private final int _stage;
    private final int _type_dmg_passive;
    private final int _chance;
    private final int _probability;
    private final int _type_gfx_id;
    private final int _type_add_dmg_min;
    private final int _type_add_dmg_max;
    private final int _type_drain_hp_min;
    private final int _type_drain_hp_max;
    private final int _type_drain_mp_min;
    private final int _type_drain_mp_max;
    private final double _type_dmgup;
    private final int _type_range;
    private final int _type_range_dmg;

    public NativeDeItemAttrWeapon(String string, int n, int n2, int n3, int n4, int n5, int n6, int n7, int n8, int n9, int n10, int n11, double d, int n12, int n13) {
        this._name = string;
        this._stage = n;
        this._chance = n2;
        this._type_dmg_passive = n3;
        this._probability = n4;
        this._type_gfx_id = n5;
        this._type_add_dmg_min = n6;
        this._type_add_dmg_max = n7;
        this._type_drain_hp_min = n8;
        this._type_drain_hp_max = n9;
        this._type_drain_mp_min = n10;
        this._type_drain_mp_max = n11;
        this._type_dmgup = d;
        this._type_range = n12;
        this._type_range_dmg = n13;
    }

    public final String getName() {
        return this._name;
    }

    public final int getStage() {
        return this._stage;
    }

    public final int getTypeDmgPassive() {
        return this._type_dmg_passive;
    }

    public final int getChance() {
        return this._chance;
    }

    public final int getProbability() {
        return this._probability;
    }

    public final int getTypeGfxId() {
        return this._type_gfx_id;
    }

    public final int getTypeAddDmgMin() {
        return this._type_add_dmg_min;
    }

    public final int getTypeAddDmgMax() {
        return this._type_add_dmg_max;
    }

    public final int getTypeDrainHpMin() {
        return this._type_drain_hp_min;
    }

    public final int getTypeDrainHpMax() {
        return this._type_drain_hp_max;
    }

    public final int getTypeDrainMpMin() {
        return this._type_drain_mp_min;
    }

    public final int getTypeDrainMpMax() {
        return this._type_drain_mp_max;
    }

    public final double getTypeDmgup() {
        return this._type_dmgup;
    }

    public final int getTypeRange() {
        return this._type_range;
    }

    public final int getTypeRangeDmg() {
        return this._type_range_dmg;
    }
}

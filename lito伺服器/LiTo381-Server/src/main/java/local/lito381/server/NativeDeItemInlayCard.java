// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.sql.ResultSet;
import java.sql.SQLException;
import local.lito381.server.NativeDeItemAbility;

public class NativeDeItemInlayCard {
    private final int _itemId;
    private final String _cardName;
    private final int _itemType;
    private final int _limitCount;
    private final NativeDeItemAbility _ability;

    public NativeDeItemInlayCard(ResultSet resultSet) throws SQLException {
        this._itemId = resultSet.getInt("item_id");
        this._cardName = resultSet.getString("card_name");
        this._itemType = resultSet.getInt("item_type");
        this._limitCount = resultSet.getInt("limit_count");
        this._ability = new NativeDeItemAbility();
        this.mapAttributesToAbility(resultSet);
    }

    private void mapAttributesToAbility(ResultSet resultSet) throws SQLException {
        this.add(NativeDeItemAbility.AbilityType.STR, resultSet.getInt("add_str"));
        this.add(NativeDeItemAbility.AbilityType.DEX, resultSet.getInt("add_dex"));
        this.add(NativeDeItemAbility.AbilityType.CON, resultSet.getInt("add_con"));
        this.add(NativeDeItemAbility.AbilityType.INT, resultSet.getInt("add_int"));
        this.add(NativeDeItemAbility.AbilityType.WIS, resultSet.getInt("add_wis"));
        this.add(NativeDeItemAbility.AbilityType.CHA, resultSet.getInt("add_cha"));
        this.add(NativeDeItemAbility.AbilityType.HP, resultSet.getInt("add_hp"));
        this.add(NativeDeItemAbility.AbilityType.MP, resultSet.getInt("add_mp"));
        this.add(NativeDeItemAbility.AbilityType.HP_REGEN, resultSet.getInt("add_hpr"));
        this.add(NativeDeItemAbility.AbilityType.MP_REGEN, resultSet.getInt("add_mpr"));
        this.add(NativeDeItemAbility.AbilityType.SP, resultSet.getInt("add_sp"));
        this.add(NativeDeItemAbility.AbilityType.MR, resultSet.getInt("add_mr"));
        this.add(NativeDeItemAbility.AbilityType.HIT, resultSet.getInt("add_hit"));
        this.add(NativeDeItemAbility.AbilityType.DMG, resultSet.getInt("add_dmg"));
        this.add(NativeDeItemAbility.AbilityType.BOW_HIT, resultSet.getInt("add_bow_hit"));
        this.add(NativeDeItemAbility.AbilityType.BOW_DMG, resultSet.getInt("add_bow_dmg"));
        this.add(NativeDeItemAbility.AbilityType.MAGIC_HIT, resultSet.getInt("add_magic_hit"));
        this.add(NativeDeItemAbility.AbilityType.MAGIC_DMG_MODIFIER, resultSet.getInt("add_magic_dmg"));
        int n = resultSet.getInt("add_ac");
        if (n != 0) {
            this._ability.set(NativeDeItemAbility.AbilityType.AC, -n);
        }
        this.add(NativeDeItemAbility.AbilityType.DMG_REDUX, resultSet.getInt("add_reduction"));
        this.add(NativeDeItemAbility.AbilityType.MELEE_REDUX, resultSet.getInt("add_melee_redux"));
        this.add(NativeDeItemAbility.AbilityType.BOW_REDUX, resultSet.getInt("add_bow_redux"));
        this.add(NativeDeItemAbility.AbilityType.IGNORE_REDUCTION, resultSet.getInt("add_ignore_reduction"));
        this.add(NativeDeItemAbility.AbilityType.DODGE, resultSet.getInt("add_dodge"));
        this.add(NativeDeItemAbility.AbilityType.ER, resultSet.getInt("add_er"));
        this.add(NativeDeItemAbility.AbilityType.PVP_DMG, resultSet.getInt("add_pvp_dmg"));
        this.add(NativeDeItemAbility.AbilityType.PVP_REDUX, resultSet.getInt("add_pvp_redux"));
        this.add(NativeDeItemAbility.AbilityType.DMG_BOSS, resultSet.getInt("add_boss_dmg"));
        this.add(NativeDeItemAbility.AbilityType.BOSS_REDUX, resultSet.getInt("add_boss_redux"));
        this.add(NativeDeItemAbility.AbilityType.PIERCE, resultSet.getInt("add_pierce"));
        this.add(NativeDeItemAbility.AbilityType.STUN_SUCCESS, resultSet.getInt("add_stun_success"));
        this.add(NativeDeItemAbility.AbilityType.WEIGHT_REDUCTION, resultSet.getInt("add_weight_reduction"));
        this.add(NativeDeItemAbility.AbilityType.MAGIC_DMG_REDUX, resultSet.getInt("add_magic_dmg_redux"));
        this.add(NativeDeItemAbility.AbilityType.DEF_FIRE, resultSet.getInt("add_fire"));
        this.add(NativeDeItemAbility.AbilityType.DEF_WATER, resultSet.getInt("add_water"));
        this.add(NativeDeItemAbility.AbilityType.DEF_WIND, resultSet.getInt("add_wind"));
        this.add(NativeDeItemAbility.AbilityType.DEF_EARTH, resultSet.getInt("add_earth"));
        this.add(NativeDeItemAbility.AbilityType.REGIST_STUN, resultSet.getInt("regist_stun"));
        this.add(NativeDeItemAbility.AbilityType.REGIST_STONE, resultSet.getInt("regist_stone"));
        this.add(NativeDeItemAbility.AbilityType.REGIST_SLEEP, resultSet.getInt("regist_sleep"));
        this.add(NativeDeItemAbility.AbilityType.REGIST_FREEZE, resultSet.getInt("regist_freeze"));
        this.add(NativeDeItemAbility.AbilityType.REGIST_BLIND, resultSet.getInt("regist_blind"));
        this.add(NativeDeItemAbility.AbilityType.REGIST_SUSTAIN, resultSet.getInt("regist_sustain"));
        this.add(NativeDeItemAbility.AbilityType.POTION_RECOVERY, resultSet.getInt("add_potion_recovery"));
        this.add(NativeDeItemAbility.AbilityType.POISON_RESIST, resultSet.getInt("add_poison_resist"));
        this.add(NativeDeItemAbility.AbilityType.EXP_RATE, resultSet.getInt("add_exp_rate"));
        this.add(NativeDeItemAbility.AbilityType.DROP_RATE, resultSet.getInt("add_drop_rate"));
        this.add(NativeDeItemAbility.AbilityType.ADENA_RATE, resultSet.getInt("add_adena_rate"));
    }

    private void add(NativeDeItemAbility.AbilityType abilityType, int n) {
        if (n != 0) {
            this._ability.set(abilityType, n);
        }
    }

    public int getItemId() {
        return this._itemId;
    }

    public String getCardName() {
        return this._cardName != null ? this._cardName : "";
    }

    public int getItemType() {
        return this._itemType;
    }

    public int getLimitCount() {
        return this._limitCount;
    }

    public NativeDeItemAbility getAbility() {
        return this._ability;
    }
}

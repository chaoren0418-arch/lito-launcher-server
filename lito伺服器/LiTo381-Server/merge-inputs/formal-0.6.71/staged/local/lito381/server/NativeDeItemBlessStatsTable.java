// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import local.lito381.server.NativeDeItemAbility;
import local.lito381.server.NativeDeItemDefinitionsScope;
import local.lito381.server.NativeDeTerrainTimer;
import local.lito381.server.NativeDeWorldLog;

public class NativeDeItemBlessStatsTable {
    private static final NativeDeWorldLog _log = NativeDeItemDefinitionsScope.Logs.getLogger(NativeDeItemBlessStatsTable.class);
    private final Map<Integer, Map<Integer, NativeDeItemAbility>> _weaponBlessStats;
    private final Map<Integer, Map<Integer, NativeDeItemAbility>> _armorBlessStats;
    final NativeDeItemDefinitionsScope.Data nativeDefinitions = NativeDeItemDefinitionsScope.loadingData();

    private NativeDeItemBlessStatsTable() {
        this._weaponBlessStats = new HashMap<Integer, Map<Integer, NativeDeItemAbility>>();
        this._armorBlessStats = new HashMap<Integer, Map<Integer, NativeDeItemAbility>>();
        this.load();
    }

    public void reload() {
        NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);
        this._weaponBlessStats.clear();
        this._armorBlessStats.clear();
        this.load();
    }

    private void load() {
        NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);
        NativeDeTerrainTimer nativeDeTerrainTimer = new NativeDeTerrainTimer();
        this.loadWeaponBlessStats();
        this.loadArmorBlessStats();
        this.nativeDefinitions.log.info("\u8f09\u5165 \u88dd\u5099_weapon_bless_\u985e\u578b ok: " + this._weaponBlessStats.size() + " (" + nativeDeTerrainTimer.get() + "ms)");
        this.nativeDefinitions.log.info("\u8f09\u5165 \u88dd\u5099_armor_bless_\u985e\u578b ok: " + this._armorBlessStats.size() + " (" + nativeDeTerrainTimer.get() + "ms)");
    }

    private void loadWeaponBlessStats() {
        block24: {
            NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);
            try (Connection connection = NativeDeItemDefinitionsScope.Database.get().getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `\u88dd\u5099_weapon_bless_\u985e\u578b`");){
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    while (resultSet.next()) {
                        int n2 = -1;
                        try {
                            n2 = resultSet.getInt("bless_id");
                            int n3 = resultSet.getInt("weapon_type");
                            NativeDeItemAbility nativeDeItemAbility = new NativeDeItemAbility();
                            this.fillCommonAbility(nativeDeItemAbility, resultSet);
                            this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.BASE_DMG_SMALL, resultSet, "add_dmg_small");
                            this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.BASE_DMG_LARGE, resultSet, "add_dmg_large");
                            this._weaponBlessStats.computeIfAbsent(n2, n -> new HashMap()).put(n3, nativeDeItemAbility);
                            continue;
                        }
                        catch (Exception exception) {
                            this.nativeDefinitions.log.error("\u8f09\u5165\u8cc7\u6599\u5931\u6557\uff0c\u5df2\u7565\u904e\u8a72\u7b46 (bless_id: " + n2 + ")", exception);
                        }
                    }
                }
                finally {
                    if (resultSet != null) {
                        resultSet.close();
                    }
                }
            }
            catch (SQLException sQLException) {
                this.nativeDefinitions.log.error("\u8f09\u5165 \u88dd\u5099_weapon_bless_\u985e\u578b \u5931\u6557! \u8acb\u6aa2\u67e5\u8cc7\u6599\u8868!", sQLException);
            }
        }
    }

    private void loadArmorBlessStats() {
        block25: {
            NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);
            try (Connection connection = NativeDeItemDefinitionsScope.Database.get().getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `\u88dd\u5099_armor_bless_\u985e\u578b`");){
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    while (resultSet.next()) {
                        int n2 = -1;
                        try {
                            int n3;
                            n2 = resultSet.getInt("bless_id");
                            int n4 = resultSet.getInt("armor_type");
                            NativeDeItemAbility nativeDeItemAbility = new NativeDeItemAbility();
                            this.fillCommonAbility(nativeDeItemAbility, resultSet);
                            if (this.hasColumn(resultSet, "add_ac") && (n3 = resultSet.getInt("add_ac")) != 0) {
                                nativeDeItemAbility.set(NativeDeItemAbility.AbilityType.AC, -n3);
                            }
                            this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.MELEE_REDUX, resultSet, "add_melee_redux");
                            this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.BOW_REDUX, resultSet, "add_bow_redux");
                            this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.REGIST_FREEZE, resultSet, "regist_freeze");
                            this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.REGIST_STONE, resultSet, "regist_stone");
                            this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.REGIST_SLEEP, resultSet, "regist_sleep");
                            this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.REGIST_BLIND, resultSet, "regist_blind");
                            this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.REGIST_SUSTAIN, resultSet, "regist_sustain");
                            this._armorBlessStats.computeIfAbsent(n2, n -> new HashMap()).put(n4, nativeDeItemAbility);
                            continue;
                        }
                        catch (Exception exception) {
                            this.nativeDefinitions.log.error("\u8f09\u5165\u8cc7\u6599\u5931\u6557\uff0c\u5df2\u7565\u904e\u8a72\u7b46 (bless_id: " + n2 + ")", exception);
                        }
                    }
                }
                finally {
                    if (resultSet != null) {
                        resultSet.close();
                    }
                }
            }
            catch (SQLException sQLException) {
                this.nativeDefinitions.log.error("\u8f09\u5165 \u88dd\u5099_armor_bless_\u985e\u578b \u5931\u6557! \u8acb\u6aa2\u67e5\u8cc7\u6599\u8868!", sQLException);
            }
        }
    }

    private void fillCommonAbility(NativeDeItemAbility nativeDeItemAbility, ResultSet resultSet) throws SQLException {
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.STR, resultSet, "add_str");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.DEX, resultSet, "add_dex");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.CON, resultSet, "add_con");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.INT, resultSet, "add_int");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.WIS, resultSet, "add_wis");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.CHA, resultSet, "add_cha");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.HP, resultSet, "add_hp");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.MP, resultSet, "add_mp");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.HP_REGEN, resultSet, "add_hpr");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.MP_REGEN, resultSet, "add_mpr");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.MR, resultSet, "add_mr");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.SP, resultSet, "add_sp");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.HIT, resultSet, "add_hit");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.DMG, resultSet, "add_dmg");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.BOW_HIT, resultSet, "add_bow_hit");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.BOW_DMG, resultSet, "add_bow_dmg");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.MAGIC_HIT, resultSet, "add_magic_hit");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.MAGIC_DMG_MODIFIER, resultSet, "add_magic_dmg");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.DMG_REDUX, resultSet, "add_reduction");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.IGNORE_REDUCTION, resultSet, "add_ignore_reduction");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.DODGE, resultSet, "add_dodge");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.ER, resultSet, "add_er");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.DEF_FIRE, resultSet, "add_fire");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.DEF_WATER, resultSet, "add_water");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.DEF_WIND, resultSet, "add_wind");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.DEF_EARTH, resultSet, "add_earth");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.PVP_DMG, resultSet, "add_pvp_dmg");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.PVP_REDUX, resultSet, "add_pvp_redux");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.DMG_BOSS, resultSet, "add_boss_dmg");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.BOSS_REDUX, resultSet, "add_boss_redux");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.PIERCE, resultSet, "add_pierce");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.STUN_SUCCESS, resultSet, "add_stun_success");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.REGIST_STUN, resultSet, "regist_stun");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.POTION_RECOVERY, resultSet, "add_potion_recovery");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.POISON_RESIST, resultSet, "add_poison_resist");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.EXP_RATE, resultSet, "add_exp_rate");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.DROP_RATE, resultSet, "add_drop_rate");
        this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.ADENA_RATE, resultSet, "add_adena_rate");
    }

    private void safeAdd(NativeDeItemAbility nativeDeItemAbility, NativeDeItemAbility.AbilityType abilityType, ResultSet resultSet, String string) {
        try {
            int n;
            if (this.hasColumn(resultSet, string) && (n = resultSet.getInt(string)) != 0) {
                nativeDeItemAbility.set(abilityType, n);
            }
        }
        catch (SQLException sQLException) {
            // empty catch block
        }
    }

    private boolean hasColumn(ResultSet resultSet, String string) throws SQLException {
        try {
            resultSet.findColumn(string);
            return true;
        }
        catch (SQLException sQLException) {
            return false;
        }
    }

    public NativeDeItemAbility getWeaponBlessStats(int n, int n2) {
        Map<Integer, NativeDeItemAbility> map = this._weaponBlessStats.get(n);
        return map != null ? map.get(n2) : null;
    }

    public NativeDeItemAbility getArmorBlessStats(int n, int n2) {
        Map<Integer, NativeDeItemAbility> map = this._armorBlessStats.get(n);
        return map != null ? map.get(n2) : null;
    }

    public static NativeDeItemBlessStatsTable getInstance() {
        return (NativeDeItemBlessStatsTable)NativeDeItemDefinitionsScope.table(NativeDeItemBlessStatsTable.class);
    }
}

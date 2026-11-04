// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import local.lito381.server.NativeDeItemAbility;
import local.lito381.server.NativeDeItemDefinitionsScope;
import local.lito381.server.NativeDeTerrainTimer;
import local.lito381.server.NativeDeWorldLog;

public class NativeDeItemGreaterTierTable {
    private static final NativeDeWorldLog _log = NativeDeItemDefinitionsScope.Logs.getLogger(NativeDeItemGreaterTierTable.class);
    private final Map<Integer, Map<Integer, NativeDeItemAbility>> _weaponTiers;
    private final Map<Integer, Map<Integer, NativeDeItemAbility>> _armorTiers;
    private final Map<Integer, Map<Integer, String>> _weaponTierNotes;
    private final Map<Integer, Map<Integer, String>> _armorTierNotes;
    private final Map<Integer, String> _tierNames;
    private final Map<Integer, List<TierChance>> _weaponChances;
    private final Map<Integer, List<TierChance>> _armorChances;
    private final Random _random;
    final NativeDeItemDefinitionsScope.Data nativeDefinitions = NativeDeItemDefinitionsScope.loadingData();

    private NativeDeItemGreaterTierTable() {
        this._weaponTiers = new HashMap<Integer, Map<Integer, NativeDeItemAbility>>();
        this._armorTiers = new HashMap<Integer, Map<Integer, NativeDeItemAbility>>();
        this._weaponTierNotes = new HashMap<Integer, Map<Integer, String>>();
        this._armorTierNotes = new HashMap<Integer, Map<Integer, String>>();
        this._tierNames = new HashMap<Integer, String>();
        this._weaponChances = new HashMap<Integer, List<TierChance>>();
        this._armorChances = new HashMap<Integer, List<TierChance>>();
        this._random = new Random();
        this.load();
    }

    public void reload() {
        NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);
        this._weaponTiers.clear();
        this._armorTiers.clear();
        this._weaponChances.clear();
        this._armorChances.clear();
        this.load();
    }

    private void load() {
        NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);
        NativeDeTerrainTimer nativeDeTerrainTimer = new NativeDeTerrainTimer();
        this.loadWeaponTiers();
        this.loadArmorTiers();
        this.nativeDefinitions.log.info("\u8f09\u5165 \u88dd\u5099_weapon_greater_\u985e\u578b ok: " + this._weaponTiers.size() + " (" + nativeDeTerrainTimer.get() + "ms)");
        this.nativeDefinitions.log.info("\u8f09\u5165 \u88dd\u5099_armor_greater_\u985e\u578b ok: " + this._armorTiers.size() + " (" + nativeDeTerrainTimer.get() + "ms)");
    }

    private void loadWeaponTiers() {
        block25: {
            NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);
            try (Connection connection = NativeDeItemDefinitionsScope.Database.get().getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `\u88dd\u5099_weapon_greater_\u985e\u578b`");){
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    while (resultSet.next()) {
                        int n2 = -1;
                        try {
                            int n3;
                            n2 = resultSet.getInt("tier_id");
                            int n4 = resultSet.getInt("weapon_type");
                            NativeDeItemAbility nativeDeItemAbility = new NativeDeItemAbility();
                            this.fillCommonAbility(nativeDeItemAbility, resultSet);
                            this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.BASE_DMG_SMALL, resultSet, "add_dmg_small");
                            this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.BASE_DMG_LARGE, resultSet, "add_dmg_large");
                            this._weaponTiers.computeIfAbsent(n2, n -> new HashMap()).put(n4, nativeDeItemAbility);
                            String string = this.readOptionalString(resultSet, "note");
                            if (string != null && !string.isEmpty()) {
                                this._weaponTierNotes.computeIfAbsent(n2, n -> new HashMap()).put(n4, string);
                            }
                            if ((n3 = this.readOptionalInt(resultSet, "chance")) <= 0) continue;
                            this._weaponChances.computeIfAbsent(n4, n -> new ArrayList()).add(new TierChance(n2, n3));
                            continue;
                        }
                        catch (Exception exception) {
                            this.nativeDefinitions.log.error("\u8f09\u5165\u8cc7\u6599\u5931\u6557\uff0c\u5df2\u7565\u904e\u8a72\u7b46 (tier_id: " + n2 + ")", exception);
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
                this.nativeDefinitions.log.error("\u8f09\u5165 \u88dd\u5099_weapon_greater_\u985e\u578b \u5931\u6557! \u8acb\u6aa2\u67e5\u8cc7\u6599\u8868!", sQLException);
            }
        }
    }

    private void loadArmorTiers() {
        block26: {
            NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);
            try (Connection connection = NativeDeItemDefinitionsScope.Database.get().getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `\u88dd\u5099_armor_greater_\u985e\u578b`");){
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    while (resultSet.next()) {
                        int n2 = -1;
                        try {
                            int n3;
                            int n4;
                            n2 = resultSet.getInt("tier_id");
                            int n5 = resultSet.getInt("armor_type");
                            NativeDeItemAbility nativeDeItemAbility = new NativeDeItemAbility();
                            this.fillCommonAbility(nativeDeItemAbility, resultSet);
                            if (this.hasColumn(resultSet, "add_ac") && (n4 = resultSet.getInt("add_ac")) != 0) {
                                nativeDeItemAbility.set(NativeDeItemAbility.AbilityType.AC, -n4);
                            }
                            this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.MELEE_REDUX, resultSet, "add_melee_redux");
                            this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.BOW_REDUX, resultSet, "add_bow_redux");
                            this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.REGIST_FREEZE, resultSet, "regist_freeze");
                            this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.REGIST_STONE, resultSet, "regist_stone");
                            this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.REGIST_SLEEP, resultSet, "regist_sleep");
                            this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.REGIST_BLIND, resultSet, "regist_blind");
                            this.safeAdd(nativeDeItemAbility, NativeDeItemAbility.AbilityType.REGIST_SUSTAIN, resultSet, "regist_sustain");
                            this._armorTiers.computeIfAbsent(n2, n -> new HashMap()).put(n5, nativeDeItemAbility);
                            String string = this.readOptionalString(resultSet, "note");
                            if (string != null && !string.isEmpty()) {
                                this._armorTierNotes.computeIfAbsent(n2, n -> new HashMap()).put(n5, string);
                            }
                            if ((n3 = this.readOptionalInt(resultSet, "chance")) <= 0) continue;
                            this._armorChances.computeIfAbsent(n5, n -> new ArrayList()).add(new TierChance(n2, n3));
                            continue;
                        }
                        catch (Exception exception) {
                            this.nativeDefinitions.log.error("\u8f09\u5165\u8cc7\u6599\u5931\u6557\uff0c\u5df2\u7565\u904e\u8a72\u7b46 (tier_id: " + n2 + ")", exception);
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
                this.nativeDefinitions.log.error("\u8f09\u5165 \u88dd\u5099_armor_greater_\u985e\u578b \u5931\u6557! \u8acb\u6aa2\u67e5\u8cc7\u6599\u8868!", sQLException);
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

    private String readOptionalString(ResultSet resultSet, String string) {
        try {
            return this.hasColumn(resultSet, string) ? resultSet.getString(string) : null;
        }
        catch (SQLException sQLException) {
            this.nativeDefinitions.log.debug("Optional item greater tier column read failed: {}", string, sQLException);
            return null;
        }
    }

    private int readOptionalInt(ResultSet resultSet, String string) {
        try {
            return this.hasColumn(resultSet, string) ? resultSet.getInt(string) : 0;
        }
        catch (SQLException sQLException) {
            this.nativeDefinitions.log.debug("Optional item greater tier column read failed: {}", string, sQLException);
            return 0;
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

    public NativeDeItemAbility getWeaponTier(int n, int n2) {
        Map<Integer, NativeDeItemAbility> map = this._weaponTiers.get(n);
        return map != null ? map.get(n2) : null;
    }

    public NativeDeItemAbility getArmorTier(int n, int n2) {
        Map<Integer, NativeDeItemAbility> map = this._armorTiers.get(n);
        return map != null ? map.get(n2) : null;
    }

    public String getTierName(int n) {
        return this._tierNames.getOrDefault(n, this.getDefaultTierName(n));
    }

    private String getDefaultTierName(int n) {
        switch (n) {
            case 0: {
                return "\u4e00\u822c";
            }
            case 1: {
                return "\u666e\u901a";
            }
            case 2: {
                return "\u7cbe\u826f";
            }
            case 3: {
                return "\u53f2\u8a69";
            }
            case 4: {
                return "\u50b3\u8aaa";
            }
            case 5: {
                return "\u795e\u8a71";
            }
        }
        if (n >= 5) {
            return "\u795e\u8a71";
        }
        return "\u968e\u7d1a " + n;
    }

    public int getRandomWeaponTier(int n) {
        List<TierChance> list = this._weaponChances.get(n);
        return this.pickRandomTier(list);
    }

    public int getRandomArmorTier(int n) {
        List<TierChance> list = this._armorChances.get(n);
        return this.pickRandomTier(list);
    }

    private int pickRandomTier(List<TierChance> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (TierChance tierChance : list) {
            n += tierChance.chance;
        }
        if (n <= 0) {
            return 0;
        }
        int n2 = 100;
        if (n > 100) {
            n2 = n;
        }
        int n3 = this._random.nextInt(n2);
        for (TierChance tierChance : list) {
            if ((n3 -= tierChance.chance) >= 0) continue;
            return tierChance.tierId;
        }
        return 0;
    }

    public String getTierNote(int n, int n2, boolean bl) {
        String string = null;
        if (bl) {
            Map<Integer, String> map = this._weaponTierNotes.get(n);
            if (map != null) {
                string = map.get(n2);
            }
        } else {
            Map<Integer, String> map = this._armorTierNotes.get(n);
            if (map != null) {
                string = map.get(n2);
            }
        }
        if (string == null) {
            return this.getDefaultTierName(n);
        }
        return string;
    }

    public static NativeDeItemGreaterTierTable getInstance() {
        return (NativeDeItemGreaterTierTable)NativeDeItemDefinitionsScope.table(NativeDeItemGreaterTierTable.class);
    }

    private static class TierChance {
        final int tierId;
        final int chance;

        TierChance(int n, int n2) {
            this.tierId = n;
            this.chance = n2;
        }
    }
}

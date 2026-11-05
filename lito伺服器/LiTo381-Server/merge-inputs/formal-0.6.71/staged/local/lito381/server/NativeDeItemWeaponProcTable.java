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
import local.lito381.server.NativeDeItemDefinitionsScope;
import local.lito381.server.NativeDeItemWeaponProc;
import local.lito381.server.NativeDeTerrainTimer;
import local.lito381.server.NativeDeWorldLog;

public class NativeDeItemWeaponProcTable {
    private static final NativeDeWorldLog _log = NativeDeItemDefinitionsScope.Logs.getLogger(NativeDeItemWeaponProcTable.class);
    // 正式 reload 仍引用此欄位，但正式 class 沒有宣告；保留可編譯版本，待 #73 主控裁定。
    private static NativeDeItemWeaponProcTable _instance;
    private Map<Integer, List<NativeDeItemWeaponProc>> _weaponProcs;
    final NativeDeItemDefinitionsScope.Data nativeDefinitions = NativeDeItemDefinitionsScope.loadingData();

    private NativeDeItemWeaponProcTable() {
        this._weaponProcs = new HashMap<Integer, List<NativeDeItemWeaponProc>>();
    }

    public void load() {
        NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);
        NativeDeTerrainTimer nativeDeTerrainTimer = new NativeDeTerrainTimer();
        Map<Integer, List<NativeDeItemWeaponProc>> hashMap = new HashMap<Integer, List<NativeDeItemWeaponProc>>();
        try (Connection connection = NativeDeItemDefinitionsScope.Database.get().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `\u88dd\u5099_weapon_\u9b54\u6cd5` ORDER BY `weapon_id`, `id`");
             ResultSet resultSet = preparedStatement.executeQuery();){
            int n2 = 0;
            while (resultSet.next()) {
                try {
                    NativeDeItemWeaponProc nativeDeItemWeaponProc = new NativeDeItemWeaponProc(resultSet.getInt("id"), resultSet.getInt("weapon_id"), resultSet.getInt("min_enchant"), resultSet.getInt("enchant_step"), resultSet.getInt("prob_base"), resultSet.getInt("prob_enchant"), resultSet.getInt("dmg_type"), resultSet.getInt("dmg_fix"), resultSet.getInt("dmg_rnd"), resultSet.getDouble("dmg_scaling"), resultSet.getInt("dmg_attr"), resultSet.getInt("gfx_id"), resultSet.getInt("gfx_target"), resultSet.getInt("area_size"), resultSet.getInt("hp_drain_fix"), resultSet.getInt("hp_drain_rnd"), resultSet.getInt("mp_drain_fix"), resultSet.getInt("mp_drain_rnd"), resultSet.getInt("skill_id"), resultSet.getInt("skill_time_sec"), resultSet.getInt("skill_target"), resultSet.getString("description"));
                    int n3 = nativeDeItemWeaponProc.weaponId();
                    hashMap.computeIfAbsent(n3, n -> new ArrayList()).add(nativeDeItemWeaponProc);
                    ++n2;
                }
                catch (SQLException sQLException) {
                    this.nativeDefinitions.log.error("\u8f09\u5165 \u88dd\u5099_weapon_\u9b54\u6cd5 \u5931\u6557 (id: " + resultSet.getInt("id") + ")", sQLException);
                }
            }
            this._weaponProcs.clear();
            this._weaponProcs.putAll(hashMap);
            this.nativeDefinitions.log.info("\u8f09\u5165 \u88dd\u5099_weapon_\u9b54\u6cd5 ok: " + n2 + " (" + nativeDeTerrainTimer.get() + "ms)");
        }
        catch (SQLException sQLException) {
            this.nativeDefinitions.log.error("\u8f09\u5165 \u88dd\u5099_weapon_\u9b54\u6cd5 \u5931\u6557! \u8acb\u6aa2\u67e5\u8cc7\u6599\u8868!", sQLException);
        }
    }

    public List<NativeDeItemWeaponProc> getProcs(int n) {
        return this._weaponProcs.getOrDefault(n, new ArrayList());
    }

    public boolean hasProcs(int n) {
        return this._weaponProcs.containsKey(n);
    }

    public void reload() {
        NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);
        _instance = new NativeDeItemWeaponProcTable();
        _instance.load();
    }

    public static NativeDeItemWeaponProcTable getInstance() {
        return (NativeDeItemWeaponProcTable)NativeDeItemDefinitionsScope.table(NativeDeItemWeaponProcTable.class);
    }
}

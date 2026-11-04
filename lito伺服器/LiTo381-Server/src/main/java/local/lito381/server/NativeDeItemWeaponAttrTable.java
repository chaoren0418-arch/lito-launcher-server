// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import local.lito381.server.NativeDeItemAttrWeapon;
import local.lito381.server.NativeDeItemDefinitionsScope;
import local.lito381.server.NativeDeTerrainTimer;
import local.lito381.server.NativeDeWorldLog;

public final class NativeDeItemWeaponAttrTable {
    private static final NativeDeWorldLog _log = NativeDeItemDefinitionsScope.Logs.getLogger(NativeDeItemWeaponAttrTable.class);
    private final Map<Integer, NativeDeItemAttrWeapon> _attrList;
    final NativeDeItemDefinitionsScope.Data nativeDefinitions = NativeDeItemDefinitionsScope.loadingData();

    public NativeDeItemWeaponAttrTable() {
        this._attrList = new LinkedHashMap<Integer, NativeDeItemAttrWeapon>();
    }

    public final void load() {
        NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);
        NativeDeTerrainTimer nativeDeTerrainTimer = new NativeDeTerrainTimer();
        this._attrList.clear();
        try (Connection connection = NativeDeItemDefinitionsScope.Database.get().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `\u88dd\u5099_weapon_\u5c6c\u6027` ORDER BY `id`");
             ResultSet resultSet = preparedStatement.executeQuery();){
            while (resultSet.next()) {
                int n = resultSet.getInt("id");
                String string = resultSet.getString("name");
                int n2 = resultSet.getInt("stage");
                int n3 = resultSet.getInt("chance");
                int n4 = resultSet.getInt("type_dmg_passive");
                int n5 = resultSet.getInt("probability");
                int n6 = resultSet.getInt("type_gfx_id");
                int n7 = resultSet.getInt("type_add_dmg_min");
                int n8 = resultSet.getInt("type_add_dmg_max");
                int n9 = resultSet.getInt("type_drain_hp_min");
                int n10 = resultSet.getInt("type_drain_hp_max");
                int n11 = resultSet.getInt("type_drain_mp_min");
                int n12 = resultSet.getInt("type_drain_mp_max");
                double d = resultSet.getDouble("type_dmgup");
                int n13 = resultSet.getInt("type_range");
                int n14 = resultSet.getInt("type_range_dmg");
                NativeDeItemAttrWeapon nativeDeItemAttrWeapon = new NativeDeItemAttrWeapon(string, n2, n3, n4, n5, n6, n7, n8, n9, n10, n11, n12, d, n13, n14);
                int n15 = n * 100 + n2;
                this._attrList.put(n15, nativeDeItemAttrWeapon);
            }
        }
        catch (SQLException sQLException) {
            this.nativeDefinitions.log.error("\u8f09\u5165 \u88dd\u5099_weapon_\u5c6c\u6027 \u5931\u6557! \u8acb\u6aa2\u67e5\u8cc7\u6599\u8868!", sQLException);
            return;
        }
        this.nativeDefinitions.log.info("\u8f09\u5165 \u88dd\u5099_weapon_\u5c6c\u6027 ok: " + this._attrList.size() + " (" + nativeDeTerrainTimer.get() + "ms)");
    }

    public final NativeDeItemAttrWeapon get(int n, int n2) {
        int n3 = n * 100 + n2;
        return this._attrList.get(n3);
    }

    public static NativeDeItemWeaponAttrTable getInstance() {
        return (NativeDeItemWeaponAttrTable)NativeDeItemDefinitionsScope.table(NativeDeItemWeaponAttrTable.class);
    }
}

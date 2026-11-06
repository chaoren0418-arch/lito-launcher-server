// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import local.lito381.server.NativeDeItemAbility;
import local.lito381.server.NativeDeItemDefinitionsScope;
import local.lito381.server.NativeDeItemMagicStone;
import local.lito381.server.NativeDeTerrainTimer;
import local.lito381.server.NativeDeWorldLog;

public class NativeDeItemMagicStoneTable {
    private static final NativeDeWorldLog _log = NativeDeItemDefinitionsScope.Logs.getLogger(NativeDeItemMagicStoneTable.class);
    private final Map<String, NativeDeItemMagicStone> _stones;
    final NativeDeItemDefinitionsScope.Data nativeDefinitions = NativeDeItemDefinitionsScope.loadingData();

    private NativeDeItemMagicStoneTable() {
        this._stones = new HashMap<String, NativeDeItemMagicStone>();
    }

    public void load() {
        NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);
        NativeDeTerrainTimer nativeDeTerrainTimer = new NativeDeTerrainTimer();
        this._stones.clear();
        try (Connection connection = NativeDeItemDefinitionsScope.Database.get().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `\u88dd\u5099_\u7279\u6b8a_\u9b54\u77f3_\u9b54\u6cd5` ORDER BY `item_id`, `tier`");
             ResultSet resultSet = preparedStatement.executeQuery();){
            int n = 0;
            while (resultSet.next()) {
                try {
                    int n2 = resultSet.getInt("item_id");
                    int n3 = resultSet.getInt("tier");
                    NativeDeItemAbility nativeDeItemAbility = NativeDeItemMagicStone.buildPassiveAbility(resultSet.getInt("add_str"), resultSet.getInt("add_dex"), resultSet.getInt("add_int"), resultSet.getInt("add_wis"), resultSet.getInt("add_con"), resultSet.getInt("add_hp"), resultSet.getInt("add_mp"), resultSet.getInt("add_mr"), resultSet.getInt("add_sp"));
                    NativeDeItemMagicStone nativeDeItemMagicStone = new NativeDeItemMagicStone(n2, n3, resultSet.getString("name_suffix"), resultSet.getInt("prob_success"), resultSet.getInt("prob_fail_mode"), resultSet.getInt("duration"), resultSet.getInt("proc_chance"), resultSet.getInt("dmg_type"), resultSet.getInt("dmg_fix"), resultSet.getInt("dmg_rnd"), resultSet.getInt("dmg_attr"), resultSet.getInt("area_size"), resultSet.getInt("gfx_id"), resultSet.getInt("skill_id"), resultSet.getInt("hp_drain"), resultSet.getInt("mp_drain"), nativeDeItemAbility);
                    this._stones.put(NativeDeItemMagicStoneTable.key(n2, n3), nativeDeItemMagicStone);
                    ++n;
                }
                catch (SQLException sQLException) {
                    this.nativeDefinitions.log.error("\u8f09\u5165 \u88dd\u5099_\u7279\u6b8a_\u9b54\u77f3_\u9b54\u6cd5 \u5931\u6557 (item_id:" + resultSet.getInt("item_id") + ", tier:" + resultSet.getInt("tier") + ")", sQLException);
                }
            }
            NativeDeItemDefinitionsScope.TableLog.loaded(this.nativeDefinitions.log, "\u88dd\u5099_\u7279\u6b8a_\u9b54\u77f3_\u9b54\u6cd5", n, nativeDeTerrainTimer.get());
        }
        catch (SQLException sQLException) {
            this.nativeDefinitions.log.error("\u8f09\u5165 \u88dd\u5099_\u7279\u6b8a_\u9b54\u77f3_\u9b54\u6cd5 \u5931\u6557! \u8acb\u6aa2\u67e5\u8cc7\u6599\u8868!", sQLException);
        }
    }

    private static String key(int n, int n2) {
        return n + "_" + n2;
    }

    public NativeDeItemMagicStone get(int n, int n2) {
        return this._stones.get(NativeDeItemMagicStoneTable.key(n, n2));
    }

    public boolean hasNextTier(int n, int n2) {
        return this._stones.containsKey(NativeDeItemMagicStoneTable.key(n, n2 + 1));
    }

    public boolean isMagicStoneItem(int n) {
        return this._stones.keySet().stream().anyMatch(string -> string.startsWith(n + "_"));
    }

    public int[] getFirstStoneForTest() {
        if (this._stones.isEmpty()) {
            return null;
        }
        String string = this._stones.keySet().iterator().next();
        String[] stringArray = string.split("_");
        if (stringArray.length >= 2) {
            return new int[]{Integer.parseInt(stringArray[0]), Integer.parseInt(stringArray[1])};
        }
        return null;
    }

    public static NativeDeItemMagicStoneTable getInstance() {
        return (NativeDeItemMagicStoneTable)NativeDeItemDefinitionsScope.table(NativeDeItemMagicStoneTable.class);
    }
}

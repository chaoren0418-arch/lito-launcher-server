// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import local.lito381.server.NativeDeShopData;
import local.lito381.server.NativeDeShopResources;
import local.lito381.server.NativeDeTerrainScope;
import local.lito381.server.NativeDeTerrainTimer;
import local.lito381.server.NativeDeWorldLog;

public class NativeDeShopTable {
    private static final NativeDeWorldLog _log = NativeDeTerrainScope.Logs.getLogger(NativeDeShopTable.class);
    private volatile Map<Integer, List<NativeDeShopData>> _shopData = Map.of();

    private NativeDeShopTable() {
        this.load();
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public void load() {
        NativeDeTerrainTimer nativeDeTerrainTimer = new NativeDeTerrainTimer();
        HashMap<Integer, List> hashMap = new HashMap<Integer, List>();
        int n2 = 0;
        try (Connection connection = NativeDeTerrainScope.Database.get().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT mapid, x_min, x_max, y_min, y_max, currency FROM mapids_shop");
             ResultSet resultSet = preparedStatement.executeQuery();){
            while (resultSet.next()) {
                int n3 = -1;
                try {
                    n3 = resultSet.getInt("mapid");
                    int n4 = resultSet.getInt("x_min");
                    int n5 = resultSet.getInt("x_max");
                    int n6 = resultSet.getInt("y_min");
                    int n7 = resultSet.getInt("y_max");
                    int n8 = resultSet.getInt("currency");
                    NativeDeShopData nativeDeShopData = new NativeDeShopData(n3, n4, n5, n6, n7, n8);
                    hashMap.computeIfAbsent(n3, n -> new ArrayList()).add(nativeDeShopData);
                    ++n2;
                }
                catch (Exception exception) {
                    _log.error("\u8f09\u5165\u8cc7\u6599\u5931\u6557\uff0c\u5df2\u7565\u904e\u8a72\u7b46 (npc_id: " + n3 + ")", exception);
                }
            }
            HashMap hashMap2 = new HashMap();
            for (Map.Entry entry : hashMap.entrySet()) {
                hashMap2.put((Integer)entry.getKey(), List.copyOf((Collection)entry.getValue()));
            }
            this._shopData = Map.copyOf(hashMap2);
            _log.info("\u8f09\u5165 mapids_shop ok: {} maps, {} ranges ({}ms)", (Object)this._shopData.size(), (Object)n2, (Object)nativeDeTerrainTimer.get());
            return;
        }
        catch (SQLException sQLException) {
            _log.error("\u8f09\u5165 mapids_shop \u5931\u6557! \u8acb\u6aa2\u67e5\u8cc7\u6599\u8868!", sQLException);
        }
    }

    public boolean checkShop(int n, int n2, int n3) {
        List<NativeDeShopData> list = this._shopData.get(n);
        if (list == null) {
            return false;
        }
        for (NativeDeShopData nativeDeShopData : list) {
            if (!nativeDeShopData.isInRange(n2, n3)) continue;
            return true;
        }
        return false;
    }

    public int getCurrency(int n, int n2, int n3) {
        List<NativeDeShopData> list = this._shopData.get(n);
        if (list == null) {
            return 0;
        }
        for (NativeDeShopData nativeDeShopData : list) {
            if (!nativeDeShopData.isInRange(n2, n3)) continue;
            return nativeDeShopData.getCurrency();
        }
        return 0;
    }

    public static NativeDeShopTable get() {
        return NativeDeShopResources.table();
    }

    public static NativeDeShopTable create() {
        return new NativeDeShopTable();
    }
}

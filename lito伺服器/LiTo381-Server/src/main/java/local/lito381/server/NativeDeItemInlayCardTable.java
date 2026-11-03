// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
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
import local.lito381.server.NativeDeItemDefinitionsScope;
import local.lito381.server.NativeDeItemInlayCard;
import local.lito381.server.NativeDeTerrainTimer;
import local.lito381.server.NativeDeWorldLog;

public class NativeDeItemInlayCardTable {
    private static final NativeDeWorldLog _log = NativeDeItemDefinitionsScope.Logs.getLogger(NativeDeItemInlayCardTable.class);
    private final Map<Integer, NativeDeItemInlayCard> _cardMap;
    final NativeDeItemDefinitionsScope.Data nativeDefinitions = NativeDeItemDefinitionsScope.loadingData();

    private NativeDeItemInlayCardTable() {
        this._cardMap = new HashMap<Integer, NativeDeItemInlayCard>();
        this.load();
    }

    public void reload() {
        NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);
        this._cardMap.clear();
        this.load();
    }

    private void load() {
        NativeDeItemDefinitionsScope.requireLoading(this.nativeDefinitions);
        NativeDeTerrainTimer nativeDeTerrainTimer = new NativeDeTerrainTimer();
        try (Connection connection = NativeDeItemDefinitionsScope.Database.get().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `\u88dd\u5099_\u7279\u6b8a_\u5361\u7247_\u80fd\u529b`");
             ResultSet resultSet = preparedStatement.executeQuery();){
            int n = 0;
            while (resultSet.next()) {
                try {
                    NativeDeItemInlayCard nativeDeItemInlayCard = new NativeDeItemInlayCard(resultSet);
                    this._cardMap.put(nativeDeItemInlayCard.getItemId(), nativeDeItemInlayCard);
                    ++n;
                }
                catch (SQLException sQLException) {
                    this.nativeDefinitions.log.error("\u8f09\u5165\u9472\u5d4c\u5361\u7247\u5931\u6557 (item_id: " + resultSet.getInt("item_id") + "): " + sQLException.getLocalizedMessage(), sQLException);
                }
            }
            this.nativeDefinitions.log.info("\u8f09\u5165 \u88dd\u5099_\u7279\u6b8a_\u5361\u7247_\u80fd\u529b ok: " + n + " (" + nativeDeTerrainTimer.get() + "ms)");
        }
        catch (SQLException sQLException) {
            this.nativeDefinitions.log.error("\u8f09\u5165 \u88dd\u5099_\u7279\u6b8a_\u5361\u7247_\u80fd\u529b \u5931\u6557! \u8acb\u6aa2\u67e5\u8cc7\u6599\u8868!", sQLException);
        }
    }

    public NativeDeItemInlayCard getCard(int n) {
        return this._cardMap.get(n);
    }

    public static NativeDeItemInlayCardTable getInstance() {
        return (NativeDeItemInlayCardTable)NativeDeItemDefinitionsScope.table(NativeDeItemInlayCardTable.class);
    }
}

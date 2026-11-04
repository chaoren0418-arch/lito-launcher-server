// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import local.lito381.server.NativeDeTerrainData;
import local.lito381.server.NativeDeTerrainScope;
import local.lito381.server.NativeDeTerrainTimer;
import local.lito381.server.NativeDeWorldLog;

public final class NativeDeTerrainTable {
    private static final NativeDeWorldLog _log = NativeDeTerrainScope.Logs.getLogger(NativeDeTerrainTable.class);

    public void load() {
        NativeDeTerrainTimer nativeDeTerrainTimer = new NativeDeTerrainTimer();
        try (Connection connection = NativeDeTerrainScope.Database.get().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `mapids`");
             ResultSet resultSet = preparedStatement.executeQuery();){
            NativeDeTerrainScope.maps().clear();
            NativeDeTerrainScope.names().clear();
            while (resultSet.next()) {
                int n = -1;
                try {
                    n = resultSet.getInt("mapid");
                    NativeDeTerrainData nativeDeTerrainData = new NativeDeTerrainData();
                    nativeDeTerrainData.mapId = n;
                    nativeDeTerrainData.startX = resultSet.getInt("startX");
                    nativeDeTerrainData.endX = resultSet.getInt("endX");
                    nativeDeTerrainData.startY = resultSet.getInt("startY");
                    nativeDeTerrainData.endY = resultSet.getInt("endY");
                    nativeDeTerrainData.monster_amount = resultSet.getDouble("monster_amount");
                    nativeDeTerrainData.dropRate = resultSet.getDouble("drop_rate");
                    nativeDeTerrainData.isUnderwater = resultSet.getBoolean("underwater");
                    nativeDeTerrainData.markable = resultSet.getBoolean("markable");
                    nativeDeTerrainData.teleportable = resultSet.getBoolean("teleportable");
                    nativeDeTerrainData.escapable = resultSet.getBoolean("escapable");
                    nativeDeTerrainData.isUseResurrection = resultSet.getBoolean("resurrection");
                    nativeDeTerrainData.isUsePainwand = resultSet.getBoolean("painwand");
                    nativeDeTerrainData.isEnabledDeathPenalty = resultSet.getBoolean("penalty");
                    nativeDeTerrainData.isTakePets = resultSet.getBoolean("take_pets");
                    nativeDeTerrainData.isRecallPets = resultSet.getBoolean("recall_pets");
                    nativeDeTerrainData.isUsableItem = resultSet.getBoolean("usable_item");
                    nativeDeTerrainData.isUsableSkill = resultSet.getBoolean("usable_skill");
                    nativeDeTerrainData.isGuaji = resultSet.getBoolean("is_guaji");
                    nativeDeTerrainData.isUsableShop = resultSet.getInt("usable_shop");
                    try {
                        nativeDeTerrainData.isArrows = resultSet.getInt("is_arrows") == 1;
                    }
                    catch (SQLException sQLException) {
                        nativeDeTerrainData.isArrows = false;
                    }
                    nativeDeTerrainData.copyMapId = -1;
                    try {
                        nativeDeTerrainData.copyMapId = resultSet.getInt("CopyMapId");
                    }
                    catch (SQLException sQLException) {
                        nativeDeTerrainData.copyMapId = -1;
                    }
                    String string = resultSet.getString("locationname");
                    NativeDeTerrainScope.names().put(n, string);
                    NativeDeTerrainScope.maps().put(n, nativeDeTerrainData);
                }
                catch (Exception exception) {
                    _log.error("\u8f09\u5165\u8cc7\u6599\u5931\u6557\uff0c\u5df2\u7565\u904e\u8a72\u7b46 (mapid: " + n + ")", exception);
                }
            }
        }
        catch (SQLException sQLException) {
            _log.error("\u8f09\u5165 mapids \u5931\u6557! \u8acb\u6aa2\u67e5\u8cc7\u6599\u8868!", sQLException);
        }
        _log.info("\u8f09\u5165 mapids ok: " + NativeDeTerrainScope.maps().size() + " (" + nativeDeTerrainTimer.get() + "ms)");
    }

    public Map<Integer, NativeDeTerrainData> getMaps() {
        return NativeDeTerrainScope.maps();
    }

    public NativeDeTerrainData getMapData(int n) {
        return NativeDeTerrainScope.maps().get(n);
    }

    public boolean hasMap(int n) {
        return NativeDeTerrainScope.maps().containsKey(n);
    }

    public int getStartX(int n) {
        NativeDeTerrainData nativeDeTerrainData = NativeDeTerrainScope.maps().get(n);
        if (nativeDeTerrainData == null) {
            return 0;
        }
        return NativeDeTerrainScope.maps().get((Object)Integer.valueOf((int)n)).startX;
    }

    public int getEndX(int n) {
        NativeDeTerrainData nativeDeTerrainData = NativeDeTerrainScope.maps().get(n);
        if (nativeDeTerrainData == null) {
            return 0;
        }
        return NativeDeTerrainScope.maps().get((Object)Integer.valueOf((int)n)).endX;
    }

    public int getStartY(int n) {
        NativeDeTerrainData nativeDeTerrainData = NativeDeTerrainScope.maps().get(n);
        if (nativeDeTerrainData == null) {
            return 0;
        }
        return NativeDeTerrainScope.maps().get((Object)Integer.valueOf((int)n)).startY;
    }

    public int getEndY(int n) {
        NativeDeTerrainData nativeDeTerrainData = NativeDeTerrainScope.maps().get(n);
        if (nativeDeTerrainData == null) {
            return 0;
        }
        return NativeDeTerrainScope.maps().get((Object)Integer.valueOf((int)n)).endY;
    }

    public double getMonsterAmount(int n) {
        NativeDeTerrainData nativeDeTerrainData = NativeDeTerrainScope.maps().get(n);
        if (nativeDeTerrainData == null) {
            return 0.0;
        }
        return nativeDeTerrainData.monster_amount;
    }

    public double getDropRate(int n) {
        NativeDeTerrainData nativeDeTerrainData = NativeDeTerrainScope.maps().get(n);
        if (nativeDeTerrainData == null) {
            return 0.0;
        }
        return nativeDeTerrainData.dropRate;
    }

    public boolean isUnderwater(int n) {
        NativeDeTerrainData nativeDeTerrainData = NativeDeTerrainScope.maps().get(n);
        if (nativeDeTerrainData == null) {
            return false;
        }
        return NativeDeTerrainScope.maps().get((Object)Integer.valueOf((int)n)).isUnderwater;
    }

    public boolean isMarkable(int n) {
        NativeDeTerrainData nativeDeTerrainData = NativeDeTerrainScope.maps().get(n);
        if (nativeDeTerrainData == null) {
            return false;
        }
        return NativeDeTerrainScope.maps().get((Object)Integer.valueOf((int)n)).markable;
    }

    public boolean isTeleportable(int n) {
        NativeDeTerrainData nativeDeTerrainData = NativeDeTerrainScope.maps().get(n);
        if (nativeDeTerrainData == null) {
            return false;
        }
        return NativeDeTerrainScope.maps().get((Object)Integer.valueOf((int)n)).teleportable;
    }

    public boolean isEscapable(int n) {
        NativeDeTerrainData nativeDeTerrainData = NativeDeTerrainScope.maps().get(n);
        if (nativeDeTerrainData == null) {
            return false;
        }
        return NativeDeTerrainScope.maps().get((Object)Integer.valueOf((int)n)).escapable;
    }

    public boolean isUseResurrection(int n) {
        NativeDeTerrainData nativeDeTerrainData = NativeDeTerrainScope.maps().get(n);
        if (nativeDeTerrainData == null) {
            return false;
        }
        return NativeDeTerrainScope.maps().get((Object)Integer.valueOf((int)n)).isUseResurrection;
    }

    public boolean isUsePainwand(int n) {
        NativeDeTerrainData nativeDeTerrainData = NativeDeTerrainScope.maps().get(n);
        if (nativeDeTerrainData == null) {
            return false;
        }
        return NativeDeTerrainScope.maps().get((Object)Integer.valueOf((int)n)).isUsePainwand;
    }

    public boolean isEnabledDeathPenalty(int n) {
        NativeDeTerrainData nativeDeTerrainData = NativeDeTerrainScope.maps().get(n);
        if (nativeDeTerrainData == null) {
            return false;
        }
        return NativeDeTerrainScope.maps().get((Object)Integer.valueOf((int)n)).isEnabledDeathPenalty;
    }

    public boolean isTakePets(int n) {
        NativeDeTerrainData nativeDeTerrainData = NativeDeTerrainScope.maps().get(n);
        if (nativeDeTerrainData == null) {
            return false;
        }
        return NativeDeTerrainScope.maps().get((Object)Integer.valueOf((int)n)).isTakePets;
    }

    public boolean isRecallPets(int n) {
        NativeDeTerrainData nativeDeTerrainData = NativeDeTerrainScope.maps().get(n);
        if (nativeDeTerrainData == null) {
            return false;
        }
        return NativeDeTerrainScope.maps().get((Object)Integer.valueOf((int)n)).isRecallPets;
    }

    public boolean isUsableItem(int n) {
        NativeDeTerrainData nativeDeTerrainData = NativeDeTerrainScope.maps().get(n);
        if (nativeDeTerrainData == null) {
            return false;
        }
        return NativeDeTerrainScope.maps().get((Object)Integer.valueOf((int)n)).isUsableItem;
    }

    public boolean isUsableSkill(int n) {
        NativeDeTerrainData nativeDeTerrainData = NativeDeTerrainScope.maps().get(n);
        if (nativeDeTerrainData == null) {
            return false;
        }
        return NativeDeTerrainScope.maps().get((Object)Integer.valueOf((int)n)).isUsableSkill;
    }

    public boolean isGuaji(int n) {
        NativeDeTerrainData nativeDeTerrainData = NativeDeTerrainScope.maps().get(n);
        if (nativeDeTerrainData == null) {
            return false;
        }
        return nativeDeTerrainData.isGuaji;
    }

    public String getMapName(int n) {
        if (NativeDeTerrainScope.names().containsKey(n)) {
            return NativeDeTerrainScope.names().get(n);
        }
        return String.valueOf(n);
    }

    public int isUsableShop(int n) {
        NativeDeTerrainData nativeDeTerrainData = NativeDeTerrainScope.maps().get(n);
        if (nativeDeTerrainData == null) {
            return 0;
        }
        return NativeDeTerrainScope.maps().get((Object)Integer.valueOf((int)n)).isUsableShop;
    }

    public boolean isArrows(int n) {
        NativeDeTerrainData nativeDeTerrainData = NativeDeTerrainScope.maps().get(n);
        if (nativeDeTerrainData == null) {
            return false;
        }
        return nativeDeTerrainData.isArrows;
    }

    public static NativeDeTerrainTable get() {
        return new NativeDeTerrainTable();
    }
}


// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeShopTable;
import local.lito381.server.NativeDeTerrainPoint;
import local.lito381.server.NativeDeTerrainScope;
import local.lito381.server.NativeDeWorldLog;

public class NativeDeTerrainV1 {
    private static final NativeDeWorldLog _log = NativeDeTerrainScope.Logs.getLogger(NativeDeTerrainV1.class);
    private int _mapId;
    private int _worldTopLeftX;
    private int _worldTopLeftY;
    private int _worldBottomRightX;
    private int _worldBottomRightY;
    private byte[][] _map;
    private final Map<Integer, Byte> _doorOverrides = new ConcurrentHashMap<Integer, Byte>();
    private boolean[][] _arrowPassableOverrideMap;
    private boolean _isUnderwater;
    private boolean _isMarkable;
    private boolean _isTeleportable;
    private boolean _isEscapable;
    private boolean _isUseResurrection;
    private boolean _isUsePainwand;
    private boolean _isEnabledDeathPenalty;
    private boolean _isTakePets;
    private boolean _isRecallPets;
    private boolean _isUsableItem;
    private boolean _isUsableSkill;
    private int _isUsableShop;
    private boolean _isGuaji;
    private static final byte BITFLAG_IS_IMPASSABLE = -128;
    private static final byte[] HEADING_TABLE_X = new byte[]{0, 1, 1, 1, 0, -1, -1, -1};
    private static final byte[] HEADING_TABLE_Y = new byte[]{-1, -1, 0, 1, 1, 1, 0, -1};

    private int getPointKey(int n, int n2) {
        return n << 16 | n2 & 0xFFFF;
    }

    protected NativeDeTerrainV1() {
    }

    public NativeDeTerrainV1(short s, byte[][] byArray, int n, int n2, int n3, int n4, boolean bl, boolean bl2, boolean bl3, boolean bl4, boolean bl5, boolean bl6, boolean bl7, boolean bl8, boolean bl9, boolean bl10, boolean bl11, boolean bl12, int n5) {
        this._mapId = s;
        this._map = byArray;
        this._arrowPassableOverrideMap = new boolean[byArray.length][byArray[0].length];
        this._worldTopLeftX = n;
        this._worldTopLeftY = n3;
        this._worldBottomRightX = n2;
        this._worldBottomRightY = n4;
        this._isUnderwater = bl;
        this._isMarkable = bl2;
        this._isTeleportable = bl3;
        this._isEscapable = bl4;
        this._isUseResurrection = bl5;
        this._isUsePainwand = bl6;
        this._isEnabledDeathPenalty = bl7;
        this._isTakePets = bl8;
        this._isRecallPets = bl9;
        this._isUsableItem = bl10;
        this._isUsableSkill = bl11;
        this._isGuaji = bl12;
        this._isUsableShop = n5;
    }

    public NativeDeTerrainV1(NativeDeTerrainV1 nativeDeTerrainV1) {
        int n;
        this._mapId = nativeDeTerrainV1._mapId;
        this._map = new byte[nativeDeTerrainV1._map.length][];
        for (n = 0; n < nativeDeTerrainV1._map.length; ++n) {
            this._map[n] = (byte[])nativeDeTerrainV1._map[n].clone();
        }
        if (nativeDeTerrainV1._arrowPassableOverrideMap != null) {
            this._arrowPassableOverrideMap = new boolean[nativeDeTerrainV1._arrowPassableOverrideMap.length][];
            for (n = 0; n < nativeDeTerrainV1._arrowPassableOverrideMap.length; ++n) {
                this._arrowPassableOverrideMap[n] = (boolean[])nativeDeTerrainV1._arrowPassableOverrideMap[n].clone();
            }
        }
        this._worldTopLeftX = nativeDeTerrainV1._worldTopLeftX;
        this._worldTopLeftY = nativeDeTerrainV1._worldTopLeftY;
        this._worldBottomRightX = nativeDeTerrainV1._worldBottomRightX;
        this._worldBottomRightY = nativeDeTerrainV1._worldBottomRightY;
        this._doorOverrides.putAll(nativeDeTerrainV1._doorOverrides);
    }

    private int accessTile(int n, int n2) {
        if (!this.isInMap(n, n2)) {
            return 0;
        }
        Byte by = this._doorOverrides.get(this.getPointKey(n, n2));
        if (by != null) {
            return by & 0xFF;
        }
        return this._map[n - this._worldTopLeftX][n2 - this._worldTopLeftY];
    }

    private int accessOriginalTile(int n, int n2) {
        return this.accessTile(n, n2) & 0x7F;
    }

    private void setTile(int n, int n2, int n3) {
        if (!this.isInMap(n, n2)) {
            return;
        }
        this._map[n - this._worldTopLeftX][n2 - this._worldTopLeftY] = (byte)n3;
    }

    public void setTestTile(int n, int n2, int n3) {
        this.setTile(n, n2, n3);
    }

    public void setArrowPassableOverride(int n, int n2) {
        if (!this.isInMap(n, n2) || this._arrowPassableOverrideMap == null) {
            return;
        }
        this._arrowPassableOverrideMap[n - this._worldTopLeftX][n2 - this._worldTopLeftY] = true;
    }

    public boolean isArrowPassableOverride(int n, int n2) {
        if (!this.isInMap(n, n2) || this._arrowPassableOverrideMap == null) {
            return false;
        }
        return this._arrowPassableOverrideMap[n - this._worldTopLeftX][n2 - this._worldTopLeftY];
    }

    public boolean isClosedDoorAt(int n, int n2) {
        if (!this.isInMap(n, n2)) {
            return false;
        }
        Byte by = this._doorOverrides.get(this.getPointKey(n, n2));
        return by != null && (by & 0xFF) == 3;
    }

    public byte[][] getRawTiles() {
        return this._map;
    }

    public int getId() {
        return this._mapId;
    }

    public int getX() {
        return this._worldTopLeftX;
    }

    public int getY() {
        return this._worldTopLeftY;
    }

    public int getWidth() {
        return this._worldBottomRightX - this._worldTopLeftX + 1;
    }

    public int getHeight() {
        return this._worldBottomRightY - this._worldTopLeftY + 1;
    }

    public int getTile(int n, int n2) {
        int n3 = this.accessTile(n, n2);
        if (0 != (n3 & 0xFFFFFF80)) {
            return 300;
        }
        return this.accessOriginalTile(n, n2);
    }

    public int getOriginalTile(int n, int n2) {
        return this.accessOriginalTile(n, n2);
    }

    public int getRawCellByte(int n, int n2) {
        if (!this.isInMap(n, n2)) {
            return 0;
        }
        return this.accessTile(n, n2) & 0xFF;
    }

    public boolean isInMap(NativeDeTerrainPoint nativeDeTerrainPoint) {
        return this.isInMap(nativeDeTerrainPoint.getX(), nativeDeTerrainPoint.getY());
    }

    public boolean isInMap(int n, int n2) {
        return this._worldTopLeftX <= n && n <= this._worldBottomRightX && this._worldTopLeftY <= n2 && n2 <= this._worldBottomRightY;
    }

    public boolean isPassable(NativeDeTerrainPoint nativeDeTerrainPoint, NativeDeCharacterFields nativeDeCharacterFields) {
        return this.isPassable(nativeDeTerrainPoint.getX(), nativeDeTerrainPoint.getY(), nativeDeCharacterFields);
    }

    public boolean isPassable(int n, int n2, NativeDeCharacterFields nativeDeCharacterFields) {
        return this.isPassable(n, n2 - 1, 4, nativeDeCharacterFields) || this.isPassable(n + 1, n2, 6, nativeDeCharacterFields) || this.isPassable(n, n2 + 1, 0, nativeDeCharacterFields) || this.isPassable(n - 1, n2, 2, nativeDeCharacterFields);
    }

    public boolean isPassable(NativeDeTerrainPoint nativeDeTerrainPoint, int n, NativeDeCharacterFields nativeDeCharacterFields) {
        return this.isPassable(nativeDeTerrainPoint.getX(), nativeDeTerrainPoint.getY(), n, nativeDeCharacterFields);
    }

    public static String describeStandingPassableFailure(NativeDeTerrainV1 nativeDeTerrainV1, int n, int n2) {
        if (nativeDeTerrainV1 == null || nativeDeTerrainV1.isNull()) {
            return "map=null";
        }
        if (!nativeDeTerrainV1.isInMap(n, n2)) {
            return "out_of_map";
        }
        int n3 = nativeDeTerrainV1.getRawCellByte(n, n2);
        if (n3 == 0) {
            return "empty_tile(raw=0)";
        }
        if ((n3 & 0xFFFFFF80) != 0) {
            return String.format("dynamic_0x80(raw=0x%02X)", n3);
        }
        if ((n3 & 3) == 0) {
            return String.format("no_walk_bits(raw=0x%02X orig=%d)", n3, nativeDeTerrainV1.getOriginalTile(n, n2));
        }
        return null;
    }

    public boolean isStandingPassable(int n, int n2) {
        if (!this.isInMap(n, n2)) {
            return false;
        }
        if (this.isClosedDoorAt(n, n2)) {
            return false;
        }
        int n3 = this.accessTile(n, n2) & 0xFF;
        if (n3 == 0 || (n3 & 0xFFFFFF80) != 0) {
            return false;
        }
        if ((n3 & 3) == 0) {
            return false;
        }
        return true;
    }

    public boolean isPassable(int n, int n2, int n3, NativeDeCharacterFields nativeDeCharacterFields) {
        if (n3 < 0 || n3 >= HEADING_TABLE_X.length) {
            return false;
        }
        try {
            int n4 = this.accessTile(n, n2);
            int n5 = n + HEADING_TABLE_X[n3];
            int n6 = n2 + HEADING_TABLE_Y[n3];
            int n7 = this.accessTile(n5, n6);
            if (this.isClosedDoorAt(n5, n6)) {
                return false;
            }
            if (!this.isStandingPassable(n5, n6)) {
                return false;
            }
            switch (n3) {
                case 0: {
                    return (n4 & 2) != 0;
                }
                case 2: {
                    return (n4 & 1) != 0;
                }
                case 4: {
                    return (n7 & 2) != 0;
                }
                case 6: {
                    return (n7 & 1) != 0;
                }
                case 1: 
                case 3: 
                case 5: 
                case 7: {
                    return (n7 & 3) != 0;
                }
            }
            return false;
        }
        catch (Exception exception) {
            _log.error("isPassable error", exception);
            return false;
        }
    }

    public boolean isPassableDna(int n, int n2, int n3) {
        if (n3 < 0 || n3 > 7) {
            return false;
        }
        return this.isPassable(n, n2, n3, null);
    }

    public void setPassable(NativeDeTerrainPoint nativeDeTerrainPoint, boolean bl) {
        this.setPassable(nativeDeTerrainPoint.getX(), nativeDeTerrainPoint.getY(), bl, 2);
    }

    public void setPassable(int n, int n2, boolean bl, int n3) {
        switch (n3) {
            case 0: {
                this.set_door_0(n, n2, bl);
                break;
            }
            case 1: {
                this.set_door_1(n, n2, bl);
                break;
            }
            default: { break; }
        }
    }

    private void set_door_0(int n, int n2, boolean bl) {
        try {
            if (bl) {
                this._doorOverrides.remove(this.getPointKey(n, n2));
                this._doorOverrides.remove(this.getPointKey(n - 1, n2));
                this._doorOverrides.remove(this.getPointKey(n + 1, n2));
            } else {
                this._doorOverrides.put(this.getPointKey(n, n2), (byte)3);
                this._doorOverrides.put(this.getPointKey(n - 1, n2), (byte)3);
                this._doorOverrides.put(this.getPointKey(n + 1, n2), (byte)3);
            }
        }
        catch (Exception exception) {
            _log.error("X:" + n + " Y:" + n2 + " MAP:" + this._mapId, exception);
        }
    }

    private void set_door_1(int n, int n2, boolean bl) {
        try {
            if (bl) {
                this._doorOverrides.remove(this.getPointKey(n, n2));
                this._doorOverrides.remove(this.getPointKey(n, n2 - 1));
                this._doorOverrides.remove(this.getPointKey(n, n2 + 1));
            } else {
                this._doorOverrides.put(this.getPointKey(n, n2), (byte)3);
                this._doorOverrides.put(this.getPointKey(n, n2 - 1), (byte)3);
                this._doorOverrides.put(this.getPointKey(n, n2 + 1), (byte)3);
            }
        }
        catch (Exception exception) {
            _log.error("X:" + n + " Y:" + n2 + " MAP:" + this._mapId, exception);
        }
    }

    public boolean isSafetyZone(NativeDeTerrainPoint nativeDeTerrainPoint) {
        return this.isSafetyZone(nativeDeTerrainPoint.getX(), nativeDeTerrainPoint.getY());
    }

    public boolean isSafetyZone(int n, int n2) {
        int n3 = this.accessOriginalTile(n, n2);
        return (n3 & 0x30) == 16;
    }

    public boolean isCombatZone(NativeDeTerrainPoint nativeDeTerrainPoint) {
        return this.isCombatZone(nativeDeTerrainPoint.getX(), nativeDeTerrainPoint.getY());
    }

    public boolean isCombatZone(int n, int n2) {
        int n3 = this.accessOriginalTile(n, n2);
        return (n3 & 0x30) == 32;
    }

    public boolean isNormalZone(NativeDeTerrainPoint nativeDeTerrainPoint) {
        return this.isNormalZone(nativeDeTerrainPoint.getX(), nativeDeTerrainPoint.getY());
    }

    public boolean isNormalZone(int n, int n2) {
        int n3 = this.accessOriginalTile(n, n2);
        return (n3 & 0x30) == 0;
    }

    public boolean isArrowPassable(NativeDeTerrainPoint nativeDeTerrainPoint) {
        return this.isArrowPassable(nativeDeTerrainPoint.getX(), nativeDeTerrainPoint.getY());
    }

    public boolean isArrowPassable(int n, int n2) {
        if (this.isClosedDoorAt(n, n2)) {
            return false;
        }
        if (this.isArrowPassableOverride(n, n2)) {
            return true;
        }
        return (this.accessOriginalTile(n, n2) & 0xC) != 0;
    }

    public boolean isArrowPassable(NativeDeTerrainPoint nativeDeTerrainPoint, int n) {
        return this.isArrowPassable(nativeDeTerrainPoint.getX(), nativeDeTerrainPoint.getY(), n);
    }

    public boolean isArrowPassable(int n, int n2, int n3) {
        try {
            int n4 = n + HEADING_TABLE_X[n3];
            int n5 = n2 + HEADING_TABLE_Y[n3];
            if (this.isClosedDoorAt(n4, n5)) {
                return false;
            }
            if (this.isArrowPassableOverride(n4, n5)) {
                return true;
            }
            int n6 = this.accessTile(n4, n5);
            switch (n6) {
                case 0: 
                case 3: {
                    return false;
                }
            }
            return (n6 & 0xC) != 0;
        }
        catch (Exception exception) {
            _log.error("isPassableNpc \u7570\u5e38", exception);
            return false;
        }
    }

    public boolean isUnderwater() {
        return this._isUnderwater;
    }

    public boolean isMarkable() {
        return this._isMarkable;
    }

    public boolean isTeleportable() {
        return this._isTeleportable;
    }

    public boolean isEscapable() {
        return this._isEscapable;
    }

    public boolean isUseResurrection() {
        return this._isUseResurrection;
    }

    public boolean isUsePainwand() {
        return this._isUsePainwand;
    }

    public boolean isEnabledDeathPenalty() {
        return this._isEnabledDeathPenalty;
    }

    public boolean isTakePets() {
        return this._isTakePets;
    }

    public boolean isRecallPets() {
        return this._isRecallPets;
    }

    public boolean isUsableItem() {
        return this._isUsableItem;
    }

    public boolean isUsableSkill() {
        return this._isUsableSkill;
    }

    public boolean isFishingZone(int n, int n2) {
        int n3 = this.accessOriginalTile(n, n2);
        return n3 == 28 || this._mapId == 5300 && n3 == 16;
    }

    public int isExistDoor(int n, int n2) {
        try {
            return this.accessTile(n, n2);
        }
        catch (Exception exception) {
            _log.error("isExistDoor \u7570\u5e38 X:" + n + " Y:" + n2 + " MAP:" + this._mapId, exception);
            return 0;
        }
    }

    public String toString(NativeDeTerrainPoint nativeDeTerrainPoint) {
        return "" + this.getOriginalTile(nativeDeTerrainPoint.getX(), nativeDeTerrainPoint.getY());
    }

    public boolean is_guaji() {
        return this._isGuaji;
    }

    public int isUsableShop() {
        return this._isUsableShop;
    }

    public boolean isUsableShop(int n, int n2) {
        return this._isUsableShop > 0 && NativeDeShopTable.get().checkShop(this._mapId, n, n2);
    }

    public boolean isNull() {
        return false;
    }
}


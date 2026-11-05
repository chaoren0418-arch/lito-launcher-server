// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

public class NativeDeShopData {
    private int _mapId;
    private int _xMin;
    private int _xMax;
    private int _yMin;
    private int _yMax;
    private int _currency;

    public NativeDeShopData(int n, int n2, int n3, int n4, int n5, int n6) {
        this._mapId = n;
        this._xMin = Math.min(n2, n3);
        this._xMax = Math.max(n2, n3);
        this._yMin = Math.min(n4, n5);
        this._yMax = Math.max(n4, n5);
        this._currency = n6;
    }

    public int getMapId() {
        return this._mapId;
    }

    public int getXMin() {
        return this._xMin;
    }

    public int getXMax() {
        return this._xMax;
    }

    public int getYMin() {
        return this._yMin;
    }

    public int getYMax() {
        return this._yMax;
    }

    public int getCurrency() {
        return this._currency;
    }

    public boolean isInRange(int n, int n2) {
        if (this._xMin == 0 && this._xMax == 0 && this._yMin == 0 && this._yMax == 0) {
            return true;
        }
        if (n < this._xMin || n > this._xMax) {
            return false;
        }
        if (n2 < this._yMin || n2 > this._yMax) {
            return false;
        }
        return true;
    }
}

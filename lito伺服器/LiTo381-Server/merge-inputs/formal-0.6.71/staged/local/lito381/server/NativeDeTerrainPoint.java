// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

public class NativeDeTerrainPoint {
    protected volatile int _x = 0;
    protected volatile int _y = 0;
    private static final int[] HEADING_TABLE_X = new int[]{0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] HEADING_TABLE_Y = new int[]{-1, -1, 0, 1, 1, 1, 0, -1};

    public NativeDeTerrainPoint() {
    }

    public NativeDeTerrainPoint(int n, int n2) {
        this._x = n;
        this._y = n2;
    }

    public NativeDeTerrainPoint(NativeDeTerrainPoint nativeDeTerrainPoint) {
        this._x = nativeDeTerrainPoint._x;
        this._y = nativeDeTerrainPoint._y;
    }

    public int getX() {
        return this._x;
    }

    public void setX(int n) {
        this._x = n;
    }

    public int getY() {
        return this._y;
    }

    public void setY(int n) {
        this._y = n;
    }

    public void set(NativeDeTerrainPoint nativeDeTerrainPoint) {
        this._x = nativeDeTerrainPoint._x;
        this._y = nativeDeTerrainPoint._y;
    }

    public void set(int n, int n2) {
        this._x = n;
        this._y = n2;
    }

    public void forward(int n) {
        this._x += HEADING_TABLE_X[n];
        this._y += HEADING_TABLE_Y[n];
    }

    public void backward(int n) {
        this._x -= HEADING_TABLE_X[n];
        this._y -= HEADING_TABLE_Y[n];
    }

    public double getLineDistance(NativeDeTerrainPoint nativeDeTerrainPoint) {
        long l = nativeDeTerrainPoint.getX() - this.getX();
        long l2 = nativeDeTerrainPoint.getY() - this.getY();
        return Math.sqrt(l * l + l2 * l2);
    }

    public double getLineDistance(int n, int n2) {
        long l = n - this.getX();
        long l2 = n2 - this.getY();
        return Math.sqrt(l * l + l2 * l2);
    }

    public int getTileLineDistance(NativeDeTerrainPoint nativeDeTerrainPoint) {
        return Math.max(Math.abs(nativeDeTerrainPoint.getX() - this.getX()), Math.abs(nativeDeTerrainPoint.getY() - this.getY()));
    }

    public int getTileDistance(NativeDeTerrainPoint nativeDeTerrainPoint) {
        return Math.abs(nativeDeTerrainPoint.getX() - this.getX()) + Math.abs(nativeDeTerrainPoint.getY() - this.getY());
    }

    public boolean isInScreen(NativeDeTerrainPoint nativeDeTerrainPoint) {
        int n = this.getTileDistance(nativeDeTerrainPoint);
        if (n > 19) {
            return false;
        }
        if (n <= 18) {
            return true;
        }
        int n2 = Math.abs(nativeDeTerrainPoint.getX() - (this.getX() - 18)) + Math.abs(nativeDeTerrainPoint.getY() - (this.getY() - 18));
        if (19 <= n2 && n2 <= 52) {
            return true;
        }
        return false;
    }

    public boolean isSamePoint(NativeDeTerrainPoint nativeDeTerrainPoint) {
        return nativeDeTerrainPoint.getX() == this.getX() && nativeDeTerrainPoint.getY() == this.getY();
    }

    public int hashCode() {
        return 7 * this.getX() + this.getY();
    }

    public boolean equals(Object object) {
        if (!(object instanceof NativeDeTerrainPoint)) {
            return false;
        }
        NativeDeTerrainPoint nativeDeTerrainPoint = (NativeDeTerrainPoint)object;
        return this.getX() == nativeDeTerrainPoint.getX() && this.getY() == nativeDeTerrainPoint.getY();
    }

    public String toString() {
        return String.format("(%d, %d)", this._x, this._y);
    }
}


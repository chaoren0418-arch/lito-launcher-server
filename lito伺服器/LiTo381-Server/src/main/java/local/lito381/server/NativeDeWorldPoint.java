// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

public class NativeDeWorldPoint {
    protected volatile int _x = 0;
    protected volatile int _y = 0;
    private static final int[] HEADING_TABLE_X = new int[]{0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] HEADING_TABLE_Y = new int[]{-1, -1, 0, 1, 1, 1, 0, -1};

    public NativeDeWorldPoint() {
    }

    public NativeDeWorldPoint(int x, int y) {
        this._x = x;
        this._y = y;
    }

    public NativeDeWorldPoint(NativeDeWorldPoint pt) {
        this._x = pt._x;
        this._y = pt._y;
    }

    public int getX() {
        return this._x;
    }

    public void setX(int x) {
        this._x = x;
    }

    public int getY() {
        return this._y;
    }

    public void setY(int y) {
        this._y = y;
    }

    public void set(NativeDeWorldPoint pt) {
        this._x = pt._x;
        this._y = pt._y;
    }

    public void set(int x, int y) {
        this._x = x;
        this._y = y;
    }

    public void forward(int heading) {
        this._x += HEADING_TABLE_X[heading];
        this._y += HEADING_TABLE_Y[heading];
    }

    public void backward(int heading) {
        this._x -= HEADING_TABLE_X[heading];
        this._y -= HEADING_TABLE_Y[heading];
    }

    public double getLineDistance(NativeDeWorldPoint pt) {
        long diffX = pt.getX() - this.getX();
        long diffY = pt.getY() - this.getY();
        return Math.sqrt(diffX * diffX + diffY * diffY);
    }

    public double getLineDistance(int x, int y) {
        long diffX = x - this.getX();
        long diffY = y - this.getY();
        return Math.sqrt(diffX * diffX + diffY * diffY);
    }

    public int getTileLineDistance(NativeDeWorldPoint pt) {
        return Math.max(Math.abs(pt.getX() - this.getX()), Math.abs(pt.getY() - this.getY()));
    }

    public int getTileDistance(NativeDeWorldPoint pt) {
        return Math.abs(pt.getX() - this.getX()) + Math.abs(pt.getY() - this.getY());
    }

    public boolean isInScreen(NativeDeWorldPoint pt) {
        int dist = this.getTileDistance(pt);
        if (dist > 19) {
            return false;
        }
        if (dist <= 18) {
            return true;
        }
        int dist2 = Math.abs(pt.getX() - (this.getX() - 18)) + Math.abs(pt.getY() - (this.getY() - 18));
        return 19 <= dist2 && dist2 <= 52;
    }

    public boolean isSamePoint(NativeDeWorldPoint pt) {
        return pt.getX() == this.getX() && pt.getY() == this.getY();
    }

    public int hashCode() {
        return 7 * this.getX() + this.getY();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof NativeDeWorldPoint)) {
            return false;
        }
        NativeDeWorldPoint pt = (NativeDeWorldPoint)obj;
        return this.getX() == pt.getX() && this.getY() == pt.getY();
    }

    public String toString() {
        return String.format("(%d, %d)", this._x, this._y);
    }
}

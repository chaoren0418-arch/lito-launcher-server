// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

public abstract class NativeDeNpcMoveExecutor {
    protected static final byte[] HEADING_TABLE_X = new byte[]{0, 1, 1, 1, 0, -1, -1, -1};
    protected static final byte[] HEADING_TABLE_Y = new byte[]{-1, -1, 0, 1, 1, 1, 0, -1};
    protected static final byte[] HEADING_TABLE_XR = new byte[]{0, -1, -1, -1, 0, 1, 1, 1};
    protected static final byte[] HEADING_TABLE_YR = new byte[]{1, 1, 0, -1, -1, -1, 0, 1};
    protected static final byte[] HEADING_RD = new byte[]{4, 5, 6, 7, 0, 1, 2, 3};
    protected static final int[][] _ary1 = new int[][]{{7, 1, 0, 6, 2}, {7, 3, 1, 0, 2}, {3, 1, 0, 4, 2}, {5, 3, 1, 4, 2}, {5, 3, 6, 4, 2}, {7, 5, 3, 6, 4}, {7, 5, 0, 6, 4}, {7, 5, 1, 0, 6}};
    protected static final int[] _heading2 = new int[]{7, 0, 1, 2, 3, 4, 5, 6};
    protected static final int[] _heading3 = new int[]{1, 2, 3, 4, 5, 6, 7, 0};
    protected static final int[] _heading4 = new int[]{2, 3, 4, 5, 6, 7, 0, 1};
    protected static final int[] _heading5 = new int[]{6, 7, 0, 1, 2, 3, 4, 5};
    protected static final int[] _heading6 = new int[]{5, 6, 7, 0, 1, 2, 3, 4};
    protected static final int[] _heading7 = new int[]{3, 4, 5, 6, 7, 0, 1, 2};
    protected static final int[] _heading8 = new int[]{4, 5, 6, 7, 0, 1, 2, 3};

    public abstract void setDirectionMove(int var1);

    public abstract int moveDirection(int var1, int var2);

    public abstract int targetReverseDirection(int var1, int var2);

    public abstract int checkObject(int var1);

    public abstract int openDoor(int var1);

    public abstract void clear();

    public abstract boolean wasLastPathUnreachable();
}

// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

public final class SharedPresentationPackets {
    private SharedPresentationPackets() {
    }

    public static byte[] bluePotion(int n) {
        if (n <= 0 || n > Short.MAX_VALUE) {
            throw new IllegalArgumentException("blue potion duration");
        }
        return new byte[]{123, 34, (byte)n, (byte)(n >>> 8)};
    }
}

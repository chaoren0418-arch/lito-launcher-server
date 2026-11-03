// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

record ClanEmblemData(long emblemId, byte[] pixels) {
    private final byte[] pixels;

    ClanEmblemData {
        if (emblemId <= 0L || emblemId > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("clan emblem id is outside native dword range");
        }
        if (pixels == null || pixels.length != 384) {
            throw new IllegalArgumentException("clan emblem must contain exactly 384 bytes");
        }
        pixels = (byte[])pixels.clone();
    }

    public byte[] pixels() {
        return (byte[])this.pixels.clone();
    }
}

package local.lito381.server;

/** Presentation-only packets for the paired shared-resource LiTo client. */
public final class SharedPresentationPackets {
    private SharedPresentationPackets() {}
    public static byte[] bluePotion(int seconds) {
        if (seconds<=0 || seconds>32767) throw new IllegalArgumentException("blue potion duration");
        return new byte[]{0x7B,34,(byte)seconds,(byte)(seconds>>>8)};
    }
}

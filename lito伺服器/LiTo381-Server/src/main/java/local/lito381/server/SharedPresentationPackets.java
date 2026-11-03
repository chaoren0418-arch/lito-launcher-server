// ORIGINAL SOURCE moved from lito登入器/LiTo381-Launcher/tools/world-resource-compat/SharedPresentationPackets.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;

/** Presentation-only packets for the paired shared-resource LiTo client. */
public final class SharedPresentationPackets {
    private SharedPresentationPackets() {}
    public static byte[] bluePotion(int seconds) {
        if (seconds<=0 || seconds>32767) throw new IllegalArgumentException("blue potion duration");
        return new byte[]{0x7B,34,(byte)seconds,(byte)(seconds>>>8)};
    }
}

package local.lito381.server;

import java.util.Arrays;

/** LiTo sub_4A3990 reads an additional byte and dword for status-mask PC actors. */
final class NativeActorTail {
    static byte[] complete(byte[] commonBody, int status) {
        if ((status & 4) == 0) return commonBody;
        byte[] packet = Arrays.copyOf(commonBody, commonBody.length + 5);
        // No native PC category override and no extra character marker.
        // Omitting these fields lets the native parser read unrelated packet-buffer bytes.
        packet[commonBody.length] = (byte) 0xff;
        return packet;
    }
}

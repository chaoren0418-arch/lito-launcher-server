package local.lito381.server;

/** Exact LiTo 3.81C S_EVENT/33 -> C_ADDR integrity contract. */
final class NativeClientIntegrity {
    static final long RANGE_START = 0x004E_D840L;
    static final long RANGE_LENGTH = 5;
    static final long CRC_SEED = 0xD883_C01BL;
    static final long EXPECTED_CHECKSUM = 0x1569_D958L;
    static final long RESPONSE_TIMEOUT_NANOS = 30_000_000_000L;

    // sub_4ED840 is a fixed-base call/pop image-anchor helper.  The requested
    // five-byte range is the relocation-free `call $+5` instruction itself.
    private static final byte[] CANONICAL_RANGE = {
            (byte) 0xE8, 0x00, 0x00, 0x00, 0x00
    };
    private static final int[] CRC_TABLE = crcTable();

    static {
        if (seededCrc(CANONICAL_RANGE, CRC_SEED) != EXPECTED_CHECKSUM) {
            throw new ExceptionInInitializerError("LiTo native integrity fixture mismatch");
        }
    }

    private NativeClientIntegrity() {}

    static long seededCrc(byte[] bytes, long seed) {
        int crc = ~((int) seed);
        for (byte value : bytes) {
            crc = CRC_TABLE[(crc ^ value) & 0xFF] ^ (crc >>> 8);
        }
        return Integer.toUnsignedLong(~crc);
    }

    static boolean checksumMatches(long checksum) {
        return checksum == EXPECTED_CHECKSUM;
    }

    private static int[] crcTable() {
        int[] result = new int[256];
        for (int index = 0; index < result.length; index++) {
            int value = index;
            for (int bit = 0; bit < 8; bit++) {
                value = (value & 1) != 0 ? 0xEDB8_8320 ^ (value >>> 1) : value >>> 1;
            }
            result[index] = value;
        }
        return result;
    }
}

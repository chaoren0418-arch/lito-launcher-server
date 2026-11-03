package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

final class ProtocolLitoBuddyErrorDomainTest {
    @Test void nativeDomainIsExactlyAlreadyRegisteredAndListFull() {
        assertArrayEquals(new int[] {0x12, 0x13},
                Arrays.stream(Protocol.LitoBuddyError.values())
                        .mapToInt(Protocol.LitoBuddyError::wireValue).toArray());
        assertEquals(Protocol.LitoBuddyError.ALREADY_REGISTERED,
                Protocol.LitoBuddyError.fromWire(0x12));
        assertEquals(Protocol.LitoBuddyError.LIST_FULL,
                Protocol.LitoBuddyError.fromWire(0x13));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.LitoBuddyError.fromWire(0x11));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.LitoBuddyError.fromWire(0x14));
    }

    @Test void writerEmitsEveryTypedResultExactly() {
        assertArrayEquals(new byte[] {(byte) Protocol.S_LITO_BUDDY_ERROR, 0x12},
                Protocol.buddyError(Protocol.LitoBuddyError.ALREADY_REGISTERED));
        assertArrayEquals(new byte[] {(byte) Protocol.S_LITO_BUDDY_ERROR, 0x13},
                Protocol.buddyError(Protocol.LitoBuddyError.LIST_FULL));
        assertThrows(NullPointerException.class, () -> Protocol.buddyError(null));
    }

    @Test void productionWriterExposesNoRawIntegerOverloadOrConstants() {
        assertThrows(NoSuchMethodException.class,
                () -> Protocol.class.getDeclaredMethod("buddyError", int.class));
        assertThrows(NoSuchFieldException.class,
                () -> Protocol.class.getDeclaredField("BUDDY_ALREADY_REGISTERED"));
        assertThrows(NoSuchFieldException.class,
                () -> Protocol.class.getDeclaredField("BUDDY_LIST_FULL"));
    }

    @Test void wireValuesSelectTheTwoNativeLtstringPointers() {
        assertEquals(0xCF,
                Protocol.LitoBuddyError.ALREADY_REGISTERED.wireValue() + 0xBD);
        assertEquals(0xD0,
                Protocol.LitoBuddyError.LIST_FULL.wireValue() + 0xBD);
    }
}

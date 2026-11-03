package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

final class ProtocolLitoBuddyActivityDomainTest {
    @Test
    void exactNativeThreeStateDomainIsTyped() {
        assertArrayEquals(new int[] {0, 1, 2},
                Arrays.stream(Protocol.LitoBuddyActivity.values())
                        .mapToInt(Protocol.LitoBuddyActivity::wireValue).toArray());
        assertEquals(Protocol.LitoBuddyActivity.WAITING,
                Protocol.LitoBuddyActivity.fromWire(0));
        assertEquals(Protocol.LitoBuddyActivity.FIGHTING,
                Protocol.LitoBuddyActivity.fromWire(1));
        assertEquals(Protocol.LitoBuddyActivity.OFFLINE,
                Protocol.LitoBuddyActivity.fromWire(2));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.LitoBuddyActivity.fromWire(-1));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.LitoBuddyActivity.fromWire(3));
    }

    @Test
    void rawIntegerStatusWriterIsNotExposed() throws Exception {
        assertThrows(NoSuchMethodException.class,
                () -> Protocol.class.getDeclaredMethod(
                        "buddyStatus", String.class, int.class));
        assertArrayEquals(new Protocol.Writer()
                        .c(Protocol.S_LITO_BUDDY_CHANGE_STATUS)
                        .s("TestMage").c(1).bytes(),
                Protocol.buddyStatus(
                        "TestMage", Protocol.LitoBuddyActivity.FIGHTING));
        assertThrows(NullPointerException.class,
                () -> Protocol.buddyStatus("TestMage", null));
    }
}

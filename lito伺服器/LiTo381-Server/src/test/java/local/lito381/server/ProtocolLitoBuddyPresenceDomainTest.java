package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

final class ProtocolLitoBuddyPresenceDomainTest {
    @Test
    void canonicalDomainIsExactlyOnlineZeroAndOfflineOne() {
        assertEquals(Protocol.LitoBuddyPresence.ONLINE,
                Protocol.LitoBuddyPresence.fromWire(0));
        assertEquals(Protocol.LitoBuddyPresence.OFFLINE,
                Protocol.LitoBuddyPresence.fromWire(1));
        assertEquals(0, Protocol.LitoBuddyPresence.ONLINE.wireValue());
        assertEquals(1, Protocol.LitoBuddyPresence.OFFLINE.wireValue());
        assertEquals(2, Protocol.LitoBuddyPresence.values().length);

        for (int value = 2; value <= 0xFF; value++) {
            int rejected = value;
            assertThrows(IllegalArgumentException.class,
                    () -> Protocol.LitoBuddyPresence.fromWire(rejected));
        }
    }

    @Test
    void typedWriterEmitsExactNativePackets() {
        assertArrayEquals(new Protocol.Writer().c(Protocol.S_LITO_BUDDY_LOGIN)
                        .s("TestMage").c(0).bytes(),
                Protocol.buddyLogin("TestMage", Protocol.LitoBuddyPresence.ONLINE));
        assertArrayEquals(new Protocol.Writer().c(Protocol.S_LITO_BUDDY_LOGIN)
                        .s("TestMage").c(1).bytes(),
                Protocol.buddyLogin("TestMage", Protocol.LitoBuddyPresence.OFFLINE));
        assertThrows(NullPointerException.class,
                () -> Protocol.buddyLogin("TestMage", null));
    }

    @Test
    void writerHasNoBooleanOrIntegerPresenceOverload() {
        Method[] writers = Arrays.stream(Protocol.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("buddyLogin"))
                .toArray(Method[]::new);
        assertEquals(1, writers.length);
        assertArrayEquals(new Class<?>[] {String.class, Protocol.LitoBuddyPresence.class},
                writers[0].getParameterTypes());
    }
}

package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ProtocolModerationTest {
    @Test void parsesExactNativeBanExcludeKickAndIncludeFrames() {
        assertEquals("TestMage", Protocol.namedCommandRequest(
                new Protocol.Writer().c(Protocol.C_BAN).s("TestMage").bytes(),
                Protocol.C_BAN, false));
        assertEquals("TestMage", Protocol.namedCommandRequest(
                new Protocol.Writer().c(Protocol.C_KICK).s("TestMage").bytes(),
                Protocol.C_KICK, false));
        assertEquals("TestMage", Protocol.namedCommandRequest(
                new Protocol.Writer().c(Protocol.C_EXCLUDE).s("TestMage").bytes(),
                Protocol.C_EXCLUDE, true));
        assertEquals("", Protocol.namedCommandRequest(
                new Protocol.Writer().c(Protocol.C_EXCLUDE).s("").bytes(),
                Protocol.C_EXCLUDE, true));
        assertEquals("TestMage", Protocol.namedCommandRequest(
                new Protocol.Writer().c(Protocol.C_INCLUDE).s("TestMage").bytes(),
                Protocol.C_INCLUDE, false));
    }

    @Test void rejectsMalformedOrOverlongNamedCommandFrames() {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.namedCommandRequest(
                        new Protocol.Writer().c(Protocol.C_KICK).s("").bytes(),
                        Protocol.C_KICK, false));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.namedCommandRequest(
                        new Protocol.Writer().c(Protocol.C_INCLUDE).s("X".repeat(28)).bytes(),
                        Protocol.C_INCLUDE, false));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.namedCommandRequest(
                        new Protocol.Writer().c(Protocol.C_BAN).s("TestMage").c(1).bytes(),
                        Protocol.C_BAN, false));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.namedCommandRequest(
                        new Protocol.Writer().c(Protocol.C_EXCLUDE).s("line\nbreak").bytes(),
                        Protocol.C_EXCLUDE, true));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.namedCommandRequest(
                        new Protocol.Writer().c(Protocol.C_BAN).s("TestMage").bytes(),
                        Protocol.C_KICK, false));
    }
}

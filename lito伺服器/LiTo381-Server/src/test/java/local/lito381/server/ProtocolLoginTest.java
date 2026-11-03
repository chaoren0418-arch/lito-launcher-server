package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ProtocolLoginTest {
    @Test
    void parsesExactDirectCssdEnvelope() {
        Protocol.LoginRequest request = Protocol.loginRequest(new Protocol.Writer()
                .c(Protocol.C_LOGIN).s("Native01").s("Passcode1")
                .d(0x0100007FL).bytes());

        assertEquals("Native01", request.account());
        assertEquals("Passcode1", request.password());
        assertEquals(0x0100007FL, request.clientAddress());
        assertEquals("", request.connector());
        assertEquals(Protocol.LoginLayout.DIRECT, request.layout());
        assertFalse(request.hasConnector());
    }

    @Test
    void parsesExactConnectorCssdsEnvelopeWithoutTrustingMetadata() {
        Protocol.LoginRequest request = Protocol.loginRequest(new Protocol.Writer()
                .c(Protocol.C_LOGIN).s("Native01").s("Passcode1")
                .d(0x0100007FL).s("lin.bin /n:connector").bytes());

        assertEquals("Native01", request.account());
        assertEquals("Passcode1", request.password());
        assertEquals(0x0100007FL, request.clientAddress());
        assertEquals("lin.bin /n:connector", request.connector());
        assertEquals(Protocol.LoginLayout.CONNECTOR, request.layout());
        assertTrue(request.hasConnector());
    }

    @Test
    void rejectsUnderflowTrailingBytesAndConnectorBeyondNativeBuffer() {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.loginRequest(new Protocol.Writer()
                        .c(Protocol.C_LOGIN).s("Native01").s("Passcode1").bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.loginRequest(new Protocol.Writer()
                        .c(Protocol.C_LOGIN).s("Native01").s("Passcode1")
                        .d(0).s("connector").c(1).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.loginRequest(new Protocol.Writer()
                        .c(Protocol.C_LOGIN).s("Native01").s("Passcode1")
                        .d(0).s("x".repeat(32)).bytes()));
    }
}

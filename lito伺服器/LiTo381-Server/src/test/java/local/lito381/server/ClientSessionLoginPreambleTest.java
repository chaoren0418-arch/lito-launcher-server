package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ClientSessionLoginPreambleTest {
    @Test
    void recognizesOnlyBoundedGameEndpointHandoff() {
        assertTrue(ClientSession.isNativeGameLoginPreamble(
                ClientSession.Endpoint.GAME, "a".repeat(174), "p".repeat(13)));

        assertFalse(ClientSession.isNativeGameLoginPreamble(
                ClientSession.Endpoint.LOGIN, "a".repeat(174), "p".repeat(13)));
        assertFalse(ClientSession.isNativeGameLoginPreamble(
                ClientSession.Endpoint.GAME, "a".repeat(64), "p".repeat(13)));
        assertFalse(ClientSession.isNativeGameLoginPreamble(
                ClientSession.Endpoint.GAME, "a".repeat(193), "p".repeat(13)));
        assertFalse(ClientSession.isNativeGameLoginPreamble(
                ClientSession.Endpoint.GAME, "a".repeat(174), ""));
        assertFalse(ClientSession.isNativeGameLoginPreamble(
                ClientSession.Endpoint.GAME, "a".repeat(174), "p".repeat(513)));
    }

    @Test
    void ordinaryCredentialShapeIsNeverClassifiedAsHandoff() {
        assertFalse(ClientSession.isNativeGameLoginPreamble(
                ClientSession.Endpoint.GAME, "test", "test"));
    }
}

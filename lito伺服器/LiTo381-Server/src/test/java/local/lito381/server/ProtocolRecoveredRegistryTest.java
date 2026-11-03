package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ProtocolRecoveredRegistryTest {
    @Test
    void acceptsRecoveredOpcodeOnlyLogoutAndNewsCallbacks() {
        Protocol.logout(new byte[] {(byte) Protocol.C_LOGOUT});
        Protocol.readNews(new byte[] {(byte) Protocol.C_READ_NEWS});
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.logout(new byte[] {(byte) Protocol.C_LOGOUT, 0}));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.readNews(new byte[] {(byte) Protocol.C_READ_NEWS, 0}));
    }

    @Test
    void parsesRecoveredBoardDeleteCdd() {
        Protocol.BoardDeleteRequest request = Protocol.boardDeleteRequest(
                new Protocol.Writer().c(Protocol.C_BOARD_DELETE)
                        .d(0x1122_3344L).d(7).bytes());
        assertEquals(0x1122_3344L, request.boardObjectId());
        assertEquals(7, request.topicId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.boardDeleteRequest(new Protocol.Writer()
                        .c(Protocol.C_BOARD_DELETE).d(0x1122_3344L).d(0).bytes()));
    }

    @Test
    void parsesBothRecoveredDeleteCharacterBranches() {
        Protocol.ClassicCharacterRequest plain = Protocol.classicCharacterRequest(
                new Protocol.Writer().c(Protocol.C_DELETE_CHARACTER)
                        .s("NativeHero").bytes(), Protocol.C_DELETE_CHARACTER);
        assertEquals("NativeHero", plain.name());
        assertFalse(plain.hasNativeDeleteValue());

        Protocol.ClassicCharacterRequest extended = Protocol.classicCharacterRequest(
                new Protocol.Writer().c(Protocol.C_DELETE_CHARACTER)
                        .s("NativeHero").d(0x1020_3040L).bytes(),
                Protocol.C_DELETE_CHARACTER);
        assertEquals("NativeHero", extended.name());
        assertTrue(extended.hasNativeDeleteValue());
        assertEquals(0x1020_3040L, extended.nativeDeleteValue());

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.classicCharacterRequest(new Protocol.Writer()
                        .c(Protocol.C_ENTER_WORLD).s("NativeHero").d(1).bytes(),
                        Protocol.C_ENTER_WORLD));
    }
}

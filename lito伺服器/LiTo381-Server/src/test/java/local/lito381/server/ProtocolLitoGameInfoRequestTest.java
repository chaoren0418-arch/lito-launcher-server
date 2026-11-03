package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ProtocolLitoGameInfoRequestTest {
    @Test void parsesBothNativeModeStepDirections() {
        assertEquals(new Protocol.LitoModeStepRequest(
                        Protocol.LitoModeStepDirection.NEXT),
                Protocol.litoGameInfoRequest(new byte[] {(byte) 0xD1, 2, 1}));
        assertEquals(new Protocol.LitoModeStepRequest(
                        Protocol.LitoModeStepDirection.PREVIOUS),
                Protocol.litoGameInfoRequest(new byte[] {(byte) 0xD1, 2, 2}));
    }

    @Test void parsesTheFullUnsignedNativeMapWord() {
        assertEquals(new Protocol.LitoMapSelectionRequest(0),
                Protocol.litoGameInfoRequest(new byte[] {(byte) 0xD1, 3, 0, 0}));
        assertEquals(new Protocol.LitoMapSelectionRequest(0xFFFF),
                Protocol.litoGameInfoRequest(new byte[] {
                        (byte) 0xD1, 3, (byte) 0xFF, (byte) 0xFF
                }));
    }

    @Test void rejectsOpcodeOnlyUnknownAndInvalidModeForms() {
        for (byte[] payload : new byte[][] {
                {(byte) 0xD1},
                {(byte) 0xD1, 1},
                {(byte) 0xD1, 2},
                {(byte) 0xD1, 2, 0},
                {(byte) 0xD1, 2, 3},
                {(byte) 0xD1, 2, 1, 0},
                {(byte) 0xD1, 4}
        }) {
            assertThrows(IllegalArgumentException.class,
                    () -> Protocol.litoGameInfoRequest(payload));
        }
    }

    @Test void rejectsPartialTrailingAndWrongOpcodeMapForms() {
        for (byte[] payload : new byte[][] {
                {(byte) 0xD1, 3},
                {(byte) 0xD1, 3, 1},
                {(byte) 0xD1, 3, 1, 2, 3},
                {(byte) 0xD0, 3, 1, 2}
        }) {
            assertThrows(IllegalArgumentException.class,
                    () -> Protocol.litoGameInfoRequest(payload));
        }
    }
}

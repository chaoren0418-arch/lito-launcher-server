package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

final class ProtocolLitoModeStepDirectionDomainTest {
    @Test void nativeDomainIsExactlyRightNextAndLeftPrevious() {
        assertEquals(List.of(Protocol.LitoModeStepDirection.NEXT,
                        Protocol.LitoModeStepDirection.PREVIOUS),
                Arrays.asList(Protocol.LitoModeStepDirection.values()));
        assertEquals(List.of(1, 2), Arrays.stream(Protocol.LitoModeStepDirection.values())
                .map(Protocol.LitoModeStepDirection::wireValue).toList());
        assertEquals(Protocol.LitoModeStepDirection.NEXT,
                Protocol.LitoModeStepDirection.fromWire(1));
        assertEquals(Protocol.LitoModeStepDirection.PREVIOUS,
                Protocol.LitoModeStepDirection.fromWire(2));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.LitoModeStepDirection.fromWire(0));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.LitoModeStepDirection.fromWire(3));
    }

    @Test void requestUnionCannotConfuseDirectionWithMapId() {
        assertThrows(NullPointerException.class,
                () -> new Protocol.LitoModeStepRequest(null));
        assertThrows(IllegalArgumentException.class,
                () -> new Protocol.LitoMapSelectionRequest(-1));
        assertThrows(IllegalArgumentException.class,
                () -> new Protocol.LitoMapSelectionRequest(0x1_0000));
    }

    @Test void productionBoundaryExposesNoRawValueOrIntegerStepOverload() throws Exception {
        assertThrows(NoSuchMethodException.class,
                () -> Protocol.LitoGameInfoRequest.class.getDeclaredMethod("value"));
        assertThrows(NoSuchMethodException.class,
                () -> BattleRules.Mode.class.getDeclaredMethod(
                        "stepNative", int.class, int.class));
        assertNotNull(BattleRules.Mode.class.getDeclaredMethod(
                "stepNative", int.class, Protocol.LitoModeStepDirection.class));
    }
}

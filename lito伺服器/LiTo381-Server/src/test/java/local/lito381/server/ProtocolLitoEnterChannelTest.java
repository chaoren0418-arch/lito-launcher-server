package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

import org.junit.jupiter.api.Test;

final class ProtocolLitoEnterChannelTest {
    private static final Set<Integer> NATIVE_VALUES = Set.of(0, 1, 2, 3, 99, 100);

    @Test void exactEnumAndParserCoverOnlyTheCompleteNativeProducerDomain() {
        assertEquals(NATIVE_VALUES.size(), Protocol.LitoChannelSelection.values().length);
        for (Protocol.LitoChannelSelection selection
                : Protocol.LitoChannelSelection.values()) {
            assertTrue(NATIVE_VALUES.contains(selection.wireValue()));
            assertEquals(selection, Protocol.litoEnterChannelRequest(
                    bytes(0xCA, selection.wireValue())));
        }
        for (int value = 0; value <= 0xFF; value++) {
            if (NATIVE_VALUES.contains(value)) continue;
            int rejected = value;
            assertThrows(IllegalArgumentException.class,
                    () -> Protocol.litoEnterChannelRequest(bytes(0xCA, rejected)));
        }
    }

    @Test void classifiesFourChannelsArgoAndOneWaySelectorReset() {
        for (int channel = 0; channel < 4; channel++) {
            Protocol.LitoChannelSelection selection =
                    Protocol.litoEnterChannelRequest(bytes(0xCA, channel));
            assertEquals(Protocol.LitoChannelTarget.CHANNEL, selection.target());
            assertEquals(channel, selection.wireValue());
        }
        assertEquals(Protocol.LitoChannelTarget.ARGO,
                Protocol.LitoChannelSelection.ARGO.target());
        assertEquals(Protocol.LitoChannelTarget.CHANNEL_SELECTION,
                Protocol.LitoChannelSelection.CHANNEL_SELECTION.target());
    }

    @Test void rejectsOpcodeOnlyPartialTrailingAndWrongOpcodeLayouts() {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoEnterChannelRequest(bytes(0xCA)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoEnterChannelRequest(bytes(0xCA, 0, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoEnterChannelRequest(bytes(0xC9, 0)));
    }

    @Test void emitsExactTypedNativeSuccessFailureMalformedAndCounterShapes() {
        assertArrayEquals(bytes(0xC9, 0, 3, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0),
                Protocol.litoEnterChannelSuccess(
                        Protocol.LitoChannelSelection.CHANNEL_3));
        assertArrayEquals(bytes(0xC9, 1, 3),
                Protocol.litoEnterChannelFailure(
                        Protocol.LitoEnterChannelFailure.CHANNEL_NOT_IN_SERVICE,
                        Protocol.LitoChannelSelection.CHANNEL_3));
        assertArrayEquals(bytes(0xC9, 10, 2),
                Protocol.litoEnterChannelFailure(
                        Protocol.LitoEnterChannelFailure.CHARACTER_LEVEL_RESTRICTED,
                        Protocol.LitoChannelSelection.CHANNEL_2));
        assertArrayEquals(bytes(0xC9, 1, 0),
                Protocol.litoMalformedEnterChannelFailure());
        assertArrayEquals(bytes(0xCA, 99, 2, 0, 0x90, 1),
                Protocol.litoChannelInfo(
                        Protocol.LitoChannelSelection.ARGO, 2, 400));
    }

    @Test void rejectsNullSelectorSentinelAndImpossibleFailureCombination() {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoEnterChannelSuccess(null));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoEnterChannelSuccess(
                        Protocol.LitoChannelSelection.CHANNEL_SELECTION));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoEnterChannelFailure(null,
                        Protocol.LitoChannelSelection.CHANNEL_0));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoEnterChannelFailure(
                        Protocol.LitoEnterChannelFailure.CHANNEL_NOT_IN_SERVICE,
                        Protocol.LitoChannelSelection.CHANNEL_SELECTION));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoEnterChannelFailure(
                        Protocol.LitoEnterChannelFailure.CHARACTER_LEVEL_RESTRICTED,
                        Protocol.LitoChannelSelection.ARGO));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoChannelInfo(
                        Protocol.LitoChannelSelection.CHANNEL_SELECTION, 1, 100));
    }

    @Test void wireBoundaryHasNoRawIntegerSelectionOverload() throws Exception {
        Method parser = Protocol.class.getDeclaredMethod(
                "litoEnterChannelRequest", byte[].class);
        assertEquals(Protocol.LitoChannelSelection.class, parser.getReturnType());

        Method success = Protocol.class.getDeclaredMethod(
                "litoEnterChannelSuccess", Protocol.LitoChannelSelection.class);
        assertEquals(byte[].class, success.getReturnType());
        Method failure = Protocol.class.getDeclaredMethod(
                "litoEnterChannelFailure", Protocol.LitoEnterChannelFailure.class,
                Protocol.LitoChannelSelection.class);
        assertEquals(byte[].class, failure.getReturnType());
        Method channelInfo = Protocol.class.getDeclaredMethod(
                "litoChannelInfo", Protocol.LitoChannelSelection.class,
                int.class, int.class);
        assertEquals(byte[].class, channelInfo.getReturnType());

        assertTrue(Arrays.stream(Protocol.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("litoEnterChannelSuccess"))
                .allMatch(method -> Arrays.equals(method.getParameterTypes(),
                        new Class<?>[] {Protocol.LitoChannelSelection.class})));
        assertTrue(Arrays.stream(Protocol.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("litoEnterChannelFailure"))
                .allMatch(method -> Arrays.equals(method.getParameterTypes(),
                        new Class<?>[] {Protocol.LitoEnterChannelFailure.class,
                                Protocol.LitoChannelSelection.class})));
        assertTrue(Arrays.stream(Protocol.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("litoChannelInfo"))
                .allMatch(method -> Arrays.equals(method.getParameterTypes(),
                        new Class<?>[] {Protocol.LitoChannelSelection.class,
                                int.class, int.class})));
    }

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int index = 0; index < values.length; index++) {
            result[index] = (byte) values[index];
        }
        return result;
    }
}

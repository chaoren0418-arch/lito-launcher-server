package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class ProtocolHypertextActionTest {
    @Test
    void parsesExactNativeDialogCdEnvelope() {
        assertEquals(0xF123_4567L, Protocol.dialogObjectId(
                new Protocol.Writer().c(0x29).d(0xF123_4567L).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.dialogObjectId(
                new Protocol.Writer().c(0x28).d(1).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.dialogObjectId(
                new Protocol.Writer().c(0x29).d(0).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.dialogObjectId(
                new Protocol.Writer().c(0x29).d(1).c(0).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.dialogObjectId(
                new byte[] {0x29, 1, 0, 0}));
    }

    @Test
    void writesExactNativeDsshAndRepeatedStringHypertextEnvelope() {
        byte[] packet = Protocol.hypertext(0x1122_3344L, "anicom", "", List.of(
                "$471", "30", "30", "5", "5", "5",
                "Arena Companion", "$611", "0", "0"));
        assertArrayEquals(new byte[] {
                0x2A, 0x44, 0x33, 0x22, 0x11,
                0x61, 0x6E, 0x69, 0x63, 0x6F, 0x6D, 0,
                0, 0x0A, 0,
                0x24, 0x34, 0x37, 0x31, 0,
                0x33, 0x30, 0, 0x33, 0x30, 0,
                0x35, 0, 0x35, 0, 0x35, 0,
                0x41, 0x72, 0x65, 0x6E, 0x61, 0x20, 0x43, 0x6F,
                0x6D, 0x70, 0x61, 0x6E, 0x69, 0x6F, 0x6E, 0,
                0x24, 0x36, 0x31, 0x31, 0,
                0x30, 0, 0x30, 0
        }, packet);
        assertThrows(IllegalArgumentException.class, () ->
                Protocol.hypertext(0, "anicom", "", List.of()));
        assertThrows(IllegalArgumentException.class, () ->
                Protocol.hypertext(1, "", "", List.of()));
    }

    @Test
    void writesExactNativeDddddcssshHypertextInputEnvelope() {
        byte[] packet = Protocol.hypertextInput(
                0x1122_3344L, 0, 5, 1, 10, 7,
                "request", "$100", "buy", List.of("A", "B"));
        assertArrayEquals(new byte[] {
                0x5B,
                0x44, 0x33, 0x22, 0x11,
                0, 0, 0, 0,
                5, 0, 0, 0,
                1, 0, 0, 0,
                10, 0, 0, 0,
                7,
                0x24, 0x31, 0x30, 0x30, 0,
                0x72, 0x65, 0x71, 0x75, 0x65, 0x73, 0x74, 0,
                0x62, 0x75, 0x79, 0,
                2, 0,
                0x41, 0, 0x42, 0
        }, packet);
        assertThrows(IllegalArgumentException.class, () -> Protocol.hypertextInput(
                0, 0, 5, 1, 10, 0, "request", "", "buy", List.of()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.hypertextInput(
                1, 0, 0, 1, 10, 0, "request", "", "buy", List.of()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.hypertextInput(
                1, 0, 1, 1, 10, 0, "x".repeat(32), "", "buy", List.of()));
    }

    @Test
    void writesExactNativeHouseAuctionListAndMapEnvelopes() {
        assertArrayEquals(new byte[] {
                0x73, 0x01, 0x00, 0x00, 0x69, 0x01, 0x00,
                0x01, 0x00, 0x04, 0x00, 0x41, 0x00,
                0x4E, 0x00, 0x08, 0x10,
                (byte) 0xA0, (byte) 0x86, 0x01, 0x00
        }, Protocol.agitList(0x6900_0001L, List.of(
                new Protocol.HouseAuctionRow(
                        0x0004_0001L, "A", 78, 8, 16, 100_000))));
        assertArrayEquals(new byte[] {
                0x74, 0x01, 0x00, 0x00, 0x69,
                0x01, 0x00, 0x04, 0x00
        }, Protocol.agitMap(0x6900_0001L, 0x0004_0001L));

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.agitList(0, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.agitMap(1, 0x0003_0001L));
        assertThrows(IllegalArgumentException.class, () -> Protocol.agitList(1, List.of(
                new Protocol.HouseAuctionRow(0x0004_0001L, "A", 78, 13, 1, 1))));
    }

    @Test
    void writesAndParsesExactNativePetRenameHandshake() {
        assertArrayEquals(new byte[] {0x24, 0x45, 0x01}, Protocol.petRenameAsk());
        Protocol.AskAnswer answer = Protocol.askAnswer(new Protocol.Writer()
                .c(0x23).h(325).c(0).s("K9").bytes());
        assertEquals(325, answer.questionId());
        assertEquals(0, answer.answer());
        assertEquals("K9", answer.text());
        assertArrayEquals(new byte[] {
                0x58, 0x44, 0x33, 0x22, 0x11, 'K', '9', 0
        }, Protocol.changeDescription(0x1122_3344L, "K9"));

        assertEquals(true, Protocol.validPetName("K9"));
        assertEquals(true, Protocol.validPetName("A".repeat(27)));
        assertEquals(false, Protocol.validPetName("A"));
        assertEquals(false, Protocol.validPetName("A".repeat(28)));
        assertEquals(false, Protocol.validPetName("K_9"));
        assertThrows(IllegalArgumentException.class, () -> Protocol.askAnswer(
                new Protocol.Writer().c(0x23).h(325).c(2).s("K9").bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.askAnswer(
                new Protocol.Writer().c(0x23).h(325).c(0).s("K9").c(0).bytes()));
    }

    @Test
    void parsesExactNativeCddcsHypertextInputResultEnvelope() {
        Protocol.HypertextInputResult result = Protocol.hypertextInputResult(
                new Protocol.Writer().c(0x51).d(0x1122_3344L).d(6)
                        .c(7).s("buy").bytes());
        assertEquals(0x1122_3344L, result.worldObjectId());
        assertEquals(6, result.value());
        assertEquals(7, result.mode());
        assertEquals("buy", result.callback());
        assertThrows(IllegalArgumentException.class, () -> Protocol.hypertextInputResult(
                new Protocol.Writer().c(0x50).d(1).d(1).c(0).s("buy").bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.hypertextInputResult(
                new Protocol.Writer().c(0x51).d(0).d(1).c(0).s("buy").bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.hypertextInputResult(
                new Protocol.Writer().c(0x51).d(1).d(1).c(0).s("buy").c(0).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.hypertextInputResult(
                new byte[] {0x51, 1, 0, 0, 0, 1, 0, 0, 0, 0, 'b', 'u', 'y'}));
    }

    @Test
    void writesExactNativeDhhsTimedCommandTargetMarker() {
        assertArrayEquals(new byte[] {
                0x7A, 0x78, 0x56, 0x34, 0x12,
                0x23, (byte) 0x81, (byte) 0xDC, (byte) 0xFE,
                0x54, 0x61, 0x72, 0x67, 0x65, 0x74, 0
        }, Protocol.commandTarget(0x1234_5678L, 0x8123, 0xFEDC, "Target"));
        assertArrayEquals(new byte[] {0x7A, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                Protocol.commandTarget(0, 0, 0, ""));
        assertThrows(IllegalArgumentException.class, () ->
                Protocol.commandTarget(1, -1, 0, "Target"));
        assertThrows(IllegalArgumentException.class, () ->
                Protocol.commandTarget(1, 0, 0, "x".repeat(53)));
    }

    @Test
    void writesAndParsesExactNativeSlaveTargetHandshake() {
        assertArrayEquals(new byte[] {
                0x57, 0x44, 0x33, 0x22, 0x11, 0
        }, Protocol.slaveControl(0x1122_3344L, 0));

        Protocol.SlaveControlRequest request = Protocol.slaveControlRequest(
                new Protocol.Writer().c(0x4F).d(0x1122_3344L)
                        .c(0).d(0x8877_6655L).bytes());
        assertEquals(0x1122_3344L, request.controllerObjectId());
        assertEquals(0, request.mode());
        assertEquals(0x8877_6655L, request.targetObjectId());

        assertThrows(IllegalArgumentException.class, () ->
                Protocol.slaveControl(0, 0));
        assertThrows(IllegalArgumentException.class, () ->
                Protocol.slaveControl(1, 0x100));
        assertThrows(IllegalArgumentException.class, () -> Protocol.slaveControlRequest(
                new Protocol.Writer().c(0x4E).d(1).c(0).d(2).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.slaveControlRequest(
                new Protocol.Writer().c(0x4F).d(0).c(0).d(2).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.slaveControlRequest(
                new Protocol.Writer().c(0x4F).d(1).c(0).d(0).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.slaveControlRequest(
                new Protocol.Writer().c(0x4F).d(1).c(0).d(2).c(0).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.slaveControlRequest(
                new byte[] {0x4F, 1, 0, 0, 0, 0, 2, 0, 0}));
    }

    @Test
    void parsesExactNativeCdssEnvelopeAndClassifiesStaticProducers() {
        Protocol.HypertextActionRequest select = Protocol.hypertextActionRequest(
                new Protocol.Writer().c(0x27).d(0x1122_3344L)
                        .s("select").s("4294967295").bytes());
        assertEquals(0x1122_3344L, select.worldObjectId());
        assertEquals("select", select.action());
        assertEquals("4294967295", select.parameter());
        assertEquals(Protocol.HypertextActionKind.SELECT, select.kind());

        Protocol.HypertextActionRequest target = Protocol.hypertextActionRequest(
                new Protocol.Writer().c(0x27).d(2).s("attackchr").s("").bytes());
        assertEquals(Protocol.HypertextActionKind.ATTACK_TARGET, target.kind());

        Protocol.HypertextActionRequest posture = Protocol.hypertextActionRequest(
                new Protocol.Writer().c(0x27).d(2).s("defensive").s("").bytes());
        assertEquals(Protocol.HypertextActionKind.POSTURE, posture.kind());

        Protocol.HypertextActionRequest dynamic = Protocol.hypertextActionRequest(
                new Protocol.Writer().c(0x27).d(9).s("quest_42").s("page=2").bytes());
        assertEquals(Protocol.HypertextActionKind.GENERIC, dynamic.kind());
    }

    @Test
    void rejectsLayoutsNoExactNativeProducerCanEmit() {
        assertThrows(IllegalArgumentException.class, () -> Protocol.hypertextActionRequest(
                new Protocol.Writer().c(0x26).d(1).s("dismiss").s("").bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.hypertextActionRequest(
                new Protocol.Writer().c(0x27).d(0).s("dismiss").s("").bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.hypertextActionRequest(
                new Protocol.Writer().c(0x27).d(1).s("").s("").bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.hypertextActionRequest(
                new Protocol.Writer().c(0x27).d(1).s(" dismiss").s("").bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.hypertextActionRequest(
                new Protocol.Writer().c(0x27).d(1).s("dismiss").s("unexpected").bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.hypertextActionRequest(
                new Protocol.Writer().c(0x27).d(1).s("select").s("-1").bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.hypertextActionRequest(
                new Protocol.Writer().c(0x27).d(1).s("select").s("4294967296").bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.hypertextActionRequest(
                new Protocol.Writer().c(0x27).d(1).s("bad\nname").s("").bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.hypertextActionRequest(
                new Protocol.Writer().c(0x27).d(1).s("a".repeat(256)).s("").bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.hypertextActionRequest(
                new Protocol.Writer().c(0x27).d(1).s("dismiss").s("").c(0).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.hypertextActionRequest(
                new byte[] {0x27, 1, 0, 0, 0, 'd', 'i', 's', 'm', 'i', 's', 's'}));
    }
}

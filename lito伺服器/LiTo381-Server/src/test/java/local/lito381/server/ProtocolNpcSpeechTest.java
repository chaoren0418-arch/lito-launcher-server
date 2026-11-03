package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ProtocolNpcSpeechTest {
    @Test void writesExactNativeSayCodeFrame() {
        assertArrayEquals(new Protocol.Writer().c(Protocol.S_SAY_CODE).c(0)
                        .d(0x1122_3344L).s("$478: ready")
                        .h(32741).h(32801).bytes(),
                Protocol.sayCode(0, 0x1122_3344L, "$478: ready", 32741, 32801));
        assertArrayEquals(new Protocol.Writer().c(Protocol.S_SAY_CODE).c(2)
                        .d(1).s("NPC 台詞").h(0).h(0xFFFF).bytes(),
                Protocol.sayCode(2, 1, "NPC 台詞", 0, 0xFFFF));
    }

    @Test void rejectsFieldsOutsideExactNativeReceiverStorage() {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.sayCode(4, 1, "speech", 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.sayCode(0, 0, "speech", 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.sayCode(0, 1, "", 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.sayCode(0, 1, "bad\0tail", 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.sayCode(0, 1, "X".repeat(512), 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.sayCode(0, 1, "speech", -1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.sayCode(0, 1, "speech", 1, 0x1_0000));
    }

    @Test void mapsRetainedQuestHelpToExactLitoNpcChannelReceiver() {
        String message = "\\fV副本任務-剩餘時間：30分";
        assertArrayEquals(new Protocol.Writer().c(Protocol.S_SAY_CODE).c(0)
                        .d(0).s(message).h(32720).h(32900).bytes(),
                Protocol.questHelpMessage(message, 32720, 32900));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.questHelpMessage("", 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.questHelpMessage("message", 0x1_0000, 1));
    }
}

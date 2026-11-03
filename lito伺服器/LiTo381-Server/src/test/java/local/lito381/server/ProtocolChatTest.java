package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ProtocolChatTest {
    @Test void parsesNativeSayAlternateChatTellAndWhoRequests() {
        assertEquals(new Protocol.OnOffRequest(0, 1),
                Protocol.onOffRequest(new Protocol.Writer().c(Protocol.C_ONOFF)
                        .c(0).c(1).bytes()));
        assertEquals(new Protocol.OnOffRequest(0xFF, 0x7F),
                Protocol.onOffRequest(new Protocol.Writer().c(Protocol.C_ONOFF)
                        .c(0xFF).c(0x7F).bytes()));
        assertEquals(new Protocol.ChatRequest(4, "global"),
                Protocol.chatRequest(new Protocol.Writer().c(Protocol.C_SAY)
                        .c(4).s("global").bytes(), Protocol.C_SAY));
        assertEquals(new Protocol.ChatRequest(12, "alternate"),
                Protocol.chatRequest(new Protocol.Writer().c(Protocol.C_CHAT)
                        .c(12).s("alternate").bytes(), Protocol.C_CHAT));
        assertEquals(new Protocol.TellRequest("TestMage", "private"),
                Protocol.tellRequest(new Protocol.Writer().c(Protocol.C_TELL)
                        .s("TestMage").s("private").bytes()));
        assertEquals("", Protocol.whoRequest(
                new Protocol.Writer().c(Protocol.C_WHO).s("").bytes()));
        assertEquals("TestMage", Protocol.whoRequest(
                new Protocol.Writer().c(Protocol.C_WHO).s("TestMage").bytes()));
    }

    @Test void writesNativeSayAndTellFrames() {
        assertArrayEquals(new Protocol.Writer().c(Protocol.S_SAY).c(4)
                        .d(0x1122_3344L).s("TestKnight: hello")
                        .h(32730).h(32798).bytes(),
                Protocol.say(4, 0x1122_3344L, "TestKnight: hello", 32730, 32798));
        assertArrayEquals(new Protocol.Writer().c(Protocol.S_TELL)
                        .s("TestMage").s("private").bytes(),
                Protocol.tell("TestMage", "private"));
    }

    @Test void rejectsMalformedOrNonNativeChatFrames() {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.onOffRequest(new Protocol.Writer().c(Protocol.C_ONOFF)
                        .c(1).c(0).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.onOffRequest(new Protocol.Writer().c(Protocol.C_ONOFF)
                        .c(6).c(2).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.chatRequest(new Protocol.Writer().c(Protocol.C_CHAT)
                        .c(11).s("wrong type").bytes(), Protocol.C_CHAT));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.chatRequest(new Protocol.Writer().c(Protocol.C_SAY)
                        .c(0).s("").bytes(), Protocol.C_SAY));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.tellRequest(new Protocol.Writer().c(Protocol.C_TELL)
                        .s("").s("private").bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.whoRequest(new Protocol.Writer().c(Protocol.C_WHO)
                        .s("X".repeat(28)).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.tell("X".repeat(28), "private"));
    }
}

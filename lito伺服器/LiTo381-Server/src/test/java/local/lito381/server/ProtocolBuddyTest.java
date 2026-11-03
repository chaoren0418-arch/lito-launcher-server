package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ProtocolBuddyTest {
    @Test
    void groupChangeMatchesNativeShContract() {
        Protocol.Reader reader = new Protocol.Reader(Protocol.buddyGroupChanged("TestMage", 0x1234));
        assertEquals(Protocol.S_LITO_CHANGE_BUDDY_GROUP_RESULT, reader.c());
        assertEquals("TestMage", reader.s());
        assertEquals(0x1234, reader.h());
        assertEquals(0, reader.remaining());
    }

    @Test
    void presenceAndStatusMatchNativeScContracts() {
        Protocol.Reader online = new Protocol.Reader(Protocol.buddyLogin(
                "TestMage", Protocol.LitoBuddyPresence.ONLINE));
        assertEquals(Protocol.S_LITO_BUDDY_LOGIN, online.c());
        assertEquals("TestMage", online.s());
        assertEquals(0, online.c());
        assertEquals(0, online.remaining());

        Protocol.Reader offline = new Protocol.Reader(Protocol.buddyLogin(
                "TestMage", Protocol.LitoBuddyPresence.OFFLINE));
        assertEquals(Protocol.S_LITO_BUDDY_LOGIN, offline.c());
        assertEquals("TestMage", offline.s());
        assertEquals(1, offline.c());
        assertEquals(0, offline.remaining());

        Protocol.Reader status = new Protocol.Reader(Protocol.buddyStatus(
                "TestMage", Protocol.LitoBuddyActivity.FIGHTING));
        assertEquals(Protocol.S_LITO_BUDDY_CHANGE_STATUS, status.c());
        assertEquals("TestMage", status.s());
        assertEquals(1, status.c());
        assertEquals(0, status.remaining());
    }

    @Test
    void listMatchesNativeShccscFieldOrder() {
        Protocol.Reader list = new Protocol.Reader(Protocol.buddyList(List.of(
                new Protocol.LitoBuddyRow("TestMage", 0x1234, 3,
                        7, "Moon", Protocol.LitoBuddyActivity.OFFLINE))));
        assertEquals(Protocol.S_LITO_BUDDY_LIST, list.c());
        assertEquals(1, list.c());
        assertEquals("TestMage", list.s());
        assertEquals(0x1234, list.h());
        assertEquals(3, list.c());
        assertEquals(7, list.c());
        assertEquals("Moon", list.s());
        assertEquals(2, list.c());
        assertEquals(0, list.remaining());
    }

    @Test
    void errorsMatchNativeCcContractAndKnownLtstringMeanings() {
        Protocol.Reader duplicate = new Protocol.Reader(
                Protocol.buddyError(Protocol.LitoBuddyError.ALREADY_REGISTERED));
        assertEquals(Protocol.S_LITO_BUDDY_ERROR, duplicate.c());
        assertEquals(0x12, duplicate.c());
        assertEquals(0, duplicate.remaining());

        Protocol.Reader full = new Protocol.Reader(
                Protocol.buddyError(Protocol.LitoBuddyError.LIST_FULL));
        assertEquals(Protocol.S_LITO_BUDDY_ERROR, full.c());
        assertEquals(0x13, full.c());
        assertEquals(0, full.remaining());
        assertThrows(NullPointerException.class, () -> Protocol.buddyError(null));
    }
}

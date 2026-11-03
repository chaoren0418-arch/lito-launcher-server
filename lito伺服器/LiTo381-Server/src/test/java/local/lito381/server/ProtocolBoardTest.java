package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ProtocolBoardTest {
    private static final long BOARD_OBJECT = 0x1122_3344L;
    private static final GameStateStore.BoardTopic TOPIC =
            new GameStateStore.BoardTopic(7, "TestKnight", "26/08/09",
                    "Native title", "Line one\r\nLine two");

    @Test
    void parsesExactNativeWriteListAndReadRequests() {
        Protocol.BoardWriteRequest write = Protocol.boardWriteRequest(
                new Protocol.Writer().c(Protocol.C_BOARD_WRITE).d(BOARD_OBJECT)
                        .s("Title").s("Body").bytes());
        assertEquals(BOARD_OBJECT, write.boardObjectId());
        assertEquals("Title", write.title());
        assertEquals("Body", write.content());

        Protocol.BoardPageRequest page = Protocol.boardPageRequest(
                new Protocol.Writer().c(Protocol.C_BOARD_LIST).d(BOARD_OBJECT)
                        .d(Protocol.BOARD_INITIAL_CURSOR).bytes());
        assertEquals(BOARD_OBJECT, page.boardObjectId());
        assertEquals(Protocol.BOARD_INITIAL_CURSOR, page.cursor());

        Protocol.BoardReadRequest read = Protocol.boardReadRequest(
                new Protocol.Writer().c(Protocol.C_BOARD_READ).d(BOARD_OBJECT).d(7).bytes());
        assertEquals(BOARD_OBJECT, read.boardObjectId());
        assertEquals(7, read.topicId());

        assertThrows(IllegalArgumentException.class, () -> Protocol.boardWriteRequest(
                new Protocol.Writer().c(Protocol.C_BOARD_WRITE).d(BOARD_OBJECT)
                        .s("").s("Body").bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.boardPageRequest(
                new Protocol.Writer().c(Protocol.C_BOARD_LIST).d(BOARD_OBJECT).d(0).c(0).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.boardReadRequest(
                new Protocol.Writer().c(Protocol.C_BOARD_READ).d(BOARD_OBJECT).d(0).bytes()));
    }

    @Test
    void writesExactDdHShListHeaderAndDsssRows() {
        byte[] expected = new Protocol.Writer().c(Protocol.S_BOARD_LIST)
                .d(BOARD_OBJECT).d(Protocol.BOARD_INITIAL_CURSOR).h(1)
                .s("LiTo Board").h(Protocol.BOARD_WRITE_FEE)
                .d(TOPIC.id()).s(TOPIC.author()).s(TOPIC.date()).s(TOPIC.title()).bytes();
        byte[] actual = Protocol.boardList(BOARD_OBJECT, Protocol.BOARD_INITIAL_CURSOR,
                "LiTo Board", Protocol.BOARD_WRITE_FEE, List.of(TOPIC));
        assertArrayEquals(expected, actual);
        Protocol.Reader in = new Protocol.Reader(actual);
        assertEquals(Protocol.S_BOARD_LIST, in.c());
        assertEquals(BOARD_OBJECT, in.d());

        assertThrows(IllegalArgumentException.class, () -> Protocol.boardList(
                BOARD_OBJECT, 0, "LiTo Board", 300,
                java.util.Collections.nCopies(9, TOPIC)));
    }

    @Test
    void writesNativeDetailOrderAuthorTitleDateContent() {
        byte[] expected = new Protocol.Writer().c(Protocol.S_BOARD_READ).d(TOPIC.id())
                .s(TOPIC.author()).s(TOPIC.title()).s(TOPIC.date()).s(TOPIC.content()).bytes();
        assertArrayEquals(expected, Protocol.boardRead(TOPIC));

        Protocol.Reader in = new Protocol.Reader(Protocol.boardRead(TOPIC));
        assertEquals(Protocol.S_BOARD_READ, in.c());
        assertEquals(TOPIC.id(), in.d());
        assertEquals(TOPIC.author(), in.s());
        assertEquals(TOPIC.title(), in.s());
        assertEquals(TOPIC.date(), in.s());
        assertEquals(TOPIC.content(), in.s());
        assertEquals(0, in.remaining());
    }
}

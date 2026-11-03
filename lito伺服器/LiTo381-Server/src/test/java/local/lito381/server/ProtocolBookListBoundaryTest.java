package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ProtocolBookListBoundaryTest {
    @Test void writesExactDormantReceiverLayoutWithoutGuessingIgnoredTag() {
        assertArrayEquals(new byte[] {
                0x7E, (byte) 0xA5, 0x02,
                0x41, 0x42, 0x00, 0x78, 0x56, 0x34, 0x12,
                0x5A, 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
        }, Protocol.bookList(0xA5, List.of(
                new Protocol.BookListEntry("AB", 0x1234_5678L),
                new Protocol.BookListEntry("Z", 0xFFFF_FFFFL))));
        assertArrayEquals(new byte[] {0x7E, 0x00, 0x00},
                Protocol.bookList(0, List.of()));
    }

    @Test void rejectsFieldsThatCannotFitTheNativeReceiver() {
        assertInvalid(() -> Protocol.bookList(-1, List.of()));
        assertInvalid(() -> Protocol.bookList(256, List.of()));
        assertInvalid(() -> Protocol.bookList(0, null));

        List<Protocol.BookListEntry> tooMany = new ArrayList<>();
        for (int index = 0; index < 256; index++) {
            tooMany.add(new Protocol.BookListEntry("", index));
        }
        assertInvalid(() -> Protocol.bookList(0, tooMany));
        assertInvalid(() -> Protocol.bookList(0, java.util.Collections.singletonList(null)));
        assertInvalid(() -> Protocol.bookList(0, List.of(
                new Protocol.BookListEntry(null, 0))));
        assertInvalid(() -> Protocol.bookList(0, List.of(
                new Protocol.BookListEntry("X".repeat(28), 0))));
        assertInvalid(() -> Protocol.bookList(0, List.of(
                new Protocol.BookListEntry("bad\nname", 0))));
        assertInvalid(() -> Protocol.bookList(0, List.of(
                new Protocol.BookListEntry("past", -1))));
        assertInvalid(() -> Protocol.bookList(0, List.of(
                new Protocol.BookListEntry("future", 0x1_0000_0000L))));
    }

    @Test void productionSourceRetainsOnlyOneFailClosedDiagnosticCallSite()
            throws Exception {
        Path main = Path.of("src/main/java");
        int calls = 0;
        int symbolicOpcodeReferences = 0;
        try (var sources = Files.walk(main)) {
            for (Path path : sources.filter(Files::isRegularFile)
                    .filter(candidate -> candidate.getFileName().toString()
                            .endsWith(".java"))
                    .toList()) {
                String source = Files.readString(path, StandardCharsets.UTF_8);
                calls += occurrences(source, "Protocol.bookList(");
                symbolicOpcodeReferences += occurrences(source, "Protocol.S_BOOK_LIST");
            }
        }
        assertEquals(1, calls, "unexpected S_BOOK_LIST call-site count");
        assertEquals(0, symbolicOpcodeReferences,
                "production source routes receiver-only S_BOOK_LIST");

        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int start = session.indexOf("private boolean sendBookListProbe(");
        int end = session.indexOf("private void tell(", start);
        assertTrue(start >= 0 && end > start);
        String diagnostic = session.substring(start, end);
        assertEquals(1, occurrences(diagnostic, "Protocol.bookList("));
        assertTrue(diagnostic.contains("config.bookListProbeEnabled()"));
        assertTrue(diagnostic.contains("phase != Phase.BATTLE"));
        assertTrue(diagnostic.contains("socket.getInetAddress().isLoopbackAddress()"));

        String protocol = Files.readString(Path.of(
                "src/main/java/local/lito381/server/Protocol.java"),
                StandardCharsets.UTF_8);
        assertEquals(1, occurrences(protocol, "static byte[] bookList("));
        assertEquals(1, occurrences(protocol, ".c(S_BOOK_LIST)"));
    }

    private static void assertInvalid(Runnable runnable) {
        assertThrows(IllegalArgumentException.class, runnable::run);
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0;
             at += needle.length()) count++;
        return count;
    }
}

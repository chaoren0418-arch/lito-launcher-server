package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Keeps every native-receiver/original-producer gap fail-closed as one set. */
final class RemainingNativeOriginBoundaryTest {
    private static final Path SOURCE = Path.of("src/main/java/local/lito381/server");

    @Test
    void allFourOriginUnprovedWritersRemainInsideDedicatedDiagnostics()
            throws IOException {
        String protocol = Files.readString(SOURCE.resolve("Protocol.java"));
        String session = Files.readString(SOURCE.resolve("ClientSession.java"));

        assertEquals(1, occurrences(protocol, ".c(S_CHARACTER_INFO)"));
        assertEquals(1, occurrences(protocol, ".c(S_ATTACK_ALL)"));
        assertEquals(1, occurrences(protocol, ".c(S_CLONE)"));
        assertEquals(1, occurrences(protocol, ".c(S_BOOK_LIST)"));

        String character = method(session,
                "private void sendNativeCharacterEventProbe()",
                "private void sendLitoServerList()");
        assertEquals(1, occurrences(session, "Protocol.specialCharacterServerEvent("));
        assertEquals(1, occurrences(character, "Protocol.specialCharacterServerEvent("));
        assertTrue(character.contains("config.nativeCharacterEventProbeEnabled()"));
        assertIsolation(character);

        String clone = method(session,
                "private boolean sendCloneAppearanceProbe(",
                "private boolean sendAttackAllProbe(");
        assertEquals(2, occurrences(session, "Protocol.cloneAppearance("));
        assertEquals(2, occurrences(clone, "Protocol.cloneAppearance("));
        assertTrue(clone.contains("config.cloneAppearanceProbeEnabled()"));
        assertIsolation(clone);

        String attack = method(session,
                "private boolean sendAttackAllProbe(",
                "private boolean sendBookListProbe(");
        assertEquals(1, occurrences(session, "Protocol.attackAll("));
        assertEquals(1, occurrences(attack, "Protocol.attackAll("));
        assertTrue(attack.contains("config.attackAllProbeEnabled()"));
        assertIsolation(attack);

        String book = method(session,
                "private boolean sendBookListProbe(",
                "private void tell(");
        assertEquals(1, occurrences(session, "Protocol.bookList("));
        assertEquals(1, occurrences(book, "Protocol.bookList("));
        assertTrue(book.contains("config.bookListProbeEnabled()"));
        assertIsolation(book);

        // No symbolic opcode shortcut may bypass the exact writer methods.
        assertFalse(session.contains("Protocol.S_CHARACTER_INFO"));
        assertFalse(session.contains("Protocol.S_ATTACK_ALL"));
        assertFalse(session.contains("Protocol.S_CLONE"));
        assertFalse(session.contains("Protocol.S_BOOK_LIST"));
    }

    @Test
    void onlyTheNamedLoopbackFixturesEnableTheFourDiagnostics()
            throws IOException {
        assertEquals(List.of("server.character-event-probe.properties"),
                enabledBy("native.character.event.probe.enabled=true"));
        assertEquals(List.of("server.attack-all-probe.properties"),
                enabledBy("attack.all.probe.enabled=true"));
        assertEquals(List.of(
                        "server.clone-appearance-physical-probe.properties",
                        "server.clone-appearance-probe.properties"),
                enabledBy("clone.appearance.probe.enabled=true"));
        assertEquals(List.of("server.book-list-probe.properties"),
                enabledBy("book.list.probe.enabled=true"));
    }

    private static void assertIsolation(String source) {
        assertTrue(source.contains("\"memory\".equals(config.databaseMode())"));
        assertTrue(source.contains("config.databaseReadOnly()"));
        assertTrue(source.contains("!config.statePersistenceEnabled()"));
        assertTrue(source.contains("config.port() == 32100"));
        assertTrue(source.contains("socket.getInetAddress().isLoopbackAddress()"));
    }

    private static List<String> enabledBy(String setting) throws IOException {
        List<String> enabled = new ArrayList<>();
        try (var paths = Files.list(
                Path.of("src/test/resources/server-config/config"))) {
            paths.filter(path -> path.getFileName().toString().endsWith(".properties"))
                    .filter(path -> {
                        try {
                            return Files.readString(path).contains(setting);
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .map(path -> path.getFileName().toString())
                    .forEach(enabled::add);
        }
        Collections.sort(enabled);
        return enabled;
    }

    private static String method(String source, String begin, String end) {
        int start = source.indexOf(begin);
        int finish = source.indexOf(end, start);
        assertTrue(start >= 0 && finish > start,
                () -> "missing method boundary " + begin + " -> " + end);
        return source.substring(start, finish);
    }

    private static int occurrences(String source, String token) {
        int count = 0;
        for (int offset = 0; (offset = source.indexOf(token, offset)) >= 0;
                offset += token.length()) {
            count++;
        }
        return count;
    }
}

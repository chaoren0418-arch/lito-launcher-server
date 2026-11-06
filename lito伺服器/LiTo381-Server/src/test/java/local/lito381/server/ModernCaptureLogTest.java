package local.lito381.server;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ModernCaptureLogTest {
    @TempDir Path temporary;

    @Test void extendedCaptureKeepsConnectionSequenceAndMasksCredentials() throws Exception {
        Path path;
        try (var capture = new ModernCaptureLog(temporary)) {
            var first = capture.connection("LOGIN");
            var second = capture.connection("GAME");
            capture.frame(first, "AUTH", "received", "client_to_server",
                    new Protocol.Writer().c(Protocol.C_LOGIN).s("account").s("secret").bytes(), 20, "login");
            capture.frame(second, "LOBBY", "sent", "server_to_client", new byte[]{1}, 3, "reply");
            path = capture.path();
            try (var other = new ModernCaptureLog(temporary)) {
                assertThrows(IllegalArgumentException.class, () -> other.frame(first, "AUTH", "received",
                        "client_to_server", new byte[]{1}, 3, "wrong owner"));
            }
        }
        var lines = Files.readAllLines(path);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("\"capture_id\":"));
        assertTrue(lines.get(0).contains("\"sequence\":1,"));
        assertTrue(lines.get(1).contains("\"sequence\":2,"));
        assertTrue(lines.get(0).contains("\"connection_id\":1,\"endpoint\":\"LOGIN\""));
        assertTrue(lines.get(1).contains("\"connection_id\":2,\"endpoint\":\"GAME\""));
        assertTrue(lines.get(0).contains("\"phase\":\"AUTH\",\"observation\":\"received\""));
        assertTrue(lines.get(0).contains("MASKED_C_LOGIN"));
        assertFalse(lines.get(0).contains("secret"));
    }

    @Test void formalAdapterKeepsFormalFormatAndOwnerCloseBoundary() throws Exception {
        var formal = new CaptureLog(temporary);
        var adapter = new ModernCaptureLog(formal);
        adapter.frame(adapter.connection("LOGIN"), "AUTH", "received", "client_to_server",
                new Protocol.Writer().c(Protocol.C_LOGIN).s("account").s("secret").bytes(), 20, "login");
        assertEquals(formal.path(), adapter.path());
        String line = Files.readString(formal.path());
        assertTrue(line.contains("MASKED_C_LOGIN"));
        assertFalse(line.contains("capture_id"));
        formal.close();
        assertThrows(java.io.IOException.class,
                () -> adapter.frame("server_to_client", new byte[]{1}, 3, "closed"));
    }
}

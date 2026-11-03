package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class ModernStandaloneMainTest {
    @Test void versionHasNoInputOrConfigSideEffects() {
        var out = new ByteArrayOutputStream(); var err = new ByteArrayOutputStream();
        var input = new InputStream() { public int read() { throw new AssertionError("version read stdin"); } };
        assertEquals(0, ModernStandaloneMain.run(new String[]{"--version"}, input, new PrintStream(out), new PrintStream(err)));
        String text = out.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("\"application\":\"LiToModernServer\""));
        assertTrue(text.contains("\"max_protocol\":47"));
        assertTrue(text.contains("\"native_entry\":false"));
        assertEquals("", err.toString(StandardCharsets.UTF_8));
    }
    @Test void dedicatedEntryNeverDefaultsToNativeOrAcceptsExtraArguments() {
        for (String[] args : new String[][]{{},{"config/server.properties"},{"--version","extra"},
                {"--modern-config"},{"--modern-config",""},{"--modern-config","a","--native"},
                {"--modern-config","a","--console","extra"}}) {
            var out = new ByteArrayOutputStream(); var err = new ByteArrayOutputStream();
            assertEquals(2, ModernStandaloneMain.run(args, InputStream.nullInputStream(), new PrintStream(out), new PrintStream(err)));
            assertEquals(0, out.size()); assertTrue(err.toString(StandardCharsets.UTF_8).startsWith("usage:"));
        }
        assertTrue(ModernStandaloneMain.validStart(new String[]{"--modern-config","a"}));
        assertTrue(ModernStandaloneMain.validStart(new String[]{"--modern-config","a","--console"}));
    }
    @Test void invalidConfigIsSanitizedAndNeverFallsBack() {
        var err = new ByteArrayOutputStream();
        assertEquals(2, ModernStandaloneMain.run(new String[]{"--modern-config","secret-invalid-path"},
                InputStream.nullInputStream(), new PrintStream(OutputStream.nullOutputStream()), new PrintStream(err)));
        assertFalse(err.toString(StandardCharsets.UTF_8).contains("secret-invalid-path"));
    }
    @Test void consoleReadsBoundedExactCommandsAndDrainsOversizedLines() throws Exception {
        var input = new ByteArrayInputStream(("x".repeat(10000)+"\nstatus\r\nSTOP\nstop\n").getBytes(StandardCharsets.US_ASCII));
        assertEquals(ModernServerConsole.Command.REJECTED, ModernServerConsole.read(input));
        assertEquals(ModernServerConsole.Command.STATUS, ModernServerConsole.read(input));
        assertEquals(ModernServerConsole.Command.REJECTED, ModernServerConsole.read(input));
        assertEquals(ModernServerConsole.Command.STOP, ModernServerConsole.read(input));
        assertEquals(ModernServerConsole.Command.EOF, ModernServerConsole.read(input));
    }
    @Test void consoleStatusDoesNotStopAndStopIsExactlyOnce() {
        var stopped = new AtomicInteger(); var statuses = new AtomicInteger(); var out = new ByteArrayOutputStream();
        ModernServerConsole.run(new ByteArrayInputStream("invalid-secret\nstatus\nstop\nstatus\n".getBytes(StandardCharsets.US_ASCII)),
                new PrintStream(out), stopped::incrementAndGet, () -> { assertEquals(0, stopped.get()); statuses.incrementAndGet(); return "STATUS"; });
        assertEquals(1, stopped.get()); assertEquals(1, statuses.get());
        assertFalse(out.toString(StandardCharsets.UTF_8).contains("invalid-secret"));
    }
    @Test void consoleOwnerEofOrReadFailureStopsOnce() {
        for (InputStream input : new InputStream[]{InputStream.nullInputStream(), new InputStream(){public int read() throws IOException {throw new IOException("secret");}}}) {
            var stopped = new AtomicInteger();
            ModernServerConsole.run(input, new PrintStream(OutputStream.nullOutputStream()), stopped::incrementAndGet, () -> "unused");
            assertEquals(1, stopped.get());
        }
    }
}

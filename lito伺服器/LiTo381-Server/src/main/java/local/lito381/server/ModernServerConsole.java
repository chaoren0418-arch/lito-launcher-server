package local.lito381.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.function.Supplier;

/** Explicit local operator console, never a network or gameplay command route. */
final class ModernServerConsole {
    private ModernServerConsole() {}
    enum Command { STATUS, STOP, REJECTED, EOF }

    static Command read(InputStream input) throws IOException {
        byte[] line = new byte[128];
        int length = 0;
        boolean overflow = false;
        for (;;) {
            int value = input.read();
            if (value == -1) return Command.EOF;
            if (value == '\n') break;
            if (length < line.length) line[length++] = (byte) value;
            else overflow = true;
        }
        if (overflow) return Command.REJECTED;
        if (length > 0 && line[length - 1] == '\r') length--;
        String text = new String(line, 0, length, java.nio.charset.StandardCharsets.US_ASCII);
        return switch (text) { case "status" -> Command.STATUS; case "stop" -> Command.STOP; default -> Command.REJECTED; };
    }

    static void run(InputStream input, PrintStream output, Runnable stop, Supplier<String> status) {
        try {
            for (;;) {
                switch (read(input)) {
                    case STATUS -> { output.println(status.get()); output.flush(); }
                    case REJECTED -> { output.println("MODERN_CONSOLE unsupported command"); output.flush(); }
                    case STOP, EOF -> { return; }
                }
            }
        } catch (IOException failure) {
            // A supervised console losing its owner must not leave an orphan server.
        } finally { stop.run(); }
    }

    static String status(ModernServer server, String prefix) {
        var state = server.snapshot();
        return prefix + " accepted=" + state.accepted() + " sessions=" + state.sessions()
                + " failed=" + state.failed() + " listenerFailed=" + server.listenerFailed();
    }
}

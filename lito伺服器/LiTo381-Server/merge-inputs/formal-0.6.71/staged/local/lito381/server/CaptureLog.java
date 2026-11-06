package local.lito381.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

final class CaptureLog implements AutoCloseable {
    private static final DateTimeFormatter DIRECTORY_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);
    private static final AtomicInteger INSTANCE_SEQUENCE = new AtomicInteger();
    private final BufferedWriter writer;
    private final Path path;

    CaptureLog(Path root) throws IOException {
        String runId = DIRECTORY_TIME.format(Instant.now()) + "-p" + ProcessHandle.current().pid()
                + "-" + INSTANCE_SEQUENCE.incrementAndGet();
        Path directory = root.resolve(runId);
        Files.createDirectories(directory);
        path = directory.resolve("frames.jsonl");
        writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    synchronized void frame(String direction, byte[] plain, int wireLength, String note) throws IOException {
        int opcode = plain.length == 0 ? -1 : plain[0] & 0xFF;
        String sensitive = direction.equals("client_to_server")
                ? maskedClientPayload(opcode, plain)
                : direction.equals("server_to_client")
                ? maskedServerPayload(opcode, plain) : null;
        String payload = sensitive == null ? Protocol.hex(plain) : sensitive;
        writer.write("{\"time\":\"" + Instant.now() + "\",\"direction\":\"" + direction
                + "\",\"opcode\":\"" + (opcode < 0 ? "none" : String.format("0x%02X", opcode))
                + "\",\"wire_length\":" + wireLength + ",\"plain\":\"" + payload
                + "\",\"note\":\"" + escape(note) + "\"}\n");
        writer.flush();
    }

    Path path() { return path; }

    static String maskedClientPayload(int opcode, byte[] plain) {
        return switch (opcode) {
            case Protocol.C_LOGIN -> "MASKED_C_LOGIN";
            case Protocol.C_NEW_ACCOUNT -> "MASKED_C_NEW_ACCOUNT";
            case Protocol.C_CHANGE_PASSWORD -> "MASKED_C_CHANGE_PASSWORD";
            case Protocol.C_REGISTER_QUIZ -> "MASKED_C_REGISTER_QUIZ";
            case Protocol.C_CHANGE_ACCOUNTINFO -> "MASKED_C_CHANGE_ACCOUNTINFO";
            case Protocol.C_WISH -> "MASKED_C_WISH";
            case Protocol.C_SILENCE -> "MASKED_C_SILENCE duration="
                    + (plain.length < 3 ? "missing"
                    : ((plain[1] & 0xFF) | ((plain[2] & 0xFF) << 8)))
                    + " bytes=" + plain.length;
            case Protocol.C_WANTED -> "MASKED_C_WANTED mode="
                    + (plain.length < 2 ? "missing" : plain[1] & 0xFF)
                    + " bytes=" + plain.length;
            case Protocol.C_WAREHOUSE_CONTROL -> "MASKED_C_WAREHOUSE_CONTROL mode="
                    + (plain.length < 2 ? "missing" : plain[1] & 0xFF)
                    + " bytes=" + plain.length;
            case Protocol.C_SMS -> "MASKED_C_SMS mode="
                    + (plain.length < 2 ? "missing" : plain[1] & 0xFF)
                    + " bytes=" + plain.length;
            case Protocol.C_BUILDER_CONTROL -> "MASKED_C_BUILDER_CONTROL subtype="
                    + (plain.length < 2 ? "missing" : String.format("0x%02X", plain[1] & 0xFF))
                    + " bytes=" + plain.length;
            default -> null;
        };
    }

    static String maskedServerPayload(int opcode, byte[] plain) {
        return switch (opcode) {
            case Protocol.S_WANTED_LOGIN -> "MASKED_S_WANTED_LOGIN bytes=" + plain.length;
            default -> null;
        };
    }

    @Override public synchronized void close() throws IOException { writer.close(); }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
    }
}

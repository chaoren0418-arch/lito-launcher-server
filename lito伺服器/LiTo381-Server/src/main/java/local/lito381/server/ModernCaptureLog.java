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

final class ModernCaptureLog implements AutoCloseable {
    private static final DateTimeFormatter DIRECTORY_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);
    private static final AtomicInteger INSTANCE_SEQUENCE = new AtomicInteger();
    private final BufferedWriter writer;
    private final Path path;
    private final CaptureLog formal;
    private final String captureId;
    private long connectionSequence;
    private long frameSequence;

    /** Immutable identity of one connection within this capture instance, never an account identity. */
    static final class Connection {
        private final ModernCaptureLog owner;
        private final long id;
        private final String endpoint;

        private Connection(ModernCaptureLog owner, long id, String endpoint) {
            this.owner = owner;
            this.id = id;
            this.endpoint = endpoint;
        }
    }

    ModernCaptureLog(Path root) throws IOException {
        formal = null;
        String runId = DIRECTORY_TIME.format(Instant.now()) + "-p" + ProcessHandle.current().pid()
                + "-" + INSTANCE_SEQUENCE.incrementAndGet();
        captureId = runId;
        Path directory = root.resolve(runId);
        Files.createDirectories(directory);
        path = directory.resolve("frames.jsonl");
        writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    // A 的正式入口仍持有 CaptureLog；保留其關閉責任與正式記錄格式。
    // 只有明確使用 ModernCaptureLog 的來源專有入口輸出延伸欄位。
    ModernCaptureLog(CaptureLog formal) {
        this.formal = java.util.Objects.requireNonNull(formal);
        this.path = formal.path();
        this.captureId = path.getParent().getFileName().toString();
        this.writer = null;
    }

    synchronized Connection connection(String endpoint) {
        if (endpoint == null) throw new IllegalArgumentException("missing capture endpoint");
        connectionSequence = Math.incrementExact(connectionSequence);
        return new Connection(this, connectionSequence, endpoint);
    }

    /** Retains the legacy caller API without inferring a connection or phase. */
    synchronized void frame(String direction, byte[] plain, int wireLength, String note) throws IOException {
        frame(null, null, "legacy_unattributed", direction, plain, wireLength, note);
    }

    /**
     * Phase is the caller's literal snapshot before acquiring this log's monitor.
     * Sequence orders serialization attempts within capture_id, not socket delivery or BIN decoding;
     * a failed write may consume a sequence number. This monitor also serializes flush and close.
     */
    synchronized void frame(Connection connection, String phase, String observation,
                            String direction, byte[] plain, int wireLength, String note) throws IOException {
        if (connection != null && connection.owner != this) {
            throw new IllegalArgumentException("connection belongs to another capture");
        }
        if (formal != null) {
            formal.frame(direction, plain, wireLength, note);
            return;
        }
        int opcode = plain.length == 0 ? -1 : plain[0] & 0xFF;
        String sensitive = direction.equals("client_to_server")
                ? maskedClientPayload(opcode, plain)
                : direction.equals("server_to_client")
                ? maskedServerPayload(opcode, plain) : null;
        String payload = sensitive == null ? Protocol.hex(plain) : sensitive;
        frameSequence = Math.incrementExact(frameSequence);
        writer.write("{\"capture_id\":\"" + captureId + "\",\"sequence\":" + frameSequence
                + ",\"connection_id\":" + (connection == null ? "null" : connection.id)
                + ",\"endpoint\":" + nullableString(connection == null ? null : connection.endpoint)
                + ",\"phase\":" + nullableString(phase)
                + ",\"observation\":" + nullableString(observation)
                + ",\"time\":\"" + Instant.now() + "\",\"direction\":\"" + direction
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

    @Override public synchronized void close() throws IOException { if (formal != null) formal.close(); else writer.close(); }

    private static String nullableString(String value) {
        return value == null ? "null" : "\"" + escape(value) + "\"";
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
    }
}

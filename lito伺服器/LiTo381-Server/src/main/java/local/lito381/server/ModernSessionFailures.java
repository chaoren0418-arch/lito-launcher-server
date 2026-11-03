package local.lito381.server;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLongArray;
import javax.net.ssl.SSLException;

/** Bounded diagnostic categories only. Never retain an exception, message,
 * cause, stack, principal, peer address or frame. This is not a success policy. */
final class ModernSessionFailures {
    enum Stage { SETUP, NEGOTIATION, APPLICATION_READ, APPLICATION_WRITE, TLS_CLOSE, OWNER_CLEANUP, DISPATCH }
    enum Kind { TIMEOUT, TLS, EOF, SOCKET, PROTOCOL, IO, RUNTIME, OTHER }
    record Point(Stage stage, Kind kind) {
        Point { Objects.requireNonNull(stage); Objects.requireNonNull(kind); }
    }
    record Count(Point point, long count) {}
    private final AtomicLongArray counts = new AtomicLongArray(Stage.values().length * Kind.values().length);

    static Point point(Stage stage, Throwable failure) {
        Objects.requireNonNull(failure);
        Kind kind = failure instanceof SocketTimeoutException ? Kind.TIMEOUT
                : failure instanceof SSLException ? Kind.TLS
                : failure instanceof EOFException ? Kind.EOF
                : failure instanceof SocketException ? Kind.SOCKET
                : failure instanceof ModernApplicationProtocol.ProtocolException || failure instanceof ModernSessionProtocol.ProtocolException ? Kind.PROTOCOL
                : failure instanceof IOException ? Kind.IO
                : failure instanceof RuntimeException ? Kind.RUNTIME : Kind.OTHER;
        return new Point(stage, kind);
    }
    void record(Point point) {
        Objects.requireNonNull(point);
        counts.updateAndGet(point.stage().ordinal() * Kind.values().length + point.kind().ordinal(),
                value -> value == Long.MAX_VALUE ? value : value + 1);
    }
    /** Immutable point-in-time counters; coherent totals require retired sessions. */
    List<Count> snapshot() {
        var result = new ArrayList<Count>();
        for (var stage : Stage.values()) for (var kind : Kind.values()) {
            long count = counts.get(stage.ordinal() * Kind.values().length + kind.ordinal());
            if (count != 0) result.add(new Count(new Point(stage, kind), count));
        }
        return List.copyOf(result);
    }
}

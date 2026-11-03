package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.*;
import javax.net.ssl.SSLException;
import org.junit.jupiter.api.Test;

class ModernSessionFailuresTest {
    @Test void onlyFixedCategoriesSurviveAndNoExceptionContentIsInspected() {
        var opaque = new IOException("must-not-retain") {
            @Override public String getMessage() { throw new AssertionError("message read"); }
            @Override public synchronized Throwable getCause() { throw new AssertionError("cause read"); }
            @Override public StackTraceElement[] getStackTrace() { throw new AssertionError("stack read"); }
        };
        var counts = new ModernSessionFailures();
        counts.record(ModernSessionFailures.point(ModernSessionFailures.Stage.APPLICATION_READ, opaque));
        var saved = counts.snapshot();
        assertEquals(List.of(new ModernSessionFailures.Count(new ModernSessionFailures.Point(
                ModernSessionFailures.Stage.APPLICATION_READ, ModernSessionFailures.Kind.IO), 1)), saved);
        assertThrows(UnsupportedOperationException.class, saved::clear);
        assertFalse(saved.toString().contains("must-not-retain"));
        counts.record(ModernSessionFailures.point(ModernSessionFailures.Stage.APPLICATION_WRITE, new SocketException("secret")));
        assertEquals(1, saved.size()); assertEquals(2, counts.snapshot().size());
    }
    @Test void categoriesUseSpecificTypesWithoutUnwrappingPrivateCauses() {
        var errors = List.of(new SocketTimeoutException(), new SSLException("private"), new EOFException(),
                new SocketException(), new ModernApplicationProtocol.ProtocolException(), new IOException(), new IllegalStateException(), new Exception());
        var kinds = List.of(ModernSessionFailures.Kind.values());
        for (int i=0;i<errors.size();i++) assertEquals(kinds.get(i),
                ModernSessionFailures.point(ModernSessionFailures.Stage.DISPATCH, errors.get(i)).kind());
        assertThrows(NullPointerException.class, () -> ModernSessionFailures.point(ModernSessionFailures.Stage.SETUP, null));
    }
    @Test void concurrentCountsHaveFixedCardinalityAndDoNotLoseFailures() throws Exception {
        var counts = new ModernSessionFailures(); var workers = Executors.newFixedThreadPool(4);
        try {
            var jobs = new java.util.ArrayList<Future<?>>();
            for (int i=0;i<4;i++) jobs.add(workers.submit(() -> {
                for (int j=0;j<500;j++) counts.record(new ModernSessionFailures.Point(
                        ModernSessionFailures.Stage.APPLICATION_READ, ModernSessionFailures.Kind.SOCKET));
            }));
            for (var job : jobs) job.get(3, TimeUnit.SECONDS);
            assertEquals(1, counts.snapshot().size()); assertEquals(2000, counts.snapshot().get(0).count());
        } finally { workers.shutdownNow(); assertTrue(workers.awaitTermination(3, TimeUnit.SECONDS)); }
    }
}

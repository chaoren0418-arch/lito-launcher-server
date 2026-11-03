package local.lito381.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Controlled cancellation against the real listener, not a fake EOF counter.
 * Synthetic repositories only; no resource, account SQL or production process. */
public final class ModernSessionFailureTestPeer {
    public static void main(String[] args) {
        var watchdog = Executors.newSingleThreadScheduledExecutor(r -> {
            var thread = new Thread(r, "owned-session-failure-deadline"); thread.setDaemon(true); return thread;
        });
        try {
            if (args.length != 4 || !args[0].equals("serve") || !Set.of("idle", "held-characters").contains(args[2])
                    || !args[3].equals("15000")) throw new IllegalArgumentException();
            watchdog.schedule(() -> System.exit(3), 15, TimeUnit.SECONDS);
            run(Path.of(args[1]), args[2].equals("held-characters"));
        } catch (Throwable failure) {
            // Deliberately no raw exception details or borrowed credentials.
            System.err.println("MODERN_SESSION_FAILURE_PEER_FAILED " + failure.getClass().getSimpleName());
            Arrays.stream(failure.getStackTrace()).limit(4).forEach(System.err::println); System.exit(2);
        } finally { watchdog.shutdownNow(); }
    }
    private static void run(Path fixtures, boolean held) throws Exception {
        var backend = new ModernApplicationTestBackend("auth");
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1); var reads = new AtomicInteger();
        ModernCharacterRepository characters = (principal, source) -> {
            require(reads.incrementAndGet() == 1); entered.countDown();
            if (held) require(release.await(5, TimeUnit.SECONDS));
            return backend.characters.characters(principal, source);
        };
        var server = new ModernServer(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0),
                ModernTlsTestMaterial.serverContext(fixtures, false), new ModernServer.Limits(2, 1, 600, 2, 1, 2, 6000, 2000, 2000, 10000),
                backend.verifier, characters);
        try {
            System.out.println("READY " + server.address().getPort()); System.out.flush();
            require(entered.await(5, TimeUnit.SECONDS));
            if (held) {
                System.out.println("READ_HELD"); System.out.flush();
                // Rust releases only after its real controller worker has retired.
                require(Arrays.equals(System.in.readNBytes(8), new byte[]{'R','E','L','E','A','S','E','\n'}));
                release.countDown();
            }
            long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (server.snapshot().sessions() != 0) { require(System.nanoTime() < end); Thread.sleep(5); }
            require(server.snapshot().accepted() == 1 && !server.listenerFailed());
            require(backend.authenticationCalls.get() == 2 && reads.get() == 1);
            require(server.snapshot().failed() == (held ? 1 : 0));
            var counts = server.failureCounts();
            if (held) {
                require(counts.size() == 1 && counts.get(0).count() == 1);
                var point = counts.get(0).point();
                require(Set.of(ModernSessionFailures.Stage.APPLICATION_READ, ModernSessionFailures.Stage.APPLICATION_WRITE).contains(point.stage()));
                require(Set.of(ModernSessionFailures.Kind.SOCKET, ModernSessionFailures.Kind.TLS, ModernSessionFailures.Kind.EOF).contains(point.kind()));
                System.out.println("DIAG " + point.stage() + " " + point.kind() + " 1");
            } else { require(counts.isEmpty()); System.out.println("DIAG CLEAN 0"); }
            System.out.flush();
        } finally {
            release.countDown(); server.close(); require(server.awaitTermination(3000));
        }
        require(server.snapshot().sessions() == 0 && server.snapshot().backendActive() == 0 && server.snapshot().backendQueued() == 0);
    }
    private static void require(boolean value) { if (!value) throw new IllegalStateException(); }
}

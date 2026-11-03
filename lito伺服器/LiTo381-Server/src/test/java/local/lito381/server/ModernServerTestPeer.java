package local.lito381.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Rust interop against the production listener, with synthetic repositories only. */
public final class ModernServerTestPeer {
    private ModernServerTestPeer() {}
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 4 || !"serve".equals(arguments[0]) || !Set.of("auth", "data", "data-large").contains(arguments[2])) {
            throw new IllegalArgumentException("serve <fixture-dir> <auth|data|data-large> <lifetime-ms>");
        }
        int lifetime = Integer.parseInt(arguments[3]);
        if (lifetime < 1000 || lifetime > 30_000) throw new IllegalArgumentException("bounded test lifetime required");
        var tls = ModernTlsIdentity.load(Path.of(arguments[1]).resolve("server.p12"),
                "modern-lito-fixture-only".toCharArray());
        var repository = new ModernApplicationTestBackend("auth");
        var data = new ModernCharacterDataTestFixtures("data-large".equals(arguments[2]));
        boolean requireData = !"auth".equals(arguments[2]);
        var presence = new TournamentPresence();
        var limits = new ModernServer.Limits(4, 2, 600, 4, 2, 4, 2000, 2000, 2000, 5000);
        var server = new ModernServer(new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 0),
                tls, limits, repository.verifier, repository.characters, presence,
                requireData ? data : ModernCharacterDataRepository.unavailable());
        try {
            System.out.println("READY " + server.address().getPort());
            System.out.flush();
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(lifetime);
            for (;;) {
                var state = server.snapshot();
                if (state.accepted() == 1 && state.sessions() == 0) break;
                if (server.listenerFailed() || System.nanoTime() >= deadline) {
                    throw new IllegalStateException("production listener test did not complete");
                }
                Thread.sleep(10);
            }
            if (repository.authenticationCalls.get() != 2) {
                throw new IllegalStateException("both independently verified source accounts are required");
            }
            if (requireData && (data.completedReads.get() == 0 || server.snapshot().failed() != 0)) {
                throw new IllegalStateException("production character data was not successfully exercised");
            }
        } finally {
            server.close();
            if (!server.awaitTermination(3000)) throw new IllegalStateException("production listener workers did not retire");
            var finalState = server.snapshot();
            if (finalState.sessions() != 0 || finalState.backendActive() != 0 || finalState.backendQueued() != 0
                    || presence.ownerCount() != 0 || presence.userCount() != 0) {
                throw new IllegalStateException("production listener retained owned work");
            }
        }
    }
}

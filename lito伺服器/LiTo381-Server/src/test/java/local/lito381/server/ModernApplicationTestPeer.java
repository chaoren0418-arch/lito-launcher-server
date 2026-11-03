package local.lito381.server;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

/** Bounded one-connection ephemeral loopback AUTH/listing peer for Rust interop. */
public final class ModernApplicationTestPeer {
    private ModernApplicationTestPeer() {}
    public static void main(String[] arguments) throws Exception {
        if (arguments.length < 3 || arguments.length > 4 || !arguments[0].equals("serve")) {
            throw new IllegalArgumentException("serve <fixture-dir> auth|auth-backend|characters-backend|characters-slow [lifetime-ms]");
        }
        String mode = arguments[2];
        if (!Set.of("auth", "heartbeat", "auth-backend", "characters-backend", "characters-slow", "auth-wrong-id",
                "characters-wrong-epoch", "characters-wrong-source").contains(mode)) {
            throw new IllegalArgumentException("unknown application peer mode");
        }
        int lifetime = arguments.length == 4 ? Integer.parseInt(arguments[3]) : 30_000;
        if (lifetime < 1000 || lifetime > 120_000) throw new IllegalArgumentException("invalid test lifetime");
        var context = ModernTlsTestMaterial.serverContext(Path.of(arguments[1]), false);
        var backend = new ModernApplicationTestBackend(mode);
        var backendExecutor = new ModernBackendExecutor(2, 2);
        try (backendExecutor; var listener = (SSLServerSocket) context.getServerSocketFactory().createServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"))) {
            listener.setEnabledProtocols(new String[] {"TLSv1.3", "TLSv1.2"});
            listener.setSoTimeout(10_000);
            System.out.println("READY " + listener.getLocalPort());
            System.out.flush();
            try (var socket = (SSLSocket) listener.accept();
                 var transport = new ModernTlsSessionTransport(socket, ModernSessionBoundary.Policy.current(),
                         ModernTlsSessionTransport.Limits.defaults(), backend.verifier)) {
                var timer = Executors.newSingleThreadScheduledExecutor(task -> {
                    Thread thread = new Thread(task, "modern-application-fixture-lifetime");
                    thread.setDaemon(true); return thread;
                });
                var stop = timer.schedule(() -> { try { transport.close(); } catch (Exception ignored) {} },
                        lifetime, TimeUnit.MILLISECONDS);
                try {
                    if (mode.contains("wrong-")) {
                        var gate = backendExecutor.openSession();
                        transport.attachBackend(gate, 5_000);
                        adversarial(transport, backend, gate, mode);
                    } else new ModernApplicationSession(transport, backend.characters, backendExecutor,
                            mode.equals("heartbeat") ? 3_000 : 10_000, 5_000, new TournamentPresence()).run();
                }
                finally { stop.cancel(false); timer.shutdownNow(); }
            }
        } finally {
            backendExecutor.close();
            if (!backendExecutor.awaitTermination(10_000)) throw new IllegalStateException("test backend failed to retire");
        }
    }

    /** Real TLS/verifier/rows; only the selected response correlation field is corrupted. */
    private static void adversarial(ModernTlsSessionTransport transport, ModernApplicationTestBackend backend,
                                    ModernBackendExecutor.SessionGate gate, String mode) throws Exception {
        transport.negotiate();
        while (true) {
            try (var message = transport.readApplication(10_000)) {
                if (message == null) return;
                if (message instanceof ModernApplicationProtocol.Ping ping) {
                    transport.writeApplication(new ModernApplicationProtocol.Pong(ping.requestId()));
                } else if (message instanceof ModernApplicationProtocol.Auth auth) {
                    var principal = transport.authenticateForApplication(auth.account(), auth.password());
                    var sources = principal.sources().stream().sorted(java.util.Comparator.comparingInt(ModernAccountAuthentication.Source::id))
                            .map(source -> new ModernApplicationProtocol.Source(source.id(), source.name())).toList();
                    int id = mode.equals("auth-wrong-id") ? (auth.requestId() == -1 ? 1 : auth.requestId() + 1) : auth.requestId();
                    transport.writeApplication(new ModernApplicationProtocol.AuthResult(id, 0, principal.account(), sources));
                    if (mode.equals("auth-wrong-id")) return;
                } else if (message instanceof ModernApplicationProtocol.CharacterRequest request) {
                    var principal = transport.authenticatedPrincipal().orElseThrow();
                    var rows = gate.call(5_000, scope -> backend.characters.characters(principal, request.sourceId()));
                    int sourceId = mode.equals("characters-wrong-source") ? (request.sourceId() == 7 ? 42 : 7) : request.sourceId();
                    long epoch = request.selectionEpoch();
                    if (mode.equals("characters-wrong-epoch")) {
                        epoch ^= Long.MIN_VALUE;
                        if (epoch == 0) epoch = 1;
                    }
                    transport.writeApplication(new ModernApplicationProtocol.CharacterResult(request.requestId(), sourceId, epoch, 0, rows));
                    return;
                } else throw new ModernApplicationProtocol.ProtocolException();
            }
        }
    }
}

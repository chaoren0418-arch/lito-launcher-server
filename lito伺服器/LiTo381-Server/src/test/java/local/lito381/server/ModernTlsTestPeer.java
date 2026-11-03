package local.lito381.server;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.Set;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

/** Test-only, one-connection ephemeral LOOPBACK peer for Java/Rust TLS interop. */
public final class ModernTlsTestPeer {
    private ModernTlsTestPeer() {}
    public static void main(String[] arguments) throws Exception {
        if (arguments.length == 2 && arguments[0].equals("prepare")) {
            ModernTlsTestMaterial.prepare(Path.of(arguments[1]));
            System.out.println("FIXTURES_READY");
            return;
        }
        if (arguments.length < 3 || arguments.length > 4 || !arguments[0].equals("serve")) {
            throw new IllegalArgumentException("prepare <target-dir> | serve <target-dir> <mode> [hold-ms]");
        }
        Path directory = Path.of(arguments[1]);
        String mode = arguments[2];
        if (!Set.of("tls", "tls12", "fragmented", "stall", "handshake-stall", "plain", "expired",
                "oversized", "bad-ack-id", "slow-ack").contains(mode)) {
            throw new IllegalArgumentException("unknown test peer mode");
        }
        int hold = arguments.length == 4 ? Integer.parseInt(arguments[3])
                : (mode.contains("stall") ? 5_000 : 2_000);
        if (hold < 50 || hold > 10_000) throw new IllegalArgumentException("hold-ms must be 50..10000");
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        if (mode.equals("plain") || mode.equals("handshake-stall")) {
            try (var listener = new ServerSocket(0, 1, loopback)) {
                listener.setSoTimeout(10_000);
                ready(listener.getLocalPort());
                try (Socket socket = listener.accept()) {
                    if (mode.equals("plain")) {
                        socket.getOutputStream().write("not TLS: LTM1 test peer\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
                        socket.getOutputStream().flush();
                    }
                    Thread.sleep(hold);
                }
            }
            return;
        }
        var context = ModernTlsTestMaterial.serverContext(directory, mode.equals("expired"));
        try (var listener = (SSLServerSocket) context.getServerSocketFactory().createServerSocket(0, 1, loopback)) {
            listener.setSoTimeout(10_000);
            listener.setEnabledProtocols(mode.equals("tls12") ? new String[] {"TLSv1.2"}
                    : new String[] {"TLSv1.3", "TLSv1.2"});
            ready(listener.getLocalPort());
            try (var socket = (SSLSocket) listener.accept()) {
                socket.setSoTimeout(5_000);
                if (Set.of("fragmented", "slow-ack", "stall", "oversized", "bad-ack-id").contains(mode)) {
                    // Adversarial peer modes deliberately control ACK delivery,
                    // but use real TLS and the actual bounded HELLO boundary.
                    socket.startHandshake();
                    byte[] hello = ModernSessionProtocol.readFrame(socket.getInputStream());
                    byte[] ack = new ModernSessionBoundary(ModernSessionBoundary.Policy.current()).acceptHello(hello);
                    if (mode.equals("stall")) {
                        Thread.sleep(hold);
                        return;
                    }
                    if (mode.equals("oversized")) {
                        ack = java.util.Arrays.copyOf(ack, ModernSessionProtocol.HEADER_BYTES);
                        java.util.Arrays.fill(ack, 4, 8, (byte) 0xff);
                    } else if (mode.equals("bad-ack-id")) {
                        var decoded = (ModernSessionProtocol.Ack) ModernSessionProtocol.decode(ack);
                        int wrongId = decoded.requestId() ^ 0x40000000;
                        if (wrongId == 0) wrongId = 1;
                        ack = ModernSessionProtocol.encode(new ModernSessionProtocol.Ack(wrongId,
                                decoded.version(), decoded.numericSchema(), decoded.capabilities()));
                    }
                    if (mode.equals("fragmented") || mode.equals("slow-ack")) {
                        for (byte value : ack) {
                            socket.getOutputStream().write(value & 0xff);
                            socket.getOutputStream().flush();
                            Thread.sleep(mode.equals("slow-ack") ? 150 : 5);
                        }
                    } else {
                        socket.getOutputStream().write(ack);
                        socket.getOutputStream().flush();
                    }
                    awaitClose(socket, hold);
                } else {
                    try (var transport = new ModernTlsSessionTransport(socket,
                            ModernSessionBoundary.Policy.current(), ModernTlsSessionTransport.Limits.defaults(),
                            (account, password) -> { throw new AssertionError("HELLO must never authenticate"); })) {
                        transport.negotiate();
                        if (transport.state() != ModernTlsSessionTransport.State.NEGOTIATED
                                || transport.authenticatedPrincipal().isPresent()) {
                            throw new AssertionError("TLS HELLO was confused with account authentication");
                        }
                        awaitClose(socket, hold);
                    }
                }
            }
        }
    }
    private static void ready(int port) {
        System.out.println("READY " + port);
        System.out.flush();
    }
    private static void awaitClose(SSLSocket socket, int milliseconds) throws Exception {
        socket.setSoTimeout(milliseconds);
        try {
            if (socket.getInputStream().read() != -1) throw new IllegalArgumentException("AUTH/gameplay wire is not implemented");
        } catch (SocketTimeoutException expected) {
            // The test peer owns a bounded hold only, not a permanent listener.
        }
    }
}

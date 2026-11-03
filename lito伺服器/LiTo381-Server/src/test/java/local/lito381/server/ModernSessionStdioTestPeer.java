package local.lito381.server;

/**
 * Test-only cross-language peer. Compiled under target/test-classes, never part
 * of the server jar. Reads one real Rust-generated frame and runs the actual
 * server boundary; stdout is binary ACK only. No listeners/accounts/database.
 */
public final class ModernSessionStdioTestPeer {
    private ModernSessionStdioTestPeer() {}

    public static void main(String[] arguments) throws Exception {
        ModernSessionBoundary.Policy policy;
        if (arguments.length == 0) {
            policy = ModernSessionBoundary.Policy.current();
        } else if (arguments.length == 1 && "--stats-only".equals(arguments[0])) {
            policy = new ModernSessionBoundary.Policy(
                    ModernSessionProtocol.CAP_EXPLICIT_I64_STATS,
                    ModernSessionProtocol.CAP_EXPLICIT_I64_STATS);
        } else {
            throw new IllegalArgumentException("test peer accepts only --stats-only");
        }
        var boundary = new ModernSessionBoundary(policy);
        byte[] request = ModernSessionProtocol.readFrame(System.in);
        System.out.write(boundary.acceptHello(request));
        System.out.flush();
    }
}

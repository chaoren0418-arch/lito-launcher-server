package local.lito381.server;

import java.io.InputStream;
import java.io.PrintStream;

/** Dedicated artifact entry: never falls through to native configuration or migrations. */
public final class ModernStandaloneMain {
    private ModernStandaloneMain() {}

    public static void main(String[] args) {
        int code = run(args, System.in, System.out, System.err);
        if (code != 0) System.exit(code);
    }

    static int run(String[] args, InputStream input, PrintStream output, PrintStream errors) {
        if (args.length == 1 && args[0].equals("--version")) {
            String version = ModernStandaloneMain.class.getPackage().getImplementationVersion();
            if (version == null || !version.matches("[0-9]+\\.[0-9]+\\.[0-9]+")) version = "development";
            output.println("{\"schema\":1,\"application\":\"LiToModernServer\",\"version\":\"" + version
                    + "\",\"status\":\"development_candidate\",\"max_protocol\":" + ModernSessionProtocol.MAX_VERSION
                    + ",\"extensions\":[\"player_healing_v1\"],\"native_entry\":false}");
            return 0;
        }
        if (!validStart(args)) {
            errors.println("usage: java -jar LiToModern-Server.jar --modern-config <absolute properties path> [--console]");
            return 2;
        }
        try {
            ModernMain.run(new String[]{args[0], args[1]}, args.length == 3 ? input : null, output);
            return 0;
        } catch (Exception failure) {
            // Provider failures may contain SQL URLs, paths or credentials.
            errors.println("modern server startup or termination failed; check its explicit configuration");
            return 2;
        }
    }

    static boolean validStart(String[] args) {
        return (args.length == 2 || args.length == 3 && args[2].equals("--console"))
                && args[0].equals("--modern-config") && !args[1].isBlank();
    }
}

package local.lito381.server;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;

/**
 * JUnit assume gates for tests that hard-read developer-machine files.
 *
 * <p>Helpers check the exact Path the calling test will open. They do not
 * pass because a different candidate (override, absolute twin, system
 * property) exists.
 */
final class LocalResources {
    static final Path SERVER_PROPERTIES = Path.of("config", "server.properties");
    static final Path LINEAGE_SQL = Path.of("..", "1-伺服器", "db", "380_db.sql");
    static final Path LITO_BIN = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    static final Path IDA_DISASSEMBLY = Path.of(
            "..", "LiTo381-開發", "ida-client-oep-v2", "full-disassembly.asm");
    static final Path IDA_DISASSEMBLY_ABSOLUTE = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");
    static final Path LITO_UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    static final Path LITO_UNPACKED_RELATIVE = Path.of(
            "..", "LiTo381-開發", "runtime", "LiTo.unpacked-oep-current.exe");

    private LocalResources() {}

    static void assumeServerProperties() {
        assumeExisting("config/server.properties", SERVER_PROPERTIES);
    }

    static void assumeLineageSources() {
        assumeExisting("lito伺服器/1-伺服器/db/380_db.sql", LINEAGE_SQL);
    }

    /** The D:/380 LiTo.bin path tests hard-code as SELECTED_CLIENT. */
    static void assumeLitoBin() {
        assumeExisting("D:/380/.../LiTo.bin", LITO_BIN);
    }

    static void assumeLitoBin(Path used) {
        assumeExisting("LiTo.bin", used);
    }

    static void assumeIdaClient(Path used) {
        assumeExisting("ida-client full-disassembly.asm", used);
    }

    static void assumeWindowsJvm() {
        Path javaExe = Path.of(System.getProperty("java.home"), "bin", "java.exe");
        assumeExisting("Windows java.exe process peer", javaExe);
    }

    /**
     * Same resolver tests use: environment variable only, then the directory.
     * A {@code -Dlito.shared.resource.root} property does not count.
     */
    static Path sharedResourceRoot() {
        String root = System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(root != null && !root.isBlank(),
                "skipped: LITO_SHARED_RESOURCE_ROOT");
        Path path = Path.of(root);
        Assumptions.assumeTrue(Files.isDirectory(path),
                "skipped: LITO_SHARED_RESOURCE_ROOT (" + path.toAbsolutePath() + ")");
        return path;
    }

    static void assumeSharedResourceRoot() {
        sharedResourceRoot();
    }

    static void assumeExisting(String label, Path path) {
        Assumptions.assumeTrue(path != null && Files.exists(path),
                "skipped: missing local " + label + " ("
                        + (path == null ? "null" : path.toAbsolutePath()) + ")");
    }

    static boolean present(Path path) {
        return path != null && Files.isRegularFile(path);
    }

    /** Current ClientSession requires a non-null capture log even for hub-only tests. */
    static CaptureLog discardCapture() {
        try {
            Path directory = Files.createTempDirectory("lito-capture-");
            directory.toFile().deleteOnExit();
            return new CaptureLog(directory);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

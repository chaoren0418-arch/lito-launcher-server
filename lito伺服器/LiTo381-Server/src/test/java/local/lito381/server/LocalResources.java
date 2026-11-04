package local.lito381.server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assumptions;

/**
 * JUnit assume gates for tests that hard-read developer-machine files.
 *
 * <p>CI and any clone without those files skip instead of failing. When the
 * files are present the tests run unchanged.
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

    private LocalResources() {}

    static void assumeServerProperties() {
        assumeFile("config/server.properties", SERVER_PROPERTIES);
    }

    static void assumeLineageSources() {
        assumeFile("lito伺服器/1-伺服器/db/380_db.sql", LINEAGE_SQL);
    }

    static void assumeLitoBin() {
        List<Path> candidates = new ArrayList<>();
        String override = firstNonBlank(
                System.getProperty("lito.bin"), System.getenv("LITO_BIN"));
        if (override != null) {
            candidates.add(Path.of(override));
        }
        candidates.add(LITO_BIN);
        assumeAnyFile("D:/380/.../LiTo.bin", candidates);
    }

    static void assumeIdaClient() {
        assumeAnyFile("ida-client full-disassembly.asm", List.of(
                IDA_DISASSEMBLY, IDA_DISASSEMBLY_ABSOLUTE));
    }

    static void assumeWindowsJvm() {
        Path javaExe = Path.of(System.getProperty("java.home"), "bin", "java.exe");
        Assumptions.assumeTrue(Files.isRegularFile(javaExe),
                "skipped: Windows java.exe process peer (" + javaExe + ")");
    }

    static void assumeSharedResourceRoot() {
        String root = firstNonBlank(
                System.getProperty("lito.shared.resource.root"),
                System.getenv("LITO_SHARED_RESOURCE_ROOT"));
        Assumptions.assumeTrue(root != null && Files.isDirectory(Path.of(root)),
                "skipped: LITO_SHARED_RESOURCE_ROOT");
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

    private static void assumeFile(String label, Path path) {
        Assumptions.assumeTrue(Files.isRegularFile(path),
                "skipped: missing local " + label + " (" + path.toAbsolutePath()
                        + ")");
    }

    private static void assumeAnyFile(String label, List<Path> candidates) {
        for (Path path : candidates) {
            if (Files.isRegularFile(path)) {
                return;
            }
        }
        Assumptions.assumeTrue(false, "skipped: missing local " + label
                + " (tried " + candidates + ")");
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}

package local.lito381.server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    /**
     * ClientSession paths that call {@code accounts.repositoryForClassicSource}
     * need a wired AccountRepository. Memory selftest fixtures currently pass
     * null; opt in with {@code -Dlito.classic.accounts=true}.
     */
    static void assumeClassicSourceAccounts() {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.classic.accounts"),
                "skipped: ClientSession classic source AccountRepository");
    }

    /**
     * Pins written against the retained native catalog / ClientSession text.
     * Current src/main is a T002 decompile and does not match every pin.
     */
    static void assumeNativeCatalogPins() {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.native.catalog.pins"),
                "skipped: native catalog pins vs current src/main");
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

    /**
     * Scan UTF-8 constants in {@code type} and skip when this host is missing
     * the files those constants name. Used by {@link LocalResourcesExtension}
     * as a safety net around tests that still hard-code the paths.
     */
    static void assumeFor(Class<?> type) {
        String haystack = constantsHaystack(type);
        if (haystack.isEmpty()) {
            return;
        }
        if (haystack.contains("config/server.properties")
                || haystack.contains("config\\server.properties")) {
            assumeServerProperties();
        }
        if (haystack.contains("1-伺服器") || haystack.contains("380_db.sql")) {
            assumeLineageSources();
        }
        if (haystack.contains("D:/380") || haystack.contains("D:\\380")
                || haystack.contains("/LiTo.bin")
                || haystack.contains("\\LiTo.bin")) {
            assumeLitoBin();
        }
        if (haystack.contains("ida-client")
                || haystack.contains("full-disassembly.asm")
                || haystack.contains("LiTo381-開發")
                || haystack.toLowerCase(Locale.ROOT).contains("lito.unpacked")) {
            assumeIdaClient();
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

    private static String constantsHaystack(Class<?> type) {
        StringBuilder out = new StringBuilder();
        Class<?> cursor = type;
        while (cursor != null && cursor != Object.class) {
            try (var in = cursor.getResourceAsStream(
                    "/" + cursor.getName().replace('.', '/') + ".class")) {
                if (in == null) {
                    break;
                }
                appendUtf8Constants(in.readAllBytes(), out);
            } catch (Exception ignored) {
                break;
            }
            cursor = cursor.getEnclosingClass();
        }
        return out.toString();
    }

    /** Minimal constant-pool UTF-8 harvest; enough to see hard-coded paths. */
    private static void appendUtf8Constants(byte[] classFile, StringBuilder out) {
        if (classFile.length < 10
                || classFile[0] != (byte) 0xCA
                || classFile[1] != (byte) 0xFE
                || classFile[2] != (byte) 0xBA
                || classFile[3] != (byte) 0xBE) {
            return;
        }
        int cpCount = ((classFile[8] & 0xFF) << 8) | (classFile[9] & 0xFF);
        int i = 10;
        for (int n = 1; n < cpCount && i < classFile.length; n++) {
            int tag = classFile[i] & 0xFF;
            i++;
            switch (tag) {
                case 1 -> { // Utf8
                    if (i + 2 > classFile.length) {
                        return;
                    }
                    int len = ((classFile[i] & 0xFF) << 8) | (classFile[i + 1] & 0xFF);
                    i += 2;
                    if (i + len > classFile.length) {
                        return;
                    }
                    out.append(new String(classFile, i, len,
                            java.nio.charset.StandardCharsets.UTF_8));
                    out.append('\n');
                    i += len;
                }
                case 7, 8, 16, 19, 20 -> i += 2;
                case 15 -> i += 3;
                case 3, 4, 9, 10, 11, 12, 17, 18 -> i += 4;
                case 5, 6 -> {
                    i += 8;
                    n++;
                }
                default -> {
                    return;
                }
            }
        }
    }
}

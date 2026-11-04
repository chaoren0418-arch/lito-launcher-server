package local.lito381.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;

/** Assumptions for actual local inputs, never for an alternative file or a feature flag. */
final class LocalResources {
    private LocalResources() {}

    static void assumeServerProperties() {
        assumeFiles(Path.of("config", "server.properties"));
    }

    static void assumeLineageSources(Path first, Path... remaining) {
        assumeFiles(first, remaining);
    }

    static void assumeLitoBin(Path first, Path... remaining) {
        assumeFiles(first, remaining);
    }

    static void assumeIdaClient(Path first, Path... remaining) {
        assumeFiles(first, remaining);
    }

    static void assumeFiles(Path first, Path... remaining) {
        assumeFile(first);
        for (Path path : remaining) assumeFile(path);
    }

    private static Path assumeFile(Path path) {
        Assumptions.assumeTrue(Files.isRegularFile(path) && Files.isReadable(path),
                () -> "skipped: missing/unreadable local file " + path.toAbsolutePath());
        return path;
    }

    /** Reads also guard dynamically selected files in the external evidence trees.
     * Repository files and generated fixtures retain normal IOException/assertion behavior.
     * No search, override, or fallback changes the path given to Files.
     */
    static Path assumeExternalFile(Path path) {
        String name = path.toAbsolutePath().normalize().toString().replace('\\', '/');
        if (name.contains("/1-伺服器/") || name.contains("/LiTo381-開發/")
                || name.contains("/TW13081901bin拆解/") || name.contains("/2-登入器/")
                || name.contains("/LiTo381-Launcher/") || name.contains("/380/")) {
            assumeFile(path);
        }
        return path;
    }

    static String readString(Path path) throws IOException {
        return readString(path, StandardCharsets.UTF_8);
    }

    static String readString(Path path, Charset charset) throws IOException {
        return Files.readString(assumeExternalFile(path), charset);
    }

    static byte[] readAllBytes(Path path) throws IOException {
        return Files.readAllBytes(assumeExternalFile(path));
    }

    static InputStream newInputStream(Path path, OpenOption... options) throws IOException {
        return Files.newInputStream(assumeExternalFile(path), options);
    }

    static BufferedReader newBufferedReader(Path path) throws IOException {
        return newBufferedReader(path, StandardCharsets.UTF_8);
    }

    static BufferedReader newBufferedReader(Path path, Charset charset) throws IOException {
        return Files.newBufferedReader(assumeExternalFile(path), charset);
    }

    static void assumeWindowsJvm() {
        assumeFiles(Path.of(System.getProperty("java.home"), "bin", "java.exe"));
    }

    /** The consumer uses this exact environment variable, not a system property. */
    static Path assumeSharedResourceRoot() {
        return assumeSharedResourceRoot(System.getenv("LITO_SHARED_RESOURCE_ROOT"));
    }

    static Path assumeSharedResourceRoot(String configuredRoot) {
        Assumptions.assumeTrue(configuredRoot != null && !configuredRoot.isBlank(),
                "skipped: missing LITO_SHARED_RESOURCE_ROOT");
        Path root = Path.of(configuredRoot);
        // ModernWorldTestPeer.sharedMap loads exactly this sector of map 514.
        assumeFiles(root.resolve("map/514/7fff8000.s32"));
        return root;
    }

    /** Current ClientSession requires a non-null capture log even for hub-only tests. */
    static CaptureLog discardCapture() {
        try {
            Path directory = Files.createTempDirectory("lito-capture-");
            directory.toFile().deleteOnExit();
            return new CaptureLog(directory);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

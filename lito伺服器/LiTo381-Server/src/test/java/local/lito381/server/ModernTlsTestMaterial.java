package local.lito381.server;

import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/** Generated test-only PKI; no installation into the OS or production trust/key stores. */
final class ModernTlsTestMaterial {
    // Fixed password protects ONLY disposable generated test fixtures, never real accounts.
    private static final String TEST_PASSWORD = "modern-lito-fixture-only";
    private ModernTlsTestMaterial() {}

    static Path defaultDirectory() { return Path.of("target", "modern-tls-test-fixtures").toAbsolutePath(); }

    static void prepare(Path directory) throws Exception {
        directory = directory.toAbsolutePath().normalize();
        Files.createDirectories(directory);
        try (var channel = FileChannel.open(directory.resolve("generation.lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = channel.lock()) {
            if (Files.isRegularFile(directory.resolve("complete-v1"))) return;
            // Never overwrite a previous or user-supplied key. A partial fixture
            // requires a new test directory, not destructive automatic cleanup.
            if (Files.exists(directory.resolve("root.p12"))) {
                throw new IllegalStateException("incomplete TLS test fixture; choose a fresh target directory");
            }
            keytool(directory, "-genkeypair", "-alias", "ca", "-keystore", "root.p12",
                    "-storetype", "PKCS12", "-keyalg", "RSA", "-keysize", "2048",
                    "-sigalg", "SHA256withRSA", "-dname", "CN=Modern LiTo Disposable Test CA",
                    "-ext", "BC=ca:true,pathlen:0", "-ext", "KU=keyCertSign,cRLSign",
                    "-validity", "3650", "-startdate", "-1d");
            keytool(directory, "-exportcert", "-alias", "ca", "-keystore", "root.p12", "-file", "root.der");
            keytool(directory, "-exportcert", "-alias", "ca", "-keystore", "root.p12", "-rfc", "-file", "root.pem");
            leaf(directory, "server", false);
            leaf(directory, "expired", true);
            Files.writeString(directory.resolve("complete-v1"), "Generated test-only CA and localhost server chains.\n");
        }
    }

    private static void leaf(Path directory, String name, boolean expired) throws Exception {
        keytool(directory, "-genkeypair", "-alias", "server", "-keystore", name + ".p12",
                "-storetype", "PKCS12", "-keyalg", "RSA", "-keysize", "2048", "-sigalg", "SHA256withRSA",
                "-dname", "CN=localhost", "-validity", "365");
        keytool(directory, "-certreq", "-alias", "server", "-keystore", name + ".p12", "-file", name + ".csr");
        keytool(directory, "-gencert", "-alias", "ca", "-keystore", "root.p12",
                "-infile", name + ".csr", "-outfile", name + ".pem", "-rfc",
                "-ext", "SAN=DNS:localhost,IP:127.0.0.1", "-ext", "EKU=serverAuth",
                "-ext", "KU=digitalSignature,keyEncipherment", "-ext", "BC=ca:false",
                "-startdate", expired ? "-10d" : "-1d", "-validity", expired ? "1" : "365");
        keytool(directory, "-importcert", "-alias", "ca", "-keystore", name + ".p12", "-file", "root.pem");
        keytool(directory, "-importcert", "-alias", "server", "-keystore", name + ".p12", "-file", name + ".pem");
        keytool(directory, "-exportcert", "-alias", "server", "-keystore", name + ".p12", "-file", name + ".der");
    }

    private static void keytool(Path directory, String... arguments) throws Exception {
        String executable = System.getProperty("os.name").startsWith("Windows") ? "keytool.exe" : "keytool";
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", executable).toString());
        command.addAll(Arrays.asList(arguments));
        command.addAll(List.of("-storepass", TEST_PASSWORD, "-keypass", TEST_PASSWORD, "-noprompt"));
        Process process = new ProcessBuilder(command).directory(directory.toFile())
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        if (!process.waitFor(20, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
            throw new IllegalStateException("test keytool timed out");
        }
        if (process.exitValue() != 0) throw new IllegalStateException("test keytool failed at " + arguments[0]);
    }

    static SSLContext serverContext(Path directory, boolean expired) throws Exception {
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream input = Files.newInputStream(directory.resolve(expired ? "expired.p12" : "server.p12"))) {
            store.load(input, TEST_PASSWORD.toCharArray());
        }
        var keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keys.init(store, TEST_PASSWORD.toCharArray());
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keys.getKeyManagers(), null, null);
        return context;
    }

    /** A real validating trust manager, with only the generated test CA as root. */
    static SSLContext clientContext(Path directory) throws Exception {
        KeyStore trust = KeyStore.getInstance("PKCS12");
        trust.load(null, null);
        try (InputStream input = Files.newInputStream(directory.resolve("root.der"))) {
            trust.setCertificateEntry("ca", CertificateFactory.getInstance("X.509").generateCertificate(input));
        }
        var manager = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        manager.init(trust);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, manager.getTrustManagers(), null);
        return context;
    }
}

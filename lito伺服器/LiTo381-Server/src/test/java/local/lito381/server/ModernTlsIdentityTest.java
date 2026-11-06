package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernTlsIdentityTest {
    @BeforeAll static void fixtures() throws Exception { ModernTlsTestMaterial.prepare(ModernTlsTestMaterial.defaultDirectory()); }

    @Test void explicitPrivateIdentityLoadsAndAlwaysClearsTheBorrowedSecret() throws Exception {
        Path material = ModernTlsTestMaterial.defaultDirectory();
        char[] password = "modern-lito-fixture-only".toCharArray();
        assertNotNull(ModernTlsIdentity.load(material.resolve("server.p12"), password));
        assertArrayEquals(new char[password.length], password);
        char[] wrong = "not-the-test-secret".toCharArray();
        assertThrows(Exception.class, () -> ModernTlsIdentity.load(material.resolve("server.p12"), wrong));
        assertArrayEquals(new char[wrong.length], wrong);
    }

    @Test void expiredOrCaOnlyKeyCannotBecomeTheServerIdentity() {
        for (String name : new String[]{"expired.p12", "root.p12"}) {
            char[] password = "modern-lito-fixture-only".toCharArray();
            assertThrows(GeneralSecurityException.class,
                    () -> ModernTlsIdentity.load(ModernTlsTestMaterial.defaultDirectory().resolve(name), password));
            assertArrayEquals(new char[password.length], password);
        }
    }

    @Test void oversizedAndMissingFileCannotReachUnboundedKeystoreParsing(@TempDir Path temporary) throws Exception {
        Path oversized = temporary.resolve("oversized.p12");
        Files.write(oversized, new byte[ModernTlsIdentity.MAX_KEYSTORE_BYTES + 1]);
        for (Path file : new Path[]{oversized, temporary.resolve("missing.p12")}) {
            char[] password = "disposable-secret".toCharArray();
            assertThrows(Exception.class, () -> ModernTlsIdentity.load(file, password));
            assertArrayEquals(new char[password.length], password);
        }
    }

    @Test void explicitEkuMatchesTheRealRustlsClientRatherThanTreatingAnyPurposeAsWildcard(@TempDir Path temporary) throws Exception {
        // Separate disposable material: never change the cached shared fixture generator
        // or any user-supplied identity. These fixed credentials protect only test keys.
        Path clientOnly = generateIdentity(temporary, "client-only.p12", "clientAuth");
        char[] password = "modern-lito-fixture-only".toCharArray();
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (var input = Files.newInputStream(clientOnly)) { store.load(input, password); }
        assertTrue(store.isKeyEntry("server"));
        var leaf = (X509Certificate) store.getCertificate("server");
        leaf.checkValidity();
        assertEquals(-1, leaf.getBasicConstraints());
        assertEquals(List.of("1.3.6.1.5.5.7.3.2"), leaf.getExtendedKeyUsage());
        var rejected = assertThrows(GeneralSecurityException.class, () -> ModernTlsIdentity.load(clientOnly, password));
        assertEquals("modern TLS certificate does not permit server authentication", rejected.getMessage());
        assertArrayEquals(new char[password.length], password);

        Path anyPurpose = generateIdentity(temporary, "any-purpose.p12", "2.5.29.37.0");
        char[] anyPassword = "modern-lito-fixture-only".toCharArray();
        var rejectedAny = assertThrows(GeneralSecurityException.class, () -> ModernTlsIdentity.load(anyPurpose, anyPassword));
        assertEquals("modern TLS certificate does not permit server authentication", rejectedAny.getMessage());
        assertArrayEquals(new char[anyPassword.length], anyPassword);

        Path serverAndAny = generateIdentity(temporary, "server-and-any.p12", "serverAuth,2.5.29.37.0");
        char[] acceptedPassword = "modern-lito-fixture-only".toCharArray();
        assertNotNull(ModernTlsIdentity.load(serverAndAny, acceptedPassword));
        assertArrayEquals(new char[acceptedPassword.length], acceptedPassword);

        Path noEku = generateIdentity(temporary, "no-eku.p12", null);
        char[] unrestrictedPassword = "modern-lito-fixture-only".toCharArray();
        assertNotNull(ModernTlsIdentity.load(noEku, unrestrictedPassword));
        assertArrayEquals(new char[unrestrictedPassword.length], unrestrictedPassword);
    }

    private static Path generateIdentity(Path directory, String name, String purpose) throws Exception {
        Path target = directory.resolve(name);
        assertFalse(Files.exists(target), "never overwrite a previous identity fixture");
        String executable = System.getProperty("os.name").startsWith("Windows") ? "keytool.exe" : "keytool";
        var command = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", executable).toString(),
                "-genkeypair", "-alias", "server", "-keystore", target.toString(), "-storetype", "PKCS12",
                "-keyalg", "RSA", "-keysize", "2048", "-sigalg", "SHA256withRSA", "-dname", "CN=localhost",
                "-ext", "BC=ca:false", "-ext", "KU=digitalSignature,keyEncipherment",
                "-ext", "SAN=DNS:localhost,IP:127.0.0.1", "-startdate", "-1d", "-validity", "3",
                "-storepass", "modern-lito-fixture-only", "-keypass", "modern-lito-fixture-only", "-noprompt"));
        if (purpose != null) command.addAll(List.of("-ext", "EKU=" + purpose));
        Process process = new ProcessBuilder(command).directory(directory.toFile())
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        try {
            assertTrue(process.waitFor(20, TimeUnit.SECONDS), "owned identity fixture keytool exceeded its deadline");
            assertEquals(0, process.exitValue(), "identity fixture keytool failed");
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly(); // This is only the child created immediately above.
                assertTrue(process.waitFor(5, TimeUnit.SECONDS), "owned fixture keytool did not retire");
            }
        }
        return target;
    }

    @Test void formalMainRejectsUnsupportedModernArgumentsBeforeNativeStartup() {
        assertThrows(java.io.IOException.class, () -> Main.main(new String[]{"--modern-config"}));
        var failure = assertThrows(IllegalArgumentException.class,
                () -> Main.main(new String[]{"--modern-config", "relative.properties"}));
        assertEquals("unsupported argument: relative.properties", failure.getMessage());
    }

    @Test void dedicatedModernEntryRejectsInvalidInputWithoutLeakingThePath() {
        assertThrows(java.io.IOException.class, () -> ModernMain.run(new String[]{"--modern-config"}));
        assertThrows(java.io.IOException.class, () -> ModernMain.run(new String[]{"--modern-config", "relative.properties"}));
        var failure = assertThrows(java.io.IOException.class,
                () -> ModernMain.run(new String[]{"--modern-config", "relative-secret-marker.properties"}));
        assertFalse(failure.toString().contains("secret-marker"));
        assertNull(failure.getCause());
    }
}

package local.lito381.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

/** Explicit local server identity, never generates or installs production certificates. */
final class ModernTlsIdentity {
    static final int MAX_KEYSTORE_BYTES = 1024 * 1024;
    private ModernTlsIdentity() {}

    /** Consumes the supplied password copy on every path; JSSE owns its key material thereafter. */
    static SSLContext load(Path file, char[] password)
            throws IOException, GeneralSecurityException, ModernServerConfig.ConfigException {
        byte[] encoded = null;
        try {
            if (password == null || password.length == 0 || password.length > 4096) {
                throw new GeneralSecurityException("invalid modern TLS identity secret");
            }
            Path checked = ModernServerConfig.checkedLocalPath(file, true);
            try (var channel = FileChannel.open(checked, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
                if (channel.size() < 1 || channel.size() > MAX_KEYSTORE_BYTES) {
                    throw new IOException("modern TLS identity exceeds its bounded file limit");
                }
                encoded = Channels.newInputStream(channel).readNBytes(MAX_KEYSTORE_BYTES + 1);
                if (encoded.length > MAX_KEYSTORE_BYTES) throw new IOException("modern TLS identity grew beyond its limit");
            }
            KeyStore store = KeyStore.getInstance("PKCS12");
            store.load(new ByteArrayInputStream(encoded), password);
            int privateEntries = 0;
            var aliases = store.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (!store.isKeyEntry(alias)) continue;
                privateEntries++;
                var chain = store.getCertificateChain(alias);
                if (chain == null || chain.length == 0 || chain.length > 16
                        || !(chain[0] instanceof X509Certificate leaf) || leaf.getBasicConstraints() >= 0) {
                    throw new GeneralSecurityException("modern TLS requires an end-entity server certificate chain");
                }
                // A valid client-only certificate is not a usable server identity.
                // Missing EKU is unrestricted. The actual rustls/webpki client requires
                // explicit serverAuth when EKU exists; anyExtendedKeyUsage alone is NOT a wildcard.
                var purposes = leaf.getExtendedKeyUsage();
                if (purposes != null && !purposes.contains("1.3.6.1.5.5.7.3.1")) {
                    throw new GeneralSecurityException("modern TLS certificate does not permit server authentication");
                }
                for (var certificate : chain) {
                    if (!(certificate instanceof X509Certificate x509)) {
                        throw new GeneralSecurityException("modern TLS requires an X509 certificate chain");
                    }
                    x509.checkValidity();
                }
            }
            if (privateEntries != 1) throw new GeneralSecurityException("modern TLS requires exactly one server key entry");
            var keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keys.init(store, password);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(keys.getKeyManagers(), null, null);
            return context;
        } finally {
            if (password != null) Arrays.fill(password, '\0');
            if (encoded != null) Arrays.fill(encoded, (byte) 0);
        }
    }
}

// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import local.lito381.server.ServerConfig;
import local.lito381.server.UnifiedConfiguration;

final class NativeCombatConfigOrigin {
    static Path entry(ServerConfig serverConfig) throws IOException {
        return NativeCombatConfigOrigin.entry(serverConfig.source());
    }

    static Path entry(Path path) throws IOException {
        Properties properties;
        Path path2 = path.toAbsolutePath().normalize();
        try {
            properties = UnifiedConfiguration.read(path2);
        }
        catch (IOException iOException) {
            throw iOException;
        }
        catch (Exception exception) {
            throw new IOException("Cannot resolve combat configuration origin: " + String.valueOf(path2), exception);
        }
        String string = properties.getProperty("configuration.base.file");
        Path path3 = string == null ? path2 : path2.getParent().resolve(string).normalize();
        return path3.getParent().resolve("combat.properties");
    }

    private NativeCombatConfigOrigin() {
    }
}

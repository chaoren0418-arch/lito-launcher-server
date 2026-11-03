// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import local.lito381.server.NativeDeSpriteCatalog;
import local.lito381.server.ServerConfig;
import local.lito381.server.SharedDatabase;
import local.lito381.server.UnifiedConfiguration;

final class NativeDeSprites {
    private static volatile Snapshot current;
    private static final System.Logger LOG;
    private static final NativeDeSpriteCatalog.Reporter REPORTER;

    private NativeDeSprites() {
    }

    static synchronized void load(ServerConfig serverConfig) throws Exception {
        if (current != null) {
            return;
        }
        Properties properties = UnifiedConfiguration.read(serverConfig.source());
        String string = SharedDatabase.identifier(properties.getProperty("database.static.database"));
        NativeDeSpriteCatalog.Catalog catalog = NativeDeSpriteCatalog.empty();
        try (Connection connection = DriverManager.getConnection(properties.getProperty("database.jdbc.base.url") + string + properties.getProperty("database.jdbc.options"), properties.getProperty("database.user"), properties.getProperty("database.password"));){
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(4);
            try (AutoCloseable autoCloseable = connection::rollback;){
                catalog = NativeDeSpriteCatalog.read(connection, catalog, REPORTER);
            }
        }
        catch (SQLException sQLException) {
            REPORTER.sql(sQLException);
            catalog = new NativeDeSpriteCatalog.Catalog(catalog.sprites(), catalog.rows(), catalog.skipped(), true);
        }
        current = new Snapshot(string, catalog);
    }

    static Snapshot snapshot() {
        Snapshot snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Source sprite catalog not loaded at native resource startup");
        }
        return snapshot;
    }

    static NativeDeSpriteCatalog.Catalog catalog() {
        return NativeDeSprites.snapshot().catalog();
    }

    static {
        LOG = System.getLogger(NativeDeSprites.class.getName());
        REPORTER = new NativeDeSpriteCatalog.Reporter(){

            @Override
            public void row(int n, Exception exception) {
                LOG.log(System.Logger.Level.ERROR, "Source sprite row skipped: " + n, (Throwable)exception);
            }

            @Override
            public void sql(SQLException sQLException) {
                LOG.log(System.Logger.Level.ERROR, "Source sprite load retained partial values", (Throwable)sQLException);
            }
        };
    }

    record Snapshot(String database, NativeDeSpriteCatalog.Catalog catalog) {
    }
}

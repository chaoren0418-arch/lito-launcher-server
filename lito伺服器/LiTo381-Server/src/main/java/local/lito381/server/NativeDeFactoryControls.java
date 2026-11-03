// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.function.IntUnaryOperator;
import local.lito381.server.NativeDeControlCatalog;
import local.lito381.server.NativeDeInitializerKernel;
import local.lito381.server.ServerConfig;
import local.lito381.server.SharedDatabase;
import local.lito381.server.UnifiedConfiguration;

final class NativeDeFactoryControls {
    private static volatile Snapshot current;
    private static final System.Logger LOG;
    private static final NativeDeControlCatalog.Reporter REPORTER;

    private NativeDeFactoryControls() {
    }

    static synchronized void load(ServerConfig serverConfig) throws Exception {
        if (current != null) {
            return;
        }
        Properties properties = UnifiedConfiguration.read(serverConfig.source());
        String string = SharedDatabase.identifier(properties.getProperty("database.static.database"));
        NativeDeControlCatalog.Names names = NativeDeControlCatalog.emptyNames();
        NativeDeControlCatalog.Abilities abilities = NativeDeControlCatalog.emptyAbilities();
        NativeDeControlCatalog.Settings settings = NativeDeControlCatalog.emptySettings();
        boolean bl = false;
        try (Connection connection = DriverManager.getConnection(properties.getProperty("database.jdbc.base.url") + string + properties.getProperty("database.jdbc.options"), properties.getProperty("database.user"), properties.getProperty("database.password"));){
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(4);
            try (AutoCloseable autoCloseable = connection::rollback;){
                names = NativeDeControlCatalog.names(connection, names, REPORTER);
                abilities = NativeDeControlCatalog.abilities(connection, abilities, false, REPORTER);
                settings = NativeDeControlCatalog.settings(connection, REPORTER);
            }
        }
        catch (SQLException sQLException) {
            bl = true;
            REPORTER.event("lifecycle-sql", 0, sQLException);
        }
        current = new Snapshot(string, new NativeDeControlCatalog.Catalog(names, abilities, settings), bl);
    }

    static Snapshot snapshot() {
        Snapshot snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Source De controls not loaded at native resource startup");
        }
        return snapshot;
    }

    static NativeDeControlCatalog.Catalog catalog() {
        return NativeDeFactoryControls.snapshot().catalog();
    }

    static NativeDeInitializerKernel.Name[] names() {
        return (NativeDeInitializerKernel.Name[])NativeDeFactoryControls.catalog().names().selection().toArray(NativeDeInitializerKernel.Name[]::new);
    }

    static NativeDeInitializerKernel.Ability ability(int n) {
        return NativeDeFactoryControls.catalog().abilities().values().get(n);
    }

    static NativeDeInitializerKernel.Setting setting(int n) {
        NativeDeControlCatalog.Definition definition = NativeDeFactoryControls.catalog().settings().byId().get(n);
        return definition == null ? null : definition.key();
    }

    static int poly(int n, int n2, IntUnaryOperator intUnaryOperator) {
        return NativeDeFactoryControls.catalog().settings().poly(n, n2, intUnaryOperator);
    }

    static int doll(int n, int n2, IntUnaryOperator intUnaryOperator) {
        return NativeDeFactoryControls.catalog().settings().doll(n, n2, intUnaryOperator);
    }

    static NativeDeInitializerKernel.PetInfo pet(int n, int n2, IntUnaryOperator intUnaryOperator) {
        return NativeDeFactoryControls.catalog().settings().pet(n, n2, intUnaryOperator);
    }

    static {
        LOG = System.getLogger(NativeDeFactoryControls.class.getName());
        REPORTER = new NativeDeControlCatalog.Reporter(){

            @Override
            public void event(String string, int n, Exception exception) {
                if (exception != null) {
                    LOG.log(System.Logger.Level.ERROR, "Source De control " + string, (Throwable)exception);
                } else if (string.equals("duplicate-name-write-suppressed") || string.equals("invalid-name")) {
                    LOG.log(System.Logger.Level.WARNING, "Source De control " + string + " id=" + n);
                }
            }
        };
    }

    record Snapshot(String database, NativeDeControlCatalog.Catalog catalog, boolean lifecycleSqlFailure) {
    }
}

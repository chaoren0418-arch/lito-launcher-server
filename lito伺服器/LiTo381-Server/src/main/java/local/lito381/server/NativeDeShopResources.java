// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import local.lito381.server.CombatAccess;
import local.lito381.server.NativeDeNpcTemplateResources;
import local.lito381.server.NativeDeShopTable;
import local.lito381.server.NativeDeTerrainScope;
import local.lito381.server.NativeDeWorldLog;
import local.lito381.server.ServerConfig;
import local.lito381.server.SharedDatabase;
import local.lito381.server.UnifiedConfiguration;

final class NativeDeShopResources {
    private static volatile Snapshot current;

    static Snapshot read(String string3, NativeDeTerrainScope.Sql sql, NativeDeNpcTemplateResources.Counts counts, NativeDeWorldLog nativeDeWorldLog) throws SQLException {
        AtomicLong atomicLong = new AtomicLong();
        NativeDeTerrainScope.Context context = new NativeDeTerrainScope.Context(sql, null, (string, string2, objectArray) -> {
            if (string.equals("error")) {
                atomicLong.incrementAndGet();
            }
            nativeDeWorldLog.emit(string, string2, objectArray);
        });
        NativeDeShopTable nativeDeShopTable = NativeDeTerrainScope.call(context, NativeDeShopTable::create);
        if (atomicLong.get() != 0L || counts.lifecycleFailures.get() != 0L || counts.opened.get() != 1L || counts.rolledBack.get() != 1L || counts.closed.get() != 1L) {
            throw new SQLException("source shop SQL failed; publication refused");
        }
        Map map = (Map)CombatAccess.field(nativeDeShopTable, "_shopData");
        int n = map.values().stream().mapToInt(value -> ((List<?>) value).size()).sum();
        return new Snapshot(string3, nativeDeShopTable, map.size(), n, counts.opened.get(), counts.rolledBack.get(), counts.closed.get(), atomicLong.get());
    }

    static synchronized void load(ServerConfig serverConfig) throws Exception {
        Snapshot snapshot;
        if (current != null) {
            return;
        }
        Properties properties = UnifiedConfiguration.read(serverConfig.source());
        String string3 = SharedDatabase.identifier(properties.getProperty("database.static.database"));
        NativeDeNpcTemplateResources.Counts counts = new NativeDeNpcTemplateResources.Counts();
        current = snapshot = NativeDeShopResources.read(string3, () -> NativeDeNpcTemplateResources.owned(DriverManager.getConnection(properties.getProperty("database.jdbc.base.url") + string3 + properties.getProperty("database.jdbc.options"), properties.getProperty("database.user"), properties.getProperty("database.password")), counts), counts, (string, string2, objectArray) -> System.getLogger(NativeDeShopResources.class.getName()).log(string.equals("error") ? System.Logger.Level.ERROR : System.Logger.Level.INFO, string2 + " " + Arrays.toString(objectArray)));
    }

    static Snapshot snapshot() {
        return Objects.requireNonNull(current, "source shop static resources not loaded");
    }

    static NativeDeShopTable table() {
        return NativeDeShopResources.snapshot().table();
    }

    private NativeDeShopResources() {
    }

    record Snapshot(String database, NativeDeShopTable table, int maps, int ranges, long opened, long rolledBack, long closed, long errors) {
    }
}

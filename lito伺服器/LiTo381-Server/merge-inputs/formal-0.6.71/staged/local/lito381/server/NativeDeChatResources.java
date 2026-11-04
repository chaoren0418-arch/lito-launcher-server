// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import local.lito381.server.NativeDeChatData;
import local.lito381.server.NativeDeChatScope;
import local.lito381.server.NativeDeChatTable;
import local.lito381.server.NativeDeNpcTemplateResources;
import local.lito381.server.NativeDeTerrainScope;
import local.lito381.server.NativeDeWorldLog;
import local.lito381.server.ServerConfig;
import local.lito381.server.SharedDatabase;
import local.lito381.server.UnifiedConfiguration;

final class NativeDeChatResources {
    private static volatile Snapshot current;

    static Snapshot read(String string3, NativeDeTerrainScope.Sql sql, NativeDeNpcTemplateResources.Counts counts, NativeDeWorldLog nativeDeWorldLog) throws SQLException {
        AtomicLong atomicLong = new AtomicLong();
        NativeDeWorldLog nativeDeWorldLog2 = (string, string2, objectArray) -> {
            if (string.equals("error")) {
                atomicLong.incrementAndGet();
            }
            nativeDeWorldLog.emit(string, string2, objectArray);
        };
        NativeDeChatScope.Data data = new NativeDeChatScope.Data(nativeDeWorldLog2);
        NativeDeTerrainScope.Context context = new NativeDeTerrainScope.Context(sql, null, nativeDeWorldLog2);
        NativeDeChatTable nativeDeChatTable = NativeDeTerrainScope.call(context, () -> NativeDeChatScope.loading(data, () -> {
            NativeDeChatTable loaded = new NativeDeChatTable();
            loaded.load();
            return loaded;
        }));
        if (atomicLong.get() != 0L || counts.lifecycleFailures.get() != 0L || counts.opened.get() != 1L || counts.rolledBack.get() != 1L || counts.closed.get() != 1L) {
            throw new SQLException("source npcchat incomplete SQL/lifecycle; publication refused");
        }
        data.frozen = true;
        return new Snapshot(string3, nativeDeChatTable, Collections.unmodifiableMap(data.appearance), Collections.unmodifiableMap(data.dead), Collections.unmodifiableMap(data.hide), Collections.unmodifiableMap(data.game), counts.opened.get(), counts.rolledBack.get(), counts.closed.get(), atomicLong.get());
    }

    static synchronized void load(ServerConfig serverConfig) throws Exception {
        Snapshot snapshot;
        if (current != null) {
            return;
        }
        Properties properties = UnifiedConfiguration.read(serverConfig.source());
        String string3 = SharedDatabase.identifier(properties.getProperty("database.static.database"));
        NativeDeNpcTemplateResources.Counts counts = new NativeDeNpcTemplateResources.Counts();
        current = snapshot = NativeDeChatResources.read(string3, () -> NativeDeNpcTemplateResources.owned(DriverManager.getConnection(properties.getProperty("database.jdbc.base.url") + string3 + properties.getProperty("database.jdbc.options"), properties.getProperty("database.user"), properties.getProperty("database.password")), counts), counts, (string, string2, objectArray) -> System.getLogger(NativeDeChatResources.class.getName()).log(string.equals("error") ? System.Logger.Level.ERROR : System.Logger.Level.INFO, string2 + " " + Arrays.toString(objectArray)));
    }

    static Snapshot snapshot() {
        return Objects.requireNonNull(current, "source npcchat static resources not loaded");
    }

    static NativeDeChatTable table() {
        return NativeDeChatResources.snapshot().table();
    }

    private NativeDeChatResources() {
    }

    record Snapshot(String database, NativeDeChatTable table, Map<Integer, NativeDeChatData> appearance, Map<Integer, NativeDeChatData> dead, Map<Integer, NativeDeChatData> hide, Map<Integer, NativeDeChatData> game, long opened, long rolledBack, long closed, long errors) {
        int size() {
            return this.appearance.size() + this.dead.size() + this.hide.size() + this.game.size();
        }
    }
}

// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import local.lito381.server.NativeDeNpcMobSkill;
import local.lito381.server.NativeDeNpcTemplate;
import local.lito381.server.NativeDeNpcTemplateLoadScope;
import local.lito381.server.NativeDeNpcTemplateMobLoader;
import local.lito381.server.NativeDeNpcTemplateNpcLoader;
import local.lito381.server.NativeDeWorldLog;
import local.lito381.server.ServerConfig;
import local.lito381.server.SharedDatabase;
import local.lito381.server.UnifiedConfiguration;

final class NativeDeNpcTemplateResources {
    private static volatile Snapshot current;

    NativeDeNpcTemplateResources() {
    }

    static Connection owned(Connection connection, Counts counts) throws SQLException {
        try {
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(4);
        }
        catch (Error | RuntimeException | SQLException throwable) {
            counts.lifecycleFailures.incrementAndGet();
            try {
                connection.close();
            }
            catch (Error | RuntimeException | SQLException throwable2) {
                throwable.addSuppressed(throwable2);
            }
            throw throwable;
        }
        counts.opened.incrementAndGet();
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class[]{Connection.class}, (object, method, objectArray) -> {
            String string;
            Object object2;
            if (method.getName().equals("close") && method.getParameterCount() == 0) {
                if (atomicBoolean.compareAndSet(false, true)) {
                    Throwable throwable = null;
                    try {
                        connection.rollback();
                        counts.rolledBack.incrementAndGet();
                    }
                    catch (Throwable throwable2) {
                        counts.lifecycleFailures.incrementAndGet();
                        throwable = throwable2;
                    }
                    try {
                        connection.close();
                        counts.closed.incrementAndGet();
                    }
                    catch (Throwable throwable3) {
                        counts.lifecycleFailures.incrementAndGet();
                        if (throwable == null) {
                            throwable = throwable3;
                        }
                        throwable.addSuppressed(throwable3);
                    }
                    if (throwable != null) {
                        throw throwable;
                    }
                }
                return null;
            }
            if (Set.of("commit", "setAutoCommit", "setReadOnly", "setTransactionIsolation").contains(method.getName())) {
                throw new SQLException("source template load owns the read-only transaction");
            }
            if ((method.getName().equals("prepareStatement") || method.getName().equals("prepareCall")) && objectArray != null && objectArray.length > 0 && (object2 = objectArray[0]) instanceof String && !(string = (String)object2).stripLeading().toLowerCase(Locale.ROOT).startsWith("select")) {
                throw new SQLException("source template SQL must be SELECT");
            }
            try {
                return method.invoke(connection, objectArray);
            }
            catch (InvocationTargetException invocationTargetException) {
                throw invocationTargetException.getCause();
            }
        });
    }

    static Snapshot read(String string3, NativeDeNpcTemplateLoadScope.Sql sql, NativeDeNpcTemplateLoadScope.Resolver resolver, Counts counts, NativeDeWorldLog nativeDeWorldLog) throws Exception {
        NativeDeWorldLog nativeDeWorldLog2 = (string, string2, objectArray) -> {
            if (string.equals("error")) {
                counts.errors.incrementAndGet();
            }
            nativeDeWorldLog.emit(string, string2, objectArray);
        };
        HashMap<Integer, String> hashMap = new HashMap<Integer, String>();
        try (Connection connection = sql.open();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT skill_id, name FROM skills");
             ResultSet resultSet = preparedStatement.executeQuery();){
            while (resultSet.next()) {
                hashMap.put(resultSet.getInt("skill_id"), resultSet.getString("name"));
            }
        }
        Object object = new NativeDeNpcTemplateLoadScope.Context(sql, resolver, nativeDeWorldLog2, hashMap);
        NativeDeNpcTemplateLoadScope.call((NativeDeNpcTemplateLoadScope.Context)object, () -> {
            NativeDeNpcTemplateNpcLoader.load();
            NativeDeNpcTemplateMobLoader.load();
            return null;
        });
        if (counts.lifecycleFailures.get() != 0L || counts.opened.get() != counts.rolledBack.get() || counts.opened.get() != counts.closed.get()) {
            throw new SQLException("source template transaction setup/rollback/close failed; publication refused");
        }
        return new Snapshot(string3, ((NativeDeNpcTemplateLoadScope.Context)object).npcs, ((NativeDeNpcTemplateLoadScope.Context)object).mobSkills, ((NativeDeNpcTemplateLoadScope.Context)object).families, ((NativeDeNpcTemplateLoadScope.Context)object).orc, ((NativeDeNpcTemplateLoadScope.Context)object).requiredImplementations, ((NativeDeNpcTemplateLoadScope.Context)object).unresolvedExecutors, ((NativeDeNpcTemplateLoadScope.Context)object).suppressedBlankNameWrites, counts.opened.get(), counts.rolledBack.get(), counts.closed.get(), counts.errors.get(), false);
    }

    static synchronized void load(ServerConfig serverConfig) throws Exception {
        if (current != null) {
            return;
        }
        Properties properties = UnifiedConfiguration.read(serverConfig.source());
        String string3 = SharedDatabase.identifier(properties.getProperty("database.static.database"));
        Counts counts = new Counts();
        NativeDeNpcTemplateLoadScope.Sql sql = () -> NativeDeNpcTemplateResources.owned(DriverManager.getConnection(properties.getProperty("database.jdbc.base.url") + string3 + properties.getProperty("database.jdbc.options"), properties.getProperty("database.user"), properties.getProperty("database.password")), counts);
        NativeDeWorldLog nativeDeWorldLog = (string, string2, objectArray) -> {
            if (!string.equals("error")) {
                System.getLogger(NativeDeNpcTemplateResources.class.getName()).log(System.Logger.Level.INFO, string2 + " " + Arrays.toString(objectArray));
            }
        };
        try {
            current = NativeDeNpcTemplateResources.read(string3, sql, string -> {
                throw new ClassNotFoundException(string);
            }, counts, nativeDeWorldLog);
        }
        catch (SQLException sQLException) {
            current = new Snapshot(string3, Map.of(), Map.of(), Map.of(), -1, Set.of(), Set.of(), 0, counts.opened.get(), counts.rolledBack.get(), counts.closed.get(), counts.errors.incrementAndGet(), true);
            System.getLogger(NativeDeNpcTemplateResources.class.getName()).log(System.Logger.Level.ERROR, "Source NPC template SQL lifecycle failed", (Throwable)sQLException);
        }
        Snapshot snapshot = current;
        System.getLogger(NativeDeNpcTemplateResources.class.getName()).log(System.Logger.Level.INFO, "Source NPC templates=" + snapshot.npcs().size() + " mobSkills=" + snapshot.mobSkills().size() + " required constructor kinds=" + snapshot.requiredImplementations().size() + " unresolved executor classes=" + snapshot.unresolvedExecutors().size() + "; source name UPDATE suppressed; full NPC factory remains unqualified");
    }

    static Snapshot snapshot() {
        Snapshot snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("source NPC templates not loaded at native resource startup");
        }
        return snapshot;
    }

    static final class Counts {
        final AtomicLong opened = new AtomicLong();
        final AtomicLong rolledBack = new AtomicLong();
        final AtomicLong closed = new AtomicLong();
        final AtomicLong errors = new AtomicLong();
        final AtomicLong lifecycleFailures = new AtomicLong();

        Counts() {
        }
    }

    record Snapshot(String database, Map<Integer, NativeDeNpcTemplate> npcs, Map<Integer, NativeDeNpcMobSkill> mobSkills, Map<String, Integer> families, int orc, Set<String> requiredImplementations, Set<String> unresolvedExecutors, int suppressedBlankNameWrites, long opened, long rolledBack, long closed, long errors, boolean lifecycleFailure) {Snapshot {
            npcs = Map.copyOf(npcs);
            mobSkills = Map.copyOf(mobSkills);
            families = Map.copyOf(families);
            requiredImplementations = Set.copyOf(requiredImplementations);
            unresolvedExecutors = Set.copyOf(unresolvedExecutors);
        }
    }
}

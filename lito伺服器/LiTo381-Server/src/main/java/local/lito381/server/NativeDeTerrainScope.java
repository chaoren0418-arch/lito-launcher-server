// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import local.lito381.server.NativeDeTerrainData;
import local.lito381.server.NativeDeTerrainFileReader;
import local.lito381.server.NativeDeWorldContext;
import local.lito381.server.NativeDeWorldLog;

final class NativeDeTerrainScope {
    private static final ThreadLocal<Context> current = new ThreadLocal();

    static Context owner() {
        return Objects.requireNonNull(current.get(), "source terrain scope absent");
    }

    static <T> T call(Context context, Supplier<T> supplier) {
        Context context2 = current.get();
        Set set = Collections.newSetFromMap(new IdentityHashMap());
        set.addAll(context.readers);
        long l = context.attempted;
        long l2 = context.opened;
        long l3 = context.closed;
        current.set(Objects.requireNonNull(context));
        Throwable throwable = null;
        try {
            T t = supplier.get();
            return t;
        }
        catch (Error | RuntimeException throwable2) {
            throwable = throwable2;
            throw throwable2;
        }
        finally {
            IllegalStateException illegalStateException = null;
            for (NativeDeTerrainFileReader nativeDeTerrainFileReader : (NativeDeTerrainFileReader[])context.readers.toArray(NativeDeTerrainFileReader[]::new)) {
                try {
                    if (set.contains(nativeDeTerrainFileReader)) continue;
                    nativeDeTerrainFileReader.close();
                }
                catch (Throwable illegalStateException2) {
                    if (illegalStateException == null) {
                        illegalStateException = illegalStateException2;
                        continue;
                    }
                    illegalStateException.addSuppressed(illegalStateException2);
                }
            }
            if (context.attempted - l != context.opened - l2 || context.opened - l2 != context.closed - l3) {
                IllegalStateException illegalStateException3 = new IllegalStateException("source terrain reader lifecycle failed; publication refused");
                if (illegalStateException == null) {
                    illegalStateException = illegalStateException3;
                } else {
                    illegalStateException.addSuppressed(illegalStateException3);
                }
            }
            if (context2 == null) {
                current.remove();
            } else {
                current.set(context2);
            }
            if (illegalStateException != null) {
                if (throwable != null) {
                    throwable.addSuppressed(illegalStateException);
                } else {
                    throw NativeDeWorldContext.failure(illegalStateException);
                }
            }
        }
    }

    static boolean present() {
        return current.get() != null;
    }

    static Map<Integer, NativeDeTerrainData> maps() {
        return NativeDeTerrainScope.owner().maps;
    }

    static Map<Integer, String> names() {
        return NativeDeTerrainScope.owner().names;
    }

    private NativeDeTerrainScope() {
    }

    static final class Context {
        final Sql sql;
        final Asset asset;
        final NativeDeWorldLog log;
        final Map<Integer, NativeDeTerrainData> maps = new ConcurrentHashMap<Integer, NativeDeTerrainData>();
        final Map<Integer, String> names = new ConcurrentHashMap<Integer, String>();
        final Set<NativeDeTerrainFileReader> readers = Collections.newSetFromMap(new IdentityHashMap());
        long attempted;
        long opened;
        long closed;

        Context(Sql sql, Asset asset, NativeDeWorldLog nativeDeWorldLog) {
            this.sql = sql;
            this.asset = asset;
            this.log = Objects.requireNonNull(nativeDeWorldLog);
        }
    }

    static final class Logs {
        Logs() {
        }

        static NativeDeWorldLog getLogger(Class<?> clazz) {
            return (string, string2, objectArray) -> {
                Context context = current.get();
                if (context == null) {
                    System.getLogger(clazz.getName()).log(System.Logger.Level.WARNING, string2 + " " + Arrays.toString(objectArray));
                } else {
                    context.log.emit(string, string2, objectArray);
                }
            };
        }
    }

    static final class Database {
        Database() {
        }

        static Database get() {
            return new Database();
        }

        Connection getConnection() throws SQLException {
            return NativeDeTerrainScope.owner().sql.open();
        }
    }

    static interface Asset {
        public Reader open(String var1) throws IOException;
    }

    static interface Sql {
        public Connection open() throws SQLException;
    }
}

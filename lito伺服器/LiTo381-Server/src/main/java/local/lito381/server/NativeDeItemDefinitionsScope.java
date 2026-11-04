// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import local.lito381.server.NativeDeItemDefinitionsSql;
import local.lito381.server.NativeDeWorldLog;

final class NativeDeItemDefinitionsScope {
    private static final ThreadLocal<Context> current = new ThreadLocal();

    static <T> T loading(Data data, NativeDeItemDefinitionsSql nativeDeItemDefinitionsSql, Supplier<T> supplier) {
        return NativeDeItemDefinitionsScope.run(new Context(data, nativeDeItemDefinitionsSql), supplier);
    }

    static <T> T call(Data data, Supplier<T> supplier) {
        if (data.loading) {
            throw new IllegalStateException("item definitions are not published");
        }
        return NativeDeItemDefinitionsScope.run(new Context(data, null), supplier);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static <T> T run(Context context, Supplier<T> supplier) {
        Context context2 = current.get();
        current.set(context);
        try {
            T t = supplier.get();
            return t;
        }
        finally {
            if (context2 == null) {
                current.remove();
            } else {
                current.set(context2);
            }
        }
    }

    static boolean present() {
        return current.get() != null;
    }

    static Data loadingData() {
        Context context = Objects.requireNonNull(current.get(), "item definition owner absent");
        NativeDeItemDefinitionsScope.requireLoading(context.data);
        return context.data;
    }

    static void requireLoading(Data data) {
        Context context = current.get();
        if (context == null || context.data != data || !data.loading || context.sql == null) {
            throw new IllegalStateException("published item definitions cannot load or reload in place");
        }
    }

    static Object table(Class<?> clazz) {
        Context context = Objects.requireNonNull(current.get(), "item definition owner absent");
        return Objects.requireNonNull(context.data.tables.get(clazz), "item definition table not loaded: " + clazz.getName());
    }

    static NativeDeWorldLog log() {
        return Objects.requireNonNull(NativeDeItemDefinitionsScope.current.get(), (String)"item definition log owner absent").data.log;
    }

    private NativeDeItemDefinitionsScope() {
    }

    private record Context(Data data, NativeDeItemDefinitionsSql sql) {
    }

    static final class Data {
        Map<Class<?>, Object> tables = new LinkedHashMap();
        final NativeDeWorldLog log;
        boolean loading = true;
        long sourceErrors;

        Data(NativeDeWorldLog nativeDeWorldLog) {
            Objects.requireNonNull(nativeDeWorldLog);
            this.log = (string, string2, objectArray) -> {
                if (this.loading && string.equals("error")) {
                    ++this.sourceErrors;
                }
                nativeDeWorldLog.emit(string, string2, objectArray);
            };
        }
    }

    static final class TableLog {
        TableLog() {
        }

        static void loaded(NativeDeWorldLog nativeDeWorldLog, String string, Object object, long l) {
            nativeDeWorldLog.info("\u8f09\u5165 {} ok: {} ({}ms)", (Object)string, object, (Object)l);
        }
    }

    static final class Database {
        Database() {
        }

        static Database get() {
            return new Database();
        }

        Connection getConnection() throws SQLException {
            Context context = Objects.requireNonNull(current.get(), "item definition SQL owner absent");
            NativeDeItemDefinitionsScope.requireLoading(context.data);
            return context.sql.lease();
        }
    }

    static final class Logs {
        Logs() {
        }

        static NativeDeWorldLog getLogger(Class<?> clazz) {
            return (string, string2, objectArray) -> NativeDeItemDefinitionsScope.log().emit(string, string2, objectArray);
        }
    }
}

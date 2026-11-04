// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import local.lito381.server.NativeDeItemDefinitionsSql;
import local.lito381.server.NativeDeItemTable;
import local.lito381.server.NativeDeWorldLog;

final class NativeDeItemTemplatesScope {
    private static final ThreadLocal<Context> current = new ThreadLocal();

    static <T> T loading(Data data, NativeDeItemDefinitionsSql nativeDeItemDefinitionsSql, Supplier<T> supplier) {
        return NativeDeItemTemplatesScope.run(new Context(data, nativeDeItemDefinitionsSql), supplier);
    }

    static <T> T call(Data data, Supplier<T> supplier) {
        if (data.loading) {
            throw new IllegalStateException("item templates not published");
        }
        return NativeDeItemTemplatesScope.run(new Context(data, null), supplier);
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
        Context context = Objects.requireNonNull(current.get(), "item template owner absent");
        NativeDeItemTemplatesScope.requireLoading(context.data);
        return context.data;
    }

    static void requireLoading(Data data) {
        Context context = current.get();
        if (context == null || context.data != data || !data.loading || context.sql == null) {
            throw new IllegalStateException("published source item templates cannot reload in place");
        }
    }

    static NativeDeItemTable table() {
        return Objects.requireNonNull(Objects.requireNonNull(NativeDeItemTemplatesScope.current.get(), (String)"item template owner absent").data.table, "item templates unavailable");
    }

    static NativeDeWorldLog log() {
        return Objects.requireNonNull(NativeDeItemTemplatesScope.current.get(), (String)"item template log owner absent").data.log;
    }

    static void debug(NativeDeWorldLog nativeDeWorldLog, String string) {
        nativeDeWorldLog.emit("debug", string, new Object[0]);
    }

    private NativeDeItemTemplatesScope() {
    }

    private record Context(Data data, NativeDeItemDefinitionsSql sql) {
    }

    static final class Data {
        final NativeDeWorldLog log;
        final Map<Integer, Binding> bindings = new LinkedHashMap<Integer, Binding>();
        NativeDeItemTable table;
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

    static final class Classes {
        Classes() {
        }

        static Classes get() {
            return new Classes();
        }

        void addList(int n, String string, int n2) {
            String[] stringArray;
            if (string.equals("0")) {
                return;
            }
            Data data = NativeDeItemTemplatesScope.loadingData();
            if (string.indexOf(32) >= 0) {
                stringArray = string.split(" ");
            } else {
                String[] stringArray2 = new String[1];
                stringArray = stringArray2;
                stringArray2[0] = string;
            }
            String[] stringArray3 = stringArray;
            String string2 = switch (n2) {
                case 0 -> "com.lineage.data.item_etcitem.";
                case 1 -> "com.lineage.data.item_weapon.";
                case 2 -> "com.lineage.data.item_armor.";
                default -> throw new IllegalArgumentException("source item executor mode");
            };
            data.bindings.put(n, new Binding(n, string, n2, string2 + stringArray3[0], List.copyOf(Arrays.asList(stringArray3))));
        }
    }

    static final class Database {
        Database() {
        }

        static Database get() {
            return new Database();
        }

        Connection getConnection() throws SQLException {
            Context context = Objects.requireNonNull(current.get(), "item template SQL owner absent");
            NativeDeItemTemplatesScope.requireLoading(context.data);
            return context.sql.lease();
        }
    }

    static final class Logs {
        Logs() {
        }

        static NativeDeWorldLog getLogger(Class<?> clazz) {
            return (string, string2, objectArray) -> NativeDeItemTemplatesScope.log().emit(string, string2, objectArray);
        }
    }

    record Binding(int itemId, String rawClass, int mode, String sourceClass, List<String> arguments) {
    }
}

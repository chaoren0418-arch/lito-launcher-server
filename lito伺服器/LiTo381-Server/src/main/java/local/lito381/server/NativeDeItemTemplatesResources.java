// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;
import local.lito381.server.NativeDeArmorTemplate;
import local.lito381.server.NativeDeEtcItemTemplate;
import local.lito381.server.NativeDeFrozenArmorTemplate;
import local.lito381.server.NativeDeFrozenEtcTemplate;
import local.lito381.server.NativeDeFrozenWeaponTemplate;
import local.lito381.server.NativeDeItemDefinitionsSql;
import local.lito381.server.NativeDeItemTable;
import local.lito381.server.NativeDeItemTemplate;
import local.lito381.server.NativeDeItemTemplatesScope;
import local.lito381.server.NativeDeTerrainScope;
import local.lito381.server.NativeDeWeaponTemplate;
import local.lito381.server.NativeDeWorldLog;
import local.lito381.server.ServerConfig;
import local.lito381.server.SharedDatabase;
import local.lito381.server.UnifiedConfiguration;

final class NativeDeItemTemplatesResources {
    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Snapshot read(String string, NativeDeTerrainScope.Sql sql, NativeDeWorldLog nativeDeWorldLog) throws SQLException {
        Object object;
        NativeDeItemTemplatesScope.Data data = new NativeDeItemTemplatesScope.Data(nativeDeWorldLog);
        NativeDeItemDefinitionsSql nativeDeItemDefinitionsSql = new NativeDeItemDefinitionsSql(sql.open(), NativeDeItemDefinitionsSql.TEMPLATE_QUERIES);
        Throwable throwable = null;
        try {
            nativeDeItemDefinitionsSql.configure();
            NativeDeItemTemplatesScope.loading(data, nativeDeItemDefinitionsSql, () -> {
                data.table = new NativeDeItemTable();
                data.table.load();
                return null;
            });
            if (data.sourceErrors != 0L) {
                throw new SQLException("source item template errors; publication refused: " + data.sourceErrors);
            }
            nativeDeItemDefinitionsSql.validate();
        }
        catch (Throwable throwable2) {
            throwable = throwable2;
        }
        finally {
            try {
                nativeDeItemDefinitionsSql.finish();
            }
            catch (Throwable throwable3) {
                if (throwable == null) {
                    throwable = throwable3;
                }
                throwable.addSuppressed(throwable3);
            }
            data.loading = false;
        }
        if (throwable != null) {
            if (throwable instanceof Error) {
                throw (Error)throwable;
            }
            if (throwable instanceof SQLException) {
                throw (SQLException)throwable;
            }
            throw new SQLException("source item template publication refused", throwable);
        }
        object = nativeDeItemDefinitionsSql.counts();
        if (((NativeDeItemDefinitionsSql.Counts)object).rolledBack() != 1 || ((NativeDeItemDefinitionsSql.Counts)object).physicalClosed() != 1 || ((NativeDeItemDefinitionsSql.Counts)object).lifecycleFailures() != 0) {
            throw new SQLException("item template SQL did not finish before publication");
        }
        int n = NativeDeItemTemplatesResources.freeze(data.table);
        return new Snapshot(string, data.table, data, Collections.unmodifiableMap(new LinkedHashMap<Integer, NativeDeItemTemplatesScope.Binding>(data.bindings)), (NativeDeItemDefinitionsSql.Counts)object, n);
    }

    static int[] copyArray(int[] nArray) {
        return nArray == null ? null : (int[])nArray.clone();
    }

    private static NativeDeItemTemplate frozen(NativeDeItemTemplate nativeDeItemTemplate, IdentityHashMap<NativeDeItemTemplate, NativeDeItemTemplate> identityHashMap) throws ReflectiveOperationException {
        NativeDeFrozenWeaponTemplate nativeDeFrozenWeaponTemplate;
        if (nativeDeItemTemplate == null) {
            return null;
        }
        NativeDeItemTemplate nativeDeItemTemplate2 = identityHashMap.get(nativeDeItemTemplate);
        if (nativeDeItemTemplate2 != null) {
            return nativeDeItemTemplate2;
        }
        NativeDeItemTemplate nativeDeItemTemplate3 = nativeDeItemTemplate instanceof NativeDeWeaponTemplate ? new NativeDeFrozenWeaponTemplate() : (nativeDeItemTemplate instanceof NativeDeArmorTemplate ? new NativeDeFrozenArmorTemplate() : (nativeDeItemTemplate instanceof NativeDeEtcItemTemplate ? new NativeDeFrozenEtcTemplate() : null));
        nativeDeFrozenWeaponTemplate = nativeDeItemTemplate3 instanceof NativeDeFrozenWeaponTemplate ? (NativeDeFrozenWeaponTemplate)nativeDeItemTemplate3 : null;
        if (nativeDeItemTemplate3 == null) {
            throw new IllegalArgumentException("unknown source item template role");
        }
        for (Class<?> clazz = nativeDeItemTemplate.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                Object object = field.get(nativeDeItemTemplate);
                if (object instanceof int[]) {
                    int[] nArray = (int[])object;
                    object = nArray.clone();
                } else if (object != null && !field.getType().isPrimitive() && !(object instanceof String)) {
                    throw new IllegalArgumentException("unexpected mutable template field " + field.getName());
                }
                field.set(nativeDeItemTemplate3, object);
            }
        }
        identityHashMap.put(nativeDeItemTemplate, nativeDeItemTemplate3);
        return nativeDeItemTemplate3;
    }

    private static int freeze(NativeDeItemTable nativeDeItemTable) throws SQLException {
        try {
            NativeDeItemTemplate[] nativeDeItemTemplateArray;
            IdentityHashMap<NativeDeItemTemplate, NativeDeItemTemplate> identityHashMap = new IdentityHashMap<NativeDeItemTemplate, NativeDeItemTemplate>();
            for (String nativeDeItemTemplateArray22 : List.of("_etcitems", "_weapons", "_armors")) {
                Field templateMapField = nativeDeItemTable.getClass().getDeclaredField(nativeDeItemTemplateArray22);
                templateMapField.setAccessible(true);
                Map<?, ?> i = (Map<?, ?>)templateMapField.get(nativeDeItemTable);
                LinkedHashMap<Object, NativeDeItemTemplate> linkedHashMap = new LinkedHashMap<Object, NativeDeItemTemplate>();
                for (Map.Entry<?, ?> entry : i.entrySet()) {
                    linkedHashMap.put(entry.getKey(), NativeDeItemTemplatesResources.frozen((NativeDeItemTemplate)entry.getValue(), identityHashMap));
                }
                templateMapField.set(nativeDeItemTable, Collections.unmodifiableMap(linkedHashMap));
            }
            Field field = nativeDeItemTable.getClass().getDeclaredField("_allTemplates");
            field.setAccessible(true);
            NativeDeItemTemplate[] nativeDeItemTemplateArray2 = (NativeDeItemTemplate[])field.get(nativeDeItemTable);
            nativeDeItemTemplateArray = new NativeDeItemTemplate[nativeDeItemTemplateArray2.length];
            for (int i = 0; i < nativeDeItemTemplateArray2.length; ++i) {
                nativeDeItemTemplateArray[i] = NativeDeItemTemplatesResources.frozen(nativeDeItemTemplateArray2[i], identityHashMap);
            }
            field.set(nativeDeItemTable, nativeDeItemTemplateArray);
            return identityHashMap.size();
        }
        catch (ReflectiveOperationException | RuntimeException exception) {
            throw new SQLException("source item template freeze failed; publication refused", exception);
        }
    }

    private NativeDeItemTemplatesResources() {
    }

    record Snapshot(String database, NativeDeItemTable table, NativeDeItemTemplatesScope.Data data, Map<Integer, NativeDeItemTemplatesScope.Binding> executorBindings, NativeDeItemDefinitionsSql.Counts counts, int templates) {
        <T> T call(Supplier<T> supplier) {
            return NativeDeItemTemplatesScope.call(this.data, supplier);
        }
    }

    static final class Owner {
        private volatile Snapshot current;

        Owner() {
        }

        Snapshot snapshot() {
            return Objects.requireNonNull(this.current, "source item templates not loaded");
        }

        synchronized Snapshot reload(String string, NativeDeTerrainScope.Sql sql, NativeDeWorldLog nativeDeWorldLog) throws SQLException {
            Snapshot snapshot;
            this.current = snapshot = NativeDeItemTemplatesResources.read(string, sql, nativeDeWorldLog);
            return snapshot;
        }

        synchronized Snapshot reload(ServerConfig serverConfig) throws Exception {
            Properties properties = UnifiedConfiguration.read(serverConfig.source());
            String string3 = SharedDatabase.identifier(properties.getProperty("database.static.database"));
            return this.reload(string3, () -> DriverManager.getConnection(properties.getProperty("database.jdbc.base.url") + string3 + properties.getProperty("database.jdbc.options"), properties.getProperty("database.user"), properties.getProperty("database.password")), (string, string2, objectArray) -> System.getLogger(NativeDeItemTemplatesResources.class.getName()).log(string.equals("error") ? System.Logger.Level.ERROR : (string.equals("warn") ? System.Logger.Level.WARNING : (string.equals("debug") ? System.Logger.Level.DEBUG : System.Logger.Level.INFO)), string2 + " " + Arrays.toString(objectArray)));
        }
    }
}

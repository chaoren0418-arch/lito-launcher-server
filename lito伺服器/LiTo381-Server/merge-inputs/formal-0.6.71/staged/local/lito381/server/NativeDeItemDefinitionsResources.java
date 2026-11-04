// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;
import local.lito381.server.NativeDeItemAbility;
import local.lito381.server.NativeDeItemAttrWeapon;
import local.lito381.server.NativeDeItemBlessStatsTable;
import local.lito381.server.NativeDeItemCommonEnchantTable;
import local.lito381.server.NativeDeItemDefinitionsScope;
import local.lito381.server.NativeDeItemDefinitionsSql;
import local.lito381.server.NativeDeItemEnchantBonusTable;
import local.lito381.server.NativeDeItemEquipProc;
import local.lito381.server.NativeDeItemEquipProcTable;
import local.lito381.server.NativeDeItemFrozenAbility;
import local.lito381.server.NativeDeItemGreaterTierTable;
import local.lito381.server.NativeDeItemIdStatsTable;
import local.lito381.server.NativeDeItemInlayCard;
import local.lito381.server.NativeDeItemInlayCardTable;
import local.lito381.server.NativeDeItemMagicStone;
import local.lito381.server.NativeDeItemMagicStoneTable;
import local.lito381.server.NativeDeItemWeaponAttrTable;
import local.lito381.server.NativeDeItemWeaponProc;
import local.lito381.server.NativeDeItemWeaponProcTable;
import local.lito381.server.NativeDeTerrainScope;
import local.lito381.server.NativeDeWorldContext;
import local.lito381.server.NativeDeWorldLog;
import local.lito381.server.ServerConfig;
import local.lito381.server.SharedDatabase;
import local.lito381.server.UnifiedConfiguration;

final class NativeDeItemDefinitionsResources {
    static final List<Class<?>> TABLES = List.of(NativeDeItemWeaponAttrTable.class, NativeDeItemBlessStatsTable.class, NativeDeItemCommonEnchantTable.class, NativeDeItemEnchantBonusTable.class, NativeDeItemGreaterTierTable.class, NativeDeItemIdStatsTable.class, NativeDeItemMagicStoneTable.class, NativeDeItemInlayCardTable.class, NativeDeItemWeaponProcTable.class, NativeDeItemEquipProcTable.class);

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Snapshot read(String string, NativeDeTerrainScope.Sql sql, NativeDeWorldLog nativeDeWorldLog) throws SQLException {
        NativeDeItemDefinitionsScope.Data data = new NativeDeItemDefinitionsScope.Data(nativeDeWorldLog);
        NativeDeItemDefinitionsSql nativeDeItemDefinitionsSql = new NativeDeItemDefinitionsSql(sql.open());
        Throwable throwable = null;
        try {
            nativeDeItemDefinitionsSql.configure();
            NativeDeItemDefinitionsScope.loading(data, nativeDeItemDefinitionsSql, () -> {
                for (Class<?> clazz : TABLES) {
                    Object object = NativeDeItemDefinitionsResources.create(clazz);
                    data.tables.put(clazz, object);
                    if (!Set.of(NativeDeItemWeaponAttrTable.class, NativeDeItemMagicStoneTable.class, NativeDeItemWeaponProcTable.class).contains(clazz)) continue;
                    NativeDeItemDefinitionsResources.invokeLoad(object);
                }
                return null;
            });
            if (data.sourceErrors != 0L) {
                throw new SQLException("source item definition load errors; snapshot refused: " + data.sourceErrors);
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
        NativeDeItemDefinitionsSql.Counts counts = nativeDeItemDefinitionsSql.counts();
        if (throwable != null) {
            if (throwable instanceof Error) {
                Error error = (Error)throwable;
                throw error;
            }
            if (throwable instanceof SQLException) {
                SQLException sQLException = (SQLException)throwable;
                throw sQLException;
            }
            throw new SQLException("source item definition snapshot refused", throwable);
        }
        if (counts.rolledBack() != 1 || counts.physicalClosed() != 1 || counts.lifecycleFailures() != 0) {
            throw new SQLException("item definition transaction did not finish before publication");
        }
        NativeDeItemDefinitionsResources.freeze(data);
        return new Snapshot(string, data, counts);
    }

    private static Object create(Class<?> clazz) {
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor(new Class[0]);
            constructor.setAccessible(true);
            return constructor.newInstance(new Object[0]);
        }
        catch (InvocationTargetException invocationTargetException) {
            throw NativeDeWorldContext.failure(invocationTargetException.getCause());
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalStateException("source definition constructor unavailable", reflectiveOperationException);
        }
    }

    private static void invokeLoad(Object object) {
        try {
            object.getClass().getMethod("load", new Class[0]).invoke(object, new Object[0]);
        }
        catch (InvocationTargetException invocationTargetException) {
            throw NativeDeWorldContext.failure(invocationTargetException.getCause());
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalStateException("source definition load unavailable", reflectiveOperationException);
        }
    }

    private static void freeze(NativeDeItemDefinitionsScope.Data data) throws SQLException {
        try {
            IdentityHashMap<Object, Object> identityHashMap = new IdentityHashMap<Object, Object>();
            for (Object object : data.tables.values()) {
                for (Field field : object.getClass().getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || !Map.class.isAssignableFrom(field.getType())) continue;
                    field.setAccessible(true);
                    field.set(object, NativeDeItemDefinitionsResources.freezeValue(field.get(object), identityHashMap));
                }
            }
            data.tables = Collections.unmodifiableMap(new LinkedHashMap(data.tables));
        }
        catch (ReflectiveOperationException | RuntimeException exception) {
            throw new SQLException("source item definition freeze failed; snapshot refused", exception);
        }
    }

    private static Object freezeValue(Object object, IdentityHashMap<Object, Object> identityHashMap) throws ReflectiveOperationException {
        if (object == null || object instanceof String || object instanceof Number || object instanceof Boolean || object.getClass().isEnum()) {
            return object;
        }
        if (identityHashMap.containsKey(object)) {
            return identityHashMap.get(object);
        }
        if (object instanceof NativeDeItemAbility) {
            NativeDeItemAbility nativeDeItemAbility = (NativeDeItemAbility)object;
            NativeDeItemFrozenAbility nativeDeItemFrozenAbility = new NativeDeItemFrozenAbility(nativeDeItemAbility);
            identityHashMap.put(object, nativeDeItemFrozenAbility);
            return nativeDeItemFrozenAbility;
        }
        if (object instanceof Map) {
            Map map = (Map)object;
            LinkedHashMap<Object, Object> linkedHashMap = new LinkedHashMap<Object, Object>();
            Map map2 = Collections.unmodifiableMap(linkedHashMap);
            identityHashMap.put(object, map2);
            for (Map.Entry<?, ?> entry : ((Map<?, ?>)map).entrySet()) {
                linkedHashMap.put(NativeDeItemDefinitionsResources.freezeValue(entry.getKey(), identityHashMap), NativeDeItemDefinitionsResources.freezeValue(entry.getValue(), identityHashMap));
            }
            return map2;
        }
        if (object instanceof List) {
            List list = (List)object;
            ArrayList<Object> arrayList = new ArrayList<Object>();
            List list2 = Collections.unmodifiableList(arrayList);
            identityHashMap.put(object, list2);
            for (Object e : list) {
                arrayList.add(NativeDeItemDefinitionsResources.freezeValue(e, identityHashMap));
            }
            return list2;
        }
        if (object instanceof NativeDeItemMagicStone) {
            NativeDeItemMagicStone nativeDeItemMagicStone = (NativeDeItemMagicStone)object;
            RecordComponent[] recordComponentArray = nativeDeItemMagicStone.getClass().getRecordComponents();
            Class[] classArray = new Class[recordComponentArray.length];
            Object[] objectArray = new Object[recordComponentArray.length];
            for (int i = 0; i < recordComponentArray.length; ++i) {
                classArray[i] = recordComponentArray[i].getType();
                objectArray[i] = NativeDeItemDefinitionsResources.freezeValue(recordComponentArray[i].getAccessor().invoke(nativeDeItemMagicStone, new Object[0]), identityHashMap);
            }
            NativeDeItemMagicStone nativeDeItemMagicStone2 = (NativeDeItemMagicStone)nativeDeItemMagicStone.getClass().getDeclaredConstructor(classArray).newInstance(objectArray);
            identityHashMap.put(object, nativeDeItemMagicStone2);
            return nativeDeItemMagicStone2;
        }
        if (object instanceof NativeDeItemInlayCard) {
            NativeDeItemInlayCard nativeDeItemInlayCard = (NativeDeItemInlayCard)object;
            Field field = nativeDeItemInlayCard.getClass().getDeclaredField("_ability");
            field.setAccessible(true);
            field.set(nativeDeItemInlayCard, NativeDeItemDefinitionsResources.freezeValue(field.get(nativeDeItemInlayCard), identityHashMap));
            identityHashMap.put(nativeDeItemInlayCard, nativeDeItemInlayCard);
            return nativeDeItemInlayCard;
        }
        if (object instanceof NativeDeItemAttrWeapon || object instanceof NativeDeItemWeaponProc || object instanceof NativeDeItemEquipProc || object.getClass().getName().equals(NativeDeItemGreaterTierTable.class.getName() + "$TierChance")) {
            identityHashMap.put(object, object);
            return object;
        }
        throw new IllegalArgumentException("unexpected mutable definition value: " + object.getClass().getName());
    }

    private NativeDeItemDefinitionsResources() {
    }

    record Snapshot(String database, NativeDeItemDefinitionsScope.Data data, NativeDeItemDefinitionsSql.Counts counts) {
        <T> T call(Supplier<T> supplier) {
            return NativeDeItemDefinitionsScope.call(this.data, supplier);
        }

        <T> T table(Class<T> clazz) {
            return clazz.cast(Objects.requireNonNull(this.data.tables.get(clazz), "definition table not present"));
        }
    }

    static final class Owner {
        private volatile Snapshot current;

        Owner() {
        }

        Snapshot snapshot() {
            return Objects.requireNonNull(this.current, "source item definitions not loaded");
        }

        synchronized Snapshot reload(String string, NativeDeTerrainScope.Sql sql, NativeDeWorldLog nativeDeWorldLog) throws SQLException {
            Snapshot snapshot = NativeDeItemDefinitionsResources.read(string, sql, nativeDeWorldLog);
            this.current = snapshot;
            return snapshot;
        }

        synchronized Snapshot reload(ServerConfig serverConfig) throws Exception {
            Properties properties = UnifiedConfiguration.read(serverConfig.source());
            String string3 = SharedDatabase.identifier(properties.getProperty("database.static.database"));
            return this.reload(string3, () -> DriverManager.getConnection(properties.getProperty("database.jdbc.base.url") + string3 + properties.getProperty("database.jdbc.options"), properties.getProperty("database.user"), properties.getProperty("database.password")), (string, string2, objectArray) -> System.getLogger(NativeDeItemDefinitionsResources.class.getName()).log(string.equals("error") ? System.Logger.Level.ERROR : (string.equals("warn") ? System.Logger.Level.WARNING : System.Logger.Level.INFO), string2 + " " + Arrays.toString(objectArray)));
        }
    }
}

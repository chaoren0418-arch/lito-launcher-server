// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.LongSupplier;
import javax.sql.DataSource;
import local.lito381.server.AccountRepository;
import local.lito381.server.ClientSession;
import local.lito381.server.GameStateStore;
import local.lito381.server.GmCommandDefinition;
import local.lito381.server.InventoryCatalog;
import local.lito381.server.LiToGmNpcs;
import local.lito381.server.RuntimeInventory;
import local.lito381.server.SourceAdministration;

final class LiToGmCommands {
    LiToGmCommands() {
    }

    static Object get(Object object, String string) throws Exception {
        Field field = object.getClass().getDeclaredField(string);
        field.setAccessible(true);
        return field.get(object);
    }

    static void set(Object object, String string, Object object2) throws Exception {
        Field field = object.getClass().getDeclaredField(string);
        field.setAccessible(true);
        field.set(object, object2);
    }

    static Object call(Object object, String string, Class<?>[] classArray, Object ... objectArray) throws Exception {
        Method method = object.getClass().getDeclaredMethod(string, classArray);
        method.setAccessible(true);
        try {
            return method.invoke(object, objectArray);
        }
        catch (InvocationTargetException invocationTargetException) {
            Throwable throwable = invocationTargetException.getCause();
            if (throwable instanceof Exception) {
                Exception exception = (Exception)throwable;
                throw exception;
            }
            throw invocationTargetException;
        }
    }

    static void reply(ClientSession clientSession, String string) throws Exception {
        LiToGmCommands.call(clientSession, "sendGmCommandMessage", new Class[]{String.class}, string);
    }

    static boolean handle(ClientSession clientSession, String string) throws IOException {
        String[] stringArray = (string == null ? "" : string.trim()).split("\\s+");
        String string2 = stringArray[0].toLowerCase(Locale.ROOT);
        if (!string2.equals(".item") && !string2.equals(".spawn")) {
            return false;
        }
        try {
            String string3;
            AccountRepository accountRepository = (AccountRepository)LiToGmCommands.call(clientSession, "selectedSourceAccounts", new Class[0], new Object[0]);
            GmCommandDefinition gmCommandDefinition = accountRepository.gmCommand(string2.substring(1));
            String string4 = string3 = string2.equals(".item") ? "L1CreateItem" : "L1SpawnCmd";
            if (gmCommandDefinition == null || !string2.substring(1).equals(gmCommandDefinition.name()) || gmCommandDefinition.systemCommand() || !string3.equals(gmCommandDefinition.executor())) {
                LiToGmCommands.reply(clientSession, "\u8cc7\u6599\u5eab\u672a\u555f\u7528 " + string2 + "\uff0c\u6216\u57f7\u884c\u5668\u8a2d\u5b9a\u4e0d\u7b26\u3002");
                return true;
            }
            String string5 = (String)LiToGmCommands.call(clientSession, "accountName", new Class[0], new Object[0]);
            int n = SourceAdministration.accessLevel(accountRepository, string5, clientSession);
            if (n <= 0 || n < gmCommandDefinition.requiredAccessLevel()) {
                LiToGmCommands.reply(clientSession, "GM \u6b0a\u9650\u4e0d\u8db3\uff1a\u9700\u8981 access_level " + Math.max(1, gmCommandDefinition.requiredAccessLevel()) + "\u3002");
                return true;
            }
            if (stringArray.length < 2 || stringArray.length > 4) {
                LiToGmCommands.usage(clientSession, string2);
                return true;
            }
            int n2 = stringArray.length > 2 ? LiToGmCommands.positive(stringArray[2]) : 1;
            int n3 = stringArray.length > 3 ? LiToGmCommands.integer(stringArray[3]) : 0;
            String string6 = LiToGmCommands.get(clientSession, "phase").toString();
            if (!Set.of("CHANNEL", "ROOM", "BATTLE", "WORLD").contains(string6) || ((String)LiToGmCommands.get(clientSession, "selectedCharacter")).isBlank()) {
                LiToGmCommands.reply(clientSession, "\u8acb\u5148\u9078\u64c7\u89d2\u8272\u4e26\u9032\u5165\u5927\u5ef3\u6216\u904a\u6232\u4e16\u754c\u3002");
                return true;
            }
            if (string2.equals(".item")) {
                LiToGmCommands.createItem(clientSession, accountRepository, stringArray[1], n2, n3);
            } else {
                LiToGmNpcs.spawn(clientSession, accountRepository, stringArray[1], n2, n3);
            }
        }
        catch (NumberFormatException numberFormatException) {
            try {
                LiToGmCommands.usage(clientSession, string2);
            }
            catch (Exception exception) {
                throw new IOException(exception);
            }
        }
        catch (IllegalArgumentException illegalArgumentException) {
            try {
                LiToGmCommands.reply(clientSession, illegalArgumentException.getMessage());
            }
            catch (Exception exception) {
                throw new IOException(exception);
            }
        }
        catch (Exception exception) {
            System.err.println("[LiTo] GM " + string2 + " failed: " + exception.getClass().getSimpleName());
            try {
                LiToGmCommands.reply(clientSession, "GM \u6307\u4ee4\u672a\u5b8c\u6210\uff0c\u8acb\u67e5\u770b\u4f3a\u670d\u5668\u7d00\u9304\uff1b\u8acb\u52ff\u9023\u7e8c\u91cd\u9001\u3002");
            }
            catch (Exception exception2) {
                throw new IOException(exception2);
            }
            exception.printStackTrace(System.err);
        }
        return true;
    }

    static int integer(String string) {
        if (!string.matches("-?[0-9]{1,10}")) {
            throw new NumberFormatException();
        }
        return Integer.parseInt(string);
    }

    static int positive(String string) {
        int n = LiToGmCommands.integer(string);
        if (n <= 0) {
            throw new NumberFormatException();
        }
        return n;
    }

    static void usage(ClientSession clientSession, String string) throws Exception {
        LiToGmCommands.reply(clientSession, string.equals(".item") ? "\u7528\u6cd5\uff1a.item \u9053\u5177ID\u6216\u540d\u7a31 [\u6578\u91cf=1] [\u5f37\u5316\u503c=0]\uff1b\u975e\u5806\u758a\u6700\u591a10\u500b\uff0c\u5f37\u5316 -128\uff5e127\u3002" : "\u7528\u6cd5\uff1a.spawn NPC_ID\u6216\u540d\u7a31 [\u6578\u91cf=1] [\u7bc4\u570d=0]\uff1b\u6578\u91cf1\uff5e100\uff0c\u7bc4\u570d0\uff5e20\u3002");
    }

    static Connection connection(AccountRepository accountRepository) throws Exception {
        if (!accountRepository.getClass().getSimpleName().equals("LegacyMySqlRepository")) {
            throw new IllegalArgumentException("GM \u5efa\u7acb\u6307\u4ee4\u9700\u8981\u6b63\u5f0f SQL \u4f86\u6e90\u3002");
        }
        Connection connection = ((DataSource)LiToGmCommands.get(accountRepository, "dataSource")).getConnection();
        connection.setReadOnly(true);
        return connection;
    }

    static String table(AccountRepository accountRepository, String string) throws Exception {
        String string2 = (String)LiToGmCommands.get(accountRepository, "resourceDatabase");
        if (!string2.matches("[A-Za-z0-9_]+")) {
            throw new SQLException("invalid schema");
        }
        return "`" + string2 + "`.`" + string + "`";
    }

    static int resolve(AccountRepository accountRepository, Connection connection, String string, boolean bl) throws Exception {
        if (string.matches("[0-9]+")) {
            return LiToGmCommands.positive(string);
        }
        LinkedHashSet<Integer> linkedHashSet = new LinkedHashSet<Integer>();
        String string2 = bl ? "npcid" : "item_id";
        for (String string3 : bl ? List.of("npc") : List.of("weapon", "armor", "etcitem")) {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT " + string2 + " FROM " + LiToGmCommands.table(accountRepository, string3) + " WHERE REPLACE(name,' ','')=? LIMIT 2");
            try {
                preparedStatement.setString(1, string);
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    while (resultSet.next()) {
                        linkedHashSet.add(resultSet.getInt(1));
                    }
                }
                finally {
                    if (resultSet == null) continue;
                    resultSet.close();
                }
            }
            finally {
                if (preparedStatement == null) continue;
                preparedStatement.close();
            }
        }
        if (linkedHashSet.size() != 1) {
            throw new IllegalArgumentException(linkedHashSet.isEmpty() ? "\u627e\u4e0d\u5230\u6307\u5b9a\u540d\u7a31\uff0c\u8acb\u6539\u7528\u8cc7\u6599\u8868 ID\u3002" : "\u540d\u7a31\u91cd\u8907\uff0c\u8acb\u6539\u7528\u8cc7\u6599\u8868 ID\u3002");
        }
        return (Integer)linkedHashSet.iterator().next();
    }

    static void createItem(ClientSession clientSession, AccountRepository accountRepository, String string, int n, int n2) throws Exception {
        LongSupplier longSupplier;
        InventoryCatalog.Definition definition;
        if (n2 < -128 || n2 > 127) {
            throw new IllegalArgumentException("\u5f37\u5316\u503c\u5fc5\u9808\u4ecb\u65bc -128\uff5e127\uff0c\u907f\u514d\u539f\u751f\u5c01\u5305\u6ea2\u4f4d\u3002");
        }
        try (Object object = LiToGmCommands.connection(accountRepository);){
            int n3 = LiToGmCommands.resolve(accountRepository, (Connection)object, string, false);
            definition = (InventoryCatalog.Definition)LiToGmCommands.call(accountRepository, "rewardItemDefinition", new Class[]{Connection.class, Integer.TYPE}, object, n3);
        }
        if (definition == null) {
            throw new IllegalArgumentException("\u9053\u5177\u4e0d\u5b58\u5728\u6216\u8cc7\u6599\u8868\u5b9a\u7fa9\u4e0d\u5b8c\u6574\u3002");
        }
        if (!definition.stackable() && n > 10) {
            throw new IllegalArgumentException("\u975e\u5806\u758a\u9053\u5177\u6bcf\u6b21\u6700\u591a\u5efa\u7acb10\u500b\u3002");
        }
        if (definition.stackable()) {
            n2 = 0;
        }
        if (n2 != 0 && !definition.weapon() && !definition.armor()) {
            throw new IllegalArgumentException("\u9019\u500b\u9053\u5177\u4e0d\u652f\u63f4\u5f37\u5316\u503c\u3002");
        }
        object = (RuntimeInventory)LiToGmCommands.get(clientSession, "inventory");
        RuntimeInventory runtimeInventory = ((RuntimeInventory)object).copy();
        if (!((Boolean)LiToGmCommands.call(clientSession, "permitsInventoryAddition", new Class[]{RuntimeInventory.class, InventoryCatalog.Definition.class, Long.TYPE}, object, definition, n)).booleanValue()) {
            throw new IllegalArgumentException("\u80cc\u5305\u8ca0\u91cd\u4e0d\u8db3\uff0c\u6c92\u6709\u5efa\u7acb\u9053\u5177\u3002");
        }
        RuntimeInventory.PersistentItem persistentItem = new RuntimeInventory.PersistentItem(0L, n, n2, 0, true, definition.blessing(), false, true, false, definition, definition.initialChargeCount(), 0L, false, false);
        if (!((Boolean)LiToGmCommands.call(runtimeInventory, "addState", new Class[]{InventoryCatalog.Definition.class, RuntimeInventory.PersistentItem.class, LongSupplier.class}, definition, persistentItem, longSupplier = () -> {
            try {
                return (Long)LiToGmCommands.callStatic(RuntimeInventory.class, "nextObjectId", new Class[0], new Object[0]);
            }
            catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        })).booleanValue()) {
            throw new IllegalArgumentException("\u80cc\u5305\u7a7a\u9593\u6216\u5806\u758a\u6578\u91cf\u5df2\u9054\u4e0a\u9650\uff0c\u6c92\u6709\u5efa\u7acb\u9053\u5177\u3002");
        }
        LiToGmCommands.call(clientSession, "persistSqlTournamentRewards", new Class[]{RuntimeInventory.class}, runtimeInventory);
        GameStateStore gameStateStore = (GameStateStore)LiToGmCommands.get(clientSession, "state");
        String string2 = (String)LiToGmCommands.call(clientSession, "stateCharacter", new Class[0], new Object[0]);
        gameStateStore.saveEconomyState(string2, (Long)LiToGmCommands.get(clientSession, "shopPoints"), runtimeInventory.catalogCounts(), runtimeInventory.catalogItemStates());
        LiToGmCommands.set(clientSession, "inventory", runtimeInventory);
        LiToGmCommands.call(clientSession, "sendInventoryDelta", new Class[]{RuntimeInventory.class, RuntimeInventory.class}, object, runtimeInventory);
        LiToGmCommands.reply(clientSession, "\u5df2\u5efa\u7acb " + definition.name() + " \u00d7" + n + (String)(n2 == 0 ? "" : "\uff08\u5f37\u5316 " + n2 + "\uff09") + "\uff0c\u5df2\u52a0\u5165\u80cc\u5305\u4e26\u4fdd\u5b58\u3002");
        System.out.println("[LiTo] GM item character=" + string2 + " item=" + definition.catalogId() + " count=" + n + " enchant=" + n2);
    }

    static Object callStatic(Class<?> clazz, String string, Class<?>[] classArray, Object ... objectArray) throws Exception {
        Method method = clazz.getDeclaredMethod(string, classArray);
        method.setAccessible(true);
        return method.invoke(null, objectArray);
    }
}

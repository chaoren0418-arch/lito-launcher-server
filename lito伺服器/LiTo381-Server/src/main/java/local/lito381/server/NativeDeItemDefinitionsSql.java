// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class NativeDeItemDefinitionsSql {
    static final Set<String> QUERIES = Set.of("SELECT * FROM `\u88dd\u5099_weapon_\u5c6c\u6027` ORDER BY `id`", "SELECT * FROM `\u88dd\u5099_weapon_bless_\u985e\u578b`", "SELECT * FROM `\u88dd\u5099_armor_bless_\u985e\u578b`", "SELECT * FROM `\u88dd\u5099_weapon_enchant_\u985e\u578b`", "SELECT * FROM `\u88dd\u5099_armor_enchant_\u985e\u578b`", "SELECT * FROM `\u88dd\u5099_weapon_enchant_\u6307\u5b9a`", "SELECT * FROM `\u88dd\u5099_armor_enchant_\u6307\u5b9a`", "SELECT * FROM `\u88dd\u5099_weapon_greater_\u985e\u578b`", "SELECT * FROM `\u88dd\u5099_armor_greater_\u985e\u578b`", "SELECT * FROM `\u88dd\u5099_weapon_bless_\u6307\u5b9a`", "SELECT * FROM `\u88dd\u5099_armor_bless_\u6307\u5b9a`", "SELECT * FROM `\u88dd\u5099_\u7279\u6b8a_\u9b54\u77f3_\u9b54\u6cd5` ORDER BY `item_id`, `tier`", "SELECT * FROM `\u88dd\u5099_\u7279\u6b8a_\u5361\u7247_\u80fd\u529b`", "SELECT * FROM `\u88dd\u5099_weapon_\u9b54\u6cd5` ORDER BY `weapon_id`, `id`", "SELECT * FROM `\u88dd\u5099_armor_\u9b54\u6cd5` ORDER BY `item_id`, `enchant_level`, `proc_group`, `id`");
    static final Set<String> TEMPLATE_QUERIES = Set.of("SELECT e.*, a.item_id AS arrow_config_id, a.consume_on_attack AS arrow_consume_on_attack, a.dmg_small AS arrow_dmg_small, a.dmg_large AS arrow_dmg_large, a.arrow_spr AS arrow_config_spr FROM `etcitem` e LEFT JOIN `etcitem_arrow` a ON a.item_id = e.item_id", "SELECT * FROM `weapon`", "SELECT * FROM `armor`");
    private final Set<String> allowedQueries;
    private final Connection raw;
    private final List<Lease> leases = new ArrayList<Lease>();
    private final Set<String> executed = new LinkedHashSet<String>();
    private int rolledBack;
    private int physicalClosed;
    private int statements;
    private int statementsClosed;
    private int resultSets;
    private int resultSetsClosed;
    private int lifecycleFailures;
    private boolean active = true;

    NativeDeItemDefinitionsSql(Connection connection) throws SQLException {
        this(connection, QUERIES);
    }

    NativeDeItemDefinitionsSql(Connection connection, Set<String> set) throws SQLException {
        if (!QUERIES.equals(set) && !TEMPLATE_QUERIES.equals(set)) {
            throw new IllegalArgumentException("unknown source static query family");
        }
        this.raw = Objects.requireNonNull(connection);
        this.allowedQueries = Set.copyOf(set);
    }

    void configure() throws SQLException {
        this.raw.setReadOnly(true);
        this.raw.setAutoCommit(false);
        this.raw.setTransactionIsolation(4);
        if (!this.raw.isReadOnly() || this.raw.getAutoCommit() || this.raw.getTransactionIsolation() != 4) {
            throw new SQLException("owned item definition transaction policy was not applied");
        }
    }

    private static Object invoke(Object object, Method method, Object[] objectArray) throws Throwable {
        try {
            return method.invoke(object, objectArray);
        }
        catch (InvocationTargetException invocationTargetException) {
            throw invocationTargetException.getCause();
        }
    }

    private static Object objectMethod(Object object, Method method, Object[] objectArray) {
        return switch (method.getName()) {
            case "toString" -> "OwnedItemDefinitionJdbc(" + object.getClass().getInterfaces()[0].getSimpleName() + ")";
            case "hashCode" -> System.identityHashCode(object);
            case "equals" -> object == objectArray[0];
            default -> throw new UnsupportedOperationException(method.getName());
        };
    }

    private SQLException forbidden(String string) {
        ++this.lifecycleFailures;
        return new SQLException("item definition JDBC operation refused: " + string);
    }

    Connection lease() throws SQLException {
        if (!this.active) {
            throw this.forbidden("closed transaction lease");
        }
        Lease lease = new Lease();
        this.leases.add(lease);
        return lease.proxy;
    }

    void validate() throws SQLException {
        if (!this.executed.equals(this.allowedQueries) || this.leases.size() != this.allowedQueries.size() || this.statements != this.allowedQueries.size() || this.resultSets != this.allowedQueries.size() || this.statementsClosed != this.allowedQueries.size() || this.resultSetsClosed != this.allowedQueries.size() || this.leases.stream().anyMatch(lease -> !lease.closed) || this.lifecycleFailures != 0) {
            throw new SQLException("incomplete source item definition query/resource inventory");
        }
    }

    void finish() throws SQLException {
        SQLException sQLException = null;
        for (Lease object2 : this.leases) {
            for (StatementLease statementLease : object2.statementsOwned) {
                for (ResultLease resultLease : statementLease.results) {
                    if (resultLease.closed) continue;
                    ++this.lifecycleFailures;
                    try {
                        resultLease.close();
                    }
                    catch (Throwable throwable) {
                        SQLException sQLException2;
                        sQLException = NativeDeItemDefinitionsSql.append(sQLException, throwable instanceof SQLException ? (sQLException2 = (SQLException)throwable) : new SQLException("owned leaked-resource cleanup failed", throwable));
                    }
                }
                if (statementLease.closed) continue;
                ++this.lifecycleFailures;
                try {
                    statementLease.close();
                }
                catch (Throwable throwable) {
                    SQLException sQLException3;
                    sQLException = NativeDeItemDefinitionsSql.append(sQLException, throwable instanceof SQLException ? (sQLException3 = (SQLException)throwable) : new SQLException("owned leaked-resource cleanup failed", throwable));
                }
            }
            if (object2.closed) continue;
            ++this.lifecycleFailures;
            object2.closed = true;
        }
        this.active = false;
        try {
            this.raw.rollback();
            ++this.rolledBack;
        }
        catch (Throwable throwable) {
            SQLException sQLException4;
            ++this.lifecycleFailures;
            sQLException = NativeDeItemDefinitionsSql.append(sQLException, throwable instanceof SQLException ? (sQLException4 = (SQLException)throwable) : new SQLException("owned transaction cleanup failed", throwable));
        }
        try {
            this.raw.close();
            ++this.physicalClosed;
        }
        catch (Throwable throwable) {
            SQLException sQLException5;
            ++this.lifecycleFailures;
            sQLException = NativeDeItemDefinitionsSql.append(sQLException, throwable instanceof SQLException ? (sQLException5 = (SQLException)throwable) : new SQLException("owned transaction cleanup failed", throwable));
        }
        if (sQLException != null) {
            throw sQLException;
        }
    }

    private static SQLException append(SQLException sQLException, SQLException sQLException2) {
        if (sQLException == null) {
            return sQLException2;
        }
        sQLException.addSuppressed(sQLException2);
        return sQLException;
    }

    Counts counts() {
        return new Counts(1, this.rolledBack, this.physicalClosed, this.leases.size(), (int)this.leases.stream().filter(lease -> lease.closed).count(), this.statements, this.statementsClosed, this.resultSets, this.resultSetsClosed, this.executed.size(), this.lifecycleFailures);
    }

    private final class Lease {
        final List<StatementLease> statementsOwned = new ArrayList<StatementLease>();
        boolean closed;
        final Connection proxy = (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class[]{Connection.class}, this::call);

        private Lease() {
        }

        Object call(Object object, Method method, Object[] objectArray) throws Throwable {
            Object object2;
            if (method.getDeclaringClass() == Object.class) {
                return NativeDeItemDefinitionsSql.objectMethod(object, method, objectArray);
            }
            String string = method.getName();
            if (string.equals("close")) {
                this.closed = true;
                return null;
            }
            if (string.equals("isClosed")) {
                return this.closed || !NativeDeItemDefinitionsSql.this.active;
            }
            if (this.closed || !NativeDeItemDefinitionsSql.this.active) {
                throw NativeDeItemDefinitionsSql.this.forbidden("closed source connection " + string);
            }
            if (string.equals("isWrapperFor")) {
                return false;
            }
            if (string.equals("prepareStatement") && objectArray.length == 1 && (object2 = objectArray[0]) instanceof String) {
                String string2 = (String)object2;
                if (!NativeDeItemDefinitionsSql.this.allowedQueries.contains(string2)) {
                    throw NativeDeItemDefinitionsSql.this.forbidden("non-whitelisted query");
                }
                try {
                    object2 = NativeDeItemDefinitionsSql.this.raw.prepareStatement(string2);
                }
                catch (SQLException sQLException) {
                    ++NativeDeItemDefinitionsSql.this.lifecycleFailures;
                    throw sQLException;
                }
                StatementLease statementLease = new StatementLease(this, string2, (PreparedStatement)object2);
                this.statementsOwned.add(statementLease);
                ++NativeDeItemDefinitionsSql.this.statements;
                return statementLease.proxy;
            }
            if (Set.of("isReadOnly", "getAutoCommit", "getTransactionIsolation").contains(string)) {
                return NativeDeItemDefinitionsSql.invoke(NativeDeItemDefinitionsSql.this.raw, method, objectArray);
            }
            throw NativeDeItemDefinitionsSql.this.forbidden(string);
        }
    }

    private final class StatementLease {
        final Lease owner;
        final String query;
        final PreparedStatement rawStatement;
        final List<ResultLease> results = new ArrayList<ResultLease>();
        boolean closed;
        boolean executedOnce;
        final PreparedStatement proxy = (PreparedStatement)Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(), new Class[]{PreparedStatement.class}, this::call);

        StatementLease(Lease lease, String string, PreparedStatement preparedStatement) {
            this.owner = lease;
            this.query = string;
            this.rawStatement = preparedStatement;
        }

        void close() throws SQLException {
            if (this.closed) {
                return;
            }
            this.closed = true;
            try {
                this.rawStatement.close();
                ++NativeDeItemDefinitionsSql.this.statementsClosed;
            }
            catch (SQLException sQLException) {
                ++NativeDeItemDefinitionsSql.this.lifecycleFailures;
                throw sQLException;
            }
        }

        Object call(Object object, Method method, Object[] objectArray) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return NativeDeItemDefinitionsSql.objectMethod(object, method, objectArray);
            }
            String string = method.getName();
            if (string.equals("close")) {
                this.close();
                return null;
            }
            if (string.equals("isClosed")) {
                return this.closed || this.owner.closed || !NativeDeItemDefinitionsSql.this.active;
            }
            if (this.closed || this.owner.closed || !NativeDeItemDefinitionsSql.this.active) {
                throw NativeDeItemDefinitionsSql.this.forbidden("closed source statement " + string);
            }
            if (string.equals("getConnection")) {
                return this.owner.proxy;
            }
            if (string.equals("isWrapperFor")) {
                return false;
            }
            if (string.equals("executeQuery") && (objectArray == null || objectArray.length == 0)) {
                ResultSet resultSet;
                if (this.executedOnce || !NativeDeItemDefinitionsSql.this.executed.add(this.query)) {
                    throw NativeDeItemDefinitionsSql.this.forbidden("duplicate source query");
                }
                this.executedOnce = true;
                try {
                    resultSet = this.rawStatement.executeQuery();
                }
                catch (SQLException sQLException) {
                    ++NativeDeItemDefinitionsSql.this.lifecycleFailures;
                    throw sQLException;
                }
                ResultLease resultLease = new ResultLease(this, resultSet);
                this.results.add(resultLease);
                ++NativeDeItemDefinitionsSql.this.resultSets;
                return resultLease.proxy;
            }
            throw NativeDeItemDefinitionsSql.this.forbidden(string);
        }
    }

    private final class ResultLease {
        final StatementLease owner;
        final ResultSet rawResult;
        boolean closed;
        final ResultSet proxy = (ResultSet)Proxy.newProxyInstance(ResultSet.class.getClassLoader(), new Class[]{ResultSet.class}, this::call);

        ResultLease(StatementLease statementLease, ResultSet resultSet) {
            this.owner = statementLease;
            this.rawResult = resultSet;
        }

        void close() throws SQLException {
            if (this.closed) {
                return;
            }
            this.closed = true;
            try {
                this.rawResult.close();
                ++NativeDeItemDefinitionsSql.this.resultSetsClosed;
            }
            catch (SQLException sQLException) {
                ++NativeDeItemDefinitionsSql.this.lifecycleFailures;
                throw sQLException;
            }
        }

        Object call(Object object, Method method, Object[] objectArray) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return NativeDeItemDefinitionsSql.objectMethod(object, method, objectArray);
            }
            String string = method.getName();
            if (string.equals("close")) {
                this.close();
                return null;
            }
            if (string.equals("isClosed")) {
                return this.closed || this.owner.closed || this.owner.owner.closed || !NativeDeItemDefinitionsSql.this.active;
            }
            if (this.closed || this.owner.closed || this.owner.owner.closed || !NativeDeItemDefinitionsSql.this.active) {
                throw NativeDeItemDefinitionsSql.this.forbidden("closed source result set " + string);
            }
            if (string.equals("getStatement")) {
                return this.owner.proxy;
            }
            if (string.equals("isWrapperFor")) {
                return false;
            }
            if (string.equals("next") || string.equals("findColumn") || string.equals("wasNull") || string.equals("getMetaData") || Set.of("getInt", "getLong", "getDouble", "getFloat", "getString", "getBoolean", "getShort", "getByte", "getBigDecimal", "getObject").contains(string)) {
                return NativeDeItemDefinitionsSql.invoke(this.rawResult, method, objectArray);
            }
            throw NativeDeItemDefinitionsSql.this.forbidden(string);
        }
    }

    record Counts(int physicalOpened, int rolledBack, int physicalClosed, int leases, int leasesClosed, int statements, int statementsClosed, int resultSets, int resultSetsClosed, int queries, int lifecycleFailures) {
    }
}

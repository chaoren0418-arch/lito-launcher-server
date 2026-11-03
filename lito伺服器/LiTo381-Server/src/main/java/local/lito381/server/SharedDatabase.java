// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;
import local.lito381.server.AccountRepository;
import local.lito381.server.DurableBattleSettlement;
import local.lito381.server.GameStateStore;
import local.lito381.server.LegacyMySqlRepository;
import local.lito381.server.RepositoryTournamentMutation;
import local.lito381.server.RepositoryTournamentMutationStatus;
import local.lito381.server.RepositoryTournamentProfile;
import local.lito381.server.RepositoryTournamentRoster;
import local.lito381.server.ServerConfig;
import local.lito381.server.SourceRoutedAccountRepository;
import local.lito381.server.UnifiedConfiguration;

final class SharedDatabase {
    private static final ThreadLocal<Boolean> SETTLEMENT = new ThreadLocal();
    static final String POLICY = "source-readonly-stats-only";
    static final Set<String> TABLES = Set.of("lito_battle_rule", "lito_battle_object", "lito_battle_spawn", "lito_battle_player_spawn", "lito_tournament_shop", "commands");
    static final Map<LegacyMySqlRepository, Route> ROUTES = Collections.synchronizedMap(new WeakHashMap());

    SharedDatabase() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static RepositoryTournamentMutation commitResult(AccountRepository accountRepository, long l, String string, RepositoryTournamentProfile repositoryTournamentProfile, GameStateStore.TournamentProfileState tournamentProfileState) throws Exception {
        Boolean bl = SETTLEMENT.get();
        SETTLEMENT.set(Boolean.TRUE);
        try {
            RepositoryTournamentMutation repositoryTournamentMutation = accountRepository.persistTournamentProfile(l, string, repositoryTournamentProfile, tournamentProfileState);
            return repositoryTournamentMutation;
        }
        finally {
            if (bl == null) {
                SETTLEMENT.remove();
            } else {
                SETTLEMENT.set(bl);
            }
        }
    }

    static Properties properties(Path path) throws Exception {
        return UnifiedConfiguration.read(path);
    }

    static boolean enabled(ServerConfig serverConfig) throws Exception {
        String string = SharedDatabase.properties(serverConfig.source()).getProperty("database.persistence.policy", "");
        if (!string.isBlank() && !POLICY.equals(string)) {
            throw new IllegalArgumentException("Unknown persistence policy");
        }
        return POLICY.equals(string);
    }

    static String identifier(String string) {
        if (string == null || !string.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid database identifier");
        }
        return string;
    }

    static AccountRepository open(ServerConfig serverConfig) throws Exception {
        if (!serverConfig.databaseReadOnly() || serverConfig.statePersistenceEnabled() || serverConfig.classicWorldCompatibilityEnabled()) {
            throw new IllegalArgumentException("Shared sources require read-only, memory state and arena mode");
        }
        Properties properties = SharedDatabase.properties(serverConfig.source());
        Properties properties2 = SharedDatabase.properties(serverConfig.legacySqlProperties());
        String string = SharedDatabase.identifier(properties.getProperty("database.lito.database"));
        if (!"character_lito".equals(properties.getProperty("database.lito.character.table"))) {
            throw new IllegalArgumentException("Only character_lito writes are supported");
        }
        LinkedHashMap<String, AccountRepository> linkedHashMap = new LinkedHashMap<String, AccountRepository>();
        try {
            for (int i = 0; i < serverConfig.classicServerNames().size(); ++i) {
                String string2 = serverConfig.classicServerNames().get(i);
                String string3 = SharedDatabase.identifier(serverConfig.legacySourceDatabases().get(i));
                LegacyMySqlRepository legacyMySqlRepository = new LegacyMySqlRepository(serverConfig.legacySqlProperties(), string2, serverConfig.classicServerIds().get(i), string3, serverConfig.legacySourceResourceDatabases().get(i), serverConfig.legacySourceWeightRates().get(i), true, "legacy_mysql");
                ROUTES.put(legacyMySqlRepository, new Route(string2, string3, string, properties2));
                linkedHashMap.put(string2, legacyMySqlRepository);
            }
            DurableBattleSettlement.open(serverConfig);
            return linkedHashMap.size() == 1 ? (AccountRepository)linkedHashMap.values().iterator().next() : new SourceRoutedAccountRepository(linkedHashMap);
        }
        catch (Exception exception) {
            for (AccountRepository accountRepository : linkedHashMap.values()) {
                accountRepository.close();
            }
            throw exception;
        }
    }

    static boolean registered(LegacyMySqlRepository legacyMySqlRepository) {
        return ROUTES.containsKey(legacyMySqlRepository);
    }

    static String resourceTable(LegacyMySqlRepository legacyMySqlRepository, String string, String string2) {
        SharedDatabase.identifier(string);
        Route route = ROUTES.get(legacyMySqlRepository);
        return "`" + (route != null && TABLES.contains(string) ? route.controls : SharedDatabase.identifier(string2)) + "`.`" + string + "`";
    }

    static void check(LegacyMySqlRepository legacyMySqlRepository) throws Exception {
        Route route = ROUTES.get(legacyMySqlRepository);
        if (route == null) {
            return;
        }
        try (Connection connection = route.connection();
             Statement statement = connection.createStatement();){
            connection.setReadOnly(true);
            ResultSet resultSet = statement.executeQuery("SELECT char_obj_id,char_name,wins,losses,version,updated_at FROM character_lito LIMIT 0");
            if (resultSet != null) {
                resultSet.close();
            }
            resultSet = statement.executeQuery("SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='character_lito'");
            try {
                if (!resultSet.next() || !"InnoDB".equalsIgnoreCase(resultSet.getString(1)) && !"MyISAM".equalsIgnoreCase(resultSet.getString(1))) {
                    throw new SQLException("character_lito requires InnoDB or MyISAM");
                }
            }
            finally {
                if (resultSet != null) {
                    resultSet.close();
                }
            }
        }
        System.out.println("[LiTo] shared-source=" + route.database + " controls=" + route.controls + " source-read-only=true write-table=character_lito");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static RepositoryTournamentRoster profiles(LegacyMySqlRepository legacyMySqlRepository) throws Exception {
        Route route;
        Route route2 = route = ROUTES.get(legacyMySqlRepository);
        synchronized (route2) {
            ArrayList<RepositoryTournamentProfile> arrayList = new ArrayList<RepositoryTournamentProfile>();
            try (Object object = route.connection();
                 Statement statement = object.createStatement();){
                object.setReadOnly(true);
                try (ResultSet object2 = statement.executeQuery("SELECT c.objid,c.char_name,t.wins,t.losses,t.version FROM characters c LEFT JOIN character_lito t ON t.char_obj_id=c.objid WHERE c.Banned=0 ORDER BY c.objid");){
                    while (object2.next()) {
                        arrayList.add(route.profile(object2.getLong(1), object2.getString(2), object2.getInt(3), object2.getInt(4), object2.getLong(5)));
                    }
                }
            }
            object = new HashSet();
            for (RepositoryTournamentProfile repositoryTournamentProfile : arrayList) {
                object.add(repositoryTournamentProfile.characterObjectId());
            }
            route.runtime.keySet().retainAll((Collection<?>)object);
            return new RepositoryTournamentRoster(true, arrayList);
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    static RepositoryTournamentMutation persist(LegacyMySqlRepository legacyMySqlRepository, long l, String string, RepositoryTournamentProfile repositoryTournamentProfile, GameStateStore.TournamentProfileState tournamentProfileState) throws Exception {
        Route route = ROUTES.get(legacyMySqlRepository);
        if (!Boolean.TRUE.equals(SETTLEMENT.get())) {
            throw new IllegalStateException("character_lito writes require battle settlement");
        }
        Route route2 = route;
        synchronized (route2) {
            boolean bl;
            Object object;
            Connection connection;
            block58: {
                if (l <= 0L) throw new IllegalArgumentException("Invalid source-qualified result");
                if (tournamentProfileState == null) throw new IllegalArgumentException("Invalid source-qualified result");
                if (!route.name.equals(tournamentProfileState.classicServerName())) throw new IllegalArgumentException("Invalid source-qualified result");
                if (repositoryTournamentProfile != null) {
                    if (repositoryTournamentProfile.characterObjectId() != l) throw new IllegalArgumentException("Invalid source-qualified result");
                    if (!repositoryTournamentProfile.characterName().equals(string)) {
                        throw new IllegalArgumentException("Invalid source-qualified result");
                    }
                }
                connection = route.connection();
                connection.setAutoCommit(false);
                try {
                    String string2;
                    object = connection.prepareStatement("SELECT char_name FROM characters WHERE objid=? AND Banned=0");
                    try {
                        object.setLong(1, l);
                        try (ResultSet resultSet = object.executeQuery();){
                            string2 = resultSet.next() ? resultSet.getString(1) : null;
                        }
                    }
                    finally {
                        if (object != null) {
                            object.close();
                        }
                    }
                    if (Objects.equals(string2, string)) break block58;
                    connection.rollback();
                    object = new RepositoryTournamentMutation(RepositoryTournamentMutationStatus.NOT_FOUND, null);
                }
                catch (Exception exception) {
                    connection.rollback();
                    throw exception;
                }
                return object;
            }
            try (Object object2 = connection.prepareStatement("SELECT wins,losses,version FROM character_lito WHERE char_obj_id=?");){
                object2.setLong(1, l);
                try (ResultSet resultSet = object2.executeQuery();){
                    bl = resultSet.next();
                    object = bl ? route.profile(l, string, resultSet.getInt(1), resultSet.getInt(2), resultSet.getLong(3)) : route.profile(l, string, 0, 0, 0L);
                }
            }
            if (repositoryTournamentProfile == null ? ((RepositoryTournamentProfile)object).snapshotVersion() != 0L : repositoryTournamentProfile.snapshotVersion() != ((RepositoryTournamentProfile)object).snapshotVersion() || repositoryTournamentProfile.state().wins() != ((RepositoryTournamentProfile)object).state().wins() || repositoryTournamentProfile.state().losses() != ((RepositoryTournamentProfile)object).state().losses()) {
                connection.rollback();
                object2 = new RepositoryTournamentMutation(RepositoryTournamentMutationStatus.CONFLICT, (RepositoryTournamentProfile)object);
                return object2;
            }
            boolean bl2 = ((RepositoryTournamentProfile)object).state().wins() != tournamentProfileState.wins() || ((RepositoryTournamentProfile)object).state().losses() != tournamentProfileState.losses();
            long l2 = ((RepositoryTournamentProfile)object).snapshotVersion();
            if (bl2) {
                PreparedStatement preparedStatement;
                block60: {
                    RepositoryTournamentMutation repositoryTournamentMutation;
                    long l3 = (long)tournamentProfileState.wins() - (long)((RepositoryTournamentProfile)object).state().wins();
                    long l4 = (long)tournamentProfileState.losses() - (long)((RepositoryTournamentProfile)object).state().losses();
                    if (l3 != 1L || l4 != 0L) {
                        if (l3 != 0L) throw new SQLException("Settlement must add exactly one win or loss");
                        if (l4 != 1L) {
                            throw new SQLException("Settlement must add exactly one win or loss");
                        }
                    }
                    if ((l2 = Math.addExact(l2, 1L)) > 0xFFFFFFFFL) {
                        throw new SQLException("Result version exhausted");
                    }
                    String string3 = !bl ? "INSERT INTO character_lito(char_obj_id,char_name,wins,losses,version) VALUES(?,?,?,?,?)" : "UPDATE character_lito SET char_name=?,wins=?,losses=?,version=? WHERE char_obj_id=? AND version=? AND wins=? AND losses=?";
                    try {
                        preparedStatement = connection.prepareStatement(string3);
                        try {
                            if (!bl) {
                                preparedStatement.setLong(1, l);
                                preparedStatement.setString(2, string);
                                preparedStatement.setInt(3, tournamentProfileState.wins());
                                preparedStatement.setInt(4, tournamentProfileState.losses());
                                preparedStatement.setLong(5, l2);
                            } else {
                                preparedStatement.setString(1, string);
                                preparedStatement.setInt(2, tournamentProfileState.wins());
                                preparedStatement.setInt(3, tournamentProfileState.losses());
                                preparedStatement.setLong(4, l2);
                                preparedStatement.setLong(5, l);
                                preparedStatement.setLong(6, ((RepositoryTournamentProfile)object).snapshotVersion());
                                preparedStatement.setInt(7, ((RepositoryTournamentProfile)object).state().wins());
                                preparedStatement.setInt(8, ((RepositoryTournamentProfile)object).state().losses());
                            }
                            if (preparedStatement.executeUpdate() == 1) break block60;
                            connection.rollback();
                            repositoryTournamentMutation = new RepositoryTournamentMutation(RepositoryTournamentMutationStatus.CONFLICT, (RepositoryTournamentProfile)object);
                            if (preparedStatement == null) return repositoryTournamentMutation;
                        }
                        catch (Throwable throwable) {
                            if (preparedStatement == null) throw throwable;
                            try {
                                preparedStatement.close();
                                throw throwable;
                            }
                            catch (Throwable throwable2) {
                                throwable.addSuppressed(throwable2);
                            }
                            throw throwable;
                        }
                        preparedStatement.close();
                    }
                    catch (SQLException sQLException) {
                        if (bl) throw sQLException;
                        if (sQLException.getErrorCode() != 1062) throw sQLException;
                        connection.rollback();
                        RepositoryTournamentMutation repositoryTournamentMutation2 = new RepositoryTournamentMutation(RepositoryTournamentMutationStatus.CONFLICT, (RepositoryTournamentProfile)object);
                        if (connection == null) return repositoryTournamentMutation2;
                        connection.close();
                        return repositoryTournamentMutation2;
                    }
                    return repositoryTournamentMutation;
                }
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            }
            connection.commit();
            RepositoryTournamentProfile repositoryTournamentProfile2 = new RepositoryTournamentProfile(l, string, l2, tournamentProfileState);
            route.runtime.put(l, repositoryTournamentProfile2);
            return new RepositoryTournamentMutation(RepositoryTournamentMutationStatus.SUCCESS, repositoryTournamentProfile2);
            finally {
                if (connection != null) {
                    connection.close();
                }
            }
        }
    }

    static boolean applySources(Properties properties, Path path) throws Exception {
        if (properties.containsKey("database.jdbc.base.url") || properties.containsKey("configuration.base.file")) {
            UnifiedConfiguration.apply(properties, path);
            return true;
        }
        String string = properties.getProperty("source.servers.file", "");
        if (string.isBlank()) {
            return false;
        }
        Path path2 = path.resolve(string).normalize();
        if (!Files.isRegularFile(path2, new LinkOption[0])) {
            return false;
        }
        Properties properties2 = SharedDatabase.properties(path2);
        if (!properties2.containsKey("source.slots")) {
            return false;
        }
        int n = Integer.parseInt(properties2.getProperty("source.slots"));
        if (n < 1 || n > 8) {
            throw new IllegalArgumentException("source.slots must be 1..8");
        }
        ArrayList<String> arrayList = new ArrayList<String>();
        ArrayList<String> arrayList2 = new ArrayList<String>();
        ArrayList<String> arrayList3 = new ArrayList<String>();
        ArrayList<String> arrayList4 = new ArrayList<String>();
        for (int i = 1; i <= n; ++i) {
            String string2 = "source." + i + ".";
            String string3 = properties2.getProperty(string2 + "enabled", "false");
            if (!Set.of("true", "false").contains(string3)) {
                throw new IllegalArgumentException("Invalid source enabled flag");
            }
            if (!Boolean.parseBoolean(string3)) continue;
            String string4 = properties2.getProperty(string2 + "name", "").trim();
            String string5 = properties2.getProperty(string2 + "id", "").trim();
            if (string4.isEmpty() || string4.contains(",") || !string5.matches("[1-8]")) {
                throw new IllegalArgumentException("Invalid source name/id");
            }
            arrayList.add(string4);
            arrayList2.add(string5);
            arrayList3.add(SharedDatabase.identifier(properties2.getProperty(string2 + "database", "")));
            arrayList4.add(properties2.getProperty(string2 + "weight.rate", "100.0"));
        }
        if (arrayList.isEmpty() || new HashSet(arrayList).size() != arrayList.size() || new HashSet(arrayList2).size() != arrayList2.size() || new HashSet(arrayList3).size() != arrayList3.size()) {
            throw new IllegalArgumentException("Sources must be nonempty and unique");
        }
        properties.setProperty("login.classic.server.names", String.join((CharSequence)",", arrayList));
        properties.setProperty("login.classic.server.ids", String.join((CharSequence)",", arrayList2));
        properties.setProperty("database.dynamic.databases", String.join((CharSequence)",", arrayList3));
        properties.setProperty("database.source.weight.rates", String.join((CharSequence)",", arrayList4));
        return true;
    }

    static final class Route {
        final String name;
        final String database;
        final String controls;
        final String url;
        final String user;
        final String password;
        final Map<Long, RepositoryTournamentProfile> runtime = new HashMap<Long, RepositoryTournamentProfile>();

        Route(String string, String string2, String string3, Properties properties) {
            this.name = string;
            this.database = string2;
            this.controls = string3;
            this.url = properties.getProperty("URL1_LOGIN") + string2 + properties.getProperty("URL3_LOGIN", "");
            this.user = properties.getProperty("Login_LOGIN", properties.getProperty("Login"));
            this.password = properties.getProperty("Password_LOGIN", properties.getProperty("Password", ""));
        }

        Connection connection() throws SQLException {
            return DriverManager.getConnection(this.url, this.user, this.password);
        }

        RepositoryTournamentProfile profile(long l, String string, int n, int n2, long l2) {
            RepositoryTournamentProfile repositoryTournamentProfile = this.runtime.get(l);
            if (repositoryTournamentProfile != null && repositoryTournamentProfile.characterName().equals(string) && repositoryTournamentProfile.snapshotVersion() == l2 && repositoryTournamentProfile.state().wins() == n && repositoryTournamentProfile.state().losses() == n2) {
                return repositoryTournamentProfile;
            }
            GameStateStore.TournamentProfileState tournamentProfileState = new GameStateStore.TournamentProfileState(0L, n, n2, 0, 0, 0L, 0, 0, Collections.nCopies(12, 0), Collections.nCopies(5, 0), false, this.name, -1, -1, -1, false, 0L, 0, 0, "", -1, -1, -1, false, 0, 0L, 0, 0, 0, 0, 0, 0, 0, 0, 0L, 1, 0L, 0L);
            RepositoryTournamentProfile repositoryTournamentProfile2 = new RepositoryTournamentProfile(l, string, l2, tournamentProfileState);
            if (repositoryTournamentProfile != null) {
                this.runtime.put(l, repositoryTournamentProfile2);
            }
            return repositoryTournamentProfile2;
        }
    }
}

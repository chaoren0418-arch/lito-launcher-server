package local.lito381.server;

import com.mysql.cj.conf.PropertySet;
import com.mysql.cj.protocol.StandardSocketFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Modern-only SELECT DAO. No native pool, repository fallback, schema migration,
 * account creation or write methods. Each operation owns one physical connection;
 * capacity is the server's bounded ModernBackendExecutor, NOT a second hidden pool.
 * Callers must configure a SELECT-only MySQL user: JDBC readOnly is not authorization.
 *
 * Plain passwords intentionally match Accounts.authenticate's exact String equality.
 * No hash autodetection is invented. JDBC requires immutable credential String copies;
 * owned char[] copies and Properties are cleared, but those driver copies cannot be erased.
 */
public final class ModernJdbcBackend {
    // LEFT bounds the returned strings before Connector/J materializes them. Lengths
    // reject overlong values instead of silently accepting a truncated identity/secret.
    static final String AUTH_SQL = "SELECT LEFT(login,65) AS login, OCTET_LENGTH(login) AS login_bytes, "
            + "LEFT(password,257) AS password, OCTET_LENGTH(password) AS password_bytes, banned "
            + "FROM accounts WHERE login = ? LIMIT 2";
    static final String CHARACTERS_SQL = "SELECT objid, LEFT(char_name,257) AS char_name, "
            + "OCTET_LENGTH(char_name) AS name_bytes, Type AS character_type, Sex AS character_sex, level "
            + "FROM characters WHERE account_name = ? AND Banned = 0 ORDER BY objid LIMIT 65";
    private static final int MAX_TRUST_BYTES = 1024 * 1024;
    private static final ThreadLocal<Attempt> ATTEMPT = new ThreadLocal<>();
    private final ModernServerConfig.DbConfig config;
    private final int connectMillis;
    private final int networkMillis;
    private final Connector connector;

    @FunctionalInterface
    interface Connector { Connection connect(String url, Properties properties) throws SQLException; }
    @FunctionalInterface
    private interface Read<T> { T run(Connection connection, ModernBackendScope scope) throws Exception; }

    ModernJdbcBackend(ModernServerConfig.DbConfig config, int connectMillis, int networkMillis) throws SQLException {
        this(config, connectMillis, networkMillis, (url, properties) -> new com.mysql.cj.jdbc.Driver().connect(url, properties));
    }

    /** Package-only JDBC seam: synthetic tests do not connect to any user's database. */
    ModernJdbcBackend(ModernServerConfig.DbConfig config, int connectMillis, int networkMillis, Connector connector)
            throws SQLException {
        this.config = Objects.requireNonNull(config);
        if (connectMillis < 100 || connectMillis > 30_000 || networkMillis < 100 || networkMillis > 30_000
                || !"plain".equals(config.passwordScheme())) {
            throw new IllegalArgumentException("invalid modern JDBC limits or password scheme");
        }
        this.connectMillis = connectMillis;
        this.networkMillis = networkMillis;
        this.connector = Objects.requireNonNull(connector);
        if ("VERIFY_IDENTITY".equals(config.sslMode())) validateTrustStore(config);
        else if (!"DISABLED".equals(config.sslMode()) || config.trustStorePath() != null) throw unavailable();
    }

    AuthResult authenticate(String account, String password) throws Exception {
        ModernBackendScope.current().checkActive();
        if (!validText(account, 64, false) || !validText(password, 256, true)) return AuthResult.rejected(account);
        return read((connection, scope) -> {
            try (PreparedStatement statement = prepare(connection, scope, AUTH_SQL, 2, account);
                 ResultSet result = statement.executeQuery()) {
                scope.checkActive();
                if (!result.next()) return AuthResult.rejected(account);
                String canonical = result.getString("login");
                long canonicalBytes = requiredLong(result, "login_bytes");
                if (!validText(canonical, 64, false) || canonicalBytes < 1 || canonicalBytes > 64) throw unavailable();
                String stored = result.getString("password");
                var storedBytes = ModernJdbcNumbers.nullable(result, "password_bytes");
                // Connector/J may report tinyint(1) as BIT/BOOLEAN. Preserve the raw
                // integer and reject malformed flags instead of getBoolean's coercion.
                long bannedValue = requiredLong(result, "banned");
                if (bannedValue != 0 && bannedValue != 1) throw unavailable();
                boolean banned = bannedValue == 1;
                boolean accepted = !banned && storedBytes.isPresent()
                        && storedBytes.getAsLong() >= 1 && storedBytes.getAsLong() <= 256
                        && password.equals(stored);
                if (result.next()) throw unavailable(); // Never select a winner among ambiguous account rows.
                scope.checkActive();
                return new AuthResult(accepted, banned, canonical);
            }
        });
    }

    List<ModernApplicationProtocol.CharacterRow> characters(String canonicalAccount) throws Exception {
        ModernBackendScope.current().checkActive();
        if (!validText(canonicalAccount, 64, false)) throw unavailable();
        return read((connection, scope) -> {
            List<ModernApplicationProtocol.CharacterRow> rows = new ArrayList<>();
            try (PreparedStatement statement = prepare(connection, scope, CHARACTERS_SQL, 65, canonicalAccount);
                 ResultSet result = statement.executeQuery()) {
                long previousId = 0;
                while (result.next()) {
                    scope.checkActive();
                    if (rows.size() == ModernApplicationProtocol.MAX_ROWS) throw unavailable();
                    long id = requiredLong(result, "objid");
                    String name = result.getString("char_name");
                    long nameBytes = requiredLong(result, "name_bytes");
                    long characterClass = requiredLong(result, "character_type");
                    long sex = requiredLong(result, "character_sex");
                    long level = requiredLong(result, "level");
                    if (id <= previousId || !validText(name, 256, false) || nameBytes < 1 || nameBytes > 256
                            || characterClass < 0 || characterClass > 65_535 || sex < 0 || sex > 1 || level < 1) {
                        throw unavailable();
                    }
                    rows.add(new ModernApplicationProtocol.CharacterRow(id, name, (int) characterClass, (int) sex, level));
                    previousId = id;
                }
                scope.checkActive();
            }
            return List.copyOf(rows);
        });
    }

    static ModernCharacterRepository characters(Map<Integer, ModernJdbcBackend> configured) {
        if (configured == null || configured.isEmpty() || configured.size() > 64
                || configured.keySet().stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("explicit modern JDBC source bindings required");
        }
        Map<Integer, ModernJdbcBackend> bindings = Map.copyOf(configured);
        return (principal, sourceId) -> {
            Objects.requireNonNull(principal);
            if (!principal.authorizesSource(sourceId)) throw new SecurityException("unauthorized character source");
            ModernJdbcBackend backend = bindings.get(sourceId);
            if (backend == null) throw new IllegalStateException("authorized character source unavailable");
            return backend.characters(principal.accountForSource(sourceId));
        };
    }

    boolean usesConfiguration(ModernServerConfig.DbConfig expected) { return config == expected; }

    java.util.Optional<ModernShopCatalog.Snapshot> shopCharacter(int sourceId,
            String canonicalAccount,long objectId,String catalogDatabase) throws Exception {
        return shopCharacter(sourceId,canonicalAccount,objectId,catalogDatabase,false);
    }
    java.util.Optional<ModernShopCatalog.Snapshot> shopCharacter(int sourceId,
            String canonicalAccount,long objectId,String catalogDatabase,boolean bowEnabled) throws Exception {
        ModernBackendScope.current().checkActive();
        if(sourceId<=0 || objectId<=0 || !ModernShopCatalog.validText(canonicalAccount,64))throw unavailable();
        ModernShopCatalogJdbc.sql(catalogDatabase);
        return read((connection,scope)->ModernShopCatalogJdbc.read(connection,scope,
                sourceId,canonicalAccount,objectId,catalogDatabase,bowEnabled));
    }

    java.util.Optional<ModernGmCommands.Snapshot> gmCommandCharacter(int sourceId,
            String canonicalAccount,long objectId,String catalogDatabase) throws Exception {
        ModernBackendScope.current().checkActive();
        if (sourceId <= 0 || objectId <= 0 || !ModernGmCommands.validText(canonicalAccount,64)) throw unavailable();
        ModernGmCommandJdbc.sql(catalogDatabase); // reject identifiers before opening the owned connection
        return read((connection,scope) -> ModernGmCommandJdbc.read(connection,scope,
                sourceId,canonicalAccount,objectId,catalogDatabase));
    }

    java.util.Optional<ModernSkills.Snapshot> skillsCharacter(int sourceId,
            String canonicalAccount, long objectId, String resourceDatabase) throws Exception {
        return skillsCharacter(sourceId,canonicalAccount,objectId,resourceDatabase,false);
    }
    java.util.Optional<ModernSkills.Snapshot> skillsCharacter(int sourceId,
            String canonicalAccount,long objectId,String resourceDatabase,boolean fireStorm) throws Exception {
        ModernBackendScope.current().checkActive();
        if (sourceId <= 0 || objectId <= 0 || !validText(canonicalAccount, 64, false)) throw unavailable();
        ModernSkillsJdbc.sql(resourceDatabase, true);
        return read((connection, scope) -> ModernSkillsJdbc.read(connection, scope,
                sourceId, canonicalAccount, objectId, resourceDatabase,fireStorm));
    }

    java.util.Optional<ModernDisintegrateCatalog.Snapshot> disintegrateCharacter(int sourceId,
            String canonicalAccount,long objectId,String catalogDatabase)throws Exception {
        ModernBackendScope.current().checkActive();
        if(sourceId<=0||objectId<=0||!validText(canonicalAccount,64,false))throw unavailable();
        ModernDisintegrateCatalog.sql(catalogDatabase,true);
        return read((connection,scope)->ModernDisintegrateCatalog.read(connection,scope,sourceId,canonicalAccount,objectId,catalogDatabase));
    }

    java.util.Optional<ModernSlowCatalog.Snapshot> slowCharacter(int sourceId,
            String canonicalAccount,long objectId,String catalogDatabase)throws Exception {
        ModernBackendScope.current().checkActive();
        if(sourceId<=0||objectId<=0||!validText(canonicalAccount,64,false))throw unavailable();
        ModernSlowCatalog.sql(catalogDatabase,true);
        return read((connection,scope)->ModernSlowCatalog.read(connection,scope,
                sourceId,canonicalAccount,objectId,catalogDatabase));
    }

    java.util.Optional<ModernHasteCatalog.Snapshot> hasteCharacter(int sourceId,
            String canonicalAccount,long objectId,String catalogDatabase)throws Exception {
        ModernBackendScope.current().checkActive();
        if(sourceId<=0||objectId<=0||!validText(canonicalAccount,64,false))throw unavailable();
        ModernHasteCatalog.sql(catalogDatabase,true);
        return read((connection,scope)->ModernHasteCatalog.read(connection,scope,
                sourceId,canonicalAccount,objectId,catalogDatabase));
    }

    java.util.Optional<ModernGreaterHasteCatalog.Snapshot> greaterHasteCharacter(int sourceId,
            String canonicalAccount,long objectId,String catalogDatabase)throws Exception {
        ModernBackendScope.current().checkActive();
        if(sourceId<=0||objectId<=0||!validText(canonicalAccount,64,false))throw unavailable();
        // Validate before creating the single owned, deadline-bound connection.
        ModernGreaterHasteCatalog.sql(catalogDatabase,true);
        return read((connection,scope)->ModernGreaterHasteCatalog.read(connection,scope,
                sourceId,canonicalAccount,objectId,catalogDatabase));
    }

    java.util.Optional<ModernCharacterData.Snapshot> persistedCharacter(int sourceId,
            String canonicalAccount, long objectId, String resourceDatabase) throws Exception {
        ModernBackendScope.current().checkActive();
        if (sourceId <= 0 || objectId <= 0 || !validText(canonicalAccount, 64, false)) throw unavailable();
        // No catalog fallback. Validate its identifier before opening a socket.
        ModernCharacterDataJdbc.sql(resourceDatabase);
        return read((connection, scope) -> ModernCharacterDataJdbc.read(connection, scope,
                sourceId, canonicalAccount, objectId, resourceDatabase));
    }

    java.util.Optional<ModernEquipmentFacts.Qualified> equipmentCharacter(int sourceId,
            String canonicalAccount, long objectId, String resourceDatabase) throws Exception {
        ModernBackendScope.current().checkActive();
        if (sourceId <= 0 || objectId <= 0 || !validText(canonicalAccount, 64, false)) throw unavailable();
        ModernEquipmentSourceJdbc.sql(resourceDatabase);
        return read((connection, scope) -> {
            var source = ModernEquipmentSourceJdbc.read(connection, scope, sourceId, canonicalAccount, objectId, resourceDatabase);
            if (source.isEmpty()) return java.util.Optional.empty();
            var facts = source.orElseThrow();
            var catalog = ModernEquipmentCatalogJdbc.read(connection, scope, sourceId, resourceDatabase,
                    ModernEquipmentFacts.requiredLookups(facts.instances()), ModernEquipmentFacts.requiredSetIds(facts.instances()));
            return java.util.Optional.of(facts.qualify(catalog));
        });
    }

    java.util.Optional<ModernCombatProfile.Input> combatCharacter(int sourceId,
            String canonicalAccount, long objectId, String resourceDatabase) throws Exception {
        ModernBackendScope.current().checkActive();
        if (sourceId <= 0 || objectId <= 0 || !validText(canonicalAccount,64,false)) throw unavailable();
        ModernEquipmentSourceJdbc.sql(resourceDatabase);
        return read((connection,scope) -> {
            var source = ModernCombatSourceJdbc.read(connection,scope,sourceId,canonicalAccount,objectId,resourceDatabase);
            if (source.isEmpty()) return java.util.Optional.empty();
            var facts = source.orElseThrow();
            var items = facts.equipment().instances();
            var catalog = ModernEquipmentCatalogJdbc.read(connection,scope,sourceId,resourceDatabase,
                    ModernEquipmentFacts.requiredLookups(items),ModernEquipmentFacts.requiredSetIds(items));
            return java.util.Optional.of(facts.qualify(catalog));
        });
    }

    java.util.Optional<ModernPhysicalCombat.Source> physicalCharacter(int sourceId,
            String canonicalAccount,long objectId,String resourceDatabase) throws Exception {
        return physicalCharacter(sourceId,canonicalAccount,objectId,resourceDatabase,false);
    }
    java.util.Optional<ModernPhysicalCombat.Source> physicalCharacter(int sourceId,
            String canonicalAccount,long objectId,String resourceDatabase,boolean magicEnabled) throws Exception {
        return physicalCharacter(sourceId,canonicalAccount,objectId,resourceDatabase,magicEnabled,false);
    }
    java.util.Optional<ModernPhysicalCombat.Source> physicalCharacter(int sourceId,
            String canonicalAccount,long objectId,String resourceDatabase,boolean magicEnabled,boolean bowEnabled) throws Exception {
        return physicalCharacter(sourceId,canonicalAccount,objectId,resourceDatabase,magicEnabled,bowEnabled,false);
    }
    java.util.Optional<ModernPhysicalCombat.Source> physicalCharacter(int sourceId,
            String canonicalAccount,long objectId,String resourceDatabase,boolean magicEnabled,boolean bowEnabled,boolean regenEnabled) throws Exception {
        return physicalCharacter(sourceId,canonicalAccount,objectId,resourceDatabase,magicEnabled,bowEnabled,regenEnabled,false);
    }
    java.util.Optional<ModernPhysicalCombat.Source> physicalCharacter(int sourceId,
            String canonicalAccount,long objectId,String resourceDatabase,boolean magicEnabled,boolean bowEnabled,boolean regenEnabled,boolean honorEnabled) throws Exception {
        return physicalCharacter(sourceId,canonicalAccount,objectId,resourceDatabase,magicEnabled,bowEnabled,regenEnabled,honorEnabled,false);
    }
    java.util.Optional<ModernPhysicalCombat.Source> physicalCharacter(int sourceId,
            String canonicalAccount,long objectId,String resourceDatabase,boolean magicEnabled,boolean bowEnabled,boolean regenEnabled,boolean honorEnabled,boolean equipmentHasteEnabled) throws Exception {
        return physicalCharacter(sourceId,canonicalAccount,objectId,resourceDatabase,magicEnabled,bowEnabled,regenEnabled,honorEnabled,equipmentHasteEnabled,false);
    }
    java.util.Optional<ModernPhysicalCombat.Source> physicalCharacter(int sourceId,
            String canonicalAccount,long objectId,String resourceDatabase,boolean magicEnabled,boolean bowEnabled,boolean regenEnabled,boolean honorEnabled,boolean equipmentHasteEnabled,boolean playerHealingEnabled) throws Exception {
        return physicalCharacter(sourceId,canonicalAccount,objectId,resourceDatabase,magicEnabled,bowEnabled,regenEnabled,honorEnabled,equipmentHasteEnabled,playerHealingEnabled,false);
    }
    java.util.Optional<ModernPhysicalCombat.Source> physicalCharacter(int sourceId,
            String canonicalAccount,long objectId,String resourceDatabase,boolean magicEnabled,boolean bowEnabled,boolean regenEnabled,boolean honorEnabled,boolean equipmentHasteEnabled,boolean playerHealingEnabled,boolean retainedVip) throws Exception {
        ModernBackendScope.current().checkActive();
        ModernPhysicalCombatSourceJdbc.requireHealingMagic(magicEnabled,playerHealingEnabled);
        if(sourceId<=0||objectId<=0||!validText(canonicalAccount,64,false))throw unavailable();
        ModernEquipmentSourceJdbc.sql(resourceDatabase);
        return read((connection,scope)->ModernPhysicalCombatSourceJdbc.read(connection,scope,sourceId,
                canonicalAccount,objectId,resourceDatabase,magicEnabled,bowEnabled,regenEnabled,honorEnabled,equipmentHasteEnabled,playerHealingEnabled,retainedVip));
    }

    /** Explicit prospective armor feature preparation. Ordinary character,
     * inventory and combat reads do not silently acquire this new dependency.
     * The owner-scoped character read happens before any skill68 query. The
     * returned bundle grants neither a manual skill nor runtime proc execution. */
    java.util.Optional<ModernArmorProtectionCatalog.BoundSource> armorProtectionCharacter(int sourceId,
            String canonicalAccount,long objectId,String resourceDatabase,boolean magicEnabled,boolean bowEnabled,
            boolean regenEnabled,boolean honorEnabled,boolean equipmentHasteEnabled,boolean playerHealingEnabled) throws Exception {
        return armorProtectionCharacter(sourceId,canonicalAccount,objectId,resourceDatabase,magicEnabled,bowEnabled,regenEnabled,honorEnabled,equipmentHasteEnabled,playerHealingEnabled,false);
    }
    java.util.Optional<ModernArmorProtectionCatalog.BoundSource> armorProtectionCharacter(int sourceId,
            String canonicalAccount,long objectId,String resourceDatabase,boolean magicEnabled,boolean bowEnabled,
            boolean regenEnabled,boolean honorEnabled,boolean equipmentHasteEnabled,boolean playerHealingEnabled,boolean retainedVip) throws Exception {
        ModernBackendScope.current().checkActive();
        ModernPhysicalCombatSourceJdbc.requireHealingMagic(magicEnabled,playerHealingEnabled);
        if(sourceId<=0||objectId<=0||!validText(canonicalAccount,64,false))throw unavailable();
        ModernEquipmentSourceJdbc.sql(resourceDatabase);
        return read((connection,scope)->{
            var source=ModernPhysicalCombatSourceJdbc.read(connection,scope,sourceId,canonicalAccount,objectId,resourceDatabase,
                    magicEnabled,bowEnabled,regenEnabled,honorEnabled,equipmentHasteEnabled,playerHealingEnabled,retainedVip);
            if(source.isEmpty())return java.util.Optional.empty();
            return java.util.Optional.of(ModernArmorProtectionCatalog.bind(connection,scope,source.orElseThrow()));
        });
    }

    /** Operator-selected startup NPC catalog, never inferred from a player or global source. */
    ModernNpcCatalog.Catalog npcCatalog(int sourceId, String database, java.util.List<Integer> ids) throws Exception {
        ModernPhysicalCombat.identifier(database);
        if(sourceId<1||ids==null||ids.isEmpty()||ids.size()>ModernNpcCatalog.MAX_NPCS
                ||ids.stream().anyMatch(id->id==null||id<1)||new java.util.HashSet<>(ids).size()!=ids.size())
            throw new IllegalArgumentException("invalid NPC catalog selection");
        var selected=java.util.List.copyOf(ids);
        return read((connection,scope)->ModernNpcCatalogJdbc.read(connection,scope,sourceId,database,selected));
    }

    private <T> T read(Read<T> work) throws Exception {
        ModernBackendScope scope = ModernBackendScope.current();
        scope.checkActive();
        Properties properties = properties(scope);
        try {
            T value;
            try (Attempt attempt = new Attempt(scope)) {
                // Lease retirement precedes owned Connection.close. The raw socket lease
                // remains armed during driver cleanup, so a stalled QUIT cannot outlive
                // the same operation deadline. No connection is returned to a pool.
                try (Connection connection = connector.connect(jdbcUrl(), properties)) {
                    if (connection == null) throw unavailable();
                    try (ModernBackendScope.ConnectionLease lease = scope.register(connection)) {
                        scope.checkActive();
                        connection.setNetworkTimeout(Runnable::run, Math.min(networkMillis, scope.remainingMillis()));
                        connection.setReadOnly(true);
                        scope.checkActive();
                        value = work.run(connection, scope);
                        scope.checkActive();
                    }
                }
            }
            scope.checkActive();
            return value;
        } catch (Exception failure) {
            scope.checkActive(); // Preserve cancellation/deadline rather than reporting bad credentials.
            // Driver exceptions can contain URLs, user names or SQL values: no cause/suppressed chain escapes.
            throw unavailable();
        } finally {
            properties.clear();
        }
    }

    private PreparedStatement prepare(Connection connection, ModernBackendScope scope, String sql, int rows, String account)
            throws Exception {
        scope.checkActive();
        PreparedStatement statement = connection.prepareStatement(sql);
        try {
            statement.setMaxRows(rows);
            // JDBC's seconds granularity is only a secondary limit. The scope's
            // monotonic millisecond watchdog always closes the raw socket first.
            statement.setQueryTimeout(Math.max(1, (scope.remainingMillis() + 999) / 1000));
            statement.setString(1, account);
            scope.checkActive();
            return statement;
        } catch (Exception failure) {
            try { statement.close(); } catch (SQLException ignored) {}
            throw failure;
        }
    }

    private String jdbcUrl() {
        String host = config.host().contains(":") ? "[" + config.host() + "]" : config.host();
        // Strict DbConfig accepts numeric address + one conservative database token,
        // never a JDBC URL, user info, hostname, propertiesTransform or query fragment.
        return "jdbc:mysql://" + host + ":" + config.port() + "/" + config.database();
    }

    private Properties properties(ModernBackendScope scope) throws Exception {
        Properties properties = new Properties();
        char[] password = null;
        char[] trustPassword = null;
        boolean complete = false;
        try {
            password = config.password();
            properties.setProperty("user", config.username());
            properties.setProperty("password", new String(password));
            properties.setProperty("connectTimeout", Integer.toString(Math.min(connectMillis, scope.remainingMillis())));
            properties.setProperty("socketTimeout", Integer.toString(Math.min(networkMillis, scope.remainingMillis())));
            properties.setProperty("socketFactory", ScopedSocketFactory.class.getName());
            properties.setProperty("autoReconnect", "false");
            properties.setProperty("allowMultiQueries", "false");
            properties.setProperty("allowLoadLocalInfile", "false");
            properties.setProperty("allowUrlInLocalInfile", "false");
            properties.setProperty("allowPublicKeyRetrieval", "false");
            properties.setProperty("autoDeserialize", "false");
            properties.setProperty("useCompression", "false");
            properties.setProperty("characterEncoding", "UTF-8");
            properties.setProperty("useServerPrepStmts", "false");
            properties.setProperty("queryTimeoutKillsConnection", "true"); // No unscoped KILL QUERY connection.
            properties.setProperty("logger", "com.mysql.cj.log.NullLogger");
            properties.setProperty("profileSQL", "false");
            properties.setProperty("traceProtocol", "false");
            properties.setProperty("enablePacketDebug", "false");
            properties.setProperty("sslMode", config.sslMode());
            properties.setProperty("fallbackToSystemTrustStore", "false");
            properties.setProperty("fallbackToSystemKeyStore", "false");
            properties.setProperty("tlsVersions", "TLSv1.2,TLSv1.3");
            if ("VERIFY_IDENTITY".equals(config.sslMode())) {
                trustPassword = config.trustStorePassword();
                properties.setProperty("trustCertificateKeyStoreUrl", config.trustStorePath().toUri().toASCIIString());
                properties.setProperty("trustCertificateKeyStoreType", "PKCS12");
                properties.setProperty("trustCertificateKeyStorePassword", new String(trustPassword));
            }
            complete = true;
            return properties;
        } finally {
            if (password != null) Arrays.fill(password, '\0');
            if (trustPassword != null) Arrays.fill(trustPassword, '\0');
            if (!complete) properties.clear();
        }
    }

    /** Driver-instantiable, but only usable inside this DAO's current owned attempt. */
    public static final class ScopedSocketFactory extends StandardSocketFactory {
        public ScopedSocketFactory() {}
        @Override protected Socket createSocket(PropertySet properties) {
            Attempt attempt = ATTEMPT.get();
            if (attempt == null || attempt.scope != ModernBackendScope.current()) {
                throw new IllegalStateException("modern JDBC socket outside owned attempt");
            }
            return attempt.createSocket();
        }
        // Deliberately inherit Connector/J's TLS handshake and VERIFY_IDENTITY.
    }

    private static final class Attempt implements AutoCloseable {
        final ModernBackendScope scope;
        Socket socket;
        ModernBackendScope.SocketLease lease;
        Attempt(ModernBackendScope scope) {
            if (ATTEMPT.get() != null) throw new IllegalStateException("nested modern JDBC attempt");
            this.scope = scope;
            ATTEMPT.set(this);
        }
        Socket createSocket() {
            if (socket != null) throw new IllegalStateException("modern JDBC cannot reconnect or fail over");
            Socket created = new Socket();
            try {
                // Register before StandardSocketFactory calls Socket.connect.
                lease = scope.registerSocket(created);
                socket = created;
                return created;
            } catch (Exception failure) {
                try { created.close(); } catch (IOException ignored) {}
                throw new IllegalStateException("modern JDBC socket unavailable");
            }
        }
        @Override public void close() throws IOException {
            try {
                if (socket != null) socket.close();
            } finally {
                if (lease != null) lease.close();
                ATTEMPT.remove();
            }
        }
    }

    private static void validateTrustStore(ModernServerConfig.DbConfig config) throws SQLException {
        char[] password = null;
        byte[] bytes = null;
        try {
            Path path = Objects.requireNonNull(config.trustStorePath());
            password = Objects.requireNonNull(config.trustStorePassword());
            // Local path shape/ancestors were validated by strict config. Reject
            // symlink leaves again and never load an unbounded key store blob.
            try (var input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
                bytes = input.readNBytes(MAX_TRUST_BYTES + 1);
            }
            if (bytes.length == 0 || bytes.length > MAX_TRUST_BYTES) throw unavailable();
            KeyStore store = KeyStore.getInstance("PKCS12");
            store.load(new ByteArrayInputStream(bytes), password);
            if (store.size() == 0 || store.size() > 64) throw unavailable();
            for (var aliases = store.aliases(); aliases.hasMoreElements();) {
                String alias = aliases.nextElement();
                if (!store.isCertificateEntry(alias) || !(store.getCertificate(alias) instanceof X509Certificate certificate)
                        || certificate.getBasicConstraints() < 0) throw unavailable();
                certificate.checkValidity();
            }
        } catch (Exception failure) {
            throw unavailable();
        } finally {
            if (password != null) Arrays.fill(password, '\0');
            if (bytes != null) Arrays.fill(bytes, (byte) 0);
        }
    }

    private static long requiredLong(ResultSet result, String field) throws SQLException {
        return ModernJdbcNumbers.required(result, field);
    }
    private static boolean validText(String text, int maximumBytes, boolean password) {
        return text != null && !text.isEmpty() && text.length() <= maximumBytes
                && (password || text.codePoints().anyMatch(point -> !Character.isWhitespace(point) && !Character.isSpaceChar(point)))
                && text.chars().noneMatch(character -> Character.isISOControl(character))
                && StandardCharsets.UTF_8.newEncoder().canEncode(text)
                && text.getBytes(StandardCharsets.UTF_8).length <= maximumBytes;
    }
    private static SQLException unavailable() { return new SQLException("modern JDBC backend unavailable"); }
    @Override public String toString() { return "ModernJdbcBackend[redacted]"; }
}

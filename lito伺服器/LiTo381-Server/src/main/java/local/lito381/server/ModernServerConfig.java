package local.lito381.server;

import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Standalone modern-listener configuration, never a native/legacy-config fallback.
 * UTF-8 Java properties syntax is used; Windows paths should use forward slashes
 * (or escaped backslashes). Duplicate decoded keys and unknown keys are errors.
 * Loading validates configuration only: no listener, database or key-store is opened.
 * All owned credential arrays are erased on close, not the immutable process environment
 * or defensive copies already handed to a caller. Neither secrets nor values appear in errors.
 */
final class ModernServerConfig implements AutoCloseable {
    static final int MAX_CONFIG_BYTES = 32 * 1024;
    private static final int MAX_PROPERTY_CHARS = 1024;
    private static final Set<String> ROOT_KEYS = Set.of(
            "schema", "bind.host", "bind.port", "tls.pkcs12", "tls.password.env", "sources", "equipment.storeDirectory", "shop.allowance", "matchmaking.enabled",
            "limits.maxSessions", "limits.maxPerIp", "limits.backendWorkers", "limits.backendQueue",
            "limits.backendDeadlineMillis", "limits.handshakeMillis", "limits.frameMillis",
            "limits.applicationReadMillis", "limits.dbConnectMillis", "limits.dbNetworkMillis",
            "limits.acceptPerMinute", "limits.acceptBurst");
    private static final List<String> SOURCE_SUFFIXES = List.of(
            "name", "mysql.host", "mysql.port", "mysql.database", "mysql.username.env",
            "mysql.password.env", "mysql.passwordScheme", "mysql.tls", "mysql.trustStore.pkcs12",
            "mysql.trustStore.password.env", "catalog.database", "weight.rate", "npc.blessingConvention", "administration.accounts", "fullVipRules");
    private static final Set<String> SETTLEMENT_KEYS = Set.of(
            "settlement.enabled", "settlement.outboxDirectory", "settlement.mysql.host", "settlement.mysql.port",
            "settlement.mysql.database", "settlement.mysql.username.env", "settlement.mysql.password.env",
            "settlement.mysql.tls", "settlement.mysql.trustStore.pkcs12", "settlement.mysql.trustStore.password.env");
    private static final Set<String> RACE_SETTLEMENT_KEYS = SETTLEMENT_KEYS.stream()
            .map(key->"raceSettlement."+key.substring("settlement.".length())).collect(java.util.stream.Collectors.toUnmodifiableSet());
    private static final Set<String> WORLD_POLICY_KEYS = Set.of("worldPolicy.enabled", "worldPolicy.namespaceSha256", "worldPolicy.revision",
            "worldPolicy.contentSha256", "worldPolicy.deadlineMillis", "worldPolicy.mysql.host", "worldPolicy.mysql.port", "worldPolicy.mysql.database",
            "worldPolicy.mysql.username.env", "worldPolicy.mysql.password.env", "worldPolicy.mysql.tls",
            "worldPolicy.mysql.trustStore.pkcs12", "worldPolicy.mysql.trustStore.password.env");

    enum Failure { INVALID_FILE, FILE_TOO_LARGE, INVALID_UTF8, INVALID_PROPERTIES,
        DUPLICATE_PROPERTY, UNKNOWN_PROPERTY, INVALID_VALUE, INVALID_PATH, INVALID_ENVIRONMENT }

    static final class ConfigException extends Exception {
        private final Failure failure;
        ConfigException(Failure failure) {
            super("invalid modern server configuration: " + failure);
            this.failure = failure;
        }
        Failure failure() { return failure; }
    }

    /** No auto-generated record toString may expose credentials, env names or paths. */
    static final class TlsConfig implements AutoCloseable {
        private final Path keyStore;
        private final char[] password;
        private boolean closed;

        private TlsConfig(Path keyStore, char[] ownedPassword) {
            this.keyStore = keyStore;
            this.password = ownedPassword;
        }
        Path keyStore() { return keyStore; }
        synchronized char[] password() { ensureOpen(closed); return password.clone(); }
        @Override public synchronized void close() { Arrays.fill(password, '\0'); closed = true; }
        @Override public String toString() { return "ModernTlsConfig[redacted]"; }
    }

    /** Remote MySQL requires verified TLS; the password representation is explicitly the proven legacy plain scheme. */
    static final class DbConfig implements AutoCloseable {
        private final String host;
        private final int port;
        private final String database;
        private final String sslMode;
        private final Path trustStorePath;
        private final char[] username;
        private final char[] password;
        private final char[] trustStorePassword;
        private boolean closed;

        private DbConfig(String host, int port, String database, String sslMode, Path trustStorePath,
                char[] ownedUsername, char[] ownedPassword, char[] ownedTrustStorePassword) {
            this.host = host;
            this.port = port;
            this.database = database;
            this.sslMode = sslMode;
            this.trustStorePath = trustStorePath;
            this.username = ownedUsername;
            this.password = ownedPassword;
            this.trustStorePassword = ownedTrustStorePassword;
        }
        String host() { return host; }
        int port() { return port; }
        String database() { return database; }
        String passwordScheme() { return "plain"; }
        String sslMode() { return sslMode; }
        Path trustStorePath() { return trustStorePath; }
        synchronized String username() { ensureOpen(closed); return new String(username); }
        synchronized char[] password() { ensureOpen(closed); return password.clone(); }
        synchronized char[] trustStorePassword() {
            ensureOpen(closed);
            return trustStorePassword == null ? null : trustStorePassword.clone();
        }
        @Override public synchronized void close() {
            Arrays.fill(username, '\0');
            Arrays.fill(password, '\0');
            if (trustStorePassword != null) Arrays.fill(trustStorePassword, '\0');
            closed = true;
        }
        @Override public String toString() { return "ModernDbConfig[redacted]"; }
    }

    static final class SourceConfig {
        private final int id;
        private final String name;
        private final DbConfig database;
        private final String catalogDatabase;
        private final Double weightRate;
        private final ModernNpcMelee.BlessingConvention npcBlessing;
        private final Set<String> administrationAccounts;
        private final boolean fullVip;
        private SourceConfig(int id, String name, DbConfig database, String catalogDatabase, Double weightRate, ModernNpcMelee.BlessingConvention npcBlessing, Set<String> administrationAccounts, boolean fullVip) {
            this.id = id;
            this.name = name;
            this.database = database;
            this.catalogDatabase = catalogDatabase;
            this.weightRate = weightRate;
            this.npcBlessing=npcBlessing;
            this.administrationAccounts=Set.copyOf(administrationAccounts);
            this.fullVip=fullVip;
        }
        int id() { return id; }
        String name() { return name; }
        DbConfig database() { return database; }
        /** Null means this auth/roster-only source has no catalog feature, never primary fallback. */
        String catalogDatabase() { return catalogDatabase; }
        /** Null means the weight rules are disabled; never infer a native default. */
        Double weightRate() { return weightRate; }
        ModernNpcMelee.BlessingConvention npcBlessing(){return npcBlessing;}
        Set<String> administrationAccounts(){return administrationAccounts;}
        boolean fullVip(){return fullVip;}
        @Override public String toString() { return "ModernSourceConfig[redacted]"; }
    }

    /** Explicit dedicated writer settings; never an alias of a source DAO's credentials. */
    static final class SettlementConfig implements AutoCloseable {
        private final DbConfig database;
        private final Path outboxDirectory;
        private SettlementConfig(DbConfig database, Path outboxDirectory) {
            this.database = database; this.outboxDirectory = outboxDirectory;
        }
        DbConfig database() { return database; }
        Path outboxDirectory() { return outboxDirectory; }
        @Override public void close() { database.close(); }
        @Override public String toString() { return "ModernSettlementConfig[redacted]"; }
    }
    /** Dedicated SELECT-only startup credentials; entirely independent of settlement enablement. */
    static final class WorldPolicyConfig implements AutoCloseable {
        private final DbConfig database;
        private final ModernWorldPolicy.Pin pin;
        private final int deadlineMillis;
        private WorldPolicyConfig(DbConfig database, ModernWorldPolicy.Pin pin, int deadlineMillis) {
            this.database=database;this.pin=pin;this.deadlineMillis=deadlineMillis;
        }
        DbConfig database(){return database;}
        ModernWorldPolicy.Pin pin(){return pin;}
        int deadlineMillis(){return deadlineMillis;}
        @Override public void close(){database.close();}
        @Override public String toString(){return "ModernWorldPolicyConfig[redacted]";}
    }

    private final InetSocketAddress bind;
    private final TlsConfig tls;
    private final List<SourceConfig> sources;
    private final Limits limits;
    private final Path equipmentStoreDirectory;
    private final ModernWorldConfiguration worlds;
    private final SettlementConfig settlement;
    private final SettlementConfig raceSettlement;
    private final WorldPolicyConfig worldPolicy;
    private final long shopAllowance;
    private final boolean matchmakingEnabled;

    private ModernServerConfig(InetSocketAddress bind, TlsConfig tls, List<SourceConfig> sources, Limits limits, Path equipmentStoreDirectory, ModernWorldConfiguration worlds, SettlementConfig settlement,long shopAllowance,WorldPolicyConfig worldPolicy,SettlementConfig raceSettlement,boolean matchmakingEnabled) {
        this.bind = bind;
        this.tls = tls;
        this.sources = List.copyOf(sources);
        this.limits = limits;
        this.equipmentStoreDirectory = equipmentStoreDirectory;
        this.worlds = worlds;
        this.settlement = settlement;
        this.raceSettlement=raceSettlement;
        this.worldPolicy=worldPolicy;
        this.shopAllowance=shopAllowance;
        this.matchmakingEnabled=matchmakingEnabled;
    }

    InetSocketAddress bind() { return bind; }
    TlsConfig tls() { return tls; }
    List<SourceConfig> sources() { return sources; }
    /** Null explicitly preserves read-only DATA mode. Never create a default directory. */
    Path equipmentStoreDirectory() { return equipmentStoreDirectory; }
    ModernWorldConfiguration worlds() { return worlds; }
    /** Null keeps the optional writer disabled. No implicit credential/schema/path fallback. */
    SettlementConfig settlement() { return settlement; }
    /** Separate ranked-result journal/database, never the two-team WIN/LOSS writer. */
    SettlementConfig raceSettlement(){return raceSettlement;}
    /** Null preserves the exact local-only startup path; no SQL fallback. */
    WorldPolicyConfig worldPolicy(){return worldPolicy;}
    /** Zero means disabled; no implicit or source-Adena allowance. */
    long shopAllowance(){return shopAllowance;}
    /** Explicit v34 opt-in, only with qualified world/equipment and central standings. */
    boolean matchmakingEnabled(){return matchmakingEnabled;}
    int maxSessions() { return limits.maxSessions; }
    int maxPerIp() { return limits.maxPerIp; }
    int backendWorkers() { return limits.backendWorkers; }
    int backendQueue() { return limits.backendQueue; }
    int backendDeadlineMillis() { return limits.backendDeadlineMillis; }
    int handshakeMillis() { return limits.handshakeMillis; }
    int frameMillis() { return limits.frameMillis; }
    int applicationReadMillis() { return limits.applicationReadMillis; }
    int dbConnectMillis() { return limits.dbConnectMillis; }
    int dbNetworkMillis() { return limits.dbNetworkMillis; }
    int acceptPerMinute() { return limits.acceptPerMinute; }
    int acceptBurst() { return limits.acceptBurst; }

    @Override public void close() {
        tls.close();
        for (SourceConfig source : sources) source.database.close();
        if (settlement != null) settlement.close();
        if (raceSettlement != null) raceSettlement.close();
        if (worldPolicy != null) worldPolicy.close();
    }
    @Override public String toString() { return "ModernServerConfig[redacted]"; }

    /**
     * Rejects aliases/reparse-like non-directories in the path before opening; the
     * final component is also opened NOFOLLOW_LINKS and checked again after reading.
     * This is not a claim that Java's path APIs can lock all ancestor directories
     * against a privileged concurrent rename. Protect the config directory with OS ACLs.
     */
    static ModernServerConfig load(Path path, Function<String, String> environment) throws ConfigException {
        Path checked = checkedLocalPath(path, true);
        byte[] storage = new byte[MAX_CONFIG_BYTES + 1];
        try {
            BasicFileAttributes before = Files.readAttributes(checked, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (before.size() > MAX_CONFIG_BYTES) throw failure(Failure.FILE_TOO_LARGE);
            int size;
            try (SeekableByteChannel channel = Files.newByteChannel(checked,
                    Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS))) {
                ByteBuffer buffer = ByteBuffer.wrap(storage);
                while (buffer.hasRemaining()) {
                    int read = channel.read(buffer);
                    if (read < 0) break;
                    if (read == 0) throw failure(Failure.INVALID_FILE);
                }
                size = buffer.position();
                if (size > MAX_CONFIG_BYTES) throw failure(Failure.FILE_TOO_LARGE);
                checkedLocalPath(checked, true);
                BasicFileAttributes after = Files.readAttributes(checked, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                if (!Objects.equals(before.fileKey(), after.fileKey()) || before.size() != after.size()
                        || !before.lastModifiedTime().equals(after.lastModifiedTime()) || size != after.size()) {
                    throw failure(Failure.INVALID_FILE);
                }
            }
            return parseBytes(ByteBuffer.wrap(storage, 0, size), environment);
        } catch (ConfigException error) {
            throw error;
        } catch (IOException | RuntimeException ignored) {
            throw failure(Failure.INVALID_FILE);
        } finally {
            Arrays.fill(storage, (byte) 0);
        }
    }

    static ModernServerConfig parse(byte[] bytes, Function<String, String> environment) throws ConfigException {
        if (bytes == null) throw failure(Failure.INVALID_PROPERTIES);
        if (bytes.length > MAX_CONFIG_BYTES) throw failure(Failure.FILE_TOO_LARGE);
        return parseBytes(ByteBuffer.wrap(bytes), environment);
    }

    private static ModernServerConfig parseBytes(ByteBuffer bytes, Function<String, String> environment)
            throws ConfigException {
        if (environment == null) throw failure(Failure.INVALID_ENVIRONMENT);
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(bytes).toString();
        } catch (CharacterCodingException ignored) {
            throw failure(Failure.INVALID_UTF8);
        }
        // A UTF-8 BOM is deliberately not silently interpreted as part of the first key.
        if (text.isEmpty() || text.charAt(0) == '\ufeff') throw failure(Failure.INVALID_PROPERTIES);
        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            if (Character.isISOControl(value) && value != '\n' && value != '\r' && value != '\t') {
                throw failure(Failure.INVALID_PROPERTIES);
            }
        }
        StrictProperties properties = new StrictProperties();
        try {
            properties.load(new StringReader(text));
        } catch (PropertyFailure error) {
            throw failure(error.failure);
        } catch (IOException | IllegalArgumentException ignored) {
            throw failure(Failure.INVALID_PROPERTIES);
        }
        if (!required(properties, "schema").equals("1")) throw failure(Failure.INVALID_VALUE);
        List<Integer> sourceIds = sourceIds(required(properties, "sources"));
        Set<String> allowed = new HashSet<>(ROOT_KEYS);
        allowed.addAll(ModernWorldConfiguration.allowedKeys());
        allowed.addAll(SETTLEMENT_KEYS);
        allowed.addAll(RACE_SETTLEMENT_KEYS);
        allowed.addAll(WORLD_POLICY_KEYS);
        for (int id : sourceIds) for (String suffix : SOURCE_SUFFIXES) allowed.add("source." + id + "." + suffix);
        for (String key : properties.stringPropertyNames()) {
            if (!allowed.contains(key)) throw failure(Failure.UNKNOWN_PROPERTY);
        }

        // Complete all static validation before consulting the environment even once.
        ModernWorldConfiguration worlds = ModernWorldConfiguration.parse(properties);
        String matchingValue=properties.getProperty("matchmaking.enabled","false");
        if(!matchingValue.equals("true")&&!matchingValue.equals("false"))throw failure(Failure.INVALID_VALUE);
        boolean matchmakingEnabled=matchingValue.equals("true");
        long shopAllowance=0;
        if(properties.containsKey("shop.allowance")) {
            String value=required(properties,"shop.allowance");
            if(!value.matches("[1-9][0-9]{0,18}"))throw failure(Failure.INVALID_VALUE);
            try{shopAllowance=Long.parseLong(value);}catch(NumberFormatException invalid){throw failure(Failure.INVALID_VALUE);}
        }
        if((shopAllowance>0)!=worlds.itemsEnabled())throw failure(Failure.INVALID_VALUE);
        InetAddress bindAddress = numericAddress(properties.getProperty("bind.host", "127.0.0.1"));
        if (bindAddress.isMulticastAddress() || bindAddress.isLinkLocalAddress() || broadcast(bindAddress)) throw failure(Failure.INVALID_VALUE);
        int port = number(required(properties, "bind.port"), 0, 65535);
        Path keyStore;
        try { keyStore = checkedLocalPath(Path.of(required(properties, "tls.pkcs12")), false); }
        catch (RuntimeException ignored) { throw failure(Failure.INVALID_PATH); }
        String tlsEnv = envName(required(properties, "tls.password.env"));
        Limits limits = limits(properties);
        Path equipmentDirectory = null;
        if (properties.containsKey("equipment.storeDirectory")) {
            try {
                equipmentDirectory = Path.of(required(properties, "equipment.storeDirectory"));
                if (!equipmentDirectory.isAbsolute() || equipmentDirectory.getFileName() == null) throw failure(Failure.INVALID_PATH);
                // Read-only validation: all ancestors including the configured
                // directory must exist and be ordinary local directories. No
                // lock, data file or directory is created while parsing config.
                checkedLocalPath(equipmentDirectory.resolve(".modern-equipment.lock"), false);
            } catch (RuntimeException ignored) { throw failure(Failure.INVALID_PATH); }
        }
        List<SourceInput> sourceInputs = new ArrayList<>();
        Set<String> sourceNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (int id : sourceIds) {
            String prefix = "source." + id + ".";
            Set<String> administrationAccounts;
            try { administrationAccounts=ModernSpeechAuthority.parseAdministrators(properties.getProperty(prefix+"administration.accounts")); }
            catch (IllegalArgumentException invalid) { throw failure(Failure.INVALID_VALUE); }
            String name = required(properties, prefix + "name");
            if (!validText(name, 64, 256) || !name.equals(name.strip())
                    || !sourceNames.add(name)) throw failure(Failure.INVALID_VALUE);
            InetAddress host = numericAddress(required(properties, prefix + "mysql.host"));
            if (host.isAnyLocalAddress() || host.isMulticastAddress() || broadcast(host)
                    || host.isLinkLocalAddress()) throw failure(Failure.INVALID_VALUE);
            int mysqlPort = number(required(properties, prefix + "mysql.port"), 1, 65535);
            String database = required(properties, prefix + "mysql.database");
            if (!database.matches("[A-Za-z0-9_]{1,64}")) throw failure(Failure.INVALID_VALUE);
            String catalogDatabase = properties.getProperty(prefix + "catalog.database");
            if (catalogDatabase != null && !catalogDatabase.matches("[A-Za-z0-9_]{1,64}")) throw failure(Failure.INVALID_VALUE);
            boolean fullVip=properties.containsKey(prefix+"fullVipRules");
            if(fullVip&&(!ModernWorldConfiguration.FULL_VIP_RULESET.equals(required(properties,prefix+"fullVipRules"))
                    ||!worlds.fullVipEnabled()||catalogDatabase==null))throw failure(Failure.INVALID_VALUE);
            String weightValue = properties.getProperty(prefix + "weight.rate");
            if ((weightValue != null) != worlds.weightEnabled()) throw failure(Failure.INVALID_VALUE);
            Double weightRate = weightValue == null ? null : weightRate(weightValue);
            String blessingValue=properties.getProperty(prefix+"npc.blessingConvention");
            if((blessingValue!=null)!=worlds.npcEnabled())throw failure(Failure.INVALID_VALUE);
            ModernNpcMelee.BlessingConvention npcBlessing=null;
            if(blessingValue!=null)try{npcBlessing=ModernNpcMelee.BlessingConvention.valueOf(blessingValue);}
                catch(IllegalArgumentException invalid){throw failure(Failure.INVALID_VALUE);}
            String usernameEnv = envName(required(properties, prefix + "mysql.username.env"));
            String passwordEnv = envName(required(properties, prefix + "mysql.password.env"));
            if (!required(properties, prefix + "mysql.passwordScheme").equals("plain")) throw failure(Failure.INVALID_VALUE);
            String tlsMode = required(properties, prefix + "mysql.tls");
            Path trustStore = null;
            String trustStoreEnv = null;
            String sslMode;
            if (tlsMode.equals("disabled")) {
                if (!host.isLoopbackAddress() || properties.containsKey(prefix + "mysql.trustStore.pkcs12")
                        || properties.containsKey(prefix + "mysql.trustStore.password.env")) throw failure(Failure.INVALID_VALUE);
                sslMode = "DISABLED";
            } else if (tlsMode.equals("verify_identity")) {
                try { trustStore = checkedLocalPath(Path.of(required(properties, prefix + "mysql.trustStore.pkcs12")), false); }
                catch (RuntimeException ignored) { throw failure(Failure.INVALID_PATH); }
                trustStoreEnv = envName(required(properties, prefix + "mysql.trustStore.password.env"));
                sslMode = "VERIFY_IDENTITY";
            } else throw failure(Failure.INVALID_VALUE);
            sourceInputs.add(new SourceInput(id, name, host.getHostAddress(), mysqlPort, database,
                    usernameEnv, passwordEnv, sslMode, trustStore, trustStoreEnv, catalogDatabase, weightRate,npcBlessing,administrationAccounts,fullVip));
        }
        if (equipmentDirectory != null && sourceInputs.stream().anyMatch(source -> source.catalogDatabase == null)) {
            throw failure(Failure.INVALID_VALUE);
        }
        if (worlds.combatEnabled() && equipmentDirectory == null) throw failure(Failure.INVALID_VALUE);
        if(worlds.fullVipEnabled()!=sourceInputs.stream().anyMatch(SourceInput::fullVip))throw failure(Failure.INVALID_VALUE);
        if(worlds.npcSpecs().values().stream().anyMatch(spec->!sourceIds.contains(spec.catalogSource())))throw failure(Failure.INVALID_VALUE);
        SettlementInput settlementInput = settlementInput(properties, sourceInputs);
        if(matchmakingEnabled && (!worlds.enabled() || !worlds.combatEnabled() || equipmentDirectory==null || settlementInput==null
                || sourceInputs.stream().anyMatch(source->source.catalogDatabase==null)))throw failure(Failure.INVALID_VALUE);
        if((worlds.questEnabled()||worlds.timeAttackEnabled()||worlds.siegeEnabled())&&settlementInput==null)throw failure(Failure.INVALID_VALUE);
        SettlementInput raceInput=settlementInput(properties,sourceInputs,"raceSettlement",RACE_SETTLEMENT_KEYS);
        if((raceInput!=null)!=worlds.raceEnabled())throw failure(Failure.INVALID_VALUE);
        if(raceInput!=null&&settlementInput!=null&&raceInput.directory.equals(settlementInput.directory))throw failure(Failure.INVALID_PATH);
        WorldPolicyInput policyInput = worldPolicyInput(properties, sourceInputs, worlds);

        TlsConfig tls = null;
        SettlementConfig settlement = null;
        SettlementConfig raceSettlement=null;
        WorldPolicyConfig worldPolicy = null;
        List<SourceConfig> sources = new ArrayList<>();
        boolean complete = false;
        try {
            tls = new TlsConfig(keyStore, secret(environment, tlsEnv, 1024, 4096, false));
            for (SourceInput input : sourceInputs) {
                char[] username = secret(environment, input.usernameEnv, 128, 512, true);
                char[] password = null;
                char[] trustStorePassword = null;
                boolean transferred = false;
                try {
                    password = secret(environment, input.passwordEnv, 1024, 4096, false);
                    if (input.trustStoreEnv != null) {
                        trustStorePassword = secret(environment, input.trustStoreEnv, 1024, 4096, false);
                    }
                    DbConfig db = new DbConfig(input.host, input.port, input.database, input.sslMode,
                            input.trustStore, username, password, trustStorePassword);
                    sources.add(new SourceConfig(input.id, input.name, db, input.catalogDatabase, input.weightRate,input.npcBlessing,input.administrationAccounts,input.fullVip));
                    transferred = true;
                } finally {
                    if (!transferred) {
                        Arrays.fill(username, '\0');
                        if (password != null) Arrays.fill(password, '\0');
                        if (trustStorePassword != null) Arrays.fill(trustStorePassword, '\0');
                    }
                }
            }
            if (settlementInput != null) {
                char[] username = secret(environment, settlementInput.usernameEnv, 128, 512, true);
                char[] password = null, trustPassword = null;
                boolean transferred = false;
                try {
                    password = secret(environment, settlementInput.passwordEnv, 1024, 4096, false);
                    if (settlementInput.trustStoreEnv != null)
                        trustPassword = secret(environment, settlementInput.trustStoreEnv, 1024, 4096, false);
                    settlement = new SettlementConfig(new DbConfig(settlementInput.host, settlementInput.port,
                            settlementInput.database, settlementInput.sslMode, settlementInput.trustStore,
                            username, password, trustPassword), settlementInput.directory);
                    transferred = true;
                } finally {
                    if (!transferred) {
                        Arrays.fill(username, '\0');
                        if (password != null) Arrays.fill(password, '\0');
                        if (trustPassword != null) Arrays.fill(trustPassword, '\0');
                    }
                }
            }
            if(policyInput!=null) {
                char[] username=secret(environment,policyInput.usernameEnv,128,512,true);
                char[] password=null,trustPassword=null;boolean transferred=false;
                try {
                    password=secret(environment,policyInput.passwordEnv,1024,4096,false);
                    if(policyInput.trustStoreEnv!=null)trustPassword=secret(environment,policyInput.trustStoreEnv,1024,4096,false);
                    worldPolicy=new WorldPolicyConfig(new DbConfig(policyInput.host,policyInput.port,policyInput.database,
                            policyInput.sslMode,policyInput.trustStore,username,password,trustPassword),policyInput.pin,policyInput.deadlineMillis);
                    transferred=true;
                } finally {
                    if(!transferred){Arrays.fill(username,'\0');if(password!=null)Arrays.fill(password,'\0');if(trustPassword!=null)Arrays.fill(trustPassword,'\0');}
                }
            }
            if(raceInput!=null)raceSettlement=ownedSettlement(raceInput,environment);
            ModernServerConfig result = new ModernServerConfig(new InetSocketAddress(bindAddress, port), tls, sources, limits, equipmentDirectory, worlds, settlement,shopAllowance,worldPolicy,raceSettlement,matchmakingEnabled);
            complete = true;
            return result;
        } finally {
            if (!complete) {
                if (tls != null) tls.close();
                for (SourceConfig source : sources) source.database.close();
                if (settlement != null) settlement.close();
                if (raceSettlement != null) raceSettlement.close();
                if (worldPolicy != null) worldPolicy.close();
            }
        }
    }

    private record SourceInput(int id, String name, String host, int port, String database,
            String usernameEnv, String passwordEnv, String sslMode, Path trustStore, String trustStoreEnv, String catalogDatabase, Double weightRate,ModernNpcMelee.BlessingConvention npcBlessing,Set<String> administrationAccounts,boolean fullVip) {
        @Override public String toString() { return "ModernSourceInput[redacted]"; }
    }

    private static double weightRate(String value) throws ConfigException {
        if (value == null || !value.matches("(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?"))
            throw failure(Failure.INVALID_VALUE);
        final double rate;
        try { rate = Double.parseDouble(value); }
        catch (NumberFormatException invalid) { throw failure(Failure.INVALID_VALUE); }
        if (!Double.isFinite(rate) || rate <= 0) throw failure(Failure.INVALID_VALUE);
        return rate;
    }

    private record SettlementInput(Path directory, String host, int port, String database,
            String usernameEnv, String passwordEnv, String sslMode, Path trustStore, String trustStoreEnv) {
        @Override public String toString() { return "ModernSettlementInput[redacted]"; }
    }
    private record WorldPolicyInput(String host,int port,String database,String usernameEnv,String passwordEnv,
            String sslMode,Path trustStore,String trustStoreEnv,ModernWorldPolicy.Pin pin,int deadlineMillis) {
        @Override public String toString(){return "ModernWorldPolicyInput[redacted]";}
    }
    private static WorldPolicyInput worldPolicyInput(Properties properties,List<SourceInput> sources,ModernWorldConfiguration worlds)throws ConfigException {
        String enabled=properties.getProperty("worldPolicy.enabled","false");
        if(!enabled.equals("true")&&!enabled.equals("false"))throw failure(Failure.INVALID_VALUE);
        if(!enabled.equals("true")) {
            for(String key:WORLD_POLICY_KEYS)if(!key.equals("worldPolicy.enabled")&&properties.containsKey(key))throw failure(Failure.INVALID_VALUE);
            return null;
        }
        if(!worlds.completelyPinned())throw failure(Failure.INVALID_VALUE);
        ModernWorldPolicy.Pin pin;
        try {
            String revision=required(properties,"worldPolicy.revision");
            if(!revision.matches("[1-9][0-9]{0,18}"))throw failure(Failure.INVALID_VALUE);
            pin=new ModernWorldPolicy.Pin(required(properties,"worldPolicy.namespaceSha256"),Long.parseLong(revision),required(properties,"worldPolicy.contentSha256"));
        }catch(IllegalArgumentException invalid){throw failure(Failure.INVALID_VALUE);}
        int deadline=number(properties.getProperty("worldPolicy.deadlineMillis","30000"),100,30000);
        String prefix="worldPolicy.mysql.";
        InetAddress host=numericAddress(required(properties,prefix+"host"));
        if(host.isAnyLocalAddress()||host.isMulticastAddress()||host.isLinkLocalAddress()||broadcast(host))throw failure(Failure.INVALID_VALUE);
        int port=number(required(properties,prefix+"port"),1,65535);
        String database=required(properties,prefix+"database");
        if(!database.matches("[A-Za-z0-9_]{1,64}"))throw failure(Failure.INVALID_VALUE);
        for(SourceInput source:sources)if(database.equalsIgnoreCase(source.database)||database.equalsIgnoreCase(source.catalogDatabase))throw failure(Failure.INVALID_VALUE);
        String user=envName(required(properties,prefix+"username.env")),password=envName(required(properties,prefix+"password.env"));
        String mode=required(properties,prefix+"tls"),ssl;Path trust=null;String trustEnv=null;
        if(mode.equals("disabled")) {
            if(!host.isLoopbackAddress()||properties.containsKey(prefix+"trustStore.pkcs12")||properties.containsKey(prefix+"trustStore.password.env"))throw failure(Failure.INVALID_VALUE);
            ssl="DISABLED";
        }else if(mode.equals("verify_identity")) {
            try{trust=checkedLocalPath(Path.of(required(properties,prefix+"trustStore.pkcs12")),false);}catch(RuntimeException invalid){throw failure(Failure.INVALID_PATH);}
            trustEnv=envName(required(properties,prefix+"trustStore.password.env"));ssl="VERIFY_IDENTITY";
        }else throw failure(Failure.INVALID_VALUE);
        return new WorldPolicyInput(host.getHostAddress(),port,database,user,password,ssl,trust,trustEnv,pin,deadline);
    }

    private static SettlementInput settlementInput(Properties properties, List<SourceInput> sources) throws ConfigException {
        return settlementInput(properties,sources,"settlement",SETTLEMENT_KEYS);
    }
    private static SettlementInput settlementInput(Properties properties,List<SourceInput> sources,String namespace,Set<String> keys)throws ConfigException {
        String enabled = properties.getProperty(namespace+".enabled", "false");
        if (!enabled.equals("true") && !enabled.equals("false")) throw failure(Failure.INVALID_VALUE);
        if (!enabled.equals("true")) {
            for (String key : keys) if (!key.equals(namespace+".enabled") && properties.containsKey(key))
                throw failure(Failure.INVALID_VALUE);
            return null;
        }
        Path directory;
        try {
            directory = Path.of(required(properties, namespace+".outboxDirectory"));
            if (!directory.isAbsolute() || directory.getFileName() == null) throw failure(Failure.INVALID_PATH);
            // Only static inspection here; the outbox separately owns/pins its lock and durable files.
            checkedLocalPath(directory.resolve(namespace.equals("settlement")?".modern-match-outbox.lock":".modern-pet-race-outbox.lock"), false);
            checkedLocalPath(directory.resolve(namespace.equals("settlement")?"outbox.mso":"race-outbox.rpo"), false);
        } catch (RuntimeException ignored) { throw failure(Failure.INVALID_PATH); }
        String prefix = namespace+".mysql.";
        InetAddress host = numericAddress(required(properties, prefix + "host"));
        if (host.isAnyLocalAddress() || host.isMulticastAddress() || host.isLinkLocalAddress() || broadcast(host))
            throw failure(Failure.INVALID_VALUE);
        int port = number(required(properties, prefix + "port"), 1, 65535);
        String database = required(properties, prefix + "database");
        if (!database.matches("[A-Za-z0-9_]{1,64}")) throw failure(Failure.INVALID_VALUE);
        for (SourceInput source : sources) if (database.equalsIgnoreCase(source.database)
                || database.equalsIgnoreCase(source.catalogDatabase)) throw failure(Failure.INVALID_VALUE);
        String usernameEnv = envName(required(properties, prefix + "username.env"));
        String passwordEnv = envName(required(properties, prefix + "password.env"));
        String mode = required(properties, prefix + "tls");
        Path trust = null; String trustEnv = null, sslMode;
        if (mode.equals("disabled")) {
            if (!host.isLoopbackAddress() || properties.containsKey(prefix + "trustStore.pkcs12")
                    || properties.containsKey(prefix + "trustStore.password.env")) throw failure(Failure.INVALID_VALUE);
            sslMode = "DISABLED";
        } else if (mode.equals("verify_identity")) {
            try { trust = checkedLocalPath(Path.of(required(properties, prefix + "trustStore.pkcs12")), false); }
            catch (RuntimeException ignored) { throw failure(Failure.INVALID_PATH); }
            trustEnv = envName(required(properties, prefix + "trustStore.password.env"));
            sslMode = "VERIFY_IDENTITY";
        } else throw failure(Failure.INVALID_VALUE);
        return new SettlementInput(directory, host.getHostAddress(), port, database, usernameEnv, passwordEnv, sslMode, trust, trustEnv);
    }
    private static SettlementConfig ownedSettlement(SettlementInput input,Function<String,String> environment)throws ConfigException {
        char[] username=secret(environment,input.usernameEnv,128,512,true),password=null,trust=null;boolean complete=false;
        try {
            password=secret(environment,input.passwordEnv,1024,4096,false);
            if(input.trustStoreEnv!=null)trust=secret(environment,input.trustStoreEnv,1024,4096,false);
            var result=new SettlementConfig(new DbConfig(input.host,input.port,input.database,input.sslMode,input.trustStore,username,password,trust),input.directory);
            complete=true;return result;
        }finally{if(!complete){Arrays.fill(username,'\0');if(password!=null)Arrays.fill(password,'\0');if(trust!=null)Arrays.fill(trust,'\0');}}
    }

    private record Limits(int maxSessions, int maxPerIp, int backendWorkers, int backendQueue,
            int backendDeadlineMillis, int handshakeMillis, int frameMillis, int applicationReadMillis,
            int dbConnectMillis, int dbNetworkMillis, int acceptPerMinute, int acceptBurst) {}

    private static Limits limits(Properties properties) throws ConfigException {
        int sessions = limit(properties, "maxSessions", 32, 1, 256);
        int perIp = limit(properties, "maxPerIp", Math.min(4, sessions), 1, sessions);
        int workers = limit(properties, "backendWorkers", 4, 1, 32);
        int queue = limit(properties, "backendQueue", 32, 0, 256);
        int deadline = limit(properties, "backendDeadlineMillis", 5000, 100, 30000);
        int handshake = limit(properties, "handshakeMillis", 5000, 100, 30000);
        int frame = limit(properties, "frameMillis", 5000, 100, 30000);
        int read = limit(properties, "applicationReadMillis", 10000, 100, 30000);
        int connect = limit(properties, "dbConnectMillis", Math.min(1000, deadline), 100, deadline);
        int network = limit(properties, "dbNetworkMillis", Math.min(3000, deadline), 100, deadline);
        int rate = limit(properties, "acceptPerMinute", 60, 1, 3600);
        int burst = limit(properties, "acceptBurst", Math.min(4, sessions), 1, sessions);
        return new Limits(sessions, perIp, workers, queue, deadline, handshake, frame, read, connect, network, rate, burst);
    }

    private static int limit(Properties properties, String name, int defaultValue, int min, int max) throws ConfigException {
        return number(properties.getProperty("limits." + name, Integer.toString(defaultValue)), min, max);
    }

    private static int number(String value, int min, int max) throws ConfigException {
        if (value.isEmpty() || value.length() > 10 || !value.matches("0|[1-9][0-9]*")) throw failure(Failure.INVALID_VALUE);
        long parsed;
        try { parsed = Long.parseLong(value); }
        catch (NumberFormatException ignored) { throw failure(Failure.INVALID_VALUE); }
        if (parsed < min || parsed > max) throw failure(Failure.INVALID_VALUE);
        return (int) parsed;
    }

    private static List<Integer> sourceIds(String value) throws ConfigException {
        String[] fields = value.split(",", -1);
        if (fields.length == 0 || fields.length > 64) throw failure(Failure.INVALID_VALUE);
        TreeSet<Integer> ids = new TreeSet<>();
        for (String field : fields) if (!ids.add(number(field, 1, Integer.MAX_VALUE))) throw failure(Failure.INVALID_VALUE);
        return List.copyOf(ids);
    }

    private static String required(Properties properties, String key) throws ConfigException {
        String value = properties.getProperty(key);
        if (value == null || value.isEmpty()) throw failure(Failure.INVALID_VALUE);
        return value;
    }

    private static String envName(String value) throws ConfigException {
        if (!value.matches("[A-Za-z_][A-Za-z0-9_]{0,127}")) throw failure(Failure.INVALID_ENVIRONMENT);
        return value;
    }

    private static char[] secret(Function<String, String> environment, String name,
            int maxChars, int maxBytes, boolean noOuterWhitespace) throws ConfigException {
        String value;
        try { value = environment.apply(name); }
        catch (RuntimeException ignored) { throw failure(Failure.INVALID_ENVIRONMENT); }
        if (!validText(value, maxChars, maxBytes) || (noOuterWhitespace && !value.equals(value.strip()))) {
            throw failure(Failure.INVALID_ENVIRONMENT);
        }
        return value.toCharArray();
    }

    private static boolean validText(String value, int maxChars, int maxBytes) {
        if (value == null || value.isEmpty() || value.length() > maxChars) return false;
        int bytes = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isISOControl(current)) return false;
            if (Character.isHighSurrogate(current)) {
                if (++index >= value.length() || !Character.isLowSurrogate(value.charAt(index))) return false;
                bytes += 4;
            } else if (Character.isLowSurrogate(current)) return false;
            else bytes += current < 0x80 ? 1 : current < 0x800 ? 2 : 3;
        }
        return bytes <= maxBytes;
    }

    private static InetAddress numericAddress(String value) throws ConfigException {
        if (value.length() > 45 || value.isEmpty()) throw failure(Failure.INVALID_VALUE);
        try {
            if (!value.contains(":")) {
                String[] octets = value.split("\\.", -1);
                if (octets.length != 4) throw failure(Failure.INVALID_VALUE);
                byte[] address = new byte[4];
                for (int index = 0; index < 4; index++) address[index] = (byte) number(octets[index], 0, 255);
                return InetAddress.getByAddress(address);
            }
            // The colon plus this whitelist makes this a literal-only parser, never a DNS lookup.
            if (!value.matches("[0-9a-fA-F:.]+")) throw failure(Failure.INVALID_VALUE);
            if (value.contains(".")) {
                String tail = value.substring(value.lastIndexOf(':') + 1);
                String[] octets = tail.split("\\.", -1);
                if (octets.length != 4) throw failure(Failure.INVALID_VALUE);
                for (String octet : octets) number(octet, 0, 255);
            }
            return InetAddress.getByName(value);
        } catch (IOException | RuntimeException ignored) {
            throw failure(Failure.INVALID_VALUE);
        }
    }

    private static boolean broadcast(InetAddress address) {
        byte[] bytes = address.getAddress();
        return bytes.length == 4 && bytes[0] == -1 && bytes[1] == -1 && bytes[2] == -1 && bytes[3] == -1;
    }

    /** Reusable validation for the later bounded key-store read; final absence is permitted only when requested. */
    static Path checkedLocalPath(Path path, boolean mustExist) throws ConfigException {
        if (path == null || !path.isAbsolute() || path.getFileName() == null) throw failure(Failure.INVALID_PATH);
        String raw = path.toString();
        if (raw.length() > MAX_PROPERTY_CHARS || raw.startsWith("\\\\") || raw.startsWith("//")) {
            throw failure(Failure.INVALID_PATH);
        }
        for (Path component : path) {
            String name = component.toString();
            if (name.equals(".") || name.equals("..") || name.endsWith(".") || name.endsWith(" ")
                    || name.contains(":") || !validText(name, MAX_PROPERTY_CHARS, 4096)) throw failure(Failure.INVALID_PATH);
            String stem = name.split("\\.", 2)[0].toUpperCase(Locale.ROOT);
            if (stem.matches("CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]")) throw failure(Failure.INVALID_PATH);
        }
        Path cursor = path.getRoot();
        try {
            checkDirectory(cursor);
            for (int index = 0; index < path.getNameCount(); index++) {
                cursor = cursor.resolve(path.getName(index));
                boolean last = index == path.getNameCount() - 1;
                BasicFileAttributes attributes;
                try { attributes = Files.readAttributes(cursor, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS); }
                catch (NoSuchFileException absent) {
                    if (last && !mustExist) return path;
                    throw failure(Failure.INVALID_PATH);
                }
                if (attributes.isSymbolicLink() || attributes.isOther()
                        || (!last && !attributes.isDirectory()) || (last && !attributes.isRegularFile())) {
                    throw failure(Failure.INVALID_PATH);
                }
                // Also rejects provider-recognized junction/alias traversal even if isSymbolicLink is false.
                if (!cursor.toRealPath().equals(cursor.toRealPath(LinkOption.NOFOLLOW_LINKS))) throw failure(Failure.INVALID_PATH);
            }
            return path;
        } catch (ConfigException error) {
            throw error;
        } catch (IOException | RuntimeException ignored) {
            throw failure(Failure.INVALID_PATH);
        }
    }

    private static void checkDirectory(Path path) throws IOException, ConfigException {
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isDirectory() || attributes.isSymbolicLink() || attributes.isOther()) throw failure(Failure.INVALID_PATH);
    }

    private static void ensureOpen(boolean closed) {
        if (closed) throw new IllegalStateException("modern server credentials are closed");
    }
    private static ConfigException failure(Failure failure) { return new ConfigException(failure); }

    private static final class PropertyFailure extends IllegalArgumentException {
        final Failure failure;
        PropertyFailure(Failure failure) { super("invalid modern properties"); this.failure = failure; }
    }

    private static final class StrictProperties extends Properties {
        @Override public synchronized Object put(Object key, Object value) {
            int maximum=key instanceof String field && field.matches("world\\.[0-5]\\.[0-9]+\\.(npcSpawns|questWave[01]_[1-4])")?8192:
                    key instanceof String field && field.matches("world\\.[0-5]\\.[0-9]+\\.npcIds")?2048:MAX_PROPERTY_CHARS;
            if (!(key instanceof String name) || !(value instanceof String text)
                    || name.isEmpty() || name.length() > 128 || text.length() > maximum
                    || !validText(name, 128, 512) || (!text.isEmpty() && !validText(text, maximum, maximum*4))
                    || size() >= ROOT_KEYS.size() + 64 * SOURCE_SUFFIXES.size()) throw new PropertyFailure(Failure.INVALID_PROPERTIES);
            if (containsKey(key)) throw new PropertyFailure(Failure.DUPLICATE_PROPERTY);
            return super.put(key, value);
        }
    }
}

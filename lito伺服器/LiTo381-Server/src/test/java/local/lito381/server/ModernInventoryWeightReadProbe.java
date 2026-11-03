package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.*;

import com.sun.nio.file.ExtendedOpenOption;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** TEST CLASSPATH ONLY. One argument: absolute native server.properties.
 * Uses the existing strict explicit-source loader and SELECT-only modern DAO.
 * Required env: LITO_MODERN_PROBE_ACCOUNT, LITO_MODERN_PROBE_PASSWORD,
 * LITO_MODERN_WEIGHT_PROBE_RATES (exact sourceId=positiveRate comma mapping),
 * Exactly one of LITO_MODERN_WEIGHT_PROBE_OVERRIDE_DIRECTORY (existing directory AND lock)
 * or LITO_MODERN_WEIGHT_PROBE_NO_OVERRIDES=source-only-observation. The latter is
 * explicitly SOURCE_ONLY_ABSENT, not the current modern authority's loadout.
 * No defaults, listener, native Main, migration, purchase, directory creation,
 * store constructor, store write, outbox or SQL mutation. An existing shared
 * lock excludes cooperating override writers for this complete observation.
 * This is NOT an authority ACK, atomic SQL generation or battle admission.
 */
public final class ModernInventoryWeightReadProbe {
    static final int DEADLINE_MILLIS = 5_000;
    static final String OBSERVATION = "readonly_observation_not_authority_ack_or_battle_admission";
    enum Stage { CONFIGURATION, AUTHENTICATION, ROSTER, ROUTING, SOURCE, CATALOG, OVERRIDES,
        RECONCILE, EVALUATE, WEIGHT, VERIFY_ROSTER, COMPLETE }
    enum Failure { NONE, CONFIGURATION, AUTHENTICATION, JDBC, ROUTING, ROSTER_CHANGED,
        OVERRIDE_UNAVAILABLE, BINDING_MISMATCH, QUALIFICATION, ARITHMETIC_OVERFLOW,
        DEADLINE, CANCELLED, CLOSED, OVERLOADED, BUSY, BACKEND, WORKER_NOT_RETIRED }
    enum OverrideMode { READONLY_EXISTING_STORE, SOURCE_ONLY_ABSENT }
    static final class Audit {
        volatile Stage stage = Stage.CONFIGURATION;
        volatile Failure failure = Failure.NONE;
        volatile Summary summary;
    }
    record SourcePlan(ModernAccountAuthentication.Principal principal,
                      List<ModernApplicationProtocol.CharacterRow> roster) {
        SourcePlan { Objects.requireNonNull(principal); roster = List.copyOf(roster);
            if (roster.size() > ModernApplicationProtocol.MAX_ROWS) throw invalid(); }
        @Override public String toString() { return "WeightProbeSourcePlan[redacted]"; }
    }
    record Summary(int sourceId, long objectId, long equipmentRevision, int storedOverrides, int staleOverrides,
                   int sourceItems, int excludedEtc, int expiredGear, int unknownGear, int uninitializedOriginalStats,
                   double rate, ModernInventoryWeight.Basis basis, OverrideMode overrideMode) {
        @Override public String toString() { return "WeightProbeSummary[redacted]"; }
    }
    @FunctionalInterface interface Inspection { void read(Connection connection, ModernBackendScope scope) throws Exception; }
    private ModernInventoryWeightReadProbe() {}

    public static void main(String[] args) {
        var output = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
        ModernDatabaseReadProbe.Settings settings = null;
        ModernServerConfig config = null;
        OverrideInput overrides = null;
        ModernBackendExecutor executor = null;
        boolean retired = true;
        int exit = 2;
        try {
            if (args.length != 1) throw invalid();
            Path main = Path.of(args[0]);
            settings = ModernDatabaseReadProbe.load(main);
            String catalog = ModernCharacterDataReadProbe.catalogDatabase(main);
            config = ModernDatabaseReadProbe.modernConfig(settings, catalog);
            Map<Integer, Double> rates = rates(System.getenv("LITO_MODERN_WEIGHT_PROBE_RATES"),
                    config.sources().stream().map(ModernServerConfig.SourceConfig::id).collect(java.util.stream.Collectors.toSet()));
            overrides = overrideInput(System.getenv("LITO_MODERN_WEIGHT_PROBE_OVERRIDE_DIRECTORY"),
                    System.getenv("LITO_MODERN_WEIGHT_PROBE_NO_OVERRIDES"));
            final OverrideInput readonly = overrides;
            String account = System.getenv("LITO_MODERN_PROBE_ACCOUNT");
            String password = System.getenv("LITO_MODERN_PROBE_PASSWORD");
            if (!ModernCharacterDataReadProbe.boundedText(account, 64, false)
                    || !ModernCharacterDataReadProbe.boundedText(password, 256, true)) throw invalid();
            executor = new ModernBackendExecutor(1, 0); retired = false;
            boolean passed = true;
            sources: for (var source : config.sources()) {
                var auditRef = new AtomicReference<>(new Audit());
                var inspection = new AtomicReference<Inspection>();
                int network = config.dbNetworkMillis();
                var backend = new ModernJdbcBackend(source.database(), config.dbConnectMillis(), network, (url, properties) -> {
                    Connection connection = new com.mysql.cj.jdbc.Driver().connect(url, properties);
                    if (connection == null) throw bad(auditRef.get(), Failure.JDBC);
                    return inspectOwned(connection, ModernBackendScope.current(), source.database().database(), network,
                            auditRef.get(), inspection.get());
                });
                var verifier = ModernAccountAuthentication.repositories(Map.of(source.id(),
                        ModernAccountAuthentication.SourceBinding.direct(source.name(), backend::authenticate)), ignored -> false);
                try (var gate = executor.openSession()) {
                    SourcePlan plan = null;
                    Audit authentication = auditRef.get();
                    char[] ownedPassword = password.toCharArray();
                    try {
                        plan = gate.call(DEADLINE_MILLIS, new ModernBackendExecutor.Work<SourcePlan>() {
                            @Override public SourcePlan run(ModernBackendScope scope) throws Exception {
                                authentication.stage = Stage.AUTHENTICATION;
                                ModernAccountAuthentication.Principal principal;
                                try { principal = ModernAccountAuthentication.verify(verifier, account, ownedPassword); }
                                catch (ModernAccountAuthentication.AuthenticationException failure) {
                                    throw bad(authentication, Failure.AUTHENTICATION);
                                }
                                authentication.stage = Stage.ROSTER;
                                return new SourcePlan(principal, backend.characters(principal.accountForSource(source.id())));
                            }
                            @Override public void close() { Arrays.fill(ownedPassword, '\0'); }
                        });
                    } catch (ModernBackendExecutor.BackendException failure) { backendFailure(authentication, failure); }
                    if (!awaitQuiescent(executor)) {
                        authentication.failure = Failure.WORKER_NOT_RETIRED;
                        output.println(renderFailure(source.id(), 0, authentication)); passed = false; break;
                    }
                    if (plan == null) { output.println(renderFailure(source.id(), 0, authentication)); passed = false; continue; }
                    output.println("{\"phase\":\"weight-roster\",\"source_id\":" + source.id()
                            + ",\"character_count\":" + plan.roster().size() + "}");
                    final SourcePlan authorized = plan;
                    for (var row : plan.roster()) {
                        var audit = new Audit(); auditRef.set(audit);
                        inspection.set((connection, scope) -> audit.summary = readPipeline(connection, scope, source,
                                authorized.principal(), row, readonly, rates.get(source.id()), audit));
                        try {
                            gate.call(DEADLINE_MILLIS, scope -> {
                                try {
                                    var roster = backend.characters(authorized.principal().accountForSource(source.id()));
                                    audit.stage = Stage.VERIFY_ROSTER;
                                    if (!roster.equals(authorized.roster()) || audit.summary == null
                                            || audit.summary.objectId() != row.objectId() || audit.summary.sourceId() != source.id()) {
                                        throw bad(audit, Failure.ROSTER_CHANGED);
                                    }
                                    readonly.verify(); scope.checkActive(); audit.stage = Stage.COMPLETE; return null;
                                } finally { inspection.set(null); }
                            });
                        } catch (ModernBackendExecutor.BackendException failure) { backendFailure(audit, failure); }
                        if (!awaitQuiescent(executor)) {
                            audit.failure = Failure.WORKER_NOT_RETIRED;
                            output.println(renderFailure(source.id(), row.objectId(), audit)); passed = false; break sources;
                        }
                        inspection.set(null);
                        if (audit.failure == Failure.NONE && audit.stage == Stage.COMPLETE) {
                            output.println(render(audit.summary));
                            if (audit.summary.basis() == null) passed = false;
                        } else { output.println(renderFailure(source.id(), row.objectId(), audit)); passed = false; }
                    }
                }
            }
            exit = passed ? 0 : 1;
        } catch (Throwable failure) {
            output.println("{\"phase\":\"weight-probe\",\"status\":\"FAILED\",\"reason\":\"CONFIGURATION_OR_STARTUP\"}");
        } finally {
            if (executor != null) {
                try { executor.close(); retired = executor.awaitTermination(3_000); }
                catch (Throwable failure) { retired = false; }
            }
            if (retired) {
                try {
                    try { if (overrides != null) overrides.close(); }
                    finally { try { if (config != null) config.close(); } finally { if (settings != null) settings.close(); } }
                } catch (Throwable failure) {
                    output.println("{\"phase\":\"cleanup\",\"reason\":\"OWNER_CLOSE_FAILED\"}"); exit = 2;
                }
            } else {
                // Do not invalidate configuration or the store pin beneath an actual live worker.
                output.println("{\"phase\":\"cleanup\",\"reason\":\"WORKER_NOT_RETIRED\"}"); exit = 2;
            }
        }
        output.flush(); if (exit != 0) System.exit(exit);
    }

    static Map<Integer, Double> rates(String text, Set<Integer> sources) {
        if (text == null || text.isEmpty() || text.length() > 8192 || sources == null || sources.isEmpty()) throw invalid();
        var result = new TreeMap<Integer, Double>();
        for (String pair : text.split(",", -1)) {
            if (!pair.matches("[1-9][0-9]{0,9}=(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?")) throw invalid();
            String[] parts = pair.split("=", -1);
            int id; double rate;
            try { id = Integer.parseInt(parts[0]); rate = Double.parseDouble(parts[1]); }
            catch (NumberFormatException failure) { throw invalid(); }
            if (!Double.isFinite(rate) || rate <= 0 || result.putIfAbsent(id, rate) != null) throw invalid();
        }
        if (!result.keySet().equals(sources)) throw invalid();
        return Map.copyOf(result);
    }

    /** Raw socket remains owned by ModernJdbcBackend.Attempt throughout this temporary lease. */
    static Connection inspectOwned(Connection connection, ModernBackendScope scope, String database,
                                   int networkMillis, Audit audit, Inspection inspection) throws SQLException {
        boolean transferred = false;
        Stage requested = audit.stage;
        try {
            try (var lease = scope.register(connection)) {
                scope.checkActive(); connection.setNetworkTimeout(Runnable::run, Math.min(networkMillis, scope.remainingMillis()));
                connection.setReadOnly(true);
                if (!connection.isReadOnly()) throw bad(audit, Failure.JDBC);
                audit.stage = Stage.ROUTING;
                try (var query = connection.prepareStatement("SELECT DATABASE()")) {
                    query.setMaxRows(2); query.setQueryTimeout((int)Math.max(1L, ((long)scope.remainingMillis() + 999) / 1000));
                    try (var rows = query.executeQuery()) {
                        if (!rows.next() || !database.equals(rows.getString(1)) || rows.next()) throw bad(audit, Failure.ROUTING);
                    }
                }
                scope.checkActive();
                if (inspection != null) inspection.read(connection, scope);
                scope.checkActive();
            }
            scope.checkActive(); if (inspection == null) audit.stage = requested;
            transferred = true; return connection;
        } catch (Exception failure) {
            if (audit.failure == Failure.NONE) audit.failure = Failure.JDBC;
            throw new SQLException("bounded weight probe failed");
        } finally { if (!transferred) try { connection.close(); } catch (SQLException | RuntimeException ignored) {} }
    }

    static Summary readPipeline(Connection connection, ModernBackendScope scope, ModernServerConfig.SourceConfig sourceConfig,
            ModernAccountAuthentication.Principal principal, ModernApplicationProtocol.CharacterRow expected,
            OverrideInput overrides, double rate, Audit audit) throws Exception {
        try {
            audit.stage = Stage.SOURCE;
            var source = ModernEquipmentSourceJdbc.read(connection, scope, sourceConfig.id(),
                    principal.accountForSource(sourceConfig.id()), expected.objectId(), sourceConfig.catalogDatabase())
                    .orElseThrow(() -> bad(audit, Failure.ROSTER_CHANGED));
            if (!source.snapshot().character().equals(expected)) throw bad(audit, Failure.ROSTER_CHANGED);
            audit.stage = Stage.CATALOG;
            var catalog = ModernEquipmentCatalogJdbc.read(connection, scope, sourceConfig.id(), sourceConfig.catalogDatabase(),
                    requiredLookups(source.instances()), requiredSetIds(source.instances()));
            var qualified = source.qualify(catalog);
            var key = new TournamentPresence.CharacterKey(sourceConfig.id(), expected.objectId());
            var binding = new ModernEquipmentReconciliation.ExpectedBinding(key, ModernEquipmentBinding.ownerBinding(principal, sourceConfig));
            audit.stage = Stage.OVERRIDES;
            Optional<ModernEquipmentOverrideStore.Stored> stored;
            try { stored = overrides.read(key, scope); }
            catch (IOException failure) { throw bad(audit, Failure.OVERRIDE_UNAVAILABLE); }
            long now = System.currentTimeMillis();
            audit.stage = Stage.RECONCILE;
            var snapshot = observation(qualified, binding, stored, now);
            scope.checkActive(); audit.stage = Stage.WEIGHT;
            return summarize(snapshot, stored, rate, overrides.mode());
        } catch (ModernEquipmentReconciliation.ReconciliationException failure) {
            throw bad(audit, failure.failure() == ModernEquipmentReconciliation.Failure.BINDING_MISMATCH
                    ? Failure.BINDING_MISMATCH : Failure.QUALIFICATION);
        } catch (ArithmeticException failure) { throw bad(audit, Failure.ARITHMETIC_OVERFLOW); }
        catch (IllegalArgumentException failure) { throw bad(audit, Failure.QUALIFICATION); }
    }

    /** Same pure evaluation inputs as Authority.READ; test-domain view token is deliberately NOT an ACK. */
    static ModernEquipmentAuthority.Snapshot observation(Qualified qualified,
            ModernEquipmentReconciliation.ExpectedBinding binding, Optional<ModernEquipmentOverrideStore.Stored> stored,
            long now) throws Exception {
        var reconciled = ModernEquipmentReconciliation.reconcile(qualified, binding, stored, now);
        var evaluation = ModernEquipmentEvaluator.evaluate(qualified, reconciled.overrides(), now);
        Ability modifiers = Ability.zero();
        for (Stat stat : Stat.values()) modifiers = modifiers.with(stat, evaluation.total(stat));
        var flags = reconciled.desiredLoadout().items().stream()
                .map(row -> new ModernEquipmentAuthority.Equipped(row.objectId(), row.equipped())).toList();
        var lifetimes = qualified.lifetimes().views(qualified.source(), now);
        long next = evaluation.nextStoneExpiry().orElse(-1);
        for (var life : lifetimes) if (life.state() == ModernItemLifetime.State.ACTIVE)
            next = next < 0 ? life.expiresAtMillis() : Math.min(next, life.expiresAtMillis());
        var token = new ModernEquipmentOverrideStore.Fingerprint(MessageDigest.getInstance("SHA-256").digest(
                "LiTo381.TEST_ONLY.WeightReadObservation.NOT_AUTHORITY_ACK.v1".getBytes(StandardCharsets.US_ASCII)));
        var snapshot = new ModernEquipmentAuthority.Snapshot(qualified.source(), reconciled.expected().revision(), token,
                ModernEquipmentBinding.catalogGeneration(qualified.catalog()).fingerprint(), now, next, modifiers,
                evaluation.magicReductionBase(), flags, reconciled.staleItems(), lifetimes);
        ModernEquipmentProtocol.validateSnapshot(snapshot);
        return snapshot;
    }

    static Summary summarize(ModernEquipmentAuthority.Snapshot snapshot,
                             Optional<ModernEquipmentOverrideStore.Stored> stored, double rate) {
        return summarize(snapshot,stored,rate,OverrideMode.READONLY_EXISTING_STORE);
    }
    static Summary summarize(ModernEquipmentAuthority.Snapshot snapshot,
                             Optional<ModernEquipmentOverrideStore.Stored> stored, double rate, OverrideMode mode) {
        if (mode == null || mode == OverrideMode.SOURCE_ONLY_ABSENT && stored.isPresent()) throw invalid();
        if (!Double.isFinite(rate) || rate <= 0) throw invalid();
        int etc = 0, expired = 0, unknown = 0;
        for (int i = 0; i < snapshot.source().inventory().size(); i++) {
            var item = snapshot.source().inventory().get(i); var life = snapshot.lifetimes().get(i);
            if (item.template().kind() == ModernCharacterData.ItemKind.ETC) etc++;
            else if (life.state() == ModernItemLifetime.State.EXPIRED) expired++;
            else if (!life.usable()) unknown++;
        }
        // Unknown source gear is visible as a rejected qualification, never silently zero weight.
        int uninitialized = (snapshot.source().persisted().originalStrength() == 0 ? 1 : 0)
                + (snapshot.source().persisted().originalConstitution() == 0 ? 1 : 0);
        ModernInventoryWeight.Basis basis;
        try {
            basis = ModernInventoryWeight.qualify(snapshot, rate);
        } catch (IllegalArgumentException rejected) {
            if (unknown == 0 && uninitialized == 0) throw rejected;
            basis = null;
        }
        if (basis != null && (unknown != 0 || uninitialized != 0)) throw invalid();
        return new Summary(snapshot.source().sourceId(), snapshot.source().character().objectId(), snapshot.revision(),
                stored.map(value -> value.value().overrides().size()).orElse(0), snapshot.stale().size(),
                snapshot.source().inventory().size(), etc, expired, unknown, uninitialized, rate, basis, mode);
    }
    static String render(Summary row) {
        var basis = row.basis();
        return "{\"phase\":\"weight-read\",\"status\":\"" + (basis == null ? "UNQUALIFIED" : "OK")
                + "\",\"observation\":\"" + OBSERVATION + "\",\"consistency\":\"multi_query_not_atomic_generation\""
                + ",\"override_mode\":\"" + row.overrideMode() + "\""
                + ",\"source_id\":" + row.sourceId() + ",\"object_id\":" + row.objectId()
                + ",\"equipment_revision\":" + row.equipmentRevision() + ",\"stored_overrides\":" + row.storedOverrides()
                + ",\"stale_overrides\":" + row.staleOverrides() + ",\"source_items\":" + row.sourceItems()
                + ",\"source_etc_excluded\":" + row.excludedEtc() + ",\"expired_gear_excluded\":" + row.expiredGear()
                + ",\"unknown_gear_rejected\":" + row.unknownGear() + ",\"uninitialized_original_stat_count\":"
                + row.uninitializedOriginalStats() + ",\"source_rate\":" + row.rate()
                + ",\"source_gear_raw\":" + (basis == null ? "null" : basis.sourceGearWeight())
                + ",\"maximum_weight\":" + (basis == null ? "null" : basis.maximumWeight())
                + ",\"stage\":" + (basis == null ? "null" : ModernInventoryWeight.weight240(basis.sourceGearWeight(), basis.maximumWeight()))
                + ",\"match_supplies_raw\":0,\"valid_until_millis\":" + (basis == null ? "null" : basis.validUntilMillis()) + "}";
    }
    static String renderFailure(int source, long object, Audit audit) {
        return "{\"phase\":\"weight-read\",\"status\":\"FAILED\",\"source_id\":" + source + ",\"object_id\":" + object
                + ",\"stage\":\"" + audit.stage + "\",\"reason\":\"" + audit.failure + "\"}";
    }
    private static SQLException bad(Audit audit, Failure failure) { audit.failure = failure; return new SQLException("bounded weight probe failed"); }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("invalid weight probe configuration or facts"); }
    private static void backendFailure(Audit audit, ModernBackendExecutor.BackendException failure) {
        if (audit.failure == Failure.NONE) audit.failure = Failure.valueOf(failure.failure().name());
    }
    private static boolean awaitQuiescent(ModernBackendExecutor executor) throws InterruptedException {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (!executor.isQuiescent()) {
            if (System.nanoTime() - end >= 0) return false;
            Thread.sleep(5);
        }
        return true;
    }

    /** Read-only counterpart, NOT a ModernEquipmentOverrideStore constructor.
     * No CREATE, force, rename, deletion, recovery or implicit rebind is possible.
     * Like the production store, an administrator must own the local ancestors;
     * a cooperative lock is not protection against a privileged external editor.
     */
    interface OverrideInput extends AutoCloseable {
        Optional<ModernEquipmentOverrideStore.Stored> read(TournamentPresence.CharacterKey key,ModernBackendScope scope) throws IOException;
        void verify() throws IOException;
        OverrideMode mode();
        @Override void close() throws IOException;
    }
    static OverrideInput overrideInput(String directory, String noOverrides) throws IOException {
        // Presence, even an empty env value, is intentional input and cannot silently choose another mode.
        if ((directory != null) == (noOverrides != null)) throw invalid();
        if (directory != null) {
            if (directory.isEmpty()) throw invalid();
            return new ReadOnlyOverrides(Path.of(directory));
        }
        if (!"source-only-observation".equals(noOverrides)) throw invalid();
        return new OverrideInput() {
            private boolean closed;
            @Override public Optional<ModernEquipmentOverrideStore.Stored> read(TournamentPresence.CharacterKey key,ModernBackendScope scope) throws IOException {
                Objects.requireNonNull(key); verify(); scope.checkActive(); return Optional.empty();
            }
            @Override public void verify() throws IOException { if (closed) throw new IOException("readonly observation closed"); }
            @Override public OverrideMode mode() { return OverrideMode.SOURCE_ONLY_ABSENT; }
            @Override public void close() { closed = true; }
        };
    }
    static final class ReadOnlyOverrides implements OverrideInput {
        private final Path directory, lockPath;
        private final boolean windows = System.getProperty("os.name", "").startsWith("Windows");
        private final FileChannel lockFile;
        private final FileLock lock;
        private final Object lockKey, directoryKey;
        ReadOnlyOverrides(Path path) throws IOException {
            FileChannel file = null; FileLock acquired = null;
            try {
                if (path == null || !path.isAbsolute() || !path.equals(path.normalize())) throw unavailable();
                directory = path; lockPath = path.resolve(".modern-equipment.lock");
                ModernServerConfig.checkedLocalPath(lockPath, true);
                directoryKey = attributes(directory).fileKey(); lockKey = attributes(lockPath).fileKey();
                if (!windows && (directoryKey == null || lockKey == null)) throw unavailable();
                file = FileChannel.open(lockPath, options(false));
                acquired = file.tryLock(0, Long.MAX_VALUE, true);
                if (acquired == null) throw unavailable();
                lockFile = file; lock = acquired; verify();
            } catch (Exception failure) {
                if (acquired != null) try { acquired.close(); } catch (IOException ignored) {}
                if (file != null) try { file.close(); } catch (IOException ignored) {}
                throw unavailable();
            }
        }
        @Override public OverrideMode mode() { return OverrideMode.READONLY_EXISTING_STORE; }
        @Override public void verify() throws IOException {
            try {
                ModernServerConfig.checkedLocalPath(lockPath, true);
                if (!lock.isValid() || !lockFile.isOpen() || !Objects.equals(lockKey, attributes(lockPath).fileKey())
                        || !Objects.equals(directoryKey, attributes(directory).fileKey())) throw unavailable();
            } catch (Exception failure) { throw unavailable(); }
        }
        @Override public Optional<ModernEquipmentOverrideStore.Stored> read(TournamentPresence.CharacterKey key, ModernBackendScope scope) throws IOException {
            try {
                scope.checkActive(); verify();
                Path path = directory.resolve("source-" + key.sourceId() + "-character-" + key.objectId() + ".meo");
                ModernServerConfig.checkedLocalPath(path, false);
                BasicFileAttributes before;
                try { before = attributes(path); }
                catch (NoSuchFileException absent) { verify(); scope.checkActive(); return Optional.empty(); }
                if (!before.isRegularFile() || before.size() < 96 || before.size() > ModernEquipmentOverrideStore.MAX_BYTES) throw unavailable();
                if (!windows && before.fileKey() == null) throw unavailable();
                ByteBuffer bytes = ByteBuffer.allocate((int)before.size() + 1);
                try (FileChannel file = FileChannel.open(path, options(true))) {
                    while (bytes.hasRemaining()) {
                        scope.checkActive(); int count = file.read(bytes);
                        if (count < 0) break;
                        if (count == 0) throw unavailable();
                    }
                    ModernServerConfig.checkedLocalPath(path, true);
                    var after = attributes(path);
                    if (!Objects.equals(before.fileKey(), after.fileKey()) || before.size() != after.size()
                            || !before.lastModifiedTime().equals(after.lastModifiedTime()) || bytes.position() != before.size()) throw unavailable();
                }
                verify(); scope.checkActive();
                return Optional.of(ModernEquipmentOverrideStore.decode(Arrays.copyOf(bytes.array(), bytes.position()), key));
            } catch (Exception failure) { throw unavailable(); }
        }
        private Set<OpenOption> options(boolean noWriters) {
            var options = new HashSet<OpenOption>(List.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS));
            if (windows) { options.add(ExtendedOpenOption.NOSHARE_DELETE); if (noWriters) options.add(ExtendedOpenOption.NOSHARE_WRITE); }
            return Set.copyOf(options);
        }
        private static BasicFileAttributes attributes(Path path) throws IOException {
            return Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        }
        private static IOException unavailable() { return new IOException("readonly equipment observation unavailable"); }
        @Override public void close() throws IOException {
            try { try { lock.close(); } finally { lockFile.close(); } }
            catch (IOException failure) { throw unavailable(); }
        }
    }
}

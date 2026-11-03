package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.*;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TEST CLASSPATH ONLY, opt-in SELECT-only equipment pipeline inspection.
 * Argument: one absolute native server.properties, read by the existing strict
 * probe loader. Credentials: LITO_MODERN_PROBE_ACCOUNT / PASSWORD, never CLI.
 * No native Main, migration, override-store open/write, equipment toggle or UI.
 * Each character's source/catalog/reconciliation/evaluation uses one owned
 * read-only connection and one bounded backend scope. Multiple statements are
 * NOT an atomic source/catalog generation, including on MyISAM. These are only
 * traced equipment modifiers, not full/effective player or battle statistics.
 */
public final class ModernEquipmentReadProbe {
    static final int DEADLINE_MILLIS = 5_000;
    static final String BINDING_KIND = "synthetic_test_only_not_production_binding";
    static final String CONSISTENCY = "multi_query_not_atomic_generation";
    enum Stage { CONFIGURATION, AUTHENTICATION, ROSTER, ROUTING, SOURCE, CATALOG,
        QUALIFY, RECONCILE, EVALUATE, VERIFY_ROSTER, COMPLETE }
    enum Failure { NONE, AUTHENTICATION, JDBC, ROUTING_MISMATCH, MISSING_CHARACTER, ROSTER_CHANGED,
        QUALIFICATION, RECONCILIATION, EVALUATION, ARITHMETIC_OVERFLOW, INVALID_RESULT,
        DEADLINE, CANCELLED, CLOSED, OVERLOADED, BUSY, BACKEND, WORKER_NOT_RETIRED }
    static final class Audit {
        volatile Stage stage = Stage.CONFIGURATION;
        volatile Failure failure = Failure.NONE;
        volatile Summary summary;
    }
    record SourcePlan(String canonical, List<ModernApplicationProtocol.CharacterRow> roster) {
        SourcePlan {
            roster = List.copyOf(roster);
            if (roster.size() > ModernApplicationProtocol.MAX_ROWS) throw new IllegalArgumentException("probe roster bound");
        }
        @Override public String toString() { return "EquipmentProbeSourcePlan[redacted]"; }
    }
    record Plan(int sourceId, String canonical, ModernApplicationProtocol.CharacterRow expected) {
        Plan {
            if (sourceId < 1 || expected == null || !ModernCharacterDataReadProbe.boundedText(canonical,64,false)) {
                throw new IllegalArgumentException("invalid equipment probe plan");
            }
        }
        long objectId() { return expected.objectId(); }
        @Override public String toString() { return "EquipmentProbePlan[redacted]"; }
    }
    record Summary(int sourceId, long objectId, int items, int equipped, int sourceEquipped,
                   int suppressedEquipped, int expiredItems, int unqualifiedItems, int requiredLookups,
                   int lookupRows, int presentLookups, int requiredSetIds, int setRows, int comboRows,
                   long evaluatedAtMillis, ModernEquipmentEvaluator.Evaluation evaluation) {}
    @FunctionalInterface interface Pipeline {
        Summary read(Connection connection, ModernBackendScope scope, String catalog, Plan plan, Audit audit) throws Exception;
    }
    private ModernEquipmentReadProbe() {}

    public static void main(String[] args) {
        PrintWriter output = new PrintWriter(new OutputStreamWriter(System.out,StandardCharsets.UTF_8),true);
        ModernDatabaseReadProbe.Settings settings = null;
        ModernServerConfig config = null;
        ModernBackendExecutor executor = null;
        boolean retired = true;
        int exit = 2;
        try {
            if (args.length != 1) throw new IllegalArgumentException("explicit config required");
            Path mainFile = Path.of(args[0]);
            settings = ModernDatabaseReadProbe.load(mainFile);
            config = ModernDatabaseReadProbe.modernConfig(settings);
            String catalog = ModernCharacterDataReadProbe.catalogDatabase(mainFile);
            String account = System.getenv("LITO_MODERN_PROBE_ACCOUNT");
            String password = System.getenv("LITO_MODERN_PROBE_PASSWORD");
            if (!ModernCharacterDataReadProbe.boundedText(account,64,false)
                    || !ModernCharacterDataReadProbe.boundedText(password,256,true)) {
                throw new IllegalArgumentException("test credentials required");
            }
            executor = new ModernBackendExecutor(1,0); retired = false;
            boolean allPassed = true;
            sourceLoop: for (var source : config.sources()) {
                var authAudit = new Audit();
                var currentAudit = new AtomicReference<>(authAudit);
                var currentPlan = new AtomicReference<Plan>();
                int networkMillis = config.dbNetworkMillis();
                ModernJdbcBackend.Connector connector = (url,properties) -> {
                    Connection connection = new com.mysql.cj.jdbc.Driver().connect(url,properties);
                    if (connection == null) throw bad(currentAudit.get(),Failure.JDBC);
                    return inspectOwned(connection,ModernBackendScope.current(),source.database().database(),catalog,
                            networkMillis,currentPlan.get(),currentAudit.get(),ModernEquipmentReadProbe::readPipeline);
                };
                var backend = new ModernJdbcBackend(source.database(),config.dbConnectMillis(),networkMillis,connector);
                var verifier = ModernAccountAuthentication.repositories(Map.of(source.id(),
                        ModernAccountAuthentication.SourceBinding.direct(source.name(),backend::authenticate)),ignored -> false);
                try (var gate = executor.openSession()) {
                    SourcePlan verified = null;
                    char[] ownedPassword = password.toCharArray();
                    try {
                        verified = gate.call(DEADLINE_MILLIS,new ModernBackendExecutor.Work<SourcePlan>() {
                            @Override public SourcePlan run(ModernBackendScope scope) throws Exception {
                                try {
                                    authAudit.stage = Stage.AUTHENTICATION;
                                    var principal = ModernAccountAuthentication.verify(verifier,account,ownedPassword);
                                    authAudit.stage = Stage.ROSTER;
                                    String canonical = principal.accountForSource(source.id());
                                    return new SourcePlan(canonical,backend.characters(canonical));
                                } catch (ModernAccountAuthentication.AuthenticationException failure) {
                                    if (authAudit.failure == Failure.NONE) authAudit.failure = Failure.AUTHENTICATION;
                                    throw failure;
                                }
                            }
                            @Override public void close() { Arrays.fill(ownedPassword,'\0'); }
                        });
                    } catch (ModernBackendExecutor.BackendException failure) { recordBackendFailure(authAudit,failure); }
                    if (!awaitQuiescent(executor,3_000)) {
                        authAudit.failure = Failure.WORKER_NOT_RETIRED;
                        output.println(renderFailure(source.id(),0,authAudit)); allPassed = false; break;
                    }
                    if (verified == null) {
                        output.println(renderFailure(source.id(),0,authAudit)); allPassed = false; continue;
                    }
                    output.println("{\"phase\":\"equipment-roster\",\"source_id\":"+source.id()
                            +",\"status\":\"OK\",\"character_count\":"+verified.roster().size()+"}");
                    final SourcePlan authorized = verified;
                    for (var row : authorized.roster()) {
                        var audit = new Audit(); currentAudit.set(audit);
                        currentPlan.set(new Plan(source.id(),authorized.canonical(),row));
                        try {
                            gate.call(DEADLINE_MILLIS,scope -> {
                                try {
                                    // Connector performs the bounded pipeline before
                                    // returning this SAME connection to its owned DAO.
                                    var rechecked = backend.characters(authorized.canonical());
                                    audit.stage = Stage.VERIFY_ROSTER;
                                    if (!rechecked.equals(authorized.roster())) throw bad(audit,Failure.ROSTER_CHANGED);
                                    if (audit.summary == null || audit.summary.sourceId() != source.id()
                                            || audit.summary.objectId() != row.objectId()) throw bad(audit,Failure.INVALID_RESULT);
                                    audit.stage = Stage.COMPLETE;
                                    return null;
                                } finally { currentPlan.set(null); }
                            });
                        } catch (ModernBackendExecutor.BackendException failure) { recordBackendFailure(audit,failure); }
                        if (!awaitQuiescent(executor,3_000)) {
                            audit.failure = Failure.WORKER_NOT_RETIRED;
                            output.println(renderFailure(source.id(),row.objectId(),audit));
                            allPassed = false; break sourceLoop;
                        }
                        currentPlan.set(null);
                        if (audit.failure == Failure.NONE && audit.stage == Stage.COMPLETE && audit.summary != null) {
                            output.println(render(audit.summary));
                        } else {
                            output.println(renderFailure(source.id(),row.objectId(),audit)); allPassed = false;
                        }
                    }
                }
            }
            exit = allPassed ? 0 : 1;
        } catch (Throwable failure) {
            output.println("{\"phase\":\"equipment-probe\",\"status\":\"FAILED\",\"reason\":\"CONFIGURATION_OR_STARTUP\"}");
        } finally {
            if (executor != null) {
                try { executor.close(); retired = executor.awaitTermination(3_000); }
                catch (Throwable failure) { retired = false; }
            }
            if (retired) {
                try {
                    try { if (config != null) config.close(); }
                    finally { if (settings != null) settings.close(); }
                } catch (Throwable failure) {
                    output.println("{\"phase\":\"cleanup\",\"status\":\"FAILED\",\"reason\":\"OWNER_CLOSE_FAILED\"}"); exit = 2;
                }
            } else {
                // Config secret copies remain owned until the actual worker
                // retires; a failed probe process must not claim detached success.
                output.println("{\"phase\":\"cleanup\",\"status\":\"FAILED\",\"reason\":\"WORKER_NOT_RETIRED\"}"); exit = 2;
            }
        }
        output.flush(); if (exit != 0) System.exit(exit);
    }

    /** Same temporary connection-lease transfer as CharacterDataReadProbe. */
    static Connection inspectOwned(Connection connection, ModernBackendScope scope, String expectedDatabase,
            String catalog, int networkMillis, Plan plan, Audit audit, Pipeline pipeline) throws SQLException {
        boolean transferred = false;
        Stage requestedStage = audit.stage;
        try {
            try (var lease = scope.register(connection)) {
                scope.checkActive();
                connection.setNetworkTimeout(Runnable::run,Math.min(networkMillis,scope.remainingMillis()));
                connection.setReadOnly(true);
                if (!connection.isReadOnly()) throw bad(audit,Failure.JDBC);
                audit.stage = Stage.ROUTING;
                try (var query = connection.prepareStatement("SELECT DATABASE()")) {
                    query.setMaxRows(2); query.setQueryTimeout((int)Math.max(1L,((long)scope.remainingMillis()+999L)/1000L));
                    try (var rows = query.executeQuery()) {
                        if (!rows.next() || !expectedDatabase.equals(rows.getString(1)) || rows.next()) {
                            throw bad(audit,Failure.ROUTING_MISMATCH);
                        }
                    }
                }
                scope.checkActive();
                if (plan != null) audit.summary = pipeline.read(connection,scope,catalog,plan,audit);
                scope.checkActive();
            }
            scope.checkActive();
            if (plan == null) audit.stage = requestedStage;
            transferred = true; return connection;
        } catch (Exception failure) {
            if (audit.failure == Failure.NONE) audit.failure = Failure.JDBC;
            throw new SQLException("bounded equipment probe failed");
        } finally {
            if (!transferred) try { connection.close(); } catch (SQLException | RuntimeException ignored) {}
        }
    }

    static Summary readPipeline(Connection connection, ModernBackendScope scope, String catalog, Plan plan, Audit audit)
            throws Exception {
        try {
            audit.stage = Stage.SOURCE;
            var source = ModernEquipmentSourceJdbc.read(connection,scope,plan.sourceId(),plan.canonical(),plan.objectId(),catalog)
                    .orElseThrow(() -> bad(audit,Failure.MISSING_CHARACTER));
            if (!source.snapshot().character().equals(plan.expected())) throw bad(audit,Failure.ROSTER_CHANGED);
            audit.stage = Stage.CATALOG;
            var rules = ModernEquipmentCatalogJdbc.read(connection,scope,plan.sourceId(),catalog,
                    requiredLookups(source.instances()),requiredSetIds(source.instances()));
            audit.stage = Stage.QUALIFY;
            var qualified = source.qualify(rules);
            audit.stage = Stage.RECONCILE;
            // Reconciliation and evaluation must observe the same expiry instant.
            long now = System.currentTimeMillis();
            var reconciled = ModernEquipmentReconciliation.reconcile(qualified,
                    syntheticBinding(plan.sourceId(),plan.objectId()),Optional.empty(),now);
            validateUnstoredReconciliation(qualified,reconciled,now);
            audit.stage = Stage.EVALUATE;
            var evaluation = ModernEquipmentEvaluator.evaluate(qualified,reconciled.overrides(),now);
            scope.checkActive();
            return summarize(qualified,evaluation,now);
        } catch (ArithmeticException failure) {
            throw bad(audit,Failure.ARITHMETIC_OVERFLOW);
        } catch (IllegalArgumentException failure) {
            throw bad(audit,switch (audit.stage) {
                case QUALIFY -> Failure.QUALIFICATION;
                case RECONCILE -> Failure.RECONCILIATION;
                case EVALUATE -> Failure.EVALUATION;
                default -> Failure.INVALID_RESULT;
            });
        }
    }

    /** Deliberately synthetic; never derived from credentials or used to open/save a store. */
    static ModernEquipmentReconciliation.ExpectedBinding syntheticBinding(int sourceId, long objectId) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                "LiTo381.TEST_ONLY.EquipmentReadProbe.NoStoredRecord.NotOwnerBinding.v1".getBytes(StandardCharsets.US_ASCII));
        return new ModernEquipmentReconciliation.ExpectedBinding(new TournamentPresence.CharacterKey(sourceId,objectId),
                new ModernEquipmentOverrideStore.Fingerprint(digest));
    }
    static void validateUnstoredReconciliation(Qualified qualified, ModernEquipmentReconciliation.Result reconciled, long now) {
        // No saved private overrides were opened. The only permitted overrides
        // are the production lifetime guard's explicit false entries, including
        // unusable items whose raw SQL equipped flag was already false.
        var expected = new TreeMap<Long, Boolean>();
        for (var item : qualified.source().inventory()) {
            if (!qualified.lifetimes().view(item.objectId(),now).usable()) expected.put(item.objectId(),false);
        }
        if (!reconciled.overrides().equals(expected) || !reconciled.staleItems().isEmpty()
                || !reconciled.expected().equals(ModernEquipmentOverrideStore.Expected.absent())) {
            throw new IllegalArgumentException("probe lifetime reconciliation mismatch");
        }
    }
    static Summary summarize(Qualified qualified, ModernEquipmentEvaluator.Evaluation evaluation, long now) {
        int sourceEquipped = 0, equipped = 0, expired = 0, unqualified = 0;
        for (var item : qualified.source().inventory()) {
            var life = qualified.lifetimes().view(item.objectId(),now);
            if (item.equipped()) {
                sourceEquipped++;
                if (life.usable()) equipped++;
            }
            if (life.state() == ModernItemLifetime.State.EXPIRED) expired++;
            else if (!life.usable()) unqualified++;
        }
        if (evaluation.equippedRows() != equipped) throw new IllegalArgumentException("probe equipped count mismatch");
        var rules = qualified.catalog();
        return new Summary(qualified.source().sourceId(),qualified.source().character().objectId(),qualified.items().size(),equipped,
                sourceEquipped,sourceEquipped-equipped,expired,unqualified,
                requiredLookups(List.copyOf(qualified.items().values())).size(),rules.lookups().size(),
                (int)rules.lookups().values().stream().filter(RuleValue::present).count(),rules.verifiedSetIds().size(),
                rules.sets().size(),rules.combos().size(),now,evaluation);
    }
    static String render(Summary row) {
        var out = new StringBuilder("{\"phase\":\"equipment-read\",\"status\":\"OK\",\"source_id\":")
                .append(row.sourceId()).append(",\"object_id\":").append(row.objectId())
                .append(",\"binding_kind\":\"").append(BINDING_KIND).append("\",\"consistency\":\"").append(CONSISTENCY)
                .append("\",\"values_kind\":\"traced_equipment_modifiers_not_live_player_stats\",\"stored_override_count\":0")
                .append(",\"item_count\":").append(row.items()).append(",\"equipped_count\":").append(row.equipped())
                .append(",\"equipped_count_kind\":\"effective_after_lifetime_guard\",\"source_equipped_count\":").append(row.sourceEquipped())
                .append(",\"lifetime_suppressed_equipped_count\":").append(row.suppressedEquipped())
                .append(",\"expired_item_count\":").append(row.expiredItems()).append(",\"unqualified_lifetime_item_count\":").append(row.unqualifiedItems())
                .append(",\"required_lookup_count\":").append(row.requiredLookups()).append(",\"lookup_count\":").append(row.lookupRows())
                .append(",\"present_lookup_count\":").append(row.presentLookups()).append(",\"absent_lookup_count\":").append(row.lookupRows()-row.presentLookups())
                .append(",\"required_set_id_count\":").append(row.requiredSetIds()).append(",\"set_row_count\":").append(row.setRows())
                .append(",\"combo_row_count\":").append(row.comboRows()).append(",\"evaluated_at_millis\":").append(row.evaluatedAtMillis())
                .append(",\"active_set_count\":").append(row.evaluation().activeSets()).append(",\"active_combo_count\":").append(row.evaluation().activeCombos())
                .append(",\"armor_count\":").append(row.evaluation().armorRows()).append(",\"vip_count\":").append(row.evaluation().vipRows())
                .append(",\"card_effect_count\":").append(row.evaluation().cardRows()).append(",\"modifiers\":{");
        boolean first = true;
        for (Stat stat : Stat.values()) {
            if (!first) out.append(','); first = false;
            out.append('"').append(stat.name()).append("\":").append(row.evaluation().total(stat));
        }
        return out.append("},\"magic_reduction_base\":").append(row.evaluation().magicReductionBase()).append('}').toString();
    }
    static String renderFailure(int sourceId, long objectId, Audit audit) {
        return "{\"phase\":\"equipment-read\",\"status\":\"FAILED\",\"source_id\":"+sourceId+",\"object_id\":"+objectId
                +",\"stage\":\""+audit.stage.name()+"\",\"reason\":\""+audit.failure.name()+"\"}";
    }
    private static SQLException bad(Audit audit, Failure failure) {
        audit.failure = failure; return new SQLException("bounded equipment probe failed");
    }
    private static void recordBackendFailure(Audit audit, ModernBackendExecutor.BackendException failure) {
        if (audit.failure == Failure.NONE || failure.failure() != ModernBackendExecutor.Failure.BACKEND) {
            audit.failure = Failure.valueOf(failure.failure().name());
        }
    }
    private static boolean awaitQuiescent(ModernBackendExecutor executor, long millis) throws InterruptedException {
        long end = System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(millis);
        while (!executor.isQuiescent()) {
            if (System.nanoTime()-end >= 0) return false;
            Thread.sleep(5);
        }
        return true;
    }
}

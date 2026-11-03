package local.lito381.server;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Explicit SELECT-only diagnosis of equipped custom executors. Uses the production physical
 * qualifier, without opening the private equipment store or executing any item/attack code.
 * Output contains catalog identifiers, validated ASCII class tokens and aggregate counts only.
 * Executor arguments, account/character names, character/item object identifiers and DB endpoints
 * are never included. SQL-equipped flags are inspected; this is not a private-override audit.
 */
public final class ModernCustomEquipmentReadProbe {
    private static final long AUDITED_VIP_CATALOG = 300086;
    // Fixed identifiers only. All numeric NULLs remain explicit in diagnostic output.
    private static final String[][] VIP_COLUMNS = {
        {"type", "類型"}, {"str", "力量"}, {"dex", "敏捷"}, {"con", "體質"},
        {"int", "智力"}, {"wis", "精神"}, {"cha", "魅力"}, {"ac", "防禦"},
        {"hp", "血量"}, {"mp", "魔量"}, {"hpr", "回血"}, {"mpr", "回魔"},
        {"melee_damage", "近戰傷害"}, {"melee_hit", "近戰命中"},
        {"ranged_damage", "遠攻傷害"}, {"ranged_hit", "遠攻命中"},
        {"damage_reduction", "傷害減免"}, {"magic_reduction", "魔法傷害減免"},
        {"mr", "抗魔"}, {"sp", "魔攻"}, {"fire", "火屬性防禦"}, {"wind", "風屬性防禦"},
        {"earth", "地屬性防禦"}, {"water", "水屬性防禦"}, {"stun", "昏迷耐性"},
        {"stone", "石化耐性"}, {"sleep", "睡眠耐性"}, {"freeze", "寒冰耐性"},
        {"sustain", "支撐耐性"}, {"blind", "暗黑耐性"}, {"exp_rate", "經驗獲取率"},
        {"adena_rate", "金幣獲取率"}, {"effect_icon", "狀態圖編號"}, {"poly_id", "變身編號"}
    };
    private ModernCustomEquipmentReadProbe() {}

    private record TimedItem(long objectId, String deadlineBucket, String remainingBucket) {
        @Override public String toString() { return "TimedItemAudit[redacted]"; }
    }
    private record VipDetails(String policyJson, List<TimedItem> items) {
        VipDetails { items = List.copyOf(items); }
        @Override public String toString() { return "VipDetails[redacted]"; }
    }

    /** Executed within inspectOwned's same read-only physical connection and connection lease. */
    private static VipDetails readVipDetails(Connection connection, ModernBackendScope scope,
            String catalog, ModernEquipmentReadProbe.Plan plan) throws Exception {
        if (catalog == null || !catalog.matches("[A-Za-z0-9_]{1,64}")) throw invalid();
        scope.checkActive();
        // Full production supplement is observed, not applied to saved stats.
        var fullVip = ModernVipEquipmentJdbc.read(connection,scope,plan.sourceId(),catalog,List.of(AUDITED_VIP_CATALOG));
        var fullRow = fullVip.vips().get(AUDITED_VIP_CATALOG);
        var fullPoly = fullRow.polymorphId()==0?null:fullVip.polymorphs().get(fullRow.polymorphId());
        String prefix = "`" + catalog + "`.";
        StringBuilder sql = new StringBuilder("SELECT v.`道具編號` AS catalog_id,t.itemid AS timed_catalog,t.h AS limit_hours");
        for (String[] field : VIP_COLUMNS) sql.append(",v.`").append(field[1]).append("` AS `").append(field[0]).append('`');
        sql.append(" FROM ").append(prefix).append("`道具_自訂穿脫能力` v LEFT JOIN ")
                .append(prefix).append("`道具_物品時間限制` t ON t.itemid=v.`道具編號` WHERE v.`道具編號`=? LIMIT 2");
        String policy;
        try (var query = prepare(connection, scope, sql.toString(), 2)) {
            query.setLong(1, AUDITED_VIP_CATALOG);
            try (var rows = query.executeQuery()) {
                if (!rows.next() || ModernJdbcNumbers.required(rows, "catalog_id") != AUDITED_VIP_CATALOG) throw invalid();
                StringBuilder values = new StringBuilder("{\"catalog_id\":").append(AUDITED_VIP_CATALOG);
                for (String[] field : VIP_COLUMNS) {
                    var value = ModernJdbcNumbers.nullable(rows, field[0]);
                    values.append(",\"").append(field[0]).append("\":");
                    if (value.isPresent()) values.append(value.getAsLong()); else values.append("null");
                }
                var timedCatalog = ModernJdbcNumbers.nullable(rows, "timed_catalog");
                var hours = ModernJdbcNumbers.nullable(rows, "limit_hours");
                if (timedCatalog.isPresent() && timedCatalog.getAsLong() != AUDITED_VIP_CATALOG
                        || timedCatalog.isEmpty() && hours.isPresent()) throw invalid();
                values.append(",\"time_rule_present\":").append(timedCatalog.isPresent()).append(",\"time_rule_hours\":");
                if (hours.isPresent()) values.append(hours.getAsLong()); else values.append("null");
                values.append(",\"full_vip_ability_fields\":").append(fullRow.abilities().values().size())
                        .append(",\"poly_ability_row_present\":").append(fullPoly!=null&&fullPoly.present())
                        .append(",\"current_configured_armor_contribution\":").append(fullVip.configuredArmorContribution(AUDITED_VIP_CATALOG))
                        .append(",\"saved_armor_provenance_verified\":false,\"poly_applied_abilities\":{");
                boolean firstPolyField=true;
                if(fullPoly!=null)for(var field:ModernVipEquipment.Field.values()) {
                    if(!firstPolyField)values.append(',');firstPolyField=false;
                    values.append('"').append(field.name()).append("\":").append(fullPoly.abilities().get(field));
                }
                values.append('}');
                policy = values.append('}').toString();
                if (rows.next()) throw invalid();
            }
        }
        scope.checkActive();
        // Bucket against the DB clock, without exporting deadlines or relying on JVM timestamp zones.
        String itemsSql = "SELECT i.id AS item_object,i.remaining_time,t.itemr_obj_id AS timed_object,"
                + "CASE WHEN t.usertime IS NULL THEN 0 WHEN t.usertime<=CURRENT_TIMESTAMP THEN 1 ELSE 2 END AS deadline_bucket"
                + " FROM characters c JOIN character_items i ON i.char_id=c.objid"
                + " LEFT JOIN character_items_time t ON t.itemr_obj_id=i.id"
                + " WHERE c.objid=? AND c.account_name=? AND c.Banned=0 AND i.item_id=? AND i.is_equipped=1"
                + " ORDER BY i.id LIMIT 513";
        var items = new ArrayList<TimedItem>();
        try (var query = prepare(connection, scope, itemsSql, 513)) {
            query.setLong(1, plan.objectId()); query.setString(2, plan.canonical()); query.setLong(3, AUDITED_VIP_CATALOG);
            try (var rows = query.executeQuery()) {
                long previous = 0;
                while (rows.next()) {
                    scope.checkActive();
                    long objectId = ModernJdbcNumbers.required(rows, "item_object");
                    if (objectId <= previous || items.size() == 512) throw invalid();
                    previous = objectId;
                    var timedObject = ModernJdbcNumbers.nullable(rows, "timed_object");
                    long bucket = ModernJdbcNumbers.required(rows, "deadline_bucket");
                    if (bucket < 0 || bucket > 2 || timedObject.isEmpty() != (bucket == 0)
                            || timedObject.isPresent() && timedObject.getAsLong() != objectId) throw invalid();
                    var remaining = ModernJdbcNumbers.nullable(rows, "remaining_time");
                    items.add(new TimedItem(objectId, bucket == 0 ? "absent" : bucket == 1 ? "expired" : "future",
                            remaining.isEmpty() ? "null" : remaining.getAsLong() < 0 ? "negative"
                                    : remaining.getAsLong() == 0 ? "zero" : "positive"));
                }
            }
        }
        scope.checkActive();
        return new VipDetails(policy, items);
    }

    private static PreparedStatement prepare(Connection connection, ModernBackendScope scope,
            String sql, int maximumRows) throws Exception {
        scope.checkActive();
        PreparedStatement statement = connection.prepareStatement(sql);
        try {
            statement.setMaxRows(maximumRows);
            statement.setQueryTimeout((int) Math.max(1L, ((long) scope.remainingMillis() + 999L) / 1000L));
            scope.checkActive();
            return statement;
        } catch (Exception failure) {
            try { statement.close(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }
    private static SQLException invalid() { return new SQLException("bounded custom equipment audit unavailable"); }

    private record Group(long catalogId, String kind, String classToken, int argumentCount)
            implements Comparable<Group> {
        @Override public int compareTo(Group other) {
            int result = Long.compare(catalogId, other.catalogId);
            if (result == 0) result = kind.compareTo(other.kind);
            if (result == 0) result = classToken.compareTo(other.classToken);
            return result == 0 ? Integer.compare(argumentCount, other.argumentCount) : result;
        }
    }

    private static Group group(long catalogId, ModernPhysicalCombat.TemplatePolicy policy) {
        // Match ItemClass.addList's literal ASCII-space split, but never print arguments.
        String[] parts = policy.className().split(" ");
        String token = parts.length == 0 ? "" : parts[0];
        if (!token.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*")) {
            token = "REDACTED_INVALID_CLASS_TOKEN";
        }
        return new Group(catalogId, policy.kind().name(), token, Math.max(0, parts.length - 1));
    }

    public static void main(String[] args) {
        int exit = 2;
        char[] password = null;
        String stage = "CONFIGURATION";
        try {
            if (args.length != 1) throw new IllegalArgumentException("explicit configuration required");
            Path file = Path.of(args[0]);
            String account = System.getenv("LITO_MODERN_PROBE_ACCOUNT");
            String secret = System.getenv("LITO_MODERN_PROBE_PASSWORD");
            if (account == null || secret == null) throw new IllegalArgumentException("explicit probe credentials required");
            password = secret.toCharArray();
            try (var settings = ModernDatabaseReadProbe.load(file);
                 var config = ModernDatabaseReadProbe.modernConfig(settings, ModernCharacterDataReadProbe.catalogDatabase(file));
                 var executor = new ModernBackendExecutor(1, 1);
                 var gate = executor.openSession()) {
                try {
                    long characters = 0, customCharacters = 0, customItems = 0, effectiveCustomItems = 0, customReasons = 0;
                    var groups = new TreeMap<Group, Long>();
                    var effectiveGroups = new TreeMap<Group, Long>();
                    var vipPolicies = new TreeMap<String, Long>();
                    var deadlines = new TreeMap<String, Long>();
                    var remainingValues = new TreeMap<String, Long>();
                    for (var source : config.sources()) {
                        var audit = new ModernEquipmentReadProbe.Audit();
                        var currentPlan = new AtomicReference<ModernEquipmentReadProbe.Plan>();
                        var currentVip = new AtomicReference<VipDetails>();
                        var dao = new ModernJdbcBackend(source.database(), config.dbConnectMillis(), config.dbNetworkMillis(),
                                (url, properties) -> ModernEquipmentReadProbe.inspectOwned(
                                        new com.mysql.cj.jdbc.Driver().connect(url, properties), ModernBackendScope.current(),
                                        source.database().database(), source.catalogDatabase(), config.dbNetworkMillis(), currentPlan.get(), audit,
                                        (connection, scope, catalog, plan, ignored) -> {
                                            currentVip.set(readVipDetails(connection, scope, catalog, plan));
                                            return null;
                                        }));
                        var verifier = ModernAccountAuthentication.repositories(Map.of(source.id(),
                                ModernAccountAuthentication.SourceBinding.direct(source.name(), dao::authenticate)), ignored -> false);
                        stage = "AUTHENTICATION";
                        var principal = ModernEquipmentRepositoryReadProbe.authenticate(gate, verifier, account, password);
                        stage = "ROSTER";
                        var rows = gate.call(5000, scope -> dao.characters(principal.accountForSource(source.id())));
                        var provider = ModernEquipmentRepository.physicalJdbc(Map.of(source.id(),
                                new ModernEquipmentRepository.Source(source, dao)));
                        for (var row : rows) {
                            stage = "PHYSICAL_SOURCE_QUALIFICATION";
                            currentVip.set(null);
                            currentPlan.set(new ModernEquipmentReadProbe.Plan(source.id(), principal.accountForSource(source.id()), row));
                            ModernEquipmentAuthority.PhysicalLoaded loaded;
                            try { loaded = gate.call(5000, scope -> provider.read(principal, source.id(), row.objectId())); }
                            finally { currentPlan.set(null); }
                            var data = loaded.source();
                            if (data.input().equipment().source().sourceId() != source.id()
                                    || !data.input().equipment().source().character().equals(row)) {
                                throw new IllegalStateException("source changed");
                            }
                            var binding = new ModernEquipmentReconciliation.ExpectedBinding(
                                    new TournamentPresence.CharacterKey(source.id(), row.objectId()), loaded.ownerBinding());
                            stage = "PURE_QUALIFICATION";
                            var fighter = ModernPhysicalCombat.qualify(data, binding, Optional.empty(), System.currentTimeMillis());
                            if (fighter.profile().equipmentRevision() != 0) throw new IllegalStateException("unexpected override revision");
                            var vip = currentVip.get();
                            // The SELECT above inspected raw SQL flags, not the
                            // effective mask after expiry. Compare like with like.
                            var expectedVipObjects = sqlEquippedVipObjects(fighter.profile().source());
                            if (vip == null || !expectedVipObjects.equals(vip.items().stream().map(TimedItem::objectId).toList())) {
                                throw new IllegalStateException("VIP source changed between read-only statements");
                            }
                            if (!vip.items().isEmpty()) {
                                vipPolicies.merge(vip.policyJson(), (long) vip.items().size(), Math::addExact);
                                if (vipPolicies.size() > 64) throw invalid();
                                for (var item : vip.items()) {
                                    deadlines.merge(item.deadlineBucket(), 1L, Math::addExact);
                                    remainingValues.merge(item.remainingBucket(), 1L, Math::addExact);
                                }
                            }
                            characters++;
                            if (fighter.unsupportedReason() == ModernPhysicalCombat.UnsupportedReason.CUSTOM_EXECUTOR) customReasons++;
                            boolean anyCustom = false;
                            for (var flag : fighter.profile().equipped()) {
                                var item = data.input().equipment().items().get(flag.objectId());
                                if (!item.source().equipped()) continue;
                                long catalogId = item.source().template().catalogId();
                                var policy = data.templates().get(catalogId);
                                if (policy.custom()) {
                                    anyCustom = true;
                                    customItems++;
                                    groups.merge(group(catalogId, policy), 1L, Math::addExact);
                                    if (flag.equipped()) {
                                        effectiveCustomItems++;
                                        effectiveGroups.merge(group(catalogId,policy),1L,Math::addExact);
                                    }
                                    if (groups.size() > 256) throw new IllegalStateException("aggregate limit exceeded");
                                }
                            }
                            if (anyCustom) customCharacters++;
                        }
                    }
                    if (characters == 0) throw new IllegalStateException("no character verified");
                    stage = "RETIREMENT";
                    executor.close();
                    if (!executor.awaitTermination(5000)) throw new IllegalStateException("worker not retired");
                    StringBuilder result = new StringBuilder("{\"phase\":\"custom-equipment-audit\",\"status\":\"OK\",\"sources\":")
                            .append(config.sources().size()).append(",\"characters\":").append(characters)
                            .append(",\"custom_characters\":").append(customCharacters)
                            .append(",\"custom_executor_rejections\":").append(customReasons)
                            .append(",\"equipped_custom_items\":").append(customItems)
                            .append(",\"equipped_custom_items_kind\":\"source_sql_flags\",\"effective_equipped_custom_items\":").append(effectiveCustomItems)
                            .append(",\"source_baseline_only_custom_items\":").append(customItems-effectiveCustomItems).append(",\"groups\":[");
                    boolean comma = false;
                    for (var entry : groups.entrySet()) {
                        if (comma) result.append(',');
                        comma = true;
                        Group group = entry.getKey();
                        result.append("{\"catalog_id\":").append(group.catalogId())
                                .append(",\"kind\":\"").append(group.kind()).append("\",\"executor\":\"")
                                .append(group.classToken()).append("\",\"argument_count\":").append(group.argumentCount())
                                .append(",\"count\":").append(entry.getValue())
                                .append(",\"effective_count\":").append(effectiveGroups.getOrDefault(group,0L)).append('}');
                    }
                    result.append("],\"vip_policy_variants\":[");
                    comma = false;
                    for (var entry : vipPolicies.entrySet()) {
                        if (comma) result.append(','); comma = true;
                        result.append("{\"equipped_observations\":").append(entry.getValue()).append(",\"fields\":").append(entry.getKey()).append('}');
                    }
                    result.append("],\"vip_deadline_buckets\":").append(renderCounts(deadlines))
                            .append(",\"vip_remaining_time_buckets\":").append(renderCounts(remainingValues))
                            .append(",\"arguments_redacted\":true,\"sql_equipped_only\":true,\"store_opened\":false,\"executor_invoked\":false,\"atomic_generation\":false}");
                    System.out.println(result);
                    exit = 0;
                } finally {
                    executor.close();
                    if (!executor.awaitTermination(5000)) throw new IllegalStateException("worker not retired");
                }
            }
        } catch (Exception failure) {
            String reason = failure instanceof ModernBackendExecutor.BackendException backend
                    ? backend.failure().name() : "INVALID_RESULT_OR_SETUP";
            System.out.println("{\"phase\":\"custom-equipment-audit\",\"status\":\"FAILED\",\"stage\":\""
                    + stage + "\",\"reason\":\"" + reason + "\"}");
        } finally {
            if (password != null) Arrays.fill(password, '\0');
        }
        if (exit != 0) System.exit(exit);
    }

    static List<Long> sqlEquippedVipObjects(ModernCharacterData.Snapshot source) {
        return source.inventory().stream().filter(item -> item.equipped()
                        && item.template().catalogId() == AUDITED_VIP_CATALOG)
                .map(ModernCharacterData.InventoryItem::objectId).sorted().toList();
    }

    private static String renderCounts(Map<String, Long> counts) {
        StringBuilder output = new StringBuilder("{");
        boolean comma = false;
        for (var entry : counts.entrySet()) {
            if (comma) output.append(','); comma = true;
            output.append('"').append(entry.getKey()).append("\":").append(entry.getValue());
        }
        return output.append('}').toString();
    }
}

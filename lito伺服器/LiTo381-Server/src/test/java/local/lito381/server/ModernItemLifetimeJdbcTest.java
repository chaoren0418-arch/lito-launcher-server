package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernEquipmentSourceJdbcTest.scoped;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ModernItemLifetimeJdbcTest {
    private static final String DB_NOW = "2026-09-15 12:34:56.123456";
    private static final long START_EPOCH = 1_800_000_000_000L;
    private static final long START_NANOS = -123_456_789L; // System.nanoTime's origin/sign is unspecified.

    @Test void oneBoundedReadonlyStatementBindsActualOwnerAndPreservesFullI64Identity() throws Exception {
        var row = item(0x1_0000_0000L, "weapon"); row.put("weapon_maximum", Long.MAX_VALUE);
        var expected = expected(List.of(row)); var jdbc = jdbc(List.of(row));
        var observation = read(jdbc, expected);
        assertEquals(7, observation.sourceId()); assertEquals(3333, observation.objectId());
        assertEquals(DB_NOW, observation.dbNowLocal()); assertEquals(START_EPOCH, observation.queryStartedEpochMillis());
        assertEquals(START_NANOS, observation.queryStartedNanos());
        assertEquals(1000, observation.dbClockUncertaintyMillis());
        assertEquals(Long.MAX_VALUE, observation.items().get(0x1_0000_0000L).templateMaxUseSeconds());
        assertEquals(1, jdbc.prepares); assertEquals(1, jdbc.executions); assertEquals(0, jdbc.metadataPrepares);
        assertEquals(Map.of(1, 3333L, 2, "owner"), jdbc.parameters);
        assertTrue(jdbc.sql.startsWith("SELECT ")); assertTrue(jdbc.sql.contains("CAST(CURRENT_TIMESTAMP AS CHAR)"));
        assertFalse(jdbc.sql.contains("CURRENT_TIMESTAMP("));
        assertTrue(jdbc.sql.contains("LEFT JOIN character_items_time t ON t.itemr_obj_id=i.id"));
        assertTrue(jdbc.sql.contains("c.account_name=? AND c.Banned=0"));
        assertFalse(jdbc.sql.contains("UNIX_TIMESTAMP")); assertFalse(jdbc.sql.contains("CONVERT_TZ"));
        assertFalse(jdbc.sql.contains("FOR UPDATE")); assertFalse(jdbc.sql.contains(" SET "));
        assertThrows(UnsupportedOperationException.class, () -> observation.items().clear());
        jdbc.assertClosed();
    }

    @Test void threeTemplateKindsMustMatchAndEtcMaximumZeroIsTheTracedConstructorValue() throws Exception {
        var weapon = item(1, "weapon"); weapon.put("weapon_maximum", 3600L);
        var armor = item(2, "armor"); armor.put("armor_maximum", 7200L);
        var etc = item(3, "etc");
        var rows = List.of(weapon, armor, etc); var jdbc = jdbc(rows);
        var observation = read(jdbc, expected(rows));
        assertEquals(3600, observation.items().get(1L).templateMaxUseSeconds());
        assertEquals(7200, observation.items().get(2L).templateMaxUseSeconds());
        assertEquals(0, observation.items().get(3L).templateMaxUseSeconds());
        assertFalse(observation.items().get(3L).lighting()); jdbc.assertClosed();
    }

    @Test void selectedNullableRemainingAndRuleHoursStayUnknownRatherThanPermanentZero() throws Exception {
        var row = item(1, "armor"); row.put("remaining_time", null);
        row.put("rule_catalog", 1001L); row.put("rule_hours", null);
        var jdbc = jdbc(List.of(row)); var fact = read(jdbc, expected(List.of(row))).items().get(1L);
        assertEquals(OptionalLong.empty(), fact.remainingSeconds());
        assertTrue(fact.timeRulePresent()); assertEquals(OptionalLong.empty(), fact.timeRuleHours());
        assertEquals(Optional.empty(), fact.absoluteLocalTime()); jdbc.assertClosed();
    }

    @Test void positiveRemainingAndConfiguredButUnstartedRulesAreRetainedWithoutInventingDeadlines() throws Exception {
        var relative = item(1, "weapon"); relative.put("remaining_time", Long.MAX_VALUE);
        var rule = item(2, "armor"); rule.put("rule_catalog", 1002L); rule.put("rule_hours", 720L);
        var rows = List.of(relative, rule); var jdbc = jdbc(rows);
        var observation = read(jdbc, expected(rows));
        assertEquals(OptionalLong.of(Long.MAX_VALUE), observation.items().get(1L).remainingSeconds());
        assertEquals(Optional.empty(), observation.items().get(1L).absoluteLocalTime());
        assertEquals(OptionalLong.of(720), observation.items().get(2L).timeRuleHours());
        assertEquals(Optional.empty(), observation.items().get(2L).absoluteLocalTime()); jdbc.assertClosed();
    }

    @Test void secondGranularityClockDeductsOneFullSecondBeforePublishingAnyDeadline() throws Exception {
        var first=item(1,"armor");text(first,"database_now","2026-09-15 12:34:56");deadline(first,"2026-09-15 12:34:57");
        var second=item(2,"armor");text(second,"database_now","2026-09-15 12:34:56");deadline(second,"2026-09-15 12:34:58");
        var rows=List.of(first,second);var jdbc=jdbc(rows);var observation=read(jdbc,expected(rows));
        assertEquals(1000,observation.dbClockUncertaintyMillis());
        assertEquals(START_EPOCH,observation.view(1,START_EPOCH).expiresAtMillis());
        assertEquals(ModernItemLifetime.State.EXPIRED,observation.view(1,START_EPOCH).state());
        assertEquals(ModernItemLifetime.State.ACTIVE,observation.view(2,START_EPOCH+999).state());
        assertEquals(ModernItemLifetime.State.EXPIRED,observation.view(2,START_EPOCH+1000).state());
        jdbc.assertClosed();
    }

    @Test void expiredAndFutureAbsoluteTimesRemainCanonicalEvenWhenRemainingIsZero() throws Exception {
        var expired = item(1, "etc"); deadline(expired, "2020-01-02 03:04:05");
        var future = item(2, "armor"); deadline(future, "2029-01-02 03:04:05.12");
        var rows = List.of(expired, future); var jdbc = jdbc(rows);
        var observation = read(jdbc, expected(rows));
        assertEquals(Optional.of("2020-01-02 03:04:05.000000"), observation.items().get(1L).absoluteLocalTime());
        assertEquals(Optional.of("2029-01-02 03:04:05.120000"), observation.items().get(2L).absoluteLocalTime());
        assertEquals(OptionalLong.of(0), observation.items().get(1L).remainingSeconds()); jdbc.assertClosed();
    }

    @Test void lightingIsExplicitAndEveryKnownEtcKindIsRecognizedWithoutUnknownTypeFallback() throws Exception {
        for (String type : List.of("arrow", "wand", "light", "gem", "totem", "firecracker", "potion", "food",
                "scroll", "questitem", "spellbook", "petitem", "other", "material", "event", "sting", "treasure_box")) {
            var row = item(1, "etc"); text(row, "etc_type", type); var jdbc = jdbc(List.of(row));
            assertEquals(type.equals("light"), read(jdbc, expected(List.of(row))).items().get(1L).lighting()); jdbc.assertClosed();
        }
        for (String type : new String[] {null, "", "OTHER", "unknown", "other ", "light\n", "a".repeat(33)}) {
            var row = item(1, "etc"); var expected = expected(List.of(row)); text(row, "etc_type", type);
            rejected(jdbc(List.of(row)), expected);
        }
    }

    @Test void emptyInventoryStillRequiresOneOwnerAndDatabaseClockRow() throws Exception {
        var row = empty(); var jdbc = jdbc(List.of(row));
        assertTrue(read(jdbc, expected(List.of())).items().isEmpty()); jdbc.assertClosed();
        rejected(jdbc(List.of()), expected(List.of()));
        rejected(jdbc(List.of(row, row)), expected(List.of()));
        row = empty(); row.put("timed_object", 1L); rejected(jdbc(List.of(row)), expected(List.of()));
    }

    @Test void exactly512InstancesAreCompleteAnd513CannotTruncateIntoSuccess() throws Exception {
        var rows = new ArrayList<Map<String, Object>>();
        for (int index = 0; index < 512; index++) rows.add(item(0x1_0000_0000L + index, "armor"));
        var expected = expected(rows); var jdbc = jdbc(rows);
        assertEquals(512, read(jdbc, expected).items().size()); jdbc.assertClosed();
        rows.add(item(0x1_0000_0200L, "armor")); rejected(jdbc(rows), expected);
    }

    @Test void duplicateMissingReorderedAndExtraItemsRejectTheEntireObservation() {
        var first = item(1, "weapon"); var second = item(2, "armor"); var expected = expected(List.of(first, second));
        for (var rows : List.of(List.of(first, first), List.of(second, first), List.of(first),
                List.of(first, second, item(3, "etc")))) rejected(jdbc(rows), expected);
    }

    @Test void ownerCatalogEquippedAndKindMustEqualTheAlreadyQualifiedSourceSnapshot() {
        for (String field : List.of("owner_object", "catalog_id", "is_equipped")) {
            var row = item(1, "weapon"); var expected = expected(List.of(row));
            row.put(field, field.equals("is_equipped") ? 0L : 9999L); rejected(jdbc(List.of(row)), expected);
        }
        var row = item(1, "weapon"); var expected = expected(List.of(row));
        row.put("weapon_catalog", null); row.put("armor_catalog", 1001L); row.put("armor_maximum", 0L);
        rejected(jdbc(List.of(row)), expected);
    }

    @Test void missingAmbiguousOrDifferentCatalogTemplateIsNotAnAbsentTimeRule() {
        for (String failure : List.of("missing", "ambiguous", "wrong")) {
            var row = item(1, "weapon"); var expected = expected(List.of(row));
            if (failure.equals("missing")) row.put("weapon_catalog", null);
            if (failure.equals("ambiguous")) row.put("armor_catalog", 1001L);
            if (failure.equals("wrong")) row.put("weapon_catalog", 9999L);
            rejected(jdbc(List.of(row)), expected);
        }
    }

    @Test void requiredAndMissingProjectedColumnsNeverBecomeZeroOrPermanent() {
        for (String field : List.of("remaining_time", "rule_hours", "timed_object", "item_deadline", "weapon_maximum")) {
            var row = item(1, "weapon"); var expected = expected(List.of(row)); row.remove(field);
            rejected(jdbc(List.of(row)), expected);
        }
        for (String field : List.of("owner_object", "catalog_id", "is_equipped", "weapon_maximum", "database_now")) {
            var row = item(1, "weapon"); var expected = expected(List.of(row)); row.put(field, null);
            rejected(jdbc(List.of(row)), expected);
        }
    }

    @Test void numericFieldsRejectFractionApproximateTextOverflowAndNegativeDurations() {
        for (String field : List.of("remaining_time", "weapon_maximum", "rule_hours")) {
            for (Object invalid : List.of(new BigDecimal("0.1"), 1.0, 1.0f, "1", -1L,
                    BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE))) {
                var row = item(1, "weapon"); row.put("rule_catalog", 1001L); row.put("rule_hours", 0L);
                var expected = expected(List.of(row)); row.put(field, invalid); rejected(jdbc(List.of(row)), expected);
            }
        }
    }

    @Test void strictBooleanAndRuleJoinKeysRejectContradictions() {
        for (long equipped : new long[] {-1, 2, Long.MAX_VALUE}) {
            var row = item(1, "armor"); var expected = expected(List.of(row)); row.put("is_equipped", equipped);
            rejected(jdbc(List.of(row)), expected);
        }
        var row = item(1, "armor"); var expected = expected(List.of(row)); row.put("rule_hours", 1L);
        rejected(jdbc(List.of(row)), expected);
        row = item(1, "armor"); row.put("rule_catalog", 9999L); row.put("rule_hours", 1L);
        rejected(jdbc(List.of(row)), expected);
    }

    @Test void absoluteDeadlineRequiresMatchingTimeRowAndCannotAcceptNullTimestampAsAbsence() {
        for (String failure : List.of("wrong-key", "missing-key", "null-date")) {
            var row = item(1, "etc"); var expected = expected(List.of(row)); deadline(row, "2026-01-01 00:00:00");
            if (failure.equals("wrong-key")) row.put("timed_object", 2L);
            if (failure.equals("missing-key")) row.put("timed_object", null);
            if (failure.equals("null-date")) text(row, "item_deadline", null);
            rejected(jdbc(List.of(row)), expected);
        }
    }

    @Test void exactLocalDateGrammarCanonicalizesMicrosecondsWithoutAnyJvmTimezoneConversion() throws Exception {
        for (String input : List.of("0001-01-01 00:00:00", "2024-02-29 12:34:56.1", "9999-12-31 23:59:59.999999")) {
            String result = ModernItemLifetimeJdbc.canonical(ModernItemLifetimeJdbc.localDateTime(input));
            assertEquals(26, result.length()); assertEquals(result, ModernItemLifetimeJdbc.canonical(ModernItemLifetimeJdbc.localDateTime(result)));
        }
        assertEquals("2024-02-29 12:34:56.100000", ModernItemLifetimeJdbc.canonical(ModernItemLifetimeJdbc.localDateTime("2024-02-29 12:34:56.1")));
    }

    @Test void malformedZeroDatesOffsetTextUnicodeLeapErrorsAndExcessPrecisionReject() {
        for (String input : new String[] {null, "", "0000-00-00 00:00:00", "0000-01-01 00:00:00", "2023-02-29 00:00:00",
                "2024-13-01 00:00:00", "2024-01-01 24:00:00", "2024-01-01 00:00:60", "2024-01-01T00:00:00",
                "2024-01-01 00:00:00Z", "2024-01-01 00:00:00+08:00", "2024-01-01 00:00:00.",
                "2024-01-01 00:00:00.1234567", "２０２４-01-01 00:00:00", "2024-01-01 00:00:00\n"}) {
            assertThrows(SQLException.class, () -> ModernItemLifetimeJdbc.localDateTime(input));
        }
    }

    @Test void textTypeLengthAndNullEvidenceAreValidatedBeforeDateOrEtcInterpretation() {
        for (String field : List.of("database_now", "item_deadline", "etc_type")) {
            var row = item(1, "etc"); deadline(row, "2026-01-01 00:00:00"); var expected = expected(List.of(row));
            var jdbc = jdbc(List.of(row)); jdbc.numericTypes.put(field, Types.TIMESTAMP); rejected(jdbc, expected);
            row = item(1, "etc"); deadline(row, "2026-01-01 00:00:00"); row.put(field + "_bytes", 999L);
            rejected(jdbc(List.of(row)), expected);
            row = item(1, "etc"); deadline(row, "2026-01-01 00:00:00"); row.put(field + "_bytes", null);
            rejected(jdbc(List.of(row)), expected);
        }
    }

    @Test void oneStatementClockMustBeIdenticalAcrossRowsNotDriftWithDriverOrFixture() {
        var first = item(1, "weapon"); var second = item(2, "armor"); var expected = expected(List.of(first, second));
        text(second, "database_now", "2026-09-15 12:34:56.123457"); rejected(jdbc(List.of(first, second)), expected);
    }

    @Test void queryStartClocksAreCapturedExactlyBeforeExecuteAndNotResampledAtCompletion() throws Exception {
        var row = item(1, "weapon"); var jdbc = jdbc(List.of(row)); var expected = expected(List.of(row));
        var calls = new AtomicInteger();
        var result = scoped(() -> ModernItemLifetimeJdbc.read(jdbc.connection(), ModernBackendScope.current(), expected, "owner", "catalog",
                () -> { assertEquals(0, jdbc.executions); assertEquals(0, calls.getAndIncrement()); return START_EPOCH; },
                () -> { assertEquals(0, jdbc.executions); assertEquals(1, calls.getAndIncrement()); return START_NANOS; }));
        assertEquals(2, calls.get()); assertEquals(START_EPOCH, result.queryStartedEpochMillis());
        jdbc.assertClosed();
    }

    @Test void cancellationDuringRowsIsNotWrappedAsPermanentOrPublishedPartialMap() {
        var rows = List.of(item(1, "weapon"), item(2, "armor")); var jdbc = jdbc(rows); jdbc.cancelAtRow = 2;
        var failure = assertThrows(ModernBackendExecutor.BackendException.class, () -> read(jdbc, expected(rows)));
        assertEquals(ModernBackendExecutor.Failure.CANCELLED, failure.failure()); jdbc.assertClosed();
    }

    @Test void queryAndCleanupFailuresKeepConnectionOwnedAndDoNotLeakDriverDetails() {
        var row = item(1, "weapon"); var expected = expected(List.of(row)); var query = jdbc(List.of(row)); query.executeFailure = true;
        safeError(assertThrows(SQLException.class, () -> read(query, expected)));
        assertTrue(query.statementClosed); assertFalse(query.resultClosed); assertFalse(query.connectionClosed);
        var cleanup = jdbc(List.of(row)); cleanup.closeFailure = true; rejected(cleanup, expected);
    }

    @Test void writableConnectionOrInvalidSourceArgumentsFailBeforePreparingAnyStatement() {
        var row = item(1, "weapon"); var expected = expected(List.of(row)); var writable = jdbc(List.of(row)); writable.readOnly = false;
        safeError(assertThrows(SQLException.class, () -> read(writable, expected))); assertEquals(0, writable.prepares);
        for (String catalog : new String[] {null, "", "catalog`;DELETE", "a.b", "a".repeat(65), "a b"}) {
            var jdbc = jdbc(List.of(row));
            assertThrows(SQLException.class, () -> scoped(() -> ModernItemLifetimeJdbc.read(jdbc.connection(), ModernBackendScope.current(), expected, "owner", catalog)));
            assertEquals(0, jdbc.prepares);
        }
        for (String owner : new String[] {null, "", " ", "x\n", "x".repeat(65)}) {
            var jdbc = jdbc(List.of(row));
            assertThrows(SQLException.class, () -> scoped(() -> ModernItemLifetimeJdbc.read(jdbc.connection(), ModernBackendScope.current(), expected, owner, "catalog")));
            assertEquals(0, jdbc.prepares);
        }
    }

    @Test void wrongScopeAndNegativeQueryEpochCannotStartARead() {
        var row = item(1, "weapon"); var expected = expected(List.of(row)); var jdbc = jdbc(List.of(row));
        assertThrows(SQLException.class, () -> scoped(() -> ModernItemLifetimeJdbc.read(jdbc.connection(),
                ModernEquipmentSourceJdbcTest.scope(30), expected, "owner", "catalog")));
        assertEquals(0, jdbc.prepares);
        var negative = jdbc(List.of(row));
        assertThrows(SQLException.class, () -> scoped(() -> ModernItemLifetimeJdbc.read(negative.connection(),
                ModernBackendScope.current(), expected, "owner", "catalog", () -> -1L, () -> 0L)));
        assertEquals(0, negative.executions); assertTrue(negative.statementClosed); assertFalse(negative.connectionClosed);
    }

    static Map<String, Object> empty() {
        var row = new HashMap<String, Object>(); row.put("owner_object", 3333L); text(row, "database_now", DB_NOW);
        for (String column : List.of("item_object", "catalog_id", "is_equipped", "remaining_time", "timed_object",
                "rule_catalog", "rule_hours", "weapon_catalog", "weapon_maximum", "armor_catalog", "armor_maximum", "etc_catalog")) row.put(column, null);
        text(row, "item_deadline", null); text(row, "etc_type", null); return row;
    }
    static Map<String, Object> item(long id, String kind) {
        var row = empty(); row.put("item_object", id); row.put("catalog_id", Math.addExact(id, 1000L));
        row.put("is_equipped", 1L); row.put("remaining_time", 0L); row.put(kind + "_catalog", Math.addExact(id, 1000L));
        if (kind.equals("etc")) text(row, "etc_type", "other"); else row.put(kind + "_maximum", 0L);
        return row;
    }
    private static void deadline(Map<String, Object> row, String date) {
        row.put("timed_object", row.get("item_object")); text(row, "item_deadline", date);
    }
    private static void text(Map<String, Object> row, String column, String value) {
        row.put(column, value); row.put(column + "_bytes", value == null ? null : (long) value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
    }
    private static ModernCharacterData.Snapshot expected(List<Map<String, Object>> rows) {
        var items = new ArrayList<ModernCharacterData.InventoryItem>();
        for (var row : rows) {
            if (row.get("item_object") == null) continue;
            var kind = row.get("weapon_catalog") != null ? ModernCharacterData.ItemKind.WEAPON
                    : row.get("armor_catalog") != null ? ModernCharacterData.ItemKind.ARMOR : ModernCharacterData.ItemKind.ETC;
            var template = new ModernCharacterData.ItemTemplate((Long) row.get("catalog_id"), kind, "$fixture", 1, 1, 1);
            items.add(new ModernCharacterData.InventoryItem((Long) row.get("item_object"), template, 1,
                    ((Long) row.get("is_equipped")) == 1, 0, 0, true, 1, false, 0));
        }
        return new ModernCharacterData.Snapshot(7, new ModernApplicationProtocol.CharacterRow(3333, "fixture", 0, 0, 70),
                new ModernCharacterData.PersistedValues(100, 50, 100, 50, -1, 12, 12, 12, 12, 12, 12, 0, 0, 12, 12, 12, 12, 12, 225), items);
    }
    private static ModernEquipmentSourceJdbcTest.Jdbc jdbc(List<Map<String, Object>> rows) {
        var jdbc = new ModernEquipmentSourceJdbcTest.Jdbc(rows);
        for (String field : List.of("database_now", "item_deadline", "etc_type")) jdbc.numericTypes.put(field, Types.VARCHAR);
        return jdbc;
    }
    private static ModernItemLifetime.Observation read(ModernEquipmentSourceJdbcTest.Jdbc jdbc, ModernCharacterData.Snapshot expected) throws Exception {
        return scoped(() -> ModernItemLifetimeJdbc.read(jdbc.connection(), ModernBackendScope.current(), expected,
                "owner", "catalog", () -> START_EPOCH, () -> START_NANOS));
    }
    private static void rejected(ModernEquipmentSourceJdbcTest.Jdbc jdbc, ModernCharacterData.Snapshot expected) {
        safeError(assertThrows(SQLException.class, () -> read(jdbc, expected))); jdbc.assertClosed();
    }
    private static void safeError(SQLException failure) {
        assertEquals("modern item lifetime unavailable", failure.getMessage()); assertNull(failure.getCause());
        assertNull(failure.getNextException()); assertEquals(0, failure.getSuppressed().length);
    }
}

package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.*;
import static local.lito381.server.ModernEquipmentFacts.Stat.*;
import static local.lito381.server.ModernEquipmentReadProbe.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ModernEquipmentReadProbeTest {
    private static final String SECRET = "synthetic-private-owner-and-item";
    private static final String DATABASE = "synthetic_source";
    private static final String CATALOG = "synthetic_catalog";
    private static final ModernApplicationProtocol.CharacterRow ROW =
            new ModernApplicationProtocol.CharacterRow(3333,SECRET,0,0,Long.MAX_VALUE);

    @Test void numericSummaryPreservesWideSignedModifiersAndLabelsNonProductionProvenance() throws Exception {
        long large = 4_294_967_299L;
        var item = ModernEquipmentEvaluatorTest.instance(1,100,ModernCharacterData.ItemKind.ARMOR,true,0,SECRET,
                List.of(),Ability.zero().with(STR,large).with(MAX_HP,large).with(AC,-large),Ability.zero(),
                ModernEquipmentEvaluatorTest.emptyCards(),Stone.none(),TemplatePhysical.zero());
        var qualified = ModernEquipmentEvaluatorTest.qualified(List.of(item));
        var before = qualified.source();
        var savedBefore = before.persisted();
        var reconciled = ModernEquipmentReconciliation.reconcile(qualified,syntheticBinding(7,3333),Optional.empty());
        assertTrue(reconciled.overrides().isEmpty()); assertTrue(reconciled.staleItems().isEmpty());
        assertEquals(ModernEquipmentOverrideStore.Expected.absent(),reconciled.expected());
        var evaluation = ModernEquipmentEvaluator.evaluate(qualified,reconciled.overrides(),1234);
        var summary = summarize(qualified,evaluation,1234);
        assertEquals(1,summary.items()); assertEquals(1,summary.equipped());
        assertEquals(3,summary.requiredLookups()); assertEquals(3,summary.lookupRows()); assertEquals(0,summary.presentLookups());
        String output = render(summary);
        assertTrue(output.contains("\"source_id\":7")); assertTrue(output.contains("\"object_id\":3333"));
        assertTrue(output.contains("\"STR\":4294967299")); assertTrue(output.contains("\"AC\":-4294967299"));
        assertTrue(output.contains(BINDING_KIND)); assertTrue(output.contains(CONSISTENCY));
        assertTrue(output.contains("traced_equipment_modifiers_not_live_player_stats"));
        assertTrue(output.contains("\"stored_override_count\":0"));
        assertFalse(output.contains(SECRET)); assertFalse(output.contains("fixture"));
        assertFalse(output.contains("catalogId")); assertFalse(output.contains("jdbc:"));
        assertSame(before,qualified.source()); assertSame(savedBefore,qualified.source().persisted());
        assertEquals(-900,qualified.source().persisted().armorClass());
        assertEquals(100,qualified.source().persisted().maxHp());
        assertEquals(1000,qualified.source().persisted().savedHp());
    }

    @Test void syntheticBindingIsExplicitlyNotAccountProvenanceAndPlansAreRedacted() throws Exception {
        var first = syntheticBinding(7,Long.MAX_VALUE); var second = syntheticBinding(42,Long.MAX_VALUE);
        assertNotEquals(first.key(),second.key());
        assertEquals(first.fingerprint(),second.fingerprint(),"fixed TEST_ONLY marker is not a production owner/config fingerprint");
        var plan = new Plan(7,SECRET,ROW);
        assertEquals(3333,plan.objectId()); assertSame(ROW,plan.expected());
        assertFalse(plan.toString().contains(SECRET));
        assertFalse(new SourcePlan(SECRET,List.of(ROW)).toString().contains(SECRET));
        assertThrows(IllegalArgumentException.class,() -> new Plan(0,SECRET,ROW));
        assertThrows(IllegalArgumentException.class,() -> new Plan(7,SECRET,null));
        assertThrows(IllegalArgumentException.class,() -> new Plan(7," ",ROW));
        assertThrows(IllegalArgumentException.class,() -> syntheticBinding(7,0));
    }

    @Test void expiredEquippedAndUnwornItemsAreReportedWithoutChangingSourceFlags() throws Exception {
        var original = ModernEquipmentEvaluatorTest.qualified(List.of(probeArmor(1,true),probeArmor(2,false),probeArmor(3,true)));
        var qualified = lifetime(original,Map.of(1L,"2026-09-29 00:00:00",2L,"2026-09-29 00:00:00"));
        var before = qualified.source();
        var reconciled = ModernEquipmentReconciliation.reconcile(qualified,syntheticBinding(7,3333),Optional.empty(),1000);
        validateUnstoredReconciliation(qualified,reconciled,1000);
        assertEquals(Map.of(1L,false,2L,false),reconciled.overrides());
        var summary = summarize(qualified,ModernEquipmentEvaluator.evaluate(qualified,reconciled.overrides(),1000),1000);
        assertEquals(2,summary.sourceEquipped()); assertEquals(1,summary.equipped());
        assertEquals(1,summary.suppressedEquipped()); assertEquals(2,summary.expiredItems());
        assertEquals(0,summary.unqualifiedItems()); assertEquals(25,summary.evaluation().total(MAX_HP));
        String output = render(summary);
        assertTrue(output.contains("\"equipped_count_kind\":\"effective_after_lifetime_guard\""));
        assertTrue(output.contains("\"source_equipped_count\":2"));
        assertTrue(output.contains("\"lifetime_suppressed_equipped_count\":1"));
        assertTrue(output.contains("\"expired_item_count\":2"));
        assertFalse(output.contains(SECRET));
        assertSame(before,qualified.source()); assertTrue(before.inventory().get(0).equipped());
        assertFalse(before.inventory().get(1).equipped());
    }

    @Test void unknownLifetimeIsSuppressedButNotMislabelledExpiredOrPermanent() throws Exception {
        var original = ModernEquipmentEvaluatorTest.qualified(List.of(probeArmor(1,true)));
        var qualified = new Qualified(original.source(),original.items(),original.catalog(),
                ModernItemLifetime.Observation.unqualified(original.source()));
        var reconciled = ModernEquipmentReconciliation.reconcile(qualified,syntheticBinding(7,3333),Optional.empty(),1000);
        validateUnstoredReconciliation(qualified,reconciled,1000);
        assertEquals(Map.of(1L,false),reconciled.overrides());
        var summary = summarize(qualified,ModernEquipmentEvaluator.evaluate(qualified,reconciled.overrides(),1000),1000);
        assertEquals(1,summary.sourceEquipped()); assertEquals(0,summary.equipped());
        assertEquals(1,summary.suppressedEquipped()); assertEquals(0,summary.expiredItems());
        assertEquals(1,summary.unqualifiedItems()); assertEquals(0,summary.evaluation().total(MAX_HP));
    }

    @Test void reconciliationAndEvaluationUseOneInstantAtExpiryBoundary() throws Exception {
        var original = ModernEquipmentEvaluatorTest.qualified(List.of(probeArmor(1,true)));
        var qualified = lifetime(original,Map.of(1L,"2026-09-30 00:00:01"));
        var active = ModernEquipmentReconciliation.reconcile(qualified,syntheticBinding(7,3333),Optional.empty(),1999);
        validateUnstoredReconciliation(qualified,active,1999);
        assertTrue(active.overrides().isEmpty());
        assertEquals(1,summarize(qualified,ModernEquipmentEvaluator.evaluate(qualified,active.overrides(),1999),1999).equipped());
        assertThrows(IllegalArgumentException.class,() -> validateUnstoredReconciliation(qualified,active,2000));
        var expired = ModernEquipmentReconciliation.reconcile(qualified,syntheticBinding(7,3333),Optional.empty(),2000);
        validateUnstoredReconciliation(qualified,expired,2000);
        assertEquals(0,summarize(qualified,ModernEquipmentEvaluator.evaluate(qualified,expired.overrides(),2000),2000).equipped());
        assertThrows(IllegalArgumentException.class,() -> validateUnstoredReconciliation(qualified,expired,1999));
    }

    @Test void missingLifetimeGuardOrRawEquippedEvaluationStillFailsInspection() throws Exception {
        var original = ModernEquipmentEvaluatorTest.qualified(List.of(probeArmor(1,true)));
        var expired = lifetime(original,Map.of(1L,"2026-09-29 00:00:00"));
        var unguarded = ModernEquipmentReconciliation.reconcile(original,syntheticBinding(7,3333),Optional.empty(),1000);
        assertThrows(IllegalArgumentException.class,() -> validateUnstoredReconciliation(expired,unguarded,1000));
        var rawEvaluation = ModernEquipmentEvaluator.evaluate(original,Map.of(),1000);
        assertThrows(IllegalArgumentException.class,() -> summarize(expired,rawEvaluation,1000));
    }

    private static Instance probeArmor(long objectId, boolean equipped) {
        return ModernEquipmentEvaluatorTest.instance(objectId,100+objectId,ModernCharacterData.ItemKind.ARMOR,equipped,0,SECRET,
                List.of(),Ability.zero().with(MAX_HP,25),Ability.zero(),ModernEquipmentEvaluatorTest.emptyCards(),
                Stone.none(),TemplatePhysical.zero());
    }
    private static Qualified lifetime(Qualified original, Map<Long,String> deadlines) {
        var facts = new TreeMap<Long,ModernItemLifetime.Fact>();
        for (var item : original.source().inventory()) {
            var deadline = Optional.ofNullable(deadlines.get(item.objectId()));
            facts.put(item.objectId(),new ModernItemLifetime.Fact(item.objectId(),item.template().catalogId(),item.template().kind(),
                    item.equipped(),OptionalLong.of(0),deadline.isPresent(),deadline.isPresent()?OptionalLong.of(720):OptionalLong.empty(),
                    deadline,0,false));
        }
        return new Qualified(original.source(),original.items(),original.catalog(),new ModernItemLifetime.Observation(
                original.source().sourceId(),original.source().character().objectId(),"2026-09-30 00:00:00",1000,0,facts));
    }

    @Test void inspectionUsesSameReadOnlyConnectionThenTransfersLeaseBackToOwnedDao() throws Exception {
        var jdbc = new Jdbc(); var audit = new Audit(); var calls = new AtomicInteger();
        scoped(() -> {
            var scope = ModernBackendScope.current();
            var result = inspectOwned(jdbc.connection,scope,DATABASE,CATALOG,3000,new Plan(7,SECRET,ROW),audit,
                    (connection, current, catalog, plan, state) -> {
                        calls.incrementAndGet();
                        assertSame(jdbc.connection,connection); assertSame(scope,current); assertSame(scope,ModernBackendScope.current());
                        assertTrue(connection.isReadOnly()); assertEquals(CATALOG,catalog); assertEquals(3333,plan.objectId());
                        assertEquals(Stage.ROUTING,state.stage);
                        return emptySummary();
                    });
            assertSame(jdbc.connection,result); assertFalse(jdbc.closed);
            assertEquals(1,calls.get()); assertNotNull(audit.summary); assertEquals(Failure.NONE,audit.failure);
            assertTrue(jdbc.networkMillis > 0 && jdbc.networkMillis <= 3000);
            assertEquals(2,jdbc.maximumRows); assertTrue(jdbc.querySeconds > 0 && jdbc.querySeconds <= 10);
            assertEquals(1,jdbc.statementCloses); assertEquals(1,jdbc.resultCloses);
            try (var transferred = scope.register(result)) { assertFalse(jdbc.closed); }
            result.close(); return null;
        });
        assertTrue(jdbc.closed); assertEquals(1,jdbc.closeCalls);
    }

    @Test void authAndRosterConnectionsOnlyCheckRoutingWithoutExecutingEquipmentPipeline() throws Exception {
        var jdbc = new Jdbc(); var audit = new Audit(); audit.stage = Stage.AUTHENTICATION;
        scoped(() -> {
            var returned = inspectOwned(jdbc.connection,ModernBackendScope.current(),DATABASE,CATALOG,3000,null,audit,
                    (connection,scope,catalog,plan,state) -> { throw new AssertionError("unauthorized equipment inspection"); });
            assertEquals(Stage.AUTHENTICATION,audit.stage); assertNull(audit.summary);
            returned.close(); return null;
        });
        assertEquals(1,jdbc.queryCount);
    }

    @Test void wrongOrMultiplePhysicalSchemasCloseOwnedConnectionAndNeverRunPipeline() throws Exception {
        for (boolean duplicate : new boolean[] {false,true}) {
            var jdbc = new Jdbc(); if (duplicate) jdbc.databaseRows = 2; else jdbc.database = SECRET;
            var audit = new Audit(); var calls = new AtomicInteger();
            scoped(() -> {
                var failure = assertThrows(SQLException.class,() -> inspectOwned(jdbc.connection,ModernBackendScope.current(),
                        DATABASE,CATALOG,3000,new Plan(7,SECRET,ROW),audit,
                        (connection,scope,catalog,plan,state) -> { calls.incrementAndGet(); return emptySummary(); }));
                assertEquals(Failure.ROUTING_MISMATCH,audit.failure); assertEquals(Stage.ROUTING,audit.stage);
                assertEquals(0,calls.get()); assertTrue(jdbc.closed); assertNull(failure.getCause());
                // The failed temporary owner is already retired; scope.finish
                // must not need to abort this closed connection a second time.
                try (var noLeakedLease = ModernBackendScope.current().register(jdbc.connection)) {
                    assertTrue(jdbc.closed);
                }
                assertEquals(0,failure.getSuppressed().length); assertFalse(failure.toString().contains(SECRET));
                assertFalse(renderFailure(7,3333,audit).contains(SECRET)); return null;
            });
        }
    }

    @Test void pipelineFailureAndCancellationDoNotLeakDriverDetailsOrTransferLiveConnection() throws Exception {
        for (boolean cancel : new boolean[] {false,true}) {
            var jdbc = new Jdbc(); var audit = new Audit();
            scoped(() -> {
                var scope = ModernBackendScope.current();
                var failure = assertThrows(SQLException.class,() -> inspectOwned(jdbc.connection,scope,DATABASE,CATALOG,3000,
                        new Plan(7,SECRET,ROW),audit,(connection,current,catalog,plan,state) -> {
                            state.stage = Stage.SOURCE;
                            if (cancel) { current.cancel(ModernBackendExecutor.Failure.DEADLINE); return emptySummary(); }
                            throw new SQLException(SECRET);
                        }));
                assertTrue(jdbc.closed); assertEquals(Failure.JDBC,audit.failure);
                assertEquals("bounded equipment probe failed",failure.getMessage());
                assertNull(failure.getCause()); assertEquals(0,failure.getSuppressed().length);
                assertFalse(renderFailure(7,3333,audit).contains(SECRET));
                if (cancel) assertEquals(ModernBackendExecutor.Failure.DEADLINE,scope.cancellation());
                return null;
            });
        }
    }

    private static Summary emptySummary() {
        var qualified = ModernEquipmentEvaluatorTest.qualified(List.of());
        return summarize(qualified,ModernEquipmentEvaluator.evaluate(qualified,java.util.Map.of(),0),0);
    }
    private static <T> T scoped(Callable<T> action) throws Exception {
        var scope = new ModernBackendScope(System.nanoTime()+TimeUnit.SECONDS.toNanos(10),new ModernBackendScope.AbortScheduler() {
            @Override public boolean execute(Runnable action) { return false; }
            @Override public void remove(Runnable action) {}
        });
        ModernBackendScope.bind(scope);
        try { return action.call(); }
        finally { try { scope.finish(); } finally { ModernBackendScope.unbind(); } }
    }
    private static final class Jdbc {
        String database = DATABASE;
        int databaseRows = 1, queryCount, maximumRows, querySeconds, networkMillis;
        int statementCloses, resultCloses, closeCalls;
        boolean readOnly, closed;
        final Connection connection = proxy(Connection.class,(method,args) -> switch (method) {
            case "setReadOnly" -> { assertEquals(true,args[0]); readOnly = true; yield null; }
            case "isReadOnly" -> readOnly;
            case "setNetworkTimeout" -> { networkMillis = (int)args[1]; yield null; }
            case "prepareStatement" -> { assertEquals("SELECT DATABASE()",args[0]); queryCount++; yield statement(); }
            case "close" -> { closed = true; closeCalls++; yield null; }
            case "toString" -> SECRET;
            default -> throw new AssertionError("unexpected connection operation: "+method);
        });
        PreparedStatement statement() {
            return proxy(PreparedStatement.class,(method,args) -> switch (method) {
                case "setMaxRows" -> { maximumRows = (int)args[0]; yield null; }
                case "setQueryTimeout" -> { querySeconds = (int)args[0]; yield null; }
                case "executeQuery" -> result();
                case "close" -> { statementCloses++; yield null; }
                default -> throw new AssertionError("unexpected statement operation: "+method);
            });
        }
        ResultSet result() {
            var cursor = new AtomicInteger();
            return proxy(ResultSet.class,(method,args) -> switch (method) {
                case "next" -> cursor.incrementAndGet() <= databaseRows;
                case "getString" -> { assertEquals(1,args[0]); yield database; }
                case "close" -> { resultCloses++; yield null; }
                default -> throw new AssertionError("unexpected result operation: "+method);
            });
        }
    }
    @FunctionalInterface private interface Invocation { Object invoke(String method,Object[] args) throws Throwable; }
    private static <T> T proxy(Class<T> type,Invocation invocation) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[] {type},
                (object,method,args) -> invocation.invoke(method.getName(),args == null ? new Object[0] : args)));
    }
}

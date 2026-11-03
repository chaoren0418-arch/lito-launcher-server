package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.*;
import static local.lito381.server.ModernInventoryWeightReadProbe.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernInventoryWeightReadProbeTest {
    @TempDir Path directory;
    private static final String PRIVATE = "synthetic-private-account-and-character";
    private static final TournamentPresence.CharacterKey KEY = new TournamentPresence.CharacterKey(7,3333);

    @Test void rateMappingRequiresEveryExplicitSourceWithoutDefaultsOrCoercion() {
        var parsed = rates("42=.25,7=2.5e1", Set.of(7,42));
        assertEquals(25.0, parsed.get(7).doubleValue()); assertEquals(.25, parsed.get(42).doubleValue());
        assertThrows(UnsupportedOperationException.class, () -> parsed.put(9,1.0));
        for (String text : new String[] {"", "7=1", "7=1,42=1,9=1", "7=1,7=2,42=1", "07=1,42=1", "0=1,42=1",
                "2147483648=1,42=1", "7=0,42=1", "7=-1,42=1", "7=NaN,42=1", "7=Infinity,42=1",
                "7=0x1p0,42=1", "7=1e999,42=1", "7=1e-999,42=1", "7=1,42=1,", "7= 1,42=1", "7=1, 42=1",
                "7=1\n,42=1", "7=1\u00a0,42=1", "7=1d,42=1"}) {
            assertThrows(IllegalArgumentException.class, () -> rates(text, Set.of(7,42)), text);
        }
        assertThrows(IllegalArgumentException.class, () -> rates(null,Set.of(7,42)));
        assertThrows(IllegalArgumentException.class, () -> rates("7=1",Set.of()));
        assertThrows(IllegalArgumentException.class, () -> rates("7=" + "1".repeat(8192),Set.of(7)));
    }

    @Test void missingNamespaceOrLockNeverCreatesAnythingAndNoRelativeOrTraversalPaths() throws Exception {
        Path missing = directory.resolve("never-created");
        assertThrows(IOException.class, () -> new ReadOnlyOverrides(missing));
        assertFalse(Files.exists(missing));
        assertThrows(IOException.class, () -> new ReadOnlyOverrides(directory));
        assertFalse(Files.exists(directory.resolve(".modern-equipment.lock")));
        assertThrows(IOException.class, () -> new ReadOnlyOverrides(Path.of("relative-store")));
        lock();
        assertThrows(IOException.class, () -> new ReadOnlyOverrides(directory.resolve("child/..")));
        assertEquals(List.of(".modern-equipment.lock"), names());
    }

    @Test void absentOverridesRequireExclusiveExplicitSourceOnlyModeAndRemainLabelled() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> overrideInput(null,null));
        assertThrows(IllegalArgumentException.class, () -> overrideInput(directory.toString(),"source-only-observation"));
        assertThrows(IllegalArgumentException.class, () -> overrideInput("","source-only-observation"));
        for (String value : List.of("","true","none"," source-only-observation","source-only-observation "))
            assertThrows(IllegalArgumentException.class, () -> overrideInput(null,value));
        var qualified = ModernEquipmentTestPeer.qualified(7);
        try (var sourceOnly = overrideInput(null,"source-only-observation")) {
            assertEquals(OverrideMode.SOURCE_ONLY_ABSENT,sourceOnly.mode());
            scoped(() -> {
                var stored = sourceOnly.read(KEY,ModernBackendScope.current()); assertTrue(stored.isEmpty());
                var row = summarize(observation(qualified,binding(),stored,2000),stored,100.0,sourceOnly.mode());
                assertTrue(render(row).contains("\"override_mode\":\"SOURCE_ONLY_ABSENT\""));
                assertTrue(render(row).contains(OBSERVATION)); assertEquals(0,row.equipmentRevision()); return null;
            });
            assertTrue(names().isEmpty(),"explicit source-only observation creates no fake namespace or lock");
        }
        var observed = observation(qualified,binding(),Optional.empty(),2000);
        assertThrows(IllegalArgumentException.class, () -> summarize(observed,Optional.of(stored(1,List.of())),1,OverrideMode.SOURCE_ONLY_ABSENT));
    }

    @Test void readsExactKeyUnderSharedLockWithoutChangingBytesTimesOrCreatingAbsentRecords() throws Exception {
        Path lock = lock();
        var record = stored(3,List.of());
        Path path = recordPath(KEY); byte[] bytes = ModernEquipmentOverrideStore.encode(record);
        Files.write(path,bytes);
        var time = Files.getLastModifiedTime(path); var lockTime = Files.getLastModifiedTime(lock);
        try (var reader = new ReadOnlyOverrides(directory)) {
            scoped(() -> {
                assertEquals(record,reader.read(KEY,ModernBackendScope.current()).orElseThrow());
                assertTrue(reader.read(new TournamentPresence.CharacterKey(42,3333),ModernBackendScope.current()).isEmpty());
                return null;
            });
            assertArrayEquals(bytes,Files.readAllBytes(path)); assertEquals(time,Files.getLastModifiedTime(path));
            assertEquals(lockTime,Files.getLastModifiedTime(lock));
            assertFalse(Files.exists(recordPath(new TournamentPresence.CharacterKey(42,3333))));
        }
        try (var writer = FileChannel.open(lock,StandardOpenOption.READ,StandardOpenOption.WRITE);
             var acquired = writer.tryLock()) { assertNotNull(acquired,"reader must release its real OS lock"); }
    }

    @Test void liveWriterExcludesProbeAndFailedOpenDoesNotLeakAnotherLock() throws Exception {
        Path lock = lock();
        try (var writer = FileChannel.open(lock,StandardOpenOption.READ,StandardOpenOption.WRITE);
             var held = writer.lock()) {
            var failure = assertThrows(IOException.class, () -> new ReadOnlyOverrides(directory));
            assertFalse(failure.toString().contains(directory.toString())); assertNull(failure.getCause());
        }
        try (var reader = new ReadOnlyOverrides(directory)) { reader.verify(); }
    }

    @Test void corruptOversizedAndWrongSourceRecordsAreUnavailableNotAbsentOrRebound() throws Exception {
        lock(); Path path = recordPath(KEY);
        byte[] valid = ModernEquipmentOverrideStore.encode(stored(1,List.of()));
        for (byte[] bytes : List.of(new byte[95], new byte[ModernEquipmentOverrideStore.MAX_BYTES+1],
                new byte[valid.length], ModernEquipmentOverrideStore.encode(new ModernEquipmentOverrideStore.Stored(
                    new TournamentPresence.CharacterKey(42,3333),1,new ModernEquipmentOverrideStore.Replacement(fingerprint(1),List.of()))))) {
            Files.write(path,bytes);
            try (var reader = new ReadOnlyOverrides(directory)) {
                scoped(() -> {
                    var failure = assertThrows(IOException.class, () -> reader.read(KEY,ModernBackendScope.current()));
                    assertEquals("readonly equipment observation unavailable",failure.getMessage()); assertNull(failure.getCause());
                    return null;
                });
            }
            assertArrayEquals(bytes,Files.readAllBytes(path));
        }
    }

    @Test void cancelledScopeCannotReadOrTurnCancellationIntoAnAbsentRecord() throws Exception {
        lock();
        try (var reader = new ReadOnlyOverrides(directory); var executor = new ModernBackendExecutor(1,0)) {
            try (var gate = executor.openSession()) {
                assertThrows(ModernBackendExecutor.BackendException.class, () -> gate.call(1000, scope -> {
                    scope.cancel(ModernBackendExecutor.Failure.CANCELLED);
                    assertThrows(IOException.class, () -> reader.read(KEY,scope)); return null;
                }));
            } finally { executor.close(); assertTrue(executor.awaitTermination(2000)); }
            reader.verify();
        }
    }

    @Test void observationUsesRealSavedOverrideButNeverChangesSqlFlagsOrPretendsToBeAck() throws Exception {
        var qualified = ModernEquipmentTestPeer.qualified(7);
        var first = qualified.source().inventory().get(0);
        var override = new ModernEquipmentOverrideStore.ItemOverride(first.objectId(),first.template().catalogId(),
                ModernEquipmentReconciliation.fingerprint(qualified.items().get(first.objectId()),qualified.lifetimes().items().get(first.objectId())),false);
        var stored = Optional.of(stored(3,List.of(override)));
        var receipt = observation(qualified,binding(),stored,2_000);
        assertEquals(3,receipt.revision()); assertFalse(receipt.equipped().get(0).equipped());
        assertTrue(receipt.source().inventory().get(0).equipped(),"raw SQL state is retained, never rewritten");
        assertEquals(0,receipt.modifiers().get(Stat.STR));
        var summary = summarize(receipt,stored,2.0);
        assertEquals(2,summary.basis().sourceGearWeight(),"both equipped and unequipped source gear count");
        assertEquals(1,summary.excludedEtc()); assertEquals(1,summary.storedOverrides()); assertEquals(0,summary.staleOverrides());
        String output = render(summary);
        assertTrue(output.contains(OBSERVATION)); assertTrue(output.contains("\"source_rate\":2.0"));
        assertTrue(output.contains("\"match_supplies_raw\":0"));
        assertFalse(output.contains(PRIVATE)); assertFalse(output.contains("fixture")); assertFalse(output.contains("jdbc:"));
        assertFalse(output.contains("$")); assertFalse(summary.toString().contains("source="));
        var other = new ModernEquipmentReconciliation.ExpectedBinding(KEY,fingerprint(2));
        var failure = assertThrows(ModernEquipmentReconciliation.ReconciliationException.class,
                () -> observation(qualified,other,stored,2000));
        assertEquals(ModernEquipmentReconciliation.Failure.BINDING_MISMATCH,failure.failure());
        assertTrue(qualified.source().inventory().get(0).equipped());
    }

    @Test void staleSavedFactsRemainReportedAndNotDeletedOrApplied() throws Exception {
        var qualified = ModernEquipmentTestPeer.qualified(7); var item = qualified.source().inventory().get(0);
        var old = new ModernEquipmentOverrideStore.ItemOverride(item.objectId(),item.template().catalogId(),fingerprint(77),false);
        var saved = Optional.of(stored(9,List.of(old)));
        var observed = observation(qualified,binding(),saved,2000);
        assertEquals(9,observed.revision()); assertEquals(1,observed.stale().size());
        assertTrue(observed.equipped().get(0).equipped()); assertEquals(List.of(old),saved.orElseThrow().value().overrides());
        assertEquals(1,summarize(observed,saved,1).staleOverrides());
    }

    @Test void expiredGearIsExcludedUnknownGearAndUninitializedOriginalStatsAreNotZeroWeightSuccess() throws Exception {
        var before = ModernEquipmentTestPeer.qualified(7);
        var facts = new HashMap<>(before.lifetimes().items());
        var first = before.source().inventory().get(0); var second = before.source().inventory().get(1);
        facts.put(first.objectId(),new ModernItemLifetime.Fact(first.objectId(),first.template().catalogId(),first.template().kind(),
                first.equipped(),OptionalLong.of(0),false,OptionalLong.empty(),Optional.of("1970-01-01 00:00:01"),0,false));
        facts.put(second.objectId(),new ModernItemLifetime.Fact(second.objectId(),second.template().catalogId(),second.template().kind(),
                second.equipped(),OptionalLong.of(1),false,OptionalLong.empty(),Optional.empty(),0,false));
        var lives = new ModernItemLifetime.Observation(7,3333,"1970-01-01 00:00:00",0,0,facts);
        var qualified = new Qualified(before.source(),before.items(),before.catalog(),lives);
        var observed = observation(qualified,binding(),Optional.empty(),2000);
        var summary = summarize(observed,Optional.empty(),1);
        assertEquals(1,summary.expiredGear()); assertEquals(1,summary.unknownGear()); assertNull(summary.basis());
        assertTrue(render(summary).contains("\"status\":\"UNQUALIFIED\"")); assertTrue(render(summary).contains("\"source_gear_raw\":null"));
        assertFalse(observed.equipped().get(0).equipped()); assertTrue(observed.source().inventory().get(0).equipped());
        facts.put(second.objectId(),before.lifetimes().items().get(second.objectId()));
        qualified = new Qualified(before.source(),before.items(),before.catalog(),new ModernItemLifetime.Observation(7,3333,
                "1970-01-01 00:00:00",0,0,facts));
        assertEquals(1,summarize(observation(qualified,binding(),Optional.empty(),2000),Optional.empty(),1).basis().sourceGearWeight());
        var p = before.source().persisted();
        var zero = new ModernCharacterData.PersistedValues(p.maxHp(),p.maxMp(),p.savedHp(),p.savedMp(),p.armorClass(),p.strength(),
                p.dexterity(),p.constitution(),p.intelligence(),p.wisdom(),p.charisma(),p.experience(),p.lawful(),0,
                p.originalDexterity(),0,p.originalIntelligence(),p.originalWisdom(),p.food225());
        var source = new ModernCharacterData.Snapshot(7,before.source().character(),zero,before.source().inventory());
        var uninitialized = new Qualified(source,before.items(),before.catalog(),ModernItemLifetime.Observation.verifiedPermanent(source));
        var rejected = summarize(observation(uninitialized,binding(),Optional.empty(),2000),Optional.empty(),1);
        assertEquals(2,rejected.uninitializedOriginalStats()); assertNull(rejected.basis());
        assertTrue(render(rejected).contains("\"uninitialized_original_stat_count\":2"));
    }

    @Test void routeAndPipelineUseSameReadOnlyLeaseThenTransferBeforeDaoRegistration() throws Exception {
        var jdbc = new Jdbc(); var audit = new Audit(); audit.stage = Stage.ROSTER; var calls = new AtomicInteger();
        scoped(() -> {
            var scope = ModernBackendScope.current();
            var connection = inspectOwned(jdbc.connection,scope,"synthetic_schema",3000,audit,(owned,current) -> {
                assertSame(scope,current); assertSame(jdbc.connection,owned); assertTrue(owned.isReadOnly());
                calls.incrementAndGet();
            });
            assertSame(jdbc.connection,connection); assertFalse(jdbc.closed); assertEquals(1,calls.get());
            try (var daoLease = scope.register(connection)) { assertFalse(jdbc.closed); }
            connection.close(); return null;
        });
        assertEquals(1,jdbc.closedCount); assertEquals(1,jdbc.statementCloses); assertEquals(1,jdbc.resultCloses);
        assertEquals(2,jdbc.maxRows); assertTrue(jdbc.network > 0 && jdbc.network <= 3000);
    }

    @Test void badRoutingOrPipelineFailureClosesAndRedactsWithoutUnownedHandoff() throws Exception {
        for (int kind = 0; kind < 3; kind++) {
            var jdbc = new Jdbc(); var audit = new Audit(); var calls = new AtomicInteger();
            if (kind == 0) jdbc.schema = PRIVATE;
            if (kind == 1) jdbc.rows = 2;
            scoped(() -> {
                var scope = ModernBackendScope.current();
                var failure = assertThrows(SQLException.class, () -> inspectOwned(jdbc.connection,scope,"synthetic_schema",3000,audit,
                        (connection,current) -> { calls.incrementAndGet(); throw new SQLException(PRIVATE); }));
                assertTrue(jdbc.closed); assertNull(failure.getCause()); assertEquals(0,failure.getSuppressed().length);
                assertFalse(failure.toString().contains(PRIVATE)); assertFalse(renderFailure(7,3333,audit).contains(PRIVATE));
                try (var noLeakedLease = scope.register(jdbc.connection)) { assertTrue(jdbc.closed); }
                return null;
            });
            assertEquals(kind == 2 ? 1 : 0,calls.get()); assertEquals(1,jdbc.closedCount);
        }
    }

    @Test void weightPeerUsesOneQualifiedSourceForGearReceiptAndFrozenPhysicalBasis() throws Exception {
        for (int sourceId : List.of(7,42)) {
            var row = new ModernApplicationProtocol.CharacterRow(3333,"fixture",sourceId == 7 ? 1 : 3,sourceId == 7 ? 0 : 1,80);
            var physical = ModernPhysicalCombatTest.fixtureSource(sourceId,row,100,10,0);
            physical = ModernMagicCombatTest.enable(ModernMagicCombatTest.stats(physical,12,0,0,0,100));
            physical = ModernCombatTestPeer.weightSource(physical);
            var qualified = physical.input().equipment();
            var binding = new ModernEquipmentReconciliation.ExpectedBinding(new TournamentPresence.CharacterKey(sourceId,3333),fingerprint(1));
            var fighter = ModernPhysicalCombat.qualify(physical,binding,Optional.empty(),2000);
            assertTrue(fighter.supported()); assertTrue(fighter.magic().supported());
            assertEquals(0,fighter.profile().weapon().objectId());
            var receipt = observation(qualified,binding,Optional.empty(),2000);
            var gearBasis = ModernInventoryWeight.qualify(receipt,1.0);
            var worldBasis = ModernInventoryWeight.qualify(fighter.profile(),1.0);
            assertTrue(gearBasis.sameFacts(worldBasis)); assertEquals(2575,gearBasis.sourceGearWeight());
            assertEquals(3150.0,gearBasis.maximumWeight());
            var two = ModernInventoryWeight.evaluate(worldBasis,List.of(new ModernInventoryWeight.Carried(40010,2,3900))).view();
            var one = ModernInventoryWeight.evaluate(worldBasis,List.of(new ModernInventoryWeight.Carried(40010,1,3900))).view();
            assertEquals(2582,two.totalWeight()); assertEquals(197,two.stage()); assertFalse(two.permitsAttackCast());
            assertEquals(2578,one.totalWeight()); assertEquals(196,one.stage()); assertTrue(one.permitsAttackCast());
        }
    }

    private Path lock() throws IOException { return Files.write(directory.resolve(".modern-equipment.lock"),new byte[0]); }
    private Path recordPath(TournamentPresence.CharacterKey key) {
        return directory.resolve("source-" + key.sourceId() + "-character-" + key.objectId() + ".meo");
    }
    private List<String> names() throws IOException {
        try (var entries = Files.list(directory)) { return entries.map(path -> path.getFileName().toString()).sorted().toList(); }
    }
    private static ModernEquipmentOverrideStore.Fingerprint fingerprint(int marker) {
        byte[] bytes = new byte[32]; bytes[0] = (byte)marker; return new ModernEquipmentOverrideStore.Fingerprint(bytes);
    }
    private static ModernEquipmentReconciliation.ExpectedBinding binding() {
        return new ModernEquipmentReconciliation.ExpectedBinding(KEY,fingerprint(1));
    }
    private static ModernEquipmentOverrideStore.Stored stored(long revision,List<ModernEquipmentOverrideStore.ItemOverride> overrides) {
        return new ModernEquipmentOverrideStore.Stored(KEY,revision,new ModernEquipmentOverrideStore.Replacement(fingerprint(1),overrides));
    }
    private static <T> T scoped(Callable<T> call) throws Exception {
        try (var executor = new ModernBackendExecutor(1,0)) {
            try (var gate = executor.openSession()) { return gate.call(3000,scope -> call.call()); }
            finally { executor.close(); assertTrue(executor.awaitTermination(2000)); }
        }
    }
    private static final class Jdbc {
        boolean readOnly, closed; int closedCount, statementCloses, resultCloses, maxRows, network, rows = 1;
        String schema = "synthetic_schema";
        final Connection connection = proxy(Connection.class,(method,args) -> switch (method) {
            case "setReadOnly" -> { readOnly = (boolean)args[0]; yield null; }
            case "isReadOnly" -> readOnly;
            case "setNetworkTimeout" -> { network = (int)args[1]; yield null; }
            case "prepareStatement" -> { assertEquals("SELECT DATABASE()",args[0]); yield statement(); }
            case "close" -> { closed = true; closedCount++; yield null; }
            case "abort" -> { closed = true; yield null; }
            default -> throw new AssertionError("unexpected JDBC method: " + method);
        });
        private PreparedStatement statement() {
            return proxy(PreparedStatement.class,(method,args) -> switch (method) {
                case "setMaxRows" -> { maxRows = (int)args[0]; yield null; }
                case "setQueryTimeout" -> { assertTrue((int)args[0] > 0); yield null; }
                case "executeQuery" -> result();
                case "close" -> { statementCloses++; yield null; }
                default -> throw new AssertionError("unexpected JDBC statement: " + method);
            });
        }
        private ResultSet result() {
            var index = new AtomicInteger();
            return proxy(ResultSet.class,(method,args) -> switch (method) {
                case "next" -> index.incrementAndGet() <= rows;
                case "getString" -> schema;
                case "close" -> { resultCloses++; yield null; }
                default -> throw new AssertionError("unexpected JDBC result: " + method);
            });
        }
    }
    @FunctionalInterface private interface Call { Object invoke(String method,Object[] args) throws Throwable; }
    @SuppressWarnings("unchecked") private static <T> T proxy(Class<T> type,Call call) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(object,method,args) -> {
            if (method.getDeclaringClass() == Object.class) return switch (method.getName()) {
                case "toString" -> "redacted-jdbc-probe";
                case "hashCode" -> System.identityHashCode(object);
                case "equals" -> object == args[0];
                default -> throw new AssertionError();
            };
            return call.invoke(method.getName(),args == null ? new Object[0] : args);
        });
    }
}

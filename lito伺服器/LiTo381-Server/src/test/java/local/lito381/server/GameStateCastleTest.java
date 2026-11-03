package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStateCastleTest {
    @TempDir Path temporary;

    @Test
    void seedsOnceAndCommitsInventoryAndTreasuryAtomically() throws Exception {
        Path file = temporary.resolve("castle-state.properties");
        GameStateStore state = new GameStateStore(file, true);
        CastleSummary legacy = new CastleSummary(
                5, "LegacyEmblem", "TestEmblem", "海音城", 1, 10, 10_000);
        GameStateStore.CastleState seed = state.castle(legacy);

        Map<Long, Integer> depositInventory = new LinkedHashMap<>(
                state.economy("TestEmblem").inventory());
        depositInventory.put(InventoryCatalog.ADENA,
                depositInventory.get(InventoryCatalog.ADENA) - 2_000);
        assertTrue(state.saveInventoryAndCastle(
                "TestEmblem", depositInventory, seed, 10, 12_000));
        assertFalse(state.saveInventoryAndCastle(
                "TestEmblem", Map.of(), seed, 10, 99_999));
        GameStateStore.CastleState afterTreasury = state.castle(legacy);
        assertFalse(afterTreasury.securityEnabled());
        assertTrue(state.saveCastleSecurity(afterTreasury, true));
        assertFalse(state.saveCastleSecurity(afterTreasury, false));
        GameStateStore.CastleState beforeMercenaries = state.castle(legacy);
        GameStateStore.MercenaryRoster hired = beforeMercenaries.mercenaries().add(2, 3);
        assertTrue(state.saveCastleMercenaries(beforeMercenaries, hired, 9_000));
        assertFalse(state.saveCastleMercenaries(beforeMercenaries,
                new GameStateStore.MercenaryRoster(99, 0, 0, 0), 1));

        GameStateStore reloaded = new GameStateStore(file, true);
        GameStateStore.CastleState durable = reloaded.castle(new CastleSummary(
                5, "LegacyEmblem", "TestEmblem", "Changed legacy seed", 2, 50, 1));
        assertEquals(10, durable.taxRate());
        assertEquals(9_000, durable.publicMoney());
        assertEquals(1, durable.warTimeMillis());
        assertTrue(durable.securityEnabled());
        assertEquals(new GameStateStore.MercenaryRoster(0, 0, 3, 0),
                durable.mercenaries());
        assertEquals(4, durable.revision());
        assertEquals(98_000, reloaded.economy("TestEmblem").inventory()
                .get(InventoryCatalog.ADENA));
        String source = Files.readString(file);
        assertTrue(source.contains("format.version=43"));
        assertTrue(source.contains("castle.count=1"));
        assertTrue(source.contains("warTimeMillis=1"));
        assertTrue(source.contains("securityEnabled=true"));
        assertTrue(source.contains("mercenary.2=3"));
    }

    @Test
    void clampsInvalidLegacySeedToNativeModalRange() throws Exception {
        GameStateStore state = new GameStateStore(temporary.resolve("unused.properties"), false);
        GameStateStore.CastleState castle = state.castle(new CastleSummary(
                3, "Clan", "Leader", "Castle", 0, 0, Long.MAX_VALUE));
        assertEquals(10, castle.taxRate());
        assertEquals(Integer.MAX_VALUE, castle.publicMoney());
        assertFalse(castle.securityEnabled());
        assertEquals(new GameStateStore.MercenaryRoster(0, 0, 0, 0),
                castle.mercenaries());
    }

    @Test
    void sqlSnapshotReplacesSourceQualifiedSidecarInsteadOfMergingIt()
            throws Exception {
        Path file = temporary.resolve("sql-castle-mirror.properties");
        GameStateStore state = new GameStateStore(file, true);
        CastleSummary legacy = new CastleSummary(
                4, "LegacyGiran", "LegacyLord", "Giran", 11, 20, 30_000);
        GameStateStore.CastleState sidecar = state.castle("380 角色庫 1", legacy);
        assertTrue(state.saveCastleMercenaries("380 角色庫 1", sidecar,
                new GameStateStore.MercenaryRoster(9, 0, 0, 0), 29_000));

        RepositoryCastleSnapshot sql = new RepositoryCastleSnapshot(
                true, 4, 35, 88_000, 22, true,
                new GameStateStore.MercenaryRoster(1, 2, 3, 4), 7);
        GameStateStore.CastleState reconciled = state.reconcileSqlCastle(
                "380 角色庫 1", sql);
        assertEquals(35, reconciled.taxRate());
        assertEquals(88_000, reconciled.publicMoney());
        assertEquals(22, reconciled.warTimeMillis());
        assertTrue(reconciled.securityEnabled());
        assertEquals(sql.mercenaries(), reconciled.mercenaries());
        assertEquals(8, reconciled.revision());

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(reconciled,
                reloaded.castle("380 角色庫 1", legacy));
        assertEquals(30_000,
                reloaded.castle("380 角色庫 2", legacy).publicMoney());
    }

    @Test
    void assignsCastleMercenariesToCharacterAtomicallyAndDurably() throws Exception {
        Path file = temporary.resolve("mercenary-assignment-state.properties");
        GameStateStore state = new GameStateStore(file, true);
        CastleSummary legacy = new CastleSummary(
                4, "LegacyGiran", "TestGiranLord", "Giran", 1, 10, 60_000);
        GameStateStore.CastleState seeded = state.castle(legacy);
        GameStateStore.MercenaryRoster hired = seeded.mercenaries().add(3, 2);
        assertTrue(state.saveCastleMercenaries(seeded, hired, 30_000));

        GameStateStore.CastleState before = state.castle(legacy);
        GameStateStore.MercenaryAssignment empty =
                state.mercenaryAssignment("TestGiranLord");
        GameStateStore.MercenaryRoster castleAfter = before.mercenaries().subtract(3, 1);
        GameStateStore.MercenaryAssignment assigned =
                new GameStateStore.MercenaryAssignment(4,
                        empty.mercenaries().add(3, 1));
        assertTrue(state.assignCastleMercenaries(
                before, castleAfter, "TestGiranLord", empty, assigned));
        assertFalse(state.assignCastleMercenaries(
                before, castleAfter, "TestGiranLord", empty, assigned));

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(new GameStateStore.MercenaryRoster(0, 0, 0, 1),
                reloaded.castle(legacy).mercenaries());
        assertEquals(assigned, reloaded.mercenaryAssignment("TestGiranLord"));
        String source = Files.readString(file);
        assertTrue(source.contains("format.version=43"));
        assertTrue(source.contains("mercenary.castleId=4"));
        assertTrue(source.contains("mercenary.3=1"));
    }

    @Test
    void sourceQualifiedCastleIdsStayIndependentAndClaimFormat18Once() throws Exception {
        Path file = temporary.resolve("source-castle-migration.properties");
        Files.writeString(file, """
                format.version=18
                castle.count=1
                castle.0.id=4
                castle.0.taxRate=20
                castle.0.publicMoney=12345
                castle.0.securityEnabled=true
                castle.0.mercenary.0=1
                castle.0.mercenary.1=0
                castle.0.mercenary.2=0
                castle.0.mercenary.3=0
                castle.0.revision=7
                """);

        CastleSummary sunSeed = new CastleSummary(
                4, "SameClan", "SunLord", "Sun Giran", 1, 10, 1_000);
        CastleSummary moonSeed = new CastleSummary(
                4, "SameClan", "MoonLord", "Moon Giran", 1, 30, 50_000);
        GameStateStore state = new GameStateStore(file, true);
        GameStateStore.CastleState sun = state.castle("Sun", sunSeed);
        assertEquals("Sun", sun.sourceId());
        assertEquals(20, sun.taxRate());
        assertEquals(12_345, sun.publicMoney());
        assertTrue(sun.securityEnabled());
        assertEquals(new GameStateStore.MercenaryRoster(1, 0, 0, 0),
                sun.mercenaries());

        GameStateStore.CastleState moon = state.castle("Moon", moonSeed);
        assertEquals("Moon", moon.sourceId());
        assertEquals(30, moon.taxRate());
        assertEquals(50_000, moon.publicMoney());
        assertFalse(moon.securityEnabled());
        assertTrue(state.saveCastleMercenaries(
                "Moon", moon, moon.mercenaries().add(2, 2), 40_000));
        assertFalse(state.saveCastleMercenaries(
                "Moon", sun, sun.mercenaries(), 1));
        assertEquals(12_345, state.castle("Sun", sunSeed).publicMoney());

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(12_345, reloaded.castle("Sun", sunSeed).publicMoney());
        GameStateStore.CastleState durableMoon = reloaded.castle("Moon", moonSeed);
        assertEquals(40_000, durableMoon.publicMoney());
        assertEquals(new GameStateStore.MercenaryRoster(0, 0, 2, 0),
                durableMoon.mercenaries());
        String durable = Files.readString(file);
        assertTrue(durable.contains("format.version=43"));
        assertTrue(durable.contains("castle.count=2"));
        assertTrue(durable.contains("source=Sun"));
        assertTrue(durable.contains("source=Moon"));
    }

    @Test
    void persistsObservedWarEndWithoutWritingTheLegacySource() throws Exception {
        Path file = temporary.resolve("castle-war-end-state.properties");
        long sourceStart = 1_800_000_000_000L;
        long nextStart = sourceStart + 120L * 60_000L;
        CastleSummary legacy = new CastleSummary(
                5, "LegacyEmblem", "TestEmblem", "Heine",
                sourceStart, 40, 50_000);
        CastleWarSchedule sourceSchedule = new CastleWarSchedule(
                5, "海音城", sourceStart, 60, 120);

        GameStateStore state = new GameStateStore(file, true);
        assertEquals(sourceStart,
                state.effectiveCastleWarSchedule("Sun", sourceSchedule).warTimeMillis());
        GameStateStore.CastleState seeded = state.castle("Sun", legacy);
        assertEquals(40, seeded.taxRate());
        assertEquals(50_000, seeded.publicMoney());

        GameStateStore.CastleState ended = state.applyCastleWarEnd("Sun", 5, nextStart);
        assertEquals(10, ended.taxRate());
        assertEquals(0, ended.publicMoney());
        assertEquals(nextStart, ended.warTimeMillis());
        assertEquals(2, ended.revision());
        assertEquals(sourceStart,
                state.effectiveCastleWarSchedule("Moon", sourceSchedule).warTimeMillis());

        CastleWarSchedule unownedSource = new CastleWarSchedule(
                6, "侏儒城", sourceStart, 60, 120);
        GameStateStore.CastleState unownedEnd = state.applyCastleWarEnd(
                "Sun", 6, nextStart);
        assertEquals(10, unownedEnd.taxRate());
        assertEquals(0, unownedEnd.publicMoney());
        assertEquals(nextStart, state.effectiveCastleWarSchedule(
                "Sun", unownedSource).warTimeMillis());

        GameStateStore reloaded = new GameStateStore(file, true);
        CastleWarSchedule effective = reloaded.effectiveCastleWarSchedule(
                "Sun", sourceSchedule);
        assertEquals(nextStart, effective.warTimeMillis());
        GameStateStore.CastleState durable = reloaded.castle("Sun", legacy);
        assertEquals(10, durable.taxRate());
        assertEquals(0, durable.publicMoney());
        assertEquals(nextStart, durable.warTimeMillis());
        String properties = Files.readString(file);
        assertTrue(properties.contains("warTimeMillis=" + nextStart));
    }

    @Test
    void castleSecurityChargeIsOncePerWarAndReadyWorkResumesAfterRestart()
            throws Exception {
        Path file = temporary.resolve("castle-security-settlement.properties");
        CastleSummary legacy = new CastleSummary(
                4, "LegacyGiran", "TestGiranLord", "Giran",
                1_800_000_000_000L, 10, 250_000);
        long firstWar = 1_800_000_100_000L;
        long secondWar = 1_800_000_200_000L;

        GameStateStore state = new GameStateStore(file, true);
        GameStateStore.CastleState seed = state.castle("Sun", legacy);
        assertTrue(state.saveCastleSecurity("Sun", seed, true));
        GameStateStore.CastleSecuritySettlement prepared =
                state.prepareCastleSecurity("Sun", 4, firstWar, 100_000);
        assertEquals(GameStateStore.CastleSecuritySettlementStatus.READY,
                prepared.status());
        assertEquals(100_000, prepared.chargedAdena());
        assertEquals(150_000, prepared.snapshot().publicMoney());

        GameStateStore resumed = new GameStateStore(file, true);
        GameStateStore.CastleSecuritySettlement retry =
                resumed.prepareCastleSecurity("Sun", 4, firstWar, 100_000);
        assertEquals(GameStateStore.CastleSecuritySettlementStatus.READY,
                retry.status());
        assertEquals(150_000, retry.snapshot().publicMoney(),
                "READY retry must not charge twice");
        assertEquals(GameStateStore.CastleSecuritySettlementStatus.ALREADY_SETTLED,
                resumed.completeCastleSecurity("Sun", 4, firstWar).status());

        GameStateStore completed = new GameStateStore(file, true);
        assertEquals(GameStateStore.CastleSecuritySettlementStatus.ALREADY_SETTLED,
                completed.prepareCastleSecurity(
                        "Sun", 4, firstWar, 100_000).status());
        GameStateStore.CastleSecuritySettlement next =
                completed.prepareCastleSecurity("Sun", 4, secondWar, 100_000);
        assertEquals(GameStateStore.CastleSecuritySettlementStatus.READY,
                next.status());
        assertEquals(50_000, next.snapshot().publicMoney());
        assertTrue(Files.readString(file).contains(
                "castle.security.settlement.count=2"));
    }

    @Test
    void insufficientCastleTreasuryIsRememberedForThatWar() throws Exception {
        Path file = temporary.resolve("castle-security-insufficient.properties");
        CastleSummary legacy = new CastleSummary(
                3, "LegacyWindawood", "TestWindawoodLord", "Windawood",
                1_800_000_000_000L, 10, 99_999);
        long war = 1_800_000_300_000L;

        GameStateStore state = new GameStateStore(file, true);
        GameStateStore.CastleState seed = state.castle("Moon", legacy);
        assertTrue(state.saveCastleSecurity("Moon", seed, true));
        GameStateStore.CastleSecuritySettlement rejected =
                state.prepareCastleSecurity("Moon", 3, war, 100_000);
        assertEquals(GameStateStore.CastleSecuritySettlementStatus.INSUFFICIENT_FUNDS,
                rejected.status());
        assertEquals(99_999, rejected.snapshot().publicMoney());

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(GameStateStore.CastleSecuritySettlementStatus.INSUFFICIENT_FUNDS,
                reloaded.prepareCastleSecurity(
                        "Moon", 3, war, 100_000).status());
        assertEquals(99_999, reloaded.castle("Moon", legacy).publicMoney());
    }
}

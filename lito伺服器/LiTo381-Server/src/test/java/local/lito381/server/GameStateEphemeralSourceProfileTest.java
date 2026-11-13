package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class GameStateEphemeralSourceProfileTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @TempDir Path temporary;

    @Test void onlyFormalReadOnlyLitoMysqlTopologyEnablesEphemeralSources()
            throws Exception {
        assertTrue(ServerConfig.load(Path.of(
                "config/server.properties"))
                .ephemeralSourceProfiles());
        assertFalse(ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.selftest.properties"))
                .ephemeralSourceProfiles());
    }

    @Test void sourceProfileIsMemoryOnlyAndRestartAlwaysReloadsFresh() throws Exception {
        Path file = temporary.resolve("ephemeral.properties");
        String character = GameStateStore.sourceCharacter(
                "380_ac_1", "SourceOnlyHero");
        GameStateStore state = new GameStateStore(file, true, true);
        GameStateStore.Economy initial = state.economy(character);
        state.saveEconomyState(character, 987_654,
                initial.inventory(), initial.itemStates());

        String sidecar = LocalResources.readString(file);
        assertFalse(sidecar.contains("SourceOnlyHero"));
        assertFalse(sidecar.contains("380_ac_1"));

        GameStateStore restarted = new GameStateStore(file, true, true);
        assertEquals(initial.points(), restarted.economy(character).points());
    }

    @Test void ephemeralStartupPurgesLegacySourceProfileAndPetResidue() throws Exception {
        Path file = temporary.resolve("migration.properties");
        String character = GameStateStore.sourceCharacter(
                "380_ac_1", "MigratedHero");
        SourcePet sourcePet = new SourcePet(10_231, 10_233,
                (int) InventoryCatalog.PET_COLLAR, 45_034,
                "牧羊犬", 12, 78, 17, 21_375, 0);
        GameStateStore legacy = new GameStateStore(file, true);
        long collar = legacy.importSourcePets(character, List.of(sourcePet))
                .get(0).collarObjectId();
        assertTrue(LocalResources.readString(file).contains("MigratedHero"));

        GameStateStore migrated = new GameStateStore(file, true, true);
        assertNull(migrated.petForCollar(collar));
        String rewritten = LocalResources.readString(file);
        assertFalse(rewritten.contains("MigratedHero"));
        assertFalse(rewritten.contains("380_ac_1"));
        assertTrue(rewritten.contains("pet.imported.count=0"));
    }

    @Test void logoutReleaseDropsProfilePetsAndImportProvenance() throws Exception {
        Path file = temporary.resolve("logout.properties");
        String character = GameStateStore.sourceCharacter(
                "380_ac_1", "LogoutHero");
        SourcePet sourcePet = new SourcePet(20_231, 20_233,
                (int) InventoryCatalog.PET_COLLAR, 45_034,
                "牧羊犬", 12, 78, 17, 21_375, 0);
        GameStateStore state = new GameStateStore(file, true, true);
        long initialPoints = state.economy(character).points();
        long collar = state.importSourcePets(character, List.of(sourcePet))
                .get(0).collarObjectId();
        GameStateStore.Economy loaded = state.economy(character);
        state.saveEconomyState(character, 456_789,
                loaded.inventory(), loaded.itemStates());

        GameStateStore.SourceProfileRelease released =
                state.releaseSourceCharacter(character);
        assertTrue(released.released());
        assertEquals(1, released.pets());
        assertEquals(1, released.provenanceKeys());
        assertNull(state.petForCollar(collar));
        assertEquals(initialPoints, state.economy(character).points());
        assertFalse(LocalResources.readString(file).contains("LogoutHero"));
    }

    @Test void releasingOneDatabaseRouteDoesNotTouchAnotherRoute() throws Exception {
        Path file = temporary.resolve("routes.properties");
        String first = GameStateStore.sourceCharacter("380_ac_1", "TwinHero");
        String second = GameStateStore.sourceCharacter("380_ac_2", "TwinHero");
        GameStateStore state = new GameStateStore(file, true, true);
        GameStateStore.Economy firstInitial = state.economy(first);
        GameStateStore.Economy secondInitial = state.economy(second);
        state.saveEconomyState(first, 111,
                firstInitial.inventory(), firstInitial.itemStates());
        state.saveEconomyState(second, 222,
                secondInitial.inventory(), secondInitial.itemStates());

        assertTrue(state.releaseSourceCharacter(first).released());
        assertEquals(222, state.economy(second).points());
        assertEquals(firstInitial.points(), state.economy(first).points());
    }

    @Test void writableTopologyRetainsItsExistingDurableSemantics() throws Exception {
        Path file = temporary.resolve("writable.properties");
        String character = GameStateStore.sourceCharacter(
                "380_ac_1", "WritableHero");
        GameStateStore state = new GameStateStore(file, true);
        GameStateStore.Economy initial = state.economy(character);
        state.saveEconomyState(character, 333,
                initial.inventory(), initial.itemStates());

        assertFalse(state.releaseSourceCharacter(character).released());
        assertEquals(333, new GameStateStore(file, true)
                .economy(character).points());
    }

    @Test void hubDisconnectReleasesOnlyTheDepartingSourceRoute() throws Exception {
        Path file = temporary.resolve("hub.properties");
        String firstIdentity = GameStateStore.sourceCharacter(
                "380_ac_1", "TwinHero");
        String secondIdentity = GameStateStore.sourceCharacter(
                "380_ac_2", "TwinHero");
        GameStateStore state = new GameStateStore(file, true, true);
        GameStateStore.Economy firstInitial = state.economy(firstIdentity);
        GameStateStore.Economy secondInitial = state.economy(secondIdentity);
        state.saveEconomyState(firstIdentity, 1_111,
                firstInitial.inventory(), firstInitial.itemStates());
        state.saveEconomyState(secondIdentity, 2_222,
                secondInitial.inventory(), secondInitial.itemStates());
        String firstAccount = GameStateStore.sourceCharacter("380_ac_1", "test");
        String secondAccount = GameStateStore.sourceCharacter("380_ac_2", "test");
        RuntimeInventory firstWarehouse = new RuntimeInventory();
        RuntimeInventory secondWarehouse = new RuntimeInventory();
        assertTrue(firstWarehouse.add(
                InventoryCatalog.require(InventoryCatalog.SHORT_SWORD), 1));
        assertTrue(secondWarehouse.add(
                InventoryCatalog.require(InventoryCatalog.NATIVE_HEALING_POTION), 2));
        state.reconcileSqlWarehouse(GameStateStore.WarehouseScope.PERSONAL,
                firstAccount, firstWarehouse);
        state.reconcileSqlWarehouse(GameStateStore.WarehouseScope.PERSONAL,
                secondAccount, secondWarehouse);

        ServerConfig hubConfig = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        ServerConfig firstConfig = sourceConfig(
                temporary.resolve("first.properties"), "380_ac_1");
        ServerConfig secondConfig = sourceConfig(
                temporary.resolve("second.properties"), "380_ac_2");
        try (GameHub hub = new GameHub(hubConfig, state)) {
            ClientSession first = new ClientSession(null,
                    ClientSession.Endpoint.GAME, firstConfig,
                    null, state, LocalResources.discardCapture(), hub);
            ClientSession second = new ClientSession(null,
                    ClientSession.Endpoint.GAME, secondConfig,
                    null, state, LocalResources.discardCapture(), hub);
            setField(first, "selectedCharacter", "TwinHero");
            setField(second, "selectedCharacter", "TwinHero");
            setField(first, "account", "test");
            setField(second, "account", "test");
            hub.connect(first);
            hub.connect(second);
            assertTrue(hub.selectCharacter(first, firstIdentity));
            assertTrue(hub.selectCharacter(second, secondIdentity));

            hub.disconnect(first);
            assertEquals(firstInitial.points(), state.economy(firstIdentity).points());
            assertEquals(2_222, state.economy(secondIdentity).points());
            assertTrue(state.personalWarehouse(firstAccount, List.of())
                    .inventory().isEmpty());
            assertEquals(2, state.personalWarehouse(secondAccount, List.of())
                    .inventory().get(InventoryCatalog.NATIVE_HEALING_POTION));

            hub.disconnect(second);
            assertEquals(secondInitial.points(), state.economy(secondIdentity).points());
            assertTrue(state.personalWarehouse(secondAccount, List.of())
                    .inventory().isEmpty());
        }
    }

    @Test void sourceAccountAndClanWarehousesAreMemoryOnlyAndReclaimed()
            throws Exception {
        Path file = temporary.resolve("warehouse.properties");
        String account = GameStateStore.sourceCharacter("380_ac_1", "test");
        String clan = GameStateStore.sourceCharacter("380_ac_1", "TestClan");
        RuntimeInventory sqlWarehouse = new RuntimeInventory();
        assertTrue(sqlWarehouse.add(
                InventoryCatalog.require(InventoryCatalog.SHORT_SWORD), 1));
        GameStateStore state = new GameStateStore(file, true, true);
        state.reconcileSqlWarehouse(
                GameStateStore.WarehouseScope.PERSONAL, account, sqlWarehouse);
        state.reconcileSqlWarehouse(
                GameStateStore.WarehouseScope.ELF, account, sqlWarehouse);
        state.reconcileSqlWarehouse(
                GameStateStore.WarehouseScope.CLAN, clan, sqlWarehouse);

        assertEquals(1, state.personalWarehouse(account, List.of())
                .inventory().get(InventoryCatalog.SHORT_SWORD));
        assertEquals(1, state.clanWarehouse(clan)
                .inventory().get(InventoryCatalog.SHORT_SWORD));
        assertFalse(LocalResources.readString(file).contains("380_ac_1"));

        GameStateStore.SourceOwnerRelease accountRelease =
                state.releaseSourceAccount(account);
        GameStateStore.SourceOwnerRelease clanRelease =
                state.releaseSourceClan(clan);
        assertTrue(accountRelease.released());
        assertEquals(2, accountRelease.warehouses());
        assertTrue(clanRelease.released());
        assertEquals(1, clanRelease.warehouses());
        assertTrue(state.personalWarehouse(account, List.of()).inventory().isEmpty());
        assertTrue(state.elfWarehouse(account).inventory().isEmpty());
        assertTrue(state.clanWarehouse(clan).inventory().isEmpty());
    }

    @Test void ephemeralStartupDropsLegacySourceWarehouseCaches() throws Exception {
        Path file = temporary.resolve("warehouse-migration.properties");
        String account = GameStateStore.sourceCharacter("380_ac_1", "legacy");
        RuntimeInventory sqlWarehouse = new RuntimeInventory();
        assertTrue(sqlWarehouse.add(
                InventoryCatalog.require(InventoryCatalog.SHORT_SWORD), 1));
        GameStateStore legacy = new GameStateStore(file, true);
        legacy.reconcileSqlWarehouse(
                GameStateStore.WarehouseScope.PERSONAL, account, sqlWarehouse);
        assertTrue(LocalResources.readString(file).contains("380_ac_1"));

        GameStateStore migrated = new GameStateStore(file, true, true);
        assertTrue(migrated.personalWarehouse(account, List.of())
                .inventory().isEmpty());
        assertFalse(LocalResources.readString(file).contains("380_ac_1"));
    }

    @Test void hubDisconnectReclaimsPartyWaterDragonAndClanEmblemCaches()
            throws Exception {
        Path file = temporary.resolve("hub-containers.properties");
        String identity = GameStateStore.sourceCharacter(
                "380_ac_1", "ContainerHero");
        String peerIdentity = GameStateStore.sourceCharacter(
                "380_ac_1", "ContainerPeer");
        GameStateStore state = new GameStateStore(file, true, true);
        ServerConfig hubConfig = ServerConfig.load(
                Path.of("config/server.properties"));
        ServerConfig sourceConfig = sourceConfig(
                temporary.resolve("container-source.properties"), "380_ac_1");
        try (GameHub hub = new GameHub(hubConfig, state)) {
            ClientSession session = new ClientSession(null,
                    ClientSession.Endpoint.GAME, sourceConfig,
                    null, state, LocalResources.discardCapture(), hub);
            setField(session, "selectedCharacter", "ContainerHero");
            setField(session, "account", "test");
            hub.connect(session);
            assertTrue(hub.selectCharacter(session, identity));

            assertEquals(GameHub.ClassicPartyJoinStatus.JOINED,
                    hub.joinClassicParty(identity, peerIdentity, 8).status());
            hub.registerOriginalClan(identity, "ContainerClan");
            byte[] pixels = new byte[Protocol.EMBLEM_BYTES];
            pixels[0] = 0x3A;
            assertTrue(hub.registerClanEmblem(identity, "ContainerClan",
                    new ClanEmblemData(0x5544L, pixels)));

            int showId = 0x3456;
            ClassicWorldWaterDragonInstances.Admission admission =
                    hub.classicWorldWaterDragonInstances().admit(
                            identity, showId, 60, true, false,
                            System.nanoTime());
            assertTrue(admission.admitted());
            hub.enterClassicWorld(session,
                    ClassicWorldWaterDragonCatalog.MAP_ID,
                    ClassicWorldWaterDragonCatalog.ENTRY.x(),
                    ClassicWorldWaterDragonCatalog.ENTRY.y(), 0, showId);

            hub.disconnect(session);

            assertFalse(hub.classicPartyData(peerIdentity).inParty());
            assertEquals(GameHub.ClanData.empty(), hub.clanData(identity));
            assertNull(hub.clanEmblem(0x5544L));
            ClassicWorldWaterDragonInstances.Snapshot remaining =
                    hub.classicWorldWaterDragonInstances().snapshot(showId);
            assertTrue(remaining == null
                    || !remaining.joinedIdentities().contains(
                            identity.toLowerCase()));
        }
    }

    private static ServerConfig sourceConfig(Path path, String source) throws Exception {
        Files.writeString(path, "database.mode=memory\n"
                + "login.classic.server.names=" + source + "\n");
        return ServerConfig.load(path);
    }

    private static void setField(Object target, String name, Object value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}

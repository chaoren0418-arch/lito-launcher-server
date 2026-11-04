package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

final class LitoMySqlCharacterMutationIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/config/sql.properties"));
    }

    @Test
    void createsReadsAndDeletesOnlyALitoOwnedCharacter(@TempDir Path temporary)
            throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
        Path base = Path.of("config", "server.properties")
                .toAbsolutePath().normalize();
        Properties properties = new Properties();
        try (Reader reader = LocalResources.newBufferedReader(base, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        properties.setProperty("database.read.only", "false");
        properties.setProperty("database.connection.properties",
                Path.of("..", "1-伺服器", "config", "sql.properties")
                        .toAbsolutePath().normalize().toString());
        properties.setProperty("maps.directory",
                Path.of("maps").toAbsolutePath().normalize().toString());
        properties.setProperty("state.file", temporary.resolve("state.properties").toString());
        properties.setProperty("capture.directory", temporary.resolve("captures").toString());
        Path writableConfig = temporary.resolve("server.properties");
        try (Writer writer = Files.newBufferedWriter(writableConfig, StandardCharsets.UTF_8)) {
            properties.store(writer, "temporary writable LiTo integration profile");
        }

        String name = "LitoSqlGate";
        String routeTwoName = "LitoSqlGateTwo";
        Protocol.CustomCharacterRequest request = new Protocol.CustomCharacterRequest(
                name, 0, 1, List.of(18, 13, 15, 9, 12, 8), 0, 0);
        try (AccountRepository accounts = AccountRepository.open(
                ServerConfig.load(writableConfig))) {
            accounts.healthCheck();
            String accountProbe = "LitoAcctGate";
            String firstPassword = "FirstPass1";
            String secondPassword = "SecondPass2";
            for (String route : List.of("380 角色庫 1", "380 角色庫 2")) {
                accounts.repositoryForClassicSource(route)
                        .deleteLitoAccount(accountProbe);
            }
            assertEquals(AccountMutationResult.SUCCESS,
                    accounts.createAccount(accountProbe, firstPassword));
            assertEquals(Set.of("380 角色庫 1", "380 角色庫 2"),
                    accounts.authenticate(accountProbe, firstPassword)
                            .authorizedClassicSources());
            for (String route : List.of("380 角色庫 1", "380 角色庫 2")) {
                AccountRepository routed = accounts.repositoryForClassicSource(route);
                assertTrue(routed.accountExists(accountProbe));
                assertTrue(routed.authenticate(accountProbe, firstPassword).accepted());
            }
            assertEquals(AccountMutationResult.SUCCESS,
                    accounts.changePassword(accountProbe, firstPassword, secondPassword));
            assertEquals(Set.of("380 角色庫 1", "380 角色庫 2"),
                    accounts.authenticate(accountProbe, secondPassword)
                            .authorizedClassicSources());
            assertTrue(accounts.deleteLitoAccount(accountProbe));
            assertFalse(accounts.accountExists(accountProbe));

            AccountRepository source = accounts.repositoryForClassicSource("380 角色庫 1");
            AccountRepository routeTwo =
                    accounts.repositoryForClassicSource("380 角色庫 2");
            RepositoryCharacterMutation stale =
                    source.deleteLitoCharacter("2222", name);
            assertTrue(stale.status() == RepositoryCharacterMutationStatus.NOT_OWNED
                    || stale.status() == RepositoryCharacterMutationStatus.SUCCESS);
            RepositoryCharacterMutation staleRouteTwo =
                    routeTwo.deleteLitoCharacter("2222", routeTwoName);
            assertTrue(staleRouteTwo.status() == RepositoryCharacterMutationStatus.NOT_OWNED
                    || staleRouteTwo.status() == RepositoryCharacterMutationStatus.SUCCESS);

            RepositoryCharacterMutation created =
                    source.createLitoCharacter("2222", request);
            assertEquals(RepositoryCharacterMutationStatus.SUCCESS, created.status());
            assertNotNull(created.character());
            assertTrue(created.character().objectId() > 0);
            assertTrue(source.characters("2222").stream()
                    .anyMatch(character -> character.name().equals(name)));
            assertTrue(source.sourceInventory(created.character().objectId()).stream()
                    .anyMatch(item -> item.catalogId()
                            == InventoryCatalog.PC_ACTION_HELPER));
            RepositoryCharacterMutation routeTwoCreated = routeTwo.createLitoCharacter(
                    "2222", new Protocol.CustomCharacterRequest(
                            routeTwoName, 0, 1, List.of(18, 13, 15, 9, 12, 8),
                            0, 0));
            assertEquals(RepositoryCharacterMutationStatus.SUCCESS,
                    routeTwoCreated.status());

            LitoCharacterPersistence persistence = new LitoCharacterPersistence(
                    "2222", name, created.character().objectId(),
                    47, 9, 12, 123456, 321,
                    2, 1_788_000_000L, 1, 1_788_000_100L,
                    77, 8, 3, 33000, 33001, 4, 6, 2,
                    java.util.Set.of(1, 4, 14), java.util.Map.of(570001, 3, 570002, 1));
            assertEquals(RepositoryPersistenceResult.SUCCESS,
                    source.persistLitoCharacter(persistence));
            CharacterSummary reloaded = source.characters("2222").stream()
                    .filter(character -> character.name().equals(name))
                    .findFirst().orElseThrow();
            assertEquals(12, reloaded.level());
            assertEquals(123456, reloaded.experience());
            assertEquals(321, reloaded.lawful());
            assertEquals(33000, reloaded.locationX());
            assertEquals(33001, reloaded.locationY());
            assertEquals(java.util.Set.of(1, 4, 14),
                    source.learnedSkills(created.character().objectId()));
            assertEquals(java.util.Map.of(570001, 3, 570002, 1),
                    source.questSteps(created.character().objectId()));
            assertEquals(77, source.hellTime(created.character().objectId()));
            assertEquals(new SourceKillDeathCounts(8, 3),
                    source.killDeathCounts(created.character().objectId()));

            Properties sqlProperties = new Properties();
            try (Reader reader = LocalResources.newBufferedReader(
                    Path.of(properties.getProperty("database.connection.properties")),
                    StandardCharsets.UTF_8)) {
                sqlProperties.load(reader);
            }
            String jdbcUrl = sqlProperties.getProperty("URL1_LOGIN") + "380_ac_1"
                    + sqlProperties.getProperty("URL3_LOGIN", "");
            String jdbcLogin = sqlProperties.getProperty(
                    "Login_LOGIN", sqlProperties.getProperty("Login"));
            String jdbcPassword = sqlProperties.getProperty(
                    "Password_LOGIN", sqlProperties.getProperty("Password", ""));

            // A real dynamic-route row must retain values beyond every native
            // LiTo field width.  The directory remains byte/word safe, while
            // negotiated Schema 2 emits the same authoritative values as dwords.
            try {
                try (Connection connection = DriverManager.getConnection(
                        jdbcUrl, jdbcLogin, jdbcPassword);
                     PreparedStatement update = connection.prepareStatement(
                             "UPDATE characters SET level=345,MaxHp=100000,CurHp=99999,"
                                     + "MaxMp=80000,CurMp=70000,Ac=-500,Str=301,Con=302,"
                                     + "Dex=303,Cha=304,Intel=305,Wis=306 WHERE objid=?")) {
                    update.setLong(1, created.character().objectId());
                    assertEquals(1, update.executeUpdate());
                }
                CharacterSummary wide = source.characters("2222").stream()
                        .filter(character -> character.name().equals(name))
                        .findFirst().orElseThrow();
                assertEquals(345, wide.level());
                assertEquals(100_000, wide.maxHp());
                assertEquals(80_000, wide.maxMp());
                assertEquals(-500, wide.armorClass());
                assertEquals(List.of(301, 302, 303, 304, 305, 306), List.of(
                        wide.strength(), wide.constitution(), wide.dexterity(),
                        wide.charisma(), wide.intelligence(), wide.wisdom()));

                NativeCharacterStats.DirectoryProjection directory =
                        NativeCharacterStats.directoryProjection(wide);
                assertEquals(Short.MAX_VALUE, directory.maxHp());
                assertEquals(Short.MAX_VALUE, directory.maxMp());
                assertEquals(Byte.MIN_VALUE, directory.armorClass());
                assertEquals(Byte.MAX_VALUE, directory.level());
                assertTrue(directory.adjusted(wide));

                String previousSchema = System.getProperty("lito.numeric.breakthrough");
                try {
                    System.setProperty("lito.numeric.breakthrough", "2");
                    Protocol.Reader status = new Protocol.Reader(Protocol.characterStatus(
                            wide.objectId(), wide.level(), wide.experience(),
                            wide.strength(), wide.constitution(), wide.dexterity(),
                            wide.charisma(), wide.intelligence(), wide.wisdom(),
                            99_999, 70_000, wide.maxHp(), wide.maxMp(),
                            wide.armorClass(), wide.food(), 0, (int) wide.lawful(),
                            0, 0, 0, 0));
                    assertEquals(Protocol.S_STATUS, status.c());
                    assertEquals(wide.objectId(), status.d());
                    assertEquals(0xFF, status.c());
                    assertEquals(wide.experience(), status.d());
                    for (int value : List.of(301, 305, 306, 303, 302, 304)) {
                        assertEquals(value, status.d());
                    }
                    assertEquals(99_999, status.d());
                    assertEquals(100_000, status.d());
                    assertEquals(70_000, status.d());
                    assertEquals(80_000, status.d());
                    assertEquals(Integer.toUnsignedLong(-500), status.d());
                } finally {
                    if (previousSchema == null) {
                        System.clearProperty("lito.numeric.breakthrough");
                    } else {
                        System.setProperty("lito.numeric.breakthrough", previousSchema);
                    }
                }
            } finally {
                try (Connection connection = DriverManager.getConnection(
                        jdbcUrl, jdbcLogin, jdbcPassword);
                     PreparedStatement restore = connection.prepareStatement(
                             "UPDATE characters SET level=?,MaxHp=?,CurHp=?,MaxMp=?,CurMp=?,"
                                     + "Ac=?,Str=?,Con=?,Dex=?,Cha=?,Intel=?,Wis=? WHERE objid=?")) {
                    restore.setInt(1, reloaded.level());
                    restore.setInt(2, reloaded.maxHp());
                    restore.setInt(3, reloaded.maxHp());
                    restore.setInt(4, reloaded.maxMp());
                    restore.setInt(5, reloaded.maxMp());
                    restore.setInt(6, reloaded.armorClass());
                    restore.setInt(7, reloaded.strength());
                    restore.setInt(8, reloaded.constitution());
                    restore.setInt(9, reloaded.dexterity());
                    restore.setInt(10, reloaded.charisma());
                    restore.setInt(11, reloaded.intelligence());
                    restore.setInt(12, reloaded.wisdom());
                    restore.setLong(13, created.character().objectId());
                    assertEquals(1, restore.executeUpdate());
                }
            }
            long itemObjectId;
            try (Connection connection = DriverManager.getConnection(
                    jdbcUrl, jdbcLogin, jdbcPassword)) {
                itemObjectId = allocateObjectId(connection);
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO character_items "
                                + "(id,item_id,char_id,item_name,count,is_equipped,is_sealed,"
                                + "enchantlvl,is_id,durability,charge_count,bless) "
                                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)")) {
                    insert.setLong(1, itemObjectId);
                    insert.setInt(2, 40308);
                    insert.setLong(3, created.character().objectId());
                    insert.setString(4, "LiTo persistence probe");
                    insert.setInt(5, 10);
                    insert.setBoolean(6, false);
                    insert.setBoolean(7, false);
                    insert.setInt(8, 0);
                    insert.setBoolean(9, false);
                    insert.setInt(10, 0);
                    insert.setInt(11, 0);
                    insert.setInt(12, 1);
                    insert.executeUpdate();
                }
            }
            long expiry = 1_788_000_200_000L;
            LitoSourceInventoryMutation itemMutation = new LitoSourceInventoryMutation(
                    itemObjectId, 40308, 7, false, true, true,
                    3, true, 2, 5, 1, expiry);
            assertEquals(RepositoryPersistenceResult.SUCCESS,
                    source.persistLitoSourceInventory(
                            created.character().objectId(), List.of(itemMutation)));
            try (Connection connection = DriverManager.getConnection(
                    jdbcUrl, jdbcLogin, jdbcPassword);
                 PreparedStatement query = connection.prepareStatement(
                         "SELECT i.count,i.is_equipped,i.is_sealed,i.enchantlvl,i.is_id,"
                                 + "i.durability,i.charge_count,i.bless,t.usertime "
                                 + "FROM character_items i JOIN character_items_time t "
                                 + "ON t.itemr_obj_id=i.id WHERE i.id=?")) {
                query.setLong(1, itemObjectId);
                try (ResultSet row = query.executeQuery()) {
                    assertTrue(row.next());
                    assertEquals(7, row.getInt("count"));
                    assertTrue(row.getBoolean("is_equipped"));
                    assertTrue(row.getBoolean("is_sealed"));
                    assertEquals(3, row.getInt("enchantlvl"));
                    assertTrue(row.getBoolean("is_id"));
                    assertEquals(2, row.getInt("durability"));
                    assertEquals(5, row.getInt("charge_count"));
                    assertEquals(1, row.getInt("bless"));
                    assertEquals(expiry, row.getTimestamp("usertime").getTime());
                }
            }
            assertEquals(RepositoryPersistenceResult.SUCCESS,
                    source.persistLitoSourceInventory(created.character().objectId(),
                            List.of(LitoSourceInventoryMutation.delete(
                                    itemObjectId, 40308))));

            long litoOwnedObjectId = 0x7000_F001L;
            long litoOwnedExpiry = 1_788_000_300_000L;
            LitoOwnedInventoryItem litoOwned = new LitoOwnedInventoryItem(
                    litoOwnedObjectId, 40308, "LiTo-owned Adena", 123,
                    false, true, 0, true, 0, 0, 1,
                    litoOwnedExpiry, true);
            assertEquals(RepositoryPersistenceResult.SUCCESS,
                    source.persistLitoInventory(
                            created.character().objectId(), List.of(), List.of(litoOwned)));
            SourceInventoryItem ownedReloaded = source.sourceInventory(
                            created.character().objectId()).stream()
                    .filter(item -> item.sourceObjectId() == litoOwnedObjectId)
                    .findFirst().orElseThrow();
            assertTrue(ownedReloaded.litoOwned());
            assertTrue(ownedReloaded.tournamentReward());
            assertEquals(123, ownedReloaded.quantity());
            assertEquals(litoOwnedExpiry, ownedReloaded.expiresAtEpochMillis());
            try (Connection connection = DriverManager.getConnection(
                    jdbcUrl, jdbcLogin, jdbcPassword);
                 PreparedStatement query = connection.prepareStatement(
                         "SELECT o.char_obj_id,o.catalog_id,o.tournament_reward,i.count "
                                 + "FROM lito_item_origin o JOIN character_items i "
                                 + "ON i.id=o.item_obj_id WHERE o.item_obj_id=?")) {
                query.setLong(1, litoOwnedObjectId);
                try (ResultSet row = query.executeQuery()) {
                    assertTrue(row.next());
                    assertEquals(created.character().objectId(), row.getLong("char_obj_id"));
                    assertEquals(40308, row.getInt("catalog_id"));
                    assertTrue(row.getBoolean("tournament_reward"));
                    assertEquals(123, row.getInt("count"));
                }
            }
            LitoOwnedInventoryItem updatedOwned = new LitoOwnedInventoryItem(
                    litoOwnedObjectId, 40308, "LiTo-owned Adena", 77,
                    false, false, 0, true, 0, 0, 1,
                    0, true);
            assertEquals(RepositoryPersistenceResult.SUCCESS,
                    source.persistLitoInventory(created.character().objectId(),
                            List.of(), List.of(updatedOwned)));
            assertEquals(77, source.sourceInventory(created.character().objectId()).stream()
                    .filter(item -> item.sourceObjectId() == litoOwnedObjectId)
                    .findFirst().orElseThrow().quantity());
            assertEquals(RepositoryPersistenceResult.SUCCESS,
                    source.persistLitoInventory(
                            created.character().objectId(), List.of(), List.of()));
            assertFalse(source.sourceInventory(created.character().objectId()).stream()
                    .anyMatch(item -> item.sourceObjectId() == litoOwnedObjectId));

            long sourcePetCollarId;
            long sourcePetObjectId;
            try (Connection connection = DriverManager.getConnection(
                    jdbcUrl, jdbcLogin, jdbcPassword)) {
                sourcePetCollarId = allocateObjectId(connection);
                sourcePetObjectId = allocateObjectId(connection);
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO character_items "
                                + "(id,item_id,char_id,item_name,count,is_equipped,is_sealed,"
                                + "enchantlvl,is_id,durability,charge_count,bless) "
                                + "VALUES (?,?,?,?,1,0,0,0,1,0,0,1)")) {
                    insert.setLong(1, sourcePetCollarId);
                    insert.setInt(2, (int) InventoryCatalog.PET_COLLAR);
                    insert.setLong(3, created.character().objectId());
                    insert.setString(4, "protected source pet collar");
                    insert.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO character_pets "
                                + "(item_obj_id,objid,npcid,name,lvl,hp,mp,exp,lawful) "
                                + "VALUES (?,?,?,?,?,?,?,?,?)")) {
                    insert.setLong(1, sourcePetCollarId);
                    insert.setLong(2, sourcePetObjectId);
                    insert.setInt(3, 45034);
                    insert.setString(4, "來源牧羊犬");
                    insert.setInt(5, 12);
                    insert.setInt(6, 78);
                    insert.setInt(7, 17);
                    insert.setLong(8, 21_375);
                    insert.setInt(9, 0);
                    insert.executeUpdate();
                }
            }
            LitoPetPersistence updatedSourcePet = new LitoPetPersistence(
                    0x7000_F030L, 0x9000_0030L,
                    (int) InventoryCatalog.PET_COLLAR, 45034,
                    "來源牧羊犬改名", 13, 84, 19, 24_000, 0,
                    false, sourcePetCollarId);
            assertEquals(RepositoryPersistenceResult.SUCCESS,
                    source.persistLitoInventory(created.character().objectId(),
                            List.of(), List.of(), List.of(updatedSourcePet), Set.of()));
            SourcePet updatedSourceReloaded = source.pets(
                            created.character().objectId()).stream()
                    .filter(candidate -> candidate.sourceCollarObjectId()
                            == sourcePetCollarId)
                    .findFirst().orElseThrow();
            assertFalse(updatedSourceReloaded.litoOwned());
            assertEquals(sourcePetObjectId,
                    updatedSourceReloaded.sourcePetObjectId());
            assertEquals("來源牧羊犬改名", updatedSourceReloaded.name());
            long evolvedCollarId = 0x7000_F011L;
            long evolvedPetId = 0x9000_0030L;
            LitoOwnedInventoryItem evolvedCollar = new LitoOwnedInventoryItem(
                    evolvedCollarId, (int) InventoryCatalog.HIGH_PET_COLLAR,
                    "LiTo evolved pet collar", 1, false, false, 0, true,
                    0, 0, 1, 0, false);
            LitoPetPersistence evolvedPet = new LitoPetPersistence(
                    evolvedCollarId, evolvedPetId,
                    (int) InventoryCatalog.HIGH_PET_COLLAR, 45687,
                    "來源牧羊犬改名", 1, 42, 9, 0, 0,
                    true, sourcePetCollarId);
            assertEquals(RepositoryPersistenceResult.SUCCESS,
                    source.persistLitoInventory(created.character().objectId(),
                            List.of(), List.of(evolvedCollar),
                            List.of(evolvedPet), Set.of()));
            assertFalse(source.sourceInventory(created.character().objectId()).stream()
                    .anyMatch(item -> item.sourceObjectId() == sourcePetCollarId));
            SourcePet evolvedReloaded = source.pets(
                            created.character().objectId()).stream()
                    .filter(candidate -> candidate.sourceCollarObjectId()
                            == evolvedCollarId)
                    .findFirst().orElseThrow();
            assertTrue(evolvedReloaded.litoOwned());
            assertEquals(evolvedPetId, evolvedReloaded.sourcePetObjectId());
            assertEquals((int) InventoryCatalog.HIGH_PET_COLLAR,
                    evolvedReloaded.collarCatalogId());
            assertEquals(RepositoryPersistenceResult.SUCCESS,
                    source.persistLitoInventory(created.character().objectId(),
                            List.of(), List.of(), List.of(), Set.of()));
            assertTrue(source.pets(created.character().objectId()).isEmpty());

            long petCollarObjectId = 0x7000_F010L;
            long petObjectId = 0x9000_0010L;
            LitoOwnedInventoryItem petCollar = new LitoOwnedInventoryItem(
                    petCollarObjectId, (int) InventoryCatalog.PET_COLLAR,
                    "LiTo-owned pet collar", 1, false, false, 0, true,
                    0, 0, 1, 0, false);
            LitoPetPersistence pet = new LitoPetPersistence(
                    petCollarObjectId, petObjectId,
                    (int) InventoryCatalog.PET_COLLAR, 45034,
                    "SQL牧羊犬", 12, 78, 17, 21_375, 0, true, 0);
            assertEquals(RepositoryPersistenceResult.SUCCESS,
                    source.persistLitoInventory(created.character().objectId(),
                            List.of(), List.of(petCollar), List.of(pet), Set.of()));
            SourcePet reloadedPet = source.pets(created.character().objectId()).stream()
                    .filter(candidate -> candidate.sourceCollarObjectId()
                            == petCollarObjectId)
                    .findFirst().orElseThrow();
            assertTrue(reloadedPet.litoOwned());
            assertEquals(petObjectId, reloadedPet.sourcePetObjectId());
            assertEquals("SQL牧羊犬", reloadedPet.name());
            LitoPetPersistence grownPet = new LitoPetPersistence(
                    petCollarObjectId, petObjectId,
                    (int) InventoryCatalog.PET_COLLAR, 45034,
                    "SQL牧羊犬改名", 13, 84, 19, 24_000, 0, true,
                    petCollarObjectId);
            assertEquals(RepositoryPersistenceResult.SUCCESS,
                    source.persistLitoInventory(created.character().objectId(),
                            List.of(), List.of(petCollar), List.of(grownPet), Set.of()));
            SourcePet grownReloaded = source.pets(created.character().objectId()).stream()
                    .filter(candidate -> candidate.sourceCollarObjectId()
                            == petCollarObjectId)
                    .findFirst().orElseThrow();
            assertEquals("SQL牧羊犬改名", grownReloaded.name());
            assertEquals(13, grownReloaded.level());
            assertEquals(24_000, grownReloaded.experience());
            assertEquals(RepositoryPersistenceResult.SUCCESS,
                    source.persistLitoInventory(created.character().objectId(),
                            List.of(), List.of(), List.of(), Set.of()));
            assertTrue(source.pets(created.character().objectId()).isEmpty());
            assertFalse(source.sourceInventory(created.character().objectId()).stream()
                    .anyMatch(item -> item.sourceObjectId() == petCollarObjectId));

            RepositoryPrivateInventorySnapshot privateBefore =
                    source.litoPrivateInventory(created.character().objectId());
            assertTrue(privateBefore.authoritative());
            long privateObjectId = 0x7000_F090L;
            LitoOwnedInventoryItem privateItem = new LitoOwnedInventoryItem(
                    privateObjectId, (int) InventoryCatalog.TRAINING_SWORD,
                    InventoryCatalog.require(InventoryCatalog.TRAINING_SWORD).name(),
                    1, false, true, 7, true, 2, 0,
                    RuntimeInventory.BLESSING_NORMAL, 1_788_000_900_000L, false);
            assertEquals(RepositoryPersistenceResult.SUCCESS,
                    source.persistLitoInventory(created.character().objectId(),
                            List.of(), List.of(privateItem), List.of(), Set.of(),
                            privateBefore));
            RepositoryPrivateInventorySnapshot privateReloaded =
                    source.litoPrivateInventory(created.character().objectId());
            assertEquals(privateBefore.snapshotVersion() + 1,
                    privateReloaded.snapshotVersion());
            assertEquals(List.of(privateItem), privateReloaded.items());
            assertFalse(source.sourceInventory(created.character().objectId()).stream()
                    .anyMatch(item -> item.sourceObjectId() == privateObjectId));
            LitoOwnedInventoryItem conflictingPrivate = new LitoOwnedInventoryItem(
                    privateObjectId, (int) InventoryCatalog.TRAINING_SWORD,
                    InventoryCatalog.require(InventoryCatalog.TRAINING_SWORD).name(),
                    1, false, false, 8, true, 1, 0,
                    RuntimeInventory.BLESSING_NORMAL, 0, true);
            assertEquals(RepositoryPersistenceResult.CONFLICT,
                    source.persistLitoInventory(created.character().objectId(),
                            List.of(), List.of(conflictingPrivate), List.of(), Set.of(),
                            privateBefore));
            assertEquals(List.of(privateItem),
                    source.litoPrivateInventory(created.character().objectId()).items());

            LitoOwnedInventoryItem routeTwoPrivate = new LitoOwnedInventoryItem(
                    0x7000_F091L, (int) InventoryCatalog.TRAINING_SWORD,
                    InventoryCatalog.require(InventoryCatalog.TRAINING_SWORD).name(),
                    1, false, false, 1, true, 0, 0,
                    RuntimeInventory.BLESSING_NORMAL, 0, true);
            RepositoryPrivateInventorySnapshot routeTwoBefore =
                    routeTwo.litoPrivateInventory(
                            routeTwoCreated.character().objectId());
            assertEquals(RepositoryPersistenceResult.SUCCESS,
                    routeTwo.persistLitoInventory(
                            routeTwoCreated.character().objectId(), List.of(),
                            List.of(routeTwoPrivate), List.of(), Set.of(), routeTwoBefore));
            assertEquals(List.of(routeTwoPrivate), routeTwo.litoPrivateInventory(
                    routeTwoCreated.character().objectId()).items());
            assertEquals(List.of(privateItem), source.litoPrivateInventory(
                    created.character().objectId()).items());

            Properties readOnlyProperties = new Properties();
            readOnlyProperties.putAll(properties);
            readOnlyProperties.setProperty("database.read.only", "true");
            Path readOnlyConfig = temporary.resolve("server-read-only.properties");
            try (Writer writer = Files.newBufferedWriter(
                    readOnlyConfig, StandardCharsets.UTF_8)) {
                readOnlyProperties.store(writer, "temporary read-only LiTo profile");
            }
            try (AccountRepository readOnlyAccounts = AccountRepository.open(
                    ServerConfig.load(readOnlyConfig))) {
                AccountRepository readOnlySource =
                        readOnlyAccounts.repositoryForClassicSource("380 角色庫 1");
                RepositoryPrivateInventorySnapshot readOnlySnapshot =
                        readOnlySource.litoPrivateInventory(
                                created.character().objectId());
                assertEquals(RepositoryPersistenceResult.READ_ONLY,
                        readOnlySource.persistLitoInventory(
                                created.character().objectId(), List.of(),
                                List.of(conflictingPrivate), List.of(), Set.of(),
                                readOnlySnapshot));
            }
            assertEquals(List.of(privateItem), source.litoPrivateInventory(
                    created.character().objectId()).items());

            LitoOwnedInventoryItem sharedReward = new LitoOwnedInventoryItem(
                    0x7000_F092L, (int) InventoryCatalog.ADENA, "Tournament Adena", 9,
                    false, false, 0, true, 0, 0,
                    RuntimeInventory.BLESSING_NORMAL, 0, true);
            LitoOwnedInventoryItem privateReward = new LitoOwnedInventoryItem(
                    0x7000_F093L, (int) InventoryCatalog.TRAINING_SWORD,
                    InventoryCatalog.require(InventoryCatalog.TRAINING_SWORD).name(),
                    1, false, false, 2, true, 1, 0,
                    RuntimeInventory.BLESSING_NORMAL, 0, true);
            RepositoryPrivateInventorySnapshot rewardBefore =
                    source.litoPrivateInventory(created.character().objectId());
            assertEquals(RepositoryPersistenceResult.SUCCESS,
                    source.persistTournamentRewardInventory(
                            created.character().objectId(), rewardBefore,
                            List.of(sharedReward, privateReward)));
            RepositoryPrivateInventorySnapshot rewardStored =
                    source.litoPrivateInventory(created.character().objectId());
            assertEquals(Set.of(privateItem, privateReward),
                    Set.copyOf(rewardStored.items()));
            assertTrue(source.sourceInventory(created.character().objectId()).stream()
                    .anyMatch(item -> item.sourceObjectId() == sharedReward.objectId()
                            && item.tournamentReward()));
            assertEquals(RepositoryPersistenceResult.CONFLICT,
                    source.persistTournamentRewardInventory(
                            created.character().objectId(), rewardBefore, List.of()));
            assertEquals(RepositoryPersistenceResult.SUCCESS,
                    source.persistTournamentRewardInventory(
                            created.character().objectId(), rewardStored, List.of()));
            assertEquals(List.of(privateItem), source.litoPrivateInventory(
                    created.character().objectId()).items());
            assertFalse(source.sourceInventory(created.character().objectId()).stream()
                    .anyMatch(item -> item.sourceObjectId() == sharedReward.objectId()));

            RepositoryCharacterMutation deleted =
                    source.deleteLitoCharacter("2222", name);
            assertEquals(RepositoryCharacterMutationStatus.SUCCESS, deleted.status());
            assertFalse(source.characters("2222").stream()
                    .anyMatch(character -> character.name().equals(name)));
            assertTrue(source.learnedSkills(created.character().objectId()).isEmpty());
            assertTrue(source.questSteps(created.character().objectId()).isEmpty());
            assertTrue(source.sourceInventory(created.character().objectId()).isEmpty());
            assertTrue(source.pets(created.character().objectId()).isEmpty());
            try (Connection connection = DriverManager.getConnection(
                    jdbcUrl, jdbcLogin, jdbcPassword);
                 PreparedStatement query = connection.prepareStatement(
                         "SELECT COUNT(*) FROM lito_private_inventory WHERE char_obj_id=?")) {
                query.setLong(1, created.character().objectId());
                try (ResultSet row = query.executeQuery()) {
                    assertTrue(row.next());
                    assertEquals(0, row.getInt(1));
                }
            }
            assertEquals(RepositoryCharacterMutationStatus.NOT_OWNED,
                    source.deleteLitoCharacter("2222", name).status());
            assertEquals(RepositoryCharacterMutationStatus.SUCCESS,
                    routeTwo.deleteLitoCharacter("2222", routeTwoName).status());
        }
    }

    private static long allocateObjectId(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE server_info SET maxid=LAST_INSERT_ID("
                    + "GREATEST(COALESCE(maxid,0),COALESCE(minid,10000),"
                    + "COALESCE((SELECT MAX(id) FROM character_items),0),"
                    + "COALESCE((SELECT MAX(objid) FROM character_pets),0))+1) WHERE id=1");
            try (ResultSet row = statement.executeQuery("SELECT LAST_INSERT_ID()")) {
                if (!row.next()) throw new IllegalStateException("object id was not allocated");
                return row.getLong(1);
            }
        }
    }
}

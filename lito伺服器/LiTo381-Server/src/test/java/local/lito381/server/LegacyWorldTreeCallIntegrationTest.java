package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Read-only proof for native skill 131 World Tree's Call. */
final class LegacyWorldTreeCallIntegrationTest {
    private static final String STATIC_NPC_COLUMNS = String.join("\t",
            "spawn_id", "npc_id", "x", "y", "heading", "movement_distance",
            "name_id", "impl", "gfx_id", "level", "hp", "mp", "exp", "lawful",
            "light_size", "passispeed", "atkspeed", "change_head",
            "normal_action", "chaotic_action");
    private static final String MONSTER_COLUMNS = String.join("\t",
            "spawn_id", "location", "count", "npc_id", "group_id",
            "locx", "locy", "randomx", "randomy",
            "locx1", "locy1", "locx2", "locy2", "heading",
            "min_respawn", "max_respawn", "respawn_screen", "movement_distance",
            "rest", "near_spawn", "name_id", "impl", "gfx_id", "level", "hp",
            "mp", "ac", "str", "con", "dex", "wis", "intel", "mr", "exp",
            "lawful", "size", "weak_attr", "ranged", "passispeed", "atkspeed",
            "bow_act_id",
            "undead", "poison_atk", "paralysis_atk", "agro", "damage_reduction",
            "hard", "doppel", "boss", "light_size", "change_head");
    private static final String DROP_COLUMNS = "mob_id\titem_id\tmin\tmax\tchance";
    private static final String DROP_ITEM_COLUMNS = String.join("\t",
            "item_id", "kind", "name_id", "use_token", "inventory_gfx",
            "ground_gfx", "blessing", "stackable", "tradable", "unit_weight",
            "item_description_id", "weapon_type", "damage_small", "damage_large",
            "material", "safe_enchant", "class_mask");
    private static final String PHANTASM_TELEPORT_COLUMNS = String.join("\t",
            "source_map_id", "spawn_id", "npc_id", "source_x", "source_y",
            "initial_html", "route_id", "action_name", "destination_x",
            "destination_y", "destination_map_id", "heading", "price_amount",
            "price_item_id", "min_level", "max_level");
    private static final String ENABLED_TELEPORT_COLUMNS = String.join("\t",
            "source_map_id", "spawn_id", "spawn_count", "npc_id", "source_x",
            "source_y", "initial_html", "route_id", "action_name",
            "destination_x", "destination_y", "destination_map_id", "heading",
            "price_amount", "price_item_id", "min_level", "max_level");
    private static final String MAP350_FLAGS_COLUMNS = String.join("\t",
            "map_id", "location_name", "start_x", "end_x", "start_y", "end_y",
            "escapable", "usable_item", "usable_skill", "teleportable",
            "take_pets", "recall_pets");
    private static final String GETBACK_COLUMNS = String.join("\t",
            "area_x1", "area_y1", "area_x2", "area_y2", "area_mapid",
            "getback_x1", "getback_y1", "getback_x2", "getback_y2",
            "getback_x3", "getback_y3", "getback_mapid", "getback_townid",
            "getback_townid_elf", "getback_townid_darkelf", "scrollescape", "note");
    private static final String PIRATE_MERCHANT_COLUMNS = String.join("\t",
            "map_id", "spawn_id", "spawn_count", "npc_id", "x", "y", "heading",
            "movement_distance", "name_id", "implementation", "gfx_id",
            "normal_action", "chaotic_action", "teleport_url", "item_id", "order_id",
            "selling_price", "pack_count", "purchasing_price", "daily_buy_limit",
            "item_name_id", "item_description_id", "use_type", "item_type", "material",
            "weight", "inventory_gfx", "ground_gfx", "stackable", "trade", "blessing",
            "delay_id", "delay_time");
    private static final String PIRATE_PORTAL_COLUMNS = String.join("\t",
            "source_map_id", "source_x", "source_y", "destination_map_id",
            "destination_x", "destination_y", "heading", "required_item_id", "note");
    private static final String PIRATE_PORTAL_SECONDARY_COLUMNS = String.join("\t",
            "source_map_id", "source_x", "source_y", "destination_map_id",
            "destination_x", "destination_y", "heading", "note");

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void everyRetainedSourceHasTheExactWorldTreeCallContract() throws Exception {
        String configPath = System.getProperty("lito.legacy.config",
                "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            int verified = 0;
            for (String source : config.classicServerNames()) {
                AccountRepository repository =
                        routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                assertTrue(database.matches("[A-Za-z0-9_]+"));
                boolean effectSkillId = hasColumn(
                        repository, database, "skills", "effect_skill_id");
                try (Connection connection = connection(repository);
                     PreparedStatement statement = connection.prepareStatement(
                             "SELECT * FROM " + database
                                     + ".`skills` WHERE skill_id=131");
                     ResultSet row = statement.executeQuery()) {
                    assertTrue(row.next(), "missing exact skill131 row");
                    assertEquals(131, row.getInt("skill_id"));
                    assertEquals("世界樹的呼喚", row.getString("name"));
                    assertEquals(17, row.getInt("skill_level"));
                    assertEquals(2, row.getInt("skill_number"));
                    assertEquals(10, row.getInt("mpConsume"));
                    assertEquals(0, row.getInt("hpConsume"));
                    assertEquals(0, row.getInt("itemConsumeId"));
                    assertEquals(0, row.getInt("itemConsumeCount"));
                    assertEquals(0, row.getInt("reuseDelay"));
                    assertEquals(0, row.getInt("buffDuration"));
                    assertEquals("none", row.getString("target"));
                    assertEquals(0, row.getInt("target_to"));
                    assertEquals(0, row.getInt("damage_value"));
                    assertEquals(0, row.getInt("damage_dice"));
                    assertEquals(0, row.getInt("damage_dice_count"));
                    assertEquals(0, row.getInt("probability_value"));
                    assertEquals(0, row.getInt("probability_dice"));
                    assertEquals(0, row.getInt("attr"));
                    assertEquals(128, row.getInt("type"));
                    assertEquals(0, row.getInt("lawful"));
                    assertEquals(0, row.getInt("ranged"));
                    assertEquals(0, row.getInt("area"));
                    assertEquals(0, row.getInt("through"));
                    assertEquals(4, row.getInt("id"));
                    assertEquals("", row.getString("nameid"));
                    assertEquals(19, row.getInt("action_id"));
                    assertEquals(169, row.getInt("castgfx"));
                    assertEquals(0, row.getInt("castgfx2"));
                    assertEquals(0, row.getInt("sysmsgID_happen"));
                    assertEquals(0, row.getInt("sysmsgID_stop"));
                    assertEquals(0, row.getInt("sysmsgID_fail"));
                    if (effectSkillId) {
                        assertEquals(0, row.getInt("effect_skill_id"));
                    }
                    assertFalse(row.next(), "duplicate skill131 row");
                }
                try (Connection connection = connection(repository);
                     PreparedStatement statement = connection.prepareStatement(
                             "SELECT COUNT(*) FROM " + database
                                     + ".`skills_item` WHERE skill_id=131"
                                     + " AND name='世界樹的呼喚' AND itemids IS NULL"
                                     + " AND counts IS NULL");
                     ResultSet row = statement.executeQuery()) {
                    assertTrue(row.next());
                    assertEquals(1, row.getInt(1));
                }
                verified++;
                System.out.printf("[LiTo-test] source=%s World-Tree-row=exact"
                                + " schema=%s selector=16/2 MP10 type128 id4"
                                + " action19 destination=4:33051,32337/5 effect169%n",
                        source, database);
            }
            assertEquals(config.classicServerNames().size(), verified);
        }
        assertRetainedJavaSemantics();
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void selectedMap4StaticNpcResourceMatchesItsReadOnlyDatabaseSource()
            throws Exception {
        String configPath = System.getProperty("lito.legacy.config",
                "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        Map<String, SourceEvidence> observed = new LinkedHashMap<>();
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            for (String source : config.classicServerNames()) {
                AccountRepository repository =
                        routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                assertTrue(database.matches("[A-Za-z0-9_]+"));
                try (Connection connection = connection(repository)) {
                    SourceEvidence evidence = staticNpcEvidence(connection, database);
                    observed.put(database, evidence);
                    System.out.printf("[LiTo-test] source=%s map4-static-npcs=%d"
                                    + " schema=%s sha256=%s read-only=true%n",
                            source, evidence.rows(), database, evidence.sha256());
                }
            }
        }

        SourceEvidence selected = observed.get("380_db");
        SourceEvidence secondary = observed.get("380yiwei_db");
        assertEquals(new SourceEvidence(641,
                        ClassicWorldNpcCatalog.MAP4_SOURCE_SHA256), selected);
        assertEquals(new SourceEvidence(678,
                        "30B513BA5475507A57329BC9FF2126D81617CDC94ADCBDD419CFACD59296CE9A"),
                secondary);
        assertNotEquals(selected, secondary,
                "selected and older map4 static-NPC sources must remain distinct");
        assertEquals(Map.of(
                "380_db", selected,
                "380yiwei_db", secondary), observed);
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void selectedMap4MonsterResourceMatchesItsReadOnlyDatabaseSource()
            throws Exception {
        String configPath = System.getProperty("lito.legacy.config",
                "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        Map<String, MonsterSourceEvidence> observed = new LinkedHashMap<>();
        Map<String, ProjectileSourceEvidence> projectileObserved =
                new LinkedHashMap<>();
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            for (String source : config.classicServerNames()) {
                AccountRepository repository =
                        routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                assertTrue(database.matches("[A-Za-z0-9_]+"));
                try (Connection connection = connection(repository)) {
                    MonsterSourceEvidence evidence = monsterEvidence(connection, database);
                    observed.put(database, evidence);
                    ProjectileSourceEvidence projectile =
                            projectileEvidence(connection, database);
                    projectileObserved.put(database, projectile);
                    System.out.printf("[LiTo-test] source=%s map4-monster-definitions=%d"
                                    + " population=%d projectile-groups=%d"
                                    + " projectile-population=%d projectile-templates=%d"
                                    + " schema=%s sha256=%s read-only=true%n",
                            source, evidence.rows(), evidence.population(),
                            projectile.groups(), projectile.population(),
                            projectile.templates(), database, evidence.sha256());
                }
            }
        }

        MonsterSourceEvidence selected = observed.get("380_db");
        MonsterSourceEvidence secondary = observed.get("380yiwei_db");
        assertEquals(new MonsterSourceEvidence(729, 5_984,
                        ClassicWorldMonsterCatalog.MAP4_SOURCE_SHA256), selected);
        assertEquals(new MonsterSourceEvidence(509, 5_930,
                        "85FC330624073D83819BA266572AD1145036A05459F3F7C2656C52F73836C05A"),
                secondary);
        assertNotEquals(selected, secondary,
                "selected and older map4 monster sources must remain distinct");
        assertEquals(Map.of(
                "380_db", selected,
                "380yiwei_db", secondary), observed);
        assertEquals(Map.of(
                "380_db", new ProjectileSourceEvidence(33, 505, 8),
                "380yiwei_db", new ProjectileSourceEvidence(33, 505, 8)),
                projectileObserved);
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void selectedMap303AndPhantasmTeleportMatchReadOnlyDatabaseSources()
            throws Exception {
        String configPath = System.getProperty("lito.legacy.config",
                "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        Map<String, MonsterSourceEvidence> monsters = new LinkedHashMap<>();
        Map<String, DropSourceEvidence> drops = new LinkedHashMap<>();
        Map<String, SourceEvidence> routes = new LinkedHashMap<>();
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            for (String source : config.classicServerNames()) {
                AccountRepository repository =
                        routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                assertTrue(database.matches("[A-Za-z0-9_]+"));
                try (Connection connection = connection(repository)) {
                    MonsterSourceEvidence monster =
                            monsterEvidence(connection, database, 303);
                    DropSourceEvidence drop = dropEvidence(connection, database, 303);
                    SourceEvidence route = phantasmTeleportEvidence(connection, database);
                    monsters.put(database, monster);
                    drops.put(database, drop);
                    routes.put(database, route);
                    System.out.printf("[LiTo-test] source=%s map303-monster-definitions=%d"
                                    + " population=%d drops=%d items=%d route-rows=%d"
                                    + " schema=%s monster-sha256=%s drop-sha256=%s"
                                    + " route-sha256=%s read-only=true%n",
                            source, monster.rows(), monster.population(), drop.rows(),
                            drop.items(), route.rows(), database, monster.sha256(),
                            drop.sha256(), route.sha256());
                }
            }
        }

        assertEquals(new MonsterSourceEvidence(45, 829,
                ClassicWorldMonsterCatalog.MAP303_SOURCE_SHA256),
                monsters.get("380_db"));
        assertEquals(new MonsterSourceEvidence(45, 829,
                "98954A41C311ED1D8A436D3F0B93453C26888CB5B365B8D729C4B143B31AE6A5"),
                monsters.get("380yiwei_db"));
        assertEquals(new DropSourceEvidence(220, 29,
                ClassicWorldDropCatalog.MAP303_SOURCE_SHA256), drops.get("380_db"));
        assertEquals(new DropSourceEvidence(128, 17,
                "A2724625962FF4697A79DFFB748BD31FA630E6216010CDA5F097BD5909736EA0"),
                drops.get("380yiwei_db"));
        assertEquals(Map.of(
                "380_db", new SourceEvidence(1,
                        ClassicWorldTeleportCatalog.PHANTASM_SOURCE_SHA256),
                "380yiwei_db", new SourceEvidence(1,
                        ClassicWorldTeleportCatalog.PHANTASM_SOURCE_SHA256)), routes);
        assertNotEquals(monsters.get("380_db"), monsters.get("380yiwei_db"));
        assertNotEquals(drops.get("380_db"), drops.get("380yiwei_db"));
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void selectedGiranTradeZoneFlowMatchesEnabledAndMapWideReadOnlySources()
            throws Exception {
        String configPath = System.getProperty("lito.legacy.config",
                "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        Map<String, SourceEvidence> staticNpcs = new LinkedHashMap<>();
        Map<String, SourceEvidence> routes = new LinkedHashMap<>();
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            for (String source : config.classicServerNames()) {
                AccountRepository repository = routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                try (Connection connection = connection(repository)) {
                    SourceEvidence npc = staticNpcEvidence(connection, database, 350);
                    SourceEvidence route = giranTradeTeleportEvidence(
                            connection, database);
                    staticNpcs.put(database, npc);
                    routes.put(database, route);

                    StringBuilder flags = new StringBuilder(
                            MAP350_FLAGS_COLUMNS).append('\n');
                    assertEquals(1, appendCanonicalRows(connection,
                            "SELECT mapid,locationname,startX,endX,startY,endY,"
                            + "escapable,usable_item,usable_skill,teleportable,"
                            + "take_pets,recall_pets FROM " + database
                            + ".`mapids` WHERE mapid=350", 12, flags));
                    assertEquals(ClassicWorldMapCatalog.MAP350_FLAGS_SOURCE_SHA256,
                            sha256(flags.toString()));

                    StringBuilder getback = new StringBuilder(
                            GETBACK_COLUMNS).append('\n');
                    assertEquals(1, appendCanonicalRows(connection,
                            "SELECT area_x1,area_y1,area_x2,area_y2,area_mapid,"
                            + "getback_x1,getback_y1,getback_x2,getback_y2,"
                            + "getback_x3,getback_y3,getback_mapid,getback_townid,"
                            + "getback_townid_elf,getback_townid_darkelf,"
                            + "scrollescape,note FROM " + database
                            + ".`getback` WHERE area_mapid=350 ORDER BY area_x1 DESC",
                            17, getback));
                    assertEquals(ClassicWorldGetbackCatalog.MAP350_SOURCE_SHA256,
                            sha256(getback.toString()));

                    try (PreparedStatement statement = connection.prepareStatement(
                            "SELECT COUNT(*) FROM " + database
                                    + ".`npcaction` WHERE npcid IN (60010,60011)"
                                    + " AND normal_action='storage'"
                                    + " AND caotic_action='storage'"
                                    + " AND teleport_url='storage'");
                         ResultSet row = statement.executeQuery()) {
                        assertTrue(row.next());
                        assertEquals(2, row.getInt(1));
                    }

                    System.out.printf("[LiTo-test] source=%s map350-static=%d"
                                    + " route181-enabled=%d flags=escape/item/skill/teleport"
                                    + " getback-town=7 read-only=true%n",
                            source, npc.rows(), route.rows());
                }
            }
        }

        SourceEvidence exactStatic = new SourceEvidence(
                ClassicWorldNpcCatalog.MAP350_ROW_COUNT,
                ClassicWorldNpcCatalog.MAP350_SOURCE_SHA256);
        assertEquals(exactStatic, staticNpcs.get("380_db"));
        assertEquals(exactStatic, staticNpcs.get("380yiwei_db"));
        assertEquals(new SourceEvidence(2,
                        ClassicWorldTeleportCatalog.GIRAN_TRADE_SOURCE_SHA256),
                routes.get("380_db"));
        assertEquals(new SourceEvidence(0,
                        "B23BCA8C4E88CC36FE68B6A74DDC7DA06B7C2D465203724784B994739941283E"),
                routes.get("380yiwei_db"));
        assertRetainedNpcTeleportSemantics();
        assertRetainedWarehouseSemantics();
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void selectedOrcElvenDungeonFlowLocksBothReadOnlySources() throws Exception {
        String configPath = System.getProperty("lito.legacy.config",
                "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        Map<String, MonsterSourceEvidence> monsters = new LinkedHashMap<>();
        Map<String, DropSourceEvidence> drops = new LinkedHashMap<>();
        Map<String, SourceEvidence> routes = new LinkedHashMap<>();
        Map<String, SourceEvidence> flags = new LinkedHashMap<>();
        Map<String, SourceEvidence> getbacks = new LinkedHashMap<>();
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            for (String source : config.classicServerNames()) {
                AccountRepository repository = routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                try (Connection connection = connection(repository)) {
                    MonsterSourceEvidence monster = monsterEvidence(
                            connection, database, 21);
                    DropSourceEvidence drop = dropEvidence(connection, database, 21);
                    SourceEvidence route = orcTeleportEvidence(connection, database);
                    StringBuilder mapFlags = new StringBuilder(
                            MAP350_FLAGS_COLUMNS).append('\n');
                    int flagRows = appendCanonicalRows(connection,
                            "SELECT mapid,locationname,startX,endX,startY,endY,"
                            + "escapable,usable_item,usable_skill,teleportable,"
                            + "take_pets,recall_pets FROM " + database
                            + ".`mapids` WHERE mapid=21", 12, mapFlags);
                    StringBuilder getback = new StringBuilder(
                            GETBACK_COLUMNS).append('\n');
                    int getbackRows = appendCanonicalRows(connection,
                            "SELECT area_x1,area_y1,area_x2,area_y2,area_mapid,"
                            + "getback_x1,getback_y1,getback_x2,getback_y2,"
                            + "getback_x3,getback_y3,getback_mapid,getback_townid,"
                            + "getback_townid_elf,getback_townid_darkelf,"
                            + "scrollescape,note FROM " + database
                            + ".`getback` WHERE area_mapid=21 ORDER BY area_x1 DESC",
                            17, getback);
                    monsters.put(database, monster);
                    drops.put(database, drop);
                    routes.put(database, route);
                    flags.put(database, new SourceEvidence(
                            flagRows, sha256(mapFlags.toString())));
                    getbacks.put(database, new SourceEvidence(
                            getbackRows, sha256(getback.toString())));
                    System.out.printf("[LiTo-test] source=%s map21-monsters=%d"
                                    + " population=%d drops=%d items=%d route769-771=%d"
                                    + " flags=%s getback=%s read-only=true%n",
                            source, monster.rows(), monster.population(), drop.rows(),
                            drop.items(), route.rows(), flags.get(database).sha256(),
                            getbacks.get(database).sha256());
                }
            }
        }

        assertEquals(new MonsterSourceEvidence(6, 30,
                        ClassicWorldMonsterCatalog.MAP21_SOURCE_SHA256),
                monsters.get("380_db"));
        assertEquals(new MonsterSourceEvidence(6, 30,
                        "FD0EBA1BEF515C373E83E3E8C916AE98A6DDF8575BA887A52340E7E3605C9506"),
                monsters.get("380yiwei_db"));
        assertEquals(new DropSourceEvidence(17, 11,
                        ClassicWorldDropCatalog.MAP21_SOURCE_SHA256),
                drops.get("380_db"));
        assertEquals(new DropSourceEvidence(16, 10,
                        "E6AB0DBC6583259F4F59C1A24287181C8C5616CEE6DBAA04F1055F24682C8F8A"),
                drops.get("380yiwei_db"));
        SourceEvidence exactRoutes = new SourceEvidence(3,
                ClassicWorldTeleportCatalog.ORC_ROUTES_SOURCE_SHA256);
        assertEquals(exactRoutes, routes.get("380_db"));
        assertEquals(exactRoutes, routes.get("380yiwei_db"));
        assertEquals(new SourceEvidence(1,
                        ClassicWorldMapCatalog.MAP21_FLAGS_SOURCE_SHA256),
                flags.get("380_db"));
        assertEquals(new SourceEvidence(1,
                        "646F65073BA2EA557967A3E081C36D63A60C8E650687D03F4382F4C5CEDC551D"),
                flags.get("380yiwei_db"));
        SourceEvidence exactGetback = new SourceEvidence(1,
                ClassicWorldGetbackCatalog.MAP21_SOURCE_SHA256);
        assertEquals(exactGetback, getbacks.get("380_db"));
        assertEquals(exactGetback, getbacks.get("380yiwei_db"));
        assertNotEquals(monsters.get("380_db"), monsters.get("380yiwei_db"));
        assertNotEquals(drops.get("380_db"), drops.get("380yiwei_db"));
        assertNotEquals(flags.get("380_db"), flags.get("380yiwei_db"));
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void selectedSleepingDragonFlowLocksBothReadOnlySources() throws Exception {
        String configPath = System.getProperty("lito.legacy.config",
                "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        Map<String, MonsterSourceEvidence> monsters19 = new LinkedHashMap<>();
        Map<String, MonsterSourceEvidence> monsters20 = new LinkedHashMap<>();
        Map<String, DropSourceEvidence> drops19 = new LinkedHashMap<>();
        Map<String, DropSourceEvidence> drops20 = new LinkedHashMap<>();
        Map<String, SourceEvidence> static19 = new LinkedHashMap<>();
        Map<String, SourceEvidence> static20 = new LinkedHashMap<>();
        Map<String, SourceEvidence> routes = new LinkedHashMap<>();
        Map<String, SourceEvidence> flags = new LinkedHashMap<>();
        Map<String, SourceEvidence> getbacks = new LinkedHashMap<>();
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            for (String source : config.classicServerNames()) {
                AccountRepository repository = routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                try (Connection connection = connection(repository)) {
                    monsters19.put(database, monsterEvidence(connection, database, 19));
                    monsters20.put(database, monsterEvidence(connection, database, 20));
                    drops19.put(database, dropEvidence(connection, database, 19));
                    drops20.put(database, dropEvidence(connection, database, 20));
                    static19.put(database, staticNpcEvidence(connection, database, 19));
                    static20.put(database, staticNpcEvidence(connection, database, 20));

                    boolean requiredItem = hasColumn(
                            repository, database, "dungeon", "require_item_id");
                    StringBuilder portal = new StringBuilder(requiredItem
                            ? PIRATE_PORTAL_COLUMNS : PIRATE_PORTAL_SECONDARY_COLUMNS)
                            .append('\n');
                    String portalSql = "SELECT src_mapid,src_x,src_y,new_mapid,new_x,new_y,"
                            + "new_heading," + (requiredItem ? "require_item_id," : "")
                            + "note FROM " + database + ".`dungeon` WHERE src_mapid IN "
                            + "(4,19,20,21) AND (new_mapid IN (4,19,20,21)) "
                            + "AND (src_mapid IN (19,20) OR new_mapid IN (19,20)) "
                            + "ORDER BY src_mapid,src_x,src_y,new_mapid,new_x,new_y";
                    int portalRows = appendCanonicalRows(
                            connection, portalSql, requiredItem ? 9 : 8, portal);
                    routes.put(database, new SourceEvidence(
                            portalRows, sha256(portal.toString())));

                    StringBuilder mapFlags = new StringBuilder(
                            MAP350_FLAGS_COLUMNS).append('\n');
                    int flagRows = appendCanonicalRows(connection,
                            "SELECT mapid,locationname,startX,endX,startY,endY,"
                            + "escapable,usable_item,usable_skill,teleportable,"
                            + "take_pets,recall_pets FROM " + database
                            + ".`mapids` WHERE mapid IN (19,20) ORDER BY mapid",
                            12, mapFlags);
                    flags.put(database, new SourceEvidence(
                            flagRows, sha256(mapFlags.toString())));

                    StringBuilder getback = new StringBuilder(
                            GETBACK_COLUMNS).append('\n');
                    int getbackRows = appendCanonicalRows(connection,
                            "SELECT area_x1,area_y1,area_x2,area_y2,area_mapid,"
                            + "getback_x1,getback_y1,getback_x2,getback_y2,"
                            + "getback_x3,getback_y3,getback_mapid,getback_townid,"
                            + "getback_townid_elf,getback_townid_darkelf,"
                            + "scrollescape,note FROM " + database
                            + ".`getback` WHERE area_mapid IN (19,20) "
                            + "ORDER BY area_mapid,area_x1 DESC",
                            17, getback);
                    getbacks.put(database, new SourceEvidence(
                            getbackRows, sha256(getback.toString())));

                    System.out.printf("[LiTo-test] source=%s sleeping-dragon"
                                    + " map19-monsters=%d/%d drops=%d/%d"
                                    + " map20-monsters=%d/%d drops=%d/%d"
                                    + " portals=%d required-item-column=%s"
                                    + " flags=%d getback=%d read-only=true%n",
                            source,
                            monsters19.get(database).rows(),
                            monsters19.get(database).population(),
                            drops19.get(database).rows(), drops19.get(database).items(),
                            monsters20.get(database).rows(),
                            monsters20.get(database).population(),
                            drops20.get(database).rows(), drops20.get(database).items(),
                            portalRows, requiredItem, flagRows, getbackRows);
                }
            }
        }

        assertEquals(new MonsterSourceEvidence(1, 15,
                        ClassicWorldMonsterCatalog.MAP19_SOURCE_SHA256),
                monsters19.get("380_db"));
        assertEquals(new MonsterSourceEvidence(4, 55,
                        "83A0BDBA38EF14C96A85FC259BAACD6647DE1952ACD259F7768601E416AA0947"),
                monsters19.get("380yiwei_db"));
        assertEquals(new MonsterSourceEvidence(5, 70,
                        ClassicWorldMonsterCatalog.MAP20_SOURCE_SHA256),
                monsters20.get("380_db"));
        assertEquals(new MonsterSourceEvidence(7, 80,
                        "758AD5D8D7A7C68FF8A127346A1CDF2511D03511A885E7879389C4D2C295DE5D"),
                monsters20.get("380yiwei_db"));
        assertEquals(new DropSourceEvidence(2, 2,
                        ClassicWorldDropCatalog.MAP19_SOURCE_SHA256),
                drops19.get("380_db"));
        assertEquals(new DropSourceEvidence(26, 14,
                        "3530C798F92D0DA2717FA1E95F90155DA795254C2C2527D0529D6CDBC0E42296"),
                drops19.get("380yiwei_db"));
        assertEquals(new DropSourceEvidence(18, 14,
                        ClassicWorldDropCatalog.MAP20_SOURCE_SHA256),
                drops20.get("380_db"));
        assertEquals(new DropSourceEvidence(31, 23,
                        "2DEEF44B5F73FE65410946A38C65C22C8D55B999AC27BB080F94E5BD0CD40597"),
                drops20.get("380yiwei_db"));

        SourceEvidence emptyStatic = new SourceEvidence(0,
                ClassicWorldNpcCatalog.MAP19_SOURCE_SHA256);
        assertEquals(emptyStatic, static19.get("380_db"));
        assertEquals(emptyStatic, static19.get("380yiwei_db"));
        assertEquals(emptyStatic, static20.get("380_db"));
        assertEquals(emptyStatic, static20.get("380yiwei_db"));
        assertEquals(new SourceEvidence(12,
                        ClassicWorldDungeonCatalog.SLEEPING_DRAGON_SOURCE_SHA256),
                routes.get("380_db"));
        assertEquals(new SourceEvidence(12,
                        ClassicWorldDungeonCatalog.SLEEPING_DRAGON_SECONDARY_SOURCE_SHA256),
                routes.get("380yiwei_db"));
        assertEquals(new SourceEvidence(2,
                        ClassicWorldMapCatalog.SLEEPING_DRAGON_FLAGS_SOURCE_SHA256),
                flags.get("380_db"));
        assertEquals(new SourceEvidence(2,
                        "8A46303861F4627EFBCA4F60C5D8721DAC9F133BE030C727FB2DFDF97EDC78D8"),
                flags.get("380yiwei_db"));
        SourceEvidence exactGetback = new SourceEvidence(2,
                ClassicWorldGetbackCatalog.SLEEPING_DRAGON_SOURCE_SHA256);
        assertEquals(exactGetback, getbacks.get("380_db"));
        assertEquals(exactGetback, getbacks.get("380yiwei_db"));
        assertNotEquals(monsters19.get("380_db"), monsters19.get("380yiwei_db"));
        assertNotEquals(monsters20.get("380_db"), monsters20.get("380yiwei_db"));
        assertNotEquals(drops19.get("380_db"), drops19.get("380yiwei_db"));
        assertNotEquals(drops20.get("380_db"), drops20.get("380yiwei_db"));
        assertNotEquals(flags.get("380_db"), flags.get("380yiwei_db"));
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void selectedDragonValleyThirdFloorFlowLocksBothReadOnlySources()
            throws Exception {
        String configPath = System.getProperty("lito.legacy.config",
                "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        Map<String, MonsterSourceEvidence> monsters = new LinkedHashMap<>();
        Map<String, DropSourceEvidence> drops = new LinkedHashMap<>();
        Map<String, SourceEvidence> staticNpcs = new LinkedHashMap<>();
        Map<String, SourceEvidence> routes = new LinkedHashMap<>();
        Map<String, SourceEvidence> flags = new LinkedHashMap<>();
        Map<String, SourceEvidence> getbacks = new LinkedHashMap<>();
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            for (String source : config.classicServerNames()) {
                AccountRepository repository = routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                try (Connection connection = connection(repository)) {
                    monsters.put(database, monsterEvidence(connection, database, 32));
                    drops.put(database, dropEvidence(connection, database, 32));
                    staticNpcs.put(database,
                            staticNpcEvidence(connection, database, 32));

                    boolean requiredItem = hasColumn(
                            repository, database, "dungeon", "require_item_id");
                    StringBuilder portal = new StringBuilder(requiredItem
                            ? PIRATE_PORTAL_COLUMNS : PIRATE_PORTAL_SECONDARY_COLUMNS)
                            .append('\n');
                    String portalSql = "SELECT src_mapid,src_x,src_y,new_mapid,new_x,new_y,"
                            + "new_heading," + (requiredItem ? "require_item_id," : "")
                            + "note FROM " + database
                            + ".`dungeon` WHERE src_mapid IN (21,32) "
                            + "AND new_mapid IN (21,32) ORDER BY src_mapid,src_x,src_y,"
                            + "new_mapid,new_x,new_y";
                    int portalRows = appendCanonicalRows(
                            connection, portalSql, requiredItem ? 9 : 8, portal);
                    routes.put(database, new SourceEvidence(
                            portalRows, sha256(portal.toString())));

                    StringBuilder mapFlags = new StringBuilder(
                            MAP350_FLAGS_COLUMNS).append('\n');
                    int flagRows = appendCanonicalRows(connection,
                            "SELECT mapid,locationname,startX,endX,startY,endY,"
                            + "escapable,usable_item,usable_skill,teleportable,"
                            + "take_pets,recall_pets FROM " + database
                            + ".`mapids` WHERE mapid=32", 12, mapFlags);
                    flags.put(database, new SourceEvidence(
                            flagRows, sha256(mapFlags.toString())));

                    StringBuilder getback = new StringBuilder(
                            GETBACK_COLUMNS).append('\n');
                    int getbackRows = appendCanonicalRows(connection,
                            "SELECT area_x1,area_y1,area_x2,area_y2,area_mapid,"
                            + "getback_x1,getback_y1,getback_x2,getback_y2,"
                            + "getback_x3,getback_y3,getback_mapid,getback_townid,"
                            + "getback_townid_elf,getback_townid_darkelf,"
                            + "scrollescape,note FROM " + database
                            + ".`getback` WHERE area_mapid=32 "
                            + "ORDER BY area_mapid,area_x1 DESC",
                            17, getback);
                    getbacks.put(database, new SourceEvidence(
                            getbackRows, sha256(getback.toString())));

                    System.out.printf("[LiTo-test] source=%s dragon-valley-map32"
                                    + " monsters=%d/%d drops=%d/%d static=%d"
                                    + " portals=%d required-item-column=%s"
                                    + " flags=%s getback=%s read-only=true%n",
                            source, monsters.get(database).rows(),
                            monsters.get(database).population(),
                            drops.get(database).rows(), drops.get(database).items(),
                            staticNpcs.get(database).rows(), portalRows, requiredItem,
                            flags.get(database).sha256(),
                            getbacks.get(database).sha256());
                }
            }
        }

        assertEquals(new MonsterSourceEvidence(7, 70,
                        ClassicWorldMonsterCatalog.MAP32_SOURCE_SHA256),
                monsters.get("380_db"));
        assertEquals(new MonsterSourceEvidence(7, 70,
                        "393FEA6A3874E146C0BF758AA147FD9186FD0B78B7BA4EF0230415F4857508C3"),
                monsters.get("380yiwei_db"));
        assertEquals(new DropSourceEvidence(59, 44,
                        ClassicWorldDropCatalog.MAP32_SOURCE_SHA256),
                drops.get("380_db"));
        assertEquals(new DropSourceEvidence(49, 34,
                        "80920F90BB08A3F2D90E3DD87F19867F52F92BAA5DDB2494ED60D75EC53571CA"),
                drops.get("380yiwei_db"));
        SourceEvidence exactStatic = new SourceEvidence(2,
                ClassicWorldNpcCatalog.MAP32_SOURCE_SHA256);
        assertEquals(exactStatic, staticNpcs.get("380_db"));
        assertEquals(exactStatic, staticNpcs.get("380yiwei_db"));
        assertEquals(new SourceEvidence(4,
                        ClassicWorldDungeonCatalog.DRAGON_VALLEY_THIRD_FLOOR_SOURCE_SHA256),
                routes.get("380_db"));
        assertEquals(new SourceEvidence(4,
                        ClassicWorldDungeonCatalog
                                .DRAGON_VALLEY_THIRD_FLOOR_SECONDARY_SOURCE_SHA256),
                routes.get("380yiwei_db"));
        assertEquals(new SourceEvidence(1,
                        ClassicWorldMapCatalog.MAP32_FLAGS_SOURCE_SHA256),
                flags.get("380_db"));
        assertEquals(new SourceEvidence(1,
                        "8DEC0AC4439223A4880291448736874BA81F515F928346AC3B1B60C72C381C14"),
                flags.get("380yiwei_db"));
        SourceEvidence exactGetback = new SourceEvidence(1,
                ClassicWorldGetbackCatalog.MAP32_SOURCE_SHA256);
        assertEquals(exactGetback, getbacks.get("380_db"));
        assertEquals(exactGetback, getbacks.get("380yiwei_db"));
        assertNotEquals(monsters.get("380_db"), monsters.get("380yiwei_db"));
        assertNotEquals(drops.get("380_db"), drops.get("380yiwei_db"));
        assertNotEquals(flags.get("380_db"), flags.get("380yiwei_db"));
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void selectedDragonValleyFloorsOneThroughFourLockBothReadOnlySources()
            throws Exception {
        String configPath = System.getProperty("lito.legacy.config",
                "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        Map<String, MonsterSourceEvidence> monsters30 = new LinkedHashMap<>();
        Map<String, MonsterSourceEvidence> monsters31 = new LinkedHashMap<>();
        Map<String, MonsterSourceEvidence> monsters33 = new LinkedHashMap<>();
        Map<String, DropSourceEvidence> drops30 = new LinkedHashMap<>();
        Map<String, DropSourceEvidence> drops31 = new LinkedHashMap<>();
        Map<String, DropSourceEvidence> drops33 = new LinkedHashMap<>();
        Map<String, SourceEvidence> static30 = new LinkedHashMap<>();
        Map<String, SourceEvidence> static31 = new LinkedHashMap<>();
        Map<String, SourceEvidence> static33 = new LinkedHashMap<>();
        Map<String, SourceEvidence> routes = new LinkedHashMap<>();
        Map<String, SourceEvidence> firstFloorRoutes = new LinkedHashMap<>();
        Map<String, SourceEvidence> flags = new LinkedHashMap<>();
        Map<String, SourceEvidence> getbacks = new LinkedHashMap<>();
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            for (String source : config.classicServerNames()) {
                AccountRepository repository = routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                try (Connection connection = connection(repository)) {
                    monsters30.put(database, monsterEvidence(connection, database, 30));
                    monsters31.put(database, monsterEvidence(connection, database, 31));
                    monsters33.put(database, monsterEvidence(connection, database, 33));
                    drops30.put(database, dropEvidence(connection, database, 30));
                    drops31.put(database, dropEvidence(connection, database, 31));
                    drops33.put(database, dropEvidence(connection, database, 33));
                    static30.put(database, staticNpcEvidence(connection, database, 30));
                    static31.put(database, staticNpcEvidence(connection, database, 31));
                    static33.put(database, staticNpcEvidence(connection, database, 33));

                    boolean requiredItem = hasColumn(
                            repository, database, "dungeon", "require_item_id");
                    StringBuilder portal = new StringBuilder(requiredItem
                            ? PIRATE_PORTAL_COLUMNS : PIRATE_PORTAL_SECONDARY_COLUMNS)
                            .append('\n');
                    String portalSql = "SELECT src_mapid,src_x,src_y,new_mapid,new_x,new_y,"
                            + "new_heading," + (requiredItem ? "require_item_id," : "")
                            + "note FROM " + database
                            + ".`dungeon` WHERE src_mapid IN (31,32,33) "
                            + "AND new_mapid IN (31,32,33) ORDER BY src_mapid,src_x,src_y,"
                            + "new_mapid,new_x,new_y";
                    int portalRows = appendCanonicalRows(
                            connection, portalSql, requiredItem ? 9 : 8, portal);
                    routes.put(database, new SourceEvidence(
                            portalRows, sha256(portal.toString())));

                    StringBuilder firstFloorPortal = new StringBuilder(requiredItem
                            ? PIRATE_PORTAL_COLUMNS : PIRATE_PORTAL_SECONDARY_COLUMNS)
                            .append('\n');
                    String firstFloorPortalSql = "SELECT src_mapid,src_x,src_y,"
                            + "new_mapid,new_x,new_y,new_heading,"
                            + (requiredItem ? "require_item_id," : "")
                            + "note FROM " + database + ".`dungeon` WHERE "
                            + "(src_mapid=30 AND new_mapid IN (4,31)) OR "
                            + "(new_mapid=30 AND src_mapid IN (4,31)) "
                            + "ORDER BY src_mapid,src_x,src_y,new_mapid,new_x,new_y";
                    int firstFloorPortalRows = appendCanonicalRows(connection,
                            firstFloorPortalSql, requiredItem ? 9 : 8,
                            firstFloorPortal);
                    firstFloorRoutes.put(database, new SourceEvidence(
                            firstFloorPortalRows, sha256(firstFloorPortal.toString())));

                    StringBuilder mapFlags = new StringBuilder(
                            MAP350_FLAGS_COLUMNS).append('\n');
                    int flagRows = appendCanonicalRows(connection,
                            "SELECT mapid,locationname,startX,endX,startY,endY,"
                            + "escapable,usable_item,usable_skill,teleportable,"
                            + "take_pets,recall_pets FROM " + database
                            + ".`mapids` WHERE mapid IN (30,31,32,33) ORDER BY mapid",
                            12, mapFlags);
                    flags.put(database, new SourceEvidence(
                            flagRows, sha256(mapFlags.toString())));

                    StringBuilder getback = new StringBuilder(
                            GETBACK_COLUMNS).append('\n');
                    int getbackRows = appendCanonicalRows(connection,
                            "SELECT area_x1,area_y1,area_x2,area_y2,area_mapid,"
                            + "getback_x1,getback_y1,getback_x2,getback_y2,"
                            + "getback_x3,getback_y3,getback_mapid,getback_townid,"
                            + "getback_townid_elf,getback_townid_darkelf,"
                            + "scrollescape,note FROM " + database
                            + ".`getback` WHERE area_mapid IN (30,31,32,33) "
                            + "ORDER BY area_mapid,area_x1 DESC", 17, getback);
                    getbacks.put(database, new SourceEvidence(
                            getbackRows, sha256(getback.toString())));
                    System.out.printf("[LiTo-test] source=%s dragon-valley-map30-33"
                                    + " map30=%d/%d/%d/%d map31=%d/%d/%d/%d"
                                    + " map33=%d/%d/%d/%d portals=%d"
                                    + " first-floor-portals=%d flags=%d getback=%d"
                                    + " read-only=true%n",
                            source, monsters30.get(database).rows(),
                            monsters30.get(database).population(),
                            drops30.get(database).rows(), drops30.get(database).items(),
                            monsters31.get(database).rows(),
                            monsters31.get(database).population(),
                            drops31.get(database).rows(), drops31.get(database).items(),
                            monsters33.get(database).rows(),
                            monsters33.get(database).population(),
                            drops33.get(database).rows(), drops33.get(database).items(),
                            portalRows, firstFloorPortalRows, flagRows, getbackRows);
                }
            }
        }

        assertEquals(new MonsterSourceEvidence(6, 75,
                        ClassicWorldMonsterCatalog.MAP30_SOURCE_SHA256),
                monsters30.get("380_db"));
        assertEquals(new MonsterSourceEvidence(7, 85,
                        "3370E982ACE9A97F062B105AFB2B977D962B804FB4822E05D6B4A64CFE6DDDAE"),
                monsters30.get("380yiwei_db"));
        assertEquals(new MonsterSourceEvidence(6, 70,
                        ClassicWorldMonsterCatalog.MAP31_SOURCE_SHA256),
                monsters31.get("380_db"));
        assertEquals(new MonsterSourceEvidence(6, 70,
                        "723CDF35087F606CEEFF1EEB4F4438BEC13B063CDAC53CC09D8F7C98F4001B1B"),
                monsters31.get("380yiwei_db"));
        assertEquals(new MonsterSourceEvidence(8, 85,
                        ClassicWorldMonsterCatalog.MAP33_SOURCE_SHA256),
                monsters33.get("380_db"));
        assertEquals(new MonsterSourceEvidence(8, 85,
                        "125949ED55CE04C52CBF47BCC1CADD996C81BEBB6F92A87F730F623A23976326"),
                monsters33.get("380yiwei_db"));
        assertEquals(new DropSourceEvidence(44, 31,
                        ClassicWorldDropCatalog.MAP30_SOURCE_SHA256),
                drops30.get("380_db"));
        assertEquals(new DropSourceEvidence(44, 25,
                        "2AE031592B04B1395CAD62E4BDF7BF325916B203DC56B8F789F843DA9C55D2F3"),
                drops30.get("380yiwei_db"));
        assertEquals(new DropSourceEvidence(47, 35,
                        ClassicWorldDropCatalog.MAP31_SOURCE_SHA256),
                drops31.get("380_db"));
        assertEquals(new DropSourceEvidence(39, 27,
                        "9A6E4B6E8DED275CD725958FAA3DF0073692C30A983B361D71800D4AC754E791"),
                drops31.get("380yiwei_db"));
        assertEquals(new DropSourceEvidence(76, 58,
                        ClassicWorldDropCatalog.MAP33_SOURCE_SHA256),
                drops33.get("380_db"));
        assertEquals(new DropSourceEvidence(65, 47,
                        "DAA5B92968F259A7186870DAEBE4C56E57A2999EA907255EFB04D671BABEBDA9"),
                drops33.get("380yiwei_db"));
        SourceEvidence emptyStatic = new SourceEvidence(
                0, ClassicWorldNpcCatalog.MAP31_SOURCE_SHA256);
        for (String database : new String[] {"380_db", "380yiwei_db"}) {
            assertEquals(emptyStatic, static30.get(database));
            assertEquals(emptyStatic, static31.get(database));
            assertEquals(emptyStatic, static33.get(database));
            assertEquals(new SourceEvidence(4,
                            ClassicWorldGetbackCatalog
                                    .DRAGON_VALLEY_DUNGEON_FLOORS_1_TO_4_SOURCE_SHA256),
                    getbacks.get(database));
        }
        assertEquals(new SourceEvidence(5,
                        ClassicWorldDungeonCatalog
                                .DRAGON_VALLEY_FLOOR_TRANSITIONS_SOURCE_SHA256),
                routes.get("380_db"));
        assertEquals(new SourceEvidence(5,
                        ClassicWorldDungeonCatalog
                                .DRAGON_VALLEY_FLOOR_TRANSITIONS_SECONDARY_SOURCE_SHA256),
                routes.get("380yiwei_db"));
        assertEquals(new SourceEvidence(20,
                        ClassicWorldDungeonCatalog
                                .DRAGON_VALLEY_FIRST_FLOOR_TRANSITIONS_SOURCE_SHA256),
                firstFloorRoutes.get("380_db"));
        assertEquals(new SourceEvidence(20,
                        ClassicWorldDungeonCatalog
                                .DRAGON_VALLEY_FIRST_FLOOR_TRANSITIONS_SECONDARY_SOURCE_SHA256),
                firstFloorRoutes.get("380yiwei_db"));
        assertEquals(new SourceEvidence(4,
                        ClassicWorldMapCatalog
                                .DRAGON_VALLEY_DUNGEON_FLOORS_1_TO_4_FLAGS_SOURCE_SHA256),
                flags.get("380_db"));
        assertEquals(new SourceEvidence(4,
                        ClassicWorldMapCatalog
                                .DRAGON_VALLEY_DUNGEON_FLOORS_1_TO_4_FLAGS_SECONDARY_SOURCE_SHA256),
                flags.get("380yiwei_db"));
        assertNotEquals(monsters30.get("380_db"), monsters30.get("380yiwei_db"));
        assertNotEquals(monsters31.get("380_db"), monsters31.get("380yiwei_db"));
        assertNotEquals(monsters33.get("380_db"), monsters33.get("380yiwei_db"));
        assertNotEquals(drops30.get("380_db"), drops30.get("380yiwei_db"));
        assertNotEquals(drops31.get("380_db"), drops31.get("380yiwei_db"));
        assertNotEquals(drops33.get("380_db"), drops33.get("380yiwei_db"));
        assertNotEquals(routes.get("380_db"), routes.get("380yiwei_db"));
        assertNotEquals(flags.get("380_db"), flags.get("380yiwei_db"));
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void selectedDeepDragonValleyFlowLocksBothReadOnlySources()
            throws Exception {
        String configPath = System.getProperty("lito.legacy.config",
                "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        int[] mapIds = {35, 36, 37};
        Map<Integer, Map<String, MonsterSourceEvidence>> monsters =
                new LinkedHashMap<>();
        Map<Integer, Map<String, DropSourceEvidence>> drops =
                new LinkedHashMap<>();
        Map<Integer, Map<String, SourceEvidence>> staticNpcs =
                new LinkedHashMap<>();
        for (int mapId : mapIds) {
            monsters.put(mapId, new LinkedHashMap<>());
            drops.put(mapId, new LinkedHashMap<>());
            staticNpcs.put(mapId, new LinkedHashMap<>());
        }
        Map<String, SourceEvidence> routes = new LinkedHashMap<>();
        Map<String, SourceEvidence> flags = new LinkedHashMap<>();
        Map<String, SourceEvidence> getbacks = new LinkedHashMap<>();
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            for (String source : config.classicServerNames()) {
                AccountRepository repository = routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                try (Connection connection = connection(repository)) {
                    for (int mapId : mapIds) {
                        monsters.get(mapId).put(database,
                                monsterEvidence(connection, database, mapId));
                        drops.get(mapId).put(database,
                                dropEvidence(connection, database, mapId));
                        staticNpcs.get(mapId).put(database,
                                staticNpcEvidence(connection, database, mapId));
                    }
                    boolean requiredItem = hasColumn(
                            repository, database, "dungeon", "require_item_id");
                    StringBuilder portal = new StringBuilder(requiredItem
                            ? PIRATE_PORTAL_COLUMNS : PIRATE_PORTAL_SECONDARY_COLUMNS)
                            .append('\n');
                    int portalRows = appendCanonicalRows(connection,
                            "SELECT src_mapid,src_x,src_y,new_mapid,new_x,new_y,"
                            + "new_heading," + (requiredItem ? "require_item_id," : "")
                            + "note FROM " + database + ".`dungeon` WHERE "
                            + "src_mapid IN (33,35,36,37) AND "
                            + "new_mapid IN (33,35,36,37) ORDER BY "
                            + "src_mapid,src_x,src_y,new_mapid,new_x,new_y",
                            requiredItem ? 9 : 8, portal);
                    routes.put(database, new SourceEvidence(
                            portalRows, sha256(portal.toString())));

                    StringBuilder mapFlags = new StringBuilder(
                            MAP350_FLAGS_COLUMNS).append('\n');
                    int flagRows = appendCanonicalRows(connection,
                            "SELECT mapid,locationname,startX,endX,startY,endY,"
                            + "escapable,usable_item,usable_skill,teleportable,"
                            + "take_pets,recall_pets FROM " + database
                            + ".`mapids` WHERE mapid IN (35,36,37) ORDER BY mapid",
                            12, mapFlags);
                    flags.put(database, new SourceEvidence(
                            flagRows, sha256(mapFlags.toString())));

                    StringBuilder getback = new StringBuilder(
                            GETBACK_COLUMNS).append('\n');
                    int getbackRows = appendCanonicalRows(connection,
                            "SELECT area_x1,area_y1,area_x2,area_y2,area_mapid,"
                            + "getback_x1,getback_y1,getback_x2,getback_y2,"
                            + "getback_x3,getback_y3,getback_mapid,getback_townid,"
                            + "getback_townid_elf,getback_townid_darkelf,"
                            + "scrollescape,note FROM " + database
                            + ".`getback` WHERE area_mapid IN (35,36,37) "
                            + "ORDER BY area_mapid,area_x1,area_y1",
                            17, getback);
                    getbacks.put(database, new SourceEvidence(
                            getbackRows, sha256(getback.toString())));
                    System.out.printf("[LiTo-test] source=%s dragon-valley-map35-37"
                                    + " map35=%d/%d/%d/%d map36=%d/%d/%d/%d"
                                    + " map37=%d/%d/%d/%d portals=%d flags=%d"
                                    + " getback=%d read-only=true%n",
                            source,
                            monsters.get(35).get(database).rows(),
                            monsters.get(35).get(database).population(),
                            drops.get(35).get(database).rows(),
                            drops.get(35).get(database).items(),
                            monsters.get(36).get(database).rows(),
                            monsters.get(36).get(database).population(),
                            drops.get(36).get(database).rows(),
                            drops.get(36).get(database).items(),
                            monsters.get(37).get(database).rows(),
                            monsters.get(37).get(database).population(),
                            drops.get(37).get(database).rows(),
                            drops.get(37).get(database).items(),
                            portalRows, flagRows, getbackRows);
                }
            }
        }

        MonsterSourceEvidence[] selectedMonsters = {
                new MonsterSourceEvidence(5, 55,
                        ClassicWorldMonsterCatalog.MAP35_SOURCE_SHA256),
                new MonsterSourceEvidence(4, 60,
                        ClassicWorldMonsterCatalog.MAP36_SOURCE_SHA256),
                new MonsterSourceEvidence(1, 15,
                        ClassicWorldMonsterCatalog.MAP37_SOURCE_SHA256)};
        MonsterSourceEvidence[] secondaryMonsters = {
                new MonsterSourceEvidence(5, 55,
                        "7C78DA5A1FBF3384111B4F8F42D645F208F345F01249E1169E1F9C4666FE1046"),
                new MonsterSourceEvidence(4, 60,
                        "B45CB87B8D2C3160D36A9D02424A6105634127AC0C4D54EE1CD052DA809C17C9"),
                new MonsterSourceEvidence(1, 15,
                        "1AA6508B360328309BC48A06A4BEA2248B26F1F1062869831AC919EB7D598AAD")};
        DropSourceEvidence[] selectedDrops = {
                new DropSourceEvidence(58, 46,
                        ClassicWorldDropCatalog.MAP35_SOURCE_SHA256),
                new DropSourceEvidence(46, 36,
                        ClassicWorldDropCatalog.MAP36_SOURCE_SHA256),
                new DropSourceEvidence(0, 0,
                        ClassicWorldDropCatalog.MAP37_SOURCE_SHA256)};
        DropSourceEvidence[] secondaryDrops = {
                new DropSourceEvidence(50, 38,
                        "509631BC85E7D7ECD7DCCFC8F7C3827C66DF7053175E303E3C87FE31267BF466"),
                new DropSourceEvidence(39, 29,
                        "3E071A9887408B7A3848494D0B717E16DAC2F0D55DEF704DE93619BF794E3E8E"),
                new DropSourceEvidence(0, 0,
                        ClassicWorldDropCatalog.MAP37_SOURCE_SHA256)};
        SourceEvidence emptyStatic = new SourceEvidence(
                0, ClassicWorldNpcCatalog.MAP35_SOURCE_SHA256);
        for (int index = 0; index < mapIds.length; index++) {
            int mapId = mapIds[index];
            assertEquals(selectedMonsters[index], monsters.get(mapId).get("380_db"));
            assertEquals(secondaryMonsters[index],
                    monsters.get(mapId).get("380yiwei_db"));
            assertEquals(selectedDrops[index], drops.get(mapId).get("380_db"));
            assertEquals(secondaryDrops[index], drops.get(mapId).get("380yiwei_db"));
            assertEquals(emptyStatic, staticNpcs.get(mapId).get("380_db"));
            assertEquals(emptyStatic, staticNpcs.get(mapId).get("380yiwei_db"));
        }
        assertEquals(new SourceEvidence(9,
                        ClassicWorldDungeonCatalog
                                .DRAGON_VALLEY_DEEP_TRANSITIONS_SOURCE_SHA256),
                routes.get("380_db"));
        assertEquals(new SourceEvidence(9,
                        ClassicWorldDungeonCatalog
                                .DRAGON_VALLEY_DEEP_TRANSITIONS_SECONDARY_SOURCE_SHA256),
                routes.get("380yiwei_db"));
        assertEquals(new SourceEvidence(3,
                        ClassicWorldMapCatalog.DRAGON_VALLEY_DEEP_FLAGS_SOURCE_SHA256),
                flags.get("380_db"));
        assertEquals(new SourceEvidence(3,
                        ClassicWorldMapCatalog
                                .DRAGON_VALLEY_DEEP_FLAGS_SECONDARY_SOURCE_SHA256),
                flags.get("380yiwei_db"));
        SourceEvidence exactGetback = new SourceEvidence(
                3, ClassicWorldGetbackCatalog.DRAGON_VALLEY_DEEP_SOURCE_SHA256);
        assertEquals(exactGetback, getbacks.get("380_db"));
        assertEquals(exactGetback, getbacks.get("380yiwei_db"));
        assertNotEquals(monsters.get(35).get("380_db"),
                monsters.get(35).get("380yiwei_db"));
        assertNotEquals(drops.get(36).get("380_db"),
                drops.get(36).get("380yiwei_db"));
        assertNotEquals(flags.get("380_db"), flags.get("380yiwei_db"));
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void selectedFairyDungeonFlowLocksBothReadOnlySources() throws Exception {
        String configPath = System.getProperty("lito.legacy.config",
                "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        Map<String, MonsterSourceEvidence> monsters209 = new LinkedHashMap<>();
        Map<String, MonsterSourceEvidence> monsters213 = new LinkedHashMap<>();
        Map<String, DropSourceEvidence> drops209 = new LinkedHashMap<>();
        Map<String, DropSourceEvidence> drops213 = new LinkedHashMap<>();
        Map<String, SourceEvidence> static209 = new LinkedHashMap<>();
        Map<String, SourceEvidence> static213 = new LinkedHashMap<>();
        Map<String, SourceEvidence> routes = new LinkedHashMap<>();
        Map<String, SourceEvidence> flags = new LinkedHashMap<>();
        Map<String, SourceEvidence> getbackCounts = new LinkedHashMap<>();
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            for (String source : config.classicServerNames()) {
                AccountRepository repository = routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                try (Connection connection = connection(repository)) {
                    monsters209.put(database, monsterEvidence(connection, database, 209));
                    monsters213.put(database, monsterEvidence(connection, database, 213));
                    drops209.put(database, dropEvidence(connection, database, 209));
                    drops213.put(database, dropEvidence(connection, database, 213));
                    static209.put(database, staticNpcEvidence(connection, database, 209));
                    static213.put(database, staticNpcEvidence(connection, database, 213));
                    routes.put(database,
                            fairyDungeonTeleportEvidence(connection, database));

                    StringBuilder mapFlags = new StringBuilder(
                            MAP350_FLAGS_COLUMNS).append('\n');
                    int flagRows = appendCanonicalRows(connection,
                            "SELECT mapid,locationname,startX,endX,startY,endY,"
                            + "escapable,usable_item,usable_skill,teleportable,"
                            + "take_pets,recall_pets FROM " + database
                            + ".`mapids` WHERE mapid IN (209,213) ORDER BY mapid",
                            12, mapFlags);
                    flags.put(database, new SourceEvidence(
                            flagRows, sha256(mapFlags.toString())));

                    StringBuilder getback = new StringBuilder(
                            "area_mapid\trows\n");
                    for (int mapId : new int[] {209, 213}) {
                        try (PreparedStatement statement = connection.prepareStatement(
                                "SELECT COUNT(*) FROM " + database
                                        + ".`getback` WHERE area_mapid=" + mapId);
                             ResultSet row = statement.executeQuery()) {
                            assertTrue(row.next());
                            getback.append(mapId).append('\t')
                                    .append(row.getInt(1)).append('\n');
                        }
                    }
                    getbackCounts.put(database,
                            new SourceEvidence(2, sha256(getback.toString())));
                    System.out.printf("[LiTo-test] source=%s fairy-routes=%d"
                                    + " map209-monsters=%d/%d drops=%d/%d static=%d"
                                    + " map213-monsters=%d/%d drops=%d/%d static=%d"
                                    + " flags=%s getback=%s read-only=true%n",
                            source, routes.get(database).rows(),
                            monsters209.get(database).rows(),
                            monsters209.get(database).population(),
                            drops209.get(database).rows(), drops209.get(database).items(),
                            static209.get(database).rows(),
                            monsters213.get(database).rows(),
                            monsters213.get(database).population(),
                            drops213.get(database).rows(), drops213.get(database).items(),
                            static213.get(database).rows(), flags.get(database).sha256(),
                            getbackCounts.get(database).sha256());
                }
            }
        }

        MonsterSourceEvidence emptyMonsters = new MonsterSourceEvidence(
                0, 0, ClassicWorldMonsterCatalog.MAP209_SOURCE_SHA256);
        DropSourceEvidence emptyDrops = new DropSourceEvidence(
                0, 0, ClassicWorldDropCatalog.MAP209_SOURCE_SHA256);
        SourceEvidence emptyStatic = new SourceEvidence(
                0, ClassicWorldNpcCatalog.MAP209_SOURCE_SHA256);
        assertEquals(emptyMonsters, monsters209.get("380_db"));
        assertEquals(emptyMonsters, monsters209.get("380yiwei_db"));
        assertEquals(emptyMonsters, monsters213.get("380_db"));
        assertEquals(new MonsterSourceEvidence(1, 1,
                        "BA532DA506A8EA71828ADED706F66EDBAE59ECD9DACD3A1D5D2C7B3FAFD53C08"),
                monsters213.get("380yiwei_db"));
        assertEquals(emptyDrops, drops209.get("380_db"));
        assertEquals(emptyDrops, drops209.get("380yiwei_db"));
        assertEquals(emptyDrops, drops213.get("380_db"));
        assertEquals(new DropSourceEvidence(1, 1,
                        "500B876DD46F93BD88725A1A1E46D1EF331AB30FA5F45C6CB17EFE268041FA9D"),
                drops213.get("380yiwei_db"));
        assertEquals(emptyStatic, static209.get("380_db"));
        assertEquals(emptyStatic, static209.get("380yiwei_db"));
        assertEquals(emptyStatic, static213.get("380_db"));
        assertEquals(emptyStatic, static213.get("380yiwei_db"));
        assertEquals(new SourceEvidence(2,
                        ClassicWorldTeleportCatalog.FAIRY_ROUTES_SOURCE_SHA256),
                routes.get("380_db"));
        assertEquals(new SourceEvidence(0,
                        "B23BCA8C4E88CC36FE68B6A74DDC7DA06B7C2D465203724784B994739941283E"),
                routes.get("380yiwei_db"));
        SourceEvidence exactFlags = new SourceEvidence(2,
                ClassicWorldMapCatalog.FAIRY_DUNGEON_FLAGS_SOURCE_SHA256);
        assertEquals(exactFlags, flags.get("380_db"));
        assertEquals(exactFlags, flags.get("380yiwei_db"));
        SourceEvidence exactGetback = new SourceEvidence(2,
                ClassicWorldGetbackCatalog.FAIRY_DUNGEONS_ABSENCE_SOURCE_SHA256);
        assertEquals(exactGetback, getbackCounts.get("380_db"));
        assertEquals(exactGetback, getbackCounts.get("380yiwei_db"));
        assertNotEquals(routes.get("380_db"), routes.get("380yiwei_db"));
        assertNotEquals(monsters213.get("380_db"), monsters213.get("380yiwei_db"));
        assertNotEquals(drops213.get("380_db"), drops213.get("380yiwei_db"));
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void selectedHiddenDockFlowLocksBothReadOnlySources() throws Exception {
        String configPath = System.getProperty("lito.legacy.config",
                "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        Map<String, MonsterSourceEvidence> monsters = new LinkedHashMap<>();
        Map<String, DropSourceEvidence> drops = new LinkedHashMap<>();
        Map<String, SourceEvidence> staticNpcs = new LinkedHashMap<>();
        Map<String, SourceEvidence> routes = new LinkedHashMap<>();
        Map<String, SourceEvidence> flags = new LinkedHashMap<>();
        Map<String, SourceEvidence> getbacks = new LinkedHashMap<>();
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            for (String source : config.classicServerNames()) {
                AccountRepository repository = routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                try (Connection connection = connection(repository)) {
                    monsters.put(database, monsterEvidence(connection, database, 445));
                    drops.put(database, dropEvidence(connection, database, 445));
                    staticNpcs.put(database, staticNpcEvidence(connection, database, 445));
                    routes.put(database,
                            hiddenDockTeleportEvidence(connection, database));
                    StringBuilder mapFlags = new StringBuilder(
                            MAP350_FLAGS_COLUMNS).append('\n');
                    int flagRows = appendCanonicalRows(connection,
                            "SELECT mapid,locationname,startX,endX,startY,endY,"
                            + "escapable,usable_item,usable_skill,teleportable,"
                            + "take_pets,recall_pets FROM " + database
                            + ".`mapids` WHERE mapid=445", 12, mapFlags);
                    flags.put(database, new SourceEvidence(
                            flagRows, sha256(mapFlags.toString())));
                    int count;
                    try (PreparedStatement statement = connection.prepareStatement(
                            "SELECT COUNT(*) FROM " + database
                                    + ".`getback` WHERE area_mapid=445");
                         ResultSet row = statement.executeQuery()) {
                        assertTrue(row.next());
                        count = row.getInt(1);
                    }
                    getbacks.put(database, new SourceEvidence(1,
                            sha256("area_mapid\trows\n445\t" + count + "\n")));
                    System.out.printf("[LiTo-test] source=%s hidden-dock route772=%d"
                                    + " static=%d monsters=%d/%d drops=%d/%d"
                                    + " flags=%s getback=%s read-only=true%n",
                            source, routes.get(database).rows(),
                            staticNpcs.get(database).rows(), monsters.get(database).rows(),
                            monsters.get(database).population(), drops.get(database).rows(),
                            drops.get(database).items(), flags.get(database).sha256(),
                            getbacks.get(database).sha256());
                }
            }
        }

        MonsterSourceEvidence emptyMonsters = new MonsterSourceEvidence(
                0, 0, ClassicWorldMonsterCatalog.MAP445_SOURCE_SHA256);
        DropSourceEvidence emptyDrops = new DropSourceEvidence(
                0, 0, ClassicWorldDropCatalog.MAP445_SOURCE_SHA256);
        SourceEvidence exactStatic = new SourceEvidence(
                1, ClassicWorldNpcCatalog.MAP445_SOURCE_SHA256);
        SourceEvidence exactRoute = new SourceEvidence(
                1, ClassicWorldTeleportCatalog.HIDDEN_DOCK_SOURCE_SHA256);
        SourceEvidence exactFlags = new SourceEvidence(
                1, ClassicWorldMapCatalog.MAP445_FLAGS_SOURCE_SHA256);
        SourceEvidence exactGetback = new SourceEvidence(
                1, ClassicWorldGetbackCatalog.MAP445_ABSENCE_SOURCE_SHA256);
        for (String database : new String[] {"380_db", "380yiwei_db"}) {
            assertEquals(emptyMonsters, monsters.get(database));
            assertEquals(emptyDrops, drops.get(database));
            assertEquals(exactStatic, staticNpcs.get(database));
            assertEquals(exactRoute, routes.get(database));
            assertEquals(exactFlags, flags.get(database));
            assertEquals(exactGetback, getbacks.get(database));
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void selectedPirateBoardingFlowLocksBothReadOnlySources() throws Exception {
        String configPath = System.getProperty("lito.legacy.config",
                "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        Map<String, SourceEvidence> merchants = new LinkedHashMap<>();
        Map<String, SourceEvidence> portals = new LinkedHashMap<>();
        Map<String, SourceEvidence> flags = new LinkedHashMap<>();
        Map<String, SourceEvidence> staticNpcs = new LinkedHashMap<>();
        Map<String, MonsterSourceEvidence> monsters = new LinkedHashMap<>();
        Map<String, DropSourceEvidence> drops = new LinkedHashMap<>();
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            for (String source : config.classicServerNames()) {
                AccountRepository repository = routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                try (Connection connection = connection(repository)) {
                    StringBuilder merchant = new StringBuilder(
                            PIRATE_MERCHANT_COLUMNS).append('\n');
                    int merchantRows = appendCanonicalRows(connection,
                            "SELECT s.mapid,s.id,s.count,s.npc_templateid,s.locx,s.locy,"
                            + "s.heading,s.movement_distance,n.nameid,n.impl,n.gfxid,"
                            + "a.normal_action,a.caotic_action,a.teleport_url,sh.item_id,"
                            + "sh.order_id,sh.selling_price,sh.pack_count,"
                            + "sh.purchasing_price,sh.daily_buy_limit,e.name_id,"
                            + "e.itemdesc_id,e.use_type,e.item_type,e.material,e.weight,"
                            + "e.invgfx,e.grdgfx,e.stackable,e.trade,e.bless,e.delay_id,"
                            + "e.delay_time FROM " + database + ".`spawnlist_npc` s JOIN "
                            + database + ".`npc` n ON n.npcid=s.npc_templateid JOIN "
                            + database + ".`npcaction` a ON a.npcid=s.npc_templateid JOIN "
                            + database + ".`shop` sh ON sh.npc_id=s.npc_templateid JOIN "
                            + database + ".`etcitem` e ON e.item_id=sh.item_id "
                            + "WHERE s.mapid=445 AND s.id=601 AND s.npc_templateid=70094 "
                            + "AND s.count=1 AND sh.order_id=0 ORDER BY s.id,sh.order_id",
                            33, merchant);
                    merchants.put(database, new SourceEvidence(
                            merchantRows, sha256(merchant.toString())));

                    boolean requiredItem = hasColumn(
                            repository, database, "dungeon", "require_item_id");
                    StringBuilder portal = new StringBuilder(requiredItem
                            ? PIRATE_PORTAL_COLUMNS : PIRATE_PORTAL_SECONDARY_COLUMNS)
                            .append('\n');
                    String portalSql = "SELECT src_mapid,src_x,src_y,new_mapid,new_x,new_y,"
                            + "new_heading," + (requiredItem ? "require_item_id," : "")
                            + "note FROM " + database + ".`dungeon` WHERE src_mapid IN "
                            + "(445,447) ORDER BY src_mapid,src_x,src_y,new_mapid,new_x,new_y";
                    int portalRows = appendCanonicalRows(
                            connection, portalSql, requiredItem ? 9 : 8, portal);
                    portals.put(database, new SourceEvidence(
                            portalRows, sha256(portal.toString())));

                    StringBuilder mapFlags = new StringBuilder(
                            MAP350_FLAGS_COLUMNS).append('\n');
                    int flagRows = appendCanonicalRows(connection,
                            "SELECT mapid,locationname,startX,endX,startY,endY,"
                            + "escapable,usable_item,usable_skill,teleportable,"
                            + "take_pets,recall_pets FROM " + database
                            + ".`mapids` WHERE mapid=447", 12, mapFlags);
                    flags.put(database, new SourceEvidence(
                            flagRows, sha256(mapFlags.toString())));
                    staticNpcs.put(database, staticNpcEvidence(connection, database, 447));
                    monsters.put(database, monsterEvidence(connection, database, 447));
                    drops.put(database, dropEvidence(connection, database, 447));
                    System.out.printf("[LiTo-test] source=%s Duran=1 ticket40302/690"
                                    + " portals=%d required-item-column=%s map447"
                                    + " static=%d monsters=%d/%d drops=%d/%d"
                                    + " read-only=true%n",
                            source, portalRows, requiredItem,
                            staticNpcs.get(database).rows(), monsters.get(database).rows(),
                            monsters.get(database).population(), drops.get(database).rows(),
                            drops.get(database).items());
                }
            }
        }

        SourceEvidence merchant = new SourceEvidence(
                1, ClassicWorldMerchantCatalog.DURAN_SOURCE_SHA256);
        SourceEvidence selectedPortals = new SourceEvidence(
                8, ClassicWorldDungeonCatalog.SOURCE_SHA256);
        SourceEvidence secondaryPortals = new SourceEvidence(8,
                "A8F29EF5C12CD24C87D9D0E177CD47F3676228317710C6CAA863CF76FF1B907F");
        SourceEvidence map447Flags = new SourceEvidence(
                1, ClassicWorldDungeonCatalog.MAP447_FLAGS_SOURCE_SHA256);
        SourceEvidence emptyStatic = new SourceEvidence(
                0, ClassicWorldNpcCatalog.MAP447_SOURCE_SHA256);
        MonsterSourceEvidence emptyMonsters = new MonsterSourceEvidence(
                0, 0, ClassicWorldMonsterCatalog.MAP447_SOURCE_SHA256);
        DropSourceEvidence emptyDrops = new DropSourceEvidence(
                0, 0, ClassicWorldDropCatalog.MAP447_SOURCE_SHA256);
        for (String database : new String[] {"380_db", "380yiwei_db"}) {
            assertEquals(merchant, merchants.get(database));
            assertEquals(map447Flags, flags.get(database));
            assertEquals(emptyStatic, staticNpcs.get(database));
            assertEquals(emptyMonsters, monsters.get(database));
            assertEquals(emptyDrops, drops.get(database));
        }
        assertEquals(selectedPortals, portals.get("380_db"));
        assertEquals(secondaryPortals, portals.get("380yiwei_db"));
        assertNotEquals(portals.get("380_db"), portals.get("380yiwei_db"));
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void homeScrollMapFlagsAndGetbackAbsenceMatchBothReadOnlySources()
            throws Exception {
        String configPath = System.getProperty("lito.legacy.config",
                "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            for (String source : config.classicServerNames()) {
                AccountRepository repository = routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                try (Connection connection = connection(repository)) {
                    StringBuilder item = new StringBuilder(
                            "item_id\tname_id\titem_type\tuse_type\tclassname\tdelay_id"
                            + "\tdelay_time\tinvgfx\tgrdgfx\titemdesc_id\tweight"
                            + "\tstackable\ttrade\tbless\tmin_lvl\tmax_lvl\n");
                    assertEquals(1, appendCanonicalRows(connection,
                            "SELECT item_id,name_id,item_type,use_type,classname,delay_id,"
                            + "delay_time,invgfx,grdgfx,itemdesc_id,weight,stackable,trade,"
                            + "bless,min_lvl,max_lvl FROM " + database
                            + ".`etcitem` WHERE item_id=40079", 16, item));
                    assertEquals(InventoryCatalog.HOME_SCROLL_SOURCE_SHA256,
                            sha256(item.toString()));

                    StringBuilder maps = new StringBuilder(
                            "mapid\tescapable\tusable_item\tusable_skill\n");
                    assertEquals(2, appendCanonicalRows(connection,
                            "SELECT mapid,escapable,usable_item,usable_skill FROM "
                            + database + ".`mapids` WHERE mapid IN (4,303)"
                            + " ORDER BY mapid", 4, maps));
                    assertEquals(ClassicWorldMapCatalog.ESCAPE_FLAGS_SOURCE_SHA256,
                            sha256(maps.toString()));

                    int getbackRows;
                    try (PreparedStatement statement = connection.prepareStatement(
                            "SELECT COUNT(*) FROM " + database
                                    + ".`getback` WHERE area_mapid=303");
                         ResultSet row = statement.executeQuery()) {
                        assertTrue(row.next());
                        getbackRows = row.getInt(1);
                    }
                    assertEquals(0, getbackRows);
                    assertEquals(ClassicWorldGetbackCatalog.MAP303_ABSENCE_SOURCE_SHA256,
                            sha256("area_mapid\trows\n303\t0\n"));

                    try (PreparedStatement statement = connection.prepareStatement(
                            "SELECT area,locx,locy,mapid FROM " + database
                                    + ".`getback_restart` WHERE area=303");
                         ResultSet row = statement.executeQuery()) {
                        assertTrue(row.next());
                        assertEquals(303, row.getInt(1));
                        assertEquals(33976, row.getInt(2));
                        assertEquals(32936, row.getInt(3));
                        assertEquals(4, row.getInt(4));
                        assertFalse(row.next());
                    }
                    System.out.printf("[LiTo-test] source=%s home-scroll=40079"
                                    + " use-type=home delay=4/1500 map303-escapable=true"
                                    + " getback303-rows=0 fallback=4:33089,33397/5"
                                    + " restart-row-not-used=4:33976,32936 read-only=true%n",
                            source);
                }
            }
        }

        Path retained = Path.of("..", "1-伺服器").toAbsolutePath().normalize();
        String use = read(retained,
                "src/com/lineage/server/clientpackets/C_ItemUSe.java");
        String executor = read(retained,
                "src/com/lineage/data/item_etcitem/teleport/GoHome_Reel.java");
        String getback = read(retained,
                "src/com/lineage/server/datatables/GetbackTable.java");
        String teleport = read(retained,
                "src/com/lineage/server/model/L1Teleport.java");
        assertTrue(use.contains("case 9"));
        assertTrue(use.contains("L1BuffUtil.getUseItemTeleport(pc)"));
        assertTrue(executor.contains("pc.getMap().isEscapable() || pc.isGm()"));
        assertTrue(executor.contains("GetbackTable.GetBack_Location(pc, true)"));
        assertTrue(executor.contains("(short) loc[2], 5, true"));
        assertTrue(executor.contains("removeItem(item, 1)"));
        assertTrue(getback.contains("loc[0] = 33089"));
        assertTrue(getback.contains("loc[1] = 33397"));
        assertTrue(teleport.contains("EFFECT_SPR = { 169, 2235, 2236, 2281 }"));
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void selectedClassicExperienceResourceMatchesItsReadOnlyDatabaseSource()
            throws Exception {
        String configPath = System.getProperty("lito.legacy.config",
                "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        Map<String, ExperienceSourceEvidence> observed = new LinkedHashMap<>();
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            for (String source : config.classicServerNames()) {
                AccountRepository repository = routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                try (Connection connection = connection(repository)) {
                    ExperienceSourceEvidence evidence =
                            experienceEvidence(connection, database);
                    observed.put(database, evidence);
                    System.out.printf("[LiTo-test] source=%s classic-exp-rows=%d"
                                    + " maximum-level=%d schema=%s sha256=%s"
                                    + " read-only=true%n",
                            source, evidence.rows(), evidence.maximumLevel(),
                            database, evidence.sha256());
                }
            }
        }
        assertEquals(new ExperienceSourceEvidence(101, 99,
                        ClassicExperienceCatalog.SOURCE_SHA256), observed.get("380_db"));
        assertEquals(new ExperienceSourceEvidence(101, 99,
                        "8B9F51F85D4FA541A70C14470F3B929E2E312EBC214F9AF9F480EB155BBE2CEA"),
                observed.get("380yiwei_db"));
        assertNotEquals(observed.get("380_db"), observed.get("380yiwei_db"));
    }

    @Test
    @EnabledIfSystemProperty(named = "lito.legacy.integration", matches = "true")
    void selectedMap4DropResourceMatchesItsReadOnlyDatabaseSource()
            throws Exception {
        String configPath = System.getProperty("lito.legacy.config",
                "src/test/resources/server-config/config/server.source-router-legacy-probe.properties");
        ServerConfig config = ServerConfig.load(Path.of(configPath));
        Map<String, DropSourceEvidence> observed = new LinkedHashMap<>();
        try (AccountRepository routed = AccountRepository.open(config)) {
            routed.healthCheck();
            for (String source : config.classicServerNames()) {
                AccountRepository repository = routed.repositoryForClassicSource(source);
                String database = stringField(repository, "resourceDatabase");
                try (Connection connection = connection(repository)) {
                    DropSourceEvidence evidence = dropEvidence(connection, database);
                    observed.put(database, evidence);
                    System.out.printf("[LiTo-test] source=%s map4-drop-rows=%d"
                                    + " item-definitions=%d schema=%s sha256=%s"
                                    + " read-only=true%n",
                            source, evidence.rows(), evidence.items(), database,
                            evidence.sha256());
                }
            }
        }
        assertEquals(new DropSourceEvidence(
                        ClassicWorldDropCatalog.MAP4_DROP_COUNT,
                        ClassicWorldDropCatalog.MAP4_ITEM_COUNT,
                        ClassicWorldDropCatalog.MAP4_SOURCE_SHA256),
                observed.get("380_db"));
        assertEquals(new DropSourceEvidence(634, 152,
                        "683605B3F261D2FC669703AC9EE4A7950C5D24A5E6D7E206A26CF484616A2656"),
                observed.get("380yiwei_db"));
        assertNotEquals(observed.get("380_db"), observed.get("380yiwei_db"));
    }

    private static SourceEvidence staticNpcEvidence(
            Connection connection, String database) throws Exception {
        return staticNpcEvidence(connection, database, 4);
    }

    private static SourceEvidence staticNpcEvidence(
            Connection connection, String database, int mapId) throws Exception {
        String sql = "SELECT s.id,s.npc_templateid,s.locx,s.locy,s.heading,"
                + "s.movement_distance,n.nameid,n.impl,n.gfxid,n.lvl,n.hp,n.mp,"
                + "n.exp,n.lawful,n.light_size,n.passispeed,n.atkspeed,n.change_head,"
                + "COALESCE(a.normal_action,''),COALESCE(a.caotic_action,'') "
                + "FROM " + database + ".`spawnlist_npc` s "
                + "JOIN " + database + ".`npc` n ON n.npcid=s.npc_templateid "
                + "LEFT JOIN " + database + ".`npcaction` a "
                + "ON a.npcid=s.npc_templateid "
                + "WHERE s.mapid=" + mapId + " AND s.count>0 ORDER BY s.id";
        StringBuilder canonical = new StringBuilder(STATIC_NPC_COLUMNS).append('\n');
        int rows = 0;
        int previousSpawnId = 0;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                int spawnId = result.getInt(1);
                assertTrue(spawnId > previousSpawnId,
                        "spawn ids must be strictly increasing");
                previousSpawnId = spawnId;
                for (int column = 1; column <= 20; column++) {
                    String value = result.getString(column);
                    assertFalse(value == null || value.indexOf('\t') >= 0
                                    || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0,
                            "non-canonical static NPC source value");
                    if (column > 1) {
                        canonical.append('\t');
                    }
                    canonical.append(value);
                }
                canonical.append('\n');
                rows++;
            }
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return new SourceEvidence(rows, HexFormat.of().withUpperCase().formatHex(
                digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8))));
    }

    private static MonsterSourceEvidence monsterEvidence(
            Connection connection, String database) throws Exception {
        return monsterEvidence(connection, database, 4);
    }

    private static MonsterSourceEvidence monsterEvidence(
            Connection connection, String database, int mapId) throws Exception {
        String sql = "SELECT s.id,s.location,s.count,s.npc_templateid,s.group_id,"
                + "s.locx,s.locy,s.randomx,s.randomy,"
                + "s.locx1,s.locy1,s.locx2,s.locy2,s.heading,"
                + "s.min_respawn_delay,s.max_respawn_delay,s.respawn_screen,"
                + "s.movement_distance,s.rest,s.near_spawn,"
                + "n.nameid,n.impl,n.gfxid,n.lvl,n.hp,n.mp,n.ac,n.str,n.con,n.dex,"
                + "n.wis,n.intel,n.mr,n.exp,n.lawful,n.size,n.weakAttr,n.ranged,"
                + "n.passispeed,n.atkspeed,n.bowActId,n.undead,n.poison_atk,n.paralysis_atk,n.agro,"
                + "n.damage_reduction,n.hard,n.doppel,n.boss,n.light_size,n.change_head "
                + "FROM " + database + ".`spawnlist` s "
                + "JOIN " + database + ".`npc` n ON n.npcid=s.npc_templateid "
                + "WHERE s.mapid=" + mapId + " AND s.count>0 ORDER BY s.id";
        StringBuilder canonical = new StringBuilder(MONSTER_COLUMNS).append('\n');
        int rows = 0;
        int population = 0;
        int previousSpawnId = 0;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                int spawnId = result.getInt(1);
                assertTrue(spawnId > previousSpawnId,
                        "spawn ids must be strictly increasing");
                previousSpawnId = spawnId;
                int count = result.getInt(3);
                assertTrue(count > 0);
                assertEquals("L1Monster", result.getString(22));
                population += count;
                for (int column = 1; column <= 51; column++) {
                    String value = result.getString(column);
                    assertFalse(value == null || value.indexOf('\t') >= 0
                                    || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0,
                            "non-canonical monster source value");
                    if (column > 1) {
                        canonical.append('\t');
                    }
                    canonical.append(value);
                }
                canonical.append('\n');
                rows++;
            }
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return new MonsterSourceEvidence(rows, population,
                HexFormat.of().withUpperCase().formatHex(
                        digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8))));
    }

    private static ProjectileSourceEvidence projectileEvidence(
            Connection connection, String database) throws Exception {
        String sql = "SELECT COUNT(DISTINCT s.id),SUM(s.count),"
                + "COUNT(DISTINCT n.npcid) FROM " + database + ".`spawnlist` s "
                + "JOIN " + database + ".`npc` n ON n.npcid=s.npc_templateid "
                + "WHERE s.mapid=4 AND s.count>0 AND n.impl='L1Monster' "
                + "AND n.ranged>1 AND n.bowActId>0";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            assertTrue(result.next());
            ProjectileSourceEvidence evidence = new ProjectileSourceEvidence(
                    result.getInt(1), result.getInt(2), result.getInt(3));
            assertFalse(result.next());
            return evidence;
        }
    }

    private static ExperienceSourceEvidence experienceEvidence(
            Connection connection, String database) throws Exception {
        StringBuilder canonical = new StringBuilder(
                "level\texp\texp_penalty\tdeath_penalty_rate\n");
        int rows = 0;
        int highestSourceLevel = -1;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT level,exp,expPenalty,death_penalty_rate FROM "
                        + database + ".`exp` ORDER BY level");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                int sourceLevel = result.getInt(1);
                assertEquals(rows, sourceLevel);
                canonical.append(sourceLevel).append('\t')
                        .append(result.getLong(2)).append('\t')
                        .append(String.format(Locale.ROOT, "%.2f",
                                result.getDouble(3))).append('\t')
                        .append(String.format(Locale.ROOT, "%.2f",
                                result.getDouble(4))).append('\n');
                highestSourceLevel = sourceLevel;
                rows++;
            }
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String hash = HexFormat.of().withUpperCase().formatHex(digest.digest(
                canonical.toString().getBytes(StandardCharsets.US_ASCII)));
        return new ExperienceSourceEvidence(rows, highestSourceLevel - 1, hash);
    }

    private static DropSourceEvidence dropEvidence(
            Connection connection, String database) throws Exception {
        return dropEvidence(connection, database, 4);
    }

    private static DropSourceEvidence dropEvidence(
            Connection connection, String database, int mapId) throws Exception {
        String mobFilter = "SELECT DISTINCT npc_templateid FROM " + database
                + ".`spawnlist` WHERE mapid=" + mapId + " AND count>0";
        String dropSql = "SELECT d.mobId,d.itemId,d.min,d.max,d.chance FROM "
                + database + ".`droplist` d JOIN (" + mobFilter
                + ") s ON s.npc_templateid=d.mobId ORDER BY d.mobId,d.itemId";
        String itemSql = "SELECT item_id,kind,name_id,use_token,invgfx,grdgfx,bless,"
                + "stackable,tradable,weight,itemdesc_id,weapon_type,dmg_small,dmg_large,"
                + "material,safenchant,class_mask FROM ("
                + "SELECT w.item_id,'weapon' AS kind,w.name_id,'weapon' AS use_token,"
                + "w.invgfx,w.grdgfx,w.bless,0 AS stackable,IF(w.trade=0,1,0) AS tradable,"
                + "w.weight,w.itemdesc_id,w.type AS weapon_type,w.dmg_small,w.dmg_large,"
                + "w.material,w.safenchant,(IF(w.use_royal<>0,1,0)+IF(w.use_knight<>0,2,0)"
                + "+IF(w.use_mage<>0,4,0)+IF(w.use_elf<>0,8,0)+IF(w.use_darkelf<>0,16,0)"
                + "+IF(w.use_dragonknight<>0,32,0)+IF(w.use_illusionist<>0,64,0)) AS class_mask "
                + "FROM " + database + ".`weapon` w WHERE w.item_id IN (SELECT DISTINCT "
                + "d.itemId FROM " + database + ".`droplist` d JOIN (" + mobFilter
                + ") s ON s.npc_templateid=d.mobId) UNION ALL "
                + "SELECT a.item_id,'armor',a.name_id,a.type,a.invgfx,a.grdgfx,a.bless,0,"
                + "IF(a.trade=0,1,0),a.weight,a.itemdesc_id,'',0,0,a.material,a.safenchant,0 "
                + "FROM " + database + ".`armor` a WHERE a.item_id IN (SELECT DISTINCT "
                + "d.itemId FROM " + database + ".`droplist` d JOIN (" + mobFilter
                + ") s ON s.npc_templateid=d.mobId) UNION ALL "
                + "SELECT e.item_id,'etc',e.name_id,e.use_type,e.invgfx,e.grdgfx,e.bless,"
                + "IF(e.stackable<>0,1,0),IF(e.trade=0,1,0),e.weight,e.itemdesc_id,'',0,0,"
                + "e.material,0,0 FROM " + database
                + ".`etcitem` e WHERE e.item_id IN (SELECT DISTINCT d.itemId FROM "
                + database + ".`droplist` d JOIN (" + mobFilter
                + ") s ON s.npc_templateid=d.mobId)) item_rows ORDER BY item_id";

        StringBuilder canonical = new StringBuilder("[drops]\n")
                .append(DROP_COLUMNS).append('\n');
        int dropRows = appendCanonicalRows(connection, dropSql, 5, canonical);
        canonical.append("[items]\n").append(DROP_ITEM_COLUMNS).append('\n');
        int itemRows = appendCanonicalRows(connection, itemSql, 17, canonical);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return new DropSourceEvidence(dropRows, itemRows,
                HexFormat.of().withUpperCase().formatHex(digest.digest(
                        canonical.toString().getBytes(StandardCharsets.UTF_8))));
    }

    private static SourceEvidence phantasmTeleportEvidence(
            Connection connection, String database) throws Exception {
        String sql = "SELECT s.mapid,s.id,s.npc_templateid,s.locx,s.locy,"
                + "a.normal_action,t.id,t.action_name,t.loc_x,t.loc_y,t.map_id,"
                + "t.heading,t.price_amount,t.price_item_id,t.min_level,t.max_level "
                + "FROM " + database + ".`spawnlist_npc` s JOIN " + database
                + ".`npcaction` a ON a.npcid=s.npc_templateid JOIN " + database
                + ".`npc_action_teleport` t ON FIND_IN_SET(CAST(s.npc_templateid AS CHAR),"
                + "REPLACE(t.npc_id,' ',''))>0 WHERE s.mapid=4 AND s.id=83 "
                + "AND t.id=429 ORDER BY s.id,t.id";
        StringBuilder canonical = new StringBuilder(
                PHANTASM_TELEPORT_COLUMNS).append('\n');
        int rows = appendCanonicalRows(connection, sql, 16, canonical);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return new SourceEvidence(rows, HexFormat.of().withUpperCase().formatHex(
                digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8))));
    }

    private static SourceEvidence giranTradeTeleportEvidence(
            Connection connection, String database) throws Exception {
        String sql = "SELECT s.mapid,s.id,s.count,s.npc_templateid,s.locx,s.locy,"
                + "a.normal_action,t.id,t.action_name,t.loc_x,t.loc_y,t.map_id,"
                + "t.heading,t.price_amount,t.price_item_id,t.min_level,t.max_level "
                + "FROM " + database + ".`spawnlist_npc` s JOIN " + database
                + ".`npcaction` a ON a.npcid=s.npc_templateid JOIN " + database
                + ".`npc_action_teleport` t ON FIND_IN_SET(CAST(s.npc_templateid AS CHAR),"
                + "REPLACE(t.npc_id,' ',''))>0 WHERE s.mapid=4 "
                + "AND s.npc_templateid=50034 AND s.count>0 AND t.id=181 "
                + "ORDER BY s.id,t.id";
        StringBuilder canonical = new StringBuilder(
                ENABLED_TELEPORT_COLUMNS).append('\n');
        int rows = appendCanonicalRows(connection, sql, 17, canonical);
        return new SourceEvidence(rows, sha256(canonical.toString()));
    }

    private static SourceEvidence orcTeleportEvidence(
            Connection connection, String database) throws Exception {
        String sql = "SELECT s.mapid,s.id,s.count,s.npc_templateid,s.locx,s.locy,"
                + "a.normal_action,t.id,t.action_name,t.loc_x,t.loc_y,t.map_id,"
                + "t.heading,t.price_amount,t.price_item_id,t.min_level,t.max_level "
                + "FROM " + database + ".`spawnlist_npc` s JOIN " + database
                + ".`npcaction` a ON a.npcid=s.npc_templateid JOIN " + database
                + ".`npc_action_teleport` t ON FIND_IN_SET(CAST(s.npc_templateid AS CHAR),"
                + "REPLACE(t.npc_id,' ',''))>0 WHERE s.mapid=4 AND s.id=106 "
                + "AND s.npc_templateid=50063 AND s.count>0 "
                + "AND t.id IN (769,770,771) ORDER BY s.id,t.id";
        StringBuilder canonical = new StringBuilder(
                ENABLED_TELEPORT_COLUMNS).append('\n');
        int rows = appendCanonicalRows(connection, sql, 17, canonical);
        return new SourceEvidence(rows, sha256(canonical.toString()));
    }

    private static SourceEvidence fairyDungeonTeleportEvidence(
            Connection connection, String database) throws Exception {
        String sql = "SELECT s.mapid,s.id,s.count,s.npc_templateid,s.locx,s.locy,"
                + "a.normal_action,t.id,t.action_name,t.loc_x,t.loc_y,t.map_id,"
                + "t.heading,t.price_amount,t.price_item_id,t.min_level,t.max_level "
                + "FROM " + database + ".`spawnlist_npc` s JOIN " + database
                + ".`npcaction` a ON a.npcid=s.npc_templateid JOIN " + database
                + ".`npc_action_teleport` t ON FIND_IN_SET(CAST(s.npc_templateid AS CHAR),"
                + "REPLACE(t.npc_id,' ',''))>0 WHERE s.mapid=4 AND s.id=1154 "
                + "AND s.npc_templateid=70853 AND s.count>0 "
                + "AND t.id IN (1142,1143) ORDER BY s.id,t.id";
        StringBuilder canonical = new StringBuilder(
                ENABLED_TELEPORT_COLUMNS).append('\n');
        int rows = appendCanonicalRows(connection, sql, 17, canonical);
        return new SourceEvidence(rows, sha256(canonical.toString()));
    }

    private static SourceEvidence hiddenDockTeleportEvidence(
            Connection connection, String database) throws Exception {
        String sql = "SELECT s.mapid,s.id,s.count,s.npc_templateid,s.locx,s.locy,"
                + "a.normal_action,t.id,t.action_name,t.loc_x,t.loc_y,t.map_id,"
                + "t.heading,t.price_amount,t.price_item_id,t.min_level,t.max_level "
                + "FROM " + database + ".`spawnlist_npc` s JOIN " + database
                + ".`npcaction` a ON a.npcid=s.npc_templateid JOIN " + database
                + ".`npc_action_teleport` t ON FIND_IN_SET(CAST(s.npc_templateid AS CHAR),"
                + "REPLACE(t.npc_id,' ',''))>0 WHERE s.mapid=4 AND s.id=107 "
                + "AND s.npc_templateid=50064 AND s.count>0 AND t.id=772 "
                + "ORDER BY s.id,t.id";
        StringBuilder canonical = new StringBuilder(
                ENABLED_TELEPORT_COLUMNS).append('\n');
        int rows = appendCanonicalRows(connection, sql, 17, canonical);
        return new SourceEvidence(rows, sha256(canonical.toString()));
    }

    private static int appendCanonicalRows(
            Connection connection, String sql, int columns, StringBuilder canonical)
            throws Exception {
        int rows = 0;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                for (int column = 1; column <= columns; column++) {
                    String value = result.getString(column);
                    assertFalse(value == null || value.indexOf('\t') >= 0
                                    || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0,
                            "non-canonical drop/item source value");
                    if (column > 1) canonical.append('\t');
                    canonical.append(value);
                }
                canonical.append('\n');
                rows++;
            }
        }
        return rows;
    }

    private static String sha256(String canonical) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().withUpperCase().formatHex(
                digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
    }

    private record SourceEvidence(int rows, String sha256) {}
    private record MonsterSourceEvidence(int rows, int population, String sha256) {}
    private record ProjectileSourceEvidence(int groups, int population, int templates) {}
    private record ExperienceSourceEvidence(int rows, int maximumLevel, String sha256) {}
    private record DropSourceEvidence(int rows, int items, String sha256) {}

    private static void assertRetainedJavaSemantics() throws Exception {
        Path retained = Path.of("..", "1-伺服器").toAbsolutePath().normalize();
        String client = read(retained,
                "src/com/lineage/server/clientpackets/C_UseSkill.java");
        assertTrue(client.contains("case 131"));

        String skillIds = read(retained,
                "src/com/lineage/server/model/skill/L1SkillId.java");
        assertTrue(skillIds.contains("TELEPORT_TO_MATHER = 131"));

        String mode = read(retained,
                "src/com/lineage/server/model/skill/skillmode/TELEPORT_TO_MATHER.java");
        assertTrue(mode.contains("THUNDER_GRAB"));
        assertTrue(mode.contains("SLAY_BREAK_NOT_TELEPORT"));
        assertWorldTreeMode(mode);

        String use = read(retained,
                "src/com/lineage/server/model/skill/L1SkillUse.java");
        assertTrue(use.contains("114, 115, 116, 117, 118, 129, 130, 131"));
        assertTrue(use.contains("TELEPORT_TO_MATHER -> // 解除傳送鎖定"));
        assertTrue(use.contains("S_Paralysis.TYPE_TELEPORT_UNLOCK"));

        Path older = retained.resolve(Path.of("其他src參考用", "381n8src"));
        String olderClient = read(older,
                "com/lineage/server/clientpackets/C_UseSkill.java");
        String olderIds = read(older,
                "com/lineage/server/model/skill/L1SkillId.java");
        String olderMode = read(older,
                "com/lineage/server/model/skill/skillmode/TELEPORT_TO_MATHER.java");
        String olderUse = read(older,
                "com/lineage/server/model/skill/L1SkillUse.java");
        assertTrue(olderClient.contains("case 131"));
        assertTrue(olderIds.contains("TELEPORT_TO_MATHER = 131"));
        assertWorldTreeMode(olderMode);
        assertTrue(olderUse.contains("114, 115, 116, 117, 118, 129, 130, 131"));
        assertTrue(olderUse.contains("case TELEPORT_TO_MATHER"));
        assertTrue(olderUse.contains("S_Paralysis.TYPE_TELEPORT_UNLOCK"));
    }

    private static void assertWorldTreeMode(String mode) {
        assertTrue(mode.contains("pc.getMap().isEscapable() || pc.isGm()"));
        assertTrue(mode.contains(
                "L1Teleport.teleport(pc, 33051, 32337, (short) 4, 5, true)"));
        assertTrue(mode.contains("new S_ServerMessage(276)"));
        assertTrue(mode.contains("S_Paralysis.TYPE_TELEPORT_UNLOCK"));
    }

    private static void assertRetainedNpcTeleportSemantics() throws Exception {
        Path retained = Path.of("..", "1-伺服器").toAbsolutePath().normalize();
        String executor = read(retained,
                "src/com/lineage/server/model/npc/action/NpcTeleportExecutor.java");
        assertTrue(executor.contains("if (priceAmount <= 0)"));
        assertTrue(executor.contains("return true;"));
        assertTrue(executor.contains(
                "L1Teleport.teleport(pc, locX, locY, mapId, heading, true)"));

        String teleport = read(retained,
                "src/com/lineage/server/model/L1Teleport.java");
        assertTrue(teleport.contains("EFFECT_SPR = { 169, 2235, 2236, 2281 }"));
        assertTrue(teleport.contains("pc.setPetModel()"));
        assertTrue(teleport.contains("pc.getPetModel()")
                || read(retained,
                    "src/com/lineage/server/utils/Teleportation.java")
                    .contains("pc.getPetModel()"));

        String getback = read(retained,
                "src/com/lineage/server/datatables/GetbackTable.java");
        assertTrue(getback.contains("if (gb.isSpecifyArea())"));
        assertTrue(getback.contains("getback = gb;"));
        assertTrue(getback.contains("L1TownLocation.getGetBackLoc(getback._getbackTownId)"));
    }

    private static void assertRetainedWarehouseSemantics() throws Exception {
        Path retained = Path.of("..", "1-伺服器").toAbsolutePath().normalize();
        String dwarf = read(retained,
                "src/com/lineage/server/model/Instance/L1DwarfInstance.java");
        String action = read(retained,
                "src/com/lineage/server/clientpackets/C_NPCAction.java");
        String retrieve = read(retained,
                "src/com/lineage/server/serverpackets/S_RetrieveList.java");
        String pledge = read(retained,
                "src/com/lineage/server/serverpackets/S_RetrievePledgeList.java");
        String elf = read(retained,
                "src/com/lineage/server/serverpackets/S_RetrieveElfList.java");
        assertTrue(dwarf.contains("class L1DwarfInstance"));
        assertTrue(dwarf.contains("new S_NPCTalkReturn(talking, objid, 1)"));
        assertTrue(action.contains("case \"retrieve\""));
        assertTrue(action.contains("case \"retrieve-pledge\""));
        assertTrue(action.contains("case \"retrieve-elven\""));
        assertTrue(action.contains("new S_ServerMessage(834)"));
        assertTrue(retrieve.contains("writeC(0x03)"));
        assertTrue(retrieve.contains("writeD(30)"));
        assertTrue(pledge.contains("writeC(0x05)"));
        assertTrue(pledge.contains("writeD(30)"));
        assertTrue(elf.contains("writeC(0x09)"));
    }

    private static String read(Path root, String relative) throws Exception {
        return Files.readString(root.resolve(relative.replace('/', '\\')),
                StandardCharsets.UTF_8);
    }

    private static boolean hasColumn(AccountRepository repository, String database,
                                     String table, String column) throws Exception {
        try (Connection connection = connection(repository);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE TABLE_SCHEMA=? AND TABLE_NAME=?"
                             + " AND COLUMN_NAME=?")) {
            statement.setString(1, database);
            statement.setString(2, table);
            statement.setString(3, column);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() && row.getInt(1) == 1;
            }
        }
    }

    private static Connection connection(AccountRepository repository)
            throws Exception {
        Connection connection = DriverManager.getConnection(
                stringField(repository, "accountUrl"),
                stringField(repository, "accountLogin"),
                stringField(repository, "accountPassword"));
        connection.setReadOnly(true);
        return connection;
    }

    private static String stringField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (String) field.get(target);
    }
}

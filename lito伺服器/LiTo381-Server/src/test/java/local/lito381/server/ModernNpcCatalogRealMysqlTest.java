package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcCatalog.Field.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/** Actual Connector/J and fresh owned MySQL; never an installed account/catalog database. */
class ModernNpcCatalogRealMysqlTest {
    static final String FIRST = "npc_catalog_a", SECOND = "npc_catalog_b", USER = "npc_reader";
    @TempDir Path temporary;

    @Test @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void selectedReadOnlyCatalogsRetainExactNumbersAndRejectRealDriverCoercionAndMissingTables() throws Exception {
        String mysql = System.getenv(ModernSettlementMysqlFixture.ENV);
        Assumptions.assumeTrue(mysql != null && !mysql.isBlank(), "explicit owned MySQL installation required");
        String secret = UUID.randomUUID().toString();
        try (var fixture = ModernSettlementMysqlFixture.open(Path.of(mysql))) {
            install(fixture, secret);
            try (var config = config(fixture.port(), secret)) {
                // Reuse the bounded physical transport only in this test, with a SELECT-only principal.
                // This does not connect the catalog reader to a production source DAO/settlement writer.
                var connections = new ModernSettlementConnections(config.sources().get(0).database(), 1000, 3000, 1);
                try {
                    var first = read(connections, 7, FIRST); var second = read(connections, 42, SECOND);
                    var npc = first.npcs().get(45262);
                    assertEquals("龜龍", npc.name()); assertEquals("$1498", npc.nameId()); assertTrue(npc.large());
                    assertEquals(280, npc.get(HP)); assertEquals(420, second.npcs().get(45262).get(HP));
                    assertTrue(npc.pendingCleanMelee().isEmpty()); assertEquals(127, first.growth().strength().size());
                    assertThrows(IllegalArgumentException.class, () -> first.require(second.npcs().get(45262).key()));
                    assertThrows(IllegalArgumentException.class, () -> first.require(new ModernNpcCatalog.Key(7, SECOND, 45262)));
                    // The NPC and growth are actual SELECT results; this player is explicitly an in-memory test fixture.
                    var player = ModernNpcMeleeTest.player(ModernNpcMeleeTest.armed());
                    var ordinary = ModernNpcMelee.againstNpc(player, first, ModernNpcMeleeTest.actor(first), 0, b -> b - 1);
                    assertEquals(15, ordinary.rawDamage()); assertEquals(12, ordinary.appliedDamage()); assertEquals(268, ordinary.remainingHp());
                    var reply = ModernNpcMelee.againstPlayer(first, ModernNpcMeleeTest.actor(first), player, 100, false,
                            new ModernNpcMeleeTest.Rolls(19, 0, 0, 99));
                    assertEquals(36, reply.rawDamage(), "STR20 uses this SQL catalog's growth row20, not the player's growth");

                    admin(fixture, "UPDATE `" + FIRST + "`.`npc` SET hp=5000000000,str=5000000000,ac=10");
                    var wideCombat = read(connections, 7, FIRST);
                    var widePlayer = ModernNpcMeleeTest.player(ModernPhysicalCombatTest.withVitals(7, ModernPhysicalCombatTest.source(7, 0, 30, List.of()), Long.MAX_VALUE, 10));
                    var wideReply = ModernNpcMelee.againstPlayer(wideCombat, ModernNpcMeleeTest.actor(wideCombat), widePlayer, Long.MAX_VALUE, false,
                            new ModernNpcMeleeTest.Rolls(19, 0, 0, 99));
                    assertEquals(4_000_000_127L, wideReply.rawDamage()); assertEquals(Long.MAX_VALUE - 4_000_000_127L, wideReply.remainingHp());
                    resetNpc(fixture); assertEquals(first, read(connections, 7, FIRST));

                    admin(fixture, "UPDATE `" + FIRST + "`.`npc` SET hp=9223372036854775807,mp=9223372036854775807,str=9223372036854775807,ac=-9223372036854775808");
                    var wide = read(connections, 7, FIRST).npcs().get(45262);
                    assertEquals(Long.MAX_VALUE, wide.get(HP)); assertEquals(Long.MAX_VALUE, wide.get(MP));
                    assertEquals(Long.MAX_VALUE, wide.get(STR)); assertEquals(Long.MIN_VALUE, wide.get(AC));
                    assertEquals(280, npc.get(HP), "the earlier immutable read is not a live/global cache");
                    assertEquals(127, first.growth().at(false, wide.get(STR)).id());
                    for (String change : List.of("hp=NULL", "hp=9223372036854775808", "mp=18446744073709551615", "hard=2")) {
                        resetNpc(fixture); admin(fixture, "UPDATE `" + FIRST + "`.`npc` SET " + change); rejected(connections, FIRST);
                    }
                    resetNpc(fixture);
                    admin(fixture, "ALTER TABLE `" + FIRST + "`.`npc` MODIFY hp DECIMAL(21,1)");
                    admin(fixture, "UPDATE `" + FIRST + "`.`npc` SET hp=1.1"); rejected(connections, FIRST);
                    admin(fixture, "ALTER TABLE `" + FIRST + "`.`npc` MODIFY hp VARCHAR(32)");
                    admin(fixture, "UPDATE `" + FIRST + "`.`npc` SET hp='280'"); rejected(connections, FIRST);
                    admin(fixture, "ALTER TABLE `" + FIRST + "`.`npc` MODIFY hp DOUBLE"); rejected(connections, FIRST);
                    admin(fixture, "ALTER TABLE `" + FIRST + "`.`npc` MODIFY hp DECIMAL(20,0)"); resetNpc(fixture);

                    for (String change : List.of("name=NULL", "name=REPEAT('龜',86)", "size='LARGE'", "impl=''",
                            "name=CONCAT('valid',CHAR(10),'invalid')")) {
                        admin(fixture, "UPDATE `" + FIRST + "`.`npc` SET " + change); rejected(connections, FIRST); resetNpc(fixture);
                    }
                    admin(fixture, "UPDATE `" + FIRST + "`.`npc` SET nameid=''");
                    assertEquals("", read(connections, 7, FIRST).npcs().get(45262).nameId()); resetNpc(fixture);

                    admin(fixture, "INSERT INTO `" + FIRST + "`.`mobskill` VALUES(45262,0)");
                    assertTrue(read(connections, 7, FIRST).npcs().get(45262).pendingCleanMelee().contains(ModernNpcCatalog.Pending.MOBSKILLS));
                    admin(fixture, "RENAME TABLE `" + FIRST + "`.`mobskill` TO `" + FIRST + "`.`mobskill_hidden`"); rejected(connections, FIRST);
                    admin(fixture, "RENAME TABLE `" + FIRST + "`.`mobskill_hidden` TO `" + FIRST + "`.`mobskill`");
                    admin(fixture, "DELETE FROM `" + FIRST + "`.`mobskill`");
                    admin(fixture, "DELETE FROM `" + FIRST + "`.`角色_力量成長` WHERE id=127"); rejected(connections, FIRST);
                    admin(fixture, "INSERT INTO `" + FIRST + "`.`角色_力量成長` VALUES(127,127,127)");
                    admin(fixture, "INSERT INTO `" + FIRST + "`.`角色_敏捷成長` VALUES(128,128,128)"); rejected(connections, FIRST);
                    admin(fixture, "DELETE FROM `" + FIRST + "`.`角色_敏捷成長` WHERE id=128");
                    admin(fixture, "ALTER TABLE `" + FIRST + "`.`npc` DROP PRIMARY KEY");
                    admin(fixture, "INSERT INTO `" + FIRST + "`.`npc` SELECT * FROM `" + FIRST + "`.`npc`"); rejected(connections, FIRST);
                    resetNpc(fixture);
                    admin(fixture, "UPDATE `" + FIRST + "`.`npc` SET npcid=45263"); rejected(connections, FIRST); resetNpc(fixture);
                    assertEquals(first, read(connections, 7, FIRST));

                    // Turn off the driver's readOnly flag to prove SQL grants themselves reject writes.
                    try (var c = connections.open(ModernSettlementMysqlFixture.deadline(5))) {
                        c.setReadOnly(false);
                        try (var s = c.prepareStatement("UPDATE `" + FIRST + "`.`npc` SET hp=1")) {
                            s.setQueryTimeout(2); var denied = assertThrows(SQLException.class, s::executeUpdate);
                            assertEquals(1142, denied.getErrorCode());
                        }
                    }
                    assertEquals(first, read(connections, 7, FIRST)); assertEquals(0, connections.activeCount());
                } finally { connections.close(); assertTrue(connections.awaitTermination(5000)); assertTrue(connections.isQuiescent()); }
            }
            assertArrayEquals(new long[]{0, 0, 0}, fixture.counts()); assertEquals(0, fixture.botCount());
            assertEquals(0, fixture.activeConnections()); assertFalse(fixture.forcedTermination()); fixture.successful();
        }
    }

    private ModernServerConfig config(int port, String secret) throws Exception {
        String text = "schema=1\nbind.port=0\ntls.pkcs12=" + temporary.resolve("unused.p12").toString().replace('\\', '/')
                + "\ntls.password.env=NPC_TLS\nsources=7\nsource.7.name=NPC測試\nsource.7.mysql.host=127.0.0.1\nsource.7.mysql.port=" + port
                + "\nsource.7.mysql.database=" + FIRST + "\nsource.7.mysql.username.env=NPC_USER\nsource.7.mysql.password.env=NPC_PASSWORD"
                + "\nsource.7.mysql.passwordScheme=plain\nsource.7.mysql.tls=disabled\n";
        return ModernServerConfig.parse(text.getBytes(StandardCharsets.UTF_8), key -> switch (key) {
            case "NPC_USER" -> USER; case "NPC_PASSWORD" -> secret; case "NPC_TLS" -> "unused-fixture-only"; default -> null;
        });
    }
    private static ModernNpcCatalog.Catalog read(ModernSettlementConnections connections, int source, String database) throws Exception {
        return ModernEquipmentSourceJdbcTest.scoped(() -> {
            var scope = ModernBackendScope.current();
            try (var c = connections.open(Math.min(scope.deadlineNanos(), ModernSettlementMysqlFixture.deadline(5)))) {
                c.setReadOnly(true);
                try (var lease = scope.register(c)) {
                    var result = ModernNpcCatalogJdbc.read(c, scope, source, database, List.of(45262));
                    assertFalse(c.isClosed(), "the caller still owns its connection"); return result;
                }
            }
        });
    }
    private static void rejected(ModernSettlementConnections connections, String database) {
        var failure = assertThrows(SQLException.class, () -> read(connections, 7, database));
        assertEquals("modern NPC catalog unavailable", failure.getMessage()); assertNull(failure.getCause());
        assertNull(failure.getNextException()); assertEquals(0, failure.getSuppressed().length); assertEquals(0, connections.activeCount());
    }
    static void install(ModernSettlementMysqlFixture fixture, String secret) throws Exception {
        try (var c = fixture.openConnection(ModernSettlementMysqlFixture.deadline(10))) {
            for (String database : List.of(FIRST, SECOND)) {
                execute(c, "CREATE DATABASE `" + database + "` CHARACTER SET utf8mb4");
                var ddl = new StringBuilder("CREATE TABLE `" + database + "`.`npc` (npcid INT PRIMARY KEY,name VARCHAR(1024),nameid VARCHAR(1024),impl VARCHAR(256),size VARCHAR(16)");
                for (var field : ModernNpcCatalog.Field.values()) ddl.append(",`").append(field.column()).append("` ")
                        .append(field == HP ? "DECIMAL(20,0)" : field == MP ? "BIGINT UNSIGNED" : field == HARD ? "TINYINT(1)" : "BIGINT");
                execute(c, ddl.append(") ENGINE=MyISAM DEFAULT CHARSET=utf8mb4").toString());
                execute(c, "CREATE TABLE `" + database + "`.`mobskill` (mobid INT,actNo INT,PRIMARY KEY(mobid,actNo)) ENGINE=MyISAM");
                for (String table : List.of("角色_力量成長", "角色_敏捷成長")) {
                    execute(c, "CREATE TABLE `" + database + "`.`" + table + "` (id INT PRIMARY KEY,`命中` BIGINT,`傷害` BIGINT) ENGINE=MyISAM");
                    try (var s = c.prepareStatement("INSERT INTO `" + database + "`.`" + table + "` VALUES(?,?,?)")) {
                        s.setQueryTimeout(2); for (int id = 1; id <= 127; id++) { s.setInt(1, id); s.setInt(2, id); s.setInt(3, id); s.addBatch(); } s.executeBatch();
                    }
                }
                seed(c, database);
            }
            execute(c, "UPDATE `" + SECOND + "`.`npc` SET hp=420");
            try (var s = c.prepareStatement("CREATE USER '" + USER + "'@'127.0.0.1' IDENTIFIED BY ?")) { s.setString(1, secret); s.setQueryTimeout(2); s.executeUpdate(); }
            for (String database : List.of(FIRST, SECOND)) for (String table : List.of("npc", "mobskill", "角色_力量成長", "角色_敏捷成長"))
                execute(c, "GRANT SELECT ON `" + database + "`.`" + table + "` TO '" + USER + "'@'127.0.0.1'");
        }
    }
    private static void seed(Connection c, String database) throws Exception {
        execute(c, "DELETE FROM `" + database + "`.`npc`");
        var columns = new StringBuilder("npcid,name,nameid,impl,size"); var placeholders = new StringBuilder("?,?,?,?,?");
        for (var field : ModernNpcCatalog.Field.values()) { columns.append(",`").append(field.column()).append('`'); placeholders.append(",?"); }
        try (var s = c.prepareStatement("INSERT INTO `" + database + "`.`npc` (" + columns + ") VALUES(" + placeholders + ")")) {
            s.setInt(1, 45262); s.setString(2, "龜龍"); s.setString(3, "$1498"); s.setString(4, "L1Monster"); s.setString(5, "large");
            int column = 6; for (var field : ModernNpcCatalog.Field.values()) s.setLong(column++, ModernNpcCatalogJdbcTest.numbers().get(field));
            s.setQueryTimeout(2); assertEquals(1, s.executeUpdate());
        }
    }
    private static void resetNpc(ModernSettlementMysqlFixture fixture) throws Exception {
        try (var c = fixture.openConnection(ModernSettlementMysqlFixture.deadline(5))) { seed(c, FIRST); }
    }
    private static void admin(ModernSettlementMysqlFixture fixture, String sql) throws Exception {
        try (var c = fixture.openConnection(ModernSettlementMysqlFixture.deadline(5))) { execute(c, sql); }
    }
    private static void execute(Connection c, String sql) throws SQLException {
        try (var s = c.prepareStatement(sql)) { s.setQueryTimeout(2); s.execute(); }
    }
}

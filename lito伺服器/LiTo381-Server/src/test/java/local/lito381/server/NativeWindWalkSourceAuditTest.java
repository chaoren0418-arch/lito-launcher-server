package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-source proof for native Elf skill150 Wind Walk. */
final class NativeWindWalkSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/datatables/user/CharBuffTable.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/ActionIntervalCalculator.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillStop.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                Path.of("../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        LocalResources.assumeLitoBin(
                Path.of("D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin"));
        LocalResources.assumeIdaClient(
                Path.of("D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PRIMARY_SQL = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY_SQL = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void bothDatabasesPinExactWindWalkRows() throws Exception {
        String primary = read(PRIMARY_SQL);
        String secondary = read(SECONDARY_SQL);
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (150, 0,"
                + " '風之疾走', 19, 5, 15, 0, 0, 0, 0, 960, 'none', 0,"
                + " 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 32, '', 19, 2247,"
                + " 0, 0, 0, 0);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (150,"
                + " '風之疾走', 19, 5, 15, 0, 0, 0, 0, 960, 'none', 0,"
                + " 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 32, '', 19, 2247,"
                + " 0, 0, 0, 0);"));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (150, '風之疾走', NULL, NULL);"));
        }
    }

    @Test void retainedExecutorPinsModeFourExclusionStopAndMovementOnlyCadence()
            throws Exception {
        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("{ HOLY_WALK, MOVING_ACCELERATION, WIND_WALK,"
                + " STATUS_BRAVE,"));
        assertTrue(use.contains("this._skillId == WIND_WALK"));
        assertTrue(use.contains("cha.setBraveSpeed(4);"));
        assertTrue(use.contains(
                "new S_SkillBrave(pc.getId(), 4, this._getBuffIconDuration)"));
        assertTrue(use.contains("new S_SkillBrave(cha.getId(), 4, 0)"));

        String stop = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillStop.java"));
        assertTrue(stop.contains("case WIND_WALK:"));
        assertTrue(stop.contains("new S_SkillBrave(pc.getId(), 0, 0)"));
        String buffs = read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"));
        assertTrue(buffs.contains("WIND_WALK, // 風之疾走"));

        String interval = read(RETAINED.resolve(
                "src/com/lineage/server/model/ActionIntervalCalculator.java"));
        assertTrue(interval.contains("actor.hasSkillEffect(WIND_WALK)"));
        assertTrue(interval.contains("type == ActionType.MOVE"
                + " && state.fastMovable()"));
        assertTrue(interval.contains("interval = multiply(interval, 0.665);"));
    }

    @Test void selectedLiToPinsCase149SelectorOnlyProducer() throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String disassembly = read(DISASSEMBLY).replace("\r\n", "\n");
        assertTrue(disassembly.contains("loc_4CBBD4:"));
        assertTrue(disassembly.contains("cases 1,2,12,13,30,31,48,51,53,71,"
                + "77,81,96-98,100,101,103-105,108-110,113,114,116,118,119,"
                + "128,129,133,136,137,145,146,149,153-155,158,160,161,163,"
                + "168,170"));
        assertTrue(disassembly.contains("byte_4CBD00 db      0,     1,     1,     2"));
        assertTrue(disassembly.contains("db    1Ch,     1,     1,     4\n"
                + "db      4,     1,     4,     2"));
        assertTrue(disassembly.contains("push    offset off_5028CC"));
        assertTrue(disassembly.contains("push    14h"));
        assertTrue(disassembly.contains("off_5028CC dd offset byte_636363"));
    }

    @Test void implementationRoutesBattleAndWorldAndPersistsTheSourceLease()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertTrue(session.contains(
                "BattleSpellCatalog.movementBuffSpell(selector.skillId())"));
        assertTrue(session.contains("phase != Phase.BATTLE && phase != Phase.WORLD"));
        assertTrue(session.contains("new GameStateStore.TimedMovementBuff("));
        assertTrue(session.contains("classicWorldMembers(originMapId)"));
        assertTrue(session.contains("movement-multiplier="));
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(LocalResources.assumeExternalFile(path)), "missing source evidence: " + path);
        return LocalResources.readString(path, StandardCharsets.UTF_8);
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var stream = LocalResources.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            for (int count; (count = stream.read(buffer)) >= 0;) {
                if (count > 0) digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }
}

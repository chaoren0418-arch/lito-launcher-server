package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-source proof for the retained PK punishment and map666 lifecycle. */
final class NativeClassicWorldHellSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/maps/666.txt"),
                Path.of("../1-伺服器/src/com/lineage/server/clientpackets/C_LoginToServer.java"),
                Path.of("../1-伺服器/src/com/lineage/server/clientpackets/C_Restart.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/Instance/L1PcInstance.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/L1TownLocation.java"),
                Path.of("../1-伺服器/src/com/lineage/server/timecontroller/pc/PcHellTimer.java"));
        LocalResources.assumeLitoBin(
                Path.of("D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin"));
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PC = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/L1PcInstance.java");
    private static final Path TIMER = RETAINED.resolve(
            "src/com/lineage/server/timecontroller/pc/PcHellTimer.java");
    private static final Path RESTART = RETAINED.resolve(
            "src/com/lineage/server/clientpackets/C_Restart.java");
    private static final Path LOGIN = RETAINED.resolve(
            "src/com/lineage/server/clientpackets/C_LoginToServer.java");
    private static final Path TOWN = RETAINED.resolve(
            "src/com/lineage/server/model/L1TownLocation.java");
    private static final Path DATABASE = RETAINED.resolve("db/380_db.sql");
    private static final Path MAP = RETAINED.resolve("maps/666.txt");

    @Test
    void selectedInputsRemainByteExact() throws Exception {
        assertHash(ClassicWorldHell.PC_SOURCE_SHA256, PC);
        assertHash(ClassicWorldHell.TIMER_SOURCE_SHA256, TIMER);
        assertHash(ClassicWorldHell.RESTART_SOURCE_SHA256, RESTART);
        assertHash(ClassicWorldHell.LOGIN_SOURCE_SHA256, LOGIN);
        assertHash(ClassicWorldHell.TOWN_SOURCE_SHA256, TOWN);
        assertHash(ClassicWorldHell.DATABASE_SOURCE_SHA256, DATABASE);
        assertHash(ClassicWorldHell.MAP_SOURCE_SHA256, MAP);
        assertHash("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                Path.of("D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin"));
    }

    @Test
    void retainedPkThresholdEntryMessagesAndReturnOrderRemainExact()
            throws Exception {
        String pc = LocalResources.readString(PC);
        int warning = pc.indexOf("fightPc.get_PKcount() >= 5");
        int belowThreshold = pc.indexOf("fightPc.get_PKcount() < 100", warning);
        int message551 = pc.indexOf("new S_BlueMessage(551", belowThreshold);
        int threshold = pc.indexOf("fightPc.get_PKcount() >= 100", message551);
        int begin = pc.indexOf("fightPc.beginHell(true);", threshold);
        assertTrue(warning >= 0 && belowThreshold > warning
                && message551 > belowThreshold && threshold > message551
                && begin > threshold);

        String lifecycle = pc.substring(
                pc.indexOf("public void beginHell(final boolean isFirst)"),
                pc.indexOf("public void setPoisonEffect", pc.indexOf(
                        "public void endHell()")));
        assertTrue(lifecycle.contains("this.getMapId() != 666"));
        assertTrue(lifecycle.contains("final int locx = 32701;"));
        assertTrue(lifecycle.contains("final int locy = 32777;"));
        assertTrue(lifecycle.contains("L1Teleport.teleport(this, locx, locy, mapid, 5, false)"));
        assertTrue(lifecycle.contains("this.get_PKcount() <= 10"));
        assertTrue(lifecycle.contains("this.setHellTime(300);"));
        assertTrue(lifecycle.contains(
                "this.setHellTime(300 * (this.get_PKcount() - 10) + 300);"));
        assertTrue(lifecycle.contains("new S_BlueMessage(552"));
        assertTrue(lifecycle.contains("this.getHellTime() / 60"));
        assertTrue(lifecycle.contains("new S_BlueMessage(637"));
        assertTrue(lifecycle.contains(
                "L1TownLocation.getGetBackLoc(L1TownLocation.TOWNID_ORCISH_FOREST)"));
        assertTrue(lifecycle.contains(
                "L1Teleport.teleport(this, loc[0], loc[1], (short) loc[2], 5, true)"));
    }

    @Test
    void retainedTimerLoginRestartAndDatabaseContractsRemainExact()
            throws Exception {
        String timer = LocalResources.readString(TIMER);
        assertTrue(timer.contains("final int timeMillis = 1100;"));
        assertTrue(timer.contains("World.get().getAllPlayers()"));
        assertTrue(timer.contains("int time = tgpc.getHellTime();"));
        assertTrue(timer.contains("time--;"));
        assertTrue(timer.contains("tgpc.setHellTime(0);"));
        assertTrue(timer.contains("tgpc.endHell();"));

        String restart = LocalResources.readString(RESTART);
        assertTrue(restart.contains("if (pc.getHellTime() > 0)"));
        assertTrue(restart.contains("loc[0] = 32701;"));
        assertTrue(restart.contains("loc[1] = 32777;"));
        assertTrue(restart.contains("loc[2] = 666;"));
        String login = LocalResources.readString(LOGIN);
        assertTrue(login.contains("if (pc.getHellTime() > 0)"));
        assertTrue(login.contains("pc.beginHell(false);"));

        String town = LocalResources.readString(TOWN);
        assertTrue(town.contains("new Point(32750, 32435), new Point(32745, 32447)"));
        assertTrue(town.contains("new Point(32738, 32452), new Point(32741, 32436)"));
        assertTrue(town.contains("new Point(32749, 32446)"));

        String database = LocalResources.readString(DATABASE);
        assertTrue(database.contains("INSERT INTO `mapids` VALUES (666, '地獄',"
                + " 32640, 32831, 32704, 32895, 1, 1, 0, 0, 1, 1, 1, 1,"
                + " 1, 1, 1, 1, 1, 1, 0, 0, -1);"));
        long spawnRows = database.lines()
                .filter(line -> line.startsWith("INSERT INTO `spawnlist` VALUES ("))
                .filter(line -> line.contains(", 666, 1, 0, 0, 0);"))
                .count();
        assertEquals(13, spawnRows);
        assertTrue(database.contains("INSERT INTO `spawnlist` VALUES (2898, '地獄犬', 50, 45241"));
        assertTrue(database.contains("INSERT INTO `spawnlist` VALUES (2910, '思克巴女皇', 20, 45451"));
    }

    @Test
    void independentRuntimeKeepsEncryptedMessagePortalAndRestartRoutes()
            throws Exception {
        String session = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertTrue(session.contains("Protocol.messageCode(551"));
        assertTrue(session.contains("Protocol.messageCode(552"));
        assertTrue(session.contains("Protocol.messageCode(637"));
        assertTrue(session.contains("queueClassicWorldHellPortal(ClassicWorldHell.entry()"));
        assertTrue(session.contains("state.elapseHell("));
        assertTrue(session.contains("ClassicWorldHell.returnDestination(index)"));
        assertTrue(session.contains("boolean hellRestart = hellRemainingTicks > 0;"));
    }

    private static void assertHash(String expected, Path path) throws Exception {
        String actual = HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(LocalResources.readAllBytes(path)));
        assertEquals(expected, actual, path.toString());
    }
}

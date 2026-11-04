package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks retained HIDDEN_STATUS_ICE semantics and its source-profile boundary. */
final class NativeClassicWorldIceMonsterSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
    }

    private static final Path RETAINED = Path.of("../1-伺服器");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)");
    private static final Set<Integer> ICE_MAPS = Set.of(
            72, 73, 74, 4472, 4473, 4474);

    @Test
    void retainedIceLifecycleStaysExact() throws Exception {
        Path source = RETAINED.resolve(
                "src/com/lineage/server/model/Instance/L1MonsterInstance.java");
        String monster = Files.readString(source, StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
        assertTrue(monster.contains(
                "case 46125:// 高侖鋼鐵怪\n"
                + "\t\tcase 46126:// 萊肯\n"
                + "\t\tcase 46127:// 歐熊\n"
                + "\t\tcase 46128:// 冰原老虎"));
        assertTrue(monster.contains(
                "this.setHiddenStatus(L1NpcInstance.HIDDEN_STATUS_ICE);"));
        assertTrue(monster.contains("this.setStatus(4);"));

        String npc = Files.readString(RETAINED.resolve(
                "src/com/lineage/server/model/Instance/L1NpcInstance.java"),
                StandardCharsets.UTF_8).replace("\r\n", "\n");
        assertTrue(npc.contains(
                "public static final int HIDDEN_STATUS_ICE = 3;"));
        assertTrue(npc.contains(
                "case HIDDEN_STATUS_ICE:\n\t\t\tif (this.getCurrentHp() < this.getMaxHp())"));
        assertTrue(npc.contains(
                "case HIDDEN_STATUS_SINK:\n\t\tcase HIDDEN_STATUS_ICE:"));
        assertTrue(npc.contains("ActionCodes.ACTION_Appear"));

        String attack = Files.readString(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_Attack.java"),
                StandardCharsets.UTF_8);
        assertTrue(attack.contains(
                "hiddenStatus == L1NpcInstance.HIDDEN_STATUS_SINK || hiddenStatus == L1NpcInstance.HIDDEN_STATUS_FLY"));
        assertFalse(attack.contains(
                "hiddenStatus == L1NpcInstance.HIDDEN_STATUS_ICE"));
    }

    @Test
    void selectedAndSecondaryEntrySourcesMustNotBeMerged() throws Exception {
        String selected = Files.readString(
                RETAINED.resolve("db/380_db.sql"), StandardCharsets.UTF_8);
        String secondary = Files.readString(RETAINED.resolve(
                "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"),
                StandardCharsets.UTF_8);

        for (int npcId : new int[] {46125, 46126, 46127, 46128}) {
            assertTrue(selected.contains(
                    "INSERT INTO `npc` VALUES (" + npcId + ","));
        }
        assertTrue(selected.contains(
                "INSERT INTO `spawnlist` VALUES (1407, '高侖鋼鐵怪', 8, 46125"));
        assertTrue(selected.contains(
                "INSERT INTO `spawnlist` VALUES (1418, '高侖鋼鐵怪', 8, 46125"));
        assertTrue(selected.contains(
                "INSERT INTO `spawnlist` VALUES (400771, '冰原老虎', 1, 46128"));
        assertTrue(selected.contains(
                "INSERT INTO `spawnlist` VALUES (400796, '高侖冰石人', 6, 46125"));

        assertTrue(selected.contains(
                "INSERT INTO `npc_action_teleport` VALUES (1141, '70751', 'a', 32743, 32833, 72"));
        assertFalse(selected.contains(
                "INSERT INTO `spawnlist_npc` VALUES (757, '布朗德', 1, 70751"));
        assertTrue(secondary.contains(
                "INSERT INTO `npc_action_teleport` VALUES (1141, '70751', 'a', 32743, 32833, 72"));
        assertTrue(secondary.contains(
                "INSERT INTO `spawnlist_npc` VALUES (757, '布朗德', 1, 70751"));
        assertTrue(selected.contains(
                "INSERT INTO `dungeon` VALUES (34213, 32183, 4, 32741, 32833, 4472"));
    }

    @Test
    void selectedClientAndProductionSchedulerPreserveTheAuthorityGate() {
        for (int mapId : new int[] {72, 73, 74}) {
            assertTrue(Files.isDirectory(
                    SELECTED_CLIENT.resolve("map/" + mapId)));
        }
        for (int mapId : new int[] {4472, 4473, 4474}) {
            assertFalse(Files.isDirectory(
                    SELECTED_CLIENT.resolve("map/" + mapId)));
        }
        for (int mapId : ICE_MAPS) {
            assertTrue(Files.isRegularFile(
                    RETAINED.resolve("maps/" + mapId + ".txt")));
            assertFalse(ClassicWorldMapCatalog.supportedMapIds().contains(mapId));
            assertTrue(ClassicWorldMonsterCatalog.definitions(mapId).isEmpty());
        }
    }

    @Test
    void serverImplementsIceWithoutActivatingCrossVersionMaps()
            throws Exception {
        String population = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClassicWorldMonsterPopulation.java"),
                StandardCharsets.UTF_8);
        assertTrue(population.contains("HIDDEN_STATUS_ICE = 3"));
        assertTrue(population.contains(
                "case 46125, 46126, 46127, 46128 -> true"));
        assertTrue(population.contains("hiddenStatus = HIDDEN_STATUS_ICE"));
        assertTrue(population.contains("actorStatus = 4"));
        assertTrue(population.contains("revealDamagedIceInScreen"));
        assertTrue(population.contains("actor.currentHp >= actor.definition.template().hp()"));

        String hub = Files.readString(Path.of(
                "src/main/java/local/lito381/server/GameHub.java"),
                StandardCharsets.UTF_8);
        assertTrue(hub.contains("population.revealDamagedIceInScreen("));
        assertTrue(hub.contains(
                "!= ClassicWorldMonsterPopulation.HIDDEN_STATUS_ICE"));

        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        assertTrue(session.contains("hidden-ice="));
        assertTrue(session.contains("BattleActionCodes.HIDE"));
    }
}

package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-source proof for the selected live L1Guard native flow. */
final class NativeClassicWorldGuardSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/model/Instance/L1GuardInstance.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/Instance/L1PcInstance.java"),
                Path.of("../1-伺服器/其他src參考用/381n8src/com/lineage/server/model/Instance/L1GuardInstance.java"));
        LocalResources.assumeLitoBin(
                Path.of("D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin"));
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path CURRENT = RETAINED.resolve("src/com/lineage/server");
    private static final Path SECONDARY = RETAINED.resolve(
            "其他src參考用/381n8src/com/lineage/server");
    private static final Path DATABASE = RETAINED.resolve("db/380_db.sql");
    private static final Path CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");

    @Test
    void retainedSourcesSelectedDatabaseAndClientStayPinned() throws Exception {
        assertEquals(ClassicWorldGuardCatalog.SELECTED_DATABASE_SHA256,
                sha256(DATABASE));
        assertEquals(ClassicWorldGuardCatalog.RETAINED_GUARD_SOURCE_SHA256,
                sha256(CURRENT.resolve("model/Instance/L1GuardInstance.java")));
        assertEquals(ClassicWorldGuardCatalog.SECONDARY_GUARD_SOURCE_SHA256,
                sha256(SECONDARY.resolve("model/Instance/L1GuardInstance.java")));
        assertEquals(ClassicWorldGuardCatalog.RETAINED_PC_DEATH_SOURCE_SHA256,
                sha256(CURRENT.resolve("model/Instance/L1PcInstance.java")));
        assertEquals(
                "CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(CLIENT));
    }

    @Test
    void retainedGuardPinsWantedTargetHomeCombatTalkAndDeathSemantics()
            throws Exception {
        String guard = readLegacy(
                CURRENT.resolve("model/Instance/L1GuardInstance.java"));
        String player = readLegacy(
                CURRENT.resolve("model/Instance/L1PcInstance.java"));
        ordered(guard,
                "World.get().getVisiblePlayer(npc)",
                "pc.isGm()",
                "npc.get_showId() != pc.get_showId()",
                "!pc.isInvisble() || npc.getNpcTemplate().is_agrocoi()",
                "pc.isWanted()");
        ordered(guard,
                "getTileLineDistance(new Point(getHomeX(), getHomeY())) > 0",
                "_npcMove.moveDirection(getHomeX(), getHomeY())",
                "_npcMove.setDirectionMove(dir)",
                "teleport(getHomeX(), getHomeY(), 1)");
        ordered(guard,
                "attack.calcDamage();",
                "attack.action();",
                "attack.commit();");
        assertTrue(guard.contains("this.removeSkillEffect(FOG_OF_SLEEPING)"));
        assertTrue(guard.contains("NPCTalkDataTable.get().getTemplate"));
        assertTrue(guard.contains("player.getLawful() < -1000"));
        assertFalse(guard.contains("CalcExp"));

        ordered(player,
                "lastAttacker instanceof L1GuardInstance",
                "set_PKcount(L1PcInstance.this.get_PKcount() - 1)",
                "setLastPk(null)");
        ordered(player,
                "fightPc.getLawful() < 30000",
                "fightPc.set_PKcount(fightPc.get_PKcount() + 1)",
                "fightPc.setLastPk()",
                "Math.pow(fightPc.getLevel(), 2)",
                "Math.pow(fightPc.getLevel(), 3)",
                "fightPc.setLawful(lawful)",
                "new S_Lawful(fightPc)");
        assertTrue(player.contains("1 * 3600 * 1000"));
    }

    @Test
    void selectedDatabaseSplitsLiveStaticAndMissingMapGuardRowsExactly()
            throws Exception {
        String sql = readLegacy(DATABASE);
        Map<Integer, NpcRow> guards = npcRows(sql);
        List<SpawnRow> spawns = spawnRows(sql).stream()
                .filter(row -> guards.containsKey(row.npcId()))
                .toList();
        Set<Integer> supported =
                Set.copyOf(ClassicWorldMapCatalog.supportedMapIds());

        List<SpawnRow> live = spawns.stream()
                .filter(row -> supported.contains(row.mapId()))
                .filter(row -> guards.get(row.npcId()).hp() > 0)
                .toList();
        List<SpawnRow> staticZero = spawns.stream()
                .filter(row -> supported.contains(row.mapId()))
                .filter(row -> guards.get(row.npcId()).hp() == 0)
                .toList();
        List<SpawnRow> missingMap = spawns.stream()
                .filter(row -> row.mapId() == 2005)
                .filter(row -> guards.get(row.npcId()).hp() > 0)
                .toList();

        assertEquals(ClassicWorldGuardCatalog.POPULATION, live.size());
        assertEquals(ClassicWorldGuardCatalog.STATIC_ZERO_HP_POPULATION,
                staticZero.size());
        assertEquals(ClassicWorldGuardCatalog.MISSING_MAP_2005_POPULATION,
                missingMap.size());
        Map<Integer, Integer> byMap = new LinkedHashMap<>();
        for (SpawnRow row : live) {
            byMap.merge(row.mapId(), 1, Math::addExact);
        }
        assertEquals(ClassicWorldGuardCatalog.POPULATION_BY_MAP, byMap);
        assertEquals(44, live.stream().map(SpawnRow::npcId).distinct().count());
        assertEquals(Map.of(60501, 4L, 60502, 4L),
                missingMap.stream().collect(java.util.stream.Collectors.groupingBy(
                        SpawnRow::npcId,
                        java.util.stream.Collectors.counting())));
    }

    @Test
    void serverPacketWritersKeepSelectedLiToObjectAttackAndTeleportContracts()
            throws Exception {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation(21L);
        ClassicWorldNpcCatalog.StaticNpc source =
                ClassicWorldGuardCatalog.all().stream()
                        .filter(npc -> npc.npcId() == 60501)
                        .findFirst().orElseThrow();
        ClassicWorldMonsterPopulation.Snapshot guard =
                population.actor(source.mapId(), source.objectId());
        Protocol.Reader object =
                new Protocol.Reader(Protocol.classicWorldMonster(guard));
        assertEquals(Protocol.S_PUT_OBJECT, object.c());
        assertEquals(guard.x(), object.h());
        assertEquals(guard.y(), object.h());
        assertEquals(guard.objectId(), object.d());
        assertEquals(guard.template().gfxId(), object.h());

        assertEquals(Protocol.S_EFFECT,
                new Protocol.Reader(
                        Protocol.effectObject(guard.objectId(), 169)).c());
        assertEquals(Protocol.S_REMOVE_OBJECT,
                new Protocol.Reader(
                        Protocol.removeObject(guard.objectId())).c());
        String session = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertTrue(session.contains(
                "Protocol.effectObject(before.objectId(), 169)"));
        assertTrue(session.contains(
                "ClassicWorldGuardCatalog.isGuard(defeated)) return"));
        assertTrue(session.contains(
                "state.clearGeneralPkAfterGuardDeath(stateCharacter())"));
    }

    private static Map<Integer, NpcRow> npcRows(String sql) {
        Map<Integer, NpcRow> result = new LinkedHashMap<>();
        for (String line : sql.lines().toList()) {
            if (!line.startsWith("INSERT INTO `npc` VALUES (")) continue;
            List<String> values = values(line);
            try {
                int id = Integer.parseInt(values.get(0));
                String implementation = unquote(values.get(5));
                if (implementation.equals("L1Guard")) {
                    result.put(id, new NpcRow(
                            id, Integer.parseInt(values.get(8))));
                }
            } catch (RuntimeException ignored) {
                // Non-template or malformed retained rows cannot be guards.
            }
        }
        return result;
    }

    private static List<SpawnRow> spawnRows(String sql) {
        List<SpawnRow> result = new ArrayList<>();
        for (String line : sql.lines().toList()) {
            if (!line.startsWith(
                    "INSERT INTO `spawnlist_npc` VALUES (")) continue;
            List<String> values = values(line);
            if (values.size() != 12) continue;
            result.add(new SpawnRow(
                    Integer.parseInt(values.get(0)),
                    Integer.parseInt(values.get(3)),
                    Integer.parseInt(values.get(10))));
        }
        return result;
    }

    private static List<String> values(String line) {
        int start = line.indexOf('(');
        int end = line.lastIndexOf(");");
        return List.of(line.substring(start + 1, end).split(", ", -1));
    }

    private static String unquote(String value) {
        return value.length() >= 2 && value.startsWith("'")
                && value.endsWith("'")
                ? value.substring(1, value.length() - 1) : value;
    }

    private static void ordered(String source, String... fragments) {
        int position = -1;
        for (String fragment : fragments) {
            position = source.indexOf(fragment, position + 1);
            assertTrue(position >= 0,
                    "missing/out-of-order source fragment: " + fragment);
        }
    }

    private static String readLegacy(Path path) throws Exception {
        byte[] bytes = LocalResources.readAllBytes(path);
        for (Charset charset : List.of(StandardCharsets.UTF_8,
                Charset.forName("Big5"), StandardCharsets.UTF_16)) {
            try {
                return charset.newDecoder()
                        .decode(ByteBuffer.wrap(bytes)).toString();
            } catch (CharacterCodingException ignored) {
                // Try the next retained encoding.
            }
        }
        throw new IllegalArgumentException("unsupported retained source " + path);
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(LocalResources.readAllBytes(path)));
    }

    private record NpcRow(int npcId, int hp) {}
    private record SpawnRow(int spawnId, int npcId, int mapId) {}
}

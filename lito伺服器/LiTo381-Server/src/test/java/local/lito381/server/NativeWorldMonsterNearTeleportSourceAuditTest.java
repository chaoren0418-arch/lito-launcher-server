package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Hash-pinned proof for retained ordinary-monster pursuit teleport. */
final class NativeWorldMonsterNearTeleportSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path NPC_INSTANCE = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/L1NpcInstance.java");
    private static final Path NPC_TABLE = RETAINED.resolve(
            "src/com/lineage/server/datatables/NpcTable.java");
    private static final Path DATABASE = RETAINED.resolve("db/380_db.sql");
    private static final Path RESOURCE = Path.of(
            "src/main/resources/world/classic-world-monster-teleport.lmt.gz");

    @Test
    void selectedDatabaseLoaderAndGeneratedResourceRemainPinned()
            throws Exception {
        assertEquals(ClassicWorldMonsterTeleportCatalog.RETAINED_NPC_INSTANCE_SHA256,
                sha256(NPC_INSTANCE));
        assertEquals(ClassicWorldMonsterTeleportCatalog.RETAINED_NPC_TABLE_SHA256,
                sha256(NPC_TABLE));
        assertEquals(ClassicWorldMonsterTeleportCatalog.SELECTED_DATABASE_SHA256,
                sha256(DATABASE));
        assertEquals(ClassicWorldMonsterTeleportCatalog.RESOURCE_SHA256,
                sha256(RESOURCE));
        assertTrue(readLegacy(NPC_TABLE)
                .contains("npc.set_teleport(rs.getBoolean(\"teleport\"))"));
    }

    @Test
    void retainedAttackBranchOrderThresholdsAndLifetimeFlagRemainPinned()
            throws Exception {
        String source = readLegacy(NPC_INSTANCE);
        assertEquals(1, occurrences(source, "private boolean firstFound = true;"));
        String attack = method(source,
                "private void attack(L1Character target)",
                "private void ascape(L1Location location)");
        ordered(attack,
                "isAttackPosition(target.getX(), target.getY(), get_ranged())",
                "glanceCheck(target.getX(), target.getY())",
                "_mobSkill.skillUse(target, true)",
                "getPassispeed() > 0",
                "getLocation().getTileDistance(target.getLocation())",
                "firstFound == true",
                "getNpcTemplate().is_teleport()",
                "distance > 3",
                "distance < DISTANCE",
                "nearTeleport(target.getX(), target.getY())",
                "firstFound = false",
                "20 > _random.nextInt(100)",
                "getCurrentMp() >= 10",
                "distance > 6",
                "distance < DISTANCE",
                "nearTeleport(target.getX(), target.getY())",
                "_npcMove.moveDirection(goalX, goalY)");
        // No retained respawn/reset assignment may silently turn the
        // successful object-lifetime flag back on.
        assertEquals(1, occurrences(source, "firstFound = true"));
    }

    @Test
    void retainedLandingSearchFacesTargetAndDebitsUpToTenMp()
            throws Exception {
        String source = readLegacy(NPC_INSTANCE);
        String teleport = method(source,
                "public boolean nearTeleport(int nx, int ny)",
                "public void teleport(final int nx, final int ny, final int dir)");
        ordered(teleport,
                "_random.nextInt(8)",
                "for (int i = 0; i < 8; i++)",
                "baseX + HEADING_TABLE_X[dir]",
                "baseY + HEADING_TABLE_Y[dir]",
                "getMap().isPassable(landX, landY, null)",
                "faceDir = dir + 4",
                "teleportInternal(landX, landY, faceDir, false)",
                "debitCurrentMpUpTo(10)");
    }

    @Test
    void productionSchedulerKeepsTeleportBetweenAttackAndPathMovement()
            throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/local/lito381/server/GameHub.java"));
        String tick = method(source,
                "private void classicWorldMonsterTick(",
                "private ClassicMonsterGroundPickup takeClassicWorldMonsterGroundItem(");
        ordered(tick,
                "population.attack(",
                "population.nearTeleport(",
                "serverClassicWorldMonsterNearTeleport(nearTeleport)",
                "population.moveToward(");
    }

    private static int occurrences(String source, String fragment) {
        int count = 0;
        for (int index = 0; (index = source.indexOf(fragment, index)) >= 0;
             index += fragment.length()) count++;
        return count;
    }

    private static void ordered(String source, String... fragments) {
        int position = -1;
        for (String fragment : fragments) {
            position = source.indexOf(fragment, position + 1);
            assertTrue(position >= 0,
                    "missing/out-of-order source fragment: " + fragment);
        }
    }

    private static String method(String source, String startText, String endText) {
        int start = source.indexOf(startText);
        int end = source.indexOf(endText, start + startText.length());
        assertTrue(start >= 0 && end > start,
                "missing method boundary " + startText);
        return source.substring(start, end);
    }

    private static String readLegacy(Path path) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        for (Charset charset : List.of(StandardCharsets.UTF_8,
                Charset.forName("Big5"), StandardCharsets.UTF_16)) {
            try {
                return charset.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
            } catch (CharacterCodingException ignored) {
                // Try the next retained source encoding.
            }
        }
        throw new IllegalArgumentException("unsupported retained source " + path);
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(Files.readAllBytes(path)));
    }
}

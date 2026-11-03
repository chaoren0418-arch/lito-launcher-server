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
import org.junit.jupiter.api.Test;

/** Hash-pinned source proof for ordinary L1NpcInstance.noTarget movement. */
final class NativeWorldMonsterIdleSourceAuditTest {
    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path NPC = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/L1NpcInstance.java");
    private static final Path MOVE = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/NpcMove.java");
    private static final Path AI = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/NpcAI.java");

    @Test
    void retainedIdleSourcesRemainHashPinned() throws Exception {
        assertEquals("72E8DC365DAAC1B27D3F2C92672FCC77FF512825C4B6EAF9C96A6A87EA80EAD2",
                sha256(NPC));
        assertEquals("2B01ACB7F7BDE1BE8019901966F3A103EA004E8E6C1D491E47F914DF4E7743B7",
                sha256(MOVE));
        assertTrue(Files.size(AI) > 0);
    }

    @Test
    void noTargetRetainsObserverGateRandomRunPauseRestAndGroupFollow()
            throws Exception {
        String source = readLegacy(NPC);
        String noTarget = method(source, "public boolean noTarget()",
                "public void onFinalAction");
        ordered(noTarget,
                "getVisiblePlayer(this, DISTANCE).size() <= 0",
                "getHomeX() != 0", "teleport(getHomeX(), getHomeY()",
                "getPassispeed() > 0", "!isRest()",
                "_randomMoveDistance == 0",
                "_random.nextInt(3) + 1",
                "_random.nextInt(40)", "_randomMoveDirection < 8",
                "moveDirection(getHomeX(), getHomeY())",
                "_randomMoveDistance--", "checkObject(_randomMoveDirection)",
                "setDirectionMove(dir)", "mobGroupInfo.getLeader()",
                "getTileLineDistance(leader.getLocation()) > 2");
    }

    @Test
    void moveCommitBroadcastsBeforeMovementDistanceHomeTeleport()
            throws Exception {
        String source = readLegacy(MOVE);
        String setMove = method(source,
                "public void setDirectionMove(final int dir)",
                "private void clearPathUnlocked");
        ordered(setMove, "setXY(targetX, targetY)",
                "broadcastPacketAll(new S_MoveCharPacket",
                "getMovementDistance() > 0", "getLineDistance",
                "getHomeX(), _npc.getHomeY()",
                "teleport(_npc.getHomeX(), _npc.getHomeY()",
                "_npc.getHeading()");
        String ai = readLegacy(AI);
        ordered(ai, "setSleepTime(300)", "noTarget()");
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

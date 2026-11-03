package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** Hash and ordering proof for retained NpcMove path-failure lifecycle. */
final class NativeWorldNpcPathFailureSourceAuditTest {
    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path NPC = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/L1NpcInstance.java");
    private static final Path MOVE = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/NpcMove.java");

    @Test
    void retainedNpcAndMoveSourcesRemainHashPinned() throws Exception {
        assertEquals("72E8DC365DAAC1B27D3F2C92672FCC77FF512825C4B6EAF9C96A6A87EA80EAD2",
                sha256(NPC));
        assertEquals("2B01ACB7F7BDE1BE8019901966F3A103EA004E8E6C1D491E47F914DF4E7743B7",
                sha256(MOVE));
    }

    @Test
    void retainedFailureClassificationAndRetryThresholdsStayOrdered()
            throws Exception {
        String move = readLegacy(MOVE);
        ordered(move, "public enum PathFailure", "NONE", "TRANSIENT",
                "UNREACHABLE", "public boolean wasLastPathUnreachable()");
        ordered(move, "public int moveDirection(int x, int y)",
                "_lastPathFailure = PathFailure.NONE",
                "d > 30D", "_lastPathFailure = PathFailure.UNREACHABLE",
                "mapRouteFound", "_lastPathFailure = PathFailure.TRANSIENT");
        ordered(move, "private int _check_targe(int x, int y, double d)",
                "!_npc.getMap().isPassable(x, y, null)",
                "_lastPathFailure = PathFailure.UNREACHABLE",
                "rawPath == null || rawPath.isEmpty()",
                "_lastPathFailure = PathFailure.UNREACHABLE");

        String npc = readLegacy(NPC);
        ordered(npc, "private static final int PATH_FAIL_GIVE_UP = 3",
                "private static final int COMPANION_PATH_FAIL_GIVE_UP = 20",
                "private static final int COMPANION_CROWD_RETRY_MILLIS = 600",
                "private static final int TRANSIENT_PATH_RETRY_MILLIS = 200");
        ordered(npc, "final int dir = _npcMove.moveDirection(goalX, goalY)",
                "final boolean mapProvedUnreachable",
                "final boolean bossPersistentChase",
                "companionChaser && freeSlot == null",
                "!mapProvedUnreachable || bossPersistentChase",
                "this._pathFailCount++", "COMPANION_PATH_FAIL_GIVE_UP",
                "PATH_FAIL_GIVE_UP", "tagertClear()");
    }

    @Test
    void implementationSeparatesLiveAndStaticPathsAndConnectsBothActors()
            throws Exception {
        Path main = Path.of("src", "main", "java", "local", "lito381", "server");
        String pathfinder = Files.readString(
                main.resolve("ClassicWorldNpcPathfinder.java"));
        ordered(pathfinder, "Result livePath = search(",
                "if (livePath != null) return livePath",
                "Result staticPath = search(",
                "Result.unreachable() : Result.transientFailure()");

        String population = Files.readString(
                main.resolve("ClassicWorldMonsterPopulation.java"));
        ordered(population, "synchronized Movement moveToward(",
                "ClassicWorldNpcPathfinder.nextStep(",
                "bossPersistentChase",
                "actor.pathFailCount++",
                "actor.pathFailCount >= PATH_FAIL_GIVE_UP",
                "actor.combatHate.remove(playerObjectId)",
                "actor.targetObjectId = maxCombatHateTarget(actor)");

        String companion = Files.readString(main.resolve("BattleCompanion.java"));
        ordered(companion, "synchronized boolean retryPath(",
                "if (sameMasterCrowding)",
                "CROWD_RETRY_MILLIS",
                "Failure.TRANSIENT",
                "TRANSIENT_PATH_RETRY_MILLIS",
                "pathFailCount++",
                "pathFailCount < PATH_FAIL_GIVE_UP",
                "targetObjectId = 0");

        String session = Files.readString(main.resolve("ClientSession.java"));
        ordered(session, "private void serverClassicWorldCompanionTick(",
                "chooseClassicWorldCompanionStep(",
                "noFreeClassicWorldCompanionApproachSlot(",
                "sameMasterCompanionCrowdingTarget(",
                "controlled.retryPath(",
                "controlled.moveOneTile(");
    }

    private static void ordered(String source, String... fragments) {
        int position = -1;
        for (String fragment : fragments) {
            position = source.indexOf(fragment, position + 1);
            assertTrue(position >= 0,
                    "missing/out-of-order source fragment: " + fragment);
        }
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(Files.readAllBytes(path)));
    }

    private static String readLegacy(Path path) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        for (Charset charset : new Charset[] {
                StandardCharsets.UTF_8, Charset.forName("MS950"),
                Charset.forName("GB18030")}) {
            try {
                return charset.newDecoder().decode(
                        java.nio.ByteBuffer.wrap(bytes)).toString();
            } catch (java.nio.charset.CharacterCodingException ignored) {
                // Try the next retained-source encoding.
            }
        }
        throw new IllegalStateException("unsupported retained source encoding");
    }
}

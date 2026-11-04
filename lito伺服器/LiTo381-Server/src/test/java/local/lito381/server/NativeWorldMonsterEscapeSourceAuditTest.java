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

/** Hash-pinned proof for retained atkspeed-zero NPC escape semantics. */
final class NativeWorldMonsterEscapeSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/src/com/lineage/server/model/Instance/L1NpcInstance.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/Instance/NpcMove.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/Instance/NpcMoveExecutor.java"));
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path NPC = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/L1NpcInstance.java");
    private static final Path MOVE = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/NpcMove.java");
    private static final Path MOVE_EXECUTOR = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/NpcMoveExecutor.java");

    @Test
    void retainedEscapeSourcesRemainHashPinned() throws Exception {
        assertEquals("72E8DC365DAAC1B27D3F2C92672FCC77FF512825C4B6EAF9C96A6A87EA80EAD2",
                sha256(NPC));
        assertEquals("2B01ACB7F7BDE1BE8019901966F3A103EA004E8E6C1D491E47F914DF4E7743B7",
                sha256(MOVE));
        assertEquals("FF1BE6D6C61F50776038F383E1DFEBFDBC7202433C333B1B96BB85FFAC00005C",
                sha256(MOVE_EXECUTOR));
    }

    @Test
    void retainedNpcSelectsEscapeBeforeOrdinaryAttackAndUsesNativeDistances()
            throws Exception {
        String source = readLegacy(NPC);
        String onTarget = method(source, "public void onTarget()",
                "private static boolean isPetOrSummon");
        ordered(onTarget, "getAtkspeed() == 0", "getPassispeed() > 0",
                "ISASCAPE = true", "if (ISASCAPE)",
                "ascape(target.getLocation())", "attack(target)");
        String escape = method(source, "private void ascape(L1Location location)",
                "private boolean isTraget");
        ordered(escape, "int escapeDistance = 17", "hasSkillEffect(DARKNESS)",
                "escapeDistance = 1", "getTileLineDistance(location)",
                "tagertClear()", "targetReverseDirection",
                "checkObject(dir)", "openDoor(dir)",
                "setDirectionMove(dir)", "getPassispeed()");
    }

    @Test
    void retainedMoveUsesOppositeHeadingThenExactEightDirectionSideStepOrder()
            throws Exception {
        String executor = readLegacy(MOVE_EXECUTOR);
        ordered(executor,
                "HEADING_RD[] = { 4, 5, 6, 7, 0,  1,  2, 3 }",
                "_heading2[] = { 7, 0, 1, 2, 3, 4, 5, 6 }",
                "_heading3[] = { 1, 2, 3, 4, 5, 6, 7, 0 }",
                "_heading4[] = { 2, 3, 4, 5, 6, 7, 0, 1 }",
                "_heading5[] = { 6, 7, 0, 1, 2, 3, 4, 5 }",
                "_heading6[] = { 5, 6, 7, 0, 1, 2, 3, 4 }",
                "_heading7[] = { 3, 4, 5, 6, 7, 0, 1, 2 }",
                "_heading8[] = { 4, 5, 6, 7, 0, 1, 2, 3 }");
        String move = readLegacy(MOVE);
        String check = method(move, "public int checkObject(final int heading)",
                "private boolean landingTileDynamicClearForNpc");
        ordered(check, "h, _heading2[h], _heading3[h], _heading4[h]",
                "_heading5[h], _heading6[h], _heading7[h], _heading8[h]",
                "map.isPassable", "landingTileDynamicClearForNpc(dir)");
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
        byte[] bytes = LocalResources.readAllBytes(path);
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
                        .digest(LocalResources.readAllBytes(path)));
    }
}

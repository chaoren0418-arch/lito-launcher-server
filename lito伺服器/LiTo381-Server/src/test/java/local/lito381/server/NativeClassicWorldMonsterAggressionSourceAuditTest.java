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
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Hash-pinned proof for ordinary WORLD L1Monster target acquisition. */
final class NativeClassicWorldMonsterAggressionSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path MONSTER = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/L1MonsterInstance.java");
    private static final Path DATABASE = RETAINED.resolve("db/380_db.sql");
    private static final Path OTHER_CONFIG = RETAINED.resolve("config/other.properties");
    private static final Path RESOURCE = Path.of(
            "src/main/resources/world/classic-world-monster-aggression.lma.gz");

    @Test
    void protectedSelectedInputsAndGeneratedResourceRemainPinned() throws Exception {
        assertEquals(ClassicWorldMonsterAggressionCatalog.RETAINED_MONSTER_SOURCE_SHA256,
                sha256(MONSTER));
        assertEquals(ClassicWorldMonsterAggressionCatalog.SELECTED_DATABASE_SHA256,
                sha256(DATABASE));
        assertEquals(ClassicWorldMonsterAggressionCatalog.RETAINED_OTHER_CONFIG_SHA256,
                sha256(OTHER_CONFIG));
        assertEquals(ClassicWorldMonsterAggressionCatalog.RESOURCE_SHA256,
                sha256(RESOURCE));
        assertTrue(Files.readString(OTHER_CONFIG, StandardCharsets.UTF_8)
                .contains("kill_red = true"));
    }

    @Test
    void retainedSearchOrderAndSourceSpecificRulesRemainPinned() throws Exception {
        String source = readLegacy(MONSTER);
        String search = method(source,
                "private L1Character searchTarget(L1MonsterInstance npc)",
                "private L1PcInstance attackPc2");
        ordered(search,
                "World.get().getVisiblePlayer(npc)",
                "pc.getCurrentHp() <= 0",
                "npc.get_showId() != pc.get_showId()",
                "pcSenseDist > AGRO_LOS_REQUIRED_BEYOND_TILES",
                "npc.getMapId() == 410",
                "pc.getTempCharGfx() == 4261",
                "npc.getNpcTemplate().get_family() == NpcTable.ORC",
                "npc.attackPc1(pc)",
                "npc.attackPc2(pc)",
                "npc.getNpcTemplate().getKarma() < 0",
                "pc.getTempCharGfx() == 6034",
                "npc.targetPlayer1000(pc)",
                "!pc.isInvisble()",
                "npc.getNpcTemplate().is_agrocoi()",
                "pc.hasSkillEffect(67)",
                "npc.getNpcTemplate().is_agrososc()",
                "npc.getNpcTemplate().is_agro()",
                "pc.getGfxId() == npc.getNpcTemplate().is_agrogfxid1()",
                "pc.getGfxId() == npc.getNpcTemplate().is_agrogfxid2()");
        assertTrue(search.contains("pc.getClan().getCastleId() == L1CastleLocation.OT_CASTLE_ID"));

        String red = method(source,
                "private L1PcInstance targetPlayer1000", "public void setLink");
        ordered(red, "ConfigOther.KILLRED", "!this.getNpcTemplate().is_agro()",
                "!this.getNpcTemplate().is_agrososc()",
                "is_agrogfxid1() < 0", "is_agrogfxid2() < 0",
                "pc.getLawful() < -1000");
    }

    @Test
    void productionAutonomousBranchDoesNotInventCompanionAcquisition()
            throws Exception {
        String gameHub = Files.readString(Path.of(
                "src/main/java/local/lito381/server/GameHub.java"));
        String method = method(gameHub,
                "private ClassicWorldTarget nearestClassicWorldMonsterTarget(",
                "private void groundTickSafely()");
        int ordinary = method.indexOf(
                "ClassicWorldMonsterAggressionCatalog.Profile profile");
        assertTrue(ordinary > 0);
        String ordinaryBranch = method.substring(ordinary);
        assertTrue(ordinaryBranch.contains(
                "ClassicWorldMonsterAggressionCatalog.acquisition("));
        assertFalse(ordinaryBranch.contains("classicWorldCompanionSnapshots"));
        assertTrue(gameHub.contains("findClassicWorldMonsterTarget("));
        assertTrue(gameHub.contains("members, actor, retainedTargetId"));
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

package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks retained NPC skill33 target rules, defense and independent state. */
final class NativeClassicWorldMonsterCompanionCurseParalysisSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
    }

    private static final Path SOURCE = Path.of("..", "1-伺服器", "src",
            "com", "lineage", "server").toAbsolutePath().normalize();
    private static final Path MODE = SOURCE.resolve(
            "model/skill/skillmode/CURSE_PARALYZE.java");
    private static final Path CURSE = SOURCE.resolve("model/L1CurseParalysis.java");
    private static final Path MAGIC = SOURCE.resolve("model/L1MagicNpc.java");
    private static final Path CHARACTER = SOURCE.resolve("model/L1Character.java");
    private static final Path POISON_PARALYSIS = SOURCE.resolve(
            "model/poison/L1ParalysisPoison.java");

    @Test void retainedSourcesRemainTheReviewedAuthority() throws Exception {
        assertEquals("A90F2CC61A31680DD7843F7EA7152815D8039B14AF3F6A28B2D19D387F73E959",
                sha256(MODE));
        assertEquals(ClassicWorldMonsterStatusSkillCatalog
                        .RETAINED_CURSE_PARALYSIS_SHA256,
                sha256(CURSE));
        assertEquals("DB8C75837554B567DDAD4D9CF1CACB62DFB8D7D9AD4A58CC9DF5C68DA685AE95",
                sha256(MAGIC));
        assertEquals("56DAD7E55D73924D631D28EE04A56413536F3921BE13D732607C412F24148C29",
                sha256(CHARACTER));
        assertEquals("7FF6F8023D81EFE9E2AEA6730575B58B595D9751340490DA503D07C78EBD3320",
                sha256(POISON_PARALYSIS));
    }

    @Test void npcCasterInstallsOnlyPcOrMonsterWithNativeTiming()
            throws Exception {
        String mode = normalized(MODE);
        assertTrue(mode.contains("L1CurseParalysis.curse(cha, delay, time, 1)"));
        assertTrue(mode.contains("else if (cha instanceof L1MonsterInstance)"));
        assertTrue(mode.contains("L1CurseParalysis.curse(cha, 0, time, 0)"));
        assertTrue(mode.contains("!cha.hasSkillEffect(EARTH_BIND)"));
        assertTrue(mode.contains("!cha.hasSkillEffect(ICE_LANCE)"));
        assertTrue(mode.contains("!cha.hasSkillEffect(FREEZING_BLIZZARD)"));
        assertTrue(mode.contains("!cha.hasSkillEffect(FREEZING_BREATH)"));

        String curse = normalized(CURSE);
        assertTrue(curse.contains(
                "if (!((cha instanceof L1PcInstance) || (cha instanceof L1MonsterInstance)))"));
        assertTrue(curse.contains("this._target.setPoisonEffect(2)"));
        assertTrue(curse.contains("if (_target instanceof L1PcInstance)"));
        assertTrue(curse.contains("new S_Paralysis(1, true)"));
        assertTrue(curse.contains("_target.setParalyzed(true)"));
    }

    @Test void npcDefenseAndPoisonParalysisUseIndependentNativeSlots()
            throws Exception {
        String magic = normalized(MAGIC);
        assertTrue(magic.contains("case NPC_NPC:"));
        assertTrue(magic.contains("this._targetNpc.hasSkillEffect(EARTH_BIND)"));
        assertTrue(magic.contains("final int targetMr = getTargetMr()"));
        assertTrue(magic.contains(
                "probability -= targetMr * config.getMrMultiplier()"));

        String character = normalized(CHARACTER);
        assertTrue(character.contains("private volatile L1Paralysis _paralysis"));
        assertTrue(character.contains("private volatile L1Poison _poison"));
        assertTrue(character.contains("tryInstallParalysis("));
        assertTrue(character.contains("tryInstallPoison("));
        assertTrue(normalized(POISON_PARALYSIS).contains("extends L1Poison"));
    }

    @Test void currentServerKeepsNpcWireAndLifecycleSeparate() throws Exception {
        String session = normalized(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String companion = normalized(Path.of(
                "src/main/java/local/lito381/server/BattleCompanion.java"));
        assertTrue(session.contains("selectedSourceAccounts().npcPhysicalStats("));
        assertTrue(session.contains("effectiveMagicResistance("));
        assertTrue(session.contains("activateNpcCurseParalysis("));
        assertTrue(session.contains("pollClassicWorldCompanionCurseParalysis("));
        assertTrue(session.contains("monster-curse-paralysis-active"));
        assertTrue(session.contains("monster-curse-paralysis-expired"));
        assertTrue(companion.contains("private final BattleTimedStatus curseParalysisStatus"));
        assertTrue(companion.contains("boolean nativeMonsterTarget = kind == Kind.TAMED"));
        assertTrue(companion.contains("poisonParalysis || curseParalysis"));
    }

    private static String normalized(Path path) throws Exception {
        return Files.readString(path).replace("\r\n", "\n");
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(Files.readAllBytes(path)));
    }
}

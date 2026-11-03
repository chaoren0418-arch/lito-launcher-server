package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks the ordinary-WORLD dispatch and phase-aware group-healing boundary. */
final class NativeWorldGroupHealingSourceAuditTest {
    private static final Path SESSION = Path.of(
            "src/main/java/local/lito381/server/ClientSession.java");

    @Test
    void worldDispatcherReachesBothSelectorOnlyHealingFamilies() throws Exception {
        String source = Files.readString(SESSION, StandardCharsets.UTF_8);
        int worldStart = source.indexOf("if (phase == Phase.WORLD)");
        int worldEnd = source.indexOf("long spellRequestNow", worldStart);
        String world = source.substring(worldStart, worldEnd);
        int healAll = world.indexOf(
                "BattleSpellCatalog.groupHealingSpell(selector.skillId())");
        int blessing = world.indexOf(
                "BattleSpellCatalog.naturesBlessingSpell(selector.skillId())");
        int unsupported = world.indexOf(
                "rejected unsupported classic-world skill=");
        assertTrue(healAll >= 0 && blessing > healAll && unsupported > blessing);
        assertTrue(world.contains(
                "useGroupHealingSpell(payload, worldGroupHealing)"));
        assertTrue(world.contains(
                "useNaturesBlessingSpell(payload, worldNaturesBlessing)"));
    }

    @Test
    void healAllUsesWorldInstanceCollisionAndCompanionSources() throws Exception {
        String source = Files.readString(SESSION, StandardCharsets.UTF_8);
        int start = source.indexOf("private void useGroupHealingSpell(");
        int end = source.indexOf("private void useClassicWorldHealingSpell(", start);
        String method = source.substring(start, end);
        assertTrue(method.contains("originPhase != Phase.BATTLE"
                + " && originPhase != Phase.WORLD"));
        assertTrue(method.contains("classicWorldMembers(originMapId)"));
        assertTrue(method.contains("classicWorldCompanionSnapshots()"));
        assertTrue(method.contains("ClassicWorldMapCatalog.forMap(originMapId)"));
        assertTrue(method.contains("classicWorldInstanceId == originInstanceId"));
        assertTrue(method.contains("casterWorldMap.hasLineOfSight("));
        assertTrue(method.contains("target.takeClassicWorldPlayerDamage("));
        assertTrue(method.contains("target-to=clan|own-pet area=native-screen"));
        assertCleanupAfterCommit(method);
    }

    private static void assertCleanupAfterCommit(String method) {
        int commit = method.indexOf("spellReadyNanos.put(spell.skillId()");
        int meditation = method.indexOf("cancelMeditation(", commit);
        int barrier = method.indexOf("cancelAbsoluteBarrier(", meditation);
        int invisibility = method.indexOf("revealInvisibility(", barrier);
        assertTrue(commit >= 0 && meditation > commit
                && barrier > meditation && invisibility > barrier);
        assertTrue(!method.contains("commitSpellReady(spell.skillId()"));
    }
}

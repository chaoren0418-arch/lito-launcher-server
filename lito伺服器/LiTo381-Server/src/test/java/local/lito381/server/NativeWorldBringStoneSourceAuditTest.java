package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill100 and its ordinary-WORLD inventory/ROI route. */
final class NativeWorldBringStoneSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient();
    }

    private static final Path RETAINED = Path.of("../1-伺服器");

    @Test
    void nativeDatabaseAndRetainedRefinementRulesStayExact() throws Exception {
        String database = Files.readString(
                RETAINED.resolve("db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (100, 0, '提煉魔石', 13, 3, 5, 0, 0, 0, 0, 0, 'none', 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 8, '$2507', 19, 2944, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (100, '提煉魔石', NULL, NULL);"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        assertTrue(assembly.contains(
                "jumptable 004CB66E cases 11,20,72,92,99,106"));
        int producer = assembly.indexOf("loc_4CBA1E:");
        int producerEnd = assembly.indexOf("loc_4CBA2C:", producer);
        String route = assembly.substring(producer, producerEnd);
        assertTrue(route.contains("call    sub_4C9DC0"));

        String use = Files.readString(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        assertTrue(use.contains("final int dark = (int) (10 + (pc.getLevel() * 0.8)"));
        assertTrue(use.contains("final int brave = (int) (dark / 2.1);"));
        assertTrue(use.contains("final int wise = (int) (brave / 2.0);"));
        assertTrue(use.contains("final int kayser = (int) (wise / 1.9);"));
        assertTrue(use.contains("final int chance = random.nextInt(100) + 1;"));
        assertTrue(use.contains("case 40320:"));
        assertTrue(use.contains("case 40323:"));
        assertTrue(use.contains("pc.sendPackets(new S_ServerMessage(403, targetNameId))"));
    }

    @Test
    void worldDispatchCommitInventoryAndRoiStayConnected() throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.BringStoneSpell worldBringStone", worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        assertTrue(session.substring(dispatch, unsupported).contains(
                "useBringStoneSpell(payload, worldBringStone)"));

        int methodStart = session.indexOf("private void useBringStoneSpell(");
        int methodEnd = session.indexOf(
                "private void useTurnUndeadSpell(", methodStart);
        String method = session.substring(methodStart, methodEnd);
        assertTrue(method.contains("Protocol.objectSpellRequest(payload)"));
        assertTrue(method.contains("phase != Phase.BATTLE && phase != Phase.WORLD"));
        assertTrue(method.contains("ClassicWorldMapCatalog.supports(classicWorldMapId)"));
        assertTrue(method.contains("inventory.item(request.targetObjectId())"));
        assertTrue(method.contains("nextInventory.remove(request.targetObjectId(), 1)"));
        assertTrue(method.contains("InventoryCatalog.require(refinement.targetCatalogId())"));
        assertTrue(method.contains("commitSpellReady(spell.skillId()"));
        assertTrue(method.contains("originPhase = phase"));
        assertTrue(method.contains("classicWorldMembers(originMapId)"));
        assertTrue(method.contains("member.classicWorldInstanceId != originInstanceId"));
        assertTrue(method.contains("BattleSpellCatalog.isInNativeScreen("));
        assertTrue(method.contains("state.saveEconomyState("));
        assertTrue(method.contains("phase=\" + originPhase"));
    }
}

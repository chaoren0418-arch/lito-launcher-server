package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill75 and its complete ordinary-WORLD resurrection families. */
final class NativeWorldGreaterResurrectionSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/clientpackets/C_Attr.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/skillmode/GREATER_RESURRECTION.java"));
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void nativeDatabaseAndRetainedPlayerNpcRulesStayExact() throws Exception {
        String database = LocalResources.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (75, 0, '終極返生術', 10, 2, 100, 0, 40318, 1, 10, 0, 'buff', 3, 0, 0, 0, 0, 0, 0, 32, 0, -1, 0, 0, 4, '$1483', 19, 3944, 0, 0, 0, 280);"));

        String mode = LocalResources.readString(RETAINED.resolve(
                "model/skill/skillmode/GREATER_RESURRECTION.java"),
                StandardCharsets.UTF_8);
        assertTrue(mode.contains("if (cha instanceof L1PcInstance)"));
        assertTrue(mode.contains("new S_Message_YN(322)"));
        assertTrue(mode.contains("if (cha instanceof L1NpcInstance)"));
        assertTrue(mode.contains("if (!(cha instanceof L1TowerInstance))"));
        assertTrue(mode.contains("isCantResurrect()"));
        assertTrue(mode.contains("npc.resurrect(npc.getMaxHp() / 4)"));
        assertTrue(mode.contains("npc instanceof L1PetInstance"));
        assertTrue(mode.contains("new S_ServerMessage(592)"));

        String answer = LocalResources.readString(RETAINED.resolve(
                "clientpackets/C_Attr.java"), StandardCharsets.UTF_8);
        int branch = answer.indexOf("case 322:");
        String question322 = answer.substring(branch, branch + 1_500);
        assertTrue(question322.contains("pc.resurrect(pc.getMaxHp())"));
        assertTrue(question322.contains("pc.resExp()"));
    }

    @Test
    void worldDispatcherConnectsDeadPcCompanionAndOrdinaryMonster() throws Exception {
        String session = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.ResurrectionSpell worldResurrection",
                worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        int call = session.indexOf(
                "useClassicWorldResurrectionSpell(payload, worldResurrection)",
                dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch
                && call > unsupported);

        int methodStart = session.indexOf(
                "private void useClassicWorldResurrectionSpell(");
        int methodEnd = session.indexOf(
                "private void useCallOfNatureSpell(", methodStart);
        assertTrue(methodStart > 0 && methodEnd > methodStart);
        String method = session.substring(methodStart, methodEnd);
        assertTrue(method.contains("Protocol.objectSpellRequest(payload)"));
        assertTrue(method.contains("hub.classicWorldMembers("));
        assertTrue(method.contains("playerTarget.playerHp == 0"));
        assertTrue(method.contains("classicWorldCompanionSnapshots()"));
        assertTrue(method.contains("ClassicWorldResurrectionCatalog.allowsSelectedTemplate("));
        assertTrue(method.contains("BattleSpellCatalog.isInNativeScreen("));
        assertTrue(method.contains("Protocol.messageCode(592)"));
        assertTrue(method.contains("Protocol.greaterResurrectionAsk()"));
        assertTrue(method.contains("new PendingResurrection("));
        assertTrue(method.contains("spell.restoredNpcHp("));
        assertTrue(method.contains("hub.resurrectClassicWorldMonster("));
        assertTrue(method.contains("Protocol.resurrect("));
        assertTrue(method.contains("Protocol.hitRatio("));
        assertFalse(method.contains("hub.roomMembers("));
        assertFalse(method.contains("hub.roomCombatants("));

        ClassicWorldMonsterCatalog.SpawnDefinition ogre =
                ClassicWorldMonsterCatalog.definition(4, 566);
        assertTrue(ogre != null && ogre.template().npcId() == 45278);
        assertTrue(ogre.template().gfxId() == 1020);
        assertTrue(ogre.template().hp() == 500);
    }
}

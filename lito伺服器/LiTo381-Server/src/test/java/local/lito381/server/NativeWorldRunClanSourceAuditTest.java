package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks skill118's retained source and ordinary-WORLD portal capability. */
final class NativeWorldRunClanSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/skillmode/RUN_CLAN.java"));
    }

    @Test
    void nativeExactTargetAndMapPolicyStayExact() throws Exception {
        String database = LocalResources.readString(
                Path.of("../1-伺服器/db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (118, 0, '援護盟友', 15, 5, 30, 0, 0, 0, 0, 0, 'buff', 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 32, '', 19, 0, 0, 0, 0, 280);"));
        String mode = LocalResources.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/model/skill/skillmode/RUN_CLAN.java"),
                StandardCharsets.UTF_8);
        assertTrue(mode.contains("pc.getMap().isEscapable() || pc.isGm()"));
        assertTrue(mode.contains("L1CastleLocation.checkInAllWarArea"));
        assertTrue(mode.contains("clanPc.getMapId() == 0"));
        assertTrue(mode.contains("clanPc.getMapId() == 4"));
        assertTrue(mode.contains("clanPc.getMapId() == 304"));
        assertTrue(mode.contains(
                "clanPc.getX(), clanPc.getY(), clanPc.getMapId(), 5, true"));
        assertTrue(mode.contains("new S_ServerMessage(1192)"));
        assertTrue(mode.contains("new S_ServerMessage(647)"));
    }

    @Test
    void worldDispatcherSnapshotsAndBothPortalRevalidationsStayConnected()
            throws Exception {
        String session = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.RunClanSpell worldRunClan", worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        String method = session.substring(
                session.indexOf("private void useRunClanSpell("),
                session.indexOf("private void useInvisibilitySpell("));
        assertTrue(method.contains("admitClassicWorldSpellFamily("));
        assertTrue(method.contains("hub.onlineMemberByName("));
        assertTrue(method.contains("callClanSharedContext(target)"));
        assertTrue(method.contains("ClassicWorldMapCatalog.forMap(classicWorldMapId)"));
        assertTrue(method.contains(".allowsEscape()"));
        assertTrue(method.contains("runClanClassicWorldDestinationAllowed(target)"));
        assertTrue(method.contains("target.classicWorldInstanceId != -1"));
        assertTrue(method.contains("target.classicWorldMapId != 0"));
        assertTrue(method.contains("target.classicWorldMapId != 4"));
        assertTrue(method.contains("target.classicWorldMapId != 304"));
        assertTrue(method.contains("callClanClassicWorldCastleId("));
        // T117: RUN_CLAN.java checkInAllWarArea has no war-time exception.
        assertTrue(!method.contains("target.hub.classicCastleWarActive("));
        assertTrue(method.contains("new PendingRunClanPortal("));
        assertTrue(method.contains("sourceMapId, sourceInstanceId"));
        assertTrue(method.contains("targetMapId, targetInstanceId"));
        assertTrue(method.contains("target.positionX, target.positionY"));
        assertTrue(method.contains("new PendingClassicWorldPortal("));
        assertTrue(method.contains("\"Run-Clan-118\""));
        assertTrue(session.indexOf(
                "runClanPortalStillValid(runClanPortal", session.indexOf(
                        "private void returnSummon(")) > 0);
        assertTrue(session.indexOf(
                "runClanPortalStillValid(runClanPortal", session.indexOf(
                        "private void completePortal(")) > 0);
    }
}

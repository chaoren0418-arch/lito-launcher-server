package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks skill116's source and ordinary-WORLD invitation/portal route. */
final class NativeWorldCallClanSourceAuditTest {
    @Test
    void nativeQuestionMapGeometryAndOccupancyStayExact() throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (116, 0, '呼喚盟友', 15, 3, 30, 0, 0, 0, 0, 0, 'buff', 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 8, '', 19, 0, 0, 0, 0, 280);"));
        String attr = Files.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/clientpackets/C_Attr.java"),
                StandardCharsets.UTF_8);
        assertTrue(attr.contains("pc.consumeIncomingCallClanInvitation(callerId)"));
        assertTrue(attr.contains("(mapId != 0) && (mapId != 4) && (mapId != 304)"));
        assertTrue(attr.contains("!pc.getMap().isEscapable()"));
        assertTrue(attr.contains("heading = (heading + 4) & 7"));
        assertTrue(attr.contains("object instanceof L1Character"));
        assertTrue(attr.contains("new S_ServerMessage(627)"));
    }

    @Test
    void worldDispatcherCapabilityRoiAndPortalRevalidationStayConnected()
            throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.CallClanSpell worldCallClan", worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        String method = session.substring(
                session.indexOf("private void useCallClanSpell("),
                session.indexOf("private void useRunClanSpell("));
        assertTrue(method.contains("admitClassicWorldSpellFamily("));
        assertTrue(method.contains("callClanSharedClassicWorld("));
        assertTrue(method.contains("classicWorldMembers(originMapId)"));
        assertTrue(method.contains("callClanClassicWorldDestinationAllowed("));
        assertTrue(method.contains("invitation.mapId() != 0"));
        assertTrue(method.contains("invitation.mapId() != 4"));
        assertTrue(method.contains("invitation.mapId() != 304"));
        assertTrue(method.contains("ClassicWorldMapCatalog.forMap(classicWorldMapId)"));
        assertTrue(method.contains(".allowsEscape()"));
        assertTrue(method.contains("hub.classicWorldMonsterOccupies("));
        assertTrue(method.contains("ClassicWorldNpcCatalog.all(mapId)"));
        assertTrue(method.contains("new PendingClassicWorldPortal("));
        assertTrue(session.contains("callClanPortalStillValid(callClanPortal"));
        assertTrue(session.contains("Call-Clan-116"));
    }
}

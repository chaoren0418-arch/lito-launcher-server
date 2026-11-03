package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** Locks 0.7.557's war-field and expulsion behavior to selected source files. */
final class NativeClassicCastleWarFieldSourceAuditTest {
    @Test
    void selectedSourceHashesAndBeginOrderRemainExact() throws Exception {
        assertHash(ClassicWorldCastleWarField.WAR_SPAWN_SOURCE_SHA256,
                Path.of("../1-伺服器/src/com/lineage/server/model/L1WarSpawn.java"));
        assertHash(ClassicWorldCastleWarField.CASTLE_LOCATION_SOURCE_SHA256,
                Path.of("../1-伺服器/src/com/lineage/server/model/L1CastleLocation.java"));
        assertHash(ClassicWorldCastleWarField.TOWN_LOCATION_SOURCE_SHA256,
                Path.of("../1-伺服器/src/com/lineage/server/model/L1TownLocation.java"));
        assertHash(ClassicWorldCastleWarField.FIELD_PACKET_SOURCE_SHA256,
                Path.of("../1-伺服器/src/com/lineage/server/serverpackets/S_NPCPack_F.java"));
        assertHash(ClassicWorldCastleWarField.MONSTER_PACKET_SOURCE_SHA256,
                Path.of("../1-伺服器/src/com/lineage/server/serverpackets/S_NPCPack.java"));

        String source = Files.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/timecontroller/server/ServerWarExecutor.java"));
        int spawnTrebuchet = source.indexOf("warspawn.SpawnTrebuchet();");
        int spawnFlag = source.indexOf("warspawn.SpawnFlag(i + 1);", spawnTrebuchet);
        int repair = source.indexOf("door.repairGate();", spawnFlag);
        int begin = source.indexOf("S_PacketBoxWar.MSG_WAR_BEGIN", repair);
        int area = source.indexOf("L1CastleLocation.getWarArea(castleId)", begin);
        int gm = source.indexOf("!pc.isGm()", area);
        int defender = source.indexOf("clan.getCastleId() == castleId", gm);
        int returnPoint = source.indexOf("getGetBackLoc(castleId)", defender);
        int teleport = source.indexOf("L1Teleport.teleport(pc", returnPoint);
        assertTrue(spawnTrebuchet >= 0 && spawnFlag > spawnTrebuchet
                && repair > spawnFlag && begin > repair && area > begin
                && gm > area && defender > gm && returnPoint > defender
                && teleport > returnPoint);
    }

    @Test
    void independentServerPreservesBeginAndEndPublicationOrder() throws Exception {
        String hub = Files.readString(Path.of(
                "src/main/java/local/lito381/server/GameHub.java"));
        int method = hub.indexOf("private void publishClassicCastleWarEvents(");
        int helper = hub.indexOf("private static void deliverClassicCastleWarEvent(", method);
        String publication = hub.substring(method, helper);

        int beginBranch = publication.indexOf("EventKind.BEGIN");
        int fieldBegin = publication.indexOf("classicCastleWarField.begin(", beginBranch);
        int fieldDelivery = publication.indexOf("deliverClassicCastleWarField(", fieldBegin);
        int repair = publication.indexOf("repairClassicCastleDoors(", fieldDelivery);
        int doors = publication.indexOf("deliverClassicCastleWarDoors(", repair);
        int beginEvent = publication.indexOf("deliverClassicCastleWarEvent(", doors);
        int expel = publication.indexOf("serverClassicCastleWarExpel(", beginEvent);
        assertTrue(fieldBegin > beginBranch && fieldDelivery > fieldBegin
                && repair > fieldDelivery && doors > repair
                && beginEvent > doors && expel > beginEvent);

        int endBranch = publication.indexOf("} else {", expel);
        int endEvent = publication.indexOf("deliverClassicCastleWarEvent(", endBranch);
        int fieldEnd = publication.indexOf("classicCastleWarField.end(", endEvent);
        int endFieldDelivery = publication.indexOf(
                "deliverClassicCastleWarField(", fieldEnd);
        int endRepair = publication.indexOf("repairClassicCastleDoors(", endFieldDelivery);
        int endDoors = publication.indexOf("deliverClassicCastleWarDoors(", endRepair);
        assertTrue(endEvent > endBranch && fieldEnd > endEvent
                && endFieldDelivery > fieldEnd && endRepair > endFieldDelivery
                && endDoors > endRepair);

        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        int expulsion = session.indexOf("boolean serverClassicCastleWarExpel(");
        int next = session.indexOf("private void sendActiveClassicCastleWars()", expulsion);
        String methodBody = session.substring(expulsion, next);
        assertTrue(methodBody.contains("area.contains(classicWorldMapId, positionX, positionY)"));
        assertTrue(methodBody.contains("isAdministrator()"));
        assertTrue(methodBody.contains("defendsClassicCastle(event.castleId())"));
        assertTrue(methodBody.contains("new PendingClassicWorldPortal("));
        assertTrue(methodBody.contains("Protocol.portal("));
    }

    private static void assertHash(String expected, Path path) throws Exception {
        byte[] content = Files.readAllBytes(path);
        String actual = HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content));
        assertEquals(expected, actual, path.toString());
    }
}

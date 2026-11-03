package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks skill113's native producer and ordinary-WORLD L1Character route. */
final class NativeWorldTrueTargetSourceAuditTest {
    @Test
    void nativeRequestResponseDatabaseAndAudienceStayExact() throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (113, 0, '精準目標', 15, 0, 1, 0, 0, 0, 0, 0, 'buff', 3, 0, 0, 0, 0, 0, 0, 2, 0, -1, 0, 0, 1, '$1995', 19, 1765, 0, 0, 0, 0);"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int producerStart = assembly.indexOf("sub_4CADF0 proc near");
        int producerEnd = assembly.indexOf("sub_4CADF0 endp", producerStart);
        String producer = assembly.substring(producerStart, producerEnd);
        assertTrue(producer.contains("push    0Eh"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("\"cccdhhs\""));
        int receiverStart = assembly.indexOf("sub_4B0540 proc near");
        int receiverEnd = assembly.indexOf("sub_4B0540 endp", receiverStart);
        String receiver = assembly.substring(receiverStart, receiverEnd);
        assertTrue(receiver.contains("push    35h ; '5'"));
        assertTrue(receiver.contains("\"dhhs\""));

        String mode = Files.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/model/skill/skillmode/TRUE_TARGET.java"),
                StandardCharsets.UTF_8);
        assertTrue(mode.contains("WorldClan.get().getClan"));
        assertTrue(mode.contains("pc.getLocation().isInScreen(srcpc.getLocation())"));
        assertTrue(mode.contains("pc.sendPackets(new S_TrueTarget"));
    }

    @Test
    void worldPlayersCompanionsMonstersAndClanRoiRemainConnected()
            throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.TrueTargetSpell worldTrueTarget", worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        assertTrue(session.substring(dispatch, unsupported).contains(
                "useTrueTargetSpell(payload, worldTrueTarget)"));

        int methodStart = session.indexOf(
                "private BattleTrueTarget.Candidate resolveTrueTargetCandidate(");
        int methodEnd = session.indexOf(
                "private void useRoyalAuraSpell(", methodStart);
        String method = session.substring(methodStart, methodEnd);
        assertTrue(method.contains("originPhase == Phase.WORLD"));
        assertTrue(method.contains("classicWorldMembers(originMapId)"));
        assertTrue(method.contains("owner.classicWorldCompanionSnapshots()"));
        assertTrue(method.contains("hub.classicWorldMonstersInScreen("));
        assertTrue(method.contains("BattleTrueTarget.Kind.WORLD_MONSTER"));
        assertTrue(method.contains("admitClassicWorldSpellFamily("));
        assertTrue(method.contains("ClassicWorldMapCatalog.forMap(originMapId)"));
        assertTrue(method.contains("candidate.classicWorldInstanceId == originInstanceId"));
        assertTrue(method.contains("cancelAbsoluteBarrier("));
        assertTrue(method.contains("revealInvisibility("));
        assertTrue(method.contains("Protocol.commandTarget("));
        assertTrue(method.contains("phase=\" + originPhase"));
    }
}

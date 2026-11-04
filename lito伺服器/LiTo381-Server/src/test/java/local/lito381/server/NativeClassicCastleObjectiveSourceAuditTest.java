package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks guardian-tower, crown, castle-war and selected-client evidence together. */
final class NativeClassicCastleObjectiveSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeIdaClient();
    }

    private static final Path SOURCE = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void exactSelectedSourcesRetainTowerCrownAndWarSemantics() throws Exception {
        Path crownPath = SOURCE.resolve("model/Instance/L1CrownInstance.java");
        Path towerPath = SOURCE.resolve("model/Instance/L1TowerInstance.java");
        Path doorPath = SOURCE.resolve("model/Instance/L1DoorInstance.java");
        Path warRequestPath = SOURCE.resolve("clientpackets/C_War.java");
        Path warPath = SOURCE.resolve("model/L1War.java");
        Path packetPath = SOURCE.resolve("serverpackets/S_PacketBoxWar.java");
        assertEquals("1101399B312D59F2BF7F27A0E4DFC17C96D2F0C42599AF064387AF165F1E47BE",
                sha256(crownPath));
        assertEquals("33D912DF3BC31C904F36E36F634C6BF584A22BCB71CC4A4D2134845059B3152F",
                sha256(towerPath));
        assertEquals("A2620D562E27C1EB1098112FAB0301A51A4EF0E294A0A26A090FAEF70413D6C9",
                sha256(doorPath));
        assertEquals("733980808F998F818A52802B3CB8D36C6E556D7F115C2B84814040E43906FED0",
                sha256(warRequestPath));
        assertEquals("A6CBE96D34C5DCD4A14C8CF5DFC3B347DDAC2E63FCD4A468CC83F1D7A5741C6A",
                sha256(warPath));
        assertEquals("EEFDB707A818CB033B6D4E83F190BEEFD3E8CD6D95DBB0ED51A42507EA3B9D35",
                sha256(packetPath));

        String tower = read(towerPath);
        ordered(tower,
                "ServerWarExecutor.get().isNowWar(_castle_id)",
                "subTowerDeadCount >= 4",
                "war.checkClanInWar(pc.getClanname())",
                "existDefenseClan == true) && (isProclamation == false)",
                "towerCrackTransition(newHp)",
                "ActionCodes.ACTION_TowerDie",
                "warspawn.SpawnCrown(_tower._castle_id)");

        String crown = read(crownPath);
        ordered(crown,
                "if (!player.isCrown())",
                "player.getId() != clan.getLeaderId()",
                "clan.getCastleId() != 0",
                "existDefenseClan && (in_war == false)",
                "war.winCastleWar(playerClanName)",
                "new S_CastleMaster(0, defence_clan.getLeaderId())",
                "new S_CastleMaster(castle_id, player.getId())",
                "new S_PacketBoxWar(S_PacketBoxWar.MSG_WAR_INITIATIVE)",
                "new S_PacketBoxWar(S_PacketBoxWar.MSG_WAR_OCCUPY)",
                "warspawn.spawnTower(castle_id)",
                "door.repairGate()");

        String door = read(doorPath);
        ordered(door,
                "if ((this.getMaxHp() == 0) || (this.getMaxHp() == 1))",
                "final int newHp = currentHp - damage",
                "doorCrackTransition(newHp)",
                "(getMaxHp() * 1 / 6) > hp",
                "ActionCodes.ACTION_DoorAction5",
                "(getMaxHp() * 5 / 6) > hp",
                "ActionCodes.ACTION_DoorAction1",
                "ActionCodes.ACTION_DoorDie",
                "setOpenStatus(ActionCodes.ACTION_Open)",
                "setPassable(PASS)",
                "sendDoorPacket()");

        String warRequest = read(warRequestPath);
        int unowned = warRequest.indexOf(
                "if (enemyClan == null) {// 攻城 城堡無主人的狀況");
        int owned = warRequest.indexOf(
                "final String enemyClanName = enemyClan.getClanName()", unowned);
        assertTrue(unowned >= 0 && owned > unowned);
        String unownedBranch = warRequest.substring(unowned, owned);
        assertTrue(unownedBranch.contains(
                "new S_War(1, clanName, \"NPC\")"));
        assertFalse(unownedBranch.contains("new L1War()"));
        assertFalse(unownedBranch.contains("addAttackClan"));

        String packet = read(packetPath);
        assertTrue(packet.contains("public static final int MSG_WAR_INITIATIVE = 3;"));
        assertTrue(packet.contains("public static final int MSG_WAR_OCCUPY = 4;"));
        assertTrue(packet.contains("public static final int MSG_WAR_OVER = 79;"));
        assertTrue(packet.contains("this.writeC(subCode);"));
    }

    @Test
    void selectedLiToReceiverAcceptsThreeAndFourButNotRetainedSubtype79()
            throws Exception {
        Path client = Path.of(
                "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
        Path assembly = Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(client));
        assertEquals("2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A",
                sha256(assembly));

        String all = read(assembly);
        int start = all.indexOf("sub_4B07C0 proc near");
        int end = all.indexOf("sub_4B07C0 endp", start);
        assertTrue(start >= 0 && end > start);
        String receiver = all.substring(start, end);
        assertTrue(receiver.contains("cmp     eax, 28h"));
        assertTrue(receiver.contains("switch 41 cases"));
        int case3 = receiver.indexOf("loc_4B09B9:");
        int case4 = receiver.indexOf("loc_4B0A3E:", case3);
        int case5 = receiver.indexOf("loc_4B0AB6:", case4);
        assertTrue(case3 >= 0 && case4 > case3 && case5 > case4);
        assertFalse(receiver.substring(case3, case4).contains("mov     esi, [esi]"),
                "S_EVENT subtype 3 consumes no payload bytes");
        assertFalse(receiver.substring(case4, case5).contains("mov     esi, [esi]"),
                "S_EVENT subtype 4 consumes no payload bytes");
        assertFalse(receiver.contains("case 79"),
                "selected LiTo.bin rejects subtype 79 above the 0..40 switch");

        String protocol = read(Path.of(
                "src/main/java/local/lito381/server/Protocol.java"));
        int method = protocol.indexOf(
                "static byte[] castleWarOwnershipEvent(ServerEventType type)");
        int next = protocol.indexOf("\n    static ", method + 1);
        String implementation = protocol.substring(method, next);
        assertTrue(implementation.contains(
                "type != ServerEventType.CASTLE_WAR_INITIATIVE"));
        assertTrue(implementation.contains(
                "type != ServerEventType.CASTLE_WAR_OCCUPIED"));
        assertTrue(implementation.contains(
                "new Writer().c(S_EVENT).c(type.wireValue()).bytes()"));
        assertFalse(implementation.contains("79"));
    }

    private static void ordered(String text, String... fragments) {
        int position = -1;
        for (String fragment : fragments) {
            position = text.indexOf(fragment, position + 1);
            assertTrue(position >= 0, "missing/out-of-order source fragment: " + fragment);
        }
    }

    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
}

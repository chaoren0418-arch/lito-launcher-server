package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Retained death-feed/count authority plus selected-LiTo opcode boundary. */
final class NativeClassicWorldKillFeedSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeExisting("LiTo381-開發 local file", Path.of("..", "LiTo381-開發", "analysis", "NATIVE-S-DISPATCH-ALL.json"));
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PC = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/L1PcInstance.java");
    private static final Path CONFIG = RETAINED.resolve(
            "src/com/lineage/config/ConfigMsg.java");
    private static final Path BROADCAST_CONFIG = RETAINED.resolve(
            "config/其他控制端/0-廣播控制表.properties");
    private static final Path GREEN = RETAINED.resolve(
            "src/com/lineage/server/utils/GreenBroadcast.java");
    private static final Path GREEN_PACKET = RETAINED.resolve(
            "src/com/lineage/server/serverpackets/S_PacketBoxGree.java");
    private static final Path SYSTEM_PACKET = RETAINED.resolve(
            "src/com/lineage/server/serverpackets/S_SystemMessage.java");
    private static final Path OTHER = RETAINED.resolve(
            "src/com/lineage/server/templates/L1PcOther.java");
    private static final Path OTHER_PERSISTENCE = RETAINED.resolve(
            "src/com/lineage/server/model/CharacterOtherPersistenceService.java");
    private static final Path SELECTED_CLIENT = Path.of("D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path SELECTED_DISPATCH = Path.of("..", "LiTo381-開發",
            "analysis", "NATIVE-S-DISPATCH-ALL.json").toAbsolutePath().normalize();
    private static final Path TW_GREEN_HOOK = Path.of("..", "2-登入器", "src",
            "_aux", "notification", "green_msg_hook.rs")
            .toAbsolutePath().normalize();

    @Test void retainedFilesRemainTheReviewedKillFeedAuthority() throws Exception {
        assertEquals("D652D175C31ED3A7D1F6682792BD447BC36AB8C0211A2C7A56B070C0D630C19F",
                sha256(PC));
        assertEquals("A0F8FD95C335223D6CF0BB0CCAE43F8CC953C866ED5B80B9BC51A565EB999D99",
                sha256(CONFIG));
        assertEquals("2E6D5DA198F295D54CE93C57A4400566E95F7FB5835557BBD01FFAB8F5A7B5CB",
                sha256(BROADCAST_CONFIG));
        assertEquals("32DD9E26D69218D349C9202C73B30ED98884875C29A0EF02EF9B57A46E19E02A",
                sha256(GREEN));
        assertEquals("D72E6B5151F8FCB0695FF6024736B3AA9ECC892A50D907A1F45BB65F6B7EDAC7",
                sha256(GREEN_PACKET));
        assertEquals("5B137EC3683E6170BC4EBC960C290CD672D24513D8BFE9C7C69D02296DDC85ED",
                sha256(SYSTEM_PACKET));
        assertEquals("9E0E0313E7CBFCA62A756F7DC746EA3DE7E8CEC9FA015B4B92E1BDE0030673C9",
                sha256(OTHER));
        assertEquals("AB6FB895D3395E4E6CA6F5AD8F54B591146CABFDF85F03C292445A8F9E735CD6",
                sha256(OTHER_PERSISTENCE));
    }

    @Test void retainedDeathOrdersWarAndFeedBeforePenaltyWithExactGates()
            throws Exception {
        String source = Files.readString(PC).replace("\r\n", "\n");
        int encounter = source.indexOf("if (isEncounter())");
        int castle = source.indexOf("castleWarResult()", encounter);
        int clan = source.indexOf("simWarResult(lastAttacker)", castle);
        int field = source.indexOf("isInWarAreaAndWarTime", clan);
        int level = source.indexOf("getLevel() >= 10", field);
        int gm = source.indexOf("!fightPc.isGm()", level);
        int combat = source.indexOf("if (!isCombatZone)", gm);
        int announce = source.indexOf("ConfigMsg.announceKill", combat);
        int kills = source.indexOf("add_killCount(1)", announce);
        int deaths = source.indexOf("add_deathCount(1)", kills);
        int penalty = source.indexOf("deathPenalty", deaths);
        assertTrue(encounter >= 0 && castle > encounter && clan > castle
                && field > clan && level > field && gm > level
                && combat > gm && announce > combat && kills > announce
                && deaths > kills && penalty > deaths);
    }

    @Test void retainedConfigurationAndBothAnnouncementWiresRemainExplicit()
            throws Exception {
        String properties = Files.readString(BROADCAST_CONFIG);
        assertTrue(properties.contains("KillMsgType=2"));
        assertTrue(properties.contains(
                "MsgKill=\\\\fX[%s] \\\\fT擊殺 \\\\fR[%s]"));

        String config = Files.readString(CONFIG);
        assertTrue(config.contains("World.get().broadcastPacketToAll(new S_SystemMessage(msg))"));
        assertTrue(config.contains("GreenBroadcast.broadcastToAll(msg)"));
        String greenPacket = Files.readString(GREEN_PACKET);
        assertTrue(greenPacket.contains("writeC(S_OPCODE_PACKETBOX)"));
        assertTrue(greenPacket.contains("writeC(GREEN_MESSAGE)"));
        assertTrue(greenPacket.contains("writeC(0x02)"));
        String systemPacket = Files.readString(SYSTEM_PACKET);
        assertTrue(systemPacket.contains("writeC(S_OPCODE_GLOBALCHAT)"));
        assertTrue(systemPacket.contains("writeC(0x09)"));
    }

    @Test void countersAreDurableCharacterOtherColumns() throws Exception {
        String other = Files.readString(OTHER);
        assertTrue(other.contains("add_killCount"));
        assertTrue(other.contains("add_deathCount"));
        String persistence = Files.readString(OTHER_PERSISTENCE);
        assertTrue(persistence.contains("character_other"));
        assertTrue(persistence.contains("killCount"));
        assertTrue(persistence.contains("deathCount"));
    }

    @Test void selectedLiToUsesFaForClanAndNativeMessageForSafeFallback()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        assertEquals("67AD2A6593D31F821DEC4F1EF34916D2186B3841F7B1AD1578A76DADD229960E",
                sha256(SELECTED_DISPATCH));
        String dispatch = Files.readString(SELECTED_DISPATCH);
        assertTrue(dispatch.contains("\"opcode\": \"0xFA\""));
        assertTrue(dispatch.contains("\"name\": \"S_LITO_CLAN\""));
        assertTrue(dispatch.contains("\"handler\": \"sub_4AC1C0\""));
        assertTrue(dispatch.contains("\"opcode\": \"0x0F\""));
        assertTrue(dispatch.contains("\"name\": \"S_MESSAGE\""));
        assertTrue(dispatch.contains("\"handler\": \"sub_4A4190\""));

        String implementation = Files.readString(Path.of("src/main/java/local/lito381/server/ClientSession.java"));
        int begin = implementation.indexOf("private void publishClassicWorldPlayerKill(");
        int end = implementation.indexOf("private void applyClassicWorldDeathExperiencePenalty", begin);
        String method = implementation.substring(begin, end);
        assertTrue(method.contains("Protocol.systemMessage(9, message)"));
        assertFalse(method.contains("Protocol.S_PACKET_BOX"));
        assertEquals(0xFA, Protocol.S_LITO_CLAN);
        assertEquals(0x0F, Protocol.S_MESSAGE);
    }

    @Test void twGreenHookIsASeparateClientSpecificAugmentation() throws Exception {
        assertEquals("350E53600F7715FB790DBA72A0292970E03717D801F695AF2BFAF6ED57A5614D",
                sha256(TW_GREEN_HOOK));
        String hook = Files.readString(TW_GREEN_HOOK);
        assertTrue(hook.contains("const OPCODE_GREEN: u32 = 0x54"));
        assertTrue(hook.contains("S_PacketBoxGree"));
        assertFalse(hook.contains("LiTo.bin"),
                "TW hook evidence must not be mislabelled as selected LiTo support");
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
}

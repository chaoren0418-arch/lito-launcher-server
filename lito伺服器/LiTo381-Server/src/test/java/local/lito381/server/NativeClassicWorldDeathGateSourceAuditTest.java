package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Hash-locked retained ordering and selected-client duel boundary. */
final class NativeClassicWorldDeathGateSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeExisting("LiTo381-開發 local file", Path.of("..", "LiTo381-開發", "analysis", "NATIVE-C-PRODUCERS-ALL.json"));
    }

    private static final Path RETAINED_PC = Path.of("..", "1-伺服器", "src",
            "com", "lineage", "server", "model", "Instance",
            "L1PcInstance.java").toAbsolutePath().normalize();
    private static final Path RETAINED_OTHER = Path.of("..", "1-伺服器", "src",
            "com", "lineage", "config", "ConfigOther.java")
            .toAbsolutePath().normalize();
    private static final Path RETAINED_WAR = Path.of("..", "1-伺服器", "src",
            "com", "lineage", "server", "model", "L1War.java")
            .toAbsolutePath().normalize();
    private static final Path C_PRODUCERS = Path.of("..", "LiTo381-開發",
            "analysis", "NATIVE-C-PRODUCERS-ALL.json")
            .toAbsolutePath().normalize();
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");

    @Test void retainedDeathGateOrderSeparatesPenaltyFromPkExemptions()
            throws Exception {
        assertEquals("D652D175C31ED3A7D1F6682792BD447BC36AB8C0211A2C7A56B070C0D630C19F",
                sha256(RETAINED_PC));
        String source = Files.readString(RETAINED_PC).replace("\r\n", "\n");
        int action = source.indexOf(
                "sendPacketsAll(new S_DoActionGFX(targetobjid, ActionCodes.ACTION_Die))");
        int duel = source.indexOf("getFightId() == fightPc.getId()", action);
        int encounter = source.indexOf("if (isEncounter())", duel);
        int castleWar = source.indexOf("castleWarResult()", encounter);
        int safety = source.indexOf("if (isSafetyZone)", castleWar);
        int item = source.indexOf("getAlwaysDropItemsForDeath()", safety);
        int skill = source.indexOf("this.lostSkillRate()", item);
        int experience = source.indexOf("this.expRate()", skill);
        int warPkReturn = source.indexOf("if (isWar)", experience);
        int castleAreaPkReturn = source.indexOf(
                "L1CastleLocation.checkInAllWarArea", warPkReturn);
        int clanWarPkReturn = source.indexOf("WorldWar.get().isWar(",
                castleAreaPkReturn);
        assertTrue(action >= 0 && action < duel && duel < encounter
                && encounter < castleWar && castleWar < safety
                && safety < item && item < skill && skill < experience
                && experience < warPkReturn && warPkReturn < castleAreaPkReturn
                && castleAreaPkReturn < clanWarPkReturn);
    }

    @Test void retainedEncounterDefaultIsTwenty() throws Exception {
        assertEquals("00DDAFED0C6D0FC88D7474A2F16C880C4D48E2294DD48F181FCAA6A047165235",
                sha256(RETAINED_OTHER));
        String source = Files.readString(RETAINED_OTHER).replace("\r\n", "\n");
        assertTrue(source.contains(
                "set.getProperty(\"encounter_lv\", \"20\")"));
        assertEquals(20, ClassicWorldDeathContext.ENCOUNTER_MAX_LEVEL);
    }

    @Test void retainedLeaderDeathEndsWarBeforePenaltyAndKeepsFatalSnapshot()
            throws Exception {
        assertEquals("A6CBE96D34C5DCD4A14C8CF5DFC3B347DDAC2E63FCD4A468CC83F1D7A5741C6A",
                sha256(RETAINED_WAR));
        String pc = Files.readString(RETAINED_PC).replace("\r\n", "\n");
        int encounter = pc.indexOf("if (isEncounter())");
        int castle = pc.indexOf("castleWarResult()", encounter);
        int simulated = pc.indexOf("simWarResult(lastAttacker)", castle);
        int penalty = pc.indexOf("getAlwaysDropItemsForDeath()", simulated);
        assertTrue(encounter >= 0 && encounter < castle
                && castle < simulated && simulated < penalty);

        int castleLeader = pc.indexOf("this.getId() == clan.getLeaderId()", 
                pc.indexOf("public boolean castleWarResult()"));
        int castleCease = pc.indexOf(
                "war.ceaseWar(this.getClanname(), enemyClanName)", castleLeader);
        int simLeader = pc.indexOf("this.getId() == clan.getLeaderId()",
                pc.indexOf("public boolean simWarResult"));
        int simCease = pc.indexOf(
                "war.ceaseWar(this.getClanname(), enemyClanName)", simLeader);
        int capturedSameWar = pc.indexOf("(warType == 2) && sameWar", simCease);
        assertTrue(castleLeader >= 0 && castleLeader < castleCease
                && simLeader >= 0 && simLeader < simCease
                && simCease < capturedSameWar);

        String war = Files.readString(RETAINED_WAR).replace("\r\n", "\n");
        int typeThree = war.indexOf("case 3: // 結束",
                war.indexOf("private void requestSimWar"));
        int firstClan = war.indexOf(
                "clan1.sendPacketsAll(new S_War(3, clan1_name, clan2_name))",
                typeThree);
        int secondClan = war.indexOf(
                "clan2.sendPacketsAll(new S_War(3, clan1_name, clan2_name))",
                firstClan);
        int remove = war.indexOf("WorldWar.get().removeWar(this)", secondClan);
        assertTrue(typeThree >= 0 && typeThree < firstClan
                && firstClan < secondClan && secondClan < remove);
    }

    @Test void selectedClientRegistersCduelButHasNoNativeProducer()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        assertEquals("C0121DF9BF5BC02656691CD16B62BE0321E4FCCF5BB95B54059071D2FB8ECE47",
                sha256(C_PRODUCERS));
        String producers = Files.readString(C_PRODUCERS).replace("\r\n", "\n");
        int duel = producers.indexOf("\"name\": \"C_DUEL\"");
        int next = producers.indexOf("\"opcode\": \"0x75\"", duel);
        assertTrue(duel >= 0 && next > duel);
        String section = producers.substring(duel, next);
        assertTrue(section.contains("\"native_producer\": \"not_found\""));
        assertTrue(section.contains("\"producers\": []"));
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(Files.readAllBytes(path)));
    }
}

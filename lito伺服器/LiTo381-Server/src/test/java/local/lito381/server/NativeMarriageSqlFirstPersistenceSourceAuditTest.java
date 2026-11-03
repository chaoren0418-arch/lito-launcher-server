package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class NativeMarriageSqlFirstPersistenceSourceAuditTest {
    @Test
    void nativePartnerIdAndLitoSqlFirstOrderingRemainConnected() throws Exception {
        String original = Files.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/clientpackets/C_Attr.java"),
                StandardCharsets.UTF_8);
        assertTrue(original.contains("setPartnerId"));
        assertTrue(original.contains("pc.save()"));
        assertTrue(original.contains("partner.save()"));

        String accounts = Files.readString(Path.of(
                "src/main/java/local/lito381/server/Accounts.java"),
                StandardCharsets.UTF_8);
        int legacy = accounts.indexOf("final class LegacyMySqlRepository");
        int persist = accounts.indexOf("persistMarriage(", legacy);
        int lock = accounts.indexOf("LOCK TABLES characters WRITE", persist);
        int first = accounts.indexOf("updateMarriagePartner(connection, firstCharacterObjectId",
                lock);
        int second = accounts.indexOf("updateMarriagePartner(connection, secondCharacterObjectId",
                first);
        int compensate = accounts.indexOf("requireMarriageCompensation(", second);
        assertTrue(legacy >= 0 && persist > legacy && lock > persist
                && first > lock && second > first && compensate > second);
        assertTrue(accounts.contains(
                "SELECT objid,char_name,COALESCE(PartnerID,0) AS partner_id"));

        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int sqlMarriage = session.indexOf(
                "selectedSourceAccounts().persistMarriage(");
        int mirrorMarriage = session.indexOf(
                "state.reconcileSqlMarriage(proposer.sourceCharacterIdentity()",
                sqlMarriage);
        int marriageSuccess = session.indexOf("sendMarriageSuccess(proposer)",
                mirrorMarriage);
        int sqlDivorce = session.indexOf("selectedSourceAccounts().persistDivorce(");
        int mirrorDivorce = session.indexOf(
                "state.reconcileSqlMarriage(stateCharacter(), \"\")", sqlDivorce);
        int divorcePacket = session.indexOf("Protocol.messageCode(662)", mirrorDivorce);
        assertTrue(sqlMarriage >= 0 && mirrorMarriage > sqlMarriage
                && marriageSuccess > mirrorMarriage);
        assertTrue(sqlDivorce >= 0 && mirrorDivorce > sqlDivorce
                && divorcePacket > mirrorDivorce);
        assertTrue(session.contains("result=cross-source-marriage"));
    }
}

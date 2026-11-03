package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks the observed-war-end persistence order to the selected server fork. */
final class NativeClassicCastleWarPersistenceSourceAuditTest {
    @Test
    void sourceOrderAndSqlGatedCastlePersistenceRemainConnected() throws Exception {
        String source = Files.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/timecontroller/server/ServerWarExecutor.java"),
                StandardCharsets.UTF_8);
        int endBranch = source.indexOf("} else if (this._war_end_time[i].before(now))");
        int endPacket = source.indexOf("S_PacketBoxWar.MSG_WAR_END", endBranch);
        int advanceStart = source.indexOf("this._war_start_time[i].add(", endPacket);
        int advanceEnd = source.indexOf("this._war_end_time[i].add(", advanceStart);
        int taxReset = source.indexOf("setTaxRate(10)", advanceEnd);
        int moneyReset = source.indexOf("setPublicMoney(0)", taxReset);
        int update = source.indexOf("updateCastle(this._l1castle[i])", moneyReset);
        assertTrue(endBranch >= 0 && endPacket > endBranch
                && advanceStart > endPacket && advanceEnd > advanceStart
                && taxReset > advanceEnd && moneyReset > taxReset && update > moneyReset);

        String persistence = Files.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/model/CastlePersistenceService.java"),
                StandardCharsets.UTF_8);
        assertTrue(persistence.contains("`war_time`=?"));
        assertTrue(persistence.contains("`tax_rate`=?"));
        assertTrue(persistence.contains("`public_money`=?"));

        String accounts = Files.readString(Path.of(
                "src/main/java/local/lito381/server/Accounts.java"),
                StandardCharsets.UTF_8);
        int legacy = accounts.indexOf("final class LegacyMySqlRepository");
        assertTrue(legacy >= 0);
        String legacyRepository = accounts.substring(legacy);
        assertTrue(legacyRepository.contains("castleWarSchedules()"));
        assertTrue(legacyRepository.contains("selected legacy Lineage source is an immutable input"));
        assertTrue(legacyRepository.contains("persistCastleState("));
        assertTrue(legacyRepository.contains("repositoryMode.equals(\"lito_mysql\")"));
        assertTrue(legacyRepository.contains("UPDATE `server_城堡` SET tax_rate=?,public_money=?"));
        assertTrue(legacyRepository.contains("persistCastleWarEnd("));
        assertTrue(legacyRepository.contains("war_time=FROM_UNIXTIME(?)"));
        assertTrue(legacyRepository.contains("persistCastleOwnership("));
        assertTrue(legacyRepository.contains("UPDATE clan_data SET hascastle=0"));
        assertTrue(legacyRepository.contains("UPDATE clan_data SET hascastle=?"));
        assertTrue(legacyRepository.contains("ownerClan.leaderObjectId()"));
        assertTrue(legacyRepository.contains("ownerClan.castleId() != current.castleId()"));

        String state = Files.readString(Path.of(
                "src/main/java/local/lito381/server/GameStateStore.java"),
                StandardCharsets.UTF_8);
        int apply = state.indexOf("synchronized CastleState applyCastleWarEnd(");
        int nextMethod = state.indexOf("synchronized boolean saveInventoryAndCastle(", apply);
        assertTrue(apply >= 0 && nextMethod > apply);
        String endMutation = state.substring(apply, nextMethod);
        int setTax = endMutation.indexOf("castle.taxRate = 10;");
        int setMoney = endMutation.indexOf("castle.publicMoney = 0;");
        int setTime = endMutation.indexOf("castle.warTimeMillis = nextWarTimeMillis;");
        int save = endMutation.indexOf("save();", setTime);
        assertTrue(setTax >= 0 && setMoney > setTax && setTime > setMoney && save > setTime);
        assertTrue(state.contains("effectiveCastleWarSchedule("));
        assertTrue(state.contains("prefix + \"warTimeMillis\""));

        String hub = Files.readString(Path.of(
                "src/main/java/local/lito381/server/GameHub.java"),
                StandardCharsets.UTF_8);
        assertTrue(hub.contains("state.effectiveCastleWarSchedule("));
        assertTrue(hub.contains("state.applyCastleWarEnd("));
        assertTrue(hub.indexOf(
                "deliverClassicCastleWarEvent(source, event, repaired, recipients)")
                < hub.indexOf("state.applyCastleWarEnd("));

        String main = Files.readString(Path.of(
                "src/main/java/local/lito381/server/Main.java"),
                StandardCharsets.UTF_8);
        assertTrue(main.contains("GameHub hub = new GameHub(config, state, accounts)"));
    }
}

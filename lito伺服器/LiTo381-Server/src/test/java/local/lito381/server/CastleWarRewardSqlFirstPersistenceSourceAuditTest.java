package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks LiTo's durable castle reward producer to the retained 3.80 semantics. */
final class CastleWarRewardSqlFirstPersistenceSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/src/com/lineage/server/model/CastleWarReward.java"),
                Path.of("../1-伺服器/src/com/lineage/server/timecontroller/server/ServerWarExecutor.java"));
    }

    @Test
    void retainedRewardInputsFeedAnIdempotentRouteLocalPendingRow() throws Exception {
        String war = LocalResources.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/timecontroller/server/ServerWarExecutor.java"),
                StandardCharsets.UTF_8);
        assertTrue(war.contains("CastleWarReward.onSiegeTimeEnd(castle_id)"));

        String retained = LocalResources.readString(Path.of(
                "../1-伺服器/src/com/lineage/server/model/CastleWarReward.java"),
                StandardCharsets.UTF_8);
        assertTrue(retained.contains("castle.getWarGiftItemId()"));
        assertTrue(retained.contains("castle.getWarGiftCount()"));
        assertTrue(retained.contains("getAccountNameByCharObjId"));
        assertTrue(retained.contains("REWARD_WRITES.submitReserved"));

        String repository = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/Accounts.java"),
                StandardCharsets.UTF_8);
        int warEnd = repository.indexOf("persistCastleWarEnd(",
                repository.indexOf("final class LegacyMySqlRepository"));
        int ownershipLock = repository.indexOf("clan_data READ, characters READ", warEnd);
        int rewardLock = repository.indexOf("`server_紀錄_獎勵` WRITE", ownershipLock);
        int resolve = repository.indexOf("loadCastleWarRewardTarget", rewardLock);
        int ensure = repository.indexOf("ensureCastleWarReward", resolve);
        int castleUpdate = repository.indexOf(
                "UPDATE `server_城堡` SET war_time=FROM_UNIXTIME(?)", ensure);
        assertTrue(warEnd >= 0 && ownershipLock > warEnd && rewardLock > ownershipLock
                && resolve > rewardLock && ensure > resolve && castleUpdate > ensure);
        assertTrue(repository.contains("SELECT item_id,count FROM `server_城堡`"));
        assertTrue(repository.contains("SELECT clan_id,leader_id FROM clan_data"));
        assertTrue(repository.contains("SELECT account_name FROM characters"));
        assertTrue(repository.contains("lito:castle-war:"));
        assertTrue(repository.contains("WHERE lito_event_key=?"));
        assertTrue(repository.contains("compensateCastleWarReward"));
    }
}

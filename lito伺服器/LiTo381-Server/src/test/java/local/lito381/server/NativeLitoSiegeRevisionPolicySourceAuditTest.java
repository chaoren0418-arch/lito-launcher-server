package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** Pins the 2003 launch limit, 2005 expansion, and selected production basis. */
final class NativeLitoSiegeRevisionPolicySourceAuditTest {
    private static final Path SOURCE = Path.of("..", "LiTo381-開發", "analysis",
            "native-lito-siege-revision-sources-2003-2005.json")
            .toAbsolutePath().normalize();

    @Test
    void historicalSourcesPreserveCapacityExpansionInsteadOfContradicting() throws Exception {
        assertEquals("D11A7E68D90613F31846B161866ACE6D85FDAC1128491604D0025DCD6589A2B1",
                sha256(SOURCE));
        String source = Files.readString(SOURCE, StandardCharsets.UTF_8);
        assertTrue(source.contains("77C20FB307B076BC356382DAD677E88DF"
                + "BC9FBFFE1C97329E3293F170CA11E7E"));
        assertTrue(source.contains("EED7A72D144CDC93C5B2BD1FB3E98C21"
                + "D00E123CBDE9729D37A488C73F9E8008"));
        assertEquals(30, NativeLitoSiegePolicy.INITIAL_2003_MAXIMUM_PER_FORCE);
        assertEquals(50, NativeLitoSiegePolicy.ACTIVE_2005_MAXIMUM_PER_FORCE);
        assertTrue(NativeLitoSiegePolicy.historicalCapacityExpanded());
        assertEquals("later-2005-observed-gameplay-contract",
                NativeLitoSiegePolicy.PRODUCTION_AUTHORITY);
    }

    @Test
    void laterLiveContractControlsRosterCapacityAndTwentyMinuteBattle() {
        assertEquals(15, BattleRules.SIEGE_MINIMUM_PLAYERS_PER_FORCE);
        assertEquals(50, BattleRules.SIEGE_MAXIMUM_PLAYERS_PER_FORCE);
        assertEquals(100, BattleRules.SIEGE_MAXIMUM_PLAYERS);
        assertEquals(100, NativeLitoSiegePolicy.activeMaximumTotal());
        assertEquals(1_200, BattleRules.Mode.SIEGE_WAR.defaultSeconds());
        assertTrue(GameHub.nativeRoomCapacity(100));
        assertFalse(GameHub.nativeRoomCapacity(101));

        Protocol.MatchMakingRequest siegeHighest = Protocol.matchMakingRequest(
                new byte[] {(byte) 0xE3, 8, 0x7F, 3, 3});
        assertEquals(BattleRules.Mode.SIEGE_WAR, siegeHighest.requestedMode());
        assertEquals(3, siegeHighest.participantRestriction());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.matchMakingRequest(
                        new byte[] {(byte) 0xE3, 8, 0x7F, 2, 3}));
    }

    @Test
    void randomAttackDefenceAssignmentIsSourceBackedButUnknownDetailsStayClosed() {
        assertEquals(1, NativeLitoSiegePolicy.resolveAttackerForce(-1, () -> 1));
        assertEquals(0, NativeLitoSiegePolicy.resolveAttackerForce(0, () -> 1));
        assertThrows(IllegalArgumentException.class,
                () -> NativeLitoSiegePolicy.resolveAttackerForce(-1, () -> 2));
        assertThrows(IllegalArgumentException.class,
                () -> NativeLitoSiegePolicy.resolveAttackerForce(2, () -> 0));
        assertFalse(NativeLitoSiegePolicy.participantBandCutoffsRecovered());
        assertFalse(NativeLitoSiegePolicy.timedSupplyPolicyRecovered());
        assertFalse(NativeLitoSiegePolicy.mpRecoveryCirclePolicyRecovered());
    }

    private static String sha256(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), "missing retained source: " + path);
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
}

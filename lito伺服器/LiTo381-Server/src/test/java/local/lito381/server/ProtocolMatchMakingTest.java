package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ProtocolMatchMakingTest {
    @Test void emitsTheExactNativeMatchmakingResultValues() {
        assertArrayEquals(new byte[] {(byte) 0xEB, 0},
                Protocol.litoAskMatchMakingResult(
                        Protocol.LitoMatchMakingResult.NO_TRANSITION));
        assertArrayEquals(new byte[] {(byte) 0xEB, 1},
                Protocol.litoAskMatchMakingResult(
                        Protocol.LitoMatchMakingResult.ACCEPTED));
        assertArrayEquals(new byte[] {(byte) 0xEB, 0x10},
                Protocol.litoAskMatchMakingResult(
                        Protocol.LitoMatchMakingResult.ITEMS_REQUIRED));
        assertArrayEquals(new byte[] {(byte) 0xEB, 0x11},
                Protocol.litoAskMatchMakingResult(
                        Protocol.LitoMatchMakingResult.OBSERVER_REJECTED));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoAskMatchMakingResult(null));
    }

    @Test void emitsOnlyTypedNativeQuitMatchmakingResults() {
        assertArrayEquals(new byte[] {(byte) 0xEC, 0},
                Protocol.litoQuitMatchMakingResult(
                        Protocol.LitoQuitMatchMakingResult.ACCEPTED));
        assertArrayEquals(new byte[] {(byte) 0xEC, 1},
                Protocol.litoQuitMatchMakingResult(
                        Protocol.LitoQuitMatchMakingResult.NO_TRANSITION));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoQuitMatchMakingResult(null));
    }

    @Test void parsesTheExactNativeC_E3Layout() {
        Protocol.MatchMakingRequest request = Protocol.matchMakingRequest(
                new byte[] {(byte) 0xE3, 8, 1, 2, 1});
        assertEquals(8, request.fixedSelector());
        assertEquals(1, request.channelRestriction());
        assertEquals(2, request.matchMethod());
        assertEquals(1, request.participantRestriction());
        assertEquals(true, request.hasChannelRestriction());
        assertEquals(BattleRules.Mode.QUEST_BATTLE, request.requestedMode());
    }

    @Test void mapsTheNativeMatchmakingOrderToIndependentRoomWireModes() {
        BattleRules.Mode[] expected = {
                BattleRules.Mode.PET_RACE,
                BattleRules.Mode.DEATHMATCH,
                BattleRules.Mode.QUEST_BATTLE,
                BattleRules.Mode.SIEGE_WAR,
                BattleRules.Mode.TIME_ATTACK,
                BattleRules.Mode.HONOR_MATCH
        };
        for (int method = 0; method < expected.length; method++) {
            Protocol.MatchMakingRequest request = Protocol.matchMakingRequest(
                    new byte[] {(byte) 0xE3, 8, 0x7F, (byte) method, 0});
            assertEquals(expected[method], request.requestedMode());
        }
    }

    @Test void rejectsTheFormerSyntheticSelectorAndTrailingBytes() {
        assertThrows(IllegalArgumentException.class, () -> Protocol.matchMakingRequest(
                new byte[] {(byte) 0xE3, 0, 0, 0, 0}));
        assertThrows(IllegalArgumentException.class, () -> Protocol.matchMakingRequest(
                new byte[] {(byte) 0xE3, 8, 0, 0, 0, 1}));
    }

    @Test void enforcesTheNativeUiSelectionDomainsAndNoneSentinel() {
        Protocol.MatchMakingRequest maximum = Protocol.matchMakingRequest(
                new byte[] {(byte) 0xE3, 8, 0x7F, 5, 2});
        assertEquals(0x7F, maximum.channelRestriction());
        assertEquals(false, maximum.hasChannelRestriction());
        Protocol.MatchMakingRequest conditionalFourthValue = Protocol.matchMakingRequest(
                new byte[] {(byte) 0xE3, 8, 0, 3, 3});
        assertEquals(3, conditionalFourthValue.participantRestriction());

        assertThrows(IllegalArgumentException.class, () -> Protocol.matchMakingRequest(
                new byte[] {(byte) 0xE3, 8, (byte) 0x80, 0, 0}));
        assertThrows(IllegalArgumentException.class, () -> Protocol.matchMakingRequest(
                new byte[] {(byte) 0xE3, 8, 5, 0, 0}));
        assertThrows(IllegalArgumentException.class, () -> Protocol.matchMakingRequest(
                new byte[] {(byte) 0xE3, 8, 0x7E, 0, 0}));
        assertThrows(IllegalArgumentException.class, () -> Protocol.matchMakingRequest(
                new byte[] {(byte) 0xE3, 8, 0, 6, 0}));
        assertThrows(IllegalArgumentException.class, () -> Protocol.matchMakingRequest(
                new byte[] {(byte) 0xE3, 8, 0, 2, 3}));
        assertThrows(IllegalArgumentException.class, () -> Protocol.matchMakingRequest(
                new byte[] {(byte) 0xE3, 8, 0, 3, 4}));
    }
}

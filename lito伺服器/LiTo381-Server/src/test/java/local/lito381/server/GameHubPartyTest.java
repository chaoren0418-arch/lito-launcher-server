package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class GameHubPartyTest {
    @Test
    void createsJoinsAndCapacityBoundsAuthoritativeParty() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            assertFalse(hub.classicPartyData("Leader").inParty());
            GameHub.ClassicPartyJoinResult first =
                    hub.joinClassicParty("Leader", "Member1", 8);
            assertEquals(GameHub.ClassicPartyJoinStatus.JOINED, first.status());
            assertEquals("Leader", first.party().leader());
            assertEquals(List.of("Leader", "Member1"), first.party().members());
            assertEquals(GameHub.ClassicPartyJoinStatus.ALREADY_MEMBER,
                    hub.joinClassicParty("Leader", "member1", 8).status());
            assertEquals(GameHub.ClassicPartyJoinStatus.NOT_LEADER,
                    hub.joinClassicParty("Member1", "Other", 8).status());

            for (int index = 2; index <= 7; index++) {
                assertEquals(GameHub.ClassicPartyJoinStatus.JOINED,
                        hub.joinClassicParty("Leader", "Member" + index, 8).status());
            }
            assertEquals(8, hub.classicPartyData("Member7").members().size());
            assertEquals(GameHub.ClassicPartyJoinStatus.FULL,
                    hub.joinClassicParty("Leader", "Member8", 8).status());
        }
    }

    @Test
    void leavePreservesOrDisbandsPartyByNativeRules() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            hub.joinClassicParty("Leader", "Member", 8);
            GameHub.ClassicPartyRemoveResult twoMemberLeave =
                    hub.leaveClassicParty("Member");
            assertEquals(GameHub.ClassicPartyRemoveStatus.LEFT_AND_DISBANDED,
                    twoMemberLeave.status());
            assertFalse(hub.classicPartyData("Leader").inParty());

            hub.joinClassicParty("Leader", "Member", 8);
            hub.joinClassicParty("Leader", "Third", 8);
            assertEquals(GameHub.ClassicPartyRemoveStatus.LEFT,
                    hub.leaveClassicParty("Member").status());
            assertEquals(List.of("Leader", "Third"),
                    hub.classicPartyData("Third").members());
            assertEquals(GameHub.ClassicPartyRemoveStatus.LEADER_DISBANDED,
                    hub.leaveClassicParty("Leader").status());
            assertFalse(hub.classicPartyData("Third").inParty());
        }
    }

    @Test
    void onlyLeaderCanBanishAndTwoMemberKickDisbands() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            assertEquals(GameHub.ClassicPartyRemoveStatus.NOT_MEMBER,
                    hub.kickClassicParty("Leader", "Member").status());
            hub.joinClassicParty("Leader", "Member", 8);
            hub.joinClassicParty("Leader", "Third", 8);
            assertEquals(GameHub.ClassicPartyRemoveStatus.NOT_LEADER,
                    hub.kickClassicParty("Member", "Third").status());
            assertEquals(GameHub.ClassicPartyRemoveStatus.TARGET_NOT_FOUND,
                    hub.kickClassicParty("Leader", "Missing").status());
            assertEquals(GameHub.ClassicPartyRemoveStatus.KICKED,
                    hub.kickClassicParty("Leader", "Member").status());
            assertFalse(hub.classicPartyData("Member").inParty());
            assertTrue(hub.classicPartyData("Third").inParty());
            assertEquals(GameHub.ClassicPartyRemoveStatus.KICKED_AND_DISBANDED,
                    hub.kickClassicParty("Leader", "Third").status());
            assertFalse(hub.classicPartyData("Leader").inParty());
        }
    }

    @Test
    void partyKeysAreSourceQualifiedWhileWireNamesStayUnqualified() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            String leader = GameStateStore.sourceCharacter("Sun", "Leader");
            String sunSame = GameStateStore.sourceCharacter("Sun", "SameHero");
            String moonSame = GameStateStore.sourceCharacter("Moon", "SameHero");
            GameHub.ClassicPartyJoinResult joined =
                    hub.joinClassicParty(leader, sunSame, 8);
            assertEquals(GameHub.ClassicPartyJoinStatus.JOINED, joined.status());
            assertEquals(List.of("Leader", "SameHero"), joined.party().members());
            assertEquals(List.of(leader.toLowerCase(), sunSame.toLowerCase()),
                    joined.party().memberIdentities());

            // Banish is name-only on the native wire, so a second identical
            // display name cannot safely join the same party.
            assertEquals(GameHub.ClassicPartyJoinStatus.INVALID,
                    hub.joinClassicParty(leader, moonSame, 8).status());
            GameHub.ClassicPartyRemoveResult removed =
                    hub.kickClassicParty(leader, "SameHero");
            assertEquals(GameHub.ClassicPartyRemoveStatus.KICKED_AND_DISBANDED,
                    removed.status());
            assertEquals(sunSame.toLowerCase(), removed.removedIdentity());

            String sunLeader = GameStateStore.sourceCharacter("Sun", "TwinLeader");
            String moonLeader = GameStateStore.sourceCharacter("Moon", "TwinLeader");
            assertEquals(GameHub.ClassicPartyJoinStatus.JOINED,
                    hub.joinClassicParty(sunLeader,
                            GameStateStore.sourceCharacter("Sun", "SunMember"), 8).status());
            assertEquals(GameHub.ClassicPartyJoinStatus.JOINED,
                    hub.joinClassicParty(moonLeader,
                            GameStateStore.sourceCharacter("Moon", "MoonMember"), 8).status());
        }
    }
}

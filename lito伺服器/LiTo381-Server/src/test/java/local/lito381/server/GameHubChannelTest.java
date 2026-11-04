package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class GameHubChannelTest {
    @Test void exposesFourTargetBinTournamentChannelsAndPaintedLevelBands() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            assertEquals(List.of("DeathKnight", "Baphomet", "Succubus", "Ogre"),
                    java.util.stream.IntStream.range(0, GameHub.CHANNEL_COUNT)
                            .mapToObj(id -> hub.channelSnapshot(id).name())
                            .toList());
            assertEquals(100, hub.channelSnapshot(0).capacity());
            assertEquals("DeathKnight", GameHub.recommendedChannel(52).name());
            assertEquals("Baphomet", GameHub.recommendedChannel(48).name());
            assertEquals("Succubus", GameHub.recommendedChannel(40).name());
            assertEquals("Ogre", GameHub.recommendedChannel(15).name());
            assertEquals(null, GameHub.recommendedChannel(14));
            assertEquals(0, GameHub.characterRankingSelector(52));
            assertEquals(1, GameHub.characterRankingSelector(48));
            assertEquals(2, GameHub.characterRankingSelector(40));
            assertEquals(3, GameHub.characterRankingSelector(15));
            assertEquals(4, GameHub.characterRankingSelector(14));
        }
    }

    @Test void assignsAndListsRoomsInsideTheirOwningChannel() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub.RoomSettings settings = new GameHub.RoomSettings(
                "Baphomet Arena", "", 4, 0, 0, 0, 0, 0, List.of());
        try (GameHub hub = new GameHub(config)) {
            long baphometRoom = hub.createRoom(1, settings);

            assertTrue(hub.roomExists(1, baphometRoom));
            assertFalse(hub.roomExists(0, baphometRoom));
            assertEquals(List.of("Local Lobby", "Baphomet Arena"),
                    hub.roomSnapshots(1).stream()
                    .map(room -> room.settings().name()).toList());
            assertTrue(hub.roomSnapshots(0).stream()
                    .noneMatch(room -> room.id() == baphometRoom));
        }
    }

    @Test void preservesNativeRoomAttributeBitsAndPasswordBit() {
        GameHub.RoomSettings publicUpperTierRoom = new GameHub.RoomSettings(
                "Native Flags", "", 4, 0x38, 0, 0, 0, 0, 0, List.of());
        assertEquals(0x38, publicUpperTierRoom.attributes());
        assertEquals(0, publicUpperTierRoom.roomListAttributes(false));
        assertEquals(0x02, publicUpperTierRoom.roomListAttributes(true));
        assertTrue(publicUpperTierRoom.allowsObservers());
        assertTrue(publicUpperTierRoom.allowsUpperTier());
        assertTrue(publicUpperTierRoom.requiresEqualSides());

        GameHub.RoomSettings noObservers = new GameHub.RoomSettings(
                "No observers", "", 4, 0, 0, 0, 0, 0, 0, List.of());
        assertFalse(noObservers.allowsObservers());

        GameHub.RoomSettings privateRoom = new GameHub.RoomSettings(
                "Private", "secret", 4, 0, 0, 0, 0, 0);
        assertEquals(0x04, privateRoom.attributes());
        assertEquals(0x04, privateRoom.roomListAttributes(false));
        assertEquals(0x02, privateRoom.roomListAttributes(true));
    }

    @Test void decodesNativeEventClassAndOriginalServerRestrictions() {
        GameHub.RoomSettings restricted = new GameHub.RoomSettings(
                "Event", "", 8, 0, BattleRules.Mode.DEATHMATCH.wireId(),
                GameHub.RoomSettings.EVENT_CLASS_RESTRICTED
                        | GameHub.RoomSettings.EVENT_SERVER_RESTRICTED
                        | GameHub.RoomSettings.EVENT_RESULT_MAIL,
                2, 3, 0b0_1001, List.of(7, 19));
        assertEquals(7, restricted.eventFlags());
        assertEquals(2, restricted.eventType());
        assertEquals(3, restricted.winnerCount());
        assertEquals(0b0_1001, restricted.allowedClassMask());
        assertTrue(restricted.sendsResultMail());
        assertTrue(restricted.allowsCharacterClass(0));
        assertTrue(restricted.allowsCharacterClass(3));
        assertFalse(restricted.allowsCharacterClass(1));
        assertFalse(restricted.allowsCharacterClass(5));
        assertTrue(restricted.allowsSourceServer(7));
        assertFalse(restricted.allowsSourceServer(8));

        GameHub.RoomSettings unrestricted = new GameHub.RoomSettings(
                "Open", "", 8, BattleRules.Mode.DEATHMATCH.wireId(),
                0, 0, 1, GameHub.RoomSettings.ALL_FIVE_CLASS_MASK, List.of());
        assertTrue(unrestricted.allowsCharacterClass(6));
        assertTrue(unrestricted.allowsSourceServer(-1));
    }

    @Test void enforcesOfficialChannelCreationAndUpperTierJoinRules() {
        GameHub.RoomSettings closed = new GameHub.RoomSettings(
                "Closed", "", 4, 0, 0, 0, 0, 0, 0, List.of());
        GameHub.RoomSettings open = new GameHub.RoomSettings(
                "Open", "", 4, GameHub.RoomSettings.ALLOW_UPPER_TIER,
                0, 0, 0, 0, 0, List.of());

        assertTrue(GameHub.canCreateRoom(0, 52));
        assertFalse(GameHub.canCreateRoom(1, 52));
        assertTrue(GameHub.canCreateRoom(0, 48));
        assertTrue(GameHub.canCreateRoom(1, 48));
        assertFalse(GameHub.canCreateRoom(2, 48));
        assertTrue(GameHub.canCreateRoom(3, 15));
        assertFalse(GameHub.canCreateRoom(0, 14));

        assertTrue(GameHub.canJoinRoom(0, 52, closed));
        assertFalse(GameHub.canJoinRoom(1, 52, closed));
        assertTrue(GameHub.canJoinRoom(1, 52, open));
        assertTrue(GameHub.canJoinRoom(0, 15, closed));
        assertTrue(GameHub.canJoinRoom(3, 15, closed));
    }

    @Test void matchmakingSkipsProtectedRoomsAndSelectsAnEligiblePublicRoom() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            ClientSession occupant = session(config, hub);
            hub.connect(occupant);
            hub.enterChannel(0, occupant);
            long protectedRoom = hub.createRoom(0, new GameHub.RoomSettings(
                    "Protected", "secret", 4, 0, 0, 0, 0, 0));
            hub.joinRoom(protectedRoom, occupant);
            long publicRoom = hub.createRoom(0, new GameHub.RoomSettings(
                    "Public", "", 4, 0, 0, 0, 0, 0));

            assertEquals(publicRoom, hub.matchRoom(0, 52, false));
        }
    }

    @Test void matchmakingRejectsWhenOnlyProtectedOrFullRoomsExist() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            hub.createRoom(0, new GameHub.RoomSettings(
                    "Protected", "secret", 4, 0, 0, 0, 0, 0));
            assertEquals(0, hub.matchRoom(0, 52, false));
        }
        try (GameHub hub = new GameHub(config)) {
            ClientSession occupant = session(config, hub);
            hub.connect(occupant);
            hub.enterChannel(0, occupant);
            long fullRoom = hub.createRoom(0, new GameHub.RoomSettings(
                    "Full", "", 1, 0, 0, 0, 0, 0));
            hub.joinRoom(fullRoom, occupant);
            assertEquals(0, hub.matchRoom(0, 52, false));
        }
    }

    @Test void matchmakingNeverSelectsTheServerGatedRentalTownMode() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            long town = hub.createRoom(0, new GameHub.RoomSettings(
                    "Rental Town", "", 100, 0, 6, 0, 0, 0, 0,
                    List.of(500, 560)));
            assertTrue(town > 0);
            assertEquals(0, hub.matchRoom(0, 52, false));
        }
    }

    @Test void matchmakingFiltersByTheRequestedNativeBattleMode() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            long petRace = hub.createRoom(0, new GameHub.RoomSettings(
                    "Pet Race", "", 4, 0, 0, 0, 0, 0));
            ClientSession occupant = session(config, hub);
            hub.connect(occupant);
            hub.enterChannel(0, occupant);
            hub.joinRoom(petRace, occupant);
            long questBattle = hub.createRoom(0, new GameHub.RoomSettings(
                    "Quest Battle", "", 4,
                    BattleRules.Mode.QUEST_BATTLE.wireId(), 0, 0, 0, 0));

            assertEquals(questBattle, hub.matchRoom(0, 52, false,
                    BattleRules.Mode.QUEST_BATTLE));
            assertEquals(petRace, hub.matchRoom(0, 52, false,
                    BattleRules.Mode.PET_RACE));
            assertEquals(0, hub.matchRoom(0, 52, false,
                    BattleRules.Mode.HONOR_MATCH));
        }
    }

    @Test void matchmakingAppliesLevelAndObserverEligibilityBeforeSuccess() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            hub.createRoom(1, new GameHub.RoomSettings(
                    "Baphomet Closed", "", 4, 0, 0, 0, 0, 0, 0, List.of()));
            assertEquals(0, hub.matchRoom(1, 52, false));
            assertEquals(0, hub.matchRoom(1, 47, true));
        }
    }

    @Test void matchmakingChannelDropdownAllowsHigherOwnAndOnlyOneLowerTier() {
        assertTrue(GameHub.canRequestMatchMakingChannel(52, 0));
        assertTrue(GameHub.canRequestMatchMakingChannel(52, 1));
        assertFalse(GameHub.canRequestMatchMakingChannel(52, 2));
        assertTrue(GameHub.canRequestMatchMakingChannel(48, 2));
        assertFalse(GameHub.canRequestMatchMakingChannel(48, 3));
        assertTrue(GameHub.canRequestMatchMakingChannel(40, 3));
        assertFalse(GameHub.canRequestMatchMakingChannel(40, 4));
        assertTrue(GameHub.canRequestMatchMakingChannel(15, 4));
        assertTrue(GameHub.canRequestMatchMakingChannel(1, 4));
        assertTrue(GameHub.canRequestMatchMakingChannel(
                52, Protocol.MatchMakingRequest.NO_CHANNEL_RESTRICTION));
    }

    @Test void randomMatchmakingPairsOnlyExactNativeRequestsIntoAnAutomaticRoom()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            ClientSession first = session(config, hub);
            ClientSession incompatible = session(config, hub);
            ClientSession second = session(config, hub);
            for (ClientSession player : List.of(first, incompatible, second)) {
                hub.connect(player);
                hub.enterChannel(0, player);
            }
            Protocol.MatchMakingRequest quest = Protocol.matchMakingRequest(
                    new byte[] {(byte) 0xE3, 8, 0x7F, 2, 0});
            Protocol.MatchMakingRequest differentBand = Protocol.matchMakingRequest(
                    new byte[] {(byte) 0xE3, 8, 0x7F, 2, 1});
            GameStateStore.TournamentStatistics veteran =
                    new GameStateStore.TournamentStatistics(10, 8, 2, 0);
            GameStateStore.TournamentStatistics newcomer =
                    new GameStateStore.TournamentStatistics(0, 0, 0, 0);

            assertFalse(hub.enqueueMatchMaking(first, 0, quest, veteran).matched());
            assertFalse(hub.enqueueMatchMaking(
                    incompatible, 0, differentBand, newcomer).matched());
            GameHub.MatchMakingQueueResult result =
                    hub.enqueueMatchMaking(second, 0, quest, newcomer);

            assertTrue(result.matched());
            assertEquals(List.of(first, second), result.sessions());
            GameHub.RoomSnapshot room = hub.roomSnapshots(0).stream()
                    .filter(candidate -> candidate.id() == result.roomId())
                    .findFirst().orElseThrow();
            assertEquals("Auto Match", room.settings().name());
            assertEquals("", room.settings().password());
            assertEquals(2, room.settings().capacity());
            assertEquals(BattleRules.Mode.QUEST_BATTLE.wireId(), room.settings().option0());
            assertTrue(room.settings().allowsObservers());
            assertTrue(room.settings().requiresEqualSides());
            assertEquals(1, hub.queuedMatchMakingCount());
            hub.cancelMatchMaking(incompatible);
            assertEquals(0, hub.queuedMatchMakingCount());
        }
    }

    @Test void randomTimeAttackCanAllocateItsNativeSinglePlayerMinimumImmediately()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            ClientSession player = session(config, hub);
            hub.connect(player);
            hub.enterChannel(0, player);
            Protocol.MatchMakingRequest request = Protocol.matchMakingRequest(
                    new byte[] {(byte) 0xE3, 8, 0x7F, 4, 0});

            GameHub.MatchMakingQueueResult result = hub.enqueueMatchMaking(player, 0,
                    request, new GameStateStore.TournamentStatistics(0, 0, 0, 0));

            assertTrue(result.matched());
            assertEquals(List.of(player), result.sessions());
            GameHub.RoomSnapshot room = hub.roomSnapshots(0).stream()
                    .filter(candidate -> candidate.id() == result.roomId())
                    .findFirst().orElseThrow();
            assertEquals(2, room.settings().capacity());
            assertEquals(BattleRules.Mode.TIME_ATTACK.wireId(), room.settings().option0());
            assertEquals(0, hub.queuedMatchMakingCount());
        }
    }

    @Test void matchmakingWinRateDistanceUsesThePersistedWinsAndLossesDenominator() {
        GameStateStore.TournamentStatistics perfect =
                new GameStateStore.TournamentStatistics(4, 4, 0, 0);
        GameStateStore.TournamentStatistics half =
                new GameStateStore.TournamentStatistics(10, 5, 5, 1);
        GameStateStore.TournamentStatistics noHistory =
                new GameStateStore.TournamentStatistics(0, 0, 0, 0);

        assertEquals(0.5, GameHub.winRateDistance(perfect, half));
        assertEquals(1.0, GameHub.winRateDistance(perfect, noHistory));
        assertEquals(0.0, GameHub.winRateDistance(noHistory, noHistory));
    }

    @Test void matchmakingQueueIsClearedByLeaveDisconnectAndExplicitCancellation()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            List<Protocol.MatchMakingRequest> requests = List.of(
                    Protocol.matchMakingRequest(
                            new byte[] {(byte) 0xE3, 8, 0x7F, 2, 0}),
                    Protocol.matchMakingRequest(
                            new byte[] {(byte) 0xE3, 8, 0x7F, 2, 1}),
                    Protocol.matchMakingRequest(
                            new byte[] {(byte) 0xE3, 8, 0x7F, 2, 2}));
            GameStateStore.TournamentStatistics noHistory =
                    new GameStateStore.TournamentStatistics(0, 0, 0, 0);
            ClientSession leaving = session(config, hub);
            ClientSession disconnecting = session(config, hub);
            ClientSession cancelling = session(config, hub);
            List<ClientSession> players = List.of(leaving, disconnecting, cancelling);
            for (int index = 0; index < players.size(); index++) {
                ClientSession player = players.get(index);
                hub.connect(player);
                hub.enterChannel(0, player);
                assertFalse(hub.enqueueMatchMaking(
                        player, 0, requests.get(index), noHistory).matched());
            }
            assertEquals(3, hub.queuedMatchMakingCount());

            hub.leaveChannel(0, leaving);
            assertEquals(2, hub.queuedMatchMakingCount());
            hub.disconnect(disconnecting);
            assertEquals(1, hub.queuedMatchMakingCount());
            hub.cancelMatchMaking(cancelling);
            assertEquals(0, hub.queuedMatchMakingCount());
        }
    }

    @Test void roomSeatClaimIsAtomicAtTheCapacityBoundary() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            ClientSession first = session(config, hub);
            ClientSession second = session(config, hub);
            hub.connect(first);
            hub.connect(second);
            hub.enterChannel(0, first);
            hub.enterChannel(0, second);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "One Seat", "", 1, 0, 0, 0, 0, 0));

            assertTrue(hub.tryJoinRoom(roomId, first).joined());
            assertFalse(hub.tryJoinRoom(roomId, second).joined());
            assertEquals(List.of(first), hub.roomMembers(roomId));
        }
    }

    @Test void eventRoomAdmissionReturnsNativeClassAndServerFailuresAtomically()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub.RoomSettings restricted = new GameHub.RoomSettings(
                "Restricted Event", "", 8, 0, BattleRules.Mode.DEATHMATCH.wireId(),
                GameHub.RoomSettings.EVENT_CLASS_RESTRICTED
                        | GameHub.RoomSettings.EVENT_SERVER_RESTRICTED,
                0, 1, 1 << 3, List.of(7));
        try (GameHub hub = new GameHub(config)) {
            ClientSession wrongClass = session(config, hub);
            ClientSession wrongServer = session(config, hub);
            ClientSession admitted = session(config, hub);
            for (ClientSession player : List.of(wrongClass, wrongServer, admitted)) {
                hub.connect(player);
                hub.enterChannel(0, player);
            }
            long roomId = hub.createRoom(0, restricted);

            GameHub.RoomJoinOutcome classFailure =
                    hub.tryJoinRoom(roomId, wrongClass, 1, 7);
            assertFalse(classFailure.joined());
            assertEquals(GameHub.RoomJoinFailure.CLASS_RESTRICTED, classFailure.failure());

            GameHub.RoomJoinOutcome serverFailure =
                    hub.tryJoinRoom(roomId, wrongServer, 3, 8);
            assertFalse(serverFailure.joined());
            assertEquals(GameHub.RoomJoinFailure.SERVER_RESTRICTED, serverFailure.failure());

            GameHub.RoomJoinOutcome success = hub.tryJoinRoom(roomId, admitted, 3, 7);
            assertTrue(success.joined());
            assertEquals(List.of(admitted), hub.roomMembers(roomId));
        }
    }

    @Test void ordinaryClanDoesNotRestrictCompactRoomButEventTeamDoes() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            ClientSession member = session(config, hub);
            hub.connect(member);
            hub.enterChannel(0, member);
            long ordinaryRoom = hub.createRoom(0, new GameHub.RoomSettings(
                    "Open Arena", "", 8, 0, 0, 0, 0, 0));
            assertTrue(hub.tryJoinRoom(
                    ordinaryRoom, member, 0, 7, "Blue Dragons").joined());
            hub.leaveRoom(ordinaryRoom, member);

            long teamRoom = hub.createRoom(0, new GameHub.RoomSettings(
                    "Blue Dragons", "", 8, 0, 0,
                    GameHub.RoomSettings.EVENT_RESULT_MAIL, 1, 1, 0, List.of()));
            assertTrue(hub.tryJoinRoom(
                    teamRoom, member, 0, 7, "blue dragons").joined());
            hub.leaveRoom(teamRoom, member);
            long wrongRoom = hub.createRoom(0, new GameHub.RoomSettings(
                    "Wrong Event", "", 8, 0, 0,
                    GameHub.RoomSettings.EVENT_RESULT_MAIL, 1, 1, 0, List.of()));

            GameHub.RoomJoinOutcome denied = hub.tryJoinRoom(
                    wrongRoom, member, 0, 7, "Blue Dragons");
            assertFalse(denied.joined());
            assertEquals(GameHub.RoomJoinFailure.TEAM_ROOM_ONLY, denied.failure());
        }
    }

    @Test void kickedParticipantCannotReenterUntilTheRoomLifecycleEnds()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            ClientSession owner = session(config, hub);
            ClientSession kicked = session(config, hub);
            ClientSession other = session(config, hub);
            for (ClientSession player : List.of(owner, kicked, other)) {
                hub.connect(player);
                hub.enterChannel(0, player);
            }
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Kick Lifecycle", "", 8, 0, 0, 0, 0, 0));
            assertTrue(hub.tryJoinRoom(roomId, owner).joined());
            assertTrue(hub.tryJoinRoom(roomId, kicked).joined());
            assertTrue(hub.markRoomKick(roomId, owner, kicked));
            assertFalse(hub.leaveRoom(roomId, kicked));

            GameHub.RoomJoinOutcome denied = hub.tryJoinRoom(roomId, kicked);
            assertFalse(denied.joined());
            assertEquals(GameHub.RoomJoinFailure.KICKED, denied.failure());
            assertTrue(hub.tryJoinRoom(roomId, other).joined());

            hub.leaveRoom(roomId, owner);
            hub.leaveRoom(roomId, other);
            long nextRoomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Next Lifecycle", "", 8, 0, 0, 0, 0, 0));
            assertNotEquals(roomId, nextRoomId);
            assertTrue(hub.tryJoinRoom(nextRoomId, kicked).joined());
        }
    }

    @Test void endedBattleRejectsNewJoinsUntilEveryResultMemberLeaves()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            ClientSession first = session(config, hub);
            ClientSession second = session(config, hub);
            ClientSession late = session(config, hub);
            for (ClientSession player : List.of(first, second, late)) {
                hub.connect(player);
                hub.enterChannel(0, player);
            }
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Result Lifecycle", "", 8, 0, 0, 0, 0, 0));
            assertTrue(hub.tryJoinRoom(roomId, first).joined());
            assertTrue(hub.tryJoinRoom(roomId, second).joined());
            hub.endBattle(roomId);

            GameHub.RoomJoinOutcome denied = hub.tryJoinRoom(roomId, late);
            assertFalse(denied.joined());
            assertEquals(GameHub.RoomJoinFailure.BATTLE_ENDED, denied.failure());

            hub.leaveRoom(roomId, first);
            assertEquals(GameHub.RoomJoinFailure.BATTLE_ENDED,
                    hub.tryJoinRoom(roomId, late).failure());
            hub.leaveRoom(roomId, second);
            long nextRoomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Next Result Lifecycle", "", 8, 0, 0, 0, 0, 0));
            assertNotEquals(roomId, nextRoomId);
            assertTrue(hub.tryJoinRoom(nextRoomId, late).joined());
        }
    }

    @Test void nativePetRaceRequiresTwoReadyRunnersAndCapsAdmissionAtTen()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.pet-race-multiplayer-selftest.properties"));
        GameHub.RoomSettings race = new GameHub.RoomSettings(
                "Native Race", "", 30, BattleRules.Mode.PET_RACE.wireId(),
                0, 0, 0, 0, List.of());
        try (GameHub hub = new GameHub(config)) {
            ClientSession first = session(config, hub);
            ClientSession second = session(config, hub);
            hub.connect(first);
            hub.connect(second);
            hub.enterChannel(0, first);
            hub.enterChannel(0, second);
            long roomId = hub.createRoom(0, race);
            assertTrue(hub.tryJoinRoom(roomId, first).joined());
            assertTrue(hub.tryJoinRoom(roomId, second).joined());
            assertEquals(GameHub.ReadyTransition.NONE,
                    hub.setReady(roomId, first, true).transition());
            assertEquals(GameHub.ReadyTransition.COUNTDOWN_STARTED,
                    hub.setReady(roomId, second, true).transition());
        }

        try (GameHub hub = new GameHub(config)) {
            long roomId = hub.createRoom(0, race);
            for (int index = 0; index < BattlePetRaceCourseCatalog.MAXIMUM_PLAYERS; index++) {
                ClientSession runner = session(config, hub);
                hub.connect(runner);
                hub.enterChannel(0, runner);
                assertTrue(hub.tryJoinRoom(roomId, runner).joined(), "runner=" + index);
            }
            ClientSession eleventh = session(config, hub);
            hub.connect(eleventh);
            hub.enterChannel(0, eleventh);
            assertFalse(hub.tryJoinRoom(roomId, eleventh).joined());
        }
    }

    @Test void exposesAuthoritativeInitialRosterReadyStateForScdRebuilds()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.pet-race-multiplayer-selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            ClientSession first = session(config, hub);
            ClientSession second = session(config, hub);
            for (ClientSession player : List.of(first, second)) {
                hub.connect(player);
                hub.enterChannel(0, player);
            }
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Ready Roster", "", 10, BattleRules.Mode.PET_RACE.wireId(),
                    0, 0, 0, 0, List.of()));
            assertTrue(hub.tryJoinRoom(roomId, first).joined());
            assertTrue(hub.tryJoinRoom(roomId, second).joined());

            assertEquals(Protocol.LitoParticipantState.NOT_READY,
                    hub.nativeRoomParticipantReadyState(roomId, first));
            assertEquals(Protocol.LitoParticipantState.NOT_READY,
                    hub.nativeRoomParticipantReadyState(roomId, second));
            assertTrue(hub.setReady(roomId, first, true).ready());
            assertEquals(Protocol.LitoParticipantState.READY,
                    hub.nativeRoomParticipantReadyState(roomId, first));
            assertEquals(Protocol.LitoParticipantState.NOT_READY,
                    hub.nativeRoomParticipantReadyState(roomId, second));
            assertFalse(hub.setReady(roomId, first, false).ready());
            assertEquals(Protocol.LitoParticipantState.NOT_READY,
                    hub.nativeRoomParticipantReadyState(roomId, first));
        }
    }

    @Test void opcodeOnlyNativeStartIsAuthorizedOnlyForTheCurrentRoomOwner()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            ClientSession owner = session(config, hub);
            ClientSession participant = session(config, hub);
            for (ClientSession player : List.of(owner, participant)) {
                hub.connect(player);
                hub.enterChannel(0, player);
            }
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Owner Start", "", 8, BattleRules.Mode.DEATHMATCH.wireId(),
                    0, 0, 0, 0, List.of()));
            assertTrue(hub.tryJoinRoom(roomId, owner).joined());
            assertTrue(hub.tryJoinRoom(roomId, participant).joined());

            GameHub.ReadyUpdate forged = hub.requestOwnerStart(roomId, participant);
            assertFalse(forged.ready());
            assertEquals(GameHub.ReadyTransition.NONE, forged.transition());
            assertEquals(Protocol.LitoParticipantState.NOT_READY,
                    hub.nativeRoomParticipantReadyState(roomId, participant));

            assertEquals(GameHub.ReadyTransition.NONE,
                    hub.setReady(roomId, participant, true).transition());
            GameHub.ReadyUpdate started = hub.requestOwnerStart(roomId, owner);
            assertTrue(started.ready());
            assertEquals(GameHub.ReadyTransition.COUNTDOWN_STARTED, started.transition());
            assertEquals(Protocol.LitoParticipantState.READY,
                    hub.nativeRoomParticipantReadyState(roomId, owner));

            GameHub.ReadyUpdate cancelled = hub.requestOwnerStart(roomId, owner);
            assertFalse(cancelled.ready());
            assertEquals(GameHub.ReadyTransition.COUNTDOWN_CANCELLED, cancelled.transition());
        }
    }

    @Test void nativeSiegeBalancesFifteenPerForceCapsFiftyAndRevalidatesCountdown()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.pet-race-multiplayer-selftest.properties"));
        GameHub.RoomSettings siege = new GameHub.RoomSettings(
                "Native Siege", "", 100, BattleRules.Mode.SIEGE_WAR.wireId(),
                0, 0, 0, 0, List.of());

        try (GameHub hub = new GameHub(config)) {
            long roomId = hub.createRoom(0, siege);
            java.util.ArrayList<ClientSession> players = new java.util.ArrayList<>();
            for (int index = 0; index < BattleRules.SIEGE_MINIMUM_PLAYERS; index++) {
                ClientSession player = session(config, hub);
                players.add(player);
                hub.connect(player);
                hub.enterChannel(0, player);
                assertTrue(hub.tryJoinRoom(roomId, player).joined());
                assertEquals(index & 1, hub.forceOf(roomId, player));
            }
            for (int index = 0; index < players.size() - 1; index++) {
                assertEquals(GameHub.ReadyTransition.NONE,
                        hub.setReady(roomId, players.get(index), true).transition());
            }
            assertEquals(GameHub.ReadyTransition.COUNTDOWN_STARTED,
                    hub.setReady(roomId, players.get(players.size() - 1), true).transition());

            GameHub.ForceUpdate unbalanced = hub.setForce(roomId, players.get(0), 1);
            assertTrue(unbalanced.changed());
            assertEquals(GameHub.ReadyTransition.COUNTDOWN_CANCELLED,
                    unbalanced.transition());
        }

        try (GameHub hub = new GameHub(config)) {
            long roomId = hub.createRoom(0, siege);
            GameHub.ReadyUpdate last = null;
            for (int index = 0; index < BattleRules.SIEGE_MINIMUM_PLAYERS - 1; index++) {
                ClientSession player = session(config, hub);
                hub.connect(player);
                hub.enterChannel(0, player);
                assertTrue(hub.tryJoinRoom(roomId, player).joined());
                last = hub.setReady(roomId, player, true);
            }
            assertEquals(GameHub.ReadyTransition.NONE, last.transition());
            assertEquals(BattleRules.SIEGE_MINIMUM_PLAYERS_PER_FORCE,
                    last.minimumPlayersPerForceWarning());
        }

        try (GameHub hub = new GameHub(config)) {
            long roomId = hub.createRoom(0, siege);
            java.util.ArrayList<ClientSession> players = new java.util.ArrayList<>();
            for (int index = 0; index < BattleRules.SIEGE_MAXIMUM_PLAYERS; index++) {
                ClientSession player = session(config, hub);
                players.add(player);
                hub.connect(player);
                hub.enterChannel(0, player);
                assertTrue(hub.tryJoinRoom(roomId, player).joined());
            }
            GameHub.ForceUpdate rejected = hub.setForce(roomId, players.get(0), 1);
            assertFalse(rejected.changed());
            assertEquals(0, rejected.force());
            assertEquals(BattleRules.SIEGE_MAXIMUM_PLAYERS_PER_FORCE + 1,
                    rejected.firstDisallowedPlayersPerForceWarning());
        }
    }

    private static ClientSession session(ServerConfig config, GameHub hub) {
        return new ClientSession(null, ClientSession.Endpoint.GAME,
                config, null, null, LocalResources.discardCapture(), hub);
    }
}

package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameHubCastleWarTest {
    @TempDir Path temporary;

    @Test
    void registersAnExactActiveWindowPerOriginalCharacterSource() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        long start = System.currentTimeMillis() - 60_000L;
        CastleWarSchedule heine = new CastleWarSchedule(
                5, "海音城", start, 60, 10_080);
        try (GameHub hub = new GameHub(config)) {
            hub.registerClassicCastleWarSchedules("Sun", List.of(heine));

            assertTrue(hub.classicCastleWarActive("sun", 5));
            assertFalse(hub.classicCastleWarActive("Moon", 5));
            assertEquals(List.of(5), hub.activeClassicCastleWars("SUN"));
            assertEquals(start,
                    hub.classicCastleWarStartMillis("Sun", 5, 123L));
            assertEquals(123L,
                    hub.classicCastleWarStartMillis("Moon", 5, 123L));
        }
    }

    @Test
    void observedEndResetsAndPersistsTheSourceQualifiedCastleState() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        Path stateFile = temporary.resolve("hub-castle-war.properties");
        GameStateStore state = new GameStateStore(stateFile, true);
        long start = System.currentTimeMillis() - 30_000L;
        CastleWarSchedule source = new CastleWarSchedule(
                5, "海音城", start, 1, 120);
        CastleSummary legacy = new CastleSummary(
                5, "LegacyEmblem", "TestEmblem", "海音城",
                start, 40, 50_000);
        state.castle("Sun", legacy);

        try (GameHub hub = new GameHub(config, state)) {
            hub.registerClassicCastleWarSchedules("Sun", List.of(source));
            assertTrue(hub.classicCastleWarActive("Sun", 5));

            hub.classicCastleWarTick(start + 60_001L);

            assertFalse(hub.classicCastleWarActive("Sun", 5));
            long nextStart = start + 120L * 60_000L;
            assertEquals(nextStart,
                    hub.classicCastleWarStartMillis("Sun", 5, 0));
            GameStateStore.CastleState ended = state.castle("Sun", legacy);
            assertEquals(10, ended.taxRate());
            assertEquals(0, ended.publicMoney());
            assertEquals(nextStart, ended.warTimeMillis());
        }

        GameStateStore reloaded = new GameStateStore(stateFile, true);
        try (GameHub hub = new GameHub(config, reloaded)) {
            hub.registerClassicCastleWarSchedules("Sun", List.of(source));
            assertEquals(start + 120L * 60_000L,
                    hub.classicCastleWarStartMillis("Sun", 5, 0));
            assertFalse(hub.classicCastleWarActive("Sun", 5));
        }
    }

    @Test
    void ownedCastleTracksMultipleAttackersWithoutLosingWarOnInvalidClaim()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        long start = System.currentTimeMillis() - 60_000L;
        CastleWarSchedule kent = new CastleWarSchedule(
                1, "肯特城", start, 60, 10_080);
        try (GameHub hub = new GameHub(config)) {
            hub.registerClassicCastleWarSchedules("Sun", List.of(kent));

            GameHub.ClassicCastleSiegeResult first =
                    hub.mutateClassicCastleSiege(
                            "Sun", 1, "Alpha", "LegacyKent",
                            Protocol.ClanWarRequestType.DECLARE);
            GameHub.ClassicCastleSiegeResult second =
                    hub.mutateClassicCastleSiege(
                            "SUN", 1, "Beta", "LegacyKent",
                            Protocol.ClanWarRequestType.DECLARE);
            assertEquals(GameHub.ClassicCastleSiegeMutation.DECLARED,
                    first.mutation());
            assertEquals(List.of(Protocol.ClanWarNoticeType.DECLARED),
                    first.notices().stream()
                    .map(GameHub.ClassicCastleWarNotice::type).toList());
            assertEquals(GameHub.ClassicCastleSiegeMutation.DECLARED,
                    second.mutation());
            assertTrue(hub.classicCastleSiegeParticipant("sun", 1, "Alpha"));
            assertTrue(hub.classicCastleSiegeParticipant("Sun", 1, "Beta"));
            assertTrue(hub.classicCastleSiegeParticipant(
                    "Sun", 1, "LegacyKent"));
            assertFalse(hub.classicCastleSiegeParticipant("Moon", 1, "Alpha"));

            GameHub.ClassicCastleSiegeResult invalid =
                    hub.winClassicCastleSiege("Sun", 1, "Undeclared");
            assertEquals(GameHub.ClassicCastleSiegeMutation.NOT_PARTICIPANT,
                    invalid.mutation());
            assertTrue(hub.classicCastleSiegeParticipant("Sun", 1, "Alpha"));
            assertTrue(hub.classicCastleSiegeParticipant("Sun", 1, "Beta"));

            GameHub.ClassicCastleSiegeResult surrender =
                    hub.mutateClassicCastleSiege(
                            "Sun", 1, "Alpha", "LegacyKent",
                            Protocol.ClanWarRequestType.SURRENDER);
            assertEquals(GameHub.ClassicCastleSiegeMutation.SURRENDERED,
                    surrender.mutation());
            assertEquals(List.of(Protocol.ClanWarNoticeType.SURRENDERED,
                            Protocol.ClanWarNoticeType.WON),
                    surrender.notices().stream()
                    .map(GameHub.ClassicCastleWarNotice::type).toList());
            assertFalse(hub.classicCastleSiegeParticipant("Sun", 1, "Alpha"));
            assertTrue(hub.classicCastleSiegeParticipant("Sun", 1, "Beta"));

            GameHub.ClassicCastleSiegeResult won =
                    hub.winClassicCastleSiege("Sun", 1, "Beta");
            assertEquals(List.of(Protocol.ClanWarNoticeType.WON,
                            Protocol.ClanWarNoticeType.ENDED),
                    won.notices().stream()
                    .map(GameHub.ClassicCastleWarNotice::type).toList());
            assertFalse(hub.classicCastleSiegeParticipant("Sun", 1, "Beta"));
        }
    }

    @Test
    void attackingClanOwnerDeathCeasesOnlyThatCastleParticipation()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        long start = System.currentTimeMillis() - 60_000L;
        CastleWarSchedule kent = new CastleWarSchedule(
                1, "肯特城", start, 60, 10_080);
        try (GameHub hub = new GameHub(config)) {
            hub.registerClassicCastleWarSchedules("Sun", List.of(kent));
            assertTrue(hub.createClan(
                    GameStateStore.sourceCharacter("Sun", "AlphaCrown"), "Alpha"));
            assertTrue(hub.createClan(
                    GameStateStore.sourceCharacter("Sun", "BetaCrown"), "Beta"));
            assertEquals(GameHub.ClassicCastleSiegeMutation.DECLARED,
                    hub.mutateClassicCastleSiege(
                            "Sun", 1, "Alpha", "LegacyKent",
                            Protocol.ClanWarRequestType.DECLARE).mutation());
            assertEquals(GameHub.ClassicCastleSiegeMutation.DECLARED,
                    hub.mutateClassicCastleSiege(
                            "Sun", 1, "Beta", "LegacyKent",
                            Protocol.ClanWarRequestType.DECLARE).mutation());

            GameHub.ClassicCastleSiegeResult result =
                    hub.ceaseClassicCastleSiegeForAttackingLeaderDeath(
                            "Sun", GameStateStore.sourceCharacter(
                                    "Sun", "AlphaCrown"));

            assertEquals(GameHub.ClassicCastleSiegeMutation.CEASED,
                    result.mutation());
            assertEquals(1, result.castleId());
            assertEquals(List.of(Protocol.ClanWarNoticeType.ENDED),
                    result.notices().stream()
                            .map(GameHub.ClassicCastleWarNotice::type).toList());
            assertEquals("Alpha", result.notices().get(0).clanOne());
            assertEquals("LegacyKent", result.notices().get(0).clanTwo());
            assertFalse(hub.classicCastleSiegeParticipant("Sun", 1, "Alpha"));
            assertTrue(hub.classicCastleSiegeParticipant("Sun", 1, "Beta"));
        }
    }

    @Test
    void unownedCastleDeclarationIsWireOnlyAndOpenCrownClaimPersists()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        Path stateFile = temporary.resolve("unowned-crown.properties");
        GameStateStore state = new GameStateStore(stateFile, true);
        long start = System.currentTimeMillis() - 60_000L;
        CastleWarSchedule kent = new CastleWarSchedule(
                1, "肯特城", start, 60, 10_080);
        try (GameHub hub = new GameHub(config, state)) {
            hub.registerClassicCastleWarSchedules("Sun", List.of(kent));
            GameHub.ClassicCastleSiegeResult declaration =
                    hub.mutateClassicCastleSiege(
                            "Sun", 1, "Alpha", "NPC",
                            Protocol.ClanWarRequestType.DECLARE);
            assertEquals(GameHub.ClassicCastleSiegeMutation.DECLARED,
                    declaration.mutation());
            assertEquals("NPC", declaration.defenderClan());
            assertEquals(List.of(Protocol.ClanWarNoticeType.DECLARED),
                    declaration.notices().stream()
                    .map(GameHub.ClassicCastleWarNotice::type).toList());
            assertFalse(hub.classicCastleSiegeParticipant("Sun", 1, "Alpha"),
                    "retained C_War does not create L1War for an unowned castle");

            ClassicWorldCastleWarField.WarObject tower =
                    hub.classicCastleWarObjects("Sun").stream()
                            .filter(object -> object.kind()
                                    == ClassicWorldCastleWarField.Kind.TOWER
                                    && object.castleId() == 1)
                            .findFirst().orElseThrow();
            ClassicWorldCastleWarField.TowerHit death =
                    hub.damageClassicCastleTower(
                            "Sun", tower.mapId(), tower.objectId(),
                            "Unannounced", "NPC", 10_000);
            assertTrue(death.diedNow());

            GameHub.ClassicCastleClaim claim = hub.claimClassicCastleCrown(
                    "Sun", death.spawnedCrown().mapId(),
                    death.spawnedCrown().objectId(),
                    "Unannounced", "NewLeader", 27);
            assertEquals(GameHub.ClassicCastleSiegeMutation.NOT_PARTICIPANT,
                    claim.siege().mutation());
            assertEquals("Unannounced", claim.ownership().clanName());
            assertEquals(1, claim.ownership().revision());
        }

        assertEquals("Unannounced", new GameStateStore(stateFile, true)
                .castleOwnership("Sun", 1).clanName());
    }
}

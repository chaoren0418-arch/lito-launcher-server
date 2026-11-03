package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class GameHubClanTest {
    @Test
    void followsNativeOwnerCandidateMemberTransitions() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            assertEquals(GameHub.ClanData.empty(), hub.clanData("TestKnight"));
            assertTrue(hub.createClan("TestKnight", "Raiders"));
            assertFalse(hub.createClan("TestKnight", "Other"));
            assertRoles(hub.clanData("TestKnight"), List.of(GameHub.CLAN_ROLE_OWNER));

            GameHub.ClanMemberSnapshot candidate =
                    hub.addClanCandidate("TestKnight", "TestMage");
            assertEquals("TestMage", candidate.name());
            assertEquals(GameHub.CLAN_ROLE_CANDIDATE, candidate.role().wireValue());
            assertNull(hub.addClanCandidate("TestKnight", "TestMage"));

            GameHub.ClanData admitted = hub.admitClanCandidate("TestKnight", "TestMage");
            assertRoles(admitted,
                    List.of(GameHub.CLAN_ROLE_OWNER, GameHub.CLAN_ROLE_MEMBER));
            assertEquals("Raiders", hub.clanData("TestMage").name());
            assertNull(hub.admitClanCandidate("TestKnight", "TestMage"));

            GameHub.ClanMemberSnapshot removed =
                    hub.removeClanMember("TestKnight", "TestMage");
            assertEquals(GameHub.CLAN_ROLE_MEMBER, removed.role().wireValue());
            assertEquals(GameHub.ClanData.empty(), hub.clanData("TestMage"));
            assertRoles(hub.clanData("TestKnight"), List.of(GameHub.CLAN_ROLE_OWNER));
        }
    }

    @Test
    void importsLegacyClanReadOnlyAndTracksDestroyUndoCommands() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            assertRoles(hub.registerOriginalClan("LegacyOwner", "Original"),
                    List.of(GameHub.CLAN_ROLE_OWNER));
            assertRoles(hub.registerOriginalClan("LegacyMember", "Original"),
                    List.of(GameHub.CLAN_ROLE_OWNER, GameHub.CLAN_ROLE_MEMBER));
            assertTrue(hub.setClanDestroyPending("LegacyOwner", true));
            assertTrue(hub.clanData("LegacyMember").destroyPending());
            assertTrue(hub.setClanDestroyPending("LegacyOwner", false));
            assertFalse(hub.clanData("LegacyOwner").destroyPending());
            assertFalse(hub.setClanDestroyPending("LegacyMember", true));
        }
    }

    @Test
    void indexesReadOnlyLegacyEmblemByPublishedIdentity() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            byte[] pixels = new byte[Protocol.EMBLEM_BYTES];
            pixels[0] = 0x21;
            hub.registerOriginalClan("LegacyOwner", "Original");
            assertTrue(hub.registerClanEmblem(
                    "Original", new ClanEmblemData(0x1020_3040L, pixels)));
            assertEquals(0x1020_3040L, hub.clanEmblemId("LegacyOwner"));
            assertArrayEquals(pixels, hub.clanEmblem(0x1020_3040L));

            byte[] copy = hub.clanEmblem(0x1020_3040L);
            copy[0] = 0;
            assertEquals(0x21, hub.clanEmblem(0x1020_3040L)[0]);

            hub.registerOriginalClan("OtherOwner", "Other");
            assertFalse(hub.registerClanEmblem(
                    "Other", new ClanEmblemData(0x1020_3040L, pixels)));
            assertEquals(0, hub.clanEmblemId("OtherOwner"));
        }
    }

    @Test
    void clanKeysAreSourceQualifiedWhileWireNamesStayUnqualified() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            String sunOwner = GameStateStore.sourceCharacter("Sun", "TwinCrown");
            String moonOwner = GameStateStore.sourceCharacter("Moon", "TwinCrown");
            String sunMember = GameStateStore.sourceCharacter("Sun", "TwinMember");
            String moonMember = GameStateStore.sourceCharacter("Moon", "TwinMember");

            assertEquals(GameHub.ClassicClanCreateStatus.CREATED,
                    hub.createClassicClan(sunOwner, "SameClan"));
            assertEquals(GameHub.ClassicClanCreateStatus.CREATED,
                    hub.createClassicClan(moonOwner, "SameClan"));
            assertEquals(GameHub.ClassicClanCreateStatus.DUPLICATE_NAME,
                    hub.createClassicClan(
                            GameStateStore.sourceCharacter("Sun", "OtherCrown"), "sameclan"));

            assertEquals(GameHub.ClassicClanJoinStatus.JOINED,
                    hub.joinClassicClan(sunOwner, sunMember, 8).status());
            assertEquals(GameHub.ClassicClanJoinStatus.JOINED,
                    hub.joinClassicClan(moonOwner, moonMember, 8).status());
            assertEquals("SameClan", hub.clanData(sunMember).name());
            assertEquals("SameClan", hub.clanData(moonMember).name());
            assertEquals(List.of("TwinCrown", "TwinMember"),
                    hub.clanData(sunOwner).members().stream()
                            .map(GameHub.ClanMemberSnapshot::name).toList());
            assertTrue(hub.sameClan(sunOwner, sunMember));
            assertTrue(hub.sameClan(moonOwner, moonMember));
            assertFalse(hub.sameClan(sunOwner, moonOwner));
            assertFalse(hub.sameClan(sunMember, moonMember));

            byte[] pixels = new byte[Protocol.EMBLEM_BYTES];
            assertTrue(hub.registerClanEmblem(
                    sunOwner, "SameClan", new ClanEmblemData(0x3344L, pixels)));
            assertFalse(hub.registerClanEmblem(
                    moonOwner, "SameClan", new ClanEmblemData(0x3344L, pixels)));
            assertEquals(0x3344L, hub.clanEmblemId(sunOwner));
            assertEquals(0, hub.clanEmblemId(moonOwner));
        }
    }

    @Test
    void sqlClanSnapshotRebuildsOfflineRosterAndKeepsDynamicSourcesIsolated()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            String sunOwner = GameStateStore.sourceCharacter("Sun", "Crown");
            String moonOwner = GameStateStore.sourceCharacter("Moon", "Crown");
            RepositoryClanSnapshot sun = new RepositoryClanSnapshot(
                    true, 40001, "SameClan", 1001, "Crown",
                    0, 0, 7, true, "Welcome", 1234, true,
                    List.of(
                            new RepositoryClanMember(
                                    1001, "Crown", 4, 10, "King", ""),
                            new RepositoryClanMember(
                                    1002, "OfflineMember", 2, 7, "Knight", "memo"),
                            new RepositoryClanMember(
                                    1003, "Candidate", 3, 8, "", "")));
            GameHub.ClanData loaded = hub.reconcileSqlClan(sunOwner, sun);
            assertEquals("SameClan", loaded.name());
            assertEquals(3, loaded.members().size());
            assertTrue(loaded.destroyPending());
            assertEquals("SameClan", hub.clanData(
                    GameStateStore.sourceCharacter("Sun", "OfflineMember")).name());
            assertEquals(GameHub.ClanData.empty(), hub.clanData(
                    GameStateStore.sourceCharacter("Sun", "Candidate")));

            RepositoryClanSnapshot moon = new RepositoryClanSnapshot(
                    true, 50001, "SameClan", 2001, "Crown",
                    0, 0, 0, false, "", 0, false,
                    List.of(new RepositoryClanMember(
                            2001, "Crown", 4, 4, "", "")));
            hub.reconcileSqlClan(moonOwner, moon);
            assertFalse(hub.sameClan(sunOwner, moonOwner));
            assertEquals(3, hub.clanData(sunOwner).members().size());
            assertEquals(1, hub.clanData(moonOwner).members().size());

            assertEquals(GameHub.ClanData.empty(),
                    hub.reconcileSqlClan(sunOwner, RepositoryClanSnapshot.empty()));
            assertEquals(GameHub.ClanData.empty(), hub.clanData(sunOwner));
            assertEquals(GameHub.ClanData.empty(), hub.clanData(
                    GameStateStore.sourceCharacter("Sun", "OfflineMember")));
            assertEquals("SameClan", hub.clanData(moonOwner).name());
        }
    }

    @Test
    void activeClanWarRequiresTwoDifferentClansInTheSameWar() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            String first = GameStateStore.sourceCharacter("Sun", "FirstCrown");
            String second = GameStateStore.sourceCharacter("Sun", "SecondCrown");
            String outsider = GameStateStore.sourceCharacter("Sun", "Outsider");
            assertEquals(GameHub.ClassicClanCreateStatus.CREATED,
                    hub.createClassicClan(first, "FirstClan"));
            assertEquals(GameHub.ClassicClanCreateStatus.CREATED,
                    hub.createClassicClan(second, "SecondClan"));
            assertEquals(GameHub.ClassicClanCreateStatus.CREATED,
                    hub.createClassicClan(outsider, "ThirdClan"));

            GameHub.ClassicClanWarResolution started = hub.resolveClassicClanWar(
                    first, second, "FirstClan", "SecondClan",
                    Protocol.ClanWarRequestType.DECLARE, true);
            assertEquals(GameHub.ClassicClanWarResolutionStatus.STARTED,
                    started.status());
            assertTrue(hub.classicClanWarActiveBetween(first, second));
            assertTrue(hub.classicClanWarActiveBetween(second, first));
            assertFalse(hub.classicClanWarActiveBetween(first, outsider));
            assertFalse(hub.classicClanWarActiveBetween(first, first));

            assertEquals(GameHub.ClassicClanWarResolutionStatus.CEASED,
                    hub.resolveClassicClanWar(
                            first, second, "FirstClan", "SecondClan",
                            Protocol.ClanWarRequestType.CEASE, true).status());
            assertFalse(hub.classicClanWarActiveBetween(first, second));
        }
    }

    @Test
    void clanOwnerDeathAtomicallyEndsWarAndPreservesFatalRelation()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            String first = GameStateStore.sourceCharacter("Sun", "FirstCrown");
            String second = GameStateStore.sourceCharacter("Sun", "SecondCrown");
            String member = GameStateStore.sourceCharacter("Sun", "FirstMember");
            String outsider = GameStateStore.sourceCharacter("Sun", "Outsider");
            assertEquals(GameHub.ClassicClanCreateStatus.CREATED,
                    hub.createClassicClan(first, "FirstClan"));
            assertEquals(GameHub.ClassicClanCreateStatus.CREATED,
                    hub.createClassicClan(second, "SecondClan"));
            assertEquals(GameHub.ClassicClanJoinStatus.JOINED,
                    hub.joinClassicClan(first, member, 8).status());

            assertEquals(GameHub.ClassicClanWarResolutionStatus.STARTED,
                    hub.resolveClassicClanWar(
                            first, second, "FirstClan", "SecondClan",
                            Protocol.ClanWarRequestType.DECLARE, true).status());
            assertFalse(hub.endClassicClanWarForLeaderDeath(member, second).ended(),
                    "only the retained clan owner ends a war on death");
            assertTrue(hub.classicClanWarActiveBetween(first, second));

            GameHub.ClassicClanWarLeaderDeathResolution sameWar =
                    hub.endClassicClanWarForLeaderDeath(first, second);
            assertTrue(sameWar.ended());
            assertTrue(sameWar.attackerSameWar());
            assertEquals(new GameHub.ClassicClanWarData(
                    "FirstClan", "SecondClan"), sameWar.war());
            assertFalse(hub.classicClanWarActiveBetween(first, second));

            assertEquals(GameHub.ClassicClanWarResolutionStatus.STARTED,
                    hub.resolveClassicClanWar(
                            first, second, "FirstClan", "SecondClan",
                            Protocol.ClanWarRequestType.DECLARE, true).status());
            GameHub.ClassicClanWarLeaderDeathResolution outsideKill =
                    hub.endClassicClanWarForLeaderDeath(first, outsider);
            assertTrue(outsideKill.ended());
            assertFalse(outsideKill.attackerSameWar());
            assertFalse(hub.classicClanWarActiveBetween(first, second));
        }
    }

    @Test
    void standardClanCreateAndQuestion97JoinAreAtomicAndCapacityBounded() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            assertEquals(GameHub.ClassicClanCreateStatus.CREATED,
                    hub.createClassicClan("Crown", "NativeClan"));
            assertEquals(GameHub.ClassicClanCreateStatus.OWNER_ALREADY_MEMBER,
                    hub.createClassicClan("Crown", "Other"));
            assertEquals(GameHub.ClassicClanCreateStatus.DUPLICATE_NAME,
                    hub.createClassicClan("OtherCrown", "nativeclan"));

            GameHub.ClassicClanJoinResult joined =
                    hub.joinClassicClan("Crown", "Applicant", 2);
            assertEquals(GameHub.ClassicClanJoinStatus.JOINED, joined.status());
            assertRoles(joined.clan(),
                    List.of(GameHub.CLAN_ROLE_OWNER, GameHub.CLAN_ROLE_MEMBER));
            assertEquals(GameHub.ClassicClanJoinStatus.FULL,
                    hub.joinClassicClan("Crown", "LateApplicant", 2).status());
            assertEquals(GameHub.ClassicClanJoinStatus.INVALID,
                    hub.joinClassicClan("Crown", "Applicant", 3).status());
        }
    }

    @Test
    void nativeRankControlKeepsOwnerRoleFourImmutableAndAssignsOneThroughThree() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            assertTrue(hub.createClan("TestKnight", "Raiders"));
            assertEquals(GameHub.CLAN_ROLE_OWNER, hub.clanRole("TestKnight"));
            assertNull(hub.changeClanRole(
                    "TestKnight", "TestKnight", GameHub.CLAN_ROLE_OFFICER));

            assertEquals(GameHub.CLAN_ROLE_CANDIDATE,
                    hub.addClanCandidate("TestKnight", "TestMage").role().wireValue());
            assertRoles(hub.changeClanRole(
                            "TestKnight", "TestMage", GameHub.CLAN_ROLE_OFFICER),
                    List.of(GameHub.CLAN_ROLE_OWNER, GameHub.CLAN_ROLE_OFFICER));
            assertEquals("Raiders", hub.clanData("TestMage").name());

            assertRoles(hub.changeClanRole(
                            "TestKnight", "TestMage", GameHub.CLAN_ROLE_CANDIDATE),
                    List.of(GameHub.CLAN_ROLE_OWNER, GameHub.CLAN_ROLE_CANDIDATE));
            assertEquals(GameHub.ClanData.empty(), hub.clanData("TestMage"));
            assertNull(hub.changeClanRole("TestMage", "TestKnight", GameHub.CLAN_ROLE_MEMBER));
            assertNull(hub.changeClanRole("TestKnight", "Missing", GameHub.CLAN_ROLE_MEMBER));
            assertNull(hub.changeClanRole("TestKnight", "TestMage", 4));
        }
    }

    @Test
    void failedEconomyCommitCanRollBackOnlyAnUntouchedNewClan() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            assertEquals(GameHub.ClassicClanCreateStatus.CREATED,
                    hub.createClassicClan("Crown", "RollbackClan"));
            hub.rollbackClassicClanCreation("Crown", "RollbackClan");
            assertEquals(GameHub.ClanData.empty(), hub.clanData("Crown"));

            assertEquals(GameHub.ClassicClanCreateStatus.CREATED,
                    hub.createClassicClan("Crown", "CommittedClan"));
            assertEquals(GameHub.ClassicClanJoinStatus.JOINED,
                    hub.joinClassicClan("Crown", "Member", 2).status());
            hub.rollbackClassicClanCreation("Crown", "CommittedClan");
            assertEquals("CommittedClan", hub.clanData("Crown").name());
        }
    }

    @Test
    void clanWarAcceptanceSurrenderAndReplayAreAtomic() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            assertTrue(hub.createClan("Crown", "Raiders"));
            assertTrue(hub.createClan("EnemyCrown", "Legacy"));
            assertTrue(hub.createClan("ThirdCrown", "Third"));

            GameHub.ClassicClanWarResolution started = hub.resolveClassicClanWar(
                    "Crown", "EnemyCrown", "Raiders", "Legacy",
                    Protocol.ClanWarRequestType.DECLARE, true);
            assertEquals(GameHub.ClassicClanWarResolutionStatus.STARTED, started.status());
            assertEquals(new GameHub.ClassicClanWarData("Raiders", "Legacy"),
                    hub.classicClanWarData("Crown"));
            assertEquals(started.war(), hub.classicClanWarData("EnemyCrown"));

            assertEquals(GameHub.ClassicClanWarResolutionStatus.INVALID,
                    hub.resolveClassicClanWar(
                            "Crown", "ThirdCrown", "Raiders", "Third",
                            Protocol.ClanWarRequestType.DECLARE, true).status());
            assertEquals(GameHub.ClassicClanWarResolutionStatus.INVALID,
                    hub.resolveClassicClanWar(
                            "Crown", "EnemyCrown", "Raiders", "Legacy",
                            Protocol.ClanWarRequestType.DECLARE, true).status());

            GameHub.ClassicClanWarResolution surrendered = hub.resolveClassicClanWar(
                    "Crown", "EnemyCrown", "Raiders", "Legacy",
                    Protocol.ClanWarRequestType.SURRENDER, true);
            assertEquals(GameHub.ClassicClanWarResolutionStatus.SURRENDERED,
                    surrendered.status());
            assertEquals(GameHub.ClassicClanWarData.empty(),
                    hub.classicClanWarData("Crown"));
            assertEquals(GameHub.ClassicClanWarData.empty(),
                    hub.classicClanWarData("EnemyCrown"));
            assertEquals(GameHub.ClassicClanWarResolutionStatus.INVALID,
                    hub.resolveClassicClanWar(
                            "Crown", "EnemyCrown", "Raiders", "Legacy",
                            Protocol.ClanWarRequestType.SURRENDER, true).status());
        }
    }

    @Test
    void clanWarRefusalAndCeasePreserveExpectedState() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (GameHub hub = new GameHub(config)) {
            assertTrue(hub.createClan("Crown", "Raiders"));
            assertTrue(hub.createClan("EnemyCrown", "Legacy"));

            assertEquals(GameHub.ClassicClanWarResolutionStatus.REFUSED,
                    hub.resolveClassicClanWar(
                            "Crown", "EnemyCrown", "Raiders", "Legacy",
                            Protocol.ClanWarRequestType.DECLARE, false).status());
            assertEquals(GameHub.ClassicClanWarData.empty(),
                    hub.classicClanWarData("Crown"));

            assertEquals(GameHub.ClassicClanWarResolutionStatus.STARTED,
                    hub.resolveClassicClanWar(
                            "Crown", "EnemyCrown", "Raiders", "Legacy",
                            Protocol.ClanWarRequestType.DECLARE, true).status());
            assertEquals(GameHub.ClassicClanWarResolutionStatus.CEASED,
                    hub.resolveClassicClanWar(
                            "EnemyCrown", "Crown", "Legacy", "Raiders",
                            Protocol.ClanWarRequestType.CEASE, true).status());
            assertEquals(GameHub.ClassicClanWarData.empty(),
                    hub.classicClanWarData("Crown"));
        }
    }

    private static void assertRoles(GameHub.ClanData clan, List<Integer> roles) {
        assertEquals(roles, clan.members().stream()
                .map(member -> member.role().wireValue()).toList());
    }
}

package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionWorldCkewEntryTest {
    @TempDir Path temporary;

    @Test
    void retainedMap2001RowCannotCreateAnEncryptedPortalCapability()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub hub = new GameHub(config);
        GameStateStore state = new GameStateStore(
                temporary.resolve("dead-second-floor-state.properties"), false);
        Fixture fixture = fixture(config, hub, state,
                "CkewDeadSecondFloor", 0, 2001);
        try {
            hub.leaveClassicWorld(fixture.session);
            setField(fixture.session, "classicWorldMapId", 2000);
            setField(fixture.session, "classicWorldInstanceId", 100);
            setField(fixture.session, "positionX", 32720);
            setField(fixture.session, "positionY", 32900);
            hub.enterClassicWorld(fixture.session, 2000, 32720, 32900, 2, 100);
            fixture.packets();

            // The retained DB row starts here, but exact LiTo Layer3 blocks
            // every heading and the nearest standing cell is 49 tiles away.
            setField(fixture.session, "positionX", 32735);
            setField(fixture.session, "positionY", 32991);
            fixture.enterPortal(32735, 32991);

            assertTrue(fixture.packets().isEmpty());
            GameHub.ClassicWorldLocation location =
                    hub.classicWorldLocation(fixture.session);
            assertEquals(List.of(2000, 32720, 32900, 100), List.of(
                    location.mapId(), location.x(), location.y(),
                    location.instanceId()));
        } finally {
            fixture.closeSession();
            hub.close();
        }
    }

    @Test
    void timeoutSendsNative3112BeforeFixedFailureExit() throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub hub = new GameHub(config);
        GameStateStore state = new GameStateStore(
                temporary.resolve("timeout-state.properties"), false);
        Fixture fixture = fixture(config, hub, state,
                "CkewTimeout", 0, 201);
        try {
            ClassicWorldCkewCatalog.Location entry =
                    ClassicWorldCkewCatalog.characterClass(0).firstFloor();
            hub.leaveClassicWorld(fixture.session);
            setField(fixture.session, "classicWorldMapId", 2000);
            setField(fixture.session, "classicWorldInstanceId", 100);
            setField(fixture.session, "positionX", entry.x());
            setField(fixture.session, "positionY", entry.y());
            hub.enterClassicWorld(fixture.session, 2000,
                    entry.x(), entry.y(), entry.heading(), 100);
            fixture.packets();

            fixture.session.serverClassicWorldCkewEnded(
                    new ClassicWorldCkewInstances.Ended(
                            100, List.of(fixture.identity()), "timeout"));
            List<byte[]> ending = fixture.packets();
            int messageIndex = indexOf(ending, Protocol.messageCode(3112));
            int portalIndex = indexOf(ending, Protocol.portal(4, 33430, 32814));
            assertTrue(messageIndex >= 0);
            assertTrue(portalIndex > messageIndex,
                    "native timeout message precedes the fixed failure exit");

            fixture.completePortal();
            fixture.packets();
            assertEquals(4, hub.classicWorldLocation(fixture.session).mapId());
            assertEquals(-1,
                    hub.classicWorldLocation(fixture.session).instanceId());
        } finally {
            fixture.closeSession();
            hub.close();
        }
    }

    @Test
    void failedDurableEntryCannotPublishPortalOrEnteredQuestState()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.world-zero-gunter-selftest.properties"));
        GameHub hub = new GameHub(config);
        Path stateFile = temporary.resolve("entry-atomic-state.properties");
        GameStateStore state = new GameStateStore(stateFile, true);
        List<Fixture> fixtures = new ArrayList<>();
        try {
            fixtures.add(fixture(config, hub, state, "AtomicCrown", 0, 111));
            fixtures.add(fixture(config, hub, state, "AtomicKnight", 1, 112));
            fixtures.add(fixture(config, hub, state, "AtomicElf", 2, 113));
            fixtures.add(fixture(config, hub, state, "AtomicWizard", 3, 114));
            String leader = fixtures.get(0).identity();
            for (int index = 1; index < fixtures.size(); index++) {
                assertEquals(GameHub.ClassicPartyJoinStatus.JOINED,
                        hub.joinClassicParty(
                                leader, fixtures.get(index).identity(), 8).status());
            }

            Fixture crown = fixtures.get(0);
            state.saveStoryQuestSteps(crown.identity(), Map.of(
                    ClassicWorldCkewCatalog.QUEST_ID,
                    ClassicWorldCkewCatalog.QUEST_STARTED,
                    ClassicWorldCkewCatalog.SELECTED_ENTRY_QUEST_ID, 0));
            Files.createDirectory(stateFile.resolveSibling(
                    stateFile.getFileName() + ".tmp"));
            ClassicWorldNpcCatalog.StaticNpc entrance =
                    ClassicWorldNpcCatalog.all(410).stream()
                            .filter(npc -> npc.npcId() == 80135)
                            .findFirst().orElseThrow();
            crown.packets();

            InvocationTargetException failure = assertThrows(
                    InvocationTargetException.class,
                    () -> crown.action(new Protocol.HypertextActionRequest(
                            entrance.objectId(), "ent", "",
                            Protocol.HypertextActionKind.GENERIC)));
            assertTrue(failure.getCause() instanceof java.io.IOException);

            GameStateStore.StoryQuestState quests =
                    state.storyQuests(crown.identity());
            assertEquals(ClassicWorldCkewCatalog.QUEST_STARTED,
                    quests.step(ClassicWorldCkewCatalog.QUEST_ID));
            assertEquals(0, quests.step(
                    ClassicWorldCkewCatalog.SELECTED_ENTRY_QUEST_ID));
            assertEquals(0, quests.step(
                    ClassicWorldCkewCatalog.SELECTED_COMPLETION_QUEST_ID));
            assertEquals(-1, crown.session.pendingClassicWorldCkewEntryShowId());
            assertTrue(hub.classicWorldCkewInstances().activeSnapshots().isEmpty());
            assertEquals(0, hub.classicWorldCkewMonsterPopulationSize(100));
            List<byte[]> packets = crown.packets();
            assertTrue(packets.stream().anyMatch(packet ->
                    java.util.Arrays.equals(packet,
                            Protocol.closeHypertext(entrance.objectId()))));
            assertFalse(packets.stream().anyMatch(packet ->
                    packet.length > 0 && (packet[0] & 0xFF) == Protocol.S_PORTAL));
        } finally {
            for (Fixture fixture : fixtures) fixture.closeSession();
            hub.close();
        }
    }

    @Test
    void endedShowInvalidatesPublishedEntryBeforeGotoPortal() throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.world-zero-gunter-selftest.properties"));
        GameHub hub = new GameHub(config);
        GameStateStore state = new GameStateStore(
                temporary.resolve("ended-before-goto-state.properties"), false);
        List<Fixture> fixtures = new ArrayList<>();
        try {
            fixtures.add(fixture(config, hub, state, "RaceCrown", 0, 121));
            fixtures.add(fixture(config, hub, state, "RaceKnight", 1, 122));
            fixtures.add(fixture(config, hub, state, "RaceElf", 2, 123));
            fixtures.add(fixture(config, hub, state, "RaceWizard", 3, 124));
            String leader = fixtures.get(0).identity();
            for (int index = 1; index < fixtures.size(); index++) {
                assertEquals(GameHub.ClassicPartyJoinStatus.JOINED,
                        hub.joinClassicParty(
                                leader, fixtures.get(index).identity(), 8).status());
            }

            Fixture crown = fixtures.get(0);
            state.saveStoryQuestSteps(crown.identity(), Map.of(
                    ClassicWorldCkewCatalog.QUEST_ID,
                    ClassicWorldCkewCatalog.QUEST_STARTED,
                    ClassicWorldCkewCatalog.SELECTED_ENTRY_QUEST_ID, 0));
            ClassicWorldNpcCatalog.StaticNpc entrance =
                    ClassicWorldNpcCatalog.all(410).stream()
                            .filter(npc -> npc.npcId() == 80135)
                            .findFirst().orElseThrow();
            crown.packets();
            crown.action(new Protocol.HypertextActionRequest(
                    entrance.objectId(), "ent", "",
                    Protocol.HypertextActionKind.GENERIC));
            int showId = crown.session.pendingClassicWorldCkewEntryShowId();
            assertTrue(showId >= ClassicWorldCkewCatalog.FIRST_SHOW_ID);
            assertTrue(crown.packets().stream().anyMatch(packet ->
                    java.util.Arrays.equals(packet, Protocol.portal(
                            2000,
                            ClassicWorldCkewCatalog.characterClass(0)
                                    .firstFloor().x(),
                            ClassicWorldCkewCatalog.characterClass(0)
                                    .firstFloor().y()))));

            // The retained endQuest only teleports users already on map2000.
            // A LiTo client still waiting to acknowledge S_PORTAL is on map410,
            // so the pending capability must be revoked instead of publishing
            // an entrance into a show that no longer exists.
            hub.endClassicWorldCkew(showId, "timeout");
            assertEquals(-1, crown.session.pendingClassicWorldCkewEntryShowId());
            assertTrue(hub.classicWorldCkewInstances().snapshot(showId) == null);
            List<byte[]> ending = crown.packets();
            assertTrue(ending.stream().anyMatch(packet ->
                    java.util.Arrays.equals(packet, Protocol.messageCode(3112))));
            assertFalse(ending.stream().anyMatch(packet ->
                    java.util.Arrays.equals(packet,
                            Protocol.portal(4, 33430, 32814))),
                    "map410 pending entrant must not receive an in-instance exit");

            crown.completePortal();
            crown.packets();
            GameHub.ClassicWorldLocation location =
                    hub.classicWorldLocation(crown.session);
            assertEquals(List.of(410, -1, -1), List.of(
                    location.mapId(),
                    crown.session.pendingClassicWorldCkewEntryShowId(),
                    location.instanceId()));
            assertEquals(ClassicWorldCkewCatalog.QUEST_ENTERED,
                    state.storyQuests(crown.identity())
                            .step(ClassicWorldCkewCatalog.QUEST_ID),
                    "retained endQuest does not roll quest122 back from step2");
        } finally {
            for (Fixture fixture : fixtures) fixture.closeSession();
            hub.close();
        }
    }

    @Test
    void fourNativeClassesStartQuestAndEnterOneSharedShowId() throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.world-zero-gunter-selftest.properties"));
        GameHub hub = new GameHub(config);
        GameStateStore state = new GameStateStore(
                temporary.resolve("state.properties"), false);
        List<Fixture> fixtures = new ArrayList<>();
        try {
            fixtures.add(fixture(config, hub, state, "CkewCrown", 0, 101));
            fixtures.add(fixture(config, hub, state, "CkewKnight", 1, 102));
            fixtures.add(fixture(config, hub, state, "CkewElf", 2, 103));
            fixtures.add(fixture(config, hub, state, "CkewWizard", 3, 104));

            // Continue the same Elf session from the native Dspy reward into
            // Dicarding's p report.  This is the cross-quest boundary which
            // the selected 12201 row and retained quest122 must share.
            Fixture elf = fixtures.get(2);
            elf.replaceInventory(Map.of(49163L, 1));
            state.saveStoryQuestSteps(elf.identity(), Map.of(
                    11601, 1, 11603, 1, 11604, 0));
            ClassicWorldNpcCatalog.StaticNpc dicarding =
                    ClassicWorldNpcCatalog.all(77).stream()
                            .filter(npc -> npc.npcId()
                                    == ClassicWorldCkewCompletionCatalog.NPC_ID)
                            .findFirst().orElseThrow();
            elf.relocateNear(dicarding);
            elf.action(new Protocol.HypertextActionRequest(
                    dicarding.objectId(), "p", "",
                    Protocol.HypertextActionKind.GENERIC));
            assertEquals(1, state.storyQuests(elf.identity()).step(11604));
            assertEquals(ClassicWorldCkewCatalog.QUEST_STARTED,
                    state.storyQuests(elf.identity()).step(
                            ClassicWorldCkewCatalog.QUEST_ID));
            assertEquals(0, state.storyQuests(elf.identity()).step(
                    ClassicWorldCkewCatalog.SELECTED_ENTRY_QUEST_ID));
            assertTrue(elf.packets().stream().anyMatch(packet ->
                    java.util.Arrays.equals(packet, Protocol.hypertext(
                            dicarding.objectId(), "dicardinge10", "", List.of()))));
            assertEquals(0, elf.inventoryCounts().getOrDefault(49163L, 0));
            ClassicWorldNpcCatalog.StaticNpc elfEntrance =
                    ClassicWorldNpcCatalog.all(410).stream()
                            .filter(npc -> npc.npcId() == 80133)
                            .findFirst().orElseThrow();
            elf.relocateNear(elfEntrance);
            elf.packets();

            String leader = fixtures.get(0).identity();
            for (int index = 1; index < fixtures.size(); index++) {
                assertEquals(GameHub.ClassicPartyJoinStatus.JOINED,
                        hub.joinClassicParty(
                                leader, fixtures.get(index).identity(), 8).status());
            }
            assertTrue(hub.validClassicWorldCkewParty(fixtures.get(0).session));
            Method tick = GameHub.class.getDeclaredMethod(
                    "classicWorldCkewTickSafely");
            tick.setAccessible(true);
            int[] sharedShowId = {-1};

            for (Fixture fixture : fixtures) {
                // Earlier entrants broadcast their ordinary-world removal to
                // every remaining map410 member; isolate this talk assertion.
                fixture.packets();
                ClassicWorldCkewCatalog.CharacterClass nativeClass =
                        ClassicWorldCkewCatalog.characterClass(fixture.type);
                ClassicWorldNpcCatalog.StaticNpc entrance =
                        ClassicWorldNpcCatalog.all(410).stream()
                                .filter(npc -> npc.npcId()
                                        == nativeClass.entranceNpcId())
                                .findFirst().orElseThrow();

                if (fixture.type == 2) {
                    assertEquals(ClassicWorldCkewCatalog.QUEST_STARTED,
                            state.storyQuests(fixture.identity())
                                    .step(ClassicWorldCkewCatalog.QUEST_ID));
                } else {
                    fixture.open(entrance.objectId());
                    assertEquals(ClassicWorldCkewCatalog.QUEST_STARTED,
                            state.storyQuests(fixture.identity())
                                    .step(ClassicWorldCkewCatalog.QUEST_ID));
                    // B-land（#138 主控裁定 1）：候選的 L1Quest 對話會讓入口
                    // NPC 轉向說話者並送 S_CHANGE_DIRECTION（正式 0.6.71
                    // 不送）。這是有意採用的行為改變；首次對話仍不送 HTML。
                    int expectedHeading = NativeStaticNpcMotion.direction(
                            NativeStaticNpcMotion.x(entrance, fixture.session),
                            NativeStaticNpcMotion.y(entrance, fixture.session),
                            (int) CombatAccess.field(fixture.session, "positionX"),
                            (int) CombatAccess.field(fixture.session, "positionY"));
                    if (expectedHeading < 0) {
                        expectedHeading = NativeStaticNpcScene.heading(
                                hub, entrance, fixture.session);
                    }
                    byte[] turn = Protocol.changeDirection(
                            entrance.objectId(), expectedHeading);
                    List<byte[]> firstTalk = fixture.packets();
                    assertFalse(firstTalk.isEmpty(),
                            "native first talk turns the entrance NPC");
                    assertTrue(firstTalk.stream().allMatch(packet ->
                                    java.util.Arrays.equals(packet, turn)),
                            "native first talk starts quest without HTML;"
                                    + " only the L1Quest talk turn is sent");
                    state.saveStoryQuestSteps(fixture.identity(), Map.of(
                            ClassicWorldCkewCatalog.QUEST_ID,
                            ClassicWorldCkewCatalog.QUEST_STARTED,
                            ClassicWorldCkewCatalog.SELECTED_ENTRY_QUEST_ID, 0));
                }

                fixture.open(entrance.objectId());
                assertTrue(fixture.packets().stream().anyMatch(packet ->
                        java.util.Arrays.equals(packet, Protocol.hypertext(
                                entrance.objectId(), nativeClass.entranceHtml(),
                                "", List.of()))));

                fixture.action(new Protocol.HypertextActionRequest(
                        entrance.objectId(), "ent", "",
                        Protocol.HypertextActionKind.GENERIC));
                assertEquals(ClassicWorldCkewCatalog.QUEST_ENTERED,
                        state.storyQuests(fixture.identity())
                                .step(ClassicWorldCkewCatalog.QUEST_ID));
                assertEquals(1, state.storyQuests(fixture.identity())
                        .step(ClassicWorldCkewCatalog.SELECTED_ENTRY_QUEST_ID));
                assertEquals(0, state.storyQuests(fixture.identity())
                        .step(ClassicWorldCkewCatalog.SELECTED_COMPLETION_QUEST_ID));

                // S_PORTAL is a two-packet handshake in LiTo.  The instance
                // must remain alive while this client has not yet returned
                // C_GOTO_PORTAL, even if the one-second audit runs here.
                int pendingShowId =
                        fixture.session.pendingClassicWorldCkewEntryShowId();
                if (sharedShowId[0] < 0) sharedShowId[0] = pendingShowId;
                assertEquals(sharedShowId[0], pendingShowId);
                tick.invoke(hub);
                assertTrue(hub.classicWorldCkewInstances()
                        .snapshot(sharedShowId[0]) != null);
                assertEquals(ClassicWorldCkewMonsterCatalog.POPULATION,
                        hub.classicWorldCkewMonsterPopulationSize(sharedShowId[0]));
                List<byte[]> admission = fixture.packets();
                assertTrue(admission.stream().anyMatch(packet ->
                        java.util.Arrays.equals(packet,
                                Protocol.closeHypertext(entrance.objectId()))));
                assertTrue(admission.stream().anyMatch(packet ->
                        java.util.Arrays.equals(packet, Protocol.portal(
                                2000, nativeClass.firstFloor().x(),
                                nativeClass.firstFloor().y()))));
                assertTrue(admission.stream().noneMatch(packet ->
                        java.util.Arrays.equals(packet,
                                Protocol.portal(4, 33430, 32814))),
                        "pending C_GOTO must not trigger outStop");
                if (fixture.type == 0) {
                    assertTrue(admission.stream().anyMatch(packet ->
                            java.util.Arrays.equals(packet,
                                    Protocol.questHelpMessage(
                                            "\\fV副本任務-剩餘時間：30分",
                                            fixture.hub.classicWorldLocation(
                                                    fixture.session).x(),
                                            fixture.hub.classicWorldLocation(
                                                    fixture.session).y()))));
                }

                fixture.completePortal();
                fixture.packets();
                assertEquals(2000,
                        hub.classicWorldLocation(fixture.session).mapId());
                assertEquals(sharedShowId[0],
                        hub.classicWorldLocation(fixture.session).instanceId());
                assertEquals(sharedShowId[0],
                        fixture.session.classicWorldInstanceId());
            }

            assertEquals(fixtures.stream().map(Fixture::session)
                            .collect(java.util.stream.Collectors.toSet()),
                    Set.copyOf(hub.classicWorldMembers(2000, sharedShowId[0])));
            assertTrue(hub.classicWorldMembers(2000).isEmpty());
            assertEquals(SetView.of(fixtures), hub.classicWorldCkewInstances()
                    .snapshot(sharedShowId[0]).joinedIdentities());

            // The runtime death hook uses the same show-local population. A
            // first-floor source grants 49165 once, while Guardian2 preserves
            // the retained no-existing-item guard and therefore stacks 49167.
            Fixture crown = fixtures.get(0);
            ClassicWorldMonsterPopulation.Snapshot guardian1 =
                    hub.classicWorldMonster(
                            2000, sharedShowId[0], 0x6600_0000L + 113);
            assertEquals(87001, guardian1.npcId());
            crown.questDrop(guardian1);
            assertEquals(1, crown.inventoryCounts().get(49165L));
            crown.questDrop(guardian1);
            assertEquals(1, crown.inventoryCounts().get(49165L));

            ClassicWorldMonsterPopulation.Snapshot guardian2 =
                    hub.classicWorldMonster(
                            2000, sharedShowId[0], 0x6600_0000L + 190);
            assertEquals(87011, guardian2.npcId());
            crown.questDrop(guardian2);
            crown.questDrop(guardian2);
            assertEquals(2, crown.inventoryCounts().get(49167L));

            crown.useMagicFlute();
            assertEquals(0, crown.inventoryCounts().getOrDefault(49167L, 0));
            assertEquals(ClassicWorldCkewMonsterCatalog.POPULATION + 4,
                    hub.classicWorldCkewMonsterPopulationSize(sharedShowId[0]));
            assertTrue(hub.classicWorldCkewHasMonsterNpc(
                    sharedShowId[0], ClassicWorldCkewCatalog.CHINO_NPC_IDS));
            java.util.Set<Integer> summonedNpcIds = new java.util.LinkedHashSet<>();
            Map<Integer, ClassicWorldMonsterPopulation.Snapshot> summonedByNpc =
                    new LinkedHashMap<>();
            for (int index = 0; index < 4; index++) {
                ClassicWorldMonsterPopulation.Snapshot summoned =
                        hub.classicWorldMonster(
                                2000, sharedShowId[0], 0x6600_0800L + index);
                summonedNpcIds.add(summoned.npcId());
                summonedByNpc.put(summoned.npcId(), summoned);
                Fixture owner = fixtures.stream().filter(candidate ->
                        ClassicWorldCkewCatalog.characterClass(candidate.type)
                                .chinoNpcId() == summoned.npcId())
                        .findFirst().orElseThrow();
                GameHub.ClassicWorldLocation ownerLocation =
                        hub.classicWorldLocation(owner.session);
                assertTrue(Math.max(Math.abs(summoned.x() - ownerLocation.x()),
                        Math.abs(summoned.y() - ownerLocation.y())) <= 2);
            }
            assertEquals(ClassicWorldCkewCatalog.CHINO_NPC_IDS, summonedNpcIds);

            // Exercise both custom Type2 packet centers over the same four
            // encrypted socket pairs used by admission and flute spawning.
            for (Fixture fixture : fixtures) fixture.packets();
            ClassicWorldMonsterPopulation population =
                    ckewPopulation(hub, sharedShowId[0]);
            Set<ClassicWorldMonsterPopulation.GridPoint> hostileCells =
                    new LinkedHashSet<>();
            for (Fixture fixture : fixtures) {
                GameHub.ClassicWorldLocation location =
                        hub.classicWorldLocation(fixture.session);
                hostileCells.add(new ClassicWorldMonsterPopulation.GridPoint(
                        location.x(), location.y()));
                setField(fixture.session, "playerHp", 5000);
                setField(fixture.session, "playerMaxHp", 5000);
            }

            Fixture royal = fixtures.get(0);
            ClassicWorldMonsterPopulation.Snapshot royalChino =
                    summonedByNpc.get(87017);
            ClassicWorldMonsterPopulation.MagicAttack darkWave =
                    customMagic(population, royalChino, royal, hostileCells,
                            30_000_000_000L, 1);
            assertEquals(10010, darkWave.skill().skillId());
            royal.session.serverClassicWorldMonsterMagicAttack(darkWave);
            byte[] darkWavePacket = firstOpcode(
                    royal.packets(), Protocol.S_ATTACK_MANY);
            assertMagicPacket(darkWavePacket, royalChino.objectId(),
                    royal.session.playerObjectId(), 2510, 0);
            assertTrue(intField(royal.session, "playerHp") < 5000);

            Fixture knight = fixtures.get(1);
            ClassicWorldMonsterPopulation.Snapshot knightChino =
                    summonedByNpc.get(87018);
            ClassicWorldMonsterPopulation.MagicAttack lightArrow =
                    customMagic(population, knightChino, knight, hostileCells,
                            31_000_000_000L, 2);
            assertEquals(10061, lightArrow.skill().skillId());
            knight.session.serverClassicWorldMonsterMagicAttack(lightArrow);
            byte[] lightArrowPacket = firstOpcode(
                    knight.packets(), Protocol.S_ATTACK_MANY);
            assertMagicPacket(lightArrowPacket, knightChino.objectId(),
                    knight.session.playerObjectId(), 2546, 8);
            assertTrue(intField(knight.session, "playerHp") < 5000);

            // Retained watcher87005 uses the separate skill33 probability
            // layer, then a 5-second warning and 10-second client action lock.
            for (Fixture fixture : fixtures) fixture.packets();
            ClassicWorldMonsterPopulation.Snapshot watcher = population.nearest(
                    ClassicWorldCkewCatalog.MAP_ID, 32890, 32820, 87005);
            assertTrue(watcher != null);
            ClassicWorldMonsterPopulation.GridPoint curseTarget =
                    targetAtDistance(watcher, 1);
            assertTrue(curseTarget != null);
            setField(royal.session, "positionX", curseTarget.x());
            setField(royal.session, "positionY", curseTarget.y());
            assertTrue(hub.updateClassicWorld(
                    royal.session, ClassicWorldCkewCatalog.MAP_ID,
                    curseTarget.x(), curseTarget.y(), 0));
            assertTrue(population.provoke(
                    ClassicWorldCkewCatalog.MAP_ID, watcher.objectId(),
                    royal.session.playerObjectId()));
            ClassicWorldMonsterPopulation.CkewSkillAttack watcherChoice =
                    population.ckewSkillAttack(
                            ClassicWorldCkewCatalog.MAP_ID, watcher.objectId(),
                            royal.session.playerObjectId(),
                            curseTarget.x(), curseTarget.y(),
                            32_000_000_000L, hostileCells, 1, 1, 1);
            assertTrue(watcherChoice != null && watcherChoice.status() != null);
            long castStarted = System.nanoTime();
            royal.session.serverClassicWorldMonsterStatusSkillAttack(
                    watcherChoice.status(), 1);
            long castFinished = System.nanoTime();
            List<byte[]> warningPackets = royal.packets();
            int warningMessage = indexOf(
                    warningPackets, Protocol.messageCode(212));
            int grayPalette = indexOf(warningPackets,
                    Protocol.poison(royal.session.playerObjectId(), 2));
            int curseAction = indexOf(warningPackets,
                    Protocol.objectAction(watcher.objectId(), 19));
            int curseEffect = indexOf(warningPackets,
                    Protocol.effectObject(royal.session.playerObjectId(), 746));
            assertTrue(warningMessage >= 0);
            assertTrue(grayPalette > warningMessage);
            assertTrue(curseAction > grayPalette);
            assertTrue(curseEffect > curseAction);

            BattleTimedStatus status = (BattleTimedStatus) objectField(
                    royal.session, "timedStatus");
            long activatesAt = longField(status,
                    "paralysisActivatesAtNanos");
            long expiresAt = longField(status, "paralysisExpiresAtNanos");
            assertTrue(activatesAt >= castStarted + 5_000_000_000L);
            assertTrue(activatesAt <= castFinished + 5_000_000_000L);
            assertEquals(10_000_000_000L, expiresAt - activatesAt);
            royal.session.serverExpireTimedStatuses(activatesAt - 1);
            assertTrue(royal.packets().stream().noneMatch(packet ->
                    java.util.Arrays.equals(packet, Protocol.paralysis(
                            Protocol.ParalysisMode.PARALYSIS_APPLY))));
            royal.session.serverExpireTimedStatuses(activatesAt);
            assertTrue(royal.packets().stream().anyMatch(packet ->
                    java.util.Arrays.equals(packet, Protocol.paralysis(
                            Protocol.ParalysisMode.PARALYSIS_APPLY))));
            assertTrue(status.paralysisActive(activatesAt));
            royal.session.serverExpireTimedStatuses(expiresAt);
            List<byte[]> unlocked = royal.packets();
            assertTrue(unlocked.stream().anyMatch(packet ->
                    java.util.Arrays.equals(packet, Protocol.paralysis(
                            Protocol.ParalysisMode.PARALYSIS_RELEASE))));
            assertTrue(unlocked.stream().anyMatch(packet ->
                    java.util.Arrays.equals(packet,
                            Protocol.poison(royal.session.playerObjectId(), 0))));
            assertTrue(!status.paralysisActive(expiresAt));

            ClassicWorldMonsterPopulation.Snapshot altar =
                    hub.classicWorldMonster(
                            2000, sharedShowId[0], 0x6600_0000L + 112);
            assertEquals(87000, altar.npcId());
            crown.completeAltar(altar);
            for (Fixture fixture : fixtures) {
                assertEquals(ClassicWorldCkewCatalog.QUEST_COMPLETED,
                        state.storyQuests(fixture.identity()).step(
                                ClassicWorldCkewCatalog.QUEST_ID));
                assertEquals(1, state.storyQuests(fixture.identity()).step(
                        ClassicWorldCkewCatalog.SELECTED_ENTRY_QUEST_ID));
                assertEquals(0, state.storyQuests(fixture.identity()).step(
                        ClassicWorldCkewCatalog.SELECTED_COMPLETION_QUEST_ID));
                assertEquals(1, fixture.inventoryCounts().get(49241L));
            }

            for (Fixture fixture : fixtures) fixture.packets();
            Fixture departed = fixtures.get(0);
            hub.leaveClassicWorld(departed.session);
            setField(departed.session, "classicWorldMapId", 4);
            setField(departed.session, "classicWorldInstanceId", -1);
            tick.invoke(hub);

            assertEquals(null, hub.classicWorldCkewInstances()
                    .snapshot(sharedShowId[0]));
            assertEquals(0,
                    hub.classicWorldCkewMonsterPopulationSize(sharedShowId[0]));
            for (int index = 1; index < fixtures.size(); index++) {
                Fixture remaining = fixtures.get(index);
                assertTrue(remaining.packets().stream().anyMatch(packet ->
                        java.util.Arrays.equals(packet,
                                Protocol.portal(4, 33430, 32814))));
                remaining.completePortal();
                remaining.packets();
                assertEquals(4, hub.classicWorldLocation(
                        remaining.session).mapId());
                assertEquals(-1, hub.classicWorldLocation(
                        remaining.session).instanceId());
            }

            // The same naturally bridged Elf now returns with altar proof and
            // selects the native bow reward.  This closes 122 and mirrors the
            // selected completion row 12202 without granting v3 entry items.
            elf.relocateNear(dicarding);
            elf.packets();
            elf.action(new Protocol.HypertextActionRequest(
                    dicarding.objectId(), "y", "",
                    Protocol.HypertextActionKind.GENERIC));
            assertEquals(1, elf.inventoryCounts().get(184L));
            assertEquals(0, elf.inventoryCounts().getOrDefault(49241L, 0));
            assertEquals(ClassicWorldCkewCatalog.QUEST_ENDED,
                    state.storyQuests(elf.identity()).step(
                            ClassicWorldCkewCatalog.QUEST_ID));
            assertEquals(1, state.storyQuests(elf.identity()).step(
                    ClassicWorldCkewCatalog.SELECTED_ENTRY_QUEST_ID));
            assertEquals(1, state.storyQuests(elf.identity()).step(
                    ClassicWorldCkewCatalog.SELECTED_COMPLETION_QUEST_ID));
            assertTrue(elf.packets().stream().anyMatch(packet ->
                    java.util.Arrays.equals(packet, Protocol.hypertext(
                            dicarding.objectId(), "dicardinge16", "", List.of()))));
        } finally {
            for (Fixture fixture : fixtures) fixture.closeSession();
            hub.close();
        }
    }

    private Fixture fixture(ServerConfig config, GameHub hub,
                            GameStateStore state, String name,
                            int type, long objectId) throws Exception {
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(100);
        CaptureLog capture = new CaptureLog(
                temporary.resolve("capture-" + type));
        ClientSession session = new ClientSession(server, ClientSession.Endpoint.GAME,
                config, null, state, capture, hub);
        int seed = 0x2300_0000 + type;
        Protocol.CipherState receiver = new Protocol.CipherState(seed);
        setField(session, "outgoing", new Protocol.CipherState(seed));
        setEnumField(session, "phase", "WORLD");
        setField(session, "account", "ckew" + type);
        setField(session, "selectedCharacter", name);
        CharacterSummary selected = character(name, type, objectId);
        setField(session, "characters", List.of(selected));
        setField(session, "sourceBaseMagicResistance",
                SourceBaseMagicResistance.from(selected));
        setField(session, "sourceEquipmentMagicStats", SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats", SourceEquipmentVitalStats.none());
        setField(session, "sourceInventoryWeightStats", SourceInventoryWeightStats.none(1.0));
        setField(session, "retainedSourceInventoryWeight", 0);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(), () -> 0x7200_0000L + type));
        setField(session, "playerHp", 100);
        setField(session, "playerMaxHp", 100);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
        setField(session, "playerClassicLevel", 50);

        ClassicWorldCkewCatalog.CharacterClass nativeClass =
                ClassicWorldCkewCatalog.characterClass(type);
        ClassicWorldNpcCatalog.StaticNpc entrance = ClassicWorldNpcCatalog.all(410)
                .stream().filter(npc -> npc.npcId() == nativeClass.entranceNpcId())
                .findFirst().orElseThrow();
        ArenaMapCatalog.Point point = nearbyPassable(entrance);
        setField(session, "classicWorldMapId", 410);
        setField(session, "positionX", point.x());
        setField(session, "positionY", point.y());
        hub.connect(session);
        hub.enterClassicWorld(session, 410, point.x(), point.y(), 0);
        return new Fixture(hub, capture, client, server, session, receiver, type);
    }

    private static CharacterSummary character(
            String name, int type, long objectId) {
        return new CharacterSummary(name, "", type, 0, 50, 100, 100,
                0, 8, 8, 8, 8, 12, 12, 12, 12,
                objectId, 0, 0, 0, 3, 32823, 32892, 410,
                8, 40, 1, 8, 8, 0);
    }

    private static ArenaMapCatalog.Point nearbyPassable(
            ClassicWorldNpcCatalog.StaticNpc target) {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(target.mapId());
        for (int radius = 1; radius <= 3; radius++) {
            for (int x = target.x() - radius; x <= target.x() + radius; x++) {
                for (int y = target.y() - radius; y <= target.y() + radius; y++) {
                    if (Math.max(Math.abs(x - target.x()),
                            Math.abs(y - target.y())) == radius
                            && map.standingPassable(x, y)
                            && !ClassicWorldNpcCatalog.occupies(target.mapId(), x, y)) {
                        return new ArenaMapCatalog.Point(x, y);
                    }
                }
            }
        }
        throw new AssertionError("no CKEW entrance interaction tile");
    }

    private static int indexOf(List<byte[]> packets, byte[] expected) {
        for (int index = 0; index < packets.size(); index++) {
            if (java.util.Arrays.equals(packets.get(index), expected)) {
                return index;
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private static ClassicWorldMonsterPopulation ckewPopulation(
            GameHub hub, int showId) throws Exception {
        Field field = GameHub.class.getDeclaredField("classicWorldCkewMonsters");
        field.setAccessible(true);
        Map<Integer, ClassicWorldMonsterPopulation> populations =
                (Map<Integer, ClassicWorldMonsterPopulation>) field.get(hub);
        ClassicWorldMonsterPopulation result = populations.get(showId);
        if (result == null) throw new AssertionError("missing CKEW population " + showId);
        return result;
    }

    private static ClassicWorldMonsterPopulation.MagicAttack customMagic(
            ClassicWorldMonsterPopulation population,
            ClassicWorldMonsterPopulation.Snapshot actor,
            Fixture target,
            Set<ClassicWorldMonsterPopulation.GridPoint> hostileCells,
            long nowNanos, int selectionFixture) {
        GameHub.ClassicWorldLocation location =
                target.hub.classicWorldLocation(target.session);
        assertTrue(population.provoke(
                ClassicWorldCkewCatalog.MAP_ID, actor.objectId(),
                target.session.playerObjectId()));
        ClassicWorldMonsterPopulation.MagicAttack result = population.magicAttack(
                ClassicWorldCkewCatalog.MAP_ID, actor.objectId(),
                target.session.playerObjectId(), location.x(), location.y(),
                nowNanos, hostileCells, 1, 1, selectionFixture);
        if (result == null) throw new AssertionError(
                "custom CKEW magic did not trigger npc=" + actor.npcId()
                        + " selection=" + selectionFixture);
        return result;
    }

    private static ClassicWorldMonsterPopulation.GridPoint targetAtDistance(
            ClassicWorldMonsterPopulation.Snapshot actor, int distance) {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(actor.mapId());
        int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
        for (int heading = 0; heading < dx.length; heading++) {
            int x = actor.x() + dx[heading] * distance;
            int y = actor.y() + dy[heading] * distance;
            if (map.attackPosition(actor.x(), actor.y(), x, y, distance)) {
                return new ClassicWorldMonsterPopulation.GridPoint(x, y);
            }
        }
        return null;
    }

    private static byte[] firstOpcode(List<byte[]> packets, int opcode) {
        return packets.stream()
                .filter(packet -> packet.length > 0
                        && (packet[0] & 0xFF) == opcode)
                .findFirst().orElseThrow(() ->
                        new AssertionError("missing opcode " + opcode));
    }

    private static void assertMagicPacket(
            byte[] packet, long casterObjectId, long targetObjectId,
            int effectGfx, int type) {
        assertTrue(packet.length >= 27);
        assertEquals(18, packet[1] & 0xFF);
        assertEquals(casterObjectId, dword(packet, 2));
        assertEquals(effectGfx, word(packet, 15));
        assertEquals(type, packet[17] & 0xFF);
        int count = word(packet, 20);
        assertTrue(count >= 1);
        boolean found = false;
        int offset = 22;
        for (int index = 0; index < count; index++, offset += 5) {
            if (dword(packet, offset) == targetObjectId
                    && (packet[offset + 4] & 0xFF) == 0x20) {
                found = true;
            }
        }
        assertTrue(found, "custom magic target and hit marker must be serialized");
    }

    private static int word(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
    }

    private static long dword(byte[] bytes, int offset) {
        return Integer.toUnsignedLong((bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24));
    }

    private static int intField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(target);
    }

    private static long longField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getLong(target);
    }

    private static Object objectField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setEnumField(Object target, String name, String value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, Enum.valueOf(
                (Class<? extends Enum>) field.getType(), value));
    }

    private record Fixture(GameHub hub, CaptureLog capture,
                           Socket client, Socket server,
                           ClientSession session,
                           Protocol.CipherState receiver, int type) {
        String identity() { return session.sourceCharacterIdentity(); }

        void open(long objectId) throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "openClassicWorldHypertext", long.class);
            method.setAccessible(true);
            method.invoke(session, objectId);
        }

        void action(Protocol.HypertextActionRequest request) throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "handleClassicWorldNpcAction",
                    Protocol.HypertextActionRequest.class);
            method.setAccessible(true);
            method.invoke(session, request);
        }

        void completePortal() throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "completePortal", byte[].class);
            method.setAccessible(true);
            method.invoke(session, (Object) new byte[] {(byte) Protocol.C_GOTO_PORTAL});
        }

        void enterPortal(int x, int y) throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "enterPortal", byte[].class);
            method.setAccessible(true);
            method.invoke(session, (Object) new Protocol.Writer()
                    .c(Protocol.C_ENTER_PORTAL).h(x).h(y).bytes());
        }

        void questDrop(ClassicWorldMonsterPopulation.Snapshot defeated)
                throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "awardClassicWorldCkewQuestDrop",
                    ClassicWorldMonsterPopulation.Snapshot.class);
            method.setAccessible(true);
            method.invoke(session, defeated);
        }

        void useMagicFlute() throws Exception {
            RuntimeInventory inventory = inventory();
            RuntimeInventory.Item flute = inventory.items().stream()
                    .filter(candidate -> candidate.definition().catalogId()
                            == ClassicWorldCkewCatalog.MAGIC_FLUTE)
                    .findFirst().orElseThrow();
            Method method = ClientSession.class.getDeclaredMethod(
                    "useClassicWorldCkewMagicFlute", RuntimeInventory.Item.class);
            method.setAccessible(true);
            method.invoke(session, flute);
        }

        void completeAltar(ClassicWorldMonsterPopulation.Snapshot altar)
                throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "completeClassicWorldCkewAltar",
                    ClassicWorldMonsterPopulation.Snapshot.class);
            method.setAccessible(true);
            method.invoke(session, altar);
        }

        Map<Long, Integer> inventoryCounts() throws Exception {
            return inventory().catalogCounts();
        }

        RuntimeInventory inventory() throws Exception {
            Field field = ClientSession.class.getDeclaredField("inventory");
            field.setAccessible(true);
            return (RuntimeInventory) field.get(session);
        }

        void replaceInventory(Map<Long, Integer> counts) throws Exception {
            setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                    counts, () -> 0x7300_0000L + type));
        }

        void relocateNear(ClassicWorldNpcCatalog.StaticNpc target) throws Exception {
            hub.leaveClassicWorld(session);
            ArenaMapCatalog.Point point = nearbyPassable(target);
            setField(session, "classicWorldMapId", target.mapId());
            setField(session, "classicWorldInstanceId", -1);
            setField(session, "positionX", point.x());
            setField(session, "positionY", point.y());
            hub.enterClassicWorld(session, target.mapId(), point.x(), point.y(), 0);
        }

        List<byte[]> packets() throws Exception {
            List<byte[]> result = new ArrayList<>();
            while (true) {
                try {
                    byte[] encrypted = Protocol.readFrame(
                            client.getInputStream(), 65_535);
                    if (encrypted == null) break;
                    result.add(receiver.decrypt(encrypted));
                } catch (SocketTimeoutException done) {
                    return result;
                }
            }
            return result;
        }

        void closeSession() throws Exception {
            hub.leaveClassicWorld(session);
            server.close();
            client.close();
            capture.close();
        }
    }

    private static final class SetView {
        private SetView() {}

        static java.util.Set<String> of(List<Fixture> fixtures) {
            return fixtures.stream().map(Fixture::identity)
                    .map(ClassicWorldCkewInstances::normalize)
                    .collect(java.util.stream.Collectors.toSet());
        }
    }
}

package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionWorldCkewCompletionTest {
    @TempDir Path temporary;

    @Test
    void fiveNativeChoicesCompleteOverEncryptedSocketsAndCleanLegacyItems()
            throws Exception {
        List<Choice> choices = List.of(
                new Choice(0, "a", 51, "dicardingp13", "dicardingp14",
                        "dicardingp15"),
                new Choice(1, "l", 56, "dicardingk14", "dicardingk15",
                        "dicardingk16"),
                new Choice(2, "y", 184, "dicardinge15", "dicardinge16",
                        "dicardinge17"),
                new Choice(2, "s", 50, "dicardinge15", "dicardinge16",
                        "dicardinge17"),
                new Choice(3, "x", 20225, "dicardingw13", "dicardingw14",
                        "dicardingw15"));
        for (int index = 0; index < choices.size(); index++) {
            Choice choice = choices.get(index);
            ServerConfig config = ServerConfig.load(
                    Path.of("src/test/resources/server-config/config/server.selftest.properties"));
            GameHub hub = new GameHub(config);
            GameStateStore state = new GameStateStore(
                    temporary.resolve("choice-" + index + ".properties"), false);
            Fixture fixture = fixture(config, hub, state,
                    "CkewCompletion" + index, choice.type(), 900 + index,
                    Map.of((long) ClassicWorldCkewCatalog.ALTAR_PROOF, 1,
                            (long) ClassicWorldCkewCatalog.SECOND_FLOOR_KEY, 2));
            try {
                state.saveStoryQuestSteps(fixture.identity(), Map.of(
                        ClassicWorldCkewCatalog.QUEST_ID,
                        ClassicWorldCkewCatalog.QUEST_COMPLETED,
                        ClassicWorldCkewCatalog.SELECTED_ENTRY_QUEST_ID, 1,
                        ClassicWorldCkewCatalog.SELECTED_COMPLETION_QUEST_ID, 0));
                RuntimeInventory before = fixture.inventory();
                RuntimeInventory.Item proof = before.items().stream()
                        .filter(item -> item.definition().catalogId()
                                == ClassicWorldCkewCatalog.ALTAR_PROOF)
                        .findFirst().orElseThrow();
                RuntimeInventory.Item cleanup = before.items().stream()
                        .filter(item -> item.definition().catalogId()
                                == ClassicWorldCkewCatalog.SECOND_FLOOR_KEY)
                        .findFirst().orElseThrow();

                fixture.open();
                assertTrue(contains(fixture.packets(), Protocol.hypertext(
                        fixture.npc().objectId(), choice.stepThreeHtml(), "", List.of())));

                fixture.action(choice.action());
                List<byte[]> packets = fixture.packets();
                int completionMessage = indexOf(packets, Protocol.questHelpMessage(
                        ClassicWorldCkewCompletionCatalog.COMPLETION_MESSAGE,
                        fixture.x(), fixture.y()));
                int successHtml = indexOf(packets, Protocol.hypertext(
                        fixture.npc().objectId(), choice.successHtml(), "", List.of()));
                int proofRemoval = indexOf(packets,
                        Protocol.removeInventory(proof.objectId()));
                int cleanupMessage = indexOf(packets,
                        Protocol.messageCode(165, cleanup.definition().name()));
                int cleanupRemoval = indexOf(packets,
                        Protocol.removeInventory(cleanup.objectId()));
                assertTrue(completionMessage >= 0);
                assertTrue(successHtml > completionMessage);
                assertTrue(proofRemoval > successHtml);
                assertTrue(cleanupMessage > proofRemoval);
                assertTrue(cleanupRemoval > cleanupMessage);

                RuntimeInventory after = fixture.inventory();
                assertEquals(1, after.catalogCounts().get((long) choice.rewardId()));
                assertFalse(after.catalogCounts().containsKey(
                        (long) ClassicWorldCkewCatalog.ALTAR_PROOF));
                assertFalse(after.catalogCounts().containsKey(
                        (long) ClassicWorldCkewCatalog.SECOND_FLOOR_KEY));
                assertEquals(ClassicWorldCkewCatalog.QUEST_ENDED,
                        state.storyQuests(fixture.identity()).step(
                                ClassicWorldCkewCatalog.QUEST_ID));
                assertEquals(1, state.storyQuests(fixture.identity()).step(
                        ClassicWorldCkewCatalog.SELECTED_ENTRY_QUEST_ID));
                assertEquals(1, state.storyQuests(fixture.identity()).step(
                        ClassicWorldCkewCatalog.SELECTED_COMPLETION_QUEST_ID));

                RuntimeInventory.Item reward = after.items().stream()
                        .filter(item -> item.definition().catalogId() == choice.rewardId())
                        .findFirst().orElseThrow();
                assertTrue(contains(packets, Protocol.questHelpMessage(
                        "\\fW" + fixture.npc().nameId() + "給你"
                                + reward.nativeLogName(),
                        fixture.x(), fixture.y())));

                fixture.open();
                assertTrue(contains(fixture.packets(), Protocol.hypertext(
                        fixture.npc().objectId(), choice.endedHtml(), "", List.of())));
            } finally {
                fixture.close();
                hub.close();
            }
        }
    }

    @Test
    void missingProofReturnsTheExactClassPageWithoutEndingQuest() throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub hub = new GameHub(config);
        GameStateStore state = new GameStateStore(
                temporary.resolve("missing.properties"), false);
        Fixture fixture = fixture(config, hub, state,
                "CkewMissing", 0, 990, Map.of());
        try {
            state.saveStoryQuestSteps(fixture.identity(), Map.of(
                    ClassicWorldCkewCatalog.QUEST_ID,
                    ClassicWorldCkewCatalog.QUEST_COMPLETED));
            fixture.action("a");
            assertTrue(contains(fixture.packets(), Protocol.hypertext(
                    fixture.npc().objectId(), "dicardingp11", "", List.of())));
            assertEquals(ClassicWorldCkewCatalog.QUEST_COMPLETED,
                    state.storyQuests(fixture.identity()).step(
                            ClassicWorldCkewCatalog.QUEST_ID));
            assertTrue(fixture.inventory().items().isEmpty());
        } finally {
            fixture.close();
            hub.close();
        }
    }

    @Test
    void rewardEquipmentUsesNativeClassSlotsAndRefreshesManaCrystalStats()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub hub = new GameHub(config);
        try {
            Fixture wrongClass = fixture(config, hub,
                    new GameStateStore(temporary.resolve("wrong-class.properties"), false),
                    "WrongRewardClass", 0, 1_201,
                    Map.of(InventoryCatalog.CKEW_KNIGHT_REWARD, 1));
            try {
                RuntimeInventory.Item sword = wrongClass.inventory().items().get(0);
                wrongClass.use(sword.objectId());
                assertTrue(contains(wrongClass.packets(), Protocol.messageCode(264)));
                assertFalse(wrongClass.inventory().item(sword.objectId()).equipped());
            } finally {
                wrongClass.close();
            }

            Fixture knight = fixture(config, hub,
                    new GameStateStore(temporary.resolve("knight-reward.properties"), false),
                    "KnightReward", 1, 1_202,
                    Map.of(InventoryCatalog.CKEW_KNIGHT_REWARD, 1));
            try {
                RuntimeInventory.Item sword = knight.inventory().items().get(0);
                knight.use(sword.objectId());
                List<byte[]> packets = knight.packets();
                RuntimeInventory.Item equippedSword = knight.inventory().item(sword.objectId());
                assertTrue(contains(packets,
                        Protocol.changeInventoryDescription(
                                sword.objectId(), equippedSword.nativeViewName())));
                SourceWeaponPhysicalStats weapon =
                        knight.inventory().equippedPhysicalWeapon();
                assertEquals(56, weapon.itemId());
                assertEquals(16, weapon.damageSmall());
                assertEquals(8, weapon.damageLarge());
                assertEquals(2, weapon.hitModifier());
            } finally {
                knight.close();
            }

            Fixture wizard = fixture(config, hub,
                    new GameStateStore(temporary.resolve("wizard-reward.properties"), false),
                    "WizardReward", 3, 1_203,
                    Map.of(InventoryCatalog.CKEW_WIZARD_REWARD, 1));
            try {
                RuntimeInventory.Item crystal = wizard.inventory().items().get(0);
                wizard.use(crystal.objectId());
                List<byte[]> packets = wizard.packets();
                RuntimeInventory.Item equippedCrystal =
                        wizard.inventory().item(crystal.objectId());
                assertTrue(contains(packets,
                        Protocol.changeInventoryDescription(
                                crystal.objectId(), equippedCrystal.nativeViewName())));
                assertTrue(contains(packets, Protocol.manaPoint(100, 200)));
                assertTrue(contains(packets,
                        Protocol.armorClass(-2, 0, 0, 0, 0)));
                assertEquals(-2, wizard.session().battleArmorClass());
                assertEquals(100, wizard.inventory().equippedNativeArmorBonuses()
                        .maximumMp());
            } finally {
                wizard.close();
            }
        } finally {
            hub.close();
        }
    }

    @Test
    void elfRewardBowConsumesExactRuntimeArrowOverEncryptedFarAttack()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameHub hub = new GameHub(config);
        GameStateStore state = new GameStateStore(
                temporary.resolve("elf-bow-attack.properties"), false);
        Fixture elf = fixture(config, hub, state,
                "ElfBowReward", 2, 1_204,
                Map.of(InventoryCatalog.CKEW_ELF_BOW_REWARD, 1,
                        InventoryCatalog.ARROW, 3));
        try {
            RuntimeInventory.Item bow = elf.inventory().items().stream()
                    .filter(item -> item.definition().catalogId()
                            == InventoryCatalog.CKEW_ELF_BOW_REWARD)
                    .findFirst().orElseThrow();
            RuntimeInventory.Item arrow = elf.inventory().items().stream()
                    .filter(item -> item.definition().catalogId()
                            == InventoryCatalog.ARROW)
                    .findFirst().orElseThrow();
            elf.use(bow.objectId());
            elf.packets();
            SourceWeaponPhysicalStats equipped =
                    elf.inventory().equippedPhysicalWeapon();
            assertEquals(15, equipped.range());
            assertEquals(6, equipped.projectile().damageSmall());
            assertEquals(5, equipped.projectile().damageLarge());

            ArenaMapCatalog.Point bot = elf.enterBattleRoom();
            elf.handle(new Protocol.Writer().c(Protocol.C_FAR_ATTACK)
                    .d(2).h(bot.x()).h(bot.y()).bytes());
            List<byte[]> packets = elf.packets();
            assertTrue(contains(packets,
                    Protocol.changeInventoryCount(arrow.objectId(), 2)));
            assertEquals(2, elf.inventory().item(arrow.objectId()).quantity());
        } finally {
            elf.close();
            hub.close();
        }
    }

    private Fixture fixture(ServerConfig config, GameHub hub,
                            GameStateStore state, String name,
                            int type, long characterObjectId,
                            Map<Long, Integer> inventoryCounts) throws Exception {
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        client.setSoTimeout(100);
        CaptureLog capture = new CaptureLog(
                temporary.resolve("capture-" + name));
        ClientSession session = new ClientSession(server, ClientSession.Endpoint.GAME,
                config, new MemoryAccountRepository("test", "test"),
                state, capture, hub);
        int seed = 0x4500_0000 + type + (int) characterObjectId;
        Protocol.CipherState receiver = new Protocol.CipherState(seed);
        setField(session, "outgoing", new Protocol.CipherState(seed));
        setEnumField(session, "phase", "WORLD");
        setField(session, "account", "completion" + characterObjectId);
        setField(session, "selectedCharacter", name);
        CharacterSummary selected = character(name, type, characterObjectId);
        setField(session, "characters", List.of(selected));
        setField(session, "sourceBaseMagicResistance",
                SourceBaseMagicResistance.from(selected));
        setField(session, "sourceEquipmentMagicStats", SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats", SourceEquipmentVitalStats.none());
        setField(session, "sourcePhysicalGrowthStats",
                SourcePhysicalGrowthStats.classic381());
        setField(session, "sourceInventoryWeightStats",
                SourceInventoryWeightStats.none(1.0));
        setField(session, "retainedSourceInventoryWeight", 0);
        AtomicLong objectIds = new AtomicLong(0x7300_0000L + characterObjectId * 16);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                inventoryCounts, objectIds::incrementAndGet));
        setField(session, "playerHp", 100);
        setField(session, "playerMaxHp", 100);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
        setField(session, "playerClassicLevel", 50);

        ClassicWorldNpcCatalog.StaticNpc npc = ClassicWorldNpcCatalog.all(77).stream()
                .filter(candidate -> candidate.npcId()
                        == ClassicWorldCkewCompletionCatalog.NPC_ID)
                .findFirst().orElseThrow();
        ArenaMapCatalog.Point point = nearbyPassable(npc);
        setField(session, "classicWorldMapId", 77);
        setField(session, "positionX", point.x());
        setField(session, "positionY", point.y());
        hub.connect(session);
        hub.enterClassicWorld(session, 77, point.x(), point.y(), 0);
        return new Fixture(hub, capture, client, server, session,
                receiver, npc, point.x(), point.y());
    }

    private static CharacterSummary character(String name, int type, long objectId) {
        return new CharacterSummary(name, "", type, 0, 50, 100, 100,
                0, 8, 8, 8, 8, 12, 12, 12, 12,
                objectId, 0, 0, 0, 3, 32780, 32800, 77,
                8, 40, 1, 8, 8, 0);
    }

    private static ArenaMapCatalog.Point nearbyPassable(
            ClassicWorldNpcCatalog.StaticNpc target) {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(target.mapId());
        for (int radius = 1; radius <= 3; radius++) {
            for (int x = target.x() - radius; x <= target.x() + radius; x++) {
                for (int y = target.y() - radius; y <= target.y() + radius; y++) {
                    if (Math.max(Math.abs(x - target.x()), Math.abs(y - target.y()))
                            == radius && map.standingPassable(x, y)
                            && !ClassicWorldNpcCatalog.occupies(target.mapId(), x, y)) {
                        return new ArenaMapCatalog.Point(x, y);
                    }
                }
            }
        }
        throw new AssertionError("no passable Dicarding interaction tile");
    }

    private static boolean contains(List<byte[]> packets, byte[] expected) {
        return indexOf(packets, expected) >= 0;
    }

    private static int indexOf(List<byte[]> packets, byte[] expected) {
        for (int index = 0; index < packets.size(); index++) {
            if (java.util.Arrays.equals(packets.get(index), expected)) return index;
        }
        return -1;
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

    private record Choice(int type, String action, int rewardId,
                          String stepThreeHtml, String successHtml,
                          String endedHtml) {}

    private record Fixture(GameHub hub, CaptureLog capture,
                           Socket client, Socket server,
                           ClientSession session,
                           Protocol.CipherState receiver,
                           ClassicWorldNpcCatalog.StaticNpc npc,
                           int x, int y) {
        String identity() { return session.sourceCharacterIdentity(); }

        RuntimeInventory inventory() throws Exception {
            Field field = ClientSession.class.getDeclaredField("inventory");
            field.setAccessible(true);
            return (RuntimeInventory) field.get(session);
        }

        void open() throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "openClassicWorldHypertext", long.class);
            method.setAccessible(true);
            method.invoke(session, npc.objectId());
        }

        void action(String action) throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "handleClassicWorldNpcAction",
                    Protocol.HypertextActionRequest.class);
            method.setAccessible(true);
            method.invoke(session, new Protocol.HypertextActionRequest(
                    npc.objectId(), action, "",
                    Protocol.HypertextActionKind.GENERIC));
        }

        void use(long objectId) throws Exception {
            byte[] payload = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                    .d(objectId).s("").bytes();
            Method method = ClientSession.class.getDeclaredMethod(
                    "useInventoryItem", byte[].class);
            method.setAccessible(true);
            method.invoke(session, (Object) payload);
        }

        ArenaMapCatalog.Point enterBattleRoom() throws Exception {
            hub.leaveClassicWorld(session);
            hub.enterChannel(0, session);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Reward Bow", "", 2, 0, 0, 0, 0, 0));
            hub.joinRoom(roomId, session);
            setField(session, "currentChannelId", 0);
            setField(session, "currentRoomId", roomId);
            setEnumField(session, "phase", "BATTLE");
            ArenaMapCatalog.Point bot = ArenaMapCatalog
                    .forMap(hub.battleMapId(roomId, session)).botSpawn();
            setField(session, "positionX", bot.x() - 1);
            setField(session, "positionY", bot.y());
            return bot;
        }

        void handle(byte[] payload) throws Exception {
            Method method = ClientSession.class.getDeclaredMethod(
                    "handle", byte[].class);
            method.setAccessible(true);
            method.invoke(session, (Object) payload);
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

        void close() throws Exception {
            hub.leaveClassicWorld(session);
            server.close();
            client.close();
            capture.close();
        }
    }
}

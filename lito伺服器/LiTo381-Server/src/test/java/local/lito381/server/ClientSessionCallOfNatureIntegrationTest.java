package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

/** Encrypted session proof for native skill165 Call of Nature. */
@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionCallOfNatureIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @TempDir Path temporary;

    @Test
    void nativeObjectRequestGatesThenResurrectsPlayersInBattleAndWorldAtFullHp()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("call-player-state.properties"), true);
        GameHub hub = new GameHub(config);
        try {
        try (ServerSocket listener = new ServerSocket(
                    0, 2, InetAddress.getByName("127.0.0.1"));
             Socket casterClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket casterServer = listener.accept();
             Socket targetClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket targetServer = listener.accept()) {
            casterClient.setSoTimeout(1_000);
            targetClient.setSoTimeout(1_000);
            ClientSession caster = new ClientSession(
                    casterServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("call-player-caster")), hub);
            ClientSession target = new ClientSession(
                    targetServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("call-player-target")), hub);

            RuntimeInventory casterInventory = baseline();
            assertTrue(casterInventory.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 3));
            initialize(caster, casterInventory);
            initialize(target, baseline());
            CharacterSummary casterElf = character(
                    "CallNatureCaster", 2, 0x3810_1650L);
            CharacterSummary targetKnight = character(
                    "CallNatureTarget", 1, 0x3810_1651L);
            configureCharacter(caster, "call-nature-caster", casterElf);
            configureCharacter(target, "call-nature-target", targetKnight);
            setField(caster, "learnedSkills", Set.of(BattleSpellCatalog.CALL_OF_NATURE));

            hub.connect(caster);
            hub.connect(target);
            hub.enterChannel(0, caster);
            hub.enterChannel(0, target);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Call of Nature", "", 2,
                    BattleRules.Mode.DEATHMATCH.wireId(), 0, 0, 0, 0));
            hub.joinRoom(roomId, caster);
            hub.joinRoom(roomId, target);
            hub.setForce(roomId, caster, 0);
            hub.setForce(roomId, target, 1);
            startBattle(hub, roomId, caster, target);
            setBattleContext(caster, roomId, 32740, 32798, 100, 100);
            setBattleContext(target, roomId, 32741, 32798, 0, 100);
            setField(target, "playerClassicExperience", 0x0012_3456L);

            int casterSeed = 0x3810_1650;
            int targetSeed = 0x3810_1651;
            setField(caster, "outgoing", new Protocol.CipherState(casterSeed));
            setField(target, "outgoing", new Protocol.CipherState(targetSeed));
            Protocol.CipherState casterReceiver = new Protocol.CipherState(casterSeed);
            Protocol.CipherState targetReceiver = new Protocol.CipherState(targetSeed);

            byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(20).c(4).d(target.playerObjectId()).bytes();
            Protocol.ObjectSpellRequest parsed = Protocol.objectSpellRequest(request);
            assertEquals(BattleSpellCatalog.CALL_OF_NATURE, parsed.skillId());
            assertEquals(target.playerObjectId(), parsed.targetObjectId());

            // Retained isAttrAgrees() is silent and precedes resource handling.
            setField(caster, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_EARTH);
            casterClient.setSoTimeout(100);
            invoke(caster, "handle", new Class<?>[] {byte[].class}, request);
            assertThrows(SocketTimeoutException.class,
                    () -> receive(casterClient, casterReceiver));
            assertEquals(100, field(caster, "playerMp"));
            assertEquals(3, crystalCount(caster));

            // A living PC on the corpse cell produces native message 592 and
            // still must not consume MP or the Spirit Crystal.
            casterClient.setSoTimeout(1_000);
            setField(caster, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_WATER);
            setField(caster, "positionX", 32741);
            invoke(caster, "handle", new Class<?>[] {byte[].class}, request);
            byte[] occupied = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MESSAGE_CODE, 4);
            assertEquals(592, unsignedShort(occupied, 1));
            assertEquals(100, field(caster, "playerMp"));
            assertEquals(3, crystalCount(caster));

            setField(caster, "positionX", 32740);
            invoke(caster, "handle", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_ACTION, 8)[5] & 0xFF);
            byte[] battleEffect = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 8);
            assertEquals(target.playerObjectId(), unsignedInt(battleEffect, 1));
            assertEquals(2245, unsignedShort(battleEffect, 5));
            assertEquals(50, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 8), 1));
            assertEquals(2, crystalCount(caster));

            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_ACTION, 8);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_EFFECT, 8);
            assertQuestion322(receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_ASK, 8));
            answer322(target);
            assertEquals(100, field(target, "playerHp"));
            assertEquals(0x0012_3456L, field(target, "playerClassicExperience"));
            assertResurrectionFrames(targetClient, targetReceiver,
                    target.playerObjectId(), 100);
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_RESURRECT, 8);
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_HIT_RATIO, 8);

            int worldMap = ClassicWorldMapCatalog.WORLD_TREE_MAP_ID;
            int worldX = ClassicWorldMapCatalog.WORLD_TREE_X;
            int worldY = ClassicWorldMapCatalog.WORLD_TREE_Y;
            hub.enterClassicWorld(caster, worldMap, worldX, worldY, 5);
            hub.enterClassicWorld(target, worldMap, worldX + 1, worldY, 5);
            setWorldContext(caster, worldMap, worldX, worldY, 100, 100);
            setWorldContext(target, worldMap, worldX + 1, worldY, 0, 100);
            setField(caster, "playerMp", 100);
            invoke(caster, "handle", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_ACTION, 16)[5] & 0xFF);
            byte[] worldEffect = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 8);
            assertEquals(target.playerObjectId(), unsignedInt(worldEffect, 1));
            assertEquals(2245, unsignedShort(worldEffect, 5));
            assertEquals(50, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 8), 1));
            assertEquals(1, crystalCount(caster));

            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_ACTION, 16);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_EFFECT, 8);
            assertQuestion322(receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_ASK, 8));
            answer322(target);
            assertEquals(100, field(target, "playerHp"));
            assertEquals(0x0012_3456L, field(target, "playerClassicExperience"));
            assertEquals("WORLD", field(target, "phase").toString());
            assertEquals(worldMap, field(target, "classicWorldMapId"));
            assertEquals(-1, field(target, "classicWorldInstanceId"));
            assertResurrectionFrames(targetClient, targetReceiver,
                    target.playerObjectId(), 100);
        }
        } finally {
            hub.close();
        }
    }

    @Test
    void nativeCallOfNatureRestoresBattleCompanionAndSelectedWorldMonsterAtFullHp()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("call-npc-state.properties"), true);
        GameHub hub = new GameHub(config);
        try {
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession caster = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("call-npc-caster")), hub);
            RuntimeInventory live = baseline();
            assertTrue(live.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 2));
            initialize(caster, live);
            CharacterSummary casterElf = character(
                    "CallNatureNpcCaster", 2, 0x3810_1652L);
            configureCharacter(caster, "call-nature-npc", casterElf);
            setField(caster, "learnedSkills", Set.of(BattleSpellCatalog.CALL_OF_NATURE));
            setField(caster, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_WATER);

            long roomId = startSoloBattle(hub, caster);
            setBattleContext(caster, roomId, 32740, 32798, 100, 100);
            int seed = 0x3810_1652;
            setField(caster, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            GameStateStore.PetState durablePet = new GameStateStore.PetState(
                    "legacy\u001f10231", 0x7000_1652L, 0x9000_1652L,
                    (int) InventoryCatalog.PET_COLLAR, 45034,
                    "CallNaturePet", 12, 78, 17, 21_375, 0);
            BattleCompanion companion = BattleCompanion.pet(
                    durablePet, SourcePetTemplate.memoryFixture(45034),
                    32741, 32798);
            @SuppressWarnings("unchecked")
            List<BattleCompanion> controlled =
                    (List<BattleCompanion>) field(caster, "tamedCompanions");
            controlled.add(companion);
            int companionMaxHp = companion.snapshot().maxHp();
            companion.applyDamage(companionMaxHp);
            assertEquals(0, companion.snapshot().hp());
            byte[] companionRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(20).c(4).d(companion.snapshot().objectId()).bytes();
            invoke(caster, "handle", new Class<?>[] {byte[].class}, companionRequest);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 8)[5] & 0xFF);
            assertEquals(2245, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(50, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8), 1));
            byte[] companionResurrect = receiveUntilOpcode(
                    client, receiver, Protocol.S_RESURRECT, 8);
            assertEquals(companion.snapshot().objectId(),
                    unsignedInt(companionResurrect, 1));
            receiveUntilOpcode(client, receiver, Protocol.S_HIT_RATIO, 4);
            assertEquals(companionMaxHp, companion.snapshot().hp());
            assertEquals(1, crystalCount(caster));

            int worldMap = ClassicWorldMapCatalog.WORLD_TREE_MAP_ID;
            int worldX = ClassicWorldMapCatalog.WORLD_TREE_X;
            int worldY = ClassicWorldMapCatalog.WORLD_TREE_Y;
            hub.enterClassicWorld(caster, worldMap, worldX, worldY, 5);
            setWorldContext(caster, worldMap, worldX, worldY, 100, 100);
            setField(caster, "playerMp", 100);
            long orcObjectId = 0x6500_1300L;
            ClassicWorldMonsterPopulation.Snapshot orc = hub.classicWorldMonster(
                    worldMap, -1, orcObjectId);
            assertNotNull(orc);
            assertTrue(orc.alive());
            assertTrue(ClassicWorldResurrectionCatalog.allowsSelectedTemplate(
                    orc.npcId(), true));
            setField(caster, "positionX", orc.x() - 1);
            setField(caster, "positionY", orc.y());
            ClassicWorldMonsterPopulation.Hit killed = hub.damageClassicWorldMonster(
                    worldMap, -1, orcObjectId, orc.maximumHp());
            assertNotNull(killed);
            assertTrue(killed.diedNow());
            assertFalse(killed.after().alive());
            assertTrue(killed.after().present());

            byte[] monsterRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(20).c(4).d(orcObjectId).bytes();
            invoke(caster, "handle", new Class<?>[] {byte[].class}, monsterRequest);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 8)[5] & 0xFF);
            assertEquals(2245, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(50, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8), 1));
            byte[] monsterResurrect = receiveUntilOpcode(
                    client, receiver, Protocol.S_RESURRECT, 8);
            assertEquals(orcObjectId, unsignedInt(monsterResurrect, 1));
            receiveUntilOpcode(client, receiver, Protocol.S_HIT_RATIO, 4);
            ClassicWorldMonsterPopulation.Snapshot restored = hub.classicWorldMonster(
                    worldMap, -1, orcObjectId);
            assertNotNull(restored);
            assertTrue(restored.alive());
            assertTrue(restored.present());
            assertEquals(restored.maximumHp(), restored.currentHp());
            assertEquals(orc.x(), restored.x());
            assertEquals(orc.y(), restored.y());
            assertEquals(0, crystalCount(caster));
        }
        } finally {
            hub.close();
        }
    }

    private static void assertQuestion322(byte[] payload) {
        Protocol.Reader in = new Protocol.Reader(payload);
        assertEquals(Protocol.S_ASK, in.c());
        assertEquals(322, in.h());
        assertEquals("", in.s());
        assertEquals(0, in.remaining());
    }

    private static void answer322(ClientSession target) throws Exception {
        invoke(target, "handle", new Class<?>[] {byte[].class},
                new Protocol.Writer().c(Protocol.C_ANSWER).h(322).c(1).s("").bytes());
    }

    private static void assertResurrectionFrames(
            Socket client, Protocol.CipherState receiver,
            long targetObjectId, int expectedHp) throws Exception {
        byte[] resurrect = receiveUntilOpcode(
                client, receiver, Protocol.S_RESURRECT, 8);
        assertEquals(targetObjectId, unsignedInt(resurrect, 1));
        byte[] hp = receiveUntilOpcode(client, receiver, Protocol.S_HIT_POINT, 8);
        assertEquals(expectedHp, unsignedShort(hp, 1));
        assertEquals(expectedHp, unsignedShort(hp, 3));
        // The local HUD is authoritative from S_HIT_POINT. S_HIT_RATIO is
        // observer-only; reflecting it to the local actor makes LiTo.bin keep
        // the player's HP/name overlay visible without mouse hover.
    }

    private static void configureCharacter(
            ClientSession session, String account, CharacterSummary character)
            throws Exception {
        setField(session, "account", account);
        setField(session, "selectedCharacter", character.name());
        setField(session, "inventoryCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "sourceBaseMagicResistance",
                SourceBaseMagicResistance.from(character));
    }

    private static void setBattleContext(
            ClientSession session, long roomId, int x, int y, int hp, int maxHp)
            throws Exception {
        setPhase(session, "BATTLE");
        setField(session, "currentRoomId", roomId);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", hp);
        setField(session, "playerMaxHp", maxHp);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
    }

    private static void setWorldContext(
            ClientSession session, int mapId, int x, int y, int hp, int maxHp)
            throws Exception {
        setPhase(session, "WORLD");
        setField(session, "classicWorldMapId", mapId);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", hp);
        setField(session, "playerMaxHp", maxHp);
    }

    private static void startBattle(
            GameHub hub, long roomId, ClientSession first, ClientSession second)
            throws Exception {
        hub.setReady(roomId, first, true);
        hub.setReady(roomId, second, true);
        Method countdown = GameHub.class.getDeclaredMethod("countdownWork", long.class);
        countdown.setAccessible(true);
        long future = System.nanoTime() + 10_000_000_000L;
        countdown.invoke(hub, future);
        countdown.invoke(hub, future + 2_000_000_000L);
        countdown.invoke(hub, future + 4_000_000_000L);
        assertTrue(hub.battleStarted(roomId));
    }

    private static long startSoloBattle(GameHub hub, ClientSession session)
            throws Exception {
        hub.connect(session);
        hub.enterChannel(0, session);
        long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                "Call of Nature NPC", "", 2,
                BattleRules.Mode.DEATHMATCH.wireId(), 0, 0, 0, 0));
        hub.joinRoom(roomId, session);
        hub.setReady(roomId, session, true);
        Method countdown = GameHub.class.getDeclaredMethod("countdownWork", long.class);
        countdown.setAccessible(true);
        long future = System.nanoTime() + 10_000_000_000L;
        countdown.invoke(hub, future);
        countdown.invoke(hub, future + 2_000_000_000L);
        countdown.invoke(hub, future + 4_000_000_000L);
        assertTrue(hub.battleStarted(roomId));
        return roomId;
    }

    private static RuntimeInventory baseline() {
        RuntimeInventory baseline = new RuntimeInventory();
        baseline.addDistinctState(InventoryCatalog.require(InventoryCatalog.SHORT_SWORD),
                new RuntimeInventory.PersistentItem(
                        0x7000_1650L, 1, 0, 0, true, 1, true, true));
        return baseline;
    }

    private static void initialize(ClientSession session, RuntimeInventory source)
            throws Exception {
        setField(session, "account", "call-nature-test");
        setField(session, "selectedCharacter", "CallNatureElf");
        setField(session, "inventory", source.copy());
        SourceEquipmentMagicStats magic = SourceEquipmentMagicStats.none();
        SourceEquipmentVitalStats vital = SourceEquipmentVitalStats.none();
        SourceEquippedFireResistance fire = new SourceEquippedFireResistance(0, 0);
        setField(session, "sourceEquipmentMagicStats", magic);
        setField(session, "sourceEquipmentVitalStats", vital);
        setField(session, "sourceEquippedFireResistance", fire);
        setField(session, "nativeTournamentMagicBaseline", magic);
        setField(session, "nativeTournamentVitalBaseline", vital);
        setField(session, "nativeTournamentFireResistanceBaseline", fire);
        setField(session, "sourceInventoryWeightStats", SourceInventoryWeightStats.none(100.0));
        setField(session, "retainedSourceInventoryWeight", 0);
        setField(session, "shopPoints", 20_000L);
        setField(session, "nativeTournamentEconomyInitialized", true);
    }

    private static int crystalCount(ClientSession session) throws Exception {
        return inventory(session).catalogCounts().getOrDefault(
                InventoryCatalog.SPIRIT_CRYSTAL, 0);
    }

    private static RuntimeInventory inventory(ClientSession session) throws Exception {
        return (RuntimeInventory) field(session, "inventory");
    }

    private static byte[] receive(Socket client, Protocol.CipherState receiver)
            throws Exception {
        return receiver.decrypt(Protocol.readFrame(client.getInputStream(), 8_192));
    }

    private static byte[] receiveUntilOpcode(
            Socket client, Protocol.CipherState receiver,
            int opcode, int maximumFrames) throws Exception {
        for (int index = 0; index < maximumFrames; index++) {
            byte[] packet = receive(client, receiver);
            if ((packet[0] & 0xFF) == opcode) return packet;
        }
        throw new AssertionError("missing encrypted opcode " + opcode
                + " within " + maximumFrames + " frames");
    }

    private static int unsignedShort(byte[] payload, int offset) {
        return (payload[offset] & 0xFF) | ((payload[offset + 1] & 0xFF) << 8);
    }

    private static long unsignedInt(byte[] payload, int offset) {
        return Integer.toUnsignedLong((payload[offset] & 0xFF)
                | ((payload[offset + 1] & 0xFF) << 8)
                | ((payload[offset + 2] & 0xFF) << 16)
                | ((payload[offset + 3] & 0xFF) << 24));
    }

    private static void invoke(
            ClientSession session, String name, Class<?>[] parameters,
            Object... arguments) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        method.invoke(session, arguments);
    }

    private static Object field(Object target, String name) throws Exception {
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
    private static void setPhase(ClientSession session, String value) throws Exception {
        Class phaseType = Class.forName(ClientSession.class.getName() + "$Phase");
        setField(session, "phase", Enum.valueOf(phaseType, value));
    }

    private static CharacterSummary character(String name, int type, long objectId) {
        return new CharacterSummary(name, "", type, 0, 50, 100, 100,
                10, 12, 12, 12, 12, 18, 12, 18, 12,
                objectId, 100, 0, 0, 0, 32740, 32798, 4,
                12, 40, 1, 12, 12, 0);
    }
}

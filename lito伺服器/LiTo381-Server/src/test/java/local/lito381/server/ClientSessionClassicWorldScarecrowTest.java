package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionClassicWorldScarecrowTest {
    @TempDir Path temporary;

    @Test
    void hitAwardsTrainingExpTurnsThenAttacksWithoutMonsterLifecycle()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("scarecrow-state.properties"), false);
        ClassicWorldNpcCatalog.StaticNpc scarecrow =
                ClassicWorldScarecrowCatalog.forMap(4).stream()
                        .filter(row -> row.npcId() == 45001
                                && row.x() == 33102 && row.y() == 33314)
                        .findFirst().orElseThrow();
        int[] attacker = adjacentAttackPoint(scarecrow);

        try (AccountRepository accounts = AccountRepository.open(config);
             GameHub hub = new GameHub(config, state);
             CaptureLog capture = new CaptureLog(
                     temporary.resolve("scarecrow-capture"));
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(75);
            Protocol.CipherState receiver = new Protocol.CipherState(0x3810_5670);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, accounts,
                    state, capture, hub);
            CharacterSummary source = accounts.characters("test").get(0);
            CharacterSummary levelOne = atLevel(source, 1, 0);
            initialize(session, config.classicServerNames().get(0), levelOne);
            setField(session, "outgoing", new Protocol.CipherState(0x3810_5670));
            prepareWorld(session, scarecrow.mapId(), attacker[0], attacker[1]);
            hub.connect(session);
            hub.enterClassicWorld(
                    session, scarecrow.mapId(), attacker[0], attacker[1], -1);
            drain(client, receiver);

            invokeDirectAttack(session, scarecrow);
            List<byte[]> packets = drain(client, receiver);
            int directionIndex = indexOf(packets, Protocol.S_CHANGE_DIRECTION);
            int attackIndex = indexOf(packets, Protocol.S_ATTACK);
            assertTrue(directionIndex >= 0);
            assertTrue(attackIndex > directionIndex,
                    "retained onAction broadcasts heading before attack.action()");
            assertTrue(contains(packets,
                    new Protocol.Writer().c(Protocol.S_CHANGE_DIRECTION)
                            .d(scarecrow.objectId()).c(5).bytes()));
            assertTrue(containsOpcode(packets, Protocol.S_EXP));
            assertFalse(containsOpcode(packets, Protocol.S_HIT_RATIO));
            assertFalse(containsOpcode(packets, Protocol.S_REMOVE_OBJECT));
            assertEquals(5, hub.classicWorldNpcHeading(scarecrow));
            assertEquals(2L, (long) field(session, "playerClassicExperience"));
            assertNull(hub.classicWorldMonster(
                    scarecrow.mapId(), -1, scarecrow.objectId()));
            assertTrue(ClassicWorldNpcCatalog.npc(
                    scarecrow.mapId(), scarecrow.objectId()) == scarecrow);

            assertTrue(hub.changeClassicWorldNpcHeading(scarecrow, 7));
            invokeDirectAttack(session, scarecrow);
            List<byte[]> wrapPackets = drain(client, receiver);
            assertTrue(contains(wrapPackets,
                    new Protocol.Writer().c(Protocol.S_CHANGE_DIRECTION)
                            .d(scarecrow.objectId()).c(0).bytes()));
            assertEquals(0, hub.classicWorldNpcHeading(scarecrow));
            assertEquals(4L, (long) field(session, "playerClassicExperience"));
            assertNull(hub.classicWorldMonster(
                    scarecrow.mapId(), -1, scarecrow.objectId()));
        }
    }

    private static int[] adjacentAttackPoint(
            ClassicWorldNpcCatalog.StaticNpc target) {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(target.mapId());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int x = target.x() + dx;
                int y = target.y() + dy;
                if (!ClassicWorldNpcCatalog.occupies(target.mapId(), x, y)
                        && map.attackPosition(
                            x, y, target.x(), target.y(), 1)) {
                    return new int[]{x, y};
                }
            }
        }
        throw new AssertionError("no adjacent scarecrow attack tile");
    }

    private static CharacterSummary atLevel(
            CharacterSummary source, int level, long experience) {
        return new CharacterSummary(
                source.name(), source.clanName(), source.type(), source.sex(),
                level, source.maxHp(), source.maxMp(), source.armorClass(),
                source.strength(), source.constitution(), source.dexterity(),
                source.charisma(), source.intelligence(), source.wisdom(),
                source.originalIntelligence(), source.originalWisdom(),
                source.objectId(), experience, source.lawful(), source.pkCount(),
                source.classId(), source.locationX(), source.locationY(), source.mapId(),
                source.originalConstitution(), source.food(),
                source.constitutionHprMaximum(), source.originalStrength(),
                source.originalDexterity(), source.elfAttribute());
    }

    private static void initialize(
            ClientSession session, String source, CharacterSummary character)
            throws Exception {
        setField(session, "account", "test");
        setField(session, "selectedCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "selectedClassicServerName", source);
        setField(session, "playerClassicLevel", character.level());
        setField(session, "playerClassicExperience", character.experience());
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(), () -> 0x7567_0001L));
        setField(session, "learnedSkills", Set.of());
        setField(session, "sourceEquipmentMagicStats",
                SourceEquipmentMagicStats.none());
        setField(session, "sourcePhysicalGrowthStats",
                SourcePhysicalGrowthStats.classic381());
        setField(session, "sourceEquipmentVitalStats",
                SourceEquipmentVitalStats.none());
        setField(session, "sourceEquippedFireResistance",
                new SourceEquippedFireResistance(0, 0));
        setField(session, "sourceBaseMagicResistance",
                SourceBaseMagicResistance.from(character));
        setField(session, "sourceInventoryWeightStats",
                SourceInventoryWeightStats.none(100.0));
        setField(session, "retainedSourceInventoryWeight", 0);
    }

    private static void prepareWorld(
            ClientSession session, int mapId, int x, int y) throws Exception {
        setPhase(session, "WORLD");
        setField(session, "classicWorldMapId", mapId);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", 100);
        setField(session, "playerMaxHp", 100);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void invokeDirectAttack(
            ClientSession session, ClassicWorldNpcCatalog.StaticNpc scarecrow)
            throws Exception {
        Class envelopeType = Class.forName(
                ClientSession.class.getName() + "$AttackEnvelope");
        Object direct = Enum.valueOf(envelopeType, "DIRECT");
        Method method = ClientSession.class.getDeclaredMethod(
                "attackClassicWorld", Protocol.AttackRequest.class, envelopeType);
        method.setAccessible(true);
        method.invoke(session, new Protocol.AttackRequest(
                scarecrow.objectId(), scarecrow.x(), scarecrow.y()), direct);
    }

    private static int indexOf(List<byte[]> packets, int opcode) {
        for (int index = 0; index < packets.size(); index++) {
            if (packets.get(index).length > 0
                    && (packets.get(index)[0] & 0xFF) == opcode) return index;
        }
        return -1;
    }

    private static boolean containsOpcode(List<byte[]> packets, int opcode) {
        return indexOf(packets, opcode) >= 0;
    }

    private static boolean contains(List<byte[]> packets, byte[] expected) {
        return packets.stream().anyMatch(packet ->
                java.util.Arrays.equals(packet, expected));
    }

    private static List<byte[]> drain(
            Socket client, Protocol.CipherState receiver) throws Exception {
        List<byte[]> packets = new ArrayList<>();
        while (true) {
            try {
                packets.add(receiver.decrypt(Protocol.readFrame(
                        client.getInputStream(), 65_535)));
            } catch (SocketTimeoutException done) {
                return packets;
            }
        }
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
    private static void setPhase(ClientSession session, String value)
            throws Exception {
        Class phaseType = Class.forName(ClientSession.class.getName() + "$Phase");
        setField(session, "phase", Enum.valueOf(phaseType, value));
    }
}

package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
final class ClientSessionClassicCastleDoorMagicTest {
    @TempDir Path temporary;

    @Test
    void retainedDoorTemplateIsTheSharedL1DoorNpcBoundary() throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (AccountRepository accounts = AccountRepository.open(config)) {
            SourceNpcPhysicalStats row = accounts.npcPhysicalStats(
                    ClassicWorldCastleDoorCombat.NPC_TEMPLATE_ID);
            assertNotNull(row);
            assertEquals(10, row.armorClass());
            assertEquals(0, row.magicResistance());
            assertEquals(0, row.maxMp());
        }
    }

    @Test
    void directAttackMagicDamagesAndKillsGiranDoorThroughNativeLifecycle()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("castle-door-magic-state.properties"), false);
        String source = config.classicServerNames().get(0);
        try (AccountRepository accounts = AccountRepository.open(config);
             GameHub hub = new GameHub(config, state);
             CaptureLog capture = new CaptureLog(
                     temporary.resolve("castle-door-magic-capture"));
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(75);
            Protocol.CipherState receiver = new Protocol.CipherState(0x3810_5640);
            ClientSession caster = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, accounts,
                    state, capture, hub);
            initialize(caster, source);
            setField(caster, "outgoing",
                    new Protocol.CipherState(0x3810_5640));

            ClassicWorldCastleDoorCombat.View door = hub.mainlandCastleDoors()
                    .stream()
                    .filter(candidate -> candidate.definition().family()
                            == ClassicWorldCastleDoorCombat.Family.GIRAN)
                    .filter(candidate -> candidate.definition().doorId() == 2031)
                    .findFirst().orElseThrow();
            int casterX = door.definition().entranceX();
            int casterY = door.definition().entranceY() + 1;

            hub.connect(caster);
            hub.enterClassicWorld(caster, door.definition().mapId(),
                    casterX, casterY, -1);
            prepareWorld(caster, door.definition().mapId(), casterX, casterY);
            drain(client, receiver);

            int beforeMp = (int) field(caster, "playerMp");
            invokeLightArrow(caster, door.definition().objectId(),
                    door.definition().x(), door.definition().y());

            ClassicWorldCastleDoorCombat.Target afterFirst =
                    hub.mainlandCastleDoorCombatTarget(
                            door.definition().mapId(), door.definition().objectId(),
                            casterX, casterY);
            assertNotNull(afterFirst);
            assertTrue(afterFirst.view().currentHp() < door.currentHp());
            assertTrue(afterFirst.view().currentHp() > door.currentHp() - 100);
            assertTrue((int) field(caster, "playerMp") < beforeMp);
            drain(client, receiver);

            ClassicWorldCastleDoorCombat.Hit primed =
                    hub.damageMainlandCastleDoor(
                            door.definition().family(), door.definition().mapId(),
                            door.definition().objectId(),
                            afterFirst.view().currentHp() - 1);
            assertNotNull(primed);
            assertEquals(1, primed.after().currentHp());
            ((Map<?, ?>) field(caster, "spellReadyNanos")).clear();
            invokeLightArrow(caster, door.definition().objectId(),
                    door.definition().x(), door.definition().y());

            ClassicWorldCastleDoorCombat.View dead = hub.mainlandCastleDoors()
                    .stream()
                    .filter(candidate -> candidate.definition().objectId()
                            == door.definition().objectId())
                    .findFirst().orElseThrow();
            assertEquals(0, dead.currentHp());
            assertTrue(dead.open());
            List<byte[]> packets = drain(client, receiver);
            assertTrue(contains(packets, Protocol.objectAction(
                    door.definition().objectId(), BattleActionCodes.DOOR_DIE)));
            assertTrue(!hub.mainlandCastleDoorBlocks(
                    door.definition().mapId(), door.definition().entranceX(),
                    door.definition().entranceY()));
        }
    }

    private static void initialize(ClientSession session, String source)
            throws Exception {
        CharacterSummary character = character();
        setField(session, "account", "castle-door-mage-account");
        setField(session, "selectedCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "selectedClassicServerName", source);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(), () -> 0x7564_0001L));
        setField(session, "learnedSkills", Set.of(BattleSpellCatalog.LIGHT_ARROW));
        setField(session, "sourceEquipmentMagicStats",
                SourceEquipmentMagicStats.none());
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
        setField(session, "playerHp", 500);
        setField(session, "playerMaxHp", 500);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
    }

    private static CharacterSummary character() {
        return new CharacterSummary("CastleDoorMage", "", 3, 0,
                50, 500, 100, 10, 12, 12, 12, 12, 18, 12, 18, 12,
                0x7564_0000L, 0, 0, 0, 0, 33631, 32736, 4,
                12, 40, 1, 12, 12, 0);
    }

    private static void invokeLightArrow(
            ClientSession caster, long objectId, int x, int y) throws Exception {
        Protocol.AttackSpellRequest request = new Protocol.AttackSpellRequest(
                0, 3, objectId, x, y);
        Method method = ClientSession.class.getDeclaredMethod(
                "useClassicWorldAttackSpell",
                Protocol.AttackSpellRequest.class,
                BattleSpellCatalog.AttackSpell.class);
        method.setAccessible(true);
        method.invoke(caster, request,
                BattleSpellCatalog.attackSpell(BattleSpellCatalog.LIGHT_ARROW));
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

    private static boolean contains(List<byte[]> packets, byte[] expected) {
        return packets.stream().anyMatch(packet ->
                java.util.Arrays.equals(packet, expected));
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

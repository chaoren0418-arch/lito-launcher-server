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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionClassicCastleDoorAreaMagicTest {
    @TempDir Path temporary;

    @Test
    void targetAndCasterCenteredAreaMagicUseNativeDoorLifecycle()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("castle-door-area-magic-state.properties"),
                false);
        String source = config.classicServerNames().get(0);
        try (AccountRepository accounts = AccountRepository.open(config);
             GameHub hub = new GameHub(config, state);
             CaptureLog capture = new CaptureLog(
                     temporary.resolve("castle-door-area-magic-capture"));
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(75);
            Protocol.CipherState receiver = new Protocol.CipherState(0x3810_5650);
            ClientSession caster = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, accounts,
                    state, capture, hub);
            initialize(caster, source);
            setField(caster, "outgoing",
                    new Protocol.CipherState(0x3810_5650));

            ClassicWorldCastleDoorCombat.View door = giranDoor(hub);
            int casterX = door.definition().entranceX();
            int casterY = door.definition().entranceY() + 1;
            hub.connect(caster);
            hub.enterClassicWorld(caster, door.definition().mapId(),
                    casterX, casterY, -1);
            prepareWorld(caster, door.definition().mapId(), casterX, casterY);
            drain(client, receiver);

            List<?> mainlandTargets = doorTargets(
                    caster, 4, casterX, casterY);
            assertEquals(EnumSet.allOf(
                    ClassicWorldCastleDoorCombat.Family.class),
                    doorFamilies(mainlandTargets));
            assertTrue(doorTargets(caster,
                    ClassicWorldDwarfCastleCatalog.MAP_ID,
                    32732, 32844).stream().anyMatch(
                            ClientSessionClassicCastleDoorAreaMagicTest
                                    ::isDwarfTarget));

            int beforeMp = (int) field(caster, "playerMp");
            invokeTargetCentered(caster, door);
            ClassicWorldCastleDoorCombat.View afterTarget = giranDoor(hub);
            assertTrue(afterTarget.currentHp() < door.currentHp());
            assertTrue((int) field(caster, "playerMp") < beforeMp);
            List<byte[]> targetPackets = drain(client, receiver);
            assertTrue(attackManyContains(
                    targetPackets, door.definition().objectId(), 8));

            int beforeCentered = afterTarget.currentHp();
            invokeCasterCentered(caster);
            ClassicWorldCastleDoorCombat.View afterCentered = giranDoor(hub);
            assertTrue(afterCentered.currentHp() < beforeCentered);
            List<byte[]> centeredPackets = drain(client, receiver);
            assertTrue(attackManyContains(
                    centeredPackets, door.definition().objectId(), 0));

            ClassicWorldCastleDoorCombat.Hit primed =
                    hub.damageMainlandCastleDoor(
                            door.definition().family(),
                            door.definition().mapId(),
                            door.definition().objectId(),
                            afterCentered.currentHp() - 1);
            assertNotNull(primed);
            assertEquals(1, primed.after().currentHp());
            ((Map<?, ?>) field(caster, "spellReadyNanos")).clear();
            invokeTargetCentered(caster, door);

            ClassicWorldCastleDoorCombat.View dead = giranDoor(hub);
            assertEquals(0, dead.currentHp());
            assertTrue(dead.open());
            List<byte[]> deathPackets = drain(client, receiver);
            assertTrue(attackManyContains(
                    deathPackets, door.definition().objectId(), 8));
            assertTrue(contains(deathPackets, Protocol.objectAction(
                    door.definition().objectId(),
                    BattleActionCodes.DOOR_DIE)));
            assertEquals(4, deathPackets.stream()
                    .filter(packet -> packet.length > 0
                            && packet[0] == (byte) Protocol.S_CHANGE_ATTR)
                    .count());
            assertTrue(!hub.mainlandCastleDoorBlocks(
                    door.definition().mapId(),
                    door.definition().entranceX(),
                    door.definition().entranceY()));
        }
    }

    private static ClassicWorldCastleDoorCombat.View giranDoor(GameHub hub) {
        return hub.mainlandCastleDoors().stream()
                .filter(candidate -> candidate.definition().family()
                        == ClassicWorldCastleDoorCombat.Family.GIRAN)
                .filter(candidate -> candidate.definition().doorId() == 2031)
                .findFirst().orElseThrow();
    }

    private static void initialize(ClientSession session, String source)
            throws Exception {
        CharacterSummary character = character();
        setField(session, "account", "castle-door-area-mage-account");
        setField(session, "selectedCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "selectedClassicServerName", source);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(), () -> 0x7565_0001L));
        setField(session, "learnedSkills", Set.of(
                BattleSpellCatalog.LIGHTNING_STORM,
                BattleSpellCatalog.FIRE_STORM));
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
        setField(session, "playerMp", 200);
        setField(session, "playerMaxMp", 200);
    }

    private static CharacterSummary character() {
        return new CharacterSummary("CastleDoorAreaMage", "", 3, 0,
                50, 500, 200, 10, 12, 12, 12, 12, 18, 12, 18, 12,
                0x7565_0000L, 0, 0, 0, 0, 33631, 32736, 4,
                12, 40, 1, 12, 12, 0);
    }

    private static void invokeTargetCentered(
            ClientSession caster, ClassicWorldCastleDoorCombat.View door)
            throws Exception {
        BattleSpellCatalog.AreaAttackSpell spell =
                BattleSpellCatalog.areaAttackSpell(
                        BattleSpellCatalog.LIGHTNING_STORM);
        invoke(caster, "useClassicWorldAreaAttackSpell",
                new Class<?>[] {Protocol.AttackSpellRequest.class,
                        BattleSpellCatalog.AreaAttackSpell.class},
                new Protocol.AttackSpellRequest(
                        spell.row(), spell.column(),
                        door.definition().objectId(),
                        door.definition().x(), door.definition().y()), spell);
    }

    private static void invokeCasterCentered(ClientSession caster)
            throws Exception {
        BattleSpellCatalog.AreaAttackSpell spell =
                BattleSpellCatalog.areaAttackSpell(
                        BattleSpellCatalog.FIRE_STORM);
        invoke(caster, "useClassicWorldCenteredAreaAttackSpell",
                new Class<?>[] {byte[].class,
                        BattleSpellCatalog.AreaAttackSpell.class},
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(spell.row()).c(spell.column()).bytes(), spell);
    }

    private static boolean attackManyContains(
            List<byte[]> packets, long objectId, int expectedType) {
        for (byte[] packet : packets) {
            if (packet.length < 22
                    || packet[0] != (byte) Protocol.S_ATTACK_MANY) continue;
            Protocol.Reader reader = new Protocol.Reader(packet);
            reader.c();
            reader.c();
            reader.d();
            reader.h();
            reader.h();
            reader.c();
            reader.d();
            reader.h();
            int type = reader.c();
            if (type == 8) {
                reader.c();
                reader.s();
            } else {
                reader.h();
            }
            int count = reader.h();
            for (int index = 0; index < count; index++) {
                long target = reader.d();
                reader.c();
                if (target == objectId && type == expectedType) return true;
            }
        }
        return false;
    }

    private static boolean contains(List<byte[]> packets, byte[] expected) {
        return packets.stream().anyMatch(packet ->
                java.util.Arrays.equals(packet, expected));
    }

    private static List<?> doorTargets(
            ClientSession caster, int mapId, int x, int y) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "classicCastleDoorMagicTargets",
                int.class, int.class, int.class);
        method.setAccessible(true);
        return (List<?>) method.invoke(caster, mapId, x, y);
    }

    private static Set<ClassicWorldCastleDoorCombat.Family> doorFamilies(
            List<?> targets) throws Exception {
        Set<ClassicWorldCastleDoorCombat.Family> result =
                EnumSet.noneOf(ClassicWorldCastleDoorCombat.Family.class);
        for (Object target : targets) {
            Method family = target.getClass().getDeclaredMethod("family");
            family.setAccessible(true);
            Object value = family.invoke(target);
            if (value instanceof ClassicWorldCastleDoorCombat.Family found) {
                result.add(found);
            }
        }
        return result;
    }

    private static boolean isDwarfTarget(Object target) {
        try {
            Method dwarf = target.getClass().getDeclaredMethod("dwarf");
            dwarf.setAccessible(true);
            return Boolean.TRUE.equals(dwarf.invoke(target));
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
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

    private static void invoke(ClientSession session, String name,
                               Class<?>[] parameters, Object... arguments)
            throws Exception {
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
    private static void setPhase(ClientSession session, String value)
            throws Exception {
        Class phaseType = Class.forName(ClientSession.class.getName() + "$Phase");
        setField(session, "phase", Enum.valueOf(phaseType, value));
    }
}

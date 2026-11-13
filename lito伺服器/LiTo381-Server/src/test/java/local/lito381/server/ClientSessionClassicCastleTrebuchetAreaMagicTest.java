package local.lito381.server;

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
final class ClientSessionClassicCastleTrebuchetAreaMagicTest {
    @TempDir Path temporary;

    @Test
    void targetAndCasterCenteredAreaMagicIncludeDynamicTrebuchet()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("trebuchet-area-magic-state.properties"), false);
        String source = config.classicServerNames().get(0);
        try (AccountRepository accounts = AccountRepository.open(config);
             GameHub hub = new GameHub(config, state);
             CaptureLog capture = new CaptureLog(
                     temporary.resolve("trebuchet-area-magic-capture"));
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(75);
            Protocol.CipherState receiver = new Protocol.CipherState(0x3810_5620);
            ClientSession caster = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, accounts,
                    state, capture, hub);
            initialize(caster, source);
            setField(caster, "outgoing",
                    new Protocol.CipherState(0x3810_5620));

            hub.registerClassicCastleWarSchedules(source, List.of(
                    new CastleWarSchedule(4, "奇岩城",
                            System.currentTimeMillis() - 60_000L, 10, 20)));
            ClassicWorldCastleWarField.WarObject trebuchet =
                    hub.classicCastleWarObjects(source).stream()
                            .filter(object -> object.kind()
                                    == ClassicWorldCastleWarField.Kind.TREBUCHET)
                            .filter(object -> object.npcId()
                                    == ClassicWorldCastleWarField
                                            .ATTACK_TREBUCHET_NPC_ID)
                            .findFirst().orElseThrow();
            int x = trebuchet.x();
            int y = trebuchet.y() - 1;
            hub.connect(caster);
            hub.enterClassicWorld(caster, 4, x, y, -1);
            prepareWorld(caster, x, y);
            drain(client, receiver);

            int beforeTargetCentered = current(
                    hub, source, trebuchet.objectId()).currentHp();
            invokeTargetCentered(caster, trebuchet);
            int afterTargetCentered = current(
                    hub, source, trebuchet.objectId()).currentHp();
            assertTrue(afterTargetCentered < beforeTargetCentered);
            List<byte[]> targetPackets = drain(client, receiver);
            assertTrue(attackManyContains(targetPackets, trebuchet.objectId()));
            assertTrue(containsOpcode(targetPackets, Protocol.S_HIT_RATIO));

            int beforeCasterCentered = afterTargetCentered;
            invokeCasterCentered(caster);
            int afterCasterCentered = current(
                    hub, source, trebuchet.objectId()).currentHp();
            assertTrue(afterCasterCentered < beforeCasterCentered);
            List<byte[]> centeredPackets = drain(client, receiver);
            assertTrue(attackManyContains(centeredPackets, trebuchet.objectId()));
            assertTrue(containsOpcode(centeredPackets, Protocol.S_HIT_RATIO));
        }
    }

    private static void initialize(ClientSession session, String source)
            throws Exception {
        CharacterSummary character = character();
        setField(session, "account", "trebuchet-area-mage-account");
        setField(session, "selectedCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "selectedClassicServerName", source);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(), () -> 0x7562_0001L));
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

    private static void prepareWorld(ClientSession session, int x, int y)
            throws Exception {
        setPhase(session, "WORLD");
        setField(session, "classicWorldMapId", 4);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", 500);
        setField(session, "playerMaxHp", 500);
        setField(session, "playerMp", 200);
        setField(session, "playerMaxMp", 200);
    }

    private static CharacterSummary character() {
        return new CharacterSummary("TrebuchetAreaMage", "", 3, 0,
                50, 500, 200, 10, 12, 12, 12, 12, 18, 12, 18, 12,
                0x7562_0000L, 0, 0, 0, 0, 33630, 32748, 4,
                12, 40, 1, 12, 12, 0);
    }

    private static void invokeTargetCentered(
            ClientSession caster, ClassicWorldCastleWarField.WarObject target)
            throws Exception {
        BattleSpellCatalog.AreaAttackSpell spell =
                BattleSpellCatalog.areaAttackSpell(
                        BattleSpellCatalog.LIGHTNING_STORM);
        invoke(caster, "useClassicWorldAreaAttackSpell",
                new Class<?>[] {Protocol.AttackSpellRequest.class,
                        BattleSpellCatalog.AreaAttackSpell.class},
                new Protocol.AttackSpellRequest(
                        spell.row(), spell.column(), target.objectId(),
                        target.x(), target.y()), spell);
    }

    private static void invokeCasterCentered(ClientSession caster)
            throws Exception {
        BattleSpellCatalog.AreaAttackSpell spell =
                BattleSpellCatalog.areaAttackSpell(BattleSpellCatalog.FIRE_STORM);
        invoke(caster, "useClassicWorldCenteredAreaAttackSpell",
                new Class<?>[] {byte[].class,
                        BattleSpellCatalog.AreaAttackSpell.class},
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(spell.row()).c(spell.column()).bytes(), spell);
    }

    private static ClassicWorldCastleWarField.WarObject current(
            GameHub hub, String source, long objectId) {
        return hub.classicCastleWarObjects(source).stream()
                .filter(object -> object.objectId() == objectId)
                .findFirst().orElseThrow();
    }

    private static boolean attackManyContains(
            List<byte[]> packets, long objectId) {
        for (byte[] packet : packets) {
            if (packet.length < 22 || packet[0] != (byte) Protocol.S_ATTACK_MANY) {
                continue;
            }
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
                if (target == objectId) return true;
            }
        }
        return false;
    }

    private static boolean containsOpcode(List<byte[]> packets, int opcode) {
        return packets.stream().anyMatch(packet -> packet.length > 0
                && packet[0] == (byte) opcode);
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

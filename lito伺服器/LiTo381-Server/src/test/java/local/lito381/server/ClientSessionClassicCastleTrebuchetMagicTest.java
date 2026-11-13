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
final class ClientSessionClassicCastleTrebuchetMagicTest {
    @TempDir Path temporary;

    @Test
    void retainedTrebuchetRowsAreExactInTheLoopbackSourceBoundary()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (AccountRepository accounts = AccountRepository.open(config)) {
            for (int npcId : List.of(20000, 20001)) {
                SourceNpcPhysicalStats row = accounts.npcPhysicalStats(npcId);
                assertNotNull(row);
                assertEquals(-10, row.armorClass());
                assertTrue(row.large());
                assertEquals(0, row.magicResistance());
                assertEquals(20, row.maxMp());
                assertEquals(0, row.strength());
                assertEquals(0, row.dexterity());
                assertEquals(0, row.lawful());
                assertEquals(0, row.ranged());
                assertTrue(!row.boss());
                assertTrue(!row.tamable());
            }
        }
    }

    @Test
    void directAttackMagicDamagesAndKillsDynamicTrebuchetWithoutReward()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("trebuchet-magic-state.properties"), false);
        String source = config.classicServerNames().get(0);
        long warStart = System.currentTimeMillis() - 60_000L;
        try (AccountRepository accounts = AccountRepository.open(config);
             GameHub hub = new GameHub(config, state);
             CaptureLog capture = new CaptureLog(
                     temporary.resolve("trebuchet-magic-capture"));
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(75);
            Protocol.CipherState receiver = new Protocol.CipherState(0x3810_5610);
            ClientSession caster = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, accounts,
                    state, capture, hub);
            initialize(caster, source);
            setField(caster, "outgoing",
                    new Protocol.CipherState(0x3810_5610));

            hub.registerClassicCastleWarSchedules(source, List.of(
                    new CastleWarSchedule(4, "奇岩城", warStart, 10, 20)));
            ClassicWorldCastleWarField.WarObject trebuchet =
                    hub.classicCastleWarObjects(source).stream()
                            .filter(object -> object.kind()
                                    == ClassicWorldCastleWarField.Kind.TREBUCHET)
                            .filter(object -> object.npcId()
                                    == ClassicWorldCastleWarField
                                            .ATTACK_TREBUCHET_NPC_ID)
                            .findFirst().orElseThrow();

            hub.connect(caster);
            hub.enterClassicWorld(caster, 4,
                    trebuchet.x(), trebuchet.y() - 1, -1);
            prepareWorld(caster, trebuchet.x(), trebuchet.y() - 1);
            drain(client, receiver);

            int beforeMp = (int) field(caster, "playerMp");
            invokeAttackSpell(caster, trebuchet);

            ClassicWorldCastleWarField.WarObject afterFirst = current(
                    hub, source, trebuchet.objectId());
            assertTrue(afterFirst.currentHp() < afterFirst.maximumHp());
            assertTrue(afterFirst.currentHp() > afterFirst.maximumHp() - 100);
            assertTrue((int) field(caster, "playerMp") < beforeMp);
            List<byte[]> firstPackets = drain(client, receiver);
            assertTrue(contains(firstPackets, Protocol.hitRatio(
                    trebuchet.objectId(), afterFirst.currentHp(),
                    afterFirst.maximumHp())));

            ClassicWorldCastleWarField.TrebuchetHit primed =
                    hub.damageClassicCastleTrebuchet(source, 4,
                            trebuchet.objectId(), afterFirst.currentHp() - 1);
            assertNotNull(primed);
            assertEquals(1, primed.after().currentHp());
            ((Map<?, ?>) field(caster, "spellReadyNanos")).clear();
            invokeAttackSpell(caster, primed.after());

            ClassicWorldCastleWarField.WarObject dead = current(
                    hub, source, trebuchet.objectId());
            assertEquals(0, dead.currentHp());
            List<byte[]> deathPackets = drain(client, receiver);
            assertTrue(contains(deathPackets, Protocol.hitRatio(
                    trebuchet.objectId(), 0, dead.maximumHp())));
            assertTrue(contains(deathPackets, Protocol.objectAction(
                    trebuchet.objectId(), BattleActionCodes.DIE)));
            assertTrue(((RuntimeInventory) field(caster, "inventory"))
                    .catalogCounts().isEmpty(),
                    "retained rows have no droplist and must create no reward");
            CharacterSummary unchanged = ((List<CharacterSummary>) field(
                    caster, "characters")).get(0);
            assertEquals(0, unchanged.experience(),
                    "retained rows have exp=0 and must create no experience");
        }
    }

    private static void initialize(ClientSession session, String source)
            throws Exception {
        CharacterSummary character = character();
        setField(session, "account", "trebuchet-mage-account");
        setField(session, "selectedCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "selectedClassicServerName", source);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(), () -> 0x7561_0001L));
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

    private static void prepareWorld(ClientSession session, int x, int y)
            throws Exception {
        setPhase(session, "WORLD");
        setField(session, "classicWorldMapId", 4);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", 500);
        setField(session, "playerMaxHp", 500);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
    }

    private static CharacterSummary character() {
        return new CharacterSummary("TrebuchetMage", "", 3, 0,
                50, 500, 100, 10, 12, 12, 12, 12, 18, 12, 18, 12,
                0x7561_0000L, 0, 0, 0, 0, 33630, 32748, 4,
                12, 40, 1, 12, 12, 0);
    }

    private static ClassicWorldCastleWarField.WarObject current(
            GameHub hub, String source, long objectId) {
        return hub.classicCastleWarObjects(source).stream()
                .filter(object -> object.objectId() == objectId)
                .findFirst().orElseThrow();
    }

    private static void invokeAttackSpell(
            ClientSession caster, ClassicWorldCastleWarField.WarObject target)
            throws Exception {
        Protocol.AttackSpellRequest request = new Protocol.AttackSpellRequest(
                0, 3, target.objectId(), target.x(), target.y());
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

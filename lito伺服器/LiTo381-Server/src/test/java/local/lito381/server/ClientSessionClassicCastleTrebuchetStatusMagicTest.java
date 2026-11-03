package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionClassicCastleTrebuchetStatusMagicTest {
    @TempDir Path temporary;

    @Test
    void retainedStatusAndMpSkillsCommitToDynamicTrebuchet() throws Exception {
        Path configPath = temporary.resolve("trebuchet-status.properties");
        Files.writeString(configPath,
                Files.readString(Path.of("src/test/resources/server-config/config/server.selftest.properties"))
                        + System.lineSeparator()
                        + "mind.break.fixture.mp.drain=5" + System.lineSeparator()
                        + "confusion.fixture.d100=1" + System.lineSeparator()
                        + "bone.break.fixture.d1000=801" + System.lineSeparator()
                        + "thunder.grab.fixture.d100=1" + System.lineSeparator()
                        + "thunder.grab.fixture.duration.offset=0"
                        + System.lineSeparator());
        ServerConfig config = ServerConfig.load(configPath);
        GameStateStore state = new GameStateStore(
                temporary.resolve("trebuchet-status-state.properties"), false);
        String source = config.classicServerNames().get(0);
        try (AccountRepository accounts = AccountRepository.open(config);
             GameHub hub = new GameHub(config, state);
             CaptureLog capture = new CaptureLog(
                     temporary.resolve("trebuchet-status-capture"));
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(75);
            Protocol.CipherState receiver = new Protocol.CipherState(0x3810_5630);
            ClientSession caster = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, accounts,
                    state, capture, hub);
            initialize(caster, source);
            setField(caster, "outgoing", new Protocol.CipherState(0x3810_5630));

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
            hub.connect(caster);
            hub.enterClassicWorld(caster, 4,
                    trebuchet.x(), trebuchet.y() - 1, -1);
            prepareWorld(caster, trebuchet.x(), trebuchet.y() - 1);
            drain(client, receiver);

            int beforeMindHp = current(hub, source, trebuchet.objectId())
                    .currentHp();
            invoke(caster, trebuchet, BattleSpellCatalog.MIND_BREAK);
            ClassicWorldMonsterPopulation.Snapshot afterMind =
                    hub.classicCastleTrebuchetCombatSnapshot(
                            source, 4, trebuchet.objectId());
            assertEquals(15, afterMind.currentMp());
            assertTrue(afterMind.currentHp() < beforeMindHp,
                    "accepted retained Mind Break must apply its x4 damage");
            assertTrue(hasOpcode(drain(client, receiver), Protocol.S_ATTACK));

            invoke(caster, trebuchet, BattleSpellCatalog.CONFUSION);
            assertTrue(hub.classicCastleTrebuchetSilenceActive(
                    source, 4, trebuchet.objectId(), System.nanoTime()));
            assertTrue(hasOpcode(drain(client, receiver), Protocol.S_ATTACK));

            invoke(caster, trebuchet, BattleSpellCatalog.BONE_BREAK);
            assertTrue(hub.classicCastleTrebuchetBoneBreakActive(
                    source, 4, trebuchet.objectId(), System.nanoTime()));
            assertTrue(hub.boneBreakEffects(4, -1, System.nanoTime()).stream()
                    .anyMatch(effect -> effect.targetObjectId()
                            == trebuchet.objectId()));
            List<byte[]> bonePackets = drain(client, receiver);
            assertTrue(hasOpcode(bonePackets, Protocol.S_ATTACK));
            assertTrue(hasOpcode(bonePackets, Protocol.S_PUT_OBJECT));

            invoke(caster, trebuchet, BattleSpellCatalog.ARM_BREAKER);
            assertTrue(hub.classicCastleTrebuchetArmBreakerActive(
                    source, 4, trebuchet.objectId(), System.nanoTime()));
            assertTrue(hasOpcode(drain(client, receiver), Protocol.S_ATTACK));

            invoke(caster, trebuchet, BattleSpellCatalog.THUNDER_GRAB);
            assertTrue(hub.classicCastleTrebuchetThunderGrabActive(
                    source, 4, trebuchet.objectId(), System.nanoTime()));
            assertTrue(hasOpcode(drain(client, receiver), Protocol.S_ATTACK));
        }
    }

    private static void initialize(ClientSession session, String source)
            throws Exception {
        CharacterSummary character = character();
        setField(session, "account", "trebuchet-status-mage-account");
        setField(session, "selectedCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "selectedClassicServerName", source);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(), () -> 0x7563_0001L));
        setField(session, "learnedSkills", Set.of(
                BattleSpellCatalog.MIND_BREAK,
                BattleSpellCatalog.CONFUSION,
                BattleSpellCatalog.BONE_BREAK,
                BattleSpellCatalog.ARM_BREAKER,
                BattleSpellCatalog.THUNDER_GRAB));
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
        setField(session, "playerHp", 1_000);
        setField(session, "playerMaxHp", 1_000);
        setField(session, "playerMp", 500);
        setField(session, "playerMaxMp", 500);
    }

    private static CharacterSummary character() {
        return new CharacterSummary("TrebuchetStatusMage", "", 3, 0,
                70, 1_000, 500, 10, 12, 12, 12, 12, 18, 12, 18, 12,
                0x7563_0000L, 0, 0, 0, 0, 33630, 32748, 4,
                12, 40, 1, 12, 12, 0);
    }

    private static ClassicWorldCastleWarField.WarObject current(
            GameHub hub, String source, long objectId) {
        return hub.classicCastleWarObjects(source).stream()
                .filter(object -> object.objectId() == objectId)
                .findFirst().orElseThrow();
    }

    private static void invoke(
            ClientSession caster, ClassicWorldCastleWarField.WarObject target,
            int skillId) throws Exception {
        BattleSpellCatalog.AttackSpell spell =
                BattleSpellCatalog.attackSpell(skillId);
        Protocol.AttackSpellRequest request = new Protocol.AttackSpellRequest(
                spell.row(), spell.column(), target.objectId(),
                target.x(), target.y());
        Method method = ClientSession.class.getDeclaredMethod(
                "useClassicWorldAttackSpell",
                Protocol.AttackSpellRequest.class,
                BattleSpellCatalog.AttackSpell.class);
        method.setAccessible(true);
        method.invoke(caster, request, spell);
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

    private static boolean hasOpcode(List<byte[]> packets, int opcode) {
        return packets.stream().anyMatch(packet -> packet.length > 0
                && packet[0] == (byte) opcode);
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

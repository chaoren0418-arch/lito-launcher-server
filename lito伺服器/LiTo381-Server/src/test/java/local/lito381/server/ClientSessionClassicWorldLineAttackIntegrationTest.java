package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionClassicWorldLineAttackIntegrationTest {
    @TempDir Path temporary;

    @Test
    void lightningUsesAuthoritativeLineIndependentDamageAndPerTargetCounterMagic()
            throws Exception {
        try (Fixture fixture = fixture()) {
            BattleSpellCatalog.LineAttackSpell lightning =
                    BattleSpellCatalog.lineAttackSpell(
                            BattleSpellCatalog.LIGHTNING);
            int primaryBefore = hp(fixture.primary().session());
            int intermediateBefore = hp(fixture.intermediate().session());
            invokeLine(fixture.caster().session(), lightning,
                    fixture.primary().objectId(),
                    fixture.primary().x(), fixture.y());

            assertEquals(80, field(
                    fixture.caster().session(), "playerMp"));
            assertEquals(2, field(
                    fixture.caster().session(), "playerHeading"));
            assertTrue(hp(fixture.primary().session()) < primaryBefore);
            assertTrue(hp(fixture.intermediate().session())
                    < intermediateBefore);

            int primaryAfterFirst = hp(fixture.primary().session());
            int intermediateAfterFirst = hp(
                    fixture.intermediate().session());
            ((BattleTimedStatus) field(
                    fixture.intermediate().session(), "timedStatus"))
                    .activateCounterMagic(System.nanoTime(), 16);
            clearCooldown(fixture.caster().session());
            invokeLine(fixture.caster().session(), lightning,
                    fixture.primary().objectId(),
                    fixture.primary().x(), fixture.y());

            assertEquals(60, field(
                    fixture.caster().session(), "playerMp"));
            assertTrue(hp(fixture.primary().session()) < primaryAfterFirst);
            assertEquals(intermediateAfterFirst,
                    hp(fixture.intermediate().session()));

            String casterFrames = Files.readString(
                    fixture.caster().log().path());
            assertTrue(casterFrames.contains(
                    "S_ATTACK_MANY classic-world area line skill=17"));
            assertTrue(casterFrames.contains("primary="
                    + fixture.primary().objectId()));
            assertTrue(casterFrames.contains("line-cells=7 targets=2"));
            assertTrue(casterFrames.contains("line-cells=7 targets=1"));
            assertTrue(casterFrames.contains("countered=1"));
            assertTrue(casterFrames.contains(
                    "S_EFFECT counter-magic-consumed target="
                            + fixture.intermediate().objectId()
                            + " gfx=10701 source-skill=17"));
            assertTrue(casterFrames.contains(
                    "S_MANA_POINT classic-world line-attack=60/100"));
        }
    }

    private Fixture fixture() throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("line-state.properties"), false);
        GameHub hub = new GameHub(config);
        Corridor corridor = corridor();
        List<Peer> peers = new ArrayList<>();
        Peer caster = peer(config, state, hub, "LineCaster",
                0x3810_2401L, corridor.sourceX(), corridor.y(), true);
        peers.add(caster);
        Peer intermediate = peer(config, state, hub, "LineIntermediate",
                0x3810_2402L, corridor.sourceX() + 3,
                corridor.y(), false);
        peers.add(intermediate);
        Peer primary = peer(config, state, hub, "LinePrimary",
                0x3810_2403L, corridor.sourceX() + 6,
                corridor.y(), false);
        peers.add(primary);
        return new Fixture(hub, caster, intermediate, primary,
                peers, corridor.y());
    }

    private Peer peer(ServerConfig config, GameStateStore state, GameHub hub,
                      String name, long objectId, int x, int y,
                      boolean caster) throws Exception {
        ServerSocket listener = new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        CaptureLog log = new CaptureLog(
                temporary.resolve(name));
        ClientSession session = new ClientSession(
                server, ClientSession.Endpoint.GAME, config, null, state,
                log, hub);
        CharacterSummary character = character(name, objectId, x, y);
        setField(session, "account", name + "-account");
        setField(session, "selectedCharacter", name);
        setField(session, "characters", List.of(character));
        setField(session, "inventory", new RuntimeInventory());
        setField(session, "learnedSkills", caster
                ? Set.of(BattleSpellCatalog.LIGHTNING) : Set.of());
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
        setField(session, "outgoing",
                new Protocol.CipherState((int) objectId));
        hub.connect(session);
        hub.enterClassicWorld(session, 4, x, y, 2);
        prepare(session, x, y);
        return new Peer(session, objectId, x, listener, client, server, log);
    }

    private static void invokeLine(
            ClientSession caster, BattleSpellCatalog.LineAttackSpell spell,
            long targetObjectId, int x, int y) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "useClassicWorldLineAttackSpell", byte[].class,
                BattleSpellCatalog.LineAttackSpell.class);
        method.setAccessible(true);
        method.invoke(caster,
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(spell.row()).c(spell.column())
                        .d(targetObjectId).h(x).h(y).bytes(),
                spell);
    }

    private static Corridor corridor() {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(4);
        for (int y = 32300; y <= 32450; y++) {
            for (int sourceX = 33250; sourceX <= 33450; sourceX++) {
                if (!map.isNormalZone(sourceX, y)
                        || !map.isNormalZone(sourceX + 3, y)
                        || !map.isNormalZone(sourceX + 6, y)
                        || !map.attackPosition(
                                sourceX, y, sourceX + 6, y, 6)) continue;
                return new Corridor(sourceX, y);
            }
        }
        throw new AssertionError(
                "selected map 4 has no retained six-cell line corridor");
    }

    private static void prepare(ClientSession session, int x, int y)
            throws Exception {
        setPhase(session, "WORLD");
        setField(session, "currentRoomId", 0L);
        setField(session, "classicWorldMapId", 4);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", 500);
        setField(session, "playerMaxHp", 500);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
    }

    private static CharacterSummary character(
            String name, long objectId, int x, int y) {
        return new CharacterSummary(name, "", 3, 0, 50, 500, 100,
                10, 12, 12, 12, 12, 19, 12, 18, 12,
                objectId, 100, 0, 0, 0, 0, 0, x, y, 4,
                12, 40, 1, 12, 12, 0);
    }

    private static int hp(ClientSession session) throws Exception {
        return (int) field(session, "playerHp");
    }

    @SuppressWarnings("unchecked")
    private static void clearCooldown(ClientSession session)
            throws Exception {
        ((Map<Integer, Long>) field(
                session, "spellReadyNanos")).clear();
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
        Class phaseType = Class.forName(
                ClientSession.class.getName() + "$Phase");
        setField(session, "phase", Enum.valueOf(phaseType, value));
    }

    private record Corridor(int sourceX, int y) {}

    private record Peer(
            ClientSession session, long objectId, int x,
            ServerSocket listener, Socket client, Socket server,
            CaptureLog log) implements AutoCloseable {
        @Override public void close() throws Exception {
            server.close();
            client.close();
            listener.close();
            log.close();
        }
    }

    private record Fixture(
            GameHub hub, Peer caster, Peer intermediate, Peer primary,
            List<Peer> peers, int y) implements AutoCloseable {
        Fixture {
            peers = List.copyOf(peers);
        }

        @Override public void close() throws Exception {
            hub.close();
            Exception first = null;
            for (Peer peer : peers) {
                try {
                    peer.close();
                } catch (Exception failure) {
                    if (first == null) first = failure;
                }
            }
            if (first != null) throw first;
        }
    }
}

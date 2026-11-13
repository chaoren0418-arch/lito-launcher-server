package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionClassicWorldGroundIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @TempDir Path temporary;

    @Test
    void nativeWorldGroundCastsCommitReducedMpReagentVisualsAndAuthoritativeEffects()
            throws Exception {
        try (Fixture fixture = fixture()) {
            BattleSpellCatalog.GroundSpell fireWall =
                    BattleSpellCatalog.groundSpell(
                            BattleSpellCatalog.FIRE_WALL);
            invokeGround(fixture.caster(), fireWall,
                    fixture.casterX() + 2, fixture.y());
            assertEquals(47, field(fixture.caster(), "playerMp"),
                    "INT19 wizard pays retained circle-reduced MP 53");
            assertEquals(2, field(fixture.caster(), "playerHeading"));
            assertEquals(1, ((RuntimeInventory) field(
                    fixture.caster(), "inventory"))
                    .catalogCounts().get(40_318L));

            int beforeDamage = (int) field(fixture.target(), "playerHp");
            fixture.target().serverClassicWorldGroundPlayerPulse(
                    4, -1, fixture.caster(), BattleSpellCatalog.FIRE_WALL);
            assertTrue((int) field(fixture.target(), "playerHp") < beforeDamage);

            BattleTimedStatus targetTimed = (BattleTimedStatus)
                    field(fixture.target(), "timedStatus");
            assertTrue(targetTimed.activateFreeze(System.nanoTime(), 16));
            int frozenHp = (int) field(fixture.target(), "playerHp");
            fixture.target().serverClassicWorldGroundPlayerPulse(
                    4, -1, fixture.caster(), BattleSpellCatalog.FIRE_WALL);
            assertEquals(frozenHp, field(fixture.target(), "playerHp"),
                    "retained firewall immunity includes freeze");

            setField(fixture.caster(), "playerMp", 100);
            clearCooldown(fixture.caster());
            invokeGround(fixture.caster(), fireWall,
                    fixture.casterX() + 2, fixture.y());
            assertEquals(100, field(fixture.caster(), "playerMp"),
                    "duplicate cells reject before MP commitment");

            setField(fixture.caster(), "playerMp", 47);
            clearCooldown(fixture.caster());
            BattleSpellCatalog.GroundSpell lifeStream =
                    BattleSpellCatalog.groundSpell(
                            BattleSpellCatalog.LIFE_STREAM);
            invokeGround(fixture.caster(), lifeStream,
                    fixture.casterX(), fixture.y());
            assertEquals(34, field(fixture.caster(), "playerMp"),
                    "INT19 wizard pays retained circle-reduced MP 13");
            assertFalse(((RuntimeInventory) field(fixture.caster(), "inventory"))
                    .catalogCounts().containsKey(40_318L));
            long now = System.nanoTime();
            assertTrue(fixture.hub().inClassicWorldLifeStream(
                    4, -1, fixture.caster(),
                    fixture.casterX(), fixture.y(), now));
            assertTrue(fixture.hub().inClassicWorldLifeStream(
                    4, -1, fixture.target(),
                    fixture.targetX(), fixture.y(), now));

            String casterFrames = LocalResources.readString(fixture.casterLog().path());
            assertTrue(casterFrames.contains(
                    "S_CHANGE_DIRECTION classic-world Fire-Wall"));
            assertTrue(casterFrames.contains(
                    "S_ACTION classic-world ground-cast"));
            assertTrue(casterFrames.contains(
                    "S_EFFECT_LOC classic-world skill=58"));
            assertTrue(casterFrames.contains(
                    "S_EFFECT_LOC classic-world skill=63"));
            assertTrue(casterFrames.contains(
                    "S_MANA_POINT classic-world ground-spell"));
        }
    }

    private Fixture fixture() throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("ground-state.properties"), false);
        GameHub hub = new GameHub(config);
        ServerSocket targetListener = listener();
        Socket targetClient = connect(targetListener);
        Socket targetServer = targetListener.accept();
        ServerSocket casterListener = listener();
        Socket casterClient = connect(casterListener);
        Socket casterServer = casterListener.accept();
        CaptureLog targetLog = new CaptureLog(temporary.resolve("target"));
        CaptureLog casterLog = new CaptureLog(temporary.resolve("caster"));
        ClientSession target = new ClientSession(
                targetServer, ClientSession.Endpoint.GAME, config, null, state,
                targetLog, hub);
        ClientSession caster = new ClientSession(
                casterServer, ClientSession.Endpoint.GAME, config, null, state,
                casterLog, hub);
        Corridor corridor = corridor();
        int targetX = corridor.sourceX() + 3;
        initialize(target, character(
                "GroundTarget", 0x3810_2301L, targetX, corridor.y()), false);
        initialize(caster, character(
                "GroundCaster", 0x3810_2302L,
                corridor.sourceX(), corridor.y()), true);
        setField(target, "outgoing", new Protocol.CipherState(0x2301));
        setField(caster, "outgoing", new Protocol.CipherState(0x2302));
        hub.connect(target);
        hub.connect(caster);
        hub.enterClassicWorld(target, 4, targetX, corridor.y(), 5);
        hub.enterClassicWorld(caster, 4, corridor.sourceX(), corridor.y(), 2);
        prepare(target, targetX, corridor.y());
        prepare(caster, corridor.sourceX(), corridor.y());
        return new Fixture(hub, target, caster,
                targetListener, targetClient, targetServer,
                casterListener, casterClient, casterServer,
                targetLog, casterLog, corridor.sourceX(), targetX, corridor.y());
    }

    private static void invokeGround(
            ClientSession caster, BattleSpellCatalog.GroundSpell spell,
            int x, int y) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "useClassicWorldGroundSpell", byte[].class,
                BattleSpellCatalog.GroundSpell.class);
        method.setAccessible(true);
        method.invoke(caster, new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(spell.row()).c(spell.column()).h(x).h(y).bytes(),
                spell);
    }

    private static Corridor corridor() {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(4);
        for (int y = 32300; y <= 32450; y++) {
            for (int sourceX = 33250; sourceX <= 33450; sourceX++) {
                if (!map.contains(sourceX, y)
                        || !map.isNormalZone(sourceX, y)
                        || !map.isNormalZone(sourceX + 3, y)) continue;
                boolean open = true;
                for (int offset = 1; offset <= 8; offset++) {
                    int x = sourceX + offset;
                    if (!map.arrowPassable(x, y)
                            || !map.attackPosition(x - 1, y, x, y, 1)) {
                        open = false;
                        break;
                    }
                }
                if (open) return new Corridor(sourceX, y);
            }
        }
        throw new AssertionError("selected map 4 has no retained 8-cell corridor");
    }

    private static void initialize(
            ClientSession session, CharacterSummary character,
            boolean caster) throws Exception {
        setField(session, "account", character.name() + "-account");
        setField(session, "selectedCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "inventory", caster
                ? RuntimeInventory.fromCatalogCounts(Map.of(40_318L, 1))
                : new RuntimeInventory());
        setField(session, "learnedSkills", caster
                ? Set.of(BattleSpellCatalog.FIRE_WALL,
                        BattleSpellCatalog.LIFE_STREAM)
                : Set.of());
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

    @SuppressWarnings("unchecked")
    private static void clearCooldown(ClientSession session) throws Exception {
        ((Map<Integer, Long>) field(
                session, "spellReadyNanos")).clear();
    }

    private static ServerSocket listener() throws Exception {
        return new ServerSocket(
                0, 1, InetAddress.getByName("127.0.0.1"));
    }

    private static Socket connect(ServerSocket listener) throws Exception {
        return new Socket("127.0.0.1", listener.getLocalPort());
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

    private record Fixture(
            GameHub hub, ClientSession target, ClientSession caster,
            ServerSocket targetListener, Socket targetClient,
            Socket targetServer, ServerSocket casterListener,
            Socket casterClient, Socket casterServer,
            CaptureLog targetLog, CaptureLog casterLog,
            int casterX, int targetX, int y) implements AutoCloseable {
        @Override public void close() throws Exception {
            hub.close();
            targetServer.close();
            targetClient.close();
            targetListener.close();
            casterServer.close();
            casterClient.close();
            casterListener.close();
            targetLog.close();
            casterLog.close();
        }
    }
}

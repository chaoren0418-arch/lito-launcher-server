package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

final class ClientSessionPhantasmIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @TempDir Path temporary;

    @Test void worldSelfTargetRetainsSuccessFailureAndCounterOrdering()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("phantasm-world-state.properties"), false);
        try (GameHub hub = new GameHub(config);
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            ClientSession caster = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("phantasm-world")), hub);
            CharacterSummary character = character();
            initialize(caster, character);
            setField(caster, "learnedSkills", Set.of(BattleSpellCatalog.PHANTASM));
            setField(caster, "outgoing", new Protocol.CipherState(0x2120));
            hub.connect(caster);
            setPhase(caster, "WORLD");
            setField(caster, "classicWorldMapId", 4);
            setField(caster, "classicWorldInstanceId", -1);
            ArenaMapCatalog.Point isolated = isolatedPoint(hub);
            setField(caster, "positionX", isolated.x());
            setField(caster, "positionY", isolated.y());
            setField(caster, "playerHp", 500);
            setField(caster, "playerMaxHp", 500);
            setField(caster, "playerMp", 100);
            setField(caster, "playerMaxMp", 100);

            BattleTimedStatus status = (BattleTimedStatus) field(
                    caster, "timedStatus");
            cast(caster, 1);
            assertEquals(475, field(caster, "playerHp"));
            assertEquals(70, field(caster, "playerMp"));
            assertTrue(status.sleepActive(System.nanoTime()));

            clearCooldown(caster);
            cast(caster, 100);
            assertEquals(450, field(caster, "playerHp"));
            assertEquals(40, field(caster, "playerMp"));
            assertFalse(status.sleepActive(System.nanoTime()),
                    "failed skill212 must remove the old FOG lease first");

            setField(caster, "playerMp", 100);
            clearCooldown(caster);
            long now = System.nanoTime();
            assertTrue(status.activateSleep(now, 5));
            status.activateCounterMagic(now, 32);
            cast(caster, 1);
            assertEquals(425, field(caster, "playerHp"));
            assertEquals(70, field(caster, "playerMp"));
            assertFalse(status.sleepActive(System.nanoTime()),
                    "Counter Magic success must not reinstall sleep");
            assertFalse(status.counterMagicActive(System.nanoTime()));
        }
    }

    @Test void activeEarthDragonEyeLowersSkill212ProbabilityByThree()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("phantasm-dragon-eye-state.properties"), false);
        try (GameHub hub = new GameHub(config);
             ServerSocket listener = new ServerSocket(
                     0, 2, InetAddress.getByName("127.0.0.1"));
             Socket casterClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket casterServer = listener.accept();
             Socket targetClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket targetServer = listener.accept()) {
            ClientSession caster = new ClientSession(
                    casterServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("phantasm-eye-caster")), hub);
            ClientSession target = new ClientSession(
                    targetServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("phantasm-eye-target")), hub);
            CharacterSummary casterCharacter = character(
                    "PhantasmEyeCaster", 0x3810_2120L);
            CharacterSummary targetCharacter = character(
                    "PhantasmEyeTarget", 0x3810_2121L);
            initialize(caster, casterCharacter);
            initialize(target, targetCharacter);
            setField(caster, "learnedSkills", Set.of(BattleSpellCatalog.PHANTASM));
            setField(target, "learnedSkills", Set.of());
            setField(caster, "outgoing", new Protocol.CipherState(0x2120));
            setField(target, "outgoing", new Protocol.CipherState(0x2121));
            hub.connect(caster);
            hub.connect(target);
            setPhase(caster, "WORLD");
            setPhase(target, "WORLD");
            for (ClientSession session : List.of(caster, target)) {
                setField(session, "classicWorldMapId", 4);
                setField(session, "classicWorldInstanceId", -1);
                setField(session, "playerHp", 500);
                setField(session, "playerMaxHp", 500);
                setField(session, "playerMp", 100);
                setField(session, "playerMaxMp", 100);
            }
            ArenaMapCatalog.Point[] pair = isolatedPair(hub);
            setField(caster, "positionX", pair[0].x());
            setField(caster, "positionY", pair[0].y());
            setField(target, "positionX", pair[1].x());
            setField(target, "positionY", pair[1].y());
            hub.enterClassicWorld(caster, 4, pair[0].x(), pair[0].y(), 0, -1);
            hub.enterClassicWorld(target, 4, pair[1].x(), pair[1].y(), 0, -1);
            setField(caster, "lastCombatTargetObjectId",
                    targetCharacter.objectId());

            BattleTimedStatus targetStatus = (BattleTimedStatus) field(
                    target, "timedStatus");
            cast(caster, 33);
            assertTrue(targetStatus.sleepActive(System.nanoTime()),
                    "roll 33 must pass the unresisted native probability 35");

            assertTrue(targetStatus.cancelSleep());
            clearCooldown(caster);
            setField(caster, "playerMp", 100);
            BattleDragonEyeStatus targetEye = (BattleDragonEyeStatus) field(
                    target, "dragonEyeStatus");
            BattleDragonEyeStatus.Activation activation = targetEye.activate(
                    0x7000_0684L,
                    BattleDragonEyeCatalog.require(
                            BattleDragonEyeCatalog.EARTH_DRAGON_EYE),
                    System.nanoTime());
            assertEquals(BattleDragonEyeStatus.Outcome.APPLIED,
                    activation.outcome());
            assertEquals(3, targetEye.snapshot(System.nanoTime()).stoneResistance());

            cast(caster, 33);
            assertFalse(targetStatus.sleepActive(System.nanoTime()),
                    "Earth Eye must reduce probability 35 to 32 so roll 33 fails");
            assertEquals(450, field(caster, "playerHp"));
            assertEquals(70, field(caster, "playerMp"));
        }
    }

    private static void cast(ClientSession caster, int roll) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "usePhantasmSpell", byte[].class,
                BattleSpellCatalog.PhantasmSpell.class, int.class);
        method.setAccessible(true);
        method.invoke(caster, new byte[] {0x14, 0x1A, 0x03},
                BattleSpellCatalog.phantasmSpell(BattleSpellCatalog.PHANTASM),
                roll);
    }

    @SuppressWarnings("unchecked")
    private static void clearCooldown(ClientSession caster) throws Exception {
        ((Map<Integer, Long>) field(caster, "spellReadyNanos")).clear();
    }

    private static void initialize(ClientSession session,
                                   CharacterSummary character) throws Exception {
        setField(session, "account", character.name() + "-account");
        setField(session, "selectedCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(Map.of()));
        setField(session, "sourceEquipmentMagicStats", SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats", SourceEquipmentVitalStats.none());
        setField(session, "sourceEquippedFireResistance",
                new SourceEquippedFireResistance(0, 0));
        setField(session, "sourceBaseMagicResistance",
                SourceBaseMagicResistance.from(character));
        setField(session, "sourceInventoryWeightStats",
                SourceInventoryWeightStats.none(100.0));
        setField(session, "retainedSourceInventoryWeight", 0);
    }

    private static CharacterSummary character() {
        return character("PhantasmCaster", 0x3810_2120L);
    }

    private static CharacterSummary character(String name, long objectId) {
        return new CharacterSummary(name, "", 6, 0, 50, 500, 100,
                10, 12, 12, 12, 12, 12, 12, 12, 12,
                objectId, 100, 0, 0, 0, 33410, 32410, 4,
                12, 40, 1, 12, 12, 0);
    }

    private static ArenaMapCatalog.Point[] isolatedPair(GameHub hub) {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(4);
        for (int x = 33280; x <= 33520; x += 4) {
            for (int y = 32280; y <= 32520; y += 4) {
                int candidateX = x;
                int candidateY = y;
                int targetX = candidateX + 1;
                if (!map.standingPassable(candidateX, candidateY)
                        || !map.standingPassable(targetX, candidateY)
                        || !map.hasLineOfSight(
                            candidateX, candidateY, targetX, candidateY)) continue;
                boolean external = hub.classicWorldMonstersInScreen(
                                4, -1, candidateX, candidateY)
                        .stream().anyMatch(monster -> monster.present()
                                && monster.alive()
                                && Math.max(Math.abs(candidateX - monster.x()),
                                    Math.abs(candidateY - monster.y())) <= 3
                                && map.hasLineOfSight(
                                    candidateX, candidateY,
                                    monster.x(), monster.y()));
                if (!external) {
                    return new ArenaMapCatalog.Point[] {
                        new ArenaMapCatalog.Point(candidateX, candidateY),
                        new ArenaMapCatalog.Point(targetX, candidateY),
                    };
                }
            }
        }
        throw new AssertionError("no isolated map4 Phantasm pair");
    }

    private static ArenaMapCatalog.Point isolatedPoint(GameHub hub) {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(4);
        for (int x = 33280; x <= 33520; x += 4) {
            for (int y = 32280; y <= 32520; y += 4) {
                if (!map.standingPassable(x, y)) continue;
                int candidateX = x;
                int candidateY = y;
                boolean external = hub.classicWorldMonstersInScreen(
                                4, -1, candidateX, candidateY)
                        .stream()
                        .anyMatch(monster -> monster.present() && monster.alive()
                                && Math.max(Math.abs(candidateX - monster.x()),
                                    Math.abs(candidateY - monster.y())) <= 3
                                && map.hasLineOfSight(
                                    candidateX, candidateY,
                                    monster.x(), monster.y()));
                if (!external) {
                    return new ArenaMapCatalog.Point(candidateX, candidateY);
                }
            }
        }
        throw new AssertionError("no isolated map4 Phantasm test cell");
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

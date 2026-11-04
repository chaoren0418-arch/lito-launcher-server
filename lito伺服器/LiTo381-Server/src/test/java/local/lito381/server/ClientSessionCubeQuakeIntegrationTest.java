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

final class ClientSessionCubeQuakeIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @TempDir Path temporary;

    @Test void worldSelectorPlacesAtCasterCommitsOnlyAcceptedCastAndAddsEarthLease()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("cube-quake-world-state.properties"), false);
        try (GameHub hub = new GameHub(config);
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            ClientSession caster = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("cube-quake-world")), hub);
            CharacterSummary character = character();
            initialize(caster, character);
            setField(caster, "learnedSkills",
                    Set.of(BattleSpellCatalog.CUBE_QUAKE));
            setField(caster, "outgoing", new Protocol.CipherState(0x2100));
            hub.connect(caster);
            int mapId = 4;
            int x = 33313;
            int y = 32339;
            hub.enterClassicWorld(caster, mapId, x, y, 5);
            setPhase(caster, "WORLD");
            setField(caster, "classicWorldMapId", mapId);
            setField(caster, "classicWorldInstanceId", -1);
            setField(caster, "positionX", x);
            setField(caster, "positionY", y);
            setField(caster, "playerHp", 500);
            setField(caster, "playerMaxHp", 500);
            setField(caster, "playerMp", 100);
            setField(caster, "playerMaxMp", 100);

            cast(caster);
            RuntimeInventory firstInventory =
                    (RuntimeInventory) field(caster, "inventory");
            assertEquals(65, field(caster, "playerMp"));
            assertEquals(1, firstInventory.catalogCounts().get(49_156L));
            List<BattleCubeQuakeEffects.Cube> first =
                    hub.classicWorldCubeQuakes(mapId, -1, System.nanoTime());
            assertEquals(1, first.size());
            assertEquals(x, first.get(0).x());
            assertEquals(y, first.get(0).y());

            clearCooldown(caster);
            cast(caster);
            assertEquals(65, field(caster, "playerMp"),
                    "nearby rejection precedes MP and reagent commitment");
            assertEquals(1, ((RuntimeInventory) field(caster, "inventory"))
                    .catalogCounts().get(49_156L));

            Method ticks = GameHub.class.getDeclaredMethod(
                    "cubeQuakeTicks", long.class);
            ticks.setAccessible(true);
            ticks.invoke(hub, System.nanoTime());
            assertEquals(30, hub.classicWorldCubeQuakeEarthResistance(
                    mapId, -1, caster, System.nanoTime()));

            int secondX = x + 4;
            assertTrue(hub.updateClassicWorld(caster, mapId, secondX, y, 5));
            setField(caster, "positionX", secondX);
            clearCooldown(caster);
            cast(caster);
            assertEquals(30, field(caster, "playerMp"));
            assertFalse(((RuntimeInventory) field(caster, "inventory"))
                    .catalogCounts().containsKey(49_156L));
            assertEquals(2, hub.classicWorldCubeQuakes(
                    mapId, -1, System.nanoTime()).size());
        }
    }

    private static void cast(ClientSession caster) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "useCubeQuakeSpell", byte[].class,
                BattleSpellCatalog.CubeQuakeSpell.class);
        method.setAccessible(true);
        method.invoke(caster, new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(26).c(1).bytes(),
                BattleSpellCatalog.cubeQuakeSpell(
                        BattleSpellCatalog.CUBE_QUAKE));
    }

    @SuppressWarnings("unchecked")
    private static void clearCooldown(ClientSession session) throws Exception {
        ((Map<Integer, Long>) field(session, "spellReadyNanos")).clear();
    }

    private static void initialize(ClientSession session,
                                   CharacterSummary character) throws Exception {
        setField(session, "account", character.name() + "-account");
        setField(session, "selectedCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(49_156L, 2)));
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

    private static CharacterSummary character() {
        return new CharacterSummary("QuakeCaster", "", 6, 0, 50, 500, 100,
                10, 12, 12, 12, 12, 12, 12, 12, 12,
                0x3810_2100L, 100, 0, 0, 0, 33313, 32339, 4,
                12, 40, 1, 12, 12, 0);
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

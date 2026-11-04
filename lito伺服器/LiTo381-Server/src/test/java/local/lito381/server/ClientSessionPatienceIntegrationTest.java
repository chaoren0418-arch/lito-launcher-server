package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

final class ClientSessionPatienceIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @TempDir Path temporary;

    @Test void worldUniqueNpcReceivesStatusButReductionIsPlayerTargetOnly()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("patience-world-state.properties"), false);
        try (GameHub hub = new GameHub(config);
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            ClientSession caster = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("patience-world")), hub);
            CharacterSummary character = character();
            initialize(caster, character);
            setField(caster, "learnedSkills", Set.of(BattleSpellCatalog.PATIENCE));
            setField(caster, "outgoing", new Protocol.CipherState(0x2110));
            hub.connect(caster);
            setPhase(caster, "WORLD");
            setField(caster, "classicWorldMapId", 4);
            setField(caster, "classicWorldInstanceId", -1);
            setField(caster, "positionX", 33313);
            setField(caster, "positionY", 32339);
            setField(caster, "playerHp", 500);
            setField(caster, "playerMaxHp", 500);
            setField(caster, "playerMp", 100);
            setField(caster, "playerMaxMp", 100);

            List<ClassicWorldMonsterPopulation.Snapshot> nearby =
                    hub.classicWorldMonstersInScreen(4, -1, 33313, 32339)
                    .stream()
                    .filter(monster -> monster.present() && monster.alive())
                    .filter(monster -> Math.max(
                            Math.abs(33313 - monster.x()),
                            Math.abs(32339 - monster.y())) <= 3)
                    .filter(monster -> ClassicWorldMapCatalog.forMap(4)
                            .hasLineOfSight(33313, 32339,
                                    monster.x(), monster.y()))
                    .toList();
            assertEquals(1, nearby.size());
            long npcObjectId = nearby.get(0).objectId();

            cast(caster);
            assertEquals(75, field(caster, "playerMp"));
            assertTrue(hub.classicWorldPatienceActive(
                    4, -1, npcObjectId, System.nanoTime()));
            assertEquals(10, damage(caster, 10, true),
                    "an NPC lease must not leak reduction to the player");
            assertEquals(10, damage(caster, 10, false));
            assertTrue(hub.activateClassicWorldPatience(
                    4, -1, character.objectId(), System.nanoTime(), 600)
                    .accepted());
            assertEquals(8, damage(caster, 10, true));
            assertEquals(10, damage(caster, 10, false));
            assertTrue(hub.cancelClassicWorldPatience(
                    4, -1, character.objectId()));
            assertEquals(10, damage(caster, 10, true));
        }
    }

    private static void cast(ClientSession caster) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "usePatienceSpell", byte[].class,
                BattleSpellCatalog.PatienceSpell.class);
        method.setAccessible(true);
        method.invoke(caster, new byte[] {0x14, 0x1A, 0x02},
                BattleSpellCatalog.patienceSpell(BattleSpellCatalog.PATIENCE));
    }

    private static int damage(ClientSession target, int damage,
                              boolean physical) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "combatDamageAfterCooking", int.class, String.class,
                boolean.class, int.class, boolean.class, boolean.class);
        method.setAccessible(true);
        return (int) method.invoke(target, damage, "patience-test",
                false, 0, false, physical);
    }

    private static void initialize(ClientSession session,
                                   CharacterSummary character) throws Exception {
        setField(session, "account", character.name() + "-account");
        setField(session, "selectedCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "inventory",
                RuntimeInventory.fromCatalogCounts(Map.of()));
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
        return new CharacterSummary("PatienceCaster", "", 6, 0, 50, 500, 100,
                10, 12, 12, 12, 12, 12, 12, 12, 12,
                0x3810_2110L, 100, 0, 0, 0, 33313, 32339, 4,
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

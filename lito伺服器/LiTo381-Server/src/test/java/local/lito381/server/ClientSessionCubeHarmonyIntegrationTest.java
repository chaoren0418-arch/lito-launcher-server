package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionCubeHarmonyIntegrationTest {
    @TempDir Path temporary;

    @Test void worldCastCommitsTwoStonesThenAppliesNativeMpHpClock()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("cube-harmony-world-state.properties"), false);
        try (GameHub hub = new GameHub(config);
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            ClientSession caster = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("cube-harmony-world")), hub);
            CharacterSummary character = character();
            initialize(caster, character);
            setField(caster, "learnedSkills",
                    Set.of(BattleSpellCatalog.CUBE_HARMONY));
            setField(caster, "outgoing", new Protocol.CipherState(0x2200));
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
            setField(caster, "playerMp", 200);
            setField(caster, "playerMaxMp", 200);

            // Drive the authoritative lease clock explicitly below.  Stopping
            // GameHub's background clock prevents a race where it installs
            // the same target status between cast completion and assertions.
            hub.close();
            cast(caster);
            assertEquals(145, field(caster, "playerMp"));
            assertEquals(2, ((RuntimeInventory) field(caster, "inventory"))
                    .catalogCounts().get(49_156L));
            List<BattleCubeHarmonyEffects.Cube> cubes =
                    hub.classicWorldCubeHarmonies(mapId, -1, System.nanoTime());
            assertEquals(1, cubes.size());
            BattleCubeHarmonyEffects.Cube cube = cubes.get(0);
            long placedAt = cube.expiresAtNanos()
                    - TimeUnit.SECONDS.toNanos(
                            BattleCubeHarmonyEffects.DURATION_SECONDS);

            BattleCubeHarmonyEffects effects = harmonyEffects(hub);
            BattleCubeHarmonyEffects.Actor actor =
                    new BattleCubeHarmonyEffects.Actor(
                            caster.playerObjectId(), x, y, true,
                            BattleCubeHarmonyEffects.ActorKind.PLAYER);
            List<BattleCubeHarmonyEffects.Event> applied = effects.advance(
                    placedAt, List.of(actor), (source, target) ->
                            BattleCubeHarmonyEffects.Relation.ALLY);
            assertEquals(BattleCubeHarmonyEffects.EventKind.STATUS_APPLIED,
                    applied.get(0).kind());
            caster.serverCubeHarmonyStatus(0, mapId, -1, caster, applied.get(0));

            BattleCubeHarmonyEffects.Event firstMp = only(effects.advance(
                    placedAt + millis(3_900), List.of(actor),
                    (source, target) -> BattleCubeHarmonyEffects.Relation.ALLY));
            assertEquals(BattleCubeHarmonyEffects.EventKind.MP_RECOVERY,
                    firstMp.kind());
            caster.serverCubeHarmonyMpRecovery(0, mapId, -1, firstMp);
            assertEquals(150, field(caster, "playerMp"));

            BattleCubeHarmonyEffects.Event damage = only(effects.advance(
                    placedAt + millis(4_900), List.of(actor),
                    (source, target) -> BattleCubeHarmonyEffects.Relation.ALLY));
            assertEquals(BattleCubeHarmonyEffects.EventKind.HP_DAMAGE,
                    damage.kind());
            caster.serverCubeHarmonyDamage(0, mapId, -1, caster, damage);
            assertEquals(475, field(caster, "playerHp"));

            BattleCubeHarmonyEffects.Event secondMp = only(effects.advance(
                    placedAt + millis(7_900), List.of(actor),
                    (source, target) -> BattleCubeHarmonyEffects.Relation.ALLY));
            assertEquals(BattleCubeHarmonyEffects.EventKind.MP_RECOVERY,
                    secondMp.kind());
            caster.serverCubeHarmonyMpRecovery(0, mapId, -1, secondMp);
            assertEquals(155, field(caster, "playerMp"));

            clearCooldown(caster);
            cast(caster);
            assertEquals(155, field(caster, "playerMp"),
                    "nearby rejection precedes MP commitment");
            assertEquals(2, ((RuntimeInventory) field(caster, "inventory"))
                    .catalogCounts().get(49_156L));
        }
    }

    private static BattleCubeHarmonyEffects.Event only(
            List<BattleCubeHarmonyEffects.Event> events) {
        assertEquals(1, events.size());
        return events.get(0);
    }

    @SuppressWarnings("unchecked")
    private static BattleCubeHarmonyEffects harmonyEffects(GameHub hub)
            throws Exception {
        Map<?, BattleCubeHarmonyEffects> map =
                (Map<?, BattleCubeHarmonyEffects>) field(
                        hub, "classicWorldCubeHarmonyEffects");
        assertEquals(1, map.size());
        return map.values().iterator().next();
    }

    private static void cast(ClientSession caster) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "useCubeHarmonySpell", byte[].class,
                BattleSpellCatalog.CubeHarmonySpell.class);
        method.setAccessible(true);
        method.invoke(caster, new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(27).c(3).bytes(),
                BattleSpellCatalog.cubeHarmonySpell(
                        BattleSpellCatalog.CUBE_HARMONY));
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
                Map.of(49_156L, 4)));
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
        return new CharacterSummary("HarmonyCaster", "", 6, 0, 50, 500, 200,
                10, 12, 12, 12, 12, 12, 12, 12, 12,
                0x3810_2200L, 100, 0, 0, 0, 33313, 32339, 4,
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

    private static long millis(long value) {
        return TimeUnit.MILLISECONDS.toNanos(value);
    }
}

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

final class ClientSessionInsightIntegrationTest {
    @TempDir Path temporary;

    @Test void battleTargetsPcBypassesCounterMagicConsumesRecastWithoutStack()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("battle-state.properties"), false);
        GameHub hub = new GameHub(config);

        try (ServerSocket targetListener = listener();
             Socket targetClient = connect(targetListener);
             Socket targetServer = targetListener.accept();
             ServerSocket casterListener = listener();
             Socket casterClient = connect(casterListener);
             Socket casterServer = casterListener.accept()) {
            ClientSession target = session(targetServer, config, state, hub,
                    "insight-target");
            ClientSession caster = session(casterServer, config, state, hub,
                    "insight-caster");
            initialize(target, character("InsightTarget", 1, 0x3810_2160L));
            initialize(caster, character("InsightCaster", 6, 0x3810_2161L));
            setField(caster, "learnedSkills", Set.of(BattleSpellCatalog.INSIGHT));
            hub.connect(target);
            hub.connect(caster);
            hub.enterChannel(0, target);
            hub.enterChannel(0, caster);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Insight", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, target);
            hub.joinRoom(roomId, caster);
            prepareBattle(target, roomId, 32741, 32798);
            prepareBattle(caster, roomId, 32740, 32798);
            setField(target, "outgoing", new Protocol.CipherState(0x2160));
            setField(caster, "outgoing", new Protocol.CipherState(0x2161));
            setField(caster, "lastCombatTargetObjectId", target.playerObjectId());

            assertAbilities(target, 12, 12, 12, 12, 12, 12);
            BattleTimedStatus targetTimed =
                    (BattleTimedStatus) field(target, "timedStatus");
            targetTimed.activateCounterMagic(System.nanoTime(), 16);
            cast(caster);

            assertEquals(60, field(caster, "playerMp"));
            assertTrue(targetTimed.counterMagicActive(System.nanoTime()));
            assertAbilities(target, 13, 13, 13, 12, 13, 13);
            BattleAbilityStatus ability =
                    (BattleAbilityStatus) field(target, "abilityStatus");
            assertTrue(ability.insight(System.nanoTime()).remainingSeconds() >= 599);

            clearCooldown(caster);
            cast(caster);
            assertEquals(20, field(caster, "playerMp"));
            assertAbilities(target, 13, 13, 13, 12, 13, 13);
            assertFalse(ability.activateInsight(System.nanoTime(), 600, 1));
            assertTrue(ability.cancelMagicEffects().insight());
            assertAbilities(target, 12, 12, 12, 12, 12, 12);
        }
    }

    @Test void worldSelectorFallsBackToSelfAndCancellationRestoresFiveOnly()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("world-state.properties"), false);
        GameHub hub = new GameHub(config);

        try (ServerSocket listener = listener();
             Socket client = connect(listener);
             Socket server = listener.accept()) {
            ClientSession caster = session(server, config, state, hub,
                    "world-insight-caster");
            initialize(caster, character(
                    "WorldInsightCaster", 6, 0x3810_2162L));
            setField(caster, "learnedSkills", Set.of(BattleSpellCatalog.INSIGHT));
            setField(caster, "outgoing", new Protocol.CipherState(0x2162));
            hub.connect(caster);
            int mapId = 4;
            int x = 33313;
            int y = 32339;
            hub.enterClassicWorld(caster, mapId, x, y, 5);
            prepareWorld(caster, mapId, x, y);

            cast(caster);
            assertEquals(60, field(caster, "playerMp"));
            assertAbilities(caster, 13, 13, 13, 12, 13, 13);
            BattleAbilityStatus ability =
                    (BattleAbilityStatus) field(caster, "abilityStatus");
            assertTrue(ability.cancelMagicEffects().insight());
            assertAbilities(caster, 12, 12, 12, 12, 12, 12);
        }
    }

    private void assertAbilities(ClientSession session, int strength,
                                 int constitution, int dexterity, int charisma,
                                 int intelligence, int wisdom) throws Exception {
        assertEquals(strength, session.battleStrength());
        assertEquals(constitution, invokeInt(session, "effectiveConstitution"));
        assertEquals(dexterity, session.battleDexterity());
        CharacterSummary selected = (CharacterSummary) invoke(
                session, "selectedCharacterSummary", new Class<?>[0]);
        assertEquals(charisma, invokeInt(session, "effectiveCharisma",
                new Class<?>[] {CharacterSummary.class}, selected));
        int baseIntelligence = invokeInt(session, "baseIntelligence");
        SourceEquipmentMagicStats equipment =
                (SourceEquipmentMagicStats) invoke(session,
                        "requiredEquipmentMagicStats", new Class<?>[0]);
        assertEquals(intelligence,
                equipment.effectiveIntelligence(baseIntelligence));
        assertEquals(wisdom, invokeInt(session, "effectiveWisdom",
                new Class<?>[] {long.class}, System.nanoTime()));
    }

    private ClientSession session(Socket socket, ServerConfig config,
                                  GameStateStore state, GameHub hub, String name)
            throws Exception {
        return new ClientSession(socket, ClientSession.Endpoint.GAME, config,
                null, state, new CaptureLog(temporary.resolve(name)), hub);
    }

    private static void cast(ClientSession caster) throws Exception {
        byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(26).c(7).bytes();
        invoke(caster, "useInsightSpell",
                new Class<?>[] {byte[].class,
                        BattleSpellCatalog.InsightSpell.class},
                request, BattleSpellCatalog.insightSpell(
                        BattleSpellCatalog.INSIGHT));
    }

    @SuppressWarnings("unchecked")
    private static void clearCooldown(ClientSession session) throws Exception {
        ((Map<Integer, Long>) field(session, "spellReadyNanos")).clear();
    }

    private static ServerSocket listener() throws Exception {
        return new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
    }

    private static Socket connect(ServerSocket listener) throws Exception {
        return new Socket("127.0.0.1", listener.getLocalPort());
    }

    private static void initialize(ClientSession session,
                                   CharacterSummary character) throws Exception {
        setField(session, "account", character.name() + "-account");
        setField(session, "selectedCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "inventory", new RuntimeInventory());
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

    private static void prepareBattle(ClientSession session, long roomId,
                                      int x, int y) throws Exception {
        setPhase(session, "BATTLE");
        setField(session, "currentRoomId", roomId);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", 500);
        setField(session, "playerMaxHp", 500);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
    }

    private static void prepareWorld(ClientSession session, int mapId,
                                     int x, int y) throws Exception {
        setPhase(session, "WORLD");
        setField(session, "classicWorldMapId", mapId);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", 500);
        setField(session, "playerMaxHp", 500);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
    }

    private static CharacterSummary character(String name, int type,
                                              long objectId) {
        return new CharacterSummary(name, "", type, 0, 50, 500, 100,
                10, 12, 12, 12, 12, 12, 12, 12, 12,
                objectId, 100, 0, 0, 0, 32740, 32798, 4,
                12, 40, 1, 12, 12, 0);
    }

    private static int invokeInt(Object target, String name,
                                 Class<?>[] parameters, Object... arguments)
            throws Exception {
        return (int) invoke(target, name, parameters, arguments);
    }

    private static int invokeInt(Object target, String name) throws Exception {
        return invokeInt(target, name, new Class<?>[0]);
    }

    private static Object invoke(Object target, String name,
                                 Class<?>[] parameters, Object... arguments)
            throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method.invoke(target, arguments);
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

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

final class ClientSessionPanicIntegrationTest {
    @TempDir Path temporary;

    @Test void curseOrderingCounterMagicFiveStatsAndRecastAreExact()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("state.properties"), false);
        try (GameHub hub = new GameHub(config);
             ServerSocket targetListener = listener();
             Socket targetClient = connect(targetListener);
             Socket targetServer = targetListener.accept();
             ServerSocket casterListener = listener();
             Socket casterClient = connect(casterListener);
             Socket casterServer = casterListener.accept()) {
            ClientSession target = session(targetServer, config, state, hub,
                    "panic-target");
            ClientSession caster = session(casterServer, config, state, hub,
                    "panic-caster");
            initialize(target, character("PanicTarget", 1, 0x3810_2170L));
            initialize(caster, character("PanicCaster", 6, 0x3810_2171L));
            setField(caster, "learnedSkills", Set.of(BattleSpellCatalog.PANIC));
            hub.connect(target);
            hub.connect(caster);
            hub.enterChannel(0, target);
            hub.enterChannel(0, caster);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Panic", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, target);
            hub.joinRoom(roomId, caster);
            prepareBattle(target, roomId, 32741, 32798);
            prepareBattle(caster, roomId, 32740, 32798);
            setField(target, "outgoing", new Protocol.CipherState(0x2170));
            setField(caster, "outgoing", new Protocol.CipherState(0x2171));
            setField(caster, "lastCombatTargetObjectId", target.playerObjectId());

            BattleTimedStatus timed = (BattleTimedStatus) field(target, "timedStatus");
            BattleEraseMagicStatus erase =
                    (BattleEraseMagicStatus) field(target, "eraseMagicStatus");
            timed.activateCounterMagic(System.nanoTime(), 16);
            timed.activateSleep(System.nanoTime(), 5);
            erase.activate(System.nanoTime(), 32);
            cast(caster);
            assertEquals(70, field(caster, "playerMp"));
            assertEquals(470, field(caster, "playerHp"));
            assertFalse(timed.counterMagicActive(System.nanoTime()));
            assertFalse(timed.sleepActive(System.nanoTime()));
            assertFalse(erase.active(System.nanoTime()));
            assertAbilities(target, 12, 12, 12, 12, 12, 12);

            clearCooldown(caster);
            cast(caster);
            assertEquals(40, field(caster, "playerMp"));
            assertEquals(440, field(caster, "playerHp"));
            assertAbilities(target, 11, 11, 11, 12, 11, 11);
            BattleAbilityStatus ability =
                    (BattleAbilityStatus) field(target, "abilityStatus");
            int remaining = ability.panic(System.nanoTime()).remainingSeconds();
            assertTrue(remaining >= 63);

            clearCooldown(caster);
            cast(caster);
            assertEquals(10, field(caster, "playerMp"));
            assertEquals(410, field(caster, "playerHp"));
            assertAbilities(target, 11, 11, 11, 12, 11, 11);
            assertTrue(ability.panic(System.nanoTime()).remainingSeconds()
                    <= remaining);
            assertTrue(ability.cancelMagicEffects().panic());
            assertAbilities(target, 12, 12, 12, 12, 12, 12);
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
        assertEquals(intelligence, equipment.effectiveIntelligence(baseIntelligence));
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
                .c(27).c(0).bytes();
        invoke(caster, "usePanicSpell",
                new Class<?>[] {byte[].class,
                        BattleSpellCatalog.PanicSpell.class, int.class},
                request, BattleSpellCatalog.panicSpell(BattleSpellCatalog.PANIC), 1);
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

    private static CharacterSummary character(String name, int type, long objectId) {
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

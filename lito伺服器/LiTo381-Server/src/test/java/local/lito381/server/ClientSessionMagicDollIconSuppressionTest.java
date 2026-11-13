package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;

/**
 * #311 (T110): summoning / recalling a magic doll must never emit opcode FA
 * (selected LiTo.bin S_LITO_CLAN) nor the formal 0.6.71 S_EVENT 7B/56|154
 * icon substitute, while the server-side doll lifecycle stays intact.
 */
@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionMagicDollIconSuppressionTest {
    private static final Pattern FRAME = Pattern.compile(
            "\"direction\":\"server_to_client\",\"opcode\":\"0x([0-9A-F]{2})\""
                    + ",\"wire_length\":\\d+,\"plain\":\"([0-9A-Fa-f ]*)\"");

    @TempDir Path temporary;

    @ParameterizedTest
    @ValueSource(ints = {0, 2184})
    void summonAndRecallSendNoFaOrLegacyDollIcon(int iconId) throws Exception {
        ServerConfig config = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.character-selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("doll-icon-" + iconId + ".properties"), false);
        GameHub hub = new GameHub(config, state);
        int x = 33051;
        int y = 32338;
        try (ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept();
             CaptureLog log = new CaptureLog(temporary.resolve("doll-icon-" + iconId))) {
            ClientSession session = session(server, config, state, log, hub,
                    0x3811_0311L, x, y);
            InventoryCatalog.Definition dollDefinition =
                    new InventoryCatalog.Definition(
                            55_000, 0, 2589, 3963, 1, true,
                            "魔法娃娃：肥肥", "retained source doll", 0, 0,
                            false, true, BattleActionCodes.WALK, 0,
                            InventoryCatalog.Effect.MAGIC_DOLL);
            RuntimeInventory inventory = new RuntimeInventory();
            assertTrue(inventory.add(dollDefinition, 1));
            assertTrue(inventory.add(InventoryCatalog.require(
                    InventoryCatalog.ADENA), 1_500));
            RuntimeInventory.Item dollItem = inventory.items().stream()
                    .filter(item -> item.definition().catalogId() == 55_000)
                    .findFirst().orElseThrow();
            SourceMagicDollType type = new SourceMagicDollType(
                    55_000, 0, iconId, List.of(102),
                    List.of(40_308), List.of(500), 300, 6_100,
                    "魔法娃娃：肥肥");
            setField(session, "inventory", inventory);
            setField(session, "sourceMagicDolls", Map.of(dollItem.objectId(), type));
            setField(session, "outgoing", new Protocol.CipherState(0x0311));
            hub.connect(session);
            hub.enterClassicWorld(session, 4, x, y, 2);

            // Summon.
            invoke(session, "useNativeMagicDoll",
                    new Class<?>[] {RuntimeInventory.Item.class}, dollItem);
            NativeMagicDoll summoned = (NativeMagicDoll) field(session, "activeMagicDoll");
            assertNotNull(summoned, "server-side doll state must be retained");
            assertTrue(summoned.remainingSeconds(System.nanoTime()) > 0);
            @SuppressWarnings("unchecked")
            Set<Long> activeIds = (Set<Long>) field(session, "activeMagicDollObjectIds");
            assertTrue(activeIds.contains(dollItem.objectId()));
            List<String[]> summonFrames = frames(log);
            System.out.println("[#311] icon=" + iconId + " summon opcodes="
                    + opcodes(summonFrames));
            assertTrue(summonFrames.stream().anyMatch(f -> f[0].equals(
                    hex(Protocol.S_PUT_OBJECT))), "doll actor still broadcast");
            assertNoDollIcon(summonFrames);

            // Recall (second use of the same item).
            invoke(session, "useNativeMagicDoll",
                    new Class<?>[] {RuntimeInventory.Item.class}, dollItem);
            assertNull(field(session, "activeMagicDoll"));
            List<String[]> allFrames = frames(log);
            System.out.println("[#311] icon=" + iconId + " recall opcodes="
                    + opcodes(allFrames.subList(summonFrames.size(), allFrames.size())));
            assertNoDollIcon(allFrames);

            // Timer expiry path also stays silent on FA.
            RuntimeInventory.Item sameItem = ((RuntimeInventory) field(
                    session, "inventory")).item(dollItem.objectId());
            invoke(session, "useNativeMagicDoll",
                    new Class<?>[] {RuntimeInventory.Item.class}, sameItem);
            NativeMagicDoll expiring = (NativeMagicDoll) field(session, "activeMagicDoll");
            assertNotNull(expiring);
            session.serverMagicDollTick(expiring.expiresAtNanos());
            assertNull(field(session, "activeMagicDoll"));
            assertNoDollIcon(frames(log));
            assertEquals(500, ((RuntimeInventory) field(session, "inventory"))
                    .catalogCounts().get(InventoryCatalog.ADENA));
        } finally {
            hub.close();
        }
    }

    private static void assertNoDollIcon(List<String[]> frames) {
        for (String[] frame : frames) {
            assertFalse(frame[0].equals("FA"),
                    "opcode FA is S_LITO_CLAN in LiTo.bin: " + frame[1]);
            if (frame[0].equals("7B")) {
                String plain = frame[1].replace(" ", "").toUpperCase();
                assertFalse(plain.startsWith("7B38") || plain.startsWith("7B9A"),
                        "legacy doll icon S_EVENT subtype outside sub_4B07C0 0..40: "
                                + frame[1]);
            }
        }
    }

    private static List<String[]> frames(CaptureLog log) throws Exception {
        List<String[]> frames = new ArrayList<>();
        Matcher matcher = FRAME.matcher(LocalResources.readString(log.path()));
        while (matcher.find()) {
            frames.add(new String[] {matcher.group(1), matcher.group(2)});
        }
        return frames;
    }

    private static String opcodes(List<String[]> frames) {
        return frames.stream().map(f -> f[0]).toList().toString();
    }

    private static String hex(int opcode) {
        return String.format("%02X", opcode & 0xFF);
    }

    private static ClientSession session(
            Socket socket, ServerConfig config, GameStateStore state,
            CaptureLog log, GameHub hub, long objectId, int x, int y) throws Exception {
        ClientSession session = new ClientSession(
                socket, ClientSession.Endpoint.GAME, config, null, state, log, hub);
        CharacterSummary character = new CharacterSummary(
                "Doll" + Long.toHexString(objectId), "", 3, 0, 50, 500, 100,
                10, 12, 12, 12, 12, 19, 12, 18, 12,
                objectId, 100, 0, 0, 0, 0, 0, x, y, 4,
                12, 40, 1, 12, 12, 0);
        setField(session, "account", "doll-icon-player");
        setField(session, "selectedCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "inventory", new RuntimeInventory());
        setField(session, "learnedSkills", Set.of());
        setField(session, "sourceEquipmentMagicStats", SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats", SourceEquipmentVitalStats.none());
        setField(session, "sourceEquippedFireResistance",
                new SourceEquippedFireResistance(0, 0));
        setField(session, "sourceBaseMagicResistance",
                SourceBaseMagicResistance.from(character));
        setField(session, "sourceInventoryWeightStats",
                SourceInventoryWeightStats.none(100.0));
        setField(session, "retainedSourceInventoryWeight", 0);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", 500);
        setField(session, "playerMaxHp", 500);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
        setPhase(session, "WORLD");
        setField(session, "classicWorldMapId", 4);
        setField(session, "classicWorldInstanceId", -1);
        return session;
    }

    private static void invoke(Object target, String name, Class<?>[] types,
                               Object... arguments) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        method.invoke(target, arguments);
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

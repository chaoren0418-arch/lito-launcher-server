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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * #138 B-land (主控裁定第 4 點): replaces both text audits of
 * {@code NativeWorldGroupHealingSourceAuditTest}.  Protected behavior, driven through the real
 * {@code C_USE_SPELL} dispatcher ({@code useSpell}) in ordinary WORLD on the CI self-test profile:
 * <ul>
 *   <li>the selector-only Heal All family is dispatched in WORLD (not rejected as an
 *       unsupported classic-world skill);</li>
 *   <li>it heals a same-map, same-instance clan member on screen, but not a non-clan player
 *       nor a clan member of another instance;</li>
 *   <li>the cast commits MP/HP cost and its reuse delay, so an immediate recast heals nothing.</li>
 * </ul>
 */
@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class NativeWorldGroupHealingBehaviorTest {
    private static final Path CONFIG = Path.of(
            "src/test/resources/server-config/config/server.selftest.properties");
    private static final int MAP = 4;
    private static final int X = ClassicWorldMapCatalog.WORLD_TREE_X;
    private static final int Y = ClassicWorldMapCatalog.WORLD_TREE_Y;

    @TempDir Path temporary;

    @Test
    void worldHealAllHealsOnlySameInstanceClanMembersAndCommitsReuseDelay() throws Exception {
        ServerConfig config = ServerConfig.load(CONFIG);
        GameStateStore state = new GameStateStore(
                temporary.resolve("world-heal-all.properties"), false);
        String source = config.classicServerNames().get(0);
        List<AutoCloseable> sockets = new ArrayList<>();
        try (AccountRepository accounts = AccountRepository.open(config);
             GameHub hub = new GameHub(config, state)) {
            ClientSession caster = peer(sockets, config, accounts, state, hub, "caster", 0x3813_8400);
            ClientSession clanMate = peer(sockets, config, accounts, state, hub, "clan", 0x3813_8401);
            ClientSession stranger = peer(sockets, config, accounts, state, hub, "stranger", 0x3813_8402);
            ClientSession otherInstance = peer(sockets, config, accounts, state, hub, "instance", 0x3813_8403);

            // Four distinct standing-passable cells within three tiles of the world tree (all on screen).
            List<int[]> cells = passableCellsNearWorldTree(4);
            initialize(caster, character("HealAllCaster", 3, 0x3813_8400L), source,
                    cells.get(0)[0], cells.get(0)[1]);
            setField(caster, "learnedSkills", Set.of(BattleSpellCatalog.HEAL_ALL));
            initialize(clanMate, character("HealAllClanMate", 1, 0x3813_8401L), source,
                    cells.get(1)[0], cells.get(1)[1]);
            initialize(stranger, character("HealAllStranger", 1, 0x3813_8402L), source,
                    cells.get(2)[0], cells.get(2)[1]);
            initialize(otherInstance, character("HealAllOtherInstance", 1, 0x3813_8403L),
                    source, cells.get(3)[0], cells.get(3)[1]);
            for (ClientSession session : List.of(caster, clanMate, stranger, otherInstance)) {
                hub.connect(session);
                hub.enterClassicWorld(session, MAP,
                        (int) field(session, "positionX"), (int) field(session, "positionY"), 0);
            }
            // Same hub membership, but the target's own classic-world instance differs.
            setField(otherInstance, "classicWorldInstanceId", 7);

            String owner = caster.sourceCharacterIdentity();
            assertTrue(hub.createClan(owner, "HealAllClan"));
            hub.joinClassicClan(owner, clanMate.sourceCharacterIdentity(), 50);
            hub.joinClassicClan(owner, otherInstance.sourceCharacterIdentity(), 50);
            assertTrue(hub.sameClan(owner, clanMate.sourceCharacterIdentity()));
            assertTrue(hub.sameClan(owner, otherInstance.sourceCharacterIdentity()));
            assertFalse(hub.sameClan(owner, stranger.sourceCharacterIdentity()));

            for (ClientSession target : List.of(clanMate, stranger, otherInstance)) {
                setField(target, "playerHp", 100);
            }
            BattleSpellCatalog.GroupHealingSpell spell =
                    BattleSpellCatalog.groupHealingSpell(BattleSpellCatalog.HEAL_ALL);
            byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(spell.row()).c(spell.column()).bytes();
            assertEquals(BattleSpellCatalog.HEAL_ALL,
                    Protocol.selectorOnlySpellRequest(request).skillId());

            useSpell(caster, request);

            assertTrue((int) field(clanMate, "playerHp") > 100,
                    "same-instance clan member on screen must be healed");
            assertTrue((int) field(clanMate, "playerHp") <= 500);
            assertEquals(100, (int) field(stranger, "playerHp"), "non-clan players are not healed");
            assertEquals(100, (int) field(otherInstance, "playerHp"),
                    "clan members of another instance are not healed");
            assertTrue((int) field(caster, "playerMp") < 100, "MP cost is committed");
            assertEquals(500 - spell.hpCost(), (int) field(caster, "playerHp"),
                    "HP cost is committed");
            @SuppressWarnings("unchecked")
            Map<Integer, Long> ready = (Map<Integer, Long>) field(caster, "spellReadyNanos");
            assertTrue(ready.getOrDefault(BattleSpellCatalog.HEAL_ALL, 0L) > System.nanoTime(),
                    "reuse delay is committed");

            setField(clanMate, "playerHp", 100);
            useSpell(caster, request);
            assertEquals(100, (int) field(clanMate, "playerHp"),
                    "an immediate recast is refused by the committed reuse delay");

            for (ClientSession session : List.of(caster, clanMate, stranger, otherInstance)) {
                hub.leaveClassicWorld(session);
            }
        } finally {
            for (AutoCloseable socket : sockets) socket.close();
        }
    }

    private ClientSession peer(List<AutoCloseable> sockets, ServerConfig config,
                               AccountRepository accounts, GameStateStore state, GameHub hub,
                               String name, int seed) throws Exception {
        ServerSocket listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
        Socket client = new Socket("127.0.0.1", listener.getLocalPort());
        Socket server = listener.accept();
        listener.close();
        sockets.add(client);
        sockets.add(server);
        client.setSoTimeout(50);
        CaptureLog capture = new CaptureLog(temporary.resolve("heal-all-" + name));
        sockets.add(capture);
        ClientSession session = new ClientSession(server, ClientSession.Endpoint.GAME,
                config, accounts, state, capture, hub);
        setField(session, "outgoing", new Protocol.CipherState(seed));
        // Keep the client side readable so the server never blocks on a full socket buffer.
        Thread reader = new Thread(() -> {
            byte[] buffer = new byte[8_192];
            try {
                while (true) {
                    try {
                        if (client.getInputStream().read(buffer) < 0) return;
                    } catch (java.net.SocketTimeoutException idle) {
                        if (client.isClosed()) return;
                    }
                }
            } catch (Exception closed) {
                // socket closed at test end
            }
        }, "heal-all-reader-" + name);
        reader.setDaemon(true);
        reader.start();
        return session;
    }

    private static List<int[]> passableCellsNearWorldTree(int count) {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(MAP);
        List<int[]> cells = new ArrayList<>();
        for (int radius = 0; radius <= 3 && cells.size() < count; radius++) {
            for (int dy = -radius; dy <= radius && cells.size() < count; dy++) {
                for (int dx = -radius; dx <= radius && cells.size() < count; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != radius) continue;
                    if (map.standingPassable(X + dx, Y + dy)) cells.add(new int[] {X + dx, Y + dy});
                }
            }
        }
        assertEquals(count, cells.size(), "world tree neighbourhood must offer passable cells");
        return cells;
    }

    private static void initialize(ClientSession session, CharacterSummary character,
                                   String source, int x, int y) throws Exception {
        setField(session, "account", character.name() + "-account");
        setField(session, "selectedCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "selectedClassicServerName", source);
        setField(session, "inventory", new RuntimeInventory());
        setField(session, "sourceEquipmentMagicStats", SourceEquipmentMagicStats.none());
        setField(session, "sourceEquipmentVitalStats", SourceEquipmentVitalStats.none());
        setField(session, "sourceEquippedFireResistance", new SourceEquippedFireResistance(0, 0));
        setField(session, "sourceBaseMagicResistance", SourceBaseMagicResistance.from(character));
        setField(session, "sourceInventoryWeightStats", SourceInventoryWeightStats.none(100.0));
        setField(session, "retainedSourceInventoryWeight", 0);
        setPhase(session, "WORLD");
        setField(session, "classicWorldMapId", MAP);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", 500);
        setField(session, "playerMaxHp", 500);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
    }

    private static CharacterSummary character(String name, int type, long objectId) {
        return new CharacterSummary(name, "", type, 0,
                50, 500, 100, 10, 12, 12, 12, 12, 18, 12, 18, 12,
                objectId, 0, 0, 0, 0, X, Y, MAP,
                12, 40, 1, 12, 12, 0);
    }

    private static void useSpell(ClientSession caster, byte[] request) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod("useSpell", byte[].class);
        method.setAccessible(true);
        method.invoke(caster, (Object) request);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setPhase(ClientSession session, String value) throws Exception {
        Class phaseType = Class.forName(ClientSession.class.getName() + "$Phase");
        setField(session, "phase", Enum.valueOf(phaseType, value));
    }
}

package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

final class ClientSessionClassicWorldLineAttackMonsterIntegrationTest {
    @TempDir Path temporary;

    @Test
    void lightningDamagesProvokesAndPublishesOrdinaryWorldMonster()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("line-monster-state.properties"), false);
        try (AccountRepository accounts =
                    new MemoryAccountRepository("test", "test");
             GameHub hub = new GameHub(config);
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket(
                     "127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept();
             CaptureLog log = new CaptureLog(
                     temporary.resolve("line-monster"))) {
            ClientSession caster = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, accounts,
                    state, log, hub);
            int casterX = 33313;
            int casterY = 32339;
            CharacterSummary character = character(casterX, casterY);
            initialize(caster, character);
            setField(caster, "outgoing",
                    new Protocol.CipherState(0x2404));
            hub.connect(caster);
            hub.enterClassicWorld(caster, 4, casterX, casterY, 2);
            prepare(caster, casterX, casterY);

            ClassicWorldMapCatalog.ClassicMap map =
                    ClassicWorldMapCatalog.forMap(4);
            ClassicWorldMonsterPopulation.Snapshot target =
                    hub.classicWorldMonstersInScreen(
                            4, -1, casterX, casterY).stream()
                    .filter(candidate -> candidate.present()
                            && candidate.alive())
                    .filter(candidate -> {
                        int range = Math.max(
                                Math.abs(candidate.x() - casterX),
                                Math.abs(candidate.y() - casterY));
                        return range > 0 && range <= 6;
                    })
                    .filter(candidate -> map.attackPosition(
                            casterX, casterY,
                            candidate.x(), candidate.y(), 6))
                    .filter(candidate -> {
                        try {
                            return accounts.npcPhysicalStats(
                                    candidate.npcId()) != null;
                        } catch (Exception ignored) {
                            return false;
                        }
                    })
                    .findFirst().orElseThrow();
            assertTrue(hub.freezeClassicWorldMonster(
                    4, -1, target.objectId(), System.nanoTime(), 16));
            target = hub.classicWorldMonster(4, -1, target.objectId());
            assertNotNull(target);
            int hpBefore = target.currentHp();

            BattleSpellCatalog.LineAttackSpell lightning =
                    BattleSpellCatalog.lineAttackSpell(
                            BattleSpellCatalog.LIGHTNING);
            Method method = ClientSession.class.getDeclaredMethod(
                    "useClassicWorldLineAttackSpell", byte[].class,
                    BattleSpellCatalog.LineAttackSpell.class);
            method.setAccessible(true);
            method.invoke(caster,
                    new Protocol.Writer().c(Protocol.C_USE_SPELL)
                            .c(lightning.row()).c(lightning.column())
                            .d(target.objectId()).h(target.x()).h(target.y())
                            .bytes(),
                    lightning);

            ClassicWorldMonsterPopulation.Snapshot after =
                    hub.classicWorldMonster(4, -1, target.objectId());
            assertNotNull(after);
            assertTrue(after.currentHp() < hpBefore);
            assertEquals(80, field(caster, "playerMp"));
            String frames = Files.readString(log.path());
            assertTrue(frames.contains(
                    "S_ATTACK_MANY classic-world area line skill=17"));
            assertTrue(frames.contains("primary=" + target.objectId()));
            assertTrue(frames.contains("targets=1"));
            assertTrue(frames.contains(
                    "S_HIT_RATIO classic-world-monster="
                            + target.objectId()));
            assertTrue(frames.contains("source=spell-17"));
        }
    }

    private static void initialize(
            ClientSession session, CharacterSummary character)
            throws Exception {
        setField(session, "account", "line-monster-account");
        setField(session, "selectedCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "inventory", new RuntimeInventory());
        setField(session, "learnedSkills",
                Set.of(BattleSpellCatalog.LIGHTNING));
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
        setField(session, "sourcePhysicalGrowthStats",
                SourcePhysicalGrowthStats.classic381());
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

    private static CharacterSummary character(int x, int y) {
        return new CharacterSummary(
                "LineMonsterCaster", "", 3, 0, 50, 500, 100,
                10, 12, 12, 12, 12, 19, 12, 18, 12,
                0x3810_2404L, 100, 0, 0, 0, 0, 0, x, y, 4,
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
        Class phaseType = Class.forName(
                ClientSession.class.getName() + "$Phase");
        setField(session, "phase", Enum.valueOf(phaseType, value));
    }
}

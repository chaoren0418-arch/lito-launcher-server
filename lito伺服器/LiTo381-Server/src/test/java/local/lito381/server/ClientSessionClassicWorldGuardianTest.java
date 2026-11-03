package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionClassicWorldGuardianTest {
    @TempDir Path temporary;

    @Test
    void nativeSessionSpawnsTalksHarvestsIgnoresUnarmedDamageAndAbsolvesElfPk()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("guardian-state.properties"), false);
        ClassicWorldNpcCatalog.StaticNpc sourceGuardian =
                ClassicWorldGuardianCatalog.all().stream()
                        .filter(row -> row.spawnId() == 807)
                        .findFirst().orElseThrow();

        try (AccountRepository accounts = AccountRepository.open(config);
             GameHub hub = new GameHub(config, state);
             CaptureLog capture = new CaptureLog(
                     temporary.resolve("guardian-capture"));
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(75);
            Protocol.CipherState receiver = new Protocol.CipherState(0x3810_5700);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, accounts,
                    state, capture, hub);
            CharacterSummary elf = accounts.characters("test5").get(0);
            assertEquals(2, elf.type());
            initialize(session, config.classicServerNames().get(0), elf, "test5");
            setField(session, "outgoing", new Protocol.CipherState(0x3810_5700));

            ClassicWorldMonsterPopulation.Snapshot guardian =
                    hub.classicWorldMonster(4, -1, sourceGuardian.objectId());
            assertNotNull(guardian);
            int[] point = adjacentAttackPoint(hub, guardian);
            prepareWorld(session, point[0], point[1]);
            hub.connect(session);
            hub.enterClassicWorld(session, 4, point[0], point[1], -1);
            invokePerceptionRebuild(session);
            List<byte[]> bootstrap = drain(client, receiver);
            assertEquals(1, bootstrap.stream()
                    .filter(packet -> putObjectId(packet) == guardian.objectId())
                    .count(), "dynamic guardian must replace the static NPC row exactly once");

            invokeOpenHypertext(session, guardian.objectId());
            List<byte[]> talk = drain(client, receiver);
            int heading = heading(guardian.x(), guardian.y(), point[0], point[1]);
            assertTrue(contains(talk, Protocol.changeDirection(
                    guardian.objectId(), heading)));
            assertTrue(contains(talk, Protocol.hypertext(
                    guardian.objectId(), "ente1", "", List.of())));

            setField(session, "playerLawful", -1_001);
            invokeOpenHypertext(session, guardian.objectId());
            List<byte[]> chaoticTalk = drain(client, receiver);
            assertTrue(contains(chaoticTalk, Protocol.hypertext(
                    guardian.objectId(), "entm1", "", List.of())));
            setField(session, "playerLawful", 0);

            assertTrue(invokeHarvest(session, guardian, 10));
            List<byte[]> harvest = drain(client, receiver);
            RuntimeInventory inventory = (RuntimeInventory) field(session, "inventory");
            assertEquals(1, inventory.catalogCounts().getOrDefault(40506L, 0));
            assertTrue(contains(harvest,
                    Protocol.messageCode(143, "$755", "$794")));

            ClassicWorldMonsterPopulation.Snapshot before =
                    hub.classicWorldMonster(4, -1, guardian.objectId());
            invokeDirectAttack(session, guardian);
            List<byte[]> attack = drain(client, receiver);
            ClassicWorldMonsterPopulation.Snapshot after =
                    hub.classicWorldMonster(4, -1, guardian.objectId());
            assertTrue(hasAttack(attack, session.playerObjectId(), guardian.objectId()));
            assertFalse(hasHitRatioFor(attack, guardian.objectId()));
            assertEquals(before.currentHp(), after.currentHp(),
                    "retained unarmed elf commit must not damage or provoke Guardian");

            session.serverClassicWorldGuardianWarning(
                    guardian, ClassicWorldGuardianCatalog.TargetDisposition.INTRUDER);
            assertTrue(contains(drain(client, receiver), Protocol.sayCode(
                    2, guardian.objectId(), "<$755> $804",
                    guardian.x(), guardian.y())));

            long now = state.currentEpochSeconds();
            String stateKey = invokeStateCharacter(session);
            state.reconcileElfPk(stateKey, 2, now - 60);
            setField(session, "elfPkCount", 2);
            setField(session, "lastPkForElfEpochSeconds", now - 60);
            assertEquals(ClassicWorldGuardianCatalog.TargetDisposition.ELF_MURDERER,
                    session.classicWorldGuardianDisposition());
            session.serverClassicWorldGuardianWarning(
                    guardian, ClassicWorldGuardianCatalog.TargetDisposition.ELF_MURDERER);
            assertTrue(contains(drain(client, receiver), Protocol.sayCode(
                    0, guardian.objectId(), "$755: $815",
                    guardian.x(), guardian.y())));

            setField(session, "playerHp", 1);
            invokeClassicWorldDirectDamage(session, 1.0, guardian.objectId());
            List<byte[]> death = drain(client, receiver);
            assertEquals(0, (int) field(session, "playerHp"));
            assertEquals(1, (int) field(session, "elfPkCount"));
            assertEquals(0L, (long) field(session, "lastPkForElfEpochSeconds"));
            assertTrue(hasAction(death, session.playerObjectId(),
                    BattleActionCodes.DIE));
            GameStateStore.ElfPkBalance durable =
                    state.reconcileElfPk(stateKey, 2, now - 60);
            assertEquals(new GameStateStore.ElfPkState(1, 0), durable.effective());
        }
    }

    @Test
    void aggressiveAiAcquiresInvisibleIntruderWarnsOnceAndRetainsTarget()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("guardian-ai-state.properties"), false);
        ClassicWorldNpcCatalog.StaticNpc sourceGuardian =
                ClassicWorldGuardianCatalog.all().stream()
                        .filter(row -> row.spawnId() == 807)
                        .findFirst().orElseThrow();

        try (AccountRepository accounts = AccountRepository.open(config);
             GameHub hub = new GameHub(config, state);
             CaptureLog capture = new CaptureLog(
                     temporary.resolve("guardian-ai-capture"));
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(75);
            Protocol.CipherState receiver = new Protocol.CipherState(0x3810_5701);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, accounts,
                    state, capture, hub);
            CharacterSummary knight = accounts.characters("test").get(0);
            initialize(session, config.classicServerNames().get(0), knight, "test");
            setField(session, "outgoing", new Protocol.CipherState(0x3810_5701));

            ClassicWorldMonsterPopulation.Snapshot guardian =
                    hub.classicWorldMonster(4, -1, sourceGuardian.objectId());
            int[] point = adjacentAttackPoint(hub, guardian);
            prepareWorld(session, point[0], point[1]);
            setField(session, "playerHp", 10_000);
            setField(session, "playerMaxHp", 10_000);
            setField(session, "invisible", true);
            hub.connect(session);
            hub.enterClassicWorld(session, 4, point[0], point[1], -1);
            drain(client, receiver);

            setBooleanField(hub, "classicWorldMonsterAggressiveAiEnabled", true);
            invokeMonsterTick(hub, 1L);
            byte[] exactWarning = Protocol.sayCode(
                    2, guardian.objectId(), "<$755> $804",
                    guardian.x(), guardian.y());
            assertTrue(contains(drain(client, receiver), exactWarning),
                    "agrocoi Guardian must acquire an invisible non-elf intruder");
            assertEquals(session.playerObjectId(), invokeGuardianTarget(hub, guardian));

            invokeMonsterTick(hub, 1L);
            assertFalse(contains(drain(client, receiver), exactWarning),
                    "retained target must not repeat the acquisition warning every tick");
        }
    }

    private static int[] adjacentAttackPoint(
            GameHub hub, ClassicWorldMonsterPopulation.Snapshot target) {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(target.mapId());
        List<ClassicWorldMonsterPopulation.Snapshot> nearby =
                hub.classicWorldMonstersInScreen(
                        target.mapId(), -1, target.x(), target.y());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int x = target.x() + dx;
                int y = target.y() + dy;
                boolean occupied = nearby.stream().anyMatch(actor ->
                        actor.present() && actor.x() == x && actor.y() == y);
                if (!occupied && !ClassicWorldNpcCatalog.occupies(
                                target.mapId(), x, y)
                        && map.attackPosition(x, y, target.x(), target.y(), 1)) {
                    return new int[] {x, y};
                }
            }
        }
        throw new AssertionError("no adjacent Guardian attack tile");
    }

    private static void initialize(
            ClientSession session, String source, CharacterSummary character,
            String account)
            throws Exception {
        setField(session, "account", account);
        setField(session, "selectedCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "selectedClassicServerName", source);
        setField(session, "playerClassicLevel", character.level());
        setField(session, "playerClassicExperience", character.experience());
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(), () -> 0x7570_0001L));
        setField(session, "learnedSkills", Set.of());
        setField(session, "sourceEquipmentMagicStats",
                SourceEquipmentMagicStats.none());
        setField(session, "sourcePhysicalGrowthStats",
                SourcePhysicalGrowthStats.classic381());
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

    private static void prepareWorld(ClientSession session, int x, int y)
            throws Exception {
        setPhase(session, "WORLD");
        setField(session, "classicWorldMapId", 4);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", 100);
        setField(session, "playerMaxHp", 100);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
    }

    private static void invokeOpenHypertext(ClientSession session, long objectId)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "openHypertext", byte[].class);
        method.setAccessible(true);
        method.invoke(session, (Object) new Protocol.Writer()
                .c(Protocol.C_DIALOG).d(objectId).bytes());
    }

    private static void invokePerceptionRebuild(ClientSession session)
            throws Exception {
        for (String methodName : List.of(
                "sendClassicWorldStaticNpcs", "sendClassicWorldMonsters")) {
            Method method = ClientSession.class.getDeclaredMethod(
                    methodName, String.class);
            method.setAccessible(true);
            method.invoke(session, "guardian-test");
        }
    }

    private static boolean invokeHarvest(
            ClientSession session,
            ClassicWorldMonsterPopulation.Snapshot guardian,
            int roll) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "harvestClassicWorldGuardian",
                ClassicWorldMonsterPopulation.Snapshot.class, int.class);
        method.setAccessible(true);
        return (boolean) method.invoke(session, guardian, roll);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void invokeDirectAttack(
            ClientSession session,
            ClassicWorldMonsterPopulation.Snapshot target) throws Exception {
        Class envelopeType = Class.forName(
                ClientSession.class.getName() + "$AttackEnvelope");
        Object direct = Enum.valueOf(envelopeType, "DIRECT");
        Method method = ClientSession.class.getDeclaredMethod(
                "attackClassicWorld", Protocol.AttackRequest.class, envelopeType);
        method.setAccessible(true);
        method.invoke(session, new Protocol.AttackRequest(
                target.objectId(), target.x(), target.y()), direct);
    }

    private static void invokeClassicWorldDirectDamage(
            ClientSession session, double damage, long attackerObjectId)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "takeClassicWorldDirectDamage", double.class, long.class);
        method.setAccessible(true);
        method.invoke(session, damage, attackerObjectId);
    }

    private static void invokeMonsterTick(GameHub hub, long nowNanos)
            throws Exception {
        Field populationField = GameHub.class.getDeclaredField(
                "classicWorldMonsters");
        populationField.setAccessible(true);
        ClassicWorldMonsterPopulation population =
                (ClassicWorldMonsterPopulation) populationField.get(hub);
        Method method = GameHub.class.getDeclaredMethod(
                "classicWorldMonsterTick", int.class, int.class,
                ClassicWorldMonsterPopulation.class, long.class);
        method.setAccessible(true);
        method.invoke(hub, 4, -1, population, nowNanos);
    }

    private static long invokeGuardianTarget(
            GameHub hub, ClassicWorldMonsterPopulation.Snapshot guardian)
            throws Exception {
        Field populationField = GameHub.class.getDeclaredField(
                "classicWorldMonsters");
        populationField.setAccessible(true);
        ClassicWorldMonsterPopulation population =
                (ClassicWorldMonsterPopulation) populationField.get(hub);
        return population.targetObjectId(guardian.mapId(), guardian.objectId());
    }

    private static String invokeStateCharacter(ClientSession session)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod("stateCharacter");
        method.setAccessible(true);
        return (String) method.invoke(session);
    }

    private static int heading(int fromX, int fromY, int toX, int toY) {
        int dx = Integer.compare(toX, fromX);
        int dy = Integer.compare(toY, fromY);
        if (dx == 0 && dy < 0) return 0;
        if (dx > 0 && dy < 0) return 1;
        if (dx > 0 && dy == 0) return 2;
        if (dx > 0) return 3;
        if (dx == 0 && dy > 0) return 4;
        if (dx < 0 && dy > 0) return 5;
        if (dx < 0 && dy == 0) return 6;
        return 7;
    }

    private static long putObjectId(byte[] packet) {
        if (packet.length < 13
                || (packet[0] & 0xFF) != Protocol.S_PUT_OBJECT) return 0;
        Protocol.Reader reader = new Protocol.Reader(packet);
        reader.c();
        reader.h();
        reader.h();
        return reader.d();
    }

    private static boolean hasAttack(
            List<byte[]> packets, long attacker, long target) {
        for (byte[] packet : packets) {
            if (packet.length < 10
                    || (packet[0] & 0xFF) != Protocol.S_ATTACK) continue;
            Protocol.Reader reader = new Protocol.Reader(packet);
            reader.c();
            reader.c();
            if (reader.d() == attacker && reader.d() == target) return true;
        }
        return false;
    }

    private static boolean hasHitRatioFor(List<byte[]> packets, long objectId) {
        for (byte[] packet : packets) {
            if (packet.length < 5
                    || (packet[0] & 0xFF) != Protocol.S_HIT_RATIO) continue;
            Protocol.Reader reader = new Protocol.Reader(packet);
            reader.c();
            if (reader.d() == objectId) return true;
        }
        return false;
    }

    private static boolean hasAction(
            List<byte[]> packets, long objectId, int action) {
        for (byte[] packet : packets) {
            if (packet.length < 6
                    || (packet[0] & 0xFF) != Protocol.S_ACTION) continue;
            Protocol.Reader reader = new Protocol.Reader(packet);
            reader.c();
            if (reader.d() == objectId && reader.c() == action) return true;
        }
        return false;
    }

    private static boolean contains(List<byte[]> packets, byte[] expected) {
        return packets.stream().anyMatch(packet -> Arrays.equals(packet, expected));
    }

    private static List<byte[]> drain(
            Socket client, Protocol.CipherState receiver) throws Exception {
        List<byte[]> packets = new ArrayList<>();
        while (true) {
            try {
                packets.add(receiver.decrypt(Protocol.readFrame(
                        client.getInputStream(), 65_535)));
            } catch (SocketTimeoutException done) {
                return packets;
            }
        }
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

    private static void setBooleanField(Object target, String name, boolean value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.setBoolean(target, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setPhase(ClientSession session, String value)
            throws Exception {
        Class phaseType = Class.forName(ClientSession.class.getName() + "$Phase");
        setField(session, "phase", Enum.valueOf(phaseType, value));
    }
}

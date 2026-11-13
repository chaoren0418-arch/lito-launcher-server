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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionClassicWorldGuardTest {
    private static final int[] DX = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] DY = {-1, -1, 0, 1, 1, 1, 0, -1};
    @TempDir Path temporary;

    @Test
    void encryptedPlayerKillRecordsNativePkLawfulPacketsAndGuardDeathAbsolvesIt()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        Path stateFile = temporary.resolve("guard-pvp-state.properties");
        GameStateStore state = new GameStateStore(stateFile, true);

        try (AccountRepository accounts = AccountRepository.open(config);
             GameHub hub = new GameHub(config, state);
             CaptureLog attackerCapture = new CaptureLog(
                     temporary.resolve("guard-pvp-attacker-capture"));
             CaptureLog victimCapture = new CaptureLog(
                     temporary.resolve("guard-pvp-victim-capture"));
             ServerSocket attackerListener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             ServerSocket victimListener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket attackerClient = new Socket(
                     "127.0.0.1", attackerListener.getLocalPort());
             Socket attackerServer = attackerListener.accept();
             Socket victimClient = new Socket(
                     "127.0.0.1", victimListener.getLocalPort());
             Socket victimServer = victimListener.accept()) {
            attackerClient.setSoTimeout(75);
            victimClient.setSoTimeout(75);
            Protocol.CipherState attackerReceiver =
                    new Protocol.CipherState(0x3810_5712);
            Protocol.CipherState victimReceiver =
                    new Protocol.CipherState(0x3810_5713);
            ClientSession attacker = new ClientSession(
                    attackerServer, ClientSession.Endpoint.GAME, config,
                    accounts, state, attackerCapture, hub);
            ClientSession victim = new ClientSession(
                    victimServer, ClientSession.Endpoint.GAME, config,
                    accounts, state, victimCapture, hub);
            CharacterSummary attackerCharacter =
                    accounts.characters("test").get(0);
            CharacterSummary victimCharacter =
                    accounts.characters("test2").get(0);
            String source = config.classicServerNames().get(0);
            initialize(attacker, source, attackerCharacter, "test");
            initialize(victim, source, victimCharacter, "test2");
            setField(attacker, "outgoing",
                    new Protocol.CipherState(0x3810_5712));
            setField(victim, "outgoing",
                    new Protocol.CipherState(0x3810_5713));

            int mapId = ClassicWorldMapCatalog.WORLD_TREE_MAP_ID;
            int[] points = normalAdjacentPoints(mapId);
            prepareWorld(attacker, mapId, points[0], points[1]);
            prepareWorld(victim, mapId, points[2], points[3]);
            setField(attacker, "playerLawful", 1_234);
            setField(victim, "playerLawful", 0);
            setField(victim, "playerHp", 1);
            hub.connect(attacker);
            hub.connect(victim);
            hub.enterClassicWorld(
                    attacker, mapId, points[0], points[1], -1);
            hub.enterClassicWorld(
                    victim, mapId, points[2], points[3], -1);
            drain(attackerClient, attackerReceiver);
            drain(victimClient, victimReceiver);

            invokeClassicWorldPlayerDamage(victim, 1, attacker);
            List<byte[]> attackerPackets =
                    drain(attackerClient, attackerReceiver);
            List<byte[]> victimPackets = drain(victimClient, victimReceiver);
            int level = (int) field(attacker, "playerClassicLevel");
            int calculated = level < 50
                    ? -(level * level * 4)
                    : -(int) (Math.pow(level, 3) * 0.08D);
            int expectedLawful = Math.max(Short.MIN_VALUE,
                    Math.min(calculated, 234));
            long lastPk = (long) field(attacker, "lastPkEpochSeconds");

            assertEquals(0, (int) field(victim, "playerHp"));
            assertEquals(1, (int) field(attacker, "generalPkCount"));
            assertTrue(lastPk > 0);
            assertEquals(expectedLawful,
                    (int) field(attacker, "playerLawful"));
            assertTrue(contains(attackerPackets, Protocol.changeAlignment(
                    attacker.playerObjectId(), expectedLawful)));
            assertTrue(contains(victimPackets, Protocol.changeAlignment(
                    attacker.playerObjectId(), expectedLawful)));
            assertTrue(contains(attackerPackets, Protocol.criminalStatus(
                    attacker.playerObjectId(),
                    ClassicWorldPinkNameStatus.WIRE_SECONDS)));
            assertTrue(hasAction(attackerPackets,
                    victim.playerObjectId(), BattleActionCodes.DIE));
            String recorded = Files.readString(stateFile);
            assertTrue(recorded.contains("generalPk.effectiveCount=1"));
            assertTrue(recorded.contains(
                    "generalPk.effectiveLastEpochSeconds=" + lastPk));

            ClassicWorldMonsterPopulation.Snapshot guard =
                    ClassicWorldGuardCatalog.all().stream()
                            .filter(row -> row.mapId() == mapId)
                            .map(row -> hub.classicWorldMonster(
                                    mapId, -1, row.objectId()))
                            .filter(java.util.Objects::nonNull)
                            .findFirst().orElseThrow();
            setField(attacker, "positionX", guard.x());
            setField(attacker, "positionY", guard.y());
            setField(attacker, "playerHp", 1);
            invokeClassicWorldDirectDamage(attacker, 1.0, guard.objectId());
            List<byte[]> absolution =
                    drain(attackerClient, attackerReceiver);
            drain(victimClient, victimReceiver);

            assertEquals(0, (int) field(attacker, "playerHp"));
            assertEquals(0, (int) field(attacker, "generalPkCount"));
            assertEquals(0L, (long) field(attacker, "lastPkEpochSeconds"));
            assertTrue(hasAction(absolution,
                    attacker.playerObjectId(), BattleActionCodes.DIE));
            String cleared = Files.readString(stateFile);
            assertTrue(cleared.contains("generalPk.effectiveCount=0"));
            assertTrue(cleared.contains(
                    "generalPk.effectiveLastEpochSeconds=0"));
        }
    }

    @Test
    void encryptedSessionSpawnsTalksTargetsWantedUsesBowEncodingAndAbsolvesPk()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("guard-state.properties"), false);
        ClassicWorldNpcCatalog.StaticNpc sourceGuard =
                ClassicWorldGuardCatalog.all().stream()
                        .filter(row -> row.npcId() == 60501)
                        .findFirst().orElseThrow();

        try (AccountRepository accounts = AccountRepository.open(config);
             GameHub hub = new GameHub(config, state);
             CaptureLog capture = new CaptureLog(
                     temporary.resolve("guard-capture"));
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(75);
            Protocol.CipherState receiver =
                    new Protocol.CipherState(0x3810_5710);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, accounts,
                    state, capture, hub);
            CharacterSummary knight = accounts.characters("test").get(0);
            initialize(session, config.classicServerNames().get(0),
                    knight, "test");
            setField(session, "outgoing",
                    new Protocol.CipherState(0x3810_5710));

            ClassicWorldMonsterPopulation.Snapshot guard =
                    hub.classicWorldMonster(
                            sourceGuard.mapId(), -1, sourceGuard.objectId());
            assertNotNull(guard);
            int[] point = rangedAttackPoint(hub, guard);
            prepareWorld(session, guard.mapId(), point[0], point[1]);
            setField(session, "playerHp", 10_000);
            setField(session, "playerMaxHp", 10_000);
            hub.connect(session);
            hub.enterClassicWorld(
                    session, guard.mapId(), point[0], point[1], -1);
            invokePerceptionRebuild(session);
            List<byte[]> bootstrap = drain(client, receiver);
            assertEquals(1, bootstrap.stream()
                    .filter(packet -> putObjectId(packet) == guard.objectId())
                    .count(),
                    "live L1Guard must replace its static row exactly once");

            invokeOpenHypertext(session, guard.objectId());
            assertTrue(contains(drain(client, receiver), Protocol.hypertext(
                    guard.objectId(), sourceGuard.normalAction(), "", List.of())));

            setBooleanField(hub,
                    "classicWorldMonsterAggressiveAiEnabled", true);
            invokeMonsterTick(hub, guard.mapId(), 1L);
            assertEquals(0, invokeGuardTarget(hub, guard));
            assertFalse(hasAttackFrom(
                    drain(client, receiver), guard.objectId()),
                    "non-wanted player must not be acquired");

            long now = state.currentEpochSeconds();
            String stateKey = invokeStateCharacter(session);
            state.reconcileGeneralPk(stateKey, 2, now - 60);
            setField(session, "generalPkCount", 2);
            setField(session, "lastPkEpochSeconds", now - 60);
            invokeMonsterTick(hub, guard.mapId(), 2L);
            List<byte[]> attack = drain(client, receiver);
            assertEquals(session.playerObjectId(), invokeGuardTarget(hub, guard));
            assertTrue(hasExactBowAttack(
                    attack, guard.objectId(), session.playerObjectId(),
                    guard.template().projectileAttackAction(),
                    guard.template().bowActionId(),
                    guard.x(), guard.y(), point[0], point[1]),
                    "selected LiTo S_ATTACK must carry guard action and projectile 66");

            setField(session, "playerHp", 1);
            invokeClassicWorldDirectDamage(session, 1.0, guard.objectId());
            List<byte[]> death = drain(client, receiver);
            assertEquals(0, (int) field(session, "playerHp"));
            assertEquals(1, (int) field(session, "generalPkCount"));
            assertEquals(0L, (long) field(session, "lastPkEpochSeconds"));
            assertTrue(hasAction(death, session.playerObjectId(),
                    BattleActionCodes.DIE));
            assertEquals(new GameStateStore.GeneralPkState(1, 0),
                    state.reconcileGeneralPk(
                            stateKey, 2, now - 60).effective());
        }
    }

    @Test
    void guardDeathNeverEntersExperienceLawfulDropOrQuestRewardPath()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("guard-reward-state.properties"), false);
        ClassicWorldNpcCatalog.StaticNpc sourceGuard =
                ClassicWorldGuardCatalog.all().stream()
                        .filter(row -> row.npcId() == 60530)
                        .findFirst().orElseThrow();

        try (AccountRepository accounts = AccountRepository.open(config);
             GameHub hub = new GameHub(config, state);
             CaptureLog capture = new CaptureLog(
                     temporary.resolve("guard-reward-capture"));
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(75);
            Protocol.CipherState receiver =
                    new Protocol.CipherState(0x3810_5711);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, accounts,
                    state, capture, hub);
            CharacterSummary knight = accounts.characters("test").get(0);
            initialize(session, config.classicServerNames().get(0),
                    knight, "test");
            setField(session, "outgoing",
                    new Protocol.CipherState(0x3810_5711));
            ClassicWorldMonsterPopulation.Snapshot guard =
                    hub.classicWorldMonster(
                            sourceGuard.mapId(), -1, sourceGuard.objectId());
            int[] point = adjacentPoint(hub, guard);
            prepareWorld(session, guard.mapId(), point[0], point[1]);
            hub.connect(session);
            hub.enterClassicWorld(
                    session, guard.mapId(), point[0], point[1], -1);
            drain(client, receiver);

            long experience = (long) field(session, "playerClassicExperience");
            int lawful = (int) field(session, "playerLawful");
            RuntimeInventory inventory =
                    ((RuntimeInventory) field(session, "inventory")).copy();
            ClassicWorldMonsterPopulation.Hit hit =
                    hub.damageClassicWorldMonster(
                            guard.mapId(), -1, guard.objectId(),
                            session.playerObjectId(), guard.currentHp());
            assertNotNull(hit);
            assertTrue(hit.diedNow());
            invokeRewards(session, hit.after(), hit.contributions());
            assertEquals(experience,
                    (long) field(session, "playerClassicExperience"));
            assertEquals(lawful, (int) field(session, "playerLawful"));
            assertEquals(inventory.catalogCounts(),
                    ((RuntimeInventory) field(session, "inventory"))
                            .catalogCounts());
            assertTrue(drain(client, receiver).isEmpty());
        }
    }

    private static int[] rangedAttackPoint(
            GameHub hub, ClassicWorldMonsterPopulation.Snapshot guard) {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(guard.mapId());
        List<ClassicWorldMonsterPopulation.Snapshot> nearby =
                hub.classicWorldMonstersInScreen(
                        guard.mapId(), -1, guard.x(), guard.y());
        for (int distance = 2; distance <= guard.template().attackRange();
             distance++) {
            for (int dx = -distance; dx <= distance; dx++) {
                for (int dy = -distance; dy <= distance; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != distance) continue;
                    int x = guard.x() + dx;
                    int y = guard.y() + dy;
                    boolean occupied = nearby.stream().anyMatch(actor ->
                            actor.present() && actor.x() == x && actor.y() == y);
                    if (!occupied
                            && !ClassicWorldNpcCatalog.occupies(
                                    guard.mapId(), x, y)
                            && map.attackPosition(
                                    guard.x(), guard.y(), x, y,
                                    guard.template().attackRange())) {
                        return new int[] {x, y};
                    }
                }
            }
        }
        throw new AssertionError("no ranged L1Guard attack tile");
    }

    private static int[] adjacentPoint(
            GameHub hub, ClassicWorldMonsterPopulation.Snapshot guard) {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(guard.mapId());
        List<ClassicWorldMonsterPopulation.Snapshot> nearby =
                hub.classicWorldMonstersInScreen(
                        guard.mapId(), -1, guard.x(), guard.y());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int x = guard.x() + dx;
                int y = guard.y() + dy;
                boolean occupied = nearby.stream().anyMatch(actor ->
                        actor.present() && actor.x() == x && actor.y() == y);
                if (!occupied && !ClassicWorldNpcCatalog.occupies(
                                guard.mapId(), x, y)
                        && map.attackPosition(
                                x, y, guard.x(), guard.y(), 1)) {
                    return new int[] {x, y};
                }
            }
        }
        throw new AssertionError("no adjacent L1Guard tile");
    }

    private static int[] normalAdjacentPoints(int mapId) {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(mapId);
        for (int y = map.startY(); y <= map.endY(); y++) {
            for (int x = map.startX(); x <= map.endX(); x++) {
                if (!map.isNormalZone(x, y) || !map.standingPassable(x, y)
                        || ClassicWorldNpcCatalog.occupies(mapId, x, y)) {
                    continue;
                }
                for (int heading = 0; heading < 8; heading++) {
                    int targetX = x + DX[heading];
                    int targetY = y + DY[heading];
                    if (map.isNormalZone(targetX, targetY)
                            && map.standingPassable(targetX, targetY)
                            && map.canStep(x, y, targetX, targetY)
                            && !ClassicWorldNpcCatalog.occupies(
                                    mapId, targetX, targetY)) {
                        return new int[] {x, y, targetX, targetY};
                    }
                }
            }
        }
        throw new AssertionError("no adjacent normal-zone player tiles");
    }

    private static void initialize(
            ClientSession session, String source, CharacterSummary character,
            String account) throws Exception {
        setField(session, "account", account);
        setField(session, "selectedCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "selectedClassicServerName", source);
        setField(session, "playerClassicLevel", character.level());
        setField(session, "playerClassicExperience", character.experience());
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(), () -> 0x7570_0011L));
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

    private static void prepareWorld(
            ClientSession session, int mapId, int x, int y) throws Exception {
        setPhase(session, "WORLD");
        setField(session, "classicWorldMapId", mapId);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", 100);
        setField(session, "playerMaxHp", 100);
        setField(session, "playerMp", 100);
        setField(session, "playerMaxMp", 100);
    }

    private static void invokePerceptionRebuild(ClientSession session)
            throws Exception {
        for (String name : List.of(
                "sendClassicWorldStaticNpcs", "sendClassicWorldMonsters")) {
            Method method = ClientSession.class.getDeclaredMethod(
                    name, String.class);
            method.setAccessible(true);
            method.invoke(session, "guard-test");
        }
    }

    private static void invokeOpenHypertext(
            ClientSession session, long objectId) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "openHypertext", byte[].class);
        method.setAccessible(true);
        method.invoke(session, (Object) new Protocol.Writer()
                .c(Protocol.C_DIALOG).d(objectId).bytes());
    }

    private static void invokeClassicWorldDirectDamage(
            ClientSession session, double damage, long attackerObjectId)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "takeClassicWorldDirectDamage", double.class, long.class);
        method.setAccessible(true);
        method.invoke(session, damage, attackerObjectId);
    }

    private static void invokeClassicWorldPlayerDamage(
            ClientSession victim, int damage, ClientSession attacker)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "takeClassicWorldPlayerDamage", int.class,
                ClientSession.class, boolean.class, boolean.class);
        method.setAccessible(true);
        method.invoke(victim, damage, attacker, true, false);
    }

    @SuppressWarnings("unchecked")
    private static void invokeRewards(
            ClientSession session,
            ClassicWorldMonsterPopulation.Snapshot defeated,
            List<ClassicWorldMonsterPopulation.Contribution> contributions)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "distributeClassicWorldMonsterRewards",
                ClassicWorldMonsterPopulation.Snapshot.class, List.class);
        method.setAccessible(true);
        method.invoke(session, defeated, contributions);
    }

    private static void invokeMonsterTick(
            GameHub hub, int mapId, long nowNanos) throws Exception {
        Field populationField = GameHub.class.getDeclaredField(
                "classicWorldMonsters");
        populationField.setAccessible(true);
        ClassicWorldMonsterPopulation population =
                (ClassicWorldMonsterPopulation) populationField.get(hub);
        Method method = GameHub.class.getDeclaredMethod(
                "classicWorldMonsterTick", int.class, int.class,
                ClassicWorldMonsterPopulation.class, long.class);
        method.setAccessible(true);
        method.invoke(hub, mapId, -1, population, nowNanos);
    }

    private static long invokeGuardTarget(
            GameHub hub, ClassicWorldMonsterPopulation.Snapshot guard)
            throws Exception {
        Field populationField = GameHub.class.getDeclaredField(
                "classicWorldMonsters");
        populationField.setAccessible(true);
        ClassicWorldMonsterPopulation population =
                (ClassicWorldMonsterPopulation) populationField.get(hub);
        return population.targetObjectId(guard.mapId(), guard.objectId());
    }

    private static String invokeStateCharacter(ClientSession session)
            throws Exception {
        Method method =
                ClientSession.class.getDeclaredMethod("stateCharacter");
        method.setAccessible(true);
        return (String) method.invoke(session);
    }

    private static boolean hasAttackFrom(
            List<byte[]> packets, long attacker) {
        for (byte[] packet : packets) {
            if (packet.length < 10
                    || (packet[0] & 0xFF) != Protocol.S_ATTACK) continue;
            Protocol.Reader reader = new Protocol.Reader(packet);
            reader.c();
            reader.c();
            if (reader.d() == attacker) return true;
        }
        return false;
    }

    private static boolean hasExactBowAttack(
            List<byte[]> packets, long attacker, long target,
            int action, int projectile, int sourceX, int sourceY,
            int targetX, int targetY) {
        for (byte[] packet : packets) {
            if (packet.length < 29
                    || (packet[0] & 0xFF) != Protocol.S_ATTACK) continue;
            Protocol.Reader reader = new Protocol.Reader(packet);
            reader.c();
            if (reader.c() != action || reader.d() != attacker
                    || reader.d() != target) continue;
            reader.c();
            reader.c();
            assertTrue(reader.d() > 0);
            if (reader.h() != projectile || reader.c() != 0x7F) continue;
            return reader.h() == sourceX && reader.h() == sourceY
                    && reader.h() == targetX && reader.h() == targetY;
        }
        return false;
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
        return packets.stream()
                .anyMatch(packet -> Arrays.equals(packet, expected));
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

    private static void setField(
            Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void setBooleanField(
            Object target, String name, boolean value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.setBoolean(target, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setPhase(
            ClientSession session, String value) throws Exception {
        Class phaseType =
                Class.forName(ClientSession.class.getName() + "$Phase");
        setField(session, "phase", Enum.valueOf(phaseType, value));
    }
}

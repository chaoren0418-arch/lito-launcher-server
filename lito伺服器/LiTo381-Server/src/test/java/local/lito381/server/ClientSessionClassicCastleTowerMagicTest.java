package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ClientSessionClassicCastleTowerMagicTest {
    @TempDir Path temporary;

    @Test
    void retainedTowerRowsAreDedicatedZeroMrL1TowerReceivers()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        try (AccountRepository accounts = AccountRepository.open(config)) {
            for (int npcId : List.of(81111, 81189, 81190, 81191, 81192, 81193)) {
                SourceNpcPhysicalStats row = accounts.npcPhysicalStats(npcId);
                assertNotNull(row, "missing retained tower NPC " + npcId);
                assertEquals(0, row.magicResistance());
                assertEquals(0, row.maxMp());
            }
        }
    }

    @Test
    void directAndBothAreaMagicUseNativeTowerLifecycleAndAdenGate()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("castle-tower-magic-state.properties"), false);
        String source = config.classicServerNames().get(0);
        try (AccountRepository accounts = AccountRepository.open(config);
             GameHub hub = new GameHub(config, state);
             CaptureLog capture = new CaptureLog(
                     temporary.resolve("castle-tower-magic-capture"));
             ServerSocket listener = new ServerSocket(
                     0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(75);
            Protocol.CipherState receiver = new Protocol.CipherState(0x3810_5660);
            ClientSession caster = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, accounts,
                    state, capture, hub);
            CharacterSummary character = accounts.characters("test26").stream()
                    .filter(candidate -> candidate.name()
                            .equalsIgnoreCase("TestSiegeLord"))
                    .findFirst().orElseThrow();
            assertEquals("LegacySiege", character.clanName());
            initialize(caster, source, character);
            hub.registerOriginalClan(
                    caster.sourceCharacterIdentity(), character.clanName());
            setField(caster, "outgoing",
                    new Protocol.CipherState(0x3810_5660));

            long start = System.currentTimeMillis() - 60_000L;
            hub.registerClassicCastleWarSchedules(source, List.of(
                    new CastleWarSchedule(6, "矮人城", start, 10, 120),
                    new CastleWarSchedule(7, "亞丁城", start, 10, 120)));
            CastleSummary dwarfOwner = accounts.castleById(6);
            CastleSummary adenOwner = accounts.castleById(7);
            declareIfOwned(hub, source, dwarfOwner, 6);
            declareIfOwned(hub, source, adenOwner, 7);

            ClassicWorldCastleWarField.WarObject dwarf = tower(
                    hub, source, 6, 81111);
            int dwarfX = dwarf.x();
            int dwarfY = dwarf.y() - 1;
            hub.connect(caster);
            hub.enterClassicWorld(caster, dwarf.mapId(), dwarfX, dwarfY, -1);
            prepareWorld(caster, dwarf.mapId(), dwarfX, dwarfY);
            drain(client, receiver);

            int beforeDrainTower = current(
                    hub, source, dwarf.objectId()).currentHp();
            setField(caster, "playerHp", 400);
            invokeChillTouch(caster, dwarf);
            int healedHp = (int) field(caster, "playerHp");
            assertTrue(healedHp > 400 && healedHp <= 500);
            assertTrue(current(hub, source, dwarf.objectId()).currentHp()
                    < beforeDrainTower);
            List<byte[]> lifeDrainPackets = drain(client, receiver);
            assertTrue(containsOpcode(lifeDrainPackets, Protocol.S_ATTACK));
            assertTrue(containsOpcode(lifeDrainPackets, Protocol.S_HIT_POINT));
            assertTrue(containsOpcode(lifeDrainPackets, Protocol.S_HIT_RATIO));
            setField(caster, "playerHp", 500);
            clearCooldowns(caster);

            int beforeDirect = current(hub, source, dwarf.objectId()).currentHp();
            invokeDirect(caster, dwarf);
            int afterDirect = current(hub, source, dwarf.objectId()).currentHp();
            assertTrue(afterDirect < beforeDirect);
            List<byte[]> directPackets = drain(client, receiver);
            assertTrue(containsOpcode(directPackets, Protocol.S_ATTACK));
            assertTrue(containsOpcode(directPackets, Protocol.S_HIT_RATIO));

            clearCooldowns(caster);
            invokeTargetCentered(caster, dwarf);
            int afterTargetArea = current(
                    hub, source, dwarf.objectId()).currentHp();
            assertTrue(afterTargetArea < afterDirect);
            List<byte[]> targetPackets = drain(client, receiver);
            assertTrue(attackManyContains(
                    targetPackets, dwarf.objectId(), 8));

            clearCooldowns(caster);
            invokeCasterCentered(caster);
            int afterCasterArea = current(
                    hub, source, dwarf.objectId()).currentHp();
            assertTrue(afterCasterArea < afterTargetArea);
            List<byte[]> centeredPackets = drain(client, receiver);
            assertTrue(attackManyContains(
                    centeredPackets, dwarf.objectId(), 0));

            ClassicWorldCastleWarField.TowerHit primed =
                    hub.damageClassicCastleTower(
                            source, dwarf.mapId(), dwarf.objectId(),
                            "LegacySiege", dwarfOwner.clanName(),
                            afterCasterArea - 1);
            assertNotNull(primed);
            assertEquals(1, primed.after().currentHp());
            clearCooldowns(caster);
            invokeDirect(caster, dwarf);
            ClassicWorldCastleWarField.WarObject dead = current(
                    hub, source, dwarf.objectId());
            assertEquals(0, dead.currentHp());
            List<byte[]> deathPackets = drain(client, receiver);
            assertTrue(contains(deathPackets, Protocol.objectAction(
                    dwarf.objectId(), ClassicWorldCastleWarField.TOWER_DIE)));
            assertTrue(hub.classicCastleWarObjects(source).stream().anyMatch(
                    object -> object.kind()
                            == ClassicWorldCastleWarField.Kind.CROWN
                            && object.castleId() == 6));

            ClassicWorldCastleWarField.WarObject adenMain = tower(
                    hub, source, 7, 81189);
            int adenX = adenMain.x();
            int adenY = adenMain.y() - 1;
            hub.enterClassicWorld(caster, adenMain.mapId(), adenX, adenY, -1);
            prepareWorld(caster, adenMain.mapId(), adenX, adenY);
            clearCooldowns(caster);
            drain(client, receiver);

            setField(caster, "playerHp", 400);
            invokeChillTouch(caster, adenMain);
            assertEquals(adenMain.maximumHp(),
                    current(hub, source, adenMain.objectId()).currentHp(),
                    "Aden main tower must reject magic while a sub tower lives");
            assertEquals(400, field(caster, "playerHp"),
                    "a rejected target commit must not grant life drain");
            List<byte[]> blockedPackets = drain(client, receiver);
            assertTrue(containsOpcode(blockedPackets, Protocol.S_ATTACK));
            assertTrue(!containsOpcode(blockedPackets, Protocol.S_HIT_POINT));
            setField(caster, "playerHp", 500);

            for (int npcId : List.of(81190, 81191, 81192, 81193)) {
                ClassicWorldCastleWarField.WarObject sub = tower(
                        hub, source, 7, npcId);
                ClassicWorldCastleWarField.TowerHit subDeath =
                        hub.damageClassicCastleTower(
                                source, sub.mapId(), sub.objectId(),
                                "LegacySiege", adenOwner.clanName(),
                                sub.maximumHp());
                assertNotNull(subDeath);
                assertTrue(subDeath.diedNow());
                assertEquals(null, subDeath.spawnedCrown());
            }
            clearCooldowns(caster);
            invokeDirect(caster, adenMain);
            assertTrue(current(hub, source, adenMain.objectId()).currentHp()
                    < adenMain.maximumHp());
            assertTrue(containsOpcode(
                    drain(client, receiver), Protocol.S_HIT_RATIO));
        }
    }

    private static void declareIfOwned(
            GameHub hub, String source, CastleSummary owner, int castleId) {
        if (owner == null || owner.clanName().equalsIgnoreCase("NPC")
                || owner.clanName().equalsIgnoreCase("LegacySiege")) return;
        GameHub.ClassicCastleSiegeResult result =
                hub.mutateClassicCastleSiege(
                        source, castleId, "LegacySiege", owner.clanName(),
                        Protocol.ClanWarRequestType.DECLARE);
        assertEquals(GameHub.ClassicCastleSiegeMutation.DECLARED,
                result.mutation());
    }

    private static ClassicWorldCastleWarField.WarObject tower(
            GameHub hub, String source, int castleId, int npcId) {
        return hub.classicCastleWarObjects(source).stream()
                .filter(object -> object.kind()
                        == ClassicWorldCastleWarField.Kind.TOWER)
                .filter(object -> object.castleId() == castleId
                        && object.npcId() == npcId)
                .findFirst().orElseThrow();
    }

    private static ClassicWorldCastleWarField.WarObject current(
            GameHub hub, String source, long objectId) {
        return hub.classicCastleWarObjects(source).stream()
                .filter(object -> object.objectId() == objectId)
                .findFirst().orElseThrow();
    }

    private static void initialize(
            ClientSession session, String source, CharacterSummary character)
            throws Exception {
        setField(session, "account", "test26");
        setField(session, "selectedCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "selectedClassicServerName", source);
        setField(session, "inventory", RuntimeInventory.fromCatalogCounts(
                Map.of(), () -> 0x7566_0001L));
        setField(session, "learnedSkills", Set.of(
                BattleSpellCatalog.CHILL_TOUCH,
                BattleSpellCatalog.LIGHT_ARROW,
                BattleSpellCatalog.LIGHTNING_STORM,
                BattleSpellCatalog.FIRE_STORM));
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

    private static void prepareWorld(
            ClientSession session, int mapId, int x, int y) throws Exception {
        setPhase(session, "WORLD");
        setField(session, "classicWorldMapId", mapId);
        setField(session, "classicWorldInstanceId", -1);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", 500);
        setField(session, "playerMaxHp", 500);
        setField(session, "playerMp", 500);
        setField(session, "playerMaxMp", 500);
    }

    private static void invokeDirect(
            ClientSession caster, ClassicWorldCastleWarField.WarObject tower)
            throws Exception {
        BattleSpellCatalog.AttackSpell spell = BattleSpellCatalog.attackSpell(
                BattleSpellCatalog.LIGHT_ARROW);
        invoke(caster, "useClassicWorldAttackSpell",
                new Class<?>[] {Protocol.AttackSpellRequest.class,
                        BattleSpellCatalog.AttackSpell.class},
                new Protocol.AttackSpellRequest(
                        spell.row(), spell.column(), tower.objectId(),
                        tower.x(), tower.y()), spell);
    }

    private static void invokeChillTouch(
            ClientSession caster, ClassicWorldCastleWarField.WarObject tower)
            throws Exception {
        BattleSpellCatalog.AttackSpell spell = BattleSpellCatalog.attackSpell(
                BattleSpellCatalog.CHILL_TOUCH);
        invoke(caster, "useClassicWorldAttackSpell",
                new Class<?>[] {Protocol.AttackSpellRequest.class,
                        BattleSpellCatalog.AttackSpell.class},
                new Protocol.AttackSpellRequest(
                        spell.row(), spell.column(), tower.objectId(),
                        tower.x(), tower.y()), spell);
    }

    private static void invokeTargetCentered(
            ClientSession caster, ClassicWorldCastleWarField.WarObject tower)
            throws Exception {
        BattleSpellCatalog.AreaAttackSpell spell =
                BattleSpellCatalog.areaAttackSpell(
                        BattleSpellCatalog.LIGHTNING_STORM);
        invoke(caster, "useClassicWorldAreaAttackSpell",
                new Class<?>[] {Protocol.AttackSpellRequest.class,
                        BattleSpellCatalog.AreaAttackSpell.class},
                new Protocol.AttackSpellRequest(
                        spell.row(), spell.column(), tower.objectId(),
                        tower.x(), tower.y()), spell);
    }

    private static void invokeCasterCentered(ClientSession caster)
            throws Exception {
        BattleSpellCatalog.AreaAttackSpell spell =
                BattleSpellCatalog.areaAttackSpell(
                        BattleSpellCatalog.FIRE_STORM);
        invoke(caster, "useClassicWorldCenteredAreaAttackSpell",
                new Class<?>[] {byte[].class,
                        BattleSpellCatalog.AreaAttackSpell.class},
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(spell.row()).c(spell.column()).bytes(), spell);
    }

    private static boolean attackManyContains(
            List<byte[]> packets, long objectId, int expectedType) {
        for (byte[] packet : packets) {
            if (packet.length < 22
                    || packet[0] != (byte) Protocol.S_ATTACK_MANY) continue;
            Protocol.Reader reader = new Protocol.Reader(packet);
            reader.c();
            reader.c();
            reader.d();
            reader.h();
            reader.h();
            reader.c();
            reader.d();
            reader.h();
            int type = reader.c();
            if (type == 8) {
                reader.c();
                reader.s();
            } else {
                reader.h();
            }
            int count = reader.h();
            for (int index = 0; index < count; index++) {
                long target = reader.d();
                reader.c();
                if (target == objectId && type == expectedType) return true;
            }
        }
        return false;
    }

    private static boolean containsOpcode(List<byte[]> packets, int opcode) {
        return packets.stream().anyMatch(packet -> packet.length > 0
                && packet[0] == (byte) opcode);
    }

    private static boolean contains(List<byte[]> packets, byte[] expected) {
        return packets.stream().anyMatch(packet ->
                java.util.Arrays.equals(packet, expected));
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

    private static void clearCooldowns(ClientSession caster) throws Exception {
        ((Map<?, ?>) field(caster, "spellReadyNanos")).clear();
    }

    private static void invoke(ClientSession session, String name,
                               Class<?>[] parameters, Object... arguments)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        method.invoke(session, arguments);
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

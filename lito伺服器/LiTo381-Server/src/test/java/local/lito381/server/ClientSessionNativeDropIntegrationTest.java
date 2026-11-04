package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

/** Encrypted-session proof for retained C_DROP gates and battle visibility. */
final class ClientSessionNativeDropIntegrationTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @TempDir Path temporary;

    @Test
    void sourceMagicDollSummonsRecallsAndExpiresThroughNativePackets()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.character-selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("magic-doll-state.properties"), false);
        GameHub hub = new GameHub(config, state);
        int x = 33051;
        int y = 32338;
        try (ServerSocket listener = listener();
             Socket client = connect(listener);
             Socket server = listener.accept();
             CaptureLog log = new CaptureLog(temporary.resolve("magic-doll"))) {
            ClientSession session = session(server, config, state, log, hub,
                    "ordinary-player", 0x3810_2790L, x, y);
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
                    55_000, 0, 0, List.of(102),
                    List.of(40_308), List.of(500), 1, 6_100,
                    "魔法娃娃：肥肥");
            setField(session, "inventory", inventory);
            setField(session, "sourceMagicDolls", Map.of(dollItem.objectId(), type));
            setField(session, "outgoing", new Protocol.CipherState(0x2790));
            hub.connect(session);
            hub.enterClassicWorld(session, 4, x, y, 2);

            invoke(session, "useNativeMagicDoll",
                    new Class<?>[] {RuntimeInventory.Item.class}, dollItem);

            RuntimeInventory afterSummon = (RuntimeInventory) field(session, "inventory");
            assertEquals(1_000, afterSummon.catalogCounts().get(
                    InventoryCatalog.ADENA));
            NativeMagicDoll summoned = (NativeMagicDoll) field(
                    session, "activeMagicDoll");
            assertTrue(summoned != null);
            invokeDrop(session, x, y, dollItem.objectId());
            String summonedCapture = read(log);
            assertTrue(summonedCapture.contains("S_PUT_OBJECT magic-doll="
                    + summoned.objectId()));
            assertTrue(summonedCapture.contains("S_EFFECT magic-doll-summon object="
                    + summoned.objectId() + " gfx=5935"));
            assertTrue(summonedCapture.contains(
                    "S_MESSAGE_CODE C_DROP-active-magic-doll=1181"));

            invoke(session, "useNativeMagicDoll",
                    new Class<?>[] {RuntimeInventory.Item.class}, dollItem);
            assertEquals(null, field(session, "activeMagicDoll"));
            assertTrue(read(log).contains("S_EFFECT magic-doll-recall object="
                    + summoned.objectId() + " gfx=5936 reason=item-recall"));

            RuntimeInventory.Item sameItem = ((RuntimeInventory) field(
                    session, "inventory")).item(dollItem.objectId());
            invoke(session, "useNativeMagicDoll",
                    new Class<?>[] {RuntimeInventory.Item.class}, sameItem);
            NativeMagicDoll expiring = (NativeMagicDoll) field(
                    session, "activeMagicDoll");
            session.serverMagicDollTick(expiring.expiresAtNanos());
            assertEquals(null, field(session, "activeMagicDoll"));
            assertEquals(500, ((RuntimeInventory) field(session, "inventory"))
                    .catalogCounts().get(InventoryCatalog.ADENA));
            assertTrue(read(log).contains("reason=timer-or-item-expiry"));
        } finally {
            hub.close();
        }
    }

    @Test
    void nativeMagicDollOneSecondTickCommitsHpMpAndItemAwards()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.character-selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("magic-doll-periodic-state.properties"), false);
        GameHub hub = new GameHub(config, state);
        try (ServerSocket listener = listener();
             Socket client = connect(listener);
             Socket server = listener.accept();
             CaptureLog log = new CaptureLog(temporary.resolve("magic-doll-periodic"))) {
            ClientSession session = session(server, config, state, log, hub,
                    "periodic-player", 0x3810_2791L, 33051, 32338);
            InventoryCatalog.Definition dollDefinition =
                    new InventoryCatalog.Definition(
                            55_001, 0, 2589, 3963, 1, true,
                            "魔法娃娃：小思克巴", "retained timed doll", 0, 0,
                            false, true, BattleActionCodes.WALK, 0,
                            InventoryCatalog.Effect.MAGIC_DOLL);
            RuntimeInventory inventory = new RuntimeInventory();
            assertTrue(inventory.add(dollDefinition, 1));
            assertTrue(inventory.add(InventoryCatalog.require(
                    InventoryCatalog.ADENA), 1));
            RuntimeInventory.Item dollItem = inventory.items().stream()
                    .filter(item -> item.definition().catalogId() == 55_001)
                    .findFirst().orElseThrow();
            SourceMagicDollPower hp = new SourceMagicDollPower(
                    133, "HP", "TIMED_HP_REGEN", 20, 0, 2,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            SourceMagicDollPower mp = new SourceMagicDollPower(
                    135, "MP", "TIMED_MP_REGEN", 10, 0, 2,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            SourceMagicDollPower item = new SourceMagicDollPower(
                    132, "adena", "TIMED_ITEM", 0, 0, 2,
                    0, 0, 0, 0, 0, 0, 0,
                    (int) InventoryCatalog.ADENA, 100, 0);
            List<SourceMagicDollPower> definitions = List.of(hp, mp, item);
            SourceMagicDollType type = new SourceMagicDollType(
                    55_001, 0, 0, List.of(133, 135, 132),
                    List.of(), List.of(), 30, 5_919,
                    "魔法娃娃：小思克巴", definitions);
            setField(session, "inventory", inventory);
            setField(session, "playerHp", 450);
            setField(session, "playerMp", 90);
            setField(session, "sourceMagicDolls", Map.of(dollItem.objectId(), type));
            setField(session, "outgoing", new Protocol.CipherState(0x2791));
            hub.connect(session);
            hub.enterClassicWorld(session, 4, 33051, 32338, 2);

            invoke(session, "useNativeMagicDoll",
                    new Class<?>[] {RuntimeInventory.Item.class}, dollItem);
            NativeMagicDoll summoned = (NativeMagicDoll) field(
                    session, "activeMagicDoll");
            session.serverMagicDollTick(summoned.expiresAtNanos() - 20_000_000_000L);
            assertEquals(450, field(session, "playerHp"));
            session.serverMagicDollTick(summoned.expiresAtNanos() - 19_000_000_000L);

            assertEquals(470, field(session, "playerHp"));
            assertEquals(100, field(session, "playerMp"));
            assertEquals(101, ((RuntimeInventory) field(session, "inventory"))
                    .catalogCounts().get(InventoryCatalog.ADENA));
            String capture = read(log);
            assertTrue(capture.contains("S_HIT_POINT magic-doll-periodic=470/500"));
            assertTrue(capture.contains("S_MANA_POINT magic-doll-periodic=100/100"));
            assertTrue(capture.contains("magic-doll-periodic-item=403"));
        } finally {
            hub.close();
        }
    }

    @Test
    void nativeMagicDollActiveDamageAndReductionCommitInClassicWorld()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.character-selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("magic-doll-active-state.properties"), false);
        GameHub hub = new GameHub(config, state);
        int x = 33051;
        int y = 32338;
        try (ServerSocket listener = new ServerSocket(
                    0, 2, InetAddress.getByName("127.0.0.1"));
             Socket attackerClient = connect(listener);
             Socket attackerServer = listener.accept();
             Socket targetClient = connect(listener);
             Socket targetServer = listener.accept();
             CaptureLog attackerLog = new CaptureLog(
                     temporary.resolve("magic-doll-active-attacker"));
             CaptureLog targetLog = new CaptureLog(
                     temporary.resolve("magic-doll-active-target"))) {
            ClientSession attacker = session(attackerServer, config, state,
                    attackerLog, hub, "active-attacker", 0x3810_2792L, x, y);
            ClientSession target = session(targetServer, config, state,
                    targetLog, hub, "active-target", 0x3810_2793L, x, y);
            InventoryCatalog.Definition dollDefinition =
                    new InventoryCatalog.Definition(
                            55_002, 0, 2589, 3963, 1, true,
                            "魔法娃娃：主動測試", "retained active doll", 0, 0,
                            false, true, BattleActionCodes.WALK, 0,
                            InventoryCatalog.Effect.MAGIC_DOLL);
            RuntimeInventory inventory = new RuntimeInventory();
            assertTrue(inventory.add(dollDefinition, 1));
            RuntimeInventory.Item dollItem = inventory.items().get(0);
            SourceMagicDollPower damage = new SourceMagicDollPower(
                    121, "damage", "ACTIVE_DMG", 0, 1_000, 0,
                    0, 1, 2, 5_528, 1, 50, 0, 0, 0, 0);
            SourceMagicDollPower reduction = new SourceMagicDollPower(
                    122, "reduction", "ACTIVE_REDUX", 0, 1_000, 0,
                    0, 0, 0, 0, 0, 10, 0, 0, 0, 0);
            SourceMagicDollType type = new SourceMagicDollType(
                    55_002, 0, 0, List.of(121, 122),
                    List.of(), List.of(), 30, 6_100,
                    "魔法娃娃：主動測試", List.of(damage, reduction));
            setField(attacker, "inventory", inventory);
            setField(attacker, "sourceMagicDolls",
                    Map.of(dollItem.objectId(), type));
            setField(attacker, "outgoing", new Protocol.CipherState(0x2792));
            setField(target, "outgoing", new Protocol.CipherState(0x2793));
            hub.connect(attacker);
            hub.connect(target);
            hub.enterClassicWorld(attacker, 4, x, y, 2);
            hub.enterClassicWorld(target, 4, x, y, 2);

            invoke(attacker, "useNativeMagicDoll",
                    new Class<?>[] {RuntimeInventory.Item.class}, dollItem);
            int reduced = (int) invokeValue(attacker,
                    "nativeMagicDollDamageAfterActiveReduction",
                    new Class<?>[] {int.class, boolean.class, String.class},
                    60, true, "integration");
            assertEquals(50, reduced);

            NativeMagicDollPowers powers = (NativeMagicDollPowers) invokeValue(
                    attacker, "activeMagicDollPowers", new Class<?>[0]);
            NativeMagicDollEffects.AttackProc proc =
                    NativeMagicDollEffects.attack(powers, () -> 0);
            invoke(attacker, "applyNativeMagicDollAttackProcs",
                    new Class<?>[] {
                            NativeMagicDollEffects.AttackProc.class, long.class,
                            ClientSession.class, BattleQuestEncounter.Snapshot.class,
                            BattleTimeAttackEncounter.Snapshot.class,
                            SourceNpcPhysicalStats.class},
                    proc, target.playerObjectId(), target, null, null, null);

            assertEquals(450, field(target, "playerHp"));
            assertTrue(read(attackerLog).contains(
                    "S_EFFECT magic-doll-proc=121"));
            assertTrue(read(targetLog).contains(
                    "S_HIT_POINT classic-world player=450/500"));
        } finally {
            hub.close();
        }
    }

    @Test
    void nativeMagicDollAutoSkillCastsRefreshesAndClearsImmediately()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.character-selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("magic-doll-auto-state.properties"), false);
        GameHub hub = new GameHub(config, state);
        try (ServerSocket listener = listener();
             Socket client = connect(listener);
             Socket server = listener.accept();
             CaptureLog log = new CaptureLog(temporary.resolve("magic-doll-auto"))) {
            ClientSession session = session(server, config, state, log, hub,
                    "auto-player", 0x3810_2794L, 33051, 32338);
            InventoryCatalog.Definition dollDefinition =
                    new InventoryCatalog.Definition(
                            55_003, 0, 2589, 3963, 1, true,
                            "魔法娃娃：力量", "retained auto-skill doll", 0, 0,
                            false, true, BattleActionCodes.WALK, 0,
                            InventoryCatalog.Effect.MAGIC_DOLL);
            RuntimeInventory inventory = new RuntimeInventory();
            assertTrue(inventory.add(dollDefinition, 1));
            RuntimeInventory.Item dollItem = inventory.items().get(0);
            SourceMagicDollPower autoStrength = new SourceMagicDollPower(
                    130, "strength", "AUTO_SKILL", 0, 0, 0,
                    BattleSpellCatalog.PHYSICAL_ENCHANT_STR,
                    0, 0, 0, 0, 0, 0, 0, 0, 0);
            SourceMagicDollType type = new SourceMagicDollType(
                    55_003, 0, 0, List.of(130),
                    List.of(), List.of(), 30, 6_100,
                    "魔法娃娃：力量", List.of(autoStrength));
            setField(session, "inventory", inventory);
            setField(session, "sourceMagicDolls",
                    Map.of(dollItem.objectId(), type));
            setField(session, "outgoing", new Protocol.CipherState(0x2794));
            hub.connect(session);
            hub.enterClassicWorld(session, 4, 33051, 32338, 2);

            invoke(session, "useNativeMagicDoll",
                    new Class<?>[] {RuntimeInventory.Item.class}, dollItem);
            BattleAbilityStatus ability = (BattleAbilityStatus) field(
                    session, "abilityStatus");
            NativeMagicDoll summoned = (NativeMagicDoll) field(
                    session, "activeMagicDoll");
            assertEquals(BattleSpellCatalog.PHYSICAL_ENCHANT_STR,
                    ability.strengthSkillId(System.nanoTime()));
            assertTrue(read(log).contains(
                    "S_MAGE_STRENGTH magic-doll-auto skill=42"));

            assertTrue(ability.removeSkill(
                    BattleSpellCatalog.PHYSICAL_ENCHANT_STR));
            session.serverMagicDollTick(summoned.expiresAtNanos()
                    - TimeUnit.SECONDS.toNanos(15));
            assertEquals(BattleSpellCatalog.PHYSICAL_ENCHANT_STR,
                    ability.strengthSkillId(System.nanoTime()));
            assertTrue(read(log).contains("reason=ten-second-check"));

            invoke(session, "useNativeMagicDoll",
                    new Class<?>[] {RuntimeInventory.Item.class}, dollItem);
            assertEquals(0, ability.strengthSkillId(System.nanoTime()));
            assertTrue(read(log).contains(
                    "S_MAGE_STRENGTH duration=0"));
        } finally {
            hub.close();
        }
    }

    @Test
    void nativeMagicDollReferencedSpEconomyWeightAndBreathPowersAreLive()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.character-selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("magic-doll-passive-state.properties"), false);
        GameHub hub = new GameHub(config, state);
        try (ServerSocket listener = listener();
             Socket client = connect(listener);
             Socket server = listener.accept();
             CaptureLog log = new CaptureLog(
                     temporary.resolve("magic-doll-passive"))) {
            ClientSession session = session(server, config, state, log, hub,
                    "passive-player", 0x3810_2795L, 33051, 32338);
            InventoryCatalog.Definition dollDefinition =
                    new InventoryCatalog.Definition(
                            55_004, 0, 2589, 3963, 1, true,
                            "魔法娃娃：現行被動能力", "retained passive doll", 0, 0,
                            false, true, BattleActionCodes.WALK, 0,
                            InventoryCatalog.Effect.MAGIC_DOLL);
            RuntimeInventory inventory = new RuntimeInventory();
            assertTrue(inventory.add(dollDefinition, 1));
            RuntimeInventory.Item dollItem = inventory.items().get(0);
            List<SourceMagicDollPower> definitions = List.of(
                    passivePower(106, "PASSIVE_SP", 2),
                    passivePower(97, "PASSIVE_EXP_RATE", 20),
                    passivePower(98, "PASSIVE_DROP_RATE", 5),
                    passivePower(102, "PASSIVE_WEIGHT_REDUCTION", 5),
                    passivePower(137, "STATUS_UNDERWATER", 1));
            SourceMagicDollType type = new SourceMagicDollType(
                    55_004, 0, 0, List.of(106, 97, 98, 102, 137),
                    List.of(), List.of(), 30, 6_100,
                    "魔法娃娃：現行被動能力", definitions);
            setField(session, "inventory", inventory);
            setField(session, "sourceMagicDolls",
                    Map.of(dollItem.objectId(), type));
            setField(session, "outgoing", new Protocol.CipherState(0x2795));
            BattleTimedStatus timed = (BattleTimedStatus) field(
                    session, "timedStatus");
            timed.addBreath(System.nanoTime(), 1_800, 3_600);
            hub.connect(session);
            hub.enterClassicWorld(session, 4, 33051, 32338, 2);

            invoke(session, "useNativeMagicDoll",
                    new Class<?>[] {RuntimeInventory.Item.class}, dollItem);

            assertEquals(2, invokeValue(session, "spellPowerModifier",
                    new Class<?>[] {long.class}, System.nanoTime()));
            BattleInventoryWeight.Profile weighted =
                    (BattleInventoryWeight.Profile) invokeValue(
                            session, "currentWeightProfile",
                            new Class<?>[] {long.class}, System.nanoTime());
            assertEquals(5, weighted.equipmentWeightReductionPercent());
            NativeMagicDollPowers powers = (NativeMagicDollPowers) invokeValue(
                    session, "activeMagicDollPowers", new Class<?>[0]);
            assertEquals(1.20D,
                    powers.percentMultiplier("PASSIVE_EXP_RATE"), 0.000001D);
            assertEquals(1.05D,
                    powers.percentMultiplier("PASSIVE_DROP_RATE"), 0.000001D);
            assertEquals(true, invokeValue(session,
                    "activeMagicDollUnderwaterBreath", new Class<?>[0]));
            assertEquals(0, timed.breath(System.nanoTime()).remainingSeconds());
            String summoned = read(log);
            assertTrue(summoned.contains(
                    "S_BREATH object=" + session.playerObjectId()
                            + " duration=-1 source=magic-doll reason=summon"));
            assertTrue(summoned.contains(
                    "S_MAGIC_STATUS magic-doll-summon"));
            assertTrue(summoned.contains("sp-modifier=2"));

            long recoveryStart = TimeUnit.SECONDS.toNanos(1);
            setField(session, "classicWorldMapId", 63);
            setField(session, "positionX", 32_768);
            setField(session, "positionY", 32_768);
            setField(session, "playerHp", 100);
            invoke(session, "serverClassicWorldMapRecovery",
                    new Class<?>[] {long.class, boolean.class},
                    recoveryStart, true);
            invoke(session, "serverClassicWorldMapRecovery",
                    new Class<?>[] {long.class, boolean.class},
                    recoveryStart + TimeUnit.SECONDS.toNanos(64), true);
            assertEquals(100, field(session, "playerHp"));

            invoke(session, "useNativeMagicDoll",
                    new Class<?>[] {RuntimeInventory.Item.class}, dollItem);
            assertEquals(0, invokeValue(session, "spellPowerModifier",
                    new Class<?>[] {long.class}, System.nanoTime()));
            BattleInventoryWeight.Profile restored =
                    (BattleInventoryWeight.Profile) invokeValue(
                            session, "currentWeightProfile",
                            new Class<?>[] {long.class}, System.nanoTime());
            assertEquals(0, restored.equipmentWeightReductionPercent());
            assertEquals(false, invokeValue(session,
                    "activeMagicDollUnderwaterBreath", new Class<?>[0]));
            String recalled = read(log);
            assertTrue(recalled.contains(
                    "S_BREATH object=" + session.playerObjectId()
                            + " duration=0 source=magic-doll reason=item-recall"));
            assertTrue(recalled.contains("S_MAGIC_STATUS magic-doll-recall"));
            assertTrue(recalled.contains("sp-modifier=0"));
            invoke(session, "serverClassicWorldMapRecovery",
                    new Class<?>[] {long.class, boolean.class},
                    recoveryStart + TimeUnit.SECONDS.toNanos(128), true);
            assertEquals(80, field(session, "playerHp"));
            assertTrue(read(log).contains(
                    "S_HIT_POINT map-recovery=80/500 map=63 rule=6 amount=-20"));
        } finally {
            hub.close();
        }
    }

    @Test
    void retainedMinuteScanDurablyRemovesExpiredItemAndActiveDollFlag()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.character-selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("timed-scan-state.properties"), false);
        GameHub hub = new GameHub(config, state);
        long deadline = 1_900_000_000_000L;
        try (ServerSocket listener = listener();
             Socket client = connect(listener);
             Socket server = listener.accept();
             CaptureLog log = new CaptureLog(temporary.resolve("timed-scan"))) {
            ClientSession session = session(server, config, state, log, hub,
                    "ordinary-player", 0x3810_2788L, 33051, 32338);
            setField(session, "selectedClassicServerName", "TestSource");
            String identity = GameStateStore.sourceCharacter(
                    "TestSource", session.selectedCharacterName());
            RuntimeInventory inventory = new RuntimeInventory();
            long objectId = inventory.addDistinctState(
                    InventoryCatalog.require(InventoryCatalog.SHORT_SWORD),
                    new RuntimeInventory.PersistentItem(
                            0, 1, 0, 0, true, RuntimeInventory.BLESSING_NORMAL,
                            false, false, false, null, 0, deadline, false));
            state.saveEconomyState(identity, 0, inventory.catalogCounts(),
                    inventory.catalogItemStates());
            setField(session, "inventory", inventory);
            setField(session, "inventoryCharacter", identity);
            setField(session, "inventoryBootstrapSent", true);
            setField(session, "outgoing", new Protocol.CipherState(0x2788));
            assertTrue(session.setNativeMagicDollActive(objectId, true));

            session.serverExpireTimedInventory(deadline + 1);

            assertTrue(((RuntimeInventory) field(session, "inventory")).items().isEmpty());
            assertFalse(state.economy(identity).inventory().containsKey(
                    InventoryCatalog.SHORT_SWORD));
            @SuppressWarnings("unchecked")
            Set<Long> activeDolls = (Set<Long>) field(
                    session, "activeMagicDollObjectIds");
            assertTrue(activeDolls.isEmpty());
            assertTrue(read(log).contains(
                    "S_REMOVE_INVENTORY object=" + objectId));
        } finally {
            hub.close();
        }
    }

    @Test
    void retainedGlobalGateSends125WhileWhitelistedMeatStillDrops()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("drop-policy-state.properties"), false);
        GameHub hub = new GameHub(config);
        int x = 33051;
        int y = 32338;
        try (ServerSocket listener = listener();
             Socket client = connect(listener);
             Socket server = listener.accept();
             CaptureLog log = new CaptureLog(temporary.resolve("drop-policy"))) {
            ClientSession session = session(server, config, state, log, hub,
                    "ordinary-player", 0x3810_2782L, x, y);
            RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(Map.of(
                    InventoryCatalog.MAGIC_LANTERN, 1,
                    40056L, 1));
            setField(session, "inventory", inventory);
            setField(session, "outgoing", new Protocol.CipherState(0x2782));
            hub.connect(session);
            hub.enterClassicWorld(session, 4, x, y, 2);

            RuntimeInventory.Item lantern = inventory.items().stream()
                    .filter(item -> item.definition().catalogId()
                            == InventoryCatalog.MAGIC_LANTERN)
                    .findFirst().orElseThrow();
            invokeDrop(session, x, y, lantern.objectId());
            assertTrue(((RuntimeInventory) field(session, "inventory"))
                    .item(lantern.objectId()) != null);
            assertTrue(read(log).contains("S_MESSAGE_CODE C_DROP-drop-disabled=125"));
            assertTrue(hub.classicWorldGroundItemsInScreen(4, -1, x, y).isEmpty());

            RuntimeInventory.Item meat = ((RuntimeInventory) field(
                    session, "inventory")).items().stream()
                    .filter(item -> item.definition().catalogId() == 40056L)
                    .findFirst().orElseThrow();
            invokeDrop(session, x + 1, y, meat.objectId());
            assertEquals(1, hub.classicWorldGroundItemsInScreen(
                    4, -1, x + 1, y).size());
            assertTrue(read(log).contains("S_PUT_OBJECT ground-item="));
        } finally {
            hub.close();
        }
    }

    @Test
    void ordinaryPlayerGetsNativeEquipmentTradabilityAndActiveCollarMessages()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.character-selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("drop-rejection-state.properties"), false);
        GameHub hub = new GameHub(config);
        int x = 33051;
        int y = 32338;
        try (ServerSocket listener = listener();
             Socket client = connect(listener);
             Socket server = listener.accept();
             CaptureLog log = new CaptureLog(temporary.resolve("drop-rejections"))) {
            ClientSession session = session(server, config, state, log, hub,
                    "ordinary-player", 0x3810_2783L, x, y);
            RuntimeInventory inventory = new RuntimeInventory();
            assertTrue(inventory.add(InventoryCatalog.require(
                    InventoryCatalog.SHORT_SWORD), 1));
            RuntimeInventory.Item sword = inventory.items().get(0);
            assertFalse(inventory.toggleWeapon(sword.objectId()).isEmpty());
            InventoryCatalog.Definition bound = new InventoryCatalog.Definition(
                    65_782, 0, 321, 322, 1, true,
                    "Native Untradable", "retained C_DROP test", 0, 0,
                    true, false, BattleActionCodes.WALK, 0,
                    InventoryCatalog.Effect.NONE, 0, 100, 0, null, null);
            assertTrue(inventory.add(bound, 1));
            assertTrue(inventory.add(InventoryCatalog.require(
                    InventoryCatalog.PET_COLLAR), 1));
            RuntimeInventory.Item untradable = inventory.items().stream()
                    .filter(item -> item.definition().catalogId() == 65_782)
                    .findFirst().orElseThrow();
            RuntimeInventory.Item collar = inventory.items().stream()
                    .filter(item -> item.definition().catalogId()
                            == InventoryCatalog.PET_COLLAR)
                    .findFirst().orElseThrow();
            GameStateStore.PetState durable = new GameStateStore.PetState(
                    "drop-test\u001fpet", collar.objectId(), 0x9000_2783L,
                    (int) InventoryCatalog.PET_COLLAR, 45034,
                    "牧羊犬", 12, 78, 17, 21_375, 0);
            @SuppressWarnings("unchecked")
            List<BattleCompanion> companions = (List<BattleCompanion>) field(
                    session, "tamedCompanions");
            companions.add(BattleCompanion.pet(
                    durable, SourcePetTemplate.memoryFixture(45034), x + 1, y));
            setField(session, "inventory", inventory);
            setField(session, "outgoing", new Protocol.CipherState(0x2783));
            hub.connect(session);
            hub.enterClassicWorld(session, 4, x, y, 2);

            invokeDrop(session, x, y, sword.objectId());
            invokeDrop(session, x, y, untradable.objectId());
            invokeDrop(session, x, y, collar.objectId());

            String capture = read(log);
            assertTrue(capture.contains("S_MESSAGE_CODE C_DROP-equipped=125"));
            assertTrue(capture.contains("S_MESSAGE_CODE C_DROP-not-tradable=210"));
            assertTrue(capture.contains("S_MESSAGE_CODE C_DROP-active-pet-collar=210"));
            assertEquals(3, ((RuntimeInventory) field(session, "inventory")).size());
            assertTrue(hub.classicWorldGroundItemsInScreen(4, -1, x, y).isEmpty());
        } finally {
            hub.close();
        }
    }

    @Test
    void timedActiveDollAndSealedItemsUseExactNativeDropMessages()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.character-selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("drop-instance-state.properties"), false);
        GameHub hub = new GameHub(config);
        int x = 33051;
        int y = 32338;
        try (ServerSocket listener = listener();
             Socket client = connect(listener);
             Socket server = listener.accept();
             CaptureLog log = new CaptureLog(temporary.resolve("drop-instance-state"))) {
            ClientSession session = session(server, config, state, log, hub,
                    "ordinary-player", 0x3810_2787L, x, y);
            RuntimeInventory inventory = new RuntimeInventory();
            InventoryCatalog.Definition sword = InventoryCatalog.require(
                    InventoryCatalog.SHORT_SWORD);
            long timed = inventory.addDistinctState(sword,
                    new RuntimeInventory.PersistentItem(
                            0, 1, 0, 0, true, RuntimeInventory.BLESSING_NORMAL,
                            false, false, false, null, 0,
                            1_900_000_000_000L, false));
            long doll = inventory.addDistinctState(sword,
                    new RuntimeInventory.PersistentItem(
                            0, 1, 0, 0, true, RuntimeInventory.BLESSING_NORMAL,
                            false, false, false, null, 0, 0L, false));
            long sealed = inventory.addDistinctState(sword,
                    new RuntimeInventory.PersistentItem(
                            0, 1, 0, 0, true, RuntimeInventory.BLESSING_NORMAL,
                            false, false, false, null, 0, 0L, true));
            assertTrue(timed > 0 && doll > 0 && sealed > 0);
            setField(session, "inventory", inventory);
            assertTrue(session.setNativeMagicDollActive(doll, true));
            assertFalse(session.setNativeMagicDollActive(0x7FFF_FFFFL, true));
            setField(session, "outgoing", new Protocol.CipherState(0x2787));
            hub.connect(session);
            hub.enterClassicWorld(session, 4, x, y, 2);

            invokeDrop(session, x, y, timed);
            invokeDrop(session, x, y, doll);
            invokeDrop(session, x, y, sealed);

            String capture = read(log);
            assertTrue(capture.contains("S_MESSAGE_CODE C_DROP-timed-item=210"));
            assertTrue(capture.contains(
                    "S_MESSAGE_CODE C_DROP-active-magic-doll=1181"));
            assertTrue(capture.contains("S_MESSAGE_CODE C_DROP-sealed-item=210"));
            assertEquals(3, ((RuntimeInventory) field(session, "inventory")).size());
            assertTrue(hub.classicWorldGroundItemsInScreen(4, -1, x, y).isEmpty());
        } finally {
            hub.close();
        }
    }

    @Test
    void administratorBypassesTheRetainedGlobalAndTradabilityGates()
            throws Exception {
        ServerConfig config = ServerConfig.load(
                Path.of("src/test/resources/server-config/config/server.character-selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("drop-admin-state.properties"), false);
        GameHub hub = new GameHub(config);
        int x = 33051;
        int y = 32338;
        try (ServerSocket listener = listener();
             Socket client = connect(listener);
             Socket server = listener.accept();
             CaptureLog log = new CaptureLog(temporary.resolve("drop-admin"))) {
            ClientSession session = session(server, config, state, log, hub,
                    "test", 0x3810_2786L, x, y);
            InventoryCatalog.Definition untradable = new InventoryCatalog.Definition(
                    65_783, 0, 321, 322, 1, true,
                    "GM Drop", "retained GM bypass", 0, 0,
                    true, false, BattleActionCodes.WALK, 0,
                    InventoryCatalog.Effect.NONE, 0, 100, 0, null, null);
            RuntimeInventory inventory = new RuntimeInventory();
            long expiresAt = 1_900_000_000_000L;
            long objectId = inventory.addDistinctState(untradable,
                    new RuntimeInventory.PersistentItem(
                            0, 1, 0, 0, true, RuntimeInventory.BLESSING_NORMAL,
                            false, false, false, null, 0, expiresAt, false));
            assertTrue(objectId > 0);
            setField(session, "inventory", inventory);
            setField(session, "outgoing", new Protocol.CipherState(0x2786));
            hub.connect(session);
            hub.enterClassicWorld(session, 4, x, y, 2);

            invokeDrop(session, x, y, objectId);

            assertTrue(((RuntimeInventory) field(session, "inventory"))
                    .items().isEmpty());
            ClassicWorldGroundItems.Item ground = hub.classicWorldGroundItemsInScreen(
                    4, -1, x, y).get(0);
            assertEquals(65_783, ground.definition().catalogId());
            assertEquals(expiresAt, ground.state().expiresAtEpochMillis());
            assertTrue(read(log).contains("S_PUT_OBJECT ground-item="));

            invokeGet(session, x, y, ground.objectId());
            assertEquals(65_783, ((RuntimeInventory) field(session, "inventory"))
                    .items().get(0).definition().catalogId());
            assertEquals(expiresAt, ((RuntimeInventory) field(session, "inventory"))
                    .items().get(0).expiresAtEpochMillis());
            assertTrue(hub.classicWorldGroundItemsInScreen(4, -1, x, y).isEmpty());
        } finally {
            hub.close();
        }
    }

    @Test
    void battleGroundAddAndRemoveArePublishedOnlyInsideNativeScreen()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("battle-ground-screen-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 2, InetAddress.getByName("127.0.0.1"));
             Socket nearClient = connect(listener);
             Socket nearServer = listener.accept();
             Socket farClient = connect(listener);
             Socket farServer = listener.accept();
             CaptureLog nearLog = new CaptureLog(temporary.resolve("ground-near"));
             CaptureLog farLog = new CaptureLog(temporary.resolve("ground-far"))) {
            ClientSession near = session(nearServer, config, state, nearLog, hub,
                    "near", 0x3810_2784L, 32740, 32798);
            ClientSession far = session(farServer, config, state, farLog, hub,
                    "far", 0x3810_2785L, 33000, 33000);
            setField(near, "outgoing", new Protocol.CipherState(0x2784));
            setField(far, "outgoing", new Protocol.CipherState(0x2785));
            hub.connect(near);
            hub.connect(far);
            hub.enterChannel(0, near);
            hub.enterChannel(0, far);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Ground visibility", "", 2,
                    BattleRules.Mode.DEATHMATCH.wireId(), 0, 0, 0, 0));
            hub.joinRoom(roomId, near);
            hub.joinRoom(roomId, far);
            setBattleContext(near, roomId, 32740, 32798);
            setBattleContext(far, roomId, 33000, 33000);

            BattleGroundItems table = new BattleGroundItems();
            BattleGroundItems.Item item = table.put(
                    InventoryCatalog.require(InventoryCatalog.NATIVE_HEALING_POTION),
                    1, 32741, 32798);
            invoke(near, "broadcastGroundItem",
                    new Class<?>[] {BattleGroundItems.Item.class, String.class},
                    item, "visibility-test");
            invoke(near, "broadcastGroundTake",
                    new Class<?>[] {GameHub.GroundTake.class, String.class},
                    new GameHub.GroundTake(item, null), "visibility-test");

            String nearCapture = read(nearLog);
            String farCapture = read(farLog);
            assertTrue(nearCapture.contains(
                    "S_PUT_OBJECT ground-item=" + item.objectId()));
            assertTrue(nearCapture.contains(
                    "S_REMOVE_OBJECT ground-item=" + item.objectId()));
            assertFalse(farCapture.contains("ground-item=" + item.objectId()));
        } finally {
            hub.close();
        }
    }

    private static ClientSession session(
            Socket socket, ServerConfig config, GameStateStore state,
            CaptureLog log, GameHub hub, String account,
            long objectId, int x, int y) throws Exception {
        ClientSession session = new ClientSession(
                socket, ClientSession.Endpoint.GAME, config, null, state, log, hub);
        CharacterSummary character = new CharacterSummary(
                "Drop" + Long.toHexString(objectId), "", 3, 0, 50, 500, 100,
                10, 12, 12, 12, 12, 19, 12, 18, 12,
                objectId, 100, 0, 0, 0, 0, 0, x, y, 4,
                12, 40, 1, 12, 12, 0);
        setField(session, "account", account);
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

    private static void setBattleContext(
            ClientSession session, long roomId, int x, int y) throws Exception {
        setPhase(session, "BATTLE");
        setField(session, "currentRoomId", roomId);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
    }

    private static void invokeDrop(
            ClientSession session, int x, int y, long objectId) throws Exception {
        invoke(session, "dropInventoryItem", new Class<?>[] {byte[].class},
                new Protocol.Writer().c(Protocol.C_DROP)
                        .h(x).h(y).d(objectId).d(1).bytes());
    }

    private static void invokeGet(
            ClientSession session, int x, int y, long objectId) throws Exception {
        invoke(session, "getGroundItem", new Class<?>[] {byte[].class},
                new Protocol.Writer().c(Protocol.C_GET)
                        .h(Protocol.groundPickupWireX(x)).h(y)
                        .d(objectId).d(1).c(0).bytes());
    }

    private static SourceMagicDollPower passivePower(
            int id, String className, int valueMain) {
        return new SourceMagicDollPower(
                id, "native-" + id, className, valueMain,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private static void invoke(Object target, String name, Class<?>[] types,
                               Object... arguments) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        method.invoke(target, arguments);
    }

    private static Object invokeValue(
            Object target, String name, Class<?>[] types,
            Object... arguments) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, arguments);
    }

    private static ServerSocket listener() throws Exception {
        return new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
    }

    private static Socket connect(ServerSocket listener) throws Exception {
        return new Socket("127.0.0.1", listener.getLocalPort());
    }

    private static String read(CaptureLog log) throws Exception {
        return Files.readString(log.path());
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

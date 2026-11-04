package local.lito381.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the complete configured TeleportMe handoff to the combat consumers.
 * Merely retaining a copied inventory is insufficient: the selected rental
 * weapon must drive the actor walk family, the S_ATTACK action and physical
 * damage, while the original character snapshot is restored at match end.
 */
final class ClientSessionRentalBattleHandoffIntegrationTest {
    @TempDir Path temporary;

    @Test
    void newGameEndpointConsumesRentalLoadoutIntoActorAttackAndResultRestore()
            throws Exception {
        Path journal = temporary.resolve("shared-rental-handoff.properties");
        ServerConfig townConfig = config("town.properties", journal);
        ServerConfig battleConfig = config("battle.properties", journal);
        GameStateStore townState = new GameStateStore(
                temporary.resolve("town-state.properties"), false);
        GameStateStore battleState = new GameStateStore(
                temporary.resolve("battle-state.properties"), false);
        GameHub townHub = new GameHub(townConfig);
        GameHub battleHub = new GameHub(battleConfig);
        try (MemoryAccountRepository sourceAccounts =
                    new MemoryAccountRepository("test", "test");
             ServerSocket listener = new ServerSocket(
                    0, 2, InetAddress.getByName("127.0.0.1"));
             Socket townClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket townServer = listener.accept();
             Socket battleClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket battleServer = listener.accept()) {
            battleClient.setSoTimeout(2_000);
            ClientSession town = new ClientSession(
                    townServer, ClientSession.Endpoint.GAME, townConfig,
                    sourceAccounts, townState,
                    new CaptureLog(temporary.resolve("town-capture")), townHub);
            ClientSession battle = new ClientSession(
                    battleServer, ClientSession.Endpoint.GAME, battleConfig,
                    sourceAccounts, battleState,
                    new CaptureLog(temporary.resolve("battle-capture")), battleHub);

            String account = "rental-handoff-account";
            CharacterSummary knight = character("RentalBattleKnight", 1,
                    0x3810_0809L);
            RuntimeInventory sourceInventory = sourceBaseline();
            initialize(town, account, knight, sourceInventory);
            initialize(battle, account, knight, sourceInventory);
            townHub.connect(town);
            battleHub.connect(battle);
            assertTrue(townHub.acceptAuthenticatedAccount(
                    town, ClientSession.Endpoint.GAME, account));

            RentalLoadoutPlan plan = RentalLoadoutPlan.require(1, 1);
            RentalLoadoutPlan.Staged staged = plan.stage(
                    RentalLoadoutPlan.StatePolicy.IDENTIFIED_TABLE_SLOTS,
                    new AtomicLong(0x7300_0000L)::getAndIncrement);
            RuntimeInventory.Item rentalWeapon = staged.inventory().equippedWeaponItem();
            assertEquals(66L, rentalWeapon.definition().catalogId());
            assertEquals(10, rentalWeapon.enchantLevel());
            assertEquals(BattleActionCodes.TWO_HAND_SWORD_WALK,
                    staged.inventory().equippedWalkAction());
            assertTrue(townHub.stageRentalBattleHandoff(
                    account, battle.sourceCharacterIdentity(), staged.inventory()));

            NativeLitoRentalTeleportEndpointCatalog.Endpoint destination =
                    NativeLitoRentalTeleportEndpointCatalog.parse(
                            "TeleportMe|LT_TELEPORT_B|Battle|127.0.0.2|32100")
                            .get(0);
            setField(town, "rentalTownTransferPending", true);
            setField(town, "pendingRentalTeleportEndpoint", destination);
            assertTrue(town.authorizesAuthenticatedEndpointTransfer(account));
            town.completeAuthenticatedEndpointTransfer();
            townHub.disconnect(town);
            assertTrue(battleHub.acceptAuthenticatedAccount(
                    battle, ClientSession.Endpoint.GAME, account));
            assertFalse(town.transportOpen(),
                    "the S_F4 source GAME socket must retire before reconnect");
            assertTrue(battleHub.hasRentalBattleHandoff(
                    account, battle.sourceCharacterIdentity()));

            int handoffSeed = 0x3810_0711;
            setField(battle, "selectedCharacter", "");
            setField(battle, "inventoryCharacter",
                    battle.sourceCharacterIdentity(knight.name()));
            setPhase(battle, "LOBBY");
            setField(battle, "outgoing", new Protocol.CipherState(handoffSeed));
            Protocol.CipherState handoffReceiver = new Protocol.CipherState(handoffSeed);
            assertTrue((boolean) invokeResult(battle,
                    "autoSelectPendingRentalBattleHandoff",
                    new Class<?>[0]));
            Protocol.Reader selectionResult = new Protocol.Reader(receiveUntilOpcode(
                    battleClient, handoffReceiver,
                    Protocol.S_LITO_SELECT_CHAR_RESULT, 64));
            assertEquals(Protocol.S_LITO_SELECT_CHAR_RESULT,
                    selectionResult.c());
            assertEquals(Protocol.LitoCharacterSelectResult.ACCEPTED.wireValue(),
                    selectionResult.c());
            assertEquals(Protocol.S_ADD_INVENTORY_BATCH,
                    receiveUntilOpcode(battleClient, handoffReceiver,
                            Protocol.S_ADD_INVENTORY_BATCH, 64)[0] & 0xFF);
            assertEquals(knight.name(), battle.selectedCharacterName());
            assertFalse(battleHub.hasRentalBattleHandoff(
                    account, battle.sourceCharacterIdentity()));
            assertTrue((boolean) field(battle, "rentalBattleLoadoutActive"));
            assertEquals(plan.expectedCatalogCounts(), inventory(battle).catalogCounts());

            int expectedWalk = BattleAnimationCatalog.get().resolveEquippedWalkAction(
                    battle.battleGfx(), BattleActionCodes.TWO_HAND_SWORD_WALK);
            int expectedAttack = BattleActionCodes.attackForWalk(expectedWalk);
            assertEquals(expectedWalk, battle.battleWalkAction());
            assertNotEquals(expectedWalk, expectedAttack,
                    "weapon attacks must not reuse the movement action");
            SourceWeaponPhysicalStats activeWeapon = (SourceWeaponPhysicalStats) invokeResult(
                    battle, "activePhysicalWeapon", new Class<?>[0]);
            SourceWeaponPhysicalStats stagedWeapon =
                    staged.inventory().equippedPhysicalWeapon();
            assertEquals(66, activeWeapon.itemId());
            assertEquals(10, activeWeapon.enchantLevel());
            assertEquals(stagedWeapon.weaponType(), activeWeapon.weaponType());
            assertEquals(stagedWeapon.damageSmall(), activeWeapon.damageSmall());
            assertEquals(stagedWeapon.damageLarge(), activeWeapon.damageLarge());
            assertTrue(activeWeapon.present());
            SourceEquipmentMagicStats activeEquipment =
                    (SourceEquipmentMagicStats) invokeResult(
                            battle, "requiredEquipmentMagicStats", new Class<?>[0]);
            RentalEquipmentBonuses bonuses =
                    staged.inventory().equippedRentalBonuses();
            assertTrue(bonuses.matchedItems() > 0);
            assertEquals(12 + bonuses.strength(),
                    activeEquipment.effectiveStrength(12));
            assertEquals(bonuses.magicResistance(),
                    activeEquipment.totalDirectMagicResistance());
            assertEquals(bonuses.meleeHit(),
                    activeEquipment.physical().totalHit(false, false));

            // Drain leftover handoff frames before the combat cipher takes over.
            // autoSelect may still have START/TIMELIMIT/status bytes queued on
            // the 0x3810_0711 stream; decrypting them with 0x3810_0809 times out.
            battleClient.setSoTimeout(150);
            try {
                while (true) receive(battleClient, handoffReceiver);
            } catch (SocketTimeoutException drained) {
                // socket is now aligned with the battle cipher below
            }
            battleClient.setSoTimeout(2_000);

            int seed = 0x3810_0809;
            setField(battle, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            // GameHub's countdown callback accepts only the room/battle-capable
            // session states used by the real C_CF -> C_D7 flow.
            setPhase(battle, "BATTLE");
            long roomId = startSoloBattle(battleHub, battle);
            setField(battle, "currentRoomId", roomId);

            BattleBootstrapEvidence bootstrap = receiveBattleBootstrap(
                    battleClient, receiver, battle.playerObjectId(), 512);
            Protocol.Reader status = new Protocol.Reader(bootstrap.status());
            assertEquals(Protocol.S_STATUS, status.c());
            assertEquals(battle.playerObjectId(), status.d());
            assertEquals(knight.level(), status.c());
            status.d(); // effective classic experience
            assertEquals(knight.strength() + bonuses.strength(), status.c());
            assertEquals(knight.intelligence() + bonuses.intelligence(), status.c());
            assertEquals(knight.wisdom() + bonuses.wisdom(), status.c());
            assertEquals(knight.dexterity() + bonuses.dexterity(), status.c());
            assertEquals(knight.constitution() + bonuses.constitution(), status.c());
            assertEquals(knight.charisma() + bonuses.charisma(), status.c());
            int expectedMaximumHp = knight.maxHp()
                    + staged.inventory().equippedNativeArmorBonuses().maximumHp();
            int expectedMaximumMp = knight.maxMp()
                    + staged.inventory().equippedNativeArmorBonuses().maximumMp();
            assertEquals(expectedMaximumHp, status.h());
            assertEquals(expectedMaximumHp, status.h());
            assertEquals(expectedMaximumMp, status.h());
            assertEquals(expectedMaximumMp, status.h());
            assertEquals(battle.battleArmorClass(), (byte) status.c());

            Protocol.Reader actor = new Protocol.Reader(bootstrap.actor());
            assertEquals(Protocol.S_PUT_OBJECT, actor.c());
            actor.h();
            actor.h();
            assertEquals(battle.playerObjectId(), actor.d());
            assertEquals(battle.battleGfx(), actor.h());
            assertEquals(expectedWalk, actor.c(),
                    "S_PUT_OBJECT must publish the handed-off weapon walk family");

            ArenaMapCatalog.Point bot = battleHub.botPosition(roomId);
            setField(battle, "positionX", bot.x() - 1);
            setField(battle, "positionY", bot.y());
            int botHpBefore = battleHub.botHp(roomId);
            handle(battle, new Protocol.Writer().c(Protocol.C_ATTACK).d(2)
                    .h(bot.x()).h(bot.y()).bytes());
            byte[] attackPacket = receiveUntilOpcode(
                    battleClient, receiver, Protocol.S_ATTACK, 512);
            Protocol.Reader attack = new Protocol.Reader(attackPacket);
            assertEquals(Protocol.S_ATTACK, attack.c());
            assertEquals(expectedAttack, attack.c(),
                    "S_ATTACK must use the attack paired with the rental weapon walk family");
            assertEquals(battle.playerObjectId(), attack.d());
            assertEquals(2L, attack.d());
            assertTrue(attack.c() > 0, "rental weapon must feed positive physical damage");
            assertTrue(battleHub.botHp(roomId) < botHpBefore);

            invoke(battle, "resetNativeTournamentEconomy",
                    new Class<?>[] {String.class}, "integration-result");
            assertFalse((boolean) field(battle, "rentalBattleLoadoutActive"));
            assertEquals(sourceInventory.catalogCounts(), inventory(battle).catalogCounts());
            assertEquals(InventoryCatalog.SHORT_SWORD,
                    inventory(battle).items().get(0).definition().catalogId());
            assertFalse(inventory(battle).catalogCounts().containsKey(66L));
            assertEquals(0, field(battle, "sourceInitialArmorClassModifier"),
                    "the destination selector restores its qualified source baseline");
            assertFalse((boolean) field(battle, "sourceWeaponSuppressed"));
        } finally {
            townHub.close();
            battleHub.close();
        }
    }

    private ServerConfig config(String name, Path journal) throws Exception {
        Path path = temporary.resolve(name);
        Files.writeString(path, """
                bind=127.0.0.1
                game.bind=127.0.0.2
                database.mode=memory
                database.read.only=true
                state.persistence.enabled=false
                lito.rental.handoff.file=%s
                """.formatted(journal.toAbsolutePath().normalize().toString()
                        .replace('\\', '/')), StandardCharsets.UTF_8);
        return ServerConfig.load(path);
    }

    private static RuntimeInventory sourceBaseline() {
        RuntimeInventory source = new RuntimeInventory();
        source.addDistinctState(InventoryCatalog.require(InventoryCatalog.SHORT_SWORD),
                new RuntimeInventory.PersistentItem(
                        0x7000_0809L, 1, 0, 0, true, 1, true, true));
        return source;
    }

    private static void initialize(ClientSession session, String account,
                                   CharacterSummary character,
                                   RuntimeInventory sourceInventory) throws Exception {
        SourceEquipmentMagicStats magic = SourceEquipmentMagicStats.none();
        SourceEquipmentVitalStats vital = SourceEquipmentVitalStats.none();
        SourceEquippedFireResistance fire = new SourceEquippedFireResistance(17, 1);
        setField(session, "account", account);
        setField(session, "selectedCharacter", character.name());
        setField(session, "inventoryCharacter", character.name());
        setField(session, "characters", List.of(character));
        setField(session, "playerClassicLevel", character.level());
        setField(session, "playerClassicExperience", character.experience());
        setField(session, "inventory", sourceInventory.copy());
        setField(session, "sourceBaseMagicResistance",
                SourceBaseMagicResistance.from(character));
        setField(session, "sourceEquipmentMagicStats", magic);
        setField(session, "sourceEquipmentVitalStats", vital);
        setField(session, "sourceEquippedFireResistance", fire);
        setField(session, "nativeTournamentMagicBaseline", magic);
        setField(session, "nativeTournamentVitalBaseline", vital);
        setField(session, "nativeTournamentFireResistanceBaseline", fire);
        setField(session, "sourcePhysicalGrowthStats",
                SourcePhysicalGrowthStats.classic381());
        setField(session, "sourceInventoryWeightStats",
                SourceInventoryWeightStats.none(100.0));
        setField(session, "retainedSourceInventoryWeight", 0);
        setField(session, "sourceInitialArmorClassModifier", 7);
        setField(session, "nativeTournamentEconomyInitialized", true);
    }

    private static long startSoloBattle(GameHub hub, ClientSession session)
            throws Exception {
        hub.enterChannel(0, session);
        // ArenaBot exists only on the channel default test room with one human
        // and no virtual roster. A user-created room cannot satisfy that.
        long roomId = hub.roomSnapshots(0).stream()
                .filter(room -> "Local Lobby".equals(room.settings().name()))
                .findFirst()
                .orElseThrow()
                .id();
        hub.joinRoom(roomId, session);
        setField(session, "currentRoomId", roomId);
        hub.setReady(roomId, session, true);
        Method countdown = GameHub.class.getDeclaredMethod("countdownWork", long.class);
        countdown.setAccessible(true);
        long future = System.nanoTime() + 10_000_000_000L;
        countdown.invoke(hub, future);
        countdown.invoke(hub, future + 2_000_000_000L);
        countdown.invoke(hub, future + 4_000_000_000L);
        assertTrue(hub.botPosition(roomId) != null);
        // countdownWork is the synchronized state transition. The production
        // countdownTickSafely wrapper then invokes these two callbacks in this
        // exact order for every combatant.
        session.serverPrepareBattle();
        session.serverStartBattle();
        return roomId;
    }

    private static BattleBootstrapEvidence receiveBattleBootstrap(
            Socket client, Protocol.CipherState receiver,
            long objectId, int maximumFrames) throws Exception {
        byte[] status = null;
        for (int index = 0; index < maximumFrames; index++) {
            byte[] packet = receive(client, receiver);
            if ((packet[0] & 0xFF) == Protocol.S_STATUS) status = packet;
            if ((packet[0] & 0xFF) == Protocol.S_PUT_OBJECT
                    && packet.length >= 10 && unsignedInt(packet, 5) == objectId) {
                if (status == null) {
                    throw new AssertionError("self actor arrived before S_STATUS");
                }
                return new BattleBootstrapEvidence(status, packet);
            }
        }
        throw new AssertionError("missing self S_PUT_OBJECT for " + objectId);
    }

    private static byte[] receiveUntilOpcode(
            Socket client, Protocol.CipherState receiver,
            int opcode, int maximumFrames) throws Exception {
        for (int index = 0; index < maximumFrames; index++) {
            byte[] packet = receive(client, receiver);
            if ((packet[0] & 0xFF) == opcode) return packet;
        }
        throw new AssertionError("missing encrypted opcode " + opcode);
    }

    private static byte[] receive(Socket client, Protocol.CipherState receiver)
            throws Exception {
        return receiver.decrypt(Protocol.readFrame(client.getInputStream(), 8_192));
    }

    private static void handle(ClientSession session, byte[] payload) throws Exception {
        invoke(session, "handle", new Class<?>[] {byte[].class}, payload);
    }

    private static void invoke(ClientSession session, String name,
                               Class<?>[] parameters, Object... arguments)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        method.invoke(session, arguments);
    }

    private static Object invokeResult(ClientSession session, String name,
                                       Class<?>[] parameters, Object... arguments)
            throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method.invoke(session, arguments);
    }

    private static RuntimeInventory inventory(ClientSession session) throws Exception {
        return (RuntimeInventory) field(session, "inventory");
    }

    private static long unsignedInt(byte[] payload, int offset) {
        return Integer.toUnsignedLong((payload[offset] & 0xFF)
                | ((payload[offset + 1] & 0xFF) << 8)
                | ((payload[offset + 2] & 0xFF) << 16)
                | ((payload[offset + 3] & 0xFF) << 24));
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

    private static CharacterSummary character(String name, int type, long objectId) {
        return new CharacterSummary(name, "", type, 0, 50, 100, 100,
                10, 12, 12, 12, 12, 18, 12, 18, 12,
                objectId, 100, 0, 0, 0, 32740, 32798, 4,
                12, 40, 1, 12, 12, 0);
    }

    private record BattleBootstrapEvidence(byte[] status, byte[] actor) {}
}

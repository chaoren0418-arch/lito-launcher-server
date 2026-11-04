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
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

final class ClientSessionNativeTournamentEconomyTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeServerProperties();
    }

    @TempDir Path temporary;

    @Test
    void encryptedChannelWeaponUsePersistsLobbyLoadoutWithoutWorldWield()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("channel-weapon-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(250);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("channel-weapon-capture")), hub);
            RuntimeInventory live = new RuntimeInventory();
            assertTrue(live.add(InventoryCatalog.require(
                    InventoryCatalog.TRAINING_SWORD), 1));
            RuntimeInventory.Item sword = live.items().get(0);
            initialize(session, live);
            setPhase(session, "CHANNEL");
            int seed = 0x3810_0489;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            byte[] use = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                    .d(sword.objectId()).s("").bytes();
            invoke(session, "useInventoryItem", new Class<?>[] {byte[].class}, use);

            byte[] detail = receive(client, receiver);
            assertEquals(Protocol.S_CHANGE_ITEM_DESC, detail[0] & 0xFF);
            assertEquals(sword.objectId(), unsignedInt(detail, 1));
            assertTrue(inventory(session).item(sword.objectId()).equipped());
            assertTrue(state.economy(session.sourceCharacterIdentity())
                    .itemStates().values().stream().flatMap(List::stream)
                    .anyMatch(item -> item.objectId() == sword.objectId()
                            && item.equipped()));
            // S_CHANGE_ITEM_USE is the native use-state/charge byte, not an
            // equipment flag. CHANNEL also has no actor for S_WIELD.
            try {
                receive(client, receiver);
                throw new AssertionError("unexpected lobby world-object frame");
            } catch (SocketTimeoutException expected) {
                // Exact expected boundary.
            }
        }
    }

    @Test
    void encryptedRoomWeaponUsePersistsPreBattleLoadoutWithoutWorldWield()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("room-weapon-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(250);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("room-weapon-capture")), hub);
            RuntimeInventory live = new RuntimeInventory();
            assertTrue(live.add(InventoryCatalog.require(
                    InventoryCatalog.TRAINING_SWORD), 1));
            RuntimeInventory.Item sword = live.items().get(0);
            initialize(session, live);
            setPhase(session, "ROOM");
            int seed = 0x3810_0491;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            byte[] use = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                    .d(sword.objectId()).s("").bytes();
            invoke(session, "useInventoryItem", new Class<?>[] {byte[].class}, use);

            byte[] detail = receive(client, receiver);
            assertEquals(Protocol.S_CHANGE_ITEM_DESC, detail[0] & 0xFF);
            assertEquals(sword.objectId(), unsignedInt(detail, 1));
            assertTrue(inventory(session).item(sword.objectId()).equipped());
            assertTrue(state.economy(session.sourceCharacterIdentity())
                    .itemStates().values().stream().flatMap(List::stream)
                    .anyMatch(item -> item.objectId() == sword.objectId()
                            && item.equipped()));
            try {
                receive(client, receiver);
                throw new AssertionError("unexpected pre-battle world-object frame");
            } catch (SocketTimeoutException expected) {
                // ROOM, like CHANNEL, has no actor for S_WIELD.
            }
        }
    }

    @Test
    void encryptedChannelClassCompatibleArmorUsePersistsLobbyLoadout()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("channel-armor-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("channel-armor-capture")), hub);
            RuntimeInventory live = new RuntimeInventory();
            assertTrue(live.add(InventoryCatalog.require(
                    InventoryCatalog.CKEW_WIZARD_REWARD), 1));
            RuntimeInventory.Item armor = live.items().get(0);
            initialize(session, live);
            setField(session, "characters", List.of(character(
                    "NativeEconomyKnight", 3, 0x3810_0042L)));
            setField(session, "playerHp", 100);
            setField(session, "playerMp", 100);
            setPhase(session, "CHANNEL");
            int seed = 0x3810_0490;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            byte[] use = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                    .d(armor.objectId()).s("").bytes();
            invoke(session, "useInventoryItem", new Class<?>[] {byte[].class}, use);

            byte[] detail = receive(client, receiver);
            assertEquals(Protocol.S_CHANGE_ITEM_DESC, detail[0] & 0xFF);
            assertEquals(armor.objectId(), unsignedInt(detail, 1));
            assertTrue(inventory(session).item(armor.objectId()).equipped());
        }
    }

    @Test
    void encryptedRoomClassCompatibleArmorUsePersistsPreBattleLoadout()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("room-armor-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("room-armor-capture")), hub);
            RuntimeInventory live = new RuntimeInventory();
            assertTrue(live.add(InventoryCatalog.require(
                    InventoryCatalog.CKEW_WIZARD_REWARD), 1));
            RuntimeInventory.Item armor = live.items().get(0);
            initialize(session, live);
            setField(session, "characters", List.of(character(
                    "NativeEconomyKnight", 3, 0x3810_0042L)));
            setField(session, "playerHp", 100);
            setField(session, "playerMp", 100);
            setPhase(session, "ROOM");
            int seed = 0x3810_0492;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            byte[] use = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                    .d(armor.objectId()).s("").bytes();
            invoke(session, "useInventoryItem", new Class<?>[] {byte[].class}, use);

            byte[] detail = receive(client, receiver);
            assertEquals(Protocol.S_CHANGE_ITEM_DESC, detail[0] & 0xFF);
            assertEquals(armor.objectId(), unsignedInt(detail, 1));
            assertTrue(inventory(session).item(armor.objectId()).equipped());
        }
    }

    @Test
    void encryptedShopTransportPublishesSixteenRowsCommitsAndListsOnlyMatchItems()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("wire-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("wire-capture")), hub);
            RuntimeInventory baseline = baseline();
            initialize(session, baseline);
            setPhase(session, "CHANNEL");
            int seed = 0x3810_0367;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            invoke(session, "sendBuyList", new Class<?>[0]);
            byte[] buyList = receive(client, receiver);
            assertEquals(Protocol.S_LITO_BUY_LIST, buyList[0] & 0xFF);
            assertEquals(16, unsignedShort(buyList, 1));
            int offset = 3;
            List<Long> ids = new ArrayList<>();
            List<Long> prices = new ArrayList<>();
            for (int index = 0; index < 16; index++) {
                ids.add(unsignedInt(buyList, offset));
                offset += 4;
                offset += 2; // icon
                prices.add(unsignedInt(buyList, offset));
                offset += 4;
                offset = zeroTerminatedEnd(buyList, offset);
                int detailLength = buyList[offset++] & 0xFF;
                offset += detailLength;
            }
            assertEquals(buyList.length, offset);
            assertEquals(TournamentShopCatalog.offers(true).stream()
                    .map(TournamentShopCatalog.Offer::catalogId).toList(), ids);
            assertEquals(TournamentShopCatalog.offers(true).stream()
                    .map(TournamentShopCatalog.Offer::buyPrice).toList(), prices);

            byte[] transaction = new Protocol.Writer().c(Protocol.C_LITO_BUY_SELL)
                    .c(0).h(1).d(InventoryCatalog.EXTRA_HEALING_POTION).d(2)
                    .raw(new byte[8]).bytes();
            invoke(session, "buySell", new Class<?>[] {byte[].class}, transaction);
            assertEquals(Protocol.S_ADD_INVENTORY,receive(client,receiver)[0]&255);
            byte[] points = receive(client, receiver);
            assertEquals(Protocol.S_LITO_POINT, points[0] & 0xFF);
            int pointOffset = zeroTerminatedEnd(points, 1);
            assertEquals(19_600L, unsignedInt(points, pointOffset));
            assertTrue(shopNotice(client, receiver).contains("購買成功"));

            invoke(session, "sendSellList", new Class<?>[0]);
            byte[] sellList = receive(client, receiver);
            assertEquals(Protocol.S_LITO_SELL_LIST, sellList[0] & 0xFF);
            assertEquals(1, unsignedShort(sellList, 1));
            long purchasedObjectId = unsignedInt(sellList, 3);
            assertEquals(200L, unsignedInt(sellList, 7));
            assertEquals(InventoryCatalog.EXTRA_HEALING_POTION,
                    inventory(session).item(purchasedObjectId).definition().catalogId());

            invokeReset(session, "wire-exit");
            assertEquals(baseline.catalogCounts(), inventory(session).catalogCounts());
            assertEquals(20_000L, field(session, "shopPoints"));
        }
    }

    @Test
    void tournamentShopIsNonGameplayAndEachListAuthorizesOneMatchingConfirmation()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("shop-phase-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(250);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("shop-phase-capture")), hub);
            initialize(session, baseline());
            int seed = 0x3810_0599;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            setPhase(session, "BATTLE");
            invoke(session, "sendBuyList", new Class<?>[0]);
            try {
                receive(client, receiver);
                throw new AssertionError("battle phase emitted native LTshop list");
            } catch (SocketTimeoutException expected) {
                // S_DF is unsafe while the native gameplay receiver is active.
            }

            setPhase(session, "ROOM");
            invoke(session, "sendBuyList", new Class<?>[0]);
            assertEquals(Protocol.S_LITO_BUY_LIST,
                    receive(client, receiver)[0] & 0xFF);

            setPhase(session, "CHANNEL");
            invoke(session, "sendBuyList", new Class<?>[0]);
            assertEquals(Protocol.S_LITO_BUY_LIST,
                    receive(client, receiver)[0] & 0xFF);
            byte[] buy = new Protocol.Writer().c(Protocol.C_LITO_BUY_SELL)
                    .c(0).h(1).d(InventoryCatalog.NATIVE_HEALING_POTION).d(1)
                    .raw(new byte[8]).bytes();
            invoke(session, "buySell", new Class<?>[] {byte[].class}, buy);
            assertEquals(Protocol.S_ADD_INVENTORY,receive(client,receiver)[0]&255);
            assertEquals(Protocol.S_LITO_POINT,
                    receive(client, receiver)[0] & 0xFF);
            assertTrue(shopNotice(client, receiver).contains("購買成功"));
            long acceptedPoints = (long) field(session, "shopPoints");

            invoke(session, "buySell", new Class<?>[] {byte[].class}, buy);
            assertEquals(acceptedPoints, (long) field(session, "shopPoints"));
            assertEquals(Protocol.S_LITO_POINT, receive(client, receiver)[0] & 0xFF);
            assertTrue(shopNotice(client, receiver).contains("交易已失效"));
        }
    }

    @Test
    void matchmakingConsumablePredicateExcludesSourceAndTournamentRewardProvenance()
            throws Exception {
        Fixture fixture = fixture();
        InventoryCatalog.Definition potion = InventoryCatalog.require(
                InventoryCatalog.NATIVE_HEALING_POTION);

        assertFalse((boolean) invokeResult(fixture.session,
                "hasNativeTournamentMatchConsumable", new Class<?>[0]));

        RuntimeInventory inventory = fixture.baseline.copy();
        assertTrue(inventory.addDistinctState(potion,
                new RuntimeInventory.PersistentItem(
                        0x7000_3811L, 1, 0, 0,
                        potion.identified(), potion.blessing(),
                        true, false, false, null,
                        potion.initialChargeCount())) != 0);
        assertTrue(inventory.addTournamentReward(potion, 1,
                () -> 0x7000_3812L));
        setField(fixture.session, "inventory", inventory);
        assertFalse((boolean) invokeResult(fixture.session,
                "hasNativeTournamentMatchConsumable", new Class<?>[0]));

        assertTrue(inventory.add(potion, 1, () -> 0x7000_3813L));
        assertTrue((boolean) invokeResult(fixture.session,
                "hasNativeTournamentMatchConsumable", new Class<?>[0]));
        RuntimeInventory.Item bought = inventory.items().stream()
                .filter(item -> item.definition().catalogId() == potion.catalogId())
                .filter(item -> !item.sourceBound() && !item.tournamentReward())
                .findFirst().orElseThrow();
        assertTrue(inventory.remove(bought.objectId(), bought.quantity()));
        assertFalse((boolean) invokeResult(fixture.session,
                "hasNativeTournamentMatchConsumable", new Class<?>[0]));
    }

    @Test
    void encryptedMatchmakingWorksBeforeAndAfterOptionalShopPurchases()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("matchmaking-consumable-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve(
                            "matchmaking-consumable-capture")), hub);
            initialize(session, baseline());
            setField(session, "characters", List.of(character(
                    "NativeEconomyKnight", 0, 0x3810_0453L)));
            setField(session, "currentChannelId", 0);
            setPhase(session, "CHANNEL");
            hub.connect(session);
            assertNotNull(hub.enterChannel(0, session));

            int seed = 0x3810_0453;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            byte[] honorMatch = new Protocol.Writer()
                    .c(Protocol.C_LITO_ASK_MATCH_MAKING)
                    .c(8).c(Protocol.MatchMakingRequest.NO_CHANNEL_RESTRICTION)
                    .c(5).c(0).bytes();

            invoke(session, "askMatchMaking",
                    new Class<?>[] {byte[].class}, honorMatch);
            byte[] acceptedBeforePurchase = receive(client, receiver);
            assertEquals(Protocol.S_LITO_ASK_MATCH_MAKING_RESULT,
                    acceptedBeforePurchase[0] & 0xFF);
            assertEquals(Protocol.LitoMatchMakingResult.ACCEPTED.wireValue(),
                    acceptedBeforePurchase[1] & 0xFF);
            assertNotNull(field(session, "pendingMatchMakingRequest"));
            assertEquals(1, hub.queuedMatchMakingCount());
            hub.cancelMatchMaking(session);
            setField(session, "pendingMatchMakingRequest", null);

            byte[] buy = new Protocol.Writer().c(Protocol.C_LITO_BUY_SELL)
                    .c(0).h(1).d(InventoryCatalog.NATIVE_HEALING_POTION).d(1)
                    .raw(new byte[8]).bytes();
            invoke(session, "sendBuyList", new Class<?>[0]);
            assertEquals(Protocol.S_LITO_BUY_LIST,
                    receive(client, receiver)[0] & 0xFF);
            invoke(session, "buySell", new Class<?>[] {byte[].class}, buy);
            assertEquals(Protocol.S_ADD_INVENTORY,receive(client,receiver)[0]&255);
            assertEquals(Protocol.S_LITO_POINT,
                    receive(client, receiver)[0] & 0xFF);

            assertTrue(shopNotice(client, receiver).contains("成功"));

            invoke(session, "askMatchMaking",
                    new Class<?>[] {byte[].class}, honorMatch);
            byte[] accepted = receive(client, receiver);
            assertEquals(Protocol.S_LITO_ASK_MATCH_MAKING_RESULT,
                    accepted[0] & 0xFF);
            assertEquals(Protocol.LitoMatchMakingResult.ACCEPTED.wireValue(),
                    accepted[1] & 0xFF);
            assertNotNull(field(session, "pendingMatchMakingRequest"));
            assertEquals(1, hub.queuedMatchMakingCount());

            hub.cancelMatchMaking(session);
            setField(session, "pendingMatchMakingRequest", null);
            RuntimeInventory.Item bought = inventory(session).items().stream()
                    .filter(item -> item.definition().catalogId()
                            == InventoryCatalog.NATIVE_HEALING_POTION)
                    .filter(item -> !item.sourceBound() && !item.tournamentReward())
                    .findFirst().orElseThrow();
            byte[] sell = new Protocol.Writer().c(Protocol.C_LITO_BUY_SELL)
                    .c(1).h(1).d(bought.objectId()).d(1)
                    .raw(new byte[8]).bytes();
            invoke(session, "sendSellList", new Class<?>[0]);
            assertEquals(Protocol.S_LITO_SELL_LIST,
                    receive(client, receiver)[0] & 0xFF);
            invoke(session, "buySell", new Class<?>[] {byte[].class}, sell);
            assertEquals(Protocol.S_REMOVE_INVENTORY,receive(client,receiver)[0]&255);
            assertEquals(Protocol.S_LITO_POINT,
                    receive(client, receiver)[0] & 0xFF);

            assertTrue(shopNotice(client, receiver).contains("成功"));

            invoke(session, "askMatchMaking",
                    new Class<?>[] {byte[].class}, honorMatch);
            byte[] acceptedAfterSale = receive(client, receiver);
            assertEquals(Protocol.S_LITO_ASK_MATCH_MAKING_RESULT,
                    acceptedAfterSale[0] & 0xFF);
            assertEquals(Protocol.LitoMatchMakingResult.ACCEPTED.wireValue(),
                    acceptedAfterSale[1] & 0xFF);
            assertNotNull(field(session, "pendingMatchMakingRequest"));
            assertEquals(1, hub.queuedMatchMakingCount());
        }
    }

    @Test
    void encryptedChoiceUseRepairsOneDurabilityAndConsumesOneWhetstone()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("whetstone-wire-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("whetstone-wire-capture")), hub);
            RuntimeInventory live = baseline();
            assertTrue(live.add(InventoryCatalog.require(InventoryCatalog.WHETSTONE), 2));
            long swordId = live.items().stream()
                    .filter(item -> item.definition().weapon())
                    .findFirst().orElseThrow().objectId();
            long stoneId = live.items().stream()
                    .filter(item -> item.definition().catalogId() == InventoryCatalog.WHETSTONE)
                    .findFirst().orElseThrow().objectId();
            assertTrue(live.damageWeapon(swordId, 3));
            initialize(session, live);
            setPhase(session, "BATTLE");
            // Keep this transport test focused on the three item frames.
            setField(session, "sourceInventoryWeightStats", null);
            int seed = 0x3810_0368;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            byte[] use = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                    .d(stoneId).d(swordId).bytes();
            invoke(session, "useInventoryItem", new Class<?>[] {byte[].class}, use);

            assertEquals(Protocol.S_CHANGE_COUNT, receive(client, receiver)[0] & 0xFF);
            var quantityName = new Protocol.Reader(receive(client, receiver));
            assertEquals(0x18, quantityName.c());
            assertEquals(stoneId, quantityName.d());
            assertEquals(inventory(session).item(stoneId).nativeViewName(), quantityName.s());
            byte[] message = receive(client, receiver);
            assertEquals(Protocol.S_MESSAGE_CODE, message[0] & 0xFF);
            assertEquals(463, unsignedShort(message, 1));
            assertEquals(Protocol.S_CHANGE_ITEM_DESC_EX,
                    receive(client, receiver)[0] & 0xFF);
            assertEquals(2, inventory(session).item(swordId).durability());
            assertEquals(1, inventory(session).item(stoneId).quantity());
        }
    }

    @Test
    void encryptedWisdomPotionUsePublishesSpTwoAndStartsRefreshableLease()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("wisdom-wire-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("wisdom-wire-capture")), hub);
            RuntimeInventory live = baseline();
            assertTrue(live.add(InventoryCatalog.require(
                    InventoryCatalog.WISDOM_POTION), 1));
            long potionId = live.items().stream()
                    .filter(item -> item.definition().catalogId()
                            == InventoryCatalog.WISDOM_POTION)
                    .findFirst().orElseThrow().objectId();
            initialize(session, live);
            setField(session, "characters", List.of(character(
                    "NativeEconomyKnight", 3, 0x3810_0001L)));
            setPhase(session, "BATTLE");
            setField(session, "sourceInventoryWeightStats", null);
            int seed = 0x3810_1368;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            byte[] use = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                    .d(potionId).s("").bytes();
            invoke(session, "useInventoryItem", new Class<?>[] {byte[].class}, use);

            assertEquals(Protocol.S_REMOVE_INVENTORY,
                    receive(client, receiver)[0] & 0xFF);
            byte[] magic = receive(client, receiver);
            assertEquals(Protocol.S_MAGIC_STATUS, magic[0] & 0xFF);
            assertEquals(InventoryCatalog.WISDOM_POTION_SPELL_POWER,
                    magic[1] & 0xFF);
            assertEquals(0, magic[2] & 0xFF);
            assertTrue(((BattleTimedStatus) field(session, "timedStatus"))
                    .wisdomPotionActive(System.nanoTime()));
            assertEquals(null, inventory(session).item(potionId));
        }
    }

    @Test
    void battleChillTouchDamagesTheNativeBotAndHealsOnlyAfterAcceptance()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("chill-touch-battle-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("chill-touch-battle-capture")),
                    hub);
            initialize(session, baseline());
            setField(session, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.CHILL_TOUCH));
            setPhase(session, "BATTLE");
            long roomId = startSoloBattle(hub, session);
            setField(session, "currentRoomId", roomId);
            ArenaMapCatalog.Point bot = hub.botPosition(roomId);
            setField(session, "positionX", bot.x() - 1);
            setField(session, "positionY", bot.y());
            setField(session, "playerMaxHp", 100);
            setField(session, "playerHp", 50);
            setField(session, "playerMaxMp", 100);
            setField(session, "playerMp", 100);
            int botHpBefore = hub.botHp(roomId);

            int seed = 0x3810_400A;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            byte[] nativeRequest = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(1).c(1)
                    .d(2).h(bot.x()).h(bot.y()).bytes();
            invoke(session, "useSpell", new Class<?>[] {byte[].class},
                    nativeRequest);

            byte[] attack = receiveUntilOpcode(
                    client, receiver, Protocol.S_ATTACK, 8);
            assertEquals(18, attack[1] & 0xFF);
            assertEquals(2, unsignedInt(attack, 6));
            assertEquals(252, unsignedShort(attack, 16));
            byte[] mana = receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8);
            assertTrue(unsignedShort(mana, 1) < 100);
            byte[] hp = receiveUntilOpcode(
                    client, receiver, Protocol.S_HIT_POINT, 8);
            int healedHp = unsignedShort(hp, 1);
            assertTrue(healedHp > 50 && healedHp <= 100);
            assertEquals(healedHp, field(session, "playerHp"));
            assertTrue(hub.botHp(roomId) < botHpBefore);
        } finally {
            hub.close();
        }
    }

    @Test
    void encryptedLightningWandConsumesSixChargesThenReturnsNativeEmptyMessage()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("lightning-wand-wire-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("lightning-wand-wire-capture")), hub);
            RuntimeInventory live = baseline();
            assertTrue(live.add(InventoryCatalog.require(
                    InventoryCatalog.LIGHTNING_WAND), 1));
            long wandId = live.items().stream()
                    .filter(item -> item.definition().catalogId()
                            == InventoryCatalog.LIGHTNING_WAND)
                    .findFirst().orElseThrow().objectId();
            initialize(session, live);
            setPhase(session, "BATTLE");
            long roomId = startSoloBattle(hub, session);
            setField(session, "currentRoomId", roomId);
            ArenaMapCatalog.Point bot = hub.botPosition(roomId);
            setField(session, "positionX", bot.x() - 1);
            setField(session, "positionY", bot.y());
            int seed = 0x3810_4007;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            byte[] botUse = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                    .d(wandId).d(2).h(bot.x()).h(bot.y()).bytes();
            invoke(session, "useInventoryItem", new Class<?>[] {byte[].class}, botUse);
            assertEquals(Protocol.S_CHANGE_DIRECTION, receive(client, receiver)[0] & 0xFF);
            byte[] wandAction = receive(client, receiver);
            assertEquals(Protocol.S_ACTION, wandAction[0] & 0xFF);
            assertEquals(BattleActionCodes.WAND, wandAction[5] & 0xFF);
            byte[] effect = receive(client, receiver);
            assertEquals(Protocol.S_EFFECT, effect[0] & 0xFF);
            assertEquals(10, unsignedShort(effect, 5));
            byte[] damageAction = receive(client, receiver);
            assertEquals(Protocol.S_ACTION, damageAction[0] & 0xFF);
            assertEquals(BattleActionCodes.DAMAGE, damageAction[5] & 0xFF);
            assertEquals(Protocol.S_HIT_RATIO, receive(client, receiver)[0] & 0xFF);
            byte[] firstCharge = receive(client, receiver);
            assertEquals(Protocol.S_CHANGE_ITEM_USE, firstCharge[0] & 0xFF);
            assertEquals(5, firstCharge[5] & 0xFF);
            assertEquals(Protocol.S_CHANGE_ITEM_DESC_EX,
                    receive(client, receiver)[0] & 0xFF);

            // The remaining callbacks deliberately use an unresolved object;
            // retained semantics still consume one charge after S_EFFECT_LOC.
            byte[] use = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                    .d(wandId).d(0x1122_3344L).h(32740).h(32798).bytes();
            for (int expected = 4; expected >= 0; expected--) {
                invoke(session, "useInventoryItem",
                        new Class<?>[] {byte[].class}, use);
                byte[] locationEffect = receive(client, receiver);
                assertEquals(Protocol.S_EFFECT_LOC, locationEffect[0] & 0xFF);
                assertEquals(32740, unsignedShort(locationEffect, 1));
                assertEquals(32798, unsignedShort(locationEffect, 3));
                assertEquals(10, unsignedShort(locationEffect, 5));
                byte[] charge = receive(client, receiver);
                assertEquals(Protocol.S_CHANGE_ITEM_USE, charge[0] & 0xFF);
                assertEquals(wandId, unsignedInt(charge, 1));
                assertEquals(expected, charge[5] & 0xFF);
                assertEquals(Protocol.S_CHANGE_ITEM_DESC_EX,
                        receive(client, receiver)[0] & 0xFF);
                assertEquals(expected,
                        inventory(session).item(wandId).chargeCount());
            }

            invoke(session, "useInventoryItem", new Class<?>[] {byte[].class}, use);
            byte[] empty = receive(client, receiver);
            assertEquals(Protocol.S_MESSAGE_CODE, empty[0] & 0xFF);
            assertEquals(79, unsignedShort(empty, 1));
            assertEquals(0, inventory(session).item(wandId).chargeCount());
        }
    }

    @Test
    void encryptedHealingPotionsPublishNativeGfxAndConsumeAtFullHp()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("healing-potion-wire-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("healing-potion-wire-capture")), hub);
            RuntimeInventory live = baseline();
            long[] catalogIds = {
                    InventoryCatalog.NATIVE_HEALING_POTION,
                    InventoryCatalog.EXTRA_HEALING_POTION,
                    InventoryCatalog.GREATER_HEALING_POTION
            };
            for (long catalogId : catalogIds) {
                assertTrue(live.add(InventoryCatalog.require(catalogId), 1));
            }
            initialize(session, live);
            setField(session, "characters", List.of(character(
                    "NativeEconomyKnight", 1, 0x3810_0010L)));
            setPhase(session, "BATTLE");
            long roomId = startSoloBattle(hub, session);
            setField(session, "currentRoomId", roomId);
            SourceEquipmentVitalStats potionRecoveryVital =
                    new SourceEquipmentVitalStats(0, 0, 0, 0, 0, 0, 4, 3);
            setField(session, "sourceEquipmentVitalStats", potionRecoveryVital);
            setField(session, "sourceInventoryWeightStats", null);
            setField(session, "playerMaxHp", 100);
            int seed = 0x3810_1370;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            int[] gfx = {189, 194, 197};
            int[][] ranges = {{10, 20}, {30, 50}, {60, 80}};

            for (int index = 0; index < catalogIds.length; index++) {
                long catalogId = catalogIds[index];
                RuntimeInventory.Item potion = inventory(session).items().stream()
                        .filter(item -> item.definition().catalogId() == catalogId)
                        .findFirst().orElseThrow();
                // The final potion deliberately starts at full HP. Retained
                // UserAddHp still consumes it and publishes the drink effect.
                int beforeHp = index == catalogIds.length - 1 ? 100 : 1;
                setField(session, "playerHp", beforeHp);
                byte[] use = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                        .d(potion.objectId()).s("").bytes();
                invoke(session, "useInventoryItem", new Class<?>[] {byte[].class}, use);

                assertEquals(Protocol.S_REMOVE_INVENTORY,
                        receive(client, receiver)[0] & 0xFF);
                byte[] effect = receive(client, receiver);
                assertEquals(Protocol.S_EFFECT, effect[0] & 0xFF);
                assertEquals(gfx[index], unsignedShort(effect, 5));
                byte[] hp = receive(client, receiver);
                assertEquals(Protocol.S_HIT_POINT, hp[0] & 0xFF);
                int resultingHp = unsignedShort(hp, 1);
                if (beforeHp < 100) {
                    assertTrue(resultingHp >= beforeHp + ranges[index][0] + 7);
                    assertTrue(resultingHp <= beforeHp + ranges[index][1] + 7);
                } else {
                    assertEquals(100, resultingHp);
                }
                // The local HUD is driven by S_HIT_POINT. S_HIT_RATIO is
                // observer-only so the player's own overhead name is not
                // permanently rendered.
                assertEquals(null, inventory(session).item(potion.objectId()));
            }
        }
    }

    @Test
    void encryptedWaterDragonPotionErosionPublishesEffectAndFatalDamageFlow()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("water-dragon-potion-wire-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve(
                            "water-dragon-potion-wire-capture")), hub);
            RuntimeInventory live = baseline();
            assertTrue(live.add(InventoryCatalog.require(
                    InventoryCatalog.NATIVE_HEALING_POTION), 1));
            initialize(session, live);
            CharacterSummary character = character(
                    "WaterDragonTarget", 1, 0x3810_4011L);
            setField(session, "account", "water-dragon-target-account");
            setField(session, "selectedCharacter", character.name());
            setField(session, "characters", List.of(character));
            setField(session, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(character));
            setField(session, "sourceInventoryWeightStats", null);
            setPhase(session, "BATTLE");
            long roomId = startSoloBattle(hub, session);
            setField(session, "currentRoomId", roomId);
            setField(session, "playerMaxHp", 100);
            setField(session, "playerHp", 5);
            int seed = 0x3810_4011;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            BattleTimedStatus status =
                    (BattleTimedStatus) field(session, "timedStatus");
            assertTrue(session.applyWaterDragonAffliction(
                    BattleWaterDragonAffliction.Kind.POTION_EROSION,
                    System.nanoTime()));
            byte[] affliction = receive(client, receiver);
            assertEquals(Protocol.S_EFFECT, affliction[0] & 0xFF);
            assertEquals(7781, unsignedShort(affliction, 5));
            assertTrue(status.waterDragonPotionErosionActive(System.nanoTime()));

            RuntimeInventory.Item potion = inventory(session).items().stream()
                    .filter(item -> item.definition().catalogId()
                            == InventoryCatalog.NATIVE_HEALING_POTION)
                    .findFirst().orElseThrow();
            byte[] use = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                    .d(potion.objectId()).s("").bytes();
            invoke(session, "useInventoryItem",
                    new Class<?>[] {byte[].class}, use);

            assertEquals(Protocol.S_REMOVE_INVENTORY,
                    receive(client, receiver)[0] & 0xFF);
            byte[] potionEffect = receive(client, receiver);
            assertEquals(Protocol.S_EFFECT, potionEffect[0] & 0xFF);
            assertEquals(189, unsignedShort(potionEffect, 5));
            byte[] hp = receive(client, receiver);
            assertEquals(Protocol.S_HIT_POINT, hp[0] & 0xFF);
            assertEquals(0, unsignedShort(hp, 1));
            byte[] death = receive(client, receiver);
            assertEquals(Protocol.S_ACTION, death[0] & 0xFF);
            assertEquals(BattleActionCodes.DIE, death[5] & 0xFF);
            assertEquals(Protocol.S_LITO_CONFIRM_DEATH,
                    receive(client, receiver)[0] & 0xFF);
            assertEquals(0, (int) field(session, "playerHp"));
            assertNull(status.waterDragonAffliction(System.nanoTime()));
            assertNull(inventory(session).item(potion.objectId()));
        }
    }

    @Test
    void encryptedWaterDragonHealingErosionTurnsHealingIntoFatalDamage()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("water-dragon-healing-wire-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 2, InetAddress.getByName("127.0.0.1"));
             Socket casterClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket casterServer = listener.accept();
             Socket targetClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket targetServer = listener.accept()) {
            casterClient.setSoTimeout(1_000);
            targetClient.setSoTimeout(1_000);
            ClientSession caster = new ClientSession(
                    casterServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve(
                            "water-dragon-healing-caster-capture")), hub);
            ClientSession target = new ClientSession(
                    targetServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve(
                            "water-dragon-healing-target-capture")), hub);
            initialize(caster, baseline());
            initialize(target, baseline());
            CharacterSummary casterCharacter = character(
                    "WaterDragonHealer", 1, 0x3810_4013L);
            CharacterSummary targetCharacter = character(
                    "WaterDragonHealingTarget", 1, 0x3810_4014L);
            setField(caster, "account", "water-dragon-healer-account");
            setField(caster, "selectedCharacter", casterCharacter.name());
            setField(caster, "characters", List.of(casterCharacter));
            setField(caster, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(casterCharacter));
            setField(target, "account", "water-dragon-healing-target-account");
            setField(target, "selectedCharacter", targetCharacter.name());
            setField(target, "characters", List.of(targetCharacter));
            setField(target, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(targetCharacter));
            hub.connect(caster);
            hub.connect(target);
            hub.enterChannel(0, caster);
            hub.enterChannel(0, target);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Water Dragon Healing", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, caster);
            hub.joinRoom(roomId, target);
            hub.setForce(roomId, caster, 0);
            hub.setForce(roomId, target, 1);
            hub.setReady(roomId, caster, true);
            hub.setReady(roomId, target, true);
            Method countdown = GameHub.class.getDeclaredMethod(
                    "countdownWork", long.class);
            countdown.setAccessible(true);
            long future = System.nanoTime() + 10_000_000_000L;
            countdown.invoke(hub, future);
            countdown.invoke(hub, future + 2_000_000_000L);
            countdown.invoke(hub, future + 4_000_000_000L);

            setPhase(caster, "BATTLE");
            setPhase(target, "BATTLE");
            setField(caster, "currentRoomId", roomId);
            setField(target, "currentRoomId", roomId);
            setField(caster, "positionX", 32740);
            setField(caster, "positionY", 32798);
            setField(target, "positionX", 32741);
            setField(target, "positionY", 32798);
            setField(caster, "playerMp", 100);
            setField(caster, "playerMaxMp", 100);
            setField(target, "playerHp", 1);
            setField(target, "playerMaxHp", 100);
            int casterSeed = 0x3810_4013;
            int targetSeed = 0x3810_4014;
            setField(caster, "outgoing", new Protocol.CipherState(casterSeed));
            setField(target, "outgoing", new Protocol.CipherState(targetSeed));
            Protocol.CipherState casterReceiver = new Protocol.CipherState(casterSeed);
            Protocol.CipherState targetReceiver = new Protocol.CipherState(targetSeed);

            BattleTimedStatus targetStatus =
                    (BattleTimedStatus) field(target, "timedStatus");
            assertTrue(target.applyWaterDragonAffliction(
                    BattleWaterDragonAffliction.Kind.HEALING_EROSION,
                    System.nanoTime()));
            assertEquals(7780, unsignedShort(receive(casterClient, casterReceiver), 5));
            assertEquals(7780, unsignedShort(receive(targetClient, targetReceiver), 5));

            BattleSpellCatalog.HealingSpell heal = BattleSpellCatalog.healingSpell(
                    BattleSpellCatalog.BASIC_HEAL);
            byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(heal.row()).c(heal.column()).d(target.playerObjectId()).bytes();
            invoke(caster, "useHealingSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.HealingSpell.class},
                    request, heal);

            assertEquals(Protocol.S_ACTION,
                    receive(casterClient, casterReceiver)[0] & 0xFF);
            assertEquals(Protocol.S_EFFECT,
                    receive(casterClient, casterReceiver)[0] & 0xFF);
            assertEquals(Protocol.S_MANA_POINT,
                    receive(casterClient, casterReceiver)[0] & 0xFF);
            assertEquals(Protocol.S_HIT_RATIO,
                    receive(casterClient, casterReceiver)[0] & 0xFF);
            assertEquals(Protocol.S_ACTION,
                    receive(casterClient, casterReceiver)[0] & 0xFF);

            assertEquals(Protocol.S_ACTION,
                    receive(targetClient, targetReceiver)[0] & 0xFF);
            byte[] healingEffect = receive(targetClient, targetReceiver);
            assertEquals(Protocol.S_EFFECT, healingEffect[0] & 0xFF);
            assertEquals(11722, unsignedShort(healingEffect, 5));
            byte[] hp = receive(targetClient, targetReceiver);
            assertEquals(Protocol.S_HIT_POINT, hp[0] & 0xFF);
            assertEquals(0, unsignedShort(hp, 1));
            byte[] death = receive(targetClient, targetReceiver);
            assertEquals(Protocol.S_ACTION, death[0] & 0xFF);
            assertEquals(BattleActionCodes.DIE, death[5] & 0xFF);
            assertEquals(Protocol.S_LITO_CONFIRM_DEATH,
                    receive(targetClient, targetReceiver)[0] & 0xFF);
            assertEquals(Protocol.S_MESSAGE_CODE,
                    receive(targetClient, targetReceiver)[0] & 0xFF);
            assertEquals(0, (int) field(target, "playerHp"));
            assertNull(targetStatus.waterDragonAffliction(System.nanoTime()));
        }
    }

    @Test
    void nativePolluteWaterBypassesCounterMagicAndHalvesEncryptedPotionRecovery()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("pollute-water-wire-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 2, InetAddress.getByName("127.0.0.1"));
             Socket casterClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket casterServer = listener.accept();
             Socket targetClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket targetServer = listener.accept()) {
            casterClient.setSoTimeout(1_000);
            targetClient.setSoTimeout(1_000);
            ClientSession caster = new ClientSession(
                    casterServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("pollute-water-caster-capture")), hub);
            ClientSession target = new ClientSession(
                    targetServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("pollute-water-target-capture")), hub);

            RuntimeInventory casterInventory = baseline();
            assertTrue(casterInventory.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 1));
            RuntimeInventory targetInventory = baseline();
            assertTrue(targetInventory.add(InventoryCatalog.require(
                    InventoryCatalog.NATIVE_HEALING_POTION), 1));
            initialize(caster, casterInventory);
            initialize(target, targetInventory);
            setField(caster, "account", "pollute-caster-account");
            setField(caster, "selectedCharacter", "PolluteCaster");
            CharacterSummary casterCharacter = character(
                    "PolluteCaster", 2, 0x3810_1730L);
            setField(caster, "characters", List.of(casterCharacter));
            setField(caster, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(casterCharacter));
            setField(caster, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.POLLUTE_WATER));
            setField(caster, "elfAttribute",
                    BattleSpellCatalog.ATTRIBUTE_WATER);
            setField(target, "account", "pollute-target-account");
            setField(target, "selectedCharacter", "PolluteTarget");
            CharacterSummary targetCharacter = character(
                    "PolluteTarget", 1, 0x3810_1731L);
            setField(target, "characters", List.of(targetCharacter));
            setField(target, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(targetCharacter));
            setField(caster, "sourceCounterMagicBypassSkills",
                    java.util.Set.of(BattleSpellCatalog.POLLUTE_WATER));

            hub.connect(caster);
            hub.connect(target);
            hub.enterChannel(0, caster);
            hub.enterChannel(0, target);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Pollute Water", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, caster);
            hub.joinRoom(roomId, target);
            hub.setForce(roomId, caster, 0);
            hub.setForce(roomId, target, 1);
            hub.setReady(roomId, caster, true);
            hub.setReady(roomId, target, true);
            Method countdown = GameHub.class.getDeclaredMethod(
                    "countdownWork", long.class);
            countdown.setAccessible(true);
            long future = System.nanoTime() + 10_000_000_000L;
            countdown.invoke(hub, future);
            countdown.invoke(hub, future + 2_000_000_000L);
            countdown.invoke(hub, future + 4_000_000_000L);

            setPhase(caster, "BATTLE");
            setPhase(target, "BATTLE");
            setField(caster, "currentRoomId", roomId);
            setField(target, "currentRoomId", roomId);
            setField(caster, "positionX", 32740);
            setField(caster, "positionY", 32798);
            setField(target, "positionX", 32741);
            setField(target, "positionY", 32798);
            setField(caster, "playerMp", 100);
            setField(caster, "playerMaxMp", 100);
            setField(target, "playerHp", 100);
            setField(target, "playerMaxHp", 100);

            int casterSeed = 0x3810_1730;
            int targetSeed = 0x3810_1731;
            setField(caster, "outgoing", new Protocol.CipherState(casterSeed));
            setField(target, "outgoing", new Protocol.CipherState(targetSeed));
            Protocol.CipherState casterReceiver = new Protocol.CipherState(casterSeed);
            Protocol.CipherState targetReceiver = new Protocol.CipherState(targetSeed);

            BattleTimedStatus targetStatus =
                    (BattleTimedStatus) field(target, "timedStatus");
            targetStatus.activateCounterMagic(System.nanoTime(), 16);
            byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(21).c(4).d(target.playerObjectId())
                    .h(32741).h(32798).bytes();
            Protocol.AttackSpellRequest parsed =
                    Protocol.attackSpellRequest(nativeRequest);
            assertEquals(BattleSpellCatalog.POLLUTE_WATER, parsed.skillId());
            assertEquals(target.playerObjectId(), parsed.targetObjectId());

            invoke(caster, "usePolluteWaterSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.PolluteWaterSpell.class, int.class},
                    nativeRequest, BattleSpellCatalog.polluteWaterSpell(
                            BattleSpellCatalog.POLLUTE_WATER), 1);

            assertEquals(Protocol.S_REMOVE_INVENTORY,
                    receive(casterClient, casterReceiver)[0] & 0xFF);
            byte[] casterAction = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_ACTION, 4);
            assertEquals(Protocol.S_ACTION, casterAction[0] & 0xFF);
            assertEquals(19, casterAction[5] & 0xFF);
            byte[] casterEffect = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 4);
            assertEquals(Protocol.S_EFFECT, casterEffect[0] & 0xFF);
            assertEquals(5830, unsignedShort(casterEffect, 5));
            byte[] mana = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 4);
            assertEquals(Protocol.S_MANA_POINT, mana[0] & 0xFF);
            assertEquals(50, unsignedShort(mana, 1));

            byte[] targetAction = receive(targetClient, targetReceiver);
            assertEquals(Protocol.S_ACTION, targetAction[0] & 0xFF);
            assertEquals(19, targetAction[5] & 0xFF);
            byte[] targetEffect = receive(targetClient, targetReceiver);
            assertEquals(Protocol.S_EFFECT, targetEffect[0] & 0xFF);
            assertEquals(5830, unsignedShort(targetEffect, 5));
            assertTrue(targetStatus.polluteWaterActive(System.nanoTime()));
            assertTrue(targetStatus.counterMagicActive(System.nanoTime()));

            RuntimeInventory.Item potion = inventory(target).items().stream()
                    .filter(item -> item.definition().catalogId()
                            == InventoryCatalog.NATIVE_HEALING_POTION)
                    .findFirst().orElseThrow();
            setField(target, "playerHp", 1);
            byte[] usePotion = new Protocol.Writer().c(Protocol.C_USE_ITEM)
                    .d(potion.objectId()).s("").bytes();
            invoke(target, "useInventoryItem", new Class<?>[] {byte[].class}, usePotion);

            assertEquals(Protocol.S_REMOVE_INVENTORY,
                    receiveUntilOpcode(targetClient, targetReceiver,
                            Protocol.S_REMOVE_INVENTORY, 12)[0] & 0xFF);
            byte[] potionEffect = receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_EFFECT, 12);
            assertEquals(Protocol.S_EFFECT, potionEffect[0] & 0xFF);
            assertEquals(189, unsignedShort(potionEffect, 5));
            byte[] hp = receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_HIT_POINT, 12);
            assertEquals(Protocol.S_HIT_POINT, hp[0] & 0xFF);
            int resultingHp = unsignedShort(hp, 1);
            assertTrue(resultingHp >= 6 && resultingHp <= 11,
                    "Pollute Water must halve native 10..20 recovery after damage-order shift");
            assertEquals(null, inventory(target).item(potion.objectId()));

            // 380yiwei_db exposes only skill 4 in skills_魔屏無效技能.
            // Re-run the same native request under that source profile: its
            // Counter Magic must be consumed and Pollute Water must not land.
            assertTrue(targetStatus.cancelMagicEffects().polluteWater());
            RuntimeInventory refilledCaster = inventory(caster).copy();
            assertTrue(refilledCaster.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 1));
            setField(caster, "inventory", refilledCaster);
            setField(caster, "playerMp", 100);
            ((Map<?, ?>) field(caster, "spellReadyNanos")).clear();
            setField(caster, "sourceCounterMagicBypassSkills", java.util.Set.of());
            targetStatus.activateCounterMagic(System.nanoTime(), 16);
            invoke(caster, "usePolluteWaterSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.PolluteWaterSpell.class, int.class},
                    nativeRequest, BattleSpellCatalog.polluteWaterSpell(
                            BattleSpellCatalog.POLLUTE_WATER), 1);
            assertFalse(targetStatus.polluteWaterActive(System.nanoTime()));
            assertFalse(targetStatus.counterMagicActive(System.nanoTime()));
        }
    }

    @Test
    void nativePolluteWaterUsesCase172InWorldWithWaterGateAndPrimaryBypass()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        Path stateFile = temporary.resolve("pollute-water-world-state.properties");
        GameStateStore state = new GameStateStore(stateFile, true);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 2, InetAddress.getByName("127.0.0.1"));
             Socket casterClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket casterServer = listener.accept();
             Socket targetClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket targetServer = listener.accept()) {
            casterClient.setSoTimeout(1_000);
            targetClient.setSoTimeout(1_000);
            ClientSession caster = new ClientSession(
                    casterServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve(
                            "pollute-water-world-caster-capture")), hub);
            ClientSession target = new ClientSession(
                    targetServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve(
                            "pollute-water-world-target-capture")), hub);
            RuntimeInventory live = baseline();
            assertTrue(live.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 1));
            initialize(caster, live);
            initialize(target, baseline());
            CharacterSummary casterElf = character(
                    "PolluteWorldCaster", 2, 0x3810_1732L);
            CharacterSummary targetElf = character(
                    "PolluteWorldTarget", 2, 0x3810_1733L);
            setField(caster, "account", "pollute-world-caster-account");
            setField(caster, "selectedCharacter", casterElf.name());
            setField(caster, "inventoryCharacter", casterElf.name());
            setField(caster, "characters", List.of(casterElf));
            setField(caster, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(casterElf));
            setField(caster, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.POLLUTE_WATER));
            setField(caster, "sourceCounterMagicBypassSkills",
                    java.util.Set.of(BattleSpellCatalog.POLLUTE_WATER));
            setField(caster, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_FIRE);
            setField(target, "account", "pollute-world-target-account");
            setField(target, "selectedCharacter", targetElf.name());
            setField(target, "inventoryCharacter", targetElf.name());
            setField(target, "characters", List.of(targetElf));
            setField(target, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(targetElf));
            setField(caster, "playerHp", 100);
            setField(caster, "playerMaxHp", 100);
            setField(caster, "playerMp", 100);
            setField(caster, "playerMaxMp", 100);
            setField(target, "playerHp", 100);
            setField(target, "playerMaxHp", 100);

            int casterSeed = 0x3810_1732;
            int targetSeed = 0x3810_1733;
            setField(caster, "outgoing", new Protocol.CipherState(casterSeed));
            setField(target, "outgoing", new Protocol.CipherState(targetSeed));
            Protocol.CipherState casterReceiver = new Protocol.CipherState(casterSeed);
            Protocol.CipherState targetReceiver = new Protocol.CipherState(targetSeed);

            hub.connect(caster);
            hub.connect(target);
            int worldMap = 4;
            int casterX = 33313;
            int targetX = 33312;
            int worldY = 32339;
            assertFalse(ClassicWorldMapCatalog.forMap(worldMap)
                    .isSafetyZone(casterX, worldY));
            assertFalse(ClassicWorldMapCatalog.forMap(worldMap)
                    .isSafetyZone(targetX, worldY));
            hub.enterClassicWorld(caster, worldMap, casterX, worldY, 5);
            hub.enterClassicWorld(target, worldMap, targetX, worldY, 5);
            setPhase(caster, "WORLD");
            setPhase(target, "WORLD");
            setField(caster, "classicWorldMapId", worldMap);
            setField(target, "classicWorldMapId", worldMap);
            setField(caster, "classicWorldInstanceId", -1);
            setField(target, "classicWorldInstanceId", -1);
            setField(caster, "positionX", casterX);
            setField(caster, "positionY", worldY);
            setField(target, "positionX", targetX);
            setField(target, "positionY", worldY);

            for (int index = 0; index < 1_024
                    && casterClient.getInputStream().available() > 0; index++) {
                receive(casterClient, casterReceiver);
            }
            for (int index = 0; index < 1_024
                    && targetClient.getInputStream().available() > 0; index++) {
                receive(targetClient, targetReceiver);
            }
            byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(21).c(4).d(target.playerObjectId())
                    .h(targetX).h(worldY).bytes();
            BattleTimedStatus targetStatus =
                    (BattleTimedStatus) field(target, "timedStatus");
            targetStatus.activateCounterMagic(System.nanoTime(), 64);

            // Retained isAttrAgrees is a silent gate before MP/reagent use.
            casterClient.setSoTimeout(100);
            invoke(caster, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(100, field(caster, "playerMp"));
            assertEquals(1, inventory(caster).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));
            assertFalse(targetStatus.polluteWaterActive(System.nanoTime()));
            try {
                while (true) {
                    byte[] unexpected = receive(casterClient, casterReceiver);
                    // The shared WORLD AI clock can legitimately move or
                    // attack with an NPC after the bootstrap drain.  Those
                    // actor frames are unrelated to the rejected cast; every
                    // spell-side frame remains forbidden.
                    int opcode = unexpected[0] & 0xFF;
                    if (opcode != Protocol.S_MOVE_OBJECT
                            && opcode != Protocol.S_ATTACK) {
                        throw new AssertionError(
                                "non-water Pollute Water unexpectedly emitted a packet: "
                                        + Protocol.hex(unexpected));
                    }
                }
            } catch (SocketTimeoutException expected) {
                // Exact retained attribute failure is silent.
            }

            setField(caster, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_WATER);
            casterClient.setSoTimeout(1_000);
            invoke(caster, "usePolluteWaterSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.PolluteWaterSpell.class, int.class},
                    request, BattleSpellCatalog.polluteWaterSpell(
                            BattleSpellCatalog.POLLUTE_WATER), 1);
            assertEquals(19, receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_ACTION, 128)[5] & 0xFF);
            assertEquals(5830, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 128), 5));
            assertEquals(50, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 128), 1));
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_ACTION, 128);
            assertEquals(5830, unsignedShort(receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_EFFECT, 128), 5));
            assertTrue(targetStatus.polluteWaterActive(System.nanoTime()));
            assertTrue(targetStatus.counterMagicActive(System.nanoTime()));
            assertEquals(0, inventory(caster).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));
        }
        assertFalse(LocalResources.readString(stateFile).contains("polluteWater"));
    }

    @Test
    void nativeStrikerGaleCase173UsesFiveCrystalsWindGateErZeroAndSourceCounterMagic()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("striker-gale-wire-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 2, InetAddress.getByName("127.0.0.1"));
             Socket casterClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket casterServer = listener.accept();
             Socket targetClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket targetServer = listener.accept()) {
            ClientSession caster = new ClientSession(
                    casterServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("striker-gale-caster-capture")), hub);
            ClientSession target = new ClientSession(
                    targetServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("striker-gale-target-capture")), hub);
            casterClient.setSoTimeout(1_000);
            targetClient.setSoTimeout(1_000);

            RuntimeInventory casterInventory = baseline();
            assertTrue(casterInventory.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 5));
            initialize(caster, casterInventory);
            initialize(target, baseline());
            setField(caster, "account", "striker-caster-account");
            setField(caster, "selectedCharacter", "StrikerCaster");
            CharacterSummary casterCharacter = character(
                    "StrikerCaster", 2, 0x3810_1740L);
            setField(caster, "characters", List.of(casterCharacter));
            setField(caster, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(casterCharacter));
            setField(caster, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.STRIKER_GALE));
            setField(caster, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_WIND);
            setField(caster, "sourceCounterMagicBypassSkills",
                    java.util.Set.of(BattleSpellCatalog.STRIKER_GALE));
            setField(target, "account", "striker-target-account");
            setField(target, "selectedCharacter", "StrikerTarget");
            CharacterSummary targetCharacter = character(
                    "StrikerTarget", 1, 0x3810_1741L);
            setField(target, "characters", List.of(targetCharacter));
            setField(target, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(targetCharacter));

            hub.connect(caster);
            hub.connect(target);
            hub.enterChannel(0, caster);
            hub.enterChannel(0, target);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Striker Gale", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, caster);
            hub.joinRoom(roomId, target);
            hub.setForce(roomId, caster, 0);
            hub.setForce(roomId, target, 1);
            hub.setReady(roomId, caster, true);
            hub.setReady(roomId, target, true);
            Method countdown = GameHub.class.getDeclaredMethod(
                    "countdownWork", long.class);
            countdown.setAccessible(true);
            long future = System.nanoTime() + 10_000_000_000L;
            countdown.invoke(hub, future);
            countdown.invoke(hub, future + 2_000_000_000L);
            countdown.invoke(hub, future + 4_000_000_000L);

            setPhase(caster, "BATTLE");
            setPhase(target, "BATTLE");
            setField(caster, "currentRoomId", roomId);
            setField(target, "currentRoomId", roomId);
            setField(caster, "positionX", 32740);
            setField(caster, "positionY", 32798);
            setField(target, "positionX", 32741);
            setField(target, "positionY", 32798);
            setField(caster, "playerMp", 100);
            setField(caster, "playerMaxMp", 100);
            setField(target, "playerHp", 100);
            setField(target, "playerMaxHp", 100);
            setField(caster, "outgoing",
                    new Protocol.CipherState(0x3810_1740));
            setField(target, "outgoing",
                    new Protocol.CipherState(0x3810_1741));
            Protocol.CipherState casterReceiver =
                    new Protocol.CipherState(0x3810_1740);
            Protocol.CipherState targetReceiver =
                    new Protocol.CipherState(0x3810_1741);

            BattleTimedStatus targetStatus =
                    (BattleTimedStatus) field(target, "timedStatus");
            targetStatus.activateCounterMagic(System.nanoTime(), 16);
            byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(21).c(5).d(target.playerObjectId())
                    .h(32741).h(32798).bytes();
            assertEquals(BattleSpellCatalog.STRIKER_GALE,
                    Protocol.attackSpellRequest(nativeRequest).skillId());

            invoke(caster, "useStrikerGaleSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.StrikerGaleSpell.class, int.class},
                    nativeRequest, BattleSpellCatalog.strikerGaleSpell(
                            BattleSpellCatalog.STRIKER_GALE), 1);

            assertEquals(Protocol.S_REMOVE_INVENTORY,
                    receive(casterClient, casterReceiver)[0] & 0xFF);
            byte[] casterAction = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_ACTION, 4);
            assertEquals(19, casterAction[5] & 0xFF);
            byte[] casterEffect = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 4);
            assertEquals(5826, unsignedShort(casterEffect, 5));
            byte[] mana = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 4);
            assertEquals(90, unsignedShort(mana, 1));
            assertEquals(19, receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_ACTION, 4)[5] & 0xFF);
            assertEquals(5826, unsignedShort(receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_EFFECT, 4), 5));
            assertTrue(targetStatus.strikerGaleActive(System.nanoTime()));
            assertTrue(targetStatus.counterMagicActive(System.nanoTime()));
            assertEquals(90, (int) field(caster, "playerMp"));
            assertEquals(0, inventory(caster).catalogCounts()
                    .getOrDefault(InventoryCatalog.SPIRIT_CRYSTAL, 0));
            assertEquals(0, (int) invokeResult(target,
                    "effectiveRangedEvasionRate",
                    new Class<?>[] {long.class}, System.nanoTime()));

            assertTrue(targetStatus.cancelMagicEffects().strikerGale());
            RuntimeInventory refilled = inventory(caster).copy();
            assertTrue(refilled.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 5));
            setField(caster, "inventory", refilled);
            setField(caster, "playerMp", 100);
            ((Map<?, ?>) field(caster, "spellReadyNanos")).clear();
            setField(caster, "sourceCounterMagicBypassSkills", java.util.Set.of());
            targetStatus.activateCounterMagic(System.nanoTime(), 16);
            invoke(caster, "useStrikerGaleSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.StrikerGaleSpell.class, int.class},
                    nativeRequest, BattleSpellCatalog.strikerGaleSpell(
                            BattleSpellCatalog.STRIKER_GALE), 1);
            assertFalse(targetStatus.strikerGaleActive(System.nanoTime()));
            assertFalse(targetStatus.counterMagicActive(System.nanoTime()));
        }
    }

    @Test
    void nativePolluteWaterCase172AppliesToClassicWorldMonster()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("pollute-water-world-monster-state.properties"),
                false);
        GameHub hub = new GameHub(config);
        AccountRepository accounts = new MemoryAccountRepository(
                "pollute-monster-fixture", "pollute-monster-password");
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession caster = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, accounts, state,
                    new CaptureLog(temporary.resolve(
                            "pollute-water-world-monster-capture")), hub);
            RuntimeInventory live = baseline();
            assertTrue(live.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 1));
            initialize(caster, live);
            CharacterSummary casterElf = character(
                    "PolluteMonsterCaster", 2, 0x3810_1734L);
            setField(caster, "account", "pollute-monster-caster-account");
            setField(caster, "selectedCharacter", casterElf.name());
            setField(caster, "inventoryCharacter", casterElf.name());
            setField(caster, "characters", List.of(casterElf));
            setField(caster, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(casterElf));
            setField(caster, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.POLLUTE_WATER, 35));
            setField(caster, "sourceCounterMagicBypassSkills",
                    java.util.Set.of(BattleSpellCatalog.POLLUTE_WATER));
            setField(caster, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_WATER);
            setField(caster, "playerHp", 100);
            setField(caster, "playerMaxHp", 100);
            setField(caster, "playerMp", 100);
            setField(caster, "playerMaxMp", 100);

            int mapId = 4;
            ClassicWorldMapCatalog.ClassicMap map =
                    ClassicWorldMapCatalog.forMap(mapId);
            ClassicWorldMonsterPopulation.Snapshot monster = null;
            int casterX = 0;
            int casterY = 0;
            for (ClassicWorldMonsterPopulation.Snapshot candidate
                    : hub.classicWorldMonstersInScreen(
                            mapId, -1, 33313, 32339)) {
                if (!candidate.alive() || candidate.template().boss()
                        || candidate.template().undead() != 0
                        || candidate.maximumHp() <= 80
                        || map.isSafetyZone(candidate.x(), candidate.y())) continue;
                for (int deltaY = -1; deltaY <= 1 && monster == null; deltaY++) {
                    for (int deltaX = -1; deltaX <= 1; deltaX++) {
                        if (deltaX == 0 && deltaY == 0) continue;
                        int x = candidate.x() + deltaX;
                        int y = candidate.y() + deltaY;
                        if (map.standingPassable(x, y)
                                && !map.isSafetyZone(x, y)
                                && !hub.classicWorldMonsterOccupies(mapId, x, y)
                                && map.attackPosition(x, y,
                                        candidate.x(), candidate.y(), 3)) {
                            monster = candidate;
                            casterX = x;
                            casterY = y;
                            break;
                        }
                    }
                }
                if (monster != null) break;
            }
            assertNotNull(monster,
                    "deterministic map4 population must expose a nearby non-boss");

            int seed = 0x3810_1734;
            setField(caster, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            hub.connect(caster);
            hub.enterClassicWorld(caster, mapId, casterX, casterY, 5);
            setPhase(caster, "WORLD");
            setField(caster, "classicWorldMapId", mapId);
            setField(caster, "classicWorldInstanceId", -1);
            setField(caster, "positionX", casterX);
            setField(caster, "positionY", casterY);
            for (int index = 0; index < 1_024
                    && client.getInputStream().available() > 0; index++) {
                receive(client, receiver);
            }

            byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(21).c(4).d(monster.objectId())
                    .h(monster.x()).h(monster.y()).bytes();
            invoke(caster, "usePolluteWaterSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.PolluteWaterSpell.class, int.class},
                    request, BattleSpellCatalog.polluteWaterSpell(
                            BattleSpellCatalog.POLLUTE_WATER), 1);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 256)[5] & 0xFF);
            byte[] effect = receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 256);
            assertEquals(monster.objectId(), unsignedInt(effect, 1));
            assertEquals(5830, unsignedShort(effect, 5));
            assertEquals(50, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 256), 1));
            assertTrue(hub.classicWorldMonsterPolluteWaterActive(
                    mapId, -1, monster.objectId(), System.nanoTime()));
            assertEquals(0, inventory(caster).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));

            ClassicWorldMonsterPopulation.Hit damaged =
                    hub.damageClassicWorldMonster(
                            mapId, -1, monster.objectId(), 80);
            assertNotNull(damaged);
            assertTrue(damaged.after().alive());
            int hpBeforeHeal = damaged.after().currentHp();
            BattleSpellCatalog.HealingSpell greaterHeal =
                    BattleSpellCatalog.healingSpell(35);
            assertNotNull(greaterHeal);
            byte[] healRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(4).c(2).d(monster.objectId()).bytes();
            invoke(caster, "useClassicWorldHealingSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.HealingSpell.class, int.class},
                    healRequest, greaterHeal, 8);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 256)[5] & 0xFF);
            byte[] healEffect = receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 256);
            assertEquals(monster.objectId(), unsignedInt(healEffect, 1));
            assertEquals(11724, unsignedShort(healEffect, 5));
            SourceEquipmentMagicStats magic = (SourceEquipmentMagicStats) field(
                    caster, "sourceEquipmentMagicStats");
            int intelligence = magic.effectiveIntelligence(
                    casterElf.intelligence());
            int originalReduction =
                    BattleSpellCatalog.originalMagicConsumeReduction(
                            casterElf.type(), casterElf.originalIntelligence());
            int expectedMp = 50 - greaterHeal.effectiveMpCost(
                    intelligence, originalReduction,
                    magic.healingHelmEquipped());
            assertEquals(expectedMp, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 256), 1));
            byte[] healRatio = receiveUntilOpcode(
                    client, receiver, Protocol.S_HIT_RATIO, 256);
            assertEquals(monster.objectId(), unsignedInt(healRatio, 1));

            int lawfulHealing = greaterHeal.applyLawfulRevision(
                    greaterHeal.diceCount(intelligence) * 8,
                    casterElf.lawful());
            int expectedHealing =
                    BattleHealingRecovery.adjustedSingleTargetAmount(
                            lawfulHealing, false, true, false, false);
            ClassicWorldMonsterPopulation.Snapshot healed =
                    hub.classicWorldMonster(mapId, -1, monster.objectId());
            assertNotNull(healed);
            assertEquals(hpBeforeHeal + expectedHealing, healed.currentHp());
            assertEquals(100 * healed.currentHp() / healed.maximumHp(),
                    healRatio[5] & 0xFF);
            assertTrue(hub.classicWorldMonsterPolluteWaterActive(
                    mapId, -1, monster.objectId(), System.nanoTime()),
                    "Pollute Water is a duration status, not a one-shot lease");
        }
    }

    @Test
    void nativeElementalFallDownLowersEncryptedEarthStatusAndExpiryRestoresIt()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("elemental-fall-down-wire-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 2, InetAddress.getByName("127.0.0.1"));
             Socket casterClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket casterServer = listener.accept();
             Socket targetClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket targetServer = listener.accept()) {
            casterClient.setSoTimeout(1_000);
            targetClient.setSoTimeout(1_000);
            ClientSession caster = new ClientSession(
                    casterServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve(
                            "elemental-fall-down-caster-capture")), hub);
            ClientSession target = new ClientSession(
                    targetServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve(
                            "elemental-fall-down-target-capture")), hub);
            initialize(caster, baseline());
            initialize(target, baseline());
            setField(caster, "account", "elemental-caster-account");
            setField(caster, "selectedCharacter", "ElementalCaster");
            CharacterSummary casterCharacter = new CharacterSummary(
                    "ElementalCaster", "", 2, 0, 50, 100, 100,
                    10, 12, 12, 12, 12, 18, 12, 18, 12,
                    0x3810_1330L, 100, 0, 0, 0, 32740, 32798, 4,
                    12, 40, 1, 12, 12,
                    BattleSpellCatalog.ATTRIBUTE_EARTH);
            setField(caster, "characters", List.of(casterCharacter));
            setField(caster, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(casterCharacter));
            setField(caster, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_EARTH);
            setField(caster, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.ELEMENTAL_FALL_DOWN));
            setField(target, "account", "elemental-target-account");
            setField(target, "selectedCharacter", "ElementalTarget");
            CharacterSummary targetCharacter = character(
                    "ElementalTarget", 1, 0x3810_1331L);
            setField(target, "characters", List.of(targetCharacter));
            setField(target, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(targetCharacter));

            hub.connect(caster);
            hub.connect(target);
            hub.enterChannel(0, caster);
            hub.enterChannel(0, target);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Elemental Fall Down", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, caster);
            hub.joinRoom(roomId, target);
            hub.setForce(roomId, caster, 0);
            hub.setForce(roomId, target, 1);
            hub.setReady(roomId, caster, true);
            hub.setReady(roomId, target, true);
            Method countdown = GameHub.class.getDeclaredMethod(
                    "countdownWork", long.class);
            countdown.setAccessible(true);
            long future = System.nanoTime() + 10_000_000_000L;
            countdown.invoke(hub, future);
            countdown.invoke(hub, future + 2_000_000_000L);
            countdown.invoke(hub, future + 4_000_000_000L);

            setPhase(caster, "BATTLE");
            setPhase(target, "BATTLE");
            setField(caster, "currentRoomId", roomId);
            setField(target, "currentRoomId", roomId);
            setField(caster, "positionX", 32740);
            setField(caster, "positionY", 32798);
            setField(target, "positionX", 32741);
            setField(target, "positionY", 32798);
            setField(caster, "playerMp", 100);
            setField(caster, "playerMaxMp", 100);
            setField(target, "playerHp", 100);
            setField(target, "playerMaxHp", 100);

            int casterSeed = 0x3810_1330;
            int targetSeed = 0x3810_1331;
            setField(caster, "outgoing", new Protocol.CipherState(casterSeed));
            setField(target, "outgoing", new Protocol.CipherState(targetSeed));
            Protocol.CipherState casterReceiver = new Protocol.CipherState(casterSeed);
            Protocol.CipherState targetReceiver = new Protocol.CipherState(targetSeed);

            byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(16).c(4).d(target.playerObjectId())
                    .h(32741).h(32798).bytes();
            Protocol.AttackSpellRequest parsed =
                    Protocol.attackSpellRequest(nativeRequest);
            assertEquals(BattleSpellCatalog.ELEMENTAL_FALL_DOWN,
                    parsed.skillId());
            assertEquals(target.playerObjectId(), parsed.targetObjectId());

            long beforeCast = System.nanoTime();
            invoke(caster, "useElementalFallDownSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.ElementalFallDownSpell.class,
                            int.class},
                    nativeRequest, BattleSpellCatalog.elementalFallDownSpell(
                            BattleSpellCatalog.ELEMENTAL_FALL_DOWN), 1);

            byte[] casterAction = receive(casterClient, casterReceiver);
            assertEquals(Protocol.S_ACTION, casterAction[0] & 0xFF);
            assertEquals(19, casterAction[5] & 0xFF);
            byte[] casterEffect = receive(casterClient, casterReceiver);
            assertEquals(Protocol.S_EFFECT, casterEffect[0] & 0xFF);
            assertEquals(4396, unsignedShort(casterEffect, 5));
            byte[] mana = receive(casterClient, casterReceiver);
            assertEquals(Protocol.S_MANA_POINT, mana[0] & 0xFF);
            assertEquals(90, unsignedShort(mana, 1));

            assertEquals(Protocol.S_ACTION,
                    receive(targetClient, targetReceiver)[0] & 0xFF);
            byte[] activeStatus = receive(targetClient, targetReceiver);
            assertEquals(Protocol.S_STATUS, activeStatus[0] & 0xFF);
            assertEquals(-50, (byte) activeStatus[activeStatus.length - 1]);
            byte[] targetEffect = receive(targetClient, targetReceiver);
            assertEquals(Protocol.S_EFFECT, targetEffect[0] & 0xFF);
            assertEquals(4396, unsignedShort(targetEffect, 5));
            BattleElementalFallDownStatus targetElemental =
                    (BattleElementalFallDownStatus) field(
                            target, "elementalFallDownStatus");
            assertEquals(-50, targetElemental.modifier(
                    BattleSpellCatalog.ATTRIBUTE_EARTH, System.nanoTime()));

            invoke(target, "serverExpireTimedStatuses",
                    new Class<?>[] {long.class},
                    beforeCast + 33_000_000_000L);
            byte[] restoredStatus = receive(targetClient, targetReceiver);
            assertEquals(Protocol.S_STATUS, restoredStatus[0] & 0xFF);
            assertEquals(0, (byte) restoredStatus[restoredStatus.length - 1]);
            assertEquals(0, targetElemental.modifier(
                    BattleSpellCatalog.ATTRIBUTE_EARTH,
                    beforeCast + 33_000_000_000L));
        }
    }

    @Test
    void nativeEraseMagicUsesCase152QuarterMrAndBypassesCounterMagic()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("erase-magic-wire-state.properties"), false);
        GameHub hub = new GameHub(config);
        AccountRepository accounts = new MemoryAccountRepository(
                "erase-fixture", "erase-fixture-password");
        try (ServerSocket listener = new ServerSocket(
                    0, 2, InetAddress.getByName("127.0.0.1"));
             Socket casterClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket casterServer = listener.accept();
             Socket targetClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket targetServer = listener.accept()) {
            casterClient.setSoTimeout(1_000);
            targetClient.setSoTimeout(1_000);
            ClientSession caster = new ClientSession(
                    casterServer, ClientSession.Endpoint.GAME, config, accounts, state,
                    new CaptureLog(temporary.resolve("erase-caster-capture")), hub);
            ClientSession target = new ClientSession(
                    targetServer, ClientSession.Endpoint.GAME, config, accounts, state,
                    new CaptureLog(temporary.resolve("erase-target-capture")), hub);

            RuntimeInventory casterInventory = baseline();
            assertTrue(casterInventory.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 1));
            initialize(caster, casterInventory);
            initialize(target, baseline());
            setField(caster, "account", "erase-caster-account");
            setField(caster, "selectedCharacter", "EraseCaster");
            CharacterSummary casterCharacter = character(
                    "EraseCaster", 2, 0x3810_1530L);
            setField(caster, "characters", List.of(casterCharacter));
            setField(caster, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(casterCharacter));
            setField(caster, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.ERASE_MAGIC));

            setField(target, "account", "erase-target-account");
            setField(target, "selectedCharacter", "EraseTarget");
            CharacterSummary targetCharacter = character(
                    "EraseTarget", 1, 0x3810_1531L);
            setField(target, "characters", List.of(targetCharacter));
            setField(target, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(targetCharacter));

            hub.connect(caster);
            hub.connect(target);
            hub.enterChannel(0, caster);
            hub.enterChannel(0, target);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Erase Magic", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, caster);
            hub.joinRoom(roomId, target);
            hub.setForce(roomId, caster, 0);
            hub.setForce(roomId, target, 1);
            hub.setReady(roomId, caster, true);
            hub.setReady(roomId, target, true);
            Method countdown = GameHub.class.getDeclaredMethod(
                    "countdownWork", long.class);
            countdown.setAccessible(true);
            long future = System.nanoTime() + 10_000_000_000L;
            countdown.invoke(hub, future);
            countdown.invoke(hub, future + 2_000_000_000L);
            countdown.invoke(hub, future + 4_000_000_000L);

            setPhase(caster, "BATTLE");
            setPhase(target, "BATTLE");
            setField(caster, "currentRoomId", roomId);
            setField(target, "currentRoomId", roomId);
            setField(caster, "positionX", 32740);
            setField(caster, "positionY", 32798);
            setField(target, "positionX", 32741);
            setField(target, "positionY", 32798);
            setField(caster, "playerMp", 100);
            setField(caster, "playerMaxMp", 100);
            setField(target, "playerHp", 100);
            setField(target, "playerMaxHp", 100);

            int casterSeed = 0x3810_1530;
            int targetSeed = 0x3810_1531;
            setField(caster, "outgoing", new Protocol.CipherState(casterSeed));
            setField(target, "outgoing", new Protocol.CipherState(targetSeed));
            Protocol.CipherState casterReceiver = new Protocol.CipherState(casterSeed);
            Protocol.CipherState targetReceiver = new Protocol.CipherState(targetSeed);

            BattleTimedStatus targetTimed =
                    (BattleTimedStatus) field(target, "timedStatus");
            targetTimed.activateCounterMagic(System.nanoTime(), 16);
            int baseMr = (int) invokeResult(target,
                    "effectiveBaseMagicResistance", new Class<?>[0]);
            byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(19).c(0).d(target.playerObjectId())
                    .h(32741).h(32798).bytes();
            Protocol.AttackSpellRequest parsed =
                    Protocol.attackSpellRequest(nativeRequest);
            assertEquals(BattleSpellCatalog.ERASE_MAGIC, parsed.skillId());
            assertEquals(target.playerObjectId(), parsed.targetObjectId());

            invoke(caster, "useEraseMagicSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.EraseMagicSpell.class, int.class},
                    nativeRequest, BattleSpellCatalog.eraseMagicSpell(
                            BattleSpellCatalog.ERASE_MAGIC), 1);

            assertEquals(Protocol.S_REMOVE_INVENTORY,
                    receive(casterClient, casterReceiver)[0] & 0xFF);
            byte[] casterAction = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_ACTION, 4);
            assertEquals(Protocol.S_ACTION, casterAction[0] & 0xFF);
            assertEquals(19, casterAction[5] & 0xFF);
            byte[] casterEffect = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 4);
            assertEquals(Protocol.S_EFFECT, casterEffect[0] & 0xFF);
            assertEquals(10706, unsignedShort(casterEffect, 5));
            byte[] mana = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 4);
            assertEquals(Protocol.S_MANA_POINT, mana[0] & 0xFF);
            assertEquals(90, unsignedShort(mana, 1));

            assertEquals(Protocol.S_ACTION,
                    receive(targetClient, targetReceiver)[0] & 0xFF);
            byte[] targetEffect = receive(targetClient, targetReceiver);
            assertEquals(Protocol.S_EFFECT, targetEffect[0] & 0xFF);
            assertEquals(10706, unsignedShort(targetEffect, 5));
            assertTrue(targetTimed.counterMagicActive(System.nanoTime()),
                    "selected source skill153 must bypass rather than consume Counter Magic");
            assertEquals(baseMr >> 2, (int) invokeResult(target,
                    "effectiveBaseMagicResistance", new Class<?>[0]));
            assertEquals(0, inventory(caster).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));

            // Retained TARGET_TO_NPC explicitly includes L1Summon/L1Pet.
            // Exercise that actor-family branch through the same encrypted
            // ClientSession path with an IsErase-qualified source NPC.
            RuntimeInventory refilled = inventory(caster).copy();
            assertTrue(refilled.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 1));
            setField(caster, "inventory", refilled);
            setField(caster, "playerMp", 100);
            ((Map<?, ?>) field(caster, "spellReadyNanos")).clear();
            BattleCompanion companion = BattleCompanion.tamed(
                    0x6000_0153L, 45207, 2158, "$1791", 26,
                    137, 200, 20, -20, 17, 17, 32742, 32798);
            @SuppressWarnings("unchecked")
            List<BattleCompanion> targetCompanions =
                    (List<BattleCompanion>) field(target, "tamedCompanions");
            targetCompanions.add(companion);
            byte[] companionRequest = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(19).c(0)
                    .d(companion.snapshot().objectId())
                    .h(companion.snapshot().x()).h(companion.snapshot().y()).bytes();
            invoke(caster, "useEraseMagicSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.EraseMagicSpell.class, int.class},
                    companionRequest, BattleSpellCatalog.eraseMagicSpell(
                            BattleSpellCatalog.ERASE_MAGIC), 1);

            assertEquals(Protocol.S_REMOVE_INVENTORY,
                    receive(casterClient, casterReceiver)[0] & 0xFF);
            assertEquals(19, receiveUntilOpcode(
                    casterClient, casterReceiver,
                    Protocol.S_ACTION, 4)[5] & 0xFF);
            byte[] companionEffect = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 4);
            assertEquals(companion.snapshot().objectId(),
                    unsignedInt(companionEffect, 1));
            assertEquals(10706, unsignedShort(companionEffect, 5));
            assertEquals(90, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver,
                    Protocol.S_MANA_POINT, 4), 1));
            assertEquals(Protocol.S_ACTION, receiveUntilOpcode(
                    targetClient, targetReceiver,
                    Protocol.S_ACTION, 3)[0] & 0xFF);
            assertEquals(10706, unsignedShort(receiveUntilOpcode(
                    targetClient, targetReceiver,
                    Protocol.S_EFFECT, 3), 5));
            assertEquals(3, companion.effectiveMagicResistance(
                    15, System.nanoTime()));
        }
    }

    @Test
    void nativeLesserElementalUsesCase153EarthNpcAndExistingPetCostNoOp()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("lesser-elemental-wire-state.properties"), false);
        GameHub hub = new GameHub(config);
        AccountRepository accounts = new MemoryAccountRepository(
                "lesser-fixture", "lesser-fixture-password");
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, accounts, state,
                    new CaptureLog(temporary.resolve(
                            "lesser-elemental-wire-capture")), hub);

            RuntimeInventory live = baseline();
            assertTrue(live.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 4));
            initialize(session, live);
            CharacterSummary caster = character(
                    "LesserElementalCaster", 2, 0x3810_1540L);
            setField(session, "account", "lesser-elemental-account");
            setField(session, "selectedCharacter", caster.name());
            setField(session, "characters", List.of(caster));
            setField(session, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(caster));
            setField(session, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.LESSER_ELEMENTAL));
            setField(session, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_EARTH);
            setPhase(session, "BATTLE");
            long roomId = startSoloBattle(hub, session);
            setField(session, "currentRoomId", roomId);
            ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(
                    hub.battleMapId(roomId, session));
            ArenaMapCatalog.Point casterSpawn = arena.spawn(0);
            setField(session, "positionX", casterSpawn.x());
            setField(session, "positionY", casterSpawn.y());
            setField(session, "playerMp", 100);
            setField(session, "playerMaxMp", 100);
            // Tournament sessions own one arena-only companion.  Retained
            // LESSER_ELEMENTAL rejects any active nonzero pet cost, so keep it
            // inactive to expose the successful source-backed branch first.
            BattleCompanion arenaCompanion =
                    (BattleCompanion) field(session, "battleCompanion");
            arenaCompanion.deactivate();

            int seed = 0x3810_1540;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            byte[] nativeRequest = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(19).c(1).bytes();
            Protocol.SpellSelector selector =
                    Protocol.selectorOnlySpellRequest(nativeRequest);
            assertEquals(BattleSpellCatalog.LESSER_ELEMENTAL,
                    selector.skillId());

            long beforeCast = System.nanoTime();
            invoke(session, "useSpell", new Class<?>[] {byte[].class},
                    nativeRequest);

            assertEquals(Protocol.S_CHANGE_COUNT,
                    receive(client, receiver)[0] & 0xFF);
            byte[] castAction = receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 4);
            assertEquals(Protocol.S_ACTION, castAction[0] & 0xFF);
            assertEquals(19, castAction[5] & 0xFF);
            byte[] castEffect = receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 4);
            assertEquals(Protocol.S_EFFECT, castEffect[0] & 0xFF);
            assertEquals(session.playerObjectId(), unsignedInt(castEffect, 1));
            assertEquals(2510, unsignedShort(castEffect, 5));
            byte[] mana = receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 4);
            assertEquals(Protocol.S_MANA_POINT, mana[0] & 0xFF);
            assertEquals(80, unsignedShort(mana, 1));
            assertEquals(Protocol.S_PUT_OBJECT,
                    receive(client, receiver)[0] & 0xFF);
            assertEquals(Protocol.S_MASTER,
                    receive(client, receiver)[0] & 0xFF);
            byte[] speed = receive(client, receiver);
            assertEquals(Protocol.S_SPEED, speed[0] & 0xFF);
            assertEquals(BattleTimedStatus.HASTE, speed[5] & 0xFF);
            byte[] spawnEffect = receive(client, receiver);
            assertEquals(Protocol.S_EFFECT, spawnEffect[0] & 0xFF);
            assertEquals(755, unsignedShort(spawnEffect, 5));

            List<BattleCompanion.Snapshot> elementals = session
                    .controlledCompanionSnapshots().stream()
                    .filter(snapshot -> snapshot.active()
                            && snapshot.kind() == BattleCompanion.Kind.SUMMON)
                    .toList();
            assertEquals(1, elementals.size());
            BattleCompanion.Snapshot elemental = elementals.get(0);
            assertEquals(45306, elemental.npcId());
            assertEquals(2273, elemental.gfx());
            assertEquals("$2007", elemental.name());
            assertEquals(BattleTimedStatus.HASTE, elemental.moveSpeed());
            assertEquals(BattleCompanion.Posture.AGGRESSIVE, elemental.posture());
            assertEquals(caster.charisma() + 7, elemental.petCost());
            assertEquals(elemental.objectId(), unsignedInt(spawnEffect, 1));
            assertTrue(elemental.expiresAtNanos()
                    >= beforeCast + 1_800_000_000_000L);
            assertTrue(elemental.expiresAtNanos()
                    <= System.nanoTime() + 1_800_000_000_000L);
            assertEquals(2, inventory(session).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));

            // The retained handler checks pet cost only after committing the
            // accepted cast.  A second cast therefore consumes the remaining
            // two crystals and 20 MP, but creates no second elemental.
            invoke(session, "useSpell", new Class<?>[] {byte[].class},
                    nativeRequest);
            assertEquals(Protocol.S_REMOVE_INVENTORY,
                    receive(client, receiver)[0] & 0xFF);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 4)[5] & 0xFF);
            assertEquals(2510, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 4), 5));
            assertEquals(60, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 4), 1));
            assertEquals(1, session.controlledCompanionSnapshots().stream()
                    .filter(snapshot -> snapshot.active()
                            && snapshot.kind() == BattleCompanion.Kind.SUMMON)
                    .count());
            assertEquals(0, inventory(session).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));
            assertEquals(0, client.getInputStream().available());
        }
    }

    @Test
    void nativeGreaterElementalUsesCase161EarthNpcAndExistingPetCostNoOp()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("greater-elemental-wire-state.properties"), false);
        GameHub hub = new GameHub(config);
        AccountRepository accounts = new MemoryAccountRepository(
                "greater-fixture", "greater-fixture-password");
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, accounts, state,
                    new CaptureLog(temporary.resolve(
                            "greater-elemental-wire-capture")), hub);

            RuntimeInventory live = baseline();
            assertTrue(live.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 8));
            initialize(session, live);
            CharacterSummary caster = character(
                    "GreaterElementalCaster", 2, 0x3810_1620L);
            setField(session, "account", "greater-elemental-account");
            setField(session, "selectedCharacter", caster.name());
            setField(session, "characters", List.of(caster));
            setField(session, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(caster));
            setField(session, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.GREATER_ELEMENTAL));
            setField(session, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_EARTH);
            setPhase(session, "BATTLE");
            long roomId = startSoloBattle(hub, session);
            setField(session, "currentRoomId", roomId);
            ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(
                    hub.battleMapId(roomId, session));
            ArenaMapCatalog.Point casterSpawn = arena.spawn(0);
            setField(session, "positionX", casterSpawn.x());
            setField(session, "positionY", casterSpawn.y());
            setField(session, "playerMp", 100);
            setField(session, "playerMaxMp", 100);

            BattleCompanion arenaCompanion =
                    (BattleCompanion) field(session, "battleCompanion");
            arenaCompanion.deactivate();

            int seed = 0x3810_1620;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            byte[] nativeRequest = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(20).c(1).bytes();
            Protocol.SpellSelector selector =
                    Protocol.selectorOnlySpellRequest(nativeRequest);
            assertEquals(BattleSpellCatalog.GREATER_ELEMENTAL,
                    selector.skillId());

            long beforeCast = System.nanoTime();
            invoke(session, "useSpell", new Class<?>[] {byte[].class},
                    nativeRequest);

            assertEquals(Protocol.S_CHANGE_COUNT,
                    receive(client, receiver)[0] & 0xFF);
            byte[] castAction = receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 4);
            assertEquals(19, castAction[5] & 0xFF);
            byte[] castEffect = receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 4);
            assertEquals(session.playerObjectId(), unsignedInt(castEffect, 1));
            assertEquals(2510, unsignedShort(castEffect, 5));
            byte[] mana = receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 4);
            assertEquals(80, unsignedShort(mana, 1));
            assertEquals(Protocol.S_PUT_OBJECT,
                    receive(client, receiver)[0] & 0xFF);
            assertEquals(Protocol.S_MASTER,
                    receive(client, receiver)[0] & 0xFF);
            byte[] speed = receive(client, receiver);
            assertEquals(Protocol.S_SPEED, speed[0] & 0xFF);
            assertEquals(BattleTimedStatus.HASTE, speed[5] & 0xFF);
            byte[] spawnEffect = receive(client, receiver);
            assertEquals(Protocol.S_EFFECT, spawnEffect[0] & 0xFF);
            assertEquals(755, unsignedShort(spawnEffect, 5));

            List<BattleCompanion.Snapshot> elementals = session
                    .controlledCompanionSnapshots().stream()
                    .filter(snapshot -> snapshot.active()
                            && snapshot.kind() == BattleCompanion.Kind.SUMMON)
                    .toList();
            assertEquals(1, elementals.size());
            BattleCompanion.Snapshot elemental = elementals.get(0);
            assertEquals(81053, elemental.npcId());
            assertEquals(2267, elemental.gfx());
            assertEquals("$2011", elemental.name());
            assertEquals(45, elemental.level());
            assertEquals(650, elemental.hp());
            assertEquals(60, elemental.mp());
            assertEquals(BattleTimedStatus.HASTE, elemental.moveSpeed());
            assertEquals(BattleCompanion.Posture.AGGRESSIVE, elemental.posture());
            assertEquals(caster.charisma() + 7, elemental.petCost());
            assertEquals(elemental.objectId(), unsignedInt(spawnEffect, 1));
            assertTrue(elemental.expiresAtNanos()
                    >= beforeCast + 1_800_000_000_000L);
            assertTrue(elemental.expiresAtNanos()
                    <= System.nanoTime() + 1_800_000_000_000L);
            assertEquals(4, inventory(session).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));

            invoke(session, "useSpell", new Class<?>[] {byte[].class},
                    nativeRequest);
            assertEquals(Protocol.S_REMOVE_INVENTORY,
                    receive(client, receiver)[0] & 0xFF);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 4)[5] & 0xFF);
            assertEquals(2510, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 4), 5));
            assertEquals(60, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 4), 1));
            assertEquals(1, session.controlledCompanionSnapshots().stream()
                    .filter(snapshot -> snapshot.active()
                            && snapshot.kind() == BattleCompanion.Kind.SUMMON)
                    .count());
            assertEquals(0, inventory(session).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));
            assertEquals(0, client.getInputStream().available());
        }
    }

    @Test
    void nativeEntangleUsesCase151WireConsumesCrystalSlowsAndStopsWith711()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("entangle-wire-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 2, InetAddress.getByName("127.0.0.1"));
             Socket casterClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket casterServer = listener.accept();
             Socket targetClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket targetServer = listener.accept()) {
            casterClient.setSoTimeout(1_000);
            targetClient.setSoTimeout(1_000);
            ClientSession caster = new ClientSession(
                    casterServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("entangle-caster-capture")), hub);
            ClientSession target = new ClientSession(
                    targetServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("entangle-target-capture")), hub);

            RuntimeInventory casterInventory = baseline();
            assertTrue(casterInventory.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 1));
            initialize(caster, casterInventory);
            initialize(target, baseline());
            setField(caster, "account", "entangle-caster-account");
            setField(caster, "selectedCharacter", "EntangleCaster");
            CharacterSummary casterCharacter = character(
                    "EntangleCaster", 2, 0x3810_1520L);
            setField(caster, "characters", List.of(casterCharacter));
            setField(caster, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(casterCharacter));
            setField(caster, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.ENTANGLE));

            setField(target, "account", "entangle-target-account");
            setField(target, "selectedCharacter", "EntangleTarget");
            CharacterSummary targetCharacter = character(
                    "EntangleTarget", 1, 0x3810_1521L);
            setField(target, "characters", List.of(targetCharacter));
            setField(target, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(targetCharacter));

            hub.connect(caster);
            hub.connect(target);
            hub.enterChannel(0, caster);
            hub.enterChannel(0, target);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Entangle", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, caster);
            hub.joinRoom(roomId, target);
            hub.setForce(roomId, caster, 0);
            hub.setForce(roomId, target, 1);
            hub.setReady(roomId, caster, true);
            hub.setReady(roomId, target, true);
            Method countdown = GameHub.class.getDeclaredMethod(
                    "countdownWork", long.class);
            countdown.setAccessible(true);
            long future = System.nanoTime() + 10_000_000_000L;
            countdown.invoke(hub, future);
            countdown.invoke(hub, future + 2_000_000_000L);
            countdown.invoke(hub, future + 4_000_000_000L);

            setPhase(caster, "BATTLE");
            setPhase(target, "BATTLE");
            setField(caster, "currentRoomId", roomId);
            setField(target, "currentRoomId", roomId);
            setField(caster, "positionX", 32740);
            setField(caster, "positionY", 32798);
            setField(target, "positionX", 32741);
            setField(target, "positionY", 32798);
            setField(caster, "playerMp", 100);
            setField(caster, "playerMaxMp", 100);
            setField(target, "playerHp", 100);
            setField(target, "playerMaxHp", 100);

            int casterSeed = 0x3810_1520;
            int targetSeed = 0x3810_1521;
            setField(caster, "outgoing", new Protocol.CipherState(casterSeed));
            setField(target, "outgoing", new Protocol.CipherState(targetSeed));
            Protocol.CipherState casterReceiver = new Protocol.CipherState(casterSeed);
            Protocol.CipherState targetReceiver = new Protocol.CipherState(targetSeed);

            byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(18).c(7).d(target.playerObjectId())
                    .h(32741).h(32798).bytes();
            Protocol.AttackSpellRequest parsed =
                    Protocol.attackSpellRequest(nativeRequest);
            assertEquals(BattleSpellCatalog.ENTANGLE, parsed.skillId());
            assertEquals(target.playerObjectId(), parsed.targetObjectId());

            long beforeCast = System.nanoTime();
            invoke(caster, "useEntangleSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.EntangleSpell.class, int.class},
                    nativeRequest, BattleSpellCatalog.entangleSpell(
                            BattleSpellCatalog.ENTANGLE), 1);

            assertEquals(Protocol.S_REMOVE_INVENTORY,
                    receive(casterClient, casterReceiver)[0] & 0xFF);
            byte[] casterAction = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_ACTION, 4);
            assertEquals(19, casterAction[5] & 0xFF);
            byte[] casterSpeed = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_SPEED, 4);
            assertEquals(BattleTimedStatus.SLOW, casterSpeed[5] & 0xFF);
            assertEquals(0, unsignedShort(casterSpeed, 6));
            byte[] casterEffect = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 4);
            assertEquals(2250, unsignedShort(casterEffect, 5));
            byte[] mana = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 4);
            assertEquals(80, unsignedShort(mana, 1));

            byte[] targetAction = receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_ACTION, 4);
            assertEquals(19, targetAction[5] & 0xFF);
            byte[] targetSpeed = receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_SPEED, 4);
            assertEquals(target.playerObjectId(), unsignedInt(targetSpeed, 1));
            assertEquals(BattleTimedStatus.SLOW, targetSpeed[5] & 0xFF);
            assertEquals(64, unsignedShort(targetSpeed, 6));
            byte[] targetEffect = receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_EFFECT, 4);
            assertEquals(2250, unsignedShort(targetEffect, 5));

            BattleTimedStatus targetStatus =
                    (BattleTimedStatus) field(target, "timedStatus");
            assertEquals(BattleTimedStatus.SLOW,
                    targetStatus.speed(System.nanoTime()).type());
            assertEquals(BattleSpellCatalog.ENTANGLE,
                    targetStatus.slowSkillId(System.nanoTime()));
            assertEquals(0, inventory(caster).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));

            invoke(target, "serverExpireTimedStatuses",
                    new Class<?>[] {long.class},
                    beforeCast + 65_000_000_000L);
            byte[] restored = receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_SPEED, 4);
            assertEquals(BattleTimedStatus.NORMAL, restored[5] & 0xFF);
            assertEquals(0, unsignedShort(restored, 6));
            byte[] stopped = receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_MESSAGE_CODE, 4);
            assertEquals(711, unsignedShort(stopped, 1));
            assertEquals(BattleTimedStatus.NORMAL,
                    targetStatus.speed(beforeCast + 65_000_000_000L).type());

            byte[] observerRestored = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_SPEED, 4);
            assertEquals(BattleTimedStatus.NORMAL,
                    observerRestored[5] & 0xFF);
            RuntimeInventory refilled = inventory(caster).copy();
            assertTrue(refilled.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 1));
            setField(caster, "inventory", refilled);
            setField(caster, "playerMp", 100);
            targetStatus.activateBrave(
                    System.nanoTime(), 300, BattleTimedStatus.BRAVE_SECOND);
            invoke(caster, "useEntangleSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.EntangleSpell.class, int.class},
                    nativeRequest, BattleSpellCatalog.entangleSpell(
                            BattleSpellCatalog.ENTANGLE), 1);
            assertEquals(Protocol.S_REMOVE_INVENTORY,
                    receive(casterClient, casterReceiver)[0] & 0xFF);
            assertEquals(Protocol.S_ACTION, receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_ACTION, 3)[0] & 0xFF);
            assertEquals(80, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver,
                    Protocol.S_MANA_POINT, 3), 1));
            assertEquals(Protocol.S_ACTION, receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_ACTION, 3)[0] & 0xFF);
            assertEquals(BattleTimedStatus.NORMAL,
                    targetStatus.speed(System.nanoTime()).type());
            assertEquals(5, targetStatus.brave(System.nanoTime()).mode());

            assertTrue(targetStatus.cancelMagicEffects().brave());
            RuntimeInventory worldRefill = inventory(caster).copy();
            assertTrue(worldRefill.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 1));
            setField(caster, "inventory", worldRefill);
            setField(caster, "playerMp", 100);
            setPhase(caster, "WORLD");
            setPhase(target, "WORLD");
            setField(caster, "classicWorldMapId", 4);
            setField(target, "classicWorldMapId", 4);
            setField(caster, "classicWorldInstanceId", -1);
            setField(target, "classicWorldInstanceId", -1);
            setField(caster, "positionX", 33313);
            setField(caster, "positionY", 32339);
            setField(target, "positionX", 33312);
            setField(target, "positionY", 32339);
            ClassicWorldMapCatalog.ClassicMap worldMap =
                    ClassicWorldMapCatalog.forMap(4);
            assertFalse(worldMap.isSafetyZone(33313, 32339));
            assertFalse(worldMap.isSafetyZone(33312, 32339));
            assertTrue(worldMap.attackPosition(
                    33313, 32339, 33312, 32339, 10));
            hub.enterClassicWorld(caster, 4, 33313, 32339, 4);
            hub.enterClassicWorld(target, 4, 33312, 32339, 0);

            byte[] worldRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(18).c(7).d(target.playerObjectId())
                    .h(33312).h(32339).bytes();
            invoke(caster, "useEntangleSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.EntangleSpell.class, int.class},
                    worldRequest, BattleSpellCatalog.entangleSpell(
                            BattleSpellCatalog.ENTANGLE), 1);
            assertEquals(Protocol.S_REMOVE_INVENTORY,
                    receive(casterClient, casterReceiver)[0] & 0xFF);
            assertEquals(19, receiveUntilOpcode(
                    casterClient, casterReceiver,
                    Protocol.S_ACTION, 4)[5] & 0xFF);
            byte[] worldObserverSpeed = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_SPEED, 4);
            assertEquals(BattleTimedStatus.SLOW,
                    worldObserverSpeed[5] & 0xFF);
            assertEquals(0, unsignedShort(worldObserverSpeed, 6));
            assertEquals(2250, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver,
                    Protocol.S_EFFECT, 4), 5));
            assertEquals(80, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver,
                    Protocol.S_MANA_POINT, 4), 1));
            assertEquals(19, receiveUntilOpcode(
                    targetClient, targetReceiver,
                    Protocol.S_ACTION, 4)[5] & 0xFF);
            byte[] worldTargetSpeed = receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_SPEED, 4);
            assertEquals(BattleTimedStatus.SLOW,
                    worldTargetSpeed[5] & 0xFF);
            assertEquals(64, unsignedShort(worldTargetSpeed, 6));
            assertEquals(2250, unsignedShort(receiveUntilOpcode(
                    targetClient, targetReceiver,
                    Protocol.S_EFFECT, 4), 5));
            assertEquals(BattleSpellCatalog.ENTANGLE,
                    targetStatus.slowSkillId(System.nanoTime()));
        }
    }

    @Test
    void nativeBloodySoulPreservesOneHpAndAddsConfiguredTwentyMp()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("bloody-soul-wire-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("bloody-soul-capture")), hub);
            initialize(session, baseline());
            CharacterSummary character = character(
                    "BloodySoulElf", 2, 0x3810_1460L);
            setField(session, "account", "bloody-soul-account");
            setField(session, "selectedCharacter", "BloodySoulElf");
            setField(session, "characters", List.of(character));
            setField(session, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(character));
            setField(session, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.BLOODY_SOUL));
            hub.connect(session);
            hub.enterChannel(0, session);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Bloody Soul", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, session);
            setPhase(session, "BATTLE");
            setField(session, "currentRoomId", roomId);
            setField(session, "playerMaxHp", 100);
            setField(session, "playerHp", 40);
            setField(session, "playerMaxMp", 100);
            setField(session, "playerMp", 75);

            int seed = 0x3810_1460;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            byte[] nativeRequest = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(18).c(1).bytes();
            assertEquals(BattleSpellCatalog.BLOODY_SOUL,
                    Protocol.selectorOnlySpellRequest(nativeRequest).skillId());

            invoke(session, "useSpell", new Class<?>[] {byte[].class},
                    nativeRequest);
            byte[] insufficient = receiveUntilOpcode(
                    client, receiver, Protocol.S_MESSAGE_CODE, 2);
            assertEquals(279, unsignedShort(insufficient, 1));
            assertEquals(40, field(session, "playerHp"));
            assertEquals(75, field(session, "playerMp"));

            setField(session, "playerHp", 81);
            invoke(session, "useSpell", new Class<?>[] {byte[].class},
                    nativeRequest);
            byte[] action = receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 4);
            assertEquals(19, action[5] & 0xFF);
            byte[] effect = receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 4);
            assertEquals(2178, unsignedShort(effect, 5));
            byte[] hp = receiveUntilOpcode(
                    client, receiver, Protocol.S_HIT_POINT, 4);
            assertEquals(41, unsignedShort(hp, 1));
            byte[] mana = receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 4);
            assertEquals(95, unsignedShort(mana, 1));
            assertEquals(41, field(session, "playerHp"));
            assertEquals(95, field(session, "playerMp"));

            ((java.util.Map<?, ?>) field(session, "spellReadyNanos")).clear();
            setField(session, "playerHp", 81);
            setField(session, "playerMp", 75);
            setPhase(session, "WORLD");
            setField(session, "classicWorldMapId", 4);
            setField(session, "classicWorldInstanceId", -1);
            setField(session, "positionX", 33051);
            setField(session, "positionY", 32337);
            hub.enterClassicWorld(session, 4, 33051, 32337, 5);
            invoke(session, "useSpell", new Class<?>[] {byte[].class},
                    nativeRequest);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 4)[5] & 0xFF);
            assertEquals(2178, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 4), 5));
            assertEquals(41, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_HIT_POINT, 4), 1));
            assertEquals(95, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 4), 1));
            assertEquals(41, field(session, "playerHp"));
            assertEquals(95, field(session, "playerMp"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void nativeReturnToNatureCoversSuccessInvalidPetAndProbabilityFailure()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("return-to-nature-wire-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (AccountRepository accounts = AccountRepository.open(config);
             ServerSocket listener = new ServerSocket(
                    0, 2, InetAddress.getByName("127.0.0.1"));
             Socket casterClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket casterServer = listener.accept();
             Socket ownerClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket ownerServer = listener.accept()) {
            casterClient.setSoTimeout(1_000);
            ownerClient.setSoTimeout(1_000);
            ClientSession caster = new ClientSession(
                    casterServer, ClientSession.Endpoint.GAME, config, accounts, state,
                    new CaptureLog(temporary.resolve(
                            "return-to-nature-caster-capture")), hub);
            ClientSession owner = new ClientSession(
                    ownerServer, ClientSession.Endpoint.GAME, config, accounts, state,
                    new CaptureLog(temporary.resolve(
                            "return-to-nature-owner-capture")), hub);
            RuntimeInventory casterInventory = baseline();
            assertTrue(casterInventory.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 7));
            initialize(caster, casterInventory);
            initialize(owner, baseline());

            CharacterSummary casterCharacter = character(
                    "NatureCaster", 2, 0x3810_1450L);
            CharacterSummary ownerCharacter = character(
                    "SummonOwner", 2, 0x3810_1451L);
            setField(caster, "account", "nature-caster-account");
            setField(caster, "selectedCharacter", "NatureCaster");
            setField(caster, "characters", List.of(casterCharacter));
            setField(caster, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(casterCharacter));
            setField(caster, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.RETURN_TO_NATURE));
            setField(owner, "account", "summon-owner-account");
            setField(owner, "selectedCharacter", "SummonOwner");
            setField(owner, "characters", List.of(ownerCharacter));
            setField(owner, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(ownerCharacter));

            hub.connect(caster);
            hub.connect(owner);
            hub.enterChannel(0, caster);
            hub.enterChannel(0, owner);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Return to Nature", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, caster);
            hub.joinRoom(roomId, owner);
            hub.setForce(roomId, caster, 0);
            hub.setForce(roomId, owner, 1);
            hub.setReady(roomId, caster, true);
            hub.setReady(roomId, owner, true);
            Method countdown = GameHub.class.getDeclaredMethod(
                    "countdownWork", long.class);
            countdown.setAccessible(true);
            long future = System.nanoTime() + 10_000_000_000L;
            countdown.invoke(hub, future);
            countdown.invoke(hub, future + 2_000_000_000L);
            countdown.invoke(hub, future + 4_000_000_000L);

            setPhase(caster, "BATTLE");
            setPhase(owner, "BATTLE");
            setField(caster, "currentRoomId", roomId);
            setField(owner, "currentRoomId", roomId);
            setField(caster, "positionX", 32740);
            setField(caster, "positionY", 32798);
            setField(owner, "positionX", 32742);
            setField(owner, "positionY", 32798);
            setField(caster, "playerHp", 100);
            setField(caster, "playerMaxHp", 100);
            setField(caster, "playerMp", 100);
            setField(caster, "playerMaxMp", 100);
            setField(owner, "playerHp", 100);
            setField(owner, "playerMaxHp", 100);

            BattleSpellCatalog.SummonChoice choice =
                    BattleSpellCatalog.summonChoice(7);
            SourceSummonTemplate template =
                    SourceSummonTemplate.memoryFixture(choice.npcId());
            BattleCompanion summon = BattleCompanion.summoned(
                    0x6810_1450L, choice, template, 6, 1_800,
                    32741, 32798, System.nanoTime());
            assertTrue(summon.receiveItem(InventoryCatalog.require(
                    InventoryCatalog.EXTRA_HEALING_POTION), 2));
            ((List<BattleCompanion>) field(owner, "tamedCompanions")).add(summon);

            int casterSeed = 0x3810_1450;
            int ownerSeed = 0x3810_1451;
            setField(caster, "outgoing", new Protocol.CipherState(casterSeed));
            setField(owner, "outgoing", new Protocol.CipherState(ownerSeed));
            Protocol.CipherState casterReceiver = new Protocol.CipherState(casterSeed);
            Protocol.CipherState ownerReceiver = new Protocol.CipherState(ownerSeed);

            byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(18).c(0).d(summon.snapshot().objectId())
                    .h(32741).h(32798).bytes();
            Protocol.AttackSpellRequest parsed =
                    Protocol.attackSpellRequest(nativeRequest);
            assertEquals(BattleSpellCatalog.RETURN_TO_NATURE, parsed.skillId());
            assertEquals(summon.snapshot().objectId(), parsed.targetObjectId());

            invoke(caster, "useReturnToNatureSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.ReturnToNatureSpell.class,
                            int.class}, nativeRequest,
                    BattleSpellCatalog.returnToNatureSpell(
                            BattleSpellCatalog.RETURN_TO_NATURE), 1);

            byte[] casterAction = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_ACTION, 8);
            assertEquals(19, casterAction[5] & 0xFF);
            byte[] casterEffect = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 8);
            assertEquals(summon.snapshot().objectId(), unsignedInt(casterEffect, 1));
            assertEquals(2245, unsignedShort(casterEffect, 5));
            assertEquals(Protocol.S_REMOVE_OBJECT,
                    receiveUntilOpcode(casterClient, casterReceiver,
                            Protocol.S_REMOVE_OBJECT, 4)[0] & 0xFF);
            assertEquals(Protocol.S_ACTION,
                    receiveUntilOpcode(ownerClient, ownerReceiver,
                            Protocol.S_ACTION, 4)[0] & 0xFF);
            byte[] ownerEffect = receiveUntilOpcode(
                    ownerClient, ownerReceiver, Protocol.S_EFFECT, 4);
            assertEquals(2245, unsignedShort(ownerEffect, 5));
            assertEquals(Protocol.S_REMOVE_OBJECT,
                    receiveUntilOpcode(ownerClient, ownerReceiver,
                            Protocol.S_REMOVE_OBJECT, 4)[0] & 0xFF);

            assertFalse(summon.snapshot().active());
            assertTrue(((List<BattleCompanion>) field(owner, "tamedCompanions"))
                    .stream().noneMatch(candidate -> candidate == summon));
            assertEquals(5, inventory(caster).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));
            assertEquals(2, inventory(owner).catalogCounts().getOrDefault(
                    InventoryCatalog.EXTRA_HEALING_POTION, 0));
            assertEquals(70, field(caster, "playerMp"));

            BattleCompanion arenaPet =
                    (BattleCompanion) field(owner, "battleCompanion");
            arenaPet.resetForBattle(32741, 32797);
            byte[] invalidPetRequest = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(18).c(0)
                    .d(arenaPet.snapshot().objectId())
                    .h(32741).h(32797).bytes();
            invoke(caster, "useReturnToNatureSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.ReturnToNatureSpell.class,
                            int.class}, invalidPetRequest,
                    BattleSpellCatalog.returnToNatureSpell(
                            BattleSpellCatalog.RETURN_TO_NATURE), 1);
            byte[] invalidMessage = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MESSAGE_CODE, 8);
            assertEquals(79, unsignedShort(invalidMessage, 1));
            assertEquals(Protocol.S_ACTION,
                    receiveUntilOpcode(ownerClient, ownerReceiver,
                            Protocol.S_ACTION, 4)[0] & 0xFF);
            assertTrue(arenaPet.snapshot().active());
            assertEquals(3, inventory(caster).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));
            assertEquals(40, field(caster, "playerMp"));

            BattleCompanion failedSummon = BattleCompanion.summoned(
                    0x6810_1451L, choice, template, 6, 1_800,
                    32741, 32798, System.nanoTime());
            ((List<BattleCompanion>) field(owner, "tamedCompanions"))
                    .add(failedSummon);
            byte[] failedRequest = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(18).c(0)
                    .d(failedSummon.snapshot().objectId())
                    .h(32741).h(32798).bytes();
            invoke(caster, "useReturnToNatureSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.ReturnToNatureSpell.class,
                            int.class}, failedRequest,
                    BattleSpellCatalog.returnToNatureSpell(
                            BattleSpellCatalog.RETURN_TO_NATURE), 100);
            byte[] failureMessage = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MESSAGE_CODE, 8);
            assertEquals(280, unsignedShort(failureMessage, 1));
            assertEquals(Protocol.S_ACTION,
                    receiveUntilOpcode(ownerClient, ownerReceiver,
                            Protocol.S_ACTION, 4)[0] & 0xFF);
            assertTrue(failedSummon.snapshot().active());
            assertEquals(1, inventory(caster).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));
            assertEquals(10, field(caster, "playerMp"));
        }
    }

    @Test
    void nativeCounterMirrorConsumesCrystalAndReflectsEncryptedPlayerMagicOnce()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("counter-mirror-wire-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 2, InetAddress.getByName("127.0.0.1"));
             Socket attackerClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket attackerServer = listener.accept();
             Socket defenderClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket defenderServer = listener.accept()) {
            attackerClient.setSoTimeout(1_000);
            defenderClient.setSoTimeout(1_000);
            ClientSession attacker = new ClientSession(
                    attackerServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve(
                            "counter-mirror-attacker-capture")), hub);
            ClientSession defender = new ClientSession(
                    defenderServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve(
                            "counter-mirror-defender-capture")), hub);
            initialize(attacker, baseline());
            initialize(defender, baseline());

            CharacterSummary attackerCharacter = character(
                    "MirrorAttacker", 3, 0x3810_1340L);
            CharacterSummary defenderCharacter = character(
                    "MirrorDefender", 2, 0x3810_1341L);
            setField(attacker, "account", "mirror-attacker-account");
            setField(attacker, "selectedCharacter", "MirrorAttacker");
            setField(attacker, "characters", List.of(attackerCharacter));
            setField(attacker, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(attackerCharacter));
            setField(defender, "account", "mirror-defender-account");
            setField(defender, "selectedCharacter", "MirrorDefender");
            setField(defender, "characters", List.of(defenderCharacter));
            setField(defender, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(defenderCharacter));
            setField(defender, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.COUNTER_MIRROR));
            RuntimeInventory defenderInventory = inventory(defender).copy();
            assertTrue(defenderInventory.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 1));
            setField(defender, "inventory", defenderInventory);

            hub.connect(attacker);
            hub.connect(defender);
            hub.enterChannel(0, attacker);
            hub.enterChannel(0, defender);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Counter Mirror", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, attacker);
            hub.joinRoom(roomId, defender);
            hub.setForce(roomId, attacker, 0);
            hub.setForce(roomId, defender, 1);
            hub.setReady(roomId, attacker, true);
            hub.setReady(roomId, defender, true);
            Method countdown = GameHub.class.getDeclaredMethod(
                    "countdownWork", long.class);
            countdown.setAccessible(true);
            long future = System.nanoTime() + 10_000_000_000L;
            countdown.invoke(hub, future);
            countdown.invoke(hub, future + 2_000_000_000L);
            countdown.invoke(hub, future + 4_000_000_000L);

            setPhase(attacker, "BATTLE");
            setPhase(defender, "BATTLE");
            setField(attacker, "currentRoomId", roomId);
            setField(defender, "currentRoomId", roomId);
            setField(attacker, "positionX", 32740);
            setField(attacker, "positionY", 32798);
            setField(defender, "positionX", 32741);
            setField(defender, "positionY", 32798);
            setField(attacker, "playerHp", 100);
            setField(attacker, "playerMaxHp", 100);
            setField(attacker, "playerMp", 100);
            setField(attacker, "playerMaxMp", 100);
            setField(defender, "playerHp", 100);
            setField(defender, "playerMaxHp", 100);
            setField(defender, "playerMp", 100);
            setField(defender, "playerMaxMp", 100);

            int attackerSeed = 0x3810_1340;
            int defenderSeed = 0x3810_1341;
            setField(attacker, "outgoing", new Protocol.CipherState(attackerSeed));
            setField(defender, "outgoing", new Protocol.CipherState(defenderSeed));
            Protocol.CipherState attackerReceiver =
                    new Protocol.CipherState(attackerSeed);
            Protocol.CipherState defenderReceiver =
                    new Protocol.CipherState(defenderSeed);

            byte[] mirrorRequest = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(16).c(5).bytes();
            invoke(defender, "useCounterMirrorSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.CounterMirrorSpell.class},
                    mirrorRequest, BattleSpellCatalog.counterMirrorSpell(
                            BattleSpellCatalog.COUNTER_MIRROR));

            byte[] defenderCast = receiveUntilOpcode(
                    defenderClient, defenderReceiver, Protocol.S_ACTION, 8);
            assertEquals(19, defenderCast[5] & 0xFF);
            byte[] activation = receiveUntilOpcode(
                    defenderClient, defenderReceiver, Protocol.S_EFFECT, 4);
            assertEquals(defender.playerObjectId(), unsignedInt(activation, 1));
            assertEquals(4395, unsignedShort(activation, 5));
            byte[] mana = receiveUntilOpcode(
                    defenderClient, defenderReceiver, Protocol.S_MANA_POINT, 4);
            assertEquals(90, unsignedShort(mana, 1));
            receiveUntilOpcode(attackerClient, attackerReceiver,
                    Protocol.S_ACTION, 4);
            assertEquals(0, inventory(defender).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));
            BattleCounterMirrorStatus mirrorStatus =
                    (BattleCounterMirrorStatus) field(
                            defender, "counterMirrorStatus");
            assertTrue(mirrorStatus.active(System.nanoTime()));

            byte[] attackRequest = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(0).c(3)
                    .d(defender.playerObjectId()).h(32741).h(32798).bytes();
            invoke(attacker, "useAttackSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.AttackSpell.class, int.class},
                    attackRequest, BattleSpellCatalog.attackSpell(
                            BattleSpellCatalog.LIGHT_ARROW), 0);

            byte[] triggerEffect = receiveUntilOpcode(
                    defenderClient, defenderReceiver, Protocol.S_EFFECT, 8);
            assertEquals(defender.playerObjectId(),
                    unsignedInt(triggerEffect, 1));
            assertEquals(4395, unsignedShort(triggerEffect, 5));
            byte[] reflectedReaction = receiveUntilOpcode(
                    defenderClient, defenderReceiver, Protocol.S_ACTION, 4);
            assertEquals(attacker.playerObjectId(),
                    unsignedInt(reflectedReaction, 1));
            assertEquals(BattleActionCodes.DAMAGE,
                    reflectedReaction[5] & 0xFF);

            assertEquals(100, field(defender, "playerHp"));
            assertTrue((int) field(attacker, "playerHp") < 100);
            assertFalse(mirrorStatus.active(System.nanoTime()));
        }
    }

    @Test
    void nativeClearMindConsumesCrystalPublishesWisdomAndRestoresItOnExpiry()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("clear-mind-wire-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("clear-mind-capture")), hub);
            initialize(session, baseline());
            CharacterSummary elf = character("ClearMindElf", 2, 0x3810_1370L);
            setField(session, "account", "clear-mind-account");
            setField(session, "selectedCharacter", elf.name());
            setField(session, "characters", List.of(elf));
            setField(session, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(elf));
            setField(session, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.CLEAR_MIND));
            RuntimeInventory live = inventory(session).copy();
            assertTrue(live.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 1));
            setField(session, "inventory", live);

            hub.connect(session);
            hub.enterChannel(0, session);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Clear Mind", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, session);
            setPhase(session, "BATTLE");
            setField(session, "currentRoomId", roomId);
            setField(session, "playerHp", 100);
            setField(session, "playerMaxHp", 100);
            setField(session, "playerMp", 100);
            setField(session, "playerMaxMp", 100);

            int seed = 0x3810_1370;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            Method effectiveMr = ClientSession.class.getDeclaredMethod(
                    "effectiveBaseMagicResistance", long.class);
            effectiveMr.setAccessible(true);
            long beforeCast = System.nanoTime();
            int baseMr = (int) effectiveMr.invoke(session, beforeCast);

            byte[] request = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(17).c(0).bytes();
            invoke(session, "useClearMindSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.ClearMindSpell.class},
                    request, BattleSpellCatalog.clearMindSpell(
                            BattleSpellCatalog.CLEAR_MIND));

            byte[] action = receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 8);
            assertEquals(19, action[5] & 0xFF);
            byte[] effect = receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 6);
            assertEquals(2180, unsignedShort(effect, 5));
            byte[] mana = receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 6);
            assertEquals(90, unsignedShort(mana, 1));
            byte[] activeStatus = receiveUntilOpcode(
                    client, receiver, Protocol.S_STATUS, 8);
            assertEquals(15, activeStatus[12] & 0xFF);
            byte[] activeAbilities = receiveUntilOpcode(
                    client, receiver, Protocol.S_ABILITY_SCORES, 4);
            assertEquals(15, activeAbilities[3] & 0xFF);
            byte[] happen = receiveUntilOpcode(
                    client, receiver, Protocol.S_MESSAGE_CODE, 4);
            assertEquals(722, unsignedShort(happen, 1));
            assertEquals(0, inventory(session).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));
            assertEquals(baseMr + 3,
                    (int) effectiveMr.invoke(session, System.nanoTime()));

            BattleClearMindStatus status = (BattleClearMindStatus) field(
                    session, "clearMindStatus");
            assertEquals(3, status.snapshot(System.nanoTime()).wisdomModifier());
            BattleCounterMirrorStatus mirror =
                    (BattleCounterMirrorStatus) field(session, "counterMirrorStatus");
            long mirrorNow = System.nanoTime();
            mirror.activate(mirrorNow, 16);
            Method mirrorDecision = ClientSession.class.getDeclaredMethod(
                    "counterMirrorDecision", int.class, boolean.class,
                    int.class, long.class);
            mirrorDecision.setAccessible(true);
            BattleCounterMirrorStatus.Decision boostedDecision =
                    (BattleCounterMirrorStatus.Decision) mirrorDecision.invoke(
                            session, 20, true, 14, mirrorNow);
            assertTrue(boostedDecision.triggered());
            assertEquals(15, boostedDecision.wisdom());

            setField(session, "playerMp", 50);
            BattleNaturalMpRegen regen =
                    (BattleNaturalMpRegen) field(session, "naturalMpRegen");
            long regenStart = System.nanoTime();
            regen.reset(regenStart);
            invoke(session, "serverNaturalMpRegen",
                    new Class<?>[] {long.class, boolean.class},
                    regenStart + 16_000_000_000L, true);
            byte[] regeneratedMana = receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 4);
            assertEquals(52, unsignedShort(regeneratedMana, 1));
            setField(session, "playerMp", 100);
            invoke(session, "serverExpireTimedStatuses",
                    new Class<?>[] {long.class},
                    beforeCast + 1_201_000_000_000L);
            byte[] restoredStatus = receiveUntilOpcode(
                    client, receiver, Protocol.S_STATUS, 12);
            assertEquals(12, restoredStatus[12] & 0xFF);
            byte[] restoredAbilities = receiveUntilOpcode(
                    client, receiver, Protocol.S_ABILITY_SCORES, 6);
            assertEquals(12, restoredAbilities[3] & 0xFF);
            assertEquals(baseMr,
                    (int) effectiveMr.invoke(
                            session, beforeCast + 1_201_000_000_000L));
            assertFalse(status.snapshot(
                    beforeCast + 1_201_000_000_000L).active());
        }
    }

    @Test
    void nativeResistElementalPublishesFourResistancesAndFeedsDamageConsumer()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("resist-elemental-wire-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve(
                            "resist-elemental-capture")), hub);
            initialize(session, baseline());
            CharacterSummary elf = character(
                    "ResistElementalElf", 2, 0x3810_1380L);
            setField(session, "account", "resist-elemental-account");
            setField(session, "selectedCharacter", elf.name());
            setField(session, "characters", List.of(elf));
            setField(session, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(elf));
            setField(session, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.RESIST_ELEMENTAL,
                            BattleSpellCatalog.CANCELLATION));
            RuntimeInventory live = inventory(session).copy();
            assertTrue(live.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 2));
            assertTrue(live.add(InventoryCatalog.require(
                    InventoryCatalog.MAGIC_GEM), 2));
            setField(session, "inventory", live);

            hub.connect(session);
            hub.enterChannel(0, session);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Resist Elemental", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, session);
            setPhase(session, "BATTLE");
            setField(session, "currentRoomId", roomId);
            setField(session, "playerHp", 100);
            setField(session, "playerMaxHp", 100);
            setField(session, "playerMp", 100);
            setField(session, "playerMaxMp", 100);

            int seed = 0x3810_1380;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            Method effectiveResistance = ClientSession.class.getDeclaredMethod(
                    "effectiveSpellElementalResistance", int.class, long.class);
            effectiveResistance.setAccessible(true);
            long beforeCast = System.nanoTime();
            int[] attributes = {
                    BattleSpellCatalog.ATTRIBUTE_FIRE,
                    BattleSpellCatalog.ATTRIBUTE_WATER,
                    BattleSpellCatalog.ATTRIBUTE_WIND,
                    BattleSpellCatalog.ATTRIBUTE_EARTH};
            int[] baselineResistance = new int[attributes.length];
            for (int i = 0; i < attributes.length; i++) {
                baselineResistance[i] = (int) effectiveResistance.invoke(
                        session, attributes[i], beforeCast);
            }

            byte[] request = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(17).c(1).bytes();
            invoke(session, "useResistElementalSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.ResistElementalSpell.class},
                    request, BattleSpellCatalog.resistElementalSpell(
                            BattleSpellCatalog.RESIST_ELEMENTAL));

            byte[] action = receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 8);
            assertEquals(19, action[5] & 0xFF);
            byte[] effect = receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 6);
            assertEquals(2184, unsignedShort(effect, 5));
            byte[] mana = receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 6);
            assertEquals(90, unsignedShort(mana, 1));
            byte[] activeStatus = receiveUntilOpcode(
                    client, receiver, Protocol.S_STATUS, 8);
            for (int i = 0; i < attributes.length; i++) {
                assertEquals(baselineResistance[i] + 10,
                        (byte) activeStatus[activeStatus.length - 4 + i]);
                assertEquals(baselineResistance[i] + 10,
                        (int) effectiveResistance.invoke(
                                session, attributes[i], System.nanoTime()));
            }
            byte[] happen = receiveUntilOpcode(
                    client, receiver, Protocol.S_MESSAGE_CODE, 4);
            assertEquals(721, unsignedShort(happen, 1));
            assertEquals(1, inventory(session).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));

            int withoutBuff = BattleElementalResistance.magicDamage(
                    64, 18, baselineResistance[0]);
            int withBuff = BattleElementalResistance.magicDamage(
                    64, 18, (int) effectiveResistance.invoke(
                            session, BattleSpellCatalog.ATTRIBUTE_FIRE,
                            System.nanoTime()));
            assertTrue(withBuff < withoutBuff);
            BattleResistElementalStatus status =
                    (BattleResistElementalStatus) field(
                            session, "resistElementalStatus");
            assertEquals(10, status.modifier(System.nanoTime()));
            BattleElementalProtectionStatus elementalProtection =
                    (BattleElementalProtectionStatus) field(
                            session, "elementalProtectionStatus");
            elementalProtection.activate(System.nanoTime(), 64,
                    BattleSpellCatalog.ATTRIBUTE_EARTH, 50);
            BattleFireWeaponStatus fireWeapon =
                    (BattleFireWeaponStatus) field(
                            session, "fireWeaponStatus");
            fireWeapon.activate(System.nanoTime(), 960, 4);
            String fireWeaponStateKey = GameStateStore.sourceCharacter(
                    (String) field(session, "selectedClassicServerName"),
                    session.playerName());
            state.saveTimedFireWeaponBuff(fireWeaponStateKey,
                    new GameStateStore.TimedFireWeaponBuff(960));
            BattleBurningWeaponStatus burningWeapon =
                    (BattleBurningWeaponStatus) field(
                            session, "burningWeaponStatus");
            burningWeapon.activate(System.nanoTime(), 960, 3, 6);
            state.saveTimedBurningWeaponBuff(fireWeaponStateKey,
                    new GameStateStore.TimedBurningWeaponBuff(960));
            BattleWindShotStatus windShot =
                    (BattleWindShotStatus) field(session, "windShotStatus");
            windShot.activate(System.nanoTime(), 960, 6);
            state.saveTimedWindShotBuff(fireWeaponStateKey,
                    new GameStateStore.TimedWindShotBuff(960));
            BattleFireBlessStatus fireBless =
                    (BattleFireBlessStatus) field(session, "fireBlessStatus");
            fireBless.activate(System.nanoTime(), 960, 4);
            state.saveTimedFireBlessBuff(fireWeaponStateKey,
                    new GameStateStore.TimedFireBlessBuff(960));
            BattleStormEyeStatus stormEye =
                    (BattleStormEyeStatus) field(session, "stormEyeStatus");
            stormEye.activate(System.nanoTime(), 960, 2, 3);
            state.saveTimedStormEyeBuff(fireWeaponStateKey,
                    new GameStateStore.TimedStormEyeBuff(960));
            BattleStormShotStatus stormShot =
                    (BattleStormShotStatus) field(session, "stormShotStatus");
            stormShot.activate(System.nanoTime(), 960, -1, 5);
            state.saveTimedStormShotBuff(fireWeaponStateKey,
                    new GameStateStore.TimedStormShotBuff(960));
            BattleEarthBlessStatus earthBless =
                    (BattleEarthBlessStatus) field(session, "earthBlessStatus");
            earthBless.activate(System.nanoTime(), 960, -7);
            state.saveTimedEarthBlessBuff(fireWeaponStateKey,
                    new GameStateStore.TimedEarthBlessBuff(960));
            BattleIronSkinStatus ironSkin =
                    (BattleIronSkinStatus) field(session, "ironSkinStatus");
            ironSkin.activate(System.nanoTime(), 960, -10);
            state.saveTimedIronSkinBuff(fireWeaponStateKey,
                    new GameStateStore.TimedIronSkinBuff(960));
            BattleAquaProtectStatus aquaProtect =
                    (BattleAquaProtectStatus) field(session, "aquaProtectStatus");
            aquaProtect.activate(System.nanoTime(), 960, 5);
            BattleTimedStatus timedStatus =
                    (BattleTimedStatus) field(session, "timedStatus");
            timedStatus.activateBrave(System.nanoTime(), 960,
                    BattleTimedStatus.BRAVE_HOLY_WALK);
            setField(session, "activeMovementBuffSkillId",
                    BattleSpellCatalog.WIND_WALK);
            state.saveTimedMovementBuff(fireWeaponStateKey,
                    new GameStateStore.TimedMovementBuff(
                            BattleSpellCatalog.WIND_WALK, 960));
            BattleAbilityStatus abilityStatus =
                    (BattleAbilityStatus) field(session, "abilityStatus");
            abilityStatus.activateArmorProtection(
                    BattleSpellCatalog.EARTH_SKIN,
                    System.nanoTime(), 960, -6);
            state.saveTimedArmorProtection(fireWeaponStateKey,
                    new GameStateStore.TimedArmorProtection(
                            BattleSpellCatalog.EARTH_SKIN, 960));

            byte[] cancellationRequest = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(5).c(3)
                    .d(session.playerObjectId()).bytes();
            invoke(session, "useCancellationSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.CancellationSpell.class},
                    cancellationRequest, BattleSpellCatalog.cancellationSpell(
                            BattleSpellCatalog.CANCELLATION));
            byte[] cancellationStatus = receiveUntilOpcode(
                    client, receiver, Protocol.S_STATUS, 20);
            for (int i = 0; i < attributes.length; i++) {
                assertEquals(baselineResistance[i],
                        (byte) cancellationStatus[
                                cancellationStatus.length - 4 + i]);
            }
            assertFalse(status.snapshot(System.nanoTime()).active());
            assertFalse(elementalProtection.snapshot(
                    System.nanoTime()).active());
            assertFalse(fireWeapon.snapshot(System.nanoTime()).active());
            assertFalse(burningWeapon.snapshot(System.nanoTime()).active());
            assertFalse(windShot.snapshot(System.nanoTime()).active());
            assertFalse(aquaProtect.snapshot(System.nanoTime()).active());
            assertFalse(fireBless.snapshot(System.nanoTime()).active());
            assertFalse(stormEye.snapshot(System.nanoTime()).active());
            assertFalse(stormShot.snapshot(System.nanoTime()).active());
            assertFalse(earthBless.snapshot(System.nanoTime()).active());
            assertFalse(ironSkin.snapshot(System.nanoTime()).active());
            assertFalse(state.timedFireWeaponBuff(
                    fireWeaponStateKey).active());
            assertFalse(state.timedBurningWeaponBuff(
                    fireWeaponStateKey).active());
            assertFalse(state.timedWindShotBuff(
                    fireWeaponStateKey).active());
            assertFalse(state.timedFireBlessBuff(
                    fireWeaponStateKey).active());
            assertFalse(state.timedStormEyeBuff(
                    fireWeaponStateKey).active());
            assertFalse(state.timedStormShotBuff(
                    fireWeaponStateKey).active());
            assertFalse(state.timedEarthBlessBuff(
                    fireWeaponStateKey).active());
            assertFalse(state.timedIronSkinBuff(
                    fireWeaponStateKey).active());
            assertEquals(BattleTimedStatus.BRAVE_NORMAL,
                    timedStatus.brave(System.nanoTime()).mode());
            assertEquals(0, field(session, "activeMovementBuffSkillId"));
            assertFalse(state.timedMovementBuff(
                    fireWeaponStateKey).active());
            assertEquals(new BattleAbilityStatus.ArmorProtection(0, 0, 0),
                    abilityStatus.armorProtection(System.nanoTime()));
            assertFalse(state.timedArmorProtection(
                    fireWeaponStateKey).active());
            assertEquals(0, inventory(session).catalogCounts().getOrDefault(
                    InventoryCatalog.MAGIC_GEM, 0));

            setField(session, "playerMp", 100);
            invoke(session, "useResistElementalSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.ResistElementalSpell.class},
                    request, BattleSpellCatalog.resistElementalSpell(
                            BattleSpellCatalog.RESIST_ELEMENTAL));
            byte[] refreshedStatus = receiveUntilOpcode(
                    client, receiver, Protocol.S_STATUS, 20);
            // Self-target Cancellation publishes one authoritative restore;
            // its enclosing player-target path may leave that restore queued
            // behind the resource frames.  Advance to the recast snapshot.
            for (int attempt = 0; attempt < 5
                    && (byte) refreshedStatus[refreshedStatus.length - 4]
                            != baselineResistance[0] + 10; attempt++) {
                refreshedStatus = receiveUntilOpcode(
                        client, receiver, Protocol.S_STATUS, 20);
            }
            for (int i = 0; i < attributes.length; i++) {
                assertEquals(baselineResistance[i] + 10,
                        (byte) refreshedStatus[
                                refreshedStatus.length - 4 + i]);
            }
            assertEquals(0, inventory(session).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));

            invoke(session, "serverExpireTimedStatuses",
                    new Class<?>[] {long.class},
                    beforeCast + 1_201_000_000_000L);
            byte[] restoredStatus = receiveUntilOpcode(
                    client, receiver, Protocol.S_STATUS, 12);
            for (int attempt = 0; attempt < 5
                    && (byte) restoredStatus[restoredStatus.length - 4]
                            != baselineResistance[0]; attempt++) {
                restoredStatus = receiveUntilOpcode(
                        client, receiver, Protocol.S_STATUS, 12);
            }
            for (int i = 0; i < attributes.length; i++) {
                assertEquals(baselineResistance[i],
                        (byte) restoredStatus[restoredStatus.length - 4 + i]);
                assertEquals(baselineResistance[i],
                        (int) effectiveResistance.invoke(
                                session, attributes[i],
                                beforeCast + 1_201_000_000_000L));
            }
            assertFalse(status.snapshot(
                    beforeCast + 1_201_000_000_000L).active());
        }
    }

    @Test
    void nativeElementalProtectionRejectsNoAttributeAndRoutesBattleAndWorld()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("elemental-protection-wire-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve(
                            "elemental-protection-capture")), hub);
            initialize(session, baseline());
            CharacterSummary elf = character(
                    "ElementalProtectionElf", 2, 0x3810_1470L);
            setField(session, "account", "elemental-protection-account");
            setField(session, "selectedCharacter", elf.name());
            setField(session, "characters", List.of(elf));
            setField(session, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(elf));
            setField(session, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.ELEMENTAL_PROTECTION));
            RuntimeInventory live = inventory(session).copy();
            assertTrue(live.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 3));
            setField(session, "inventory", live);

            hub.connect(session);
            hub.enterChannel(0, session);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Elemental Protection", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, session);
            setPhase(session, "BATTLE");
            setField(session, "currentRoomId", roomId);
            setField(session, "playerHp", 100);
            setField(session, "playerMaxHp", 100);
            setField(session, "playerMp", 100);
            setField(session, "playerMaxMp", 100);
            setField(session, "elfAttribute", 0);

            int seed = 0x3810_1470;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            byte[] request = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(18).c(2).bytes();

            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            byte[] noAttribute = receiveUntilOpcode(
                    client, receiver, Protocol.S_MESSAGE_CODE, 4);
            assertEquals(280, unsignedShort(noAttribute, 1));
            assertEquals(100, field(session, "playerMp"));
            assertEquals(3, inventory(session).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));

            int appliedMask = BattleSpellCatalog.ATTRIBUTE_EARTH
                    | BattleSpellCatalog.ATTRIBUTE_WATER;
            setField(session, "elfAttribute", appliedMask);
            Method effectiveSpell = ClientSession.class.getDeclaredMethod(
                    "effectiveSpellElementalResistance", int.class, long.class);
            effectiveSpell.setAccessible(true);
            Method effectiveWeapon = ClientSession.class.getDeclaredMethod(
                    "effectiveWeaponAttributeResistance", int.class);
            effectiveWeapon.setAccessible(true);
            long baselineNow = System.nanoTime();
            int[] attributes = {
                    BattleSpellCatalog.ATTRIBUTE_FIRE,
                    BattleSpellCatalog.ATTRIBUTE_WATER,
                    BattleSpellCatalog.ATTRIBUTE_WIND,
                    BattleSpellCatalog.ATTRIBUTE_EARTH};
            int[] baselineResistance = new int[attributes.length];
            for (int index = 0; index < attributes.length; index++) {
                baselineResistance[index] = (int) effectiveSpell.invoke(
                        session, attributes[index], baselineNow);
            }

            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            byte[] action = receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 8);
            assertEquals(19, action[5] & 0xFF);
            byte[] effect = receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 6);
            assertEquals(2285, unsignedShort(effect, 5));
            byte[] mana = receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 6);
            assertEquals(94, unsignedShort(mana, 1));
            byte[] activeStatus = receiveUntilOpcode(
                    client, receiver, Protocol.S_STATUS, 8);
            for (int index = 0; index < attributes.length; index++) {
                int expected = baselineResistance[index]
                        + ((appliedMask & attributes[index]) != 0 ? 50 : 0);
                assertEquals(expected,
                        (byte) activeStatus[activeStatus.length - 4 + index]);
                assertEquals(expected, (int) effectiveSpell.invoke(
                        session, attributes[index], System.nanoTime()));
            }
            assertEquals(baselineResistance[1] + 50,
                    (int) effectiveWeapon.invoke(
                            session, BattleSpellCatalog.ATTRIBUTE_WATER));
            int withoutProtection = BattleElementalResistance.magicDamage(
                    64, 18, baselineResistance[1]);
            int withProtection = BattleElementalResistance.magicDamage(
                    64, 18, (int) effectiveSpell.invoke(session,
                            BattleSpellCatalog.ATTRIBUTE_WATER,
                            System.nanoTime()));
            assertTrue(withProtection < withoutProtection);
            BattleElementalProtectionStatus status =
                    (BattleElementalProtectionStatus) field(
                            session, "elementalProtectionStatus");
            assertEquals(appliedMask,
                    status.snapshot(System.nanoTime()).attributeMask());
            assertEquals(2, inventory(session).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));

            setField(session, "playerMp", 100);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            byte[] refreshedStatus = receiveUntilOpcode(
                    client, receiver, Protocol.S_STATUS, 20);
            for (int index = 0; index < attributes.length; index++) {
                int expected = baselineResistance[index]
                        + ((appliedMask & attributes[index]) != 0 ? 50 : 0);
                assertEquals(expected,
                        (byte) refreshedStatus[refreshedStatus.length - 4 + index]);
            }
            assertEquals(1, inventory(session).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));

            long expiredAt = System.nanoTime()
                    + java.util.concurrent.TimeUnit.SECONDS.toNanos(65);
            invoke(session, "serverExpireTimedStatuses",
                    new Class<?>[] {long.class}, expiredAt);
            for (int index = 0; index < attributes.length; index++) {
                assertEquals(baselineResistance[index],
                        (int) effectiveSpell.invoke(
                                session, attributes[index], expiredAt));
            }
            assertFalse(status.snapshot(expiredAt).active());

            int worldMap = ClassicWorldMapCatalog.WORLD_TREE_MAP_ID;
            int worldX = ClassicWorldMapCatalog.WORLD_TREE_X;
            int worldY = ClassicWorldMapCatalog.WORLD_TREE_Y;
            hub.enterClassicWorld(session, worldMap, worldX, worldY, 5);
            setPhase(session, "WORLD");
            setField(session, "classicWorldMapId", worldMap);
            setField(session, "classicWorldInstanceId", -1);
            setField(session, "positionX", worldX);
            setField(session, "positionY", worldY);
            setField(session, "playerMp", 100);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 12)[5] & 0xFF);
            assertEquals(2285, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(94, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8), 1));
            byte[] worldStatus = receiveUntilOpcode(
                    client, receiver, Protocol.S_STATUS, 10);
            for (int index = 0; index < attributes.length; index++) {
                int expected = baselineResistance[index]
                        + ((appliedMask & attributes[index]) != 0 ? 50 : 0);
                assertEquals(expected,
                        (byte) worldStatus[worldStatus.length - 4 + index]);
            }
            assertEquals(0, inventory(session).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));
        }
    }

    @Test
    void nativeFireWeaponUsesObjectSelfTargetAndRoutesBattleAndWorld()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        Path stateFile = temporary.resolve("fire-weapon-wire-state.properties");
        GameStateStore state = new GameStateStore(stateFile, true);
        GameHub hub = new GameHub(config);
        String persistedKey = null;
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("fire-weapon-capture")), hub);
            initialize(session, baseline());
            CharacterSummary elf = character("FireWeaponElf", 2, 0x3810_1480L);
            setField(session, "account", "fire-weapon-account");
            setField(session, "selectedCharacter", elf.name());
            setField(session, "inventoryCharacter", elf.name());
            setField(session, "characters", List.of(elf));
            setField(session, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(elf));
            setField(session, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.FIRE_WEAPON));

            hub.connect(session);
            hub.enterChannel(0, session);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Fire Weapon", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, session);
            setPhase(session, "BATTLE");
            setField(session, "currentRoomId", roomId);
            setField(session, "playerHp", 100);
            setField(session, "playerMaxHp", 100);
            setField(session, "playerMp", 100);
            setField(session, "playerMaxMp", 100);

            int seed = 0x3810_1480;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            byte[] wrongTarget = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(18).c(3)
                    .d(session.playerObjectId() + 1).bytes();
            setField(session, "elfAttribute", 0);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, wrongTarget);
            assertEquals(280, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MESSAGE_CODE, 4), 1));
            assertEquals(100, field(session, "playerMp"));

            byte[] request = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(18).c(3)
                    .d(session.playerObjectId()).bytes();
            // Exact retained DB attr=0 bypasses isAttrAgrees even when the
            // character currently has no elemental affinity.
            setField(session, "elfAttribute", 0);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 8)[5] & 0xFF);
            assertEquals(2182, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(85, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8), 1));
            assertEquals(723, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MESSAGE_CODE, 8), 1));
            BattleFireWeaponStatus status =
                    (BattleFireWeaponStatus) field(session, "fireWeaponStatus");
            assertEquals(4, status.meleeDamage(System.nanoTime()));
            String fireWeaponStateKey = GameStateStore.sourceCharacter(
                    (String) field(session, "selectedClassicServerName"),
                    elf.name());
            persistedKey = fireWeaponStateKey;
            assertEquals(960, state.timedFireWeaponBuff(
                    fireWeaponStateKey).remainingSeconds());

            setField(session, "playerMp", 100);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            receiveUntilOpcode(client, receiver, Protocol.S_ACTION, 8);
            receiveUntilOpcode(client, receiver, Protocol.S_EFFECT, 8);
            receiveUntilOpcode(client, receiver, Protocol.S_MANA_POINT, 8);
            receiveUntilOpcode(client, receiver, Protocol.S_MESSAGE_CODE, 8);
            assertEquals(4, status.meleeDamage(System.nanoTime()));

            long expiredAt = System.nanoTime()
                    + java.util.concurrent.TimeUnit.SECONDS.toNanos(961);
            invoke(session, "serverExpireTimedStatuses",
                    new Class<?>[] {long.class}, expiredAt);
            assertFalse(status.snapshot(expiredAt).active());
            assertFalse(state.timedFireWeaponBuff(fireWeaponStateKey).active());

            int worldMap = ClassicWorldMapCatalog.WORLD_TREE_MAP_ID;
            int worldX = ClassicWorldMapCatalog.WORLD_TREE_X;
            int worldY = ClassicWorldMapCatalog.WORLD_TREE_Y;
            hub.enterClassicWorld(session, worldMap, worldX, worldY, 5);
            setPhase(session, "WORLD");
            setField(session, "classicWorldMapId", worldMap);
            setField(session, "classicWorldInstanceId", -1);
            setField(session, "positionX", worldX);
            setField(session, "positionY", worldY);
            setField(session, "playerMp", 100);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 12)[5] & 0xFF);
            assertEquals(2182, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(85, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8), 1));
            assertEquals(723, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MESSAGE_CODE, 8), 1));
            assertEquals(4, status.meleeDamage(System.nanoTime()));
        }
        assertTrue(persistedKey != null);
        assertTrue(new GameStateStore(stateFile, true)
                .timedFireWeaponBuff(persistedKey).active());
    }

    @Test
    void nativeBurningWeaponUsesCase162SelfTargetBattleWorldAndPersistence()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        Path stateFile = temporary.resolve("burning-weapon-wire-state.properties");
        GameStateStore state = new GameStateStore(stateFile, true);
        GameHub hub = new GameHub(config);
        String persistedKey = null;
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("burning-weapon-capture")), hub);
            initialize(session, baseline());
            CharacterSummary elf = character(
                    "BurningWeaponElf", 2, 0x3810_1630L);
            setField(session, "account", "burning-weapon-account");
            setField(session, "selectedCharacter", elf.name());
            setField(session, "inventoryCharacter", elf.name());
            setField(session, "characters", List.of(elf));
            setField(session, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(elf));
            setField(session, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.BURNING_WEAPON));

            hub.connect(session);
            hub.enterChannel(0, session);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Burning Weapon", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, session);
            setPhase(session, "BATTLE");
            setField(session, "currentRoomId", roomId);
            setField(session, "playerHp", 100);
            setField(session, "playerMaxHp", 100);
            setField(session, "playerMp", 100);
            setField(session, "playerMaxMp", 100);

            int seed = 0x3810_1630;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            byte[] wrongTarget = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(20).c(2)
                    .d(session.playerObjectId() + 1).bytes();
            setField(session, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_FIRE);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, wrongTarget);
            assertEquals(280, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MESSAGE_CODE, 4), 1));
            assertEquals(100, field(session, "playerMp"));

            byte[] request = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(20).c(2)
                    .d(session.playerObjectId()).bytes();
            // Retained isAttrAgrees() runs before resource/effect handling.
            // An ordinary non-fire elf receives no packet and consumes no MP.
            setField(session, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_EARTH);
            client.setSoTimeout(100);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            try {
                receive(client, receiver);
                throw new AssertionError(
                        "non-fire Burning Weapon unexpectedly emitted a packet");
            } catch (SocketTimeoutException expected) {
                // Exact retained rejection is silent.
            }
            assertEquals(100, field(session, "playerMp"));
            BattleBurningWeaponStatus status =
                    (BattleBurningWeaponStatus) field(
                            session, "burningWeaponStatus");
            assertFalse(status.snapshot(System.nanoTime()).active());

            setField(session, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_FIRE);
            client.setSoTimeout(1_000);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 8)[5] & 0xFF);
            assertEquals(2242, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(70, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8), 1));
            assertEquals(723, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MESSAGE_CODE, 8), 1));
            long now = System.nanoTime();
            assertEquals(3, status.physicalHit(false, now));
            assertEquals(6, status.physicalDamage(false, now));
            assertEquals(0, status.physicalHit(true, now));
            assertEquals(0, status.physicalDamage(true, now));
            String stateKey = GameStateStore.sourceCharacter(
                    (String) field(session, "selectedClassicServerName"),
                    elf.name());
            persistedKey = stateKey;
            assertEquals(960, state.timedBurningWeaponBuff(
                    stateKey).remainingSeconds());

            long expiredAt = System.nanoTime()
                    + java.util.concurrent.TimeUnit.SECONDS.toNanos(961);
            invoke(session, "serverExpireTimedStatuses",
                    new Class<?>[] {long.class}, expiredAt);
            assertFalse(status.snapshot(expiredAt).active());
            assertFalse(state.timedBurningWeaponBuff(stateKey).active());

            int worldMap = ClassicWorldMapCatalog.WORLD_TREE_MAP_ID;
            int worldX = ClassicWorldMapCatalog.WORLD_TREE_X;
            int worldY = ClassicWorldMapCatalog.WORLD_TREE_Y;
            hub.enterClassicWorld(session, worldMap, worldX, worldY, 5);
            setPhase(session, "WORLD");
            setField(session, "classicWorldMapId", worldMap);
            setField(session, "classicWorldInstanceId", -1);
            setField(session, "positionX", worldX);
            setField(session, "positionY", worldY);
            setField(session, "playerMp", 100);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 12)[5] & 0xFF);
            assertEquals(2242, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(70, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8), 1));
            assertEquals(723, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MESSAGE_CODE, 8), 1));
            now = System.nanoTime();
            assertEquals(3, status.physicalHit(false, now));
            assertEquals(6, status.physicalDamage(false, now));
        }
        assertTrue(persistedKey != null);
        assertTrue(new GameStateStore(stateFile, true)
                .timedBurningWeaponBuff(persistedKey).active());
    }

    @Test
    void nativeStormShotUsesCase165WindSelfTargetBattleWorldAndPersistence()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        Path stateFile = temporary.resolve("storm-shot-wire-state.properties");
        GameStateStore state = new GameStateStore(stateFile, true);
        GameHub hub = new GameHub(config);
        String persistedKey = null;
        try {
            try (ServerSocket listener = new ServerSocket(
                        0, 1, InetAddress.getByName("127.0.0.1"));
                 Socket client = new Socket("127.0.0.1", listener.getLocalPort());
                 Socket server = listener.accept()) {
                client.setSoTimeout(1_000);
                ClientSession session = new ClientSession(
                        server, ClientSession.Endpoint.GAME, config, null, state,
                        new CaptureLog(temporary.resolve("storm-shot-capture")), hub);
                initialize(session, baseline());
                CharacterSummary elf = character("StormShotElf", 2, 0x3810_1660L);
                setField(session, "account", "storm-shot-account");
                setField(session, "selectedCharacter", elf.name());
                setField(session, "inventoryCharacter", elf.name());
                setField(session, "characters", List.of(elf));
                setField(session, "sourceBaseMagicResistance",
                        SourceBaseMagicResistance.from(elf));
                setField(session, "learnedSkills",
                        java.util.Set.of(BattleSpellCatalog.STORM_SHOT));

                hub.connect(session);
                hub.enterChannel(0, session);
                long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                        "Storm Shot", "", 2, 1, 0, 0, 0, 0));
                hub.joinRoom(roomId, session);
                setPhase(session, "BATTLE");
                setField(session, "currentRoomId", roomId);
                setField(session, "playerHp", 100);
                setField(session, "playerMaxHp", 100);
                setField(session, "playerMp", 100);
                setField(session, "playerMaxMp", 100);

                int seed = 0x3810_1660;
                setField(session, "outgoing", new Protocol.CipherState(seed));
                Protocol.CipherState receiver = new Protocol.CipherState(seed);
                byte[] wrongTarget = new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(20).c(5)
                        .d(session.playerObjectId() + 1).bytes();
                setField(session, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_WIND);
                invoke(session, "useSpell", new Class<?>[] {byte[].class}, wrongTarget);
                assertEquals(280, unsignedShort(receiveUntilOpcode(
                        client, receiver, Protocol.S_MESSAGE_CODE, 4), 1));
                assertEquals(100, field(session, "playerMp"));

                byte[] request = new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(20).c(5)
                        .d(session.playerObjectId()).bytes();
                // Exact DB attr=0: execution does not re-check wind. Normal
                // source progression still grants the skill only to wind elves.
                setField(session, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_EARTH);
                BattleStormShotStatus status = (BattleStormShotStatus) field(
                        session, "stormShotStatus");
                assertFalse(status.snapshot(System.nanoTime()).active());
                client.setSoTimeout(1_000);
                invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
                assertEquals(19, receiveUntilOpcode(
                        client, receiver, Protocol.S_ACTION, 8)[5] & 0xFF);
                assertEquals(2248, unsignedShort(receiveUntilOpcode(
                        client, receiver, Protocol.S_EFFECT, 8), 5));
                assertEquals(70, unsignedShort(receiveUntilOpcode(
                        client, receiver, Protocol.S_MANA_POINT, 8), 1));
                assertEquals(724, unsignedShort(receiveUntilOpcode(
                        client, receiver, Protocol.S_MESSAGE_CODE, 8), 1));
                long now = System.nanoTime();
                assertEquals(-1, status.physicalHit(true, now));
                assertEquals(5, status.physicalDamage(true, now));
                assertEquals(0, status.physicalHit(false, now));
                assertEquals(0, status.physicalDamage(false, now));
                Object rangedModifiers = invokeResult(session,
                        "nativeWeaponBuffModifiers",
                        new Class<?>[] {boolean.class, long.class}, true, now);
                assertEquals(-1, field(rangedModifiers, "hit"));
                assertEquals(5, field(rangedModifiers, "damage"));
                Object meleeModifiers = invokeResult(session,
                        "nativeWeaponBuffModifiers",
                        new Class<?>[] {boolean.class, long.class}, false, now);
                assertEquals(0, field(meleeModifiers, "hit"));
                assertEquals(0, field(meleeModifiers, "damage"));

                String stateKey = GameStateStore.sourceCharacter(
                        (String) field(session, "selectedClassicServerName"),
                        elf.name());
                persistedKey = stateKey;
                assertEquals(960, state.timedStormShotBuff(
                        stateKey).remainingSeconds());

                long expiredAt = System.nanoTime()
                        + java.util.concurrent.TimeUnit.SECONDS.toNanos(961);
                invoke(session, "serverExpireTimedStatuses",
                        new Class<?>[] {long.class}, expiredAt);
                assertFalse(status.snapshot(expiredAt).active());
                assertFalse(state.timedStormShotBuff(stateKey).active());

                int worldMap = ClassicWorldMapCatalog.WORLD_TREE_MAP_ID;
                int worldX = ClassicWorldMapCatalog.WORLD_TREE_X;
                int worldY = ClassicWorldMapCatalog.WORLD_TREE_Y;
                hub.enterClassicWorld(session, worldMap, worldX, worldY, 5);
                setPhase(session, "WORLD");
                setField(session, "classicWorldMapId", worldMap);
                setField(session, "classicWorldInstanceId", -1);
                setField(session, "positionX", worldX);
                setField(session, "positionY", worldY);
                setField(session, "playerMp", 100);
                invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
                assertEquals(19, receiveUntilOpcode(
                        client, receiver, Protocol.S_ACTION, 12)[5] & 0xFF);
                assertEquals(2248, unsignedShort(receiveUntilOpcode(
                        client, receiver, Protocol.S_EFFECT, 8), 5));
                assertEquals(70, unsignedShort(receiveUntilOpcode(
                        client, receiver, Protocol.S_MANA_POINT, 8), 1));
                assertEquals(724, unsignedShort(receiveUntilOpcode(
                        client, receiver, Protocol.S_MESSAGE_CODE, 8), 1));
                now = System.nanoTime();
                assertEquals(-1, status.physicalHit(true, now));
                assertEquals(5, status.physicalDamage(true, now));
            }
        } finally {
            hub.close();
        }
        assertTrue(persistedKey != null);
        assertTrue(new GameStateStore(stateFile, true)
                .timedStormShotBuff(persistedKey).active());
    }

    @Test
    void nativeNaturesBlessingUsesSelectorOnlyWaterGateModifiersAndBothWorlds()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("natures-blessing-wire-state.properties"), true);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("natures-blessing-capture")), hub);
            initialize(session, baseline());
            CharacterSummary elf = character(
                    "NaturesBlessingElf", 2, 0x3810_1640L);
            setField(session, "account", "natures-blessing-account");
            setField(session, "selectedCharacter", elf.name());
            setField(session, "inventoryCharacter", elf.name());
            setField(session, "characters", List.of(elf));
            setField(session, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(elf));
            setField(session, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.NATURES_BLESSING));
            setField(session, "playerLawful", 0);

            hub.connect(session);
            hub.enterChannel(0, session);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Nature's Blessing", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, session);
            setPhase(session, "BATTLE");
            setField(session, "currentRoomId", roomId);
            setField(session, "playerHp", 20);
            setField(session, "playerMaxHp", 100);
            setField(session, "playerMp", 100);
            setField(session, "playerMaxMp", 100);
            BattleCompanion companion = (BattleCompanion) field(
                    session, "battleCompanion");
            companion.resetForBattle(
                    (int) field(session, "positionX") + 1,
                    (int) field(session, "positionY"));
            companion.applyDamage(10);
            assertEquals(20, companion.snapshot().hp());

            int seed = 0x3810_1640;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            byte[] request = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(20).c(3).bytes();
            BattleSpellCatalog.NaturesBlessingSpell spell =
                    BattleSpellCatalog.naturesBlessingSpell(
                            BattleSpellCatalog.NATURES_BLESSING);
            Class<?>[] fixtureSignature = {
                    byte[].class,
                    BattleSpellCatalog.NaturesBlessingSpell.class,
                    int.class
            };

            // Attribute rejection happens before target/resource/effect work.
            setField(session, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_EARTH);
            client.setSoTimeout(100);
            invoke(session, "useNaturesBlessingSpell", fixtureSignature,
                    request, spell, 1);
            try {
                receive(client, receiver);
                throw new AssertionError(
                        "non-water Nature's Blessing unexpectedly emitted a packet");
            } catch (SocketTimeoutException expected) {
                // Exact retained rejection is silent.
            }
            assertEquals(100, field(session, "playerMp"));
            assertEquals(20, field(session, "playerHp"));

            setField(session, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_WATER);
            BattleWaterLifeStatus waterLife = (BattleWaterLifeStatus) field(
                    session, "waterLifeStatus");
            BattleTimedStatus timed = (BattleTimedStatus) field(session, "timedStatus");
            long statusNow = System.nanoTime();
            waterLife.activate(statusNow, 64);
            timed.activatePolluteWater(statusNow, 64);
            client.setSoTimeout(1_000);
            invoke(session, "useNaturesBlessingSpell", fixtureSignature,
                    request, spell, 1);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 8)[5] & 0xFF);
            assertEquals(2244, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(70, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8), 1));
            byte[] battleHp = receiveUntilOpcode(
                    client, receiver, Protocol.S_HIT_POINT, 8);
            int intelligence = ((SourceEquipmentMagicStats) field(
                    session, "sourceEquipmentMagicStats"))
                    .effectiveIntelligence(elf.intelligence());
            int expectedHp = 20 + spell.diceCount(intelligence);
            assertEquals(expectedHp, unsignedShort(battleHp, 1));
            assertFalse(waterLife.active(System.nanoTime()));
            // WATER_LIFE doubles first and POLLUTE_WATER halves afterward.
            assertEquals(expectedHp, field(session, "playerHp"));
            assertEquals(30, companion.snapshot().hp());
            assertEquals(Protocol.S_HIT_RATIO,
                    receiveUntilOpcode(client, receiver,
                            Protocol.S_HIT_RATIO, 8)[0] & 0xFF);

            @SuppressWarnings("unchecked")
            Map<Integer, Long> cooldowns =
                    (Map<Integer, Long>) field(session, "spellReadyNanos");
            cooldowns.clear();
            int worldMap = ClassicWorldMapCatalog.WORLD_TREE_MAP_ID;
            int worldX = ClassicWorldMapCatalog.WORLD_TREE_X;
            int worldY = ClassicWorldMapCatalog.WORLD_TREE_Y;
            hub.enterClassicWorld(session, worldMap, worldX, worldY, 5);
            setPhase(session, "WORLD");
            setField(session, "classicWorldMapId", worldMap);
            setField(session, "classicWorldInstanceId", -1);
            setField(session, "positionX", worldX);
            setField(session, "positionY", worldY);
            setField(session, "playerHp", 20);
            setField(session, "playerMp", 100);
            timed.cancelMagicEffects();
            waterLife.activate(System.nanoTime(), 64);
            invoke(session, "useNaturesBlessingSpell", fixtureSignature,
                    request, spell, 1);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 8)[5] & 0xFF);
            assertEquals(2244, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(70, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8), 1));
            // Without Pollute Water, the same fixed roll is doubled.
            assertEquals(20 + spell.diceCount(intelligence) * 2,
                    unsignedShort(receiveUntilOpcode(
                            client, receiver, Protocol.S_HIT_POINT, 8), 1));
            assertFalse(waterLife.active(System.nanoTime()));
        }
    }

    @Test
    void nativeNaturesBlessingHealsVisiblePartyButNotUnpartiedRoomPeer()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("natures-blessing-party-state.properties"), true);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 2, InetAddress.getByName("127.0.0.1"));
             Socket casterClient = new Socket(
                     "127.0.0.1", listener.getLocalPort());
             Socket casterServer = listener.accept();
             Socket targetClient = new Socket(
                     "127.0.0.1", listener.getLocalPort());
             Socket targetServer = listener.accept()) {
            ClientSession caster = new ClientSession(
                    casterServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("natures-party-caster")), hub);
            ClientSession target = new ClientSession(
                    targetServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("natures-party-target")), hub);
            initialize(caster, baseline());
            initialize(target, baseline());
            CharacterSummary casterElf = character(
                    "NaturePartyCaster", 2, 0x3810_1641L);
            CharacterSummary targetElf = character(
                    "NaturePartyTarget", 2, 0x3810_1642L);
            setField(caster, "account", "nature-party-caster-account");
            setField(caster, "selectedCharacter", casterElf.name());
            setField(caster, "inventoryCharacter", casterElf.name());
            setField(caster, "characters", List.of(casterElf));
            setField(caster, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_WATER);
            setField(caster, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.NATURES_BLESSING));
            setField(caster, "playerLawful", 0);
            setField(target, "account", "nature-party-target-account");
            setField(target, "selectedCharacter", targetElf.name());
            setField(target, "inventoryCharacter", targetElf.name());
            setField(target, "characters", List.of(targetElf));
            setField(target, "playerLawful", 0);

            hub.connect(caster);
            hub.connect(target);
            hub.enterChannel(0, caster);
            hub.enterChannel(0, target);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Nature party target", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, caster);
            hub.joinRoom(roomId, target);
            for (ClientSession session : List.of(caster, target)) {
                setPhase(session, "BATTLE");
                setField(session, "currentRoomId", roomId);
                setField(session, "playerHp", 20);
                setField(session, "playerMaxHp", 100);
                setField(session, "playerMp", 100);
                setField(session, "playerMaxMp", 100);
            }
            setField(caster, "positionX", 32800);
            setField(caster, "positionY", 32800);
            setField(target, "positionX", 32801);
            setField(target, "positionY", 32800);
            setField(caster, "outgoing", new Protocol.CipherState(0x3810_1641));
            setField(target, "outgoing", new Protocol.CipherState(0x3810_1642));

            byte[] request = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(20).c(3).bytes();
            BattleSpellCatalog.NaturesBlessingSpell spell =
                    BattleSpellCatalog.naturesBlessingSpell(
                            BattleSpellCatalog.NATURES_BLESSING);
            Class<?>[] fixtureSignature = {
                    byte[].class,
                    BattleSpellCatalog.NaturesBlessingSpell.class,
                    int.class
            };

            // Sharing a room/map is insufficient without classic party membership.
            invoke(caster, "useNaturesBlessingSpell", fixtureSignature,
                    request, spell, 1);
            assertEquals(20, field(target, "playerHp"));

            @SuppressWarnings("unchecked")
            Map<Integer, Long> cooldowns =
                    (Map<Integer, Long>) field(caster, "spellReadyNanos");
            cooldowns.clear();
            setField(caster, "playerHp", 20);
            setField(caster, "playerMp", 100);
            GameHub.ClassicPartyJoinResult joined = hub.joinClassicParty(
                    caster.sourceCharacterIdentity(),
                    target.sourceCharacterIdentity(), 8);
            assertEquals(GameHub.ClassicPartyJoinStatus.JOINED, joined.status());

            BattleWaterLifeStatus targetWaterLife =
                    (BattleWaterLifeStatus) field(target, "waterLifeStatus");
            targetWaterLife.activate(System.nanoTime(), 64);
            invoke(caster, "useNaturesBlessingSpell", fixtureSignature,
                    request, spell, 1);
            int intelligence = ((SourceEquipmentMagicStats) field(
                    caster, "sourceEquipmentMagicStats"))
                    .effectiveIntelligence(casterElf.intelligence());
            assertEquals(20 + spell.diceCount(intelligence) * 2,
                    field(target, "playerHp"));
            assertFalse(targetWaterLife.active(System.nanoTime()));
        }
    }

    @Test
    void nativeWindShotUsesObjectSelfTargetAndRoutesBattleAndWorld()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        Path stateFile = temporary.resolve("wind-shot-wire-state.properties");
        GameStateStore state = new GameStateStore(stateFile, true);
        GameHub hub = new GameHub(config);
        String persistedKey = null;
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("wind-shot-capture")), hub);
            initialize(session, baseline());
            CharacterSummary elf = character("WindShotElf", 2, 0x3810_1490L);
            setField(session, "account", "wind-shot-account");
            setField(session, "selectedCharacter", elf.name());
            setField(session, "inventoryCharacter", elf.name());
            setField(session, "characters", List.of(elf));
            setField(session, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(elf));
            setField(session, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.WIND_SHOT));

            hub.connect(session);
            hub.enterChannel(0, session);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Wind Shot", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, session);
            setPhase(session, "BATTLE");
            setField(session, "currentRoomId", roomId);
            setField(session, "playerHp", 100);
            setField(session, "playerMaxHp", 100);
            setField(session, "playerMp", 100);
            setField(session, "playerMaxMp", 100);

            int seed = 0x3810_1490;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            byte[] wrongTarget = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(18).c(4)
                    .d(session.playerObjectId() + 1).bytes();
            setField(session, "elfAttribute", 0);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, wrongTarget);
            assertEquals(280, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MESSAGE_CODE, 4), 1));
            assertEquals(100, field(session, "playerMp"));

            byte[] request = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(18).c(4)
                    .d(session.playerObjectId()).bytes();
            // Exact retained DB attr=0: Wind Shot has no ElfAttr cast gate.
            setField(session, "elfAttribute", 0);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 8)[5] & 0xFF);
            assertEquals(2246, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(85, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8), 1));
            assertEquals(724, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MESSAGE_CODE, 8), 1));
            BattleWindShotStatus status =
                    (BattleWindShotStatus) field(session, "windShotStatus");
            assertEquals(0, status.physicalHit(false, System.nanoTime()));
            assertEquals(6, status.physicalHit(true, System.nanoTime()));
            String stateKey = GameStateStore.sourceCharacter(
                    (String) field(session, "selectedClassicServerName"),
                    elf.name());
            persistedKey = stateKey;
            assertEquals(960, state.timedWindShotBuff(
                    stateKey).remainingSeconds());

            int worldMap = ClassicWorldMapCatalog.WORLD_TREE_MAP_ID;
            int worldX = ClassicWorldMapCatalog.WORLD_TREE_X;
            int worldY = ClassicWorldMapCatalog.WORLD_TREE_Y;
            hub.enterClassicWorld(session, worldMap, worldX, worldY, 5);
            setPhase(session, "WORLD");
            setField(session, "classicWorldMapId", worldMap);
            setField(session, "classicWorldInstanceId", -1);
            setField(session, "positionX", worldX);
            setField(session, "positionY", worldY);
            setField(session, "playerMp", 100);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 12)[5] & 0xFF);
            assertEquals(2246, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(85, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8), 1));
            assertEquals(724, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MESSAGE_CODE, 8), 1));
            assertEquals(6, status.physicalHit(true, System.nanoTime()));
        }
        assertTrue(persistedKey != null);
        assertTrue(new GameStateStore(stateFile, true)
                .timedWindShotBuff(persistedKey).active());
    }

    @Test
    void nativeFireBlessSelectorTargetsOnlySelfThenVisiblePartyMember()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        Path stateFile = temporary.resolve("fire-bless-wire-state.properties");
        GameStateStore state = new GameStateStore(stateFile, true);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 2, InetAddress.getByName("127.0.0.1"));
             Socket casterClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket casterServer = listener.accept();
             Socket targetClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket targetServer = listener.accept()) {
            casterClient.setSoTimeout(1_000);
            targetClient.setSoTimeout(1_000);
            ClientSession caster = new ClientSession(
                    casterServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("fire-bless-caster-capture")), hub);
            ClientSession target = new ClientSession(
                    targetServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("fire-bless-target-capture")), hub);
            initialize(caster, baseline());
            initialize(target, baseline());
            CharacterSummary casterCharacter = character(
                    "FireBlessCaster", 2, 0x3810_1550L);
            CharacterSummary targetCharacter = character(
                    "FireBlessTarget", 2, 0x3810_1551L);
            setField(caster, "account", "fire-bless-caster-account");
            setField(caster, "selectedCharacter", casterCharacter.name());
            setField(caster, "inventoryCharacter", casterCharacter.name());
            setField(caster, "characters", List.of(casterCharacter));
            setField(caster, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(casterCharacter));
            setField(caster, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.FIRE_BLESS));
            setField(target, "account", "fire-bless-target-account");
            setField(target, "selectedCharacter", targetCharacter.name());
            setField(target, "inventoryCharacter", targetCharacter.name());
            setField(target, "characters", List.of(targetCharacter));
            setField(target, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(targetCharacter));

            hub.connect(caster);
            hub.connect(target);
            hub.enterChannel(0, caster);
            hub.enterChannel(0, target);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Fire Bless", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, caster);
            hub.joinRoom(roomId, target);
            setPhase(caster, "BATTLE");
            setPhase(target, "BATTLE");
            setField(caster, "currentRoomId", roomId);
            setField(target, "currentRoomId", roomId);
            setField(caster, "positionX", 32740);
            setField(caster, "positionY", 32798);
            setField(target, "positionX", 32741);
            setField(target, "positionY", 32798);
            setField(caster, "playerHp", 100);
            setField(caster, "playerMaxHp", 100);
            setField(caster, "playerMp", 100);
            setField(caster, "playerMaxMp", 100);
            setField(target, "playerHp", 100);
            setField(target, "playerMaxHp", 100);

            int casterSeed = 0x3810_1550;
            int targetSeed = 0x3810_1551;
            setField(caster, "outgoing", new Protocol.CipherState(casterSeed));
            setField(target, "outgoing", new Protocol.CipherState(targetSeed));
            Protocol.CipherState casterReceiver = new Protocol.CipherState(casterSeed);
            Protocol.CipherState targetReceiver = new Protocol.CipherState(targetSeed);
            byte[] request = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(19).c(2).bytes();

            // Same room and same map are insufficient: target_to=8 requires
            // actual classic-party membership.
            invoke(caster, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_ACTION, 8)[5] & 0xFF);
            assertEquals(2286, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(60, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 8), 1));
            assertEquals(723, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MESSAGE_CODE, 8), 1));
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_ACTION, 8);
            assertEquals(caster.playerObjectId(), unsignedInt(receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_EFFECT, 8), 1));
            BattleFireBlessStatus casterStatus = (BattleFireBlessStatus) field(
                    caster, "fireBlessStatus");
            BattleFireBlessStatus targetStatus = (BattleFireBlessStatus) field(
                    target, "fireBlessStatus");
            assertEquals(4, casterStatus.physicalDamage(false, System.nanoTime()));
            assertEquals(0, targetStatus.physicalDamage(false, System.nanoTime()));

            GameHub.ClassicPartyJoinResult joined = hub.joinClassicParty(
                    GameStateStore.sourceCharacter(
                            (String) field(caster, "selectedClassicServerName"),
                            casterCharacter.name()),
                    GameStateStore.sourceCharacter(
                            (String) field(target, "selectedClassicServerName"),
                            targetCharacter.name()), 8);
            assertEquals(GameHub.ClassicPartyJoinStatus.JOINED, joined.status());
            invoke(caster, "useSpell", new Class<?>[] {byte[].class}, request);
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_ACTION, 8);
            byte[] casterEffect = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 8);
            byte[] targetEffect = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 8);
            assertEquals(caster.playerObjectId(), unsignedInt(casterEffect, 1));
            assertEquals(target.playerObjectId(), unsignedInt(targetEffect, 1));
            assertEquals(20, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 8), 1));
            assertEquals(723, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MESSAGE_CODE, 8), 1));

            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_ACTION, 8);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_EFFECT, 8);
            byte[] targetOwnEffect = receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_EFFECT, 8);
            assertEquals(target.playerObjectId(), unsignedInt(targetOwnEffect, 1));
            assertEquals(2286, unsignedShort(targetOwnEffect, 5));
            assertEquals(723, unsignedShort(receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_MESSAGE_CODE, 8), 1));
            assertEquals(4, targetStatus.physicalDamage(false, System.nanoTime()));
            assertEquals(0, targetStatus.physicalDamage(true, System.nanoTime()));

            int worldMap = ClassicWorldMapCatalog.WORLD_TREE_MAP_ID;
            int worldX = ClassicWorldMapCatalog.WORLD_TREE_X;
            int worldY = ClassicWorldMapCatalog.WORLD_TREE_Y;
            hub.enterClassicWorld(caster, worldMap, worldX, worldY, 5);
            hub.enterClassicWorld(target, worldMap, worldX + 1, worldY, 5);
            setPhase(caster, "WORLD");
            setPhase(target, "WORLD");
            setField(caster, "classicWorldMapId", worldMap);
            setField(target, "classicWorldMapId", worldMap);
            setField(caster, "classicWorldInstanceId", -1);
            setField(target, "classicWorldInstanceId", -1);
            setField(caster, "positionX", worldX);
            setField(caster, "positionY", worldY);
            setField(target, "positionX", worldX + 1);
            setField(target, "positionY", worldY);
            setField(caster, "playerMp", 100);
            invoke(caster, "useSpell", new Class<?>[] {byte[].class}, request);
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_ACTION, 12);
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_EFFECT, 8);
            byte[] worldTargetEffect = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 8);
            assertEquals(target.playerObjectId(), unsignedInt(worldTargetEffect, 1));
            assertEquals(60, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 8), 1));
            assertEquals(723, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MESSAGE_CODE, 8), 1));
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_ACTION, 12);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_EFFECT, 8);
            assertEquals(target.playerObjectId(), unsignedInt(receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_EFFECT, 8), 1));
            assertEquals(723, unsignedShort(receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_MESSAGE_CODE, 8), 1));
            assertEquals(4, targetStatus.physicalDamage(false, System.nanoTime()));

            String casterKey = GameStateStore.sourceCharacter(
                    (String) field(caster, "selectedClassicServerName"),
                    casterCharacter.name());
            String targetKey = GameStateStore.sourceCharacter(
                    (String) field(target, "selectedClassicServerName"),
                    targetCharacter.name());
            assertEquals(960, state.timedFireBlessBuff(casterKey).remainingSeconds());
            assertEquals(960, state.timedFireBlessBuff(targetKey).remainingSeconds());
        }
    }

    @Test
    void nativeStormEyeSelectorTargetsOnlySelfThenVisiblePartyMember()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        Path stateFile = temporary.resolve("storm-eye-wire-state.properties");
        GameStateStore state = new GameStateStore(stateFile, true);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 2, InetAddress.getByName("127.0.0.1"));
             Socket casterClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket casterServer = listener.accept();
             Socket targetClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket targetServer = listener.accept()) {
            casterClient.setSoTimeout(1_000);
            targetClient.setSoTimeout(1_000);
            ClientSession caster = new ClientSession(
                    casterServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("storm-eye-caster-capture")), hub);
            ClientSession target = new ClientSession(
                    targetServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("storm-eye-target-capture")), hub);
            initialize(caster, baseline());
            initialize(target, baseline());
            CharacterSummary casterCharacter = character(
                    "StormEyeCaster", 2, 0x3810_1560L);
            CharacterSummary targetCharacter = character(
                    "StormEyeTarget", 2, 0x3810_1561L);
            setField(caster, "account", "storm-eye-caster-account");
            setField(caster, "selectedCharacter", casterCharacter.name());
            setField(caster, "inventoryCharacter", casterCharacter.name());
            setField(caster, "characters", List.of(casterCharacter));
            setField(caster, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(casterCharacter));
            setField(caster, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.STORM_EYE));
            setField(target, "account", "storm-eye-target-account");
            setField(target, "selectedCharacter", targetCharacter.name());
            setField(target, "inventoryCharacter", targetCharacter.name());
            setField(target, "characters", List.of(targetCharacter));
            setField(target, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(targetCharacter));

            hub.connect(caster);
            hub.connect(target);
            hub.enterChannel(0, caster);
            hub.enterChannel(0, target);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Storm Eye", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, caster);
            hub.joinRoom(roomId, target);
            setPhase(caster, "BATTLE");
            setPhase(target, "BATTLE");
            setField(caster, "currentRoomId", roomId);
            setField(target, "currentRoomId", roomId);
            setField(caster, "positionX", 32740);
            setField(caster, "positionY", 32798);
            setField(target, "positionX", 32741);
            setField(target, "positionY", 32798);
            setField(caster, "playerHp", 100);
            setField(caster, "playerMaxHp", 100);
            setField(caster, "playerMp", 100);
            setField(caster, "playerMaxMp", 100);
            setField(target, "playerHp", 100);
            setField(target, "playerMaxHp", 100);

            int casterSeed = 0x3810_1560;
            int targetSeed = 0x3810_1561;
            setField(caster, "outgoing", new Protocol.CipherState(casterSeed));
            setField(target, "outgoing", new Protocol.CipherState(targetSeed));
            Protocol.CipherState casterReceiver = new Protocol.CipherState(casterSeed);
            Protocol.CipherState targetReceiver = new Protocol.CipherState(targetSeed);
            byte[] request = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(19).c(3).bytes();

            invoke(caster, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_ACTION, 8)[5] & 0xFF);
            assertEquals(2288, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(60, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 8), 1));
            assertEquals(724, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MESSAGE_CODE, 8), 1));
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_ACTION, 8);
            assertEquals(caster.playerObjectId(), unsignedInt(receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_EFFECT, 8), 1));
            BattleStormEyeStatus casterStatus = (BattleStormEyeStatus) field(
                    caster, "stormEyeStatus");
            BattleStormEyeStatus targetStatus = (BattleStormEyeStatus) field(
                    target, "stormEyeStatus");
            assertEquals(2, casterStatus.physicalHit(true, System.nanoTime()));
            assertEquals(3, casterStatus.physicalDamage(true, System.nanoTime()));
            assertFalse(targetStatus.snapshot(System.nanoTime()).active());

            GameHub.ClassicPartyJoinResult joined = hub.joinClassicParty(
                    GameStateStore.sourceCharacter(
                            (String) field(caster, "selectedClassicServerName"),
                            casterCharacter.name()),
                    GameStateStore.sourceCharacter(
                            (String) field(target, "selectedClassicServerName"),
                            targetCharacter.name()), 8);
            assertEquals(GameHub.ClassicPartyJoinStatus.JOINED, joined.status());
            invoke(caster, "useSpell", new Class<?>[] {byte[].class}, request);
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_ACTION, 8);
            byte[] casterEffect = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 8);
            byte[] targetEffect = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 8);
            assertEquals(caster.playerObjectId(), unsignedInt(casterEffect, 1));
            assertEquals(target.playerObjectId(), unsignedInt(targetEffect, 1));
            assertEquals(20, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 8), 1));
            assertEquals(724, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MESSAGE_CODE, 8), 1));

            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_ACTION, 8);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_EFFECT, 8);
            byte[] targetOwnEffect = receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_EFFECT, 8);
            assertEquals(target.playerObjectId(), unsignedInt(targetOwnEffect, 1));
            assertEquals(2288, unsignedShort(targetOwnEffect, 5));
            assertEquals(724, unsignedShort(receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_MESSAGE_CODE, 8), 1));
            assertEquals(0, targetStatus.physicalHit(false, System.nanoTime()));
            assertEquals(0, targetStatus.physicalDamage(false, System.nanoTime()));
            assertEquals(2, targetStatus.physicalHit(true, System.nanoTime()));
            assertEquals(3, targetStatus.physicalDamage(true, System.nanoTime()));

            int worldMap = ClassicWorldMapCatalog.WORLD_TREE_MAP_ID;
            int worldX = ClassicWorldMapCatalog.WORLD_TREE_X;
            int worldY = ClassicWorldMapCatalog.WORLD_TREE_Y;
            hub.enterClassicWorld(caster, worldMap, worldX, worldY, 5);
            hub.enterClassicWorld(target, worldMap, worldX + 1, worldY, 5);
            setPhase(caster, "WORLD");
            setPhase(target, "WORLD");
            setField(caster, "classicWorldMapId", worldMap);
            setField(target, "classicWorldMapId", worldMap);
            setField(caster, "classicWorldInstanceId", -1);
            setField(target, "classicWorldInstanceId", -1);
            setField(caster, "positionX", worldX);
            setField(caster, "positionY", worldY);
            setField(target, "positionX", worldX + 1);
            setField(target, "positionY", worldY);
            setField(caster, "playerMp", 100);
            invoke(caster, "useSpell", new Class<?>[] {byte[].class}, request);
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_ACTION, 12);
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_EFFECT, 8);
            byte[] worldTargetEffect = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 8);
            assertEquals(target.playerObjectId(), unsignedInt(worldTargetEffect, 1));
            assertEquals(60, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 8), 1));
            assertEquals(724, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MESSAGE_CODE, 8), 1));
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_ACTION, 12);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_EFFECT, 8);
            assertEquals(target.playerObjectId(), unsignedInt(receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_EFFECT, 8), 1));
            assertEquals(724, unsignedShort(receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_MESSAGE_CODE, 8), 1));

            // Exercise the third physical consumer over the real encrypted
            // WORLD-monster route.  A deliberately large test-only lease is
            // used as a visible marker: the emitted ranged S_ATTACK damage
            // cannot reach this range through the retained bow/arrow alone.
            RuntimeInventory rangedInventory = new RuntimeInventory();
            rangedInventory.addDistinctState(
                    InventoryCatalog.require(InventoryCatalog.CKEW_ELF_BOW_REWARD),
                    new RuntimeInventory.PersistentItem(
                            0x7000_1560L, 1, 0, 0, true, 1,
                            false, true));
            rangedInventory.addDistinctState(
                    InventoryCatalog.require(InventoryCatalog.ARROW),
                    new RuntimeInventory.PersistentItem(
                            0x7000_1561L, 3, 0, 0, true, 1));
            setField(caster, "inventory", rangedInventory);
            setField(caster, "sourcePhysicalGrowthStats",
                    SourcePhysicalGrowthStats.classic381());
            setField(caster, "playerClassicLevel", 50);
            casterStatus.activate(System.nanoTime(), 960, 127, 127);
            long orcObjectId = 0x6500_1300L;
            ClassicWorldMonsterPopulation.Snapshot orc = hub.classicWorldMonster(
                    worldMap, -1, orcObjectId);
            assertTrue(orc != null && orc.alive());
            invoke(caster, "handle", new Class<?>[] {byte[].class},
                    new Protocol.Writer().c(Protocol.C_FAR_ATTACK)
                            .d(orcObjectId).h(orc.x()).h(orc.y()).bytes());
            Protocol.Reader worldAttack = new Protocol.Reader(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_ATTACK, 16));
            assertEquals(Protocol.S_ATTACK, worldAttack.c());
            worldAttack.c();
            assertEquals(caster.playerObjectId(), worldAttack.d());
            assertEquals(orcObjectId, worldAttack.d());
            assertTrue(worldAttack.c() >= 127,
                    "WORLD-monster S_ATTACK must consume Storm Eye damage");

            String casterKey = GameStateStore.sourceCharacter(
                    (String) field(caster, "selectedClassicServerName"),
                    casterCharacter.name());
            String targetKey = GameStateStore.sourceCharacter(
                    (String) field(target, "selectedClassicServerName"),
                    targetCharacter.name());
            assertEquals(960, state.timedStormEyeBuff(casterKey).remainingSeconds());
            assertEquals(960, state.timedStormEyeBuff(targetKey).remainingSeconds());
        }
    }

    @Test
    void nativeNaturesTouchObjectTargetWorksInBattleAndWorldAndConsumesWaterLife()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        Path stateFile = temporary.resolve("natures-touch-wire-state.properties");
        GameStateStore state = new GameStateStore(stateFile, true);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 2, InetAddress.getByName("127.0.0.1"));
             Socket casterClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket casterServer = listener.accept();
             Socket targetClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket targetServer = listener.accept()) {
            casterClient.setSoTimeout(1_000);
            targetClient.setSoTimeout(1_000);
            ClientSession caster = new ClientSession(
                    casterServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("natures-touch-caster-capture")), hub);
            ClientSession target = new ClientSession(
                    targetServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("natures-touch-target-capture")), hub);
            initialize(caster, baseline());
            initialize(target, baseline());
            CharacterSummary casterCharacter = character(
                    "NaturesTouchCaster", 2, 0x3810_1580L);
            CharacterSummary targetCharacter = character(
                    "NaturesTouchTarget", 2, 0x3810_1581L);
            setField(caster, "account", "natures-touch-caster-account");
            setField(caster, "selectedCharacter", casterCharacter.name());
            setField(caster, "inventoryCharacter", casterCharacter.name());
            setField(caster, "characters", List.of(casterCharacter));
            setField(caster, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(casterCharacter));
            setField(caster, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.NATURES_TOUCH));
            setField(target, "account", "natures-touch-target-account");
            setField(target, "selectedCharacter", targetCharacter.name());
            setField(target, "inventoryCharacter", targetCharacter.name());
            setField(target, "characters", List.of(targetCharacter));
            setField(target, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(targetCharacter));

            hub.connect(caster);
            hub.connect(target);
            hub.enterChannel(0, caster);
            hub.enterChannel(0, target);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Nature's Touch", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, caster);
            hub.joinRoom(roomId, target);
            setPhase(caster, "BATTLE");
            setPhase(target, "BATTLE");
            setField(caster, "currentRoomId", roomId);
            setField(target, "currentRoomId", roomId);
            setField(caster, "positionX", 32740);
            setField(caster, "positionY", 32798);
            setField(target, "positionX", 32741);
            setField(target, "positionY", 32798);
            setField(caster, "playerHp", 100);
            setField(caster, "playerMaxHp", 100);
            setField(caster, "playerMp", 100);
            setField(caster, "playerMaxMp", 100);
            setField(target, "playerHp", 50);
            setField(target, "playerMaxHp", 100);

            int casterSeed = 0x3810_1580;
            int targetSeed = 0x3810_1581;
            setField(caster, "outgoing", new Protocol.CipherState(casterSeed));
            setField(target, "outgoing", new Protocol.CipherState(targetSeed));
            Protocol.CipherState casterReceiver = new Protocol.CipherState(casterSeed);
            Protocol.CipherState targetReceiver = new Protocol.CipherState(targetSeed);
            BattleWaterLifeStatus waterLife = (BattleWaterLifeStatus) field(
                    target, "waterLifeStatus");
            waterLife.activate(System.nanoTime(), 64);
            byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(19).c(5).d(target.playerObjectId()).bytes();

            invoke(caster, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_ACTION, 8)[5] & 0xFF);
            byte[] battleEffect = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 8);
            assertEquals(target.playerObjectId(), unsignedInt(battleEffect, 1));
            assertEquals(2243, unsignedShort(battleEffect, 5));
            assertEquals(80, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 8), 1));
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_ACTION, 8);
            assertEquals(target.playerObjectId(), unsignedInt(receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_EFFECT, 8), 1));
            BattleNaturesTouchStatus targetStatus =
                    (BattleNaturesTouchStatus) field(target, "naturesTouchStatus");
            assertEquals(15, targetStatus.hpRegen(System.nanoTime()));
            assertFalse(waterLife.active(System.nanoTime()));

            int worldMap = ClassicWorldMapCatalog.WORLD_TREE_MAP_ID;
            int worldX = ClassicWorldMapCatalog.WORLD_TREE_X;
            int worldY = ClassicWorldMapCatalog.WORLD_TREE_Y;
            hub.enterClassicWorld(caster, worldMap, worldX, worldY, 5);
            hub.enterClassicWorld(target, worldMap, worldX + 1, worldY, 5);
            setPhase(caster, "WORLD");
            setPhase(target, "WORLD");
            setField(caster, "classicWorldMapId", worldMap);
            setField(target, "classicWorldMapId", worldMap);
            setField(caster, "classicWorldInstanceId", -1);
            setField(target, "classicWorldInstanceId", -1);
            setField(caster, "positionX", worldX);
            setField(caster, "positionY", worldY);
            setField(target, "positionX", worldX + 1);
            setField(target, "positionY", worldY);
            setField(caster, "playerMp", 100);
            invoke(caster, "useSpell", new Class<?>[] {byte[].class}, request);
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_ACTION, 8);
            assertEquals(target.playerObjectId(), unsignedInt(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 8), 1));
            assertEquals(80, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 8), 1));
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_ACTION, 8);
            assertEquals(2243, unsignedShort(receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(15, targetStatus.hpRegen(System.nanoTime()));
            assertFalse(LocalResources.readString(stateFile)
                    .contains("natures-touch"));
        }
    }

    @Test
    void nativeEarthBlessPartyAcCoexistsWithEarthSkinInBattleAndWorld()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        Path stateFile = temporary.resolve("earth-bless-wire-state.properties");
        GameStateStore state = new GameStateStore(stateFile, true);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 2, InetAddress.getByName("127.0.0.1"));
             Socket casterClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket casterServer = listener.accept();
             Socket targetClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket targetServer = listener.accept()) {
            casterClient.setSoTimeout(1_000);
            targetClient.setSoTimeout(1_000);
            ClientSession caster = new ClientSession(
                    casterServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("earth-bless-caster-capture")), hub);
            ClientSession target = new ClientSession(
                    targetServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("earth-bless-target-capture")), hub);
            initialize(caster, baseline());
            initialize(target, baseline());
            CharacterSummary casterCharacter = character(
                    "EarthBlessCaster", 2, 0x3810_1590L);
            CharacterSummary targetCharacter = character(
                    "EarthBlessTarget", 2, 0x3810_1591L);
            setField(caster, "account", "earth-bless-caster-account");
            setField(caster, "selectedCharacter", casterCharacter.name());
            setField(caster, "inventoryCharacter", casterCharacter.name());
            setField(caster, "characters", List.of(casterCharacter));
            setField(caster, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(casterCharacter));
            setField(caster, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.EARTH_BLESS));
            setField(target, "account", "earth-bless-target-account");
            setField(target, "selectedCharacter", targetCharacter.name());
            setField(target, "inventoryCharacter", targetCharacter.name());
            setField(target, "characters", List.of(targetCharacter));
            setField(target, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(targetCharacter));

            hub.connect(caster);
            hub.connect(target);
            hub.enterChannel(0, caster);
            hub.enterChannel(0, target);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Earth Bless", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, caster);
            hub.joinRoom(roomId, target);
            setPhase(caster, "BATTLE");
            setPhase(target, "BATTLE");
            setField(caster, "currentRoomId", roomId);
            setField(target, "currentRoomId", roomId);
            setField(caster, "positionX", 32740);
            setField(caster, "positionY", 32798);
            setField(target, "positionX", 32741);
            setField(target, "positionY", 32798);
            setField(caster, "playerHp", 100);
            setField(caster, "playerMaxHp", 100);
            setField(caster, "playerMp", 100);
            setField(caster, "playerMaxMp", 100);
            setField(target, "playerHp", 100);
            setField(target, "playerMaxHp", 100);

            int casterSeed = 0x3810_1590;
            int targetSeed = 0x3810_1591;
            setField(caster, "outgoing", new Protocol.CipherState(casterSeed));
            setField(target, "outgoing", new Protocol.CipherState(targetSeed));
            Protocol.CipherState casterReceiver = new Protocol.CipherState(casterSeed);
            Protocol.CipherState targetReceiver = new Protocol.CipherState(targetSeed);
            byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(19).c(6).bytes();

            int casterBaseAc = caster.battleArmorClass();
            invoke(caster, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_ACTION, 8)[5] & 0xFF);
            assertEquals(2287, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(60, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 8), 1));
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_AC, 8);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_ACTION, 8);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_EFFECT, 8);
            assertEquals(casterBaseAc - 7, caster.battleArmorClass());
            BattleEarthBlessStatus targetEarth = (BattleEarthBlessStatus) field(
                    target, "earthBlessStatus");
            assertFalse(targetEarth.snapshot(System.nanoTime()).active());

            GameHub.ClassicPartyJoinResult joined = hub.joinClassicParty(
                    GameStateStore.sourceCharacter(
                            (String) field(caster, "selectedClassicServerName"),
                            casterCharacter.name()),
                    GameStateStore.sourceCharacter(
                            (String) field(target, "selectedClassicServerName"),
                            targetCharacter.name()), 8);
            assertEquals(GameHub.ClassicPartyJoinStatus.JOINED, joined.status());
            BattleAbilityStatus targetAbility = (BattleAbilityStatus) field(
                    target, "abilityStatus");
            BattleSpellCatalog.ShieldSpell earthSkin =
                    BattleSpellCatalog.shieldSpell(BattleSpellCatalog.EARTH_SKIN);
            targetAbility.activateArmorProtection(earthSkin.skillId(),
                    System.nanoTime(), 2_000,
                    earthSkin.armorClassModifier());
            int targetEarthSkinAc = target.battleArmorClass();
            invoke(caster, "useSpell", new Class<?>[] {byte[].class}, request);
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_ACTION, 8);
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_EFFECT, 8);
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_EFFECT, 8);
            assertEquals(20, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 8), 1));
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_AC, 8);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_ACTION, 8);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_EFFECT, 8);
            assertEquals(target.playerObjectId(), unsignedInt(receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_EFFECT, 8), 1));
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_AC, 8);
            assertEquals(-7, targetEarth.armorClass(System.nanoTime()));
            assertEquals(targetEarthSkinAc - 7, target.battleArmorClass(),
                    "selected coexist mode must preserve Earth Skin AC");

            int worldMap = ClassicWorldMapCatalog.WORLD_TREE_MAP_ID;
            int worldX = ClassicWorldMapCatalog.WORLD_TREE_X;
            int worldY = ClassicWorldMapCatalog.WORLD_TREE_Y;
            hub.enterClassicWorld(caster, worldMap, worldX, worldY, 5);
            hub.enterClassicWorld(target, worldMap, worldX + 1, worldY, 5);
            setPhase(caster, "WORLD");
            setPhase(target, "WORLD");
            setField(caster, "classicWorldMapId", worldMap);
            setField(target, "classicWorldMapId", worldMap);
            setField(caster, "classicWorldInstanceId", -1);
            setField(target, "classicWorldInstanceId", -1);
            setField(caster, "positionX", worldX);
            setField(caster, "positionY", worldY);
            setField(target, "positionX", worldX + 1);
            setField(target, "positionY", worldY);
            setField(caster, "playerMp", 100);
            invoke(caster, "useSpell", new Class<?>[] {byte[].class}, request);
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_ACTION, 8);
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_EFFECT, 8);
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_EFFECT, 8);
            assertEquals(60, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 8), 1));
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_AC, 8);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_ACTION, 8);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_EFFECT, 8);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_EFFECT, 8);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_AC, 8);

            String casterKey = GameStateStore.sourceCharacter(
                    (String) field(caster, "selectedClassicServerName"),
                    casterCharacter.name());
            String targetKey = GameStateStore.sourceCharacter(
                    (String) field(target, "selectedClassicServerName"),
                    targetCharacter.name());
            assertEquals(960,
                    state.timedEarthBlessBuff(casterKey).remainingSeconds());
            assertEquals(960,
                    state.timedEarthBlessBuff(targetKey).remainingSeconds());

            long expiryNow = System.nanoTime()
                    + java.util.concurrent.TimeUnit.SECONDS.toNanos(961);
            invoke(caster, "serverExpireTimedStatuses",
                    new Class<?>[] {long.class}, expiryNow);
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_AC, 8);
            assertEquals(725, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MESSAGE_CODE, 8), 1));
            invoke(target, "serverExpireTimedStatuses",
                    new Class<?>[] {long.class}, expiryNow);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_AC, 8);
            assertEquals(725, unsignedShort(receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_MESSAGE_CODE, 8), 1));
            assertFalse(state.timedEarthBlessBuff(casterKey).active());
            assertFalse(state.timedEarthBlessBuff(targetKey).active());
            assertEquals(BattleSpellCatalog.EARTH_SKIN,
                    targetAbility.armorProtection(expiryNow).skillId());
        }
    }

    @Test
    void nativeAquaProtectSelfAndPcTargetAddFiveRangedErInBattleAndWorld()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        Path stateFile = temporary.resolve("aqua-protect-wire-state.properties");
        GameStateStore state = new GameStateStore(stateFile, true);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 2, InetAddress.getByName("127.0.0.1"));
             Socket casterClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket casterServer = listener.accept();
             Socket targetClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket targetServer = listener.accept()) {
            casterClient.setSoTimeout(1_000);
            targetClient.setSoTimeout(1_000);
            ClientSession caster = new ClientSession(
                    casterServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("aqua-protect-caster-capture")), hub);
            ClientSession target = new ClientSession(
                    targetServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("aqua-protect-target-capture")), hub);
            initialize(caster, baseline());
            initialize(target, baseline());
            CharacterSummary casterCharacter = character(
                    "AquaProtectCaster", 2, 0x3810_1600L);
            CharacterSummary targetCharacter = character(
                    "AquaProtectTarget", 2, 0x3810_1601L);
            setField(caster, "account", "aqua-protect-caster-account");
            setField(caster, "selectedCharacter", casterCharacter.name());
            setField(caster, "inventoryCharacter", casterCharacter.name());
            setField(caster, "characters", List.of(casterCharacter));
            setField(caster, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(casterCharacter));
            setField(caster, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.AQUA_PROTECTER));
            setField(target, "account", "aqua-protect-target-account");
            setField(target, "selectedCharacter", targetCharacter.name());
            setField(target, "inventoryCharacter", targetCharacter.name());
            setField(target, "characters", List.of(targetCharacter));
            setField(target, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(targetCharacter));

            hub.connect(caster);
            hub.connect(target);
            hub.enterChannel(0, caster);
            hub.enterChannel(0, target);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Aqua Protecter", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, caster);
            hub.joinRoom(roomId, target);
            setPhase(caster, "BATTLE");
            setPhase(target, "BATTLE");
            setField(caster, "currentRoomId", roomId);
            setField(target, "currentRoomId", roomId);
            setField(caster, "positionX", 32740);
            setField(caster, "positionY", 32798);
            setField(target, "positionX", 32741);
            setField(target, "positionY", 32798);
            setField(caster, "playerHp", 100);
            setField(caster, "playerMaxHp", 100);
            setField(caster, "playerMp", 100);
            setField(caster, "playerMaxMp", 100);
            setField(target, "playerHp", 100);
            setField(target, "playerMaxHp", 100);

            int casterSeed = 0x3810_1600;
            int targetSeed = 0x3810_1601;
            setField(caster, "outgoing", new Protocol.CipherState(casterSeed));
            setField(target, "outgoing", new Protocol.CipherState(targetSeed));
            Protocol.CipherState casterReceiver = new Protocol.CipherState(casterSeed);
            Protocol.CipherState targetReceiver = new Protocol.CipherState(targetSeed);
            BattleAquaProtectStatus casterStatus =
                    (BattleAquaProtectStatus) field(caster, "aquaProtectStatus");
            BattleAquaProtectStatus targetStatus =
                    (BattleAquaProtectStatus) field(target, "aquaProtectStatus");

            // target_to=PC admits the caster itself.
            byte[] selfRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(19).c(7).d(caster.playerObjectId()).bytes();
            invoke(caster, "useSpell", new Class<?>[] {byte[].class}, selfRequest);
            assertEquals(19, receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_ACTION, 8)[5] & 0xFF);
            assertEquals(5829, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(70, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 8), 1));
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_ACTION, 8);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_EFFECT, 8);
            assertEquals(5, casterStatus.evasionRate(System.nanoTime()));

            // The raw retained row pins target_to=PC.  A different visible PC
            // is therefore legal even when neither character has a clan.
            byte[] targetRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(19).c(7).d(target.playerObjectId()).bytes();
            assertFalse(targetStatus.snapshot(System.nanoTime()).active());

            String casterIdentity = GameStateStore.sourceCharacter(
                    (String) field(caster, "selectedClassicServerName"),
                    casterCharacter.name());
            String targetIdentity = GameStateStore.sourceCharacter(
                    (String) field(target, "selectedClassicServerName"),
                    targetCharacter.name());
            assertFalse(hub.sameClan(casterIdentity, targetIdentity));

            BattleAbilityStatus targetAbility =
                    (BattleAbilityStatus) field(target, "abilityStatus");
            targetAbility.activateSolidCarriage(System.nanoTime(), 64, 15);
            int solidOnlyEr = (int) invokeResult(target,
                    "effectiveRangedEvasionRate",
                    new Class<?>[] {long.class}, System.nanoTime());
            setField(caster, "playerMp", 100);
            invoke(caster, "useSpell", new Class<?>[] {byte[].class}, targetRequest);
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_ACTION, 8);
            byte[] targetEffect = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 8);
            assertEquals(target.playerObjectId(), unsignedInt(targetEffect, 1));
            assertEquals(5829, unsignedShort(targetEffect, 5));
            assertEquals(70, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 8), 1));
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_ACTION, 8);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_EFFECT, 8);
            int aquaAndSolidEr = (int) invokeResult(target,
                    "effectiveRangedEvasionRate",
                    new Class<?>[] {long.class}, System.nanoTime());
            assertEquals(solidOnlyEr + 5, aquaAndSolidEr);
            int fixedRoll = solidOnlyEr + 1;
            assertTrue(BattleRangedEvasion.hitsAfterEvasion(
                    solidOnlyEr, fixedRoll));
            assertFalse(BattleRangedEvasion.hitsAfterEvasion(
                    aquaAndSolidEr, fixedRoll));

            int worldMap = ClassicWorldMapCatalog.WORLD_TREE_MAP_ID;
            int worldX = ClassicWorldMapCatalog.WORLD_TREE_X;
            int worldY = ClassicWorldMapCatalog.WORLD_TREE_Y;
            hub.enterClassicWorld(caster, worldMap, worldX, worldY, 5);
            hub.enterClassicWorld(target, worldMap, worldX + 1, worldY, 5);
            setPhase(caster, "WORLD");
            setPhase(target, "WORLD");
            setField(caster, "classicWorldMapId", worldMap);
            setField(target, "classicWorldMapId", worldMap);
            setField(caster, "classicWorldInstanceId", -1);
            setField(target, "classicWorldInstanceId", -1);
            setField(caster, "positionX", worldX);
            setField(caster, "positionY", worldY);
            setField(target, "positionX", worldX + 1);
            setField(target, "positionY", worldY);
            setField(caster, "playerMp", 100);
            invoke(caster, "useSpell", new Class<?>[] {byte[].class}, targetRequest);
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_ACTION, 8);
            assertEquals(5829, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(70, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 8), 1));
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_ACTION, 8);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_EFFECT, 8);
            assertEquals(5, targetStatus.evasionRate(System.nanoTime()));
            assertFalse(LocalResources.readString(stateFile)
                    .contains("aqua-protect"));
        }
    }

    @Test
    void nativeAreaSilenceHitsCasterAndVisiblePcWithoutConsumingCounterMagic()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        Path stateFile = temporary.resolve("area-silence-wire-state.properties");
        GameStateStore state = new GameStateStore(stateFile, true);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 2, InetAddress.getByName("127.0.0.1"));
             Socket casterClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket casterServer = listener.accept();
             Socket targetClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket targetServer = listener.accept()) {
            casterClient.setSoTimeout(1_000);
            targetClient.setSoTimeout(1_000);
            ClientSession caster = new ClientSession(
                    casterServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("area-silence-caster-capture")),
                    hub);
            ClientSession target = new ClientSession(
                    targetServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("area-silence-target-capture")),
                    hub);
            initialize(caster, baseline());
            initialize(target, baseline());
            CharacterSummary casterCharacter = character(
                    "AreaSilenceCaster", 2, 0x3810_1610L);
            CharacterSummary targetCharacter = character(
                    "AreaSilenceTarget", 2, 0x3810_1611L);
            setField(caster, "account", "area-silence-caster-account");
            setField(caster, "selectedCharacter", casterCharacter.name());
            setField(caster, "inventoryCharacter", casterCharacter.name());
            setField(caster, "characters", List.of(casterCharacter));
            setField(caster, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(casterCharacter));
            setField(caster, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.AREA_OF_SILENCE));
            setField(target, "account", "area-silence-target-account");
            setField(target, "selectedCharacter", targetCharacter.name());
            setField(target, "inventoryCharacter", targetCharacter.name());
            setField(target, "characters", List.of(targetCharacter));
            setField(target, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(targetCharacter));
            RuntimeInventory live = inventory(caster).copy();
            assertTrue(live.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 16));
            setField(caster, "inventory", live);

            hub.connect(caster);
            hub.connect(target);
            hub.enterChannel(0, caster);
            hub.enterChannel(0, target);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Area of Silence", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, caster);
            hub.joinRoom(roomId, target);
            setPhase(caster, "BATTLE");
            setPhase(target, "BATTLE");
            setField(caster, "currentRoomId", roomId);
            setField(target, "currentRoomId", roomId);
            setField(caster, "positionX", 32740);
            setField(caster, "positionY", 32798);
            setField(target, "positionX", 32741);
            setField(target, "positionY", 32798);
            setField(caster, "playerHp", 100);
            setField(caster, "playerMaxHp", 100);
            setField(caster, "playerMp", 100);
            setField(caster, "playerMaxMp", 100);
            setField(target, "playerHp", 100);
            setField(target, "playerMaxHp", 100);

            int casterSeed = 0x3810_1610;
            int targetSeed = 0x3810_1611;
            setField(caster, "outgoing", new Protocol.CipherState(casterSeed));
            setField(target, "outgoing", new Protocol.CipherState(targetSeed));
            Protocol.CipherState casterReceiver = new Protocol.CipherState(casterSeed);
            Protocol.CipherState targetReceiver = new Protocol.CipherState(targetSeed);
            BattleTimedStatus casterTimed =
                    (BattleTimedStatus) field(caster, "timedStatus");
            BattleTimedStatus targetTimed =
                    (BattleTimedStatus) field(target, "timedStatus");
            targetTimed.activateCounterMagic(System.nanoTime(), 64);

            byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(20).c(0).bytes();
            invoke(caster, "useAreaSilenceSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.AreaSilenceSpell.class, int.class},
                    request, BattleSpellCatalog.areaSilenceSpell(
                            BattleSpellCatalog.AREA_OF_SILENCE), 1);
            assertEquals(19, receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_ACTION, 10)[5] & 0xFF);
            byte[] casterEffect = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 10);
            byte[] targetEffect = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 10);
            assertEquals(2241, unsignedShort(casterEffect, 5));
            assertEquals(2241, unsignedShort(targetEffect, 5));
            assertEquals(60, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 10), 1));
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_ACTION, 10);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_EFFECT, 10);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_EFFECT, 10);
            long now = System.nanoTime();
            assertTrue(casterTimed.areaSilenceActive(now));
            assertTrue(targetTimed.areaSilenceActive(now));
            assertTrue(targetTimed.counterMagicActive(now));
            assertEquals(8, inventory(caster).catalogCounts()
                    .getOrDefault(InventoryCatalog.SPIRIT_CRYSTAL, 0));

            // The combined silence gate rejects another cast before cooldown,
            // resource or target processing.
            invoke(caster, "useAreaSilenceSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.AreaSilenceSpell.class, int.class},
                    request, BattleSpellCatalog.areaSilenceSpell(
                            BattleSpellCatalog.AREA_OF_SILENCE), 1);
            assertEquals(60, field(caster, "playerMp"));
            assertEquals(8, inventory(caster).catalogCounts()
                    .getOrDefault(InventoryCatalog.SPIRIT_CRYSTAL, 0));

            casterTimed.cancelMagicEffects();
            targetTimed.cancelMagicEffects();
            @SuppressWarnings("unchecked")
            java.util.Map<Integer, Long> ready =
                    (java.util.Map<Integer, Long>) field(caster, "spellReadyNanos");
            ready.clear();
            RuntimeInventory refill = inventory(caster).copy();
            assertTrue(refill.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 8));
            setField(caster, "inventory", refill);
            setField(caster, "playerMp", 100);
            int worldMap = 4;
            int worldX = 33313;
            int worldY = 32339;
            assertFalse(ClassicWorldMapCatalog.forMap(worldMap)
                    .isSafetyZone(worldX, worldY));
            assertFalse(ClassicWorldMapCatalog.forMap(worldMap)
                    .isSafetyZone(worldX - 1, worldY));
            hub.enterClassicWorld(caster, worldMap, worldX, worldY, 5);
            hub.enterClassicWorld(target, worldMap, worldX - 1, worldY, 5);
            setPhase(caster, "WORLD");
            setPhase(target, "WORLD");
            setField(caster, "classicWorldMapId", worldMap);
            setField(target, "classicWorldMapId", worldMap);
            setField(caster, "classicWorldInstanceId", -1);
            setField(target, "classicWorldInstanceId", -1);
            setField(caster, "positionX", worldX);
            setField(caster, "positionY", worldY);
            setField(target, "positionX", worldX - 1);
            setField(target, "positionY", worldY);
            assertEquals(2, hub.classicWorldMembers(worldMap, -1).size());
            assertEquals("WORLD", field(caster, "phase").toString());
            assertEquals("WORLD", field(target, "phase").toString());
            assertEquals(-1, field(caster, "classicWorldInstanceId"));
            assertEquals(-1, field(target, "classicWorldInstanceId"));
            assertEquals(100, field(caster, "playerHp"));
            assertEquals(100, field(target, "playerHp"));
            assertTrue(BattleSpellCatalog.isInNativeScreen(
                    worldX, worldY, worldX - 1, worldY));
            invoke(caster, "useAreaSilenceSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.AreaSilenceSpell.class, int.class},
                    request, BattleSpellCatalog.areaSilenceSpell(
                            BattleSpellCatalog.AREA_OF_SILENCE), 1);
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_ACTION, 10);
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_EFFECT, 10);
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_EFFECT, 10);
            assertEquals(60, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 64), 1));
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_ACTION, 10);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_EFFECT, 10);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_EFFECT, 64);
            assertTrue(casterTimed.areaSilenceActive(System.nanoTime()));
            assertTrue(targetTimed.areaSilenceActive(System.nanoTime()));
            assertFalse(LocalResources.readString(stateFile)
                    .contains("area-silence"));
        }
    }

    @Test
    void nativeWindShackleUsesCccdhhHitsScreenAndCounterMagicThenThrottlesActions()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        Path stateFile = temporary.resolve("wind-shackle-wire-state.properties");
        GameStateStore state = new GameStateStore(stateFile, true);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 2, InetAddress.getByName("127.0.0.1"));
             Socket casterClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket casterServer = listener.accept();
             Socket targetClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket targetServer = listener.accept()) {
            casterClient.setSoTimeout(1_000);
            targetClient.setSoTimeout(1_000);
            ClientSession caster = new ClientSession(
                    casterServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("wind-shackle-caster-capture")),
                    hub);
            ClientSession target = new ClientSession(
                    targetServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("wind-shackle-target-capture")),
                    hub);
            initialize(caster, baseline());
            initialize(target, baseline());
            CharacterSummary casterCharacter = character(
                    "WindShackleCaster", 2, 0x3810_1670L);
            CharacterSummary targetCharacter = character(
                    "WindShackleTarget", 2, 0x3810_1671L);
            setField(caster, "account", "wind-shackle-caster-account");
            setField(caster, "selectedCharacter", casterCharacter.name());
            setField(caster, "inventoryCharacter", casterCharacter.name());
            setField(caster, "characters", List.of(casterCharacter));
            setField(caster, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(casterCharacter));
            setField(caster, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.WIND_SHACKLE));
            setField(caster, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_WIND);
            setField(target, "account", "wind-shackle-target-account");
            setField(target, "selectedCharacter", targetCharacter.name());
            setField(target, "inventoryCharacter", targetCharacter.name());
            setField(target, "characters", List.of(targetCharacter));
            setField(target, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(targetCharacter));
            RuntimeInventory live = inventory(caster).copy();
            assertTrue(live.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 2));
            setField(caster, "inventory", live);

            hub.connect(caster);
            hub.connect(target);
            hub.enterChannel(0, caster);
            hub.enterChannel(0, target);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Wind Shackle", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, caster);
            hub.joinRoom(roomId, target);
            setPhase(caster, "BATTLE");
            setPhase(target, "BATTLE");
            setField(caster, "currentRoomId", roomId);
            setField(target, "currentRoomId", roomId);
            setField(caster, "positionX", 32740);
            setField(caster, "positionY", 32798);
            setField(target, "positionX", 32741);
            setField(target, "positionY", 32798);
            setField(caster, "playerHp", 100);
            setField(caster, "playerMaxHp", 100);
            setField(caster, "playerMp", 100);
            setField(caster, "playerMaxMp", 100);
            setField(target, "playerHp", 100);
            setField(target, "playerMaxHp", 100);

            int casterSeed = 0x3810_1670;
            int targetSeed = 0x3810_1671;
            setField(caster, "outgoing", new Protocol.CipherState(casterSeed));
            setField(target, "outgoing", new Protocol.CipherState(targetSeed));
            Protocol.CipherState casterReceiver = new Protocol.CipherState(casterSeed);
            Protocol.CipherState targetReceiver = new Protocol.CipherState(targetSeed);
            BattleWindShackleStatus casterStatus =
                    (BattleWindShackleStatus) field(caster, "windShackleStatus");
            BattleWindShackleStatus targetStatus =
                    (BattleWindShackleStatus) field(target, "windShackleStatus");
            BattleTimedStatus targetTimed =
                    (BattleTimedStatus) field(target, "timedStatus");

            byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(20).c(6).d(target.playerObjectId())
                    .h(32741).h(32798).bytes();
            invoke(caster, "useWindShackleSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.WindShackleSpell.class, int.class},
                    request, BattleSpellCatalog.windShackleSpell(
                            BattleSpellCatalog.WIND_SHACKLE), 1);
            assertEquals(19, receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_ACTION, 16)[5] & 0xFF);
            assertEquals(1799, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 16), 5));
            assertEquals(1799, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 16), 5));
            assertEquals(50, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 16), 1));
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_ACTION, 16);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_EFFECT, 16);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_EFFECT, 16);
            byte[] happen = receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_MESSAGE_CODE, 16);
            assertEquals(1001, unsignedShort(happen, 1));
            assertTrue(casterStatus.snapshot(System.nanoTime()).active());
            assertTrue(targetStatus.snapshot(System.nanoTime()).active());
            assertEquals(1, inventory(caster).catalogCounts()
                    .getOrDefault(InventoryCatalog.SPIRIT_CRYSTAL, 0));
            assertEquals(Boolean.TRUE, invokeResult(caster,
                    "admitWindShackleAction",
                    new Class<?>[] {int.class, String.class}, 19, "test-spell"));
            assertEquals(Boolean.FALSE, invokeResult(caster,
                    "admitWindShackleAction",
                    new Class<?>[] {int.class, String.class}, 19, "test-spell"));

            casterStatus.cancel();
            targetStatus.cancel();
            @SuppressWarnings("unchecked")
            java.util.Map<Integer, Long> ready =
                    (java.util.Map<Integer, Long>) field(caster, "spellReadyNanos");
            ready.clear();
            setField(caster, "playerMp", 100);
            targetTimed.activateCounterMagic(System.nanoTime(), 64);
            invoke(caster, "useWindShackleSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.WindShackleSpell.class, int.class},
                    request, BattleSpellCatalog.windShackleSpell(
                            BattleSpellCatalog.WIND_SHACKLE), 1);
            assertTrue(casterStatus.snapshot(System.nanoTime()).active());
            assertFalse(targetStatus.snapshot(System.nanoTime()).active());
            assertFalse(targetTimed.counterMagicActive(System.nanoTime()));
            assertEquals(0, inventory(caster).catalogCounts()
                    .getOrDefault(InventoryCatalog.SPIRIT_CRYSTAL, 0));
            assertFalse(LocalResources.readString(stateFile)
                    .contains("wind-shackle"));
        }
    }

    @Test
    void nativeWindWalkUsesSelectorOnlyModeFourInBattleAndWorld()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        Path stateFile = temporary.resolve("wind-walk-wire-state.properties");
        GameStateStore state = new GameStateStore(stateFile, true);
        GameHub hub = new GameHub(config);
        String persistedKey = null;
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("wind-walk-capture")), hub);
            initialize(session, baseline());
            CharacterSummary elf = character("WindWalkElf", 2, 0x3810_1500L);
            setField(session, "account", "wind-walk-account");
            setField(session, "selectedCharacter", elf.name());
            setField(session, "inventoryCharacter", elf.name());
            setField(session, "characters", List.of(elf));
            setField(session, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(elf));
            setField(session, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.WIND_WALK));

            hub.connect(session);
            hub.enterChannel(0, session);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Wind Walk", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, session);
            setPhase(session, "BATTLE");
            setField(session, "currentRoomId", roomId);
            setField(session, "playerHp", 100);
            setField(session, "playerMaxHp", 100);
            setField(session, "playerMp", 100);
            setField(session, "playerMaxMp", 100);

            int seed = 0x3810_1500;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            byte[] request = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(18).c(5).bytes();
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 8)[5] & 0xFF);
            assertEquals(2247, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 8), 5));
            byte[] brave = receiveUntilOpcode(
                    client, receiver, Protocol.S_EMOTION, 8);
            assertEquals(BattleTimedStatus.BRAVE_HOLY_WALK, brave[5] & 0xFF);
            assertEquals(960, unsignedShort(brave, 6));
            assertEquals(85, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8), 1));
            BattleTimedStatus timed =
                    (BattleTimedStatus) field(session, "timedStatus");
            assertEquals(BattleTimedStatus.BRAVE_HOLY_WALK,
                    timed.brave(System.nanoTime()).mode());
            String stateKey = GameStateStore.sourceCharacter(
                    (String) field(session, "selectedClassicServerName"),
                    elf.name());
            persistedKey = stateKey;
            assertEquals(new GameStateStore.TimedMovementBuff(
                            BattleSpellCatalog.WIND_WALK, 960),
                    state.timedMovementBuff(stateKey));

            int worldMap = ClassicWorldMapCatalog.WORLD_TREE_MAP_ID;
            int worldX = ClassicWorldMapCatalog.WORLD_TREE_X;
            int worldY = ClassicWorldMapCatalog.WORLD_TREE_Y;
            hub.enterClassicWorld(session, worldMap, worldX, worldY, 5);
            setPhase(session, "WORLD");
            setField(session, "classicWorldMapId", worldMap);
            setField(session, "classicWorldInstanceId", -1);
            setField(session, "positionX", worldX);
            setField(session, "positionY", worldY);
            setField(session, "playerMp", 100);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 12)[5] & 0xFF);
            assertEquals(2247, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 8), 5));
            brave = receiveUntilOpcode(
                    client, receiver, Protocol.S_EMOTION, 8);
            assertEquals(BattleTimedStatus.BRAVE_HOLY_WALK, brave[5] & 0xFF);
            assertEquals(960, unsignedShort(brave, 6));
            assertEquals(85, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8), 1));
            assertEquals(BattleSpellCatalog.WIND_WALK,
                    field(session, "activeMovementBuffSkillId"));
        }
        assertTrue(persistedKey != null);
        assertTrue(new GameStateStore(stateFile, true)
                .timedMovementBuff(persistedKey).active());
    }

    @Test
    void nativeEarthSkinUsesCase150ObjectEnvelopeInBattleAndWorld()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        Path stateFile = temporary.resolve("earth-skin-wire-state.properties");
        GameStateStore state = new GameStateStore(stateFile, true);
        GameHub hub = new GameHub(config);
        String persistedKey = null;
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("earth-skin-capture")), hub);
            initialize(session, baseline());
            CharacterSummary elf = character("EarthSkinElf", 2, 0x3810_1510L);
            setField(session, "account", "earth-skin-account");
            setField(session, "selectedCharacter", elf.name());
            setField(session, "inventoryCharacter", elf.name());
            setField(session, "characters", List.of(elf));
            setField(session, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(elf));
            setField(session, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.EARTH_SKIN));

            hub.connect(session);
            hub.enterChannel(0, session);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Earth Skin", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, session);
            setPhase(session, "BATTLE");
            setField(session, "currentRoomId", roomId);
            setField(session, "playerHp", 100);
            setField(session, "playerMaxHp", 100);
            setField(session, "playerMp", 100);
            setField(session, "playerMaxMp", 100);

            int seed = 0x3810_1510;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            byte[] invalid = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(18).c(6)
                    .d(session.playerObjectId() + 1).bytes();
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, invalid);
            assertEquals(280, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MESSAGE_CODE, 2), 1));
            assertEquals(100, field(session, "playerMp"));
            BattleAbilityStatus ability =
                    (BattleAbilityStatus) field(session, "abilityStatus");
            assertEquals(new BattleAbilityStatus.ArmorProtection(0, 0, 0),
                    ability.armorProtection(System.nanoTime()));

            int baseArmorClass = session.battleArmorClass();
            byte[] request = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(18).c(6)
                    .d(session.playerObjectId()).bytes();
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 8)[5] & 0xFF);
            assertEquals(2249, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(85, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8), 1));
            byte[] icon = receiveUntilOpcode(
                    client, receiver, Protocol.S_MAGE_SHIELD, 8);
            assertEquals(960, unsignedShort(icon, 1));
            assertEquals(6, icon[3] & 0xFF);
            byte[] armor = receiveUntilOpcode(
                    client, receiver, Protocol.S_AC, 8);
            assertEquals(baseArmorClass - 6, (byte) armor[1]);
            assertEquals(new BattleAbilityStatus.ArmorProtection(
                            BattleSpellCatalog.EARTH_SKIN, -6, 960),
                    ability.armorProtection(System.nanoTime()));
            String stateKey = GameStateStore.sourceCharacter(
                    (String) field(session, "selectedClassicServerName"),
                    elf.name());
            persistedKey = stateKey;
            assertEquals(new GameStateStore.TimedArmorProtection(
                            BattleSpellCatalog.EARTH_SKIN, 960),
                    state.timedArmorProtection(stateKey));

            int worldMap = ClassicWorldMapCatalog.WORLD_TREE_MAP_ID;
            int worldX = ClassicWorldMapCatalog.WORLD_TREE_X;
            int worldY = ClassicWorldMapCatalog.WORLD_TREE_Y;
            hub.enterClassicWorld(session, worldMap, worldX, worldY, 5);
            setPhase(session, "WORLD");
            setField(session, "classicWorldMapId", worldMap);
            setField(session, "classicWorldInstanceId", -1);
            setField(session, "positionX", worldX);
            setField(session, "positionY", worldY);
            setField(session, "playerMp", 100);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 12)[5] & 0xFF);
            assertEquals(2249, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(85, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8), 1));
            icon = receiveUntilOpcode(
                    client, receiver, Protocol.S_MAGE_SHIELD, 8);
            assertEquals(960, unsignedShort(icon, 1));
            assertEquals(6, icon[3] & 0xFF);
            armor = receiveUntilOpcode(client, receiver, Protocol.S_AC, 8);
            assertEquals(baseArmorClass - 6, (byte) armor[1]);
            assertEquals(BattleSpellCatalog.EARTH_SKIN,
                    ability.armorProtection(System.nanoTime()).skillId());
        }
        assertTrue(persistedKey != null);
        assertTrue(new GameStateStore(stateFile, true)
                .timedArmorProtection(persistedKey).active());
    }

    @Test
    void nativeIronSkinUsesCase167AndCoexistsWithEarthSkinBattleWorld()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        Path stateFile = temporary.resolve("iron-skin-wire-state.properties");
        GameStateStore state = new GameStateStore(stateFile, true);
        GameHub hub = new GameHub(config);
        String persistedKey = null;
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("iron-skin-capture")), hub);
            initialize(session, baseline());
            CharacterSummary elf = character("IronSkinElf", 2, 0x3810_1680L);
            setField(session, "account", "iron-skin-account");
            setField(session, "selectedCharacter", elf.name());
            setField(session, "inventoryCharacter", elf.name());
            setField(session, "characters", List.of(elf));
            setField(session, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(elf));
            setField(session, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.IRON_SKIN));

            hub.connect(session);
            hub.enterChannel(0, session);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Iron Skin", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, session);
            setPhase(session, "BATTLE");
            setField(session, "currentRoomId", roomId);
            setField(session, "playerHp", 100);
            setField(session, "playerMaxHp", 100);
            setField(session, "playerMp", 100);
            setField(session, "playerMaxMp", 100);
            // Acquisition is earth-only, but the exact skill row attr=0 means
            // an already learned skill is not re-gated by current affinity.
            setField(session, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_FIRE);

            int seed = 0x3810_1680;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            byte[] invalid = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(20).c(7)
                    .d(session.playerObjectId() + 1).bytes();
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, invalid);
            assertEquals(280, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MESSAGE_CODE, 2), 1));
            assertEquals(100, field(session, "playerMp"));
            BattleIronSkinStatus iron =
                    (BattleIronSkinStatus) field(session, "ironSkinStatus");
            assertFalse(iron.snapshot(System.nanoTime()).active());

            BattleAbilityStatus ability =
                    (BattleAbilityStatus) field(session, "abilityStatus");
            ability.activateArmorProtection(BattleSpellCatalog.EARTH_SKIN,
                    System.nanoTime(), 2_000, -6);
            int earthSkinArmorClass = session.battleArmorClass();
            byte[] request = new Protocol.Writer()
                    .c(Protocol.C_USE_SPELL).c(20).c(7)
                    .d(session.playerObjectId()).bytes();
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 8)[5] & 0xFF);
            assertEquals(2252, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(70, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8), 1));
            byte[] armor = receiveUntilOpcode(
                    client, receiver, Protocol.S_AC, 8);
            assertEquals(earthSkinArmorClass - 10, (byte) armor[1]);
            assertEquals(714, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MESSAGE_CODE, 8), 1));
            assertEquals(-10, iron.armorClass(System.nanoTime()));
            assertEquals(BattleSpellCatalog.EARTH_SKIN,
                    ability.armorProtection(System.nanoTime()).skillId());
            String stateKey = GameStateStore.sourceCharacter(
                    (String) field(session, "selectedClassicServerName"),
                    elf.name());
            persistedKey = stateKey;
            assertEquals(960,
                    state.timedIronSkinBuff(stateKey).remainingSeconds());

            int worldMap = ClassicWorldMapCatalog.WORLD_TREE_MAP_ID;
            int worldX = ClassicWorldMapCatalog.WORLD_TREE_X;
            int worldY = ClassicWorldMapCatalog.WORLD_TREE_Y;
            hub.enterClassicWorld(session, worldMap, worldX, worldY, 5);
            setPhase(session, "WORLD");
            setField(session, "classicWorldMapId", worldMap);
            setField(session, "classicWorldInstanceId", -1);
            setField(session, "positionX", worldX);
            setField(session, "positionY", worldY);
            setField(session, "playerMp", 100);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 12)[5] & 0xFF);
            assertEquals(2252, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(70, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8), 1));
            armor = receiveUntilOpcode(client, receiver, Protocol.S_AC, 8);
            assertEquals(earthSkinArmorClass - 10, (byte) armor[1]);
            assertEquals(714, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MESSAGE_CODE, 8), 1));
            assertTrue(iron.snapshot(System.nanoTime()).active());
            assertEquals(BattleSpellCatalog.EARTH_SKIN,
                    ability.armorProtection(System.nanoTime()).skillId());

            long expiryNow = System.nanoTime()
                    + java.util.concurrent.TimeUnit.SECONDS.toNanos(961);
            invoke(session, "serverExpireTimedStatuses",
                    new Class<?>[] {long.class}, expiryNow);
            armor = receiveUntilOpcode(client, receiver, Protocol.S_AC, 8);
            assertEquals(earthSkinArmorClass, (byte) armor[1]);
            assertEquals(725, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MESSAGE_CODE, 8), 1));
            assertFalse(iron.snapshot(expiryNow).active());
            assertEquals(BattleSpellCatalog.EARTH_SKIN,
                    ability.armorProtection(expiryNow).skillId());
            assertFalse(state.timedIronSkinBuff(stateKey).active());
        }
        assertTrue(persistedKey != null);
        assertFalse(new GameStateStore(stateFile, true)
                .timedIronSkinBuff(persistedKey).active());
    }

    @Test
    void nativeExoticVitalizeUsesCase168AndBypassesWeightRegenBattleWorld()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        Path stateFile = temporary.resolve("exotic-vitalize-wire-state.properties");
        GameStateStore state = new GameStateStore(stateFile, true);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("exotic-vitalize-capture")), hub);
            initialize(session, baseline());
            CharacterSummary elf = character(
                    "ExoticVitalizeElf", 2, 0x3810_1690L);
            setField(session, "account", "exotic-vitalize-account");
            setField(session, "selectedCharacter", elf.name());
            setField(session, "inventoryCharacter", elf.name());
            setField(session, "characters", List.of(elf));
            setField(session, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(elf));
            setField(session, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.EXOTIC_VITALIZE));

            hub.connect(session);
            hub.enterChannel(0, session);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Exotic Vitalize", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, session);
            setPhase(session, "BATTLE");
            setField(session, "currentRoomId", roomId);
            setField(session, "playerHp", 100);
            setField(session, "playerMaxHp", 100);
            setField(session, "playerMp", 100);
            setField(session, "playerMaxMp", 100);

            int seed = 0x3810_1690;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(21).c(0).bytes();

            // The skill row consumes one Spirit Crystal; the NULL skills_item
            // row is not a replacement for that authoritative row field.
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(299, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MESSAGE_CODE, 2), 1));
            assertEquals(100, field(session, "playerMp"));
            BattleWeightStatus weight =
                    (BattleWeightStatus) field(session, "weightStatus");
            assertFalse(weight.snapshot(System.nanoTime())
                    .regenerationBypassActive());

            RuntimeInventory supplied = inventory(session).copy();
            assertTrue(supplied.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 3));
            setField(session, "inventory", supplied);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 10)[5] & 0xFF);
            assertEquals(4400, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(70, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8), 1));
            assertEquals(2, inventory(session).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));
            assertTrue(weight.snapshot(System.nanoTime())
                    .regenerationBypassActive());

            @SuppressWarnings("unchecked")
            java.util.Map<Integer, Long> ready =
                    (java.util.Map<Integer, Long>) field(session, "spellReadyNanos");
            ready.clear();
            int worldMap = ClassicWorldMapCatalog.WORLD_TREE_MAP_ID;
            int worldX = ClassicWorldMapCatalog.WORLD_TREE_X;
            int worldY = ClassicWorldMapCatalog.WORLD_TREE_Y;
            hub.enterClassicWorld(session, worldMap, worldX, worldY, 5);
            setPhase(session, "WORLD");
            setField(session, "classicWorldMapId", worldMap);
            setField(session, "classicWorldInstanceId", -1);
            setField(session, "positionX", worldX);
            setField(session, "positionY", worldY);
            setField(session, "playerMp", 100);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 12)[5] & 0xFF);
            assertEquals(4400, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(70, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8), 1));
            assertEquals(1, inventory(session).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));

            // Put the authoritative inventory in native stage120..196 and
            // prove WORLD MPR runs only while the retained bypass is active.
            long profileNow = System.nanoTime();
            BattleInventoryWeight.Profile profile =
                    (BattleInventoryWeight.Profile) invokeResult(session,
                            "currentWeightProfile",
                            new Class<?>[] {long.class}, profileNow);
            int desiredRaw = (int) Math.round(
                    BattleInventoryWeight.maxWeight(profile) * 180.0 / 240.0);
            int sourceRaw = Math.max(0,
                    desiredRaw - inventory(session).totalWeight());
            setField(session, "retainedSourceInventoryWeight", sourceRaw);
            int stage = (int) invokeResult(session, "currentWeight240",
                    new Class<?>[] {long.class}, profileNow);
            assertTrue(stage >= BattleInventoryWeight.NORMAL_REGEN_LIMIT);
            assertTrue(stage < BattleInventoryWeight.BUFFED_REGEN_LIMIT);

            setField(session, "playerHp", 50);
            BattleNaturalHpRegen hpRegen =
                    (BattleNaturalHpRegen) field(session, "naturalHpRegen");
            long hpRegenStart = System.nanoTime();
            hpRegen.reset(hpRegenStart);
            invoke(session, "serverNaturalHpRegen",
                    new Class<?>[] {long.class, boolean.class},
                    hpRegenStart + 31_000_000_000L, true);
            assertTrue(unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_HIT_POINT, 6), 1) > 50);

            setField(session, "playerMp", 50);
            BattleNaturalMpRegen regen =
                    (BattleNaturalMpRegen) field(session, "naturalMpRegen");
            long regenStart = System.nanoTime();
            regen.reset(regenStart);
            invoke(session, "serverNaturalMpRegen",
                    new Class<?>[] {long.class, boolean.class},
                    regenStart + 16_000_000_000L, true);
            assertTrue(unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 6), 1) > 50);

            assertTrue(weight.cancelMagicEffects());
            setField(session, "playerHp", 50);
            hpRegenStart = System.nanoTime();
            hpRegen.reset(hpRegenStart);
            invoke(session, "serverNaturalHpRegen",
                    new Class<?>[] {long.class, boolean.class},
                    hpRegenStart + 31_000_000_000L, true);
            assertEquals(50, field(session, "playerHp"));
            setField(session, "playerMp", 50);
            regenStart = System.nanoTime();
            regen.reset(regenStart);
            invoke(session, "serverNaturalMpRegen",
                    new Class<?>[] {long.class, boolean.class},
                    regenStart + 16_000_000_000L, true);
            assertEquals(50, field(session, "playerMp"));

            ready.clear();
            setField(session, "playerMp", 100);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            receiveUntilOpcode(client, receiver, Protocol.S_ACTION, 12);
            receiveUntilOpcode(client, receiver, Protocol.S_EFFECT, 8);
            receiveUntilOpcode(client, receiver, Protocol.S_MANA_POINT, 8);
            assertEquals(0, inventory(session).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));
            long expiryNow = System.nanoTime()
                    + java.util.concurrent.TimeUnit.SECONDS.toNanos(961);
            invoke(session, "serverExpireTimedStatuses",
                    new Class<?>[] {long.class}, expiryNow);
            receiveUntilOpcode(client, receiver, Protocol.S_STATUS, 8);
            assertFalse(weight.snapshot(expiryNow).regenerationBypassActive());
        }
        assertFalse(LocalResources.readString(stateFile).contains("exoticVitalize"));
    }

    @Test
    void nativeAdditionalFireUsesCase175AndBypassesWeightRegenBattleWorld()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        Path stateFile = temporary.resolve("additional-fire-wire-state.properties");
        GameStateStore state = new GameStateStore(stateFile, true);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("additional-fire-capture")), hub);
            initialize(session, baseline());
            CharacterSummary elf = character(
                    "AdditionalFireElf", 2, 0x3810_1760L);
            setField(session, "account", "additional-fire-account");
            setField(session, "selectedCharacter", elf.name());
            setField(session, "inventoryCharacter", elf.name());
            setField(session, "characters", List.of(elf));
            setField(session, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(elf));
            setField(session, "learnedSkills", java.util.Set.of());
            setField(session, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_WATER);

            hub.connect(session);
            hub.enterChannel(0, session);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Additional Fire", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, session);
            setPhase(session, "BATTLE");
            setField(session, "currentRoomId", roomId);
            setField(session, "playerHp", 100);
            setField(session, "playerMaxHp", 100);
            setField(session, "playerMp", 100);
            setField(session, "playerMaxMp", 100);

            int seed = 0x3810_1760;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(21).c(7).bytes();
            BattleWeightStatus weight =
                    (BattleWeightStatus) field(session, "weightStatus");

            // Fire acquisition supplies learnedSkills; attr=0 means there is
            // no second cast-time affinity check in the authoritative row.
            client.setSoTimeout(100);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(100, field(session, "playerMp"));
            assertFalse(weight.snapshot(System.nanoTime())
                    .regenerationBypassActive());
            try {
                receive(client, receiver);
                throw new AssertionError(
                        "unlearned Additional Fire unexpectedly emitted a packet");
            } catch (SocketTimeoutException expected) {
                // Exact retained learned-skill rejection is silent.
            }

            setField(session, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.ADDITIONAL_FIRE));
            client.setSoTimeout(1_000);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(299, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MESSAGE_CODE, 2), 1));
            assertEquals(100, field(session, "playerMp"));
            assertFalse(weight.snapshot(System.nanoTime())
                    .regenerationBypassActive());

            RuntimeInventory supplied = inventory(session).copy();
            assertTrue(supplied.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 3));
            setField(session, "inventory", supplied);
            setField(session, "sourceSkillReuseDelays",
                    java.util.Map.of(BattleSpellCatalog.ADDITIONAL_FIRE, 0));
            long battleBefore = System.nanoTime();
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            long battleAfter = System.nanoTime();
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 10)[5] & 0xFF);
            assertEquals(5825, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(70, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8), 1));
            assertEquals(2, inventory(session).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));
            assertTrue(weight.snapshot(System.nanoTime())
                    .regenerationBypassActive());
            @SuppressWarnings("unchecked")
            java.util.Map<Integer, Long> ready =
                    (java.util.Map<Integer, Long>) field(session, "spellReadyNanos");
            assertTrue(ready.get(BattleSpellCatalog.ADDITIONAL_FIRE)
                    >= battleBefore);
            assertTrue(ready.get(BattleSpellCatalog.ADDITIONAL_FIRE)
                    <= battleAfter);

            assertTrue(weight.cancelMagicEffects());
            assertFalse(weight.snapshot(System.nanoTime())
                    .regenerationBypassActive());
            ready.clear();
            setField(session, "sourceSkillReuseDelays",
                    java.util.Map.of(BattleSpellCatalog.ADDITIONAL_FIRE, 100));
            int worldMap = ClassicWorldMapCatalog.WORLD_TREE_MAP_ID;
            int worldX = ClassicWorldMapCatalog.WORLD_TREE_X;
            int worldY = ClassicWorldMapCatalog.WORLD_TREE_Y;
            hub.enterClassicWorld(session, worldMap, worldX, worldY, 5);
            setPhase(session, "WORLD");
            setField(session, "classicWorldMapId", worldMap);
            setField(session, "classicWorldInstanceId", -1);
            setField(session, "positionX", worldX);
            setField(session, "positionY", worldY);
            setField(session, "playerMp", 100);
            long worldBefore = System.nanoTime();
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 12)[5] & 0xFF);
            assertEquals(5825, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(70, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8), 1));
            assertEquals(1, inventory(session).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));
            assertTrue(ready.get(BattleSpellCatalog.ADDITIONAL_FIRE)
                    >= worldBefore + java.util.concurrent.TimeUnit.MILLISECONDS
                            .toNanos(50));

            long profileNow = System.nanoTime();
            BattleInventoryWeight.Profile profile =
                    (BattleInventoryWeight.Profile) invokeResult(session,
                            "currentWeightProfile",
                            new Class<?>[] {long.class}, profileNow);
            int desiredRaw = (int) Math.round(
                    BattleInventoryWeight.maxWeight(profile) * 180.0 / 240.0);
            int sourceRaw = Math.max(0,
                    desiredRaw - inventory(session).totalWeight());
            setField(session, "retainedSourceInventoryWeight", sourceRaw);
            int stage = (int) invokeResult(session, "currentWeight240",
                    new Class<?>[] {long.class}, profileNow);
            assertTrue(stage >= BattleInventoryWeight.NORMAL_REGEN_LIMIT);
            assertTrue(stage < BattleInventoryWeight.BUFFED_REGEN_LIMIT);

            setField(session, "playerHp", 50);
            BattleNaturalHpRegen hpRegen =
                    (BattleNaturalHpRegen) field(session, "naturalHpRegen");
            long hpRegenStart = System.nanoTime();
            hpRegen.reset(hpRegenStart);
            invoke(session, "serverNaturalHpRegen",
                    new Class<?>[] {long.class, boolean.class},
                    hpRegenStart + 31_000_000_000L, true);
            assertTrue(unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_HIT_POINT, 6), 1) > 50);

            setField(session, "playerMp", 50);
            BattleNaturalMpRegen mpRegen =
                    (BattleNaturalMpRegen) field(session, "naturalMpRegen");
            long mpRegenStart = System.nanoTime();
            mpRegen.reset(mpRegenStart);
            invoke(session, "serverNaturalMpRegen",
                    new Class<?>[] {long.class, boolean.class},
                    mpRegenStart + 16_000_000_000L, true);
            assertTrue(unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 6), 1) > 50);

            assertTrue(weight.cancelMagicEffects());
            ready.clear();
            setField(session, "playerMp", 100);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            receiveUntilOpcode(client, receiver, Protocol.S_ACTION, 12);
            receiveUntilOpcode(client, receiver, Protocol.S_EFFECT, 8);
            receiveUntilOpcode(client, receiver, Protocol.S_MANA_POINT, 8);
            assertEquals(0, inventory(session).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));
            long expiryNow = System.nanoTime()
                    + java.util.concurrent.TimeUnit.SECONDS.toNanos(961);
            invoke(session, "serverExpireTimedStatuses",
                    new Class<?>[] {long.class}, expiryNow);
            receiveUntilOpcode(client, receiver, Protocol.S_STATUS, 8);
            assertFalse(weight.snapshot(expiryNow).regenerationBypassActive());
        }
        assertFalse(LocalResources.readString(stateFile).contains("additionalFire"));
    }

    @Test
    void nativeWaterLifeUsesCase169TargetsPcAndDoublesOnlyNextHeal()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        Path stateFile = temporary.resolve("water-life-wire-state.properties");
        GameStateStore state = new GameStateStore(stateFile, true);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 2, InetAddress.getByName("127.0.0.1"));
             Socket casterClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket casterServer = listener.accept();
             Socket targetClient = new Socket("127.0.0.1", listener.getLocalPort());
             Socket targetServer = listener.accept()) {
            casterClient.setSoTimeout(1_000);
            targetClient.setSoTimeout(1_000);
            ClientSession caster = new ClientSession(
                    casterServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("water-life-caster-capture")), hub);
            ClientSession target = new ClientSession(
                    targetServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("water-life-target-capture")), hub);
            initialize(caster, baseline());
            initialize(target, baseline());
            CharacterSummary casterElf = character(
                    "WaterLifeCaster", 2, 0x3810_1700L);
            CharacterSummary targetElf = character(
                    "WaterLifeTarget", 2, 0x3810_1701L);
            setField(caster, "account", "water-life-caster-account");
            setField(caster, "selectedCharacter", casterElf.name());
            setField(caster, "inventoryCharacter", casterElf.name());
            setField(caster, "characters", List.of(casterElf));
            setField(caster, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(casterElf));
            setField(caster, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.WATER_LIFE));
            setField(caster, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_WATER);
            setField(target, "account", "water-life-target-account");
            setField(target, "selectedCharacter", targetElf.name());
            setField(target, "inventoryCharacter", targetElf.name());
            setField(target, "characters", List.of(targetElf));
            setField(target, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(targetElf));
            setField(target, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.NATURES_BLESSING));
            setField(target, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_WATER);
            setField(target, "playerLawful", 0);

            hub.connect(caster);
            hub.connect(target);
            hub.enterChannel(0, caster);
            hub.enterChannel(0, target);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Water Life", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, caster);
            hub.joinRoom(roomId, target);
            setPhase(caster, "BATTLE");
            setPhase(target, "BATTLE");
            setField(caster, "currentRoomId", roomId);
            setField(target, "currentRoomId", roomId);
            setField(caster, "positionX", 32740);
            setField(caster, "positionY", 32798);
            setField(target, "positionX", 32741);
            setField(target, "positionY", 32798);
            setField(caster, "playerHp", 100);
            setField(caster, "playerMaxHp", 100);
            setField(caster, "playerMp", 100);
            setField(caster, "playerMaxMp", 100);
            setField(target, "playerHp", 20);
            setField(target, "playerMaxHp", 100);
            setField(target, "playerMp", 100);
            setField(target, "playerMaxMp", 100);

            int casterSeed = 0x3810_1700;
            int targetSeed = 0x3810_1701;
            setField(caster, "outgoing", new Protocol.CipherState(casterSeed));
            setField(target, "outgoing", new Protocol.CipherState(targetSeed));
            Protocol.CipherState casterReceiver = new Protocol.CipherState(casterSeed);
            Protocol.CipherState targetReceiver = new Protocol.CipherState(targetSeed);
            byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(21).c(1).d(target.playerObjectId()).bytes();
            BattleWaterLifeStatus targetStatus = (BattleWaterLifeStatus) field(
                    target, "waterLifeStatus");

            // The exact skill row, not skills_item, requires one Spirit Crystal.
            invoke(caster, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(299, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MESSAGE_CODE, 2), 1));
            assertEquals(100, field(caster, "playerMp"));
            assertFalse(targetStatus.active(System.nanoTime()));

            RuntimeInventory supplied = inventory(caster).copy();
            assertTrue(supplied.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 3));
            setField(caster, "inventory", supplied);
            invoke(caster, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_ACTION, 10)[5] & 0xFF);
            byte[] casterEffect = receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_EFFECT, 8);
            assertEquals(target.playerObjectId(), unsignedInt(casterEffect, 1));
            assertEquals(4401, unsignedShort(casterEffect, 5));
            assertEquals(99, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 8), 1));
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_ACTION, 8);
            byte[] targetEffect = receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_EFFECT, 8);
            assertEquals(target.playerObjectId(), unsignedInt(targetEffect, 1));
            assertEquals(4401, unsignedShort(targetEffect, 5));
            assertTrue(targetStatus.active(System.nanoTime()));
            assertEquals(2, inventory(caster).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));

            // A real retained heal consumes the lease after doubling it.
            byte[] blessingRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(20).c(3).bytes();
            BattleSpellCatalog.NaturesBlessingSpell blessing =
                    BattleSpellCatalog.naturesBlessingSpell(
                            BattleSpellCatalog.NATURES_BLESSING);
            invoke(target, "useNaturesBlessingSpell", new Class<?>[] {
                    byte[].class, BattleSpellCatalog.NaturesBlessingSpell.class,
                    int.class
            }, blessingRequest, blessing, 1);
            assertEquals(19, receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_ACTION, 10)[5] & 0xFF);
            assertEquals(2244, unsignedShort(receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(70, unsignedShort(receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_MANA_POINT, 8), 1));
            int intelligence = ((SourceEquipmentMagicStats) field(
                    target, "sourceEquipmentMagicStats"))
                    .effectiveIntelligence(targetElf.intelligence());
            int expectedHp = 20 + blessing.diceCount(intelligence) * 2;
            assertEquals(expectedHp, unsignedShort(receiveUntilOpcode(
                    targetClient, targetReceiver, Protocol.S_HIT_POINT, 8), 1));
            assertEquals(expectedHp, field(target, "playerHp"));
            assertFalse(targetStatus.active(System.nanoTime()));

            @SuppressWarnings("unchecked")
            java.util.Map<Integer, Long> ready =
                    (java.util.Map<Integer, Long>) field(caster, "spellReadyNanos");
            int worldMap = ClassicWorldMapCatalog.WORLD_TREE_MAP_ID;
            int worldX = ClassicWorldMapCatalog.WORLD_TREE_X;
            int worldY = ClassicWorldMapCatalog.WORLD_TREE_Y;
            hub.enterClassicWorld(caster, worldMap, worldX, worldY, 5);
            hub.enterClassicWorld(target, worldMap, worldX + 1, worldY, 5);
            setPhase(caster, "WORLD");
            setPhase(target, "WORLD");
            setField(caster, "classicWorldMapId", worldMap);
            setField(target, "classicWorldMapId", worldMap);
            setField(caster, "classicWorldInstanceId", -1);
            setField(target, "classicWorldInstanceId", -1);
            setField(caster, "positionX", worldX);
            setField(caster, "positionY", worldY);
            setField(target, "positionX", worldX + 1);
            setField(target, "positionY", worldY);
            setField(caster, "playerMp", 100);

            ready.clear();
            invoke(caster, "useSpell", new Class<?>[] {byte[].class}, request);
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_ACTION, 10);
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_EFFECT, 8);
            assertEquals(99, unsignedShort(receiveUntilOpcode(
                    casterClient, casterReceiver, Protocol.S_MANA_POINT, 8), 1));
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_ACTION, 8);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_EFFECT, 8);
            assertTrue(targetStatus.active(System.nanoTime()));
            assertEquals(1, inventory(caster).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));

            ready.clear();
            setField(caster, "playerMp", 100);
            invoke(caster, "useSpell", new Class<?>[] {byte[].class}, request);
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_ACTION, 10);
            receiveUntilOpcode(casterClient, casterReceiver, Protocol.S_EFFECT, 8);
            receiveUntilOpcode(casterClient, casterReceiver,
                    Protocol.S_MANA_POINT, 8);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_ACTION, 8);
            receiveUntilOpcode(targetClient, targetReceiver, Protocol.S_EFFECT, 8);
            assertTrue(targetStatus.active(System.nanoTime()));
            assertEquals(0, inventory(caster).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));

            long expiryNow = System.nanoTime()
                    + java.util.concurrent.TimeUnit.SECONDS.toNanos(65);
            invoke(target, "serverExpireTimedStatuses",
                    new Class<?>[] {long.class}, expiryNow);
            assertFalse(targetStatus.active(expiryNow));
        }
        assertFalse(LocalResources.readString(stateFile).contains("waterLife"));
    }

    @Test
    void nativeElementalFireUsesCase170InBattleAndWorldWithTransientMeleeProc()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        Path stateFile = temporary.resolve("elemental-fire-wire-state.properties");
        GameStateStore state = new GameStateStore(stateFile, true);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve(
                            "elemental-fire-capture")), hub);
            initialize(session, baseline());
            CharacterSummary elf = character(
                    "ElementalFireCaster", 2, 0x3810_1710L);
            setField(session, "account", "elemental-fire-account");
            setField(session, "selectedCharacter", elf.name());
            setField(session, "inventoryCharacter", elf.name());
            setField(session, "characters", List.of(elf));
            setField(session, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(elf));
            setField(session, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.ELEMENTAL_FIRE));
            setField(session, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_WATER);
            setField(session, "playerHp", 100);
            setField(session, "playerMaxHp", 100);
            setField(session, "playerMp", 100);
            setField(session, "playerMaxMp", 100);
            setField(session, "positionX", 32740);
            setField(session, "positionY", 32798);

            hub.connect(session);
            hub.enterChannel(0, session);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Elemental Fire", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, session);
            setPhase(session, "BATTLE");
            setField(session, "currentRoomId", roomId);
            int seed = 0x3810_1710;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(21).c(2).bytes();
            BattleTimedStatus timed = (BattleTimedStatus) field(
                    session, "timedStatus");

            // Exact fire affinity is a silent pre-resource runtime gate.
            client.setSoTimeout(100);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(100, field(session, "playerMp"));
            assertFalse(timed.elementalFireActive(System.nanoTime()));
            try {
                receive(client, receiver);
                throw new AssertionError(
                        "non-fire Elemental Fire unexpectedly emitted a packet");
            } catch (SocketTimeoutException expected) {
                // Exact retained attribute rejection is silent.
            }

            setField(session, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_FIRE);
            client.setSoTimeout(1_000);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(299, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MESSAGE_CODE, 2), 1));
            assertEquals(100, field(session, "playerMp"));
            assertFalse(timed.elementalFireActive(System.nanoTime()));

            RuntimeInventory supplied = inventory(session).copy();
            assertTrue(supplied.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 2));
            setField(session, "inventory", supplied);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 10)[5] & 0xFF);
            byte[] effect = receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 8);
            assertEquals(session.playerObjectId(), unsignedInt(effect, 1));
            assertEquals(4402, unsignedShort(effect, 5));
            assertEquals(80, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8), 1));
            assertTrue(timed.elementalFireActive(System.nanoTime()));
            assertEquals(1, inventory(session).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));
            assertTrue(BattlePhysicalProcBuff.elementalFire(
                    true, true, 33).triggered());
            assertFalse(BattlePhysicalProcBuff.elementalFire(
                    true, false, 0).eligible());

            BattleTimedStatus.MagicCancellation cancellation =
                    timed.cancelMagicEffects();
            assertTrue(cancellation.elementalFire());
            assertFalse(timed.elementalFireActive(System.nanoTime()));

            @SuppressWarnings("unchecked")
            java.util.Map<Integer, Long> ready =
                    (java.util.Map<Integer, Long>) field(
                            session, "spellReadyNanos");
            ready.clear();
            int worldMap = ClassicWorldMapCatalog.WORLD_TREE_MAP_ID;
            int worldX = ClassicWorldMapCatalog.WORLD_TREE_X;
            int worldY = ClassicWorldMapCatalog.WORLD_TREE_Y;
            hub.enterClassicWorld(session, worldMap, worldX, worldY, 5);
            setPhase(session, "WORLD");
            setField(session, "classicWorldMapId", worldMap);
            setField(session, "classicWorldInstanceId", -1);
            setField(session, "positionX", worldX);
            setField(session, "positionY", worldY);
            setField(session, "playerMp", 100);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 10)[5] & 0xFF);
            assertEquals(4402, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(80, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8), 1));
            assertTrue(timed.elementalFireActive(System.nanoTime()));
            assertEquals(0, inventory(session).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));

            long expiryNow = System.nanoTime()
                    + java.util.concurrent.TimeUnit.SECONDS.toNanos(193);
            invoke(session, "serverExpireTimedStatuses",
                    new Class<?>[] {long.class}, expiryNow);
            assertFalse(timed.elementalFireActive(expiryNow));
        }
        assertFalse(LocalResources.readString(stateFile).contains("elementalFire"));
    }

    @Test
    void nativeSoulOfFlameUsesCase174AndSelectedSourceReuseInBattleAndWorld()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        Path stateFile = temporary.resolve("soul-of-flame-wire-state.properties");
        GameStateStore state = new GameStateStore(stateFile, true);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("soul-of-flame-capture")), hub);
            initialize(session, baseline());
            CharacterSummary elf = character("SoulOfFlameCaster", 2, 0x3810_1750L);
            setField(session, "account", "soul-of-flame-account");
            setField(session, "selectedCharacter", elf.name());
            setField(session, "inventoryCharacter", elf.name());
            setField(session, "characters", List.of(elf));
            setField(session, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(elf));
            setField(session, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.SOUL_OF_FLAME));
            setField(session, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_WATER);
            setField(session, "playerHp", 100);
            setField(session, "playerMaxHp", 100);
            setField(session, "playerMp", 100);
            setField(session, "playerMaxMp", 100);
            setField(session, "positionX", 32740);
            setField(session, "positionY", 32798);

            hub.connect(session);
            hub.enterChannel(0, session);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Soul of Flame", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, session);
            setPhase(session, "BATTLE");
            setField(session, "currentRoomId", roomId);
            int seed = 0x3810_1750;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(21).c(6).bytes();
            BattleTimedStatus timed = (BattleTimedStatus) field(
                    session, "timedStatus");

            // A non-fire source is rejected before MP, materials, or packets.
            client.setSoTimeout(100);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(100, field(session, "playerMp"));
            assertFalse(timed.soulOfFlameActive(System.nanoTime()));
            try {
                receive(client, receiver);
                throw new AssertionError(
                        "non-fire Soul of Flame unexpectedly emitted a packet");
            } catch (SocketTimeoutException expected) {
                // Exact retained attribute rejection is silent.
            }

            setField(session, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_FIRE);
            client.setSoTimeout(1_000);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(299, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MESSAGE_CODE, 2), 1));
            assertEquals(100, field(session, "playerMp"));
            assertFalse(timed.soulOfFlameActive(System.nanoTime()));

            RuntimeInventory supplied = inventory(session).copy();
            assertTrue(supplied.add(InventoryCatalog.require(
                    InventoryCatalog.SPIRIT_CRYSTAL), 6));
            setField(session, "inventory", supplied);
            // The secondary resource source has reuseDelay=0 for skill175.
            setField(session, "sourceSkillReuseDelays",
                    java.util.Map.of(BattleSpellCatalog.SOUL_OF_FLAME, 0));
            long battleBefore = System.nanoTime();
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            long battleAfter = System.nanoTime();
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 10)[5] & 0xFF);
            byte[] effect = receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 8);
            assertEquals(session.playerObjectId(), unsignedInt(effect, 1));
            assertEquals(5833, unsignedShort(effect, 5));
            assertEquals(70, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8), 1));
            assertTrue(timed.soulOfFlameActive(System.nanoTime()));
            assertEquals(3, inventory(session).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));
            @SuppressWarnings("unchecked")
            java.util.Map<Integer, Long> ready =
                    (java.util.Map<Integer, Long>) field(
                            session, "spellReadyNanos");
            assertTrue(ready.get(BattleSpellCatalog.SOUL_OF_FLAME) >= battleBefore);
            assertTrue(ready.get(BattleSpellCatalog.SOUL_OF_FLAME) <= battleAfter);

            BattleTimedStatus.MagicCancellation cancellation =
                    timed.cancelMagicEffects();
            assertTrue(cancellation.soulOfFlame());
            assertFalse(timed.soulOfFlameActive(System.nanoTime()));

            ready.clear();
            setField(session, "sourceSkillReuseDelays",
                    java.util.Map.of(BattleSpellCatalog.SOUL_OF_FLAME, 300));
            int worldMap = ClassicWorldMapCatalog.WORLD_TREE_MAP_ID;
            int worldX = ClassicWorldMapCatalog.WORLD_TREE_X;
            int worldY = ClassicWorldMapCatalog.WORLD_TREE_Y;
            hub.enterClassicWorld(session, worldMap, worldX, worldY, 5);
            setPhase(session, "WORLD");
            setField(session, "classicWorldMapId", worldMap);
            setField(session, "classicWorldInstanceId", -1);
            setField(session, "positionX", worldX);
            setField(session, "positionY", worldY);
            setField(session, "playerMp", 100);
            long worldBefore = System.nanoTime();
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(19, receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 10)[5] & 0xFF);
            assertEquals(5833, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_EFFECT, 8), 5));
            assertEquals(70, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MANA_POINT, 8), 1));
            assertTrue(timed.soulOfFlameActive(System.nanoTime()));
            assertEquals(0, inventory(session).catalogCounts().getOrDefault(
                    InventoryCatalog.SPIRIT_CRYSTAL, 0));
            assertTrue(ready.get(BattleSpellCatalog.SOUL_OF_FLAME)
                    >= worldBefore + java.util.concurrent.TimeUnit.MILLISECONDS
                            .toNanos(250));

            long expiryNow = System.nanoTime()
                    + java.util.concurrent.TimeUnit.SECONDS.toNanos(65);
            invoke(session, "serverExpireTimedStatuses",
                    new Class<?>[] {long.class}, expiryNow);
            assertFalse(timed.soulOfFlameActive(expiryNow));
        }
        assertFalse(LocalResources.readString(stateFile).contains("soulOfFlame"));
    }

    @Test
    void nativeStormWalkUsesCase171EconomyOnlyInBattleAndWorld()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        Path stateFile = temporary.resolve("storm-walk-wire-state.properties");
        GameStateStore state = new GameStateStore(stateFile, true);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("storm-walk-capture")), hub);
            initialize(session, baseline());
            CharacterSummary elf = character("StormWalkCaster", 2, 0x3810_1720L);
            setField(session, "account", "storm-walk-account");
            setField(session, "selectedCharacter", elf.name());
            setField(session, "inventoryCharacter", elf.name());
            setField(session, "characters", List.of(elf));
            setField(session, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(elf));
            setField(session, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.STORM_WALK));
            setField(session, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_FIRE);
            setField(session, "playerHp", 100);
            setField(session, "playerMaxHp", 100);
            setField(session, "playerMp", 100);
            setField(session, "playerMaxMp", 100);
            setField(session, "positionX", 32740);
            setField(session, "positionY", 32798);

            hub.connect(session);
            hub.enterChannel(0, session);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Storm Walk", "", 2, 1, 0, 0, 0, 0));
            hub.joinRoom(roomId, session);
            setPhase(session, "BATTLE");
            setField(session, "currentRoomId", roomId);
            int seed = 0x3810_1720;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(21).c(3).bytes();

            // Exact wind affinity rejection is silent and pre-resource.
            client.setSoTimeout(100);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(100, field(session, "playerMp"));
            try {
                receive(client, receiver);
                throw new AssertionError(
                        "non-wind Storm Walk unexpectedly emitted a packet");
            } catch (SocketTimeoutException expected) {
                // Exact retained attribute rejection is silent.
            }

            setField(session, "elfAttribute", BattleSpellCatalog.ATTRIBUTE_WIND);
            setField(session, "playerMp", 5);
            client.setSoTimeout(1_000);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            assertEquals(278, unsignedShort(receiveUntilOpcode(
                    client, receiver, Protocol.S_MESSAGE_CODE, 2), 1));
            assertEquals(5, field(session, "playerMp"));

            // Row172 has action19 metadata but gfx0/duration0: successful casts
            // publish only the authoritative MP value, with no action/effect,
            // lease or cooldown.  Immediate recast must therefore also succeed.
            setField(session, "playerMp", 100);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            byte[] firstMana = receive(client, receiver);
            assertEquals(Protocol.S_MANA_POINT, firstMana[0] & 0xFF);
            assertEquals(90, unsignedShort(firstMana, 1));
            assertEquals(90, field(session, "playerMp"));

            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            byte[] secondMana = receive(client, receiver);
            assertEquals(Protocol.S_MANA_POINT, secondMana[0] & 0xFF);
            assertEquals(80, unsignedShort(secondMana, 1));
            assertEquals(80, field(session, "playerMp"));
            @SuppressWarnings("unchecked")
            java.util.Map<Integer, Long> ready =
                    (java.util.Map<Integer, Long>) field(session, "spellReadyNanos");
            assertFalse(ready.containsKey(BattleSpellCatalog.STORM_WALK));

            client.setSoTimeout(100);
            try {
                receive(client, receiver);
                throw new AssertionError(
                        "Storm Walk unexpectedly emitted action/effect/icon state");
            } catch (SocketTimeoutException expected) {
                // Retained gfx0/duration0 path has no additional frame.
            }

            int worldMap = ClassicWorldMapCatalog.WORLD_TREE_MAP_ID;
            int worldX = ClassicWorldMapCatalog.WORLD_TREE_X;
            int worldY = ClassicWorldMapCatalog.WORLD_TREE_Y;
            hub.enterClassicWorld(session, worldMap, worldX, worldY, 5);
            setPhase(session, "WORLD");
            setField(session, "classicWorldMapId", worldMap);
            setField(session, "classicWorldInstanceId", -1);
            setField(session, "positionX", worldX);
            setField(session, "positionY", worldY);
            setField(session, "playerMp", 100);
            client.setSoTimeout(1_000);
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, request);
            byte[] worldMana = receive(client, receiver);
            assertEquals(Protocol.S_MANA_POINT, worldMana[0] & 0xFF);
            assertEquals(90, unsignedShort(worldMana, 1));
            assertEquals(90, field(session, "playerMp"));
        }
        assertFalse(LocalResources.readString(stateFile).contains("stormWalk"));
    }

    @Test
    void nativeReserved81Through86RequestsNeverMutateOrEmitInBattleOrWorld()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("reserved-81-86-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(200);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve(
                            "reserved-81-86-capture")), hub);
            initialize(session, baseline());
            setField(session, "playerHp", 100);
            setField(session, "playerMaxHp", 100);
            setField(session, "playerMp", 100);
            setField(session, "playerMaxMp", 100);
            setField(session, "learnedSkills", java.util.Set.of(
                    81, 82, 83, 84, 85, 86));
            RuntimeInventory before = inventory(session).copy();

            for (String phase : List.of("BATTLE", "WORLD")) {
                setPhase(session, phase);
                for (int skillId = 81; skillId <= 86; skillId++) {
                    int column = skillId - 81;
                    byte[] request = new Protocol.Writer()
                            .c(Protocol.C_USE_SPELL).c(10).c(column).bytes();
                    Protocol.SpellSelector parsed =
                            Protocol.spellSelector(request);
                    assertEquals(skillId, parsed.skillId());
                    assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                            BattleSpellCatalog.reservedSkillSlot(skillId)
                                    .requestShape());
                    invoke(session, "useSpell", new Class<?>[] {byte[].class},
                            request);
                }
            }

            assertEquals(100, field(session, "playerHp"));
            assertEquals(100, field(session, "playerMp"));
            assertEquals(before.catalogCounts(),
                    inventory(session).catalogCounts());
            assertTrue(((java.util.Map<?, ?>) field(
                    session, "spellReadyNanos")).isEmpty());
            assertEquals(0, client.getInputStream().available());
        }
    }

    @Test
    void forgedReserved139Through144RequestsNeverMutateOrEmitFrames()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("reserved-139-144-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(200);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve(
                            "reserved-139-144-capture")), hub);
            initialize(session, baseline());
            setPhase(session, "BATTLE");
            setField(session, "playerHp", 100);
            setField(session, "playerMaxHp", 100);
            setField(session, "playerMp", 100);
            setField(session, "playerMaxMp", 100);
            setField(session, "learnedSkills", java.util.Set.of(
                    139, 140, 141, 142, 143, 144));
            RuntimeInventory before = inventory(session).copy();

            for (int skillId = 139; skillId <= 144; skillId++) {
                int row = (skillId - 1) / 8;
                int column = (skillId - 1) % 8;
                byte[] forged = new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(row).c(column).bytes();
                Protocol.SpellSelector parsed = Protocol.spellSelector(forged);
                assertEquals(skillId, parsed.skillId());
                assertEquals(BattleSpellCatalog.RequestShape.NO_PRODUCER,
                        BattleSpellCatalog.reservedSkillSlot(skillId)
                                .requestShape());
                invoke(session, "useSpell", new Class<?>[] {byte[].class},
                        forged);
            }

            assertEquals(100, field(session, "playerMp"));
            assertEquals(before.catalogCounts(),
                    inventory(session).catalogCounts());
            assertEquals(0, client.getInputStream().available());
        }
    }

    @Test
    void matchResultKeepsZeroAllowanceUntilLobbyReturn()
            throws Exception {
        Fixture fixture = fixture();
        RuntimeInventory live = fixture.baseline.copy();
        assertTrue(live.add(InventoryCatalog.require(
                InventoryCatalog.EXTRA_HEALING_POTION), 7));
        setField(fixture.session, "inventory", live);
        setField(fixture.session, "shopPoints", 18_600L);
        setField(fixture.session, "inventoryBootstrapSent", true);
        setField(fixture.session, "retainedSourceInventoryWeight", 9_999);

        invokeReset(fixture.session, "battle-result");

        RuntimeInventory reset = inventory(fixture.session);
        assertEquals(fixture.baseline.catalogCounts(), reset.catalogCounts());
        assertTrue(reset.items().stream().allMatch(RuntimeInventory.Item::sourceBound));
        assertEquals(0L, field(fixture.session, "shopPoints"));
        assertFalse((boolean) field(fixture.session, "inventoryBootstrapSent"));
        assertEquals(0, field(fixture.session, "retainedSourceInventoryWeight"));
        assertEquals(SourceEquipmentMagicStats.none(),
                field(fixture.session, "sourceEquipmentMagicStats"));
        GameStateStore.Economy durable = fixture.state.economy(
                fixture.session.sourceCharacterIdentity());
        assertEquals(0L, durable.points());
        assertEquals(fixture.baseline.catalogCounts(), durable.inventory());
        invokeReset(fixture.session, "room-exit");
        invokeReset(fixture.session, "room-exit");
        assertEquals(20_000L, field(fixture.session, "shopPoints"));
        assertEquals(20_000L, fixture.state.economy(fixture.session.sourceCharacterIdentity()).points());
    }

    @Test
    void heroPumpkinBagOpensIntoProvenanceBoundStackAndResetDropsBoughtTwin()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("pumpkin-bag-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("pumpkin-bag-capture")), hub);
            RuntimeInventory live = baseline();
            initialize(session, live);
            InventoryCatalog.Definition potion = InventoryCatalog.require(
                    InventoryCatalog.NATIVE_HEALING_POTION);
            assertTrue(live.add(potion, 5));
            // Simulate a 0.7.387 unmarked unopened bag; opening must migrate
            // its result into the explicit format-34 provenance domain.
            assertTrue(live.add(InventoryCatalog.require(
                    BattlePetRaceRewardCatalog.HERO_PUMPKIN_BAG), 1));
            RuntimeInventory.Item bag = live.items().stream()
                    .filter(candidate -> candidate.definition().catalogId()
                            == BattlePetRaceRewardCatalog.HERO_PUMPKIN_BAG)
                    .findFirst().orElseThrow();
            setField(session, "inventory", live);
            setField(session, "playerHp", 100);
            setPhase(session, "BATTLE");
            setField(session, "outgoing", new Protocol.CipherState(0x3810_0388));

            invoke(session, "useHeroPumpkinBag",
                    new Class<?>[] {RuntimeInventory.Item.class, int.class},
                    bag, 1_000);

            RuntimeInventory opened = inventory(session);
            assertEquals(0, opened.catalogCounts().getOrDefault(
                    BattlePetRaceRewardCatalog.HERO_PUMPKIN_BAG, 0));
            assertEquals(35, opened.catalogCounts().get(potion.catalogId()));
            assertEquals(List.of(false, true), opened.items().stream()
                    .filter(candidate -> candidate.definition().catalogId()
                            == potion.catalogId())
                    .map(RuntimeInventory.Item::tournamentReward).toList());

            invokeReset(session, "post-bag-open");
            RuntimeInventory retained = inventory(session);
            assertEquals(30, retained.catalogCounts().get(potion.catalogId()));
            assertTrue(retained.items().stream()
                    .filter(candidate -> candidate.definition().catalogId()
                            == potion.catalogId())
                    .allMatch(RuntimeInventory.Item::tournamentReward));
            assertEquals(30, state.economy(
                    session.sourceCharacterIdentity()).inventory()
                    .get(potion.catalogId()));
        }
    }

    @Test
    void battleRankingResultDoesNotReplaceTheCurrentMatchAllowance() throws Exception {
        Fixture fixture = fixture();
        setField(fixture.session, "shopPoints", 17_250L);

        Method result = ClientSession.class.getDeclaredMethod(
                "recordBattleResult", boolean.class, boolean.class);
        result.setAccessible(true);
        result.invoke(fixture.session, true, false);

        assertEquals(17_250L, field(fixture.session, "shopPoints"));
        assertEquals(1, fixture.state.tournamentStatistics(
                fixture.session.sourceCharacterIdentity()).wins());
    }

    @Test
    void sharedNativeWeaponBuffAuthorityFeedsAllThreePhysicalRoutes()
            throws Exception {
        Fixture fixture = fixture();
        ClientSession session = fixture.session;
        long now = System.nanoTime();
        ((BattleFireWeaponStatus) field(session, "fireWeaponStatus"))
                .activate(now, 960, 4);
        ((BattleFireBlessStatus) field(session, "fireBlessStatus"))
                .activate(now, 960, 4);
        ((BattleWindShotStatus) field(session, "windShotStatus"))
                .activate(now, 960, 6);
        ((BattleStormEyeStatus) field(session, "stormEyeStatus"))
                .activate(now, 960, 2, 3);
        ((BattleStormShotStatus) field(session, "stormShotStatus"))
                .activate(now, 960, -1, 5);
        ((BattleBurningWeaponStatus) field(session, "burningWeaponStatus"))
                .activate(now, 960, 3, 6);

        Object melee = invokeResult(session, "nativeWeaponBuffModifiers",
                new Class<?>[] {boolean.class, long.class}, false, now);
        assertEquals(3, field(melee, "hit"));
        assertEquals(14, field(melee, "damage"));

        Object ranged = invokeResult(session, "nativeWeaponBuffModifiers",
                new Class<?>[] {boolean.class, long.class}, true, now);
        assertEquals(7, field(ranged, "hit"));
        assertEquals(8, field(ranged, "damage"));
    }

    private Fixture fixture() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("state.properties"), false);
        GameHub hub = new GameHub(config);
        ClientSession session = new ClientSession(
                null, ClientSession.Endpoint.GAME, config, null, state, LocalResources.discardCapture(), hub);
        setField(session, "account", "native-economy-test");
        setField(session, "selectedCharacter", "NativeEconomyKnight");
        RuntimeInventory baseline = baseline();
        initialize(session, baseline);
        return new Fixture(session, state, baseline);
    }

    @Test
    void encryptedHonorDeathWaitsFiveSecondsAndResurrectsAtDeathPosition()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("honor-respawn-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(400);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("honor-respawn-capture")), hub);
            initialize(session, baseline());
            CharacterSummary character = character(
                    "HonorRespawnKnight", 1, 0x3810_5618L);
            setField(session, "account", "honor-respawn-account");
            setField(session, "selectedCharacter", character.name());
            setField(session, "characters", List.of(character));
            setField(session, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(character));
            hub.connect(session);
            hub.enterChannel(0, session);
            long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                    "Honor Respawn", "", 2,
                    BattleRules.Mode.HONOR_MATCH.wireId(), 0, 0, 0, 0));
            hub.joinRoom(roomId, session);
            hub.setReady(roomId, session, true);
            Method countdown = GameHub.class.getDeclaredMethod(
                    "countdownWork", long.class);
            countdown.setAccessible(true);
            long future = System.nanoTime() + 10_000_000_000L;
            countdown.invoke(hub, future);
            countdown.invoke(hub, future + 2_000_000_000L);
            countdown.invoke(hub, future + 4_000_000_000L);

            setPhase(session, "BATTLE");
            setField(session, "currentRoomId", roomId);
            int deathX = 32755;
            int deathY = 32802;
            setField(session, "positionX", deathX);
            setField(session, "positionY", deathY);
            setField(session, "playerMaxHp", 100);
            setField(session, "playerHp", 0);
            setField(session, "deathConfirmationPending", true);
            int seed = 0x3810_5618;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            long confirmedAt = System.nanoTime();
            invoke(session, "confirmDeath", new Class<?>[] {byte[].class},
                    new Protocol.Writer().c(Protocol.C_LITO_CONFIRM_DEATH).bytes());
            assertEquals(0, (int) field(session, "playerHp"));
            assertEquals(deathX, (int) field(session, "positionX"));
            assertEquals(deathY, (int) field(session, "positionY"));
            assertFalse(hub.matchSnapshot(roomId).alivePlayers().contains(
                    session.playerObjectId()));
            try {
                receive(client, receiver);
                throw new AssertionError("Honor respawn arrived before five seconds");
            } catch (SocketTimeoutException expected) {
                // The native corpse interval remains authoritative.
            }

            client.setSoTimeout(6_000);
            byte[] resurrected = receive(client, receiver);
            long arrivedAt = System.nanoTime();
            assertEquals(Protocol.S_RESURRECT, resurrected[0] & 0xFF);
            assertTrue((arrivedAt - confirmedAt) / 1_000_000L >= 4_900L);
            byte[] hp = receive(client, receiver);
            assertEquals(Protocol.S_HIT_POINT, hp[0] & 0xFF);
            assertEquals(100, unsignedShort(hp, 1));
            assertEquals(100, (int) field(session, "playerHp"));
            assertEquals(deathX, (int) field(session, "positionX"));
            assertEquals(deathY, (int) field(session, "positionY"));
            assertTrue(hub.matchSnapshot(roomId).alivePlayers().contains(
                    session.playerObjectId()));

            client.setSoTimeout(250);
            try {
                byte[] unexpected = receive(client, receiver);
                assertFalse((unexpected[0] & 0xFF) == Protocol.S_MOVE_OBJECT,
                        "Honor same-position resurrection must not teleport to a spawn anchor");
            } catch (SocketTimeoutException expected) {
                // No spawn movement/minimap rewrite is the exact boundary.
            }
        } finally {
            hub.close();
        }
    }

    @Test
    void lethalDisintegrateKeepsNativeWorldAliveUntilItsSixtyFramesComplete()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("disintegrate-terminal-effect-state.properties"), false);
        GameHub hub = new GameHub(config, state);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(500);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("disintegrate-terminal-effect-capture")),
                    hub);
            initialize(session, baseline());
            long roomId = startSoloBattle(hub, session);
            ArenaMapCatalog.Point bot = hub.botPosition(roomId);
            assertNotNull(bot);
            setPhase(session, "BATTLE");
            setField(session, "currentRoomId", roomId);
            setField(session, "positionX", bot.x() - 1);
            setField(session, "positionY", bot.y());
            setField(session, "playerHp", 100);
            setField(session, "playerMaxHp", 100);
            setField(session, "playerMp", 100);
            setField(session, "playerMaxMp", 100);
            setField(session, "playerLawful", 500);

            int seed = 0x3810_77D3;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(9).c(4).d(2).h(bot.x()).h(bot.y()).bytes();
            long castAt = System.nanoTime();
            invoke(session, "useAttackSpell",
                    new Class<?>[] {byte[].class, BattleSpellCatalog.AttackSpell.class},
                    request, BattleSpellCatalog.attackSpell(
                            BattleSpellCatalog.DISINTEGRATE));

            byte[] attack = receiveUntilOpcode(
                    client, receiver, Protocol.S_ATTACK, 8);
            int effectGfx = unsignedShort(attack, 16);
            assertTrue(effectGfx == 11746 || effectGfx == 11748,
                    "unexpected Disintegrate resource " + effectGfx);
            byte[] death = receiveUntilOpcode(
                    client, receiver, Protocol.S_ACTION, 8);
            assertEquals(2, unsignedInt(death, 1));
            assertEquals(BattleActionCodes.DIE, death[5] & 0xFF);
            assertTrue(hub.battleStarted(roomId),
                    "battle world was torn down in the lethal spell burst");

            client.setSoTimeout(350);
            try {
                receive(client, receiver);
                throw new AssertionError("S_D3 arrived before Disintegrate completed");
            } catch (SocketTimeoutException expected) {
                // Exact native 60-frame effect remains the only live controller.
            }

            client.setSoTimeout(3_500);
            byte[] result = receiveUntilOpcode(
                    client, receiver, Protocol.S_LITO_END_GAME, 12);
            long resultAt = System.nanoTime();
            assertEquals(Protocol.S_LITO_END_GAME, result[0] & 0xFF);
            assertTrue((resultAt - castAt) / 1_000_000L >= 2_400L,
                    "native result controller replaced the 2.5-second spell too early");
            long stateDeadline = System.nanoTime() + 500_000_000L;
            while (hub.battleStarted(roomId) && System.nanoTime() < stateDeadline) {
                Thread.onSpinWait();
            }
            assertFalse(hub.battleStarted(roomId));
        } finally {
            hub.close();
        }
    }

    @Test
    void lethalDisintegrateAgainstPlayerDefersConfirmedResultUntilEffectEnds()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("disintegrate-player-terminal-state.properties"), false);
        GameHub hub = new GameHub(config, state);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(700);
            ClientSession victim = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve(
                            "disintegrate-player-terminal-capture")), hub);
            initialize(victim, baseline());
            long roomId = startSoloBattle(hub, victim);
            setPhase(victim, "BATTLE");
            setField(victim, "currentRoomId", roomId);
            setField(victim, "playerMaxHp", 100);
            setField(victim, "playerHp", 1);

            int seed = 0x3810_77D4;
            setField(victim, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            long lethalAt = System.nanoTime();
            invoke(victim, "takePlayerDamage",
                    new Class<?>[] {int.class, ClientSession.class,
                            boolean.class, boolean.class, long.class},
                    1, null, true, false,
                    BattleSpellCatalog.DISINTEGRATE_EFFECT_COMPLETION_MILLIS);
            receiveUntilOpcode(client, receiver,
                    Protocol.S_LITO_CONFIRM_DEATH, 16);
            invoke(victim, "confirmDeath", new Class<?>[] {byte[].class},
                    new Protocol.Writer().c(
                            Protocol.C_LITO_CONFIRM_DEATH).bytes());
            assertTrue(hub.battleStarted(roomId));

            client.setSoTimeout(350);
            try {
                receive(client, receiver);
                throw new AssertionError(
                        "player result arrived before Disintegrate effect completed");
            } catch (SocketTimeoutException expected) {
                // No result controller may replace the live 11746/11748 effect.
            }

            client.setSoTimeout(3_500);
            byte[] result = receiveUntilOpcode(
                    client, receiver, Protocol.S_LITO_END_GAME, 12);
            long resultAt = System.nanoTime();
            assertEquals(Protocol.S_LITO_END_GAME, result[0] & 0xFF);
            assertTrue((resultAt - lethalAt) / 1_000_000L >= 2_400L,
                    "confirmed player death dismantled world before final effect tick");
            long stateDeadline = System.nanoTime() + 500_000_000L;
            while (hub.battleStarted(roomId) && System.nanoTime() < stateDeadline) {
                Thread.onSpinWait();
            }
            assertFalse(hub.battleStarted(roomId));
        } finally {
            hub.close();
        }
    }

    @Test
    void emptyFireStormStillEmitsNativeTypeZeroEffectEnvelope()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("fire-storm-empty-effect-state.properties"), false);
        GameHub hub = new GameHub(config, state);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(2_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve(
                            "fire-storm-empty-effect-capture")), hub);
            initialize(session, baseline());
            setField(session, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.FIRE_STORM));
            long roomId = startSoloBattle(hub, session);
            setPhase(session, "BATTLE");
            setField(session, "currentRoomId", roomId);
            BattleSpellCatalog.AreaAttackSpell fireStorm =
                    BattleSpellCatalog.areaAttackSpell(
                            BattleSpellCatalog.FIRE_STORM);
            ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(
                    hub.battleMapId(roomId, session));
            ArenaMapCatalog.Point bot = hub.botPosition(roomId);
            ArenaMapCatalog.Point caster = arena.spawns().stream()
                    .filter(point -> Math.max(Math.abs(point.x() - bot.x()),
                            Math.abs(point.y() - bot.y())) > fireStorm.areaRadius())
                    .findFirst().orElseThrow();
            setField(session, "positionX", caster.x());
            setField(session, "positionY", caster.y());
            setField(session, "playerHp", 100);
            setField(session, "playerMaxHp", 100);
            setField(session, "playerMp", 100);
            setField(session, "playerMaxMp", 100);

            int seed = 0x3810_70E0;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            invoke(session, "useCenteredAreaAttackSpell",
                    new Class<?>[] {byte[].class,
                            BattleSpellCatalog.AreaAttackSpell.class},
                    new Protocol.Writer().c(Protocol.C_USE_SPELL)
                            .c(8).c(5).bytes(), fireStorm);

            byte[] packet = receiveUntilOpcode(
                    client, receiver, Protocol.S_ATTACK_MANY, 8);
            Protocol.Reader attack = new Protocol.Reader(packet);
            assertEquals(Protocol.S_ATTACK_MANY, attack.c());
            assertEquals(fireStorm.actionId(), attack.c());
            attack.d();
            attack.h();
            attack.h();
            attack.c();
            attack.d();
            assertEquals(fireStorm.effectGfx(), attack.h());
            assertEquals(0, attack.c());
            assertEquals(0, attack.c());
            assertEquals("", attack.s());
            assertEquals(0, attack.h());
            assertEquals(0, attack.remaining());
        } finally {
            hub.close();
        }
    }

    @Test
    void battleSkillFiveTeleportUsesNativePacketDirectPortalAndUnlocksAfterRebuild()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("battle-skill-five-teleport-state.properties"), false);
        GameHub hub = new GameHub(config, state);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(2_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve(
                            "battle-skill-five-teleport-capture")), hub);
            initialize(session, baseline());
            CharacterSummary character = character("NativeTeleportWizard", 3,
                    0x3810_0005L);
            setField(session, "account", "test");
            setField(session, "characters", List.of(character));
            setField(session, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(character));
            setField(session, "sourcePhysicalGrowthStats",
                    SourcePhysicalGrowthStats.classic381());
            setField(session, "learnedSkills",
                    java.util.Set.of(BattleSpellCatalog.TELEPORT));
            long roomId = startSoloBattle(hub, session);
            setPhase(session, "BATTLE");
            setField(session, "currentRoomId", roomId);
            ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(
                    hub.battleMapId(roomId, session));
            ArenaMapCatalog.Point start = arena.spawn(0);
            setField(session, "positionX", start.x());
            setField(session, "positionY", start.y());
            setField(session, "playerHp", 100);
            setField(session, "playerMaxHp", 100);
            setField(session, "playerMp", 100);
            setField(session, "playerMaxMp", 100);

            int seed = 0x3810_0005;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(0).c(4).h(arena.mapId()).h(0).h(0).bytes();
            invoke(session, "useSpell", new Class<?>[] {byte[].class}, nativeRequest);

            boolean actionSeen = false;
            boolean effectSeen = false;
            boolean firstUnlockSeen = false;
            boolean portalSeen = false;
            for (int index = 0; index < 12 && !portalSeen; index++) {
                byte[] packet = receive(client, receiver);
                int opcode = packet[0] & 0xFF;
                assertFalse(opcode == Protocol.S_REQUEST_SUMMON,
                        "skill-5 teleport entered mass-teleport summon negotiation");
                actionSeen |= opcode == Protocol.S_ACTION;
                effectSeen |= opcode == Protocol.S_EFFECT;
                firstUnlockSeen |= opcode == Protocol.S_PARALYSE
                        && packet.length == 2
                        && (packet[1] & 0xFF)
                            == Protocol.ParalysisMode.TELEPORT_UNLOCK.wireValue();
                portalSeen = opcode == Protocol.S_PORTAL;
            }
            assertTrue(actionSeen, "skill-5 teleport omitted native cast action 19");
            assertTrue(effectSeen, "skill-5 teleport omitted native gfx 169");
            assertTrue(firstUnlockSeen,
                    "skill-5 teleport did not acknowledge the native cursor lock");
            assertTrue(portalSeen, "skill-5 teleport did not issue direct S_PORTAL");
            assertTrue((boolean) field(session, "pendingPortalReady"));
            Protocol.PortalDestination destination =
                    (Protocol.PortalDestination) field(session, "pendingPortal");
            assertNotNull(destination);
            assertEquals(arena.mapId(), destination.mapId());
            assertTrue(arena.nativeStandingFloor(destination.x(), destination.y()));

            invoke(session, "completePortal", new Class<?>[] {byte[].class},
                    new Protocol.Writer().c(Protocol.C_GOTO_PORTAL).bytes());
            boolean finalUnlock = false;
            for (int index = 0; index < 4_096 && !finalUnlock; index++) {
                byte[] packet = receive(client, receiver);
                int opcode = packet[0] & 0xFF;
                assertFalse(opcode == Protocol.S_REQUEST_SUMMON,
                        "skill-5 world rebuild entered summon negotiation");
                finalUnlock = opcode == Protocol.S_PARALYSE
                        && packet.length == 2
                        && (packet[1] & 0xFF)
                            == Protocol.ParalysisMode.TELEPORT_UNLOCK.wireValue();
            }
            assertTrue(finalUnlock,
                    "skill-5 teleport rebuild did not release mouse/action cursor");
            assertEquals(destination.x(), field(session, "positionX"));
            assertEquals(destination.y(), field(session, "positionY"));
            assertEquals(90, field(session, "playerMp"));
        } finally {
            hub.close();
        }
    }

    @Test
    void battleInstantTeleportUsesDirectPortalAndReleasesCursorAfterRebuild()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of(
                "src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("battle-teleport-direct-state.properties"), false);
        GameHub hub = new GameHub(config, state);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(2_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve(
                            "battle-teleport-direct-capture")), hub);
            initialize(session, baseline());
            CharacterSummary character = character("NativeEconomyKnight", 0,
                    0x3810_77E0L);
            setField(session, "account", "test");
            setField(session, "characters", List.of(character));
            setField(session, "sourceBaseMagicResistance",
                    SourceBaseMagicResistance.from(character));
            setField(session, "sourcePhysicalGrowthStats",
                    SourcePhysicalGrowthStats.classic381());
            long roomId = startSoloBattle(hub, session);
            setPhase(session, "BATTLE");
            setField(session, "currentRoomId", roomId);
            ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(
                    hub.battleMapId(roomId, session));
            ArenaMapCatalog.Point start = arena.spawn(0);
            ArenaMapCatalog.Point destination = arena.spawn(2);
            setField(session, "positionX", start.x());
            setField(session, "positionY", start.y());
            setField(session, "playerHp", 100);
            setField(session, "playerMaxHp", 100);
            setField(session, "playerMp", 100);
            setField(session, "playerMaxMp", 100);

            int seed = 0x3810_77E0;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            invoke(session, "requestTeleport", new Class<?>[] {byte[].class},
                    new Protocol.Writer().c(Protocol.C_TELEPORT)
                            .h(arena.mapId()).h(destination.x())
                            .h(destination.y()).bytes());

            boolean portalSeen = false;
            for (int index = 0; index < 8 && !portalSeen; index++) {
                byte[] packet = receive(client, receiver);
                int opcode = packet[0] & 0xFF;
                assertFalse(opcode == Protocol.S_REQUEST_SUMMON,
                        "instant teleport entered summon/cursor negotiation");
                portalSeen = opcode == Protocol.S_PORTAL;
            }
            assertTrue(portalSeen, "instant teleport did not issue direct S_PORTAL");
            assertTrue((boolean) field(session, "pendingPortalReady"));

            invoke(session, "completePortal", new Class<?>[] {byte[].class},
                    new Protocol.Writer().c(Protocol.C_GOTO_PORTAL).bytes());
            boolean finalUnlock = false;
            for (int index = 0; index < 4_096 && !finalUnlock; index++) {
                byte[] packet = receive(client, receiver);
                int opcode = packet[0] & 0xFF;
                assertFalse(opcode == Protocol.S_REQUEST_SUMMON,
                        "world rebuild re-entered summon negotiation");
                finalUnlock = opcode == Protocol.S_PARALYSE
                        && packet.length == 2
                        && (packet[1] & 0xFF)
                            == Protocol.ParalysisMode.TELEPORT_UNLOCK.wireValue();
            }
            assertTrue(finalUnlock,
                    "battle teleport rebuild did not release mouse/action cursor");
            assertEquals(destination.x(), field(session, "positionX"));
            assertEquals(destination.y(), field(session, "positionY"));
        } finally {
            hub.close();
        }
    }

    @Test
    void classicInstantTeleportUnlocksAfterRebuildAndKeepsOwnNameHoverOnly()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("classic-teleport-unlock-state.properties"), false);
        GameHub hub = new GameHub(config, state);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(2_000);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("classic-teleport-unlock-capture")),
                    hub);
            initialize(session, baseline());
            setField(session, "selectedClassicServerName",
                    config.classicServerNames().get(0));
            setField(session, "playerHp", 100);
            setField(session, "playerMaxHp", 100);
            int mapId = ClassicWorldMapCatalog.WORLD_TREE_MAP_ID;
            int startX = ClassicWorldMapCatalog.WORLD_TREE_X;
            int startY = ClassicWorldMapCatalog.WORLD_TREE_Y;
            var map = ClassicWorldMapCatalog.forMap(mapId);
            assertTrue(map.allowsRandomTeleport());
            ArenaMapCatalog.Point destination = null;
            for (int dy = -8; dy <= 8 && destination == null; dy++) {
                for (int dx = -8; dx <= 8; dx++) {
                    if ((dx != 0 || dy != 0)
                            && map.standingPassable(startX + dx, startY + dy)) {
                        destination = new ArenaMapCatalog.Point(startX + dx, startY + dy);
                        break;
                    }
                }
            }
            assertNotNull(destination);
            hub.connect(session);
            hub.enterClassicWorld(session, mapId, startX, startY,
                    ClassicWorldMapCatalog.WORLD_TREE_HEADING);
            setPhase(session, "WORLD");
            setField(session, "classicWorldMapId", mapId);
            setField(session, "classicWorldInstanceId", -1);
            setField(session, "positionX", startX);
            setField(session, "positionY", startY);
            setField(session, "playerHeading",
                    ClassicWorldMapCatalog.WORLD_TREE_HEADING);

            int seed = 0x3810_2E25;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);
            invoke(session, "requestTeleport", new Class<?>[] {byte[].class},
                    new Protocol.Writer().c(Protocol.C_TELEPORT)
                            .h(destination.x()).h(destination.y()).bytes());

            byte[] firstUnlock = receive(client, receiver);
            assertEquals(Protocol.S_PARALYSE, firstUnlock[0] & 0xFF);
            assertEquals(Protocol.ParalysisMode.TELEPORT_UNLOCK.wireValue(),
                    firstUnlock[1] & 0xFF);
            receiveUntilOpcode(client, receiver, Protocol.S_PORTAL, 4);

            invoke(session, "completePortal", new Class<?>[] {byte[].class},
                    new Protocol.Writer().c(Protocol.C_GOTO_PORTAL).bytes());
            boolean ownActorHoverOnly = false;
            boolean rebuiltUnlock = false;
            for (int index = 0; index < 4_096 && !rebuiltUnlock; index++) {
                byte[] packet = receive(client, receiver);
                int opcode = packet[0] & 0xFF;
                if (opcode == Protocol.S_PUT_OBJECT
                        && battleActorObjectId(packet) == session.playerObjectId()) {
                    ownActorHoverOnly = battleActorHpRatio(packet) == 0xFF;
                }
                if (opcode == Protocol.S_PARALYSE && packet.length == 2
                        && (packet[1] & 0xFF)
                            == Protocol.ParalysisMode.TELEPORT_UNLOCK.wireValue()) {
                    rebuiltUnlock = true;
                }
            }
            assertTrue(ownActorHoverOnly,
                    "local actor supplied a persistent HP/name overlay ratio");
            assertTrue(rebuiltUnlock,
                    "world rebuild did not release the native mouse/action lock");
            assertEquals(destination.x(), field(session, "positionX"));
            assertEquals(destination.y(), field(session, "positionY"));
        } finally {
            hub.close();
        }
    }

    private static long battleActorObjectId(byte[] packet) {
        Protocol.Reader actor = new Protocol.Reader(packet);
        if (actor.c() != Protocol.S_PUT_OBJECT) return 0;
        actor.h();
        actor.h();
        return actor.d();
    }

    private static int battleActorHpRatio(byte[] packet) {
        Protocol.Reader actor = new Protocol.Reader(packet);
        assertEquals(Protocol.S_PUT_OBJECT, actor.c());
        actor.h();
        actor.h();
        actor.d();
        actor.h();
        actor.c();
        actor.c();
        actor.c();
        actor.c();
        actor.d();
        actor.h();
        actor.s();
        actor.s();
        actor.c();
        actor.d();
        actor.s();
        actor.s();
        actor.c();
        return actor.c();
    }

    private static RuntimeInventory baseline() {
        RuntimeInventory baseline = new RuntimeInventory();
        baseline.addDistinctState(InventoryCatalog.require(InventoryCatalog.SHORT_SWORD),
                new RuntimeInventory.PersistentItem(
                        0x7000_3810L, 1, 0, 0, true, 1, true, true));
        return baseline;
    }

    private static void initialize(ClientSession session, RuntimeInventory baseline)
            throws Exception {
        setField(session, "account", "native-economy-test");
        setField(session, "selectedCharacter", "NativeEconomyKnight");
        setField(session, "inventory", baseline.copy());
        SourceEquipmentMagicStats magic = SourceEquipmentMagicStats.none();
        SourceEquipmentVitalStats vital = SourceEquipmentVitalStats.none();
        SourceEquippedFireResistance fire = new SourceEquippedFireResistance(0, 0);
        setField(session, "sourceEquipmentMagicStats", magic);
        setField(session, "sourceEquipmentVitalStats", vital);
        setField(session, "sourceEquippedFireResistance", fire);
        setField(session, "nativeTournamentMagicBaseline", magic);
        setField(session, "nativeTournamentVitalBaseline", vital);
        setField(session, "nativeTournamentFireResistanceBaseline", fire);
        setField(session, "sourceInventoryWeightStats", SourceInventoryWeightStats.none(100.0));
        setField(session, "retainedSourceInventoryWeight", 0);
        setField(session, "shopPoints", 20_000L);
        setField(session, "nativeTournamentEconomyInitialized", true);
    }

    private static void invokeReset(ClientSession session, String reason) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod(
                "resetNativeTournamentEconomy", String.class);
        method.setAccessible(true);
        method.invoke(session, reason);
    }

    private static void invoke(ClientSession session, String name, Class<?>[] parameters,
                               Object... arguments) throws Exception {
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

    private static long startSoloBattle(GameHub hub, ClientSession session)
            throws Exception {
        hub.connect(session);
        hub.enterChannel(0, session);
        long roomId = hub.createRoom(0, new GameHub.RoomSettings(
                "Native Economy", "", 2,
                BattleRules.Mode.DEATHMATCH.wireId(), 0, 0, 0, 0));
        hub.joinRoom(roomId, session);
        hub.setReady(roomId, session, true);
        Method countdown = GameHub.class.getDeclaredMethod(
                "countdownWork", long.class);
        countdown.setAccessible(true);
        long future = System.nanoTime() + 10_000_000_000L;
        countdown.invoke(hub, future);
        countdown.invoke(hub, future + 2_000_000_000L);
        countdown.invoke(hub, future + 4_000_000_000L);
        assertTrue(hub.botPosition(roomId) != null);
        return roomId;
    }

    private static byte[] receive(Socket client, Protocol.CipherState receiver)
            throws Exception {
        return receiver.decrypt(Protocol.readFrame(client.getInputStream(), 8_192));
    }

    private static byte[] receiveUntilOpcode(
            Socket client, Protocol.CipherState receiver,
            int opcode, int maximumFrames) throws Exception {
        for (int index = 0; index < maximumFrames; index++) {
            byte[] packet = receive(client, receiver);
            if ((packet[0] & 0xFF) == opcode) return packet;
        }
        throw new AssertionError("missing encrypted opcode " + opcode
                + " within " + maximumFrames + " frames");
    }

    private static int zeroTerminatedEnd(byte[] payload, int offset) {
        while (offset < payload.length && payload[offset] != 0) offset++;
        if (offset >= payload.length) throw new AssertionError("unterminated native string");
        return offset + 1;
    }

    private static int unsignedShort(byte[] payload, int offset) {
        return (payload[offset] & 0xFF) | ((payload[offset + 1] & 0xFF) << 8);
    }

    private static long unsignedInt(byte[] payload, int offset) {
        return Integer.toUnsignedLong((payload[offset] & 0xFF)
                | ((payload[offset + 1] & 0xFF) << 8)
                | ((payload[offset + 2] & 0xFF) << 16)
                | ((payload[offset + 3] & 0xFF) << 24));
    }

    private static RuntimeInventory inventory(ClientSession session) throws Exception {
        return (RuntimeInventory) field(session, "inventory");
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

    private record Fixture(ClientSession session, GameStateStore state,
                           RuntimeInventory baseline) {}

    private static String shopNotice(Socket socket, Protocol.CipherState cipher) throws Exception {
        Protocol.Reader packet = new Protocol.Reader(receive(socket, cipher));
        assertEquals(Protocol.S_MESSAGE, packet.c());
        assertEquals(9, packet.c());
        return packet.s();
    }

    @Test
    void shopTransactionsAreAtomicVisibleAndPersistBeforePublish() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        Path file = temporary.resolve("atomic-shop.properties");
        GameStateStore store = new GameStateStore(file, true);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(1000);
            ClientSession session = new ClientSession(server, ClientSession.Endpoint.GAME,
                    config, null, store, new CaptureLog(temporary.resolve("atomic-capture")), hub);
            initialize(session, baseline());
            setPhase(session, "CHANNEL");
            int seed = 0x38100117;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState cipher = new Protocol.CipherState(seed);
            String identity = (String) invokeResult(session, "stateCharacter", new Class<?>[0]);
            store.saveEconomyState(identity, 20000, inventory(session).catalogCounts(),
                    inventory(session).catalogItemStates());
            byte[] originalFile = LocalResources.readAllBytes(file);
            long potion = InventoryCatalog.EXTRA_HEALING_POTION;
            // The first row is affordable; the second must cancel the ENTIRE order.
            byte[][] rejected = {
                new Protocol.Writer().c(0xDB).c(0).h(2).d(potion).d(1)
                    .d(InventoryCatalog.MITHRIL_ARROW).d(100000).bytes(),
                new Protocol.Writer().c(0xDB).c(0).h(1).d(potion).d(0).bytes(),
                new Protocol.Writer().c(0xDB).c(0).h(1).d(0x7FFFFFFFL).d(1).bytes()
            };
            String[] reasons = {"金幣不足", "數量無效", "無法交易"};
            for (int i=0;i<rejected.length;i++) {
                invoke(session, "sendBuyList", new Class<?>[0]); receive(client,cipher);
                invoke(session,"buySell",new Class<?>[]{byte[].class},rejected[i]);
                assertEquals(Protocol.S_LITO_POINT,receive(client,cipher)[0]&255);
                assertTrue(shopNotice(client,cipher).contains(reasons[i]));
                assertEquals(20000L,field(session,"shopPoints"));
                assertEquals(baseline().catalogCounts(),inventory(session).catalogCounts());
                assertTrue(java.util.Arrays.equals(originalFile,LocalResources.readAllBytes(file)));
            }
            // An unwritable temp destination forces a real disk commit failure.
            Path blocked = file.resolveSibling(file.getFileName()+".tmp");
            Files.createDirectory(blocked);
            byte[] buy = new Protocol.Writer().c(0xDB).c(0).h(2)
                    .d(potion).d(1).d(potion).d(1).bytes();
            invoke(session,"sendBuyList",new Class<?>[0]); receive(client,cipher);
            invoke(session,"buySell",new Class<?>[]{byte[].class},buy);
            receive(client,cipher);
            assertTrue(shopNotice(client,cipher).contains("儲存失敗"));
            assertEquals(20000L,field(session,"shopPoints"));
            assertEquals(20000L,store.economy(identity).points());
            assertEquals(baseline().catalogCounts(),store.economy(identity).inventory());
            assertEquals(baseline().catalogCounts(),inventory(session).catalogCounts());
            assertTrue(java.util.Arrays.equals(originalFile,LocalResources.readAllBytes(file)));
            Files.delete(blocked);
            invoke(session,"sendBuyList",new Class<?>[0]);receive(client,cipher);
            invoke(session,"buySell",new Class<?>[]{byte[].class},buy);
            assertEquals(Protocol.S_ADD_INVENTORY,receive(client,cipher)[0]&255);
            assertEquals(Protocol.S_LITO_POINT,receive(client,cipher)[0]&255);
            assertTrue(shopNotice(client,cipher).contains("購買成功"));
            assertEquals(19600L,field(session,"shopPoints"));
            assertEquals(2,inventory(session).catalogCounts().get(potion));
            assertEquals(inventory(session).catalogCounts(),new GameStateStore(file,true).economy(identity).inventory());
            long object = inventory(session).items().stream().filter(it->it.definition().catalogId()==potion).findFirst().orElseThrow().objectId();
            invoke(session,"sendSellList",new Class<?>[0]);receive(client,cipher);
            invoke(session,"buySell",new Class<?>[]{byte[].class},new Protocol.Writer().c(0xDB).c(1).h(1).d(object).d(3).bytes());
            receive(client,cipher);assertTrue(shopNotice(client,cipher).contains("無效"));
            assertEquals(19600L,field(session,"shopPoints"));assertEquals(2,inventory(session).catalogCounts().get(potion));
            invoke(session,"sendSellList",new Class<?>[0]);receive(client,cipher);
            invoke(session,"buySell",new Class<?>[]{byte[].class},new Protocol.Writer().c(0xDB).c(1).h(1).d(object).d(2).bytes());
            assertEquals(Protocol.S_REMOVE_INVENTORY,receive(client,cipher)[0]&255);
            assertEquals(Protocol.S_LITO_POINT,receive(client,cipher)[0]&255);assertTrue(shopNotice(client,cipher).contains("出售成功"));
            assertEquals(20000L,field(session,"shopPoints"));assertNull(inventory(session).item(object));
        } finally { hub.close(); }
    }

    @Test
    @SuppressWarnings("unchecked")
    void sharedSpellCooldownBlocksDifferentSkillsAndUnlocksIdleTeleport() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("config/server.properties"));
        GameStateStore state = new GameStateStore(temporary.resolve("shared-cooldown.properties"),false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"));
             Socket client=new Socket("127.0.0.1",listener.getLocalPort());Socket server=listener.accept()) {
            client.setSoTimeout(150);
            ClientSession session=new ClientSession(server,ClientSession.Endpoint.GAME,config,null,state,
                    new CaptureLog(temporary.resolve("shared-cooldown-capture")),hub);
            initialize(session,baseline());setPhase(session,"BATTLE");
            setField(session,"playerHp",100);setField(session,"playerMaxHp",100);
            setField(session,"playerMp",100);setField(session,"playerMaxMp",100);
            setField(session,"learnedSkills",java.util.Set.of(3,5));
            int seed=0x38101801;setField(session,"outgoing",new Protocol.CipherState(seed));
            Protocol.CipherState cipher=new Protocol.CipherState(seed);
            Map<Integer,Long> deadlines=(Map<Integer,Long>)field(session,"spellReadyNanos");
            deadlines.put(77,System.nanoTime()+10_000_000_000L);
            byte[] shield=new Protocol.Writer().c(Protocol.C_USE_SPELL).c(0).c(2).bytes();
            for(String phase:List.of("BATTLE","WORLD")) {
                setPhase(session,phase);
                invoke(session,"useSpell",new Class<?>[]{byte[].class},shield);
                assertEquals(100,field(session,"playerMp"),"different skill bypassed global reuse in "+phase);
                assertFalse(deadlines.containsKey(3));
                try {receive(client,cipher);throw new AssertionError("rejected cast emitted effect");}
                catch(SocketTimeoutException expected){}
                byte[] teleport=new Protocol.Writer().c(Protocol.C_USE_SPELL).c(0).c(4).h(0).h(0).h(0).bytes();
                invoke(session,"useSpell",new Class<?>[]{byte[].class},teleport);
                byte[] unlock=receive(client,cipher);
                assertEquals(Protocol.S_PARALYSE,unlock[0]&255);assertEquals(7,unlock[1]&255);
                setField(session,"pendingPortalReady",true);
                invoke(session,"useSpell",new Class<?>[]{byte[].class},teleport);
                try {receive(client,cipher);throw new AssertionError("active portal incorrectly unlocked");}
                catch(SocketTimeoutException expected){}
                setField(session,"pendingPortalReady",false);
            }
            setPhase(session,"BATTLE");deadlines.put(77,System.nanoTime()-1);
            invoke(session,"useSpell",new Class<?>[]{byte[].class},shield);
            assertEquals(92,field(session,"playerMp"),"expired reuse did not allow normal cast");
            assertTrue(deadlines.containsKey(3));
        } finally {hub.close();}
    }
}

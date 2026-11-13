package local.lito381.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
final class ClientSessionNativeRentalTownCitizenshipTest {
    @TempDir Path temporary;

    @Test void catalogPinsExactLtIdentityAndPassableProvisionalPlacement() {
        List<NativeLitoRentalTownNpcCatalog.Npc> npcs =
                NativeLitoRentalTownNpcCatalog.forMap(500);
        assertEquals(13, npcs.size());
        assertTrue(NativeLitoRentalTownNpcCatalog.forMap(560).isEmpty());
        assertFalse(NativeLitoRentalTownNpcCatalog.HISTORICAL_ACTOR_COORDINATES_EXACT);

        String[] names = {"指導員-潘塔休", "血盟管理員-蘇耳", "武器商人-瑪雅",
                "首席傳送師-伊奇", "旅館主人-阿里亞"};
        String[] html = {"Phan_Start", "Soul_Start", "Maya_Start",
                "Ezee_Start", "Aria_Start"};
        NativeLitoTownPassabilityCatalog passability =
                NativeLitoTownPassabilityCatalog.get();
        int[] actorGfx = {
                NativeLitoRentalTownNpcCatalog.GUIDE_GFX,
                NativeLitoRentalTownNpcCatalog.CLAN_MANAGER_GFX,
                NativeLitoRentalTownNpcCatalog.WEAPON_MERCHANT_GFX,
                NativeLitoRentalTownNpcCatalog.LEAD_TELEPORTER_GFX,
                NativeLitoRentalTownNpcCatalog.INNKEEPER_GFX};
        for (int index = 0; index < names.length; index++) {
            NativeLitoRentalTownNpcCatalog.Npc npc = npcs.get(index);
            assertEquals(index, npc.resourceIndex());
            assertEquals(actorGfx[index], npc.gfxId());
            assertTrue(BattleAnimationCatalog.get().supportsTimedAction(
                    npc.gfxId(), BattleActionCodes.WALK));
            assertTrue(npc.gfxId() < Protocol.LITO_RESOURCE_WIRE_BASE);
            assertEquals(0x6C10_0000L + index, npc.objectId());
            assertEquals(names[index], npc.name());
            assertEquals(html[index], npc.startHtml());
            assertTrue(passability.isStandingPassable(500, npc.x(), npc.y()));
            Protocol.Reader actor = new Protocol.Reader(
                    Protocol.litoRentalTownNpc(npc));
            assertEquals(Protocol.S_PUT_OBJECT, actor.c());
            assertEquals(npc.x(), actor.h());
            assertEquals(npc.y(), actor.h());
            assertEquals(npc.objectId(), actor.d());
            assertEquals(npc.gfxId(), actor.h());
            assertEquals(0, actor.c());
            assertEquals(npc.heading(), actor.c());
        }

        int[] serviceResources = {10, 11, 12, 91};
        String[] serviceNames = {"防具商人-菲佛", "防具商人-克里",
                "飾品商人-勒夫", "旅館服務生-席巴"};
        String[] serviceHtml = {"Paper_start", "Click_Start", "Love_Start", "inn"};
        NativeLitoRentalTownNpcCatalog.Role[] serviceRoles = {
                NativeLitoRentalTownNpcCatalog.Role.RENTAL_MERCHANT,
                NativeLitoRentalTownNpcCatalog.Role.RENTAL_MERCHANT,
                NativeLitoRentalTownNpcCatalog.Role.RENTAL_MERCHANT,
                NativeLitoRentalTownNpcCatalog.Role.HOTEL_SERVICE};
        for (int index = 0; index < serviceResources.length; index++) {
            NativeLitoRentalTownNpcCatalog.Npc npc = npcs.get(index + 5);
            assertEquals(serviceResources[index], npc.resourceIndex());
            assertTrue(BattleAnimationCatalog.get().supportsTimedAction(
                    npc.gfxId(), BattleActionCodes.WALK));
            assertTrue(npc.gfxId() < Protocol.LITO_RESOURCE_WIRE_BASE);
            assertEquals(0x6C10_0000L + serviceResources[index], npc.objectId());
            assertEquals(serviceNames[index], npc.name());
            assertEquals(serviceHtml[index], npc.startHtml());
            assertEquals(serviceRoles[index], npc.role());
            assertTrue(passability.isStandingPassable(500, npc.x(), npc.y()));
        }

        int[] teleporterResources = {92, 93, 94, 95};
        String[] teleporterNames = {"傳送師-尤依琳", "傳送師-艾恩",
                "傳送師-尤蘿雅", "傳送師-阿霞"};
        for (int index = 0; index < teleporterResources.length; index++) {
            NativeLitoRentalTownNpcCatalog.Npc npc = npcs.get(index + 9);
            assertEquals(teleporterResources[index], npc.resourceIndex());
            assertTrue(BattleAnimationCatalog.get().supportsTimedAction(
                    npc.gfxId(), BattleActionCodes.WALK));
            assertTrue(npc.gfxId() < Protocol.LITO_RESOURCE_WIRE_BASE);
            assertEquals(0x6C10_0000L + teleporterResources[index], npc.objectId());
            assertEquals(teleporterNames[index], npc.name());
            assertEquals(NativeLitoRentalTownNpcCatalog.TELEPORT_DEFAULT_HTML,
                    npc.startHtml());
            assertEquals(NativeLitoRentalTownNpcCatalog.Role.TELEPORT_SERVICE,
                    npc.role());
            assertTrue(passability.isStandingPassable(500, npc.x(), npc.y()));
        }
    }

    @Test void encryptedDialogActionChainGrantsFourEmblemsAndCompletesCitizenship()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("citizenship-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(250);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("citizenship-capture")), hub);
            setPhase(session, "BATTLE");
            setField(session, "selectedCharacter", "TownCitizen");
            setField(session, "rentalTownRoomState", true);
            setField(session, "rentalTownMapId", 500);
            setField(session, "currentRoomId", 0x5000_0001L);
            setField(session, "rentalTownLightGemCount", 20L);
            int seed = 0x3810_0640;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            List<NativeLitoRentalTownNpcCatalog.Npc> npcs =
                    NativeLitoRentalTownNpcCatalog.forMap(500);
            for (NativeLitoRentalTownNpcCatalog.Npc npc : npcs) {
                if (npc.role() != NativeLitoRentalTownNpcCatalog.Role.EMBLEM) continue;
                setField(session, "positionX", npc.x());
                setField(session, "positionY", npc.y());

                handle(session, new Protocol.Writer().c(Protocol.C_DIALOG)
                        .d(npc.objectId()).bytes());
                assertArrayEquals(Protocol.hypertext(
                                npc.objectId(), npc.startHtml(), "", List.of()),
                        receive(client, receiver));

                handle(session, action(npc.objectId(), npc.action(), ""));
                long emblemObjectId = NativeLitoRentalTownNpcCatalog.emblemObjectId(
                        npc.emblemResourceIndex());
                assertArrayEquals(Protocol.addInventory(
                                Protocol.litoRentalTownEmblem(emblemObjectId,
                                        npc.emblemResourceIndex(), npc.emblemName())),
                        receive(client, receiver));
                assertArrayEquals(Protocol.hypertext(
                                npc.objectId(), npc.endHtml(), "", List.of()),
                        receive(client, receiver));
            }

            assertEquals(Set.of(6, 7, 8, 9),
                    Set.copyOf(numberSetField(session, "rentalTownEmblemsGranted")));
            assertEquals(Set.of(6, 7, 8, 9),
                    Set.copyOf(numberSetField(session, "rentalTownEmblemsHeld")));

            NativeLitoRentalTownNpcCatalog.Npc guide = npcs.get(0);
            setField(session, "positionX", guide.x());
            setField(session, "positionY", guide.y());
            handle(session, new Protocol.Writer().c(Protocol.C_DIALOG)
                    .d(guide.objectId()).bytes());
            assertArrayEquals(Protocol.hypertext(
                            guide.objectId(), guide.startHtml(), "", List.of()),
                    receive(client, receiver));

            handle(session, action(guide.objectId(), guide.action(), ""));
            for (int resourceIndex : List.of(6, 7, 8, 9)) {
                assertArrayEquals(Protocol.removeInventory(
                                NativeLitoRentalTownNpcCatalog.emblemObjectId(resourceIndex)),
                        receive(client, receiver));
            }
            assertArrayEquals(Protocol.changeInventoryCount(0x6C00_0002L, 21),
                    receive(client, receiver));
            assertArrayEquals(Protocol.hypertext(
                            guide.objectId(), guide.endHtml(), "", List.of()),
                    receive(client, receiver));
            assertTrue((boolean) field(session, "rentalTownCitizen"));
            assertTrue(numberSetField(session, "rentalTownEmblemsHeld").isEmpty());
            assertEquals(21L, field(session, "rentalTownLightGemCount"));

            NativeLitoRentalTownNpcCatalog.Npc soul = npcs.get(1);
            setField(session, "positionX", soul.x());
            setField(session, "positionY", soul.y());
            handle(session, action(soul.objectId(), soul.action(), ""));
            assertArrayEquals(Protocol.hypertext(
                            soul.objectId(), soul.endHtml(), "", List.of()),
                    receive(client, receiver));
            assertThrows(SocketTimeoutException.class,
                    () -> receive(client, receiver));
        }
    }

    @Test void wrongObjectActionDistanceAndParameterCannotMintAnEmblem()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("negative-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(150);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("negative-capture")), hub);
            setPhase(session, "BATTLE");
            setField(session, "rentalTownRoomState", true);
            setField(session, "rentalTownMapId", 500);
            setField(session, "positionX", 32770);
            setField(session, "positionY", 32818);
            setField(session, "outgoing", new Protocol.CipherState(0x3810_0641));

            NativeLitoRentalTownNpcCatalog.Npc soul =
                    NativeLitoRentalTownNpcCatalog.forMap(500).get(1);
            handle(session, action(soul.objectId(), soul.action(), "unexpected"));
            handle(session, action(soul.objectId(),
                    NativeLitoRentalTownNpcCatalog.REQUEST_PEACE, ""));
            handle(session, action(0x6C10_00FFL, soul.action(), ""));

            assertTrue(numberSetField(session, "rentalTownEmblemsGranted").isEmpty());
            assertThrows(SocketTimeoutException.class,
                    () -> Protocol.readFrame(client.getInputStream(), 8_192));
        }
    }

    @Test void MayaRetainedLightGemCatalogRentsAndRefundsOneWeapon()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("maya-rental-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(150);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("maya-rental-capture")), hub);
            setPhase(session, "BATTLE");
            setField(session, "selectedCharacter", "RentalReturner");
            setField(session, "characters", List.of(new CharacterSummary(
                    "RentalReturner", "", 1, 0, 50, 500, 100,
                    10, 13, 10, 10, 9, 8, 11, 8, 11,
                    0x5000_0641L, 0, 0, 0, 0, 0, 0, 0)));
            setField(session, "rentalTownRoomState", true);
            setField(session, "rentalTownMapId", 500);
            setField(session, "currentRoomId", 0x5000_0001L);
            setField(session, "rentalTownLightGemCount", 20L);
            NativeLitoRentalTownNpcCatalog.Npc maya =
                    NativeLitoRentalTownNpcCatalog.forMap(500).get(
                            NativeLitoRentalTownNpcCatalog.MAYA_RESOURCE_INDEX);
            setField(session, "positionX", maya.x());
            setField(session, "positionY", maya.y());
            int seed = 0x3810_0641;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            handle(session, action(maya.objectId(),
                    NativeLitoRentalTownNpcCatalog.RETURN_ACTION, ""));
            assertArrayEquals(Protocol.hypertext(
                            maya.objectId(),
                            NativeLitoRentalTownNpcCatalog.NO_RENT_HTML,
                            "", List.of(maya.name())),
                    receive(client, receiver));

            handle(session, action(maya.objectId(),
                    NativeLitoRentalTownNpcCatalog.RENT_ACTION, ""));
            byte[] buyList = receive(client, receiver);
            Protocol.Reader buyReader = new Protocol.Reader(buyList);
            assertEquals(Protocol.S_BUY_LIST, buyReader.c());
            assertEquals(maya.objectId(), buyReader.d());
            assertEquals(25, buyReader.h());
            assertFalse(NativeLitoRentalMerchantCatalog.HISTORICAL_OFFICIAL_EXACT);
            assertTrue(NativeLitoRentalTownNpcCatalog
                    .RETAINED_RENTAL_PRICE_CATALOG_AVAILABLE);
            assertFalse(NativeLitoRentalTownNpcCatalog
                    .HISTORICAL_RENTAL_PRICE_CATALOG_AVAILABLE);

            handle(session, new Protocol.Writer()
                    .c(Protocol.C_BUY_SELL).d(maya.objectId())
                    .c(0).c(1).c(0).d(0).d(1).bytes());
            RuntimeInventory rented = (RuntimeInventory) field(
                    session, "rentalTownInventory");
            RuntimeInventory.Item weapon = rented.items().stream()
                    .filter(item -> item.definition().catalogId() == 19000)
                    .findFirst().orElseThrow();
            assertArrayEquals(Protocol.addInventory(weapon.wireItem()),
                    receive(client, receiver));
            assertArrayEquals(Protocol.changeInventoryCount(
                            0x6C00_0002L, 10),
                    receive(client, receiver));
            assertEquals(10L, field(session, "rentalTownLightGemCount"));
            assertTrue(weapon.timed());

            handle(session, action(maya.objectId(),
                    NativeLitoRentalTownNpcCatalog.RETURN_ACTION, ""));
            assertArrayEquals(Protocol.classicSellList(
                            maya.objectId(), List.of(
                                    new Protocol.ClassicSellListRow(
                                            weapon.objectId(), 10))),
                    receive(client, receiver));
            handle(session, new Protocol.Writer()
                    .c(Protocol.C_BUY_SELL).d(maya.objectId())
                    .c(1).c(1).c(0).d(weapon.objectId()).d(1).bytes());
            assertArrayEquals(Protocol.removeInventory(weapon.objectId()),
                    receive(client, receiver));
            assertArrayEquals(Protocol.changeInventoryCount(
                            0x6C00_0002L, 20),
                    receive(client, receiver));
            assertEquals(20L, field(session, "rentalTownLightGemCount"));
            assertTrue(((RuntimeInventory) field(
                    session, "rentalTownInventory")).items().isEmpty());

            handle(session, action(maya.objectId(),
                    NativeLitoRentalTownNpcCatalog.RENT_ACTION, ""));
            receive(client, receiver);
            handle(session, new Protocol.Writer()
                    .c(Protocol.C_BUY_SELL).d(maya.objectId())
                    .c(0).c(1).c(0).d(0).d(1).bytes());
            RuntimeInventory.Item expiring = ((RuntimeInventory) field(
                    session, "rentalTownInventory")).items().stream()
                    .filter(item -> item.definition().catalogId() == 19000)
                    .findFirst().orElseThrow();
            assertArrayEquals(Protocol.addInventory(expiring.wireItem()),
                    receive(client, receiver));
            receive(client, receiver);
            session.serverExpireTimedInventory(
                    expiring.expiresAtEpochMillis() + 1);
            assertArrayEquals(Protocol.removeInventory(expiring.objectId()),
                    receive(client, receiver));
            for (int index = 0; index < 6; index++) {
                receive(client, receiver);
            }
            assertTrue(((RuntimeInventory) field(
                    session, "rentalTownInventory")).items().isEmpty());
            assertEquals(10L, field(session, "rentalTownLightGemCount"));

            handle(session, action(maya.objectId(),
                    NativeLitoRentalTownNpcCatalog.RETURN_ACTION, "unexpected"));
            assertThrows(SocketTimeoutException.class,
                    () -> Protocol.readFrame(client.getInputStream(), 8_192));
        }
    }

    @Test void auxiliaryMerchantsAndShebaExposeNativeHotelFlowWithoutInventedKeyItem()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("auxiliary-service-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(150);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("auxiliary-service-capture")), hub);
            setPhase(session, "BATTLE");
            setField(session, "selectedCharacter", "ServiceVisitor");
            setField(session, "characters", List.of(new CharacterSummary(
                    "ServiceVisitor", "", 1, 0, 50, 500, 100,
                    10, 13, 10, 10, 9, 8, 11, 8, 11,
                    0x5000_0100L, 0, 0, 0, 0, 0, 0, 0)));
            setField(session, "rentalTownRoomState", true);
            setField(session, "rentalTownMapId", 500);
            setField(session, "currentRoomId", 0x5000_0001L);
            int seed = 0x3810_0642;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            for (int resourceIndex : List.of(10, 11, 12)) {
                NativeLitoRentalTownNpcCatalog.Npc merchant = npc(resourceIndex);
                setField(session, "positionX", merchant.x());
                setField(session, "positionY", merchant.y());
                handle(session, new Protocol.Writer().c(Protocol.C_DIALOG)
                        .d(merchant.objectId()).bytes());
                assertArrayEquals(Protocol.hypertext(
                                merchant.objectId(), merchant.startHtml(), "", List.of()),
                        receive(client, receiver));
                handle(session, action(merchant.objectId(),
                        NativeLitoRentalTownNpcCatalog.RETURN_ACTION, ""));
                assertArrayEquals(Protocol.hypertext(
                                merchant.objectId(),
                                NativeLitoRentalTownNpcCatalog.NO_RENT_HTML,
                                "", List.of(merchant.name())),
                        receive(client, receiver));
                handle(session, action(merchant.objectId(),
                        NativeLitoRentalTownNpcCatalog.RENT_ACTION, ""));
                Protocol.Reader list = new Protocol.Reader(receive(client, receiver));
                assertEquals(Protocol.S_BUY_LIST, list.c());
                assertEquals(merchant.objectId(), list.d());
                assertEquals(NativeLitoRentalMerchantCatalog
                        .offeredBy(resourceIndex).size(), list.h());
            }

            NativeLitoRentalTownNpcCatalog.Npc sheba =
                    npc(NativeLitoRentalTownNpcCatalog.SHEBA_RESOURCE_INDEX);
            setField(session, "positionX", sheba.x());
            setField(session, "positionY", sheba.y());
            handle(session, new Protocol.Writer().c(Protocol.C_DIALOG)
                    .d(sheba.objectId()).bytes());
            assertArrayEquals(Protocol.hypertext(
                            sheba.objectId(), "inn", "", List.of()),
                    receive(client, receiver));
            handle(session, action(sheba.objectId(),
                    NativeLitoRentalTownNpcCatalog.HOTEL_ROOM_ACTION, ""));
            assertArrayEquals(Protocol.hypertextInput(
                            sheba.objectId(), 0, 1, 1, 8, 0,
                            NativeLitoRentalHotelCatalog.INPUT_HTML,
                            "", "", List.of(sheba.name())),
                    receive(client, receiver));
            handle(session, new Protocol.Writer()
                    .c(Protocol.C_HYPERTEXT_INPUT_RESULT)
                    .d(sheba.objectId()).d(5).c(0).s("").bytes());
            for (int ignored = 0; ignored < 5; ignored++) {
                assertEquals(Protocol.S_ADD_INVENTORY,
                        receive(client, receiver)[0] & 0xFF);
            }
            assertArrayEquals(Protocol.hypertext(
                            sheba.objectId(), NativeLitoRentalHotelCatalog.RENTED_HTML,
                            "", List.of(sheba.name())),
                    receive(client, receiver));
            assertNotNull(field(session, "rentalHotelLease"));

            handle(session, action(sheba.objectId(),
                    NativeLitoRentalTownNpcCatalog.HOTEL_RETURN_ACTION, ""));
            assertArrayEquals(Protocol.hypertext(
                            sheba.objectId(), NativeLitoRentalHotelCatalog.RETURNED_HTML,
                            "", List.of(sheba.name())),
                    receive(client, receiver));
            for (int ignored = 0; ignored < 5; ignored++) {
                assertEquals(Protocol.S_REMOVE_INVENTORY,
                        receive(client, receiver)[0] & 0xFF);
            }
            assertNull(field(session, "rentalHotelLease"));

            handle(session, action(sheba.objectId(),
                    NativeLitoRentalTownNpcCatalog.HOTEL_ROOM_ACTION, ""));
            assertArrayEquals(Protocol.hypertextInput(
                            sheba.objectId(), 0, 1, 1, 8, 0,
                            NativeLitoRentalHotelCatalog.INPUT_HTML,
                            "", "", List.of(sheba.name())),
                    receive(client, receiver));
            handle(session, new Protocol.Writer()
                    .c(Protocol.C_HYPERTEXT_INPUT_RESULT)
                    .d(sheba.objectId()).d(5).c(0).s("").bytes());
            for (int ignored = 0; ignored < 5; ignored++) {
                assertEquals(Protocol.S_ADD_INVENTORY,
                        receive(client, receiver)[0] & 0xFF);
            }
            assertArrayEquals(Protocol.hypertext(
                            sheba.objectId(), NativeLitoRentalHotelCatalog.RENTED_HTML,
                            "", List.of(sheba.name())),
                    receive(client, receiver));
            handle(session, action(sheba.objectId(),
                    NativeLitoRentalTownNpcCatalog.HOTEL_ENTER_ACTION, ""));
            NativeLitoRentalHotelCatalog.Room hall =
                    NativeLitoRentalHotelCatalog.forKeyCount(5);
            assertArrayEquals(Protocol.portal(
                            hall.mapId(), hall.entryX(), hall.entryY()),
                    receive(client, receiver));
            assertNotNull(field(session, "pendingRentalTownMapTransition"));

            handle(session, new Protocol.Writer()
                    .c(Protocol.C_GOTO_PORTAL).bytes());
            for (int opcode : new int[] {
                    Protocol.S_STATUS, Protocol.S_MANA_POINT,
                    Protocol.S_ABILITY_SCORES, Protocol.S_EXP,
                    Protocol.S_AC, Protocol.S_MAGIC_STATUS,
                    Protocol.S_PUT_OBJECT, Protocol.S_LITO_MINIMAP,
                    Protocol.S_HIT_POINT}) {
                assertEquals(opcode, receive(client, receiver)[0] & 0xFF);
            }
            assertEquals(hall.mapId(), field(session, "rentalTownMapId"));
            assertEquals(hall.entryX(), field(session, "positionX"));
            assertEquals(hall.entryY(), field(session, "positionY"));

            int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
            int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
            int stepX = 0;
            int stepY = 0;
            int heading = 0;
            for (int index = 0; index < 8; index++) {
                int candidateX = hall.entryX() + dx[index];
                int candidateY = hall.entryY() + dy[index];
                if (NativeLitoRentalHotelCatalog.get().canStep(
                        hall.mapId(), hall.entryX(), hall.entryY(),
                        candidateX, candidateY)) {
                    stepX = candidateX;
                    stepY = candidateY;
                    heading = index;
                    break;
                }
            }
            assertTrue(stepX != 0);
            handle(session, new Protocol.Writer().c(Protocol.C_MOVE)
                    .h(hall.entryX()).h(hall.entryY()).c(heading).bytes());
            assertEquals(Protocol.S_LITO_MINIMAP,
                    receive(client, receiver)[0] & 0xFF);
            assertEquals(stepX, field(session, "positionX"));
            assertEquals(stepY, field(session, "positionY"));

            Object lease = field(session, "rentalHotelLease");
            long token = ((Number) field(lease, "token")).longValue();
            var constructor = lease.getClass().getDeclaredConstructor(
                    long.class, int.class, NativeLitoRentalHotelCatalog.Room.class,
                    long.class);
            constructor.setAccessible(true);
            setField(session, "rentalHotelLease", constructor.newInstance(
                    token, 5, hall, System.nanoTime() - 1));
            session.serverRentalHotelExpiry(token);
            for (int ignored = 0; ignored < 5; ignored++) {
                assertEquals(Protocol.S_REMOVE_INVENTORY,
                        receive(client, receiver)[0] & 0xFF);
            }
            assertArrayEquals(Protocol.portal(
                            NativeLitoRentalHotelCatalog.RETURN_MAP_ID,
                            NativeLitoRentalHotelCatalog.RETURN_X,
                            NativeLitoRentalHotelCatalog.RETURN_Y),
                    receive(client, receiver));
            handle(session, new Protocol.Writer()
                    .c(Protocol.C_GOTO_PORTAL).bytes());
            for (int opcode : new int[] {
                    Protocol.S_STATUS, Protocol.S_MANA_POINT,
                    Protocol.S_ABILITY_SCORES, Protocol.S_EXP,
                    Protocol.S_AC, Protocol.S_MAGIC_STATUS,
                    Protocol.S_PUT_OBJECT}) {
                assertEquals(opcode, receive(client, receiver)[0] & 0xFF);
            }
            for (int ignored = 0;
                 ignored < NativeLitoRentalTownNpcCatalog.forMap(500).size();
                 ignored++) {
                assertEquals(Protocol.S_PUT_OBJECT,
                        receive(client, receiver)[0] & 0xFF);
            }
            assertEquals(Protocol.S_LITO_MINIMAP,
                    receive(client, receiver)[0] & 0xFF);
            assertEquals(Protocol.S_HIT_POINT,
                    receive(client, receiver)[0] & 0xFF);
            assertEquals(NativeLitoRentalHotelCatalog.RETURN_MAP_ID,
                    field(session, "rentalTownMapId"));
            assertNull(field(session, "rentalHotelLease"));

            handle(session, action(sheba.objectId(),
                    NativeLitoRentalTownNpcCatalog.HOTEL_ROOM_ACTION, "1"));
            assertThrows(SocketTimeoutException.class,
                    () -> Protocol.readFrame(client.getInputStream(), 8_192));
            assertFalse(NativeLitoRentalTownNpcCatalog
                    .HISTORICAL_HOTEL_ROOM_MAP_ASSIGNMENT_AVAILABLE);
            assertTrue(NativeLitoRentalHotelCatalog
                    .HISTORICAL_KEY_ITEM_RESOURCE_AVAILABLE);
            assertTrue(NativeLitoRentalHotelCatalog
                    .HISTORICAL_KEY_TRANSFER_AVAILABLE);
        }
    }

    @Test void teleportersExposeDefaultPageWithoutCollapsingEndpointLetters()
            throws Exception {
        assertEquals(Set.of(
                        NativeLitoRentalTownNpcCatalog.TELEPORT_ACTION_B,
                        NativeLitoRentalTownNpcCatalog.TELEPORT_ACTION_C),
                NativeLitoRentalTownNpcCatalog.TELEPORT_ACTIONS_BY_HTML.get(
                        NativeLitoRentalTownNpcCatalog.TELEPORT_DEFAULT_HTML));
        assertEquals(Set.of(
                        NativeLitoRentalTownNpcCatalog.TELEPORT_ACTION_B,
                        NativeLitoRentalTownNpcCatalog.TELEPORT_ACTION_C,
                        NativeLitoRentalTownNpcCatalog.TELEPORT_ACTION_D),
                NativeLitoRentalTownNpcCatalog.TELEPORT_ACTIONS_BY_HTML.get(
                        NativeLitoRentalTownNpcCatalog.TELEPORT_TRIAL_HTML));
        assertEquals(Set.of(NativeLitoRentalTownNpcCatalog.TELEPORT_ACTION_A),
                NativeLitoRentalTownNpcCatalog.TELEPORT_ACTIONS_BY_HTML.get(
                        NativeLitoRentalTownNpcCatalog.TELEPORT_HONOR_HTML));

        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("teleporter-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(150);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("teleporter-capture")), hub);
            setPhase(session, "BATTLE");
            setField(session, "selectedCharacter", "EndpointVisitor");
            setField(session, "rentalTownRoomState", true);
            setField(session, "rentalTownMapId", 500);
            setField(session, "currentRoomId", 0x5000_0001L);
            int seed = 0x3810_0643;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            for (int resourceIndex : List.of(92, 93, 94, 95)) {
                NativeLitoRentalTownNpcCatalog.Npc teleporter = npc(resourceIndex);
                setField(session, "positionX", teleporter.x());
                setField(session, "positionY", teleporter.y());
                handle(session, new Protocol.Writer().c(Protocol.C_DIALOG)
                        .d(teleporter.objectId()).bytes());
                assertArrayEquals(Protocol.hypertext(
                                teleporter.objectId(), "TeleportMe", "", List.of()),
                        receive(client, receiver));
                for (String action : List.of(
                        NativeLitoRentalTownNpcCatalog.TELEPORT_ACTION_B,
                        NativeLitoRentalTownNpcCatalog.TELEPORT_ACTION_C)) {
                    assertTrue(NativeLitoRentalTownNpcCatalog
                            .isTeleportActionForNpc(teleporter, action));
                    handle(session, action(teleporter.objectId(), action, ""));
                    assertThrows(SocketTimeoutException.class,
                            () -> receive(client, receiver));
                }
                assertFalse(NativeLitoRentalTownNpcCatalog
                        .isTeleportActionForNpc(teleporter,
                                NativeLitoRentalTownNpcCatalog.TELEPORT_ACTION_A));
                assertFalse(NativeLitoRentalTownNpcCatalog
                        .isTeleportActionForNpc(teleporter,
                                NativeLitoRentalTownNpcCatalog.TELEPORT_ACTION_D));
            }
            assertFalse(NativeLitoRentalTownNpcCatalog
                    .HISTORICAL_TELEPORT_HTML_ASSIGNMENT_EXACT);
            assertFalse(NativeLitoRentalTownNpcCatalog
                    .HISTORICAL_TELEPORT_ENDPOINT_CATALOG_AVAILABLE);
        }
    }

    @Test void twoSessionsTransferNativeHotelKeyAndGuestEntersSameLease()
            throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("hotel-key-share-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket ownerListener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             ServerSocket guestListener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket ownerClient = new Socket(
                    "127.0.0.1", ownerListener.getLocalPort());
             Socket ownerServer = ownerListener.accept();
             Socket guestClient = new Socket(
                    "127.0.0.1", guestListener.getLocalPort());
             Socket guestServer = guestListener.accept()) {
            ownerClient.setSoTimeout(250);
            guestClient.setSoTimeout(250);
            ClientSession owner = new ClientSession(
                    ownerServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("hotel-key-owner-capture")), hub);
            ClientSession guest = new ClientSession(
                    guestServer, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("hotel-key-guest-capture")), hub);
            long roomId = 0x5000_0011L;
            NativeLitoRentalTownNpcCatalog.Npc sheba = npc(
                    NativeLitoRentalTownNpcCatalog.SHEBA_RESOURCE_INDEX);
            configureRentalHotelSession(owner, "HotelOwner", 0x5000_0201L,
                    roomId, sheba.x(), sheba.y());
            configureRentalHotelSession(guest, "HotelGuest", 0x5000_0202L,
                    roomId, sheba.x(), sheba.y());
            int ownerSeed = 0x3810_0644;
            int guestSeed = 0x3810_0645;
            setField(owner, "outgoing", new Protocol.CipherState(ownerSeed));
            setField(guest, "outgoing", new Protocol.CipherState(guestSeed));
            Protocol.CipherState ownerReceiver = new Protocol.CipherState(ownerSeed);
            Protocol.CipherState guestReceiver = new Protocol.CipherState(guestSeed);
            hub.connect(owner);
            hub.connect(guest);

            RuntimeInventory ownerInventory = new RuntimeInventory();
            assertTrue(ownerInventory.add(InventoryCatalog.require(
                    NativeLitoRentalHotelCatalog.KEY_ITEM_ID), 2));
            List<Long> keyIds = ownerInventory.items().stream()
                    .map(RuntimeInventory.Item::objectId).toList();
            long token = 0x646L;
            long expiry = System.nanoTime() + 10_000_000_000L;
            NativeLitoRentalHotelCatalog.Room room =
                    NativeLitoRentalHotelCatalog.forKeyCount(2);
            assertNotNull(hub.registerRentalHotelLease(
                    token, 0x5000_0201L, 2, room, expiry, keyIds));
            setField(owner, "rentalTownInventory", ownerInventory);
            setField(owner, "rentalHotelLease", rentalLease(
                    token, 2, room, expiry));

            long sharedKey = keyIds.get(1);
            handle(owner, new Protocol.Writer().c(Protocol.C_DROP)
                    .h(sheba.x()).h(sheba.y()).d(sharedKey).d(1).bytes());
            assertEquals(Protocol.S_REMOVE_INVENTORY,
                    receive(ownerClient, ownerReceiver)[0] & 0xFF);
            byte[] ownerGround = receive(ownerClient, ownerReceiver);
            byte[] guestGround = receive(guestClient, guestReceiver);
            assertEquals(Protocol.S_PUT_OBJECT, ownerGround[0] & 0xFF);
            assertArrayEquals(ownerGround, guestGround);

            handle(guest, new Protocol.Writer().c(Protocol.C_GET)
                    .h(Protocol.groundPickupWireX(sheba.x())).h(sheba.y())
                    .d(sharedKey).d(1).c(0).bytes());
            assertEquals(Protocol.S_REMOVE_OBJECT,
                    receive(ownerClient, ownerReceiver)[0] & 0xFF);
            assertEquals(Protocol.S_REMOVE_OBJECT,
                    receive(guestClient, guestReceiver)[0] & 0xFF);
            assertEquals(Protocol.S_ADD_INVENTORY,
                    receive(guestClient, guestReceiver)[0] & 0xFF);
            RuntimeInventory guestInventory =
                    (RuntimeInventory) field(guest, "rentalTownInventory");
            assertNotNull(guestInventory.item(sharedKey));
            Object guestLease = field(guest, "rentalHotelLease");
            assertEquals(token, ((Number) field(guestLease, "token")).longValue());

            handle(guest, action(sheba.objectId(),
                    NativeLitoRentalTownNpcCatalog.HOTEL_ENTER_ACTION, ""));
            assertEquals(Protocol.S_REMOVE_OBJECT,
                    receive(ownerClient, ownerReceiver)[0] & 0xFF);
            assertEquals(Protocol.S_REMOVE_OBJECT,
                    receive(guestClient, guestReceiver)[0] & 0xFF);
            assertArrayEquals(Protocol.portal(
                            room.mapId(), room.entryX(), room.entryY()),
                    receive(guestClient, guestReceiver));
            handle(owner, action(sheba.objectId(),
                    NativeLitoRentalTownNpcCatalog.HOTEL_RETURN_ACTION, ""));
            assertArrayEquals(Protocol.hypertext(
                            sheba.objectId(), NativeLitoRentalHotelCatalog.RETURNED_HTML,
                            "", List.of(sheba.name())),
                    receive(ownerClient, ownerReceiver));
            assertEquals(Protocol.S_REMOVE_INVENTORY,
                    receive(ownerClient, ownerReceiver)[0] & 0xFF);
            assertEquals(Protocol.S_REMOVE_INVENTORY,
                    receive(guestClient, guestReceiver)[0] & 0xFF);
            assertNull(field(owner, "rentalHotelLease"));
            assertNull(field(guest, "rentalHotelLease"));
            assertNull(field(guest, "pendingRentalTownMapTransition"));
            hub.disconnect(owner);
            hub.disconnect(guest);
        }
    }

    @Test void rentalTownGameplayKeepsNativeMonitorPingAlive() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
        GameStateStore state = new GameStateStore(
                temporary.resolve("rental-town-ping-state.properties"), false);
        GameHub hub = new GameHub(config);
        try (ServerSocket listener = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"));
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            client.setSoTimeout(250);
            ClientSession session = new ClientSession(
                    server, ClientSession.Endpoint.GAME, config, null, state,
                    new CaptureLog(temporary.resolve("rental-town-ping-capture")), hub);
            configureRentalHotelSession(session, "TownPing", 0x5000_0210L,
                    0x5000_0012L, 32770, 32818);
            int seed = 0x3810_0647;
            setField(session, "outgoing", new Protocol.CipherState(seed));
            Protocol.CipherState receiver = new Protocol.CipherState(seed);

            handle(session, new Protocol.Writer()
                    .c(Protocol.C_MONITOR_CONTROL).c(0).bytes());
            assertArrayEquals(Protocol.ping(0), receive(client, receiver));
        }
    }

    private static NativeLitoRentalTownNpcCatalog.Npc npc(int resourceIndex) {
        return NativeLitoRentalTownNpcCatalog.forMap(500).stream()
                .filter(candidate -> candidate.resourceIndex() == resourceIndex)
                .findFirst().orElseThrow();
    }

    private static void configureRentalHotelSession(
            ClientSession session, String name, long objectId, long roomId,
            int x, int y) throws Exception {
        setPhase(session, "BATTLE");
        setField(session, "selectedCharacter", name);
        setField(session, "characters", List.of(new CharacterSummary(
                name, "", 1, 0, 50, 500, 100,
                10, 13, 10, 10, 9, 8, 11, 8, 11,
                objectId, 0, 0, 0, 0, 0, 0, 0)));
        setField(session, "rentalTownRoomState", true);
        setField(session, "rentalTownMapId", 500);
        setField(session, "currentRoomId", roomId);
        setField(session, "positionX", x);
        setField(session, "positionY", y);
        setField(session, "playerHp", 100);
    }

    private static Object rentalLease(
            long token, int keyCount, NativeLitoRentalHotelCatalog.Room room,
            long expiresAtNanos) throws Exception {
        Class<?> type = Class.forName(
                ClientSession.class.getName() + "$RentalHotelLease");
        var constructor = type.getDeclaredConstructor(
                long.class, int.class, NativeLitoRentalHotelCatalog.Room.class,
                long.class);
        constructor.setAccessible(true);
        return constructor.newInstance(token, keyCount, room, expiresAtNanos);
    }

    private static byte[] action(long objectId, String action, String parameter) {
        return new Protocol.Writer().c(Protocol.C_HACTION).d(objectId)
                .s(action).s(parameter).bytes();
    }

    private static byte[] receive(Socket client, Protocol.CipherState receiver)
            throws Exception {
        return receiver.decrypt(Protocol.readFrame(client.getInputStream(), 8_192));
    }

    private static void handle(ClientSession session, byte[] packet) throws Exception {
        Method method = ClientSession.class.getDeclaredMethod("handle", byte[].class);
        method.setAccessible(true);
        method.invoke(session, (Object) packet);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    @SuppressWarnings("unchecked")
    private static Set<Integer> numberSetField(Object target, String name) throws Exception {
        return (Set<Integer>) field(target, name);
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

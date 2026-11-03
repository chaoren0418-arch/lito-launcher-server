package local.lito381.server;

import java.util.List;

/** Selected-source fixed merchants reachable in the independent classic world. */
final class ClassicWorldMerchantCatalog {
    static final String DURAN_SOURCE_SHA256 =
            "07EEA19054C140EDF4B55F503A7CA00634033FF7D0BFC765928C1E7673F0CD13";
    static final int DURAN_MAP_ID = 445;
    static final int DURAN_SPAWN_ID = 601;
    static final int DURAN_NPC_ID = 70094;
    static final String DURAN_INITIAL_HTML = "duran";
    static final String DURAN_LOCAL_SHOP_HTML = "duran2";
    static final String DURAN_BUY_ACTION = "buy";
    static final String ROXXANNE_RECONSTRUCTION_SOURCE_SHA256 =
            "7721A509A25E798FDE86672A0DA296BC5C4D1A8E3733E649919C8280A6EA3789";
    static final String ROXXANNE_SHOP_SOURCE_SHA256 =
            "247569564E6A9898A843062C5E29EC7E72B123D9ECE30642476B0AA3696C3A2C";
    static final int ROXXANNE_MAP_ID = 440;
    static final int ROXXANNE_RECONSTRUCTED_SPAWN_ID = 71054;
    static final int ROXXANNE_NPC_ID = 71054;
    static final String ROXXANNE_INITIAL_HTML = "roxxan";
    static final String ROXXANNE_LOCAL_SHOP_HTML = "roxxan1";
    static final String ROXXANNE_BUY_ACTION = "buy";
    static final int TALKING_ISLAND_PORT_MASTER_SPAWN_ID = 1324;
    static final int TALKING_ISLAND_PORT_MASTER_NPC_ID = 70011;
    static final String TALKING_ISLAND_PORT_MASTER_HTML = "shipEvI3";
    static final String TALKING_ISLAND_PORT_MASTER_CHAOTIC_HTML = "shipEvI1";
    static final String TALKING_ISLAND_PORT_MASTER_NIGHT_HTML = "shipEvI6";
    static final int GLUDIO_PORT_MASTER_SPAWN_ID = 1325;
    static final int GLUDIO_PORT_MASTER_NPC_ID = 70022;
    static final String GLUDIO_PORT_MASTER_HTML = "shipEvM1";
    static final String FORGOTTEN_ISLAND_TICKET_MERCHANT_SOURCE_SHA256 =
            "72F87522054BB2E181E2CD309F27C9BF3764B534B207A9F832452B41DABD4BFE";
    static final int ISVALL_SPAWN_ID = 594;
    static final int ISVALL_NPC_ID = 70086;
    static final String ISVALL_HTML = "isvall4";
    static final String ISVALL_LOCAL_SHOP_HTML = "isvall1";
    static final int OBLIVION_SPAWN_ID = 597;
    static final int OBLIVION_NPC_ID = 70089;
    static final String OBLIVION_HTML = "oblivion1";
    static final String OBLIVION_LOCAL_SHOP_HTML = "oblivion2";
    static final String BLOOD_SOUL_SOURCE_SHA256 =
            ClassicWorldShopItemCatalog.BLOOD_SOUL_SHOP_SOURCE_SHA256;
    static final int BLOOD_SOUL_MAP_ID = 12;
    static final int BLOOD_SOUL_SPAWN_ID = 1012;
    static final int BLOOD_SOUL_NPC_ID = 81118;
    static final String BLOOD_SOUL_HTML = "bsoul1";
    static final String BLOOD_SOUL_BUY_ACTION = "buy";
    static final int PAGORU_SPAWN_ID = 1306;
    static final int RINDA_SPAWN_ID = 1323;
    static final String IVORY_TOWER_BUY_ACTION = "buy";
    static final String IVORY_TOWER_SELL_ACTION = "sell";

    record Offer(long orderId, InventoryCatalog.Definition definition, long unitPrice,
                 long packCount) {
        Offer(long orderId, InventoryCatalog.Definition definition, long unitPrice) {
            this(orderId, definition, unitPrice, 1);
        }

        Offer {
            if (orderId < 0 || orderId > 0xFFFF_FFFFL || definition == null
                    || unitPrice <= 0 || unitPrice > 0xFFFF_FFFFL
                    || packCount <= 0 || packCount > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("invalid classic-world merchant offer");
            }
        }

        Protocol.ClassicBuyListRow wireRow() {
            return new Protocol.ClassicBuyListRow(
                    orderId, definition.icon(), unitPrice, definition.name(),
                    definition.detail().getBytes(Protocol.LEGACY_TEXT));
        }
    }

    record Merchant(int mapId, int spawnId, int npcId, long objectId,
                    int x, int y, int maximumDistance, String initialHtml,
                    String chaoticHtml, String localShopHtml,
                    String buyAction, List<Offer> offers) {
        Merchant {
            offers = List.copyOf(offers);
            if (mapId < 0 || spawnId <= 0 || npcId <= 0
                    || objectId != ClassicWorldNpcCatalog.objectId(spawnId)
                    || x <= 0 || y <= 0 || maximumDistance <= 0
                    || initialHtml == null || initialHtml.isBlank()
                    || chaoticHtml == null || chaoticHtml.isBlank()
                    || localShopHtml == null || localShopHtml.isBlank()
                    || buyAction == null || buyAction.isBlank() || offers.isEmpty()) {
                throw new IllegalArgumentException("invalid classic-world merchant");
            }
        }

        boolean matches(ClassicWorldNpcCatalog.StaticNpc npc) {
            return npc != null && npc.mapId() == mapId && npc.spawnId() == spawnId
                    && npc.npcId() == npcId && npc.objectId() == objectId
                    && npc.x() == x && npc.y() == y
                    && npc.implementation().equals("L1Merchant")
                    && npc.normalAction().equals(initialHtml)
                    && npc.chaoticAction().equals(chaoticHtml);
        }
    }

    private static final Merchant DURAN = new Merchant(
            DURAN_MAP_ID, DURAN_SPAWN_ID, DURAN_NPC_ID,
            ClassicWorldNpcCatalog.objectId(DURAN_SPAWN_ID),
            32753, 32860, 3, DURAN_INITIAL_HTML, DURAN_INITIAL_HTML,
            DURAN_LOCAL_SHOP_HTML,
            DURAN_BUY_ACTION,
            List.of(new Offer(0,
                    InventoryCatalog.require(InventoryCatalog.SHIP_TICKET_PIRATE_ISLAND),
                    690)));

    private static final Merchant ROXXANNE = new Merchant(
            ROXXANNE_MAP_ID, ROXXANNE_RECONSTRUCTED_SPAWN_ID,
            ROXXANNE_NPC_ID,
            ClassicWorldNpcCatalog.objectId(ROXXANNE_RECONSTRUCTED_SPAWN_ID),
            32292, 33067, 3,
            ROXXANNE_INITIAL_HTML, ROXXANNE_INITIAL_HTML,
            ROXXANNE_LOCAL_SHOP_HTML, ROXXANNE_BUY_ACTION,
            List.of(new Offer(0,
                    InventoryCatalog.require(InventoryCatalog.SHIP_TICKET_HIDDEN_DOCK),
                    690)));

    private static final Merchant TALKING_ISLAND_PORT_MASTER = new Merchant(
            ClassicWorldMapCatalog.TALKING_ISLAND_MAP_ID,
            TALKING_ISLAND_PORT_MASTER_SPAWN_ID,
            TALKING_ISLAND_PORT_MASTER_NPC_ID,
            ClassicWorldNpcCatalog.objectId(TALKING_ISLAND_PORT_MASTER_SPAWN_ID),
            32633, 32968, 3,
            TALKING_ISLAND_PORT_MASTER_HTML,
            TALKING_ISLAND_PORT_MASTER_CHAOTIC_HTML,
            TALKING_ISLAND_PORT_MASTER_HTML,
            "buy", List.of(new Offer(0,
                    InventoryCatalog.require(InventoryCatalog.SHIP_TICKET_TO_GLUDIO),
                    300)));

    private static final Merchant GLUDIO_PORT_MASTER = new Merchant(
            ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
            GLUDIO_PORT_MASTER_SPAWN_ID,
            GLUDIO_PORT_MASTER_NPC_ID,
            ClassicWorldNpcCatalog.objectId(GLUDIO_PORT_MASTER_SPAWN_ID),
            32560, 32723, 3,
            GLUDIO_PORT_MASTER_HTML, GLUDIO_PORT_MASTER_HTML,
            GLUDIO_PORT_MASTER_HTML,
            "buy", List.of(new Offer(0,
                    InventoryCatalog.require(
                            InventoryCatalog.SHIP_TICKET_TO_TALKING_ISLAND),
                    300)));

    private static final Merchant ISVALL = new Merchant(
            ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
            ISVALL_SPAWN_ID, ISVALL_NPC_ID,
            ClassicWorldNpcCatalog.objectId(ISVALL_SPAWN_ID),
            33446, 33476, 3,
            ISVALL_HTML, ISVALL_HTML, ISVALL_LOCAL_SHOP_HTML,
            "buy", List.of(new Offer(0,
                    InventoryCatalog.require(
                            InventoryCatalog.SHIP_TICKET_FORGOTTEN_ISLAND),
                    15_000)));

    private static final Merchant OBLIVION = new Merchant(
            ClassicWorldMapCatalog.FORGOTTEN_ISLAND_MAP_ID,
            OBLIVION_SPAWN_ID, OBLIVION_NPC_ID,
            ClassicWorldNpcCatalog.objectId(OBLIVION_SPAWN_ID),
            32932, 33046, 3,
            OBLIVION_HTML, OBLIVION_HTML, OBLIVION_LOCAL_SHOP_HTML,
            "buy", List.of(new Offer(0,
                    InventoryCatalog.require(InventoryCatalog.SHIP_TICKET_HEINE),
                    15_000)));

    private static final Merchant BLOOD_SOUL = new Merchant(
            BLOOD_SOUL_MAP_ID, BLOOD_SOUL_SPAWN_ID, BLOOD_SOUL_NPC_ID,
            ClassicWorldNpcCatalog.objectId(BLOOD_SOUL_SPAWN_ID),
            32734, 32796, 3,
            BLOOD_SOUL_HTML, BLOOD_SOUL_HTML, BLOOD_SOUL_HTML,
            BLOOD_SOUL_BUY_ACTION, List.of(new Offer(0,
                    InventoryCatalog.require(
                            ClassicWorldShopItemCatalog.BLOOD_SOUL_ITEM_ID),
                    100_000)));
    private static final Merchant PAGORU = ivoryTowerMerchant(
            PAGORU_SPAWN_ID, ClassicWorldIvoryTowerCatalog.PAGORU_NPC_ID,
            "pagoru");
    private static final Merchant RINDA = ivoryTowerMerchant(
            RINDA_SPAWN_ID, ClassicWorldIvoryTowerCatalog.RINDA_NPC_ID,
            "rinda");
    private static final Merchant BUAKHEU = heineMerchant();
    private static final Merchant SCWATY = silentCavernMerchant();
    private static final Merchant KARUPU = rastabadOumMerchant();
    private static final Merchant BOBO = demonTempleMerchant();
    private static final Merchant SHIP_GRAVE = shipGraveMerchant();

    private static final List<Merchant> MERCHANTS = List.of(
            DURAN, ROXXANNE, TALKING_ISLAND_PORT_MASTER, GLUDIO_PORT_MASTER,
            ISVALL, OBLIVION, BLOOD_SOUL, PAGORU, RINDA, BUAKHEU, SCWATY,
            KARUPU, BOBO, SHIP_GRAVE);

    private ClassicWorldMerchantCatalog() {}

    static Merchant find(ClassicWorldNpcCatalog.StaticNpc npc) {
        return MERCHANTS.stream().filter(merchant -> merchant.matches(npc))
                .findFirst().orElse(null);
    }

    static boolean supportsEmptySell(Merchant merchant) {
        return merchant != null && (merchant == SCWATY || merchant == KARUPU
                || merchant == BOBO || merchant == SHIP_GRAVE
                || ClassicWorldIvoryTowerCatalog.hasShop(merchant.npcId()));
    }

    static String dialogueHtml(
            ClassicWorldNpcCatalog.StaticNpc npc,
            long playerLawful, int secondsOfDay) {
        ClassicWorldShipSchedule.requireSecondsOfDay(secondsOfDay);
        if (npc != null && npc.npcId() == TALKING_ISLAND_PORT_MASTER_NPC_ID
                && (secondsOfDay < 6 * 3_600
                    || secondsOfDay > 20 * 3_600)) {
            return TALKING_ISLAND_PORT_MASTER_NIGHT_HTML;
        }
        return npc == null ? "" : npc.initialHtml(playerLawful);
    }

    private static Merchant ivoryTowerMerchant(
            int spawnId, int npcId, String html) {
        ClassicWorldNpcCatalog.StaticNpc npc = ClassicWorldNpcCatalog.npc(
                75, ClassicWorldNpcCatalog.objectId(spawnId));
        if (npc == null || npc.npcId() != npcId || !npc.fixedMerchant()
                || !npc.normalAction().equals(html)
                || !npc.chaoticAction().equals(html)) {
            throw new ExceptionInInitializerError(
                    "Ivory Tower merchant spawn contract changed " + spawnId);
        }
        List<Offer> offers = ClassicWorldIvoryTowerCatalog.shopOffers(npcId).stream()
                .map(offer -> new Offer(
                        offer.wireOrderId(), offer.definition(), offer.unitPrice()))
                .toList();
        return new Merchant(75, spawnId, npcId, npc.objectId(), npc.x(), npc.y(),
                5, html, html, html, IVORY_TOWER_BUY_ACTION, offers);
    }

    private static Merchant heineMerchant() {
        ClassicWorldNpcCatalog.StaticNpc npc = ClassicWorldNpcCatalog.npc(
                63, ClassicWorldNpcCatalog.objectId(
                        ClassicWorldHeineCatalog.BUAKHEU_SPAWN_ID));
        if (!ClassicWorldHeineCatalog.isBuakheu(npc)) {
            throw new ExceptionInInitializerError("Buakheu spawn contract changed");
        }
        List<Offer> offers = ClassicWorldHeineCatalog.buakheuOffers().stream()
                .map(offer -> new Offer(offer.wireOrderId(), offer.definition(),
                        offer.unitPrice(), offer.packCount()))
                .toList();
        return new Merchant(63, npc.spawnId(), npc.npcId(), npc.objectId(),
                npc.x(), npc.y(), 5,
                ClassicWorldHeineCatalog.BUAKHEU_HTML,
                ClassicWorldHeineCatalog.BUAKHEU_HTML,
                ClassicWorldHeineCatalog.BUAKHEU_HTML,
                ClassicWorldHeineCatalog.BUAKHEU_BUY_ACTION, offers);
    }

    private static Merchant silentCavernMerchant() {
        ClassicWorldNpcCatalog.StaticNpc npc = ClassicWorldNpcCatalog.npc(
                ClassicWorldSilentCavernCatalog.SILENT_CAVERN_MAP_ID,
                ClassicWorldNpcCatalog.objectId(
                        ClassicWorldSilentCavernCatalog.SCWATY_SPAWN_ID));
        if (!ClassicWorldSilentCavernCatalog.isScwaty(npc)) {
            throw new ExceptionInInitializerError("Scwaty spawn contract changed");
        }
        List<Offer> offers = ClassicWorldSilentCavernCatalog.scwatyOffers().stream()
                .map(offer -> new Offer(offer.wireOrderId(), offer.definition(),
                        offer.unitPrice(), offer.packCount()))
                .toList();
        return new Merchant(npc.mapId(), npc.spawnId(), npc.npcId(), npc.objectId(),
                npc.x(), npc.y(), 5,
                ClassicWorldSilentCavernCatalog.SCWATY_HTML,
                ClassicWorldSilentCavernCatalog.SCWATY_HTML,
                ClassicWorldSilentCavernCatalog.SCWATY_HTML,
                "buy", offers);
    }

    private static Merchant rastabadOumMerchant() {
        ClassicWorldNpcCatalog.StaticNpc npc = ClassicWorldNpcCatalog.npc(
                ClassicWorldRastabadOumCatalog.OUM_DUNGEON_MAP_ID,
                ClassicWorldNpcCatalog.objectId(
                        ClassicWorldRastabadOumCatalog.KARUPU_SPAWN_ID));
        if (!ClassicWorldRastabadOumCatalog.isKarupu(npc)) {
            throw new ExceptionInInitializerError("Karupu spawn contract changed");
        }
        List<Offer> offers = ClassicWorldRastabadOumCatalog.karupuOffers().stream()
                .map(offer -> new Offer(offer.wireOrderId(), offer.definition(),
                        offer.unitPrice(), offer.packCount()))
                .toList();
        return new Merchant(npc.mapId(), npc.spawnId(), npc.npcId(), npc.objectId(),
                npc.x(), npc.y(), 5,
                ClassicWorldRastabadOumCatalog.KARUPU_HTML,
                ClassicWorldRastabadOumCatalog.KARUPU_HTML,
                ClassicWorldRastabadOumCatalog.KARUPU_HTML,
                "buy", offers);
    }

    private static Merchant demonTempleMerchant() {
        ClassicWorldNpcCatalog.StaticNpc npc = ClassicWorldNpcCatalog.npc(
                ClassicWorldDemonTempleCatalog.MAP_ID,
                ClassicWorldNpcCatalog.objectId(
                        ClassicWorldDemonTempleCatalog.BOBO_SPAWN_ID));
        if (!ClassicWorldDemonTempleCatalog.isBobo(npc)) {
            throw new ExceptionInInitializerError("Bobo spawn contract changed");
        }
        List<Offer> offers = ClassicWorldDemonTempleCatalog.boboOffers().stream()
                .map(offer -> new Offer(offer.wireOrderId(), offer.definition(),
                        offer.unitPrice(), offer.packCount()))
                .toList();
        return new Merchant(npc.mapId(), npc.spawnId(), npc.npcId(), npc.objectId(),
                npc.x(), npc.y(), 5,
                ClassicWorldDemonTempleCatalog.BOBO_HTML,
                ClassicWorldDemonTempleCatalog.BOBO_HTML,
                ClassicWorldDemonTempleCatalog.BOBO_HTML,
                "buy", offers);
    }

    private static Merchant shipGraveMerchant() {
        ClassicWorldNpcCatalog.StaticNpc npc = ClassicWorldNpcCatalog.npc(
                ClassicWorldPirateShipGraveCatalog.MAP_ID,
                ClassicWorldNpcCatalog.objectId(
                        ClassicWorldPirateShipGraveCatalog.MERCHANT_SPAWN_ID));
        if (!ClassicWorldPirateShipGraveCatalog.isMerchant(npc)) {
            throw new ExceptionInInitializerError("Ship Grave merchant contract changed");
        }
        List<Offer> offers = ClassicWorldPirateShipGraveCatalog.offers().stream()
                .map(offer -> new Offer(offer.wireOrderId(), offer.definition(),
                        offer.sellingPrice(), offer.packCount()))
                .toList();
        return new Merchant(npc.mapId(), npc.spawnId(), npc.npcId(), npc.objectId(),
                npc.x(), npc.y(), 5,
                ClassicWorldPirateShipGraveCatalog.MERCHANT_HTML,
                ClassicWorldPirateShipGraveCatalog.MERCHANT_HTML,
                ClassicWorldPirateShipGraveCatalog.MERCHANT_HTML,
                "buy", offers);
    }
}

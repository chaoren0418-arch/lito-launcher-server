package local.lito381.server;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/** Hash-locked staged native Rastabad cave / Oum Dungeon maps307-310. */
final class ClassicWorldRastabadOumCatalog {
    static final List<Integer> MAP_IDS = List.of(307, 308, 309, 310);
    static final int OUM_DUNGEON_MAP_ID = 310;
    static final String ROUTES_SOURCE_SHA256 =
            "CD4446B48BC2692E941F150A37F1BE22C1646587880D7FB3D5F11A5347B473E4";
    static final String FLAGS_SOURCE_SHA256 =
            "99BC5E725D0DAC9B4290EE9BF51D4A5E490D2D811B574AD3A53EF3CA28D8B066";
    static final String GETBACK_SOURCE_SHA256 =
            "BB0BD4B4FA903E95622EDD2128DA19D1CF48E32EB1F14B1F51522E1F91D95EAA";
    static final String NPC_ACTION_SOURCE_SHA256 =
            "8DB934130A057F8C111586D8960930929A58347F5EC1DC30ED4C24520AF4B446";
    static final String NAMED_TELEPORT_SOURCE_SHA256 =
            "29DBF42E27453C3006061B695EA8B6D3E5D81868711624D41B351841E1C351B0";
    static final String TEXT_SOURCE_SHA256 =
            "924C93CD2DDC57E1F225A61FF9D57DF368292B6FD179B747F801953F952508AB";
    static final String SHOP_SOURCE_SHA256 =
            "08C4EF8015C258D1C65455963C613BA48837A837D51E491B807541E6E7A9124C";
    static final String SHOP_RESOURCE_SHA256 =
            "3212BA94E114A853E570D5A6D7F975880404F285F0AAA295745D6D2E4DC1F63F";

    static final int TELEPORTER_SPAWN_ID = 113;
    static final int TELEPORTER_NPC_ID = 50072;
    static final String TELEPORT_INITIAL_HTML = "teleoum1";
    static final String TELEPORT_CONFIRM_HTML = "teleoum2";
    static final String TELEPORT_CONFIRM_ACTION = "teleportURL";
    static final int WAREHOUSE_SPAWN_ID = 291;
    static final int WAREHOUSE_NPC_ID = 60033;
    static final String WAREHOUSE_NORMAL_HTML = "gawl";
    static final String WAREHOUSE_CHAOTIC_HTML = "gawll";
    static final int KARUPU_SPAWN_ID = 600;
    static final int KARUPU_NPC_ID = 70093;
    static final String KARUPU_HTML = "karupu";
    static final int EXCLUDED_CUSTOM_SPAWN_ID = 1077;

    record MapFlags(int mapId, String locationName,
                    int startX, int endX, int startY, int endY,
                    boolean escapable, boolean usableItem, boolean usableSkill,
                    boolean teleportable, boolean takePets, boolean recallPets) {}

    record SourceContract(int rows, int population, String sourceSha256,
                          String resourceSha256) {}

    record ShopOffer(int wireOrderId, int sourceOrderId, int itemId,
                     int unitPrice, int packCount,
                     InventoryCatalog.Definition definition) {}

    private record ShopRow(int npcId, int itemId, int sourceOrderId,
                           int sellingPrice, int packCount,
                           int purchasingPrice, String note, int dailyBuyLimit) {}
    private record ShopCatalog(List<ShopRow> rows,
                               Map<Integer, ClassicWorldDropCatalog.ItemWire> items) {}

    static final Map<Integer, String> MAP_SOURCE_SHA256 = Map.of(
            307, "191708369E55D4CF4245111F4B07BC1A9CBD3F629422DD4A91E811B01FF5CF3C",
            308, "9FAF70680E5EFACA4CD67DB13B63B57C489CBB2EA290C8E1D05FEC0018E23331",
            309, "F6F7DBCB96F6491D35CFCCDE2A17FAE9A1489C4BBA3CBB6B31FF0261306C0EA0",
            310, "C540E9D90076AC3259B7AAA134DBA5E7DD09D22A43DADC68BCB4E782CF612AAA");
    static final Map<Integer, String> MAP_RESOURCE_SHA256 = Map.of(
            307, "EAB35660A8E42AFB30BADBB45416BD6BC505EBE8627042CF7953F28D3C939CE4",
            308, "81C4534753D309EAAADF10160FC1CBDF5FDD02653674926CF40411BC57BC6EF5",
            309, "9E2BB22BCE8682F77B802C1770367427808EA0A6FB49CF44D8533A97F6C0CCDE",
            310, "3A515CE667A0CA06B801B0AF9F03DB82912D42A953AA3D7D08AA6E353AAD1419");
    static final Map<Integer, SourceContract> NPC_CONTRACTS = Map.of(
            307, contract(0, 0,
                    "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF",
                    "9681105F0A7FF535913C865017B4D3A1C21396272EF966989DC75212AE49690B"),
            308, contract(0, 0,
                    "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF",
                    "C4D9034A2E57AAB958FFAA471B3DC05EF4E489C771F59F5304475EC544A62E7D"),
            309, contract(0, 0,
                    "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF",
                    "C5409B1FDFA6D6E11863035EDA509DCD00F2CFFFC4DFD25D0EE8171B8E784621"),
            310, contract(3, 0,
                    "5B8EB82C0F6B9D9527640E97AA4CE0EF6A89F5C28210F64D315735E95827546D",
                    "CAD7264E2DF3D5B69E1652C645438DBB48B65DC89E8EF6581B1C8FEA07F6B7D7"));
    static final Map<Integer, SourceContract> MONSTER_CONTRACTS = Map.of(
            307, contract(4, 110,
                    "A734395E6528F6994EDB4A7B32A0135D3B4F51EC56362325D041D6F4BBADA637",
                    "4EACD293875E31E2B65BE6BA8F9A36B597B21F28CA382E9E946C310C01EDD47D"),
            308, contract(7, 120,
                    "E79B52BEDEB3444164538F9447A6B72A6E4378A737F3E38E6EAAD05DB8E63ACA",
                    "803E4FA11020AB0580B59F09772B196D6A5C46325165F762212D3EAEA9F584E4"),
            309, contract(30, 270,
                    "EA90E227E9328DE6447B379F49B2AFA1DE9D48CEAFC2D63BB65241F30D7D24D0",
                    "1352A415E981D7F3F1349D6C0F39DCA7CA8924AD36A6EB9C6DB38D0E1616D1E1"),
            310, contract(28, 221,
                    "428C2D052D4F038F527814183EBDC54FC4FF113DF1EE63E37FDF217EA4D5D6E4",
                    "2A6AFF106201031DEBF9E6819414C56DE19374A9F6CCFBE0EABB212CAB37A8F0"));
    static final Map<Integer, SourceContract> DROP_CONTRACTS = Map.of(
            307, contract(15, 8,
                    "8C79E16ED8CF5B1309B807C99A68E4DF1DD62DB9DDA13D0773DD75A64E6F245C",
                    "49F82DB3882AAAE67FEFFDC81A04DE1EDF0C4E88526A731D16A1D0D930267073"),
            308, contract(81, 41,
                    "B52CA96065077FACB02A9F4E1C9C112CD7E9E198B46662D449E94AAF40517083",
                    "12B1700BF315374628F75625FCB1C29D5F253CF5CA9C632FDCAC4E4DB496AC17"),
            309, contract(207, 66,
                    "9BE5E7ECF3123C1938975EEC21018820A6F6036BABA3810CC0D8B761DD0585FF",
                    "B6F8AA38DE0F25E300CC4E7659A715CE62A94D9889A32E224025750D792CA092"),
            310, contract(61, 35,
                    "985BA9B3F156F05B8ADA2C5E941674C6537BA5C62C03F13C321E99F22D2B4815",
                    "23AEA950FE9EB35095D4260B959CC49E6371F7BBE816A99D624A358EE862AFFF"));

    private static final Map<Integer, MapFlags> FLAGS = Map.of(
            307, new MapFlags(307, "拉斯塔巴德地下洞穴 1樓",
                    32704, 32959, 32768, 32959, true, true, true, true, true, true),
            308, new MapFlags(308, "拉斯塔巴德地下洞穴 2樓",
                    32704, 33023, 32768, 32959, true, true, true, true, true, true),
            309, new MapFlags(309, "拉斯塔巴德地下洞穴 3樓",
                    32704, 33087, 32768, 32959, true, true, true, true, true, true),
            310, new MapFlags(310, "歐姆地監",
                    32640, 32895, 32768, 33023, true, true, true, false, true, true));
    private static final ShopCatalog KARUPU_SHOP = loadShop();

    private ClassicWorldRastabadOumCatalog() {}

    static MapFlags flags(int mapId) {
        MapFlags result = FLAGS.get(mapId);
        if (result == null) throw new IllegalArgumentException("not a Rastabad/Oum map");
        return result;
    }

    static boolean isTeleporter(ClassicWorldNpcCatalog.StaticNpc npc) {
        return matches(npc, TELEPORTER_SPAWN_ID, TELEPORTER_NPC_ID,
                32805, 32794, "L1Teleporter", TELEPORT_INITIAL_HTML,
                TELEPORT_INITIAL_HTML);
    }

    static boolean isWarehouse(ClassicWorldNpcCatalog.StaticNpc npc) {
        return matches(npc, WAREHOUSE_SPAWN_ID, WAREHOUSE_NPC_ID,
                32806, 32797, "L1Dwarf", WAREHOUSE_NORMAL_HTML,
                WAREHOUSE_CHAOTIC_HTML);
    }

    static boolean isKarupu(ClassicWorldNpcCatalog.StaticNpc npc) {
        return matches(npc, KARUPU_SPAWN_ID, KARUPU_NPC_ID,
                32788, 32806, "L1Merchant", KARUPU_HTML, KARUPU_HTML);
    }

    static List<ShopOffer> karupuOffers() {
        List<ShopOffer> offers = new ArrayList<>();
        int wireOrder = 0;
        for (ShopRow row : KARUPU_SHOP.rows()) {
            ClassicWorldDropCatalog.ItemWire item = KARUPU_SHOP.items().get(row.itemId());
            offers.add(new ShopOffer(wireOrder++, row.sourceOrderId(), row.itemId(),
                    row.sellingPrice(), row.packCount() == 0 ? 1 : row.packCount(),
                    item.inventoryDefinition()));
        }
        return List.copyOf(offers);
    }

    private static SourceContract contract(int rows, int population,
                                           String source, String resource) {
        return new SourceContract(rows, population, source, resource);
    }

    private static boolean matches(ClassicWorldNpcCatalog.StaticNpc npc,
                                   int spawnId, int npcId, int x, int y,
                                   String implementation, String normal, String chaotic) {
        return npc != null && npc.mapId() == OUM_DUNGEON_MAP_ID
                && npc.spawnId() == spawnId && npc.npcId() == npcId
                && npc.objectId() == ClassicWorldNpcCatalog.objectId(spawnId)
                && npc.x() == x && npc.y() == y
                && npc.implementation().equals(implementation)
                && npc.normalAction().equals(normal)
                && npc.chaoticAction().equals(chaotic);
    }

    private static ShopCatalog loadShop() {
        String resourceName = "/world/map310-karupu-shop.lws.gz";
        InputStream resource = ClassicWorldRastabadOumCatalog.class
                .getResourceAsStream(resourceName);
        if (resource == null) throw new ExceptionInInitializerError("missing Karupu shop");
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(resource))) {
            if (in.readInt() != 0x4C575331 || in.readUnsignedShort() != OUM_DUNGEON_MAP_ID) {
                throw new IOException("invalid Karupu shop header");
            }
            int rows = in.readInt();
            int items = in.readInt();
            String sourceHash = HexFormat.of().withUpperCase().formatHex(in.readNBytes(32));
            byte[] table = in.readAllBytes();
            if (rows != 24 || items != 24 || !SHOP_SOURCE_SHA256.equals(sourceHash)
                    || !sourceHash.equals(sha256(table))
                    || !SHOP_RESOURCE_SHA256.equals(sha256Resource(resourceName))) {
                throw new IOException("Karupu shop source contract changed");
            }
            return parseShop(table, rows, items);
        } catch (IOException | IllegalArgumentException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static ShopCatalog parseShop(byte[] table, int expectedRows, int expectedItems)
            throws IOException {
        String shopHeader = String.join("\t", "npc_id", "item_id", "order_id",
                "selling_price", "pack_count", "purchasing_price", "note",
                "daily_buy_limit");
        String itemHeader = String.join("\t", "item_id", "kind", "name_id", "use_token",
                "inventory_gfx", "ground_gfx", "blessing", "stackable", "tradable",
                "unit_weight", "item_description_id", "weapon_type", "damage_small",
                "damage_large", "material", "safe_enchant", "class_mask");
        List<ShopRow> rows = new ArrayList<>();
        Map<Integer, ClassicWorldDropCatalog.ItemWire> items = new LinkedHashMap<>();
        Set<Integer> referenced = new LinkedHashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(table), StandardCharsets.UTF_8))) {
            if (!"[shops]".equals(reader.readLine()) || !shopHeader.equals(reader.readLine())) {
                throw new IOException("Karupu shop TSV header changed");
            }
            for (String line; (line = reader.readLine()) != null;) {
                if (line.equals("[items]")) break;
                String[] f = line.split("\t", -1);
                if (f.length != 8) throw new IOException("Karupu shop field count changed");
                ShopRow row = new ShopRow(integer(f[0]), integer(f[1]), integer(f[2]),
                        integer(f[3]), integer(f[4]), integer(f[5]), f[6], integer(f[7]));
                if (row.npcId() != KARUPU_NPC_ID || row.sellingPrice() <= 0
                        || row.packCount() < 0 || row.purchasingPrice() != -1
                        || row.dailyBuyLimit() != 0) {
                    throw new IOException("invalid Karupu shop row");
                }
                rows.add(row);
                referenced.add(row.itemId());
            }
            if (rows.size() != expectedRows || !itemHeader.equals(reader.readLine())) {
                throw new IOException("Karupu shop/item boundary changed");
            }
            for (String line; (line = reader.readLine()) != null;) {
                String[] f = line.split("\t", -1);
                if (f.length != 17) throw new IOException("Karupu item field count changed");
                int itemId = integer(f[0]);
                items.put(itemId, new ClassicWorldDropCatalog.ItemWire(
                        itemId, f[1], f[2], f[3], integer(f[4]), integer(f[5]),
                        integer(f[6]), integer(f[7]) != 0, integer(f[8]) != 0,
                        integer(f[9]), integer(f[10]), f[11], integer(f[12]),
                        integer(f[13]), f[14], integer(f[15]), integer(f[16])));
            }
        }
        if (items.size() != expectedItems || !items.keySet().equals(referenced)) {
            throw new IOException("Karupu shop rows/items disagree");
        }
        return new ShopCatalog(List.copyOf(rows), Map.copyOf(items));
    }

    private static int integer(String value) { return Integer.parseInt(value); }

    private static String sha256Resource(String name) throws IOException {
        InputStream stream = ClassicWorldRastabadOumCatalog.class.getResourceAsStream(name);
        if (stream == null) throw new IOException("missing resource " + name);
        try (stream) { return sha256(stream.readAllBytes()); }
    }

    private static String sha256(byte[] bytes) throws IOException {
        try {
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IOException("SHA-256 unavailable", impossible);
        }
    }
}

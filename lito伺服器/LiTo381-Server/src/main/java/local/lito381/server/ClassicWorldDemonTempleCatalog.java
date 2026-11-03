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

/** Hash-locked selected-source Demon Temple map410 service boundary. */
final class ClassicWorldDemonTempleCatalog {
    static final int MAP_ID = 410;
    static final String MAP_SOURCE_SHA256 =
            "545FAC11D6D1D1B14ABE5D320596EE9FA08B8B07159AF3F2CE956648194D3D14";
    static final String MAP_RESOURCE_SHA256 =
            "8763515D1307205D6D54CF4F07E1F5CD97EDBA3FAD42C19A3E49F5844BE1C5C1";
    static final String ROUTES_SOURCE_SHA256 =
            "9C0003468B7BF8973A6CE79B933B83C4A90A914E596DA39384D3EB2E5CE52302";
    static final String FLAGS_SOURCE_SHA256 =
            "3542CC81B56778D32F831ED7B8338013294853A5B3C937F0ABB80F1B04AD7498";
    static final String GETBACK_ABSENCE_SHA256 =
            "D085B9DD5C474E494A355530CA2EF0ED75F1B40118B42D5C7BAB27CF0DD94768";
    static final String NPC_TELEPORT_SOURCE_SHA256 =
            "D4DF261F0D87846738782813342454239988BC7C16FF65BDB0A810A73A00ED9C";
    static final String NPC_SOURCE_SHA256 =
            "6F1440497D8BD56C526EDF4B4DD450111D8B24F15A6BAB3F47D1A56E476251D6";
    static final String NPC_RESOURCE_SHA256 =
            "BB9A57107595B3740158C61433F115EEDE11F41ED858CB530FBC740B54925D79";
    static final String MONSTER_SOURCE_SHA256 =
            "244E6DA558AE474C051937DC3F6EB3B36CEC6E5B1ECEE74743705EABC74785EC";
    static final String MONSTER_RESOURCE_SHA256 =
            "C31F2207D0D67F4C507F6BE5D8114201D2B4BC610FF5C19F111B45526C0728B6";
    static final String DROP_SOURCE_SHA256 =
            "7909FB40776D0088E2890FEE299DCF141F35E3D5CACA495FFBA6716AA86814C9";
    static final String DROP_RESOURCE_SHA256 =
            "9579ACD3BF73526CF9E711DE5016B0582B30468210338422274B3318D3248BDD";
    static final String SHOP_SOURCE_SHA256 =
            "EE673C4BCCB1BBD89D76DEE26BF87F339F9B0C3474F865811905DD5FBFA8D002";
    static final String SHOP_RESOURCE_SHA256 =
            "181E360D4E5F8D486EE87303CBFCCA6F6A3BE1F0D3A66E6D9C63D98DE78F866B";
    static final String CRAFT_SOURCE_SHA256 =
            "3324A7481C6AED9D4220071920D449FF3D957A53493469260BDF0C103AE65895";
    static final String STORY_SOURCE_SHA256 =
            "118074CDD5E8FB28557612156845891F5D2D475D911A921C65A3AB021770E18C";
    static final String INSTANCE_SOURCE_SHA256 =
            "D677801FEEC295C8C9321E7B1A277A63E6EB8F71949E1F33065421E14BAA744F";
    static final String TRANSFORM_SOURCE_SHA256 =
            "91F243435E10E3261D708E9D0D9720550DC03611488D250C8822B6770F154EB9";

    static final int BOBO_SPAWN_ID = 605;
    static final int BOBO_NPC_ID = 70098;
    static final String BOBO_HTML = "bobo1";
    static final int BASCH_SPAWN_ID = 1141;
    static final int BASCH_NPC_ID = 70964;
    static final int KIHOLL_SPAWN_ID = 1181;
    static final int KIHOLL_NPC_ID = 80132;
    static final int DSPYM_SPAWN_ID = 1183;
    static final int DSPYM_NPC_ID = 80013;
    static final int ROI_SPAWN_ID = 1297;
    static final int ROI_NPC_ID = 81209;
    static final int ROI_CURSED_GFX = 144;
    static final int ROI_RESTORED_GFX = 4310;
    static final int ROI_QUEST_LEVEL = 40;
    static final Set<Integer> STORY_NPC_IDS = Set.of(
            KIHOLL_NPC_ID, DSPYM_NPC_ID, 80133, 80134, 80135, 80136);
    static final Set<Integer> INSTANCE_ENTRANCE_NPC_IDS = Set.of(
            80133, 80134, 80135, 80136);

    record MapFlags(boolean escapable, boolean usableItem, boolean usableSkill,
                    boolean teleportable, boolean takePets, boolean recallPets) {}
    record ShopOffer(int wireOrderId, int sourceOrderId, int itemId,
                     int unitPrice, int packCount,
                     InventoryCatalog.Definition definition) {}
    private record ShopRow(int npcId, int itemId, int sourceOrderId,
                           int sellingPrice, int packCount,
                           int purchasingPrice, int dailyBuyLimit) {}
    private record ShopCatalog(List<ShopRow> rows,
                               Map<Integer, ClassicWorldDropCatalog.ItemWire> items) {}

    private static final MapFlags FLAGS =
            new MapFlags(true, true, true, false, true, true);
    private static final ShopCatalog BOBO_SHOP = loadShop();

    private ClassicWorldDemonTempleCatalog() {}

    static MapFlags flags() { return FLAGS; }

    static boolean isBobo(ClassicWorldNpcCatalog.StaticNpc npc) {
        return matches(npc, BOBO_SPAWN_ID, BOBO_NPC_ID, 32848, 32991,
                "L1Merchant", BOBO_HTML, BOBO_HTML);
    }

    static boolean isBasch(ClassicWorldNpcCatalog.StaticNpc npc) {
        return matches(npc, BASCH_SPAWN_ID, BASCH_NPC_ID, 32919, 32978,
                "L1Merchant", "", "");
    }

    static boolean isRoi(ClassicWorldNpcCatalog.StaticNpc npc) {
        return matches(npc, ROI_SPAWN_ID, ROI_NPC_ID, 32823, 32895,
                "L1Quest", "", "");
    }

    static boolean isNativeDialogueNpc(ClassicWorldNpcCatalog.StaticNpc npc) {
        return isBasch(npc) || isRoi(npc);
    }

    static String dialogueHtml(ClassicWorldNpcCatalog.StaticNpc npc, int level) {
        return dialogueHtml(npc, level, npc == null ? 0 : npc.gfxId());
    }

    static String dialogueHtml(
            ClassicWorldNpcCatalog.StaticNpc npc, int level, int currentGfx) {
        if (isBasch(npc)) return "basch";
        if (isRoi(npc)) {
            return currentGfx == ROI_RESTORED_GFX && level >= ROI_QUEST_LEVEL
                    ? "roi1" : "roi";
        }
        return "";
    }

    static boolean isRoiTransformSource(
            ClassicWorldNpcCatalog.StaticNpc npc, int currentGfx) {
        return isRoi(npc) && npc.gfxId() == ROI_CURSED_GFX
                && currentGfx == ROI_CURSED_GFX;
    }

    static boolean isRestoredRoi(
            ClassicWorldNpcCatalog.StaticNpc npc, int currentGfx) {
        return isRoi(npc) && npc.gfxId() == ROI_CURSED_GFX
                && currentGfx == ROI_RESTORED_GFX;
    }

    static boolean isStoryNpc(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && npc.mapId() == MAP_ID && STORY_NPC_IDS.contains(npc.npcId());
    }

    static boolean isDeferredInstanceAction(
            ClassicWorldNpcCatalog.StaticNpc npc, String action) {
        return npc != null && npc.mapId() == MAP_ID
                && INSTANCE_ENTRANCE_NPC_IDS.contains(npc.npcId())
                && "ent".equalsIgnoreCase(action);
    }

    static boolean isDeferredRoiFollowerAction(
            ClassicWorldNpcCatalog.StaticNpc npc, String action) {
        return isRoi(npc) && "start".equalsIgnoreCase(action);
    }

    static List<ShopOffer> boboOffers() {
        List<ShopOffer> result = new ArrayList<>();
        int wireOrder = 0;
        for (ShopRow row : BOBO_SHOP.rows()) {
            ClassicWorldDropCatalog.ItemWire item = BOBO_SHOP.items().get(row.itemId());
            result.add(new ShopOffer(wireOrder++, row.sourceOrderId(), row.itemId(),
                    row.sellingPrice(), row.packCount() == 0 ? 1 : row.packCount(),
                    item.inventoryDefinition()));
        }
        return List.copyOf(result);
    }

    private static boolean matches(ClassicWorldNpcCatalog.StaticNpc npc,
                                   int spawnId, int npcId, int x, int y,
                                   String implementation, String normal, String chaotic) {
        return npc != null && npc.mapId() == MAP_ID && npc.spawnId() == spawnId
                && npc.npcId() == npcId
                && npc.objectId() == ClassicWorldNpcCatalog.objectId(spawnId)
                && npc.x() == x && npc.y() == y
                && npc.implementation().equals(implementation)
                && npc.normalAction().equals(normal) && npc.chaoticAction().equals(chaotic);
    }

    private static ShopCatalog loadShop() {
        String resourceName = "/world/map410-bobo-shop.lws.gz";
        InputStream resource = ClassicWorldDemonTempleCatalog.class
                .getResourceAsStream(resourceName);
        if (resource == null) throw new ExceptionInInitializerError("missing Bobo shop");
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(resource))) {
            if (in.readInt() != 0x4C575331 || in.readUnsignedShort() != MAP_ID) {
                throw new IOException("invalid Bobo shop header");
            }
            int rows = in.readInt();
            int items = in.readInt();
            String sourceHash = HexFormat.of().withUpperCase().formatHex(in.readNBytes(32));
            byte[] table = in.readAllBytes();
            if (rows != 3 || items != 3 || !SHOP_SOURCE_SHA256.equals(sourceHash)
                    || !sourceHash.equals(sha256(table))
                    || !SHOP_RESOURCE_SHA256.equals(sha256Resource(resourceName))) {
                throw new IOException("Bobo shop source contract changed");
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
                throw new IOException("Bobo shop TSV header changed");
            }
            for (String line; (line = reader.readLine()) != null;) {
                if (line.equals("[items]")) break;
                String[] f = line.split("\t", -1);
                if (f.length != 8) throw new IOException("Bobo shop field count changed");
                ShopRow row = new ShopRow(integer(f[0]), integer(f[1]), integer(f[2]),
                        integer(f[3]), integer(f[4]), integer(f[5]), integer(f[7]));
                if (row.npcId() != BOBO_NPC_ID || row.sellingPrice() <= 0
                        || row.packCount() < 0 || row.purchasingPrice() != -1
                        || row.dailyBuyLimit() != 0) {
                    throw new IOException("invalid Bobo shop row");
                }
                rows.add(row);
                referenced.add(row.itemId());
            }
            if (rows.size() != expectedRows || !itemHeader.equals(reader.readLine())) {
                throw new IOException("Bobo shop/item boundary changed");
            }
            for (String line; (line = reader.readLine()) != null;) {
                String[] f = line.split("\t", -1);
                if (f.length != 17) throw new IOException("Bobo item field count changed");
                int itemId = integer(f[0]);
                items.put(itemId, new ClassicWorldDropCatalog.ItemWire(
                        itemId, f[1], f[2], f[3], integer(f[4]), integer(f[5]),
                        integer(f[6]), integer(f[7]) != 0, integer(f[8]) != 0,
                        integer(f[9]), integer(f[10]), f[11], integer(f[12]),
                        integer(f[13]), f[14], integer(f[15]), integer(f[16])));
            }
        }
        if (items.size() != expectedItems || !items.keySet().equals(referenced)) {
            throw new IOException("Bobo shop rows/items disagree");
        }
        return new ShopCatalog(List.copyOf(rows), Map.copyOf(items));
    }

    private static int integer(String value) { return Integer.parseInt(value); }

    private static String sha256Resource(String name) throws IOException {
        InputStream stream = ClassicWorldDemonTempleCatalog.class.getResourceAsStream(name);
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

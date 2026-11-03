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

/** Exact selected-source Ship Grave merchant surface. */
final class ClassicWorldPirateShipGraveCatalog {
    static final int MAP_ID = 550;
    static final int MERCHANT_SPAWN_ID = 971;
    static final int MERCHANT_NPC_ID = 80075;
    static final String MERCHANT_HTML = "vehikeu1";
    static final String SHOP_SOURCE_SHA256 =
            "E78D53D2BCD74F5476510C79E32BE7FB73347D1FE223AC2F9FD3259A6D258240";
    static final String SHOP_RESOURCE_SHA256 =
            "A71727773A35AB57CDE4A9F9CFB5B878848F7C2E2D97285A66090AA4DB42523C";
    static final int VOYAGER_SPAWN_ID = 972;
    static final int VOYAGER_NPC_ID = 80076;
    static final int ALDRAN_SPAWN_ID = 973;
    static final int ALDRAN_NPC_ID = 80077;
    static final int COMPLETE_VOYAGE_LOG = 41058;
    static final int ODD_INCOMPLETE_LOG = 49082;
    static final int EVEN_INCOMPLETE_LOG = 49083;
    static final int FIRST_LOG_PAGE = 41038;
    static final int SECOND_LOG_PAGE = 41039;
    static final int CREW_CORPSE = 41207;
    static final int WEAK_SOUL = 41208;
    static final String ALDRAN_MISSING_HTML = "aldran9";
    static final String RETAINED_DIALOGUE_SOURCE_SHA256 =
            "357612978AACEFB49412D046C78104E4CE166A63A68F5BCFBB495A0DF03E96AA";
    static final String WEAK_SOUL_SOURCE_SHA256 =
            "9C6082A3430983D44FA6E412BCA6F072391DDAAF4C30E52FDCBC69BB9DD7D894";

    record ShopOffer(int wireOrderId, int sourceOrderId, int itemId,
                     int sellingPrice, int packCount,
                     InventoryCatalog.Definition definition) {}

    record AldranRoute(String action, String textLabel, int mapId,
                       int x, int y, int heading) {
        AldranRoute {
            if (action == null || action.length() != 1
                    || textLabel == null || textLabel.isBlank()
                    || (mapId != 550 && mapId != 558)
                    || heading < 0 || heading > 7) {
                throw new IllegalArgumentException("invalid Aldran route");
            }
        }

        Protocol.PortalDestination destination() {
            return new Protocol.PortalDestination(mapId, x, y, true);
        }
    }

    private record ShopRow(int npcId, int itemId, int sourceOrderId,
                           int sellingPrice, int packCount,
                           int purchasingPrice, String note,
                           int dailyBuyLimit) {}

    private record ShopCatalog(List<ShopRow> rows,
                               Map<Integer, ClassicWorldDropCatalog.ItemWire> items) {}

    private static final ShopCatalog SHOP = loadShop();

    static boolean isMerchant(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && npc.mapId() == MAP_ID
                && npc.spawnId() == MERCHANT_SPAWN_ID
                && npc.npcId() == MERCHANT_NPC_ID
                && npc.objectId() == ClassicWorldNpcCatalog.objectId(MERCHANT_SPAWN_ID)
                && npc.x() == 32831 && npc.y() == 32695
                && npc.fixedMerchant() && npc.normalAction().equals(MERCHANT_HTML)
                && npc.chaoticAction().equals(MERCHANT_HTML);
    }

    static List<ShopOffer> offers() {
        List<ShopOffer> offers = new ArrayList<>();
        int wireOrder = 0;
        for (ShopRow row : SHOP.rows()) {
            ClassicWorldDropCatalog.ItemWire wire = SHOP.items().get(row.itemId());
            offers.add(new ShopOffer(wireOrder++, row.sourceOrderId(), row.itemId(),
                    row.sellingPrice(), row.packCount() == 0 ? 1 : row.packCount(),
                    wire.inventoryDefinition()));
        }
        return List.copyOf(offers);
    }

    static boolean isVoyager(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && npc.mapId() == MAP_ID
                && npc.spawnId() == VOYAGER_SPAWN_ID
                && npc.npcId() == VOYAGER_NPC_ID;
    }

    static boolean isAldran(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && npc.mapId() == MAP_ID
                && npc.spawnId() == ALDRAN_SPAWN_ID
                && npc.npcId() == ALDRAN_NPC_ID;
    }

    /** Exact branch order retained by L1MerchantInstance for NPC 80076. */
    static String voyagerDialogue(Map<Long, Integer> inventory) {
        if (inventory == null) throw new IllegalArgumentException(
                "voyager inventory is required");
        if (has(inventory, COMPLETE_VOYAGE_LOG)) return "voyager8";
        if (has(inventory, ODD_INCOMPLETE_LOG)
                || has(inventory, EVEN_INCOMPLETE_LOG)) {
            return has(inventory, FIRST_LOG_PAGE) || has(inventory, SECOND_LOG_PAGE)
                    ? "voyager9" : "voyager7";
        }
        // The retained later 49082..49091 branch is unreachable for 49082/3,
        // but still governs its remaining eight incomplete-log variants.
        for (int itemId = 49084; itemId <= 49091; itemId++) {
            if (has(inventory, itemId)) return "voyager7";
        }
        return "voyager1";
    }

    static int voyagerReward(int sourceRoll) {
        if (sourceRoll == 0) return ODD_INCOMPLETE_LOG;
        if (sourceRoll == 1) return EVEN_INCOMPLETE_LOG;
        throw new IllegalArgumentException("voyager source roll must be zero or one");
    }

    static String voyagerRewardHtml(int itemId) {
        return switch (itemId) {
            case ODD_INCOMPLETE_LOG -> "voyager6a";
            case EVEN_INCOMPLETE_LOG -> "voyager6b";
            default -> throw new IllegalArgumentException("not a voyager reward");
        };
    }

    /**
     * Post-February-2013 Aldran action table. Text labels are the exact
     * lowercase-button meanings recovered from this client's Text.pak;
     * coordinates are independently retained by two descendant C_NPCAction2
     * sources. The original teleport call used heading zero.
     */
    static AldranRoute aldranRoute(String action) {
        if (action == null) return null;
        return switch (action) {
            case "a" -> new AldranRoute("a", "水面中央地區",
                    550, 32674, 32871, 0);
            case "b" -> new AldranRoute("b", "水面東邊地區",
                    550, 32778, 33009, 0);
            case "c" -> new AldranRoute("c", "水面西邊地區",
                    550, 32471, 32766, 0);
            case "d" -> new AldranRoute("d", "水面南邊地區",
                    550, 32511, 32998, 0);
            case "e" -> new AldranRoute("e", "深海中央地區",
                    558, 32998, 33028, 0);
            default -> null;
        };
    }

    static InventoryCatalog.Definition inventoryDefinition(int itemId) {
        if (itemId != ODD_INCOMPLETE_LOG && itemId != EVEN_INCOMPLETE_LOG) return null;
        int description = itemId == ODD_INCOMPLETE_LOG ? 2131 : 2136;
        return new ClassicWorldDropCatalog.ItemWire(
                itemId, "etc", "$4403", "normal", 2256, 5614,
                0, false, true, 0, description,
                "", 0, 0, "paper", 0, 0x7F).inventoryDefinition();
    }

    private static boolean has(Map<Long, Integer> inventory, int itemId) {
        return inventory.getOrDefault((long) itemId, 0) > 0;
    }

    private static ShopCatalog loadShop() {
        String name = "/world/map550-shops.lws.gz";
        byte[] packed;
        try (InputStream resource = ClassicWorldPirateShipGraveCatalog.class
                .getResourceAsStream(name)) {
            if (resource == null) throw new IOException("missing Ship Grave shop resource");
            packed = resource.readAllBytes();
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
        if (!SHOP_RESOURCE_SHA256.equals(sha256(packed))) {
            throw new ExceptionInInitializerError("Ship Grave shop resource changed");
        }
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(
                new ByteArrayInputStream(packed)))) {
            if (in.readInt() != 0x4C575331 || in.readUnsignedShort() != MAP_ID) {
                throw new IOException("invalid Ship Grave shop header");
            }
            int rows = in.readInt();
            int items = in.readInt();
            byte[] hash = in.readNBytes(32);
            byte[] table = in.readAllBytes();
            String source = HexFormat.of().withUpperCase().formatHex(hash);
            if (rows != 18 || items != 18 || !SHOP_SOURCE_SHA256.equals(source)
                    || !source.equals(sha256(table))) {
                throw new IOException("Ship Grave shop source contract changed");
            }
            return parseShop(table, rows, items);
        } catch (IOException | RuntimeException error) {
            ExceptionInInitializerError wrapped = new ExceptionInInitializerError(
                    "cannot load Ship Grave shop: " + error.getMessage());
            wrapped.initCause(error);
            throw wrapped;
        }
    }

    private static ShopCatalog parseShop(byte[] table, int expectedRows,
                                         int expectedItems) throws IOException {
        String shopHeader = String.join("\t", "npc_id", "item_id", "order_id",
                "selling_price", "pack_count", "purchasing_price", "note",
                "daily_buy_limit");
        String itemHeader = String.join("\t", "item_id", "kind", "name_id",
                "use_token", "inventory_gfx", "ground_gfx", "blessing",
                "stackable", "tradable", "unit_weight", "item_description_id",
                "weapon_type", "damage_small", "damage_large", "material",
                "safe_enchant", "class_mask");
        List<ShopRow> rows = new ArrayList<>();
        Map<Integer, ClassicWorldDropCatalog.ItemWire> items = new LinkedHashMap<>();
        Set<Integer> referenced = new LinkedHashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(table), StandardCharsets.UTF_8))) {
            if (!"[shops]".equals(reader.readLine())
                    || !shopHeader.equals(reader.readLine())) {
                throw new IOException("Ship Grave shop TSV header changed");
            }
            int previousItem = 0;
            for (String line; (line = reader.readLine()) != null;) {
                if (line.equals("[items]")) break;
                String[] field = line.split("\t", -1);
                if (field.length != 8) throw new IOException("invalid shop row");
                ShopRow row = new ShopRow(integer(field[0]), integer(field[1]),
                        integer(field[2]), integer(field[3]), integer(field[4]),
                        integer(field[5]), field[6], integer(field[7]));
                if (row.npcId() != MERCHANT_NPC_ID || row.itemId() <= previousItem
                        || row.sourceOrderId() < 0 || row.sellingPrice() <= 0
                        || row.packCount() < 0 || row.purchasingPrice() != -1
                        || row.dailyBuyLimit() != 0) {
                    throw new IOException("invalid or unordered Ship Grave shop row");
                }
                previousItem = row.itemId();
                rows.add(row);
                referenced.add(row.itemId());
            }
            if (rows.size() != expectedRows || !itemHeader.equals(reader.readLine())) {
                throw new IOException("Ship Grave shop/item boundary changed");
            }
            int previousWire = 0;
            for (String line; (line = reader.readLine()) != null;) {
                String[] field = line.split("\t", -1);
                if (field.length != 17) throw new IOException("invalid shop item row");
                int itemId = integer(field[0]);
                if (itemId <= previousWire) throw new IOException("shop items unordered");
                previousWire = itemId;
                items.put(itemId, new ClassicWorldDropCatalog.ItemWire(
                        itemId, field[1], field[2], field[3], integer(field[4]),
                        integer(field[5]), integer(field[6]), integer(field[7]) != 0,
                        integer(field[8]) != 0, integer(field[9]), integer(field[10]),
                        field[11], integer(field[12]), integer(field[13]), field[14],
                        integer(field[15]), integer(field[16])));
            }
        }
        if (items.size() != expectedItems || !items.keySet().equals(referenced)) {
            throw new IOException("Ship Grave shop rows/items disagree");
        }
        return new ShopCatalog(List.copyOf(rows), Map.copyOf(items));
    }

    private static int integer(String value) { return Integer.parseInt(value); }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private ClassicWorldPirateShipGraveCatalog() {}
}

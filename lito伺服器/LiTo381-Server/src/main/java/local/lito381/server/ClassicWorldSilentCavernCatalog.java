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

/** Hash-locked native Silent Cavern / Bludika Cave component. */
final class ClassicWorldSilentCavernCatalog {
    static final List<Integer> MAP_IDS = List.of(304, 305);
    static final int SILENT_CAVERN_MAP_ID = 304;
    static final int BLUDIKA_CAVE_MAP_ID = 305;
    static final String ROUTES_SOURCE_SHA256 =
            "8A6A340CD5700ED3CE2C5EE472F617AB85804F5014932E00D305CE6B297B2D44";
    static final String FLAGS_SOURCE_SHA256 =
            "B6BF0243A1FFB277A39A179854680665ECE7E8DFD7F1850FA36A775A1433FC65";
    static final String GETBACK_SOURCE_SHA256 =
            "D085B9DD5C474E494A355530CA2EF0ED75F1B40118B42D5C7BAB27CF0DD94768";
    static final String TEXT_SOURCE_SHA256 =
            "F977EB2FF02F8A64693A2701D2D981C1A7ED91DADDC0E9E890C6E95BA8CE576B";
    static final String STORY_SLICE_SOURCE_SHA256 =
            "1A59725C4AAEA23D895426EA2306B417377EB8A15A6D9FD4A66B1348CF3982EE";
    static final String SHOP_SOURCE_SHA256 =
            "8611657705536E0890B14CF23D9DF699210453B6C78BF8C74980EBA02DCF37F7";
    static final String SHOP_RESOURCE_SHA256 =
            "7F3F52DB1215B850036490342CCAA8AC762045AB240F2541C40D7A6F1C3CA602";

    static final int TELEPORTER_SPAWN_ID = 109;
    static final int TELEPORTER_NPC_ID = 50068;
    static final String TELEPORT_INITIAL_HTML = "telescave1";
    static final String TELEPORT_CONFIRM_HTML = "telescave2";
    static final String TELEPORT_CONFIRM_ACTION = "teleportURL";
    static final String TEXT_ONLY_SILVER_ACTION = "teleport scave-silver";
    static final int SCWATY_SPAWN_ID = 596;
    static final int SCWATY_NPC_ID = 70088;
    static final String SCWATY_HTML = "scwaty1";
    static final int HADESTY_SPAWN_ID = 915;
    static final int HADESTY_NPC_ID = 70910;
    static final String HADESTY_HTML = "hadesty1";
    static final String HADESTY_HEALED_HTML = "hadesty2";
    static final String FULL_HEAL_ACTION = "fullheal";
    static final int KANGUARD_NPC_ID = 70885;
    static final int RONDE_NPC_ID = 70892;
    static final int KIMA_NPC_ID = 70906;
    static final int PEARS_NPC_ID = 70908;
    static final int BLUDIKA_NPC_ID = 70895;
    static final int BLUDIKA_ASSASSIN_NPC_ID = 70896;
    static final int EXCLUDED_CUSTOM_SPAWN_ID = 1076;

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
            304, "6B1F05F7F87D6CD082ADB7A73591F5517D65572505A475126923B982D1973B6F",
            305, "9037A9D16B181AF91F242A322A07B9721081DA7BFEB5F31AC2D865CE8238553A");
    static final Map<Integer, String> MAP_RESOURCE_SHA256 = Map.of(
            304, "799C173A7D41604BBB9D440980BAA3227AC2A96D02775C08D3B29582199E89BC",
            305, "C2938BFEA27A37F903BB11D032D93BE0E255D338AC5A335A678E84432B7869AD");
    static final Map<Integer, SourceContract> NPC_CONTRACTS = Map.of(
            304, new SourceContract(7, 0,
                    "91B2C7B4D826F3796F8E089314005C86180EBB89132CA1EB29B04E7EF638B6EE",
                    "A983EB408BD5220B998B3479ECF455CE40AAF9DB778E256CCB5387F1614641A6"),
            305, new SourceContract(7, 0,
                    "918135E37077DEAC613156DD27940FFE42FABA4EA05F95762B9E96A53ACDA05D",
                    "AB13CAF56614BC54D5BEE681222703544D47B3862B85A0133445A7E3F0C8A1E3"));
    static final Map<Integer, SourceContract> MONSTER_CONTRACTS = Map.of(
            304, new SourceContract(24, 370,
                    "DD756E8163916823E3ECC89CC46EDE5BFA235CE38BE24462B8354B067B211B86",
                    "4A133E414E84BAA6A6ABBFD0C62A3DC071C02EB53CCF6AE96C0E983C0D6F1A9D"),
            305, new SourceContract(0, 0,
                    "219255B3830F63B32C0D35542B91DF47B5532113719CB79075F5CD233B6DE5E7",
                    "E51926D214F9D4F9FDA691F8AFFD72925DD596E2C869EE41A8AC6E427A1817BA"));
    static final Map<Integer, SourceContract> DROP_CONTRACTS = Map.of(
            304, new SourceContract(146, 69,
                    "208B9733F4E9FE9799568D059660432FFDB76AF15E6EBF74CF8A42F4CEBA5F52",
                    "3EACA41D1650C315135C7E7D8154F43A74C387EB33373A4A311E66072C2855A5"),
            305, new SourceContract(0, 0,
                    "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828",
                    "9537A88BF5088012980999C4029A725F9DA47FD30C788B48CC28F204AD75904F"));

    private static final Map<Integer, MapFlags> FLAGS = Map.of(
            304, new MapFlags(304, "沉默洞穴", 32576, 32959, 32768, 33023,
                    true, true, true, true, true, true),
            305, new MapFlags(305, "布魯迪卡洞穴", 32704, 32767, 32768, 32831,
                    true, true, true, true, true, true));
    private static final Set<Integer> STORY_NPC_IDS = Set.of(
            PEARS_NPC_ID, KANGUARD_NPC_ID, RONDE_NPC_ID, KIMA_NPC_ID,
            BLUDIKA_NPC_ID);
    private static final ShopCatalog SCWATY_SHOP = loadShop();

    private ClassicWorldSilentCavernCatalog() {}

    static MapFlags flags(int mapId) {
        MapFlags result = FLAGS.get(mapId);
        if (result == null) throw new IllegalArgumentException("not a Silent Cavern map");
        return result;
    }

    static boolean isTeleporter(ClassicWorldNpcCatalog.StaticNpc npc) {
        return matches(npc, SILENT_CAVERN_MAP_ID, TELEPORTER_SPAWN_ID,
                TELEPORTER_NPC_ID, 32894, 32886, "L1Teleporter", TELEPORT_INITIAL_HTML);
    }

    static boolean isScwaty(ClassicWorldNpcCatalog.StaticNpc npc) {
        return matches(npc, SILENT_CAVERN_MAP_ID, SCWATY_SPAWN_ID,
                SCWATY_NPC_ID, 32870, 32893, "L1Merchant", SCWATY_HTML);
    }

    static boolean isHadesty(ClassicWorldNpcCatalog.StaticNpc npc) {
        return matches(npc, SILENT_CAVERN_MAP_ID, HADESTY_SPAWN_ID,
                HADESTY_NPC_ID, 32859, 32899, "L1Merchant", HADESTY_HTML);
    }

    static boolean isStoryNpc(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && MAP_IDS.contains(npc.mapId())
                && STORY_NPC_IDS.contains(npc.npcId())
                && ClassicWorldStoryQuestCatalog.hasNpc(npc.npcId());
    }

    static boolean isDialogueNpc(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && npc.mapId() == BLUDIKA_CAVE_MAP_ID
                && npc.npcId() == BLUDIKA_ASSASSIN_NPC_ID
                && npc.implementation().equals("L1Npc")
                && npc.normalAction().equals("bluedum")
                && npc.chaoticAction().equals("bluedum");
    }

    static boolean isTextOnlyTeleportAction(
            ClassicWorldNpcCatalog.StaticNpc npc, String action) {
        return isTeleporter(npc)
                && TEXT_ONLY_SILVER_ACTION.equalsIgnoreCase(action);
    }

    static List<ShopOffer> scwatyOffers() {
        List<ShopOffer> offers = new ArrayList<>();
        int wireOrder = 0;
        for (ShopRow row : SCWATY_SHOP.rows()) {
            ClassicWorldDropCatalog.ItemWire item = SCWATY_SHOP.items().get(row.itemId());
            offers.add(new ShopOffer(wireOrder++, row.sourceOrderId(), row.itemId(),
                    row.sellingPrice(), row.packCount() == 0 ? 1 : row.packCount(),
                    item.inventoryDefinition()));
        }
        return List.copyOf(offers);
    }

    private static boolean matches(ClassicWorldNpcCatalog.StaticNpc npc,
                                   int mapId, int spawnId, int npcId, int x, int y,
                                   String implementation, String html) {
        return npc != null && npc.mapId() == mapId && npc.spawnId() == spawnId
                && npc.npcId() == npcId && npc.objectId() == ClassicWorldNpcCatalog.objectId(spawnId)
                && npc.x() == x && npc.y() == y && npc.implementation().equals(implementation)
                && npc.normalAction().equals(html) && npc.chaoticAction().equals(html);
    }

    private static ShopCatalog loadShop() {
        String resourceName = "/world/map304-scwaty-shop.lws.gz";
        InputStream resource = ClassicWorldSilentCavernCatalog.class
                .getResourceAsStream(resourceName);
        if (resource == null) throw new ExceptionInInitializerError("missing Scwaty shop");
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(resource))) {
            if (in.readInt() != 0x4C575331 || in.readUnsignedShort() != 304) {
                throw new IOException("invalid Scwaty shop header");
            }
            int rows = in.readInt();
            int items = in.readInt();
            String sourceHash = HexFormat.of().withUpperCase().formatHex(in.readNBytes(32));
            byte[] table = in.readAllBytes();
            if (rows != 26 || items != 26 || !SHOP_SOURCE_SHA256.equals(sourceHash)
                    || !sourceHash.equals(sha256(table))
                    || !SHOP_RESOURCE_SHA256.equals(sha256Resource(resourceName))) {
                throw new IOException("Scwaty shop source contract changed");
            }
            return parseShop(table, rows, items);
        } catch (IOException | IllegalArgumentException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static ShopCatalog parseShop(byte[] table, int expectedRows, int expectedItems)
            throws IOException {
        String shopHeader = String.join("\t", "npc_id", "item_id", "order_id",
                "selling_price", "pack_count", "purchasing_price", "note", "daily_buy_limit");
        String itemHeader = String.join("\t", "item_id", "kind", "name_id", "use_token",
                "inventory_gfx", "ground_gfx", "blessing", "stackable", "tradable", "unit_weight",
                "item_description_id", "weapon_type", "damage_small", "damage_large", "material",
                "safe_enchant", "class_mask");
        List<ShopRow> rows = new ArrayList<>();
        Map<Integer, ClassicWorldDropCatalog.ItemWire> items = new LinkedHashMap<>();
        Set<Integer> referenced = new LinkedHashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(table), StandardCharsets.UTF_8))) {
            if (!"[shops]".equals(reader.readLine()) || !shopHeader.equals(reader.readLine())) {
                throw new IOException("Scwaty shop TSV header changed");
            }
            for (String line; (line = reader.readLine()) != null;) {
                if (line.equals("[items]")) break;
                String[] f = line.split("\t", -1);
                if (f.length != 8) throw new IOException("Scwaty shop field count changed");
                ShopRow row = new ShopRow(integer(f[0]), integer(f[1]), integer(f[2]),
                        integer(f[3]), integer(f[4]), integer(f[5]), f[6], integer(f[7]));
                if (row.npcId() != SCWATY_NPC_ID || row.sellingPrice() <= 0
                        || row.packCount() < 0 || row.purchasingPrice() != -1
                        || row.dailyBuyLimit() != 0) {
                    throw new IOException("invalid Scwaty shop row");
                }
                rows.add(row);
                referenced.add(row.itemId());
            }
            if (rows.size() != expectedRows || !itemHeader.equals(reader.readLine())) {
                throw new IOException("Scwaty shop/item boundary changed");
            }
            for (String line; (line = reader.readLine()) != null;) {
                String[] f = line.split("\t", -1);
                if (f.length != 17) throw new IOException("Scwaty item field count changed");
                int itemId = integer(f[0]);
                items.put(itemId, new ClassicWorldDropCatalog.ItemWire(
                        itemId, f[1], f[2], f[3], integer(f[4]), integer(f[5]),
                        integer(f[6]), integer(f[7]) != 0, integer(f[8]) != 0,
                        integer(f[9]), integer(f[10]), f[11], integer(f[12]),
                        integer(f[13]), f[14], integer(f[15]), integer(f[16])));
            }
        }
        if (items.size() != expectedItems || !items.keySet().equals(referenced)) {
            throw new IOException("Scwaty shop rows/items disagree");
        }
        return new ShopCatalog(List.copyOf(rows), Map.copyOf(items));
    }

    private static int integer(String value) { return Integer.parseInt(value); }

    private static String sha256Resource(String name) throws IOException {
        InputStream stream = ClassicWorldSilentCavernCatalog.class.getResourceAsStream(name);
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

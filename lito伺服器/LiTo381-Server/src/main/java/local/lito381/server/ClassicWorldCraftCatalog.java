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
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/** Hash-locked selected-source NPC crafting recipes and exact item wire rows. */
final class ClassicWorldCraftCatalog {
    static final int NERUPA_MAP_ID = 17;
    static final int NERUPA_SPAWN_ID = 1327;
    static final int NERUPA_NPC_ID = 70838;
    static final long NERUPA_OBJECT_ID =
            ClassicWorldNpcCatalog.objectId(NERUPA_SPAWN_ID);
    static final String NERUPA_INITIAL_HTML = "nerupaE1";
    static final int NERUPA_RECIPE_COUNT = 32;
    static final int NERUPA_ITEM_COUNT = 58;
    static final String NERUPA_SOURCE_SHA256 =
            "2828CAFBFCEBCA9FC71F8C958BE68CCC09D49C17B688036DB87B78144B4AD183";
    static final String NERUPA_RESOURCE_SHA256 =
            "45035435DADC7AA5158DC53986125FCCF9C5D67068160D7FB7233383B6E3C85E";
    static final int FLAME_MAP_ID = 12;
    static final int FLAME_SPAWN_ID = 1010;
    static final int FLAME_NPC_ID = 81114;
    static final long FLAME_OBJECT_ID =
            ClassicWorldNpcCatalog.objectId(FLAME_SPAWN_ID);
    static final String FLAME_INITIAL_HTML = "fsflame1";
    static final int FLAME_RECIPE_COUNT = 3;
    static final int FLAME_ITEM_COUNT = 7;
    static final String FLAME_SOURCE_SHA256 =
            "3AC87240ED8C621A3FF768D1C04E92A70C8E78ED0264DCC4F1DB107912B9AF07";
    static final String FLAME_RESOURCE_SHA256 =
            "2E79D0CA4286CFE3AB44C1AFBD95000FCA175F1C39572552AE45FD2228DC7789";
    static final int EVEUROL_MAP_ID = 62;
    static final int EVEUROL_SPAWN_ID = ClassicWorldHeineCatalog.EVA_CRAFTER_SPAWN_ID;
    static final int EVEUROL_NPC_ID = ClassicWorldHeineCatalog.EVA_CRAFTER_NPC_ID;
    static final long EVEUROL_OBJECT_ID =
            ClassicWorldNpcCatalog.objectId(EVEUROL_SPAWN_ID);
    static final int EVEUROL_RECIPE_COUNT = 4;
    static final int EVEUROL_ITEM_COUNT = 8;
    static final int DIADRAD_CONSUL_SPAWN_ID = 132;
    static final int DIADRAD_ENO_SPAWN_ID = 750;
    static final int DIADRAD_ADELIO_SPAWN_ID = 940;
    static final int DIADRAD_CL_ALCHEMIST_SPAWN_ID = 938;
    static final int DIADRAD_SI_ALCHEMIST_SPAWN_ID = 939;
    static final int DIADRAD_KALBASS_SPAWN_ID = 1014;

    private static final int MAGIC = 0x4C574331; // LWC1
    private static final String RECIPE_HEADER = String.join("\t",
            "id", "npc_ids", "action_name", "amount_inputable", "materials",
            "materials_count", "materials_enchant", "results", "results_count",
            "results_enchant", "success_rate", "fail_item_id", "min_level", "note");
    private static final String ITEM_HEADER = String.join("\t",
            "item_id", "kind", "name_id", "use_token", "inventory_gfx",
            "ground_gfx", "blessing", "stackable", "tradable", "unit_weight",
            "item_description_id", "weapon_type", "damage_small", "damage_large",
            "material", "safe_enchant", "class_mask");
    private static final Catalog NERUPA = load(
            "/world/map17-crafting.lwc.gz", NERUPA_MAP_ID, NERUPA_NPC_ID,
            NERUPA_RECIPE_COUNT, NERUPA_ITEM_COUNT,
            NERUPA_SOURCE_SHA256, NERUPA_RESOURCE_SHA256);
    private static final Catalog FLAME = load(
            "/world/map12-flame-crafting.lwc.gz", FLAME_MAP_ID, FLAME_NPC_ID,
            FLAME_RECIPE_COUNT, FLAME_ITEM_COUNT,
            FLAME_SOURCE_SHA256, FLAME_RESOURCE_SHA256);
    private static final Catalog EVEUROL = load(
            "/world/map62-eveurol-crafting.lwc.gz", EVEUROL_MAP_ID, EVEUROL_NPC_ID,
            EVEUROL_RECIPE_COUNT, EVEUROL_ITEM_COUNT,
            ClassicWorldHeineCatalog.CRAFT_SOURCE_SHA256,
            ClassicWorldHeineCatalog.CRAFT_RESOURCE_SHA256);
    private static final Catalog DIADRAD_CONSUL = load(
            "/world/map320-npc50127-crafting.lwc.gz", 320, 50127, 2, 5,
            "2C33EB0B7EE48772C4A2DEF77D022BE0712134E942A67D230E9F4E5BE45561D3",
            "D3F1C856EF6B9FB085D97276FF1957C35B44ADA657521006A34424A610685148");
    private static final Catalog DIADRAD_ENO = load(
            "/world/map320-npc70707-crafting.lwc.gz", 320, 70707, 2, 4,
            "6C7841BA3207ED602487EA634697186A23320FC75549283F158511221C152CB3",
            "3857FE9FB3905B6DEFAC99D2566BC163AED7A64D8391FB59E4A99DDF674DA080");
    private static final Catalog DIADRAD_ADELIO = load(
            "/world/map320-npc71125-crafting.lwc.gz", 320, 71125, 11, 31,
            "1B9E71F2F48B22732AFD9BE0DB496B2873EDD3FEB2C4181769CECF2F59737201",
            "8FF7FC1ED6A5090601C332AFCBCF38654DDA04425F01CE958A51804F43A10B77");
    private static final Catalog DIADRAD_CL_ALCHEMIST = load(
            "/world/map400-npc71113-crafting.lwc.gz", 400, 71113, 3, 12,
            "209780AEC5E23B5D158E25B67CFC2DC085A97C7C42F33E2E2C3080E33743B430",
            "EEB7D2B2AC25DBDBF7727290EF34E0E824FC866D0E16E599FF5BD116250D682B");
    private static final Catalog DIADRAD_SI_ALCHEMIST = load(
            "/world/map400-npc71114-crafting.lwc.gz", 400, 71114, 4, 9,
            "9E493581812A498D0597AC3CCC0BD56AD2889AD1D5A58F3A08F0B421D221502A",
            "B28AB6A4DA846E8402DC1B135D81CDA7186D2FF7247DB155EE6A35BEEDA30752");
    private static final Catalog DIADRAD_KALBASS = load(
            "/world/map400-npc81120-crafting.lwc.gz", 400, 81120, 1, 5,
            "F55704FD1533C13B148BE000A7756AFD10802B40BB196055615BCC494EABF5F2",
            "09BE25D8D4B7E667C848E8B0895FFCBFD7321890FDAEAD5FC707DDE1E97EB9BE");
    private static final Catalog PIRATE_DUNGEON_ELEMENTAL = load(
            "/world/map444-npc81121-crafting.lwc.gz", 444,
            ClassicWorldPirateDungeonCatalog.CRAFTER_NPC_ID, 1, 3,
            ClassicWorldPirateDungeonCatalog.CRAFT_SOURCE_SHA256,
            ClassicWorldPirateDungeonCatalog.CRAFT_RESOURCE_SHA256);
    private static final Catalog SHIP_GRAVE_VOYAGER = load(
            "/world/map550-npc80076-crafting.lwc.gz", 550,
            ClassicWorldPirateBackHalfCatalog.VOYAGER_NPC_ID, 1, 2,
            "28F0549BE184A84F79FDAF77803EAE650926950544802894AB77977E20EBB09B",
            "BDC012C06A17F881B357D02A3D807088DC9DEDC5535ADF89CD0463796D4B4E4A");
    private static final Catalog SHIP_GRAVE_ALDRAN = load(
            "/world/map550-npc80077-crafting.lwc.gz", 550,
            ClassicWorldPirateBackHalfCatalog.ALDRAN_NPC_ID, 1, 2,
            "2F445BD09DF97A86091479B9708324CFBA47610A5C33B96F2943109C7B145846",
            "86711D66DB4D19415BEF2FF09C2A6A449E44F01171C93D96341AC94EC33B5010");
    private static final Catalog SILVERIA_BATR = load(
            "/world/map1000-npc85025-crafting.lwc.gz", 1000, 85025, 3, 17,
            "D385CB3804724E1085D0186AF6B2554BA40B4C62E578AD45FFE553B5B25C68A6",
            "3ED610BEEB61BFCF4143BA7D6AC4F340A64834977E2D6C6C35A426EDF9380066");
    private static final Catalog BEHEMOTH_PUAL = load(
            "/world/map1001-npc85028-crafting.lwc.gz", 1001, 85028, 7, 29,
            "400ADCDF325C49A01823C29C87AADDD1A73F32036450EAB9F918850904CD77FB",
            "42301780D3144244FF11EEE8D97CD0D49D1015078D0248E212904D0599F6243C");
    private static final List<Catalog> CATALOGS = List.of(
            NERUPA, FLAME, EVEUROL, DIADRAD_CONSUL, DIADRAD_ENO,
            DIADRAD_ADELIO, DIADRAD_CL_ALCHEMIST,
            DIADRAD_SI_ALCHEMIST, DIADRAD_KALBASS,
            PIRATE_DUNGEON_ELEMENTAL, SHIP_GRAVE_VOYAGER, SHIP_GRAVE_ALDRAN,
            SILVERIA_BATR, BEHEMOTH_PUAL);
    private static final Map<Integer, Set<String>> DIADRAD_NATIVE_ACTIONS = Map.of(
            50127, Set.of("request cavern teleport scroll",
                    "request diad teleport scroll"),
            70707, Set.of("request reward of aden one",
                    "request reward of aden two"),
            71125, Set.of("request enforce sword of emperor",
                    "request sword of windblade", "request dualblade of red shadow",
                    "request staff of holyhedron", "request helm of dantes",
                    "request armor of dantes", "request gloves of dantes",
                    "request cloak of dantes", "request boots of dantes",
                    "request ingot of dark"),
            71113, Set.of("request recluse teleport scroll"),
            71114, Set.of("request resist teleport scroll"),
            81120, Set.of("request stone of alchemist"));

    record MakingItem(int itemId, int count, int enchant) {
        MakingItem {
            if (itemId <= 0 || count <= 0 || enchant < -1 || enchant > 0x7F) {
                throw new IllegalArgumentException("invalid classic WORLD crafting item");
            }
        }
    }

    record Recipe(int id, int mapId, int npcId, String action,
                  boolean amountInputable, List<MakingItem> materials,
                  List<MakingItem> results, int successRate,
                  int failItemId, int minimumLevel, String note) {
        Recipe {
            if (id <= 0 || mapId < 0 || npcId <= 0 || action == null
                    || action.isEmpty() || materials == null || materials.isEmpty()
                    || results == null || results.isEmpty()
                    || successRate < 0 || successRate > 1_000
                    || failItemId < 0 || minimumLevel < 0 || note == null) {
                throw new IllegalArgumentException("invalid classic WORLD crafting recipe");
            }
            materials = List.copyOf(materials);
            results = List.copyOf(results);
        }
    }

    private record Catalog(int mapId, int npcId, Map<String, Recipe> recipes,
                           Map<Integer, ClassicWorldDropCatalog.ItemWire> items) {}

    private ClassicWorldCraftCatalog() {}

    static Recipe find(int mapId, int npcId, String action) {
        Catalog catalog = catalog(mapId, npcId);
        Recipe recipe = catalog == null || action == null
                ? null : catalog.recipes().get(action);
        if (recipe == null || !ClassicWorldDiadradCatalog.MAP_IDS.contains(mapId)) {
            return recipe;
        }
        Set<String> visible = DIADRAD_NATIVE_ACTIONS.get(npcId);
        return visible != null && visible.contains(recipe.action()) ? recipe : null;
    }

    static List<Recipe> recipes(int mapId, int npcId) {
        Catalog catalog = catalog(mapId, npcId);
        return catalog == null ? List.of() : List.copyOf(catalog.recipes().values());
    }

    static Recipe recipe(int mapId, int recipeId) {
        return CATALOGS.stream().filter(catalog -> catalog.mapId() == mapId)
                .flatMap(catalog -> catalog.recipes().values().stream())
                .filter(recipe -> recipe.id() == recipeId)
                .findFirst().orElse(null);
    }

    static boolean matchesSource(ClassicWorldNpcCatalog.StaticNpc npc, Recipe recipe) {
        if (npc == null || recipe == null || npc.mapId() != recipe.mapId()
                || npc.npcId() != recipe.npcId()) return false;
        return recipe.mapId() == NERUPA_MAP_ID
                && npc.spawnId() == NERUPA_SPAWN_ID
                && npc.objectId() == NERUPA_OBJECT_ID
                || recipe.mapId() == FLAME_MAP_ID
                && npc.spawnId() == FLAME_SPAWN_ID
                && npc.objectId() == FLAME_OBJECT_ID
                || recipe.mapId() == EVEUROL_MAP_ID
                && npc.spawnId() == EVEUROL_SPAWN_ID
                && npc.objectId() == EVEUROL_OBJECT_ID
                || recipe.mapId() == 320 && recipe.npcId() == 50127
                && npc.spawnId() == DIADRAD_CONSUL_SPAWN_ID
                || recipe.mapId() == 320 && recipe.npcId() == 70707
                && npc.spawnId() == DIADRAD_ENO_SPAWN_ID
                || recipe.mapId() == 320 && recipe.npcId() == 71125
                && npc.spawnId() == DIADRAD_ADELIO_SPAWN_ID
                || recipe.mapId() == 400 && recipe.npcId() == 71113
                && npc.spawnId() == DIADRAD_CL_ALCHEMIST_SPAWN_ID
                || recipe.mapId() == 400 && recipe.npcId() == 71114
                && npc.spawnId() == DIADRAD_SI_ALCHEMIST_SPAWN_ID
                || recipe.mapId() == 400 && recipe.npcId() == 81120
                && npc.spawnId() == DIADRAD_KALBASS_SPAWN_ID
                || recipe.mapId() == 444
                && recipe.npcId() == ClassicWorldPirateDungeonCatalog.CRAFTER_NPC_ID
                && npc.spawnId() == ClassicWorldPirateDungeonCatalog.CRAFTER_SPAWN_ID
                || recipe.mapId() == 550
                && recipe.npcId() == ClassicWorldPirateBackHalfCatalog.VOYAGER_NPC_ID
                && npc.spawnId() == 972
                || recipe.mapId() == 550
                && recipe.npcId() == ClassicWorldPirateBackHalfCatalog.ALDRAN_NPC_ID
                && npc.spawnId() == 973
                || recipe.mapId() == 1000 && recipe.npcId() == 85025
                && npc.spawnId() == 1059
                || recipe.mapId() == 1001 && recipe.npcId() == 85028
                && npc.spawnId() == 1061;
    }

    static InventoryCatalog.Definition inventoryDefinition(int itemId) {
        ClassicWorldDropCatalog.ItemWire wire = itemWire(itemId);
        return wire == null ? null : wire.inventoryDefinition();
    }

    static InventoryCatalog.NativeWeaponStatus nativeWeaponStatus(int itemId) {
        ClassicWorldDropCatalog.ItemWire wire = itemWire(itemId);
        return wire == null ? null : wire.nativeWeaponStatus();
    }

    static int maximumAmount(RuntimeInventory inventory, Recipe recipe) {
        if (inventory == null || recipe == null) return 0;
        long maximum = Integer.MAX_VALUE;
        for (MakingItem material : recipe.materials()) {
            long available = inventory.items().stream()
                    .filter(item -> item.definition().catalogId() == material.itemId())
                    .filter(item -> material.enchant() < 0
                            || item.enchantLevel() >= material.enchant())
                    .mapToLong(RuntimeInventory.Item::quantity).sum();
            maximum = Math.min(maximum, available / material.count());
        }
        return (int) Math.max(0, maximum);
    }

    private static Catalog catalog(int mapId, int npcId) {
        return CATALOGS.stream().filter(candidate -> candidate.mapId() == mapId
                && candidate.npcId() == npcId).findFirst().orElse(null);
    }

    private static ClassicWorldDropCatalog.ItemWire itemWire(int itemId) {
        ClassicWorldDropCatalog.ItemWire selected = null;
        for (Catalog catalog : CATALOGS) {
            ClassicWorldDropCatalog.ItemWire candidate = catalog.items().get(itemId);
            if (candidate == null) continue;
            if (selected != null && !selected.equals(candidate)) {
                throw new IllegalStateException(
                        "classic crafting item metadata conflicts: " + itemId);
            }
            selected = candidate;
        }
        return selected;
    }

    private static Catalog load(
            String resourceName, int contractedMapId, int contractedNpcId,
            int contractedRecipeCount, int contractedItemCount,
            String contractedSourceSha256, String contractedResourceSha256) {
        byte[] packed;
        try (InputStream resource = ClassicWorldCraftCatalog.class
                .getResourceAsStream(resourceName)) {
            if (resource == null) throw new IOException("missing crafting resource");
            packed = resource.readAllBytes();
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
        if (!contractedResourceSha256.equals(sha256(packed))) {
            throw new ExceptionInInitializerError(
                    "classic crafting resource changed " + resourceName);
        }
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(
                new ByteArrayInputStream(packed)))) {
            if (in.readInt() != MAGIC) throw new IOException("invalid crafting magic");
            int mapId = in.readUnsignedShort();
            int npcId = in.readInt();
            int expectedRecipes = in.readInt();
            int expectedItems = in.readInt();
            byte[] sourceHash = in.readNBytes(32);
            if (sourceHash.length != 32) throw new IOException("truncated crafting hash");
            byte[] table = in.readAllBytes();
            String headerHash = HexFormat.of().withUpperCase().formatHex(sourceHash);
            if (!headerHash.equals(sha256(table))
                    || !headerHash.equals(contractedSourceSha256)
                    || mapId != contractedMapId || npcId != contractedNpcId
                    || expectedRecipes != contractedRecipeCount
                    || expectedItems != contractedItemCount) {
                throw new IOException("classic crafting source contract changed");
            }
            Catalog result = parse(mapId, npcId, table);
            if (result.recipes().size() != expectedRecipes
                    || result.items().size() != expectedItems) {
                throw new IOException("classic crafting row count changed");
            }
            return result;
        } catch (IOException | RuntimeException error) {
            ExceptionInInitializerError wrapped = new ExceptionInInitializerError(
                    "cannot load classic crafting resource: " + error.getMessage());
            wrapped.initCause(error);
            throw wrapped;
        }
    }

    private static Catalog parse(int mapId, int npcId, byte[] table) throws IOException {
        Map<String, Recipe> recipes = new LinkedHashMap<>();
        Map<Integer, ClassicWorldDropCatalog.ItemWire> items = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(table), StandardCharsets.UTF_8))) {
            if (!"[recipes]".equals(reader.readLine())
                    || !RECIPE_HEADER.equals(reader.readLine())) {
                throw new IOException("crafting recipe header changed");
            }
            int previousId = 0;
            String line;
            while ((line = reader.readLine()) != null && !"[items]".equals(line)) {
                String[] field = line.split("\t", -1);
                if (field.length != 14) throw new IOException("invalid crafting recipe row");
                int id = integer(field[0]);
                if (id <= previousId || !containsId(field[1], npcId)) {
                    throw new IOException("unordered or mismatched crafting recipe");
                }
                previousId = id;
                List<MakingItem> materials = makingItems(
                        field[4], field[5], field[6], -1);
                List<MakingItem> results = makingItems(
                        field[7], field[8], field[9], 0);
                Recipe recipe = new Recipe(
                        id, mapId, npcId, field[2], integer(field[3]) != 0,
                        materials, results, integer(field[10]), integer(field[11]),
                        integer(field[12]), field[13]);
                if (recipes.put(recipe.action(), recipe) != null) {
                    throw new IOException("duplicate crafting action");
                }
            }
            if (!ITEM_HEADER.equals(reader.readLine())) {
                throw new IOException("crafting item header changed");
            }
            int previousItem = 0;
            while ((line = reader.readLine()) != null) {
                String[] field = line.split("\t", -1);
                if (field.length != 17) throw new IOException("invalid crafting item row");
                int itemId = integer(field[0]);
                if (itemId <= previousItem) throw new IOException("unordered crafting item");
                previousItem = itemId;
                ClassicWorldDropCatalog.ItemWire wire =
                        new ClassicWorldDropCatalog.ItemWire(
                                itemId, field[1], field[2], field[3],
                                integer(field[4]), integer(field[5]), integer(field[6]),
                                integer(field[7]) != 0, integer(field[8]) != 0,
                                integer(field[9]), integer(field[10]), field[11],
                                integer(field[12]), integer(field[13]), field[14],
                                integer(field[15]), integer(field[16]));
                items.put(itemId, wire);
            }
        }
        for (Recipe recipe : recipes.values()) {
            for (MakingItem item : concat(recipe.materials(), recipe.results())) {
                if (!items.containsKey(item.itemId())) {
                    throw new IOException("crafting item definition missing " + item.itemId());
                }
            }
            if (recipe.failItemId() != 0 && !items.containsKey(recipe.failItemId())) {
                throw new IOException("crafting failure item definition missing");
            }
        }
        return new Catalog(mapId, npcId,
                Collections.unmodifiableMap(new LinkedHashMap<>(recipes)),
                Collections.unmodifiableMap(new LinkedHashMap<>(items)));
    }

    private static List<MakingItem> makingItems(
            String idsText, String countsText, String enchantsText,
            int defaultEnchant) throws IOException {
        int[] ids = integers(idsText);
        int[] counts = integers(countsText);
        int[] enchants = enchantsText.isEmpty() ? new int[0] : integers(enchantsText);
        if (ids.length == 0 || ids.length != counts.length
                || (enchants.length != 0 && enchants.length != ids.length)) {
            throw new IOException("crafting parallel item columns changed");
        }
        List<MakingItem> result = new ArrayList<>();
        for (int index = 0; index < ids.length; index++) {
            result.add(new MakingItem(ids[index], counts[index],
                    enchants.length == 0 ? defaultEnchant : enchants[index]));
        }
        return List.copyOf(result);
    }

    private static List<MakingItem> concat(
            List<MakingItem> left, List<MakingItem> right) {
        List<MakingItem> result = new ArrayList<>(left.size() + right.size());
        result.addAll(left);
        result.addAll(right);
        return result;
    }

    private static boolean containsId(String csv, int value) throws IOException {
        for (int candidate : integers(csv)) {
            if (candidate == value) return true;
        }
        return false;
    }

    private static int[] integers(String csv) throws IOException {
        if (csv == null || csv.isEmpty()) return new int[0];
        String[] values = csv.split(",", -1);
        int[] result = new int[values.length];
        for (int index = 0; index < values.length; index++) {
            try {
                result[index] = Integer.parseInt(values[index]);
            } catch (NumberFormatException error) {
                throw new IOException("invalid crafting integer", error);
            }
        }
        return result;
    }

    private static int integer(String value) throws IOException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException error) {
            throw new IOException("invalid crafting integer", error);
        }
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}

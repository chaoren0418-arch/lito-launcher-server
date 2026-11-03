package local.lito381.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Restores the original crown level-15 hunt that the selected DB leaves
 * disconnected: ordinary 81066 actors south of Kent and conditional quest
 * items from ordinary 81066/45126 deaths.
 */
final class ClassicWorldRoyalTrialCatalog {
    static final int CROWN_CHARACTER_TYPE = 0;
    static final int SEARCH_STAGE_QUEST_ID = 10101;
    static final int GUNTER_STAGE_QUEST_ID = 10102;
    static final int LIFE_STAGE_QUEST_ID = 10103;
    static final int STONE_GOLEM_NPC_ID = 45126;
    static final int BLACK_KNIGHT_SEARCH_PARTY_NPC_ID = 81066;
    static final long LIFE_SCROLL_ITEM_ID = InventoryCatalog.LIFE_SCROLL;
    static final long SEARCH_DOCUMENT_ITEM_ID = InventoryCatalog.SEARCH_DOCUMENT;
    static final int QUEST_DROP_PERCENT = 40;
    static final int BLACK_KNIGHT_SPAWN_ROWS = 13;
    static final int BLACK_KNIGHT_POPULATION = 26;
    static final int STONE_GOLEM_SPAWN_ROWS = 1;
    static final int STONE_GOLEM_POPULATION = 10;

    private static final ClassicWorldMonsterCatalog.MonsterTemplate BLACK_KNIGHT =
            new ClassicWorldMonsterCatalog.MonsterTemplate(
                    BLACK_KNIGHT_SEARCH_PARTY_NPC_ID, "$1929", "L1Monster", 4003,
                    16, 100, 10, -10, 16, 16, 15, 12, 12, 10, 257, -15,
                    "small", 0, 2, 480, 1120, 0, 0, 0, 0,
                    true, 0, false, false, false, 14, false);

    private static final ClassicWorldMonsterCatalog.MonsterTemplate STONE_GOLEM =
            new ClassicWorldMonsterCatalog.MonsterTemplate(
                    STONE_GOLEM_NPC_ID, "$56", "L1Monster", 49,
                    13, 150, 50, -1, 58, 16, 10, 6, 10, 5, 170, -12,
                    "large", 2, 1, 1280, 1840, 0, 0, 0, 0,
                    false, 0, true, false, false, 0, false);

    /* Exact classic map4 rows 8335-8341 and 26553-26558. */
    private static final List<ClassicWorldMonsterCatalog.SpawnDefinition>
            BLACK_KNIGHT_SPAWNS = List.of(
                    spawn(8335, "Black Knight", 32798, 32944, 0),
                    spawn(8336, "Black Knight", 32822, 32947, 0),
                    spawn(8337, "Black Knight", 32856, 32955, 0),
                    spawn(8338, "Black Knight", 32888, 32953, 0),
                    spawn(8339, "Black Knight", 32914, 32936, 0),
                    spawn(8340, "Black Knight", 32884, 32918, 0),
                    spawn(8341, "Black Knight", 32843, 32927, 0),
                    spawn(26553, "Black Knight Search Party", 32913, 32912, 6),
                    spawn(26554, "Black Knight Search Party", 32886, 32907, 3),
                    spawn(26555, "Black Knight Search Party", 32848, 32899, 6),
                    spawn(26556, "Black Knight Search Party", 32814, 32907, 6),
                    spawn(26557, "Black Knight Search Party", 32781, 32949, 4),
                    spawn(26558, "Black Knight Search Party", 32915, 32947, 6));

    /* Pinned l1j-en/classic 103e6f6 spawn 200009: the retained DB omits it. */
    private static final List<ClassicWorldMonsterCatalog.SpawnDefinition>
            STONE_GOLEM_SPAWNS = List.of(new ClassicWorldMonsterCatalog.SpawnDefinition(
                    ClassicWorldMapCatalog.ROYAL_TRIAL_CAVE_SECOND_MAP_ID,
                    200009, "Stone Golem", 10, 0, 32700, 32870, 0, 0,
                    32640, 32768, 32831, 32895, 5,
                    0, 0, true, 0, false, 0, STONE_GOLEM));

    /* Exact selected 380_db droplist rows for ordinary NPC 81066. */
    private static final List<ClassicWorldDropCatalog.DropRule> BLACK_KNIGHT_DROPS =
            List.of(
                    drop(40015, 50_000), drop(40014, 50_000),
                    drop(20213, 0), drop(20205, 0), drop(20162, 0),
                    drop(20154, 0), drop(20149, 0), drop(20085, 10_000),
                    drop(20063, 10_000), drop(103, 0), drop(94, 0),
                    drop(93, 0), drop(52, 6_000), drop(40047, 3_000),
                    drop(40046, 3_000), drop(40045, 3_000),
                    drop(40044, 10_000));

    /* Exact selected 380_db droplist rows for ordinary NPC 45126. */
    private static final List<ClassicWorldDropCatalog.DropRule> STONE_GOLEM_DROPS =
            List.of(
                    stoneDrop(136, 1, 1, 0), stoneDrop(137, 1, 1, 0),
                    stoneDrop(138, 1, 1, 0), stoneDrop(139, 1, 1, 0),
                    stoneDrop(143, 1, 1, 0), stoneDrop(40007, 1, 1, 0),
                    stoneDrop(40015, 1, 1, 10_000),
                    stoneDrop(40045, 1, 1, 3_000),
                    stoneDrop(40049, 1, 1, 3_000),
                    stoneDrop(40053, 1, 1, 500),
                    stoneDrop(40177, 1, 1, 10_000),
                    stoneDrop(40234, 1, 1, 10_000),
                    stoneDrop(40265, 1, 1, 0),
                    stoneDrop(40267, 1, 1, 2_000),
                    stoneDrop(40304, 1, 1, 10_000),
                    stoneDrop(40308, 37, 72, ClassicWorldDropCatalog.CHANCE_SCALE),
                    stoneDrop(40494, 1, 1, 10_000),
                    stoneDrop(40496, 1, 1, 5_000));

    private static final Map<Integer, ClassicWorldDropCatalog.ItemWire>
            MISSING_MAP4_ITEM_WIRES = missingMap4ItemWires();
    private static final Map<Integer, ClassicWorldDropCatalog.ItemWire>
            MISSING_MAP2_ITEM_WIRES = missingMap2ItemWires();

    record QuestDropDecision(
            int npcId, long itemId, int roll, boolean crown,
            boolean trialActive, boolean alreadyOwned, boolean awarded) {
        QuestDropDecision {
            if ((npcId != STONE_GOLEM_NPC_ID
                    && npcId != BLACK_KNIGHT_SEARCH_PARTY_NPC_ID)
                    || (itemId != LIFE_SCROLL_ITEM_ID
                        && itemId != SEARCH_DOCUMENT_ITEM_ID)
                    || roll < 0 || roll >= 100
                    || awarded != (crown && trialActive && !alreadyOwned
                                   && roll < QUEST_DROP_PERCENT)) {
                throw new IllegalArgumentException("invalid royal trial quest drop decision");
            }
        }
    }

    private ClassicWorldRoyalTrialCatalog() {}

    static List<ClassicWorldMonsterCatalog.SpawnDefinition> definitions(int mapId) {
        if (mapId == ClassicWorldMapCatalog.WORLD_TREE_MAP_ID) {
            return BLACK_KNIGHT_SPAWNS;
        }
        return mapId == ClassicWorldMapCatalog.ROYAL_TRIAL_CAVE_SECOND_MAP_ID
                ? STONE_GOLEM_SPAWNS : List.of();
    }

    static List<ClassicWorldDropCatalog.DropRule> ordinaryRules(int mapId, int npcId) {
        if (mapId == ClassicWorldMapCatalog.WORLD_TREE_MAP_ID
                && npcId == BLACK_KNIGHT_SEARCH_PARTY_NPC_ID
                ) return BLACK_KNIGHT_DROPS;
        return mapId == ClassicWorldMapCatalog.ROYAL_TRIAL_CAVE_SECOND_MAP_ID
                && npcId == STONE_GOLEM_NPC_ID ? STONE_GOLEM_DROPS : List.of();
    }

    static QuestDropDecision decideQuestDrop(
            int npcId, int characterType, Map<Integer, Integer> questSteps,
            Map<Long, Integer> inventory, int zeroBasedRoll) {
        long itemId = switch (npcId) {
            case STONE_GOLEM_NPC_ID -> LIFE_SCROLL_ITEM_ID;
            case BLACK_KNIGHT_SEARCH_PARTY_NPC_ID -> SEARCH_DOCUMENT_ITEM_ID;
            default -> 0;
        };
        if (itemId == 0) return null;
        if (questSteps == null || inventory == null
                || zeroBasedRoll < 0 || zeroBasedRoll >= 100) {
            throw new IllegalArgumentException("invalid royal trial quest drop input");
        }
        boolean crown = characterType == CROWN_CHARACTER_TYPE;
        boolean active = questSteps.containsKey(SEARCH_STAGE_QUEST_ID)
                && questSteps.getOrDefault(LIFE_STAGE_QUEST_ID, 0) < 1;
        boolean owned = inventory.getOrDefault(itemId, 0) > 0;
        boolean awarded = crown && active && !owned
                && zeroBasedRoll < QUEST_DROP_PERCENT;
        return new QuestDropDecision(
                npcId, itemId, zeroBasedRoll, crown, active, owned, awarded);
    }

    static List<ClassicWorldDropCatalog.RolledItem> rollOrdinaryDrops(
            int mapId, int npcId, double itemRate, double adenaRate, double mapRate,
            RandomGenerator random) {
        if (!Double.isFinite(itemRate) || itemRate < 0
                || !Double.isFinite(adenaRate) || adenaRate < 0
                || !Double.isFinite(mapRate) || mapRate < 0 || random == null) {
            throw new IllegalArgumentException("invalid royal trial drop rate/random");
        }
        List<ClassicWorldDropCatalog.RolledItem> result = new ArrayList<>();
        for (ClassicWorldDropCatalog.DropRule rule : ordinaryRules(mapId, npcId)) {
            int roll = random.nextInt(ClassicWorldDropCatalog.CHANCE_SCALE) + 1;
            double finalChance = rule.chance() * itemRate * mapRate;
            if (itemRate == 0 || finalChance < roll) continue;
            ClassicWorldDropCatalog.ItemWire item = itemWire(mapId, rule.itemId());
            if (item == null) {
                throw new IllegalStateException(
                        "royal trial item wire row vanished " + rule.itemId());
            }
            int quantity = rule.minimum();
            int span = rule.maximum() - rule.minimum() + 1;
            if (span > 1) quantity += random.nextInt(span);
            if (rule.itemId() == InventoryCatalog.ADENA) {
                double scaled = quantity * adenaRate;
                quantity = !Double.isFinite(scaled)
                        || scaled >= ClassicWorldDropCatalog.MAXIMUM_QUANTITY
                        ? ClassicWorldDropCatalog.MAXIMUM_QUANTITY
                        : Math.max(0, (int) scaled);
            }
            if (quantity > 0) {
                result.add(new ClassicWorldDropCatalog.RolledItem(item, quantity, roll));
            }
        }
        return List.copyOf(result);
    }

    private static ClassicWorldMonsterCatalog.SpawnDefinition spawn(
            int spawnId, String location, int x, int y, int heading) {
        return new ClassicWorldMonsterCatalog.SpawnDefinition(
                ClassicWorldMapCatalog.WORLD_TREE_MAP_ID, spawnId, location,
                2, 1, x, y, 3, 3, 0, 0, 0, 0, heading,
                60, 120, false, 100, false, 1, BLACK_KNIGHT);
    }

    private static ClassicWorldDropCatalog.DropRule drop(int itemId, int chance) {
        return new ClassicWorldDropCatalog.DropRule(
                BLACK_KNIGHT_SEARCH_PARTY_NPC_ID, itemId, 1, 1, chance);
    }

    private static ClassicWorldDropCatalog.DropRule stoneDrop(
            int itemId, int minimum, int maximum, int chance) {
        return new ClassicWorldDropCatalog.DropRule(
                STONE_GOLEM_NPC_ID, itemId, minimum, maximum, chance);
    }

    private static ClassicWorldDropCatalog.ItemWire itemWire(int mapId, int itemId) {
        ClassicWorldDropCatalog.ItemWire selected =
                ClassicWorldDropCatalog.item(mapId, itemId);
        if (selected != null) return selected;
        return mapId == ClassicWorldMapCatalog.ROYAL_TRIAL_CAVE_SECOND_MAP_ID
                ? MISSING_MAP2_ITEM_WIRES.get(itemId)
                : MISSING_MAP4_ITEM_WIRES.get(itemId);
    }

    private static Map<Integer, ClassicWorldDropCatalog.ItemWire> missingMap4ItemWires() {
        Map<Integer, ClassicWorldDropCatalog.ItemWire> result = new LinkedHashMap<>();
        result.put(93, wire(93, "weapon", "$102", "singlespear", 111, 77,
                25_000, 0, "singlespear", 5, 4, "steel", 6, 11));
        result.put(94, wire(94, "weapon", "$87", "singlespear", 72, 5,
                80_000, 0, "singlespear", 6, 6, "steel", 6, 11));
        result.put(103, wire(103, "weapon", "$77", "spear", 50, 5,
                150_000, 0, "spear", 16, 12, "steel", 6, 3));
        result.put(20085, wire(20085, "armor", "$168", "T", 110, 24,
                5_000, 104, "", 0, 0, "cloth", 4, 127));
        result.put(20154, wire(20154, "armor", "$152", "armor", 73, 3768,
                450_000, 0, "", 0, 0, "iron", 4, 3));
        return Map.copyOf(result);
    }

    private static Map<Integer, ClassicWorldDropCatalog.ItemWire> missingMap2ItemWires() {
        Map<Integer, ClassicWorldDropCatalog.ItemWire> result = new LinkedHashMap<>();
        result.put(137, wire(137, "weapon", "$14", "weapon", 8, 14,
                10_000, 0, "blunt", 4, 4, "steel", 6, 107));
        result.put(138, wire(138, "weapon", "$69", "weapon", 31, 14,
                30_000, 0, "blunt", 6, 3, "steel", 6, 111));
        result.put(139, wire(139, "weapon", "$74", "weapon", 42, 83,
                15_000, 0, "blunt", 6, 4, "steel", 6, 111));
        result.put(40045, stackableWire(40045, "$513", "none", 242, 774,
                40, 0, "gemstone"));
        result.put(40049, stackableWire(40049, "$799 $513", "none", 243, 774,
                40, 0, "gemstone"));
        result.put(40053, stackableWire(40053, "$800 $513", "none", 244, 774,
                40, 0, "gemstone"));
        result.put(40177, stackableWire(40177, "$1586", "book", 249, 776,
                7_000, 0, "paper"));
        result.put(40234, stackableWire(40234, "$1831", "book", 673, 1561,
                7_000, 0, "gemstone"));
        result.put(40494, stackableWire(40494, "$767", "none", 318, 888,
                500, 0, "mithril"));
        return Map.copyOf(result);
    }

    private static ClassicWorldDropCatalog.ItemWire stackableWire(
            int itemId, String nameId, String useToken,
            int inventoryGfx, int groundGfx, int unitWeight,
            int itemDescriptionId, String material) {
        return new ClassicWorldDropCatalog.ItemWire(
                itemId, "etc", nameId, useToken, inventoryGfx, groundGfx,
                RuntimeInventory.BLESSING_NORMAL, true, true, unitWeight,
                itemDescriptionId, "", 0, 0, material, 0, 0);
    }

    private static ClassicWorldDropCatalog.ItemWire wire(
            int itemId, String kind, String nameId, String useToken,
            int inventoryGfx, int groundGfx, int unitWeight,
            int itemDescriptionId, String weaponType, int damageSmall,
            int damageLarge, String material, int safeEnchant, int classMask) {
        return new ClassicWorldDropCatalog.ItemWire(
                itemId, kind, nameId, useToken, inventoryGfx, groundGfx,
                RuntimeInventory.BLESSING_NORMAL, false, true, unitWeight,
                itemDescriptionId, weaponType, damageSmall, damageLarge,
                material, safeEnchant, classMask);
    }
}

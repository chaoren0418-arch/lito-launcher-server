package local.lito381.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Material requirements from the classic {@code skills_item} table. */
final class SpellMaterialCatalog {
    record Requirement(long catalogId, int quantity) {
        Requirement {
            if (catalogId <= 0 || catalogId > 0xFFFF_FFFFL || quantity <= 0) {
                throw new IllegalArgumentException("invalid spell material requirement");
            }
        }
    }

    private static final List<Requirement> CIRCLE_ONE = List.of(
            new Requirement(InventoryCatalog.RYUKEN_WEB, 10),
            new Requirement(InventoryCatalog.PURE_MITHRIL, 50),
            new Requirement(InventoryCatalog.ELVEN_DUST, 100),
            new Requirement(InventoryCatalog.PAN_MANE, 10));
    private static final List<Requirement> CIRCLE_TWO = List.of(
            new Requirement(InventoryCatalog.MITHRIL_THREAD, 10),
            new Requirement(InventoryCatalog.MUSHROOM_JUICE, 8),
            new Requirement(InventoryCatalog.ENT_BARK, 3),
            new Requirement(InventoryCatalog.PAN_HORN, 1));
    private static final List<Requirement> CIRCLE_THREE = List.of(
            new Requirement(InventoryCatalog.ORIHARUKON, 45),
            new Requirement(InventoryCatalog.RYUKEN_SKIN, 3),
            new Requirement(InventoryCatalog.ELVEN_WING, 3),
            new Requirement(InventoryCatalog.PAN_HORN, 3));
    private static final Map<Integer, List<Requirement>> REQUIREMENTS;

    static {
        Map<Integer, List<Requirement>> requirements = new LinkedHashMap<>();
        for (int skillId = 1; skillId <= 8; skillId++) {
            requirements.put(skillId, CIRCLE_ONE);
        }
        for (int skillId = 9; skillId <= 16; skillId++) {
            requirements.put(skillId, CIRCLE_TWO);
        }
        for (int skillId = 17; skillId <= 23; skillId++) {
            requirements.put(skillId, CIRCLE_THREE);
        }
        REQUIREMENTS = Map.copyOf(requirements);
    }

    private SpellMaterialCatalog() {}

    /** Only configured, class/level-eligible and unlearned rows are exposed. */
    static List<Integer> availableWireSkillIds(CharacterSummary character,
                                               Set<Integer> learnedSkillIds) {
        List<Integer> available = new ArrayList<>();
        for (int wireSkillId : SpellShopCatalog.availableWireSkillIds(character, learnedSkillIds)) {
            if (REQUIREMENTS.containsKey(wireSkillId + 1)) available.add(wireSkillId);
        }
        return List.copyOf(available);
    }

    static List<Requirement> requirementsForWireSkillId(int wireSkillId) {
        if (wireSkillId < 0 || wireSkillId >= SpellShopCatalog.MAX_NATIVE_SKILL_ID) {
            throw new IllegalArgumentException("invalid native material-spell skill id");
        }
        List<Requirement> requirements = REQUIREMENTS.get(wireSkillId + 1);
        if (requirements == null) {
            throw new IllegalArgumentException("native skill has no material configuration");
        }
        return requirements;
    }

    static Map<Long, Long> totalRequirements(List<Integer> wireSkillIds) {
        if (wireSkillIds == null || wireSkillIds.isEmpty()) {
            throw new IllegalArgumentException("native material-spell purchase is empty");
        }
        Set<Integer> unique = new LinkedHashSet<>();
        Map<Long, Long> totals = new LinkedHashMap<>();
        for (Integer wireSkillId : wireSkillIds) {
            if (wireSkillId == null || !unique.add(wireSkillId)) {
                throw new IllegalArgumentException("duplicate native material-spell row");
            }
            for (Requirement requirement : requirementsForWireSkillId(wireSkillId)) {
                totals.merge(requirement.catalogId(), (long) requirement.quantity(), Math::addExact);
            }
        }
        return Map.copyOf(totals);
    }

    /** Deterministic isolated-test material bundle for zero-based skill row 1. */
    static boolean seedFixture(RuntimeInventory inventory) {
        if (inventory == null) return false;
        for (Requirement requirement : requirementsForWireSkillId(1)) {
            long available = inventory.catalogCounts().getOrDefault(requirement.catalogId(), 0);
            long missing = requirement.quantity() - available;
            if (missing > 0 && !inventory.add(InventoryCatalog.require(requirement.catalogId()), missing)) {
                return false;
            }
        }
        return true;
    }

    /** Two selected item49157 copies for activation/off/missing-material isolation. */
    static boolean seedAwakenAntharasFixture(RuntimeInventory inventory) {
        if (inventory == null) return false;
        int available = inventory.catalogCounts().getOrDefault(
                InventoryCatalog.AWAKENING_BONE_FRAGMENT, 0);
        return available >= 2 || inventory.add(InventoryCatalog.require(
                InventoryCatalog.AWAKENING_BONE_FRAGMENT), 2L - available);
    }

    /** Four item49157 copies for Valakas activation/off/missing-material isolation. */
    static boolean seedAwakenValakasFixture(RuntimeInventory inventory) {
        if (inventory == null) return false;
        int available = inventory.catalogCounts().getOrDefault(
                InventoryCatalog.AWAKENING_BONE_FRAGMENT, 0);
        return available >= 4 || inventory.add(InventoryCatalog.require(
                InventoryCatalog.AWAKENING_BONE_FRAGMENT), 4L - available);
    }
}

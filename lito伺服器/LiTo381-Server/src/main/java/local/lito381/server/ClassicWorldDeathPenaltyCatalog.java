package local.lito381.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

/** Exact selected 3.81 death-item, death-skill and protection-item policy. */
final class ClassicWorldDeathPenaltyCatalog {
    static final String SOURCE_SHA256 =
            "DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766";
    static final int MAP_COUNT = 731;
    static final int DISABLED_MAP_COUNT = 72;
    static final int EXPERIENCE_CATEGORY = 1;
    static final int ITEM_CATEGORY = 2;
    static final int SKILL_CATEGORY = 4;
    static final int ALL_CATEGORIES = 7;

    enum Control {
        NONE(0), ALWAYS_DROP(1), NEVER_DROP(2), ALWAYS_VANISH(3),
        VANISH_IF_DROPPED(4);

        private final int code;
        Control(int code) { this.code = code; }
        int code() { return code; }
    }

    record Rule(int maximumLawful, int minimumLawful,
                int chancePercent, int maximumLoss) {
        Rule {
            if (minimumLawful < -32_768 || maximumLawful > 32_767
                    || minimumLawful > maximumLawful
                    || chancePercent < 0 || chancePercent > 100
                    || maximumLoss < 0) {
                throw new IllegalArgumentException("invalid native death-penalty rule");
            }
        }

        boolean contains(int lawful) {
            return lawful >= minimumLawful && lawful <= maximumLawful;
        }
    }

    record DeathSafe(long itemId, int categoryMask, boolean consume,
                     int timedHours) {
        DeathSafe {
            if (itemId <= 0 || (categoryMask & ~ALL_CATEGORIES) != 0
                    || categoryMask == 0 || timedHours < 0) {
                throw new IllegalArgumentException("invalid native DeathSafe item");
            }
        }

        boolean protects(int category) {
            return (categoryMask & category) != 0;
        }
    }

    private static final List<Rule> ITEM_RULES = List.of(
            new Rule(32_767, 20_000, 0, 0),
            new Rule(19_999, 10_000, 0, 0),
            new Rule(9_999, 0, 5, 1),
            new Rule(-1, -10_000, 10, 2),
            new Rule(-10_001, -20_000, 25, 3),
            new Rule(-20_001, -32_768, 50, 3));
    private static final List<Rule> SKILL_RULES = List.of(
            new Rule(32_767, 20_000, 0, 0),
            new Rule(19_999, 10_000, 0, 0),
            new Rule(9_999, 0, 0, 0),
            new Rule(-1, -10_000, 5, 1),
            new Rule(-10_001, -20_000, 25, 3),
            new Rule(-20_001, -32_768, 99, 3));

    private static final Set<Integer> DISABLED_MAPS = disabledMaps();
    private static final Map<Long, Control> CONTROLS = buildControls();
    private static final Map<Long, DeathSafe> DEATH_SAFE = Map.of(
            InventoryCatalog.DEATH_SAFE_EXPERIENCE,
            new DeathSafe(InventoryCatalog.DEATH_SAFE_EXPERIENCE,
                    EXPERIENCE_CATEGORY, true, 0),
            InventoryCatalog.DEATH_SAFE_ITEM,
            new DeathSafe(InventoryCatalog.DEATH_SAFE_ITEM,
                    ITEM_CATEGORY, true, 0),
            InventoryCatalog.DEATH_SAFE_SKILL,
            new DeathSafe(InventoryCatalog.DEATH_SAFE_SKILL,
                    SKILL_CATEGORY, true, 0),
            InventoryCatalog.DEATH_SAFE_ALL,
            new DeathSafe(InventoryCatalog.DEATH_SAFE_ALL,
                    ALL_CATEGORIES, false, 168));

    private ClassicWorldDeathPenaltyCatalog() {}

    static Rule itemRule(int lawful) { return rule(ITEM_RULES, lawful); }
    static Rule skillRule(int lawful) { return rule(SKILL_RULES, lawful); }
    static List<Rule> itemRules() { return ITEM_RULES; }
    static List<Rule> skillRules() { return SKILL_RULES; }
    static Map<Long, Control> controls() { return CONTROLS; }
    static Control control(long itemId) {
        return CONTROLS.getOrDefault(itemId, Control.NONE);
    }
    static Map<Long, DeathSafe> deathSafeItems() { return DEATH_SAFE; }
    static DeathSafe deathSafe(long itemId) { return DEATH_SAFE.get(itemId); }
    static boolean allowsSelectedMap(int mapId, boolean selectedRowPresent) {
        return selectedRowPresent && mapId >= 0 && !DISABLED_MAPS.contains(mapId);
    }
    static boolean mapDisabled(int mapId) { return DISABLED_MAPS.contains(mapId); }

    private static Rule rule(List<Rule> rules, int lawful) {
        if (lawful < -32_768 || lawful > 32_767) {
            throw new IllegalArgumentException("lawful is outside native range");
        }
        return rules.stream().filter(candidate -> candidate.contains(lawful))
                .findFirst().orElseThrow();
    }

    private static Map<Long, Control> buildControls() {
        LinkedHashMap<Long, Control> result = new LinkedHashMap<>();
        for (long itemId : new long[] {
                40308, 44070, 141245, 300001, 300014, 300086,
                300178, 340010, 340018, 340733, 349552 }) {
            result.put(itemId, Control.NEVER_DROP);
        }
        addRange(result, 300094, 300103, Control.NEVER_DROP);
        addRange(result, 300114, 300123, Control.NEVER_DROP);
        addRange(result, 300134, 300143, Control.NEVER_DROP);
        addRange(result, 300087, 300093, Control.VANISH_IF_DROPPED);
        return Map.copyOf(result);
    }

    private static Set<Integer> disabledMaps() {
        LinkedHashSet<Integer> result = new LinkedHashSet<>(List.of(
                303, 997, 998, 4301, 4831, 5143, 5153, 6051, 6801, 6901));
        for (int mapId = 4302; mapId <= 4363; mapId++) result.add(mapId);
        if (result.size() != DISABLED_MAP_COUNT) {
            throw new IllegalStateException("selected mapids death-penalty count drift");
        }
        return Set.copyOf(result);
    }

    private static void addRange(Map<Long, Control> target,
                                 long first, long last, Control control) {
        for (long itemId = first; itemId <= last; itemId++) {
            target.put(itemId, control);
        }
    }
}

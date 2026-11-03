package local.lito381.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Exact retained {@code mapids_恢復區域} runtime projection. */
final class ClassicWorldMapRecoveryCatalog {
    static final int DEFAULT_INTERVAL_SECONDS = 64;

    record Rule(int id, int mapId, int minX, int minY, int maxX, int maxY,
                int hp, int mp, int configuredIntervalSeconds,
                boolean disableNaturalMp, String note) {
        Rule {
            if (id <= 0 || mapId < -1 || configuredIntervalSeconds < 0
                    || note == null || note.isBlank()) {
                throw new IllegalArgumentException("invalid native map-recovery row");
            }
            boolean wholeMap = minX == 0 && minY == 0 && maxX == 0 && maxY == 0;
            if (!wholeMap && (minX <= 0 || minY <= 0
                    || maxX < minX || maxY < minY)) {
                throw new IllegalArgumentException("invalid native map-recovery zone");
            }
        }

        int intervalSeconds() {
            return configuredIntervalSeconds <= 0
                    ? DEFAULT_INTERVAL_SECONDS : configuredIntervalSeconds;
        }

        boolean wholeMap() {
            return minX == 0 && minY == 0 && maxX == 0 && maxY == 0;
        }

        boolean contains(int candidateMapId, int x, int y) {
            if (candidateMapId != mapId) return false;
            return wholeMap() || (x >= minX && x <= maxX
                    && y >= minY && y <= maxY);
        }
    }

    private static final List<Integer> INNER_CASTLE_MAP_IDS =
            List.of(15, 29, 52, 64, 300);

    private static final List<Rule> RULES = List.of(
            whole(1, -1, 15, 10, 16, false, "全伺服器盟屋/內城通用"),
            whole(2, 410, -10, 0, 0, false, "魔族神殿"),
            whole(3, 306, -10, 0, 0, false, "黑暗妖精試煉地監"),
            whole(4, 2000, -10, 0, 0, true, "再生聖殿"),
            new Rule(5, 4, 33045, 32325, 33067, 32350,
                    10, 3, 16, false, "森林之母(妖精森林)"),
            whole(6, 63, -20, 0, 0, false, "海音地監 4樓"),
            whole(7, 65, -20, 0, 0, false, "法利昂棲息地"),
            whole(8, 420, -20, 0, 0, false, "地底湖"),
            whole(9, 558, -20, 0, 0, false, "船舶之墓 深海"),
            whole(10, 604, -20, 0, 0, false, "水之領域"),
            whole(11, 779, -20, 0, 0, false, "原生魔族拋棄之地海底"),
            whole(138, 16384, 15, 10, 16, false, "旅館房間"),
            whole(139, 16896, 15, 10, 16, false, "旅館會議廳"),
            whole(140, 17408, 15, 10, 16, false, "旅館房間"),
            whole(141, 17920, 15, 10, 16, false, "旅館會議廳"),
            whole(142, 18432, 15, 10, 16, false, "旅館房間"),
            whole(143, 18944, 15, 10, 16, false, "旅館會議廳"),
            whole(144, 19456, 15, 10, 16, false, "旅館房間"),
            whole(145, 19968, 15, 10, 16, false, "旅館會議廳"),
            whole(146, 20480, 15, 10, 16, false, "旅館房間"),
            whole(147, 20992, 15, 10, 16, false, "旅館會議廳"),
            whole(148, 21504, 15, 10, 16, false, "旅館房間"),
            whole(149, 22016, 15, 10, 16, false, "旅館會議廳"),
            whole(150, 22528, 15, 10, 16, false, "旅館房間"),
            whole(151, 23040, 15, 10, 16, false, "旅館會議廳"),
            whole(152, 23552, 15, 10, 16, false, "旅館房間"),
            whole(153, 24064, 15, 10, 16, false, "旅館會議廳"),
            whole(154, 24576, 15, 10, 16, false, "旅館房間"),
            whole(155, 25088, 15, 10, 16, false, "旅館會議廳"));

    private static final Map<Integer, Rule> WHOLE_MAP_RULES = wholeMapRules();

    private ClassicWorldMapRecoveryCatalog() {}

    static Rule ruleFor(int mapId, int x, int y) {
        for (Rule rule : RULES) {
            if (!rule.wholeMap() && rule.contains(mapId, x, y)) return rule;
        }
        Rule direct = WHOLE_MAP_RULES.get(mapId);
        if (direct != null) return direct;
        return INNER_CASTLE_MAP_IDS.contains(mapId)
                ? WHOLE_MAP_RULES.get(-1) : null;
    }

    static boolean disablesNaturalMp(int mapId, int x, int y) {
        Rule rule = ruleFor(mapId, x, y);
        return rule != null && rule.disableNaturalMp();
    }

    static List<Rule> rules() {
        return RULES;
    }

    private static Rule whole(int id, int mapId, int hp, int mp,
                              int intervalSeconds, boolean disableNaturalMp,
                              String note) {
        return new Rule(id, mapId, 0, 0, 0, 0, hp, mp,
                intervalSeconds, disableNaturalMp, note);
    }

    private static Map<Integer, Rule> wholeMapRules() {
        Map<Integer, Rule> result = new LinkedHashMap<>();
        for (Rule rule : RULES) {
            if (rule.wholeMap() && result.put(rule.mapId(), rule) != null) {
                throw new IllegalStateException(
                        "duplicate native map-recovery map " + rule.mapId());
            }
        }
        return Map.copyOf(result);
    }
}

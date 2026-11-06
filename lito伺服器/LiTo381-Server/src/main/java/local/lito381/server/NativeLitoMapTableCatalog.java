package local.lito381.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Exact active rows from the selected client's Big5 LTtext.pak/LTmapstr-c.tbl.
 *
 * <p>This is the native LiTo map namespace. It must not be merged with the
 * retained Lineage root-map database: identical numeric ids can describe
 * unrelated terrain, and exact LiTo.bin opens only LiTo/LTMap sectors.</p>
 */
final class NativeLitoMapTableCatalog {
    record Entry(int slot, int mapId, String category, String name) {}

    private static final List<Entry> ACTIVE = List.of(
            entry(0, 500, "town", "town"),
            entry(1, 560, "town", "town2"),
            entry(4, 565, "petRace", "競速城市"),
            entry(5, 566, "petRace", "安逸之鎮"),
            entry(7, 569, "petRace", "競速城市(R)"),
            entry(8, 570, "petRace", "安逸之鎮(R)"),
            entry(9, 564, "deathmatch", "死戰之地"),
            entry(10, 514, "deathmatch", "聖殿"),
            entry(11, 509, "deathmatch", "死亡之地"),
            entry(12, 508, "deathmatch", "死亡陷阱"),
            entry(13, 551, "deathmatch", "雷諾雅碼頭"),
            entry(14, 504, "deathmatch", "歐洲花園"),
            entry(15, 505, "deathmatch", "命運十字路"),
            entry(16, 552, "deathmatch", "天國之路"),
            entry(17, 506, "deathmatch", "妖魔祭壇"),
            entry(18, 526, "deathmatch", "方形廣場"),
            entry(19, 525, "deathmatch", "試驗之門"),
            entry(20, 516, "deathmatch", "詭譎之房"),
            entry(21, 531, "deathmatch", "死亡地獄"),
            entry(22, 534, "deathmatch", "羅登城市"),
            entry(23, 529, "deathmatch", "福爾摩莎之死鬥"),
            entry(31, 510, "honormatch", "榮耀競技場"),
            entry(32, 528, "honormatch", "榮譽之門"),
            entry(33, 532, "honormatch", "邪惡神殿"),
            entry(34, 512, "honormatch", "水之帷幕"),
            entry(35, 517, "honormatch", "神之火"),
            entry(36, 524, "honormatch", "聖殿"),
            entry(37, 530, "honormatch", "福爾摩莎之榮耀"),
            entry(38, 535, "honormatch", "羅登城市"),
            entry(41, 554, "questbattle", "惡魔之墓"),
            entry(42, 553, "questbattle", "深淵"),
            entry(43, 513, "questbattle", "地獄之門"),
            entry(44, 527, "questbattle", "顫慄任務"),
            entry(45, 518, "questbattle", "挑戰競技場(Lv1)"),
            entry(46, 519, "questbattle", "挑戰競技場(Lv2)"),
            entry(47, 520, "questbattle", "挑戰競技場(Lv3)"),
            entry(48, 521, "questbattle", "挑戰競技場(Lv4)"),
            entry(49, 522, "questbattle", "挑戰競技場(Lv5)"),
            entry(53, 540, "timeattack", "地獄廣場(R)"),
            entry(57, 573, "siegewar", "台灣特色地圖"),
            entry(58, 574, "siegewar", "台灣特色地圖"),
            entry(59, 562, "siegewar", "亞丁城堡"),
            entry(60, 563, "siegewar", "亞丁城堡"),
            entry(61, 549, "siegewar", "強襲號角(Red)"),
            entry(62, 550, "siegewar", "強襲號角(Blue)"),
            entry(63, 547, "siegewar", "雨鳥(R)"),
            entry(64, 548, "siegewar", "雨鳥(R)"));

    /**
     * Terrain directories present in LiTo/LTmap but absent from every active
     * LTmapstr row.  A directory is a renderer input, not a selectable scene:
     * these ids must stay fail-closed until an independent native producer or
     * transition proves reachability.
     */
    private static final Set<Integer> RESOURCE_ONLY = Set.of(
            501, 502, 503, 507,
            541, 542, 543, 545, 546, 555, 567, 568,
            16_384, 16_896, 17_408);

    private static final Map<Integer, Entry> BY_MAP = index();

    private NativeLitoMapTableCatalog() {}

    static List<Entry> active() {
        return ResourceDatabase.maps(ACTIVE);
    }

    static Entry forMap(int mapId) {
        return ResourceDatabase.map(BY_MAP.get(mapId), mapId);
    }

    static boolean isActive(int mapId) {
        return ResourceDatabase.mapActive(BY_MAP.containsKey(mapId), mapId);
    }

    static Set<Integer> resourceOnlyMapIds() {
        return RESOURCE_ONLY;
    }

    static boolean hasResourceOnlyTerrain(int mapId) {
        return RESOURCE_ONLY.contains(mapId);
    }

    static boolean isTown(int mapId) {
        Entry entry = forMap(mapId);
        return entry != null && entry.category().equals("town");
    }

    private static Entry entry(int slot, int mapId, String category, String name) {
        return new Entry(slot, mapId, category, name);
    }

    private static Map<Integer, Entry> index() {
        LinkedHashMap<Integer, Entry> result = new LinkedHashMap<>();
        for (Entry entry : ACTIVE) {
            if (result.put(entry.mapId(), entry) != null) {
                throw new IllegalStateException("duplicate native LiTo map " + entry.mapId());
            }
        }
        return Map.copyOf(result);
    }
}

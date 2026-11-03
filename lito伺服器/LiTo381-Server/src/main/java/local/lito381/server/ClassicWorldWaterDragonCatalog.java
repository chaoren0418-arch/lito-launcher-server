package local.lito381.server;

import java.util.List;
import java.util.Map;

/** Hash-pinned retained contract for quest117, the Fafurion map1011 instance. */
final class ClassicWorldWaterDragonCatalog {
    static final int QUEST_ID = 117;
    static final int MAP_ID = 1011;
    static final int GATE_MAP_ID = 613;
    static final int GATE_NPC_ID = 70937;
    static final int SAELL_NPC_ID = 70689;
    static final int PASS_ITEM_ID = 82504;
    static final int ADENA_ITEM_ID = 40308;
    static final int PASS_PRICE = 300;
    static final int PASS_RENTAL_COUNT = 32;
    static final int PASS_RENTAL_SECONDS = 7_200;
    static final int ROOM_COUNT = 16;
    static final int MAXIMUM_USERS = 8;
    static final int INSTANCE_SECONDS = 1_800;
    static final int MINIMUM_ENTRY_LEVEL = 60;
    static final int QUEST_METADATA_LEVEL = 80;
    static final int QUEST_CLASS_MASK = 127;
    static final int WATER_DRAGON_BLOOD_SKILL_ID = 6798;
    static final int REENTRY_PENALTY_SECONDS = 43_200;
    static final int COMPLETION_BLOOD_SECONDS = 86_400;
    static final int COMPLETION_GFX = 7854;

    static final int WORLD_BLESSING_SKILL_ID = 4010;
    static final int WORLD_BLESSING_SECONDS = 2_400;
    static final int WORLD_BLESSING_GFX = 7680;

    static final String QUEST_SOURCE_SHA256 =
            "918E8B8E4C328582D3922B3011E255E31AA8C2EB101E769547F12AC16F2752B1";
    static final String ENTRY_SOURCE_SHA256 =
            "245E2C35FE3F74F666B0E410E59F7591C1A19E8A4FCFE7C53B0DE10C6E5C6165";
    static final String BOSS_GATE_SOURCE_SHA256 =
            "82B454171137ACEF6BAB5B0352811E5B012D0710D369112F5DF411D7D9EACE30";
    static final String BOSS_CHAIN_SOURCE_SHA256 =
            "8C120140A38CE82E4820B3B9FBB5D670F907E4E4608201DAD1059386B5B8D72B";
    static final String SAELL_SOURCE_SHA256 =
            "4AD99E07F0DF2FE5204B3A38E1EB7E4AB2198BEBA2E6711266BF274557BC3063";
    static final String SAELL_SKILL_SOURCE_SHA256 =
            "89BCD86ED1C73B2D23AEF8FF12B0434EC543F5A5301E61D9B742E1001D0450B1";
    static final String RETAINED_DATABASE_SHA256 =
            "3BF9B04B6E1D788E361534A79F3AA49CEEEF84FAE5CE04471EDF1EC9764CA423";

    static final Location ENTRY = new Location(MAP_ID, 32957, 32743, 1);
    static final Location BOSS_GATE_DESTINATION =
            new Location(MAP_ID, 32990, 32842, 4);
    static final RandomLocation FIRST_BOSS_SPAWN =
            new RandomLocation(MAP_ID, 32958, 32835, 5);
    static final RandomRectangle LATER_BOSS_SPAWN =
            new RandomRectangle(MAP_ID, 32948, 32967, 32825, 32844);
    static final Location COMPLETION_EXIT = new Location(4, 33718, 32506, 0);
    static final Location RESTART_EXIT = new Location(4, 33429, 32800, 0);

    record Location(int mapId, int x, int y, int heading) {
        Location {
            if (mapId < 0 || x <= 0 || y <= 0 || heading < 0 || heading > 7) {
                throw new IllegalArgumentException("invalid Water Dragon location");
            }
        }
    }

    record RandomLocation(int mapId, int centerX, int centerY, int radius) {}

    record RandomRectangle(int mapId, int minX, int maxX, int minY, int maxY) {}

    record SpawnRow(int spawnId, int questId, boolean staticSpawn, String name,
                    int count, int npcId, int minX, int maxX, int minY, int maxY,
                    int heading, int respawnSeconds, int mapId,
                    int movementDistance, boolean avoidPlayers) {
        SpawnRow {
            if (spawnId <= 0 || questId != QUEST_ID || name == null || name.isBlank()
                    || count <= 0 || npcId <= 0 || minX <= 0 || maxX < minX
                    || minY <= 0 || maxY < minY || heading < 0 || heading > 7
                    || respawnSeconds < 0 || mapId < 0 || movementDistance < 0) {
                throw new IllegalArgumentException("invalid Water Dragon spawn row");
            }
        }

        boolean inInstance() { return mapId == MAP_ID; }
    }

    record Door(int doorId, int gfxId, int x, int y, int heading,
                int guardianNpcId) {
        Door {
            if (doorId < 10008 || doorId > 10010 || gfxId <= 0 || x <= 0 || y <= 0
                    || heading < 0 || heading > 7 || guardianNpcId <= 0) {
                throw new IllegalArgumentException("invalid Water Dragon door");
            }
        }

        long objectId(int showId) {
            if (showId <= 0) {
                throw new IllegalArgumentException("invalid Water Dragon showId");
            }
            return 0x6D00_0000L | ((long) (showId & 0xFFFF) << 8)
                    | (doorId - 10000L);
        }

        int entranceX() { return heading == 0 ? x : x - 1; }

        int entranceY() { return heading == 0 ? y + 1 : y; }

        int edgeLow() { return (heading == 0 ? x : y) - 2; }

        int edgeHigh() { return (heading == 0 ? x : y) + 3; }

        boolean blocks(int targetX, int targetY) {
            return heading == 0
                    ? targetY == entranceY()
                        && targetX >= edgeLow() && targetX <= edgeHigh()
                    : targetX == entranceX()
                        && targetY >= edgeLow() && targetY <= edgeHigh();
        }
    }

    record BossPhase(int npcId, int gfxId, int level, int maximumHp,
                     int maximumMp, int nextNpcId) {}

    record TimedMessage(int offsetSeconds, int messageId) {}

    record Blessing(int skillId, int durationSeconds, int gfxId,
                    int hpr, int mpr, int maximumHp, int maximumMp,
                    int bowHit, int bowDamage, int waterResistance, int ac) {}

    private static final List<SpawnRow> SPAWNS = List.of(
            spawn(50203, true, "藍色 龍之門扉-法利昂(奇怪的村落)", 1,
                    GATE_NPC_ID, 32809, 32896, 7, 0, GATE_MAP_ID, 0, false),
            spawn(50206, true, "莎爾-法利昂洞穴", 1,
                    SAELL_NPC_ID, 32859, 32664, 4, 0, MAP_ID, 0, false),
            spawn(50207, false, "阿爾波斯 麥嘉-法利昂洞穴", 40,
                    70974, 32721, 32657, 32876, 32753, 5, 0, MAP_ID, 30, false),
            spawn(50208, false, "阿爾波斯 菲斯-法利昂洞穴", 40,
                    70975, 32721, 32657, 32876, 32753, 5, 0, MAP_ID, 30, false),
            spawn(50209, false, "阿爾波斯 耐爾(A)-法利昂洞穴", 1,
                    70976, 32739, 32716, 4, 30, MAP_ID, 30, false),
            spawn(50210, false, "阿爾波斯 耐爾(B)-法利昂洞穴", 1,
                    70977, 32776, 32682, 6, 30, MAP_ID, 30, false),
            spawn(50211, false, "阿爾波斯 耐爾(C)-法利昂洞穴", 1,
                    70978, 32861, 32712, 4, 30, MAP_ID, 30, false));

    private static final List<Door> DOORS = List.of(
            new Door(10008, 7858, 32741, 32712, 0, 70976),
            new Door(10009, 7859, 32779, 32681, 1, 70977),
            new Door(10010, 7858, 32861, 32709, 0, 70978));

    private static final Map<Integer, BossPhase> BOSS_PHASES = Map.of(
            71026, new BossPhase(71026, 7864, 85, 40_000, 5_000, 71027),
            71027, new BossPhase(71027, 7869, 95, 40_000, 5_000, 71028),
            71028, new BossPhase(71028, 7870, 105, 40_000, 5_000, -1));

    private static final List<TimedMessage> START_MESSAGES = List.of(
            new TimedMessage(30, 1657), new TimedMessage(32, 1658),
            new TimedMessage(34, 1659), new TimedMessage(36, 1660));
    private static final Map<Integer, List<TimedMessage>> DEATH_MESSAGES = Map.of(
            71026, List.of(new TimedMessage(5, 1661), new TimedMessage(10, 1662),
                    new TimedMessage(20, 1663), new TimedMessage(30, 1664)),
            71027, List.of(new TimedMessage(5, 1665), new TimedMessage(10, 1666),
                    new TimedMessage(20, 1667)),
            71028, List.of(new TimedMessage(5, 1668), new TimedMessage(10, 1669)));
    private static final List<TimedMessage> COMPLETION_MESSAGES = List.of(
            new TimedMessage(30, 1476), new TimedMessage(40, 1477),
            new TimedMessage(50, 1478), new TimedMessage(55, 1480),
            new TimedMessage(56, 1481), new TimedMessage(57, 1482),
            new TimedMessage(58, 1483), new TimedMessage(59, 1484));

    static final Blessing SAELL_BLESSING = new Blessing(
            WORLD_BLESSING_SKILL_ID, WORLD_BLESSING_SECONDS, WORLD_BLESSING_GFX,
            8, 1, 80, 10, 6, 3, 30, -8);

    private ClassicWorldWaterDragonCatalog() {}

    static List<SpawnRow> retainedSpawnRows() { return SPAWNS; }

    static List<SpawnRow> instanceSpawnRows() {
        return SPAWNS.stream().filter(SpawnRow::inInstance).toList();
    }

    static List<Door> doors() { return DOORS; }

    static Door doorForGuardian(int npcId) {
        return DOORS.stream().filter(door -> door.guardianNpcId() == npcId)
                .findFirst().orElse(null);
    }

    static BossPhase bossPhase(int npcId) { return BOSS_PHASES.get(npcId); }

    static List<TimedMessage> startMessages() { return START_MESSAGES; }

    static List<TimedMessage> deathMessages(int npcId) {
        return DEATH_MESSAGES.getOrDefault(npcId, List.of());
    }

    static List<TimedMessage> completionMessages() { return COMPLETION_MESSAGES; }

    static int deathResolutionSeconds(int npcId) {
        return switch (npcId) {
            case 71026 -> 40;
            case 71027 -> 30;
            case 71028 -> 20;
            default -> -1;
        };
    }

    private static SpawnRow spawn(int spawnId, boolean staticSpawn, String name,
                                  int count, int npcId, int x, int y, int heading,
                                  int respawnSeconds, int mapId,
                                  int movementDistance, boolean avoidPlayers) {
        return new SpawnRow(spawnId, QUEST_ID, staticSpawn, name, count, npcId,
                x, x, y, y, heading, respawnSeconds, mapId,
                movementDistance, avoidPlayers);
    }

    private static SpawnRow spawn(int spawnId, boolean staticSpawn, String name,
                                  int count, int npcId, int minX, int minY,
                                  int maxX, int maxY, int heading,
                                  int respawnSeconds, int mapId,
                                  int movementDistance, boolean avoidPlayers) {
        return new SpawnRow(spawnId, QUEST_ID, staticSpawn, name, count, npcId,
                minX, maxX, minY, maxY, heading, respawnSeconds, mapId,
                movementDistance, avoidPlayers);
    }
}

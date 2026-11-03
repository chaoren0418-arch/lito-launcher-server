package local.lito381.server;

import java.util.List;
import java.util.Map;

/** Exact selected-source {@code L1GuardianInstance} contracts for map 4. */
final class ClassicWorldGuardianCatalog {
    static final int MAP_ID = 4;
    static final int POPULATION = 54;
    static final int ARACHNE = 70846;
    static final int ENT = 70848;
    static final int PAN = 70850;
    static final int FAIRY = 70851;
    static final int FAIRY_QUEEN = 70852;
    static final int INTRUDER_MESSAGE = 804;
    static final int ELF_MURDERER_MESSAGE = 815;
    static final int HARVEST_MESSAGE = 143;
    static final int TALK_REST_SECONDS = 10;
    static final long WANTED_SECONDS = 24L * 60L * 60L;

    static final String SELECTED_DATABASE_SHA256 =
            "DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766";
    static final String RETAINED_GUARDIAN_SOURCE_SHA256 =
            "C87137DEB9E74AC949E7FCD45CE07E8B5A9903C83E1CD8C04092D01DB666D9AE";
    static final String SECONDARY_GUARDIAN_SOURCE_SHA256 =
            "4446F5A3C66B6BBAF19F7CA75CD1F88B64C9717CFF85C6DCB207456302B6B0B0";
    static final String RETAINED_NPC_CHAT_SOURCE_SHA256 =
            "F24851772A3111921F9DE1A3301B47DA5F7503066EA4293310E2567B12B3C12E";
    static final String RETAINED_NPC_SHOUT_SOURCE_SHA256 =
            "57517A252C5FE3CF12906C11BD4C648F56FD5994EF15CC55F4677560242140F4";
    static final String RETAINED_CHANGE_HEADING_SOURCE_SHA256 =
            "DC8EEAE09BEC67D5BB0DAAD7E658A87390718F8E0A3362480D633E80F749E015";

    enum TargetDisposition {
        NONE,
        INTRUDER,
        ELF_MURDERER
    }

    record Profile(String size, int ranged, String normalHtml,
                   String chaoticHtml, int expectedSpawns) {
        Profile {
            if (size == null || normalHtml == null || chaoticHtml == null
                    || ranged < 1 || expectedSpawns < 1) {
                throw new IllegalArgumentException("invalid guardian profile");
            }
        }
    }

    record Harvest(int itemId, int quantity, String giverNameId,
                   String itemMessageArgument) {
        Harvest {
            if (itemId <= 0 || quantity <= 0 || giverNameId == null
                    || giverNameId.isBlank() || itemMessageArgument == null
                    || itemMessageArgument.isBlank()) {
                throw new IllegalArgumentException("invalid guardian harvest");
            }
        }
    }

    private static final Map<Integer, Profile> PROFILES = Map.of(
            ARACHNE, new Profile("small", 2, "arachnee1", "arachnem1", 10),
            ENT, new Profile("large", 2, "ente1", "entm1", 19),
            PAN, new Profile("large", 2, "pane1", "panm1", 10),
            FAIRY, new Profile("small", 1, "fairye1", "fairym1", 14),
            FAIRY_QUEEN, new Profile("small", 1, "fairyqe1", "fairyqm1", 1));

    private static final List<ClassicWorldNpcCatalog.StaticNpc> NPCS =
            ClassicWorldNpcCatalog.all(MAP_ID).stream()
                    .filter(ClassicWorldGuardianCatalog::isGuardian)
                    .toList();
    private static final List<ClassicWorldMonsterCatalog.SpawnDefinition> DEFINITIONS =
            NPCS.stream().map(ClassicWorldGuardianCatalog::definition).toList();

    static {
        if (NPCS.size() != POPULATION) {
            throw new ExceptionInInitializerError(
                    "selected guardian population changed actual=" + NPCS.size());
        }
        for (Map.Entry<Integer, Profile> entry : PROFILES.entrySet()) {
            long actual = NPCS.stream()
                    .filter(npc -> npc.npcId() == entry.getKey()).count();
            if (actual != entry.getValue().expectedSpawns()) {
                throw new ExceptionInInitializerError(
                        "selected guardian spawn count changed npc=" + entry.getKey()
                                + " actual=" + actual + " expected="
                                + entry.getValue().expectedSpawns());
            }
        }
    }

    private ClassicWorldGuardianCatalog() {}

    static List<ClassicWorldNpcCatalog.StaticNpc> all() {
        return NPCS;
    }

    static List<ClassicWorldMonsterCatalog.SpawnDefinition> definitions() {
        return DEFINITIONS;
    }

    static boolean isGuardian(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && npc.mapId() == MAP_ID
                && npc.implementation().equals("L1Guardian")
                && PROFILES.containsKey(npc.npcId());
    }

    static boolean isGuardian(ClassicWorldMonsterPopulation.Snapshot actor) {
        return actor != null && actor.mapId() == MAP_ID
                && actor.template().implementation().equals("L1Guardian")
                && PROFILES.containsKey(actor.npcId());
    }

    static boolean isGuardianNpcId(int npcId) {
        return PROFILES.containsKey(npcId);
    }

    static String dialogueHtml(int npcId, long lawful) {
        Profile profile = requireProfile(npcId);
        return lawful < -1000 ? profile.chaoticHtml() : profile.normalHtml();
    }

    static TargetDisposition disposition(boolean elf, boolean wantedForElf) {
        if (!elf) return TargetDisposition.INTRUDER;
        return wantedForElf ? TargetDisposition.ELF_MURDERER
                : TargetDisposition.NONE;
    }

    /** One-based roll from retained {@code Random.nextInt(100) + 1}. */
    static Harvest harvest(int npcId, int oneBasedRoll) {
        if (oneBasedRoll < 1 || oneBasedRoll > 100) {
            throw new IllegalArgumentException("guardian harvest roll outside 1..100");
        }
        return switch (npcId) {
            case ARACHNE -> oneBasedRoll <= 30
                    ? new Harvest((int) InventoryCatalog.RYUKEN_WEB, 1,
                            "$752", "$769") : null;
            case ENT -> oneBasedRoll <= 10
                    ? new Harvest((int) InventoryCatalog.ENT_FRUIT, 1,
                            "$755", "$794")
                    : oneBasedRoll <= 60
                    ? new Harvest((int) InventoryCatalog.ENT_BRANCH, 1,
                            "$755", "$763")
                    : oneBasedRoll <= 70
                    ? new Harvest((int) InventoryCatalog.ENT_BARK, 1,
                            "$755", "$770") : null;
            case PAN -> oneBasedRoll <= 30
                    ? new Harvest((int) InventoryCatalog.PAN_MANE, 5,
                            "$753", "$760 (5)") : null;
            case FAIRY, FAIRY_QUEEN -> null;
            default -> throw new IllegalArgumentException(
                    "not a selected guardian npc " + npcId);
        };
    }

    private static Profile requireProfile(int npcId) {
        Profile profile = PROFILES.get(npcId);
        if (profile == null) {
            throw new IllegalArgumentException("not a selected guardian npc " + npcId);
        }
        return profile;
    }

    private static ClassicWorldMonsterCatalog.SpawnDefinition definition(
            ClassicWorldNpcCatalog.StaticNpc npc) {
        Profile profile = requireProfile(npc.npcId());
        ClassicWorldMonsterCatalog.MonsterTemplate template =
                new ClassicWorldMonsterCatalog.MonsterTemplate(
                        npc.npcId(), npc.nameId(), "L1Guardian", npc.gfxId(),
                        npc.level(), npc.hp(), npc.mp(), 0,
                        30, 30, 30, 30, 30, 70, npc.experience(), npc.lawful(),
                        profile.size(), 0, profile.ranged(),
                        npc.passispeed(), npc.atkspeed(), 0, 0,
                        0, 0, true, 0, false, false, false,
                        npc.lightSize(), npc.changeHead());
        return new ClassicWorldMonsterCatalog.SpawnDefinition(
                npc.mapId(), npc.spawnId(), "selected-L1Guardian", 1, 0,
                npc.x(), npc.y(), 0, 0, 0, 0, 0, 0, npc.heading(),
                0, 0, false, npc.movementDistance(), false, 0, template);
    }
}

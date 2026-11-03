package local.lito381.server;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Hash-locked native Dragon Knight / Illusionist home-town component. */
final class ClassicWorldBehemothSilveriaCatalog {
    static final int SILVERIA_MAP_ID = 1000;
    static final int BEHEMOTH_MAP_ID = 1001;
    static final List<Integer> MAP_IDS = List.of(SILVERIA_MAP_ID, BEHEMOTH_MAP_ID);

    static final String FLAGS_SOURCE_SHA256 =
            "8AE50C9A73D76810B1CB2D582A0544E8FF1A6D70A72AAEC6D25342B90981B65A";
    static final String EMPTY_MONSTER_SOURCE_SHA256 =
            "219255B3830F63B32C0D35542B91DF47B5532113719CB79075F5CD233B6DE5E7";
    static final String EMPTY_DROP_SOURCE_SHA256 =
            "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828";

    record MapFlags(int mapId, String locationName,
                    int startX, int endX, int startY, int endY,
                    boolean escapable, boolean usableItem, boolean usableSkill,
                    boolean teleportable, boolean takePets, boolean recallPets) {}

    record ResourceContract(int rows, int population, String sourceSha256,
                            String resourceSha256) {}

    static final Map<Integer, String> MAP_SOURCE_SHA256 = Map.of(
            SILVERIA_MAP_ID,
                "F86DCECAE71286E7555D753452F8116C24B131FD2DB0D31575B479D923481CBC",
            BEHEMOTH_MAP_ID,
                "F168811F860D42B1B7D9FDCD8E10DF7156854C1E3916950454BF3FA582DBE746");
    static final Map<Integer, String> MAP_RESOURCE_SHA256 = Map.of(
            SILVERIA_MAP_ID,
                "3A291771775DB5EE8413BB0DB873E4D7669902A32457D8E17CDC7251A86EF1EE",
            BEHEMOTH_MAP_ID,
                "E110CF69993C9A3A771DB1A44C828A1A44DD65C85959E3A7A11517B93D25CDF7");
    static final Map<Integer, ResourceContract> NPC_CONTRACTS = Map.of(
            SILVERIA_MAP_ID, new ResourceContract(12, 0,
                    "6D14C2564DB02424E4B0A241E20E2AD98B5B583CE0524D52941CF0EC704A60E8",
                    "42A4D9E2272FD9256873509462F024CC071EA8FBBE5C36B189FC7D12345BFD4F"),
            BEHEMOTH_MAP_ID, new ResourceContract(16, 0,
                    "4D9642D90EBE96216EBAEA3219AF7CF91D66E62C3A2ED6E35ED0A91AEDC4615E",
                    "2C082F421BA9E16A4ABD584D596A55240908B7534DCDAC06D78A0D31F06ADD8A"));
    static final Map<Integer, ResourceContract> MONSTER_CONTRACTS = Map.of(
            SILVERIA_MAP_ID, new ResourceContract(0, 0, EMPTY_MONSTER_SOURCE_SHA256,
                    "1684F7F08C2E1450A75A1B5A01312E1C51CE075FEBB24FC53296BF436E31579E"),
            BEHEMOTH_MAP_ID, new ResourceContract(0, 0, EMPTY_MONSTER_SOURCE_SHA256,
                    "0B858232D014C1D54A199D10D06E42FE1DD045C2F3926FF38C3816D0E2702443"));
    static final Map<Integer, ResourceContract> DROP_CONTRACTS = Map.of(
            SILVERIA_MAP_ID, new ResourceContract(0, 0, EMPTY_DROP_SOURCE_SHA256,
                    "EFA273853C57571C63444791DEF990065FE436DDC31A0F279289FEFC9B9231BE"),
            BEHEMOTH_MAP_ID, new ResourceContract(0, 0, EMPTY_DROP_SOURCE_SHA256,
                    "9D1167C88242E5991A2A90D3F5ABDF7B89A10E406F8FBD302C1BC2FA3C3B518D"));

    private static final Map<Integer, MapFlags> FLAGS = Map.of(
            SILVERIA_MAP_ID, new MapFlags(SILVERIA_MAP_ID, "希培莉亞",
                    32704, 32895, 32768, 32959,
                    true, true, true, false, true, true),
            BEHEMOTH_MAP_ID, new MapFlags(BEHEMOTH_MAP_ID, "貝希摩斯",
                    32704, 32895, 32768, 32959,
                    true, true, true, false, true, true));

    private static final Set<Integer> STORY_NPC_IDS = Set.of(85019, 85020, 85023, 85027);
    private static final Set<Integer> DIALOGUE_NPC_IDS = Set.of(85021, 85024, 85029, 85030);
    private static final Set<Integer> SKILL_TUTOR_NPC_IDS = Set.of(85022, 85026);
    private static final Set<Integer> REPAIR_NPC_IDS = Set.of(85025, 85028);

    private ClassicWorldBehemothSilveriaCatalog() {}

    static boolean supports(int mapId) {
        return MAP_IDS.contains(mapId);
    }

    static MapFlags flags(int mapId) {
        MapFlags flags = FLAGS.get(mapId);
        if (flags == null) throw new IllegalArgumentException("not a home-town map");
        return flags;
    }

    static boolean isStoryNpc(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && supports(npc.mapId()) && STORY_NPC_IDS.contains(npc.npcId())
                && ClassicWorldStoryQuestCatalog.hasNpc(npc.npcId());
    }

    static boolean isDialogueNpc(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && supports(npc.mapId())
                && DIALOGUE_NPC_IDS.contains(npc.npcId())
                && !npc.initialHtml(0).isEmpty();
    }

    static boolean isSkillTutor(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && supports(npc.mapId())
                && SKILL_TUTOR_NPC_IDS.contains(npc.npcId())
                && npc.fixedMerchant() && npc.initialHtml(0).isEmpty();
    }

    static boolean isRepairer(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && supports(npc.mapId())
                && REPAIR_NPC_IDS.contains(npc.npcId()) && npc.fixedMerchant();
    }

    static String linkedTeleportHtml(
            ClassicWorldNpcCatalog.StaticNpc npc, String action) {
        if (npc == null || action == null || !supports(npc.mapId())) return null;
        if (npc.npcId() == 85024 && npc.spawnId() == 1058) {
            if (action.equalsIgnoreCase("teleportURL")) return "sharial2";
            if (action.equalsIgnoreCase("teleportURLA")) return "sharial3";
        }
        if (npc.npcId() == 85021 && npc.spawnId() == 1056) {
            if (action.equalsIgnoreCase("teleportURL")) return "dekabia2";
            if (action.equalsIgnoreCase("teleportURLA")) return "dekabia3";
        }
        return null;
    }
}
